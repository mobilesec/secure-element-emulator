/*
 * Copyright 2013 FH OOe Forschungs & Entwicklungs GmbH, Michael Roland.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package at.mroland.objectstaterecovery;

import at.mroland.logging.Logging;
import at.mroland.objectstaterecovery.helper.UniqueObjectIdentifier;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

/**
 *
 * @author mroland
 */
public class ClassState {
    private static final String LOG_TAG = "ClassState";
    
    protected PersistentMemory mMemoryManager;
    private Class mClassObject;
    private final String mClassName;
    private final Map<String, FieldState> mFields;
    private final Map<String, Long> mFieldReferences;
    private boolean mInitializeAfterCreation;  // indicates if class instance has never been refreshed after creation of ClassState (i.e. if no state information has been collected yet)
    private int mRefreshTag;
    private int mReachableTag;

    /**
     * Create ClassState instance for a given class.
     * 
     * @param memoryManager PersistentMemory instance that manages this FieldState hierarchy.
     * @param classObject Class to be wrapped in this FieldState.
     */
    public ClassState(PersistentMemory memoryManager,
                      Class classObject) {
        mMemoryManager = memoryManager;
        mRefreshTag = mMemoryManager.getCurrentRefreshTag();
        mReachableTag = mRefreshTag;
        mInitializeAfterCreation = true;
        mClassObject = classObject;
        if (classObject != null) {
            mClassName = classObject.getName();
        } else {
            mClassName = "";
        }
        mFields = new HashMap();
        mFieldReferences = new HashMap();
    }

    /**
     * Create ClassState instance based on class name during de-serialization.
     * 
     * @param memoryManager PersistentMemory instance that manages this FieldState hierarchy.
     * @param className Class name.
     */
    protected ClassState(PersistentMemory memoryManager,
                         String className) {
        mMemoryManager = memoryManager;
        mRefreshTag = 0;
        mReachableTag = 0;
        mInitializeAfterCreation = false;
        mClassObject = null;
        mClassName = className;
        mFields = new HashMap();
        mFieldReferences = new HashMap();
    }
    
    /**
     * Add a named member field.
     * 
     * @param qualifiedName Member field's qualified name.
     * @param fieldState 
     */
    public void addField(String qualifiedName, FieldState fieldState) {
        mFields.put(qualifiedName, fieldState);
    }

    /**
     * Recreates the links between this ClassState instance and its
     * member fields' FieldState instances (hierarchical sub-FieldState
     * instances) during de-serialization based on the given member field
     * references.
     * 
     * @param deserializedReferenceMap Mapping of de-serialized reference hash codes to corresponding FieldStates.
     */
    /* package */ void relinkReferences(Map<Long, FieldState> deserializedReferenceMap) {
        mFields.clear();
        for (Map.Entry<String, Long> fieldReference : mFieldReferences.entrySet()) {
            FieldState fieldState = deserializedReferenceMap.get(fieldReference.getValue());
            if (fieldState == null) {
                Logging.error(LOG_TAG, "Could not resolve de-serialized reference!");
            }
            mFields.put(fieldReference.getKey(), fieldState);
        }
        mFieldReferences.clear();
    }

    /**
     * Return this class's name.
     * 
     * @return Class's name.
     */
    public String getClassName() {
        return mClassName;
    }

    /**
     * Retrieve {@link Class} object for the class stored in this ClassState.
     * 
     * @return {@link Class} instance.
     */
    protected Class getClassObject() {
        if (mClassObject == null) {
            try {
                mClassObject = Class.forName(mClassName);
            } catch (Exception e) {
                Logging.error(LOG_TAG, "Could not get class by name " + mClassName, e);
            }
        }
        
        return mClassObject;
    }

    /**
     * Restore {@link Class} object static member fields based on stored ClassState.
     */
    /* package */ void restoreClass() {
        Class classObject = getClassObject();

        for (Map.Entry<String, FieldState> entry : mFields.entrySet()) {
            final String fieldQualifiedName = entry.getKey();
            final FieldState fieldState = entry.getValue();
            final String[] qnParts = fieldQualifiedName.split("#", 2);
            if (qnParts.length == 2) {
                final String className = qnParts[0];
                final String fieldName = qnParts[1];

                Logging.debug(LOG_TAG, "Restoring static field " + fieldName + " from " + className + ":");

                try {
                    if (className.equals(mClassName)) {
                        Field field = classObject.getDeclaredField(fieldName);

                        field.setAccessible(true);
                        fieldState.restoreInstanceToField(field, null);
                    } else {
                        // this case can only occur if this ClassState contains fields of other
                        // classes than this class and should, therefore, normally not happen
                        Class declaringClass = Class.forName(className);
                        if (declaringClass != null) {
                            Field field = declaringClass.getDeclaredField(fieldName);

                            field.setAccessible(true);
                            fieldState.restoreInstanceToField(field, null);
                        }
                    }
                } catch (Exception e) {
                    Logging.error(LOG_TAG, "Failed to restore " + fieldQualifiedName + ": " + e.toString(), e);
                }
            }
        }
        
        mInitializeAfterCreation = false;
        mRefreshTag = mMemoryManager.getCurrentRefreshTag();
        mReachableTag = mRefreshTag;
    }

    /**
     * Revert class to reflect image stored in ClassState.
     * Skips reversal if refresh tag indicates that class state matches
     * current state data.
     * 
     * @return this object instance (for convenience)
     */
    public final ClassState revertClass() {
        final int currentRefreshTag = mMemoryManager.getCurrentRefreshTag();
        if (mInitializeAfterCreation) {
            Logging.error(LOG_TAG, "ClassState has not been initialized yet. Can't revert without state information!");
        } else if (mRefreshTag != currentRefreshTag) {
            mRefreshTag = currentRefreshTag;  // update refresh tag to prevent multiple (or infinite recusrive) updates within one reversal cycle
            mReachableTag = currentRefreshTag;
            internalRevertClass();
        }
        return this;
    }
    
    /**
     * Revert class to reflect image stored in ClassState.
     */
    protected void internalRevertClass() {
        Class classObject = getClassObject();

        for (Map.Entry<String, FieldState> entry : mFields.entrySet()) {
            final String fieldQualifiedName = entry.getKey();
            final FieldState fieldState = entry.getValue();
            final String[] qnParts = fieldQualifiedName.split("#", 2);
            if (qnParts.length == 2) {
                final String className = qnParts[0];
                final String fieldName = qnParts[1];

                Logging.debug(LOG_TAG, "Reverting static field " + fieldName + " from " + className + ":");

                try {
                    if (className.equals(mClassName)) {
                        Field field = classObject.getDeclaredField(fieldName);

                        field.setAccessible(true);
                        fieldState.revertInstanceToField(field, null);
                    } else {
                        // this case can only occur if this ClassState contains fields of other
                        // classes than this class and should, therefore, normally not happen
                        Class declaringClass = Class.forName(className);
                        if (declaringClass != null) {
                            Field field = declaringClass.getDeclaredField(fieldName);

                            field.setAccessible(true);
                            fieldState.revertInstanceToField(field, null);
                        }
                    }
                } catch (Exception e) {
                    Logging.error(LOG_TAG, "Failed to revert " + fieldQualifiedName + ": " + e.toString(), e);
                }
            }
        }
    }
    
    /**
     * Refresh image stored in ClassState to reflect current class.
     * Skips refresh if refresh tag indicates that class state contains
     * current state data.
     * 
     * @param noDeepRefresh Do not recursively refresh the state of existing objects.
     * @return this object instance (for convenience)
     */
    public final ClassState refreshClass(boolean noDeepRefresh) {
        final int currentRefreshTag = mMemoryManager.getCurrentRefreshTag();
        if (mInitializeAfterCreation || (mRefreshTag != currentRefreshTag)) {
            mRefreshTag = currentRefreshTag;  // update refresh tag to prevent multiple (or infinite recusrive) updates within one refresh cycle
            if (!noDeepRefresh) mReachableTag = currentRefreshTag;
            mInitializeAfterCreation = false;
            internalRefreshClass(noDeepRefresh);
        }
        return this;
    }
    
    /**
     * Refresh image stored in ClassState to reflect current class.
     * 
     * @param noDeepRefresh Do not recursively refresh the state of existing objects.
     */
    protected void internalRefreshClass(boolean noDeepRefresh) {
        Class classObject = getClassObject();
        
        if (classObject != null) {
            Logging.debug(LOG_TAG, "Inner classes from " + classObject.getName() + ":");
            final Class[] instanceInnerClasses = classObject.getDeclaredClasses();
            for (Class innerClass : instanceInnerClasses) {
                mMemoryManager.storeClass(innerClass, noDeepRefresh);
            }

            //while (classObject != null) {  // don't iterate through class hierarchy, only persist fields of _this_ class
            Logging.debug(LOG_TAG, "Fields from " + classObject.getName() + ":");
            final Field[] instanceClassFields = classObject.getDeclaredFields();
            for (Field field : instanceClassFields) {
                try {
                    field.setAccessible(true);

                    final int fieldModifiers = field.getModifiers();
                    final boolean isStatic = Modifier.isStatic(fieldModifiers);
                    final boolean isFinal = Modifier.isFinal(fieldModifiers);

                    if (isStatic) {
                        Logging.debug(LOG_TAG, "" + classObject.getName() + "::" + field.getName() + " (" + field.toGenericString() + ")");

                        final Object fieldValue = field.get(null);
                        Class fieldType = field.getType();
                        final String fieldQualifiedName = classObject.getName() + "#" + field.getName();

                        if (isFinal && PrimitiveValueState.isPrimitiveValueType(fieldType)) {
                            // skip static constants of primitive types
                        } else if (isFinal && fieldType.isArray() && PrimitiveValueState.isPrimitiveValueType(fieldType.getComponentType())) {
                            // skip static constants of primitive array types
                        } else {
                            FieldState fieldState = mFields.get(fieldQualifiedName);
                            if ((fieldState == null) || (!fieldState.isIdentityMatch(fieldValue))) {
                                if (!fieldType.isPrimitive() && (fieldValue != null)) {
                                    fieldType = fieldValue.getClass();
                                }
                                fieldState = mMemoryManager.storeObject(fieldValue, fieldType, noDeepRefresh);
                                mFields.put(fieldQualifiedName, fieldState);
                            } else if (!noDeepRefresh) {
                                fieldState.refreshInstance(false);
                            }
                        }
                    }
                } catch (Exception e) {
                    Logging.error(LOG_TAG, "Exception while accessing field: " + e.toString(), e);
                }
            }
            //  classObject = classObject.getSuperclass();
            //}
        }
    }
    
    /**
     * Ping ClassState to prevent garbage collection.
     */
    /* package */ final void pingClass() {
        final int currentRefreshTag = mMemoryManager.getCurrentRefreshTag();
        if (!mInitializeAfterCreation && (mReachableTag != currentRefreshTag)) {
            mReachableTag = currentRefreshTag;
            internalPingClass();
        } else if (mInitializeAfterCreation) {
            Logging.error(LOG_TAG, "ClassState has not been initialized yet. Can't ping without state information!");
        }
    }
    
    /**
     * Ping ClassState to prevent garbage collection.
     */
    protected void internalPingClass() {
        for (FieldState fieldState : mFields.values()) {
            if (fieldState != null) {
                fieldState.pingInstance();
            }
        }
    }
    
    /**
     * Check if ClassState has been marked as reachable.
     * 
     * @return true if ClassState is marked as reachable.
     */
    /* package */ final boolean isReachable() {
        final int currentRefreshTag = mMemoryManager.getCurrentRefreshTag();
        return !mInitializeAfterCreation && (mReachableTag == currentRefreshTag);
    }
    
    /**
     * Serialize ObjectReferenceState instance to XML.
     * 
     * @param xml XmlSerializer instance used as target for serialization.
     */
    /* package */ void serializeToXml(XmlSerializer xml) {
        try {
            xml.startTag(null, XmlSchemaPersistentMemory.TAG_CLASSSTATE_CLASS);

            xml.attribute(null, XmlSchemaPersistentMemory.ATTRIBUTE_NAME, mClassName);

            for (Map.Entry<String, FieldState> field : mFields.entrySet()) {
                xml.startTag(null, XmlSchemaPersistentMemory.TAG_OBJECT_FIELD);
                xml.attribute(null, XmlSchemaPersistentMemory.ATTRIBUTE_NAME, (field != null) ? field.getKey() : "");
                long hashCode = UniqueObjectIdentifier.NULL_IDENTIFIER;
                if (field != null) {
                    final FieldState fieldState = field.getValue();
                    if (fieldState != null) {
                        hashCode = fieldState.getHashCode();
                    }
                }
                xml.attribute(null, XmlSchemaPersistentMemory.ATTRIBUTE_HASH_CODE, Long.toString(hashCode));
                xml.endTag(null, XmlSchemaPersistentMemory.TAG_OBJECT_FIELD);
            }

            xml.endTag(null, XmlSchemaPersistentMemory.TAG_CLASSSTATE_CLASS);
        } catch (Exception e) {
            Logging.error(LOG_TAG, "Exception while serializing to XML: " + e.toString(), e);
        }
    }
    
    /**
     * De-serialize ClassState from XML.
     * 
     * @param memoryManager PersistentMemory instance that manages this FieldState hierarchy.
     * @param xml XmlPullParser instance used as source for de-serialization.
     * @param tag Currently processed tag.
     * @return Returns the de-serialized ClassState instance, or null if tag does not match a
     *         ClassState or the ClassState could not be de-serialized.
     */
    /* package */ static ClassState deserializeFromXml(PersistentMemory memoryManager, XmlPullParser xml, String tag) {
        ClassState classState = null;
        
        if (tag.equals(XmlSchemaPersistentMemory.TAG_CLASSSTATE_CLASS)) {
            String className = xml.getAttributeValue(null, XmlSchemaPersistentMemory.ATTRIBUTE_NAME);

            classState = new ClassState(memoryManager, className);
            
            int eventType;
            String subTag = tag;
            do {
                try {
                    eventType = xml.next();
                } catch (Exception e) {
                    Logging.error(LOG_TAG, "Exception while de-serializing from XML: " + e.toString(), e);
                    eventType = XmlPullParser.END_TAG;
                    subTag = tag;
                }

                if (eventType == XmlPullParser.START_TAG) {
                    subTag = xml.getName();

                    if (subTag.equals(XmlSchemaPersistentMemory.TAG_OBJECT_FIELD)) {
                        String fieldName = xml.getAttributeValue(null, XmlSchemaPersistentMemory.ATTRIBUTE_NAME);
                        String fieldHashCode = xml.getAttributeValue(null, XmlSchemaPersistentMemory.ATTRIBUTE_HASH_CODE);
                        classState.mFieldReferences.put(fieldName, Long.valueOf(fieldHashCode));
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    subTag = xml.getName();
                }
            } while ((eventType != XmlPullParser.END_TAG) || !subTag.equals(tag));
            
            memoryManager.addClass(className, classState);
        }
        
        return classState;
    }
}

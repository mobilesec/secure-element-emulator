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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

/**
 *
 * @author mroland
 */
public abstract class FieldState {
    private static final String LOG_TAG = "FieldState";
    
    protected PersistentMemory mMemoryManager;
    private Object mReferencedObject;
    private Class mReferencedObjectClass;
    private final String mFieldType;
    private Long mIdentityHashCode;
    private boolean mRecreateAfterDeserialization;  // indicates if object instance still needs to be recreated as part of the de-serialization procedure
    private boolean mInitializeAfterCreation;  // indicates if object instance has never been refreshed after creation of FieldState (i.e. if no state information has been collected yet)
    private int mRefreshTag;
    private int mReachableTag;
    private List<ClassState> mBoundClasses;  // lists classes that must be updated together with this object
    private List<String> mBoundClassNames;  // lists names of classes that must be updated together with this object

    /**
     * Create FieldState instance for a given object.
     * 
     * @param memoryManager PersistentMemory instance that manages this FieldState hierarchy.
     * @param referencedObject Object to be wrapped in this FieldState.
     */
    protected FieldState(PersistentMemory memoryManager,
                         Object referencedObject) {
        mMemoryManager = memoryManager;
        mRefreshTag = mMemoryManager.getCurrentRefreshTag();
        mReachableTag = mRefreshTag;
        mRecreateAfterDeserialization = false;
        mInitializeAfterCreation = true;
        mReferencedObject = referencedObject;
        if (referencedObject != null) {
            mReferencedObjectClass = referencedObject.getClass();
            if (mReferencedObjectClass != null) {
                mFieldType = mReferencedObjectClass.getName();
            } else {
                mFieldType = "";
            }
        } else {
            mFieldType = "";
        }
        mIdentityHashCode = getObjectIdentityHashCode(referencedObject);
        mBoundClasses = new ArrayList();
        mBoundClassNames = new ArrayList();
    }

    /**
     * Create FieldState instance based on class name and reference hash code
     * during de-serialization.
     * 
     * @param memoryManager PersistentMemory instance that manages this FieldState hierarchy.
     * @param recoveredObjectClass De-serialized object class name.
     * @param recoveredIdentityHashCode De-serialized reference hash code.
     */
    protected FieldState(PersistentMemory memoryManager,
                         String recoveredObjectClass, Long recoveredIdentityHashCode) {
        mMemoryManager = memoryManager;
        mRefreshTag = 0;
        mReachableTag = 0;
        mRecreateAfterDeserialization = true;
        mInitializeAfterCreation = false;
        mReferencedObject = null;
        mReferencedObjectClass = null;
        mFieldType = recoveredObjectClass;
        mIdentityHashCode = recoveredIdentityHashCode;
        mBoundClasses = new ArrayList();
        mBoundClassNames = new ArrayList();
    }

    /**
     * Get unique hash code for given object.
     * 
     * @param o An object.
     * @return Unique identity hash code for <code>o</code>.
     */
    public static Long getObjectIdentityHashCode(Object o) {
        if ((o != null) && PrimitiveValueState.isPrimitiveValueType(o.getClass())) {
            return Long.valueOf(PrimitiveValueState.getPrimitiveValueIdentityHashCode(o));
        }
        
        return Long.valueOf(UniqueObjectIdentifier.get(o));
    }

    /**
     * Get the unique hash code for the object wrapped in this FieldState.
     * 
     * @return Unique identity hash code.
     */
    public long getHashCode() {
        return mIdentityHashCode.longValue();
    }
    
    /**
     * Test if the given Object matches the Object recorded in this FieldState.
     * 
     * @param o Object to check
     * @return Returns true on identity match, else false.
     */
    public boolean isIdentityMatch(Object o) {
        Long newHashCode = FieldState.getObjectIdentityHashCode(o);
        return newHashCode.longValue() == getHashCode();
    }

    /**
     * Get the {@link Class} object representing this object instance's class.
     * 
     * @return {@link Class} object.
     */
    protected Class getObjectClass() {
        if ((mFieldType == null) || mFieldType.isEmpty()) {
            return null;
        }

        if (mReferencedObjectClass == null) {
            try {
                mReferencedObjectClass = Class.forName(mFieldType);
            } catch (Exception e) {
                Logging.error(LOG_TAG, "Could not get class by name " + mFieldType, e);
            }
        }
        
        return mReferencedObjectClass;
    }

    /**
     * Get the textual representation of this object instance's class.
     * 
     * @return Class name.
     */
    protected String getFieldType() {
        if (mFieldType == null) {
            return "";
        }
        
        return mFieldType;
    }
    
    /**
     * Indicates if the associated object instance has been recreated
     * as part of the de-serialization procedure.
     * If this returns false <i>after</i> de-serialization is complete,
     * this means that the object tree spanning from this FieldState is
     * unreachable (not referenced) and, thus, can be garbage collected.
     * 
     * @return true if object instance has been recreated, else false.
     */
    public boolean isRecreated() {
        return !mRecreateAfterDeserialization;
    }

    /**
     * Get the current instance of the object wrapped in this FieldState.
     * 
     * @return Object instance.
     */
    public Object getInstance() {
        if (mRecreateAfterDeserialization) {
            return restoreInstance();
        } else {
            return mReferencedObject;
        }
    }

    /**
     * Recreates the links between this FieldState instance and its
     * hierarchical sub-FieldState instances during de-serialization.
     * 
     * @param deserializedReferenceMap Mapping of de-serialized reference hash codes to corresponding FieldStates.
     */
    /* package */ void relinkReferences(Map<Long, FieldState> deserializedReferenceMap) {
        mBoundClasses.clear();
        for (String className : mBoundClassNames) {
            ClassState classState = mMemoryManager.getClass(className);
            if (classState == null) {
                Logging.error(LOG_TAG, "Could not resolve de-serialized class name!");
            }
            mBoundClasses.add(classState);
        }
        mBoundClassNames.clear();
    }

    /**
     * Restore Object instance to a parent objects member field.
     * 
     * @param field Member field.
     * @param parentInstance Parent object.
     * @param instance Object instance to load.
     * @throws Exception 
     */
    protected void loadInstanceToField(Field field, Object parentInstance, Object instance) throws Exception {
        field.set(parentInstance, instance);
    }
    
    /**
     * Restore object instance from de-serialized FieldState.
     * 
     * @return Restored object instance.
     */
    protected abstract Object restoreInstance();

    /**
     * Re-link newly created object instance after restoring instance
     * from de-serialized FieldState. Must be called at the end of
     * {@link #restoreInstance()}.
     * 
     * @param newInstance Newly created object instance.
     */
    protected final void setInstanceRestored(Object newInstance) {
        mReferencedObject = newInstance;
        mReferencedObjectClass = getObjectClass();
        mIdentityHashCode = getObjectIdentityHashCode(newInstance);
        mRecreateAfterDeserialization = false;
        mInitializeAfterCreation = false;
        mRefreshTag = mMemoryManager.getCurrentRefreshTag();
        mReachableTag = mRefreshTag;
        mMemoryManager.addReference(mIdentityHashCode, this);
    }

    /**
     * Restore this FieldState to a parent objects member field.
     * 
     * @param field Member field.
     * @param parentInstance Parent object.
     * @throws Exception 
     */
    /* package */ void restoreInstanceToField(Field field, Object parentInstance) throws Exception {
        Object instance = getInstance();
        loadInstanceToField(field, parentInstance, instance);
    }

    /**
     * Revert object to reflect image stored in FieldState.
     * Skips reversal if refresh tag indicates that object state matches
     * current state data.
     * 
     * @return this object instance (for convenience)
     */
    public final FieldState revertInstance() {
        final int currentRefreshTag = mMemoryManager.getCurrentRefreshTag();
        if (mInitializeAfterCreation) {
            Logging.error(LOG_TAG, "FieldState has not been initialized yet. Can't revert without state information!");
        } else if (mRefreshTag != currentRefreshTag) {
            mRefreshTag = currentRefreshTag;  // update refresh tag to prevent multiple (or infinite recursive) updates within one refresh cycle
            mReachableTag = currentRefreshTag;
            internalRevertInstance();
            
            for (ClassState classState : mBoundClasses) {
                classState.revertClass();
            }
        }
        return this;
    }

    /**
     * Revert object to reflect image stored in FieldState.
     */
    protected abstract void internalRevertInstance();
    
    /**
     * Revert this FieldState to a parent objects member field.
     * 
     * @param field Member field.
     * @param parentInstance Parent object.
     * @throws Exception 
     */
    /* package */ void revertInstanceToField(Field field, Object parentInstance) throws Exception {
        restoreInstanceToField(field, parentInstance);
        revertInstance();
    }

    /**
     * Refresh image stored in FieldState to reflect current objects.
     * Skips refresh if refresh tag indicates that object state contains
     * current state data.
     * 
     * @param noDeepRefresh Do not recursively refresh the state of existing objects.
     * @return this object instance (for convenience)
     */
    public final FieldState refreshInstance(boolean noDeepRefresh) {
        final int currentRefreshTag = mMemoryManager.getCurrentRefreshTag();
        if (mInitializeAfterCreation || (mRefreshTag != currentRefreshTag)) {
            mRefreshTag = currentRefreshTag;  // update refresh tag to prevent multiple (or infinite recursive) updates within one refresh cycle
            mInitializeAfterCreation = false;
            internalRefreshInstance(noDeepRefresh);
            
            if (!noDeepRefresh) {
                mReachableTag = currentRefreshTag;
                for (ClassState classState : mBoundClasses) {
                    classState.refreshClass(false);
                }
            }
        }
        return this;
    }
    
    /**
     * Refresh image stored in FieldState to reflect current objects.
     * 
     * @param noDeepRefresh Do not recursively refresh the state of existing objects.
     */
    protected abstract void internalRefreshInstance(boolean noDeepRefresh);
    
    /**
     * Ping FieldState to prevent garbage collection.
     */
    /* package */ final void pingInstance() {
        final int currentRefreshTag = mMemoryManager.getCurrentRefreshTag();
        if (!mInitializeAfterCreation && (mReachableTag != currentRefreshTag)) {
            Logging.debug(LOG_TAG, "Ping " + currentRefreshTag + ": #" + mIdentityHashCode);
            mReachableTag = currentRefreshTag;
            internalPingInstance();
            
            for (ClassState classState : mBoundClasses) {
                classState.pingClass();
            }
        } else if (mInitializeAfterCreation) {
            Logging.error(LOG_TAG, "FieldState has not been initialized yet. Can't ping without state information!");
        } else {
            Logging.debug(LOG_TAG, "Re-ping " + currentRefreshTag + ": #" + mIdentityHashCode);
        }
    }
    
    /**
     * Ping FieldState to prevent garbage collection.
     */
    protected abstract void internalPingInstance();
    
    /**
     * Check if FieldState has been marked as reachable.
     * 
     * @return true if FieldState is marked as reachable.
     */
    /* package */ final boolean isReachable() {
        if (mInitializeAfterCreation) {
            Logging.error(LOG_TAG, "FieldState is tested for reachability prior to initialization!");
        }
        if (mRecreateAfterDeserialization) {
            Logging.error(LOG_TAG, "FieldState is tested for reachability prior to recreation!");
            return true;
        }
        final int currentRefreshTag = mMemoryManager.getCurrentRefreshTag();
        return !mInitializeAfterCreation && (mReachableTag == currentRefreshTag);
    }
    
    /**
     * Bind a class to this FieldState. If this FieldState is refreshed/
     * reverted, bound classes will be refreshed/reverted too.
     * 
     * @param classState ClassState to be bound to this FieldState.
     */
    public void addBoundClass(ClassState classState) {
        if (!mBoundClasses.contains(classState)) {
            mBoundClasses.add(classState);
        }
    }
    
    /**
     * Bind a de-serialized class name to this FieldState.
     * 
     * @param className Class name to be bound to this FieldState.
     */
    protected void addDeserializedBoundClassName(String className) {
        if (!mBoundClassNames.contains(className)) {
            mBoundClassNames.add(className);
        }
    }
    
    /**
     * Serialize FieldState instance to XML.
     * 
     * @param xml XmlSerializer instance used as target for serialization.
     */
    public void serializeToXml(XmlSerializer xml) {
        try {
            xml.attribute(null, XmlSchemaPersistentMemory.ATTRIBUTE_HASH_CODE, mIdentityHashCode.toString());
            xml.attribute(null, XmlSchemaPersistentMemory.ATTRIBUTE_TYPE, mFieldType);
        } catch (Exception e) {
            Logging.error(LOG_TAG, "Exception while serializing to XML: " + e.toString(), e);
        }
    }
    
    /**
     * Serialize bound classes to XML.
     * 
     * @param xml XmlSerializer instance used as target for serialization.
     */
    protected void serializeBoundClassesToXml(XmlSerializer xml) {
        try {
            for (ClassState classState : mBoundClasses) {
                xml.startTag(null, XmlSchemaPersistentMemory.TAG_BOUNDCLASS);
                xml.attribute(null, XmlSchemaPersistentMemory.ATTRIBUTE_NAME, (classState != null) ? classState.getClassName() : "");
                xml.endTag(null, XmlSchemaPersistentMemory.TAG_BOUNDCLASS);
            }
        } catch (Exception e) {
            Logging.error(LOG_TAG, "Exception while serializing to XML: " + e.toString(), e);
        }
    }

    public static class DeserializedFieldState {
        Long hashCode;
        FieldState fieldState;
    }
    /**
     * De-serialize FieldState from XML.
     * 
     * @param memoryManager PersistentMemory instance that manages this FieldState hierarchy.
     * @param xml XmlPullParser instance used as source for de-serialization.
     * @param tag Currently processed tag.
     * @return Returns the de-serialized FieldState instance, or null if tag does not match a
     *         FieldState or the FieldState could not be de-serialized.
     */
    public static DeserializedFieldState deserializeFromXml(PersistentMemory memoryManager, XmlPullParser xml, String tag) {
        if (tag.equals(XmlSchemaPersistentMemory.TAG_FIELDSTATE_OBJECT)) {
            return ObjectReferenceState.deserializeFromXml(memoryManager, xml, tag);
        } else if (tag.equals(XmlSchemaPersistentMemory.TAG_FIELDSTATE_ARRAY)) {
            return ArrayState.deserializeFromXml(memoryManager, xml, tag);
        } else if (tag.equals(XmlSchemaPersistentMemory.TAG_FIELDSTATE_TRANSIENT_ARRAY)) {
            return TransientArrayState.deserializeFromXml(memoryManager, xml, tag);
        } else if (tag.equals(XmlSchemaPersistentMemory.TAG_FIELDSTATE_PRIMITIVE)) {
            return PrimitiveValueState.deserializeFromXml(memoryManager, xml, tag);
        }
        
        return null;
    }
}

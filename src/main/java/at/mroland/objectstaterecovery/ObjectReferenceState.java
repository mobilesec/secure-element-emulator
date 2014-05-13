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
import org.objenesis.ObjenesisHelper;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

/**
 *
 * @author mroland
 */
public class ObjectReferenceState extends FieldState {
    private static final String LOG_TAG = "ObjectReferenceState";
    private final Map<String, FieldState> mFields;
    private final Map<String, Long> mFieldReferences;

    public ObjectReferenceState(PersistentMemory memoryManager,
                                Object referencedObject) {
        super(memoryManager, referencedObject);
        mFields = new HashMap();
        mFieldReferences = new HashMap();
    }

    protected ObjectReferenceState(PersistentMemory memoryManager,
                                   String recoveredObjectClass, Long recoveredIdentityHashCode) {
        super(memoryManager, recoveredObjectClass, recoveredIdentityHashCode);
        mFields = new HashMap();
        mFieldReferences = new HashMap();
    }

    /**
     * Recreates the links between this FieldState instance and its
     * member fields' FieldState instances (hierarchical sub-FieldState
     * instances) during de-serialization based on the given member field
     * references.
     * 
     * @param deserializedReferenceMap Mapping of de-serialized reference hash codes to corresponding FieldStates.
     */
    @Override
    /* package */ void relinkReferences(Map<Long, FieldState> deserializedReferenceMap) {
        super.relinkReferences(deserializedReferenceMap);
        mFields.clear();
        for (Map.Entry<String, Long> fieldReference : mFieldReferences.entrySet()) {
            FieldState fieldState = deserializedReferenceMap.get(fieldReference.getValue());
            if (fieldState == null) {
                Logging.error(LOG_TAG, "Could not resolve de-serialized reference #" + fieldReference.getValue() + "!");
            }
            mFields.put(fieldReference.getKey(), fieldState);
        }
        mFieldReferences.clear();
    }
    

    /**
     * Restore object instance from de-serialized ObjectReferenceState.
     * 
     * @return Restored object instance.
     */
    protected Object restoreInstance() {
        Class objectClass = getObjectClass();

        if (objectClass == null) {
            setInstanceRestored(null);
            return null;
        }

        Object instance = ObjenesisHelper.newInstance(objectClass);
        setInstanceRestored(instance);

        for (Map.Entry<String, FieldState> entry : mFields.entrySet()) {
            final String fieldQualifiedName = entry.getKey();
            final FieldState fieldState = entry.getValue();
            final String[] qnParts = fieldQualifiedName.split("#", 2);
            if (qnParts.length == 2) {
                final String className = qnParts[0];
                final String fieldName = qnParts[1];

                Logging.debug(LOG_TAG, "Restoring field " + fieldName + " from " + className + ":");

                try {
                    Class declaringClass = Class.forName(className);
                    Field field = declaringClass.getDeclaredField(fieldName);

                    field.setAccessible(true);
                    fieldState.restoreInstanceToField(field, instance);
                } catch (Exception e) {
                    Logging.error(LOG_TAG, "Failed to restore " + fieldQualifiedName + ": " + e.toString(), e);
                }
            }
        }

        return instance;
    }

    /**
     * Revert object to reflect image stored in ObjectReferenceState.
     */
    protected void internalRevertInstance() {
        Object instance = getInstance();
        if (instance != null) {
            for (Map.Entry<String, FieldState> entry : mFields.entrySet()) {
                final String fieldQualifiedName = entry.getKey();
                final FieldState fieldState = entry.getValue();
                final String[] qnParts = fieldQualifiedName.split("#", 2);
                if (qnParts.length == 2) {
                    final String className = qnParts[0];
                    final String fieldName = qnParts[1];

                    Logging.debug(LOG_TAG, "Revert field " + fieldName + " from " + className + ":");

                    try {
                        Class declaringClass = Class.forName(className);
                        Field field = declaringClass.getDeclaredField(fieldName);

                        field.setAccessible(true);
                        fieldState.revertInstanceToField(field, instance);
                    } catch (Exception e) {
                        Logging.error(LOG_TAG, "Failed to revert " + fieldQualifiedName + ": " + e.toString(), e);
                    }
                }
            }
        }
    }

    /**
     * Refresh image stored in ObjectReferenceState to reflect current object.
     * 
     * @param noDeepRefresh Do not recursively refresh the state of existing objects.
     */
    protected void internalRefreshInstance(boolean noDeepRefresh) {
        Object object = getInstance();
        
        if (object != null) {
            Class objectClass = getObjectClass();

            Logging.debug(LOG_TAG, "" + getHashCode() + ": " + getFieldType());

            while (objectClass != null) {
                Logging.debug(LOG_TAG, "Fields from " + objectClass.getName() + ":");
                final Field[] instanceClassFields = objectClass.getDeclaredFields();
                for (Field field : instanceClassFields) {
                    try {
                        field.setAccessible(true);

                        final int fieldModifiers = field.getModifiers();
                        final boolean isStatic = Modifier.isStatic(fieldModifiers);

                        if (!isStatic) {
                            Logging.debug(LOG_TAG, "" + objectClass.getName() + "::" + field.getName() + " (" + field.toGenericString() + ")");

                            final Object fieldValue = field.get(object);
                            Class fieldType = field.getType();
                            final String fieldQualifiedName = objectClass.getName() + "#" + field.getName();
                            //final String fieldPath = path + "." + field.getName();

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
                    } catch (Exception e) {
                        Logging.error(LOG_TAG, "Exception while accessing field: " + e.toString(), e);
                    }
                }

                objectClass = objectClass.getSuperclass();
            }
        }
    }
    
    /**
     * Ping ObjectReferenceState to prevent garbage collection.
     */
    protected void internalPingInstance() {
        for (FieldState fieldState : mFields.values()) {
            if (fieldState != null) {
                fieldState.pingInstance();
            }
        }
    }
    
    /**
     * Serialize ObjectReferenceState instance to XML.
     * 
     * @param xml XmlSerializer instance used as target for serialization.
     */
    @Override
    public void serializeToXml(XmlSerializer xml) {
        try {
            xml.startTag(null, XmlSchemaPersistentMemory.TAG_FIELDSTATE_OBJECT);

            super.serializeToXml(xml);

            serializeBoundClassesToXml(xml);
            
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

            xml.endTag(null, XmlSchemaPersistentMemory.TAG_FIELDSTATE_OBJECT);
        } catch (Exception e) {
            Logging.error(LOG_TAG, "Exception while serializing to XML: " + e.toString(), e);
        }
    }
    
    /**
     * De-serialize ObjectReferenceState from XML.
     * 
     * @param memoryManager PersistentMemory instance that manages this FieldState hierarchy.
     * @param xml XmlPullParser instance used as source for de-serialization.
     * @param tag Currently processed tag.
     * @return Returns the de-serialized ObjectReferenceState instance, or null if tag does not match an
     *         ObjectReferenceState or the ObjectReferenceState could not be de-serialized.
     */
    public static DeserializedFieldState deserializeFromXml(PersistentMemory memoryManager, XmlPullParser xml, String tag) {
        DeserializedFieldState deserializationResult = new DeserializedFieldState();
        
        if (tag.equals(XmlSchemaPersistentMemory.TAG_FIELDSTATE_OBJECT)) {
            String hashCode = xml.getAttributeValue(null, XmlSchemaPersistentMemory.ATTRIBUTE_HASH_CODE);
            String fieldType = xml.getAttributeValue(null, XmlSchemaPersistentMemory.ATTRIBUTE_TYPE);

            deserializationResult.hashCode = Long.valueOf(hashCode);
            deserializationResult.fieldState = new ObjectReferenceState(memoryManager, fieldType, deserializationResult.hashCode);
            
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
                        ((ObjectReferenceState)deserializationResult.fieldState).mFieldReferences.put(fieldName, Long.valueOf(fieldHashCode));
                    } else if (subTag.equals(XmlSchemaPersistentMemory.TAG_BOUNDCLASS)) {
                        String fieldName = xml.getAttributeValue(null, XmlSchemaPersistentMemory.ATTRIBUTE_NAME);
                        deserializationResult.fieldState.addDeserializedBoundClassName(fieldName);
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    subTag = xml.getName();
                }
            } while ((eventType != XmlPullParser.END_TAG) || !subTag.equals(tag));
        }
        
        return deserializationResult;
    }
}

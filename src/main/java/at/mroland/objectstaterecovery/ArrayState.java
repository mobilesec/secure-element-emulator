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
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Map;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

/**
 *
 * @author mroland
 */
public class ArrayState extends FieldState {
    private static final String LOG_TAG = "ArrayState";
    protected final ArrayList<FieldState> mElements;
    protected final ArrayList<Long> mElementReferences;
    protected final String mElementType;

    public ArrayState(PersistentMemory memoryManager,
                      Object referencedObject,
                      String elementType) {
        super(memoryManager, referencedObject);
        mElements = new ArrayList();
        mElementReferences = new ArrayList();
        mElementType = elementType;
    }

    public ArrayState(PersistentMemory memoryManager,
                      String recoveredObjectClass, Long recoveredIdentityHashCode,
                      String elementType) {
        super(memoryManager, recoveredObjectClass, recoveredIdentityHashCode);
        mElements = new ArrayList();
        mElementReferences = new ArrayList();
        mElementType = elementType;
    }

    /**
     * Recreates the links between this FieldState instance and its
     * elements' FieldState instances (hierarchical sub-FieldState
     * instances) during de-serialization based on the given element
     * references.
     * 
     * @param deserializedReferenceMap Mapping of de-serialized reference hash codes to corresponding FieldStates.
     */
    @Override
    /* package */ void relinkReferences(Map<Long, FieldState> deserializedReferenceMap) {
        super.relinkReferences(deserializedReferenceMap);
        mElements.clear();
        for (Long elementReference : mElementReferences) {
            FieldState fieldState = deserializedReferenceMap.get(elementReference);
            if (fieldState == null) {
                Logging.error(LOG_TAG, "Could not resolve de-serialized reference #" + elementReference + "!");
            }
            mElements.add(fieldState);
        }
        mElementReferences.clear();
    }

    /**
     * Get the class of the elements of the array stored in this ArrayState.
     * 
     * @return Class of array elements.
     */
    protected Class getElementClass() {
        try {
            Class objectClass;
            if (boolean.class.getName().equals(mElementType)) {
                objectClass = boolean.class;
            } else if (byte.class.getName().equals(mElementType)) {
                objectClass = byte.class;
            } else if (short.class.getName().equals(mElementType)) {
                objectClass = short.class;
            } else if (int.class.getName().equals(mElementType)) {
                objectClass = int.class;
            } else if (long.class.getName().equals(mElementType)) {
                objectClass = long.class;
            } else if (float.class.getName().equals(mElementType)) {
                objectClass = float.class;
            } else if (double.class.getName().equals(mElementType)) {
                objectClass = double.class;
            } else if (char.class.getName().equals(mElementType)) {
                objectClass = char.class;
            } else {
                objectClass = Class.forName(mElementType);
            }
            return objectClass;
        } catch (Exception e) {
        }

        return null;
    }

    /**
     * Restore array instance from de-serialized ArrayState.
     * 
     * @return Restored array object instance.
     */
    protected Object restoreInstance() {
        // TODO: remove debugging
        long oldHashCode = getHashCode();
        int elementIndex = -1;
        long elementHashCode = -1;
        long elementValue = -1;
        try {
        final int numElements = mElements.size();

        if (boolean.class.getName().equals(mElementType)) {
            boolean[] array = new boolean[numElements];
            setInstanceRestored(array);

            int i = 0;
            for (FieldState fieldState : mElements) {
                elementIndex = i;
                elementHashCode = fieldState.getHashCode();
                array[i++] = ((Boolean)fieldState.getInstance()).booleanValue();
            }

            return array;
        } else if (byte.class.getName().equals(mElementType)) {
            byte[] array = new byte[numElements];
            setInstanceRestored(array);

            int i = 0;
            for (FieldState fieldState : mElements) {
                elementIndex = i;
                elementHashCode = fieldState.getHashCode();
                elementValue = ((Number)fieldState.getInstance()).longValue();
                array[i++] = ((Byte)fieldState.getInstance()).byteValue();
            }

            return array;
        } else if (short.class.getName().equals(mElementType)) {
            short[] array = new short[numElements];
            setInstanceRestored(array);

            int i = 0;
            for (FieldState fieldState : mElements) {
                elementIndex = i;
                elementHashCode = fieldState.getHashCode();
                elementValue = ((Number)fieldState.getInstance()).longValue();
                array[i++] = ((Short)fieldState.getInstance()).shortValue();
            }

            return array;
        } else if (int.class.getName().equals(mElementType)) {
            int[] array = new int[numElements];
            setInstanceRestored(array);

            int i = 0;
            for (FieldState fieldState : mElements) {
                elementIndex = i;
                elementHashCode = fieldState.getHashCode();
                elementValue = ((Number)fieldState.getInstance()).longValue();
                array[i++] = ((Integer)fieldState.getInstance()).intValue();
            }

            return array;
        } else if (long.class.getName().equals(mElementType)) {
            long[] array = new long[numElements];
            setInstanceRestored(array);

            int i = 0;
            for (FieldState fieldState : mElements) {
                elementIndex = i;
                elementHashCode = fieldState.getHashCode();
                elementValue = ((Number)fieldState.getInstance()).longValue();
                array[i++] = ((Long)fieldState.getInstance()).longValue();
            }

            return array;
        } else if (float.class.getName().equals(mElementType)) {
            float[] array = new float[numElements];
            setInstanceRestored(array);

            int i = 0;
            for (FieldState fieldState : mElements) {
                elementIndex = i;
                elementHashCode = fieldState.getHashCode();
                elementValue = ((Number)fieldState.getInstance()).longValue();
                array[i++] = ((Float)fieldState.getInstance()).floatValue();
            }

            return array;
        } else if (double.class.getName().equals(mElementType)) {
            double[] array = new double[numElements];
            setInstanceRestored(array);

            int i = 0;
            for (FieldState fieldState : mElements) {
                elementIndex = i;
                elementHashCode = fieldState.getHashCode();
                elementValue = ((Number)fieldState.getInstance()).longValue();
                array[i++] = ((Double)fieldState.getInstance()).doubleValue();
            }

            return array;
        } else if (char.class.getName().equals(mElementType)) {
            char[] array = new char[numElements];
            setInstanceRestored(array);

            int i = 0;
            for (FieldState fieldState : mElements) {
                elementIndex = i;
                elementHashCode = fieldState.getHashCode();
                array[i++] = ((Character)fieldState.getInstance()).charValue();
            }

            return array;
        } else {
            Object[] array = (Object[])Array.newInstance(getElementClass(), numElements);
            setInstanceRestored(array);

            int i = 0;
            for (FieldState fieldState : mElements) {
                elementIndex = i;
                elementHashCode = fieldState.getHashCode();
                array[i++] = fieldState.getInstance();
            }

            return array;
        }
        }catch(RuntimeException e){
            Logging.error(LOG_TAG, "ArrayState(old #" + oldHashCode + ", new #" + getHashCode() + "): " + getFieldType() + " at index " + elementIndex + " with #" + elementHashCode + " and value " + elementValue);
            throw e;
        }
    }

    /**
     * Revert array instance to reflect image stored in ArrayState.
     */
    protected void internalRevertInstance() {
        Object instance = getInstance();
        
        if (instance == null) {
            Logging.error(LOG_TAG, "Trying to revert ArrayState that has not been created!");
        } else {
            final int numElements = mElements.size();

            if (boolean.class.getName().equals(mElementType)) {
                if (((boolean[])instance).length == numElements) {
                    int i = 0;
                    for (FieldState fieldState : mElements) {
                        fieldState.revertInstance();
                        ((boolean[])instance)[i++] = ((Boolean)fieldState.getInstance()).booleanValue();
                    }
                } else {
                    Logging.error(LOG_TAG, "Unexpected array length");
                }
            } else if (byte.class.getName().equals(mElementType)) {
                if (((byte[])instance).length == numElements) {
                    int i = 0;
                    for (FieldState fieldState : mElements) {
                        fieldState.revertInstance();
                        ((byte[])instance)[i++] = ((Byte)fieldState.getInstance()).byteValue();
                    }
                } else {
                    Logging.error(LOG_TAG, "Unexpected array length");
                }
            } else if (short.class.getName().equals(mElementType)) {
                if (((short[])instance).length == numElements) {
                    int i = 0;
                    for (FieldState fieldState : mElements) {
                        fieldState.revertInstance();
                        ((short[])instance)[i++] = ((Short)fieldState.getInstance()).shortValue();
                    }
                } else {
                    Logging.error(LOG_TAG, "Unexpected array length");
                }
            } else if (int.class.getName().equals(mElementType)) {
                if (((int[])instance).length == numElements) {
                    int i = 0;
                    for (FieldState fieldState : mElements) {
                        fieldState.revertInstance();
                        ((int[])instance)[i++] = ((Integer)fieldState.getInstance()).intValue();
                    }
                } else {
                    Logging.error(LOG_TAG, "Unexpected array length");
                }
            } else if (long.class.getName().equals(mElementType)) {
                if (((long[])instance).length == numElements) {
                    int i = 0;
                    for (FieldState fieldState : mElements) {
                        fieldState.revertInstance();
                        ((long[])instance)[i++] = ((Long)fieldState.getInstance()).longValue();
                    }
                } else {
                    Logging.error(LOG_TAG, "Unexpected array length");
                }
            } else if (float.class.getName().equals(mElementType)) {
                if (((float[])instance).length == numElements) {
                    int i = 0;
                    for (FieldState fieldState : mElements) {
                        fieldState.revertInstance();
                        ((float[])instance)[i++] = ((Float)fieldState.getInstance()).floatValue();
                    }
                } else {
                    Logging.error(LOG_TAG, "Unexpected array length");
                }
            } else if (double.class.getName().equals(mElementType)) {
                if (((double[])instance).length == numElements) {
                    int i = 0;
                    for (FieldState fieldState : mElements) {
                        fieldState.revertInstance();
                        ((double[])instance)[i++] = ((Double)fieldState.getInstance()).doubleValue();
                    }
                } else {
                    Logging.error(LOG_TAG, "Unexpected array length");
                }
            } else if (char.class.getName().equals(mElementType)) {
                if (((char[])instance).length == numElements) {
                    int i = 0;
                    for (FieldState fieldState : mElements) {
                        fieldState.revertInstance();
                        ((char[])instance)[i++] = ((Character)fieldState.getInstance()).charValue();
                    }
                } else {
                    Logging.error(LOG_TAG, "Unexpected array length");
                }
            } else {
                if (((Object[])instance).length == numElements) {
                    int i = 0;
                    for (FieldState fieldState : mElements) {
                        fieldState.revertInstance();
                        ((Object[])instance)[i++] = fieldState.getInstance();
                    }
                } else {
                    Logging.error(LOG_TAG, "Unexpected array length");
                }
            }
        }
    }

    /**
     * Refresh image stored in ArrayState to reflect current array.
     * 
     * @param noDeepRefresh Do not recursively refresh the state of existing objects.
     */
    protected void internalRefreshInstance(boolean noDeepRefresh) {
        Object instance = getInstance();
        
        if (instance == null) {
            Logging.error(LOG_TAG, "Trying to refresh ArrayState that has not been created!");
        } else {
            Class componentClass = getElementClass();

            if (componentClass == null) {
                Logging.error(LOG_TAG, "Unexpected component class: null");
            } else {
                Logging.debug(LOG_TAG, "" + getHashCode() + ": Array of " + componentClass.getName());

                if (!componentClass.isPrimitive()) {
                    Object[] array = (Object[])instance;
                    
                    if (array.length == mElements.size()) {
                        for (int i = 0; i < array.length; ++i) {
                            FieldState fieldState = mElements.get(i);
                            if ((fieldState == null) || (!fieldState.isIdentityMatch(array[i]))) {
                                fieldState = mMemoryManager.storeObject(array[i], (array[i] == null) ? componentClass : array[i].getClass(), noDeepRefresh);
                                mElements.set(i, fieldState);
                            } else if (!noDeepRefresh) {
                                fieldState.refreshInstance(false);
                            }
                        }
                    } else {
                        mElements.clear();
                        mElements.ensureCapacity(array.length);
                        for (int i = 0; i < array.length; ++i) {
                            FieldState fieldState = mMemoryManager.storeObject(array[i], (array[i] == null) ? componentClass : array[i].getClass(), noDeepRefresh);
                            mElements.add(fieldState);
                        }
                    }
                } else if (componentClass == boolean.class) {
                    boolean[] array = (boolean[])instance;

                    if (array.length == mElements.size()) {
                        for (int i = 0; i < array.length; ++i) {
                            FieldState fieldState = mElements.get(i);
                            if ((fieldState == null) || (!fieldState.isIdentityMatch(array[i]))) {
                                fieldState = mMemoryManager.storeObject(array[i], componentClass, noDeepRefresh);
                                mElements.set(i, fieldState);
                            } else if (!noDeepRefresh) {
                                fieldState.refreshInstance(false);
                            }
                        }
                    } else {
                        mElements.clear();
                        mElements.ensureCapacity(array.length);
                        for (int i = 0; i < array.length; ++i) {
                            FieldState fieldState = mMemoryManager.storeObject(array[i], componentClass, noDeepRefresh);
                            mElements.add(fieldState);
                        }
                    }
                } else if (componentClass == byte.class) {
                    byte[] array = (byte[])instance;

                    if (array.length == mElements.size()) {
                        for (int i = 0; i < array.length; ++i) {
                            FieldState fieldState = mElements.get(i);
                            if ((fieldState == null) || (!fieldState.isIdentityMatch(array[i]))) {
                                fieldState = mMemoryManager.storeObject(array[i], componentClass, noDeepRefresh);
                                mElements.set(i, fieldState);
                            } else if (!noDeepRefresh) {
                                fieldState.refreshInstance(false);
                            }
                        }
                    } else {
                        mElements.clear();
                        mElements.ensureCapacity(array.length);
                        for (int i = 0; i < array.length; ++i) {
                            FieldState fieldState = mMemoryManager.storeObject(array[i], componentClass, noDeepRefresh);
                            mElements.add(fieldState);
                        }
                    }
                } else if (componentClass == short.class) {
                    short[] array = (short[])instance;

                    if (array.length == mElements.size()) {
                        for (int i = 0; i < array.length; ++i) {
                            FieldState fieldState = mElements.get(i);
                            if ((fieldState == null) || (!fieldState.isIdentityMatch(array[i]))) {
                                fieldState = mMemoryManager.storeObject(array[i], componentClass, noDeepRefresh);
                                mElements.set(i, fieldState);
                            } else if (!noDeepRefresh) {
                                fieldState.refreshInstance(false);
                            }
                        }
                    } else {
                        mElements.clear();
                        mElements.ensureCapacity(array.length);
                        for (int i = 0; i < array.length; ++i) {
                            FieldState fieldState = mMemoryManager.storeObject(array[i], componentClass, noDeepRefresh);
                            mElements.add(fieldState);
                        }
                    }
                } else if (componentClass == int.class) {
                    int[] array = (int[])instance;

                    if (array.length == mElements.size()) {
                        for (int i = 0; i < array.length; ++i) {
                            FieldState fieldState = mElements.get(i);
                            if ((fieldState == null) || (!fieldState.isIdentityMatch(array[i]))) {
                                fieldState = mMemoryManager.storeObject(array[i], componentClass, noDeepRefresh);
                                mElements.set(i, fieldState);
                            } else if (!noDeepRefresh) {
                                fieldState.refreshInstance(false);
                            }
                        }
                    } else {
                        mElements.clear();
                        mElements.ensureCapacity(array.length);
                        for (int i = 0; i < array.length; ++i) {
                            FieldState fieldState = mMemoryManager.storeObject(array[i], componentClass, noDeepRefresh);
                            mElements.add(fieldState);
                        }
                    }
                } else if (componentClass == long.class) {
                    long[] array = (long[])instance;

                    if (array.length == mElements.size()) {
                        for (int i = 0; i < array.length; ++i) {
                            FieldState fieldState = mElements.get(i);
                            if ((fieldState == null) || (!fieldState.isIdentityMatch(array[i]))) {
                                fieldState = mMemoryManager.storeObject(array[i], componentClass, noDeepRefresh);
                                mElements.set(i, fieldState);
                            } else if (!noDeepRefresh) {
                                fieldState.refreshInstance(false);
                            }
                        }
                    } else {
                        mElements.clear();
                        mElements.ensureCapacity(array.length);
                        for (int i = 0; i < array.length; ++i) {
                            FieldState fieldState = mMemoryManager.storeObject(array[i], componentClass, noDeepRefresh);
                            mElements.add(fieldState);
                        }
                    }
                } else if (componentClass == float.class) {
                    float[] array = (float[])instance;

                    if (array.length == mElements.size()) {
                        for (int i = 0; i < array.length; ++i) {
                            FieldState fieldState = mElements.get(i);
                            if ((fieldState == null) || (!fieldState.isIdentityMatch(array[i]))) {
                                fieldState = mMemoryManager.storeObject(array[i], componentClass, noDeepRefresh);
                                mElements.set(i, fieldState);
                            } else if (!noDeepRefresh) {
                                fieldState.refreshInstance(false);
                            }
                        }
                    } else {
                        mElements.clear();
                        mElements.ensureCapacity(array.length);
                        for (int i = 0; i < array.length; ++i) {
                            FieldState fieldState = mMemoryManager.storeObject(array[i], componentClass, noDeepRefresh);
                            mElements.add(fieldState);
                        }
                    }
                } else if (componentClass == double.class) {
                    double[] array = (double[])instance;

                    if (array.length == mElements.size()) {
                        for (int i = 0; i < array.length; ++i) {
                            FieldState fieldState = mElements.get(i);
                            if ((fieldState == null) || (!fieldState.isIdentityMatch(array[i]))) {
                                fieldState = mMemoryManager.storeObject(array[i], componentClass, noDeepRefresh);
                                mElements.set(i, fieldState);
                            } else if (!noDeepRefresh) {
                                fieldState.refreshInstance(false);
                            }
                        }
                    } else {
                        mElements.clear();
                        mElements.ensureCapacity(array.length);
                        for (int i = 0; i < array.length; ++i) {
                            FieldState fieldState = mMemoryManager.storeObject(array[i], componentClass, noDeepRefresh);
                            mElements.add(fieldState);
                        }
                    }
                } else if (componentClass == char.class) {
                    char[] array = (char[])instance;

                    if (array.length == mElements.size()) {
                        for (int i = 0; i < array.length; ++i) {
                            FieldState fieldState = mElements.get(i);
                            if ((fieldState == null) || (!fieldState.isIdentityMatch(array[i]))) {
                                fieldState = mMemoryManager.storeObject(array[i], componentClass, noDeepRefresh);
                                mElements.set(i, fieldState);
                            } else if (!noDeepRefresh) {
                                fieldState.refreshInstance(false);
                            }
                        }
                    } else {
                        mElements.clear();
                        mElements.ensureCapacity(array.length);
                        for (int i = 0; i < array.length; ++i) {
                            FieldState fieldState = mMemoryManager.storeObject(array[i], componentClass, noDeepRefresh);
                            mElements.add(fieldState);
                        }
                    }
                } else {
                    // ERROR: unexpected primitive type!!!
                    Logging.error(LOG_TAG, "Unexpected primitive type for array: " + componentClass.getName());
                }
            }
        }
    }
    
    /**
     * Ping ArrayState to prevent garbage collection.
     */
    protected void internalPingInstance() {
        for (FieldState fieldState : mElements) {
            if (fieldState != null) {
                fieldState.pingInstance();
            }
        }
    }
    
    /**
     * Serialize ArrayState instance to XML.
     * 
     * @param xml XmlSerializer instance used as target for serialization.
     */
    @Override
    public void serializeToXml(XmlSerializer xml) {
        serializeToXml(xml, XmlSchemaPersistentMemory.TAG_FIELDSTATE_ARRAY);
    }
    
    /**
     * Serialize ArrayState instance to XML.
     * 
     * @param xml XmlSerializer instance used as target for serialization.
     */
    protected void serializeToXml(XmlSerializer xml, String enclosingTag) {
        try {
            xml.startTag(null, enclosingTag);
            
            super.serializeToXml(xml);
            
            xml.attribute(null, XmlSchemaPersistentMemory.ATTRIBUTE_ARRAY_ELEMENT_TYPE, mElementType);

            serializeBoundClassesToXml(xml);
            
            for (FieldState element : mElements) {
                xml.startTag(null, XmlSchemaPersistentMemory.TAG_ARRAY_ELEMENT);
                xml.attribute(null, XmlSchemaPersistentMemory.ATTRIBUTE_HASH_CODE, Long.toString((element != null) ? element.getHashCode() : UniqueObjectIdentifier.NULL_IDENTIFIER));
                xml.endTag(null, XmlSchemaPersistentMemory.TAG_ARRAY_ELEMENT);
            }

            xml.endTag(null, enclosingTag);
        } catch (Exception e) {
            Logging.error(LOG_TAG, "Exception while serializing to XML: " + e.toString(), e);
        }
    }
    
    /**
     * De-serialize ArrayState from XML.
     * 
     * @param memoryManager PersistentMemory instance that manages this FieldState hierarchy.
     * @param xml XmlPullParser instance used as source for de-serialization.
     * @param tag Currently processed tag.
     * @return Returns the de-serialized ArrayState instance, or null if tag does not match an
     *         ArrayState or the ArrayState could not be de-serialized.
     */
    public static DeserializedFieldState deserializeFromXml(PersistentMemory memoryManager, XmlPullParser xml, String tag) {
        DeserializedFieldState deserializationResult = new DeserializedFieldState();
        
        if (tag.equals(XmlSchemaPersistentMemory.TAG_FIELDSTATE_ARRAY)) {
            String hashCode = xml.getAttributeValue(null, XmlSchemaPersistentMemory.ATTRIBUTE_HASH_CODE);
            String fieldType = xml.getAttributeValue(null, XmlSchemaPersistentMemory.ATTRIBUTE_TYPE);
            
            String elementType = xml.getAttributeValue(null, XmlSchemaPersistentMemory.ATTRIBUTE_ARRAY_ELEMENT_TYPE);
            deserializationResult.hashCode = Long.valueOf(hashCode);
            deserializationResult.fieldState = new ArrayState(memoryManager, fieldType, deserializationResult.hashCode, elementType);

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

                    if (subTag.equals(XmlSchemaPersistentMemory.TAG_ARRAY_ELEMENT)) {
                        String fieldHashCode = xml.getAttributeValue(null, XmlSchemaPersistentMemory.ATTRIBUTE_HASH_CODE);
                        ((ArrayState)deserializationResult.fieldState).mElementReferences.add(Long.valueOf(fieldHashCode));
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

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
import java.lang.reflect.Array;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

/**
 *
 * @author mroland
 */
public class TransientArrayState extends ArrayState {
    private static final String LOG_TAG = "TransientArrayState";

    public TransientArrayState(PersistentMemory memoryManager,
                      Object referencedObject,
                      String elementType) {
        super(memoryManager, referencedObject, elementType);
    }

    public TransientArrayState(PersistentMemory memoryManager,
                      String recoveredObjectClass, Long recoveredIdentityHashCode,
                      String elementType) {
        super(memoryManager, recoveredObjectClass, recoveredIdentityHashCode, elementType);
    }

    /**
     * Restore array instance from de-serialized ArrayState.
     * 
     * @return Restored array object instance.
     */
    @Override
    protected Object restoreInstance() {
        final int numElements = mElements.size();

        if (boolean.class.getName().equals(mElementType)) {
            boolean[] array = new boolean[numElements];
            setInstanceRestored(array);

            return array;
        } else if (byte.class.getName().equals(mElementType)) {
            byte[] array = new byte[numElements];
            setInstanceRestored(array);

            return array;
        } else if (short.class.getName().equals(mElementType)) {
            short[] array = new short[numElements];
            setInstanceRestored(array);

            return array;
        } else if (int.class.getName().equals(mElementType)) {
            int[] array = new int[numElements];
            setInstanceRestored(array);

            return array;
        } else if (long.class.getName().equals(mElementType)) {
            long[] array = new long[numElements];
            setInstanceRestored(array);

            return array;
        } else if (float.class.getName().equals(mElementType)) {
            float[] array = new float[numElements];
            setInstanceRestored(array);

            return array;
        } else if (double.class.getName().equals(mElementType)) {
            double[] array = new double[numElements];
            setInstanceRestored(array);

            return array;
        } else if (char.class.getName().equals(mElementType)) {
            char[] array = new char[numElements];
            setInstanceRestored(array);

            return array;
        } else {
            Object[] array = (Object[])Array.newInstance(getElementClass(), numElements);
            setInstanceRestored(array);

            return array;
        }
    }

    /**
     * Revert array instance to reflect image stored in ArrayState.
     */
    @Override
    protected void internalRevertInstance() {
        Object instance = getInstance();
        
        if (instance == null) {
            Logging.error(LOG_TAG, "Trying to revert TransientArrayState that has not been created!");
        }
    }

    /**
     * Refresh image stored in ArrayState to reflect current array.
     * 
     * @param noDeepRefresh Do not recursively refresh the state of existing objects.
     */
    @Override
    protected void internalRefreshInstance(boolean noDeepRefresh) {
        Object instance = getInstance();
        
        if (instance == null) {
            Logging.error(LOG_TAG, "Trying to refresh TransientArrayState that has not been created!");
        } else {
            Class componentClass = getElementClass();

            if (componentClass == null) {
                Logging.error(LOG_TAG, "" + getHashCode() + ": Unexpected component class: null");
            } else {
                Logging.debug(LOG_TAG, "" + getHashCode() + ": Array of " + componentClass.getName());

                if (!componentClass.isPrimitive()) {
                    Object[] array = (Object[])instance;
                    
                    if (array.length != mElements.size()) {
                        mElements.clear();
                        mElements.ensureCapacity(array.length);
                        for (int i = 0; i < array.length; ++i) {
                            FieldState fieldState = mMemoryManager.storeObject(null, componentClass, noDeepRefresh);
                            mElements.add(fieldState);
                        }
                    }
                } else if (componentClass == boolean.class) {
                    boolean[] array = (boolean[])instance;

                    if (array.length != mElements.size()) {
                        mElements.clear();
                        mElements.ensureCapacity(array.length);
                        for (int i = 0; i < array.length; ++i) {
                            FieldState fieldState = mMemoryManager.storeObject(false, componentClass, noDeepRefresh);
                            mElements.add(fieldState);
                        }
                    }
                } else if (componentClass == byte.class) {
                    byte[] array = (byte[])instance;

                    if (array.length != mElements.size()) {
                        mElements.clear();
                        mElements.ensureCapacity(array.length);
                        for (int i = 0; i < array.length; ++i) {
                            FieldState fieldState = mMemoryManager.storeObject((byte)0, componentClass, noDeepRefresh);
                            mElements.add(fieldState);
                        }
                    }
                } else if (componentClass == short.class) {
                    short[] array = (short[])instance;

                    if (array.length != mElements.size()) {
                        mElements.clear();
                        mElements.ensureCapacity(array.length);
                        for (int i = 0; i < array.length; ++i) {
                            FieldState fieldState = mMemoryManager.storeObject((short)0, componentClass, noDeepRefresh);
                            mElements.add(fieldState);
                        }
                    }
                } else if (componentClass == int.class) {
                    int[] array = (int[])instance;

                    if (array.length != mElements.size()) {
                        mElements.clear();
                        mElements.ensureCapacity(array.length);
                        for (int i = 0; i < array.length; ++i) {
                            FieldState fieldState = mMemoryManager.storeObject((int)0, componentClass, noDeepRefresh);
                            mElements.add(fieldState);
                        }
                    }
                } else if (componentClass == long.class) {
                    long[] array = (long[])instance;

                    if (array.length != mElements.size()) {
                        mElements.clear();
                        mElements.ensureCapacity(array.length);
                        for (int i = 0; i < array.length; ++i) {
                            FieldState fieldState = mMemoryManager.storeObject((long)0, componentClass, noDeepRefresh);
                            mElements.add(fieldState);
                        }
                    }
                } else if (componentClass == float.class) {
                    float[] array = (float[])instance;

                    if (array.length != mElements.size()) {
                        mElements.clear();
                        mElements.ensureCapacity(array.length);
                        for (int i = 0; i < array.length; ++i) {
                            FieldState fieldState = mMemoryManager.storeObject((float)0.0, componentClass, noDeepRefresh);
                            mElements.add(fieldState);
                        }
                    }
                } else if (componentClass == double.class) {
                    double[] array = (double[])instance;

                    if (array.length != mElements.size()) {
                        mElements.clear();
                        mElements.ensureCapacity(array.length);
                        for (int i = 0; i < array.length; ++i) {
                            FieldState fieldState = mMemoryManager.storeObject((double)0.0, componentClass, noDeepRefresh);
                            mElements.add(fieldState);
                        }
                    }
                } else if (componentClass == char.class) {
                    char[] array = (char[])instance;

                    if (array.length != mElements.size()) {
                        mElements.clear();
                        mElements.ensureCapacity(array.length);
                        for (int i = 0; i < array.length; ++i) {
                            FieldState fieldState = mMemoryManager.storeObject((char)0, componentClass, noDeepRefresh);
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
     * Serialize ArrayState instance to XML.
     * 
     * @param xml XmlSerializer instance used as target for serialization.
     */
    @Override
    public void serializeToXml(XmlSerializer xml) {
        serializeToXml(xml, XmlSchemaPersistentMemory.TAG_FIELDSTATE_TRANSIENT_ARRAY);
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
        
        if (tag.equals(XmlSchemaPersistentMemory.TAG_FIELDSTATE_TRANSIENT_ARRAY)) {
            String hashCode = xml.getAttributeValue(null, XmlSchemaPersistentMemory.ATTRIBUTE_HASH_CODE);
            String fieldType = xml.getAttributeValue(null, XmlSchemaPersistentMemory.ATTRIBUTE_TYPE);
            
            String elementType = xml.getAttributeValue(null, XmlSchemaPersistentMemory.ATTRIBUTE_ARRAY_ELEMENT_TYPE);
            deserializationResult.hashCode = Long.valueOf(hashCode);
            deserializationResult.fieldState = new TransientArrayState(memoryManager, fieldType, deserializationResult.hashCode, elementType);

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
                        ((TransientArrayState)deserializationResult.fieldState).mElementReferences.add(Long.valueOf(fieldHashCode));
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

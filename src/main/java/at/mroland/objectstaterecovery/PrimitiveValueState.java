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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

/**
 *
 * @author mroland
 */
public class PrimitiveValueState extends FieldState {
    private static final String LOG_TAG = "PrimitiveValueState";
    
    private static enum ValueType {
        eNull,
        eBoolean,
        eByte,
        eShort,
        eInteger,
        eLong,
        eBigInteger,
        eFloat,
        eDouble,
        eBigDecimal,
        eCharacter,
        eString,
        eEnum,
        eClass,
        eUnknownPrimitive,
        eUnknown
    }
    private String mValue;
    private final ValueType mValueType;
    private Object mRecoveredObject;

    public PrimitiveValueState(PersistentMemory memoryManager,
                               Object referencedObject, Class referencedObjectClass) {
        super(memoryManager, getBoxedPrimitiveValue(referencedObject, getPrimitiveValueType(referencedObject, referencedObjectClass)));
        mValueType = getPrimitiveValueType(referencedObject, referencedObjectClass);
        mValue = getPrimitiveValue(referencedObject, mValueType);
    }
    
    public static PrimitiveValueState getInstance(PersistentMemory memoryManager,
                                                  Object referencedObject, Class referencedObjectClass) {
        ValueType valueType = getPrimitiveValueType(referencedObject, referencedObjectClass);
        String value = getPrimitiveValue(referencedObject, valueType);
        
        Object newObjectInstance = getSingleInstancePrimitiveFromObject(referencedObject, valueType);
        Long newIdentityHashCode = getObjectIdentityHashCode(newObjectInstance);
        
        FieldState instance = memoryManager.getReference(newIdentityHashCode);
        
        if (instance == null) {
            instance = new PrimitiveValueState(memoryManager, newObjectInstance, referencedObjectClass);
            instance.refreshInstance(false);
        } else if (!(instance instanceof PrimitiveValueState)) {
            instance = null;
            Logging.error(LOG_TAG, "Found instance with duplicate hash code of primitive value that is not an instance of PrimitiveValueState (#" + newIdentityHashCode + ")");
        } else if ((referencedObjectClass != null) && !referencedObjectClass.getName().equals(instance.getFieldType())) {
            Logging.error(LOG_TAG, "Found instance of different primitive value type: " + value + " [" + referencedObjectClass.getName() + ", " + valueType + ", #" + newIdentityHashCode + ", was #unknown] vs. " + instance.getInstance() + " [" + instance.getFieldType() + ", " + ((PrimitiveValueState)instance).mValueType + ", #" + instance.getHashCode() + "]");
        } else {
            Logging.debug(LOG_TAG, "Re-using PrimitiveValueState (new #" + newIdentityHashCode + ", was #unknown)");
        }
        
        return (PrimitiveValueState)instance;
    }

    protected PrimitiveValueState(PersistentMemory memoryManager,
                               Object recoveredObject, String recoveredObjectClass, Long recoveredIdentityHashCode,
                               String valueType, String value) {
        super(memoryManager, recoveredObjectClass, recoveredIdentityHashCode);
        mValueType = ValueType.valueOf(valueType);
        mValue = value;
        mRecoveredObject = recoveredObject;
    }

    public static PrimitiveValueState getInstance(PersistentMemory memoryManager,
                               String recoveredObjectClass, Long recoveredIdentityHashCode,
                               String valueType, String value) {
        Object newObjectInstance = getRestoredPrimitiveFromString(value, ValueType.valueOf(valueType), recoveredObjectClass);
        Long newIdentityHashCode = getObjectIdentityHashCode(newObjectInstance);
        
        FieldState instance = memoryManager.getReference(newIdentityHashCode);
        
        if (instance == null) {
            instance = new PrimitiveValueState(memoryManager, newObjectInstance, recoveredObjectClass, recoveredIdentityHashCode, valueType, value);
            instance.getInstance();
        } else if (!(instance instanceof PrimitiveValueState)) {
            instance = null;
            Logging.error(LOG_TAG, "Found instance with duplicate hash code of primitive value that is not an instance of PrimitiveValueState (#" + newIdentityHashCode + ")");
        } else if (!recoveredObjectClass.equals(instance.getFieldType())) {
            Logging.error(LOG_TAG, "Found instance of different primitive value type: " + value + " [" + recoveredObjectClass + ", " + valueType + ", #" + newIdentityHashCode + ", was #" + recoveredIdentityHashCode + "] vs. " + instance.getInstance() + " [" + instance.getFieldType() + ", " + ((PrimitiveValueState)instance).mValueType + ", #" + instance.getHashCode() + "]");
        } else {
            Logging.debug(LOG_TAG, "Re-using PrimitiveValueState (new #" + newIdentityHashCode + ", was #" + recoveredIdentityHashCode + ")");
        }
        
        return (PrimitiveValueState)instance;
    }
    
    /**
     * Test if a given class represents a primitive value type handled
     * by this class. Besides primitive data types also their boxes and
     * some other immutable types are covered.
     * 
     * @param c Class.
     * @return true if <code>c</code> represents a primitive data type.
     */
    public static boolean isPrimitiveValueType(Class c) {
        if (c == null) {
            return false;
        }

        if (c.isPrimitive()) {
            return true;
        } else {
            if (c == Boolean.class) {
                return true;
            } else if (c == Byte.class) {
                return true;
            } else if (c == Short.class) {
                return true;
            } else if (c == Integer.class) {
                return true;
            } else if (c == Long.class) {
                return true;
            } else if (c == BigInteger.class) {
                return true;
            } else if (c == Float.class) {
                return true;
            } else if (c == Double.class) {
                return true;
            } else if (c == BigDecimal.class) {
                return true;
            } else if (c == Character.class) {
                return true;
            } else if (c == String.class) {
                return true;
            } else if (c == Enum.class) {
                return true;
            } else if (c.isEnum()) {
                return true;
            } else if (c == Class.class) {
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Get the primitive value type for a given primitive object.
     * 
     * @param o Primitive value object.
     * @param c Primitive value class (required only for boxed primitive data types).
     * @return Primitive value type.
     */
    private static ValueType getPrimitiveValueType(Object o, Class c) {
        if (o == null) {
            return ValueType.eNull;
        }
        if (c == null) {
            c = o.getClass();
        }

        if (c.isPrimitive()) {
            if (c == boolean.class) {
                return ValueType.eBoolean;
            } else if (c == byte.class) {
                return ValueType.eByte;
            } else if (c == short.class) {
                return ValueType.eShort;
            } else if (c == int.class) {
                return ValueType.eInteger;
            } else if (c == long.class) {
                return ValueType.eLong;
            } else if (c == float.class) {
                return ValueType.eFloat;
            } else if (c == double.class) {
                return ValueType.eDouble;
            } else if (c == char.class) {
                return ValueType.eCharacter;
            } else {
                return ValueType.eUnknownPrimitive;
            }
        } else {
            if (c == Boolean.class) {
                return ValueType.eBoolean;
            } else if (c == Byte.class) {
                return ValueType.eByte;
            } else if (c == Short.class) {
                return ValueType.eShort;
            } else if (c == Integer.class) {
                return ValueType.eInteger;
            } else if (c == Long.class) {
                return ValueType.eLong;
            } else if (c == BigInteger.class) {
                return ValueType.eBigInteger;
            } else if (c == Float.class) {
                return ValueType.eFloat;
            } else if (c == Double.class) {
                return ValueType.eDouble;
            } else if (c == BigDecimal.class) {
                return ValueType.eBigDecimal;
            } else if (c == Character.class) {
                return ValueType.eCharacter;
            } else if (c == String.class) {
                return ValueType.eString;
            } else if ((c == Enum.class) || c.isEnum()) {
                return ValueType.eEnum;
            } else if (c == Class.class) {
                return ValueType.eClass;
            } else {
                return ValueType.eUnknown;
            }
        }
    }

    /**
     * Get the value of a given primitive object as a string.
     * 
     * @param o Primitive value object.
     * @param type Primitive value type.
     * @return String representation of primitive value.
     */
    private static String getPrimitiveValue(Object o, ValueType type) {
        if ((o == null) || (type == null)) {
            return null;
        }

        switch (type) {
            case eBoolean:
                return Boolean.toString((Boolean)o);
            case eByte:
                return Byte.toString((Byte)o);
            case eShort:
                return Short.toString((Short)o);
            case eInteger:
                return Integer.toString((Integer)o);
            case eLong:
                return Long.toString((Long)o);
            case eBigInteger:
                return ((BigInteger)o).toString();
            case eFloat:
                return Float.toString((Float)o);
            case eDouble:
                return Double.toString((Double)o);
            case eBigDecimal:
                return ((BigDecimal)o).toString();
            case eCharacter:
                return Long.toString((long)((Character)o).charValue());
            case eString:
                return (String)o;
            case eEnum:
                return ((Enum)o).name();
            case eClass:
                return ((Class)o).getName();
            default:
                return null;
        }
    }

    /**
     * Get the box Object for a given primitive value as a string.
     * 
     * @param o Primitive value object.
     * @param type Primitive value type.
     * @return String representation of primitive value.
     */
    private static Object getBoxedPrimitiveValue(Object o, ValueType type) {
        if ((o == null) || (type == null)) {
            return o;
        }

        switch (type) {
            case eBoolean:
                return (Boolean)o;
            case eByte:
                return (Byte)o;
            case eShort:
                return (Short)o;
            case eInteger:
                return (Integer)o;
            case eLong:
                return (Long)o;
            case eFloat:
                return (Float)o;
            case eDouble:
                return (Double)o;
            case eCharacter:
                return (Character)o;
            default:
                return o;
        }
    }
    
    private static Object getRestoredPrimitiveFromString(String value, ValueType type, String objectClassName) {
        Object newInstance = null;
        try {
            switch (type) {
                case eBoolean:
                    newInstance = Boolean.valueOf(Boolean.valueOf(value));
                    break;
                case eByte:
                    newInstance = Byte.valueOf(Byte.valueOf(value));
                    break;
                case eShort:
                    newInstance = Short.valueOf(Short.valueOf(value));
                    break;
                case eInteger:
                    newInstance = Integer.valueOf(Integer.valueOf(value));
                    break;
                case eLong:
                    newInstance = Long.valueOf(Long.valueOf(value));
                    break;
                case eBigInteger:
                    newInstance = new BigInteger(value);
                    break;
                case eFloat:
                    newInstance = Float.valueOf(Float.valueOf(value));
                    break;
                case eDouble:
                    newInstance = Double.valueOf(Double.valueOf(value));
                    break;
                case eBigDecimal:
                    newInstance = new BigDecimal(value);
                    break;
                case eCharacter:
                    newInstance = Character.valueOf((char)Long.parseLong(value));
                    break;
                case eString:
                    newInstance = value;
                    break;
                case eEnum:
                    try {
                        Class objectClass = Class.forName(objectClassName);
                        if (objectClass != null) {
                            newInstance = Enum.valueOf(objectClass, value);
                        }
                    } catch (Exception e) {
                        Logging.error(LOG_TAG, "Could not get class by name " + objectClassName, e);
                    }
                    break;
                case eClass:
                    try {
                        if (boolean.class.getName().equals(value)) {
                            newInstance = boolean.class;
                        } else if (byte.class.getName().equals(value)) {
                            newInstance = byte.class;
                        } else if (short.class.getName().equals(value)) {
                            newInstance = short.class;
                        } else if (int.class.getName().equals(value)) {
                            newInstance = int.class;
                        } else if (long.class.getName().equals(value)) {
                            newInstance = long.class;
                        } else if (float.class.getName().equals(value)) {
                            newInstance = float.class;
                        } else if (double.class.getName().equals(value)) {
                            newInstance = double.class;
                        } else if (char.class.getName().equals(value)) {
                            newInstance = char.class;
                        } else {
                            newInstance = Class.forName(value);
                        }
                    } catch (Exception e) {
                        Logging.error(LOG_TAG, "Could not get class by name " + value, e);
                    }
                    break;
                default:
                    break;
            }
        } catch (NumberFormatException e) {
            Logging.error(LOG_TAG, "Tried to load value that is not a number: " + e.toString(), e);
        } catch (IllegalArgumentException e) {
            Logging.error(LOG_TAG, "Tried to load value that does not fit type: " + e.toString(), e);
        } catch (NullPointerException e) {
            Logging.error(LOG_TAG, "Error loading value: " + e.toString(), e);
        }

        if (newInstance == null) {
            Logging.error(LOG_TAG, "Error restoring primitive value: " + value + " (" + type + ", " + objectClassName + ")");
        }
        
        return newInstance;
    }
    
    private static Object getSingleInstancePrimitiveFromObject(Object o, ValueType type) {
        Object newInstance = null;
        try {
            switch (type) {
                case eBoolean:
                    newInstance = Boolean.valueOf((Boolean)o);
                    break;
                case eByte:
                    newInstance = Byte.valueOf((Byte)o);
                    break;
                case eShort:
                    newInstance = Short.valueOf((Short)o);
                    break;
                case eInteger:
                    newInstance = Integer.valueOf((Integer)o);
                    break;
                case eLong:
                    newInstance = Long.valueOf((Long)o);
                    break;
                case eBigInteger:
                    newInstance = (BigInteger)o;
                    break;
                case eFloat:
                    newInstance = Float.valueOf((Float)o);
                    break;
                case eDouble:
                    newInstance = Double.valueOf((Double)o);
                    break;
                case eBigDecimal:
                    newInstance = (BigDecimal)o;
                    break;
                case eCharacter:
                    newInstance = Character.valueOf((Character)o);
                    break;
                case eString:
                    newInstance = (String)o;
                    break;
                case eEnum:
                    if (o != null) {
                        Class objectClass = o.getClass();
                        if (objectClass != null) {
                            try {
                                newInstance = Enum.valueOf(objectClass, ((Enum)o).name());
                            } catch (Exception e) {
                                Logging.error(LOG_TAG, "Could not get enum value by name " + objectClass.getName(), e);
                            }
                        }
                    }
                    break;
                case eClass:
                    newInstance = (Class)o;
                    break;
                default:
                    newInstance = o;
                    break;
            }
//        } catch (NumberFormatException e) {
//            Logging.error(LOG_TAG, "Tried to load value that is not a number: " + e.toString(), e);
        } catch (IllegalArgumentException e) {
            Logging.error(LOG_TAG, "Tried to load value that does not fit type: " + e.toString(), e);
        } catch (NullPointerException e) {
            Logging.error(LOG_TAG, "Error loading value: " + e.toString(), e);
        }

        if ((newInstance == null) && (newInstance != o)) {
            Logging.error(LOG_TAG, "Error restoring primitive value: " + o + " (" + type + ", " + o.getClass().getName() + ")");
        }
        
        return newInstance;
    }
    
    /**
     * Get unique hash code for given primitive value.
     * 
     * @param o An object.
     * @return Unique identity hash code for <code>o</code>.
     */
    public static Long getPrimitiveValueIdentityHashCode(Object o) {
        ValueType valueType = getPrimitiveValueType(o, null);
        Object singleObjectInstance = getSingleInstancePrimitiveFromObject(o, valueType);
        return Long.valueOf(UniqueObjectIdentifier.get(singleObjectInstance));
    }
    
    /**
     * Recreates the links between this FieldState instance and its
     * hierarchical sub-FieldState instances during de-serialization.
     * 
     * Implementation does nothing as primitives do not refer to sub-FieldStates.
     * 
     * @param deserializedReferenceMap Mapping of de-serialized reference hash codes to corresponding FieldStates.
     */
    @Override
    /* package */ void relinkReferences(Map<Long, FieldState> deserializedReferenceMap) {
        super.relinkReferences(deserializedReferenceMap);
    }

    /**
     * Load primitive value to a parent objects member field.
     * 
     * @param field Member field.
     * @param parentInstance Parent object.
     * @param instance Object instance.
     * @throws Exception 
     */
    @Override
    protected void loadInstanceToField(Field field, Object parentInstance, Object instance) throws Exception {
        if (field.getType().isPrimitive()) {
            switch (mValueType) {
                case eBoolean:
                    field.setBoolean(parentInstance, ((Boolean)instance).booleanValue());
                    break;
                case eByte:
                    field.setByte(parentInstance, ((Byte)instance).byteValue());
                    break;
                case eShort:
                    field.setShort(parentInstance, ((Short)instance).shortValue());
                    break;
                case eInteger:
                    field.setInt(parentInstance, ((Integer)instance).intValue());
                    break;
                case eLong:
                    field.setLong(parentInstance, ((Long)instance).longValue());
                    break;
                case eFloat:
                    field.setFloat(parentInstance, ((Float)instance).floatValue());
                    break;
                case eDouble:
                    field.setDouble(parentInstance, ((Double)instance).doubleValue());
                    break;
                case eCharacter:
                    field.setChar(parentInstance, ((Character)instance).charValue());
                    break;
                default:
                    super.loadInstanceToField(field, parentInstance, instance);
                    break;
            }
        } else {
            super.loadInstanceToField(field, parentInstance, instance);
        }
    }
    
    /**
     * Restore object instance from de-serialized PrimitiveValueState.
     * 
     * @return Restored object instance.
     */
    protected Object restoreInstance() {
        Object newInstance = mRecoveredObject;

        if (newInstance == null) {
            Logging.error(LOG_TAG, "Error restoring primitive value: " + mValue + " (" + mValueType + ")");
        }
        
        setInstanceRestored(newInstance);
        return newInstance;
    }

    /**
     * Revert object to reflect image stored in FieldState.
     * 
     * Implementation does nothing as primitive values are immutable. (I.e. a changed
     * value would result into use of a different FieldState instance and would not
     * cause reverting.)
     */
    protected void internalRevertInstance() {
    }
    
    /**
     * Refresh image stored in PrimitiveValueState to reflect current primitive value.
     * 
     * Implementation does nothing as primitive values are immutable. (I.e. a changed
     * value would result into use of a different FieldState instance and would not
     * cause refreshing.)
     * 
     * @param noDeepRefresh Do not recursively refresh the state of existing objects.
     */
    protected void internalRefreshInstance(boolean noDeepRefresh) {
    }

    /**
     * Ping PrimitiveValueState to prevent garbage collection.
     * 
     * Implementation does nothing as primitive values are immutable.
     */
    protected void internalPingInstance() {
    }
    
    /**
     * Serialize PrimitiveValueState instance to XML.
     * 
     * @param xml XmlSerializer instance used as target for serialization.
     */
    @Override
    public void serializeToXml(XmlSerializer xml) {
        try {
            xml.startTag(null, XmlSchemaPersistentMemory.TAG_FIELDSTATE_PRIMITIVE);

            super.serializeToXml(xml);

            serializeBoundClassesToXml(xml);
            
            xml.startTag(null, XmlSchemaPersistentMemory.TAG_PRIMITIVE_VALUE);
            xml.attribute(null, XmlSchemaPersistentMemory.ATTRIBUTE_PRIMITIVE_VALUE_TYPE, mValueType.name());
            xml.text(mValue);
            xml.endTag(null, XmlSchemaPersistentMemory.TAG_PRIMITIVE_VALUE);

            xml.endTag(null, XmlSchemaPersistentMemory.TAG_FIELDSTATE_PRIMITIVE);
        } catch (Exception e) {
            Logging.error(LOG_TAG, "Exception while serializing to XML: " + e.toString(), e);
        }
    }
    
    /**
     * De-serialize PrimitiveValueState from XML.
     * 
     * @param memoryManager PersistentMemory instance that manages this FieldState hierarchy.
     * @param xml XmlPullParser instance used as source for de-serialization.
     * @param tag Currently processed tag.
     * @return Returns the de-serialized PrimitiveValueState instance, or null if tag does
     *         not match a PrimitiveValueState or the PrimitiveValueState could not be de-serialized.
     */
    public static DeserializedFieldState deserializeFromXml(PersistentMemory memoryManager, XmlPullParser xml, String tag) {
        DeserializedFieldState deserializationResult = new DeserializedFieldState();
        
        if (tag.equals(XmlSchemaPersistentMemory.TAG_FIELDSTATE_PRIMITIVE)) {
            String hashCodeStr = xml.getAttributeValue(null, XmlSchemaPersistentMemory.ATTRIBUTE_HASH_CODE);
            String fieldType = xml.getAttributeValue(null, XmlSchemaPersistentMemory.ATTRIBUTE_TYPE);

            deserializationResult.hashCode = Long.valueOf(hashCodeStr);
            
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

                    if (subTag.equals(XmlSchemaPersistentMemory.TAG_PRIMITIVE_VALUE)) {
                        String fieldPrimitiveType = xml.getAttributeValue(null, XmlSchemaPersistentMemory.ATTRIBUTE_PRIMITIVE_VALUE_TYPE);
                        String fieldPrimitiveValue = "";
                        try {
                            fieldPrimitiveValue = xml.nextText();
                        } catch (Exception e) {
                            Logging.error(LOG_TAG, "Exception while de-serializing from XML: " + e.toString(), e);
                        }
                        deserializationResult.fieldState = getInstance(memoryManager, fieldType, deserializationResult.hashCode, fieldPrimitiveType, fieldPrimitiveValue);
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

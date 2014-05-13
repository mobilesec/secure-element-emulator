/*
 * Copyright 2014 Licel LLC.
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

/**
 *
 * @author mroland
 */
public interface XmlSchemaPersistentMemory {
    public static final String URI = "http://mroland.at/xml/persistentmemory";
    
    public static final String TAG_ROOT = "PersistentMemory";
    
    // general attributes
    public static final String ATTRIBUTE_HASH_CODE = "hashCode";
    public static final String ATTRIBUTE_NAME = "name";
    public static final String ATTRIBUTE_TYPE = "type";
    
    // FieldStates
    public static final String TAG_REFERENCES = "References";
    public static final String TAG_FIELDSTATE_OBJECT = "ObjectReferenceState";
    public static final String TAG_FIELDSTATE_ARRAY = "ArrayState";
    public static final String TAG_FIELDSTATE_TRANSIENT_ARRAY = "TransientArrayState";
    public static final String TAG_FIELDSTATE_PRIMITIVE = "PrimitiveValueState";
    
    // Class bound to FieldState
    public static final String TAG_BOUNDCLASS = "BoundClass";
    
    // PrimitiveValue
    public static final String TAG_PRIMITIVE_VALUE = "Value";
    
    // PrimitiveValue attributes
    public static final String ATTRIBUTE_PRIMITIVE_VALUE_TYPE = "primitiveType";
    
    // Array
    public static final String TAG_ARRAY_ELEMENT = "Element";

    // Array attributes
    public static final String ATTRIBUTE_ARRAY_ELEMENT_TYPE = "elementType";
    
    // Object
    public static final String TAG_OBJECT_FIELD = "Field";

    // ClassState
    public static final String TAG_CLASSES = "Classes";
    public static final String TAG_CLASSSTATE_CLASS = "ClassState";

    // NamedInstances
    public static final String TAG_NAMED_INSTANCES = "NamedInstances";
    public static final String TAG_NAMED_INSTANCE = "NamedInstance";
}

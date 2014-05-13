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
public interface XmlSchemaTransientMemory {
    public static final String URI = "http://mroland.at/xml/transientmemory";
    
    public static final String TAG_ROOT = "TransientMemory";
    
    // general attributes
    public static final String ATTRIBUTE_AID = "aid";
    public static final String ATTRIBUTE_HASH_CODE = "hashCode";
    
    // Segments
    public static final String TAG_SEGMENT_CLEARONDESELECT = "ClearOnDeselect";
    public static final String TAG_SEGMENT_CLEARONRESET = "ClearOnReset";
    
    // Reference
    public static final String TAG_REFERENCE = "Reference";
}

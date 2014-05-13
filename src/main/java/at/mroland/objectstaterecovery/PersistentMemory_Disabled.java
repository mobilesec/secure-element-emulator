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
import javacard.framework.AID;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

/**
 * Basic implementation of storage persistent memory of JCRE
 */
public class PersistentMemory_Disabled extends PersistentMemory {
    private static final String LOG_TAG = "PersistentMemory_Disabled";
  
    /**
     * Register an object instance that must never be included in persistent
     * memory. References to such objects will be replaced with null references
     * during state collection.
     * 
     * @param object Object to be excluded.
     */
    public void addProhibitedReference(Object object) {
    }

    /**
     * Register class that must never be included in persistent memory.
     * Requests to record the state of these classes' static members will
     * be ignored.
     * 
     * @param classObject Class to be excluded.
     */
    public void addProhibitedClass(Class classObject) {
    }

    /**
     * Register class that must never be included in persistent memory.
     * Requests to record the state of these classes' static members will
     * be ignored.
     * 
     * @param className Class to be excluded (given by name).
     */
    public void addProhibitedClass(String className) {
    }

    /**
     * Add or refresh a stored class.
     * 
     * NOTE: Prohibited classes will be silently ignored.
     * 
     * @param classObject Class object.
     * @param noDeepRefresh Do not recursively refresh the state of existing objects.
     */
    public void updateStoredClass(Class classObject, boolean noDeepRefresh) {
    }
    
    /**
     * Add or refresh a stored class by name.
     * 
     * NOTE: Prohibited classes will be silently ignored.
     * 
     * @param className Class name.
     * @param noDeepRefresh Do not recursively refresh the state of existing objects.
     */
    public void updateStoredClass(String className, boolean noDeepRefresh) {
    }

    /**
     * Add or refresh a stored named object instance.
     * 
     * NOTE: Prohibited objects will be silently ignored.
     * 
     * @param object Object instance.
     * @param objectClass Object instance class (may be null).
     * @param aid Object instance name as AID.
     * @param noDeepRefresh Do not recursively refresh the state of existing objects.
     */
    public void updateStoredNamedInstance(Object object, Class objectClass, AID aid, boolean noDeepRefresh) {
    }
    
    /**
     * Add or refresh a stored named object instance.
     * 
     * NOTE: Prohibited objects will be silently ignored.
     * 
     * @param object Object instance.
     * @param objectClass Object instance class (may be null).
     * @param instanceName Object instance name.
     * @param noDeepRefresh Do not recursively refresh the state of existing objects.
     */
    public void updateStoredNamedInstance(Object object, Class objectClass, String instanceName, boolean noDeepRefresh) {
    }
    
    /**
     * Add or refresh a stored object reference.
     * 
     * NOTE: Prohibited objects will be silently ignored.
     * 
     * @param object Object instance.
     * @param objectClass Object instance class (may be null).
     * @param noDeepRefresh Do not recursively refresh the state of existing objects.
     */
    public void updateStoredReference(Object object, Class objectClass, boolean noDeepRefresh) {
    }
    
    /**
     * Store class instance based on class name in this persistent memory manager.
     * 
     * @param className Class name.
     * @param noDeepRefresh Do not recursively refresh the state of existing objects.
     * @return {@link ClassState} instance containing the stored class state.
     */
    /* package */ ClassState storeClass(String className, boolean noDeepRefresh) {
        return null;
    }

    /**
     * Store class instance in this persistent memory manager.
     * 
     * @param classObject Class object instance.
     * @param noDeepRefresh Do not recursively refresh the state of existing objects.
     * @return {@link ClassState} instance containing the stored class state.
     */
    /* package */ ClassState storeClass(Class classObject, boolean noDeepRefresh) {
        return null;
    }

    /**
     * Store object instance in this persistent memory manager.
     * 
     * @param object Object instance.
     * @param paramObjectClass Object class (required only for (boxed) primitives).
     * @return {@link FieldState} instance containing the stored object state.
     */
    /* package */ FieldState storeObject(Object object, Class paramObjectClass, boolean noDeepRefresh) {
        return null;
    }

    /**
     * Store transient array object instance in this persistent memory manager.
     * 
     * @param object Array object instance.
     * @return {@link FieldState} instance containing the stored array object state.
     */
    /* package */ FieldState storeTransientArray(Object object) {
        FieldState instance = null;
        
        Class objectClass = (object != null) ? object.getClass() : null;

        if ((object == null) || (objectClass == null)) {
            instance = new ObjectReferenceState(this, object);
        } else if (objectClass.isArray()) {
            Class componentClass = objectClass.getComponentType();

            if (componentClass == null) {
                Logging.error(LOG_TAG, "Unexpected component class: null");
            } else {
                instance = new TransientArrayState(this, object, componentClass.getName());
            }
        }

        return instance;
    }
    
    /**
     * Return the currently valid refresh tag.
     * 
     * @return Current refresh tag value.
     */
    /* package */ int getCurrentRefreshTag() {
        return 0;
    }

    public void bindClassToObject(Class classObject, Object instance) {
    }
    
    public void bindClassToObject(String className, Object instance) {
    }
    
    public void bindClassToNamedInstance(Class classObject, String instanceName) {
    }
    
    public void bindClassToNamedInstance(String className, String instanceName) {
    }
    
    public void bindClassToNamedInstance(Class classObject, AID aid) {
    }
    
    public void bindClassToNamedInstance(String className, AID aid) {
    }
    
    public void setDirtyClass(Class classObject) {
    }
    
    public void setDirtyClass(String className) {
    }

    public void setDirtyReference(Object object) {
    }
    
    public void setDirtyNamedInstance(String instanceName) {
    }
    
    public void setDirtyNamedInstance(AID aid) {
    }
    
    public void memoryBarrier(boolean revertMemory) {
        if (revertMemory) {
            Logging.error(LOG_TAG, "Reverting not supported if no persistence is used!");
        }
    }
    
    public void clearDirtyFlags() {
    }
    
    public void pushDirtyFlags() {
    }
    
    public void popDirtyFlags() {
    }
    
    
    /**
     * Add entry to list of current references.
     * 
     * @param hashCode Current hash code.
     * @param fieldState Current field state.
     */
    /* package */ void addReference(Long hashCode, FieldState fieldState) {
    }

    /**
     * Get FieldState based on current hash code reference.
     * 
     * @param hashCode Current hash code.
     */
    /* package */ FieldState getReference(Long hashCode) {
        return null;
    }
    
    /**
     * Get ClassState based on class object.
     * 
     * @param classObject Class object.
     */
    /* package */ ClassState getClass(Class classObject) {
        return null;
    }
    
    /**
     * Get ClassState based on class name.
     * 
     * @param className Class name.
     */
    /* package */ ClassState getClass(String className) {
        return null;
    }
    
    /**
     * Get FieldState based on de-serialized hash code reference.
     * 
     * @param hashCode Current hash code.
     */
    /* package */ FieldState getDeserializedReference(Long hashCode) {
        return null;
    }
    
    public Object getNamedInstance(String instanceName) {
        return null;
    }
    
    public Object getNamedInstance(AID aid) {
        return null;
    }

    /**
     * Add entry to list of classes.
     * 
     * @param className Class name.
     * @param classState Current class state.
     */
    /* package */ void addClass(String className, ClassState classState) {
    }

    public boolean isProhibitedReference(Long hashCode) {
        return false;
    }
    
    /**
     * Request garbage collection of unused references.
     */
    public void garbageCollect(boolean includeTransientMemory) {
    }
    
    /**
     * Reset persistent object storage.
     */
    public void reset(boolean resetProhibitedList) {
    }
    
    /**
     * Serialize persistent memory to XML.
     * 
     * @param xml XmlSerializer instance used as target for serialization.
     */
    public void serializeToXml(XmlSerializer xml) {
    }

    /**
     * De-serialize persistent memory from XML.
     * 
     * @param xml XmlPullParser instance used as source for de-serialization.
     */
    public void deserializeFromXml(XmlPullParser xml) {
    }
}

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
import com.licel.jcardsim.base.SimulatorSystem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import javacard.framework.AID;
import javacard.framework.__AIDWrapper;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

/**
 * Basic implementation of storage persistent memory of JCRE
 */
public class PersistentMemory {
    private static final String LOG_TAG = "PersistentMemory";
  
    private Map<Long, FieldState> mReferenceMap = new HashMap();
    private Map<Long, FieldState> mDeserializedReferenceMap = new HashMap();
    private Map<String, ClassState> mClassMap = new HashMap();
    private Map<String, FieldState> mNamedInstanceMap = new HashMap();
    private List<String> mProhibitedClasses = new ArrayList();
    private List<Long> mProhibitedReferences = new ArrayList();
    private List<String> mDirtyClasses = new ArrayList();
    private List<Long> mDirtyReferences = new ArrayList();
    private Stack<List<String>> mPreviousDirtyClasses = new Stack();
    private Stack<List<Long>> mPreviousDirtyReferences = new Stack();
    private int mCurrentRefreshTag = 0;

    /**
     * Register an object instance that must never be included in persistent
     * memory. References to such objects will be replaced with null references
     * during state collection.
     * 
     * @param object Object to be excluded.
     */
    public void addProhibitedReference(Object object) {
        if (object != null) {
            mProhibitedReferences.add(FieldState.getObjectIdentityHashCode(object));
        }
    }

    /**
     * Register class that must never be included in persistent memory.
     * Requests to record the state of these classes' static members will
     * be ignored.
     * 
     * @param classObject Class to be excluded.
     */
    public void addProhibitedClass(Class classObject) {
        if (classObject != null) {
            addProhibitedClass(classObject.getName());
        }
    }

    /**
     * Register class that must never be included in persistent memory.
     * Requests to record the state of these classes' static members will
     * be ignored.
     * 
     * @param className Class to be excluded (given by name).
     */
    public void addProhibitedClass(String className) {
        if (className != null) {
            mProhibitedClasses.add(className);
        }
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
        storeClass(classObject, noDeepRefresh);
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
        storeClass(className, noDeepRefresh);
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
        String instanceName = getInstanceNameFromAid(aid);
        updateStoredNamedInstance(object, objectClass, instanceName, noDeepRefresh);
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
        FieldState instance = storeObject(object, objectClass, noDeepRefresh);
        mNamedInstanceMap.put(instanceName, instance);
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
        storeObject(object, objectClass, noDeepRefresh);
    }
    
    /**
     * Store class instance based on class name in this persistent memory manager.
     * 
     * @param className Class name.
     * @param noDeepRefresh Do not recursively refresh the state of existing objects.
     * @return {@link ClassState} instance containing the stored class state.
     */
    /* package */ ClassState storeClass(String className, boolean noDeepRefresh) {
        if (className == null) {
            return null;
        }

        Class classObject = null;
        try {
            classObject = Class.forName(className);
        } catch (Exception e) {
            Logging.error(LOG_TAG, "Could not resolve Class object for " + className + ": " + e.toString(), e);
        }

        return storeClass(classObject, noDeepRefresh);
    }

    /**
     * Store class instance in this persistent memory manager.
     * 
     * @param classObject Class object instance.
     * @param noDeepRefresh Do not recursively refresh the state of existing objects.
     * @return {@link ClassState} instance containing the stored class state.
     */
    /* package */ ClassState storeClass(Class classObject, boolean noDeepRefresh) {
        if (classObject == null) {
            return null;
        }

        Logging.debug(LOG_TAG, "Processing class " + classObject.getName());

        if (classObject.isEnum() || classObject.isPrimitive()) {
            Logging.debug(LOG_TAG, "Skipping primitive or enumeration type " + classObject.getName());
            return null;
        }

        if (mProhibitedClasses.contains(classObject.getName())) {
            Logging.debug(LOG_TAG, "Skipping prohibited class " + classObject.getName());
            return null;
        }
        
        ClassState instance = mClassMap.get(classObject.getName());

        boolean refresh = !noDeepRefresh;
        
        if (instance == null) {
            // class does not yet exist
            instance = new ClassState(this, classObject);
            mClassMap.put(classObject.getName(), instance);
            refresh = true;
        }

        if ((instance != null) && refresh) {
            instance.refreshClass(noDeepRefresh);
        }
        return instance;
    }

    /**
     * Store object instance in this persistent memory manager.
     * 
     * @param object Object instance.
     * @param paramObjectClass Object class (required only for (boxed) primitives).
     * @return {@link FieldState} instance containing the stored object state.
     */
    /* package */ FieldState storeObject(Object object, Class paramObjectClass, boolean noDeepRefresh) {
        Long identityHashCode = FieldState.getObjectIdentityHashCode(object);
        if (mProhibitedReferences.contains(identityHashCode)) {
            Logging.debug(LOG_TAG, "Skipping prohibited object #" + identityHashCode);
            object = null;
            identityHashCode = FieldState.getObjectIdentityHashCode(object);
        }
        FieldState instance = mReferenceMap.get(identityHashCode);
        Class objectClass = paramObjectClass;

        boolean refresh = !noDeepRefresh;
        
        if (instance == null) {
            // FieldState does not yet exist
            if (object != null) {
                objectClass = object.getClass();
            }

            if (paramObjectClass == null) {
                paramObjectClass = objectClass;
            }

            if ((object == null) || (objectClass == null)) {
                instance = new ObjectReferenceState(this, object);
            } else if (objectClass.isArray()) {
                Class componentClass = objectClass.getComponentType();

                if (componentClass == null) {
                    Logging.error(LOG_TAG, "Unexpected component class: null");
                } else {
                    instance = new ArrayState(this, object, componentClass.getName());
                }
            } else if (PrimitiveValueState.isPrimitiveValueType(objectClass)) {
                //instance = new PrimitiveValueState(this, object, paramObjectClass);
                instance = PrimitiveValueState.getInstance(this, object, paramObjectClass);
            } else {
                instance = new ObjectReferenceState(this, object);
            }
            
            if (instance != null) {
                mReferenceMap.put(identityHashCode, instance);
                refresh = true;
            }
        }

        // always refresh instance (= refresh, if it already exists; initial state capture, if it is newly created)
        if ((instance != null) && refresh) {
            instance.refreshInstance(noDeepRefresh);
        }
        return instance;
    }

    /**
     * Store transient array object instance in this persistent memory manager.
     * 
     * @param object Array object instance.
     * @return {@link FieldState} instance containing the stored array object state.
     */
    /* package */ FieldState storeTransientArray(Object object) {
        Long identityHashCode = FieldState.getObjectIdentityHashCode(object);
        if (mProhibitedReferences.contains(identityHashCode)) {
            Logging.debug(LOG_TAG, "Skipping prohibited object #" + identityHashCode);
            object = null;
            identityHashCode = FieldState.getObjectIdentityHashCode(object);
        }
        FieldState instance = mReferenceMap.get(identityHashCode);

        if (instance == null) {
            // FieldState does not yet exist
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
            
            if (instance != null) {
                mReferenceMap.put(identityHashCode, instance);
                instance.refreshInstance(false);
            }
        }

        return instance;
    }
    
    /**
     * Start a new refresh cycle.
     */
    private void beginRefresh() {
        ++mCurrentRefreshTag;
    }
    
    /**
     * Return the currently valid refresh tag.
     * 
     * @return Current refresh tag value.
     */
    /* package */ int getCurrentRefreshTag() {
        return mCurrentRefreshTag;
    }

    public void bindClassToObject(Class classObject, Object instance) {
        if (classObject != null) {
            String className = classObject.getName();
            bindClassToObject(className, instance);
        }
    }
    
    public void bindClassToObject(String className, Object instance) {
        ClassState classState = mClassMap.get(className);
        FieldState fieldState = getReference(FieldState.getObjectIdentityHashCode(instance));
        if (classState == null) {
            Logging.error(LOG_TAG, "State of class " + className + " has not been recorded!");
        }
        if (fieldState != null) {
            fieldState.addBoundClass(classState);
        } else {
            Logging.error(LOG_TAG, "State of reference " + FieldState.getObjectIdentityHashCode(instance) + " has not been recorded!");
        }
    }
    
    public void bindClassToNamedInstance(Class classObject, String instanceName) {
        if (classObject != null) {
            String className = classObject.getName();
            bindClassToNamedInstance(className, instanceName);
        }
    }
    
    public void bindClassToNamedInstance(String className, String instanceName) {
        ClassState classState = mClassMap.get(className);
        FieldState fieldState = mNamedInstanceMap.get(instanceName);
        if (classState == null) {
            Logging.error(LOG_TAG, "State of class " + className + " has not been recorded!");
        }
        if (fieldState != null) {
            fieldState.addBoundClass(classState);
        } else {
            Logging.error(LOG_TAG, "State of named instance " + instanceName + " has not been recorded!");
        }
    }
    
    public void bindClassToNamedInstance(Class classObject, AID aid) {
        String instanceName = getInstanceNameFromAid(aid);
        bindClassToNamedInstance(classObject, instanceName);
    }
    
    public void bindClassToNamedInstance(String className, AID aid) {
        String instanceName = getInstanceNameFromAid(aid);
        bindClassToNamedInstance(className, instanceName);
    }
    
    public void setDirtyClass(Class classObject) {
        if (classObject != null) {
            setDirtyClass(classObject.getName());
        }
        
    }
    
    public void setDirtyClass(String className) {
        if (!mClassMap.containsKey(className)) {
            Logging.error(LOG_TAG, "State of class " + className + " has not been recorded!");
        }
        mDirtyClasses.add(className);
    }

    public void setDirtyReference(Object object) {
        Long hashCode = FieldState.getObjectIdentityHashCode(object);
        if (!mReferenceMap.containsKey(hashCode)) {
            Logging.error(LOG_TAG, "State of reference " + hashCode + " has not been recorded!");
        }
        mDirtyReferences.add(hashCode);
    }
    
    public void setDirtyNamedInstance(String instanceName) {
        FieldState instance = mNamedInstanceMap.get(instanceName);
        if (instance != null) {
            mDirtyReferences.add(instance.getHashCode());
        } else {
            Logging.error(LOG_TAG, "State of named instance " + instanceName + " has not been recorded!");
        }
    }
    
    public void setDirtyNamedInstance(AID aid) {
        String instanceName = getInstanceNameFromAid(aid);
        setDirtyNamedInstance(instanceName);
    }
    
    public void memoryBarrier(boolean revertMemory) {
        beginRefresh();
        Logging.debug(LOG_TAG, "Memory barrier " + mCurrentRefreshTag + (revertMemory ? " for reversal" : " for refresh"));
        
        if (revertMemory) {
            // revert all changes to dirty objects/classes
            for (String dirtyClass : mDirtyClasses) {
                ClassState dirtyState = mClassMap.get(dirtyClass);
                if (dirtyState != null) {
                    dirtyState.revertClass();
                }
            }
            
            for (Long dirtyReference : mDirtyReferences) {
                FieldState dirtyState = mReferenceMap.get(dirtyReference);
                if (dirtyState != null) {
                    dirtyState.revertInstance();
                }
            }
        } else {
            // update persistent memory to reflect all changes in dirty objects/classes
            for (String dirtyClass : mDirtyClasses) {
                ClassState dirtyState = mClassMap.get(dirtyClass);
                if (dirtyState != null) {
                    dirtyState.refreshClass(false);
                }
            }
            
            for (Long dirtyReference : mDirtyReferences) {
                FieldState dirtyState = mReferenceMap.get(dirtyReference);
                if (dirtyState != null) {
                    dirtyState.refreshInstance(false);
                }
            }
            
            //garbageCollect();
        }
    }
    
    public void clearDirtyFlags() {
        mDirtyClasses.clear();
        mDirtyReferences.clear();
    }
    
    public void pushDirtyFlags() {
        mPreviousDirtyClasses.push(mDirtyClasses);
        mPreviousDirtyReferences.push(mDirtyReferences);
        mDirtyClasses = new ArrayList();
        mDirtyReferences = new ArrayList();
    }
    
    public void popDirtyFlags() {
        clearDirtyFlags();
        
        if (!mPreviousDirtyClasses.empty()) {
            mDirtyClasses = mPreviousDirtyClasses.pop();
        }
        if (!mPreviousDirtyReferences.empty()) {
            mDirtyReferences = mPreviousDirtyReferences.pop();
        }
    }
    
    
    /**
     * Add entry to list of current references.
     * 
     * @param hashCode Current hash code.
     * @param fieldState Current field state.
     */
    /* package */ void addReference(Long hashCode, FieldState fieldState) {
        if (mReferenceMap.containsKey(hashCode)) {
            FieldState fs = mReferenceMap.get(hashCode);
            Logging.error(LOG_TAG, "Adding instance that already exists #" + hashCode + ". Possible loss of identity mapping! (" + fs.toString() + ", " + fs.getFieldType() + ", " + fs.getInstance() + ")");
        }
        mReferenceMap.put(hashCode, fieldState);
    }

    /**
     * Get FieldState based on current hash code reference.
     * 
     * @param hashCode Current hash code.
     */
    /* package */ FieldState getReference(Long hashCode) {
        return mReferenceMap.get(hashCode);
    }
    
    /**
     * Get ClassState based on class object.
     * 
     * @param classObject Class object.
     */
    /* package */ ClassState getClass(Class classObject) {
        return mClassMap.get(classObject.getName());
    }
    
    /**
     * Get ClassState based on class name.
     * 
     * @param className Class name.
     */
    /* package */ ClassState getClass(String className) {
        return mClassMap.get(className);
    }
    
    /**
     * Get FieldState based on de-serialized hash code reference.
     * 
     * @param hashCode Current hash code.
     */
    /* package */ FieldState getDeserializedReference(Long hashCode) {
        return mDeserializedReferenceMap.get(hashCode);
    }
    
    public Object getNamedInstance(String instanceName) {
        FieldState instance = mNamedInstanceMap.get(instanceName);
        if (instance != null) {
            return instance.getInstance();
        }
        
        Logging.error(LOG_TAG, "State of named instance " + instanceName + " has not been recorded!");
        return null;
    }
    
    public Object getNamedInstance(AID aid) {
        String instanceName = getInstanceNameFromAid(aid);
        return getNamedInstance(instanceName);
    }

    /**
     * Add entry to list of classes.
     * 
     * @param className Class name.
     * @param classState Current class state.
     */
    /* package */ void addClass(String className, ClassState classState) {
        mClassMap.put(className, classState);
    }

    public boolean isProhibitedReference(Long hashCode) {
        return mProhibitedReferences.contains(hashCode);
    }
    
    /**
     * Request garbage collection of unused references.
     */
    public void garbageCollect(boolean includeTransientMemory) {
        // TODO: It might be a good idea to only perform garbage collection if mReferencesMap.size() reaches a certain threshold or if GC was skipped N times before.

        if (mCurrentRefreshTag == 0) {
            Logging.debug(LOG_TAG, "GC called without refresh!");
            return;
        }
        
        Logging.debug(LOG_TAG, "Performing garbage-collection @" + mCurrentRefreshTag);
        
        for (ClassState classState : mClassMap.values()) {
            classState.pingClass();
        }
        for (FieldState fieldState : mNamedInstanceMap.values()) {
            fieldState.pingInstance();
        }
        if (!includeTransientMemory) {
            TransientMemory transientMemory = SimulatorSystem.getTransientMemoryInstance();
            for (ArrayList<FieldState> segment : transientMemory.clearOnDeselect.values()) {
                for (FieldState fieldState : segment) {
                    fieldState.pingInstance();
                }
            }
            for (ArrayList<FieldState> segment : transientMemory.clearOnReset.values()) {
                for (FieldState fieldState : segment) {
                    fieldState.pingInstance();
                }
            }
        } else {
            TransientMemory transientMemory = SimulatorSystem.getTransientMemoryInstance();
            for (Map.Entry<String, ArrayList<FieldState>> entry : transientMemory.clearOnDeselect.entrySet()) {
                String key = entry.getKey();
                ArrayList<FieldState> segment = entry.getValue();
                if ((key == null) || key.isEmpty()) {
                    for (FieldState fieldState : segment) {
                        fieldState.pingInstance();
                    }
                } else {
                    Iterator<FieldState> iterField = segment.iterator();
                    while (iterField.hasNext()) {
                        final FieldState fieldState = iterField.next();
                        if ((fieldState == null) || !fieldState.isReachable()) {
                            iterField.remove();
                            Logging.debug(LOG_TAG, "Garbage-collected transient object #" + fieldState.getHashCode() + " (" + fieldState.getFieldType() + ") during GC!");
                        }
                    }
                }
            }
            for (Map.Entry<String, ArrayList<FieldState>> entry : transientMemory.clearOnReset.entrySet()) {
                String key = entry.getKey();
                ArrayList<FieldState> segment = entry.getValue();
                if ((key == null) || key.isEmpty()) {
                    for (FieldState fieldState : segment) {
                        fieldState.pingInstance();
                    }
                } else {
                    Iterator<FieldState> iterField = segment.iterator();
                    while (iterField.hasNext()) {
                        final FieldState fieldState = iterField.next();
                        if ((fieldState == null) || !fieldState.isReachable()) {
                            iterField.remove();
                            Logging.debug(LOG_TAG, "Garbage-collected transient object #" + fieldState.getHashCode() + " (" + fieldState.getFieldType() + ") during GC!");
                        }
                    }
                }
            }
        }
        Iterator<Map.Entry<Long, FieldState>> iter = mReferenceMap.entrySet().iterator();
        while (iter.hasNext()) {
            final Map.Entry<Long, FieldState> entry = iter.next();
            final FieldState fieldState = entry.getValue();
            if ((fieldState == null) || !fieldState.isReachable()) {
                iter.remove();
                if (isProhibitedReference(fieldState.getHashCode())) {
                    Logging.error(LOG_TAG, "Garbage-collected #" + entry.getKey() + " (" + fieldState.getFieldType() + ") that is a prohibited reference!");
                }
                UniqueObjectIdentifier.forget(fieldState.getInstance());
                Logging.debug(LOG_TAG, "Garbage-collected #" + entry.getKey() + " (" + fieldState.getFieldType() + ") during GC!");
            }
        }
        
        Runtime.getRuntime().gc();  // invest additional effort towards garbage collection
    }
    
    /**
     * Reset persistent object storage.
     */
    public void reset(boolean resetProhibitedList) {
        mReferenceMap.clear();
        mDeserializedReferenceMap.clear();
        mClassMap.clear();
        mNamedInstanceMap.clear();
        if (resetProhibitedList) {
            mProhibitedClasses.clear();
            mProhibitedReferences.clear();
        }
        mDirtyClasses.clear();
        mDirtyReferences.clear();
        mPreviousDirtyClasses.clear();
        mPreviousDirtyReferences.clear();
        mCurrentRefreshTag = 0;
        UniqueObjectIdentifier.reset();
    }
    
    /**
     * Serialize persistent memory to XML.
     * 
     * @param xml XmlSerializer instance used as target for serialization.
     */
    public void serializeToXml(XmlSerializer xml) {
        try {
            try {
                xml.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            } catch (Exception e) {
            }
            xml.startDocument("UTF-8", Boolean.TRUE);

            xml.setPrefix("", XmlSchemaPersistentMemory.URI);
            xml.startTag(XmlSchemaPersistentMemory.URI, XmlSchemaPersistentMemory.TAG_ROOT);

            xml.startTag(null, XmlSchemaPersistentMemory.TAG_NAMED_INSTANCES);
            for (Map.Entry<String, FieldState> obj : mNamedInstanceMap.entrySet()) {
                FieldState fieldState = obj.getValue();
                if (fieldState != null) {
                    // make sure that this FieldState's object instance has been created, otherwise we would link to an invalid hash code
                    fieldState.getInstance();
                    
                    xml.startTag(null, XmlSchemaPersistentMemory.TAG_NAMED_INSTANCE);
                    xml.attribute(null, XmlSchemaPersistentMemory.ATTRIBUTE_NAME, obj.getKey());
                    xml.attribute(null, XmlSchemaPersistentMemory.ATTRIBUTE_HASH_CODE, Long.toString(fieldState.getHashCode()));
                    xml.endTag(null, XmlSchemaPersistentMemory.TAG_NAMED_INSTANCE);
                }
            }
            xml.endTag(null, XmlSchemaPersistentMemory.TAG_NAMED_INSTANCES);

            xml.startTag(null, XmlSchemaPersistentMemory.TAG_REFERENCES);
            for (Map.Entry<Long, FieldState> ref : mReferenceMap.entrySet()) {
                FieldState state = ref.getValue();
                if (state != null) {
                    if (state.isRecreated()) {  // garbage-collect unused references
                        state.serializeToXml(xml);
                    } else {
                        Logging.debug(LOG_TAG, "Garbage-collected #" + ref.getKey() + " (" + state.getFieldType() + ") during serialization!");
                    }
                }
            }
            xml.endTag(null, XmlSchemaPersistentMemory.TAG_REFERENCES);

            xml.startTag(null, XmlSchemaPersistentMemory.TAG_CLASSES);
            for (Map.Entry<String, ClassState> cls : mClassMap.entrySet()) {
                ClassState state = cls.getValue();
                if (state != null) {
                    state.serializeToXml(xml);
                }
            }
            xml.endTag(null, XmlSchemaPersistentMemory.TAG_CLASSES);

            xml.endTag(XmlSchemaPersistentMemory.URI, XmlSchemaPersistentMemory.TAG_ROOT);

            xml.endDocument();
        } catch (Exception e) {
            Logging.error(LOG_TAG, "Exception while serializing to XML: " + e.toString(), e);
        }
    }

    /**
     * Internal state definition for XML de-serialization parser to determine
     * the currently parsed section.
     */
    private static enum DeserializationParserState {
        eNone,
        eReferences,
        eClasses,
        eNamedInstances,
    }

    /**
     * De-serialize persistent memory from XML.
     * 
     * @param xml XmlPullParser instance used as source for de-serialization.
     */
    public void deserializeFromXml(XmlPullParser xml) {
        mReferenceMap.clear();
        mDeserializedReferenceMap.clear();
        mClassMap.clear();
        mNamedInstanceMap.clear();
        Map<String, Long> deserializedNamedInstanceMap = new HashMap();

        try {
            DeserializationParserState parserState = DeserializationParserState.eNone;
            int eventType = xml.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                final String tag;
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        tag = xml.getName();

                        if (tag.equals(XmlSchemaPersistentMemory.TAG_REFERENCES)) {
                            parserState = DeserializationParserState.eReferences;
                        } else if (tag.equals(XmlSchemaPersistentMemory.TAG_CLASSES)) {
                            parserState = DeserializationParserState.eClasses;
                        } else if (tag.equals(XmlSchemaPersistentMemory.TAG_NAMED_INSTANCES)) {
                            parserState = DeserializationParserState.eNamedInstances;
                        } else {
                            switch (parserState) {
                                case eReferences:
                                    FieldState.DeserializedFieldState fieldStateResult = FieldState.deserializeFromXml(this, xml, tag);
                                    if ((fieldStateResult != null) && (fieldStateResult.fieldState != null)) {
                                        // processed FieldState
                                        mDeserializedReferenceMap.put(fieldStateResult.hashCode, fieldStateResult.fieldState);
                                    }
                                    break;
                                case eClasses:
                                    ClassState classState = ClassState.deserializeFromXml(this, xml, tag);
                                    if (classState != null) {
                                        // processed ClassState
                                    }
                                    break;
                                case eNamedInstances:
                                    if (tag.equals(XmlSchemaPersistentMemory.TAG_NAMED_INSTANCE)) {
                                        String objectName = xml.getAttributeValue(null, XmlSchemaPersistentMemory.ATTRIBUTE_NAME);
                                        String objectHashCode = xml.getAttributeValue(null, XmlSchemaPersistentMemory.ATTRIBUTE_HASH_CODE);

                                        deserializedNamedInstanceMap.put(objectName, Long.valueOf(objectHashCode));
                                    }
                                    break;
                            }
                        }
                        break;

                    case XmlPullParser.END_TAG:
                        tag = xml.getName();

                        if (tag.equals(XmlSchemaPersistentMemory.TAG_REFERENCES)) {
                            parserState = DeserializationParserState.eNone;
                        } else if (tag.equals(XmlSchemaPersistentMemory.TAG_CLASSES)) {
                            parserState = DeserializationParserState.eNone;
                        } else if (tag.equals(XmlSchemaPersistentMemory.TAG_NAMED_INSTANCES)) {
                            parserState = DeserializationParserState.eNone;
                        }
                        break;
                }

                eventType = xml.next();
            }
        } catch (Exception e) {
            Logging.error(LOG_TAG, "Exception while de-serializing from XML: " + e.toString(), e);
        }

        for (FieldState fieldState : mDeserializedReferenceMap.values()) {
            fieldState.relinkReferences(mDeserializedReferenceMap);
        }

        for (ClassState classState : mClassMap.values()) {
            classState.relinkReferences(mDeserializedReferenceMap);
            classState.restoreClass();  // while objects can be restored upon "using" (assigning) them, classes need to be restored immediately
        }

        for (Map.Entry<String, Long> instance : deserializedNamedInstanceMap.entrySet()) {
            Long hashCode = instance.getValue();
            if (hashCode != null) {
                FieldState fieldState = mDeserializedReferenceMap.get(hashCode);
                if (fieldState != null) {
                    mNamedInstanceMap.put(instance.getKey(), fieldState);
                }
            }
        }
    }
    
    /**
     * Convert an AID to an instance named for storing named object instances.
     * 
     * @param aid AID.
     * @return Instance name.
     */
    private static String getInstanceNameFromAid(AID aid) {
        return "AID:" + __AIDWrapper.getAIDString(aid);
    }
}

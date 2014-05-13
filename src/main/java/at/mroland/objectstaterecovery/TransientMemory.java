/*
 * Copyright 2011 Licel LLC.
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
import com.licel.jcardsim.base.SimulatorSystem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javacard.framework.AID;
import javacard.framework.JCSystem;
import javacard.framework.SystemException;
import javacard.framework.__AIDWrapper;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

/**
 * Basic implementation of storage transient memory of JCRE
 */
public class TransientMemory {
    private static final String LOG_TAG = "TransientMemory";

    private PersistentMemory persistentMemory;
    /* package */ HashMap<String, ArrayList<FieldState>> clearOnDeselect = new HashMap();
    /* package */ HashMap<String, ArrayList<FieldState>> clearOnReset = new HashMap();

    public TransientMemory(PersistentMemory memoryManager) {
        persistentMemory = memoryManager;
    }
    
    /**
     * Creates a transient boolean array with the specified array length.
     * @param length the length of the boolean array
     * @param event the <code>CLEAR_ON...</code> event which causes the array elements to be cleared
     * @return the new transient boolean array
     * @throws NegativeArraySizeException if the <CODE>length</CODE> parameter is negative
     * @throws SystemException with the following reason codes:
     * <ul>
     * <li><code>SystemException.ILLEGAL_VALUE</code> if event is not a valid event code.
     * <li><code>SystemException.NO_TRANSIENT_SPACE</code> if sufficient transient space is not available.
     * <li><code>SystemException.ILLEGAL_TRANSIENT</code> if the current applet context
     * is not the currently selected applet context and <code>CLEAR_ON_DESELECT</code> is specified.
     * </ul>
     */
    public boolean[] makeBooleanArray(short length, byte event) {
        boolean[] array = new boolean[length];
        storeArray(array, event);
        return array;
    }

    /**
     * Creates a transient byte array with the specified array length.
     * @param length the length of the byte array
     * @param event the <code>CLEAR_ON...</code> event which causes the array elements to be cleared
     * @return the new transient byte array
     * @throws NegativeArraySizeException if the <CODE>length</CODE> parameter is negative
     * @throws SystemException with the following reason codes:
     * <ul>
     * <li><code>SystemException.ILLEGAL_VALUE</code> if event is not a valid event code.
     * <li><code>SystemException.NO_TRANSIENT_SPACE</code> if sufficient transient space is not available.
     * <li><code>SystemException.ILLEGAL_TRANSIENT</code> if the current applet context
     * is not the currently selected applet context and <code>CLEAR_ON_DESELECT</code> is specified.
     * </ul>
     */
    public byte[] makeByteArray(short length, byte event) {
        byte[] array = new byte[length];
        storeArray(array, event);
        return array;
    }

    /**
     * Creates a transient short array with the specified array length.
     * @param length the length of the short array
     * @param event the <code>CLEAR_ON...</code> event which causes the array elements to be cleared
     * @return the new transient short array
     * @throws NegativeArraySizeException if the <CODE>length</CODE> parameter is negative
     * @throws SystemException with the following reason codes:
     * <ul>
     * <li><code>SystemException.ILLEGAL_VALUE</code> if event is not a valid event code.
     * <li><code>SystemException.NO_TRANSIENT_SPACE</code> if sufficient transient space is not available.
     * <li><code>SystemException.ILLEGAL_TRANSIENT</code> if the current applet context
     * is not the currently selected applet context and <code>CLEAR_ON_DESELECT</code> is specified.
     * </ul>
     */
    public short[] makeShortArray(short length, byte event) {
        short[] array = new short[length];
        storeArray(array, event);
        return array;
    }

    /**
     * Creates a transient array of <code>Object</code> with the specified array length.
     * @param length the length of the Object array
     * @param event the <code>CLEAR_ON...</code> event which causes the array elements to be cleared
     * @return the new transient Object array
     * @throws NegativeArraySizeException if the <CODE>length</CODE> parameter is negative
     * @throws SystemException with the following reason codes:
     * <ul>
     * <li><code>SystemException.ILLEGAL_VALUE</code> if event is not a valid event code.
     * <li><code>SystemException.NO_TRANSIENT_SPACE</code> if sufficient transient space is not available.
     * <li><code>SystemException.ILLEGAL_TRANSIENT</code> if the current applet context
     * is not the currently selected applet context and <code>CLEAR_ON_DESELECT</code> is specified.
     * </ul>
     */
    public Object[] makeObjectArray(short length, byte event) {
        Object[] array = new Object[length];
        storeArray(array, event);
        return array;
    }

    /**
     * Checks if the specified object is transient.
     * <p>Note:
     * <ul>
     * <em>This method returns </em><code>NOT_A_TRANSIENT_OBJECT</code><em> if the specified object is
     * <code>null</code> or is not an array type.</em>
     * </ul>
     * @param theObj the object being queried
     * @return <code>NOT_A_TRANSIENT_OBJECT</code>, <code>CLEAR_ON_RESET</code>, or <code>CLEAR_ON_DESELECT</code>
     * @see #makeBooleanArray(short, byte)
     * @see #makeByteArray(short, byte)
     * @see #makeObjectArray(short, byte)
     * @see #makeShortArray(short, byte)
     */
    public byte isTransient(Object theObj) {
        FieldState fieldState = persistentMemory.getReference(FieldState.getObjectIdentityHashCode(theObj));
        
        if (fieldState != null) {
            for (ArrayList list : clearOnDeselect.values()) {
                if ((list != null) && list.contains(fieldState)) {
                    return JCSystem.CLEAR_ON_DESELECT;
                }
            }

            for (ArrayList list : clearOnReset.values()) {
                if ((list != null) && list.contains(fieldState)) {
                    return JCSystem.CLEAR_ON_RESET;
                }
            }
        }
        
        return JCSystem.NOT_A_TRANSIENT_OBJECT;
    }

    /**
     * Store <code>arrayRef</code> in memory depends by event type
     * @param arrayRef array reference
     * @param event event type
     */
    private void storeArray(Object arrayRef, byte event) {
        AID currentContextAID;
        String currentContextAIDString;
        FieldState fieldState;
        
        switch (event) {
            case JCSystem.CLEAR_ON_DESELECT:
                currentContextAID = SimulatorSystem.getCurrentPackageContextAID();
                AID selectedContextAID = SimulatorSystem.getSelectedPackageContextAID();
                
                if (currentContextAID != selectedContextAID) {
                    // Transient objects of CLEAR_ON_DESELECT type can only be
                    // created when the currently active context is the context
                    // of the currently selected applet.
                    SystemException.throwIt(SystemException.ILLEGAL_TRANSIENT);
                }
                
                currentContextAIDString = __AIDWrapper.getAIDString(currentContextAID);
                ArrayList clearOnDeselectSegment = clearOnDeselect.get(currentContextAIDString);
                if (clearOnDeselectSegment == null) {
                    clearOnDeselectSegment = new ArrayList();
                    clearOnDeselect.put(currentContextAIDString, clearOnDeselectSegment);
                }
                fieldState = persistentMemory.storeTransientArray(arrayRef);
                clearOnDeselectSegment.add(fieldState);
                break;
            case JCSystem.CLEAR_ON_RESET:
                currentContextAID = SimulatorSystem.getCurrentPackageContextAID();
                
                currentContextAIDString = __AIDWrapper.getAIDString(currentContextAID);
                ArrayList clearOnResetSegment = clearOnReset.get(currentContextAIDString);
                if (clearOnResetSegment == null) {
                    clearOnResetSegment = new ArrayList();
                    clearOnReset.put(currentContextAIDString, clearOnResetSegment);
                }
                fieldState = persistentMemory.storeTransientArray(arrayRef);
                clearOnResetSegment.add(fieldState);
                break;
            default:
                SystemException.throwIt(SystemException.ILLEGAL_VALUE);
        }
    }

    /**
     * Get the number of bytes available in transient memory of given type.
     * @param event  transient memory type (CLEAR_ON_RESET/CLEAR_ON_DESELECT)
     * @return       number of bytes available in transient memory of given type, or <code>Short.MAX_VALUE</code> if number of bytes exceeds <code>Short.MAX_VALUE</code>
     */
    public short getAvailableMemory(byte event) {
        return Short.MAX_VALUE;
    }
    
    /**
     * Clear data of transient objects in a specific memory segment.
     * 
     * @param segment memory segment
     */
    private void clearSegment(ArrayList<FieldState> segment) {
        if (segment != null) {
            for (FieldState fieldState : segment) {
                //if ((fieldState != null) && fieldState.isRecreated()) {  // only reset objects that have been referenced (permit garbage-collection of unused objects)
                if (fieldState != null) {
                    Object obj = fieldState.getInstance();
                    if (obj instanceof boolean[]) {
                        for (int i = 0; i < ((boolean[])obj).length; ++i) {
                            ((boolean[])obj)[i] = false;
                        }
                    }
                    if (obj instanceof byte[]) {
                        for (int i = 0; i < ((byte[])obj).length; ++i) {
                            ((byte[])obj)[i] = 0;
                        }
                    }
                    if (obj instanceof short[]) {
                        for (int i = 0; i < ((short[])obj).length; ++i) {
                            ((short[])obj)[i] = 0;
                        }
                    }
                    if (obj instanceof Object[]) {
                        for (int i = 0; i < ((Object[])obj).length; ++i) {
                            ((Object[])obj)[i] = null;
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Clear data of transient objects depending on event type.
     * @param event  event type (CLEAR_ON_RESET/CLEAR_ON_DESELECT)
     */
    public void clear(byte event) {
        AID selectedContextAID = SimulatorSystem.getSelectedPackageContextAID();
        String selectedContextAIDString = __AIDWrapper.getAIDString(selectedContextAID);
        
        switch (event) {
            case JCSystem.CLEAR_ON_RESET:
                if (selectedContextAID == null) {
                    for (ArrayList<FieldState> segment : clearOnReset.values()) {
                        clearSegment(segment);
                    }
                } else {
                    clearSegment(clearOnReset.get(selectedContextAIDString));
                }
                // don't break here => also clear CLEAR_ON_DESELECT memory!
            case JCSystem.CLEAR_ON_DESELECT:
                if (selectedContextAID == null) {
                    for (ArrayList<FieldState> segment : clearOnDeselect.values()) {
                        clearSegment(segment);
                    }
                } else {
                    clearSegment(clearOnDeselect.get(selectedContextAIDString));
                }
                break;
            default:
                SystemException.throwIt(SystemException.ILLEGAL_VALUE);
        }
    }
    
    /**
     * Delete transient memory segment of a given context.
     */
    public void deleteContextSegments(AID contextAID) {
        if (contextAID == null) {
            String contextAIDString = __AIDWrapper.getAIDString(contextAID);
            clearOnDeselect.remove(contextAIDString);
            clearOnReset.remove(contextAIDString);
        }
    }
    
    /**
     * Reset transient object storage.
     */
    public void reset() {
        clearOnDeselect.clear();
        clearOnReset.clear();
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

            xml.setPrefix("", XmlSchemaTransientMemory.URI);
            xml.startTag(XmlSchemaTransientMemory.URI, XmlSchemaTransientMemory.TAG_ROOT);

            for (Map.Entry<String, ArrayList<FieldState>> obj : clearOnDeselect.entrySet()) {
                ArrayList<FieldState> segment = obj.getValue();
                if (segment != null) {
                    xml.startTag(null, XmlSchemaTransientMemory.TAG_SEGMENT_CLEARONDESELECT);
                    xml.attribute(null, XmlSchemaTransientMemory.ATTRIBUTE_AID, obj.getKey());
                    for (FieldState reference : segment) {
                        if ((reference != null) && (reference.isRecreated())) {  // garbage-collect unused references
                            xml.startTag(null, XmlSchemaTransientMemory.TAG_REFERENCE);
                            xml.attribute(null, XmlSchemaTransientMemory.ATTRIBUTE_HASH_CODE, Long.toString(reference.getHashCode()));
                            xml.endTag(null, XmlSchemaTransientMemory.TAG_REFERENCE);
                        }
                    }
                    xml.endTag(null, XmlSchemaTransientMemory.TAG_SEGMENT_CLEARONDESELECT);
                }
            }
            for (Map.Entry<String, ArrayList<FieldState>> obj : clearOnReset.entrySet()) {
                ArrayList<FieldState> segment = obj.getValue();
                if (segment != null) {
                    String aid = obj.getKey();
                    xml.startTag(null, XmlSchemaTransientMemory.TAG_SEGMENT_CLEARONRESET);
                    xml.attribute(null, XmlSchemaTransientMemory.ATTRIBUTE_AID, (aid != null) ? aid : "");
                    for (FieldState reference : segment) {
                        if ((reference != null) && (reference.isRecreated())) {  // garbage-collect unused references
                            xml.startTag(null, XmlSchemaTransientMemory.TAG_REFERENCE);
                            xml.attribute(null, XmlSchemaTransientMemory.ATTRIBUTE_HASH_CODE, Long.toString(reference.getHashCode()));
                            xml.endTag(null, XmlSchemaTransientMemory.TAG_REFERENCE);
                        }
                    }
                    xml.endTag(null, XmlSchemaTransientMemory.TAG_SEGMENT_CLEARONRESET);
                }
            }

            xml.endTag(XmlSchemaTransientMemory.URI, XmlSchemaTransientMemory.TAG_ROOT);

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
        eSegmentClearOnDeselect,
        eSegmentClearOnReset,
    }

    /**
     * De-serialize persistent memory from XML.
     * 
     * @param xml XmlPullParser instance used as source for de-serialization.
     */
    public void deserializeFromXml(XmlPullParser xml) {
        clearOnDeselect.clear();
        clearOnReset.clear();

        try {
            DeserializationParserState parserState = DeserializationParserState.eNone;
            ArrayList<FieldState> segment = null;
            int eventType = xml.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                final String tag;
                
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        tag = xml.getName();

                        if (tag.equals(XmlSchemaTransientMemory.TAG_SEGMENT_CLEARONDESELECT)) {
                            parserState = DeserializationParserState.eSegmentClearOnDeselect;
                            String aid = xml.getAttributeValue(null, XmlSchemaTransientMemory.ATTRIBUTE_AID);
                            if ((aid != null) && aid.isEmpty()) aid = null;
                            segment = new ArrayList();
                            clearOnDeselect.put(aid, segment);

                        } else if (tag.equals(XmlSchemaTransientMemory.TAG_SEGMENT_CLEARONRESET)) {
                            parserState = DeserializationParserState.eSegmentClearOnReset;
                            String aid = xml.getAttributeValue(null, XmlSchemaTransientMemory.ATTRIBUTE_AID);
                            if ((aid != null) && aid.isEmpty()) aid = null;
                            segment = new ArrayList();
                            clearOnReset.put(aid, segment);
                            
                        } else if (tag.equals(XmlSchemaTransientMemory.TAG_REFERENCE)) {
                            if ((parserState == DeserializationParserState.eSegmentClearOnDeselect) ||
                                (parserState == DeserializationParserState.eSegmentClearOnReset)) {
                                String hashCode = xml.getAttributeValue(null, XmlSchemaTransientMemory.ATTRIBUTE_HASH_CODE);

                                segment.add(persistentMemory.getDeserializedReference(Long.valueOf(hashCode)));
                            }
                        }
                        break;

                    case XmlPullParser.END_TAG:
                        tag = xml.getName();

                        if (tag.equals(XmlSchemaTransientMemory.TAG_SEGMENT_CLEARONRESET)) {
                            parserState = DeserializationParserState.eNone;
                        } else if (tag.equals(XmlSchemaTransientMemory.TAG_SEGMENT_CLEARONDESELECT)) {
                            parserState = DeserializationParserState.eNone;
                        }
                        break;
                }

                eventType = xml.next();
            }
        } catch (Exception e) {
            Logging.error(LOG_TAG, "Exception while de-serializing from XML: " + e.toString(), e);
        }
    }
}

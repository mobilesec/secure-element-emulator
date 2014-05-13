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
package com.licel.jcardsim.base;

import javacard.framework.ISO7816;
import javacard.framework.Util;

/**
 * Internal class to parse command APDUs.
 */
final class CommandAPDU {
    private static final byte LOGICAL_CHN_MASK_TYPE4 = (byte)0x03;
    private static final byte LOGICAL_CHN_MASK_TYPE16 = (byte)0x0F;
    private static final byte APDU_ISOCLASS_MASK = (byte)0x80;
    private static final byte APDU_TYPE_MASK = (byte)0x40;
    private static final byte APDU_SM_MASK_TYPE4 = (byte)0x0C;
    private static final byte APDU_SM_MASK_TYPE16 = (byte)0x20;
    private static final byte APDU_CHAINING_MASK = (byte)0x10;

    private static final int NE_MAX = 256;
    private static final int NE_MAX_EXTENDED = 65536;
  
    private static enum APDUType {
        eAPDUTypeUnknown,
        eAPDUTypeCase1,
        eAPDUTypeCase2S,
        eAPDUTypeCase3S,
        eAPDUTypeCase4S,
        eAPDUTypeCase2E,
        eAPDUTypeCase3E,
        eAPDUTypeCase4E
    }
    
    private final byte[] commandAPDU;
    private final APDUType commandAPDUType;
    private final int commandNc;
    private final int commandNe;

    CommandAPDU(byte[] commandAPDU) {
        if ((commandAPDU == null) || (commandAPDU.length < ISO7816.OFFSET_LC)) {
            throw new IllegalArgumentException();
        }
        
        final int commandBodyLen = commandAPDU.length - ISO7816.OFFSET_LC;

        int fieldNc = 0;
        int fieldNe = 0;
        APDUType apduType = APDUType.eAPDUTypeUnknown;

        if (commandBodyLen <= 0) {
            // Case 1
            apduType = APDUType.eAPDUTypeCase1;
        } else if (commandBodyLen == 1) {
            // Case 2S
            apduType = APDUType.eAPDUTypeCase2S;

            fieldNe = commandAPDU[ISO7816.OFFSET_LC] & 0x0ff;
            if (fieldNe == 0) {
                fieldNe = NE_MAX;
            } else if (fieldNe > NE_MAX) {
                fieldNe = NE_MAX;
            }
        } else {
            if (commandAPDU[ISO7816.OFFSET_LC] == 0) {
                if (commandBodyLen == 3) {
                    // Case 2E
                    apduType = APDUType.eAPDUTypeCase2E;
                    fieldNe = Util.getShort(commandAPDU, (short)(ISO7816.OFFSET_LC + 1)) & 0x0ffff;
                    if (fieldNe == 0) {
                        fieldNe = NE_MAX_EXTENDED;
                    } else if (fieldNe > NE_MAX_EXTENDED) {
                        fieldNe = NE_MAX_EXTENDED;
                    }
                } else if (commandBodyLen > 3) {
                    fieldNc = Util.getShort(commandAPDU, (short)(ISO7816.OFFSET_LC + 1)) & 0x0ffff;

                    if (commandBodyLen == (3 + fieldNc)) {
                        // Case 3E
                        apduType = APDUType.eAPDUTypeCase3E;
                    } else if (commandBodyLen == (5 + fieldNc)) {
                        // Case 4E
                        apduType = APDUType.eAPDUTypeCase4E;
                        fieldNe = Util.getShort(commandAPDU, (short)(ISO7816.OFFSET_LC + 3 + fieldNc)) & 0x0ffff;
                        if (fieldNe == 0) {
                            fieldNe = NE_MAX_EXTENDED;
                        } else if (fieldNe > NE_MAX_EXTENDED) {
                            fieldNe = NE_MAX_EXTENDED;
                        }
                    }
                }
            } else {
                fieldNc = commandAPDU[ISO7816.OFFSET_LC] & 0x0ff;

                if (commandBodyLen == (1 + fieldNc)) {
                    // Case 3S
                    apduType = APDUType.eAPDUTypeCase3S;
                } else if (commandBodyLen == (2 + fieldNc)) {
                    // Case 4S
                    apduType = APDUType.eAPDUTypeCase4S;
                    fieldNe = commandAPDU[ISO7816.OFFSET_CDATA + fieldNc] & 0x0ff;
                    if (fieldNe == 0) {
                        fieldNe = NE_MAX;
                    } else if (fieldNe > NE_MAX) {
                        fieldNe = NE_MAX;
                    }
                }
            }
        }

        if (apduType == APDUType.eAPDUTypeUnknown) {
            throw new IllegalArgumentException();
        }

        this.commandAPDUType = apduType;
        this.commandNc = fieldNc;
        this.commandNe = fieldNe;
        this.commandAPDU = commandAPDU;
    }

    boolean isExtendedLength() {
        return ((commandAPDUType == APDUType.eAPDUTypeCase2E) ||
                (commandAPDUType == APDUType.eAPDUTypeCase3E) ||
                (commandAPDUType == APDUType.eAPDUTypeCase4E));
    }

    byte[] getCommand() {
        return commandAPDU;
    }

    byte getCLA() {
        return commandAPDU[ISO7816.OFFSET_CLA];
    }
    
    byte getCLAChannel() {
        if ((commandAPDU[ISO7816.OFFSET_CLA] & APDU_TYPE_MASK) == 0) {
            return (byte)(commandAPDU[ISO7816.OFFSET_CLA] & LOGICAL_CHN_MASK_TYPE4);
        } else {
            return (byte)(commandAPDU[ISO7816.OFFSET_CLA] & LOGICAL_CHN_MASK_TYPE16);
        }
    }

    boolean isCommandChainingCLA() {
        return isISOInterindustryCLA() &&
               ((commandAPDU[ISO7816.OFFSET_CLA] & APDU_CHAINING_MASK) != 0);
    }

    boolean isSecureMessagingCLA() {
        return isISOInterindustryCLA() &&
               ((((commandAPDU[ISO7816.OFFSET_CLA] & APDU_TYPE_MASK) == 0) &&
                 ((commandAPDU[ISO7816.OFFSET_CLA] & APDU_SM_MASK_TYPE4) != 0)) ||
                (((commandAPDU[ISO7816.OFFSET_CLA] & APDU_TYPE_MASK) != 0) &&
                 ((commandAPDU[ISO7816.OFFSET_CLA] & APDU_SM_MASK_TYPE16) != 0)));
    }

    boolean isISOInterindustryCLA () {
        return ((commandAPDU[ISO7816.OFFSET_CLA] & APDU_ISOCLASS_MASK) == 0);
    }
    
    byte getINS() {
        return commandAPDU[ISO7816.OFFSET_INS];
    }
    
    byte getP1() {
        return commandAPDU[ISO7816.OFFSET_P1];
    }
    
    byte getP2() {
        return commandAPDU[ISO7816.OFFSET_P2];
    }
    
    int getOffsetCData() {
        return isExtendedLength() ? ISO7816.OFFSET_EXT_CDATA : ISO7816.OFFSET_CDATA;
    }

    int getNc() {
        return commandNc;
    }
    
    void getCommandData(byte[] buffer, short offset) {
        Util.arrayCopyNonAtomic(commandAPDU, (short)getOffsetCData(), buffer, offset, (short)commandNc);
    }
    
    int getNe() {
        return commandNe;
    }
}

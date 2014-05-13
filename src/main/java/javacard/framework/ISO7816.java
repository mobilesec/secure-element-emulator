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

package javacard.framework;

/**
 * <code>ISO7816</code> encapsulates constants related to ISO 7816-3 and ISO 7816-4.
 * <code>ISO7816</code> interface contains only static fields.<p>
 * The static fields with <code>SW_</code> prefixes define constants for the ISO 7816-4 defined response
 * status word. The fields which use the <code>_00</code> suffix require the low order byte to be
 * customized appropriately e.g (ISO7816.SW_CORRECT_LENGTH_00 + (0x0025 & 0xFF)).<p>
 * The static fields with <code>OFFSET_</code> prefixes define constants to be used to index into
 * the APDU buffer byte array to access ISO 7816-4 defined header information.
 *
 */
public interface ISO7816
{
    /**
     * Response status : No Error = (short)0x9000
     */
    public static final short SW_NO_ERROR = (short)0x9000;
    /**
     * Response status : Response bytes remaining = 0x6100
     */
    public static final short SW_BYTES_REMAINING_00 = 0x6100;
    /**
     * Response status : Wrong length = 0x6700
     */
    public static final short SW_WRONG_LENGTH = 0x6700;
    /**
     * Response status : Security condition not satisfied = 0x6982
     */
    public static final short SW_SECURITY_STATUS_NOT_SATISFIED = 0x6982;
    /**
     * Response status : File invalid = 0x6983
     */
    public static final short SW_FILE_INVALID = 0x6983;
    /**
     * Response status : Data invalid = 0x6984
     */
    public static final short SW_DATA_INVALID = 0x6984;
    /**
     * Response status : Conditions of use not satisfied = 0x6985
     */
    public static final short SW_CONDITIONS_NOT_SATISFIED = 0x6985;
    /**
     * Response status : Command not allowed (no current EF) = 0x6986
     */
    public static final short SW_COMMAND_NOT_ALLOWED = 0x6986;
    /**
     * Response status : Applet selection failed = 0x6999;
     */
    public static final short SW_APPLET_SELECT_FAILED = 0x6999;
    /**
     * Response status : Wrong data = 0x6A80
     */
    public static final short SW_WRONG_DATA = 0x6A80;
    /**
     * Response status : Function not supported = 0x6A81
     */
    public static final short SW_FUNC_NOT_SUPPORTED = 0x6A81;
    /**
     * Response status : File not found = 0x6A82
     */
    public static final short SW_FILE_NOT_FOUND = 0x6A82;
    /**
     * Response status : Record not found = 0x6A83
     */
    public static final short SW_RECORD_NOT_FOUND = 0x6A83;
    /**
     * Response status : Incorrect parameters (P1,P2) = 0x6A86
     */
    public static final short SW_INCORRECT_P1P2 = 0x6A86;
    /**
     * Response status : Wrong parameters (P1,P2) = 0x6B00
     */
    public static final short SW_WRONG_P1P2 = 0x6B00;
    /**
     * Response status : Wrong Le field; SW2 encodes the exact number of available data bytes = 0x6C00
     */
    public static final short SW_CORRECT_LENGTH_00 = 0x6C00;
    /**
     * Response status : Instruction code not supported or invalid = 0x6D00
     */
    public static final short SW_INS_NOT_SUPPORTED = 0x6D00;
    /**
     * Response status : CLA value not supported = 0x6E00
     */
    public static final short SW_CLA_NOT_SUPPORTED = 0x6E00;
    /**
     * Response status : No precise diagnosis = 0x6F00
     */
    public static final short SW_UNKNOWN = 0x6F00;
    /**
     * Response status : Not enough memory space in the file  = 0x6A84
     */
    public static final short SW_FILE_FULL = 0x6A84;
    /**
     * Response status : Card does not support the operation on the specified logical channel = 0x6881
     */
    public static final short SW_LOGICAL_CHANNEL_NOT_SUPPORTED = 0x6881;
    /**
     * Response status : Card does not support secure messaging = 0x6882
     */
    public static final short SW_SECURE_MESSAGING_NOT_SUPPORTED = 0x6882;
    /**
     * Response status : Warning, card state unchanged  = 0x6200
     */
    public static final short SW_WARNING_STATE_UNCHANGED = 0x6200;
    /**
     * Response status : Last command of the chain expected = 0x6883
     */
    public static final short SW_LAST_COMMAND_EXPECTED = 0x6883;
    /**
     * Response status : Command chaining not supported = 0x6884
     */
    public static final short SW_COMMAND_CHAINING_NOT_SUPPORTED = 0x6884;
    /**
     * APDU header offset : CLA = 0
     */
    public static final byte OFFSET_CLA = 0;
    /**
     * APDU header offset : INS = 1
     */
    public static final byte OFFSET_INS = 1;
    /**
     * APDU header offset : P1 = 2
     */
    public static final byte OFFSET_P1 = 2;
    /**
     * APDU header offset : P2 = 3
     */
    public static final byte OFFSET_P2 = 3;
    /**
     * APDU header offset : LC = 4
     */
    public static final byte OFFSET_LC = 4;
    /**
     * APDU command data offset : CDATA = 5
     */
    public static final byte OFFSET_CDATA = 5;
    /**
     * APDU command data offset : CDATA for extended length APDUs = 7
     */
    public static final byte OFFSET_EXT_CDATA = 7;
    /**
     * APDU command CLA : ISO 7816 = 0x00
     */
    public static final byte CLA_ISO7816 = 0x00;
    /**
     * APDU command INS : SELECT = 0xA4
     */
    public static final byte INS_SELECT = (byte)0xA4;
    /**
     * APDU command INS : EXTERNAL AUTHENTICATE = 0x82
     */
    public static final byte INS_EXTERNAL_AUTHENTICATE = (byte)0x82;
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package javacard.framework;

import at.mroland.objectstaterecovery.PersistentMemory;
import com.licel.jcardsim.base.SimulatorConfig;
import com.licel.jcardsim.base.SimulatorSystem;

/**
 * 
 * Application Protocol Data Unit (APDU) is
 * the communication format between the card and the off-card applications.
 * The format of the APDU is defined in ISO specification 7816-4.<p>
 * 
 * This class only supports messages which conform to the structure of
 * command and response defined in ISO 7816-4. The behavior of messages which
 * use proprietary structure of messages ( for example with header CLA byte in range 0xD0-0xFE ) is
 * undefined. This class optionally supports extended length fields but only when
 * the currently selected applet implements the <code>javacardx.apdu.ExtendedLength</code> interface.<p>
 * 
 * The <code>APDU</code> object is owned by the Java Card runtime environment. The <code>APDU</code> class maintains a byte array
 * buffer which is used to transfer incoming APDU header and data bytes as well as outgoing data.
 * The buffer length must be at least 133 bytes ( 5 bytes of header and 128 bytes of data ).
 * The Java Card runtime environment must zero out the APDU buffer before each new message received from the CAD.<p>
 * 
 * The Java Card runtime environment designates the <code>APDU</code> object as a temporary Java Card runtime environment Entry Point Object
 * (See <em>Runtime Environment
 * Specification for the Java Card Platform</em>, section 6.2.1 for details).
 * A temporary Java Card runtime environment Entry Point Object can be accessed from any applet context. References
 * to these temporary objects cannot be stored in class variables or instance variables
 * or array components.
 * <p>The Java Card runtime environment similarly marks the APDU buffer as a global array
 * (See <em>Runtime Environment
 * Specification for the Java Card Platform</em>, section 6.2.2 for details).
 * A global array
 * can be accessed from any applet context. References to global arrays
 * cannot be stored in class variables or instance variables or array components.
 * <p>
 * 
 * The applet receives the <code>APDU</code> instance to process from
 * the Java Card runtime environment in the <code>Applet.process(APDU)</code> method, and
 * the first five header bytes [ CLA, INS, P1, P2, P3 ] are available
 * in the APDU buffer. (The header format is the ISO7816-4 defined 7 byte extended APDU format
 * with a 3 byte Lc field when the Lc field in the incoming APDU header is 3 bytes long).<p>
 * 
 * The <code>APDU</code> class API is designed to be transport protocol independent.
 * In other words, applets can use the same APDU methods regardless of whether
 * the underlying protocol in use is T=0 or T=1 (as defined in ISO 7816-3).<p>
 * The incoming APDU data size may be bigger than the APDU buffer size and may therefore
 * need to be read in portions by the applet. Similarly, the
 * outgoing response APDU data size may be bigger than the APDU buffer size and may
 * need to be written in portions by the applet. The <code>APDU</code> class has methods
 * to facilitate this.<p>
 * 
 * For sending large byte arrays as response data,
 * the <code>APDU</code> class provides a special method <code>sendBytesLong()</code> which
 * manages the APDU buffer.<p>
 * 
 * <pre>
 * // The purpose of this example is to show most of the methods
 * // in use and not to depict any particular APDU processing
 * 
 * class MyApplet extends javacard.framework.Applet{
 * // ...
 * public void process(APDU apdu){
 * // ...
 * byte[] buffer = apdu.getBuffer();
 * byte cla = buffer[ISO7816.OFFSET_CLA];
 * byte ins = buffer[ISO7816.OFFSET_INS];
 * ...
 * // assume this command has incoming data
 * // Lc tells us the incoming apdu command length
 * short bytesLeft = (short) (buffer[ISO7816.OFFSET_LC] & 0x00FF);
 * if (bytesLeft < (short)55) ISOException.throwIt( ISO7816.SW_WRONG_LENGTH );
 * 
 * short readCount = apdu.setIncomingAndReceive();
 * while ( bytesLeft > 0){
 * // process bytes in buffer[5] to buffer[readCount+4];
 * bytesLeft -= readCount;
 * readCount = apdu.receiveBytes ( ISO7816.OFFSET_CDATA );
 * }
 * //
 * //...
 * //
 * // Note that for a short response as in the case illustrated here
 * // the three APDU method calls shown : setOutgoing(),setOutgoingLength() & sendBytes()
 * // could be replaced by one APDU method call : setOutgoingAndSend().
 * 
 * // construct the reply APDU
 * short le = apdu.setOutgoing();
 * if (le < (short)2) ISOException.throwIt( ISO7816.SW_WRONG_LENGTH );
 * apdu.setOutgoingLength( (short)3 );
 * 
 * // build response data in apdu.buffer[ 0.. outCount-1 ];
 * buffer[0] = (byte)1; buffer[1] = (byte)2; buffer[3] = (byte)3;
 * apdu.sendBytes ( (short)0 , (short)3 );
 * // return good complete status 90 00
 * }
 * // ...
 * }
 * </pre>
 * 
 * The <code>APDU</code> class also defines a set of <code>STATE_..</code> constants
 * which represent the various processing states of the <code>APDU</code> object based
 * on the methods invoked and the state of the data transfers. The
 * <code>getCurrentState()</code> method returns the current state.
 * <p>
 * Note that the state number assignments are ordered as follows:
 * STATE_INITIAL < STATE_PARTIAL_INCOMING < STATE_FULL_INCOMING <
 * STATE_OUTGOING < STATE_OUTGOING_LENGTH_KNOWN < STATE_PARTIAL_OUTGOING <
 * STATE_FULL_OUTGOING.
 * <p>
 * The following are processing error states and have negative state number
 * assignments :
 * STATE_ERROR_NO_T0_GETRESPONSE, STATE_ERROR_T1_IFD_ABORT, STATE_ERROR_IO and
 * STATE_ERROR_NO_T0_REISSUE.
 * <p>Note:<ul>
 * <li><em>The method descriptions use the ISO7816-4 notation
 * for the various APDU I/O cases of input and output directions. For example -
 * T=0 (Case 2S) protocol - refers to short length outbound only case
 * using the T=0 protocol.
 * The perspective of the notation used in the method descriptions
 * is that of the card(ICC) as seen at the transport layer(TPDU). External
 * transformations of the APDU I/O case may have occurred at the CAD
 * and therefore not visible to the card.</em>
 * </ul>
 * @see APDUException
 * @see ISOException
 */ 

public final class APDU {
    /**
     * This is the state of a new <CODE>APDU</CODE> object when only the command
     * header is valid.
     */
    public static final byte STATE_INITIAL = 0;
    /**
     * This is the state of a <CODE>APDU</CODE> object when incoming data
     * has partially been received.
     */
    public static final byte STATE_PARTIAL_INCOMING = 1;
    /**
     * This is the state of a <CODE>APDU</CODE> object when all the
     * incoming data been received.
     */
    public static final byte STATE_FULL_INCOMING = 2;
    /**
     * This is the state of a new <CODE>APDU</CODE> object when data transfer
     * mode is outbound but length is not yet known.
     */
    public static final byte STATE_OUTGOING = 3;
    /**
     * This is the state of a <CODE>APDU</CODE> object when data transfer
     * mode is outbound and outbound length is known.
     */
    public static final byte STATE_OUTGOING_LENGTH_KNOWN = 4;
    /**
     * This is the state of a <CODE>APDU</CODE> object when some outbound
     * data has been transferred but not all.
     */
    public static final byte STATE_PARTIAL_OUTGOING = 5;
    /**
     * This is the state of a <CODE>APDU</CODE> object when all outbound data
     * has been transferred.
     */
    public static final byte STATE_FULL_OUTGOING = 6;
    /**
     * This error state of a <CODE>APDU</CODE> object occurs when an <CODE>APDUException</CODE>
     * with reason code <CODE>APDUException.NO_T0_GETRESPONSE</CODE> has been
     * thrown.
     */
    public static final byte STATE_ERROR_NO_T0_GETRESPONSE = -1;
    /**
     * This error state of a <CODE>APDU</CODE> object occurs when an <CODE>APDUException</CODE>
     * with reason code <CODE>APDUException.T1_IFD_ABORT</CODE> has been
     * thrown.
     */
    public static final byte STATE_ERROR_T1_IFD_ABORT = -2;
    /**
     * This error state of a <CODE>APDU</CODE> object occurs when an <CODE>APDUException</CODE>
     * with reason code <CODE>APDUException.IO_ERROR</CODE> has been
     * thrown.
     */
    public static final byte STATE_ERROR_IO = -3;
    /**
     * This error state of a <CODE>APDU</CODE> object occurs when an <CODE>APDUException</CODE>
     * with reason code <CODE>APDUException.NO_T0_REISSUE</CODE> has been
     * thrown.
     */
    public static final byte STATE_ERROR_NO_T0_REISSUE = -4;
    /**
     * Media nibble mask in protocol byte.
     */
    public static final byte PROTOCOL_MEDIA_MASK = (byte)0xF0;
    /**
     * Type nibble mask in protocol byte.
     */
    public static final byte PROTOCOL_TYPE_MASK = (byte)0x0F;
    /**
     * ISO 7816 transport protocol type T=0.
     */
    public static final byte PROTOCOL_T0 = 0;
    /**
     * This constant is used to denote both the ISO 7816 transport protocol
     * type T=1 and the variant for contactless cards defined in ISO 14443-4.
     */
    public static final byte PROTOCOL_T1 = 1;
    /**
     * Transport protocol Media - Contacted Asynchronous Half Duplex.
     */
    public static final byte PROTOCOL_MEDIA_DEFAULT = (byte)0x00;
    /**
     * Transport protocol Media - Contactless Type A.
     */
    public static final byte PROTOCOL_MEDIA_CONTACTLESS_TYPE_A = (byte)0x80;
    /**
     * Transport protocol Media - Contactless Type B.
     */
    public static final byte PROTOCOL_MEDIA_CONTACTLESS_TYPE_B = (byte)0x90;
    /**
     * Transport protocol Media - USB.
     */
    public static final byte PROTOCOL_MEDIA_USB = (byte)0xA0;
    
    /**
     * APDU buffer.
     */
    private byte[] buffer;
    /**
     * APDU buffer size.
     */
    private static final short BUFFER_SIZE = SimulatorConfig.APDU_BUFFER_SIZE;
    
    /**
     * Fixed input block size for T=0 protocol = 1.
     */
    private static final short T0_IBS = 1;
    /**
     * Fixed output block size for T=0 protocol = 258.
     */
    private static final short T0_OBS = 258;
    /**
     * Fixed NAD for T=0 protocol = 0.
     */
    private static final byte T0_NAD = 0;
    
    /**
     * Transient array to store short-typed variables.
     */
    private short[] ramVarsShort;
    /**
     * Ne (CAD expected length) variable offset in ramVarsShort.
     */
    private static final byte RV_SHORT_NE = 0;
    /**
     * Lr (Applet response length set via setOutgoingLength method) variable offset in ramVarsShort.
     */
    private static final byte RV_SHORT_LR = RV_SHORT_NE+1;
    /**
     * Remaining bytes to be sent (Lr - already sent bytes) variable offset in ramVarsShort.
     */
    private static final byte RV_SHORT_LR_REMAINING = RV_SHORT_LR+1;
    /**
     * Nc (received data length) variable offset in ramVarsShort.
     */
    private static final byte RV_SHORT_NC = RV_SHORT_LR_REMAINING+1;
    /**
     * Remaining bytes to be received (Nc - already received bytes) variable offset in ramVarsShort.
     */
    private static final byte RV_SHORT_NC_REMAINING = RV_SHORT_NC+1;
    /**
     * Total length of ramVarsShort.
     */
    private static final byte RAM_VARS_SHORT_LENGTH = RV_SHORT_NC_REMAINING+1;

    /**
     * Transient array to store byte-typed variables.
     */
    private byte[] ramVarsByte;
    /**
     * CURRENT_STATE variable offset in ramVarsByte.
     */
    private static final byte RV_BYTE_CURRENT_STATE = 0;
    /**
     * LOGICAL_CHANNEL variable offset in ramVarsByte.
     */
    private static final byte RV_BYTE_LOGICAL_CHANNEL = RV_BYTE_CURRENT_STATE+1;
    /**
     * CLA byte variable offset in ramVarsByte.
     */
    private static final byte RV_BYTE_CLA_BYTE = RV_BYTE_LOGICAL_CHANNEL+1;
    /**
     * Total length of ramVarsByte.
     */
    private static final byte RAM_VARS_BYTE_LENGTH = RV_BYTE_CLA_BYTE+1;
    
    /**
     * Transient array to store boolean flags.
     */
    private boolean[] flags;
    /**
     * Outgoing flag.
     */
    private static final byte FLAG_OUTGOING = 0;
    /**
     * Outgoing length set flag.
     */
    private static final byte FLAG_OUTGOING_LEN_SET = FLAG_OUTGOING+1;
    /**
     * No chaining flag.
     */
    private static final byte FLAG_NO_CHAINING = FLAG_OUTGOING_LEN_SET+1;
    /**
     * Incoming flag.
     */
    private static final byte FLAG_INCOMING = FLAG_NO_CHAINING+1;
    /**
     * No GET RESPONSE flag.
     */
    private static final byte FLAG_NO_GET_RESPONSE = FLAG_INCOMING+1;
    /**
     * No command re-issue response flag.
     */
    private static final byte FLAG_NO_REISSUE = FLAG_NO_GET_RESPONSE+1;
    /**
     * Extended length indication flag.
     */
    private static final byte FLAG_EXTENDED_LENGTH = FLAG_NO_REISSUE+1;
    /**
     * Total length of flags.
     */
    private static final byte FLAGS_LENGTH = FLAG_EXTENDED_LENGTH+1;
    
    /**
     * Maximum value of Ne for regular-length APDUs.
     */
    private static final short NE_MAX = 256;
    /**
     * Maximum value of Ne for extended-length APDUs.
     */
    private static final short NE_MAX_EXTENDED = 32767;  //65536;
    
    /**
     * Reference to global APDU instance.
     */
    private static final APDU thisAPDU;
    
    static {
        thisAPDU = new APDU();
        PersistentMemory pm = SimulatorSystem.getPersistentMemoryInstance();
        pm.addProhibitedClass(APDU.class);
        pm.addProhibitedReference(thisAPDU);
        pm.addProhibitedReference(thisAPDU.buffer);
        pm.addProhibitedReference(thisAPDU.ramVarsByte);
        pm.addProhibitedReference(thisAPDU.ramVarsShort);
        pm.addProhibitedReference(thisAPDU.flags);
    }
    
    APDU() {
        buffer = JCSystem.makeTransientByteArray(BUFFER_SIZE, JCSystem.CLEAR_ON_RESET);
        ramVarsByte = JCSystem.makeTransientByteArray(RAM_VARS_BYTE_LENGTH, JCSystem.CLEAR_ON_RESET);
        ramVarsShort = JCSystem.makeTransientShortArray(RAM_VARS_SHORT_LENGTH, JCSystem.CLEAR_ON_RESET);
        flags = JCSystem.makeTransientBooleanArray(FLAGS_LENGTH, JCSystem.CLEAR_ON_RESET);
    }

    /**
     * Returns the APDU buffer byte array.
     * <p>Note:<ul>
     * <li><em>References to the APDU buffer byte array
     * cannot be stored in class variables or instance variables or array components.
     * See <em>Runtime
     * Specification for the Java Card Platform</em>, section 6.2.2 for details.</em>
     * </ul>
     * @return byte array containing the APDU buffer
     */
    public byte[] getBuffer() {
        return buffer;
    }

    /**
     * Returns the configured incoming block size.
     * In T=1 protocol, this corresponds to IFSC (information field size for ICC),
     * the maximum size of incoming data blocks into the card.  In T=0 protocol,
     * this method returns 1.
     * IFSC is defined in ISO 7816-3.<p>
     * This information may be used to ensure that there is enough space remaining in the
     * APDU buffer when <code>receiveBytes()</code> is invoked.
     * <p>Note:
     * <ul>
     * <li><em>On </em><code>receiveBytes()</code><em> the </em><code>bOff</code><em> param
     * should account for this potential blocksize.</em>
     * </ul>
     * @return incoming block size setting
     * @see #receiveBytes(short)
     */
    public static short getInBlockSize() {
        byte protocol = getProtocol();
        if ((protocol & PROTOCOL_TYPE_MASK) == PROTOCOL_T0) {
            return T0_IBS;
        } else {
            return SimulatorSystem.getInBlockSize(protocol);
        }
    }

    /**
     * Returns the configured outgoing block size.
     * In T=1 protocol, this corresponds to IFSD (information field size for interface device),
     * the maximum size of outgoing data blocks to the CAD.
     * In T=0 protocol, this method returns 258 (accounts for 2 status bytes).
     * IFSD is defined in ISO 7816-3.
     * <p>This information may be used prior to invoking the <code>setOutgoingLength()</code> method,
     * to limit the length of outgoing messages when BLOCK CHAINING is not allowed.
     * <p>Note:<ul>
     * <li><em>On </em><code>setOutgoingLength()</code><em> the </em><code>len</code><em> param
     * should account for this potential blocksize.</em>
     * </ul>
     * @return outgoing block size setting
     * @see #setOutgoingLength(short)
     */
    public static short getOutBlockSize() {
        byte protocol = getProtocol();
        if ((protocol & PROTOCOL_TYPE_MASK) == PROTOCOL_T0) {
            return T0_OBS;
        } else {
            return SimulatorSystem.getOutBlockSize(protocol);
        }
    }

    /**
     * Returns the ISO 7816 transport protocol type, T=1 or T=0 in the low nibble
     * and the transport media in the upper nibble in use.
     * @return he protocol media and type in progress
     * Valid nibble codes are listed in PROTOCOL_ .. constants above.
     * @see <CODE>PROTOCOL_T0</CODE>
     */
    public static byte getProtocol() {
        return SimulatorSystem.getCurrentProtocol();
    }

    /**
     * Returns the Node Address byte (NAD) in T=1 protocol, and 0
     * in T=0 protocol.
     * This may be used as additional information to maintain multiple contexts.
     * @return NAD transport byte as defined in ISO 7816-3
     */
    public byte getNAD() {
        byte protocol = getProtocol();
        if ((protocol & PROTOCOL_TYPE_MASK) == PROTOCOL_T0) {
            return T0_NAD;
        } else {
            return SimulatorSystem.getNAD(protocol);
        }
    }

    /**
     * This method is used to set the data transfer direction to
     * outbound and to obtain the expected length of response (Le).
     * This method should only be called on a case 2 or case 4 command,
     * otherwise erroneous behavior may result.
     * <p>Notes.
     * <ul>
     * <li><em>On a case 4 command, the <code>setIncomingAndReceive()</code>
     * must be invoked prior to calling this method. Otherwise, erroneous
     * behavior may result in T=0 protocol.</em>
     * <li><em>Any remaining incoming data will be discarded.</em>
     * <li><em>In T=0 (Case 4S) protocol, this method will return 256 with
     * normal semantics.</em>
     * <li><em>In T=0 (Case 2E, 4S) protocol, this method will return 32767
     * when the currently selected applet implements the
     * <code>javacardx.apdu.ExtendedLength</code> interface.</em>
     * <li><em>In T=1 (Case 2E, 4E) protocol, this method will return 32767
     * when the Le field in the APDU command is 0x0000 and the currently
     * selected applet implements the <code>javacardx.apdu.ExtendedLength</code>
     * interface.</em>
     * <li><em>This method sets the state of the <code>APDU</code> object to
     * <code>STATE_OUTGOING</code>.</em>
     * </ul>
     * @return Le, the expected length of response
     * @throws APDUException with the following reason codes:<ul>
     * <li><code>APDUException.ILLEGAL_USE</code> if this method, or <code>setOutgoingNoChaining()</code> method already invoked.
     * <li><code>APDUException.IO_ERROR</code> on I/O error.
     * </ul>
     */
    public short setOutgoing() throws APDUException {
        if (flags[FLAG_OUTGOING]) {
            APDUException.throwIt(APDUException.ILLEGAL_USE);
        }
        flags[FLAG_OUTGOING] = true;
        ramVarsByte[RV_BYTE_CURRENT_STATE] = STATE_OUTGOING;
        
        ramVarsShort[RV_SHORT_NE] = SimulatorSystem.receiveNe();
        
        if ((getProtocol() & PROTOCOL_TYPE_MASK) == PROTOCOL_T0) {
            if (ramVarsShort[RV_SHORT_NE] == 0) {
                if (flags[FLAG_EXTENDED_LENGTH]) {
                    ramVarsShort[RV_SHORT_NE] = NE_MAX_EXTENDED;
                } else {
                    ramVarsShort[RV_SHORT_NE] = NE_MAX;
                }
            }            
        }
        
        return ramVarsShort[RV_SHORT_NE];
    }

    /**
     * This method is used to set the data transfer direction to outbound
     * without using BLOCK CHAINING (See ISO 7816-3/4) and to obtain the
     * expected length of response (Le).
     * This method should be used in place of the <code>setOutgoing()</code>
     * method by applets which need to be compatible with legacy CAD/terminals
     * which do not support ISO 7816-3/4 defined block chaining. See
     * <em>Runtime Environment Specification for the Java Card Platform</em>,
     * section 9.4 for details.
     * <p>Notes.
     * <ul>
     * <li><em>On a case 4 command, the <code>setIncomingAndReceive()</code>
     * must be invoked prior to calling this method. Otherwise, erroneous
     * behavior may result in T=0 protocol.</em>
     * <li><em>Any remaining incoming data will be discarded.</em>
     * <li><em>In T=0 (Case 4S) protocol, this method will return 256 with
     * normal semantics.</em>
     * <li><em>In T=0 (Case 2E, 4S) protocol, this method will return 256 when
     * the currently selected applet implements the
     * <code>javacardx.apdu.ExtendedLength</code> interface.</em>
     * <li><em>When this method is used, the <code>waitExtension()</code>
     * method cannot be used.</em>
     * <li><em>In T=1 protocol, retransmission on error may be restricted.</em>
     * <li><em>In T=0 protocol, the outbound transfer must be performed
     * without using <code>(ISO7816.SW_BYTES_REMAINING_00+count)</code>
     * response status chaining.</em>
     * <li><em>In T=1 protocol, the outbound transfer must not set the
     * More(M) Bit in the PCB of the I block. See ISO 7816-3.</em>
     * <li><em>This method sets the state of the <code>APDU</code> object to
     * <code>STATE_OUTGOING</code>.</em>
     * </ul>
     * @return Le, the expected length of response data
     * @throws APDUException with the following reason codes:<ul>
     * <li><code>APDUException.ILLEGAL_USE</code> if this method, or <code>setOutgoing()</code> method already invoked.
     * <li><code>APDUException.IO_ERROR</code> on I/O error</ul>
     */
    public short setOutgoingNoChaining() throws APDUException {
        if (flags[FLAG_OUTGOING]) {
            APDUException.throwIt(APDUException.ILLEGAL_USE);
        }
        flags[FLAG_OUTGOING] = true;
        flags[FLAG_NO_CHAINING] = true;
        ramVarsByte[RV_BYTE_CURRENT_STATE] = STATE_OUTGOING;

        ramVarsShort[RV_SHORT_NE] = SimulatorSystem.receiveNe();
        
        if ((getProtocol() & PROTOCOL_TYPE_MASK) == PROTOCOL_T0) {
            if ((ramVarsShort[RV_SHORT_NE] == 0) || (ramVarsShort[RV_SHORT_NE] > NE_MAX)) {
              ramVarsShort[RV_SHORT_NE] = NE_MAX;
            }
        }
        
        return ramVarsShort[RV_SHORT_NE];
    }

    /**
     * Sets the actual length of response data. If a length of <code>0</code> is specified, no data will be output.
     * <p>Note:<ul>
     * <li><em>In T=0 (Case 2&4) protocol, the length is used by the Java Card runtime environment to prompt the CAD for GET RESPONSE commands.</em>
     * <li><em>This method sets the state of the <code>APDU</code> object to
     * <code>STATE_OUTGOING_LENGTH_KNOWN</code>.</em>
     * </ul>
     * @param len the length of response data
     * @throws APDUException  with the following reason codes:<ul>
     * <li><code>APDUException.ILLEGAL_USE</code> if <code>setOutgoing()</code>
     * or <code>setOutgoingNoChaining()</code> not called or if
     * <code>setOutgoingAndSend()</code> already invoked, or this method
     * already invoked.
     * <li><code>APDUException.BAD_LENGTH</code> if any one of the following is
     * true:
     * <ul>
     * <li><code>len</code> is negative.</li>
     * <li><code>len</code> is greater than 256 and the currently selected
     * applet does not implement the <code>javacardx.apdu.ExtendedLength</code>
     * interface.</li>
     * <li>T=0 protocol is in use, non BLOCK CHAINED data transfer is requested
     * and <code>len</code> is greater than 256.</li>
     * <li>T=1 protocol is in use, non BLOCK CHAINED data transfer is requested
     * and <code>len</code> is greater than (IFSD-2), where IFSD is the
     * Outgoing Block Size. The -2 accounts for the status bytes in T=1.</li>
     * </ul>
     * <li><code>APDUException.NO_T0_GETRESPONSE</code> if T=0 protocol is in
     * use and the CAD does not respond to <code>(ISO7816.SW_BYTES_REMAINING_00+count)</code>
     * response status with GET RESPONSE command on the same origin logical
     * channel number as that of the current APDU command.
     * <li><code>APDUException.NO_T0_REISSUE</code> if T=0 protocol is in use
     * and the CAD does not respond to <code>(ISO7816.SW_CORRECT_LENGTH_00+count)</code>
     * response status by re-issuing same APDU command on the same origin
     * logical channel number as that of the current APDU command with the
     * corrected length.
     * <li><code>APDUException.IO_ERROR</code> on I/O error.
     * </ul>
     * @see #getOutBlockSize()
     */
    public void setOutgoingLength(short len) throws APDUException {
        if (!flags[FLAG_OUTGOING]) {
            APDUException.throwIt(APDUException.ILLEGAL_USE);
        }
        if (flags[FLAG_OUTGOING_LEN_SET]) {
            APDUException.throwIt(APDUException.ILLEGAL_USE);
        }
        if (len < 0) {
            APDUException.throwIt(APDUException.BAD_LENGTH);
        }
        if (flags[FLAG_EXTENDED_LENGTH]) {
            if (len > NE_MAX_EXTENDED) {
                APDUException.throwIt(APDUException.BAD_LENGTH);
            }
        } else {
            if (len > NE_MAX) {
                APDUException.throwIt(APDUException.BAD_LENGTH);
            }
        }
        if (flags[FLAG_NO_CHAINING]) {
            if (len > (getOutBlockSize() - 2)) {
                APDUException.throwIt(APDUException.BAD_LENGTH);
            }
        }
        
        if ((getProtocol() & PROTOCOL_TYPE_MASK) == PROTOCOL_T0) {
            // TODO: Implement command reissuance and GET RESPONSE processing for T=0
        }
        
        flags[FLAG_OUTGOING_LEN_SET] = true;
        ramVarsByte[RV_BYTE_CURRENT_STATE] = STATE_OUTGOING_LENGTH_KNOWN;
        ramVarsShort[RV_SHORT_LR] = len;
        ramVarsShort[RV_SHORT_LR_REMAINING] = len;
    }

    /**
     * Gets as many data bytes as will fit without APDU buffer overflow,
     * at the specified offset <code>bOff</code>.
     * Gets all the remaining bytes if they fit.
     * <p>Notes:
     * <ul>
     * <li><em>The space in the buffer must allow for incoming block size.</em>
     * <li><em>In T=1 protocol, if all the remaining bytes do not fit in the
     * buffer, this method may return less bytes than the maximum incoming
     * block size (IFSC).</em>
     * <li><em>In T=0 protocol, if all the remaining bytes do not fit in the
     * buffer, this method may return less than a full buffer of bytes to
     * optimize and reduce protocol overhead.</em>
     * <li><em>In T=1 protocol, if this method throws an <code>APDUException</code>
     * with <code>T1_IFD_ABORT</code> reason code, the Java Card runtime
     * environment will restart APDU command processing using the newly
     * received command. No more input data can be received. No output data can
     * be transmitted. No error status response can be returned.</em>
     * <li><em>This method sets the state of the <code>APDU</code> object to
     * <code>STATE_PARTIAL_INCOMING</code> if all incoming bytes are not
     * received.</em>
     * <li><em>This method sets the state of the <code>APDU</code> object to
     * <code>STATE_FULL_INCOMING</code> if all incoming bytes are received.</em>
     * </ul>
     * @param bOff - the offset into APDU buffer
     * @return number of bytes read. Returns 0 if no bytes are available.
     * @throws APDUException - with the following reason codes:<ul>
     * <li><code>APDUException.ILLEGAL_USE</code> if <code>setIncomingAndReceive()</code>
     * not called or if <code>setOutgoing()</code> or <code>setOutgoingNoChaining()</code>
     * previously invoked.
     * <li><code>APDUException.BUFFER_BOUNDS</code> if not enough buffer space
     * for incoming block size.
     * <li><code>APDUException.IO_ERROR</code> on I/O error.
     * <li><code>APDUException.T1_IFD_ABORT</code> if T=1 protocol is in use
     * and the CAD sends in an ABORT S-Block command to abort the data transfer.
     * @see #getInBlockSize()
     */
    public short receiveBytes(short bOff) throws APDUException {
        if (!flags[FLAG_INCOMING] || flags[FLAG_OUTGOING]) {
            APDUException.throwIt(APDUException.ILLEGAL_USE);
        }
        short ncRemaining = ramVarsShort[RV_SHORT_NC_REMAINING];
        if ((bOff < 0) || ((ncRemaining > 0) && (bOff >= BUFFER_SIZE))) {
            APDUException.throwIt(APDUException.BUFFER_BOUNDS);
        }
        if (ncRemaining != 0) {
            short len = (short) (BUFFER_SIZE - bOff);
            if (len > ncRemaining) len = ncRemaining;
            ncRemaining = SimulatorSystem.receiveAPDU(buffer, bOff, len);
            ramVarsShort[RV_SHORT_NC_REMAINING] = ncRemaining;
            if (ncRemaining == 0) {
                ramVarsByte[RV_BYTE_CURRENT_STATE] = STATE_FULL_INCOMING;
            } else {
                ramVarsByte[RV_BYTE_CURRENT_STATE] = STATE_PARTIAL_INCOMING;
            }
            return len;
        } else {
            ramVarsByte[RV_BYTE_CURRENT_STATE] = STATE_FULL_INCOMING;
            return 0;
        }
    }

    /**
     * This is the primary receive method.
     * Calling this method indicates that this APDU has incoming data. This
     * method gets as many bytes as will fit without buffer overflow in the
     * APDU buffer following the header. It gets all the incoming bytes if
     * they fit.
     * <p>This method should only be called on a case 3 or case 4 command,
     * otherwise erroneous behavior may result.
     * <p>Notes:
     * <ul>
     * <li><em>In T=0 ( Case 3&4 ) protocol, the P3 param is assumed to be Lc.</em>
     * <li><em>Data is read into the buffer at offset 5 for normal APDU semantics.</em>
     * <li><em>Data is read into the buffer at offset 7 for an extended length APDU (Case 3E/4E).</em>
     * <li><em>In T=1 protocol, if all the incoming bytes do not fit in the buffer,
     * this method may return less bytes than the maximum incoming block size (IFSC).</em>
     * <li><em>In T=0 protocol, if all the incoming bytes do not fit in the buffer,
     * this method may return less than a full buffer of bytes to optimize and
     * reduce protocol overhead.</em>
     * <li><em>This method sets the transfer direction to be inbound and calls
     * <code>receiveBytes(5)</code> for normal semantics or
     * <code>receiveBytes(7)</code> for extended semantics.</em>
     * <li><em>This method may only be called once in a <code>Applet.process()</code> method.</em>
     * <li><em>This method sets the state of the <code>APDU</code> object to
     * <code>STATE_PARTIAL_INCOMING</code> if all incoming bytes are not received.</em>
     * <li><em>This method sets the state of the <code>APDU</code> object to
     * <code>STATE_FULL_INCOMING</code> if all incoming bytes are received.</em>
     * </ul>
     * @return number of data bytes read. The Le byte, if any, is not included in the count.
     * Returns 0 if no bytes are available.
     * @throws APDUException with the following reason codes:
     * <ul>
     * <li><code>APDUException.ILLEGAL_USE</code> if <code>setIncomingAndReceive()</code> already invoked or
     * if <code>setOutgoing()</code> or <code>setOutgoingNoChaining()</code> previously invoked.
     * <li><code>APDUException.IO_ERROR</code> on I/O error.
     *  <li><code>APDUException.T1_IFD_ABORT</code> if T=1 protocol is in use and the CAD sends
     * in an ABORT S-Block command to abort the data transfer.
     * </ul>
     * @see #getIncomingLength()
     * @see #getOffsetCdata()
     */
    public short setIncomingAndReceive() throws APDUException {
        if (flags[FLAG_INCOMING] || flags[FLAG_OUTGOING]) {
            APDUException.throwIt(APDUException.ILLEGAL_USE);
        }
        flags[FLAG_INCOMING] = true;
        
        ramVarsShort[RV_SHORT_NC_REMAINING] = ramVarsShort[RV_SHORT_NC];

        if (flags[FLAG_EXTENDED_LENGTH]) {
            return receiveBytes(ISO7816.OFFSET_EXT_CDATA);
        } else {
            return receiveBytes(ISO7816.OFFSET_CDATA);
        }
    }

    /**
     * Sends <code>len</code> more bytes from APDU buffer at specified offset
     * <code>bOff</code>.
     * <p>If the last part of the response is being sent by the invocation
     * of this method, the APDU buffer must not be altered. If the data is
     * altered, incorrect output may be sent to the CAD. Requiring that the
     * buffer not be altered allows the implementation to reduce protocol
     * overhead by transmitting the last part of the response along with the
     * status bytes.
     * <p>Notes:
     * <ul>
     * <li><em>If </em><code>setOutgoingNoChaining()</code><em> was invoked, output block chaining must not be used.</em>
     * <li><em>In T=0 protocol, if <code>setOutgoingNoChaining()</code> was invoked, Le bytes must be transmitted
     * before <code>(ISO7816.SW_BYTES_REMAINING_00+remaining bytes)</code> response status is returned.</em>
     * <li><em>In T=0 protocol, if this method throws an <code>APDUException</code>
     * with <code>NO_T0_GETRESPONSE</code> or <code>NO_T0_REISSUE</code> reason code,
     * the Java Card runtime environment will restart APDU command processing using the newly
     * received command. No more output data can be transmitted. No error status response can be returned.</em>
     * <li><em>In T=1 protocol, if this method throws an <code>APDUException</code>
     * with <code>T1_IFD_ABORT</code> reason code, the Java Card runtime environment
     * will restart APDU command processing using the newly received command.
     * No more output data can be transmitted. No error status response can be returned.</em>
     * <li><em>This method sets the state of the <code>APDU</code> object to
     * <code>STATE_PARTIAL_OUTGOING</code> if all outgoing bytes have not been sent.</em>
     * <li><em>This method sets the state of the <code>APDU</code> object to
     * <code>STATE_FULL_OUTGOING</code> if all outgoing bytes have been sent.</em>
     * </ul>
     * @param bOff - the offset into APDU buffer
     * @param len - the length of the data in bytes to send
     * @throws APDUException - with the following reason codes:<ul>
     * <li><code>APDUException.ILLEGAL_USE</code> if <code>setOutgoingLength()</code> not called
     * or <code>setOutgoingAndSend()</code> previously invoked
     * or response byte count exceeded or if <code>APDUException.NO_T0_GETRESPONSE</code> or
     * <code>APDUException.NO_T0_REISSUE</code> or <code>APDUException.T1_IFD_ABORT</code>
     * previously thrown.
     * <li><code>APDUException.BUFFER_BOUNDS</code> if <code>bOff</code> is negative or
     * <code>len</code> is negative or <code>bOff+len</code> exceeds the buffer size.
     * <li><code>APDUException.IO_ERROR</code> on I/O error.
     * <li><code>APDUException.NO_T0_GETRESPONSE</code> if T=0 protocol is in use and
     * the CAD does not respond to <code>(ISO7816.SW_BYTES_REMAINING_00+count)</code> response status
     * with GET RESPONSE command on the same origin logical channel number as that of the current
     * APDU command.
     * <li><code>APDUException.NO_T0_REISSUE</code> if T=0 protocol is in use and
     * the CAD does not respond to <code>(ISO7816.SW_CORRECT_LENGTH_00+count)</code> response status
     * by re-issuing same APDU command on the same origin logical channel number as that of the current
     * APDU command with the corrected length.
     * <li><code>APDUException.T1_IFD_ABORT</code> if T=1 protocol is in use and the CAD sends
     * in an ABORT S-Block command to abort the data transfer.
     * </ul>
     * @see #setOutgoing()
     * @see #setOutgoingNoChaining()
     */
    public void sendBytes(short bOff, short len) throws APDUException {
        if ((bOff < 0) || (len < 0) || ((short)(bOff + len) > BUFFER_SIZE)) {
            APDUException.throwIt(APDUException.BUFFER_BOUNDS);
        }
        if (!flags[FLAG_OUTGOING_LEN_SET]) {
            APDUException.throwIt(APDUException.ILLEGAL_USE);
        }
        if ((getProtocol() & PROTOCOL_TYPE_MASK) == PROTOCOL_T0) {
            if (flags[FLAG_NO_GET_RESPONSE] || flags[FLAG_NO_REISSUE]) {
                APDUException.throwIt(APDUException.ILLEGAL_USE);
            }
        }
        
        if (len == 0) {
            return;
        }
        
        short Lr = ramVarsShort[RV_SHORT_LR_REMAINING];
        if (len > Lr) {
            APDUException.throwIt(APDUException.ILLEGAL_USE);
        }

        SimulatorSystem.sendAPDU(buffer, bOff, len);

        Lr -= len;

        if (Lr == 0) {
            ramVarsByte[RV_BYTE_CURRENT_STATE] = STATE_FULL_OUTGOING;
        } else {
            ramVarsByte[RV_BYTE_CURRENT_STATE] = STATE_PARTIAL_OUTGOING;
        }
        
        ramVarsShort[RV_SHORT_LR_REMAINING] = Lr;
    }

    /**
     * Sends <code>len</code> more bytes from <code>outData</code> byte array starting at specified offset
     * <code>bOff</code>. <p>If the last of the response is being sent by the invocation
     * of this method, the APDU buffer must not be altered. If the data is altered, incorrect output may be sent to
     * the CAD.
     * Requiring that the buffer not be altered allows the implementation to reduce protocol overhead
     * by transmitting the last part of the response along with the status bytes.
     * <p>The Java Card runtime environment may use the APDU buffer to send data to the CAD.
     * <p>Notes:
     * <ul>
     * <li><em>If </em><code>setOutgoingNoChaining()</code><em> was invoked, output block chaining must not be used.</em>
     * <li><em>In T=0 protocol, if </em><code>setOutgoingNoChaining()</code><em> was invoked, Le bytes must be transmitted
     * before </em><code>(ISO7816.SW_BYTES_REMAINING_00+remaining bytes)</code><em> response status is returned.</em>
     * <li><em>In T=0 protocol, if this method throws an </em><code>APDUException</code><em> with
     * </em><code>NO_T0_GETRESPONSE</code><em> or </em><code>NO_T0_REISSUE</code><em> reason code,
     * the Java Card runtime environment will restart APDU command processing using the newly received command. No more output
     * data can be transmitted. No error status response can be returned.</em>
     * <li><em>In T=1 protocol, if this method throws an </em><code>APDUException</code><em>
     * with </em><code>T1_IFD_ABORT</code><em> reason code, the Java Card runtime environment will restart APDU command processing using the newly
     * received command. No more output data can be transmitted. No error status response can be returned.</em>
     * <li><em>This method sets the state of the <code>APDU</code> object to
     * <code>STATE_PARTIAL_OUTGOING</code> if all outgoing bytes have not been sent.</em>
     * <li><em>This method sets the state of the <code>APDU</code> object to
     * <code>STATE_FULL_OUTGOING</code> if all outgoing bytes have been sent.</em>
     * </ul>
     * @param outData the source data byte array
     * @param bOff the offset into OutData array
     * @param len the byte length of the data to send
     * @throws APDUException with the following reason codes:
     * <ul>
     * <li><code>APDUException.ILLEGAL_USE</code> if <code>setOutgoingLength()</code> not called
     * or <code>setOutgoingAndSend()</code> previously invoked
     * or response byte count exceeded or if <code>APDUException.NO_T0_GETRESPONSE</code> or
     * <code>APDUException.NO_T0_REISSUE</code> or <code>APDUException.NO_T0_REISSUE</code>
     * previously thrown.
     * <li><code>APDUException.IO_ERROR</code> on I/O error.
     * <li><code>APDUException.NO_T0_GETRESPONSE</code> if T=0 protocol is in use and
     * CAD does not respond to <code>(ISO7816.SW_BYTES_REMAINING_00+count)</code> response status
     * with GET RESPONSE command on the same origin logical channel number as that of the current
     * APDU command.
     * <li><code>APDUException.T1_IFD_ABORT</code> if T=1 protocol is in use and the CAD sends
     * in an ABORT S-Block command to abort the data transfer.
     * </ul>
     * @throws SecurityException if the <code>outData</code> array is not accessible in the caller's context
     * @see #setOutgoing()
     * @see #setOutgoingNoChaining()
     */
    public void sendBytesLong(byte outData[], short bOff, short len)
            throws APDUException, SecurityException {
        short sendLength = BUFFER_SIZE;
        while (len > 0) {
            if (len < sendLength) {
                sendLength = len;
            }
            Util.arrayCopy(outData, bOff, buffer, (short) 0, sendLength);
            sendBytes((short) 0, sendLength);
            len -= sendLength;
            bOff += sendLength;
        }
    }

    /**
     * This is the "convenience" send method. It provides for the most efficient way to send a short
     * response which fits in the buffer and needs the least protocol overhead.
     * This method is a combination of <code>setOutgoing(), setOutgoingLength( len )</code> followed by
     * <code>sendBytes ( bOff, len )</code>. In addition, once this method is invoked, <code>sendBytes()</code> and
     * <code>sendBytesLong()</code> methods cannot be invoked and the APDU buffer must not be altered.<p>
     * Sends <code>len</code> byte response from the APDU buffer starting at the specified offset <code>bOff</code>.
     * <p>Notes:
     * <ul>
     * <li><em>No other </em><code>APDU</code><em> send methods can be invoked.</em>
     * <li><em>The APDU buffer must not be altered. If the data is altered, incorrect output may be sent to
     * the CAD.</em>
     * <li><em>The actual data transmission may only take place on return from </em><code>Applet.process()</code>
     * <li><em>This method sets the state of the <code>APDU</code> object to
     * <code>STATE_FULL_OUTGOING</code>.</em>
     * </ul>
     * @param bOff the offset into APDU buffer
     * @param len the bytelength of the data to send
     * @throws APDUException ith the following reason codes:
     * <ul>
     * <li><code>APDUException.ILLEGAL_USE</code> if <code>setOutgoing()</code>
     * or <code>setOutgoingAndSend()</code> previously invoked
     * or response byte count exceeded.
     * <li><code>APDUException.IO_ERROR</code> on I/O error.</ul>
     */
    public void setOutgoingAndSend(short bOff, short len) throws APDUException {
        setOutgoing();
        setOutgoingLength(len);
        sendBytes(bOff, len);
    }

    /**
     * This method returns the current processing state of the
     * <CODE>APDU</CODE> object. It is used by the <CODE>BasicService</CODE> class to help
     * services collaborate in the processing of an incoming APDU command.
     * Valid codes are listed in STATE_ .. constants above.
     * @see #STATE_INITIAL
     * @return the current processing state of the APDU
     */
    public byte getCurrentState() {
        return ramVarsByte[RV_BYTE_CURRENT_STATE];
    }

    /**
     * This method is called to obtain a reference to the current <CODE>APDU</CODE> object.
     * This method can only be called in the context of the currently
     * selected applet.
     * <p>Note:
     * <ul>
     * <li><em>Do not call this method directly or indirectly from within a method
     * invoked remotely via Java Card RMI method invocation from the client. The
     * APDU object and APDU buffer are reserved for use by RMIService. Remote
     * method parameter data may become corrupted.</em>
     * </ul>
     * @return the current <CODE>APDU</CODE> object being processed
     * @throws SecurityException if
     * <ul>
     * <li>the current context is not the context of the currently selected applet instance or
     * <li>this method was not called, directly or indirectly, from the applet's
     * process method (called directly by the Java Card runtime environment), or
     * <li>the method is called during applet installation or deletion.
     * </ul>
     */
    public static APDU getCurrentAPDU() throws SecurityException {
        return thisAPDU;
    }

    /**
     * This method is called to obtain a reference to the current
     * APDU buffer.
     * This method can only be called in the context of the currently
     * selected applet.
     * <p>Note:<ul>
     * <li><em>Do not call this method directly or indirectly from within a method
     * invoked remotely via Java Card RMI method invocation from the client. The
     * <CODE>APDU</CODE> object and APDU buffer are reserved for use by <CODE>RMIService</CODE>. Remote
     * method parameter data may become corrupted.</em>
     * </ul>
     * @return the APDU buffer of the <CODE>APDU</CODE> object being processed
     * @throws SecurityException if
     * <ul>
     * <li>the current context is not the context of the currently selected applet or
     * <li>this method was not called, directly or indirectly, from the applet's
     * process method (called directly by the Java Card runtime environment), or
     * <li>the method is called during applet installation or deletion.
     * </ul>
     */
    public static byte[] getCurrentAPDUBuffer() throws SecurityException {
        return thisAPDU.getBuffer();
    }

    /**
     * Returns the logical channel number associated with the current <CODE>APDU</CODE> command
     * based on the CLA byte. A number in the range 0-3 based on the least
     * significant two bits of the CLA byte is returned if the command contains
     * logical channel encoding. If the command does not contain logical channel
     * information, 0 is returned.
     * See <em>Runtime
     * Specification for the Java Card Platform</em>, section
     * 4.3 for encoding details.
     * @return logical channel number, if present, within the CLA byte, 0 otherwise
     */
    public static byte getCLAChannel() {
        return thisAPDU.ramVarsByte[RV_BYTE_LOGICAL_CHANNEL];
    }

    /**
     * Requests additional processing time from CAD. The implementation should ensure that this method
     * needs to be invoked only under unusual conditions requiring excessive processing times.
     * <p>Notes:
     * <ul>
     * <li><em>In T=0 protocol, a NULL procedure byte is sent to reset the work waiting time (see ISO 7816-3).</em>
     * <li><em>In T=1 protocol, the implementation needs to request the same T=0 protocol work waiting time quantum
     * by sending a T=1 protocol request for wait time extension(see ISO 7816-3).</em>
     * <li><em>If the implementation uses an automatic timer mechanism instead, this method may do nothing.</em>
     * </ul>
     * @throws APDUException with the following reason codes:
     * <ul>
     * <li><code>APDUException.ILLEGAL_USE</code> if <code>setOutgoingNoChaining()</code> previously invoked.
     * <li><code>APDUException.IO_ERROR</code> on I/O error.</ul>
     */
    public static void waitExtension() throws APDUException {
        if (thisAPDU.flags[FLAG_NO_CHAINING]) {
            APDUException.throwIt(APDUException.ILLEGAL_USE);
        }
    }

    /**
     * Returns whether the current <CODE>APDU</CODE> command is the first or
     * part of a command chain.  Bit b5 of the CLA byte if set, indicates
     * that the <CODE>APDU</CODE> is the first or part of a chain of commands.
     * See Runtime Environment Specification for the Java Card Platform,
     * section 4.3 for encoding details.
     * @return <CODE>true</CODE> if this APDU is not the last APDU of a command chain, <CODE>false</CODE> otherwise.
     * @since 2.2.2
     */
    public boolean isCommandChainingCLA() {
        return isISOInterindustryCLA() &&
               ((ramVarsByte[RV_BYTE_CLA_BYTE] & 0x010) != 0);
    }

    /**
     * Returns <CODE>true</CODE> if the encoding of the current <CODE>APDU</CODE>
     * command based on the CLA byte indicates secure messaging. The secure
     * messaging information is in bits (b4,b3) for commands with origin channel
     * numbers 0-3, and in bit b6 for origin channel numbers 4-19.
     * See Runtime Environment Specification for the Java Card Platform,
     * section 4.3 for encoding details.
     * @return <CODE>true</CODE> if the secure messaging bit(s) is(are) nonzero, <CODE>false</CODE> otherwise.
     * @since 2.2.2
     */
    public boolean isSecureMessagingCLA() {
        return isISOInterindustryCLA() &&
               ((((ramVarsByte[RV_BYTE_CLA_BYTE] & 0x040) == 0) &&
                 ((ramVarsByte[RV_BYTE_CLA_BYTE] & 0x00C) != 0)) ||
                (((ramVarsByte[RV_BYTE_CLA_BYTE] & 0x040) != 0) &&
                 ((ramVarsByte[RV_BYTE_CLA_BYTE] & 0x020) != 0)));
    }

    /**
     * Returns whether the current <CODE>APDU</CODE> command CLA byte corresponds
     * to an interindustry command as defined in ISO 7816-4:2005 specification.
     * Bit b8 of the CLA byte if <code>0</code>, indicates that the <CODE>APDU</CODE>
     * is an interindustry command.
     * @return <CODE>true</CODE> if this APDU CLA byte corresponds to an interindustry command, <CODE>false</CODE> otherwise.
     * @since 2.2.2
     */
    public boolean isISOInterindustryCLA() {
        return ((ramVarsByte[RV_BYTE_CLA_BYTE] & 0x080) == 0);
    }

    /**
     * Returns the incoming data length(Lc). This method can be invoked whenever
     * inbound data processing methods can be invoked during case 1, 3 or 4
     * processing. It is most useful for an extended length enabled applet to
     * avoid parsing the variable length Lc format in the APDU header.
     * @return the incoming byte length indicated by the Lc field in the APDU header. Return <code>0</code> if no incoming data (Case 1)
     * @throws APDUException - with the following reason codes:<ul>
     * <li><code>APDUException.ILLEGAL_USE</code> if <code>setIncomingAndReceive()</code> not called or
     * if <code>setOutgoing()</code> or <code>setOutgoingNoChaining()</code> previously invoked.
     * @since 2.2.2
     */
    public short getIncomingLength() {
        if (!flags[FLAG_INCOMING] || flags[FLAG_OUTGOING]) {
            APDUException.throwIt(APDUException.ILLEGAL_USE);
        }

        return ramVarsShort[RV_SHORT_NC];
    }

    /**
     * Returns the offset within the APDU buffer for incoming command data.
     * This method can be invoked whenever inbound data processing methods can be
     * invoked during case 1, 3 or 4 processing. It is most useful for an extended
     * length enabled applet to avoid parsing the variable length Lc format in the
     * APDU header.
     * @return the offset within the APDU buffer for incoming command data from the
     * previous call to <code>setIncomingAndReceive()</code> method. The
     * value returned is either 5 (Lc is 1 byte), or 7 (when Lc is 3 bytes)
     * @throws APDUException - with the following reason codes:<ul>
     * <li><code>APDUException.ILLEGAL_USE</code> if <code>setIncomingAndReceive()</code> not called or
     * if <code>setOutgoing()</code> or <code>setOutgoingNoChaining()</code> previously invoked.
     * @since 2.2.2
     */
    public short getOffsetCdata() {
        if (!flags[FLAG_INCOMING] || flags[FLAG_OUTGOING]) {
            APDUException.throwIt(APDUException.ILLEGAL_USE);
        }

        if (flags[FLAG_EXTENDED_LENGTH]) {
            return ISO7816.OFFSET_EXT_CDATA;
        } else {
            return ISO7816.OFFSET_CDATA;
        }
    }

    /**
     * Load internal state of the APDU.
     * 
     * @param headerBuffer
     * @param headerOffset
     * @param commandDataLength
     * @param logicalChannel
     * @param extendedLength 
     */
    public void load(byte[] headerBuffer, short headerOffset, short commandDataLength, byte logicalChannel, boolean extendedLength) {
        reset();
        
        Util.arrayCopyNonAtomic(headerBuffer, headerOffset, buffer, (short)0, extendedLength ? ISO7816.OFFSET_EXT_CDATA : ISO7816.OFFSET_CDATA);
        ramVarsByte[RV_BYTE_CLA_BYTE] = buffer[ISO7816.OFFSET_CLA];
        ramVarsByte[RV_BYTE_LOGICAL_CHANNEL] = logicalChannel;
        ramVarsShort[RV_SHORT_NC] = commandDataLength;
        flags[FLAG_EXTENDED_LENGTH] = extendedLength;
    }
    
    /**
     * Clear internal state of the APDU.
     */
    public void reset() {
        Util.arrayFillNonAtomic(buffer, (short)0, (short) buffer.length, (byte)0);
        Util.arrayFillNonAtomic(ramVarsByte, (short)0, (short) ramVarsByte.length, (byte)0);
        for (byte i = 0; i < ramVarsShort.length; ++i) { ramVarsShort[i] = (short)0; }
        for (byte i = 0; i < flags.length; ++i) { flags[i] = false; }
    }
}

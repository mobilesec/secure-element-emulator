/*
 * Copyright 2011 Licel LLC.
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

import at.mroland.logging.Logging;
import at.mroland.objectstaterecovery.PersistentMemory;
import at.mroland.objectstaterecovery.PersistentMemory_Disabled;
import at.mroland.objectstaterecovery.TransientMemory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.InvalidParameterException;
import javacard.framework.*;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

/**
 * Base implementation of <code>JCSystem</code>
 * @see JCSystem
 */
public class SimulatorSystem {
    private static final String TAG = SimulatorSystem.class.getName();
    
    /**
     * Persistent memory.
     */
    private static final PersistentMemory persistentMemory = new PersistentMemory(); //new PersistentMemory_Disabled();
    
    /**
     * Transient memory storage.
     */
    private static final TransientMemory transientMemory = new TransientMemory(persistentMemory);

    /**
     * Transaction manager.
     */
    private static final TransactionManager transactionManager = new TransactionManager(persistentMemory);

    /**
     * JavaCard simulator runtime instance.
     */
    private static SimulatorRuntime runtime = new SimulatorRuntime();

    private SimulatorSystem() {
    }

    /**
     * Returns the configured incoming block size.
     * 
     * @param protocol currently used protocol
     * @return configured incoming block size
     */
    public static short getInBlockSize(byte protocol) {
        if ((protocol & APDU.PROTOCOL_TYPE_MASK) != APDU.PROTOCOL_T0) {
            if (((protocol & APDU.PROTOCOL_MEDIA_MASK) == APDU.PROTOCOL_MEDIA_CONTACTLESS_TYPE_A) ||
                ((protocol & APDU.PROTOCOL_MEDIA_MASK) == APDU.PROTOCOL_MEDIA_CONTACTLESS_TYPE_B)) {
                return SimulatorConfig.IFSD_TCL;
            } else {
                return SimulatorConfig.IFSD_T1;
            }
        } else {
            return 1;
        }
    }

    /**
     * Returns the configured outgoing block size.
     * 
     * @param protocol currently used protocol
     * @return configured outgoing block size
     */
    public static short getOutBlockSize(byte protocol) {
        if ((protocol & APDU.PROTOCOL_TYPE_MASK) != APDU.PROTOCOL_T0) {
            if (((protocol & APDU.PROTOCOL_MEDIA_MASK) == APDU.PROTOCOL_MEDIA_CONTACTLESS_TYPE_A) ||
                ((protocol & APDU.PROTOCOL_MEDIA_MASK) == APDU.PROTOCOL_MEDIA_CONTACTLESS_TYPE_B)) {
                return SimulatorConfig.IFSC_TCL;
            } else {
                return SimulatorConfig.IFSC_T1;
            }
        } else {
            return 258;
        }
    }

    /**
     * Returns the currently used communication protocol.
     * 
     * @return currently used communication protocol
     */
    public static byte getCurrentProtocol() {
        return runtime.getCurrentProtocol();
    }

    /**
     * Returns the NAD byte used in the current communication sequence.
     * 
     * @return current NAD, always returns 0
     */
    public static byte getNAD(byte protocol) {
        // TODO: NAD addressing not implemented
        return 0;
    }

    /**
     * Clear transient memory upon event.
     * 
     * @param event 
     */
    public static void clearTransientMemory(byte event) {
        transientMemory.clear(event);
    }
    
    /**
     * Clear transient memory upon event.
     * 
     * @param packageContextAID 
     */
    public static void deleteTransientMemorySegments(AID packageContextAID) {
        transientMemory.deleteContextSegments(packageContextAID);
    }
    
    /**
     * Reset transient memory.
     */
    public static void resetTransientMemory() {
        transientMemory.reset();
    }
    
    /**
     * Reset persistent memory.
     */
    public static void resetPersistentMemory() {
        // WARN: Do not reset the prohibited lists as they might be written from static initializers!
        persistentMemory.reset(false);
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
     * @see #makeTransientBooleanArray(short, byte)
     * @see #makeTransientByteArray(short, byte)
     * @see #makeTransientObjectArray(short, byte)
     * @see #makeTransientShortArray(short, byte)
     */
    public static byte isTransient(Object theObj) {
        return transientMemory.isTransient(theObj);
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
    public static boolean[] makeTransientBooleanArray(short length, byte event) {
        return transientMemory.makeBooleanArray(length, event);
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
    public static byte[] makeTransientByteArray(short length, byte event) {
        return transientMemory.makeByteArray(length, event);
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
    public static short[] makeTransientShortArray(short length, byte event) {
        return transientMemory.makeShortArray(length, event);
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
    public static Object[] makeTransientObjectArray(short length, byte event) {
        return transientMemory.makeObjectArray(length, event);
    }

    /**
     * Returns the Java Card runtime environment-owned instance of the <code>AID</code> object associated with
     * the current applet context, or
     * <code>null</code> if the <code>Applet.register()</code> method
     * has not yet been invoked.
     * <p>Java Card runtime environment-owned instances of <code>AID</code> are permanent Java Card runtime environment
     * Entry Point Objects and can be accessed from any applet context.
     * References to these permanent objects can be stored and re-used.
     * <p>See <em>Runtime Environment Specification for the Java Card Platform</em>, section 6.2.1 for details.
     * @return the <code>AID</code> object
     */
    public static AID getCurrentContextAID() {
        return runtime.getCurrentContextAID();
    }

    /**
     * Returns the Java Card runtime environment-owned instance of the <code>AID</code> object associated with
     * the selected applet context.
     * 
     * @return the <code>AID</code> object
     */
    public static AID getSelectedContextAID() {
        return runtime.getSelectedContextAID();
    }
    
    /**
     * Returns the Java Card runtime environment-owned instance of the <code>AID</code> object associated with
     * the current package context.
     * 
     * @return the <code>AID</code> object
     */
    public static AID getCurrentPackageContextAID() {
        return runtime.getCurrentPackageContextAID();
    }
    
    /**
     * Returns the Java Card runtime environment-owned instance of the <code>AID</code> object associated with
     * the selected applet's package context.
     * 
     * @return the <code>AID</code> object
     */
    public static AID getSelectedPackageContextAID() {
        return runtime.getSelectedPackageContextAID();
    }
    
    /**
     * Returns the Java Card runtime environment-owned instance of the <code>AID</code> object, if any,
     * encapsulating the specified AID bytes in the <code>buffer</code> parameter
     * if there exists a successfully installed applet on the card whose instance AID
     * exactly matches that of the specified AID bytes.
     * <p>Java Card runtime environment-owned instances of <code>AID</code> are permanent Java Card runtime environment
     * Entry Point Objects and can be accessed from any applet context.
     * References to these permanent objects can be stored and re-used.
     * <p>See <em>Runtime Environment Specification for the Java Card Platform</em>, section 6.2.1 for details.
     * @param buffer byte array containing the AID bytes
     * @param offset offset within buffer where AID bytes begin
     * @param length length of AID bytes in buffer
     * @return the <code>AID</code> object, if any; <code>null</code> otherwise. A VM exception
     * is thrown if <code>buffer</code> is <code>null</code>,
     * or if <code>offset</code> or <code>length</code> are out of range.
     */
    public static AID lookupAID(byte buffer[], short offset, byte length) {
        return runtime.lookupAID(buffer, offset, length, true);
    }

    public static AID lookupAnyAID(byte buffer[], short offset, byte length) {
        return runtime.lookupAID(buffer, offset, length, false);
    }
    
    /**
     * Begins an atomic transaction. If a transaction is already in
     * progress (transaction nesting depth level != 0), a TransactionException is
     * thrown.
     * <p>Note:
     * <ul>
     * <li><em>This method may do nothing if the <code>Applet.register()</code>
     * method has not yet been invoked. In case of tear or failure prior to successful
     * registration, the Java Card runtime environment will roll back all atomically updated persistent state.</em>
     * </ul>
     * @throws TransactionException  with the following reason codes:
     * <ul>
     * <li><code>TransactionException.IN_PROGRESS</code> if a transaction is already in progress.
     * </ul>
     * @see #abortTransaction()
     * @see #commitTransaction()
     */
    public static void beginTransaction() {
        transactionManager.beginTransaction();
    }

    /**
     * Aborts the atomic transaction. The contents of the commit
     * buffer is discarded.
     * <p>Note:
     * <ul>
     * <li><em>This method may do nothing if the <code>Applet.register()</code>
     * method has not yet been invoked. In case of tear or failure prior to successful
     * registration, the Java Card runtime environment will roll back all atomically updated persistent state.</em>
     * <li><em>Do not call this method from within a transaction which creates new objects because
     * the Java Card runtime environment may not recover the heap space used by the new object instances.</em>
     * <li><em>Do not call this method from within a transaction which creates new objects because
     * the Java Card runtime environment may, to ensure the security of the card and to avoid heap space loss,
     * lock up the card session to force tear/reset processing.</em>
     * <li><em>The Java Card runtime environment ensures that any variable of reference type which references an object
     * instantiated from within this aborted transaction is equivalent to
     * a </em><code>null</code><em> reference.</em>
     * </ul>
     * @throws TransactionException - with the following reason codes:
     * <ul>
     * <li><code>TransactionException.NOT_IN_PROGRESS</code> if a transaction is not in progress.
     * </ul>
     * @see #beginTransaction()
     * @see #commitTransaction()
     */
    public static void abortTransaction() {
        transactionManager.abortTransaction();
    }

    /**
     * Commits an atomic transaction. The contents of commit
     * buffer is atomically committed. If a transaction is not in
     * progress (transaction nesting depth level == 0) then a TransactionException is
     * thrown.
     * <p>Note:
     * <ul>
     * <li><em>This method may do nothing if the <code>Applet.register()</code>
     * method has not yet been invoked. In case of tear or failure prior to successful
     * registration, the Java Card runtime environment will roll back all atomically updated persistent state.</em>
     * </ul>
     * @throws TransactionException with the following reason codes:
     * <ul>
     * <li><code>TransactionException.NOT_IN_PROGRESS</code> if a transaction is not in progress.
     * </ul>
     * @see #beginTransaction()
     * @see #abortTransaction()
     */
    public static void commitTransaction() {
        transactionManager.commitTransaction();
    }

    /**
     * Returns the current transaction nesting depth level. At present,
     * only 1 transaction can be in progress at a time.
     * @return 1 if transaction in progress, 0 if not
     */
    public static byte getTransactionDepth() {
        return transactionManager.getTransactionDepth();
    }

    /**
     * Returns the number of bytes left in the commit buffer.
     * @return the number of bytes left in the commit buffer
     * @see #getMaxCommitCapacity()
     */
    public static short getUnusedCommitCapacity() {
        return transactionManager.getUnusedCommitCapacity();
    }

    /**
     * Returns the total number of bytes in the commit buffer.
     * This is approximately the maximum number of bytes of
     * persistent data which can be modified during a transaction.
     * However, the transaction subsystem requires additional bytes
     * of overhead data to be included in the commit buffer, and this
     * depends on the number of fields modified and the implementation
     * of the transaction subsystem. The application cannot determine
     * the actual maximum amount of data which can be modified during
     * a transaction without taking these overhead bytes into consideration.
     * @return the total number of bytes in the commit buffer
     * @see #getUnusedCommitCapacity()
     */
    public static short getMaxCommitCapacity() {
        return transactionManager.getMaxCommitCapacity();
    }

    /**
     * Obtains the Java Card runtime environment-owned instance of the <code>AID</code> object associated
     * with the previously active applet context. This method is typically used by a server applet,
     * while executing a shareable interface method to determine the identity of its client and
     * thereby control access privileges.
     * <p>Java Card runtime environment-owned instances of <code>AID</code> are permanent Java Card runtime environment
     * Entry Point Objects and can be accessed from any applet context.
     *  References to these permanent objects can be stored and re-used.
     * <p>See <em>Runtime Environment Specification for the Java Card Platform</em>, section 6.2.1 for details.
     * @return the <code>AID</code> object of the previous context, or <code>null</code> if Java Card runtime environment
     */
    public static AID getPreviousContextAID() {
        return runtime.getPreviousContextAID();
    }

    /**
     * Get the number of bytes available in persistent memory.
     * @return number of bytes available in persistent memory, or <code>Short.MAX_VALUE</code> if number of bytes exceeds <code>Short.MAX_VALUE</code>
     */
    public static short getAvailablePersistentMemory() {
        return transactionManager.getAvailablePersistentMemory();
    }

    /**
     * Get the number of bytes available in transient CLEAR_ON_RESET memory.
     * @return number of bytes available in transient CLEAR_ON_RESET memory, or <code>Short.MAX_VALUE</code> if number of bytes exceeds <code>Short.MAX_VALUE</code>
     */
    public static short getAvailableTransientResetMemory() {
        return transientMemory.getAvailableMemory(JCSystem.CLEAR_ON_RESET);
    }

    /**
     * Get the number of bytes available in transient CLEAR_ON_DESELECT memory.
     * @return number of bytes available in transient CLEAR_ON_DESELECT memory, or <code>Short.MAX_VALUE</code> if number of bytes exceeds <code>Short.MAX_VALUE</code>
     */
    public static short getAvailableTransientDeselectMemory() {
        return transientMemory.getAvailableMemory(JCSystem.CLEAR_ON_DESELECT);
    }

    public static TransientMemory getTransientMemoryInstance() {
        return transientMemory;
    }

    public static PersistentMemory getPersistentMemoryInstance() {
        return persistentMemory;
    }
    
    /**
     * Called by a client applet to get a server applet's
     * shareable interface object. <p>This method returns <code>null</code>
     * if:
     * <ul>
     *  <li>the <code>Applet.register()</code> has not yet been invoked</li>
     *  <li>the server does not exist</li>
     *  <li>the server returns <code>null</code></li>
     * </ul>
     * @param serverAID the AID of the server applet
     * @param parameter optional parameter data
     * @return the shareable interface object or <code>null</code>
     * @see Applet#getShareableInterfaceObject(AID, byte)
     */
    public static Shareable getSharedObject(AID serverAID, byte parameter) {
        return runtime.getSharedObject(serverAID, parameter);
    }

    /**
     * Indicate if the implementation for the Java Card platform supports the
     * object deletion mechanism.
     * <p>Note: Object deletion is automatically handled by the underlying
     * Java VM's garbage collection. We cannot really influence this! However,
     * we provide a configuration flag to simulate possible support.</p>
     * 
     * @return true if object deletion is (indicated as) supported, else false
     */
    public static boolean isObjectDeletionSupported() {
        return SimulatorConfig.OBJECT_DELETION_SUPPORT;
    }

    /**
     * Trigger the object deletion service of the Java Card runtime environment.
     * <p>Note: Object deletion is automatically handled by the underlying
     * Java VM's garbage collection. We cannot really influence this! However,
     * we provide a configuration flag to simulate possible support.</p>
     * 
     * @throws SystemException with the following reason codes:<ul>
     * <li><code>SystemException.ILLEGAL_USE</code> if the object deletion mechanism is
     * not implemented.
     */
    public static void requestObjectDeletion() throws SystemException {
        if (SimulatorConfig.OBJECT_DELETION_SUPPORT) {
            SystemException.throwIt(SystemException.ILLEGAL_USE);
        }
    }

    /**
     * Get the currently active logical channel.
     * @return currently selected logical channel
     */
    public static byte getCurrentlySelectedChannel() {
        return runtime.getCurrentlySelectedChannel();
    }

    /**
     * This method is used to determine if the specified applet is
     * active on the card.
     * <p>Note:
     * <ul>
     * <li><em>This method returns <code>false</code> if the specified applet is
     * not active, even if its context is active.</em>
     * </ul>
     * @param theApplet the AID of the applet object being queried
     * @return <code>true</code> if and only if the applet specified by the
     * AID parameter is currently active on this or another logical channel
     */
    public static boolean isAppletActive(AID theApplet) {
        return runtime.isAppletActive(theApplet);
    }

    /**
     * Get the expected response length (Ne) for the current APDU.
     * 
     * @return Ne
     */
    public static short receiveNe() {
        return runtime.receiveNe();
    }

    /**
     * Receive a number of bytes of the current APDU into the specified buffer.
     * 
     * @param buffer  receive buffer
     * @param bOff    starting offset in buffer for receiving APDU bytes
     * @param len     maximum number of bytes to write into buffer
     * @return        remaining number of bytes not yet received into buffer
     */
    public static short receiveAPDU(byte[] buffer, short bOff, short len) {
        return runtime.receiveAPDU(buffer, bOff, len);
    }
    
    /**
     * Send a number of bytes of the response APDU provided in the specified buffer.
     * 
     * @param buffer  send buffer
     * @param bOff    starting offset of the response APDU in buffer
     * @param len     number of bytes of the response APDU in buffer
     */
    public static void sendAPDU(byte[] buffer, short bOff, short len) {
        runtime.sendAPDU(buffer, bOff, len);
    }

    /**
     * This method is used by the applet to register <code>this</code> applet instance with
     * the Java Card runtime environment and to
     * assign the Java Card platform name of the applet as its instance AID bytes.
     * One of the <code>register()</code> methods must be called from within <code>install()</code>
     * to be registered with the Java Card runtime environment.
     * See <em>Runtime Environment Specification for the Java Card Platform</em>, section 3.1 for details.
     * <p>Note:<ul>
     * <li><em>The phrase "Java Card platform name of the applet" is a reference to the </em><code>AID[AID_length]</code><em>
     * item in the </em><code>applets[]</code><em> item of the </em><code>applet_component</code><em>, as documented in Section 6.5
     * Applet Component in the Virtual Machine Specification for the Java Card Platform.</em>
     * </ul>
     * @throws SystemException with the following reason codes:<ul>
     * <li><code>SystemException.ILLEGAL_AID</code> if the <code>Applet</code> subclass AID bytes are in use or
     * if the applet instance has previously successfully registered with the Java Card runtime environment via one of the
     * <code>register()</code> methods or if a Java Card runtime environment initiated <code>install()</code> method execution is not in progress.
     * </ul>
     */
    public static void registerApplet(Applet applet) throws SystemException {
        runtime.registerApplet(null, applet);
    }

    /**
     * This method is used by the applet to register <code>this</code> applet instance with the Java Card runtime environment and
     * assign the specified AID bytes as its instance AID bytes.
     * One of the <code>register()</code> methods must be called from within <code>install()</code>
     * to be registered with the Java Card runtime environment.
     * See <em>Runtime Environment Specification for the Java Card Platform</em>, section 3.1 for details.
     * <p>Note:<ul>
     * <li><em>The implementation may require that the instance AID bytes specified are the same as that
     * supplied in the install parameter data. An ILLEGAL_AID exception may be thrown otherwise.</em>
     * </ul>
     */
    public static void registerApplet(Applet applet, byte[] bArray, short bOffset, byte bLength)
            throws SystemException {
        runtime.registerApplet(new AID(bArray, bOffset, bLength), applet);
    }

    /**
     * Transceive APDU with Java Card emulator environment.
     * 
     * @param interfaceName name of interface used for APDU exchange
     * @param command       command APDU
     * @return              response APDU
     * @throws InvalidParameterException if specified interface does not exist
     */
    public static byte[] transceiveAPDU(String interfaceName, byte[] command) throws InvalidParameterException {
        return runtime.transceiveAPDU(interfaceName, command);
    }
    
    /**
     * This method is used by the applet <code>process()</code> method to distinguish
     * the SELECT APDU command which selected the applet <code>aThis</code>, from all other
     * other SELECT APDU commands which may relate to file or internal applet state selection.
     * @param applet an applet to be checked
     * @return <code>true</code> if applet <code>aThis</code> is being selected
     */
    public static boolean isAppletSelecting(Applet applet) {
        return runtime.isAppletSelecting(applet);
    }
    
    /**
     * Return the <code>SimulatorRuntime</code> instance.
     * @return instance of the SimulatorRuntime
     */
    static SimulatorRuntime getRuntime() {
        return runtime;
    }
    
    /**
     * Force a reset of the simulator runtime environment.
     */
    static void resetRuntime() {
        runtime.resetRuntime();
    }
    
    public static void saveToPersistentStorage(File basePath) {
        // save state of runtime (applets, packages, etc)
        runtime.saveState(persistentMemory);

        // save persistent memory manager
        XmlPullParserFactory pullParserFactory;
        try {
            pullParserFactory = XmlPullParserFactory.newInstance();
        } catch (XmlPullParserException e) {
            pullParserFactory = null;
            Logging.error(TAG, "Exception while retrieving XmlPullParserFactory: " + e.toString(), e);
        }
        
        try {
            FileOutputStream ostr = new FileOutputStream(new File(basePath, "persistentmemory.xml"));
            XmlSerializer xml = pullParserFactory.newSerializer();
//            XmlSerializer xml = Xml.newSerializer();

            xml.setOutput(ostr, "UTF-8");

            persistentMemory.serializeToXml(xml);

            ostr.flush();
            ostr.close();
        } catch (Exception e) {
            Logging.error(TAG, "Exception while serializing to persistent storage: " + e.toString(), e);
        }

        // save transient memory state
        try {
            FileOutputStream ostr = new FileOutputStream(new File(basePath, "transientmemory.xml"));
            XmlSerializer xml = pullParserFactory.newSerializer();
//            XmlSerializer xml = Xml.newSerializer();

            xml.setOutput(ostr, "UTF-8");

            transientMemory.serializeToXml(xml);

            ostr.flush();
            ostr.close();
        } catch (Exception e) {
            Logging.error(TAG, "Exception while serializing to persistent storage: " + e.toString(), e);
        }
    }
    
    public static void loadFromPersistentStorage(File basePath) {
        // load persistent memory manager
        XmlPullParserFactory pullParserFactory;
        try {
            pullParserFactory = XmlPullParserFactory.newInstance();
        } catch (XmlPullParserException e) {
            pullParserFactory = null;
            Logging.error(TAG, "Exception while retrieving XmlPullParserFactory: " + e.toString(), e);
        }
        
        try {
            FileInputStream istr = new FileInputStream(new File(basePath, "persistentmemory.xml"));
            XmlPullParser xml = pullParserFactory.newPullParser();
//            XmlPullParser xml = Xml.newPullParser();
            
            xml.setInput(istr, "UTF-8");

            persistentMemory.deserializeFromXml(xml);

            istr.close();
        } catch (Exception e) {
            Logging.error(TAG, "Exception while de-serializing persistent memory from persistent storage: " + e.toString(), e);
        }
        
        // load transient memory state
        try {
            FileInputStream istr = new FileInputStream(new File(basePath, "transientmemory.xml"));
            XmlPullParser xml = pullParserFactory.newPullParser();
//            XmlPullParser xml = Xml.newPullParser();
            
            xml.setInput(istr, "UTF-8");

            transientMemory.deserializeFromXml(xml);

            istr.close();
        } catch (Exception e) {
            Logging.error(TAG, "Exception while de-serializing transient memory from persistent storage: " + e.toString(), e);
        }
        
        // load state of runtime (applets, packages, etc)
        runtime.loadState(persistentMemory);
    }
    
    
    /**
     * Load package for installation.
     * 
     * @param packageDef package definition structure
     */
    public static void installForLoad(PackageDefinition packageDef) {
        runtime.installForLoad(packageDef);
    }

    /**
     * Install applet.
     * 
     * @param classAID
     * @param instanceAID
     * @param controlInfo
     * @param appletData 
     */
    public static void installForInstall(AID classAID, byte[] instanceAID, byte[] controlInfo, byte[] appletData) {
        runtime.installForInstall(classAID, instanceAID, controlInfo, appletData);
    }
    
    /**
     * Make applet selectable/non-selectable.
     * 
     * @param instanceAID    applet instance AID
     * @param selectable     if true, applet will become selectable; if false, applet will become non-selectable
     */
    public static void installForMakeSelectable(AID instanceAID, boolean selectable) {
        runtime.installForMakeSelectable(instanceAID, selectable);
    }
    
    /**
     * Make applet the default applet on a specific interface and logical channel.
     * 
     * @param instanceAID    applet instance AID or null to remove default selection
     * @param interfaceName  interface to modify default selection on or null to address all interfaces
     * @param channel        channel to modify default selection on or -1 to address all channels
     */
    public static void installForDefaultSelection(AID instanceAID, String interfaceName, byte channel) {
        runtime.installForDefaultSelection(instanceAID, interfaceName, channel);
    }
    
    /**
     * Uninstall a previously installed applet instance.
     * 
     * @param instanceAID applet instance's AID
     */
    public static void uninstall(AID instanceAID) {
        runtime.uninstall(instanceAID);
    }

    /**
     * Remove a previously loaded package.
     * This method automatically uninstalls all applets contained in the package.
     * 
     * @param packageDef package definition structure
     */
    public static void remove(PackageDefinition packageDef) {
        runtime.remove(packageDef);
    }
    
    /**
     * Remove a previously loaded package.
     * This method automatically uninstalls all applets contained in the package.
     * 
     * @param packageAID package's AID
     */
    public static void remove(AID packageAID) {
        runtime.remove(packageAID);
    }
}

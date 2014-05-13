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
import at.mroland.utils.StringUtils;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.locks.ReentrantLock;
import javacard.framework.*;

/**
 * Base implementation of Java Card Runtime
 * @see JCSystem
 * @see Applet
 */
public class SimulatorRuntime {
    private static final String TAG = SimulatorRuntime.class.getName();

    /**
     * Storage for loaded packages.
     */
    private HashMap<AID, PackageHolder> packages = new HashMap();
    
    /**
     * Storage for loaded applets and registered applet instances.
     */
    private HashMap<AID, AppletInstanceHolder> applets = new HashMap();
    
    /**
     * Storage for card interfaces and their logical channels.
     */
    private HashMap<String, CardInterface> interfaces = new HashMap();
    
    /**
     * Currently selected applet context.
     */
    private AppletInstanceHolder selectedAppletContext;
    
    /**
     * Current applet context.
     */
    private AppletInstanceHolder currentAppletContext;
    
    /**
     * Stack of previously selected applet contexts.
     * 
     * TODO: Implement context switching (if possible)
     *       Will be difficult as this would need to be done on calls into
     *       sharable interfaces!
     */
    private Stack<AppletInstanceHolder> previousAppletContexts = new Stack();
    
    /**
     * Current active channel.
     */
    private byte activeChannel;
    
    /**
     * Current active interface.
     */
    private CardInterface activeInterface;
    
    /**
     * Applet in INSTALL phase.
     */
    private AID appletToInstallAID;
    
    /**
     * Applet installation parameter.
     */
    private byte[] installParameter = new byte[127];
    
    /**
     * Inbound command byte array buffer.
     */
    private byte[] commandBuffer = null;
    
    /**
     * Outbound response byte array buffer data size.
     */
    private int commandBufferOffset = 0;
    
    /**
     * Outbound response byte array buffer.
     */
    private byte[] responseBuffer = new byte[SimulatorConfig.EXTENDED_LENGTH_SUPPORT ? SimulatorConfig.EXTENDED_LENGTH_MAXIMUM : 256];
    
    /**
     * Outbound response byte array buffer data size.
     */
    private int responseBufferSize = 0;
    
    /**
     * Expected response length.
     */
    private int responseExpectedLength;
    
    /**
     * APDU processing serialization lock.
     */
    private ReentrantLock singleProcessLock;

    /**
     * Constructs and initializes the SimulatorRuntime.
     */
    SimulatorRuntime() {
        singleProcessLock = new ReentrantLock();
        
        interfaces.clear();
        for (CardInterface.InterfaceConfig ifcfg : SimulatorConfig.INTERFACES) {
            if (ifcfg != null) {
                interfaces.put(ifcfg.NAME, new CardInterface(ifcfg.NAME, ifcfg.PROTOCOL, ifcfg.MAX_CHANNELS));
            }
        }
    }
    
    /**
     * Return current applet context AID or null.
     * 
     * @return current applet context's instance AID
     */
    AID getCurrentContextAID() {
        if (currentAppletContext != null) {
            return currentAppletContext.getInstanceAID();
        } else {
            return null;
        }
    }

    /**
     * Return selected applet context AID or null.
     * 
     * @return selected applet's instance AID
     */
    AID getSelectedContextAID() {
        if (selectedAppletContext != null) {
            return selectedAppletContext.getInstanceAID();
        } else {
            return null;
        }
    }
    
    /**
     * Return current package context AID or null.
     * 
     * @return current package context AID
     */
    AID getCurrentPackageContextAID() {
        if (currentAppletContext != null) {
            return currentAppletContext.getPackageAID();
        } else {
            return null;
        }
    }

    /**
     * Return selected package context AID or null.
     * 
     * @return selected applet's package context AID
     */
    AID getSelectedPackageContextAID() {
        if (selectedAppletContext != null) {
            return selectedAppletContext.getPackageAID();
        } else {
            return null;
        }
    }

    /**
     * Lookup applet AID instance by AID value contained in byte array.
     * 
     * @param buffer buffer containing AID
     * @param offset offset in buffer where AID starts
     * @param length length of AID in buffer
     * @param appletInstancesOnly search within applet instances only
     * @return       applet context AID instance
     */
    public AID lookupAID(byte buffer[], short offset, byte length, boolean appletInstancesOnly) {
        for (Map.Entry<AID, AppletInstanceHolder> entry : applets.entrySet()) {
            AppletInstanceHolder ah = entry.getValue();
            
            if ((ah != null) && !appletInstancesOnly || ah.isInstalled()) {
                AID aid = entry.getKey();
                if ((aid != null) && aid.equals(buffer, offset, length)) {
                    return aid;
                }
            }
        }
        if (!appletInstancesOnly) {
            for (AID packageAid : packages.keySet()) {
                if ((packageAid != null) && packageAid.equals(buffer, offset, length)) {
                    return packageAid;
                }
            }
        }
        return null;
    }

    /**
     * Lookup applet by AID.
     * 
     * @param lookupAid applet AID
     * @param appletInstancesOnly search within applet instances only
     * @return          applet's AppletInstanceHolder instance
     */
    AppletInstanceHolder lookupApplet(AID lookupAid, boolean appletInstancesOnly) {
        if (lookupAid != null) {
            for (Map.Entry<AID, AppletInstanceHolder> entry : applets.entrySet()) {
                AppletInstanceHolder ah = entry.getValue();

                if ((ah != null) && (!appletInstancesOnly || ah.isInstalled()) &&
                    lookupAid.equals(entry.getKey())) {
                    return ah;
                }
            }
        }
        return null;
    }

    /**
     * Lookup applet by AID value contained in byte array.
     * 
     * @param buffer buffer containing AID
     * @param offset offset in buffer where AID starts
     * @param length length of AID in buffer
     * @param appletInstancesOnly search within applet instances only
     * @return          applet's AppletInstanceHolder instance
     */
    AppletInstanceHolder lookupApplet(byte buffer[], short offset, byte length, boolean appletInstancesOnly) {
        if ((buffer != null) && (offset >= 0) && (length > 0) && (offset + length < buffer.length))  {
            for (Map.Entry<AID, AppletInstanceHolder> entry : applets.entrySet()) {
                AppletInstanceHolder ah = entry.getValue();

                if ((ah != null) && (!appletInstancesOnly || ah.isInstalled())) {
                    AID aid = entry.getKey();
                    if ((aid != null) && aid.equals(buffer, offset, length)) {
                        return ah;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Find applet by (partial) AID.
     * 
     * TODO: matchingType is ignored in current implementation, first match will be returned regardless of matchingType.
     * 
     * @param queryAid applet AID
     * @param matchingType partial AID matching type according to P2(b1,b2) of SELECT (by DF name) command
     * @param appletInstancesOnly search within applet instances only
     * @return applet's AppletInstanceHolder instance
     */
    AppletInstanceHolder findApplet(byte[] queryAid, byte matchingType, boolean appletInstancesOnly) {
        if ((queryAid == null) || (queryAid.length > 16)) return null;
        
        AppletInstanceHolder ahMatch = lookupApplet(queryAid, (short)0, (byte)queryAid.length, appletInstancesOnly);
        
        if (ahMatch != null) {
            return ahMatch;
        } else {
            List<Map.Entry<AID, AppletInstanceHolder>> appletSet = new ArrayList(applets.entrySet());
            Collections.sort(appletSet, new Comparator<Map.Entry<AID, AppletInstanceHolder>>() {
                /**
                 * Compares two Map.Entry&lt;AID, AppletInstanceHolder&gt; based on the contained AID.
                 * 
                 * Sort order is based on the unsigned comparison of the AID bytes
                 * and the size of the AID as in the following example:<ol>
                 * <li>(null)</li>
                 * <li>A000000001</li>
                 * <li>A0000010201010</li>
                 * <li>A00000102010</li>
                 * <li>A0000010202000</li>
                 * <li>A000001020</li>
                 * </ol>
                 */
                public int compare(Map.Entry<AID, AppletInstanceHolder> o1,
                                   Map.Entry<AID, AppletInstanceHolder> o2) {
                    if (o1 == o2) return 0;
                    if (o1 == null) return Integer.MIN_VALUE;
                    if (o2 == null) return Integer.MAX_VALUE;
                    
                    AID aid1 = o1.getKey();
                    AID aid2 = o2.getKey();
                    
                    if (aid1 == aid2) return 0;
                    if (aid1 == null) return Integer.MIN_VALUE;
                    if (aid2 == null) return Integer.MAX_VALUE;
                    
                    if (aid1.equals(aid2)) return 0;
                    
                    byte[] aid1Bytes = new byte[16];
                    int aid1Length = aid1.getBytes(aid1Bytes, (short)0);
                    byte[] aid2Bytes = new byte[16];
                    int aid2Length = aid2.getBytes(aid2Bytes, (short)0);
                    
                    if (aid1Length == aid2Length) {
                        for (int i = 0; i < aid1Length; ++i) {
                            if ((aid1Bytes[i] & 0x0FF) < (aid2Bytes[i] & 0x0FF)) {
                                return -1;
                            } else if ((aid1Bytes[i] & 0x0FF) > (aid2Bytes[i] & 0x0FF)) {
                                return +1;
                            }
                        }
                        return 0;
                    } else if (aid1Length < aid2Length) {
                        for (int i = 0; i < aid1Length; ++i) {
                            if ((aid1Bytes[i] & 0x0FF) < (aid2Bytes[i] & 0x0FF)) {
                                return -1;
                            } else if ((aid1Bytes[i] & 0x0FF) > (aid2Bytes[i] & 0x0FF)) {
                                return +1;
                            }
                        }
                        return +1;
                    } else {
                        for (int i = 0; i < aid2Length; ++i) {
                            if ((aid1Bytes[i] & 0x0FF) < (aid2Bytes[i] & 0x0FF)) {
                                return -1;
                            } else if ((aid1Bytes[i] & 0x0FF) > (aid2Bytes[i] & 0x0FF)) {
                                return +1;
                            }
                        }
                        return -1;
                    }
                }
            });
            for (Map.Entry<AID, AppletInstanceHolder> entry : appletSet) {
                AppletInstanceHolder ah = entry.getValue();

                if ((ah != null) && (!appletInstancesOnly || ah.isInstalled())) {
                    AID aid = entry.getKey();
                    if ((aid != null) && aid.partialEquals(queryAid, (short)0, (byte)queryAid.length)) {
                        return ah;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Lookup package by AID.
     * 
     * @param lookupAid package AID
     * @return          package's PackageHolder instance
     */
    PackageHolder lookupPackage(AID lookupAid) {
        return packages.get(lookupAid);
    }

    /**
     * Return previously selected applet context AID.
     * 
     * @return previously selected applet context AID, or null if no previously selected context
     */
    AID getPreviousContextAID() {
        if ((previousAppletContexts != null) && !previousAppletContexts.empty()) {
            AppletInstanceHolder ah = previousAppletContexts.peek();
            if (ah != null) {
                return ah.getInstanceAID();
            }
        }
        return null;
    }

    /**
     * Return previously selected applet context AIDs.
     * 
     * @return list of previously selected applet context AIDs
     */
    AID[] getPreviousContextAIDs() {
        if (previousAppletContexts != null) {
            return previousAppletContexts.toArray(new AID[0]);
        }
        return new AID[0];
    }

    /**
     * Return <code>Applet</code> by it's AID or null.
     * @param aid applet instance <code>AID</code>
     * @return    applet instance
     */
    Applet getApplet(AID aid) {
        if (aid == null) {
            return null;
        }
        AppletInstanceHolder a = lookupApplet(aid, true);
        if (a == null) {
            return null;
        } else {
            return a.getApplet();
        }
    }

    /**
     * Return <code>Applet class</code> by it's AID or null
     * @param aid applet <code>AID</code>
     * @return    applet class
     */
    Class getAppletClass(AID aid) {
        if (aid == null) {
            return null;
        }
        AppletInstanceHolder a = lookupApplet(aid, false);
        if (a == null) {
            return null;
        } else {
            return a.getAppletClass();
        }
    }
    
    /**
     * Load package for installation.
     * 
     * @param packageDef package definition structure
     */
    void installForLoad(PackageDefinition packageDef) {
        if ((packageDef != null) && (packageDef.APPLETS != null)) {
            try {
                singleProcessLock.lock();

                if (lookupPackage(packageDef.PACKAGE_AID) != null) {
                    SystemException.throwIt(SystemException.ILLEGAL_AID);
                }
                
                ArrayList<AID> rollbackAppletAIDs = new ArrayList();
                
                try {
                    PackageHolder ph = new PackageHolder(packageDef.PACKAGE_AID, packageDef);
                    
                    for (AppletDefinition appletDef : packageDef.APPLETS) {
                        if (appletDef.APPLET_CLASS == null) {
                            SystemException.throwIt(SystemException.ILLEGAL_VALUE);
                        }

                        // assure that applet AIDs are unique
                        if (lookupApplet(appletDef.APPLET_AID, false) != null) {
                            SystemException.throwIt(SystemException.ILLEGAL_AID);
                        }

                        AppletInstanceHolder ah = new AppletInstanceHolder(appletDef.APPLET_CLASS, appletDef.APPLET_AID, ph);
                        applets.put(appletDef.APPLET_AID, ah);
                        rollbackAppletAIDs.add(appletDef.APPLET_AID);
                    }
                    
                    packages.put(packageDef.PACKAGE_AID, ph);
                } catch (RuntimeException e) {
                    for (AID aid : rollbackAppletAIDs) {
                        applets.remove(aid);
                    }
                    throw e;
                }
                
                PersistentMemory pm = SimulatorSystem.getPersistentMemoryInstance();
                if (packageDef.CLASSES != null) {
                    for (Class clazz : packageDef.CLASSES) {
                        // register package's (non-applet) classes for persistent memory management
                        pm.updateStoredClass(clazz, false);
                    }
                }
                for (AppletDefinition appletDef : packageDef.APPLETS) {
                    // register applet classes for persistent memory management
                    pm.updateStoredClass(appletDef.APPLET_CLASS, false);
                }
            } finally {
                singleProcessLock.unlock();
            }
        }
    }
    
    /**
     * Install applet.
     * 
     * @param classAID
     * @param instanceAID
     * @param controlInfo
     * @param appletData 
     */
    void installForInstall(AID classAID, byte[] instanceAID, byte[] controlInfo, byte[] appletData) {
        try {
            singleProcessLock.lock();
            
            AppletInstanceHolder ah = lookupApplet(classAID, false);

            if (ah == null) {
                appletToInstallAID = null;
                SystemException.throwIt(SystemException.ILLEGAL_AID);
            }

            appletToInstallAID = classAID;

            if (instanceAID == null) instanceAID = new byte[0];
            if (controlInfo == null) controlInfo = new byte[0];
            if (appletData == null) appletData = new byte[0];

            if ((instanceAID.length != 0) && ((instanceAID.length < 5) || (instanceAID.length > 16))) {
                SystemException.throwIt(SystemException.ILLEGAL_VALUE);
            }

            if ((3 + instanceAID.length + controlInfo.length + appletData.length) > 127) {
                SystemException.throwIt(SystemException.ILLEGAL_VALUE);
            }

            PersistentMemory pm = SimulatorSystem.getPersistentMemoryInstance();

            AppletInstanceHolder oldSelectedAppletContext = selectedAppletContext;
            ArrayList<AppletInstanceHolder> oldPreviousAppletContexts = new ArrayList<AppletInstanceHolder>(previousAppletContexts);
            AppletInstanceHolder oldCurrentAppletContext = currentAppletContext;
            pm.pushDirtyFlags();
            selectedAppletContext = ah;
            previousAppletContexts.clear();
            previousAppletContexts.push(null);
            currentAppletContext = selectedAppletContext;
            
            try {
                short offset = 0;
                installParameter[offset] = (byte)instanceAID.length;
                offset = Util.arrayCopyNonAtomic(instanceAID, (short)0, installParameter, (short)(offset + 1), (short)instanceAID.length);
                installParameter[offset] = (byte)controlInfo.length;
                offset = Util.arrayCopyNonAtomic(controlInfo, (short)0, installParameter, (short)(offset + 1), (short)controlInfo.length);
                installParameter[offset] = (byte)appletData.length;
                offset = Util.arrayCopyNonAtomic(appletData, (short)0, installParameter, (short)(offset + 1), (short)appletData.length);

                // flag all package classes as dirty
                PackageHolder ph = ah.getPackage();
                if (ph != null) {
                    if (ph.PACKAGE_DEFINITION.CLASSES != null) {
                        for (Class clazz : ph.PACKAGE_DEFINITION.CLASSES) {
                            // register package's (non-applet) classes for persistent memory management
                            pm.updateStoredClass(clazz, false);
                            pm.setDirtyClass(clazz);
                        }
                    }
                    for (AppletDefinition appletDef : ph.PACKAGE_DEFINITION.APPLETS) {
                        // register applet classes for persistent memory management
                        pm.updateStoredClass(appletDef.APPLET_CLASS, false);
                        pm.setDirtyClass(appletDef.APPLET_CLASS);
                    }
                }
                
                Class appletClass = ah.getAppletClass();
                
                Method installMethod = appletClass.getMethod("install", new Class[]{ byte[].class, short.class, byte.class });
                installMethod.invoke(null, new Object[]{ installParameter, new Short((short)0), new Byte((byte)offset) });
            } catch (InvocationTargetException ex) {
                Throwable tx = ex.getTargetException();
                if (tx instanceof CardRuntimeException) {
                    throw (CardRuntimeException)tx;
                } else if (tx instanceof CardException) {
                    SystemException.throwIt(((CardException)tx).getReason());
                } else {
                    ISOException.throwIt(ISO7816.SW_UNKNOWN);
                }
            } catch (Exception ex) {
                SystemException.throwIt(SystemException.ILLEGAL_USE);
            } finally {
                // clear global install parameter array
                Util.arrayFillNonAtomic(installParameter, (short)0, (short)installParameter.length, (byte)0);
                appletToInstallAID = null;
                
                if (SimulatorSystem.getTransactionDepth() != 0) {
                    SimulatorSystem.abortTransaction();
                } else {
                    // write-back object state to persistent memory
                    pm.memoryBarrier(false);
                }
                pm.popDirtyFlags();

                currentAppletContext = oldCurrentAppletContext;
                previousAppletContexts.clear();
                previousAppletContexts.addAll(oldPreviousAppletContexts);
                selectedAppletContext = oldSelectedAppletContext;
            }
        } finally {
            singleProcessLock.unlock();
        }
    }

    /**
     * Register applet upon installation.
     * 
     * @param instanceAID    applet instance AID
     * @param appletInstance applet instance
     */
    void registerApplet(AID instanceAID, Applet appletInstance) {
        AppletInstanceHolder ah = null;

        if ((appletToInstallAID != null) && ((instanceAID == null) || instanceAID.equals(appletToInstallAID) )) {
            ah = lookupApplet(appletToInstallAID, false);
            instanceAID = appletToInstallAID;
        } else if (instanceAID != null) {
            ah = lookupApplet(instanceAID, false);
            
            if (ah == null) {
                AppletInstanceHolder base = lookupApplet(appletToInstallAID, false);
                if (base != null) {
                    ah = base.newInstance();
                    applets.put(instanceAID, ah);
                }
            } else {
                // instance AID exists!
                SystemException.throwIt(SystemException.ILLEGAL_USE);
            }
        }
        if (ah == null) {
            SystemException.throwIt(SystemException.ILLEGAL_AID);
        } else if (ah.isInstalled()) {
            SystemException.throwIt(SystemException.ILLEGAL_USE);
        }
        
        ah.register(instanceAID, appletInstance);
        
        // do not perform a deep refresh (i.e. only add new objects) if we are within a transaction
        boolean noDeepRefresh = SimulatorSystem.getTransactionDepth() != 0;
        
        if (noDeepRefresh) {
            Logging.warn(TAG, "Registering an applet instance within a transaction may lead to undefined behavior");
        }
        
        // register applet instance for persistent memory management
        PersistentMemory pm = SimulatorSystem.getPersistentMemoryInstance();
        pm.updateStoredNamedInstance(appletInstance, ah.getAppletClass(), instanceAID, noDeepRefresh);
        PackageHolder ph = ah.getPackage();
        if (ph != null) {
            if (ph.PACKAGE_DEFINITION.CLASSES != null) {
                for (Class clazz : ph.PACKAGE_DEFINITION.CLASSES) {
                    // register package's (non-applet) classes for persistent memory management
                    pm.updateStoredClass(clazz, noDeepRefresh);
                    pm.bindClassToNamedInstance(clazz, instanceAID);
                }
            }
            for (AppletDefinition appletDef : ph.PACKAGE_DEFINITION.APPLETS) {
                // register applet classes for persistent memory management
                pm.updateStoredClass(appletDef.APPLET_CLASS, noDeepRefresh);
                pm.bindClassToNamedInstance(appletDef.APPLET_CLASS, instanceAID);
            }
        }
        
        appletToInstallAID = null;
    }

    /**
     * Make applet selectable/non-selectable.
     * 
     * @param instanceAID    applet instance AID
     * @param selectable     if true, applet will become selectable; if false, applet will become non-selectable
     */
    void installForMakeSelectable(AID instanceAID, boolean selectable) {
        try {
            singleProcessLock.lock();
            
            AppletInstanceHolder ah = lookupApplet(instanceAID, true);

            if (ah == null) {
                SystemException.throwIt(SystemException.ILLEGAL_AID);
            }

            if (selectable) {
                ah.makeSelectable();
            } else {
                ah.revokeSelectable();
            }
        } finally {
            singleProcessLock.unlock();
        }
    }

    /**
     * Make applet the default applet on a specific interface and logical channel.
     * 
     * @param instanceAID    applet instance AID or null to remove default selection
     * @param interfaceName  interface to modify default selection on or null to address all interfaces
     * @param channel        channel to modify default selection on or -1 to address all channels
     */
    void installForDefaultSelection(AID instanceAID, String interfaceName, byte channel) {
        if ((channel < -1) || (channel > SimulatorConfig.MAX_LOGICAL_CHANNELS)) SystemException.throwIt(SystemException.ILLEGAL_VALUE);
        
        CardInterface ifc;
        if (interfaceName != null) {
            ifc = interfaces.get(interfaceName);
            if (ifc == null) throw new InvalidParameterException("Interface '" + interfaceName + "' does not exist!");
            if (channel > ifc.MAX_CHANNELS) SystemException.throwIt(SystemException.ILLEGAL_VALUE);
        } else {
            ifc = null;
        }
        
        try {
            singleProcessLock.lock();
            
            AppletInstanceHolder ah;
            
            if (instanceAID != null) {
                ah = lookupApplet(instanceAID, true);
                
                if (ah == null) {
                    SystemException.throwIt(SystemException.ILLEGAL_AID);
                }
            } else {
                ah = null;
            }

            if (ifc == null) {
                // apply to all interfaces
                for (CardInterface item : interfaces.values()) {
                    if (item != null) {
                        if (channel == -1) {
                            // apply to all channels
                            for (byte i = 0; i < ifc.MAX_CHANNELS; ++i) {
                                item.setDefaultApplet(ah, i);
                            }
                        } else if (channel < item.MAX_CHANNELS) {
                            item.setDefaultApplet(ah, channel);
                        }
                    }
                }
            } else {
                if (channel == -1) {
                    // apply to all channels
                    for (byte i = 0; i < ifc.MAX_CHANNELS; ++i) {
                        ifc.setDefaultApplet(ah, i);
                    }
                } else {
                    ifc.setDefaultApplet(ah, channel);
                }
            }
        } finally {
            singleProcessLock.unlock();
        }
    }
    
    /**
     * Uninstall a previously installed applet instance.
     * 
     * @param instanceAID applet instance's AID
     */
    void uninstall(AID instanceAID) {
        try {
            singleProcessLock.lock();
            
            AppletInstanceHolder ah = lookupApplet(instanceAID, true);

            if (ah == null) {
                SystemException.throwIt(SystemException.ILLEGAL_AID);
            }

            if (ah.isInstalled()) {
                if (ah.isSelectable()) {
                    installForMakeSelectable(instanceAID, false);
                }
                
                Applet applet = ah.getApplet();
                if ((applet != null) && (applet instanceof AppletEvent)) {
                    PersistentMemory pm = SimulatorSystem.getPersistentMemoryInstance();
                    
                    AppletInstanceHolder oldSelectedAppletContext = selectedAppletContext;
                    ArrayList<AppletInstanceHolder> oldPreviousAppletContexts = new ArrayList<AppletInstanceHolder>(previousAppletContexts);
                    AppletInstanceHolder oldCurrentAppletContext = currentAppletContext;
                    pm.pushDirtyFlags();
                    selectedAppletContext = ah;
                    previousAppletContexts.clear();
                    previousAppletContexts.push(null);
                    currentAppletContext = selectedAppletContext;
                    
                    // flag applet as dirty (all package classes are bound to the applet instance and are therefore implicitly marked as dirty)
                    pm.setDirtyNamedInstance(selectedAppletContext.getInstanceAID());
                    
                    try {
                        ((AppletEvent)applet).uninstall();
                    } catch (Exception e) {
                    }
                    
                    if (SimulatorSystem.getTransactionDepth() != 0) {
                        SimulatorSystem.abortTransaction();
                    } else {
                        // write-back object state to persistent memory
                        pm.memoryBarrier(false);
                    }
                    
                    pm.popDirtyFlags();
                    
                    // cleanup removed applet (delete from named instances in persistent memory)
                    pm.updateStoredNamedInstance(null, null, selectedAppletContext.getInstanceAID(), false);

                    currentAppletContext = oldCurrentAppletContext;
                    previousAppletContexts.clear();
                    previousAppletContexts.addAll(oldPreviousAppletContexts);
                    selectedAppletContext = oldSelectedAppletContext;
                }
                
                ah.uninstall();
                
                if (!instanceAID.equals(ah.getClassAID())) {
                    ah.remove();
                }
            }
            
            if (!instanceAID.equals(ah.getClassAID())) {
                applets.remove(instanceAID);
            }
        } finally {
            singleProcessLock.unlock();
        }
    }
    
    /**
     * Remove a previously loaded package.
     * This method automatically uninstalls all applets contained in the package.
     * 
     * @param packageDef package definition structure
     */
    void remove(PackageDefinition packageDef) {
        if (packageDef != null) {
            remove(packageDef.PACKAGE_AID);
        }
    }
    
    /**
     * Remove a previously loaded package.
     * This method automatically uninstalls all applets contained in the package.
     * 
     * @param packageAID package's AID
     */
    void remove(AID packageAID) {
        try {
            singleProcessLock.lock();
            
            List<Map.Entry<AID, AppletInstanceHolder>> appletSet = new ArrayList(applets.entrySet());
            for (Map.Entry<AID, AppletInstanceHolder> entry : appletSet) {
                AppletInstanceHolder ah = entry.getValue();
                
                if ((ah != null) && packageAID.equals(ah.getPackageAID())) {
                    AID aid = entry.getKey();
                    
                    if (ah.isInstalled()) {
                        uninstall(aid);
                    }
                    
                    ah.remove();
                    applets.remove(aid);
                }
            }
            
            SimulatorSystem.deleteTransientMemorySegments(packageAID);
            packages.remove(packageAID);
        } finally {
            singleProcessLock.unlock();
        }
    }

    /**
     * Transceive APDU with Java Card emulator environment.
     * 
     * @param interfaceName name of interface used for APDU exchange
     * @param commandAPDU   command APDU
     * @return              response APDU
     * @throws InvalidParameterException if specified interface does not exist
     */
    byte[] transceiveAPDU(String interfaceName, byte[] command) throws InvalidParameterException {
        CardInterface ifc = interfaces.get(interfaceName);
        if (ifc == null) throw new InvalidParameterException("Interface '" + interfaceName + "' does not exist!");

        CommandAPDU commandAPDU = null;
        ResponseAPDU responseAPDU = null;
        byte[] response = null;
        
        try {
            singleProcessLock.lock();  // serialize APDU exchange

            try {
                activeInterface = ifc;

                Logging.info(TAG, "transceiveAPDU: Interface = " + interfaceName);
                Logging.info(TAG, "transceiveAPDU: C-APDU = " + StringUtils.convertByteArrayToHexString(command).toUpperCase());

                try {
                    commandAPDU = new CommandAPDU(command);
                } catch (IllegalArgumentException e) {
                    ISOException.throwIt(ISO7816.SW_UNKNOWN);
                }

                if (commandAPDU != null) {
                    activeChannel = commandAPDU.getCLAChannel();
                    Logging.info(TAG, "transceiveAPDU: Channel = " + Byte.toString(activeChannel));

                    selectedAppletContext = null;
                    currentAppletContext = null; // current context is RTE
                    previousAppletContexts.clear(); // clear context switching stack

                    if (commandAPDU.isExtendedLength() &&
                        (!SimulatorConfig.EXTENDED_LENGTH_SUPPORT ||
                         commandAPDU.getNc() > SimulatorConfig.EXTENDED_LENGTH_MAXIMUM)) {
                        // C-APDU is extended length type but implementation options
                        // does not permit extended length APDUs or command data field
                        // exceeds Java Card extended length dimensions
                        ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
                    }

                    if (commandAPDU.isISOInterindustryCLA()) {
                        final byte ins = commandAPDU.getINS();
                        final byte p1 = commandAPDU.getP1();
                        final byte p2 = commandAPDU.getP2();
                        final int nc = commandAPDU.getNc();
                        final int ne = commandAPDU.getNe();

                        if (ins == ISO7816Extended.INS_MANAGE_CHANNEL) {
                            // MANAGE_CHANNEL
                            Logging.info(TAG, "transceiveAPDU: MANAGE_CHANNEL");

                            if (commandAPDU.isCommandChainingCLA()) ISOException.throwIt(ISO7816.SW_COMMAND_CHAINING_NOT_SUPPORTED);
                            if (commandAPDU.isSecureMessagingCLA()) ISOException.throwIt(ISO7816.SW_SECURE_MESSAGING_NOT_SUPPORTED);

                            if ((p1 & 0x07F) != 0) ISOException.throwIt(ISO7816.SW_FUNC_NOT_SUPPORTED);

                            if (!activeInterface.isOpen(activeChannel)) ISOException.throwIt(ISO7816.SW_LOGICAL_CHANNEL_NOT_SUPPORTED);

                            if (activeInterface.MAX_CHANNELS <= 1) ISOException.throwIt(ISO7816.SW_LOGICAL_CHANNEL_NOT_SUPPORTED);


                            if (p1 == 0) {
                                // OPEN
                                Logging.info(TAG, "transceiveAPDU: MANAGE_CHANNEL operation = OPEN");

                                if ((p2 < 0) || (p2 >= activeInterface.MAX_CHANNELS)) ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
                                if ((p2 > 0) && activeInterface.isOpen(p2)) ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
                                if ((p2 == 0) && (ne != 1)) ISOException.throwIt((short)((ISO7816.SW_CORRECT_LENGTH_00 & 0x0ffff) + 1));

                                byte newChannel;
                                if (p2 == 0) {
                                    newChannel = activeInterface.open();
                                } else {
                                    newChannel = activeInterface.open(p2);
                                }
                                if (newChannel == -1) ISOException.throwIt(ISO7816.SW_FUNC_NOT_SUPPORTED);

                                AppletInstanceHolder defaultApplet = activeInterface.getDefaultApplet(newChannel, activeChannel);

                                if (defaultApplet != null) {
                                    activeChannel = newChannel;

                                    try {
                                        selectApplet(defaultApplet, null);
                                    } catch (CardRuntimeException e) {
                                        activeInterface.close(activeChannel);
                                        throw e;
                                    }
                                }

                                if (p2 == 0) {
                                    responseAPDU = new ResponseAPDU(new byte[]{ activeChannel }, 1, ISO7816.SW_NO_ERROR);
                                } else {
                                    responseAPDU = new ResponseAPDU(null, 0, ISO7816.SW_NO_ERROR);
                                }
                            } else {
                                // CLOSE
                                Logging.info(TAG, "transceiveAPDU: MANAGE_CHANNEL operation = CLOSE");

                                if (p2 != 0) {
                                    activeChannel = p2;
                                }
                                if (activeChannel == CardChannel.BASIC_CHANNEL) ISOException.throwIt(ISO7816.SW_FUNC_NOT_SUPPORTED);
                                if ((activeChannel < 0) || (activeChannel >= activeInterface.MAX_CHANNELS)) ISOException.throwIt(ISO7816.SW_FUNC_NOT_SUPPORTED);
                                if (!activeInterface.isOpen(activeChannel)) ISOException.throwIt(ISO7816.SW_WARNING_STATE_UNCHANGED);

                                try {
                                    selectApplet((AppletInstanceHolder)null, null);
                                } catch (Exception e) {
                                }

                                activeInterface.close(activeChannel);

                                responseAPDU = new ResponseAPDU(null, 0, ISO7816.SW_NO_ERROR);
                            }
                        } else if ((ins == ISO7816.INS_SELECT) &&
                                   !commandAPDU.isSecureMessagingCLA() &&
                                   (p1 == ISO7816Extended.P1_SELECT_BY_DF_NAME) &&
                                   ((p2 & 0x0E0) == 0)) {
                            // SELECT APPLET
                            Logging.info(TAG, "transceiveAPDU: SELECT APPLET");

                            if (commandAPDU.isCommandChainingCLA()) ISOException.throwIt(ISO7816.SW_COMMAND_CHAINING_NOT_SUPPORTED);

                            if (nc > 16) ISOException.throwIt(ISO7816.SW_WRONG_DATA);

                            final byte[] partialAID = new byte[nc];
                            commandAPDU.getCommandData(partialAID, (short)0);
                            final byte matchingType = (byte)(p2 & 0x003);

                            responseAPDU = selectAppletByPartialAID(partialAID, matchingType, commandAPDU);
                        } else {
                            // applet specific command
                            Logging.info(TAG, "transceiveAPDU: INTER-INDUSTRY APPLET-SPECIFIC COMMAND");

                            if (!activeInterface.isOpen(activeChannel)) ISOException.throwIt(ISO7816.SW_LOGICAL_CHANNEL_NOT_SUPPORTED);

                            selectedAppletContext = activeInterface.getCurrentSelectedApplet(activeChannel); // switch into applet context
                            previousAppletContexts.push(currentAppletContext);
                            currentAppletContext = selectedAppletContext;
                            if (selectedAppletContext == null) ISOException.throwIt(ISO7816.SW_APPLET_SELECT_FAILED);

                            responseAPDU = processCommand(commandAPDU);
                        }
                    } else {
                        // applet specific command
                        Logging.info(TAG, "transceiveAPDU: APPLET-SPECIFIC COMMAND");

                        if (!activeInterface.isOpen(activeChannel)) ISOException.throwIt(ISO7816.SW_LOGICAL_CHANNEL_NOT_SUPPORTED);

                        selectedAppletContext = activeInterface.getCurrentSelectedApplet(activeChannel); // switch into applet context
                        previousAppletContexts.push(currentAppletContext);
                        currentAppletContext = selectedAppletContext;
                        if (selectedAppletContext == null) ISOException.throwIt(ISO7816.SW_APPLET_SELECT_FAILED);

                        responseAPDU = processCommand(commandAPDU);
                    }
                }
            } catch (Throwable e) {
                Logging.info(TAG, "transceiveAPDU: Processing exception", e);
                
                if (e instanceof CardRuntimeException) {
                    responseAPDU = new ResponseAPDU(null, 0, ((CardRuntimeException)e).getReason());
                } else if (e instanceof CardException) {
                    responseAPDU = new ResponseAPDU(null, 0, ((CardException)e).getReason());
                } else if (e instanceof RuntimeException) {
                    throw (RuntimeException)e;
                } else {
                    responseAPDU = null;
                }
            }

            if (responseAPDU == null) {
                responseAPDU = new ResponseAPDU(null, 0, ISO7816.SW_UNKNOWN);
            }

            response = responseAPDU.toBytes();
            
            PersistentMemory pm = SimulatorSystem.getPersistentMemoryInstance();
            pm.garbageCollect(true);

            Logging.info(TAG, "transceiveAPDU: R-APDU = " + StringUtils.convertByteArrayToHexString(response).toUpperCase());
        } finally {
            try {
                selectedAppletContext = null;
                currentAppletContext = null; // current context is RTE
                previousAppletContexts.clear(); // clear context switching stack
                activeInterface = null;
                activeChannel = 0;
            } finally {
                singleProcessLock.unlock();  // serialize APDU exchange (release serialization lock)
            }
        }
        
        return response;
    }

//    private ResponseAPDU selectAppletByAID(AID instanceAID, CommandAPDU selectCommand) {
//        return selectApplet(lookupApplet(instanceAID, true), selectCommand);
//    }
    
    private ResponseAPDU selectAppletByPartialAID(byte[] partialInstanceAID, byte matchingType, CommandAPDU selectCommand) {
        Logging.debug(TAG, "selectAppletByPartialAID: Looking for AID " + StringUtils.convertByteArrayToHexString(partialInstanceAID).toUpperCase());
        return selectApplet(findApplet(partialInstanceAID, matchingType, true), selectCommand);
    }
    
    private ResponseAPDU selectApplet(AppletInstanceHolder appletInstance, CommandAPDU selectCommand) {
        if (activeInterface == null) throw new InvalidParameterException("No active interface!");
        if (!activeInterface.isOpen(activeChannel)) activeChannel = activeInterface.open(activeChannel);
        if (activeChannel == -1) ISOException.throwIt(ISO7816.SW_LOGICAL_CHANNEL_NOT_SUPPORTED);
        
        PersistentMemory pm = SimulatorSystem.getPersistentMemoryInstance();
        
        selectedAppletContext = activeInterface.getCurrentSelectedApplet(activeChannel); // switch into old applet context
        previousAppletContexts.push(currentAppletContext);
        currentAppletContext = selectedAppletContext;

        if (selectedAppletContext != null) {
            if ((appletInstance == null) && (selectCommand != null)) {
                Logging.debug(TAG, "selectApplet: Forwarding command to current applet");
                return processCommand(selectCommand);
            } else {
                // deselect currently selected applet instance (if any)
                if (selectedAppletContext.isSelected()) {
                    pm.pushDirtyFlags();
                    
                    // flag applet as dirty (all package classes are bound to the applet instance and are therefore implicitly marked as dirty)
                    pm.setDirtyNamedInstance(selectedAppletContext.getInstanceAID());
                    
                    try {
                        if (selectedAppletContext.isPackageMultiSelected()) {
                            Logging.debug(TAG, "selectApplet: Multi-deselecting applet " + __AIDWrapper.getAIDString(selectedAppletContext.getInstanceAID()) + " (" + selectedAppletContext.getAppletClass().getName() + ")");
                            ((MultiSelectable)selectedAppletContext.getApplet()).deselect(selectedAppletContext.isMultiSelected());
                        } else {
                            Logging.debug(TAG, "selectApplet: Deselecting applet " + __AIDWrapper.getAIDString(selectedAppletContext.getInstanceAID()) + " (" + selectedAppletContext.getAppletClass().getName() + ")");
                            selectedAppletContext.getApplet().deselect();
                        }
                    } catch (Exception e) {
                        // ignore all exceptions
                    }
                    if (SimulatorSystem.getTransactionDepth() != 0) {
                        SimulatorSystem.abortTransaction();
                    } else {
                        // write-back object state to persistent memory
                        pm.memoryBarrier(false);
                    }
                    
                    pm.popDirtyFlags();
                    
                    selectedAppletContext.deselect();
                    if (!selectedAppletContext.isPackageSelected()) {
                        SimulatorSystem.clearTransientMemory(JCSystem.CLEAR_ON_DESELECT);
                    }
                    activeInterface.select(null, activeChannel);
                    selectedAppletContext = null;
                    currentAppletContext = null;
                    previousAppletContexts.clear();
                }
            }
        }
        
        if (appletInstance == null) {
            activeInterface.select(null, activeChannel);
            ISOException.throwIt(ISO7816.SW_APPLET_SELECT_FAILED);
        } else {
            if (!appletInstance.isSelectable()) ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);

            selectedAppletContext = appletInstance;  // switch into new applet context
            previousAppletContexts.push(currentAppletContext);
            currentAppletContext = selectedAppletContext;

            // ... and select new applet
            selectedAppletContext.select();
            
            pm.pushDirtyFlags();
            
            // flag applet as dirty (all package classes are bound to the applet instance and are therefore implicitly marked as dirty)
            pm.setDirtyNamedInstance(selectedAppletContext.getInstanceAID());
            
            boolean selectResult;
            try {
                if (selectedAppletContext.isPackageSelected()) {
                    Logging.debug(TAG, "selectApplet: Multi-selecting applet " + __AIDWrapper.getAIDString(selectedAppletContext.getInstanceAID()) + " (" + selectedAppletContext.getAppletClass().getName() + ")");
                    selectResult = ((MultiSelectable)selectedAppletContext.getApplet()).select(selectedAppletContext.isSelected());
                } else {
                    Logging.debug(TAG, "selectApplet: Selecting applet " + __AIDWrapper.getAIDString(selectedAppletContext.getInstanceAID()) + " (" + selectedAppletContext.getAppletClass().getName() + ")");
                    selectResult = selectedAppletContext.getApplet().select();
                }
            } catch (Exception e) {
                selectResult = false;
                Logging.debug(TAG, "selectApplet: Select exception", e);
            }
            
            if (SimulatorSystem.getTransactionDepth() != 0) {
                SimulatorSystem.abortTransaction();
            } else {
                // write-back object state to persistent memory
                pm.memoryBarrier(false);
            }
            
            pm.popDirtyFlags();
            
            if (!selectResult) {
                selectedAppletContext.cancelSelect();
                activeInterface.select(null, activeChannel);
                selectedAppletContext = null;
                currentAppletContext = null;
                previousAppletContexts.clear();
                ISOException.throwIt(ISO7816.SW_APPLET_SELECT_FAILED);
            }
            
            ResponseAPDU responseAPDU = null;
            
            if (selectCommand != null) {
                try {
                    Logging.debug(TAG, "selectApplet: Forwarding SELECT command to newly selected applet");
                    responseAPDU = processCommand(selectCommand);
                } catch (CardRuntimeException e) {
                    selectedAppletContext.cancelSelect();
                    activeInterface.select(null, activeChannel);
                    selectedAppletContext = null;
                    currentAppletContext = null;
                    previousAppletContexts.clear();
                    throw e;
                }
            } else {
                responseAPDU = new ResponseAPDU(null, 0, ISO7816.SW_NO_ERROR);
            }
            
            selectedAppletContext.selected();
            activeInterface.select(selectedAppletContext, activeChannel);
            
            return responseAPDU;
        }
        
        return null;
    }

    private ResponseAPDU processCommand(CommandAPDU command) {
        if (activeInterface == null) throw new InvalidParameterException("No active interface!");
        if (!activeInterface.isOpen(activeChannel)) activeChannel = activeInterface.open(activeChannel);
        if (activeChannel == -1) ISOException.throwIt(ISO7816.SW_LOGICAL_CHANNEL_NOT_SUPPORTED);
        if (command == null) ISOException.throwIt(ISO7816.SW_LOGICAL_CHANNEL_NOT_SUPPORTED);

        // processCommand requires that the currentAppletContext is setup and ready
        if ((selectedAppletContext != null) &&
            (selectedAppletContext.isSelected() || selectedAppletContext.isSelecting())) {
            
            PersistentMemory pm = SimulatorSystem.getPersistentMemoryInstance();
            pm.pushDirtyFlags();
            
            APDU apdu = APDU.getCurrentAPDU();
            
            try {
                // in case we process an extended length APDU make sure that the applet supports it
                if (command.isExtendedLength() && !selectedAppletContext.hasExtendedLengthSupport()) ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);

                commandBuffer = command.getCommand();
                final int apduNc = command.getNc();

                commandBufferOffset = command.getOffsetCData();
                int apduNe = command.getNe();
                if (apduNe > SimulatorConfig.EXTENDED_LENGTH_MAXIMUM) apduNe = SimulatorConfig.EXTENDED_LENGTH_MAXIMUM;
                responseBufferSize = 0;
                responseExpectedLength = apduNe;

                apdu.load(commandBuffer, (short)0, (short)apduNc, activeChannel, command.isExtendedLength());

                // flag applet as dirty (all package classes are bound to the applet instance and are therefore implicitly marked as dirty)
                pm.setDirtyNamedInstance(selectedAppletContext.getInstanceAID());
                
                Logging.debug(TAG, "selectApplet: Processing command with applet " + __AIDWrapper.getAIDString(selectedAppletContext.getInstanceAID()) + " (" + selectedAppletContext.getAppletClass().getName() + ")");
                selectedAppletContext.getApplet().process(apdu);

                return new ResponseAPDU(responseBuffer, responseBufferSize, ISO7816.SW_NO_ERROR);
            } finally {
                if (SimulatorSystem.getTransactionDepth() != 0) {
                    SimulatorSystem.abortTransaction();
                } else {
                    // write-back object state to persistent memory
                    pm.memoryBarrier(false);
                }
                
                pm.popDirtyFlags();

                commandBufferOffset = 0;
                responseBufferSize = 0;
                responseExpectedLength = 0;
                commandBuffer = null;
                Arrays.fill(responseBuffer, (byte)0);
                apdu.reset();
            }
        } else {
            // no active applet context or applet context is not selected (the
            // latter indicates inconsistent state that should never happen)
            if (!command.isISOInterindustryCLA()) {
                ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
            } else if (command.getINS() == ISO7816.INS_SELECT) {
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
            } else {
                ISOException.throwIt(ISO7816.SW_WRONG_P1P2);
            }
        }
        
        return null;
    }

    /**
     * Get the expected response length (Ne) for the current APDU.
     * 
     * @return Ne
     */
    short receiveNe() {
        return (short)responseExpectedLength;
    }

    /**
     * Receive a number of bytes of the current APDU into the specified buffer.
     * 
     * @param buffer  receive buffer
     * @param bOff    starting offset in buffer for receiving APDU bytes
     * @param len     maximum number of bytes to write into buffer
     * @return        remaining number of bytes not yet received into buffer
     */
    short receiveAPDU(byte[] buffer, short bOff, short len) {
        if (commandBuffer != null) {
            int newOffset = commandBufferOffset + len;
            if (newOffset > commandBuffer.length) {
                newOffset = commandBuffer.length;
                len = (short)(newOffset - commandBufferOffset);
            }
            System.arraycopy(commandBuffer, commandBufferOffset, buffer, bOff, len);
            commandBufferOffset = newOffset;
            return (short)(commandBuffer.length - commandBufferOffset);
        } else {
            return 0;
        }
    }
    
    /**
     * Copy response bytes to internal buffer
     * 
     * @param buffer source byte array
     * @param bOff the starting offset in buffer
     * @param len the length in bytes of the response
     */
    void sendAPDU(byte[] buffer, short bOff, short len) {
        System.arraycopy(buffer, bOff, responseBuffer, responseBufferSize, len);
        responseBufferSize += len;
    }

    /**
     * Check if applet is currently being selected.
     * 
     * @param applet applet to be checked
     * @return true if applet is currently being selected, else false
     */
    boolean isAppletSelecting(Applet applet) {
        if (selectedAppletContext != null) {
            if (selectedAppletContext.getApplet() == applet) {
                return selectedAppletContext.isSelecting();
            }
        }
        
        return false;
    }

    /**
     * Get protocol used on currently active interface.
     * 
     * @return protocol byte for currently active interface, or 0 if no interface is active
     */
    byte getCurrentProtocol() {
        if (activeInterface != null) {
            return activeInterface.PROTOCOL;
        } else {
            return (byte)0;
        }
    }

    /**
     * Get currently active channel.
     * 
     * @return channel number of currently active channel
     */
    byte getCurrentlySelectedChannel() {
        return activeChannel;
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
    boolean isAppletActive(AID theApplet) {
        AppletInstanceHolder ah = lookupApplet(theApplet, true);
        if ((ah != null) && ah.isInstalled()) {
            return ah.isSelected() || ah.isSelecting();
        } else {
            return false;
        }
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
    public Shareable getSharedObject(AID serverAID, byte parameter) {
        Applet serverApplet = getApplet(serverAID);
        if (serverApplet != null) {
            PersistentMemory pm = SimulatorSystem.getPersistentMemoryInstance();
            
            // mark serverApplet as dirty
            pm.setDirtyNamedInstance(serverAID);
            
            Shareable shareableInterface = serverApplet.getShareableInterfaceObject(getCurrentContextAID(), parameter);
            // Register shareable interface with persistent memory processing (if not already registered,
            // but be careful not to update its FieldState if it exists and we are within a transaction)
            if (shareableInterface != null) {
                // do not perform a deep refresh (i.e. only add new objects) if we are within a transaction
                boolean noDeepRefresh = SimulatorSystem.getTransactionDepth() != 0;

                // add shareable interface to persistent memory (if it does not already exist) and mark it as dirty
                pm.updateStoredReference(shareableInterface, shareableInterface.getClass(), noDeepRefresh);
                pm.setDirtyReference(shareableInterface);
                
                // Bind server package's classes to this shareable object
                AppletInstanceHolder ah = applets.get(serverAID);
                if (ah != null) {
                    PackageHolder ph = ah.getPackage();
                    if (ph != null) {
                        if (ph.PACKAGE_DEFINITION.CLASSES != null) {
                            for (Class clazz : ph.PACKAGE_DEFINITION.CLASSES) {
                                // bind server package's (non-applet) classes to the shareable interface for persistent memory management
                                pm.updateStoredClass(clazz, noDeepRefresh);
                                pm.bindClassToObject(clazz, shareableInterface);
                            }
                        }
                        for (AppletDefinition appletDef : ph.PACKAGE_DEFINITION.APPLETS) {
                            // bind server package's applet classes to the shareable interface for persistent memory management
                            pm.updateStoredClass(appletDef.APPLET_CLASS, noDeepRefresh);
                            pm.bindClassToObject(appletDef.APPLET_CLASS, shareableInterface);
                        }
                    }
                }
            }
            return shareableInterface;
        }
        return null;
    }
    
    void resetCard() {
        try {
            singleProcessLock.lock();

            // cancel ongoing transactions (this should never happen, right?)
            if (SimulatorSystem.getTransactionDepth() != 0) {
                SimulatorSystem.abortTransaction();
            }
            
            // cancel ongoing installation attempts
            Util.arrayFillNonAtomic(installParameter, (short)0, (short)installParameter.length, (byte)0);
            appletToInstallAID = null;
            
            // clear APDU buffers
            commandBufferOffset = 0;
            responseBufferSize = 0;
            responseExpectedLength = 0;
            commandBuffer = null;
            Arrays.fill(responseBuffer, (byte)0);
            APDU apdu = APDU.getCurrentAPDU();
            if (apdu != null) {
                apdu.reset();
            }
            
            // clear context stack
            selectedAppletContext = null;
            currentAppletContext = null;
            previousAppletContexts.clear();
            
            // reset all interfaces
            activeInterface = null;
            activeChannel = 0;
            
            for (String interfaceName : interfaces.keySet()) {
                resetInterface(interfaceName);
            }
            
            // clear all transient memory
            SimulatorSystem.clearTransientMemory(JCSystem.CLEAR_ON_RESET);
            
            // if packages are loaded but have no installed applets,
            // these packages (and their applets) should be removed upon reset:
            ArrayList<AID> deletablePackages = new ArrayList<AID>(packages.keySet());
            Map<AID, AppletInstanceHolder> appletMap = new HashMap<AID, AppletInstanceHolder>(applets);  // copy to permit remove-operations on original
            for (Map.Entry<AID, AppletInstanceHolder> entry : appletMap.entrySet()) {
                AppletInstanceHolder ah = entry.getValue();

                if ((ah != null) && ah.isInstalled()) {
                    // if there is an applet installed, don't mark the associated package for removal
                    deletablePackages.remove(ah.getPackageAID());
                }
            }
            // remove all applets of packages marked for removal
            for (Map.Entry<AID, AppletInstanceHolder> entry : appletMap.entrySet()) {
                AID appletAID = entry.getKey();
                AppletInstanceHolder ah = entry.getValue();

                if ((ah != null)  && deletablePackages.contains(ah.getPackageAID())) {
                    ah.remove();
                    applets.remove(appletAID);
                }
            }
            // remove packages marked for removal
            for (AID packageAID : deletablePackages) {
                SimulatorSystem.deleteTransientMemorySegments(packageAID);
                packages.remove(packageAID);
                // Can't remove package classes from persistent memory management as we can't be
                // 100% sure that they are not used by multiple packages. Though strict adherence
                // to JavaCard specifications would prevent this as only interfaces can be shared
                // across JC package namespaces, right?
                // However, classes that don't exist during de-serialization will be wiped
                // ("garbage collected") from persistent memory anyways!!!
            }
            deletablePackages.clear();
        } finally {
            singleProcessLock.unlock();
        }
    }
    
    void resetInterface(String interfaceName) {
        CardInterface ifc = interfaces.get(interfaceName);
        if (ifc == null) throw new InvalidParameterException("Interface '" + interfaceName + "' does not exist!");
        
        try {
            singleProcessLock.lock();
            
            AppletInstanceHolder rememberSelectedAppletContext = selectedAppletContext;
            CardInterface rememberActiveInterface = activeInterface;
            byte rememberActiveChannel = activeChannel;
            
            activeInterface = ifc;
            
            for (activeChannel = 0; activeChannel < activeInterface.MAX_CHANNELS; ++activeChannel) {
                if (activeInterface.isOpen(activeChannel)) {
                    // channel is open
                    
                    // -> deselect applet (if any)
                    selectedAppletContext = activeInterface.getCurrentSelectedApplet(activeChannel);
                    if (selectedAppletContext.isSelected()) {
                        // implicit deselect (i.e. do not call Applet.deselect() or MultiSelectable.deselect())
                        selectedAppletContext.deselect();
                    }
                    
                    if (!selectedAppletContext.isPackageSelected()) {
                        SimulatorSystem.clearTransientMemory(JCSystem.CLEAR_ON_RESET);
                    }
                    
                    // close channel
                    activeInterface.close(activeChannel);
                }
            }
            
            selectedAppletContext = rememberSelectedAppletContext;
            activeInterface = rememberActiveInterface;
            activeChannel = rememberActiveChannel;
        } finally {
            singleProcessLock.unlock();
        }
    }
    
    void resetRuntime() {
        try {
            singleProcessLock.lock();
            
            // reset card
            resetCard();
            
            // reset transient memory
            SimulatorSystem.resetTransientMemory();
            
            // remove applets & packages
            for (AppletInstanceHolder ah : applets.values()) {
                if (ah != null) {
                    ah.uninstall();
                    ah.remove();
                }
            }
            applets.clear();
            packages.clear();
            
            // re-initialize all interfaces
            interfaces.clear();
            for (CardInterface.InterfaceConfig ifcfg : SimulatorConfig.INTERFACES) {
                if (ifcfg != null) {
                    interfaces.put(ifcfg.NAME, new CardInterface(ifcfg.NAME, ifcfg.PROTOCOL, ifcfg.MAX_CHANNELS));
                }
            }

            // reset persistent memory
            SimulatorSystem.resetPersistentMemory();
        } finally {
            singleProcessLock.unlock();
        }
    }
    
    
    /**
     * Save runtime state to persistent memory.
     */
    public void saveState(PersistentMemory pm) {
        // loaded packages
        if (packages != null) {
            Collection<PackageHolder> pkgCollection = packages.values();
            if (pkgCollection != null) {
                PackageHolder[] pkgs = pkgCollection.toArray(new PackageHolder[0]);
                pm.updateStoredNamedInstance(pkgs, null, "runtime:packages", false);
            }
        }
        
        // loaded/installed applets
        if (applets != null) {
            Collection<AppletInstanceHolder> appletCollection = applets.values();
            if (appletCollection != null) {
                AppletInstanceHolder[] aplts = appletCollection.toArray(new AppletInstanceHolder[0]);
                pm.updateStoredNamedInstance(aplts, null, "runtime:applets", false);
            }
        }
        
        // channels: default selected applets
        if (interfaces != null) {
            for (CardInterface iface : interfaces.values()) {
                for (int i = 0; i < iface.MAX_CHANNELS; ++i) {
                    AppletInstanceHolder ah = iface.getDefaultApplet((byte)(i & 0x0ff), CardChannel.BASIC_CHANNEL);
                    pm.updateStoredNamedInstance(ah, null, "runtime:channels:" + iface.NAME + ":" + Integer.toString(i), false);
                }
            }
        }
    }

    /**
     * Load runtime state from persistent memory.
     */
    public void loadState(PersistentMemory pm) {
        // loaded packages
        packages.clear();
        PackageHolder[] pkgs = (PackageHolder[])pm.getNamedInstance("runtime:packages");
        if (pkgs != null) {
            for (PackageHolder pkg : pkgs) {
                if (pkg != null) {
                    pkg.reset();
                    packages.put(pkg.getPackageAID(), pkg);
                }
            }
        }
        
        // loaded/installed applets
        applets.clear();
        AppletInstanceHolder[] aplts = (AppletInstanceHolder[])pm.getNamedInstance("runtime:applets");
        if (aplts != null) {
            for (AppletInstanceHolder applet : aplts) {
                if (applet != null) {
                    applet.reset();
                    applets.put(applet.getClassAID(), applet);
                }
            }
        }
        
        // channels: default selected applets
        if (interfaces != null) {
            for (CardInterface iface : interfaces.values()) {
                for (int i = 0; i < iface.MAX_CHANNELS; ++i) {
                    AppletInstanceHolder ah = (AppletInstanceHolder)pm.getNamedInstance("runtime:channels:" + iface.NAME + ":" + Integer.toString(i));
                    if (ah != null) {
                        iface.setDefaultApplet(ah, (byte)(i & 0x0ff));
                    }
                }
            }
        }
    }
}

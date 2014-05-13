/*
 * Copyright 2013 FH OOe Forschungs & Entwicklungs GmbH, Michael Roland.
 * Copyright 2013 Licel LLC.
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

import at.mroland.objectstaterecovery.PersistentMemory;
import javacard.framework.AID;
import javacard.framework.Applet;
import javacard.framework.MultiSelectable;
import javacard.framework.SystemException;
import javacardx.apdu.ExtendedLength;

/**
 * Internal class which is holds an Applet instance and it's state.
 */
class AppletInstanceHolder {
    
    /**
     * Applet flags.
     */
    private boolean[] flags;
    /**
     * Applet flag: Applet is loaded.
     */
    private static final byte FLAG_LOADED = 0;
    /**
     * Applet flag: Applet is installed.
     */
    private static final byte FLAG_INSTALLED = FLAG_LOADED+1;
    /**
     * Applet flag: Applet is selectable.
     */
    private static final byte FLAG_SELECTABLE = FLAG_INSTALLED+1;
    /**
     * Applet flag: Applet is currently being selected.
     */
    private static final byte FLAG_SELECTING = FLAG_SELECTABLE+1;
    /**
     * Applet flag: Applet is currently selected.
     */
    private static final byte FLAG_SELECTED = FLAG_SELECTING+1;
    /**
     * Applet flag: Applet supports multi-selection.
     */
    private static final byte FLAG_MULTI_SELECTABLE = FLAG_SELECTED+1;
    /**
     * Applet flag: Applet supports extended-length APDUs.
     */
    private static final byte FLAG_EXTENDED_LENGTH = FLAG_MULTI_SELECTABLE+1;
    /**
     * Total length of flags.
     */
    private static final byte FLAGS_LENGTH = FLAG_EXTENDED_LENGTH+1;
    
    /**
     * Applet class.
     */
    private Class appletClass;
    
    /**
     * Applet instance.
     */
    private Applet applet;

    /**
     * Applet package.
     */
    private PackageHolder appletPackage;
    
    /**
     * Applet AID.
     */
    private AID aidAppletClass;
    
    /**
     * Applet instance AID.
     */
    private AID aidAppletInstance;
    
    /**
     * Applet instance selection counter for multi-selectable applets.
     */
    private short multiSelectCount;
    
    /**
     * Construct an <code>AppletInstanceHolder</code> for a given applet class and implicitly perform load operation.
     * @param appletClass    applet class
     * @param appletClassAID applet's class AID (not necessarily equal to the applet's selectable instance AID!)
     * @param appletPackage  applet's package
     */
    AppletInstanceHolder(Class appletClass, AID appletClassAID, PackageHolder appletPackage) {
        this.flags = new boolean[FLAGS_LENGTH];
        this.appletClass = appletClass;
        this.applet = null;
        this.appletPackage = null;
        this.aidAppletClass = null;
        this.aidAppletInstance = null;
        this.multiSelectCount = 0;
        if (MultiSelectable.class.isAssignableFrom(appletClass)) {
            this.flags[FLAG_MULTI_SELECTABLE] = true;
        }
        if (ExtendedLength.class.isAssignableFrom(appletClass)) {
            this.flags[FLAG_EXTENDED_LENGTH] = true;
        }
        load(appletClassAID, appletPackage);
    }

    /**
     * Get applet class.
     * 
     * @return applet class
     */
    Class getAppletClass() {
        return appletClass;
    }
    
    /**
     * Get applet package AID.
     * 
     * @return package AID
     */
    AID getPackageAID() {
        if (appletPackage != null) {
            return appletPackage.getPackageAID();
        } else {
            return null;
        }
    }

    /**
     * Get applet package.
     * 
     * @return package
     */
    PackageHolder getPackage() {
        return appletPackage;
    }

    /**
     * Get applet class AID.
     * 
     * @return applet class AID
     */
    AID getClassAID() {
        return aidAppletClass;
    }
    
    /**
     * Get applet instance AID.
     * 
     * @return applet instance AID
     */
    AID getInstanceAID() {
        return aidAppletInstance;
    }
    
    /**
     * Get applet instance.
     * 
     * @return applet instance
     */
    Applet getApplet() {
        if ((applet == null) && (aidAppletInstance != null)) {
            PersistentMemory pm = SimulatorSystem.getPersistentMemoryInstance();
            applet = (Applet)pm.getNamedInstance(aidAppletInstance);
        }
        return applet;
    }

    /**
     * Mark applet as loaded.
     * @param appletClassAID applet's class AID (not necessarily equal to the applet's selectable instance AID!)
     * @param appletPackage  applet's package
     */
    final void load(AID appletClassAID, PackageHolder appletPackage) {
        if (flags[FLAG_LOADED]) {
            SystemException.throwIt(SystemException.ILLEGAL_USE);
        }
        this.appletPackage = appletPackage;
        aidAppletClass = appletClassAID;
        flags[FLAG_LOADED] = true;
        flags[FLAG_INSTALLED] = false;
        flags[FLAG_SELECTABLE] = false;
        flags[FLAG_SELECTING] = false;
        flags[FLAG_SELECTED] = false;
        
        if (flags[FLAG_MULTI_SELECTABLE] != appletPackage.hasMultiSelectSupport()) {
            // JC 2.2.2, RTE Spec.: All applets within a package SHALL be multiselectable or none shall be.
            SystemException.throwIt(SystemException.ILLEGAL_USE);
        }
    }

    /**
     * Mark applet as installed and registered.
     * 
     * @param appletInstanceAID applet's instance AID
     * @param appletInstance    applet instance
     */
    void register(AID appletInstanceAID, Applet appletInstance) {
        if (!flags[FLAG_LOADED]) {
            SystemException.throwIt(SystemException.ILLEGAL_USE);
        }
        applet = appletInstance;
        aidAppletInstance = appletInstanceAID;
        flags[FLAG_INSTALLED] = true;
        flags[FLAG_SELECTABLE] = false;
        flags[FLAG_SELECTING] = false;
        flags[FLAG_SELECTED] = false;
        multiSelectCount = 0;
        
        if (flags[FLAG_MULTI_SELECTABLE] != appletPackage.hasMultiSelectSupport()) {
            // JC 2.2.2, RTE Spec.: All applets within a package SHALL be multiselectable or none shall be.
            SystemException.throwIt(SystemException.ILLEGAL_USE);
        }
    }

    /**
     * Test if applet is installed and registered.
     * @return true if applet is installed and registered, else false
     */
    boolean isInstalled() {
        return flags[FLAG_INSTALLED];
    }
    
    /**
     * Mark applet as selectable.
     */
    void makeSelectable() {
        if (!flags[FLAG_INSTALLED]) {
            SystemException.throwIt(SystemException.ILLEGAL_USE);
        }
        flags[FLAG_SELECTABLE] = true;
    }

    /**
     * Mark applet as selecting.
     */
    void select() {
        if (!flags[FLAG_SELECTABLE]) {
            SystemException.throwIt(SystemException.ILLEGAL_USE);
        }
        flags[FLAG_SELECTING] = true;
    }
    
    /**
     * Cancel applet as selection.
     */
    void cancelSelect() {
        flags[FLAG_SELECTING] = false;
    }
    
    /**
     * Test if applet is selectable.
     * @return true if applet is selectable, else false
     */
    boolean isSelectable() {
        if (appletPackage.isSelected() && !appletPackage.hasMultiSelectSupport()) {
            return false;
        }
        if (flags[FLAG_SELECTED] && !flags[FLAG_MULTI_SELECTABLE]) {
            return false;
        }
        return flags[FLAG_SELECTABLE];
    }
    
    /**
     * Test if applet has multi-selection support.
     * @return true if applet supports multi-selection, else false
     */
    boolean hasMultiSelectSupport() {
        return flags[FLAG_MULTI_SELECTABLE] && appletPackage.hasMultiSelectSupport();
    }
    
    /**
     * Test if applet is currently selecting.
     * @return true if applet is processing its selection command, else false
     */
    boolean isSelecting() {
        return flags[FLAG_SELECTING];
    }
    
    /**
     * Mark applet as selected.
     */
    void selected() {
        appletPackage.select();
        
        if (!flags[FLAG_SELECTABLE]) {
            SystemException.throwIt(SystemException.ILLEGAL_USE);
        }
        if (flags[FLAG_SELECTED] && !flags[FLAG_MULTI_SELECTABLE]) {
            SystemException.throwIt(SystemException.ILLEGAL_USE);
        }
        flags[FLAG_SELECTING] = false;
        flags[FLAG_SELECTED] = true;
        
        if (multiSelectCount < Short.MAX_VALUE) {
            ++multiSelectCount;
        }
    }
    
    /**
     * Test if applet is selected.
     * @return true if applet is selected, else false
     */
    boolean isSelected() {
        return flags[FLAG_SELECTED];
    }
    
    /**
     * Test if applet is multi-selected.
     * @return true if applet is selected multiple times, else false
     */
    boolean isMultiSelected() {
        return flags[FLAG_SELECTED] && flags[FLAG_MULTI_SELECTABLE] && (multiSelectCount > 1);
    }
    
    /**
     * Test if applet package is selected.
     * @return true if applet package is selected, else false
     */
    boolean isPackageSelected() {
        return appletPackage.isSelected();
    }
    
    /**
     * Test if applet package is multi-selected.
     * @return true if applet package is selected multiple times, else false
     */
    boolean isPackageMultiSelected() {
        return appletPackage.isMultiSelected();
    }
    
    /**
     * Deselect applet.
     */
    void deselect() {
        if (multiSelectCount > 0) {
            --multiSelectCount;
        }
        
        if (multiSelectCount == 0) {
            flags[FLAG_SELECTING] = false;
            flags[FLAG_SELECTED] = false;
        }
        
        appletPackage.deselect();
    }
    
    /**
     * Mark applet as not selectable.
     */
    void revokeSelectable() {
        if (!flags[FLAG_SELECTABLE] || !flags[FLAG_INSTALLED]) {
            SystemException.throwIt(SystemException.ILLEGAL_USE);
        }
        flags[FLAG_SELECTABLE] = false;
    }
    
    /**
     * Mark applet as uninstalled.
     */
    void uninstall() {
//        if (!flags[FLAG_INSTALLED]) {
//            SystemException.throwIt(SystemException.ILLEGAL_USE);
//        }
        flags[FLAG_SELECTED] = false;
        flags[FLAG_SELECTING] = false;
        flags[FLAG_SELECTABLE] = false;
        flags[FLAG_INSTALLED] = false;
    }
    
    /**
     * Mark applet as removed.
     */
    void remove() {
//        if (!flags[FLAG_LOADED] || flags[FLAG_INSTALLED]) {
//            SystemException.throwIt(SystemException.ILLEGAL_USE);
//        }
        flags[FLAG_LOADED] = false;
    }
    
    /**
     * Test if applet has support for extended-length APDUs.
     * @return true if applet supports extended-length APDUs, else false
     */
    boolean hasExtendedLengthSupport() {
        return flags[FLAG_EXTENDED_LENGTH];
    }

    AppletInstanceHolder newInstance() {
        if (!flags[FLAG_LOADED]) {
            SystemException.throwIt(SystemException.ILLEGAL_USE);
        }
        return new AppletInstanceHolder(appletClass, aidAppletClass, appletPackage);
    }
    
    void reset() {
        flags[FLAG_SELECTING] = false;
        flags[FLAG_SELECTED] = false;
        multiSelectCount = 0;
    }
}

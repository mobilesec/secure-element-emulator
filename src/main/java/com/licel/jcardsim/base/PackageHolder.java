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

import javacard.framework.AID;
import javacard.framework.MultiSelectable;
import javacard.framework.SystemException;

/**
 * Internal class which is holds a Java Card package and it's state.
 */
class PackageHolder {
    
    /**
     * Package flags.
     */
    private boolean[] flags;
    /**
     * Package flag: Package is currently selected.
     */
    private static final byte FLAG_SELECTED = 0;
    /**
     * Package flag: Package supports multi-selection.
     */
    private static final byte FLAG_MULTI_SELECTABLE = FLAG_SELECTED+1;
    /**
     * Total length of flags.
     */
    private static final byte FLAGS_LENGTH = FLAG_MULTI_SELECTABLE+1;
    
    /**
     * Package AID.
     */
    private AID aidPackage;

    /**
     * Applet classes.
     */
    private Class[] appletClasses;
    
    /**
     * Applet selection counter for multi-selectable packages.
     */
    private short multiSelectCount;
    
    public final PackageDefinition PACKAGE_DEFINITION;
    
    /**
     * Construct a <code>PackageHolder</code> for a given Java Card package.
     * @param packageAID package AID
     */
    PackageHolder(AID packageAID, PackageDefinition packageDef) {
        this.flags = new boolean[FLAGS_LENGTH];
        this.aidPackage = packageAID;
        this.multiSelectCount = 0;
        if (packageDef.APPLETS != null) {
            this.appletClasses = new Class[packageDef.APPLETS.length];
            for (int i = 0; i < packageDef.APPLETS.length; ++i) {
                this.appletClasses[i] = packageDef.APPLETS[i].APPLET_CLASS;
                if (MultiSelectable.class.isAssignableFrom(this.appletClasses[i])) {
                    if (i == 0) {
                        flags[FLAG_MULTI_SELECTABLE] = true;
                    } else if (!flags[FLAG_MULTI_SELECTABLE]) {
                        // JC 2.2.2, RTE Spec.: All applets within a package SHALL be multiselectable or none shall be.
                        SystemException.throwIt(SystemException.ILLEGAL_USE);
                    }
                }
            }
        } else {
            this.appletClasses = new Class[0];
        }
        PACKAGE_DEFINITION = packageDef;
    }

    /**
     * Get package AID.
     * 
     * @return package AID
     */
    AID getPackageAID() {
        return aidPackage;
    }

    /**
     * Test if package has multi-selection support.
     * @return true if package supports multi-selection, else false
     */
    boolean hasMultiSelectSupport() {
        return flags[FLAG_MULTI_SELECTABLE];
    }
    
    /**
     * Mark package as selected.
     */
    void select() {
        if (flags[FLAG_SELECTED] && !flags[FLAG_MULTI_SELECTABLE]) {
            SystemException.throwIt(SystemException.ILLEGAL_USE);
        }
        flags[FLAG_SELECTED] = true;
        
        if (multiSelectCount < Short.MAX_VALUE) {
            ++multiSelectCount;
        }
    }
    
    /**
     * Test if package is selected.
     * @return true if package is selected, else false
     */
    boolean isSelected() {
        return flags[FLAG_SELECTED];
    }
    
    /**
     * Test if package is multi-selected.
     * @return true if package is selected multiple times, else false
     */
    boolean isMultiSelected() {
        return flags[FLAG_SELECTED] && flags[FLAG_MULTI_SELECTABLE] && (multiSelectCount > 1);
    }
    
    /**
     * Deselect package.
     */
    void deselect() {
        if (multiSelectCount > 0) {
            --multiSelectCount;
        }
        
        if (multiSelectCount == 0) {
            flags[FLAG_SELECTED] = false;
        }
    }
    
    void reset() {
        multiSelectCount = 0;
        flags[FLAG_SELECTED] = false;
    }
}

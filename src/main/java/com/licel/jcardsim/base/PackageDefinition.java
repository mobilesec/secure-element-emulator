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

import at.mroland.utils.StringUtils;
import javacard.framework.AID;

/**
 * Java Card package definition.
 */
public class PackageDefinition {
    /**
     * Package AID.
     */
    public final AID PACKAGE_AID;
    
    /**
     * Applets contained in this package.
     */
    public final AppletDefinition[] APPLETS;

    /**
     * Classes contained in this package.
     */
    public final Class[] CLASSES;
    
    /**
     * 
     * @param aidPackage
     * @param applets
     * @param classes 
     */
    public PackageDefinition(byte[] aidPackage, AppletDefinition[] applets, Class[] classes) {
        PACKAGE_AID = new AID(aidPackage, (short)0, (byte)aidPackage.length);
        APPLETS = applets;
        CLASSES = classes;
    }
    
    /**
     * 
     * @param aidPackage
     * @param applets
     * @param classes 
     */
    public PackageDefinition(String aidPackage, AppletDefinition[] applets, Class[] classes) {
        byte[] aidPackageBytes = StringUtils.convertHexStringToByteArray(aidPackage);
        PACKAGE_AID = new AID(aidPackageBytes, (short)0, (byte)aidPackageBytes.length);
        APPLETS = applets;
        CLASSES = classes;
    }
}

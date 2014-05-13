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
 * Java Card applet class definition.
 */
public class AppletDefinition {
    /**
     * Applet class AID.
     */
    public final AID APPLET_AID;
    
    /**
     * Applet class.
     */
    public final Class APPLET_CLASS;
    
    /**
     * 
     * @param aidApplet
     * @param classApplet 
     */
    public AppletDefinition(byte[] aidApplet, Class classApplet) {
        APPLET_AID = new AID(aidApplet, (short)0, (byte)aidApplet.length);
        APPLET_CLASS = classApplet;
    }
    
    /**
     * 
     * @param aidApplet
     * @param classApplet 
     */
    public AppletDefinition(String aidApplet, Class classApplet) {
        byte[] aidAppletBytes = StringUtils.convertHexStringToByteArray(aidApplet);
        APPLET_AID = new AID(aidAppletBytes, (short)0, (byte)aidAppletBytes.length);
        APPLET_CLASS = classApplet;
    }
}

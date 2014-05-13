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

import javacard.framework.APDU;
import javacard.framework.JCSystem;

/**
 * Configuration options for <code>SimulatorSystem</code>
 * @see JCSystem
 */
public interface SimulatorConfig {
    
    /**
     * Java Card API version.
     */
    public static final short API_VERSION = 0x0202;
    /**
     * APDU Buffer size.
     */
    public static final short APDU_BUFFER_SIZE = 262;
    //public static final short APDU_BUFFER_SIZE = 32768;
    /**
     * Information field size IFD to ICC for T=1.
     */
    public static final short IFSD_T1 = 254;
    /**
     * Information field size ICC to IFD for T=1.
     */
    public static final short IFSC_T1 = 254;
    /**
     * Information field size IFD to ICC for T=CL.
     */
    public static final short IFSD_TCL = 256;
    /**
     * Information field size ICC to IFD for T=CL.
     */
    public static final short IFSC_TCL = 256;
    /**
     * Indicate object deletion support by not throwing an exception upon <code>JCSystem.requestObjectDeletion()</code>.
     */
    public static final boolean OBJECT_DELETION_SUPPORT = true;
    /**
     * Maximum number of logical channels.
     */
    public static final byte MAX_LOGICAL_CHANNELS = 20;
    /**
     * Support for extended-length APDUs.
     */
    public static final boolean EXTENDED_LENGTH_SUPPORT = true;
    /**
     * Maximum size of extended length APDUs.
     */
    public static final int EXTENDED_LENGTH_MAXIMUM = 32767;
    /**
     * Interface name of internal interface.
     */
    public static final String INTERFACE_INTERNAL_NAME = "internal";
    /**
     * Interface protocol of internal interface.
     */
    public static final byte INTERFACE_INTERNAL_PROTOCOL = (byte)(APDU.PROTOCOL_T1 | APDU.PROTOCOL_MEDIA_DEFAULT);
    /**
     * Interface name of software card emulation interface.
     */
    public static final String INTERFACE_EXTERNAL_NAME = "external";
    /**
     * Interface protocol of software card emulation interface.
     */
    public static final byte INTERFACE_EXTERNAL_PROTOCOL = (byte)(APDU.PROTOCOL_T1 | APDU.PROTOCOL_MEDIA_CONTACTLESS_TYPE_A);
    /**
     * Interface configuration.
     */
    public static final CardInterface.InterfaceConfig[] INTERFACES = new CardInterface.InterfaceConfig[] {
        new CardInterface.InterfaceConfig(INTERFACE_INTERNAL_NAME, INTERFACE_INTERNAL_PROTOCOL, MAX_LOGICAL_CHANNELS),
        new CardInterface.InterfaceConfig(INTERFACE_EXTERNAL_NAME, INTERFACE_EXTERNAL_PROTOCOL, MAX_LOGICAL_CHANNELS),
    };
}

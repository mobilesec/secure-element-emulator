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

/**
 * <code>ISO7816Extended</code> encapsulates constants related to ISO 7816-3 and
 * ISO 7816-4 beyond <code>javacard.framework.ISO7816</code>.
 */
public interface ISO7816Extended {
    /**
     * APDU command INS: MANAGE_CHANNNEL = 0x70
     */
    public static final byte INS_MANAGE_CHANNEL = (byte)0x70;
    
    /**
     * APDU command P1: SELECT by DF name = 0x04
     */
    public static final byte P1_SELECT_BY_DF_NAME = (byte)0x04;
}

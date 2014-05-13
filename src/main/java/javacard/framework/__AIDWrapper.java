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
package javacard.framework;

import at.mroland.utils.StringUtils;
import com.licel.jcardsim.base.SimulatorSystem;
import java.util.Arrays;

/**
 *
 */
public class __AIDWrapper {
    private __AIDWrapper() {
    }
    
    public static byte[] getAIDBytes(AID aid) {
        if (aid != null) {
            return Arrays.copyOf(aid.aid, aid.aid.length);
        } else {
            return null;
        }
    }
    
    public static String getAIDString(AID aid) {
        if (aid != null) {
            return StringUtils.convertByteArrayToHexString(aid.aid).toUpperCase();
        } else {
            return "";
        }
    }
    
    public static AID getAIDInstance(String aidString) {
        return getAIDInstance(StringUtils.convertHexStringToByteArray(aidString));
    }
    
    public static AID getAIDInstance(byte[] aidBytes) {
        if (aidBytes != null) {
            AID aidInstance = SimulatorSystem.lookupAnyAID(aidBytes, (short)0, (byte)(aidBytes.length & 0x0ff));
            if (aidInstance == null) {
                aidInstance = new AID(aidBytes, (short)0, (byte)(aidBytes.length & 0x0ff));
            }
            return aidInstance;
        } else {
            return null;
        }
    }
    
    public static AID getAIDInstance(byte[] aidBytes, short offset, byte length) {
        if ((aidBytes == null) || (offset < 0) || (length < 0) || (aidBytes.length < (offset + length))) return null;
        
        AID aidInstance = SimulatorSystem.lookupAnyAID(aidBytes, offset, length);
        if (aidInstance == null) {
            aidInstance = new AID(aidBytes, offset, length);
        }
        return aidInstance;
    }
}

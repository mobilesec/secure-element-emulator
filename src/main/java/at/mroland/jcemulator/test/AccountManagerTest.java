/*
 * Copyright 2014 FH OOe Forschungs & Entwicklungs GmbH, Michael Roland.
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
package at.mroland.jcemulator.test;

import at.mroland.logging.Logging;
import at.mroland.utils.ArrayUtils;
import at.mroland.utils.StringUtils;
import com.licel.jcardsim.base.AppletDefinition;
import com.licel.jcardsim.base.PackageDefinition;
import com.licel.jcardsim.base.SimulatorConfig;
import com.licel.jcardsim.base.SimulatorSystem;
import java.io.File;
import java.util.Arrays;
import javacard.framework.AID;
import javacard.framework.__AIDWrapper;


/**
 * @author mroland
 */
public class AccountManagerTest {
    private final static boolean DEBUG = false;
    
    static {
        Logging.DEBUG = DEBUG;
    }

    private final static File CONFIG_PATH = new File(".");
    
    public static void main(String args[]) {
        
        System.out.println("======================================================================");
        System.out.println("=== Account Manager Test Tool                                      ===");
        System.out.println("======================================================================");
        System.out.println("");
        
        if ((args == null) || (args.length < 1)) {
            System.out.println("Usage: AccountManagerTest <command> [args]");
            System.out.println("");
            System.out.println("<command> is one of the following:");
            System.out.println("  install");
            System.out.println("            Install the applet into the emulated JCRE.");
            System.out.println("  verifypin [PIN]");
            System.out.println("            Verify the specified PIN.");
            System.out.println("            \"00000000\" is used if parameter [PIN] not supplied.");
            System.out.println("  changepin <PIN> <newPIN>");
            System.out.println("            Change the PIN to the value of <newPIN>.");
            System.out.println("  setvalue <PIN> <slotId> <value>");
            System.out.println("            Update the key-slot <slotId> to the given string <value>.");
            System.out.println("  getvalue <PIN> <slotId>");
            System.out.println("            Get the value that is stored in the key-slot <slotId>.");
            System.out.println("  sendapdu [APDU] [APDU] ...");
            System.out.println("            Select the AccountManager applet and send zero, one or more command APDUs.");
            System.exit(0);
            return;
        }
        
        
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                cleanup();
            }
        }));
        
        startup();
        
        
        String command = args[0].toLowerCase();
        
        System.out.println("");
        
        if ("install".equals(command)) {
            PackageDefinition pkg = new PackageDefinition(
                    "F075736D696C6541434D475200",
                    new AppletDefinition[] {
                        new AppletDefinition("F075736D696C6541434D475201", at.fhooe.usmile.mc1010237019.AccountManager.class),
                    },
                    new Class[] {
                    });
            System.out.println("Loading package...");
            SimulatorSystem.installForLoad(pkg);
            System.out.println("Installing applet...");
            SimulatorSystem.installForInstall(pkg.APPLETS[0].APPLET_AID, StringUtils.convertHexStringToByteArray("F075736D696C6541434D4752"), null, null);
            System.out.println("Making applet selectable...");
            SimulatorSystem.installForMakeSelectable(__AIDWrapper.getAIDInstance("F075736D696C6541434D4752"), true);
            System.out.println("Done.");
            System.out.println("");
        } else if ("verifypin".equals(command)) {
            byte[] result;
            result = selectAID(StringUtils.convertHexStringToByteArray("F075736D696C6541434D4752"));
            if (!isResultSuccess(result)) {
                Logging.error("AccountManagerTest", "Could not select applet! (response = " + StringUtils.convertByteArrayToHexString(result) + ")");
            }
            
            String pinStr = (args.length > 1) ? args[1] : "00000000";
            pinStr = pinStr.replace("\"", "").replace("\'", "").replace(" ", "");
            byte[] pinBytes = StringUtils.convertHexStringToByteArray(pinStr);
            result = transceiveAPDU(ArrayUtils.concatenate(new byte[]{ (byte)0x00, (byte)0x10, (byte)0x00, (byte)0x00, (byte)(pinBytes.length & 0x0ff) }, pinBytes ));
            if (!isResultSuccess(result)) {
                Logging.error("AccountManagerTest", "PIN verification failed! (response = " + StringUtils.convertByteArrayToHexString(result) + ")");
            } else {
                System.out.println("Verification successful.");
                System.out.println("");
            }
        } else if ("changepin".equals(command)) {
            byte[] result;
            result = selectAID(StringUtils.convertHexStringToByteArray("F075736D696C6541434D4752"));
            if (!isResultSuccess(result)) {
                Logging.error("AccountManagerTest", "Could not select applet! (response = " + StringUtils.convertByteArrayToHexString(result) + ")");
            }

            if (args.length < 3) {
                Logging.error("AccountManagerTest", "Not enough parameters!");
            } else {
                String pinStr = args[1];
                pinStr = pinStr.replace("\"", "").replace("\'", "").replace(" ", "");
                byte[] pinBytes = StringUtils.convertHexStringToByteArray(pinStr);
                result = transceiveAPDU(ArrayUtils.concatenate(new byte[]{ (byte)0x00, (byte)0x10, (byte)0x00, (byte)0x00, (byte)(pinBytes.length & 0x0ff) }, pinBytes ));
                if (!isResultSuccess(result)) {
                    Logging.error("AccountManagerTest", "PIN verification failed! (response = " + StringUtils.convertByteArrayToHexString(result) + ")");
                }
                pinStr = args[2];
                pinStr = pinStr.replace("\"", "").replace("\'", "").replace(" ", "");
                pinBytes = StringUtils.convertHexStringToByteArray(pinStr);
                result = transceiveAPDU(ArrayUtils.concatenate(new byte[]{ (byte)0x00, (byte)0x12, (byte)0x00, (byte)0x00, (byte)(pinBytes.length & 0x0ff) }, pinBytes ));
                if (!isResultSuccess(result)) {
                    Logging.error("AccountManagerTest", "PIN update failed! (response = " + StringUtils.convertByteArrayToHexString(result) + ")");
                } else {
                    System.out.println("PIN successfully changed.");
                    System.out.println("");
                }
            }
        } else if ("setvalue".equals(command)) {
            byte[] result;
            result = selectAID(StringUtils.convertHexStringToByteArray("F075736D696C6541434D4752"));
            if (!isResultSuccess(result)) {
                Logging.error("AccountManagerTest", "Could not select applet! (response = " + StringUtils.convertByteArrayToHexString(result) + ")");
            }

            if (args.length < 4) {
                Logging.error("AccountManagerTest", "Not enough parameters!");
            } else {
                String slotIdStr = args[2];
                slotIdStr = slotIdStr.replace("\"", "").replace("\'", "").replace(" ", "");
                int slotId = -1;
                try {
                    slotId = Integer.parseInt(slotIdStr);
                } catch (Exception e) {
                }
                if ((slotId < 0) || (slotId > 255)) {
                    Logging.error("AccountManagerTest", "Invalid slotId!");
                } else {
                    String pinStr = args[1];
                    pinStr = pinStr.replace("\"", "").replace("\'", "").replace(" ", "");
                    byte[] pinBytes = StringUtils.convertHexStringToByteArray(pinStr);
                    result = transceiveAPDU(ArrayUtils.concatenate(new byte[]{ (byte)0x00, (byte)0x10, (byte)0x00, (byte)0x00, (byte)(pinBytes.length & 0x0ff) }, pinBytes ));
                    if (!isResultSuccess(result)) {
                        Logging.error("AccountManagerTest", "PIN verification failed! (response = " + StringUtils.convertByteArrayToHexString(result) + ")");
                    }
                    String value = args[3];
                    byte[] valueBytes = StringUtils.convertASCIIStringToByteArray(value);
                    valueBytes = Arrays.copyOf(valueBytes, 224);
                    result = transceiveAPDU(ArrayUtils.concatenate(new byte[]{ (byte)0x00, (byte)0x22, (byte)(slotId & 0x0ff), (byte)0x00, (byte)(valueBytes.length & 0x0ff) }, valueBytes ));
                    if (!isResultSuccess(result)) {
                        Logging.error("AccountManagerTest", "Key slot update failed! (response = " + StringUtils.convertByteArrayToHexString(result) + ")");
                    } else {
                        System.out.println("Value successfully set.");
                        System.out.println("");
                    }
                }
            }
        } else if ("getvalue".equals(command)) {
            byte[] result;
            result = selectAID(StringUtils.convertHexStringToByteArray("F075736D696C6541434D4752"));
            if (!isResultSuccess(result)) {
                Logging.error("AccountManagerTest", "Could not select applet! (response = " + StringUtils.convertByteArrayToHexString(result) + ")");
            }

            if (args.length < 3) {
                Logging.error("AccountManagerTest", "Not enough parameters!");
            } else {
                String slotIdStr = args[2];
                slotIdStr = slotIdStr.replace("\"", "").replace("\'", "").replace(" ", "");
                int slotId = -1;
                try {
                    slotId = Integer.parseInt(slotIdStr);
                } catch (Exception e) {
                }
                if ((slotId < 0) || (slotId > 255)) {
                    Logging.error("AccountManagerTest", "Invalid slotId!");
                } else {
                    String pinStr = args[1];
                    pinStr = pinStr.replace("\"", "").replace("\'", "").replace(" ", "");
                    byte[] pinBytes = StringUtils.convertHexStringToByteArray(pinStr);
                    result = transceiveAPDU(ArrayUtils.concatenate(new byte[]{ (byte)0x00, (byte)0x10, (byte)0x00, (byte)0x00, (byte)(pinBytes.length & 0x0ff) }, pinBytes ));
                    if (!isResultSuccess(result)) {
                        Logging.error("AccountManagerTest", "PIN verification failed! (response = " + StringUtils.convertByteArrayToHexString(result) + ")");
                    }
                    result = transceiveAPDU(new byte[]{ (byte)0x00, (byte)0x20, (byte)(slotId & 0x0ff), (byte)0x00, (byte)0x00 });
                    if (!isResultSuccess(result)) {
                        Logging.error("AccountManagerTest", "Key slot read failed! (response = " + StringUtils.convertByteArrayToHexString(result) + ")");
                    } else {
                        String value = StringUtils.convertByteArrayToASCIIString(Arrays.copyOf(result, result.length - 2));
                        value = value.replace("\0", "");
                        System.out.println("Key-slot " + slotId + " contains \"" + value + "\".");
                        System.out.println("");
                    }
                }
            }
        } else if ("sendapdu".equals(command)) {
            byte[] result;
            result = selectAID(StringUtils.convertHexStringToByteArray("F075736D696C6541434D4752"));
            if (!isResultSuccess(result)) {
                Logging.error("AccountManagerTest", "Could not select applet! (response = " + StringUtils.convertByteArrayToHexString(result) + ")");
            }

            for (int i = 1; i < args.length; ++i) {
                String apduStr = args[i];
                apduStr = apduStr.replace("\"", "").replace("\'", "").replace(" ", "").replace(":", "").replace("0x", "");
                byte[] apduBytes = StringUtils.convertHexStringToByteArray(apduStr);
                System.out.println(">>> " + apduStr.toUpperCase());
                result = transceiveAPDU(ArrayUtils.concatenate(apduBytes));
                String value = StringUtils.convertByteArrayToHexString(result);
                System.out.println("<<< " + value.toUpperCase());
                System.out.println("");
            }
        } else {
                System.out.println("Unknown command!");
                System.out.println("");
        }     
        
        System.exit(0);
    }
    
    private static byte[] selectAID(byte[] aid) {
        return SimulatorSystem.transceiveAPDU(SimulatorConfig.INTERFACE_INTERNAL_NAME, ArrayUtils.concatenate(new byte[]{ (byte)0x00, (byte)0xA4, (byte)0x04, (byte)0x00, (byte)(aid.length & 0x0ff) }, aid ));
    }
    
    private static byte[] transceiveAPDU(byte[] apdu) {
        return SimulatorSystem.transceiveAPDU(SimulatorConfig.INTERFACE_INTERNAL_NAME, apdu);
    }
    
    private static boolean isResultSuccess(byte[] result) {
        if ((result == null) || (result.length < 2)) return false;
        return (result[result.length - 2] == (byte)0x90) && (result[result.length - 1] == (byte)0x00);
    }
    
    private static void startup() {
        //MemoryStatusDebugRunner.start();
        Logging.debug("AccountManagerTest", "Start loading from persistent storage.");
        SimulatorSystem.loadFromPersistentStorage(CONFIG_PATH);
        Logging.debug("AccountManagerTest", "End loading from persistent storage.");
    }
    
    private static void cleanup() {
        Logging.debug("AccountManagerTest", "Start saving to persistent storage.");
        SimulatorSystem.saveToPersistentStorage(CONFIG_PATH);
        Logging.debug("AccountManagerTest", "End saving to persistent storage.");
        //MemoryStatusDebugRunner.stop();
    }
}

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

import at.fhooe.usmile.app.securechannel.SecureChannelTesterActivity;
import at.mroland.logging.Logging;
import at.mroland.objectstaterecovery.PersistentMemory;
import at.mroland.objectstaterecovery.TransientMemory;
import com.licel.jcardsim.base.AppletDefinition;
import com.licel.jcardsim.base.PackageDefinition;
import com.licel.jcardsim.base.SimulatorSystem;
import java.io.File;
import javacard.framework.__AIDWrapper;


/**
 * @author mroland
 */
public class EmulatorSCPTestInstall {
    private final static boolean DEBUG = false;
    
    static {
        Logging.DEBUG = DEBUG;
    }

    private final static File CONFIG_PATH = new File(".");
    
    public static void main(String args[]) {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                cleanup();
            }
        }));
        
        startup();
        
        //////////////////////////////////////////////////////////////////////////////////////////////////////
        // install applets
        //////////////////////////////////////////////////////////////////////////////////////////////////////
        // SecureChannel UsmileSecureChannel_SRP_6a_2_4_2048_SM
//        SimulatorSystem.uninstall(__AIDWrapper.getAIDInstance("0102030405080001"));
//        SimulatorSystem.remove(__AIDWrapper.getAIDInstance("0102030405080000"));
        PackageDefinition pkg1 = new PackageDefinition(
                "0102030405080000",
                new AppletDefinition[] {
                    new AppletDefinition("0102030405080001", at.fhooe.usmile.securechannel.sc_srp_6a.TestSecureChannel.class),
                },
                new Class[] {
                    at.fhooe.usmile.securechannel.sc_srp_6a.SecureMessaging.class,
                    at.fhooe.usmile.securechannel.sc_srp_6a.UsmileKeyAgreement.class,
                    at.fhooe.usmile.securechannel.sc_srp_6a.UsmileSecureChannel.class,
                });
        SimulatorSystem.installForLoad(pkg1);
        byte[] appletInstallParam1;
        try {
            appletInstallParam1 = "usmile:usmile".getBytes("US-ASCII");
        } catch (Exception e) {
            appletInstallParam1 = null;
        }
        SimulatorSystem.installForInstall(pkg1.APPLETS[0].APPLET_AID, __AIDWrapper.getAIDBytes(pkg1.APPLETS[0].APPLET_AID), null, appletInstallParam1);
        SimulatorSystem.installForMakeSelectable(pkg1.APPLETS[0].APPLET_AID, true);
        //////////////////////////////////////////////////////////////////////////////////////////////////////
        
        //////////////////////////////////////////////////////////////////////////////////////////////////////
        // run test cases
        //////////////////////////////////////////////////////////////////////////////////////////////////////
        SecureChannelTesterActivity secureChannelTester = new SecureChannelTesterActivity();
        secureChannelTester.run();
        //////////////////////////////////////////////////////////////////////////////////////////////////////
        
        boolean run = true;
//        PersistentMemory pm = SimulatorSystem.getPersistentMemoryInstance();
//        TransientMemory tm = SimulatorSystem.getTransientMemoryInstance();
        while (run) {
            try {
                Thread.sleep(100);
                run = System.in.read() == -1;
            } catch (Exception e) {
            }
        }
        
        System.exit(0);
    }
    
    
    private static void startup() {
        MemoryStatusDebugRunner.start();
        SimulatorSystem.loadFromPersistentStorage(CONFIG_PATH);
    }
    
    private static void cleanup() {
        SimulatorSystem.saveToPersistentStorage(CONFIG_PATH);
        MemoryStatusDebugRunner.stop();
    }
}

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

import at.fhooe.usmile.app.se_performance.TestPerformer2Activity;
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
public class EmulatorPerformanceTest2Install {
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
        // CryptoTest
        PackageDefinition pkg2 = new PackageDefinition(
                "0102030405020000",
                new AppletDefinition[] {
                    new AppletDefinition("0102030405020001", at.fhooe.usmile.simpleecho.SimpleEcho.class),
                },
                new Class[] {
                });
        SimulatorSystem.installForLoad(pkg2);
        byte[] appletData2 = null;
        SimulatorSystem.installForInstall(pkg2.APPLETS[0].APPLET_AID, __AIDWrapper.getAIDBytes(pkg2.APPLETS[0].APPLET_AID), null, appletData2);
        SimulatorSystem.installForMakeSelectable(pkg2.APPLETS[0].APPLET_AID, true);
        //////////////////////////////////////////////////////////////////////////////////////////////////////
        
        TestPerformer2Activity testPerformer = new TestPerformer2Activity();
        testPerformer.run();
        
//        boolean run = true;
////        PersistentMemory pm = SimulatorSystem.getPersistentMemoryInstance();
////        TransientMemory tm = SimulatorSystem.getTransientMemoryInstance();
//        while (run) {
//            try {
//                Thread.sleep(100);
//                run = System.in.read() == -1;
//            } catch (Exception e) {
//            }
//        }
        
        System.exit(0);
    }
    
    
    private static void startup() {
        MemoryStatusDebugRunner.start();
        Logging.info("EmulatorPerformenceTest", "Start loading from persistent storage.");
        SimulatorSystem.loadFromPersistentStorage(CONFIG_PATH);
        Logging.info("EmulatorPerformenceTest", "End loading from persistent storage.");
    }
    
    private static void cleanup() {
        Logging.info("EmulatorPerformenceTest", "Start saving to persistent storage.");
        SimulatorSystem.saveToPersistentStorage(CONFIG_PATH);
        Logging.info("EmulatorPerformenceTest", "End saving to persistent storage.");
        MemoryStatusDebugRunner.stop();
    }
}

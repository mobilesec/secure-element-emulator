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

/**
 *
 * @author mroland
 */
public class MemoryStatusDebugRunner implements Runnable {
    private static final String LOG_TAG = "MemoryStatusDebugRunner";
    
    private static final long REPEAT_INTERVAL = 30000;
    private static final long MAX_MEMORY = Runtime.getRuntime().maxMemory();
    private static final long MEMORY_LIMIT = MAX_MEMORY - MAX_MEMORY / 4;
    
    private static MemoryStatusDebugRunner sInstance = null;
    private static Thread sThread = null;
    
    private boolean mRun;
    
    private MemoryStatusDebugRunner() {
    }
    
    public static void start() {
        if (sInstance == null) {
            sInstance = new MemoryStatusDebugRunner();
            sInstance.mRun = true;
            sThread = new Thread(sInstance);
            sThread.start();
        }
    }
    
    public static void stop() {
        if (sInstance != null) {
            sInstance.mRun = false;
            sInstance = null;
        }
        if (sThread != null) {
            try {
                sThread.join();
            } catch (InterruptedException ex) {
            }
            sThread = null;
        }
    }
    
    public void run() {
        final Runtime rt = Runtime.getRuntime();
        
        Logging.warn(LOG_TAG, "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        Logging.warn(LOG_TAG, "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        Logging.warn(LOG_TAG, "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        Logging.warn(LOG_TAG, "Free memory: " + rt.freeMemory() + " bytes");
        Logging.warn(LOG_TAG, "Total memory: " + rt.totalMemory() + " bytes");
        Logging.warn(LOG_TAG, "Maximum memory: " + MAX_MEMORY + " bytes");
        Logging.warn(LOG_TAG, "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        Logging.warn(LOG_TAG, "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        Logging.warn(LOG_TAG, "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        
        while (mRun) {
            final long freeMemory = rt.freeMemory();
            final long totalMemory = rt.totalMemory();
            final long usedMemory = totalMemory - freeMemory;
            
            if (usedMemory > MEMORY_LIMIT) {
                Logging.warn(LOG_TAG, "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
                Logging.warn(LOG_TAG, "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
                Logging.warn(LOG_TAG, "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
                Logging.warn(LOG_TAG, "Free memory: " + freeMemory + " bytes");
                Logging.warn(LOG_TAG, "Total memory: " + rt.totalMemory() + " bytes");
                Logging.warn(LOG_TAG, "Used memory: " + usedMemory + " of " + MAX_MEMORY + " bytes");
                Logging.warn(LOG_TAG, "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
                Logging.warn(LOG_TAG, "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
                Logging.warn(LOG_TAG, "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            }
            try {
                Thread.sleep(REPEAT_INTERVAL);
            } catch (InterruptedException ex) {
                mRun = false;
            }
        }
    }
}

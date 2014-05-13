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
package at.mroland.logging;


/**
 * Class for logging.
 */
public class Logging {
    public static boolean DEBUG = true;
    
    public static void debug(String tag, String message) {
        debug(tag, message, null);
    }
    
    public static void debug(String tag, String message, Throwable throwable) {
        if (DEBUG) {
            StringBuilder s = new StringBuilder();

            s.append("*** DEBUG *** ");

            s.append(Long.toString(System.currentTimeMillis())).append(" ");

            if (tag != null) s.append("[").append(tag).append("] ");

            if (message != null) s.append(message);

//            if (throwable != null) {
//                throwable.fillInStackTrace();
//            }

            synchronized (Logging.class) {
                System.out.println(s.toString());

                if (throwable != null) {
                    throwable.printStackTrace(System.out);
                }
            }
        }
    }

    public static void info(String tag, String message) {
        info(tag, message, null);
    }
    
    public static void info(String tag, String message, Throwable throwable) {
        StringBuilder s = new StringBuilder();
        
        s.append("*** INFO *** ");
        
        s.append(Long.toString(System.currentTimeMillis())).append(" ");
        
        if (tag != null) s.append("[").append(tag).append("] ");
        
        if (message != null) s.append(message);
        
//        if (throwable != null) {
//            throwable.fillInStackTrace();
//        }
        
        synchronized (Logging.class) {
            System.out.println(s.toString());
            
            if (throwable != null) {
                throwable.printStackTrace(System.out);
            }
        }
    }

    public static void warn(String tag, String message) {
        warn(tag, message, null);
    }
    
    public static void warn(String tag, String message, Throwable throwable) {
        StringBuilder s = new StringBuilder();
        
        s.append("*** WARN *** ");
        
        s.append(Long.toString(System.currentTimeMillis())).append(" ");
        
        if (tag != null) s.append("[").append(tag).append("] ");
        
        if (message != null) s.append(message);
        
//        if (throwable != null) {
//            throwable.fillInStackTrace();
//        }
        
        synchronized (Logging.class) {
            System.out.println(s.toString());
            
            if (throwable != null) {
                throwable.printStackTrace(System.out);
            }
        }
    }

    public static void error(String tag, String message) {
        error(tag, message, null);
    }
    
    public static void error(String tag, String message, Throwable throwable) {
        StringBuilder s = new StringBuilder();
        
        s.append("*** ERROR *** ");
        
        s.append(Long.toString(System.currentTimeMillis())).append(" ");
        
        if (tag != null) s.append("[").append(tag).append("] ");
        
        if (message != null) s.append(message);
        
//        if (throwable != null) {
//            throwable.fillInStackTrace();
//        }
        
        synchronized (Logging.class) {
            System.out.println(s.toString());
            
            if (throwable != null) {
                throwable.printStackTrace(System.out);
            }
        }
    }

    public static void failure(String tag, String message) {
        failure(tag, message, null);
    }
    
    public static void failure(String tag, String message, Throwable throwable) {
        StringBuilder s = new StringBuilder();
        
        s.append("*** FAILURE *** ");
        
        s.append(Long.toString(System.currentTimeMillis())).append(" ");
        
        if (tag != null) s.append("[").append(tag).append("] ");
        
        if (message != null) s.append(message);
        
//        if (throwable != null) {
//            throwable.fillInStackTrace();
//        }
        
        synchronized (Logging.class) {
            System.out.println(s.toString());
            
            if (throwable != null) {
                throwable.printStackTrace(System.out);
            }
        }
    }
}

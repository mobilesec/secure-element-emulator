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
package at.mroland.objectstaterecovery.helper;

import at.mroland.logging.Logging;
import java.util.Stack;

/**
 *
 * @author mroland
 */
public class UniqueObjectIdentifier {
    private static final String LOG_TAG = "UniqueObjectIdentifier";
    
    public static final int NULL_IDENTIFIER = 0;
    private static final int OFFSET_FLOAT = 0;
    private static final int OFFSET_DOUBLE = OFFSET_FLOAT;// + 1;
    private static final int OFFSET_OBJECT = OFFSET_DOUBLE + 1;
    private static final int OFFSET_LONG = OFFSET_OBJECT;// + 1;
    private static final int OFFSET_CHARACTER = OFFSET_LONG;// + 1;
    private static final int OFFSET_STRING = OFFSET_CHARACTER;// + 1;
    private static final int NUMBER_OF_IDENTITY_MAPS = OFFSET_STRING + 1;
    private static final int OFFSET_BOOLEAN = OFFSET_STRING + 1;
    private static final int OFFSET_BYTE = OFFSET_BOOLEAN + 1;
    private static final int OFFSET_SHORT = OFFSET_BYTE + 1;
    private static final int OFFSET_INTEGER_P = OFFSET_SHORT + 1;
    private static final int OFFSET_INTEGER_N = OFFSET_INTEGER_P + 1;
    private static final int LOCAL_IDENTIFIER_BITS = Integer.SIZE;
    private static final long LOCAL_IDENTIFIER_MASK = (((long)1) << LOCAL_IDENTIFIER_BITS) - 1;
    
    private static IdentityObjectIntMap[] sIdentityMap = new IdentityObjectIntMap[NUMBER_OF_IDENTITY_MAPS];
    private static Stack<Integer>[] sUnusedObjectIdentifiers = new Stack[NUMBER_OF_IDENTITY_MAPS];
    private static int[] sLastUniqueObjectIdentifier = new int[NUMBER_OF_IDENTITY_MAPS];

    static {
        reset();
    }
    
    public static long get(Object o) {
        if (o == null) return NULL_IDENTIFIER;
        
        int identityMapIndex = OFFSET_OBJECT;
        
        Class clazz = o.getClass();
        
        if ((clazz == int.class) || (clazz == Integer.class)) {
            if ((Integer)o > 0) {
                identityMapIndex = OFFSET_INTEGER_P;
            } else {
                identityMapIndex = OFFSET_INTEGER_N;
            }
            long uoi = ((long)((Integer)o).intValue()) & LOCAL_IDENTIFIER_MASK;
            uoi |= ((long)identityMapIndex) << LOCAL_IDENTIFIER_BITS;
            return uoi;
        } else if ((clazz == boolean.class) || (clazz == Boolean.class)) {
            identityMapIndex = OFFSET_BOOLEAN;
            long uoi = ((long)(((Boolean)o).booleanValue() ? 1 : 0)) & LOCAL_IDENTIFIER_MASK;
            uoi |= ((long)identityMapIndex) << LOCAL_IDENTIFIER_BITS;
            return uoi;
        } else if ((clazz == byte.class) || (clazz == Byte.class)) {
            identityMapIndex = OFFSET_BYTE;
            long uoi = ((long)((Byte)o).byteValue()) & LOCAL_IDENTIFIER_MASK;
            uoi |= ((long)identityMapIndex) << LOCAL_IDENTIFIER_BITS;
            return uoi;
        } else if ((clazz == short.class) || (clazz == Short.class)) {
            identityMapIndex = OFFSET_SHORT;
            long uoi = ((long)((Short)o).shortValue()) & LOCAL_IDENTIFIER_MASK;
            uoi |= ((long)identityMapIndex) << LOCAL_IDENTIFIER_BITS;
            return uoi;
        } else if ((clazz == long.class) || (clazz == Long.class)) {
            identityMapIndex = OFFSET_LONG;
        } else if ((clazz == float.class) || (clazz == Float.class)) {
            identityMapIndex = OFFSET_FLOAT;
        } else if ((clazz == double.class) || (clazz == Double.class)) {
            identityMapIndex = OFFSET_DOUBLE;
        } else if ((clazz == char.class) || (clazz == Character.class)) {
            identityMapIndex = OFFSET_CHARACTER;
        } else if (clazz == String.class) {
            identityMapIndex = OFFSET_STRING;
        }

        IdentityObjectIntMap thisMap = sIdentityMap[identityMapIndex];
        
        int localUOI;
        try {
            localUOI = thisMap.get(o, NULL_IDENTIFIER);
        } catch (RuntimeException e) {
            Logging.error(LOG_TAG, "Could not check identity hash code for object " + o + "!\n" +
                                   "Status:\n" + 
//                                   "    BOOL " + sIdentityMap[0].size + ", last #0/" + sLastUniqueObjectIdentifier[0] + ", backlog " + sUnusedObjectIdentifiers[0].size() + "\n" +
//                                   "    BYTE " + sIdentityMap[1].size + ", last #1/" + sLastUniqueObjectIdentifier[1] + ", backlog " + sUnusedObjectIdentifiers[1].size() + "\n" +
//                                   "    SHORT " + sIdentityMap[2].size + ", last #2/" + sLastUniqueObjectIdentifier[2] + ", backlog " + sUnusedObjectIdentifiers[2].size() + "\n" +
//                                   "    INT_P " + sIdentityMap[3].size + ", last #3/" + sLastUniqueObjectIdentifier[3] + ", backlog " + sUnusedObjectIdentifiers[3].size() + "\n" +
//                                   "    INT_N " + sIdentityMap[4].size + ", last #4/" + sLastUniqueObjectIdentifier[4] + ", backlog " + sUnusedObjectIdentifiers[4].size() + "\n" +
                                   "    FLOAT/DOUBLE " + sIdentityMap[0].size + ", last #0/" + sLastUniqueObjectIdentifier[0] + ", backlog " + sUnusedObjectIdentifiers[0].size() + "\n" +
                                   "    OBJECT/LONG/CHAR/STRING " + sIdentityMap[1].size + ", last #1/" + sLastUniqueObjectIdentifier[1] + ", backlog " + sUnusedObjectIdentifiers[1].size() + "\n" +
                                   "", e);
            throw e;
        }
        if (localUOI == NULL_IDENTIFIER) {
            Stack<Integer> thisUnusedObjectIdentifiers = sUnusedObjectIdentifiers[identityMapIndex];
            if (thisUnusedObjectIdentifiers.empty()) {
                localUOI = ++sLastUniqueObjectIdentifier[identityMapIndex];
            } else {
                localUOI = thisUnusedObjectIdentifiers.pop();
            }
            if (localUOI != NULL_IDENTIFIER) {
                try {
                    thisMap.put(o, localUOI);
                } catch (Exception e) {
                    Logging.error(LOG_TAG, "Could not add identity hash code #" + identityMapIndex + "/" + localUOI + " for object " + o + "!\n" +
                                           "Status:\n" + 
//                                           "    BOOL " + sIdentityMap[0].size + ", last #0/" + sLastUniqueObjectIdentifier[0] + ", backlog " + sUnusedObjectIdentifiers[0].size() + "\n" +
//                                           "    BYTE " + sIdentityMap[1].size + ", last #1/" + sLastUniqueObjectIdentifier[1] + ", backlog " + sUnusedObjectIdentifiers[1].size() + "\n" +
//                                           "    SHORT " + sIdentityMap[2].size + ", last #2/" + sLastUniqueObjectIdentifier[2] + ", backlog " + sUnusedObjectIdentifiers[2].size() + "\n" +
//                                           "    INT_P " + sIdentityMap[3].size + ", last #3/" + sLastUniqueObjectIdentifier[3] + ", backlog " + sUnusedObjectIdentifiers[3].size() + "\n" +
//                                           "    INT_N " + sIdentityMap[4].size + ", last #4/" + sLastUniqueObjectIdentifier[4] + ", backlog " + sUnusedObjectIdentifiers[4].size() + "\n" +
                                           "    FLOAT/DOUBLE " + sIdentityMap[0].size + ", last #0/" + sLastUniqueObjectIdentifier[0] + ", backlog " + sUnusedObjectIdentifiers[0].size() + "\n" +
                                           "    OBJECT/LONG/CHAR/STRING " + sIdentityMap[1].size + ", last #1/" + sLastUniqueObjectIdentifier[1] + ", backlog " + sUnusedObjectIdentifiers[1].size() + "\n" +
                                           "", e);
                }
            } else {
                Logging.error(LOG_TAG, "Reached maximum number of unique object identifiers for type " + identityMapIndex + "!");
            }
        }
        
        long uoi = ((long)localUOI) & LOCAL_IDENTIFIER_MASK;
        uoi |= ((long)identityMapIndex) << LOCAL_IDENTIFIER_BITS;
        
        return uoi;
    }
    
    public static void forget(Object o) {
        if (o == null) return;
        
        int identityMapIndex = OFFSET_OBJECT;
        
        Class clazz = o.getClass();
        
        if ((clazz == boolean.class) || (clazz == Boolean.class)) {
            identityMapIndex = OFFSET_BOOLEAN;
            return;
        } else if ((clazz == byte.class) || (clazz == Byte.class)) {
            identityMapIndex = OFFSET_BYTE;
            return;
        } else if ((clazz == short.class) || (clazz == Short.class)) {
            identityMapIndex = OFFSET_SHORT;
            return;
        } else if ((clazz == int.class) || (clazz == Integer.class)) {
            if ((Integer)o > 0) {
                identityMapIndex = OFFSET_INTEGER_P;
            } else {
                identityMapIndex = OFFSET_INTEGER_N;
            }
            return;
        } else if ((clazz == long.class) || (clazz == Long.class)) {
            identityMapIndex = OFFSET_LONG;
        } else if ((clazz == float.class) || (clazz == Float.class)) {
            identityMapIndex = OFFSET_FLOAT;
        } else if ((clazz == double.class) || (clazz == Double.class)) {
            identityMapIndex = OFFSET_DOUBLE;
        } else if ((clazz == char.class) || (clazz == Character.class)) {
            identityMapIndex = OFFSET_CHARACTER;
        } else if (clazz == String.class) {
            identityMapIndex = OFFSET_STRING;
        }

        IdentityObjectIntMap thisMap = sIdentityMap[identityMapIndex];
        
        int localUOI = thisMap.remove(o, NULL_IDENTIFIER);
        if (localUOI != NULL_IDENTIFIER) {
            if (localUOI == sLastUniqueObjectIdentifier[identityMapIndex]) {
                --sLastUniqueObjectIdentifier[identityMapIndex];
            } else {
                sUnusedObjectIdentifiers[identityMapIndex].push(localUOI);
            }
        }
    }

    public static void reset() {
        for (int i = 0; i < NUMBER_OF_IDENTITY_MAPS; ++i) {
            if (sIdentityMap[i] == null) {
                if (i >= OFFSET_FLOAT) {
                    sIdentityMap[i] = new IdentityObjectIntMap(4);
                } else {
                    sIdentityMap[i] = new IdentityObjectIntMap();
                }
            } else {
                if (i >= OFFSET_FLOAT) {
                    sIdentityMap[i].clear(4);
                } else {
                    sIdentityMap[i].clear(32);
                }
            }
            if (sUnusedObjectIdentifiers[i] == null) {
                sUnusedObjectIdentifiers[i] = new Stack<Integer>();
            } else {
                sUnusedObjectIdentifiers[i].clear();
            }
            sLastUniqueObjectIdentifier[i] = NULL_IDENTIFIER;
        }
    }
}

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

import java.util.Arrays;
import javacard.framework.Util;

/**
 * Internal class to parse command APDUs.
 */
class ResponseAPDU {
    private final byte[] responseAPDU;
    private final short statusWord;

    ResponseAPDU(byte[] responseData, int responseBufferSize, short statusWord) {
        this.statusWord = statusWord;
        if (responseData == null) {
            responseAPDU = new byte[2];
            Util.setShort(responseAPDU, (short)0, statusWord);
        } else {
            if (responseBufferSize > responseData.length) responseBufferSize = responseData.length;
            responseAPDU = new byte[2 + responseBufferSize];
            Util.arrayCopyNonAtomic(responseData, (short)0, responseAPDU, (short)0, (short)responseBufferSize);
            Util.setShort(responseAPDU, (short)responseBufferSize, statusWord);
        }
    }

    byte[] toBytes() {
        return responseAPDU;
    }

    short getSW() {
        return statusWord;
    }
    
    byte[] getData() {
        return Arrays.copyOf(responseAPDU, responseAPDU.length - 2);
    }
}

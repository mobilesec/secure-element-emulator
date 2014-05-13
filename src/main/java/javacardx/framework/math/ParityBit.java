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
package javacardx.framework.math;

/**
 * The <code>ParityBit</code> class is a utility to assist with DES key parity bit generation.
 * 
 * @since 2.2.2
 */
public final class ParityBit {
  public ParityBit() {
  }

  /**
   * Inserts the computed parity bit of the specified type as the last bit(LSB) in each
   * of the bytes of the specified byte array. The parity is computed over the first(MS)
   * 7 bits of each byte. The incoming last bit of each byte is ignored.
   * 
   * <p>Note:
   * <ul>
   * <li><em>If <code>bOff</code> or <code>bLen</code> is negative an <code>ArrayIndexOutOfBoundsException</code> exception is thrown.</em></li>
   * <li><em>If <code>bLen</code> is equal to 0 no parity bits are inserted.</em></li>
   * <li><em>If <code>bOff+bLen</code> is greater than <code>bArray.length</code>, the length of
   * the <code>bArray</code> array a <code>ArrayIndexOutOfBoundsException</code> exception is
   * thrown and no parity bits are inserted.</em></li>
   * <li><em>If <code>bArray</code> is <code>null</code> a <code>NullPointerException</code> exception is thrown.</em></li>
   * </ul></p>
   * 
   * @param bArray  input/output byte array
   * @param bOff    offset within byte array to start setting parity on
   * @param bLen    byte length of input/output bytes
   * @param isEven  <code>true</code> if even parity is required and false if odd parity is required 
   * @throws NullPointerException            if <code>bArray</code> is null
   * @throws ArrayIndexOutOfBoundsException  if accessing the input array would cause access of data outside array bounds or if <code>bLen</code> is negative
   */
  public static void set(byte[] bArray, short bOff, short bLen, boolean isEven) {
    if ((bOff < 0) || (bLen < 0)) throw new ArrayIndexOutOfBoundsException();
    if ((bOff + bLen) > bArray.length) throw new ArrayIndexOutOfBoundsException();
    
    for (short i = bOff; i < (short)(bOff + bLen); ++i) {
      byte c = (byte)((byte)(bArray[i] >> 1) & 0x7F);
      byte p = 0;
      while (c != 0) {
        if ((byte)(c & 0x1) == 1) {
          p = (byte)(1 - p);
        }
        c = (byte)(c >> 1);
      }
      bArray[i] = (byte)((byte)(bArray[i] & 0xFE) | (isEven ? p : (byte)(1 - p)));
    }
  }
}

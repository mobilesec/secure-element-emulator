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
 * The <code>BCDUtil</code> class contains common BCD (binary coded decimal) related utility functions.
 * This class supports Packed BCD format. All methods in this class are static.
 * 
 * <p>The <code>BCDUtil</code> class only supports unsigned numbers whose value can are represented
 * in hexadecimal format using an implementation specific maximum number of bytes.</p>
 * 
 * @since 2.2.2
 */
public final class BCDUtil {
  private static final short MAX_BYTES_SUPPORTED = 8;
  
  public BCDUtil() {
  }

  /**
   * This method returns the largest value that can be used with the BCD utility functions.
   * This number represents the the byte length of the largest value in hex byte representation.
   * All implementations must support at least 8 byte length usage capacity.
   * @return the byte length of the largest hex value supported
   */
  public static short getMaxBytesSupported() {
    return MAX_BYTES_SUPPORTED;
  }

  /**
   * Converts the input BCD data into hexadecimal format.
   * 
   * <p>Note:
   * <ul>
   * <li><em>If <code>bOff</code> or <code>bLen</code> or <code>outOff</code> parameter is negative
   * an <code>ArrayIndexOutOfBoundsException</code> exception is thrown.</em></li>
   * <li><em>If <code>bOff+bLen</code> is greater than <code>bcdArray.length</code>, the length of
   * the <code>bcdArray</code> array a <code>ArrayIndexOutOfBoundsException</code> exception is
   * thrown and no conversion is performed.</em></li>
   * <li><em>If the output bytes need to be written at an offset greater than <code>hexArray.length</code>,
   * the length of the <code>hexArray</code> array an <code>ArrayIndexOutOfBoundsException</code>
   * exception is thrown and no conversion is performed.</em></li>
   * <li><em>If <code>bcdArray</code> or <code>hexArray</code> parameter is <code>null</code> a
   * <code>NullPointerException</code> exception is thrown.</em></li>
   * <li><em>If the <code>bcdArray</code> and <code>hexArray</code> arguments refer to the same
   * array object, then the conversion is performed as if the components at positions <code>bOff</code>
   * through <code>bOff+bLen-1</code> were first copied to a temporary array with <code>bLen</code>
   * components and then the contents of the temporary array were converted into positions
   * <code>outOff</code> onwards for the converted bytes of the output array.</em></li>
   * </ul></p>
   * 
   * @param bcdArray  input byte array
   * @param bOff      offset within byte array containing first byte (the high order byte)
   * @param bLen      byte length of input BCD data
   * @param hexArray  output byte array
   * @param outOff    offset within <code>hexArray</code> where output data begins
   * @return the byte length of the output hexadecimal data
   * @throws ArrayIndexOutOfBoundsException  if converting would cause access of data outside array bounds or if <code>bLen</code> is negative
   * @throws NullPointerException            if either <code>bcdArray</code> or <code>hexArray</code> is null
   * @throws ArithmeticException             for the following conditions:
   * <ul>
   *   <li>if the input byte array format is not a correctly formed BCD value</li>
   *   <li>if the size of the BCD value requires greater than supported maximum number of bytes to represent in hex format</li>
   *   <li>if <code>bLen</code> is 0</li>
   * </ul>
   */
  public static short convertToHex(byte[] bcdArray, short bOff, short bLen, byte[] hexArray, short outOff) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Converts the input hexadecimal data into BCD format. The output data is right justified.
   * If the number of output BCD nibbles is odd, the first BCD nibble written is 0.
   * 
   * <p>Note:
   * <ul>
   * <li><em>If <code>bOff</code> or <code>bLen</code> or <code>outOff</code> parameter is negative
   * an <code>ArrayIndexOutOfBoundsException</code> exception is thrown.</em></li>
   * <li><em>If <code>bOff+bLen</code> is greater than <code>hexArray.length</code>, the length of
   * the <code>hexArray</code> array a <code>ArrayIndexOutOfBoundsException</code> exception is
   * thrown and no conversion is performed.</em></li>
   * <li><em>If the output bytes need to be written at an offset greater than <code>bcdArray.length</code>,
   * the length of the <code>bcdArray</code> array an <code>ArrayIndexOutOfBoundsException</code>
   * exception is thrown and no conversion is performed.</em></li>
   * <li><em>If <code>bcdArray</code> or <code>hexArray</code> parameter is <code>null</code> a
   * <code>NullPointerException</code> exception is thrown.</em></li>
   * <li><em>If the <code>bcdArray</code> and <code>hexArray</code> arguments refer to the same
   * array object, then the conversion is performed as if the components at positions <code>bOff</code>
   * through <code>bOff+bLen-1</code> were first copied to a temporary array with <code>bLen</code>
   * components and then the contents of the temporary array were converted into positions
   * <code>outOff</code> onwards for the converted bytes of the output array.</em></li>
   * </ul></p>
   * 
   * @param hexArray  input byte array
   * @param bOff      offset within byte array containing first byte (the high order byte)
   * @param bLen      byte length of input hex data
   * @param bcdArray  output byte array
   * @param outOff    offset within <code>bcdArray</code> where output data begins
   * @return the byte length of the output BCD formatted data
   * @throws ArrayIndexOutOfBoundsException  if converting would cause access of data outside array bounds or if <code>bLen</code> is negative
   * @throws NullPointerException            if either <code>bcdArray</code> or <code>hexArray</code> is null
   * @throws ArithmeticException             for the following conditions:
   * <ul>
   *   <li>if the length input hex value is larger than the supported maximum number of bytes</li>
   *   <li>if <code>bLen</code> is 0</li>
   * </ul>
   */
  public static short convertToBCD(byte[] hexArray, short bOff, short bLen, byte[] bcdArray, short outOff) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Checks if the input data is in BCD format. Note that this method does not enforce
   * an upper bound on the length of the input BCD value.
   * 
   * @param bcdArray  input byte array
   * @param bOff      offset within byte array containing first byte (the high order byte)
   * @param bLen      byte length of input BCD data
   * @return true if input data is in BCD format, false otherwise 
   * @throws ArrayIndexOutOfBoundsException  if accessing the input array would cause access of data outside array bounds or if <code>bLen</code> is negative
   * @throws NullPointerException            if <code>bcdArray</code> is null
   * @throws ArithmeticException             if <code>bLen</code> is 0
   */
  public static boolean isBCDFormat(byte[] bcdArray, short bOff, short bLen) {
    throw new UnsupportedOperationException("Not yet implemented");
  }
}

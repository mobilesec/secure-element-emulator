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

import javacard.framework.Util;

/**
 * The <code>BigNumber</code> class encapsulates an unsigned number whose value is represented in
 * internal hexadecimal format using an implementation specific maximum number of bytes.
 * This class supports the BCD (binary coded decimal) format for I/O.
 * 
 * @since 2.2.2
 */
public final class BigNumber {
  /**
   * Constant to indicate a BCD (binary coded decimal) data format. When this format
   * is used a binary coded decimal digit is stored in 1 nibble (4 bits). A byte is
   * packed with 2 BCD digits.
   */
  public static final byte FORMAT_BCD = (byte)1;
  /**
   * Constant to indicate a hexadecimal (simple binary) data format.
   */
  public static final byte FORMAT_HEX = (byte)2;
  
  private static final short MAX_BYTES_SUPPORTED = 8;
  
  private byte[] number;
  private byte[] maxValue;
  
  /**
   * Creates a BigNumber instance with initial value 0. All implementations must support at least 8 byte length internal representation capacity.
   * 
   * @param maxBytes  maximum number of bytes needed in the hexadecimal format for the largest unsigned big number. For example, maxBytes = 2 allows a big number representation range 0-65535.
   * @throws ArithmeticException  if maxBytes is 0, negative or larger than the supported maximum
   */
  public BigNumber(short maxBytes) {
    if ((maxBytes < 0) || (maxBytes > MAX_BYTES_SUPPORTED)) throw new ArithmeticException();
    
    number = new byte[maxBytes];
    maxValue = new byte[maxBytes];

    Util.arrayFillNonAtomic(maxValue, (short)0, maxBytes, (byte)-1);
  }

  public void setMaximum(byte[] maxValue, short bOff, short bLen, byte arrayFormat) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * This method returns the byte length of the hex array that can store the biggest BigNumber supported.
   * This number is the maximum number in hex byte representation. All implementations must support at least 8 bytes.
   * @return the byte length of the largest hex value supported
   */
  public static short getMaxBytesSupported() {
    return MAX_BYTES_SUPPORTED;
  }

  public void init(byte[] bArray, short bOff, short bLen, byte arrayFormat)
    throws NullPointerException, ArrayIndexOutOfBoundsException, ArithmeticException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  public void add(byte[] bArray, short bOff, short bLen, byte arrayFormat)
    throws NullPointerException, ArrayIndexOutOfBoundsException, ArithmeticException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  public void subtract(byte[] bArray, short bOff, short bLen, byte arrayFormat)
    throws ArithmeticException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  public void multiply(byte[] bArray, short bOff, short bLen, byte arrayFormat)
    throws ArithmeticException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  public byte compareTo(BigNumber operand) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  public byte compareTo(byte[] bArray, short bOff, short bLen, byte arrayFormat) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  public void toBytes(byte[] outBuf, short bOff, short numBytes, byte arrayFormat)
    throws ArrayIndexOutOfBoundsException, NullPointerException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  public short getByteLength(byte arrayFormat) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  public void reset() {
    throw new UnsupportedOperationException("Not yet implemented");
  }
}

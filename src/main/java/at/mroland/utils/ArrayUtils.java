/**
 * @version $Id: ArrayUtils.java 123 2011-04-30 08:02:44Z mroland $
 *
 * @author Michael Roland <mi.roland@gmail.com>
 *
 * Copyright (c) 2009-2011 Michael Roland
 *
 * ALL RIGHTS RESERVED.
 */

package at.mroland.utils;

import java.util.ArrayList;

/**
 * Utilities for arrays.
 */
public class ArrayUtils {
  private ArrayUtils() {}

  public static byte[] toByteArray (ArrayList<Byte> ... list) {
    int len = 0;
    for (ArrayList<Byte> l : list) {
      if (l != null) {
        len += l.size();
      }
    }

    byte[] result = new byte[len];

    int index = 0;
    for (ArrayList<Byte> l : list) {
      if (l != null) {
        for (Byte b : l) {
          result[index++] = b.byteValue();
        }
      }
    }
    return result;
  }

  public static byte[] toByteArray (ArrayList<byte[]> list) {
    int len = 0;
    for (byte[] array : list) {
      if (array != null) {
        len += array.length;
      }
    }

    byte[] result = new byte[len];

    int index = 0;
    for (byte[] array : list) {
      if (array != null) {
        java.lang.System.arraycopy(array, 0, result, index, array.length);
        index += array.length;
      }
    }
    return result;
  }

  public static ArrayList concatenate (ArrayList ... list) {
    ArrayList result = new ArrayList();
    for (ArrayList l : list) {
      if (l != null) {
        result.addAll(l);
      }
    }
    return result;
  }

  public static byte[] concatenate (byte[] ... array) {
    int len = 0;
    for (byte[] a : array) {
      if (a != null) {
        len += a.length;
      }
    }

    byte[] result = new byte[len];

    int index = 0;
    for (byte[] a : array) {
      if (a != null) {
        java.lang.System.arraycopy(a, 0, result, index, a.length);
        index += a.length;
      }
    }

    return result;
  }

}

/**
 * @version $Id: StringUtils.java 218 2012-04-25 16:19:29Z mroland $
 *
 * @author Michael Roland <mi.roland@gmail.com>
 *
 * Copyright (c) 2009-2012 Michael Roland
 *
 * ALL RIGHTS RESERVED.
 */

package at.mroland.utils;

import 	java.nio.charset.Charset;

/**
 * Utilities for conversions between byte-arrays and strings.
 */
public class StringUtils {
  private StringUtils() {}

  public static String convertByteArrayToHexString (byte[] b) {
    if (b != null) {
      StringBuilder s = new StringBuilder(2 * b.length);
      
      for (int i = 0; i < b.length; ++i) {
        final String t = Integer.toHexString(b[i]);
        final int l = t.length();
        if (l > 2) {
          s.append(t.substring(l - 2));
        } else {
          if (l == 1) {
            s.append("0");
          }
          s.append(t);
        }
      }
      
      return s.toString();
    } else {
      return "";
    }
  }

  public static String convertByteArrayToReverseHexString (byte[] b) {
    if (b != null) {
      StringBuilder s = new StringBuilder(2 * b.length);
      
      for (int i = (b.length - 1); i >= 0; --i) {
        final String t = Integer.toHexString(b[i]);
        final int l = t.length();
        if (l > 2) {
          s.append(t.substring(l - 2));
        } else {
          if (l == 1) {
            s.append("0");
          }
          s.append(t);
        }
      }
      
      return s.toString();
    } else {
      return "";
    }
  }

  public static String convertByteArrayToASCIIString (byte[] b) {
    String s = "";
    
    try {
      s = new String(b, Charset.forName("US-ASCII"));
    } catch (Exception e) {}
    
    return s;
  }

  public static byte[] convertASCIIStringToByteArray (String s) {
    byte[] b = new byte[0];
    
    try {
      b = s.getBytes(Charset.forName("US-ASCII"));
    } catch (Exception e) {}
    
    return b;
  }

  public static String convertByteArrayToUTF8String (byte[] b) {
    String s = "";
    
    try {
      s = new String(b, Charset.forName("UTF-8"));
    } catch (Exception e) {}
    
    return s;
  }

  public static byte[] convertUTF8StringToByteArray (String s) {
    byte[] b = new byte[0];
    
    try {
      b = s.getBytes(Charset.forName("UTF-8"));
    } catch (Exception e) {}
    
    return b;
  }

  public static String convertByteArrayToUTF16String (byte[] b) {
    String s = "";
    
    try {
      s = new String(b, Charset.forName("UTF-16"));
    } catch (Exception e) {}
    
    return s;
  }

  public static byte[] convertUTF16StringToByteArray (String s) {
    byte[] b = new byte[0];
    
    try {
      b = s.getBytes(Charset.forName("UTF-16"));
    } catch (Exception e) {}
    
    return b;
  }

  public static byte[] convertHexStringToByteArray (String s) {
    s = s.replace(" 0x", "");
    s = s.replace(":0x", "");
    s = s.replace(" ", "");
    s = s.replace(":", "");
    final int len = s.length();
    final int rem = len % 2;
    
    byte[] ret = new byte[len / 2 + rem];
    
    if (rem != 0) {
      try {
        ret[0] = (byte)(Integer.parseInt(s.substring(0, 1), 16) & 0x00F);
      } catch (Exception e) {
        ret[0] = 0;
      }
    }
    
    for (int i = rem; i < len; i += 2) {
      try {
        ret[i / 2 + rem] = (byte)(Integer.parseInt(s.substring(i, i + 2), 16) & 0x0FF);
      } catch (Exception e) {
        ret[i / 2 + rem] = 0;
      }
    }
    
    return ret;
  }
}

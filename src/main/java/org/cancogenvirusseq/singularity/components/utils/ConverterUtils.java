package org.cancogenvirusseq.singularity.components.utils;

import java.text.StringCharacterIterator;
import lombok.experimental.UtilityClass;
import lombok.val;

@UtilityClass
public class ConverterUtils {
  /**
   * Utility function to convert bytes to human readable string. Converts up to Terabyte. Negative
   * numbers are converted to positive.
   *
   * @param bytes
   * @return bytes converted to SI units
   */
  public static String convertBytesToHumanReadable(long bytes) {
    if (bytes < 0) {
      bytes = bytes * -1;
    }
    if (bytes < 1000 && bytes >= 0) {
      return bytes + "B";
    }

    val ci = new StringCharacterIterator("_KMGT");
    while (bytes > 1000 && ci.getIndex() < ci.getEndIndex() - 1) {
      bytes /= 1000;
      ci.next();
    }
    return bytes + " " + ci.current() + "B";
  }
}

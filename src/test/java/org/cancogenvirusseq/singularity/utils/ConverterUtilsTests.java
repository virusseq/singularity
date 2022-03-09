package org.cancogenvirusseq.singularity.utils;

import static org.cancogenvirusseq.singularity.components.utils.ConverterUtils.convertBytesToHumanReadable;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ConverterUtilsTests {
  @Test
  public void testConvertBytesToHumanReadable() {
    assertEquals(convertBytesToHumanReadable(25020L), "25 KB");
    assertEquals(convertBytesToHumanReadable(25029133L), "25 MB");
    assertEquals(convertBytesToHumanReadable(25029133405L), "25 GB");
    assertEquals(convertBytesToHumanReadable(2502913340512L), "2 TB");
  }
}

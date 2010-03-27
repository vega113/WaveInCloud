// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.id;


import junit.framework.TestCase;

/**
 * Tests for wavelet name.
 *
*
 * @author anorth@google.com (Alex North)
 */
public class WaveletNameTest extends TestCase {

  public void testNullPartsAreRejected() {
    try {
      WaveletName.of(null, new WaveletId("example.com", "id"));
      fail("Expected NPE from wavelet name with null wave id");
    } catch (NullPointerException expected) {
    }

    try {
      WaveletName.of(new WaveId("example.com", "id"), null);
      fail("Expected NPE from wavelet name with null wavelet id");
    } catch (NullPointerException expected) {
    }
  }

  public void testOfToString() throws Exception {
    final WaveId waveId = new WaveId("example.com", "w+abcd1234");
    final WaveletId waveletId = new WaveletId("acmewave.com", "conv+blah");
    WaveletName name = WaveletName.of(waveId, waveletId);
    String expected =
            "[WaveId:example.com!w+abcd1234]/[WaveletId:acmewave.com!conv+blah]";
    assertEquals(expected, name.toString());
  }
}

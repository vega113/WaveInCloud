/**
 * Copyright 2009 Google Inc.
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
 *
 */

package org.waveprotocol.wave.federation;


import junit.framework.TestCase;

import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;

/**
 * Tests cases for FederationURICodec.
 *
 * @author thorogood@google.com (Sam Thorogood)
 */

public class FederationURICodecTest extends TestCase {
  private FederationURICodec uriCodec = null;

  private final static WaveletName SINGLE_DOMAIN_WAVELETNAME =
      WaveletName.of(new WaveId("acmewave.com", "wave"), new WaveletId("acmewave.com", "wavelet"));
  private final static String SINGLE_DOMAIN_STRING = "wave://acmewave.com/wave/wavelet";

  private final static WaveletName DISTINCT_DOMAIN_WAVELETNAME =
      WaveletName.of(new WaveId("google.com", "wave"), new WaveletId("acmewave.com", "wavelet"));
  private final static String DISTINCT_DOMAIN_STRING =
      "wave://acmewave.com/google.com!wave/wavelet";

  @Override
  public void setUp() throws Exception {
    super.setUp();
    uriCodec = new FederationURICodec();
  }

  @Override
  public void tearDown() throws Exception {
    super.tearDown();
    uriCodec = null;
  }

  public void testEncode() throws Exception {
    assertEquals(DISTINCT_DOMAIN_STRING, uriCodec.encode(DISTINCT_DOMAIN_WAVELETNAME));
    assertEquals(SINGLE_DOMAIN_STRING, uriCodec.encode(SINGLE_DOMAIN_WAVELETNAME));
  }

  public void testDecode() throws Exception {
    assertEquals(DISTINCT_DOMAIN_WAVELETNAME, uriCodec.decode(DISTINCT_DOMAIN_STRING));
    assertEquals(SINGLE_DOMAIN_WAVELETNAME, uriCodec.decode(SINGLE_DOMAIN_STRING));
  }

}

/**
 * Copyright 2010 Google Inc.
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

package org.waveprotocol.wave.examples.fedone.robots;

import junit.framework.TestCase;

import org.waveprotocol.wave.examples.common.HashedVersion;
import org.waveprotocol.wave.examples.common.HashedVersionFactory;
import org.waveprotocol.wave.examples.common.HashedVersionZeroFactoryImpl;
import org.waveprotocol.wave.examples.fedone.common.VersionedWaveletDelta;
import org.waveprotocol.wave.examples.fedone.util.URLEncoderDecoderBasedPercentEncoderDecoder;
import org.waveprotocol.wave.examples.fedone.util.WaveletDataUtil;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.impl.DocInitializationBuilder;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.core.CoreAddParticipant;
import org.waveprotocol.wave.model.operation.core.CoreWaveletDelta;
import org.waveprotocol.wave.model.operation.core.CoreWaveletOperation;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.opbased.OpBasedWavelet;

import java.util.Collections;
import java.util.List;

/**
 * Unit test for {@link RobotWaveletData}.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class RobotWaveletTest extends TestCase {

  private static final IdURIEncoderDecoder URI_CODEC =
      new IdURIEncoderDecoder(new URLEncoderDecoderBasedPercentEncoderDecoder());
  private static final HashedVersionFactory HASH_FACTORY =
      new HashedVersionZeroFactoryImpl(URI_CODEC);
  private static final String WAVE_ID = "example.com!waveid";
  private static final String WAVELET_ID = "example!conv+root";
  private static final WaveletName WAVELET_NAME = WaveletName.of(WAVE_ID, WAVELET_ID);
  private static final ParticipantId ALEX = ParticipantId.ofUnsafe("alex@example.com");
  private static final ParticipantId BOB = ParticipantId.ofUnsafe("bob@example.com");
  private static final ParticipantId TRIXIE = ParticipantId.ofUnsafe("trixie@example.com");

  private RobotWaveletData wavelet;
  private HashedVersion hashedVersionZero;

  @Override
  protected void setUp() {
    ObservableWaveletData waveletData = WaveletDataUtil.createEmptyWavelet(WAVELET_NAME, ALEX, 0L);
    DocInitialization content = new DocInitializationBuilder().characters("Hello there").build();
    waveletData.createBlip("b+example", ALEX, Collections.singletonList(ALEX), content, 0L, 0);

    hashedVersionZero = HASH_FACTORY.createVersionZero(WAVELET_NAME);
    wavelet = new RobotWaveletData(waveletData, hashedVersionZero);
  }

  public void testGetWaveletName() {
    assertEquals(wavelet.getWaveletName(), WAVELET_NAME);
  }

  public void testGetOpBasedWaveletReturnsSame() {
    OpBasedWavelet waveletAlex = wavelet.getOpBasedWavelet(ALEX);
    assertSame(waveletAlex, wavelet.getOpBasedWavelet(ALEX));
  }

  public void testGetDeltas() {
    // Alex adds a participant to the wavelet
    OpBasedWavelet waveletAlex = wavelet.getOpBasedWavelet(ALEX);
    waveletAlex.addParticipant(TRIXIE);

    // Bob doesn't perform any operations but we do retrieve his wavelet
    wavelet.getOpBasedWavelet(BOB);

    List<VersionedWaveletDelta> deltas = wavelet.getDeltas();
    assertTrue("Only one participant has performed operations", deltas.size() == 1);

    VersionedWaveletDelta versionedDelta = deltas.get(0);

    HashedVersion version = versionedDelta.version;
    assertEquals(
        "Delta should apply to the version given on construction", hashedVersionZero, version);

    CoreWaveletDelta delta = versionedDelta.delta;
    assertEquals(ALEX, delta.getAuthor());

    List<CoreWaveletOperation> operations = delta.getOperations();
    assertTrue(operations.size() == 1);

    CoreAddParticipant addParticipantOp = new CoreAddParticipant(TRIXIE);
    assertEquals(
        "Expected operation that adds Trixie to the wavelet", addParticipantOp, operations.get(0));
  }

  public void testDeltasAreReturnedInOrder() {
    // Alex adds a participant to the wavelet
    OpBasedWavelet waveletAlex = wavelet.getOpBasedWavelet(ALEX);
    waveletAlex.addParticipant(TRIXIE);

    // Bob adds a new document to the wavelet
    OpBasedWavelet waveletBob = wavelet.getOpBasedWavelet(BOB);
    waveletBob.getDocument("r+randomDocument").insertText(0, "/nHello");

    List<VersionedWaveletDelta> deltas = wavelet.getDeltas();
    assertTrue(deltas.size() == 2);

    assertEquals("Expected Alex to be first", ALEX, deltas.get(0).delta.getAuthor());
    assertEquals("Expected Bob to be the second author", BOB, deltas.get(1).delta.getAuthor());
  }
}

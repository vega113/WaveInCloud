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

package org.waveprotocol.box.server.robots.passive;

import com.google.common.collect.Lists;

import junit.framework.TestCase;

import org.waveprotocol.box.server.util.WaveletDataUtil;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.impl.DocInitializationBuilder;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.core.CoreWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.AddParticipant;
import org.waveprotocol.wave.model.operation.wave.ConversionUtil;
import org.waveprotocol.wave.model.operation.wave.RemoveParticipant;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperationContext;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Unit test for {@link WaveletAndDeltas}.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class WaveletAndDeltasTest extends TestCase {

  private static final String WAVE_ID = "example.com!waveid";
  private static final String WAVELET_ID = "example!conv+root";
  private static final WaveletName WAVELET_NAME = WaveletName.of(WAVE_ID, WAVELET_ID);
  private static final ParticipantId ALEX = ParticipantId.ofUnsafe("alex@example.com");
  private static final ParticipantId BOB = ParticipantId.ofUnsafe("bob@example.com");
  private static final ParticipantId CAROL = ParticipantId.ofUnsafe("carol@example.com");

  private WaveletAndDeltas wavelet;
  private HashedVersion hashedVersionOne;
  private ObservableWaveletData waveletData;
  private WaveletOperationContext context;
  private WaveletOperation addCarolOp;
  private WaveletOperation removeAlexOp;
  private HashedVersion hashedVersionTwo;
  private HashedVersion hashedVersionThree;

  @Override
  protected void setUp() throws Exception {
    waveletData = WaveletDataUtil.createEmptyWavelet(WAVELET_NAME, ALEX, 0L);
    DocInitialization content = new DocInitializationBuilder().build();
    waveletData.addParticipant(ALEX);

    context = new WaveletOperationContext(ALEX, 0L, 1);
    AddParticipant addBobOp = new AddParticipant(context, BOB);

    addBobOp.apply(waveletData);

    List<WaveletOperation> ops = Lists.newArrayList();
    ops.add(addBobOp);

    hashedVersionOne = HashedVersion.unsigned(1L);
    hashedVersionTwo = HashedVersion.unsigned(2L);
    hashedVersionThree = HashedVersion.unsigned(3L);

    CoreWaveletDelta delta = ConversionUtil.toCoreWaveletDelta(ops, ALEX, hashedVersionOne);

    wavelet = WaveletAndDeltas.create(waveletData, Collections.singletonList(delta),
        hashedVersionOne);

    addCarolOp = new AddParticipant(context, CAROL);
    removeAlexOp = new RemoveParticipant(context, ALEX);
  }

  public void testgetSnapshotBeforeDeltas() throws Exception {
    ReadableWaveletData firstSnapshot = wavelet.getSnapshotBeforeDeltas();
    assertFalse("The operation to add bob should have been rolled back",
        firstSnapshot.getParticipants().contains(BOB));
    assertTrue("Operations should have been rolled back, together with the version",
        firstSnapshot.getVersion() == 0);
  }

  public void testgetSnapshotAfterDeltas() throws Exception {
    ReadableWaveletData latestSnapshot = wavelet.getSnapshotAfterDeltas();
    assertNotSame("A copy of the waveletdata must be made", waveletData, latestSnapshot);
    assertTrue("Bob should be a participant", latestSnapshot.getParticipants().contains(BOB));
    assertTrue(latestSnapshot.getVersion() == 1);
  }

  public void testGetVersionAfterDeltas() throws Exception {
    assertEquals(hashedVersionOne, wavelet.getVersionAfterDeltas());
  }

  public void testAppendDeltas() throws Exception {
    AddParticipant addCarolOp = new AddParticipant(context, CAROL);

    addCarolOp.apply(waveletData);

    List<WaveletOperation> ops = Lists.newArrayList();
    ops.add(addCarolOp);

    HashedVersion hashedVersionTwo = HashedVersion.unsigned(2);

    CoreWaveletDelta delta = ConversionUtil.toCoreWaveletDelta(ops, ALEX, hashedVersionOne);
    wavelet.appendDeltas(waveletData, Collections.singletonList(delta), hashedVersionTwo);

    ReadableWaveletData firstSnapshot = wavelet.getSnapshotBeforeDeltas();
    assertFalse("Bob should not be a participant", firstSnapshot.getParticipants().contains(BOB));
    assertEquals(hashedVersionTwo, wavelet.getVersionAfterDeltas());

    ReadableWaveletData latestSnapshot = wavelet.getSnapshotAfterDeltas();
    assertNotSame("A copy of the waveletdata must be made", waveletData, latestSnapshot);

    Collection<ParticipantId> participants =
        Collections.unmodifiableCollection(Arrays.asList(BOB, CAROL));
    assertTrue("Bob and Carol should be participating",
        latestSnapshot.getParticipants().containsAll(participants));
  }

  public void testContiguousDeltas() throws Exception {
    addCarolOp.apply(waveletData);
    List<WaveletOperation> ops = Lists.newArrayList(addCarolOp);

    CoreWaveletDelta deltaAdd = ConversionUtil.toCoreWaveletDelta(ops, ALEX, hashedVersionOne);

    removeAlexOp.apply(waveletData);
    ops = Lists.newArrayList(removeAlexOp);
    CoreWaveletDelta deltaRemove = ConversionUtil.toCoreWaveletDelta(ops, ALEX, hashedVersionTwo);

    List<CoreWaveletDelta> deltas = Lists.newArrayList(deltaAdd, deltaRemove);

    wavelet.appendDeltas(waveletData, deltas, hashedVersionThree);
  }

  public void testNonContiguousDeltas() throws Exception {
    addCarolOp.apply(waveletData);
    List<WaveletOperation> ops = Lists.newArrayList(addCarolOp);

    CoreWaveletDelta deltaAdd = ConversionUtil.toCoreWaveletDelta(ops, ALEX, hashedVersionOne);

    removeAlexOp.apply(waveletData);
    ops = Lists.newArrayList(removeAlexOp);
    CoreWaveletDelta deltaRemove = ConversionUtil.toCoreWaveletDelta(ops, ALEX, hashedVersionThree);

    List<CoreWaveletDelta> deltas = Lists.newArrayList(deltaAdd, deltaRemove);

    try {
      wavelet.appendDeltas(waveletData, deltas, hashedVersionThree);
      fail("Expected exception because deltas aren't contiguous");
    } catch (IllegalArgumentException e) {
      // Expected
    }
  }
}

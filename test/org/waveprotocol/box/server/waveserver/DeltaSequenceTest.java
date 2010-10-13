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
package org.waveprotocol.box.server.waveserver;

import com.google.common.collect.ImmutableList;

import junit.framework.TestCase;

import org.waveprotocol.box.server.common.DeltaSequence;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.operation.core.CoreAddParticipant;
import org.waveprotocol.wave.model.operation.core.CoreNoOp;
import org.waveprotocol.wave.model.operation.core.CoreRemoveParticipant;
import org.waveprotocol.wave.model.operation.core.CoreWaveletDelta;
import org.waveprotocol.wave.model.operation.core.CoreWaveletOperation;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.List;

/**
 * Tests {@link DeltaSequence}.
 */
public class DeltaSequenceTest extends TestCase {
  private static final long START_VERSION = 23;
  private static final HashedVersion HASHED_START_VERSION = (HashedVersion.unsigned(START_VERSION));
  private static final ParticipantId USER = new ParticipantId("user@host.com");

  private final List<CoreWaveletOperation> ops = ImmutableList.of(
      CoreNoOp.INSTANCE, CoreNoOp.INSTANCE, new CoreAddParticipant(USER), CoreNoOp.INSTANCE,
      new CoreRemoveParticipant(USER));

  private final CoreWaveletDelta delta1 =
      new CoreWaveletDelta(USER, HashedVersion.unsigned(START_VERSION), ops);
  private final CoreWaveletDelta delta2 =
      new CoreWaveletDelta(USER, HashedVersion.unsigned(START_VERSION + ops.size()), ops);
  private final CoreWaveletDelta delta3 =
      new CoreWaveletDelta(USER, HashedVersion.unsigned(START_VERSION + (2 * ops.size())), ops);

  private final List<CoreWaveletDelta> twoDeltas = ImmutableList.of(delta1, delta2);
  private final HashedVersion twoDeltasEndVersion =
    HashedVersion.unsigned(START_VERSION + 2 * ops.size());

  public void testEmptySequence() {
    DeltaSequence empty = DeltaSequence.empty(HASHED_START_VERSION);
    assertEquals(HASHED_START_VERSION, empty.getStartVersion());
    assertEquals(HASHED_START_VERSION, empty.getEndVersion());
    assertEquals(ImmutableList.<ProtocolWaveletDelta>of(), empty);
  }

  public void testValidSequence() {
    DeltaSequence deltaseq = new DeltaSequence(twoDeltas, twoDeltasEndVersion);
    assertEquals(START_VERSION, deltaseq.getStartVersion().getVersion());
    assertEquals(twoDeltasEndVersion, deltaseq.getEndVersion());
  }

  public void testInvalidEndVersion() {
    assertSequenceInvalid(twoDeltas, HashedVersion.unsigned(twoDeltasEndVersion.getVersion() + 1));
  }

  public void testInvalidIntermediateVersion() {
    // Repeated version, end version correct for last delta.
    assertSequenceInvalid(ImmutableList.of(delta1, delta1),
        HashedVersion.unsigned(START_VERSION + ops.size()));
    // Repeated version, end version correct as sum of ops.
    assertSequenceInvalid(ImmutableList.of(delta1, delta1),
        HashedVersion.unsigned(START_VERSION + (2 * ops.size())));

    // Skipped version, end version correct for last delta.
    assertSequenceInvalid(ImmutableList.of(delta1, delta3),
        HashedVersion.unsigned(START_VERSION + (3 * ops.size())));
    // Skipped version, end version correct as sum of ops.
    assertSequenceInvalid(ImmutableList.of(delta1, delta3),
        HashedVersion.unsigned(START_VERSION + (2 * ops.size())));
  }

  /**
   * Tests DeltaSequence.subList() on both empty and nonempty delta sequences.
   */
  public void testSubList() {
    DeltaSequence empty = DeltaSequence.empty(HASHED_START_VERSION);
    assertEquals(empty, empty.subList(0, 0));

    DeltaSequence deltaseq = new DeltaSequence(twoDeltas, twoDeltasEndVersion);
    assertEquals(twoDeltas, deltaseq.subList(0, twoDeltas.size()));

    assertEquals(empty, deltaseq.subList(0, 0));

    // Check test data set up as expected by the test below
    assertEquals(2, deltaseq.size());
    assertTrue(deltaseq.getEndVersion().getVersion() > ops.size());

    // Now construct a sublist with just the first delta
    DeltaSequence subDeltas = deltaseq.subList(0, 1);
    assertEquals(START_VERSION + ops.size(), subDeltas.getEndVersion().getVersion());
    assertEquals(deltaseq.getStartVersion(), subDeltas.getStartVersion());
  }

  private static void assertSequenceInvalid(List<CoreWaveletDelta> deltas,
      HashedVersion endVersion) {
    try {
      new DeltaSequence(ImmutableList.of(deltas.get(0), deltas.get(0)), endVersion);
      fail("Expected delta sequence construction to fail");
    } catch (IllegalArgumentException expected) {
    }
  }
}

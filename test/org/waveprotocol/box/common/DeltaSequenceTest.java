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
package org.waveprotocol.box.common;

import com.google.common.collect.ImmutableList;

import junit.framework.TestCase;

import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.AddParticipant;
import org.waveprotocol.wave.model.operation.wave.NoOp;
import org.waveprotocol.wave.model.operation.wave.RemoveParticipant;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperationContext;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.List;

/**
 * Tests {@link DeltaSequence}.
 */
public class DeltaSequenceTest extends TestCase {
  private static final long START_VERSION = 23;
  private static final ParticipantId USER = new ParticipantId("user@host.com");
  private static final WaveletOperationContext CONTEXT = new WaveletOperationContext(USER, 0, 1);

  private final List<WaveletOperation> ops = ImmutableList.of(
      new NoOp(CONTEXT), new NoOp(CONTEXT), new AddParticipant(CONTEXT, USER), new NoOp(CONTEXT),
      new RemoveParticipant(CONTEXT, USER));

  private final TransformedWaveletDelta delta1 = new TransformedWaveletDelta(USER,
      HashedVersion.unsigned(START_VERSION + ops.size()), 0L, ops);
  private final TransformedWaveletDelta delta2 = new TransformedWaveletDelta(USER,
      HashedVersion.unsigned(START_VERSION + (2 * ops.size())), 0L, ops);
  private final TransformedWaveletDelta delta3 = new TransformedWaveletDelta(USER,
      HashedVersion.unsigned(START_VERSION + (3 * ops.size())), 0L, ops);

  private final List<TransformedWaveletDelta> twoDeltas = ImmutableList.of(delta1, delta2);

  public void testEmptySequence() {
    DeltaSequence empty = DeltaSequence.empty();
    assertEquals(-1, empty.getStartVersion());
    try {
      empty.getEndVersion();
      fail("Expected illegal state exception");
    } catch (IllegalStateException expected) {
    }
    assertEquals(ImmutableList.<ProtocolWaveletDelta>of(), empty);
  }

  public void testValidSequence() {
    DeltaSequence deltaseq = new DeltaSequence(twoDeltas);
    assertEquals(START_VERSION, deltaseq.getStartVersion());
    assertEquals(delta2.getResultingVersion(), deltaseq.getEndVersion());
  }

  public void testInvalidIntermediateVersion() {
    // Repeated version.
    assertSequenceInvalid(delta1, delta1);
    // Skipped version.
    assertSequenceInvalid(delta1, delta3);
  }

  /**
   * Tests DeltaSequence.subList() on both empty and nonempty delta sequences.
   */
  public void testSubList() {
    DeltaSequence empty = DeltaSequence.empty();
    assertEquals(empty, empty.subList(0, 0));

    DeltaSequence deltaseq = new DeltaSequence(twoDeltas);
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

  private static void assertSequenceInvalid(TransformedWaveletDelta... deltas) {
    try {
      new DeltaSequence(ImmutableList.copyOf(deltas));
      fail("Expected delta sequence construction to fail");
    } catch (IllegalArgumentException expected) {
    }
  }
}

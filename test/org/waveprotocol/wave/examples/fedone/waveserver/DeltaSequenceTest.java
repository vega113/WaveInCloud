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
package org.waveprotocol.wave.examples.fedone.waveserver;

import com.google.common.collect.ImmutableList;

import junit.framework.TestCase;

import org.waveprotocol.wave.examples.common.HashedVersion;
import org.waveprotocol.wave.examples.fedone.common.DeltaSequence;
import org.waveprotocol.wave.examples.fedone.common.VersionedWaveletDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.operation.core.CoreAddParticipant;
import org.waveprotocol.wave.model.operation.core.CoreNoOp;
import org.waveprotocol.wave.model.operation.core.CoreRemoveParticipant;
import org.waveprotocol.wave.model.operation.core.CoreWaveletDelta;
import org.waveprotocol.wave.model.operation.core.CoreWaveletOperation;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.List;

/**
 * Tests {@link DeltaSequence}.
 */
public class DeltaSequenceTest extends TestCase {
  private static final long START_VERSION = 23;
  private static final HashedVersion PROTO_START_VERSION = (HashedVersion.unsigned(START_VERSION));
  private static final ParticipantId USER = new ParticipantId("user@host.com");

  private List<CoreWaveletOperation> ops;
  private List<VersionedWaveletDelta> deltas;
  private HashedVersion endVersion;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    ops = ImmutableList.of(
        CoreNoOp.INSTANCE, CoreNoOp.INSTANCE, new CoreAddParticipant(USER), CoreNoOp.INSTANCE,
        new CoreRemoveParticipant(USER));

    CoreWaveletDelta delta = new CoreWaveletDelta(USER, ops);
    CoreWaveletDelta delta2 = new CoreWaveletDelta(USER, ops);
    deltas = ImmutableList.of(
        new VersionedWaveletDelta(delta, HashedVersion.unsigned(START_VERSION)),
        new VersionedWaveletDelta(delta, HashedVersion.unsigned(START_VERSION + ops.size())));
    endVersion = HashedVersion.unsigned(START_VERSION + 2 * ops.size());
  }

  public void testEmptySequence() {
    DeltaSequence empty = DeltaSequence.empty(PROTO_START_VERSION);
    assertEquals(PROTO_START_VERSION, empty.getStartVersion());
    assertEquals(PROTO_START_VERSION, empty.getEndVersion());
    assertEquals(ImmutableList.<ProtocolWaveletDelta>of(), empty);
  }

  public void testNegativeEndVersion() {
    HashedVersion invalidVersion = new HashedVersion(-1, new byte[0]);
    try {
      DeltaSequence.empty(invalidVersion);
      fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
      // pass
    }
  }

  public void testValidSequence() {
    DeltaSequence deltaseq = new DeltaSequence(deltas, endVersion);
    assertEquals(START_VERSION, deltaseq.getStartVersion().getVersion());
    assertEquals(endVersion, deltaseq.getEndVersion());
  }

  public void testInvalidEndVersion() {
    try {
      new DeltaSequence(deltas, HashedVersion.unsigned(endVersion.getVersion() + 1));
    } catch (IllegalArgumentException expected) {
      // pass
    }
  }

  public void testInvalidIntermediateVersion() {
    try {
      new DeltaSequence(
          ImmutableList.of(deltas.get(0), deltas.get(0)),
          endVersion
          );
    } catch (IllegalArgumentException expected) {
      // pass
    }

    try {
      new DeltaSequence(ImmutableList.of(deltas.get(0), deltas.get(0)),
          HashedVersion.unsigned(START_VERSION + 1 * ops.size()));
    } catch (IllegalArgumentException expected) {
      // pass
    }
  }

  /**
   * Tests DeltaSequence.subList() on both empty and nonempty delta sequences.
   */
  public void testSubList() {
    DeltaSequence empty = DeltaSequence.empty(PROTO_START_VERSION);
    assertEquals(empty, empty.subList(0, 0));

    DeltaSequence deltaseq = new DeltaSequence(deltas, endVersion);
    assertEquals(deltas, deltaseq.subList(0, deltas.size()));

    assertEquals(empty, deltaseq.subList(0, 0));

    // Check test data set up as expected by the test below
    assertEquals(2, deltaseq.size());
    assertTrue(deltaseq.getEndVersion().getVersion() > ops.size());

    // Now construct a sublist with just the first delta
    DeltaSequence subDeltas = deltaseq.subList(0, 1);
    assertEquals(START_VERSION + ops.size(), subDeltas.getEndVersion().getVersion());
    assertEquals(deltaseq.getStartVersion(), subDeltas.getStartVersion());
  }
}

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

import static org.waveprotocol.wave.examples.fedone.common.CoreWaveletOperationSerializer.serialize;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;

import junit.framework.TestCase;

import org.waveprotocol.wave.examples.fedone.common.DeltaSequence;
import org.waveprotocol.wave.examples.fedone.common.HashedVersion;
import org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion;
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
  private static final ProtocolHashedVersion PROTO_START_VERSION =
    serialize(HashedVersion.unsigned(START_VERSION));
  private static final ParticipantId USER = new ParticipantId("user@host.com");

  private List<CoreWaveletOperation> ops;
  private List<ProtocolWaveletDelta> protoDeltas;
  private ProtocolHashedVersion protoEndVersion;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    ops = ImmutableList.of(
        CoreNoOp.INSTANCE, CoreNoOp.INSTANCE, new CoreAddParticipant(USER), CoreNoOp.INSTANCE,
        new CoreRemoveParticipant(USER));

    CoreWaveletDelta delta = new CoreWaveletDelta(USER, ops);
    CoreWaveletDelta delta2 = new CoreWaveletDelta(USER, ops);
    protoDeltas = ImmutableList.of(
        serialize(delta, HashedVersion.unsigned(START_VERSION),
                  HashedVersion.unsigned(START_VERSION + ops.size())),
        serialize(delta, HashedVersion.unsigned(START_VERSION + ops.size()),
                  HashedVersion.unsigned(START_VERSION + ops.size() + ops.size())));
    protoEndVersion = serialize(HashedVersion.unsigned(START_VERSION + 2 * ops.size()));
  }

  public void testEmptySequence() {
    DeltaSequence empty = DeltaSequence.empty(PROTO_START_VERSION);
    assertEquals(PROTO_START_VERSION, empty.getStartVersion());
    assertEquals(PROTO_START_VERSION, empty.getEndVersion());
    assertEquals(ImmutableList.<ProtocolWaveletDelta>of(), empty);
  }

  public void testNegativeEndVersion() {
    ProtocolHashedVersion invalidVersion = ProtocolHashedVersion.newBuilder()
    .setHistoryHash(ByteString.copyFrom(new byte[0])).setVersion(-1).build();

    try {
      DeltaSequence.empty(invalidVersion);
      fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
      // pass
    }
  }

  public void testValidSequence() {
    DeltaSequence deltas = new DeltaSequence(protoDeltas, protoEndVersion);
    assertEquals(START_VERSION, deltas.getStartVersion().getVersion());
    assertEquals(protoEndVersion, deltas.getEndVersion());
  }

  public void testInvalidEndVersion() {
    try {
      new DeltaSequence(protoDeltas,
          serialize(HashedVersion.unsigned(protoEndVersion.getVersion() + 1)));
    } catch (IllegalArgumentException expected) {
      // pass
    }
  }

  public void testInvalidIntermediateVersion() {
    try {
      new DeltaSequence(
          ImmutableList.of(protoDeltas.get(0), protoDeltas.get(0)),
          protoEndVersion
          );
    } catch (IllegalArgumentException expected) {
      // pass
    }

    try {
      new DeltaSequence(
          ImmutableList.of(protoDeltas.get(0), protoDeltas.get(0)),
          serialize(HashedVersion.unsigned(START_VERSION + 1 * ops.size()))
          );
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

    DeltaSequence deltas = new DeltaSequence(protoDeltas, protoEndVersion);
    assertEquals(deltas, deltas.subList(0, deltas.size()));

    assertEquals(empty, deltas.subList(0, 0));

    // Check test data set up as expected by the test below
    assertEquals(2, deltas.size());
    assertTrue(deltas.getEndVersion().getVersion() > ops.size());

    // Now construct a sublist with just the first delta
    DeltaSequence subDeltas = deltas.subList(0, 1);
    assertEquals(START_VERSION + ops.size(), subDeltas.getEndVersion().getVersion());
    assertEquals(deltas.getStartVersion(), subDeltas.getStartVersion());
  }
}

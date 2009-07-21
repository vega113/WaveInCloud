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

import static org.waveprotocol.wave.examples.fedone.common.WaveletOperationSerializer.serialize;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import junit.framework.TestCase;

import org.waveprotocol.wave.examples.fedone.common.HashedVersion;
import org.waveprotocol.wave.examples.fedone.common.WaveletOperationSerializer;
import org.waveprotocol.wave.examples.fedone.waveserver.ClientFrontend.OpenListener;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.wave.NoOp;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.protocol.common.ProtocolHashedVersion;
import org.waveprotocol.wave.protocol.common.ProtocolWaveletDelta;
import org.waveprotocol.wave.protocol.common.ProtocolWaveletOperation;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests {@link UserManager}.
 *
 *
 */
public class UserManagerTest extends TestCase {
  private static final WaveId W1 = new WaveId("waveId", "1");
  private static final WaveId W2 = new WaveId("waveId", "2");

  private static final WaveletId WA = new WaveletId("waveletId", "A");
  private static final WaveletId WB = new WaveletId("waveletId", "B");

  private static final WaveletName W1A = WaveletName.of(W1, WA);
  private static final WaveletName W2A = WaveletName.of(W2, WA);
  private static final WaveletName W2B = WaveletName.of(W2, WB);

  private static final ParticipantId USER = new ParticipantId("user@host.com");

  private static final ProtocolWaveletDelta DELTA =
    serialize(new WaveletDelta(USER, ImmutableList.of(new NoOp(), new NoOp())),
        HashedVersion.UNSIGNED_VERSION_0);

  private static final ProtocolHashedVersion END_VERSION = serialize(HashedVersion.unsigned(2));

  private static final DeltaSequence DELTAS =
    new DeltaSequence(ImmutableList.of(DELTA), END_VERSION);

  private UserManager m;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    m = new UserManager();
  }

  /** Tests that adding an existing participant throws an exception. */
  public void testAddExistingParticipant() {
    m.addWavelet(W1A);
    try {
      m.addWavelet(W1A);
      fail("Should have thrown IllegalStateException");
    } catch (IllegalStateException expected) {
      // pass
    }
  }

  /** Tests that removing a non-participant throws an exception. */
  public void testRemoveNonParticipant() {
    try {
      m.removeWavelet(W1A);
      fail("Should have thrown IllegalStateException");
    } catch (IllegalStateException expected) {
      // pass
    }
  }

  /** Tests that isParticipant() agrees with added and removed wavelets. */
  public void testIsParticipant() {
    assertFalse(m.isParticipant(W1A));
    m.addWavelet(W2A);
    assertFalse(m.isParticipant(W1A));
    m.addWavelet(W1A);
    assertTrue(m.isParticipant(W1A));
    m.removeWavelet(W1A);
    assertFalse(m.isParticipant(W1A));
  }

  /**
   * Tests that {@link UserManager#getWaveletIds(WaveId)} accurately reflects
   * the added and removed wavelets.
   */
  public void testGetWaveletIds() {
    assertEquals(ImmutableSet.<WaveletId>of(), m.getWaveletIds(W1));
    m.addWavelet(W1A);
    assertEquals(ImmutableSet.<WaveletId>of(WA), m.getWaveletIds(W1));
    m.addWavelet(W2A);
    assertEquals(ImmutableSet.<WaveletId>of(WA), m.getWaveletIds(W1));
    assertEquals(ImmutableSet.<WaveletId>of(WA), m.getWaveletIds(W2));
    m.addWavelet(W2B);
    assertEquals(ImmutableSet.<WaveletId>of(WA, WB), m.getWaveletIds(W2));
    m.removeWavelet(W1A);
    assertEquals(ImmutableSet.<WaveletId>of(), m.getWaveletIds(W1));
  }

  /**
   * Mock implementation that only implements toString() (for easy
   * interpretation of test failures).
   */
  private static class MockListener implements OpenListener {
    private final String serializedForm;

    public MockListener(String serializedForm) {
      this.serializedForm = serializedForm;
    }

    @Override
    public void onCommit(WaveletName waveletName, ProtocolHashedVersion commitNotice) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void onFailure(String errorMessage) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void onUpdate(WaveletName waveletName, List<ProtocolWaveletDelta> deltas,
        ProtocolHashedVersion resultingVersion) {
      throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
      return serializedForm;
    }
  }

  /**
   * Tests that {@link UserManager#matchSubscriptions(WaveletName)} accurately
   * reflects subscription, independent of whether we actually are on any
   * wavelets.
   */
  public void testMatchSubscriptions() {
    assertEquals(ImmutableList.<OpenListener>of(), m.matchSubscriptions(W1A));

    OpenListener l1 = new MockListener("listener 1");
    OpenListener l2 = new MockListener("listener 2");
    OpenListener l3 = new MockListener("listener 3");
    OpenListener l4 = new MockListener("listener 4");
    OpenListener l5 = new MockListener("listener 5");

    m.subscribe(W2, ImmutableSet.of(WA.serialise()), l1);
    m.subscribe(W2, ImmutableSet.of(""), l2);
    m.subscribe(W1, ImmutableSet.of("", WA.serialise()), l3);
    m.subscribe(W2, ImmutableSet.of("nonexisting-prefix"), l4);
    m.subscribe(W2, ImmutableSet.of("wav", "waveletId"), l5);

    assertEquals(ImmutableList.of(l1, l2, l5), m.matchSubscriptions(W2A));
    assertEquals(ImmutableList.of(l2, l5), m.matchSubscriptions(W2B));

    m.addWavelet(W2B); // Doesn't make any difference
    assertEquals(ImmutableList.of(l1, l2, l5), m.matchSubscriptions(W2A));
    assertEquals(ImmutableList.of(l2, l5), m.matchSubscriptions(W2B));
  }

  /** Tests onUpdate() for a wavelet we're not a participant of. */
  public void testOnUpdateForUnknownWavelet() {
    try {
      m.onUpdate(W1A, DELTAS);
      fail("Should have thrown IllegalStateException");
    } catch (IllegalStateException expected) {
      // pass
    }
  }

  /** Tests onCommit() for a wavelet we're not a participant of. */
  public void testOnCommitForUnknownWavelet() {
    try {
      m.onCommit(W1A, serialize(HashedVersion.UNSIGNED_VERSION_0));
      fail("Should have thrown IllegalStateException");
    } catch (IllegalStateException expected) {
      // pass
    }
  }

  /**
   * Tests that sending a single delta with the correct start version
   * number 0 to a wavelet we're subscribed to succeeds.
   */
  public void testUpdateSingleDeltaVersion() {
    m.addWavelet(W1A);
    m.onUpdate(W1A, DELTAS); // pass
  }

  /**
   * Test that a second delta marked as version 2 = DELTA.getOperationCount()
   * succeeds.
   */
  public void testUpdateSeveralDeltas() {
    // Check that test was set up correctly
    assertEquals(2, DELTA.getOperationCount());

    ProtocolWaveletOperation noOp = serialize(new NoOp());

    ProtocolHashedVersion v2 =
        WaveletOperationSerializer.serialize(HashedVersion.unsigned(2));

    ProtocolWaveletDelta delta2 = ProtocolWaveletDelta.newBuilder().setAuthor(
        USER.getAddress()).addOperation(noOp).setHashedVersion(v2).build();

    m.addWavelet(W1A);
    ProtocolHashedVersion endVersion2 = serialize(HashedVersion.unsigned(3));
    m.onUpdate(W1A, new DeltaSequence(ImmutableList.of(DELTA, delta2), endVersion2)); // success

    // Also succeeds when sending the two deltas via separate onUpdates()
    m.addWavelet(W2A);
    m.onUpdate(W2A, DELTAS); // success
    m.onUpdate(W2A, new DeltaSequence(ImmutableList.of(delta2), endVersion2)); // success
  }

  /**
   * Tests that subscribed listeners are only invoked from onUpdate when
   * at least one delta is passed in.
   */
  public void testListenersInvokedOnlyForNonemptyDeltas() {
    final AtomicInteger updates = new AtomicInteger(0);
    OpenListener listener = new MockListener("1") {
      @Override
      public void onUpdate(WaveletName waveletName, List<ProtocolWaveletDelta> deltas,
          ProtocolHashedVersion endVersion) {
        updates.incrementAndGet();
        assertEquals(DELTAS, new DeltaSequence(deltas, endVersion));
      }
    };
    m.addWavelet(W1A);
    m.subscribe(W1, ImmutableSet.of(""), listener);
    m.onUpdate(W1A, DeltaSequence.empty(serialize(HashedVersion.UNSIGNED_VERSION_0)));
    assertEquals(0, updates.get());
    m.onUpdate(W1A, DELTAS);
    assertEquals(1, updates.get());
  }
}

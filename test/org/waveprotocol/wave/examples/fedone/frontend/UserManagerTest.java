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

package org.waveprotocol.wave.examples.fedone.frontend;

import static org.waveprotocol.wave.examples.fedone.common.CoreWaveletOperationSerializer.serialize;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.internal.Nullable;

import junit.framework.TestCase;

import org.waveprotocol.wave.examples.fedone.common.DeltaSequence;
import org.waveprotocol.wave.examples.fedone.common.HashedVersion;
import org.waveprotocol.wave.examples.fedone.common.HashedVersionFactoryImpl;
import org.waveprotocol.wave.examples.fedone.frontend.ClientFrontend.OpenListener;
import org.waveprotocol.wave.examples.fedone.frontend.UserManager.Subscription;
import org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletOperation;
import org.waveprotocol.wave.model.id.IdFilter;
import org.waveprotocol.wave.model.id.IdFilters;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.core.CoreNoOp;
import org.waveprotocol.wave.model.operation.core.CoreWaveletDelta;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests {@link UserManager}.
 */
public class UserManagerTest extends TestCase {
  private static final WaveId W1 = new WaveId("example.com", "111");
  private static final WaveId W2 = new WaveId("example.com", "222");

  private static final WaveletId WA = new WaveletId("example.com", "AAA");
  private static final WaveletId WB = new WaveletId("example.com", "BBB");

  private static final WaveletName W1A = WaveletName.of(W1, WA);
  private static final WaveletName W2A = WaveletName.of(W2, WA);
  private static final WaveletName W2B = WaveletName.of(W2, WB);

  private static final ParticipantId USER = ParticipantId.ofUnsafe("user@host.com");

  private static final ProtocolWaveletDelta DELTA =
    serialize(new CoreWaveletDelta(USER, ImmutableList.of(CoreNoOp.INSTANCE, CoreNoOp.INSTANCE)),
        HashedVersion.UNSIGNED_VERSION_0);

  private static final ProtocolHashedVersion END_VERSION = serialize(HashedVersion.unsigned(2));

  private static final DeltaSequence DELTAS =
    new DeltaSequence(ImmutableList.of(DELTA), END_VERSION);

  private UserManager m;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    m = new UserManager(new HashedVersionFactoryImpl());
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
    public void onFailure(String errorMessage) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void onUpdate(WaveletName waveletName,
        @Nullable WaveletSnapshotAndVersions snapshot,
        List<ProtocolWaveletDelta> deltas, @Nullable ProtocolHashedVersion endVersion,
        @Nullable ProtocolHashedVersion committedVersion, final boolean hasMarker,
        @Nullable String channelId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
      return serializedForm;
    }
  }

  // TODO(arb): add tests testing subscriptions with channelIds.

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
    String channelId = "";

    m.subscribe(W2, IdFilter.ofIds(WA), channelId, l1);
    m.subscribe(W2, IdFilters.ALL_IDS, channelId, l2);
    m.subscribe(W1, IdFilter.ofPrefixes("", WA.getId()), channelId, l3);
    m.subscribe(W2, IdFilters.NO_IDS, channelId, l4);
    m.subscribe(W2, IdFilter.ofPrefixes("A", "B"), channelId, l5);

    checkListenersMatchSubscriptions(ImmutableList.of(l1, l2, l5), m.matchSubscriptions(W2A));
    checkListenersMatchSubscriptions(ImmutableList.of(l2, l5), m.matchSubscriptions(W2B));

    m.addWavelet(W2B); // Doesn't make any difference
    checkListenersMatchSubscriptions(ImmutableList.of(l1, l2, l5), m.matchSubscriptions(W2A));
    checkListenersMatchSubscriptions(ImmutableList.of(l2, l5), m.matchSubscriptions(W2B));
  }

  /**
   * Method to check whether the given subscriptions contain exactly the expected
   * {@link OpenListener}s.
   *
   * @param expectedListeners the {@link List} of {@link OpenListener}s we are
   *        expecting
   * @param matchedSubscriptions the {@link List} of subscriptions to get the
   *        {@link OpenListener} from
   */
  private void checkListenersMatchSubscriptions(List<OpenListener> expectedListeners,
      List<Subscription> matchedSubscriptions) {
    List<OpenListener> actualListeners = new ArrayList<OpenListener>();
    for (Subscription subscription : matchedSubscriptions) {
      actualListeners.add(subscription.getOpenListener());
    }
    assertEquals(expectedListeners, actualListeners);
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
      m.onCommit(W1A, serialize(HashedVersion.UNSIGNED_VERSION_0), null /* no channelId */);
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

    ProtocolWaveletOperation noOp = serialize(CoreNoOp.INSTANCE);

    ProtocolHashedVersion v2 = serialize(HashedVersion.unsigned(2));

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
      public void onUpdate(WaveletName waveletName,
          @Nullable WaveletSnapshotAndVersions snapshot,
          List<ProtocolWaveletDelta> deltas, @Nullable ProtocolHashedVersion endVersion,
          @Nullable ProtocolHashedVersion committedVersion, final boolean hasMarker,
          @Nullable String channelId) {
        updates.incrementAndGet();
        assertEquals(DELTAS, new DeltaSequence(deltas, endVersion));
      }
    };
    String channelId = "";

    m.addWavelet(W1A);
    m.subscribe(W1, IdFilters.ALL_IDS, channelId, listener);
    m.onUpdate(W1A, DeltaSequence.empty(serialize(HashedVersion.UNSIGNED_VERSION_0)));
    assertEquals(0, updates.get());
    m.onUpdate(W1A, DELTAS);
    assertEquals(1, updates.get());
  }
}

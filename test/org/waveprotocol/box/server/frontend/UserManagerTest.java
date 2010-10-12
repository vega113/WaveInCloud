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

package org.waveprotocol.box.server.frontend;

import static org.mockito.Mockito.mock;

import com.google.common.collect.ImmutableList;

import junit.framework.TestCase;

import org.mockito.Matchers;
import org.mockito.Mockito;
import org.waveprotocol.box.server.common.DeltaSequence;
import org.waveprotocol.box.server.common.VersionedWaveletDelta;
import org.waveprotocol.box.server.frontend.UserManager;
import org.waveprotocol.box.server.frontend.WaveViewSubscription;
import org.waveprotocol.box.server.frontend.WaveletSnapshotAndVersion;
import org.waveprotocol.box.server.frontend.ClientFrontend.OpenListener;
import org.waveprotocol.wave.model.id.IdFilter;
import org.waveprotocol.wave.model.id.IdFilters;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.core.CoreNoOp;
import org.waveprotocol.wave.model.operation.core.CoreWaveletDelta;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

  private static final VersionedWaveletDelta DELTA =
    new VersionedWaveletDelta(new CoreWaveletDelta(USER,
        HashedVersion.UNSIGNED_VERSION_0, ImmutableList.of(CoreNoOp.INSTANCE, CoreNoOp.INSTANCE)),
        HashedVersion.UNSIGNED_VERSION_0);

  private static final HashedVersion END_VERSION = HashedVersion.unsigned(2);

  private static final DeltaSequence DELTAS =
      new DeltaSequence(ImmutableList.of(DELTA), END_VERSION);

  private UserManager m;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    m = new UserManager();
  }

  // TODO(arb): add tests testing subscriptions with channelIds.

  /**
   * Tests that {@link UserManager#matchSubscriptions(WaveletName)} accurately
   * reflects subscription, independent of whether we actually are on any
   * wavelets.
   */
  public void testMatchSubscriptions() {
    assertEquals(ImmutableList.<OpenListener>of(), m.matchSubscriptions(W1A));

    OpenListener l1 = mock(OpenListener.class, "listener 1");
    OpenListener l2 = mock(OpenListener.class, "listener 2");
    OpenListener l3 = mock(OpenListener.class, "listener 3");
    OpenListener l4 = mock(OpenListener.class, "listener 4");
    OpenListener l5 = mock(OpenListener.class, "listener 5");
    String channelId = "";

    m.subscribe(W2, IdFilter.ofIds(WA), channelId, l1);
    m.subscribe(W2, IdFilters.ALL_IDS, channelId, l2);
    m.subscribe(W1, IdFilter.ofPrefixes("", WA.getId()), channelId, l3);
    m.subscribe(W2, IdFilters.NO_IDS, channelId, l4);
    m.subscribe(W2, IdFilter.ofPrefixes("A", "B"), channelId, l5);

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
      List<WaveViewSubscription> matchedSubscriptions) {
    List<OpenListener> actualListeners = new ArrayList<OpenListener>();
    for (WaveViewSubscription subscription : matchedSubscriptions) {
      actualListeners.add(subscription.getOpenListener());
    }
    assertEquals(expectedListeners, actualListeners);
  }

  /**
   * Tests that sending a single delta with the correct start version
   * number 0 to a wavelet we're subscribed to succeeds.
   */
  public void testUpdateSingleDeltaVersion() {
    m.subscribe(W1, IdFilters.ALL_IDS, "channel", mock(OpenListener.class));
    m.onUpdate(W1A, DELTAS); // pass
  }

  /**
   * Test that a second delta marked as version 2 = DELTA.getOperationCount()
   * succeeds.
   */
  public void testUpdateSeveralDeltas() {
    // Check that test was set up correctly
    assertEquals(2, DELTA.delta.getOperations().size());

    HashedVersion v2 = HashedVersion.unsigned(2);

    CoreWaveletDelta coreDelta2 = new CoreWaveletDelta(USER, v2, Arrays.asList(CoreNoOp.INSTANCE));
    VersionedWaveletDelta delta2 = new VersionedWaveletDelta(coreDelta2, v2);

    m.subscribe(W1, IdFilters.ALL_IDS, "ch1", mock(OpenListener.class));

    HashedVersion endVersion2 = HashedVersion.unsigned(3);
    m.onUpdate(W1A, new DeltaSequence(ImmutableList.of(DELTA, delta2), endVersion2)); // success

    // Also succeeds when sending the two deltas via separate onUpdates()
    m.subscribe(W2, IdFilters.ALL_IDS, "ch2", mock(OpenListener.class));
    m.onUpdate(W2A, DELTAS); // success
    m.onUpdate(W2A, new DeltaSequence(ImmutableList.of(delta2), endVersion2)); // success
  }

  /**
   * Tests that subscribed listeners are only invoked from onUpdate when
   * at least one delta is passed in.
   */
  public void testListenersInvokedOnlyForNonemptyDeltas() {
    OpenListener listener = mock(OpenListener.class, "1");
    String channelId = "ch";
    m.subscribe(W1, IdFilters.ALL_IDS, channelId, listener);
    m.onUpdate(W1A, DeltaSequence.empty(HashedVersion.UNSIGNED_VERSION_0));
    Mockito.verifyZeroInteractions(listener);
    m.onUpdate(W1A, DELTAS);
    Mockito.verify(listener).onUpdate(Mockito.eq(W1A), (WaveletSnapshotAndVersion)Matchers.isNull(),
        Mockito.eq(DELTAS.getDeltas()), Mockito.eq(DELTAS.getEndVersion()),
        Mockito.any(HashedVersion.class), Mockito.eq(false), Mockito.eq(channelId));
  }
}

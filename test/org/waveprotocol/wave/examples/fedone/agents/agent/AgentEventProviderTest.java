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

package org.waveprotocol.wave.examples.fedone.agents.agent;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import junit.framework.TestCase;

import org.waveprotocol.wave.examples.fedone.util.testing.TestingConstants;
import org.waveprotocol.wave.examples.fedone.waveclient.common.ClientUtils;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.core.CoreWaveletDocumentOperation;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.core.CoreWaveletData;

/**
 * Tests for the {@code AgentEventProvider}.
 *
 * @author mk.mateng@gmail.com (Michael Kuntzman)
 */
public class AgentEventProviderTest extends TestCase implements TestingConstants {
  private static final ParticipantId SELF_PARTICIPANT = PARTICIPANT;

  private static final String SELF_USER = USER;

  private static final CoreWaveletDocumentOperation OPERATION =
      new CoreWaveletDocumentOperation(BLIP_ID, ClientUtils.createEmptyDocument());

  /** A stubbed connection for the event provider */
  private AgentConnection connection;

  /** A listener for the generated agent events */
  private AgentEventListener listener;

  /** The provider we're testing */
  private AgentEventProvider provider;

  /** A stubbed wavelet */
  private CoreWaveletData wavelet;

  @Override
  public void setUp() {
    connection = mock(AgentConnection.class);
    when(connection.isConnected()).thenReturn(true);
    when(connection.getParticipantId()).thenReturn(SELF_PARTICIPANT);

    listener = mock(AgentEventListener.class);

    provider = new AgentEventProvider(connection);
    provider.addAgentEventListener(listener);

    wavelet = mock(CoreWaveletData.class);
    when(wavelet.getWaveletName()).thenReturn(WAVELET_NAME);
  }

  // Tests

  /** Should correctly identify events generated by other users. */
  public void testIdentifiesNonSelfGeneratedEvents() {
    assertFalse(provider.isSelfGeneratedEvent(OTHER_USER, wavelet));
  }

  /** Should correctly identify self-generated events. */
  public void testIdentifiesSelfGeneratedEvents() {
    assertTrue(provider.isSelfGeneratedEvent(SELF_USER, wavelet));
  }

  /** Should forward PaticipantAdded events when some other user is added. */
  public void testForwardsParticipantAdded() {
    provider.participantAdded(OTHER_USER, wavelet, OTHER_PARTICIPANT);
    verify(listener).onParticipantAdded(wavelet, OTHER_PARTICIPANT);
  }  

  /**
   * Should ignore PaticipantAdded events when not connected.
   * See {@link shouldIgnoreDocumentUpdatedWhenNotConnected} for details.
   */
  public void testIgnoresParticipantAddedWhenNotConnected() {
    when(connection.isConnected()).thenReturn(false);
    provider.participantAdded(OTHER_USER, wavelet, OTHER_PARTICIPANT);
    verifyZeroInteractions(listener);
  }

  /** Should generate SelfAdded events when agent is added by another user. */
  public void testGeneratesSelfAddedByOtherUser() {
    provider.participantAdded(OTHER_USER, wavelet, SELF_PARTICIPANT);
    verify(listener).onSelfAdded(wavelet);
  }

  /** Should generate SelfAdded events when agent is added by itself. */
  public void testGeneratesSelfAddedBySelf() {
    provider.participantAdded(SELF_USER, wavelet, SELF_PARTICIPANT);
    verify(listener).onSelfAdded(wavelet);
  }

  /** Should forward PaticipantRemoved events when some other user is removed. */
  public void testForwardsParticipantRemoved() {
    provider.participantRemoved(OTHER_USER, wavelet, OTHER_PARTICIPANT);
    verify(listener).onParticipantRemoved(wavelet, OTHER_PARTICIPANT);
  }

  /**
   * Should ignore PaticipantRemoved events when not connected.
   * See {@link shouldIgnoreDocumentUpdatedWhenNotConnected} for details.
   */
  public void testIgnoresParticipantRemovedWhenNotConnected() {
    when(connection.isConnected()).thenReturn(false);
    provider.participantRemoved(OTHER_USER, wavelet, OTHER_PARTICIPANT);
    verifyZeroInteractions(listener);
  }

  /** Should generate SelfRemoved events when agent is removed by another user. */
  public void testGeneratesSelfRemovedByOtherUser() {
    provider.participantRemoved(OTHER_USER, wavelet, SELF_PARTICIPANT);
    verify(listener).onSelfRemoved(wavelet);
  }

  /** Should generate SelfRemoved events when agent is removed by itself. */
  public void testGeneratesSelfRemovedBySelf() {
    provider.participantRemoved(SELF_USER, wavelet, SELF_PARTICIPANT);
    verify(listener).onSelfRemoved(wavelet);
  }

  /** Should forward DocumentUpdated events. */
  public void testForwardsDocumentUpdated() {
    provider.waveletDocumentUpdated(OTHER_USER, wavelet, OPERATION);
    verify(listener).onDocumentChanged(wavelet, OPERATION);
  }

  /**
   * Should ignore DocumentUpdated events when not connected. If events are fed to the event
   * provider from a separate thread, the event provider might receive events after the connection
   * has been closed. These should be ignored to avoid accessing a null backend (and possibly other
   * problems).
   */
  public void testIgnoresDocumentUpdatedWhenNotConnected() {
    when(connection.isConnected()).thenReturn(false);
    provider.waveletDocumentUpdated(OTHER_USER, wavelet, OPERATION);
    verifyZeroInteractions(listener);
  }
}

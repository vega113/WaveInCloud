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

package org.waveprotocol.wave.examples.fedone.agents.agent;

import com.google.common.annotations.VisibleForTesting;

import org.waveprotocol.wave.examples.client.common.WaveletOperationListener;
import org.waveprotocol.wave.examples.fedone.common.HashedVersion;
import org.waveprotocol.wave.examples.fedone.frontend.IndexWave;
import org.waveprotocol.wave.examples.fedone.util.Log;
import org.waveprotocol.wave.examples.fedone.util.WaveletDataUtil;
import org.waveprotocol.wave.model.document.operation.BufferedDocOp;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.WaveletData;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Provides agent events.
 */
public class AgentEventProvider implements WaveletOperationListener, AgentEventListener {
  private static final Log LOG = Log.get(AgentEventProvider.class);

  private final AgentConnection connection;
  private final Set<AgentEventListener> listeners = new HashSet<AgentEventListener>();

  /**
   * Constructor.
   *
   * @param connection a connection to the server.
   */
  AgentEventProvider(AgentConnection connection) {
    this.connection = connection;
    this.connection.addWaveletOperationListener(this);
  }

  /**
   * Adds an event listener to this agent.
   *
   * @param listener the event listener.
   */
  void addAgentEventListener(AgentEventListener listener) {
    listeners.add(listener);
  }

  /**
   * @return the list of currently registered agent event listeners
   */
  @VisibleForTesting
  Set<AgentEventListener> getListeners() {
    return Collections.unmodifiableSet(listeners);
  }

  /**
   * Should updates by the passed author onto the given wavelet be ignored by
   * agents.
   */
  private boolean isIgnored(String author, WaveletData wavelet) {
    // If we receive events from a separate thread, we might receive them after
    // disconnecting.
    if (!connection.isConnected()) {
      return true;
    }
    return IndexWave.isIndexWave(WaveletDataUtil.waveletNameOf(wavelet).waveId);
  }

  /**
   * Returns true if this was an event originated by the agent itself.
   */
  @VisibleForTesting
  boolean isSelfGeneratedEvent(String author, WaveletData wavelet) {
    // TODO: in the coming agent framework refactor, change author to be
    // a ParticipantId throughout.
    return new ParticipantId(author).equals(connection.getParticipantId());
  }

  @Override
  public void noOp(String author, WaveletData wavelet) {}

  @Override
  public void onDeltaSequenceEnd(WaveletData wavelet) {}

  @Override
  public void onDeltaSequenceStart(WaveletData wavelet) {}

  @Override
  public void onCommitNotice(WaveletData wavelet, HashedVersion version) {}

  @Override
  public void onDocumentChanged(WaveletData wavelet, String docId, BufferedDocOp docOp) {
    for (AgentEventListener l : listeners) {
      l.onDocumentChanged(wavelet, docId, docOp);
    }
  }

  @Override
  public void onParticipantAdded(WaveletData wavelet, ParticipantId participant) {
    for (AgentEventListener l : listeners) {
      l.onParticipantAdded(wavelet, participant);
    }
  }

  @Override
  public void onParticipantRemoved(WaveletData wavelet, ParticipantId participant) {
    for (AgentEventListener l : listeners) {
      l.onParticipantRemoved(wavelet, participant);
    }
  }

  @Override
  public void onSelfAdded(WaveletData wavelet) {
    for (AgentEventListener l : listeners) {
      l.onSelfAdded(wavelet);
    }
  }

  @Override
  public void onSelfRemoved(WaveletData wavelet) {
    for (AgentEventListener l : listeners) {
      l.onSelfRemoved(wavelet);
    }
  }

  @Override
  public void participantAdded(String author, WaveletData wavelet,
      ParticipantId participantId) {
    if (isIgnored(author, wavelet)) {
      return;
    }

    if (participantId.equals(connection.getParticipantId())) {
      onSelfAdded(wavelet);
    } else {
      onParticipantAdded(wavelet, participantId);
    }
  }

  @Override
  public void participantRemoved(String author, WaveletData wavelet,
      ParticipantId participantId) {
    if (isIgnored(author, wavelet)) {
      return;
    }

    if (participantId.equals(connection.getParticipantId())) {
      onSelfRemoved(wavelet);
    } else {
      onParticipantRemoved(wavelet, participantId);
    }
  }

  /**
   * Start listening for events.
   */
  void startListening() {
    boolean done = false;
    while (!done) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        LOG.info("Aborting.");
        return;
      }
    }
  }

  @Override
  public void waveletDocumentUpdated(String author, WaveletData wavelet,
      String docId, BufferedDocOp docOp) {
    if (isIgnored(author, wavelet) || isSelfGeneratedEvent(author, wavelet)) {
      return;
    }
    onDocumentChanged(wavelet, docId, docOp);
  }
}

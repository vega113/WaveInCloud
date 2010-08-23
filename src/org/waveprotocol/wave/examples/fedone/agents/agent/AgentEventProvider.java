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

import org.waveprotocol.wave.examples.fedone.common.CommonConstants;
import org.waveprotocol.wave.examples.fedone.common.HashedVersion;
import org.waveprotocol.wave.examples.fedone.util.Log;
import org.waveprotocol.wave.examples.fedone.waveclient.common.WaveletOperationListener;
import org.waveprotocol.wave.model.operation.core.CoreWaveletDocumentOperation;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.core.CoreWaveletData;

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
   * Should updates by the passed author onto the given wavelet be ignored by
   * agents.
   */
  private boolean isIgnored(String author, CoreWaveletData wavelet) {
    return wavelet.getWaveletName().waveId.equals(CommonConstants.INDEX_WAVE_ID);
  }

  /**
   * Returns true if this was an event originated by the agent itself.
   */
  private boolean isSelfEvent(String author, CoreWaveletData wavelet) {
    // TODO: in the coming agent framework refactor, change author to be
    // a participantId throughout.
    return new ParticipantId(author).equals(connection.getParticipantId());
  }

  @Override
  public void noOp(String author, CoreWaveletData wavelet) {}

  @Override
  public void onDeltaSequenceEnd(CoreWaveletData wavelet) {}

  @Override
  public void onDeltaSequenceStart(CoreWaveletData wavelet) {}

  @Override
  public void onCommitNotice(CoreWaveletData wavelet, HashedVersion version) {}

  @Override
  public void onDocumentChanged(CoreWaveletData wavelet, CoreWaveletDocumentOperation documentOperation) {
    for (AgentEventListener l : listeners) {
      l.onDocumentChanged(wavelet, documentOperation);
    }
  }

  @Override
  public void onParticipantAdded(CoreWaveletData wavelet, ParticipantId participant) {
    for (AgentEventListener l : listeners) {
      l.onParticipantAdded(wavelet, participant);
    }
  }

  @Override
  public void onParticipantRemoved(CoreWaveletData wavelet, ParticipantId participant) {
    for (AgentEventListener l : listeners) {
      l.onParticipantRemoved(wavelet, participant);
    }
  }

  @Override
  public void onSelfAdded(CoreWaveletData wavelet) {
    for (AgentEventListener l : listeners) {
      l.onSelfAdded(wavelet);
    }
  }

  @Override
  public void onSelfRemoved(CoreWaveletData wavelet) {
    for (AgentEventListener l : listeners) {
      l.onSelfRemoved(wavelet);
    }
  }

  @Override
  public void participantAdded(String author, CoreWaveletData wavelet, ParticipantId participantId) {
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
  public void participantRemoved(String author, CoreWaveletData wavelet, ParticipantId participantId) {
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
  public void waveletDocumentUpdated(String author, CoreWaveletData wavelet, CoreWaveletDocumentOperation operation) {
    if (isIgnored(author, wavelet) || isSelfEvent(author, wavelet)) {
      return;
    }
    onDocumentChanged(wavelet, operation);
  }
}

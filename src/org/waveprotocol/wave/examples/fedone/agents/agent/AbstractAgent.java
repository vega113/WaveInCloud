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

import org.waveprotocol.wave.examples.client.common.ClientBackend;
import org.waveprotocol.wave.examples.client.common.ClientWaveView;
import org.waveprotocol.wave.examples.fedone.util.Log;
import org.waveprotocol.wave.examples.fedone.util.SuccessFailCallback;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveClientRpc;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.core.CoreWaveletDelta;
import org.waveprotocol.wave.model.operation.core.CoreWaveletOperation;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.io.IOException;

/**
 * Abstract base class implementation of a federated agent.
 */
public abstract class AbstractAgent implements AgentEventListener {
  private static final Log LOG = Log.get(AbstractAgent.class);

  private final AgentConnection connection;
  private AgentEventProvider eventProvider;

  /**
   * Constructor.
   *
   * @param connection the agent's connection to the server.
   */
  protected AbstractAgent(AgentConnection connection) {
    this.connection = connection;
  }

  @VisibleForTesting
  void connect() {
    try {
      connection.connect();
      // These must be done after we connect, so that we have a backend available to register for
      // events:
      eventProvider = new AgentEventProvider(connection);
      eventProvider.addAgentEventListener(this);
    } catch (IOException e) {
      throw new RuntimeException("Failed to connect.", e);
    }
    LOG.info("Connected as " + connection.getParticipantId());
  }

  @VisibleForTesting
  void disconnect() {
    connection.disconnect();
  }

  /**
   * @return the agent's backend.
   */
  @VisibleForTesting
  ClientBackend getBackend() {
    return connection.getBackend();
  }

  /**
   * @return the agent's event provider.
   */
  @VisibleForTesting
  AgentEventProvider getEventProvider() {
    return eventProvider;
  }

  /**
   * @return true if the agent is connected to a server.
   */
  @VisibleForTesting
  public boolean isConnected() {
    return connection.isConnected();
  }

  private void listen() {
    eventProvider.startListening();
  }

  /**
   * Starts the agent.
   */
  protected final void run() {
    connect();
    listen();
    disconnect();
  }

  /**
   * @return a new random document id for this agent.
   */
  protected final String getNewDocumentId() {
    return connection.getNewDocumentId();
  }

  /**
   * Creates a new wave for this agent, and adds the agent as a participant.
   *
   * @return the new wave.
   */
  protected ClientWaveView newWave() {
    return connection.newWave();
  }

  /**
   * Fetches an existing wave for this agent.
   *
   * @param waveId the wave ID
   * @return the wave, or null
   */
  protected ClientWaveView getWave(WaveId waveId) {
    return connection.getWave(waveId);
  }

  /**
   * Return the participant ID for this agent.
   */
  public final ParticipantId getParticipantId() {
    return connection.getParticipantId();
  }

  /**
   * Sends an operation to server.
   *
   * @param waveletName of the wavelet to apply the operation to.
   * @param operation the operation to apply.
   * @param callback completion callback
   */
  public void sendWaveletOperation(WaveletName waveletName, CoreWaveletOperation operation,
      SuccessFailCallback<WaveClientRpc.ProtocolSubmitResponse, String> callback) {
    connection.sendWaveletOperation(waveletName, operation, callback);
  }

  /**
   * Sends an operation to server and waits for it to be applied locally.
   *
   * @param waveletName of the wavelet to apply the operation to.
   * @param operation the operation to apply.
   */
  public void sendAndAwaitWaveletOperation(WaveletName waveletName,
      CoreWaveletOperation operation) {
    connection.sendAndAwaitWaveletOperation(waveletName, operation);
  }

  /**
   * Sends a delta to the server.
   *
   * @param waveletName of the wavelet to apply the operation to
   * @param waveletDelta to send
   * @param callback completion callback
   */
  public void sendWaveletDelta(WaveletName waveletName, CoreWaveletDelta waveletDelta,
      SuccessFailCallback<WaveClientRpc.ProtocolSubmitResponse, String> callback) {
    connection.sendWaveletDelta(waveletName, waveletDelta, callback);
  }

    /**
   * Sends a delta to the server and waits for a response.
   *
   * @param waveletName of the wavelet to apply the operation to
   * @param waveletDelta to send
   */
  public void sendAndAwaitWaveletDelta(WaveletName waveletName, CoreWaveletDelta waveletDelta) {
    connection.sendAndAwaitWaveletDelta(waveletName, waveletDelta);
  }
}

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

package org.waveprotocol.box.server.agents.agent;

import com.google.common.annotations.VisibleForTesting;

import org.waveprotocol.box.client.ClientBackend;
import org.waveprotocol.box.client.ClientWaveView;
import org.waveprotocol.box.server.util.Log;
import org.waveprotocol.box.server.util.SuccessFailCallback;
import org.waveprotocol.box.server.waveserver.WaveClientRpc;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.wave.BasicWaveletOperationContextFactory;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperationContext;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.io.IOException;

/**
 * Abstract base class implementation of a federated agent.
 */
public abstract class AbstractAgent implements AgentEventListener {
  private static final Log LOG = Log.get(AbstractAgent.class);

  private final AgentConnection connection;
  private AgentEventProvider eventProvider;
  protected WaveletOperationContext.Factory contextFactory;


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
      this.contextFactory = new BasicWaveletOperationContextFactory(getParticipantId());
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
   * Sends operations to server.
   *
   * @param waveletName of the wavelet to send the operations to
   * @param callback completion callback
   * @param operations operations to send
   */
  public void sendWaveletOperations(WaveletName waveletName,
      SuccessFailCallback<WaveClientRpc.ProtocolSubmitResponse, String> callback,
      WaveletOperation... operations) {
    connection.sendWaveletOperations(waveletName, callback, operations);
  }

  /**
   * Sends an operation to server and waits for it to be applied locally.
   *
   * @param waveletName of the wavelet to send the operations to
   * @param operations operations to send
   */
  public void sendAndAwaitWaveletOperations(WaveletName waveletName,
      WaveletOperation... operations) {
    connection.sendAndAwaitWaveletOperations(waveletName, operations);
  }
}

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

import org.waveprotocol.wave.examples.fedone.util.Log;
import org.waveprotocol.wave.examples.fedone.waveclient.common.ClientWaveView;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.io.IOException;

/**
 * Abstract base class implementation of a federated agent.
 */
public abstract class AbstractAgent implements AgentEventListener {
  private static final Log LOG = Log.get(AbstractAgent.class);

  private final AgentConnection connection;

  /**
   * Constructor.
   *
   * @param connection the agent's connection to the server.
   */
  protected AbstractAgent(AgentConnection connection) {
    this.connection = connection;
  }

  private void connect() {
    try {
      connection.connect();
    } catch (IOException e) {
      throw new RuntimeException("Failed to connect.", e);
    }
    LOG.info("Connected as " + connection.getParticipantId());
  }

  private void disconnect() {
    connection.disconnect();
  }

  private void listen() {
    AgentEventProvider provider = new AgentEventProvider(connection);
    provider.addAgentEventListener(this);
    provider.startListening();
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
   * Return the participant ID for this agent.
   */
  protected final ParticipantId getParticipantId() {
    return connection.getParticipantId();
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
  protected ClientWaveView getWave(String waveId) {
    return connection.getWave(WaveId.deserialise(waveId));
  }
  
  /**
   * Sends an operation to server.
   *
   * @param waveletName of the wavelet to apply the operation to.
   * @param operation the operation to apply.
   */
  public void sendWaveletOperation(WaveletName waveletName, WaveletOperation operation) {
    connection.sendWaveletOperation(waveletName, operation);
  }

  /**
   * Sends a delta to the server.
   *
   * @param waveletName of the wavelet to apply the operation to
   * @param waveletDelta to send
   */
  public void sendWaveletDelta(WaveletName waveletName, WaveletDelta waveletDelta) {
    connection.sendWaveletDelta(waveletName, waveletDelta);
  }
}

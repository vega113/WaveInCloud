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
import org.waveprotocol.wave.examples.fedone.waveclient.common.ClientBackend;
import org.waveprotocol.wave.examples.fedone.waveclient.common.WaveletOperationListener;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Provides agent connections which are used to communicate with the server.
 */
public class AgentConnection {
  private static final Log LOG = Log.get(AgentConnection.class);

  /**
   * Creates a new connection to the server for an agent.
   *
   * @param participantId the agent's participant id.
   * @param hostname the server hostname.
   * @param port the server port.
   * @return an agent connection.
   */
  public static AgentConnection newConnection(String participantId, String hostname, int port) {
    return new AgentConnection(participantId, hostname, port);
  }
  private ClientBackend backend = null;
  private final String hostname;
  private final String participantId;

  private final int port;

  private AgentConnection(String participantId, String hostname, int port) {
    this.participantId = participantId;
    this.hostname = hostname;
    this.port = port;
  }

  /**
   * Adds a wavelet operation listener to this connection if we're connected.
   *
   * @param listener the operation listener.
   */
  void addWaveletOperationListener(WaveletOperationListener listener) {
    if (isConnected()) {
      backend.addWaveletOperationListener(listener);
    }
  }

  /**
   * Starts the connection.
   *
   * @throws IOException if a connection error occurred.
   */
  void connect() throws IOException {
    backend = new ClientBackend(participantId, hostname, port);
  }

  /**
   * Disconnects from the server.
   */
  void disconnect() {
    if (isConnected()) {
      LOG.info("Closing connection.");
      backend.shutdown();
      backend = null;
    }
  }

  /**
   * Returns this agent's participant id.
   *
   * @return the agent's participant id.
   */
  String getParticipantId() {
    return participantId;
  }

  /**
   * Returns true if the agent is currently connected.
   *
   * @return true if the agent is connected.
   */
  boolean isConnected() {
    return backend != null;
  }

  /**
   * Submits a wavelet operation to the backend and waits for it to be applied locally (round trip).
   *
   * @param waveletName of the wavelet to operate on.
   * @param operation the operation to apply to the wavelet.
   */
  public void sendWaveletOperation(WaveletName waveletName, WaveletOperation operation) {
    if (!isConnected()) {
      throw new IllegalStateException("Not connected.");
    }
    backend.sendAndAwaitWaveletOperation(waveletName, operation, 1, TimeUnit.MINUTES);
  }
}

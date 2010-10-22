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
import org.waveprotocol.box.client.WaveletOperationListener;
import org.waveprotocol.box.server.util.BlockingSuccessFailCallback;
import org.waveprotocol.box.server.util.Log;
import org.waveprotocol.box.server.util.SuccessFailCallback;
import org.waveprotocol.box.server.waveserver.WaveClientRpc.ProtocolSubmitResponse;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.core.CoreWaveletOperation;
import org.waveprotocol.wave.model.wave.ParticipantId;

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
  public static AgentConnection newConnection(
      String participantId, char[] password, String hostname, int port) {
    return new AgentConnection(
        participantId, password, hostname, port, new ClientBackend.DefaultFactory());
  }

  /**
   * A factory used to construct the client backend instance.
   */
  private final ClientBackend.Factory backendFactory;

  private final String hostname;
  private final String participantId;
  private final int port;

  /**
   * The client backend used for this connection. This is set in
   * {@link #connect} and set to null in {@link #disconnect}.
   */
  private ClientBackend backend = null;

  private char[] password;

  /**
   * Default constructor.
   *
   * @param participantId the participant address (user@domain) of the agent.
   * @param password the agent's password
   * @param hostname of the server.
   * @param port number of the server.
   * @param backendFactory the factory to use to create a backend for this
   * connection.
   */
  @VisibleForTesting
  AgentConnection(String participantId, char[] password, String hostname, int port,
      ClientBackend.Factory backendFactory) {
    this.backendFactory = backendFactory;
    this.participantId = participantId;
    this.password = password;
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
    backend = backendFactory.create(participantId, hostname, port);
    if (!backend.authenticate(password)) {
      throw new IOException("Authentication failure");
    }
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
   * @return the agent's backend.
   */
  @VisibleForTesting
  ClientBackend getBackend() {
    return backend;
  }

  /**
   * Returns this agent's participant id.
   *
   * @return the agent's participant id.
   */
  ParticipantId getParticipantId() {
    return backend.getUserId();
  }

  /**
   * @return a new random document id
   */
  String getNewDocumentId() {
    return backend.getIdGenerator().newBlipId();
  }

  /**
   * Returns true if the agent is currently connected.
   *
   * @return true if the agent is connected.
   */
  boolean isConnected() {
    return backend != null;
  }

  public ClientWaveView newWave() {
    BlockingSuccessFailCallback<ProtocolSubmitResponse, String> callback =
        BlockingSuccessFailCallback.create();
    ClientWaveView waveview = backend.createConversationWave(callback);
    callback.await(1, TimeUnit.MINUTES);
    return waveview;
  }

  public ClientWaveView getWave(WaveId waveId) {
    return backend.getWave(waveId);
  }

  /**
   * Submits wavelet operations to the backend.
   *
   * @param waveletName of the wavelet to operate on.
   * @param callback the callback that will be invoked
   * @param operations the operation to apply to the wavelet.
   */
  public void sendWaveletOperations(WaveletName waveletName,
      SuccessFailCallback<ProtocolSubmitResponse, String> callback,
      CoreWaveletOperation... operations) {
    if (!isConnected()) {
      throw new IllegalStateException("Not connected.");
    }
    backend.sendWaveletOperations(waveletName, callback, operations);
  }

  /**
   * Submits wavelet operations to the backend and waits for them to be applied
   * locally (round trip).
   *
   * @param waveletName of the wavelet to operate on.
   * @param operations the operation to apply to the wavelet.
   */
  public void sendAndAwaitWaveletOperations(WaveletName waveletName,
      CoreWaveletOperation... operations) {
    if (!isConnected()) {
      throw new IllegalStateException("Not connected.");
    }
    backend.sendAndAwaitWaveletOperations(waveletName, 1, TimeUnit.MINUTES, operations);
  }
}

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

package org.waveprotocol.box.webclient.client;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.websockets.client.WebSocket;
import com.google.gwt.websockets.client.WebSocketCallback;

import org.waveprotocol.box.server.waveserver.ProtocolOpenRequest;
import org.waveprotocol.box.server.waveserver.ProtocolSubmitRequest;
import org.waveprotocol.box.server.waveserver.ProtocolSubmitResponse;
import org.waveprotocol.box.server.waveserver.ProtocolWaveletUpdate;
import org.waveprotocol.box.webclient.client.events.NetworkStatusEvent;
import org.waveprotocol.box.webclient.client.events.NetworkStatusEvent.ConnectionStatus;
import org.waveprotocol.box.webclient.util.Log;
import org.waveprotocol.box.webclient.waveclient.common.SubmitResponseCallback;
import org.waveprotocol.box.webclient.waveclient.common.WebClientBackend;

import java.util.HashMap;
import java.util.Map;


/**
 * Wrapper around Websocket that handles the FedOne client-server protocol.
 */
public class WaveWebSocketClient implements WebSocketCallback {
  private static final Log LOG = Log.get(WaveWebSocketClient.class);
  private static final int RECONNECT_TIME_MS = 5000;
  private static final int VERSION = 1;

  private final WebSocket client;
  private final Map<Integer, SubmitResponseCallback> submitRequestCallbacks;

  private enum ConnectState {
    CONNECTED, CONNECTING, DISCONNECTED
  }

  private ConnectState connected = ConnectState.DISCONNECTED;
  private String url;
  private WebClientBackend legacy;
  private WaveWebSocketCallback callback;
  private int sequenceNo;

  private final RepeatingCommand reconnectCommand = new RepeatingCommand() {
    public boolean execute() {
      if (connected == ConnectState.DISCONNECTED) {
        LOG.info("Attemping to reconnect");
        connected = ConnectState.CONNECTING;
        client.connect(url);
      }
      return true;
    }
  };

  public WaveWebSocketClient() {
    submitRequestCallbacks = new HashMap<Integer, SubmitResponseCallback>();
    client = new WebSocket(this);
  }

  /**
   * Attaches the legacy wave handling object, notifying it of incoming updates.
   */
  public void attachLegacy(WebClientBackend legacy) {
    Preconditions.checkState(this.legacy == null);
    Preconditions.checkArgument(legacy != null);
    this.legacy = legacy;
  }

  /**
   * Attaches the handler for incoming messages. Once the client's workflow has
   * been fixed, this callback attachment will become part of
   * {@link #connect(String)}.
   */
  public void attachHandler(WaveWebSocketCallback callback) {
    Preconditions.checkState(this.callback == null);
    Preconditions.checkArgument(callback != null);
    this.callback = callback;
  }

  /**
   * Opens this connection.
   *
   * @param url
   */
  public void connect(String url) {
    this.url = url;

    reconnectCommand.execute();
    Scheduler.get().scheduleFixedDelay(reconnectCommand, RECONNECT_TIME_MS);
  }

  @Override
  public void onConnect() {
    connected = ConnectState.CONNECTED;
    ClientEvents.get().fireEvent(new NetworkStatusEvent(ConnectionStatus.CONNECTED));
  }

  @Override
  public void onDisconnect() {
    connected = ConnectState.DISCONNECTED;
    ClientEvents.get().fireEvent(new NetworkStatusEvent(ConnectionStatus.DISCONNECTED));
  }

  @Override
  public void onMessage(final String message) {
    LOG.info("received JSON message " + message);
    JSONValue json = JSONParser.parseStrict(message);
    // TODO(arb): pull apart the wrapper, extract the message.
    JSONObject wrapper = json.isObject();
    String messageType = wrapper.get("messageType").isString().stringValue();
    String payload = wrapper.get("messageJson").isString().stringValue();
    if (messageType.equals("ProtocolWaveletUpdate")) {
      ProtocolWaveletUpdate payloadMessage = ProtocolWaveletUpdate.parse(payload);
      if (callback != null) {
        callback.onWaveletUpdate(payloadMessage);
      }
      if (legacy != null) {
        legacy.receiveWaveletUpdate(payloadMessage);
      }
    } else if (messageType.equals("ProtocolSubmitResponse")) {
      ProtocolSubmitResponse payloadMessage = ProtocolSubmitResponse.parse(payload);
      SubmitResponseCallback submitCallback = submitRequestCallbacks.remove(
          (int) wrapper.get("sequenceNumber").isNumber().doubleValue());
      submitCallback.run(payloadMessage);
    }
  }

  /**
   *
   * @param message
   * @param callback callback to invoke for response, or null for none.
   */
  public void sendMessage(JavaScriptObject message, SubmitResponseCallback callback) {
    Preconditions.checkState(connected == ConnectState.CONNECTED);
    int seqNo = sequenceNo++;

    JSONObject wrapper = new JSONObject();
    wrapper.put("version", new JSONNumber(VERSION));
    wrapper.put("sequenceNumber", new JSONNumber(seqNo));
    final String protocolBufferName = ProtocolOpenRequest.getProtocolBufferName(message);
    wrapper.put("messageType", new JSONString(protocolBufferName));
    deleteMessageName(message);

    if (protocolBufferName.equals("ProtocolOpenRequest")) {
      wrapper.put("messageJson",
          new JSONString(ProtocolOpenRequest.stringify((ProtocolOpenRequest) message)));
    } else if (protocolBufferName.equals("ProtocolSubmitRequest")) {
      wrapper.put("messageJson",
          new JSONString(ProtocolSubmitRequest.stringify((ProtocolSubmitRequest) message)));
      submitRequestCallbacks.put(seqNo, callback);
    }
    String json = wrapper.toString();
    LOG.info("Sending JSON data " + json);
    client.send(json);
  }

  // TODO(arb): filthy filthy hack. make this not necessary
  private native void deleteMessageName(final JavaScriptObject message) /*-{
    delete message._protoMessageName;
  }-*/;
}

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

package org.waveprotocol.wave.examples.client.webclient.client;

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

import org.waveprotocol.wave.examples.client.webclient.util.Log;
import org.waveprotocol.wave.examples.client.webclient.waveclient.common.SubmitResponseCallback;
import org.waveprotocol.wave.examples.fedone.waveserver.ProtocolOpenRequest;
import org.waveprotocol.wave.examples.fedone.waveserver.ProtocolSubmitRequest;
import org.waveprotocol.wave.examples.fedone.waveserver.ProtocolSubmitResponse;
import org.waveprotocol.wave.examples.fedone.waveserver.ProtocolWaveletUpdate;

import java.util.HashMap;
import java.util.Map;


/**
 * Wrapper around Websocket that handles the FedOne client-server protocol.
 */
public class WaveWebSocketClient {
  private static Log LOG = Log.get(WaveWebSocketClient.class);

  final int VERSION = 1;
  private final WebSocket client;
  private Map<Integer, SubmitResponseCallback> submitRequestCallbacks;

  private enum ConnectState {
    CONNECTED, CONNECTING, DISCONNECTED
  }
  private ConnectState connected = ConnectState.DISCONNECTED;
  private String url;
  private static final int RECONNECT_TIME_MS = 5000;

//  private Map<Int, Command<ProtocolSubmitResponse>> submitCallbacks =
//      new HashMap<Int, Callable<ProtocolSubmitResponse>>();
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
  public WaveWebSocketClient(final WaveWebSocketCallback callback) {
    submitRequestCallbacks = new HashMap<Integer, SubmitResponseCallback>();
    WebSocketCallback wsCallback = new WebSocketCallback() {

      public void onConnect() {
        connected = ConnectState.CONNECTED;
        callback.connected();
      }

      public void onDisconnect() {
        connected = ConnectState.DISCONNECTED;
        callback.disconnected();
      }

      public void onMessage(final String message) {
        LOG.info("received JSON message " + message);
        JSONValue json = JSONParser.parse(message);
        // TODO(arb): pull apart the wrapper, extract the message.
        JSONObject wrapper = json.isObject();
        String messageType = wrapper.get("messageType").isString().stringValue();
        String payload = wrapper.get("messageJson").isString().stringValue();
        if (messageType.equals("ProtocolWaveletUpdate")) {
          ProtocolWaveletUpdate payloadMessage = ProtocolWaveletUpdate.parse(payload);
          callback.handleWaveletUpdate(payloadMessage);
        } else if (messageType.equals("ProtocolSubmitResponse")) {
          ProtocolSubmitResponse payloadMessage = ProtocolSubmitResponse.parse(payload);
          SubmitResponseCallback submitCallback = submitRequestCallbacks.get(
              (int)wrapper.get("sequenceNumber").isNumber().doubleValue());
          submitCallback.run(payloadMessage);
        }
      }
    };

    client = new WebSocket(wsCallback);
  }

  public void connect(String url) {
    assert client!=null;
    this.url = url;
    reconnectCommand.execute();
    Scheduler.get().scheduleFixedDelay(reconnectCommand, RECONNECT_TIME_MS);
  }

  /**
   *
   * @param sequenceNo
   * @param message
   * @param callback callback to invoke for response, or null for none.
   */
  public void sendMessage(int sequenceNo, JavaScriptObject message, SubmitResponseCallback callback) {

    JSONObject wrapper = new JSONObject();
    wrapper.put("version", new JSONNumber(VERSION));
    wrapper.put("sequenceNumber", new JSONNumber(sequenceNo));
    final String protocolBufferName = ProtocolOpenRequest.getProtocolBufferName(message);
    wrapper.put("messageType", new JSONString(protocolBufferName));
    deleteMessageName(message);

    if (protocolBufferName.equals("ProtocolOpenRequest")) {
      wrapper.put("messageJson", new JSONString(
          ProtocolOpenRequest.stringify((ProtocolOpenRequest) message)));
    } else if (protocolBufferName.equals("ProtocolSubmitRequest")) {
      wrapper.put("messageJson", new JSONString(
          ProtocolSubmitRequest.stringify((ProtocolSubmitRequest) message)));
      submitRequestCallbacks.put(sequenceNo, callback);
    }
    String json = wrapper.toString();
    LOG.info("Sending JSON data " + json);
    client.send(json);
  }

  // TODO(arb): filthy filthy hack. make this not necessary
  private native void deleteMessageName(final JavaScriptObject message) /*-{
    delete message._protoMessageName;
  }-*/;

  public native void wrapMessage(int sequenceNumber, String messageType, String messageBody) /*-{
    return
  }-*/;


}

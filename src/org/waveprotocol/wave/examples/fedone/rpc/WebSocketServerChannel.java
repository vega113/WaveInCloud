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
 
package org.waveprotocol.wave.examples.fedone.rpc;

import java.io.IOException;

import com.google.gson.Gson;
import com.google.protobuf.JsonFormat;
import com.google.protobuf.Message;

import org.waveprotocol.wave.examples.fedone.util.Log;

import org.eclipse.jetty.websocket.WebSocket;

public class WebSocketServerChannel extends WebSocketChannel implements WebSocket {
  private static final Log LOG = Log.get(WebSocketServerChannel.class);
  
  private Outbound outbound;
  
  public WebSocketServerChannel(ProtoCallback callback) {
    super(callback);
  }
  
  public void onConnect(Outbound outbound) {
    this.outbound = outbound;
  }

  public void onMessage(byte frame, byte[] data, int offset, int length) {
    // do nothing. we don't expect this type of message.
  }
  
  public void onMessage(byte frame, String data) {
    handleMessageString(data);
  }
  
  public void onDisconnect() {
    LOG.info("disconnected: "+this);
  }
  
  /**
   * Send the given data String
   *
   * @param data
   */
  protected void sendMessageString(String data) {
    try {
      outbound.sendMessage((byte) 0x00, data);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }  
}
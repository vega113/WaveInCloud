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
import com.google.gson.JsonParseException;
import com.google.protobuf.JsonFormat;
import com.google.protobuf.Message;

import org.waveprotocol.wave.examples.fedone.util.Log;

import org.eclipse.jetty.websocket.WebSocket;

/**
 * A channel abstraction for websocket, for sending and receiving strings.
 */
public abstract class WebSocketChannel extends MessageExpectingChannel {
  private static final Log LOG = Log.get(WebSocketChannel.class);
  private static final int VERSION = 0;

  private final ProtoCallback callback;
  private Gson gson = new Gson();
  
  /**
   * Constructs a new WebSocketChannel, using the callback to handle any
   * incoming messages.
   *
   * @param callback a protocallback to be called when data arrives on this
   *                 channel
   */
  public WebSocketChannel(ProtoCallback callback) {
    this.callback = callback;
  }
  
  /**
   * A simple message wrapper that bundles a json string with a version,
   * sequence number, and type information.
   */
  private static class MessageWrapper {
    private int version;
    private long sequenceNumber;
    private String messageType;
    private String messageJson;
    
    MessageWrapper() {
      // no-args constructor
    }
    MessageWrapper(int version, long sequenceNumber, String messageType, 
        String messageJson) {
      this.version = version;
      this.sequenceNumber = sequenceNumber;
      this.messageType = messageType;
      this.messageJson = messageJson;
    }
  }
  
  /**
   * Convert the given string into a Message object and pass it to the proto 
   * callback.
   *
   * @param data A json-encoded MessageWrapper object.
   */
  public void handleMessageString(String data) {
    MessageWrapper wrapper = null;
    try {
      wrapper = gson.fromJson(data, MessageWrapper.class);
    } catch (JsonParseException jpe) {
      LOG.info("Unable to parse JSON: " + jpe.getMessage());
      throw new IllegalArgumentException(jpe);
    }
    
    if (wrapper.version != VERSION) {
      LOG.info("Bad message version number: " + wrapper.version);
      throw new IllegalArgumentException("Bad version number: " + wrapper.version);
    }
    
    Message prototype = getMessagePrototype(wrapper.messageType);
    if (prototype == null) {
      LOG.info("Received misunderstood message (??? " + wrapper.messageType + " ???, seq "
          + wrapper.sequenceNumber + ") from: " + this);
      callback.unknown(wrapper.sequenceNumber, wrapper.messageType, wrapper.messageJson);
    } else {
      Message.Builder builder = prototype.newBuilderForType();
      try {
        JsonFormat.merge(wrapper.messageJson, builder);
        callback.message(wrapper.sequenceNumber, builder.build());        
      } catch (JsonFormat.ParseException pe) {
        LOG.info("Unable to parse message (" + wrapper.messageType + ", seq "
          + wrapper.sequenceNumber + ") from: " + this + " -- " + wrapper.messageJson);
        callback.unknown(wrapper.sequenceNumber, wrapper.messageType, wrapper.messageJson);
      }
    }
  }
  
  /**
   * Send the given data String
   *
   * @param data
   */
  protected abstract void sendMessageString(String data);
  
  /**
   * Send the given message across the connection along with the sequence number.
   * 
   * @param sequenceNo
   * @param message
   */
  public void sendMessage(long sequenceNo, Message message) {
    sendMessageString(gson.toJson(new MessageWrapper(
      VERSION, sequenceNo, message.getDescriptorForType().getFullName(),
      JsonFormat.printToString(message))));      
  }
}
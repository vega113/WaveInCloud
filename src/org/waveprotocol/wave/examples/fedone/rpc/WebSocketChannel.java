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

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.protobuf.Message;

import org.waveprotocol.wave.examples.fedone.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * A channel abstraction for websocket, for sending and receiving strings.
 */
public abstract class WebSocketChannel extends MessageExpectingChannel {
  private static final Log LOG = Log.get(WebSocketChannel.class);
  private static final int VERSION = 1;

  /**
   * A simple message wrapper that bundles a json string with a version,
   * sequence number, and type information.
   */
  private static class MessageWrapper {
    public int version;
    public long sequenceNumber;
    public String messageType;
    public String messageJson;

    /** No-args constructor for {@link Gson#fromJson(String,Class)}. */
    public MessageWrapper() { }

    public MessageWrapper(int version, long sequenceNumber, String messageType,
        String messageJson) {
      this.version = version;
      this.sequenceNumber = sequenceNumber;
      this.messageType = messageType;
      this.messageJson = messageJson;
    }
  }

  private final ProtoCallback callback;
  private Gson gson = new Gson();
  private Map<String, Class<? extends Message>> protosByName;
  private ProtoSerializer serializer;

  /**
   * Constructs a new WebSocketChannel, using the callback to handle any
   * incoming messages.
   *
   * @param callback a protocallback to be called when data arrives on this
   *                 channel
   */
  public WebSocketChannel(ProtoCallback callback) {
    this.callback = callback;
    // The ProtoSerializer could really be singleton.
    // TODO: Figure out a way to inject a singleton instance using Guice
    this.serializer = new ProtoSerializer();
    protosByName = Maps.newHashMap();
    for (Class<? extends Message> class_ : ProtoSerializer.MODULE_CLASSES) {
      protosByName.put(shortName(class_.getName()), class_);
    }
  }

  private String shortName(final String className) {
    String[] pieces = className.split("[\\.\\$]");
    return pieces[pieces.length-1];
  }

  public void handleMessageString(String data) {
    LOG.fine("received JSON message " + data);
    MessageWrapper wrapper = gson.fromJson(data, MessageWrapper.class);
    Message m;
    Class<? extends Message> protoClass = protosByName.get(wrapper.messageType);
    try {
      m = serializer
          .parseFrom(new ByteArrayInputStream(wrapper.messageJson.getBytes()),
              protoClass);
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }
    LOG.fine("message was " + m.getDescriptorForType().getName());
    callback.message(wrapper.sequenceNumber, m);
  }

  /**
   * Send the given data String
   *
   * @param data
   */
  protected abstract void sendMessageString(String data);

  @Override
  public void sendMessage(long sequenceNo, Message message) {
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    try {
      serializer.writeTo(outputStream, message);
    } catch (IOException e) {
      e.printStackTrace();
    }
    String json = outputStream.toString();

    MessageWrapper wrapper = new MessageWrapper(VERSION, sequenceNo,
        message.getDescriptorForType().getName(), json);
    sendMessageString(gson.toJson(wrapper));
    LOG.fine("sent JSON message over websocket, sequence number " + sequenceNo
        + ", message " + message.toString());
  }
}

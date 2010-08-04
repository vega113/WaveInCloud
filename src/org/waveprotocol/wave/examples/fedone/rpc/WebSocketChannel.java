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

import com.dyuproject.protostuff.json.ReflectionNumericJSON;

import org.waveprotocol.wave.examples.fedone.util.Log;
import org.waveprotocol.wave.examples.fedone.waveserver.MessageWrapper;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveClientRpc;
import org.waveprotocol.wave.federation.Proto;

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

  private final ProtoCallback callback;
  private Gson gson = new Gson();
  private final ReflectionNumericJSON jsonConverter;
  @SuppressWarnings("unchecked")
  private static final Class<? extends Message>[] MODULE_CLASSES = new Class[]{
      WaveClientRpc.ProtocolOpenRequest.class,
      WaveClientRpc.ProtocolSubmitRequest.class,
      WaveClientRpc.ProtocolSubmitResponse.class,
      WaveClientRpc.ProtocolWaveClientRpc.class,
      WaveClientRpc.ProtocolWaveletUpdate.class,
      WaveClientRpc.WaveletSnapshot.class,
      Proto.ProtocolAppliedWaveletDelta.class,
      Proto.ProtocolDocumentOperation.class,
      Proto.ProtocolDocumentOperation.Component.class,
      Proto.ProtocolDocumentOperation.Component.KeyValuePair.class,
      Proto.ProtocolDocumentOperation.Component.KeyValueUpdate.class,
      Proto.ProtocolDocumentOperation.Component.ElementStart.class,
      Proto.ProtocolDocumentOperation.Component.ReplaceAttributes.class,
      Proto.ProtocolDocumentOperation.Component.UpdateAttributes.class,
      Proto.ProtocolDocumentOperation.Component.AnnotationBoundary.class,
      Proto.ProtocolHashedVersion.class,
      Proto.ProtocolSignature.class,
      Proto.ProtocolSignedDelta.class,
      Proto.ProtocolSignerInfo.class,
      Proto.ProtocolWaveletDelta.class,
      Proto.ProtocolWaveletOperation.class,
      Proto.ProtocolWaveletOperation.MutateDocument.class,
      Rpc.CancelRpc.class,
      Rpc.RpcFinished.class,
  };
  private Map<String, Class<? extends Message>> protosByName;

  /**
   * Constructs a new WebSocketChannel, using the callback to handle any
   * incoming messages.
   *
   * @param callback a protocallback to be called when data arrives on this
   *                 channel
   */
  public WebSocketChannel(ProtoCallback callback) {
    this.callback = callback;
    jsonConverter = new ReflectionNumericJSON(MODULE_CLASSES);
    protosByName = Maps.newHashMap();
    for (Class<? extends Message> class_ : MODULE_CLASSES) {
      protosByName.put(shortName(class_.getName()), class_);
    }
  }

  private String shortName(final String className) {
    String[] pieces = className.split("[\\.\\$]");
    return pieces[pieces.length-1];
  }

  public void handleMessageString(String data) {
    LOG.info("received JSON message " + data);
    MessageWrapper wrapper = gson.fromJson(data, MessageWrapper.class);
    Message m;
    Class<? extends Message> protoClass = protosByName.get(wrapper.messageType);
    try {
      m = jsonConverter
          .parseFrom(new ByteArrayInputStream(wrapper.messageJson.getBytes()),
              protoClass);
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }
    LOG.info("message was " + m.getDescriptorForType().getName());
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
      jsonConverter.writeTo(outputStream, message);
    } catch (IOException e) {
      e.printStackTrace();
    }
    String json = outputStream.toString();

    MessageWrapper wrapper = new MessageWrapper(VERSION, sequenceNo,
        message.getDescriptorForType().getName(), json);
    sendMessageString(gson.toJson(wrapper));
    LOG.info("sent JSON message over websocket, sequence number " + sequenceNo + ", message " + message.toString());
  }
}

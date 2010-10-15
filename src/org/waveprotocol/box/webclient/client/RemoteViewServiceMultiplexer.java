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

import org.waveprotocol.box.server.waveserver.ProtocolOpenRequest;
import org.waveprotocol.box.server.waveserver.ProtocolSubmitRequest;
import org.waveprotocol.box.server.waveserver.ProtocolWaveletUpdate;
import org.waveprotocol.box.webclient.util.URLEncoderDecoderBasedPercentEncoderDecoder;
import org.waveprotocol.box.webclient.waveclient.common.SubmitResponseCallback;
import org.waveprotocol.wave.model.id.IdFilter;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.id.URIEncoderDecoder;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.util.CollectionUtils;

import java.util.Map;


/**
 * Multiplexes incoming
 */
public final class RemoteViewServiceMultiplexer implements WaveWebSocketCallback {

  private static final IdURIEncoderDecoder URI_CODEC =
      new IdURIEncoderDecoder(new URLEncoderDecoderBasedPercentEncoderDecoder());

  /** Per-wave streams. */
  private final Map<WaveId, WaveWebSocketCallback> streams = CollectionUtils.newHashMap();

  /** Underlying socket. */
  private final WaveWebSocketClient socket;

  /** Identity, for authoring messages. */
  private final String userId;

  /**
   * Creates a multiplexer.
   *
   * @param socket communication object
   * @param userId identity of viewer
   */
  public RemoteViewServiceMultiplexer(WaveWebSocketClient socket, String userId) {
    this.socket = socket;
    this.userId = userId;

    // Note: Currently, the client's communication stack (websocket) is opened
    // too early, before an identity is established. Once that is fixed, this
    // object will be registered as a callback when the websocket is opened,
    // rather than afterwards here.
    socket.attachHandler(this);
  }

  /** Dispatches an update to the appropriate wave stream. */
  @Override
  public void onWaveletUpdate(ProtocolWaveletUpdate message) {
    WaveletName wavelet = deserialize(message.getWaveletName());

    // Route to the appropriate stream handler.
    WaveWebSocketCallback stream = streams.get(wavelet.waveId);
    if (stream != null) {
      stream.onWaveletUpdate(message);
    } else {
      // This is either a server error, or a message after a stream has been
      // locally closed (there is no way to tell the server to stop sending
      // updates).
    }
  }

  /**
   * Opens a wave stream.
   *
   * @param id wave to open
   * @param stream handler to updates directed at that wave
   */
  public void open(WaveId id, IdFilter filter, WaveWebSocketCallback stream) {
    // Prepare to receive updates for the new stream.
    streams.put(id, stream);

    // Request those updates.
    ProtocolOpenRequest request = ProtocolOpenRequest.create();
    request.setWaveId(id.serialise());
    request.setParticipantId(userId);
    for (String prefix : filter.getPrefixes()) {
      request.addWaveletIdPrefix(prefix);
    }
    socket.sendMessage(request, null);
  }

  /**
   * Closes a wave stream.
   *
   * @param id wave to close
   * @param stream stream previously registered against that wave
   */
  public void close(WaveId id, WaveWebSocketCallback stream) {
    if (streams.get(id) == stream) {
      streams.remove(id);
    }

    // The C/S protocol does not support closing the stream.
  }

  /**
   * Submits a delta.
   *
   * @param request delta to submit
   * @param callback callback for submit response
   */
  public void submit(ProtocolSubmitRequest request, SubmitResponseCallback callback) {
    request.getDelta().setAuthor(userId);
    socket.sendMessage(request, callback);
  }

  public static WaveletName deserialize(String name) {
    try {
      return URI_CODEC.uriToWaveletName(name);
    } catch (URIEncoderDecoder.EncodingException e) {
      throw new IllegalArgumentException(e);
    }
  }

  public static String serialize(WaveletName name) {
    try {
      return URI_CODEC.waveletNameToURI(name);
    } catch (URIEncoderDecoder.EncodingException e) {
      throw new IllegalArgumentException(e);
    }
  }
}

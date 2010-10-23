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

package org.waveprotocol.box.webclient.waveclient.common;

import com.google.gwt.core.client.JavaScriptObject;

import org.waveprotocol.box.common.CommonConstants;
import org.waveprotocol.box.server.waveserver.ProtocolOpenRequest;
import org.waveprotocol.box.server.waveserver.ProtocolWaveletUpdate;
import org.waveprotocol.box.server.waveserver.WaveletSnapshot;
import org.waveprotocol.box.webclient.client.ClientEvents;
import org.waveprotocol.box.webclient.client.WaveWebSocketClient;
import org.waveprotocol.box.webclient.client.events.NetworkStatusEvent;
import org.waveprotocol.box.webclient.client.events.NetworkStatusEventHandler;
import org.waveprotocol.box.webclient.common.CoreWaveletOperationSerializer;
import org.waveprotocol.box.webclient.common.WaveletOperationSerializer;
import org.waveprotocol.box.webclient.util.Log;
import org.waveprotocol.box.webclient.util.URLEncoderDecoderBasedPercentEncoderDecoder;
import org.waveprotocol.wave.concurrencycontrol.channel.WaveViewService;
import org.waveprotocol.wave.concurrencycontrol.common.Delta;
import org.waveprotocol.wave.federation.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.IdGeneratorImpl;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.id.URIEncoderDecoder;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.wave.ConversionUtil;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperationContext;
import org.waveprotocol.wave.model.util.CharBase64;
import org.waveprotocol.wave.model.util.Pair;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.version.HashedVersionZeroFactoryImpl;
import org.waveprotocol.wave.model.wave.Constants;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.DocumentFactory;
import org.waveprotocol.wave.model.wave.data.core.CoreWaveletData;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Backend for the web client.
 *
 * @author Anthony Baxter (arb@google.com)
 */
public class WebClientBackend {
  private static final Log LOG = Log.get(WebClientBackend.class);
  /**
   * User id of the user of the backend (encapsulating both user and server).
   */
  final ParticipantId userId;

  private NetworkStatusEvent.ConnectionStatus connectionStatus;

  /**
   * A cache of known waves. TODO(arb): think about ways to stop this growing without limit.
   */
  private final Map<WaveId, WaveViewServiceImpl> waveViews = new HashMap<WaveId, WaveViewServiceImpl>();

  /**
   * Id URI encoder and decoder.
   */
  private static final IdURIEncoderDecoder URI_CODEC =
      new IdURIEncoderDecoder(new URLEncoderDecoderBasedPercentEncoderDecoder());

  private static final HashedVersionZeroFactoryImpl HASH_FACTORY =
      new HashedVersionZeroFactoryImpl(URI_CODEC);

  List<Pair<JavaScriptObject, SubmitResponseCallback>> queuedMessages =
      new ArrayList<Pair<JavaScriptObject, SubmitResponseCallback>>();

  /**
   * Waves this backend is aware of.
   */
  private final Map<WaveId, WebClientWaveView> waves = new HashMap<WaveId, WebClientWaveView>();
  final WaveWebSocketClient websocket;
  private final IdGenerator idGenerator;

  public WebClientBackend(final ParticipantId userId, WaveWebSocketClient websocket) {
    connectionStatus = NetworkStatusEvent.ConnectionStatus.CONNECTED;
    this.userId = userId;
    this.websocket = websocket;
    this.idGenerator = new IdGeneratorImpl(this.userId.getDomain(), new IdGeneratorImpl.Seed() {
      private final String seed;

      {
        String start = userId.getAddress() + System.currentTimeMillis();
        char[] chars = start.toCharArray();
        byte[] bytes = new byte[chars.length];
        for (int i = 0, j = chars.length; i < j; i++) {
          bytes[i] = (byte) chars[i];
        }
        seed = CharBase64.encodeWebSafe(bytes, false);
      }

      @Override
      public String get() {
        return seed;
      }
    });
    ClientEvents.get().addNetworkStatusEventHandler(new NetworkStatusEventHandler() {

      @Override
      public void onNetworkStatus(final NetworkStatusEvent event) {
        LOG.info("got network status event " + event.getStatus());
        connectionStatus = event.getStatus();
        switch (event.getStatus()) {
          case CONNECTED:
            networkReconnected();
            break;
          case DISCONNECTED:
            break;
          case RECONNECTED:
            networkReconnected();
            break;
          case RECONNECTING:
            break;
          case NEVER_CONNECTED:
            break;

        }
      }
    });
  }

  private void networkReconnected() {
    sendQueuedMessages();
    // Drop and recreate the index wave, such that we can get new v0 deltas.
    // TODO(arb): We should instead more generically revert back to the version of any 'old'
    // delta given to us by the server.
    WebClientWaveView waveView = waves.remove(CommonConstants.INDEX_WAVE_ID);
    if (waveView != null) {
      waveView = createWave(CommonConstants.INDEX_WAVE_ID);
      WaveViewServiceImpl viewService = waveViews.get(CommonConstants.INDEX_WAVE_ID);
      if (viewService != null) {
        viewService.updateClientWaveView(waveView);
      }
    }
    reopenWaves();
  }

  private void reopenWaves() {
    for (WaveViewServiceImpl viewService : waveViews.values()) {
      viewService.reopen();
    }
  }

  /**
   * Creates a new, empty wave view and stores it in {@code waves}.
   *
   * @param waveId the new wave id
   * @return the new wave's {@code ClientWaveView}
   */
  private WebClientWaveView createWave(WaveId waveId) {
    WebClientWaveView wave = new WebClientWaveView(HASH_FACTORY, waveId);
    waves.put(waveId, wave);
    return wave;
  }

  /**
   * Sends a message, queueing it if the network is down.
   *
   * @param message the message to send
   * @param callback for submit requests, a callback to trigger when we get a response.
   */
  private void sendMessage(final JavaScriptObject message,
      final SubmitResponseCallback callback) {
    if (connectionStatus != NetworkStatusEvent.ConnectionStatus.CONNECTED &&
        connectionStatus != NetworkStatusEvent.ConnectionStatus.RECONNECTED) {
      queuedMessages.add(
          new Pair<JavaScriptObject, SubmitResponseCallback>(message, callback));
    } else {
      websocket.sendMessage(message, callback);
    }
  }

  private void sendQueuedMessages() {
    while (!queuedMessages.isEmpty()) {
      Pair<JavaScriptObject, SubmitResponseCallback> messageAndCallback = queuedMessages.remove(0);
      websocket.sendMessage(messageAndCallback.first, messageAndCallback.second);
    }
  }

  private boolean isIndexWave(WaveletName waveletName) {
    return waveletName.waveId.equals(CommonConstants.INDEX_WAVE_ID);
  }

  private boolean isDummyWavelet(WaveletName waveletName) {
    return "dummy+root".equals(waveletName.waveletId.getId());
  }

  /**
   * Receive a protocol wavelet update from the wave server.
   *
   * @param waveletUpdate the wavelet update
   */
  public void receiveWaveletUpdate(final ProtocolWaveletUpdate waveletUpdate) {
    LOG.info("Received update for " + waveletUpdate.getWaveletName());

    WaveletName waveletName;
    try {
      waveletName = URI_CODEC.uriToWaveletName(waveletUpdate.getWaveletName());
    } catch (URIEncoderDecoder.EncodingException e) {
      throw new IllegalArgumentException(e);
    }
    LOG.info("wavelet name decoding " + waveletUpdate.getWaveletName() + " -> " + waveletName);

    WebClientWaveView wave = waves.get(waveletName.waveId);
    if (wave == null) {
      // Not a wave for us.
      return;
    }

    CoreWaveletData oldWaveletData = wave.getWavelet(waveletName.waveletId);
    if (oldWaveletData == null) {
      oldWaveletData = wave.createWavelet(waveletName.waveletId);
    }

    HashedVersion previousVersion = null;

    // Apply operations to the wavelet.
    List<Pair<String, WaveletOperation>> successfulOps =
        new ArrayList<Pair<String, WaveletOperation>>();
    if (waveletUpdate.hasSnapshot()) {
      LOG.info("applying snapshot");
      final WaveletSnapshot snapshot = waveletUpdate.getSnapshot();


      // Kinda bogus - we need something better here.
      // TODO(arb): talk to soren about this - what should we do about contributors?
//      final String creator = snapshot.getParticipantId(0);
//      for (CoreWaveletOperation op : WaveletOperationSerializer.deserialize(snapshot)) {
//        try {
//          op.apply(oldWaveletData);
////          successfulOps.add(Pair.of(creator, op));
//        } catch (OperationException e) {
//          // It should be okay (if cheeky) for the client to just ignore failed ops.  In any case,
//          // this should never happen if our server is behaving correctly.
//          LOG.severe("OperationException when applying snapshot " + op + " to " + wavelet, e);
//        }
//      }
    } else if (waveletUpdate.getAppliedDeltaCount() > 0) {
      previousVersion = waves.get(waveletName.waveId).getWaveletVersion(waveletName.waveletId);
      for (int i = 0; i < waveletUpdate.getAppliedDeltaCount(); i++) {
        ProtocolWaveletDelta protobufDelta = waveletUpdate.getAppliedDelta(i);
        WaveletOperationContext woc =
            new WaveletOperationContext(new ParticipantId(protobufDelta.getAuthor()),
                Constants.NO_TIMESTAMP, 1);
        Delta deltaAndVersion =
            WaveletOperationSerializer.deserialize(protobufDelta,
                CoreWaveletOperationSerializer.deserialize(protobufDelta.getHashedVersion()), woc);

        // TODO(anorth): Use the right timestamp when the protocol supplies it.
        long applicationTimestamp = 0L;
        final TransformedWaveletDelta oldDelta = CoreWaveletOperationSerializer.deserialize(
            protobufDelta,
            CoreWaveletOperationSerializer.deserialize(waveletUpdate.getResultingVersion()),
            applicationTimestamp);

        if (isIndexWave(waveletName)) { // only apply the hacky ops to index wave.
          for (WaveletOperation op : oldDelta.getOperations()) {
            try {
              ConversionUtil.toCoreWaveletOperation(op).apply(oldWaveletData);
            } catch (OperationException e) {
              LOG.severe("OperationException when applying " + op + " to " + oldWaveletData, e);
            }
          }
        }

        for (WaveletOperation op : deltaAndVersion) {
//          try {
//            op.apply(wavelet);
          successfulOps.add(Pair.of(protobufDelta.getAuthor(), op));
//          } catch (OperationException e) {
//            // It should be okay (if cheeky) for the client to just ignore failed ops.  In any case,
//            // this should never happen if our server is behaving correctly.
//            LOG.severe("OperationException when applying " + op + " to " + wavelet, e);
//          }
        }
      }
    }

    if (isIndexWave(waveletName) && waveletUpdate.hasResultingVersion()) {
      wave.setWaveletVersion(waveletName.waveletId, CoreWaveletOperationSerializer
          .deserialize(waveletUpdate.getResultingVersion()));
    }
    // If we have been removed from this wavelet then remove the data too, since if we're re-added
    // then we will get a fresh snapshot or deltas from version 0, not the latest version we've
    // seen.
    if (isIndexWave(waveletName) && !oldWaveletData.getParticipants().contains(getUserId())) {
      wave.removeWavelet(waveletName.waveletId);
    }
    LOG.info("applied wavelet update for " + waveletName.waveletId.serialise());

    if (waveViews.containsKey(wave.getWaveId())) {
      WaveViewServiceImpl waveView = waveViews.get(wave.getWaveId());
      LOG.info("Have a WaveViewServiceImpl for " + wave.getWaveId().toString());

      if (waveletUpdate.hasSnapshot()) {
        LOG.info("Publishing snapshot update");
        waveView.publishSnapshot(waveletName, waveletUpdate);
      } else if (!isDummyWavelet(waveletName)) {
        // Publish operations.
        List<ProtocolWaveletDelta> deltaList = new AbstractList<ProtocolWaveletDelta>() {
          @Override
          public ProtocolWaveletDelta get(int i) {
            return waveletUpdate.getAppliedDelta(i);
          }

          @Override
          public int size() {
            return waveletUpdate.getAppliedDeltaCount();
          }
        };
        waveView.publishDeltaList(waveletName, deltaList,
            waveletUpdate.hasCommitNotice() ? waveletUpdate.getCommitNotice() : null,
            waveletUpdate.hasResultingVersion() ? waveletUpdate.getResultingVersion() : null,
            waveletUpdate.hasChannelId() ? waveletUpdate.getChannelId() : null);

        if (waveletUpdate.hasCommitNotice()) {
          LOG.info("Publishing commit notice");
          waveView.publishCommitNotice(waveletName, CoreWaveletOperationSerializer
              .deserialize(waveletUpdate.getCommitNotice()));
        }
      } else if (waveletUpdate.hasChannelId()) {
        waveView.publishChannelId(waveletName, waveletUpdate.getChannelId());
      }
      if (waveletUpdate.hasMarker() && waveletUpdate.getMarker() && !isDummyWavelet(waveletName)) {
        LOG.info("update had an up-to-date marker");
        waveView.publishMarker(waveletName);
      }
    }
  }

  /**
   * @return the id generator which generates wave, wavelet, and document ids
   */
  public IdGenerator getIdGenerator() {
    return idGenerator;
  }

  /**
   * Returns a {@code WaveViewService} for the given WaveId. This method will return immediately and
   * updates will be delivered to callback.onUpdate() registered with WaveViewService.viewOpen.
   *
   * @param waveId the wave ID
   * @param waveletIdPrefix a filter for the wavelet IDs, or null.
   * @return the WaveViewService for the wave.
   */
  public WaveViewService getWaveView(WaveId waveId, String waveletIdPrefix,
      DocumentFactory<?> documentFactory) {
    LOG.info("getWaveView for " + waveId);
    if (waveViews.get(waveId) == null) {
      WebClientWaveView clientWaveView = createWave(waveId);

      waveViews.put(waveId, new WaveViewServiceImpl(this, waveId, waveletIdPrefix, clientWaveView,
          documentFactory));
    }
    ProtocolOpenRequest openRequest = ProtocolOpenRequest.create();

    openRequest.setParticipantId(getUserId().getAddress());
    openRequest.setWaveId(waveId.serialise());
    if (waveletIdPrefix != null) {
      openRequest.addWaveletIdPrefix(waveletIdPrefix);
    } else {
      openRequest.addWaveletIdPrefix("");
    }
    LOG.info("Opening wave " + waveId + " for prefix \"" + waveletIdPrefix + '"');
    sendMessage(openRequest, null);

    return waveViews.get(waveId);
  }

  public void clearWaveView(WaveId waveId) {
    waveViews.remove(waveId);
  }

  /**
   * @return a view on the special wave containing the index data
   */
  public WaveViewService getIndexWave(DocumentFactory<?> documentFactory) {
    return getWaveView(CommonConstants.INDEX_WAVE_ID, null, documentFactory);
  }


  public ParticipantId getUserId() {
    return userId;
  }

  public void sendRequest(final JavaScriptObject request,
      final SubmitResponseCallback submitResponseCallback) {
    // TODO(arb): check the network is live, else queue.
    sendMessage(request, submitResponseCallback);
  }
}

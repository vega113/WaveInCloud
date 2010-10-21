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

import org.waveprotocol.box.server.waveserver.DocumentSnapshot;
import org.waveprotocol.box.server.waveserver.ProtocolOpenRequest;
import org.waveprotocol.box.server.waveserver.ProtocolSubmitRequest;
import org.waveprotocol.box.server.waveserver.ProtocolSubmitResponse;
import org.waveprotocol.box.server.waveserver.ProtocolWaveletUpdate;
import org.waveprotocol.box.server.waveserver.WaveletVersion;
import org.waveprotocol.box.webclient.client.RemoteWaveViewService;
import org.waveprotocol.box.webclient.common.CoreWaveletOperationSerializer;
import org.waveprotocol.box.webclient.common.WaveletOperationSerializer;
import org.waveprotocol.box.webclient.util.Log;
import org.waveprotocol.box.webclient.util.URLEncoderDecoderBasedPercentEncoderDecoder;
import org.waveprotocol.wave.concurrencycontrol.channel.WaveViewService;
import org.waveprotocol.wave.concurrencycontrol.common.Delta;
import org.waveprotocol.wave.concurrencycontrol.common.ResponseCode;
import org.waveprotocol.wave.federation.ProtocolHashedVersion;
import org.waveprotocol.wave.federation.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.document.operation.BufferedDocOp;
import org.waveprotocol.wave.model.document.operation.impl.DocOpUtil;
import org.waveprotocol.wave.model.id.IdFilter;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.id.URIEncoderDecoder;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperationContext;
import org.waveprotocol.wave.model.util.Pair;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.version.HashedVersionFactory;
import org.waveprotocol.wave.model.version.HashedVersionZeroFactoryImpl;
import org.waveprotocol.wave.model.wave.Constants;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.DocumentFactory;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.data.core.CoreWaveletData;
import org.waveprotocol.wave.model.wave.data.impl.WaveletDataImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Notifies clients about events on a wave.
 *
 * @deprecated use {@link RemoteWaveViewService} instead.
 */
@Deprecated
public class WaveViewServiceImpl implements WaveViewService {

  private static final Log LOG = Log.get(WaveViewServiceImpl.class);

  private static final IdURIEncoderDecoder URI_CODEC =
      new IdURIEncoderDecoder(new URLEncoderDecoderBasedPercentEncoderDecoder());

  private final WaveId waveId;
  // Map of channel id to <filter, listener>
  private Pair<IdFilter, OpenCallback> waveletFilter;
  // TODO(arb): remove WebClientWaveView entirely.
  private WebClientWaveView clientWaveView;
  private final WebClientBackend clientBackend;
  private final String waveletIdPrefix;
  private final DocumentFactory<?> documentFactory;
  private final Map<WaveletName, Map<Long, ProtocolHashedVersion>> versionToHistoryHashMap =
      new HashMap<WaveletName, Map<Long, ProtocolHashedVersion>>();
  private final HashedVersionFactory hashedVersionFactory =
      new HashedVersionZeroFactoryImpl(URI_CODEC);

  /**
   * Constructor
   *
   * @param clientBackend the client backend - used to send messages
   * @param waveId the ID of this wave.
   * @param waveletIdPrefix wavelets that are of interest
   * @param clientWaveView a wrapper around the wavelet data
   * @param documentFactory used to deserialize snapshots
   */
  WaveViewServiceImpl(final WebClientBackend clientBackend, WaveId waveId,
      final String waveletIdPrefix, final WebClientWaveView clientWaveView,
      final DocumentFactory<?> documentFactory) {
    this.documentFactory = documentFactory;
    this.clientBackend = clientBackend;
    this.waveId = waveId;
    this.clientWaveView = clientWaveView;
    this.waveletIdPrefix = waveletIdPrefix;
  }

  public void updateClientWaveView(WebClientWaveView clientWaveView) {
    this.clientWaveView = clientWaveView;
  }

  public WaveId getWaveId() {
    return this.waveId;
  }

  /**
   * Gets the wavelets in this wave view. The order of iteration is unspecified.
   *
   * @return wavelets in this wave view.
   */
  public Iterable<? extends CoreWaveletData> getWavelets() {
    return clientWaveView.getWavelets();
  }

  /**
   * Gets the last known version for a wavelet.
   *
   * @param waveletId of the wavelet
   * @return last known version for wavelet
   */
  public HashedVersion getWaveletVersion(WaveletId waveletId) {
    return clientWaveView.getWaveletVersion(waveletId);

  }

  /**
   * Get a wavelet from the view by id.
   *
   * @param waveletId the wavelet id
   * @return the requested wavelet, or null if it is not in view.
   */
  public CoreWaveletData getWavelet(WaveletId waveletId) {
    return clientWaveView.getWavelet(waveletId);
  }

  /**
   * Open one or more wavelets. To view all wavelets on the wave, specify an empty filter and an
   * empty set of known wavelets. Changes to the wave and the initial snapshot will be sent back via
   * the specified callback.
   *
   * @param filter filter on wavelet IDs
   * @param knownWavelets wavelets and versions that we already know about
   * @param callback callback to signal wavelet updates
   */
  @Override
  public void viewOpen(final IdFilter filter,
      final Map<WaveletId, List<HashedVersion>> knownWavelets,
      final OpenCallback callback) {
    LOG.info("viewOpen called on " + waveId + " with " + filter);
    // In this implementation, we can ignore knownWavelets. This is used for reconnect logic,
    // which the fedone server doesn't support at all.
    waveletFilter = new Pair<IdFilter, OpenCallback>(filter, callback);

    // knownWavelets is an optimisation in the real ViewServer protocol, not used here.

    // TODO(arb): we need to send a wavelet snapshot for any wavelets we already have in cache.
  }

  @Override
  public String viewSubmit(final WaveletName waveletName,
      final Delta delta,
      final String channelId,
      final SubmitCallback callback) {
    ProtocolHashedVersion waveletVersion;

    if (delta.getVersion() == 0) {
      waveletVersion = CoreWaveletOperationSerializer.serialize(
          hashedVersionFactory.createVersionZero(waveletName));
    } else {
      waveletVersion = versionToHistoryHashMap.get(waveletName).get(delta.getVersion());
    }
    LOG.severe("VERSION IN SUBMIT IS " + waveletVersion.getVersion() + " " +
        waveletVersion.getHistoryHash());
    ProtocolSubmitRequest submitRequest = ProtocolSubmitRequest.create();
    String waveletNameString;
    try {
      waveletNameString =
          URI_CODEC.waveletNameToURI(WaveletName.of(waveletName.waveId, waveletName.waveletId));
    } catch (URIEncoderDecoder.EncodingException e) {
      throw new IllegalArgumentException(e);
    }
    submitRequest.setWaveletName(waveletNameString);
    ProtocolWaveletDelta protocolDelta = ProtocolWaveletDelta.create();
    for (WaveletOperation op : delta) {
      protocolDelta.addOperation(WaveletOperationSerializer.serialize(op));
    }
    protocolDelta.setAuthor(clientBackend.userId.toString());
    protocolDelta.setHashedVersion(waveletVersion);

    submitRequest.setDelta(protocolDelta);
    submitRequest.setChannelId(channelId);
    clientBackend.sendRequest(submitRequest, new SubmitResponseCallback() {
      @Override
      public void run(final ProtocolSubmitResponse response) {
        updateVersionMap(waveletName, response.getHashedVersionAfterApplication());
        HashedVersion resultVersion = CoreWaveletOperationSerializer.deserialize(
            response.getHashedVersionAfterApplication());
        callback.onSuccess(resultVersion, response.getOperationsApplied(), null, ResponseCode.OK);
      }
    });
    // We don't support the getDebugProfiling thing anyway.
    return null;
  }

  @Override
  public void viewClose(final WaveId waveId, final String channelId,
      final CloseCallback callback) {
    LOG.info("closing viewserver channel " + this);
    // waveletFilter = null;
    callback.onSuccess();
    // TODO(arb): consider whether to remove a wave with no listeners from the waves map.
    // TODO(arb): the client server protocol needs a ProtocolCloseRequest. Oops.
  }

  @Override
  public String debugGetProfilingInfo(final String requestId) {
    return null;
  }

  /**
   * Publish a commit notice to the relevant listeners.
   *
   * @param wavelet the wavelet that had the commit notice
   * @param hashedVersion the commit notice version
   */
  void publishCommitNotice(final WaveletName wavelet, final HashedVersion hashedVersion) {
    final WebClientWaveViewUpdate commitUpdate =
        new WebClientWaveViewUpdate().setWaveletId(wavelet.waveletId)
            .setLastCommittedVersion(hashedVersion);

    for (OpenCallback listener : lookupListenersForWavelet(wavelet)) {
      listener.onUpdate(commitUpdate);
    }

  }

  void publishMarker(final WaveletName waveletName) {
    final WebClientWaveViewUpdate markerUpdate =
        new WebClientWaveViewUpdate().setWaveletId(waveletName.waveletId)
            .setMarker(true);
    final List<OpenCallback> listeners = lookupListenersForWavelet(waveletName);

    for (OpenCallback listener : listeners) {
      LOG.info("PUBLISHING MARKER FOR " + waveletName + " to listener " + listener);
      listener.onUpdate(markerUpdate);
    }
  }

  public void publishDeltaList(WaveletName waveletName, List<ProtocolWaveletDelta> protobufDeltaList,
      final ProtocolHashedVersion commitNotice, final ProtocolHashedVersion resultingVersion,
      String channelId) {

    ArrayList<Delta> deltaList = new ArrayList<Delta>();
    for (int i = 0; i < protobufDeltaList.size(); i++) {
      ProtocolHashedVersion deltaEndVersion = (i < protobufDeltaList.size() - 1)
          ? protobufDeltaList.get(i + 1).getHashedVersion()
          : resultingVersion;
      ProtocolWaveletDelta protobufDelta = protobufDeltaList.get(i);
      WaveletOperationContext woc =
          new WaveletOperationContext(new ParticipantId(protobufDelta.getAuthor()),
              Constants.NO_TIMESTAMP, 1);
      Delta delta =
          WaveletOperationSerializer.deserialize(protobufDelta,
              CoreWaveletOperationSerializer.deserialize(deltaEndVersion), woc);
      updateVersionMap(waveletName, deltaEndVersion);
      deltaList.add(delta);
    }
    LOG.info("Publishing deltas: "+deltaList.toString());

    HashedVersion resultingHashedVersion =
        CoreWaveletOperationSerializer.deserialize(resultingVersion);
    final WebClientWaveViewUpdate deltaUpdate =
      new WebClientWaveViewUpdate().setWaveletId(waveletName.waveletId)
          .setDeltaList(deltaList)
          .setCurrentVersion(resultingHashedVersion);

    if (commitNotice != null) {
      HashedVersion commitHashedVersion = CoreWaveletOperationSerializer.deserialize(commitNotice);
      deltaUpdate.setLastCommittedVersion(commitHashedVersion);
    }

    final List<OpenCallback> listeners = lookupListenersForWavelet(waveletName);
    for (OpenCallback listener : listeners) {
      if (channelId != null) {
        // Pass the channel ID back on the callback as a channel ID op
        listener.onUpdate(new WebClientWaveViewUpdate().setChannelId(channelId));
      }
      listener.onUpdate(deltaUpdate);
    }
  }

  /**
   * Publish a snapshot to the relevant listeners.
   * @param waveletName the wavelet name
   * @param waveletUpdate the update message
   */
  void publishSnapshot(final WaveletName waveletName, final ProtocolWaveletUpdate waveletUpdate) {
    final ObservableWaveletData waveletSnapshot = deserializeSnapshot(waveletName, waveletUpdate);
    LOG.info("publishing snapshot for " + waveletName);
    final long version = (long) waveletUpdate.getResultingVersion().getVersion();
    final WebClientWaveViewUpdate snapshotUpdate =
        new WebClientWaveViewUpdate().setWaveletId(waveletName.waveletId)
            .setWaveletSnapshot(waveletSnapshot)
            .setCurrentVersion(waveletSnapshot.getHashedVersion());
    if (waveletUpdate.hasCommitNotice()) {
      snapshotUpdate.setLastCommittedVersion(
          CoreWaveletOperationSerializer.deserialize(waveletUpdate.getCommitNotice()));
    } else {
      LOG.severe("snapshot was missing commit_notice. Things won't work right.");
    }
    updateVersionMap(waveletName, waveletUpdate.getResultingVersion());

    if (waveletUpdate.hasChannelId()) {
      LOG.severe("saw channelId " + waveletUpdate.getChannelId());
      publishChannelId(waveletName, waveletUpdate.getChannelId());
    }
    final List<OpenCallback> listeners = lookupListenersForWavelet(waveletName);
    for (OpenCallback listener : listeners) {
      listener.onUpdate(snapshotUpdate);
    }
  }

  private void updateVersionMap(final WaveletName waveletName,
      final ProtocolHashedVersion version) {
    if (!versionToHistoryHashMap.containsKey(waveletName)) {
      versionToHistoryHashMap.put(waveletName, new HashMap<Long, ProtocolHashedVersion>());
    }
    versionToHistoryHashMap.get(waveletName).put((long) version.getVersion(), version);
  }

  /**
   * Determine which listeners need to be told about updates for this wavelet.
   *
   * @param wavelet the WaveletData of the wave we've got an event for
   * @return a list of listeners to trigger
   */
  private List<OpenCallback> lookupListenersForWavelet(
      final WaveletName wavelet) {
    WaveletId waveletId = wavelet.waveletId;
    List<OpenCallback> callbacks =
        new ArrayList<OpenCallback>();
    if (waveletFilter != null && IdFilter.accepts(waveletFilter.first, waveletId)) {
      callbacks.add(waveletFilter.second);
    }
    return callbacks;
  }

  /**
   * Sends a ProtocolOpenRequest for the current known wavelets.
   */
  void reopen() {
    ProtocolOpenRequest openRequest = ProtocolOpenRequest.create();
    openRequest.setParticipantId(clientBackend.getUserId().getAddress());
    openRequest.setWaveId(waveId.serialise());
    if (waveletIdPrefix != null) {
      openRequest.addWaveletIdPrefix(waveletIdPrefix);
    } else {
      openRequest.addWaveletIdPrefix("");
    }
    for (CoreWaveletData wavelet : getWavelets()) {
      HashedVersion waveletVersion = getWaveletVersion(wavelet.getWaveletName().waveletId);
      ProtocolHashedVersion hashedVersion;
      if (waveletVersion.getVersion() == 0) {
        hashedVersion = CoreWaveletOperationSerializer.serialize(
            hashedVersionFactory.createVersionZero(wavelet.getWaveletName()));
      } else {
        hashedVersion = CoreWaveletOperationSerializer.serialize(waveletVersion);
      }
      openRequest.addKnownWavelet(WaveletVersion.create()
          .setHashedVersion(hashedVersion)
          .setWaveletId(wavelet.getWaveletName().waveletId.serialise()));
    }
    LOG.info("Opening wave " + waveId + " for prefix \"" + waveletIdPrefix
        + "\" with " + openRequest.getKnownWaveletCount() + " known wavelets.");
    clientBackend.sendRequest(openRequest, null);
  }

  private ObservableWaveletData deserializeSnapshot(WaveletName waveletName,
      ProtocolWaveletUpdate update) {
    final ParticipantId creator = new ParticipantId(update.getSnapshot().getParticipantId(0));
    long currentTimeMillis = System.currentTimeMillis();

    final long currentVersion = (long) update.getResultingVersion().getVersion();
    WaveletDataImpl waveletData = new WaveletDataImpl(waveletName.waveletId, creator,
        currentTimeMillis, currentVersion,
        CoreWaveletOperationSerializer.deserialize(update.getResultingVersion()),
        currentTimeMillis, waveletName.waveId, documentFactory);
    for (String participant : update.getSnapshot().getParticipantIdList()) {
      waveletData.addParticipant(new ParticipantId(participant));
    }
    for (int i = 0; i < update.getSnapshot().getDocumentCount(); i++) {
      DocumentSnapshot docSnapshot = update.getSnapshot().getDocument(i);
      BufferedDocOp docOp =
          WaveletOperationSerializer.deserialize(docSnapshot.getDocumentOperation());
      String docId = docSnapshot.getDocumentId();
      waveletData.createDocument(docId, DocOpUtil.asInitialization(docOp));
    }
    return waveletData;
  }

  public void publishChannelId(WaveletName waveletName, String channelId) {
    final List<OpenCallback> listeners = lookupListenersForWavelet(waveletName);
    for (OpenCallback listener : listeners) {
      listener.onUpdate(new WebClientWaveViewUpdate().setChannelId(channelId));
    }
  }
}

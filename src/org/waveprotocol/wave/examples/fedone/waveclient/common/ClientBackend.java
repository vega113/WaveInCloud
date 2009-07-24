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

package org.waveprotocol.wave.examples.fedone.waveclient.common;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;

import org.waveprotocol.wave.examples.fedone.common.HashedVersion;
import org.waveprotocol.wave.examples.fedone.common.HashedVersionFactory;
import org.waveprotocol.wave.examples.fedone.common.WaveletOperationSerializer;
import org.waveprotocol.wave.examples.fedone.model.util.HashedVersionZeroFactoryImpl;
import org.waveprotocol.wave.examples.fedone.rpc.ClientRpcChannel;
import org.waveprotocol.wave.examples.fedone.util.Log;
import org.waveprotocol.wave.examples.fedone.util.URLEncoderDecoderBasedPercentEncoderDecoder;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveClientRpc.ProtocolOpenRequest;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveClientRpc.ProtocolSubmitRequest;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveClientRpc.ProtocolSubmitResponse;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveClientRpc.ProtocolWaveClientRpc;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveClientRpc.ProtocolWaveletUpdate;
import org.waveprotocol.wave.model.id.IdConstants;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.id.URIEncoderDecoder.EncodingException;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.wave.AddParticipant;
import org.waveprotocol.wave.model.operation.wave.NoOp;
import org.waveprotocol.wave.model.operation.wave.RemoveParticipant;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletDocumentOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.util.Pair;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.WaveViewData;
import org.waveprotocol.wave.model.wave.data.WaveletData;
import org.waveprotocol.wave.model.wave.data.impl.WaveViewDataImpl;
import org.waveprotocol.wave.protocol.common.ProtocolWaveletDelta;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

/**
 * The "backend" of a basic wave client, designed for user interfaces to interact with.
 *
 *
 */
public class ClientBackend {
  /**
   * A client's view of a wave, with the current wavelet versions.
   */
  private final class ClientWaveView {
    private final WaveViewData data;
    private final Map<WaveletId, HashedVersion> currentVersions;

    ClientWaveView(WaveViewData data) {
      this.data = data;
      this.currentVersions = Maps.newHashMap();
    }

    WaveViewData getData() {
      return data;
    }

    /**
     * Gets the last known version for a wavelet, which is zero if
     * the wavelet is unknown.
     */
    HashedVersion getWaveletVersion(WaveletName name) {
      HashedVersion version = currentVersions.get(name.waveletId);
      if (version == null) {
        version = hashedVersionFactory.createVersionZero(name);
        currentVersions.put(name.waveletId, version);
      }
      return version;
    }

    /**
     * Sets the last known version for a wavelet.
     */
    void setWaveletVersion(WaveletId waveletId, HashedVersion version) {
      currentVersions.put(waveletId, version);
    }

    /**
     * Removes a wavelet and its current hashed version from the wave view.
     */
    void removeWavelet(WaveletId waveletId) {
      data.removeWavelet(waveletId);
      currentVersions.remove(waveletId);
    }
  }

  private static final Log LOG = Log.get(ClientBackend.class);

  /** User id of the user of the backend (encapsulating both user and server). */
  private final ParticipantId userId;

  /** Waves this backend is aware of. */
  private final Map<WaveId, ClientWaveView> waves = Maps.newHashMap();

  /** RPC controllers for the open wave connections. */
  private final Map<WaveId, RpcController> waveControllers = Maps.newHashMap();

  /** Listeners waiting on wave updates. */
  private final List<WaveletOperationListener> waveletOperationListeners = Lists.newArrayList();

  /** Id generator used for this (server, user) pair. */
  private final IdGenerator idGenerator;

  /** Id URI encoder and decoder. */
  private final IdURIEncoderDecoder uriCodec;

  /** RPC stub for communicating with server. */
  private final ProtocolWaveClientRpc.Stub rpcServer;

  /** RPC channel for communicating with server. */
  private final ClientRpcChannel rpcChannel;

  /** Factory for hashed wavelet versions. */
  private final HashedVersionFactory hashedVersionFactory;

  /**
   * Create new client backend tied permanently to a given server and user, open that client's
   * index, and begin managing waves it has access to.
   *
   * @param userAtDomain the user and their domain (for example, foo@bar.org)
   * @param server the server to connect to (for example, acmewave.com)
   * @param port port to connect to server with
   */
  public ClientBackend(String userAtDomain, String server, int port) throws IOException {
    if (userAtDomain.split("@").length != 2) {
      throw new IllegalArgumentException("userAtName must be in form user@domain");
    }

    this.userId = new ParticipantId(userAtDomain);
    this.idGenerator = new RandomIdGenerator(userId.getDomain());
    this.uriCodec = new IdURIEncoderDecoder(new URLEncoderDecoderBasedPercentEncoderDecoder());
    this.rpcChannel = new ClientRpcChannel(new InetSocketAddress(server, port));
    this.rpcServer = ProtocolWaveClientRpc.newStub(rpcChannel);

    // TODO: guicify ?
    this.hashedVersionFactory = new HashedVersionZeroFactoryImpl();

    // Opening the index wave will kickstart the process of receiving waves
    WaveId indexWaveId = IndexUtils.createIndexWaveId();
    List<String> waveletIdPrefixes = ImmutableList.of("");
    openWave(indexWaveId, waveletIdPrefixes);
  }

  /**
   * Gracefully shut down the backend, closing all connections to the server and clearing the
   * collection of waves (since they will no longer be valid).  This renders the backend relatively
   * useless since no more updates from the index wave will be received, but it is still valid.
   */
  public void shutdown() {
    for (RpcController rpcController : waveControllers.values()) {
      rpcController.startCancel();
    }

    waves.clear();
    waveControllers.clear();
  }

  /**
   * Open a wave.  This method will return immediately and updates will be delivered internally
   * from the RPC interface, and externally to {@link WaveletOperationListener}s.
   *
   * @param waveId of wave to open
   * @param waveletIdPrefix filter such that the server will send wavelet updates for ids that
   * match any of these prefixes
   */
  private void openWave(WaveId waveId, List<String> waveletIdPrefix) {
    if (waveControllers.containsKey(waveId)) {
      throw new IllegalArgumentException(waveId + " is already open");
    } else {
      // May already be there if created with createNewWave
      if (!waves.containsKey(waveId)) {
        createWave(waveId);
      }
    }

    ProtocolOpenRequest.Builder openRequest = ProtocolOpenRequest.newBuilder();

    openRequest.setParticipantId(getUserId().getAddress());
    openRequest.setWaveId(waveId.serialise());
    openRequest.addAllWaveletIdPrefix(waveletIdPrefix);

    final RpcController rpcController = rpcChannel.newRpcController();
    waveControllers.put(waveId, rpcController);

    rpcServer.open(
        rpcController,
        openRequest.build(),
        new RpcCallback<ProtocolWaveletUpdate>() {
          @Override public void run(ProtocolWaveletUpdate update) {
            if (update == null) {
              LOG.warning("RPC failed: " + rpcController.errorText());
            } else {
              receiveWaveletUpdate(update);
            }
          }
        }
    );
  }

  /**
   * Create a new wave and tell the server about it.
   *
   * @return the {@link WaveViewData} created
   */
  public WaveViewData createNewWave() {
    return createNewWave(getIdGenerator().newWaveId());
  }

  /**
   * Create a new wave with a given id and tell the server about it (by adding ourselves as a
   * participant to the conversation root).
   *
   * @param newWaveId the id to give the new wave
   * @return the {@link WaveViewData} created
   */
  private WaveViewData createNewWave(WaveId newWaveId) {
    WaveViewData newWaveView = createWave(newWaveId);
    WaveletData newWavelet = newWaveView.createWavelet(
        getIdGenerator().newConversationRootWaveletId());

    sendWaveletOperation(newWavelet, new AddParticipant(getUserId()));
    return newWaveView;
  }

  /**
   * @param id of wave to get
   * @return wave with the given id, or null if not found
   */
  public WaveViewData getWave(WaveId id) {
    // TODO: the data should not be exposed directly lest it get out
    // of sync with the operations, wavelet versions etc.
    ClientWaveView view = waves.get(id);
    return (view != null) ? view.getData() : null;
  }

  /**
   * @return the special wave containing the index data
   */
  public WaveViewData getIndexWave() {
    return getWave(IndexUtils.createIndexWaveId());
  }

  /**
   * Send a single wavelet operation over the wire.
   *
   * This is a convenience wrapper for sendWaveletDelta which creates a delta from the
   * single operation.
   *
   * @param wavelet to apply the operation to
   * @param op to send
   */
  public void sendWaveletOperation(WaveletData wavelet, WaveletOperation op) {
    sendWaveletDelta(wavelet, new WaveletDelta(getUserId(), ImmutableList.of(op)));
  }

  /**
   * Send a wavelet delta over the wire.
   *
   * @param wavelet that the delta applies to
   * @param delta to send
   */
  public void sendWaveletDelta(WaveletData wavelet, WaveletDelta delta) {
    // Build the submit request
    ProtocolSubmitRequest.Builder submitRequest = ProtocolSubmitRequest.newBuilder();

    try {
      submitRequest.setWaveletName(uriCodec.waveletNameToURI(wavelet.getWaveletName()));
    } catch (EncodingException e) {
      throw new IllegalArgumentException(e);
    }

    ClientWaveView clientView = waves.get(wavelet.getWaveletName().waveId);
    submitRequest.setDelta(WaveletOperationSerializer.serialize(delta,
        clientView.getWaveletVersion(wavelet.getWaveletName())));
    final RpcController rpcController = rpcChannel.newRpcController();

    rpcServer.submit(
        rpcController,
        submitRequest.build(),
        new RpcCallback<ProtocolSubmitResponse>() {
          @Override public void run(ProtocolSubmitResponse response) {
            if (response == null) {
              LOG.severe("RPC submit error: " + rpcController.errorText());
            } else if (response.hasErrorMessage()) {
              throw new IllegalStateException(response.getErrorMessage());
            }
          }
        }
    );
  }

  /**
   * Receive a protocol wavelet update from the wave server.
   *
   * @param waveletUpdate the wavelet update
   */
  public void receiveWaveletUpdate(ProtocolWaveletUpdate waveletUpdate) {
    // Extract relevant fields
    List<ProtocolWaveletDelta> protobufDeltas = waveletUpdate.getAppliedDeltaList();

    WaveletName waveletName;
    try {
      waveletName = uriCodec.uriToWaveletName(waveletUpdate.getWaveletName());
    } catch (EncodingException e) {
      throw new IllegalArgumentException(e);
    }

    ClientWaveView clientView = waves.get(waveletName.waveId);

    if (clientView == null) {
      // The wave view should always be present, since openWave adds them immediately
      throw new AssertionError("Received update on absent waveId " + waveletName.waveId);
    }

    WaveViewData wave = clientView.getData();
    WaveletData wavelet = wave.getWavelet(waveletName.waveletId);

    if (wavelet == null) {
      wavelet = wave.createWavelet(waveletName.waveletId);
    }

    // Apply operations to the wavelet
    for (ProtocolWaveletDelta protobufDelta : protobufDeltas) {
      Pair<WaveletDelta, HashedVersion> deltaAndVersion =
        WaveletOperationSerializer.deserialize(protobufDelta);

      for (WaveletOperation op : deltaAndVersion.first.getOperations()) {
        try {
          op.apply(wavelet);
          notifyWaveletOperationListeners(wavelet, op);
        } catch (OperationException e) {
          LOG.warning("OperationException when applying " + op + " to " + wavelet);
        }

        // If this is a remove participant on ourselves we need to remove the wavelet locally,
        // important for the way the client is handling the index wave.
        // TODO: this is wrong for anything other than the index wave, and dubious even then
        if (op instanceof RemoveParticipant &&
            ((RemoveParticipant) op).getParticipantId().equals(getUserId())) {
          wave.removeWavelet(waveletName.waveletId);
        }
      }

      // Update wavelet version
      clientView.setWaveletVersion(waveletName.waveletId,
          WaveletOperationSerializer.deserialize(waveletUpdate.getResultingVersion()));
    }

    notifyWaveletOperationListeners(wavelet, new NoOp());

    // If it was an update to the index wave, might need to open/close some more waves
    if (IndexUtils.isIndexWave(wave)) {
      syncWithIndexWave(wave);
    }
  }

  /**
   * Creates a new, empty wave view and stores it in {@code waves}.
   */
  private WaveViewData createWave(WaveId waveId) {
    WaveViewData newWave = new WaveViewDataImpl(waveId);
    ClientWaveView clientView = new ClientWaveView(newWave);
    waves.put(waveId, clientView);
    return clientView.getData();
  }

  /**
   * Synchronise the state with the index wave by opening any waves that are referred to but not
   * present, and closing any waves that are present but not referred to.
   *
   * @param indexWave the index wave to synchronise with
   */
  private void syncWithIndexWave(WaveViewData indexWave) {
    List<WaveId> indexEntries = IndexUtils.getIndexEntries(indexWave);

    // This is somewhat inefficient, it would be better to only look at the changes to the index
    // wave since last time this was run (that is, be less lazy in receiveWaveletUpdate)
    for (WaveId indexEntry : indexEntries) {
      if (!waveControllers.containsKey(indexEntry)) {
        // TODO: Subscribe only to the "conv+root" wavelet, but it must also be
        // qualified with the wavelet domain.
        openWave(indexEntry, ImmutableList.of(""));
      }
    }

//    for (WaveId waveEntry : waves.keySet()) {
//      if (!IndexUtils.isIndexWaveId(waveEntry) && !indexEntries.contains(waveEntry)) {
//        waves.remove(waveEntry);
//      }
//    }
  }

  /**
   * Notify all wavelet operation listeners of a wavelet operation.
   *
   * @param wavelet operated on
   * @param op the operation
   */
  private void notifyWaveletOperationListeners(WaveletData wavelet, WaveletOperation op) {
    for (WaveletOperationListener listener : waveletOperationListeners) {
      if (op instanceof WaveletDocumentOperation) {
        listener.waveletDocumentUpdated(wavelet, ((WaveletDocumentOperation) op).getDocumentId());
      } else if (op instanceof AddParticipant) {
        listener.participantAdded(wavelet, ((AddParticipant) op).getParticipantId());
      } else if (op instanceof RemoveParticipant) {
        listener.participantRemoved(wavelet, ((RemoveParticipant) op).getParticipantId());
      } else if (op instanceof NoOp) {
        listener.noOp(wavelet);
      }
    }
  }

  /**
   * Ensure that a wave has a conversation root, and create it if missing (locally).
   *
   * @param wave to check
   */
  public void ensureConversationRoot(WaveViewData wave) {
    WaveletData convRoot = getConversationRoot(wave);

    if (convRoot == null) {
      wave.createWavelet(getConversationRootId(wave));
    }
  }

  /**
   * Get the conversation root wavelet of a wave.
   *
   * @param wave to get conversation root of
   * @return conversation root wavelet of the wave
   */
  public WaveletData getConversationRoot(WaveViewData wave) {
    return wave.getWavelet(getConversationRootId(wave));
  }

  /**
   * @return the conversation root wavelet id of a wave.
   */
  private WaveletId getConversationRootId(WaveViewData wave) {
    return new WaveletId(wave.getWaveId().getDomain(), IdConstants.CONVERSATION_ROOT_WAVELET);
  }

  /**
   * @return the {@link ParticipantId} id of the user
   */
  public ParticipantId getUserId() {
    return userId;
  }

  /**
   * @return the id generator which generates wave and wavelet ids
   */
  public IdGenerator getIdGenerator() {
    return idGenerator;
  }

  /**
   * Add a {@link WaveletOperationListener} to be notified whenever a wave is updated.
   *
   * @param listener new listener
   */
  public void addWaveletOperationListener(WaveletOperationListener listener) {
    waveletOperationListeners.add(listener);
  }

  /**
   * @param listener listener to be removed
   */
  public void removeWaveletOperationListener(WaveletOperationListener listener) {
    waveletOperationListeners.remove(listener);
  }
}

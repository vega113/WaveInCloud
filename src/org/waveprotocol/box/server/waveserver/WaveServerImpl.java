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

package org.waveprotocol.box.server.waveserver;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import org.waveprotocol.box.common.DeltaSequence;
import org.waveprotocol.box.server.common.CoreWaveletOperationSerializer;
import org.waveprotocol.box.server.frontend.WaveletSnapshotAndVersion;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.box.server.util.WaveletDataUtil;
import org.waveprotocol.wave.crypto.SignatureException;
import org.waveprotocol.wave.crypto.SignerInfo;
import org.waveprotocol.wave.crypto.UnknownSignerException;
import org.waveprotocol.wave.federation.FederationErrors;
import org.waveprotocol.wave.federation.FederationException;
import org.waveprotocol.wave.federation.FederationHostBridge;
import org.waveprotocol.wave.federation.FederationRemoteBridge;
import org.waveprotocol.wave.federation.SubmitResultListener;
import org.waveprotocol.wave.federation.WaveletFederationListener;
import org.waveprotocol.wave.federation.WaveletFederationProvider;
import org.waveprotocol.wave.federation.FederationErrorProto.FederationError;
import org.waveprotocol.wave.federation.Proto.ProtocolAppliedWaveletDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion;
import org.waveprotocol.wave.federation.Proto.ProtocolSignature;
import org.waveprotocol.wave.federation.Proto.ProtocolSignedDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolSignerInfo;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;
import org.waveprotocol.wave.model.wave.data.WaveViewData;
import org.waveprotocol.wave.model.wave.data.impl.WaveViewDataImpl;
import org.waveprotocol.wave.util.logging.Log;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The main class that services the FederationHost, FederationRemote and ClientFrontend.
 */
@Singleton
public class WaveServerImpl implements WaveBus, WaveletProvider,
    WaveletFederationProvider, WaveletFederationListener.Factory, SearchProvider {

  private static final Log LOG = Log.get(WaveServerImpl.class);

  protected static final long HISTORY_REQUEST_LENGTH_LIMIT_BYTES = 1024 * 1024;

  /** Picks out the transformed deltas from a list of delta records. */
  private static ImmutableList<TransformedWaveletDelta> transformedDeltasOf(
      Iterable<WaveletDeltaRecord> deltaRecords) {
    ImmutableList.Builder<TransformedWaveletDelta> transformedDeltas = ImmutableList.builder();
    for (WaveletDeltaRecord deltaRecord : deltaRecords) {
      transformedDeltas.add(deltaRecord.getTransformedDelta());
    }
    return transformedDeltas.build();
  }

  /** Picks out the byte strings of the applied deltas from a list of delta records. */
  private static ImmutableList<ByteString> serializedAppliedDeltasOf(
      Iterable<WaveletDeltaRecord> deltaRecords) {
    ImmutableList.Builder<ByteString> serializedAppliedDeltas = ImmutableList.builder();
    for (WaveletDeltaRecord deltaRecord : deltaRecords) {
      serializedAppliedDeltas.add(deltaRecord.getAppliedDelta().getByteString());
    }
    return serializedAppliedDeltas.build();
  }

  private final Executor listenerExecutor;
  private final CertificateManager certificateManager;
  private final WaveletFederationListener.Factory federationHostFactory;
  private final RemoteWaveletContainer.Factory remoteWaveletContainerFactory;
  private final LocalWaveletContainer.Factory localWaveletContainerFactory;
  private final WaveletFederationProvider federationRemote;
  private final WaveBusDispatcher dispatcher = new WaveBusDispatcher();

  /** Wavelet states */
  private final Map<WaveId, Map<WaveletId, WaveletContainer>> waveMap =
    new MapMaker().makeComputingMap(
        new Function<WaveId, Map<WaveletId, WaveletContainer>>() {
          @Override
          public Map<WaveletId, WaveletContainer> apply(WaveId from) {
            return Maps.newHashMap();
          }
        });

  /** List of federation hosts for which we have listeners */
  private final Map<String, WaveletFederationListener> federationHosts =
      // Add a new entry to the map on demand.
      new MapMaker().makeComputingMap(
        new Function<String, WaveletFederationListener>() {
          @Override
          public WaveletFederationListener apply(String domain) {
            return federationHostFactory.listenerForDomain(domain);
          }
        }
      );

  private final WaveletNotificationSubscriber notifiee = new WaveletNotificationSubscriber() {
    @Override
    public void waveletUpdate(ReadableWaveletData wavelet, ImmutableList<WaveletDeltaRecord> deltas,
        ImmutableSet<String> domainsToNotify) {
      dispatcher.waveletUpdate(wavelet, DeltaSequence.of(transformedDeltasOf(deltas)));
      Set<String> remoteDomainsToNotify = Sets.difference(domainsToNotify, getLocalDomains());
      if (!remoteDomainsToNotify.isEmpty()) {
        ImmutableList<ByteString> serializedAppliedDeltas = serializedAppliedDeltasOf(deltas);
        for (String domain : remoteDomainsToNotify) {
          federationHosts.get(domain).waveletDeltaUpdate(WaveletDataUtil.waveletNameOf(wavelet),
              serializedAppliedDeltas, federationCallback("delta update"));
        }
      }        
    }

    @Override
    public void waveletCommitted(WaveletName waveletName, HashedVersion version,
        ImmutableSet<String> domainsToNotify) {
      dispatcher.waveletCommitted(waveletName, version);
      Set<String> remoteDomainsToNotify = Sets.difference(domainsToNotify, getLocalDomains());
      if (!remoteDomainsToNotify.isEmpty()) {
        ProtocolHashedVersion serializedVersion = CoreWaveletOperationSerializer.serialize(version);
        for (String domain : remoteDomainsToNotify) {
          federationHosts.get(domain).waveletCommitUpdate(
              waveletName, serializedVersion, federationCallback("commit notice"));
        }
      }
    }

    private WaveletFederationListener.WaveletUpdateCallback federationCallback(
        final String description) {
      return new WaveletFederationListener.WaveletUpdateCallback() {
        @Override
        public void onSuccess() {
          LOG.info(description + " success");
        }

        @Override
        public void onFailure(FederationError error) {
          LOG.warning(description + " failure: " + error);
        }
      };
    }
  };

  //
  // WaveletFederationListener.Factory implementation.
  //

  /**
   * Listener for notifications coming from the Federation Remote. For now we accept updates
   * for wavelets on any domain.
   */
  @Override
  public WaveletFederationListener listenerForDomain(final String domain) {
    return new WaveletFederationListener() {
      @Override
      public void waveletDeltaUpdate(final WaveletName waveletName,
          List<ByteString> deltas, final WaveletUpdateCallback callback) {
        Preconditions.checkArgument(!deltas.isEmpty());

        if (isLocalWavelet(waveletName)) {
          LOG.warning("Remote tried to update local wavelet " + waveletName);
          callback.onFailure(FederationErrors.badRequest("Received update to local wavelet"));
          return;
        }

        // Update wavelet container with the applied deltas
        final RemoteWaveletContainer remoteWavelet;
        try {
          remoteWavelet = getOrCreateRemoteWavelet(waveletName);
        } catch (PersistenceException e) {
          // We swallow e.getMessage() to avoid leaking information.
          callback.onFailure(FederationErrors.internalServerError("Storage access failure"));
          return;
        }

        // Update this remote wavelet with the immediately incoming delta,
        // providing a callback so that incoming historic deltas (as well as
        // this delta) can be provided to the wave bus.
        final ListenableFuture<Void> result =
            remoteWavelet.update(deltas, domain, federationRemote, certificateManager);
        result.addListener(
            new Runnable() {
              @Override
              public void run() {
                FederationError error;
                try {
                  result.get();
                  callback.onSuccess();
                  return;
                } catch (InterruptedException e) {
                  LOG.severe("Interrupted updating " + waveletName, e);
                  error = FederationErrors.internalServerError("Interrupted");
                } catch (ExecutionException e) {
                  LOG.warning("Failed updating " + waveletName, e);
                  Throwable cause = e.getCause();
                  error = (cause instanceof FederationException)
                      ? ((FederationException) cause).getError()
                      : FederationErrors.internalServerError("Unknown error");
                }
                callback.onFailure(error);
              }
            },
            listenerExecutor);
      }

      @Override
      public void waveletCommitUpdate(WaveletName waveletName,
          ProtocolHashedVersion committedVersion, WaveletUpdateCallback callback) {
        Preconditions.checkNotNull(committedVersion);
        WaveletContainer wavelet = getWavelet(waveletName);
        if (wavelet instanceof RemoteWaveletContainer) {
          ((RemoteWaveletContainer) wavelet).commit(
              CoreWaveletOperationSerializer.deserialize(committedVersion));
        } else if (wavelet == null) {
          LOG.info("Got commit update for missing wavelet " + waveletName);
        } else {
          LOG.severe("Got commit update for local wavelet " + waveletName);
        }
        callback.onSuccess(); // TODO(soren): only call success if successful?
      }
    };
  }

  //
  // WaveletFederationProvider implementation.
  //

  @Override
  public void submitRequest(WaveletName waveletName, ProtocolSignedDelta signedDelta,
      SubmitResultListener listener) {
    if (!isLocalWavelet(waveletName)) {
      LOG.warning("Remote tried to submit to non-local wavelet " + waveletName);
      listener.onFailure(FederationErrors.badRequest("Non-local wavelet update"));
      return;
    }

    ProtocolWaveletDelta delta;
    try {
      delta = ByteStringMessage.parseProtocolWaveletDelta(signedDelta.getDelta()).getMessage();
    } catch (InvalidProtocolBufferException e) {
      LOG.warning("Submit request: Invalid delta protobuf. WaveletName: " + waveletName, e);
      listener.onFailure(FederationErrors.badRequest("Signed delta contains invalid delta"));
      return;
    }

    // Disallow creation of wavelets by remote users.
    if (delta.getHashedVersion().getVersion() == 0) {
      LOG.warning("Remote user tried to submit delta at version 0 - disallowed. " + signedDelta);
      listener.onFailure(FederationErrors.badRequest("Remote users may not create wavelets."));
      return;
    }

    try {
      certificateManager.verifyDelta(signedDelta);
      submitDelta(waveletName, delta, signedDelta, listener);
    } catch (SignatureException e) {
      LOG.warning("Submit request: Delta failed verification. WaveletName: " + waveletName +
          " delta: " + signedDelta, e);
      listener.onFailure(FederationErrors.badRequest("Remote verification failed"));
    } catch (UnknownSignerException e) {
      LOG.warning("Submit request: unknown signer.  WaveletName: " + waveletName +
          "delta: " + signedDelta, e);
      listener.onFailure(FederationErrors.badRequest("Unknown signer"));
    }
  }

  @Override
  public void requestHistory(WaveletName waveletName, String domain,
      ProtocolHashedVersion startVersion, ProtocolHashedVersion endVersion,
      long lengthLimit, HistoryResponseListener listener) {
    WaveletContainer wc = getWavelet(waveletName);
    if (wc == null) {
      listener.onFailure(FederationErrors.badRequest("Wavelet " + waveletName
          + " does not exist."));
      LOG.info("Request history: " + domain + " requested non-existent wavelet: " + waveletName);
    } else {
      // TODO: once we support federated groups, expand support to request
      // remote wavelets too.
      if (!isLocalWavelet(waveletName)) {
        listener.onFailure(FederationErrors.badRequest(
            "Wavelet " + waveletName + " not hosted here."));
        LOG.info("Federation remote for domain: " + domain + " requested a remote wavelet: " +
            waveletName);
      } else {
        try {
          Collection<ByteStringMessage<ProtocolAppliedWaveletDelta>> deltaHistory =
              wc.requestHistory(CoreWaveletOperationSerializer.deserialize(startVersion),
                  CoreWaveletOperationSerializer.deserialize(endVersion));
          List<ByteString> deltaHistoryBytes = Lists.newArrayList();
          for (ByteStringMessage<ProtocolAppliedWaveletDelta> d : deltaHistory) {
            deltaHistoryBytes.add(d.getByteString());
          }

          // Now determine whether we received the entire requested wavelet history.
          LOG.info("Found deltaHistory between " + startVersion + " - " + endVersion
              + ", returning to requester domain " + domain + " -- " + deltaHistory);
          listener.onSuccess(deltaHistoryBytes, endVersion, endVersion.getVersion());
          // TODO: ### check length limit ??
//           else {
//            ProtocolAppliedWaveletDelta lastDelta = deltaHistory.last();
//            long lastVersion = lastDelta.getHashedVersionAppliedAt().getVersion() +
//                lastDelta.getOperationsApplied();
//            listener.onSuccess(deltaHistory, lcv.getVersion(),
//                lastVersion);
//          }
        } catch (WaveServerException e) {
          LOG.severe("Error retrieving wavelet history: " + waveletName + " " + startVersion +
              " - " + endVersion);
          // TODO(soren): choose a better error code (depending on e)
          listener.onFailure(FederationErrors.badRequest(
              "Server error while retrieving wavelet history."));
        }
      }
    }
  }

  @Override
  public void getDeltaSignerInfo(ByteString signerId,
      WaveletName waveletName, ProtocolHashedVersion deltaEndVersion,
      DeltaSignerInfoResponseListener listener) {
    WaveletContainer wavelet = getWavelet(waveletName);
    HashedVersion endVersion = CoreWaveletOperationSerializer.deserialize(deltaEndVersion);

    if (wavelet == null) {
      LOG.info("getDeltaSignerInfo for nonexistent wavelet " + waveletName);
      listener.onFailure(FederationErrors.badRequest("Wavelet does not exist"));
    } else if (!(wavelet instanceof LocalWaveletContainer)) {
      LOG.info("getDeltaSignerInfo for remote wavelet " + waveletName);
      listener.onFailure(FederationErrors.badRequest("Wavelet is not locally hosted"));
    } else {
      LocalWaveletContainer localWavelet = (LocalWaveletContainer) wavelet;
      if (localWavelet.isDeltaSigner(endVersion, signerId)) {
        ProtocolSignerInfo signerInfo = certificateManager.retrieveSignerInfo(signerId);
        if (signerInfo == null) {
          // Oh no!  We are supposed to store it, and we already know they did sign this delta.
          LOG.severe("No stored signer info for valid getDeltaSignerInfo on " + waveletName);
          listener.onFailure(FederationErrors.badRequest("Unknown signer info"));
        } else {
          listener.onSuccess(signerInfo);
        }
      } else {
        LOG.info("getDeltaSignerInfo was not authrorised for wavelet " + waveletName
            + ", end version " + deltaEndVersion);
        listener.onFailure(FederationErrors.badRequest("Not authorised to get signer info"));
      }
    }
  }

  @Override
  public void postSignerInfo(String destinationDomain, ProtocolSignerInfo signerInfo,
      PostSignerInfoResponseListener listener) {
    try {
      certificateManager.storeSignerInfo(signerInfo);
    } catch (SignatureException e) {
      String error = "verification failure from domain " + signerInfo.getDomain();
      LOG.warning("incoming postSignerInfo: " + error, e);
      listener.onFailure(FederationErrors.badRequest(error));
      return;
    }
    listener.onSuccess();
  }

  //
  // WaveletProvider implementation.
  //

  @Override
  public Collection<TransformedWaveletDelta> getHistory(WaveletName waveletName,
      HashedVersion startVersion, HashedVersion endVersion)
      throws AccessControlException, WaveletStateException {
    WaveletContainer wc = getWavelet(waveletName);
    if (wc == null) {
      throw new AccessControlException(
          "Client request for history made for non-existent wavelet: " + waveletName);
    }
    return wc.requestTransformedHistory(startVersion, endVersion);
  }

  @Override
  public WaveletSnapshotAndVersion getSnapshot(WaveletName waveletName) {
    WaveletContainer wc = getWavelet(waveletName);
    if (wc == null) {
      LOG.info("client requested snapshot for non-existent wavelet: " + waveletName);
      return null;
    } else {
      return wc.getSnapshot();
    }
  }

  @Override
  public void submitRequest(WaveletName waveletName, ProtocolWaveletDelta delta,
      final SubmitRequestListener listener) {
    if (delta.getOperationCount() == 0) {
      listener.onFailure("Empty delta at version " + delta.getHashedVersion().getVersion());
      return;
    }

    // The serialised version of this delta happens now.  This should be the only place, ever!
    ProtocolSignedDelta signedDelta =
        certificateManager.signDelta(ByteStringMessage.serializeMessage(delta));

    submitDelta(waveletName, delta, signedDelta, new SubmitResultListener() {
      @Override
      public void onFailure(FederationError errorMessage) {
        listener.onFailure(errorMessage.getErrorMessage());
      }

      @Override
      public void onSuccess(int operationsApplied,
          ProtocolHashedVersion hashedVersionAfterApplication, long applicationTimestamp) {
        listener.onSuccess(operationsApplied,
            CoreWaveletOperationSerializer.deserialize(hashedVersionAfterApplication),
            applicationTimestamp);
      }
    });
  }

  @Override
  public boolean checkAccessPermission(WaveletName waveletName, ParticipantId participantId)
      throws WaveletStateException {
    WaveletContainer wc = getWavelet(waveletName);
    return wc != null && wc.checkAccessPermission(participantId);
  }

  //
  // WaveBus implementation.
  //

  @Override
  public void subscribe(Subscriber s) {
    dispatcher.subscribe(s);
  }

  @Override
  public void unsubscribe(Subscriber s) {
    dispatcher.unsubscribe(s);
  }

  //
  // Constructor and privates.
  //

  /**
   * Constructor.
   *
   * @param listenerExecutor executes callback listeners
   * @param certificateManager provider of certificates; it also determines which
   *        domains this wave server regards as local wavelets.
   * @param federationHostFactory factory that returns federation host instance listening
   *        on a given domain.
   * @param federationRemote federation remote interface
   * @param localWaveletContainerFactory factory for local WaveletContainers
   * @param remoteWaveletContainerFactory factory for remote WaveletContainers
   */
  @Inject
  public WaveServerImpl(
      @Named("listener_executor") Executor listenerExecutor,
      CertificateManager certificateManager,
      @FederationHostBridge WaveletFederationListener.Factory federationHostFactory,
      @FederationRemoteBridge WaveletFederationProvider federationRemote,
      LocalWaveletContainer.Factory localWaveletContainerFactory,
      RemoteWaveletContainer.Factory remoteWaveletContainerFactory) {
    this.listenerExecutor = listenerExecutor;
    this.certificateManager = certificateManager;
    this.federationHostFactory = federationHostFactory;
    this.federationRemote = federationRemote;

    this.localWaveletContainerFactory = localWaveletContainerFactory;
    this.remoteWaveletContainerFactory = remoteWaveletContainerFactory;

    LOG.info("Wave Server configured to host local domains: "
        + certificateManager.getLocalDomains());

    // Preemptively add our own signer info to the certificate manager
    SignerInfo signerInfo = certificateManager.getLocalSigner().getSignerInfo();
    if (signerInfo != null) {
      try {
        certificateManager.storeSignerInfo(signerInfo.toProtoBuf());
      } catch (SignatureException e) {
        LOG.severe("Failed to add our own signer info to the certificate store", e);
      }
    }
  }

  private boolean isLocalWavelet(WaveletName waveletName) {
    boolean isLocal = getLocalDomains().contains(waveletName.waveletId.getDomain());
    LOG.fine("" + waveletName + " is " + (isLocal? "" : "not") + " local");
    return isLocal;
  }

  private Set<String> getLocalDomains() {
    return certificateManager.getLocalDomains();
  }

  /**
   * Returns a container for a remote wavelet. If it doesn't exist, it will be created.
   * This method is only called in response to a Federation Remote doing an update
   * or commit on this wavelet.
   *
   * @param waveletName name of wavelet
   * @return an existing or new instance.
   * @throws IllegalArgumentException if the name refers to a local wavelet.
   * @throws PersistenceException if persistence fails.
   */
  private RemoteWaveletContainer getOrCreateRemoteWavelet(WaveletName waveletName)
      throws PersistenceException {
    Preconditions.checkArgument(!isLocalWavelet(waveletName), "%s is local", waveletName);
    synchronized (waveMap) {
      Map<WaveletId, WaveletContainer> wave = waveMap.get(waveletName.waveId);
      // This will blow up if we messed up and put a local wavelet in by mistake.
      RemoteWaveletContainer wc = (RemoteWaveletContainer) wave.get(waveletName.waveletId);
      if (wc == null) {
        wc = remoteWaveletContainerFactory.create(notifiee, waveletName);
        wave.put(waveletName.waveletId, wc);
      }
      return wc;
    }
  }

  /**
   * Returns a container for a local wavelet. If it doesn't exist, it will be created.
   * Local wavelets are retrieved or created by a federation host or client on behalf
   * of a participant. The participant must have permission to access the wavelet if
   * it already exists.
   *
   * @param waveletName name of wavelet
   * @param participantId on who's behalf this wavelet is to be accessed.
   * @return an existing or new instance.
   * @throws AccessControlException if the participant may not access the wavelet.
   * @throws PersistenceException if the persistence storage fails.
   * @throws IllegalArgumentException if the name refers to a remote wavelet.
   * @throws WaveletStateException if the wavelet is in a bad state.
   */
  private LocalWaveletContainer getOrCreateLocalWavelet(WaveletName waveletName,
      ParticipantId participantId)
      throws AccessControlException, PersistenceException, WaveletStateException {
    Preconditions.checkArgument(isLocalWavelet(waveletName), "%s is remote", waveletName);
    synchronized (waveMap) {
      Map<WaveletId, WaveletContainer> wave = waveMap.get(waveletName.waveId);
      // This will blow up if we messed up and put a remote wavelet in by mistake.
      LocalWaveletContainer wc = (LocalWaveletContainer) wave.get(waveletName.waveletId);
      if (wc == null) {
        wc = localWaveletContainerFactory.create(notifiee, waveletName);
        // TODO: HACK(Jochen): do we need a namespace policer here ??? ###
        // TODO(ljvderijk): Do we want to put in a wavelet wich has not been submitted yet?
        wave.put(waveletName.waveletId, wc);
      } else {
        if (!wc.checkAccessPermission(participantId)) {
          throw new AccessControlException(participantId + " is not a participant: " + waveletName);
        }
      }
      return wc;
    }
  }

  /**
   * Returns a generic wavelet container, when the caller doesn't need to validate whether
   * its a local or remote wavelet.
   *
   * @param waveletName name of wavelet.
   * @return an wavelet container or null if it doesn't exist.
   */
  private WaveletContainer getWavelet(WaveletName waveletName) {
    synchronized (waveMap) {
      Map<WaveletId, WaveletContainer> wave = waveMap.get(waveletName.waveId);
      return wave.get(waveletName.waveletId);
    }
  }

  /**
   * Callback interface for sending a list of certificates to a domain.
   */
  private interface PostSignerInfoCallback {
    public void done(int successCount);
  }

  /**
   * Submit the delta to local or remote wavelets, return results via listener.
   * Also broadcast updates to federationHosts and clientFrontend.
   *
   * @param waveletName the wavelet to apply the delta to
   * @param delta the {@link ProtocolWaveletDelta} inside {@code signedDelta}
   * @param signedDelta the signed delta
   * @param resultListener callback
   *
   * TODO: For now the WaveletFederationProvider will have to ensure this is a
   * local wavelet. Once we support federated groups, that test should be
   * removed.
   */
  private void submitDelta(final WaveletName waveletName, ProtocolWaveletDelta delta,
      final ProtocolSignedDelta signedDelta, final SubmitResultListener resultListener) {
    Preconditions.checkArgument(delta.getOperationCount() > 0, "empty delta");

    if (isLocalWavelet(waveletName)) {
      try {
        LOG.info("Submit to " + waveletName + " by " + delta.getAuthor() + " @ "
            + delta.getHashedVersion().getVersion() + " with " + delta.getOperationCount()
            + " ops");

        // TODO(arb): add v0 policer here.
        LocalWaveletContainer wc =
            getOrCreateLocalWavelet(waveletName, new ParticipantId(delta.getAuthor()));

        WaveletDeltaRecord submitResult = wc.submitRequest(waveletName, signedDelta);
        TransformedWaveletDelta transformedDelta = submitResult.getTransformedDelta();

        // Return result to caller.
        ProtocolHashedVersion resultVersionProto =
            CoreWaveletOperationSerializer.serialize(transformedDelta.getResultingVersion());
        LOG.info("Submit result for " + waveletName + " by "
            + transformedDelta.getAuthor() + " applied "
            + transformedDelta.size() + " ops at v: "
            + transformedDelta.getAppliedAtVersion() + " t: "
            + transformedDelta.getApplicationTimestamp());
        resultListener.onSuccess(transformedDelta.size(), resultVersionProto,
            transformedDelta.getApplicationTimestamp());
      } catch (OperationException e) {
        resultListener.onFailure(FederationErrors.badRequest(e.getMessage()));
        return;
      } catch (IllegalArgumentException e) {
        resultListener.onFailure(FederationErrors.badRequest(e.getMessage()));
        return;
      } catch (InvalidProtocolBufferException e) {
        resultListener.onFailure(FederationErrors.badRequest(e.getMessage()));
        return;
      } catch (PersistenceException e) {
        // TODO(soren): pick a better error response than BAD_REQUEST
        resultListener.onFailure(FederationErrors.badRequest(e.getMessage()));
        return;
      } catch (WaveServerException e) {
        resultListener.onFailure(FederationErrors.badRequest(e.getMessage()));
        return;
      }
    } else {
      // For remote wavelets post required signatures to the authorative server then send delta
      postAllSignerInfo(signedDelta.getSignatureList(), waveletName.waveletId.getDomain(),
          new PostSignerInfoCallback() {
            @Override public void done(int successCount) {
              LOG.info("Remote: successfully sent " + successCount + " of "
                  + signedDelta.getSignatureCount() + " certs to "
                  + waveletName.waveletId.getDomain());
              federationRemote.submitRequest(waveletName, signedDelta, resultListener);
            }
          });
    }
  }

  /**
   * Post a list of certificates to a domain and run a callback when all are finished.  The
   * callback will run whether or not all posts succeed.
   *
   * @param sigs list of signatures to post signer info for
   * @param domain to post signature to
   * @param callback to run when all signatures have been posted, successfully or unsuccessfully
   */
  private void postAllSignerInfo(final List<ProtocolSignature> sigs, final String domain,
      final PostSignerInfoCallback callback) {

    // In the current implementation there should only be a single signer
    if (sigs.size() != 1) {
      LOG.warning(sigs.size() + " signatures to broadcast, expecting exactly 1");
    }

    final AtomicInteger resultCount = new AtomicInteger(sigs.size());
    final AtomicInteger successCount = new AtomicInteger(0);

    for (final ProtocolSignature sig : sigs) {
      final ProtocolSignerInfo psi = certificateManager.retrieveSignerInfo(sig.getSignerId());

      if (psi == null) {
        LOG.warning("Couldn't find signer info for " + sig);
        if (resultCount.decrementAndGet() == 0) {
          LOG.info("Finished signature broadcast with " + successCount.get()
              + " successful, running callback");
          callback.done(successCount.get());
        }
      } else {
        federationRemote.postSignerInfo(domain, psi, new PostSignerInfoResponseListener() {
          @Override
          public void onFailure(FederationError error) {
            LOG.warning("Failed to post " + sig + " to " + domain + ": " + error);
            countDown();
          }

          @Override
          public void onSuccess() {
            LOG.info("Successfully broadcasted " + sig + " to " + domain);
            successCount.incrementAndGet();
            countDown();
          }

          private void countDown() {
            if (resultCount.decrementAndGet() == 0) {
              LOG.info("Finished signature broadcast with " + successCount.get()
                  + " successful, running callback");
              callback.done(successCount.get());
            }
          }
        });
      }
    }
  }

  @Override
  public Collection<WaveViewData> search(ParticipantId user, String query, int startAt,
      int numResults) {
    LOG.info("Search query '" + query + "' from user: " + user);

    if (!query.equals("in:inbox") && !query.equals("with:me")) {
      throw new AssertionError("Only queries for the inbox work");
    }

    Map<WaveId, WaveViewData> results = CollectionUtils.newHashMap();

    synchronized (waveMap) {
      int resultIndex = 0;
      for (Entry<WaveId, Map<WaveletId, WaveletContainer>> entry : waveMap.entrySet()) {
        WaveId waveId = entry.getKey();
        for (WaveletContainer c : entry.getValue().values()) {
          if (c.hasParticipant(user)) {
            if (resultIndex >= startAt && resultIndex < (startAt + numResults)) {
              WaveViewData wave = results.get(waveId);
              if (wave == null) {
                wave = WaveViewDataImpl.create(waveId);
                results.put(waveId, wave);
              }

              wave.addWavelet(c.copyWaveletData());
            }

            resultIndex++;
            if (resultIndex > startAt + numResults) {
              return results.values();
            }
          }
        }
      }
    }

    return results.values();
  }
}

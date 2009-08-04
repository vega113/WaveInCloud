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

package org.waveprotocol.wave.examples.fedone.waveserver;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;

import org.waveprotocol.wave.examples.fedone.common.HashedVersion;
import org.waveprotocol.wave.examples.fedone.common.WaveletOperationSerializer;
import org.waveprotocol.wave.examples.fedone.crypto.SignatureException;
import org.waveprotocol.wave.examples.fedone.federation.xmpp.XmppFederationRemote;
import org.waveprotocol.wave.examples.fedone.util.Log;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveletContainer.State;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveletFederationListener.Factory;
import org.waveprotocol.wave.model.document.operation.BufferedDocOp;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.protocol.common.ProtocolAppliedWaveletDelta;
import org.waveprotocol.wave.protocol.common.ProtocolHashedVersion;
import org.waveprotocol.wave.protocol.common.ProtocolSignedDelta;
import org.waveprotocol.wave.protocol.common.ProtocolSignerInfo;
import org.waveprotocol.wave.protocol.common.ProtocolWaveletDelta;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;

/**
 * The main class that services the FederationHost, FederationRemote and ClientFrontend.
 *
 *
 */
@Singleton
public class WaveServerImpl implements WaveServer {

  private static final Log LOG = Log.get(WaveServerImpl.class);

  protected static final long HISTORY_REQUEST_LENGTH_LIMIT_BYTES = 1024 * 1024;

  // -------------------------------------------------------------------------------------------
  // PRIVATES INITIALIZED IN CONSTRUCTOR.
  // -------------------------------------------------------------------------------------------

  private final CertificateManager certificateManager;
  private final Factory federationHostFactory;
  private final RemoteWaveletContainer.Factory remoteWaveletContainerFactory;
  private final LocalWaveletContainer.Factory localWaveletContainerFactory;
  private final WaveletFederationProvider federationRemote;
  private WaveletListener clientListener = null;

  // -------------------------------------------------------------------------------------------
  // MAPS FOR WAVES AND FEDERATION HOSTS.
  // -------------------------------------------------------------------------------------------

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

  // -------------------------------------------------------------------------------------------
  // IMPLEMENTATION OF THE WAVELET-FEDERATION-LISTENER.FACTORY INTERFACE USED BY THE FED HOST.
  // -------------------------------------------------------------------------------------------

  /**
   * Listener for notifications coming from the Federation Remote. For now we accept updates
   * for wavelets on any domain.
   */
  public WaveletFederationListener listenerForDomain(final String domain) {
    return new WaveletFederationListener() {

      @Override
      public void waveletUpdate(final WaveletName waveletName,
          List<ProtocolAppliedWaveletDelta> appliedDeltas,
          ProtocolHashedVersion committedHashedVersion,
          WaveletUpdateCallback callback) {
        for (ProtocolAppliedWaveletDelta d : appliedDeltas) {
          try {
            certificateManager.verifyDelta(d.getSignedOriginalDelta());
          } catch (SignatureException e) {
            String error = "verification failure, wavelet: " + waveletName + " delta: " + d;
            LOG.warning("incoming waveletUpdate: " + error, e);
            WaveletContainer wavelet = getWavelet(waveletName);
            if (wavelet != null) {
              wavelet.setState(State.CORRUPTED);
            }
            callback.onFailure(error);
            return;
          }
        }
        String error = null;
        try {
          final RemoteWaveletContainer remoteWavelet = getOrCreateRemoteWavelet(waveletName);

          // Update this remote wavelet with the immediately incoming delta, providing a callback
          // so that incoming historic deltas (as well as this delta) can be given to the
          // clientListener at a later point
          remoteWavelet.update(appliedDeltas, domain, federationRemote,
              new RemoteWaveletDeltaCallback() {
                public void ready(DeltaSequence result) {
                  if (clientListener != null) {
                    Map<String, BufferedDocOp> documentState =
                        getWavelet(waveletName).getWaveletData().getDocuments();
                    clientListener.waveletUpdate(waveletName, result, result.getEndVersion(),
                        documentState);
                  } else {
                    LOG.warning("Got valid deltaSequence for " + waveletName
                        + ", clientListener is null");
                  }
                }
              });
          if (committedHashedVersion != null && remoteWavelet.committed(committedHashedVersion)) {
            if (clientListener != null) {
              clientListener.waveletCommitted(waveletName, committedHashedVersion);
            } else {
              LOG.warning("Client listener is null");
            }
          }
          // TODO: when we support federated groups, forward to federationHosts too.
        } catch(WaveletStateException e) {
          // HACK(jochen): TODO: fix the case of the missing history! ###
          LOG.severe("DANGER WILL ROBINSON, WAVELET HISTORY IS INCOMPLETE!!!", e);
          error = e.getMessage();
        } catch (HostingException e) {
          error = e.getMessage();
        } catch (WaveServerException e) {
          error = e.getMessage();
        } catch (IllegalArgumentException e) {
          error = "Bad wavelet name " + e.getMessage();
        }
        if (error == null) {
          callback.onSuccess();
        } else {
          LOG.warning("incoming waveletUpdate: bad update, " + error);
          callback.onFailure(error);
        }
      }
    };
  }

  // -------------------------------------------------------------------------------------------
  // METHODS IMPLEMENTING THE WAVELET-FEDERATION-PROVIDER INTERFACE USED BY THE FED REMOTE.
  // -------------------------------------------------------------------------------------------

  @Override
  public void setListener(WaveletListener listener) {
    clientListener = listener;
  }

  @Override
  public void submitRequest(WaveletName waveletName, ProtocolSignedDelta signedDelta,
      SubmitResultListener listener) {
    // Disallow creation of wavelets by remote users.
    if (signedDelta.getDelta().getHashedVersion().getVersion() == 0) {
      LOG.warning("Remote user tried to submit delta at version 0 - disallowed. " + signedDelta);
      listener.onFailure("Remote users may not create wavelets.");
      return;
    }
    try {
      certificateManager.verifyDelta(signedDelta);
      submitDelta(waveletName, signedDelta, listener);
    } catch(SignatureException e) {
      LOG.warning("Submit request: Delta failed verification. WaveletName: " + waveletName +
          " delta: " + signedDelta, e);
      WaveletContainer wavelet = getWavelet(waveletName);
      if (wavelet != null) {
        wavelet.setState(State.CORRUPTED);
      }
    }
  }

  @Override
  public void requestHistory(WaveletName waveletName, String domain,
      ProtocolHashedVersion startVersion, ProtocolHashedVersion endVersion,
      long lengthLimit, HistoryResponseListener listener) {
    WaveletContainer wc = getWavelet(waveletName);
    if (wc == null) {
      listener.onFailure("Wavlet " + waveletName + " does not exist.");
      LOG.info("Request history: " + domain + " requested non-existant wavelet: " +
          waveletName);
    } else {
      // TODO: once we support federated groups, expand support to request
      // remote wavelets too.
      if (!isLocalWavelet(waveletName)) {
        listener.onFailure("Wavlet " + waveletName + " not hosted here.");
        LOG.info("Federation remote for domain: " + domain + " requested a remote wavelet: " +
            waveletName);
      } else {
        NavigableSet<ProtocolAppliedWaveletDelta> deltaHistory;
        try {
          deltaHistory = wc.requestHistory(startVersion, endVersion);
          // Now determine whether we received the entire requested wavelet history.
          LOG.info("Found deltaHistory between " + startVersion + " - " + endVersion
              + ", returning to requester domain " + domain + " -- " + deltaHistory);
          listener.onSuccess(deltaHistory, endVersion.getVersion(), endVersion.getVersion());
          // TODO: ### check length limit ??
//           else {
//            ProtocolAppliedWaveletDelta lastDelta = deltaHistory.last();
//            long lastVersion = lastDelta.getHashedVersionAppliedAt().getVersion() +
//                lastDelta.getOperationsApplied();
//            listener.onSuccess(deltaHistory, lcv.getVersion(),
//                lastVersion);
//          }
        } catch (WaveletStateException e) {
          LOG.severe("Error retrieving wavelet history: " + waveletName + " " + startVersion +
              " - " + endVersion);
          listener.onFailure("Server error while retrieving wavelet history.");
          return;
        }
      }
    }
  }

  @Override
  public void getDeltaSignerInfo(ByteString signerId,
      WaveletName waveletName, ProtocolHashedVersion deltaEndVersion,
      DeltaSignerInfoResponseListener listener) {
    // TODO: retrieve the delta and check that the signer id is in one of its signature,
    //       otherwise call onFailure() to complain
    ProtocolSignerInfo info = certificateManager.retrieveSignerInfo(signerId);
    if (info == null) {
      // This is bad: we breach our obligation to store the signer
      // info (certificate chain) of all signed deltas that we host.
      String error = "cannot find signer info for signed delta (breach of contract!)";
      LOG.severe("incoming getDeltaSignerInfo: " + error);
      listener.onFailure(error);
      return;
    }
    listener.onSuccess(info);
  }

  @Override
  public void postSignerInfo(String destinationDomain, ProtocolSignerInfo signerInfo,
      PostSignerInfoResponseListener listener) {
    try {
      certificateManager.storeSignerInfo(signerInfo);
    } catch (SignatureException e) {
      String error = "verification failure from domain " + signerInfo.getDomain();
      LOG.warning("incoming postSignerInfo: " + error, e);
      listener.onFailure(error);
      return;
    }
    listener.onSuccess();
  }

  // -------------------------------------------------------------------------------------------
  // METHODS IMPLEMENTING THE WAVELET-PROVIDER INTERFACE USED BY THE CLIENT FRONTEND.
  // -------------------------------------------------------------------------------------------

  @Override
  public NavigableSet<ProtocolWaveletDelta> requestHistory(WaveletName waveletName,
      ProtocolHashedVersion startVersion, ProtocolHashedVersion endVersion) {
    WaveletContainer wc = getWavelet(waveletName);
    if (wc == null) {
      LOG.info("Client request for history made for non-existant wavelet: " + waveletName);
      return null;
    } else {
      NavigableSet<ProtocolWaveletDelta> deltaHistory = null;
      try {
        deltaHistory = wc.requestTransformedHistory(startVersion, endVersion);
      } catch (AccessControlException e) {
        LOG.warning("Client requested history with incorrect hashedVersion: " + waveletName + " " +
            " startVersion: " + startVersion + " endVersion: " + endVersion);
      } catch (WaveletStateException e) {
        if (e.getState() == State.LOADING) {
          LOG.severe("Client Frontend requested history for a remote wavelet that is not yet" +
          "available - this should not happen as we have not sent an update for the wavelet.");
        } else {
          LOG.severe("Error retrieving wavelet history: " + waveletName + " " + startVersion +
              " - " + endVersion);
        }
      }
      return deltaHistory;
    }
  }

  @Override
  public void submitRequest(WaveletName waveletName, ProtocolWaveletDelta delta,
      SubmitResultListener listner) {
    ProtocolSignedDelta signedDelta = certificateManager.signDelta(delta);
    submitDelta(waveletName, signedDelta, listner);
  }

  // -------------------------------------------------------------------------------------------
  //  CONSTRUCTOR AND PRIVATE UTILITY METHODS.
  // -------------------------------------------------------------------------------------------

  /**
   * Constructor.
   *
   * @param certificateManager provider of certificates; it also determines which
   *        domains this wave server regards as local wavelets.
   * @param federationHostFactory factory that returns federation host instance listening
   *        on a given domain.
   */
  @Inject
  public WaveServerImpl(CertificateManager certificateManager,
      @FederationHostBridge WaveletFederationListener.Factory federationHostFactory,
      @FederationRemoteBridge WaveletFederationProvider federationRemote,
      LocalWaveletContainer.Factory localWaveletContainerFactory,
      RemoteWaveletContainer.Factory remoteWaveletContainerFactory) {
    this.certificateManager = certificateManager;
    this.federationHostFactory = federationHostFactory;
    this.federationRemote = federationRemote;

    this.localWaveletContainerFactory = localWaveletContainerFactory;
    this.remoteWaveletContainerFactory = remoteWaveletContainerFactory;

    LOG.info("Wave Server configured to host local domains: "
        + certificateManager.getLocalDomains().toString());
  }

  private boolean isLocalWavelet(WaveletName waveletName) {
    LOG.info("### WS is local? " + waveletName + " = " + certificateManager.getLocalDomains().
        contains(waveletName.waveletId.getDomain()));

    return certificateManager.getLocalDomains().contains(waveletName.waveletId.getDomain());
  }

  private boolean checkWaveletHosting(boolean isLocal, WaveletName waveletName)
      throws HostingException {
    boolean l = isLocalWavelet(waveletName);
    if (l != isLocal) {
      throw new HostingException("Wavelet (" + waveletName + ") which is " +
          (l ? "local" : "remote") + " should have been " + (l ? "remote." : "local."));
    }
    return l == isLocal;
  }

  /**
   * Returns a container for a remote wavelet. If it doesn't exist, it will be created.
   * This method is only called in response to a Federation Remote doing an update
   * or commit on this wavelet.
   *
   * @param waveletName name of wavelet
   * @throws HostingException if the name refers to a local wavelet.
   * @return an existing or new instance.
   * @throws IllegalArgumentException on bad wavelet name
   */
  private RemoteWaveletContainer getOrCreateRemoteWavelet(WaveletName waveletName) throws
      HostingException {
    checkWaveletHosting(false, waveletName);
    synchronized (waveMap) {
      Map<WaveletId, WaveletContainer> wave = waveMap.get(waveletName.waveId);
      // This will blow up if we messed up and put a local wavelet in by mistake.
      RemoteWaveletContainer wc = (RemoteWaveletContainer) wave.get(waveletName.waveletId);
      if (wc == null) {
        wc = remoteWaveletContainerFactory.create(waveletName);
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
   * @throws HostingException if the name refers to a local wavelet.
   * @throws AccessControlException if the participant may not access the wavelet.
   * @return an existing or new instance.
   * @throws WaveletStateException if the wavelet is in a bad state.
   */
  private LocalWaveletContainer getOrCreateLocalWavelet(WaveletName waveletName,
      ParticipantId participantId) throws
      HostingException, AccessControlException, WaveletStateException {
    checkWaveletHosting(true, waveletName);
    synchronized (waveMap) {
      Map<WaveletId, WaveletContainer> wave = waveMap.get(waveletName.waveId);
      // This will blow up if we messed up and put a remote wavelet in by mistake.
      LocalWaveletContainer wc = (LocalWaveletContainer) wave.get(waveletName.waveletId);
      if (wc == null) {
        wc = localWaveletContainerFactory.create(waveletName);
        // TODO: HACK(Jochen): do we need a namespace policer here ??? ###
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

  private ProtocolHashedVersion getVersionAfter(ProtocolAppliedWaveletDelta delta) {
    // ### TODO: This is a unsigned version. Need to create a signed one!
    long version = delta.getHashedVersionAppliedAt().getVersion() + delta.getOperationsApplied();
    return WaveletOperationSerializer.serialize(HashedVersion.unsigned(version));
  }

  /**
   * Submit the delta to local or remote wavelets, return results via listener.
   * Also broadcast updates to federationHosts and clientFrontend.
   *
   * TODO: for now a the WaveletFederationProvider will have
   *               made sure this is a local wavelet. Once we support
   *               federated groups, that test should be removed.
   */
  private void submitDelta(WaveletName waveletName, ProtocolSignedDelta delta,
      SubmitResultListener resultListener) {
    if (isLocalWavelet(waveletName)) {
      DeltaApplicationResult submitResult;
      ProtocolAppliedWaveletDelta appliedDelta;
      LocalWaveletContainer wc;
      try {

        LOG.info("## WS: Got submit: " + waveletName + " delta: " + delta.toString());

        wc = getOrCreateLocalWavelet(waveletName,
            new ParticipantId(delta.getDelta().getAuthor()));
        submitResult = wc.submitRequest(waveletName, delta);
        appliedDelta = submitResult.getAppliedDelta();

        // return result to caller.
        ProtocolHashedVersion resultingVersion = getVersionAfter(appliedDelta);
        LOG.info("## WS: Submit result: " + waveletName + " appliedDelta: " + appliedDelta);
        resultListener.onSuccess(appliedDelta.getOperationsApplied(),
            resultingVersion, appliedDelta.getApplicationTimestamp());
      } catch (AccessControlException e) {
        resultListener.onFailure(e.getMessage());
        return;
      } catch (OperationException e) {
        resultListener.onFailure(e.getMessage());
        return;
      } catch (WaveletStateException e) {
        resultListener.onFailure(e.getMessage());
        return;
      } catch (IllegalArgumentException e) {
        resultListener.onFailure(e.getMessage());
        return;
      } catch (HostingException e) {
        throw new IllegalStateException("Should not get HostingException after " +
        		"checking isLocalWavelet", e);
      }

      // broadcast the results.
      if (clientListener != null) {
        Map<String, BufferedDocOp> documentState =
          getWavelet(waveletName).getWaveletData().getDocuments();
        clientListener.waveletUpdate(waveletName, ImmutableList.of(submitResult.getDelta()),
            submitResult.getHashedVersionAfterApplication(), documentState);
      }

      for (WaveletFederationListener host : getParticipantsFederationHosts(wc)) {
         host.waveletUpdate(waveletName, ImmutableList.of(appliedDelta), null,
            new WaveletFederationListener.WaveletUpdateCallback() {
              @Override public void onSuccess() { }
              @Override public void onFailure(String errorMessage) {
                LOG.warning("outgoing waveletUpdate failure: " + errorMessage);
                // TODO: add retransmit logic
              }
            });
      }
    } else {
      // For remote wavelets wait for the result to come back from the federation remote.
      federationRemote.submitRequest(waveletName, delta, resultListener);
    }
  }

  private Set<WaveletFederationListener> getParticipantsFederationHosts(LocalWaveletContainer lwc) {
    HashSet<WaveletFederationListener> hosts = Sets.newHashSet();
    Set<String> localDomains = certificateManager.getLocalDomains();
    for (ParticipantId p : lwc.getParticipants()) {
      String domain = p.getDomain();
      if (localDomains.contains(domain)) {
        // Ignore, don't re-federate to a local domain.
      } else {
        hosts.add(federationHosts.get(p.getDomain()));
      }
    }
    return ImmutableSet.copyOf(hosts);
  }
}

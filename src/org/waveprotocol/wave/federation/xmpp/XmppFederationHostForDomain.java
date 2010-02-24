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

package org.waveprotocol.wave.federation.xmpp;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.inject.name.Named;
import com.google.protobuf.ByteString;


import org.dom4j.Element;
import org.waveprotocol.wave.federation.FederationErrorProto.FederationError;
import org.waveprotocol.wave.federation.FederationErrors;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.waveserver.WaveletFederationListener;
import org.waveprotocol.wave.protocol.common.ProtocolHashedVersion;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;

import java.util.List;
import java.util.Queue;
import java.util.logging.Logger;

/**
 * An instance of this class is created on demand for outgoing messages to
 * another wave Federation Remote. The wave server asks the WaveXmppComponent to
 * create these.
 */
class XmppFederationHostForDomain implements WaveletFederationListener {
  private static final Logger LOG =
    Logger.getLogger(XmppFederationHostForDomain.class.getCanonicalName());

  // Timeout for outstanding listener updates sent over XMPP.
  private static final int XMPP_LISTENER_TIMEOUT = 30;

  private static enum DiscoStatus {
    PENDING,
    COMPLETED,
    FAILED
  }

  private final String remoteDomain;
  private final XmppManager manager;
  private final Queue<SuccessFailCallback<String, String>> queuedMessages =
      Lists.newLinkedList();
  private final String jid;
  private String remoteJid;
  private DiscoStatus discoStatus = DiscoStatus.PENDING;

  public XmppFederationHostForDomain(final String domain, XmppManager manager,
      XmppDisco disco, @Named("xmpp_jid") String jid) {
    this.remoteDomain = domain;
    this.manager = manager;
    this.jid = jid;

    // start discovery.
    disco.discoverRemoteJid(remoteDomain, new SuccessFailCallback<String, String>() {
      @Override
      public void onSuccess(String result) {
        synchronized (queuedMessages) {
          discoCompleted(result);
        }
      }

      @Override
      public void onFailure(String errorMessage) {
        synchronized (queuedMessages) {
          discoFailed(errorMessage);
        }
      }
    });
  }

  @Override
  public void waveletCommitUpdate(WaveletName waveletName, ProtocolHashedVersion committedVersion,
      WaveletUpdateCallback callback) {
    waveletUpdate(waveletName, null, committedVersion, callback);
  }

  @Override
  public void waveletDeltaUpdate(WaveletName waveletName, List<ByteString> appliedDeltas,
      WaveletUpdateCallback callback) {
    waveletUpdate(waveletName, appliedDeltas, null, callback);
  }

  /**
   * Called when XMPP discovery is complete. Sends queued messages.
   *
   * @param remoteJid the discovered remote JID for this domain
   */
  private void discoCompleted(String remoteJid) {
    Preconditions.checkState(discoStatus == DiscoStatus.PENDING);
    Preconditions.checkNotNull(remoteJid);
    this.remoteJid = remoteJid;
    this.discoStatus = DiscoStatus.COMPLETED;
    LOG.info("Disco completed for " + remoteDomain + ", running " + queuedMessages.size()
        + " queued messages");
    while (!queuedMessages.isEmpty()) {
      queuedMessages.poll().onSuccess(remoteJid);
    }
  }

  /**
   * Called when XMPP discovery fails.  Queued messages are flushed.
   *
   * @param errorMessage
   */
  private void discoFailed(String errorMessage) {
    Preconditions.checkState(discoStatus == DiscoStatus.PENDING);
    this.remoteJid = null;
    this.discoStatus = DiscoStatus.FAILED;
    LOG.warning("Disco failed for " + remoteDomain + ", failing " + queuedMessages.size()
        + " queued messages");
    while (!queuedMessages.isEmpty()) {
      queuedMessages.poll().onFailure(errorMessage);
    }
  }

  /**
   * @return the current remote JID
   */
  @VisibleForTesting
  String getRemoteJid() {
    return remoteJid;
  }

  /**
   * Sends a wavelet update message on behalf of the wave server. This method
   * may contain applied deltas, a commit notice, or both.
   *
   * @param waveletName the wavelet name
   * @param deltaList the deltas to include in the message, or null
   * @param committedVersion last committed version to include, or null
   * @param callback callback to invoke on delivery success/failure
   */
  public void waveletUpdate(final WaveletName waveletName, final List<ByteString> deltaList,
      final ProtocolHashedVersion committedVersion, final WaveletUpdateCallback callback) {
    if ((deltaList == null || deltaList.isEmpty()) && committedVersion == null) {
      throw new IllegalArgumentException("Must send at least one delta, "
          + "or a last committed version notice, for the target wavelet: " + waveletName);
    }

    // If disco is not yet complete, register a runnable to invoke this method
    // at a later point in time.
    synchronized (queuedMessages) {
      if (discoStatus == DiscoStatus.PENDING) {
        LOG.info("Disco is pending for " + remoteDomain + ", queueing update (already "
            + queuedMessages.size() + " messages in queue)");
        queuedMessages.offer(new SuccessFailCallback<String, String>() {
          @Override
          public void onSuccess(String remoteJid) {
            waveletUpdate(waveletName, deltaList, committedVersion, callback);
          }

          @Override
          public void onFailure(String errorMessage) {
            callback.onFailure(FederationErrors.newFederationError(
                FederationError.Code.RESOURCE_CONSTRAINT, errorMessage));
          }
        });
        return;
      } else if (discoStatus == DiscoStatus.FAILED) {
        // TODO(kalman): reactivate after a time period, or hook into FedEx, or something in order
        // to not blacklist this domain for the lifetime of this gateway.
        String error = "Disco failed for " + remoteDomain + ", ignoring update for " + waveletName;
        LOG.warning(error);
        callback.onFailure(FederationErrors.newFederationError(
            FederationError.Code.RESOURCE_CONSTRAINT, error));
        return;
      } else {
        // Disco has completed, so we must have found a JID.
        Preconditions.checkState(remoteJid != null);
      }
    }

    Message message = new Message();
    message.setType(Message.Type.normal);
    message.setFrom(jid);
    message.setTo(remoteJid);
    message.setID(XmppUtil.generateUniqueId());
    message.addChildElement("request", XmppNamespace.NAMESPACE_XMPP_RECEIPTS);

    final String encodedWaveletName;
    try {
      encodedWaveletName = XmppUtil.waveletNameCodec.encode(waveletName);
    } catch (IllegalArgumentException e) {
      // TODO(thorogood): Error message.
      callback.onFailure(FederationError.newBuilder()
          .setErrorCode(FederationError.Code.BAD_REQUEST).build());
      return;
    }

    Element itemElement = message.addChildElement("event", XmppNamespace.NAMESPACE_PUBSUB_EVENT)
        .addElement("items").addElement("item");
    if (deltaList != null) {
      for (ByteString delta : deltaList) {
        Element waveletUpdate = itemElement.addElement("wavelet-update",
            XmppNamespace.NAMESPACE_WAVE_SERVER).addAttribute("wavelet-name", encodedWaveletName);
        waveletUpdate.addElement("applied-delta").addCDATA(Base64Util.encode(delta.toByteArray()));
      }
    }
    if (committedVersion != null) {
      Element waveletUpdate =
          itemElement.addElement("wavelet-update", XmppNamespace.NAMESPACE_WAVE_SERVER)
              .addAttribute("wavelet-name", encodedWaveletName);
      waveletUpdate.addElement("commit-notice").addAttribute("version",
          Long.toString(committedVersion.getVersion())).addAttribute("history-hash",
          Base64Util.encode(committedVersion.getHistoryHash()));
    }

    // Send the generated message through to the foreign XMPP server.
    manager.send(message, new PacketCallback() {
      @Override
      public void error(FederationError error) {
        callback.onFailure(error);
      }

      @Override
      public void run(Packet packet) {
        callback.onSuccess();
      }
    }, XMPP_LISTENER_TIMEOUT);
  }
}

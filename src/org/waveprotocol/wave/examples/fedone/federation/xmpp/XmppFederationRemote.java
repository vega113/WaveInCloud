/*
 * Copyright (C) 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.waveprotocol.wave.examples.fedone.federation.xmpp;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.RpcCallback;

import org.apache.commons.codec.binary.Base64;
import org.dom4j.Attribute;
import org.dom4j.Element;
import org.waveprotocol.wave.examples.fedone.waveserver.FederationRemoteBridge;
import org.waveprotocol.wave.examples.fedone.waveserver.SubmitResultListener;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveletFederationListener;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveletFederationProvider;
import org.waveprotocol.wave.model.id.URIEncoderDecoder;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.protocol.common;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Remote implementation. Receives submit and history requests from the local
 * wave server and sends them to a remote wave server Host, and also receives
 * update messages from a remote wave server Host and sends them to the local
 * wave server.
 *
 *
 */
@Singleton
public class XmppFederationRemote implements WaveletFederationProvider {

  private static final Logger logger =
      Logger.getLogger(XmppFederationRemote.class.getCanonicalName());
  private WaveletFederationListener.Factory updatesListenerFactory;
  private WaveXmppComponent component = null;

  /**
   * Constructor.
   *
   * @param updatesListenerFactory used to communicate back to the local wave
   *                               server when an update arrives.
   */
  @Inject
  public XmppFederationRemote(
      @FederationRemoteBridge WaveletFederationListener.Factory updatesListenerFactory
  ) {
    this.updatesListenerFactory = updatesListenerFactory;
  }

  /**
   * Set the wave server (i.e. its WaveletFederationListener.Factory) after
   * construction.
   *
   * TODO: used to current resolve problems with Guice, should be removed.
   *
   * @param updatesListenerFactory used to communicate back to the local wave
   *                               server when an update arrives.
   */
  public void setWaveServer(
      WaveletFederationListener.Factory updatesListenerFactory) {
    this.updatesListenerFactory = updatesListenerFactory;
  }

  /**
   * Sets the parent component for this instance. Must be called before any
   * other methods will work.
   *
   * @param component the component
   */
  public void setComponent(WaveXmppComponent component) {
    this.component = component;
  }

  /**
   * Request submission of signed delta. This is part of the Federation Remote
   * interface - sends a submit request on behalf of the wave server. <p/> Part
   * of the WaveletFederationProvider interface.
   *
   * @param waveletName name of wavelet.
   * @param delta       delta signed by the submitting wave server.
   * @param listener    callback for the result.
   */
  public void submitRequest(final WaveletName waveletName,
                            final common.ProtocolSignedDelta delta,
                            final SubmitResultListener listener) {
    // TODO: LOG.debug
    logger.info("Submitting delta to remote server, wavelet " + waveletName + " target version "
        + delta.getDelta().getHashedVersion());
    final IQ submitIq = new IQ(IQ.Type.set);
    submitIq.setFrom(component.componentJID);
    submitIq.setID(component.generateId());
    Element pubsub =
        submitIq.setChildElement("pubsub", WaveXmppComponent.NAMESPACE_PUBSUB);
    Element publish = pubsub.addElement("publish");
    publish.addAttribute("node", "wavelet");
    Element submitRequest =
        publish.addElement("item").addElement("submit-request",
                                              WaveXmppComponent.NAMESPACE_WAVE_SERVER);
    Element deltaElement = submitRequest.addElement("delta");
    deltaElement.addCDATA(new String(Base64.encodeBase64(delta.toByteArray())));
    try {
      deltaElement.addAttribute("wavelet-name",
                                WaveXmppComponent.waveletNameEncoder.waveletNameToURIPath(
                                    waveletName));
    } catch (URIEncoderDecoder.EncodingException e) {
      listener.onFailure("couldn't encode wavelet name " + waveletName);
      return;
    }
    final SubmitResponseListener onSuccess =
        new SubmitResponseListener(listener);

    component.disco.discoverRemoteJid(waveletName.waveletId.getDomain(),
                                      new RpcCallback<String>() {
                                        public void run(String jid) {
                                          if (jid == null) {
                                            listener.onFailure(
                                                "no such wave server "
                                                + waveletName.waveletId
                                                    .getDomain());
                                          }
                                          submitIq.setTo(jid);
                                          component.sendPacket(submitIq, true,
                                                               onSuccess);
                                        }
                                      });
  }

  /**
   * Retrieve delta history for the given wavelet. <p/> Part of the
   * WaveletFederationProvider interface.
   *
   * @param waveletName  name of wavelet.
   * @param domain       the remote Federation Host
   * @param startVersion beginning of range (inclusive), minimum 0.
   * @param endVersion   end of range (exclusive).
   * @param lengthLimit  estimated size, in bytes, as an upper limit on the
   *                     amount of data returned.
   * @param listener     callback for the result.
   */
  public void requestHistory(final WaveletName waveletName, final String domain,
                             common.ProtocolHashedVersion startVersion,
                             common.ProtocolHashedVersion endVersion,
                             long lengthLimit,
                             final WaveletFederationProvider.HistoryResponseListener listener) {
    // TODO: LOG.debug
    logger.info("Getting history from remote server, wavelet " + waveletName + " version "
        + startVersion + " (inc) through " + endVersion + " (ex)");
    final IQ submitIq = new IQ(IQ.Type.get);
    submitIq.setID(component.generateId());
    submitIq.setFrom(component.componentJID);

    Element pubsub =
        submitIq.setChildElement("pubsub", WaveXmppComponent.NAMESPACE_PUBSUB);
    Element items = pubsub.addElement("items");
    items.addAttribute("node", "wavelet");
    Element historyDelta = items.addElement("delta-history",
                                            WaveXmppComponent.NAMESPACE_WAVE_SERVER);

    historyDelta.addAttribute("start-version",
                              Long.toString(startVersion.getVersion()));
    historyDelta.addAttribute("start-version-hash",
                              Base64Util.encode(startVersion.getHistoryHash()));
    historyDelta
        .addAttribute("end-version", Long.toString(endVersion.getVersion()));
    historyDelta.addAttribute("end-version-hash",
                              Base64Util.encode(endVersion.getHistoryHash()));
    if (lengthLimit > 0) {
      historyDelta
          .addAttribute("response-length-limit", Long.toString(lengthLimit));
    }
    try {
      historyDelta.addAttribute("wavelet-name",
                                WaveXmppComponent.waveletNameEncoder.waveletNameToURIPath(
                                    waveletName));
    } catch (URIEncoderDecoder.EncodingException e) {
      listener.onFailure("couldn't encode wavelet name " + waveletName);
      return;
    }
    // TODO: move to inner class
    final HistoryResponseListener onSuccess =
        new HistoryResponseListener(listener);
    component.disco.discoverRemoteJid(domain,
                                      new RpcCallback<String>() {
                                        public void run(String jid) {
                                          if (jid == null) {
                                            listener.onFailure(
                                                "no such wave server "
                                                + domain);
                                          }
                                          submitIq.setTo(jid);
                                          component.sendPacket(submitIq, true,
                                                               onSuccess);
                                        }
                                      });


  }

  @Override
  public void getDeltaSignerInfo(ByteString signerId, WaveletName waveletName,
                                 common.ProtocolHashedVersion deltaEndVersion,
                                 final DeltaSignerInfoResponseListener listener) {
    final IQ getSignerIq = new IQ(IQ.Type.get);
    getSignerIq.setID(component.generateId());
    getSignerIq.setFrom(component.componentJID);
    // Extract domain from waveletId
    final String remoteDomain = waveletName.waveletId.getDomain();
    Element pubsub =
        getSignerIq
            .setChildElement("pubsub", WaveXmppComponent.NAMESPACE_PUBSUB);
    Element items = pubsub.addElement("items");
    items.addAttribute("node", "signer");
    // TODO: should allow multiple requests in the same packet
    Element signerRequest = items.addElement("signer-request",
                                             WaveXmppComponent.NAMESPACE_WAVE_SERVER);
    signerRequest.addAttribute("signer-id", Base64Util.encode(signerId));
    signerRequest.addAttribute("history-hash",
                               Base64Util.encode(
                                   deltaEndVersion.getHistoryHash()));
    signerRequest.addAttribute("version", String.valueOf(
        deltaEndVersion.getVersion()));
    try {
      signerRequest.addAttribute("wavelet-name",
                                 WaveXmppComponent.waveletNameEncoder.waveletNameToURIPath(
                                     waveletName));
    } catch (URIEncoderDecoder.EncodingException e) {
      listener.onFailure("couldn't encode wavelet name " + waveletName);
      return;
    }

    final GetSignerResponseListener onSuccess =
        new GetSignerResponseListener(listener);
    component.disco.discoverRemoteJid(remoteDomain,
                                      new RpcCallback<String>() {
                                        public void run(String jid) {
                                          if (jid == null) {
                                            listener.onFailure(
                                                "no such wave server "
                                                + remoteDomain);
                                          }
                                          getSignerIq.setTo(jid);
                                          component
                                              .sendPacket(getSignerIq, true,
                                                          onSuccess);
                                        }
                                      });
  }

  public void postSignerInfo(final String destinationDomain,
                             common.ProtocolSignerInfo signerInfo,
                             final WaveletFederationProvider.PostSignerInfoResponseListener listener) {
    final IQ request = new IQ(IQ.Type.set);
    request.setFrom(component.componentJID);
    request.setID(component.generateId());
    Element pubsub =
        request.setChildElement("pubsub", WaveXmppComponent.NAMESPACE_PUBSUB);
    Element publish = pubsub.addElement("publish");
    publish.addAttribute("node", "signer");
    XmppUtil.protocolSignerInfoToXml(signerInfo, publish.addElement("item"));
    final PostSignerInfoResponseListener onSuccess = new
        PostSignerInfoResponseListener(listener);
    component.disco.discoverRemoteJid(destinationDomain,
                                      new RpcCallback<String>() {
                                        public void run(String jid) {
                                          if (jid == null) {
                                            listener.onFailure(
                                                "no such wave server "
                                                + destinationDomain);
                                          }
                                          request.setTo(jid);
                                          component.sendPacket(request, true,
                                                               onSuccess);
                                        }
                                      });
  }


  /**
   * Handles a wavelet update message from a foreign Federation Host.
   *
   * @param updateMessage the XMPP message.
   */
  @SuppressWarnings("unchecked")
  public void update(final Message updateMessage) {
    final Element receiptRequested =
        updateMessage.getChildElement("request",
                                      WaveXmppComponent.NAMESPACE_XMPP_RECEIPTS);

    Element event =
        updateMessage.getChildElement("event",
                                      WaveXmppComponent.NAMESPACE_PUBSUB_EVENT);
    if (event == null) {
      logger.warning("event element missing from message: " + updateMessage);
      // TODO: send back an error message
      return;
    }
    Element items = event.element("items");
    if (items == null) {
      logger.warning(
          "items element missing from update message: " + updateMessage);
      // TODO: send back an error message
      return;
    }
    //noinspection unchecked
    List<Element> elements = items.elements("item");
    if (elements.isEmpty()) {
      // TODO: send back error if receiptRequested
      return;
    }
    final AtomicInteger callbackCount = new AtomicInteger(elements.size());
    WaveletFederationListener.WaveletUpdateCallback callback =
        new WaveletFederationListener.WaveletUpdateCallback() {
          @Override
          public void onSuccess() {
            countDown();
          }

          @Override
          public void onFailure(String errorMessage) {
            logger.warning(
                "incoming XMPP waveletUpdate failure: " + errorMessage);
            // TODO: propagate error to foreign remote in countDown()
            countDown();
          }

          private void countDown() {
            if (callbackCount.decrementAndGet() == 0
                && receiptRequested != null) {
              Message response = new Message();
              WaveXmppComponent
                  .copyRequestPacketFields(updateMessage, response);
              response.addChildElement("received",
                                       WaveXmppComponent.NAMESPACE_XMPP_RECEIPTS);
              component
                  .sendPacket(response, false /* no retry */, null
                              /* no callback */);
            }
          }
        };
    // We must call callback once on every iteration to ensure that we send response
    // if receiptRequested != null.
    for (Element item : elements) {
      Element waveletUpdate = item.element("wavelet-update");

      if (waveletUpdate == null) {
        callback.onFailure(
            "wavelet-update element missing from message: " + updateMessage);
        continue;
      }
      WaveletName waveletName;
      try {
        waveletName =
            WaveXmppComponent.waveletNameEncoder.uriPathToWaveletName(
                waveletUpdate.attributeValue("wavelet-name"));
      } catch (URIEncoderDecoder.EncodingException e) {
        callback.onFailure("couldn't decode wavelet name "
                           + waveletUpdate.attributeValue("wavelet-name"));
        continue;
      }
      WaveletFederationListener listener =
          updatesListenerFactory
              .listenerForDomain(waveletName.waveletId.getDomain());

      List<common.ProtocolAppliedWaveletDelta> deltas = Lists.newArrayList();
      common.ProtocolHashedVersion commitNotice = null;

      // Submit all applied deltas to the domain-focused listener.
      //noinspection unchecked
      for (Element appliedDeltaElement :
          (List<Element>) waveletUpdate.elements("applied-delta")) {
        String deltaBody = appliedDeltaElement.getText();

        try {
          deltas.add(common.ProtocolAppliedWaveletDelta.parseFrom(
              Base64.decodeBase64(deltaBody.getBytes())));
        } catch (InvalidProtocolBufferException e) {
          logger.warning("couldn't decode delta: " + appliedDeltaElement);
          continue;
        }
      }

      // Optionally submit any received last committed notice.
      Element commitNoticeElement = waveletUpdate.element("commit-notice");
      if (commitNoticeElement != null) {
        commitNotice =
            common.ProtocolHashedVersion.newBuilder()
                .setHistoryHash(Base64Util.decode(
                    commitNoticeElement.attributeValue("history-hash")))
                .setVersion(Long.parseLong(
                    commitNoticeElement.attributeValue("version")))
                .build();
      }

      listener.waveletUpdate(waveletName, deltas, commitNotice, callback);
    }
  }

  /**
   * This class receives a response from a submit request, decodes it and
   * forwards it to the wave server's federation remote listener.
   */
  class SubmitResponseListener implements RpcCallback<Packet> {

    private final SubmitResultListener listener;

    /**
     * Constructor.
     *
     * @param listener the listener in the local wave server that receives the
     *                 decoded result.
     */
    public SubmitResponseListener(SubmitResultListener listener) {
      this.listener = listener;
    }

    /**
     * Receives a submit request response packet and decode it, passing it on to
     * the wave server.
     */
    @Override
    public void run(Packet result) {
      Element pubsub, publish = null, item = null, submitResponse =
          null, hashedVersionElement = null;
      pubsub = ((IQ) result).getChildElement();
      if (pubsub != null) {
        publish = pubsub.element("publish");
        if (publish != null) {
          item = publish.element("item");
          if (item != null) {
            submitResponse = item.element("submit-response");
            if (submitResponse != null) {
              hashedVersionElement = submitResponse.element("hashed-version");
            }
          }
        }
      }

      if (pubsub == null || publish == null || item == null
          || submitResponse == null
          || hashedVersionElement == null
          || hashedVersionElement.attribute("history-hash") == null
          || hashedVersionElement.attribute("version") == null
          || submitResponse.attribute("application-timestamp") == null
          || submitResponse.attribute("operations-applied") == null) {
        logger.severe("unexpected submitResponse to submit request: "
                      + result);
        listener.onFailure("invalid submitResponse: " + result);
        return;
      }

      common.ProtocolHashedVersion.Builder hashedVersion =
          common.ProtocolHashedVersion.newBuilder();
      hashedVersion.setHistoryHash(Base64Util.decode(
          hashedVersionElement.attributeValue("history-hash")));
      hashedVersion.setVersion(Long.parseLong(
          hashedVersionElement.attributeValue("version")));
      long applicationTimestamp = Long.parseLong(
          submitResponse.attributeValue("application-timestamp"));
      int operationsApplied = Integer.parseInt(
          submitResponse.attributeValue("operations-applied"));
      listener.onSuccess(operationsApplied, hashedVersion.build(),
                         applicationTimestamp);
    }
  }

  /**
   * This class handles the response from a history request, decodes it and
   * forwards it to the wave server's federation remote listener.
   */
  class HistoryResponseListener implements RpcCallback<Packet> {

    private final WaveletFederationProvider.HistoryResponseListener listener;

    /**
     * Constructor.
     *
     * @param listener the listener in the local wave server that receives the
     *                 decoded result.
     */
    public HistoryResponseListener(
        WaveletFederationProvider.HistoryResponseListener listener) {
      this.listener = listener;
    }

    /**
     * Receives a history request response packet and decode it, passing it on
     * to the wave server.
     */
    @SuppressWarnings("unchecked")
    public void run(Packet historyResponse) {
      Element pubsubResponse = historyResponse.getElement().element("pubsub");
      Element items = pubsubResponse.element("items");
      long versionTruncatedAt = 0;
      long lastCommittedVersion = 0;
      Set<common.ProtocolAppliedWaveletDelta> deltaSet =
          new HashSet<common.ProtocolAppliedWaveletDelta>();

      if (items != null) {
        //noinspection unchecked
        for (Element itemElement : (List<Element>) items.elements()) {
          //noinspection unchecked
          for (Element element : (List<Element>) itemElement.elements()) {
            String elementName = element.getQName().getName();
            if (elementName.equals("applied-delta")) {
              String deltaBody = element.getText();
              try {
                deltaSet.add(common.ProtocolAppliedWaveletDelta.parseFrom(
                    Base64.decodeBase64(deltaBody.getBytes())));
              } catch (InvalidProtocolBufferException e) {
                listener.onFailure("couldn't decode delta: " + element);
              }
            } else if (elementName.equals("commit-notice")) {
              Attribute commitVersion = element.attribute("version");
              if (commitVersion != null) {
                lastCommittedVersion =
                    Long.parseLong(commitVersion.getValue());
              }
            } else if (elementName.equals("history-truncated")) {
              Attribute truncVersion = element.attribute("version");
              if (truncVersion != null) {
                versionTruncatedAt = Long.parseLong(truncVersion.getValue());
              }
            } else {
              listener.onFailure("bad response packet: " + historyResponse);
            }
          }
        }

      } else {
        listener.onFailure("bad response packet: " + historyResponse);
      }

      listener.onSuccess(deltaSet, lastCommittedVersion, versionTruncatedAt);
    }
  }

  /**
   * Receives a response from a getSigner query.
   */
  private class GetSignerResponseListener implements RpcCallback<Packet> {

    private DeltaSignerInfoResponseListener listener;

    public GetSignerResponseListener(DeltaSignerInfoResponseListener listener) {
      this.listener = listener;
    }

    public void run(Packet packet) {
      IQ response = (IQ) packet;
      Element items = response.getChildElement().element("items");
      Element signature = items.element("signature");
      if (signature == null) {
        logger.severe("empty getDeltaSignerRequest response: " + response);
        listener.onFailure("failed - bad getDeltaSignatureRequest response");
        return;
      }
      String domain = signature.attributeValue("domain");
      String hashName = signature.attributeValue("algorithm");
      if (domain == null || hashName == null
          || signature.element("certificate") == null) {
        logger.severe("bad getDeltaSignerRequest response: " + response);
        listener.onFailure("failed - bad getDeltaSignatureRequest response");
        return;
      }
      common.ProtocolSignerInfo signer =
          XmppUtil.xmlToProtocolSignerInfo(signature);
      listener.onSuccess(signer);
    }

  }

  static class PostSignerInfoResponseListener implements RpcCallback<Packet> {

    private WaveletFederationProvider.PostSignerInfoResponseListener listener;

    public PostSignerInfoResponseListener(
        WaveletFederationProvider.PostSignerInfoResponseListener listener) {
      this.listener = listener;
    }

    public void run(Packet packet) {
      IQ response = (IQ) packet;
      Element pubsub = response.getChildElement();
      Element item = pubsub.element("publish").element("item");
      if (item.element("signature-response") != null) {
        listener.onSuccess();
      } else {
        listener.onFailure("no valid response");
      }
    }
  }
}

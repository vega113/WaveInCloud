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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.InvalidProtocolBufferException;

import org.apache.commons.codec.binary.Base64;
import org.dom4j.Element;
import org.waveprotocol.wave.examples.fedone.waveserver.FederationHostBridge;
import org.waveprotocol.wave.examples.fedone.waveserver.SubmitResultListener;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveletFederationProvider;
import org.waveprotocol.wave.model.id.URIEncoderDecoder;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.protocol.common;
import org.xmpp.packet.IQ;

import java.util.Set;
import java.util.logging.Logger;

/**
 * This class encapsulates the incoming packet processing portion of the
 * Federation Host. Messages arrive on this class from a foreign Federation
 * Remote for wavelets hosted by the attached wave server.
 *
 *
 */
@Singleton
public class XmppFederationHost {

  private static final Logger logger =
      Logger.getLogger(XmppFederationHost.class.getCanonicalName());
  private WaveXmppComponent component;
  private final WaveletFederationProvider waveletProvider;

  /**
   * Constructor.
   *
   * @param waveletProvider used for communicating back to the Host part of the
   *                        wavelet server.
   */
  @Inject
  public XmppFederationHost(
      @FederationHostBridge WaveletFederationProvider waveletProvider) {
    this.waveletProvider = waveletProvider;
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

  /** History handling code for Host. Should be moved into another class */

  /**
   * Reads a history request off the wire, sends it to the Host with a callback
   * to sendHistoryResponse.
   *
   * @param historyPacket the XMPP IQ packet
   */
  void processHistoryRequest(IQ historyPacket) {
    Element items = null, historyDelta = null;
    Element pubsubRequest = historyPacket.getElement().element("pubsub");
    if (pubsubRequest != null) {
      items = pubsubRequest.element("items");
      if (items != null) {
        historyDelta = items.element("delta-history");
      }
    }
    if (items == null || historyDelta == null
        || historyDelta.attribute("start-version") == null
        || historyDelta.attribute("start-version-hash") == null
        || historyDelta.attribute("end-version") == null
        || historyDelta.attribute("end-version-hash") == null
        || historyDelta.attribute("wavelet-name") == null) {
      logger.warning("received invalid history request" + historyPacket);
      // TODO: send back an error to foreign remote
      return;
    }

    long startVersionVersion =
        Long.parseLong(historyDelta.attributeValue("start-version"));
    String startVersionHash = historyDelta.attributeValue("start-version-hash");
    common.ProtocolHashedVersion startVersion =
        common.ProtocolHashedVersion.newBuilder()
            .setVersion(startVersionVersion)
            .setHistoryHash(Base64Util.decode(startVersionHash)).build();

    long endVersionVersion =
        Long.parseLong(historyDelta.attributeValue("end-version"));
    String endVersionHash = historyDelta.attributeValue("end-version-hash");
    common.ProtocolHashedVersion endVersion =
        common.ProtocolHashedVersion.newBuilder()
            .setVersion(endVersionVersion)
            .setHistoryHash(Base64Util.decode(endVersionHash)).build();

    long responseLengthLimit = 0;
    if (historyDelta.attribute("response-length-limit") != null) {

      responseLengthLimit = Long.parseLong(
          historyDelta.attributeValue("response-length-limit"));
    }
    WaveletName waveletName;
    try {
      waveletName = WaveXmppComponent.waveletNameEncoder.uriPathToWaveletName(
          historyDelta.attributeValue("wavelet-name"));
    } catch (URIEncoderDecoder.EncodingException e) {
      logger.warning("couldn't decode wavelet name " + historyDelta
          .attributeValue("wavelet-name"));
      // TODO: return an error to foreign remote
      return;
    }

    // Hand off a history request to the waveletProvider.
    waveletProvider
        .requestHistory(waveletName, waveletName.waveletId.getDomain(),
                        startVersion, endVersion,
                        responseLengthLimit,
                        new HistoryResponsePacketListener(component,
                                                          historyPacket));
  }

  /**
   * Handles a submit request from a foreign wave remote. Sends it to the wave
   * server, sets up a callback to send the response.
   *
   * @param submitPacket the XMPP IQ packet
   */
  void processSubmitRequest(IQ submitPacket) {
    Element item = null, submitRequest = null, deltaElement = null;
    Element pubsubRequest = submitPacket.getElement().element("pubsub");
    // TODO: check for correct elements.
    Element publish = pubsubRequest.element("publish");
    if (publish != null) {
      item = publish.element("item");
      if (item != null) {
        submitRequest = item.element("submit-request");
        if (submitRequest != null) {
          deltaElement = submitRequest.element("delta");
        }
      }
    }
    if (publish == null || item == null || submitRequest == null
        || deltaElement == null
        || deltaElement.attribute("wavelet-name") == null
        || deltaElement.getText() == null) {
      logger.warning("received invalid submit request" + submitPacket);
      // TODO: send back an error to foreign remote.
      return;
    }

    final WaveletName waveletName;
    try {
      waveletName =
          WaveXmppComponent.waveletNameEncoder.uriPathToWaveletName(
              (deltaElement.attributeValue("wavelet-name")));
    } catch (URIEncoderDecoder.EncodingException e) {
      logger.warning("couldn't decode wavelet name "
                     + deltaElement.attributeValue("wavelet-name"));
      // TODO: return an error to foreign remote
      return;
    }
    common.ProtocolSignedDelta delta;

    try {
      delta = common.ProtocolSignedDelta.parseFrom(
          Base64.decodeBase64(deltaElement.getText().getBytes()));
    } catch (InvalidProtocolBufferException e) {
      // TODO: report a failure to the foreign remote.
      logger.warning("couldn't decode base64 delta: " + deltaElement.getText());
      return;
    }
    waveletProvider.submitRequest(waveletName, delta,
                                  new SubmitResponsePacketListener(component,
                                                                   submitPacket));
  }


  public void processGetSignerRequest(IQ request) {
    Element items = request.getChildElement().element("items");
    Element signerRequest = items.element("signer-request");

    String signerId = signerRequest.attributeValue("signer-id");
    String waveletNameUri = signerRequest.attributeValue("wavelet-name");
    String historyHash = signerRequest.attributeValue("history-hash");
    Long version = Long.parseLong(signerRequest.attributeValue("version"));
    if (waveletNameUri == null || historyHash == null || version == null) {
      logger.info("bad get signer request:\n" + request);
      // TODO: return error response
      return;
    }

    WaveletName waveletName;
    try {
      waveletName =
          WaveXmppComponent.waveletNameEncoder
              .uriPathToWaveletName(waveletNameUri);
    } catch (URIEncoderDecoder.EncodingException e) {
      logger.info("bad get signer request wavelet-name:\n" + waveletNameUri);
      // TODO: return error response
      return;
    }

    common.ProtocolHashedVersion.Builder deltaEndVersion =
        common.ProtocolHashedVersion.newBuilder();
    deltaEndVersion.setHistoryHash(Base64Util.decode(historyHash));
    deltaEndVersion.setVersion(version);

    DeltaSignerInfoPacketListener listener =
        new DeltaSignerInfoPacketListener(component, request);
    waveletProvider
        .getDeltaSignerInfo(Base64Util.decode(signerId), waveletName, deltaEndVersion.build(),
                            listener);
  }

  public void publishPostSignerRequest(IQ signerPacket) {
    Element item = null, signatureElement = null;
    Element pubsubRequest = signerPacket.getElement().element("pubsub");
    Element publish = pubsubRequest.element("publish");
    if (publish != null) {
      item = publish.element("item");
      if (item != null) {
        signatureElement = item.element("signature");
      }
    }
    if (publish == null || item == null || signatureElement == null
        || signatureElement.attribute("domain") == null
        || signatureElement.attribute("algorithm") == null
        || signatureElement.element("certificate") == null) {
      logger.warning("received invalid submit request" + signerPacket);
      // TODO: send back an error to foreign remote.
      return;
    }
    common.ProtocolSignerInfo signer =
        XmppUtil.xmlToProtocolSignerInfo(signatureElement);
    PostSignerInfoResponsePacketListener listener =
        new PostSignerInfoResponsePacketListener(component, signerPacket);
    // TODO: is the first element here meant to be the remote signing domain? it's also in the
    // common.PSI??
    waveletProvider.postSignerInfo(signatureElement.attributeValue("domain"),
                                   signer, listener);
  }

  /**
   * Handles the History Request response from the wave server, sends to the
   * foreign federation remote.
   */
  static class HistoryResponsePacketListener
      implements WaveletFederationProvider.HistoryResponseListener {

    private final WaveXmppComponent component;
    private final IQ historyRequest;

    public HistoryResponsePacketListener(
        WaveXmppComponent component, IQ historyRequest) {
      this.component = component;
      this.historyRequest = historyRequest;
    }

    public void onFailure(String errorMessage) {
      logger.warning("history request to waveserver failed: " + errorMessage);
      // TODO: communicate failure back to foreign remote.
    }

    public void onSuccess(
        Set<common.ProtocolAppliedWaveletDelta> deltaSet,
        long lastCommittedVersion,
        long versionTruncatedAt) {
      IQ response = new IQ(IQ.Type.result);
      WaveXmppComponent.copyRequestPacketFields(historyRequest, response);
      Element pubsub =
          response
              .setChildElement("pubsub", WaveXmppComponent.NAMESPACE_PUBSUB);
      Element items = pubsub.addElement("items");
      for (common.ProtocolAppliedWaveletDelta appliedDelta : deltaSet) {
        // add each delta
        items.addElement("item").addElement("applied-delta",
                                            WaveXmppComponent.NAMESPACE_WAVE_SERVER)
            .addCDATA(
                new String(Base64.encodeBase64(appliedDelta.toByteArray())));
      }
      if (lastCommittedVersion > 0) {
        items.addElement("item").addElement("commit-notice",
                                            WaveXmppComponent.NAMESPACE_WAVE_SERVER)
            .addAttribute("version", String.valueOf(lastCommittedVersion));
      }
      if (versionTruncatedAt > 0) {
        items.addElement("item").addElement("history-truncated",
                                            WaveXmppComponent.NAMESPACE_WAVE_SERVER)
            .addAttribute("version", String.valueOf(versionTruncatedAt));
      }
      component
          .sendPacket(response, false /* no retry */, null /*no callback*/);
    }
  }

  /**
   * This class gets the Submit Request result from the wave server, and sends
   * it out back to the original foreign federation remote.
   */
  static class SubmitResponsePacketListener implements SubmitResultListener {

    private final WaveXmppComponent component;
    private final IQ submitPacket;

    public SubmitResponsePacketListener(
        WaveXmppComponent component, IQ submitPacket) {
      this.component = component;
      this.submitPacket = submitPacket;
    }

    public void onSuccess(int operationsApplied,
                          common.ProtocolHashedVersion hashedVersionAfterApplication,
                          long applicationTimestamp) {
      IQ response = new IQ(IQ.Type.result);
      WaveXmppComponent.copyRequestPacketFields(submitPacket, response);
      Element pubsub =
          response
              .setChildElement("pubsub", WaveXmppComponent.NAMESPACE_PUBSUB);
      Element submitResponse = pubsub.addElement("publish").addElement("item")
          .addElement("submit-response",
                      WaveXmppComponent.NAMESPACE_WAVE_SERVER);
      submitResponse.addAttribute("application-timestamp",
                                  String.valueOf(applicationTimestamp));
      submitResponse.addAttribute("operations-applied",
                                  String.valueOf(operationsApplied));
      Element hashedVersion = submitResponse.addElement("hashed-version");
      hashedVersion.addAttribute("history-hash", Base64Util.encode(
          hashedVersionAfterApplication.getHistoryHash()));
      hashedVersion.addAttribute("version", String.valueOf(
          hashedVersionAfterApplication.getVersion()));

      component
          .sendPacket(response, false /* no retry */, null /* no callback */);
    }

    @Override
    public void onFailure(String errorMessage) {
      logger.warning("submit request to waveserver failed: " + errorMessage);
      // TODO: communicate failure back
    }
  }

  /**
   * Accepts a ProtocolSignerInfo response from the waveserver (local host) and
   * sends it out as XMPP.
   */
  static class DeltaSignerInfoPacketListener
      implements WaveletFederationProvider.DeltaSignerInfoResponseListener {

    private final WaveXmppComponent component;
    private IQ request;

    public DeltaSignerInfoPacketListener(
        WaveXmppComponent component, IQ request) {
      this.component = component;
      this.request = request;
    }

    public void onSuccess(common.ProtocolSignerInfo signerInfo) {
      IQ response = new IQ(IQ.Type.result);
      WaveXmppComponent.copyRequestPacketFields(request, response);
      Element pubsub =
          response
              .setChildElement("pubsub", WaveXmppComponent.NAMESPACE_PUBSUB);
      Element items = pubsub.addElement("items");
      XmppUtil.protocolSignerInfoToXml(signerInfo, items);
      /* no retry, no callback */
      component.sendPacket(response, false, null);
    }


    public void onFailure(String errorMessage) {
      // TODO: send back failure
    }
  }

  private class PostSignerInfoResponsePacketListener
      implements WaveletFederationProvider.PostSignerInfoResponseListener {

    private IQ request;
    private WaveXmppComponent component;

    public PostSignerInfoResponsePacketListener(WaveXmppComponent component,
                                                IQ signerPacket) {
      this.component = component;
      this.request = signerPacket;
    }

    public void onSuccess() {
      IQ response = new IQ(IQ.Type.result);
      WaveXmppComponent.copyRequestPacketFields(request, response);
      Element pubsub = response
          .setChildElement("pubsub", WaveXmppComponent.NAMESPACE_PUBSUB);
      Element item = pubsub.addElement("publish").addElement("item");
      item.addAttribute("node", "signer");
      item.addElement("signature-response",
                      WaveXmppComponent.NAMESPACE_WAVE_SERVER);
      component.sendPacket(response, false, null);
    }

    public void onFailure(String errorMessage) {
      // TODO: error response
    }
  }
}

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

import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import com.google.protobuf.RpcCallback;

import org.apache.commons.codec.binary.Base64;
import org.dom4j.Element;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveletFederationListener;
import org.waveprotocol.wave.model.id.URIEncoderDecoder;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.protocol.common;
import org.xmpp.packet.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * An instance of this class is created on demand for outgoing messages to
 * another wave Federation Remote. The wave server asks the WaveXMPPComponent to
 * create these.
 *
 *
 */
@Singleton
class XmppFederationHostForDomain implements WaveletFederationListener {

  private final String remoteDomain;
  private final WaveXmppComponent component;
  private boolean discoCompleted = false;
  // not private so tests can check it.
  String remoteJID;
  private static final Logger logger =
      Logger.getLogger(XmppFederationHostForDomain.class.getCanonicalName());
  private final List<Runnable> queuedMessages;

  public XmppFederationHostForDomain(final String domain,
                                     WaveXmppComponent component) {
    this.remoteDomain = domain;
    this.component = component;
    queuedMessages = new ArrayList<Runnable>();
    // start discovery.
    XmppDisco disco = component.getDisco();
    disco.discoverRemoteJid(remoteDomain, new RpcCallback<String>() {
      public void run(String targetJID) {
        if (targetJID != null) {
          discoCompleted(targetJID);
        } else {
          // TODO: invoke a callback to inform the wave server.
          logger.severe(remoteDomain + " does not appear to have wave");
        }
      }
    });
  }

  /**
   * Create a new message.
   *
   * @return the new message.
   */
  private Message newMessage() {
    Message message = new Message();
    message.setType(Message.Type.normal);
    message.setFrom(component.componentJID);
    message.setID(component.generateId());
    message
        .addChildElement("request", WaveXmppComponent.NAMESPACE_XMPP_RECEIPTS);
    return message;
  }

  /**
   * Called when XMPP discovery is complete. Sends queued messages.
   *
   * @param remoteJID the discovered remote JID for this domain
   */
  private void discoCompleted(String remoteJID) {
    this.remoteJID = remoteJID;
    this.discoCompleted = true;
    while (!queuedMessages.isEmpty()) {
      queuedMessages.remove(0).run();
    }
  }

  /**
   * Sends a wavelet update message on behalf of the wave server.
   *
   * @param waveletName      the wavelet name
   * @param deltaList        the deltas to include in the message
   * @param committedVersion the commit update to include in the message
   * @param callback         the callback to invoke upon confirmed message
   *                         delivery or failure
   */
  public void waveletUpdate(final WaveletName waveletName,
                            final List<ByteString> deltaList,
                            final common.ProtocolHashedVersion committedVersion,
                            final WaveletUpdateCallback callback) {
    if (!discoCompleted) {
      queuedMessages.add(new Runnable() {
        public void run() {
          waveletUpdate(waveletName, deltaList, committedVersion, callback);
        }
      });
      return;
    }

    if (remoteJID == null) {
      logger.warning("attempting to send an update to non-wavelet host"
                     + remoteDomain);
      // TODO: Somehow need to communicate back to the wave server to stop.
    }
    Message message = newMessage();
    message.setTo(remoteJID);
    final String encodedWaveletName;
    try {
      encodedWaveletName =
          WaveXmppComponent.waveletNameEncoder.waveletNameToURI(
              waveletName);
    } catch (URIEncoderDecoder.EncodingException e) {
      logger.severe("couldn't encode wavelet name " + waveletName);
      // TODO: signal back to the fed host
      return;
    }
    Element itemElement =
        message
            .addChildElement("event", WaveXmppComponent.NAMESPACE_PUBSUB_EVENT)
            .addElement("items")
            .addElement("item");
    for (ByteString delta : deltaList) {
      Element waveletUpdate =
          itemElement
              .addElement("wavelet-update",
                          WaveXmppComponent.NAMESPACE_WAVE_SERVER)
              .addAttribute("wavelet-name", encodedWaveletName);
      String encodedDelta =
          new String(Base64.encodeBase64(delta.toByteArray()));
      waveletUpdate.addElement("applied-delta")
          .addCDATA(encodedDelta);
    }
    if (committedVersion != null) {
      Element waveletUpdate =
          itemElement
              .addElement("wavelet-update",
                          WaveXmppComponent.NAMESPACE_WAVE_SERVER)
              .addAttribute("wavelet-name", encodedWaveletName);
      waveletUpdate.addElement("commit-notice")
          .addAttribute("version", Long.toString(committedVersion.getVersion()))
          .addAttribute("history-hash",
                        Base64Util.encode(committedVersion.getHistoryHash()));
    }
    component.sendPacket(message, /* retry */true, /* no callback */null);
  }
}

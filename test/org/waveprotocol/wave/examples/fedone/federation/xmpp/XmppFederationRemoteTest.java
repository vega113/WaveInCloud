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

import junit.framework.TestCase;

import org.apache.commons.codec.binary.Base64;
import org.dom4j.Element;
import static org.easymock.classextension.EasyMock.createMock;
import static org.easymock.classextension.EasyMock.expect;
import static org.easymock.classextension.EasyMock.replay;
import static org.easymock.classextension.EasyMock.verify;
import org.waveprotocol.wave.examples.fedone.waveserver.SubmitResultListener;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveletFederationListener;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveletFederationProvider;
import org.xmpp.packet.Message;

/**
 * Tests for the XMPPFederationRemote class.
 *
 *
 */
public class XmppFederationRemoteTest extends TestCase {

  private XmppTestUtil.MockWaveXmppComponent mockComponent;
  private WaveletFederationListener.Factory mockUpdateListenerFactory;
  private XmppTestUtil.MockDisco mockDisco;

  private XmppFederationRemote fedRemote;
  private WaveletFederationProvider.HistoryResponseListener mockHistoryListener;
  private SubmitResultListener mockSubmitListener;
  private XmppTestUtil.MockWaveletListener mockUpdateListener;

  private static final String EXPECTED_RECEIPT_MESSAGE =
      "\n<message id=\"1-1\" from=\"wave.initech-corp.com\""
      + " to=\"wave.acmewave.com\">\n"
      + "  <received xmlns=\"urn:xmpp:receipts\"/>\n"
      + "</message>";

  @Override
  public void setUp() {
    mockUpdateListenerFactory =
        createMock(WaveletFederationListener.Factory.class);
    mockHistoryListener =
        createMock(WaveletFederationProvider.HistoryResponseListener.class);
    mockSubmitListener = createMock(SubmitResultListener.class);

    mockDisco = new XmppTestUtil.MockDisco();
    mockDisco.setRemoteJID(XmppTestUtil.TEST_REMOTE_WAVE_JID);

    // This is just a dummy needed for the WaveXmppComponent.
    XmppFederationRemote mockRemote = createMock(XmppFederationRemote.class);
    XmppFederationHost mockHost = createMock(XmppFederationHost.class);

    mockComponent = new XmppTestUtil.MockWaveXmppComponent("", "", "", 0,
                                                           null, mockHost,
                                                           mockRemote,
                                                           mockDisco);
  }

  /**
   * Tests that the constructor behaves as expected.
   */
  public void testConstructor() {
    replayMocks();
    createRemote();
    assertEquals(0, mockComponent.packetsSent);
    assertEquals(false, mockDisco.discoStarted);
  }

  private void createRemote() {
    fedRemote = new XmppFederationRemote(mockUpdateListenerFactory);
    fedRemote.setComponent(mockComponent);
  }

  /**
   * Tests that a submit request from a local wave server is sent out to the
   * foreign federation host, and that the response from it is passed back to
   * the wave server.
   */
  public void testSubmitRequest() {
    // TODO: implement
  }

  /**
   * Tests that a submit request waits for disco to complete.
   */
  public void testSubmitRequestWaitsForDisco() {
    // TODO: implement
  }

  /**
   * Tests that a submit request doesn't fall over if disco fails, but instead
   * passes an error back to the wave server.
   */
  public void testSubmitRequestDiscoFailed() {
    // TODO: implement
  }

  /**
   * Tests that a history request from a local wave server is sent out to the
   * foreign federation host, and that the response from it is passed back to
   * the wave server.
   */
  public void testHistoryRequest() {
    // TODO: implement
  }

  /**
   * Tests that a submit request waits for disco to complete.
   */
  public void testHistoryRequestWaitsForDisco() {
    // TODO: implement
  }

  /**
   * Tests that a submit request doesn't fall over if disco fails, but instead
   * passes an error back to the wave server.
   */
  public void testHistoryRequestDiscoFailed() {
    // TODO: implement
  }

  public void testGetSigner() {
    // TODO: implement
  }

  public void testPostSigner() {
    // TODO: implement
  }

  /**
   * Tests an update message from a foreign federation host is correctly decoded
   * and passed to the Update Listener Factory, and a response is sent.
   *
   * @throws Exception should not be thrown
   */
  public void testUpdate() throws Exception {
    Message updateMessage = new Message();
    Element waveletUpdate = addWaveletUpdate(updateMessage);
    waveletUpdate.addAttribute("wavelet-name",
                               WaveXmppComponent.waveletNameEncoder.waveletNameToURI(
                                   XmppTestUtil.waveletName));
    addUpdateToWaveletDelta(waveletUpdate);
    createRemote();
    mockUpdateListener = new XmppTestUtil.MockWaveletListener();
    expect(mockUpdateListenerFactory.listenerForDomain("acmewave.com"))
        .andReturn(mockUpdateListener);
    replayMocks();
    fedRemote.update(updateMessage);
    verifyMocks();
    assertEquals(XmppTestUtil.waveletName,
                 mockUpdateListener.savedUpdateWaveletName);
    mockUpdateListener.savedCallback.onSuccess();
    assertEquals(1, mockComponent.packetsSent);
    assertEquals(EXPECTED_RECEIPT_MESSAGE,
                 mockComponent.lastPacketSent.toString());
  }

  private Element addWaveletUpdate(Message updateMessage) throws Exception {
    updateMessage.setFrom(XmppTestUtil.TEST_REMOTE_WAVE_JID);
    updateMessage.setTo(XmppTestUtil.TEST_LOCAL_JID);
    updateMessage.setID(mockComponent.generateId());
    updateMessage
        .addChildElement("request", WaveXmppComponent.NAMESPACE_XMPP_RECEIPTS);
    Element event =
        updateMessage
            .addChildElement("event", WaveXmppComponent.NAMESPACE_PUBSUB_EVENT);
    Element waveletUpdate =
        event.addElement("items").addElement("item")
            .addElement("wavelet-update");
    waveletUpdate.addAttribute("wavelet-name",
                               WaveXmppComponent.waveletNameEncoder.waveletNameToURI(
                                   XmppTestUtil.waveletName));
    return waveletUpdate;
  }

  private void addCommitToWaveletUpdate(Element waveletUpdate)
      throws Exception {
    waveletUpdate.addElement("commit-notice")
        .addAttribute("version", String.valueOf(XmppTestUtil.TEST_VERSION))
        .addAttribute("history-hash", XmppTestUtil.TEST_HISTORY_HASH)
        .addAttribute("wavelet-name",
                      WaveXmppComponent.waveletNameEncoder.waveletNameToURI(
                          XmppTestUtil.waveletName));
  }

  private void addUpdateToWaveletDelta(Element waveletUpdate) {
    String encodedDelta =
        new String(Base64.encodeBase64(
            XmppTestUtil.createTestAppliedWaveletDelta().toByteArray()));
    waveletUpdate.addElement("applied-delta").addCDATA(encodedDelta);
  }

  private void replayMocks() {
    replay(mockUpdateListenerFactory);
    replay(mockHistoryListener);
    replay(mockSubmitListener);
  }

  private void verifyMocks() {
    verify(mockUpdateListenerFactory);
    verify(mockHistoryListener);
    verify(mockSubmitListener);
  }
}

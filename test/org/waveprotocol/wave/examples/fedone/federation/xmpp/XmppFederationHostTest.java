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

import static org.easymock.EasyMock.isA;
import static org.easymock.classextension.EasyMock.createMock;
import static org.easymock.classextension.EasyMock.replay;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;

import junit.framework.TestCase;

import org.apache.commons.codec.binary.Base64;
import org.dom4j.Element;
import org.waveprotocol.wave.examples.fedone.federation.xmpp.XmppTestUtil.MockDisco;
import org.waveprotocol.wave.examples.fedone.federation.xmpp.XmppTestUtil.MockWaveXmppComponent;
import org.waveprotocol.wave.protocol.common;
import org.xmpp.packet.IQ;

import java.util.List;


/**
 *
 */
public class XmppFederationHostTest extends TestCase {

  private XmppTestUtil.MockProvider mockProvider;
  private XmppFederationHost fedHost;
  private MockDisco mockDisco;
  private MockWaveXmppComponent mockComponent;

  @Override
  public void setUp() {
    mockProvider = new XmppTestUtil.MockProvider();

    mockDisco = new XmppTestUtil.MockDisco();
    mockDisco.setRemoteJID(XmppTestUtil.TEST_REMOTE_WAVE_JID);

    XmppFederationRemote mockRemote = createMock(XmppFederationRemote.class);
    // This is just a dummy needed for the WaveXmppComponent.
    XmppFederationHost mockHost = createMock(XmppFederationHost.class);

    mockHost.setComponent(isA(WaveXmppComponent.class));
    replay(mockHost);
    mockRemote.setComponent(isA(WaveXmppComponent.class));
    replay(mockRemote);
    mockComponent = new XmppTestUtil.MockWaveXmppComponent("", "", "", 0,
                                                           null, mockHost,
                                                           mockRemote,
                                                           mockDisco);

    fedHost = new XmppFederationHost(mockProvider);
    fedHost.setComponent(mockComponent);
  }

  /**
   * Tests we can construct a Federation Host, and that no network traffic
   * results.
   */
  public void testConstructor() {
    assertEquals(0, mockComponent.packetsSent);
    assertEquals(false, mockDisco.discoStarted);
  }

  /**
   * Tests receiving a history request from a foreign federation remote, passing
   * it to the wave server, and sending the response back.
   *
   * @throws Exception should not be thrown.
   */
  public void testHistoryRequest() throws Exception {
    IQ historyRequest = new IQ(IQ.Type.get);
    historyRequest.setID(mockComponent.generateId());
    historyRequest.setTo(XmppTestUtil.TEST_LOCAL_JID);
    historyRequest.setFrom(XmppTestUtil.TEST_REMOTE_WAVE_JID);
    Element deltaHistory = historyRequest.setChildElement("pubsub",
                                                          WaveXmppComponent.NAMESPACE_PUBSUB)
        .addElement("items")
        .addElement("delta-history");
    deltaHistory.addAttribute("xmlns", WaveXmppComponent.NAMESPACE_WAVE_SERVER);
    deltaHistory.addAttribute("start-version",
                              Long.toString(
                                  XmppTestUtil.TEST_START_VERSION.getVersion()));
    deltaHistory.addAttribute("start-version-hash", Base64Util.encode(
        XmppTestUtil.TEST_START_VERSION.getHistoryHash()));
    deltaHistory.addAttribute("end-version",
                              Long.toString(
                                  XmppTestUtil.TEST_END_VERSION.getVersion()));
    deltaHistory.addAttribute("end-version-hash", Base64Util.encode(
        XmppTestUtil.TEST_END_VERSION.getHistoryHash()));

    deltaHistory.addAttribute("response-length-limit",
                              String.valueOf(XmppTestUtil.TEST_LENGTH_LIMIT));
    deltaHistory.addAttribute("wavelet-name",
                              WaveXmppComponent.waveletNameEncoder.waveletNameToURI(
                                  XmppTestUtil.waveletName));

    fedHost.processHistoryRequest(historyRequest);
    assertEquals(XmppTestUtil.waveletName, mockProvider.savedWaveletName);
    assertEquals(XmppTestUtil.TEST_START_VERSION,
                 mockProvider.savedStartVersion);
    assertEquals(XmppTestUtil.TEST_END_VERSION, mockProvider.savedEndVersion);
    assertEquals(XmppTestUtil.TEST_LENGTH_LIMIT, mockProvider.savedLengthLimit);
    assertTrue(mockProvider.savedHistoryListener
        instanceof XmppFederationHost.HistoryResponsePacketListener);

    // Now trigger the response.
    List<ByteString> deltaSet = ImmutableList.of(XmppTestUtil.createTestAppliedWaveletDelta());

    mockProvider.savedHistoryListener
        .onSuccess(deltaSet, XmppTestUtil.TEST_COMMITTED, XmppTestUtil.TEST_TRUNCATED);

    assertEquals(1, mockComponent.packetsSent);

    final String packet = mockComponent.lastPacketSent.toString();
    XmppTestUtil
        .assertEqualsWithoutCData(XmppTestUtil.EXPECTED_HISTORY_RESPONSE,
                                  packet);
    XmppTestUtil
        .verifyTestAppliedWaveletDelta(XmppTestUtil.extractCData(packet));
  }

  /**
   * Tests receiving a history request from a foreign federation remote, passing
   * it to the wave server, and sending the error response back when the wave
   * server fails.
   */
  public void testHistoryRequestFailed() {
    // TODO: implement once we have error sending
  }

  /**
   * Tests receiving a submit request from a foreign federation remote, passing
   * it to the wave server, and sending the response back.
   *
   * @throws Exception should not be thrown
   */
  public void testSubmitRequest() throws Exception {
    IQ submitRequest = new IQ(IQ.Type.get);
    submitRequest.setID(mockComponent.generateId());
    submitRequest.setTo(XmppTestUtil.TEST_LOCAL_JID);
    submitRequest.setFrom(XmppTestUtil.TEST_REMOTE_WAVE_JID);
    Element publishElement = submitRequest.setChildElement("pubsub",
                                                           WaveXmppComponent.NAMESPACE_PUBSUB)
        .addElement("publish");
    publishElement.addAttribute("node", "wavelet");
    Element submit =
        publishElement.addElement("item").addElement("submit-request");
    submit.addAttribute("xmlns", WaveXmppComponent.NAMESPACE_WAVE_SERVER);
    Element delta = submit.addElement("delta");
    delta.addAttribute("wavelet-name",
                       WaveXmppComponent.waveletNameEncoder.waveletNameToURI(
                           XmppTestUtil.waveletName));
    final common.ProtocolSignedDelta deltaPB =
        XmppTestUtil.createTestSignedDelta();
    delta.addCDATA(new String(Base64.encodeBase64(deltaPB.toByteArray())));
    fedHost.processSubmitRequest(submitRequest);
    assertEquals(XmppTestUtil.waveletName, mockProvider.savedWaveletName);
    assertTrue(deltaPB.equals(mockProvider.savedDelta));

    // Now trigger the callback
    mockProvider.savedSubmitListener.onSuccess(XmppTestUtil.TEST_OPERATIONS,
                                               XmppTestUtil.createTestHistoryHashVersion(),
                                               XmppTestUtil.TEST_TIMESTAMP);

    assertEquals(1, mockComponent.packetsSent);
    assertEquals(XmppTestUtil.EXPECTED_SUBMIT_RESPONSE,
                 mockComponent.lastPacketSent.toString());

  }

  /**
   * Tests receiving a submit request from a foreign federation remote, passing
   * it to the wave server, and sending the error response back when the wave
   * server fails.
   */
  public void testSubmitRequestFailed() {
    // TODO: implement once we have error sending
  }

  /**
   * Tests receiving a getSignerInfo request from a foreign federation remote,
   * passing it to the waveserver and sending the response back.
   */
  public void testGetSignerInfo() {
    // TODO: implement
  }

  public void testGetSignerInfoFailed() {
    // TODO: implement
  }

  public void testPostSigner() {
    // TODO: implement
  }

  public void testPostSignerFailed() {
    // TODO: implement
  }
}

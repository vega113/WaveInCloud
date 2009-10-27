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

import com.google.protobuf.RpcCallback;

import junit.framework.TestCase;

import org.dom4j.Element;
import org.dom4j.tree.DefaultElement;
import static org.easymock.classextension.EasyMock.createMock;
import static org.easymock.classextension.EasyMock.eq;
import static org.easymock.classextension.EasyMock.expect;
import static org.easymock.classextension.EasyMock.isA;
import static org.easymock.classextension.EasyMock.replay;
import static org.easymock.classextension.EasyMock.verify;
import org.jivesoftware.whack.ExternalComponentManager;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveletFederationListener;
import org.xmpp.component.ComponentException;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;

/**
 * Tests for the WaveXmppComponent class.
 *
 *
 */
public class WaveXmppComponentTest extends TestCase {

  private static final String TEST_COMPONENT_NAME = "wave";
  private static final int TEST_PORT = 1234;
  private static final String TEST_SECRET = "ssshsekrit";
  private static final String TEST_SERVER_IP = "127.0.0.1";
  private ExternalComponentManager mockComponentManager;
  private XmppDisco mockDisco;
  private XmppFederationHost mockHost;
  private WaveXmppComponent.ExternalComponentManagerFactory mockManagerFactory;
  private XmppFederationRemote mockRemote;
  private PacketCallback testCallback;
  private WaveXmppComponent xmppComponent;

  class PacketCallback implements RpcCallback<Packet> {

    public Packet responsePacket = null;

    public void run(Packet p) {
      this.responsePacket = p;
    }
  }

  @Override
  public void setUp() {

    mockHost = createMock(XmppFederationHost.class);
    mockRemote = createMock(XmppFederationRemote.class);
    mockDisco = createMock(XmppDisco.class);
    mockHost.setComponent(isA(WaveXmppComponent.class));
    mockRemote.setComponent(isA(WaveXmppComponent.class));
    mockDisco.setComponent(isA(WaveXmppComponent.class));
    mockComponentManager = createMock(ExternalComponentManager.class);
    mockComponentManager.setDefaultSecretKey(TEST_SECRET);
    mockComponentManager.setServerName(XmppTestUtil.TEST_LOCAL_DOMAIN);

    try {
      mockComponentManager
          .addComponent(eq(TEST_COMPONENT_NAME), isA(WaveXmppComponent.class));
    } catch (ComponentException e) {
      fail("mocks should never throw");
    }
    mockManagerFactory =
        createMock(WaveXmppComponent.ExternalComponentManagerFactory.class);
    expect(mockManagerFactory.connect(TEST_SERVER_IP, TEST_PORT))
        .andReturn(mockComponentManager);
    testCallback = new PacketCallback();
  }

  /**
   * Tests the component initializes the other classes correctly.
   *
   * @throws Exception should not be thrown
   */
  public void testConstructor() throws Exception {
    replayMocks();
    createComponent();
    verifyMocks();
  }

  /**
   * Tests that shutting down the connection queues packets for later delivery.
   *
   * @throws Exception should not be thrown
   */
  public void testConnectAndDisconnect() throws Exception {
    replayMocks();
    createComponent();
    verifyMocks();
    XmppTestUtil.MockExternalComponentManager mockComponentManager2 =
        new XmppTestUtil.MockExternalComponentManager("");
    xmppComponent.componentManager = mockComponentManager2;
    xmppComponent.shutdown();
    xmppComponent.sendPacket(new IQ(), true, null, null);
    xmppComponent.sendPacket(new IQ(), true, null, null);
    xmppComponent.sendPacket(new IQ(), true, null, null);
    assertEquals(0, mockComponentManager2.packetsSent);
    xmppComponent.start();
    assertEquals(3, mockComponentManager2.packetsSent);
  }

  /**
   * Tests the copyRequestPacketFields method.
   *
   * @throws Exception should not be thrown
   */
  public void testCopyRequestPacketFields() throws Exception {
    replayMocks();
    createComponent();
    verifyMocks();
    Message request = new Message();
    Message response = new Message();
    request.setID(xmppComponent.generateId());
    request.setFrom(XmppTestUtil.TEST_LOCAL_JID);
    request.setTo(XmppTestUtil.TEST_REMOTE_WAVE_JID);
    WaveXmppComponent.copyRequestPacketFields(request, response);
    assertEquals(request.getID(), response.getID());
    assertEquals(request.getFrom(), response.getTo());
    assertEquals(request.getTo(), response.getFrom());
  }

  @SuppressWarnings("unchecked")
  public void testListenForDomain() throws Exception {
    //noinspection unchecked
    mockDisco.discoverRemoteJid(eq(XmppTestUtil.TEST_REMOTE_DOMAIN),
                                isA(RpcCallback.class));
    replayMocks();
    createComponent();
    WaveletFederationListener listener =
        xmppComponent.listenerForDomain(XmppTestUtil.TEST_REMOTE_DOMAIN);
    assertNotNull(listener);
    verifyMocks();

  }

  public void testProcessPacketDiscoInfoGet() throws Exception {
    IQ iq = new IQ(IQ.Type.get);
    mockDisco.processDiscoInfoGet(iq);
    iq.setChildElement("query", WaveXmppComponent.NAMESPACE_DISCO_INFO);
    runProcessPacket(iq);
  }

  public void testProcessPacketDiscoInfoResult() throws Exception {
    IQ iq = new IQ(IQ.Type.result);
    iq.setChildElement("query", WaveXmppComponent.NAMESPACE_DISCO_INFO);
    mockDisco.processDiscoInfoResult(iq);
    runProcessPacket(iq);
    // TODO: set up retry, make sure it's cancelled.
  }

  public void testProcessPacketDiscoItemsGet() throws Exception {
    IQ iq = new IQ(IQ.Type.get);
    iq.setChildElement("query", WaveXmppComponent.NAMESPACE_DISCO_ITEMS);
    mockDisco.processDiscoItemsGet(iq);
    runProcessPacket(iq);
  }

  public void testProcessPacketDiscoItemsResult() throws Exception {
    IQ iq = new IQ(IQ.Type.result);
    iq.setChildElement("query", WaveXmppComponent.NAMESPACE_DISCO_ITEMS);
    mockDisco.processDiscoItemsResult(iq);
    runProcessPacket(iq);
    // TODO: set up retry, make sure it's cancelled.
  }

  public void testProcessPacketHistoryRequest() throws Exception {
    IQ iq = new IQ(IQ.Type.get);
    iq.setChildElement("pubsub", WaveXmppComponent.NAMESPACE_PUBSUB)
        .addElement("items").addAttribute("node", "wavelet");
    mockHost.processHistoryRequest(iq);
    runProcessPacket(iq);
  }

  public void testProcessPacketHistoryResponse() throws Exception {
    runProcessResponseWithCallback(IQ.Type.get, "pubsub",
                                   WaveXmppComponent.NAMESPACE_PUBSUB,
                                   new DefaultElement("items"));
  }

  public void testProcessPacketSubmitRequest() throws Exception {
    IQ iq = new IQ(IQ.Type.set);
    iq.setChildElement("pubsub", WaveXmppComponent.NAMESPACE_PUBSUB)
        .addElement("publish").addAttribute("node", "wavelet");
    mockHost.processSubmitRequest(iq);
    runProcessPacket(iq);
  }

  public void testProcessPacketSubmitResponse() throws Exception {
    Element body = new DefaultElement("publish");
    body.addElement("item").addElement("submit-response",
                                       WaveXmppComponent.NAMESPACE_WAVE_SERVER);
    runProcessResponseWithCallback(IQ.Type.set, "pubsub",
                                   WaveXmppComponent.NAMESPACE_PUBSUB,
                                   body);
  }

  /**
   * Sends a IQ message of the given type, then a matching response, and checks
   * that the callback gets run.
   *
   * @param requestType          IQ type of request
   * @param element              request/response element in IQ.
   * @param namespace            namespace of request/response element.
   * @param responseChildElement child element of response
   * @throws Exception should not be thrown
   */
  private void runProcessResponseWithCallback(IQ.Type requestType,
                                              String element,
                                              String namespace,
                                              Element responseChildElement
  ) throws Exception {
    IQ testRequest = new IQ(requestType);
    IQ testResponse = new IQ(IQ.Type.result);
    testRequest.setTo(XmppTestUtil.TEST_REMOTE_WAVE_JID);
    testRequest.setFrom(XmppTestUtil.TEST_LOCAL_JID);
    testRequest.setChildElement(element, namespace);
    testResponse.setChildElement(element, namespace)
        .add(responseChildElement);
    mockComponentManager
        .sendPacket(isA(WaveXmppComponent.class), isA(IQ.class));
    replayMocks();
    createComponent();
    testRequest.setID(xmppComponent.generateId());
    xmppComponent.sendPacket(testRequest, true /* retry */, testCallback, null);
    WaveXmppComponent.copyRequestPacketFields(testRequest, testResponse);
    xmppComponent.processPacket(testResponse);
    verifyMocks();
    // and the response should have come through to the callback.
    assertEquals(testResponse, testCallback.responsePacket);
  }

  public void testProcessPacketUpdateRequest() throws Exception {
    Message message = new Message();
    message
        .addChildElement("event", WaveXmppComponent.NAMESPACE_PUBSUB_EVENT)
        .addElement("items");
    mockRemote.update(message);
    runProcessPacket(message);
  }

  public void testProcessPacketUpdateResponse() throws Exception {
    Message message = new Message();
    message
        .addChildElement("received", WaveXmppComponent.NAMESPACE_XMPP_RECEIPTS);
    message
        .addChildElement("publish", WaveXmppComponent.NAMESPACE_PUBSUB_EVENT);
    runProcessPacket(message);
    // TODO: set up retry, make sure it's cancelled.
  }

  public void testSendErrorResponse() {
    // TODO: write error sending code.
  }


  /**
   * Creates the component, and hooks up the mock component manager.
   *
   * @throws ComponentException should not be thrown
   */
  private void createComponent() throws ComponentException {
    xmppComponent = new WaveXmppComponent(TEST_COMPONENT_NAME,
                                          XmppTestUtil.TEST_LOCAL_DOMAIN,
                                          TEST_SECRET,
                                          WaveXmppComponentTest.TEST_SERVER_IP,
                                          TEST_PORT,
                                          "", mockManagerFactory, mockHost,
                                          mockRemote, mockDisco);
    xmppComponent.run();
    xmppComponent
        .initialize(new JID(XmppTestUtil.TEST_LOCAL_JID), mockComponentManager);
    xmppComponent.start();
  }

  /**
   * Puts all mocks into replay state.
   */
  private void replayMocks() {
    replay(mockManagerFactory);
    replay(mockComponentManager);
    replay(mockDisco);
    replay(mockHost);
    replay(mockRemote);
  }

  /**
   * Runs a processPacket test.
   *
   * @param packet the packet to be passed to processPacket.
   * @throws Exception should not be thrown
   */
  private void runProcessPacket(Packet packet) throws Exception {
    replayMocks();
    createComponent();
    xmppComponent.processPacket(packet);
    verifyMocks();
  }

  /**
   * Verifies all mocks.
   */
  private void verifyMocks() {
    verify(mockManagerFactory);
    verify(mockComponentManager);
    verify(mockDisco);
    verify(mockHost);
    verify(mockRemote);
  }


}

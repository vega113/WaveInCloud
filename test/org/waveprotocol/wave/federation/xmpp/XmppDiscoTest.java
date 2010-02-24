/**
 * Copyright 2010 Google Inc.
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

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.google.common.collect.Lists;

import junit.framework.TestCase;

import org.dom4j.Element;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Packet;
import org.xmpp.packet.PacketError;

import java.util.List;

/**
 * Tests for the {@link XmppDisco} class. Also provides coverage over
 * {@link RemoteDisco} which is used internally by XmppDisco.
 */
public class XmppDiscoTest extends TestCase {
  private static final String LOCAL_DOMAIN = "something.com";
  private static final String LOCAL_JID = "wave." + LOCAL_DOMAIN;
  private static final String REMOTE_DOMAIN = "other.com";
  private static final String REMOTE_JID = "wave." + REMOTE_DOMAIN;

  private static final String DISCO_ITEMS_ID = "disco-items";
  private static final String DISCO_INFO_ID = "disco-info";
  private static final String SERVER_DESCRIPTION = "Google Wave Server";

  // The following JID is intentionally non-Wave.
  private static final String REMOTE_PUBSUB_JID = "pubsub." + REMOTE_DOMAIN;

  private static final String EXPECTED_DISCO_ITEMS_GET =
      "\n<iq type=\"get\" id=\"" + DISCO_ITEMS_ID + "\" to=\"" + REMOTE_DOMAIN + "\" "
      + "from=\"" + LOCAL_JID + "\">\n"
      + "  <query xmlns=\"http://jabber.org/protocol/disco#items\"/>\n"
      + "</iq>";

  private static final String EXPECTED_DISCO_INFO_GET =
      "\n<iq type=\"get\" id=\"" + DISCO_INFO_ID + "\" to=\"" + REMOTE_JID + "\" "
      + "from=\"" + LOCAL_JID + "\">\n"
      + "  <query xmlns=\"http://jabber.org/protocol/disco#info\"/>\n"
      + "</iq>";

  private static final String EXPECTED_DISCO_INFO_GET_PUBSUB =
      "\n<iq type=\"get\" id=\"" + DISCO_INFO_ID + "\" to=\"" + REMOTE_PUBSUB_JID + "\" "
      + "from=\"" + LOCAL_JID + "\">\n"
      + "  <query xmlns=\"http://jabber.org/protocol/disco#info\"/>\n"
      + "</iq>";

  private static final String EXPECTED_DISCO_ITEMS_RESULT =
    "\n<iq type=\"result\" id=\"" + DISCO_ITEMS_ID + "\" from=\"" + LOCAL_JID + "\" "
    + "to=\"" + REMOTE_JID + "\">\n"
    + "  <query xmlns=\"http://jabber.org/protocol/disco#items\"/>\n"
    + "</iq>";

  private static final String EXPECTED_DISCO_INFO_RESULT =
    "\n<iq type=\"result\" id=\""+ DISCO_INFO_ID + "\" from=\"" + LOCAL_JID + "\" "
    + "to=\"" + REMOTE_JID + "\">\n"
    + "  <query xmlns=\"http://jabber.org/protocol/disco#info\">\n"
    + "    <identity category=\"collaboration\" type=\"google-wave\" "
    + "name=\"" + SERVER_DESCRIPTION + "\"/>\n"
    + "    <feature var=\"http://waveprotocol.org/protocol/0.2/waveserver\"/>\n"
    + "  </query>\n"
    + "</iq>";

  private MockOutgoingPacketTransport transport;
  private XmppManager manager;
  private XmppDisco disco;

  // Explicitly mocked out disco callback usable by individual tests.
  private SuccessFailCallback<String, String> discoCallback;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    disco = new XmppDisco(SERVER_DESCRIPTION);
    transport = new MockOutgoingPacketTransport();
    manager = new XmppManager(mock(XmppFederationHost.class), mock(XmppFederationRemote.class),
        disco, transport, LOCAL_JID);
    disco.setManager(manager);

    discoCallback = mockDiscoCallback();
  }

  @SuppressWarnings("unchecked")
  private SuccessFailCallback<String, String> mockDiscoCallback() {
    return mock(SuccessFailCallback.class);
  }

  /**
   * Tests that starting disco sends a disco#items to the remote server.
   */
  public void testDiscoStart() {
    XmppUtil.fakeUniqueId = DISCO_ITEMS_ID;
    disco.discoverRemoteJid(REMOTE_DOMAIN, discoCallback);
    assertEquals(1, transport.packets.size());
    Packet packet = transport.packets.poll();
    assertEquals(REMOTE_DOMAIN, packet.getTo().toString());
    assertEquals(LOCAL_JID, packet.getFrom().toString());
    assertEquals(EXPECTED_DISCO_ITEMS_GET, packet.toString());
  }

  /**
   * Tests that starting disco sends a disco#items to the remote server, and subsequent
   * disco requests are not sent until there is a retransmit timeout.  Also test that the callback
   * is run even after timing out.
   */
  public void testDiscoRetransmitsOnNoReply() {
    int expectedFailures = 0;
    int expectedPackets = 0;

    disco.discoverRemoteJid(REMOTE_DOMAIN, discoCallback);
    expectedFailures++;
    expectedPackets++;
    assertEquals("Should have sent disco packet", expectedPackets, transport.packetsSent);

    for (int i = 1; i < RemoteDisco.MAXIMUM_DISCO_ATTEMPTS; i++) {
      manager.causeImmediateTimeout(transport.packets.remove());
      expectedPackets++;
      assertEquals("Should have retried", expectedPackets, transport.packetsSent);

      disco.discoverRemoteJid(REMOTE_DOMAIN, discoCallback);
      disco.discoverRemoteJid(REMOTE_DOMAIN, discoCallback);
      expectedFailures += 2;
      assertEquals("Should not have sent more outgoing packets",
          expectedPackets, transport.packetsSent);

      // Should be no activity on the callback
      verifyZeroInteractions(discoCallback);
    }

    // This final timeout should cause all callbacks to be invoked.
    manager.causeImmediateTimeout(transport.packets.remove());
    verify(discoCallback, times(expectedFailures)).onFailure(anyString());
    verify(discoCallback, never()).onSuccess(anyString());

    // The next request should return a cached response.
    SuccessFailCallback<String, String> cachedDiscoCallback = mockDiscoCallback();
    disco.discoverRemoteJid(REMOTE_DOMAIN, cachedDiscoCallback);
    verify(cachedDiscoCallback).onFailure(anyString());
    verify(cachedDiscoCallback, never()).onSuccess(anyString());

    // No more outgoing packets.
    assertEquals("Should not have sent more outgoing packets",
        expectedPackets, transport.packetsSent);
  }

  /**
   * Tests that starting disco sends a disco#items to the remote server, and no
   * subsequent disco requests start after we get a successful reply.
   */
  public void testDiscoNoRetransmitsAfterReply() {
    XmppUtil.fakeUniqueId = DISCO_ITEMS_ID;
    disco.discoverRemoteJid(REMOTE_DOMAIN, discoCallback);
    assertEquals("Expected disco packet to be sent", 1, transport.packetsSent);
    Packet packet = transport.lastPacketSent;
    assertEquals(EXPECTED_DISCO_ITEMS_GET, packet.toString());
    assertTrue(disco.isDiscoRequestPending(REMOTE_DOMAIN));

    IQ discoItemsResult = createDiscoItems(true /* wave */, false /* not pubsub */);
    discoItemsResult.setID(packet.getID());
    XmppUtil.fakeUniqueId = DISCO_INFO_ID;
    manager.receivePacket(discoItemsResult);
    assertEquals("Expected disco info get to be sent", 2, transport.packetsSent);
    assertEquals(EXPECTED_DISCO_INFO_GET, transport.lastPacketSent.toString());

    // Check that we haven't yet finished - we should only get up to sending the items request.
    verifyZeroInteractions(discoCallback);
    assertTrue(disco.isDiscoRequestPending(REMOTE_DOMAIN));
  }

  /**
   * Tests stage 2 of disco. Inject a disco#items into the disco code, check it
   * calls disco#info on the JID.
   */
  public void testDiscoItemsResult() {
    initiateDiscoRequest();  // sends one packet.
    // create with wave, no pubsub
    IQ discoItemsResult = createDiscoItems(true /* wave */, false /* not pubsub */);

    XmppUtil.fakeUniqueId = DISCO_INFO_ID;
    manager.receivePacket(discoItemsResult);
    assertEquals(2, transport.packetsSent);
    Packet packet = transport.lastPacketSent;
    assertEquals(REMOTE_JID, packet.getTo().toString());
    assertEquals(LOCAL_JID, packet.getFrom().toString());
    assertEquals(EXPECTED_DISCO_INFO_GET, packet.toString());
  }

  /**
   * Tests stage 3 of disco. Inject a disco#info into the disco code (one that
   * matches wave) and check the callback gets run.
   */
  public void testDiscoInfoResultWave() {
    initiateDiscoRequest();  // sends one packet.
    // create with wave, no pubsub
    IQ discoItemsResult = createDiscoItems(true /* wave */, false /* not pubsub */);
    // Start the process.
    XmppUtil.fakeUniqueId = DISCO_INFO_ID;
    manager.receivePacket(discoItemsResult);
    assertEquals(2, transport.packetsSent);
    Packet packet = transport.lastPacketSent;
    assertEquals(EXPECTED_DISCO_INFO_GET, packet.toString());
    // create a wave disco result, inject into disco.
    manager.receivePacket(createDiscoInfo(true /* wave */));
    assertEquals(2, transport.packetsSent);
    verify(discoCallback).onSuccess(eq(REMOTE_JID));
  }

  /**
   * Tests stage 3 of disco. Inject a disco#info into the disco code (one that
   * doesn't match wave) and check callback gets run with null.
   */
  public void testDiscoInfoResultPubsub() {
    initiateDiscoRequest();  // sends one packet.
    transport.packets.remove(); // remove packet from queue

    // create with just pubsub
    IQ discoItemsResult = createDiscoItems(false /* not wave */, true /* pubsub */);
    XmppUtil.fakeUniqueId = DISCO_INFO_ID;
    manager.receivePacket(discoItemsResult);
    assertEquals(3, transport.packetsSent);

    // Expect a wave request even if we didn't send it (automatic wave request)
    Packet wavePacket = transport.packets.poll();
    assertEquals(EXPECTED_DISCO_INFO_GET, wavePacket.toString());

    // Expect pubsub packet
    Packet pubsubPacket = transport.packets.poll();
    assertEquals(EXPECTED_DISCO_INFO_GET_PUBSUB, pubsubPacket.toString());

    // Create pubsub response, should not yet invoke callback
    manager.receivePacket(createDiscoInfo(false /* not wave */));
    verifyZeroInteractions(discoCallback);

    // Create response to wave request, with ITEM_NOT_FOUND
    IQ failWaveResponse = IQ.createResultIQ((IQ) wavePacket);
    failWaveResponse.setError(PacketError.Condition.item_not_found);
    manager.receivePacket(failWaveResponse);
    verify(discoCallback).onFailure(anyString());

    // No more outgoing packets
    assertEquals(3, transport.packetsSent);
  }

  /**
   * Tests stage 3 of disco. Inject a disco#items into the disco code with
   * pubsub, then wave. Then give it pubsub's disco#info, and check it then
   * sends a disco#info for wave.
   */
  public void testDiscoInfoResultPubsubAndWave() {
    initiateDiscoRequest();  // sends one packet.
    transport.packets.remove(); // remove packet from queue

    // create with both pubsub and wave
    IQ discoItemsResult = createDiscoItems(true /* wave */, true /* pubsub */);
    XmppUtil.fakeUniqueId = DISCO_INFO_ID;
    manager.receivePacket(discoItemsResult);
    assertEquals(3, transport.packetsSent);

    // Expect a wave request
    Packet wavePacket = transport.packets.poll();
    assertEquals(EXPECTED_DISCO_INFO_GET, wavePacket.toString());

    // Expect pubsub packet
    Packet pubsubPacket = transport.packets.poll();
    assertEquals(EXPECTED_DISCO_INFO_GET_PUBSUB, pubsubPacket.toString());

    // Create pubsub response, should not yet invoke callback
    manager.receivePacket(createDiscoInfo(false /* not wave */));
    verifyZeroInteractions(discoCallback);

    // Create response to wave request, with ITEM_NOT_FOUND
    manager.receivePacket(createDiscoInfo(true /* wave */));
    verify(discoCallback).onSuccess(eq(REMOTE_JID));

    // No more outgoing packets
    assertEquals(3, transport.packetsSent);
  }

  /**
   * Tests that if disco is started for a remote server for which we already
   * have the result, the cached result is just passed to the callback.
   */
  public void testDiscoStartWithCachedResult() {
    disco.testInjectInDomainToJidMap(REMOTE_DOMAIN, REMOTE_JID);
    disco.discoverRemoteJid(REMOTE_DOMAIN, discoCallback);
    assertEquals(0, transport.packetsSent);
    verify(discoCallback).onSuccess(eq(REMOTE_JID));
  }

  /**
   * Tests that we return a (useless, empty) IQ for a disco#items.
   */
  public void testDiscoGetDiscoItems() {
    IQ request = createDiscoRequest(XmppNamespace.NAMESPACE_DISCO_ITEMS);
    manager.receivePacket(request);
    assertEquals(1, transport.packetsSent);
    Packet packet = transport.lastPacketSent;
    assertEquals(REMOTE_JID, packet.getTo().toString());
    assertEquals(LOCAL_JID, packet.getFrom().toString());
    assertEquals(EXPECTED_DISCO_ITEMS_RESULT, packet.toString());
  }

  /**
   * Tests that we return the right wave-identifying IQ for a disco#info.
   */
  public void testDiscoGetDiscoInfo() {
    IQ request = createDiscoRequest(XmppNamespace.NAMESPACE_DISCO_INFO);
    manager.receivePacket(request);
    assertEquals(1, transport.packetsSent);
    Packet packet = transport.lastPacketSent;
    assertEquals(REMOTE_JID, packet.getTo().toString());
    assertEquals(LOCAL_JID, packet.getFrom().toString());
    assertEquals(EXPECTED_DISCO_INFO_RESULT, packet.toString());
  }

  /**
   * Tests sending multiple disco requests result in multiple callbacks.
   */
  public void testMultipleDiscoRequestsToSameDomain() {
    final int CALL_COUNT = 10;
    XmppUtil.fakeUniqueId = DISCO_ITEMS_ID;
    List<SuccessFailCallback<String, String>> callbacks = Lists.newLinkedList();
    for (int i = 0; i < CALL_COUNT; i++) {
      @SuppressWarnings("unchecked")
      SuccessFailCallback<String, String> cb = mock(SuccessFailCallback.class);
      assertTrue(callbacks.add(cb));
      disco.discoverRemoteJid(REMOTE_DOMAIN, cb);
    }
    // Expect only one disco request to be sent.
    assertEquals(1, transport.packetsSent);
    Packet packet = transport.lastPacketSent;
    assertEquals(REMOTE_DOMAIN, packet.getTo().toString());
    assertEquals(LOCAL_JID, packet.getFrom().toString());
    assertEquals(EXPECTED_DISCO_ITEMS_GET, packet.toString());

    XmppUtil.fakeUniqueId = DISCO_INFO_ID;
    manager.receivePacket(createDiscoItems(true /* wave */, true /* pubsub */));
    manager.receivePacket(createDiscoInfo(true /* wave */));

    for(SuccessFailCallback<String, String> cb : callbacks) {
      verify(cb).onSuccess(eq(REMOTE_JID));
      verify(cb, never()).onFailure(anyString());
    }
  }

  /**
   * Tests that if a disco items requests fails due to some error, that we still
   * perform a disco info request on fallback JIDs.
   */
  public void testDiscoItemsFallback() {
    XmppUtil.fakeUniqueId = DISCO_INFO_ID;
    disco.discoverRemoteJid(REMOTE_DOMAIN, discoCallback);
    assertEquals("Should have sent disco packet", 1, transport.packetsSent);

    // Generate an error response.
    IQ errorResponse = IQ.createResultIQ((IQ) transport.packets.poll());
    errorResponse.setError(PacketError.Condition.conflict);
    manager.receivePacket(errorResponse);

    // Confirm that two outgoing packets are sent.
    assertEquals(3, transport.packetsSent);

    // Expect a wave request
    Packet wavePacket = transport.packets.poll();
    assertEquals(EXPECTED_DISCO_INFO_GET, wavePacket.toString());

    // Expect packet targeted at TLD
    Packet pubsubPacket = transport.packets.poll();
    assertEquals(REMOTE_DOMAIN, pubsubPacket.getTo().toBareJID());
  }

  /**
   * Create a disco#info result from the remote server.
   *
   * @param forWaveJID if true, it's for the remote Wave JID, else it's the
   *                   remote pubsub JID.
   * @return the new IQ packet.
   */
  private IQ createDiscoInfo(boolean forWaveJID) {
    IQ response = new IQ(IQ.Type.result);
    response.setTo(LOCAL_JID);
    response.setID(DISCO_INFO_ID);
    Element query = response.setChildElement("query", XmppNamespace.NAMESPACE_DISCO_INFO);

    if (forWaveJID) {
      response.setFrom(REMOTE_JID);
      query.addElement("identity")
          .addAttribute("category", XmppDisco.DISCO_INFO_CATEGORY)
          .addAttribute("type", XmppDisco.DISCO_INFO_TYPE)
          .addAttribute("name", SERVER_DESCRIPTION);
      query.addElement("feature")
          .addAttribute("var", XmppNamespace.NAMESPACE_WAVE_SERVER);
    } else {
      response.setFrom(REMOTE_PUBSUB_JID);
      query.addElement("identity")
          .addAttribute("category", "pubsub")
          .addAttribute("type", "whatever")
          .addAttribute("name", "not a wave server");
      query.addElement("feature")
          .addAttribute("var", XmppNamespace.NAMESPACE_PUBSUB);
    }

    return response;
  }

  /**
   * Create a disco#items result, with either or both of a pubsub and a wave
   * JID.
   *
   * @param wave   if true, create a wave JID item.
   * @param pubsub if true, create a pubsub JID item.
   * @return the new IQ packet.
   */
  private IQ createDiscoItems(boolean wave, boolean pubsub) {
    IQ discoItemsResult = new IQ(IQ.Type.result);
    discoItemsResult.setFrom(REMOTE_DOMAIN);
    discoItemsResult.setTo(LOCAL_JID);
    discoItemsResult.setID(DISCO_ITEMS_ID);
    Element discoBody =
        discoItemsResult.setChildElement("query", XmppNamespace.NAMESPACE_DISCO_ITEMS);
    if (wave) {
      discoBody.addElement("item").addAttribute("jid", REMOTE_JID);
    }
    if (pubsub) {
      discoBody.addElement("item").addAttribute("jid", REMOTE_PUBSUB_JID);
    }
    return discoItemsResult;
  }

  /**
   * Create a disco#info or disco#items query.
   *
   * @param namespace the namespace of the query - disco#info or disco#items
   * @return the new IQ packet
   */
  private IQ createDiscoRequest(String namespace) {
    IQ request = new IQ(IQ.Type.get);
    if (namespace.equals(XmppNamespace.NAMESPACE_DISCO_ITEMS)) {
      request.setID(DISCO_ITEMS_ID);
    } else if (namespace.equals(XmppNamespace.NAMESPACE_DISCO_INFO)) {
      request.setID(DISCO_INFO_ID);
    } else {
      throw new IllegalArgumentException();
    }
    request.setTo(LOCAL_JID);
    request.setFrom(REMOTE_JID);
    request.setChildElement("query", namespace);
    return request;
  }

  /**
   * Initiate a simple disco request to REMOTE_DOMAIN.
   */
  private void initiateDiscoRequest() {
    XmppUtil.fakeUniqueId = DISCO_ITEMS_ID;
    disco.discoverRemoteJid(REMOTE_DOMAIN, discoCallback);
    assertEquals("Disco packet should have been sent", 1, transport.packetsSent);
    Packet packet = transport.lastPacketSent;
    assertEquals(EXPECTED_DISCO_ITEMS_GET, packet.toString());
  }
}

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

import junit.framework.TestCase;

import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.createMock;
import static org.easymock.classextension.EasyMock.replay;
import static org.easymock.classextension.EasyMock.verify;

import org.waveprotocol.wave.examples.fedone.common.HashedVersion;
import org.waveprotocol.wave.examples.fedone.federation.xmpp.XmppTestUtil.MockWaveXmppComponent;
import org.waveprotocol.wave.protocol.common;
import org.xmpp.packet.Packet;

import java.util.Collections;
import java.util.List;


/**
 *
 */
public class XmppFederationHostForDomainTest extends TestCase {

  private XmppFederationHostForDomain fedHost;
  private WaveXmppComponent mockSimpleComponent;
  private XmppTestUtil.MockDisco mockDisco;
  private MockWaveXmppComponent mockComponent;

  private static final String EXPECTED_COMMIT_MESSAGE =
      "\n<message type=\"normal\" from=\"wave.initech-corp.com\" id=\"1-1\""
      + " to=\"wave.acmewave.com\">\n"
      + "  <request xmlns=\"urn:xmpp:receipts\"/>\n"
      + "  <event xmlns=\"http://jabber.org/protocol/pubsub#event\">\n"
      + "    <items>\n"
      + "      <item>\n"
      + "        <wavelet-update"
      + " xmlns=\"http://waveprotocol.org/protocol/0.2/waveserver\""
      + " wavelet-name=\"acmewave.com/initech-corp.com!a/b\">\n"
      + "          <commit-notice version=\"1234\" history-hash=\""
      + Base64Util.encode(HashedVersion.UNSIGNED_VERSION_0.getHistoryHash())
      + "\"/>\n"
      + "        </wavelet-update>\n"
      + "      </item>\n"
      + "    </items>\n"
      + "  </event>\n"
      + "</message>";

  private static final String EXPECTED_UPDATE_MESSAGE =
      "\n<message type=\"normal\" from=\"wave.initech-corp.com\" id=\"1-1\""
      + " to=\"wave.acmewave.com\">\n"
      + "  <request xmlns=\"urn:xmpp:receipts\"/>\n"
      + "  <event xmlns=\"http://jabber.org/protocol/pubsub#event\">\n"
      + "    <items>\n"
      + "      <item>\n"
      + "        <wavelet-update"
      + " xmlns=\"http://waveprotocol.org/protocol/0.2/waveserver\""
      + " wavelet-name=\"acmewave.com/initech-corp.com!a/b\">\n"
      + "          <applied-delta><![CDATA[ignored]]></applied-delta>\n"
      + "        </wavelet-update>\n"
      + "      </item>\n"
      + "    </items>\n"
      + "  </event>\n"
      + "</message>";

  private static final List<common.ProtocolAppliedWaveletDelta> NO_DELTAS =
      Collections.emptyList();

  private common.ProtocolHashedVersion hashedVersion =
      XmppTestUtil.createTestHistoryHashVersion();
  private common.ProtocolAppliedWaveletDelta appliedDelta =
      XmppTestUtil.createTestAppliedWaveletDelta();

  @Override
  public void setUp() {
    mockSimpleComponent = createMock(WaveXmppComponent.class);

    mockDisco = new XmppTestUtil.MockDisco();
    mockDisco.setRemoteJID(XmppTestUtil.TEST_REMOTE_WAVE_JID);

    XmppFederationRemote mockRemote = createMock(XmppFederationRemote.class);
    XmppFederationHost mockHost = createMock(XmppFederationHost.class);

    mockComponent = new XmppTestUtil.MockWaveXmppComponent("", "", "", 0,
                                                           null, mockHost,
                                                           mockRemote,
                                                           mockDisco);
    fedHost = new XmppFederationHostForDomain(XmppTestUtil.TEST_REMOTE_DOMAIN,
                                              mockComponent);
  }

  /**
   * Tests that the constructor correctly starts disco and saves the result
   */
  public void testConstructor() {
    expect(mockSimpleComponent.getDisco()).andReturn(mockDisco);
    replayMocks();
    fedHost = new XmppFederationHostForDomain(XmppTestUtil.TEST_REMOTE_DOMAIN,
                                              mockSimpleComponent);
    mockDisco.discoComplete();
    assertEquals(true, mockDisco.discoStarted);
    assertEquals(XmppTestUtil.TEST_REMOTE_DOMAIN, mockDisco.savedRemoteDomain);
    assertEquals(XmppTestUtil.TEST_REMOTE_WAVE_JID, fedHost.remoteJID);
    verifyMocks();
  }

  /**
   * Tests that commit sends a correctly formatted XMPP packet.
   *
   * @throws Exception should not be thrown
   */
  public void testCommit() throws Exception {
    replayMocks();
    mockDisco.discoComplete();
    commit();
    checkCommitMessage();
  }


  /**
   * Tests that the commit method waits for disco.
   *
   * @throws Exception should not be thrown
   */
  public void testCommitWaitsForDisco() throws Exception {
    replayMocks();
    commit();
    // No packets should have been sent yet.
    assertEquals(0, mockComponent.packetsSent);
    // Complete disco, the packet should be sent
    mockDisco.discoComplete();
    checkCommitMessage();
  }


  /**
   * Test we don't fall in a heap if disco fails.
   *
   * @throws Exception should not be thrown�
   */
  public void testCommitWithFailedDisco() throws Exception {
    mockDisco.setRemoteJID(null);
    replayMocks();
    mockDisco.discoComplete();
    commit();
    // No packets should be sent.
    assertEquals(0, mockComponent.packetsSent);
  }

  /**
   * Tests that update sends a correctly formatted XMPP packet.
   *
   * @throws Exception should not be thrown
   */
  public void testUpdate() throws Exception {
    replayMocks();
    mockDisco.discoComplete();
    update();
    checkUpdateMessage();
  }

  /**
   * Tests that the update method waits for disco to complete.
   *
   * @throws Exception should not be thrown
   */
  public void testUpdateWaitsForDisco() throws Exception {
    replayMocks();
    update();
    // No packets should have been sent yet.
    assertEquals(0, mockComponent.packetsSent);
    // Complete disco, the packet should be sent
    mockDisco.discoComplete();
    checkUpdateMessage();
  }

  /**
   * Test we don't fall in a heap if disco fails.
   *
   * @throws Exception should not be thrown
   */
  public void testUpdateWithFailedDisco() throws Exception {
    mockDisco.setRemoteJID(null);
    replayMocks();
    mockDisco.discoComplete();
    update();
    assertEquals(0, mockComponent.packetsSent);
  }


  private void commit() throws Exception {
    // TODO: null callback OK?
    fedHost.waveletUpdate(XmppTestUtil.waveletName, NO_DELTAS, hashedVersion,
                          null);
  }

  private void update() throws Exception {
    // TODO: null callback OK?
    fedHost.waveletUpdate(
        XmppTestUtil.waveletName, Lists.newArrayList(appliedDelta), null, null);
  }

  /**
   * Check the commit message is as expected.
   */
  private void checkCommitMessage() {
    assertEquals(1, mockComponent.packetsSent);
    Packet packet = mockComponent.lastPacketSent;
    assertEquals(XmppTestUtil.TEST_REMOTE_WAVE_JID, packet.getTo().toString());
    assertEquals(XmppTestUtil.TEST_LOCAL_JID, packet.getFrom().toString());
    assertEquals(EXPECTED_COMMIT_MESSAGE, packet.toString());
  }

  /**
   * Checks the update message is as expected.
   */
  private void checkUpdateMessage() {
    assertEquals(1, mockComponent.packetsSent);
    Packet packet = mockComponent.lastPacketSent;
    assertEquals(XmppTestUtil.TEST_REMOTE_WAVE_JID, packet.getTo().toString());
    assertEquals(XmppTestUtil.TEST_LOCAL_JID, packet.getFrom().toString());
    XmppTestUtil
        .assertEqualsWithoutCData(EXPECTED_UPDATE_MESSAGE, packet.toString());
    XmppTestUtil.verifyTestAppliedWaveletDelta(
        XmppTestUtil.extractCData(packet.toString()));
  }

  private void replayMocks() {
    replay(mockSimpleComponent);
  }

  private void verifyMocks() {
    verify(mockSimpleComponent);
  }
}

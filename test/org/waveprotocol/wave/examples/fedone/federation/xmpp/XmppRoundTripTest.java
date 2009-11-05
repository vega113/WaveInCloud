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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;

import junit.framework.TestCase;

import org.waveprotocol.wave.examples.fedone.waveserver.SubmitResultListener;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveletFederationProvider;
import org.waveprotocol.wave.protocol.common;
import org.xmpp.packet.Packet;

import java.util.List;

/**
 * Tests roundtripping packets from Federation Host to Remote and viceversa.
 *
 *
 */
public class XmppRoundTripTest extends TestCase {

  /**
   * Receives the submitRequest request (fed host)
   */
  private XmppTestUtil.MockProvider mockProvider;
  /**
   * Receives the results of a submitRequest/requestHistory response (fed
   * remote)
   */
  private XmppTestUtil.MockSubmitResultListener mockSubmitResultListener;
  private XmppTestUtil.MockHistoryResponseListener mockHistoryResultLister;
  private XmppTestUtil.MockDeltaSignerResponseListener mockDeltaSignerResponseListener;
  private XmppTestUtil.MockPostSignerResponseListener mockPostSignerListener;

  private XmppTestUtil.MockWaveletListener mockRemoteListener;

  private XmppTestUtil.MockWaveXmppComponent mockComponent;
  private XmppTestUtil.MockDisco mockDisco;

  private XmppFederationRemote fedRemote;
  private XmppFederationHostForDomain fedHostForDomain;

  // canned protobuffers.
  private common.ProtocolSignedDelta signedDelta;
  private common.ProtocolHashedVersion hashedVersion;
  private ByteString appliedDelta;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    mockDisco = new XmppTestUtil.MockDisco();
    mockDisco.setRemoteJID(XmppTestUtil.TEST_REMOTE_WAVE_JID);
    mockProvider = new XmppTestUtil.MockProvider();
    XmppTestUtil.MockWaveletListenerFactory mockListenerFactory =
        new XmppTestUtil.MockWaveletListenerFactory();
    mockRemoteListener = new XmppTestUtil.MockWaveletListener();
    mockListenerFactory.mockWaveletListener = mockRemoteListener;

    fedRemote = new XmppFederationRemote(mockListenerFactory);
    XmppFederationHost fedHost = new XmppFederationHost(mockProvider);

    mockComponent =
        new XmppTestUtil.MockWaveXmppComponent("", "", "", 0, null, fedHost,
                                               fedRemote, mockDisco);
    fedHostForDomain =
        new XmppFederationHostForDomain(XmppTestUtil.TEST_REMOTE_DOMAIN,
                                        mockComponent);

    mockSubmitResultListener =
        new XmppTestUtil.MockSubmitResultListener();
    mockHistoryResultLister = new XmppTestUtil.MockHistoryResponseListener();
    mockDeltaSignerResponseListener =
        new XmppTestUtil.MockDeltaSignerResponseListener();
    mockPostSignerListener = new XmppTestUtil.MockPostSignerResponseListener();
    hashedVersion = XmppTestUtil.createTestHistoryHashVersion();
    signedDelta = XmppTestUtil.createTestSignedDelta();
    appliedDelta = XmppTestUtil.createTestAppliedWaveletDelta();

  }

  /**
   * Starting with a wave server remote, make a submit request. Check it is sent
   * to the foreign federation host by the XMPP remote code. Decode the packet
   * with the XMPP host code. Check it is passed as a submit request to the wave
   * server host. Send a response back from the wave server host. Check it is
   * sent to the foreign federation remote by the XMPP host code. Decode the
   * submit response packet with the XMPP remote code. Check it is passed to the
   * wave server remote.
   */
  public void testSubmitRequest() {
    fedRemote
        .submitRequest(XmppTestUtil.waveletName, signedDelta,
                       mockSubmitResultListener);
    mockDisco.discoComplete();
    assertEquals(1, mockComponent.packetsSent);
    final Packet request = mockComponent.lastPacketSent;
    // TODO: check to and from are correct.

    // inject the request into the federation host, via the component
    mockComponent.processPacket(request);

    assertEquals(XmppTestUtil.waveletName, mockProvider.savedWaveletName);
    assertEquals(signedDelta, mockProvider.savedDelta);
    final SubmitResultListener hostListener = mockProvider.savedSubmitListener;

    // send the response back
    hostListener.onSuccess(XmppTestUtil.TEST_OPERATIONS,
                           hashedVersion,
                           XmppTestUtil.TEST_TIMESTAMP);
    assertEquals(2, mockComponent.packetsSent);
    final Packet response = mockComponent.lastPacketSent;
    // TODO: check to and from are correct.

    // inject the response into the federation remote, via the component
    mockComponent.processPacket(response);

    assertEquals((Integer) XmppTestUtil.TEST_OPERATIONS,
                 mockSubmitResultListener.savedOperationsApplied);
    assertEquals(hashedVersion, mockSubmitResultListener.savedHashedVersion);
    assertEquals((Long) XmppTestUtil.TEST_TIMESTAMP,
                 mockSubmitResultListener.savedTimestamp);
    assertNull(mockSubmitResultListener.savedErrorMessage);
  }

  public void testSubmitRequestFailed() {
    // TODO: error generation and handling
  }

  /**
   * Starting with a wave server remote, make a history request. Check it is
   * sent to the foreign federation host by the XMPP remote code. Decode the
   * packet with the XMPP host code. Check it is passed as a history request to
   * the wave server host. Send a response back from the wave server host. Check
   * it is sent to the foreign federation remote by the XMPP host code. Decode
   * the history response packet with the XMPP remote code. Check it is passed
   * to the wave server remote.
   */
  public void testHistoryRequest() {
    fedRemote.requestHistory(XmppTestUtil.waveletName,
                             XmppTestUtil.TEST_REMOTE_DOMAIN,
                             XmppTestUtil.TEST_START_VERSION,
                             XmppTestUtil.TEST_END_VERSION,
                             XmppTestUtil.TEST_LENGTH_LIMIT,
                             mockHistoryResultLister);
    mockDisco.discoComplete();
    assertEquals(1, mockComponent.packetsSent);
    final Packet request = mockComponent.lastPacketSent;
    // inject the request into the federation host, via the component
    mockComponent.processPacket(request);
    assertEquals(XmppTestUtil.waveletName, mockProvider.savedWaveletName);
    assertEquals(XmppTestUtil.TEST_REMOTE_DOMAIN, mockProvider.savedDomain);
    assertEquals(XmppTestUtil.TEST_START_VERSION,
                 mockProvider.savedStartVersion);
    assertEquals(XmppTestUtil.TEST_END_VERSION, mockProvider.savedEndVersion);
    assertEquals(XmppTestUtil.TEST_LENGTH_LIMIT, mockProvider.savedLengthLimit);

    final WaveletFederationProvider.HistoryResponseListener hostListener =
        mockProvider.savedHistoryListener;

    // trigger the response
    List<ByteString> deltaList = ImmutableList.of(appliedDelta);
    hostListener.onSuccess(deltaList, XmppTestUtil.TEST_COMMITTED,
                           XmppTestUtil.TEST_TRUNCATED);
    assertEquals(2, mockComponent.packetsSent);
    final Packet response = mockComponent.lastPacketSent;

    // inject the request into the federation host, via the component
    mockComponent.processPacket(response);

    assertEquals(deltaList, mockHistoryResultLister.savedDeltaList);
    assertEquals((Long) XmppTestUtil.TEST_COMMITTED,
                 mockHistoryResultLister.savedCommittedVersion);
    assertEquals((Long) XmppTestUtil.TEST_TRUNCATED,
                 mockHistoryResultLister.savedVersionTruncated);
    assertNull(mockHistoryResultLister.savedErrorMessage);
  }

  /**
   * Tests roundtripping a wavelet committed message from the federation host to
   * the federation remote and back.
   */
  public void testWaveletUpdate() {
    fedHostForDomain.waveletDeltaUpdate(XmppTestUtil.waveletName,
                                   Lists.newArrayList(appliedDelta),
                                   null);  // TODO: OK to not null callback?
    mockDisco.discoComplete();
    assertEquals(1, mockComponent.packetsSent);
    final Packet request = mockComponent.lastPacketSent;
    // inject the request into the federation remote, via the component
    mockComponent.processPacket(request);

    assertNotNull(mockRemoteListener.savedUpdateWaveletName);
    assertEquals(XmppTestUtil.waveletName,
                 mockRemoteListener.savedUpdateWaveletName);
    assertEquals(ImmutableList.of(appliedDelta),
                 mockRemoteListener.savedDeltas);
    assertNull(mockRemoteListener.savedCommitWaveletName);
    assertNull(mockRemoteListener.savedVersion);

    // Remote automatically replies.
    assertEquals(2, mockComponent.packetsSent);
    final Packet response = mockComponent.lastPacketSent;
    mockComponent.processPacket(response);
    // TODO: when retries are done, test that this cancels the retry
  }

  /**
   * Tests the remote's getDeltaSignerRequest.
   */
  public void testGetSigner() {
    common.ProtocolHashedVersion hashVersion =
        XmppTestUtil.createTestHistoryHashVersion();
    fedRemote.getDeltaSignerInfo(XmppTestUtil.testSignerId, XmppTestUtil.waveletName, hashVersion,
                                 mockDeltaSignerResponseListener);
    mockDisco.discoComplete();

    assertEquals(1, mockComponent.packetsSent);
    final Packet request = mockComponent.lastPacketSent;

    mockComponent.processPacket(request);

    assertEquals(XmppTestUtil.waveletName, mockProvider.savedWaveletName);
    assertEquals(hashVersion, mockProvider.savedSignerRequestDelta);
    common.ProtocolSignerInfo signer = XmppTestUtil.createProtocolSignerInfo();
    mockProvider.savedGetSignerListener.onSuccess(signer);

    assertEquals(2, mockComponent.packetsSent);
    final Packet response = mockComponent.lastPacketSent;

    mockComponent.processPacket(response);
    assertEquals(signer, mockDeltaSignerResponseListener.signerInfo);
    assertNull(mockDeltaSignerResponseListener.errorMessage);
  }

  /**
   * Test the remote's postDeltaSigner request
   */
  public void testPostSigner() {
    common.ProtocolSignerInfo signerInfo =
        XmppTestUtil.createProtocolSignerInfo();
    fedRemote.postSignerInfo(XmppTestUtil.TEST_LOCAL_DOMAIN, signerInfo,
                             mockPostSignerListener);
    mockDisco.discoComplete();

    assertEquals(1, mockComponent.packetsSent);
    final Packet request = mockComponent.lastPacketSent;

    mockComponent.processPacket(request);

    assertEquals(XmppTestUtil.TEST_LOCAL_DOMAIN,
                 mockProvider.savedPostedDomain);
    assertEquals(signerInfo, mockProvider.savedSignerInfo);

    mockProvider.savedPostSignerResponseListener.onSuccess();

    assertEquals(2, mockComponent.packetsSent);
    final Packet response = mockComponent.lastPacketSent;

    mockComponent.processPacket(response);
    assertTrue(mockPostSignerListener.onSuccessCalled);
    assertNull(mockPostSignerListener.savedErrorMessage);

  }
}

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

import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.RpcCallback;

import junit.framework.TestCase;

import org.apache.commons.codec.binary.Base64;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.jivesoftware.whack.ExternalComponentManager;
import org.waveprotocol.wave.examples.fedone.common.HashedVersion;
import org.waveprotocol.wave.examples.fedone.common.WaveletOperationSerializer;
import org.waveprotocol.wave.examples.fedone.waveserver.SubmitResultListener;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveletFederationListener;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveletFederationProvider;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.protocol.common;
import org.xmpp.component.Component;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility functions and mock classes for testing XMPP Federation code.
 *
 *
 */
class XmppTestUtil extends TestCase {

  static final String TEST_AUTHOR = "fozzie@initech-corp.com";
  static final long TEST_COMMITTED = 2344;
  // TODO: Set correct hashes
  static final common.ProtocolHashedVersion TEST_END_VERSION =
      WaveletOperationSerializer.serialize(HashedVersion.unsigned(2345));

  static final String TEST_IQ_ID = "1-1";

  static final String TEST_HISTORY_HASH =
      Base64Util.encode(HashedVersion.UNSIGNED_VERSION_0.getHistoryHash());

  static final long TEST_LENGTH_LIMIT = 300000;
  static final String TEST_LOCAL_JID = "wave.initech-corp.com";
  static final String TEST_LOCAL_DOMAIN = "initech-corp.com";
  static final int TEST_OPERATIONS = 2;

  static final String TEST_REMOTE_DOMAIN = "acmewave.com";
  static final String TEST_REMOTE_PUBSUB_JID = "pubsub.acmewave.com";
  static final String TEST_REMOTE_WAVE_JID = "wave.acmewave.com";
  // TODO: Set correct hashes
  static final common.ProtocolHashedVersion TEST_START_VERSION =
      WaveletOperationSerializer.serialize(HashedVersion.unsigned(12));

  //    common.ProtocolHashedVersion.newBuilder().setVersion(12).setHistoryHash("hash-12").build();
  static final long TEST_TIMESTAMP = 1234567890;
  static final long TEST_TRUNCATED = 2300;
  static final int TEST_VERSION = 1234;

  static final Pattern cDataPattern =
      Pattern.compile("(.*!\\[CDATA\\[)(.*)(\\]\\].*)",
                      Pattern.DOTALL | Pattern.MULTILINE);
  static final WaveletName waveletName =
      WaveletName.of("initech-corp.com!a", "acmewave.com!b");
  static final String EXPECTED_SUBMIT_RESPONSE =
      "\n<iq type=\"result\" id=\"1-1\" from=\"wave.initech-corp.com\""
      + " to=\"wave.acmewave.com\">\n"
      + "  <pubsub xmlns=\"http://jabber.org/protocol/pubsub\">\n"
      + "    <publish>\n"
      + "      <item>\n"
      + "        <submit-response"
      + " xmlns=\"http://waveprotocol.org/protocol/0.2/waveserver\""
      + " application-timestamp=\"1234567890\""
      + " operations-applied=\"2\">\n"
      + "          <hashed-version history-hash=\""
      + Base64Util.encode(HashedVersion.UNSIGNED_VERSION_0.getHistoryHash())
      + "\" version=\"1234\"/>\n"
      + "        </submit-response>\n"
      + "      </item>\n"
      + "    </publish>\n"
      + "  </pubsub>\n"
      + "</iq>";
  static final String EXPECTED_HISTORY_RESPONSE =
      "\n<iq type=\"result\" id=\"1-1\" from=\"wave.initech-corp.com\""
      + " to=\"wave.acmewave.com\">\n"
      + "  <pubsub xmlns=\"http://jabber.org/protocol/pubsub\">\n"
      + "    <items>\n"
      + "      <item>\n"
      + "        <applied-delta"
      + " xmlns=\"http://waveprotocol.org/protocol/0.2/waveserver\">"
      + "<![CDATA[ignored]]></applied-delta>\n"
      + "      </item>\n"
      + "      <item>\n"
      + "        <commit-notice"
      + " xmlns=\"http://waveprotocol.org/protocol/0.2/waveserver\""
      + " version=\"2344\"/>\n"
      + "      </item>\n"
      + "      <item>\n"
      + "        <history-truncated"
      + " xmlns=\"http://waveprotocol.org/protocol/0.2/waveserver\""
      + " version=\"2300\"/>\n"
      + "      </item>\n"
      + "    </items>\n"
      + "  </pubsub>\n"
      + "</iq>";
  private static final String TEST_FAKE_CERTIFICATE =
      "CERTIFICATE OF PARTICIPATION";
  private static final String TEST_FAKE_CERTIFICATE_2 = "BEST CHOCOLATE CAKE";
  public static ByteString testSignerId = ByteString.copyFromUtf8("user@acmewave.com");

  public void testNothing() {
    /** Placeholder to shut up over eager test runners */
  }

  /**
   * Asserts that two XML strings are equal, after replacing CDATA elements with
   * a fixed string.
   *
   * @param expected the expected result
   * @param actual   the actual result
   */
  static void assertEqualsWithoutCData(String expected, String actual) {
    assertEquals(stripCData(expected), stripCData(actual));
  }

  /**
   * Extracts the first CDATA from an XML string
   *
   * @param original the original string
   * @return the extracted string, or null if no CDATA found.
   */
  static String extractCData(String original) {
    Matcher matcher = cDataPattern.matcher(original);
    if (matcher.find()) {
      return matcher.group(2);
    } else {
      return null;
    }
  }

  /**
   * Strips the first CDATA from an XML string.
   *
   * @param original the original string
   * @return the stripped string.
   */
  static String stripCData(String original) {
    Matcher matcher = cDataPattern.matcher(original);
    if (matcher.find()) {
      return matcher.group(1) + matcher.group(3);
    } else {
      return original;
    }
  }

  /**
   * Turns an XML string into the correct subclass of Packet.
   *
   * @param xml the XML string
   * @return an instance of a Packet subclass
   */
  static Packet xmlToPacket(String xml) {
    SAXReader reader = new SAXReader();
    Element root;
    try {
      root = reader.read(xml).getRootElement();
    } catch (DocumentException e) {
      fail("invalid XML: " + xml);
      return null;
    }
    String tag = root.getQName().getName();
    if (tag.equals("message")) {
      return new Message(root);
    } else if (tag.equals("iq")) {
      return new IQ(root);
    } else {
      fail("unsupported packet type: " + tag);
      return null;
    }
  }

  /**
   * Methods that create and test standard canned protobuffers.
   */

  /**
   * Creates a ByteString representation of a ProtocolAppliedWaveletDelta for use in tests.
   *
   * @return the new PB
   */
  static ByteString createTestAppliedWaveletDelta() {
    common.ProtocolHashedVersion hashedVersion = createTestHistoryHashVersion();
    common.ProtocolAppliedWaveletDelta.Builder appliedDelta =
        common.ProtocolAppliedWaveletDelta.newBuilder();
    appliedDelta.setHashedVersionAppliedAt(hashedVersion);
    appliedDelta.setApplicationTimestamp(TEST_TIMESTAMP);
    appliedDelta.setOperationsApplied(TEST_OPERATIONS);
    common.ProtocolSignedDelta delta = createTestSignedDelta();
    appliedDelta.setSignedOriginalDelta(delta);
    return appliedDelta.build().toByteString();
  }

  /**
   * Creates a ProtocolHashedVersion for use in tests.
   *
   * @return the new PB
   */
  static common.ProtocolHashedVersion createTestHistoryHashVersion() {
    return WaveletOperationSerializer
        .serialize(HashedVersion.unsigned(TEST_VERSION));
  }

  /**
   * Creates a test ProtocolSignerInfo protobuffer.
   *
   * @return the new PB
   */
  public static common.ProtocolSignerInfo createProtocolSignerInfo() {
    common.ProtocolSignerInfo.Builder signer =
        common.ProtocolSignerInfo.newBuilder();
    signer.addCertificate(ByteString.copyFromUtf8(TEST_FAKE_CERTIFICATE));
    signer.addCertificate(ByteString.copyFromUtf8(TEST_FAKE_CERTIFICATE_2));
    signer.setDomain(TEST_LOCAL_DOMAIN);
    signer.setHashAlgorithm(common.ProtocolSignerInfo.HashAlgorithm.SHA256);
    return signer.build();
  }

  /**
   * Creates a ProtocolSignedDelta PB for use in tests.
   *
   * @return the new PB
   */
  static common.ProtocolSignedDelta createTestSignedDelta() {
    common.ProtocolHashedVersion hashedVersion = createTestHistoryHashVersion();

    common.ProtocolSignedDelta.Builder delta =
        common.ProtocolSignedDelta.newBuilder();
    common.ProtocolWaveletDelta.Builder waveletDelta =
        common.ProtocolWaveletDelta.newBuilder();
    waveletDelta.setAuthor(TEST_AUTHOR);
    waveletDelta.setHashedVersion(hashedVersion);
    delta.setDelta(waveletDelta.build().toByteString());
    return delta.build();
  }

  /**
   * Checks that the signed delta from the packet is the same as the one we
   * created in createTestAppliedWaveletDelta.
   *
   * @param base64 base64 encoded PB.
   */
  static void verifyTestAppliedWaveletDelta(String base64) {
    try {
      common.ProtocolAppliedWaveletDelta appliedDelta =
          common.ProtocolAppliedWaveletDelta.parseFrom(
              Base64.decodeBase64(base64.getBytes()));
    } catch (InvalidProtocolBufferException e) {
      fail("String not valid base64 PB: '" + base64 + "'");
    }
    // TODO: implement field checks.
  }

  /**
   * Checks that the signed delta from the packet is the same as the one we
   * created in createTestSignedDelta.
   *
   * @param base64 base64 encoded PB.toByteArray()
   */
  static void verifyTestSignedDelta(String base64) {
    try {
      common.ProtocolSignedDelta delta =
          common.ProtocolSignedDelta.parseFrom(
              Base64.decodeBase64(base64.getBytes()));
    } catch (InvalidProtocolBufferException e) {
      fail("String not valid base64 PB: '" + base64 + "'");
    }
    // TODO: implement field checks
  }

  /**
   * Mock classes that record the parameters passed to interesting methods.
   */

  static class MockDeltaSignerResponseListener
      implements WaveletFederationProvider.DeltaSignerInfoResponseListener {

    public common.ProtocolSignerInfo signerInfo = null;
    public String errorMessage = null;

    public void onSuccess(common.ProtocolSignerInfo signerInfo) {
      this.signerInfo = signerInfo;
    }

    public void onFailure(String errorMessage) {
      this.errorMessage = errorMessage;
    }
  }


  /**
   * A mock of XMPPDisco that allows controlled triggering of disco completion.
   */
  static class MockDisco extends XmppDisco {

    String remoteJID;
    boolean discoStarted = false;
    String savedRemoteDomain;
    private RpcCallback<String> callback;

    @Override
    void discoverRemoteJid(String remoteDomain, RpcCallback<String> callback) {
      discoStarted = true;
      this.savedRemoteDomain = remoteDomain;
      this.callback = callback;
    }

    void discoComplete() {
      callback.run(remoteJID);
    }

    void setRemoteJID(String jid) {
      this.remoteJID = jid;
    }
  }

  /**
   * A mock ExternalComponentManager that tracks how many packets are sent
   */
  static class MockExternalComponentManager
      extends ExternalComponentManager {

    int packetsSent = 0;

    public MockExternalComponentManager(String ip) {
      super(ip);
    }

    @Override
    public void sendPacket(Component component, Packet packet) {
      packetsSent++;
    }
  }


  /**
   * A mock HistoryResponseListener that saves the passed arguments.
   */
  static class MockHistoryResponseListener
      implements WaveletFederationProvider.HistoryResponseListener {

    public List<ByteString> savedDeltaList = null;
    public Long savedCommittedVersion = null;
    public Long savedVersionTruncated = null;
    public String savedErrorMessage = null;

    @Override
    public void onSuccess(List<ByteString> deltaList,
                          long lastCommittedVersion, long versionTruncatedAt) {
      savedDeltaList = deltaList;
      savedCommittedVersion = lastCommittedVersion;
      savedVersionTruncated = versionTruncatedAt;
    }

//     @Override
//     public void onSuccess(Set<common.ProtocolAppliedWaveletDelta> deltaSet,
//                           long lastCommittedVersion) {
//       savedDeltaSet = deltaSet;
//       savedCommittedVersion = lastCommittedVersion;
//     }

    @Override
    public void onFailure(String errorMessage) {
      savedErrorMessage = errorMessage;
    }
  }

  /**
   * A mock of WaveletFederationProvider that saves the arguments passed to it.
   */
  static class MockProvider implements WaveletFederationProvider {

    public WaveletName savedWaveletName;
    public String savedDomain;
    public common.ProtocolHashedVersion savedStartVersion;
    public common.ProtocolHashedVersion savedEndVersion;
    public long savedLengthLimit;
    public HistoryResponseListener savedHistoryListener;
    public common.ProtocolSignedDelta savedDelta;
    public SubmitResultListener savedSubmitListener;
    public common.ProtocolHashedVersion savedSignerRequestDelta = null;
    public DeltaSignerInfoResponseListener savedGetSignerListener = null;
    public PostSignerInfoResponseListener savedPostSignerResponseListener =
        null;
    public String savedPostedDomain = null;
    public common.ProtocolSignerInfo savedSignerInfo = null;
    private ByteString savedSignerId;

    /**
     * Mock method that saves its results.
     */
    @Override
    public void requestHistory(WaveletName waveletName, String domain,
                               common.ProtocolHashedVersion startVersion,
                               common.ProtocolHashedVersion endVersion,
                               long lengthLimit,
                               HistoryResponseListener listener) {
      this.savedWaveletName = waveletName;
      this.savedDomain = domain;
      this.savedStartVersion = startVersion;
      this.savedEndVersion = endVersion;
      this.savedLengthLimit = lengthLimit;
      this.savedHistoryListener = listener;
    }

    /**
     * Mock method that saves its results.
     */
    @Override
    public void submitRequest(WaveletName waveletName,
                              common.ProtocolSignedDelta delta,
                              SubmitResultListener listener) {
      this.savedWaveletName = waveletName;
      this.savedDelta = delta;
      this.savedSubmitListener = listener;
    }

    @Override
    public void getDeltaSignerInfo(ByteString signerId,
                                   WaveletName waveletName,
                                   common.ProtocolHashedVersion deltaEndVersion,
                                   DeltaSignerInfoResponseListener listener) {
      this.savedSignerId = signerId;
      this.savedWaveletName = waveletName;
      this.savedSignerRequestDelta = deltaEndVersion;
      this.savedGetSignerListener = listener;
    }

    @Override
    public void postSignerInfo(String destinationDomain,
                               common.ProtocolSignerInfo signerInfo,
                               PostSignerInfoResponseListener listener) {
      this.savedPostedDomain = destinationDomain;
      this.savedSignerInfo = signerInfo;
      this.savedPostSignerResponseListener = listener;
    }
  }

  /**
   * A mock SubmitResultListener that saves it's arguments.
   */
  static class MockSubmitResultListener implements SubmitResultListener {

    public Integer savedOperationsApplied = null;
    public common.ProtocolHashedVersion savedHashedVersion = null;
    public Long savedTimestamp = null;
    public String savedErrorMessage = null;

    @Override
    public void onSuccess(int operationsApplied,
                          common.ProtocolHashedVersion hashedVersionAfterApplication,
                          long applicationTimestamp) {
      savedOperationsApplied = operationsApplied;
      savedHashedVersion = hashedVersionAfterApplication;
      savedTimestamp = applicationTimestamp;
    }

    @Override
    public void onFailure(String errorMessage) {
      savedErrorMessage = errorMessage;
    }
  }

  /**
   * A listener factory that returns a known instance of a MockWaveletListener.
   */
  static class MockWaveletListenerFactory
      implements WaveletFederationListener.Factory {

    public MockWaveletListener mockWaveletListener;
    public String savedDomain = null;

    @Override
    public WaveletFederationListener listenerForDomain(String domain) {
      savedDomain = domain;
      return mockWaveletListener;
    }
  }

  /**
   * A mock listener (Remote interface) that listens for update and commit
   * messages and saves them.
   */
  static class MockWaveletListener implements WaveletFederationListener {

    WaveletName savedUpdateWaveletName = null;
    WaveletName savedCommitWaveletName = null;
    List<ByteString> savedDeltas = null;
    common.ProtocolHashedVersion savedVersion = null;
    public WaveletUpdateCallback savedCallback = null;

    @Override
    public void waveletDeltaUpdate(WaveletName waveletName, List<ByteString> deltas,
        WaveletUpdateCallback callback) {
      Preconditions.checkState(savedUpdateWaveletName == null);
      Preconditions.checkState(savedCallback == null);
      savedUpdateWaveletName = waveletName;
      savedCallback = callback;
      savedDeltas = deltas;
      callback.onSuccess();
    }

    @Override
    public void waveletCommitUpdate(WaveletName waveletName, common.ProtocolHashedVersion version,
        WaveletUpdateCallback callback) {
      Preconditions.checkState(savedUpdateWaveletName == null);
      Preconditions.checkState(savedCallback == null);
      savedUpdateWaveletName = waveletName;
      savedCallback = callback;
      savedVersion = version;
      callback.onSuccess();
    }
  }

  /**
   * A mock of the WaveXmppComponent that tracks the last packet sent, as well
   * as a count of packets sent. It also produces a fixed ID for all packets.
   */
  static class MockWaveXmppComponent extends WaveXmppComponent {

    public int packetsSent = 0;
    public Packet lastPacketSent = null;
    public boolean lastRetryFlag;
    public RpcCallback<Packet> lastCallback = null;

    public MockWaveXmppComponent(String componentName, String serverHostname,
                                 String serverSecret, int serverPort,
                                 WaveXmppComponent.ExternalComponentManagerFactory managerFactory,
                                 XmppFederationHost host,
                                 XmppFederationRemote remote, XmppDisco disco) {
      super(componentName, serverHostname, serverSecret, "127.0.0.1",
            serverPort,
            "",
            managerFactory, host,
            remote, disco);
      componentJID = TEST_LOCAL_JID;
    }

    @Override
    public void sendPacket(Packet packet, boolean retry,
                           RpcCallback<Packet> callback, RpcCallback<Packet> errorCallback) {
      if (callback != null) {
        String key = genCallbackKey(packet, true /* request */);
        requestCallbacks.put(key, callback);
      }
      packetsSent++;
      lastPacketSent = packet;
      lastRetryFlag = retry;
      lastCallback = callback;
    }

    @Override
    public String generateId() {
      return TEST_IQ_ID;
    }
  }

  static class MockPostSignerResponseListener
      implements WaveletFederationProvider.PostSignerInfoResponseListener {

    public String savedErrorMessage = null;
    public boolean onSuccessCalled = false;

    public void onSuccess() {
      onSuccessCalled = true;
    }

    public void onFailure(String errorMessage) {
      this.savedErrorMessage = errorMessage;
    }
  }
}

/**
 * Copyright 2009 Google Inc.
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

package org.waveprotocol.wave.examples.fedone.waveserver;

import static org.waveprotocol.wave.examples.fedone.common.WaveletOperationSerializer.serialize;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;

import junit.framework.TestCase;

import org.waveprotocol.wave.examples.fedone.common.HashedVersion;
import org.waveprotocol.wave.examples.fedone.common.WaveletOperationSerializer;
import org.waveprotocol.wave.examples.fedone.model.util.HashedVersionZeroFactoryImpl;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.wave.AddParticipant;
import org.waveprotocol.wave.model.operation.wave.RemoveParticipant;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.protocol.common.ProtocolSignature;
import org.waveprotocol.wave.protocol.common.ProtocolSignedDelta;
import org.waveprotocol.wave.protocol.common.ProtocolWaveletDelta;

import java.util.Collections;
import java.util.List;

/**
 * Tests for local and remote wavelet containers.
 *
 *
 */
public class WaveletContainerTest extends TestCase {
  private static final String domain = "wave.google.com";
  private static final WaveletName waveletName = WaveletName.of(
      new WaveId(domain, "waveid"), new WaveletId(domain, "waveletid"));
  private static final ParticipantId author = new ParticipantId("admin@" + domain);
  private static final List<ParticipantId> participants = ImmutableList.of(
      new ParticipantId("foo@" + domain), new ParticipantId("bar@example.com"));
  private static final HashedVersion version0 =
      new HashedVersionZeroFactoryImpl().createVersionZero(waveletName);
  private static final ProtocolSignature fakeSignature = ProtocolSignature.newBuilder()
      .setSignatureBytes(ByteString.EMPTY)
      .setSignerId(ByteString.EMPTY)
      .setSignatureAlgorithm(ProtocolSignature.SignatureAlgorithm.SHA1_RSA)
      .build();

  private List<WaveletOperation> addParticipantOps;
  private List<WaveletOperation> removeParticipantOps;
  private List<WaveletOperation> doubleRemoveParticipantOps;

  private ProtocolWaveletDelta addParticipantDelta;
  private ProtocolWaveletDelta removeParticipantDelta;
  private ProtocolWaveletDelta doubleRemoveParticipantDelta;

  private LocalWaveletContainerImpl localWavelet;
  private RemoteWaveletContainerImpl remoteWavelet;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    localWavelet = new LocalWaveletContainerImpl(waveletName);
    remoteWavelet = new RemoteWaveletContainerImpl(waveletName);

    addParticipantOps = Lists.newArrayList();
    removeParticipantOps = Lists.newArrayList();


    for (ParticipantId p : participants) {
      addParticipantOps.add(new AddParticipant(p));
      removeParticipantOps.add(new RemoveParticipant(p));
    }

    Collections.reverse(removeParticipantOps);
    doubleRemoveParticipantOps = Lists.newArrayList(removeParticipantOps);
    doubleRemoveParticipantOps.addAll(removeParticipantOps);

    addParticipantDelta = WaveletOperationSerializer.serialize(
        new WaveletDelta(author, addParticipantOps), version0);
    removeParticipantDelta = WaveletOperationSerializer.serialize(
        new WaveletDelta(author, removeParticipantOps), version0);
    doubleRemoveParticipantDelta = WaveletOperationSerializer.serialize(
        new WaveletDelta(author, doubleRemoveParticipantOps), version0);
  }

  // Tests

  public void testLocalApplyWaveletOperation() throws Exception {
    assertSuccessfulApplyWaveletOperations(localWavelet);
  }

  public void testRemoteApplyWaveletOperation() throws Exception {
    assertSuccessfulApplyWaveletOperations(remoteWavelet);
  }

  public void testLocalFailedWaveletOperations() throws Exception {
    assertFailedWaveletOperations(localWavelet);
  }

  public void testRemoteFailedWaveletOperations() throws Exception {
    assertFailedWaveletOperations(localWavelet);
  }

  public void testSuccessfulLocalRequest() throws Exception {
    ProtocolSignedDelta addDelta = ProtocolSignedDelta.newBuilder()
        .addSignature(fakeSignature)
        .setDelta(addParticipantDelta.toByteString())
        .build();
    localWavelet.submitRequest(waveletName, addDelta);
    assertEquals(localWavelet.getCurrentVersion().getVersion(), 2);

    ProtocolSignedDelta removeDelta = ProtocolSignedDelta.newBuilder()
        .addSignature(fakeSignature)
        .setDelta(ProtocolWaveletDelta.newBuilder(removeParticipantDelta).setHashedVersion(
            serialize(localWavelet.getCurrentVersion())).build().toByteString())
        .build();
    localWavelet.submitRequest(waveletName, removeDelta);
    assertEquals(localWavelet.getCurrentVersion().getVersion(), 4);
  }

  public void testFailedLocalWaveletRequest() throws Exception {
    ProtocolSignedDelta removeDelta = ProtocolSignedDelta.newBuilder()
        .addSignature(fakeSignature)
        .setDelta(removeParticipantDelta.toByteString())
        .build();
    try {
      localWavelet.submitRequest(waveletName, removeDelta);
      fail("Should fail");
    } catch (OperationException e) {
      // Correct
    }
    assertEquals(localWavelet.getCurrentVersion(), version0);

    ProtocolSignedDelta addDelta = ProtocolSignedDelta.newBuilder()
        .addSignature(fakeSignature)
        .setDelta(addParticipantDelta.toByteString())
        .build();

    localWavelet.submitRequest(waveletName, addDelta);
    try {
      ProtocolSignedDelta addAgainDelta = ProtocolSignedDelta.newBuilder()
          .addSignature(fakeSignature)
          .setDelta(ProtocolWaveletDelta.newBuilder(addParticipantDelta)
              .setHashedVersion(serialize(localWavelet.getCurrentVersion()))
              .build().toByteString())
          .build();
      localWavelet.submitRequest(waveletName, addAgainDelta);
      fail("Should fail");
    } catch (OperationException e) {
      // Correct
    }
    assertEquals(localWavelet.getCurrentVersion().getVersion(), 2);

    HashedVersion oldVersion = localWavelet.getCurrentVersion();
    ProtocolSignedDelta rollbackDelta = ProtocolSignedDelta.newBuilder()
        .addSignature(fakeSignature)
        .setDelta(ProtocolWaveletDelta.newBuilder(doubleRemoveParticipantDelta)
            .setHashedVersion(serialize(localWavelet.getCurrentVersion()))
            .build().toByteString())
        .build();
    try {
      localWavelet.submitRequest(waveletName, rollbackDelta);
      fail("Should fail");
    } catch (OperationException e) {
      // Correct
    }
    assertEquals(localWavelet.getCurrentVersion(), oldVersion);
  }

  public void testLocalEmptyDelta() throws Exception {
    ProtocolSignedDelta emptyDelta = ProtocolSignedDelta.newBuilder()
        .addSignature(fakeSignature)
        .setDelta(ProtocolWaveletDelta.newBuilder()
            .setAuthor(author.toString())
            .setHashedVersion(serialize(version0))
            .build().toByteString())
        .build();
    try {
      localWavelet.submitRequest(waveletName, emptyDelta);
      fail("Should fail");
    } catch (EmptyDeltaException e) {
      // Correct
    }
  }

  // Utilities

  /**
   * Check that a container succeeds when adding non-existent participants and removing existing
   * participants.
   */
  private void assertSuccessfulApplyWaveletOperations(WaveletContainerImpl with) throws Exception {
    with.applyWaveletOperations(addParticipantOps);
    assertDeepEquals(with.getParticipants(), participants);

    with.applyWaveletOperations(removeParticipantOps);
    assertDeepEquals(with.getParticipants(), Collections.emptyList());
  }

  /**
   * Check that a container fails when removing non-existent participants and adding duplicate
   * participants, and that the partipant list is preserved correctly.
   */
  private void assertFailedWaveletOperations(WaveletContainerImpl with) throws Exception {
    try {
      with.applyWaveletOperations(removeParticipantOps);
      fail("Should fail");
    } catch (OperationException e) {
      // Correct
    }
    assertDeepEquals(localWavelet.getParticipants(), Collections.emptyList());

    with.applyWaveletOperations(addParticipantOps);
    try {
      with.applyWaveletOperations(addParticipantOps);
      fail("Should fail");
    } catch (OperationException e) {
      // Correct
    }
    assertDeepEquals(with.getParticipants(), participants);

    try {
      with.applyWaveletOperations(doubleRemoveParticipantOps);
      fail("Should fail");
    } catch (OperationException e) {
      // Correct
    }
    assertDeepEquals(with.getParticipants(), participants);
  }

  /**
   * Assert two lists are equals using equals() on elements (rather than ==).
   */
  private static void assertDeepEquals(List<?> list1, List<?> list2) {
    assertEquals(list1.size(), list2.size());
    for (int i = 0; i < list1.size(); i++) {
      assertEquals(list1.get(i), list2.get(i));
    }
  }
}

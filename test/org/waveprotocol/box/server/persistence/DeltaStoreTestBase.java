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

package org.waveprotocol.box.server.persistence;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.ByteString;

import junit.framework.TestCase;

import org.waveprotocol.box.server.common.CoreWaveletOperationSerializer;
import org.waveprotocol.box.server.util.testing.TestingConstants;
import org.waveprotocol.box.server.waveserver.ByteStringMessage;
import org.waveprotocol.box.server.waveserver.DeltaStore;
import org.waveprotocol.box.server.waveserver.WaveletDeltaRecord;
import org.waveprotocol.box.server.waveserver.DeltaStore.DeltasAccess;
import org.waveprotocol.wave.federation.Proto.ProtocolAppliedWaveletDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolSignature;
import org.waveprotocol.wave.federation.Proto.ProtocolSignedDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolSignature.SignatureAlgorithm;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.wave.AddParticipant;
import org.waveprotocol.wave.model.operation.wave.NoOp;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperationContext;
import org.waveprotocol.wave.model.util.Pair;
import org.waveprotocol.wave.model.version.HashedVersion;

import java.io.IOException;
import java.util.List;

/**
 * Tests for all DeltaStore implementations.
 *
 * @author Joseph Gentle (josephg@gmail.com)
 */
public abstract class DeltaStoreTestBase extends TestCase {
  private final WaveletName WAVE1_WAVELET1 =
    WaveletName.of(new WaveId("example.com", "wave1"), new WaveletId("example.com", "wavelet1"));

  /** Create and return a new delta store instance of the type being tested. */
  protected abstract DeltaStore newDeltaStore() throws Exception;

  public void testOpenNonexistantWavelet() throws Exception {
    DeltaStore store = newDeltaStore();
    DeltasAccess wavelet = store.open(WAVE1_WAVELET1);

    // Sanity check a bunch of values in the wavelet.
    assertTrue(wavelet.isEmpty());
    assertEquals(WAVE1_WAVELET1, wavelet.getWaveletName());
    assertNull(wavelet.getEndVersion());
    assertNull(wavelet.getDelta(0));
    assertNull(wavelet.getDeltaByEndVersion(0));
    assertNull(wavelet.getAppliedAtVersion(0));
    assertNull(wavelet.getResultingVersion(0));
    assertNull(wavelet.getAppliedDelta(0));
    assertNull(wavelet.getTransformedDelta(0));

    wavelet.close();
  }

  public void testWriteToNewWavelet() throws Exception {
    Pair<DeltaStore,WaveletDeltaRecord> pair = newDeltaStoreWithRecord(WAVE1_WAVELET1);
    DeltaStore store = pair.first;
    WaveletDeltaRecord record = pair.second;

    DeltasAccess wavelet = store.open(WAVE1_WAVELET1);

    assertFalse(wavelet.isEmpty());
    assertEquals(WAVE1_WAVELET1, wavelet.getWaveletName());
    assertEquals(record.getResultingVersion(), wavelet.getEndVersion());
    assertEquals(record, wavelet.getDelta(0));
    assertEquals(record, wavelet.getDeltaByEndVersion(record.getResultingVersion().getVersion()));
    assertEquals(record.getAppliedAtVersion(), wavelet.getAppliedAtVersion(0));
    assertEquals(record.getAppliedDelta(), wavelet.getAppliedDelta(0));
    assertEquals(record.getTransformedDelta(), wavelet.getTransformedDelta(0));

    wavelet.close();
  }

  public void testDeleteWaveletRemovesDeltas() throws Exception {
    Pair<DeltaStore, WaveletDeltaRecord> pair = newDeltaStoreWithRecord(WAVE1_WAVELET1);
    DeltaStore store = pair.first;

    store.delete(WAVE1_WAVELET1);
    DeltasAccess wavelet = store.open(WAVE1_WAVELET1);
    assertTrue(wavelet.isEmpty());
    wavelet.close();
  }

  public void testLookupReturnsWavelets() throws Exception {
    Pair<DeltaStore,WaveletDeltaRecord> pair = newDeltaStoreWithRecord(WAVE1_WAVELET1);
    DeltaStore store = pair.first;

    assertEquals(ImmutableSet.of(WAVE1_WAVELET1.waveletId), store.lookup(WAVE1_WAVELET1.waveId));
  }

  public void testLookupDoesNotReturnEmptyWavelets() throws Exception {
    DeltaStore store = newDeltaStore();
    DeltasAccess wavelet = store.open(WAVE1_WAVELET1);
    wavelet.close();

    assertTrue(store.lookup(WAVE1_WAVELET1.waveId).isEmpty());
  }

  public void testLookupDoesNotReturnDeletedWavelets() throws Exception {
    Pair<DeltaStore, WaveletDeltaRecord> pair = newDeltaStoreWithRecord(WAVE1_WAVELET1);
    DeltaStore store = pair.first;
    store.delete(WAVE1_WAVELET1);

    assertTrue(store.lookup(WAVE1_WAVELET1.waveId).isEmpty());
  }

  // *** Helpers

  protected WaveletDeltaRecord createRecord() throws IOException {
    HashedVersion targetVersion = HashedVersion.of(0, new byte[] {3, 2, 1});
    HashedVersion resultingVersion = HashedVersion.of(2, new byte[] {1, 2, 3});

    WaveletOperationContext context =
        new WaveletOperationContext(TestingConstants.PARTICIPANT, 1234567890, 1);
    List<WaveletOperation> ops =
        ImmutableList.of(new NoOp(context), new AddParticipant(context,
            TestingConstants.OTHER_PARTICIPANT));
    TransformedWaveletDelta transformed =
        new TransformedWaveletDelta(TestingConstants.PARTICIPANT, resultingVersion, 1234567890, ops);

    ProtocolWaveletDelta serializedDelta = CoreWaveletOperationSerializer.serialize(transformed);

    ProtocolSignature signature =
        ProtocolSignature.newBuilder().setSignatureAlgorithm(SignatureAlgorithm.SHA1_RSA)
            .setSignatureBytes(ByteString.copyFrom(new byte[] {1, 2, 3})).setSignerId(
                ByteString.copyFromUtf8("somebody")).build();
    ProtocolSignedDelta signedDelta =
        ProtocolSignedDelta.newBuilder().setDelta(
            ByteStringMessage.serializeMessage(serializedDelta).getByteString()).addAllSignature(
            ImmutableList.of(signature)).build();

    ProtocolAppliedWaveletDelta delta =
        ProtocolAppliedWaveletDelta.newBuilder().setApplicationTimestamp(1234567890)
            .setHashedVersionAppliedAt(CoreWaveletOperationSerializer.serialize(targetVersion))
            .setSignedOriginalDelta(signedDelta).setOperationsApplied(2).build();

    return new WaveletDeltaRecord(targetVersion, ByteStringMessage.serializeMessage(delta),
        transformed);
  }

  private Pair<DeltaStore, WaveletDeltaRecord> newDeltaStoreWithRecord(WaveletName waveletName)
      throws Exception {
    DeltaStore store = newDeltaStore();
    DeltasAccess wavelet = store.open(waveletName);

    WaveletDeltaRecord record = createRecord();
    wavelet.append(ImmutableList.of(record));
    wavelet.close();

    return new Pair<DeltaStore, WaveletDeltaRecord>(store, record);
  }
}

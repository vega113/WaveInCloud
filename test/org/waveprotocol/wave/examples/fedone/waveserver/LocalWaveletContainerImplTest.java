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
package org.waveprotocol.wave.examples.fedone.waveserver;

import com.google.protobuf.ByteString;
import junit.framework.TestCase;
import org.waveprotocol.wave.examples.fedone.common.CoreWaveletOperationSerializer;
import org.waveprotocol.wave.examples.fedone.common.HashedVersion;
import org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion;
import org.waveprotocol.wave.federation.Proto.ProtocolSignature;
import org.waveprotocol.wave.federation.Proto.ProtocolSignature.SignatureAlgorithm;
import org.waveprotocol.wave.federation.Proto.ProtocolSignedDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletOperation;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuilder;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.core.CoreWaveletDocumentOperation;


/**
 * Tests for LocalWaveletContainerImpl.
 *
 * @author arb@google.com (Anthony Baxter)
 */
public class LocalWaveletContainerImplTest extends TestCase {

  private static final WaveletName WAVELET_NAME = WaveletName.of("a!a", "b!b");
  private static final ProtocolSignature SIGNATURE = ProtocolSignature.newBuilder()
      .setSignatureAlgorithm(SignatureAlgorithm.SHA1_RSA)
      .setSignatureBytes(ByteString.EMPTY)
      .setSignerId(ByteString.EMPTY)
      .build();
  private static final String AUTHOR = "kermit@muppetshow.com";

  private static final HashedVersion HASHED_VERSION_ZERO =
      HashedVersion.versionZero(WAVELET_NAME);
  private ProtocolWaveletOperation addParticipantOp;
  private static final String BLIP_ID = "b+muppet";
  private ProtocolWaveletOperation addBlipOp;
  private ProtocolHashedVersion versionZeroHashedVersion;
  private LocalWaveletContainer wavelet;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    addParticipantOp = ProtocolWaveletOperation.newBuilder()
        .setAddParticipant(AUTHOR)
        .build();
    // An empty blip operation - creates a new document.

    addBlipOp = CoreWaveletOperationSerializer.serialize(
        new CoreWaveletDocumentOperation(BLIP_ID, new DocOpBuilder().build()));

    versionZeroHashedVersion = createProtocolHashedVersion(HASHED_VERSION_ZERO);
    wavelet = new LocalWaveletContainerImpl(WAVELET_NAME);
  }

  private ProtocolHashedVersion createProtocolHashedVersion(HashedVersion hashedVersion) {
    return ProtocolHashedVersion.newBuilder()
        .setHistoryHash(ByteString.copyFrom(hashedVersion.getHistoryHash()))
        .setVersion(hashedVersion.getVersion())
        .build();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  /**
   * Tests that duplicate operations are a no-op.
   *
   * @throws Exception should not be thrown.
   */
  public void testDuplicateOperations() throws Exception {

    // create the wavelet.
    DeltaApplicationResult v0Response = wavelet.submitRequest(
        WAVELET_NAME, createProtocolSignedDelta(addParticipantOp, versionZeroHashedVersion));

    ProtocolSignedDelta psd = createProtocolSignedDelta(
        addBlipOp, v0Response.getHashedVersionAfterApplication());

    DeltaApplicationResult dar1 = wavelet.submitRequest(WAVELET_NAME, psd);
    DeltaApplicationResult dar2 = wavelet.submitRequest(WAVELET_NAME, psd);
    assertEquals(dar1.getHashedVersionAfterApplication(), dar2.getHashedVersionAfterApplication());
  }

  private ProtocolSignedDelta createProtocolSignedDelta(ProtocolWaveletOperation operation,
      ProtocolHashedVersion protocolHashedVersion) {
    ProtocolWaveletDelta delta = ProtocolWaveletDelta.newBuilder()
        .setAuthor(AUTHOR)
        .setHashedVersion(protocolHashedVersion)
        .addOperation(operation)
        .build();

    return ProtocolSignedDelta.newBuilder()
        .setDelta(ByteStringMessage.fromMessage(delta).getByteString())
        .addSignature(SIGNATURE)
        .build();
  }
}

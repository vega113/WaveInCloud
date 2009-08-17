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

import com.google.inject.ImplementedBy;
import com.google.protobuf.ByteString;

import org.waveprotocol.wave.examples.fedone.crypto.SignatureException;
import org.waveprotocol.wave.examples.fedone.crypto.SignerInfo;
import org.waveprotocol.wave.examples.fedone.crypto.UnknownSignerException;
import org.waveprotocol.wave.examples.fedone.crypto.WaveSigner;
import org.waveprotocol.wave.protocol.common.ProtocolSignedDelta;
import org.waveprotocol.wave.protocol.common.ProtocolSignerInfo;
import org.waveprotocol.wave.protocol.common.ProtocolWaveletDelta;

import java.util.Set;

/**
 * Stand-in interface for the certificate manager.
 *
 *
 */
@ImplementedBy(CertificateManagerImpl.class)
public interface CertificateManager {

  Set<String> getLocalDomains();

  /**
   * @return the signer info for the local wave signer.
   */
  WaveSigner getLocalSigner();

  /**
   * Verify the signature in the Signed Delta. Use the local WSP's certificate
   * to sign the delta.
   *
   * @param delta as a byte string (the canonical representation of a ProtocolWaveletDelta)
   * @return signed delta
   */
  ProtocolSignedDelta signDelta(ByteStringMessage<ProtocolWaveletDelta> delta);

  /**
   * Verify the signature in the Signed Delta. Use the delta's author's WSP
   * address to identify the certificate.
   *
   * @param signedDelta to verify
   * @return verified canonical ProtocolWaveletDelta, if signatures can be verified
   * @throws SignatureException if the signatures cannot be verified.
   */
  ByteStringMessage<ProtocolWaveletDelta> verifyDelta(ProtocolSignedDelta signedDelta)
      throws SignatureException, UnknownSignerException;

  /**
   * Stores information about a signer (i.e., its certificate chain) in a
   * permanent store. In addition to a certificate chain, a {@link SignerInfo}
   * also contains an identifier of hash algorithm. Signers will use the hash
   * of the cert chain to refer to this signer info in their signatures.
   *
   * @param signerInfo
   * @throws SignatureException if the {@link SignerInfo} doesn't check out
   */
  void storeSignerInfo(ProtocolSignerInfo signerInfo) throws SignatureException;

  /**
   * Retrieves information about a signer.
   *
   * @param signerId identifier of the signer (the hash of its certificate chain)
   * @return the signer information, if found, null otherwise
   */
  ProtocolSignerInfo retrieveSignerInfo(ByteString signerId);
}

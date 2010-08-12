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

import com.google.common.collect.ImmutableList;

import org.waveprotocol.wave.crypto.SignerInfo;
import org.waveprotocol.wave.crypto.WaveSigner;
import org.waveprotocol.wave.federation.Proto.ProtocolSignature;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;

/**
 * A signature handler that delegates to a wave signer to sign deltas.
 */
public class SigningSignatureHandler implements SignatureHandler {

  private final WaveSigner signer;

  public SigningSignatureHandler(WaveSigner signer) {
    this.signer = signer;
  }

  @Override
  public String getDomain() {
    return signer.getSignerInfo().getDomain();
  }

  public SignerInfo getSignerInfo() {
    return signer.getSignerInfo();
  }

  @Override
  public Iterable<ProtocolSignature> sign(ByteStringMessage<ProtocolWaveletDelta> delta) {
    return ImmutableList.of(signer.sign(delta.getByteString().toByteArray()));
  }

}

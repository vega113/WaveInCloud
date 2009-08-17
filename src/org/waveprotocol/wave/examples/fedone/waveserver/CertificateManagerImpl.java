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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import org.waveprotocol.wave.examples.fedone.crypto.CertPathStore;
import org.waveprotocol.wave.examples.fedone.crypto.SignatureException;
import org.waveprotocol.wave.examples.fedone.crypto.SignerInfo;
import org.waveprotocol.wave.examples.fedone.crypto.UnknownSignerException;
import org.waveprotocol.wave.examples.fedone.crypto.WaveSignatureVerifier;
import org.waveprotocol.wave.examples.fedone.crypto.WaveSigner;
import org.waveprotocol.wave.examples.fedone.util.Log;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.protocol.common.ProtocolSignature;
import org.waveprotocol.wave.protocol.common.ProtocolSignedDelta;
import org.waveprotocol.wave.protocol.common.ProtocolSignerInfo;
import org.waveprotocol.wave.protocol.common.ProtocolWaveletDelta;

import java.util.List;
import java.util.Set;

/**
 * Default implementation of {@link CertificateManager}.
 */
@Singleton
public class CertificateManagerImpl implements CertificateManager {

  private static final Log LOG = Log.get(CertificateManagerImpl.class);

  private final WaveSigner waveSigner;
  private final WaveSignatureVerifier verifier;
  private final CertPathStore certPathStore;
  private final boolean disableVerfication;

  @Inject
  public CertificateManagerImpl(
      @Named("waveserver_disable_verification") boolean disableVerfication,
      WaveSigner signer, WaveSignatureVerifier verifier, CertPathStore certPathStore) {
    this.disableVerfication = disableVerfication;
    this.waveSigner = signer;
    this.verifier = verifier;
    this.certPathStore = certPathStore;

    if (disableVerfication) {
      LOG.warning("** SIGNATURE VERIFICATION DISABLED ** "
          + "see flag \"waveserver_disable_verification\"");
    }
  }

  @Override
  public Set<String> getLocalDomains() {

    // TODO: for now, we just support a single signer
    return ImmutableSet.of(waveSigner.getSignerInfo().getDomain());
  }

  @Override
  public WaveSigner getLocalSigner() {
    return waveSigner;
  }

  @Override
  public ProtocolSignedDelta signDelta(ByteStringMessage<ProtocolWaveletDelta> delta) {

    // TODO: support extended address paths. For now, there will be exactly
    // one signature, and we don't support federated groups.
    Preconditions.checkState(delta.getMessage().getAddressPathCount() == 0);

    ProtocolSignedDelta.Builder signedDelta = ProtocolSignedDelta.newBuilder();

    signedDelta.setDelta(delta.getByteString());
    signedDelta.addAllSignature(ImmutableList.of(waveSigner.sign(
        delta.getByteString().toByteArray())));
    return signedDelta.build();
  }

  @Override
  public ByteStringMessage<ProtocolWaveletDelta> verifyDelta(ProtocolSignedDelta signedDelta)
      throws SignatureException, UnknownSignerException {

    ByteStringMessage<ProtocolWaveletDelta> canonicalDelta;
    try {
      canonicalDelta = ByteStringMessage.from(
          ProtocolWaveletDelta.getDefaultInstance(), signedDelta.getDelta());
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalArgumentException("signed delta does not contain valid delta", e);
    }

    if (disableVerfication) {
      return canonicalDelta;
    }

    List<String> domains = getParticipantDomains(canonicalDelta.getMessage());

    if (domains.size() != signedDelta.getSignatureCount()) {
      throw new SignatureException("found " + domains.size() + " domains in " +
          "extended address path, but " + signedDelta.getSignatureCount() +
          " signatures.");
    }

    for (int i = 0; i < domains.size(); i++) {
      String domain = domains.get(i);
      ProtocolSignature signature = signedDelta.getSignature(i);
      verifySingleSignature(canonicalDelta, signature, domain);
    }

    return canonicalDelta;
  }

  /**
   * Verifies a single signature.
   * @param delta the payload that we're verifying the signature on.
   * @param signature the signature on the payload
   * @param domain the authority (domain name) that should have signed the
   *   payload.
   * @throws SignatureException if the signature doesn't verify.
   */
  private void verifySingleSignature(ByteStringMessage<ProtocolWaveletDelta> delta,
      ProtocolSignature signature, String domain) throws SignatureException, UnknownSignerException {
    verifier.verify(delta.getByteString().toByteArray(), signature, domain);
  }

  /**
   * Returns the domains of all the addresses in the extended address path.
   */
  private List<String> getParticipantDomains(ProtocolWaveletDelta delta) {
    Iterable<String> addresses = getExtendedAddressPath(delta);
    return getDeDupedDomains(addresses);
  }

  /**
   * Extracts the domains from user addresses, and removes duplicates.
   */
  private List<String> getDeDupedDomains(Iterable<String> addresses) {
    List<String> domains = Lists.newArrayList();
    for (String address : addresses) {
      String participantDomain = new ParticipantId(address).getDomain();
      if (!domains.contains(participantDomain)) {
        domains.add(participantDomain);
      }
    }
    return domains;
  }

  /**
   * Returns the extended address path, i.e., the addresses in the delta's
   * address path, plus the author of the delta.
   */
  private Iterable<String> getExtendedAddressPath(ProtocolWaveletDelta delta) {
    return Iterables.concat(delta.getAddressPathList(),
        ImmutableList.of(delta.getAuthor()));
  }

  @Override
  public void storeSignerInfo(ProtocolSignerInfo signerInfo)
      throws SignatureException {
    verifier.verifySignerInfo(new SignerInfo(signerInfo));
    certPathStore.put(signerInfo);
  }

  @Override
  public ProtocolSignerInfo retrieveSignerInfo(ByteString signerId) {
    SignerInfo signerInfo = certPathStore.get(signerId.toByteArray());
    return signerInfo == null ? null : signerInfo.toProtoBuf();
  }
}

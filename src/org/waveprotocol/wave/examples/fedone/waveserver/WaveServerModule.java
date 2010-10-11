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

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.waveprotocol.wave.crypto.CachedCertPathValidator;
import org.waveprotocol.wave.crypto.CertPathStore;
import org.waveprotocol.wave.crypto.DefaultCacheImpl;
import org.waveprotocol.wave.crypto.DefaultTimeSource;
import org.waveprotocol.wave.crypto.DefaultTrustRootsProvider;
import org.waveprotocol.wave.crypto.DisabledCertPathValidator;
import org.waveprotocol.wave.crypto.TimeSource;
import org.waveprotocol.wave.crypto.TrustRootsProvider;
import org.waveprotocol.wave.crypto.VerifiedCertChainCache;
import org.waveprotocol.wave.crypto.WaveCertPathValidator;
import org.waveprotocol.wave.crypto.WaveSignatureVerifier;
import org.waveprotocol.wave.examples.fedone.common.HashedVersionFactoryImpl;
import org.waveprotocol.wave.examples.fedone.frontend.ClientFrontend;
import org.waveprotocol.wave.examples.fedone.frontend.ClientFrontendImpl;
import org.waveprotocol.wave.examples.fedone.frontend.WaveClientRpcImpl;
import org.waveprotocol.wave.examples.fedone.util.URLEncoderDecoderBasedPercentEncoderDecoder;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveClientRpc.ProtocolWaveClientRpc;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.version.HashedVersionFactory;

/**
 * Guice Module for the prototype Server.
 *
 */
public class WaveServerModule extends AbstractModule {

  private static class LocalWaveletContainerFactory implements LocalWaveletContainer.Factory {
    @Override
    public LocalWaveletContainer create(WaveletName waveletName) {
      return new LocalWaveletContainerImpl(waveletName);
    }
  }

  private static class RemoteWaveletContainerFactory implements RemoteWaveletContainer.Factory {
    @Override
    public RemoteWaveletContainer create(WaveletName waveletName) {
      return new RemoteWaveletContainerImpl(waveletName);
    }
  }

  private static final IdURIEncoderDecoder URI_CODEC =
      new IdURIEncoderDecoder(new URLEncoderDecoderBasedPercentEncoderDecoder());
  private static final HashedVersionFactory HASH_FACTORY = new HashedVersionFactoryImpl(URI_CODEC);

  @Override
  protected void configure() {
    bind(TimeSource.class).to(DefaultTimeSource.class).in(Singleton.class);
    bind(SignatureHandler.class).toProvider(SignatureHandlerProvider.class);

    try {
      bind(WaveSignatureVerifier.class).toConstructor(WaveSignatureVerifier.class.getConstructor(
          WaveCertPathValidator.class, CertPathStore.class));
      bind(VerifiedCertChainCache.class).to(DefaultCacheImpl.class).in(Singleton.class);
      bind(DefaultCacheImpl.class).toConstructor(
          DefaultCacheImpl.class.getConstructor(TimeSource.class));
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException(e);
    }

    bind(TrustRootsProvider.class).to(DefaultTrustRootsProvider.class).in(Singleton.class);
    bind(CertificateManager.class).to(CertificateManagerImpl.class).in(Singleton.class);
    bind(WaveletProvider.class).to(WaveServerImpl.class).in(Singleton.class);
    bind(WaveBus.class).to(WaveServerImpl.class).in(Singleton.class);
    bind(HashedVersionFactory.class).toInstance(HASH_FACTORY);
    bind(ClientFrontend.class).to(ClientFrontendImpl.class).in(Singleton.class);
    bind(ProtocolWaveClientRpc.Interface.class).to(WaveClientRpcImpl.class).in(Singleton.class);
    bind(LocalWaveletContainer.Factory.class).to(LocalWaveletContainerFactory.class).in(
        Singleton.class);
    bind(RemoteWaveletContainer.Factory.class).to(RemoteWaveletContainerFactory.class).in(
        Singleton.class);
  }

  /**
   * Guice provider of {@code WaveCertPathValidator}s.
   */
  @Provides
  protected WaveCertPathValidator provideWaveCertPathValidator(
      @Named("waveserver_disable_signer_verification") boolean disableSignerVerification,
      TimeSource timeSource, VerifiedCertChainCache certCache,
      TrustRootsProvider trustRootsProvider) {
    if (disableSignerVerification) {
      return new DisabledCertPathValidator();
    } else {
      return new CachedCertPathValidator(certCache, timeSource, trustRootsProvider);
    }
  }
}

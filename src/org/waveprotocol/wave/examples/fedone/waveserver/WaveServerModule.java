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
import com.google.inject.Singleton;

import org.waveprotocol.wave.examples.fedone.waveserver.WaveClientRpc.ProtocolWaveClientRpc;
import org.waveprotocol.wave.model.id.WaveletName;

/**
 * Guice Module for the prototype Server.
 *
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

  @Override
  protected void configure() {
    bind(WaveletProvider.class).to(WaveServer.class);
    bind(WaveServer.class).to(WaveServerImpl.class).in(Singleton.class);
    bind(ClientFrontend.class).to(ClientFrontendImpl.class).in(Singleton.class);
    bind(ProtocolWaveClientRpc.Interface.class).to(WaveClientRpcImpl.class).in(Singleton.class);
    bind(LocalWaveletContainer.Factory.class).to(LocalWaveletContainerFactory.class)
        .in(Singleton.class);
    bind(RemoteWaveletContainer.Factory.class).to(RemoteWaveletContainerFactory.class)
        .in(Singleton.class);
  }
}

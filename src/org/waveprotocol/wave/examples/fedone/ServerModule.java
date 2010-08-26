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

package org.waveprotocol.wave.examples.fedone;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

import org.waveprotocol.wave.examples.fedone.waveserver.WaveServerImpl;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveServerModule;
import org.waveprotocol.wave.federation.xmpp.ComponentPacketTransport;
import org.waveprotocol.wave.federation.xmpp.IncomingPacketHandler;
import org.waveprotocol.wave.federation.xmpp.OutgoingPacketTransport;
import org.waveprotocol.wave.federation.xmpp.XmppDisco;
import org.waveprotocol.wave.federation.xmpp.XmppFederationHost;
import org.waveprotocol.wave.federation.xmpp.XmppFederationRemote;
import org.waveprotocol.wave.federation.xmpp.XmppManager;
import org.waveprotocol.wave.waveserver.federation.FederationHostBridge;
import org.waveprotocol.wave.waveserver.federation.FederationRemoteBridge;
import org.waveprotocol.wave.waveserver.federation.WaveletFederationListener;
import org.waveprotocol.wave.waveserver.federation.WaveletFederationProvider;

import java.util.Arrays;
import java.util.List;

/**
 * Guice Module for the prototype Server.
 *
 *
 */
public class ServerModule extends AbstractModule {

  @Override
  protected void configure() {
    // Receive updates from the outside world, and push them into our local Wave Server.
    bind(WaveletFederationListener.Factory.class).annotatedWith(FederationRemoteBridge.class)
        .to(WaveServerImpl.class);

    // Request history and submit deltas to the outside world *from* our local
    // Wave Server.
    bind(WaveletFederationProvider.class).annotatedWith(FederationRemoteBridge.class)
        .to(XmppFederationRemote.class).in(Singleton.class);

    // Serve updates to the outside world about local waves.
    bind(WaveletFederationListener.Factory.class).annotatedWith(FederationHostBridge.class)
        .to(XmppFederationHost.class).in(Singleton.class);

    // Provide history and respond to submits about our own local waves.
    bind(WaveletFederationProvider.class).annotatedWith(FederationHostBridge.class)
        .to(WaveServerImpl.class);

    bind(XmppDisco.class).in(Singleton.class);
    bind(XmppFederationRemote.class).in(Singleton.class);
    bind(XmppFederationHost.class).in(Singleton.class);

    bind(XmppManager.class).in(Singleton.class);
    bind(IncomingPacketHandler.class).to(XmppManager.class);
    bind(ComponentPacketTransport.class).in(Singleton.class);
    bind(OutgoingPacketTransport.class).to(ComponentPacketTransport.class);

    install(new WaveServerModule());
    bind(String.class).annotatedWith(Names.named("privateKey")).toInstance("");
    bind(String.class).annotatedWith(Names.named("domain")).toInstance("");
    TypeLiteral<List<String>> certs = new TypeLiteral<List<String>>() {};
    bind(certs).annotatedWith(Names.named("certs")).toInstance(Arrays.<String> asList());
  }
}

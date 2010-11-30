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

package org.waveprotocol.wave.federation.xmpp;

import com.google.inject.Inject;

import org.waveprotocol.box.server.util.Log;
import org.waveprotocol.wave.federation.FederationTransport;
import org.xmpp.component.ComponentException;

/**
 * An implementation of {@link FederationManger} for XMPP federation.
 * 
 * @author tad.glines@gmail.com (Tad Glines)
 */
public class XmppFederationTransport implements FederationTransport {
  private static final Log LOG = Log.get(XmppFederationTransport.class);
  private final ComponentPacketTransport transport;
  
  @Inject
  XmppFederationTransport(ComponentPacketTransport transport) {
    this.transport = transport;
  }
  
  @Override
  public void startFederation() {
    try {
      transport.run();
    } catch (ComponentException e) {
      LOG.warning("couldn't connect to XMPP server:", e);
    }
  }
}

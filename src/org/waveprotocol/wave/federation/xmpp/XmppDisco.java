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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.MapMaker;
//import com.google.common.stats.Varz;
//import com.google.common.stats.VarzExport;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.dom4j.Element;
import org.xmpp.packet.IQ;
import org.waveprotocol.wave.federation.FederationErrorProto.FederationError;
import org.waveprotocol.wave.federation.FederationErrors;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Implementation of XMPP Discovery. Provides public methods to respond to
 * incoming disco requests (via {@link XmppManager}), as well as outgoing disco
 * via {{@link #discoverRemoteJid}.
 *
 * @author arb@google.com (Anthony Baxter)
 * @author thorogood@google.com (Sam Thorogood)
 */
public class XmppDisco {
  @SuppressWarnings("unused")
  private static final Logger LOG = Logger.getLogger(XmppDisco.class.getCanonicalName());

  static final String DISCO_INFO_CATEGORY = "collaboration";
  static final String DISCO_INFO_TYPE = "google-wave";

//  @Varz(value = "xmpp-disco-started", key = "domain",
//        docstring = "Number of times disco was started, per domain.")
  public static final Map<String, AtomicLong> varzDiscoStarted =
      new MapMaker().makeComputingMap(
          new Function<String, AtomicLong>() {
            @Override
            public AtomicLong apply(String domain) {
              return new AtomicLong();
            }
          });

//  @Varz(value = "xmpp-disco-success", key = "domain",
//        docstring = "Number of times disco succeeded, per domain.")
  public static final Map<String, AtomicLong> varzDiscoSuccess =
      new MapMaker().makeComputingMap(
          new Function<String, AtomicLong>() {
            @Override
            public AtomicLong apply(String domain) {
              return new AtomicLong();
            }
          });

//  @Varz(value = "xmpp-disco-failed", key = "domain",
//        docstring = "Number of times disco failed, per domain.")
  public static final Map<String, AtomicLong> varzDiscoFailed =
      new MapMaker().makeComputingMap(
          new Function<String, AtomicLong>() {
            @Override
            public AtomicLong apply(String domain) {
              return new AtomicLong();
            }
          });

//  static {
//    VarzExport.exportAll(XmppDisco.class);
//  }

  private static final int RESOLVED_ITEM_TTL_HOURS = 6;

  private final ConcurrentMap<String, RemoteDisco> discoRequests;
  private final String serverDescription;

  private XmppManager manager = null;

  /**
   * Constructor. Note that {@link #setManager} must be called before this class
   * is ready to use.
   */
  @Inject
  public XmppDisco(@Named("xmpp_server_description") String serverDescription) {
    this.serverDescription = serverDescription;
    discoRequests =
        new MapMaker().expiration(RESOLVED_ITEM_TTL_HOURS, TimeUnit.HOURS).makeComputingMap(
            new Function<String, RemoteDisco>() {
              @Override
              public RemoteDisco apply(String domain) {
                return new RemoteDisco(manager, domain);
              }
            });
  }
  /**
   * Set the manager instance for this class. Must be invoked before any other
   * methods are used.
   */
  public void setManager(XmppManager manager) {
    this.manager = manager;
  }

  /**
   * Handles a disco info get from a foreign source. A remote server is trying
   * to ask us what we support. Send back a message identifying as a wave
   * component.
   *
   * @param iq the IQ packet.
   * @param responseCallback
   */
  void processDiscoInfoGet(IQ iq, PacketCallback responseCallback) {
    IQ response = IQ.createResultIQ(iq);
    Element query = response.setChildElement("query", XmppNamespace.NAMESPACE_DISCO_INFO);

    query.addElement("identity")
        .addAttribute("category", DISCO_INFO_CATEGORY)
        .addAttribute("type", DISCO_INFO_TYPE)
        .addAttribute("name", serverDescription);

    query.addElement("feature")
        .addAttribute("var", XmppNamespace.NAMESPACE_WAVE_SERVER);

    responseCallback.run(response);
  }


  /**
   * Handles a disco items get from a foreign XMPP agent. No useful responses,
   * since we're not a domain on it's own: just the wave component.
   *
   * @param iq the IQ packet.
   */
  void processDiscoItemsGet(IQ iq, PacketCallback responseCallback) {
    IQ response = IQ.createResultIQ(iq);
    response.setChildElement("query", XmppNamespace.NAMESPACE_DISCO_ITEMS);
    responseCallback.run(response);
  }

  /**
   * Attempt to discover the remote JID for this domain. Hands control to
   * {@link RemoteDisco}.
   */
  public void discoverRemoteJid(String remoteDomain, SuccessFailCallback<String, String> callback) {
    discoRequests.get(remoteDomain).discoverRemoteJID(callback);
  }

  /**
   * Inject a predetermined result into the disco results map. If the passed jid
   * is null, generate an error/not-found case.
   *
   * @param domain remote domain
   * @param jid remote JID
   * @throws IllegalStateException if there is already a result for this domain
   */
  @VisibleForTesting
  void testInjectInDomainToJidMap(String domain, String jid) {
    FederationError error = null;
    if (jid == null) {
      error = FederationErrors.badRequest("Fake injected error");
    }
    Preconditions.checkState(
        discoRequests.putIfAbsent(domain, new RemoteDisco(domain, jid, error)) == null);
  }

  /**
   * Determine whether a request for the given domain is pending.
   *
   * @param domain remote domain
   * @return true/false
   */
  @VisibleForTesting
  boolean isDiscoRequestPending(String domain) {
    return discoRequests.containsKey(domain) && discoRequests.get(domain).isRequestPending();
  }

  /**
   * Determine whether the disco request for the given domain has been touched
   * or is at all available.
   *
   * @param domain remote domain
   * @return true/false
   */
  @VisibleForTesting
  boolean isDiscoRequestAvailable(String domain) {
    return discoRequests.containsKey(domain);
  }

}

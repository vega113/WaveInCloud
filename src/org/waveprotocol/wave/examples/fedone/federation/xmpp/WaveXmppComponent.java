/*
 * Copyright (C) 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.waveprotocol.wave.examples.fedone.federation.xmpp;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.protobuf.RpcCallback;

import org.dom4j.Element;
import org.jivesoftware.whack.ExternalComponentManager;
import org.waveprotocol.wave.examples.fedone.util.URLEncoderDecoderBasedPercentEncoderDecoder;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveletFederationListener;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.xmpp.component.Component;
import org.xmpp.component.ComponentException;
import org.xmpp.component.ComponentManager;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * The WaveXmppComponent is the interface between a local wave server and
 * foreign wave servers, talking over XMPP via the XMPP Component Protocol
 * (XEP-0114) to a local XMPP server.
 *
 *
 */
@Singleton
public class WaveXmppComponent implements Component,
                                          WaveletFederationListener.Factory {

  private static final Logger logger =
      Logger.getLogger(WaveXmppComponent.class.getCanonicalName());

  /**
   * Used for ID generation
   */
  private static int idSequence = 0;
  private static final SecureRandom idRandom = new SecureRandom();

  /**
   * Options for connecting to the local XMPP server
   */
  private final String serverHostname;
  private final String serverSecret;
  private final String componentName;
  private final String serverPing;
  private final int serverPort;

  /**
   * set in the run() method
   */
  ExternalComponentManager componentManager;
  String componentJID;

  /**
   * helper class instances
   */
  XmppDisco disco;
  private final XmppFederationRemote remote;
  private final XmppFederationHost host;

  /**
   * Request callbacks to be invoked
   */
  protected final Map<String, RpcCallback<Packet>> requestCallbacks;
  /**
   * Packets that might need a retry.
   */
  private final Map<String, Packet> packetsForRetry;

  /**
   * Packets that need to be sent when we go online
   */
  private final List<Packet> offlineQueuedPackets;

  /**
   * Are we connected to the XMPP server?
   */
  private boolean connected = false;

  static final IdURIEncoderDecoder waveletNameEncoder =
      new IdURIEncoderDecoder(
          new URLEncoderDecoderBasedPercentEncoderDecoder());

  /**
   * Constants for namespaces
   */
  static final String NAMESPACE_XMPP_RECEIPTS = "urn:xmpp:receipts";
  static final String NAMESPACE_DISCO_INFO =
      "http://jabber.org/protocol/disco#info";
  static final String NAMESPACE_DISCO_ITEMS =
      "http://jabber.org/protocol/disco#items";
  static final String NAMESPACE_PUBSUB =
      "http://jabber.org/protocol/pubsub";
  static final String NAMESPACE_PUBSUB_EVENT =
      "http://jabber.org/protocol/pubsub#event";
  static final String NAMESPACE_WAVE_SERVER =
      "http://waveprotocol.org/protocol/0.2/waveserver";
  private ExternalComponentManagerFactory managerFactory;
  private final String serverIP;
  private String serverPingKey = null;

  /**
   * Constructor.
   *
   * @param componentName  the name of the component, e.g. "wave"
   * @param serverHostname the name of the XMPP server
   * @param serverSecret   the shared secret of the XMPP server
   * @param serverIP       the IP address of the XMPP server
   * @param serverPort     the port on the XMPP server listening for component
   * @param serverPing     the remote server to ping, or empty/null for none
   * @param managerFactory factory to create ExternalComponentManagers
   * @param host           an instance of XMPPFederationHost
   * @param remote         an instance of XMPPFederationRemote
   * @param disco          an instance of XMPPDisco
   */
  @Inject
  public WaveXmppComponent(@Named("xmpp_component_name")String componentName,
                           @Named("xmpp_server_hostname")String serverHostname,
                           @Named("xmpp_server_secret")String serverSecret,
                           @Named("xmpp_server_ip")String serverIP,
                           @Named("xmpp_server_port")int serverPort,
                           @Named("xmpp_server_ping")String serverPing,
                           ExternalComponentManagerFactory managerFactory,
                           XmppFederationHost host,
                           XmppFederationRemote remote, XmppDisco disco
  ) {
    this.serverHostname = serverHostname;
    this.componentName = componentName;
    this.serverSecret = serverSecret;
    this.serverPort = serverPort;
    this.serverPing = serverPing;
    this.managerFactory = managerFactory;

    requestCallbacks = new HashMap<String, RpcCallback<Packet>>();
    // TODO: set up some sort of retry thread.
    packetsForRetry = new HashMap<String, Packet>();
    offlineQueuedPackets = new ArrayList<Packet>();

    this.remote = remote;
    this.remote.setComponent(this);
    this.host = host;
    this.host.setComponent(this);
    this.disco = disco;
    this.disco.setComponent(this);
    this.serverIP = serverIP;
  }

  /**
   * Bind the component to the XMPP server.
   *
   * @throws org.xmpp.component.ComponentException
   *          if the component couldn't talk to the XMPP server.
   */
  public void run() throws ComponentException {
    componentManager = managerFactory.connect(serverIP, serverPort);
    componentManager.setDefaultSecretKey(serverSecret);
    componentManager.setServerName(serverHostname);
    componentJID = componentName + "." + serverHostname;

    componentManager.addComponent(componentName, this);

  }

  /**
   * Callback when the component is registered with the server and will start
   * receiving packets.
   */
  public void start() {
    logger.info("connected to XMPP server with JID: " + componentName + "."
                + serverHostname);
    connected = true;
    if (serverPing != null && !serverPing.isEmpty()) {
      sendServerPing();
    }
    while (!offlineQueuedPackets.isEmpty()) {
      componentManager.sendPacket(this, offlineQueuedPackets.remove(0));
    }
  }

  /**
   * Discovers remote host, sends a ping.
   */
  private void sendServerPing() {
    disco.discoverRemoteJid(serverPing, new RpcCallback<String>() {
      @Override
      public void run(String targetJID) {
        if (targetJID != null) {
          transmitDummyMessage(targetJID);
        } else {
          System.out
              .println(serverPing + " does not appear to have wave");
        }
      }


    });

  }

  private void transmitDummyMessage(String targetJID) {
    Message message = new Message();
    message.setType(Message.Type.normal);
    message.setTo(targetJID);
    message.setFrom(componentJID);
    message.setID(generateId());
    message.addChildElement("ping", NAMESPACE_WAVE_SERVER);
    message.addChildElement("request", NAMESPACE_XMPP_RECEIPTS);
    logger.info("sending ping message:\n" + message);
    sendPacket(message, false, null);
    serverPingKey = genCallbackKey(message, true);
  }

  /**
   * Called when the component is initialized. Doesn't do anything.
   *
   * @param jid              the component's XMPP address
   * @param componentManager the component manager
   */
  public void initialize(JID jid, ComponentManager componentManager) {
    logger.info("initializing with JID: " + jid);
  }

  /**
   * Called when we lose the connection to the XMPP server.
   */
  public void shutdown() {
    logger.info("disconnected from XMPP server");
    connected = false;
  }


  public String getName() {
    return componentName;
  }

  public String getDescription() {
    return "Google Prototype Wave Server - FedOne";
  }

  XmppDisco getDisco() {
    return disco;
  }

  /**
   * Sends a packet, with optional retry and an optional callback when a
   * response comes in.
   *
   * @param packet   the packet to send
   * @param retry    boolean flag to indicate retries are requested
   * @param callback a callback to invoke when a response comes in, or null
   */
  public void sendPacket(Packet packet, boolean retry,
                         RpcCallback<Packet> callback) {
    logger.info("sent XMPP packet: " + packet);
    if (retry) {
      packetsForRetry.put(genCallbackKey(packet, true), packet);
    }
    if (callback != null) {
      String key = genCallbackKey(packet, true /* request */);
      requestCallbacks.put(key, callback);
    }
    if (connected) {
      componentManager.sendPacket(this, packet);
    } else {
      offlineQueuedPackets.add(packet);
    }
  }

  /**
   * Generate a callback key for a given packet.
   *
   * @param packet  the packet
   * @param request if true, packet is a request, else a response
   * @return a string representing the packet
   */
  String genCallbackKey(Packet packet, boolean request) {
    JID remote;
    if (request) {
      remote = packet.getTo();
    } else {
      remote = packet.getFrom();
    }
    return packet.getClass().getCanonicalName() + ":" + remote + ":"
           + packet.getID();
  }

  /**
   * Send an error response.
   *
   * @param error   the error
   * @param request the original packet
   */
  private void sendErrorResponse(String error, Packet request) {
    // TODO: implement error sending
    // See: http://xmpp.org/rfcs/rfc3920.html#stanzas-error 
//    Class<? extends Packet> rc = request.getClass();
//    Packet response = new Packet();
//    // Hm. copy the existing packet type's class. can I do this without if/else?
//    copyRequestPacketFields(request, response);
//    // TODO: set the error on the packet.
//    sendPacket(response, false, null);
  }

  /**
   * Handles an incoming packet.
   *
   * @param packet the incoming XMPP packet
   */
  public void processPacket(Packet packet) {
    logger.info("received XMPP packet:\n" + packet);
    String packetType = packet.getElement().getQName().getName();
    if (packetType.equals("iq")) {
      processIqPacket((IQ) packet);
    } else if (packetType.equals("message")) {
      processMessage((Message) packet);
    } else {
      // TODO: fix the error code
      sendErrorResponse("unknown", packet);
    }
  }

  /**
   * Dispatch XMPP Messages based on their contents.
   *
   * @param message the Message object
   */
  void processMessage(Message message) {
    if (message.getChildElement("received", NAMESPACE_XMPP_RECEIPTS) != null) {
      processMessageReceipt(message);
    } else if (message.getChildElement("event", NAMESPACE_PUBSUB_EVENT) != null) {
      remote.update(message);
    } else if (message.getChildElement("ping", NAMESPACE_WAVE_SERVER) != null) {
      Message response = new Message();
      response.setType(Message.Type.normal);
      copyRequestPacketFields(message, response);
      response.addChildElement("received", NAMESPACE_XMPP_RECEIPTS);
      sendPacket(response, false, null);
    } else {
      logger.info("got unhandled message: " + message);
    }
  }

  /**
   * Dispatch XMPP IQs based on their contents.
   *
   * @param iq the IQ object
   */
  private void processIqPacket(IQ iq) {
    Element body = iq.getChildElement();
    String iqNamespace = body.getQName().getNamespace().getURI();
    logger.fine("type " + iq.getType() + " namespace " + iqNamespace);
    if (iq.getType().equals(IQ.Type.get)) {
      if (iqNamespace.equals(NAMESPACE_DISCO_INFO)) {
        disco.processDiscoInfoGet(iq);
      } else if (iqNamespace.equals(NAMESPACE_DISCO_ITEMS)) {
        disco.processDiscoItemsGet(iq);
      } else if (iqNamespace.equals(NAMESPACE_PUBSUB)) {
        // history request from foreign fed remote
        processPubsubGet(iq);
      } else {
        // TODO: fix the error code
        sendErrorResponse("unknown iq packet", iq);
      }
    } else if (iq.getType().equals(IQ.Type.result)) {
      if (iqNamespace.equals(NAMESPACE_DISCO_INFO)) {
        disco.processDiscoInfoResult(iq);
      } else if (iqNamespace.equals(NAMESPACE_DISCO_ITEMS)) {
        disco.processDiscoItemsResult(iq);
      } else if (iqNamespace.equals(NAMESPACE_PUBSUB)) {
        // foreign fed host sending either history response or update response
        processPubsubResult(iq);
      } else {
        // TODO: fix the error code
        sendErrorResponse("unknown iq packet", iq);
      }
    } else if (iq.getType().equals(IQ.Type.set)) {
      if (iqNamespace.equals(NAMESPACE_PUBSUB)) {
        // foreign fed remote sending wave updates
        processPubsubSet(iq);
      } else {
        // TODO: fix the error code
        sendErrorResponse("unknown iq packet", iq);
      }
    }
  }

  /**
   * Handles a pubsub get from a foreign federation remote. The only valid
   * messages we should see here are history requests and signer requests.
   *
   * @param iq the IQ packet.
   */
  private void processPubsubGet(IQ iq) {
    Element pubsub = iq.getChildElement();
    Element items = pubsub.element("items");
    if (items != null && items.attribute("node") != null) {
      // TODO: switch on the node name.
      if (items.attributeValue("node").equals("wavelet")) {
        host.processHistoryRequest(iq);
      } else if (items.attributeValue("node").equals("signer")) {
        host.processGetSignerRequest(iq);
      } else {
        logger.warning("unknown pubsub request:\n" + iq);
      }
    } else {
      logger.warning("unknown pubsub request:\n" + iq);
      // TODO: fix the error code
      sendErrorResponse("unknown pubsub request", iq);
    }
  }

  /**
   * Handles a pubsub set from a foreign federation remote. The only valid
   * messages we should see here are wavelet update requests and signer delta
   * posts.
   *
   * @param iq the iq packet
   */
  private void processPubsubSet(IQ iq) {
    Element pubsub = iq.getChildElement();
    Element publishElement = pubsub.element("publish");
    if (publishElement != null
        && publishElement.attributeValue("node") != null) {
      if (publishElement.attributeValue("node").equals("wavelet")) {
        host.processSubmitRequest(iq);
      } else if (publishElement.attributeValue("node").equals("signer")) {
        host.publishPostSignerRequest(iq);
      } else {
        logger.warning("unknown pubsub request:\n + iq");
      }
    } else {
      logger.warning("unknown pubsub request:\n" + iq);
      // TODO: fix the error code
      sendErrorResponse("unknown pubsub request", iq);
    }
  }

  /**
   * Handles a pubsub result from a foreign federation host. The only valid
   * messages here are wavelet update responses, history responses, and
   * signature post results.
   *
   * @param iq the IQ packet
   */
  private void processPubsubResult(IQ iq) {
    Element pubsub = iq.getChildElement();
    if (pubsub.element("items") != null) {
      processHistoryResponse(iq);
      return;
    } else {
      Element item = pubsub.element("publish").element("item");
      if (item != null) {
        if (item.element("submit-response") != null) {
          processSubmitResponse(iq);
          return;
        } else if (item.element("signature-response") != null) {
          processPostSignerResponse(iq);
          return;
        }
      }

    }
    logger.warning("unknown pubsub response:\n" + iq);
  }

  /**
   * Reads a postSigner response off the wire, sends it to the Remote.
   *
   * @param iq wire response
   */
  private void processPostSignerResponse(IQ iq) {
    String key = genCallbackKey(iq, false /* response */);
    if (requestCallbacks.containsKey(key)) {
      requestCallbacks.remove(key).run(iq);
    } else {
      logger.warning("unexpected submit response:\n" + iq);
    }
  }

  /**
   * Reads a history response off the wire, sends it to the Remote.
   *
   * @param iq the wire response
   */
  private void processHistoryResponse(IQ iq) {
    String key = genCallbackKey(iq, false /* response */);
    if (requestCallbacks.containsKey(key)) {
      requestCallbacks.remove(key).run(iq);
    } else {
      logger.warning("unexpected history response:\n" + iq);
    }
  }

  /**
   * Processes a wavelet update response from the wire, cancels the retry for
   * this message
   *
   * @param message the response message (a receipt).
   */
  private void processMessageReceipt(Message message) {
    final String key = genCallbackKey(message, false);
    packetsForRetry.remove(key);
    if (key.equals(serverPingKey)) {
      logger.info("got ping response from " + serverPing + "\n" + message);
    }
  }

  /**
   * Reads a submit response off the wire, triggers the callback for it (in
   * XMPPFederationRemote).
   *
   * @param iq the submit response.
   */
  private void processSubmitResponse(IQ iq) {
    String key = genCallbackKey(iq, false /* response */);
    if (requestCallbacks.containsKey(key)) {
      requestCallbacks.remove(key).run(iq);
    } else {
      logger.warning("unexpected submit response:\n" + iq);
    }
  }

  /**
   * Creates a Federation Host for the given domain for the wave server.
   *
   * @param domain the foreign domain
   * @return the FederationHostForDomain object.
   */
  @Override
  public WaveletFederationListener listenerForDomain(String domain) {
    // TODO: start disco here, instead of in the Host
    return new XmppFederationHostForDomain(domain, this);
  }

  /**
   * Copies relevant request headers to the response.
   *
   * @param request  the request Packet
   * @param response the response Packet
   */
  static void copyRequestPacketFields(Packet request, Packet response) {
    // TODO: check the request and response are the same class.
    response.setID(request.getID());
    // switch From and To.
    response.setFrom(request.getTo());
    response.setTo(request.getFrom());
  }

  /**
   * Generates a unique ID.
   *
   * @return a new string suitable for an ID.
   */
  String generateId() {
    return String.valueOf(idRandom.nextInt(10000) + "-" + idSequence++);
  }

  /**
   * Factory for ExternalComponentManagers. Needed to make the code testable.
   */
  static class ExternalComponentManagerFactory {

    /**
     * Creates a new ExternalComponentManager, connecting the WaveXmppComponent
     * to the XMPP server.
     *
     * @param serverIP   IP address of XMPP server
     * @param serverPort component connection listener port on XMPP server
     * @return the new ExternalComponentManager
     */
    public ExternalComponentManager connect(String serverIP, int serverPort) {
      return new ExternalComponentManager(serverIP, serverPort);
    }
  }
}

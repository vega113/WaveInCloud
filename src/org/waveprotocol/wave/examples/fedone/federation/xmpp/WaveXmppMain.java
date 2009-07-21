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

import com.google.protobuf.ByteString;
import com.google.protobuf.RpcCallback;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.waveprotocol.wave.examples.fedone.waveserver.SubmitResultListener;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveletFederationListener;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveletFederationProvider;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.protocol.common;
import org.xmpp.component.ComponentException;
import org.xmpp.packet.Message;

import java.util.logging.Logger;

/**
 * A simple test main for WaveXmppComponent. Starts up, connects to a local XMPP
 * server, and optionally sends a message to a remote wave server.
 *
 *
 */
public class WaveXmppMain {

  private static final String SERVER_HOSTNAME_OPTION = "h";
  private static final String SERVER_IP_OPTION = "i";
  private static final String SERVER_PORT_OPTION = "p";
  private static final String COMPONENT_NAME_OPTION = "n";
  private static final String TARGET_OPTION = "t";

  // TODO: having this a command line option is not the cleanest
  private static final String SERVER_SECRET_OPTION = "s";

  private static Options options;

  static {
    options = new Options();
    options.addOption(SERVER_HOSTNAME_OPTION, true, "XMPP server hostname");
    options.addOption(SERVER_PORT_OPTION, true, "XMPP server port");
    options.addOption(SERVER_IP_OPTION, true, "XMPP server IP");
    options.addOption(SERVER_SECRET_OPTION, true, "shared secret");
    options.addOption(COMPONENT_NAME_OPTION, true, "name of component");
    options.addOption(TARGET_OPTION, true, "target JID");
  }

  public static void main(String args[])
      throws ParseException, ComponentException {
    WaveXmppComponent server = loadArgs(args);
    server.run();

    // When run as a main program, never exit.
    try {
      Thread.currentThread().join();
    } catch (InterruptedException e) {
      // nothing
    }
  }

  /**
   * Parse command line arguments.
   *
   * @param args argv from command line
   * @return a new WaveXmppComponent
   * @throws ParseException on bad command line args
   */
  static WaveXmppComponent loadArgs(String[] args) throws ParseException {
    CommandLineParser parser = new PosixParser();

    CommandLine cmd;
    cmd = parser.parse(options, args);

    mandatoryOption(cmd, SERVER_HOSTNAME_OPTION,
                    "must supply server XMPP name");
    mandatoryOption(cmd, SERVER_SECRET_OPTION, "must supply server secret");
    mandatoryOption(cmd, COMPONENT_NAME_OPTION, "must supply component name");
    mandatoryOption(cmd, SERVER_PORT_OPTION, "must supply server port");
    mandatoryOption(cmd, SERVER_IP_OPTION, "must supply server IP");

    String serverHostname = cmd.getOptionValue(SERVER_HOSTNAME_OPTION);
    String serverSecret = cmd.getOptionValue(SERVER_SECRET_OPTION);
    String componentName = cmd.getOptionValue(COMPONENT_NAME_OPTION);
    int serverPort = Integer.parseInt(cmd.getOptionValue(SERVER_PORT_OPTION));
    String serverIP = cmd.getOptionValue(SERVER_IP_OPTION);

    XmppFederationRemote remote =
        new XmppFederationRemote(new DummyRemoteFactory());
    XmppFederationHost host = new XmppFederationHost(new DummyHostFactory());
    XmppDisco disco = new XmppDisco();
    WaveXmppMainComponent wavexmpp =
        new WaveXmppMainComponent(componentName, serverHostname, serverSecret,
                                  serverIP, serverPort, host, remote, disco);

    if (cmd.hasOption(TARGET_OPTION)) {
      wavexmpp.remoteServer = cmd.getOptionValue(TARGET_OPTION);
    }

    return wavexmpp;
  }

  /**
   * Checks a mandatory option is set, spits out help and dies if not.
   *
   * @param cmd        parsed options
   * @param option     the option to check
   * @param helpString the error message to emit if not.
   */
  static void mandatoryOption(CommandLine cmd, String option,
                              String helpString) {
    if (!cmd.hasOption(option)) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp(helpString, options);
      System.exit(1);
    }
  }

  private static class DummyRemoteFactory
      implements WaveletFederationListener.Factory {

    public WaveletFederationListener listenerForDomain(String domain) {
      return null;
    }
  }

  static class WaveXmppMainComponent extends WaveXmppComponent {

    private final Logger logger =
        Logger.getLogger(WaveXmppMainComponent.class.getCanonicalName());
    public String remoteServer = null;

    /**
     * Constructor.
     *
     * @param componentName  the name of the component, e.g. "wave"
     * @param serverHostname the name of the XMPP server
     * @param serverSecret   the shared secret of the XMPP server
     * @param serverIP       the IP address of the XMPP servers
     * @param serverPort     the port on the XMPP server listening for component
     *                       connections
     * @param host           an XMPPHost instance
     * @param remote         an XMPPRemote instance
     * @param disco          an XMPPDisco instance
     */
    public WaveXmppMainComponent(String componentName, String serverHostname,
                                 String serverSecret, String serverIP,
                                 int serverPort,
                                 XmppFederationHost host,
                                 XmppFederationRemote remote,
                                 XmppDisco disco) {
      super(componentName, serverHostname, serverSecret, serverIP,
            serverPort,
            "", new WaveXmppComponent.ExternalComponentManagerFactory(),
            host, remote, disco);
    }

    /**
     * Callback when the component is registered with the server and will start
     * receiving packets.
     */
    public void start() {
      super.start();
    }

    /**
     * Test method invoked by the main code.
     */
    private void startDummyPing() {
      // TODO: remove this when it's no longer needed.
      if (remoteServer != null) {
        disco.discoverRemoteJid(remoteServer, new RpcCallback<String>() {
          public void run(String targetJID) {
            if (targetJID != null) {
              sendDummyMessage(targetJID);
            } else {
              System.out
                  .println(remoteServer + " does not appear to have wave");
            }
          }
        });
      }
    }

    /**
     * Silly test method that just pings the remote host.
     *
     * @param toAddr the remote XMPP host.
     */
    private void sendDummyMessage(String toAddr) {
      Message message = new Message();
      message.setType(Message.Type.normal);
      message.setTo(toAddr);
      message.setFrom(componentJID);
      message.setID(generateId());
      message.setBody("hello");
      message.addChildElement("request", NAMESPACE_XMPP_RECEIPTS);
      logger.fine("sending message:\n" + message);
      sendPacket(message, true, null);
    }

  }

  private static class DummyHostFactory implements WaveletFederationProvider {

    @Override
    public void submitRequest(WaveletName waveletName,
                              common.ProtocolSignedDelta delta,
                              SubmitResultListener listener) {
      // whatever
    }

    @Override
    public void requestHistory(WaveletName waveletName, String domain,
                               common.ProtocolHashedVersion startVersion,
                               common.ProtocolHashedVersion endVersion,
                               long lengthLimit,
                               HistoryResponseListener listener) {
      // whatever
    }

    @Override
    public void getDeltaSignerInfo(ByteString signerId, WaveletName waveletName,
                                   common.ProtocolHashedVersion deltaEndVersion,
                                   DeltaSignerInfoResponseListener listener) {
      // whatever
    }

    @Override
    public void postSignerInfo(String destinationDomain,
                               common.ProtocolSignerInfo signerInfo,
                               PostSignerInfoResponseListener listener) {
      // whatever
    }

  }
}

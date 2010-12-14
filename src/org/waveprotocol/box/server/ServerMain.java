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

package org.waveprotocol.box.server;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.name.Names;

import org.apache.commons.configuration.ConfigurationException;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.ProxyServlet;
import org.waveprotocol.box.common.comms.WaveClientRpc.ProtocolWaveClientRpc;
import org.waveprotocol.box.server.authentication.AccountStoreHolder;
import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.box.server.persistence.AccountStore;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.box.server.persistence.PersistenceModule;
import org.waveprotocol.box.server.persistence.SignerInfoStore;
import org.waveprotocol.box.server.robots.RobotApiModule;
import org.waveprotocol.box.server.robots.RobotRegistrationServlet;
import org.waveprotocol.box.server.robots.active.ActiveApiServlet;
import org.waveprotocol.box.server.robots.dataapi.DataApiOAuthServlet;
import org.waveprotocol.box.server.robots.dataapi.DataApiServlet;
import org.waveprotocol.box.server.robots.passive.RobotsGateway;
import org.waveprotocol.box.server.rpc.AttachmentServlet;
import org.waveprotocol.box.server.rpc.AuthenticationServlet;
import org.waveprotocol.box.server.rpc.FetchServlet;
import org.waveprotocol.box.server.rpc.ServerRpcProvider;
import org.waveprotocol.box.server.rpc.SignOutServlet;
import org.waveprotocol.box.server.rpc.UserRegistrationServlet;
import org.waveprotocol.box.server.rpc.WaveClientServlet;
import org.waveprotocol.box.server.waveserver.WaveBus;
import org.waveprotocol.wave.crypto.CertPathStore;
import org.waveprotocol.wave.federation.FederationSettings;
import org.waveprotocol.wave.federation.FederationTransport;
import org.waveprotocol.wave.federation.noop.NoOpFederationModule;
import org.waveprotocol.wave.federation.xmpp.XmppFederationModule;
import org.waveprotocol.wave.util.logging.Log;
import org.waveprotocol.wave.util.settings.SettingsBinder;

import java.io.IOException;


/**
 * Wave Server entrypoint.
 */
public class ServerMain {

  /**
   * This is the name of the system property used to find the server config file.
   */
  private static final String PROPERTIES_FILE_KEY = "wave.server.config";

  private static final Log LOG = Log.get(ServerMain.class);

  public static void main(String... args) {
    try {
      Module coreSettings = SettingsBinder.bindSettings(PROPERTIES_FILE_KEY, CoreSettings.class);
      run(coreSettings);
      return;
    } catch (IOException e) {
      LOG.severe("IOException when running server:", e);
    } catch (PersistenceException e) {
      LOG.severe("PersistenceException when running server:", e);
    } catch (ConfigurationException e) {
      LOG.severe("ConfigurationException when running server:", e);
    }
  }

  public static void run(Module coreSettings) throws IOException, PersistenceException,
      ConfigurationException {
    Injector settingsInjector = Guice.createInjector(coreSettings);

    boolean enableFederation =
        settingsInjector.getInstance(Key.get(Boolean.class,
            Names.named(CoreSettings.ENABLE_FEDERATION)));
    Module federationModule;
    if (enableFederation) {
      Module federationSettings =
          SettingsBinder.bindSettings(PROPERTIES_FILE_KEY, FederationSettings.class);
      settingsInjector = settingsInjector.createChildInjector(federationSettings);
      federationModule = settingsInjector.getInstance(XmppFederationModule.class);
    } else {
      federationModule = settingsInjector.getInstance(NoOpFederationModule.class);
    }

    PersistenceModule persistenceModule = settingsInjector.getInstance(PersistenceModule.class);

    Injector injector =
        settingsInjector.createChildInjector(new ServerModule(enableFederation), new RobotApiModule(),
            federationModule, persistenceModule);

    AccountStore accountStore = injector.getInstance(AccountStore.class);
    accountStore.initializeAccountStore();
    AccountStoreHolder.init(accountStore,
        injector.getInstance(Key.get(String.class, Names.named(CoreSettings.WAVE_SERVER_DOMAIN))));

    // Initialize the SignerInfoStore
    CertPathStore certPathStore = injector.getInstance(CertPathStore.class);
    if (certPathStore instanceof SignerInfoStore) {
      ((SignerInfoStore)certPathStore).initializeSignerInfoStore();
    }

    ServerRpcProvider server = injector.getInstance(ServerRpcProvider.class);

    server.addServlet("/attachment/*", injector.getInstance(AttachmentServlet.class));

    server
        .addServlet(SessionManager.SIGN_IN_URL, injector.getInstance(AuthenticationServlet.class));
    server.addServlet("/auth/signout", injector.getInstance(SignOutServlet.class));
    server.addServlet("/auth/register", injector.getInstance(UserRegistrationServlet.class));

    server.addServlet("/fetch/*", injector.getInstance(FetchServlet.class));

    server.addServlet("/robot/dataapi", injector.getInstance(DataApiServlet.class));
    server.addServlet(DataApiOAuthServlet.DATA_API_OAUTH_PATH + "/*",
        injector.getInstance(DataApiOAuthServlet.class));
    server.addServlet("/robot/dataapi/rpc", injector.getInstance(DataApiServlet.class));
    server.addServlet("/robot/register/*", injector.getInstance(RobotRegistrationServlet.class));
    server.addServlet("/robot/rpc", injector.getInstance(ActiveApiServlet.class));

    String gadgetServerHostname =
        injector.getInstance(Key.get(String.class, Names.named(CoreSettings.GADGET_SERVER_HOSTNAME)));
    ProxyServlet.Transparent proxyServlet =
        new ProxyServlet.Transparent("/gadgets", "http", gadgetServerHostname,
            injector.getInstance(Key.get(int.class, Names.named(CoreSettings.GADGET_SERVER_PORT))), "/gadgets");
    ServletHolder proxyServletHolder = server.addServlet("/gadgets/*", proxyServlet);
    proxyServletHolder.setInitParameter("HostHeader", gadgetServerHostname);

    server.addServlet("/", injector.getInstance(WaveClientServlet.class));

    RobotsGateway robotsGateway = injector.getInstance(RobotsGateway.class);
    WaveBus waveBus = injector.getInstance(WaveBus.class);
    waveBus.subscribe(robotsGateway);

    ProtocolWaveClientRpc.Interface rpcImpl =
        injector.getInstance(ProtocolWaveClientRpc.Interface.class);
    server.registerService(ProtocolWaveClientRpc.newReflectiveService(rpcImpl));

    FederationTransport federationManager = injector.getInstance(FederationTransport.class);
    federationManager.startFederation();

    LOG.info("Starting server");
    server.startWebSocketServer();
  }
}

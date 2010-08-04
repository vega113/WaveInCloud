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

package org.waveprotocol.wave.examples.fedone;

/**
 * Flags configuration for XMPP module.
 *
 *
 */
// TODO - add descriptions to all flags.
@SuppressWarnings("unused") // We inject them by the name of their flag
public class FlagSettings {
  @Flag(name="xmpp_server_hostname")
  private static String xmppServerHostname;

  @Flag(name="xmpp_server_secret")
  private static String xmppServerSecret;

  @Flag(name="xmpp_component_name")
  private static String xmppComponentName;

  @Flag(name="xmpp_server_port")
  private static int xmppServerPort;

  @Flag(name="xmpp_server_ip")
  private static String xmppServerIp;

  @Flag(name="xmpp_server_ping")
  private static String xmppServerPing;

  @Flag(name="client_frontend_hostname")
  private static String clientFrontendHost;

  @Flag(name="client_frontend_port")
  private static String clientFrontEndPort;

  @Flag(name="http_frontend_hostname", defaultValue="localhost")
  private static String httpFrontendHost;

  @Flag(name="http_frontend_port", defaultValue="9898")
  private static int httpFrontEndPort;

  @Flag(name="certificate_private_key")
  private static String certificatePrivKey;

  @Flag(name="certificate_files", description="comma separated WITH NO SPACES.")
  private static String certificateFiles;

  @Flag(name="certificate_domain")
  private static String certificateDomain;

  @Flag(name="waveserver_disable_verification")
  private static boolean waveserverDisableVerification;

  @Flag(name="waveserver_disable_signer_verification")
  private static boolean waveserverDisableSignerVerification;

  @Flag(name="xmpp_server_description")
  private static String xmppServerDescription;

  // default value is 5 minutes
  @Flag(name="xmpp_disco_failed_expiry_secs", defaultValue="300")
  private static int xmppDiscoFailedExpirySecs;

  // default value is 2 hours
  @Flag(name="xmpp_disco_successful_expiry_secs", defaultValue="7200")
  private static int xmppDiscoSuccessfulExpirySecs;

  @Flag(name="xmpp_jid")
  private static String xmppJid;

  /*set to true to enable or false to disable persistence*/
  @Flag(name="waveserver_enable_persistence", defaultValue="false")
  private static boolean waveserverEnablePersistence;
}

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

/**
 * Flags configuration for FedOne.
 */
// TODO - add descriptions to all flags.
@SuppressWarnings("unused") // We inject them by the name of their flag
public class FlagSettings {
  @Flag(name = "xmpp_server_hostname")
  private static String xmppServerHostname;

  @Flag(name = "xmpp_server_secret")
  private static String xmppServerSecret;

  @Flag(name = "xmpp_component_name")
  private static String xmppComponentName;

  @Flag(name = "xmpp_server_port")
  private static int xmppServerPort;

  @Flag(name = "xmpp_server_ip")
  private static String xmppServerIp;

  @Flag(name = "xmpp_server_ping")
  private static String xmppServerPing;

  @Flag(name = "wave_server_domain")
  private static String waveServerDomain;

  @Flag(name = "http_frontend_public_address",
      defaultValue = "localhost:9898",
      description = "The server's public address.")
  private static String httpFrontEndPublicAddress;

  @Flag(name = "http_frontend_addresses",
      defaultValue = "localhost:9898",
      description = "A comman seperated list of address on which to listen for connections."+
                    " Each address is a host or ip and port seperated by a colon.")
  private static String httpFrontEndAddresses;

  @Flag(name = "certificate_private_key")
  private static String certificatePrivKey;

  @Flag(name = "certificate_files", description = "comma separated WITH NO SPACES.")
  private static String certificateFiles;

  @Flag(name = "certificate_domain")
  private static String certificateDomain;

  @Flag(name = "waveserver_disable_verification")
  private static boolean waveserverDisableVerification;

  @Flag(name = "waveserver_disable_signer_verification")
  private static boolean waveserverDisableSignerVerification;

  @Flag(name = "enable_federation", defaultValue = "true")
  private static boolean enableFederation;

  @Flag(name = "xmpp_server_description")
  private static String xmppServerDescription;

  // default value is 5 minutes
  @Flag(name = "xmpp_disco_failed_expiry_secs", defaultValue = "300")
  private static int xmppDiscoFailedExpirySecs;

  // default value is 2 hours
  @Flag(name = "xmpp_disco_successful_expiry_secs", defaultValue = "7200")
  private static int xmppDiscoSuccessfulExpirySecs;

  @Flag(name = "xmpp_jid")
  private static String xmppJid;

  @Flag(name = "cert_path_store_type",
      description = "Type of persistence to use for the Certificate Storage",
      defaultValue = "memory")
  private static String certPathStoreType;
  
  @Flag(name = "attachment_store_type",
      description = "Type of persistence store to use for attachments",
      defaultValue = "disk")
  private static String attachmentStoreType;

  @Flag(name = "attachment_store_directory",
      description = "Location on disk where the attachment store lives. Must be writeable by the "
          + "fedone process. Only used by disk-based attachment store.",
      defaultValue = "_attachments")
  private static String attachmentStoreDirectory;

  @Flag(name = "account_store_type",
      description = "Type of persistence to use for the Account Storage",
      defaultValue = "memory")
  private static String accountStoreType;
}

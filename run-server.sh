#!/bin/bash

# This script will start the federated wave server.
# Please see http://code.google.com/p/wave-protocol/wiki/Installation for
# instructions on how to configure the flags.
#
# The following is an example configuration, please take care to configure
# your instance correctly.

PRIVATE_KEY_FILENAME=../certs/local.key
CERTIFICATE_FILENAME_LIST=../certs/local.cert
CERTIFICATE_DOMAIN_NAME=localhost

XMPP_SERVER_HOSTNAME=$CERTIFICATE_DOMAIN_NAME
XMPP_SERVER_IP=$XMPP_SERVER_HOSTNAME
XMPP_SERVER_SECRET="opensesame"

java -jar dist/fedone-0.2.jar \
  --client_frontend_hostname=127.0.0.1 \
  --client_frontend_port=9876 \
  --xmpp_component_name=wave \
  --xmpp_server_hostname=$XMPP_SERVER_HOSTNAME \
  --xmpp_server_ip=$XMPP_SERVER_IP \
  --xmpp_server_port=5275 \
  --xmpp_server_secret $XMPP_SERVER_SECRET \
  --xmpp_server_ping="" \
  --certificate_private_key=$PRIVATE_KEY_FILENAME \
  --certificate_files=$CERTIFICATE_FILENAME_LIST \
  --certificate_domain=$CERTIFICATE_DOMAIN_NAME \
  --waveserver_disable_verification=true \
  --waveserver_disable_signer_verification=true

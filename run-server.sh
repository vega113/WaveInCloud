#!/bin/bash

# This script will start the FedOne wave server.
#

if [ -f run-config.sh ] ; then
  . run-config.sh
else
  echo "You need to copy run-config.sh.example to run-config.sh and configure" ; exit 1
fi

java -jar dist/fedone-server-$FEDONE_VERSION.jar \
  --client_frontend_hostname=$WAVE_SERVER_HOSTNAME \
  --client_frontend_port=$WAVE_SERVER_PORT \
  --xmpp_component_name=wave \
  --xmpp_server_hostname=$XMPP_SERVER_HOSTNAME \
  --xmpp_server_ip=$XMPP_SERVER_IP \
  --xmpp_server_port=$XMPP_SERVER_PORT \
  --xmpp_server_secret $XMPP_SERVER_SECRET \
  --xmpp_server_ping=$XMPP_SERVER_PING \
  --certificate_private_key=$PRIVATE_KEY_FILENAME \
  --certificate_files=$CERTIFICATE_FILENAME_LIST \
  --certificate_domain=$CERTIFICATE_DOMAIN_NAME \
  --waveserver_disable_verification=$WAVESERVER_DISABLE_VERIFICATION \
  --waveserver_disable_signer_verification=$WAVESERVER_DISABLE_SIGNER_VERIFICATION

#!/bin/bash

# This script will start the Wave in a Box server.
#

if [ -f run-config.sh ] ; then
  . run-config.sh
else
  . run-nofed-config.sh
fi

. process-script-args.sh

exec java $DEBUG_FLAGS \
  -Dorg.eclipse.jetty.util.log.DEBUG=true \
  -Djava.security.auth.login.config=jaas.config \
  -jar dist/waveinabox-server-$WAVEINABOX_VERSION.jar \
  --wave_server_domain=$WAVE_SERVER_DOMAIN_NAME \
  --xmpp_component_name=wave \
  --xmpp_jid=wave.${WAVE_SERVER_DOMAIN_NAME} \
  --xmpp_server_description="Wave in a Box" \
  --xmpp_server_hostname=$XMPP_SERVER_HOSTNAME \
  --xmpp_server_ip=$XMPP_SERVER_IP \
  --xmpp_server_port=$XMPP_SERVER_PORT \
  --xmpp_server_secret $XMPP_SERVER_SECRET \
  --xmpp_server_ping=$XMPP_SERVER_PING \
  --certificate_private_key=$PRIVATE_KEY_FILENAME \
  --certificate_files=$CERTIFICATE_FILENAME_LIST \
  --certificate_domain=$CERTIFICATE_DOMAIN_NAME \
  --waveserver_disable_verification=$WAVESERVER_DISABLE_VERIFICATION \
  --waveserver_disable_signer_verification=$WAVESERVER_DISABLE_SIGNER_VERIFICATION \
  --http_frontend_public_address=$HTTP_SERVER_PUBLIC_ADDRESS \
  --http_frontend_addresses=$HTTP_SERVER_ADDRESSES \
  --enable_federation=$ENABLE_FEDERATION \
  --cert_path_store_type=$CERT_PATH_STORE_TYPE \
  --cert_path_store_directory=$CERT_PATH_STORE_DIRECTORY \
  --attachment_store_type=$ATTACHMENT_STORE_TYPE \
  --attachment_store_directory=$ATTACHMENT_STORE_DIRECTORY \
  --account_store_type=$ACCOUNT_STORE_TYPE \
  --account_store_directory=$ACCOUNT_STORE_DIRECTORY \
  --use_socketio=$USE_SOCKETIO

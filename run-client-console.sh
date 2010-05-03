#!/bin/bash

# This script will start the FedOne wave client.
#

if [ -f run-config.sh ] ; then
  . run-config.sh
else
  echo "You need to copy run-config.sh.example to run-config.sh and configure" ; exit 1
fi

if [ -z "$WEBSOCKET_SERVER_PORT" -o -z "$WEBSOCKET_SERVER_HOSTNAME" ]; then
  echo "You need to specity WEBSOCKET_SERVER_HOSTNAME and WEBSOCKET_SERVER_PORT in run-config.sh"; exit 1
fi

if [[ $# != 1 ]]; then
  echo "usage: ${0} <username EXCLUDING DOMAIN>"
else
  USER_NAME=$1@$WAVE_SERVER_DOMAIN_NAME
  echo "running client as user: ${USER_NAME}"
  exec java -jar dist/fedone-client-console-$FEDONE_VERSION.jar $USER_NAME $WEBSOCKET_SERVER_HOSTNAME $WEBSOCKET_SERVER_PORT
fi

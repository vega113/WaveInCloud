#!/bin/bash

# This script will start the Echoey the Wave in a Box wave agent.
#

if [ -f run-config.sh ] ; then
  . run-config.sh
else
  echo "You need to copy run-config.sh.example to run-config.sh and configure" ; exit 1
fi

if [ -z "$WEBSOCKET_SERVER_PORT" -o -z "$WEBSOCKET_SERVER_HOSTNAME" ]; then
  echo "You need to specity WEBSOCKET_SERVER_HOSTNAME and WEBSOCKET_SERVER_PORT in run-config.sh"; exit 1
fi

. process-script-args.sh

USER_NAME=echoey@$WAVE_SERVER_DOMAIN_NAME
echo "running agent.echoey as user: ${USER_NAME}"
exec java $DEBUG_FLAGS -jar dist/waveinabox-agent-echoey-$WAVEINABOX_VERSION.jar $USER_NAME $WEBSOCKET_SERVER_HOSTNAME $WEBSOCKET_SERVER_PORT

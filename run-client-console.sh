#!/bin/bash

# This script will start the Wave in a Box wave client.
#

if [ -f run-config.sh ] ; then
  . run-config.sh
else
  . run-nofed-config.sh
fi

. process-script-args.sh

if [[ $ARGC != 1 ]]; then
  echo "usage: ${0} <username EXCLUDING DOMAIN>"
else
  USER_NAME=${ARGV[0]}@$WAVE_SERVER_DOMAIN_NAME
  echo "running client as user: ${USER_NAME}"
  exec java $DEBUG_FLAGS -jar dist/waveinabox-client-console-$WAVEINABOX_VERSION.jar $USER_NAME $HTTP_SERVER_PUBLIC_ADDRESS
fi

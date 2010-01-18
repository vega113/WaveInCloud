#!/bin/bash

# This script will start the Echoey the FedOne wave agent.
#

if [ -f run-config.sh ] ; then
  . run-config.sh
else
  echo "You need to copy run-config.sh.example to run-config.sh and configure" ; exit 1
fi

PROBEY_PORT=8090

USER_NAME=probey@$WAVE_SERVER_DOMAIN_NAME
echo "running agent.probey as user: ${USER_NAME}"
exec java -jar dist/fedone-agent-probey-$FEDONE_VERSION.jar $USER_NAME $WAVE_SERVER_HOSTNAME $WAVE_SERVER_PORT $PROBEY_PORT

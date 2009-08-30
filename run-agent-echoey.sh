#!/bin/bash

# This script will start the Echoey the FedOne wave agent.
#

if [ -f run-config.sh ] ; then
  . run-config.sh
else
  echo "You need to copy run-config.sh.example to run-config.sh and configure" ; exit 1
fi

USER_NAME=echoey@$WAVE_SERVER_DOMAIN_NAME
echo "running agent.echoey as user: ${USER_NAME}"
java -jar dist/fedone-agent-echoey-$FEDONE_VERSION.jar $USER_NAME $WAVE_SERVER_HOSTNAME $WAVE_SERVER_PORT

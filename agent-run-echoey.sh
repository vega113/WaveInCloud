#!/bin/bash

# This script will start the Echoey the wave agent.
#
# The following is an example configuration, please take care to configure
# your instance correctly.

# Comment out the next line by putting a # at the front, once you have
# changed the flags.

echo "You need to edit the run-echoey.sh script" ; exit 0

# Use the same flag for the domain name as CERTIFICATE_DOMAIN_NAME in
# run-server.sh.

WAVE_SERVER_DOMAIN_NAME=localhost
WAVE_SERVER_HOSTNAME=127.0.0.1
WAVE_SERVER_PORT=9876

USER_NAME=echoey@$WAVE_SERVER_DOMAIN_NAME
echo "running agent.echoey as user: ${USER_NAME}"
java -jar dist/fedone-echoey-0.2.jar $USER_NAME $WAVE_SERVER_HOSTNAME $WAVE_SERVER_PORT

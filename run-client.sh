#!/bin/sh

# This script will start the federated wave server.
# Please see http://code.google.com/p/wave-protocol/wiki/Installation for
# instructions on how to configure the flags.
#
# The following is an example configuration, please take care to configure
# your instance correctly.

# Comment out the next line by putting a # at the front, once you have
# changed the flags.

echo "You need to edit the run-client.sh script" ; exit 0

# Use the same flag for the domain name as CERTIFICATE_DOMAIN_NAME in
# run-server.sh.
WAVE_SERVER_DOMAIN_NAME=yourdomainnamehere
WAVE_SERVER_HOSTNAME=127.0.0.1
WAVE_SERVER_PORT=9876

if [[ $# != 1 ]]; then
  echo "usage: ${0} <username EXCLUDING DOMAIN>"
else
  USER_NAME=$1@$WAVE_SERVER_DOMAIN_NAME
  echo "running client as user: ${USER_NAME}"
  java -jar dist/fedone-client-0.2.jar $USER_NAME $WAVE_SERVER_HOSTNAME $WAVE_SERVER_PORT
fi

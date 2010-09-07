#!/bin/bash                                                                       

# This script will make a set of certificates for federation.
# To actually federate, the certificiates will need to be signed.
#
# For instructions, see this wiki page:
# http://code.google.com/p/wave-protocol/wiki/Certificates

NAME=$1

if [ "$NAME" == '' ]
then
  echo "Usage: $0 <domain name>" 1>&2
  echo "See http://code.google.com/p/wave-protocol/wiki/Certificates\
 for more information" 1>&2
  exit 1
fi

echo "1) Generating key for $NAME in '$NAME.key' ..."
echo
openssl genrsa 2048 | openssl pkcs8 -topk8 -nocrypt -out "$NAME.key"

echo
echo "2) Generating certificate request for $NAME in '$NAME.crt' ..."
echo
openssl req -new -x509 -nodes -sha1 -days 365 -key "$NAME.key" -out "$NAME.crt"

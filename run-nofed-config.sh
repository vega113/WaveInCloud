#!/bin/bash

# Configuration for Wave in a Box run scripts without federation support.  To add
# federation support see run-config.sh.example.

# Domain name of the wave server 
WAVE_SERVER_DOMAIN_NAME=`hostname`

# A comma separated list of address on which to listen for connections.
# Each address is a comma separated host:port pair.
HTTP_SERVER_PUBLIC_ADDRESS=`hostname`:9898
HTTP_SERVER_ADDRESSES=${HTTP_SERVER_PUBLIC_ADDRESS}

# The version of Wave in a Box, extracted from the build.properties file
WAVEINABOX_VERSION=`grep ^waveinabox.version= build.properties | cut -f2 -d=`

# Disabled federation, as promised.
ENABLE_FEDERATION=false

# These are not used but have to be set to non-empty values.
XMPP_SERVER_SECRET=opensesame
PRIVATE_KEY_FILENAME=${WAVE_SERVER_DOMAIN_NAME}.key
CERTIFICATE_FILENAME_LIST=${WAVE_SERVER_DOMAIN_NAME}.crt
CERTIFICATE_DOMAIN_NAME=$WAVE_SERVER_DOMAIN_NAME
XMPP_SERVER_HOSTNAME=$WAVE_SERVER_DOMAIN_NAME
XMPP_SERVER_PORT=5275
XMPP_SERVER_PING=wavesandbox.com
XMPP_SERVER_IP=$XMPP_SERVER_HOSTNAME
WAVESERVER_DISABLE_VERIFICATION=true
WAVESERVER_DISABLE_SIGNER_VERIFICATION=true

# Settings for the different persistence stores. Currently supported: 'memory' and 'mongodb'
CERT_PATH_STORE_TYPE=memory

# Currently supported attachment types: mongodb, disk
ATTACHMENT_STORE_TYPE=disk

# The location where attachments are stored on disk. This should be changed.
# Note: This is only used when using the disk attachment store. It is ignored
# for other data store types.
ATTACHMENT_STORE_DIRECTORY=_attachments

# Currently supported Account store types: memory
ACCOUNT_STORE_TYPE=mongodb

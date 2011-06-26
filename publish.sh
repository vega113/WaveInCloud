#!/bin/bash

ant clean
ant compile-gwt
ant dist-server
rm ./dist/publish/publish.zip
mkdir ./dist/publish
zip -r ./dist/publish/publish.zip -xi ./dist/waveinabox-server-0.3.jar ./war/* ./*.properties
synchronize.sh UP waveincloud/publish/dist/$(date +%b-%d) ./dist/publish/publish.zip
s3cmd setacl --acl-public s3://waveincloud/publish/dist/$(date +%b-%d)/publish.zip
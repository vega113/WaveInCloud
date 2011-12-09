#!/bin/bash

ant clean
ant compile-gwt-demo
ant dist-server
rm ./dist/publish/publish.zip
mkdir ./dist/publish
zip -r ./dist/publish/publish.zip -xi ./dist/waveinabox-server-0.3.jar ./war/* ./*.properties ./*.sh
synchronize.sh UP waveincloud/publish/dist/$(date --universal  +%b-%d) ./dist/publish/publish.zip
# Make sure to set the synchronize property to PUBLIC-READ. If you don't want to, try to use s3cmd to set ACL.
#s3cmd setacl --acl-public s3://waveincloud/publish/dist/$(date --universal  +%b-%d)/publish.zip
ssh -i ~/laptop.pem ubuntu@waveinabox.net 'sudo chmod a+x /var/wave/run-server.sh; sudo ~/updateWiab.sh '

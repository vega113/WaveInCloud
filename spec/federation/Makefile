# Copyright 2009 Google Inc. All Rights Reserved.
# Author: ohler@google.com (Christian Ohler)
JING=~/src/jing-20081028/bin/jing.jar

default: wavespec.html

.PHONY: clean
clean:
	-rm wavespec.html waveschema.xml

wavespec.html: wavespec.xml waveschema.xml waveproto.xml
# If there's a syntax error, xml2rfc will, by default, bring up a
# dialog window if DISPLAY is set.  We just want the message on
# stderr, so we run it without DISPLAY.
	DISPLAY= xml2rfc $< $@

waveschema.xml: waveschema.rnc
	echo "<![CDATA[" >waveschema.xml
	cat waveschema.rnc >>waveschema.xml
	echo "]]>" >>waveschema.xml

waveproto.xml: ../src/org/waveprotocol/wave/protocol/common.proto
	echo "<![CDATA[" >waveproto.xml
	cat ../src/org/waveprotocol/wave/protocol/common.proto >>waveproto.xml
	echo "]]>" >>waveproto.xml

testrnc:
	java -jar $(JING)  -c waveschema.rnc test/*.xml

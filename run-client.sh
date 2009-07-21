#!/bin/sh

export CLASSPATH="build/core:third_party/jline-0.9.94.jar:third_party/google-collect-1.0-rc2.jar:third_party/protobuf-java-2.1.0.jar"
java org.waveprotocol.wave.examples.fedone.waveclient.console.ConsoleClient $@

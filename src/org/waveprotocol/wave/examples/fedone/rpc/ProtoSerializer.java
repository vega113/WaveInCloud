// Copyright 2010 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.examples.fedone.rpc;

import com.google.protobuf.Message;

import com.dyuproject.protostuff.json.ReflectionNumericJSON;

import org.waveprotocol.wave.examples.fedone.waveserver.WaveClientRpc;
import org.waveprotocol.wave.federation.Proto;

/**
 * A wrapper around the json protobuf serialization.
 * 
 * @author josephg@gmail.com (Joseph Gentle)
 */
public class ProtoSerializer extends ReflectionNumericJSON {
  @SuppressWarnings("unchecked")
  public static final Class<? extends Message>[] MODULE_CLASSES = new Class[]{
    WaveClientRpc.ProtocolOpenRequest.class,
    WaveClientRpc.ProtocolSubmitRequest.class,
    WaveClientRpc.ProtocolSubmitResponse.class,
    WaveClientRpc.ProtocolWaveClientRpc.class,
    WaveClientRpc.ProtocolWaveletUpdate.class,
    WaveClientRpc.WaveletSnapshot.class,
    WaveClientRpc.DocumentSnapshot.class,
    WaveClientRpc.WaveSnapshot.class,
    Proto.ProtocolAppliedWaveletDelta.class,
    Proto.ProtocolDocumentOperation.class,
    Proto.ProtocolDocumentOperation.Component.class,
    Proto.ProtocolDocumentOperation.Component.KeyValuePair.class,
    Proto.ProtocolDocumentOperation.Component.KeyValueUpdate.class,
    Proto.ProtocolDocumentOperation.Component.ElementStart.class,
    Proto.ProtocolDocumentOperation.Component.ReplaceAttributes.class,
    Proto.ProtocolDocumentOperation.Component.UpdateAttributes.class,
    Proto.ProtocolDocumentOperation.Component.AnnotationBoundary.class,
    Proto.ProtocolHashedVersion.class,
    Proto.ProtocolSignature.class,
    Proto.ProtocolSignedDelta.class,
    Proto.ProtocolSignerInfo.class,
    Proto.ProtocolWaveletDelta.class,
    Proto.ProtocolWaveletOperation.class,
    Proto.ProtocolWaveletOperation.MutateDocument.class,
    Rpc.CancelRpc.class,
    Rpc.RpcFinished.class,
  };
  
  public ProtoSerializer() {
    super(MODULE_CLASSES);
  }
}

package org.waveprotocol.wave.examples.fedone.rpc;

import com.google.protobuf.Message;
import com.google.protobuf.UnknownFieldSet;

public interface ProtoCallback {
  public void message(long sequenceNo, Message message);
  public void unknown(long sequenceNo, String messageType, Object message);
}


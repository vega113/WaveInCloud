package org.waveprotocol.wave.examples.fedone.rpc;

import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.protobuf.JsonFormat;
import com.google.protobuf.Message;

import org.waveprotocol.wave.examples.fedone.util.Log;

import org.eclipse.jetty.websocket.WebSocket;

public abstract class WebSocketChannel extends MessageExpectingChannel {
  private static final Log LOG = Log.get(WebSocketChannel.class);
  private static final int VERSION = 0;

  private final ProtoCallback callback;
  private Gson gson = new Gson();
  
  public WebSocketChannel(ProtoCallback callback) {
    this.callback = callback;
  }
  
  public static class MessageWrapper {
    private int version;
    private long sequenceNumber;
    private String messageType;
    private String messageJson;
    
    MessageWrapper() {
      // no-args constructor
    }
    MessageWrapper(int v, long sNo, String mType, String mJson) {
      v = version;
      sequenceNumber = sNo;
      messageType = mType;
      messageJson = mJson;
    }
  }
  
  public void handleMessageString(String data) {
    MessageWrapper wrapper = null;
    try {
      wrapper = gson.fromJson(data, MessageWrapper.class);
    } catch (JsonParseException jpe) {
      throw new IllegalStateException("Unable to parse JSON.", jpe);
    }
    
    if (wrapper.version != VERSION) {
      throw new IllegalStateException("Bad message version number: " + wrapper.version);
    }
    
    Message prototype = getMessagePrototype(wrapper.messageType);
    if (prototype == null) {
      LOG.info("Received misunderstood message (??? " + wrapper.messageType + " ???, seq "
          + wrapper.sequenceNumber + ") from: " + this);
      callback.unknown(wrapper.sequenceNumber, wrapper.messageType, wrapper.messageJson);
    } else {
      Message.Builder builder = prototype.newBuilderForType();
      try {
        JsonFormat.merge(wrapper.messageJson, builder);
        callback.message(wrapper.sequenceNumber, builder.build());        
      } catch (JsonFormat.ParseException pe) {
        LOG.info("Unable to parse message ("+wrapper.messageType+", seq "
          + wrapper.sequenceNumber + ") from: " + this + " -- " + wrapper.messageJson);
        callback.unknown(wrapper.sequenceNumber, wrapper.messageType, wrapper.messageJson);
      }
    }
  }
  
  /**
   * Send the given data String
   *
   * @param data
   */
  protected abstract void sendMessageString(String data);
  
  /**
   * Send the given message across the connection along with the sequence number.
   * 
   * @param sequenceNo
   * @param message
   */
  public void sendMessage(long sequenceNo, Message message) {
    sendMessageString(gson.toJson(new MessageWrapper(
      VERSION, sequenceNo, message.getDescriptorForType().getFullName(),
      JsonFormat.printToString(message))));      
  }
}
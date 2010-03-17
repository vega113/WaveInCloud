package org.waveprotocol.wave.examples.fedone.rpc;

import org.waveprotocol.wave.examples.fedone.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ExecutorService;

public class WebSocketClientRpcChannel extends ClientRpcChannel {
  private static final Log LOG = Log.get(ClientRpcChannel.class);
  
  public WebSocketClientRpcChannel(SocketAddress serverAddress, ExecutorService threadPool) 
      throws IOException {
    super(serverAddress, threadPool);
  }

  public WebSocketClientRpcChannel(SocketAddress serverAddress) 
      throws IOException {
    super(serverAddress);
  }
  
  @Override
  protected MessageExpectingChannel startChannel(SocketAddress serverAddress, 
      ExecutorService threadPool, ProtoCallback callback) throws IOException {
    InetSocketAddress inetAddress = (InetSocketAddress) serverAddress;
    
    WebSocketClientChannel protoChannel = 
      new WebSocketClientChannel(inetAddress.getHostName(), inetAddress.getPort(),
        callback, threadPool);
    protoChannel.expectMessage(Rpc.RpcFinished.getDefaultInstance());
    protoChannel.startAsyncRead();
    LOG.fine("Opened a new ClientRpcChannel to " + serverAddress);    
    return protoChannel;
  }
}
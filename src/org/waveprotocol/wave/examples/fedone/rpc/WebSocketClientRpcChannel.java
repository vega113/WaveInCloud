/**
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
 
package org.waveprotocol.wave.examples.fedone.rpc;

import org.waveprotocol.wave.examples.fedone.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ExecutorService;

/**
 * WebSocketClientRpcChannel starts a WebSocketClientChannel and returns it.
 */
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
  
  /**
   * Creates, starts, and returns a new WebSocketClientChannel connected to the given 
   * address, reading in the given thread pool, with incoming messages handled by the
   * given callback.
   * 
   * @param serverAddress Which websocket server to connect to.
   * @param threadPool The thread pool to create a thread to read on.
   * @param callback The callback to handle incoming messages.
   * @return a WebSocketClientChannel already reading
   */
  @Override
  protected MessageExpectingChannel startChannel(SocketAddress serverAddress, 
      ExecutorService threadPool, ProtoCallback callback) throws IOException {
    InetSocketAddress inetAddress = (InetSocketAddress) serverAddress;

    if (inetAddress == null) {
      throw new IllegalArgumentException("can't have null serverAddress!");
    }
    
    WebSocketClientChannel protoChannel = 
      new WebSocketClientChannel(inetAddress.getHostName(), inetAddress.getPort(),
        callback, threadPool);
    protoChannel.expectMessage(Rpc.RpcFinished.getDefaultInstance());
    protoChannel.startAsyncRead();
    LOG.fine("Opened a new ClientRpcChannel to " + serverAddress);    
    return protoChannel;
  }
}
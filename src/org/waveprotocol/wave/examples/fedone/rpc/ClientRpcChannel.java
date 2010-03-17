/**
 * Copyright 2009 Google Inc.
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

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcChannel;
import com.google.protobuf.RpcController;
import com.google.protobuf.UnknownFieldSet;
import com.google.protobuf.Descriptors.MethodDescriptor;

import org.waveprotocol.wave.examples.fedone.util.Log;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Provides the client end-point of a wave server connection. Used to generate
 * usable Stub implementations of any service type.
 * 
 *
 */
public class ClientRpcChannel implements RpcChannel {
  private static final Log LOG = Log.get(ClientRpcChannel.class);

  private final MessageExpectingChannel protoChannel;
  private final AtomicLong lastSequenceNumber;
  private final BiMap<Long, ClientRpcController> activeMethodMap = HashBiMap.create();

  /**
   * Set up a new ClientRpcChannel pointing at the given server address.
   * 
   * @param serverAddress the target server address
   */
  public ClientRpcChannel(SocketAddress serverAddress, ExecutorService threadPool)
      throws IOException {
    lastSequenceNumber = new AtomicLong();

    ProtoCallback callback = new ProtoCallback() {
      @Override
      public void message(long sequenceNo, Message message) {
        final ClientRpcController controller;
        synchronized (activeMethodMap) {
          controller = activeMethodMap.get(sequenceNo);
          // TODO: remove controller from activeMethodMap
        }
        if (message instanceof Rpc.RpcFinished) {
          Rpc.RpcFinished finished = (Rpc.RpcFinished) message;
          if (finished.getFailed()) {
            controller.failure(finished.getErrorText());
          } else {
            controller.response(null);
          }
        } else {
          controller.response(message);
        }
      }

      @Override
      public void unknown(long sequenceNo, String messageType, Object message) {
        final ClientRpcController controller;
        synchronized (activeMethodMap) {
          controller = activeMethodMap.get(sequenceNo);
        }
        controller.failure("Client RPC got unknown message: " + messageType);
      }
    };
    
    protoChannel = startChannel(serverAddress, threadPool, callback);
  }
  
  protected MessageExpectingChannel startChannel(SocketAddress serverAddress, 
      ExecutorService threadPool, ProtoCallback callback) throws IOException {
    SocketChannel channel = SocketChannel.open(serverAddress);
    SequencedProtoChannel protoChannel = 
      new SequencedProtoChannel(channel, callback, threadPool);
    protoChannel.expectMessage(Rpc.RpcFinished.getDefaultInstance());
    protoChannel.startAsyncRead();
    LOG.fine("Opened a new ClientRpcChannel to " + serverAddress);    
    return protoChannel;
  }

  /**
   * Create a new ClientRpcChannel backed onto a new single thread executor.
   */
  public ClientRpcChannel(SocketAddress serverAddress) throws IOException {
    this(serverAddress, Executors.newSingleThreadExecutor());
  }
  
  /**
   * Return a new RpcController for users of this RpcChannel.
   * 
   * @return the new RpcController implementation
   */
  public RpcController newRpcController() {
    return new ClientRpcController(this);
  }

  @Override
  public void callMethod(MethodDescriptor method, RpcController genericRpcController,
      Message request, Message responsePrototype, RpcCallback<Message> callback) {
    // Cast the given generic controller to a ClientRpcController.
    final ClientRpcController controller;
    if (genericRpcController instanceof ClientRpcController) {
      controller = (ClientRpcController) genericRpcController;
    } else {
      throw new IllegalArgumentException("Expected ClientRpcController, got: "
          + genericRpcController.getClass());
    }

    // Generate a new sequence number, and configure the controller - notably,
    // this throws an IllegalStateException if it is *already* configured.
    final long sequenceNo = lastSequenceNumber.incrementAndGet();
    final ClientRpcController.RpcState rpcStatus =
        new ClientRpcController.RpcState(this, method.getOptions()
            .getExtension(Rpc.isStreamingRpc), callback, new Runnable() {
          @Override
          public void run() {
            sendMessage(sequenceNo, Rpc.CancelRpc.getDefaultInstance());
          }
        });
    controller.configure(rpcStatus);
    synchronized (activeMethodMap) {
      activeMethodMap.put(sequenceNo, controller);
    }
    LOG.fine("Calling a new RPC (seq " + sequenceNo + "), method " + method.getFullName() + " for "
        + protoChannel);

    // Kick off the RPC by sending the request to the server end-point.
    sendMessage(sequenceNo, request, responsePrototype);
  }
  
  protected void sendMessage(long sequenceNo, Message message, Message responsePrototype) {
    protoChannel.sendMessage(sequenceNo, message, responsePrototype);
  }
  protected void sendMessage(long sequenceNo, Message message) {
    protoChannel.sendMessage(sequenceNo, message);
  }  
}

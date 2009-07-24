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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.Service;
import com.google.protobuf.UnknownFieldSet;
import com.google.protobuf.Descriptors.MethodDescriptor;

import org.waveprotocol.wave.examples.fedone.util.Log;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * ServerRpcProvider can provide instances of type Service over an incoming
 * network socket and service incoming RPCs to these services and their methods.
 * 
 *
 */
public class ServerRpcProvider {
  private static final Log LOG = Log.get(ServerRpcProvider.class);
  
  private final SocketAddress hostingAddress;
  private final Set<Connection> incomingConnections = Sets.newHashSet();
  private final ExecutorService threadPool;
  private ServerSocketChannel server = null;
  private Future<?> acceptorThread = null;
  
  // Mapping from incoming protocol buffer type -> specific handler.
  private final Map<Descriptors.Descriptor, RegisteredServiceMethod> registeredServices =
      Maps.newHashMap();

  /**
   * Internal, static container class for any specific registered service method.
   */
  static class RegisteredServiceMethod {
    final Service service;
    final MethodDescriptor method;

    RegisteredServiceMethod(Service service, MethodDescriptor method) {
      this.service = service;
      this.method = method;
    }
  }

  class Connection implements SequencedProtoChannel.ProtoCallback {
    private final SequencedProtoChannel protoChannel;
    private final Map<Long, ServerRpcController> activeRpcs =
        new ConcurrentHashMap<Long, ServerRpcController>();
    private final SocketChannel channel;
    
    Connection(SocketChannel channel) {
      this.channel = channel;
      LOG.info("New Connection set up from " + this.channel);
      
      // Set up protoChannel, let it know to expect messages of all the
      // registered service/method types.
      // TODO: dynamic lookup for these types instead
      protoChannel = new SequencedProtoChannel(channel, this, threadPool);
      synchronized (registeredServices) {
        for (RegisteredServiceMethod serviceMethod : registeredServices.values()) {
          protoChannel.expectMessage(serviceMethod.service.getRequestPrototype(serviceMethod.method));
          LOG.fine("Expecting: " + serviceMethod.method.getFullName());
        }
      }
      protoChannel.expectMessage(Rpc.CancelRpc.getDefaultInstance());
      protoChannel.startAsyncRead();
    }

    @Override
    public void message(final long sequenceNo, Message message) {
      if (message instanceof Rpc.CancelRpc) {
        final ServerRpcController controller = activeRpcs.get(sequenceNo);
        if (controller == null) {
          throw new IllegalStateException("Trying to cancel an RPC that is not active!");
        } else {
          LOG.info("Cancelling open RPC " + sequenceNo);
          controller.cancel();
        }
      } else if (registeredServices.containsKey(message.getDescriptorForType())) {
          if (activeRpcs.containsKey(sequenceNo)) {
            throw new IllegalStateException(
              "Can't invoke a new RPC with a sequence number already in use.");
          } else {
            final RegisteredServiceMethod serviceMethod =
              registeredServices.get(message.getDescriptorForType());

            // Create the internal ServerRpcController used to invoke the call.
            final ServerRpcController controller =
              new ServerRpcController(message, serviceMethod.service, serviceMethod.method,
                  new RpcCallback<Message>() {
                    @Override
                    synchronized public void run(Message message) {
                      if (message instanceof Rpc.RpcFinished
                          || !serviceMethod.method.getOptions().getExtension(Rpc.isStreamingRpc)) {
                        // This RPC is over - remove it from the map.
                        boolean failed = message instanceof Rpc.RpcFinished
                            ? ((Rpc.RpcFinished) message).getFailed() : false;
                        LOG.fine("RPC " + sequenceNo + " is now finished, failed = " + failed);
                        if (failed) {
                          LOG.info("error = " + ((Rpc.RpcFinished) message).getErrorText());
                        }
                        activeRpcs.remove(sequenceNo);
                      }
                      protoChannel.sendMessage(sequenceNo, message);
                    }
                  });

            // Kick off a new thread specific to this RPC.
            activeRpcs.put(sequenceNo, controller);
            threadPool.execute(controller);
          }
      } else {
        // Sent a message type we understand, but don't expect - erronous case!
        throw new IllegalStateException("Got expected but unknown message  (" + message
            + ") for sequence: " + sequenceNo);
      }
    }

    @Override
    public void unknown(long sequenceNo, String messageType, UnknownFieldSet message) {
      throw new IllegalStateException("Got unknown message (type: " + messageType + ", " + message
          + ") for sequence: " + sequenceNo);
    }
  }

  /**
   * Construct a new ServerRpcProvider, hosting on the passed SocketAddress.
   * Also accepts an ExecutorService for spawning managing threads.
   * 
   * @param host the hosting socket
   * @param threadPool the service used to create threads
   */
  public ServerRpcProvider(SocketAddress host, ExecutorService threadPool) {
    hostingAddress = host;
    this.threadPool = threadPool;
  }

  /**
   * Constructs a new ServerRpcProvider with a default ExecutorService.
   */
  public ServerRpcProvider(SocketAddress host) {
    this(host, Executors.newCachedThreadPool());
  }

  /**
   * Starts this server, binding to the previously passed SocketAddress.
   */
  public void startServer() throws IOException {
    server = ServerSocketChannel.open();
    server.socket().setReuseAddress(true);
    server.socket().bind(hostingAddress);
    server.configureBlocking(true);

    // Spawn a new server acceptor thread, which must accept incoming
    // connections indefinitely - until a ClosedChannelException is thrown.
    acceptorThread = threadPool.submit(new Runnable() {
      @Override
      public void run() {
        try {
          LOG.fine("ServerRpcProvider acceptorThread waiting for connections.");
          while (true) {
            SocketChannel serverSocket = server.accept();
            incomingConnections.add(new Connection(serverSocket));
          }
        } catch (ClosedChannelException e) {
          return;
        } catch (IOException e) {
          throw new IllegalStateException("Server should not throw a misunderstood IOException", e);
        }
      }
    });
  }

  /**
   * Returns the bound socket. This is null if this server is not running.
   */
  public SocketAddress getBoundAddress() {
    return server != null ? server.socket().getLocalSocketAddress() : null;
  }

  /**
   * Stops this server.
   */
  public void stopServer() throws IOException {
    server.close();
    try {
      acceptorThread.get();
    } catch (InterruptedException e) {
      throw new IllegalStateException();
    } catch (ExecutionException e) {
      throw new IllegalStateException("Server thread threw an exception", e.getCause());
    }
    if (!acceptorThread.isDone()) {
      throw new IllegalStateException("Server acceptor thread has not stopped.");
    }
    LOG.fine("server shutdown.");
  }

  /**
   * Register all methods provided by the given service type.
   */
  public void registerService(Service service) {
    synchronized (registeredServices) {
      for (MethodDescriptor methodDescriptor : service.getDescriptorForType().getMethods()) {
        registeredServices.put(methodDescriptor.getInputType(),
            new RegisteredServiceMethod(service, methodDescriptor));
      }
    }
  }  
}

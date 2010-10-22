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

package org.waveprotocol.box.server.rpc;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.Service;
import com.google.protobuf.UnknownFieldSet;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketServlet;
import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.box.server.util.Log;
import org.waveprotocol.box.server.waveserver.WaveClientRpc.ProtocolAuthenticate;
import org.waveprotocol.box.server.waveserver.WaveClientRpc.ProtocolAuthenticationResult;
import org.waveprotocol.wave.model.util.Pair;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * ServerRpcProvider can provide instances of type Service over an incoming
 * network socket and service incoming RPCs to these services and their methods.
 *
 *
 */
public class ServerRpcProvider {
  private static final Log LOG = Log.get(ServerRpcProvider.class);

  private final SocketAddress rpcHostingAddress;
  private final String httpHost;
  private final Integer httpPort;
  private final Set<Connection> incomingConnections = Sets.newHashSet();
  private final ExecutorService threadPool;
  private final SessionManager sessionManager;
  private final org.eclipse.jetty.server.SessionManager jettySessionManager;
  private ServerSocketChannel rpcServer = null;
  private Server httpServer = null;
  private Future<?> acceptorThread = null;

  // Mapping from incoming protocol buffer type -> specific handler.
  private final Map<Descriptors.Descriptor, RegisteredServiceMethod> registeredServices =
      Maps.newHashMap();

  /**
   * Internal, static container class for any specific registered service
   * method.
   */
  static class RegisteredServiceMethod {
    final Service service;
    final MethodDescriptor method;

    RegisteredServiceMethod(Service service, MethodDescriptor method) {
      this.service = service;
      this.method = method;
    }
  }

  class SequencedProtoChannelConnection extends Connection {
    private final SequencedProtoChannel protoChannel;
    private final SocketChannel channel;

    SequencedProtoChannelConnection(SocketChannel channel) {
      super(null);

      this.channel = channel;
      LOG.info("New Connection set up from " + this.channel);

      // Set up protoChannel, let it know to expect messages of all the
      // registered service/method types.
      // TODO: dynamic lookup for these types instead
      protoChannel = new SequencedProtoChannel(channel, this, threadPool);
      expectMessages(protoChannel);
      protoChannel.startAsyncRead();
    }

    @Override
    protected void sendMessage(long sequenceNo, Message message) {
      protoChannel.sendMessage(sequenceNo, message);
    }
  }

  class WebSocketConnection extends Connection {
    private final WebSocketServerChannel socketChannel;

    WebSocketConnection(ParticipantId loggedInUser) {
      super(loggedInUser);
      socketChannel = new WebSocketServerChannel(this);
      LOG.info("New websocket connection set up for user " + loggedInUser);
      expectMessages(socketChannel);
    }

    @Override
    protected void sendMessage(long sequenceNo, Message message) {
      socketChannel.sendMessage(sequenceNo, message);
    }

    public WebSocketServerChannel getWebSocketServerChannel() {
      return socketChannel;
    }
  }

  abstract class Connection implements ProtoCallback {
    private final Map<Long, ServerRpcController> activeRpcs =
        new ConcurrentHashMap<Long, ServerRpcController>();

    // The logged in user.
    // Note: Due to this bug: http://code.google.com/p/wave-protocol/issues/detail?id=119,
    // the field may be null on first connect and then set later using an RPC.
    private ParticipantId loggedInUser;

    /**
     * @param loggedInUser The currently logged in user, or null if no user is logged in.
     */
    public Connection(ParticipantId loggedInUser) {
      this.loggedInUser = loggedInUser;
    }

    protected void expectMessages(MessageExpectingChannel channel) {
      synchronized (registeredServices) {
        for (RegisteredServiceMethod serviceMethod : registeredServices.values()) {
          channel.expectMessage(serviceMethod.service.getRequestPrototype(serviceMethod.method));
          LOG.fine("Expecting: " + serviceMethod.method.getFullName());
        }
      }
      channel.expectMessage(Rpc.CancelRpc.getDefaultInstance());
    }

    protected abstract void sendMessage(long sequenceNo, Message message);

    private ParticipantId authenticate(String token) {
      HttpSession session = sessionManager.getSessionFromToken(token);
      ParticipantId user = sessionManager.getLoggedInUser(session);
      return user;
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
      } else if (message instanceof ProtocolAuthenticate) {
        // Workaround for bug: http://codereview.waveprotocol.org/224001/

        // When we get this message, either the connection will not be logged in
        // (loggedInUser == null) or the connection will have been authenticated via cookies
        // (in which case loggedInUser must match the authenticated user, and this message has no
        // effect).

        ProtocolAuthenticate authMessage = (ProtocolAuthenticate) message;
        ParticipantId authenticatedAs = authenticate(authMessage.getToken());

        Preconditions.checkArgument(authenticatedAs != null, "Auth token invalid");
        Preconditions.checkState(loggedInUser == null || loggedInUser.equals(authenticatedAs),
            "Session already authenticated as a different user");

        loggedInUser = authenticatedAs;
        LOG.info("Session authenticated as " + loggedInUser);
        sendMessage(sequenceNo, ProtocolAuthenticationResult.getDefaultInstance());
      } else if (registeredServices.containsKey(message.getDescriptorForType())) {
        if (activeRpcs.containsKey(sequenceNo)) {
          throw new IllegalStateException(
              "Can't invoke a new RPC with a sequence number already in use.");
        } else {
          final RegisteredServiceMethod serviceMethod =
              registeredServices.get(message.getDescriptorForType());

          // Create the internal ServerRpcController used to invoke the call.
          final ServerRpcController controller = new ServerRpcControllerImpl(
              message, serviceMethod.service, serviceMethod.method, loggedInUser,
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
                  sendMessage(sequenceNo, message);
                }
              });

          // Kick off a new thread specific to this RPC.
          activeRpcs.put(sequenceNo, controller);
          threadPool.execute(controller);
        }
      } else {
        // Sent a message type we understand, but don't expect - erronous case!
        throw new IllegalStateException(
            "Got expected but unknown message  (" + message + ") for sequence: " + sequenceNo);
      }
    }

    @Override
    public void unknown(long sequenceNo, String messageType, UnknownFieldSet message) {
      throw new IllegalStateException(
          "Got unknown message (type: " + messageType + ", " + message + ") for sequence: "
              + sequenceNo);
    }

    @Override
    public void unknown(long sequenceNo, String messageType, String message) {
      throw new IllegalStateException(
          "Got unknown message (type: " + messageType + ", " + message + ") for sequence: "
              + sequenceNo);
    }
  }

  /**
   * Construct a new ServerRpcProvider, hosting on the passed SocketAddress and
   * WebSocket host and port. (The http address isn't passed in as a
   * SocketAddress because Jetty requires host + port.)
   *
   * Also accepts an ExecutorService for spawning managing threads.
   *
   * @param rpcHost the hosting socket
   * @param httpHost host for http server
   * @param httpPort port for http server
   * @param threadPool the service used to create threads
   */
  public ServerRpcProvider(SocketAddress rpcHost, String httpHost, Integer httpPort,
      ExecutorService threadPool, SessionManager sessionManager,
      org.eclipse.jetty.server.SessionManager jettySessionManager) {
    rpcHostingAddress = rpcHost;
    this.httpHost = httpHost;
    this.httpPort = httpPort;
    this.threadPool = threadPool;
    this.sessionManager = sessionManager;
    this.jettySessionManager = jettySessionManager;
  }

  /**
   * Constructs a new ServerRpcProvider with a default ExecutorService and session manager.
   */
  public ServerRpcProvider(SocketAddress rpcHost, String httpHost, Integer httpPort,
      SessionManager sessionManager, org.eclipse.jetty.server.SessionManager jettySessionManager) {
    this(rpcHost, httpHost, httpPort, Executors.newCachedThreadPool(), sessionManager,
        jettySessionManager);
  }

  @Inject
  public ServerRpcProvider(@Named("client_frontend_hostname") String rpcHost,
      @Named("client_frontend_port") Integer rpcPort,
      @Named("http_frontend_hostname") String httpHost,
      @Named("http_frontend_port") Integer httpPort,
      SessionManager sessionManager,
      org.eclipse.jetty.server.SessionManager jettySessionManager) {
    this(new InetSocketAddress(rpcHost, rpcPort), httpHost, httpPort, sessionManager,
        jettySessionManager);
  }

  /**
   * Starts this server, binding to the previously passed SocketAddress.
   */
  public void startRpcServer() throws IOException {
    rpcServer = ServerSocketChannel.open();
    rpcServer.socket().setReuseAddress(true);
    rpcServer.socket().bind(rpcHostingAddress);
    rpcServer.configureBlocking(true);

    // Spawn a new server acceptor thread, which must accept incoming
    // connections indefinitely - until a ClosedChannelException is thrown.
    acceptorThread = threadPool.submit(new Runnable() {
      @Override
      public void run() {
        try {
          LOG.fine("ServerRpcProvider acceptorThread waiting for connections.");
          while (true) {
            SocketChannel serverSocket = rpcServer.accept();
            incomingConnections.add(new SequencedProtoChannelConnection(serverSocket));
          }
        } catch (ClosedChannelException e) {
          return;
        } catch (IOException e) {
          throw new IllegalStateException("Server should not throw a misunderstood IOException", e);
        }
      }
    });
  }

  public void startWebSocketServer() {
    httpServer = new Server();

    // Listen on httpHost and localhost.
    httpServer.addConnector(createSelectChannelConnector(httpHost, httpPort));
    if (!httpHost.equals("localhost")) {
      httpServer.addConnector(createSelectChannelConnector("localhost", httpPort));
    }

    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    if (jettySessionManager != null) {
      context.getSessionHandler().setSessionManager(jettySessionManager);
    }
    context.setResourceBase("./war");

    // Servlet where the websocket connection is served from.
    ServletHolder holder = new ServletHolder(new WaveWebSocketServlet());
    context.addServlet(holder, "/socket");
    // TODO(zamfi): fix to let messages span frames.
    holder.setInitParameter("bufferSize", "" + 1024 * 1024); // 1M buffer

    // Serve the static content and GWT web client with the default servlet
    // (acts like a standard file-based web server).
    ServletHolder defaultServlet = new ServletHolder(new DefaultServlet());
    context.addServlet(defaultServlet, "/static/*");
    context.addServlet(defaultServlet, "/webclient/*");

    for (Pair<String, HttpServlet> servlet : servletRegistry) {
      context.addServlet(new ServletHolder(servlet.getSecond()), servlet.getFirst());
    }

    httpServer.setHandler(context);

    try {
      httpServer.start();
    } catch (Exception e) { // yes, .start() throws "Exception"
      LOG.severe("Fatal error starting http server.", e);
      return;
    }
    LOG.fine("WebSocket server running.");
  }

  /**
   * @return a {@link SelectChannelConnector} bound to a given host and port
   */
  private SelectChannelConnector createSelectChannelConnector(String host, int port) {
    SelectChannelConnector connector = new SelectChannelConnector();
    connector.setHost(host);
    connector.setPort(port);
    return connector;
  }

  public class WaveWebSocketServlet extends WebSocketServlet {
    @Override
    protected WebSocket doWebSocketConnect(HttpServletRequest request, String protocol) {
      ParticipantId loggedInUser = sessionManager.getLoggedInUser(request.getSession(false));

      WebSocketConnection connection = new WebSocketConnection(loggedInUser);
      return connection.getWebSocketServerChannel();
    }
  }

  /**
   * Returns the bound socket. This is null if this server is not running.
   */
  public SocketAddress getBoundAddress() {
    return rpcServer != null ? rpcServer.socket().getLocalSocketAddress() : null;
  }

  /**
   * Returns the socket the WebSocket server is listening on.
   */
  public SocketAddress getWebSocketAddress() {
    if (httpServer == null) {
      return null;
    } else {
      Connector c = httpServer.getConnectors()[0];
      return new InetSocketAddress(c.getHost(), c.getLocalPort());
    }
  }

  /**
   * Stops this server.
   */
  public void stopServer() throws IOException {
    if (rpcServer != null) {
      rpcServer.close();
    }
    try {
      httpServer.stop(); // yes, .stop() throws "Exception"
    } catch (Exception e) {
      LOG.warning("Fatal error stopping http server.", e);
    }
    if (acceptorThread != null) {
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

  /**
   * Set of servlets
   */
  List<Pair<String, HttpServlet> > servletRegistry = Lists.newArrayList();

  /**
   * Add a servlet to the servlet registry. This servlet will be attached to the
   * specified URL pattern when the server is started up.
   *
   * @param urlPattern URL pattern for paths. Eg, '/foo', '/foo/*'
   * @param servlet The servlet object to bind to the specified paths
   */
  public void addServlet(String urlPattern, HttpServlet servlet) {
    servletRegistry.add(new Pair<String, HttpServlet>(urlPattern, servlet));
  }
}

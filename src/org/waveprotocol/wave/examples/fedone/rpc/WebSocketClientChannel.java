package org.waveprotocol.wave.examples.fedone.rpc;

import org.waveprotocol.wave.examples.fedone.util.Log;

import com.sixfire.websocket.WebSocket;

import java.net.URI;
import java.net.URISyntaxException;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


class WebSocketClientChannel extends WebSocketChannel {
  private static final Log LOG = Log.get(WebSocketClientChannel.class);
  private final ExecutorService threadPool;
  private final Runnable asyncRead;
  private boolean isReading = false;

  private final URI uri;
  private final WebSocket websocket;

  public WebSocketClientChannel(String host, Integer port, ProtoCallback callback) {
    this(host, port, callback, Executors.newSingleThreadExecutor());
  }

  /**
   * Standard constructur opens a websocket connection to host:port, sets up
   * async read.
   *
   * @param host
   * @param port
   * @param callback
   * @param threadPool
   */
  public WebSocketClientChannel(String host, Integer port, 
      ProtoCallback callback, ExecutorService threadPool) {
    super(callback);
    this.threadPool = threadPool;
    try {
      uri = new URI("ws", null, host, port, "/", null, null);      
    } catch (URISyntaxException use) {
      LOG.severe("Unable to create ws:// uri from given host ("
        +host+") and port ("+port+")", use);
      throw new IllegalStateException(use);
    }
    
    websocket = new WebSocket(uri);
    try {
      websocket.connect();
    } catch (IOException e) {
      LOG.severe("WebSocket unable to connect.", e);
      throw new IllegalStateException(e);
    }
    
    this.asyncRead = new Runnable() {
      @Override
      public void run() {
        try {
          String data = websocket.recv();
          while (data != null) {
            handleMessageString(data);
            data = websocket.recv();
          }
        } catch (IOException e) {
          LOG.severe("WebSocket async data read failed, aborting connection.", e);
        }
      }
    };
  }
  
  public void startAsyncRead() {
    if (isReading) {
      throw new IllegalStateException("This websocket is already reading asynchronously.");
    }
    threadPool.execute(asyncRead);
    isReading = true; 
  }
  
  public void sendMessageString(String data) {
    try {
      websocket.send(data);
    } catch (IOException e) {
      LOG.warning("Websocket send failed.", e);
    }
  }
}
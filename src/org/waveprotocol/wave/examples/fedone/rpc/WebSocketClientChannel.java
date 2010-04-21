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

import com.sixfire.websocket.WebSocket;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The client side of a WebSocketHannel. 
 */
class WebSocketClientChannel extends WebSocketChannel {
  private static final Log LOG = Log.get(WebSocketClientChannel.class);
  private final ExecutorService threadPool;
  private final Runnable asyncRead;
  private boolean isReading = false;

  private final WebSocket websocket;

  /**
   * Creates a WebSocketClientChannel with a default thread executor. See 
   * {@link #WebSocketCLientChannel(String, Integer, ProtoCallback, ExecutorService)}
   */
  public WebSocketClientChannel(String host, Integer port, ProtoCallback callback) {
    this(host, port, callback, Executors.newSingleThreadExecutor());
  }

  /**
   * Constructs a WebSocketClientChannel, connects to host:port, and sets up
   * async read.
   *
   * @param host host to connect to
   * @param port port to connect to
   * @param callback ProtoCallback handler for incoming messages
   * @param threadPool threadpool for thread that performs async read.
   */
  public WebSocketClientChannel(String host, Integer port, 
      ProtoCallback callback, ExecutorService threadPool) {
    super(callback);
    this.threadPool = threadPool;
    URI uri;
    try {
      uri = new URI("ws", null, host, port, "/", null, null);      
    } catch (URISyntaxException use) {
      LOG.severe("Unable to create ws:// uri from given host ("
        + host + ") and port (" + port + ")", use);
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
  
  /**
   * Start reading asynchronously from the websocket client.
   */
  @Override
  public void startAsyncRead() {
    if (isReading) {
      throw new IllegalStateException("This websocket is already reading asynchronously.");
    }
    threadPool.execute(asyncRead);
    isReading = true; 
  }
  
  /**
   * Propagate a String message to the websocket client.
   */
  @Override
  public void sendMessageString(String data) {
    try {
      websocket.send(data);
    } catch (IOException e) {
      LOG.warning("Websocket send failed.", e);
    }
  }
}
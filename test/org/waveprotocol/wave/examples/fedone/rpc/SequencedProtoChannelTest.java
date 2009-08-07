/*
 * Copyright (C) 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.waveprotocol.wave.examples.fedone.rpc;

import com.google.protobuf.UnknownFieldSet;

import junit.framework.TestCase;

import org.waveprotocol.wave.examples.fedone.waveserver.WaveClientRpc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.SecureRandom;
import java.util.Random;

/**
 * Test the SequencedProtoChannel.
 * 
 *
 */
public class SequencedProtoChannelTest extends TestCase {

  private Random random = new SecureRandom();
  private ClientServerPipe connection = null;

  /**
   * A convenient container class to generate and store a pair of SocketChannel
   * instances connected to each other.
   */
  class ClientServerPipe {
    public final SocketChannel serverSocket;
    public final SocketChannel clientSocket;

    ClientServerPipe() throws IOException {
      ServerSocketChannel server;
      server = ServerSocketChannel.open();
      server.socket().bind(null);
      clientSocket = SocketChannel.open();
      clientSocket.connect(server.socket().getLocalSocketAddress());
      serverSocket = server.accept();
      assertTrue(clientSocket.finishConnect());
      assertTrue(clientSocket.isConnected());
      assertTrue(serverSocket.isConnected());

      // Confirm that this socket is working properly!
      clientSocket.write(ByteBuffer.wrap(new byte[] {'a'}));
      ByteBuffer dsts = ByteBuffer.allocate(1);
      assertEquals(1, serverSocket.read(dsts));
      assertEquals('a', dsts.get(0));

      clientSocket.configureBlocking(true);
      serverSocket.configureBlocking(true);
    }
  }
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    connection = new ClientServerPipe();
  }

  @Override
  public void tearDown() throws Exception {
    connection = null;
    super.tearDown();
  }

  /**
   * Test a faux-RPC style request/response interface passing messages between a
   * server and client.
   */
  public void testRequestResponse() throws Exception {
    final int MESSAGE_TIMEOUT = 5;    
    FakeProtoChannelManager clientManager = new FakeProtoChannelManager(connection.clientSocket);
    FakeProtoChannelManager serverManager = new FakeProtoChannelManager(connection.serverSocket);

    // Notify both the server and client of the messages they should expect.
    serverManager.channel.expectMessage(WaveClientRpc.ProtocolOpenRequest.getDefaultInstance());
    clientManager.channel.expectMessage(WaveClientRpc.ProtocolWaveletUpdate.getDefaultInstance());

    // Generate and send a client message to the server.
    WaveClientRpc.ProtocolOpenRequest clientMessage =
        WaveClientRpc.ProtocolOpenRequest.newBuilder().setParticipantId("sam@google.com")
            .setWaveId("foowave").build();
    assertTrue(clientMessage.isInitialized());
    clientManager.channel.sendMessage(0, clientMessage);

    // Confirm that the server has received this message.
    assertEquals(FakeProtoChannelManager.SequencedObject.of(0, clientMessage),
        serverManager.waitForMessage(MESSAGE_TIMEOUT));

    // Generate and send a server message to the client.
    WaveClientRpc.ProtocolWaveletUpdate serverMessage = 
        WaveClientRpc.ProtocolWaveletUpdate.newBuilder().setWaveletName("foowave").build();
    assertTrue(serverMessage.isInitialized());
    serverManager.channel.sendMessage(0, serverMessage);

    // Confirm that the client has received this message.
    assertEquals(FakeProtoChannelManager.SequencedObject.of(0, serverMessage),
        clientManager.waitForMessage(MESSAGE_TIMEOUT));
  }

  /**
   * Test that multiple messages are passed over a SequencedProtoChannel.
   */
  public void testMultipleMessages() throws Exception {
    final int MESSAGE_TIMEOUT = 5;
    final int MESSAGES_TO_SEND = random.nextInt(10) + 10; // between 10-20 messages
    FakeProtoChannelManager clientManager = new FakeProtoChannelManager(connection.clientSocket);
    FakeProtoChannelManager serverManager = new FakeProtoChannelManager(connection.serverSocket);

    // Notify the client of the message it should expect.
    clientManager.channel.expectMessage(WaveClientRpc.ProtocolWaveletUpdate.getDefaultInstance());

    // Generate and send multiple server messages to the client.
    for (int m = 0; m < MESSAGES_TO_SEND; ++m) {
      WaveClientRpc.ProtocolWaveletUpdate serverMessage =
          WaveClientRpc.ProtocolWaveletUpdate.newBuilder().setWaveletName("wave_" + m).build();
      assertTrue(serverMessage.isInitialized());
      serverManager.channel.sendMessage(m, serverMessage);
    }

    // Confirm that all messages are received by the client.
    for (int m = 0; m < MESSAGES_TO_SEND; ++m) {
      FakeProtoChannelManager.SequencedObject<?> response =
          clientManager.waitForMessage(MESSAGE_TIMEOUT);
      assertNotNull("Couldn't get message number " + m + "/" + MESSAGES_TO_SEND, response);
      assertTrue(response.message instanceof WaveClientRpc.ProtocolWaveletUpdate);
      WaveClientRpc.ProtocolWaveletUpdate waveletUpdate =
          (WaveClientRpc.ProtocolWaveletUpdate) response.message;
      assertEquals("wave_" + m, waveletUpdate.getWaveletName());
    }
  }
  
  /**
   * Test that an unknown message is received properly, and that the correct
   * callback is invoked.
   */
  public void testUnknownMessage() throws Exception {
    final int MESSAGE_TIMEOUT = 5;
    FakeProtoChannelManager clientManager = new FakeProtoChannelManager(connection.clientSocket);
    FakeProtoChannelManager serverManager = new FakeProtoChannelManager(connection.serverSocket);

    // Generate standard message, pass from server to client - who has not had
    // this type registered.
    WaveClientRpc.ProtocolWaveletUpdate serverMessage = 
      WaveClientRpc.ProtocolWaveletUpdate.newBuilder().setWaveletName("foowave").build();
    assertTrue(serverMessage.isInitialized());
    serverManager.channel.sendMessage(1, serverMessage);
    assertEquals(UnknownFieldSet.class, clientManager.waitForMessage(MESSAGE_TIMEOUT).message
        .getClass());
  }
}

/**
 * Copyright (C) 2010 Google Inc.
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

package org.waveprotocol.box.client.testing;

import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;

import org.mockito.Mockito;
import org.waveprotocol.box.client.ClientBackend;
import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.box.server.authentication.SessionManagerImpl;
import org.waveprotocol.box.server.frontend.WaveClientRpcImpl;
import org.waveprotocol.box.server.frontend.testing.FakeWaveServer;
import org.waveprotocol.box.server.persistence.memory.MemoryStore;
import org.waveprotocol.box.server.rpc.ClientRpcChannel;
import org.waveprotocol.box.server.rpc.testing.FakeServerRpcController;
import org.waveprotocol.box.server.waveserver.WaveClientRpc.ProtocolAuthenticate;
import org.waveprotocol.box.server.waveserver.WaveClientRpc.ProtocolAuthenticationResult;
import org.waveprotocol.box.server.waveserver.WaveClientRpc.ProtocolWaveClientRpc;

import java.net.InetSocketAddress;

/**
 * A factory of fake RPC objects for the client backend.
 *
 * @author mk.mateng@gmail.com (Michael Kuntzman)
 */
public class FakeRpcObjectFactory implements ClientBackend.RpcObjectFactory {
  /**
   * A {@code ClientRpcChannel} that only returns fake RPC controllers.
   */
  private static class FakeClientRpcChannel implements ClientRpcChannel {
    @Override
    public RpcController newRpcController() {
      return new FakeServerRpcController();
    }

    @Override
    public void callMethod(MethodDescriptor method, RpcController genericRpcController,
        Message request, Message responsePrototype, RpcCallback<Message> callback) {
    }
  }

  /**
   * @return a fake {@code ClientRpcChannel} implementation.
   */
  @Override
  public ClientRpcChannel createClientChannel(InetSocketAddress serverAddress) {
    return new FakeClientRpcChannel();
  }

  /**
   * @return a {@code WaveClientRpcImpl} backed by a {@code FakeWaveServer}.
   */
  @Override
  public ProtocolWaveClientRpc.Interface createServerInterface(ClientRpcChannel channel) {
    org.eclipse.jetty.server.SessionManager jettySessionManager =
        Mockito.mock(org.eclipse.jetty.server.SessionManager.class);
    SessionManager sessionManager = new SessionManagerImpl(new MemoryStore(), jettySessionManager);
    return new WaveClientRpcImpl(new FakeWaveServer()){
      @Override
      public void authenticate(RpcController controller, ProtocolAuthenticate request,
          RpcCallback<ProtocolAuthenticationResult> done) {
        done.run(ProtocolAuthenticationResult.getDefaultInstance());
      }
    };
  }
}

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

package org.waveprotocol.wave.examples.fedone.robots.passive;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.wave.api.InvalidRequestException;
import com.google.wave.api.OperationRequest;
import com.google.wave.api.ProtocolVersion;
import com.google.wave.api.RobotSerializer;
import com.google.wave.api.event.EventType;
import com.google.wave.api.impl.EventMessageBundle;
import com.google.wave.api.robot.Capability;
import com.google.wave.api.robot.RobotConnection;
import com.google.wave.api.robot.RobotConnectionException;

import junit.framework.TestCase;

import org.waveprotocol.wave.examples.fedone.account.RobotAccountData;
import org.waveprotocol.wave.examples.fedone.account.RobotAccountDataImpl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Unit test for the {@link RobotConnector}.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class RobotConnectorTest extends TestCase {

  private static final ProtocolVersion PROTOCOL_VERSION = ProtocolVersion.DEFAULT;

  private static final String ROBOT_ACCOUNT_NAME = "test@example.com";

  private static final String TEST_URL = "www.example.com/robot";

  private static final RobotAccountData ROBOT_ACCOUNT = new RobotAccountDataImpl(
      ROBOT_ACCOUNT_NAME, TEST_URL, new HashMap<EventType, Capability>(), "FakeHash", true);

  private static final EventMessageBundle BUNDLE =
      new EventMessageBundle(ROBOT_ACCOUNT_NAME, "www.example.com/rpc");

  private static final String SERIALIZED_BUNDLE = "BUNDLE";

  private static final String RETURNED_OPERATION = "OPERATION";

  private RobotConnection connection;

  private RobotSerializer serializer;

  private RobotConnector connector;

  @Override
  protected void setUp() throws Exception {
    connection = mock(RobotConnection.class);
    serializer = mock(RobotSerializer.class);
    connector = new RobotConnector(connection, serializer);
  }

  public void testSuccessfulCall() throws Exception {
    final List<OperationRequest> expectedOperations = Collections.unmodifiableList(
        Lists.newArrayList(new OperationRequest("wavelet.setTitle", "op1")));

    when(serializer.serialize(BUNDLE, PROTOCOL_VERSION)).thenReturn(SERIALIZED_BUNDLE);
    when(connection.postJson(TEST_URL, SERIALIZED_BUNDLE)).thenReturn(RETURNED_OPERATION);
    when(serializer.deserializeOperations(RETURNED_OPERATION)).thenReturn(expectedOperations);

    List<OperationRequest> operations =
        connector.sendMessageBundle(BUNDLE, ROBOT_ACCOUNT, PROTOCOL_VERSION);
    assertEquals(expectedOperations, operations);
  }

  public void testConnectionFailsSafely() throws Exception {
    when(serializer.serialize(BUNDLE, PROTOCOL_VERSION)).thenReturn(SERIALIZED_BUNDLE);
    when(connection.postJson(TEST_URL, SERIALIZED_BUNDLE)).thenThrow(
        new RobotConnectionException("Connection Failed"));

    List<OperationRequest> operations =
        connector.sendMessageBundle(BUNDLE, ROBOT_ACCOUNT, PROTOCOL_VERSION);
    assertTrue("Expected no operations to be returned", operations.isEmpty());
  }

  public void testDeserializationFailsSafely() throws Exception {
    when(serializer.serialize(BUNDLE, PROTOCOL_VERSION)).thenReturn(SERIALIZED_BUNDLE);
    when(connection.postJson(TEST_URL, SERIALIZED_BUNDLE)).thenReturn(RETURNED_OPERATION);
    when(serializer.deserializeOperations(RETURNED_OPERATION)).thenThrow(
        new InvalidRequestException("Invalid Request"));

    List<OperationRequest> operations =
        connector.sendMessageBundle(BUNDLE, ROBOT_ACCOUNT, PROTOCOL_VERSION);
    assertTrue("Expected no operations to be returned", operations.isEmpty());
  }
}

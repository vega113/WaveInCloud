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

package org.waveprotocol.box.server.agents.agent;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.waveprotocol.box.server.util.testing.Matchers.contains;

import junit.framework.TestCase;

import org.waveprotocol.box.client.ClientBackend;
import org.waveprotocol.box.client.testing.ClientTestingUtil;
import org.waveprotocol.box.server.agents.agent.AbstractAgent;
import org.waveprotocol.box.server.agents.agent.AgentConnection;
import org.waveprotocol.box.server.util.Log;
import org.waveprotocol.box.server.util.testing.ExceptionLogHandler;
import org.waveprotocol.box.server.util.testing.TestingConstants;

import java.util.logging.Level;


/**
 * Base class for agent test cases.
 *
 * @author mk.mateng@gmail.com (Michael Kuntzman)
 */
public abstract class AgentTestBase<A extends AbstractAgent> extends TestCase
    implements TestingConstants {
  /** The agent we're testing */
  protected A agent;

  /** The agent's client backend */
  protected ClientBackend backend;

  /** The testing utility for the agent */
  protected ClientTestingUtil util;

  static {
    // TODO(Michael): Move this to a client-backend test case.
    Log.get(ClientBackend.class).getLogger().addHandler(new ExceptionLogHandler(Level.SEVERE));
  }

  @Override
  public void setUp() {
    agent = createAgent(new AgentConnection(USER, DOMAIN, PORT,
        ClientTestingUtil.backendSpyFactory));
    agent.connect();

    // These must be done after connecting, otherwise the backend may be null:
    backend = agent.getBackend();
    util = new ClientTestingUtil(backend);
  }

  @Override
  public void tearDown() {
    agent.disconnect();
  }

  // Tests

  /**
   * The agent should be connected (to our fake server). A quick test to avoid untested
   * assumptions in more important tests, and to make sure the agent connects successfully.
   */
  public void testIsConnected() {
    assertTrue(agent.isConnected());
  }

  /**
   * The agent should be listening to events. Added after a bug in the event listeners chain setup
   * code.
   */
  public void testIsListeningToEvents() {
    // TODO(Michael): Find a more implementation-independent way to test this. Perhaps by injecting
    // an event to the backend somehow and checking that the corresponding handler is invoked in
    // the agent.
    assertNotNull(agent.getEventProvider());
    assertThat(backend.getListeners(), contains(agent.getEventProvider()));
    assertThat(agent.getEventProvider().getListeners(), contains(agent));
  }

  // Utility methods

  /**
   * Create an agent for testing, using the specified connection.
   *
   * @param connection to use for the agent.
   * @return a new agent for testing.
   */
  protected abstract A createAgent(AgentConnection connection);
}

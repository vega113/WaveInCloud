/**
 * Copyright 2011 Google Inc.
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

package org.waveprotocol.box.server.robots.agent.passwd;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;

import junit.framework.TestCase;

import org.apache.commons.cli.CommandLine;
import org.waveprotocol.box.server.CoreSettings;
import org.waveprotocol.box.server.robots.agent.AbstractRobotAgent;

/**
 * Unit tests for the {@link AbstractRobotAgent}.
 * 
 * @author yurize@apache.org (Yuri Zelikov)
 */
public class AbstractRobotAgentTest extends TestCase {

  @SuppressWarnings("serial")
  private class FakeRobotAgent extends AbstractRobotAgent {


    public FakeRobotAgent(Injector injector) {
      super(injector);
    }

    @Override
    protected CommandLine preprocessCommand(String blipContent) throws IllegalArgumentException {
      return super.preprocessCommand(blipContent);
    }

    @Override
    protected String getRobotName() {
      return commandName;
    }

    @Override
    protected String maybeExecuteCommand(CommandLine commandLine, String modifiedBy) {
      return "";
    }

    @Override
    public String getShortDescription() {
      return "";
    }

    @Override
    public int getMinNumOfArguments() {
      return 2;
    }

    @Override
    public int getMaxNumOfArguments() {
      return 3;
    }

    @Override
    public String getFullDescription() {
      return null;
    }

    @Override
    public String getExample() {
      return null;
    }

    @Override
    public String getCommandName() {
      return commandName;
    }

    @Override
    public String getCmdLineSyntax() {
      return "";
    }
  };

  private static final String commandName = "agent";

  private Injector injector;
  private FakeRobotAgent agent;

  @Override
  protected void setUp() throws Exception {
    injector = mock(Injector.class);
    when(injector.getInstance(Key.get(String.class, Names.named(CoreSettings.WAVE_SERVER_DOMAIN))))
        .thenReturn("example.com");
    agent = new FakeRobotAgent(injector);
  }

  public void testPreprocessCommandValidInput() throws Exception {
    String content = String.format("%s arg1 arg2\n", commandName);
    CommandLine commandLine = agent.preprocessCommand(content);
    assertEquals(3, commandLine.getArgs().length);
  }

  public void testPreprocessCommandNoCommandInput() throws Exception {
    String content = String.format("%s arg1 arg2\n", "not_a_command");
    CommandLine commandLine = agent.preprocessCommand(content);
    assertNull(commandLine);
  }

  public void testPreprocessCommandFailsOnTooManyArgs() throws Exception {
    String content = String.format("%s arg1 arg2 arg3 arg4\n", commandName);
    try {
      agent.preprocessCommand(content);
      fail();
    } catch (IllegalArgumentException e) {
      // Expected.
    }
  }
  
  public void testPreprocessCommandFailsOnTooFewArgs() throws Exception {
    String content = String.format("%s arg1 \n", commandName);
    try {
      agent.preprocessCommand(content);
      fail();
    } catch (IllegalArgumentException e) {
      // Expected.
    }
  }
}

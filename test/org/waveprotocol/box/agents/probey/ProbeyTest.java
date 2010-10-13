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

package org.waveprotocol.box.agents.probey;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.waveprotocol.box.server.util.testing.Matchers.contains;
import static org.waveprotocol.box.server.util.testing.Matchers.doesNotContain;
import static org.waveprotocol.box.server.util.testing.Matchers.Aliases.contains;

import org.eclipse.jetty.server.Request;
import org.mockito.ArgumentCaptor;
import org.waveprotocol.box.agents.probey.Probey;
import org.waveprotocol.box.client.ClientWaveView;
import org.waveprotocol.box.client.testing.ClientTestingUtil;
import org.waveprotocol.box.common.DocumentConstants;
import org.waveprotocol.box.server.agents.agent.AgentConnection;
import org.waveprotocol.box.server.agents.agent.AgentTestBase;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.BlipData;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

/**
 * Tests for the {@link Probey} agent.
 *
 * @author mk.mateng@gmail.com (Michael Kuntzman)
 */
public class ProbeyTest extends AgentTestBase<Probey> {
  private static final String OK = "OK";

  private static final String PLAIN_TEXT = "text/plain";

  /** Probey's web handler */
  private Probey.WebHandler handler;

  @Override
  public void setUp() {
    super.setUp();
    handler = new Probey.WebHandler(agent);
  }

  // Tests

  /** "new" command should create a single wave and open it */
  public void testNewCreatesOneWaveAndOpensIt() throws IOException {
    createNewWave();

    // There should be 2 waves: the index wave and our new wave.
    assertEquals(2, util.getOpenWavesCount(true));
  }

  /** "new" command should only add the user, and nothing else (no other users and no content) */
  public void testNewOnlyAddsSelf() throws IOException {
    createNewWave();

    ClientWaveView newWave = util.getFirstWave();
    String newWaveContent = util.getText(newWave);
    Set<ParticipantId> participants = util.getAllParticipants(newWave);

    assertEquals("", newWaveContent);
    assertEquals(1, participants.size());
    assertThat(participants, contains(agent.getParticipantId()));
  }

  /** "new" command should report the wave Id of the wave it has created */
  public void testNewReportsCorrectWaveId() throws IOException {
    String output = createNewWave();

    String newWaveId = util.getFirstWaveId().serialise();

    // The output should match exactly (give or take surrounding whitespace), otherwise Probey
    // would be less useful for automated testing.
    assertEquals(newWaveId, output.trim());
  }

  /** "add" command should not add/delete/modify waves */
  public void testAddHasNoSideEffects() throws IOException {
    createNewWave();

    // Use a WaveId (and not a ClientWaveView) to make sure that the wave is retrieved again after
    // we run the command. 
    WaveId waveId = util.getFirstWaveId();
    int oldWaveCount = util.getOpenWavesCount(true);
    String oldContent = util.getText(waveId);

    addUser(waveId, OTHER_PARTICIPANT);

    int newWaveCount = util.getOpenWavesCount(true);
    String newContent = util.getText(waveId);

    assertEquals(oldWaveCount, newWaveCount); // No waves were created/deleted.
    assertEquals(oldContent, newContent);     // The wave content was not changed.
  }

  /** "add" command should add the specified user to the correct wave */
  public void testAddAddsUserToCorrectWave() throws IOException {
    createNewWave(); // 0
    createNewWave(); // 1
    createNewWave(); // 2

    WaveId targetWaveId = util.getOpenWaveId(2, false);
    Set<ParticipantId> oldUsers = util.getAllParticipants(targetWaveId);

    addUser(targetWaveId, OTHER_PARTICIPANT);

    for (WaveId waveId : util.getOpenWaveIds(true)) {
      Set<ParticipantId> newUsers = util.getAllParticipants(waveId);
      if (waveId.equals(targetWaveId)) {
        // The target wave should have the added user as a participant.
        assertThat(newUsers, contains(OTHER_PARTICIPANT));
        newUsers.remove(OTHER_PARTICIPANT);
        assertEquals(oldUsers, newUsers);
      } else {
        // All other waves should not have the added user as a participant.
        assertThat(newUsers, doesNotContain(OTHER_PARTICIPANT));
      }
    }
  }

  /** "add" command should report success */
  public void testAddReportsSuccess() throws IOException {
    createNewWave();

    String output = addUser(util.getFirstWaveId(), OTHER_PARTICIPANT);

    // The output should match exactly (give or take surrounding whitespace), otherwise Probey
    // would be less useful for automated testing.
    assertEquals(OK, output.trim());
  }

  /** "addblip" command should not add/delete waves or users */
  public void testAddBlipHasNoSideEffects() throws IOException {
    createNewWave();

    // Use a WaveId (and not a ClientWaveView) to make sure that the wave is retrieved again after
    // we run the command. 
    WaveId waveId = util.getFirstWaveId();
    int oldWaveCount = util.getOpenWavesCount(true);
    Set<ParticipantId> oldUsers = util.getAllParticipants(waveId);

    addBlip(waveId, MESSAGE);

    int newWaveCount = util.getOpenWavesCount(true);
    Set<ParticipantId> newUsers = util.getAllParticipants(waveId);

    assertEquals(oldWaveCount, newWaveCount); // No waves were created/deleted.
    assertEquals(oldUsers, newUsers);         // No users were added/removed.
  }

  /** "addblip" command should add the specified blip to the correct wave */
  public void testAddBlipAddsBlipToCorrectWave() throws IOException {
    createNewWave(); // 0
    createNewWave(); // 1
    createNewWave(); // 2

    WaveId targetWaveId = util.getOpenWaveId(2, false);
    int oldDocCount = util.getAllDocuments(targetWaveId).size();

    addBlip(targetWaveId, MESSAGE);

    // The target wave should have one new blip with the correct content. Other waves should have
    // no new blips and should still be empty. The index wave won't pass this test, so skip it.
    for (WaveId waveId : util.getOpenWaveIds(false)) {
      int newDocCount = util.getAllDocuments(waveId).size();
      String content = util.getText(waveId);

      if (waveId.equals(targetWaveId)) {
        assertEquals(oldDocCount + 1, newDocCount);
        assertEquals(MESSAGE, content);
      } else {
        assertEquals(oldDocCount, newDocCount);
        assertEquals("", content);
      }
    }
  }

  /** "addblip" command should report the correct blip id */
  public void testAddBlipReportsCorrectBlipId() throws IOException {
    createNewWave();

    WaveId waveId = util.getFirstWaveId();
    String output = addBlip(waveId, MESSAGE);

    // The output should equal the blip ID exactly (give or take surrounding whitespace), otherwise
    // Probey would be less useful for automated testing.
    String blipId = output.trim();
    BlipData blip = util.getAllDocuments(waveId).get(blipId);
    assertNotNull(blip);
    String blipContent = ClientTestingUtil.getText(blip);
    assertEquals(MESSAGE, blipContent);
  }

  /** "getblips" command should not add/remove waves, participants, or content. */
  public void testGetBlipsHasNoSideEffects() throws IOException {
    createNewWave();
    WaveId waveId = util.getFirstWaveId();
    addBlip(waveId, MESSAGE);

    int oldWaveCount = util.getOpenWavesCount(true);
    Set<ParticipantId> oldUsers = util.getAllParticipants(waveId);
    String oldContent = util.getText(waveId);

    getBlips(waveId);

    int newWaveCount = util.getOpenWavesCount(true);
    Set<ParticipantId> newUsers = util.getAllParticipants(waveId);
    String newContent = util.getText(waveId);

    assertEquals(oldWaveCount, newWaveCount); // No waves were created/deleted.
    assertEquals(oldUsers, newUsers);         // No users were added/removed.
    assertEquals(oldContent, newContent);     // The wave content was not modified.
  }

  /** "getblips" command should report all blips from the requested wave, and only them */
  public void testGetBlipsReturnsCorrectBlips() throws IOException {
    createNewWave();
    createNewWave();

    WaveId targetWaveId = util.getOpenWaveId(1, false);
    WaveId otherWaveId = util.getOpenWaveId(0, false);
    addBlip(targetWaveId, MESSAGE);
    addBlip(otherWaveId, MESSAGE2);
    addBlip(targetWaveId, MESSAGE3);

    String output = getBlips(targetWaveId);

    // Probey should report all blips from the requested wave. It should not report the manifest
    // document (which is not a blip).
    Map<String, BlipData> docs = util.getAllDocuments(targetWaveId);
    for (Entry<String, BlipData> entry : docs.entrySet()) {
      String docId = entry.getKey();
      String docText = ClientTestingUtil.getText(entry.getValue());

      if (docId.equals(DocumentConstants.MANIFEST_DOCUMENT_ID)) {
        assertThat(output, doesNotContain(docId));
        // The manifest text should be empty, so we can't check that the output doesn't contain it.
      } else {
        assertThat(output, contains(docId));
        assertThat(output, contains(docText));
      }
    }

    // Probey should not report any blips from other waves.
    docs = util.getAllDocuments(otherWaveId);
    for (Entry<String, BlipData> entry : docs.entrySet()) {
      String docId = entry.getKey();
      String docText = ClientTestingUtil.getText(entry.getValue());

      assertThat(output, doesNotContain(docId));
      // The manifest text should be empty, so we can't check that the output doesn't contain it.
      if (!docId.equals(DocumentConstants.MANIFEST_DOCUMENT_ID)) {
        assertThat(output, doesNotContain(docText));
      }
    }
  }

  // Utility methods

  @Override
  protected Probey createAgent(AgentConnection connection) {
    return new Probey(connection);
  }

  /**
   * Add the specified user to the specfied wave (run the "add" command).
   *
   * @param waveId to add the user to.
   * @param user to add.
   * @return the output returned by Probey.
   */
  private String addUser(WaveId waveId, ParticipantId user) throws IOException {
    return runCommand(String.format("/add/%s/%s", waveId.serialise(), user.getAddress()));
  }

  /**
   * Add the a blip with the specified message to the specfied wave (run the "addblip" command).
   *
   * @param waveId to add the blip to.
   * @param message to add.
   * @return the output returned by Probey.
   */
  private String addBlip(WaveId waveId, String message) throws IOException {
    return runCommand(String.format("/addblip/%s/%s", waveId.serialise(), message));
  }

  /**
   * Create a new wave (run the "new" command).
   *
   * @return the output returned by Probey.
   */
  private String createNewWave() throws IOException {
    return runCommand("/new");
  }

  /**
   * Read the blips from the specified wave (run the "getblips" command).
   *
   * @param waveId to get the blips from.
   * @return the output returned by Probey.
   */
  private String getBlips(WaveId waveId) throws IOException {
    return runCommand(String.format("/getblips/%s", waveId.serialise()));
  }

  /**
   * Create a fake response object that we can pass to probey's web handler.
   *
   * @param outputStream for probey's response.
   * @return the fake response object.
   */
  private HttpServletResponse createFakeResponse(ByteArrayOutputStream outputStream)
      throws IOException {
    PrintWriter writer = new PrintWriter(outputStream, true);

    HttpServletResponse response = mock(HttpServletResponse.class);
    when(response.getWriter()).thenReturn(writer);

    return response;
  }

  /**
   * Tells probey to process the specified command.
   *
   * @param command url of the command that Probey should process
   * @return the output returned by Probey.
   */
  private String runCommand(String command) throws IOException {
    // Set up the request for Probey.
    Request request = new Request();
    request.setRequestURI(command);
    // Create a mock for the response.
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    HttpServletResponse response = createFakeResponse(output);

    // Send the command to Probey and process the server response.
    handler.handle("", null, request, response);
    backend.waitForAccumulatedEventsToProcess();

    verifyResponseParameters(response);
    return output.toString();
  }

  /**
   * Checks that probey has set up the response correctly.
   */
  private void verifyResponseParameters(HttpServletResponse response) {
    ArgumentCaptor<String> contentType = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Integer> status = ArgumentCaptor.forClass(Integer.class);

    verify(response, atLeastOnce()).setContentType(contentType.capture());
    verify(response, atLeastOnce()).setStatus(status.capture());
    // Check the latest values of the content type and status code.
    assertEquals(PLAIN_TEXT, contentType.getValue());
    assertEquals(HttpServletResponse.SC_OK, (long)status.getValue());
  }
}

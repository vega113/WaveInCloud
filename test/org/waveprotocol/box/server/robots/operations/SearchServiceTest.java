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

package org.waveprotocol.box.server.robots.operations;

import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.waveprotocol.box.server.util.testing.TestingConstants.OTHER_PARTICIPANT;
import static org.waveprotocol.box.server.util.testing.TestingConstants.PARTICIPANT;
import static org.waveprotocol.box.server.util.testing.TestingConstants.WAVE_ID;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.wave.api.InvalidRequestException;
import com.google.wave.api.JsonRpcConstant.ParamsProperty;
import com.google.wave.api.OperationRequest;
import com.google.wave.api.SearchResult;
import com.google.wave.api.SearchResult.Digest;

import junit.framework.TestCase;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.waveprotocol.box.server.robots.OperationContext;
import org.waveprotocol.box.server.robots.util.ConversationUtil;
import org.waveprotocol.box.server.waveserver.SearchProvider;
import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ConversationView;
import org.waveprotocol.wave.model.conversation.TitleHelper;
import org.waveprotocol.wave.model.conversation.WaveBasedConversationView;
import org.waveprotocol.wave.model.conversation.WaveletBasedConversation;
import org.waveprotocol.wave.model.document.util.LineContainers;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.operation.SilentOperationSink;
import org.waveprotocol.wave.model.operation.wave.BasicWaveletOperationContextFactory;
import org.waveprotocol.wave.model.testing.BasicFactories;
import org.waveprotocol.wave.model.testing.FakeIdGenerator;
import org.waveprotocol.wave.model.version.DistinctVersion;
import org.waveprotocol.wave.model.wave.ObservableWavelet;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.ParticipationHelper;
import org.waveprotocol.wave.model.wave.ReadOnlyWaveView;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.data.WaveViewData;
import org.waveprotocol.wave.model.wave.data.impl.WaveViewDataImpl;
import org.waveprotocol.wave.model.wave.data.impl.WaveletDataImpl;
import org.waveprotocol.wave.model.wave.opbased.OpBasedWavelet;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/**
 * Unit tests for {@link SearchService}.
 *
 * @author josephg@gmail.com (Joseph Gentle)
 */
public class SearchServiceTest extends TestCase {
  private static final ParticipantId USER = ParticipantId.ofUnsafe("me@example.com");
  private static final WaveletId CONVERSATION_WAVELET_ID =
      new WaveletId("example.com", "conv+root");

  private SearchService service;

  @Mock private SearchProvider searchProvider;
  @Mock private OperationRequest operation;
  @Mock private OperationContext context;

  /**
   * Builds a wavelet and provides direct access to the various layers of
   * abstraction.
   */
  private class TestingWaveletData {
    public final ObservableWaveletData waveletData;
    public final ObservableWavelet wavelet;
    public final ReadOnlyWaveView waveView;
    public final ConversationView conversationView;
    public final Conversation conversation;
    public final WaveViewData waveViewData;
  
    public TestingWaveletData(
        WaveId waveId, WaveletId waveletId, ParticipantId author, boolean isConversational) {
      waveletData =
        new WaveletDataImpl(waveletId, author, 1234567890, 0, DistinctVersion.ZERO, 0, waveId,
            BasicFactories.muteDocumentFactory());
      wavelet =
        new OpBasedWavelet(waveId, waveletData, new BasicWaveletOperationContextFactory(author),
            ParticipationHelper.IGNORANT, SilentOperationSink.Executor.build(waveletData),
            SilentOperationSink.VOID);
      waveView = new ReadOnlyWaveView(waveId);
      waveView.addWavelet(wavelet);
  
      if (isConversational) {
        conversationView = WaveBasedConversationView.create(waveView, FakeIdGenerator.create());
        WaveletBasedConversation.makeWaveletConversational(wavelet);
        conversation = conversationView.getRoot();
  
        conversation.addParticipant(author);
      } else {
        conversationView = null;
        conversation = null;
      }
  
      waveViewData = WaveViewDataImpl.create(waveId, ImmutableList.of(waveletData));
    }
  
    public void appendBlipWithText(String text) {
      ConversationBlip blip = conversation.getRootThread().appendBlip();
      LineContainers.appendToLastLine(blip.getContent(), XmlStringBuilder.createText(text));
      TitleHelper.maybeFindAndSetImplicitTitle(blip.getContent());
    }
  }

  @Override
  protected void setUp() {
    MockitoAnnotations.initMocks(this);

    when(operation.getParameter(ParamsProperty.QUERY)).thenReturn("in:inbox");
    IdGenerator idGenerator = mock(IdGenerator.class);
    service = new SearchService(searchProvider, new ConversationUtil(idGenerator));
  }
  
  public void testSearchWrapsSearchProvidersResult() throws InvalidRequestException {
    TestingWaveletData data =
        new TestingWaveletData(WAVE_ID, CONVERSATION_WAVELET_ID, PARTICIPANT, true);

    data.conversation.addParticipant(OTHER_PARTICIPANT);
    data.appendBlipWithText("title");

    when(searchProvider.search(USER, "in:inbox", 0, 10)).thenReturn(
        Arrays.asList(data.waveViewData));
    service.execute(operation, context, USER);

    verify(context).constructResponse(
        eq(operation),
        argThat(matchesSearchResult("in:inbox", WAVE_ID, "title", PARTICIPANT, ImmutableSet.of(
            PARTICIPANT, OTHER_PARTICIPANT), 0, 1)));
  }

  public void testWaveletWithNoBlipsResultsInEmptyTitleAndNoBlips() {
    TestingWaveletData data =
        new TestingWaveletData(WAVE_ID, CONVERSATION_WAVELET_ID, PARTICIPANT, true);

    Digest digest = service.generateDigestFromWavelet(data.waveletData);
    assertEquals("", digest.getTitle());
    assertEquals(digest.getBlipCount(), 0);
  }

  public void testWaveletWithNoConversationNotIncludedInResults() throws Exception {
    TestingWaveletData data =
      new TestingWaveletData(WAVE_ID, CONVERSATION_WAVELET_ID, PARTICIPANT, false);

    Digest digest = service.generateDigestFromWavelet(data.waveletData);
    assertNull(digest);

    when(searchProvider.search(USER, "in:inbox", 0, 10)).thenReturn(
        Arrays.asList(data.waveViewData));
    service.execute(operation, context, USER);

    verify(context).constructResponse(
        eq(operation), argThat(new BaseMatcher<Map<ParamsProperty, Object>>() {
          @SuppressWarnings("unchecked")
          @Override
          public boolean matches(Object item) {
            Map<ParamsProperty, Object> map = (Map<ParamsProperty, Object>) item;
            assertTrue(map.containsKey(ParamsProperty.SEARCH_RESULTS));

            Object resultsObj = map.get(ParamsProperty.SEARCH_RESULTS);
            SearchResult results = (SearchResult) resultsObj;

            assertEquals(0, results.getNumResults());
            assertEquals(0, results.getDigests().size());

            return true;
          }

          @Override
          public void describeTo(Description description) {
            description.appendText("Check digests match expected data");
          }
        }));
  }

  public void testDefaultFieldsMatchSpec() throws InvalidRequestException {
    service.execute(operation, context, USER);

    verify(searchProvider).search(USER, "in:inbox", 0, 10);
  }

  public void testSearchThrowsOnMissingQueryParameter() {
    when(operation.getParameter(ParamsProperty.QUERY)).thenReturn(null);
    try {
      service.execute(operation, context, USER);
      fail("Should have thrown an invalid request exception");
    } catch (InvalidRequestException e) {
      // pass.
    }
  }

  // *** Helpers

  public Matcher<Map<ParamsProperty, Object>> matchesSearchResult(final String query,
      final WaveId waveId, final String title, final ParticipantId author,
      final Set<ParticipantId> participants, final int unreadCount, final int blipCount) {
    return new BaseMatcher<Map<ParamsProperty, Object>>() {
      @SuppressWarnings("unchecked")
      @Override
      public boolean matches(Object item) {
        Map<ParamsProperty, Object> map = (Map<ParamsProperty, Object>) item;
        assertTrue(map.containsKey(ParamsProperty.SEARCH_RESULTS));
  
        Object resultsObj = map.get(ParamsProperty.SEARCH_RESULTS);
        SearchResult results = (SearchResult) resultsObj;
  
        assertEquals(query, results.getQuery());
        assertEquals(1, results.getNumResults());
  
        Digest digest = results.getDigests().get(0);
        assertEquals(title, digest.getTitle());
        assertEquals(waveId.serialise(), digest.getWaveId());
  
        Builder<ParticipantId> participantIds = ImmutableSet.builder();
        for (String name : digest.getParticipants()) {
          participantIds.add(ParticipantId.ofUnsafe(name));
        }
        assertEquals(participants, participantIds.build());
  
        assertEquals(unreadCount, digest.getUnreadCount());
        assertEquals(blipCount, digest.getBlipCount());
  
        return true;
      }
  
      @Override
      public void describeTo(Description description) {
        description.appendText("Check digests match expected data");
      }
    };
  }
}

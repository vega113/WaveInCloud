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

import com.google.common.annotations.VisibleForTesting;
import com.google.gxp.com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.wave.api.InvalidRequestException;
import com.google.wave.api.JsonRpcConstant.ParamsProperty;
import com.google.wave.api.OperationRequest;
import com.google.wave.api.SearchResult;
import com.google.wave.api.SearchResult.Digest;

import org.waveprotocol.box.common.Snippets;
import org.waveprotocol.box.server.robots.OperationContext;
import org.waveprotocol.box.server.robots.util.ConversationUtil;
import org.waveprotocol.box.server.robots.util.OperationUtil;
import org.waveprotocol.box.server.waveserver.SearchProvider;
import org.waveprotocol.wave.model.conversation.BlipIterators;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ObservableConversation;
import org.waveprotocol.wave.model.conversation.ObservableConversationBlip;
import org.waveprotocol.wave.model.conversation.ObservableConversationView;
import org.waveprotocol.wave.model.conversation.TitleHelper;
import org.waveprotocol.wave.model.conversation.WaveletBasedConversation;
import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.id.IdUtil;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.wave.ObservableWavelet;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.data.WaveViewData;
import org.waveprotocol.wave.model.wave.opbased.OpBasedWavelet;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * {@link OperationService} for the "search" operation.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 * @author josephg@gmail.com (Joseph Gentle)
 */
public class SearchService implements OperationService {
  /**
   * The number of search results to return if not defined in the request.
   * Defined in the spec.
   */
  private static final int DEFAULT_NUMBER_SEARCH_RESULTS = 10;
  private static final int DIGEST_SNIPPET_LENGTH = 30;
  private static final String EMPTY_WAVELET_TITLE = "";

  private final SearchProvider searchProvider;
  private final ConversationUtil conversationUtil;

  @Inject
  public SearchService(SearchProvider searchProvider, ConversationUtil conversationUtil) {
    this.searchProvider = searchProvider;
    this.conversationUtil = conversationUtil;
  }

  @Override
  public void execute(
      OperationRequest operation, OperationContext context, ParticipantId participant)
      throws InvalidRequestException {
    String query = OperationUtil.getRequiredParameter(operation, ParamsProperty.QUERY);
    int index = OperationUtil.getOptionalParameter(operation, ParamsProperty.INDEX, 0);
    int numResults = OperationUtil.getOptionalParameter(
        operation, ParamsProperty.NUM_RESULTS, DEFAULT_NUMBER_SEARCH_RESULTS);

    SearchResult result = search(participant, query, index, numResults);

    Map<ParamsProperty, Object> data =
        ImmutableMap.<ParamsProperty, Object> of(ParamsProperty.SEARCH_RESULTS, result);
    context.constructResponse(operation, data);
  }

  @VisibleForTesting
  SearchResult.Digest generateDigestFromWavelet(ObservableWaveletData waveletData) {
    ObservableWavelet wavelet = OpBasedWavelet.createReadOnly(waveletData);
    if (!WaveletBasedConversation.waveletHasConversation(wavelet)) {
      // Wavelet doesn't actually have a conversation model inside. Skip it.
      return null;
    }

    ObservableConversationView conversation = conversationUtil.getConversation(wavelet);
    ObservableConversation rootConversation = conversation.getRoot();
    if (rootConversation == null) {
      // Once again, there's no conversation here. Don't return it in the results.
      return null;
    }

    ObservableConversationBlip firstBlip = rootConversation.getRootThread().getFirstBlip();

    String title;
    if (firstBlip != null) {
      Document firstBlipContents = firstBlip.getContent();
      title = TitleHelper.extractTitle(firstBlipContents);
    } else {
      title = EMPTY_WAVELET_TITLE;
    }

    String snippet = Snippets.renderSnippet(waveletData, DIGEST_SNIPPET_LENGTH);
    String waveId = waveletData.getWaveId().serialise();
    List<String> participants = CollectionUtils.newArrayList();
    for (ParticipantId p : waveletData.getParticipants()) {
      participants.add(p.getAddress());
    }
    // TODO(josephg): Set unread count once user data wavelets are in.
    int unreadCount = 0;

    int blipCount = 0;
    for (ConversationBlip blip : BlipIterators.breadthFirst(rootConversation)) {
      blipCount++;
    }

    return new Digest(title, snippet, waveId, participants, waveletData.getLastModifiedTime(),
          unreadCount, blipCount);
  }

  private SearchResult search(
      ParticipantId participant, String query, int startAt, int numResults) {
    Collection<WaveViewData> results =
        searchProvider.search(participant, query, startAt, numResults);

    SearchResult result = new SearchResult(query);
    for (WaveViewData wave : results) {
      for (ObservableWaveletData waveletData : wave.getWavelets()) {
        if (!IdUtil.isConversationalId(waveletData.getWaveletId())) {
          // Wavelet isn't a conversation wave. It might be metadata or
          // something.
          // Skip it.
          continue;
        }

        Digest digest = generateDigestFromWavelet(waveletData);

        // TODO(josephg): This is not the behaviour. When multiple wavelets are returned
        // in a single wave, the search service should only generate & return one digest.
        // See bug: http://code.google.com/p/wave-protocol/issues/detail?id=123
        if (digest != null) {
          result.addDigest(digest);
        }
      }
    }

    return result;
  }
}

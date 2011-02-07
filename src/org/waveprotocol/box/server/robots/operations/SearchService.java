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
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.wave.api.ApiIdSerializer;
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
import org.waveprotocol.wave.model.id.ModernIdSerialiser;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.wave.ObservableWavelet;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.data.WaveViewData;
import org.waveprotocol.wave.model.wave.data.WaveletData;
import org.waveprotocol.wave.model.wave.opbased.OpBasedWavelet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
  private static final int PARTICIPANTS_SNIPPET_LENGTH = 5;
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
    // Attaching an OpBasedWavelet to a data object makes it an owner.
    // This means that copies must be made.
    ObservableWavelet wavelet = OpBasedWavelet.createReadOnly(waveletData);
    if (!WaveletBasedConversation.waveletHasConversation(wavelet)) {
      // Wavelet doesn't actually have a conversation model inside. Skip it.
      return null;
    }

    ObservableConversationView conversation = conversationUtil.buildConversation(wavelet);
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
    String waveId = ApiIdSerializer.instance().serialiseWaveId(waveletData.getWaveId());
    List<String> participants = CollectionUtils.newArrayList();
    for (ParticipantId p : waveletData.getParticipants()) {
      if (participants.size() < PARTICIPANTS_SNIPPET_LENGTH) {
        participants.add(p.getAddress());
      } else {
        break;
      }
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

  /** @return a digest for an empty wave. */
  private Digest emptyDigest(WaveViewData wave) {
    String title = ModernIdSerialiser.INSTANCE.serialiseWaveId(wave.getWaveId());
    String id = ApiIdSerializer.instance().serialiseWaveId(wave.getWaveId());
    return new Digest(title, "(empty)", id, Collections.<String> emptyList(), -1L, 0, 0);
  }

  /** @return a digest for an unrecognised type of wave. */
  private Digest unknownDigest(WaveViewData wave) {
    String title = ModernIdSerialiser.INSTANCE.serialiseWaveId(wave.getWaveId());
    String id = ApiIdSerializer.instance().serialiseWaveId(wave.getWaveId());
    long lmt = -1L;
    int docs = 0;
    List<String> participants = new ArrayList<String>();
    for (WaveletData data : wave.getWavelets()) {
      lmt = Math.max(lmt, data.getLastModifiedTime());
      docs += data.getDocumentIds().size();

      for (ParticipantId p : data.getParticipants()) {
        if (participants.size() < PARTICIPANTS_SNIPPET_LENGTH) {
          participants.add(p.getAddress());
        } else {
          break;
        }
      }
    }
    return new Digest(title, "(unknown)", id, participants, lmt, 0, docs);
  }

  // Note that this search implementation is only of prototype quality.
  private SearchResult search(
      ParticipantId participant, String query, int startAt, int numResults) {
    Collection<WaveViewData> results =
        searchProvider.search(participant, query, startAt, numResults);

    // Generate exactly one digest per wave. This includes conversational and
    // non-conversational waves. The position-based API for search prevents the
    // luxury of extra filtering here. Filtering can only be done in the
    // searchProvider. All waves returned by the search provider must be
    // included in the search result.
    SearchResult result = new SearchResult(query);
    outer: for (WaveViewData wave : results) {
      // Empty waves get an empty digest, waves with root conversations get a
      // digest from that, waves with a conversational wavelet get a digest from
      // the first such wavelet, then remaining waves get an unknown digest.
      //
      // This is not the ideal solution. In particular, a digest should be built
      // from all the conversations in a user's view of the wave, not just one.
      // See bug: http://code.google.com/p/wave-protocol/issues/detail?id=123
      //
      for (ObservableWaveletData waveletData : wave.getWavelets()) {
        if (IdUtil.isConversationRootWaveletId(waveletData.getWaveletId())) {
          result.addDigest(generateDigestFromWavelet(waveletData));
          continue outer;
        }
      }
      for (ObservableWaveletData waveletData : wave.getWavelets()) {
        if (IdUtil.isConversationalId(waveletData.getWaveletId())) {
          result.addDigest(generateDigestFromWavelet(waveletData));
          continue outer;
        }
      }
      // The wave matched the search, but it is unknown how to present it.
      boolean empty = !wave.getWavelets().iterator().hasNext();
      result.addDigest(empty ? emptyDigest(wave) : unknownDigest(wave));
    }

    assert result.getDigests().size() == results.size();
    return result;
  }
}

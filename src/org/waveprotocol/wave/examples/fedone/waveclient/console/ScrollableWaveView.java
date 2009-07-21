/**
 * Copyright 2009 Google Inc.
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

package org.waveprotocol.wave.examples.fedone.waveclient.console;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.waveprotocol.wave.examples.fedone.waveclient.common.ClientBackend;
import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMap;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.BufferedDocOp;
import org.waveprotocol.wave.model.document.operation.DocInitializationCursor;
import org.waveprotocol.wave.model.document.operation.impl.InitializationCursorAdapter;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.WaveViewData;
import org.waveprotocol.wave.model.wave.data.WaveletData;

import java.util.Collections;
import java.util.List;

/**
 * A {@link WaveViewData} wrapper that can be rendered and scrolled for the console.
 *
 *
 *
 */
public class ScrollableWaveView extends ConsoleScrollable {
  /** Wave we are wrapping. */
  private final WaveViewData wave;

  /** Backend this wave belongs to. */
  private final ClientBackend backend;

  /**
   * Create new scrollable wave view.
   *
   * @param backend the wave belongs to
   * @param wave to render
   */
  public ScrollableWaveView(ClientBackend backend, WaveViewData wave) {
    this.backend = backend;
    this.wave = wave;
  }

  /**
   * @return the wrapped wave view
   */
  public WaveViewData getWave() {
    return wave;
  }

  @Override
  public synchronized List<String> render(final int width, final int height) {
    final List<String> lines = Lists.newArrayList();
    final WaveletData convRoot = backend.getConversationRoot(wave);
    final StringBuilder currentLine = new StringBuilder();

    for (BufferedDocOp document : convRoot.getDocuments().values()) {
      document.apply(new InitializationCursorAdapter(
          new DocInitializationCursor() {
            @Override public void characters(String s) {
              currentLine.append(ConsoleUtils.renderNice(s));

              while (currentLine.length() >= width) {
                lines.add(currentLine.substring(0, width));
                currentLine.delete(0, width);
              }
            }

            @Override public void elementStart(String type, Attributes attrs) {
              if (type.equals(ConsoleUtils.LINE)) {
                if (!attrs.containsKey(ConsoleUtils.LINE_AUTHOR)) {
                  throw new IllegalArgumentException("Line element must have author");
                }

                ConsoleUtils.ensureWidth(width, currentLine);
                lines.add(currentLine.toString());
                currentLine.delete(0, currentLine.length() - 1);

                lines.add(ConsoleUtils.blankLine(width));
                lines.add(ConsoleUtils.ansiWrap(
                    ImmutableList.of(ConsoleUtils.ANSI_GREEN_FG),
                    ConsoleUtils.ensureWidth(width, attrs.get(ConsoleUtils.LINE_AUTHOR))));
              } else {
                throw new IllegalArgumentException("Unsupported element type " + type);
              }
            }

            @Override public void elementEnd() {}
            @Override public void annotationBoundary(AnnotationBoundaryMap map) {}
          }));
    }

    if (currentLine.length() > 0) {
      ConsoleUtils.ensureWidth(width, currentLine);
      lines.add(currentLine.toString());
    }

    // Also render a little header
    List<String> header = renderHeader(width);

    // In this case, actually want to scroll from the bottom
    Collections.reverse(lines);
    List<String> reverseScroll = scroll(height - header.size(), lines);
    Collections.reverse(reverseScroll);
    ConsoleUtils.ensureHeight(width, height - header.size(), reverseScroll);

    header.addAll(reverseScroll);
    return header;
  }

  /**
   * Render a header, containing extra information about the participants.
   *
   * @param width of the header
   * @return list of lines that make up the header
   */
  private List<String> renderHeader(int width) {
    List<ParticipantId> participants = backend.getConversationRoot(wave).getParticipants();
    StringBuilder participantLineBuilder = new StringBuilder();

    if (participants.isEmpty()) {
      participantLineBuilder.append("No participants!?");
    } else {
      participantLineBuilder.append("With ");
      participantLineBuilder.append(participants.get(0));

      for (int i = 1; i < participants.size(); i++) {
        participantLineBuilder.append(", ");
        participantLineBuilder.append(participants.get(i));
      }
    }

    String participantLine = ConsoleUtils.ensureWidth(width, participantLineBuilder.toString());
    List<String> lines = Lists.newArrayList();

    lines.add(ConsoleUtils.ansiWrap(ImmutableList.of(ConsoleUtils.ANSI_YELLOW_FG), participantLine));
    lines.add(ConsoleUtils.ensureWidth(width, "----"));

    return lines;
  }
}

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

import com.google.common.collect.Lists;

import org.waveprotocol.wave.examples.fedone.waveclient.common.ClientUtils;
import org.waveprotocol.wave.examples.fedone.waveclient.common.ClientWaveView;
import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMap;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.BufferedDocOp;
import org.waveprotocol.wave.model.document.operation.DocInitializationCursor;
import org.waveprotocol.wave.model.document.operation.impl.InitializationCursorAdapter;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

/**
 * A {@link ClientWaveView} wrapper that can be rendered and scrolled for the console.
 *
 *
 *
 */
public class ScrollableWaveView extends ConsoleScrollable {
  public enum RenderMode {
    NORMAL,
    XML
  }

  /** Wave we are wrapping. */
  private final ClientWaveView wave;

  /** Render mode. */
  private RenderMode renderMode = RenderMode.NORMAL;

  /**
   * Create new scrollable wave view.
   *
   * @param wave to render
   */
  public ScrollableWaveView(ClientWaveView wave) {
    this.wave = wave;
  }

  /**
   * @return the wrapped wave view
   */
  public ClientWaveView getWave() {
    return wave;
  }

  @Override
  public synchronized List<String> render(final int width, final int height) {
    final List<String> lines = Lists.newArrayList();
    final StringBuilder currentLine = new StringBuilder();
    final Deque<String> elemStack = new LinkedList<String>();

    for (BufferedDocOp document : ClientUtils.getConversationRoot(wave).getDocuments().values()) {
      document.apply(new InitializationCursorAdapter(
          new DocInitializationCursor() {
            @Override public void characters(String s) {
              currentLine.append(ConsoleUtils.renderNice(s));
              wrap(lines, width, currentLine);
            }

            @Override
            public void elementStart(String type, Attributes attrs) {
              elemStack.push(type);

              if (renderMode.equals(RenderMode.NORMAL)) {
                if (type.equals(ConsoleUtils.LINE)) {
                  if (!attrs.containsKey(ConsoleUtils.LINE_AUTHOR)) {
                    throw new IllegalArgumentException("Line element must have author");
                  }

                  ConsoleUtils.ensureWidth(width, currentLine);
                  lines.add(currentLine.toString());
                  currentLine.delete(0, currentLine.length() - 1);

                  lines.add(ConsoleUtils.blankLine(width));
                  lines.add(ConsoleUtils.ansiWrap(ConsoleUtils.ANSI_GREEN_FG,
                      ConsoleUtils.ensureWidth(width, attrs.get(ConsoleUtils.LINE_AUTHOR))));
                } else {
                  throw new IllegalArgumentException("Unsupported element type " + type);
                }
              } else if (renderMode.equals(RenderMode.XML)) {
                if (attrs.isEmpty()) {
                  currentLine.append("<" + type + ">");
                } else {
                  currentLine.append("<" + type + " ");
                  for (String key : attrs.keySet()) {
                    currentLine.append(key + "=\"" + attrs.get(key) + "\"");
                  }
                  currentLine.append(">");
                }
              }
            }

            @Override
            public void elementEnd() {
              String type = elemStack.pop();

              if (renderMode.equals(RenderMode.XML)) {
                currentLine.append("</" + type + ">");
                wrap(lines, width, currentLine);
              }
            }

            @Override public void annotationBoundary(AnnotationBoundaryMap map) {}
          }));
    }

    if (currentLine.length() > 0) {
      ConsoleUtils.ensureWidth(width, currentLine);
      lines.add(currentLine.toString());
    }

    // Also render a header, not too big...
    List<String> header = renderHeader(width);

    while (header.size() > height / 2) {
      header.remove(header.size() - 1);
    }

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
    List<String> lines = Lists.newArrayList();
    List<ParticipantId> participants = ClientUtils.getConversationRoot(wave).getParticipants();
    StringBuilder participantLineBuilder = new StringBuilder();

    // HashedVersion
    lines.add("Version " + wave.getWaveletVersion(ClientUtils.getConversationRootId(wave)));

    // Participants
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

    // Render as lines
    wrap(lines, width, participantLineBuilder);
    if (participantLineBuilder.length() > 0) {
      lines.add(participantLineBuilder.toString());
    }

    for (int i = 0; i < lines.size(); i++) {
      lines.set(i, ConsoleUtils.ansiWrap(ConsoleUtils.ANSI_YELLOW_FG, lines.get(i)));
    }

    lines.add(ConsoleUtils.ensureWidth(width, "----"));

    return lines;
  }

  /**
   * Wrap a line by continually removing characters from a string and adding to a list of lines,
   * until the line is shorter than width.
   *
   * @param lines to append the wrapped string to
   * @param width to wrap
   * @param line to wrap
   */
  private void wrap(List<String> lines, int width, StringBuilder line) {
    while (line.length() >= width) {
      lines.add(line.substring(0, width));
      line.delete(0, width);
    }
  }

  /**
   * Set rendering mode.
   *
   * @param mode for rendering
   */
  public void setRenderingMode(RenderMode mode) {
    this.renderMode = mode;
  }
}

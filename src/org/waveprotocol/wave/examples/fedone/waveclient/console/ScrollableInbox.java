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

import org.waveprotocol.wave.examples.fedone.waveclient.common.ClientBackend;
import org.waveprotocol.wave.examples.fedone.waveclient.common.ClientUtils;
import org.waveprotocol.wave.examples.fedone.waveclient.common.IndexUtils;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.wave.data.WaveViewData;

import java.util.List;

/**
 * Wraps and renders the index wave.
 *
 *
 */
public class ScrollableInbox extends ConsoleScrollable {
  /** Index wave we are rendering. */
  private final WaveViewData indexWave;

  /** Backend the index wave belongs to. */
  private final ClientBackend backend;

  /** Open wave, to potentially render differently. */
  private WaveViewData openWave = null;

  /**
   * Create new scrollable inbox given an index wave.
   *
   * @param backend the index wave belongs to
   * @param indexWave to render
   */
  public ScrollableInbox(ClientBackend backend, WaveViewData indexWave) {
    if (!IndexUtils.isIndexWave(indexWave)) {
      throw new IllegalArgumentException("Wave is not an index wave");
    }

    this.indexWave = indexWave;
    this.backend = backend;
  }

  @Override
  public List<String> render(int width, int height) {
    List<WaveId> indexEntries = IndexUtils.getIndexEntries(indexWave);
    List<String> lines = Lists.newArrayList();

    for (int i = 0; i < indexEntries.size(); i++) {
      WaveViewData wave = backend.getWave(indexEntries.get(i));
      StringBuilder line = new StringBuilder(String.format("%4d) ", i));

      boolean isOpen = false;
      boolean isUnread = false;

      if (wave == null) {
        line.append("..");
      } else if (backend.getConversationRoot(wave) == null) {
        line.append("...");
      } else {
        if (openWave != null && wave.equals(openWave)) {
          isOpen = true;
        }

        line.append(String.format("(%s) ", wave.getWaveId().getId()));
        String digest = ClientUtils.renderDocuments(wave);
        line.append(ConsoleUtils.renderNice(digest));
      }

      List<Integer> ansiCodes = Lists.newArrayList();

      // TODO support this
      if (isUnread) {
        ansiCodes.add(ConsoleUtils.ANSI_BLUE_FG);
      }

      if (isOpen) {
        ansiCodes.add(ConsoleUtils.ANSI_BLUE_BG);
        ansiCodes.add(ConsoleUtils.ANSI_WHITE_FG);
      }

      ConsoleUtils.ensureWidth(width, line);
      lines.add(ConsoleUtils.ansiWrap(ansiCodes, line.toString()));
    }

    List<String> scrolledLines = scroll(height, lines);
    ConsoleUtils.ensureHeight(width, height, scrolledLines);
    return scrolledLines;
  }

  /**
   * Set the open wave.
   *
   * @param openWave the new open wave
   */
  public void setOpenWave(WaveViewData openWave) {
    this.openWave = openWave;
  }
}

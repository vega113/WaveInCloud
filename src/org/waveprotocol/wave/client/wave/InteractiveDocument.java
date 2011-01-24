/**
 * Copyright 2010 Google Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.waveprotocol.wave.client.wave;

import org.waveprotocol.wave.client.common.util.LogicalPanel;
import org.waveprotocol.wave.client.editor.content.ContentDocument;
import org.waveprotocol.wave.client.editor.content.Registries;

/**
 * A document implementation that is suitable for use in an interactive client.
 * An interactive document has three primary concerns:
 * <ul>
 * <li>it reveals a {@link ContentDocument}, for sophisticated document
 * rendering;</li>
 * <li>it exposes rendering control, to start and stop live rendering; and</li>
 * <li>it exposes reading control (diff highlighting).</li>
 * </ul>
 *
 * @author hearnden@google.com (David Hearnden)
 */
public interface InteractiveDocument {
  /**
   * @return the document implementation.
   */
  ContentDocument getDocument();

  /**
   * Renders this document, keeping the rendering live until
   * {@link #stopRendering() stopped}.
   *
   * @param registries rendering definitions and event handlers for doodads.
   * @param panel parent for adoption of GWT widgets that may occur in doodads.
   */
  void startRendering(Registries registries, LogicalPanel panel);

  /**
   * Stops rendering this document.
   */
  void stopRendering();

  /**
   * Enters a diff-disabling scope. Diffs are cleared, and diffs are not shown
   * until {@link #stopDiffSuppression() exited}. This scope can not be entered
   * while diff clearing is disabled (i.e., within a diff retention scope).
   *
   * @throws IllegalStateException if currently in either a diff-suppression or
   *         diff-retention scope.
   */
  void startDiffSuppression();

  /**
   * Leaves the diff-suppression scope.
   *
   * @throws IllegalStateException if not in a diff-suppression scope.
   */
  void stopDiffSuppression();

  /**
   * Enters a diff-retention scope. Calls to {@link #clearDiffs} will have no
   * effect until {@link #enableDiffClearing() exited}. A diff-retention scope
   * may be entered within a diff-suppressed scope.
   *
   * @throws IllegalStateException if currently in a retention scope.
   */
  void disableDiffClearing();

  /**
   * Leaves the diff-retention scope, undoing the effect of the last call to
   * {@link #disableDiffClearing}.
   *
   * @throws IllegalStateException if not in a diff-retention scope.
   */
  void enableDiffClearing();

  /** Collapses any diff state. */
  void clearDiffs();

  /** @return true if this document has no state other than diff state. */
  boolean isCompleteDiff();
}

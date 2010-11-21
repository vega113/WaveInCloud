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

package org.waveprotocol.wave.client.editor.harness;

import com.google.common.base.Preconditions;

import org.waveprotocol.wave.client.doodad.diff.DiffController;
import org.waveprotocol.wave.client.editor.DiffState;
import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.editor.content.ContentDocument;
import org.waveprotocol.wave.client.editor.content.DiffHighlightingFilter;
import org.waveprotocol.wave.common.logging.LoggerBundle;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.operation.OperationException;

/**
 * This class maintains diff state and consumes diff ops
 *
 */
public final class HighlightingDiffState implements DiffState, DiffController {

  private final LoggerBundle logger;

  // Keeps track of the currentContentDoc associated with this DiffState.
  private ContentDocument currentContentDoc = null;

  // The diffFilter corresponding to the current content document. If the
  // contentDocument is changed, this needs to be reinitialized.
  private DiffHighlightingFilter diffFilter;

  // Whether to highlight diffs as remote ops are applied
  private boolean showDiffMode = false;

  // The corresponding editor
  private Editor editor;

  public HighlightingDiffState(LoggerBundle logger) {
    this.logger = logger;
  }

  @Override
  public void clearDiffs() {
    // This null check is needed when the editor is not rendered, hence
    // diffFilter not attached.
    DiffHighlightingFilter filter = getDiffFilter();
    if (filter != null) {
      filter.clearDiffs();
    } else {
      logger.error().log("clear diff called with uninitialized filter");
    }
  }

  @Override
  public void setShowDiffMode(boolean isOn) {
    showDiffMode = isOn;
  }

  @Override
  public boolean shouldShowAsDiff() {
    return showDiffMode; // TODO(user) Clean this up: && !WaveletUndoController.isUndoing();
  }

  /**
   * Consumes a diff op.
   *
   * @param operation
   * @throws OperationException
   */
  public void consume(DocOp operation) throws OperationException {
    Preconditions.checkState(shouldShowAsDiff());
    DiffHighlightingFilter filter = getDiffFilter();
    if (filter != null) {
      filter.consume(operation);
    } else {
      logger.error().log("clear diff called with uninitialized filter");
    }
  }

  /**
   * Ideally, this should be setDocument, however, the document can change from
   * under an editor and be attached/detached at any time. So it is safer to do
   * this.
   *
   * @param e
   */
  public void setEditor(Editor e) {
    this.editor = e;

    // Null these out when we set a new editor. These will be lazily assigned when required.
    currentContentDoc = null;
    diffFilter = null;
  }

  /**
   * Resets the DiffState.
   */
  public void reset() {
    showDiffMode = false;
    diffFilter = null;
    currentContentDoc = null;
  }

  private DiffHighlightingFilter getDiffFilter() {
    if (editor == null || editor.getContent()  == null) {
      return null;
    }

    if (editor.getContent() != currentContentDoc) {
      currentContentDoc = editor.getContent();
      diffFilter = new DiffHighlightingFilter(currentContentDoc.getDiffTarget());
    }

    return diffFilter;
  }
}

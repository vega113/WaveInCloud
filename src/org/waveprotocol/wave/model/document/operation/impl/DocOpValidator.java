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

package org.waveprotocol.wave.model.document.operation.impl;

import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMap;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.AttributesUpdate;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.DocOpCursor;
import org.waveprotocol.wave.model.document.operation.automaton.AutomatonDocument;
import org.waveprotocol.wave.model.document.operation.automaton.DocOpAutomaton;
import org.waveprotocol.wave.model.document.operation.automaton.DocOpAutomaton.ValidationResult;
import org.waveprotocol.wave.model.document.operation.automaton.DocOpAutomaton.ViolationCollector;

/**
 * Validates an operation against a document.
 *
 *
 */
public final class DocOpValidator {

  private DocOpValidator() {}

  /**
   * Returns whether m is a well-formed document initialization and satisfies
   * the given schema constraints.
   */
  public static ViolationCollector validate(DocInitialization m) {
    return validate(DocOpAutomaton.EMPTY_DOCUMENT, m);
  }

  private static final class IllFormed extends RuntimeException {}

  /**
   * Returns whether m is well-formed, applies to doc, and preserves the given
   * schema constraints.  Will not modify doc.
   */
  public static <N, E extends N, T extends N> ViolationCollector validate(
      AutomatonDocument doc, DocOp m) {
    final DocOpAutomaton a = new DocOpAutomaton(doc);
    final ViolationCollector v = new ViolationCollector();
    try {
      m.apply(new DocOpCursor() {
        @Override
        public void characters(String s) {
          if (a.checkCharacters(s, v).isIllFormed()) {
            throw new IllFormed();
          }
          a.doCharacters(s);
        }

        @Override
        public void deleteCharacters(String chars) {
          if (a.checkDeleteCharacters(chars, v).isIllFormed()) {
            throw new IllFormed();
          }
          a.doDeleteCharacters(chars);
        }

        @Override
        public void deleteElementEnd() {
          if (a.checkDeleteElementEnd(v).isIllFormed()) {
            throw new IllFormed();
          }
          a.doDeleteElementEnd();
        }

        @Override
        public void deleteElementStart(String type, Attributes attrs) {
          if (a.checkDeleteElementStart(type, attrs, v).isIllFormed()) {
            throw new IllFormed();
          }
          a.doDeleteElementStart(type, attrs);
        }

        @Override
        public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
          if (a.checkReplaceAttributes(oldAttrs, newAttrs, v).isIllFormed()) {
            throw new IllFormed();
          }
          a.doReplaceAttributes(oldAttrs, newAttrs);
        }

        @Override
        public void retain(int itemCount) {
          if (a.checkRetain(itemCount, v).isIllFormed()) {
            throw new IllFormed();
          }
          a.doRetain(itemCount);
        }

        @Override
        public void updateAttributes(AttributesUpdate u) {
          if (a.checkUpdateAttributes(u, v).isIllFormed()) {
            throw new IllFormed();
          }
          a.doUpdateAttributes(u);
        }

        @Override
        public void annotationBoundary(AnnotationBoundaryMap map) {
          if (a.checkAnnotationBoundary(map, v).isIllFormed()) {
            throw new IllFormed();
          }
          a.doAnnotationBoundary(map);
        }

        @Override
        public void elementEnd() {
          if (a.checkElementEnd(v).isIllFormed()) {
            throw new IllFormed();
          }
          a.doElementEnd();
        }

        @Override
        public void elementStart(String type, Attributes attrs) {
          if (a.checkElementStart(type, attrs, v).isIllFormed()) {
            throw new IllFormed();
          }
          a.doElementStart(type, attrs);
        }
      });
    } catch (IllFormed e) {
      return v;
    }
    if (a.checkFinish(v) == ValidationResult.ILL_FORMED) {
      return v;
    }
    return v;
  }

}

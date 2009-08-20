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
 */

package org.waveprotocol.wave.model.operation;

import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.impl.DocOpUtil;
import org.waveprotocol.wave.model.operation.wave.AddParticipant;
import org.waveprotocol.wave.model.operation.wave.NoOp;
import org.waveprotocol.wave.model.operation.wave.RemoveParticipant;
import org.waveprotocol.wave.model.operation.wave.WaveletDocumentOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.util.Preconditions;

/**
 * Utilities for comparing operations.
 */
public class OpComparators {
  private OpComparators() {}

  // Proper equators would also provide hash codes.

  public interface OpEquator {
    boolean equalNullable(DocOp a, DocOp b);
    boolean equal(DocOp a, DocOp b);

    boolean equalNullable(WaveletOperation a, WaveletOperation b);
    boolean equal(WaveletOperation a, WaveletOperation b);
  }

  public abstract static class AbstractOpEquator implements OpEquator {
    @Override
    public boolean equal(WaveletOperation a, WaveletOperation b) {
      Preconditions.checkNotNull(a, "First argument is null");
      Preconditions.checkNotNull(b, "Second argument is null");
      return equalNullable(a, b);
    }

    @Override
    public boolean equalNullable(WaveletOperation a, WaveletOperation b) {
      if (a == null) {
        return b == null;
      } else if (a instanceof AddParticipant) {
        return (b instanceof AddParticipant)
            && ((AddParticipant) a).getParticipantId().equals(
                ((AddParticipant) b).getParticipantId());
      } else if (a instanceof RemoveParticipant) {
        return (b instanceof RemoveParticipant)
            && ((RemoveParticipant) a).getParticipantId().equals(
                ((RemoveParticipant) b).getParticipantId());
      } else if (a instanceof NoOp) {
        return b instanceof NoOp;
      } else if (a instanceof WaveletDocumentOperation) {
        if (!(b instanceof WaveletDocumentOperation)) {
          return false;
        }
        WaveletDocumentOperation a1 = (WaveletDocumentOperation) a;
        WaveletDocumentOperation b1 = (WaveletDocumentOperation) b;
        return (a1.getDocumentId().equals(b1.getDocumentId())
            && equal(a1.getOperation(), b1.getOperation()));
      } else {
        throw new AssertionError("Unexpected WaveletOperation subtype: " + a);
      }
    }
  }

  public static final OpEquator SYNTACTIC_IDENTITY = new AbstractOpEquator() {
    @Override
    public boolean equal(DocOp a, DocOp b) {
      Preconditions.checkNotNull(a, "First argument is null");
      Preconditions.checkNotNull(b, "Second argument is null");
      return equalNullable(a, b);
    }

    @Override
    public boolean equalNullable(DocOp a, DocOp b) {
      if (a == null) {
        return b == null;
      }
      if (b == null) {
        return false;
      }
      // TODO: Comparing by stringifying is unnecessarily expensive.
      return DocOpUtil.toConciseString(a).equals(DocOpUtil.toConciseString(b));
    }
  };

}

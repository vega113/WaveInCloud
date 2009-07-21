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

package org.waveprotocol.wave.model.operation.wave;

import org.waveprotocol.wave.model.document.operation.BufferedDocOp;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.wave.data.WaveletData;

/**
 * Operation class for an operation that will modify a document within a given wavelet.
 *
 *
 */
public class WaveletDocumentOperation extends WaveletOperation {
  /** Identifier of the document within the target wavelet to modify. */
  private final String documentId;

  /** Document operation which modifies the target document. */
  private final BufferedDocOp operation;

  /**
   * Constructor.
   *
   * @param documentId
   * @param operation
   */
  public WaveletDocumentOperation(String documentId, BufferedDocOp operation) {
    this.documentId = documentId;
    this.operation = operation;
  }

  public String getDocumentId() {
    return documentId;
  }

  public BufferedDocOp getOperation() {
    return operation;
  }

  @Override
  protected void doApply(WaveletData target) throws OperationException {
    target.modifyDocument(documentId, operation);
  }

  @Override
  public int hashCode() {
    // (see Effective Java)
    int result = 17;

    result = 31 * result + documentId.hashCode();
    result = 31 * result + operation.hashCode();

    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof WaveletDocumentOperation)) {
      return false;
    }
    WaveletDocumentOperation wop = (WaveletDocumentOperation) obj;
    return documentId.equals(wop.documentId) && operation.equals(wop.operation);
  }

  @Override
  public int size() {
    return operation.size();
  }
}

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
package org.waveprotocol.wave.model.undo;


import org.waveprotocol.wave.model.document.operation.BufferedDocOp;
import org.waveprotocol.wave.model.document.operation.algorithm.Composer;
import org.waveprotocol.wave.model.document.operation.algorithm.DocOpInverter;
import org.waveprotocol.wave.model.document.operation.algorithm.Transformer;
import org.waveprotocol.wave.model.operation.OperationPair;
import org.waveprotocol.wave.model.operation.TransformException;

import java.util.List;

/**
 * A factory for creating undo managers for document operations.
 *
 */
public final class UndoManagerFactory {

  private static final UndoManagerImpl.Algorithms<BufferedDocOp> algorithms =
      new UndoManagerImpl.Algorithms<BufferedDocOp>() {

    @Override
    public BufferedDocOp invert(BufferedDocOp operation) {
      return DocOpInverter.invert(operation);
    }

    @Override
    public BufferedDocOp compose(List<BufferedDocOp> operations) {
      return Composer.compose(operations);
    }

    @Override
    public OperationPair<BufferedDocOp> transform(BufferedDocOp operation1,
        BufferedDocOp operation2) throws TransformException {
      return Transformer.transform(operation1, operation2);
    }

  };

  /**
   * Creates a new undo manager.
   *
   * @return A new undo manager.
   */
  public static UndoManagerPlus<BufferedDocOp> createUndoManager() {
    return new UndoManagerImpl<BufferedDocOp>(algorithms);
  }

}

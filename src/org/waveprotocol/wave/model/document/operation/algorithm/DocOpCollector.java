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

package org.waveprotocol.wave.model.document.operation.algorithm;

import org.waveprotocol.wave.model.document.operation.BufferedDocOp;
import org.waveprotocol.wave.model.operation.OperationException;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * A class that collects document operations together and composes them in an
 * efficient manner.
 */
public final class DocOpCollector {

  private final List<BufferedDocOp> operations = new ArrayList<BufferedDocOp>();

  public void add(BufferedDocOp operation) {
    ListIterator<BufferedDocOp> iterator = operations.listIterator();
    while (iterator.hasNext()) {
      BufferedDocOp nextOperation = iterator.next();
      if (nextOperation == null) {
        iterator.set(operation);
        return;
      }
      iterator.set(null);
      operation = compose(nextOperation, operation);
    }
    operations.add(operation);
  }

  public BufferedDocOp composeAll() {
    BufferedDocOp result = null;
    for (BufferedDocOp operation : operations) {
      if (operation != null) {
        result = (result != null) ? compose(operation, result) : operation;
      }
    }
    operations.clear();
    return result;
  }

  private BufferedDocOp compose(BufferedDocOp op1, BufferedDocOp op2) {
    try {
      return Composer.compose(op1, op2);
    } catch (OperationException e) {
      throw new IllegalArgumentException();
    }
  }

}

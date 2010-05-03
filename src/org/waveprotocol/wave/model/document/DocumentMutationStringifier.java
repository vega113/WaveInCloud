// Copyright 2008 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document;

import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.impl.DocOpUtil;


/**
 * A class for creating a debugging string from a DocOp.
 *
*
 */
public final class DocumentMutationStringifier {

  // TODO(ohler): eliminate this class

  private DocumentMutationStringifier() {}

  public static String stringify(DocOp op) {
    return DocOpUtil.toConciseString(op);
  }

}

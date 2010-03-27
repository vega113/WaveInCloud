// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.util;

import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.impl.DocOpUtil;

/**
 * A ModifiableDocument that takes DocumentOperations that are applied to it
 * and prints out Java code to reconstruct these DocumentOperations.
 *
 * @author ohler@google.com (Christian Ohler)
 */
public class DocumentOperationCodePrinter {

  // TODO(ohler): bring this functionality back, but put it in DocOpUtil.

  public static String getCode(DocOp op, String targetDocumentSymbolName) {
    return "Java code that corresponds to " + DocOpUtil.toConciseString(op);
  }

  /**
   * Handy utility method to put the java code into a string
   *
   * @param op
   * @param lineDelimiter e.g. "\n"
   * @param targetDocumentSymbolName
   * @return java code
   */
  // Using a lineDelimiter that has no \n in it won't interact well with
  // the way actionWithAttributes tries to indent output.
  public static String getCode(DocOp op,
      String targetDocumentSymbolName, final String lineDelimiter) {
    return "Java code that corresponds to " + DocOpUtil.toConciseString(op);
  }

  /**
   * Minimal stream interface to avoid the need for external dependencies
   */
  public interface SimpleStream {
    /** Send a line to the stream */
    void println(String str);
  }

  private DocumentOperationCodePrinter(SimpleStream out, String targetDocumentSymbolName) {
  }

}

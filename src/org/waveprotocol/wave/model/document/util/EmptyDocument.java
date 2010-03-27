// Copyright 2008 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.util;

import org.waveprotocol.wave.model.document.operation.BufferedDocInitialization;
import org.waveprotocol.wave.model.document.operation.BufferedDocOp;
import org.waveprotocol.wave.model.document.operation.impl.DocInitializationBuilder;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuilder;

/**
 * A utility class containing an empty document.
 *
*
 */
public final class EmptyDocument {

  /**
   * An empty document.
   */
  public static final BufferedDocInitialization EMPTY_DOCUMENT =
      new DocInitializationBuilder().build();

  /**
   * An empty document operation.
   */
  public static final BufferedDocOp EMPTY_DOC_OP = new DocOpBuilder().build();

  private EmptyDocument() {}

}

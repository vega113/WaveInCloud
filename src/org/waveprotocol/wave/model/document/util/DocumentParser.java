// Copyright 2008 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.util;

/**
 * Something that can parse text into a document.
 *
 * @param <D> document type produced by this parser
 */
public interface DocumentParser<D> {
  /**
   * Creates a document by parsing XML text.
   *
   * @param text  XML text to parse
   * @return new document constructed from {@code text}.
   */
  D parse(String text);
}

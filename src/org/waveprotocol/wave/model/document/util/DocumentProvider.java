// Copyright 2008 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.util;

import org.waveprotocol.wave.model.document.DocumentFactory;

import java.util.Map;

/**
 * A document provider implements the following mechanisms for obtaining a
 * document:
 * <ul>
 *   <li>{@link #create(String, Map) constructing};</li>
 *   <li>{@link #parse(String) parsing}; and</li>
 * </ul>
 *
 * @param <D> document type produced
 */
public interface DocumentProvider<D> extends DocumentFactory<D> {

  /**
   * Creates a document by parsing XML text.
   *
   * @param text  XML text to parse
   * @return new document constructed from {@code text}.
   */
  D parse(String text);

}

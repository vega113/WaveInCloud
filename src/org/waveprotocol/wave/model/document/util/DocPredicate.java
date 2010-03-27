// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.util;

import org.waveprotocol.wave.model.document.ReadableDocument;

/**
 * A predicate on document nodes, reusable independently
 * of the specific node implementations
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public interface DocPredicate {

  /**
   * @param doc document for the given node
   * @param node node the predicate is to be applied on
   * @return the result of this predicate on the node
   */
  <N, E extends N, T extends N> boolean apply(ReadableDocument<N, E, T> doc, N node);
}

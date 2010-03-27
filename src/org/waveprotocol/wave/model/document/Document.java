// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document;

/**
 * A mutable view of a wave document.
 *
 * A document comprises a DOM-style tree of nodes, which are elements with
 * attributes or text, and key-value annotation pairs over ranges of those
 * nodes. Locations within the document are given by integer offsets or {@code
 * Point}s, referencing a space between two nodes.
 *
 * Unlike traditional DOMs, the node types are not mutable objects; all
 * mutations are applied through this interface.
 *
 * The node types are represented by the vacuous {@code Doc.E} for elements,
 * {@code Doc.T} for text nodes, and {@code Doc.N} for both. These are type
 * parameters to {@link MutableDocument}, which is the Generic version of this
 * interface.
 *
 * @see MutableDocument
 * @see ReadableAnnotationSet
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public interface Document extends MutableDocument<Doc.N, Doc.E, Doc.T> {

}

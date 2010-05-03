// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.util;

import org.waveprotocol.wave.model.document.MutableAnnotationSet;
import org.waveprotocol.wave.model.document.MutableDocument;
import org.waveprotocol.wave.model.document.indexed.LocationMapper;
import org.waveprotocol.wave.model.document.raw.TextNodeOrganiser;

/**
 * An initial, tentative stab at what a document context would look like, based
 * on rough use cases. This needs to be compacted/refined muchly.
 *
 * TODO(danilatos): Carry through with planned changes to this interface.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 *
 * @param <N> Document Node
 * @param <E> Document Element
 * @param <T> Document Text Node
 */
public interface DocumentContext<N, E extends N, T extends N> {

  /** Document, persistent view, operation generating mutators, annotation set */
  MutableDocument<N, E, T> document();

  /** Full view of the document, with mutator accessors that do not affect the persistent view */
  LocalDocument<N, E, T> annotatableContent();

  /** Maps ints to points and vice versa in the persistent view */
  LocationMapper<N> locationMapper();

  /** Makes non-operation generating changes to text nodes */
  TextNodeOrganiser<T> textNodeOrganiser();

  /** A ReadableDocumentView interface for the persistent view */
  ReadableDocumentView<N, E, T> persistentView();

  /**
   * View of the hard elements plus text nodes
   * @see PersistentContent#hardView()
   */
  ReadableDocumentView<N, E, T> hardView();

  /** Non-persistent annotations for local book keeping. Can have values of any type. */
  MutableAnnotationSet.Local localAnnotations();

  /** For getting transient properties from an element */
  ElementManager<E> elementManager();
}

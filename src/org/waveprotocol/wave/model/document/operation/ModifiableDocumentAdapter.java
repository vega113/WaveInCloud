// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.operation;

import org.waveprotocol.wave.model.util.Preconditions;

/**
 * Adapter utility which wraps a DocOpCursor and delegates all calls to it.
 * Classes may extend this and implement whichever methods they want to add logic to.
 *
 * @author ohler@google.com (Christian Ohler)
 */
public abstract class ModifiableDocumentAdapter implements DocOpCursor {

  /** The cursor we are delegating to */
  protected final DocOpCursor target;

  /**
   * Constructs the adapter, taking the document that we will delegate to.
   * @param target Delegation target
   */
  protected ModifiableDocumentAdapter(DocOpCursor target) {
    Preconditions.checkNotNull(target, "Null target");
    this.target = target;
  }

  @Override
  public void annotationBoundary(AnnotationBoundaryMap map) {
    target.annotationBoundary(map);
  }

  @Override
  public void characters(String chars) {
    target.characters(chars);
  }

  @Override
  public void deleteCharacters(String chars) {
    target.deleteCharacters(chars);
  }

  @Override
  public void deleteElementEnd() {
    target.deleteElementEnd();
  }

  @Override
  public void deleteElementStart(String type, Attributes attrs) {
    target.deleteElementStart(type, attrs);
  }

  @Override
  public void elementEnd() {
    target.elementEnd();
  }

  @Override
  public void elementStart(String type, Attributes attrs) {
    target.elementStart(type, attrs);
  }

  @Override
  public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
    target.replaceAttributes(oldAttrs, newAttrs);
  }

  @Override
  public void retain(int itemCount) {
    target.retain(itemCount);
  }

  @Override
  public void updateAttributes(AttributesUpdate attrUpdate) {
    target.updateAttributes(attrUpdate);
  }

}

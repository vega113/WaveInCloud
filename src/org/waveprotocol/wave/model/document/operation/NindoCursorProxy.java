// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.operation;

import org.waveprotocol.wave.model.document.operation.Nindo.NindoCursor;

import java.util.Map;


/**
 * Delegates all methods to a wrapped target (useful for subclassing).
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public abstract class NindoCursorProxy implements NindoCursor {

  /** the target */
  protected final NindoCursor target;

  /**
   * @param target target to delegate to
   */
  protected NindoCursorProxy(NindoCursor target) {
    this.target = target;
  }

  public void begin() {
    target.begin();
  }

  public void characters(String s) {
    target.characters(s);
  }

  public void deleteCharacters(int n) {
    target.deleteCharacters(n);
  }

  public void deleteElementEnd() {
    target.deleteElementEnd();
  }

  public void deleteElementStart() {
    target.deleteElementStart();
  }

  public void elementEnd() {
    target.elementEnd();
  }

  public void elementStart(String type, Attributes attrs) {
    target.elementStart(type, attrs);
  }

  public void endAnnotation(String key) {
    target.endAnnotation(key);
  }

  public void finish() {
    target.finish();
  }

  public void replaceAttributes(Attributes attrs) {
    target.replaceAttributes(attrs);
  }

  public void skip(int n) {
    target.skip(n);
  }

  public void startAnnotation(String key, String value) {
    target.startAnnotation(key, value);
  }

  public void updateAttributes(Map<String, String> attrUpdate) {
    target.updateAttributes(attrUpdate);
  }
}

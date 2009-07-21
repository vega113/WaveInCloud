/**
 * Copyright 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.waveprotocol.wave.model.document.operation.algorithm;

import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMap;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.AttributesUpdate;
import org.waveprotocol.wave.model.document.operation.BufferedDocOp;
import org.waveprotocol.wave.model.document.operation.DocOpCursor;
import org.waveprotocol.wave.model.document.operation.EvaluatingDocOpCursor;
import org.waveprotocol.wave.model.document.operation.impl.AnnotationBoundaryMapImpl;
import org.waveprotocol.wave.model.document.operation.impl.AttributesUpdateImpl;
import org.waveprotocol.wave.model.document.operation.impl.BufferedDocOpImpl;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A utility class for composing document operations.
 *
 * TODO: Make detection of illegal compositions more thorough.
 *
 *
 */
public final class Composer {

  private static class ComposeException extends RuntimeException {

    ComposeException(String message) {
      super(message);
    }

  }

  private static abstract class Target implements DocOpCursor {

    abstract boolean isPreTarget();

  }

  private abstract class PreTarget extends Target {

    @Override
    public final void deleteCharacters(String chars) {
      preAnnotationQueue.flush();
      normalizer.deleteCharacters(chars);
    }

    @Override
    public final void deleteElementStart(String type, Attributes attrs) {
      preAnnotationQueue.flush();
      normalizer.deleteElementStart(type, attrs);
    }

    @Override
    public final void deleteElementEnd() {
      preAnnotationQueue.flush();
      normalizer.deleteElementEnd();
    }

    @Override
    public final void annotationBoundary(AnnotationBoundaryMap map) {
      preAnnotationQueue.queue(map);
    }

    @Override
    final boolean isPreTarget() {
      return true;
    }

  }

  private abstract class PostTarget extends Target {

    @Override
    public final void characters(String chars) {
      postAnnotationQueue.flush();
      normalizer.characters(chars);
    }

    @Override
    public final void elementStart(String type, Attributes attrs) {
      postAnnotationQueue.flush();
      normalizer.elementStart(type, attrs);
    }

    @Override
    public final void elementEnd() {
      postAnnotationQueue.flush();
      normalizer.elementEnd();
    }

    @Override
    public final void annotationBoundary(AnnotationBoundaryMap map) {
      postAnnotationQueue.queue(map);
    }

    @Override
    final boolean isPreTarget() {
      return false;
    }

  }

  private final class DefaultPreTarget extends PreTarget {

    @Override
    public void retain(int itemCount) {
      target = new RetainPostTarget(itemCount);
    }

    @Override
    public void characters(String chars) {
      target = new CharactersPostTarget(chars);
    }

    @Override
    public void elementStart(String type, Attributes attrs) {
      target = new ElementStartPostTarget(type, attrs);
    }

    @Override
    public void elementEnd() {
      target = new ElementEndPostTarget();
    }

    @Override
    public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
      target = new ReplaceAttributesPostTarget(oldAttrs, newAttrs);
    }

    @Override
    public void updateAttributes(AttributesUpdate attrUpdate) {
      target = new UpdateAttributesPostTarget(attrUpdate);
    }

  }

  private final class RetainPreTarget extends PreTarget {

    private int itemCount;

    RetainPreTarget(int itemCount) {
      this.itemCount = itemCount;
    }

    @Override
    public void retain(int itemCount) {
      flushAnnotations();
      if (itemCount <= this.itemCount) {
        normalizer.retain(itemCount);
        cancelRetain(itemCount);
      } else {
        normalizer.retain(this.itemCount);
        target = new RetainPostTarget(itemCount - this.itemCount);
      }
    }

    @Override
    public void characters(String chars) {
      flushAnnotations();
      if (chars.length() <= itemCount) {
        normalizer.characters(chars);
        cancelRetain(chars.length());
      } else {
        normalizer.characters(chars.substring(0, itemCount));
        target = new CharactersPostTarget(chars.substring(itemCount));
      }
    }

    @Override
    public void elementStart(String type, Attributes attrs) {
      flushAnnotations();
      normalizer.elementStart(type, attrs);
      cancelRetain(1);
    }

    @Override
    public void elementEnd() {
      flushAnnotations();
      normalizer.elementEnd();
      cancelRetain(1);
    }

    @Override
    public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
      flushAnnotations();
      normalizer.replaceAttributes(oldAttrs, newAttrs);
      cancelRetain(1);
    }

    @Override
    public void updateAttributes(AttributesUpdate attrsUpdate) {
      flushAnnotations();
      normalizer.updateAttributes(attrsUpdate);
      cancelRetain(1);
    }

    private void cancelRetain(int size) {
      if (size < itemCount) {
        itemCount -= size;
      } else {
        target = defaultTarget;
      }
    }

  }

  private final class DeleteCharactersPreTarget extends PreTarget {

    private String chars;

    DeleteCharactersPreTarget(String chars) {
      this.chars = chars;
    }

    @Override
    public void retain(int itemCount) {
      flushAnnotations();
      if (itemCount <= chars.length()) {
        normalizer.deleteCharacters(chars.substring(0, itemCount));
        cancelDeleteCharacters(itemCount);
      } else {
        normalizer.deleteCharacters(chars);
        target = new RetainPostTarget(itemCount - chars.length());
      }
    }

    @Override
    public void characters(String chars) {
      flushAnnotations();
      if (chars.length() <= this.chars.length()) {
        cancelDeleteCharacters(chars.length());
      } else {
        target = new CharactersPostTarget(chars.substring(this.chars.length()));
      }
    }

    @Override
    public void elementStart(String type, Attributes attrs) {
      throw new ComposeException("Illegal composition");
    }

    @Override
    public void elementEnd() {
      throw new ComposeException("Illegal composition");
    }

    @Override
    public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
      throw new ComposeException("Illegal composition");
    }

    @Override
    public void updateAttributes(AttributesUpdate attrUpdate) {
      throw new ComposeException("Illegal composition");
    }

    private void cancelDeleteCharacters(int size) {
      if (size < chars.length()) {
        chars = chars.substring(size);
      } else {
        target = defaultTarget;
      }
    }

  }

  private final class DeleteElementStartPreTarget extends PreTarget {

    private String type;
    private Attributes attrs;

    DeleteElementStartPreTarget(String type, Attributes attrs) {
      this.type = type;
      this.attrs = attrs;
    }

    @Override
    public void retain(int itemCount) {
      flushAnnotations();
      normalizer.deleteElementStart(type, attrs);
      if (itemCount > 1) {
        target = new RetainPostTarget(itemCount - 1);
      } else {
        target = defaultTarget;
      }
    }

    @Override
    public void characters(String chars) {
      throw new ComposeException("Illegal composition");
    }

    @Override
    public void elementStart(String type, Attributes attrs) {
      flushAnnotations();
      target = defaultTarget;
    }

    @Override
    public void elementEnd() {
      throw new ComposeException("Illegal composition");
    }

    @Override
    public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
      flushAnnotations();
      normalizer.deleteElementStart(type, oldAttrs);
      target = defaultTarget;
    }

    @Override
    public void updateAttributes(AttributesUpdate attrUpdate) {
      flushAnnotations();
      normalizer.deleteElementStart(type, attrs.updateWith(invertUpdate(attrUpdate)));
      target = defaultTarget;
    }

  }

  private final class DeleteElementEndPreTarget extends PreTarget {

    @Override
    public void retain(int itemCount) {
      flushAnnotations();
      normalizer.deleteElementEnd();
      if (itemCount > 1) {
        target = new RetainPostTarget(itemCount - 1);
      } else {
        target = defaultTarget;
      }
    }

    @Override
    public void characters(String chars) {
      throw new ComposeException("Illegal composition");
    }

    @Override
    public void elementStart(String type, Attributes attrs) {
      throw new ComposeException("Illegal composition");
    }

    @Override
    public void elementEnd() {
      flushAnnotations();
      target = defaultTarget;
    }

    @Override
    public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
      throw new ComposeException("Illegal composition");
    }

    @Override
    public void updateAttributes(AttributesUpdate attrUpdate) {
      throw new ComposeException("Illegal composition");
    }

  }

  private final class ReplaceAttributesPreTarget extends PreTarget {

    Attributes oldAttrs;
    Attributes newAttrs;

    ReplaceAttributesPreTarget(Attributes oldAttrs, Attributes newAttrs) {
      this.oldAttrs = oldAttrs;
      this.newAttrs = newAttrs;
    }

    @Override
    public void retain(int itemCount) {
      flushAnnotations();
      normalizer.replaceAttributes(oldAttrs, newAttrs);
      if (itemCount > 1) {
        target = new RetainPostTarget(itemCount - 1);
      } else {
        target = defaultTarget;
      }
    }

    @Override
    public void characters(String chars) {
      throw new ComposeException("Illegal composition");
    }

    @Override
    public void elementStart(String type, Attributes attrs) {
      flushAnnotations();
      normalizer.elementStart(type, newAttrs);
      target = defaultTarget;
    }

    @Override
    public void elementEnd() {
      throw new ComposeException("Illegal composition");
    }

    @Override
    public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
      flushAnnotations();
      normalizer.replaceAttributes(oldAttrs, this.newAttrs);
      target = defaultTarget;
    }

    @Override
    public void updateAttributes(AttributesUpdate attrUpdate) {
      flushAnnotations();
      normalizer.replaceAttributes(oldAttrs.updateWith(invertUpdate(attrUpdate)), newAttrs);
      target = defaultTarget;
    }

  }

  private final class UpdateAttributesPreTarget extends PreTarget {

    AttributesUpdate attrUpdate;

    UpdateAttributesPreTarget(AttributesUpdate attrUpdate) {
      this.attrUpdate = attrUpdate;
    }

    @Override
    public void retain(int itemCount) {
      flushAnnotations();
      normalizer.updateAttributes(attrUpdate);
      if (itemCount > 1) {
        target = new RetainPostTarget(itemCount - 1);
      } else {
        target = defaultTarget;
      }
    }

    @Override
    public void characters(String chars) {
      throw new ComposeException("Illegal composition");
    }

    @Override
    public void elementStart(String type, Attributes attrs) {
      flushAnnotations();
      normalizer.elementStart(type, attrs.updateWith(attrUpdate));
      target = defaultTarget;
    }

    @Override
    public void elementEnd() {
      throw new ComposeException("Illegal composition");
    }

    @Override
    public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
      flushAnnotations();
      normalizer.replaceAttributes(oldAttrs, newAttrs.updateWith(attrUpdate));
      target = defaultTarget;
    }

    @Override
    public void updateAttributes(AttributesUpdate attrUpdate) {
      flushAnnotations();
      normalizer.updateAttributes(attrUpdate.composeWith(this.attrUpdate));
      target = defaultTarget;
    }

  }

  private final class DefaultPostTarget extends PostTarget {

    @Override
    public void retain(int itemCount) {
      target = new RetainPreTarget(itemCount);
    }

    @Override
    public void deleteCharacters(String chars) {
      target = new DeleteCharactersPreTarget(chars);
    }

    @Override
    public void deleteElementStart(String type, Attributes attrs) {
      target = new DeleteElementStartPreTarget(type, attrs);
    }

    @Override
    public void deleteElementEnd() {
      target = new DeleteElementEndPreTarget();
    }

    @Override
    public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
      target = new ReplaceAttributesPreTarget(oldAttrs, newAttrs);
    }

    @Override
    public void updateAttributes(AttributesUpdate attrUpdate) {
      target = new UpdateAttributesPreTarget(attrUpdate);
    }

  }

  private final class RetainPostTarget extends PostTarget {

    private int itemCount;

    RetainPostTarget(int itemCount) {
      this.itemCount = itemCount;
    }

    @Override
    public void retain(int itemCount) {
      flushAnnotations();
      if (itemCount <= this.itemCount) {
        normalizer.retain(itemCount);
        cancelRetain(itemCount);
      } else {
        normalizer.retain(this.itemCount);
        target = new RetainPreTarget(itemCount - this.itemCount);
      }
    }

    @Override
    public void deleteCharacters(String chars) {
      flushAnnotations();
      if (chars.length() <= itemCount) {
        normalizer.deleteCharacters(chars);
        cancelRetain(chars.length());
      } else {
        normalizer.deleteCharacters(chars.substring(0, itemCount));
        target = new DeleteCharactersPreTarget(chars.substring(itemCount));
      }
    }

    @Override
    public void deleteElementStart(String type, Attributes attrs) {
      flushAnnotations();
      normalizer.deleteElementStart(type, attrs);
      cancelRetain(1);
    }

    @Override
    public void deleteElementEnd() {
      flushAnnotations();
      normalizer.deleteElementEnd();
      cancelRetain(1);
    }

    @Override
    public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
      flushAnnotations();
      normalizer.replaceAttributes(oldAttrs, newAttrs);
      cancelRetain(1);
    }

    @Override
    public void updateAttributes(AttributesUpdate attrUpdate) {
      flushAnnotations();
      normalizer.updateAttributes(attrUpdate);
      cancelRetain(1);
    }

    private void cancelRetain(int size) {
      if (size < itemCount) {
        itemCount -= size;
      } else {
        target = defaultTarget;
      }
    }

  }

  private final class CharactersPostTarget extends PostTarget {

    private String chars;

    CharactersPostTarget(String chars) {
      this.chars = chars;
    }

    @Override
    public void retain(int itemCount) {
      flushAnnotations();
      if (itemCount <= chars.length()) {
        normalizer.characters(chars.substring(0, itemCount));
        cancelCharacters(itemCount);
      } else {
        normalizer.characters(chars);
        target = new RetainPreTarget(itemCount - chars.length());
      }
    }

    @Override
    public void deleteCharacters(String chars) {
      flushAnnotations();
      if (chars.length() <= this.chars.length()) {
        cancelCharacters(chars.length());
      } else {
        target = new DeleteCharactersPreTarget(chars.substring(this.chars.length()));
      }
    }

    @Override
    public void deleteElementStart(String type, Attributes attrs) {
      throw new ComposeException("Illegal composition");
    }

    @Override
    public void deleteElementEnd() {
      throw new ComposeException("Illegal composition");
    }

    @Override
    public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
      throw new ComposeException("Illegal composition");
    }

    @Override
    public void updateAttributes(AttributesUpdate attrUpdate) {
      throw new ComposeException("Illegal composition");
    }

    private void cancelCharacters(int size) {
      if (size < chars.length()) {
        chars = chars.substring(size);
      } else {
        target = defaultTarget;
      }
    }

  }

  private final class ElementStartPostTarget extends PostTarget {

    private String type;
    private Attributes attrs;

    ElementStartPostTarget(String type, Attributes attrs) {
      this.type = type;
      this.attrs = attrs;
    }

    @Override
    public void retain(int itemCount) {
      flushAnnotations();
      normalizer.elementStart(type, attrs);
      if (itemCount > 1) {
        target = new RetainPreTarget(itemCount - 1);
      } else {
        target = defaultTarget;
      }
    }

    @Override
    public void deleteCharacters(String chars) {
      throw new ComposeException("Illegal composition");
    }

    @Override
    public void deleteElementStart(String type, Attributes attrs) {
      flushAnnotations();
      target = defaultTarget;
    }

    @Override
    public void deleteElementEnd() {
      throw new ComposeException("Illegal composition");
    }

    @Override
    public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
      flushAnnotations();
      normalizer.elementStart(type, newAttrs);
      target = defaultTarget;
    }

    @Override
    public void updateAttributes(AttributesUpdate attrUpdate) {
      flushAnnotations();
      normalizer.elementStart(type, attrs.updateWith(attrUpdate));
      target = defaultTarget;
    }

  }

  private final class ElementEndPostTarget extends PostTarget {

    @Override
    public void retain(int itemCount) {
      flushAnnotations();
      normalizer.elementEnd();
      if (itemCount > 1) {
        target = new RetainPreTarget(itemCount - 1);
      } else {
        target = defaultTarget;
      }
    }

    @Override
    public void deleteCharacters(String chars) {
      throw new ComposeException("Illegal composition");
    }

    @Override
    public void deleteElementStart(String type, Attributes attrs) {
      throw new ComposeException("Illegal composition");
    }

    @Override
    public void deleteElementEnd() {
      flushAnnotations();
      target = defaultTarget;
    }

    @Override
    public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
      throw new ComposeException("Illegal composition");
    }

    @Override
    public void updateAttributes(AttributesUpdate attrUpdate) {
      throw new ComposeException("Illegal composition");
    }

  }

  private final class ReplaceAttributesPostTarget extends PostTarget {

    Attributes oldAttrs;
    Attributes newAttrs;

    ReplaceAttributesPostTarget(Attributes oldAttrs, Attributes newAttrs) {
      this.oldAttrs = oldAttrs;
      this.newAttrs = newAttrs;
    }

    @Override
    public void retain(int itemCount) {
      flushAnnotations();
      normalizer.replaceAttributes(oldAttrs, newAttrs);
      if (itemCount > 1) {
        target = new RetainPreTarget(itemCount - 1);
      } else {
        target = defaultTarget;
      }
    }

    @Override
    public void deleteCharacters(String chars) {
      throw new ComposeException("Illegal composition");
    }

    @Override
    public void deleteElementStart(String type, Attributes attrs) {
      flushAnnotations();
      normalizer.deleteElementStart(type, oldAttrs);
      target = defaultTarget;
    }

    @Override
    public void deleteElementEnd() {
      throw new ComposeException("Illegal composition");
    }

    @Override
    public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
      flushAnnotations();
      normalizer.replaceAttributes(this.oldAttrs, newAttrs);
      target = defaultTarget;
    }

    @Override
    public void updateAttributes(AttributesUpdate attrUpdate) {
      flushAnnotations();
      normalizer.replaceAttributes(oldAttrs, newAttrs.updateWith(attrUpdate));
      target = defaultTarget;
    }

  }

  private final class UpdateAttributesPostTarget extends PostTarget {

    AttributesUpdate attrUpdate;

    UpdateAttributesPostTarget(AttributesUpdate attrUpdate) {
      this.attrUpdate = attrUpdate;
    }

    @Override
    public void retain(int itemCount) {
      flushAnnotations();
      normalizer.updateAttributes(attrUpdate);
      if (itemCount > 1) {
        target = new RetainPreTarget(itemCount - 1);
      } else {
        target = defaultTarget;
      }
    }

    @Override
    public void deleteCharacters(String chars) {
      throw new ComposeException("Illegal composition");
    }

    @Override
    public void deleteElementStart(String type, Attributes attrs) {
      flushAnnotations();
      normalizer.deleteElementStart(type, attrs.updateWith(invertUpdate(attrUpdate)));
      target = defaultTarget;
    }

    @Override
    public void deleteElementEnd() {
      throw new ComposeException("Illegal composition");
    }

    @Override
    public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
      flushAnnotations();
      normalizer.replaceAttributes(oldAttrs.updateWith(invertUpdate(attrUpdate)), newAttrs);
      target = defaultTarget;
    }

    @Override
    public void updateAttributes(AttributesUpdate attrUpdate) {
      flushAnnotations();
      normalizer.updateAttributes(this.attrUpdate.composeWith(attrUpdate));
      target = defaultTarget;
    }

  }

  private final class FinisherPreTarget extends PreTarget {

    @Override
    public void retain(int itemCount) {
      throw new ComposeException("Illegal composition");
    }

    @Override
    public void characters(String chars) {
      throw new ComposeException("Illegal composition");
    }

    @Override
    public void elementStart(String type, Attributes attrs) {
      throw new ComposeException("Illegal composition");
    }

    @Override
    public void elementEnd() {
      throw new ComposeException("Illegal composition");
    }

    @Override
    public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
      throw new ComposeException("Illegal composition");
    }

    @Override
    public void updateAttributes(AttributesUpdate attrUpdate) {
      throw new ComposeException("Illegal composition");
    }

  }

  private final class FinisherPostTarget extends PostTarget {

    @Override
    public void retain(int itemCount) {
      throw new ComposeException("Illegal composition");
    }

    @Override
    public void deleteCharacters(String chars) {
      throw new ComposeException("Illegal composition");
    }

    @Override
    public void deleteElementStart(String type, Attributes attrs) {
      throw new ComposeException("Illegal composition");
    }

    @Override
    public void deleteElementEnd() {
      throw new ComposeException("Illegal composition");
    }

    @Override
    public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
      throw new ComposeException("Illegal composition");
    }

    @Override
    public void updateAttributes(AttributesUpdate attrUpdate) {
      throw new ComposeException("Illegal composition");
    }

  }

  /**
   * The currently active annotations in the first operation. This maps keys to
   * pairs representing the old annotation value and the new annotation value.
   */
  private final Map<String, Pair<String, String>> preAnnotations =
      new HashMap<String, Pair<String, String>>();

  /**
   * The currently active annotations in the second operation. This maps keys to
   * pairs representing the old annotation value and the new annotation value.
   */
  private final Map<String, Pair<String, String>> postAnnotations =
      new HashMap<String, Pair<String, String>>();

  private final AnnotationQueue preAnnotationQueue = new AnnotationQueue() {

    @Override
    void unqueue(AnnotationBoundaryMap map) {
      // TODO: This seems pretty awkward. Perhaps we should give
      // AnnotationBoundaryMapImpl an easier builder to use.
      List<String> changeKeys = new ArrayList<String>();
      List<String> changeOldValues = new ArrayList<String>();
      List<String> changeNewValues = new ArrayList<String>();
      List<String> endKeys = new ArrayList<String>();
      for (int i = 0; i < map.changeSize(); ++i) {
        String key = map.getChangeKey(i);
        String oldValue = map.getOldValue(i);
        String newValue = map.getNewValue(i);
        Pair<String, String> postValues = postAnnotations.get(key);
        changeKeys.add(key);
        changeOldValues.add(oldValue);
        if (postValues != null) {
          changeNewValues.add(postValues.second);
        } else {
          changeNewValues.add(newValue);
        }
        preAnnotations.put(key, new Pair<String, String>(oldValue, newValue));
      }
      for (int i = 0; i < map.endSize(); ++i) {
        String key = map.getEndKey(i);
        Pair<String, String> postValues = postAnnotations.get(key);
        if (postValues != null) {
          changeKeys.add(key);
          changeOldValues.add(postValues.first);
          changeNewValues.add(postValues.second);
        } else {
          endKeys.add(key);
        }
        preAnnotations.remove(key);
      }
      normalizer.annotationBoundary(new AnnotationBoundaryMapImpl(
          endKeys.toArray(new String[0]),
          changeKeys.toArray(new String[0]),
          changeOldValues.toArray(new String[0]),
          changeNewValues.toArray(new String[0])));
    }

  };

  private final AnnotationQueue postAnnotationQueue = new AnnotationQueue() {

    @Override
    void unqueue(AnnotationBoundaryMap map) {
      // TODO: This seems pretty awkward. Perhaps we should give
      // AnnotationBoundaryMapImpl an easier builder to use.
      List<String> changeKeys = new ArrayList<String>();
      List<String> changeOldValues = new ArrayList<String>();
      List<String> changeNewValues = new ArrayList<String>();
      List<String> endKeys = new ArrayList<String>();
      for (int i = 0; i < map.changeSize(); ++i) {
        String key = map.getChangeKey(i);
        String oldValue = map.getOldValue(i);
        String newValue = map.getNewValue(i);
        Pair<String, String> preValues = preAnnotations.get(key);
        changeKeys.add(key);
        changeNewValues.add(newValue);
        if (preValues != null) {
          changeOldValues.add(preValues.first);
        } else {
          changeOldValues.add(oldValue);
        }
        postAnnotations.put(key, new Pair<String, String>(oldValue, newValue));
      }
      for (int i = 0; i < map.endSize(); ++i) {
        String key = map.getEndKey(i);
        Pair<String, String> preValues = preAnnotations.get(key);
        if (preValues != null) {
          changeKeys.add(key);
          changeOldValues.add(preValues.first);
          changeNewValues.add(preValues.second);
        } else {
          endKeys.add(key);
        }
        postAnnotations.remove(key);
      }
      normalizer.annotationBoundary(new AnnotationBoundaryMapImpl(
          endKeys.toArray(new String[0]),
          changeKeys.toArray(new String[0]),
          changeOldValues.toArray(new String[0]),
          changeNewValues.toArray(new String[0])));
    }

  };

  private final EvaluatingDocOpCursor<BufferedDocOp> normalizer =
      OperationNormalizer.createNormalizer(new BufferedDocOpImpl.DocOpBuilder());

  private final Target defaultTarget;

  private Target target;

  private Composer(boolean isClientOperation) {
    defaultTarget = isClientOperation ? new DefaultPostTarget() : new DefaultPreTarget();
  }

  private BufferedDocOp compose(BufferedDocOp op1, BufferedDocOp op2) {
    target = defaultTarget;
    int op1Index = 0;
    int op2Index = 0;
    process: {
      while (op2Index < op2.size()) {
        while (target.isPreTarget()) {
          if (op1Index >= op1.size()) {
            target = new FinisherPostTarget();
            while (op2Index < op2.size()) {
              op2.applyComponent(op2Index++, target);
            }
            break process;
          }
          op1.applyComponent(op1Index++, target);
        }
        op2.applyComponent(op2Index++, target);
      }
      while (op1Index < op1.size()) {
        if (!target.isPreTarget()) {
          target = new FinisherPreTarget();
        }
        op1.applyComponent(op1Index++, target);
      }
    }
    flushAnnotations();
    return normalizer.finish();
  }

  /**
   * Perform a composition of two client operations.
   *
   * @param op1 the first client operation
   * @param op2 the second client operation
   * @return the result of the composition
   * @throws OperationException if there was a problem composing
   */
  public static BufferedDocOp clientCompose(BufferedDocOp op1, BufferedDocOp op2)
      throws OperationException {
    try {
      return new Composer(true).compose(op1, op2);
    } catch (ComposeException e) {
      throw new OperationException(e.getMessage());
    }
  }

  /**
   * Perform a composition of two server operations.
   *
   * @param op1 the first server operation
   * @param op2 the second server operation
   * @return the result of the composition
   * @throws OperationException if there was a problem composing
   */
  public static BufferedDocOp serverCompose(BufferedDocOp op1, BufferedDocOp op2)
      throws OperationException {
    try {
      return new Composer(false).compose(op1, op2);
    } catch (ComposeException e) {
      throw new OperationException(e.getMessage());
    }
  }

  /**
   * Compose client operations.
   *
   * @param operations an iterator through the client operations to compose
   * @return the result of the composition
   */
  public static BufferedDocOp clientCompose(Iterable<BufferedDocOp> operations) {
    return composeUsing(new DocOpCollector.ClientOpCollector(), operations);
  }

  /**
   * Compose server operations.
   *
   * @param operations an iterator through the server operations to compose
   * @return the result of the composition
   */
  public static BufferedDocOp serverCompose(Iterable<BufferedDocOp> operations) {
    return composeUsing(new DocOpCollector.ServerOpCollector(), operations);
  }

  private static BufferedDocOp composeUsing(DocOpCollector collector,
      Iterable<BufferedDocOp> operations) {
    for (BufferedDocOp operation : operations) {
      collector.add(operation);
    }
    return collector.composeAll();
  }

  private void flushAnnotations() {
    preAnnotationQueue.flush();
    postAnnotationQueue.flush();
  }

  private static AttributesUpdate invertUpdate(AttributesUpdate attrUpdate) {
    AttributesUpdate inverseUpdate = new AttributesUpdateImpl();
    // TODO: This is a little silly. We should do this a better way.
    for (int i = 0; i < attrUpdate.changeSize(); ++i) {
      inverseUpdate = inverseUpdate.composeWith(new AttributesUpdateImpl(
          attrUpdate.getChangeKey(i), attrUpdate.getNewValue(i), attrUpdate.getOldValue(i)));
    }
    return inverseUpdate;
  }

}

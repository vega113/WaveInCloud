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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMap;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.AttributesUpdate;
import org.waveprotocol.wave.model.document.operation.BufferedDocOp;
import org.waveprotocol.wave.model.document.operation.EvaluatingDocOpCursor;
import org.waveprotocol.wave.model.document.operation.impl.AnnotationBoundaryMapImpl;
import org.waveprotocol.wave.model.document.operation.impl.AttributesUpdateImpl;
import org.waveprotocol.wave.model.document.operation.impl.BufferedDocOpImpl;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.OperationPair;
import org.waveprotocol.wave.model.util.Pair;

/**
 * A utility class for transforming document operations.
 *
 * TODO: Make detection of illegal transformations more thorough.
 *
 *
 */
public final class Transformer {

  private static class TransformException extends RuntimeException {

    TransformException(String message) {
      super(message);
    }

  }

  /**
   * The relative position of one cursor relative to a second cursor.
   */
  private interface RelativePosition {

    /**
     * Increase the relative position of the cursor.
     *
     * @param amount The amount by which to increase the relative position.
     */
    void increase(int amount);

    /**
     * @return The relative position.
     */
    int get();

  }

  /**
   * A cache for the effect of a component of a document mutation that affects a
   * range of the document.
   */
  private static abstract class RangeCache {

    abstract void resolveRetain(int retain);

    void resolveDeleteCharacters(String characters) {
      throw new TransformException("Incompatible operations in transformation");
    }

    void resolveDeleteElementStart(String type, Attributes attributes) {
      throw new TransformException("Incompatible operations in transformation");
    }

    void resolveDeleteElementEnd() {
      throw new TransformException("Incompatible operations in transformation");
    }

    void resolveReplaceAttributes(Attributes oldAttributes, Attributes newAttributes) {
      throw new TransformException("Incompatible operations in transformation");
    }

    void resolveUpdateAttributes(AttributesUpdate update) {
      throw new TransformException("Incompatible operations in transformation");
    }

  }

  /**
   * A resolver for mutation components which affects ranges.
   */
  private interface RangeResolver {

    /**
     * Resolves a mutation component with a cached mutation component from a
     * different document mutation.
     *
     * @param size The size of the range affected by the range modifications to
     *        resolve.
     * @param cache The cached range.
     */
    void resolve(int size, RangeCache cache);

  }

  /**
   * A tracker that tracks the positions of two cursors relative to each other.
   */
  private static final class PositionTracker {

    int position = 0;

    /**
     * @return A RelativePosition representing the client's position relative to
     *         the server's position.
     */
    RelativePosition getClientPosition() {
      return new RelativePosition() {

        public void increase(int amount) {
          position += amount;
        }

        public int get() {
          return position;
        }

      };
    }

    /**
     * @return A RelativePosition representing the server's position relative to
     *         the client's position.
     */
    RelativePosition getServerPosition() {
      return new RelativePosition() {

        public void increase(int amount) {
          position -= amount;
        }

        public int get() {
          return -position;
        }

      };
    }

  }

  /**
   * A resolver for "deleteCharacters" mutation components.
   */
  private static final class DeleteCharactersResolver implements RangeResolver {

    private String characters;

    DeleteCharactersResolver(String characters) {
      this.characters = characters;
    }

    @Override
    public void resolve(int size, RangeCache range) {
      range.resolveDeleteCharacters(characters.substring(0, size));
      characters = characters.substring(size);
    }

  }

  /**
   * A resolver for "deleteElementStart" mutation components.
   */
  private static final class DeleteElementStartResolver implements RangeResolver {

    private final String type;
    private final Attributes attributes;

    DeleteElementStartResolver(String type, Attributes attributes) {
      this.type = type;
      this.attributes = attributes;
    }

    @Override
    public void resolve(int size, RangeCache range) {
      range.resolveDeleteElementStart(type, attributes);
    }

  }

  /**
   * A resolver for "replaceAttributes" mutation components.
   */
  private static final class ReplaceAttributesResolver implements RangeResolver {

    private final Attributes oldAttributes;
    private final Attributes newAttributes;

    ReplaceAttributesResolver(Attributes oldAttributes, Attributes newAttributes) {
      this.oldAttributes = oldAttributes;
      this.newAttributes = newAttributes;
    }

    @Override
    public void resolve(int size, RangeCache range) {
      range.resolveReplaceAttributes(oldAttributes, newAttributes);
    }

  }

  /**
   * A resolver for "updateAttributes" mutation components.
   */
  private static final class UpdateAttributesResolver implements RangeResolver {

    private final AttributesUpdate update;

    UpdateAttributesResolver(AttributesUpdate update) {
      this.update = update;
    }

    @Override
    public void resolve(int size, RangeCache range) {
      range.resolveUpdateAttributes(update);
    }

  }

  /**
   * A resolver for "retain" mutation components.
   */
  private static final RangeResolver retainResolver = new RangeResolver() {

    @Override
    public void resolve(int size, RangeCache range) {
      range.resolveRetain(size);
    }

  };

  /**
   * A resolver for "deleteElementEnd" mutation components.
   */
  private static final RangeResolver deleteElementEndResolver = new RangeResolver() {

    @Override
    public void resolve(int size, RangeCache range) {
      range.resolveDeleteElementEnd();
    }

  };

  /**
   * A target of a document mutation which can be used to transform document
   * mutations by making use primarily of information from one mutation with the
   * help of auxiliary information from a second mutation. These targets should
   * be used in pairs.
   */
  private static final class Target implements EvaluatingDocOpCursor<BufferedDocOp> {

    private final class DeleteCharactersCache extends RangeCache {

      private String characters;

      DeleteCharactersCache(String characters) {
        this.characters = characters;
      }

      @Override
      void resolveRetain(int itemCount) {
        // TODO: We should fix annotations so that they work here.
        targetDocument.deleteCharacters(characters.substring(0, itemCount));
        characters = characters.substring(itemCount);
      }

      @Override
      void resolveDeleteCharacters(String characters) {
        this.characters = this.characters.substring(characters.length());
      }

    }

    private final class DeleteElementStartCache extends RangeCache {

      private final String type;
      private final Attributes attributes;

      DeleteElementStartCache(String type, Attributes attributes) {
        this.type = type;
        this.attributes = attributes;
      }

      @Override
      void resolveRetain(int itemCount) {
        // TODO: We should fix annotations so that they work here.
        targetDocument.deleteElementStart(type, attributes);
        ++depth;
      }

      @Override
      void resolveDeleteElementStart(String type, Attributes attributes) {
        ++depth;
        ++otherTarget.depth;
      }

      @Override
      void resolveReplaceAttributes(Attributes oldAttributes, Attributes newAttributes) {
        // TODO: We should fix annotations so that they work here.
        targetDocument.deleteElementStart(type, newAttributes);
        ++depth;
      }

      @Override
      void resolveUpdateAttributes(AttributesUpdate update) {
        // TODO: We should fix annotations so that they work here.
        targetDocument.deleteElementStart(type, attributes.updateWith(update));
        ++depth;
      }

    }

    private final class DeleteElementEndCache extends RangeCache {

      @Override
      void resolveRetain(int itemCount) {
        // TODO: We should fix annotations so that they work here.
        targetDocument.deleteElementEnd();
        --depth;
      }

      @Override
      void resolveDeleteElementEnd() {
        --depth;
        --otherTarget.depth;
      }

    }

    private final class ReplaceAttributesCache extends RangeCache {

      private final Attributes oldAttributes;
      private final Attributes newAttributes;

      ReplaceAttributesCache(Attributes oldAttributes, Attributes newAttributes) {
        this.oldAttributes = oldAttributes;
        this.newAttributes = newAttributes;
      }

      @Override
      void resolveRetain(int itemCount) {
        flushAnnotations();
        targetDocument.replaceAttributes(oldAttributes, newAttributes);
        otherTarget.targetDocument.retain(1);
      }

      @Override
      void resolveDeleteElementStart(String type, Attributes attributes) {
        // TODO: We should fix annotations so that they work here.
        otherTarget.targetDocument.deleteElementStart(type, newAttributes);
        ++otherTarget.depth;
      }

      @Override
      void resolveReplaceAttributes(Attributes oldAttributes, Attributes newAttributes) {
        flushAnnotations();
        targetDocument.replaceAttributes(newAttributes, this.newAttributes);
        otherTarget.targetDocument.retain(1);
      }

      @Override
      void resolveUpdateAttributes(AttributesUpdate update) {
        flushAnnotations();
        targetDocument.replaceAttributes(this.oldAttributes.updateWith(update), this.newAttributes);
        otherTarget.targetDocument.retain(1);
      }

    }

    private final class UpdateAttributesCache extends RangeCache {

      private final AttributesUpdate update;

      UpdateAttributesCache(AttributesUpdate update) {
        this.update = update;
      }

      @Override
      void resolveRetain(int itemCount) {
        flushAnnotations();
        targetDocument.updateAttributes(update);
        otherTarget.targetDocument.retain(1);
      }

      @Override
      void resolveDeleteElementStart(String type, Attributes attributes) {
        // TODO: We should fix annotations so that they work here.
        otherTarget.targetDocument.deleteElementStart(type, attributes.updateWith(update));
        ++otherTarget.depth;
      }

      @Override
      void resolveReplaceAttributes(Attributes oldAttributes, Attributes newAttributes) {
        flushAnnotations();
        targetDocument.retain(1);
        otherTarget.targetDocument.replaceAttributes(oldAttributes.updateWith(update),
            newAttributes);
      }

      @Override
      void resolveUpdateAttributes(AttributesUpdate update) {
        flushAnnotations();
        Map<String, String> updated = new HashMap<String, String>();
        for (int i = 0; i < update.changeSize(); ++i) {
          updated.put(update.getChangeKey(i), update.getNewValue(i));
        }
        AttributesUpdate newUpdate = new AttributesUpdateImpl();
        // TODO: This is a little silly. We should do this a better way.
        for (int i = 0; i < this.update.changeSize(); ++i) {
          String key = this.update.getChangeKey(i);
          String newOldValue = updated.containsKey(key) ? updated.get(key)
              : this.update.getOldValue(i);
          newUpdate = newUpdate.composeWith(new AttributesUpdateImpl(key,
              newOldValue, this.update.getNewValue(i)));
        }
        targetDocument.updateAttributes(newUpdate);
        Set<String> keySet = new HashSet<String>();
        for (int i = 0; i < this.update.changeSize(); ++i) {
          keySet.add(this.update.getChangeKey(i));
        }
        AttributesUpdate transformedAttributes = update.exclude(keySet);
        otherTarget.targetDocument.updateAttributes(transformedAttributes);
      }

    }

    private final RangeCache retainCache = new RangeCache() {

      @Override
      void resolveRetain(int itemCount) {
        flushAnnotations();
        targetDocument.retain(itemCount);
        otherTarget.targetDocument.retain(itemCount);
      }

      @Override
      void resolveDeleteCharacters(String characters) {
        // TODO: We should fix annotations so that they work here.
        otherTarget.targetDocument.deleteCharacters(characters);
      }

      @Override
      void resolveDeleteElementStart(String type, Attributes attributes) {
        // TODO: We should fix annotations so that they work here.
        otherTarget.targetDocument.deleteElementStart(type, attributes);
        ++otherTarget.depth;
      }

      @Override
      void resolveDeleteElementEnd() {
        // TODO: We should fix annotations so that they work here.
        otherTarget.targetDocument.deleteElementEnd();
        --otherTarget.depth;
      }

      @Override
      void resolveReplaceAttributes(Attributes oldAttributes, Attributes newAttributes) {
        flushAnnotations();
        targetDocument.retain(1);
        otherTarget.targetDocument.replaceAttributes(oldAttributes, newAttributes);
      }

      @Override
      void resolveUpdateAttributes(AttributesUpdate update) {
        flushAnnotations();
        targetDocument.retain(1);
        otherTarget.targetDocument.updateAttributes(update);
      }

    };


    /**
     * The target to which to write the transformed mutation.
     */
    private final EvaluatingDocOpCursor<BufferedDocOp> targetDocument;

    /**
     * The position of the processing cursor associated with this target
     * relative to the position of the processing cursor associated to the
     * opposing target. All positional calculations are based on cursor
     * positions in the original document on which the two original operations
     * apply.
     */
    private final RelativePosition relativePosition;

    /**
     * An annotation queue that tracks annotation modifications at the current
     * cursor position.
     */
    private final AnnotationQueue annotationQueue;

    /**
     * The target that is used opposite this target in the transformation.
     */
    private Target otherTarget;

    /**
     * A cache for the effect of mutation components which affect ranges.
     */
    private RangeCache rangeCache = retainCache;

    /**
     * The current depth of element deletions.
     */
    private int depth = 0;

    Target(EvaluatingDocOpCursor<BufferedDocOp> targetDocument, RelativePosition relativePosition,
        AnnotationQueue annotationQueue) {
      this.targetDocument = targetDocument;
      this.relativePosition = relativePosition;
      this.annotationQueue = annotationQueue;
    }

    // TODO: See if we can remove this explicit method and find a
    // better way to do this using a constructor or factory.
    public void setOtherTarget(Target otherTarget) {
      this.otherTarget = otherTarget;
    }

    public BufferedDocOp finish() {
      annotationQueue.flush();
      return targetDocument.finish();
    }

    @Override
    public void retain(int itemCount) {
      resolveRange(itemCount, retainResolver);
      rangeCache = retainCache;
    }

    @Override
    public void characters(String chars) {
      annotationQueue.flush();
      if (otherTarget.depth > 0) {
        otherTarget.targetDocument.deleteCharacters(chars);
      } else {
        targetDocument.characters(chars);
        otherTarget.targetDocument.retain(chars.length());
      }
    }

    @Override
    public void elementStart(String tag, Attributes attrs) {
      annotationQueue.flush();
      if (otherTarget.depth > 0) {
        otherTarget.targetDocument.deleteElementStart(tag, attrs);
      } else {
        targetDocument.elementStart(tag, attrs);
        otherTarget.targetDocument.retain(1);
      }
    }

    @Override
    public void elementEnd() {
      annotationQueue.flush();
      if (otherTarget.depth > 0) {
        otherTarget.targetDocument.deleteElementEnd();
      } else {
        targetDocument.elementEnd();
        otherTarget.targetDocument.retain(1);
      }
    }

    @Override
    public void deleteCharacters(String chars) {
      int resolutionSize = resolveRange(chars.length(), new DeleteCharactersResolver(chars));
      if (resolutionSize >= 0) {
        rangeCache = new DeleteCharactersCache(chars.substring(resolutionSize));
      }
    }

    @Override
    public void deleteElementStart(String tag, Attributes attrs) {
      if (resolveRange(1, new DeleteElementStartResolver(tag, attrs)) == 0) {
        rangeCache = new DeleteElementStartCache(tag, attrs);
      }
    }

    @Override
    public void deleteElementEnd() {
      if (resolveRange(1, deleteElementEndResolver) == 0) {
        rangeCache = new DeleteElementEndCache();
      }
    }

    @Override
    public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
      if (resolveRange(1, new ReplaceAttributesResolver(oldAttrs, newAttrs)) == 0) {
        rangeCache = new ReplaceAttributesCache(oldAttrs, newAttrs);
      }
    }

    @Override
    public void updateAttributes(AttributesUpdate attrUpdate) {
      if (resolveRange(1, new UpdateAttributesResolver(attrUpdate)) == 0) {
        rangeCache = new UpdateAttributesCache(attrUpdate);
      }
    }

    @Override
    public void annotationBoundary(AnnotationBoundaryMap map) {
      annotationQueue.queue(map);
    }

    /**
     * Resolves the transformation of a range.
     *
     * @param size the requested size to resolve
     * @param resolver the resolver to use
     * @return the portion of the requested size that was resolved, or -1 to
     *         indicate that the entire range was resolved
     */
    private int resolveRange(int size, RangeResolver resolver) {
      int oldPosition = relativePosition.get();
      relativePosition.increase(size);
      if (relativePosition.get() > 0) {
        if (oldPosition < 0) {
          resolver.resolve(-oldPosition, otherTarget.rangeCache);
        }
        return -oldPosition;
      } else {
        resolver.resolve(size, otherTarget.rangeCache);
        return -1;
      }
    }

    private void flushAnnotations() {
      annotationQueue.flush();
      otherTarget.annotationQueue.flush();
    }

  }

  private final EvaluatingDocOpCursor<BufferedDocOp> clientOperation =
      OperationNormalizer.createNormalizer(new BufferedDocOpImpl.DocOpBuilder());
  private final EvaluatingDocOpCursor<BufferedDocOp> serverOperation =
      OperationNormalizer.createNormalizer(new BufferedDocOpImpl.DocOpBuilder());

  /**
   * The currently active client annotations. This maps keys to pairs
   * representing the old annotation value and the new annotation value.
   */
  private final Map<String, Pair<String, String>> clientAnnotations =
      new HashMap<String, Pair<String, String>>();

  /**
   * The currently active server annotations. This maps keys to pairs
   * representing the old annotation value and the new annotation value.
   */
  private final Map<String, Pair<String, String>> serverAnnotations =
      new HashMap<String, Pair<String, String>>();

  private final AnnotationQueue clientAnnotationQueue = new AnnotationQueue() {

    @Override
    void unqueue(AnnotationBoundaryMap map) {
      // TODO: This seems pretty awkward. Perhaps we should give
      // AnnotationBoundaryMapImpl an easier builder to use.
      List<String> clientChangeKeys = new ArrayList<String>();
      List<String> clientChangeOldValues = new ArrayList<String>();
      List<String> clientChangeNewValues = new ArrayList<String>();
      List<String> clientEndKeys = new ArrayList<String>();
      List<String> serverChangeKeys = new ArrayList<String>();
      List<String> serverChangeOldValues = new ArrayList<String>();
      List<String> serverChangeNewValues = new ArrayList<String>();
      List<String> serverEndKeys = new ArrayList<String>();
      for (int i = 0; i < map.changeSize(); ++i) {
        String key = map.getChangeKey(i);
        String oldValue = map.getOldValue(i);
        String newValue = map.getNewValue(i);
        Pair<String, String> serverValues = serverAnnotations.get(key);
        clientChangeKeys.add(key);
        clientChangeNewValues.add(newValue);
        if (serverValues != null) {
          clientChangeOldValues.add(serverValues.second);
          serverEndKeys.add(key);
        } else {
          clientChangeOldValues.add(oldValue);
        }
        clientAnnotations.put(key, new Pair<String, String>(oldValue, newValue));
      }
      for (int i = 0; i < map.endSize(); ++i) {
        String key = map.getEndKey(i);
        Pair<String, String> serverValues = serverAnnotations.get(key);
        clientEndKeys.add(key);
        if (serverValues != null) {
          serverChangeKeys.add(key);
          serverChangeOldValues.add(serverValues.first);
          serverChangeNewValues.add(serverValues.second);
        }
        clientAnnotations.remove(key);
      }
      clientOperation.annotationBoundary(new AnnotationBoundaryMapImpl(
          clientEndKeys.toArray(new String[0]),
          clientChangeKeys.toArray(new String[0]),
          clientChangeOldValues.toArray(new String[0]),
          clientChangeNewValues.toArray(new String[0])));
      serverOperation.annotationBoundary(new AnnotationBoundaryMapImpl(
          serverEndKeys.toArray(new String[0]),
          serverChangeKeys.toArray(new String[0]),
          serverChangeOldValues.toArray(new String[0]),
          serverChangeNewValues.toArray(new String[0])));
    }

  };

  private final AnnotationQueue serverAnnotationQueue = new AnnotationQueue() {

    @Override
    void unqueue(AnnotationBoundaryMap map) {
      // TODO: This seems pretty awkward. Perhaps we should give
      // AnnotationBoundaryMapImpl an easier builder to use.
      List<String> serverChangeKeys = new ArrayList<String>();
      List<String> serverChangeOldValues = new ArrayList<String>();
      List<String> serverChangeNewValues = new ArrayList<String>();
      List<String> serverEndKeys = new ArrayList<String>();
      List<String> clientChangeKeys = new ArrayList<String>();
      List<String> clientChangeOldValues = new ArrayList<String>();
      List<String> clientChangeNewValues = new ArrayList<String>();
      List<String> clientEndKeys = new ArrayList<String>();
      for (int i = 0; i < map.changeSize(); ++i) {
        String key = map.getChangeKey(i);
        String oldValue = map.getOldValue(i);
        String newValue = map.getNewValue(i);
        Pair<String, String> clientValues = clientAnnotations.get(key);
        if (clientValues != null) {
          clientChangeKeys.add(key);
          clientChangeOldValues.add(newValue);
          clientChangeNewValues.add(clientValues.second);
        } else {
          serverChangeKeys.add(key);
          serverChangeOldValues.add(oldValue);
          serverChangeNewValues.add(newValue);
        }
        serverAnnotations.put(key, new Pair<String, String>(oldValue, newValue));
      }
      for (int i = 0; i < map.endSize(); ++i) {
        String key = map.getEndKey(i);
        Pair<String, String> clientValues = clientAnnotations.get(key);
        if (clientValues != null) {
          clientChangeKeys.add(key);
          clientChangeOldValues.add(clientValues.first);
          clientChangeNewValues.add(clientValues.second);
        } else {
          serverEndKeys.add(key);
        }
        serverAnnotations.remove(key);
      }
      serverOperation.annotationBoundary(new AnnotationBoundaryMapImpl(
          serverEndKeys.toArray(new String[0]),
          serverChangeKeys.toArray(new String[0]),
          serverChangeOldValues.toArray(new String[0]),
          serverChangeNewValues.toArray(new String[0])));
      clientOperation.annotationBoundary(new AnnotationBoundaryMapImpl(
          clientEndKeys.toArray(new String[0]),
          clientChangeKeys.toArray(new String[0]),
          clientChangeOldValues.toArray(new String[0]),
          clientChangeNewValues.toArray(new String[0])));
    }

  };

  /**
   * Transform a pair of operations.
   *
   * @param clientOp The operation from the client.
   * @param serverOp The operation from the server.
   * @return The transformed pair of operations.
   * @throws OperationException if a problem was encountered during the
   *         transformation process.
   */
  public OperationPair<BufferedDocOp> transformOperations(BufferedDocOp clientOp,
      BufferedDocOp serverOp) throws OperationException {
    try {
      PositionTracker positionTracker = new PositionTracker();

      RelativePosition clientPosition = positionTracker.getClientPosition();
      RelativePosition serverPosition = positionTracker.getServerPosition();

      // The target responsible for processing components of the client operation.
      Target clientTarget = new Target(clientOperation, clientPosition, clientAnnotationQueue);

      // The target responsible for processing components of the server operation.
      Target serverTarget = new Target(serverOperation, serverPosition, serverAnnotationQueue);

      clientTarget.setOtherTarget(serverTarget);
      serverTarget.setOtherTarget(clientTarget);


      // Incrementally apply the two operations in a linearly-ordered interleaving
      // fashion.
      int clientIndex = 0;
      int serverIndex = 0;
      while (clientIndex < clientOp.size()) {
        clientOp.applyComponent(clientIndex++, clientTarget);
        while (clientPosition.get() > 0) {
          serverOp.applyComponent(serverIndex++, serverTarget);
        }
      }
      while (serverIndex < serverOp.size()) {
        serverOp.applyComponent(serverIndex++, serverTarget);
      }
      clientOp = clientTarget.finish();
      serverOp = serverTarget.finish();
    } catch (TransformException e) {
      throw new OperationException(e.getMessage());
    }
    return new OperationPair<BufferedDocOp>(clientOp, serverOp);
  }

  /**
   * Transform a pair of operations.
   *
   * @param clientOp The operation from the client.
   * @param serverOp The operation from the server.
   * @return The transformed pair of operations.
   * @throws OperationException if a problem was encountered during the
   *         transformation process.
   */
  public static OperationPair<BufferedDocOp> transform(BufferedDocOp clientOp,
      BufferedDocOp serverOp) throws OperationException {
    return new Transformer().transformOperations(clientOp, serverOp);
  }

}

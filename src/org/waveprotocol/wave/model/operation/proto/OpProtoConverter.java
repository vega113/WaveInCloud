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
 */

package org.waveprotocol.wave.model.operation.proto;

import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMap;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.AttributesUpdate;
import org.waveprotocol.wave.model.document.operation.BufferedDocOp;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.DocOpCursor;
import org.waveprotocol.wave.model.document.operation.automaton.DocOpAutomaton.ViolationCollector;
import org.waveprotocol.wave.model.document.operation.impl.AnnotationBoundaryMapImpl;
import org.waveprotocol.wave.model.document.operation.impl.AttributesImpl;
import org.waveprotocol.wave.model.document.operation.impl.AttributesUpdateImpl;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuffer;
import org.waveprotocol.wave.model.document.operation.impl.DocOpValidator;
import org.waveprotocol.wave.model.document.operation.util.ImmutableStateMap.Attribute;
import org.waveprotocol.wave.model.document.operation.util.ImmutableUpdateMap.AttributeUpdate;
import org.waveprotocol.wave.model.operation.proto.OpProto.DocumentOperation;
import org.waveprotocol.wave.model.operation.wave.AddParticipant;
import org.waveprotocol.wave.model.operation.wave.NoOp;
import org.waveprotocol.wave.model.operation.wave.RemoveParticipant;
import org.waveprotocol.wave.model.operation.wave.WaveletDocumentOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility class for converting between operation objects and their
 * protocol buffer representation.
 */
public class OpProtoConverter {
  private OpProtoConverter() {}

  public static class InvalidInputException extends Exception {
    public InvalidInputException(String message) {
      super(message);
    }

    public InvalidInputException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  public static OpProto.WaveletOperation serialize(WaveletOperation waveletOp) {
    if (waveletOp instanceof NoOp) {
      return OpProto.WaveletOperation.newBuilder().setNoOp(true).build();
    } else if (waveletOp instanceof AddParticipant) {
      return OpProto.WaveletOperation.newBuilder()
          .setAddParticipant(((AddParticipant) waveletOp).getParticipantId().getAddress()).build();
    } else if (waveletOp instanceof RemoveParticipant) {
      return OpProto.WaveletOperation.newBuilder()
          .setRemoveParticipant(((RemoveParticipant) waveletOp).getParticipantId().getAddress())
          .build();
    } else if (waveletOp instanceof WaveletDocumentOperation) {
      return OpProto.WaveletOperation.newBuilder().setMutateDocument(
          OpProto.WaveletOperation.MutateDocument.newBuilder()
          .setDocumentId(((WaveletDocumentOperation) waveletOp).getDocumentId())
          .setDocumentOperation(
              serialize(((WaveletDocumentOperation) waveletOp).getOperation())))
          .build();
    } else {
      throw new AssertionError("Unexpected operation type: " + waveletOp);
    }
  }

  public static DocumentOperation serialize(DocOp input) {
    assert DocOpValidator.isWellFormed(null, input);

    final DocumentOperation.Builder output = DocumentOperation.newBuilder();

    input.apply(new DocOpCursor() {
      private DocumentOperation.Component.Builder newComponent() {
        return DocumentOperation.Component.newBuilder();
      }

      private DocumentOperation.KeyValuePair newPair(Map.Entry<String, String> e) {
        assert e.getKey() != null;
        assert e.getValue() != null;
        return DocumentOperation.KeyValuePair.newBuilder()
            .setKey(e.getKey())
            .setValue(e.getValue())
            .build();
      }

      private DocumentOperation.KeyValueUpdate newUpdate(
          String key, String oldValue, String newValue) {
        DocumentOperation.KeyValueUpdate.Builder b =
          DocumentOperation.KeyValueUpdate.newBuilder();
        assert key != null;
        b.setKey(key);
        if (oldValue != null) {
          b.setOldValue(oldValue);
        }
        if (newValue != null) {
          b.setNewValue(newValue);
        }
        return b.build();
      }

      private DocumentOperation.ElementStart newElementStart(String type,
          Attributes attributes) {
        DocumentOperation.ElementStart.Builder s = DocumentOperation.ElementStart.newBuilder();
        s.setType(type);
        for (Map.Entry<String, String> e : attributes.entrySet()) {
          s.addAttribute(newPair(e));
        }
        return s.build();
      }

      @Override public void annotationBoundary(AnnotationBoundaryMap map) {
        DocumentOperation.AnnotationBoundary.Builder b =
          DocumentOperation.AnnotationBoundary.newBuilder();
        int m = map.endSize();
        int n = map.changeSize();
        if (m == 0 && n == 0) {
          b.setEmpty(true);
        } else {
          for (int i = 0; i < m; i++) {
            b.addEnd(map.getEndKey(i));
          }
          for (int i = 0; i < n; i++) {
            b.addChange(newUpdate(map.getChangeKey(i), map.getOldValue(i), map.getNewValue(i)));
          }
        }
        output.addComponent(newComponent().setAnnotationBoundary(b));
      }

      @Override public void characters(String characters) {
        output.addComponent(newComponent().setCharacters(characters));
      }

      @Override public void elementStart(String type, Attributes attributes) {
        output.addComponent(newComponent()
            .setElementStart(newElementStart(type, attributes)));
      }

      @Override public void elementEnd() {
        output.addComponent(newComponent().setElementEnd(true));
      }

      @Override public void retain(int itemCount) {
        output.addComponent(newComponent().setRetainItemCount(itemCount));
      }

      @Override public void deleteCharacters(String characters) {
        output.addComponent(newComponent().setDeleteCharacters(characters));
      }

      @Override public void deleteElementStart(String type, Attributes attributes) {
        output.addComponent(newComponent()
            .setDeleteElementStart(newElementStart(type, attributes)));
      }

      @Override public void deleteElementEnd() {
        output.addComponent(newComponent().setDeleteElementEnd(true));
      }

      @Override public void replaceAttributes(Attributes oldAttributes,
          Attributes newAttributes) {
        DocumentOperation.ReplaceAttributes.Builder r =
            DocumentOperation.ReplaceAttributes.newBuilder();
        if (oldAttributes.isEmpty() && newAttributes.isEmpty()) {
          r.setEmpty(true);
        } else {
          for (Map.Entry<String, String> e : oldAttributes.entrySet()) {
            r.addOldAttribute(newPair(e));
          }
          for (Map.Entry<String, String> e : newAttributes.entrySet()) {
            r.addNewAttribute(newPair(e));
          }
        }
        output.addComponent(newComponent().setReplaceAttributes(r));
      }

      @Override public void updateAttributes(AttributesUpdate attributes) {
        DocumentOperation.UpdateAttributes.Builder u =
          DocumentOperation.UpdateAttributes.newBuilder();
        int n = attributes.changeSize();
        if (n == 0) {
          u.setEmpty(true);
        } else {
          for (int i = 0; i < n; i++) {
            u.addAttributeUpdate(newUpdate(attributes.getChangeKey(i),
                attributes.getOldValue(i), attributes.getNewValue(i)));
          }
        }
        output.addComponent(newComponent().setUpdateAttributes(u));
      }
    });

    return output.build();
  }


  private static boolean exactlyOneTrue(boolean... booleans) {
    boolean foundOneTrue = false;
    for (boolean b : booleans) {
      if (b) {
        if (foundOneTrue) {
          return false;
        } else {
          foundOneTrue = true;
        }
      }
    }
    return foundOneTrue;
  }

  public static WaveletOperation deserialize(OpProto.WaveletOperation input)
      throws InvalidInputException {
    if (!input.isInitialized()) {
      throw new InvalidInputException("Attempt to deserialize protobuf with missing fields: "
          + input);
    }

    if (!exactlyOneTrue(input.hasNoOp(),
        input.hasAddParticipant(),
        input.hasRemoveParticipant(),
        input.hasMutateDocument())) {
      throw new InvalidInputException("Exactly one wavelet operation type must be set: "
          + input);
    }
    if (input.hasNoOp()) {
      if (!input.getNoOp()) {
        throw new InvalidInputException("Ill-formed no-op: " + input);
      }
      return new NoOp();
    } else if (input.hasAddParticipant()) {
      return new AddParticipant(new ParticipantId(input.getAddParticipant()));
    } else if (input.hasRemoveParticipant()) {
      return new RemoveParticipant(new ParticipantId(input.getRemoveParticipant()));
    } else if (input.hasMutateDocument()) {
      return new WaveletDocumentOperation(
          input.getMutateDocument().getDocumentId(),
          deserialize(input.getMutateDocument().getDocumentOperation()));
    } else {
      throw new AssertionError("Fell through in deserialize (WaveletOperation)");
    }
  }

  private static Attribute deserializePair(OpProto.DocumentOperation.KeyValuePair pair) {
    assert pair.hasKey(); // required
    assert pair.hasValue(); // required
    return new Attribute(pair.getKey(), pair.getValue());
  }

  private static List<Attribute> deserializeAttributes(
      List<OpProto.DocumentOperation.KeyValuePair> input) {
    List<Attribute> attributes = new ArrayList<Attribute>(input.size());
    for (OpProto.DocumentOperation.KeyValuePair pair : input) {
      attributes.add(deserializePair(pair));
    }
    return attributes;
  }

  private static AttributeUpdate deserializeUpdate(
      OpProto.DocumentOperation.KeyValueUpdate input) {
    assert input.hasKey(); // required
    return new AttributeUpdate(input.getKey(),
        input.hasOldValue() ? input.getOldValue() : null,
        input.hasNewValue() ? input.getNewValue() : null);
  }

  private static List<AttributeUpdate> deserializeAttributeUpdates(
      List<OpProto.DocumentOperation.KeyValueUpdate> input) {
    List<AttributeUpdate> updates = new ArrayList<AttributeUpdate>(input.size());
    for (OpProto.DocumentOperation.KeyValueUpdate update : input) {
      updates.add(deserializeUpdate(update));
    }
    return updates;
  }

  public static BufferedDocOp deserialize(DocumentOperation input)
      throws InvalidInputException {
    if (!input.isInitialized()) {
      throw new InvalidInputException("Attempt to deserialize protobuf with missing fields: "
          + input);
    }

    DocOpBuffer output = new DocOpBuffer();

    for (OpProto.DocumentOperation.Component c : input.getComponentList()) {
      if (!exactlyOneTrue(c.hasAnnotationBoundary(),
          c.hasCharacters(),
          c.hasElementStart(),
          c.hasElementEnd(),
          c.hasRetainItemCount(),
          c.hasDeleteCharacters(),
          c.hasDeleteElementStart(),
          c.hasDeleteElementEnd(),
          c.hasReplaceAttributes(),
          c.hasUpdateAttributes())) {
        throw new InvalidInputException(
            "Exactly one document operation component type must be set: " + c);
      }
      if (c.hasAnnotationBoundary()) {
        OpProto.DocumentOperation.AnnotationBoundary b = c.getAnnotationBoundary();
        if (b.hasEmpty()) {
          if (!b.getEmpty() || b.getEndCount() != 0 || b.getChangeCount() != 0) {
            throw new InvalidInputException("Ill-formed empty annotation boundary: " + c);
          }
          output.annotationBoundary(AnnotationBoundaryMapImpl.EMPTY_MAP);
        } else {
          String[] ends = new String[b.getEndCount()];
          String[] changeKeys = new String[b.getChangeCount()];
          String[] oldValues = new String[b.getChangeCount()];
          String[] newValues = new String[b.getChangeCount()];
          for (int i = 0; i < b.getEndCount(); i++) {
            ends[i] = b.getEnd(i);
          }
          for (int i = 0; i < b.getChangeCount(); i++) {
            OpProto.DocumentOperation.KeyValueUpdate u = b.getChange(i);
            assert u.hasKey(); // required
            changeKeys[i] = u.getKey();
            oldValues[i] = u.hasOldValue() ? u.getOldValue() : null;
            newValues[i] = u.hasNewValue() ? u.getNewValue() : null;
          }
          AnnotationBoundaryMapImpl.Builder map = AnnotationBoundaryMapImpl.builder();
          try {
            map.initializationEnd(ends);
            map.updateValues(changeKeys, oldValues, newValues);
            output.annotationBoundary(map.build());
          } catch (IllegalArgumentException e) {
            throw new InvalidInputException("Invalid annotation boundary: " + b + ": " + e, e);
          }
        }
      } else if (c.hasCharacters()) {
        output.characters(c.getCharacters());
      } else if (c.hasElementStart()) {
        OpProto.DocumentOperation.ElementStart s = c.getElementStart();
        assert s.hasType(); // required
        // No need to check sort order here, the DocOpValidator call below will check it.
        output.elementStart(s.getType(), AttributesImpl.fromSortedAttributesUnchecked(
            deserializeAttributes(s.getAttributeList())));
      } else if (c.hasElementEnd()) {
        if (!c.getElementEnd()) {
          throw new InvalidInputException("Element end present but false: " + c);
        }
        output.elementEnd();
      } else if (c.hasRetainItemCount()) {
        output.retain(c.getRetainItemCount());
      } else if (c.hasDeleteCharacters()) {
        output.deleteCharacters(c.getDeleteCharacters());
      } else if (c.hasDeleteElementStart()) {
        OpProto.DocumentOperation.ElementStart s = c.getDeleteElementStart();
        assert s.hasType(); // required
        // No need to check sort order here, the DocOpValidator call below will check it.
        output.deleteElementStart(s.getType(), AttributesImpl.fromSortedAttributesUnchecked(
            deserializeAttributes(s.getAttributeList())));
      } else if (c.hasDeleteElementEnd()) {
        if (!c.getDeleteElementEnd()) {
          throw new InvalidInputException("Delete element end present but false: " + c);
        }
        output.deleteElementEnd();
      } else if (c.hasUpdateAttributes()) {
        OpProto.DocumentOperation.UpdateAttributes u = c.getUpdateAttributes();
        if (u.hasEmpty()) {
          if (!u.getEmpty() || u.getAttributeUpdateCount() != 0) {
            throw new InvalidInputException("Ill-formed empty UpdateAttributes: " + c);
          }
          output.updateAttributes(AttributesUpdateImpl.EMPTY_MAP);
        } else {
          // No need to check sort order here, the DocOpValidator call below will check it.
          output.updateAttributes(AttributesUpdateImpl.fromSortedUpdatesUnchecked(
              deserializeAttributeUpdates(c.getUpdateAttributes().getAttributeUpdateList())));
        }
      } else if (c.hasReplaceAttributes()) {
        OpProto.DocumentOperation.ReplaceAttributes r = c.getReplaceAttributes();
        if (r.hasEmpty()) {
          if (!r.getEmpty() || r.getOldAttributeCount() != 0 || r.getNewAttributeCount() != 0) {
            throw new InvalidInputException("Ill-formed empty ReplaceAttributes: " + c);
          }
          output.replaceAttributes(Attributes.EMPTY_MAP, Attributes.EMPTY_MAP);
        } else {
          // No need to check sort order here, the DocOpValidator call below will check it.
          output.replaceAttributes(
              AttributesImpl.fromSortedAttributesUnchecked(
                  deserializeAttributes(r.getOldAttributeList())),
              AttributesImpl.fromSortedAttributesUnchecked(
                  deserializeAttributes(r.getNewAttributeList())));
        }
      } else {
        throw new AssertionError("Fell through in deserialize (DocumentOperation)");
      }
    }

    BufferedDocOp op = output.finishUnchecked();
    if (!DocOpValidator.isWellFormed(null, op)) {
      // Validate again, this time collecting violations to provide as diagnostics.
      ViolationCollector v = new ViolationCollector();
      DocOpValidator.isWellFormed(v, op);
      throw new InvalidInputException("Ill-formed document operation: " + v + ":\n"
          + input + "\n" + op);
    }
    return op;
  }
}

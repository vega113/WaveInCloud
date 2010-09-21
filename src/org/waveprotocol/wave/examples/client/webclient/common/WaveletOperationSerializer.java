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

package org.waveprotocol.wave.examples.client.webclient.common;


import org.waveprotocol.wave.concurrencycontrol.common.Delta;
import org.waveprotocol.wave.examples.common.HashedVersion;
import org.waveprotocol.wave.federation.ProtocolDocumentOperation;
import org.waveprotocol.wave.federation.ProtocolWaveletDelta;
import org.waveprotocol.wave.federation.ProtocolWaveletOperation;
import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMap;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.AttributesUpdate;
import org.waveprotocol.wave.model.document.operation.BufferedDocOp;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.DocOpCursor;
import org.waveprotocol.wave.model.document.operation.impl.AnnotationBoundaryMapImpl;
import org.waveprotocol.wave.model.document.operation.impl.AttributesImpl;
import org.waveprotocol.wave.model.document.operation.impl.AttributesUpdateImpl;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuilder;
import org.waveprotocol.wave.model.operation.wave.AddParticipant;
import org.waveprotocol.wave.model.operation.wave.BlipContentOperation;
import org.waveprotocol.wave.model.operation.wave.BlipOperation;
import org.waveprotocol.wave.model.operation.wave.BlipOperationVisitor;
import org.waveprotocol.wave.model.operation.wave.NoOp;
import org.waveprotocol.wave.model.operation.wave.RemoveParticipant;
import org.waveprotocol.wave.model.operation.wave.SubmitBlip;
import org.waveprotocol.wave.model.operation.wave.WaveletBlipOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperationContext;
import org.waveprotocol.wave.model.version.DistinctVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for serializing/deserializing wavelet operations (and their components) to/from
 * their protocol buffer representations (and their components).
 */
public class WaveletOperationSerializer {
  private WaveletOperationSerializer() {
  }

  /**
   * Serialize a {@link Delta} as a {@link ProtocolWaveletDelta} at a specific version.
   *
   * @param waveletDelta to serialize
   * @param version version at which the delta applies
   * @return serialized protocol buffer wavelet delta
   */
  public static ProtocolWaveletDelta serialize(Delta waveletDelta, HashedVersion version) {
    ProtocolWaveletDelta.Builder protobufDelta = ProtocolWaveletDelta.newBuilder();

    for (WaveletOperation waveletOp : waveletDelta) {
      protobufDelta.addOperation(serialize(waveletOp));
    }

    protobufDelta.setAuthor(waveletDelta.get(0).getContext().getCreator().getAddress());
    protobufDelta.setHashedVersion(CoreWaveletOperationSerializer.serialize(version));
    return protobufDelta.build();
  }

  /**
   * Serialize a {@link WaveletOperation} as a {@link ProtocolWaveletOperation}.
   *
   * @param waveletOp wavelet operation to serialize
   * @return serialized protocol buffer wavelet operation
   */
  public static ProtocolWaveletOperation serialize(WaveletOperation waveletOp) {
    ProtocolWaveletOperation.Builder protobufOp = ProtocolWaveletOperation.newBuilder();

    if (waveletOp instanceof NoOp) {
      protobufOp.setNoOp(true);
    } else if (waveletOp instanceof AddParticipant) {
      protobufOp.setAddParticipant(
          ((AddParticipant) waveletOp).getParticipantId().getAddress());
    } else if (waveletOp instanceof RemoveParticipant) {
      protobufOp.setRemoveParticipant(
          ((RemoveParticipant) waveletOp).getParticipantId().getAddress());
    } else if (waveletOp instanceof WaveletBlipOperation) {
      ProtocolWaveletOperation.MutateDocument.Builder mutation =
          ProtocolWaveletOperation.MutateDocument.newBuilder();
      mutation.setDocumentId(((WaveletBlipOperation) waveletOp).getBlipId());
      mutation.setDocumentOperation(
          serialize(((WaveletBlipOperation) waveletOp).getBlipOp()));
      protobufOp.setMutateDocument(mutation.build());
    } else {
      throw new IllegalArgumentException("Unsupported operation type: " + waveletOp);
    }

    return protobufOp.build();
  }


  /**
   * Serialize a {@link DocOp} as a {@link ProtocolDocumentOperation}.
   *
   * @param inputOp document operation to serialize
   * @return serialized protocol buffer document operation
   */
  public static ProtocolDocumentOperation serialize(BlipOperation inputOp) {
    final ProtocolDocumentOperation.Builder output = ProtocolDocumentOperation.newBuilder();

    inputOp.acceptVisitor(new BlipOperationVisitor() {
      @Override
      public void visitBlipContentOperation(BlipContentOperation op) {
        BufferedDocOp mutationOp = op.getContentOp();
        mutationOp.apply(new DocOpCursor() {
          private ProtocolDocumentOperation.Component.Builder newComponentBuilder() {
            return ProtocolDocumentOperation.Component.newBuilder();
          }

          @Override
          public void retain(int itemCount) {
            output.addComponent(newComponentBuilder().setRetainItemCount(itemCount));
          }

          @Override
          public void characters(String characters) {
            output.addComponent(newComponentBuilder().setCharacters(characters));
          }

          @Override
          public void deleteCharacters(String characters) {
            output.addComponent(newComponentBuilder().setDeleteCharacters(characters));
          }

          @Override
          public void elementStart(String type, Attributes attributes) {
            ProtocolDocumentOperation.Component.ElementStart e = makeElementStart(type, attributes);
            output.addComponent(newComponentBuilder().setElementStart(e));
          }

          @Override
          public void deleteElementStart(String type, Attributes attributes) {
            ProtocolDocumentOperation.Component.ElementStart e = makeElementStart(type, attributes);
            output.addComponent(newComponentBuilder().setDeleteElementStart(e));
          }

          private ProtocolDocumentOperation.Component.ElementStart makeElementStart(
              String type, Attributes attributes) {
            ProtocolDocumentOperation.Component.ElementStart.Builder e =
                ProtocolDocumentOperation.Component.ElementStart.newBuilder();

            e.setType(type);

            for (String name : attributes.keySet()) {
              e.addAttribute(ProtocolDocumentOperation.Component.KeyValuePair.newBuilder()
                  .setKey(name).setValue(attributes.get(name)));
            }

            return e.build();
          }

          @Override
          public void elementEnd() {
            output.addComponent(newComponentBuilder().setElementEnd(true));
          }

          @Override
          public void deleteElementEnd() {
            output.addComponent(newComponentBuilder().setDeleteElementEnd(true));
          }

          @Override
          public void replaceAttributes(Attributes oldAttributes, Attributes newAttributes) {
            ProtocolDocumentOperation.Component.ReplaceAttributes.Builder r =
                ProtocolDocumentOperation.Component.ReplaceAttributes.newBuilder();

            if (oldAttributes.isEmpty() && newAttributes.isEmpty()) {
              r.setEmpty(true);
            } else {
              for (String name : oldAttributes.keySet()) {
                r.addOldAttribute(ProtocolDocumentOperation.Component.KeyValuePair.newBuilder()
                    .setKey(name).setValue(oldAttributes.get(name)));
              }

              for (String name : newAttributes.keySet()) {
                r.addNewAttribute(ProtocolDocumentOperation.Component.KeyValuePair.newBuilder()
                    .setKey(name).setValue(newAttributes.get(name)));
              }
            }

            output.addComponent(newComponentBuilder().setReplaceAttributes(r.build()));
          }

          @Override
          public void updateAttributes(AttributesUpdate attributes) {
            ProtocolDocumentOperation.Component.UpdateAttributes.Builder u =
                ProtocolDocumentOperation.Component.UpdateAttributes.newBuilder();

            if (attributes.changeSize() == 0) {
              u.setEmpty(true);
            } else {
              for (int i = 0; i < attributes.changeSize(); i++) {
                u.addAttributeUpdate(makeKeyValueUpdate(
                    attributes.getChangeKey(i), attributes.getOldValue(i),
                    attributes.getNewValue(i)));
              }
            }

            output.addComponent(newComponentBuilder().setUpdateAttributes(u.build()));
          }

          @Override
          public void annotationBoundary(AnnotationBoundaryMap map) {
            ProtocolDocumentOperation.Component.AnnotationBoundary.Builder a =
                ProtocolDocumentOperation.Component.AnnotationBoundary.newBuilder();

            if (map.endSize() == 0 && map.changeSize() == 0) {
              a.setEmpty(true);
            } else {
              for (int i = 0; i < map.endSize(); i++) {
                a.addEnd(map.getEndKey(i));
              }
              for (int i = 0; i < map.changeSize(); i++) {
                a.addChange(makeKeyValueUpdate(
                    map.getChangeKey(i), map.getOldValue(i), map.getNewValue(i)));
              }
            }

            output.addComponent(newComponentBuilder().setAnnotationBoundary(a.build()));
          }

          private ProtocolDocumentOperation.Component.KeyValueUpdate makeKeyValueUpdate(
              String key, String oldValue, String newValue) {
            ProtocolDocumentOperation.Component.KeyValueUpdate.Builder kvu =
                ProtocolDocumentOperation.Component.KeyValueUpdate.newBuilder();
            kvu.setKey(key);
            if (oldValue != null) {
              kvu.setOldValue(oldValue);
            }
            if (newValue != null) {
              kvu.setNewValue(newValue);
            }

            return kvu.build();
          }
        });
      }

      @Override
      public void visitSubmitBlip(SubmitBlip op) {
        // we don't support this operation here.
      }
    });

    return output.build();
  }

  /**
   * Deserializes a {@link ProtocolWaveletDelta} as a {@link Delta}
   *
   * @param protocolDelta protocol buffer wavelet delta to deserialize
   * @return deserialized wavelet delta and version
   */
  public static Delta deserialize(ProtocolWaveletDelta protocolDelta, HashedVersion postVersion,
      WaveletOperationContext ctx) {
    Delta delta =
        new Delta((long)protocolDelta.getHashedVersion().getVersion(), newDistinctVersion(postVersion));

    for (ProtocolWaveletOperation op : protocolDelta.getOperationList()) {
      delta.add(deserialize(op, ctx));
    }

    return delta;
  }

  public static DistinctVersion newDistinctVersion(final HashedVersion postVersion) {
    if (postVersion.getHistoryHash().length == 0) {
      return DistinctVersion.NO_DISTINCT_VERSION;
    }
    byte[] hash = postVersion.getHistoryHash();
    // NOTE(arb): The hash is actually b64(hash), but the distinction just
    // needs to be distinct.
    int signature = (hash[0] << 24) & (hash[1] << 16) & (hash[2] << 8) & (hash[3]);
    return DistinctVersion.of(postVersion.getVersion(), signature);
  }

  /**
   * Deserialize a {@link ProtocolWaveletOperation} as a {@link WaveletOperation}.
   *
   * @param protobufOp protocol buffer wavelet operation to deserialize
   * @return deserialized wavelet operation
   */
  public static WaveletOperation deserialize(ProtocolWaveletOperation protobufOp,
      WaveletOperationContext ctx) {
    if (protobufOp.hasNoOp()) {
      return new NoOp(ctx);
    } else if (protobufOp.hasAddParticipant()) {
      return new AddParticipant(ctx, new ParticipantId(protobufOp.getAddParticipant()));
    } else if (protobufOp.hasRemoveParticipant()) {
      return new RemoveParticipant(ctx, new ParticipantId(protobufOp.getRemoveParticipant()));
    } else if (protobufOp.hasMutateDocument()) {
      return new WaveletBlipOperation(
          protobufOp.getMutateDocument().getDocumentId(),
          new BlipContentOperation(ctx,
              deserialize(protobufOp.getMutateDocument().getDocumentOperation())));
    } else {
      throw new IllegalArgumentException("Unsupported operation: " + protobufOp);
    }
  }

  /**
   * Deserialize a {@link ProtocolDocumentOperation} into a {@link DocOp}.
   *
   * @param op protocol buffer document operation to deserialize
   * @return deserialized DocOp
   */
  public static BufferedDocOp deserialize(ProtocolDocumentOperation op) {
    DocOpBuilder output = new DocOpBuilder();

    for (ProtocolDocumentOperation.Component c : op.getComponentList()) {
      if (c.hasAnnotationBoundary()) {
        if (c.getAnnotationBoundary().getEmpty()) {
          output.annotationBoundary(AnnotationBoundaryMapImpl.EMPTY_MAP);
        } else {
          String[] ends = new String[c.getAnnotationBoundary().getEndCount()];
          String[] changeKeys = new String[c.getAnnotationBoundary().getChangeCount()];
          String[] oldValues = new String[c.getAnnotationBoundary().getChangeCount()];
          String[] newValues = new String[c.getAnnotationBoundary().getChangeCount()];
          if (c.getAnnotationBoundary().getEndCount() > 0) {
            c.getAnnotationBoundary().getEndList().toArray(ends);
          }
          for (int i = 0; i < changeKeys.length; i++) {
            ProtocolDocumentOperation.Component.KeyValueUpdate kvu =
                c.getAnnotationBoundary().getChange(i);
            changeKeys[i] = kvu.getKey();
            oldValues[i] = kvu.hasOldValue() ? kvu.getOldValue() : null;
            newValues[i] = kvu.hasNewValue() ? kvu.getNewValue() : null;
          }
          output.annotationBoundary(
              new AnnotationBoundaryMapImpl(ends, changeKeys, oldValues, newValues));
        }
      } else if (c.hasCharacters()) {
        output.characters(c.getCharacters());
      } else if (c.hasElementStart()) {
        Map<String, String> attributesMap = new HashMap<String, String>();
        for (ProtocolDocumentOperation.Component.KeyValuePair pair :
            c.getElementStart().getAttributeList()) {
          attributesMap.put(pair.getKey(), pair.getValue());
        }
        output.elementStart(c.getElementStart().getType(), new AttributesImpl(attributesMap));
      } else if (c.hasElementEnd()) {
        output.elementEnd();
      } else if (c.hasRetainItemCount()) {
        output.retain(c.getRetainItemCount());
      } else if (c.hasDeleteCharacters()) {
        output.deleteCharacters(c.getDeleteCharacters());
      } else if (c.hasDeleteElementStart()) {
        Map<String, String> attributesMap = new HashMap<String, String>();
        for (ProtocolDocumentOperation.Component.KeyValuePair pair :
            c.getDeleteElementStart().getAttributeList()) {
          attributesMap.put(pair.getKey(), pair.getValue());
        }
        output.deleteElementStart(c.getDeleteElementStart().getType(),
            new AttributesImpl(attributesMap));
      } else if (c.hasDeleteElementEnd()) {
        output.deleteElementEnd();
      } else if (c.hasReplaceAttributes()) {
        if (c.getReplaceAttributes().getEmpty()) {
          output.replaceAttributes(AttributesImpl.EMPTY_MAP, AttributesImpl.EMPTY_MAP);
        } else {
          Map<String, String> oldAttributesMap = new HashMap<String, String>();
          Map<String, String> newAttributesMap = new HashMap<String, String>();
          for (ProtocolDocumentOperation.Component.KeyValuePair pair :
              c.getReplaceAttributes().getOldAttributeList()) {
            oldAttributesMap.put(pair.getKey(), pair.getValue());
          }
          for (ProtocolDocumentOperation.Component.KeyValuePair pair :
              c.getReplaceAttributes().getNewAttributeList()) {
            newAttributesMap.put(pair.getKey(), pair.getValue());
          }
          output.replaceAttributes(new AttributesImpl(oldAttributesMap),
              new AttributesImpl(newAttributesMap));
        }
      } else if (c.hasUpdateAttributes()) {
        if (c.getUpdateAttributes().getEmpty()) {
          output.updateAttributes(AttributesUpdateImpl.EMPTY_MAP);
        } else {
          String[] triplets = new String[c.getUpdateAttributes().getAttributeUpdateCount() * 3];
          for (int i = 0, j = 0; i < c.getUpdateAttributes().getAttributeUpdateCount(); i++) {
            ProtocolDocumentOperation.Component.KeyValueUpdate kvu =
                c.getUpdateAttributes().getAttributeUpdate(i);
            triplets[j++] = kvu.getKey();
            triplets[j++] = kvu.hasOldValue() ? kvu.getOldValue() : null;
            triplets[j++] = kvu.hasNewValue() ? kvu.getNewValue() : null;
          }
          output.updateAttributes(new AttributesUpdateImpl(triplets));
        }
      } else {
        //throw new IllegalArgumentException("Unsupported operation component: " + c);
      }
    }

    return output.build();
  }

}

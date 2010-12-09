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

package org.waveprotocol.box.webclient.common;

import org.waveprotocol.wave.federation.ProtocolDocumentOperation;
import org.waveprotocol.wave.federation.ProtocolHashedVersion;
import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMap;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.AttributesUpdate;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.DocOpCursor;
import org.waveprotocol.wave.model.util.CharBase64;
import org.waveprotocol.wave.model.version.HashedVersion;

/**
 * Utility class for serialising/deserialising wavelet operations (and their components) to/from
 * their protocol buffer representations (and their components).
 *
 *
 */
public class CoreWaveletOperationSerializer {
  private CoreWaveletOperationSerializer() {
  }

  /**
   * Serializes a {@link DocOp} as a {@link ProtocolDocumentOperation}.
   *
   * @param inputOp document operation to serialize
   * @return serialized protocol buffer document operation
   */
  public static ProtocolDocumentOperation serialize(DocOp inputOp) {
    final ProtocolDocumentOperation.Builder output = ProtocolDocumentOperation.newBuilder();

    inputOp.apply(new DocOpCursor() {
      private ProtocolDocumentOperation.Component.Builder newComponentBuilder() {
        return ProtocolDocumentOperation.Component.newBuilder();
      }

      @Override public void retain(int itemCount) {
        output.addComponent(newComponentBuilder().setRetainItemCount(itemCount));
      }

      @Override public void characters(String characters) {
        output.addComponent(newComponentBuilder().setCharacters(characters));
      }

      @Override public void deleteCharacters(String characters) {
        output.addComponent(newComponentBuilder().setDeleteCharacters(characters));
      }

      @Override public void elementStart(String type, Attributes attributes) {
        ProtocolDocumentOperation.Component.ElementStart e = makeElementStart(type, attributes);
        output.addComponent(newComponentBuilder().setElementStart(e));
      }

      @Override public void deleteElementStart(String type, Attributes attributes) {
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

      @Override public void elementEnd() {
        output.addComponent(newComponentBuilder().setElementEnd(true));
      }

      @Override public void deleteElementEnd() {
        output.addComponent(newComponentBuilder().setDeleteElementEnd(true));
      }

      @Override public void replaceAttributes(Attributes oldAttributes, Attributes newAttributes) {
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

      @Override public void updateAttributes(AttributesUpdate attributes) {
        ProtocolDocumentOperation.Component.UpdateAttributes.Builder u =
          ProtocolDocumentOperation.Component.UpdateAttributes.newBuilder();

        if (attributes.changeSize() == 0) {
          u.setEmpty(true);
        } else {
          for (int i = 0; i < attributes.changeSize(); i++) {
            u.addAttributeUpdate(makeKeyValueUpdate(
                attributes.getChangeKey(i), attributes.getOldValue(i), attributes.getNewValue(i)));
          }
        }

        output.addComponent(newComponentBuilder().setUpdateAttributes(u.build()));
      }

      @Override public void annotationBoundary(AnnotationBoundaryMap map) {
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

    return output.build();
  }

  /**
   * Serializes a {@link HashedVersion} POJO to a {@link ProtocolHashedVersion}.
   */
  public static ProtocolHashedVersion serialize(HashedVersion hashedVersion) {
    String b64Hash = CharBase64.encode(hashedVersion.getHistoryHash());
    return ProtocolHashedVersion.newBuilder().setVersion(hashedVersion.getVersion())
        .setHistoryHash(b64Hash);
  }
}

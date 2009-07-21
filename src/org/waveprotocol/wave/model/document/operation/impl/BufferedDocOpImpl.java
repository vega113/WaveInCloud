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

package org.waveprotocol.wave.model.document.operation.impl;

import java.util.ArrayList;

import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMap;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.AttributesUpdate;
import org.waveprotocol.wave.model.document.operation.BufferedDocOp;
import org.waveprotocol.wave.model.document.operation.DocOpComponentType;
import org.waveprotocol.wave.model.document.operation.DocOpCursor;
import org.waveprotocol.wave.model.document.operation.EvaluatingDocOpCursor;
import org.waveprotocol.wave.model.document.operation.impl.OperationComponents.AnnotationBoundary;
import org.waveprotocol.wave.model.document.operation.impl.OperationComponents.Characters;
import org.waveprotocol.wave.model.document.operation.impl.OperationComponents.DeleteCharacters;
import org.waveprotocol.wave.model.document.operation.impl.OperationComponents.DeleteElementEnd;
import org.waveprotocol.wave.model.document.operation.impl.OperationComponents.DeleteElementStart;
import org.waveprotocol.wave.model.document.operation.impl.OperationComponents.DocOpComponent;
import org.waveprotocol.wave.model.document.operation.impl.OperationComponents.ElementEnd;
import org.waveprotocol.wave.model.document.operation.impl.OperationComponents.ElementStart;
import org.waveprotocol.wave.model.document.operation.impl.OperationComponents.ReplaceAttributes;
import org.waveprotocol.wave.model.document.operation.impl.OperationComponents.Retain;
import org.waveprotocol.wave.model.document.operation.impl.OperationComponents.UpdateAttributes;
import org.waveprotocol.wave.model.util.Preconditions;

/*
 * TODO: We should get rid of BufferedDocOpImpl. All we need is the
 * builder. The BufferedDocOpImpl type has a private constructor, and the
 * Builder does not return the concrete type, so it can never really be used
 * directly by anything so there seems to be no real purpose for having this type.
 */
public final class BufferedDocOpImpl implements BufferedDocOp {

  public static class DocOpBuilder implements EvaluatingDocOpCursor<BufferedDocOp> {

    private static final DocOpComponent[] EMPTY_ARRAY = new DocOpComponent[0];

    private final ArrayList<DocOpComponent> accu = new ArrayList<DocOpComponent>();

    @Override
    public final BufferedDocOp finish() {
      return new BufferedDocOpImpl(accu.toArray(EMPTY_ARRAY));
    }

    @Override
    public final void annotationBoundary(AnnotationBoundaryMap map) {
      accu.add(new AnnotationBoundary(map));
    }
    @Override
    public final void characters(String s) {
      accu.add(new Characters(s));
    }
    @Override
    public final void elementEnd() {
      accu.add(ElementEnd.INSTANCE);
    }
    @Override
    public final void elementStart(String type, Attributes attrs) {
      accu.add(new ElementStart(type, attrs));
    }
    @Override
    public final void deleteCharacters(String s) {
      accu.add(new DeleteCharacters(s));
    }
    @Override
    public final void retain(int itemCount) {
      accu.add(new Retain(itemCount));
    }
    @Override
    public final void deleteElementEnd() {
      accu.add(DeleteElementEnd.INSTANCE);
    }
    @Override
    public final void deleteElementStart(String type, Attributes attrs) {
      accu.add(new DeleteElementStart(type, attrs));
    }
    @Override
    public final void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
      accu.add(new ReplaceAttributes(oldAttrs, newAttrs));
    }
    @Override
    public final void updateAttributes(AttributesUpdate update) {
      accu.add(new UpdateAttributes(update));
    }

  }

  private final DocOpComponent[] components;

  private BufferedDocOpImpl(DocOpComponent[] components) {
    this.components = components;
  }

  @Override
  public int size() {
    return components.length;
  }

  @Override
  public DocOpComponentType getType(int i) {
    return components[i].getType();
  }

  @Override
  public void applyComponent(int i, DocOpCursor cursor) {
    components[i].apply(cursor);
  }

  @Override
  public void apply(DocOpCursor cursor) {
    for (DocOpComponent component : components) {
      component.apply(cursor);
    }
  }

  @Override
  public String getCharactersString(int i) {
    check(i, DocOpComponentType.CHARACTERS);
    return ((Characters) components[i]).string;
  }

  @Override
  public String getDeleteCharactersString(int i) {
    check(i, DocOpComponentType.DELETE_CHARACTERS);
    return ((DeleteCharacters) components[i]).string;
  }

  @Override
  public Attributes getReplaceAttributesNewAttributes(int i) {
    check(i, DocOpComponentType.REPLACE_ATTRIBUTES);
    return ((ReplaceAttributes) components[i]).newAttrs;
  }

  @Override
  public Attributes getReplaceAttributesOldAttributes(int i) {
    check(i, DocOpComponentType.REPLACE_ATTRIBUTES);
    return ((ReplaceAttributes) components[i]).oldAttrs;
  }

  @Override
  public int getRetainItemCount(int i) {
    check(i, DocOpComponentType.RETAIN);
    return ((Retain) components[i]).itemCount;
  }

  @Override
  public AnnotationBoundaryMap getAnnotationBoundary(int i) {
    check(i, DocOpComponentType.ANNOTATION_BOUNDARY);
    return ((AnnotationBoundary) components[i]).boundary;
  }

  @Override
  public Attributes getDeleteElementStartAttributes(int i) {
    check(i, DocOpComponentType.DELETE_ELEMENT_START);
    return ((DeleteElementStart) components[i]).attrs;
  }

  @Override
  public String getDeleteElementStartTag(int i) {
    check(i, DocOpComponentType.DELETE_ELEMENT_START);
    return ((DeleteElementStart) components[i]).type;
  }

  @Override
  public Attributes getElementStartAttributes(int i) {
    check(i, DocOpComponentType.ELEMENT_START);
    return ((ElementStart) components[i]).attrs;
  }

  @Override
  public String getElementStartTag(int i) {
    check(i, DocOpComponentType.ELEMENT_START);
    return ((ElementStart) components[i]).type;
  }

  @Override
  public AttributesUpdate getUpdateAttributesUpdate(int i) {
    check(i, DocOpComponentType.UPDATE_ATTRIBUTES);
    return ((UpdateAttributes) components[i]).update;
  }

  private void check(int i, DocOpComponentType type) {
    Preconditions.checkArgument(components[i].getType() == type,
        "Component " + i + " is not of type ' " + type + "', " +
        "it is '" + components[i].getType() + "'");
  }
}

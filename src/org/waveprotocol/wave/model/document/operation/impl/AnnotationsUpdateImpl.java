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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMap;
import org.waveprotocol.wave.model.document.operation.util.ImmutableUpdateMap;
import org.waveprotocol.wave.model.util.Preconditions;

public class AnnotationsUpdateImpl
    extends ImmutableUpdateMap<AnnotationsUpdateImpl, AnnotationsUpdate>
    implements AnnotationsUpdate {

  public static final AnnotationsUpdateImpl EMPTY_MAP = new AnnotationsUpdateImpl();

  public AnnotationsUpdateImpl() {}

  private AnnotationsUpdateImpl(List<AttributeUpdate> updates) {
    super(updates);
  }

  @Override
  protected AnnotationsUpdateImpl createFromList(List<AttributeUpdate> updates) {
    return new AnnotationsUpdateImpl(updates);
  }

  @Override
  public AnnotationsUpdateImpl composeWith(AnnotationBoundaryMap map) {
    Map<String, AttributeUpdate> newUpdates = new HashMap<String, AttributeUpdate>(updates.size());
    for (AttributeUpdate u : updates) {
      newUpdates.put(u.name, u);
    }
    for (int i = 0; i < map.changeSize(); i++) {
      String key = map.getChangeKey(i);
      newUpdates.put(key, new AttributeUpdate(key, map.getOldValue(i), map.getNewValue(i)));
    }
    for (int i = 0; i < map.endSize(); i++) {
      newUpdates.remove(map.getEndKey(i));
    }
    List<AttributeUpdate> l = new ArrayList<AttributeUpdate>(newUpdates.values());
    Collections.sort(l, comparator);
    return createFromList(l);
  }

  public boolean containsKey(String key) {
    Preconditions.checkNotNull(key, "Null key");
    // TODO: Use Arrays.binarySearch(a, key, c).
    for (AttributeUpdate u : updates) {
      if (key.equals(u.name)) {
        return true;
      }
    }
    return false;
  }
}

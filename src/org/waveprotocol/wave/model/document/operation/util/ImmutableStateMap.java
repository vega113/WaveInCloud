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

package org.waveprotocol.wave.model.document.operation.util;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.waveprotocol.wave.model.document.operation.util.ImmutableUpdateMap.AttributeUpdate;
import org.waveprotocol.wave.model.util.Preconditions;

public abstract class ImmutableStateMap<T extends ImmutableStateMap<T, U>, U extends UpdateMap>
    extends AbstractMap<String, String> {

  /**
   * A name-value pair representing an attribute.
   */
  public static final class Attribute implements Map.Entry<String,String> {
    // TODO: This class can be simplified greatly if
    // AbstractMap.SimpleImmutableEntry from Java 6 can be used.

    private final String name;
    private final String value;

    /**
     * Creates an attribute with a map entry representing an attribute
     * name-value pair.
     *
     * @param entry The attribute's name-value pair.
     */
    public Attribute(Map.Entry<String,String> entry) {
      this(entry.getKey(), entry.getValue());
    }

    /**
     * Creates an attribute given a name-value pair.
     *
     * @param name The name of the attribute.
     * @param value The value of the attribute.
     */
    public Attribute(String name, String value) {
      Preconditions.checkNotNull(name, "Null attribute name");
      Preconditions.checkNotNull(name, "Null attribute value");
      this.name = name;
      this.value = value;
    }

    @Override
    public String getKey() {
      return name;
    }

    @Override
    public String getValue() {
      return value;
    }

    @Override
    public String setValue(String value) {
      throw new UnsupportedOperationException("Attempt to modify an immutable map entry.");
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Map.Entry)) {
        return false;
      }
      Map.Entry<?,?> entry = (Map.Entry<?,?>) o;
      return ((name == null) ? entry.getKey() == null : name.equals(entry.getKey())) &&
          ((value == null) ? entry.getValue() == null : value.equals(entry.getValue()));
    }

    @Override
    public int hashCode() {
      return ((name == null) ? 0 : name.hashCode()) ^
          ((value == null) ? 0 : value.hashCode());
    }

    @Override
    public String toString() {
      return "Attribute(" + name + "=" + value + ")";
    }
  }

  private final List<Attribute> attributes;

  private final Set<Map.Entry<String,String>> entrySet =
      new AbstractSet<Map.Entry<String,String>>() {

        @Override
        public Iterator<Map.Entry<String,String>> iterator() {
          return new Iterator<Map.Entry<String,String>>() {
            private Iterator<Attribute> iterator = attributes.iterator();
            public boolean hasNext() {
              return iterator.hasNext();
            }
            public Attribute next() {
              return iterator.next();
            }
            public void remove() {
              throw new UnsupportedOperationException("Attempt to modify an immutable set.");
            }
          };
        }

        @Override
        public int size() {
          return attributes.size();
        }

      };

  /**
   * Creates a new T object containing no T.
   */
  public ImmutableStateMap() {
    attributes = Collections.emptyList();
  }

  private static final Comparator<Attribute> comparator = new Comparator<Attribute>() {
    @Override
    public int compare(Attribute a, Attribute b) {
      return a.name.compareTo(b.name);
    }
  };

  /**
   * Constructs a new <code>T</code> object with the T
   * specified by the given mapping.
   *
   * @param map The mapping of attribute names to attribute values.
   */
  public ImmutableStateMap(Map<String,String> map) {
    this.attributes = attributeListFromMap(map);
  }

  public ImmutableStateMap(String ... pairs) {
    if (pairs.length % 2 != 0) {
      throw new IllegalArgumentException("Pairs must contain an even number of strings");
    }

    Map<String, String> map = new HashMap<String, String>();

    for (int i = 0; i < pairs.length; i += 2) {
      if (pairs[i] == null || pairs[i + 1] == null) {
        throw new IllegalArgumentException("A state map may not contain null keys or values");
      }
      if (map.containsKey(pairs[i])) {
        throw new IllegalArgumentException("Duplicate key: '" + pairs[i] + "'");
      }
      map.put(pairs[i], pairs[i + 1]);
    }

    this.attributes = attributeListFromMap(map);
  }

  private List<Attribute> attributeListFromMap(Map<String, String> map) {
    ArrayList<Attribute> attributeList = new ArrayList<Attribute>(map.size());
    for (Iterator<Map.Entry<String,String>> iterator = map.entrySet().iterator();
        iterator.hasNext();) {
      Map.Entry<String,String> entry = iterator.next();
      if (entry.getKey() == null || entry.getValue() == null) {
        throw new NullPointerException("This map does not allow null keys or values.");
      }
      attributeList.add(new Attribute(entry));
    }
    Collections.sort(attributeList, comparator);
    return attributeList;
  }

  protected ImmutableStateMap(List<Attribute> attributes) {
    this.attributes = attributes;
  }

  @Override
  public Set<Map.Entry<String,String>> entrySet() {
    return entrySet;
  }

  /**
   * Returns a <code>T</code> object obtained by applying the update
   * specified by the <code>U</code> object into this
   * <code>T</code> object.
   *
   * @param attributeUpdate The update to apply.
   * @return A <code>T</code> object obtained by applying the given
   *         update onto this object.
   */
  public T updateWith(U attributeUpdate) {
    List<Attribute> newImmutableStateMap = new ArrayList<Attribute>();
    Iterator<Attribute> iterator = attributes.iterator();
    Attribute nextAttribute = iterator.hasNext() ? iterator.next() : null;
    // TODO: Have a slow path when the cast would fail.
    List<AttributeUpdate> updates = ((ImmutableUpdateMap<?,?>) attributeUpdate).updates;
    for (AttributeUpdate update : updates) {
      while (nextAttribute != null) {
        int comparison = update.name.compareTo(nextAttribute.name);
        if (comparison > 0) {
          newImmutableStateMap.add(nextAttribute);
          nextAttribute = iterator.hasNext() ? iterator.next() : null;
        } else if (comparison < 0) {
          if (update.oldValue != null) {
            throw new IllegalArgumentException(
                "Mismatched old value: attempt to update unset attribute with " + update);
          }
          break;
        } else if (comparison == 0 ) {
          if (!nextAttribute.value.equals(update.oldValue)) {
            throw new IllegalArgumentException("Mismatched old value: attempt to update " +
                nextAttribute + " with " + update);
          }
          nextAttribute = iterator.hasNext() ? iterator.next() : null;
          break;
        }
      }
      if (update.newValue != null) {
        newImmutableStateMap.add(new Attribute(update.name, update.newValue));
      }
    }
    if (nextAttribute != null) {
      newImmutableStateMap.add(nextAttribute);
      while (iterator.hasNext()) {
        newImmutableStateMap.add(iterator.next());
      }
    }
    return createFromList(newImmutableStateMap);
  }

  protected abstract T createFromList(List<Attribute> attributes);
}
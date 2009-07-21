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

package org.waveprotocol.wave.model.id;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * A filter which accepts a set of wavelets ids.
 *
 * A filter comprises a (possibly empty) set of ids to explicitly match, plus a
 * (possibly empty) set of id prefixes to match. A wavelet id is accepted by
 * the filter iff it exactly matches one of the specified ids, or its prefix
 * matches one of the specified prefixes, or the filter is empty. A filter is
 * immutable.
 *
 * A filter which is empty accepts all ids. There is no way to construct a
 * filter which accepts no ids.
 *
 * TODO: revisit this in the light of domain-qualified ids when they are
 * implemented.
 *
 *
 */
public final class IdFilter {

  private static final Collection<WaveletId> NO_WAVELET_IDS = Collections.<WaveletId> emptyList();
  private static final Collection<String> NONE_PREFIX = Collections.<String> emptyList();

  /** An id filter which accepts all ids. */
  public static final IdFilter ALL_IDS = new IdFilter(NO_WAVELET_IDS, NONE_PREFIX);

  /** Creates a filter accepting a specified list of ids. */
  public static IdFilter ofIds(WaveletId... ids) {
    return new IdFilter(Arrays.asList(ids), NONE_PREFIX);
  }

  /** Creates a filter accepting a specified list of prefixes. */
  public static IdFilter ofPrefixes(String... prefixes) {
    return new IdFilter(NO_WAVELET_IDS, Arrays.asList(prefixes));
  }

  private final Set<WaveletId> ids;
  private final Set<String> prefixes;

  /**
   * Creates a new filter from a collection of ids and a collection of
   * id prefixes. Either parameter may be empty but neither parameter
   * may be null.
   *
   * Prefixes must be non-empty strings.
   *
   * @param ids ids to match exactly
   * @param prefixes id prefixes to match
   */
  public IdFilter(Collection<WaveletId> ids, Collection<String> prefixes) {
    this.ids = new HashSet<WaveletId>(ids);
    this.prefixes = new HashSet<String>(prefixes);
    if (prefixes.contains("")) {
      throw new IllegalArgumentException("Empty id prefix");
    }
  }

  /**
   * Gets an unmodifiable view of the ids in the filter.
   */
  public Collection<WaveletId> getIds() {
    return Collections.unmodifiableSet(this.ids);
  }

  /**
   * Gets an unmodifiable view of the id prefixes in the filter.
   */
  public Collection<String> getPrefixes() {
    return Collections.unmodifiableSet(this.prefixes);
  }

  /**
   * Checks whether an id is accepted by the filter.
   */
  public boolean accepts(WaveletId id) {
    boolean match = ids.isEmpty() && prefixes.isEmpty();
    if (!match) {
      match = ids.contains(id);
    }
    Iterator<String> itr = prefixes.iterator();
    while (itr.hasNext() && !match) {
      match = match || id.getId().startsWith(itr.next());
    }
    return match;
  }
}

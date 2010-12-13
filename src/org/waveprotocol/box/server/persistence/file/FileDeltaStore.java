/**
 * Copyright 2010 Google Inc.
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

package org.waveprotocol.box.server.persistence.file;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.waveprotocol.box.common.ExceptionalIterator;
import org.waveprotocol.box.server.CoreSettings;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.box.server.waveserver.DeltaStore;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * A flat file based implementation of DeltaStore.
 *
 * The delta store lives at some base directory. The directory structure looks like this:
 * base/encoded-wave-id/encoded-wavelet-id.delta
 * base/encoded-wave-id/encoded-wavelet-id.index
 *
 * See design doc:
 * https://sites.google.com/a/waveprotocol.org/wave-protocol/protocol/design-proposals/wave-store-design-for-wave-in-a-box
 *

 * @author josephg@gmail.com (Joseph Gentle)
 */
public class FileDeltaStore implements DeltaStore {
  /**
   * The directory in which the wavelets are stored
   */
  final private String basePath;

  @Inject
  public FileDeltaStore(@Named(CoreSettings.DELTA_STORE_DIRECTORY) String basePath) {
    Preconditions.checkNotNull(basePath, "Requested path is null");
    this.basePath = basePath;
  }

  @Override
  public void delete(WaveletName waveletName) throws PersistenceException {
    new FileDeltaCollection(waveletName, basePath).delete();
  }

  @Override
  public Set<WaveletId> lookup(WaveId waveId) throws PersistenceException {
    String waveDirectory = FileUtils.waveIdToPathSegment(waveId);
    File waveDir = new File(basePath, waveDirectory);
    if (!waveDir.exists()) {
      return ImmutableSet.of();
    }

    File[] deltaFiles = waveDir.listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return name.endsWith(FileDeltaCollection.DELTAS_FILE_SUFFIX);
      }
    });

    ImmutableSet.Builder<WaveletId> results = ImmutableSet.builder();
    for(File deltaFile : deltaFiles) {
      String name = deltaFile.getName();
      String encodedWaveletId =
          name.substring(0, name.lastIndexOf(FileDeltaCollection.DELTAS_FILE_SUFFIX));
      WaveletId waveletId = FileUtils.waveletIdFromPathSegment(encodedWaveletId);
      results.add(waveletId);
    }

    return results.build();
  }

  @Override
  public DeltasAccess open(WaveletName waveletName) throws PersistenceException {
    return new FileDeltaCollection(waveletName, basePath);
  }
  
  @Override
  public ExceptionalIterator<WaveId, PersistenceException> getWaveIdIterator()
      throws PersistenceException {
    File baseDir = new File(basePath);
    if (!baseDir.exists()) {
      return ExceptionalIterator.Empty.create();
    }

    File[] waveDirs = baseDir.listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        try {
          FileUtils.waveIdFromPathSegment(name);
          return true;
        } catch (IllegalArgumentException e) {
          return false;
        }
      }
    });

    final ImmutableSet.Builder<WaveId> results = ImmutableSet.builder();
    for(File waveDir : waveDirs) {
      results.add(FileUtils.waveIdFromPathSegment(waveDir.getName()));
    }
    
    return new ExceptionalIterator<WaveId, PersistenceException>() {
      private final Iterator<WaveId> iterator = results.build().iterator();
      private boolean nextFetched = false;
      private WaveId nextWaveId = null;

      private void fetchNext() throws PersistenceException {
        while(!nextFetched) {
          if (iterator.hasNext()) {
            nextWaveId = iterator.next();
            if (!lookup(nextWaveId).isEmpty()) {
              nextFetched = true;
            }
          } else {
            nextFetched = true;
            nextWaveId = null;
          }
        }
      }
      
      @Override
      public boolean hasNext() throws PersistenceException {
        fetchNext();
        return nextWaveId != null; 
      }

      @Override
      public WaveId next() throws PersistenceException {
        fetchNext();
        if (nextWaveId == null) {
          throw new NoSuchElementException();
        } else {
          nextFetched = false;
          return nextWaveId;
        }
      }
      
    };
  }
}

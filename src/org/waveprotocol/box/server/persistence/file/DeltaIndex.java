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

import org.waveprotocol.wave.model.util.Pair;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * An index for quickly accessing deltas. The index is an array of longs, one for each version.
 * The file contains -1 for any version for which there is not a delta. This will happen whenever
 * the previous delta contains multiple ops.
 *
 * @author josephg@google.com (Joseph Gentle)
 */
public class DeltaIndex {
  /** Returned from methods when there is no record for a specified version. */
  public static final int NO_RECORD_FOR_VERSION = -1;

  private static final int NO_RECORD_MARKER = NO_RECORD_FOR_VERSION;
  private static final int RECORD_LENGTH = 8;
  private final File fileRef;
  private RandomAccessFile file;

  public DeltaIndex(File indexFile) {
    this.fileRef = indexFile;
  }

  /**
   * Open the index.
   *
   * @param baseCollection the collection which the index indexes.
   * @throws IOException
   */
  public void openForCollection(FileDeltaCollection baseCollection) throws IOException {
    if (!fileRef.exists()) {
      fileRef.mkdirs();
      rebuildIndexFromDeltas(baseCollection);
    } else {
      // TODO(josephg): For now, we just rebuild the index anyway.
      rebuildIndexFromDeltas(baseCollection);
    }
  }

  private void checkOpen() {
    Preconditions.checkState(file != null, "Index file not open");
  }

  /**
   * Rebuild the index based on a delta collection. This will wipe the index file.
   *
   * @param collection
   * @throws IOException
   */
  public void rebuildIndexFromDeltas(FileDeltaCollection collection) throws IOException {
    if (file != null) {
      file.close();
    }

    if (fileRef.exists()) {
      fileRef.delete();
    }

    file = FileUtils.getOrCreateFile(fileRef);
    long lastVersion = 0;

    for (Pair<Long, Long> pair : collection.getOffsetsIterator()) {
      long version = pair.first;
      long position = pair.second;

      // Because the file is sparse, we need to fill in zeros from v=lastVersion to v=version.
      while (lastVersion < (version - 1)) {
        file.writeLong(NO_RECORD_MARKER);
        lastVersion++;
      }

      file.writeLong(position);
    }
  }

  /**
   * Get the delta file offset for the specified version.
   *
   * @param version
   * @return the offset on success, -1 if there's no record.
   * @throws IOException
   */
  public long getOffsetForVersion(long version) throws IOException {
    Preconditions.checkArgument(version >= 0);
    checkOpen();

    long position = version * RECORD_LENGTH;
    if (position >= file.length()) {
      return NO_RECORD_FOR_VERSION;
    }

    file.seek(position);
    long offset = file.readLong();
    if (offset == NO_RECORD_MARKER) {
      return NO_RECORD_FOR_VERSION;
    } else {
      return offset;
    }
  }

  /**
   * Set the delta file offset for a particular delta version.
   *
   * @param version
   * @param offset
   * @throws IOException
   */
  public void setOffsetForVersion(long version, long offset) throws IOException {
    checkOpen();

    long position = version * RECORD_LENGTH;
    long fileLength = file.length();
    while (fileLength < position) {
      file.seek(fileLength);
      file.writeLong(NO_RECORD_MARKER);
      fileLength += RECORD_LENGTH;
    }

    file.seek(position);
    file.writeLong(offset);
  }

  /**
   * @return number of records in the index
   */
  public long length() {
    checkOpen();

    long fileLength;
    try {
      fileLength = file.length();
    } catch (IOException e) {
      // This shouldn't happen in practice.
      throw new RuntimeException("IO error reading index file length", e);
    }
    return fileLength / RECORD_LENGTH;
  }

  public void close() throws IOException {
    if (file != null) {
      file.close();
      file = null;
    }
  }
}

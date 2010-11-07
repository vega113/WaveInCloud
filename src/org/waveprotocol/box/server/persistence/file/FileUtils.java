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

import com.google.gxp.com.google.common.base.Preconditions;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.util.Pair;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;

/**
 * Utility methods for file stores.
 *
 * @author josephg@gmail.com (Joseph Gentle)
 */
public class FileUtils {
  private static final String SEPARATOR = "_";

  /**
   * Converts an arbitrary string into a format that can be stored safely on the filesystem.
   *
   * @param str the string to encode
   * @return the encoded string
   */
  public static String toFilenameFriendlyString(String str) {
    byte[] bytes;
    try {
      bytes = str.getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      // This should never happen.
      throw new IllegalStateException("UTF-8 not supported", e);
    }

    return new String(Hex.encodeHex(bytes));
  }

  /**
   * Decodes a string that was encoded using toFilenameFriendlyString.
   *
   * @param encoded the encoded string
   * @return the decoded string
   * @throws DecoderException the string's encoding is invalid
   */
  public static String fromFilenameFriendlyString(String encoded) throws DecoderException {
    byte[] bytes = Hex.decodeHex(encoded.toCharArray());
    try {
      return new String(bytes, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      // This should never happen.
      throw new IllegalStateException("UTF-8 not supported", e);
    }
  }

  /** Decode a path segment pair. Throws IllegalArgumentException if the encoding is invalid */
  private static Pair<String, String> decodePathSegmentPair(String pathSegment) {
    String[] components = pathSegment.split(SEPARATOR);
    Preconditions.checkArgument(components.length == 2, "WaveId path name invalid");
    try {
      return new Pair<String, String>(fromFilenameFriendlyString(components[0]),
          fromFilenameFriendlyString(components[1]));
    } catch (DecoderException e) {
      throw new IllegalArgumentException("Wave path component encoding invalid");
    }
  }

  /**
   * Creates a filename-friendly pathname for the given waveId.
   *
   * The format is DOMAIN + '_' + ID where both the domain and the id are encoded
   * to a pathname friendly format.
   *
   * @param waveId the waveId to encode
   * @return a path segment which corresponds to the waveId
   */
  public static String waveIdToPathSegment(WaveId waveId) {
    String domain = toFilenameFriendlyString(waveId.getDomain());
    String id = toFilenameFriendlyString(waveId.getId());
    return domain + SEPARATOR + id;
  }

  /**
   * Converts a path segment created using waveIdToPathSegment back to a wave id
   *
   * @param pathSegment
   * @return the decoded WaveId
   * @throws IllegalArgumentException the encoding on the path segment is invalid
   */
  public static WaveId waveIdFromPathSegment(String pathSegment) {
    Pair<String, String> segments = decodePathSegmentPair(pathSegment);
    return new WaveId(segments.first, segments.second);
  }

  /**
   * Creates a filename-friendly path segment for a waveId.
   *
   * The format is "domain_id", encoded in a pathname friendly format.
   * @param waveletId
   * @return the decoded WaveletId
   */
  public static String waveletIdToPathSegment(WaveletId waveletId) {
    String domain = toFilenameFriendlyString(waveletId.getDomain());
    String id = toFilenameFriendlyString(waveletId.getId());
    return domain + SEPARATOR + id;
  }

  /**
   * Converts a path segment created using waveIdToPathSegment back to a wave id.
   *
   * @param pathSegment
   * @return the decoded waveletId
   * @throws IllegalArgumentException the encoding on the path segment is invalid
   */
  public static WaveletId waveletIdFromPathSegment(String pathSegment) {
    Pair<String, String> segments = decodePathSegmentPair(pathSegment);
    return new WaveletId(segments.first, segments.second);
  }

  /**
   * Creates a filename-friendly path segment for a wavelet name.
   *
   * @param waveletId
   * @return the filename-friendly path segment representing the waveletId
   */
  public static String waveletNameToPathSegment(WaveletName waveletName) {
    return waveIdToPathSegment(waveletName.waveId)
        + File.separatorChar
        + waveletIdToPathSegment(waveletName.waveletId);
  }

  /**
   * Get a file for random binary access. If the file doesn't exist, it will be created.
   *
   * Calls to write() will not flush automatically. Call file.getChannel().force(true) to force
   * writes to flush to disk.
   *
   * @param fileRef the file to open
   * @return an opened RandomAccessFile wrapping the requested file
   * @throws IOException an error occurred opening or creating the file
   */
  public static RandomAccessFile getOrCreateFile(File fileRef) throws IOException {
    if (!fileRef.exists()) {
      fileRef.getParentFile().mkdirs();
      fileRef.createNewFile();
    }

    RandomAccessFile file;
    try {
      file = new RandomAccessFile(fileRef, "rw");
    } catch (FileNotFoundException e) {
      // This should never happen.
      throw new IllegalStateException("Java said the file exists, but it can't open it", e);
    }

    return file;
  }

  /** Create and return a new temporary directory */
  public static File createTemporaryDirectory() throws IOException {
    // We want a temporary directory. createTempFile will make a file with a
    // good temporary path. Its a bit nasty, but we'll create the file, then
    // delete it and create a directory with the same name.

    File dir = File.createTempFile("fedoneattachments", null);

    if (!dir.delete() || !dir.mkdir()) {
      throw new IOException("Could not make temporary directory for attachment store: "
          + dir);
    }

    return dir.getAbsoluteFile();
  }
}
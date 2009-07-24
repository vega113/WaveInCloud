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

package org.waveprotocol.wave.examples.fedone.waveclient.console;

import jline.ANSIBuffer;

import java.util.List;

/**
 * Utility methods and constants for use with the console client.
 *
 *
 */
public class ConsoleUtils {
  /** Key for paragraph elements. */
  public static final String LINE = "line";

  /** Tag for identifying the author of a line. */
  public static final String LINE_AUTHOR = "by";

  /** ANSI code for text with no attributes. */
  public static final int ANSI_NO_ATTRS = 0;

  /** ANSI code for underlined text. */
  public static final int ANSI_UNDERLINE = 4;

  /** ANSI code for green foreground text. */
  public static final int ANSI_GREEN_FG = 32;

  /** ANSI code for yellow foreground text. */
  public static final int ANSI_YELLOW_FG = 33;

  /** ANSI code for blue foreground text. */
  public static final int ANSI_BLUE_FG = 34;

  /** ANSI code for white foreground text. */
  public static final int ANSI_WHITE_FG = 37;

  /** ANSI code for green background text. */
  public static final int ANSI_GREEN_BG = 42;

  /** ANSI code for blue background text. */
  public static final int ANSI_BLUE_BG = 44;

  /** ANSI code for cyan background text. */
  public static final int ANSI_CYAN_BG = 46;

  private ConsoleUtils() {
  }

  /**
   * Ensure the width of a StringBuilder by dropping characters at the end or filling with spaces.
   *
   * @param width to set the string buffer to
   * @param builder to ensure length of
   */
  public static void ensureWidth(int width, StringBuilder builder) {
    if (builder.length() > width) {
      builder.delete(width, builder.length());
    } else {
      while (builder.length() < width) {
        builder.append(' ');
      }
    }
  }

  /**
   * Ensure the width of a String by dropping characters at the end or filling with spaces.
   *
   * @param width of the new String
   * @param string to ensure length of
   * @return String guaranteed to be of given width
   */
  public static String ensureWidth(int width, String string) {
    StringBuilder builder = new StringBuilder(string);
    ensureWidth(width, builder);
    return builder.toString();
  }

  /**
   * Create a blank line of a given width.
   *
   * @param width of the blank line
   * @return blank line of given width
   */
  public static String blankLine(int width) {
    return ensureWidth(width, "");
  }

  /**
   * Ensure the "height" of a number of lines by adding blank lines to the end.
   *
   * @param width each line should be (for filling in missing lines)
   * @param height
   * @param lines to ensure height of
   */
  public static void ensureHeight(int width, int height, List<String> lines) {
    while (lines.size() < height) {
      lines.add(blankLine(width));
    }
  }


  /**
   * Wrap a string in a list of ANSI escape codes, then reset at the end.
   *
   * @param ansiCodes to apply to the string
   * @param string to apply the codes to
   * @return string with applied ANSI codes
   */
  public static String ansiWrap(List<Integer> ansiCodes, String string) {
    StringBuilder builder = new StringBuilder(string);

    for (Integer code : ansiCodes) {
      builder.insert(0, ANSIBuffer.ANSICodes.attrib(code));
    }

    builder.append(ANSIBuffer.ANSICodes.attrib(ANSI_NO_ATTRS));
    return builder.toString();
  }

  /**
   * Render a String "nicely" by replacing new lines and tabs with spaces (etc).
   *
   * @param string to render nicely
   * @return nice version of string
   */
  public static String renderNice(String string) {
    string = string.replaceAll("\n", " ");
    string = string.replaceAll("\t", "        ");
    return string;
  }
}
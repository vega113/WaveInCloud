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

package org.waveprotocol.pst;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.waveprotocol.pst.style.JavaStyler;
import org.waveprotocol.pst.style.PstStyler;

import java.io.File;
import java.util.Map;

/**
 * Encapsulates the command line options to protobuf-stringtemplate.
 *
 * @author kalman@google.com (Benjamin Kalman)
 */
public final class PstCommandLine {

  private static final String DEFAULT_OUTPUT_DIR = ".";
  private static final Map<String, JavaStyler> STYLERS = ImmutableMap.<String, JavaStyler> builder()
      .put("none", JavaStyler.EMPTY)
      .put("pst", new PstStyler())
      .build();

  private final CommandLine cl;

  public PstCommandLine(String... args) throws ParseException {
    cl = new BasicParser().parse(getOptions(), args);
    checkArgs();
  }

  private void checkArgs() throws ParseException {
    if (!hasFile()) {
      throw new ParseException("Must specify file");
    }
    if (cl.getArgList().isEmpty()) {
      throw new ParseException("Must specify at least one template");
    }
  }

  private static Options getOptions() {
    Options options = new Options();
    options.addOption("h", "help", false, "Show this help");
    options.addOption("f", "file", true, "The protobuf specification file to use");
    options.addOption("d", "dir", true, String.format(
        "The base directory to output generated files to (default: %s)", DEFAULT_OUTPUT_DIR));
    options.addOption("s", "styler", true, "The styler to use, if any (default: none). " +
        "Available options: " + STYLERS.keySet());
    options.addOption("b", "backup", false, "Save backups of intermediate files (e.g. pre-style)");
    return options;
  }

  public boolean hasHelp() {
    return cl.hasOption('h');
  }

  // NOTE: private because it's always true, checked in checkArgs().
  private boolean hasFile() {
    return cl.hasOption('f');
  }

  public static void printHelp() {
    new HelpFormatter().printHelp(
        PstMain.class.getSimpleName() + " [options] templates...", getOptions());
  }

  public File getProtoFile() {
    return new File(cl.getOptionValue('f'));
  }

  @SuppressWarnings("unchecked")
  public Iterable<File> getTemplateFiles() {
    return Iterables.transform(cl.getArgList(), new Function<String, File>() {
      @Override public File apply(String filename) {
        return new File(filename);
      }
    });
  }

  public File getOutputDir() {
    return new File(cl.hasOption('d') ? cl.getOptionValue('d') : DEFAULT_OUTPUT_DIR);
  }

  public JavaStyler getStyler() {
    if (cl.hasOption('s')) {
      String stylerName = cl.getOptionValue('s');
      if (STYLERS.containsKey(stylerName)) {
        return STYLERS.get(stylerName);
      } else {
        System.err.println("WARNING: unrecognised styler: " + stylerName + ", using none");
        return JavaStyler.EMPTY;
      }
    } else {
      return JavaStyler.EMPTY;
    }
  }

  public boolean shouldSaveBackups() {
    return cl.hasOption('b');
  }
}

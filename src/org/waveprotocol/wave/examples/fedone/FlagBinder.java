/*
 * Copyright (C) 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.waveprotocol.wave.examples.fedone;

import com.google.protobuf.RpcCallback;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.name.Names;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.protocol.common;
import org.waveprotocol.wave.waveserver.SubmitResultListener;
import org.waveprotocol.wave.waveserver.WaveletFederationListener;
import org.waveprotocol.wave.waveserver.WaveletFederationProvider;
import org.xmpp.packet.Message;

import java.lang.reflect.Field;
import java.util.logging.Logger;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A command line flag parsing system that converts a given
 * FlagSettings class into a Guice module with injectable
 * @Named parameters.
 *
 * Based on some CLI work by arb@google.com (Anthony Baxter).
 *
 *
 */
public class FlagBinder {

  private static final Set<Class<?>> supportedFlagTypes;

  static {
    supportedFlagTypes = new HashSet<Class<?>>();
    supportedFlagTypes.add(int.class);
    supportedFlagTypes.add(boolean.class);
    supportedFlagTypes.add(String.class);
  }

  /**
   * Parse command line arguments.
   *
   * @param args argv from command line
   * @return a Guice module configured with flag support.
   * @throws ParseException on bad command line args
   */
  public static Module parseFlags(String[] args, Class<?>... flagSettings) throws ParseException {
    Options options = new Options();

    List<Field> fields = new ArrayList<Field>();
    for (Class<?> settings : flagSettings) {
      fields.addAll(Arrays.asList(settings.getDeclaredFields()));
    }

    // Reflect on flagSettings class and absorb flags
    final Map<Flag, Field> flags = new LinkedHashMap<Flag, Field>();
    for (Field field : fields) {
      if (!field.isAnnotationPresent(Flag.class)) {
        continue;
      }

      // Validate target type
      if (!supportedFlagTypes.contains(field.getType())) {
        throw new IllegalArgumentException(field.getType()
            + " is not one of the supported flag types "
            + supportedFlagTypes);
      }

      Flag flag = field.getAnnotation(Flag.class);
      options.addOption(OptionBuilder.withLongOpt(flag.name())
          .withDescription(flag.description())
          .hasArg()
          .withArgName(flag.name().toUpperCase())
          .create());

      flags.put(flag, field);
    }

    // Parse up our cmd line
    CommandLineParser parser = new PosixParser();
    final CommandLine cmd = parser.parse(options, args);

    // Now validate them
    for (Flag flag : flags.keySet()) {
      if (flag.mandatory()) {
        String help = !"".equals(flag.description()) ? flag.description()
            : flag.name();
        mandatoryOption(cmd, flag.name(), "must supply " + help, options);
      }
    }

    // bundle everything up in an injectable guice module
    return new AbstractModule() {

      @Override
      protected void configure() {
        // We must iterate the flags a third time when binding.
        // Note: do not collapse these loops as that will damage
        // early error detection. The runtime is still O(n) in flag count.
        for (Map.Entry<Flag, Field> entry : flags.entrySet()) {
          Class<?> type = entry.getValue().getType();
          Flag flag = entry.getKey();

          // Skip non-mandatory, missing flags.
          if (!flag.mandatory()) {
            continue;
          }

          String flagValue = cmd.getOptionValue(flag.name());

          // Coerce String flag into target type.
          // NOTE(dhanji): only supported types are int, String and boolean.
          if (int.class.equals(type)) {
            bindConstant().annotatedWith(Names.named(flag.name()))
                .to(Integer.parseInt(flagValue));
          } else if (boolean.class.equals(type)) {
            bindConstant().annotatedWith(Names.named(flag.name()))
                .to(Boolean.parseBoolean(flagValue));
          } else {
            bindConstant().annotatedWith(Names.named(flag.name()))
                .to(flagValue);
          }
        }
      }
    };
  }

  /**
   * Checks a mandatory option is set, spits out help and dies if not.
   *
   * @param cmd        parsed options
   * @param option     the option to check
   * @param helpString the error message to emit if not.
   */
  static void mandatoryOption(CommandLine cmd, String option,
                              String helpString, Options options) {
    if (!cmd.hasOption(option)) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp(helpString, options);
      System.exit(1);
    }
  }
}

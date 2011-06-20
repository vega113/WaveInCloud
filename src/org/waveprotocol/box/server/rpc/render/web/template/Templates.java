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
package org.waveprotocol.box.server.rpc.render.web.template;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.MapMaker;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.servlet.ServletContext;

/**
 * Handles all our html templates, loading, parsing and processing them.
 * 
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
@Singleton
public class Templates {
  // Template file names go here.
  public static final String OUTER_TEMPLATE = "start.html.fragment";
  public static final String WAVELIST_TEMPLATE = "wavelist.html.fragment";
  public static final String AVATAR_TEMPLATE = "avatar.html.fragment";
  public static final String DIGEST_TEMPLATE = "digest.html.fragment";
  public static final String BLIP_TEMPLATE = "blip.html.fragment";
  public static final String ANCHOR_TEMPLATE = "anchor.html.fragment";
  public static final String HEADER_TEMPLATE = "header.html.fragment";
  public static final String FEED_TEMPLATE = "feed.html.fragment";
  public static final String PERMALINK_WAVE_TEMPLATE = "permalink_client.html";
  public static final String CLIENT_TEMPLATE = "full_client.html";
  public static final String MOBILE_TEMPLATE = "mobile_client.html";
  public static final String WAVE_NOT_FOUND_TEMPLATE = "wave_not_found.html.fragment";
  public static final String GA_FRAGMENT =
      "<script type=\"text/javascript\">"
          + "var gaJsHost = ((\"https:\" == document.location.protocol) ? \"https://ssl.\" : \"http://www.\");"
          + "document.write(unescape(\"%3Cscript src='\" + gaJsHost + \"google-analytics.com/ga.js' type='text/javascript'%3E%3C/script%3E\"));"
          + "</script>" + "<script type=\"text/javascript\">" + "try {"
          + "var pageTracker = _gat._getTracker(\"UA-13269470-9\");"
          + "pageTracker._trackPageview();" + "} catch(err) {}</script>";


  private static boolean PRODUCTION_MODE = false;

  private static final Logger log = Logger.getLogger(Templates.class.getName());

  public static String convertStreamToString(InputStream is) throws IOException {
    /*
     * To convert the InputStream to String we use the Reader.read(char[]
     * buffer) method. We iterate until the Reader return -1 which means there's
     * no more data to read. We use the StringWriter class to produce the
     * string.
     */
    if (is != null) {
      Writer writer = new StringWriter();

      char[] buffer = new char[1024];
      try {
        Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        int n;
        while ((n = reader.read(buffer)) != -1) {
          writer.write(buffer, 0, n);
        }
      } finally {
        is.close();
      }
      return writer.toString();
    } else {
      return "";
    }
  }

  public static String makeAnchorTag(String id) {
    return "<div class='anchor' id='" + id + "'></anchor>";
  }

  public static void insertIntoAnchor(String id, String inner, StringBuilder outer) {
    String anchorPrefix = "<div class='anchor' id='" + id + "'>";
    int offset = outer.indexOf(anchorPrefix);
    outer.insert(offset + anchorPrefix.length(), inner);
  }

  /**
   * file name of template -> compiled template lazy cache.
   */
  private final ConcurrentMap<String, String> templates = new MapMaker()
      .makeComputingMap(new Function<String, String>() {
        @Override
        public String apply(@Nullable String template) {
          return loadTemplate(template);
        }
      });

  private final Provider<ServletContext> servletContext;

  @Inject
  public Templates(Provider<ServletContext> servletContext) {
    this.servletContext = servletContext;
  }

  private String loadTemplate(String template) {
    // Load from jar if in production mode, otherwise servlet root.
    InputStream input = openResource(template);
    Preconditions.checkArgument(input != null, "Could not find template named: " + template);

    try {
      InputStream is = openResource(template);
      return convertStreamToString(is);
    } catch (IOException e) {
      log.warning(e.toString());
    }
    return "";
  }

  /**
   * Opens a packaged resource from the file system.
   * 
   * @param file The name of the file/resource to open.
   * @return An {@linkplain InputStream} to the named file, if found
   */
  public InputStream openResource(String file) {
    InputStream stream =
        PRODUCTION_MODE ? Templates.class.getResourceAsStream(file) : servletContext.get()
            .getResourceAsStream("/templates/" + file);

    // load + compile templates on-demand
    if (null == stream) {
      log.info("Could not find resource named: " + file);
    }
    return stream;
  }

  /**
   * Loads templates if necessary.
   * 
   * @param template Name of the template file. example: "blip.html.fragment"
   * @param context an object to process against
   * @return the processed, filled-in template.
   */
  public String process(String template, Object[] context) {
    // Reload template each time for development mode.
    String pattern = PRODUCTION_MODE ? templates.get(template) : loadTemplate(template);
    return MessageFormat.format(pattern, context);
  }

  public static void main(String[] args) {
    Templates templates = new Templates(null);
    String out = templates.process(BLIP_TEMPLATE, new String[] {"\'0\'", "\'1\'", "2", "3", "4"});
    System.out.println(out);
  }
}

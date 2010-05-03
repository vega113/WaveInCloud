// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.indexed;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Simple XML pull-parser.
 *
 * Only handles elements, text, minimal entities.
 * No CDATA, comments, etc.
 *
 * @author danilatos@google.com (Daniel Danilatos)
*
 */
public class SimpleXmlParser {

  /**
   * Supported item types
   */
  public enum ItemType {
    /** Start tag, including attributes */
    START_ELEMENT,
    /** End tag */
    END_ELEMENT,
    /** Non-empty text data */
    TEXT,
    /** At end of document */
    END
  }

  private final String attributeQuoteCharacter;
  private final String[] parts;
  private int position = -1;
  private ItemType currentType = null;
  private Map<String, String> attributes = new HashMap<String, String>();
  private String text = null;
  private String tagName = null;
  private boolean isSelfClosing = false;

  /**
   * NOTE(user): This attribute string is only here in case clients
   * need to get at the original string unescaped. Can be removed when no
   * longer needed, as it feels unnecessary to this simple interface.
   */
  private String attributeString = null;

  /**
   * @param content XML to be parsed
   */
  public SimpleXmlParser(String content) {
    this(content, "\"");
  }

  /**
   * Optional constructor to allow alternative attribute quote delimeters to be
   * used.
   *
   * @param content XML to be parsed
   * @param attributeQuoteCharacter character class used to determine attribute
   *     boundaries.
   */
  public SimpleXmlParser(String content, String attributeQuoteCharacter) {
    this.parts = content.split("[<>]", -1);
    this.attributeQuoteCharacter = attributeQuoteCharacter;
  }

  /**
   * Move to the next token
   * @return The type of item we are at
   */
  public ItemType next() {

    if (currentType == ItemType.START_ELEMENT && isSelfClosing) {
      currentType = ItemType.END_ELEMENT;
      return currentType;
    }

    position++;

    if (position >= parts.length) {
      currentType = ItemType.END;
      return currentType;
    } else if (position % 2 != 0) {
      String elementString = parts[position];

      if (elementString.startsWith("/")) {
        currentType = ItemType.END_ELEMENT;
        if (elementString.length() < 2) {
          throw new RuntimeException("invalid XML: missing element name");
        }
        tagName = elementString.substring(1);

      } else if (elementString.startsWith("?")) {
        throw new UnsupportedOperationException("XML processing instructions are not supported");

      } else {
        currentType = ItemType.START_ELEMENT;
        isSelfClosing = elementString.endsWith("/");
        if (isSelfClosing) {
          elementString = elementString.substring(0, elementString.length() - 1);
        }
        if (elementString.length() < 1) {
          throw new RuntimeException("invalid XML: missing element name");
        }
        String[] elementParts = elementString.split("\\s", 2);
        tagName = elementParts[0];
        if (elementParts.length == 2) {
          attributeString = elementParts[1];
          attributes = parseAttributes(elementParts[1], attributeQuoteCharacter);
        } else {
          attributeString = "";
          attributes = Collections.<String, String>emptyMap();
        }
      }

    } else {
      if (parts[position].length() == 0) {
        return next();
      }

      currentType = ItemType.TEXT;

      text = unescape(parts[position]);
    }

    return currentType;
  }

  /**
   * @return Type of item at current position
   */
  public ItemType getCurrentType() {
    return currentType;
  }

  /**
   * @return character data at current position; only valid
   *   when we are at a text item
   */
  public String getText() {
    if (currentType != ItemType.TEXT) {
      throw new IllegalStateException("Not at text");
    }

    return text;
  }

  /**
   * @return tag name for current start element; not valid over
   *   other items
   */
  public String getTagName() {
    if (currentType != ItemType.START_ELEMENT && currentType != ItemType.END_ELEMENT) {
      throw new IllegalStateException("Not at start/end element");
    }

    return tagName;
  }

  /**
   * Returns whether an element is self-closing (ie &lt;tag/&rt;). An exception
   * is thrown if this check is attempted on an element that is not a start
   * element.
   *
   * @return true if the element is self closing, false otherwise.
   */
  public boolean isSelfClosing() {
    if (currentType != ItemType.START_ELEMENT) {
      throw new IllegalStateException("Not at start element");
    }
    return isSelfClosing;
  }

  /**
   * @return attributes for current start element; not valid over
   *   other items
   */
  public Map<String, String> getAttributes() {
    if (currentType != ItemType.START_ELEMENT) {
      throw new IllegalStateException("Not at start element");
    }

    return attributes;
  }

  /**
   * @return the original unescaped and unparsed attribute string.
   */
  public String getOriginalAttributeString() {
    if (currentType != ItemType.START_ELEMENT) {
      throw new IllegalStateException("Not at start element");
    }

    return attributeString;
  }

  /**
   * Split a |str| at occurrences of a separator |c|.  The difference between this and
   * String.split(String) is that this will return empty strings between sequences of
   * the separator string, whereas String.split(String) will skip the whole sequence
   * as if it were one atomic separator.
   *
   * @param str the string to split
   * @param c the string to split by
   * @return an array of the split parts
   */
  public static String[] split(String str, String c) {
    ArrayList<String> elms = new ArrayList<String>();
    int lastIndex = 0;
    while (true) {
      int nextIndex = str.indexOf(c, lastIndex);
      if (nextIndex == -1) {
        break;
      }
      elms.add(str.substring(lastIndex, nextIndex));
      lastIndex = nextIndex + c.length();
    }
    if (lastIndex < str.length()) {
      elms.add(str.substring(lastIndex, str.length()));
    }
    return elms.toArray(new String[elms.size()]);
  }

  /**
   * Parses the attributes of an element.
   *
   * @param attributesString The string containing the attributes to parse.
   * @param quote The regex classed used to determine a quote or attribute
   *     delimeter.
   * @return A map mapping attribute names to attribute values.
   */
  private static Map<String,String> parseAttributes(String attributesString, String quote) {
    Map<String,String> attributes = new TreeMap<String,String>();

    String[] parts = split(attributesString, quote);
    for (int i = 0; i < parts.length - 1;) {
      String name = parts[i++];
      name = name.substring(0, name.indexOf('=')).trim();
      String value = unescape(parts[i++]);
      attributes.put(name, value);
    }
    return attributes;
  }

  /**
   * Unescapes the XML entity references in a string.
   *
   * @param escapedString The string to unescape.
   * @return The unescaped string.
   */
  private static String unescape(String escapedString) {
    // NOTE(user): This does not handle numeric character references.
    return escapedString.replaceAll("&lt;", "<").replaceAll("&gt;", ">")
        .replaceAll("&apos;", "'").replaceAll("&quot;", "\"").replaceAll("&amp;", "&");
  }

}

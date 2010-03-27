// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.util;

import org.waveprotocol.wave.model.document.ReadableDocument;
import org.waveprotocol.wave.model.document.raw.impl.Element;
import org.waveprotocol.wave.model.document.raw.impl.Node;
import org.waveprotocol.wave.model.document.raw.impl.Text;

/**
 * Implements style accessors for POJO elements.
 *
*
 */
public class RawElementStyleView extends IdentityView<Node, Element, Text>
    implements ElementStyleView<Node, Element, Text> {
  /**
   * Constructor.
   * @param inner
   */
  public RawElementStyleView(ReadableDocument<Node, Element, Text> inner) {
    super(inner);
  }

  /** {@inheritDoc} */
  @Override
  public String getStylePropertyValue(Element element, String name) {
    // TODO(user): This is a highly non-optimal solution, but this is just for
    // testing. Did not want to pollute the raw Element class with this.
    String styles = element.getAttribute("style");
    if (styles != null && styles.contains(name)) {
      for (String stylePair : styles.split(";")) {
        int index = stylePair.indexOf(':');
        if (index >= 0 && index < stylePair.length() - 1) {
          String key = stylePair.substring(0, index).trim();
          String value = stylePair.substring(index + 1);
          if (key.equalsIgnoreCase(name)) {
            return value.trim();
          }
        }
      }
    }
    return null;
  }
}

// Copyright 2008 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.raw;

import org.waveprotocol.wave.model.document.indexed.SimpleXmlParser;
import org.waveprotocol.wave.model.document.indexed.SimpleXmlParser.ItemType;
import org.waveprotocol.wave.model.document.util.DocumentParser;

/**
 * Parses a string into a RawDocument
 *
 * @author danilatos@google.com (Daniel Danilatos)
*
 */
public class RawDocumentParserImpl<N, E extends N, T extends N, D extends RawDocument<N, E, T>>
    implements DocumentParser<D> {

  private final RawDocument.Factory<D> factory;

  /**
   * Creates a parser that uses a particular builder.
   *
   * @param factory  builder to use when parsing
   * @return a new parser.
   */
  public static <N, E extends N, T extends N, D extends RawDocument<N, E, T>>
      RawDocumentParserImpl<N, E, T, D> create(RawDocument.Factory<D> factory) {
    return new RawDocumentParserImpl<N, E, T, D>(factory);
  }

  /**
   * Creates a parser.
   *
   * @param factory  builder to use
   */
  private RawDocumentParserImpl(RawDocument.Factory<D> factory) {
    this.factory = factory;
  }

  /**
   * @param xmlString
   * @return parsed string
   */
  public D parse(String xmlString) {
    SimpleXmlParser parser = new SimpleXmlParser(xmlString);
    // TODO(ohler): This can be an infinite loop.  Fix that.
    while (parser.getCurrentType() != ItemType.START_ELEMENT) {
      parser.next();
    }

    D document = factory.create(parser.getTagName(), parser.getAttributes());
    parseChildren(parser, document, document.getDocumentElement());

    return document;
  }

  /**
   * Parses an element.
   *
   * @param parser  tokenizer
   * @param parentElement the parent element to attach the parsed node to
   * @return a new element.
   */
  private E parseElement(SimpleXmlParser parser, D doc, E parentElement) {
    E element = doc.createElement(parser.getTagName(), parser.getAttributes(),
        parentElement, null);
    parseChildren(parser, doc, element);
    return element;
  }

  private void parseChildren(SimpleXmlParser parser, D doc, E element) {
    boolean done = false;
    do {
      N child = null;
      parser.next();
      switch (parser.getCurrentType()) {
        case TEXT:
          child = parseText(parser, doc, element);
          break;
        case START_ELEMENT:
          child = parseElement(parser, doc, element);
          break;
        case END_ELEMENT:
          done = true;
          break;
        case END:
          // This is a bit of judgment call. If this happens, the document is
          // invalid, since the closing tag is missing. By setting done to true
          // we're silently repairing the invalid doc.
          done = true;
          break;
      }
      if (child != null) {
        doc.insertBefore(element, child, null);
      }
    } while (!done);
  }

  /**
   * Parses a text node.
   *
   * @param parser  tokenizer
   * @param parentElement the parent element to attach the parsed node to
   * @return a new text node.
   */
  private T parseText(SimpleXmlParser parser, D doc, E parentElement) {
    String text = parser.getText();
    T child = null;
    if (text.length() > 0) {
      child = doc.createTextNode(text, parentElement, null);
    }
    return child;
  }
}

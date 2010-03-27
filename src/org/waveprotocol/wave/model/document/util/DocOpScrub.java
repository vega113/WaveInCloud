// Copyright 2010 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.util;

import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMap;
import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMapBuilder;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.AttributesUpdate;
import org.waveprotocol.wave.model.document.operation.BufferedDocOp;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.DocOpCursor;
import org.waveprotocol.wave.model.document.operation.impl.AttributesImpl;
import org.waveprotocol.wave.model.document.operation.impl.AttributesUpdateImpl;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuffer;
import org.waveprotocol.wave.model.document.operation.util.ImmutableStateMap.Attribute;
import org.waveprotocol.wave.model.document.operation.util.ImmutableUpdateMap.AttributeUpdate;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.Utf16Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility for scrubbing user-sensitive information out of operations.
 *
 * It is not intended to make reverse-engineering the original content totally
 * impossible, given enough time and effort. It is meant to prevent
 * casual/accidental viewing of user data in logs, while still preserving
 * valuable debugging information, such as the type of characters in a string
 * (so we can tell, for example, if the error is related to CJK input or not).
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public final class DocOpScrub {

  /**
   * Number of leading characters to avoid scrubbing in cases where we do
   * not scrub the entire string
   */
  static final int CHARS_TO_LEAVE = 3;

  static final char PSI = 0x3c8, JIA = 0x42f, ARMENIAN = 0x554, WO = 0x6211;

  /**
   * Creates a scrubbed copy of the given operation.
   *
   * @param op does not have to be well formed. invalid characters will be
   *        clearly noted.
   * @return the output will be well formed only if the input was well formed
   */
  public static BufferedDocOp scrub(DocOp op) {
    final DocOpBuffer b = new DocOpBuffer();

    op.apply(new DocOpCursor() {
      @Override
      public void deleteCharacters(String chars) {
        b.deleteCharacters(scrubString(chars));
      }

      @Override
      public void deleteElementEnd() {
        b.deleteElementEnd();
      }

      @Override
      public void deleteElementStart(String type, Attributes attrs) {
        b.deleteElementStart(type, scrubAttributes(attrs));
      }

      @Override
      public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
        b.replaceAttributes(scrubAttributes(oldAttrs), scrubAttributes(newAttrs));
      }

      @Override
      public void retain(int itemCount) {
        b.retain(itemCount);
      }

      @Override
      public void updateAttributes(AttributesUpdate attrUpdate) {
        b.updateAttributes(scrubAttributesUpdate(attrUpdate));
      }

      @Override
      public void annotationBoundary(AnnotationBoundaryMap map) {
        b.annotationBoundary(scrubAnnotationBoundary(map));
      }

      @Override
      public void characters(String chars) {
        b.characters(scrubString(chars));
      }

      @Override
      public void elementEnd() {
        b.elementEnd();
      }

      @Override
      public void elementStart(String type, Attributes attrs) {
        b.elementStart(type, scrubAttributes(attrs));
      }
    });

    return b.finishUnchecked();
  }

  public static AnnotationBoundaryMap scrubAnnotationBoundary(AnnotationBoundaryMap unscrubbed) {
    AnnotationBoundaryMapBuilder b = new AnnotationBoundaryMapBuilder();
    for (int i = 0; i < unscrubbed.endSize(); i++) {
      b.end(scrubMostAnnotationKey(unscrubbed.getEndKey(i)));
    }
    for (int i = 0; i < unscrubbed.changeSize(); i++) {
      b.change(scrubMostAnnotationKey(unscrubbed.getChangeKey(i)),
          scrubMostString(unscrubbed.getOldValue(i)),
          scrubMostString(unscrubbed.getNewValue(i)));
    }
    return b.build();
  }

  public static Attributes scrubAttributes(Attributes unscrubbed) {
    List<Attribute> list = new ArrayList<Attribute>();
    for (Map.Entry<String, String> entry : unscrubbed.entrySet()) {
      list.add(new Attribute(scrubMostString(entry.getKey()), scrubMostString(entry.getValue())));
    }
    return AttributesImpl.fromUnsortedAttributes(list);
  }

  public static AttributesUpdate scrubAttributesUpdate(AttributesUpdate unscrubbed) {
    List<AttributeUpdate> list = new ArrayList<AttributeUpdate>();
    int size = unscrubbed.changeSize();
    for (int i = 0; i < size; i++) {
      list.add(new AttributeUpdate(scrubMostString(unscrubbed.getChangeKey(i)),
          scrubMostString(unscrubbed.getOldValue(i)),
          scrubMostString(unscrubbed.getNewValue(i))));
    }
    return AttributesUpdateImpl.fromUnsortedUpdates(list);
  }

  /**
   * Scrubs most of an annotation key. Uses {@link #scrubMostString(String)} on
   * each slash (/) separated component of the key, preserving the original
   * slashes.
   *
   * @param unscrubbed key
   * @return mostly scrubbed key
   */
  public static String scrubMostAnnotationKey(String unscrubbed) {
    // pass -1 to split to prevent dropping trailing empty strings
    String[] parts = unscrubbed.split("/", -1);
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < parts.length; i++) {
      parts[i] = scrubMostString(parts[i]);
    }
    return CollectionUtils.join('/', parts);
  }

  /**
   * Scrubs most of a string based on a heuristic that attempts to clean out
   * user sensitive data while still retaining some amount of information that
   * would be useful for debugging.
   *
   * If the string looks a bit like an email address, then everything but the @
   * symbol is scrubbed. Otherwise, everything but the first few characters is
   * scrubbed. In any case, invalid characters are always scrubbed for
   * readability. The motivation in the case of email addresses is both to make
   * it clear that the piece of data is an email address, and to be a little
   * stronger about removing the identifiable information
   *
   * NOTE(danilatos): Consider also always scrubbing confusing-to-print
   * characters such as RTL or ligature forming characters?
   *
   * @param unscrubbed unscrubbed string. may be null, in which case null is
   *        returned.
   * @return mostly scrubbed string
   */
  public static String scrubMostString(String unscrubbed) {
    if (unscrubbed == null) {
      return null;
    }

    int index = unscrubbed.indexOf('@');
    if (index != -1 && unscrubbed.lastIndexOf('@') == index) {
      // If it looks vaguely like an email address (contains a single '@'),
      // then scrub everything except for the '@' symbol.
      // pass 2 to split to prevent dropping of trailing empty strings
      String[] parts = unscrubbed.split("@", 2);
      return scrubString(parts[0]) + '@' + scrubString(parts[1]);
    } else if (unscrubbed.length() >= CHARS_TO_LEAVE){
      // Otherwise scrub everything but the first few characters,
      // which we leave to aid debugging.
      return
          scrubString(unscrubbed.substring(0, CHARS_TO_LEAVE), false) +
          scrubString(unscrubbed.substring(CHARS_TO_LEAVE), true);
    } else {
      return unscrubbed;
    }
  }

  public static String scrubString(String unscrubbed) {
    return scrubString(unscrubbed, true);
  }

  static String scrubString(String unscrubbed, boolean scrubValidChars) {
    char[] chars = new char[unscrubbed.length()];
    for (int i = 0; i < unscrubbed.length(); i++) {
      chars[i] = scrubChar(unscrubbed.charAt(i), scrubValidChars);
    }
    return new String(chars);
  }

  static char scrubChar(char c, boolean scrubValid) {

    assert Utf16Util.isCodePoint(c) : "isCodePoint() should always be true for char";

    if (Utf16Util.isSurrogate(c)) {
      // High surrogate comes first. Matching pairs should appear as "<>"
      if (Utf16Util.isHighSurrogate(c)) {
        return '<';
      } else {
        assert Utf16Util.isLowSurrogate(c);
        return '>';
      }
    }

    switch (Utf16Util.isCodePointGoodForBlip(c)) {
    case OK:
      return scrubValid ? scrubValidChar(c) : c;
    case BIDI:
      return '|';
    case CONTROL:
      return '^';
    case NONCHARACTER:
      return '!';
    default:
      // Other invalid characters
      return '#';
    }
  }

  private static char scrubValidChar(char c) {
    assert c >= 0x20;

    // Reference: http://www.alanwood.net/unicode/fontsbyrange.html#u0180
    // The ranges are approximate, some encompass more than they describe.
    if (c <= 0x7f) { // Basic Latin
      return 'a';
    } else if (c <= 0xff) { // Latin-1 Supplement
      return 'b';
    } else if (c <= 0x17f) { // Latin Extended-A
      return 'c';
    } else if (c <= 0x24f) { // Latin Extended-B
      return 'd';
    } else if (0x2c60 <= c && c <= 0x2c7f) { // Latin Extended-C
      return 'e';
    } else if (0xa720 <= c && c <= 0xa7ff) { // Latin Extended-D
      return 'f';
    } else if (c <= 0x2AF) { // IPA Extensions
      return 'I';
    } else if (c <= 0x2FF) { // Spacing modifier letters
      return 'S';
    } else if (c <= 0x36f) { // Combining diacritical marks
      return ':';
    } else if (c <= 0x3FF) { // Greek and coptic
      return PSI;
    } else if (c <= 0x52F) { // Cyrillic & Cyrillic supplement
      return JIA;
    } else if (c <= 0x58f) { // Armenian
      return ARMENIAN;
    } else if (c <= 0x5ff) { // Hebrew
      return 'H';
    } else if (c <= 0x6ff) { // Arabic
      return 'A';
    } else if (0x900 <= c && c <= 0x97f) { // Devangari
      return 'D';
    } else if (0xe00 <= c && c <= 0xe7f) { // Thai
      return 'T';
    } else if (0x1100 <= c && c <= 0x11ff) { // Some Hangul Jamo
      return 'K';
    } else if (0x20a0 <= c && c <= 0x2bff) { // Some symbol type things
      return '%';
    } else if (0x2e80 <= c && c <= 0x2eff ||
               0x3000 <= c && c <= 0x303f ||
               0x3200 <= c && c <= 0x9fff) { // Some CJK
      return WO;
    } else if (0x3040 <= c && c <= 0x30ff) { // Some Japanese
      return 'J';
    } else { // TODO others
      return '?';
    }
  }

  private DocOpScrub() {}
}

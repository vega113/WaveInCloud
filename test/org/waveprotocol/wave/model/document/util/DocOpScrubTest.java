// Copyright 2010 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.util;

import static org.waveprotocol.wave.model.document.util.DocOpScrub.CHARS_TO_LEAVE;
import static org.waveprotocol.wave.model.document.util.DocOpScrub.JIA;
import static org.waveprotocol.wave.model.document.util.DocOpScrub.PSI;
import static org.waveprotocol.wave.model.document.util.DocOpScrub.WO;


import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMap;
import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMapBuilder;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.AttributesUpdate;
import org.waveprotocol.wave.model.document.operation.BufferedDocOp;
import org.waveprotocol.wave.model.document.operation.impl.AttributesImpl;
import org.waveprotocol.wave.model.document.operation.impl.AttributesUpdateImpl;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuilder;
import org.waveprotocol.wave.model.operation.OpComparators;

/**
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class DocOpScrubTest extends TestCase {

  // Sprinkling from a few random character ranges
  static final String VALID = " abcDEF^#!(_+90" +
      "\u03e0\u042e\u6202\u05f3\u0610\u0710\u3045\u00ee\u0175\u0a00";
  static final String VALID_SCRUBBED = "aaaaaaaaaaaaaaa" + PSI + JIA + WO + "HA?Jbc?";

  static final String INVALID = "\u200e\u200f\u202a\u202e\u0000\u001f\u007f\u009f\u206a\u206f" +
      "\ud800\udbff\udc00\udfff\uffff\ufffe\ufdd0\ufdef";
  static final String INVALID_SCRUBBED = "||||^^^^##<<>>!!!!";

  static final String BOTH = VALID + INVALID;
  static final String BOTH_SCRUBBED = VALID_SCRUBBED + INVALID_SCRUBBED;

  static final String VALID_PARTIAL = leaveInitial(VALID, VALID_SCRUBBED);
  static final String INVALID_PARTIAL = INVALID_SCRUBBED; // Always scrub invalid characters

  static final Attributes ATTRS = new AttributesImpl(
      VALID, VALID + "x",
      INVALID, INVALID + "y",
      "joe.bloe@example.com", "john.smith@foobar.net",
      "@", "@x",
      "a@b", "c@d",
      "a@@@b@@@c", "d@@e",
      "b", "bb",
      "", "");

  static final Attributes ATTRS_SCRUBBED = new AttributesImpl(
      VALID_PARTIAL, VALID_PARTIAL +"a",
      INVALID_PARTIAL, INVALID_PARTIAL + "a",
      "aaaaaaaa@aaaaaaaaaaa", "aaaaaaaaaa@aaaaaaaaaa",
      "@", "@a",
      "a@a", "a@a",
      "a@@aaaaaa", "d@@a",
      "b", "bb",
      "", "");

  static final AttributesUpdate ATTRSUP = new AttributesUpdateImpl(
      VALID, VALID + "w", VALID + "x",
      INVALID, INVALID + "y", INVALID + "z",
      "joe.bloe@example.com", "john.smith@foobar.net", "joe.shmoe@fubar.org",
      "a", null, "x@",
      "@", "@x", null,
      "a@b", "c@d", "e@f",
      "a@@@b@@@c", "d@@e", "@@",
      "b", "bb", "ccc",
      "", "", "");

  static final AttributesUpdate ATTRSUP_SCRUBBED = new AttributesUpdateImpl(
      VALID_PARTIAL, VALID_PARTIAL + "a", VALID_PARTIAL + "a",
      INVALID_PARTIAL, INVALID_PARTIAL + "a", INVALID_PARTIAL + "a",
      "aaaaaaaa@aaaaaaaaaaa", "aaaaaaaaaa@aaaaaaaaaa", "aaaaaaaaa@aaaaaaaaa",
      "a", null, "a@",
      "@", "@a", null,
      "a@a", "a@a", "a@a",
      "a@@aaaaaa", "d@@a", "@@",
      "b", "bb", "ccc",
      "", "", "");

  static final AnnotationBoundaryMap ANNO = new AnnotationBoundaryMapBuilder()
      .change("x", "y", VALID)
      .change("x", null, INVALID)
      .change("", "", null)
      .change("/qwerty", "/qwerty/", "//qwert/y")
      .change(VALID + "/" + INVALID + "/c@@b//c@b/johno@example.com", "x", "y")
      .end(VALID + "/" + INVALID + "/c@@b/c@b/johno@example.com/")
      .build();

  static final AnnotationBoundaryMap ANNO_SCRUBBED = new AnnotationBoundaryMapBuilder()
      .change("x", "y", VALID_SCRUBBED)
      .change("x", null, INVALID_PARTIAL)
      .change("", "", null)
      .change("/qweaaa", "/qwaaaaa", "//qaaaaaa") // slashes only meaningful for keys
      .change(VALID_PARTIAL + "/" + INVALID_PARTIAL + "/c@@a//a@a/aaaaa@aaaaaaaaaaa", "x", "y")
      .end(VALID_PARTIAL + "/" + INVALID_PARTIAL + "/c@@a/a@a/aaaaa@aaaaaaaaaaa/")
      .build();

  public void testScrubs() {
    DocOpBuilder b1 = new DocOpBuilder();
    DocOpBuilder b2 = new DocOpBuilder();

    b1.characters(VALID);
    b2.characters(VALID_SCRUBBED);

    b1.deleteCharacters(VALID);
    b2.deleteCharacters(VALID_SCRUBBED);

    b1.characters(INVALID);
    b2.characters(INVALID_SCRUBBED);

    b1.deleteCharacters(INVALID);
    b2.deleteCharacters(INVALID_SCRUBBED);

    b1.elementStart("abc", ATTRS);
    b2.elementStart("abc", ATTRS_SCRUBBED);

    b1.elementEnd();
    b2.elementEnd();

    b1.deleteElementStart("abc", ATTRS);
    b2.deleteElementStart("abc", ATTRS_SCRUBBED);

    b1.deleteElementEnd();
    b2.deleteElementEnd();

    b1.retain(5);
    b2.retain(5);

    b1.replaceAttributes(ATTRS, ATTRS);
    b2.replaceAttributes(ATTRS_SCRUBBED, ATTRS_SCRUBBED);

    b1.updateAttributes(ATTRSUP);
    b2.updateAttributes(ATTRSUP_SCRUBBED);

    b1.annotationBoundary(ANNO);
    b2.annotationBoundary(ANNO_SCRUBBED);

    BufferedDocOp o1 = DocOpScrub.scrub(b1.buildUnchecked());
    BufferedDocOp o2 = b2.buildUnchecked();
    assertTrue("\n" + o1 + "\n but expected \n" + o2,
        OpComparators.SYNTACTIC_IDENTITY.equal(o1, o2));
  }

  static String leaveInitial(String original, String scrubbed) {
    return original.substring(0, DocOpScrub.CHARS_TO_LEAVE) + scrubbed.substring(CHARS_TO_LEAVE);
  }
}

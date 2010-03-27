// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.util;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.util.ElementHandlerRegistry.HasHandlers;

/**
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class ElementHandlerRegistryTest extends TestCase {

  static class H1 extends Object {}
  static class H2 extends Object {}

  private final H1 a = new H1();
  private final H1 b = new H1();
  private final H1 c = new H1();
  private final H1 d = new H1();

  private final H2 e = new H2();
  private final H2 f = new H2();
  private final H2 g = new H2();
  private final H2 h = new H2();

  private final HasHandlers el1 = mock(HasHandlers.class);
  private final HasHandlers el2 = mock(HasHandlers.class);
  private final HasHandlers el3 = mock(HasHandlers.class);
  {
    when(el1.getTagName()).thenReturn("x:y");
    when(el2.getTagName()).thenReturn("x:z");
    when(el3.getTagName()).thenReturn("x:a");
  }


  public void testRegister() {
    ElementHandlerRegistry r1 = ElementHandlerRegistry.ROOT.createExtension();

    r1.register(H1.class, "x", "y", null, a);
    r1.register(H2.class, "x", "z", null, f);
    assertSame(a, r1.getHandler(el1, null, H1.class));
    assertSame(f, r1.getHandler(el2, null, H2.class));

    ElementHandlerRegistry r2 = r1.createExtension();

    // Check overriding in the same registry
    r1.register(H2.class, "x", "z", null, h);
    assertSame(h, r1.getHandler(el2, null, H2.class));

    // Check overriding in a child registry
    r2.register(H1.class, "x", "y", null, b);
    r2.register(H2.class, "x", "z", null, g);
    assertSame(b, r2.getHandler(el1, null, H1.class));
    assertSame(g, r2.getHandler(el2, null, H2.class));

    // Check propagation
    r1.register(H1.class, "x", "a", null, c);
    assertSame(c, r2.getHandler(el3, null, H1.class));
  }

  public void testConcurrent() {
    ElementHandlerRegistry r1 = ElementHandlerRegistry.ROOT.createExtension();
    ElementHandlerRegistry r2 = r1.createExtension();

    // Check overriding in the child registry with concurrent propagation
    r2.register(H1.class, "x", "y", null, a);
    r2.register(H1.class, "x", "y", null, b);
    r1.register(H1.class, "x", "y", null, c);
    r1.register(H1.class, "x", "y", null, d);
    assertSame(b, r2.getHandler(el1, null, H1.class));

  }

  public void testOverrideDifferentTypes() {
    ElementHandlerRegistry r1 = ElementHandlerRegistry.ROOT.createExtension();

    r1.register(H1.class, "x", "y", null, a);
    r1.register(H2.class, "x", "y", null, e);

    ElementHandlerRegistry r2 = r1.createExtension();

    // Check overriding in the same registry
    r2.register(H2.class, "x", "y", null, h);
    assertSame(h, r2.getHandler(el1, null, H2.class));
    assertSame(a, r2.getHandler(el1, null, H1.class));

  }
}

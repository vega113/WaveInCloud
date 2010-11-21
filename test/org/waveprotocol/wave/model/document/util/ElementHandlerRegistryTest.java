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
    when(el1.getTagName()).thenReturn("x");
    when(el2.getTagName()).thenReturn("y");
    when(el3.getTagName()).thenReturn("z");
  }


  public void testRegister() {
    ElementHandlerRegistry r1 = ElementHandlerRegistry.ROOT.createExtension();

    r1.register(H1.class, "x", a);
    r1.register(H2.class, "y", f);
    assertSame(a, r1.getHandler(el1, H1.class));
    assertSame(f, r1.getHandler(el2, H2.class));

    ElementHandlerRegistry r2 = r1.createExtension();

    // Check overriding in the same registry
    r1.register(H2.class, "y", h);
    assertSame(h, r1.getHandler(el2, H2.class));

    // Check overriding in a child registry
    r2.register(H1.class, "x", b);
    r2.register(H2.class, "y", g);
    assertSame(b, r2.getHandler(el1, H1.class));
    assertSame(g, r2.getHandler(el2, H2.class));

    // Check propagation
    r1.register(H1.class, "z", c);
    assertSame(c, r2.getHandler(el3, H1.class));
  }

  public void testConcurrent() {
    ElementHandlerRegistry r1 = ElementHandlerRegistry.ROOT.createExtension();
    ElementHandlerRegistry r2 = r1.createExtension();

    // Check overriding in the child registry with concurrent propagation
    r2.register(H1.class, "x", a);
    r2.register(H1.class, "x", b);
    r1.register(H1.class, "x", c);
    r1.register(H1.class, "x", d);
    assertSame(b, r2.getHandler(el1, H1.class));

  }

  public void testOverrideDifferentTypes() {
    ElementHandlerRegistry r1 = ElementHandlerRegistry.ROOT.createExtension();

    r1.register(H1.class, "x", a);
    r1.register(H2.class, "x", e);

    ElementHandlerRegistry r2 = r1.createExtension();

    // Check overriding in the same registry
    r2.register(H2.class, "x", h);
    assertSame(h, r2.getHandler(el1, H2.class));
    assertSame(a, r2.getHandler(el1, H1.class));

  }
}

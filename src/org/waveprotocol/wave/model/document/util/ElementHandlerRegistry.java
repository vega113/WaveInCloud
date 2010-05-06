// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.util;

import org.waveprotocol.wave.model.util.ChainedData;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.DataDomain;
import org.waveprotocol.wave.model.util.IdentityMap;
import org.waveprotocol.wave.model.util.StringMap;

/**
 * Registry for different types of handlers for document elements, based
 * on a match condition. Currently just tag name plus special attribute
 * matching is supported.
 *
 * There is a limitation in this approach, namely that handler subclasses
 * are not recognised - so, if you call getHandler to get a handler, you will
 * only get a result when passing in the exact .class with which it was
 * registered. For example, if you subclass Renderer, you'll probably need
 * to register the renderer twice, both with Renderer.class and YourRenderer.class,
 * or do type casting on the return value from Renderer.class.
 *
 * TODO(danilatos): Conceive of a nicer registering mechanism than class plus
 * tagname plus attribute
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class ElementHandlerRegistry {

  /**
   * The minimum interface required by the registry to identify an element in
   * order to ascertain look up its handlers.
   */
  public interface HasHandlers {

    String getTagName();
  }

  private static class HandlerData {
    /**
     * Our map of handlers for different element types
     */
    StringMap<IdentityMap<Class<Object>, Object>> handlers = CollectionUtils.createStringMap();
  }

  private static final DataDomain<HandlerData, HandlerData> handlerDataDomain =
      new DataDomain<HandlerData, HandlerData>() {
        @Override
        public void compose(HandlerData target, HandlerData changes, HandlerData base) {
          target.handlers.clear();
          copyInto(target, base);
          copyInto(target, changes);
        }

        private void copyInto(final HandlerData target, HandlerData source) {
          source.handlers.each(new StringMap.ProcV<IdentityMap<Class<Object>, Object>>() {
            @Override
            public void apply(final String key,
                IdentityMap<Class<Object>, Object> sourceElemHandlers) {

              final IdentityMap<Class<Object>, Object> destElementHandlers =
                  target.handlers.containsKey(key)
                      ? target.handlers.get(key)
                      : CollectionUtils.<Class<Object>, Object>createIdentityMap();

              sourceElemHandlers.each(new IdentityMap.ProcV<Class<Object>, Object>() {
                public void apply(Class<Object> key, Object handler) {
                  destElementHandlers.put(key, handler);
                }
              });

              target.handlers.put(key, destElementHandlers);
            }
          });
        }

        @Override
        public HandlerData empty() {
          return new HandlerData();
        }

        @Override
        public HandlerData readOnlyView(HandlerData modifiable) {
          return modifiable;
        }
      };

  /**
   * The singleton registry that, as the terminal of the priority chain, is
   * always consulted last by any registry.
   */
  public static final ElementHandlerRegistry ROOT = new ElementHandlerRegistry();

  private final ChainedData<HandlerData, HandlerData> data;

  private ElementHandlerRegistry() {
    data = new ChainedData<HandlerData, HandlerData>(handlerDataDomain);
  }

  /**
   * @param parent  parent registry in the chain
   */
  private ElementHandlerRegistry(ElementHandlerRegistry parent) {
    data = new ChainedData<HandlerData, HandlerData>(parent.data);
  }

  /**
   * @return a chained child
   */
  public ElementHandlerRegistry createExtension() {
    return new ElementHandlerRegistry(this);
  }


  /**
   * Registers a handler.
   *
   * @param <T> The handler type
   * @param handlerType The .class of the handler type
   * @param ns Element namespace. May be null
   * @param tag Element tag name
   * @param typeAttr Special type attribute (currently "_t"). May be null.
   * @param handler The handler to register
   */
  @SuppressWarnings("unchecked")
  public <T> void register(Class<T> handlerType, String ns, String tag, String typeAttr,
      T handler) {

    String id = getElementIdentifier(ns, tag, typeAttr);
    IdentityMap<Class<Object>, Object> handlersForElement = data.modify().handlers.get(id);
    if (handlersForElement == null) {
      handlersForElement = CollectionUtils.createIdentityMap();
      data.modify().handlers.put(id, handlersForElement);
    }

    handlersForElement.put((Class<Object>) handlerType, (Object)handler);
  }

  /**
   * Get the handler for the given element.
   *
   * @param element The element
   * @param typeAttrVal additional subtype attribute value
   * @param handlerType The .class of the handler
   * @return The handler instance, or null if none available
   */
  @SuppressWarnings("unchecked")
  public <T> T getHandler(HasHandlers element, String typeAttrVal, Class<T> handlerType) {
    String id = element.getTagName();
    if (typeAttrVal != null) {
      id += "." + typeAttrVal;
    }
    IdentityMap<Class<Object>, Object> handlersForElement = data.inspect().handlers.get(id);
    if (handlersForElement == null) {
      return null;
    }
    return (T) handlersForElement.get((Class<Object>) handlerType);
  }

  private String getElementIdentifier(String ns, String tag, String typeAttr) {
    return (ns != null ? ns + ":" : "") + tag + (typeAttr != null ? "." + typeAttr : "");
  }

}

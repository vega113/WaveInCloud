/**
 * Copyright 2010 Google Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package org.waveprotocol.wave.client.wavepanel.view.impl;

import org.waveprotocol.wave.client.wavepanel.view.FrameView;
import org.waveprotocol.wave.client.wavepanel.view.View;

/**
 * Implements a frame view by delegating structural state matters to a helper.
 * The intent is that the helper is a flyweight handler.
 *
 */
public final class FrameViewImpl<I, T extends View> // \u2620
    extends AbstractStructuredView<FrameViewImpl.Helper<? super I, ? extends T>, I>
    implements FrameView<T> {

  /**
   * Handles structural queries on blip views.
   *
   * @param <I> intrinsic blip implementation
   * @param <T> contained view type
   */
  public interface Helper<I, T> {

    //
    // Structure
    //

    void remove(I impl);

    T getContents(I impl);
  }

  FrameViewImpl(Helper<? super I, ? extends T> helper, I impl) {
    super(helper, impl);
  }

  public static <I, T extends View> FrameViewImpl<I, T> create(
      Helper<? super I, ? extends T> helper, I impl) {
    return new FrameViewImpl<I, T>(helper, impl);
  }

  @Override
  public Type getType() {
    return Type.FRAME;
  }

  //
  // Structural delegation.
  //

  @Override
  public void remove() {
    helper.remove(impl);
  }

  @Override
  public T getContents() {
    return helper.getContents(impl);
  }
}

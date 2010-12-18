/**
 * Copyright 2008 Google Inc.
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

package org.waveprotocol.wave.client.editor.gwt;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.Widget;

import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.model.util.Preconditions;

/**
 * Restricted interface (usually to a ComplexPanel) to provide the necessary
 * implementation to perform the GWT motions of logical widget attachment and
 * detachment
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public interface LogicalPanel {

  /**
   * Add a widget
   *
   * @param child Widget to logically attach to parent
   * @param location Physical location to attach child to
   */
  void addWidget(Widget child, Element location);

  /**
   * Remove a widget
   *
   * @param child The widget
   */
  void removeWidget(Widget child);

  /**
   * Default implementation of LogicalPanel using a ComplexPanel, provided
   * for convenience.
   *
   * @author danilatos@google.com (Daniel Danilatos)
   */
  public class Impl extends ComplexPanel implements LogicalPanel {
    /** {@inheritDoc} */
    public void addWidget(Widget child, Element location) {
      // No physical attach if location is null
      if (location == null) {
        if (child.getParent() != this) {
          child.removeFromParent();

          // Logical attach.
          getChildren().add(child);

          // Adopt.
          adopt(child);
        }
      } else {

        // TODO(danilatos): remove this cast when ComplexPanel#add() accepts a new-style element
        add(child, DomHelper.castToOld(location));
      }
    }

    /** {@inheritDoc} */
    public void removeWidget(Widget child) {
      // Validate.
      Preconditions.checkArgument(child.getParent() == this,
          "widget is not a logical child of this panel");

      try {
        // Orphan.
        orphan(child);
      } finally {

        // Logical detach.
        getChildren().remove(child);
      }
    }
  }
}

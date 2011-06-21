/**
 * Copyright 2010 Google Inc.
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
 */

package org.waveprotocol.wave.client.wavepanel.impl.toolbar.gadget.info;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

import org.waveprotocol.wave.client.wavepanel.impl.toolbar.gadget.GadgetInfo;

/**
 * Widget implementation of a blip link info popup.
 *
 * @author vega113@gmail.com (Yuri Z.)
 */
public final class GadgetInfoWidget extends Composite  {

  interface Binder extends UiBinder<CaptionPanel, GadgetInfoWidget> {
  }

  /** Resources used by this widget. */
  public interface Resources extends ClientBundle {
    /** CSS */
    @Source("GadgetInfoWidget.css")
    Style style();
  }

  interface Style extends CssResource {

    String explanation();

    String link();

    String self();

    String title();
    
    String valueTxt();
    
    String gadgetImg();
    
    String urlStyle();
    
    String textInfoPnl();
    
    String imgInfoPnl();
  }

  private final static Binder BINDER = GWT.create(Binder.class);
  
  @UiField(provided = true)
  final static Resources res = GWT.<Resources> create(Resources.class);

  @UiField(provided = true)
  final static Style style = res.style();

  static {
    // StyleInjector's default behaviour of deferred injection messes up
    // popups, which do synchronous layout queries for positioning. Therefore,
    // we force synchronous injection.
    StyleInjector.inject(style.getText(), true);
  }
  
  private static final  HTML NO_IMAGE =  new HTML("No image");
  
  @UiField
  Label name;
  
  @UiField
  Label description;
  
  @UiField
  Label primaryCategory;
  
  @UiField
  Label secondaryCategory;
  
  @UiField
  Label gadgetUrl;
  
  @UiField
  Label author;
  
  @UiField
  Label submittedby;
  
  @UiField
  VerticalPanel gadgetImagePanel;
  
  
  private Image gadgetImage;
  
  /**
   * Creates link info popup.
   */
  public GadgetInfoWidget() {
    initWidget(BINDER.createAndBindUi(this));
  }

  public void setGadgetInfo (final GadgetInfo gadgetInfo) {
    gadgetImagePanel.clear();
    String imageUrl = gadgetInfo.getImageUrl();
    if (false && imageUrl != null && !imageUrl.isEmpty()) {
      gadgetImage = new Image(new ImageResource() {
        
        @Override
        public String getName() {
          return gadgetInfo.getName();
        }
        
        @Override
        public boolean isAnimated() {
          return false;
        }
        
        @Override
        public int getWidth() {
          return 150;
        }
        
        @Override
        public String getURL() {
          return gadgetInfo.getImageUrl();
        }
        
        @Override
        public int getTop() {
          return 0;
        }
        
        @Override
        public int getLeft() {
          return 0;
        }
        
        @Override
        public int getHeight() {
          return 150;
        }
      });
      gadgetImagePanel.add(gadgetImage);
    }
    gadgetUrl.setText(gadgetInfo.getGadgetUrl());
    name.setText(gadgetInfo.getName());
    description.setText(gadgetInfo.getDescription());
    primaryCategory.setText(gadgetInfo.getPrimaryCategory().getValue());
    secondaryCategory.setText(gadgetInfo.getSecondaryCategory().getValue());
    author.setText(gadgetInfo.getAuthor());
    submittedby.setText(gadgetInfo.getSubmittedBy());
  }
}

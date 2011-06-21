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

package org.waveprotocol.wave.client.wavepanel.impl.toolbar.gadget.popup;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.VerticalPanel;

import org.waveprotocol.wave.client.common.util.UserAgent;
import org.waveprotocol.wave.client.scheduler.ScheduleCommand;
import org.waveprotocol.wave.client.scheduler.Scheduler;
import org.waveprotocol.wave.client.wavepanel.impl.toolbar.gadget.GadgetCategoryType;
import org.waveprotocol.wave.client.wavepanel.impl.toolbar.gadget.GadgetInfo;
import org.waveprotocol.wave.client.wavepanel.impl.toolbar.gadget.GadgetInfoImpl;
import org.waveprotocol.wave.client.wavepanel.impl.toolbar.gadget.GadgetInfoProvider;
import org.waveprotocol.wave.client.wavepanel.impl.toolbar.gadget.SimpleGadgetInfoProviderImpl;
import org.waveprotocol.wave.client.wavepanel.impl.toolbar.gadget.info.GadgetInfoWidget;
import org.waveprotocol.wave.client.widget.popup.CenterPopupPositioner;
import org.waveprotocol.wave.client.widget.popup.PopupChrome;
import org.waveprotocol.wave.client.widget.popup.PopupChromeFactory;
import org.waveprotocol.wave.client.widget.popup.PopupEventListener;
import org.waveprotocol.wave.client.widget.popup.PopupEventSourcer;
import org.waveprotocol.wave.client.widget.popup.PopupFactory;
import org.waveprotocol.wave.client.widget.popup.RelativePopupPositioner;
import org.waveprotocol.wave.client.widget.popup.UniversalPopup;

import java.util.Map;

/**
 * Widget implementation of a blip link info popup.
 *
 * @author vega113@gmail.com (Yuri Z.)
 */
public final class GadgetPopupWidget extends Composite
    implements GadgetPopupView, PopupEventListener {

  interface Binder extends UiBinder<VerticalPanel, GadgetPopupWidget> {
  }

  /** Resources used by this widget. */
  public interface Resources extends ClientBundle {
    /** CSS */
    @Source("GadgetPopupWidget.css")
    Style style();
  }

  interface Style extends CssResource {

    String explanation();

    String self();

    String title();
    
    String dropbox();
    
    String insertBtn();
  }

  private final static Binder BINDER = GWT.create(Binder.class);

  @UiField(provided = true)
  final static Style style = GWT.<Resources> create(Resources.class).style();

  static {
    // StyleInjector's default behaviour of deferred injection messes up
    // popups, which do synchronous layout queries for positioning. Therefore,
    // we force synchronous injection.
    StyleInjector.inject(style.getText(), true);
  }
  
  @UiField
  ListBox dropbox;
  @UiField
  ListBox multibox;
  @UiField
  SimplePanel suggestPanel;
  @UiField
  Button insertBtn;
  @UiField
  GadgetInfoWidget gadgetInfoBox;
  
  private MultiWordSuggestOracle oracle;
  private SuggestBox suggestBox;
  
  GadgetInfoProvider<GadgetInfoImpl> gadgetInfoProvider = SimpleGadgetInfoProviderImpl.create();
  
  private Map<String, GadgetInfoImpl> infos;

  private Listener listener;
  
  /** Popup containing this widget. */
  private final UniversalPopup popup;

  /**
   * Creates link info popup.
   */
  public GadgetPopupWidget() {
    initWidget(BINDER.createAndBindUi(this));
    PopupChrome chrome = PopupChromeFactory.createPopupChrome();
    if (UserAgent.isFirefox()) {
      popup =
          PopupFactory.createPopup(this.getElement(), new RelativePopupPositioner() {
            
            @Override
            public void setPopupPositionAndMakeVisible(Element relative, final Element p) {
              ScheduleCommand.addCommand(new Scheduler.Task() {
                @Override
                public void execute() {
                  p.getStyle().setLeft((RootPanel.get().getOffsetWidth() - p.getOffsetWidth()) / 2, Unit.PX);
                  int top = (RootPanel.get().getOffsetHeight() - p.getOffsetHeight()) / 4;
                  p.getStyle().setTop(Math.max(top, 280), Unit.PX);
                  p.getStyle().setVisibility(Visibility.VISIBLE);
                }
              });
            }
          }, chrome,
              true);
    } else {
      popup = PopupFactory.createPopup(null, new CenterPopupPositioner(), chrome, true);
    }
    popup.add(this);
    popup.addPopupEventListener(this);
  }

  private void init() {
    if (oracle == null) {
      oracle = new MultiWordSuggestOracle();
    }
    if (suggestBox == null) {
      suggestBox = new SuggestBox(oracle);
      suggestPanel.add(suggestBox);
      suggestBox.addValueChangeHandler(new ValueChangeHandler<String>() {
        
        @Override
        public void onValueChange(ValueChangeEvent<String> event) {
          String name = suggestBox.getTextBox().getText();
          for (int i = 0; i < multibox.getItemCount(); i++) {
            if(multibox.getItemText(i).equals(name)) {
              multibox.setSelectedIndex(i);
              displayGadgetInfo(infos.get(name));
              break;
            }
          }
        }
      });
    }
    showCategory(GadgetCategoryType.ALL);
  }
  
  private void showCategory (GadgetCategoryType categoryType) {
    dropbox.clear();
    for (GadgetCategoryType type: GadgetCategoryType.values()) {
      dropbox.addItem(type.getValue(),type.name());
    }
    dropbox.setSelectedIndex(categoryType.ordinal());
    infos = gadgetInfoProvider.retrieveGadgetInfo(categoryType);
    multibox.clear();
    multibox.setStylePrimaryName(style.dropbox());
    oracle.clear();
    for( GadgetInfo info : infos.values()) {
      String gadgetName = info.getName();
      multibox.addItem(gadgetName);
      oracle.add(gadgetName);
    }
    String name = multibox.getValue(multibox.getSelectedIndex());
    displayGadgetInfo(infos.get(name));
  }
  
  @UiHandler("dropbox")
  void handleDropboxChange(ChangeEvent event) {
    GadgetCategoryType category = GadgetCategoryType.valueOf(dropbox.getValue(dropbox
            .getSelectedIndex()));
    showCategory(category);
    suggestBox.setText("");
  }
  
  @UiHandler("multibox")
  void handleMultidropboxChange(ChangeEvent event) {
    String name = multibox.getValue(multibox.getSelectedIndex());
    suggestBox.getTextBox().setText(name);
    displayGadgetInfo(infos.get(name));
  }
  
  @UiHandler("insertBtn")
  void handleInsertGadget(ClickEvent event) {
    String name = suggestBox.getTextBox().getText();
    String url = null;
    int selectedIndex = -1;
    for (int i = 0; i < multibox.getItemCount(); i++) {
      if(multibox.getItemText(i).equals(name)) {
        selectedIndex = i;
        break;
      }
    }
    if (selectedIndex == -1 && (name == null || name.isEmpty())) {
      selectedIndex = 0;
    }
    if (selectedIndex != -1) {
      url = infos.get(multibox.getItemText(selectedIndex)).getGadgetUrl();
    } else {
      url = name;
    }
    listener.onInsert(url);
    hide();
  }
  
  private void displayGadgetInfo(GadgetInfo gadgetInfo) {
    gadgetInfoBox.setGadgetInfo(gadgetInfo);
  }

  @Override
  public void init(Listener listener) {
    this.listener = listener;
  }


  @Override
  public void reset() {
    Preconditions.checkState(this.listener != null);
    this.listener = null;
    oracle.clear();
    multibox.clear();
    dropbox.clear();
  }

  @Override
  public void show() {
    popup.show();
  }

  @Override
  public void hide() {
    popup.hide();
  }

  @Override
  public void onShow(PopupEventSourcer source) {
    if (listener != null) {
      listener.onShow();
    }
    init();
  }

  @Override
  public void onHide(PopupEventSourcer source) {
    if (listener != null) {
      listener.onHide();
    }
  }
}

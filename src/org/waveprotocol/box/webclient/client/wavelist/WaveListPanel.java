/**
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.waveprotocol.box.webclient.client.wavelist;

import com.google.common.base.Preconditions;
import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellList;
import com.google.gwt.user.cellview.client.CellList.Style;
import com.google.gwt.user.cellview.client.HasKeyboardPagingPolicy.KeyboardPagingPolicy;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;

import org.waveprotocol.box.common.IndexEntry;
import org.waveprotocol.box.webclient.client.ClientEvents;
import org.waveprotocol.box.webclient.client.events.WaveCreationEvent;
import org.waveprotocol.box.webclient.client.events.WaveIndexUpdatedEvent;
import org.waveprotocol.box.webclient.client.events.WaveIndexUpdatedEventHandler;
import org.waveprotocol.box.webclient.client.events.WaveSelectionEvent;
import org.waveprotocol.box.webclient.client.events.WaveSelectionEventHandler;
import org.waveprotocol.box.webclient.util.Log;
import org.waveprotocol.wave.client.widget.toolbar.ToolbarButtonViewBuilder;
import org.waveprotocol.wave.client.widget.toolbar.ToolbarView;
import org.waveprotocol.wave.client.widget.toolbar.ToplevelToolbarWidget;
import org.waveprotocol.wave.client.widget.toolbar.buttons.ToolbarClickButton;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.StringMap;
import org.waveprotocol.wave.model.waveref.WaveRef;

import java.util.Collection;
import java.util.List;

/**
 * Panel that contains search, toolbar and wave list digests.
 *
 * @author vega113@gmail.com (Yuri Z.)
 *
 */
public class WaveListPanel extends Composite {

  private static WaveListPanelUiBinder uiBinder = GWT.create(WaveListPanelUiBinder.class);

  interface WaveListPanelUiBinder extends UiBinder<Widget, WaveListPanel> {
  }

  /** Resources used by this widget. */
  public interface Resources extends ClientBundle {
    /** CSS */
    @Source("WaveListPanel.css")
    Css css();

    /** Default background images */
    @Source("images/panel_n.png")
    @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
    ImageResource chromeNorth();

    @Source("images/panel_ne.png")
    ImageResource chromeNorthEast();

    @Source("images/panel_e.png")
    @ImageOptions(repeatStyle = RepeatStyle.Vertical)
    ImageResource chromeEast();

    @Source("images/panel_se.png")
    ImageResource chromeSouthEast();

    @Source("images/panel_s.png")
    @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
    ImageResource chromeSouth();

    @Source("images/panel_sw.png")
    ImageResource chromeSouthWest();

    @Source("images/panel_w.png")
    @ImageOptions(repeatStyle = RepeatStyle.Vertical)
    ImageResource chromeWest();

    @Source("images/panel_nw.png")
    ImageResource chromeNorthWest();

    @Source("images/toolbar_empty.png")
    @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
    ImageResource emptyToolbar();
  }

  /** CSS for this widget. */
  public interface Css extends CssResource {
    // Background images for the bread crumb.
    String north();

    String northEast();

    String east();

    String southEast();

    String south();

    String southWest();

    String west();

    String northWest();

    /** Whole frame. */
    String frame();

    /** Element containing the content. */
    String contentContainer();

    /** Element containing the panel with search box. */
    String panel();

    /** The menu toolbar background image. */
    String toolbar();

    /** Top level toolbar widget. */
    String toolbarMenu();

    /** Scroll panel widget that contains wave list digests. */
    String pagerPanel();

    /** The pager with range label. */
    String rangePager();
  }

  public interface CellListResources extends CellList.Resources {
    /** The styles used in the CellList widget. */
    @Source("CellList.css")
    Style cellListStyle();
  }

  public final static Resources RESOURCES = GWT.create(Resources.class);
  public final static Css CSS = RESOURCES.css();

  /** Customized resources for the CellList widget. */
  public final static CellListResources CELL_LIST_RESOURCES = GWT.create(CellListResources.class);

  static {
    StyleInjector.inject(CSS.getText(), true);
  }

  private static final Log LOG = Log.get(WaveListPanel.class);

  /** The default number of wave digests per page. */
  private static final int DEFAULT_PAGE_SIZE = 50;

  /** The maximum digest snippet length. */
  private static final int MAX_SNIPPET_LENGTH = 64;

  /** The height of search panel. */
  private static final int SEARCH_PANEL_HEIGHT = 50;

  private static final String EMPTY_SNIPPET = "(empty)";

  /** The toolbar menu widget. */
  @UiField
  ToplevelToolbarWidget toolbarUi;

  /** The pager used to change the range of data. */
  @UiField
  ShowMorePagerPanel pagerPanel;

  /** The pager to display the current range */
  @UiField
  RangeLabelPager rangePagerLabel;

  /** The CellList. */
  private CellList<DigestInfo> cellList;

  /** The provider that holds the list of digests. */
  private final ListDataProvider<DigestInfo> dataProvider = new ListDataProvider<DigestInfo>(DigestInfo.KEY_PROVIDER);

  /**
   * This map contains all the digests and allows to test whether certain digest
   * is contained in the dataProvider.getList in O(1).
   */
  private final StringMap<DigestInfo> digestsMap = CollectionUtils.createStringMap();

  static class CssConstants {
    // Called from @eval in css.
    final static String TOOLBAR_LENGTH_CSS = RESOURCES.chromeSouthEast().getWidth()
        - RESOURCES.chromeNorthEast().getWidth() + "px";
    final static String SEARCH_PANEL_HEIGHT_CSS = SEARCH_PANEL_HEIGHT + "px";
    final static String TOOLBAR_TOP_CSS = SEARCH_PANEL_HEIGHT + 1 + "px";
    final static String TOOLBAR_HEIGHT_CSS = RESOURCES.emptyToolbar().getHeight() + "px";
    final static String PAGER_PANEL_TOP_CSS = SEARCH_PANEL_HEIGHT + 1
        + RESOURCES.emptyToolbar().getHeight() + "px";
  }

  /**
   * This object encapsulates the wave list digest data and logic.
   */
  public static final class DigestInfo {

    private String snippet;

    private final String id;

    private boolean isUnread = false;

    /**
     * The key provider that provides the unique ID of a digest.
     */
    public static final ProvidesKey<DigestInfo> KEY_PROVIDER = new ProvidesKey<DigestInfo>() {
      public Object getKey(DigestInfo item) {
        return item == null ? null : item.getId();
      }
    };

    /**
     * Constructs {@link DigestInfo}.
     *
     * @param id ID cannot be null or empty.
     */
    private DigestInfo(String id) {
      Preconditions.checkArgument(id != null, "ID cannot be null");
      Preconditions.checkArgument(!id.isEmpty(), "ID cannot be empty");
      this.id = id;
    }

    /**
     *  Returns the digest snippet.
     */
    public String getSnippet() {
      return snippet;
    }

    /**
     * Sets the digest snippet.
     */
    public void setSnippet(String snippet) {
      this.snippet = snippet;
    }

    /**
     * Returns digestInfo ID.
     */
    public String getId() {
      return id;
    }

    /**
     * Returns true if the digest is changed by other and become unread, false
     *         otherwise.
     */
    public boolean isUnread() {
      return isUnread;
    }

    /**
     * Sets the digest read status. If the digest is unread - the snippet is
     * rendered as black, gray otherwise.
     *
     * @param isUnread
     */
    public void setUnread(boolean isUnread) {
      this.isUnread = isUnread;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((id == null) ? 0 : id.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      DigestInfo other = (DigestInfo) obj;
      if (id == null) {
        if (other.id != null) return false;
      } else if (!id.equals(other.id)) return false;
      return true;
    }

    /**
     * Static factory method to construct {@link DigestInfo}
     *
     * @param id The unique digest ID - i.e. serialized wave ID.
     * @return New DigestInfo instance.
     */
    public static DigestInfo create(String id) {
      DigestInfo digestInfo = new DigestInfo(id);
      return digestInfo;
    }
  }

  /**
   * The Cell used to render a {@link DigestInfo}.
   */
  static class DigestCell extends AbstractCell<DigestInfo> {

    /**
     * Renders each wave list digest row.
     */
    @Override
    public void render(Context context, DigestInfo value, SafeHtmlBuilder sb) {
      if (value == null) {
        return;
      }
      // TODO (Yuri Z.) Extend to include more info - like participants, last
      // modified, unread count...
      String mainStyle = "style=' height: 20px;";
      String addStyle = value.isUnread() ? " color: black;'" : " color: gray;'";
      String style = mainStyle + addStyle;
      sb.appendHtmlConstant("<div " + style + ">");
      sb.appendEscaped(value.getSnippet());
      sb.appendHtmlConstant("</div>");
    }
  }

  public WaveListPanel() {
    initWidget(uiBinder.createAndBindUi(this));
    initialize();
    ClientEvents.get().addWaveSelectionEventHandler(new WaveSelectionEventHandler() {
      @Override
      public void onSelection(WaveRef id) {
        LOG.info("WaveList changing selection to " + id.toString());
        select(id);
      }
    });
    ClientEvents.get().addWaveIndexUpdatedEventHandler(new WaveIndexUpdatedEventHandler() {
      @Override
      public void onWaveIndexUpdate(WaveIndexUpdatedEvent event) {
        LOG.info("WaveList refreshing due to index update");
        update(event.getEntries());
      }
    });
    initToolbarMenu();
  }

  /**
   * Add custom buttons to the toolbar.
   */
  private void initToolbarMenu() {
    ToolbarView group = toolbarUi.addGroup();
    new ToolbarButtonViewBuilder().setText("New Wave").applyTo(group.addClickButton(),
        new ToolbarClickButton.Listener() {
          @Override
          public void onClicked() {
            ClientEvents.get().fireEvent(WaveCreationEvent.CREATE_NEW_WAVE);
          }
        });
    // Fake group with empty button - to force the separator be displayed.
    group = toolbarUi.addGroup();
    new ToolbarButtonViewBuilder().setText("").applyTo(group.addClickButton(), null);

  }

  /**
   * Adds/updates digests collection to the model and to the display. It also
   * synchronizes the digestsMap to hold the same entries as the model.
   *
   * @param digest The digest to add/update.
   */
  private void addDigests(Collection<DigestInfo> digests) {
    List<DigestInfo> wrappedList = dataProvider.getList();
    // TODO (Yuri Z.) The list should be sorted according to the lastModified
    // time.
    wrappedList.addAll(0, digests);
    // Add the digests also to the map so it will contain the same entries as
    // wrappedList.
    for (DigestInfo digest: digests) {
      digestsMap.put(digest.getId(), digest);
    }
  }

  /**
   * Removes a digest to the model and to the display.
   *
   * @param digest the digest to remove
   */
  private void removeDigest(DigestInfo digest) {
    List<DigestInfo> digests = dataProvider.getList();
    digests.remove(digest);
  }


  /**
   * Adds a display to the database. The current range of interest of the display
   * will be populated with data.
   *
   * @param display a {@link HasData}.
   */
  void addDataDisplay(HasData<DigestInfo> display) {
    dataProvider.addDataDisplay(display);
  }

  /**
   * Refresh all displays.
   */
  public void refreshDisplays() {
    dataProvider.refresh();
  }

  private WaveId currentSelectionId;

  public void initialize() {
    // Replace ArrayList implementation of DataProvider with LinkedList in order
    // to optimize add/remove operations.
    List<DigestInfo> wrappedList = CollectionUtils.newLinkedList();
    dataProvider.setList(wrappedList);
    // Set a key provider that provides a unique key for each digest.
    cellList =
        new CellList<DigestInfo>(new DigestCell(), CELL_LIST_RESOURCES, DigestInfo.KEY_PROVIDER);
    cellList.setPageSize(DEFAULT_PAGE_SIZE);
    cellList.setKeyboardPagingPolicy(KeyboardPagingPolicy.INCREASE_RANGE);
    // Add a selection model so we can select cells.
    final SingleSelectionModel<DigestInfo> selectionModel =
        new SingleSelectionModel<DigestInfo>(DigestInfo.KEY_PROVIDER);
    cellList.setSelectionModel(selectionModel);

    selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
      public void onSelectionChange(SelectionChangeEvent event) {
        @SuppressWarnings("unchecked")
        SingleSelectionModel<DigestInfo> selectionModel =
            (SingleSelectionModel<DigestInfo>) event.getSource();
        String serializedId = selectionModel.getSelectedObject().getId();
        WaveId waveId = WaveId.deserialise(serializedId);
        if (!waveId.equals(currentSelectionId)) {
          ClientEvents.get().fireEvent(new WaveSelectionEvent(WaveRef.of(waveId)));
        }
      }
    });
    // Set the cellList as the display of the pagers.
    // pagerPanel is a scrollable pager that extends the range when the
    // user scrolls to the bottom.
    pagerPanel.setDisplay(cellList);
    rangePagerLabel.setDisplay(cellList);

    addDataDisplay(cellList);
  }

  private void select(WaveRef waveRef) {
    String serializedId = waveRef.getWaveId().serialise();
    DigestInfo digest = findDigestInfoById(serializedId);
    if (digest == null) {
      currentSelectionId = null;
    } else {
      currentSelectionId = waveRef.getWaveId();
      cellList.getSelectionModel().setSelected(digest, true);
    }
  }

  private DigestInfo findDigestInfoById(String id) {
    Preconditions.checkArgument(id != null);
    DigestInfo digest = digestsMap.get(id);
    return digest;
  }

  /**
   * Updates the wave list with new/updated entries.
   */
  private void update(List<IndexEntry> entries) {
    // The update process below is potentially O(n^2) in the number of entries.
    // However, the expected case is that entries are listed in the same order
    // as the digests, which makes the process below only O(n). Also, the
    // number of entries in an update is expected to be small (typically one).
    List<DigestInfo> digestsList =  CollectionUtils.newLinkedList();
    for (IndexEntry entry : entries) {
      String serializedId = entry.getWaveId().serialise();
      String snippet = entry.getDigest();
      if (snippet.trim().isEmpty()) {
        snippet = EMPTY_SNIPPET;
      }
      // Check fast using StringMap if the digest is new.
      DigestInfo digest = digestsMap.get(serializedId);
      if (digest != null) {
        // Remove the digest from the displayed list first, so we don't add duplicates.
        removeDigest(digest);
      } else {
        // Create a new digest.
        digest = DigestInfo.create(serializedId);
      }
      digestsList.add(digest);
      // TODO (Yuri Z.) if the update is by someone else - set the unread flag
      // to true so the snippet will be black (unread).
      digest.setSnippet(snippet.substring(0, Math.min(snippet.length(), MAX_SNIPPET_LENGTH)));
      if (entry.getWaveId().equals(currentSelectionId)) {
        WaveRef waveRef = WaveRef.of(entry.getWaveId());
        select(waveRef);
      }
    }
    addDigests(digestsList);
    refreshDisplays();
  }
}

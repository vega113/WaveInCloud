/**
 * Copyright 2011 Google Inc.
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
package org.waveprotocol.box.webclient.search;

import org.waveprotocol.box.webclient.client.ClientEvents;
import org.waveprotocol.box.webclient.client.events.WaveCreationEvent;
import org.waveprotocol.box.webclient.search.Search.State;
import org.waveprotocol.wave.client.scheduler.Scheduler.IncrementalTask;
import org.waveprotocol.wave.client.scheduler.SchedulerInstance;
import org.waveprotocol.wave.client.scheduler.TimerService;
import org.waveprotocol.wave.client.widget.toolbar.GroupingToolbar;
import org.waveprotocol.wave.client.widget.toolbar.ToolbarButtonViewBuilder;
import org.waveprotocol.wave.client.widget.toolbar.ToolbarView;
import org.waveprotocol.wave.client.widget.toolbar.buttons.ToolbarClickButton;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.IdentityMap;

import java.util.List;

/**
 * Presents a search model into a search view.
 * <p>
 * This class invokes rendering, and controls the lifecycle of digest views. It
 * also handles all UI gesture events sourced from views in the search panel.
 *
 * @author hearnden@google.com (David Hearnden)
 */
public final class SearchPresenter
    implements Search.Listener, SearchPanelView.Listener, SearchView.Listener, IncrementalTask {

  /**
   * Handles digest selection actions.
   */
  public interface WaveSelectionHandler {
    void onWaveSelected(WaveId id);
  }

  /** How often to repeat the search query. */
  private final static int POLLING_INTERVAL_MS = 10000; // 10s
  private final static String DEFAULT_SEARCH = "in:inbox";

  // External references
  private final TimerService scheduler;
  private final Search search;
  private final SearchPanelView searchUi;

  // Internal state
  private final IdentityMap<DigestView, Digest> digestUis = CollectionUtils.createIdentityMap();
  private final WaveSelectionHandler selectionHandler;
  /** Current search query. */
  private String query = DEFAULT_SEARCH;
  /** Current selected digest. */
  private DigestView selected;

  SearchPresenter(TimerService scheduler, Search search, SearchPanelView searchUi,
      WaveSelectionHandler selectionHandler) {
    this.search = search;
    this.searchUi = searchUi;
    this.scheduler = scheduler;
    this.selectionHandler = selectionHandler;
  }

  /**
   * Creates a search presenter.
   *
   * @param model model to present
   * @param view view to render into
   * @param selectionHandler handler for selection actions
   */
  public static SearchPresenter create(
      Search model, SearchPanelView view, WaveSelectionHandler selectionHandler) {
    SearchPresenter presenter = new SearchPresenter(
        SchedulerInstance.getMediumPriorityTimer(), model, view, selectionHandler);
    presenter.init();
    return presenter;
  }

  /**
   * Performs initial presentation, and attaches listeners to live objects.
   */
  private void init() {
    initToolbarMenu();
    initSearchBox();
    render();
    search.addListener(this);
    searchUi.init(this);
    searchUi.getSearch().init(this);

    // Fire a polling search.
    scheduler.scheduleRepeating(this, 0, POLLING_INTERVAL_MS);
  }

  /**
   * Releases resources and detaches listeners.
   */
  public void destroy() {
    scheduler.cancel(this);
    searchUi.getSearch().reset();
    searchUi.reset();
    search.removeListener(this);
  }

  /**
   * Adds custom buttons to the toolbar.
   */
  private void initToolbarMenu() {
    GroupingToolbar.View toolbarUi = searchUi.getToolbar();
    ToolbarView group = toolbarUi.addGroup();
    new ToolbarButtonViewBuilder().setText("New Wave").applyTo(
        group.addClickButton(), new ToolbarClickButton.Listener() {
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
   * Initializes the search box.
   */
  private void initSearchBox() {
    searchUi.getSearch().setQuery(query);
  }

  /**
   * Executes the current search.
   */
  private void doSearch() {
    search.find(query);
  }

  /**
   * Renders the current state of the search result into the panel.
   */
  private void render() {
    // Preserve selection on re-rendering.
    WaveId toSelect = selected != null ? digestUis.get(selected).getWaveId() : null;
    searchUi.clearDigests();
    digestUis.clear();
    setSelected(null);
    for (int i = 0, size = search.getTotal(); i < size; i++) {
      Digest digest = search.getDigest(i);
      DigestView digestUi = searchUi.insertBefore(null, digest);
      digestUis.put(digestUi, digest);
      if (digest.getWaveId().equals(toSelect)) {
        setSelected(digestUi);
      }
    }
  }

  //
  // UI gesture events.
  //

  private void setSelected(DigestView digestUi) {
    if (selected != null) {
      selected.deselect();
    }
    selected = digestUi;
    if (selected != null) {
      selected.select();
    }
  }

  /**
   * Invokes the wave-select action on the currently selected digest.
   */
  private void openSelected() {
    selectionHandler.onWaveSelected(digestUis.get(selected).getWaveId());
  }

  @Override
  public void onClicked(DigestView digestUi) {
    setSelected(digestUi);
    openSelected();
  }

  @Override
  public void onQueryEntered() {
    query = searchUi.getSearch().getQuery();
    doSearch();
  }

  //
  // Periodic poll.
  //

  @Override
  public boolean execute() {
    doSearch();
    // Keep running until destroyed.
    return true;
  }

  //
  // Search events. For now, dumbly re-render the whole list.
  //

  @Override
  public void onStateChanged() {
    if (search.getState() == State.READY) {
      render();
    }
  }

  @Override
  public void onDigestAdded(int index, Digest digest) {
    render();
  }

  @Override
  public void onDigestRemoved(int index, Digest digest) {
    render();
  }

  @Override
  public void onDigestsReady(List<Integer> indices) {
    render();
  }
}

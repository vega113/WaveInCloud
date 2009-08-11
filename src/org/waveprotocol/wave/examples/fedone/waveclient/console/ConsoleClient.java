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

package org.waveprotocol.wave.examples.fedone.waveclient.console;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import jline.ANSIBuffer;
import jline.ConsoleReader;

import org.waveprotocol.wave.examples.fedone.waveclient.common.ClientBackend;
import org.waveprotocol.wave.examples.fedone.waveclient.common.ClientUtils;
import org.waveprotocol.wave.examples.fedone.waveclient.common.ClientWaveView;
import org.waveprotocol.wave.examples.fedone.waveclient.common.IndexEntry;
import org.waveprotocol.wave.examples.fedone.waveclient.common.WaveletOperationListener;
import org.waveprotocol.wave.examples.fedone.waveclient.console.ScrollableWaveView.RenderMode;
import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMap;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.BufferedDocOp;
import org.waveprotocol.wave.model.document.operation.DocInitializationCursor;
import org.waveprotocol.wave.model.document.operation.impl.AttributesImpl;
import org.waveprotocol.wave.model.document.operation.impl.InitializationCursorAdapter;
import org.waveprotocol.wave.model.document.operation.impl.BufferedDocOpImpl.DocOpBuilder;
import org.waveprotocol.wave.model.operation.wave.AddParticipant;
import org.waveprotocol.wave.model.operation.wave.RemoveParticipant;
import org.waveprotocol.wave.model.operation.wave.WaveletDocumentOperation;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.WaveletData;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * User interface for the console client using the JLine library.
 *
 *
 */
public class ConsoleClient implements WaveletOperationListener {

  /** All document rendering and operations are done to a single document. */
  private static final String MAIN_DOCUMENT_ID = "main";

  /** Single active client-server interface, or null when not connected to a server. */
  private ClientBackend backend = null;

  /** Single active console reader. */
  private final ConsoleReader reader;

  /** Currently open wave.  */
  private ScrollableWaveView openWave;

  /** Inbox we are rendering. */
  private ScrollableInbox inbox;

  /** PrintStream to use for output.  We don't use ConsoleReader's functionality because it's too
   * verbose and doesn't really give us anything in return. */
  private final PrintStream out = System.out;

  /**
   * Create new console client.
   */
  public ConsoleClient() throws IOException {
    reader = new ConsoleReader();
  }

  /**
   * Entry point for the user interface, receives user input, terminates on EOF.
   *
   * @param args command line arguments
   */
  public void run(String[] args) throws IOException {
    // Initialise screen and move cursor to bottom left corner
    reader.clearScreen();
    reader.setDefaultPrompt("(not connected) ");
    out.println(ANSIBuffer.ANSICodes.gotoxy(reader.getTermheight(), 1));

    // Set up scrolling -- these are the opposite to how you would expect because of the way that
    // the waves are scrolled (where the bottom is treated as the top)
    reader.addTriggeredAction('}', new ActionListener() {
      @Override public void actionPerformed(ActionEvent e) {
        if (isWaveOpen()) {
          openWave.scrollUp(1);
          render();
        }
      }
    });

    reader.addTriggeredAction('{', new ActionListener() {
      @Override public void actionPerformed(ActionEvent e) {
        if (isWaveOpen()) {
          openWave.scrollDown(1);
          render();
        }
      }
    });

    reader.addTriggeredAction('_', new ActionListener() {
      @Override public void actionPerformed(ActionEvent e) {
        if (isWaveOpen()) {
          openWave.scrollToTop();
          render();
        }
      }
    });

    // Immediately establish connection if desired, otherwise the user will need to use "/connect"
    if (args.length == 3) {
      connect(args[0], args[1], args[2]);
    } else if (args.length != 0) {
      System.out.println("Usage: java ConsoleClient [user@domain server port]");
      System.exit(1);
    }

    for (String line = reader.readLine(); line != null; line = reader.readLine()) {
      if (line.startsWith("/")) {
        doCommand(extractCmd(line), extractArgs(line));
      } else if (line.length() > 0) {
        sendAppendMutation(line);
      } else {
        render();
      }
    }

    if (isConnected()) {
      backend.shutdown();
    }

    // And yet there still seem to be threads hanging around
    System.exit(0);
  }

  /**
   * Extract the command from a command line String.
   *
   * For example, extractCmd("/connect hello") returns "connect".
   *
   * @param commandLine the command line input
   * @return command
   */
  private String extractCmd(String commandLine) {
    return extractCmdBits(commandLine).get(0).substring(1);
  }

  /**
   * Extract the command arguments from a command line String.
   *
   * For example, extractArgs("/connect hello") returns ["hello"].
   *
   * @param commandLine the command line input
   * @return list of command arguments
   */
  private List<String> extractArgs(String commandLine) {
    List<String> bits = extractCmdBits(commandLine);
    return bits.subList(1, bits.size());
  }

  /**
   * Extract a list of command line components from a command line String.
   *
   * For example, extractCmdBits("/connect hello") return ["/connect", "hello"].
   *
   * @param commandLine the command line input
   * @return list of command line components
   */
  private List<String> extractCmdBits(String commandLine) {
    return Arrays.asList(commandLine.trim().split(" +"));
  }

  /**
   * Perform command with given arguments.
   *
   * @param cmd command string to perform
   * @param args list of arguments to the command
   */
  private void doCommand(String cmd, List<String> args) {
    if (cmd.equals("connect")) {
      if (args.size() == 3) {
        connect(args.get(0), args.get(1), args.get(2));
      } else {
        badArgs(cmd);
      }
    } else if (cmd.equals("open")) {
      if (args.size() == 1) {
        try {
          doOpenWave(Integer.parseInt(args.get(0)));
        } catch (NumberFormatException e) {
          out.println("Error: " + args.get(0) + " is not a number");
        }
      } else {
        badArgs(cmd);
      }
    } else if (cmd.equals("new")) {
      newWave();
    } else if (cmd.equals("add")) {
      if (args.size() == 1) {
        addParticipant(args.get(0));
      } else {
        badArgs(cmd);
      }
    } else if (cmd.equals("remove")) {
      if (args.size() == 1) {
        removeParticipant(args.get(0));
      } else {
        badArgs(cmd);
      }
    } else if (cmd.equals("view")) {
      if (args.size() == 1) {
        setView(args.get(0));
      } else {
        badArgs(cmd);
      }
    } else if (cmd.equals("read")) {
      readAllWaves();
    } else if (cmd.equals("undo")) {
      if (args.size() == 1) {
        undo(args.get(0));
      } else if (backend != null) {
        undo(backend.getUserId().getAddress());
      } else {
        errorNotConnected();
      }
    } else if (cmd.equals("quit")) {
      System.exit(0);
    } else {
      out.println("Recognised commands:");
      out.println("  /connect  user@domain server port  connect to server:port as user@domain");
      out.println("  /open     entry                    open a wave given an inbox entry");
      out.println("  /new                               create and open a new wave");
      out.println("  /add      participantId            add a participant to a wave");
      out.println("  /remove   participantId            remove a participant from a wave");
      out.println("  /view     mode                     change view mode for the open wavelet:");
      out.println("                                       normal  normal rendering");
      out.println("                                       xml     show raw XML");
      out.println("  /read                              set all waves as read");
      out.println("  /undo     [participantId]          undo last line by a user, defaulting to "
          + (backend == null ? "(nobody)" : backend.getUserId()));
      out.println("  /quit                              quit the client");
      out.println("Interactive commands:");
      out.println("  {  scroll up open wave");
      out.println("  }  scroll down open wave");
      out.println("  _  scroll open wave to the bottom");
      out.println();
    }
  }

  /**
   * Print some error message when there are bad arguments to a user interface command.
   *
   * @param cmd the bad command
   */
  private void badArgs(String cmd) {
    out.println("Error: bad args to " + cmd + ", run \"/?\"");
  }

  /**
   * Register a user and server with a new {@link ClientBackend}.
   */
  private void connect(String userAtDomain, String server, String portString) {
    // We can only connect to one server at a time (at least, in this simple UI)
    if (isConnected()) {
      out.println("Warning: already connected");
      backend.shutdown();
      backend = null;
      openWave = null;
      inbox = null;
    }

    int port;
    try {
      port = Integer.parseInt(portString);
    } catch (NumberFormatException e) {
      out.println("Error: must provide valid port");
      return;
    }

    try {
      backend = new ClientBackend(userAtDomain, server, port);
    } catch (IOException e) {
      out.println("Error: failed to connect, " + e.getMessage());
      return;
    }

    backend.addWaveletOperationListener(this);
    reader.setDefaultPrompt(ConsoleUtils.ansiWrap(ConsoleUtils.ANSI_RED_FG, userAtDomain + "> "));
    inbox = new ScrollableInbox(backend, backend.getIndexWave());

    render();
  }

  /**
   * Send a mutation across the wire that appends text to the currently open document, and inserting
   * a new line element with us as the author.
   *
   * @param text text to append and send
   */
  private void sendAppendMutation(String text) {
    if (text.length() == 0) {
      throw new IllegalArgumentException("Cannot append a empty String");
    } else if (isWaveOpen()) {
      BufferedDocOp openDoc = getOpenDocument();
      int docSize = (openDoc == null) ? 0 : ClientUtils.findDocumentSize(openDoc);
      DocOpBuilder docOp = new DocOpBuilder();

      if (docSize > 0) {
        docOp.retain(docSize);
      }

      docOp.elementStart(
          ConsoleUtils.LINE,
          new AttributesImpl(
              ImmutableMap.of(ConsoleUtils.LINE_AUTHOR, backend.getUserId().getAddress())));
      docOp.elementEnd();
      docOp.characters(text);

      backend.sendWaveletOperation(
          getOpenWavelet(),
          new WaveletDocumentOperation(MAIN_DOCUMENT_ID, docOp.finish()));
    } else {
      out.println("Error: no open wave, run \"/open\"");
    }
  }

  /**
   * @return the open wavelet of the open wave, or null if no wave is open
   */
  private WaveletData getOpenWavelet() {
    return (openWave == null) ? null : ClientUtils.getConversationRoot(openWave.getWave());
  }

  /**
   * @return open document, or null if no wave is open or main document doesn't exist
   */
  private BufferedDocOp getOpenDocument() {
    return getOpenWavelet() == null ? null : getOpenWavelet().getDocuments().get(MAIN_DOCUMENT_ID);
  }

  /**
   * Open a wave with a given entry (index in the inbox).
   *
   * @param entry into the inbox
   */
  private void doOpenWave(int entry) {
    if (isConnected()) {
      List<IndexEntry> index = ClientUtils.getIndexEntries(backend.getIndexWave());

      if (entry >= index.size()) {
        out.print("Error: entry is out of range, ");
        if (index.isEmpty()) {
          out.println("there are no available waves (try \"/new\")");
        } else {
          out.println("expecting [0.." + (index.size() - 1) + "] (for example, \"/open 0\")");
        }
      } else {
        setOpenWave(backend.getWave(index.get(entry).getWaveId()));
      }
    } else {
      errorNotConnected();
    }
  }

  /**
   * Set a wave as the open wave.
   *
   * @param wave to set as open
   */
  private void setOpenWave(ClientWaveView wave) {
    if (ClientUtils.getConversationRoot(wave) == null) {
      wave.createWavelet(ClientUtils.getConversationRootId(wave));
    }

    openWave = new ScrollableWaveView(wave);
    render();
  }

  /**
   * Add a new wave.
   */
  private void newWave() {
    if (isConnected()) {
      backend.createNewWave();
    } else {
      errorNotConnected();
    }
  }

  /**
   * Add a participant to the currently open wave(let).
   *
   * @param name name of the participant to add
   */
  private void addParticipant(String name) {
    if (isWaveOpen()) {
      ParticipantId addId = new ParticipantId(name);

      // Don't send an invalid op, although the server should be robust enough to deal with it
      if (!getOpenWavelet().getParticipants().contains(addId)) {
        backend.sendWaveletOperation(getOpenWavelet(), new AddParticipant(addId));
      } else {
        out.println("Error: " + name + " is already a participant on this wave");
      }
    } else {
      errorNoWaveOpen();
    }
  }

  /**
   * Remove a participant from the currently open wave(let).
   *
   * @param name name of the participant to remove
   */
  private void removeParticipant(String name) {
    if (isWaveOpen()) {
      ParticipantId removeId = new ParticipantId(name);

      if (getOpenWavelet().getParticipants().contains(removeId)) {
        backend.sendWaveletOperation(getOpenWavelet(), new RemoveParticipant(removeId));
      } else {
        out.println("Error: " + name + " is not a participant on this wave");
      }
    } else {
      errorNoWaveOpen();
    }
  }

  /**
   * Set the view type for the open wavelet.
   *
   * @param mode for rendering
   */
  private void setView(String mode) {
    if (isWaveOpen()) {
      if (mode.equals("normal")) {
        openWave.setRenderingMode(RenderMode.NORMAL);
        render();
      } else if (mode.equals("xml")) {
        openWave.setRenderingMode(RenderMode.XML);
        render();
      } else {
        out.println("Error: unsupported rendering, run \"?\"");
      }
    } else {
      errorNoWaveOpen();
    }
  }

  /**
   * Set all waves as read.
   */
  private void readAllWaves() {
    if (isConnected()) {
      inbox.updateHashedVersions();
      render();
    } else {
      errorNotConnected();
    }
  }

  /**
   * Undo last line (line elements and text) sent by a user.
   *
   * @param userId of user
   */
  private void undo(String userId) {
    if (isWaveOpen()) {
      if (getOpenWavelet().getParticipants().contains(new ParticipantId(userId))) {
        undoLastLineBy(userId);
      } else {
        out.println("Error: " + userId + " is not a participant of this wave");
      }
    } else {
      errorNoWaveOpen();
    }
  }

  /**
   * Do the real work for undo.
   *
   * @param userId of user
   */
  private void undoLastLineBy(final String userId) {
    if (getOpenDocument() == null) {
      out.println("Error: document is empty");
      return;
    }

    // Find the last line written by the participant given by userId (by counting the number of
    // <line></line> elements, and comparing to their authors).
    final AtomicInteger totalLines = new AtomicInteger(0);
    final AtomicInteger lastLine = new AtomicInteger(-1);

    getOpenDocument().apply(new InitializationCursorAdapter(
        new DocInitializationCursor() {
          @Override
          public void elementStart(String type, Attributes attrs) {
            if (type.equals(ConsoleUtils.LINE)) {
              totalLines.incrementAndGet();

              if (userId.equals(attrs.get(ConsoleUtils.LINE_AUTHOR))) {
                lastLine.set(totalLines.get() - 1);
              }
            }
          }

          @Override public void characters(String s) {}
          @Override public void annotationBoundary(AnnotationBoundaryMap map) {}
          @Override public void elementEnd() {}
        }));

    // Delete the line
    if (lastLine.get() >= 0) {
      WaveletDocumentOperation undoOp = new WaveletDocumentOperation(MAIN_DOCUMENT_ID,
          ConsoleUtils.createLineDeletion(getOpenDocument(), lastLine.get()));
      backend.sendWaveletOperation(getOpenWavelet(), undoOp);
    } else {
      out.println("Error: " + userId + " hasn't written anything yet");
    }
  }

  /**
   * Print error message if user is not connected to a server.
   */
  private void errorNotConnected() {
    out.println("Error: not connected, run \"/connect user@domain server port\"");
  }

  /**
   * Print error message if user does not have a wave open.
   */
  private void errorNoWaveOpen() {
    out.println("Error: no wave is open, run \"/open index\"");
  }

  /**
   * @return whether the client is connected to any server
   */
  private boolean isConnected() {
    return backend != null;
  }

  /**
   * @return whether the client has a wave open
   */
  private boolean isWaveOpen() {
    return isConnected() && openWave != null;
  }

  /**
   * Render everything (inbox, open wave, input).
   */
  private void render() {
    StringBuilder buf = new StringBuilder();

    // Clear screen
    buf.append(ANSIBuffer.ANSICodes.save());
    buf.append(ANSIBuffer.ANSICodes.gotoxy(1, 1));
    buf.append(((char) 27) + "[J");

    // Render inbox and wave size by side
    List<String> inboxRender;
    List<String> waveRender;

    if (isConnected() && backend.getIndexWave() != null) {
      inbox.setOpenWave(openWave == null ? null : openWave.getWave());
      inboxRender = inbox.render(getCanvassWidth(), getCanvassHeight());
    } else {
      inboxRender = Lists.newArrayList();
      ConsoleUtils.ensureHeight(getCanvassWidth(), getCanvassHeight(), inboxRender);
    }

    if (isWaveOpen()) {
      waveRender = openWave.render(getCanvassWidth(), getCanvassHeight());
    } else {
      waveRender = Lists.newArrayList();
      ConsoleUtils.ensureHeight(getCanvassWidth(), getCanvassHeight(), waveRender);
    }

    buf.append(renderSideBySide(inboxRender, waveRender));

    // Draw what the user was typing at the time of rendering
    buf.append(ANSIBuffer.ANSICodes.gotoxy(reader.getTermheight(), 1));
    buf.append(reader.getDefaultPrompt());
    buf.append(reader.getCursorBuffer());

    // Restore cursor
    buf.append(ANSIBuffer.ANSICodes.restore());

    out.print(buf);
    out.flush();
  }

  /**
   * @return the width of the "canvass", how wide a single rendering panel is
   */
  private int getCanvassWidth() {
    return (reader.getTermwidth() / 2) - 2; // there are 2 panels, then leave some space
  }

  /**
   * @return the height of the "canvass", how high a single rendering panel is
   */
  private int getCanvassHeight() {
    return reader.getTermheight() - 1; // subtract a line for the input
  }

  /**
   * Render two list of Strings (lines) side by side.
   *
   * @param left column
   * @param right column
   * @return rendered columns
   */
  private String renderSideBySide(List<String> left, List<String> right) {
    StringBuilder rendered = new StringBuilder();

    if (left.size() != right.size()) {
      throw new IllegalArgumentException("Left and right are different heights");
    }

    for (int i = 0; i < left.size(); i++) {
      rendered.append(left.get(i));
      rendered.append(" | ");
      rendered.append(right.get(i));
      rendered.append("\n");
    }

    return rendered.toString();
  }

  @Override
  public void waveletDocumentUpdated(WaveletData wavelet, WaveletDocumentOperation docOp) {
  }

  @Override
  public void participantAdded(WaveletData wavelet, ParticipantId participantId) {
  }

  @Override
  public void participantRemoved(WaveletData wavelet, ParticipantId participantId) {
    if (isWaveOpen() && participantId.equals(backend.getUserId())) {
      // We might have been removed from our open wave (an impressively verbose check...)
      if (wavelet.getWaveletName().waveId.equals(openWave.getWave().getWaveId())) {
        openWave = null;
      }
    }
  }

  @Override
  public void noOp(WaveletData wavelet) {
  }

  @Override
  public void onDeltaSequenceStart(WaveletData wavelet) {
  }

  @Override
  public void onDeltaSequenceEnd(WaveletData wavelet) {
    render();
  }

  public static void main(String[] args) {
    try {
      ConsoleClient ui = new ConsoleClient();
      ui.run(args);
    } catch (IOException e) {
      System.err.println("IOException when running client: " + e);
    }
  }
}

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
 */

package org.waveprotocol.wave.client.wavepanel.impl.toolbar.gadget;

import com.google.common.collect.Maps;

import org.waveprotocol.wave.model.util.CollectionUtils;

import java.util.Map;

public class SimpleGadgetInfoProviderImpl implements GadgetInfoProvider<GadgetInfoImpl> {

  private static final Map<String, GadgetInfoImpl> GADGETS_CACHE = Maps.newTreeMap();

  static {
    GadgetInfoImpl pollo =
        GadgetInfoImpl.of("Pollo", "A polling gadget that allows to perform multi choice polls.",
            GadgetCategoryType.VOTING, GadgetCategoryType.OTHER, "http://goo.gl/gAHa8",
            "fabian.linz@gmail.com", "", "");
    GadgetInfoImpl mindMap =
        GadgetInfoImpl.of("MindMap", "Collaborate using a hierarchical mind map.",
            GadgetCategoryType.PRODUCTIVITY, GadgetCategoryType.OTHER, "http://goo.gl/8TqQ5",
            "Bruce Cooper", "Jeremy", "");

    GADGETS_CACHE.put(pollo.getName(), pollo);
    GADGETS_CACHE.put(mindMap.getName(), mindMap);

    GadgetInfoImpl map =
        GadgetInfoImpl
            .of("Map",
                "Collaborate on a map of placemarks, paths, and shapes with other participants. Great for planning events and trips.",
                GadgetCategoryType.MAP, GadgetCategoryType.OTHER, "http://goo.gl/0YlHY", "Google",
                "Jeremy", "");
    GADGETS_CACHE.put(map.getName(), map);

    GadgetInfoImpl mapCluster =
        GadgetInfoImpl
            .of("Map Cluster",
                "Add your location to the map, and see where everyone else is from, using a cluster visualization.",
                GadgetCategoryType.MAP, GadgetCategoryType.OTHER, "http://goo.gl/TE5LJ", "Google",
                "Jeremy", "");
    GADGETS_CACHE.put(mapCluster.getName(), mapCluster);

    GadgetInfoImpl yesNoMini =
        GadgetInfoImpl.of("Yes/No/Mini", "A miniature version of the Yes/No/Maybe gadget.",
            GadgetCategoryType.VOTING, GadgetCategoryType.OTHER, "http://goo.gl/WnAkm",
            "Zachary 'Gamer_Z.' Yaro", "Jeremy", "");
    GADGETS_CACHE.put(yesNoMini.getName(), yesNoMini);

    GadgetInfoImpl yesNoMaybePlus =
        GadgetInfoImpl.of("Yes/No/Maybe/+",
            "Same as Google's Yes/No/Maybe, only you can edit the titles and add new ones.",
            GadgetCategoryType.VOTING, GadgetCategoryType.OTHER, "http://goo.gl/fydAe",
            "everybodywave", "Jeremy", "");
    GADGETS_CACHE.put(yesNoMaybePlus.getName(), yesNoMaybePlus);

    GadgetInfoImpl yesNoMaybe =
        GadgetInfoImpl
            .of("Yes/No/Maybe",
                "Use this to ask friends if they want to join you for a party, to get their opinion on a topic, or even to petition their support for a movement.",
                GadgetCategoryType.VOTING, GadgetCategoryType.OTHER, "http://goo.gl/24YOf",
                "everybodywave", "Jeremy", "");
    GADGETS_CACHE.put(yesNoMaybe.getName(), yesNoMaybe);

    GadgetInfoImpl codeSnippet =
        GadgetInfoImpl
            .of("Code Snippet",
                "Paste and edit snippets of code within your waves, including syntax highlighting for over 20 languages. Uses SyntaxHighlighter by Alex Gorbatchev.",
                GadgetCategoryType.UTILITY, GadgetCategoryType.OTHER, "http://goo.gl/0cQcc",
                " Harry Denholm, Ishani.org", "Jeremy", "");
    GADGETS_CACHE.put(codeSnippet.getName(), codeSnippet);

    GadgetInfoImpl image =
        GadgetInfoImpl.of("Image", "Lets you insert, resize and annotate any image from the web.",
            GadgetCategoryType.IMAGE, GadgetCategoryType.UTILITY, "http://goo.gl/wCw4o",
            "everybodywave", "Jeremy", "");
    GADGETS_CACHE.put(image.getName(), image);

    GadgetInfoImpl waveTube =
        GadgetInfoImpl.of("WaveTube", "A collaborative YouTube player for Wave.",
            GadgetCategoryType.VIDEO, GadgetCategoryType.OTHER, "http://goo.gl/4k9f3",
            "everybodywave", "Jeremy", "");
    GADGETS_CACHE.put(waveTube.getName(), waveTube);

    GadgetInfoImpl napkin =
        GadgetInfoImpl.of("Napkin",
            "The Napkin Gadget is a blank canvas for collaborative doodling.",
            GadgetCategoryType.UTILITY, GadgetCategoryType.OTHER, "http://goo.gl/FMBPf",
            "Marcin Szczepanski", "Jeremy", "");
    GADGETS_CACHE.put(napkin.getName(), napkin);

    GadgetInfoImpl html =
        GadgetInfoImpl.of("HTML", "Insert HTML code directly in to a wave.",
            GadgetCategoryType.UTILITY, GadgetCategoryType.OTHER, "http://goo.gl/bN5AD",
            "MBTE Sweden AB", "Jeremy", "");
    GADGETS_CACHE.put(html.getName(), html);

    GadgetInfoImpl iFrame =
        GadgetInfoImpl.of("iFrame", "Insert IFRAME directly in to a wave.",
            GadgetCategoryType.UTILITY, GadgetCategoryType.OTHER, "http://goo.gl/jvA7z",
            "MBTE Sweden AB", "Jeremy", "");
    GADGETS_CACHE.put(iFrame.getName(), iFrame);

    GadgetInfoImpl iFrameNoBorders =
        GadgetInfoImpl.of("iFrame - no borders", "Insert iFrame DISCRETLY into a wave.",
            GadgetCategoryType.UTILITY, GadgetCategoryType.OTHER, "http://goo.gl/z46Sg",
            "Pooja Srinivas", "Jeremy", "");
    GADGETS_CACHE.put(iFrameNoBorders.getName(), iFrameNoBorders);

    GadgetInfoImpl noEdit =
        GadgetInfoImpl.of("No Edit", "Kindly readers to not edit your blip.",
            GadgetCategoryType.UTILITY, GadgetCategoryType.OTHER, "http://goo.gl/0gEjd",
            "everybodywave", "Jeremy", "");
    GADGETS_CACHE.put(noEdit.getName(), noEdit);

    GadgetInfoImpl noEditNoText =
        GadgetInfoImpl.of("No Edit - with no text", "DISCRETLY prevent the edition of your blip.",
            GadgetCategoryType.UTILITY, GadgetCategoryType.OTHER, "http://goo.gl/cNgLA",
            "Pooja Srinivas", "Jeremy", "");
    GADGETS_CACHE.put(noEditNoText.getName(), noEditNoText);

    GadgetInfoImpl wordCloud =
        GadgetInfoImpl.of("Word Cloud", "Add words and ideas into a collaborative word cloud.",
            GadgetCategoryType.UTILITY, GadgetCategoryType.OTHER, "http://goo.gl/juj3U",
            "everybodywave", "Jeremy", "");
    GADGETS_CACHE.put(wordCloud.getName(), wordCloud);

    GadgetInfoImpl trackerAgent =
        GadgetInfoImpl.of("Views tracker",
            "A small gadget that when added to a wave tracks wave views. You can display the number of views with Views Display gadget.",
            GadgetCategoryType.UTILITY, GadgetCategoryType.OTHER, "http://goo.gl/wIQKU",
            "Yuri Zelikov <yuri@waveinabox.net>", "Yuri", "");
    GADGETS_CACHE.put(trackerAgent.getName(), trackerAgent);

    GadgetInfoImpl trackerView =
        GadgetInfoImpl
            .of("Views display",
                "A display for the views tracker gadget. When added to a wave with the tracker gadget - displays how many times the wave was viewed.",
                GadgetCategoryType.UTILITY, GadgetCategoryType.OTHER, "http://goo.gl/hPDJD",
                "Yuri <yuri@waveinabox.net>", "Yuri", "");
    GADGETS_CACHE.put(trackerView.getName(), trackerView);

    GadgetInfoImpl remainingTime =
        GadgetInfoImpl
            .of("Remaining Time",
                "Create an Event and see how much time remains. Import it in your calendar and Share it on twitter, in Blogs and by mail.",
                GadgetCategoryType.UTILITY, GadgetCategoryType.OTHER, "http://goo.gl/hwHu5",
                "time-labs.com", "Jeremy", "");
    GADGETS_CACHE.put(remainingTime.getName(), remainingTime);

    GadgetInfoImpl likeButton =
        GadgetInfoImpl
            .of("Like Button",
                "A like button similar to those in Google Reader, Google Buzz, and other Google products.",
                GadgetCategoryType.VOTING, GadgetCategoryType.OTHER, "http://goo.gl/7wkly",
                "Zachary 'Gamer_Z.' Yaro", "Jeremy", "");
    GADGETS_CACHE.put(likeButton.getName(), likeButton);


    GadgetInfoImpl pinwand =
        GadgetInfoImpl.of("Pinwand",
            "Collaborate on a virtual pinwand. Add text, images, video, comments and many more.",
            GadgetCategoryType.OTHER, GadgetCategoryType.OTHER, "http://goo.gl/0PmBc",
            "Michael Hielscher", "Jeremy", "");
    GADGETS_CACHE.put(pinwand.getName(), pinwand);

    GadgetInfoImpl approver =
        GadgetInfoImpl
            .of("Approver",
                "Allow people to approve or disapprove by clicking a thumbs up or thumbs down. With 7 different themes.",
                GadgetCategoryType.VOTING, GadgetCategoryType.OTHER, "http://goo.gl/qLtYT",
                "cmdskp", "Jeremy", "");
    GADGETS_CACHE.put(approver.getName(), approver);

    GadgetInfoImpl iLikeIt =
        GadgetInfoImpl
            .of("I Like It!",
                "Adds a favorites button to your wave, so that you and everyone else in the wave can indicate that they like the wave - with a cute smiley face!",
                GadgetCategoryType.VOTING, GadgetCategoryType.OTHER, "http://goo.gl/aXybB",
                "Jaken", "Jeremy", "");
    GADGETS_CACHE.put(iLikeIt.getName(), iLikeIt);

    GadgetInfoImpl diagramEditor =
        GadgetInfoImpl
            .of("Diagram Editor",
                "Create cool diagrams (UML, BPMN, EPC, FMC, etc.) together with your friends in Google Wave!",
                GadgetCategoryType.PRODUCTIVITY, GadgetCategoryType.OTHER, "http://goo.gl/HvuA4",
                "processWave.org", "Jeremy", "");
    GADGETS_CACHE.put(diagramEditor.getName(), diagramEditor);

    GadgetInfoImpl wordNetwork =
        GadgetInfoImpl
            .of("Word Network",
                "Collaborate on a linking words together and organizing concepts. Double Click on any word to link it to another or double click on the background to c...",
                GadgetCategoryType.PRODUCTIVITY, GadgetCategoryType.OTHER, "http://goo.gl/6vwxY",
                "antimatter15", "Jeremy", "");
    GADGETS_CACHE.put(wordNetwork.getName(), wordNetwork);

    GadgetInfoImpl googleFight =
        GadgetInfoImpl
            .of("Google Fight!",
                "Google Fights - the title should explain it all. Another gadget by www.processWave.org.",
                GadgetCategoryType.UTILITY, GadgetCategoryType.OTHER, "http://goo.gl/Mg26y",
                "Michael Goderbauer", "Jeremy", "");
    GADGETS_CACHE.put(googleFight.getName(), googleFight);

    GadgetInfoImpl poll =
        GadgetInfoImpl
            .of("Poll",
                "Poll participants for their opinion. Supports both single and multiple selection polls, and optionally allows votes to be changed after they're cast.",
                GadgetCategoryType.VOTING, GadgetCategoryType.OTHER, "http://goo.gl/0G7qU",
                "Eric Williams", "Jeremy", "");
    GADGETS_CACHE.put(poll.getName(), poll);

    GadgetInfoImpl chart =
        GadgetInfoImpl.of("Chart", "Lets you insert various charts into wave.",
            GadgetCategoryType.PRODUCTIVITY, GadgetCategoryType.OTHER, "http://goo.gl/Tb7Q3",
            "everybodywave", "Jeremy", "");
    GADGETS_CACHE.put(chart.getName(), chart);

    GadgetInfoImpl retroChat =
        GadgetInfoImpl.of("Retro Chat", "Chat room gadget for old-fashioned IMing in Wave.",
            GadgetCategoryType.PRODUCTIVITY, GadgetCategoryType.OTHER, "http://goo.gl/AW0Vm",
            "Charles Lehner", "Jeremy", "");
    GADGETS_CACHE.put(retroChat.getName(), retroChat);

    GadgetInfoImpl picasa =
        GadgetInfoImpl.of("Picasa", "Add a Picasa photo album to a wave.",
            GadgetCategoryType.IMAGE, GadgetCategoryType.OTHER, "http://goo.gl/NUYIs",
            "Genliang Guan, University of Sydney", "Jeremy", "");
    GADGETS_CACHE.put(picasa.getName(), picasa);

    GadgetInfoImpl googlUrlShortener =
        GadgetInfoImpl.of("Goo.gl URL Shortener",
            "Shorten url with goo.gl, the new Google url shortener.(http://goo.gl/)",
            GadgetCategoryType.UTILITY, GadgetCategoryType.OTHER, "http://goo.gl/oRonD",
            "haru1ban", "Jeremy", "");
    GADGETS_CACHE.put(googlUrlShortener.getName(), googlUrlShortener);

    GadgetInfoImpl pacMan =
        GadgetInfoImpl.of("PacMan", "Play Pacman inside a Wave.", GadgetCategoryType.GAME,
            GadgetCategoryType.OTHER, "http://goo.gl/RFzqt", "www.schulz.dk", "Jeremy", "");
    GADGETS_CACHE.put(pacMan.getName(), pacMan);

    GadgetInfoImpl superMarioBros =
        GadgetInfoImpl.of("Super Mario Bros", "Play Super Mario Bros inside a Wave.",
            GadgetCategoryType.GAME, GadgetCategoryType.OTHER, "http://goo.gl/Ca6d0",
            "www.schulz.dk", "Jeremy", "");
    GADGETS_CACHE.put(superMarioBros.getName(), superMarioBros);

    GadgetInfoImpl sudoku =
        GadgetInfoImpl
            .of("Sudoku",
                "A cool game to share with your friends. Solve challenging Sudoku boards together and see who is the best Sudoku player!",
                GadgetCategoryType.GAME, GadgetCategoryType.OTHER, "http://goo.gl/FxORa",
                "LabPixies", "Jeremy", "");
    GADGETS_CACHE.put(sudoku.getName(), sudoku);

    GadgetInfoImpl searchSharedWaves =
        GadgetInfoImpl
            .of("Search shared waves",
                "\"Google\" the shared waves on waveinabox.net with a customized Google search gadget.",
                GadgetCategoryType.UTILITY, GadgetCategoryType.OTHER, "http://goo.gl/1sMZy",
                "Yuri Zelikov <yuri@waveinabox.net>", "Yuri", "");
    GADGETS_CACHE.put(searchSharedWaves.getName(), searchSharedWaves);

    GadgetInfoImpl accuWeather =
        GadgetInfoImpl
            .of("AccuWeather",
                "The AccuWeather Wave Gadget is the perfect companion for trip planning. Select a location and date, and the gadget will return a forecast.",
                GadgetCategoryType.UTILITY, GadgetCategoryType.OTHER, "http://goo.gl/iODX9",
                "AccuWeather.com", "Jeremy", "");
    GADGETS_CACHE.put(accuWeather.getName(), accuWeather);

    GadgetInfoImpl decingGadget =
        GadgetInfoImpl
            .of("Decing",
                "Need to make a decision? Arrange a secret vote inside of a wave. Participants' votes are not shared to others.",
                GadgetCategoryType.UTILITY, GadgetCategoryType.OTHER, "http://goo.gl/MghBe",
                "Decing.com", "Jeremy", "");
    GADGETS_CACHE.put(decingGadget.getName(), decingGadget);

    GadgetInfoImpl groceryList =
        GadgetInfoImpl
            .of("Grocery List",
                "Use this for your grocery list needs - share with your family, sort the list automatically, print and take it to the store, and more!",
                GadgetCategoryType.UTILITY, GadgetCategoryType.OTHER, "http://goo.gl/kna0V",
                "Quaker", "Jeremy", "");
    GADGETS_CACHE.put(groceryList.getName(), groceryList);

    GadgetInfoImpl likey =
        GadgetInfoImpl
            .of("Likey",
                "A simple like/dislike Wave gadget that can be added to a blip for intuitive user rating.",
                GadgetCategoryType.VOTING, GadgetCategoryType.PRODUCTIVITY, "http://goo.gl/KrlJE",
                "Ben Griffiths", "Jeremy", "");
    GADGETS_CACHE.put(likey.getName(), likey);

    GadgetInfoImpl paginatorGadget =
        GadgetInfoImpl
            .of("Paginator",
                "The Paginator is a compact reading aid Gadget for Google Wave which paginates large amounts of text and bookmarks the wave viewer's current location",
                GadgetCategoryType.UTILITY, GadgetCategoryType.PRODUCTIVITY, "http://goo.gl/Ol9GW",
                "Dan Smith", "Jeremy", "");
    GADGETS_CACHE.put(paginatorGadget.getName(), paginatorGadget);

    GadgetInfoImpl piano =
        GadgetInfoImpl.of("Piano", "A real-time piano gadget.", GadgetCategoryType.MUSIC,
            GadgetCategoryType.OTHER, "http://goo.gl/x9vHX", "everybodywave", "Jeremy", "");
    GADGETS_CACHE.put(piano.getName(), piano);

    GadgetInfoImpl team =
        GadgetInfoImpl.of("Team ",
            "Lets you create a list of wave participants in a particular order.",
            GadgetCategoryType.PRODUCTIVITY, GadgetCategoryType.OTHER, "http://goo.gl/VJnId",
            "everybodywave", "Jeremy", "");
    GADGETS_CACHE.put(team.getName(), team);

    GadgetInfoImpl vectorEditor =
        GadgetInfoImpl
            .of("Vector Editor",
                "This gadget is useful for creating graphics. Shapes can be added, resized, moved, and rotated. The application supports Lines, Freeform, Polygons, Rec...",
                GadgetCategoryType.PRODUCTIVITY, GadgetCategoryType.OTHER, "http://goo.gl/VSkn5",
                "antimatter15", "Jeremy", "");
    GADGETS_CACHE.put(vectorEditor.getName(), vectorEditor);

    GadgetInfoImpl yourBrainStormer =
        GadgetInfoImpl.of("YourBrainStormer",
            "Share your ideas more efficiently! Special Thanks to JiWei, Ze Zhou and Lin Myat.",
            GadgetCategoryType.PRODUCTIVITY, GadgetCategoryType.OTHER, "http://goo.gl/rtnFD",
            "WyeMun and KaiLin", "Jeremy", "");
    GADGETS_CACHE.put(yourBrainStormer.getName(), yourBrainStormer);

    GadgetInfoImpl ratings =
        GadgetInfoImpl.of("Ratings",
            "Add your vote from 1-5 stars, and see the total votes from others.",
            GadgetCategoryType.VOTING, GadgetCategoryType.OTHER, "http://goo.gl/uQ9vi", "Google",
            "Jeremy", "");
    GADGETS_CACHE.put(ratings.getName(), ratings);

    GadgetInfoImpl drawBoard =
        GadgetInfoImpl.of("Draw Board", "Draw images collaboratively with other users.",
            GadgetCategoryType.UTILITY, GadgetCategoryType.PRODUCTIVITY, "http://goo.gl/uQ9vi",
            "Miron Sadziak", "Jeremy", "");
    GADGETS_CACHE.put(drawBoard.getName(), drawBoard);

    GadgetInfoImpl colcrop =
        GadgetInfoImpl
            .of("Colcrop",
                "Cover as many cells as possible, by choosing adjacent colors. Play against a participant or the computer. Computer Level 4 is pretty hard to defeat.",
                GadgetCategoryType.GAME, GadgetCategoryType.OTHER, "http://goo.gl/Vh9ME",
                "Alexis Vuillemin", "Jeremy", "");
    GADGETS_CACHE.put(colcrop.getName(), colcrop);

    GadgetInfoImpl bones =
        GadgetInfoImpl
            .of("Bones",
                "Bones provides graphical dice that any participant in a wave can set up to be rolled by themselves or others. Results are shared with everyone.",
                GadgetCategoryType.UTILITY, GadgetCategoryType.OTHER, "http://goo.gl/GbhWR",
                "10x10 Room", "Jeremy", "");
    GADGETS_CACHE.put(bones.getName(), bones);

  }

  public static SimpleGadgetInfoProviderImpl create() {
    return new SimpleGadgetInfoProviderImpl();
  }

  private SimpleGadgetInfoProviderImpl() {

  }

  @Override
  public Map<String, GadgetInfoImpl> retrieveGadgetInfo(GadgetCategoryType category) {
    Map<String, GadgetInfoImpl> gadgetInfoList = null;
    if (GadgetCategoryType.ALL.equals(category)) {
      gadgetInfoList = CollectionUtils.newHashMap(GADGETS_CACHE);
    } else {
      gadgetInfoList = CollectionUtils.newHashMap();
      for (Map.Entry<String, GadgetInfoImpl> entry : GADGETS_CACHE.entrySet()) {
        GadgetInfoImpl info = entry.getValue();
        if (info.getPrimaryCategory().equals(category)
            || info.getSecondaryCategory().equals(category)) {
          gadgetInfoList.put(info.getName(), info);
        }
      }
    }
    return gadgetInfoList;
  }
}

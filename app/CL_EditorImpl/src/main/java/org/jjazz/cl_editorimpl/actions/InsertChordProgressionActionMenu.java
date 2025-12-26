/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 *  This file is part of the JJazzLab software.
 *   
 *  JJazzLab is free software: you can redistribute it and/or modify
 *  it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *  as published by the Free Software Foundation, either version 3 of the License, 
 *  or (at your option) any later version.
 *
 *  JJazzLab is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 * 
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 * 
 *  Contributor(s): 
 */
package org.jjazz.cl_editorimpl.actions;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.cl_editor.api.CL_Editor;
import org.jjazz.cl_editor.api.CL_EditorTopComponent;
import org.jjazz.cl_editor.api.CL_Selection;
import org.jjazz.filedirectorymanager.api.FileDirectoryManager;
import org.jjazz.harmony.api.Position;
import org.jjazz.importers.api.TextReader;
import org.jjazz.rhythm.api.UserErrorGenerationException;
import org.jjazz.rhythmmusicgeneration.api.ChordSequence;
import org.jjazz.rhythmmusicgeneration.api.SongChordSequence;
import org.jjazz.song.api.Song;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.utilities.api.IntRange;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.utilities.api.Utilities;
import org.netbeans.api.annotations.common.StaticResource;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.NbPreferences;
import org.openide.util.actions.Presenter;

/**
 * Action menu to insert chord progression.
 * <p>
 */
@ActionID(category = "JJazz", id = "org.jjazz.cl_editor.actions.insertchordprogression")
@ActionRegistration(displayName = "not_used", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/BarInsert", position = 200)
        })
public final class InsertChordProgressionActionMenu extends AbstractAction implements Presenter.Popup, ContextAwareAction
{

    private static File CHORD_PROGRESSION_TEXT_FILE;
    @StaticResource(relative = true)
    private static final String CHORD_PROGRESSION_RESOURCE = "resources/ChordProgressions.txt";
    private static final String CHORD_PROGRESSION_TEXT_FILE_NAME = "ChordProgressions.txt";
    private static final String PREF_CHORD_PROGRESSION_WARNING_SHOWN = "PrefChordProgressionWarningShown";
    private JMenu menu;
    private static final Preferences prefs = NbPreferences.forModule(InsertChordProgressionActionMenu.class);
    private static final Logger LOGGER = Logger.getLogger(InsertChordProgressionActionMenu.class.getSimpleName());

    public InsertChordProgressionActionMenu()
    {
        // Not used besides for creating the ContextAwareAction
    }

    public InsertChordProgressionActionMenu(Lookup context)
    {
        Objects.requireNonNull(context);
        menu = new JMenu(ResUtil.getString(getClass(), "CTL_InsertChordProgression"));


        var selection = new CL_Selection(context);
        boolean b = selection.getSelectedBarIndexesWithinCls().size() == 1;
        setEnabled(b);
        menu.setEnabled(b);
        if (!b)
        {
            return;
        }

        prepareMenu(menu, selection);
    }

    @Override
    public Action createContextAwareInstance(Lookup lkp)
    {
        return new InsertChordProgressionActionMenu(lkp);
    }

    @Override
    public void actionPerformed(ActionEvent ae)
    {
        // Not used
    }


    // ============================================================================================= 
    // Presenter.Popup implementation
    // =============================================================================================      
    @Override
    public JMenuItem getPopupPresenter()
    {
        return menu;
    }

    // ============================================================================================= 
    // Private methods
    // =============================================================================================    

    private JMenu prepareMenu(JMenu menu, CL_Selection selection)
    {
        File chordProgressionFile = getChordProgressionTextFile();
        if (chordProgressionFile == null)
        {
            return menu;
        }

        // First menu entry opens the chord progression text file in an editor
        String txt = ResUtil.getString(getClass(), "OpenChordProgressionFile", chordProgressionFile.getAbsolutePath());
        JMenuItem mi = new JMenuItem(txt);
        mi.addActionListener(e -> Utilities.systemOpenFile(chordProgressionFile, false, Utilities.SAFE_OPEN_EXTENSIONS));
        menu.add(mi);


        // Read file
        List<String> lines;
        try
        {
            lines = Files.readAllLines(chordProgressionFile.toPath());
        } catch (IOException ex)
        {
            LOGGER.log(Level.SEVERE, "Error while reading chordProgressionFile={0} ex={1}", new Object[]
            {
                chordProgressionFile.getAbsolutePath(), ex.getMessage()
            });
            return menu;
        }


        prepareMenu(menu, selection, lines);

        return menu;
    }

    /**
     * Update the specified menu by adding items and submenus based on lines.
     *
     * @param menu
     * @param selection
     * @param lines
     */
    private void prepareMenu(JMenu menu, CL_Selection selection, List<String> lines)
    {
        String[] currentPath = null;    // null is root menu
        Map<String, JMenu> cache = new HashMap<>();

        for (var line : lines)
        {
            // Remove comments and blank lines
            line = line.replaceFirst("//.*", "").trim();
            if (line.isBlank())
            {
                continue;
            }

            if (line.charAt(0) != '|')
            {
                // Category line, set current path               
                currentPath = line.split("\\s*--\\s*");
            } else
            {
                // grid
                var cSeq = getChordSequence(line);
                if (checkChordSequence(cSeq))
                {
                    var mi = new JMenuItem(line);
                    mi.setFont(new java.awt.Font("Courier New", 0, 11)); // NOI18N
                    mi.addActionListener(ae -> insertChordProgression(selection, cSeq));

                    JMenu parentMenu = ensureMenuHierarchy(menu, cache, currentPath);
                    parentMenu.add(mi);
                } else
                {
                    LOGGER.log(Level.WARNING, "buildMenu() Invalid chord progression line={0} ignored", line);
                }
            }
        }
    }

    /**
     * Ensure menus exist for the given path.
     * <p>
     * Returns the deepest JMenu (leaf) or root if path is empty.
     *
     * @param root
     * @param cache
     * @param pathParts
     * @return
     */
    private JMenu ensureMenuHierarchy(JMenu root, Map<String, JMenu> cache, String[] pathParts)
    {
        if (pathParts == null || pathParts.length == 0)
        {
            return root;
        }

        Container parent = root;
        StringJoiner joiner = new StringJoiner(",");
        for (String part : pathParts)
        {
            joiner.add(part);
            String key = joiner.toString();

            JMenu m = cache.get(key);
            if (m == null)
            {
                m = new JMenu(part);
                parent.add(m);
                cache.put(key, m);
            }
            parent = m;
        }

        return (JMenu) parent;
    }

    /**
     * File is expected to be at the root of the JJazzLab user directory -if not present, create one from CHORD_PROGRESSION_RESOURCE.
     * <p>
     * @return
     */
    private File getChordProgressionTextFile()
    {
        if (CHORD_PROGRESSION_TEXT_FILE == null)
        {
            File jjazzLabDir = FileDirectoryManager.getInstance().getJJazzLabUserDirectory();
            if (jjazzLabDir == null)
            {
                LOGGER.severe("getChordProgressionTextFile() Can not get chord progression file because jjazzLabDir is null");
                return null;
            }
            CHORD_PROGRESSION_TEXT_FILE = new File(jjazzLabDir, CHORD_PROGRESSION_TEXT_FILE_NAME);
        }

        if (!CHORD_PROGRESSION_TEXT_FILE.exists()
                && !org.jjazz.utilities.api.Utilities.copyResource(getClass(), CHORD_PROGRESSION_RESOURCE, CHORD_PROGRESSION_TEXT_FILE.toPath()))
        {
            LOGGER.log(Level.SEVERE, "getChordProgressionTextFile() Could not " + CHORD_PROGRESSION_RESOURCE + " to {0}",
                    CHORD_PROGRESSION_TEXT_FILE.getAbsolutePath());
            CHORD_PROGRESSION_TEXT_FILE = null;
        }

        return CHORD_PROGRESSION_TEXT_FILE;
    }


    /**
     * Convert the line in a chord sequence.
     *
     * @param line
     * @return
     */
    private ChordSequence getChordSequence(String line)
    {
        ChordSequence res = null;
        Song song = new TextReader(line).readSong();
        if (song != null)
        {
            try
            {
                res = new SongChordSequence(song, null);
            } catch (UserErrorGenerationException ex)
            {
                LOGGER.log(Level.WARNING, "getChordSequence() Can not build SongChordSequence from line={0}. ex={1}", new Object[]
                {
                    line, ex.getMessage()
                });
            }
        }
        return res;
    }

    /**
     * Perform the undoable action.
     *
     * @param selection
     * @param cSeq
     */
    private void insertChordProgression(CL_Selection selection, ChordSequence cSeq)
    {
        var cls = selection.getChordLeadSheet();
        var modelBarIndex = selection.getMinBarIndexWithinCls();

        LOGGER.log(Level.FINE, "insertChordProgression() cls={0} modelBarIndex={1} cSeq={2}", new Object[]
        {
            cls, modelBarIndex, cSeq
        });

        CL_Editor editor = CL_EditorTopComponent.getActive().getEditor();
        Song song = editor.getSongModel();
        List<CLI_ChordSymbol> addedChords = new ArrayList<>();
        var barRange = cSeq.getBarRange();


        var um = JJazzUndoManagerFinder.getDefault().get(cls);
        um.startCEdit(menu.getText());


        // resize if required
        int newSize = modelBarIndex + barRange.size();
        if (newSize > cls.getSizeInBars())
        {
            try
            {
                cls.setSizeInBars(newSize);
            } catch (UnsupportedEditException ex)
            {
                LOGGER.log(Level.SEVERE, "insertChordProgression() Can not set size={0} of cls. ex={1}", new Object[]
                {
                    newSize, ex.getMessage()
                });
                String msg = "Impossible to resize chord leadsheet.\n" + ex.getLocalizedMessage();
                um.abortCEdit(menu.getText(), msg);
                return;
            }
        }


        // Add chords
        for (int bar : barRange)
        {
            int destBar = modelBarIndex + bar;

            // Clear existing chords
            var oldChords = cls.getItems(destBar, destBar, CLI_ChordSymbol.class);
            oldChords.forEach(cliCs -> cls.removeItem(cliCs));

            // Add the new ones
            var subSeq = cSeq.subSequence(new IntRange(bar, bar), false);
            var cliSection = cls.getSection(destBar);
            TimeSignature destTs = cliSection.getData().getTimeSignature();

            if (!subSeq.isEmpty())
            {
                var cliCs = subSeq.getFirst();
                var pos = cliCs.getPosition();
                assert pos.isFirstBarBeat();
                var newPos = cliCs.getPosition().setBar(destBar);
                var newCliCs = (CLI_ChordSymbol) cliCs.getCopy(null, newPos);
                cls.addItem(newCliCs);
                addedChords.add(newCliCs);
            }
            if (subSeq.size() == 2)
            {
                var cliCs = subSeq.getLast();
                var beat = destTs.getHalfBarBeat(guessSwing(cliSection, song));
                var newPos = new Position(destBar, beat);
                var newCliCs = (CLI_ChordSymbol) cliCs.getCopy(null, newPos);
                addedChords.add(newCliCs);
                cls.addItem(newCliCs);
            }
        }

        um.endCEdit(menu.getText());


        if (addedChords.isEmpty())
        {
            return;
        }

        // Select all added chords so that use can easily transpose them
        editor.clearSelection();
        editor.selectItems(addedChords, true);


        if (!prefs.getBoolean(PREF_CHORD_PROGRESSION_WARNING_SHOWN, false))
        {
            String msg = ResUtil.getString(getClass(), "TransposeChordProgression");
            NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.INFORMATION_MESSAGE);
            DialogDisplayer.getDefault().notify(nd);
            prefs.putBoolean(PREF_CHORD_PROGRESSION_WARNING_SHOWN, true);
        }

    }

    /**
     * Check that each bar has at most 2 chords
     *
     * @param cSeq
     * @return
     */
    private boolean checkChordSequence(ChordSequence cSeq)
    {
        boolean b = cSeq.getBarRange().stream()
                .allMatch(bar -> cSeq.subSequence(new IntRange(bar, bar), false).size() <= 2);
        return b;
    }

    private boolean guessSwing(CLI_Section cliSection, Song song)
    {
        boolean b = song.getSongStructure().getSongParts().stream()
                .filter(spt -> spt.getParentSection() == cliSection)
                .allMatch(spt -> !spt.getRhythm().getFeatures().division().isBinary());
        return b;
    }


}

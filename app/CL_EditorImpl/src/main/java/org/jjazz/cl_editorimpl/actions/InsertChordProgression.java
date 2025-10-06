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

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.event.ClsChangeEvent;
import org.jjazz.chordleadsheet.api.event.SizeChangedEvent;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.cl_editor.api.CL_ContextAction;
import org.jjazz.cl_editor.api.CL_Editor;
import org.jjazz.cl_editor.api.CL_EditorTopComponent;
import org.jjazz.cl_editor.api.CL_SelectionUtilities;
import org.jjazz.filedirectorymanager.api.FileDirectoryManager;
import org.jjazz.harmony.api.Position;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.importers.api.TextReader;
import org.jjazz.rhythm.api.UserErrorGenerationException;
import org.jjazz.rhythmmusicgeneration.api.ChordSequence;
import org.jjazz.rhythmmusicgeneration.api.SongChordSequence;
import org.jjazz.song.api.Song;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.utilities.api.IntRange;
import org.jjazz.utilities.api.ResUtil;
import org.netbeans.api.annotations.common.StaticResource;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbPreferences;
import org.openide.util.actions.Presenter;

/**
 * Insert chord progression as submenu.
 * <p>
 */
@ActionID(category = "JJazz", id = "org.jjazz.cl_editor.actions.insertchordprogression")
@ActionRegistration(displayName = "not_used", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/BarInsert", position = 200)
        })
public class InsertChordProgression extends CL_ContextAction implements Presenter.Popup
{

    private static File CHORD_PROGRESSION_TEXT_FILE;
    @StaticResource(relative = true)
    private static final String CHORD_PROGRESSION_RESOURCE = "resources/ChordProgressions.txt";
    private static final String CHORD_PROGRESSION_TEXT_FILE_NAME = "ChordProgressions.txt";
    private static final String PREF_CHORD_PROGRESSION_WARNING_SHOWN = "PrefChordProgressionWarningShown";
    private JMenu menu;
    private static final Logger LOGGER = Logger.getLogger(InsertChordProgression.class.getSimpleName());
    private static final Preferences prefs = NbPreferences.forModule(InsertChordProgression.class);

    @Override
    protected void configureAction()
    {
        putValue(NAME, ResUtil.getString(getClass(), "CTL_InsertChordProgression"));
        putValue(LISTENING_TARGETS, EnumSet.of(ListeningTarget.BAR_SELECTION, ListeningTarget.ACTIVE_CLS_CHANGES));        
    }

    @Override
    protected void actionPerformed(ActionEvent ae, ChordLeadSheet cls, CL_SelectionUtilities selection)
    {
        // useless
    }

    @Override
    public void selectionChange(CL_SelectionUtilities selection)
    {
        boolean b = selection.getSelectedBarIndexesWithinCls().size() == 1;
        setEnabled(b);
        menu.setEnabled(b);
    }

    @Override
    public void chordLeadSheetChanged(ClsChangeEvent event)
    {
        if (event instanceof SizeChangedEvent)
        {
            selectionChange(getSelection());
        }
    }

    // ============================================================================================= 
    // Presenter.Popup implementation
    // =============================================================================================      
    @Override
    public JMenuItem getPopupPresenter()
    {
        if (menu == null)
        {
            menu = buildMenu(getActionName());
        }
        menu.setEnabled(isEnabled());
        return menu;
    }

    // ============================================================================================= 
    // Private methods
    // ============================================================================================= 
    private JMenu buildMenu(String menuTitle)
    {
        menu = new JMenu(menuTitle);

        File chordProgressionFile = getChordProgressionTextFile();
        if (chordProgressionFile == null)
        {
            return menu;
        }


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


        // Convert to menu items
        JMenu currentSubMenu = null;
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
                // Category
                currentSubMenu = new JMenu(line);
                menu.add(currentSubMenu);
            } else
            {
                // grid
                var cSeq = getChordSequence(line);
                if (checkChordSequence(cSeq))
                {
                    var mi = new JMenuItem(line);
                    mi.setFont(new java.awt.Font("Courier New", 0, 11)); // NOI18N
                    mi.addActionListener(ae -> insertChordProgression(cSeq));
                    (currentSubMenu == null ? menu : currentSubMenu).add(mi);
                } else
                {
                    LOGGER.log(Level.WARNING, "buildMenu() Invalid chord progression line={0} ignored", line);
                }
            }
        }
        return menu;
    }


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
     * @param cSeq
     */
    private void insertChordProgression(ChordSequence cSeq)
    {
        var cls = getActiveChordLeadSheet();
        var modelBarIndex = getSelection().getMinBarIndexWithinCls();

        LOGGER.log(Level.FINE, "insertChordProgression() cls={0} modelBarIndex={1} cSeq={2}", new Object[]
        {
            cls, modelBarIndex, cSeq
        });

        CL_Editor editor = CL_EditorTopComponent.getActive().getEditor();
        Song song = editor.getSongModel();
        List<CLI_ChordSymbol> addedChords = new ArrayList<>();
        var barRange = cSeq.getBarRange();


        JJazzUndoManagerFinder.getDefault().get(cls).startCEdit(getActionName());


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
                JJazzUndoManagerFinder.getDefault().get(cls).endCEdit(getActionName());
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

        JJazzUndoManagerFinder.getDefault().get(cls).endCEdit(getActionName());


        if (addedChords.isEmpty())
        {
            return;
        }

        // Select all added chords so that use can easily transpose them
        editor.unselectAll();
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

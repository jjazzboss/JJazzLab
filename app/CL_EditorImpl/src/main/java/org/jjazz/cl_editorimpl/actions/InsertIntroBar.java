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
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import static javax.swing.Action.ACCELERATOR_KEY;
import static javax.swing.Action.NAME;
import javax.swing.KeyStroke;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.event.ClsChangeEvent;
import org.jjazz.chordleadsheet.api.event.SizeChangedEvent;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.cl_editor.api.CL_ContextAction;
import static org.jjazz.cl_editor.api.CL_ContextAction.LISTENING_TARGETS;
import org.jjazz.cl_editor.api.CL_Editor;
import org.jjazz.cl_editor.api.CL_EditorClientProperties;
import org.jjazz.cl_editor.api.CL_EditorTopComponent;
import org.jjazz.cl_editor.api.CL_Selection;
import org.jjazz.harmony.api.Position;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Variation;
import org.jjazz.song.api.Song;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.utilities.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

/**
 * Insert an intro bar at bar 0.
 * <p>
 * This action inserts one bar at the beginning of the chord leadsheet, renames the initial section to "Intro",
 * copies the chord symbol from bar 1 to bar 0, sets the section at bar 1 to start on a new line,
 * and sets the variation to "Intro-A" for the new intro SongPart.
 */
@ActionID(category = "JJazz", id = "org.jjazz.cl_editor.actions.insertintrobar")
@ActionRegistration(displayName = "not_used", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/BarInsert", position = 105)
        })
public class InsertIntroBar extends CL_ContextAction
{

    private static final Logger LOGGER = Logger.getLogger(InsertIntroBar.class.getSimpleName());

    @Override
    protected void configureAction()
    {
        putValue(NAME, ResUtil.getString(getClass(), "CTL_InsertIntroBar"));
        putValue(LISTENING_TARGETS, EnumSet.of(CL_ContextAction.ListeningTarget.BAR_SELECTION, CL_ContextAction.ListeningTarget.ACTIVE_CLS_CHANGES));
    }

    @Override
    protected void actionPerformed(ActionEvent ae, ChordLeadSheet cls, CL_Selection selection)
    {
        CL_Editor editor = CL_EditorTopComponent.getActive().getEditor();
        Song song = editor.getSongModel();
        SongStructure ss = song.getSongStructure();
        
        var um = JJazzUndoManagerFinder.getDefault().get(cls);
        um.startCEdit(getActionName());

        try
        {
            // Insert one bar at position 0
            cls.insertBars(0, 1);

            // Get the initial section (now at bar 0)
            CLI_Section introSection = cls.getSection(0);
            
            // Determine unique section name
            String introName = "Intro";
            if (isSectionNameUsed(cls, introName))
            {
                // Find a unique name
                int counter = 2;
                while (isSectionNameUsed(cls, introName + counter))
                {
                    counter++;
                }
                introName = introName + counter;
            }
            
            // Set the section name
            cls.setSectionName(introSection, introName);

            // Copy chord symbol from bar 1 to bar 0
            List<CLI_ChordSymbol> chordsAtBar1 = cls.getItems(1, 1, CLI_ChordSymbol.class);
            if (!chordsAtBar1.isEmpty())
            {
                // Get the first chord symbol at bar 1
                CLI_ChordSymbol firstChord = chordsAtBar1.get(0);
                // Create a copy at bar 0
                Position newPos = new Position(0, 0);
                CLI_ChordSymbol newChord = (CLI_ChordSymbol) firstChord.getCopy(null, newPos);
                cls.addItem(newChord);
            }

            // Set the section at bar 1 to start on a new line
            CLI_Section sectionAtBar1 = cls.getSection(1);
            CL_EditorClientProperties.setSectionIsOnNewLine(sectionAtBar1, true);

            // Set variation to "Intro-A" for the SongPart at bar 0
            SongPart introSongPart = ss.getSongPart(0);
            if (introSongPart != null)
            {
                var rhythm = introSongPart.getRhythm();
                RP_SYS_Variation rpVariation = RP_SYS_Variation.getVariationRp(rhythm);
                
                if (rpVariation != null)
                {
                    String targetVariation = "Intro-A";
                    String variationToUse = targetVariation;
                    
                    // Check if "Intro-A" is a valid variation for this rhythm
                    if (!rpVariation.getPossibleValues().contains(targetVariation))
                    {
                        // Use any available variation (the first one)
                        variationToUse = (String) rpVariation.getDefaultValue();
                    }
                    
                    ss.setRhythmParameterValue(introSongPart, rpVariation, variationToUse);
                }
            }

            um.endCEdit(getActionName());
            
        } catch (UnsupportedEditException ex)
        {
            LOGGER.log(Level.WARNING, "actionPerformed() Error inserting intro bar: {0}", ex.getMessage());
            String msg = "Failed to insert intro bar: " + ex.getLocalizedMessage();
            um.abortCEdit(getActionName(), msg);
        }
    }

    @Override
    public void selectionChange(CL_Selection selection)
    {
        // Enable only if at least one bar is selected
        boolean b = selection.isBarSelected();
        setEnabled(b);
    }

    @Override
    public void chordLeadSheetChanged(ClsChangeEvent event)
    {
        if (event instanceof SizeChangedEvent)
        {
            selectionChange(getSelection());
        }
    }

    /**
     * Check if a section name is already used in the chord leadsheet.
     */
    private boolean isSectionNameUsed(ChordLeadSheet cls, String name)
    {
        return cls.getItems(CLI_Section.class).stream()
                .anyMatch(section -> section.getData().getName().equals(name));
    }
}

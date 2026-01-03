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
import java.util.logging.Logger;
import static javax.swing.Action.NAME;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.CLI_Factory;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.cl_editor.api.CL_ContextAction;
import static org.jjazz.cl_editor.api.CL_ContextAction.LISTENING_TARGETS;
import org.jjazz.cl_editor.api.CL_Editor;
import org.jjazz.cl_editor.api.CL_EditorClientProperties;
import org.jjazz.cl_editor.api.CL_EditorTopComponent;
import org.jjazz.cl_editor.api.CL_Selection;
import org.jjazz.harmony.api.Position;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Variation;
import org.jjazz.song.api.Song;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.utilities.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.Exceptions;

/**
 * Insert a start bar/section whose corresponding SongPart is set to use the Intro A-1 variation.
 * <p>
 */
@ActionID(category = "JJazz", id = "org.jjazz.cl_editor.actions.insertendbar")
@ActionRegistration(displayName = "not_used", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/BarInsert", position = 6)
        })
public class InsertEndBar extends CL_ContextAction
{
    
    private static final String DEFAULT_END_SECTION_NAME = "End";
    private static final Logger LOGGER = Logger.getLogger(InsertEndBar.class.getSimpleName());
    
    
    @Override
    protected void configureAction()
    {
        putValue(NAME, ResUtil.getString(getClass(), "CTL_InsertEndBar"));
        putValue(LISTENING_TARGETS, EnumSet.of(CL_ContextAction.ListeningTarget.BAR_SELECTION));
    }
    
    @Override
    protected void actionPerformed(ActionEvent ae, ChordLeadSheet cls, CL_Selection selection)
    {
        CL_Editor editor = CL_EditorTopComponent.getActive().getEditor();
        Song song = editor.getSongModel();
        SongStructure sgs = song.getSongStructure();
        var um = JJazzUndoManagerFinder.getDefault().get(cls);
        
        try
        {
            
            um.startCEdit(getActionName());


            // Add one extra bar
            int oldSize = cls.getSizeInBars();
            cls.setSizeInBars(oldSize + 1);          // throws UnsupportedEditException


            // Add end section
            CLI_Section lastSection = cls.getSection(oldSize);
            CLI_Section endSection = CLI_Factory.getDefault().createSection(computeUniqueSectionName(cls),
                    lastSection.getData().getTimeSignature(),
                    oldSize,
                    cls);
            cls.addSection(endSection);         // throws UnsupportedEditException


            // Copy last chord symbol to last bar
            CLI_ChordSymbol lastChord = cls.getLastItemBefore(endSection, CLI_ChordSymbol.class, i -> true);
            if (lastChord != null)
            {
                Position newPos = new Position(oldSize);
                CLI_ChordSymbol newChord = (CLI_ChordSymbol) lastChord.getCopy(null, newPos);
                cls.addItem(newChord);
            }

            // Make sure end SongPart is last
            var allSpts = sgs.getSongParts();
            var sptEnd = allSpts.stream()
                    .filter(spt -> spt.getParentSection() == endSection)
                    .findAny()
                    .orElseThrow();
            if (allSpts.getLast() != sptEnd)
            {
                // Move the SongPart at the end
                sgs.removeSongParts(List.of(sptEnd));
                sptEnd = sptEnd.getCopy(null, sgs.getSizeInBars(), 1, endSection);
                sgs.addSongParts(List.of(sptEnd));
            }
            
            // Try to set its variation to "Ending A-1"
            var rpVariation = RP_SYS_Variation.getVariationRp(sptEnd.getRhythm());            
            if (rpVariation != null)
            {
                String variation = "Ending A-1";
                if (!rpVariation.getPossibleValues().contains(variation))
                {
                    // Use the default variation
                    variation = rpVariation.getDefaultValue();
                }
                sgs.setRhythmParameterValue(sptEnd, rpVariation, variation);
            }
            
            um.endCEdit(getActionName());
        } catch (UnsupportedEditException ex)
        {
            // Should never happen
            Exceptions.printStackTrace(ex);
            um.abortCEdit(getActionName(), ex.getLocalizedMessage());
        }
    }
    
    @Override
    public void selectionChange(CL_Selection selection)
    {
        boolean b = selection.isBarSelectedWithinCls();
        setEnabled(b);
    }

    /**
     * Compute a unique section name.
     * <p>
     * @param cls
     * @return
     * @see #DEFAULT_END_SECTION_NAME
     */
    private String computeUniqueSectionName(ChordLeadSheet cls)
    {
        String name = DEFAULT_END_SECTION_NAME;
        while (cls.getSection(name) != null)
        {
            name = "_" + name;
        }
        return name;
    }
    
}

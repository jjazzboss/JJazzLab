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
import java.util.logging.Logger;
import static javax.swing.Action.NAME;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
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
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.utilities.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

/**
 * Insert a start bar/section whose corresponding SongPart is set to use the Intro A-1 variation.
 * <p>
 */
@ActionID(category = "JJazz", id = "org.jjazz.cl_editor.actions.insertstartbar")
@ActionRegistration(displayName = "not_used", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/BarInsert", position = 5)
        })
public class InsertStartBar extends CL_ContextAction
{

    private static final String DEFAULT_START_SECTION_NAME = "Start";
    private static final Logger LOGGER = Logger.getLogger(InsertStartBar.class.getSimpleName());


    @Override
    protected void configureAction()
    {
        putValue(NAME, ResUtil.getString(getClass(), "CTL_InsertStartBar"));
        putValue(LISTENING_TARGETS, EnumSet.of(CL_ContextAction.ListeningTarget.BAR_SELECTION));
    }

    @Override
    protected void actionPerformed(ActionEvent ae, ChordLeadSheet cls, CL_Selection selection)
    {
        CL_Editor editor = CL_EditorTopComponent.getActive().getEditor();
        Song song = editor.getSongModel();
        SongStructure sgs = song.getSongStructure();


        var um = JJazzUndoManagerFinder.getDefault().get(cls);
        um.startCEdit(getActionName());

        // Insert one bar at position 0
        cls.insertBars(0, 1);

        // Update start section name
        CLI_Section startSection = cls.getSection(0);
        cls.setSectionName(startSection, computeUniqueStartSectionName(cls));

        // Copy chord symbol from bar 1 to bar 0
        CLI_ChordSymbol firstChord = cls.getFirstItemAfter(new Position(1), true, CLI_ChordSymbol.class, i -> true);
        if (firstChord != null)
        {
            Position newPos = new Position(0, 0);
            CLI_ChordSymbol newChord = (CLI_ChordSymbol) firstChord.getCopy(null, newPos);
            cls.addItem(newChord);
        }

        // Set the section at bar 1 to start on a new line
        CLI_Section sectionAtBar1 = cls.getSection(1);
        CL_EditorClientProperties.setSectionIsOnNewLine(sectionAtBar1, true);

        // Try to set start SongPart to use variation "Intro A-1"
        SongPart startSpt = sgs.getSongPart(0);
        if (startSpt != null)
        {
            assert startSpt.getParentSection() == startSection : "startSpt=" + startSpt + " startSection=" + startSection;
            RP_SYS_Variation rpVariation = RP_SYS_Variation.getVariationRp(startSpt.getRhythm());
            if (rpVariation != null)
            {
                String variation = "Intro A-1";
                if (!rpVariation.getPossibleValues().contains(variation))
                {
                    // Use the default variation
                    variation = rpVariation.getDefaultValue();
                }
                sgs.setRhythmParameterValue(startSpt, rpVariation, variation);
            }
        }

        um.endCEdit(getActionName());
    }

    @Override
    public void selectionChange(CL_Selection selection)
    {
        boolean b = selection.isBarSelectedWithinCls();
        setEnabled(b);
    }

    /**
     * Compute a unique section name for the start section.
     * <p>
     * @param cls
     * @return
     * @see #DEFAULT_START_SECTION_NAME
     */
    private String computeUniqueStartSectionName(ChordLeadSheet cls)
    {
        var startSection = cls.getSection(0);
        String name = DEFAULT_START_SECTION_NAME;
        CLI_Section section;
        while ((section = cls.getSection(name)) != null && section != startSection)
        {
            name = "_" + name;
        }
        return name;
    }

}

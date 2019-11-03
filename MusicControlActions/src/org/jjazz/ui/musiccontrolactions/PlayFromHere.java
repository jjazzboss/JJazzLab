/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 *  This file is part of the JJazzLabX software.
 *   
 *  JJazzLabX is free software: you can redistribute it and/or modify
 *  it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *  as published by the Free Software Foundation, either version 3 of the License, 
 *  or (at your option) any later version.
 *
 *  JJazzLabX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 * 
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JJazzLabX.  If not, see <https://www.gnu.org/licenses/>
 * 
 *  Contributor(s): 
 */
package org.jjazz.ui.musiccontrolactions;

import java.awt.event.ActionEvent;
import java.beans.PropertyVetoException;
import java.util.Collection;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;
import org.jjazz.activesong.ActiveSongManager;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.musiccontrol.MusicController;
import org.jjazz.rhythmmusicgeneration.spi.MusicGenerationException;
import org.jjazz.song.api.Song;
import org.jjazz.ui.cl_editor.api.CL_EditorTopComponent;
import org.jjazz.ui.cl_editor.api.CL_SelectionUtilities;
import static org.jjazz.ui.musiccontrolactions.Bundle.*;
import org.jjazz.ui.ss_editor.api.SS_EditorTopComponent;
import org.jjazz.ui.ss_editor.api.SS_SelectionUtilities;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ui.cl_editor.api.CL_Editor;
import org.jjazz.ui.cl_editor.api.SelectedBar;
import org.jjazz.ui.ss_editor.api.SS_Editor;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;

/**
 * Play music from the 1st focused bar and/or 1st focused song part.
 *
 * Action is enabled as long as there is a Song object in the global lookup.
 */
@ActionID(category = "MusicControls", id = "org.jjazz.ui.musiccontrolactions.playfromhere")
@ActionRegistration(displayName = "#CTL_PlayFromHere", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/Bar", position = 830, separatorBefore = 829, separatorAfter = 831),
            @ActionReference(path = "Actions/SongPart", position = 830, separatorBefore = 829, separatorAfter = 831),
            @ActionReference(path = "Shortcuts", name = "C-SPACE")
        })
@NbBundle.Messages(
        {
            "CTL_PlayFromHere=Play from Here",
            "CTL_PlayFromHereToolTip=Play from selected bar or song part (ctrl+space)",
            "ERR_NotActive=Can't play from here: song is not active"
        })
public class PlayFromHere extends AbstractAction implements LookupListener
{

    private final Lookup.Result<Song> lookupResult;
    private Song song;
    private static final Logger LOGGER = Logger.getLogger(PlayFromHere.class.getSimpleName());

    public PlayFromHere()
    {
        putValue(Action.NAME, CTL_PlayFromHere());
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("control SPACE"));     // For popup display only     

        // Listen to the current Song changes
        lookupResult = Utilities.actionsGlobalContext().lookupResult(Song.class);
        lookupResult.addLookupListener(this);
        resultChanged(null);
    }

    @Override
    public void resultChanged(LookupEvent ev)
    {
        Collection<? extends Song> songs = lookupResult.allInstances();
        song = songs.isEmpty() ? null : songs.iterator().next();
        setEnabled(song != null);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        // Song must be active !
        ActiveSongManager asm = ActiveSongManager.getInstance();
        if (asm.getActiveSong() != song)
        {
            String msg = ERR_NotActive();
            NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return;
        }

        ChordLeadSheet cls = song.getChordLeadSheet();
        CL_EditorTopComponent clTc = CL_EditorTopComponent.get(cls);
        assert clTc != null;
        CL_Editor clEditor = clTc.getCL_Editor();

        SongStructure ss = song.getSongStructure();
        SS_EditorTopComponent ssTc = SS_EditorTopComponent.get(ss);
        assert ssTc != null;
        SS_Editor ssEditor = ssTc.getSS_Editor();

        int playFromBar = -1;

        // Where is the focus ?
        SelectedBar focusedBar;
        SongPart focusedSpt;
        if (((focusedBar = clEditor.getFocusedBar(true)) != null) && focusedBar.getModelBarIndex() != SelectedBar.POST_END_BAR_MODEL_BAR_INDEX)
        {
            // Focus in the CL_Editor on a valid bar
            // Try to find a bar in a matching SongPart
            playFromBar = getSsBarIndex(focusedBar.getModelBarIndex(), cls, ss);            // Can return -1
        } else if ((focusedSpt = ssEditor.getFocusedSongPart(true)) != null)
        {
            // Focus in the SS_Editor
            CL_SelectionUtilities clSelection = new CL_SelectionUtilities(clTc.getLookup());
            playFromBar = focusedSpt.getStartBarIndex() + getSelectedBarIndexRelativeToSection(focusedSpt.getParentSection(), clSelection);
        }

        if (playFromBar == -1)
        {
            String msg = "Can't play from here. Click first on a bar in the chord leadsheet editor, or on a song part in the song structure editor.";
            NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return;
        }

        MusicController mc = MusicController.getInstance();
        mc.stop();

        // OK we can go
        try
        {
            mc.start(song, playFromBar);
        } catch (MusicGenerationException | PropertyVetoException ex)
        {
            if (ex.getMessage() != null)
            {
                NotifyDescriptor d = new NotifyDescriptor.Message(ex.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(d);
            }
        }
    }

    //=====================================================================================
    // Private methods
    //=====================================================================================     
    /**
     * Find a SongStructure bar from a ChordLeadSheet bar.
     *
     * @param clsBarIndex
     * @param cls
     * @param ss
     * @return -1 if no corresponding bar.
     */
    private int getSsBarIndex(int clsBarIndex, ChordLeadSheet cls, SongStructure ss)
    {
        int sgsBarIndex = -1;
        CLI_Section section = cls.getSection(clsBarIndex);

        // If there some selected spts, try to match one of them
        SS_EditorTopComponent ssTc = SS_EditorTopComponent.get(ss);
        assert ssTc != null : "sgs=" + ss;
        SS_SelectionUtilities ssSelection = new SS_SelectionUtilities(ssTc.getLookup());
        if (ssSelection.isSongPartSelected())
        {
            for (SongPart spt : ssSelection.getSelectedSongParts())
            {
                if (spt.getParentSection() == section)
                {
                    sgsBarIndex = spt.getStartBarIndex();
                    sgsBarIndex += clsBarIndex - section.getPosition().getBar();
                    break;
                }
            }
        }

        if (sgsBarIndex == -1)
        {
            // It did not work, search the first SongPart which matches
            for (SongPart spt : ss.getSongParts())
            {
                if (spt.getParentSection() == section)
                {
                    sgsBarIndex = spt.getStartBarIndex();
                    sgsBarIndex += clsBarIndex - section.getPosition().getBar();
                    break;
                }
            }
        }
        return sgsBarIndex;
    }

    /**
     * Find the section-relative bar index from where to start when a song part is selected.
     *
     * @param cliSection The parent section of the selected song part.
     * @return 0 (=start of the section) if no selection in the section
     */
    private int getSelectedBarIndexRelativeToSection(CLI_Section cliSection, CL_SelectionUtilities clSelection)
    {
        int sectionStartBar = cliSection.getPosition().getBar();
        int sectionEndBar = sectionStartBar + cliSection.getContainer().getSectionSize(cliSection) - 1;

        int clsInSectionBarIndex = -1;
        if (clSelection.isBarSelected())
        {
            int minBar = clSelection.getMinBarIndexWithinCls();
            if (minBar >= sectionStartBar && minBar <= sectionEndBar)
            {
                clsInSectionBarIndex = minBar - sectionStartBar;
            }
        }

        if (clsInSectionBarIndex == -1)
        {
            // No matching selection, start from start of the section
            clsInSectionBarIndex = 0;
        }
        return clsInSectionBarIndex;
    }

}

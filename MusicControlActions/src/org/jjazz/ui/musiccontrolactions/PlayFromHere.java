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
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.util.logging.Logger;
import javax.sound.midi.MidiUnavailableException;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.jjazz.activesong.ActiveSongManager;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.midimix.MidiMix;
import org.jjazz.midimix.MidiMixManager;
import org.jjazz.musiccontrol.MusicController;
import org.jjazz.rhythmmusicgeneration.MusicGenerationContext;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.song.api.Song;
import org.jjazz.ui.cl_editor.api.CL_EditorTopComponent;
import org.jjazz.ui.cl_editor.api.CL_SelectionUtilities;
import org.jjazz.ui.ss_editor.api.SS_EditorTopComponent;
import org.jjazz.ui.ss_editor.api.SS_SelectionUtilities;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ui.cl_editor.api.CL_Editor;
import org.jjazz.ui.mixconsole.api.MixConsoleTopComponent;
import org.jjazz.ui.ss_editor.api.SS_Editor;
import static org.jjazz.ui.utilities.Utilities.getGenericControlKeyStroke;
import org.jjazz.util.ResUtil;
import org.openide.windows.TopComponent;

/**
 * Play music from the 1st selected bar and/or 1st selected song part.
 * <p>
 * Action is enabled when the active TopComponent is a CL_Editor or a SS_Editor.
 */
@ActionID(category = "MusicControls", id = "org.jjazz.ui.musiccontrolactions.playfromhere")
@ActionRegistration(displayName = "#CTL_PlayFromHere", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/Bar", position = 830, separatorBefore = 829),
            @ActionReference(path = "Actions/SongPart", position = 830, separatorBefore = 829),
            @ActionReference(path = "Shortcuts", name = "D-SPACE")
        })
public class PlayFromHere extends AbstractAction
{

    private Song song;
    private TopComponent lastValidActivatedTc;
    private static final Logger LOGGER = Logger.getLogger(PlayFromHere.class.getSimpleName());

    public PlayFromHere()
    {
        putValue(Action.NAME, ResUtil.getString(getClass(), "CTL_PlayFromHere"));
        putValue(ACCELERATOR_KEY, getGenericControlKeyStroke(KeyEvent.VK_SPACE));     // For popup display only     


        // Listen to TopComponent activation changes
        TopComponent.getRegistry().addPropertyChangeListener(new PropertyChangeListener()
        {
            @Override
            public void propertyChange(PropertyChangeEvent evt)
            {
                if (evt.getPropertyName().equals(TopComponent.Registry.PROP_ACTIVATED)
                        || evt.getPropertyName().equals(TopComponent.Registry.PROP_TC_CLOSED))
                {
                    updateEnabledStatus();
                }
            }
        }
        );
        updateEnabledStatus();

    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        if (song == null)
        {
            LOGGER.severe("actionPerformed() unexpected value song=" + song);   //NOI18N
            return;
        }

        // Song must be active !
        ActiveSongManager asm = ActiveSongManager.getInstance();
        if (asm.getActiveSong() != song)
        {
            String msg = ResUtil.getString(getClass(), "ERR_NotActive");
            NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return;
        }


        ChordLeadSheet cls = song.getChordLeadSheet();
        CL_EditorTopComponent clTc = CL_EditorTopComponent.get(cls);
        assert clTc != null;   //NOI18N
        CL_Editor clEditor = clTc.getCL_Editor();
        CL_SelectionUtilities clSelection = new CL_SelectionUtilities(clEditor.getLookup());


        SongStructure ss = song.getSongStructure();
        SS_EditorTopComponent ssTc = SS_EditorTopComponent.get(ss);
        assert ssTc != null;   //NOI18N
        SS_Editor ssEditor = ssTc.getSS_Editor();
        SS_SelectionUtilities ssSelection = new SS_SelectionUtilities(ssEditor.getLookup());


        int playFromBar = -1;


        if (lastValidActivatedTc == clTc && !clSelection.isEmpty())
        {
            // Focus in the CL_Editor            
            int clsBarIndex = clSelection.getMinBarIndexWithinCls();
            if (clsBarIndex != -1)
            {
                // Try to find a bar in a matching SongPart
                playFromBar = getSsBarIndex(clsBarIndex, cls, ss);            // Can return -1
            }

        } else if (lastValidActivatedTc == ssTc && !ssSelection.isEmpty())
        {
            // Focus in the SS_Editor
            SongPart firstSpt = ssSelection.getIndirectlySelectedSongParts().get(0);
            playFromBar = firstSpt.getStartBarIndex() + getSelectedBarIndexRelativeToSection(firstSpt.getParentSection(), clSelection);
        }


        if (playFromBar == -1)
        {
            String msg = ResUtil.getString(getClass(), "ERR_CantPlayFromHere");
            NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return;
        }


        // Make sure playback is stopped
        MusicController mc = MusicController.getInstance();
        mc.stop();


        // Configure and play
        try
        {
            MidiMix midiMix = MidiMixManager.getInstance().findMix(song);      // Can raise MidiUnavailableException
            MusicGenerationContext context = new MusicGenerationContext(song, midiMix);
            mc.setContext(context);
            mc.play(playFromBar);
        } catch (MusicGenerationException | PropertyVetoException | MidiUnavailableException ex)
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
    private void updateEnabledStatus()
    {
        MixConsoleTopComponent mcTc = MixConsoleTopComponent.getInstance(); // Can be null
        CL_EditorTopComponent clTc = CL_EditorTopComponent.getActive(); // Can be null
        SS_EditorTopComponent ssTc = SS_EditorTopComponent.getActive(); // Can be null

        song = null;

        if (clTc != null)
        {
            song = clTc.getSongModel();
            lastValidActivatedTc = clTc;
        } else if (ssTc != null)
        {
            song = ssTc.getSongModel();
            lastValidActivatedTc = ssTc;
        } else if (mcTc != null && TopComponent.getRegistry().getActivated() == mcTc)
        {
            song = (lastValidActivatedTc != null) ? mcTc.getEditor().getSong() : null;
        }

        boolean b = song != null;
        LOGGER.fine("updateEnabledStatus() b=" + b);   //NOI18N

        setEnabled(b);
    }

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
        LOGGER.fine("getSsBarIndex() section=" + section);   //NOI18N


        // If there are some selected spts, try to match one of them
        SS_EditorTopComponent ssTc = SS_EditorTopComponent.get(ss);
        assert ssTc != null : "sgs=" + ss;   //NOI18N
        SS_SelectionUtilities ssSelection = new SS_SelectionUtilities(ssTc.getLookup());
        for (SongPart spt : ssSelection.getIndirectlySelectedSongParts())
        {
            if (spt.getParentSection() == section)
            {
                sgsBarIndex = spt.getStartBarIndex();
                sgsBarIndex += clsBarIndex - section.getPosition().getBar();
                break;
            }
        }


        if (sgsBarIndex == -1)
        {
            // It did not work, search the first SongPart which matches
            LOGGER.fine("getSsBarIndex() no matching in selected spt, test all spts");   //NOI18N
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


        LOGGER.fine("getSsBarIndex() sgsBarIndex=" + sgsBarIndex);   //NOI18N
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
        int sectionEndBar = sectionStartBar + cliSection.getContainer().getSectionRange(cliSection).size() - 1;


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

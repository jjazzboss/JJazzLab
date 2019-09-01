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
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import org.jjazz.activesong.ActiveSongManager;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.musiccontrol.MusicController;
import org.jjazz.rhythmmusicgeneration.spi.MusicGenerationException;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongManager;
import org.jjazz.ui.cl_editor.api.CL_EditorTopComponent;
import org.jjazz.ui.cl_editor.api.CL_SelectionUtilities;
import org.jjazz.ui.cl_editor.api.SelectedBar;
import static org.jjazz.ui.musiccontrolactions.Bundle.*;
import org.jjazz.ui.ss_editor.api.RL_EditorTopComponent;
import org.jjazz.ui.ss_editor.api.RL_SelectionUtilities;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;

/**
 * Play music from the 1st selected bar or the 1st song part present in the global lookup.
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
            "CTL_PlayFromHere=Play from here",
            "CTL_PlayFromHereToolTip=Play from selected bar or song part (ctrl+space)",
            "ERR_NotActive=Song is not active"
        })
public class PlayFromHere extends AbstractAction implements ContextAwareAction
{

    private Lookup context;
    Lookup.Result<SelectedBar> lkpResultSelectedBar;
    LookupListener lkpListenerSelectedBar;
    Lookup.Result<SongPart> lkpResultSongPart;
    LookupListener lkpListenerSongPart;
    private static final Logger LOGGER = Logger.getLogger(PlayFromHere.class.getSimpleName());

    public PlayFromHere()
    {
        this(Utilities.actionsGlobalContext());
    }

    private PlayFromHere(Lookup context)
    {
        this.context = context;
        putValue(NAME, CTL_PlayFromHere());
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("control SPACE"));     // For popup display only     
    }

    /**
     * Initialize the lookup listeners.
     */
    private void init()
    {
        assert SwingUtilities.isEventDispatchThread() : "this shall be called just from AWT thread";

        if (lkpListenerSelectedBar != null)
        {
            return;
        }

        // For WeakReferences to work, we need to keep a strong reference on the listeners (see WeakListeners java doc).
        lkpListenerSelectedBar = new LookupListener()
        {
            @Override
            public void resultChanged(LookupEvent le)
            {
                resultChanged_SelectedBar();
            }
        };

        // The things we want to listen for the presence or absence of on the global selection
        lkpResultSelectedBar = context.lookupResult(SelectedBar.class);
        // Need to use WeakListeners so than action can be GC'ed
        // See http://forums.netbeans.org/viewtopic.php?t=35921
        lkpResultSelectedBar.addLookupListener(WeakListeners.create(LookupListener.class, lkpListenerSelectedBar, lkpResultSelectedBar));

        lkpListenerSongPart = new LookupListener()
        {
            @Override
            public void resultChanged(LookupEvent le)
            {
                resultChanged_SongPart();
            }
        };
        lkpResultSongPart = context.lookupResult(SongPart.class);
        lkpResultSelectedBar.addLookupListener(WeakListeners.create(LookupListener.class, lkpListenerSongPart, lkpResultSongPart));

        resultChanged_SelectedBar();
        resultChanged_SongPart();
    }

    @Override
    public boolean isEnabled()
    {
        init();
        return super.isEnabled();
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        init();

        SongManager sf = SongManager.getInstance();
        ChordLeadSheet cls;
        SongStructure sgs;
        Song song;
        int fromBar = -1;

        RL_SelectionUtilities sgsSelection = new RL_SelectionUtilities(context);
        CL_SelectionUtilities clSelection = new CL_SelectionUtilities(context);
        if (sgsSelection.isSongPartSelected())
        {
            // SongPart selected, easy
            sgs = sgsSelection.getModel();
            song = sf.findSong(sgs);
            assert song != null : "sgs=" + sgs;
            SongPart spt = sgs.getSongParts().get(sgsSelection.getMinStartSptIndex());
            fromBar = spt.getStartBarIndex() + getClsInSectionBarIndex(spt.getParentSection());
        } else
        {
            // ChordLeadSheet bar selected, if a matching song part is selected use this one, otherwise choose first matching one
            assert clSelection.isBarSelectedWithinCls() : "clSelection=" + clSelection + " sgsSelection=" + sgsSelection;
            cls = clSelection.getChordLeadSheet();
            song = sf.findSong(cls);
            assert song != null : "cls=" + cls;
            sgs = song.getSongStructure();
            fromBar = getSgsBarIndex(clSelection.getMinBarIndexWithinCls(), cls, sgs);
            assert fromBar != -1 : "clSelection=" + clSelection;
        }

        // Song must be active !
        ActiveSongManager asm = ActiveSongManager.getInstance();
        if (asm.getActiveSong() != song)
        {
            String msg = ERR_NotActive();
            NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return;
        }

        MusicController mc = MusicController.getInstance();
        mc.stop();

        // OK we can go
        try
        {
            mc.start(song, fromBar);
        } catch (MusicGenerationException | PropertyVetoException ex)
        {
            if (ex.getMessage() != null)
            {
                NotifyDescriptor d = new NotifyDescriptor.Message(ex.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(d);
            }
        }
    }

    private void resultChanged_SelectedBar()
    {
        // Update the enabled state 
        boolean b = false;
        CL_SelectionUtilities clSelection = new CL_SelectionUtilities(context);
        if (clSelection.isBarSelectedWithinCls())
        {
            // Check also there is a corresponding songpart to the selected bar
            SongManager sf = SongManager.getInstance();
            SongStructure sgs = sf.findSong(clSelection.getChordLeadSheet()).getSongStructure();
            b = getSgsBarIndex(clSelection.getMinBarIndexWithinCls(), clSelection.getChordLeadSheet(), sgs) != -1;
        }
        setEnabled(b);
    }

    private void resultChanged_SongPart()
    {
        // Update the enabled state 
        boolean b = new RL_SelectionUtilities(context).isSongPartSelected();
        setEnabled(b);
    }

    //=====================================================================================
    // ContextAwareAction interface
    //=====================================================================================
    @Override
    public Action createContextAwareInstance(Lookup context)
    {
        return new PlayFromHere(context);
    }

    //=====================================================================================
    // Private methods
    //=====================================================================================
    /**
     * Find the corresponding SongStructure bar for a ChordLeadSheet bar.
     *
     * @param clsBarIndex
     * @param cls
     * @param sgs
     * @return -1 if no corresponding bar.
     */
    private int getSgsBarIndex(int clsBarIndex, ChordLeadSheet cls, SongStructure sgs)
    {
        int sgsBarIndex = -1;
        CLI_Section section = cls.getSection(clsBarIndex);

        // If there is a selected spt, try to match it first
        RL_EditorTopComponent rlTc = RL_EditorTopComponent.get(sgs);
        assert rlTc != null : "sgs=" + sgs;
        RL_SelectionUtilities sgsSelection = new RL_SelectionUtilities(rlTc.getLookup());
        if (sgsSelection.isSongPartSelected())
        {
            for (SongPart spt : sgsSelection.getSelectedSongParts())
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
            // It did not work, search in the rest of SongPart
            for (SongPart spt : sgs.getSongParts())
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
    private int getClsInSectionBarIndex(CLI_Section cliSection)
    {
        int clsInSectionBarIndex = -1;
        ChordLeadSheet cls = cliSection.getContainer();
        int sectionStartBar = cliSection.getPosition().getBar();
        int sectionEndBar = sectionStartBar + cls.getSectionSize(cliSection) - 1;

        // If there is a selected bar in the parent section, use it
        CL_EditorTopComponent clTc = CL_EditorTopComponent.get(cls);
        assert clTc != null : "cliSection=" + cliSection + " cls=" + cls;
        CL_SelectionUtilities clSelection = new CL_SelectionUtilities(clTc.getLookup());
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

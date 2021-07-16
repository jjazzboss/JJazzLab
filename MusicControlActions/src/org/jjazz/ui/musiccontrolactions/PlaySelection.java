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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.util.List;
import java.util.logging.Logger;
import javax.sound.midi.MidiUnavailableException;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;
import org.jjazz.activesong.api.ActiveSongManager;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.api.MidiMixManager;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.musiccontrol.api.playbacksession.PlaybackSession;
import org.jjazz.musiccontrol.api.playbacksession.SongContextSession;
import org.jjazz.rhythmmusicgeneration.api.SongContext;
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
import org.jjazz.util.api.IntRange;
import org.jjazz.util.api.ResUtil;
import org.openide.windows.TopComponent;

/**
 * Play music corresponding to the contiguous selection of bars or songParts.
 * <p>
 * Action is enabled when the active TopComponent is a CL_Editor or a SS_Editor.
 */
@ActionID(category = "MusicControls", id = "org.jjazz.ui.musiccontrolactions.playselection")
@ActionRegistration(displayName = "#CTL_PlaySelection", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/Bar", position = 831, separatorAfter = 832),
            @ActionReference(path = "Actions/SongPart", position = 831, separatorAfter = 832),
            @ActionReference(path = "Shortcuts", name = "DS-SPACE")
        })
public class PlaySelection extends AbstractAction
{

    private Song song;
    private TopComponent lastValidActivatedTc;
    private static final Logger LOGGER = Logger.getLogger(PlaySelection.class.getSimpleName());

    public PlaySelection()
    {
        putValue(Action.NAME, ResUtil.getString(getClass(), "CTL_PlaySelection"));
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("control shift SPACE"));     // For popup display only     

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


        IntRange rg = null;
        String errMsg = ResUtil.getString(getClass(), "ERR_NeedContiguousSelection");     // By default


        if (lastValidActivatedTc == clTc && clSelection.isContiguousBarboxSelectionWithinCls())
        {
            // Focus in the CL_Editor            
            rg = toSgsRange(ss, cls, new IntRange(clSelection.getMinBarIndexWithinCls(), clSelection.getMaxBarIndexWithinCls()), ssSelection);   // Can be null
            if (rg == null)
            {
                errMsg = ResUtil.getString(getClass(), "ERR_BadSelection");
            }

        } else if (lastValidActivatedTc == ssTc && ssSelection.isContiguousSptSelection())
        {
            // Focus in the SS_Editor
            List<SongPart> spts = ssSelection.getIndirectlySelectedSongParts();
            SongPart firstSpt = spts.get(0);
            SongPart lastSpt = spts.get(spts.size() - 1);
            rg = new IntRange(firstSpt.getStartBarIndex(), lastSpt.getBarRange().to);

        }


        if (rg == null)
        {
            String msg = ResUtil.getString(getClass(), "ERR_InvalidPlayableSelection", errMsg);
            NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return;
        }


        MusicController mc = MusicController.getInstance();
        mc.stop();


        // OK we can go
        try
        {
            MidiMix midiMix = MidiMixManager.getInstance().findMix(song);      // Can raise MidiUnavailableException
            SongContext context = new SongContext(song, midiMix, rg);
            SongContextSession session = SongContextSession.getSession(context);
            if (session.getState().equals(PlaybackSession.State.NEW))
            {
                session.generate();
            }
            mc.play(session, rg.from);
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
     * Convert a ChordLeadSheet range into a range within a SongStructure.
     * <p>
     * <p>
     * Example:<br>
     * - bar0=C7 (section=S1), bar1=Em (section=S2), bar2=D (section=S3)<br>
     * - SongStructure=S1 S1 S3 S2<br>
     * If cls range=bar0+bar1, then sgs range=[0;3]<br>
     *
     * @param sgs The parent sections of the song parts must be in cls.
     * @param cls
     * @param clsRange
     * @return Null if no valid range could be constructed
     */
    private IntRange toSgsRange(SongStructure ss, ChordLeadSheet cls, IntRange clsRange, SS_SelectionUtilities ssSelection)
    {
        if (ss == null || cls == null || clsRange.to > cls.getSize() - 1)
        {
            throw new IllegalArgumentException("cls=" + cls + ", ss=" + ss + ", clsRange=" + clsRange);   //NOI18N
        }
        CLI_Section fromSection = cls.getSection(clsRange.from);
        int fromBar = -1;
        CLI_Section toSection = cls.getSection(clsRange.to);
        int toBar = -1;
        IntRange r = null;
        List<SongPart> spts = ssSelection.isEmpty() || !ssSelection.isContiguousSptSelection() ? ss.getSongParts() : ssSelection.getIndirectlySelectedSongParts();
        for (SongPart spt : spts)
        {
            if (fromBar == -1 && spt.getParentSection() == fromSection)
            {
                fromBar = spt.getStartBarIndex() + clsRange.from - fromSection.getPosition().getBar();
            }
            if (toBar == -1 && spt.getParentSection() == toSection)
            {
                toBar = spt.getStartBarIndex() + clsRange.to - toSection.getPosition().getBar();
            }
            if (toBar != -1 && fromBar != -1 && fromBar > toBar)
            {
                break;
            }
            if (toBar != -1 && fromBar != -1)
            {
                r = new IntRange(fromBar, toBar);
                break;
            }
        }
        return r;
    }

}

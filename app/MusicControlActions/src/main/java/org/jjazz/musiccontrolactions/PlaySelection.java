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
package org.jjazz.musiccontrolactions;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MidiUnavailableException;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;
import org.jjazz.activesong.spi.ActiveSongManager;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.spi.MidiMixManager;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.musiccontrol.api.playbacksession.UpdateProviderSongSession;
import org.jjazz.musiccontrol.api.playbacksession.UpdatableSongSession;
import org.jjazz.songcontext.api.SongContext;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.song.api.Song;
import org.jjazz.cl_editor.api.CL_EditorTopComponent;
import org.jjazz.cl_editor.api.CL_Selection;
import org.jjazz.ss_editor.api.SS_EditorTopComponent;
import org.jjazz.ss_editor.api.SS_Selection;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.cl_editor.api.CL_Editor;
import org.jjazz.mixconsole.api.MixConsoleTopComponent;
import org.jjazz.musiccontrol.api.playbacksession.PlaybackSession;
import org.jjazz.musiccontrolactions.api.FixMissingSectionStartChord;
import org.jjazz.outputsynth.api.FixMidiMix;
import org.jjazz.ss_editor.api.SS_Editor;
import org.jjazz.utilities.api.IntRange;
import org.jjazz.utilities.api.ResUtil;
import org.openide.windows.TopComponent;

/**
 * Play music corresponding to the contiguous selection of bars or songParts.
 * <p>
 * Action is enabled when the active TopComponent is a CL_Editor or a SS_Editor.
 */
@ActionID(category = "MusicControls", id = "org.jjazz.musiccontrolactions.playselection")
@ActionRegistration(displayName = "#CTL_PlaySelection", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/Bar", position = 831),
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
        TopComponent.getRegistry().addPropertyChangeListener(evt -> 
        {
            if (evt.getPropertyName().equals(TopComponent.Registry.PROP_ACTIVATED)
                    || evt.getPropertyName().equals(TopComponent.Registry.PROP_TC_CLOSED))
            {
                updateEnabledState();
            }
        });
        updateEnabledState();
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        if (song == null)
        {
            LOGGER.log(Level.SEVERE, "actionPerformed() unexpected value song={0}", song);
            return;
        }


        // Song must be active !
        ActiveSongManager asm = ActiveSongManager.getDefault();
        if (asm.getActiveSong() != song)
        {
            String msg = ResUtil.getString(getClass(), "ERR_NotActive");
            NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return;
        }


        ChordLeadSheet cls = song.getChordLeadSheet();
        CL_EditorTopComponent clTc = CL_EditorTopComponent.get(cls);
        assert clTc != null;
        CL_Editor clEditor = clTc.getEditor();
        CL_Selection clSelection = new CL_Selection(clEditor.getLookup());


        SongStructure ss = song.getSongStructure();
        SS_EditorTopComponent ssTc = SS_EditorTopComponent.get(ss);
        assert ssTc != null;
        SS_Editor ssEditor = ssTc.getEditor();
        SS_Selection ssSelection = new SS_Selection(ssEditor.getLookup());


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
        UpdatableSongSession session = null;
        try
        {
            MidiMix midiMix = MidiMixManager.getDefault().findMix(song);      // Can raise MidiUnavailableException
            SongContext context = new SongContext(song, midiMix, rg);
            
            new FixMissingSectionStartChord(context).autofix();
            FixMidiMix.checkAndPossiblyFix(midiMix, true);

            UpdateProviderSongSession dynSession = UpdateProviderSongSession.getSession(context, PlaybackSession.Context.SONG);            
            session = UpdatableSongSession.getSession(dynSession);
            mc.setPlaybackSession(session, false);      // Can raise MusicGenerationException
            mc.play(rg.from);

        } catch (MusicGenerationException | MidiUnavailableException ex)
        {
            if (session != null)
            {
                session.close();
            }
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
    private void updateEnabledState()
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
        LOGGER.log(Level.FINE, "updateEnabledStatus() b={0}", b);

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
     * @param ss      The parent sections of the song parts must be in cls.
     * @param cls
     * @param clsRange
     * @param ssSelection
     * @return Null if no valid range could be constructed
     */
    private IntRange toSgsRange(SongStructure ss, ChordLeadSheet cls, IntRange clsRange, SS_Selection ssSelection)
    {
        if (ss == null || cls == null || clsRange.to > cls.getSizeInBars() - 1)
        {
            throw new IllegalArgumentException("cls=" + cls + ", ss=" + ss + ", clsRange=" + clsRange);
        }
        CLI_Section fromSection = cls.getSection(clsRange.from);
        int fromBar = -1;
        CLI_Section toSection = cls.getSection(clsRange.to);
        int toBar = -1;
        IntRange r = null;
        List<SongPart> spts = ssSelection.isEmpty() || !ssSelection.isContiguousSptSelection() ? ss.getSongParts()
                : ssSelection.getIndirectlySelectedSongParts();
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

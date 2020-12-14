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
package org.jjazz.ui.ss_editor.actions;

import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.swing.SwingUtilities;
import org.jjazz.activesong.ActiveSongManager;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.midi.Instrument;
import org.jjazz.midi.InstrumentMix;
import org.jjazz.midi.MidiConst;
import org.jjazz.midi.MidiUtilities;
import org.jjazz.midi.synths.StdSynth;
import org.jjazz.midimix.MidiMix;
import org.jjazz.midimix.MidiMixManager;
import org.jjazz.musiccontrol.MusicController;
import org.jjazz.outputsynth.OutputSynth;
import org.jjazz.outputsynth.OutputSynthManager;
import org.jjazz.rhythm.api.AdaptedRhythm;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythmmusicgeneration.MidiSequenceBuilder;
import org.jjazz.rhythmmusicgeneration.MusicGenerationContext;
import org.jjazz.rhythmmusicgeneration.spi.MusicGenerator.PostProcessor;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongFactory;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.ui.ss_editor.spi.RhythmSelectionDialog;
import org.jjazz.util.ResUtil;
import org.openide.util.Exceptions;

/**
 * A RhythmPreviewProvider instance which plays one song part.
 */
public class EditRhythmPreviewer implements RhythmSelectionDialog.RhythmPreviewProvider, MetaEventListener
{

    private boolean isPreviewRunning;
    private PostProcessor[] originalPostProcessors;
    private Song originalSong;
    private Song previouslyActivatedSong;
    private SongPart originalSpt;
    private Rhythm rhythm;
    private Sequencer sequencer;
    private ActionListener endAction;
    private Set<Rhythm> previewedRhythms = new HashSet<>();  // To release rhythm resources upon cleanup
    private static final Logger LOGGER = Logger.getLogger(EditRhythmPreviewer.class.getSimpleName());

    /**
     *
     * @param sg The song for which we preview rhythm
     * @param spt The spt for which rhythm is changed
     * @throws MidiUnavailableException
     */
    public EditRhythmPreviewer(Song sg, SongPart spt) throws MidiUnavailableException
    {
        if (sg == null || spt == null)
        {
            throw new IllegalArgumentException("sg=" + sg + " spt=" + spt);   //NOI18N
        }
        originalSong = sg;
        originalSpt = spt;
        originalPostProcessors = MusicController.getInstance().getPostProcessors();
        previouslyActivatedSong = ActiveSongManager.getInstance().getActiveSong();
    }

    @Override
    public void cleanup()
    {
        stop();
        if (sequencer != null)
        {
            sequencer.removeMetaEventListener(this);
            var mc = MusicController.getInstance();
            mc.releaseSequencer(this);
        }

        // Release resources of all previewed rhythms
        for (Rhythm r : previewedRhythms)
        {
            r.releaseResources();
        }

        // Reactivate song
        var asm = ActiveSongManager.getInstance();
        MidiMix mm = null;
        try
        {
            mm = previouslyActivatedSong == null ? null : MidiMixManager.getInstance().findMix(previouslyActivatedSong);
        } catch (MidiUnavailableException ex)
        {
            LOGGER.severe("cleanup() ex=" + ex.getLocalizedMessage());   //NOI18N
            Exceptions.printStackTrace(ex);
            previouslyActivatedSong = null;
        }
        asm.setActive(previouslyActivatedSong, mm);
    }

    @Override
    public void previewRhythm(Rhythm r, boolean useRhythmTempo, boolean loop, ActionListener endListener) throws MusicGenerationException
    {
        if (r == null)
        {
            throw new IllegalArgumentException("r=" + r + " useRhythmTempo=" + useRhythmTempo + " loop=" + loop);   //NOI18N
        }

        LOGGER.fine("previewRhythm() -- r=" + r + " useRhythmTempo=" + useRhythmTempo + " loop=" + loop + " endListener=" + endListener);   //NOI18N

        if (isPreviewRunning)
        {
            if (rhythm == r)
            {
                return;
            }
            stopSequencer();
        }

        isPreviewRunning = false;
        rhythm = r;
        endAction = endListener;

        // If sequencer not already acquired, stop any previous playing and acquire sequencer
        if (sequencer == null)
        {
            var mc = MusicController.getInstance();
            mc.stop();
            sequencer = mc.acquireSequencer(this);
            if (sequencer == null)
            {
                throw new MusicGenerationException(ResUtil.getString(getClass(), "ERR_CantAcquireSequencer"));
            }
            sequencer.addMetaEventListener(this);
        }

        // Build the preview song and context
        Song song;
        MusicGenerationContext context;
        MidiMix mm;
        try
        {
            song = buildPreviewSong(originalSong, originalSpt, r);
            mm = MidiMixManager.getInstance().findMix(song);        // Possible exception here
            // LOGGER.severe("previewRhythm() mm BEFORE=" + mm.toDumpString());
            fixMidiMix(mm);
            // LOGGER.severe("previewRhythm() mm AFTER=" + mm.toDumpString());
            context = new MusicGenerationContext(song, mm);
        } catch (UnsupportedEditException | MidiUnavailableException ex)
        {
            LOGGER.warning("previewRhythm() ex=" + ex.getLocalizedMessage());   //NOI18N
            throw new MusicGenerationException(ex.getLocalizedMessage());
        }
        SongPart spt0 = song.getSongStructure().getSongPart(0);
        song.setTempo(useRhythmTempo ? r.getPreferredTempo() : originalSong.getTempo());

        // Activate the song to initialize Midi instruments
        ActiveSongManager asm = ActiveSongManager.getInstance();
        asm.setActive(song, mm);

        // Build the sequence from context
        MidiSequenceBuilder seqBuilder = new MidiSequenceBuilder(context, originalPostProcessors);
        Sequence sequence = seqBuilder.buildSequence(false);                  // Can raise MusicGenerationException
        if (sequence == null)
        {
            // Can happen if unexpected error, assertion error etc.
            song.close(false);
            throw new MusicGenerationException(ResUtil.getString(getClass(), "ERR_BuildingSequence"));
        }

        // Reroute drums channels if needed
        List<Integer> toBeRerouted = mm.getDrumsReroutedChannels();
        MidiUtilities.rerouteShortMessages(sequence, toBeRerouted, MidiConst.CHANNEL_DRUMS);

        // Prepare sequencer
        try
        {

            sequencer.setSequence(sequence);
        } catch (InvalidMidiDataException ex)
        {
            LOGGER.warning("previewRhythm() ex=" + ex.getLocalizedMessage());   //NOI18N
            throw new MusicGenerationException(ex.getLocalizedMessage());
        }
        sequencer.setTempoInBPM(MidiConst.SEQUENCER_REF_TEMPO);
        sequencer.setTickPosition(0);
        sequencer.setLoopCount(loop ? Sequencer.LOOP_CONTINUOUSLY : 0);
        sequencer.setLoopStartPoint(0);
        long songTickEnd = (long) (context.getSptBeatRange(spt0).size() * MidiConst.PPQ_RESOLUTION);
        sequencer.setLoopEndPoint(songTickEnd);

        // Start
        sequencer.start();
        sequencer.setTempoInBPM(MidiConst.SEQUENCER_REF_TEMPO);  // JDK -11 BUG: start() resets tempo at 120 !
        float songTempoFactor = (float) song.getTempo() / MidiConst.SEQUENCER_REF_TEMPO;
        sequencer.setTempoFactor(songTempoFactor);

        // Update state
        isPreviewRunning = true;
        previewedRhythms.add(rhythm);

        // Close the song but don't release the rhythm resources: it will be done by cleanup()
        song.close(false);
    }

    @Override
    public Rhythm getPreviewedRhythm()
    {
        return isPreviewRunning ? rhythm : null;
    }

    @Override
    public void stop()
    {
        if (!isPreviewRunning)
        {
            return;
        }
        stopSequencer();
    }

    // ===============================================================================================
    // MetaEventListener implementation
    // ===============================================================================================
    @Override
    public void meta(MetaMessage meta)
    {
        if (meta.getType() == 47) // Meta Event for end of sequence
        {
            // This method  is called from the Sequencer thread, NOT from the EDT !
            // So if this method impacts the UI, it must use SwingUtilities.InvokeLater() (or InvokeAndWait())
            LOGGER.fine("Sequence end reached");   //NOI18N
            Runnable doRun = new Runnable()
            {
                @Override
                public void run()
                {
                    stopSequencer();
                }
            };
            SwingUtilities.invokeLater(doRun);
        }
    }

    // ===============================================================================================
    // Private methods
    // ===============================================================================================
    private void stopSequencer()
    {
        assert sequencer != null;   //NOI18N
        sequencer.stop();
        isPreviewRunning = false;
        if (endAction != null)
        {
            endAction.actionPerformed(null);
        }
    }

    /**
     * Fix MidiMix : reroute drums channels if needed and change instruments to fit current output synth.
     *
     * @param mm
     */
    private void fixMidiMix(MidiMix mm)
    {

        // Fix instruments Vs output synth
        OutputSynth outputSynth = OutputSynthManager.getInstance().getOutputSynth();
        HashMap<Integer, Instrument> mapNewInstruments = outputSynth.getNeedFixInstruments(mm);

        LOGGER.fine("fixMidiMix()    mapNewInstruments=" + mapNewInstruments);   //NOI18N

        for (int channel : mapNewInstruments.keySet())
        {
            Instrument newIns = mapNewInstruments.get(channel);
            InstrumentMix insMix = mm.getInstrumentMixFromChannel(channel);
            insMix.setInstrument(newIns);
            if (newIns != StdSynth.getInstance().getVoidInstrument())
            {
                // If we set a (non void) instrument it should not be rerouted anymore if it was the case before
                mm.setDrumsReroutedChannel(false, channel);
            }
        }

        // Reroute drums channels
        List<Integer> reroutableChannels = mm.getChannelsNeedingDrumsRerouting(mapNewInstruments);
        LOGGER.fine("fixMidiMix()    reroutableChannels=" + reroutableChannels);   //NOI18N
        for (int ch : reroutableChannels)
        {
            mm.setDrumsReroutedChannel(true, ch);
        }

    }

    /**
     * Build the song used for preview of the specified rhythm.
     * <p>
     * Song will be only one SongPart, unless r is an AdaptedRhythm and another similar SongPart is added with the source rhythm.
     * Only the first SongPart should be used.
     *
     * @param song
     * @param spt
     * @param r
     * @return
     */
    private Song buildPreviewSong(Song song, SongPart spt, Rhythm r) throws UnsupportedEditException
    {
        // Get a copy
        Song newSong = SongFactory.getInstance().getCopy(song);
        SongStructure newSs = newSong.getSongStructure();
        ChordLeadSheet newCls = newSong.getChordLeadSheet();

        // Remove everything
        newSs.removeSongParts(newSs.getSongParts());

        // Get the first SongPart with the new rhythm
        List<SongPart> newSpts = new ArrayList<>();
        var parentSection = newCls.getSection(spt.getParentSection().getData().getName());
        var newSpt = spt.clone(r, 0, spt.getNbBars(), parentSection);
        newSpts.add(newSpt);

        // If r is an AdaptedRhythm we must also add its the source rhythm
        if (r instanceof AdaptedRhythm)
        {
            AdaptedRhythm ar = (AdaptedRhythm) r;
            Rhythm sourceRhythm = ar.getSourceRhythm();
            parentSection = newCls.getItems(CLI_Section.class) // Find a parent section with the right signature
                    .stream()
                    .filter(s -> s.getData().getTimeSignature().equals(sourceRhythm.getTimeSignature()))
                    .findFirst().orElseThrow();     // Exception should never be thrown
            newSpt = spt.clone(ar.getSourceRhythm(), spt.getNbBars(), spt.getNbBars(), parentSection);
            newSpts.add(newSpt);
        }

        // Add the SongParts
        newSs.addSongParts(newSpts);

        return newSong;
    }

}

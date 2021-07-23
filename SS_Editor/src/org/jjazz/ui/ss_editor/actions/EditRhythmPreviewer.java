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
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequencer;
import org.jjazz.activesong.api.ActiveSongManager;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.midi.api.Instrument;
import org.jjazz.midi.api.InstrumentMix;
import org.jjazz.midi.api.MidiUtilities;
import org.jjazz.midi.api.synths.StdSynth;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.api.MidiMixManager;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.musiccontrol.api.playbacksession.BasicSongContextSession;
import org.jjazz.musiccontrol.api.playbacksession.PlaybackSession;
import org.jjazz.outputsynth.api.OutputSynth;
import org.jjazz.outputsynth.api.OutputSynthManager;
import org.jjazz.rhythm.api.AdaptedRhythm;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.rhythmmusicgeneration.api.SongContext;
import org.jjazz.rhythmmusicgeneration.spi.MusicGenerator;
import org.jjazz.rhythmmusicgeneration.spi.MusicGenerator.PostProcessor;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongFactory;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.ui.ss_editor.spi.RhythmSelectionDialog;
import org.jjazz.util.api.LongRange;
import org.openide.util.Exceptions;

/**
 * A RhythmPreviewProvider instance which plays one song part using the MusicController/PlaybackSession mechanism.
 */
public class EditRhythmPreviewer implements RhythmSelectionDialog.RhythmPreviewProvider
{

    private PostProcessor[] originalPostProcessors;
    private Song originalSong;
    private Song previouslyActivatedSong;
    private SongPart originalSpt;
    private Rhythm rhythm;
    private Map<RhythmParameter<?>, Object> rpValues;
    private ActionListener endAction;
    private BasicSongContextSession session;
    private final Set<Rhythm> previewedRhythms = new HashSet<>();  // To release rhythm resources upon cleanup
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
        originalPostProcessors = new PostProcessor[0]; // MusicController.getInstance().getPostProcessors();
        previouslyActivatedSong = ActiveSongManager.getInstance().getActiveSong();
    }

    @Override
    public void cleanup()
    {

        MusicController.getInstance().stop();


        // Release resources of all previewed rhythms
        for (Rhythm r : previewedRhythms)
        {
            r.releaseResources();
        }

        if (session != null)
        {
            session.cleanup();
        }

        // Reactivate song
        var asm = ActiveSongManager.getInstance();
        MidiMix mm = null;
        try
        {
            mm = previouslyActivatedSong == null ? null : MidiMixManager.getInstance().findMix(previouslyActivatedSong);
        } catch (MidiUnavailableException ex)
        {
            LOGGER.severe("cleanup() ex=" + ex.getMessage());   //NOI18N
            Exceptions.printStackTrace(ex);
            previouslyActivatedSong = null;
        }
        asm.setActive(previouslyActivatedSong, mm);
    }

    @Override
    public void previewRhythm(Rhythm r, Map<RhythmParameter<?>, Object> rpValues, boolean useRhythmTempo, boolean loopCount, ActionListener endListener) throws MusicGenerationException
    {
        if (r == null)
        {
            throw new IllegalArgumentException("r=" + r + " rpValues=" + rpValues + " useRhythmTempo=" + useRhythmTempo + " loopCount=" + loopCount);   //NOI18N
        }

        LOGGER.fine("previewRhythm() -- r=" + r + " rpValues=" + rpValues + " useRhythmTempo=" + useRhythmTempo + " loop=" + loopCount + " endListener=" + endListener);   //NOI18N

        MusicController mc = MusicController.getInstance();
        if (mc.getState().equals(MusicController.State.PLAYING))
        {
            if (getPreviewedRhythm() == r && Objects.equals(this.rpValues, rpValues))
            {
                return;
            } else
            {
                mc.stop();
            }
        }

        rhythm = r;
        endAction = endListener;


        // Build the preview song and context
        SongContext sgContext = buildSongContext(r, rpValues, useRhythmTempo);
        Song song = sgContext.getSong();
        MidiMix mm = sgContext.getMidiMix();
        SongPart spt0 = song.getSongStructure().getSongPart(0); // There might be 2 song parts for AdaptedRhythm
        LongRange spt0TickRange = sgContext.getSptTickRange(spt0);


        // Build the corresponding session
        if (session != null)
        {
            session.cleanup();
        }
        session = new PreviewSession(sgContext,
                loopCount ? Sequencer.LOOP_CONTINUOUSLY : 0,
                endAction,
                originalPostProcessors);
        if (session.getState().equals(PlaybackSession.State.NEW))
        {
            session.generate();
        }

        // Activate the song to initialize Midi instruments
        ActiveSongManager asm = ActiveSongManager.getInstance();
        asm.setActive(song, mm);


        // Start playback
        mc.play(session, 0);


        // Save previewed rhythm
        previewedRhythms.add(rhythm);


    }

    @Override
    public void stop()
    {
        MusicController.getInstance().stop();
    }

    @Override
    public Rhythm getPreviewedRhythm()
    {
        var mc = MusicController.getInstance();
        return mc.getState().equals(MusicController.State.PLAYING) && mc.getPlaybackSession() == session ? rhythm : null;
    }

    // ===============================================================================================
    // Private methods
    // ===============================================================================================
    private SongContext buildSongContext(Rhythm r, Map<RhythmParameter<?>, Object> rpValues, boolean useRhythmTempo) throws MusicGenerationException
    {
        SongContext sgContext;
        try
        {
            Song song = buildPreviewSong(originalSong, originalSpt, r, rpValues);
            song.setTempo(useRhythmTempo ? r.getPreferredTempo() : originalSong.getTempo());
            MidiMix mm = MidiMixManager.getInstance().findMix(song);        // Possible exception here
            fixMidiMix(mm);
            sgContext = new SongContext(song, mm);
        } catch (UnsupportedEditException | MidiUnavailableException ex)
        {
            LOGGER.warning("buildSongContext() r=" + r + " ex=" + ex.getMessage());   //NOI18N
            throw new MusicGenerationException(ex.getLocalizedMessage());
        }

        return sgContext;
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
    private Song buildPreviewSong(Song song, SongPart spt, Rhythm r, Map<RhythmParameter<?>, Object> rpValues) throws UnsupportedEditException
    {
        // Get a copy
        var sf = SongFactory.getInstance();
        Song newSong = sf.getCopy(song);
        sf.unregisterSong(newSong);

        SongStructure newSs = newSong.getSongStructure();
        ChordLeadSheet newCls = newSong.getChordLeadSheet();

        // Remove everything
        newSs.removeSongParts(newSs.getSongParts());

        // Get the first SongPart with the new rhythm
        List<SongPart> newSpts = new ArrayList<>();
        var parentSection = newCls.getSection(spt.getParentSection().getData().getName());
        var newSpt0 = spt.clone(r, 0, spt.getNbBars(), parentSection);
        newSpts.add(newSpt0);

        // If r is an AdaptedRhythm we must also add its source rhythm
        if (r instanceof AdaptedRhythm)
        {
            AdaptedRhythm ar = (AdaptedRhythm) r;
            Rhythm sourceRhythm = ar.getSourceRhythm();
            parentSection = newCls.getItems(CLI_Section.class) // Find a parent section with the right signature
                    .stream()
                    .filter(s -> s.getData().getTimeSignature().equals(sourceRhythm.getTimeSignature()))
                    .findFirst().orElseThrow();     // Exception should never be thrown
            var newSpt1 = spt.clone(ar.getSourceRhythm(), spt.getNbBars(), spt.getNbBars(), parentSection);
            newSpts.add(newSpt1);
        }

        // Add the SongParts to the song
        newSs.addSongParts(newSpts);


        // Update spt0 RhythmParameter values
        for (RhythmParameter rp : r.getRhythmParameters())
        {
            Object rpValue = rpValues.get(rp);
            if (rpValue != null)
            {
                newSs.setRhythmParameterValue(newSpt0, rp, rpValue);
            }
        }

        return newSong;
    }

    /**
     * Our own session to manage the special case of a SongPart with an AdaptedRhythm which needs the source rhythm to be present
     * in the song for building the sequence.
     * <p>
     * In this case we shorten the generated sequence and update loopEndTick.
     */
    private class PreviewSession extends BasicSongContextSession
    {

        protected PreviewSession(SongContext sgContext, int loopCount, ActionListener endOfPlaybackAction, MusicGenerator.PostProcessor... postProcessors)
        {
            super(sgContext, 0, -1, loopCount, endOfPlaybackAction, postProcessors);
        }

        @Override
        public void generate() throws MusicGenerationException
        {
            super.generate();


            // Check if we preview an AdaptedRhythm
            var spts = sgContext.getSong().getSongStructure().getSongParts();
            SongPart spt0 = spts.get(0);
            if (!(spt0.getRhythm() instanceof AdaptedRhythm))
            {
                // Nothing to do
                return;
            }

            // AdaptedRhythm: there must be a 2nd songpart for the source rhythm,  cut it
            assert spts.size() == 2 : "spts=" + spts;
            loopEndTick = sgContext.getSptTickRange(spt0).to;
            MidiUtilities.setSequenceDuration(sequence, loopEndTick);

        }

    }

}

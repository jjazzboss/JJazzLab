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
package org.jjazz.ui.musiccontrolactions.api;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import org.jjazz.analytics.api.Analytics;
import org.jjazz.midi.api.InstrumentMix;
import org.jjazz.midi.api.MidiUtilities;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.api.MidiMixManager;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.musiccontrol.api.PlaybackSettings;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythmmusicgeneration.api.SongSequenceBuilder;
import org.jjazz.song.api.Song;
import org.jjazz.songcontext.api.SongContext;
import org.jjazz.util.api.ResUtil;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.StatusDisplayer;

/**
 * Helper methods to export a Song to a Midi file.
 */
public class SongExportSupport
{

    private static final Logger LOGGER = Logger.getLogger(SongExportSupport.class.getSimpleName());

    /**
     * Generate the sequence for the specified song and write it to the specified midi File.
     * <p>
     * Notify user if problem occured.
     *
     * @param sg
     * @param midiFile
     * @return True if write was successful.
     */
    static public boolean songToMidiFile(Song sg, File midiFile)
    {

        // Playback must be stopped, otherwise seems to have side effects on the generated Midi file (missing tracks...?)
        MusicController.getInstance().stop();


        // Log event
        Analytics.logEvent("Export Midi");


        MidiMix midiMix = null;
        try
        {
            midiMix = MidiMixManager.getInstance().findMix(sg);
        } catch (MidiUnavailableException ex)
        {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);   
            NotifyDescriptor nd = new NotifyDescriptor.Message(ex.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(nd);
            return false;
        }


        // Check there is at least one unmuted track
        boolean allMuted = true;
        for (InstrumentMix insMix : midiMix.getInstrumentMixes())
        {
            if (!insMix.isMute())
            {
                allMuted = false;
                break;
            }
        }
        if (allMuted)
        {
            String msg = ResUtil.getString(SongExportSupport.class, "ERR_AllChannelsMuted");
            LOGGER.warning(msg);   
            NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(nd);
            return false;
        }


        // Build the sequence
        SongContext sgContext = new SongContext(sg, midiMix);
        SongSequenceBuilder seqBuilder = new SongSequenceBuilder(sgContext);
        SongSequenceBuilder.SongSequence songSequence = null;
        try
        {
            songSequence = seqBuilder.buildExportableSequence(false, false);
        } catch (MusicGenerationException ex)
        {
            LOGGER.log(Level.WARNING, "export() ex={0}", ex.getMessage());   
            if (ex.getLocalizedMessage() != null)
            {
                NotifyDescriptor d = new NotifyDescriptor.Message(ex.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(d);
            }
            return false;
        }


        assert songSequence != null;   
        Sequence sequence = songSequence.sequence;


        // Add click & precount tracks if required
        var ps = PlaybackSettings.getInstance();
        if (ps.isPlaybackClickEnabled())
        {
            ps.addClickTrack(sequence, sgContext);
        }
        if (ps.isClickPrecountEnabled())
        {
            ps.addPrecountClickTrack(sequence, sgContext);      // Must be done last, shift all events
        }


        // Dump sequence in debug mode
        if (MusicController.getInstance().isDebugPlayedSequence())
        {
            LOGGER.log(Level.INFO, "export() sg={0} - sequence :", sg.getName());   
            LOGGER.info(MidiUtilities.toString(sequence));   
        }


        // Write to file
        LOGGER.log(Level.INFO, "export() writing sequence to Midi file: {0}", midiFile.getAbsolutePath());   
        try
        {
            MidiSystem.write(sequence, 1, midiFile);
            StatusDisplayer.getDefault().setStatusText(ResUtil.getString(SongExportSupport.class, "CTL_MidiSequenceWritten", midiFile.getAbsolutePath()));
        } catch (IOException ex)
        {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);   
            NotifyDescriptor d = new NotifyDescriptor.Message(ex.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return false;
        }

        return true;
    }
}

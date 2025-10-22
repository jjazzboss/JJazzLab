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
package org.jjazz.musiccontrol.api;

import com.google.common.base.Preconditions;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Track;
import org.jjazz.musiccontrol.spi.ActiveSongBackgroundMusicBuilder;
import org.jjazz.midi.api.MidiUtilities;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythmmusicgeneration.api.SongSequenceBuilder;
import org.jjazz.song.api.Song;
import org.jjazz.songcontext.api.SongContext;
import org.jjazz.utilities.api.ResUtil;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.StatusDisplayer;

/**
 * Helper methods to export a Song to a Midi file.
 */
public class SongMidiExporter
{

    private static final Logger LOGGER = Logger.getLogger(SongMidiExporter.class.getSimpleName());

    /**
     * Export a song to the specified midi File.
     * <p>
     * If song is the active song, try to reuse the last result from the ActiveSongMusicBuilder, otherwise generate the music. Notify user if a problem occured.
     *
     * @param song
     * @param midiMix
     * @param midiFile
     * @param rv       If non-null, only export the specified RhythmVoice
     * @return True if write was successful.
     */
    static public boolean songToMidiFile(Song song, MidiMix midiMix, File midiFile, RhythmVoice rv)
    {
        Preconditions.checkNotNull(song);
        Preconditions.checkNotNull(midiMix);


        // Playback must be stopped, otherwise seems to have side effects on the generated Midi file (missing tracks...?)
        MusicController.getInstance().stop();


        // Check if there is at least one unmuted track
        boolean allMuted = midiMix.getInstrumentMixes().stream()
                .allMatch(insMix -> insMix.isMute());
        if (allMuted)
        {
            String msg = ResUtil.getString(SongMidiExporter.class, "ERR_AllChannelsMuted");
            LOGGER.warning(msg);
            NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(nd);
            return false;
        }

        var sgContext = new SongContext(song, midiMix);
        var ssb = new SongSequenceBuilder(sgContext);
        SongSequenceBuilder.SongSequence songSequence = null;


        // First try to reuse the ActiveSongBackgroundMusicBuilder service if available
        var asmb = ActiveSongBackgroundMusicBuilder.getDefault();
        if (asmb != null)
        {
            var result = asmb.getLastResult();
            if (asmb.isLastResultUpToDate() && result.throwable() == null && result.songContext().getSong() == song)
            {
                // We can reuse the last music generation
                songSequence = ssb.buildSongSequence(result.mapRvPhrases());
            }
        }

        if (songSequence == null)
        {
            try
            {
                // Need to build music, this might take some time for a large song
                // Don't make it silent: user must be aware we're generating music at the start of dragging
                songSequence = ssb.buildAll(false);         // throws MusicGenerationException
            } catch (MusicGenerationException ex)
            {
                String exceptionError = ex.getMessage();
                String msg = ResUtil.getString(SongMidiExporter.class, "MidiExportProblem", exceptionError);
                NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(d);
                LOGGER.log(Level.WARNING, "songToMidiFile() exception={0}", exceptionError);
                return false;
            }
        }


        // Make the sequence exportable
        ssb.makeSequenceExportable(songSequence, false);


        // Keep only rv track if defined
        if (rv != null)
        {
            int trackId = songSequence.mapRvTrackId.get(rv);
            assert trackId != 0;

            // Remove all tracks except trackId and track0 (which contain Tempo change event etc.), need to start from last track
            Track[] tracks = songSequence.sequence.getTracks();
            for (int i = tracks.length - 1; i > 0; i--)
            {
                if (i != trackId)
                {
                    songSequence.sequence.deleteTrack(tracks[i]);
                }
            }

        } else
        {
            // Add click & precount tracks
            var ps = PlaybackSettings.getInstance();
            if (ps.isPlaybackClickEnabled())
            {
                ps.addClickTrack(songSequence.sequence, sgContext);
            }
            if (ps.isClickPrecountEnabled())
            {
                ps.addPrecountClickTrack(songSequence.sequence, sgContext);      // Must be done last, shift all events
            }
        }

        // Dump sequence in debug mode
        if (MusicController.getInstance().isDebugPlayedSequence())
        {
            LOGGER.log(Level.INFO, "songToMidiFile() sg={0} - sequence :", song.getName());
            LOGGER.info(MidiUtilities.toString(songSequence.sequence));
        }


        // Write to file
        LOGGER.log(Level.INFO, "songToMidiFile() writing sequence to Midi file: {0}", midiFile.getAbsolutePath());
        try
        {
            MidiSystem.write(songSequence.sequence, 1, midiFile);
            StatusDisplayer.getDefault().setStatusText(ResUtil.getString(SongMidiExporter.class, "CTL_MidiSequenceWritten",
                    midiFile.getAbsolutePath()));
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

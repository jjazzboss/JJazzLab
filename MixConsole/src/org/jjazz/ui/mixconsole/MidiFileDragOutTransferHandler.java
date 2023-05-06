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
package org.jjazz.ui.mixconsole;

import com.google.common.base.Preconditions;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Track;
import javax.swing.JComponent;
import javax.swing.TransferHandler;
import org.jjazz.backgroundsongmusicbuilder.api.ActiveSongMusicBuilder;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.musiccontrol.api.PlaybackSettings;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythmmusicgeneration.api.SongSequenceBuilder;
import org.jjazz.song.api.Song;
import org.jjazz.songcontext.api.SongContext;
import org.jjazz.ui.utilities.api.FileTransferable;
import org.jjazz.ui.utilities.api.MidiFileDragInTransferHandler;
import org.jjazz.util.api.ResUtil;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;

/**
 * Drag'n drop support to export the song or a single track as Midi file when mouse dragging from a component.
 * <p>
 */
public class MidiFileDragOutTransferHandler extends TransferHandler
{

    private final RhythmVoice rhythmVoice;
    private final Song songModel;
    private final MidiMix songMidiMix;
    private static final Logger LOGGER = Logger.getLogger(MidiFileDragOutTransferHandler.class.getSimpleName());


    /**
     *
     * @param song
     * @param midiMix
     * @param rv      If null, export the whole sequence, otherwise only the rv track.
     */
    public MidiFileDragOutTransferHandler(Song song, MidiMix midiMix, RhythmVoice rv)
    {
        Preconditions.checkNotNull(song);
        Preconditions.checkNotNull(midiMix);
        this.songModel = song;
        this.songMidiMix = midiMix;
        this.rhythmVoice = rv;
    }

    @Override
    public int getSourceActions(JComponent jc)
    {
        LOGGER.log(Level.FINE, "getSourceActions()  jc={0}", jc.getName());
        int res = TransferHandler.NONE;
        // Make sure we'll be able to generate a song
        if (songModel != null && songMidiMix != null)
        {
            if (!songModel.getSongStructure().getSongParts().isEmpty() && (rhythmVoice != null || !isAllMuted(songMidiMix)))
            {
                res = TransferHandler.COPY_OR_MOVE;
            }
        }
        return res;
    }

    @Override
    public Transferable createTransferable(JComponent jc)
    {
        LOGGER.log(Level.FINE, "createTransferable()  jc={0}", jc.getName());

        setDragImage(MidiFileDragInTransferHandler.DRAG_ICON.getImage());


        // Create the temp midi file
        final File midiFile;
        try
        {
            String baseName = songModel.getName();
            if (rhythmVoice != null)
            {
                baseName += "-" + rhythmVoice.getName();
            }
            midiFile = File.createTempFile(baseName, ".mid"); // throws IOException
            midiFile.deleteOnExit();
        } catch (IOException ex)
        {
            LOGGER.log(Level.WARNING, "createTransferable() temporary Midi file creation exception={0}", ex.getMessage());
            NotifyDescriptor d = new NotifyDescriptor.Message(ex.getMessage(), NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return null;
        }


        // Write the temp midi file
        try
        {
            exportSequenceToMidiTempFile(rhythmVoice, midiFile);
        } catch (IOException | MusicGenerationException ex)
        {
            String exceptionError = ex.getMessage();
            String msg = ResUtil.getString(getClass(), "MidiExportProblem", exceptionError);
            NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            LOGGER.log(Level.WARNING, "createTransferable() exception={0}", exceptionError);
            return null;
        }


        Transferable t = new FileTransferable(midiFile);
        return t;
    }


    @Override
    public boolean canImport(TransferHandler.TransferSupport support)
    {
        return false;
    }


    @Override
    public boolean importData(TransferHandler.TransferSupport support)
    {
        throw new UnsupportedOperationException();
    }


    // ===============================================================================================
    // Private methods
    // ===============================================================================================
    private boolean isAllMuted(MidiMix mm)
    {
        boolean res = true;
        for (RhythmVoice rv : mm.getRhythmVoices())
        {
            if (!mm.getInstrumentMix(rv).isMute())
            {
                res = false;
                break;
            }
        }
        return res;
    }


    /**
     * Build an exportable sequence to a midi file.
     * <p>
     * If song is the active song, try to reuse the last result from the ActiveSongMusicBuilder, otherwise generate the music.
     *
     * @param rv       If not null export only the rv track, otherwise all tracks.
     * @param midiFile
     * @throws IOException
     * @throws MusicGenerationException
     */
    private void exportSequenceToMidiTempFile(RhythmVoice rv, File midiFile) throws IOException, MusicGenerationException
    {
        LOGGER.log(Level.FINE, "exportSequenceToMidiTempFile() -- rv={0} midiFile={1}", new Object[]
        {
            rv, midiFile
        });

        var sgContext = new SongContext(songModel, songMidiMix);
        var ssb = new SongSequenceBuilder(sgContext);
        SongSequenceBuilder.SongSequence songSequence;


        var asmb = ActiveSongMusicBuilder.getInstance();
        var result = asmb.getLastResult();
        if (asmb.getSong() == songModel && result != null && result.userException() == null)
        {
            // We can reuse the last music generation
            songSequence = ssb.buildSongSequence(result.mapRvPhrases());

        } else
        {
            // Need to build music, this might take some time for a large song 
            // Don't make it silent: user must be aware we're generating music at the start of dragging
            songSequence = ssb.buildAll(false);    // throws MusicGenerationException

        }


        // Make the sequence exportable
        ssb.makeSequenceExportable(songSequence, false);


        // Keep only rv track if defined
        if (rv != null)
        {
            int trackId = songSequence.mapRvTrackId.get(rv);

            // Remove all tracks except trackId, need to start from last track
            Track[] tracks = songSequence.sequence.getTracks();
            for (int i = tracks.length - 1; i >= 0; i--)
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


        // Write the midi file     
        MidiSystem.write(songSequence.sequence, 1, midiFile);       // throws IOException

    }

    // ===============================================================================================
    // Inner classes
    // ===============================================================================================

}

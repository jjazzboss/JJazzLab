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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Track;
import javax.swing.JComponent;
import javax.swing.TransferHandler;
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
 * Win and Linux only.
 */
public class MidiFileDragOutTransferHandler extends TransferHandler
{

    private final RhythmVoice rhythmVoice;
    private final Song songModel;
    private final MidiMix songMidiMix;
    private Future<?> future;
    private ExecutorService executorService;
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
        executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    public int getSourceActions(JComponent c)
    {
        LOGGER.log(Level.FINE, "getSourceActions()  c={0}", c);
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
        LOGGER.log(Level.FINE, "createTransferable()  jc={0}", jc.getClass());

        setDragImage(MidiFileDragInTransferHandler.DRAG_ICON.getImage());

        final File midiFile;
        try
        {
            // Create the temp file
            midiFile = File.createTempFile("JJazzMixConsoleDragOut", ".mid"); // throws IOException
            midiFile.deleteOnExit();
        } catch (IOException ex)
        {
            NotifyDescriptor d = new NotifyDescriptor.Message(ex.getMessage(), NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return null;
        }


        Runnable task = new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    LOGGER.log(Level.FINE, "Exporting sequence to {0}", midiFile.getAbsolutePath());
                    exportSequenceToMidiTempFile(rhythmVoice, midiFile);
                    LOGGER.log(Level.FINE, "Completed export   to {0}", midiFile.getAbsolutePath());
                } catch (IOException | MusicGenerationException e)
                {
                    // Notify right away
                    String exceptionError = e.getMessage();
                    String msg = ResUtil.getString(getClass(), "MidiExportProblem", exceptionError);
                    NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
                    DialogDisplayer.getDefault().notify(d);
                    LOGGER.log(Level.WARNING, "createTransferable().task.run() ex={0}", exceptionError);
                }
            }
        };
        // Generation might take a few seconds, we can't block UI, see Issue #345 
        future = executorService.submit(task);


        List<File> data = midiFile == null ? null : Arrays.asList(midiFile);
        Transferable t = new FileTransferable(data);
        return t;
    }


    /**
     * Check if music generation was ok.
     *
     * @param c
     * @param data
     * @param action DnDConstants.NONE=0 / COPY=1 / MOVE=2
     */
    @Override
    protected void exportDone(JComponent c, Transferable data, int action)
    {
        // Will be called if drag was initiated from this handler
        LOGGER.log(Level.FINE, "exportDone()  c={0} action={1} future.isDone()={2}",
                new Object[]
                {
                    c.getClass(), action, future.isDone()
                });

        // The action seems to vary depending on the import result
        // When the receiving component does not support import (no TransferHandler if it's a Swing components), or if an import error occured, then action == DnDConstants.NONE.
        // We use this to avoid triggering the warning below for nothing
        if (action == 0)
        {
            return;
        }


        // Check if we were not done exporting the midiFile yet
        assert future != null;
        if (!future.isDone())
        {
            String msg = ResUtil.getString(getClass(), "DragMusicGenerationNotComplete");
            NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            future.cancel(true);
        }

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


        // Build the sequence
        var sgContext = new SongContext(songModel, songMidiMix);
        SongSequenceBuilder.SongSequence songSequence = new SongSequenceBuilder(sgContext).buildExportableSequence(true, false); // throws MusicGenerationException


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
        MidiSystem.write(songSequence.sequence, 1, midiFile);   // throws IOException

    }

    // ===============================================================================================
    // Inner classes
    // ===============================================================================================

}

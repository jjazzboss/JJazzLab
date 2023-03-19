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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
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
import org.openide.util.Exceptions;

/**
 * Drag'n drop support to export the song or a single track as Midi file when mouse dragging from a component.
 */
public class MidiFileDragOutTransferHandler extends TransferHandler
{

    /**
     * Shared flag between instances.
     */
    static private boolean blockExceptionCheckOnExportDone;

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

        blockExceptionCheckOnExportDone = false;
        setDragImage(MidiFileDragInTransferHandler.DRAG_ICON.getImage());

        File midiFile = null;
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


        final var midiFile2 = midiFile;
        Runnable task = new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    exportSequenceToMidiTempFile(rhythmVoice, midiFile2);
                } catch (IOException | MusicGenerationException e)
                {
                    // Notify right away
                    blockExceptionCheckOnExportDone = true;              // Need to be first because exportDone might be called in parallel
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
     * @param action
     */
    @Override
    protected void exportDone(JComponent c, Transferable data, int action)
    {
        // Will be called if drag was initiated from this handler
        LOGGER.log(Level.FINE, "exportDone()  c={0} action={1} future.isDone()={2} blockExceptionCheckOnExportDone={3}",
                new Object[]
                {
                    c.getClass(), action, future.isDone(), blockExceptionCheckOnExportDone
                });

        if (blockExceptionCheckOnExportDone)
        {
            // This avoid notifying user of "music generatino not finished yet" error if we dropped on ourselves
            // Note that it won't prevent to have the notification when dropping on a different JJazzLab region (ed CL_Editor) which does not handle Midi file DnD.
            return;
        }

        assert future != null;
        if (!future.isDone())
        {
            String err = ResUtil.getString(getClass(), "DragMusicGenerationNotComplete");
            String msg = ResUtil.getString(getClass(), "MidiExportProblem", err);
            NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            future.cancel(true);                        
        }

    }


    /**
     * Set to true so that importData is called and we can detect when we drop on ourselves.
     *
     * @param support
     * @return
     */
    @Override
    public boolean canImport(TransferHandler.TransferSupport support)
    {
        return true;
    }


    /**
     * Do nothing if we drop on one of the MidiFileDragOutTransferHandler instances.
     *
     * @param support
     * @return
     */
    @Override
    public boolean importData(TransferHandler.TransferSupport support)
    {
        LOGGER.fine("importData() -- setting blockExceptionCheckOnExportDone=true");
        blockExceptionCheckOnExportDone = true;
        return false;
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

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
package org.jjazz.mixconsole;

import com.google.common.base.Preconditions;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.TransferHandler;
import static javax.swing.TransferHandler.COPY;
import org.jjazz.musiccontrol.api.SongMidiExporter;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.mixconsole.api.MixConsoleTopComponent;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.song.api.Song;
import org.jjazz.uiutilities.api.FileTransferable;
import org.jjazz.uiutilities.api.SingleFileDragInTransferHandler;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;

/**
 * Drag'n drop support to export the song or a single track as Midi file when mouse dragging from a component, and to handle song file drag-in.
 * <p>
 * Note that behaviour is different on MacOS: getSourceActions(), createTransferable(), exportDone() can be called several times during a drag operation ! (only
 * 1 for Win/Linux). Also on MacOS the support parameter is not always fully initialized on canImport(), is is fully initialized only when importData() is
 * called (see MidiFileDragInTransferHandler.java for example).
 */
public class MixConsoleTransferHandler extends TransferHandler
{

    private final RhythmVoice rhythmVoice;
    private final Song songModel;
    private final MidiMix songMidiMix;

    private static final Logger LOGGER = Logger.getLogger(MixConsoleTransferHandler.class.getSimpleName());


    /**
     *
     * @param song
     * @param midiMix
     * @param rv      If null, export the whole sequence, otherwise only the rv track.
     */
    public MixConsoleTransferHandler(Song song, MidiMix midiMix, RhythmVoice rv)
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

        setDragImage(SingleFileDragInTransferHandler.DRAG_MIDI_FILE_ICON.getImage());


        // Create the temp midi file
        final File midiFile;
        try
        {
            String baseName = songModel.getName() + "-";
            if (rhythmVoice != null)
            {
                baseName += rhythmVoice.getName() + "-";
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
        if (!SongMidiExporter.songToMidiFile(songModel, songMidiMix, midiFile, rhythmVoice))
        {
            return null;
        }


        Transferable t = new FileTransferable(midiFile);
        return t;
    }


    @Override
    public boolean canImport(TransferHandler.TransferSupport info)
    {
        return MixConsoleTopComponent.getInstance().getTransferHandler().canImport(info);
    }


    @Override
    public boolean importData(TransferHandler.TransferSupport info)
    {
       return MixConsoleTopComponent.getInstance().getTransferHandler().importData(info);
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


    // ===============================================================================================
    // Inner classes
    // ===============================================================================================
}

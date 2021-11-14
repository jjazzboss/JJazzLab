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
package org.jjazz.ui.mixconsole.actions;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import org.jjazz.analytics.api.Analytics;
import org.jjazz.filedirectorymanager.api.FileDirectoryManager;
import org.jjazz.midi.api.InstrumentMix;
import org.jjazz.midi.api.MidiUtilities;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.api.MidiMixManager;
import org.jjazz.musiccontrol.api.PlaybackSettings;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.rhythmmusicgeneration.api.SongSequenceBuilder;
import org.jjazz.songcontext.api.SongContext;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.song.api.Song;
import org.jjazz.ui.utilities.api.Utilities;
import org.jjazz.util.api.ResUtil;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.awt.StatusDisplayer;
import org.openide.windows.WindowManager;

/**
 * Export song to a midi file.
 */
@ActionID(category = "MusicControls", id = "org.jjazz.ui.mixconsole.actions.exporttomidifile")
@ActionRegistration(displayName = "#CTL_ExportToMidiFile", lazy = true)
@ActionReferences(
        {
            @ActionReference(path = "Menu/File", position = 1580, separatorAfter = 1590)
        })
public class ExportToMidiFile extends AbstractAction
{

    private Song song;
    private static File saveExportDir = null;

    private static final Logger LOGGER = Logger.getLogger(ExportToMidiFile.class.getSimpleName());

    public ExportToMidiFile(Song context)
    {
        song = context;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        assert song != null;   //NOI18N

        if (song.getSongStructure().getSongParts().isEmpty())
        {
            String msg = ResUtil.getString(getClass(), "ERR_CantExportEmptySong");
            NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(nd);
            return;
        }


        // Get the target midi file
        File midiFile = getMidiFile(song);
        if (midiFile == null)
        {
            String msg = ResUtil.getString(getClass(), "ERR_CantBuildMidiFile", song.getName());
            NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(nd);
            return;
        }
        JFileChooser chooser = Utilities.getFileChooserInstance();
        chooser.resetChoosableFileFilters();
        chooser.setMultiSelectionEnabled(false);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setSelectedFile(midiFile);
        chooser.setDialogTitle(ResUtil.getString(getClass(), "CTL_ExportToMidiDialogTitle"));
        int res = chooser.showSaveDialog(WindowManager.getDefault().getMainWindow());
        if (res != JFileChooser.APPROVE_OPTION)
        {
            return;
        }

        midiFile = chooser.getSelectedFile();
        saveExportDir = midiFile.getParentFile();


        if (midiFile.exists())
        {
            // File overwrite confirm dialog
            String msg = ResUtil.getString(getClass(), "CTL_ConfirmFileOverwrite", midiFile);
            NotifyDescriptor nd = new NotifyDescriptor.Confirmation(msg, NotifyDescriptor.OK_CANCEL_OPTION);
            Object result = DialogDisplayer.getDefault().notify(nd);
            if (result != NotifyDescriptor.OK_OPTION)
            {
                return;
            }
        }


        generateAndWriteMidiFile(song, midiFile);

    }


    // ======================================================================
    // Private methods
    // ======================================================================   
    /**
     *
     * @param sg
     * @return Can be null
     */
    private File getMidiFile(Song sg)
    {
        File f = null;
        File songFile = sg.getFile();
        String midiFilename = (songFile == null) ? sg.getName() + ".mid" : org.jjazz.util.api.Utilities.replaceExtension(songFile.getName(), ".mid");
        if (saveExportDir != null && !saveExportDir.isDirectory())
        {
            saveExportDir = null;
        }
        File dir = saveExportDir;
        if (dir == null)
        {
            if (songFile != null)
            {
                dir = songFile.getParentFile();         // Can be null
            }
            if (dir == null)
            {
                FileDirectoryManager fdm = FileDirectoryManager.getInstance();
                dir = fdm.getLastSongDirectory();       // Can be null                       
            }
        }
        if (dir != null)
        {
            f = new File(dir, midiFilename);
        }
        return f;
    }

    /**
     * Generate the sequence for specified song and write to a midi File.
     * <p>
     * Notify user if problem occured.
     *
     * @param sg
     * @param midiFile
     * @return True if write was successful.
     */
    private boolean generateAndWriteMidiFile(Song sg, File midiFile)
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
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);   //NOI18N
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
            String msg = ResUtil.getString(ExportToMidiFile.class, "ERR_AllChannelsMuted");
            LOGGER.warning(msg);   //NOI18N
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
            LOGGER.warning("generateAndWriteMidiFile() ex=" + ex.getMessage());   //NOI18N
            if (ex.getLocalizedMessage() != null)
            {
                NotifyDescriptor d = new NotifyDescriptor.Message(ex.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(d);
            }
            return false;
        }


        assert songSequence != null;   //NOI18N
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
            LOGGER.info("generateAndWriteMidiFile() sg=" + sg.getName() + " - sequence :");   //NOI18N
            LOGGER.info(MidiUtilities.toString(sequence));   //NOI18N
        }


        // Write to file
        LOGGER.info("generateAndWriteMidiFile() writing sequence to Midi file: " + midiFile.getAbsolutePath());   //NOI18N
        try
        {
            MidiSystem.write(sequence, 1, midiFile);
            StatusDisplayer.getDefault().setStatusText(ResUtil.getString(ExportToMidiFile.class, "CTL_MidiSequenceWritten", midiFile.getAbsolutePath()));
        } catch (IOException ex)
        {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);   //NOI18N
            NotifyDescriptor d = new NotifyDescriptor.Message(ex.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return false;
        }

        return true;
    }
}

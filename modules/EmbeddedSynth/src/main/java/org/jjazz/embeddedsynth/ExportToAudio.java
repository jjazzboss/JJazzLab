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
package org.jjazz.embeddedsynth;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Logger;
import javax.sound.midi.MidiUnavailableException;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jjazz.backgroundsongmusicbuilder.api.SongMidiExporter;
import org.jjazz.embeddedsynth.api.EmbeddedSynth;
import org.jjazz.embeddedsynth.api.EmbeddedSynthException;
import org.jjazz.embeddedsynth.spi.EmbeddedSynthProvider;
import org.jjazz.embeddedsynth.spi.Mp3EncoderProvider;
import org.jjazz.song.api.Song;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.api.MidiMixManager;
import org.jjazz.uiutilities.api.UIUtilities;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.utilities.api.Utilities;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.awt.StatusDisplayer;
import org.openide.util.Exceptions;
import org.openide.windows.WindowManager;

/**
 * Export song to a midi file.
 */
@ActionID(category = "MusicControls", id = "org.jjazz.embeddedsynth.exporttoaudio")
@ActionRegistration(displayName = "#CTL_ExportToAudio", lazy = true)
@ActionReferences(
        {
            @ActionReference(path = "Menu/File", position = 1585)
        })
public class ExportToAudio extends AbstractAction
{

    private final Song song;
    private static File saveExportDir = null;

    private static final Logger LOGGER = Logger.getLogger(ExportToAudio.class.getSimpleName());

    public ExportToAudio(Song context)
    {
        song = context;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        assert song != null;

        EmbeddedSynth synth = EmbeddedSynthProvider.getDefaultSynth();
        if (synth == null || !synth.isOpen())
        {
            String msg = ResUtil.getString(getClass(), "ERR_NoEmbeddedSynth");
            NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(nd);
            return;
        }


        if (song.getSongStructure().getSongParts().isEmpty())
        {
            String msg = ResUtil.getString(getClass(), "ERR_CantExportEmptySong");
            NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(nd);
            return;
        }


        // Get the target audio file
        String songNameNoBlank = song.getName().replace(" ", "_");
        File audioFile = new File(songNameNoBlank + ".mp3");
        var audioFileFilter = new FileNameExtensionFilter(ResUtil.getString(getClass(), "AudioFiles"), "wav", "mp3");
        JFileChooser chooser = UIUtilities.getFileChooserInstance();
        chooser.resetChoosableFileFilters();
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.addChoosableFileFilter(audioFileFilter);
        chooser.setMultiSelectionEnabled(false);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setCurrentDirectory(saveExportDir);
        chooser.setSelectedFile(audioFile);
        chooser.setDialogTitle(ResUtil.getString(getClass(), "CTL_ExportToAudioDialogTitle"));
        int res = chooser.showSaveDialog(WindowManager.getDefault().getMainWindow());
        if (res != JFileChooser.APPROVE_OPTION)
        {
            return;
        }
        audioFile = chooser.getSelectedFile();
        saveExportDir = audioFile.getParentFile();


        // Check extension is valid
        boolean isMp3 = Utilities.getExtension(audioFile.getName()).equalsIgnoreCase("mp3");
        boolean isWav = Utilities.getExtension(audioFile.getName()).equalsIgnoreCase("wav");
        if (!isMp3 && !isWav)
        {
            String msg = ResUtil.getString(getClass(), "UnsupportedFileExtension", audioFile.getName());
            NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(nd);
            return;
        }


        if (audioFile.exists())
        {
            // File overwrite confirm dialog
            String msg = ResUtil.getString(getClass(), "CTL_ConfirmFileOverwrite", audioFile.getName());
            NotifyDescriptor nd = new NotifyDescriptor.Confirmation(msg, NotifyDescriptor.OK_CANCEL_OPTION);
            Object result = DialogDisplayer.getDefault().notify(nd);
            if (result != NotifyDescriptor.OK_OPTION)
            {
                return;
            }
        }


        // Generate the Midi file
        File midiFile;
        MidiMix midiMix;
        try
        {
            midiFile = Files.createTempFile(songNameNoBlank, ".mid").toFile();
            midiMix = MidiMixManager.getInstance().findMix(song);
        } catch (MidiUnavailableException | IOException ex)
        {
            // Should not happen
            Exceptions.printStackTrace(ex);
            return;
        }
        assert midiMix != null : "song=" + song;
        if (!SongMidiExporter.songToMidiFile(song, midiMix, midiFile, null))        // Notifies user if error occurs while exporting
        {
            // An error occured
            return;
        }


        // Generate the wav file
        File wavFile = audioFile;       // Assume isWav by default
        if (isMp3)
        {
            try
            {
                wavFile = Files.createTempFile(songNameNoBlank, ".wav").toFile();
            } catch (IOException ex)
            {
                Exceptions.printStackTrace(ex);
                return;
            }
        };
        try
        {
            synth.generateWavFile(midiFile, wavFile);
        } catch (EmbeddedSynthException ex)
        {
            String msg = ResUtil.getString(getClass(), "ErrorGeneratingAudioFile", wavFile.getAbsolutePath(), ex.getLocalizedMessage());
            NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(nd);
            return;
        }


        if (isMp3)
        {
            var mp3Encoder = Mp3EncoderProvider.getDefault();
            assert mp3Encoder != null;
            try
            {
                mp3Encoder.encode(wavFile, audioFile, false, false);
            } catch (EmbeddedSynthException ex)
            {
                String msg = ResUtil.getString(getClass(), "ErrorGeneratingAudioFile", audioFile.getAbsolutePath(), ex.getLocalizedMessage());
                NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(nd);
                return;
            }
        }

        
        StatusDisplayer.getDefault().setStatusText(ResUtil.getString(getClass(), "ExportToAudioComplete",
                audioFile.getAbsolutePath()));


    }


    // ======================================================================
    // Private methods
    // ======================================================================   
}

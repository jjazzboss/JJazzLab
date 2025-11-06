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
package org.jjazz.songeditormanager;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicFileChooserUI;
import org.jjazz.analytics.api.Analytics;
import org.jjazz.musiccontrol.api.SongMidiExporter;
import org.jjazz.embeddedsynth.api.EmbeddedSynth;
import org.jjazz.embeddedsynth.api.EmbeddedSynthException;
import org.jjazz.embeddedsynth.spi.EmbeddedSynthProvider;
import org.jjazz.embeddedsynth.spi.Mp3EncoderProvider;
import org.jjazz.filedirectorymanager.api.FileDirectoryManager;
import org.jjazz.song.api.Song;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.spi.MidiMixManager;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.utilities.api.Utilities;
import org.netbeans.api.progress.BaseProgressUtils;
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
@ActionID(category = "File", id = "org.jjazz.songeditormanager.exporttoaudio")
@ActionRegistration(displayName = "#CTL_ExportToAudio", lazy = true)
@ActionReferences(
        {
            @ActionReference(path = "Menu/File", position = 1585)
        })
public class ExportToAudio extends AbstractAction
{

    private static JFileChooser FILE_CHOOSER;
    private static AccessoryComponent accessoryComponent;

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


        MidiMix midiMix = MidiMixManager.getDefault().findExistingMix(song);
        int nbNonMutedChannels = (int) midiMix.getInstrumentMixes().stream().filter(im -> !im.isMute()).count();
        if (nbNonMutedChannels == 0)
        {
            String msg = ResUtil.getString(getClass(), "ERR_CantExportAllChannelsMuted");
            LOGGER.warning(msg);
            NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(nd);
            return;
        }

        // Show file chooser
        File audioFile = ExportToMidiFile.getExportFile(song, ".mp3", saveExportDir);        
        JFileChooser chooser = getFileChooser(audioFile);
        int res = chooser.showSaveDialog(WindowManager.getDefault().getMainWindow());
        if (res != JFileChooser.APPROVE_OPTION)
        {
            return;
        }
        audioFile = chooser.getSelectedFile();
        String audioFileName = Utilities.replaceExtension(audioFile.getName(), "");
        String audioFileExt = Utilities.getExtension(audioFile.getName());
        File audioFileDir = audioFile.getParentFile();
        saveExportDir = audioFileDir;
        boolean separateTracks = accessoryComponent.cb_oneFilePerTrack.isSelected();


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


        if (separateTracks)
        {
            // audioFile will be the first midi track to export
            RhythmVoice rv = getFirstNonMutedRv(midiMix);
            String fileName0 = buidTrackFilename(audioFileName, rv, audioFileExt);
            audioFile = new File(audioFileDir, fileName0);
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


        LOGGER.log(Level.INFO, "actionPerformed() Start export audio {0}, oneAudioPerTrack={1}", new Object[]
        {
            audioFile.getAbsolutePath(), separateTracks
        });
        Analytics.logEvent("Export audio", Analytics.buildMap("separateTracks", separateTracks));


        // Prepare temporary files
        final File tmpMidiFile;
        final File tmpWavFile;
        try
        {
            tmpMidiFile = Files.createTempFile(audioFileName, ".mid").toFile();
            tmpWavFile = isMp3 ? Files.createTempFile(audioFileName, ".wav").toFile() : null;
        } catch (IOException ex)
        {
            // Should never happen
            Exceptions.printStackTrace(ex);
            return;
        }


        // Prepare the MidiMixes
        List<MidiMix> midiMixes = Arrays.asList(midiMix);           // By default export the whole song as is
        if (separateTracks)
        {
            // Create for each non-muted channel a specific mix
            midiMixes = new ArrayList<>();
            for (int channel : midiMix.getUsedChannels())
            {
                if (!midiMix.getInstrumentMix(channel).isMute())
                {
                    var newMidiMix = midiMix.getDeepCopy();
                    for (int newChannel : newMidiMix.getUsedChannels())
                    {
                        newMidiMix.getInstrumentMix(newChannel).setMute(newChannel != channel);

                    }
                    midiMixes.add(newMidiMix);
                }
            }
        }


        // Prepare task
        final var midiMixesFinal = midiMixes;
        final var audioFileFinal = audioFile;
        class ProcessTask implements Runnable
        {

            String errorMessage = null;

            @Override
            public void run()
            {
                // Perform the export for each MidiMix
                File file = audioFileFinal;
                for (MidiMix mm : midiMixesFinal)
                {
                    if (separateTracks)
                    {
                        RhythmVoice rv = getFirstNonMutedRv(mm);
                        file = new File(audioFileDir, buidTrackFilename(audioFileName, rv, audioFileExt));
                    }

                    // Generate
                    if (!SongMidiExporter.songToMidiFile(song, mm, tmpMidiFile, null))        // Notifies user if error occurs while exporting
                    {
                        return;
                    }
                    if (tmpMidiFile.length() < 10)                      // Robustness
                    {
                        errorMessage = ResUtil.getString(getClass(), "ErrorGeneratingAudioFile",
                                file.getAbsolutePath(),
                                "temporary Midi file is empty " + tmpMidiFile.getAbsolutePath());
                        return;
                    }

                    // Generate the wav file
                    File wavFile = isMp3 ? tmpWavFile : file;
                    try
                    {
                        synth.generateWavFile(tmpMidiFile, wavFile);
                        if (wavFile.length() < 10)
                        {
                            throw new EmbeddedSynthException("generated file is empty");        // Robustness
                        }
                    } catch (EmbeddedSynthException ex)
                    {
                        errorMessage = ResUtil.getString(getClass(), "ErrorGeneratingAudioFile", wavFile.getAbsolutePath(), ex.getLocalizedMessage());
                        return;
                    }

                    if (isMp3)
                    {
                        var mp3Encoder = Mp3EncoderProvider.getDefault();
                        assert mp3Encoder != null;
                        try
                        {
                            mp3Encoder.encode(wavFile, file, false, false);
                            if (file.length() == 0)
                            {
                                throw new EmbeddedSynthException("generated file is empty");
                            }
                        } catch (EmbeddedSynthException ex)
                        {
                            errorMessage = ResUtil.getString(getClass(), "ErrorGeneratingAudioFile", file.getAbsolutePath(), ex.getLocalizedMessage());
                            return;
                        }
                    }
                }
            }
        }


        // Run task
        String param = separateTracks ? audioFile.getName() + " + " + (nbNonMutedChannels - 1) + " " + ResUtil.getString(getClass(), "OtherFiles")
                : audioFile.getName();
        ProcessTask task = new ProcessTask();
        BaseProgressUtils.showProgressDialogAndRun(task, ResUtil.getString(getClass(), "GeneratingAudioFile", param));
        if (task.errorMessage != null)
        {
            LOGGER.log(Level.WARNING, "actionPerformed() {0}", task.errorMessage);
            NotifyDescriptor nd = new NotifyDescriptor.Message(task.errorMessage, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(nd);
            return;
        }


        StatusDisplayer.getDefault().setStatusText(ResUtil.getString(getClass(), "ExportToAudioComplete", param));
        LOGGER.log(Level.INFO, "actionPerformed() Export to audio completed : {0}", param);

    }

    // ======================================================================
    // Private methods
    // ======================================================================   

    private String buidTrackFilename(String base, RhythmVoice rv, String ext)
    {
        return base + "-" + rv.getName() + "." + ext;
    }

    private RhythmVoice getFirstNonMutedRv(MidiMix mm)
    {
        RhythmVoice res = mm.getRhythmVoices().stream()
                .filter(rv -> !mm.getInstrumentMix(rv).isMute())
                .findFirst()
                .orElseThrow();
        return res;
    }

    private JFileChooser getFileChooser(File f)
    {
        if (FILE_CHOOSER == null)
        {
            FILE_CHOOSER = new JFileChooser();
            FILE_CHOOSER.setDialogType(JFileChooser.SAVE_DIALOG);
            FILE_CHOOSER.resetChoosableFileFilters();
            FILE_CHOOSER.setAcceptAllFileFilterUsed(false);
            FILE_CHOOSER.addChoosableFileFilter(new FileNameExtensionFilter(ResUtil.getString(getClass(), "AudioFilesMp3"), "mp3"));
            FILE_CHOOSER.addChoosableFileFilter(new FileNameExtensionFilter(ResUtil.getString(getClass(), "AudioFilesWav"), "wav"));
            FILE_CHOOSER.setMultiSelectionEnabled(false);
            FILE_CHOOSER.setFileSelectionMode(JFileChooser.FILES_ONLY);
            FILE_CHOOSER.setDialogTitle(ResUtil.getString(getClass(), "CTL_ExportToAudioDialogTitle"));

            
            // Adjust the selected file extension when user selects a different file filter
            FILE_CHOOSER.addPropertyChangeListener(JFileChooser.FILE_FILTER_CHANGED_PROPERTY, (var evt) -> 
            {
                FileNameExtensionFilter filter = (FileNameExtensionFilter) evt.getNewValue();
                String extension = filter.getExtensions()[0];
                // Can't use FILE_CHOOSER.getSelectedFile(), it returns null because file does not match filter, see https://stackoverflow.com/questions/596429/adjust-selected-file-to-filefilter-in-a-jfilechooser
                String typedName = ((BasicFileChooserUI) FILE_CHOOSER.getUI()).getFileName();
                if (!typedName.isBlank() && !typedName.toLowerCase().endsWith(extension.toLowerCase()))
                {
                    typedName = Utilities.replaceExtension(typedName, extension);
                    FILE_CHOOSER.setSelectedFile(new File(FILE_CHOOSER.getCurrentDirectory(), typedName));
                }
            });


            // Add the accessory component
            accessoryComponent = new AccessoryComponent();
            FILE_CHOOSER.setAccessory(accessoryComponent);
        }

        FILE_CHOOSER.setSelectedFile(f);


        return FILE_CHOOSER;
    }


    /**
     * An accessory component to propose to export 1 file per track
     */
    private static class AccessoryComponent extends JPanel
    {

        private JCheckBox cb_oneFilePerTrack = new JCheckBox(ResUtil.getString(getClass(), "ExportOneAudioFilePerTrack"));

        public AccessoryComponent()
        {
            cb_oneFilePerTrack.setToolTipText(ResUtil.getString(getClass(), "ExportOneAudioFilePerTrackTooltip"));
            add(cb_oneFilePerTrack);
        }
    }
}

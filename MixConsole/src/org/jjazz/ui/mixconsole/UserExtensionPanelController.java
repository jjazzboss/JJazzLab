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

import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MidiSystem;
import org.jjazz.midi.api.JJazzMidiSystem;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythmmusicgeneration.api.SongSequenceBuilder;
import org.jjazz.song.api.Song;
import org.jjazz.songcontext.api.SongContext;
import org.jjazz.ui.utilities.api.PleaseWaitDialog;
import org.jjazz.util.api.ResUtil;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Exceptions;

/**
 * The controller for a UserExtensionPanel.
 */
public class UserExtensionPanelController
{

    private UserExtensionPanel panel;
    private static final Logger LOGGER = Logger.getLogger(UserExtensionPanelController.class.getSimpleName());

    public void setUserExtentionPanel(UserExtensionPanel panel)
    {
        this.panel = panel;
    }

    public void userChannelNameEdited(String newName)
    {
        Song song = panel.getSong();
        String oldName = panel.getUserRhythmVoice().getName();
        Phrase p = song.getUserPhrase(oldName);
        song.removeUserPhrase(oldName);
        try
        {
            song.addUserPhrase(newName, p);
        } catch (PropertyVetoException ex)
        {
            // Should never happen since we replace an existing phrase
            Exceptions.printStackTrace(ex);
        }
    }

    public void editUserPhrase()
    {
        SongContext songContext = new SongContext(panel.getSong(), panel.getMidiMix());

        File midiFile;
        try
        {
            // Build and store sequence
            midiFile = exportSequenceToMidiTempFile(songContext);
        } catch (IOException | MusicGenerationException ex)
        {
            NotifyDescriptor d = new NotifyDescriptor.Message(ex.getMessage(), NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            LOGGER.warning("editUserPhrase() Can't create Midi file ex=" + ex.getMessage());
            return;
        }


        Runnable runEditorTask = () ->
        {
            try
            {
                // Start the midi editor
                JJazzMidiSystem.getInstance().editMidiFileWithExternalEditor(midiFile);     // Blocks until editor quits
            } catch (IOException ex)
            {
                NotifyDescriptor d = new NotifyDescriptor.Message(ex.getMessage(), NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(d);
                LOGGER.warning("editUserPhrase() Can't launch external Midi editor. ex=" + ex.getMessage());
                return;
            } finally
            {
                LOGGER.info("editUserPhrase() resuming from external Midi editing");
            }


            importMidiFile(midiFile);
        };

        
        // Prepare the text to be shown while editing
        RhythmVoice rv = panel.getUserRhythmVoice();
        String track = SongSequenceBuilder.buildTrackName(rv, songContext.getMidiMix().getChannel(rv));
        String msg = ResUtil.getString(getClass(), "UserExtensionPanelController.WaitForEditorQuit", track);
        var dlg = new PleaseWaitDialog(msg, runEditorTask);
        // dlg.setUndecorated(false);
        dlg.setVisible(true);       // Blocks
        
        
        LOGGER.info("editUserPhrase() POST EDIT");
    }

    public void closePanel()
    {
        Song song = panel.getSong();
        song.removeUserPhrase(panel.getUserRhythmVoice().getName());
    }


    // ============================================================================
    // Private methods
    // ============================================================================
    /**
     * Build an exportable sequence in a temp file.
     *
     * @return The generated Midi temporary file.
     * @throws IOException
     * @throws MusicGenerationException
     */
    private File exportSequenceToMidiTempFile(SongContext sgContext) throws IOException, MusicGenerationException
    {
        LOGGER.log(Level.FINE, "exportSequenceToMidiTempFile() -- sgContext={0}", sgContext);

        // Create the temp file
        File midiFile = File.createTempFile("JJazzUserPhrase", ".mid"); // throws IOException
        midiFile.deleteOnExit();


        // Build the sequence
        SongSequenceBuilder.SongSequence songSequence = new SongSequenceBuilder(sgContext).buildExportableSequence(true); // throws MusicGenerationException


        // Write the midi file     
        MidiSystem.write(songSequence.sequence, 1, midiFile);   // throws IOException

        return midiFile;
    }

    /**
     * Import phrases from the Midi file.
     * <p>
     * <p>
     * Notify user if problems occur.
     *
     * @param midiFile
     * @return True if import was successful
     */
    private boolean importMidiFile(File midiFile)
    {
        // Load file into a sequence
//        Sequence sequence;
//        try
//        {
//            sequence = MidiSystem.getSequence(midiFile);
//        } catch (IOException | InvalidMidiDataException ex)
//        {
//            NotifyDescriptor d = new NotifyDescriptor.Message(ex.getMessage(), NotifyDescriptor.ERROR_MESSAGE);
//            DialogDisplayer.getDefault().notify(d);
//            return false;
//        }
//
//        // LOGGER.severe("importMidiFile() importSequence=" + MidiUtilities.toString(importSequence));
//
//        // Get one phrase per channel
//        Track[] tracks = sequence.getTracks();
//        List<Phrase> phrases = Phrase.getPhrases(tracks);
//
//
//        boolean contentFound = false;
//        List<RhythmVoice> impactedRvs = new ArrayList<>();
//
//
//        // Check which phrases are relevant and if they have changed
//        MidiMix mm = songContext.getMidiMix();
//        for (Phrase pNew : phrases)
//        {
//            RhythmVoice rv = mm.getRhythmVoice(pNew.getChannel());
//            if (rv != null)
//            {
//                contentFound = true;
//                Phrase pOld = getPhrase(rv);
//                if (!pNew.equalsLoosePosition(pOld, PHRASE_COMPARE_BEAT_WINDOW))
//                {
//                    // LOGGER.info("importMidiFile() setting custom phrase for rv=" + rv);
//                    RP_SYS_CustomPhraseValue.SptPhrase sp = new RP_SYS_CustomPhraseValue.SptPhrase(pNew, songContext.getBeatRange().size(), songPart.getRhythm().getTimeSignature());
//                    addCustomizedPhrase(rv, sp);
//                    impactedRvs.add(rv);
//                }
//            }
//        }
//
//
//        // Notify user
//        if (!contentFound)
//        {
//            String msg = ResUtil.getString(getClass(), "RP_SYS_CustomPhraseComp.NoContent", midiFile.getName());
//            NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
//            DialogDisplayer.getDefault().notify(d);
//            LOGGER.info("importMidiFile() No relevant Midi notes found in file " + midiFile.getAbsolutePath());
//
//        } else if (impactedRvs.isEmpty())
//        {
//            String msg = ResUtil.getString(getClass(), "RP_SYS_CustomPhraseComp.NothingImported", midiFile.getName());
//            NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
//            DialogDisplayer.getDefault().notify(d);
//            LOGGER.info("importMidiFile() No new phrase found in file " + midiFile.getAbsolutePath());
//
//        } else
//        {
//            // We customized at least 1 phrase
//            List<String> strs = impactedRvs.stream()
//                    .map(rv -> rv.getName())
//                    .collect(Collectors.toList());
//            String strRvs = Joiner.on(",").join(strs);
//            String msg = ResUtil.getString(getClass(), "RP_SYS_CustomPhraseComp.CustomizedRvs", strRvs);
//            StatusDisplayer.getDefault().setStatusText(msg);
//            LOGGER.info("importMidiFile() Successfully set custom phrases for " + strRvs + " from Midi file " + midiFile.getAbsolutePath());
//        }

        return true;
    }

}

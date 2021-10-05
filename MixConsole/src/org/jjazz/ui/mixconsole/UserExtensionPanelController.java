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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;
import org.jjazz.midi.api.JJazzMidiSystem;
import org.jjazz.midimix.api.MidiMix;
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
import org.openide.awt.StatusDisplayer;
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
        Song song = panel.getSong();
        RhythmVoice rv = panel.getUserRhythmVoice();
        String name = rv.getName();
        Phrase oldPhrase = panel.getSong().getUserPhrase(name);
        int channel = panel.getMidiMix().getChannel(rv);


        File midiFile;
        try
        {
            // Build and store sequence
            midiFile = exportSequenceToMidiTempFile(new SongContext(panel.getSong(), panel.getMidiMix()));
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

            Phrase newPhrase;
            try
            {
                newPhrase = importMidiFile(midiFile, channel);
            } catch (IOException | InvalidMidiDataException ex)
            {
                NotifyDescriptor d = new NotifyDescriptor.Message(ex.getMessage(), NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(d);
                LOGGER.warning("editUserPhrase() Problem importing edited Midi file. ex=" + ex.getMessage());
                return;
            }


            // Check we do not erase a non-empty user phrase
            if (newPhrase.isEmpty() && !oldPhrase.isEmpty())
            {
                String msg = ResUtil.getString(getClass(), "UserExtensionPanelController.ConfirmEmptyPhrase");
                NotifyDescriptor d = new NotifyDescriptor.Confirmation(msg, NotifyDescriptor.YES_NO_CANCEL_OPTION);
                Object result = DialogDisplayer.getDefault().notify(d);
                if (result != NotifyDescriptor.YES_OPTION)
                {
                    return;
                }
            }


            String msg = ResUtil.getString(getClass(), "UserExtensionPanelController.NewUserPhrase", newPhrase.size());
            StatusDisplayer.getDefault().setStatusText(msg);


            // Do the change
            try
            {
                song.addUserPhrase(name, newPhrase);
            } catch (PropertyVetoException ex)
            {
                // Should never happen, we replace an existing user phrase
                Exceptions.printStackTrace(ex);
            }

        };


        // Prepare the text to be shown while editing
        String msg = ResUtil.getString(getClass(), "UserExtensionPanelController.WaitForEditorQuit", channel + 1);
        var dlg = new PleaseWaitDialog(msg, runEditorTask);
        dlg.setVisible(true);       // Blocks until runEditorTask is complete


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
     * Import phrase from the Midi file and from the specified channel notes.
     * <p>
     *
     * @param midiFile
     *
     * @return Can be an empty phrase
     */
    private Phrase importMidiFile(File midiFile, int channel) throws IOException, InvalidMidiDataException
    {
        Phrase res = new Phrase(channel);


        // Load file into a sequence
        Sequence sequence = MidiSystem.getSequence(midiFile);       // Throws IOException, InvalidMidiDataException

        // Get our phrase
        Track[] tracks = sequence.getTracks();
        List<Phrase> phrases = Phrase.getPhrases(tracks, channel);
        if (phrases.size() == 1)
        {
            res.addAll(phrases.get(channel));
        }

        LOGGER.severe("importMidiFile() channel=" + channel + " phrase.size()=" + res.size());

        return res;
    }

}

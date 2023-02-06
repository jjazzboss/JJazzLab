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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.VetoableChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.midimix.api.UserRhythmVoice;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.phrase.api.Phrases;
import org.jjazz.pianoroll.api.PianoRollEditor;
import org.jjazz.pianoroll.api.PianoRollEditorTopComponent;
import org.jjazz.pianoroll.spi.PianoRollEditorSettings;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythmmusicgeneration.api.SongSequenceBuilder;
import org.jjazz.song.api.Song;
import org.jjazz.songcontext.api.SongContext;
import org.jjazz.ui.mixconsole.actions.AddUserTrack;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
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

    /**
     * Set the associated UserExtensionPanel.
     * <p>
     *
     * @param panel
     */
    public void setUserExtensionPanel(UserExtensionPanel panel)
    {
        this.panel = panel;
    }

    public void userChannelNameEdited(String oldName, String newName)
    {
        String undoText = "Rename user track";
        JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(getSong());

        um.startCEdit(undoText);

        getSong().renameUserPhrase(oldName, newName);

        um.endCEdit(undoText);
    }


    /**
     * Create (or reuse existing) a PianoRollEditorTopComponent, manage the synchronization between editor and song's model.
     */
    public void editUserPhrase()
    {

        LOGGER.log(Level.FINE, "editUserPhrase() userRhythmVoice={0}", new Object[]
        {
            getUserRhythmVoice()
        });


        if (getSong().getSize() == 0)
        {
            return;
        }


        // Create editor TopComponent and open it if required
        var preTc = PianoRollEditorTopComponent.get(getSong());
        if (preTc == null)
        {
            preTc = new PianoRollEditorTopComponent(getSong(), PianoRollEditorSettings.getDefault());
            preTc.openNextToSongEditor();
        }


        // Update model of the editor
        DrumKit drumKit = panel.getMidiMix().getInstrumentMixFromKey(getUserRhythmVoice()).getInstrument().getDrumKit();
        DrumKit.KeyMap keyMap = drumKit == null ? null : drumKit.getKeyMap();
        var p = getUserPhrase();
        preTc.setModel(p, keyMap);
        preTc.setTitle(buildTitle());


        // Prepare listeners to:
        // - Stop listening when editor is destroyed or its model is changed  
        // - Update title if phrase name is changed
        // - Remove PianoRollEditor if user phrase is removed
        var editor = preTc.getEditor();
        var preTc2 = preTc;
        VetoableChangeListener vcl = new VetoableChangeListener()
        {
            @Override
            public void vetoableChange(PropertyChangeEvent evt)
            {
                if (evt.getSource() == getSong())
                {
                    if (evt.getPropertyName().equals(Song.PROP_VETOABLE_USER_PHRASE))
                    {
                        // Close the editor if phrase is removed
                        String removedPhraseName = (String) evt.getOldValue();
                        if (getUserPhraseName().equals(removedPhraseName))
                        {
                            preTc2.close();
                        }
                    }
                }
            }
        };
        PropertyChangeListener pcl = new PropertyChangeListener()
        {
            @Override
            public void propertyChange(PropertyChangeEvent evt)
            {
                // LOGGER.severe("addUserPhrase.propertyChange() e=" + Utilities.toDebugString(evt));
                if (evt.getSource() == editor)
                {
                    switch (evt.getPropertyName())
                    {
                        case PianoRollEditor.PROP_MODEL, PianoRollEditor.PROP_EDITOR_ALIVE ->
                        {
                            editor.removePropertyChangeListener(this);
                            panel.removePropertyChangeListener(this);
                            getSong().removeVetoableChangeListener(vcl);

                        }
                    }
                } else if (evt.getSource() == panel)
                {
                    if (evt.getPropertyName().equals(UserExtensionPanel.PROP_RHYTHM_VOICE))
                    {
                        preTc2.setTitle(buildTitle());
                    }
                }
            }
        };


        editor.addPropertyChangeListener(pcl);
        panel.addPropertyChangeListener(pcl);
        getSong().addVetoableChangeListener(vcl);


        preTc.requestActive();

    }

    /**
     * Called by panel cleanup().
     */
    public void cleanup()
    {
        var preTc = PianoRollEditorTopComponent.get(getSong());
        if (preTc != null)
        {
            preTc.close();
        }
    }

    public void closePanel()
    {
        String undoText = "Remove user track";

        JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(getSong());
        um.startCEdit(undoText);

        getSong().removeUserPhrase(getUserRhythmVoice().getName());

        um.endCEdit(undoText);
    }

    public boolean midiFileDraggedIn(File midiFile)
    {
        return addUserPhrase(midiFile);
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
        SongSequenceBuilder.SongSequence songSequence = new SongSequenceBuilder(sgContext).buildExportableSequence(true, true); // throws MusicGenerationException


        // Write the midi file     
        MidiSystem.write(songSequence.sequence, 1, midiFile);   // throws IOException

        return midiFile;
    }

    private boolean addUserPhrase(File midiFile)
    {
        String name = getUserRhythmVoice().getName();
        Phrase oldPhrase = panel.getSong().getUserPhrase(name);
        int channel = panel.getMidiMix().getChannel(getUserRhythmVoice());


        Phrase newPhrase;
        try
        {
            newPhrase = importPhrase(midiFile, channel);
        } catch (IOException | InvalidMidiDataException ex)
        {
            NotifyDescriptor d = new NotifyDescriptor.Message(ex.getMessage(), NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            LOGGER.warning("editUserPhrase() Problem importing edited Midi file. ex=" + ex.getMessage());
            return false;
        } catch (Exception e)    // Capture other programming exceptions, otherwise thread is just blocked
        {
            Exceptions.printStackTrace(e);
            return false;
        }


        // Check we do not erase a non-empty user phrase
        if (newPhrase.isEmpty() && !oldPhrase.isEmpty())
        {
            String msg = ResUtil.getString(getClass(), "UserExtensionPanelController.ConfirmEmptyPhrase");
            NotifyDescriptor d = new NotifyDescriptor.Confirmation(msg, NotifyDescriptor.YES_NO_CANCEL_OPTION);
            Object result = DialogDisplayer.getDefault().notify(d);
            if (result != NotifyDescriptor.YES_OPTION)
            {
                return false;
            }
        }


        String msg = ResUtil.getString(getClass(), "UserExtensionPanelController.NewUserPhrase", newPhrase.size());
        StatusDisplayer.getDefault().setStatusText(msg);


        return AddUserTrack.setUserPhraseAction(getSong(), name, newPhrase);
    }

    /**
     * Import phrase from the Midi file and from the specified channel notes.
     * <p>
     *
     * @param midiFile
     *
     * @return Can be an empty phrase
     */
    private Phrase importPhrase(File midiFile, int channel) throws IOException, InvalidMidiDataException
    {
        Phrase res = new Phrase(channel);


        // Load file into a sequence
        Sequence sequence = MidiSystem.getSequence(midiFile);       // Throws IOException, InvalidMidiDataException
        if (sequence.getDivisionType() != Sequence.PPQ)
        {
            throw new InvalidMidiDataException("Midi file does not use PPQ division: midifile=" + midiFile.getAbsolutePath());
        }

        // Get our phrase
        Track[] tracks = sequence.getTracks();
        List<Phrase> phrases = Phrases.getPhrases(sequence.getResolution(), tracks, channel);
        if (phrases.size() == 1)
        {
            res.addAll(phrases.get(0));
        }

        LOGGER.log(Level.INFO, "importPhrase() channel={0} phrase.size()={1}", new Object[]
        {
            channel, res.size()
        });

        return res;
    }

    private String getUserPhraseName()
    {
        return getUserRhythmVoice().getName();
    }

    private Phrase getUserPhrase()
    {
        return getSong().getUserPhrase(getUserPhraseName());
    }

    private Song getSong()
    {
        return panel.getSong();
    }

    private UserRhythmVoice getUserRhythmVoice()
    {
        return panel.getUserRhythmVoice();
    }


    private String buildTitle()
    {
        int channel = panel.getMidiMix().getChannel(getUserRhythmVoice());
        return ResUtil.getString(getClass(), "UserTrackTitle", getUserPhraseName(), channel + 1);
    }


    // ============================================================================
    // Inner classes
    // ============================================================================
}

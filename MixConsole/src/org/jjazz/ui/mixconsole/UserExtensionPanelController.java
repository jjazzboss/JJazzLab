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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;
import javax.swing.SwingUtilities;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.midi.api.JJazzMidiSystem;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.phrase.api.Phrases;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.pianoroll.api.PianoRollEditor;
import org.jjazz.pianoroll.api.PianoRollEditorTopComponent;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythmmusicgeneration.api.SongSequenceBuilder;
import org.jjazz.song.api.Song;
import org.jjazz.songcontext.api.SongContext;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.ui.mixconsole.actions.AddUserTrack;
import org.jjazz.ui.utilities.api.PleaseWaitDialog;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.util.api.ResUtil;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.StatusDisplayer;
import org.openide.util.Exceptions;
import org.openide.windows.Mode;
import org.openide.windows.WindowManager;

/**
 * The controller for a UserExtensionPanel.
 */
public class UserExtensionPanelController
{

    private UserExtensionPanel panel;
    private PianoRollEditorTopComponent pianoRollEditorTc;
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
            song.setUserPhrase(newName, p);
        } catch (PropertyVetoException ex)
        {
            // Should never happen since we replace an existing phrase
            Exceptions.printStackTrace(ex);
        }
    }

    public void OLDeditUserPhrase()
    {
        RhythmVoice rv = panel.getUserRhythmVoice();
        int channel = panel.getMidiMix().getChannel(rv);


        LOGGER.log(Level.INFO, "editUserPhrase() rv={0} channel={1}", new Object[]
        {
            rv, channel
        });


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
            } catch (Exception e)    // Capture other programming exceptions, otherwise thread is just blocked
            {
                Exceptions.printStackTrace(e);
                return;
            } finally
            {
                LOGGER.info("editUserPhrase() resuming from external Midi editing");
            }

            addUserPhrase(midiFile);

        };


        // Prepare the text to be shown while editing
        String msg = ResUtil.getString(getClass(), "UserExtensionPanelController.WaitForEditorQuit", channel + 1);
        var dlg = new PleaseWaitDialog(msg, runEditorTask);
        dlg.setVisible(true);       // Blocks until runEditorTask is complete
    }

    public void editUserPhrase()
    {
        if (pianoRollEditorTc != null)
        {
            pianoRollEditorTc.requestAttention(true);
            pianoRollEditorTc.requestActive();
            return;
        }

        var rv = panel.getUserRhythmVoice();
        int channel = panel.getMidiMix().getChannel(rv);

        LOGGER.log(Level.FINE, "editUserPhrase() rv={0} channel={1}", new Object[]
        {
            rv, channel
        });


        // Prepare data for the editor               
        Song song = panel.getSong();
        SongStructure ss = song.getSongStructure();


        var songBeatRange = ss.getBeatRange(null);
        DrumKit drumKit = panel.getMidiMix().getInstrumentMixFromKey(rv).getInstrument().getDrumKit();
        DrumKit.KeyMap keyMap = drumKit == null ? null : drumKit.getKeyMap();
        Phrase p = getUserPhrase();
        var sp = new SizedPhrase(p.getChannel(), songBeatRange, ss.getSongPart(0).getRhythm().getTimeSignature(), p.isDrums());
        sp.add(p);


        // Create the editor
        String tabName = song.getName() + "-" + getUserPhraseName();
        String title = getUserPhraseName() + "(" + (channel + 1) + ")";
        pianoRollEditorTc = new PianoRollEditorTopComponent(tabName, title, sp, keyMap, 0);


        // Listen to its user actions
        pianoRollEditorTc.getEditor().getUndoManager().addUserEditListener((um, actionName) -> userEditReceived(um, actionName));


        // Show it
        Mode mode = WindowManager.getDefault().findMode(PianoRollEditorTopComponent.MODE);
        mode.dockInto(pianoRollEditorTc);
        SwingUtilities.invokeLater(() ->
        {
            // https://dzone.com/articles/secrets-netbeans-window-system
            // https://web.archive.org/web/20170314072532/https://blogs.oracle.com/geertjan/entry/creating_a_new_mode_in
            pianoRollEditorTc.open();
            pianoRollEditorTc.requestActive();
            // WindowManager.getDefault().setTopComponentFloating(pianoRollEditorTc, true);            
        });
        
    }


    public void closePanel()
    {
        Song song = panel.getSong();
        String undoText = "Remove user track";

        JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(song);
        um.startCEdit(undoText);

        song.removeUserPhrase(panel.getUserRhythmVoice().getName());

        um.endCEdit(undoText);
    }

    protected boolean midiFileDraggedIn(File midiFile)
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
        Song song = panel.getSong();
        RhythmVoice rv = panel.getUserRhythmVoice();
        String name = rv.getName();
        Phrase oldPhrase = panel.getSong().getUserPhrase(name);
        int channel = panel.getMidiMix().getChannel(rv);


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


        return AddUserTrack.setUserPhraseAction(song, name, newPhrase);
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

    private void userEditReceived(JJazzUndoManager um, String actionName)
    {
        PianoRollEditor editor = pianoRollEditorTc.getEditor();
        assert um == editor.getUndoManager();

        // Ignore actionName, just update the song
        SizedPhrase editorSp = editor.getModel();
        Phrase p = new Phrase(editorSp.getChannel());
        p.add(editorSp);
        try
        {
            panel.getSong().setUserPhrase(getUserPhraseName(), p);
        } catch (PropertyVetoException ex)
        {
            // Should never be there, we're just replacing an existing phrase
            Exceptions.printStackTrace(ex);
        }
    }

    private String getUserPhraseName()
    {
        return panel.getUserRhythmVoice().getName();
    }

    private Phrase getUserPhrase()
    {
        return panel.getSong().getUserPhrase(getUserPhraseName());
    }


    // ============================================================================
    // Inner classes
    // ============================================================================
}

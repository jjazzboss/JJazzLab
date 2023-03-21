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

import com.google.common.base.Preconditions;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MidiUnavailableException;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.api.MidiMixManager;
import org.jjazz.midimix.api.UserRhythmVoice;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.pianoroll.api.PianoRollEditor;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.song.api.Song;
import org.jjazz.songeditormanager.api.SongEditorManager;
import org.jjazz.ui.mixconsole.MixChannelPanelControllerImpl;
import org.jjazz.ui.mixconsole.api.MixConsole;
import org.jjazz.ui.mixconsole.api.MixConsoleTopComponent;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.util.api.ResUtil;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionRegistration;
import org.openide.util.Exceptions;


@ActionID(category = "MixConsole", id = "org.jjazz.ui.mixconsole.actions.addusertrack")
@ActionRegistration(displayName = "not_used", lazy = false)
public class AddUserTrack extends AbstractAction
{

    private static final String undoText = ResUtil.getString(AddUserTrack.class, "CTL_AddUserTrack");
    private static final Logger LOGGER = Logger.getLogger(AddUserTrack.class.getSimpleName());

    public AddUserTrack()
    {
        putValue("hideActionText", true);
        putValue(SHORT_DESCRIPTION, ResUtil.getString(getClass(), "CTL_AddUserTrackTooltip"));
        putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("/org/jjazz/ui/mixconsole/resources/AddUser-16x16.png")));
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        MixConsole mixConsole = MixConsoleTopComponent.getInstance().getEditor();
        Song song = mixConsole.getSong();
        if (song == null)
        {
            return;
        }


        // Find a name not already used
        String basename = "User";
        int index = 1;
        var usedNames = song.getUserPhraseNames();
        while (usedNames.contains(basename + index))
        {
            index++;
        }
        String name = basename + index;


        // Is it a drums or a melodic user phrase ?
        Phrase p;
        String title = ResUtil.getString(getClass(), "UserTrackTypeDialogTitle");
        String question = ResUtil.getString(getClass(), "UserTrackTypeQuestion");
        String drums = ResUtil.getString(getClass(), "Drums");
        String melodic = ResUtil.getString(getClass(), "Melodic");
        NotifyDescriptor d = new NotifyDescriptor.Confirmation(question, title, NotifyDescriptor.YES_NO_CANCEL_OPTION);
        d.setOptions(new String[]
        {
            melodic, drums
        });
        var res = DialogDisplayer.getDefault().notify(d);
        if (res.equals(-1))
        {
            return;
        }
        p = new Phrase(0, res == drums);


        // Save existing user rhythmvoices to detect which one will be the new one
        MidiMix midiMix;
        try
        {
            midiMix = MidiMixManager.getInstance().findMix(song);
        } catch (MidiUnavailableException ex)
        {
            // Should never happen
            Exceptions.printStackTrace(ex);
            return;
        }

        // Perform the change
        if (setUserPhraseAction(song, name, p))     // Returns true if success
        {
            // Set the PianoRollEditor
            var userRv = midiMix.getUserRhythmVoice(name);
            assert userRv != null : " midiMix=" + midiMix + " name=" + name;
            editUserPhrase(song, midiMix, userRv);
        }

    }


    /**
     * Open (or show) the song's PianoRollEditor to edit the track associated to userRhythmVoice.
     *
     * @param song
     * @param midiMix
     * @param userRhythmVoice
     */
    static public void editUserPhrase(Song song, MidiMix midiMix, UserRhythmVoice userRhythmVoice)
    {
        Preconditions.checkNotNull(song);
        Preconditions.checkNotNull(midiMix);
        Preconditions.checkNotNull(userRhythmVoice);

        LOGGER.log(Level.FINE, "editUserPhrase() song={0} userRhythmVoice={1}", new Object[]
        {
            song, userRhythmVoice
        });

        if (song.getSize() == 0)
        {
            return;
        }


        String initialPhraseName = userRhythmVoice.getName();
        int initialChannel = midiMix.getChannel(userRhythmVoice);
        assert initialChannel != -1 : "midiMix=" + midiMix + " userRhythmVoice=" + userRhythmVoice;


        // Create editor TopComponent and open it if required
        DrumKit drumKit = midiMix.getInstrumentMix(userRhythmVoice).getInstrument().getDrumKit();
        DrumKit.KeyMap keyMap = drumKit == null ? null : drumKit.getKeyMap();
        var userPhrase = song.getUserPhrase(initialPhraseName);
        var preTc = SongEditorManager.getInstance().showPianoRollEditor(song);
        preTc.setModelForUserPhrase(userPhrase, initialChannel, keyMap);
        preTc.setTitle(buildPianoRollEditorTitle(initialPhraseName, initialChannel));


        // Prepare listeners to:
        // - Stop listening when editor is destroyed or its model is changed  
        // - Update title if phrase name or channel is changed
        // - Remove PianoRollEditor if user phrase is removed
        var editor = preTc.getEditor();
        VetoableChangeListener vcl = evt -> 
        {
            if (evt.getSource() == song)
            {
                if (evt.getPropertyName().equals(Song.PROP_VETOABLE_USER_PHRASE))
                {
                    // Close the editor if our phrase is removed
                    if (evt.getOldValue() instanceof String && evt.getNewValue() instanceof Phrase p && p == userPhrase)
                    {
                        preTc.close();
                    }
                }
            }
        };
        PropertyChangeListener pcl = new PropertyChangeListener()
        {
            @Override
            public void propertyChange(PropertyChangeEvent evt)
            {
                // LOGGER.severe("editUserPhrase.propertyChange() e=" + Utilities.toDebugString(evt));
                if (evt.getSource() == editor)
                {
                    switch (evt.getPropertyName())
                    {
                        case PianoRollEditor.PROP_MODEL_PHRASE, PianoRollEditor.PROP_EDITOR_ALIVE ->
                        {
                            editor.removePropertyChangeListener(this);
                            midiMix.removePropertyChangeListener(this);
                            song.removeVetoableChangeListener(vcl);
                        }
                    }
                } else if (evt.getSource() == midiMix)
                {
                    if (evt.getPropertyName().equals(MidiMix.PROP_RHYTHM_VOICE))
                    {
                        // Used for UserRhythmVoice name change
                        var newRv = (RhythmVoice) evt.getNewValue();
                        var newRvName = newRv.getName();
                        if (newRv instanceof UserRhythmVoice && song.getUserPhrase(newRvName) == userPhrase)
                        {
                            int channel = midiMix.getChannel(newRv);                // Normally unchanged
                            preTc.setTitle(buildPianoRollEditorTitle(newRvName, channel));
                        }

                    } else if (evt.getPropertyName().equals(MidiMix.PROP_RHYTHM_VOICE_CHANNEL))
                    {
                        // Used to change channel of a RhythmVoice
                        int newChannel = (int) evt.getNewValue();
                        var rv = midiMix.getRhythmVoice(newChannel);
                        var rvName = rv.getName();
                        if (rv instanceof UserRhythmVoice && song.getUserPhrase(rvName) == userPhrase)
                        {
                            preTc.setModelForUserPhrase(userPhrase, newChannel, keyMap);
                            preTc.setTitle(buildPianoRollEditorTitle(rvName, newChannel));
                        }
                    }
                }
            }
        };


        editor.addPropertyChangeListener(pcl);
        midiMix.addPropertyChangeListener(pcl);
        song.addVetoableChangeListener(vcl);


        preTc.requestActive();
    }

    // ======================================================================================================
    // Private methods
    // ======================================================================================================

    /**
     * The undoable action.
     *
     * @param song
     * @param name
     * @param p
     * @return True if operation was successful
     * @throws PropertyVetoException
     */
    private boolean setUserPhraseAction(Song song, String name, Phrase p)
    {
        JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(song);
        um.startCEdit(undoText);

        try
        {
            song.setUserPhrase(name, p);
        } catch (PropertyVetoException ex)
        {
            String msg = "Impossible to add or update user phrase " + name + ".\n" + ex.getLocalizedMessage();
            um.handleUnsupportedEditException(undoText, msg);
            return false;
        } catch (Exception ex)    // Capture other programming exceptions, because method can be called from within a thread
        {
            String msg = "Unexpected exception! Impossible to add or update user phrase " + name + ".\n" + ex.getMessage();
            um.handleUnsupportedEditException(undoText, msg);
            LOGGER.log(Level.SEVERE, "setUserPhraseAction() {0}", msg);
            Exceptions.printStackTrace(ex);
            return false;
        }

        um.endCEdit(undoText);

        return true;
    }


    static private String buildPianoRollEditorTitle(String phraseName, int channel)
    {
        return ResUtil.getString(MixChannelPanelControllerImpl.class, "UserTrackTitle", phraseName, channel + 1);
    }


}

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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.midimix.api.UserRhythmVoice;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.pianoroll.api.PianoRollEditor;
import org.jjazz.pianoroll.api.PianoRollEditorTopComponent;
import org.jjazz.pianoroll.spi.PianoRollEditorSettings;
import org.jjazz.song.api.Song;
import org.jjazz.songeditormanager.api.SongEditorManager;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.util.api.ResUtil;

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
        var preTc = SongEditorManager.getInstance().showPianoRollEditor(getSong());


        // Update model of the editor
        DrumKit drumKit = panel.getMidiMix().getInstrumentMixFromKey(getUserRhythmVoice()).getInstrument().getDrumKit();
        DrumKit.KeyMap keyMap = drumKit == null ? null : drumKit.getKeyMap();
        var p = getUserPhrase();
        preTc.setModel(p, getChannel(), keyMap);
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
                        String name = (String) evt.getOldValue();   // name is null if a user phrase has been added
                        if (getUserPhraseName().equals(name))
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


    public void closePanel()
    {
        String undoText = "Remove user track";

        JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(getSong());
        um.startCEdit(undoText);

        getSong().removeUserPhrase(getUserRhythmVoice().getName());

        um.endCEdit(undoText);
    }

    // ============================================================================
    // Private methods
    // ============================================================================

    private String getUserPhraseName()
    {
        return getUserRhythmVoice().getName();
    }

    private Phrase getUserPhrase()
    {
        return getSong().getUserPhrase(getUserPhraseName());
    }

    private int getChannel()
    {
        return panel.getMidiMix().getChannel(getUserRhythmVoice());
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
        return ResUtil.getString(getClass(), "UserTrackTitle", getUserPhraseName(), getChannel() + 1);
    }


    // ============================================================================
    // Inner classes
    // ============================================================================
}

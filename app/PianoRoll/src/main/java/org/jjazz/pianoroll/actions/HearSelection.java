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
package org.jjazz.pianoroll.actions;

import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import org.jjazz.instrumentcomponents.keyboard.api.KeyboardMouseHelper;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.pianoroll.api.NoteView;
import org.jjazz.pianoroll.api.PianoRollEditor;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.testplayerservice.spi.TestPlayer;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.utilities.api.ToggleAction;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;

/**
 * Action to toggle the play of the last selected note.
 * <p>
 * Also enable hearing the piano keyboard note when clicked.
 */
public class HearSelection extends ToggleAction implements PropertyChangeListener
{

    public static final String ACTION_ID = "HearSelection";
    public static final String KEYBOARD_SHORTCUT = "H";
    private final PianoRollEditor editor;
    private KeyboardMouseHelper keyboardMouseHelper;
    private MyKbdMouseListener myListener;
    private static final Logger LOGGER = Logger.getLogger(HearSelection.class.getSimpleName());

    public HearSelection(PianoRollEditor editor)
    {
        super(false);

        this.editor = editor;


        // UI settings for the FlatToggleButton
        putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("resources/HearNoteOFF.png")));
        setSelectedIcon(new ImageIcon(getClass().getResource("resources/HearNoteON.png")));
        // putValue("JJazzDisabledIcon", new ImageIcon(getClass().getResource("/org/jjazz/musiccontrolactions/resources/PlaybackPointDisabled-24x24.png")));                                   
        putValue(Action.SHORT_DESCRIPTION, ResUtil.getString(getClass(), "HearNoteTooltip") + " (" + KEYBOARD_SHORTCUT + ")");
        putValue("hideActionText", true);


        this.editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KEYBOARD_SHORTCUT), HearSelection.ACTION_ID);
        this.editor.getActionMap().put(HearSelection.ACTION_ID, this);

        keyboardMouseHelper = new KeyboardMouseHelper(editor.getKeyboard());
        myListener = new MyKbdMouseListener();

        // Disable when music is being played
        var mc = MusicController.getInstance();
        mc.addPropertyChangeListener(this);
        setEnabled(!mc.isPlaying());

        selectedStateChanged(false);        // Disabled by default
    }


    @Override
    public void selectedStateChanged(boolean b)
    {
        if (b)
        {
            editor.addPropertyChangeListener(this);
            keyboardMouseHelper.addListener(myListener);
        } else
        {
            editor.removePropertyChangeListener(this);
            keyboardMouseHelper.removeListener(myListener);
        }
    }

    // ====================================================================================
    // PropertyChangeListener interface
    // ====================================================================================
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        var mc = MusicController.getInstance();
        if (evt.getSource() == editor)
        {
            if (evt.getPropertyName().equals(PianoRollEditor.PROP_SELECTED_NOTE_VIEWS))
            {
                boolean b = (boolean) evt.getNewValue();
                if (b)
                {
                    List<NoteView> nvs = (List<NoteView>) evt.getOldValue();
                    hearNotes(NoteView.getNotes(nvs));
                }
            } else if (evt.getPropertyName().equals(PianoRollEditor.PROP_EDITOR_ALIVE))
            {
                editor.removePropertyChangeListener(this);
            }
        } else if (evt.getSource() == mc)
        {
            if (evt.getPropertyName().equals(MusicController.PROP_STATE))
            {
                setEnabled(mc.getState() != MusicController.State.PLAYING);
            }
        }
    }


// ====================================================================================
// Private methods
// ====================================================================================
    private void hearNotes(List<NoteEvent> noteEvents)
    {
        if (noteEvents.isEmpty() || !isEnabled())
        {
            return;
        }

        float firstNotePos = noteEvents.get(0).getPositionInBeats();
        Phrase p = new Phrase(editor.getChannel());
        for (var ne : noteEvents)
        {
            float pos = ne.getPositionInBeats() - firstNotePos;
            var newNe = ne.setPosition(pos, true);
            p.add(newNe);
        }

        playPhrase(p);
    }

    private void hearSingleNote(int pitch)
    {
        Phrase p = new Phrase(editor.getChannel());
        p.add(new NoteEvent(pitch, 2f, 75, 0f));
        playPhrase(p);
    }

    private void playPhrase(Phrase p)
    {
        try
        {
            TestPlayer.getDefault().playTestNotes(p, null);     // playTestNotes will stop MusicController first
        } catch (MusicGenerationException ex)
        {
            LOGGER.log(Level.WARNING, "hearNotes() {0}", ex.getMessage());
            NotifyDescriptor nd = new NotifyDescriptor.Message(ex.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(nd);
        }
    }

    // ====================================================================================
    // Inner classes
    // ====================================================================================

    public class MyKbdMouseListener extends KeyboardMouseHelper.ListenerAdapter
    {

        @Override
        public void mousePressed(int pitch, MouseEvent me)
        {
            hearSingleNote(pitch);
        }
    }
}

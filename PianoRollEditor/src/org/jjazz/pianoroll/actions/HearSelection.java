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
package org.jjazz.pianoroll.actions;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.pianoroll.api.NoteView;
import org.jjazz.pianoroll.api.PianoRollEditor;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.testplayerservice.spi.TestPlayer;
import org.jjazz.ui.utilities.api.ToggleAction;
import org.jjazz.util.api.ResUtil;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;

/**
 * Action to toggle the play of the last selected note.
 */
public class HearSelection extends ToggleAction implements PropertyChangeListener
{

    public static final String ACTION_ID = "HearSelection";
    public static final String KEYBOARD_SHORTCUT = "H";
    private final PianoRollEditor editor;
    private static final Logger LOGGER = Logger.getLogger(HearSelection.class.getSimpleName());

    public HearSelection(PianoRollEditor editor)
    {
        super(false);

        this.editor = editor;


        // UI settings for the FlatToggleButton
        putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("resources/HearNoteOFF.png")));
        setSelectedIcon(new ImageIcon(getClass().getResource("resources/HearNoteON.png")));
        // putValue("JJazzDisabledIcon", new ImageIcon(getClass().getResource("/org/jjazz/ui/musiccontrolactions/resources/PlaybackPointDisabled-24x24.png")));                                   
        putValue(Action.SHORT_DESCRIPTION, ResUtil.getString(getClass(), "HearNoteTooltip") + " (" + KEYBOARD_SHORTCUT + ")");
        putValue("hideActionText", true);


        this.editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KEYBOARD_SHORTCUT),
                HearSelection.ACTION_ID);
        this.editor.getActionMap().put(HearSelection.ACTION_ID, this);

    }


    @Override
    public void selectedStateChanged(boolean b)
    {
        if (b)
        {
            editor.addPropertyChangeListener(PianoRollEditor.PROP_SELECTED_NOTE_VIEWS, this);
        } else
        {
            editor.removePropertyChangeListener(PianoRollEditor.PROP_SELECTED_NOTE_VIEWS, this);
        }
    }

    // ====================================================================================
    // PropertyChangeListener interface
    // ====================================================================================
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        // This is PianoRollEditor.PROP_SELECTED_NOTE_VIEWS
        boolean b = (boolean) evt.getNewValue();
        if (b)
        {
            List<NoteView> nvs = (List<NoteView>) evt.getOldValue();
            hearNotes(NoteView.getNotes(nvs));
        }
    }


    // ====================================================================================
    // Private methods
    // ====================================================================================
    private void hearNotes(List<NoteEvent> noteEvents)
    {
        if (noteEvents.isEmpty())
        {
            return;
        }

        float firstNotePos = noteEvents.get(0).getPositionInBeats();
        Phrase p = new Phrase(editor.getChannel());
        for (var ne : noteEvents)
        {
            float pos = ne.getPositionInBeats() - firstNotePos;
            var newNe = ne.getCopyPos(pos);
            p.add(newNe);
        }

        try
        {
            TestPlayer.getDefault().playTestNotes(p, null);
        } catch (MusicGenerationException ex)
        {
            LOGGER.log(Level.WARNING, "hearNotes() {0}", ex.getMessage());
            NotifyDescriptor nd = new NotifyDescriptor.Message(ex.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(nd);
        }

    }

}

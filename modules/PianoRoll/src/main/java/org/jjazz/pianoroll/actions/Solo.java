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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import org.jjazz.midi.api.InstrumentMix;
import org.jjazz.pianoroll.api.PianoRollEditor;
import org.jjazz.pianoroll.api.PianoRollEditorTopComponent;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.utilities.api.ToggleAction;

/**
 * Action to toggle the solo mode.
 */
public class Solo extends ToggleAction implements PropertyChangeListener
{

    public static final String ACTION_ID = "Solo";
    public static final String KEYBOARD_SHORTCUT = "S";
    private InstrumentMix insMix;
    private final PianoRollEditor editor;
    private final PianoRollEditorTopComponent topComponent;
    private static final Logger LOGGER = Logger.getLogger(Solo.class.getSimpleName());


    public Solo(PianoRollEditorTopComponent topComponent)
    {
        super(false);
        this.topComponent = topComponent;
        this.editor = topComponent.getEditor();


        // UI settings for the FlatToggleButton
        putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("resources/SoloOFF.png")));
        setSelectedIcon(new ImageIcon(getClass().getResource("resources/SoloON.png")));
        // putValue("JJazzDisabledIcon", new ImageIcon(getClass().getResource("/org/jjazz/musiccontrolactions/resources/PlaybackPointDisabled-24x24.png")));                                   
        putValue(Action.SHORT_DESCRIPTION, ResUtil.getString(getClass(), "SoloModeTooltip") + " (" + KEYBOARD_SHORTCUT + ")");
        putValue("hideActionText", true);


        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KEYBOARD_SHORTCUT), Solo.ACTION_ID);
        editor.getActionMap().put(Solo.ACTION_ID, this);


        editor.addPropertyChangeListener(this);
    }


    @Override
    public void selectedStateChanged(boolean b)
    {
        insMix.setSolo(isSelected());
    }

    // ====================================================================================
    // PropertyChangeListener interface
    // ====================================================================================

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        // LOGGER.severe("propertyChange() -- " + Utilities.toDebugString(evt));
        if (evt.getSource() == insMix)
        {
            if (evt.getPropertyName().equals(InstrumentMix.PROP_SOLO))
            {
                setSelected(insMix.isSolo());
            }
        } else if (evt.getSource() == editor)
        {
            if (evt.getPropertyName().equals(PianoRollEditor.PROP_MODEL_CHANNEL)
                    || evt.getPropertyName().equals(PianoRollEditor.PROP_MODEL_PHRASE))
            {
                editorChannelChanged();
            } else if (evt.getPropertyName().equals(PianoRollEditor.PROP_EDITOR_ALIVE))
            {
                cleanup();
            }
        }
    }


    // ====================================================================================
    // Private methods
    // ====================================================================================
    private void cleanup()
    {
        editor.removePropertyChangeListener(this);
        if (insMix != null)
        {
            insMix.removePropertyChangeListener(this);
        }
    }

    private void editorChannelChanged()
    {
        if (insMix != null)
        {
            insMix.removePropertyChangeListener(this);
        }
        insMix = topComponent.getMidiMix().getInstrumentMix(editor.getChannel());
        insMix.addPropertyChangeListener(this);
    }

}

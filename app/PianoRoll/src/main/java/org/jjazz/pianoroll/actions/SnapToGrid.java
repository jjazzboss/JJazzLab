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
import org.jjazz.pianoroll.api.PianoRollEditor;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.utilities.api.ToggleAction;

/**
 * Action to toggle the snap to the grid.
 */
public class SnapToGrid extends ToggleAction implements PropertyChangeListener
{

    public static final String ACTION_ID = "SnapToGrid";
    public static final String KEYBOARD_SHORTCUT = "G";
    private final PianoRollEditor editor;
    private static final Logger LOGGER = Logger.getLogger(SnapToGrid.class.getSimpleName());

    public SnapToGrid(PianoRollEditor editor)
    {
        super(editor.isSnapEnabled());

        this.editor = editor;

        // UI settings for the FlatToggleButton
        putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("resources/SnapOFF.png")));
        setSelectedIcon(new ImageIcon(getClass().getResource("resources/SnapON.png")));
        // putValue("JJazzDisabledIcon", new ImageIcon(getClass().getResource("/org/jjazz/musiccontrolactions/resources/PlaybackPointDisabled-24x24.png")));                                   
        putValue(Action.SHORT_DESCRIPTION, ResUtil.getString(getClass(), "SnapTooltip") + " (" + KEYBOARD_SHORTCUT + ")");
        putValue("hideActionText", true);


        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KEYBOARD_SHORTCUT), SnapToGrid.ACTION_ID);
        editor.getActionMap().put(SnapToGrid.ACTION_ID, this);


        this.editor.addPropertyChangeListener(this);

    }


    @Override
    public void selectedStateChanged(boolean b)
    {
        editor.setSnapEnabled(b);
    }

    // ====================================================================================
    // PropertyChangeListener interface
    // ====================================================================================
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getSource() == editor)
        {
            switch (evt.getPropertyName())
            {
                case PianoRollEditor.PROP_SNAP_ENABLED -> setSelected(editor.isSnapEnabled());
                case PianoRollEditor.PROP_EDITOR_ALIVE -> cleanup();
            }
        }
    }


// ====================================================================================
// Private methods
// ====================================================================================
    private void cleanup()
    {
        editor.removePropertyChangeListener(this);
    }
}

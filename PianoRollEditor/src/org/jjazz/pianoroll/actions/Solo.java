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

import java.awt.event.ActionEvent;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import org.jjazz.pianoroll.api.PianoRollEditor;
import org.jjazz.ui.utilities.api.ToggleAction;
import org.jjazz.util.api.ResUtil;

/**
 * Action to toggle the solo mode.
 */
public class Solo extends ToggleAction
{

    private final PianoRollEditor editor;
    private static final Logger LOGGER = Logger.getLogger(Solo.class.getSimpleName());

    public Solo(PianoRollEditor editor)
    {
        super(false);
        
        this.editor = editor;

        // UI settings for the FlatToggleButton
        putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("resources/SoloOFF.png")));
        setSelectedIcon(new ImageIcon(getClass().getResource("resources/SoloON.png")));
        // putValue("JJazzDisabledIcon", new ImageIcon(getClass().getResource("/org/jjazz/ui/musiccontrolactions/resources/PlaybackPointDisabled-24x24.png")));   //NOI18N                                
        putValue(Action.SHORT_DESCRIPTION, ResUtil.getString(getClass(), "SoloModeTooltip"));
        putValue("hideActionText", true);


        // Keyboard shortcut
        editor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("S"), "SoloMode");   //NOI18N
        editor.getActionMap().put("SoloMode", this);   //NOI18N

    }


    @Override
    public void actionPerformed(ActionEvent e)
    {
        setSelected(!isSelected());
    }

    @Override
    public void selectedStateChanged(boolean b)
    {
    }

 

    // ====================================================================================
    // Private methods
    // ====================================================================================

   
}

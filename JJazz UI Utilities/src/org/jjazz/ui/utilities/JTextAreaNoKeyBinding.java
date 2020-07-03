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
package org.jjazz.ui.utilities;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;

/**
 * A special JTextArea which does not forward key event to the container hierarchy via the key binding framework.
 * <p>
 * This allows to avoid the issue of a global action with a Netbeans global shortcut executed when user presses the shortcut (eg SPACE) in
 * the JtextArea.
 */
public class JTextAreaNoKeyBinding extends JTextArea
{

    public JTextAreaNoKeyBinding()
    {
        // HACK ! 
        // Key presses are not consumed by JTextField, they are also processed by the keybinding framework
        // Only way to block them is to capture all the printable keys
        // see https://docs.oracle.com/javase/tutorial/uiswing/misc/keybinding.html
        for (char c = 32; c <= 126; c++)
        {
            getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(c, 0), "doNothing");
            getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(c, InputEvent.SHIFT_DOWN_MASK), "doNothing");
        }
        getActionMap().put("doNothing", new NoAction());
    }

    private class NoAction extends AbstractAction
    {

        @Override
        public void actionPerformed(ActionEvent e)
        {
            //do nothing
        }
    }
}

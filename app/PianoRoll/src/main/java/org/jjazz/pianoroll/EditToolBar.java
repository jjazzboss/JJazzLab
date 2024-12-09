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
package org.jjazz.pianoroll;

import java.awt.FlowLayout;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.jjazz.pianoroll.api.EditTool;
import org.jjazz.pianoroll.api.PianoRollEditor;
import org.jjazz.flatcomponents.api.FlatToggleButton;

/**
 * The toolbar for EditTools.
 */
public class EditToolBar extends JPanel
{

    private final PianoRollEditor editor;
    private final Map<EditTool, FlatToggleButton> mapToolButton = new HashMap<>();
    private Runnable clickTask;

    public EditToolBar(PianoRollEditor editor)
    {
        this.editor = editor;
        
        FlowLayout layout = (FlowLayout) getLayout();
        layout.setAlignment(FlowLayout.LEADING);
        layout.setVgap(0);
        layout.setHgap(0);
        
        for (var tool : editor.getEditTools())
        {
            FlatToggleButton btn = new FlatToggleButton();
            btn.setIcon(tool.getIcon(false));
            btn.setSelectedIcon(tool.getIcon(true));
            btn.setToolTipText(tool.getName());
            btn.addActionListener(al -> buttonPressed(btn, tool));
            mapToolButton.put(tool, btn);
            add(btn);
        }

        getButton(editor.getActiveTool()).setSelected(true);

        editor.addPropertyChangeListener(PianoRollEditor.PROP_ACTIVE_TOOL,
                e -> activeToolChanged((EditTool) e.getOldValue(), (EditTool) e.getNewValue()));
    }

    /**
     * Invoke the specified task later on the EDT when one of the button is clicked.
     *
     * @param r
     */
    public void setClickListener(Runnable task)
    {
        clickTask = task;
    }

    // ====================================================================================
    // Private methods
    // ====================================================================================

    private void buttonPressed(FlatToggleButton btn, EditTool tool)
    {
        if (!btn.isSelected())
        {
            // Active tool button went off, we don't allow this
            SwingUtilities.invokeLater(() -> btn.setSelected(true));
            notifyClickListener();
            return;
        }

        editor.setActiveTool(tool);
        notifyClickListener();
    }

    private void activeToolChanged(EditTool oldTool, EditTool newTool)
    {
        getButton(oldTool).setSelected(false);
        getButton(newTool).setSelected(true);
    }

    private FlatToggleButton getButton(EditTool tool)
    {
        return mapToolButton.get(tool);
    }

    private void notifyClickListener()
    {
        if (clickTask != null)
        {
            SwingUtilities.invokeLater(clickTask);
        }
    }


}

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
package org.jjazz.cl_editorimpl;

import com.google.common.base.Preconditions;
import java.util.List;
import javax.swing.Action;
import javax.swing.Box.Filler;
import javax.swing.JToolBar;
import org.jjazz.cl_editor.api.CL_Editor;
import org.jjazz.cl_editorimpl.actions.ToggleBarAnnotations;
import org.jjazz.flatcomponents.api.FlatButton;
import org.jjazz.flatcomponents.api.FlatToggleButton;
import org.openide.util.Utilities;

/**
 * The side toolbar of the ChordLeadSheet editor.
 * <p>
 * Built from stateless unique actions found in the layer.xml at Actions/CL_EditorToolBar, plus some stateful specific actions.
 */
public class CL_EditorToolBar extends JToolBar
{

    private static final Filler FILLER_GLUE = new Filler(new java.awt.Dimension(1, 0), new java.awt.Dimension(1, 0), new java.awt.Dimension(1, 32767));
    private boolean alignRight = false;
    private final CL_Editor editor;

    public CL_EditorToolBar(CL_Editor editor)
    {
        Preconditions.checkNotNull(editor);
        this.editor = editor;


        // Add the stateless unique actions like EditorSettings (the same action instance is used for all toolbars)
        List<? extends Action> actions = Utilities.actionsForPath("Actions/CL_EditorToolBar");
        for (Action action : actions)
        {
            if (action == null)
            {
                Filler hardFiller = new javax.swing.Box.Filler(new java.awt.Dimension(1, 5), new java.awt.Dimension(1, 5), new java.awt.Dimension(32767, 5));
                add(hardFiller);
            } else
            {
                FlatButton btn = new FlatButton(action);
                add(btn);
            }
        }

        Filler hardFiller = new javax.swing.Box.Filler(new java.awt.Dimension(1, 5), new java.awt.Dimension(1, 5), new java.awt.Dimension(32767, 5));
        add(hardFiller);

        
        // Add song-specific actions
        
        // Show/hide bar annotations        
        FlatToggleButton ftb = new FlatToggleButton(ToggleBarAnnotations.getInstance(editor));
        add(ftb);


        add(FILLER_GLUE);    // At the end
    }

    public boolean isAlignRight()
    {
        return alignRight;
    }

    public void setAlignRight(boolean alignRight)
    {
        if (this.alignRight == alignRight)
        {
            return;
        }
        this.alignRight = alignRight;
        remove(FILLER_GLUE);
        if (alignRight)
        {
            add(FILLER_GLUE, 0);     // Add first
        } else
        {
            add(FILLER_GLUE);    // At the end
        }
    }


}

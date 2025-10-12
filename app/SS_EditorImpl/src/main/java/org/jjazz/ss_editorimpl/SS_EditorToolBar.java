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
package org.jjazz.ss_editorimpl;

import java.util.List;
import javax.swing.Action;
import javax.swing.Box.Filler;
import javax.swing.JToolBar;
import org.jjazz.flatcomponents.api.FlatButton;
import org.jjazz.flatcomponents.api.FlatToggleButton;
import org.jjazz.ss_editorimpl.actions.ToggleCompactView;
import org.jjazz.ss_editor.api.SS_Editor;
import org.openide.util.Utilities;

/**
 * The side toolbar of the SongStructure editor.
 * <p>
 * Built from stateless unique actions found in the layer.xml at Actions/SS_EditorToolBar, plus some stateful specific actions.
 */
public class SS_EditorToolBar extends JToolBar
{

    private static final Filler FILLER_GLUE = new Filler(new java.awt.Dimension(1, 0), new java.awt.Dimension(1, 0), new java.awt.Dimension(1, 32767));
    private boolean alignRight = false;
    private SS_Editor editor;

    public SS_EditorToolBar(SS_Editor editor)
    {
        if (editor == null)
        {
            throw new NullPointerException("editor");
        }
        this.editor = editor;


        // Add the stateless unique actions like ZoomToFit or (the same action instance is used for all toolbars)
        List<? extends Action> actions = Utilities.actionsForPath("Actions/SS_EditorToolBar");
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


        // Add song-specific actions
        FlatToggleButton ftb = new FlatToggleButton(ToggleCompactView.getInstance(editor));             // Compact/Full view switching button
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

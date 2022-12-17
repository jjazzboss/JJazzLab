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
package org.jjazz.pianoroll.edittools;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.pianoroll.api.EditTool;
import org.jjazz.pianoroll.api.NotesSelection;
import org.jjazz.pianoroll.api.PianoRollEditor;
import org.jjazz.util.api.ResUtil;
import org.netbeans.api.annotations.common.StaticResource;

/**
 * Select, move, resize notes.
 */
public class SelectionTool implements EditTool
{
    
    @StaticResource(relative = true)
    private static final String ICON_PATH_OFF = "resources/SelectionToolOFF.png";
    @StaticResource(relative = true)
    private static final String ICON_PATH_ON = "resources/SelectionToolON.png";
    private final PianoRollEditor editor;
    /**
     * If not null means dragging has started.
     */
    private Point startDraggingPoint;    
    
    public SelectionTool(PianoRollEditor editor)
    {
        this.editor = editor;
    }
    
    @Override
    public Icon getIcon(boolean b)
    {
        return b ? new ImageIcon(SelectionTool.class.getResource(ICON_PATH_ON)) : new ImageIcon(SelectionTool.class.getResource(ICON_PATH_OFF));
    }
    
    @Override
    public String getName()
    {
        return ResUtil.getString(getClass(), "SelectionName");
    }
    
    @Override
    public void editorWheelMoved(MouseWheelEvent e)
    {
        //
    }
    
    @Override
    public void editorClicked(MouseEvent e)
    {
        unselectAll();
    }
    
    @Override
    public void noteClicked(MouseEvent e, NoteEvent ne)
    {
        boolean shift_or_ctrl = (e.getModifiersEx() & (InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK)) != 0;
        boolean b = editor.isSelectedNote(ne);
        if (!shift_or_ctrl)
        {
            unselectAll();
        }
        editor.setSelectedNote(ne, !b);
    }
    
    @Override
    public void noteWheelMoved(MouseWheelEvent e, NoteEvent ne)
    {
        //
    }
    
    @Override
    public void noteDragged(MouseEvent e, NoteEvent ne)
    {
        
    }
    
    @Override
    public void noteReleased(MouseEvent e, NoteEvent ne)
    {
        
    }
    
    @Override
    public void editorDragged(MouseEvent e)
    {
        if (startDraggingPoint == null)
        {
            startDraggingPoint = e.getPoint();
            unselectAll();
        } else
        {
            Rectangle r = new Rectangle(startDraggingPoint);
            r.add(e.getPoint());
            editor.showSelectionRectangle(r);
        }
    }
    
    @Override
    public void editorReleased(MouseEvent e)
    {
        if (startDraggingPoint != null)
        {
            Rectangle r = new Rectangle(startDraggingPoint);
            r.add(e.getPoint());
            editor.showSelectionRectangle(null);
            startDraggingPoint = null;
            
            unselectAll();
            for (var ne : editor.getNotes(r))
            {
                editor.setSelectedNote(ne, true);
            }
        }
    }


    // =============================================================================================
    // Private methods
    // =============================================================================================    
    private void unselectAll()
    {
        new NotesSelection(editor.getLookup()).unselectAll(editor);
    }
    
}

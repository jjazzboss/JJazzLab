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

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.pianoroll.api.EditTool;
import org.jjazz.pianoroll.api.NoteView;
import org.jjazz.pianoroll.api.PianoRollEditor;
import org.jjazz.util.api.ResUtil;
import org.netbeans.api.annotations.common.StaticResource;

/**
 * Select, move, resize notes.
 */
public class SelectionTool implements EditTool
{

    @StaticResource(relative = true)
    private static final String ICON_PATH_OFF = "resources/SelectionOFF.png";
    @StaticResource(relative = true)
    private static final String ICON_PATH_ON = "resources/SelectionON.png";
    private static final int RESIZE_PIXEL_LIMIT = 10;

    private enum State
    {
        EDITOR, RESIZE_WEST, RESIZE_EST, MOVE
    };
    private State state;
    private final PianoRollEditor editor;
    private final SizedPhrase spModel;
    /**
     * If null, no dragging.
     */
    private Point dragStartPoint;
    private NoteEvent dragNoteEvent;
    private static final Logger LOGGER = Logger.getLogger(SelectionTool.class.getSimpleName());

    public SelectionTool(PianoRollEditor editor)
    {
        this.editor = editor;
        spModel = editor.getModel();
        state = State.EDITOR;
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
    public void editorClicked(MouseEvent e)
    {
        editor.unselectAll();
    }

    @Override
    public void noteClicked(MouseEvent e, NoteView nv)
    {
        boolean shift_or_ctrl = (e.getModifiersEx() & (InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK)) != 0;

        if (!shift_or_ctrl)
        {
            editor.unselectAll();
        }
        nv.setSelected(!nv.isSelected());
    }

    @Override
    public void noteWheelMoved(MouseWheelEvent e, NoteView nv)
    {
        //
    }

    @Override
    public void noteDragged(MouseEvent e, NoteView nv)
    {
        if (dragStartPoint == null)
        {
            // Strart dragging
            dragStartPoint = e.getPoint();
            LOGGER.severe("noteDragged() START dragStartPoint=" + dragStartPoint + " ne=" + nv.getModel());
        } else
        {
            var notesPanel = (JPanel) nv.getParent();
            Point editorPoint = SwingUtilities.convertPoint(nv, e.getPoint(), notesPanel);

            int newPitch = editor.getPitchFromPoint(editorPoint);
            float newPos = editor.getPositionFromPoint(editorPoint);

            LOGGER.log(Level.SEVERE, "noteDragged() editorPoint()={0} newPitch={1} newPos={2}", new Object[]
            {
                editorPoint, newPitch, newPos
            });

            if (dragNoteEvent == null)
            {
                dragNoteEvent = nv.getModel().getCopy(newPitch, newPos);
                spModel.add(dragNoteEvent);
            } else
            {
                switch (state)
                {
                    case EDITOR:
                        break;
                    case RESIZE_WEST:
                        break;
                    case RESIZE_EST:
                        break;
                    case MOVE:
                         dragNoteEvent = spModel.move(dragNoteEvent, newPos);
                        int index = spModel.indexOf(dragNoteEvent);
                        dragNoteEvent = dragNoteEvent.getCopyPitch(newPitch);
                        spModel.set(index, dragNoteEvent);
                        break;
                    default:
                        throw new AssertionError(state.name());
                }

            }
        }
    }

    @Override
    public void noteReleased(MouseEvent e, NoteView nv)
    {
        if (dragStartPoint == null)
        {
            throw new IllegalStateException("Should not be here e=" + e + " nv=" + nv);
        } else
        {
            switch (state)
            {
                case EDITOR:
                    break;
                case RESIZE_WEST:
                    break;
                case RESIZE_EST:
                    break;
                case MOVE:
                    spModel.remove(nv.getModel());
                    spModel.add(dragNoteEvent);
                    dragNoteEvent = null;
                    dragStartPoint = null;
                    break;
                default:
                    throw new AssertionError(state.name());

            }
        }
    }

    @Override
    public void noteMoved(MouseEvent e, NoteView nv)
    {
        updateNoteCursor(e, nv);
    }

    @Override
    public void noteEntered(MouseEvent e, NoteView nv)
    {
        updateNoteCursor(e, nv);
    }

    @Override
    public void noteExited(MouseEvent e, NoteView nv)
    {
        nv.setCursor(Cursor.getDefaultCursor());
        state = State.MOVE;
    }

    @Override
    public void editorDragged(MouseEvent e)
    {

    }

    @Override
    public void editorReleased(MouseEvent e)
    {

    }

    @Override
    public void editorWheelMoved(MouseWheelEvent e)
    {

    }

    @Override
    public void editMultipleNotes(List<NoteView> noteViews)
    {
        editor.unselectAll();
        noteViews.forEach(nv -> nv.setSelected(true));
    }


    // =============================================================================================
    // Private methods
    // =============================================================================================   
    private void updateNoteCursor(MouseEvent e, NoteView nv)
    {
        Cursor c;
        if (isNearLeftSide(e, nv))
        {
            c = Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR);
            state = State.RESIZE_WEST;
        } else if (isNearRightSide(e, nv))
        {
            c = Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
            state = State.RESIZE_EST;
        } else
        {
            c = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
            state = State.MOVE;
        }
        nv.setCursor(c);
    }

    private boolean isNearLeftSide(MouseEvent e, NoteView nv)
    {

        boolean res = false;
        int w = nv.getWidth();
        if (w >= RESIZE_PIXEL_LIMIT)
        {
            res = e.getX() < RESIZE_PIXEL_LIMIT;
        }
        return res;
    }

    private boolean isNearRightSide(MouseEvent e, NoteView nv)
    {
        boolean res = false;
        int w = nv.getWidth();
        if (w >= RESIZE_PIXEL_LIMIT)
        {
            res = e.getX() > w - RESIZE_PIXEL_LIMIT - 1;
        }
        return res;
    }

}

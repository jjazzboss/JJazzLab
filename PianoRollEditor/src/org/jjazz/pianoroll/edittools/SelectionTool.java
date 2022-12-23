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
        EDITOR, RESIZE_WEST, RESIZE_EAST, MOVE, COPY
    };
    private State state;
    private final PianoRollEditor editor;
    private final SizedPhrase spModel;

    private boolean isDragging;
    private NoteEvent dragNoteEvent;
    private Point dragNoteOffset;
    private float dragNoteStartPos=-1;
    private float dragNoteStartDuration=-1;
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
        var ne = nv.getModel();
        var point = e.getPoint();
        var notesPanel = (JPanel) nv.getParent();
        Point editorPoint = SwingUtilities.convertPoint(nv, e.getPoint(), notesPanel);

        if (!isDragging)
        {
            // Start dragging
            isDragging = true;
            dragNoteOffset = point;


            // Select only the dragged note
            editor.unselectAll();
            nv.setSelected(true);


            switch (state)
            {
                case EDITOR:
                    break;
                case RESIZE_WEST:
                case RESIZE_EAST:
                    dragNoteStartPos = editor.getPositionFromPoint(editorPoint);
                    dragNoteStartDuration = ne.getDurationInBeats();
                    break;
                case MOVE:
                case COPY:
                    dragNoteEvent = ne.clone();
                    spModel.add(dragNoteEvent);
                    break;
                default:
                    throw new AssertionError(state.name());
            }


            LOGGER.log(Level.FINE, "\n----- noteDragged() ----- START ne={0}   state={1}  dragNoteStartPos={2}  dragNoteEvent={3}", new Object[]
            {
                ne, state, dragNoteStartPos, dragNoteEvent
            });


        } else
        {
            // Continue dragging
            switch (state)
            {
                case EDITOR:
                    break;
                case RESIZE_WEST:
                {

                }
                break;
                case RESIZE_EAST:
                {
                    float newPos = editor.getPositionFromPoint(editorPoint);
                    float dPos = newPos - dragNoteStartPos;
                    float newDur = Math.max(dragNoteStartDuration + dPos, 0.1f);
                    if (ne.getDurationInBeats() != newDur)
                    {
                        var newNe = ne.getCopyDur(newDur);
                        spModel.replace(ne, newNe);
                    }
                }
                break;
                case COPY:
                case MOVE:
                {
                    // Calculate new point
                    editorPoint.translate(-dragNoteOffset.x, -dragNoteOffset.y);
                    if (editorPoint.x < 0)
                    {
                        editorPoint.x = 0;
                    }
                    int newPitch = editor.getPitchFromPoint(editorPoint);
                    float newPos = editor.getPositionFromPoint(editorPoint);
                    if (newPos + ne.getDurationInBeats() > spModel.getBeatRange().to)
                    {
                        newPos = spModel.getBeatRange().to - ne.getDurationInBeats();
                    }

                    if (dragNoteEvent.getPitch() == newPitch && dragNoteEvent.getPositionInBeats() == newPos)
                    {
                        LOGGER.fine("noteDragged()  newPitch and newPos unchanged, skipping");
                        return;
                    }
                    LOGGER.log(Level.FINE, "\nnoteDragged() continue dragging newPitch={0} newPos={1}", new Object[]
                    {
                        newPitch, newPos
                    });
                    dragNoteEvent = spModel.move(dragNoteEvent, newPos);   // Does nothing if newPos unchanged
                    if (dragNoteEvent.getPitch() != newPitch)
                    {
                        var newNe = dragNoteEvent.getCopyPitch(newPitch);
                        spModel.replace(dragNoteEvent, newNe);
                        dragNoteEvent = newNe;
                    }
                }
                break;
                default:
                    throw new AssertionError(state.name());
            }
        }
    }

    @Override
    public void noteReleased(MouseEvent e, NoteView nv)
    {
        var ne = nv.getModel();
        LOGGER.log(Level.FINE, "\nnoteReleased() -- state={0} ne={1} dragNoteEvent={2}", new Object[]
        {
            state, ne, dragNoteEvent
        });

        if (!isDragging)
        {
            LOGGER.fine("noteReleased() Should not be here. Ignored. nv=" + nv + " state=" + state);
            return;
        }

        switch (state)
        {
            case EDITOR:
                break;
            case RESIZE_WEST:
            case RESIZE_EAST:
                dragNoteStartPos = -1;
                dragNoteStartDuration = -1;
                break;
            case MOVE:
                assert spModel.remove(ne) : "ne=" + ne + " spModel=" + spModel;
            // Fall back to COPY
            case COPY:
                var dnv = editor.getNoteView(dragNoteEvent);
                dnv.setSelected(true);
                dragNoteEvent = null;
                dragNoteOffset = null;
                break;
            default:
                throw new AssertionError(state.name());

        }
        isDragging = false;

    }

    @Override
    public void noteMoved(MouseEvent e, NoteView nv)
    {
        if (isDragging)
        {
            return;
        }
        updateNoteCursor(e, nv);
    }

    @Override
    public void noteEntered(MouseEvent e, NoteView nv)
    {
        if (isDragging)
        {
            return;
        }
        updateNoteCursor(e, nv);
    }

    @Override
    public void noteExited(MouseEvent e, NoteView nv)
    {
        if (isDragging)
        {
            return;
        }
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
            state = State.RESIZE_EAST;
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

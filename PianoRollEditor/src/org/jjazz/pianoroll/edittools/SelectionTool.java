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

import java.awt.Container;
import java.awt.Cursor;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.pianoroll.api.EditTool;
import org.jjazz.pianoroll.api.NoteView;
import org.jjazz.pianoroll.api.PianoRollEditor;
import org.jjazz.quantizer.api.Quantization;
import org.jjazz.quantizer.api.Quantizer;
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
        EDITOR(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)),
        RESIZE_WEST(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR)),
        RESIZE_EAST(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR)),
        // MOVE(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)),
        MOVE(load("DnD.Cursor.MoveDrop")),
        COPY(load("DnD.Cursor.CopyDrop"));      // Taken from JDK DragSource.java

        private final Cursor cursor;

        private State(Cursor c)
        {
            cursor = c;
        }

        public Cursor getCursor()
        {
            return cursor;
        }

    };
    private State state;
    private final PianoRollEditor editor;
    private final SizedPhrase spModel;

    private boolean isDragging;
    private NoteEvent dragNoteEvent;
    private NoteView dragNoteView;
    private Point dragNoteOffset;
    private float dragStartPos = -1;
    private float draggedNoteStartDuration = -1;
    private float draggedNoteStartPosition = -1;
    private KeyListener ctrlKeyListener;
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
        var altPressed = (e.getModifiersEx() & InputEvent.ALT_DOWN_MASK) != 0;
        var ctrlPressed = (e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0;
        Point editorPoint = SwingUtilities.convertPoint(nv, e.getPoint(), nv.getParent());
        editorPoint.x = Math.max(0, editorPoint.x);
        editorPoint.x = Math.min(nv.getParent().getWidth() - 1, editorPoint.x);
        float editorPointPos = editor.getPositionFromPoint(editorPoint);
        Quantization q = editor.getQuantization();


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
                    dragStartPos = editorPointPos;
                    draggedNoteStartPosition = ne.getPositionInBeats();
                    draggedNoteStartDuration = ne.getDurationInBeats();
                    break;
                case MOVE:
                case COPY:
                    dragNoteEvent = ne.clone();
                    spModel.add(dragNoteEvent);
                    dragNoteView = editor.getNoteView(dragNoteEvent);

                    // Listen to ctrl key
                    addControlKeyListener(dragNoteView);

                    break;
                default:
                    throw new AssertionError(state.name());
            }


            LOGGER.log(Level.FINE, "\n----- noteDragged() ----- START ne={0}   state={1}  dragNoteStartPos={2}  dragNoteEvent={3}", new Object[]
            {
                ne, state, dragStartPos, dragNoteEvent
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
                    if (!altPressed)
                    {
                        editorPointPos = Quantizer.quantize(q, editorPointPos);
                    }
                    float dPos = editorPointPos - dragStartPos;
                    float newPos = draggedNoteStartPosition + dPos;

                    if (newPos >= 0 && dPos < (draggedNoteStartDuration - 0.05f))
                    {
                        float newDur = draggedNoteStartDuration - dPos;
                        var newNe = ne.getCopyDurPos(newDur, newPos);
                        spModel.replace(ne, newNe);
                    }
                }
                break;
                case RESIZE_EAST:
                {
                    float newPos = editorPointPos;
                    if (!altPressed)
                    {
                        newPos = Quantizer.quantize(q, newPos);
                    }
                    float dPos = newPos - dragStartPos;
                    float newDur = Math.max(draggedNoteStartDuration + dPos, 0.05f);
                    if (ne.getDurationInBeats() != newDur && (ne.getPositionInBeats() + newDur) < spModel.getBeatRange().to)
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

                    if (!altPressed)
                    {
                        newPos = Quantizer.quantize(q, newPos);
                    }

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
        var container = nv.getParent();         // Save if nv is removed
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
                // Nothing
                break;
            case RESIZE_WEST:
            case RESIZE_EAST:
                // Nothing
                break;
            case MOVE:
                assert spModel.remove(ne) : "ne=" + ne + " spModel=" + spModel;
                dragNoteView.setSelected(true);
                removeControlKeyListener(dragNoteView);
                break;
            case COPY:
                nv.setSelected(false);
                dragNoteView.setSelected(true);
                removeControlKeyListener(dragNoteView);
                break;
            default:
                throw new AssertionError(state.name());

        }
        isDragging = false;
        dragStartPos = -1;
        draggedNoteStartDuration = -1;
        draggedNoteStartPosition = -1;
        dragNoteEvent = null;
        dragNoteOffset = null;
        dragNoteView = null;


        changeState(State.EDITOR, container);
    }

    @Override
    public void noteMoved(MouseEvent e, NoteView nv)
    {
        State newState;

        if (isNearLeftSide(e, nv))
        {
            newState = State.RESIZE_WEST;
        } else if (isNearRightSide(e, nv))
        {
            newState = State.RESIZE_EAST;
        } else
        {
            newState = (e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0 ? State.COPY : State.MOVE;
        }

        changeState(newState, nv.getParent());
    }

    @Override
    public void noteEntered(MouseEvent e, NoteView nv)
    {

    }

    @Override
    public void noteExited(MouseEvent e, NoteView nv)
    {
        if (isDragging)
        {
            return;
        }
        changeState(State.EDITOR, nv.getParent());
    }

    @Override
    public void editorDragged(MouseEvent e)
    {

    }

    @Override
    public void editorReleased(MouseEvent e
    )
    {

    }

    @Override
    public void editorWheelMoved(MouseWheelEvent e
    )
    {

    }

    @Override
    public void editMultipleNotes(List<NoteView> noteViews
    )
    {
        editor.unselectAll();
        noteViews.forEach(nv -> nv.setSelected(true));
    }
    // =============================================================================================
    // Private methods
    // =============================================================================================   


    private void changeState(State newState, Container container)
    {
        if (state.equals(newState))
        {
            return;
        }
        state = newState;
        container.setCursor(state.getCursor());      
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

    private static Cursor load(String name)
    {
        if (GraphicsEnvironment.isHeadless())
        {
            throw new IllegalStateException();
        }

        try
        {
            return (Cursor) Toolkit.getDefaultToolkit().getDesktopProperty(name);
        } catch (Exception e)
        {
            throw new RuntimeException("load() Failed to load system cursor: " + name + " : " + e.getMessage());
        }
    }

    private void removeControlKeyListener(NoteView nv)
    {
        assert ctrlKeyListener != null;
        nv.removeKeyListener(ctrlKeyListener);
        ctrlKeyListener = null;
    }

    private void addControlKeyListener(NoteView nv)
    {
        assert ctrlKeyListener == null;

        nv.requestFocusInWindow();

        ctrlKeyListener = new KeyAdapter()
        {
            @Override
            public void keyPressed(KeyEvent e)
            {
                // LOGGER.severe("keyPressed() e=" + e + " icControlDown()=" + e.isControlDown());
                if (e.isControlDown())
                {
                    changeState(State.COPY, nv.getParent());
                }
            }

            @Override
            public void keyReleased(KeyEvent e)
            {
                // LOGGER.severe("keyReleased() e=" + e + " isControlDown()=" + e.isControlDown());
                if (!e.isControlDown())
                {
                    changeState(State.MOVE, nv.getParent());
                }
            }
        };

        nv.addKeyListener(ctrlKeyListener);

    }

}

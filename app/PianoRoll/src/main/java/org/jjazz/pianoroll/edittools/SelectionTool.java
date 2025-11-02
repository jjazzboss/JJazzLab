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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.pianoroll.api.EditTool;
import org.jjazz.pianoroll.api.NoteView;
import org.jjazz.pianoroll.api.PianoRollEditor;
import org.jjazz.quantizer.api.Quantization;
import org.jjazz.quantizer.api.Quantizer;
import org.jjazz.utilities.api.ResUtil;
import org.netbeans.api.annotations.common.StaticResource;

/**
 * Select, moveAll, resize notes.
 */
public class SelectionTool implements EditTool
{

    @StaticResource(relative = true)
    private static final String ICON_PATH_OFF = "resources/SelectionOFF.png";
    @StaticResource(relative = true)
    private static final String ICON_PATH_ON = "resources/SelectionON.png";
    private static final int RESIZE_PIXEL_LIMIT = 8;


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

    private boolean isDragging;
    private final Map<NoteEvent, NoteEvent> mapSrcDragNotes = new HashMap<>();
    private NoteView dragNoteView;
    private float dragStartPos = -1;
    private int dragStartPitch = -1;
    private KeyListener ctrlKeyListener;
    private static final Logger LOGGER = Logger.getLogger(SelectionTool.class.getSimpleName());

    public SelectionTool(PianoRollEditor editor)
    {
        this.editor = editor;
        state = State.EDITOR;
    }

    @Override
    public Cursor getCursor()
    {
        return Cursor.getDefaultCursor();
    }

    @Override
    public Icon getIcon(boolean b)
    {
        return b ? new ImageIcon(SelectionTool.class.getResource(ICON_PATH_ON))
                : new ImageIcon(SelectionTool.class.getResource(ICON_PATH_OFF));
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
        var ne = nv.getModel();
        editor.selectNote(ne, !editor.isNoteSelected(ne));
    }

    @Override
    public void noteWheelMoved(MouseWheelEvent e, NoteView nv)
    {
        // shift-wheel for selected note transposition
        editorWheelMoved((MouseWheelEvent) SwingUtilities.convertMouseEvent(nv, e, nv.getParent()));
    }

    @Override
    public void noteDragged(MouseEvent e, NoteView nvSource)
    {
        var neSource = nvSource.getModel();
        var point = e.getPoint();
        boolean overrideSnapSetting = isOverrideSnapSetting(e);
        Point editorPoint = SwingUtilities.convertPoint(nvSource, e.getPoint(), nvSource.getParent());
        editorPoint.x = Math.max(0, editorPoint.x);
        editorPoint.x = Math.min(nvSource.getParent().getWidth() - 1, editorPoint.x);
        float editorPointPos = editor.getPositionFromPoint(editorPoint);
        Quantization q = editor.getQuantization();


        if (!isDragging)
        {
            // Start dragging
            isDragging = true;
            dragStartPos = editorPointPos;
            dragStartPitch = neSource.getPitch();
            mapSrcDragNotes.clear();


            // Adjust selection if required
            var selectedNotes = editor.getSelectedNoteEvents();
            if (!selectedNotes.contains(neSource))
            {
                // If moved note was not previously selected, just select it and create its drag note
                editor.unselectAll();
                editor.selectNote(neSource, true);
                mapSrcDragNotes.put(neSource, neSource.clone());
            } else
            {
                // Create a drag note for each selected note
                selectedNotes.forEach(ne -> mapSrcDragNotes.put(ne, ne.clone()));
            }


            // Mark all drag notes are isAdjusting
            mapSrcDragNotes.values().forEach(ne -> NoteEvent.markIsAdjustingNote(ne, true));


            switch (state)
            {
                case EDITOR:
                    break;
                case RESIZE_WEST:
                case RESIZE_EAST:
                    // Important: we don't want drag events to be recognized by the undoManager, we'll manage  undo at drag release
                    assert editor.getUndoManager().isEnabled();
                    editor.getUndoManager().setEnabled(false);

                    editor.getModel().replaceAll(mapSrcDragNotes, true);

                    break;
                case MOVE:
                case COPY:
                    // Important: we don't want drag events to be recognized by the undoManager, we'll manage  undo at drag release
                    assert editor.getUndoManager().isEnabled();
                    editor.getUndoManager().setEnabled(false);
                    editor.getModel().addAll(mapSrcDragNotes.values(), true);

                    dragNoteView = editor.getNoteView(mapSrcDragNotes.get(neSource));

                    // Listen to ctrl key
                    addControlKeyListener(dragNoteView.getParent());

                    break;
                default:
                    throw new AssertionError(state.name());
            }


            LOGGER.log(Level.FINE, "\n----- noteDragged() ----- START ne={0}   state={1}  dragNoteStartPos={2}  mapSrcDragNotes={3}",
                    new Object[]
                    {
                        neSource, state, dragStartPos, mapSrcDragNotes
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
                    float posDelta = editorPointPos - dragStartPos;

                    Map<NoteEvent, NoteEvent> mapOldNew = new HashMap<>();
                    for (var srcNe : mapSrcDragNotes.keySet())
                    {
                        float newPos = srcNe.getPositionInBeats() + posDelta;
                        newPos = Math.max(0, newPos);

                        // Quantize
                        if ((editor.isSnapEnabled() && !overrideSnapSetting) || (!editor.isSnapEnabled() && overrideSnapSetting))
                        {
                            newPos = Quantizer.getQuantized(q, newPos);
                            posDelta = newPos - srcNe.getPositionInBeats();
                        }

                        var dragNe = mapSrcDragNotes.get(srcNe);
                        if (Float.compare(dragNe.getPositionInBeats(), newPos) != 0
                                && posDelta < (srcNe.getDurationInBeats() - 0.05f))
                        {
                            float newDur = srcNe.getDurationInBeats() - posDelta;
                            newDur = Math.min(newDur, editor.getPhraseBeatRange().to - 0.001f - newPos);
                            var newDragNe = srcNe.setAll(-1, newDur, -1, newPos, null, true);
                            NoteEvent.markIsAdjustingNote(newDragNe, true);
                            mapOldNew.put(dragNe, newDragNe);
                            mapSrcDragNotes.put(srcNe, newDragNe);
                        }
                    }
                    editor.getModel().replaceAll(mapOldNew, true);
                }
                break;
                case RESIZE_EAST:
                {
                    float dPos = editorPointPos - dragStartPos;

                    Map<NoteEvent, NoteEvent> mapOldNew = new HashMap<>();
                    final float DURATION_MIN = 0.05f;
                    for (var srcNe : mapSrcDragNotes.keySet())
                    {
                        float newDur = Math.max(srcNe.getDurationInBeats() + dPos, DURATION_MIN);

                        // Quantize
                        if ((editor.isSnapEnabled() && !overrideSnapSetting) || (!editor.isSnapEnabled() && overrideSnapSetting))
                        {
                            float endPoint = Quantizer.getQuantized(q, srcNe.getPositionInBeats() + newDur);
                            newDur = Math.max(endPoint - srcNe.getPositionInBeats(), DURATION_MIN);
                        }

                        if (Float.compare(srcNe.getDurationInBeats(), newDur) != 0
                                && (srcNe.getPositionInBeats() + newDur) < editor.getPhraseBeatRange().to)
                        {
                            var dragNe = mapSrcDragNotes.get(srcNe);
                            var newDragNe = srcNe.setDuration(newDur, true);
                            NoteEvent.markIsAdjustingNote(newDragNe, true);
                            mapOldNew.put(dragNe, newDragNe);
                            mapSrcDragNotes.put(srcNe, newDragNe);
                        }
                    }
                    editor.getModel().replaceAll(mapOldNew, true);
                }
                break;
                case COPY:
                case MOVE:
                {
                    // Pitch delta
                    int newDragPitch = editor.getPitchFromPoint(editorPoint);
                    int dPitch = newDragPitch - dragStartPitch;


                    // Position delta
                    float dPos = editorPointPos - dragStartPos;


                    // Apply to each note
                    Map<NoteEvent, NoteEvent> mapOldNew = new HashMap<>();
                    for (var sne : mapSrcDragNotes.keySet())
                    {
                        var dragNe = mapSrcDragNotes.get(sne);


                        // Calculate pos/pitch changes
                        float newDragPos = sne.getPositionInBeats() + dPos;
                        newDragPos = Math.max(0, newDragPos);


                        // Quantize
                        if ((editor.isSnapEnabled() && !overrideSnapSetting) || (!editor.isSnapEnabled() && overrideSnapSetting))
                        {
                            newDragPos = Quantizer.getQuantized(q, newDragPos);
                        }


                        // Check editor limits
                        if (newDragPos + dragNe.getDurationInBeats() >= editor.getPhraseBeatRange().to)
                        {
                            newDragPos = Math.max(0, editor.getPhraseBeatRange().to - 0.001f - dragNe.getDurationInBeats());
                        }
                        newDragPitch = isConstantPitchModifier(e) ? sne.getPitch() : MidiConst.clamp(sne.getPitch() + dPitch);


                        if (dragNe.getPitch() == newDragPitch && Float.compare(dragNe.getPositionInBeats(), newDragPos) == 0)
                        {
                            // No change
                            continue;
                        }
                        LOGGER.log(Level.FINE, "\nnoteDragged() continue dragging newDragPitch={0} newDragPos={1}", new Object[]
                        {
                            newDragPitch, newDragPos
                        });


                        // Move the drag note
                        var newDragNe = dragNe.setAll(newDragPitch, -1, -1, newDragPos, null, true);          // This also copies the isAdjusting clientProperties flag
                        mapOldNew.put(dragNe, newDragNe);
                        mapSrcDragNotes.put(sne, newDragNe);
                    }
                    editor.getModel().replaceAll(mapOldNew, true);


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
        if (!isDragging)
        {
            return;
        }

        var ne = nv.getModel();
        var container = nv.getParent();         // Save if nv is removed
        LOGGER.log(Level.FINE, "\nnoteReleased() -- state={0} ne={1} mapSrcDragNotes={2}", new Object[]
        {
            state, ne, mapSrcDragNotes
        });


        switch (state)
        {
            case EDITOR ->
            {
            }
            case RESIZE_WEST, RESIZE_EAST ->
            {
                // Restore original state before doing the undoable edit
                Map<NoteEvent, NoteEvent> mapOldNew = new HashMap<>();
                for (var srcNe : mapSrcDragNotes.keySet())
                {
                    var dragNe = mapSrcDragNotes.get(srcNe);
                    mapOldNew.put(dragNe, srcNe);
                }
                editor.getModel().replaceAll(mapOldNew, true);


                // Now we make the undoable changes
                assert !editor.getUndoManager().isEnabled();
                editor.getUndoManager().setEnabled(true);
                String undoText = ResUtil.getString(getClass(), "ResizeNote");
                editor.getUndoManager().startCEdit(editor, undoText);


                mapSrcDragNotes.values().forEach(nei -> NoteEvent.markIsAdjustingNote(nei, false));
                editor.getModel().replaceAll(mapSrcDragNotes, false);


                editor.getUndoManager().endCEdit(undoText);

                editor.selectNotes(mapSrcDragNotes.values(), true);

            }
            case MOVE ->
            {
                removeControlKeyListener(dragNoteView.getParent());

                // Restore original state before doing the undoable edit
                editor.getModel().removeAll(mapSrcDragNotes.values(), true);


                // Now we make the undoable changes
                assert !editor.getUndoManager().isEnabled();
                editor.getUndoManager().setEnabled(true);


                String undoText = ResUtil.getString(getClass(), "MoveNote");
                editor.getUndoManager().startCEdit(editor, undoText);

                mapSrcDragNotes.values().forEach(nei -> NoteEvent.markIsAdjustingNote(nei, false));
                editor.getModel().replaceAll(mapSrcDragNotes, false);

                editor.getUndoManager().endCEdit(undoText);

                editor.selectNotes(mapSrcDragNotes.values(), true);
            }

            case COPY ->
            {
                removeControlKeyListener(dragNoteView.getParent());

                // Restore original state before doing the undoable edit
                editor.getModel().removeAll(mapSrcDragNotes.values(), true);


                // Now we make the undoable changes
                assert !editor.getUndoManager().isEnabled();
                editor.getUndoManager().setEnabled(true);


                String undoText = ResUtil.getString(getClass(), "CopyNote");
                editor.getUndoManager().startCEdit(editor, undoText);


                mapSrcDragNotes.values().forEach(nei -> NoteEvent.markIsAdjustingNote(nei, false));
                editor.getModel().addAll(mapSrcDragNotes.values(), false);


                editor.selectNotes(mapSrcDragNotes.keySet(), false);
                editor.selectNotes(mapSrcDragNotes.values(), true);


                editor.getUndoManager().endCEdit(undoText);
            }
            default ->
                throw new AssertionError(state.name());

        }


        isDragging = false;
        dragStartPos = -1;
        mapSrcDragNotes.clear();
        dragNoteView = null;


        changeState(State.EDITOR, container);
    }

    @Override
    public void noteMoved(MouseEvent e, NoteView nv)
    {
        State newState;

        if (!editor.isDrums() && isNearLeftSide(e, nv))
        {
            newState = State.RESIZE_WEST;
        } else if (!editor.isDrums() && isNearRightSide(e, nv))
        {
            newState = State.RESIZE_EAST;
        } else
        {
            newState = (e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0 ? State.COPY : State.MOVE;
        }

        changeState(newState, nv.getParent());
        // LOGGER.severe("noteMoved() ne=" + nv.getModel() + " > state=" + newState);
    }

    @Override
    public void noteEntered(MouseEvent e, NoteView nv)
    {

    }

    @Override
    public void noteExited(MouseEvent e, NoteView nv)
    {
        // Adding SwingUtilities.isLeftMouseButton(e) check to fix #602 
        // We don't change the state if mouse is being pressed, because it probably means that user has initiated a drag. But on Linux, drag threshold can be bigger (5 pixels ?)
        // than on Windows (2 pixels), so on Linux you need to move the mouse more to get the first call to mouseDragged().
        // When drag will be over, this method should be called again and this time we will switch state to EDITOR.
        if (isDragging || SwingUtilities.isLeftMouseButton(e))
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
    public void editorReleased(MouseEvent e)
    {

    }

    @Override
    public void editorWheelMoved(MouseWheelEvent e)
    {
        // alt-wheel to change velocity of selected notes
        if (e.isAltDown() && !e.isControlDown())
        {
            final int STEP = 4;

            String undoText = ResUtil.getString(getClass(), "ChangeVelocity");
            editor.getUndoManager().startCEdit(editor, undoText);


            Map<NoteEvent, NoteEvent> mapOldNew = new HashMap<>();
            for (var snv : editor.getSelectedNoteViews())
            {
                var ne = snv.getModel();
                int delta = (e.getWheelRotation() < 0) ? STEP : -STEP;
                int newVel = MidiConst.clamp(ne.getVelocity() + delta);
                var newNe = ne.setVelocity(newVel, true);
                mapOldNew.put(ne, newNe);
            }
            editor.getModel().replaceAll(mapOldNew, false);

            editor.getUndoManager().endCEdit(undoText);
        }

    }

    @Override
    public boolean isEditMultipleNotesSupported()
    {
        return true;
    }

    @Override
    public void editMultipleNotes(List<NoteView> noteViews)
    {
        editor.unselectAll();
        editor.selectNotes(NoteView.getNotes(noteViews), true);
    }

    @Override
    public String toString()
    {
        return "SelectionTool";
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
        int w = nv.getWidth();
        int limit = Math.min(w / 2, RESIZE_PIXEL_LIMIT);
        limit = Math.max(2, limit);
        boolean res = e.getX() < limit;
        return res;
    }

    private boolean isNearRightSide(MouseEvent e, NoteView nv)
    {
        int w = nv.getWidth();
        int limit = Math.min(w / 2, RESIZE_PIXEL_LIMIT);
        limit = Math.max(2, limit);
        boolean res = e.getX() >= w - limit - 1;
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

    private void removeControlKeyListener(Container container)
    {
        assert ctrlKeyListener != null;
        container.removeKeyListener(ctrlKeyListener);
        ctrlKeyListener = null;
    }

    private void addControlKeyListener(Container container)
    {
        assert ctrlKeyListener == null;

        container.requestFocusInWindow();

        ctrlKeyListener = new KeyAdapter()
        {
            @Override
            public void keyPressed(KeyEvent e)
            {
                // LOGGER.severe("keyPressed() e=" + e + " icControlDown()=" + e.isControlDown());
                if (e.isControlDown())
                {
                    changeState(State.COPY, container);
                }
            }

            @Override
            public void keyReleased(KeyEvent e)
            {
                // LOGGER.severe("keyReleased() e=" + e + " isControlDown()=" + e.isControlDown());
                if (!e.isControlDown())
                {
                    changeState(State.MOVE, container);
                }
            }
        };

        container.addKeyListener(ctrlKeyListener);

    }
}

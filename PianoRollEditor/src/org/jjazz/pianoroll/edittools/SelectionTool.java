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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import org.jjazz.midi.api.MidiUtilities;
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
    private final SizedPhrase spModel;

    private boolean isDragging;
    private final Map<NoteEvent, NoteEvent> mapSrcDragNotes = new HashMap<>();
    private NoteView dragNoteView;
    private Point dragNoteOffset;
    private float dragStartPos = -1;
    private int dragStartPitch = -1;
    private KeyListener ctrlKeyListener;
    private static final Logger LOGGER = Logger.getLogger(SelectionTool.class.getSimpleName());

    public SelectionTool(PianoRollEditor editor)
    {
        this.editor = editor;
        spModel = editor.getModel();
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
            dragNoteOffset = point;
            dragStartPitch = neSource.getPitch();
            mapSrcDragNotes.clear();


            // Adjust selection if required
            var selectedNotes = editor.getSelectedNotes();
            if (!selectedNotes.contains(neSource))
            {
                editor.unselectAll();
                nvSource.setSelected(true);
                // Create one drag note
                mapSrcDragNotes.put(neSource, neSource.clone());
            } else
            {
                // Create the drag notes for the selected notes
                selectedNotes.forEach(ne -> mapSrcDragNotes.put(ne, ne.clone()));
            }


            switch (state)
            {
                case EDITOR:
                    break;
                case RESIZE_WEST:
                case RESIZE_EAST:
                    // Important: we don't want drag events to be recognized by the undoManager, we'll manage  undo at drag release
                    assert editor.getUndoManager().isEnabled();
                    editor.getUndoManager().setEnabled(false);

                    mapSrcDragNotes.keySet().forEach(ne -> spModel.replace(ne, mapSrcDragNotes.get(ne)));

                    break;
                case MOVE:
                case COPY:
                    // Important: we don't want drag events to be recognized by the undoManager, we'll manage  undo at drag release
                    assert editor.getUndoManager().isEnabled();
                    editor.getUndoManager().setEnabled(false);

                    mapSrcDragNotes.values().forEach(ne -> spModel.add(ne));
                    dragNoteView = editor.getNoteView(mapSrcDragNotes.get(neSource));

                    // Listen to ctrl key
                    addControlKeyListener(dragNoteView);

                    break;
                default:
                    throw new AssertionError(state.name());
            }


            LOGGER.log(Level.FINE, "\n----- noteDragged() ----- START ne={0}   state={1}  dragNoteStartPos={2}  mapSrcDragNotes={3}", new Object[]
            {
                neSource, state, dragStartPos, mapSrcDragNotes
            });


        } else
        {
            // Continue dragging


            // Quantize new position if required
            if ((editor.isSnapEnabled() && !overrideSnapSetting) || (!editor.isSnapEnabled() && overrideSnapSetting))
            {
                editorPointPos = Quantizer.getQuantized(q, editorPointPos);
            }


            switch (state)
            {
                case EDITOR:
                    break;
                    
                case RESIZE_WEST:
                {
                    float dPos = editorPointPos - dragStartPos;

                    for (var srcNe : mapSrcDragNotes.keySet())
                    {
                        float newPos = srcNe.getPositionInBeats() + dPos;

                        if (newPos >= 0 && dPos < (srcNe.getDurationInBeats() - 0.05f))
                        {
                            var dragNe = mapSrcDragNotes.get(srcNe);
                            float newDur = srcNe.getDurationInBeats() - dPos;
                            var newNe = srcNe.getCopyDurPos(newDur, newPos);
                            spModel.replace(dragNe, newNe);
                            mapSrcDragNotes.put(srcNe, newNe);
                        }
                    }
                }
                break;
                case RESIZE_EAST:
                {
                    float dPos = editorPointPos - dragStartPos;

                    for (var srcNe : mapSrcDragNotes.keySet())
                    {
                        float newDur = Math.max(srcNe.getDurationInBeats() + dPos, 0.05f);
                        if (Float.floatToIntBits(srcNe.getDurationInBeats()) != Float.floatToIntBits(newDur) && (srcNe.getPositionInBeats() + newDur) < spModel.getBeatRange().to)
                        {
                            var dragNe = mapSrcDragNotes.get(srcNe);
                            var newNe = srcNe.getCopyDur(newDur);
                            spModel.replace(dragNe, newNe);
                            mapSrcDragNotes.put(srcNe, newNe);
                        }
                    }
                }
                break;
                case COPY:
                case MOVE:
                {
                    // Pitch delta
                    int newPitch = editor.getPitchFromPoint(editorPoint);
                    int dPitch = newPitch - dragStartPitch;

                    // Position delta
                    float dPos = editorPointPos - dragStartPos;


                    // Apply to each note
                    for (var sne : mapSrcDragNotes.keySet())
                    {
                        var dragNe = mapSrcDragNotes.get(sne);

                        // Calculate pos/pitch changes
                        float newPos = sne.getPositionInBeats() + dPos;
                        newPos = Math.max(0, newPos);
                        if (newPos + sne.getDurationInBeats() >= spModel.getBeatRange().to)
                        {
                            newPos = spModel.getBeatRange().to - sne.getDurationInBeats();
                        }
                        newPitch = isConstantPitchModifier(e) ? sne.getPitch() : MidiUtilities.limit(sne.getPitch() + dPitch);


                        if (dragNe.getPitch() == newPitch && Float.floatToIntBits(dragNe.getPositionInBeats()) == Float.floatToIntBits(newPos))
                        {

                            return;
                        }
                        LOGGER.log(Level.FINE, "\nnoteDragged() continue dragging newPitch={0} newPos={1}", new Object[]
                        {
                            newPitch, newPos
                        });


                        var newNe = spModel.move(dragNe, newPos);   // Does nothing if newPos unchanged                        
                        if (newNe.getPitch() != newPitch)
                        {
                            var newNePitch = newNe.getCopyPitch(newPitch);
                            spModel.replace(newNe, newNePitch);
                            newNe = newNePitch;
                        }

                        mapSrcDragNotes.put(sne, newNe);
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
            case EDITOR:
                // Nothing
                break;
            case RESIZE_WEST:
            case RESIZE_EAST:
            {
                // Restore original state before doing the undoable edit
                for (var srcNe : mapSrcDragNotes.keySet())
                {
                    var dragNe = mapSrcDragNotes.get(srcNe);
                    spModel.replace(dragNe, srcNe);
                }

                // Now we make the undoable changes
                assert !editor.getUndoManager().isEnabled();
                editor.getUndoManager().setEnabled(true);


                String undoText = ResUtil.getString(getClass(), "ResizeNote");
                editor.getUndoManager().startCEdit(undoText);

                for (var srcNe : mapSrcDragNotes.keySet())
                {
                    var dragNe = mapSrcDragNotes.get(srcNe);
                    spModel.replace(srcNe, dragNe);
                    editor.getNoteView(dragNe).setSelected(true);
                }

                editor.getUndoManager().endCEdit(undoText);
                break;
            }

            case MOVE:
            {
                removeControlKeyListener(dragNoteView);


                // Restore original state before doing the undoable edit
                mapSrcDragNotes.values().forEach(dragNe -> spModel.remove(dragNe));


                // Now we make the undoable changes
                assert !editor.getUndoManager().isEnabled();
                editor.getUndoManager().setEnabled(true);


                String undoText = ResUtil.getString(getClass(), "MoveNote");
                editor.getUndoManager().startCEdit(undoText);

                for (var srcNe : mapSrcDragNotes.keySet())
                {
                    var dragNe = mapSrcDragNotes.get(srcNe);
                    spModel.replace(srcNe, dragNe);
                    editor.getNoteView(dragNe).setSelected(true);
                }

                editor.getUndoManager().endCEdit(undoText);


                break;
            }
            case COPY:
            {
                removeControlKeyListener(dragNoteView);

                // Restore original state before doing the undoable edit
                mapSrcDragNotes.values().forEach(dragNe -> spModel.remove(dragNe));


                // Now we make the undoable changes
                assert !editor.getUndoManager().isEnabled();
                editor.getUndoManager().setEnabled(true);

                String undoText = ResUtil.getString(getClass(), "CopyNote");
                editor.getUndoManager().startCEdit(undoText);

                for (var srcNe : mapSrcDragNotes.keySet())
                {
                    var dragNe = mapSrcDragNotes.get(srcNe);
                    spModel.add(dragNe);
                    editor.getNoteView(dragNe).setSelected(true);
                    editor.getNoteView(srcNe).setSelected(false);
                }

                editor.getUndoManager().endCEdit(undoText);


                break;
            }
            default:
                throw new AssertionError(state.name());

        }

        isDragging = false;
        dragStartPos = -1;
        mapSrcDragNotes.clear();
        dragNoteOffset = null;
        dragNoteView = null;


        changeState(State.EDITOR, container);
    }

    @Override
    public void noteMoved(MouseEvent e, NoteView nv)
    {

        State newState;

        if (!editor.isDrumEdit() && isNearLeftSide(e, nv))
        {
            newState = State.RESIZE_WEST;
        } else if (!editor.isDrumEdit() && isNearRightSide(e, nv))
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
    public boolean isEditMultipleNotesSupported()
    {
        return true;
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
        int w = nv.getWidth();
        int limit = Math.min(w / 3, RESIZE_PIXEL_LIMIT);
        limit = Math.max(1, limit);
        boolean res = e.getX() < limit;
        return res;
    }

    private boolean isNearRightSide(MouseEvent e, NoteView nv)
    {
        int w = nv.getWidth();
        int limit = Math.min(w / 3, RESIZE_PIXEL_LIMIT);
        limit = Math.max(1, limit);
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

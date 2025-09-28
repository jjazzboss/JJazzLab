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

import java.awt.Cursor;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.pianoroll.NoteViewDrum;
import org.jjazz.pianoroll.api.EditTool;
import org.jjazz.pianoroll.api.NoteView;
import org.jjazz.pianoroll.api.PianoRollEditor;
import org.jjazz.quantizer.api.Quantizer;
import org.jjazz.utilities.api.FloatRange;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.utilities.api.Utilities;
import org.netbeans.api.annotations.common.StaticResource;

/**
 * Draw notes.
 * <p>
 * Use the velocity of the last selected note.
 */
public class PencilTool implements EditTool
{

    @StaticResource(relative = true)
    private static final String ICON_PATH_OFF = "resources/PencilOFF.png";
    private static final Icon ICON_SELECTED = new ImageIcon(PencilTool.class.getResource(ICON_PATH_OFF));
    @StaticResource(relative = true)
    private static final String ICON_PATH_ON = "resources/PencilON.png";
    private static final Icon ICON_UNSELECTED = new ImageIcon(PencilTool.class.getResource(ICON_PATH_ON));
    @StaticResource(relative = true)
    private static final String CURSOR_PATH = "resources/PencilCursor.png";
    @StaticResource(relative = true)
    private static final String CURSOR_PATH_WIN = "resources/PencilCursor-Win.png";
    private static final Image CURSOR_IMAGE = new ImageIcon(PencilTool.class.getResource(CURSOR_PATH)).getImage();
    private static final Image CURSOR_IMAGE_WIN = new ImageIcon(PencilTool.class.getResource(CURSOR_PATH_WIN)).getImage();
    private static final Cursor CURSOR = Utilities.isWindows()
            ? Toolkit.getDefaultToolkit().createCustomCursor(CURSOR_IMAGE_WIN, new Point(0, 31), "Pencil")
            : Toolkit.getDefaultToolkit().createCustomCursor(CURSOR_IMAGE, new Point(0, 13), "Pencil");


    private final PianoRollEditor editor;
    private NoteEvent srcNote;
    private NoteEvent dragNote;
    private int dragPitch;
    private float dragStartPos;
    private int lastSelectedNoteVelocity;

    private static final Logger LOGGER = Logger.getLogger(PencilTool.class.getSimpleName());

    public PencilTool(PianoRollEditor editor)
    {
        this.editor = editor;
        lastSelectedNoteVelocity = 64;


        // Listen to selection to update lastSelectedNoteVelocity
        this.editor.addPropertyChangeListener(PianoRollEditor.PROP_SELECTED_NOTE_VIEWS, e -> 
        {
            boolean b = (boolean) e.getNewValue();
            List<NoteView> nvs = (List<NoteView>) e.getOldValue();
            if (b && !nvs.isEmpty())
            {
                lastSelectedNoteVelocity = nvs.get(0).getModel().getVelocity();
            }
        });
    }

    @Override
    public Icon getIcon(boolean b)
    {
        return b ? ICON_UNSELECTED : ICON_SELECTED;
    }

    @Override
    public Cursor getCursor()
    {
        return CURSOR;
    }

    @Override
    public String getName()
    {
        return ResUtil.getString(getClass(), "PencilName");
    }

    @Override
    public void editorClicked(MouseEvent e)
    {
        // LOGGER.log(Level.SEVERE, "editorClicked() -- e={0}", e);
        if (!SwingUtilities.isLeftMouseButton(e))
        {
            return;
        }
        editor.unselectAll();

        String undoText = ResUtil.getString(getClass(), "AddNote");
        editor.getUndoManager().startCEdit(editor, undoText);

        var ne = addNote(e, false);

        editor.getUndoManager().endCEdit(undoText);

        editor.selectNote(ne, true);
    }

    @Override
    public void editorDragged(MouseEvent e)
    {
        Point point = e.getPoint();
        boolean overrideSnapSetting = isOverrideSnapSetting(e);
        FloatRange beatRange = editor.getPhraseBeatRange();
        float pos = editor.getPositionFromPoint(point);

        if (dragNote == null)
        {
            // Start dragging     


            // Important: we don't want drag note to be recognized by the undoManager, we'll manage  undo at drag release
            assert editor.getUndoManager().isEnabled();
            editor.getUndoManager().setEnabled(false);


            dragNote = addNote(e, true);
            dragStartPos = dragNote.getPositionInBeats();
            dragPitch = dragNote.getPitch();

            editor.unselectAll();
            editor.selectNote(dragNote, true);

        } else if (!editor.isDrums())
        {
            // Continue dragging
            float dur = pos - dragStartPos;
            dur = Math.max(0.1f, dur);
            if ((editor.isSnapEnabled() && !overrideSnapSetting) || (!editor.isSnapEnabled() && overrideSnapSetting))
            {
                var q = editor.getQuantization();
                dur = Quantizer.getQuantized(q, dur);
                dur = Math.max(q.getSymbolicDuration().getDuration(), dur);
            }
            dur = Math.min(beatRange.to - dragStartPos, dur);
            var ne = dragNote.setDuration(dur, true);         // This copies also the adjustingNote flag in clientProperties
            editor.getModel().replaceAll(Map.of(dragNote, ne), true);
            dragNote = ne;
        }
    }

    @Override
    public void editorReleased(MouseEvent e)
    {
        if (dragNote == null)
        {
            return;
        }

        editor.getModel().remove(dragNote, true);       // This will unselect and remove the NoteViewer

        // Now we can make the undoable changes
        assert !editor.getUndoManager().isEnabled();
        editor.getUndoManager().setEnabled(true);


        String undoText = ResUtil.getString(getClass(), "AddNote");
        editor.getUndoManager().startCEdit(editor, undoText);

        NoteEvent.markIsAdjustingNote(dragNote, false);
        editor.getModel().add(dragNote, false);
        editor.selectNote(dragNote, true);

        editor.getUndoManager().endCEdit(undoText);

        dragNote = null;
    }


    @Override
    public void editorWheelMoved(MouseWheelEvent e)
    {

    }

    @Override
    public void noteClicked(MouseEvent e, NoteView nv)
    {
        // LOGGER.log(Level.SEVERE, "noteClicked() nv={0}", nv);
        String undoText = ResUtil.getString(getClass(), "ReplaceNote");
        MouseEvent editorEvent = SwingUtilities.convertMouseEvent(nv, e, nv.getParent());

        editor.getUndoManager().startCEdit(editor, undoText);

        if (!editor.isDrums())
        {
            // Replace note in melodic mode
            editor.getModel().remove(nv.getModel());
        }
        NoteEvent ne = addNote(editorEvent, false);

        editor.getUndoManager().endCEdit(undoText);


        editor.unselectAll();
        editor.selectNote(ne, true);

    }

    @Override
    public void noteWheelMoved(MouseWheelEvent e, NoteView nv)
    {

    }

    @Override
    public void noteDragged(MouseEvent e, NoteView nv)
    {
        Point editorPoint = SwingUtilities.convertPoint(nv, e.getPoint(), nv.getParent());
        boolean overrideSnapSetting = isOverrideSnapSetting(e);
        FloatRange beatRange = editor.getPhraseBeatRange();
        float pos = editor.getPositionFromPoint(editorPoint);
        if ((editor.isSnapEnabled() && !overrideSnapSetting) || (!editor.isSnapEnabled() && overrideSnapSetting))
        {
            var q = editor.getQuantization();
            pos = Quantizer.getQuantized(q, pos);
        }
        pos = Math.min(beatRange.to - 0.1f, pos);


        if (editor.isDrums())
        {
            // Special mode: just add the note then ignore the rest of the dragging
            if (dragNote == null)
            {
                var editorEvent = SwingUtilities.convertMouseEvent(nv, e, nv.getParent());
                dragNote = addNote(editorEvent, false);
                editor.unselectAll();
                editor.selectNote(dragNote, true);
            }
            return;
        }

        if (dragNote == null)
        {
            // Start dragging  
            srcNote = nv.getModel();
            dragStartPos = srcNote.getPositionInBeats();
            float dur = pos - dragStartPos;
            dur = Math.max(0.1f, dur);
            dragNote = srcNote.setDuration(dur, true);
            NoteEvent.markIsAdjustingNote(dragNote, true);
            editor.getModel().replaceAll(Map.of(srcNote, dragNote), true);

            editor.unselectAll();
            editor.selectNote(dragNote, true);

        } else
        {
            // Continue dragging
            float dur = pos - dragStartPos;
            dur = Math.max(0.1f, dur);
            var newNe = dragNote.setDuration(dur, true);                      // This also copies the adjusting flag in clientProperties
            editor.getModel().replaceAll(Map.of(dragNote, newNe), true);
            dragNote = newNe;
        }
    }

    @Override
    public void noteReleased(MouseEvent e, NoteView nv)
    {
        if (dragNote == null || editor.isDrums())
        {
            dragNote = null;
            return;
        }


        // Restore state
        editor.getModel().replace(dragNote, srcNote, true);
        

        // Perform the change        
        String undoText = ResUtil.getString(getClass(), "ResizeNote", dragNote.toPianoOctaveString());
        editor.getUndoManager().startCEdit(editor, undoText);

        NoteEvent.markIsAdjustingNote(dragNote, false);
        editor.getModel().replace(srcNote, dragNote, false);
        
        editor.getUndoManager().endCEdit(undoText);

        dragNote = null;

    }

    @Override
    public void noteMoved(MouseEvent e, NoteView nv)
    {
    }

    @Override
    public void noteEntered(MouseEvent e, NoteView nv)
    {

    }

    @Override
    public void noteExited(MouseEvent e, NoteView nv)
    {
    }

    @Override
    public boolean isEditMultipleNotesSupported()
    {
        return false;
    }

    @Override
    public void editMultipleNotes(List<NoteView> noteViews)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString()
    {
        return "PencilTool";
    }
    // =============================================================================================
    // Private methods
    // =============================================================================================   

    /**
     * Add a NoteEvent at the mouse position using the last selected note velocity.
     *
     * @param e
     * @param isAdjusting
     * @return The created NoteEvent.
     */
    private NoteEvent addNote(MouseEvent e, boolean isAdjusting)
    {
        boolean overrideSnapSetting = isOverrideSnapSetting(e);
        var q = editor.getQuantization();
        var point = e.getPoint();
        var beatRange = editor.getPhraseBeatRange();


        float pos = editor.getPositionFromPoint(point);
        int pitch = editor.getPitchFromPoint(point);
        if ((editor.isSnapEnabled() && !overrideSnapSetting) || (!editor.isSnapEnabled() && overrideSnapSetting))
        {
            pos = Quantizer.getQuantizedPrevious(q, pos);
        }
        pos = Math.min(pos, beatRange.to - 0.1f);
        float dur = editor.isDrums() ? NoteViewDrum.DURATION : q.getSymbolicDuration().getDuration();
        if (!beatRange.contains(pos + dur, false))
        {
            dur = beatRange.to - pos;
        }

        NoteEvent ne = new NoteEvent(pitch, dur, getNewNoteVelocity(), pos);
        NoteEvent.markIsAdjustingNote(ne, isAdjusting);
        boolean b = editor.getModel().add(ne, isAdjusting);          // Fix Issue #441 (add was performed as an assert !)
        assert b : "ne=" + ne + " editor.getModel()=" + editor.getModel();

        return ne;
    }

    private int getNewNoteVelocity()
    {
        return lastSelectedNoteVelocity;
    }


}

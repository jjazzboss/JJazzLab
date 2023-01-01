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
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.pianoroll.api.EditTool;
import org.jjazz.pianoroll.api.NoteView;
import org.jjazz.pianoroll.api.PianoRollEditor;
import org.jjazz.quantizer.api.Quantizer;
import org.jjazz.util.api.FloatRange;
import org.jjazz.util.api.ResUtil;
import org.jjazz.util.api.Utilities;
import org.netbeans.api.annotations.common.StaticResource;

/**
 * Draw notes.
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
    private final SizedPhrase spModel;
    private NoteEvent dragNote;
    private int dragPitch;
    private float dragStartPos;

    private static final Logger LOGGER = Logger.getLogger(PencilTool.class.getSimpleName());

    public PencilTool(PianoRollEditor editor)
    {
        this.editor = editor;
        spModel = editor.getModel();
    }

    @Override
    public Icon getIcon(boolean b)
    {
        return b ? ICON_UNSELECTED : ICON_SELECTED;
    }

    @Override
    public Cursor getCURSOR()
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
        String undoText = ResUtil.getString(getClass(), "AddNote");
        editor.getUndoManager().startCEdit(undoText);

        var ne = addNote(e);

        editor.getUndoManager().endCEdit(undoText);

        editor.unselectAll();
        editor.getNoteView(ne).setSelected(true);
    }

    @Override
    public void editorDragged(MouseEvent e)
    {
        Point point = e.getPoint();
        boolean overrideSnapSetting = isOverrideSnapSetting(e);
        FloatRange beatRange = spModel.getBeatRange();
        float pos = editor.getPositionFromPoint(point);

        if (dragNote == null)
        {
            // Start dragging            
            dragNote = addNote(e);
            dragStartPos = dragNote.getPositionInBeats();
            dragPitch = dragNote.getPitch();

            editor.unselectAll();
            editor.getNoteView(dragNote).setSelected(true);

        } else
        {
            // Continue dragging
            float dur = pos - dragStartPos;
            dur = Math.max(0.1f, dur);
            if ((editor.isSnapEnabled() && !overrideSnapSetting) || (!editor.isSnapEnabled() && overrideSnapSetting))
            {
                var q = editor.getQuantization();
                dur = Quantizer.getQuantized(q, dur);
                dur = Math.max(q.getDuration(), dur);
            }
            dur = Math.min(beatRange.to - dragStartPos, dur);
            var ne = new NoteEvent(dragPitch, dur, getNewNotePitch(), dragStartPos);
            spModel.replace(dragNote, ne);
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

        spModel.remove(dragNote);

        String undoText = ResUtil.getString(getClass(), "AddNote");
        editor.getUndoManager().startCEdit(undoText);

        spModel.add(dragNote);

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
        String undoText = ResUtil.getString(getClass(), "ReplaceNote");
        editor.getUndoManager().startCEdit(undoText);


        MouseEvent editorEvent = SwingUtilities.convertMouseEvent(nv, e, nv.getParent());
        spModel.remove(nv.getModel());
        var ne = addNote(editorEvent);
        editor.unselectAll();
        editor.getNoteView(ne).setSelected(true);


        editor.getUndoManager().endCEdit(undoText);

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
        FloatRange beatRange = spModel.getBeatRange();
        float pos = editor.getPositionFromPoint(editorPoint);
        if ((editor.isSnapEnabled() && !overrideSnapSetting) || (!editor.isSnapEnabled() && overrideSnapSetting))
        {
            var q = editor.getQuantization();
            pos = Quantizer.getQuantized(q, pos);
        }
        pos = Math.min(beatRange.to - 0.1f, pos);


        if (dragNote == null)
        {
            // Start dragging  
            var ne = nv.getModel();
            dragStartPos = ne.getPositionInBeats();
            float dur = pos - dragStartPos;
            dur = Math.max(0.1f, dur);
            dragNote = ne.getCopyDur(dur);
            spModel.replace(ne, dragNote);

            editor.unselectAll();
            editor.getNoteView(dragNote).setSelected(true);

        } else
        {
            // Continue dragging
            float dur = pos - dragStartPos;
            dur = Math.max(0.1f, dur);
            var newNe = dragNote.getCopyDur(dur);
            spModel.replace(dragNote, newNe);
            dragNote = newNe;
        }
    }

    @Override
    public void noteReleased(MouseEvent e, NoteView nv)
    {
        if (dragNote == null)
        {
            return;
        }

        spModel.replace(dragNote, nv.getModel());

        String undoText = ResUtil.getString(getClass(), "ResizeNote", dragNote.toPianoOctaveString());
        editor.getUndoManager().startCEdit(undoText);

        spModel.replace(nv.getModel(), dragNote);

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

    // =============================================================================================
    // Private methods
    // =============================================================================================   

    private NoteEvent addNote(MouseEvent e)
    {
        boolean overrideSnapSetting = isOverrideSnapSetting(e);
        var q = editor.getQuantization();
        var point = e.getPoint();
        var beatRange = spModel.getBeatRange();


        float pos = editor.getPositionFromPoint(point);
        int pitch = editor.getPitchFromPoint(point);
        if (editor.isSnapEnabled() && !overrideSnapSetting)
        {
            pos = Quantizer.getQuantizedPrevious(q, pos);
        }
        pos = Math.min(pos, beatRange.to - 0.1f);
        float dur = q.getDuration();
        if (!beatRange.contains(pos + dur, false))
        {
            dur = beatRange.to - pos;
        }

        NoteEvent ne = new NoteEvent(pitch, dur, getNewNotePitch(), pos);
        assert spModel.add(ne) : "ne=" + ne + " spModel=" + spModel;

        return ne;
    }

    private int getNewNotePitch()
    {
        return 64;
    }

}

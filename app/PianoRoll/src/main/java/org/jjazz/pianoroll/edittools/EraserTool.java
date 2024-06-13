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
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.jjazz.pianoroll.api.EditTool;
import org.jjazz.pianoroll.api.NoteView;
import org.jjazz.pianoroll.api.PianoRollEditor;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.utilities.api.Utilities;
import org.netbeans.api.annotations.common.StaticResource;

/**
 * Erase notes.
 */
public class EraserTool implements EditTool
{

    @StaticResource(relative = true)
    private static final String ICON_PATH_OFF = "resources/EraserOFF.png";
    private static final Icon ICON_SELECTED = new ImageIcon(EraserTool.class.getResource(ICON_PATH_OFF));
    @StaticResource(relative = true)
    private static final String ICON_PATH_ON = "resources/EraserON.png";
    private static final Icon ICON_UNSELECTED = new ImageIcon(EraserTool.class.getResource(ICON_PATH_ON));
    @StaticResource(relative = true)
    private static final String CURSOR_PATH = "resources/EraserCursor.png";
    @StaticResource(relative = true)
    private static final String CURSOR_PATH_WIN = "resources/EraserCursor-Win.png";
    private static final Image CURSOR_IMAGE = new ImageIcon(EraserTool.class.getResource(CURSOR_PATH)).getImage();
    private static final Image CURSOR_IMAGE_WIN = new ImageIcon(EraserTool.class.getResource(CURSOR_PATH_WIN)).getImage();
    private static final Cursor CURSOR = Utilities.isWindows()
            ? Toolkit.getDefaultToolkit().createCustomCursor(CURSOR_IMAGE_WIN, new Point(2, 29), "Eraser")
            : Toolkit.getDefaultToolkit().createCustomCursor(CURSOR_IMAGE, new Point(2, 16), "Eraser");


    private final PianoRollEditor editor;

    private static final Logger LOGGER = Logger.getLogger(EraserTool.class.getSimpleName());

    public EraserTool(PianoRollEditor editor)
    {
        this.editor = editor;
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
        return ResUtil.getString(getClass(), "EraserName");
    }

    @Override
    public void editorClicked(MouseEvent e)
    {
        // Check if there is note near the clicked point, if yes erase it
        final int RANGE = 3;
        var container = (Container) e.getSource();

        Function<Point, Boolean> removeIfNoteViewFound = p ->
        {
            var c = container.getComponentAt(p);
            return (c instanceof NoteView nv) && editor.getModel().remove(nv.getModel());
        };

        Point p = e.getPoint();
        var points = Arrays.asList(new Point(p.x, p.y - RANGE),
                new Point(p.x - RANGE, p.y - RANGE),
                new Point(p.x + RANGE, p.y - RANGE),
                new Point(p.x - RANGE, p.y),
                new Point(p.x + RANGE, p.y),
                new Point(p.x, p.y + RANGE),
                new Point(p.x - RANGE, p.y + RANGE),
                new Point(p.x + RANGE, p.y + RANGE)
        );


        String undoText = ResUtil.getString(getClass(), "EraseNote");
        editor.getUndoManager().startCEdit(editor, undoText);

        points.stream()
                .filter(pt -> removeIfNoteViewFound.apply(pt))
                .findAny();     // Stop at first found

        editor.getUndoManager().endCEdit(undoText);
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
    public void noteClicked(MouseEvent e, NoteView nv)
    {
        String undoText = ResUtil.getString(getClass(), "EraseNote");
        editor.getUndoManager().startCEdit(editor, undoText);

        editor.getModel().remove(nv.getModel());

        editor.getUndoManager().endCEdit(undoText);
    }

    @Override
    public void noteWheelMoved(MouseWheelEvent e, NoteView nv)
    {
        //
    }

    @Override
    public void noteDragged(MouseEvent e, NoteView nv)
    {
    }

    @Override
    public void noteReleased(MouseEvent e, NoteView nv)
    {
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
        return true;
    }

    @Override
    public void editMultipleNotes(List<NoteView> noteViews)
    {
        eraseNotes(editor, noteViews);
    }
    
    @Override
    public String toString()
    {
        return"EraserTool";
    }

    /**
     * Delete the specified notes as an undoable action.
     *
     * @param editor
     * @param noteViews
     */
    static public void eraseNotes(PianoRollEditor editor, List<NoteView> noteViews)
    {
        String undoText = ResUtil.getString(EraserTool.class, "EraseNote");
        editor.getUndoManager().startCEdit(editor, undoText);

        editor.getModel().removeAll(NoteView.getNotes(noteViews), false);

        editor.getUndoManager().endCEdit(undoText);
    }

    // =============================================================================================
    // Private methods
    // =============================================================================================   

}

/*
 * 
 *   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 *   Copyright @2019 Jerome Lelasseux. All rights reserved.
 * 
 *   This file is part of the JJazzLab software.
 *    
 *   JJazzLab is free software: you can redistribute it and/or modify
 *   it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *   as published by the Free Software Foundation, either version 3 of the License, 
 *   or (at your option) any later version.
 * 
 *   JJazzLab is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *  
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *  
 *   Contributor(s): 
 * 
 */
package org.jjazz.pianoroll;

import java.util.List;
import javax.swing.JPanel;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.pianoroll.api.NoteView;

/**
 * A view or edit panel used by the editor.
 * <p>
 * The main EditorPanel is notesPanel, others are e.g. VelocityPanel or ScorePanel.
 * <p>
 */
public abstract class EditorPanel extends JPanel
{

    abstract public void cleanup();

    /**
     * Set the X scale factor.
     * <p>
     *
     * @param factorX A value &gt; 0
     */
    abstract public void setScaleFactorX(float factorX);

    /**
     * Get the current scale factor on the X axis.
     *
     * @return
     */
    abstract public float getScaleFactorX();
    
    /**
     * Show the playback point.
     * 
     * @param xPos If &lt; 0 do not show anything
     */
    abstract public void showPlaybackPoint(int xPos);

    /**
     * Get the NoteView corresponding to ne.
     *
     * @param ne Must be a ne added via addNoteView(NoteEvent ne)
     * @return Can be null
     */
    abstract public NoteView getNoteView(NoteEvent ne);

    /**
     * The NoteViews sorted by NoteEvent natural order.
     *
     * @return
     */
    abstract public List<NoteView> getNoteViews();

    /**
     * Replace the model of an existing NoteView.
     * <p>
     * Caller is responsible of calling revalidate() and/or repaint() after as needed.
     *
     * @param oldNe
     * @param newNe
     */
    abstract public void setNoteViewModel(NoteEvent oldNe, NoteEvent newNe);


    /**
     * Create and add a NoteView for the specified NoteEvent.
     * <p>
     * Caller is responsible to call revalidate() and/or repaint() after, if required.
     *
     * @param ne
     * @return
     */
    abstract public NoteView addNoteView(NoteEvent ne);

    /**
     * Remove NoteView.
     * <p>
     * Caller must call revalidate() and/or repaint() after as needed.
     *
     * @param ne
     */
    abstract public void removeNoteView(NoteEvent ne);
}

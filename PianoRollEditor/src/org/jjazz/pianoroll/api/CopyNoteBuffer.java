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
package org.jjazz.pianoroll.api;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import org.jjazz.phrase.api.NoteEvent;

/**
 * Singleton class to manage NoteEvent copy/paste operations.
 */
public class CopyNoteBuffer
{

    static private CopyNoteBuffer INSTANCE;
    private SortedSet<NoteEvent> noteBuffer = new TreeSet<>();
    private boolean isEmpty = true;

    private CopyNoteBuffer()
    {
    }

    public static CopyNoteBuffer getInstance()
    {
        synchronized (CopyNoteBuffer.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new CopyNoteBuffer();
            }
        }
        return INSTANCE;
    }

    /**
     * Put items in the buffer in ItemMode.
     *
     * @param notes
     */
    public void copy(List<NoteEvent> notes)
    {
        Preconditions.checkNotNull(notes);
        if (notes.isEmpty())
        {
            return;
        }
        isEmpty = false;
        copyNotes(notes);
    }

    public void clear()
    {
        noteBuffer.clear();
        isEmpty = true;
    }

    /**
     * @return int The number of notes in the buffer.
     */
    public int getSize()
    {
        return noteBuffer.size();
    }

    /**
     *
     * @return Can be null if buffer is empty.
     */
    public NoteEvent getFirstNote()
    {
        return noteBuffer.first();
    }

    /**
     *
     * @return Can be null if buffer is empty.
     */
    public NoteEvent getLastNote()
    {
        return noteBuffer.last();
    }

    public boolean isEmpty()
    {
        return isEmpty;
    }

    /**
     * Return a copy of the buffer notes adjusted so that the first note is at targetStartPosition.
     * <p>
     *
     * @return Can be an empty list
     */
    public List<NoteEvent> getNotesCopy(float targetStartPosition)
    {
        ArrayList<NoteEvent> notes = new ArrayList<>();
        if (noteBuffer.isEmpty())
        {
            return notes;
        }
        float delta = targetStartPosition - getFirstNote().getPositionInBeats();
        for (var ne : noteBuffer)
        {
            notes.add(ne.getCopyPos(ne.getPositionInBeats() + delta));
        }
        return notes;
    }

    // ===========================================================================================
    // Private methods
    // ===========================================================================================    
    /**
     * Store in the buffer a clone copy of each note.
     *
     * @param notes List
     */
    private void copyNotes(List<NoteEvent> notes)
    {
        noteBuffer.clear();
        notes.forEach(ne -> noteBuffer.add(ne.clone()));
    }


}

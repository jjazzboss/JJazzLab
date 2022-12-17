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
import java.util.Collections;
import java.util.List;
import org.jjazz.phrase.api.NoteEvent;
import org.openide.util.Lookup;

/**
 * Convenience methods to handle the current selection of notes.
 */
public class NotesSelection
{

    private final List<NoteEvent> notes;

    public NotesSelection(Lookup lookup)
    {
        Preconditions.checkNotNull(lookup);

        notes = new ArrayList<>(lookup.lookupAll(NoteEvent.class));
        Collections.sort(notes);
    }

    /**
     * Get the selected notes sorted by NoteEvent natural order.
     *
     * @return
     */
    private List<NoteEvent> getNotes()
    {
        return notes;
    }

    /**
     * Unselect the current selection in the specified editor.
     *
     * @param editor
     */
    public void unselectAll(PianoRollEditor editor)
    {
        notes.forEach(n -> editor.setSelectedNote(n, false));
    }

}

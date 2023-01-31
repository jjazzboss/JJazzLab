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
import org.openide.util.Lookup;

/**
 * Convenience methods to handle the current selection of notes in a lookup.
 */
public class NotesSelection
{

    private final List<NoteView> noteViews;

    public NotesSelection(Lookup lookup)
    {
        Preconditions.checkNotNull(lookup);

        noteViews = new ArrayList<>(lookup.lookupAll(NoteView.class));
        noteViews.sort((nv1, nv2) -> nv1.getModel().compareTo(nv2.getModel()));
    }

    public int size()
    {
        return noteViews.size();
    }

    public boolean isEmpty()
    {
        return noteViews.isEmpty();
    }

    /**
     * Get the selected NoteViews sorted by NoteEvent natural order.
     *
     * @return An immutable list.
     */
    public List<NoteView> getNoteViews()
    {
        return Collections.unmodifiableList(noteViews);
    }

    /**
     * Unselect the current selection.
     */
    public void unselectAll()
    {
        noteViews.forEach(nv -> nv.setSelected(false));
    }
    
}

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
package org.jjazz.songstructure.api.event;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.jjazz.songstructure.api.SongStructure;

/**
 * An event to indicate that a high-level action, i.e. a call to a public API method that mutates the song structure, has started or is complete.
 * <p>
 * All low-level SgsChangeEvents fired by a SongStructure are always preceded and followed by start and complete SgsActionEvents. The complete SgsActionEvent
 * contains the low-level SgsChangeEvents.
 * <p>
 *
 * @see SongStructure
 */
public class SgsActionEvent extends SgsChangeEvent
{

    /**
     * This corresponds to public API methods that can mutate a SongStructure.
     */
    public enum API_ID
    {
        /**
         * data=List&lt;SongPart&gt;
         */
        AddSongParts,
        /**
         * data=List&lt;SongPart&gt;
         */
        RemoveSongParts,
        /**
         * data=List&lt;SongPart&gt;
         */
        ReplaceSongParts,
        /**
         * data=Map&lt;SongPart, Integer&gt;
         */
        ResizeSongParts,
        /**
         * data=rp
         */
        SetRhythmParameterValue,
        /**
         * Used for a mutable RP value change.
         * <p>
         * data=rp
         */
        SetRhythmParameterMutableValue,
        /**
         * data=List&lt;SongPart&gt;
         */
        setSongPartsName,
    };

    private boolean complete;
    private final List<SgsChangeEvent> subEvents;
    private final API_ID apiId;
    private final Object data;


    /**
     * Create a SgsActionEvent in started state, with no subEvents.
     *
     * @param src
     * @param apiId
     * @param data  An optional data associated to the event
     */
    public SgsActionEvent(SongStructure src, API_ID apiId, Object data)
    {
        super(src);
        Objects.requireNonNull(apiId);

        this.complete = false;
        this.apiId = apiId;
        this.data = data;
        this.subEvents = new ArrayList<>();
    }

    public API_ID getApiId()
    {
        return apiId;
    }

    /**
     * Check if complete() was called.
     *
     * @return
     * @see #complete()
     */
    public boolean isComplete()
    {
        return complete;
    }

    /**
     * Mark this SgsActionEvent as complete.
     *
     * @see #isComplete()
     */
    public void complete()
    {
        complete = true;
    }

    /**
     * Add a SgsChangeEvent to this SgsActionEvent.
     *
     * @param e Can not be a SgsActionEvent
     */
    public void addSubEvent(SgsChangeEvent e)
    {
        Objects.requireNonNull(e);
        Preconditions.checkArgument(!(e instanceof SgsActionEvent), "e=%s", e);
        subEvents.add(e);
    }

    /**
     * The lower-level SgsChangeEvents added to this instance.
     *
     * @return An unmodifiable list. Can be empty.
     */
    public List<SgsChangeEvent> getSubEvents()
    {
        return Collections.unmodifiableList(subEvents);
    }

    /**
     * Optional data associated to the event.
     * <p>
     * Each actionId can have it own kind of data. Check SongStructureImpl.java code to know which data is available.
     *
     * @return Can be null.
     */
    public Object getData()
    {
        return data;
    }


    @Override
    public String toString()
    {
        return "SgsActionEvent(" + apiId + ", complete=" + complete + ", subEvents=" + subEvents + ")";
    }
}

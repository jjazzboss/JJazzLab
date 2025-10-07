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
package org.jjazz.chordleadsheet.api.event;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;

/**
 * A special event to indicate that a high-level action, i.e. a call to a public API method that mutates the chord leadsheet, has started or is complete.
 * <p>
 * All low-level ClsChangeEvents fired by a ChordLeadSheet are always preceded and followed by start and complete ClsActionEvents. The complete ClsActionEvent
 * contains the low-level ClsChangeEvents.
 * <p>
 *
 * @see ChordLeadSheet
 */
public class ClsActionEvent extends ClsChangeEvent
{

    /**
     * This corresponds to public API methods that can mutate a ChordLeadSheet.
     */
    public enum API_ID
    {
        /**
         * data=cli
         */
        AddItem,
        /**
         * data=cli
         */
        RemoveItem,
        /**
         * data=cli
         */
        MoveItem,
        /**
         * data=cli
         */
        ChangeItem,
        /**
         * data=cliSection
         */
        AddSection,
        /**
         * data=cliSection
         */
        RemoveSection,
        /**
         * data=cliSection
         */
        MoveSection,
        /**
         * data=cliSection
         */
        SetSectionName,
        /**
         * data=cliSection
         */
        SetSectionTimeSignature,
        /**
         * data=barIndex
         */
        DeleteBars,
        /**
         * data=barIndex
         */
        InsertBars,
        /**
         * data=oldSize
         */
        SetSizeInBars
    };
    private boolean complete;
    private final List<ClsChangeEvent> subEvents;
    private final API_ID apiId;
    private final Object data;

    /**
     * Create a ClsActionEvent in started state, with no subEvents.
     *
     * @param src
     * @param apiId
     * @param data     An optional data associated to the event
     */
    public ClsActionEvent(ChordLeadSheet src, API_ID apiId, Object data)
    {
        super(src);
        Objects.requireNonNull(apiId);

        this.complete = false;
        this.apiId = apiId;
        this.data = data;
        this.subEvents = new ArrayList<>();
    }

    /**
     * An optional data associated to the event.
     * <p>
     * Check the source code to know which object is associated to which actionId.
     *
     * @return Can be null
     */
    public Object getData()
    {
        return data;
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
     * Mark this ClsActionEvent as complete.
     *
     * @see #isComplete()
     */
    public void complete()
    {
        complete = true;
    }

    /**
     * Add a ClsChangeEvent to this ClsActionEvent.
     *
     * @param e Can not be a ClsActionEvent
     */
    public void addSubEvent(ClsChangeEvent e)
    {
        Objects.requireNonNull(e);
        Preconditions.checkArgument(!(e instanceof ClsActionEvent), "e=%s", e);
        subEvents.add(e);
    }

    /**
     * The lower-level ClsChangeEvents added to this instance.
     *
     * @return An unmodifiable list. Can be empty.
     */
    public List<ClsChangeEvent> getSubEvents()
    {
        return Collections.unmodifiableList(subEvents);
    }

    public API_ID getApiId()
    {
        return apiId;
    }

    @Override
    public String toString()
    {
        return "ClsActionEvent(" + apiId + ", complete=" + complete + ", subEvents=" + subEvents + ")";
    }
}

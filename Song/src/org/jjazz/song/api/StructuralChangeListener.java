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
package org.jjazz.song.api;

import static com.google.common.base.Preconditions.checkNotNull;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.ClsChangeListener;
import org.jjazz.leadsheet.chordleadsheet.api.Section;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.leadsheet.chordleadsheet.api.event.ClsActionEvent;
import org.jjazz.leadsheet.chordleadsheet.api.event.ClsChangeEvent;
import org.jjazz.leadsheet.chordleadsheet.api.event.ItemAddedEvent;
import org.jjazz.leadsheet.chordleadsheet.api.event.ItemBarShiftedEvent;
import org.jjazz.leadsheet.chordleadsheet.api.event.ItemChangedEvent;
import org.jjazz.leadsheet.chordleadsheet.api.event.ItemMovedEvent;
import org.jjazz.leadsheet.chordleadsheet.api.event.ItemRemovedEvent;
import org.jjazz.leadsheet.chordleadsheet.api.event.SectionMovedEvent;
import org.jjazz.leadsheet.chordleadsheet.api.event.SizeChangedEvent;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.songstructure.api.SgsChangeListener;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.event.SgsActionEvent;
import org.jjazz.songstructure.api.event.SgsChangeEvent;

/**
 * A song listener to detect a structural change in the song: bar added or removed, time signature changed, section moved.
 * <p>
 * Chord symbols changes or rhythm parameter changes are not structural changes.
 * <p>
 * The listener automatically unregisters itself if song is closed.
 */
public class StructuralChangeListener implements ClsChangeListener, SgsChangeListener, PropertyChangeListener
{

    private final Song song;
    private final ChordLeadSheet cls;
    private final SongStructure sgs;
    private ChangeListener changedListener;

    /**
     * Create a listener for structural changes in the song.
     *
     * @param song
     * @param listener Called when a structural change occured.
     */
    public StructuralChangeListener(Song song, ChangeListener listener)
    {
        checkNotNull(song);
        checkNotNull(listener);
        this.song = song;
        this.cls = song.getChordLeadSheet();
        this.cls.addClsChangeListener(this);
        this.sgs = song.getSongStructure();
        this.sgs.addSgsChangeListener(song);
        this.changedListener = listener;
    }

    public void cleanup()
    {
        changedListener = null;
        song.removePropertyChangeListener(this);
        cls.removeClsChangeListener(this);
        sgs.removeSgsChangeListener(song);

    }

    // ====================================================================================
    // ClsChangeListener
    // ====================================================================================    
    @Override
    public void authorizeChange(ClsChangeEvent e) throws UnsupportedEditException
    {
        // Nothing
    }

    @Override
    public void chordLeadSheetChanged(ClsChangeEvent event)
    {
        boolean changed = false;

        if (event instanceof ClsActionEvent && ((ClsActionEvent) event).isActionComplete())
        {
            String actionId = ((ClsActionEvent) event).getActionId();
            switch (actionId)
            {
                case "addItem":
                case "removeItem":
                case "setSectionName":
                    changed = false;
                    break;
                default:
                    changed = true;
            }
        }

        if (changed)
        {
            fireChanged();
        }
    }

    // ====================================================================================
    // SgsChangeListener
    // ====================================================================================    
    @Override
    public void authorizeChange(SgsChangeEvent e) throws UnsupportedEditException
    {
        // Nothing
    }

    @Override
    public void songStructureChanged(SgsChangeEvent event)
    {
        boolean changed = false;

        if (event instanceof SgsActionEvent && ((SgsActionEvent) event).isActionComplete())
        {
            String actionId = ((SgsActionEvent) event).getActionId();
            switch (actionId)
            {
                case "replaceSongParts":
                case "setRhythmParameterValue":
                case "setSongPartsName":
                    changed = false;
                    break;
                default:
                    changed = true;
            }
        }

        if (changed)
        {
            fireChanged();
        }
    }

    // ====================================================================================
    // PropertyChangeListener
    // ====================================================================================   
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getSource() == song && evt.getPropertyName().equals(Song.PROP_CLOSED))
        {
            cleanup();
        }
    }

    // ====================================================================================
    // Private methods
    // ====================================================================================   

    private void fireChanged()
    {
        changedListener.stateChanged(new ChangeEvent(song));
    }
}

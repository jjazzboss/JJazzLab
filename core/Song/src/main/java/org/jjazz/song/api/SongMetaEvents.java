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
package org.jjazz.song.api;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.chordleadsheet.api.ClsChangeListener;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.event.ClsActionEvent;
import static org.jjazz.chordleadsheet.api.event.ClsActionEvent.API_ID.AddSection;
import static org.jjazz.chordleadsheet.api.event.ClsActionEvent.API_ID.MoveSection;
import static org.jjazz.chordleadsheet.api.event.ClsActionEvent.API_ID.RemoveSection;
import static org.jjazz.chordleadsheet.api.event.ClsActionEvent.API_ID.SetSectionTimeSignature;
import org.jjazz.chordleadsheet.api.event.ClsChangeEvent;
import org.jjazz.songstructure.api.SgsChangeListener;
import org.jjazz.songstructure.api.event.SgsActionEvent;
import org.jjazz.songstructure.api.event.SgsChangeEvent;

/**
 * A helper class which listens to lower-level Song/ChordLeadSheet/SongStructure change events to provide higher-level song change events.
 * <p>
 * Whenever possible change events are fired when all low-level changes are complete, in order to minimize the number of fired events.
 */
public class SongMetaEvents implements ClsChangeListener, SgsChangeListener, PropertyChangeListener, VetoableChangeListener
{

    /**
     * Combine one source ClsActionEvent and its related SgsActionEvent sub-events.
     * <p>
     * Using a ChordLeadSheet API method will often impact the SongStructure as well, possibly with several sub-changes: e.g. reducing ChordLeadSheet size in
     * bars might remove and resize SongParts in the SongStructure.
     */
    public record ClsSourceActionEvent(ClsActionEvent clsSourceEvent, List<SgsActionEvent> sgsSubEvents)
            {

        public ClsSourceActionEvent 
        {
            Objects.requireNonNull(clsSourceEvent);
            Objects.requireNonNull(sgsSubEvents);
        }
    }

    /**
     * Combine one source SgsActionEvent and its related ClsActionEvent sub-events.
     * <p>
     * Using a SongStructure API might impact in rare cases the ChordLeadSheet as well, possibly with several sub-changes: e.g. switching to a swing-feel rhythm
     * in the SongStructure might change the position of chords in the ChordLeadSheet.
     */
    public record SgsSourceActionEvent(SgsActionEvent sgsSourceEvent, List<ClsActionEvent> clsSubEvents)
            {

        public SgsSourceActionEvent 
        {
            Objects.requireNonNull(sgsSourceEvent);
            Objects.requireNonNull(clsSubEvents);
        }
    }

    /**
     * Fired when the song modification initiated by a ChordLeadSheet or SongStructure or Song API method is complete.
     * <p>
     * oldValue=the Song property name at the origin of the change, or null<br>
     * newValue=a ClsSourceActionEvent or SgsSourceActionEvent, or null if it was a song property change<br>
     */
    public static final String PROP_SONG_API_CHANGE_COMPLETE = "PropSongAPIChangeComplete";

    /**
     * Fired when the "musical content" of the song is modified, i.e. any related music generation process should be updated or restarted.
     * <p>
     * Source changes might contain e.g. chord symbol changes, inserted bars, rhythm parameter value changes -but not a section name change. Because a rhythm
     * generation engine might adjust the generated music to the tempo, a song tempo change is considered as a musical content change.<p>
     * <p>
     * OldValue=the Song property name or the source ClsActionEvent/SgsActionEvent that initially triggered the musical change.<br>
     * NewValue=the optional associated data
     */
    public static final String PROP_MUSICAL_CONTENT = "PropMusicalContent";

    /**
     * Fired when at least one song bar was added/removed/moved, or a time signature was changed.
     * <p>
     * OldValue=the source ClsActionEvent or SgsActionEvent that initially triggered the change.<br>
     * NewValue=the optional associated data
     */
    public static final String PROP_BAR_BEAT_SEQUENCE = "PropBarBeatSequence";

    private static final Map<Song, SongMetaEvents> MAP_SONG_INSTANCE = new HashMap<>();

    static public SongMetaEvents getInstance(Song song)
    {
        var inst = MAP_SONG_INSTANCE.get(song);
        if (inst == null)
        {
            inst = new SongMetaEvents(song);
            MAP_SONG_INSTANCE.put(song, inst);
        }
        return inst;
    }

    private final Song song;
    private ClsSourceActionEvent activeClsSourceActionEvent;
    private SgsSourceActionEvent activeSgsSourceActionEvent;
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(SongMetaEvents.class.getSimpleName());

    private SongMetaEvents(Song song)
    {
        this.song = song;
        this.song.addPropertyChangeListener(this);
        this.song.addVetoableChangeListener(this);
    }

    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(l);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(propertyName, l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(propertyName, l);
    }

    //-----------------------------------------------------------------------
    // PropertiesListener interface
    //-----------------------------------------------------------------------

    @Override
    public void propertyChange(PropertyChangeEvent e)
    {
        LOGGER.log(Level.FINE, "propertyChange() source={0} prop={1} newValue={2}", new Object[]
        {
            e.getSource().getClass(), e.getPropertyName(), e.getNewValue()
        });

        if (e.getSource() == song)
        {
            switch (e.getPropertyName())
            {
                case Song.PROP_CLOSED ->
                {
                    cleanup();
                }
                case Song.PROP_TEMPO ->
                {
                    fireSongAPIChangeComplete(Song.PROP_TEMPO);
                    fireMusicalContentChanged(Song.PROP_TEMPO, e.getOldValue());
                }
                default ->
                {
                    // Nothing
                }
            }
        }
    }


    //-----------------------------------------------------------------------
    // VetoableChangeListener interface
    //-----------------------------------------------------------------------
    @Override
    public void vetoableChange(PropertyChangeEvent e) throws PropertyVetoException
    {
        LOGGER.log(Level.FINE, "vetoableChange() src={0} prop={1} oldValue={2} newValue={3}", new Object[]
        {
            e.getSource().getClass(), e.getPropertyName(), e.getOldValue(), e.getNewValue()
        });
        assert e.getSource() == song;
        switch (e.getPropertyName())
        {
            case Song.PROP_VETOABLE_USER_PHRASE ->
            {
                fireSongAPIChangeComplete(Song.PROP_VETOABLE_USER_PHRASE);
                String addedOrRemovedPhraseName = e.getOldValue() == null ? e.getNewValue().toString() : e.getOldValue().toString();
                fireMusicalContentChanged(Song.PROP_VETOABLE_USER_PHRASE, addedOrRemovedPhraseName);
            }
            case Song.PROP_VETOABLE_USER_PHRASE_CONTENT ->
            {
                fireSongAPIChangeComplete(Song.PROP_VETOABLE_USER_PHRASE_CONTENT);
                fireMusicalContentChanged(Song.PROP_VETOABLE_USER_PHRASE_CONTENT, e.getNewValue().toString());
            }
            default ->
            {
                // Nothing
            }
        }
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

        if (!(event instanceof ClsActionEvent))
        {
            return;
        }
        var cae = (ClsActionEvent) event;

        if (activeSgsSourceActionEvent != null)
        {
            // Save sub-events of the active SgsSourceActionEvent
            if (cae.isComplete())
            {
                activeSgsSourceActionEvent.clsSubEvents().add(cae);
            }
        } else if (activeClsSourceActionEvent == null)
        {
            // No active SourceActionEvent
            if (!cae.isComplete())
            {
                activeClsSourceActionEvent = new ClsSourceActionEvent(cae, new ArrayList<>());
            }
        } else if (!cae.isComplete())
        {
            throw new IllegalStateException("activeClsSourceActionEvent=" + activeClsSourceActionEvent + " cae=" + cae);
        } else
        {
            // cae is complete, this is the end of our ClsSourceActionEvent
            assert activeClsSourceActionEvent.clsSourceEvent().getApiId() == cae.getApiId() :
                    "activeClsSourceActionEvent=" + activeClsSourceActionEvent + " cae=" + cae;
            fireSongAPIChangeComplete(activeClsSourceActionEvent);
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
        if (!(event instanceof SgsActionEvent))
        {
            return;
        }
        var sae = (SgsActionEvent) event;

        if (activeClsSourceActionEvent != null)
        {
            // Save sub-events of the active ClsSourceActionEvent
            if (sae.isComplete())
            {
                activeClsSourceActionEvent.sgsSubEvents().add(sae);
            }
        } else if (activeSgsSourceActionEvent == null)
        {
            // No active SourceActionEvent
            if (!sae.isComplete())
            {
                activeSgsSourceActionEvent = new SgsSourceActionEvent(sae, new ArrayList<>());
            }
        } else if (!sae.isComplete())
        {
            throw new IllegalStateException("activeSgsSourceActionEvent=" + activeSgsSourceActionEvent + " sae=" + sae);
        } else
        {
            // sae is complete, this is the end of our SgsSourceActionEvent
            assert activeSgsSourceActionEvent.sgsSourceEvent().getApiId() == sae.getApiId() :
                    "activeSgsSourceActionEvent=" + activeSgsSourceActionEvent + " sae=" + sae;
            fireSongAPIChangeComplete(activeSgsSourceActionEvent);
        }
    }
    // ===================================================================================================================
    // Private methods
    // ===================================================================================================================    

    /**
     * Fire a PROP_MUSICAL_CONTENT property change event.
     *
     * @param songPropOrClsOrSgsActionEvent
     * @param data
     */
    private void fireMusicalContentChanged(Object songPropOrClsOrSgsActionEvent, Object data)
    {
        pcs.firePropertyChange(PROP_MUSICAL_CONTENT, songPropOrClsOrSgsActionEvent, data);
    }

    /**
     * Fire a PROP_BAR_BEAT_SEQUENCE property change event.
     *
     * @param songPropOrClsOrSgsActionEvent
     * @param data
     */
    private void fireBarBeatSequenceChanged(Object songPropOrClsOrSgsActionEvent, Object data)
    {
        pcs.firePropertyChange(PROP_BAR_BEAT_SEQUENCE, songPropOrClsOrSgsActionEvent, data);
    }

    private void fireSongAPIChangeComplete(String songPropertyName)
    {
        Objects.requireNonNull(songPropertyName);
        assert activeClsSourceActionEvent == null;
        assert activeSgsSourceActionEvent == null;
        pcs.firePropertyChange(PROP_SONG_API_CHANGE_COMPLETE, songPropertyName, null);
    }

    private void fireSongAPIChangeComplete(ClsSourceActionEvent csae)
    {
        Objects.requireNonNull(csae);
        pcs.firePropertyChange(PROP_SONG_API_CHANGE_COMPLETE, null, csae);
        activeClsSourceActionEvent = null;


        // We can fire other events
        boolean musicalContentChanged = false;
        boolean barBeatSequenceChanged = false;
        switch (csae.clsSourceEvent().getApiId())
        {
            case SetSectionName ->
            {
            }
            case SetSectionTimeSignature ->
            {
                musicalContentChanged = true;
                barBeatSequenceChanged = true;
            }
            case AddSection, RemoveSection, MoveSection ->
            {
                musicalContentChanged = true;       // actually could be false in specific cases, depends on time signature changes and how songparts are impacted
                barBeatSequenceChanged = true;      // idem
            }
            case AddItem, RemoveItem, ChangeItem, MoveItem ->
            {
                musicalContentChanged = true;
            }
            case DeleteBars, InsertBars, SetSizeInBars ->
            {
                musicalContentChanged = true;
                barBeatSequenceChanged = true;
            }
            default -> throw new AssertionError(csae.clsSourceEvent().getApiId());
        }

        if (musicalContentChanged)
        {
            fireMusicalContentChanged(csae, csae.clsSourceEvent().getData());
        }

        if (barBeatSequenceChanged)
        {
            fireBarBeatSequenceChanged(csae, csae.clsSourceEvent().getData());
        }

    }

    private void fireSongAPIChangeComplete(SgsSourceActionEvent ssae)
    {
        Objects.requireNonNull(ssae);
        pcs.firePropertyChange(PROP_SONG_API_CHANGE_COMPLETE, null, ssae);
        activeSgsSourceActionEvent = null;


        // We can fire other events
        boolean musicalContentChanged = false;
        boolean barBeatSequenceChanged = false;
        switch (ssae.sgsSourceEvent().getApiId())
        {

            case SetSectionName ->
            {
            }
            case SetSectionTimeSignature ->
            {
                musicalContentChanged = true;
                barBeatSequenceChanged = true;
            }
            case AddSection, RemoveSection, MoveSection ->
            {
                musicalContentChanged = true;       // actually could be false in specific cases, depends on time signature changes and how songparts are impacted
                barBeatSequenceChanged = true;      // idem
            }
            case AddItem, RemoveItem, ChangeItem, MoveItem ->
            {
                musicalContentChanged = true;
            }
            case DeleteBars, InsertBars, SetSizeInBars ->
            {
                musicalContentChanged = true;
                barBeatSequenceChanged = true;
            }
            default -> throw new AssertionError(ssae.sgsSourceEvent().getApiId());
        }

        if (musicalContentChanged)
        {
            fireMusicalContentChanged(ssae, ssae.sgsSourceEvent().getData());
        }
        if (barBeatSequenceChanged)
        {
            fireBarBeatSequenceChanged(ssae, ssae.sgsSourceEvent().getData());
        }
    }

    private void cleanup()
    {
        song.removePropertyChangeListener(this);
        song.removeVetoableChangeListener(this);
    }

}

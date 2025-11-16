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
import java.util.Collections;
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
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.songstructure.api.SgsChangeListener;
import org.jjazz.songstructure.api.event.SgsActionEvent;
import static org.jjazz.songstructure.api.event.SgsActionEvent.API_ID.SetRhythmParameterMutableValue;
import org.jjazz.songstructure.api.event.SgsChangeEvent;

/**
 * A helper class which listens to lower-level Song/ChordLeadSheet/SongStructure change events to provide higher-level song change events.
 * <p>
 * Whenever possible change events are fired when all low-level changes are complete, in order to minimize the number of fired events.
 */
public class SongMetaEvents implements ClsChangeListener, SgsChangeListener, PropertyChangeListener, VetoableChangeListener
{


    /**
     * Fired when the song modification initiated by a ChordLeadSheet/SongStructure API method is complete.
     * <p>
     * OldValue=the source ClsSourceActionEvent/SgsSourceActionEvent that initially triggered the change.<br>
     * NewValue=the optional associated data
     */
    public static final String PROP_CLS_SGS_API_CHANGE_COMPLETE = "PropClsSgsAPIChangeComplete";

    /**
     * Fired when the "musical content" of the song is modified, i.e. any related music generation process should be updated or restarted.
     * <p>
     * Source changes might contain e.g. chord symbol changes, inserted bars, rhythm parameter value changes -but not a section name change. Because a rhythm
     * generation engine might adjust the generated music to the tempo, a song tempo change is considered as a musical content change.<p>
     * <p>
     * OldValue=the Song property name or the source ClsSourceActionEvent/SgsSourceActionEvent that initially triggered the musical change.<br>
     * NewValue=the optional associated data
     */
    public static final String PROP_MUSIC_GENERATION = "PropMusicGeneration";

    /**
     * Fired when at least one song bar was added/removed/moved, or a time signature was changed.
     * <p>
     * OldValue=the source ClsSourceActionEvent/SgsSourceActionEvent that initially triggered the change.<br>
     * NewValue=the optional associated data
     */
    public static final String PROP_SONG_STRUCTURE = "PropSongStructure";

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
        this.song.getChordLeadSheet().addClsChangeListener(this);
        this.song.getSongStructure().addSgsChangeListener(this);
    }

    public Song getSong()
    {
        return song;
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
                String addedOrRemovedPhraseName = e.getOldValue() == null ? e.getNewValue().toString() : e.getOldValue().toString();
                fireMusicalContentChanged(Song.PROP_VETOABLE_USER_PHRASE, addedOrRemovedPhraseName);
            }
            case Song.PROP_VETOABLE_USER_PHRASE_CONTENT ->
            {
                String phraseName = e.getNewValue().toString();
                fireMusicalContentChanged(Song.PROP_VETOABLE_USER_PHRASE_CONTENT, phraseName);
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
    public void chordLeadSheetChanged(ClsChangeEvent event) throws UnsupportedEditException
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
                activeSgsSourceActionEvent.addClsSubEvent(cae);
            }
        } else if (activeClsSourceActionEvent == null)
        {
            // No active SourceActionEvent
            if (!cae.isComplete())
            {
                activeClsSourceActionEvent = new ClsSourceActionEvent(cae);
            }
        } else if (!cae.isComplete())
        {
            throw new IllegalStateException("activeClsSourceActionEvent=" + activeClsSourceActionEvent + " cae=" + cae);
        } else
        {
            // cae is complete, this is the end of our ClsSourceActionEvent
            assert activeClsSourceActionEvent.getApiId() == cae.getApiId() :
                    "activeClsSourceActionEvent=" + activeClsSourceActionEvent + " cae=" + cae;
            fireSongAPIChangeComplete(activeClsSourceActionEvent);
        }


    }

    // ====================================================================================
    // SgsChangeListener
    // ====================================================================================    
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
                activeClsSourceActionEvent.addSgsSubEvent(sae);
            }
        } else if (activeSgsSourceActionEvent == null)
        {
            // No active SourceActionEvent
            if (!sae.isComplete())
            {
                activeSgsSourceActionEvent = new SgsSourceActionEvent(sae);
            }
        } else if (!sae.isComplete())
        {
            throw new IllegalStateException("activeSgsSourceActionEvent=" + activeSgsSourceActionEvent + " sae=" + sae);
        } else
        {
            // sae is complete, this is the end of our SgsSourceActionEvent
            assert activeSgsSourceActionEvent.getApiId() == sae.getApiId() :
                    "activeSgsSourceActionEvent=" + activeSgsSourceActionEvent + " sae=" + sae;
            fireSongAPIChangeComplete(activeSgsSourceActionEvent);
        }
    }
    // ===================================================================================================================
    // Private methods
    // ===================================================================================================================    

    private void fireMusicalContentChanged(Object src, Object data)
    {
        assert src instanceof String || src instanceof ClsSourceActionEvent || src instanceof SgsSourceActionEvent;
        pcs.firePropertyChange(PROP_MUSIC_GENERATION, src, data);
    }

    private void fireBarBeatSequenceChanged(Object src, Object data)
    {
        assert src instanceof ClsSourceActionEvent || src instanceof SgsSourceActionEvent;
        pcs.firePropertyChange(PROP_SONG_STRUCTURE, src, data);
    }

    private void fireSongAPIChangeComplete(ClsSourceActionEvent csae)
    {
        Objects.requireNonNull(csae);
        pcs.firePropertyChange(PROP_CLS_SGS_API_CHANGE_COMPLETE, csae, csae.getData());
        activeClsSourceActionEvent = null;


        // We can fire other events
        boolean musicalContentChanged = false;
        boolean barBeatSequenceChanged = false;
        switch (csae.getApiId())
        {
            case SetSectionName ->
            {
            }
            case SetSectionTimeSignature ->
            {
                musicalContentChanged = true;
                barBeatSequenceChanged = true;
            }
            case AddSection, RemoveSection ->
            {
                musicalContentChanged = !csae.sgsSubEvents.isEmpty();       // Change if SongStructure was impacted
                barBeatSequenceChanged = musicalContentChanged;
            }
            case MoveSection ->
            {
                musicalContentChanged = true;
                barBeatSequenceChanged = true;
            }
            case AddItem, RemoveItem, ChangeItem ->
            {
                // There is a music change only if a SongPart is impacted
                ChordLeadSheetItem<?> item = (ChordLeadSheetItem<?>) csae.getData();
                var cliSection = song.getChordLeadSheet().getSection(item.getPosition().getBar());
                musicalContentChanged = song.getSongStructure().getSongParts().stream().anyMatch(spt -> spt.getParentSection() == cliSection);
            }
            case MoveItem ->
            {
                musicalContentChanged = true;
            }
            case DeleteBars, InsertBars, SetSizeInBars ->
            {
                musicalContentChanged = true;
                barBeatSequenceChanged = true;
            }
            default -> throw new AssertionError(csae.getApiId());
        }

        if (musicalContentChanged)
        {
            fireMusicalContentChanged(csae, csae.getData());
        }

        if (barBeatSequenceChanged)
        {
            fireBarBeatSequenceChanged(csae, csae.getData());
        }

    }

    private void fireSongAPIChangeComplete(SgsSourceActionEvent ssae)
    {
        Objects.requireNonNull(ssae);
        pcs.firePropertyChange(PROP_CLS_SGS_API_CHANGE_COMPLETE, ssae, ssae.getData());
        activeSgsSourceActionEvent = null;


        // We can fire other events
        boolean musicalContentChanged = false;
        boolean barBeatSequenceChanged = false;
        switch (ssae.getApiId())
        {
            case AddSongParts, RemoveSongParts, ReplaceSongParts, ResizeSongParts ->
            {
                musicalContentChanged = true;
                barBeatSequenceChanged = true;
            }
            case SetRhythmParameterValue, SetRhythmParameterMutableValue ->
            {
                musicalContentChanged = true;
            }
            case setSongPartsName ->
            {
            }
            default -> throw new AssertionError(ssae.getApiId());
        }

        if (musicalContentChanged)
        {
            fireMusicalContentChanged(ssae, ssae.getData());
        }
        if (barBeatSequenceChanged)
        {
            fireBarBeatSequenceChanged(ssae, ssae.getData());
        }
    }

    private void cleanup()
    {
        song.removePropertyChangeListener(this);
        song.removeVetoableChangeListener(this);
        song.getChordLeadSheet().removeClsChangeListener(this);
        song.getSongStructure().removeSgsChangeListener(this);
    }

    // ========================================================================================================
    // Inner classes
    // ========================================================================================================

    /**
     * A special ClsActionEvent which stores its related SgsActionEvent sub-events.
     * <p>
     * Using a ChordLeadSheet API method will often impact the SongStructure as well, possibly with several sub-changes: e.g. reducing ChordLeadSheet size in
     * bars might remove and resize SongParts in the SongStructure. This class allows to save most of this information (other possible source is
     * JJazzUndoManager).
     */
    public static class ClsSourceActionEvent extends ClsActionEvent
    {

        private final List<SgsActionEvent> sgsSubEvents = new ArrayList<>();

        public ClsSourceActionEvent(ClsActionEvent cae)
        {
            super(cae.getSource(), cae.getApiId(), cae.getData());
            cae.getSubEvents().forEach(e -> addSubEvent(e));
        }

        public void addSgsSubEvent(SgsActionEvent sae)
        {
            Objects.requireNonNull(sae);
            sgsSubEvents.add(sae);
        }

        public List<SgsActionEvent> getSgsSubEvents()
        {
            return Collections.unmodifiableList(sgsSubEvents);
        }
    }

    /**
     * A special SgsActionEvent which stores its related ClsActionEvent sub-events.
     * <p>
     * Using a SongStructure API might impact in rare cases the ChordLeadSheet as well, possibly with several sub-changes: e.g. switching to a swing-feel rhythm
     * in the SongStructure might change the position of chords in the ChordLeadSheet. This class allows to save most of this information (other possible source
     * is JJazzUndoManager).
     */
    public static class SgsSourceActionEvent extends SgsActionEvent
    {

        private final List<ClsActionEvent> clsSubEvents = new ArrayList<>();

        public SgsSourceActionEvent(SgsActionEvent sae)
        {
            super(sae.getSource(), sae.getApiId(), sae.getData());
            sae.getSubEvents().forEach(e -> addSubEvent(e));
        }

        public void addClsSubEvent(ClsActionEvent sae)
        {
            Objects.requireNonNull(sae);
            clsSubEvents.add(sae);
        }

        public List<ClsActionEvent> getClsSubEvents()
        {
            return Collections.unmodifiableList(clsSubEvents);
        }
    }
}

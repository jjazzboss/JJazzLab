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

import com.google.common.base.Preconditions;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.chordleadsheet.api.ClsChangeListener;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.event.ClsChangeEvent;
import org.jjazz.chordleadsheet.api.event.ItemAddedEvent;
import org.jjazz.chordleadsheet.api.event.ItemChangedEvent;
import org.jjazz.chordleadsheet.api.event.ItemMovedEvent;
import org.jjazz.chordleadsheet.api.event.ItemRemovedEvent;
import org.jjazz.songstructure.api.SgsChangeListener;
import org.jjazz.songstructure.api.event.RpValueChangedEvent;
import org.jjazz.songstructure.api.event.SgsChangeEvent;
import org.jjazz.songstructure.api.event.SptRenamedEvent;
import org.jjazz.songstructure.api.event.SptRhythmChangedEvent;

/**
 * A helper class which listens to Song/ChordLeadSheet/SongStructure change events to provide higher-level property change events.
 * <p>
 */
public class SongMetaEvents implements ClsChangeListener, SgsChangeListener, PropertyChangeListener
{

    /**
     * Fired when the song's ChordLeadSheet or SongStructure has changed.
     * <p>
     * OldValue=the source ClsChangeEvent or SgsChangeEvent
     */
    public static final String PROP_CLS_SGS_CHANGE = "PropClsSgsChange";

    /**
     * Fired when the "musical content" of the song is modified, i.e. a new music generation task is needed.
     * <p>
     * For example source changes can be chord symbol changes, inserted bars, rhythm parameter value changes -but not a section name change. Because a rhythm
     * generation engine might adjust the generated music to the tempo, a song tempo change is considered as a musical content change.<p>
     * <p>
     * OldValue=the source Song's PropertyChangeEvent or ClsChangeEvent or SgsChangeEvent<br>
     */
    public static final String PROP_MUSIC_GENERATION = "PropMusicalContent";

    /**
     * Fired when at least one song bar was added/removed/moved, or a time signature was changed.
     * <p>
     * OldValue=the source ClsChangeEvent or SgsChangeEvent
     */
    public static final String PROP_SIZE_IN_BEATS = "PropSizeInBeats";


    private static final Map<Song, SongMetaEvents> MAP_SONG_INSTANCE = new IdentityHashMap<>();

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
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(SongMetaEvents.class.getSimpleName());

    private SongMetaEvents(Song song)
    {
        this.song = song;
        this.song.addPropertyChangeListener(this);
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
    // PropertyChangeListener interface
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
                case Song.PROP_TEMPO, Song.PROP_USER_PHRASE, Song.PROP_USER_PHRASE_CONTENT ->
                {
                    fireMusicalContentChanged(e);
                }
                default ->
                {
                    // Nothing
                }
            }
        }
    }

    // ====================================================================================
    // ClsChangeListener
    // ====================================================================================    
    @Override
    public void chordLeadSheetChanged(ClsChangeEvent event) throws UnsupportedEditException
    {
        // Many ClsChangeEvents will trigger a SongStructure event, we'll catch them in songStructureChanged()
        // So we focus here only on the events which do not trigger a SongStructure change
        boolean musicalContentChanged = switch (event)
        {
            case ItemAddedEvent e ->
                true;
            case ItemChangedEvent e ->
                true;
            case ItemMovedEvent e ->
                true;
            case ItemRemovedEvent e ->
                true;
            default -> false;
        };

        fireClsSgsChanged(event);
        
        if (musicalContentChanged)
        {
            fireMusicalContentChanged(event);
        }
    }

    // ====================================================================================
    // SgsChangeListener
    // ====================================================================================    

    @Override
    public void songStructureChanged(SgsChangeEvent event)
    {
        boolean musicalContentChanged, sizeInBeatsChanged;

        switch (event)
        {
            case RpValueChangedEvent e ->
            {
                musicalContentChanged = true;
                sizeInBeatsChanged = false;
            }
            case SptRenamedEvent e ->
            {
                musicalContentChanged = false;
                sizeInBeatsChanged = false;
            }
            case SptRhythmChangedEvent e ->
            {
                musicalContentChanged = true;
                sizeInBeatsChanged = false;
            }
            default ->
            {
                musicalContentChanged = true;
                sizeInBeatsChanged = true;
            }
        }

        fireClsSgsChanged(event);
        if (musicalContentChanged)
        {
            fireMusicalContentChanged(event);
        }
        if (sizeInBeatsChanged)
        {
            fireSizeInBeatsChanged(event);
        }
    }
    // ===================================================================================================================
    // Private methods
    // ===================================================================================================================    

    private void fireClsSgsChanged(Object src)
    {
        Preconditions.checkArgument(src instanceof ClsChangeEvent || src instanceof SgsChangeEvent, "src=%s", src);
        pcs.firePropertyChange(PROP_CLS_SGS_CHANGE, src, null);
    }

    private void fireMusicalContentChanged(Object src)
    {
        Preconditions.checkArgument(src instanceof PropertyChangeEvent || src instanceof ClsChangeEvent || src instanceof SgsChangeEvent, "src=%s", src);
        pcs.firePropertyChange(PROP_MUSIC_GENERATION, src, null);
    }

    private void fireSizeInBeatsChanged(Object src)
    {
        Preconditions.checkArgument(src instanceof ClsChangeEvent || src instanceof SgsChangeEvent, "src=%s", src);
        pcs.firePropertyChange(PROP_SIZE_IN_BEATS, src, null);
    }


    private void cleanup()
    {
        song.removePropertyChangeListener(this);
        song.getChordLeadSheet().removeClsChangeListener(this);
        song.getSongStructure().removeSgsChangeListener(this);
        MAP_SONG_INSTANCE.remove(song);
    }

    // ========================================================================================================
    // Inner classes
    // ========================================================================================================

}

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
package org.jjazz.improvisionsupport;

import static com.google.common.base.Preconditions.checkNotNull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jjazz.harmony.api.TimeSignature;
import static org.jjazz.improvisionsupport.PlayRestScenario.Level.EASY;
import org.jjazz.song.api.Song;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.util.api.IntRange;

/**
 * A play/rest scenario for a given song.
 * <p>
 * A change event is fired each time generate() is called.
 */
public class PlayRestScenario
{

    public enum Level
    {
        EASY, MEDIUM
    };

    public enum Value
    {
        PLAY, REST
    };
    private final Level level;
    private final Song songOriginal;
    private final Set<ChangeListener> listeners = new HashSet<>();
    private List<Value> values;
    private static final Logger LOGGER = Logger.getLogger(PlayRestScenario.class.getName());

    public PlayRestScenario(Level level, Song song)
    {
        checkNotNull(song);
        this.songOriginal = song;
        this.level = level;
    }

    /**
     * Regenerate the scenario for the current context.
     * <p>
     * Fire a change event.
     *
     * @return The list of Values, one for each natural beat of the song.
     * @see #getPlayRestValues(int)
     */
    public List<Value> generate()
    {
        values = new ArrayList<>();
        var ss = songOriginal.getSongStructure();
        int sizeInBeats = (int) ss.getBeatRange(new IntRange(0, ss.getSizeInBars() - 1)).size();
        double threshold = level.equals(EASY) ? 0.8d : 0.4d;
        for (int i = 0; i < sizeInBeats; i++)
        {

            Value v = Math.random() > threshold ? Value.PLAY : Value.REST;
            values.add(v);
        }
        fireChanged();
        return values;
    }

    /**
     * Provide the Value for each logical beat of the specified song bar index.
     * <p>
     * Require that generate() was called before, if it's not the case return an empty list.
     *
     * @param barIndex
     * @return
     * @see #generate()
     */
    public List<Value> getPlayRestValues(int barIndex)
    {
        var res = new ArrayList<Value>();
        if (values == null)
        {
            return res;
        }

        SongStructure ss = songOriginal.getSongStructure();
        TimeSignature ts = ss.getSongPart(barIndex).getParentSection().getData().getTimeSignature();
        int index = (int) ss.getPositionInNaturalBeats(barIndex);
        for (int i = 0; i < ts.getNbNaturalBeats(); i++)
        {
            res.add(values.get(index + i));
        }
        return res;
    }

    public Level getLevel()
    {
        return level;
    }

    public Song getSongOriginal()
    {
        return songOriginal;
    }

    public void addChangeListener(ChangeListener l)
    {
        listeners.add(l);
    }

    public void removeChangeListener(ChangeListener l)
    {
        listeners.remove(l);
    }

    // =======================================================================
    // Private methods
    // =======================================================================    
    private void fireChanged()
    {
        var e = new ChangeEvent(this);
        listeners.forEach(l -> l.stateChanged(e));
    }
}

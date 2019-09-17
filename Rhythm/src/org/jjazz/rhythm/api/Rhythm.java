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
package org.jjazz.rhythm.api;

import java.io.File;
import org.jjazz.rhythm.parameters.RhythmParameter;
import java.util.List;
import org.jjazz.harmony.TimeSignature;
import org.openide.util.Lookup;

/**
 * A rhythm can generate music tracks for each of its supported rhythm voices.
 * <p>
 * The class is used as a descriptor of the rhythm, the actual rhythm's capabilites are MidiMusicGenerator object(s) which must be
 * put in the rhythm's lookup.<p>
 * The framework will call the rhythm's loadResources() before accessing the MidiMusicGenerator object(s). This allow to save
 * memory usage when rhythm object is only used in catalogs... (and not in an actual song to be played).
 *
 */
public interface Rhythm extends Lookup.Provider, Comparable<Rhythm>
{

    public enum Feel
    {
        TERNARY, BINARY
    };

    /**
     * Tell the rhythm it may load any memory-heavy resources.
     * <p>
     * This method will be called by the framework before using the rhythm's MidiMusicGenerator objects in its lookup.
     *
     * @return False if there was a problem loading resources.
     * @see releaseResources()
     */
    public boolean loadResources();

    /**
     * Ask the rhythm to release any memory-heavy resources.
     *
     * @see loadResources()
     */
    public void releaseResources();

    public boolean isResourcesLoaded();

    /**
     * @return The voices for which this rhythm can generate music. Each voice must have a unique name.
     */
    public List<RhythmVoice> getRhythmVoices();

    /**
     * @return The RhythmParameters that influence the way this rhythm generates music.
     */
    List<RhythmParameter<?>> getRhythmParameters();

    TimeSignature getTimeSignature();

    /**
     * Optional file from which this rhythm was loaded.
     *
     * @return Can't be null, but can be an empty path ("") if no file associated.
     */
    File getFile();

    /**
     * A unique string identifier representing this rhythm.
     * <p>
     * It will be used by other serialized objects who want to refer this rhythm -typically a Song object.
     *
     * @return A non-empty String with spaces trimmed.
     */
    String getUniqueId();

    String getDescription();

    Feel getFeel();

    TempoRange getTempoRange();

    int getPreferredTempo();

    String getName();

    String getAuthor();

    String getVersion();

    /**
     * Can be any keyword strings used to describe the rhythm.
     *
     * @return
     */
    String[] getTags();

}

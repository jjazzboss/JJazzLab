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

/**
 * A marker interface for a rhythm which is an adapted version of an existing rhythm but for a different time signature.
 * <p>
 * This is used when there is e.g. a 2/4 bar in a 4/4 song (so with a 4/4 rhythm) and we don't want to have a specific 2/4 rhythm,
 * but just the same 4/4 rhythm truncated to a 2/2 bar.<p>
 * <p>
 * An AdaptedRhythm should have only RhythmVoiceDelegate instances as RhythmVoices, so that they do not take "Midi channel space"
 * in a song's MidiMix.
 */
public interface AdaptedRhythm extends Rhythm
{

    /**
     * Delimiter to be used by getUniqueId().
     * <p>
     */
    public static final String RHYTHM_ID_DELIMITER = "___"; // No regex chars !

    /**
     * The source rhythm for this object.
     *
     * @return A rhythm with a different time signature than this rhythm.
     */
    public Rhythm getSourceRhythm();

    /**
     * A unique string identifier representing this adapted rhythm.
     * <p>
     * As an AdaptedRhythm, the returned id must follow this syntax:<br>
     * &lt;RhythmProviderId&gt&lt;RHYTHM_ID_DELIMITER&gt;&lt;RhythmId&gt;&lt;RHYTHM_ID_DELIMITER&gt;&lt;TimeSignature&gt;<br>
     * Example: "YamJJazzRhythmProviderID$@$BossaNova.s25.styID$@$3/4"<br>
     * RhythmId must a valid rhythm id for the rhythm provider identified by RhythmProviderId.
     * <p>
     * It will be used by other serialized objects who want to refer this rhythm -typically a Song object.
     *
     * @return
     */
    @Override
    String getUniqueId();

}

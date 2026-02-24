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
package org.jjazz.rhythm.api;

import java.io.Serializable;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Rhythm general features.
 */
public record RhythmFeatures(Genre genre, Division division, TempoRange tempoRange) implements Serializable
        {

    private static final long serialVersionUID = 1223380872L;
    private static final Logger LOGGER = Logger.getLogger(RhythmFeatures.class.getSimpleName());

    /**
     * Construct an object with all default values.
     * <p>
     * Values are set to UNKNOWN, except for TempoRange which is set to TempoRange.ALL_TEMPO.
     * <p>
     */
    public RhythmFeatures()
    {
        this(Genre.UNKNOWN, Division.UNKNOWN, TempoRange.ALL_TEMPO);
    }


    /**
     * Compute a matching score between this object and rf.
     * <p>
     * Score calculation on each variable:<br>
     * - Add 100 points if genre values are defined and match  <br>
     * - Add 30 points division values are defined and match<br>
     * - Add up to 30 points depending how TempoRange values match<br>
     *
     * @param rf
     * @return The matching score
     */
    public int getMatchingScore(RhythmFeatures rf)
    {
        int score = 0;

        if (genre != Genre.UNKNOWN && genre == rf.genre())
        {
            score += 100;
        }

        if (division != Division.UNKNOWN && division == rf.division())
        {
            score += 30;
        }

        if (tempoRange != TempoRange.ALL_TEMPO && rf.tempoRange != TempoRange.ALL_TEMPO)
        {
            score += Math.round(tempoRange.computeSimilarityLevel(rf.tempoRange) * 30);
        }

        return score;
    }

    /**
     * Try to guess features from a text (for example a style name).
     * <p>
     * Use defaultValues to set some values in the returned RhythmFeatures object.
     *
     * @param text  eg "Bossa Nova"
     * @param tempo Ignored if negative
     * @return If no match, all features will be UNKNOWN and TempoRange.ALL_TEMPO for the TempoRange
     */
    static public RhythmFeatures guessFeatures(String text, int tempo)
    {
        Objects.requireNonNull(text);

        var lowerCaseText = text.toLowerCase();

        Genre g = Genre.guess(lowerCaseText);
        Division d = Division.guess(g, lowerCaseText, tempo);
        TempoRange tr = tempo >= TempoRange.TEMPO_MIN ? TempoRange.getStandardTempoRange(tempo) : TempoRange.guess(g, text);
        LOGGER.log(Level.FINE, "guessFeatures() text={0} tempo={1}  =>  g={2} d={3} tr={4}", new Object[]
        {
            text, tempo, g, d, tr
        });

        return new RhythmFeatures(g, d, tr);
    }


}

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
package org.jjazz.jjswing.bass;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.harmony.api.ChordType;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.jjswing.api.BassStyle;
import static org.jjazz.jjswing.bass.BassGenerator.NON_QUANTIZED_WINDOW;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.rhythmmusicgeneration.api.SimpleChordSequence;
import org.jjazz.utilities.api.FloatRange;
import org.jjazz.utilities.api.IntRange;

/**
 * A phrase builder for BassStyle.WALKING_DOUBLE_NOTE.
 * <p>
 * Delegates to a WalkingBassPhraseBuilder then post-process the results.
 */
public class WalkingDoubleNotePhraseBuilder implements PhraseBuilder
{

    private static final BassStyle STYLE = BassStyle.WALKING_DOUBLE_NOTE;
    private static final Logger LOGGER = Logger.getLogger(WalkingDoubleNotePhraseBuilder.class.getSimpleName());

    @Override
    public Phrase build(List<SimpleChordSequence> scsList, int tempo)
    {
        LOGGER.log(PhraseBuilderLogLevel, "build() -- tempo={0} scsList={1}", new Object[]
        {
            tempo, scsList
        });


        // Delegates to the normal walking bass phrase builder
        PhraseBuilder walkingBuilder = BassStyle.WALKING.getBassPhraseBuilder();
        Phrase p = walkingBuilder.build(scsList, tempo);


        // Turn it into double notes phrase using notes from beat 0 and beat 2
        for (var scs : scsList)
        {
            var barRange = scs.getBarRange();
            for (int bar : barRange)
            {
                float barStartBeatPos = scs.getStartBeatPosition() + ((bar - barRange.from) * scs.getTimeSignature().getNbNaturalBeats());
                var barScs = scs.subSequence(new IntRange(bar, bar), true);
                if (barScs.size() > 2 || (barScs.size() == 2 && barScs.getLast().getPosition().getBeat() != 2f))
                {
                    // Non standard case, leave normal walking bass
                    continue;
                }
                var cliCs0 = barScs.getFirst();
                var cliCs2 = barScs.size() == 1 ? null : barScs.getLast();

                FloatRange fr0 = new FloatRange(Math.max(0, barStartBeatPos - NON_QUANTIZED_WINDOW), barStartBeatPos + 2 - NON_QUANTIZED_WINDOW);
                var p0 = p.subSet(fr0, true);
                if (p0.isEmpty())
                {
                    LOGGER.log(Level.WARNING, "build() No note found in first half of bar={0}", bar);
                    continue;
                }
                var ne0 = p0.first();
                float beatPos0 = ne0.getPositionInBeats() < barStartBeatPos + NON_QUANTIZED_WINDOW ? ne0.getPositionInBeats() : barStartBeatPos;
                if (cliCs0.getData().getChord().indexOfRelativePitch(ne0.getPitch()) == -1)
                {
                    // It's an unusual note, leave the normal walking bass 
                    LOGGER.log(Level.FINE, "build() bar {0}: non chordtone starting note. ne0={1}  cliCs0={2}  p0={3}", new Object[]
                    {
                        bar, ne0, cliCs0, p0
                    });
                    continue;
                }
                ne0 = ne0.setAll(-1, 0.9f, -1, beatPos0, null, false);
                var ne1 = ne0.setPosition(barStartBeatPos + 1, false);


                FloatRange fr2 = new FloatRange(barStartBeatPos + 2f - NON_QUANTIZED_WINDOW, barStartBeatPos + 4 - NON_QUANTIZED_WINDOW);
                var p2 = p.subSet(fr2, true);
                if (p2.isEmpty())
                {
                    LOGGER.log(Level.WARNING, "build() No note found in second half of bar={0}", bar);
                    continue;
                }
                var ne2 = p2.first();
                var ne2pitch = (cliCs2 == null) ? ne2.getPitch()
                        : BassGenerator.getClosestAndAcceptableBassPitch(ne2, cliCs2.getData().getBassNote().getRelativePitch());
                if (cliCs2 == null && cliCs0.getData().getChord().indexOfRelativePitch(ne2pitch) == -1)
                {
                    LOGGER.log(Level.FINE, "build() bar {0}: non chordtone 2nd half note. ne2={1} cliCs0={2} cliCs2={3}  p2={4}", new Object[]
                    {
                        bar, ne2, cliCs0, cliCs2, p2
                    });
                    // It happens a little bit too often, so sometimes we force another note
                    if (Math.random() > 0.6f)
                    {
                        ne2pitch = findBassNote(cliCs0, ne0);
                        LOGGER.log(Level.FINE, "   => use note {0}", ne2pitch);
                    } else
                    {
                        continue;
                    }
                }
                if (ne0.getPitch() == ne2pitch)
                {
                    LOGGER.log(Level.FINE, "build() bar {0}: repeated note ne2={1} cliCs0={2} cliCs2={3}  p2={4}", new Object[]
                    {
                        bar, ne2, cliCs0, cliCs2, p2
                    });
                    // Use another note
                    ne2pitch = findBassNote(cliCs2 == null ? cliCs0 : cliCs2, ne0);
                }


                float beatPos2 = ne2.getPositionInBeats() < barStartBeatPos + 2f + NON_QUANTIZED_WINDOW ? ne2.getPositionInBeats() : barStartBeatPos + 2f;
                ne2 = ne2.setAll(ne2pitch, 0.9f, -1, beatPos2, null, false);
                var ne3 = ne2.setPosition(barStartBeatPos + 3f, false);


                p.removeAll(new ArrayList<>(p0));
                p.removeAll(new ArrayList<>(p2));
                p.addAll(List.of(ne0, ne1, ne2, ne3));

            }
        }


        return p;
    }

    // ===============================================================================
    // Private methods
    // ===============================================================================

    /**
     * Find a suitable bass note for cliCs different from forbiddentTargetNote
     *
     * @param cliCs
     * @param forbiddentTargetNote
     * @return
     */
    private int findBassNote(CLI_ChordSymbol cliCs, NoteEvent forbiddentTargetNote)
    {
        int forbiddenPitch = forbiddentTargetNote.getPitch();
        var ecs = cliCs.getData();

        int res = BassGenerator.getClosestAndAcceptableBassPitch(forbiddentTargetNote, ecs.getBassNote().getRelativePitch());
        if (res == forbiddenPitch && ecs.isSlashChord())
        {
            res = BassGenerator.getClosestAndAcceptableBassPitch(forbiddentTargetNote, ecs.getRootNote().getRelativePitch());
        }
        if (res == forbiddenPitch)
        {
            int relPitch5th = ecs.getRelativePitch(ChordType.DegreeIndex.FIFTH);
            int res5th = BassGenerator.getClosestAndAcceptableBassPitch(forbiddentTargetNote, relPitch5th);
            int relPitch3rd = ecs.getRelativePitch(ChordType.DegreeIndex.THIRD_OR_FOURTH);
            if (relPitch3rd == -1)
            {
                res = res5th;
            } else
            {
                int res3rd = BassGenerator.getClosestAndAcceptableBassPitch(forbiddentTargetNote, relPitch3rd);
                res = Math.abs(res3rd - forbiddenPitch) < Math.abs(res5th - forbiddenPitch) ? res3rd : res5th;
            }
        }

        assert res != forbiddenPitch : "cliCs=" + cliCs + " forbiddenNote=" + forbiddentTargetNote;
        return res;
    }

}

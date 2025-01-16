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
package org.jjazz.test.walkingbass;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.CLI_Factory;
import org.jjazz.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.harmony.api.ChordType;
import org.jjazz.harmony.api.Degree;
import static org.jjazz.harmony.api.Degree.ELEVENTH_SHARP;
import static org.jjazz.harmony.api.Degree.FOURTH_OR_ELEVENTH;
import static org.jjazz.harmony.api.Degree.NINTH;
import static org.jjazz.harmony.api.Degree.NINTH_FLAT;
import static org.jjazz.harmony.api.Degree.NINTH_SHARP;
import static org.jjazz.harmony.api.Degree.SEVENTH;
import static org.jjazz.harmony.api.Degree.SEVENTH_FLAT;
import static org.jjazz.harmony.api.Degree.SIXTH_OR_THIRTEENTH;
import static org.jjazz.harmony.api.Degree.THIRTEENTH_FLAT;
import org.jjazz.harmony.api.StandardScaleInstance;
import org.jjazz.phrase.api.NoteEvent;

/**
 * Associate a chord symbol from a WbpSource with its corresponding sub-phrase.
 * <p>
 * Used to analyze phrase compatibility with chord symbol extensions.
 */
public class WbpSourceChordPhrase
{

    /**
     * +/- beat position tolerance when comparing notes (accomodate for unquantized notes)
     */
    public static final float NEAR_WINDOW = 0.15f;
    private final WbpSource wbpSource;
    private final CLI_ChordSymbol srcCliChordSymbol;
    private final ExtChordSymbol srcExtChordSymbol;
    private final ChordType srcChordType;
    private final List<NoteEvent> notes;

    /**
     *
     * @param wbpSource
     * @param srcCliCs  Must be part of the wbpSource chord sequence
     */
    public WbpSourceChordPhrase(WbpSource wbpSource, CLI_ChordSymbol srcCliCs)
    {
        Objects.requireNonNull(wbpSource);
        Preconditions.checkArgument(wbpSource.getSimpleChordSequence().contains(srcCliCs), "wbpSource=%s srcCliCs=%s", wbpSource, srcCliCs);

        this.wbpSource = wbpSource;
        this.srcCliChordSymbol = srcCliCs;
        this.srcExtChordSymbol = srcCliCs.getData();
        this.srcChordType = srcExtChordSymbol.getChordType();
        this.notes = extractNotes();
    }

    /**
     * Check if our chord phrase can be used for targetExtChordSymbol, and if yes score this compatibility.
     * <p>
     * If targetExtChordSymbol is considered incompatible, score is 0. If the chord types are equal, score is 100 (max). Note that 6 and 7M degrees are
     * considered equal in this method (e.g. C6=C7M, Cm69=Cm7M9).
     * <p>
     * Score is 0 if chord types have the same nb of Degrees but are not equal, or if targetExtChordSymbol has a scale incompatible with the phrase.<p>
     * If chord types do not have the same nb of Degrees:<br>
     * - Score is 0 if at least 1 common degree does not match, or if srcChordType has more degrees than targetChordType - If targetChordType has more degrees
     * than srcChordType (e.g. C7b9 and C): check if the phrase notes are compatible with the extra degrees. Score is 0 if one extra degree is INCOMPATIBLE,
     * otherwise score equals 100 - (10 * each COMPATIBLE_NO_USE extra degree).<br>
     * <p>
     * Examples srcExtChordSymbol - targetExtChordSymbol:<br>
     * C - Cm: 0<br>
     * C6 - Cm7M: 0<br>
     * C6 - C7: 0<br>
     * C7b9 - C9: 0<br>
     * C - C9: 90 if phrase is ExtraDegreeCompatibility.COMPATIBLE_NO_USE with 7th (-10), and ExtraDegreeCompatibility.COMPATIBLE_USE with 9th<br>
     * C6 - C9M: 90 if phrase is ExtraDegreeCompatibility.COMPATIBLE_NO_USE with 9th (-10)<br>
     * C7 - C: 0<br>
     * C - C or C6 - C7M or C13b9-C13b9: 100 (max value)
     *
     * @param targetExtChordSymbol
     * @return [0; 100] 0 means incompatibility
     */
    public float getHarmonyCompatibilityScore(ExtChordSymbol targetExtChordSymbol)
    {
        Objects.requireNonNull(targetExtChordSymbol);
        float res;

        var targetChordType = targetExtChordSymbol.getChordType();
        int nbDegreesSrc = srcChordType.getNbDegrees();
        int nbDegreesTarget = targetChordType.getNbDegrees();


        if (srcChordType.equalsSixthMajorSeventh(targetChordType))
        {
            res = 100;

        } else if (nbDegreesSrc < nbDegreesTarget && srcChordType.getNbCommonDegrees(targetChordType, true) == nbDegreesSrc)
        {
            // Need to check that each extra degree from targetChordType against the phrase notes

            res = 100;

            for (int i = nbDegreesSrc; i < nbDegreesTarget; i++)
            {
                var targetDegree = targetChordType.getDegrees().get(i);
                var dc = getExtraDegreeCompatibility(targetExtChordSymbol, targetDegree);
                switch (dc)
                {
                    case INCOMPATIBLE ->
                    {
                        res = 0;
                        break;
                    }
                    case COMPATIBLE_NO_USE ->
                    {
                        res -= 10;
                    }
                    case COMPATIBLE_USE ->
                    {
                        // Nothing
                    }
                    default -> throw new AssertionError(dc.name());

                }
            }

        } else
        {
            res = 0;
        }


        // If we're compatible check it's also compatible with the optional scale
        if (res > 0 && !isPhraseScaleCompatible(targetExtChordSymbol))
        {
            res = 0;
        }


        return res;
    }

    /**
     * Try to get a simplified source chord symbol of srcCliChordSymbol based on the used phrase notes.
     * <p>
     * If srcCliChordSymbol uses 6/7 or extension degrees (eg C69), check that the phrase really uses the corresponding degrees, and if not, return a simplified
     * chord symbol.
     * <p>
     *
     * @return srcCliChordSymbol or a simplified version (less degrees)
     */
    public CLI_ChordSymbol getSimplifiedSourceChordSymbol()
    {
        CLI_ChordSymbol res = srcCliChordSymbol;

        // Check that each extension note is indeed used in the notes
        var degrees = srcChordType.getDegrees();
        int degreeIndex = degrees.size() - 1;     // Start from last

        while (degreeIndex > 2)     // Leave root/3rd/5th alone
        {
            var d = degrees.get(degreeIndex);
            if (!isUsed(d))
            {
                var newCs = srcExtChordSymbol.getSimplified(degreeIndex);
                var newEcs = srcExtChordSymbol.getCopy(newCs, null, null, null);
                res = CLI_Factory.getDefault().createChordSymbol(newEcs, srcCliChordSymbol.getPosition());
                degreeIndex--;
            } else
            {
                break;
            }
        }

        return res;
    }

    /**
     * The WbpSource phrase notes corresponding to the srcCliCs slice of time.
     * <p>
     *
     * @return Can be empty. Unmodifiable list. Because of non-quantization, some notes might start a little bit earlier than srcCliCs.
     */
    public List<NoteEvent> getNotes()
    {
        return Collections.unmodifiableList(notes);
    }


    // =================================================================================================================
    // Private methods
    // =================================================================================================================    
    private void removeNonSignificantNotes(List<NoteEvent> notes)
    {
        notes.removeIf(ne -> ne.getDurationInBeats() <= NEAR_WINDOW);    // Remove very short notes
    }

    /**
     * Extract the notes corresponding to srcCliChordSymbol, take non-quantization into account.
     *
     * @return
     */
    private List<NoteEvent> extractNotes()
    {
        var csBeatRange = wbpSource.getSimpleChordSequence().getBeatRange(srcCliChordSymbol, 0);
        float fromOffset = Math.min(NEAR_WINDOW, csBeatRange.from);
        float toOffset = Math.min(NEAR_WINDOW, csBeatRange.size());
        csBeatRange = csBeatRange.getTransformed(-fromOffset, -toOffset);     // phrase can be non-quantized

        var res = new ArrayList<>(wbpSource.getSizedPhrase().subSet(csBeatRange, true));
        removeNonSignificantNotes(res);

        return res;
    }

    /**
     * Check that our phrase does not contain notes usually considered "musically incompatible" with the extra degree.
     * <p>
     * Examples:<br>
     * - If extraDegree=9th of targetEcs=C9, check that notes do not contain "significant" Db or D# notes.<br>
     * - If extraDegree=9th of targetEcs=Cm9, check that notes do not contain "significant" Db notes.<br>
     *
     * @param targetEcs
     * @param extraDegree extraDegree for srcChordType (but degree used by targetChordType). Valid extra degrees are any degree except root/3rd/5th.
     * @return
     */
    private ExtraDegreeCompatibility getExtraDegreeCompatibility(ExtChordSymbol targetEcs, Degree extraDegree)
    {
        assert targetEcs.getChordType().getDegrees().contains(extraDegree) : "targetEcs=" + targetEcs + " extraDegree=" + extraDegree;
        assert !srcChordType.getDegrees().contains(extraDegree) : "srcChordType=" + srcChordType + " extraDegree=" + extraDegree;


        ExtraDegreeCompatibility res = ExtraDegreeCompatibility.INCOMPATIBLE;
        if (checkNoIncompatibleDegrees(targetEcs, extraDegree))
        {
            res = isUsed(extraDegree) ? ExtraDegreeCompatibility.COMPATIBLE_USE : ExtraDegreeCompatibility.COMPATIBLE_NO_USE;
        }

        return res;
    }


    private boolean checkNoIncompatibleDegrees(ExtChordSymbol targetEcs, Degree extraDegree)
    {
        boolean b = true;

        var degreesToAvoid = getIncompatibleDegrees(targetEcs, extraDegree);

        boolean avoidNotesPresent = degreesToAvoid.stream()
                .anyMatch(d -> isUsed(d));

        if (avoidNotesPresent)
        {
            // There is at least 1 degree to avoid, check that it's not more "significant" than extraDegree
            float totalAvoidDuration = (float) degreesToAvoid.stream()
                    .mapToDouble(d -> getTotalWeightedDuration(d)) // Promotes downbeat over upbeat 
                    .sum();

            b = getTotalWeightedDuration(extraDegree) > totalAvoidDuration;
        }

        return b;
    }

    /**
     * Compute total duration used by relPitch, but upbeat notes are counted for half of their duration.
     *
     * @param d
     * @return
     */
    private float getTotalWeightedDuration(Degree d)
    {
        float res = 0;
        var ts = wbpSource.getTimeSignature();
        int relPitch = srcExtChordSymbol.getRelativePitch(d);

        for (var n : notes)
        {
            if (n.getRelativePitch() == relPitch)
            {
                float pos = n.getPositionInBeats();
                int nbBars = (int) (pos / ts.getNbNaturalBeats());
                float beat = pos - (nbBars * ts.getNbNaturalBeats());
                float dur = ts.isDownBeat(beat) ? n.getDurationInBeats() : n.getDurationInBeats() / 2;
                res += dur;
            }
        }

        return res;
    }

    /**
     * Check if d is used in the source phrase.
     *
     * @param d
     * @return
     */
    private boolean isUsed(Degree d)
    {
        int relPitch = srcExtChordSymbol.getRelativePitch(d);
        return notes.stream().anyMatch(n -> n.getRelativePitch() == relPitch);
    }


    /**
     * Get the "usually" musically-incompatible degrees of extraDegree when playing targetEcs.
     * <p>
     *
     * @param targetEcs
     * @param extraDegree
     * @return
     */
    private List<Degree> getIncompatibleDegrees(ExtChordSymbol targetEcs, Degree extraDegree)
    {
        var targetCt = targetEcs.getChordType();

        // targetCt is guaranteed to have the same root/3rd/5th degrees than srcExtChordType
        // srcExtChordType can be any "shorter" chord type than the targetCt listed below
        // Example If targetCt=Cm69, srcExtChordType can be Cm or Cm6

        List<Degree> res = switch (extraDegree)
        {
            case NINTH_FLAT ->
                // targetCt can be C7b9(b5,#5,#11), C13b9(b5,#11), Cm7b9, C7susb9, C13susb9
                List.of(Degree.NINTH);

            case NINTH ->
                // targetCt can be C69, CM9(#11), C13M(#11), C9(b5,#5,#11), m69, m9(11,M7), m13
                targetCt.isMajor() ? List.of(Degree.NINTH_FLAT, Degree.NINTH_SHARP) : List.of(Degree.NINTH_FLAT);

            case NINTH_SHARP ->
            {
                // targetCt can be C7#9(b5,#5,#11), C13#9(b5)
                assert targetCt.isSeventhMinor() : "targetEcs=" + targetEcs + " extraDegree=" + extraDegree;
                yield List.of(Degree.NINTH);
            }

            case FOURTH_OR_ELEVENTH ->
            {
                // It's an extra degree beyond the 3 first degrees, so it's an eleventh
                // targetCt can be Cm11(b5)
                assert targetCt.isMinor() : "targetEcs=" + targetEcs + " extraDegree=" + extraDegree;
                yield Collections.emptyList();    // 3rd note is incompatible, but if it's there it's a transition note since we're in minor. b5 also possible with eleventh
            }

            case ELEVENTH_SHARP ->
            {
                // targetCt can be CM7#11, CM9#11, CM13#11, C7(b9,#9)#11, C9#11, C13(b9)#11
                assert targetCt.isMajor() : "targetEcs=" + targetEcs + " extraDegree=" + extraDegree;
                yield Collections.emptyList();    // 11th note is incompatible, but if it's there it's a transition note since we're in major. 5th is ok.
            }

            case THIRTEENTH_FLAT ->         // never used ?
                List.of(Degree.FIFTH, Degree.SIXTH_OR_THIRTEENTH);

            case SIXTH_OR_THIRTEENTH ->
                // targetCt can be C6(0), Cm6(9) 
                // targetCt can be CM713, CM13(#11)
                // targetCt can be C13(b9#9b5#11), Cm713, Cm13, Csus13(b9)
                targetCt.isSixth() || targetCt.isSeventhMajor() ? List.of(Degree.FIFTH_SHARP, Degree.SEVENTH_FLAT) : List.of(Degree.FIFTH_SHARP);

            case SEVENTH_FLAT ->
                // targetCt can be C7(xxx), C9(xxx), C13(xxx)
                List.of(Degree.SEVENTH);

            case SEVENTH ->
                // targetCt can be CM7(b5,#5,#11,13), CM9(#11), CM13(#11)
                // targetCt can be Cm7M, Cm9M7, Cdim7M
                List.of(Degree.SEVENTH_FLAT);

            default ->
                throw new IllegalStateException("extraDegree=" + extraDegree);
        };

        return res;
    }

    /**
     * Check that source phrase is also compatible with targetExtChordSymbol optional scale.
     * <p>
     * Example: srcCliChordSymbol=Cm, targetExtChordSymbol=Cm7 with phrygian scale (Ab key) => phrase should not use 9th significant notes to be compatible.<p>
     * Returns true if all phrase notes belong to the scale: it's a bit radical condition, there are probably smarter ways to do this...
     *
     * @param targetExtChordSymbol
     * @return
     */
    private boolean isPhraseScaleCompatible(ExtChordSymbol targetExtChordSymbol)
    {
        boolean b = true;

        var stdScaleInstance = targetExtChordSymbol.getRenderingInfo().getScaleInstance();
        if (stdScaleInstance != null)
        {
            List<NoteEvent> notesClean = new ArrayList<>(notes);
            removeNonSignificantNotes(notesClean);

            // We need to transpose notes to targetExtChordSymbol root
            int t = srcExtChordSymbol.getRootNote().getRelativeAscInterval(targetExtChordSymbol.getRootNote());
            var tNotes = notesClean.stream()
                    .map(ne -> ne.getTransposed(t))
                    .toList();

            b = tNotes.stream()
                    .allMatch(n -> stdScaleInstance.getRelativePitches().contains(n.getRelativePitch()));
        }

        return b;
    }


// =================================================================================================================
// Inner classes
// =================================================================================================================    
    private enum ExtraDegreeCompatibility
    {
        /**
         * Phrase seems not compatible with the specified degree.
         * <p>
         * Example: A C7 phrase which contains significant Db notes and no D note, when tested against the 9th.
         */
        INCOMPATIBLE,
        /**
         * Phrase seems compatible and it does not use the specified degree (non-significant notes ignored).
         */
        COMPATIBLE_NO_USE,
        /**
         * Phrase seems compatible and it does use the specified degree (non-significant notes ignored).
         */
        COMPATIBLE_USE;
    }


}

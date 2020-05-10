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
package org.jjazz.rhythmmusicgeneration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jjazz.harmony.Chord;
import org.jjazz.harmony.ChordType;
import org.jjazz.harmony.Degree;
import org.jjazz.harmony.Note;
import org.jjazz.harmony.ScaleManager;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordRenderingInfo.Feature;
import org.jjazz.leadsheet.chordleadsheet.api.item.ExtChordSymbol;
import static org.jjazz.rhythmmusicgeneration.Phrase.PARENT_NOTE;
import static org.jjazz.util.Utilities.heapPermutation;
import org.jjazz.rhythmmusicgeneration.SourcePhrase.ChordMode;

/**
 * Phrase manipulation methods.
 */
public class Phrases
{

    private static final Logger LOGGER = Logger.getLogger(Phrases.class.getSimpleName());

    /**
     * Build a Phrase with 12 notes C-D-E-F-G-A-B-C that fit into nbBeats.
     *
     * @param channel
     * @param startPos Position of the 1st note 'C'
     * @param nbBeats
     * @return
     */
    static public Phrase getCscalePhrase(int channel, float startPos, float nbBeats)
    {
        Phrase p = new Phrase(channel);
        float noteDur = nbBeats / 8f;
        float pos = startPos;
        for (Note n : ScaleManager.MAJOR.getNotes())
        {
            NoteEvent ne = new NoteEvent(n.getPitch() + 60, noteDur, n.getVelocity(), pos);
            pos += noteDur;
            p.addOrdered(ne);
        }
        // Add octave note at this end
        NoteEvent ne = new NoteEvent(72, noteDur, Note.VELOCITY_STD, pos);
        p.addOrdered(ne);
        return p;
    }


    /**
     * Adapt the notes from a melody-oriented source phrase to a destination chord symbol.
     * <p>
     * Notes are transposed to the destination root note and adapted to the destination chord type degrees.<br>
     * Ex: if pSrc=C3,G3,B3,E4 and ecsSrc=C7M and ecsDest=F7b5, then destination phrase=F3,B3,Eb4,Ab4<p>
     * Each destination note of the resulting phrase stores its corresponding source note in the PARENT_NOTE client property.
     *
     * @param pSrc The source phrase
     * @param ecsDest The destination extended chord symbol.
     * @param chordMode True if source phrase is a chord phrase for which we want the melodic handling.
     * @return A new phrase with destination notes.
     */
    static public Phrase fitMelodyPhrase2ChordSymbol(SourcePhrase pSrc, ExtChordSymbol ecsDest, boolean chordMode)
    {
        if (pSrc == null || ecsDest == null)
        {
            throw new IllegalArgumentException("pSrc=" + pSrc + " ecsDest=" + ecsDest);
        }
        LOGGER.fine("fitMelodyPhrase2ChordSymbol() -- ecsDest=" + ecsDest + "chordMode=" + chordMode + " pSrc=" + pSrc);
        Phrase pDest = new Phrase(pSrc.getChannel());
        if (pSrc.isEmpty())
        {
            // Special case, easy
            return pDest;
        }


        int rootPitchDelta = Note.getNormalizedRelPitch(
                ecsDest.getRootNote().getRelativePitch() - pSrc.getSourceChordSymbol().getRootNote().getRelativePitch());
        ExtChordSymbol ecsSrc = pSrc.getSourceChordSymbol();


        if (ecsSrc.isSameChordType(ecsDest))
        {
            // Special case, same chord types, just transpose notes to destination key
            for (NoteEvent srcNote : pSrc)
            {
                int destRelPitch = ecsSrc.getRelativePitch(srcNote.getRelativePitch(), ecsDest);
                int destPitch = new Note(srcNote.getPitch() + rootPitchDelta).getClosestPitch(destRelPitch);
                NoteEvent destNote = new NoteEvent(srcNote, destPitch);
                destNote.putClientProperty(PARENT_NOTE, srcNote);
                pDest.add(destNote);  // Don't need addOrdered here
            }
            return pDest;
        }


        // Get the destination degree for each source phrase degree
        HashMap<Degree, Degree> mapSrcDestDegrees = pSrc.getDestDegrees(ecsDest, chordMode ? ChordMode.NO_INVERSION : ChordMode.OFF);


        // Create the result phrase
        for (NoteEvent srcNote : pSrc)
        {
            int srcRelPitchToRoot = Note.getNormalizedRelPitch(srcNote.getRelativePitch() - ecsSrc.getRootNote().getRelativePitch());
            Degree srcDegree = ecsSrc.getChordType().getDegreeMostProbable(srcRelPitchToRoot);
            Degree destDegree = mapSrcDestDegrees.get(srcDegree);
            assert destDegree != null : "srcDegree=" + srcDegree + " srcNote=" + srcNote + " pSrc=" + pSrc + " ecsDest=" + ecsDest + " chordMode=" + chordMode;
            int destRelPitch = ecsDest.getRelativePitch(destDegree);
            int destPitch = new Note(srcNote.getPitch() + rootPitchDelta).getClosestPitch(destRelPitch);
            NoteEvent destNote = new NoteEvent(srcNote, destPitch);
            destNote.putClientProperty(PARENT_NOTE, srcNote);
            pDest.add(destNote);        // Don't need addOrdered here
        }


        return pDest;
    }

    /**
     * Adapt the notes from a bass melody-oriented source phrase to a destination chord symbol.
     * <p>
     * Notes are transposed to the destination root note and adapted to the destination chord type degrees.<br>
     * If destination symbol bass note is different from the root note, it is used instead of the root note. The method takes also
     * into account the ChordRenderingInfo.BassLineModifiers of the chord symbol.<br>
     * Ex: if pSrc=C3,G3,B3,E4 and ecsSrc=C7M and ecsDest=F7b5/A, then destination phrase=A3,B3,Eb4,Ab4.<br>
     * <p>
     * Each destination note of the resulting phrase stores its corresponding source note in the PARENT_NOTE client property.
     *
     * @param pSrc The source phrase
     * @param ecsDest The destination extended chord symbol.
     * @return A new phrase with destination notes.
     */
    static public Phrase fitBassPhrase2ChordSymbol(SourcePhrase pSrc, ExtChordSymbol ecsDest)
    {
        if (pSrc == null || ecsDest == null)
        {
            throw new IllegalArgumentException("pSrc=" + pSrc + " ecsDest=" + ecsDest);
        }


        LOGGER.fine("fitBassPhrase2ChordSymbol() -- ecsDest=" + ecsDest + " pSrc=" + pSrc);


        Phrase pDest = new Phrase(pSrc.getChannel());
        if (pSrc.isEmpty())
        {
            return pDest;
        }

        // Prepare data
        boolean useFixedNote = ecsDest.getRenderingInfo().hasAllFeatures(Feature.BASS_PEDAL);
        int rootPitchDelta = Note.getNormalizedRelPitch(
                ecsDest.getRootNote().getRelativePitch() - pSrc.getSourceChordSymbol().getRootNote().getRelativePitch());
        ExtChordSymbol ecsSrc = pSrc.getSourceChordSymbol();


        // Special case, same chord types, just transpose notes to destination key
        if (ecsSrc.isSameChordType(ecsDest))
        {

            for (NoteEvent srcNote : pSrc)
            {
                int destRelPitch = ecsSrc.getRelativePitch(srcNote.getRelativePitch(), ecsDest);

                // Use the chord symbol bass note if BassLineModifier says so, or to replace the chord symbol root note
                if (useFixedNote || destRelPitch == ecsDest.getRootNote().getRelativePitch())
                {
                    destRelPitch = ecsDest.getBassNote().getRelativePitch();
                }

                int destPitch = new Note(srcNote.getPitch() + rootPitchDelta).getClosestPitch(destRelPitch);
                NoteEvent destNote = new NoteEvent(srcNote, destPitch);
                destNote.putClientProperty(PARENT_NOTE, srcNote);
                pDest.add(destNote);         // Don't need addOrdered here
            }

            return pDest;
        }


        // Get the destination degree for each source phrase degree
        HashMap<Degree, Degree> mapSrcDestDegrees = pSrc.getDestDegrees(ecsDest, ChordMode.OFF);
        LOGGER.fine("fitBassPhrase2ChordSymbol() mapSrcDestDegrees=" + mapSrcDestDegrees);


        // Create the result phrase      
        for (NoteEvent srcNote : pSrc)
        {

            int srcRelPitchToRoot = Note.getNormalizedRelPitch(srcNote.getRelativePitch() - ecsSrc.getRootNote().getRelativePitch());
            Degree srcDegree = ecsSrc.getChordType().getDegreeMostProbable(srcRelPitchToRoot);
            Degree destDegree = mapSrcDestDegrees.get(srcDegree);
            assert destDegree != null : "srcDegree=" + srcDegree + " srcNote=" + srcNote + " pSrc=" + pSrc;
            int destRelPitch = ecsDest.getRelativePitch(destDegree);

            // Use the chord symbol bass note if BassLineModifier says so, or to replace the chord symbol root note
            if (useFixedNote || destDegree.equals(Degree.ROOT))
            {
                destRelPitch = ecsDest.getBassNote().getRelativePitch();
            }

            int destPitch = new Note(srcNote.getPitch() + rootPitchDelta).getClosestPitch(destRelPitch);
            NoteEvent destNote = new NoteEvent(srcNote, destPitch);
            destNote.putClientProperty(PARENT_NOTE, srcNote);
            pDest.add(destNote);         // Don't need addOrdered here
        }


        return pDest;
    }


    /**
     * Adapt the notes from a chord-oriented source phrase to a destination chord symbol.
     * <p>
     * Same as fitMelodyPhrase2ChordSymbol() except:<br>
     * - we must select which destination degrees are used if destination chord is more complex than source chord (eg
     * C=&gt;C7b9)<br>
     * - if destination chord is less complex than source chord (eg C7M=&gt;C), which dest degree should be reused ?<br>
     * - we search all the possible chord inversions to find the best matching destination (eg which minimize top voice motion).
     * <p>
     * Ex: if pSrc=C3,G3,B3,E4 and ecsSrc=C7M and ecsDest=F7b5, then destination phrase=F3,B3,Eb4,Ab4<p>
     * Each destination note of the resulting phrase stores its corresponding source note in the PARENT_NOTE client property.
     *
     * @param pSrc The source phrase
     * @param ecsDest The destination extended chord symbol.
     * @return A new phrase with destination notes.
     */
    static public Phrase fitChordPhrase2ChordSymbol(SourcePhrase pSrc, ExtChordSymbol ecsDest)
    {
        if (pSrc == null || ecsDest == null)
        {
            throw new IllegalArgumentException("pSrc=" + pSrc + " ecsDest=" + ecsDest);
        }

        LOGGER.log(Level.FINE, "fitChordPhrase2ChordSymbol() -- ecsDest={0} pSrc={1}", new Object[]
        {
            ecsDest, pSrc
        });


        Phrase pDest = new Phrase(pSrc.getChannel());


        if (pSrc.isEmpty())
        {
            return pDest;
        }


        // Get the destination degrees for each source phrase degree
        HashMap<Degree, Degree> mapSrcDestDegrees = pSrc.getDestDegrees(ecsDest, ChordMode.INVERSION_ALLOWED);
        Collection<Degree> destDegrees = mapSrcDestDegrees.values();


        LOGGER.log(Level.FINE, "fitChordPhrase2ChordSymbol()   mapSrcDestDegrees={0}", mapSrcDestDegrees);


        // Compute all the destination degrees permutations, eg [1,3,7] [7,3,1] [3,1,7] etc.
        // CAUTIOUS this is X! => 6!=720 permutations. But normally most Yamaha chord source phrase should use max 4 chord notes.
        List<Degree[]> destDegreesPermutations = new ArrayList<>();
        heapPermutation(destDegrees.toArray(new Degree[0]), destDegrees.size(), destDegrees.size(), destDegreesPermutations);


        // Chord made of each unique pitch note of the phrase
        Chord pSrcChord = pSrc.getChord();


        // Calculate matching score for each permutation 
        int bestScore = 100000;
        Chord bestDestChord = null;
        for (Degree[] destDegreePermutation : destDegreesPermutations)
        {

            // Build the destination phrase for this permutation
            List<Integer> relPitches = getRelativePitches(ecsDest.getRootNote().getRelativePitch(), destDegreePermutation);


            // Try with startnote below
            Chord destChord = pSrcChord.computeParallelChord(relPitches, false);
            int score = computeChordMatchingScore(pSrcChord, destChord, ecsDest);
            if (bestDestChord == null || score < bestScore)
            {
                bestDestChord = destChord;
                bestScore = score;
            }


            // Try with startnote above
            destChord = pSrcChord.computeParallelChord(relPitches, true);
            score = computeChordMatchingScore(pSrcChord, destChord, ecsDest);
            if (score < bestScore)
            {
                bestDestChord = destChord;
                bestScore = score;
            }
        }

        // Fix musical problems, like 2 contiguous top notes etc.
        // fixChordMusicalProblems(bestDestChord, ecsDest);
        assert bestDestChord != null : "pSrc=" + pSrc + " ecsDest=" + ecsDest;


        // Create the destination phrase with the best matching chord
        for (NoteEvent srcNote : pSrc)
        {
            int srcPitch = srcNote.getPitch();
            int srcIndex = pSrcChord.indexOfPitch(srcPitch);
            assert srcIndex != -1 : "srcPitch=" + srcPitch + " pSrcChord=" + pSrcChord + " pSrc=" + pSrc;
            int destPitch = bestDestChord.getNote(srcIndex).getPitch();


            NoteEvent destNote = new NoteEvent(srcNote, destPitch);
            destNote.putClientProperty(PARENT_NOTE, srcNote);
            pDest.add(destNote);     // Don't need addOrdered here
        }


        return pDest;

    }

    //==================================================================================================
    // Private methods
    //==================================================================================================
    /**
     * Compute a score indicating the "compatibility" between the notes of the 2 chords for chord-oriented voicings or melodies.
     * <p>
     * Best score is 0, 1 is less good, etc. Both chords should be same size. If not, return a score of 10000. Score is also
     * impacted by musical problems in the destination chord, such as 2 top contiguous notes for example.
     *
     * @param cSrc
     * @param cDest
     * @param ecsDest The chord symbol for the destination phrase
     * @return
     */
    static private int computeChordMatchingScore(Chord cSrc, Chord cDest, ExtChordSymbol ecsDest)
    {
        if (cSrc.size() != cDest.size())
        {
            return 10000;
        }
        int score = cSrc.computeDistance(cDest);
        int maxPitch = cDest.getMaxPitch();
        int minPitch = cDest.getMinPitch();
        int topPitchDelta = Math.abs(cSrc.getMaxPitch() - maxPitch);
        int lowPitchDelta = Math.abs(cSrc.getMinPitch() - minPitch);
        score += 3 * topPitchDelta + lowPitchDelta;

        // Add penalties for non-musical stuff
        int size = cDest.size();
        if (size > 2)
        {
            ChordType ct = ecsDest.getChordType();
            if (ct.getBase().equals("13"))
            {
                // For 7/13 chord symbols promote voicings in fourth eg C13 => Bb C A         
                if (maxPitch - minPitch < 11)
                {
                    score += 4 * size;
                }
            }
            if (cDest.getNote(size - 2).getPitch() == maxPitch - 1)
            {
                // 2 contiguous notes on the top
                score += 3 * size;
            }
            if (maxPitch - minPitch == 13)
            {
                // 9b interval
                score += 4 * size;
            }
            if ((cDest.getNote(1).getPitch() - minPitch) >= 9 && (minPitch % 12 != ecsDest.getRootNote().getRelativePitch()))
            {
                // Interval >= 6th on 2 first notes with first note not being the root
                score += 2 * size;
            }
        }

//      LOGGER.log(Level.FINE, "computeChordMatchingScore() score={0}  cDest={1}", new Object[]
//      {
//         score, cDest
//      });
        return score;
    }

    /**
     * Get the relative pitches corresponding to the degrees for a chord symbol whose root=rootPitch.
     * <p>
     * Ex: rootPitch=0, degrees=ROOT,FIFTH =&gt; return [0,7]=[C,G]
     *
     * @param rootPitch
     * @param degrees
     * @return
     */
    static public List<Integer> getRelativePitches(int rootPitch, Degree[] degrees)
    {
        if (!Note.checkPitch(rootPitch) || degrees == null)
        {
            throw new IllegalArgumentException("rootPitch=" + rootPitch + " degrees=" + degrees);
        }
        List<Integer> res = Stream.of(degrees)
                .map(d -> Note.getNormalizedRelPitch(rootPitch + d.getPitch()))
                .collect(Collectors.toList());
        return res;
    }

    /**
     * A predicate to test velocity if within the specified bounds.
     *
     * @param min
     * @param max
     * @return
     */
    static public Predicate<NoteEvent> testVelocityRange(final int min, final int max)
    {
        return ne -> ne.getVelocity() >= min && ne.getVelocity() <= max;
    }

    /**
     * A predicate to test if the notes have one of the specified pitches.
     *
     * @param pitches
     * @return
     */
    static public Predicate<NoteEvent> testPitches(List<Integer> pitches)
    {
        return ne -> pitches.contains(ne.getPitch());
    }

}

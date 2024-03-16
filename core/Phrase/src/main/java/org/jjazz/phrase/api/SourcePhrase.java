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
package org.jjazz.phrase.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Logger;
import org.jjazz.harmony.api.ChordType;
import org.jjazz.harmony.api.ChordType.DegreeIndex;
import org.jjazz.harmony.api.Degree;
import static org.jjazz.harmony.api.Degree.SIXTH_OR_THIRTEENTH;
import org.jjazz.harmony.api.Note;
import org.jjazz.harmony.api.StandardScaleInstance;
import org.jjazz.chordleadsheet.api.item.ExtChordSymbol;
import static org.jjazz.phrase.api.Phrase.PARENT_NOTE;

/**
 * A source Phrase is a Phrase associated to a source chord symbol and possibly with some client properties.
 */
public class SourcePhrase extends Phrase
{

    private ExtChordSymbol chordSymbol;
    private HashMap<String, String> clientProperties;

    public enum ChordMode
    {
        OFF, INVERSION_ALLOWED, NO_INVERSION
    };

    private static final Logger LOGGER = Logger.getLogger(SourcePhrase.class.getSimpleName());

    public SourcePhrase(int channel, ExtChordSymbol ecs)
    {
        super(channel);
        if (ecs == null)
        {
            throw new IllegalArgumentException("channel=" + channel + " ecs=" + ecs);   
        }
        chordSymbol = ecs;
    }

    /**
     * Build a new SourcePhrase .
     *
     * @param p
     * @param ecs
     */
    public SourcePhrase(Phrase p, ExtChordSymbol ecs)
    {
        super(p.getChannel());
        if (ecs == null)
        {
            throw new IllegalArgumentException("p=" + p + " ecs=" + ecs);   
        }
        chordSymbol = ecs;
        add(p);
    }

    /**
     * Set a client property to this object.
     *
     * @param propName
     * @param propValue If null, this removes the property.
     */
    public void setClientProperty(String propName, String propValue)
    {
        if (propName == null)
        {
            throw new IllegalArgumentException("propName=" + propName + " propValue=" + propValue);   
        }
        if (clientProperties == null)
        {
            clientProperties = new HashMap<>();
        }
        if (propValue == null)
        {
            clientProperties.remove(propName);
        } else
        {
            clientProperties.put(propName, propValue);
        }
    }

    /**
     * Get a client property.
     *
     * @param propName
     * @return Null if property not set.
     */
    public String getClientProperty(String propName)
    {
        if (propName == null)
        {
            throw new IllegalArgumentException("propName=" + propName);   
        }
        return clientProperties.get(propName);
    }

    /**
     * @return The chordSymbol associated to this source phrase.
     */
    public ExtChordSymbol getSourceChordSymbol()
    {
        return chordSymbol;
    }

    /**
     * Overridden to return a SourcePhrase.
     *
     * @param tester
     * @param mapper
     * @return
     */
    @Override
    public SourcePhrase getProcessedPhrase(Predicate<NoteEvent> tester, Function<NoteEvent, NoteEvent> mapper)
    {
        SourcePhrase res = new SourcePhrase(getChannel(), chordSymbol);
        for (NoteEvent ne : this)
        {
            if (tester.test(ne))
            {
                NoteEvent newNe = mapper.apply(ne);
                newNe.getClientProperties().set(ne.getClientProperties());
                if (newNe.getClientProperties().get(PARENT_NOTE) == null)
                {
                    newNe.getClientProperties().put(PARENT_NOTE, ne);         // If no previous PARENT_NOTE client property we can add one
                }
                res.add(newNe);
            }
        }
        return res;
    }

    /**
     * Get all the source chord symbol degrees used in this source phrase.
     *
     * @return An ordered list of Degrees.
     */
    public List<Degree> getUsedDegrees()
    {
        ArrayList<Degree> degrees = new ArrayList<>();
        ChordType ct = chordSymbol.getChordType();
        for (Note note : Phrases.getChord(this).getRelativePitchChord().getNotes())
        {
            int relPitchToRoot = Note.getNormalizedRelPitch(note.getRelativePitch() - chordSymbol.getRootNote().getRelativePitch());
            Degree d = ct.getDegreeMostProbable(relPitchToRoot);
            if (!degrees.contains(d))
            {
                degrees.add(d);
            }
        }
        Collections.sort(degrees);
        return degrees;
    }

    /**
     * Map each degree of this source phrase (as returned by getUsedDegrees()) to a degree of the specified destination chord
     * symbol.
     * <p>
     * If chordMode==false:<br>
     * Destination degrees are the source phrase degrees fitted to the destination chord symbol.
     * <p>
     * If chordMode==true:<br>
     * Destination degrees are the first most important notes of the destination chord symbol. <br>
     * If destination chord symbol is less complex than the source chord symbol(eg C7M=&gt;C) then one or more destination degrees
     * are reused.<br>
     *
     * @param ecsDest   The destination chord symbol.
     * @param chordMode
     * @return A map with key="a source chord symbol degree" and value="a destination chord symbol degree".
     */
    public Map<Degree, Degree> getDestDegrees(ExtChordSymbol ecsDest, ChordMode chordMode)
    {
        Map<Degree, Degree> result = new HashMap<>();
        if (isEmpty())
        {
            return result;
        }

        List<Degree> srcDegrees = getUsedDegrees();

        if (chordSymbol.isSameChordType(ecsDest) && ecsDest.getRenderingInfo() == null)
        {
            // Special case, same chord type, no harmony defined, just reuse the source degrees
            for (Degree srcDegree : srcDegrees)
            {
                result.put(srcDegree, srcDegree);
            }
        } else if (!chordMode.equals(ChordMode.OFF))
        {
            // CHORD MODE
            result = getDestDegreesChordMode(ecsDest, chordMode);
        } else
        {
            // MELODY MODE
            ChordType ctDest = ecsDest.getChordType();
            for (Degree srcDegree : srcDegrees)
            {
                // Try basic degree fit 
                Degree destDegree = ctDest.fitDegree(srcDegree);
                if (destDegree == null)
                {
                    // Did not work, try fitAdvanced() if some scales are defined or for "easy to fit" degrees
                    StandardScaleInstance scale = ecsDest.getRenderingInfo().getScaleInstance();
                    if (scale != null || srcDegree.getNatural().equals(Degree.Natural.NINTH)
                            || srcDegree.equals(SIXTH_OR_THIRTEENTH) // Only natural thirteenth is OK
                            || srcDegree.getNatural().equals(Degree.Natural.SEVENTH))
                    {
                        destDegree = ctDest.fitDegreeAdvanced(srcDegree, scale);
                    } else
                    {
                        // Just reuse it
                        destDegree = srcDegree;
                    }
                }
                result.put(srcDegree, destDegree);
            }
        }

        return result;
    }

    @Override
    public SourcePhrase clone()
    {
        Phrase p = super.clone();
        SourcePhrase sp = new SourcePhrase(p, chordSymbol);
        return sp;
    }

    @Override
    public String toString()
    {
        String s = "(" + chordSymbol + ")" + super.toString();
        return s;
    }

    //==================================================================================================
    // Private methods
    //==================================================================================================
    /**
     * In chord mode, identify the destination chord degrees which should be used to play the degrees (as returned by
     * getUsedDegrees()) of the source phrase.
     * <p>
     * Based on the use of the most important degrees of the destination chord symbol. Sizes may differ between the nb of source
     * degrees and the nb of destination degrees. If a direct map from a sourceDegree to a destDegree is not possible, try to find the
     * "closest" note.
     * <p>
     * Algorithm uses 3 cases:<br>
     * 1/ Destination chord is identical or more complex than source chord symbol (C=&gt;C7): use the first most important degrees of
     * the destination chord as necessary.<br>
     * 2/ When dest. chord is simpler than source symbol (C7M=&gt;C): same as 1 plus one or more destination degrees must be reused.<br>
     * 3/ Special case if only 1 or 2 degrees in the source phrase, just reuse them.
     * <p>
     * Ex: C7M source phrase notes=C E G B<br>
     * ecsDest=Fm9, =&gt;4 most important notes=THIRD_OR_FOURTH, SEVENTH, EXTENSION1, FIFTH.<br>
     * result map=[THIRD=&gt;THIRD_FLAT, SEVENTH=&gt;SEVENTH_FLAT, FIFTH=&gt;FIFTH, ROOT=&gt;NINTH]
     * <p>
     * Ex: C source phrase notes=C E G<br>
     * ecsDest=Fm9, =&gt;3 most important notes=THIRD_OR_FOURTH, SEVENTH, EXTENSION1.<br>
     * result map=[THIRD=&gt;THIRD_FLAT, ROOT(C)=&gt;SEVENTH_FLAT, FIFTH(G)=&gt;NINTH] with chordMode=INVERSION_ALLOWED<br>
     * result map=[THIRD=&gt;THIRD_FLAT, ROOT(F)=&gt;NINTH, FIFTH(C)=&gt;SEVENTH_FLAT] with chordMode=NO_INVERSION
     * <p>
     * Ex: C7M source phrase notes=C E G B and ecsDest=Am<br>
     * result map=[ROOT=&gt;ROOT, THIRD=&gt;THIRD_FLAT, FIFTH=&gt;FIFTH, SEVENTH=&gt;THIRD_FLAT] with chordMode=INVERSION_ALLOWED<br>
     * result map=[ROOT=&gt;ROOT, THIRD=&gt;THIRD_FLAT, FIFTH=&gt;FIFTH, SEVENTH=&gt;ROOT] with chordMode=NO_INVERSION
     * <p>
     * Ex: C7M source phrase notes=C G and ecsDest=XX<br>
     * result map=[ROOT=&gt;ROOT, FIFTH=&gt;FIFTH]
     * <p>
     *
     * @param ecsDest   The destination chord symbol.
     * @param chordMode Can not be equal to "OFF".
     * @return The source phrase degrees and the corresponding destination degrees. A destination degree may appear more than once
     *         (see case 2/ above).
     */
    private Map<Degree, Degree> getDestDegreesChordMode(ExtChordSymbol ecsDest, ChordMode chordMode)
    {
        if (ecsDest == null || chordMode.equals(ChordMode.OFF))
        {
            throw new IllegalArgumentException("ecsDest=" + ecsDest + " chordMode=" + chordMode);   
        }

        HashMap<Degree, Degree> mapResult = new HashMap<>();

        ChordType ctSrc = chordSymbol.getChordType();
        ChordType ctDest = ecsDest.getChordType();

        List<Degree> srcDegrees = getUsedDegrees();
        int nbSrcDegrees = srcDegrees.size();
        int nbDestDegrees = ctDest.getDegrees().size();
        List<ChordType.DegreeIndex> miDestDegreeIndexes = new ArrayList<>(ctDest.getMostImportantDegreeIndexes());

        if (nbSrcDegrees <= 2)
        {
            // Special case, just fit the src degrees to destination chord, like in melody mode
            for (Degree srcDegree : srcDegrees)
            {
                Degree destDegree = ctDest.fitDegreeAdvanced(srcDegree, ecsDest.getRenderingInfo().getScaleInstance());
                mapResult.put(srcDegree, destDegree);
            }
        } else if (nbDestDegrees >= nbSrcDegrees)
        {
            // Source phrase needs to be adapted to an equivalent or more complex destination chord
            // Ex: ecsSrc=C7M, pSrc=C,E,G,B, and ecsDest=Cm79
            // Take as many "most important dest degrees" as there are source degrees present in the source phrase
            List<DegreeIndex> degreeIndexes = miDestDegreeIndexes.subList(0, nbSrcDegrees);
            for (DegreeIndex di : degreeIndexes.toArray(DegreeIndex[]::new))
            {
                Degree destDegree = ctDest.fitDegreeAdvanced(di, ecsDest.getRenderingInfo().getScaleInstance());
                Degree srcDegree = ctSrc.fitDegreeAdvanced(di, chordSymbol.getRenderingInfo().getScaleInstance());
                if (srcDegrees.contains(srcDegree))
                {
                    // Map the srcDegree only if it's really present in the source phrase
                    mapResult.put(srcDegree, destDegree);
                    degreeIndexes.remove(di);     // should not reuse it
                    srcDegrees.remove(srcDegree); // should not reuse it
                }
            }

            // Handle the remaining unmapped degrees: find the closest remaining destination degrees
            for (Degree srcDegree : srcDegrees)
            {
                // By default INVERSION_ALLOWED mode
                int srcPitch = chordSymbol.getRootNote().getPitch() + srcDegree.getPitch();
                if (chordMode.equals(ChordMode.NO_INVERSION))
                {
                    srcPitch = ecsDest.getRootNote().getPitch() + srcDegree.getPitch();
                }
                Note srcNote = new Note(srcPitch);

                ChordType.DegreeIndex closestDegreeIndex = null;
                int smallestPitchDelta = 100000;
                for (ChordType.DegreeIndex di : degreeIndexes.toArray(DegreeIndex[]::new))
                {
                    int destRelPitch = ecsDest.getRelativePitch(di);
                    int pitchDelta = Math.abs(srcNote.getRelativePitchDelta(destRelPitch));
                    if (closestDegreeIndex == null || pitchDelta < smallestPitchDelta)
                    {
                        smallestPitchDelta = pitchDelta;
                        closestDegreeIndex = di;
                    }
                }
                Degree destDegree = ctDest.fitDegreeAdvanced(closestDegreeIndex, ecsDest.getRenderingInfo().getScaleInstance());
                mapResult.put(srcDegree, destDegree);
                degreeIndexes.remove(closestDegreeIndex);     // should not reuse it            
            }


        } else
        {
            // Special case : a "complex" source phrase needs to be adapted to a simpler destination chord
            // Ex: ecsSrc=C7M, pSrc=C,E,G,B, and ecsDest=C
            // ==> some dest degrees must be reused 

            // Take all the "most important notes" of the destination degrees
            List<DegreeIndex> degreeIndexes = miDestDegreeIndexes;

            for (DegreeIndex di : degreeIndexes.toArray(DegreeIndex[]::new))
            {
                Degree destDegree = ctDest.fitDegreeAdvanced(di, ecsDest.getRenderingInfo().getScaleInstance());
                Degree srcDegree = ctSrc.fitDegreeAdvanced(di, chordSymbol.getRenderingInfo().getScaleInstance());
                if (srcDegrees.contains(srcDegree))
                {
                    // Map the srcDegree only if it's really present in the source phrase
                    mapResult.put(srcDegree, destDegree);
                    srcDegrees.remove(srcDegree); // should not reuse it
                }
            }

            // Now reuse one or more "most important notes" to map the remaining unmapped source degrees
            for (Degree srcDegree : srcDegrees)
            {
                int srcPitch = chordSymbol.getRootNote().getPitch() + srcDegree.getPitch();
                if (chordMode.equals(ChordMode.NO_INVERSION))
                {
                    srcPitch = ecsDest.getRootNote().getPitch() + srcDegree.getPitch();
                }
                Note srcNote = new Note(srcPitch);

                // Search the destination degreeIndex closest from the remaining source note
                ChordType.DegreeIndex closestDegreeIndex = null;
                int smallestPitchDelta = 100000;
                for (ChordType.DegreeIndex di : degreeIndexes)
                {
                    int destRelPitch = ecsDest.getRelativePitch(di);
                    int pitchDelta = Math.abs(srcNote.getRelativePitchDelta(destRelPitch));
                    if (closestDegreeIndex == null || pitchDelta < smallestPitchDelta)
                    {
                        smallestPitchDelta = pitchDelta;
                        closestDegreeIndex = di;
                    }
                }
                Degree destDegree = ctDest.fitDegreeAdvanced(closestDegreeIndex, ecsDest.getRenderingInfo().getScaleInstance());
                mapResult.put(srcDegree, destDegree);
            }
        }
        return mapResult;
    }

}

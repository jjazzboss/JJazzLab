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

import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Factory;
import org.jjazz.leadsheet.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.leadsheet.chordleadsheet.api.item.VoidAltExtChordSymbol;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.parameters.RP_SYS_Marker;
import org.jjazz.rhythm.parameters.RhythmParameter;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.util.IntRange;

/**
 * A ChordSequence built for a given MusicGenerationContext.
 * <p>
 * When constructed a ContextChordSequence always has a starting chord symbol.
 */
public class ContextChordSequence extends ChordSequence
{

    private MusicGenerationContext context;
    protected static final Logger LOGGER = Logger.getLogger(ContextChordSequence.class.getSimpleName());

    /**
     * Build a ChordSequence from the specified MusicGenerationContext.
     * <p>
     * Use the song's SongStructure and ChordLeadSheet, limited to the context range, to build this ChordSequence. Process the
     * alternate chord symbols when relevant. Make sure that the created object has a ChordSymbol at beginning.<br>
     * Example: <br>
     * - ChordLeadSheet: Section B1: bar0=Cm7, bar1=empty Section B2: bar2=Bb bar3=empty<br>
     * - SongStructure: B1 B2 B1 <br>
     * - Range: [bar1; bar5] Method returns: Cm7(bar1), Bb(bar2), Cm7(bar4), empty(bar5)
     *
     * @param context
     * @throws IllegalArgumentException If no chord found to be the 1st chord of the ChordSequence.
     */
    public ContextChordSequence(MusicGenerationContext context)
    {
        super(context.getBarRange().from, context.getBarRange().to - context.getBarRange().from + 1);

        this.context = context;

        ChordLeadSheet cls = this.context.getSong().getChordLeadSheet();
        CLI_Factory clif = CLI_Factory.getDefault();

        // Process all SongParts in the range
        for (SongPart spt : this.context.getSongParts())
        {
            IntRange clsRange = toClsRange(spt);
            RP_SYS_Marker rpMarker = RP_SYS_Marker.getMarkerRp(spt.getRhythm());
            String sptMarker = (rpMarker == null) ? null : spt.getRPValue(rpMarker);

            for (CLI_ChordSymbol cliCs : cls.getItems(clsRange.from, clsRange.to, CLI_ChordSymbol.class))
            {
                Position pos = cliCs.getPosition();
                ExtChordSymbol ecs = cliCs.getData();
                int absoluteBar = spt.getStartBarIndex() + pos.getBar() - spt.getParentSection().getPosition().getBar();

                // Prepare the ChordSymbol copy to be added
                Position newPos = new Position(absoluteBar, pos.getBeat());
                ExtChordSymbol newEcs = ecs.getChordSymbol(sptMarker);      // Use alternate chord symbol if relevant      

                // Don't allow Void chordsymbol if it's the init chord symbol
                if (newEcs == VoidAltExtChordSymbol.getInstance() && newPos.equals(new Position(0, 0)))
                {
                    LOGGER.info("ContextChordSequence() Can't use the void alternate chord symbol of " + ecs.getName() + " at initial position.");   //NOI18N
                    newEcs = ecs;
                }

                // Add the ChordSymbol to this ChordSequence
                if (newEcs != VoidAltExtChordSymbol.getInstance())
                {
                    CLI_ChordSymbol newCliCs = clif.createChordSymbol(cls, newEcs, newPos);
                    add(newCliCs);
                }
            }
        }

        if (!hasChordAtBeginning())
        {
            // This must be because the range starts in the middle of the first section                
            SongPart spt0 = this.context.getSongParts().get(0);
            IntRange clsRange = toClsRange(spt0);
            assert clsRange.from > 0 : "clsRange=" + clsRange;   //NOI18N
            List<? extends CLI_ChordSymbol> items = cls.getItems(0, clsRange.from - 1, CLI_ChordSymbol.class);
            CLI_ChordSymbol prevCliCs = items.get(items.size() - 1);        // Take the last chord before the range
            CLI_ChordSymbol newCs = getInitCopy(prevCliCs);
            add(0, newCs);      // Add at first position                    
            LOGGER.log(Level.FINE, "fixChordSequence()   lacking a starting chord. Add a copy of previous chord={0}", newCs);   //NOI18N
        }
    }

    public MusicGenerationContext getContext()
    {
        return context;
    }

    /**
     * Split this in ChordSequences for each context's contiguous Rhythm's SongParts which have the same specified RhythmParameter
     * value.
     * <p>
     * The resulting ChordSequences will have a starting chord symbol.
     * <p>
     * Example: <br>
     * Spt0 rpValue=Main A-1 chords=Cm7 F7<br>
     * Spt1 rpValue=Main A-1 chords=Bbm7 Eb7<br>
     * Spt2 rpValue=Main B-2 chords=F7M Dm7<br>
     * Then return 1 chordSequence for Main A-1=Spt0+Spt1="Cm7 F7 Bbm7 Eb7", and 1 chordSequence for Main B-2=Spt2=F7M Dm7<br>
     *
     * @param <T> The type of the RhythmParameter value
     * @param r
     * @param rp The Rhythm's RhythmParameter for which we will check the value
     * @return The list of ChordSequences with their respective common rpValue.
     */
    public <T> HashMap<ChordSequence, T> split(Rhythm r, RhythmParameter<T> rp)
    {
        LOGGER.fine("split() --");   //NOI18N
        HashMap<ChordSequence, T> mapCsRpValue = new HashMap<>();
        int seqStartBar = getStartBar();
        int seqEndBar = seqStartBar;
        T lastRpValue = null;
        for (SongPart spt : context.getSongParts())
        {
            IntRange sptCsRange = context.getSptBarRange(spt);
            if (spt.getRhythm() == r)
            {
                // Song part is covered by this ChordSequence and it's our rhythm
                T rpValue = spt.getRPValue(rp);
                if (lastRpValue == null)
                {
                    // Start a new chord sequence
                    seqStartBar = sptCsRange.from;
                    seqEndBar = sptCsRange.to;
                    lastRpValue = rpValue;
                } else if (lastRpValue == rpValue)
                {
                    // Different song parts with same rpValues: we continue the current chord sequence                    
                    seqEndBar += sptCsRange.size();
                } else
                {
                    // Different song parts with different rpValues: complete the chord sequence and start a new one
                    ChordSequence cSeq = subSequence(seqStartBar, seqEndBar, true);
                    mapCsRpValue.put(cSeq, lastRpValue);
                    seqStartBar = sptCsRange.from;
                    seqEndBar = sptCsRange.to;
                    lastRpValue = rpValue;
                }
            } else
            {
                // Not our rhythm
                if (lastRpValue != null)
                {
                    // We have one chord sequence pending, save it
                    ChordSequence cSeq = subSequence(seqStartBar, seqEndBar, true);
                    mapCsRpValue.put(cSeq, lastRpValue);
                    lastRpValue = null;
                }
            }
        }

        if (lastRpValue != null)
        {
            // Complete the last chord sequence 
            ChordSequence cSeq = subSequence(seqStartBar, seqEndBar, true);
            mapCsRpValue.put(cSeq, lastRpValue);
        }
        LOGGER.log(Level.FINE, "split()   mapCsSp={0}", mapCsRpValue.toString());   //NOI18N
        return mapCsRpValue;
    }

    // ====================================================================================
    // Private methods
    // ====================================================================================
    private IntRange toClsRange(SongPart spt)
    {
        IntRange sptRange = context.getSptBarRange(spt);
        CLI_Section section = spt.getParentSection();
        int sectionStartBar = section.getPosition().getBar();
        IntRange r = new IntRange(sectionStartBar + sptRange.from - spt.getStartBarIndex(), sectionStartBar + sptRange.to - spt.getStartBarIndex());
        return r;
    }

}

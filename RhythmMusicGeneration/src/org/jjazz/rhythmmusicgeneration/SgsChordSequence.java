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
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.item.AltExtChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Factory;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordRenderingInfo;
import org.jjazz.leadsheet.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.leadsheet.chordleadsheet.api.item.VoidAltExtChordSymbol;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.parameters.RP_SYS_Marker;
import org.jjazz.rhythm.parameters.RhythmParameter;
import org.jjazz.rhythmmusicgeneration.spi.MusicGenerationException;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;

/**
 * A special ChordSequence built from a SongStructure and its parent ChordLeadSheet.
 */
public class SgsChordSequence extends ChordSequence
{

    private SongStructure sgs;
    protected static final Logger LOGGER = Logger.getLogger(SgsChordSequence.class.getSimpleName());

    /**
     * Build a ChordSequence with all the chords of the specified SongStructure.
     *
     * @param sgs
     */
    public SgsChordSequence(SongStructure sgs)
    {
        this(sgs, 0, sgs.getSizeInBars() - 1);
    }

    /**
     * Build a ChordSequence from the specified SongStructure and its parent ChordLeadSheet.
     * <p>
     * Use the alternate chord symbols when relevant.<br>
     * If sgs's parentChordLeadSheet is null, nothing is done.<br>
     * Make sure returned ChordSequences has a ChordSymbol at beginning.<br>
     * Example: <br>
     * - ChordLeadSheet: Section B1: bar0=Cm7, bar1=F7 Section B2: bar2-3=Bb<br>
     * - SongStructure: B1 B2 B1 <br>
     * Then getChordSequence(0,5) returns: Cm7(bar0), F7(bar1), Bb(bar2), Cm7(bar4), F7(bar5)
     *
     * @param sgs
     * @param absoluteStartBar Include chords from this bar.
     * @param absoluteEndBar Include chords until this bar (included).
     * @throws IllegalArgumentException If no chord found to be the 1st chord of the ChordSequence.
     */
    public SgsChordSequence(SongStructure sgs, int absoluteStartBar, int absoluteEndBar)
    {
        super(absoluteStartBar, absoluteEndBar - absoluteStartBar + 1);
        if (sgs == null)
        {
            throw new IllegalArgumentException("sgs=" + sgs + " absoluteStartBar=" + absoluteEndBar + " absoluteEndBar=" + absoluteEndBar);
        }
        this.sgs = sgs;
        ChordLeadSheet cls = sgs.getParentChordLeadSheet();
        if (cls == null)
        {
            // Nothing to do
            return;
        }
        CLI_Factory clif = CLI_Factory.getDefault();
        for (SongPart spt : sgs.getSongParts())
        {
            int sptStartBar = spt.getStartBarIndex();
            CLI_Section section = spt.getParentSection();
            Position sectionPos = section.getPosition();
            RP_SYS_Marker rpMarker = RP_SYS_Marker.getMarkerRp(spt.getRhythm());
            String sptMarker = rpMarker == null ? null : spt.getRPValue(rpMarker);
            for (CLI_ChordSymbol cliCs : cls.getItems(section, CLI_ChordSymbol.class))
            {
                Position pos = cliCs.getPosition();
                ExtChordSymbol ecs = cliCs.getData();
                int absoluteBar = sptStartBar + pos.getBar() - sectionPos.getBar();
                if (absoluteBar >= absoluteStartBar && absoluteBar <= absoluteEndBar)
                {
                    Position newPos = new Position(absoluteBar, pos.getBeat());
                    ExtChordSymbol newEcs = ecs.getChordSymbol(sptMarker);  // Use alternate chord symbol if relevant      
                    if (newEcs == VoidAltExtChordSymbol.getInstance() && isEmpty() && pos.getBar() == 0 && pos.isFirstBarBeat())
                    {
                        // Special case: this is the chordleadsheet's initial chord which is also the initial chord of the SongStructure.
                        // If it's Void we won't be able to find another valid starting chord, so we can't use this Void alternate chord symbol.
                        newEcs = ecs;
                        LOGGER.info("SgsChordSequence() chord symbol " + cliCs.toString() + " is used as initial chord: ignoring its Void alternate chord symbol.");
                    }
                    if (newEcs == VoidAltExtChordSymbol.getInstance())
                    {
                        // Nothing : don't add a chord symbol
                    } else
                    {
                        CLI_ChordSymbol newCs = clif.createChordSymbol(cls, newEcs, newPos);
                        add(newCs);
                    }
                }
            }
        }
        if (!hasChordAtBeginning())
        {
            // There must be 1 or more sections (unused in the SongStructure) before the section corresponding to the 1st SongPart.
            // Add the last chord of these sections.
            SongPart spt0 = sgs.getSongPart(0);
            RP_SYS_Marker rpMarker0 = RP_SYS_Marker.getMarkerRp(spt0.getRhythm());
            String sptMarker0 = rpMarker0 == null ? null : spt0.getRPValue(rpMarker0);
            CLI_Section parentSection = spt0.getParentSection();
            int parentSectionBar = parentSection.getPosition().getBar();
            CLI_ChordSymbol newCs = null;
            if (parentSectionBar > 0)
            {
                List<? extends CLI_ChordSymbol> items = cls.getItems(0, parentSectionBar - 1, CLI_ChordSymbol.class);
                if (!items.isEmpty())
                {
                    CLI_ChordSymbol cliCs = items.get(items.size() - 1);
                    ExtChordSymbol ecs = cliCs.getData();
                    Position newPos = new Position(getStartBar(), 0);
                    ExtChordSymbol newEcs = ecs.getChordSymbol(sptMarker0);         // Use alternate chord symbol if relevant      
                    if (newEcs == VoidAltExtChordSymbol.getInstance())
                    {
                        // We can't accept an empty alternate data here, use the normal data instead
                        newEcs = ecs;
                        LOGGER.info("SgsChordSequence() chord symbol " + cliCs.toString() + " is used as initial chord: ignoring its Void alternate chord symbol.");
                    }
                    newCs = clif.createChordSymbol(cls, newEcs, newPos);
                }
            }
            if (newCs != null)
            {
                add(0, newCs);
            } else
            {
                throw new IllegalArgumentException(
                        "Can't find an initial ChordSymbol for the ChordSequence. Parent chordleadsheet=" + cls + ", sgs=" + sgs);
            }
        }
    }

    public SongStructure getSongStructure()
    {
        return sgs;
    }

    /**
     * Split in one or more chord sequences which share the same time signature.
     *
     * @return
     */
    public List<ChordSequence> splitByTimeSignature()
    {
        List<ChordSequence> res = new ArrayList<>();
        TimeSignature ts = null;
        int startBar = -1;
        for (SongPart spt : sgs.getSongParts())
        {
            if (!spt.getRhythm().getTimeSignature().equals(ts))
            {
                if (startBar != -1)
                {
                    // Add a subsequence
                    ChordSequence cSeq = subSequence(startBar, spt.getStartBarIndex() - 1);
                    res.add(cSeq);
                }
                startBar = spt.getStartBarIndex();
                ts = spt.getRhythm().getTimeSignature();
            }
        }
        // Add the last sequence
        ChordSequence cSeq = subSequence(startBar, sgs.getSizeInBars() - 1);
        res.add(cSeq);
        return res;
    }

    /**
     * Make sure that the specified ChordSequence starts with a chord symbol on beat 0.
     * <p>
     * If not, try to add a copy of the last chord before the start of the chord sequence. If no chord before, try to move the
     * first chord of the sequence on beat 0.
     *
     * @param cSeq A ChordSequence extracted from this SgsChordSequence.
     */
    public void fixChordSequenceStart(ChordSequence cSeq) throws MusicGenerationException
    {
        LOGGER.log(Level.FINER, "fixChordSequenceStart() -- cSeq=" + cSeq.toString());
        int startBar = cSeq.getStartBar();
        Position startPos = new Position(startBar, 0);
        CLI_Factory clif = CLI_Factory.getDefault();
        if (!cSeq.hasChordAtBeginning())
        {
            // No ChordSymbol at the beginning, try to fix it

            // Get the last chord before the previous sequence
            CLI_ChordSymbol prevCs = null;
            if (startBar > 0)
            {
                // Not first bar, try to reuse last chord from previous bar
                int prevCsIndex = indexOfLastBeforeBar(startBar);
                prevCs = (prevCsIndex != -1) ? get(prevCsIndex) : null;
            }

            if (prevCs == null)
            {
                // There is no chord before startBar, move the first chord of the sequence on beat 0
                if (!cSeq.isEmpty())
                {
                    CLI_ChordSymbol cliCs = cSeq.get(0);
                    LOGGER.log(Level.FINE, "fixChordSequence() cSeq is lacking a starting chord. Moving chord={0} to startPos={1}",
                            new Object[]
                            {
                                cliCs, startPos
                            });
                    cSeq.remove(0);
                    CLI_ChordSymbol newCs = (CLI_ChordSymbol) cliCs.getCopy(null, startPos);
                    cSeq.add(0, newCs);
                } else
                {
                    // No chord anywhere ! 
                    String msg = "cSeq is lacking a starting chord but no chord found before or after bar=" + startBar;
                    LOGGER.log(Level.SEVERE, "fixChordSequence() {0}", msg);
                    throw new MusicGenerationException(msg);
                }
            } else
            {
                // Put a copy of the previous chord symbol at the beginning of the chord sequence
                // As it is a copy, reset the ChordRenderingInfo PlayStyle and OffBeatStyle, and the alternate chord symbol
                ExtChordSymbol prevEcs = prevCs.getData();
                ChordRenderingInfo newCri = new ChordRenderingInfo(ChordRenderingInfo.PlayStyle.NORMAL,
                        true, prevEcs.getRenderingInfo().getScaleInstance());
                ExtChordSymbol newEcs = new ExtChordSymbol(prevEcs, newCri, null, null);
                CLI_ChordSymbol newCs = clif.createChordSymbol(prevCs.getContainer(), newEcs, startPos);
                cSeq.add(0, newCs);
                LOGGER.log(Level.FINE, "fixChordSequence()   lacking a starting chord. Add a copy of previous chord={0}", newCs);
            }
        }
        LOGGER.log(Level.FINER, "fixChordSequenceStart()   cSeq=" + cSeq.toString());
    }

    /**
     * Split this SgsChordSequence in ChordSequences for contiguous Rhythm's SongParts which have the same specified
     * RhythmParameter value.
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
        LOGGER.fine("split() --");
        HashMap<ChordSequence, T> mapCsRpValue = new HashMap<>();
        int startBar = 0;
        int endBar = 0;
        T lastRpValue = null;
        for (SongPart spt : sgs.getSongParts())
        {
            if (spt.getRhythm() == r)
            {
                T rpValue = spt.getRPValue(rp);
                if (lastRpValue == null)
                {
                    // Start a new chord sequence
                    startBar = spt.getStartBarIndex();
                    endBar = startBar + spt.getNbBars() - 1;
                    lastRpValue = rpValue;
                } else if (lastRpValue == rpValue)
                {
                    // Different song parts with same rpValues: we continue the current chord sequence                    
                    endBar += spt.getNbBars();
                } else
                {
                    // Different song parts with different rpValues: complete the chord sequence and start a new one
                    ChordSequence cSeq = subSequence(startBar, endBar);
                    mapCsRpValue.put(cSeq, lastRpValue);
                    startBar = spt.getStartBarIndex();
                    endBar = startBar + spt.getNbBars() - 1;
                    lastRpValue = rpValue;
                }
            } else
            {
                // Not our rhythm !
                if (lastRpValue != null)
                {
                    // We have one chord sequence pending, save it
                    ChordSequence cSeq = subSequence(startBar, endBar);
                    mapCsRpValue.put(cSeq, lastRpValue);
                    lastRpValue = null;
                }
            }
        }
        if (lastRpValue != null)
        {
            // Complete the last chord sequence 
            ChordSequence cSeq = subSequence(startBar, endBar);
            mapCsRpValue.put(cSeq, lastRpValue);
        }
        LOGGER.log(Level.FINE, "buildChordSequences()   mapCsSp={0}", mapCsRpValue.toString());
        return mapCsRpValue;
    }

}

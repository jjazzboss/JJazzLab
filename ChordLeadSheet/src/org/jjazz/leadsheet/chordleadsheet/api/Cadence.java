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
package org.jjazz.leadsheet.chordleadsheet.api;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.ExtChordSymbol;


/**
 * A series of chord symbols on 1, 2, or 4 bars, used for harmonic analysis.
 * <p>
 * There are 2 chord symbols per bar. This is an immutable class.
 */
public class Cadence
{

    public class UnsupportedChordPositionException extends Exception
    {

        public UnsupportedChordPositionException(String msg)
        {
            super(msg);
        }
    }


    private final List<ExtChordSymbol> chordSymbols;
    private final int nbBars;

    /**
     * Create a cadence with nbBars * 2 chordSymbols.
     *
     * @param nbBars
     * @param chordSymbols There must be exactly 2 * nbBars chord symbols.
     */
    public Cadence(int nbBars, List<ExtChordSymbol> chordSymbols)
    {
        checkArgument(nbBars == 1 || nbBars == 2 || nbBars == 4, "nbBars=%s", nbBars);
        checkArgument(chordSymbols != null && chordSymbols.size() == 2 * nbBars, "chordSymbols=%s", chordSymbols);
        this.nbBars = nbBars;
        this.chordSymbols = new ArrayList<>(chordSymbols);
    }

    /**
     * Create a cadence from chord symbols in a chord leadsheet.
     * <p>
     * Specified bars must have :<br>
     * - be in 4/4 or 3/4<br>
     * - no chord symbol (then the last active chord symbol is used)<br>
     * - 1 chord symbol on first beat or half-bar<br>
     * - 2 chord symbols on first beat and half-bar<br>
     * Otherwise an UnsupportedChordPositionException is thrown.
     *
     * @param cls
     * @param fromBar
     * @param nbBars
     * @throws org.jjazz.leadsheet.chordleadsheet.api.Cadence.UnsupportedChordPositionException
     */
//    public Cadence(ChordLeadSheet cls, int fromBar, int nbBars) throws UnsupportedChordPositionException
//    {
//        checkNotNull(cls);
//        checkArgument(nbBars == 1 || nbBars == 2 || nbBars == 4, "nbBars=%s", nbBars);
//        checkArgument(fromBar >= 0 && fromBar + nbBars - 1 < cls.getSizeInBars() - nbBars, "fromBar=%s cls=%s", fromBar, cls);
//        TimeSignature ts = cls.getSection(fromBar).getData().getTimeSignature();
//        checkArgument(ts.equals(TimeSignature.FOUR_FOUR) || ts.equals(TimeSignature.THREE_FOUR), " ts=%s", ts);
//
//        this.nbBars = nbBars;
//
//        for (int i = fromBar; i < fromBar + nbBars; i++)
//        {
//            checkArgument(cls.getSection(i).getData().getTimeSignature().equals(ts), " ts=%ts i=%s cls=%s", ts, i, cls);
//
//            var cliCsList = cls.getItems(i, i, CLI_ChordSymbol.class).stream().limit(2).collect(Collectors.toList());
//            ExtChordSymbol firstEcs, secondEcs;
//
//            if (cliCsList.isEmpty() )
//            {
//                firstEcs = cls.getLastItem(0, fromBar - 1, CLI_ChordSymbol.class).getData();
//                secondEcs = firstEcs;
//            } else if (cliCsList.size()==1 && (cliCsList.get(0).getPosition().isHalfBarBeat(ts, true) || cliCsList.get(0).getPosition().isHalfBarBeat(ts, false)))
//            {
//                firstEcs = cliCsList.get(0).getData();
//                secondEcs = 
//            }
//
//
//        }
//
//
//        this.chordSymbols = new ArrayList<>(chordSymbols);
//    }

    /**
     * There is always 2 chord symbols per bar.
     *
     * @return
     */
    public List<ExtChordSymbol> getChordSymbols()
    {
        return chordSymbols;
    }

    public int getNbBars()
    {
        return nbBars;
    }

}

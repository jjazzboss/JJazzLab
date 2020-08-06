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
package org.jjazz.chordsymboltextinput;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Factory;
import org.jjazz.leadsheet.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.quantizer.Quantization;
import org.jjazz.quantizer.Quantizer;

/**
 * Define methods how to convert CLI_ChordSymbols from/to Strings.
 */
public class ChordSymbolTextInput
{

    public final static char OPEN_POS_CHAR = Position.START_CHAR;
    public final static char CLOSE_POS_CHAR = Position.END_CHAR;
    private static final Logger LOGGER = Logger.getLogger(ChordSymbolTextInput.class.getSimpleName());

    public enum PositionDisplay
    {
        NO, // C7
        BEAT, // C7(2.4)
        BAR_BEAT // C7(3:2.4)
    }

    /**
     * Return a new CLI_ChordSymbol built from specified string.
     * <p>
     * Recognized strings:<br>
     * "C7" =&gt; C7 at pos=defaultPos<br>
     * "C7[2]" =&gt; C7 at pos=defaultPos.getBar()/beat 2<br>
     * "C7[1:2]" =&gt; C7 at pos=bar 1/beat 2
     * <p>
     * @param str As produced by ChordSymboInput.toString(CLI_ChordSymbol).
     * @param defaultPos Used when position data is missing in str.
     * @param cls The container for this CLI_ChordSymbol.
     * @return
     * @throws ParseException
     */
    public static CLI_ChordSymbol toCLI_ChordSymbol(String str, Position defaultPos, ChordLeadSheet cls) throws ParseException
    {
        if (str == null || defaultPos == null)
        {
            throw new IllegalArgumentException("str=" + str + " defaultPos=" + defaultPos + " cls=" + cls);
        }

        Position newPos;

        String s = str.trim();
        if (s.isEmpty())
        {
            throw new IllegalArgumentException("str=" + str + " defaultPos=" + defaultPos + " cls=" + cls);
        }
        int openIndex = s.indexOf(OPEN_POS_CHAR);
        int closeIndex = s.indexOf(CLOSE_POS_CHAR);

        if (openIndex == -1)
        {
            // No position provided, use defaultPos
            newPos = defaultPos;
            openIndex = s.length();

        } else
        {
            // Position is provided
            if (closeIndex == -1)
            {
                throw new ParseException(str + " : " + "Missing closing parenthesis", 0);
            }
            newPos = new Position();
            newPos.valueOf(s.substring(openIndex, closeIndex + 1), defaultPos.getBar());

        }

        // Chord Symbol
        String csStr = s.substring(0, openIndex);
        ExtChordSymbol ecs = new ExtChordSymbol(csStr);


        // Build the CLI_ChordSymbol
        CLI_ChordSymbol cli = CLI_Factory.getDefault().createChordSymbol(cls, ecs, newPos);
        return cli;
    }

    /**
     * Create CLI_ChordSymbols from a string containing chord symbols of one bar.
     * <p/>
     * Examples of valid specification strings : "C6(1) F7(3) Em7(4.5)" : Add chord C6 on beat 0, F7 on beat 2 and Em7 on beat
     * 3.5.
     * <p/>
     * There are special cases if chord symbols are specified without absolute positioning. These examples are based on a 4/4 bar
     * : "C7" : add C7 on first beat. "C7 F7" : add C7 on first beat and F7 on half of the bar, e.g. like "C7(1) F7(3)". "C7 F7
     * Gb7" : add C7 on first beat, F7 on half of the bar, Gb7 on last beat. Other nb of chord symbols : add the chords at regular
     * intervals in the bar.
     *
     * @param str The String describing the chord symbols.
     * @param barIndex The bar where CLI_ChordSymbol will belong to.
     * @param cls The container for the created CLI_ChordSymbols.
     *
     * @return A list of CLI_ChordSymbol.
     *
     * @throws ParseException If error ParseException.getErrorOffset() will return the index of the erroneous chord symbol.
     */
//   public static List<CLI_ChordSymbol> toCLI_ChordSymbols(String str, int barIndex, ChordLeadSheet cls) throws ParseException
//   {
//      if ((barIndex < 0) || str == null || cls == null || barIndex >= cls.getSize())
//      {
//         throw new IllegalArgumentException("str=" + str + " barIndex=" + barIndex + " cls=" + cls);
//      }
//
//      ArrayList<CLI_ChordSymbol> newItems = new ArrayList<>();
//
//      // Get each separate string
//      String s = str.trim();
//      if (s.isEmpty())
//      {
//         return newItems;
//      }
//      String[] rawStrings = s.split("\\s+");
//
//      // Special cases if there are only chord symbols without absolute positioning
//      if (s.indexOf(OPEN_POS_CHAR) == -1)
//      {
//         TimeSignature ts = cls.getSection(barIndex).getData().getTimeSignature();
//         Position pos;
//
//         // We must add ourself the position string
//         if (rawStrings.length == 1)
//         {
//            // Position on first beat
//            pos = new Position(barIndex, 0);
//            rawStrings[0] = rawStrings[0] + pos.toString();
//         } else if (rawStrings.length == 2)
//         {
//            // Position on first beat and half bar, and last beat
//            pos = new Position(barIndex, 0);
//            rawStrings[0] = rawStrings[0] + pos.toString();
//            pos = new Position(barIndex, ts.getHalfBarBeat());
//            rawStrings[1] = rawStrings[1] + pos.toString();
//         } else if ((rawStrings.length == 3) && (rawStrings.length != ts.getNbNaturalBeats()))
//         {
//            // Position on first beat and half bar, and last beat
//            pos = new Position(barIndex, 0);
//            rawStrings[0] = rawStrings[0] + pos.toString();
//            pos = new Position(barIndex, ts.getHalfBarBeat());
//            rawStrings[1] = rawStrings[1] + pos.toString();
//            pos = new Position(barIndex, ts.getNbNaturalBeats() - 1);
//            rawStrings[2] = rawStrings[2] + pos.toString();
//         } else
//         {
//            // Place chord symbols at regular intervals
//            float step = (float) ts.getNbNaturalBeats() / rawStrings.length;
//
//            for (int i = 0; i < rawStrings.length; i++)
//            {
//               pos = new Position(barIndex, i * step);
//               rawStrings[i] = rawStrings[i] + pos.toString();
//            }
//         }
//      }
//
//      // Here all chord symbols have absolute positioning
//      Position pos = new Position(barIndex, 0);
//      for (int i = 0; i < rawStrings.length; i++)
//      {
//         CLI_ChordSymbol cli = null;
//         try
//         {
//            cli = toCLI_ChordSymbol(rawStrings[i], pos, cls);
//         } catch (ParseException e)
//         {
//            // Change to a ParseException with offset being the index of the erroneous chord symbol
//            throw new ParseException(e.getLocalizedMessage(), i);
//         }
//         newItems.add(cli);
//      }
//
//      // Return the added events
//      return newItems;
//   }
    
    
    /**
     * Analyze a string describing a bar like "C Fm7" and return the list of CLI_ChordSymbols.
     * <p>
     * If 1 chord, beat=0.<br>
     * If 2 chords, second chord is at the "half" of the bar.<br>
     * If 3 chords, second chord is at the "half" of the bar and last one on last beat<br>
     * If more chords place them at regular intervals.
     *
     * @param str
     * @param barIndex
     * @param cls
     * @param swing If true for example for 3/4 time signature place half-beat chord symbols at 1.666 (5/3) instead of 1.5
     * @return
     * @throws ParseException
     */
    public static List<CLI_ChordSymbol> toCLI_ChordSymbolsNoPosition(String str, int barIndex, ChordLeadSheet cls, boolean swing) throws ParseException
    {
        if ((barIndex < 0) || str == null || cls == null || barIndex >= cls.getSize())
        {
            throw new IllegalArgumentException("str=" + str + " barIndex=" + barIndex + " cls=" + cls);
        }

        ArrayList<CLI_ChordSymbol> newItems = new ArrayList<>();

        String s = str.trim();
        if (s.isEmpty())
        {
            return newItems;
        }
        String[] rawStrings = s.split("\\s+");

        TimeSignature ts = cls.getSection(barIndex).getData().getTimeSignature();
        Position pos;
        if (rawStrings.length == 1)
        {
            // Position on first beat
            CLI_ChordSymbol i0 = ChordSymbolTextInput.toCLI_ChordSymbol(rawStrings[0], new Position(barIndex, 0), cls);
            newItems.add(i0);
        } else if (rawStrings.length == 2)
        {
            // Position on first beat and half bar
            CLI_ChordSymbol i0 = ChordSymbolTextInput.toCLI_ChordSymbol(rawStrings[0], new Position(barIndex, 0), cls);
            CLI_ChordSymbol i1 = ChordSymbolTextInput.toCLI_ChordSymbol(rawStrings[1], new Position(barIndex, ts.getHalfBarBeat(swing)), cls);
            newItems.add(i0);
            newItems.add(i1);
        } else if (rawStrings.length == 3 && ts.getNbNaturalBeats() > 3)
        {
            // Position on first beat and half bar, and last beat
            CLI_ChordSymbol i0 = ChordSymbolTextInput.toCLI_ChordSymbol(rawStrings[0], new Position(barIndex, 0), cls);
            CLI_ChordSymbol i1 = ChordSymbolTextInput.toCLI_ChordSymbol(rawStrings[1], new Position(barIndex, ts.getHalfBarBeat(swing)), cls);
            CLI_ChordSymbol i2 = ChordSymbolTextInput.toCLI_ChordSymbol(rawStrings[2], new Position(barIndex, ts.getNbNaturalBeats() - 1), cls);
            newItems.add(i0);
            newItems.add(i1);
            newItems.add(i2);
        } else if (rawStrings.length == 3)     // TimeSignature like 3/4 or 2/4
        {
            // Position on first beat and half bar, and last beat
            CLI_ChordSymbol i0 = ChordSymbolTextInput.toCLI_ChordSymbol(rawStrings[0], new Position(barIndex, 0), cls);
            CLI_ChordSymbol i1 = ChordSymbolTextInput.toCLI_ChordSymbol(rawStrings[1], new Position(barIndex, 1), cls);
            CLI_ChordSymbol i2 = ChordSymbolTextInput.toCLI_ChordSymbol(rawStrings[2], new Position(barIndex, ts.getNbNaturalBeats() - 1), cls);
            newItems.add(i0);
            newItems.add(i1);
            newItems.add(i2);
        } else if (rawStrings.length == 4 && ts.getNbNaturalBeats() >= 4)
        {
            // Position on first beat and half bar, and last beat
            CLI_ChordSymbol i0 = ChordSymbolTextInput.toCLI_ChordSymbol(rawStrings[0], new Position(barIndex, 0), cls);
            CLI_ChordSymbol i1 = ChordSymbolTextInput.toCLI_ChordSymbol(rawStrings[1], new Position(barIndex, 1), cls);
            CLI_ChordSymbol i2 = ChordSymbolTextInput.toCLI_ChordSymbol(rawStrings[2], new Position(barIndex, 2), cls);
            CLI_ChordSymbol i4 = ChordSymbolTextInput.toCLI_ChordSymbol(rawStrings[3], new Position(barIndex, 3), cls);
            newItems.add(i0);
            newItems.add(i1);
            newItems.add(i2);
            newItems.add(i4);
        } else
        {
            // Place chord symbols at half-beat intervals, excess chords on last beat
            float beat = 0;
            for (String rawString : rawStrings)
            {
                pos = new Position(barIndex, beat);
                pos = Quantizer.quantize(swing ? Quantization.ONE_THIRD_BEAT : Quantization.HALF_BEAT, pos, ts, barIndex);
                CLI_ChordSymbol cs = ChordSymbolTextInput.toCLI_ChordSymbol(rawString, pos, cls);
                newItems.add(cs);
                beat = Math.min(ts.getNbNaturalBeats() - 0.5f, beat + 0.5f);
            }
        }

        // Return the added events
        return newItems;
    }

    /**
     * Make a string representing the bar chord symbols.<br>
     * This string can be used as input for toCLI_ChordSymbols().
     *
     * @param ts The TimeSignature of the bar.
     * @param items The chord symbols in the bar.
     *
     * @return
     *
     */
    public static String toBarString(TimeSignature ts, List<? extends CLI_ChordSymbol> items)
    {
        if ((ts == null) || (items == null))
        {
            throw new IllegalArgumentException("ts=" + ts + " barEvents=" + items);
        }

        // Check if nb of items and their position match the special configuration
        // that don't show the positions.
        boolean configNoPosition = false;

        if (items.size() == 1)
        {
            // One event on start of the bar
            CLI_ChordSymbol i0 = items.get(0);
            configNoPosition = (i0.getPosition().getBeat() == 0);
        } else if (items.size() == 2)
        {
            // Two events on start and half of the bar
            CLI_ChordSymbol i0 = items.get(0);
            CLI_ChordSymbol i1 = items.get(1);
            configNoPosition = ((i0.getPosition().getBeat() == 0)
                    && (i1.getPosition().getBeat() == ts.getHalfBarBeat(true) || i1.getPosition().getBeat() == ts.getHalfBarBeat(false)));
        } else if ((items.size() == 3) && (items.size() != ts.getNbNaturalBeats()))
        {
            // Two events on start and half of the bar, on on last beat
            CLI_ChordSymbol i0 = items.get(0);
            CLI_ChordSymbol i1 = items.get(1);
            CLI_ChordSymbol i2 = items.get(2);
            configNoPosition = ((i0.getPosition().getBeat() == 0)
                    && (i1.getPosition().getBeat() == ts.getHalfBarBeat(true) || i1.getPosition().getBeat() == ts.getHalfBarBeat(false))
                    && (i2.getPosition().getBeat() == (ts.getNbNaturalBeats() - 1)));
        } else if (items.size() == ts.getNbNaturalBeats())
        {
            // As many chord symbols as natural beats
            float b = 0;

            for (CLI_ChordSymbol item : items)
            {
                configNoPosition = (item.getPosition().getBeat() == b);
                if (!configNoPosition)
                {
                    break;
                }
                b += ts.getNaturalBeat();
            }
        }

        // Build the string
        StringBuilder barString = new StringBuilder();

        for (CLI_ChordSymbol item : items)
        {
            String s = (configNoPosition) ? toString(item, PositionDisplay.NO) : toString(item, PositionDisplay.BEAT);
            String prefix = (barString.length() == 0) ? "" : " ";
            barString.append(prefix).append(s);
        }

        return barString.toString();
    }

    /**
     * A String like "C7 Am7 D"
     *
     * @param items
     * @return
     */
    public static String toStringNoPosition(List<? extends CLI_ChordSymbol> items)
    {
        StringBuilder str = new StringBuilder();
        for (CLI_ChordSymbol item : items)
        {
            String space = (str.length() == 0) ? "" : " ";
            str.append(space).append(toString(item, PositionDisplay.NO));
        }
        return str.toString();
    }

    /**
     * Return a CLI_ChordSymbol as a string. Returned String can be used as an input for toCLI_ChordSymbol().
     * <p>
     * @param cli
     * @param pDisplay Define how position is represented.
     * @return
     */
    public static String toString(CLI_ChordSymbol cli, PositionDisplay pDisplay)
    {
        StringBuilder str = new StringBuilder(cli.getData().getOriginalName());
        Position pos = cli.getPosition();
        switch (pDisplay)
        {
            case BEAT:
                str.append(OPEN_POS_CHAR).append(pos.getBeatAsUserString()).append(CLOSE_POS_CHAR);
                break;
            case BAR_BEAT:
                str.append(OPEN_POS_CHAR).append(pos.toUserString()).append(CLOSE_POS_CHAR);
                break;
        }
        return str.toString();
    }
}

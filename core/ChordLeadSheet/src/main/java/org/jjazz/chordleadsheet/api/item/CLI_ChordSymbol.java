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
package org.jjazz.chordleadsheet.api.item;

import com.google.common.base.Preconditions;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.harmony.api.Position;
import org.jjazz.harmony.api.StandardScaleInstance;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.harmony.spi.ScaleManager;
import org.jjazz.quantizer.api.Quantization;
import org.jjazz.quantizer.api.Quantizer;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.utilities.api.StringProperties;

/**
 * A ChordLeadSheetItem which uses ExtChordSymbol objects as data.
 * <p>
 */
public interface CLI_ChordSymbol extends ChordLeadSheetItem<ExtChordSymbol>
{

    public static final int POSITION_ORDER = 1000;

    public enum PositionDisplay
    {
        NO, // C7
        BEAT, // C7(2.4)
        BAR_BEAT // C7(3:2.4)
    }

    public static final DataFlavor DATA_FLAVOR = new DataFlavor(CLI_ChordSymbol.class, "Chord Symbol");


    /**
     * Return a new CLI_ChordSymbol built from a string.
     * <p>
     * Recognized strings:<br>
     * "C7" =&gt; C7 at pos=defaultPos<br>
     * "C7$lydian_b7" =&gt; same with lydian_b7 scale<br>
     * "C7[2]" =&gt; C7 at pos=defaultPos.getBar()/beat 2<br>
     * "Cm7[1:2]" =&gt; Cm7 at pos=bar 1/beat 2<br>
     * "Cm7[1:2]$phrygian" =&gt; same with phrygian scale<p>
     * NOTES:<br>
     * - bar and beat in string position string [bar:beat] must be 1-based.<br>
     * - scale name case does not matter. Scale must be compatible with the chord symbol, otherwise it is ignored.<br>
     * <p>
     *
     * @param str        As produced by toString(CLI_ChordSymbol).
     * @param defaultPos Used when position data is missing in str.
     * @param cls        The container for this CLI_ChordSymbol. Can be null.
     * @return
     * @throws ParseException
     * @see #toString(org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol, org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol.PositionDisplay)
     */
    public static CLI_ChordSymbol toCLI_ChordSymbol(String str, Position defaultPos, ChordLeadSheet cls) throws ParseException
    {
        if (str == null || defaultPos == null)
        {
            throw new IllegalArgumentException("str=" + str + " defaultPos=" + defaultPos + " cls=" + cls);
        }

        Position newPos;

        String s = str.strip();
        if (s.isEmpty())
        {
            throw new IllegalArgumentException("str=" + str + " defaultPos=" + defaultPos + " cls=" + cls);
        }

        // Check scale         
        final char SCALE_CHAR = '$';
        String scaleName = null;
        int scaleIndex = s.indexOf(SCALE_CHAR);
        if (scaleIndex != -1)
        {
            scaleName = s.substring(scaleIndex + 1).toLowerCase();
            s = s.substring(0, scaleIndex);
        }


        // Check position
        int openIndex = s.indexOf(Position.START_CHAR);
        int closeIndex = s.indexOf(Position.END_CHAR);

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
                throw new ParseException(str + " : " + ResUtil.getString(CLI_ChordSymbol.class, "MissingClosingBracket"), 0);
            }
            newPos = new Position();
            newPos.setFromString(s.substring(openIndex, closeIndex + 1), defaultPos.getBar(), true);

        }
        
        
        String csStr = s.substring(0, openIndex);
        ExtChordSymbol ecs = ExtChordSymbol.get(csStr);

        
        // Check scale       
        StandardScaleInstance stdScaleInstance = null;
        if (scaleName != null && !scaleName.isBlank())
        {
            var sm = ScaleManager.getDefault();
            var stdScaleInstances = sm.getMatchingScales(ecs);
            var scaleNameLc = scaleName.toLowerCase();
            stdScaleInstance = stdScaleInstances.stream()
                    .filter(sc -> sc.getScale().getName().toLowerCase().startsWith(scaleNameLc))
                    .findAny()
                    .orElse(null);

            if (stdScaleInstance == null)
            {
                var stdScale = sm.getStandardScale(scaleNameLc);
                if (stdScale == null)
                {
                    // It 's a wrong scale name
                    String validScaleNames = sm.getStandardScales().stream()
                            .map(sc -> sc.getName())
                            .collect(Collectors.joining(", "))
                            .toLowerCase();
                    throw new ParseException(str + " : " + ResUtil.getString(CLI_ChordSymbol.class, "InvalidModeOrScale", scaleNameLc, validScaleNames), 0);
                } else
                {
                    // Scale is incompatible with chord symbol
                    throw new ParseException(str + " : " + ResUtil.getString(CLI_ChordSymbol.class, "IncompatibleModeOrScale", stdScale.getName().toLowerCase(),
                            csStr), 0);
                }
            }
        }


        // Build the CLI_ChordSymbol
        ChordRenderingInfo cri = new ChordRenderingInfo(stdScaleInstance);
        ecs = ecs.getCopy(null, cri, null, null);
        CLI_ChordSymbol cli = CLI_Factory.getDefault().createChordSymbol(ecs, newPos);
        return cli;
    }


    /**
     * Analyze a string describing a bar like "C Fm7" and return the list of CLI_ChordSymbols.
     * <p>
     * If 1 chord, beat=0.<br>
     * If 2 chords, second chord is at the "half" of the bar.<br>
     * If 3 chords, second chord is at the "half" of the bar and last one on last beat<br>
     * If more chords place them at regular intervals.
     *
     * @param str
     * @param ts       Can be null if cls is non-null. If specified and cls is non-null, must be consistent with cls and time signature at barIndex.
     * @param cls      Can be null if ts is non-null.
     * @param barIndex Must be consistent with cls (if cls is specified)
     * @param swing    If true for example for 3/4 time signature place half-beat chord symbols at 1.666 (5/3) instead of 1.5
     * @return
     * @throws ParseException When thrown, GetErrorOffset() represents the faulty chord symbol index.
     * @see #toCLI_ChordSymbol(java.lang.String, org.jjazz.harmony.api.Position, org.jjazz.chordleadsheet.api.ChordLeadSheet)
     */
    public static List<CLI_ChordSymbol> toCLI_ChordSymbolsNoPosition(String str, TimeSignature ts, ChordLeadSheet cls, int barIndex, boolean swing) throws ParseException
    {
        Preconditions.checkNotNull(str);
        Preconditions.checkArgument(barIndex >= 0);
        if ((cls == null && ts == null)
                || (cls != null && barIndex >= cls.getSizeInBars())
                || (cls != null && ts != null && !ts.equals(cls.getSection(barIndex).getData().getTimeSignature())))
        {
            throw new IllegalArgumentException("str=" + str + " ts=" + ts + " cls=" + cls + " barIndex=" + barIndex + " swing=" + swing);
        }

        ArrayList<CLI_ChordSymbol> newItems = new ArrayList<>();

        String s = str.trim();
        if (s.isEmpty())
        {
            return newItems;
        }
        String[] rawStrings = s.split("\\s+");

        if (ts == null)
        {
            assert cls != null;
            ts = cls.getSection(barIndex).getData().getTimeSignature();
        }

        Position pos;
        int errorChordIndex = 0;

        try
        {
            if (rawStrings.length == 1)
            {
                // Position on first beat
                CLI_ChordSymbol i0 = toCLI_ChordSymbol(rawStrings[0], new Position(barIndex), cls);
                newItems.add(i0);
            } else if (rawStrings.length == 2)
            {
                // Position on first beat and half bar
                CLI_ChordSymbol i0 = toCLI_ChordSymbol(rawStrings[0], new Position(barIndex), cls);
                errorChordIndex++;
                CLI_ChordSymbol i1 = toCLI_ChordSymbol(rawStrings[1], new Position(barIndex, ts.getHalfBarBeat(swing)), cls);
                newItems.add(i0);
                newItems.add(i1);
            } else if (rawStrings.length == 3 && ts.getNbNaturalBeats() > 3)
            {
                // Position on first beat and half bar, and last beat
                CLI_ChordSymbol i0 = toCLI_ChordSymbol(rawStrings[0], new Position(barIndex), cls);
                errorChordIndex++;
                CLI_ChordSymbol i1 = toCLI_ChordSymbol(rawStrings[1], new Position(barIndex, ts.getHalfBarBeat(swing)), cls);
                errorChordIndex++;
                CLI_ChordSymbol i2 = toCLI_ChordSymbol(rawStrings[2], new Position(barIndex, ts.getNbNaturalBeats() - 1), cls);
                newItems.add(i0);
                newItems.add(i1);
                newItems.add(i2);
            } else if (rawStrings.length == 3)     // TimeSignature like 3/4 or 2/4
            {
                // Position on first beat and half bar, and last beat
                CLI_ChordSymbol i0 = toCLI_ChordSymbol(rawStrings[0], new Position(barIndex), cls);
                errorChordIndex++;
                CLI_ChordSymbol i1 = toCLI_ChordSymbol(rawStrings[1], new Position(barIndex, 1), cls);
                errorChordIndex++;
                CLI_ChordSymbol i2 = toCLI_ChordSymbol(rawStrings[2], new Position(barIndex, ts.getNbNaturalBeats() - 1), cls);
                newItems.add(i0);
                newItems.add(i1);
                newItems.add(i2);
            } else if (rawStrings.length == 4 && ts.getNbNaturalBeats() >= 4)
            {
                // Position on first beat and half bar, and last beat
                CLI_ChordSymbol i0 = toCLI_ChordSymbol(rawStrings[0], new Position(barIndex), cls);
                errorChordIndex++;
                CLI_ChordSymbol i1 = toCLI_ChordSymbol(rawStrings[1], new Position(barIndex, 1), cls);
                errorChordIndex++;
                CLI_ChordSymbol i2 = toCLI_ChordSymbol(rawStrings[2], new Position(barIndex, 2), cls);
                errorChordIndex++;
                CLI_ChordSymbol i4 = toCLI_ChordSymbol(rawStrings[3], new Position(barIndex, 3), cls);
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
                    pos = Quantizer.getQuantized(swing ? Quantization.ONE_THIRD_BEAT : Quantization.HALF_BEAT, pos, ts, 1f, barIndex);
                    CLI_ChordSymbol cs = toCLI_ChordSymbol(rawString, pos, cls);
                    errorChordIndex++;
                    newItems.add(cs);
                    beat = Math.min(ts.getNbNaturalBeats() - 0.5f, beat + 0.5f);
                }
            }
        } catch (ParseException ex)
        {
            // Throw a new ParseException with error offset correctly set for str
            assert errorChordIndex < rawStrings.length :
                    "errorChordIndex=" + errorChordIndex + " rawStrings.length=" + rawStrings.length + " str=" + str;
            throw new ParseException(ex.getLocalizedMessage(), errorChordIndex);
        }

        // Return the added events
        return newItems;
    }

    /**
     * Build a string representing the bar chord symbols.<br>
     * This string can be used as input for toCLI_ChordSymbols().
     *
     * @param ts    The TimeSignature of the bar.
     * @param items The chord symbols in the bar.
     *
     * @return
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
        return items.stream()
                .map(item -> toString(item, PositionDisplay.NO))
                .collect(Collectors.joining(" "));
    }

    /**
     * Return a CLI_ChordSymbol as a string.
     * <p>
     * Returned String can be used as an input for toCLI_ChordSymbol().
     *
     * @param cli
     * @param pDisplay Define how position is represented.
     * @return
     * @see #toCLI_ChordSymbol(java.lang.String, org.jjazz.harmony.api.Position, org.jjazz.chordleadsheet.api.ChordLeadSheet)
     */
    public static String toString(CLI_ChordSymbol cli, PositionDisplay pDisplay)
    {
        StringBuilder str = new StringBuilder(cli.getData().getOriginalName());
        Position pos = cli.getPosition();
        switch (pDisplay)
        {
            case BEAT -> str.append(Position.START_CHAR).append(pos.getBeatAsUserString()).append(Position.END_CHAR);
            case BAR_BEAT -> str.append(Position.START_CHAR).append(pos.toUserString()).append(Position.END_CHAR);
        }
        return str.toString();
    }


    /**
     * Create an item right after the specified position for comparison purposes.
     * <p>
     * For the Comparable interface, any item whose position is before (or equal if inclusive is true) to pos will be considered BEFORE the returned item.
     *
     * @param pos
     * @param inclusive
     * @return
     */
    public static CLI_ChordSymbol createItemTo(Position pos, boolean inclusive)
    {
        return new ComparableCsItem(pos, false, inclusive);
    }

    /**
     * Create an item at the end of the specified bar for comparison purposes.
     * <p>
     * For the Comparable interface, any normal item in the bar will be considered BEFORE the returned item.
     *
     * @param bar
     * @return
     */
    public static CLI_ChordSymbol createItemTo(int bar)
    {
        return new ComparableCsItem(new Position(bar, Float.MAX_VALUE), false, true);
    }

    /**
     * Create an item right before the specified position for comparison purposes.
     * <p>
     * For the Comparable interface, any item whose position is after (or equal if inclusive is true) to pos will be considered AFTER the returned item.
     *
     * @param pos
     * @param inclusive
     * @return
     */
    public static CLI_ChordSymbol createItemFrom(Position pos, boolean inclusive)
    {
        return new ComparableCsItem(pos, true, inclusive);
    }

    /**
     * Create an item at the beginning of the specified bar for comparison purposes.
     * <p>
     * For the Comparable interface, any normal item in the bar will be considered AFTER the returned item.
     *
     * @param bar
     * @return
     */
    public static CLI_ChordSymbol createItemFrom(int bar)
    {
        return new ComparableCsItem(new Position(bar), true, true);
    }


    // ==================================================================================================
    // Inner classes
    // ==================================================================================================
    /**
     * A dummy CLI_ChordSymbol class which can be used only for position comparison when using the NavigableSet/SortedSet-based methods of ChordLeadSheet or
     * ChordSequence.
     */
    public static class ComparableCsItem implements CLI_ChordSymbol
    {

        private final int positionOrder;
        private final Position position;

        /**
         *
         * @param pos
         * @param beforeOrAfterItem If true it's a "before item", otherwise an "after" item
         * @param inclusive         If true other items at same pos should be included
         */
        private ComparableCsItem(Position pos, boolean beforeOrAfterItem, boolean inclusive)
        {
            Objects.requireNonNull(pos);
            this.position = pos;

            if (beforeOrAfterItem)
            {
                positionOrder = inclusive ? Integer.MIN_VALUE : Integer.MAX_VALUE;
            } else
            {
                positionOrder = inclusive ? Integer.MAX_VALUE : Integer.MIN_VALUE;
            }
        }

        @Override
        public int getPositionOrder()
        {
            return positionOrder;
        }

        @Override
        public ChordLeadSheet getContainer()
        {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public CLI_ChordSymbol getCopy(ExtChordSymbol newData, Position newPos)
        {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public boolean isBarSingleItem()
        {
            return false;
        }

        @Override
        public ExtChordSymbol getData()
        {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public Position getPosition()
        {
            return new Position(position);
        }

        @Override
        public void addPropertyChangeListener(PropertyChangeListener listener)
        {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public void removePropertyChangeListener(PropertyChangeListener listener)
        {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public DataFlavor[] getTransferDataFlavors()
        {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor)
        {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException
        {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public String toString()
        {
            return getClass().getSimpleName() + "[" + getPosition() + ", posOrder=" + positionOrder + "]";
        }

        @Override
        public StringProperties getClientProperties()
        {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }
    }

}

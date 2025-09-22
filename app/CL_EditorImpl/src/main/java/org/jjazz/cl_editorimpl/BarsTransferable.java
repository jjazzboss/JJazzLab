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
package org.jjazz.cl_editorimpl;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.ChordLeadSheetFactory;
import org.jjazz.chordleadsheet.api.Section;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.harmony.api.Position;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.importers.api.TextReader;
import org.jjazz.utilities.api.IntRange;
import org.openide.util.Exceptions;

/**
 * A transferable for copy/cut/paste operations on bars.
 * <p>
 * Provides 2 data flavors: custom DATA_FLAVOR and javaStringFlavor.
 */
public class BarsTransferable implements Transferable
{

    public static final DataFlavor DATA_FLAVOR = new DataFlavor(BarsTransferable.Data.class, "Items");
    public static final DataFlavor[] DATA_FLAVORS = new DataFlavor[]
    {
        DATA_FLAVOR, DataFlavor.stringFlavor
    };

    private final Data data;

    public BarsTransferable(Data data)
    {
        this.data = data;
    }


    @Override
    public DataFlavor[] getTransferDataFlavors()
    {
        return DATA_FLAVORS;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor)
    {
        return Arrays.asList(DATA_FLAVORS).contains(flavor);
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException
    {
        if (!isDataFlavorSupported(flavor))
        {
            throw new UnsupportedFlavorException(flavor);
        }
        Object res = data;      // DATA_FLAVOR by default
        if (flavor == DataFlavor.stringFlavor)
        {
            res = TextReader.toText(buildCls(), 4, true);
        }

        return res;
    }


    // ========================================================================================================
    // Private methods
    // ========================================================================================================
    /**
     * Create a ChordLeadSheet from the stored items.
     * <p>
     * Shift items so that barRange.from becomes bar 0.
     *
     * @return
     */
    private ChordLeadSheet buildCls()
    {

        var res = ChordLeadSheetFactory.getDefault().createEmptyLeadSheet(data.firstBarSection.getName(), data.firstBarSection.getTimeSignature(),
                data.barRange.size(), null);


        for (var item : data.getItemsCopy(0))
        {
            if (item instanceof CLI_Section sectionItem)
            {
                try
                {
                    res.addSection(sectionItem);
                } catch (UnsupportedEditException ex)
                {
                    // Should never happen
                    Exceptions.printStackTrace(ex);
                }
            } else
            {
                res.addItem(item);
            }
        }


        return res;
    }

    // ========================================================================================================
    // Inner classes
    // ========================================================================================================
    /**
     * Store a bar range plus an ordered list of ChordLeadSheetItems.
     */
    public static class Data
    {

        private final TreeSet<ChordLeadSheetItem> items = new TreeSet<>();
        private final IntRange barRange;
        private final Section firstBarSection;


        /**
         *
         * @param firstBarSection
         * @param barRange
         * @param items           Can be empty
         */
        public Data(Section firstBarSection, IntRange barRange, List<? extends ChordLeadSheetItem> items)
        {
            Objects.requireNonNull(firstBarSection);
            Objects.requireNonNull(barRange);
            Objects.requireNonNull(items);
            items.stream().forEach(item -> this.items.add(item.getCopy(null, null)));
            this.barRange = barRange;
            this.firstBarSection = firstBarSection;
        }

        /**
         * @return int The number of ChordLeadSheetItems
         */
        public int getItemsSize()
        {
            return items.size();
        }

        public IntRange getBarRange()
        {
            return barRange;
        }

        public Section getFirstBarSection()
        {
            return firstBarSection;
        }

        /**
         * Return a copy of the items adjusted to targetBarIndex and with the specified container.
         * <p>
         * The items are shifted so that barRange.from matches targetBarIndex.
         *
         * @param targetBarIndex The barIndex where items are copied to. If barIndex&lt;0, positions are not changed.
         * @return Items are returned ordered by position.
         */
        public List<ChordLeadSheetItem> getItemsCopy(int targetBarIndex)
        {
            List<ChordLeadSheetItem> res = new ArrayList<>();
            if (!items.isEmpty())
            {
                int minBarIndex = items.first().getPosition().getBar();
                int itemShift = minBarIndex - barRange.from;
                int barShift = targetBarIndex < 0 ? 0 : targetBarIndex + itemShift - minBarIndex;
                for (ChordLeadSheetItem<?> item : items)
                {
                    Position newPos = item.getPosition().getMoved(barShift, 0);
                    ChordLeadSheetItem<?> newItem = item.getCopy(null, newPos);
                    res.add(newItem);
                }
            }

            return res;
        }
    }
}

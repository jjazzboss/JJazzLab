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
import java.util.TreeSet;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.ChordLeadSheetFactory;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.harmony.api.Position;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.importers.api.TextReader;
import org.openide.util.Exceptions;

/**
 * A transferable for copy/cut/paste operations on ChordLeadSheetItems.
 * <p>
 * Provides 2 data flavors: custom DATA_FLAVOR and javaStringFlavor.
 */
public class ItemsTransferable implements Transferable
{

    public static final DataFlavor DATA_FLAVOR = new DataFlavor(ItemsTransferable.Data.class, "Items");
    public static final DataFlavor[] DATA_FLAVORS = new DataFlavor[]
    {
        DATA_FLAVOR, DataFlavor.stringFlavor
    };

    private final Data data;

    public ItemsTransferable(Data data)
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
        Object res = flavor != DataFlavor.stringFlavor ? data : TextReader.toText(buildCls(), 4, true);

        return res;
    }

    // ========================================================================================================
    // Private methods
    // ========================================================================================================
    /**
     * Create a ChordLeadSheet from the stored items.
     * <p>
     * Shift items if required so that first item is on bar 0.
     *
     * @return
     */
    private ChordLeadSheet buildCls()
    {
        var cli0 = data.items.first();
        int bar0 = cli0.getPosition().getBar();
        int lastBar = data.items.last().getPosition().getBar();

        var cls = cli0.getContainer();
        TimeSignature ts0 = cls == null ? TimeSignature.FOUR_FOUR : cls.getSection(bar0).getData().getTimeSignature();

        var res = ChordLeadSheetFactory.getDefault().createEmptyLeadSheet("A", ts0, lastBar - bar0 + 1, null);
        for (var item : data.getItemsCopy(0))
        {
            if (item instanceof CLI_Section sectionItem)
            {
                try
                {
                    sectionItem = res.addSection(sectionItem);
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
     * Store an ordered list of ChordLeadSheetItems.
     */
    public static class Data
    {

        private final TreeSet<ChordLeadSheetItem> items = new TreeSet<>();

        /**
         *
         * @param items Can't be empty
         */
        public Data(List<? extends ChordLeadSheetItem> items)
        {
            if (items == null || items.isEmpty())
            {
                throw new IllegalArgumentException("items=" + items);
            }
            items.stream().forEach(item -> this.items.add(item.getCopy(null, null)));
        }

        /**
         * @return int The number of ChordLeadSheetItems
         */
        public int getItemsSize()
        {
            return items.size();
        }

        /**
         * Return a copy of the items adjusted to targetBarIndex.
         * <p>
         * The items are shitfed so the first item start at targetBarIndex.
         *
         * @param targetBarIndex The barIndex where items are copied to. If barIndex&lt;0, positions are not changed.
         * @return Items are returned ordered by position.
         */
        public List<ChordLeadSheetItem> getItemsCopy(int targetBarIndex)
        {
            List<ChordLeadSheetItem> res = new ArrayList<>();

            int barShift = targetBarIndex < 0 ? 0 : targetBarIndex - items.first().getPosition().getBar();
            for (ChordLeadSheetItem<?> item : items)
            {
                Position newPos = item.getPosition().getMoved(barShift, 0);
                ChordLeadSheetItem<?> newItem = item.getCopy(null, newPos);
                res.add(newItem);
            }

            return res;
        }
    }
}

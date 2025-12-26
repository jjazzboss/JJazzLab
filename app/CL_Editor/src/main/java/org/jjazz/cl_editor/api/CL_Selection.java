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
package org.jjazz.cl_editor.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.item.CLI_BarAnnotation;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.utilities.api.IntRange;
import org.openide.util.Lookup;

/**
 * Represent the selection of a CL_Editor, built from its Lookup.
 * <p>
 * Selected items can be either SelectedCLI or SelectedBar, but not both in the same time. Furthermore, a selection can not contain SelectedCLIs with
 * incompatible ChordLeadSheetItem classes (eg CLI_ChordSymbol and CLI_Section).
 *
 * @see SelectedBar
 * @see SelectedCLI
 */
@SuppressWarnings("unchecked")
final public class CL_Selection
{

    private final List<SelectedCLI> selectedCLIs;
    private final List<ChordLeadSheetItem> items;
    private final List<SelectedBar> selectedBars;
    private static final Logger LOGGER = Logger.getLogger(CL_Selection.class.getSimpleName());

    /**
     * Create a selection with items or bars.
     *
     * @param lookup
     * @throws IllegalStateException If lookup contains both SelectedBars and SelectedCLIs
     */
    @SuppressWarnings(
        {
            "rawtypes",
            "unchecked"
        })
    public CL_Selection(Lookup lookup)
    {
        this(lookup, true, true);
    }

    /**
     * Create an instance with only the searched types.
     *
     * @param lookup
     * @param searchItems If false SelectedCLIs are ignored
     * @param searchBars If false SelectedBars are ignored
     * @throws IllegalStateException If lookup contains both SelectedBars and SelectedCLIs, or incompatible SelectedCLIs
     */
    @SuppressWarnings(
        {
            "rawtypes",
            "unchecked"
        })
    public CL_Selection(Lookup lookup, boolean searchItems, boolean searchBars)
    {
        Objects.requireNonNull(lookup);

        selectedCLIs = !searchItems ? Collections.emptyList() : new ArrayList<>(lookup.lookupAll(SelectedCLI.class));
        items = selectedCLIs.stream()
            .map(selCli -> selCli.getItem())
            .collect(Collectors.toList());  // to get a mutable list
        selectedBars = !searchBars ? Collections.emptyList() : new ArrayList<>((Collection<SelectedBar>) lookup.lookupAll(SelectedBar.class));

        if (!items.isEmpty() && !selectedBars.isEmpty())
        {
            throw new IllegalStateException("items=" + items + " selectedBars=" + selectedBars);
        }
        if (!items.isEmpty())
        {
            var cliClass = items.getFirst().getClass();
            if (!items.stream()
                .skip(1)
                .allMatch(item -> cliClass.isInstance(item) || cliClass.isAssignableFrom(item.getClass())))
            {
                throw new IllegalStateException("items=" + items + " cliClass=" + cliClass);
            }
        }

        Collections.sort(items);
        Collections.sort(selectedBars);
    }

    /**
     * Utility method to quickly check if selection contains at least one SelectedCLI.
     * <p>
     * This is faster than new CL_Selection(Lookup).isItemSelected().
     *
     * @param lookup
     * @return
     */
    static public boolean isItemSelected(Lookup lookup)
    {
        return lookup.lookup(SelectedCLI.class) != null;
    }

    /**
     * Utility method to quickly check if selection contains at least one SelectedBar.
     * <p>
     * This is faster than new CL_Selection(Lookup).isBarSelected().
     *
     * @param lookup
     * @return
     */
    static public boolean isBarSelected(Lookup lookup)
    {
        return lookup.lookup(SelectedBar.class) != null;
    }

    /**
     * True if all selectedBars are contiguous.
     *
     * @return
     */
    public boolean isContiguousBarSelection()
    {
        if (!isBarSelected())
        {
            return false;
        }
        boolean b = true;
        int nextExpectedIndex = selectedBars.get(0).getBarBoxIndex();
        for (SelectedBar sb : selectedBars)
        {
            int bbIndex = sb.getBarBoxIndex();
            if (bbIndex != nextExpectedIndex)
            {
                b = false;
                break;
            }
            nextExpectedIndex = bbIndex + 1;
        }
        return b;
    }

    /**
     * Same as isContiguousBarSelection but limited to bars within the chordleadsheet's range.
     *
     * @return
     */
    public boolean isContiguousBarboxSelectionWithinCls()
    {
        if (!isBarSelected())
        {
            return false;
        }
        boolean b = true;
        int nextExpectedIndex = selectedBars.get(0).getBarBoxIndex();
        for (SelectedBar sb : selectedBars)
        {
            int modelBarIndex = sb.getModelBarIndex();
            if (modelBarIndex != nextExpectedIndex)
            {
                b = false;
                break;
            }
            nextExpectedIndex = modelBarIndex + 1;
        }
        return b;
    }

    public boolean isEmpty()
    {
        return !isItemSelected() && !isBarSelected();
    }

    public boolean isChordSymbolSelected()
    {
        return !items.isEmpty() && items.get(0) instanceof CLI_ChordSymbol;
    }

    public boolean isSectionSelected()
    {
        return !items.isEmpty() && items.get(0) instanceof CLI_Section;
    }

    public boolean isBarAnnotationSelected()
    {
        return !items.isEmpty() && items.get(0) instanceof CLI_BarAnnotation;
    }

    public boolean isItemSelected()
    {
        return !items.isEmpty();
    }

    /**
     * True if a BarBox is selected.
     *
     * @return
     */
    public boolean isBarSelected()
    {
        return !selectedBars.isEmpty();
    }

    /**
     * True if at least one selected BarBox is within the chordleadsheet range.
     *
     * @return
     */
    public boolean isBarSelectedWithinCls()
    {
        return isBarSelected() && getMinBarIndex() < getChordLeadSheet().getSizeInBars();
    }

    /**
     * Same as getMinBarIndex but limited to bars within the chordleadsheet's range.
     *
     * @return -1 if selection is empty or first selected bar is after chordleadsheet's end.
     */
    public int getMinBarIndexWithinCls()
    {
        int res = -1;
        if (isItemSelected())
        {
            res = items.get(0).getPosition().getBar();
        } else if (isBarSelected())
        {
            int barIndex = selectedBars.get(0).getModelBarIndex();
            if (barIndex != SelectedBar.POST_END_BAR_MODEL_BAR_INDEX)
            {
                res = barIndex;
            }
        }
        return res;
    }

    /**
     * Same as getMaxBarIndex but limited to bars within the chordleadsheet's range.
     *
     * @return -1 if selection is empty or selected bars are after chordleadsheet's end.
     */
    public int getMaxBarIndexWithinCls()
    {
        int res = -1;
        if (isItemSelected())
        {
            res = items.get(items.size() - 1).getPosition().getBar();
        } else if (isBarSelected() && selectedBars.get(0).getModelBarIndex() != SelectedBar.POST_END_BAR_MODEL_BAR_INDEX)
        {
            for (int i = selectedBars.size() - 1; i >= 0; i--)
            {
                SelectedBar sb = selectedBars.get(i);
                if (sb.getModelBarIndex() != SelectedBar.POST_END_BAR_MODEL_BAR_INDEX)
                {
                    res = sb.getBarBoxIndex();
                    break;
                }
            }
        }
        return res;
    }

    /**
     * The selected bar range within the chord leadsheet.
     *
     * @return Can be null if selection empty or outside the chord leadsheet.
     */
    public IntRange getBarRangeWithinCls()
    {
        int min = getMinBarIndexWithinCls();
        int max = getMaxBarIndexWithinCls();
        if (min < 0 || max < 0)
        {
            return null;
        }
        return new IntRange(min, max);
    }

    /**
     * The selected bar range within the chord leadsheet.
     *
     * @return Can be null if selection empty
     */
    public IntRange getBarRange()
    {
        int min = getMinBarIndex();
        int max = getMaxBarIndex();
        if (min < 0 || max < 0)
        {
            return null;
        }
        return new IntRange(min, max);
    }

    /**
     * The first selected bar index if bars are selected, or the barIndex of the first item selected if items are selected.
     *
     * @return -1 if selection is empty.
     */
    public int getMaxBarIndex()
    {
        int res = -1;
        if (isItemSelected())
        {
            res = items.getLast().getPosition().getBar();
        } else if (isBarSelected())
        {
            res = selectedBars.getLast().getBarBoxIndex();
        }
        return res;
    }

    /**
     * The first selected bar index if bars are selected, or the barIndex of the first item selected if items are selected.
     *
     * @return -1 if selection is empty.
     */
    public int getMinBarIndex()
    {
        int res = -1;
        if (isItemSelected())
        {
            res = items.get(0).getPosition().getBar();
        } else if (isBarSelected())
        {
            res = selectedBars.get(0).getBarBoxIndex();
        }
        return res;
    }

    /**
     * Can be null if selection is empty.
     *
     * @return
     */
    public ChordLeadSheet getChordLeadSheet()
    {
        if (isEmpty())
        {
            return null;
        }
        if (isItemSelected())
        {
            return items.get(0).getContainer();
        } else
        {
            return selectedBars.get(0).getContainer();
        }
    }

    public boolean isItemSelected(ChordLeadSheetItem<?> item)
    {
        return items.contains(item);
    }

    /**
     * Check if the selected ChordLeadSheet items are compatible with itemClass.
     *
     * @param itemClass
     * @return
     */
    public boolean isItemTypeSelected(Class<? extends ChordLeadSheetItem> itemClass)
    {
        boolean b = false;
        if (!items.isEmpty())
        {
            var item0 = items.getFirst();
            b = itemClass.isInstance(item0) || itemClass.isAssignableFrom(item0.getClass());
        }
        return b;
    }

    public boolean isBarSelected(int bbIndex)
    {
        for (SelectedBar sb : selectedBars)
        {
            if (sb.getBarBoxIndex() == bbIndex)
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Get all the SelectedCLIs, unsorted.
     *
     * @return Can be empty.
     */
    public List<SelectedCLI> getSelectedCLIs()
    {
        return selectedCLIs;
    }

    /**
     * Get all the selected items sections, chord symbols, etc. sorted by position.
     *
     * @return Can be empty.
     */
    public List<ChordLeadSheetItem> getSelectedItems()
    {
        return items;
    }

    /**
     * Get the selected items from specified class sorted by position.
     *
     * @param <T>
     * @param itemClass
     * @return An unmodifable list. Can be empty.
     */
    <T extends ChordLeadSheetItem> List<T> getSelectedItems(Class<T> itemClass)
    {
        return items.stream()
            .filter(i -> itemClass.isAssignableFrom(i.getClass()))
            .map(i -> (T) i)
            .toList();
    }

    /**
     * Get only the selected sections sorted by position.
     *
     * @return Can be empty.
     */
    public List<CLI_Section> getSelectedSections()
    {
        return getSelectedItems(CLI_Section.class);
    }

    /**
     * Get only the selected chord symbols sorted by position.
     *
     * @return Can be empty.
     */
    public List<CLI_ChordSymbol> getSelectedChordSymbols()
    {
        return getSelectedItems(CLI_ChordSymbol.class);
    }

    /**
     * Get only the selected chord symbols sorted by position.
     *
     * @return Can be empty.
     */
    public List<CLI_BarAnnotation> getSelectedBarAnnotations()
    {
        return getSelectedItems(CLI_BarAnnotation.class);
    }

    /**
     * Get the selected bars sorted by position.
     *
     * @return Can be empty if isItemSelected() returns true.
     */
    public List<SelectedBar> getSelectedBars()
    {
        return selectedBars;
    }

    /**
     * The bar indexes of the SelectedBars (sorted by position).
     * <p>
     * Return an empty list if selection is not made of SelectedBars.
     *
     * @return
     */
    public List<Integer> getSelectedBarIndexes()
    {
        List<Integer> res = new ArrayList<>();
        for (SelectedBar sb : selectedBars)
        {
            res.add(sb.getBarBoxIndex());
        }
        return res;
    }

    /**
     * Same as getSelectedBars() but limited to bars within the chordleadsheet's range.
     *
     * @return Can be empty if isItemSelected() returns true.
     */
    public List<SelectedBar> getSelectedBarsWithinCls()
    {
        List<SelectedBar> res = new ArrayList<>();
        for (SelectedBar sb : selectedBars)
        {
            if (sb.getModelBarIndex() != SelectedBar.POST_END_BAR_MODEL_BAR_INDEX)
            {
                res.add(sb);
            } else
            {
                // No need to continue since selectedBars is ordered
                break;
            }
        }
        return res;
    }

    /**
     * Same as getSelectedBarsIndexes() but limited to bars within the chordleadsheet's range.
     * <p>
     *
     * @return
     */
    public List<Integer> getSelectedBarIndexesWithinCls()
    {
        List<Integer> res = new ArrayList<>();
        for (SelectedBar sb : selectedBars)
        {
            if (sb.getModelBarIndex() != SelectedBar.POST_END_BAR_MODEL_BAR_INDEX)
            {
                res.add(sb.getBarBoxIndex());
            } else
            {
                // No need to continue since selectedBars is ordered
                break;
            }
        }
        return res;
    }

    @Override
    public String toString()
    {
        return "CL_selection items=" + items + " bars=" + selectedBars;
    }
}

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
package org.jjazz.ui.cl_editor.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordLeadSheetItem;
import org.openide.util.Lookup;

/**
 * Provide convenience methods to get information about a selection in a lookup.
 * <p>
 * Selected items can be either ChordLeadSheetItems or SelectedBars, but not both in the same time.
 */
@SuppressWarnings("unchecked")
final public class CL_SelectionUtilities
{

    private List<ChordLeadSheetItem<?>> items;
    private final List<SelectedBar> selectedBars;
    private static final Logger LOGGER = Logger.getLogger(CL_SelectionUtilities.class.getSimpleName());

    /**
     * Refresh the selection with selected objects in the specified lookup.
     *
     * @param lookup
     * @throws IllegalStateException If lookup contains both SelectedBars and ChordLeadSheetItems
     */
    @SuppressWarnings(
            {
                "rawtypes",
                "unchecked"
            })
    public CL_SelectionUtilities(Lookup lookup)
    {
        if (lookup == null)
        {
            throw new IllegalArgumentException("lookup=" + lookup);   //NOI18N
        }

        items = new ArrayList<>((Collection<ChordLeadSheetItem<?>>) lookup.lookupAll(ChordLeadSheetItem.class));
        selectedBars = new ArrayList<>((Collection<SelectedBar>) lookup.lookupAll(SelectedBar.class));

        if (!items.isEmpty() && !selectedBars.isEmpty())
        {
            throw new IllegalStateException("items=" + items + " selectedBars=" + selectedBars);   //NOI18N
        }

        if (!items.isEmpty())
        {
            Collections.sort(items, new Comparator<ChordLeadSheetItem>()
            {
                @Override
                public int compare(ChordLeadSheetItem t, ChordLeadSheetItem t1)
                {
                    return t.getPosition().compareTo(t1.getPosition());
                }

            });
        } else if (!selectedBars.isEmpty())
        {
            Collections.sort(selectedBars);
        }
    }

    /**
     * Unselect the current selection in the specified editor.
     *
     * @param editor
     */
    public void unselectAll(CL_Editor editor)
    {
        if (isBarSelected())
        {
            editor.selectBars(0, editor.getNbBarBoxes() - 1, false);
        } else if (isItemSelected())
        {
            editor.selectItems(items, false);
        }
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
        return isBarSelected() && geMinBarIndex() < getChordLeadSheet().getSize();
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
     * The first selected bar index if bars are selected, or the barIndex of the first item selected if items are selected.
     *
     * @return -1 if selection is empty.
     */
    public int geMaxBarIndex()
    {
        int res = -1;
        if (isItemSelected())
        {
            res = items.get(items.size() - 1).getPosition().getBar();
        } else if (isBarSelected())
        {
            res = selectedBars.get(selectedBars.size() - 1).getBarBoxIndex();
        }
        return res;
    }

    /**
     * The first selected bar index if bars are selected, or the barIndex of the first item selected if items are selected.
     *
     * @return -1 if selection is empty.
     */
    public int geMinBarIndex()
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
     * Get all the selected items sections, chord symbols, sorted by position.
     *
     * @return Can be empty.
     */
    public List<ChordLeadSheetItem<?>> getSelectedItems()
    {
        return items;
    }

    /**
     * Get only the selected chord symbols sorted by position.
     *
     * @return Can be empty.
     */
    public List<CLI_ChordSymbol> getSelectedChordSymbols()
    {
        ArrayList<CLI_ChordSymbol> res = new ArrayList<>();
        for (ChordLeadSheetItem<?> item : items)
        {
            if (item instanceof CLI_ChordSymbol)
            {
                res.add((CLI_ChordSymbol) item);
            }
        }
        return res;
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

    /**
     * Get only the selected sections sorted by position.
     *
     * @return Can be empty.
     */
    public List<CLI_Section> getSelectedSections()
    {
        ArrayList<CLI_Section> res = new ArrayList<>();
        for (ChordLeadSheetItem<?> item : items)
        {
            if (item instanceof CLI_Section)
            {
                res.add((CLI_Section) item);
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

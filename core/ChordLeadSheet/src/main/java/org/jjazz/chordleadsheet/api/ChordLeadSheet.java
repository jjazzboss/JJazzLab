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
package org.jjazz.chordleadsheet.api;

import com.google.common.base.Preconditions;
import java.util.List;
import java.util.function.Predicate;
import javax.swing.event.UndoableEditListener;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.harmony.api.Position;
import org.jjazz.utilities.api.IntRange;

/**
 * The model for a chord leadsheet.
 * <p>
 * The leadsheet is made of sections (a name + a time signature) and items like chord symbols or bar annotations.
 * <p>
 * Implementation must fire the relevant ClsChangeEvents: start and complete ClsActionEvents for an API method, with the related low-level ClsChangeEvents. If
 * API method1 calls API method2, ClsActionEvents are only fired for API method1, i.e. ClsActionEvents are not nested.
 * <p>
 * - The first bar must always contain a section <br>
 * - 2 sections can't have the same name<br>
 * - All ChordLeadSheetItems belonging to a chord leadsheet must be unique (2 ChordLeadSheetItems can not be equal)<br>
 */
public interface ChordLeadSheet
{

    public static final int MAX_SIZE = 1024;


    /**
     * Get a deep copy of this ChordLeadSheet (all ChordLeadSheetItems are deeply copied).
     *
     * Transient fields such as listeners are not copied.
     * @return
     */
    ChordLeadSheet getDeepCopy();

    /**
     * Add an item to the leadsheet.
     * <p>
     * Item position might be adjusted to the bar's TimeSignature. This will set the item's container to this ChordLeadSheet. Nothing is done if an equal item
     * is already in the ChordLeadSheet.
     *
     * @param item The ChordLeadSheetItem to add. Must be a WritableItem. Can not be a CLI_Section.
     * @return True is item was added
     * @throws IllegalArgumentException If item's position out of leadsheet bounds or item is a CLI_Section.
     * @see #addSection(org.jjazz.chordleadsheet.api.item.CLI_Section)
     */
    boolean addItem(ChordLeadSheetItem<?> item);

    /**
     * Remove an item from the leadsheet.
     *
     * @param item The item to be removed.
     * @throws IllegalArgumentException If no such item or item is a section.
     * @see #removeSection(org.jjazz.chordleadsheet.api.item.CLI_Section)
     */
    void removeItem(ChordLeadSheetItem<?> item);

    /**
     * Add a section to the leadsheet or, if a section already exists at the same bar, update its section name and time signature.
     * <p>
     * Trailing items' position might be adjusted if it results in a time signature change.
     *
     * @param cliSection
     * @return cliSection if it was added, or the existing CLI_Section that was updated.
     * @throws IllegalArgumentException If section is invalid (e.g. section is after chordleadsheet size)
     * @throws UnsupportedEditException If a ChordLeadSheet change listener vetoes this edit. Exception is thrown before any change is done.
     */
    CLI_Section addSection(CLI_Section cliSection) throws UnsupportedEditException;

    /**
     * Remove a section from the leadsheet.
     * <p>
     * The section on bar 0 can not be removed. Trailing items' position might be adjusted if it results in a time signature change.
     *
     * @param section
     * @throws UnsupportedEditException If a ChordLeadSheet change listener vetoes this edit. Exception is thrown before any change is done.
     */
    void removeSection(CLI_Section section) throws UnsupportedEditException;

    /**
     * Change the name of section.
     *
     * @param section The section to be changed.
     * @param name
     * @throws IllegalArgumentException If name already exist, is a reserved name, or section does not belong to this leadsheet.
     */
    void setSectionName(CLI_Section section, String name);

    /**
     * Change the TimeSignature of a section.
     * <p>
     * Trailing items' position might be adjusted to fit the new TimeSignature.
     *
     * @param section The section to be changed.
     * @param ts
     * @throws IllegalArgumentException If section does not belong to this leadsheet.
     * @throws UnsupportedEditException If a ChordLeadSheet change listener vetoes this edit. Exception is thrown before any change is done.
     */
    void setSectionTimeSignature(CLI_Section section, TimeSignature ts) throws UnsupportedEditException;

    /**
     * Move a section to a new position.
     * <p>
     * New position must be free of a section. Section on first bar can not be moved. Some items position might be adjusted to the new bar's TimeSignature.
     *
     * @param section     The section to be moved
     * @param newBarIndex The bar index section will be moved to
     * @throws UnsupportedEditException If a ChordLeadSheet change listener vetoes this edit. Exception is thrown before any change is done.
     * @throws IllegalArgumentException If new position is not valid.
     */
    void moveSection(CLI_Section section, int newBarIndex) throws UnsupportedEditException;

    /**
     * Move an item to a new position.
     * <p>
     * Item position might be adjusted to the bar's TimeSignature. Nothing is done if an equal item is already at the target position.
     *
     * @param item The item to be moved. Must be a WritableItem. Can not be used on a Section.
     * @param pos  The new position.
     * @return True if the item was moved.
     * @throws IllegalArgumentException If new position is not valid of if item is a CLI_Section
     * @see #moveSection(org.jjazz.chordleadsheet.api.item.CLI_Section, int)
     */
    boolean moveItem(ChordLeadSheetItem<?> item, Position pos);


    /**
     * Change the data of a specific item.
     * <p>
     * Nothing is done if an equal item (using data) is already in the chord leadsheet.
     *
     * @param <T>
     * @param item Must be a WritableItem. Can not be a section.
     * @param data
     * @return True if item was changed
     * @see #setSectionName(org.jjazz.chordleadsheet.api.item.CLI_Section, java.lang.String)
     * @see #setSectionTimeSignature(org.jjazz.chordleadsheet.api.item.CLI_Section, org.jjazz.harmony.api.TimeSignature)
     */
    <T> boolean changeItem(ChordLeadSheetItem<T> item, T data);

    /**
     * Test if specified item belongs to this object.
     *
     * @param item
     * @return
     */
    boolean contains(ChordLeadSheetItem<?> item);

    /**
     * Insert bars from a specific position.
     * <p>
     * If there are bars after barIndex, they are shifted accordingly.
     *
     * @param barIndex The bar index from which to insert the new bars.
     * @param nbBars   The number of bars to insert.
     * @throws IllegalArgumentException If barIndex &lt; 0 or barIndex &gt; size()
     */
    void insertBars(int barIndex, int nbBars);

    /**
     * Delete bars and items from barIndexFrom to barIndexTo (inclusive).
     * <p>
     * Bars after the deleted bars are shifted accordingly. Trailing items positions might be adjusted if it results in a time signature change.
     *
     * @param barIndexFrom
     * @param barIndexTo
     * @throws UnsupportedEditException If a ChordLeadSheet change listener does not authorize this edit. IMPORTANT:some undoable changes might have been done
     *                                  before exception is thrown, caller will need to rollback them.
     * @throws IllegalArgumentException If barIndexFrom/To are not consistent with this chord leadsheet, or if all bars would be deleted
     */
    void deleteBars(int barIndexFrom, int barIndexTo) throws UnsupportedEditException;

    /**
     * Cleanup function to be called so that the object can be garbaged.
     */
    void cleanup();


    /**
     * Get the matching items whose position is in the position range.
     *
     * @param <T>
     * @param posFrom
     * @param inclusiveFrom
     * @param posTo
     * @param inclusiveTo
     * @param itemClass     Accept items which are instance of class itemClass
     * @param tester        Accept items which satisfy the tester.
     * @return A non-modifiable ordered list of items
     */
    <T extends ChordLeadSheetItem<?>> List<T> getItems(Position posFrom, boolean inclusiveFrom, Position posTo, boolean inclusiveTo,
            Class<T> itemClass,
            Predicate<T> tester);

    /**
     * Get the last matching item whose position is before (or equal, if inclusive is true) posHigh.
     *
     * @param <T>
     * @param posTo
     * @param inclusiveTo
     * @param itemClass   Accept items which are assignable from aClass
     * @param tester
     * @return Can be null.
     */
    <T extends ChordLeadSheetItem<?>> T getLastItemBefore(Position posTo, boolean inclusiveTo, Class<T> itemClass, Predicate<T> tester);

    /**
     * Get the last matching item before cli.
     *
     * @param <T>
     * @param itemClass Accept items which are assignable from aClass
     * @param tester
     * @return Can be null.
     */
    <T extends ChordLeadSheetItem<?>> T getLastItemBefore(ChordLeadSheetItem<?> cli, Class<T> itemClass, Predicate<T> tester);

    /**
     * Get the first matching item whose position is after (or equal, if inclusive is true) posFrom.
     *
     * @param <T>
     * @param posFrom
     * @param inclusiveFrom
     * @param itemClass
     * @param tester
     * @return Can be null
     */
    <T extends ChordLeadSheetItem<?>> T getFirstItemAfter(Position posFrom, boolean inclusiveFrom, Class<T> itemClass, Predicate<T> tester);

    /**
     * Get the first matching item after cli.
     *
     * @param <T>
     * @param cli
     * @param itemClass
     * @param tester
     * @return Can be null
     */
    <T extends ChordLeadSheetItem<?>> T getFirstItemAfter(ChordLeadSheetItem<?> cli, Class<T> itemClass, Predicate<T> tester);

    /**
     * Get all the items.
     *
     * @return A non-modifiable ordered list of items
     */
    default List<ChordLeadSheetItem> getItems()
    {
        return getItems(ChordLeadSheetItem.class);
    }

    /**
     * Get all the matching items of this leadsheet.
     *
     * @param <T>
     * @param itemClass Accept items which are instance of class itemClass
     * @return A non-modifiable ordered list of items
     */
    default <T extends ChordLeadSheetItem<?>> List<T> getItems(Class<T> itemClass)
    {
        return getItems(itemClass, item -> true);
    }

    /**
     * Get all the matching items of this leadsheet.
     *
     * @param <T>
     * @param itemClass Accept items which are instance of class itemClass
     * @param tester    Accept items which satisfy the tester.
     * @return A non-modifiable ordered list of items
     */
    default <T extends ChordLeadSheetItem<?>> List<T> getItems(Class<T> itemClass, Predicate<T> tester)
    {
        return getItems(0, getSizeInBars() - 1, itemClass, tester);
    }

    /**
     * Get the matching items which belong to bars between barFrom and barTo (included).
     * <p>
     *
     * @param <T>
     * @param barFrom
     * @param barTo
     * @param itemClass Accept items which are instance of class aClass.
     * @param tester    Accept items which satisfy the tester.
     * @return A non-modifiable ordered list of items
     */
    default <T extends ChordLeadSheetItem<?>> List<T> getItems(int barFrom, int barTo, Class<T> itemClass, Predicate<T> tester)
    {
        var posFrom = new Position(barFrom);
        var posTo = new Position(barTo < Integer.MAX_VALUE ? barTo + 1 : barTo);
        return getItems(posFrom, true, posTo, false, itemClass, tester);
    }

    /**
     * Get the matching items which belong to bars between barFrom and barTo (included).
     * <p>
     *
     * @param <T>
     * @param barFrom
     * @param barTo
     * @param itemClass Accept items which are instance of class aClass.
     * @return A non-modifiable ordered list of items
     */
    default <T extends ChordLeadSheetItem<?>> List<T> getItems(int barFrom, int barTo, Class<T> itemClass)
    {
        return getItems(barFrom, barTo, itemClass, cli -> true);
    }

    /**
     * Get the matching items whose position is after (or equal, if inclusive is true) posLow.
     *
     * @param <T>
     * @param posFrom
     * @param inclusive
     * @param itemClass Accept items which are instance of class itemClass
     * @param tester    Accept items which satisfy the tester.
     * @return A non-modifiable ordered list of items
     */
    default <T extends ChordLeadSheetItem<?>> List<T> getItemsAfter(Position posFrom, boolean inclusive, Class<T> itemClass, Predicate<T> tester)
    {
        var posTo = new Position(Integer.MAX_VALUE, Float.MAX_VALUE);
        return getItems(posFrom, inclusive, posTo, false, itemClass, tester);
    }

    /**
     * Get the matching items whose position is before (or equal, if inclusive is true) posTo.
     *
     * @param <T>
     * @param posTo
     * @param inclusive
     * @param itemClass Accept items which are instance of class itemClass. Can't be null.
     * @param tester    Accept items which satisfy the tester.
     * @return A non-modifiable ordered list of items
     */
    default <T extends ChordLeadSheetItem<?>> List<T> getItemsBefore(Position posTo, boolean inclusive, Class<T> itemClass, Predicate<T> tester)
    {
        var posFrom = new Position(0);
        return getItems(posFrom, true, posTo, inclusive, itemClass, tester);
    }

    /**
     * Get the matching items which belong to a specific section.
     * <p>
     *
     * @param <T>
     * @param cliSection
     * @param itemClass  Accept items which are instance of class aClass
     * @param tester     Accept items which satisfy the tester.
     * @return A non-modifiable ordered list of items
     */
    default <T extends ChordLeadSheetItem<?>> List<T> getItems(CLI_Section cliSection, Class<T> itemClass, Predicate<T> tester)
    {
        var barRange = getBarRange(cliSection);
        return getItems(barRange.from, barRange.to, itemClass, cli -> cli != cliSection);
    }

    /**
     * Get the items which belong to a specific section.
     * <p>
     *
     * @param <T>
     * @param cliSection
     * @param itemClass  Accept items which are instance of class aClass
     * @return A non-modifiable ordered list of items
     */
    default <T extends ChordLeadSheetItem<?>> List<T> getItems(CLI_Section cliSection, Class<T> itemClass)
    {
        return getItems(cliSection, itemClass, cli -> cli != cliSection);
    }


    /**
     * Get the first matching item in the specified bar.
     *
     * @param <T>
     * @param barIndex
     * @param itemClass
     * @param tester
     * @return Can be null
     */
    default <T extends ChordLeadSheetItem<?>> T getBarFirstItem(int barIndex, Class<T> itemClass, Predicate<T> tester)
    {
        var items = getItems(barIndex, barIndex, itemClass, tester);
        return items.isEmpty() ? null : items.get(0);
    }

    /**
     * Get the last matching item in the specified bar.
     *
     * @param <T>
     * @param barIndex
     * @param itemClass
     * @param tester
     * @return Can be null
     */
    default <T extends ChordLeadSheetItem<?>> T getBarLastItem(int barIndex, Class<T> itemClass, Predicate<T> tester)
    {
        var items = getItems(barIndex, barIndex, itemClass, tester);
        return items.isEmpty() ? null : items.get(items.size() - 1);
    }

    /**
     * Get the next similar item (same class or subclass) after the specified item.
     *
     * @param <T>
     * @param item
     * @return Can be null if item is the last item of its kind.
     */
    default <T> ChordLeadSheetItem<T> getNextItem(ChordLeadSheetItem<T> item)
    {
        return getFirstItemAfter(item, item.getClass(), cli -> true);
    }

    /**
     * Get the previous similar item (same class or subclass) before the specified item.
     *
     * @param <T>
     * @param item
     * @return Can be null if item is the first item of its kind.
     */
    default <T> ChordLeadSheetItem<T> getPreviousItem(ChordLeadSheetItem<T> item)
    {
        return getLastItemBefore(item, item.getClass(), cli -> true);
    }

    /**
     * Get the Section of a specific bar.
     * <p>
     * If the bar is after the end of the leadsheet, return the section of the last bar.
     *
     * @param barIndex The index of the bar.
     * @return The section.
     */
    default CLI_Section getSection(int barIndex)
    {
        Preconditions.checkArgument(barIndex >= 0, "barIndex=%s", barIndex);
        return getLastItemBefore(new Position(barIndex), true, CLI_Section.class, cli -> true);
    }

    /**
     * Get a CLI_Section from its name.
     *
     * @param sectionName
     * @return The section or null if not found.
     */
    default CLI_Section getSection(String sectionName)
    {
        return getFirstItemAfter(new Position(0), true, CLI_Section.class, cli -> cli.getData().getName().equals(sectionName));
    }


    /**
     * Get the size of the leadsheet in bars.
     *
     * @return
     */
    int getSizeInBars();

    /**
     * Get the bar range of this chord leadsheet.
     *
     * @return [0; getSizeInBars()-1]
     */
    default IntRange getBarRange()
    {
        return new IntRange(0, getSizeInBars() - 1);
    }

    /**
     * The bar range of the specified section.
     *
     * @param cliSection
     * @return
     * @throws IllegalArgumentException If section does not exist in this ChordLeadSheet.
     */
    default IntRange getBarRange(CLI_Section cliSection)
    {
        Preconditions.checkNotNull(cliSection);
        Position pos = cliSection.getPosition();
        Preconditions.checkArgument(getSection(pos.getBar()) == cliSection, "cliSection=%s this=%s", cliSection, this);

        var nextSection = getNextItem(cliSection);
        int lastBar = nextSection == null ? getSizeInBars() - 1 : nextSection.getPosition().getBar() - 1;
        return new IntRange(pos.getBar(), lastBar);
    }


    /**
     * Set the size of the ChordLeadSheet.
     *
     * @param size The numbers of bars, must be &gt;= 1 and &lt; MAX_SIZE.
     * @throws UnsupportedEditException If a ChordLeadSheet change listener does not authorize this edit. Exception is thrown before any change is done.
     */
    void setSizeInBars(int size) throws UnsupportedEditException;

    /**
     * Add a listener to item changes of this object.
     *
     * @param l
     */
    void addClsChangeListener(ClsChangeListener l);

    /**
     * Remove a listener to this object's changes.
     *
     * @param l
     */
    void removeClsChangeListener(ClsChangeListener l);

    /**
     * Add a listener to undoable edits.
     *
     * @param l
     */
    void addUndoableEditListener(UndoableEditListener l);

    /**
     * Remove a listener to undoable edits.
     *
     * @param l
     */
    void removeUndoableEditListener(UndoableEditListener l);


    /**
     * Get a string showing the complete chord leadsheet.
     *
     * @return
     */
    public default String toDebugString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(toString()).append(" ");
        for (var item : getItems())
        {
            if (item instanceof CLI_Section)
            {
                sb.append('\n').append(" ").append(item.getData()).append(item.getPosition()).append(" : ");
            } else
            {
                sb.append(item.getData()).append(item.getPosition()).append(" ");
            }
        }
        return sb.toString();
    }
}

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

import java.util.List;
import javax.swing.event.UndoableEditListener;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;

/**
 * The model for a chord leadsheet.
 * <p>
 * The leadsheet is made of sections (a name + a time signature) and items like chord symbols.
 * <p>
 * Regarding sections:<br>
 * - The first bar must always contain a section <br>
 * - Sections can not be moved <br>
 * - 2 sections can't have the same name
 */
public interface ChordLeadSheet
{

    public static final int MAX_SIZE = 1024;

    /**
     * Add an item to the leadsheet.
     * <p>
     * Item position might be adjusted to the bar's TimeSignature.
     *
     * @param <T>
     * @param item The ChordLeadSheetItem to add.
     * @throws IllegalArgumentException If item's position out of leadsheet bounds or item is a section.
     */
    public <T> void addItem(ChordLeadSheetItem<T> item);

    /**
     * Remove an item from the leadsheet.
     *
     * @param <T>
     * @param item The item to be removed.
     * @throws IllegalArgumentException If no such item or item is a section.
     */
    public <T> void removeItem(ChordLeadSheetItem<T> item);

    /**
     * Add a section to the leadsheet.
     * <p>
     * Trailing items' position might be adjusted if it results in a time signature change.
     *
     * @param section
     * @throws org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException
     * @throws IllegalArgumentException If section already exists at specified position or invalid section.
     * @throws UnsupportedEditException If a ChordLeadSheet change listener veto this edit.
     */
    public void addSection(CLI_Section section) throws UnsupportedEditException;

    /**
     * Remove a section from the leadsheet.
     * <p>
     * The section on bar 0 can not be removed. Trailing items' position might be adjusted if it results in a time signature
     * change.
     *
     * @param section
     */
    public void removeSection(CLI_Section section);

    /**
     * Change the name of section.
     *
     * @param section The section to be changed.
     * @param name
     * @throws UnsupportedEditException
     * @throws IllegalArgumentException If name already exist, is a reserved name, or section does not belong to this leadsheet.
     */
    public void setSectionName(CLI_Section section, String name) throws UnsupportedEditException;

    /**
     * Change the TimeSignature of a section.
     * <p>
     * Trailing items' position might be adjusted to fit the new TimeSignature.
     *
     * @param section The section to be changed.
     * @param ts
     * @throws IllegalArgumentException If section does not belong to this leadsheet.
     * @throws UnsupportedEditException If a ChordLeadSheet change listener veto this edit.
     */
    public void setSectionTimeSignature(CLI_Section section, TimeSignature ts) throws UnsupportedEditException;

    /**
     * Move a section to a new position. N
     * <p>
     * New position must be free of a section. Section on first bar can not be moved. Some items position might be adjusted to the
     * new bar's TimeSignature.
     *
     * @param section The section to be moved
     * @param newBarIndex The bar index section will be moved to
     * @throws IllegalArgumentException If new position is not valid.
     */
    public void moveSection(CLI_Section section, int newBarIndex);

    /**
     * Move an item to a new position.
     * <p>
     * Can not be used on a Section. Item position might be adjusted to the bar's TimeSignature.
     *
     * @param <T>
     * @param item The item to be moved
     * @param pos The new position.
     * @throws IllegalArgumentException If new position is not valid.
     */
    public <T> void moveItem(ChordLeadSheetItem<T> item, Position pos);

    /**
     * Change the data of a specific item.
     * <p>
     * Can not be used on Section, use setSectionName() or setSectionTimeSignature() instead.
     *
     * @param <T>
     * @param item
     * @param data
     */
    public <T> void changeItem(ChordLeadSheetItem<T> item, T data);

    /**
     * Insert bars from a specific position.
     * <p>
     * If there are bars after barIndex, they are shifted accordingly.
     *
     * @param barIndex The bar index from which to insert the new bars.
     * @param nbBars The number of bars to insert.
     * @throws IllegalArgumentException If barIndex &lt; 0 or barIndex &gt; size()
     */
    public void insertBars(int barIndex, int nbBars);

    /**
     * Delete bars and items from barIndexFrom to barIndexTo (inclusive).
     * <p>
     * Bars after the deleted bars are shifted accordingly. Trailing items positions might be adjusted if it results in a time
     * signature change.
     *
     * @param barIndexFrom
     * @param barIndexTo
     */
    public void deleteBars(int barIndexFrom, int barIndexTo);

    /**
     * Cleanup function to be called so that the object can be garbaged.
     */
    public void cleanup();

    /**
     * Get all the items of this chordleadsheet.
     *
     * @return
     */
    public List<ChordLeadSheetItem<?>> getItems();

    /**
     * Get all the items of this leadsheet which match a specicic class.
     *
     * @param <T>
     * @param aClass Return only items which are instance of class cl. If null all items are returned.
     * @return A list of items ordered by position.
     */
    public <T> List<? extends T> getItems(Class<T> aClass);

    /**
     * Get the items which belong to bars between barFrom and barTo (included), ordered by position.
     * <p>
     * The results can be filtered to get only items which are instance of a specified class.
     *
     * @param <T>
     * @param barFrom
     * @param barTo
     * @param aClass Return only items which are instance of class aClass.
     * @return A list of items, can be empty if no item found.
     */
    public <T> List<? extends T> getItems(int barFrom, int barTo, Class<T> aClass);

    /**
     * Get the items which belong to a specific section, ordered by position.
     * <p>
     * The results can be filtered to get only items which are instance of a specified class.
     *
     * @param <T>
     * @param sectionItem
     * @param aClass Return only items only items which are instance of class aClass.
     * @return The list of all the items that are part of sectionItem. The sectionItem itself and the special END_EVENT are not
     * included.
     */
    public <T> List<? extends T> getItems(CLI_Section sectionItem, Class<T> aClass);

    /**
     * Get the Section for a specific bar.
     * <p>
     * If the bar is after the end of the leadsheet, return the section of the last bar.
     *
     * @param barIndex The index of the bar.
     * @return The section.
     */
    public CLI_Section getSection(int barIndex);

    /**
     * Get the Section from his name.
     *
     * @param sectionName The name of the bar (case is ignored).
     * @return The section or null if not found.
     */
    public CLI_Section getSection(String sectionName);

    /**
     * @param section
     * @return int The size in bars of the section. -1 if section does not exist.
     */
    public int getSectionSize(CLI_Section section);

    /**
     * Get the size of the leadsheet in bars.
     *
     * @return
     */
    public int getSize();

    /**
     * Set the size of the ChordLeadSheet.
     *
     * @param size The numbers of bars, must be &gt;= 1 and &lt; MAX_SIZE.
     */
    public void setSize(int size);

    /**
     * Add a listener to item changes of this object.
     *
     * @param l
     */
    public void addClsChangeListener(ClsChangeListener l);

    /**
     * Remove a listener to this object's changes.
     *
     * @param l
     */
    public void removeClsChangeListener(ClsChangeListener l);

    /**
     * Add a listener to undoable edits.
     *
     * @param l
     */
    public void addUndoableEditListener(UndoableEditListener l);

    /**
     * Remove a listener to undoable edits.
     *
     * @param l
     */
    public void removeUndoableEditListener(UndoableEditListener l);

}

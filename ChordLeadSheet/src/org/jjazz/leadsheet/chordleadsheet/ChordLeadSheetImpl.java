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
package org.jjazz.leadsheet.chordleadsheet;

import org.jjazz.undomanager.SimpleEdit;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.UndoableEdit;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.leadsheet.chordleadsheet.api.Section;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.event.*;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.leadsheet.chordleadsheet.item.CLI_SectionImpl;
import org.jjazz.leadsheet.chordleadsheet.item.WritableItem;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.leadsheet.chordleadsheet.api.ClsChangeListener;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Factory;
import org.openide.util.Exceptions;

public class ChordLeadSheetImpl implements ChordLeadSheet, Serializable
{

    /**
     * The tree nodes who store the items per section.
     */
    private ItemArray items = new ItemArray();
    /**
     * The size of the leadsheet.
     */
    private int size;
    /**
     * The listeners for changes in this LeadSheet.
     */
    protected transient List<ClsChangeListener> listeners = new ArrayList<>();
    /**
     * The listeners for undoable edits in this LeadSheet.
     */
    protected transient List<UndoableEditListener> undoListeners = new ArrayList<>();
    private static final Logger LOGGER = Logger.getLogger(ChordLeadSheetImpl.class.getSimpleName());

    public ChordLeadSheetImpl(String initSection, TimeSignature ts, int size)
    {
        this(new CLI_SectionImpl(initSection, ts, 0), size);
    }

    public ChordLeadSheetImpl(CLI_Section initSection, int size)
    {
        if (initSection == null || initSection.getPosition().getBar() != 0 || size < 1 || size > MAX_SIZE)
        {
            throw new IllegalArgumentException("section=" + initSection + " size=" + size);
        }
        this.size = size;
        WritableItem<Section> wSection = (WritableItem<Section>) initSection;
        wSection.setContainer(this);
        items.insertOrdered(initSection);
    }

    @Override
    public void setSize(final int newSize)
    {
        if (newSize < 1 || newSize > MAX_SIZE)
        {
            throw new IllegalArgumentException("newSize=" + newSize);
        }
        int delta = newSize - getSize();
        List<ChordLeadSheetItem<?>> removedItems = new ArrayList<>();
        if (delta == 0)
        {
            return;
        } else if (delta > 0)
        {
            // Nothing to do
        } else if (delta < 0)
        {
            // Need to remove possible extra items
            int newLastBarIndex = getSize() - 1 + delta;
            for (int index = items.size() - 1; index > 0; index--)
            {
                ChordLeadSheetItem<?> item = items.get(index);
                if (item.getPosition().getBar() > newLastBarIndex)
                {
                    removedItems.add(item);
                } else
                {
                    break;
                }
            }
            if (!removedItems.isEmpty())
            {
                removeItems(removedItems);
            }
        }

        final int oldSize = getSize();
        size = newSize;

        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("Set Size " + newSize)
        {
            @Override
            public void undoBody()
            {
                size = oldSize;
                fireChangeEvent(new SizeChangedEvent(ChordLeadSheetImpl.this, newSize, oldSize));
            }

            @Override
            public void redoBody()
            {
                size = newSize;
                fireChangeEvent(new SizeChangedEvent(ChordLeadSheetImpl.this, oldSize, newSize));
            }
        };
        fireUndoableEditHappened(edit);

        fireChangeEvent(new SizeChangedEvent(ChordLeadSheetImpl.this, oldSize, newSize));
    }

    @Override
    public int getSize()
    {
        return size;
    }

    @Override
    public final <T> void addItem(ChordLeadSheetItem<T> item)
    {
        if (item == null || (item instanceof CLI_Section) || !(item instanceof WritableItem))
        {
            throw new IllegalArgumentException("item=" + item);
        }
        final WritableItem<?> wItem = (WritableItem<?>) item;
        int barIndex = wItem.getPosition().getBar();
        if (barIndex >= getSize())
        {
            throw new IllegalArgumentException("item=" + item + " size=" + getSize());
        }

        // Set the container
        final ChordLeadSheet oldContainer = wItem.getContainer();
        final ChordLeadSheet newContainer = this;
        wItem.setContainer(newContainer);

        // Adjust position if required
        final Position oldPos = wItem.getPosition();
        final Position newAdjustedPos = oldPos.getAdjustedPosition(getSection(barIndex).getData().
                getTimeSignature());
        wItem.setPosition(newAdjustedPos);

        // Add item
        final int index = items.insertOrdered(wItem);

        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("Add " + wItem)
        {
            @Override
            public void undoBody()
            {
                wItem.setPosition(oldPos);
                wItem.setContainer(oldContainer);
                items.remove(wItem);
                fireChangeEvent(new ItemRemovedEvent(ChordLeadSheetImpl.this, wItem));
            }

            @Override
            public void redoBody()
            {
                wItem.setPosition(newAdjustedPos);
                wItem.setContainer(newContainer);
                items.add(index, wItem);
                fireChangeEvent(new ItemAddedEvent(ChordLeadSheetImpl.this, wItem));
            }
        };
        fireUndoableEditHappened(edit);

        // Fire ItemChange event
        fireChangeEvent(new ItemAddedEvent(ChordLeadSheetImpl.this, wItem));
    }

    @Override
    public void addSection(final CLI_Section cliSection) throws UnsupportedEditException
    {
        if (cliSection == null || cliSection.getPosition().getBar() >= getSize() || getSection(cliSection.getData().getName()) != null
                || !(cliSection instanceof WritableItem))
        {
            throw new IllegalArgumentException("cliSection=" + cliSection
                    + ", getSize()=" + getSize()
                    + (cliSection != null ? ", getSection(cliSection.getData().getName())=" + getSection(cliSection.getData().getName()) : ""));
        }
        final int barIndex = cliSection.getPosition().getBar();
        final WritableItem<Section> wSection = (WritableItem<Section>) cliSection;
        final ChordLeadSheet oldContainer = wSection.getContainer();
        final ChordLeadSheet newContainer = this;
        wSection.setContainer(this);

        final CLI_Section prevSection = getSection(barIndex);
        if (prevSection.getPosition().getBar() == barIndex)
        {
            throw new IllegalArgumentException("cliSection=" + cliSection + " prevSection=" + prevSection);
        }

        // Add the section
        items.insertOrdered(wSection);

        // Adjust position of trailing items if required
        // For undo to work properly, must be done before firing 
        // the "Add section" UndoableEdit.        
        adjustItemsToTimeSignature(cliSection);

        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("Add section " + wSection)
        {
            @Override
            public void undoBody()
            {
                wSection.setContainer(oldContainer);
                items.remove(wSection);
                fireChangeEvent(new ItemRemovedEvent(ChordLeadSheetImpl.this, wSection));
            }

            @Override
            public void redoBody()
            {
                wSection.setContainer(newContainer);
                items.insertOrdered(wSection);
                fireChangeEvent(new ItemAddedEvent(ChordLeadSheetImpl.this, wSection));
            }
        };
        // Must be fired BEFORE the vetoable change
        fireUndoableEditHappened(edit);

        // Possible exception here !
        fireVetoableChangeEvent(new ItemAddedEvent(ChordLeadSheetImpl.this, cliSection));
    }

    @Override
    public void removeSection(final CLI_Section cliSection)
    {
        if (cliSection == null)
        {
            throw new IllegalArgumentException("cliSection=" + cliSection);
        }
        final int barIndex = cliSection.getPosition().getBar();
        final int index = items.indexOf(cliSection);
        if (barIndex == 0 || index == -1)
        {
            throw new IllegalArgumentException("cliSection=" + cliSection);
        }

        // remove the item and adjust if required trailing items
        items.remove(index);

        // Adjust section items position if required. 
        // For undo to work properly, must be done before firing 
        // the "Remove section" UndoableEdit.
        adjustItemsToTimeSignature(getSection(barIndex));

        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("Remove section " + cliSection)
        {
            @Override
            public void undoBody()
            {
                items.add(index, cliSection);
                fireChangeEvent(new ItemAddedEvent(ChordLeadSheetImpl.this, cliSection));
            }

            @Override
            public void redoBody()
            {
                items.remove(index);
                fireChangeEvent(new ItemRemovedEvent(ChordLeadSheetImpl.this, cliSection));
            }
        };
        fireUndoableEditHappened(edit);

        // Fire ItemChange event
        fireChangeEvent(new ItemRemovedEvent(ChordLeadSheetImpl.this, cliSection));
    }

    @Override
    public void moveSection(final CLI_Section section, final int newBarIndex)
    {
        if (section == null || newBarIndex <= 0 || newBarIndex >= getSize() || !(section instanceof WritableItem))
        {
            throw new IllegalArgumentException("section=" + section + " newBarIndex=" + newBarIndex);
        }

        final int oldBarIndex = section.getPosition().getBar();
        if (newBarIndex == oldBarIndex)
        {
            return;
        }
        CLI_Section newPosPrevSection = getSection(newBarIndex);
        if (oldBarIndex == 0 || newPosPrevSection.getPosition().getBar() == newBarIndex)
        {
            // Tried to move initial section, or there is already a section at destination
            throw new IllegalArgumentException("section=" + section + " newBarIndex=" + newBarIndex);
        }

        // OK move is safe
        final WritableItem<Section> wSection = (WritableItem<Section>) section;
        wSection.setPosition(new Position(newBarIndex, 0));
        final int indexOld = items.indexOf(wSection);
        if (indexOld == -1)
        {
            throw new IllegalArgumentException("wSection=" + wSection);
        }
        items.remove(wSection);
        final int indexNew = items.insertOrdered(wSection);

        // Section items adjusting must be done BEFORE firing the moved section undoable event
        // Adjust section at new position
        adjustItemsToTimeSignature(section);

        // Section before old position might need to be checked too
        CLI_Section oldPosPrevSection = getSection(oldBarIndex - 1);
        adjustItemsToTimeSignature(oldPosPrevSection);

        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("Move " + wSection)
        {
            @Override
            public void undoBody()
            {
                wSection.setPosition(new Position(oldBarIndex, 0));
                items.remove(wSection);
                items.add(indexOld, wSection);
                fireChangeEvent(new SectionMovedEvent(ChordLeadSheetImpl.this, section, newBarIndex));
            }

            @Override
            public void redoBody()
            {
                wSection.setPosition(new Position(newBarIndex, 0));
                items.remove(wSection);
                items.add(indexNew, wSection);
                fireChangeEvent(new SectionMovedEvent(ChordLeadSheetImpl.this, section, oldBarIndex));
            }
        };
        fireUndoableEditHappened(edit);

        // Fire ItemChange event
        fireChangeEvent(new SectionMovedEvent(ChordLeadSheetImpl.this, section, oldBarIndex));
    }

    @Override
    public <T> void removeItem(final ChordLeadSheetItem<T> item)
    {
        if (item == null || (item instanceof CLI_Section))
        {
            throw new IllegalArgumentException("item=" + item);
        }

        final int index = items.indexOf(item);
        if (index == -1)
        {
            throw new IllegalArgumentException("item=" + item);
        }

        items.remove(index);

        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("Remove " + item)
        {
            @Override
            public void undoBody()
            {
                items.add(index, item);
                fireChangeEvent(new ItemAddedEvent(ChordLeadSheetImpl.this, item));
            }

            @Override
            public void redoBody()
            {
                items.remove(item);
                fireChangeEvent(new ItemRemovedEvent(ChordLeadSheetImpl.this, item));
            }
        };
        fireUndoableEditHappened(edit);

        // Fire ItemChange event
        fireChangeEvent(new ItemRemovedEvent(ChordLeadSheetImpl.this, item));
    }

    @Override
    public <T> void moveItem(ChordLeadSheetItem<T> item, Position newPos)
    {
        if (item == null || newPos == null || !(item instanceof WritableItem) || (item instanceof CLI_Section))
        {
            throw new IllegalArgumentException("item=" + item + " newPos=" + newPos);
        }

        final WritableItem<T> wItem = (WritableItem<T>) item;
        final Position oldPos = wItem.getPosition();
        final Position newAdjustedPos = newPos.getAdjustedPosition(getSection(newPos.getBar()).getData().getTimeSignature());
        if (oldPos.equals(newAdjustedPos))
        {
            return;
        }

        // Change the position
        wItem.setPosition(newAdjustedPos);

        // And update items accordingly
        final int indexOld = items.indexOf(wItem);
        if (indexOld == -1)
        {
            throw new IllegalArgumentException("oldPosition=" + oldPos + " wItem=" + wItem + " items=" + items);
        }
        items.remove(wItem);
        final int indexNew = items.insertOrdered(wItem);

        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("Move " + wItem)
        {
            @Override
            public void undoBody()
            {
                wItem.setPosition(oldPos);
                items.remove(wItem);
                items.add(indexOld, wItem);
                fireChangeEvent(new ItemMovedEvent(ChordLeadSheetImpl.this, wItem, newAdjustedPos));
            }

            @Override
            public void redoBody()
            {
                wItem.setPosition(newAdjustedPos);
                items.remove(wItem);
                items.add(indexNew, wItem);
                fireChangeEvent(new ItemMovedEvent(ChordLeadSheetImpl.this, wItem, oldPos));
            }
        };
        fireUndoableEditHappened(edit);

        // Fire ItemChange event
        fireChangeEvent(new ItemMovedEvent(ChordLeadSheetImpl.this, wItem, oldPos));
    }

    @Override
    public <T> void changeItem(ChordLeadSheetItem<T> item, final T newData)
    {
        if (item == null || items.indexOf(item) == -1 || !(item instanceof WritableItem) || (item instanceof CLI_Section))
        {
            throw new IllegalArgumentException("item=" + item + " newData=" + newData + " items=" + items);
        }
        // Change item's data
        final T oldData = item.getData();
        if (oldData.equals(newData))
        {
            return;
        }

        // Do the change
        final WritableItem<T> wItem = (WritableItem<T>) item;
        wItem.setData(newData);

        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("Change " + item)
        {
            @Override
            public void undoBody()
            {
                wItem.setData(oldData);
                fireChangeEvent(new ItemChangedEvent(ChordLeadSheetImpl.this, wItem, newData));
            }

            @Override
            public void redoBody()
            {
                wItem.setData(newData);
                fireChangeEvent(new ItemChangedEvent(ChordLeadSheetImpl.this, wItem, oldData));
            }
        };
        fireUndoableEditHappened(edit);

        // Fire ItemChange event
        fireChangeEvent(new ItemChangedEvent(ChordLeadSheetImpl.this, item, oldData));

    }

    @Override
    public void insertBars(final int barIndex, final int nbBars)
    {
        if (barIndex < 0 || barIndex > getSize() || nbBars <= 0)
        {
            throw new IllegalArgumentException("barIndex=" + barIndex + " nbBars=" + nbBars);
        }

        // First set the size larger
        final int newSize = getSize() + nbBars;
        setSize(newSize);

        // And move items to make room for the new bars
        int indexFrom = items.getItemIndex(barIndex);
        if (indexFrom == -1)
        {
            // Nothing to do
            return;
        }
        shiftItemsPosition(items.getSubList(indexFrom, items.size() - 1), nbBars);
        if (indexFrom == 0)
        {
            // Special case: the initial section has been moved too, create a new initial section
            Section oldInitSection = ((CLI_Section) items.get(0)).getData();
            CLI_Factory clif = CLI_Factory.getDefault();
            CLI_Section newInitSection = clif.createSection(this, "_" + oldInitSection.getName(), oldInitSection.getTimeSignature(), 0);
            try
            {
                addSection(newInitSection);
            } catch (UnsupportedEditException ex)
            {
                // We should never be there since we don't change the time signature
                Exceptions.printStackTrace(ex);
            }
        }
    }

    @Override
    public void deleteBars(int barIndexFrom, int barIndexTo)
    {
        if (barIndexFrom < 0 || barIndexTo < barIndexFrom || barIndexTo >= getSize()
                || (barIndexTo - barIndexFrom + 1) >= getSize())
        {
            throw new IllegalArgumentException(
                    "barIndexFrom=" + barIndexFrom + " barIndexTo=" + barIndexTo);
        }
        int range = barIndexTo - barIndexFrom + 1;
        // Get items to be moved or removed
        List<ChordLeadSheetItem<?>> removedItems = new ArrayList<>();
        List<ChordLeadSheetItem<?>> movedItems = new ArrayList<>();
        // Avoid index=0  initial section
        for (int index = items.size() - 1; index > 0; index--)
        {
            ChordLeadSheetItem<?> item = items.get(index);
            int barIndex = item.getPosition().getBar();
            if (barIndex > barIndexTo)
            {
                // Shift item
                movedItems.add(item);
            } else if (barIndex >= barIndexFrom)
            {
                // We iterate backwards so remove is safe
                removedItems.add(item);
            } else
            {
                // We can stop
                break;
            }
        }

        // Handle exception if barIndexFrom == 0
        if (barIndexFrom == 0 && getSection(barIndexTo + 1) != getSection(barIndexTo))
        {
            // In this case we can safely delete the initial section which will be replaced
            removedItems.add(items.get(0));
        }

        // Perform the changes
        if (!removedItems.isEmpty())
        {
            removeItems(removedItems);
        }
        if (!movedItems.isEmpty())
        {
            shiftItemsPosition(movedItems, -range);
        }

        // Adjust the size
        setSize(getSize() - range);

        // Adjust positions if required
        if (barIndexFrom < getSize())
        {
            adjustItemsToTimeSignature(getSection(barIndexFrom));
        }
    }

    @Override
    public void setSectionName(CLI_Section cliSection, String name)
    {
        if (cliSection == null || items.indexOf(cliSection) == -1 || name == null || (getSection(name) != null && getSection(name) != cliSection)
                || !(cliSection instanceof WritableItem))
        {
            throw new IllegalArgumentException("section=" + cliSection + " name=" + name);
        }
        try
        {
            changeSection(cliSection, new Section(name, cliSection.getData().getTimeSignature()));
        } catch (UnsupportedEditException ex)
        {
            // Should never happen
            Exceptions.printStackTrace(ex);
        }
    }

    @Override
    public void setSectionTimeSignature(CLI_Section cliSection, TimeSignature ts) throws UnsupportedEditException
    {
        if (cliSection == null || ts == null || items.indexOf(cliSection) == -1
                || !(cliSection instanceof WritableItem))
        {
            throw new IllegalArgumentException("section=" + cliSection + " ts=" + ts);
        }
        changeSection(cliSection, new Section(cliSection.getData().getName(), ts));
    }

    @Override
    public void cleanup()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean contains(ChordLeadSheetItem<?> item)
    {
        return items.contains(item);
    }

    @Override
    public List<ChordLeadSheetItem<?>> getItems()
    {
        return new ArrayList<>(items);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<? extends T> getItems(Class<T> aClass)
    {
        List<T> result = new ArrayList<>();
        for (ChordLeadSheetItem<?> item : items)
        {
            if (aClass == null || aClass.isAssignableFrom(item.getClass()))
            {
                result.add((T) item);
            }
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<? extends T> getItems(int barFrom, int barTo, Class<T> aClass)
    {
        if (barFrom < 0 || barTo < barFrom || barTo >= getSize() || aClass == null)
        {
            throw new IllegalArgumentException(
                    "barFrom=" + barFrom + " barTo=" + barTo + " aClass=" + aClass);
        }

        List<T> result = new ArrayList<>();
        for (ChordLeadSheetItem<?> item : items)
        {
            int barIndex = item.getPosition().getBar();
            if (barIndex >= barFrom && barIndex <= barTo && (aClass == null || aClass.isAssignableFrom(item.getClass())))
            {
                result.add((T) item);
            } else if (barIndex > barTo)
            {
                break;
            }
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<? extends T> getItems(CLI_Section cliSection, Class<T> aClass)
    {
        if (cliSection == null || aClass == null)
        {
            throw new IllegalArgumentException("cliSection=" + cliSection + " aClass=" + aClass);
        }
        List<T> result = new ArrayList<>();
        int index = items.indexOf(cliSection);
        if (index == -1)
        {
            throw new IllegalArgumentException("cliSection=" + cliSection + " aClass=" + aClass + " items=" + items);
        }
        index++;
        while (index < items.size())
        {
            ChordLeadSheetItem<?> item = items.get(index);
            if (item instanceof CLI_Section)
            {
                break;
            }
            if (aClass == null || aClass.isAssignableFrom(item.getClass()))
            {
                result.add((T) item);
            }
            index++;
        }
        return result;
    }

    @Override
    public CLI_Section getSection(int barIndex)
    {
        if (barIndex < 0 || barIndex >= getSize())
        {
            throw new IllegalArgumentException("barIndex=" + barIndex);
        }
        CLI_Section lastSection = (CLI_Section) items.get(0);       // The initial block
        for (int i = 1; i < items.size(); i++)
        {
            ChordLeadSheetItem<?> item = items.get(i);
            if (item.getPosition().getBar() > barIndex)
            {
                break;
            }
            if (item instanceof CLI_Section)
            {
                lastSection = (CLI_Section) item;
            }
        }
        return lastSection;
    }

    @Override
    public CLI_Section getSection(String sectionName)
    {
        if (sectionName == null)
        {
            throw new NullPointerException("sectionName=" + sectionName);
        }
        for (ChordLeadSheetItem<?> item : items)
        {
            if (item instanceof CLI_Section)
            {
                CLI_Section cliSection = (CLI_Section) item;
                if (cliSection.getData().getName().equals(sectionName))
                {
                    return cliSection;
                }
            }
        }
        return null;
    }

    @Override
    public int getSectionSize(CLI_Section cliSection)
    {
        if (cliSection == null)
        {
            throw new NullPointerException("cliSection=" + cliSection);
        }
        int barSection = cliSection.getPosition().getBar();
        int index = items.indexOf(cliSection);
        if (index == -1)
        {
            throw new IllegalArgumentException("cliSection=" + cliSection);
        }
        index++;
        while (index < items.size())
        {
            ChordLeadSheetItem<?> item = items.get(index);
            if (item instanceof CLI_Section)
            {
                return item.getPosition().getBar() - barSection;
            }
            index++;
        }
        return getSize() - barSection;
    }

    @Override
    public void addUndoableEditListener(UndoableEditListener l)
    {
        if (l == null)
        {
            throw new NullPointerException("l=" + l);
        }
        undoListeners.remove(l);
        undoListeners.add(l);
    }

    @Override
    public void removeUndoableEditListener(UndoableEditListener l)
    {
        if (l == null)
        {
            throw new NullPointerException("l=" + l);
        }
        undoListeners.remove(l);
    }

    @Override
    public void addClsChangeListener(ClsChangeListener l)
    {
        if (l == null)
        {
            throw new NullPointerException("l=" + l);
        }
        listeners.remove(l);
        listeners.add(l);
    }

    @Override
    public void removeClsChangeListener(ClsChangeListener l)
    {
        if (l == null)
        {
            throw new NullPointerException("l=" + l);
        }
        listeners.remove(l);
    }

    @Override
    public String toString()
    {
        return "ChordLeadSheet section0=" + getSection(0).getData().getName() + " size=" + getSize();
    }

    public String toDumpString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(toString());
        for (ChordLeadSheetItem<?> item : items)
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

    // --------------------------------------------------------------------------------------
    // Private functions
    // --------------------------------------------------------------------------------------
    /**
     * Adjust the position of a section's items
     *
     * @param sectionNode
     */
    private void adjustItemsToTimeSignature(CLI_Section section)
    {
        if (section == null || items.indexOf(section) == -1)
        {
            throw new IllegalArgumentException("section=" + section);
        }
        TimeSignature ts = section.getData().getTimeSignature();
        for (ChordLeadSheetItem<?> item : getItems(section, ChordLeadSheetItem.class))
        {
            final Position oldPos = item.getPosition();
            final Position newPos = oldPos.getAdjustedPosition(ts);
            if (oldPos.equals(newPos))
            {
                continue;
            }
            final WritableItem<?> wItem = (WritableItem<?>) item;
            wItem.setPosition(newPos);

            // Undoable edit
            final ChordLeadSheet cls = this;
            UndoableEdit edit = new SimpleEdit("Adjust item position")
            {
                @Override
                public void undoBody()
                {
                    wItem.setPosition(oldPos);
                    fireChangeEvent(new ItemMovedEvent(ChordLeadSheetImpl.this, wItem, newPos));
                }

                @Override
                public void redoBody()
                {
                    wItem.setPosition(newPos);
                    fireChangeEvent(new ItemMovedEvent(ChordLeadSheetImpl.this, wItem, oldPos));
                }
            };
            fireUndoableEditHappened(edit);

            fireChangeEvent(new ItemMovedEvent(ChordLeadSheetImpl.this, item, oldPos));
        }
    }

    /**
     * Shift the position of a list of items of nbBars.
     *
     * @param shiftedItems
     * @param nbBars Shift
     */
    private void shiftItemsPosition(final List<ChordLeadSheetItem<?>> shiftedItems, final int nbBars)
    {
        if (shiftedItems == null)
        {
            throw new IllegalArgumentException("shiftedItems=" + shiftedItems + " nbBars=" + nbBars);
        }
        if (shiftedItems.isEmpty())
        {
            return;
        }
        final ArrayList<Position> oldPositions = new ArrayList<>();
        final ArrayList<Position> newPositions = new ArrayList<>();
        for (ChordLeadSheetItem<?> item : shiftedItems)
        {
            WritableItem<?> wItem = (WritableItem<?>) item;
            oldPositions.add(wItem.getPosition());
            Position newPos = wItem.getPosition();
            int newBar = newPos.getBar() + nbBars;
            if (newBar < 0 || newBar >= getSize())
            {
                throw new IllegalArgumentException("wItem=" + wItem + " nbBars=" + nbBars + " size="
                        + getSize());
            }
            newPos.setBar(newBar);
            wItem.setPosition(newPos);
            newPositions.add(newPos);
        }

        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("Move items " + nbBars + " bars")
        {
            @Override
            public void undoBody()
            {
                int i = 0;
                for (ChordLeadSheetItem<?> item : shiftedItems)
                {
                    Position oldPos = oldPositions.get(i++);
                    ((WritableItem) item).setPosition(oldPos);
                }
                fireChangeEvent(new ItemBarShiftedEvent(ChordLeadSheetImpl.this, shiftedItems, -nbBars));
            }

            @Override
            public void redoBody()
            {
                int i = 0;
                for (ChordLeadSheetItem<?> item : shiftedItems)
                {
                    Position newPos = newPositions.get(i++);
                    ((WritableItem) item).setPosition(newPos);
                }
                fireChangeEvent(new ItemBarShiftedEvent(ChordLeadSheetImpl.this, shiftedItems, nbBars));
            }
        };
        fireUndoableEditHappened(edit);

        // Fire the change
        fireChangeEvent(new ItemBarShiftedEvent(ChordLeadSheetImpl.this, shiftedItems, nbBars));
    }

    /**
     * Remove a list of items.
     */
    private void removeItems(final List<ChordLeadSheetItem<?>> removedItems)
    {
        if (removedItems == null)
        {
            throw new IllegalArgumentException("removedItems=" + removedItems);
        }
        if (removedItems.isEmpty())
        {
            return;
        }

        for (ChordLeadSheetItem<?> item : removedItems)
        {
            items.remove(item);
        }

        // Create the undoable event        
        UndoableEdit edit = new SimpleEdit("Remove items")
        {
            @Override
            public void undoBody()
            {
                for (ChordLeadSheetItem<?> item : removedItems)
                {
                    items.insertOrdered(item);
                }
                fireChangeEvent(new ItemAddedEvent(ChordLeadSheetImpl.this, removedItems));
            }

            @Override
            public void redoBody()
            {
                for (ChordLeadSheetItem<?> item : removedItems)
                {
                    items.remove(item);
                }
                fireChangeEvent(new ItemRemovedEvent(ChordLeadSheetImpl.this, removedItems));
            }
        };
        fireUndoableEditHappened(edit);

        // Fire the change
        fireChangeEvent(new ItemRemovedEvent(ChordLeadSheetImpl.this, removedItems));
    }

    private void changeSection(CLI_Section cliSection, final Section newData) throws UnsupportedEditException
    {
        if (cliSection == null || !(cliSection instanceof WritableItem) || newData == null)
        {
            throw new IllegalArgumentException("cliSection=" + cliSection + " newData=" + newData);
        }
        final Section oldData = cliSection.getData();
        if (oldData.equals(newData))
        {
            return;
        }
        final WritableItem<Section> wSection = (WritableItem<Section>) cliSection;
        wSection.setData(newData);

        if (!oldData.getTimeSignature().equals(newData.getTimeSignature()))
        {
            // Possibly adjust items position if we have a new time signature with less natural beats 
            // (and generate undoable SimpleEdits accordingly)
            adjustItemsToTimeSignature(cliSection);
        }

        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("Change Section " + cliSection)
        {
            @Override
            public void undoBody()
            {
                wSection.setData(oldData);
                fireChangeEvent(new ItemChangedEvent(ChordLeadSheetImpl.this, wSection, newData));
            }

            @Override
            public void redoBody()
            {
                wSection.setData(newData);
                fireChangeEvent(new ItemChangedEvent(ChordLeadSheetImpl.this, wSection, oldData));
            }
        };
        // Must be fired BEFORE the vetoablechangeevent to enable a clean undo if exception
        fireUndoableEditHappened(edit);

        // Possible exception here !
        fireVetoableChangeEvent(new ItemChangedEvent(ChordLeadSheetImpl.this, wSection, oldData));
    }

    /**
     * Convenience method, identitical to fireVetoableChangeEvent, except that caller considers that an UnsupportedEditException
     * will never be thrown.
     *
     * @param event
     * @throws IllegalStateException If an UnsupportedEditException was catched.
     */
    private void fireChangeEvent(ClsChangeEvent event)
    {
        if (event == null)
        {
            throw new IllegalArgumentException("event=" + event);
        }
        for (ClsChangeListener l : listeners.toArray(new ClsChangeListener[listeners.size()]))
        {
            try
            {
                l.chordLeadSheetChanged(event);
            } catch (UnsupportedEditException ex)
            {
                LOGGER.severe("fireChangeEvent() unexpected UnsupportedEditException ex=" + ex);
                throw new IllegalStateException("Unexpected UnsupportedEditException", ex);
            }
        }
    }

    private void fireVetoableChangeEvent(ClsChangeEvent event) throws UnsupportedEditException
    {
        if (event == null)
        {
            throw new IllegalArgumentException("event=" + event);
        }
        for (ClsChangeListener l : listeners.toArray(new ClsChangeListener[listeners.size()]))
        {
            l.chordLeadSheetChanged(event);
        }
    }

    private void fireUndoableEditHappened(UndoableEdit edit)
    {
        if (edit == null)
        {
            throw new IllegalArgumentException("edit=" + edit);
        }
        UndoableEditEvent event = new UndoableEditEvent(this, edit);
        for (UndoableEditListener l : undoListeners.toArray(new UndoableEditListener[undoListeners.size()]))
        {
            l.undoableEditHappened(event);
        }
    }

    // --------------------------------------------------------------------- 
    //  Serialization
    // --------------------------------------------------------------------- */
    private Object writeReplace()
    {
        return new SerializationProxy(this);
    }

    private void readObject(ObjectInputStream stream) throws InvalidObjectException
    {
        throw new InvalidObjectException("Serialization proxy required");
    }

    /**
     * Need to restore each item's container.
     * <p>
     * Allow to be independent of future chordleadsheet internal data structure changes.
     */
    private static class SerializationProxy implements Serializable
    {

        private static final long serialVersionUID = 2879716323116L;
        private final int spVERSION = 1;
        private final ArrayList<ChordLeadSheetItem<?>> spItems;
        private final int spSize;

        private SerializationProxy(ChordLeadSheetImpl cls)
        {
            spSize = cls.getSize();
            spItems = new ArrayList<>();
            spItems.addAll(cls.getItems());
        }

        private Object readResolve() throws ObjectStreamException
        {
            if (spItems == null || spItems.size() < 1 || !(spItems.get(0) instanceof CLI_Section))
            {
                throw new IllegalStateException("spItems=" + spItems);
            }
            // CLI_Section's container field must be transient, otherwise with Xstream line below produces 
            // a non-null but empty section (data=null, pos=null).            
            // See Effective Java p315.
            CLI_Section initSection = (CLI_Section) spItems.get(0);
            ChordLeadSheetImpl cls = new ChordLeadSheetImpl(initSection, spSize);
            for (int i = 1; i < spItems.size(); i++)
            {
                ChordLeadSheetItem<?> item = spItems.get(i);
                if (item instanceof CLI_Section)
                {
                    try
                    {
                        cls.addSection((CLI_Section) item);
                    } catch (UnsupportedEditException ex)
                    {
                        // Translate to an ObjectStreamException
                        throw new InvalidObjectException(ex.getLocalizedMessage());
                    }
                } else
                {
                    cls.addItem(item);
                }
            }
            return cls;
        }
    }
}

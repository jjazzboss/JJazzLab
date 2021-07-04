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

import org.jjazz.undomanager.api.SimpleEdit;
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
import org.jjazz.harmony.api.TimeSignature;
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
import org.jjazz.util.api.IntRange;
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
            throw new IllegalArgumentException("section=" + initSection + " size=" + size);   //NOI18N
        }
        this.size = size;
        WritableItem<Section> wSection = (WritableItem<Section>) initSection;
        wSection.setContainer(this);
        items.insertOrdered(initSection);
    }

    @Override
    public void setSize(final int newSize) throws UnsupportedEditException
    {
        if (newSize < 1 || newSize > MAX_SIZE)
        {
            throw new IllegalArgumentException("newSize=" + newSize);   //NOI18N
        }

        LOGGER.fine("setSize() -- newSize=" + newSize);   //NOI18N

        // First remove items
        final int oldSize = size;
        int delta = newSize - oldSize;
        List<ChordLeadSheetItem<?>> removedItems = new ArrayList<>();
        if (delta == 0)
        {
            // Easy
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
                removeSectionsAndItems(removedItems);
            }
        }

        // Then update size
        // Check that change is not vetoed
        var event = new SizeChangedEvent(ChordLeadSheetImpl.this, oldSize, newSize);
        authorizeChangeEvent(event);        // Possible exception here !

        // Update state
        size = newSize;

        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("Set Size " + newSize)
        {
            @Override
            public void undoBody()
            {
                LOGGER.finer("setSize.undoBody() newSize=" + newSize);   //NOI18N
                size = oldSize;
                fireAuthorizedChangeEvent(new SizeChangedEvent(ChordLeadSheetImpl.this, newSize, oldSize));
            }

            @Override
            public void redoBody()
            {
                LOGGER.finer("setSize.redoBody() newSize=" + newSize);   //NOI18N
                size = newSize;
                fireAuthorizedChangeEvent(new SizeChangedEvent(ChordLeadSheetImpl.this, oldSize, newSize));
            }
        };

        fireUndoableEditHappened(edit);

        fireAuthorizedChangeEvent(event);
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
            throw new IllegalArgumentException("item=" + item);   //NOI18N
        }

        LOGGER.fine("addItem() -- item=" + item);   //NOI18N

        final WritableItem<?> wItem = (WritableItem<?>) item;
        int barIndex = wItem.getPosition().getBar();
        if (barIndex >= getSize())
        {
            throw new IllegalArgumentException("item=" + item + " size=" + getSize());   //NOI18N
        }

        // Set the container
        final ChordLeadSheet oldContainer = wItem.getContainer();
        final ChordLeadSheet newContainer = this;
        wItem.setContainer(newContainer);

        // Adjust position if required
        final Position oldPos = wItem.getPosition();
        final Position newAdjustedPos = oldPos.limitToTimeSignature(getSection(barIndex).getData().
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
                LOGGER.finer("addItem.undoBody() item=" + item);   //NOI18N
                wItem.setPosition(oldPos);
                wItem.setContainer(oldContainer);
                items.remove(wItem);
                fireAuthorizedChangeEvent(new ItemRemovedEvent(ChordLeadSheetImpl.this, wItem));
            }

            @Override
            public void redoBody()
            {
                LOGGER.finer("addItem.redoBody() item=" + item);   //NOI18N
                wItem.setPosition(newAdjustedPos);
                wItem.setContainer(newContainer);
                items.add(index, wItem);
                fireAuthorizedChangeEvent(new ItemAddedEvent(ChordLeadSheetImpl.this, wItem));
            }
        };

        fireUndoableEditHappened(edit);

        // Fire ItemChange event
        fireAuthorizedChangeEvent(new ItemAddedEvent(ChordLeadSheetImpl.this, wItem));
    }

    @Override
    public void addSection(final CLI_Section cliSection) throws UnsupportedEditException
    {
        if (cliSection == null || cliSection.getPosition().getBar() >= getSize() || getSection(cliSection.getData().getName()) != null
                || !(cliSection instanceof WritableItem))
        {
            throw new IllegalArgumentException("cliSection=" + cliSection //NOI18N
                    + ", getSize()=" + getSize()
                    + (cliSection != null ? ", getSection(cliSection.getData().getName())=" + getSection(cliSection.getData().getName()) : ""));
        }

        LOGGER.fine("addSection() -- cliSection=" + cliSection);   //NOI18N

        // Check that change is not vetoed
        var event = new ItemAddedEvent(ChordLeadSheetImpl.this, cliSection);
        authorizeChangeEvent(event);        // Possible exception here !

        // Prepare data
        final int barIndex = cliSection.getPosition().getBar();
        final WritableItem<Section> wSection = (WritableItem<Section>) cliSection;
        final ChordLeadSheet oldContainer = wSection.getContainer();
        final ChordLeadSheet newContainer = this;
        wSection.setContainer(this);
        final CLI_Section prevSection = getSection(barIndex);
        if (prevSection.getPosition().getBar() == barIndex)
        {
            throw new IllegalArgumentException("cliSection=" + cliSection + " prevSection=" + prevSection);   //NOI18N
        }

        // Change state
        items.insertOrdered(wSection);

        // Adjust position of trailing items if required
        // For undo to work properly, must be done before firing 
        // the "Add section" UndoableEdit.        
        adjustItemsToTimeSignature(prevSection.getData().getTimeSignature(), cliSection.getData().getTimeSignature(), getItems(cliSection, ChordLeadSheetItem.class));

        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("Add section " + wSection)
        {
            @Override
            public void undoBody()
            {
                LOGGER.finer("addSection.undoBody() cliSection=" + cliSection);   //NOI18N
                wSection.setContainer(oldContainer);
                items.remove(wSection);
                fireAuthorizedChangeEvent(new ItemRemovedEvent(ChordLeadSheetImpl.this, wSection));
            }

            @Override
            public void redoBody()
            {
                LOGGER.finer("addSection.redoBody() cliSection=" + cliSection);   //NOI18N
                wSection.setContainer(newContainer);
                items.insertOrdered(wSection);
                fireAuthorizedChangeEvent(new ItemAddedEvent(ChordLeadSheetImpl.this, wSection));
            }
        };

        // Must be fired BEFORE the vetoable change
        fireUndoableEditHappened(edit);

        fireAuthorizedChangeEvent(event);
    }

    @Override
    public void removeSection(final CLI_Section cliSection) throws UnsupportedEditException
    {
        if (cliSection == null)
        {
            throw new IllegalArgumentException("cliSection=" + cliSection);   //NOI18N
        }

        LOGGER.fine("removeSection() -- cliSection=" + cliSection);   //NOI18N

        final int barIndex = cliSection.getPosition().getBar();
        final int index = items.indexOf(cliSection);
        if (barIndex == 0 || index == -1)
        {
            throw new IllegalArgumentException("cliSection=" + cliSection);   //NOI18N
        }

        // Check change is not vetoed
        var event = new ItemRemovedEvent(ChordLeadSheetImpl.this, cliSection);
        authorizeChangeEvent(event);            // Possible exception here! 

        // Prepare data
        var prevSection = getSection(barIndex - 1);
        var removedSectionItems = getItems(cliSection, ChordLeadSheetItem.class);

        // Change state: remove the item and adjust if required trailing items
        items.remove(index);

        // Adjust removed section items position if required. 
        // For undo to work properly, must be done before firing 
        // the "Remove section" UndoableEdit.
        adjustItemsToTimeSignature(cliSection.getData().getTimeSignature(), prevSection.getData().getTimeSignature(), removedSectionItems);

        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("Remove section " + cliSection)
        {
            @Override
            public void undoBody()
            {
                LOGGER.finer("removeSection.undoBody() cliSection=" + cliSection);   //NOI18N
                items.add(index, cliSection);
                fireAuthorizedChangeEvent(new ItemAddedEvent(ChordLeadSheetImpl.this, cliSection));
            }

            @Override
            public void redoBody()
            {
                LOGGER.finer("removeSection.redoBody() cliSection=" + cliSection);   //NOI18N
                items.remove(index);
                fireAuthorizedChangeEvent(new ItemRemovedEvent(ChordLeadSheetImpl.this, cliSection));
            }
        };

        fireUndoableEditHappened(edit);

        // Fire ItemChange event
        fireAuthorizedChangeEvent(event);
    }

    @Override
    public void moveSection(final CLI_Section cliSection, final int newBarIndex) throws UnsupportedEditException
    {
        if (cliSection == null || newBarIndex <= 0 || newBarIndex >= getSize() || !(cliSection instanceof WritableItem))
        {
            throw new IllegalArgumentException("cliSection=" + cliSection + " newBarIndex=" + newBarIndex);   //NOI18N
        }

        LOGGER.fine("moveSection() -- cliSection=" + cliSection + " newBarIndex=" + newBarIndex);   //NOI18N

        final int oldBarIndex = cliSection.getPosition().getBar();
        if (newBarIndex == oldBarIndex)
        {
            return;
        }

        CLI_Section newPosPrevSection = getSection(newBarIndex);
        if (oldBarIndex == 0 || newPosPrevSection.getPosition().getBar() == newBarIndex)
        {
            // Tried to move initial section, or there is already a section at destination
            throw new IllegalArgumentException("section=" + cliSection + " newBarIndex=" + newBarIndex);   //NOI18N
        }

        if (items.indexOf(cliSection) == -1)
        {
            throw new IllegalArgumentException("section=" + cliSection);   //NOI18N
        }

        // Check change is not vetoed
        var event = new SectionMovedEvent(ChordLeadSheetImpl.this, cliSection, oldBarIndex, newBarIndex);
        authorizeChangeEvent(event);            // Possible exception here! 

        // OK move is safe, change is safe
        final WritableItem<Section> wSection = (WritableItem<Section>) cliSection;
        wSection.setPosition(new Position(newBarIndex, 0));
        items.remove(wSection);
        items.insertOrdered(wSection);

        // Section items adjusting must be done BEFORE firing the moved section undoable event
        // Adjust section at new position
        adjustItemsToTimeSignature(newPosPrevSection.getData().getTimeSignature(), cliSection.getData().getTimeSignature(), getItems(cliSection, ChordLeadSheetItem.class));

        // Section before old position might need to be checked too
        CLI_Section oldPosPrevSection = getSection(oldBarIndex - 1);
        var oldItems = getItems(oldBarIndex, getSectionRange(oldPosPrevSection).to, ChordLeadSheetItem.class);
        adjustItemsToTimeSignature(cliSection.getData().getTimeSignature(), oldPosPrevSection.getData().getTimeSignature(), oldItems);

        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("Move " + wSection)
        {
            @Override
            public void undoBody()
            {
                LOGGER.finer("moveSection.undoBody() cliSection=" + cliSection + " newBarIndex=" + newBarIndex);   //NOI18N
                wSection.setPosition(new Position(oldBarIndex, 0));
                items.remove(wSection);
                items.insertOrdered(wSection);
                fireAuthorizedChangeEvent(new SectionMovedEvent(ChordLeadSheetImpl.this, cliSection, newBarIndex, oldBarIndex));
            }

            @Override
            public void redoBody()
            {
                LOGGER.finer("moveSection.redoBody() cliSection=" + cliSection + " newBarIndex=" + newBarIndex);   //NOI18N
                wSection.setPosition(new Position(newBarIndex, 0));
                items.remove(wSection);
                items.insertOrdered(wSection);
                fireAuthorizedChangeEvent(new SectionMovedEvent(ChordLeadSheetImpl.this, cliSection, oldBarIndex, newBarIndex));
            }
        };

        fireUndoableEditHappened(edit);

        // Fire ItemChange event
        fireAuthorizedChangeEvent(event);
    }

    @Override
    public <T> void removeItem(final ChordLeadSheetItem<T> item)
    {
        if (item == null || (item instanceof CLI_Section))
        {
            throw new IllegalArgumentException("item=" + item);   //NOI18N
        }

        LOGGER.fine("removeItem() -- item=" + item);   //NOI18N

        final int index = items.indexOf(item);
        if (index == -1)
        {
            throw new IllegalArgumentException("item=" + item);   //NOI18N
        }

        // Change state
        items.remove(index);

        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("Remove " + item)
        {
            @Override
            public void undoBody()
            {
                LOGGER.finer("removeItem.undoBody() item=" + item);   //NOI18N
                items.insertOrdered(item);
                fireAuthorizedChangeEvent(new ItemAddedEvent(ChordLeadSheetImpl.this, item));
            }

            @Override
            public void redoBody()
            {
                LOGGER.finer("removeItem.redoBody() item=" + item);   //NOI18N
                items.remove(item);
                fireAuthorizedChangeEvent(new ItemRemovedEvent(ChordLeadSheetImpl.this, item));
            }
        };

        fireUndoableEditHappened(edit);

        // Fire ItemChange event
        fireAuthorizedChangeEvent(new ItemRemovedEvent(ChordLeadSheetImpl.this, item));
    }

    @Override
    public <T> void moveItem(ChordLeadSheetItem<T> item, Position newPos)
    {
        if (item == null || newPos == null || !(item instanceof WritableItem) || (item instanceof CLI_Section))
        {
            throw new IllegalArgumentException("item=" + item + " newPos=" + newPos);   //NOI18N
        }

        LOGGER.fine("moveItem() -- item=" + item + " newPos=" + newPos);   //NOI18N

        final WritableItem<T> wItem = (WritableItem<T>) item;
        final Position oldPos = wItem.getPosition();
        final Position newAdjustedPos = newPos.limitToTimeSignature(getSection(newPos.getBar()).getData().getTimeSignature());
        if (oldPos.equals(newAdjustedPos))
        {
            return;
        }

        // Change the position 
        if (!items.remove(wItem))
        {
            throw new IllegalArgumentException("oldPosition=" + oldPos + " wItem=" + wItem + " items=" + items);   //NOI18N
        }
        wItem.setPosition(newAdjustedPos);
        items.insertOrdered(wItem);

        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("Move " + wItem)
        {
            @Override
            public void undoBody()
            {
                LOGGER.finer("moveItem.undoBody() item=" + item + " oldPos=" + oldPos + " newAdjustedPos=" + newAdjustedPos);   //NOI18N
                items.remove(wItem);
                wItem.setPosition(oldPos);
                items.insertOrdered(wItem);
                fireAuthorizedChangeEvent(new ItemMovedEvent(ChordLeadSheetImpl.this, wItem, newAdjustedPos, oldPos));
            }

            @Override
            public void redoBody()
            {
                LOGGER.finer("moveItem.redoBody() item=" + item + " oldPos=" + oldPos + " newAdjustedPos=" + newAdjustedPos);   //NOI18N
                items.remove(wItem);
                wItem.setPosition(newAdjustedPos);
                items.insertOrdered(wItem);
                fireAuthorizedChangeEvent(new ItemMovedEvent(ChordLeadSheetImpl.this, wItem, oldPos, newAdjustedPos));
            }
        };
        fireUndoableEditHappened(edit);

        // Fire ItemChange event
        fireAuthorizedChangeEvent(new ItemMovedEvent(ChordLeadSheetImpl.this, wItem, oldPos, newAdjustedPos));
    }

    @Override
    public <T> void changeItem(ChordLeadSheetItem<T> item, final T newData)
    {
        if (item == null || items.indexOf(item) == -1 || !(item instanceof WritableItem) || (item instanceof CLI_Section))
        {
            throw new IllegalArgumentException("item=" + item + " newData=" + newData + " items=" + items);   //NOI18N
        }

        LOGGER.fine("changeItem() -- item=" + item + " newData=" + newData);   //NOI18N

        final T oldData = item.getData();
        if (oldData.equals(newData))
        {
            return;
        }

        // Change state
        final WritableItem<T> wItem = (WritableItem<T>) item;
        wItem.setData(newData);

        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("Change " + item)
        {
            @Override
            public void undoBody()
            {
                LOGGER.finer("changeItem.undoBody() item=" + item + " oldData=" + oldData + " newData=" + newData);   //NOI18N
                wItem.setData(oldData);
                fireAuthorizedChangeEvent(new ItemChangedEvent(ChordLeadSheetImpl.this, wItem, newData, oldData));
            }

            @Override
            public void redoBody()
            {
                LOGGER.finer("changeItem.undoBody() item=" + item + " oldData=" + oldData + " newData=" + newData);   //NOI18N
                wItem.setData(newData);
                fireAuthorizedChangeEvent(new ItemChangedEvent(ChordLeadSheetImpl.this, wItem, oldData, newData));
            }
        };
        fireUndoableEditHappened(edit);

        // Fire ItemChange event
        fireAuthorizedChangeEvent(new ItemChangedEvent(ChordLeadSheetImpl.this, item, oldData, newData));

    }

    @Override
    public void insertBars(final int barIndex, final int nbBars)
    {
        if (barIndex < 0 || barIndex > getSize() || nbBars <= 0)
        {
            throw new IllegalArgumentException("barIndex=" + barIndex + " nbBars=" + nbBars);   //NOI18N
        }

        LOGGER.fine("insertBars() -- barIndex=" + barIndex + " nbBars=" + nbBars);   //NOI18N

        // First set the size larger
        final int newSize = getSize() + nbBars;
        try
        {
            setSize(newSize);
        } catch (UnsupportedEditException ex)
        {
            // Should never happen as we increase the size
            Exceptions.printStackTrace(ex);
        }

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
    public void deleteBars(int barIndexFrom, int barIndexTo) throws UnsupportedEditException
    {
        if (barIndexFrom < 0 || barIndexTo < barIndexFrom || barIndexTo >= getSize()
                || (barIndexTo - barIndexFrom + 1) >= getSize())
        {
            throw new IllegalArgumentException( //NOI18N
                    "barIndexFrom=" + barIndexFrom + " barIndexTo=" + barIndexTo);
        }

        LOGGER.fine("deleteBars() -- barIndexFrom=" + barIndexFrom + " barIndexTo=" + barIndexTo);   //NOI18N

        // Save data after the bar deletions
        CLI_Section afterDeletionSection = (barIndexTo + 1 > size - 1) ? null : getSection(barIndexTo + 1);
        @SuppressWarnings("rawtypes")
        List<? extends ChordLeadSheetItem> afterDeletionItems = null;
        if (afterDeletionSection != null)
        {
            afterDeletionItems = getItems(barIndexTo + 1, getSectionRange(afterDeletionSection).to, ChordLeadSheetItem.class);
        }

        // Get items to be moved or removed
        List<ChordLeadSheetItem<?>> removedItems = new ArrayList<>();
        List<ChordLeadSheetItem<?>> movedItems = new ArrayList<>();

        // Avoid initial section at index=0
        // Iterate backwards so that remove operations have less impact
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
                removedItems.add(item);

            } else
            {
                // We can stop
                break;
            }
        }

        // Remove everything to be removed except the initial block
        if (!removedItems.isEmpty())
        {
            // Possible exception below! Note that some changes might have been done before exception is thrown
            removeSectionsAndItems(removedItems);
        }

        // Handle special case if barIndexFrom == 0 and there is a section right after the deleted bars
        if (barIndexFrom == 0 && afterDeletionSection != null && afterDeletionSection.getPosition().getBar() == barIndexTo + 1)
        {
            // Remove the initial section (and fire undoableEvent)
            removeInitialSection();
        }

        // Shift remaining items
        int range = barIndexTo - barIndexFrom + 1;
        if (!movedItems.isEmpty())
        {
            shiftItemsPosition(movedItems, -range);
        }

        // Adjust the size
        setSize(getSize() - range);         // Possible exception here! But should not happen since we removed items/sections before.

        // Adjust positions of items after the deletion if any
        if (afterDeletionItems != null)
        {
            CLI_Section newSection = getSection(barIndexFrom);
            adjustItemsToTimeSignature(afterDeletionSection.getData().getTimeSignature(), newSection.getData().getTimeSignature(), afterDeletionItems);
        }

    }

    @Override
    public void setSectionName(CLI_Section cliSection, String name)
    {
        if (cliSection == null || items.indexOf(cliSection) == -1 || name == null || (getSection(name) != null && getSection(name) != cliSection)
                || !(cliSection instanceof WritableItem))
        {
            throw new IllegalArgumentException("section=" + cliSection + " name=" + name);   //NOI18N
        }

        LOGGER.fine("setSectionName() -- cliSection=" + cliSection + " name=" + name);   //NOI18N

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
            throw new IllegalArgumentException("section=" + cliSection + " ts=" + ts);   //NOI18N
        }

        LOGGER.fine("setSectionTimeSignature() -- cliSection=" + cliSection + " ts=" + ts);   //NOI18N

        changeSection(cliSection, new Section(cliSection.getData().getName(), ts));
    }

    @Override
    public void cleanup()
    {
        // Nothing
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
    public <T> T getLastItem(int barFrom, int barTo, Class<T> aClass)
    {
        if (barFrom < 0 || barTo < barFrom || barTo >= getSize())
        {
            throw new IllegalArgumentException( //NOI18N
                    "barFrom=" + barFrom + " barTo=" + barTo + " aClass=" + aClass);
        }
        T res = null;
        for (int i = items.size() - 1; i >= 0; i--)
        {
            var item = items.get(i);
            int barIndex = item.getPosition().getBar();
            
            
            if (barIndex > barTo
                    || (aClass != null && !aClass.isAssignableFrom(item.getClass())))
            {
                continue;
            }

            if (barIndex < barFrom)
            {
                break;
            }

            res = (T) item;
            break;
        }

        return res;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<? extends T> getItems(int barFrom, int barTo, Class<T> aClass)
    {
        if (barFrom < 0 || barTo < barFrom || barTo >= getSize())
        {
            throw new IllegalArgumentException( //NOI18N
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
        if (cliSection == null)
        {
            throw new IllegalArgumentException("cliSection=" + cliSection + " aClass=" + aClass);   //NOI18N
        }
        List<T> result = new ArrayList<>();
        int index = items.indexOf(cliSection);
        if (index == -1)
        {
            throw new IllegalArgumentException("cliSection=" + cliSection + " aClass=" + aClass + " items=" + items);   //NOI18N
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
            throw new IllegalArgumentException("barIndex=" + barIndex);   //NOI18N
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
            throw new NullPointerException("sectionName=" + sectionName);   //NOI18N
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
    public IntRange getSectionRange(CLI_Section cliSection)
    {
        if (cliSection == null)
        {
            throw new NullPointerException("cliSection=" + cliSection);   //NOI18N
        }
        int startBar = cliSection.getPosition().getBar();
        int index = items.indexOf(cliSection);
        if (index == -1)
        {
            throw new IllegalArgumentException("cliSection=" + cliSection);   //NOI18N
        }

        index++;
        while (index < items.size())
        {
            ChordLeadSheetItem<?> item = items.get(index);
            if (item instanceof CLI_Section)
            {
                return new IntRange(startBar, item.getPosition().getBar() - 1);
            }
            index++;
        }
        return new IntRange(startBar, getSize() - 1);
    }

    @Override
    public void addUndoableEditListener(UndoableEditListener l)
    {
        if (l == null)
        {
            throw new NullPointerException("l=" + l);   //NOI18N
        }
        undoListeners.remove(l);
        undoListeners.add(l);
    }

    @Override
    public void removeUndoableEditListener(UndoableEditListener l)
    {
        if (l == null)
        {
            throw new NullPointerException("l=" + l);   //NOI18N
        }
        undoListeners.remove(l);
    }

    @Override
    public void addClsChangeListener(ClsChangeListener l)
    {
        if (l == null)
        {
            throw new NullPointerException("l=" + l);   //NOI18N
        }
        listeners.remove(l);
        listeners.add(l);
    }

    @Override
    public void removeClsChangeListener(ClsChangeListener l)
    {
        if (l == null)
        {
            throw new NullPointerException("l=" + l);   //NOI18N
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
    // Private methods
    // --------------------------------------------------------------------------------------
    /**
     * Remove sections and items.
     * <p>
     * Chord symbols are removed in a single undoable operation. Then sections are removed one by one using removeSection()
     * (possible exception).
     *
     * @param allItems
     * @throws UnsupportedEditException
     */
    private void removeSectionsAndItems(List<ChordLeadSheetItem<?>> allItems) throws UnsupportedEditException
    {
        LOGGER.finer("removeSectionsAndItems() -- removedItems=" + allItems);   //NOI18N

        if (allItems.isEmpty())
        {
            return;
        }

        // Make 2 lists: removedSections for sections, removedItems for other items
        final List<CLI_Section> removedSections = new ArrayList<>();
        final List<ChordLeadSheetItem<?>> removedItems = new ArrayList<>();
        for (var item : allItems)
        {
            if (item instanceof CLI_Section)
            {
                removedSections.add((CLI_Section) item);
            } else
            {
                removedItems.add(item);
            }
        }

        // Remove all non section items
        if (!removedItems.isEmpty())
        {

            items.removeAll(removedItems);

            // Create the undoable event        
            UndoableEdit edit = new SimpleEdit("Remove items")
            {
                @Override
                public void undoBody()
                {
                    LOGGER.finer("removeSectionsAndItems.undoBody() removedItems=" + removedItems);   //NOI18N
                    for (ChordLeadSheetItem<?> item : removedItems)
                    {
                        items.insertOrdered(item);
                    }
                    fireAuthorizedChangeEvent(new ItemAddedEvent(ChordLeadSheetImpl.this, removedItems));
                }

                @Override
                public void redoBody()
                {
                    LOGGER.finer("removeSectionsAndItems.redoBody() removedItems=" + removedItems);   //NOI18N
                    items.removeAll(removedItems);
                    fireAuthorizedChangeEvent(new ItemRemovedEvent(ChordLeadSheetImpl.this, removedItems));
                }
            };
            fireUndoableEditHappened(edit);

            // Fire the change
            fireAuthorizedChangeEvent(new ItemRemovedEvent(ChordLeadSheetImpl.this, removedItems));
        }

        // Remove all sections
        for (CLI_Section section : removedSections)
        {
            removeSection(section);      // Possible exception here
        }

    }

    /**
     * Adjust the position of the specified items from oldTs to newTs.
     *
     * @param oldTs
     * @param newTs
     * @param items
     */
    @SuppressWarnings("rawtypes")
    private void adjustItemsToTimeSignature(TimeSignature oldTs, TimeSignature newTs, List<? extends ChordLeadSheetItem> items)
    {
        if (newTs == null || items == null || oldTs == null)
        {
            throw new IllegalArgumentException("items=" + items + " oldTs=" + oldTs + " newTs=" + newTs);   //NOI18N
        }

        LOGGER.finer("adjustItemsToTimeSignature() -- oldTs=" + oldTs + " newTs=" + newTs + " items=" + items);   //NOI18N

        if (oldTs.equals(newTs))
        {
            return;
        }

        for (ChordLeadSheetItem<?> item : items)
        {
            if (item instanceof CLI_Section)
            {
                continue;
            }
            Position oldPos = item.getPosition();
            Position newPos = oldPos.getConvertedPosition(oldTs, newTs);
            moveItem(item, newPos);
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
            throw new IllegalArgumentException("shiftedItems=" + shiftedItems + " nbBars=" + nbBars);   //NOI18N
        }

        LOGGER.finer("shiftItemsPosition() -- shiftedItems=" + shiftedItems + " nbBars=" + nbBars);   //NOI18N

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
                throw new IllegalArgumentException("wItem=" + wItem + " nbBars=" + nbBars + " size=" //NOI18N
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
                LOGGER.finer("shiftItemsPosition.undoBody() shiftedItems=" + shiftedItems + " nbBars=" + nbBars);   //NOI18N
                int i = 0;
                for (ChordLeadSheetItem<?> item : shiftedItems)
                {
                    Position oldPos = oldPositions.get(i++);
                    ((WritableItem) item).setPosition(oldPos);
                }
                fireAuthorizedChangeEvent(new ItemBarShiftedEvent(ChordLeadSheetImpl.this, shiftedItems, -nbBars));
            }

            @Override
            public void redoBody()
            {
                LOGGER.finer("shiftItemsPosition.redoBody() shiftedItems=" + shiftedItems + " nbBars=" + nbBars);   //NOI18N
                int i = 0;
                for (ChordLeadSheetItem<?> item : shiftedItems)
                {
                    Position newPos = newPositions.get(i++);
                    ((WritableItem) item).setPosition(newPos);
                }
                fireAuthorizedChangeEvent(new ItemBarShiftedEvent(ChordLeadSheetImpl.this, shiftedItems, nbBars));
            }
        };
        fireUndoableEditHappened(edit);

        // Fire the change
        fireAuthorizedChangeEvent(new ItemBarShiftedEvent(ChordLeadSheetImpl.this, shiftedItems, nbBars));
    }

    private void changeSection(CLI_Section cliSection, final Section newData) throws UnsupportedEditException
    {
        if (cliSection == null || !(cliSection instanceof WritableItem) || newData == null)
        {
            throw new IllegalArgumentException("cliSection=" + cliSection + " newData=" + newData);   //NOI18N
        }

        LOGGER.finer("changeSection() -- cliSection=" + cliSection + " newData=" + newData);   //NOI18N

        final Section oldData = cliSection.getData();
        if (oldData.equals(newData))
        {
            return;
        }

        // Check change is not vetoed
        var event = new ItemChangedEvent(ChordLeadSheetImpl.this, cliSection, oldData, newData);
        authorizeChangeEvent(event);                // Possible exception here

        // Change state
        final WritableItem<Section> wSection = (WritableItem<Section>) cliSection;
        wSection.setData(newData);

        // Possibly adjust items position if we have a new time signature with less natural beats 
        // (and generate undoable SimpleEdits accordingly)        
        if (!oldData.getTimeSignature().equals(newData.getTimeSignature()))
        {
            adjustItemsToTimeSignature(oldData.getTimeSignature(), newData.getTimeSignature(), getItems(cliSection, ChordLeadSheetItem.class));
        }

        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("Change Section " + cliSection)
        {
            @Override
            public void undoBody()
            {
                LOGGER.finer("changeSection.undoBody() cliSection=" + cliSection + " oldData=" + oldData + " newData=" + newData);   //NOI18N
                wSection.setData(oldData);
                fireAuthorizedChangeEvent(new ItemChangedEvent(ChordLeadSheetImpl.this, wSection, newData, oldData));
            }

            @Override
            public void redoBody()
            {
                LOGGER.finer("changeSection.redoBody() cliSection=" + cliSection + " oldData=" + oldData + " newData=" + newData);   //NOI18N
                wSection.setData(newData);
                fireAuthorizedChangeEvent(new ItemChangedEvent(ChordLeadSheetImpl.this, wSection, oldData, newData));
            }
        };

        fireUndoableEditHappened(edit);

        // Notify listeners
        fireAuthorizedChangeEvent(event);
    }

    private void removeInitialSection() throws UnsupportedEditException
    {
        CLI_Section cliSection = getSection(0);
        LOGGER.fine("removeInitialSection() -- initSection=" + cliSection);   //NOI18N

        // Check change is not vetoed
        var event = new ItemRemovedEvent(ChordLeadSheetImpl.this, cliSection);
        authorizeChangeEvent(event);            // Possible exception here! 

        // Change state: remove the item and adjust if required trailing items
        items.remove(0);

        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("Remove initial section " + cliSection)
        {
            @Override
            public void undoBody()
            {
                LOGGER.finer("removeInitialSection.undoBody() cliSection=" + cliSection);   //NOI18N
                items.add(0, cliSection);
                fireAuthorizedChangeEvent(new ItemAddedEvent(ChordLeadSheetImpl.this, cliSection));
            }

            @Override
            public void redoBody()
            {
                LOGGER.finer("removeInitialSection.redoBody() cliSection=" + cliSection);   //NOI18N
                items.remove(0);
                fireAuthorizedChangeEvent(new ItemRemovedEvent(ChordLeadSheetImpl.this, cliSection));
            }
        };

        fireUndoableEditHappened(edit);

        // Fire ItemChange event
        fireAuthorizedChangeEvent(event);
    }

    /**
     * Make sure change is authorized by all listeners.
     *
     * @param event
     * @throws UnsupportedEditException
     */
    protected void authorizeChangeEvent(ClsChangeEvent event) throws UnsupportedEditException
    {
        if (event == null)
        {
            throw new IllegalArgumentException("event=" + event);   //NOI18N
        }
        var ls = listeners.toArray(new ClsChangeListener[0]);
        for (ClsChangeListener l : ls)
        {
            l.authorizeChange(event);   // Possible exception here
        }
    }

    /**
     * Fire an authorized change event to all listeners.
     *
     * @param event
     */
    protected void fireAuthorizedChangeEvent(ClsChangeEvent event)
    {
        if (event == null)
        {
            throw new IllegalArgumentException("event=" + event);   //NOI18N
        }
        for (ClsChangeListener l : listeners.toArray(new ClsChangeListener[0]))
        {
            l.chordLeadSheetChanged(event);
        }
    }

    private void fireUndoableEditHappened(UndoableEdit edit)
    {
        if (edit == null)
        {
            throw new IllegalArgumentException("edit=" + edit);   //NOI18N
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
                throw new IllegalStateException("spItems=" + spItems);   //NOI18N
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
                        throw new InvalidObjectException(ex.getMessage());
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

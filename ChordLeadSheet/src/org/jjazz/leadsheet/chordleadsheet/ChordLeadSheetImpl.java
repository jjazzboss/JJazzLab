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

import com.google.common.base.Preconditions;
import org.jjazz.undomanager.api.SimpleEdit;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
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
    private final ItemArray items = new ItemArray();
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
    public void setSizeInBars(final int newSize) throws UnsupportedEditException
    {
        setSize(newSize, true);
    }


    @Override
    public synchronized int getSizeInBars()
    {
        return size;
    }

    @Override
    public final <T> void addItem(ChordLeadSheetItem<T> item)
    {
        addItem(item, true);
    }


    @Override
    public void addSection(final CLI_Section cliSection) throws UnsupportedEditException
    {
        addSection(cliSection, true);
    }


    @Override
    public void removeSection(final CLI_Section cliSection) throws UnsupportedEditException
    {
        removeSection(cliSection, true);
    }


    @Override
    public void moveSection(final CLI_Section cliSection, final int newBarIndex) throws UnsupportedEditException
    {
        moveSection(cliSection, newBarIndex, true);
    }


    @Override
    public <T> void removeItem(final ChordLeadSheetItem<T> item)
    {
        removeItem(item, true);
    }


    @Override
    public <T> void moveItem(ChordLeadSheetItem<T> item, Position newPos)
    {
        moveItem(item, newPos, true);
    }


    @Override
    public <T> void changeItem(ChordLeadSheetItem<T> item, final T newData)
    {
        changeItem(item, newData, true);
    }


    @Override
    public void insertBars(final int barIndex, final int nbBars)
    {
        insertBars(barIndex, nbBars, true);
    }


    @Override
    public void deleteBars(int barIndexFrom, int barIndexTo) throws UnsupportedEditException
    {
        deleteBars(barIndexFrom, barIndexTo, true);
    }


    @Override
    public void setSectionName(CLI_Section cliSection, String name)
    {
        setSectionName(cliSection, name, true);
    }


    @Override
    public void setSectionTimeSignature(CLI_Section cliSection, TimeSignature ts) throws UnsupportedEditException
    {
        setSectionTimeSignature(cliSection, ts, true);
    }


    @Override
    public void cleanup()
    {
        // Nothing
    }

    @Override
    public synchronized boolean contains(ChordLeadSheetItem<?> item)
    {
        return items.contains(item);
    }

    @Override
    public synchronized List<ChordLeadSheetItem<?>> getItems()
    {
        return new ArrayList<>(items);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<? extends T> getItems(Class<T> aClass)
    {
        List<T> result = new ArrayList<>();
        for (ChordLeadSheetItem<?> item : getItems())
        {
            if (aClass == null || aClass.isAssignableFrom(item.getClass()))
            {
                result.add((T) item);
            }
        }
        return result;
    }

    @Override
    public <T> ChordLeadSheetItem<T> getLastItem(Position pos, Class<T> aClass)
    {
        Preconditions.checkNotNull(pos);
        Preconditions.checkArgument(pos.getBar() < getSizeInBars());

        ChordLeadSheetItem<T> res = null;
        var iitems = getItems();        // Synchronized
        for (int i = iitems.size() - 1; i >= 0; i--)
        {
            var item = iitems.get(i);
            var itemPos = item.getPosition();

            if (itemPos.compareTo(pos) > 0 || (aClass != null && !aClass.isAssignableFrom(item.getClass())))
            {
                continue;
            }
            res = (ChordLeadSheetItem<T>) item;
            break;
        }

        return res;
    }

    @Override
    public <T> T getLastItem(int barFrom, int barTo, Class<T> aClass)
    {
        if (barFrom < 0 || barTo < barFrom || barTo >= getSizeInBars())
        {
            throw new IllegalArgumentException(
                    "barFrom=" + barFrom + " barTo=" + barTo + " aClass=" + aClass);
        }
        T res = null;
        var iitems = getItems();        // Synchronized        
        for (int i = iitems.size() - 1; i >= 0; i--)
        {
            var item = iitems.get(i);
            int barIndex = item.getPosition().getBar();


            if (barIndex > barTo || (aClass != null && !aClass.isAssignableFrom(item.getClass())))
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
    public <T> ChordLeadSheetItem<T> getNextItem(ChordLeadSheetItem<T> item)
    {
        ChordLeadSheetItem<T> res = null;
        boolean takeNext = false;
        boolean itemFound = false;

        var iitems = getItems();        // Synchronized        
        for (int i = 0; i < iitems.size(); i++)
        {
            var it = iitems.get(i);

            if (takeNext && item.getClass().isAssignableFrom(it.getClass()))
            {
                res = (ChordLeadSheetItem<T>) it;
                break;
            }

            if (it == item)
            {
                takeNext = true;
                itemFound = true;
            }

        }

        if (!itemFound)
        {
            throw new IllegalArgumentException("Item not found: " + item);
        }

        return res;
    }

    @Override
    public <T> ChordLeadSheetItem<T> getPreviousItem(ChordLeadSheetItem<T> item)
    {
        ChordLeadSheetItem<T> res = null;
        boolean takeNext = false;
        boolean itemFound = false;

        var iitems = getItems();        // Synchronized        
        for (int i = iitems.size() - 1; i >= 0; i--)
        {
            var it = iitems.get(i);

            if (takeNext && item.getClass().isAssignableFrom(it.getClass()))
            {
                res = (ChordLeadSheetItem<T>) it;
                break;
            }

            if (it == item)
            {
                takeNext = true;
                itemFound = true;
            }

        }

        if (!itemFound)
        {
            throw new IllegalArgumentException("Item not found: " + item);
        }

        return res;
    }


    @Override
    @SuppressWarnings("unchecked")
    public <T> List<? extends T> getItems(int barFrom, int barTo, Class<T> aClass)
    {
        if (barFrom < 0 || barTo < barFrom || barTo >= getSizeInBars())
        {
            throw new IllegalArgumentException(
                    "barFrom=" + barFrom + " barTo=" + barTo + " aClass=" + aClass);
        }

        List<T> result = new ArrayList<>();
        for (ChordLeadSheetItem<?> item : getItems())           // Synchronized
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
            throw new IllegalArgumentException("cliSection=" + cliSection + " aClass=" + aClass);
        }
        List<T> result = new ArrayList<>();
        var iitems = getItems();        // Synchronized
        int index = iitems.indexOf(cliSection);
        if (index == -1)
        {
            throw new IllegalArgumentException("cliSection=" + cliSection + " aClass=" + aClass + " iitems=" + iitems);
        }
        index++;
        while (index < iitems.size())
        {
            ChordLeadSheetItem<?> item = iitems.get(index);
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
        if (barIndex < 0 || barIndex >= getSizeInBars())
        {
            throw new IllegalArgumentException("barIndex=" + barIndex);
        }
        var iitems = getItems();        // Synchronized
        CLI_Section lastSection = (CLI_Section) iitems.get(0);       // The initial block
        for (int i = 1; i < iitems.size(); i++)
        {
            ChordLeadSheetItem<?> item = iitems.get(i);
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
        for (ChordLeadSheetItem<?> item : getItems())       // Synchronized
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
    public IntRange getBarRange(CLI_Section cliSection)
    {
        if (cliSection == null)
        {
            throw new NullPointerException("cliSection=" + cliSection);
        }
        var iitems = getItems();        // Synchronized        
        int startBar = cliSection.getPosition().getBar();
        int index = iitems.indexOf(cliSection);
        if (index == -1)
        {
            throw new IllegalArgumentException("cliSection=" + cliSection);
        }

        index++;
        while (index < iitems.size())
        {
            ChordLeadSheetItem<?> item = iitems.get(index);
            if (item instanceof CLI_Section)
            {
                return new IntRange(startBar, item.getPosition().getBar() - 1);
            }
            index++;
        }
        return new IntRange(startBar, getSizeInBars() - 1);
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
        return "ChordLeadSheet section0=" + getSection(0).getData().getName() + " size=" + getSizeInBars();
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

    private void setSize(final int newSize, boolean enableActionEvent) throws UnsupportedEditException
    {
        if (newSize < 1 || newSize > MAX_SIZE)
        {
            throw new IllegalArgumentException("newSize=" + newSize);
        }

        LOGGER.fine("setSize() -- newSize=" + newSize);


        final int oldSize = size;
        int delta = newSize - oldSize;
        List<ChordLeadSheetItem<?>> removedItems = new ArrayList<>();
        if (delta == 0)
        {
            return;
        }

        fireActionEvent(enableActionEvent, "setSize", false);


        UnsupportedEditException ueException = null;
        synchronized (this)
        {
            if (delta > 0)
            {
                // Nothing to do            

            } else if (delta < 0)
            {
                // For undo to work we need to remove possible extra items before setting the size
                int newLastBarIndex = getSizeInBars() - 1 + delta;

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
                    try
                    {
                        removeSectionsAndItems(removedItems);       // Possible exception here
                    } catch (UnsupportedEditException ex)
                    {
                        ueException = ex;          // Save to throw the exception outside the synchronized block
                    }
                }
            }
        }
        if (ueException != null)
        {
            fireActionEvent(enableActionEvent, "setSize", true);       // We need to complete the action
            throw ueException;

        }


        // Then update size
        // Check that change is not vetoed
        var event = new SizeChangedEvent(ChordLeadSheetImpl.this, oldSize, newSize);
        try
        {
            authorizeChangeEvent(event);        // Possible exception here !
        } catch (UnsupportedEditException ex)
        {
            fireActionEvent(enableActionEvent, "setSize", true);       // We need to complete the action
            throw ex;
        }


        // Update state
        synchronized (this)
        {
            size = newSize;
        }


        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("Set Size " + newSize)
        {
            @Override
            public void undoBody()
            {
                LOGGER.finer("setSize.undoBody() newSize=" + newSize);
                synchronized (ChordLeadSheetImpl.this)
                {
                    size = oldSize;
                }
                fireAuthorizedChangeEvent(new SizeChangedEvent(ChordLeadSheetImpl.this, newSize, oldSize));
            }

            @Override
            public void redoBody()
            {
                LOGGER.finer("setSize.redoBody() newSize=" + newSize);
                synchronized (ChordLeadSheetImpl.this)
                {
                    size = newSize;
                }
                fireAuthorizedChangeEvent(new SizeChangedEvent(ChordLeadSheetImpl.this, oldSize, newSize));
            }
        };

        fireUndoableEditHappened(edit);

        fireAuthorizedChangeEvent(event);

        fireActionEvent(enableActionEvent, "setSize", true);
    }

    private <T> void addItem(ChordLeadSheetItem<T> item, boolean enableActionEvent)
    {
        if (item == null || (item instanceof CLI_Section) || !(item instanceof WritableItem))
        {
            throw new IllegalArgumentException("item=" + item);
        }

        LOGGER.fine("addItem() -- item=" + item);

        final WritableItem<?> wItem = (WritableItem<?>) item;
        int barIndex = wItem.getPosition().getBar();
        if (barIndex >= getSizeInBars())
        {
            throw new IllegalArgumentException("item=" + item + " size=" + getSizeInBars());
        }

        fireActionEvent(enableActionEvent, "addItem", false);


        final Position oldPos;
        final ChordLeadSheet oldContainer;
        final ChordLeadSheet newContainer;
        final int index;
        final Position newAdjustedPos;
        synchronized (this)
        {
            // Set the container
            oldContainer = wItem.getContainer();
            newContainer = this;
            wItem.setContainer(newContainer);

            // Adjust position if required
            oldPos = wItem.getPosition();
            newAdjustedPos = oldPos.limitToTimeSignature(getSection(barIndex).getData().
                    getTimeSignature());
            wItem.setPosition(newAdjustedPos);

            // Add item
            index = items.insertOrdered(wItem);
        }

        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("Add " + wItem)
        {
            @Override
            public void undoBody()
            {
                LOGGER.finer("addItem.undoBody() item=" + item);
                synchronized (ChordLeadSheetImpl.this)
                {
                    wItem.setPosition(oldPos);
                    wItem.setContainer(oldContainer);
                    items.remove(wItem);
                }
                fireAuthorizedChangeEvent(new ItemRemovedEvent(ChordLeadSheetImpl.this, wItem));
            }

            @Override
            public void redoBody()
            {
                LOGGER.finer("addItem.redoBody() item=" + item);
                synchronized (ChordLeadSheetImpl.this)
                {
                    wItem.setPosition(newAdjustedPos);
                    wItem.setContainer(newContainer);
                    items.add(index, wItem);
                }
                fireAuthorizedChangeEvent(new ItemAddedEvent(ChordLeadSheetImpl.this, wItem));
            }
        };

        fireUndoableEditHappened(edit);

        // Fire ItemChange event
        fireAuthorizedChangeEvent(new ItemAddedEvent(ChordLeadSheetImpl.this, wItem));

        fireActionEvent(enableActionEvent, "addItem", true);
    }

    /**
     *
     * @param doFire   If false do nothing.
     * @param actionId
     * @param complete
     */
    private void fireActionEvent(boolean doFire, String actionId, boolean complete)
    {
        if (!doFire)
        {
            return;
        }

        // Create an undoable event for this event which does nothing but refiring the ClsActionEvent
        UndoableEdit edit = new SimpleEdit("ClsActionEventEdit(" + actionId + ")")
        {
            @Override
            public void undoBody()
            {
                fireAuthorizedChangeEvent(new ClsActionEvent(ChordLeadSheetImpl.this, actionId, !complete, true));
            }

            @Override
            public void redoBody()
            {
                fireAuthorizedChangeEvent(new ClsActionEvent(ChordLeadSheetImpl.this, actionId, complete, false));
            }
        };
        fireUndoableEditHappened(edit);


        fireAuthorizedChangeEvent(new ClsActionEvent(this, actionId, complete, false));

    }

    private void addSection(final CLI_Section cliSection, boolean enableActionEvent) throws UnsupportedEditException
    {
        if (cliSection == null || cliSection.getPosition().getBar() >= getSizeInBars() || getSection(cliSection.getData().getName()) != null
                || !(cliSection instanceof WritableItem))
        {
            throw new IllegalArgumentException("cliSection=" + cliSection
                    + ", getSize()=" + getSizeInBars()
                    + (cliSection != null ? ", getSection(cliSection.getData().getName())=" + getSection(cliSection.getData().getName())
                            : ""));
        }

        LOGGER.log(Level.FINE, "addSection() -- cliSection={0}", cliSection);

        // Check that change is not vetoed
        var event = new ItemAddedEvent(ChordLeadSheetImpl.this, cliSection);
        authorizeChangeEvent(event);        // Possible exception here !        
        fireActionEvent(enableActionEvent, "addSection", false);

        // Prepare data
        final int barIndex = cliSection.getPosition().getBar();
        final WritableItem<Section> wSection = (WritableItem<Section>) cliSection;
        final ChordLeadSheet oldContainer = wSection.getContainer();
        final ChordLeadSheet newContainer = this;
        synchronized (this)
        {
            wSection.setContainer(this);
        }
        final CLI_Section prevSection = getSection(barIndex);
        if (prevSection.getPosition().getBar() == barIndex)
        {
            throw new IllegalArgumentException("cliSection=" + cliSection + " prevSection=" + prevSection);
        }

        synchronized (this)
        {
            items.insertOrdered(wSection);


            // Adjust position of trailing items if required
            // For undo to work properly, must be done before firing the "Add section" UndoableEdit.        
            adjustItemsToTimeSignature(prevSection.getData().getTimeSignature(), cliSection.getData().getTimeSignature(), getItems(
                    cliSection,
                    ChordLeadSheetItem.class));
        }


        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("Add section " + wSection)
        {
            @Override
            public void undoBody()
            {
                LOGGER.log(Level.FINER, "addSection.undoBody() cliSection={0}", cliSection);
                synchronized (ChordLeadSheetImpl.this)
                {
                    wSection.setContainer(oldContainer);
                    items.remove(wSection);
                }
                fireAuthorizedChangeEvent(new ItemRemovedEvent(ChordLeadSheetImpl.this, wSection));
            }

            @Override
            public void redoBody()
            {
                LOGGER.log(Level.FINER, "addSection.redoBody() cliSection={0}", cliSection);
                synchronized (ChordLeadSheetImpl.this)
                {
                    wSection.setContainer(newContainer);
                    items.insertOrdered(wSection);
                }
                fireAuthorizedChangeEvent(new ItemAddedEvent(ChordLeadSheetImpl.this, wSection));
            }
        };

        // Must be fired BEFORE the vetoable change
        fireUndoableEditHappened(edit);

        fireAuthorizedChangeEvent(event);

        fireActionEvent(enableActionEvent, "addSection", true);
    }

    private void removeSection(final CLI_Section cliSection, boolean enableActionEvent) throws UnsupportedEditException
    {
        if (cliSection == null)
        {
            throw new IllegalArgumentException("cliSection=" + cliSection);
        }

        LOGGER.fine("removeSection() -- cliSection=" + cliSection);

        final int barIndex = cliSection.getPosition().getBar();
        final int index = items.indexOf(cliSection);
        if (barIndex == 0 || index == -1)
        {
            throw new IllegalArgumentException("cliSection=" + cliSection);
        }

        // Check change is not vetoed
        var event = new ItemRemovedEvent(ChordLeadSheetImpl.this, cliSection);
        authorizeChangeEvent(event);            // Possible exception here! 
        fireActionEvent(enableActionEvent, "removeSection", false);

        // Prepare data
        var prevSection = getSection(barIndex - 1);
        var removedSectionItems = getItems(cliSection, ChordLeadSheetItem.class);

        // Change state: remove the item and adjust trailing items if required 
        synchronized (this)
        {
            items.remove(index);


            // Adjust removed section items position if required. 
            // For undo to work properly, must be done before firing 
            // the "Remove section" UndoableEdit.
            adjustItemsToTimeSignature(cliSection.getData().getTimeSignature(), prevSection.getData().getTimeSignature(),
                    removedSectionItems);
        }


        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("Remove section " + cliSection)
        {
            @Override
            public void undoBody()
            {
                LOGGER.finer("removeSection.undoBody() cliSection=" + cliSection);
                synchronized (ChordLeadSheetImpl.this)
                {
                    items.add(index, cliSection);
                }
                fireAuthorizedChangeEvent(new ItemAddedEvent(ChordLeadSheetImpl.this, cliSection));
            }

            @Override
            public void redoBody()
            {
                LOGGER.finer("removeSection.redoBody() cliSection=" + cliSection);
                synchronized (ChordLeadSheetImpl.this)
                {
                    items.remove(index);
                }
                fireAuthorizedChangeEvent(new ItemRemovedEvent(ChordLeadSheetImpl.this, cliSection));
            }
        };

        fireUndoableEditHappened(edit);

        // Fire ItemChange event
        fireAuthorizedChangeEvent(event);


        fireActionEvent(enableActionEvent, "removeSection", true);
    }

    private synchronized void moveSection(final CLI_Section cliSection, final int newBarIndex, boolean enableActionEvent) throws UnsupportedEditException
    {
        if (cliSection == null || newBarIndex <= 0 || newBarIndex >= getSizeInBars() || !(cliSection instanceof WritableItem))
        {
            throw new IllegalArgumentException("cliSection=" + cliSection + " newBarIndex=" + newBarIndex);
        }

        LOGGER.fine("moveSection() -- cliSection=" + cliSection + " newBarIndex=" + newBarIndex);

        final int oldBarIndex = cliSection.getPosition().getBar();
        if (newBarIndex == oldBarIndex)
        {
            return;
        }

        CLI_Section newPosPrevSection = getSection(newBarIndex);
        if (oldBarIndex == 0 || newPosPrevSection.getPosition().getBar() == newBarIndex)
        {
            // Tried to move initial section, or there is already a section at destination
            throw new IllegalArgumentException("section=" + cliSection + " newBarIndex=" + newBarIndex);
        }

        if (items.indexOf(cliSection) == -1)
        {
            throw new IllegalArgumentException("section=" + cliSection);
        }

        // Check change is not vetoed
        var event = new SectionMovedEvent(ChordLeadSheetImpl.this, cliSection, oldBarIndex, newBarIndex);
        authorizeChangeEvent(event);            // Possible exception here! 
        fireActionEvent(enableActionEvent, "moveSection", false);

        // OK move is safe, change is safe
        final WritableItem<Section> wSection = (WritableItem<Section>) cliSection;
        synchronized (this)
        {
            wSection.setPosition(new Position(newBarIndex, 0));
            items.remove(wSection);
            items.insertOrdered(wSection);

            // Section items adjusting must be done BEFORE firing the moved section undoable event
            // Adjust section at new position
            adjustItemsToTimeSignature(newPosPrevSection.getData().getTimeSignature(), cliSection.getData().getTimeSignature(), getItems(
                    cliSection, ChordLeadSheetItem.class));

            // Section before old position might need to be checked too
            CLI_Section oldPosPrevSection = getSection(oldBarIndex - 1);
            var oldItems = getItems(oldBarIndex, getBarRange(oldPosPrevSection).to, ChordLeadSheetItem.class);
            adjustItemsToTimeSignature(cliSection.getData().getTimeSignature(), oldPosPrevSection.getData().getTimeSignature(), oldItems);
        }

        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("Move " + wSection)
        {
            @Override
            public void undoBody()
            {
                LOGGER.finer("moveSection.undoBody() cliSection=" + cliSection + " newBarIndex=" + newBarIndex);
                synchronized (ChordLeadSheetImpl.this)
                {
                    wSection.setPosition(new Position(oldBarIndex, 0));
                    items.remove(wSection);
                    items.insertOrdered(wSection);
                }
                fireAuthorizedChangeEvent(new SectionMovedEvent(ChordLeadSheetImpl.this, cliSection, newBarIndex, oldBarIndex));
            }

            @Override
            public synchronized void redoBody()
            {
                LOGGER.finer("moveSection.redoBody() cliSection=" + cliSection + " newBarIndex=" + newBarIndex);
                synchronized (ChordLeadSheetImpl.this)
                {
                    wSection.setPosition(new Position(newBarIndex, 0));
                    items.remove(wSection);
                    items.insertOrdered(wSection);
                }
                fireAuthorizedChangeEvent(new SectionMovedEvent(ChordLeadSheetImpl.this, cliSection, oldBarIndex, newBarIndex));
            }
        };

        fireUndoableEditHappened(edit);

        // Fire ItemChange event
        fireAuthorizedChangeEvent(event);

        fireActionEvent(enableActionEvent, "moveSection", true);
    }

    private <T> void removeItem(final ChordLeadSheetItem<T> item, boolean enableActionEvent)
    {
        if (item == null || (item instanceof CLI_Section))
        {
            throw new IllegalArgumentException("item=" + item);
        }

        LOGGER.fine("removeItem() -- item=" + item);

        final int index = items.indexOf(item);
        if (index == -1)
        {
            throw new IllegalArgumentException("item=" + item);
        }

        fireActionEvent(enableActionEvent, "removeItem", false);

        // Change state
        synchronized (this)
        {
            items.remove(index);
        }

        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("Remove " + item)
        {
            @Override
            public void undoBody()
            {
                LOGGER.finer("removeItem.undoBody() item=" + item);
                synchronized (ChordLeadSheetImpl.this)
                {
                    items.insertOrdered(item);
                }
                fireAuthorizedChangeEvent(new ItemAddedEvent(ChordLeadSheetImpl.this, item));
            }

            @Override
            public void redoBody()
            {
                LOGGER.finer("removeItem.redoBody() item=" + item);
                synchronized (ChordLeadSheetImpl.this)
                {
                    items.remove(item);
                }
                fireAuthorizedChangeEvent(new ItemRemovedEvent(ChordLeadSheetImpl.this, item));
            }
        };

        fireUndoableEditHappened(edit);

        // Fire ItemChange event
        fireAuthorizedChangeEvent(new ItemRemovedEvent(ChordLeadSheetImpl.this, item));


        fireActionEvent(enableActionEvent, "removeItem", true);
    }

    private <T> void moveItem(ChordLeadSheetItem<T> item, Position newPos, boolean enableActionEvent)
    {
        if (item == null || newPos == null || !(item instanceof WritableItem) || (item instanceof CLI_Section))
        {
            throw new IllegalArgumentException("item=" + item + " newPos=" + newPos);
        }

        LOGGER.fine("moveItem() -- item=" + item + " newPos=" + newPos);

        final WritableItem<T> wItem = (WritableItem<T>) item;
        final Position oldPos = wItem.getPosition();
        final Position newAdjustedPos = newPos.limitToTimeSignature(getSection(newPos.getBar()).getData().getTimeSignature());
        if (oldPos.equals(newAdjustedPos))
        {
            return;
        }

        fireActionEvent(enableActionEvent, "moveItem", false);

        synchronized (this)
        {
            // Change the position 
            if (!items.remove(wItem))
            {
                throw new IllegalArgumentException("oldPosition=" + oldPos + " wItem=" + wItem + " items=" + items);
            }
            wItem.setPosition(newAdjustedPos);
            items.insertOrdered(wItem);
        }

        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("Move " + wItem)
        {
            @Override
            public void undoBody()
            {
                LOGGER.log(Level.FINER, "moveItem.undoBody() item={0} oldPos={1} newAdjustedPos={2}", new Object[]
                {
                    item, oldPos,
                    newAdjustedPos
                });
                synchronized (ChordLeadSheetImpl.this)
                {
                    items.remove(wItem);
                    wItem.setPosition(oldPos);
                    items.insertOrdered(wItem);
                }
                fireAuthorizedChangeEvent(new ItemMovedEvent(ChordLeadSheetImpl.this, wItem, newAdjustedPos, oldPos));
            }

            @Override
            public void redoBody()
            {
                LOGGER.log(Level.FINER, "moveItem.redoBody() item={0} oldPos={1} newAdjustedPos={2}", new Object[]
                {
                    item, oldPos,
                    newAdjustedPos
                });
                synchronized (ChordLeadSheetImpl.this)
                {
                    items.remove(wItem);
                    wItem.setPosition(newAdjustedPos);
                    items.insertOrdered(wItem);
                }
                fireAuthorizedChangeEvent(new ItemMovedEvent(ChordLeadSheetImpl.this, wItem, oldPos, newAdjustedPos));
            }
        };
        fireUndoableEditHappened(edit);

        // Fire ItemChange event
        fireAuthorizedChangeEvent(new ItemMovedEvent(ChordLeadSheetImpl.this, wItem, oldPos, newAdjustedPos));


        fireActionEvent(enableActionEvent, "moveItem", true);
    }

    private <T> void changeItem(ChordLeadSheetItem<T> item, final T newData, boolean enableActionEvent)
    {
        if (item == null || items.indexOf(item) == -1 || !(item instanceof WritableItem) || (item instanceof CLI_Section))
        {
            throw new IllegalArgumentException("item=" + item + " newData=" + newData + " items=" + items);
        }

        LOGGER.fine("changeItem() -- item=" + item + " newData=" + newData);

        final T oldData = item.getData();
        if (oldData.equals(newData))
        {
            return;
        }

        fireActionEvent(enableActionEvent, "changeItem", false);

        // Change state

        final WritableItem<T> wItem = (WritableItem<T>) item;
        synchronized (this)
        {
            wItem.setData(newData);
        }

        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("Change " + item)
        {
            @Override
            public void undoBody()
            {
                LOGGER.log(Level.FINER, "changeItem.undoBody() item={0} oldData={1} newData={2}", new Object[]
                {
                    item, oldData, newData
                });
                synchronized (ChordLeadSheetImpl.this)
                {
                    wItem.setData(oldData);
                }
                fireAuthorizedChangeEvent(new ItemChangedEvent(ChordLeadSheetImpl.this, wItem, newData, oldData));
            }

            @Override
            public void redoBody()
            {
                LOGGER.log(Level.FINER, "changeItem.undoBody() item={0} oldData={1} newData={2}", new Object[]
                {
                    item, oldData, newData
                });
                synchronized (ChordLeadSheetImpl.this)
                {
                    wItem.setData(newData);
                }
                fireAuthorizedChangeEvent(new ItemChangedEvent(ChordLeadSheetImpl.this, wItem, oldData, newData));
            }
        };
        fireUndoableEditHappened(edit);

        // Fire ItemChange event
        fireAuthorizedChangeEvent(new ItemChangedEvent(ChordLeadSheetImpl.this, item, oldData, newData));


        fireActionEvent(enableActionEvent, "changeItem", true);
    }

    private void insertBars(final int barIndex, final int nbBars, boolean enableActionEvent)
    {
        if (barIndex < 0 || barIndex > getSizeInBars() || nbBars <= 0)
        {
            throw new IllegalArgumentException("barIndex=" + barIndex + " nbBars=" + nbBars);
        }

        LOGGER.log(Level.FINE, "insertBars() -- barIndex={0} nbBars={1}", new Object[]
        {
            barIndex, nbBars
        });


        fireActionEvent(enableActionEvent, "insertBars", false);


        // First set the size larger
        final int newSize = getSizeInBars() + nbBars;
        try
        {
            setSize(newSize, false);
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

        if (indexFrom > 0)
        {
            shiftItemsPosition(items.getSubList(indexFrom, items.size() - 1), nbBars);
        } else
        {
            // Special case: the initial section will be moved too, create a new initial section            
            synchronized (this)
            {
                shiftItemsPosition(items.getSubList(indexFrom, items.size() - 1), nbBars);

                Section oldInitSection = ((CLI_Section) items.get(0)).getData();
                CLI_Factory clif = CLI_Factory.getDefault();
                CLI_Section newInitSection = clif.createSection(this, "_" + oldInitSection.getName(), oldInitSection.getTimeSignature(),
                        0);
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

        fireActionEvent(enableActionEvent, "insertBars", true);
    }

    private void deleteBars(int barIndexFrom, int barIndexTo, boolean enableActionEvent) throws UnsupportedEditException
    {
        if (barIndexFrom < 0 || barIndexTo < barIndexFrom || barIndexTo >= getSizeInBars()
                || (barIndexTo - barIndexFrom + 1) >= getSizeInBars())
        {
            throw new IllegalArgumentException(
                    "barIndexFrom=" + barIndexFrom + " barIndexTo=" + barIndexTo);
        }

        LOGGER.log(Level.FINE, "deleteBars() -- barIndexFrom={0} barIndexTo={1}", new Object[]
        {
            barIndexFrom, barIndexTo
        });


        fireActionEvent(enableActionEvent, "deleteBars", false);


        // Save data after the bar deletions
        CLI_Section afterDeletionSection = (barIndexTo + 1 > size - 1) ? null : getSection(barIndexTo + 1);
        @SuppressWarnings("rawtypes")
        List<? extends ChordLeadSheetItem> afterDeletionItems = null;
        if (afterDeletionSection != null)
        {
            afterDeletionItems = getItems(barIndexTo + 1, getBarRange(afterDeletionSection).to, ChordLeadSheetItem.class);
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
            try
            {
                removeSectionsAndItems(removedItems);       // Possible exception here
            } catch (UnsupportedEditException ex)
            {
                fireActionEvent(enableActionEvent, "deleteBars", true);       // We need to complete the action
                throw ex;
            }
        }

        int range = barIndexTo - barIndexFrom + 1;
        synchronized (this)
        {
            // Handle special case if barIndexFrom == 0 and there is a section right after the deleted bars
            if (barIndexFrom == 0 && afterDeletionSection != null && afterDeletionSection.getPosition().getBar() == barIndexTo + 1)
            {
                // Remove the initial section (and fire undoableEvent)
                try
                {
                    removeInitialSection();     // Possible exception here
                } catch (UnsupportedEditException ex)
                {
                    fireActionEvent(enableActionEvent, "deleteBars", true);        // Need to complete the ClsActionEvent
                    throw ex;
                }
            }

            // Shift remaining items
            if (!movedItems.isEmpty())
            {
                shiftItemsPosition(movedItems, -range);
            }
        }

        // Adjust the size
        setSize(getSizeInBars() - range, false);         // Possible exception here! But should not happen since we removed items/sections before.


        // Adjust positions of items after the deletion if any
        if (afterDeletionItems != null)
        {
            CLI_Section newSection = getSection(barIndexFrom);
            adjustItemsToTimeSignature(afterDeletionSection.getData().getTimeSignature(), newSection.getData().getTimeSignature(),
                    afterDeletionItems);
        }

        fireActionEvent(enableActionEvent, "deleteBars", true);

    }

    private void setSectionName(CLI_Section cliSection, String name, boolean enableActionEvent)
    {
        if (cliSection == null || items.indexOf(cliSection) == -1 || name == null || (getSection(name) != null && getSection(name) != cliSection)
                || !(cliSection instanceof WritableItem))
        {
            throw new IllegalArgumentException("section=" + cliSection + " name=" + name);
        }

        LOGGER.log(Level.FINE, "setSectionName() -- cliSection={0} name={1}", new Object[]
        {
            cliSection, name
        });

        fireActionEvent(enableActionEvent, "setSectionName", false);

        try
        {
            changeSection(cliSection, new Section(name, cliSection.getData().getTimeSignature()));
        } catch (UnsupportedEditException ex)
        {
            // Should never happen
            Exceptions.printStackTrace(ex);
        }

        fireActionEvent(enableActionEvent, "setSectionName", true);
    }

    private void setSectionTimeSignature(CLI_Section cliSection, TimeSignature ts, boolean enableActionEvent) throws UnsupportedEditException
    {
        if (cliSection == null || ts == null || items.indexOf(cliSection) == -1
                || !(cliSection instanceof WritableItem))
        {
            throw new IllegalArgumentException("section=" + cliSection + " ts=" + ts);
        }

        LOGGER.log(Level.FINE, "setSectionTimeSignature() -- cliSection={0} ts={1}", new Object[]
        {
            cliSection, ts
        });

        fireActionEvent(enableActionEvent, "setSectionTimeSignature", false);

        try
        {
            changeSection(cliSection, new Section(cliSection.getData().getName(), ts));
        } catch (UnsupportedEditException ex)
        {
            fireActionEvent(enableActionEvent, "setSectionTimeSignature", true);           // Need to complete the ClsActionEvent
            throw ex;
        }

        fireActionEvent(enableActionEvent, "setSectionTimeSignature", true);
    }

    /**
     * Remove sections and items.
     * <p>
     * Chord symbols are removed in a single undoable operation. Then sections are removed one by one using removeSection() (possible
     * exception).
     *
     * @param allItems
     * @throws UnsupportedEditException
     */
    private void removeSectionsAndItems(List<ChordLeadSheetItem<?>> allItems) throws UnsupportedEditException
    {
        LOGGER.log(Level.FINER, "removeSectionsAndItems() -- removedItems={0}", allItems);

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

            synchronized (this)
            {
                items.removeAll(removedItems);
            }

            // Create the undoable event        
            UndoableEdit edit = new SimpleEdit("Remove items")
            {
                @Override
                public void undoBody()
                {
                    LOGGER.log(Level.FINER, "removeSectionsAndItems.undoBody() removedItems={0}", removedItems);
                    synchronized (ChordLeadSheetImpl.this)
                    {
                        for (ChordLeadSheetItem<?> item : removedItems)
                        {
                            items.insertOrdered(item);
                        }
                    }
                    fireAuthorizedChangeEvent(new ItemAddedEvent(ChordLeadSheetImpl.this, removedItems));
                }

                @Override
                public void redoBody()
                {
                    LOGGER.log(Level.FINER, "removeSectionsAndItems.redoBody() removedItems={0}", removedItems);
                    synchronized (ChordLeadSheetImpl.this)
                    {
                        items.removeAll(removedItems);
                    }
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
            removeSection(section, false);      // Possible exception here
        }

    }

    /**
     * Adjust the position of the specified items from oldTs to newTs.
     *
     * @param oldTs
     * @param newTs
     * @param iitems
     */
    @SuppressWarnings("rawtypes")
    private void adjustItemsToTimeSignature(TimeSignature oldTs, TimeSignature newTs, List<? extends ChordLeadSheetItem> iitems)
    {
        if (newTs == null || iitems == null || oldTs == null)
        {
            throw new IllegalArgumentException("iitems=" + iitems + " oldTs=" + oldTs + " newTs=" + newTs);
        }

        LOGGER.log(Level.FINER, "adjustItemsToTimeSignature() -- oldTs={0} newTs={1} iitems={2}", new Object[]
        {
            oldTs, newTs, iitems
        });

        if (oldTs.equals(newTs))
        {
            return;
        }

        for (ChordLeadSheetItem<?> item : iitems)
        {
            if (item instanceof CLI_Section)
            {
                continue;
            }
            Position oldPos = item.getPosition();
            Position newPos = oldPos.getConvertedPosition(oldTs, newTs);
            moveItem(item, newPos, false);
        }
    }

    /**
     * Shift the position of a list of items of nbBars.
     *
     * @param shiftedItems
     * @param nbBars       Shift
     */
    private void shiftItemsPosition(final List<ChordLeadSheetItem<?>> shiftedItems, final int nbBars)
    {
        if (shiftedItems == null)
        {
            throw new IllegalArgumentException("shiftedItems=" + shiftedItems + " nbBars=" + nbBars);
        }

        LOGGER.log(Level.FINER, "shiftItemsPosition() -- shiftedItems={0} nbBars={1}", new Object[]
        {
            shiftedItems, nbBars
        });

        if (shiftedItems.isEmpty())
        {
            return;
        }
        final ArrayList<Position> oldPositions = new ArrayList<>();
        final ArrayList<Position> newPositions = new ArrayList<>();
        synchronized (this)
        {
            for (ChordLeadSheetItem<?> item : shiftedItems)
            {
                WritableItem<?> wItem = (WritableItem<?>) item;
                oldPositions.add(wItem.getPosition());
                Position newPos = wItem.getPosition();
                int newBar = newPos.getBar() + nbBars;
                if (newBar < 0 || newBar >= getSizeInBars())
                {
                    throw new IllegalArgumentException("wItem=" + wItem + " nbBars=" + nbBars + " size="
                            + getSizeInBars());
                }
                newPos.setBar(newBar);
                wItem.setPosition(newPos);
                newPositions.add(newPos);
            }
        }

        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("Move items " + nbBars + " bars")
        {
            @Override
            public void undoBody()
            {
                LOGGER.log(Level.FINER, "shiftItemsPosition.undoBody() shiftedItems={0} nbBars={1}", new Object[]
                {
                    shiftedItems, nbBars
                });
                int i = 0;
                synchronized (ChordLeadSheetImpl.this)
                {
                    for (ChordLeadSheetItem<?> item : shiftedItems)
                    {
                        Position oldPos = oldPositions.get(i++);
                        ((WritableItem) item).setPosition(oldPos);
                    }
                }
                fireAuthorizedChangeEvent(new ItemBarShiftedEvent(ChordLeadSheetImpl.this, shiftedItems, -nbBars));
            }

            @Override
            public void redoBody()
            {
                LOGGER.log(Level.FINER, "shiftItemsPosition.redoBody() shiftedItems={0} nbBars={1}", new Object[]
                {
                    shiftedItems, nbBars
                });
                int i = 0;
                synchronized (ChordLeadSheetImpl.this)
                {
                    for (ChordLeadSheetItem<?> item : shiftedItems)
                    {
                        Position newPos = newPositions.get(i++);
                        ((WritableItem) item).setPosition(newPos);
                    }
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
            throw new IllegalArgumentException("cliSection=" + cliSection + " newData=" + newData);
        }

        LOGGER.log(Level.FINER, "changeSection() -- cliSection={0} newData={1}", new Object[]
        {
            cliSection, newData
        });


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
        synchronized (this)
        {
            wSection.setData(newData);

            // Possibly adjust items position if we have a new time signature with less natural beats 
            // (and generate undoable SimpleEdits accordingly)        
            if (!oldData.getTimeSignature().equals(newData.getTimeSignature()))
            {
                adjustItemsToTimeSignature(oldData.getTimeSignature(), newData.getTimeSignature(),
                        getItems(cliSection, ChordLeadSheetItem.class));
            }
        }
        

        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("Change Section " + cliSection)
        {
            @Override
            public void undoBody()
            {
                LOGGER.log(Level.FINER, "changeSection.undoBody() cliSection={0} oldData={1} newData={2}", new Object[]
                {
                    cliSection, oldData,
                    newData
                });
                synchronized (ChordLeadSheetImpl.this)
                {
                    wSection.setData(oldData);
                }
                fireAuthorizedChangeEvent(new ItemChangedEvent(ChordLeadSheetImpl.this, wSection, newData, oldData));
            }

            @Override
            public void redoBody()
            {
                LOGGER.log(Level.FINER, "changeSection.redoBody() cliSection={0} oldData={1} newData={2}", new Object[]
                {
                    cliSection, oldData,
                    newData
                });
                synchronized (ChordLeadSheetImpl.this)
                {
                    wSection.setData(newData);
                }
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
        LOGGER.log(Level.FINE, "removeInitialSection() -- initSection={0}", cliSection);

        // Check change is not vetoed
        var event = new ItemRemovedEvent(ChordLeadSheetImpl.this, cliSection);
        authorizeChangeEvent(event);            // Possible exception here! 

        // Change state: remove the item and adjust if required trailing items
        synchronized (this)
        {
            items.remove(0);
        }

        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("Remove initial section " + cliSection)
        {
            @Override
            public void undoBody()
            {
                LOGGER.finer("removeInitialSection.undoBody() cliSection=" + cliSection);
                synchronized (ChordLeadSheetImpl.this)
                {
                    items.add(0, cliSection);
                }
                fireAuthorizedChangeEvent(new ItemAddedEvent(ChordLeadSheetImpl.this, cliSection));
            }

            @Override
            public void redoBody()
            {
                LOGGER.finer("removeInitialSection.redoBody() cliSection=" + cliSection);
                synchronized (ChordLeadSheetImpl.this)
                {
                    items.remove(0);
                }
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
            throw new IllegalArgumentException("event=" + event);
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
            throw new IllegalArgumentException("event=" + event);
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
            spSize = cls.getSizeInBars();
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
                        cls.addSection((CLI_Section) item, false);
                    } catch (UnsupportedEditException ex)
                    {
                        // Translate to an ObjectStreamException
                        throw new InvalidObjectException(ex.getMessage());
                    }
                } else
                {
                    cls.addItem(item, false);
                }
            }
            return cls;
        }
    }
}

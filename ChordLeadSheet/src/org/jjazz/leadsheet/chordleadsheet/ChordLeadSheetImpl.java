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
import com.google.common.collect.Lists;
import org.jjazz.undomanager.api.SimpleEdit;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.TreeSet;
import java.util.function.Predicate;
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
import org.openide.util.Exceptions;

public class ChordLeadSheetImpl implements ChordLeadSheet, Serializable
{

    /**
     * The main data structure: store the items sorted.
     */
    private final TreeSet<ChordLeadSheetItem> items = new TreeSet<>();
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
        items.add(initSection);
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
    public final void addItem(ChordLeadSheetItem<?> item)
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
    public void removeItem(final ChordLeadSheetItem<?> item)
    {
        removeItem(item, true);
    }


    @Override
    public void moveItem(ChordLeadSheetItem<?> item, Position newPos)
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
    public synchronized <T extends ChordLeadSheetItem<?>> T getFirstItemAfter(Position posFrom, boolean inclusiveFrom, Class<T> itemClass, Predicate<T> tester)
    {
        Preconditions.checkNotNull(posFrom);
        Preconditions.checkNotNull(tester);
        Preconditions.checkNotNull(itemClass);

        T res = null;

        var tailSet = items.tailSet(ChordLeadSheetItem.createItemFrom(posFrom, inclusiveFrom),
                inclusiveFrom);   // useless because of createItemFrom
        for (var item : tailSet)
        {
            if (itemClass.isAssignableFrom(item.getClass()))
            {
                T itemT = (T) item;
                if (tester.test(itemT))
                {
                    res = itemT;
                    break;
                }
            }
        }

        return res;
    }


    @Override
    public synchronized <T extends ChordLeadSheetItem<?>> T getLastItemBefore(Position posTo, boolean inclusiveTo, Class<T> itemClass, Predicate<T> tester)
    {
        Preconditions.checkNotNull(posTo);
        Preconditions.checkNotNull(tester);
        Preconditions.checkNotNull(itemClass);
        T res = null;

        var headSet = items.headSet(ChordLeadSheetItem.createItemTo(posTo, inclusiveTo),
                false);   // useless because of createItemFrom
        var it = headSet.descendingIterator();
        while (it.hasNext())
        {
            var item = it.next();
            if (itemClass.isAssignableFrom(item.getClass()))
            {
                T itemT = (T) item;
                if (tester.test(itemT))
                {
                    res = itemT;
                    break;
                }
            }
        }

        return res;
    }


    @Override
    public synchronized <T extends ChordLeadSheetItem<?>> List<T> getItems(Position posFrom, boolean inclusiveFrom, Position posTo, boolean inclusiveTo,
            Class<T> itemClass,
            Predicate<T> tester)
    {
        Preconditions.checkNotNull(posFrom);
        Preconditions.checkNotNull(posTo);
        Preconditions.checkNotNull(tester);
        Preconditions.checkNotNull(itemClass);

        var rangeItems = items.subSet(ChordLeadSheetItem.createItemFrom(posFrom, inclusiveFrom),
                false, // useless because of createItemFrom
                ChordLeadSheetItem.createItemTo(posTo, inclusiveTo),
                false);   // useless because of createItemFrom

        var res = rangeItems.stream()
                .filter(item -> itemClass.isAssignableFrom(item.getClass()))
                .map(cli -> (T) cli)
                .filter(cli -> tester.test(cli))
                .toList();

        return res;
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

        LOGGER.log(Level.FINE, "setSize() -- newSize={0}", newSize);


        final int oldSize = size;
        int delta = newSize - oldSize;
        if (delta == 0)
        {
            return;
        }

        fireActionEvent(enableActionEvent, "setSize", false, oldSize);


        synchronized (this)
        {
            if (delta > 0)
            {
                // Nothing to do            

            } else if (delta < 0)
            {
                // For undo to work we need to remove possible extra items before setting the size
                var itemFrom = ChordLeadSheetItem.createItemFrom(newSize);
                var itemsToRemove = items.tailSet(itemFrom);
                try
                {
                    removeSectionsAndItems(itemsToRemove);       // Possible exception here
                } catch (UnsupportedEditException ex)
                {
                    // If here synchronized lock is automatically released
                    fireActionEvent(enableActionEvent, "setSize", true, oldSize);       // We need to complete the action
                    throw ex;
                }
            }
        }


        // Check that change is not vetoed
        var event = new SizeChangedEvent(ChordLeadSheetImpl.this, oldSize, newSize);
        try
        {
            authorizeChangeEvent(event);        // Possible exception here !
        } catch (UnsupportedEditException ex)
        {
            fireActionEvent(enableActionEvent, "setSize", true, oldSize);       // We need to complete the action
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
                LOGGER.log(Level.FINER, "setSize.undoBody() newSize={0}", newSize);
                synchronized (ChordLeadSheetImpl.this)
                {
                    size = oldSize;
                }
                fireAuthorizedChangeEvent(new SizeChangedEvent(ChordLeadSheetImpl.this, newSize, oldSize));
            }

            @Override
            public void redoBody()
            {
                LOGGER.log(Level.FINER, "setSize.redoBody() newSize={0}", newSize);
                synchronized (ChordLeadSheetImpl.this)
                {
                    size = newSize;
                }
                fireAuthorizedChangeEvent(new SizeChangedEvent(ChordLeadSheetImpl.this, oldSize, newSize));
            }
        };

        fireUndoableEditHappened(edit);

        fireAuthorizedChangeEvent(event);

        fireActionEvent(enableActionEvent, "setSize", true, oldSize);
    }

    private <T> void addItem(ChordLeadSheetItem<T> item, boolean enableActionEvent)
    {
        if (item == null || (item instanceof CLI_Section) || !(item instanceof WritableItem))
        {
            throw new IllegalArgumentException("item=" + item);
        }

        LOGGER.log(Level.FINE, "addItem() -- item={0}", item);

        final WritableItem<?> wItem = (WritableItem<?>) item;
        int barIndex = wItem.getPosition().getBar();
        if (barIndex >= getSizeInBars())
        {
            throw new IllegalArgumentException("item=" + item + " size=" + getSizeInBars());
        }

        fireActionEvent(enableActionEvent, "addItem", false, item);


        final Position oldPos;
        final ChordLeadSheet oldContainer;
        final ChordLeadSheet newContainer;
        final Position newAdjustedPos;

        // Set the container
        oldContainer = wItem.getContainer();
        newContainer = this;
        wItem.setContainer(newContainer);

        // Adjust position if required
        oldPos = wItem.getPosition();
        newAdjustedPos = oldPos.limitToTimeSignature(getSection(barIndex).getData().getTimeSignature());
        wItem.setPosition(newAdjustedPos);


        // Change state
        addItemChecked(wItem);


        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("Add " + wItem)
        {
            @Override
            public void undoBody()
            {
                LOGGER.log(Level.FINER, "addItem.undoBody() item={0}", item);
                removeItemChecked(wItem);
                wItem.setPosition(oldPos);
                wItem.setContainer(oldContainer);
                fireAuthorizedChangeEvent(new ItemRemovedEvent(ChordLeadSheetImpl.this, wItem));
            }

            @Override
            public void redoBody()
            {
                LOGGER.log(Level.FINER, "addItem.redoBody() item={0}", item);
                wItem.setPosition(newAdjustedPos);
                wItem.setContainer(newContainer);
                addItemChecked(wItem);
                fireAuthorizedChangeEvent(new ItemAddedEvent(ChordLeadSheetImpl.this, wItem));
            }
        };

        fireUndoableEditHappened(edit);

        // Fire ItemChange event
        fireAuthorizedChangeEvent(new ItemAddedEvent(ChordLeadSheetImpl.this, wItem));

        fireActionEvent(enableActionEvent, "addItem", true, item);
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
        fireActionEvent(enableActionEvent, "addSection", false, cliSection);

        // Prepare data
        final int barIndex = cliSection.getPosition().getBar();
        final WritableItem<Section> wSection = (WritableItem<Section>) cliSection;
        final ChordLeadSheet oldContainer = wSection.getContainer();
        final ChordLeadSheet newContainer = this;
        wSection.setContainer(newContainer);
        final CLI_Section oldSection = getSection(barIndex);
        if (oldSection.getPosition().getBar() == barIndex)
        {
            throw new IllegalArgumentException("cliSection=" + cliSection + " prevSection=" + oldSection);
        }
        TimeSignature oldTs = oldSection.getData().getTimeSignature();
        TimeSignature newTs = cliSection.getData().getTimeSignature();


        // Change state
        if (!newTs.equals(oldTs))
        {
            // Adjust position of trailing items if required
            // For undo to work properly, must be done before firing the "Add section" UndoableEdit.      
            // No need to synchronize the call because change is done before the section change
            var oldSectionBarRange = getBarRange(oldSection);
            var iitems = getItems(barIndex, oldSectionBarRange.to, ChordLeadSheetItem.class);
            adjustItemsToTimeSignature(oldTs, newTs, iitems);
        }
        addItemChecked(wSection);


        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("Add section " + wSection)
        {
            @Override
            public void undoBody()
            {
                LOGGER.log(Level.FINER, "addSection.undoBody() cliSection={0}", cliSection);
                removeItemChecked(wSection);
                wSection.setContainer(oldContainer);
                fireAuthorizedChangeEvent(new ItemRemovedEvent(ChordLeadSheetImpl.this, wSection));
            }

            @Override
            public void redoBody()
            {
                LOGGER.log(Level.FINER, "addSection.redoBody() cliSection={0}", cliSection);
                wSection.setContainer(newContainer);
                addItemChecked(wSection);
                fireAuthorizedChangeEvent(new ItemAddedEvent(ChordLeadSheetImpl.this, wSection));
            }
        };

        // Must be fired BEFORE the vetoable change
        fireUndoableEditHappened(edit);

        fireAuthorizedChangeEvent(event);

        fireActionEvent(enableActionEvent, "addSection", true, cliSection);
    }

    private void removeSection(final CLI_Section cliSection, boolean enableActionEvent) throws UnsupportedEditException
    {
        if (cliSection == null || cliSection.getPosition().getBar() == 0)
        {
            throw new IllegalArgumentException("cliSection=" + cliSection);
        }

        LOGGER.log(Level.FINE, "removeSection() -- cliSection={0}", cliSection);

        final int barIndex = cliSection.getPosition().getBar();


        // Check that change is not vetoed
        var event = new ItemRemovedEvent(ChordLeadSheetImpl.this, cliSection);
        authorizeChangeEvent(event);            // Possible exception here! 
        fireActionEvent(enableActionEvent, "removeSection", false, cliSection);

        // Prepare data
        var newSection = getSection(barIndex - 1);
        var newTs = newSection.getData().getTimeSignature();
        var oldTs = cliSection.getData().getTimeSignature();


        // Change state
        if (!newTs.equals(oldTs))
        {
            // Adjust position of trailing items if required
            // For undo to work properly, must be done before firing the "Remove section" UndoableEdit.      
            // No need to synchronize the call because change is done before the section change
            adjustItemsToTimeSignature(oldTs, newTs, getItems(cliSection, ChordLeadSheetItem.class, cli -> true));
        }
        removeItemChecked(cliSection);


        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("Remove section " + cliSection)
        {
            @Override
            public void undoBody()
            {
                LOGGER.log(Level.FINER, "removeSection.undoBody() cliSection={0}", cliSection);
                addItemChecked(cliSection);
                fireAuthorizedChangeEvent(new ItemAddedEvent(ChordLeadSheetImpl.this, cliSection));
            }

            @Override
            public void redoBody()
            {
                LOGGER.log(Level.FINER, "removeSection.redoBody() cliSection={0}", cliSection);
                removeItemChecked(cliSection);
                fireAuthorizedChangeEvent(new ItemRemovedEvent(ChordLeadSheetImpl.this, cliSection));
            }
        };

        fireUndoableEditHappened(edit);

        // Fire ItemChange event
        fireAuthorizedChangeEvent(event);


        fireActionEvent(enableActionEvent, "removeSection", true, cliSection);
    }

    private void moveSection(final CLI_Section cliSection, final int newBarIndex, boolean enableActionEvent) throws UnsupportedEditException
    {
        if (cliSection == null || newBarIndex <= 0 || newBarIndex >= getSizeInBars() || !(cliSection instanceof WritableItem))
        {
            throw new IllegalArgumentException("cliSection=" + cliSection + " newBarIndex=" + newBarIndex);
        }

        LOGGER.log(Level.FINE, "moveSection() -- cliSection={0} newBarIndex={1}", new Object[]
        {
            cliSection, newBarIndex
        });

        final int oldBarIndex = cliSection.getPosition().getBar();
        if (newBarIndex == oldBarIndex)
        {
            return;
        }

        CLI_Section newPosOldSection = getSection(newBarIndex);
        if (oldBarIndex == 0 || newPosOldSection.getPosition().getBar() == newBarIndex)
        {
            // Tried to move initial section, or there is already a section at destination
            throw new IllegalArgumentException("section=" + cliSection + " newBarIndex=" + newBarIndex);
        }


        // Check that change is not vetoed
        var event = new SectionMovedEvent(ChordLeadSheetImpl.this, cliSection, oldBarIndex, newBarIndex);
        authorizeChangeEvent(event);            // Possible exception here! 
        fireActionEvent(enableActionEvent, "moveSection", false, cliSection);


        // Prepare data
        CLI_Section oldPosNewSection = getSection(oldBarIndex - 1);
        var newPosOldTs = newPosOldSection.getData().getTimeSignature();
        var oldPosNewTs = oldPosNewSection.getData().getTimeSignature();
        var ts = cliSection.getData().getTimeSignature();


        if (!ts.equals(newPosOldTs))
        {
            // Adjust position of section items if required
            // For undo to work properly, must be done before firing the "Move section" UndoableEdit.      
            // No need to synchronize the call because change is done before the section change
            var newPosOldSectionBarRange = getBarRange(newPosOldSection);
            var iitems = getItems(newBarIndex, newPosOldSectionBarRange.to, ChordLeadSheetItem.class, cli -> true);
            adjustItemsToTimeSignature(newPosOldTs, ts, iitems);
        }

        if (!ts.equals(oldPosNewTs))
        {
            // CLI_Section before old position might need to be checked too
            var cliSectionBarRange = getBarRange(cliSection);
            var iitems = getItems(oldBarIndex, cliSectionBarRange.to, ChordLeadSheetItem.class, cli -> true);
            adjustItemsToTimeSignature(ts, oldPosNewTs, iitems);
        }

        changeItemPositionChecked(cliSection, new Position(newBarIndex, 0));

        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("Move " + cliSection)
        {
            @Override
            public void undoBody()
            {
                LOGGER.log(Level.FINER, "moveSection.undoBody() cliSection={0} newBarIndex={1}", new Object[]
                {
                    cliSection, newBarIndex
                });
                changeItemPositionChecked(cliSection, new Position(oldBarIndex, 0));
                fireAuthorizedChangeEvent(new SectionMovedEvent(ChordLeadSheetImpl.this, cliSection, newBarIndex, oldBarIndex));
            }

            @Override
            public void redoBody()
            {
                LOGGER.log(Level.FINER, "moveSection.redoBody() cliSection={0} newBarIndex={1}", new Object[]
                {
                    cliSection, newBarIndex
                });
                changeItemPositionChecked(cliSection, new Position(newBarIndex, 0));
                fireAuthorizedChangeEvent(new SectionMovedEvent(ChordLeadSheetImpl.this, cliSection, oldBarIndex, newBarIndex));
            }
        };

        fireUndoableEditHappened(edit);

        // Fire ItemChange event
        fireAuthorizedChangeEvent(event);

        fireActionEvent(enableActionEvent, "moveSection", true, cliSection);
    }

    private <T> void removeItem(final ChordLeadSheetItem<T> item, boolean enableActionEvent)
    {
        if (item == null || (item instanceof CLI_Section))
        {
            throw new IllegalArgumentException("item=" + item);
        }

        LOGGER.log(Level.FINE, "removeItem() -- item={0}", item);

        fireActionEvent(enableActionEvent, "removeItem", false, item);

        // Change state
        removeItemChecked(item);

        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("Remove " + item)
        {
            @Override
            public void undoBody()
            {
                LOGGER.log(Level.FINER, "removeItem.undoBody() item={0}", item);
                addItemChecked(item);
                fireAuthorizedChangeEvent(new ItemAddedEvent(ChordLeadSheetImpl.this, item));
            }

            @Override
            public void redoBody()
            {
                LOGGER.log(Level.FINER, "removeItem.redoBody() item={0}", item);
                removeItemChecked(item);
                fireAuthorizedChangeEvent(new ItemRemovedEvent(ChordLeadSheetImpl.this, item));
            }
        };

        fireUndoableEditHappened(edit);

        // Fire ItemChange event
        fireAuthorizedChangeEvent(new ItemRemovedEvent(ChordLeadSheetImpl.this, item));


        fireActionEvent(enableActionEvent, "removeItem", true, item);
    }

    private <T> void moveItem(ChordLeadSheetItem<T> item, Position newPos, boolean enableActionEvent)
    {
        if (item == null || newPos == null || !(item instanceof WritableItem) || (item instanceof CLI_Section))
        {
            throw new IllegalArgumentException("item=" + item + " newPos=" + newPos);
        }

        LOGGER.log(Level.FINE, "moveItem() -- item={0} newPos={1}", new Object[]
        {
            item, newPos
        });

        final Position oldPos = item.getPosition();
        final Position newAdjustedPos = newPos.limitToTimeSignature(getSection(newPos.getBar()).getData().getTimeSignature());
        if (oldPos.equals(newAdjustedPos))
        {
            return;
        }

        fireActionEvent(enableActionEvent, "moveItem", false, item);


        // Change state
        changeItemPositionChecked(item, newAdjustedPos);


        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("Move " + item)
        {
            @Override
            public void undoBody()
            {
                LOGGER.log(Level.FINER, "moveItem.undoBody() item={0} oldPos={1} newAdjustedPos={2}", new Object[]
                {
                    item, oldPos,
                    newAdjustedPos
                });
                changeItemPositionChecked(item, oldPos);
                fireAuthorizedChangeEvent(new ItemMovedEvent(ChordLeadSheetImpl.this, item, newAdjustedPos, oldPos));
            }

            @Override
            public void redoBody()
            {
                LOGGER.log(Level.FINER, "moveItem.redoBody() item={0} oldPos={1} newAdjustedPos={2}", new Object[]
                {
                    item, oldPos,
                    newAdjustedPos
                });
                changeItemPositionChecked(item, newAdjustedPos);
                fireAuthorizedChangeEvent(new ItemMovedEvent(ChordLeadSheetImpl.this, item, oldPos, newAdjustedPos));
            }
        };
        fireUndoableEditHappened(edit);

        // Fire ItemChange event
        fireAuthorizedChangeEvent(new ItemMovedEvent(ChordLeadSheetImpl.this, item, oldPos, newAdjustedPos));

        fireActionEvent(enableActionEvent, "moveItem", true, item);
    }

    private <T> void changeItem(ChordLeadSheetItem<T> item, final T newData, boolean enableActionEvent)
    {
        if (item == null || !(item instanceof WritableItem) || (item instanceof CLI_Section))
        {
            throw new IllegalArgumentException("item=" + item + " newData=" + newData + " items=" + items);
        }

        LOGGER.log(Level.FINE, "changeItem() -- item={0} newData={1}", new Object[]
        {
            item, newData
        });

        final T oldData = item.getData();
        if (oldData.equals(newData))
        {
            return;
        }

        fireActionEvent(enableActionEvent, "changeItem", false, item);


        // Change state
        changeItemDataChecked(item, newData);


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

                changeItemDataChecked(item, oldData);
                fireAuthorizedChangeEvent(new ItemChangedEvent(ChordLeadSheetImpl.this, item, newData, oldData));
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
                    changeItemDataChecked(item, newData);
                }
                fireAuthorizedChangeEvent(new ItemChangedEvent(ChordLeadSheetImpl.this, item, oldData, newData));
            }
        };
        fireUndoableEditHappened(edit);

        // Fire ItemChange event
        fireAuthorizedChangeEvent(new ItemChangedEvent(ChordLeadSheetImpl.this, item, oldData, newData));

        fireActionEvent(enableActionEvent, "changeItem", true, item);
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


        fireActionEvent(enableActionEvent, "insertBars", false, barIndex);


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

        // Get items to be moved (don't include initiSection)
        CLI_Section initSection = barIndex > 0 ? null : getSection(0);
        var itemsToBeShifted = getItems(barIndex, Integer.MAX_VALUE, ChordLeadSheetItem.class, cli -> cli != initSection);
        if (barIndex > 0 && itemsToBeShifted.isEmpty())
        {
            // Nothing to do
            return;
        }

        if (barIndex > 0)
        {
            shiftItems(itemsToBeShifted, nbBars);
        } else
        {
            // Special case: create a copy of the initial section        
            synchronized (this)
            {

                shiftItems(itemsToBeShifted, nbBars);

                // Create a copy of the init section with different name
                CLI_Factory clif = CLI_Factory.getDefault();
                CLI_Section initSectionCopy = clif.createSection(this,
                        "_" + initSection.getData().getName(),
                        initSection.getData().getTimeSignature(),
                        nbBars);
                try
                {
                    addSection(initSectionCopy);
                } catch (UnsupportedEditException ex)
                {
                    // We should never be there since we don't change the time signature
                    Exceptions.printStackTrace(ex);
                }
            }
        }

        fireActionEvent(enableActionEvent, "insertBars", true, barIndex);
    }

    private void deleteBars(int barIndexFrom, int barIndexTo, boolean enableActionEvent) throws UnsupportedEditException
    {
        if (barIndexFrom < 0 || barIndexTo < barIndexFrom || barIndexTo >= getSizeInBars()
                || (barIndexTo - barIndexFrom + 1) >= getSizeInBars())
        {
            throw new IllegalArgumentException("barIndexFrom=" + barIndexFrom + " barIndexTo=" + barIndexTo);
        }

        LOGGER.log(Level.FINE, "deleteBars() -- barIndexFrom={0} barIndexTo={1}", new Object[]
        {
            barIndexFrom, barIndexTo
        });


        fireActionEvent(enableActionEvent, "deleteBars", false, barIndexFrom);


        // Save data after the bars deletion
        CLI_Section afterDeletionSection = (barIndexTo + 1 > size - 1) ? null : getSection(barIndexTo + 1);
        var afterDeletionSectionItems = afterDeletionSection == null ? null : getItems(afterDeletionSection,
                ChordLeadSheetItem.class,
                cli -> cli.getPosition().getBar() > barIndexTo);


        // Get items to be moved or removed
        CLI_Section initSection = getSection(0);
        var itemsToMove = getItems(barIndexTo + 1, Integer.MAX_VALUE, ChordLeadSheetItem.class, cli -> true);
        var itemsToRemove = getItems(barIndexFrom, barIndexTo, ChordLeadSheetItem.class, cli -> cli != initSection);


        // Remove items except the initial block
        try
        {
            removeSectionsAndItems(itemsToRemove);       // Possible exception here. Note that some changes might have been done before exception is thrown
        } catch (UnsupportedEditException ex)
        {
            fireActionEvent(enableActionEvent, "deleteBars", true, barIndexFrom);       // We need to complete the action
            throw ex;
        }

        int range = barIndexTo - barIndexFrom + 1;
        synchronized (this)
        {
            // Handle special case if barIndexFrom == 0 and there is a section right after the deleted bars
            if (barIndexFrom == 0 && afterDeletionSection != null && afterDeletionSection.getPosition().getBar() == barIndexTo + 1)
            {
                // 


                // Remove the initial section (and fire undoableEvent)
                try
                {
                    removeInitialSection();     // Possible exception here
                } catch (UnsupportedEditException ex)
                {
                    fireActionEvent(enableActionEvent, "deleteBars", true, barIndexFrom);        // Need to complete the ClsActionEvent
                    throw ex;
                }
            }

            // Shift remaining items
            if (!itemsToMove.isEmpty())
            {

                shiftItems(itemsToMove, -range);
            }
        }

        // Adjust the size
        setSize(getSizeInBars() - range, false);         // Possible exception here! But should not happen since we removed items/sections before.


        // Adjust positions of items after the deletion
        if (afterDeletionSectionItems != null)
        {
            CLI_Section newSection = getSection(barIndexFrom);
            adjustItemsToTimeSignature(afterDeletionSection.getData().getTimeSignature(),
                    newSection.getData().getTimeSignature(),
                    afterDeletionSectionItems);
        }

        fireActionEvent(enableActionEvent, "deleteBars", true, barIndexFrom);

    }

    private void setSectionName(CLI_Section cliSection, String name, boolean enableActionEvent)
    {
        Preconditions.checkNotNull(cliSection);
        Preconditions.checkNotNull(name);

        LOGGER.log(Level.FINE, "setSectionName() -- cliSection={0} name={1}", new Object[]
        {
            cliSection, name
        });

        if (cliSection.getData().getName().equals(name))
        {
            return;
        }

        if (!(cliSection instanceof WritableItem) || !items.contains(cliSection) || getSection(name) != null)
        {
            throw new IllegalArgumentException("section=" + cliSection + " name=" + name + " items=" + items);
        }

        fireActionEvent(enableActionEvent, "setSectionName", false, cliSection);

        try
        {
            changeSection(cliSection, new Section(name, cliSection.getData().getTimeSignature()));
        } catch (UnsupportedEditException ex)
        {
            // Should never happen
            Exceptions.printStackTrace(ex);
        }

        fireActionEvent(enableActionEvent, "setSectionName", true, cliSection);
    }

    private void setSectionTimeSignature(CLI_Section cliSection, TimeSignature ts, boolean enableActionEvent) throws UnsupportedEditException
    {
        if (cliSection == null || ts == null || !items.contains(cliSection) || !(cliSection instanceof WritableItem))
        {
            throw new IllegalArgumentException("section=" + cliSection + " ts=" + ts);
        }

        LOGGER.log(Level.FINE, "setSectionTimeSignature() -- cliSection={0} ts={1}", new Object[]
        {
            cliSection, ts
        });

        if (cliSection.getData().getTimeSignature().equals(ts))
        {
            return;
        }


        fireActionEvent(enableActionEvent, "setSectionTimeSignature", false, cliSection);

        try
        {
            changeSection(cliSection, new Section(cliSection.getData().getName(), ts));
        } catch (UnsupportedEditException ex)
        {
            fireActionEvent(enableActionEvent, "setSectionTimeSignature", true, cliSection);           // Need to complete the ClsActionEvent
            throw ex;
        }

        fireActionEvent(enableActionEvent, "setSectionTimeSignature", true, cliSection);
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
    private void removeSectionsAndItems(Collection<ChordLeadSheetItem> allItems) throws UnsupportedEditException
    {
        LOGGER.log(Level.FINER, "removeSectionsAndItems() -- allItems={0}", allItems);

        if (allItems.isEmpty())
        {
            return;
        }

        // Make 2 lists: removedSections for sections, removedItems for other items
        final List<CLI_Section> removedSections = new ArrayList<>();
        final List<ChordLeadSheetItem> removedItems = new ArrayList<>();
        for (var item : allItems)
        {
            if (item instanceof CLI_Section cliSection)
            {
                removedSections.add(cliSection);
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
                        items.addAll(removedItems);
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


        // Remove all sections in reverse order, this has less impact for the editor
        for (var cliSection : Lists.reverse(removedSections))
        {
            removeSection(cliSection, false); // Possible exception here
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
            if (item.isBarSingleItem())
            {
                continue;
            }
            Position oldPos = item.getPosition();
            Position newPos = oldPos.getConvertedPosition(oldTs, newTs);
            if (!newPos.equals(oldPos))
            {
                moveItem(item, newPos, false);
            }
        }
    }

    /**
     * Shift the position of items by nbBars.
     *
     * @param shiftedItems
     * @param nbBars
     */
    private void shiftItems(final List<ChordLeadSheetItem> shiftedItems, final int nbBars)
    {
        Preconditions.checkNotNull(shiftedItems);

        LOGGER.log(Level.FINER, "shiftItems() -- shiftedItems={0} nbBars={1}", new Object[]
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
            for (var item : shiftedItems)
            {
                Position oldPos = item.getPosition();
                oldPositions.add(oldPos);
                int newBar = oldPos.getBar() + nbBars;
                if (newBar < 0 || newBar >= getSizeInBars())
                {
                    throw new IllegalArgumentException("item=" + item + " nbBars=" + nbBars + " size=" + getSizeInBars());
                }
                Position newPos = new Position(newBar, oldPos.getBeat());
                newPositions.add(newPos);

                // Change state
                changeItemPositionChecked(item, newPos);
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
                    for (var item : shiftedItems)
                    {
                        Position oldPos = oldPositions.get(i++);
                        changeItemPositionChecked(item, oldPos);
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
                synchronized (ChordLeadSheetImpl.this)
                {
                    int i = 0;
                    for (var item : shiftedItems)
                    {
                        Position newPos = newPositions.get(i++);
                        changeItemPositionChecked(item, newPos);
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
        var newTs = newData.getTimeSignature();
        var oldTs = oldData.getTimeSignature();

        // Possibly adjust items position if we have a new time signature with less natural beats (adjust also half-bar positions).
        // (and generate undoable SimpleEdits accordingly)   
        // No need to synchronize the call because change is done before the section change
        if (!newTs.equals(oldTs))
        {
            var iitems = getItems(cliSection, ChordLeadSheetItem.class, cli -> true);
            adjustItemsToTimeSignature(oldTs, newTs, iitems);
        }

        changeItemDataChecked(cliSection, newData);


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
                changeItemDataChecked(cliSection, oldData);
                fireAuthorizedChangeEvent(new ItemChangedEvent(ChordLeadSheetImpl.this, cliSection, newData, oldData));
            }

            @Override
            public void redoBody()
            {
                LOGGER.log(Level.FINER, "changeSection.redoBody() cliSection={0} oldData={1} newData={2}", new Object[]
                {
                    cliSection, oldData,
                    newData
                });
                changeItemDataChecked(cliSection, newData);
                fireAuthorizedChangeEvent(new ItemChangedEvent(ChordLeadSheetImpl.this, cliSection, oldData, newData));
            }
        };

        fireUndoableEditHappened(edit);

        // Notify listeners
        fireAuthorizedChangeEvent(event);
    }

    private void removeInitialSection() throws UnsupportedEditException
    {
        CLI_Section cliSection = getSection(0);
        assert cliSection != null : "this=" + this;
        LOGGER.log(Level.FINE, "removeInitialSection() -- initSection={0}", cliSection);

        // Check change is not vetoed
        var event = new ItemRemovedEvent(ChordLeadSheetImpl.this, cliSection);
        authorizeChangeEvent(event);            // Possible exception here! 

        // Change state
        removeItemChecked(cliSection);

        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("Remove initial section " + cliSection)
        {
            @Override
            public void undoBody()
            {
                LOGGER.log(Level.FINER, "removeInitialSection.undoBody() cliSection={0}", cliSection);
                addItemChecked(cliSection);
                fireAuthorizedChangeEvent(new ItemAddedEvent(ChordLeadSheetImpl.this, cliSection));
            }

            @Override
            public void redoBody()
            {
                LOGGER.log(Level.FINER, "removeInitialSection.redoBody() cliSection={0}", cliSection);
                removeItemChecked(cliSection);
                fireAuthorizedChangeEvent(new ItemRemovedEvent(ChordLeadSheetImpl.this, cliSection));
            }
        };

        fireUndoableEditHappened(edit);

        // Fire ItemChange event
        fireAuthorizedChangeEvent(event);
    }

    private synchronized void addItemChecked(ChordLeadSheetItem<?> item)
    {
        var b = items.add(item);
        assert b : "item=" + item + " items=" + items;
    }

    private synchronized void removeItemChecked(ChordLeadSheetItem<?> item)
    {
        var b = items.remove(item);
        assert b : "item=" + item + " items=" + items;
    }

    private synchronized <T> void changeItemDataChecked(ChordLeadSheetItem<T> item, T newData)
    {
        // Remove and add, in case data impacts the ordering
        WritableItem<T> wItem = (WritableItem<T>) item;
        var b = items.remove(wItem);
        assert b : "wItem=" + wItem + " newData=" + newData + " items=" + items;
        wItem.setData(newData);
        items.add(wItem);
        assert b : "wItem=" + wItem + " newData=" + newData + " items=" + items;
    }

    private synchronized <T> void changeItemPositionChecked(ChordLeadSheetItem<T> item, Position newPos)
    {
        WritableItem<T> wItem = (WritableItem<T>) item;
        var b = items.remove(wItem);
        assert b : "wItem=" + wItem + " newPos=" + newPos + " items=" + items;
        wItem.setPosition(newPos);
        items.add(wItem);
        assert b : "wItem=" + wItem + " newPos=" + newPos + " items=" + items;
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
        var ls = listeners.toArray(ClsChangeListener[]::new);
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
        for (ClsChangeListener l : listeners.toArray(ClsChangeListener[]::new))
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
        for (UndoableEditListener l : undoListeners.toArray(UndoableEditListener[]::new))
        {
            l.undoableEditHappened(event);
        }
    }

    /**
     *
     * @param doFire   If false do nothing.
     * @param actionId
     * @param complete
     */
    private void fireActionEvent(boolean doFire, String actionId, boolean complete, Object data)
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
                fireAuthorizedChangeEvent(new ClsActionEvent(ChordLeadSheetImpl.this, actionId, !complete, true, data));
            }

            @Override
            public void redoBody()
            {
                fireAuthorizedChangeEvent(new ClsActionEvent(ChordLeadSheetImpl.this, actionId, complete, false, data));
            }
        };
        fireUndoableEditHappened(edit);


        fireAuthorizedChangeEvent(new ClsActionEvent(this, actionId, complete, false, data));

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
        private final ArrayList<ChordLeadSheetItem> spItems;
        private final int spSize;

        private SerializationProxy(ChordLeadSheetImpl cls)
        {
            spSize = cls.getSizeInBars();
            spItems = new ArrayList<>();
            spItems.addAll(cls.getItems());
        }

        private Object readResolve() throws ObjectStreamException
        {
            if (spItems == null || spItems.size() < 1)
            {
                throw new IllegalStateException("Invalid spItems=" + spItems);
            }


            // Find the initial section
            CLI_Section initSection = null;
            for (var item : spItems)
            {
                if (item instanceof CLI_Section && item.getPosition().getBar() == 0 && item.getPosition().isFirstBarBeat())
                {
                    // CLI_Section's container field must be transient, otherwise with Xstream line below produces 
                    // a non-null but empty section (data=null, pos=null).            
                    // See Effective Java p315.
                    initSection = (CLI_Section) item;
                    break;
                }
            }
            if (initSection == null)
            {
                throw new InvalidObjectException("Missing init section, invalid spItems=" + spItems);
            }


            // Create a ChordLeadSheet and add the items
            ChordLeadSheetImpl cls = new ChordLeadSheetImpl(initSection, spSize);
            for (var item : spItems)
            {
                if (item == initSection)
                {
                    continue;
                }
                if (item instanceof CLI_Section section)
                {
                    try
                    {
                        cls.addSection(section, false);
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

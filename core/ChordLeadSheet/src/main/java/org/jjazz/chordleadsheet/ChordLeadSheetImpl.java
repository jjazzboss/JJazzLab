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
package org.jjazz.chordleadsheet;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.thoughtworks.xstream.XStream;
import org.jjazz.undomanager.api.SimpleEdit;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.UndoableEdit;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.chordleadsheet.api.Section;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.event.*;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.chordleadsheet.item.CLI_SectionImpl;
import org.jjazz.chordleadsheet.api.item.WritableItem;
import org.jjazz.harmony.api.Position;
import org.jjazz.chordleadsheet.api.ClsChangeListener;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.xstream.spi.XStreamConfigurator;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_SAVE;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_SAVE;
import org.openide.util.Exceptions;
import org.openide.util.lookup.ServiceProvider;


public class ChordLeadSheetImpl implements ChordLeadSheet, Serializable
{

    /**
     * The main data structure: keep the items sorted.
     * <p>
     * We can safely use a TreeSet because methods to add/move/change prevent having 2 equal ChordLeadSheetItems.
     */
    private final TreeSet<ChordLeadSheetItem> items = new TreeSet<>();
    /**
     * The size of the leadsheet in bars.
     */
    private int size;
    private transient ClsActionEvent activeClsActionEvent;
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
        Objects.requireNonNull(initSection);
        Preconditions.checkArgument(initSection.getPosition().getBar() == 0, "initSection=%s", initSection);
        Preconditions.checkArgument(size >= 1 && size <= MAX_SIZE, "size=%s", size);

        this.size = size;

        WritableItem<Section> wSection = (WritableItem<Section>) initSection;
        wSection.setContainer(this);
        addItemChecked(wSection);
    }
    
    @Override
    public synchronized ChordLeadSheetImpl getDeepCopy()
    {
        var initSection=getSection(0);
        assert initSection!=null;
        
        ChordLeadSheetImpl clsCopy = new ChordLeadSheetImpl(initSection.getData().getName(), initSection.getData().getTimeSignature(), size);
                
        
        for (var item : getItems())
        {
            if (item == initSection)
            {
                clsCopy.getSection(0).getClientProperties().set(initSection.getClientProperties());
                continue;
            }
            
            var itemCopy = item.getCopy(null, null);
            if (itemCopy instanceof CLI_Section cliSectionCopy)
            {
                try
                {
                    clsCopy.addSection(cliSectionCopy);
                } catch (UnsupportedEditException ex)
                {
                    // We should not be there normally
                    throw new IllegalStateException("Unexpected 'UnsupportedEditException'.", ex);
                }
            } else
            {
                clsCopy.addItem(itemCopy);
            }
        }
        return clsCopy;
    }

    @Override
    public synchronized void setSizeInBars(final int newSize) throws UnsupportedEditException
    {
        Preconditions.checkArgument(newSize >= 1 && newSize <= MAX_SIZE, "newSize=%s", newSize);

        LOGGER.log(Level.FINE, "setSize() -- newSize={0}", newSize);


        final int oldSize = size;
        int delta = newSize - oldSize;
        if (delta == 0)
        {
            return;
        }

        fireClsActionEventStart(ClsActionEvent.API_ID.SetSizeInBars, oldSize);


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
                fireClsActionEventComplete(ClsActionEvent.API_ID.SetSizeInBars);       // We need to complete the action
                throw ex;
            }
        }


        // Check that change is not vetoed
        var event = new SizeChangedEvent(ChordLeadSheetImpl.this, oldSize, newSize);
        try
        {
            fireVetoableChangeEvent(new ClsVetoableChangeEvent(this, event));
        } catch (UnsupportedEditException ex)
        {
            fireClsActionEventComplete(ClsActionEvent.API_ID.SetSizeInBars);       // We need to complete the action
            throw ex;
        }


        // Update state
        size = newSize;


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
                fireNonVetoableChangeEvent(new SizeChangedEvent(ChordLeadSheetImpl.this, newSize, oldSize));
            }

            @Override
            public void redoBody()
            {
                LOGGER.log(Level.FINER, "setSize.redoBody() newSize={0}", newSize);
                synchronized (ChordLeadSheetImpl.this)
                {
                    size = newSize;
                }
                fireNonVetoableChangeEvent(new SizeChangedEvent(ChordLeadSheetImpl.this, oldSize, newSize));
            }
        };

        fireUndoableEditHappened(edit);

        fireNonVetoableChangeEvent(event);

        fireClsActionEventComplete(ClsActionEvent.API_ID.SetSizeInBars);
    }


    @Override
    public synchronized int getSizeInBars()
    {
        return size;
    }

    @Override
    public synchronized boolean addItem(ChordLeadSheetItem<?> item)
    {
        Objects.requireNonNull(item);
        Preconditions.checkArgument(item instanceof WritableItem, "item=%s", item);
        Preconditions.checkArgument(!(item instanceof CLI_Section), "item=%s", item);

        LOGGER.log(Level.FINE, "addItem() -- item={0}", item);


        if (items.contains(item))
        {
            return false;
        }

        final WritableItem<?> wItem = (WritableItem<?>) item;
        int barIndex = wItem.getPosition().getBar();
        if (barIndex >= getSizeInBars())
        {
            throw new IllegalArgumentException("item=" + item + " size=" + getSizeInBars());
        }


        fireClsActionEventStart(ClsActionEvent.API_ID.AddItem, item);


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
        newAdjustedPos = oldPos.getAdjusted(getSection(barIndex).getData().getTimeSignature());
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
                synchronized (ChordLeadSheetImpl.this)
                {
                    removeItemChecked(wItem);
                    wItem.setPosition(oldPos);
                    wItem.setContainer(oldContainer);
                }
                fireNonVetoableChangeEvent(new ItemRemovedEvent(ChordLeadSheetImpl.this, wItem));
            }

            @Override
            public void redoBody()
            {
                LOGGER.log(Level.FINER, "addItem.redoBody() item={0}", item);
                synchronized (ChordLeadSheetImpl.this)
                {
                    wItem.setPosition(newAdjustedPos);
                    wItem.setContainer(newContainer);
                    addItemChecked(wItem);
                }
                fireNonVetoableChangeEvent(new ItemAddedEvent(ChordLeadSheetImpl.this, wItem));
            }
        };

        fireUndoableEditHappened(edit);

        // Fire ItemChange event
        fireNonVetoableChangeEvent(new ItemAddedEvent(ChordLeadSheetImpl.this, wItem));

        fireClsActionEventComplete(ClsActionEvent.API_ID.AddItem);

        return true;
    }


    @Override
    public synchronized CLI_Section addSection(final CLI_Section cliSection) throws UnsupportedEditException
    {
        Objects.requireNonNull(cliSection);
        Preconditions.checkArgument(cliSection instanceof WritableItem, "cliSection=%s", cliSection);

        var bar = cliSection.getPosition().getBar();
        Preconditions.checkArgument(bar < this.size, "cliSection=%s this.size=%s", cliSection, size);

        var sameNameSection = getSection(cliSection.getData().getName());
        Preconditions.checkArgument(sameNameSection == null || sameNameSection.getPosition().getBar() == bar,
                "cliSection=%s sameNameSection=%s", cliSection, sameNameSection);


        CLI_Section res;
        var curSection = getSection(bar);
        if (curSection.getPosition().getBar() == bar)
        {
            setSectionName(curSection, cliSection.getData().getName());
            setSectionTimeSignature(curSection, cliSection.getData().getTimeSignature());
            res = curSection;

        } else
        {
            LOGGER.log(Level.FINE, "addSection() -- cliSection={0}", cliSection);

            res = cliSection;

            // Check that change is not vetoed
            var event = new ItemAddedEvent(ChordLeadSheetImpl.this, cliSection);
            fireVetoableChangeEvent(new ClsVetoableChangeEvent(this, event));   // throws UnsupportedEditException
            fireClsActionEventStart(ClsActionEvent.API_ID.AddSection, cliSection);


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
                    synchronized (ChordLeadSheetImpl.this)
                    {
                        removeItemChecked(wSection);
                        wSection.setContainer(oldContainer);
                    }
                    fireNonVetoableChangeEvent(new ItemRemovedEvent(ChordLeadSheetImpl.this, wSection));
                }

                @Override
                public void redoBody()
                {
                    LOGGER.log(Level.FINER, "addSection.redoBody() cliSection={0}", cliSection);
                    synchronized (ChordLeadSheetImpl.this)
                    {
                        wSection.setContainer(newContainer);
                        addItemChecked(wSection);
                    }
                    fireNonVetoableChangeEvent(new ItemAddedEvent(ChordLeadSheetImpl.this, wSection));
                }
            };

            // Must be fired BEFORE the vetoable change
            fireUndoableEditHappened(edit);

            fireNonVetoableChangeEvent(event);

            fireClsActionEventComplete(ClsActionEvent.API_ID.AddSection);

        }

        return res;
    }


    @Override
    public synchronized void removeSection(final CLI_Section cliSection) throws UnsupportedEditException
    {
        Objects.requireNonNull(cliSection);
        Preconditions.checkArgument(cliSection.getPosition().getBar() != 0, "cliSection=%s", cliSection);
        Preconditions.checkArgument(items.contains(cliSection), "cliSection=%s items=%s", cliSection, items);

        LOGGER.log(Level.FINE, "removeSection() -- cliSection={0}", cliSection);

        final int barIndex = cliSection.getPosition().getBar();


        // Check that change is not vetoed
        var event = new ItemRemovedEvent(ChordLeadSheetImpl.this, cliSection);
        fireVetoableChangeEvent(new ClsVetoableChangeEvent(this, event));   // throws UnsupportedEditException
        fireClsActionEventStart(ClsActionEvent.API_ID.RemoveSection, cliSection);

        // Prepare data
        var newSection = getSection(barIndex - 1);
        var newTs = newSection.getData().getTimeSignature();
        var oldTs = cliSection.getData().getTimeSignature();


        // Change state
        if (!newTs.equals(oldTs))
        {
            // Adjust position of trailing items if required
            // For undo to work properly, must be done before firing the "Remove section" UndoableEdit.      
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
                synchronized (ChordLeadSheetImpl.this)
                {
                    addItemChecked(cliSection);
                }
                fireNonVetoableChangeEvent(new ItemAddedEvent(ChordLeadSheetImpl.this, cliSection));
            }

            @Override
            public void redoBody()
            {
                LOGGER.log(Level.FINER, "removeSection.redoBody() cliSection={0}", cliSection);
                synchronized (ChordLeadSheetImpl.this)
                {
                    removeItemChecked(cliSection);
                }
                fireNonVetoableChangeEvent(new ItemRemovedEvent(ChordLeadSheetImpl.this, cliSection));
            }
        };

        fireUndoableEditHappened(edit);

        // Fire ItemChange event
        fireNonVetoableChangeEvent(event);


        fireClsActionEventComplete(ClsActionEvent.API_ID.RemoveSection);
    }


    @Override
    public synchronized void moveSection(final CLI_Section cliSection, final int newBarIndex) throws UnsupportedEditException
    {
        Preconditions.checkArgument(cliSection instanceof WritableItem, "cliSection=%s", cliSection);
        Preconditions.checkArgument(newBarIndex > 0 && newBarIndex < getSizeInBars(), "newBarIndex=%s", newBarIndex);
        Preconditions.checkArgument(items.contains(cliSection), "cliSection=%s items=%s", cliSection, items);

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
        fireVetoableChangeEvent(new ClsVetoableChangeEvent(this, event));   // throws UnsupportedEditException
        fireClsActionEventStart(ClsActionEvent.API_ID.MoveSection, cliSection);


        // Prepare data
        CLI_Section oldPosNewSection = getSection(oldBarIndex - 1);
        var newPosOldTs = newPosOldSection.getData().getTimeSignature();
        var oldPosNewTs = oldPosNewSection.getData().getTimeSignature();
        var ts = cliSection.getData().getTimeSignature();


        if (!ts.equals(newPosOldTs))
        {
            // Adjust position of section items if required
            // For undo to work properly, must be done before firing the "Move section" UndoableEdit.      
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

        changeItemPositionChecked(cliSection, new Position(newBarIndex));

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
                synchronized (ChordLeadSheetImpl.this)
                {
                    changeItemPositionChecked(cliSection, new Position(oldBarIndex));
                }
                fireNonVetoableChangeEvent(new SectionMovedEvent(ChordLeadSheetImpl.this, cliSection, newBarIndex, oldBarIndex));
            }

            @Override
            public void redoBody()
            {
                LOGGER.log(Level.FINER, "moveSection.redoBody() cliSection={0} newBarIndex={1}", new Object[]
                {
                    cliSection, newBarIndex
                });
                synchronized (ChordLeadSheetImpl.this)
                {
                    changeItemPositionChecked(cliSection, new Position(newBarIndex));
                }
                fireNonVetoableChangeEvent(new SectionMovedEvent(ChordLeadSheetImpl.this, cliSection, oldBarIndex, newBarIndex));
            }
        };

        fireUndoableEditHappened(edit);

        // Fire ItemChange event
        fireNonVetoableChangeEvent(event);

        fireClsActionEventComplete(ClsActionEvent.API_ID.MoveSection);
    }


    @Override
    public synchronized void removeItem(final ChordLeadSheetItem<?> item)
    {
        Objects.requireNonNull(item);
        Preconditions.checkArgument(!(item instanceof CLI_Section), "item=%s", item);
        Preconditions.checkArgument(items.contains(item), "items=%s item=%s", items, item);

        LOGGER.log(Level.FINE, "removeItem() -- item={0}", item);

        fireClsActionEventStart(ClsActionEvent.API_ID.RemoveItem, item);

        // Change state
        removeItemChecked(item);

        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("Remove " + item)
        {
            @Override
            public void undoBody()
            {
                LOGGER.log(Level.FINER, "removeItem.undoBody() item={0}", item);
                synchronized (ChordLeadSheetImpl.this)
                {
                    addItemChecked(item);
                }
                fireNonVetoableChangeEvent(new ItemAddedEvent(ChordLeadSheetImpl.this, item));
            }

            @Override
            public void redoBody()
            {
                LOGGER.log(Level.FINER, "removeItem.redoBody() item={0}", item);
                synchronized (ChordLeadSheetImpl.this)
                {
                    removeItemChecked(item);
                }
                fireNonVetoableChangeEvent(new ItemRemovedEvent(ChordLeadSheetImpl.this, item));
            }
        };

        fireUndoableEditHappened(edit);

        // Fire ItemChange event
        fireNonVetoableChangeEvent(new ItemRemovedEvent(ChordLeadSheetImpl.this, item));


        fireClsActionEventComplete(ClsActionEvent.API_ID.RemoveItem);
    }


    @Override
    public synchronized boolean moveItem(ChordLeadSheetItem<?> item, Position newPos)
    {
        Objects.requireNonNull(newPos);
        Preconditions.checkArgument(item instanceof WritableItem, "item=%s", item);
        Preconditions.checkArgument(!(item instanceof CLI_Section), "item=%s", item);
        Preconditions.checkArgument(items.contains(item), "items=%s item=%s", items, item);


        LOGGER.log(Level.FINE, "moveItem() -- item={0} newPos={1}", new Object[]
        {
            item, newPos
        });

        final Position oldPos = item.getPosition();
        final Position newAdjustedPos = newPos.getAdjusted(getSection(newPos.getBar()).getData().getTimeSignature());
        if (oldPos.equals(newAdjustedPos) || items.contains(item.getCopy(null, newPos)))
        {
            return false;
        }

        fireClsActionEventStart(ClsActionEvent.API_ID.MoveItem, item);


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
                synchronized (ChordLeadSheetImpl.this)
                {
                    changeItemPositionChecked(item, oldPos);
                }
                fireNonVetoableChangeEvent(new ItemMovedEvent(ChordLeadSheetImpl.this, item, newAdjustedPos, oldPos));
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
                    changeItemPositionChecked(item, newAdjustedPos);
                }
                fireNonVetoableChangeEvent(new ItemMovedEvent(ChordLeadSheetImpl.this, item, oldPos, newAdjustedPos));
            }
        };
        fireUndoableEditHappened(edit);

        // Fire ItemChange event
        fireNonVetoableChangeEvent(new ItemMovedEvent(ChordLeadSheetImpl.this, item, oldPos, newAdjustedPos));

        fireClsActionEventComplete(ClsActionEvent.API_ID.MoveItem);

        return true;
    }


    @Override
    public synchronized <T> boolean changeItem(ChordLeadSheetItem<T> item, final T newData)
    {
        Preconditions.checkArgument(item instanceof WritableItem, "item=%s", item);
        Preconditions.checkArgument(!(item instanceof CLI_Section), "item=%s", item);
        Preconditions.checkArgument(items.contains(item), "items=%s item=%s", items, item);

        LOGGER.log(Level.FINE, "changeItem() -- item={0} newData={1}", new Object[]
        {
            item, newData
        });

        final T oldData = item.getData();
        if (oldData.equals(newData) || items.contains(item.getCopy(newData, null)))
        {
            return false;
        }

        fireClsActionEventStart(ClsActionEvent.API_ID.ChangeItem, item);


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

                synchronized (ChordLeadSheetImpl.this)
                {
                    changeItemDataChecked(item, oldData);
                }
                fireNonVetoableChangeEvent(new ItemChangedEvent(ChordLeadSheetImpl.this, item, newData, oldData));
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
                fireNonVetoableChangeEvent(new ItemChangedEvent(ChordLeadSheetImpl.this, item, oldData, newData));
            }
        };
        fireUndoableEditHappened(edit);

        // Fire ItemChange event
        fireNonVetoableChangeEvent(new ItemChangedEvent(ChordLeadSheetImpl.this, item, oldData, newData));

        fireClsActionEventComplete(ClsActionEvent.API_ID.ChangeItem);

        return true;
    }


    @Override
    public synchronized void insertBars(final int barIndex, final int nbBars)
    {
        Preconditions.checkArgument(barIndex >= 0 && barIndex <= getSizeInBars(), "barIndex=%s nbBars=%s", barIndex, nbBars);
        Preconditions.checkArgument(nbBars >= 0, "barIndex=%s nbBars=%s", barIndex, nbBars);

        LOGGER.log(Level.FINE, "insertBars() -- barIndex={0} nbBars={1}", new Object[]
        {
            barIndex, nbBars
        });

        if (nbBars == 0)
        {
            return;
        }

        fireClsActionEventStart(ClsActionEvent.API_ID.InsertBars, barIndex);


        // First set the size larger
        final int newSize = getSizeInBars() + nbBars;
        try
        {
            setSizeInBars(newSize);
        } catch (UnsupportedEditException ex)
        {
            // Should never happen as we increase the size
            fireClsActionEventComplete(ClsActionEvent.API_ID.InsertBars);
            Exceptions.printStackTrace(ex);
        }

        // Shift items 
        if (barIndex > 0)
        {
            // Easy case, we don't touch the initSection
            var shiftedItems = getItems(barIndex, Integer.MAX_VALUE, ChordLeadSheetItem.class, cli -> true);
            shiftItems(shiftedItems, nbBars);

        } else
        {
            // Handle the initSection
            CLI_Section initSection = getSection(0);
            var shiftedItems = getItems(barIndex, Integer.MAX_VALUE, ChordLeadSheetItem.class, cli -> cli != initSection);
            shiftItems(shiftedItems, nbBars);


            // Create a copy of the init section before changing its name
            CLI_Section initSectionCopy = initSection.getCopy(new Position(nbBars), null);  // cls=null so that name is reused as is. 

            // Rename init section                
            String newInitSectionName = "_" + initSection.getData().getName();
            while (getSection(newInitSectionName) != null)
            {
                newInitSectionName = "_" + newInitSectionName;
            }
            setSectionName(initSection, newInitSectionName);


            try
            {
                addSection(initSectionCopy);    // will update its container with this
            } catch (UnsupportedEditException ex)
            {
                // We should never be there since we don't change the time signature
                Exceptions.printStackTrace(ex);
            }
        }


        fireClsActionEventComplete(ClsActionEvent.API_ID.InsertBars);
    }


    @Override
    public synchronized void deleteBars(int barIndexFrom, int barIndexTo) throws UnsupportedEditException
    {
        Preconditions.checkArgument(barIndexFrom >= 0
                && barIndexTo >= barIndexFrom
                && barIndexTo < getSizeInBars()
                && (barIndexTo - barIndexFrom + 1) < getSizeInBars(),
                "barIndexFrom=%s barIndexTo=%s", barIndexFrom, barIndexTo);

        LOGGER.log(Level.FINE, "deleteBars() -- barIndexFrom={0} barIndexTo={1}", new Object[]
        {
            barIndexFrom, barIndexTo
        });


        fireClsActionEventStart(ClsActionEvent.API_ID.DeleteBars, barIndexFrom);


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
            fireClsActionEventComplete(ClsActionEvent.API_ID.DeleteBars);       // We need to complete the action
            throw ex;
        }

        int range = barIndexTo - barIndexFrom + 1;

        // Handle special case if barIndexFrom == 0 and there is a section right after the deleted bars
        if (barIndexFrom == 0 && afterDeletionSection != null && afterDeletionSection.getPosition().getBar() == barIndexTo + 1)
        {
            // Remove the initial section (and fire undoableEvent)
            try
            {
                removeInitialSection();     // Possible exception here
            } catch (UnsupportedEditException ex)
            {
                fireClsActionEventComplete(ClsActionEvent.API_ID.DeleteBars);        // Need to complete the ClsActionEvent
                throw ex;
            }
        }

        // Shift remaining items
        shiftItems(itemsToMove, -range);

        // Adjust the size
        setSizeInBars(getSizeInBars() - range);         // Possible exception here! But should not happen since we removed items/sections before.


        // Adjust positions of items after the deletion
        if (afterDeletionSectionItems != null)
        {
            CLI_Section newSection = getSection(barIndexFrom);
            adjustItemsToTimeSignature(afterDeletionSection.getData().getTimeSignature(),
                    newSection.getData().getTimeSignature(),
                    afterDeletionSectionItems);
        }

        fireClsActionEventComplete(ClsActionEvent.API_ID.DeleteBars);

    }


    @Override
    public synchronized void setSectionName(CLI_Section cliSection, String name)
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

        if (!(cliSection instanceof WritableItem) || !items.contains(cliSection) || getSection(name) != null || !items.contains(cliSection))
        {
            throw new IllegalArgumentException("section=" + cliSection + " name=" + name + " items=" + items);
        }

        fireClsActionEventStart(ClsActionEvent.API_ID.SetSectionName, cliSection);

        try
        {
            changeSection(cliSection, new Section(name, cliSection.getData().getTimeSignature()));
        } catch (UnsupportedEditException ex)
        {
            // Should never happen
            fireClsActionEventComplete(ClsActionEvent.API_ID.SetSectionName);
            Exceptions.printStackTrace(ex);
        }

        fireClsActionEventComplete(ClsActionEvent.API_ID.SetSectionName);
    }


    @Override
    public synchronized void setSectionTimeSignature(CLI_Section cliSection, TimeSignature ts) throws UnsupportedEditException
    {
        Preconditions.checkNotNull(ts);
        Preconditions.checkArgument(cliSection instanceof WritableItem, "cliSection=%s", cliSection);
        Preconditions.checkArgument(items.contains(cliSection), "items=%s cliSection=%s", items, cliSection);

        LOGGER.log(Level.FINE, "setSectionTimeSignature() -- cliSection={0} ts={1}", new Object[]
        {
            cliSection, ts
        });

        if (cliSection.getData().getTimeSignature().equals(ts))
        {
            return;
        }


        fireClsActionEventStart(ClsActionEvent.API_ID.SetSectionTimeSignature, cliSection);

        try
        {
            changeSection(cliSection, new Section(cliSection.getData().getName(), ts));
        } catch (UnsupportedEditException ex)
        {
            fireClsActionEventComplete(ClsActionEvent.API_ID.SetSectionTimeSignature);          // Need to complete the ClsActionEvent
            throw ex;
        }

        fireClsActionEventComplete(ClsActionEvent.API_ID.SetSectionTimeSignature);
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
    public <T extends ChordLeadSheetItem<?>> T getFirstItemAfter(Position posFrom, boolean inclusiveFrom, Class<T> itemClass, Predicate<T> tester)
    {
        var itemFrom = ChordLeadSheetItem.createItemFrom(posFrom, inclusiveFrom);
        return getFirstItemAfter(itemFrom, itemClass, tester);
    }

    @Override
    public synchronized <T extends ChordLeadSheetItem<?>> T getFirstItemAfter(ChordLeadSheetItem<?> cli, Class<T> itemClass, Predicate<T> tester)
    {
        Preconditions.checkNotNull(cli);
        Preconditions.checkNotNull(tester);
        Preconditions.checkNotNull(itemClass);

        T res = null;
        var tailSet = items.tailSet(cli, false);
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
    public <T extends ChordLeadSheetItem<?>> T getLastItemBefore(Position posTo, boolean inclusiveTo, Class<T> itemClass, Predicate<T> tester)
    {
        var itemTo = ChordLeadSheetItem.createItemTo(posTo, inclusiveTo);
        return getLastItemBefore(itemTo, itemClass, tester);
    }

    @Override
    public synchronized <T extends ChordLeadSheetItem<?>> T getLastItemBefore(ChordLeadSheetItem<?> cli, Class<T> itemClass, Predicate<T> tester)
    {
        Preconditions.checkNotNull(cli);
        Preconditions.checkNotNull(tester);
        Preconditions.checkNotNull(itemClass);
        T res = null;
        var headSet = items.headSet(cli, false);
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

        var rangeItems = items.subSet(
                ChordLeadSheetItem.createItemFrom(posFrom, inclusiveFrom),
                false,
                ChordLeadSheetItem.createItemTo(posTo, inclusiveTo),
                false);

        var res = rangeItems.stream()
                .filter(item -> itemClass.isAssignableFrom(item.getClass()))
                .map(cli -> (T) cli)
                .filter(tester)
                .toList();

        return res;
    }


    @Override
    public synchronized void addUndoableEditListener(UndoableEditListener l)
    {
        if (l == null)
        {
            throw new NullPointerException("l=" + l);
        }
        undoListeners.remove(l);
        undoListeners.add(l);
    }

    @Override
    public synchronized void removeUndoableEditListener(UndoableEditListener l)
    {
        if (l == null)
        {
            throw new NullPointerException("l=" + l);
        }
        undoListeners.remove(l);
    }

    @Override
    public synchronized void addClsChangeListener(ClsChangeListener l)
    {
        if (l == null)
        {
            throw new NullPointerException("l=" + l);
        }
        listeners.remove(l);
        listeners.add(l);
    }

    @Override
    public synchronized void removeClsChangeListener(ClsChangeListener l)
    {
        if (l == null)
        {
            throw new NullPointerException("l=" + l);
        }
        listeners.remove(l);
    }

    @Override
    public synchronized String toString()
    {
        return "ChordLeadSheet section0=" + getSection(0).getData().getName() + " size=" + getSizeInBars();
    }


    @Override
    public synchronized boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final ChordLeadSheetImpl other = (ChordLeadSheetImpl) obj;
        if (this.size != other.size)
        {
            return false;
        }
        return Objects.equals(this.items, other.items);
    }


    // --------------------------------------------------------------------------------------
    // Private methods
    // --------------------------------------------------------------------------------------
    /**
     * Remove sections and items.
     * <p>
     * Chord symbols are removed in a single undoable operation. Then sections are removed one by one using removeSection() (possible exception).
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
                removedItems.forEach(item -> removeItemChecked(item));
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
                        removedItems.forEach(item -> addItemChecked(item));
                    }
                    fireNonVetoableChangeEvent(new ItemAddedEvent(ChordLeadSheetImpl.this, removedItems));
                }

                @Override
                public void redoBody()
                {
                    LOGGER.log(Level.FINER, "removeSectionsAndItems.redoBody() removedItems={0}", removedItems);
                    synchronized (ChordLeadSheetImpl.this)
                    {
                        removedItems.forEach(item -> removeItemChecked(item));
                    }
                    fireNonVetoableChangeEvent(new ItemRemovedEvent(ChordLeadSheetImpl.this, removedItems));
                }
            };
            fireUndoableEditHappened(edit);

            // Fire the change
            fireNonVetoableChangeEvent(new ItemRemovedEvent(ChordLeadSheetImpl.this, removedItems));
        }


        // Remove all sections in reverse order, this has less impact for the editor
        for (var cliSection : Lists.reverse(removedSections))
        {
            removeSection(cliSection); // Possible exception here
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
            Position oldPos = item.getPosition();
            Position newPos = oldPos.getConverted(oldTs, newTs);
            if (!newPos.equals(oldPos))
            {
                if (!moveItem(item, newPos))
                {
                    // Special case, eg in 4/4: 2 identical chords are on beats 3 and 3.5, when switching to 3/4, the move from 3.5 to 3 will do nothing
                    removeItem(item);
                }
            }
        }
    }

    /**
     * Shift the position of items by nbBars.
     *
     * @param shiftedItems Must be ordered by position
     * @param nbBars
     */
    private void shiftItems(final List<ChordLeadSheetItem> shiftedItems, final int nbBars)
    {
        Preconditions.checkNotNull(shiftedItems);
        if (nbBars < 0)
        {
            Preconditions.checkArgument(shiftedItems.isEmpty() || shiftedItems.getFirst().getPosition().getBar() - nbBars >= 0,
                    "nbBars=%s shiftedItems=%s", nbBars, shiftedItems);
        } else
        {
            Preconditions.checkArgument(shiftedItems.isEmpty() || shiftedItems.getLast().getPosition().getBar() + nbBars < getSizeInBars(),
                    "nbBars=%s shiftedItems=%s", nbBars, shiftedItems);
        }

        LOGGER.log(Level.FINER, "shiftItems() -- shiftedItems={0} nbBars={1}", new Object[]
        {
            shiftedItems, nbBars
        });

        Collections.synchronizedList(listeners);
        if (shiftedItems.isEmpty() || nbBars == 0)
        {
            return;
        }

        record SavedItem(ChordLeadSheetItem item, Position oldPos, Position newPos)
                {

        }
        final List<SavedItem> savedItems = new ArrayList<>();
        final List<ChordLeadSheetItem> itemList = nbBars < 0 ? shiftedItems : shiftedItems.reversed();

        synchronized (this)
        {
            for (var item : itemList)
            {
                var oldPos = item.getPosition();
                var newPos = oldPos.getMoved(nbBars, 0);
                savedItems.add(new SavedItem(item, oldPos, newPos));

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
                synchronized (ChordLeadSheetImpl.this)
                {
                    for (var savedItem : savedItems.reversed())
                    {
                        changeItemPositionChecked(savedItem.item(), savedItem.oldPos());
                    }
                }
                fireNonVetoableChangeEvent(new ItemBarShiftedEvent(ChordLeadSheetImpl.this, shiftedItems, -nbBars));
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
                    for (var savedItem : savedItems)
                    {
                        changeItemPositionChecked(savedItem.item(), savedItem.newPos());
                    }
                }
                fireNonVetoableChangeEvent(new ItemBarShiftedEvent(ChordLeadSheetImpl.this, shiftedItems, nbBars));
            }
        };
        fireUndoableEditHappened(edit);

        // Fire the change
        fireNonVetoableChangeEvent(new ItemBarShiftedEvent(ChordLeadSheetImpl.this, shiftedItems, nbBars));
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
        fireVetoableChangeEvent(new ClsVetoableChangeEvent(this, event));   // throws UnsupportedEditException

        // Change state
        var newTs = newData.getTimeSignature();
        var oldTs = oldData.getTimeSignature();

        // Possibly adjust items position if we have a new time signature with less natural beats (adjust also half-bar positions).
        // (and generate undoable SimpleEdits accordingly)   
        if (!newTs.equals(oldTs))
        {
            var iitems = getItems(cliSection, ChordLeadSheetItem.class, cli -> true);
            adjustItemsToTimeSignature(oldTs, newTs, iitems);
        }

        synchronized (this)
        {
            changeItemDataChecked(cliSection, newData);
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
                    changeItemDataChecked(cliSection, oldData);
                }
                fireNonVetoableChangeEvent(new ItemChangedEvent(ChordLeadSheetImpl.this, cliSection, newData, oldData));
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
                    changeItemDataChecked(cliSection, newData);
                }
                fireNonVetoableChangeEvent(new ItemChangedEvent(ChordLeadSheetImpl.this, cliSection, oldData, newData));
            }
        };

        fireUndoableEditHappened(edit);

        // Notify listeners
        fireNonVetoableChangeEvent(event);
    }

    private void removeInitialSection() throws UnsupportedEditException
    {
        CLI_Section cliSection = getSection(0);
        assert cliSection != null : "this=" + this;
        LOGGER.log(Level.FINE, "removeInitialSection() -- initSection={0}", cliSection);

        // Check change is not vetoed
        var event = new ItemRemovedEvent(ChordLeadSheetImpl.this, cliSection);
        fireVetoableChangeEvent(new ClsVetoableChangeEvent(this, event));   // throws UnsupportedEditException

        // Change state
        synchronized (this)
        {
            removeItemChecked(cliSection);
        }

        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("Remove initial section " + cliSection)
        {
            @Override
            public void undoBody()
            {
                LOGGER.log(Level.FINER, "removeInitialSection.undoBody() cliSection={0}", cliSection);
                synchronized (ChordLeadSheetImpl.this)
                {
                    addItemChecked(cliSection);
                }
                fireNonVetoableChangeEvent(new ItemAddedEvent(ChordLeadSheetImpl.this, cliSection));
            }

            @Override
            public void redoBody()
            {
                LOGGER.log(Level.FINER, "removeInitialSection.redoBody() cliSection={0}", cliSection);
                synchronized (ChordLeadSheetImpl.this)
                {
                    removeItemChecked(cliSection);
                }
                fireNonVetoableChangeEvent(new ItemRemovedEvent(ChordLeadSheetImpl.this, cliSection));
            }
        };

        fireUndoableEditHappened(edit);

        // Fire ItemChange event
        fireNonVetoableChangeEvent(event);
    }

    /**
     * Perform the add: check internal state consistency
     *
     * @param item
     */
    private void addItemChecked(ChordLeadSheetItem<?> item)
    {
        var b = items.add(item);
        assert b : "item=" + item + " items=" + items;
    }

    private void removeItemChecked(ChordLeadSheetItem<?> item)
    {
        var b = items.remove(item);
        assert b : "item=" + item + " items=" + items;
    }

    private <T> void changeItemDataChecked(ChordLeadSheetItem<T> item, T newData)
    {
        WritableItem<T> wItem = (WritableItem<T>) item;
        var b = items.remove(wItem);
        assert b : "wItem=" + wItem + " newData=" + newData + " items=" + items;
        wItem.setData(newData);
        b = items.add(wItem);
        assert b : "wItem=" + wItem + " newData=" + newData + " items=" + items;
    }

    private <T> void changeItemPositionChecked(ChordLeadSheetItem<T> item, Position newPos)
    {
        WritableItem<T> wItem = (WritableItem<T>) item;
        var b = items.remove(wItem);
        assert b : "wItem=" + wItem + " newPos=" + newPos + " items=" + items;
        wItem.setPosition(newPos);
        b = items.add(wItem);
        assert b : "wItem=" + wItem + " newPos=" + newPos + " items=" + items;
    }

    /**
     * Make sure that a change is authorized by all listeners.
     * <p>
     *
     * @param event
     * @throws org.jjazz.chordleadsheet.api.UnsupportedEditException
     */
    protected void fireVetoableChangeEvent(ClsVetoableChangeEvent event) throws UnsupportedEditException
    {
        ClsChangeListener[] snapshot;
        synchronized (this)
        {
            snapshot = listeners.toArray(ClsChangeListener[]::new);
        }
        for (ClsChangeListener l : snapshot)
        {
            l.chordLeadSheetChanged(event);
        }
    }

    /**
     * Fire a change event to all listeners.
     * <p>
     * If it's not a ClsActionEvent also add the event to the active ClsActionEvent.
     *
     * @param event Can not be a ClsVetoableChangeEvent
     */
    protected void fireNonVetoableChangeEvent(ClsChangeEvent event)
    {
        Objects.requireNonNull(event);
        Preconditions.checkArgument(!(event instanceof ClsVetoableChangeEvent), "event=%s", event);

        ClsChangeListener[] snapshot;
        synchronized (this)
        {
            if (!(event instanceof ClsActionEvent))
            {
                assert activeClsActionEvent != null : "event=" + event;
                activeClsActionEvent.addSubEvent(event);
            }

            snapshot = listeners.toArray(ClsChangeListener[]::new);
        }

        for (ClsChangeListener l : snapshot)
        {
            try
            {
                l.chordLeadSheetChanged(event);
            } catch (UnsupportedEditException ex)
            {
                // Should never happen
                Exceptions.printStackTrace(ex);
            }
        }
    }

    private void fireUndoableEditHappened(UndoableEdit edit)
    {
        if (edit == null)
        {
            throw new IllegalArgumentException("edit=" + edit);
        }
        UndoableEditEvent event = new UndoableEditEvent(this, edit);

        UndoableEditListener[] snapshot;
        synchronized (this)
        {
            snapshot = undoListeners.toArray(UndoableEditListener[]::new);
        }

        for (UndoableEditListener l : snapshot)
        {
            l.undoableEditHappened(event);
        }
    }

    /**
     * Set the active ClsActionEvent, unless there is already an active ClsActionEvent, and fire the required events.
     * <p>
     * This creates an undoable edit and notify listeners.
     *
     * @param apiId
     * @param data
     */
    private synchronized void fireClsActionEventStart(ClsActionEvent.API_ID apiId, Object data)
    {
        Objects.requireNonNull(apiId);
        LOGGER.log(Level.FINE, "fireClsActionEventStart() -- apiId={0} activeClsActionEvent.apiId={1}  data={2}", new Object[]
        {
            apiId, activeClsActionEvent == null ? "null" : activeClsActionEvent.getApiId(), data
        });

        if (activeClsActionEvent != null)
        {
            assert activeClsActionEvent.getApiId() != apiId :
                    "apiId=" + apiId + " activeClsActionEvent=" + activeClsActionEvent + " data=" + Objects.toString(data, "null");
            return;
        }

        // Create an undoable event for this event which does nothing but refiring the ClsActionEvent
        UndoableEdit edit = new SimpleEdit("ClsActionEventEdit(" + apiId + ")")
        {
            @Override
            public void undoBody()
            {
                synchronized (ChordLeadSheetImpl.this)
                {
                    assert activeClsActionEvent != null;        // previously set by undo from fireClsActionEventComplete()
                    activeClsActionEvent.complete();
                    fireNonVetoableChangeEvent(activeClsActionEvent);
                    activeClsActionEvent = null;
                }
            }

            @Override
            public void redoBody()
            {
                synchronized (ChordLeadSheetImpl.this)
                {
                    assert activeClsActionEvent == null : "activeClsActionEvent" + activeClsActionEvent;        // previously set by undoBody()
                    activeClsActionEvent = new ClsActionEvent(ChordLeadSheetImpl.this, apiId, data);
                    fireNonVetoableChangeEvent(activeClsActionEvent);
                }
            }
        };
        fireUndoableEditHappened(edit);

        LOGGER.log(Level.FINE, "fireClsActionEventStart() create new activeClsActionEvent apiId={0}  data={1}", new Object[]
        {
            apiId, data
        });
        activeClsActionEvent = new ClsActionEvent(this, apiId, data);
        fireNonVetoableChangeEvent(activeClsActionEvent);
    }

    /**
     * Complete the active ClsActionEvent, unless the active one is not linked to actionId, and fire the required events.
     *
     * @param apiId
     */
    private synchronized void fireClsActionEventComplete(ClsActionEvent.API_ID apiId)
    {
        Objects.requireNonNull(apiId);
        UndoableEdit edit;

        assert activeClsActionEvent != null : "apiId=" + apiId;
        if (activeClsActionEvent.getApiId() != apiId)
        {
            return;
        }

        var data = activeClsActionEvent.getData();

        // Create an undoable event for this event which does nothing but refiring the ClsActionEvent
        edit = new SimpleEdit("ClsActionEventEdit(" + apiId + ")")
        {
            @Override
            public void undoBody()
            {
                synchronized (ChordLeadSheetImpl.this)
                {
                    assert activeClsActionEvent == null : "activeClsActionEvent=" + activeClsActionEvent;   // previously set by fireClsActionEventComplete()
                    activeClsActionEvent = new ClsActionEvent(ChordLeadSheetImpl.this, apiId, data);
                    fireNonVetoableChangeEvent(activeClsActionEvent);
                }
            }

            @Override
            public void redoBody()
            {
                synchronized (ChordLeadSheetImpl.this)
                {
                    assert activeClsActionEvent != null;            // previously set by undoBody
                    activeClsActionEvent.complete();
                    fireNonVetoableChangeEvent(activeClsActionEvent);
                    activeClsActionEvent = null;
                }
            }
        };
        fireUndoableEditHappened(edit);

        activeClsActionEvent.complete();
        fireNonVetoableChangeEvent(activeClsActionEvent);
        LOGGER.log(Level.FINE, "fireClsActionEventComplete() RESETTING activeClsActionEvent apiId={0}", new Object[]
        {
            apiId
        });
        activeClsActionEvent = null;
    }

    /**
     * This enables XStream instance configuration even for private classes or classes from non-public packages of Netbeans modules.
     * <p>
     */
    @ServiceProvider(service = XStreamConfigurator.class)
    public static class XStreamConfig implements XStreamConfigurator
    {

        @Override
        public void configure(XStreamConfigurator.InstanceId instanceId, XStream xstream)
        {
            switch (instanceId)
            {
                case SONG_LOAD, SONG_SAVE ->
                {

                    // From 4.1.0 new aliases to get rid of fully qualified class names in .sng files
                    xstream.alias("ChordLeadSheetImpl", ChordLeadSheetImpl.class);
                    xstream.alias("ChordLeadSheetImplSP", SerializationProxy.class);

                }
                case MIDIMIX_LOAD ->
                {
                    // Nothing
                }
                case MIDIMIX_SAVE ->
                {
                    // Nothing
                }
                default -> throw new AssertionError(instanceId.name());
            }
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
     * Allow to be independent of future chordleadsheet internal data structure changes.<p>
     * spVERSION 2 (JJazzLab 4.1.0) introduces several aliases to get rid of hard-coded qualified class names (XStreamConfig class introduction).<br>
     */
    private static class SerializationProxy implements Serializable
    {

        private static final long serialVersionUID = 2879716323116L;
        private final int spVERSION = 2;
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
                        cls.addSection(section);
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

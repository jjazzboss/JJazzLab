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
import com.thoughtworks.xstream.XStream;
import org.jjazz.undomanager.api.SimpleEdit;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.function.Supplier;
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

/**
 * ChordLeadSheet implementation.
 * <p>
 * This implementation is thread-safe: <br>
 * - Reads and writes are serialized using ReentrantReadWriteLock.<br>
 * - Item implementations cannot be modified by API clients (WritableItem interface is module-private).<br>
 * <p>
 * Synchronous listeners (which are invoked while holding the write lock) must follow the documented non-blocking contract.
 */
public class ChordLeadSheetImpl implements ChordLeadSheet, Serializable
{
    /**
     * The main data structure: keep the items sorted by position and type.
     * <p>
     * We can safely use a TreeSet because methods to add/move/change prevent having 2 equal ChordLeadSheetItems.
     */
    private final TreeSet<ChordLeadSheetItem> items;

    /**
     * The size of the leadsheet in bars.
     */
    private int size;
    /**
     * Lock can be shared via getLock() for external synchronization
     */
    private final transient ReentrantReadWriteLock lock;
    /**
     * The listeners for changes in this LeadSheet.
     */
    protected final transient CopyOnWriteArrayList<ClsChangeListener> listeners;
    protected final transient CopyOnWriteArrayList<ClsChangeListener> syncListeners;
    /**
     * The listeners for undoable edits in this LeadSheet.
     */
    protected final transient CopyOnWriteArrayList<UndoableEditListener> undoListeners;
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

        this.syncListeners = new CopyOnWriteArrayList<>();
        this.listeners = new CopyOnWriteArrayList<>();
        this.undoListeners = new CopyOnWriteArrayList<>();
        this.items = new TreeSet<>();
        this.lock = new ReentrantReadWriteLock();
        this.size = size;


        items.add(initSection);
    }

    @Override
    public ReentrantReadWriteLock getLock()
    {
        return lock;
    }

    @Override
    public ChordLeadSheetImpl getDeepCopy()
    {
        ChordLeadSheetImpl clsCopy = null;

        lock.readLock().lock();

        try
        {
            var initSection = getSection(0);
            assert initSection != null;

            clsCopy = new ChordLeadSheetImpl(initSection.getData().getName(), initSection.getData().getTimeSignature(), size);


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
                        // we should not be there since current instance is supposed to be ok
                        throw new IllegalStateException("Unexpected 'UnsupportedEditException'.", ex);
                    }
                } else
                {
                    clsCopy.addItem(itemCopy);
                }
            }
        } finally
        {
            lock.readLock().unlock();
        }

        return clsCopy;
    }

    @Override
    public void setSizeInBars(final int newSize)
    {
        Preconditions.checkArgument(newSize >= 1 && newSize <= MAX_SIZE, "newSize=%s", newSize);

        LOGGER.log(Level.FINE, "setSizeInBars() -- newSize={0}", newSize);


        // Operation
        Supplier<OperationResults> operation = () -> 
        {
            final int oldSize = size;

            if (newSize == oldSize)
            {
                return new OperationResults(null, null, null);
            }


            final List<ChordLeadSheetItem> itemsToRemove = new ArrayList<>();
            if (newSize < oldSize)
            {
                itemsToRemove.addAll(getItems(newSize, Integer.MAX_VALUE, ChordLeadSheetItem.class));
            }


            // Update model
            itemsToRemove.forEach(item -> removeItemChecked(item));
            size = newSize;


            // Create events
            UndoableEdit edit = new SimpleEdit("Set Size " + newSize)
            {
                @Override
                public void undoBody()
                {
                    LOGGER.log(Level.FINER, "setSizeInBars.undoBody() newSize={0}", newSize);
                    performAPImethodUndoRedo(() -> 
                    {
                        size = oldSize;
                        itemsToRemove.forEach(item -> addItemChecked(item));

                        var event = new SizeChangedEvent(ChordLeadSheetImpl.this, oldSize, newSize, itemsToRemove);
                        event.setIsUndo();
                        return event;
                    });
                }

                @Override
                public void redoBody()
                {
                    LOGGER.log(Level.FINER, "setSizeInBars.redoBody() newSize={0}", newSize);
                    performAPImethodUndoRedo(() -> 
                    {
                        itemsToRemove.forEach(item -> removeItemChecked(item));
                        size = newSize;

                        var event = new SizeChangedEvent(ChordLeadSheetImpl.this, oldSize, newSize, itemsToRemove);
                        event.setIsRedo();
                        return event;
                    });
                }
            };

            var event = new SizeChangedEvent(ChordLeadSheetImpl.this, oldSize, newSize, itemsToRemove);
            return new OperationResults(event, edit, null);
        };

        performAPImethod(operation);
    }

    @Override
    public int getSizeInBars()
    {
        lock.readLock().lock();
        try
        {
            return size;
        } finally
        {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean addItem(ChordLeadSheetItem<?> item)
    {
        Objects.requireNonNull(item);
        Preconditions.checkArgument(item instanceof WritableItem, "item=%s", item);
        Preconditions.checkArgument(!(item instanceof CLI_Section), "item=%s", item);


        LOGGER.log(Level.FINE, "addItem() -- item={0}", item);


        Supplier<OperationResults> operation = () -> 
        {
            if (items.contains(item))
            {
                LOGGER.log(Level.FINE, "addItem() item already present. item={0}", item);
                return new OperationResults(null, null, Boolean.FALSE);
            }

            final Position oldPos = item.getPosition();
            int barIndex = oldPos.getBar();
            if (barIndex >= getSizeInBars())
            {
                throw new IllegalArgumentException("item=" + item + " size=" + getSizeInBars());
            }


            final ChordLeadSheet oldContainer = item.getContainer();


            // Update model
            final WritableItem<?> wItem = (WritableItem<?>) item;
            final Position newAdjustedPos = oldPos.getAdjusted(getSection(barIndex).getData().getTimeSignature());
            wItem.setPosition(newAdjustedPos);
            addItemChecked(wItem);


            // Create the undoable event
            UndoableEdit edit = new SimpleEdit("Add " + wItem)
            {
                @Override
                public void undoBody()
                {
                    LOGGER.log(Level.FINER, "addItem.undoBody() item={0}", item);
                    performAPImethodUndoRedo(() -> 
                    {
                        removeItemChecked(wItem);
                        wItem.setPosition(oldPos);
                        wItem.setContainer(oldContainer);
                        var event = new ItemAddedEvent(ChordLeadSheetImpl.this, wItem);
                        event.setIsUndo();
                        return event;
                    });

                }

                @Override
                public void redoBody()
                {
                    LOGGER.log(Level.FINER, "addItem.redoBody() item={0}", item);
                    performAPImethodUndoRedo(() -> 
                    {
                        wItem.setPosition(newAdjustedPos);
                        addItemChecked(wItem);
                        var event = new ItemAddedEvent(ChordLeadSheetImpl.this, wItem);
                        event.setIsRedo();
                        return event;
                    });
                }
            };

            var event = new ItemAddedEvent(ChordLeadSheetImpl.this, wItem);
            return new OperationResults(event, edit, Boolean.TRUE);
        };

        Boolean b = performAPImethod(operation);
        return b;
    }

    @Override
    public void addSection(final CLI_Section cliSection) throws UnsupportedEditException
    {
        Objects.requireNonNull(cliSection);
        Preconditions.checkArgument(cliSection instanceof WritableItem, "cliSection=%s", cliSection);


        LOGGER.log(Level.FINE, "addSection() -- cliSection={0}", cliSection);


        // Operation
        ThrowingSupplier<OperationResults, UnsupportedEditException> operation = () -> 
        {

            // Verify constraints
            final int bar = cliSection.getPosition().getBar();
            Preconditions.checkArgument(bar < this.size, "cliSection=%s this.size=%s", cliSection, size);

            final CLI_Section curSection = getSection(bar);
            var sameNameSection = getSection(cliSection.getData().getName());
            Preconditions.checkArgument(sameNameSection == null || sameNameSection.getPosition().getBar() == bar,
                    "cliSection=%s sameNameSection=%s", cliSection, sameNameSection);


            // Fire vetoable event
            var vetoEvent = new SectionAddedEvent(ChordLeadSheetImpl.this, cliSection,
                    curSection.getPosition().getBar() == bar ? curSection : null,
                    Collections.emptyList());
            fireSynchronizedVetoableChangeEvent(new ClsVetoableChangeEvent(ChordLeadSheetImpl.this, vetoEvent));   // throws UnsupportedEditException


            // Prepare data
            final TimeSignature oldTs = curSection.getData().getTimeSignature();
            final boolean isReplace = curSection.getPosition().getBar() == bar;
            final WritableItem<Section> wSection = (WritableItem<Section>) cliSection;
            final ChordLeadSheet oldContainer = wSection.getContainer();


            // Update model
            if (isReplace)
            {
                removeItemChecked(curSection);
            }

            addItemChecked(cliSection);

            final TimeSignature newTs = cliSection.getData().getTimeSignature();
            final List<ItemMoved> adjustments = new ArrayList<>();
            final List<ChordLeadSheetItem> adjustedItems = new ArrayList<>();
            if (newTs.getNbNaturalBeats() < oldTs.getNbNaturalBeats())
            {
                var iitems = getItems(cliSection, ChordLeadSheetItem.class, cli -> !newTs.checkBeat(cli.getPosition().getBeat()));
                adjustments.addAll(adjustItemsToTimeSignature(oldTs, newTs, iitems));
                adjustedItems.addAll(adjustments.stream().map(ItemMoved::item).toList());
            }


            // Create events
            UndoableEdit edit = new SimpleEdit("Add Section " + wSection)
            {
                @Override
                public void undoBody()
                {
                    LOGGER.log(Level.FINER, "addSection.undoBody() section={0}", wSection);
                    performAPImethodUndoRedo(() -> 
                    {
                        for (var adjustment : adjustments.reversed())
                        {
                            changeItemPositionChecked(adjustment.item(), adjustment.oldPos());
                        }

                        removeItemChecked(wSection);
                        wSection.setContainer(oldContainer);

                        if (isReplace)
                        {
                            addItemChecked(curSection);
                        }

                        var event = new SectionAddedEvent(ChordLeadSheetImpl.this, cliSection, isReplace ? curSection : null, adjustedItems);
                        event.setIsUndo();
                        return event;
                    });
                }

                @Override
                public void redoBody()
                {
                    LOGGER.log(Level.FINER, "addSection.redoBody() section={0}", wSection);
                    performAPImethodUndoRedo(() -> 
                    {
                        if (isReplace)
                        {
                            removeItemChecked(curSection);
                        }

                        addItemChecked(wSection);

                        for (var adjustment : adjustments)
                        {
                            changeItemPositionChecked(adjustment.item(), adjustment.newPos());
                        }

                        var event = new SectionAddedEvent(ChordLeadSheetImpl.this, cliSection, isReplace ? curSection : null, adjustedItems);
                        event.setIsRedo();
                        return event;
                    });
                }
            };

            var event = new SectionAddedEvent(ChordLeadSheetImpl.this, cliSection, isReplace ? curSection : null, adjustedItems);
            return new OperationResults(event, edit, null);
        };


        performAPImethodThrowing(operation);            // throws UnsupportedEditException

    }

    @Override
    public void removeSection(final CLI_Section cliSection)
    {
        Objects.requireNonNull(cliSection);
        Preconditions.checkArgument(cliSection.getPosition().getBar() != 0, "cliSection=%s", cliSection);

        LOGGER.log(Level.FINE, "removeSection() -- cliSection={0}", cliSection);


        // Operation
        Supplier<OperationResults> operation = () -> 
        {
            Preconditions.checkArgument(items.contains(cliSection), "cliSection=%s items=%s", cliSection, items);


            // Context: The section previous to the one being removed will take over the scope
            final int bar = cliSection.getPosition().getBar();
            final CLI_Section prevSection = getSection(bar - 1);
            final TimeSignature newTs = prevSection.getData().getTimeSignature();
            final TimeSignature oldTs = cliSection.getData().getTimeSignature();


            // Update model
            removeItemChecked(cliSection);

            final List<ItemMoved> adjustments = new ArrayList<>();
            final List<ChordLeadSheetItem> adjustedItems = new ArrayList<>();
            if (newTs.getNbNaturalBeats() < oldTs.getNbNaturalBeats())
            {
                var iitems = getItems(prevSection, ChordLeadSheetItem.class, cli -> !newTs.checkBeat(cli.getPosition().getBeat()));
                adjustments.addAll(adjustItemsToTimeSignature(oldTs, newTs, iitems));
                adjustedItems.addAll(adjustments.stream().map(ItemMoved::item).toList());
            }


            // Create events
            UndoableEdit edit = new SimpleEdit("Remove Section " + cliSection)
            {
                @Override
                public void undoBody()
                {
                    LOGGER.log(Level.FINER, "removeSection.undoBody() section={0}", cliSection);
                    performAPImethodUndoRedo(() -> 
                    {
                        for (var adjustment : adjustments.reversed())
                        {
                            changeItemPositionChecked(adjustment.item(), adjustment.oldPos());
                        }

                        addItemChecked(cliSection);

                        var event = new SectionRemovedEvent(ChordLeadSheetImpl.this, cliSection, adjustedItems);
                        event.setIsUndo();
                        return event;
                    });
                }

                @Override
                public void redoBody()
                {
                    LOGGER.log(Level.FINER, "removeSection.redoBody() section={0}", cliSection);
                    performAPImethodUndoRedo(() -> 
                    {
                        removeItemChecked(cliSection);

                        for (var adjustment : adjustments)
                        {
                            changeItemPositionChecked(adjustment.item(), adjustment.newPos());
                        }

                        var event = new SectionRemovedEvent(ChordLeadSheetImpl.this, cliSection, adjustedItems);
                        event.setIsRedo();
                        return event;
                    });
                }
            };

            var event = new SectionRemovedEvent(ChordLeadSheetImpl.this, cliSection, adjustedItems);
            return new OperationResults(event, edit, null);
        };

        performAPImethod(operation);
    }

    @Override
    public void moveSection(final CLI_Section cliSection, final int newBarIndex)
    {
        Preconditions.checkArgument(cliSection instanceof WritableItem && cliSection.getPosition().getBar() != 0, "cliSection=%s", cliSection);
        Preconditions.checkArgument(newBarIndex > 0, "newBarIndex=%s", newBarIndex);

        LOGGER.log(Level.FINE, "moveSection() -- cliSection={0} newBarIndex={1}", new Object[]
        {
            cliSection, newBarIndex
        });

        Supplier<OperationResults> operation = () -> 
        {
            Preconditions.checkArgument(newBarIndex < size, "newBarIndex=%s size=%s", newBarIndex, size);
            Preconditions.checkArgument(items.contains(cliSection), "cliSection=%s items=%s", cliSection, items);

            final int oldBarIndex = cliSection.getPosition().getBar();
            if (newBarIndex == oldBarIndex)
            {
                return new OperationResults(null, null, null);
            }

            CLI_Section sectionAtNewPos = getSection(newBarIndex);
            if (sectionAtNewPos.getPosition().getBar() == newBarIndex)
            {
                throw new IllegalArgumentException("There is already a section at destination bar " + newBarIndex);
            }


            // Update model
            changeItemPositionChecked(cliSection, new Position(newBarIndex));


            final Set<CLI_Section> impactedSections = new HashSet<>();
            impactedSections.add(cliSection);
            impactedSections.add(getSection(oldBarIndex));      // might be also cliSection, hence the Set
            final List<ItemMoved> adjustments = new ArrayList<>();
            final List<ChordLeadSheetItem> adjustedItems = new ArrayList<>();
            for (var cliSect : impactedSections)
            {
                var newTs = cliSect.getData().getTimeSignature();
                var oldTs = getSection(cliSect.getPosition().getBar() - 1).getData().getTimeSignature();
                var iitems = getItems(cliSect, ChordLeadSheetItem.class, cli -> !newTs.checkBeat(cli.getPosition().getBeat()));
                adjustments.addAll(adjustItemsToTimeSignature(oldTs, newTs, iitems));
                adjustedItems.addAll(adjustments.stream().map(ItemMoved::item).toList());
            }


            // Events
            UndoableEdit edit = new SimpleEdit("Move Section " + cliSection)
            {
                @Override
                public void undoBody()
                {
                    LOGGER.log(Level.FINER, "moveSection.undoBody() section={0}", cliSection);
                    performAPImethodUndoRedo(() -> 
                    {
                        for (var adjustment : adjustments.reversed())
                        {
                            changeItemPositionChecked(adjustment.item(), adjustment.oldPos());
                        }

                        changeItemPositionChecked(cliSection, new Position(oldBarIndex));

                        var event = new SectionMovedEvent(ChordLeadSheetImpl.this, cliSection, oldBarIndex, newBarIndex, adjustedItems);
                        event.setIsUndo();
                        return event;
                    });
                }

                @Override
                public void redoBody()
                {
                    LOGGER.log(Level.FINER, "moveSection.redoBody() section={0}", cliSection);
                    performAPImethodUndoRedo(() -> 
                    {
                        changeItemPositionChecked(cliSection, new Position(newBarIndex));

                        for (var adjustment : adjustments)
                        {
                            changeItemPositionChecked(adjustment.item(), adjustment.newPos());
                        }

                        var event = new SectionMovedEvent(ChordLeadSheetImpl.this, cliSection, oldBarIndex, newBarIndex, adjustedItems);
                        event.setIsRedo();
                        return event;
                    });
                }
            };

            var event = new SectionMovedEvent(ChordLeadSheetImpl.this, cliSection, oldBarIndex, newBarIndex, adjustedItems);
            return new OperationResults(event, edit, null);
        };

        performAPImethod(operation);
    }

    @Override
    public boolean removeItem(final ChordLeadSheetItem<?> item)
    {
        Objects.requireNonNull(item);
        Preconditions.checkArgument(!(item instanceof CLI_Section), "item=%s", item);

        LOGGER.log(Level.FINE, "removeItem() -- item={0}", item);

        Supplier<OperationResults> operation = () -> 
        {
            if (!items.contains(item))
            {
                LOGGER.log(Level.FINE, "addItem() item not present. item={0}", item);
                return new OperationResults(null, null, Boolean.FALSE);
            }


            // Change state
            removeItemChecked(item);


            // Create the undoable event
            UndoableEdit edit = new SimpleEdit("Remove " + item)
            {
                @Override
                public void undoBody()
                {
                    LOGGER.log(Level.FINER, "removeItem.undoBody() item={0}", item);
                    performAPImethodUndoRedo(() -> 
                    {
                        addItemChecked(item);
                        var event = new ItemRemovedEvent(ChordLeadSheetImpl.this, item);
                        event.setIsUndo();
                        return event;
                    });
                }

                @Override
                public void redoBody()
                {
                    LOGGER.log(Level.FINER, "removeItem.redoBody() item={0}", item);
                    performAPImethodUndoRedo(() -> 
                    {
                        removeItemChecked(item);
                        var event = new ItemRemovedEvent(ChordLeadSheetImpl.this, item);
                        event.setIsRedo();
                        return event;
                    });
                }
            };

            var event = new ItemRemovedEvent(ChordLeadSheetImpl.this, item);
            return new OperationResults(event, edit, Boolean.TRUE);
        };

        Boolean b = performAPImethod(operation);
        return b;
    }

    @Override
    public boolean moveItem(ChordLeadSheetItem<?> item, Position newPos)
    {
        Objects.requireNonNull(newPos);
        Preconditions.checkArgument(item instanceof WritableItem, "item=%s", item);
        Preconditions.checkArgument(!(item instanceof CLI_Section), "item=%s", item);

        LOGGER.log(Level.FINE, "moveItem() -- item={0} newPos={1}", new Object[]
        {
            item, newPos
        });

        Supplier<OperationResults> operation = () -> 
        {
            Preconditions.checkArgument(items.contains(item), "item=%s items=%s", item, items);

            final Position oldPos = item.getPosition();
            final Position newAdjustedPos = newPos.getAdjusted(getSection(newPos.getBar()).getData().getTimeSignature());

            if (oldPos.equals(newAdjustedPos) || items.contains(item.getCopy(null, newAdjustedPos)))
            {
                return new OperationResults(null, null, Boolean.FALSE);
            }

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
                    performAPImethodUndoRedo(() -> 
                    {
                        changeItemPositionChecked(item, oldPos);
                        var event = new ItemMovedEvent(ChordLeadSheetImpl.this, item, oldPos, newAdjustedPos);
                        event.setIsUndo();
                        return event;
                    });
                }

                @Override
                public void redoBody()
                {
                    LOGGER.log(Level.FINER, "moveItem.redoBody() item={0} oldPos={1} newAdjustedPos={2}", new Object[]
                    {
                        item, oldPos,
                        newAdjustedPos
                    });
                    performAPImethodUndoRedo(() -> 
                    {
                        changeItemPositionChecked(item, newAdjustedPos);
                        var event = new ItemMovedEvent(ChordLeadSheetImpl.this, item, oldPos, newAdjustedPos);
                        event.setIsRedo();
                        return event;
                    });
                }
            };

            var event = new ItemMovedEvent(ChordLeadSheetImpl.this, item, oldPos, newAdjustedPos);
            return new OperationResults(event, edit, Boolean.TRUE);
        };

        Boolean b = performAPImethod(operation);
        return b;
    }

    @Override
    public <T> boolean changeItem(ChordLeadSheetItem<T> item, final T newData)
    {
        Preconditions.checkArgument(item instanceof WritableItem, "item=%s", item);
        Preconditions.checkArgument(!(item instanceof CLI_Section), "item=%s", item);

        LOGGER.log(Level.FINE, "changeItem() -- item={0} newData={1}", new Object[]
        {
            item, newData
        });

        Supplier<OperationResults> operation = () -> 
        {
            Preconditions.checkArgument(items.contains(item), "item=%s items=%s", item, items);

            final T oldData = item.getData();
            if (oldData.equals(newData) || items.contains(item.getCopy(newData, null)))
            {
                return new OperationResults(null, null, Boolean.FALSE);
            }

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
                    performAPImethodUndoRedo(() -> 
                    {
                        changeItemDataChecked(item, oldData);
                        var event = new ItemChangedEvent(ChordLeadSheetImpl.this, item, oldData, newData);
                        event.setIsUndo();
                        return event;
                    });
                }

                @Override
                public void redoBody()
                {
                    LOGGER.log(Level.FINER, "changeItem.redoBody() item={0} oldData={1} newData={2}", new Object[]
                    {
                        item, oldData, newData
                    });
                    performAPImethodUndoRedo(() -> 
                    {
                        changeItemDataChecked(item, newData);
                        var event = new ItemChangedEvent(ChordLeadSheetImpl.this, item, oldData, newData);
                        event.setIsRedo();
                        return event;
                    });
                }
            };

            var event = new ItemChangedEvent(ChordLeadSheetImpl.this, item, oldData, newData);
            return new OperationResults(event, edit, Boolean.TRUE);
        };

        boolean b = performAPImethod(operation);
        return b;
    }

    @Override
    public void insertBars(final int barIndex, final int nbBars)
    {
        Preconditions.checkArgument(barIndex >= 0 && nbBars >= 0, "barIndex=%s nbBars=%s", barIndex, nbBars);

        LOGGER.log(Level.FINE, "insertBars() -- barIndex={0} nbBars={1}", new Object[]
        {
            barIndex, nbBars
        });

        if (nbBars == 0)
        {
            return;
        }

        Supplier<OperationResults> operation = () -> 
        {
            Preconditions.checkArgument(barIndex <= size, "barIndex=%s size=%s", barIndex, size);

            final int oldSize = size;
            final int newSize = oldSize + nbBars;


            // Init section special case
            final CLI_Section newSection0;
            if (barIndex == 0)
            {
                var section0 = getSection(0);
                assert section0 instanceof WritableItem : "section0=" + section0;
                var data0 = section0.getData();
                String newName = "_" + data0.getName();
                while (getSection(newName) != null)
                {
                    newName = "_" + newName;
                }
                newSection0 = section0.getCopy(new Position(0), null);
                ((WritableItem) newSection0).setData(new Section(newName, data0.getTimeSignature()));
            } else
            {
                newSection0 = null;
            }


            // --- Update model ---
            size = newSize;

            final List<ChordLeadSheetItem> shiftedItems = getItems(barIndex, Integer.MAX_VALUE, ChordLeadSheetItem.class, cli -> true);
            for (var item : shiftedItems.reversed())
            {
                changeItemPositionChecked(item, item.getPosition().getMoved(nbBars, 0));
            }

            if (newSection0 != null)
            {
                addItemChecked(newSection0);
            }


            // Create events
            UndoableEdit edit = new SimpleEdit("Insert Bars " + barIndex + " nb=" + nbBars)
            {
                @Override
                public void undoBody()
                {
                    LOGGER.log(Level.FINER, "insertBars.undoBody() nbBars={0}", nbBars);
                    performAPImethodUndoRedo(() -> 
                    {
                        if (newSection0 != null)
                        {
                            removeItemChecked(newSection0);
                        }

                        for (var item : shiftedItems)
                        {
                            changeItemPositionChecked(item, item.getPosition().getMoved(-nbBars, 0));
                        }

                        size = oldSize;

                        var undoEvent = new InsertedBarsEvent(ChordLeadSheetImpl.this, barIndex, nbBars, shiftedItems);
                        undoEvent.setIsUndo();
                        return undoEvent;
                    });
                }

                @Override
                public void redoBody()
                {
                    LOGGER.log(Level.FINER, "insertBars.redoBody() nbBars={0}", nbBars);
                    performAPImethodUndoRedo(() -> 
                    {
                        size = newSize;


                        for (var item : shiftedItems.reversed())
                        {
                            changeItemPositionChecked(item, item.getPosition().getMoved(nbBars, 0));
                        }

                        if (newSection0 != null)
                        {
                            addItemChecked(newSection0);
                        }

                        var redoEvent = new InsertedBarsEvent(ChordLeadSheetImpl.this, barIndex, nbBars, shiftedItems);
                        redoEvent.setIsRedo();
                        return redoEvent;
                    });
                }
            };

            var event = new InsertedBarsEvent(ChordLeadSheetImpl.this, barIndex, nbBars, shiftedItems);
            return new OperationResults(event, edit, null);
        };

        performAPImethod(operation);
    }

    @Override
    public void deleteBars(int barIndexFrom, int barIndexTo)
    {
        Preconditions.checkArgument(barIndexFrom >= 0 && barIndexTo >= barIndexFrom,
                "barIndexFrom=%s barIndexTo=%s", barIndexFrom, barIndexTo);

        LOGGER.log(Level.FINE, "deleteBars() -- barIndexFrom={0} barIndexTo={1}", new Object[]
        {
            barIndexFrom, barIndexTo
        });


        Supplier<OperationResults> operation = () -> 
        {
            Preconditions.checkArgument(barIndexTo < size && barIndexTo - barIndexFrom + 1 < size, "barIndexFrom=%s barIndexTo=%s size=%s", barIndexFrom,
                    barIndexTo, size);

            final int nbBars = barIndexTo - barIndexFrom + 1;
            final int oldSize = size;
            final int newSize = size - nbBars;
            CLI_Section section0 = getSection(0);


            // Determine context *before* mutation at the point just after the cut
            final boolean noBarsAfterCut = barIndexTo + 1 == oldSize;
            final CLI_Section oldSectionAfter = noBarsAfterCut ? null : getSection(barIndexTo + 1);


            // Identify items to remove
            // If we delete from 0, and there is a section immediately after the cut, it replaces the current initSection, then we can remove it
            boolean removeInitSection = barIndexFrom == 0 && oldSectionAfter != null && oldSectionAfter.getPosition().getBar() == barIndexTo + 1;
            final var itemsToRemove = getItems(barIndexFrom, barIndexTo, ChordLeadSheetItem.class, removeInitSection ? cli -> true : cli -> cli != section0);


            // Identify items to shift
            final var itemsToShift = getItems(barIndexTo + 1, Integer.MAX_VALUE, ChordLeadSheetItem.class, cli -> true);      // empty if noBarsAfterCut


            // Update model
            for (var item : itemsToRemove)
            {
                removeItemChecked(item);
            }

            for (var item : itemsToShift)
            {
                Position oldPos = item.getPosition();
                Position newPos = oldPos.getMoved(-nbBars, 0);
                changeItemPositionChecked(item, newPos);
            }

            final List<ItemMoved> adjustments = new ArrayList<>();
            final List<ChordLeadSheetItem> itemsToAdjust = new ArrayList<>();
            if (!noBarsAfterCut)
            {
                // Determine the new context at the deletion point
                assert oldSectionAfter != null;
                var oldTsAfter = oldSectionAfter.getData().getTimeSignature();
                CLI_Section newSectionAfter = getSection(barIndexFrom);
                TimeSignature newTsAfter = newSectionAfter.getData().getTimeSignature();
                if (newTsAfter.getNbNaturalBeats() < oldTsAfter.getNbNaturalBeats())
                {
                    var iitems = getItems(newSectionAfter, ChordLeadSheetItem.class, cli -> itemsToShift.contains(cli));
                    adjustments.addAll(adjustItemsToTimeSignature(oldTsAfter, newTsAfter, iitems));
                    itemsToAdjust.addAll(adjustments.stream().map(ItemMoved::item).toList());
                }
            }

            size = newSize;


            // Create events
            UndoableEdit edit = new SimpleEdit("Delete Bars " + barIndexFrom + "-" + barIndexTo)
            {
                @Override
                public void undoBody()
                {
                    LOGGER.log(Level.FINER, "deleteBars.undoBody() nbBars={0}", nbBars);
                    performAPImethodUndoRedo(() -> 
                    {
                        size = oldSize;

                        for (var adjustment : adjustments.reversed())
                        {
                            changeItemPositionChecked(adjustment.item(), adjustment.oldPos());
                        }

                        for (var item : itemsToShift.reversed())
                        {
                            Position newPos = item.getPosition().getMoved(nbBars, 0);
                            changeItemPositionChecked(item, newPos);
                        }

                        for (var item : itemsToRemove)
                        {
                            addItemChecked(item);
                        }

                        var event = new DeletedBarsEvent(ChordLeadSheetImpl.this, barIndexFrom, barIndexTo, itemsToRemove, itemsToShift, itemsToAdjust);
                        event.setIsUndo();
                        return event;
                    });
                }

                @Override
                public void redoBody()
                {
                    LOGGER.log(Level.FINER, "deleteBars.redoBody() nbBars={0}", nbBars);
                    performAPImethodUndoRedo(() -> 
                    {
                        for (var item : itemsToRemove)
                        {
                            removeItemChecked(item);
                        }

                        for (var item : itemsToShift)
                        {
                            Position newPos = item.getPosition().getMoved(-nbBars, 0);
                            changeItemPositionChecked(item, newPos);
                        }

                        for (var adjustment : adjustments)
                        {
                            changeItemPositionChecked(adjustment.item(), adjustment.newPos());
                        }

                        size = newSize;
                        var event = new DeletedBarsEvent(ChordLeadSheetImpl.this, barIndexFrom, barIndexTo, itemsToRemove, itemsToShift, itemsToAdjust);
                        event.setIsRedo();
                        return event;
                    });
                }
            };

            var event = new DeletedBarsEvent(ChordLeadSheetImpl.this, barIndexFrom, barIndexTo, itemsToRemove, itemsToShift, itemsToAdjust);
            return new OperationResults(event, edit, null);
        };

        performAPImethod(operation);
    }

    @Override
    public void setSectionName(CLI_Section cliSection, String name)
    {
        Preconditions.checkNotNull(cliSection);
        Preconditions.checkNotNull(name);
        Preconditions.checkArgument(cliSection instanceof WritableItem, "cliSection=%s", cliSection);

        if (cliSection.getData().getName().equals(name))
        {
            return;
        }

        LOGGER.log(Level.FINE, "setSectionName() -- cliSection={0} name={1}", new Object[]
        {
            cliSection, name
        });

        Supplier<OperationResults> operation = () -> 
        {
            Preconditions.checkArgument(items.contains(cliSection), "cliSection=%s items=%s", cliSection, items);
            Preconditions.checkArgument(getSection(name) == null, "cliSection=%s items=%s", cliSection, items);


            final Section oldData = cliSection.getData();
            final Section newData = new Section(name, oldData.getTimeSignature());


            // Change state
            changeItemDataChecked(cliSection, newData);

            // Create the undoable event
            UndoableEdit edit = new SimpleEdit("Set Section Name " + name)
            {
                @Override
                public void undoBody()
                {
                    LOGGER.log(Level.FINER, "setSectionName.undoBody() cliSection={0} oldData={1} newData={2}", new Object[]
                    {
                        cliSection, oldData,
                        newData
                    });
                    performAPImethodUndoRedo(() -> 
                    {
                        changeItemDataChecked(cliSection, oldData);
                        var event = new SectionChangedEvent(ChordLeadSheetImpl.this, cliSection, oldData, newData, Collections.emptyList());
                        event.setIsUndo();
                        return event;
                    });
                }

                @Override
                public void redoBody()
                {
                    LOGGER.log(Level.FINER, "setSectionName.redoBody() cliSection={0} oldData={1} newData={2}", new Object[]
                    {
                        cliSection, oldData,
                        newData
                    });
                    performAPImethodUndoRedo(() -> 
                    {
                        changeItemDataChecked(cliSection, newData);
                        var event = new SectionChangedEvent(ChordLeadSheetImpl.this, cliSection, oldData, newData, Collections.emptyList());
                        event.setIsRedo();
                        return event;
                    });
                }
            };

            var event = new SectionChangedEvent(ChordLeadSheetImpl.this, cliSection, oldData, newData, Collections.emptyList());
            return new OperationResults(event, edit, null);
        };

        performAPImethod(operation);
    }

    @Override
    public void setSectionTimeSignature(CLI_Section cliSection, TimeSignature ts) throws UnsupportedEditException
    {
        Preconditions.checkNotNull(ts);
        Preconditions.checkArgument(cliSection instanceof WritableItem, "cliSection=%s", cliSection);
        final Section oldData = cliSection.getData();
        if (oldData.getTimeSignature().equals(ts))
        {
            return;
        }

        LOGGER.log(Level.FINE, "setSectionTimeSignature() -- cliSection={0} ts={1}", new Object[]
        {
            cliSection, ts
        });


        ThrowingSupplier<OperationResults, UnsupportedEditException> operation = () -> 
        {
            Preconditions.checkArgument(items.contains(cliSection), "cliSection=%s items=%s", cliSection, items);

            final Section newData = new Section(oldData.getName(), ts);

            // Fire vetoable event (don't need moved items yet)
            var vetoEvent = new SectionChangedEvent(ChordLeadSheetImpl.this, cliSection, oldData, newData, Collections.emptyList());
            fireSynchronizedVetoableChangeEvent(new ClsVetoableChangeEvent(this, vetoEvent));       // throws UnsupportedEditException


            // First adjust items if required
            final List<ItemMoved> adjustments;
            final List<ChordLeadSheetItem<?>> adjustedItems;
            if (ts.getNbNaturalBeats() < oldData.getTimeSignature().getNbNaturalBeats())
            {
                var iitems = getItems(cliSection, ChordLeadSheetItem.class, cli -> !ts.checkBeat(cli.getPosition().getBeat()));
                adjustments = adjustItemsToTimeSignature(oldData.getTimeSignature(), ts, iitems);
                adjustedItems = adjustments.stream()
                        .map(ItemMoved::item)
                        .toList();
            } else
            {
                adjustments = Collections.emptyList();
                adjustedItems = Collections.emptyList();
            }


            // Update section
            changeItemDataChecked(cliSection, newData);


            // Prepare event 
            UndoableEdit edit = new SimpleEdit("Set Section Time Signature " + ts)
            {
                @Override
                public void undoBody()
                {
                    LOGGER.log(Level.FINER, "setSectionTimeSignature.undoBody() cliSection={0}", cliSection);
                    performAPImethodUndoRedo(() -> 
                    {
                        changeItemDataChecked(cliSection, oldData);

                        for (var adj : adjustments.reversed())
                        {
                            changeItemPositionChecked(adj.item(), adj.oldPos());
                        }

                        var event = new SectionChangedEvent(ChordLeadSheetImpl.this, cliSection, oldData, newData, adjustedItems);
                        event.setIsUndo();
                        return event;
                    });
                }

                @Override
                public void redoBody()
                {
                    LOGGER.log(Level.FINER, "setSectionTimeSignature.redoBody() cliSection={0}", cliSection);
                    performAPImethodUndoRedo(() -> 
                    {
                        for (var adj : adjustments)
                        {
                            changeItemPositionChecked(adj.item(), adj.newPos());
                        }

                        changeItemDataChecked(cliSection, newData);

                        var event = new SectionChangedEvent(ChordLeadSheetImpl.this, cliSection, oldData, newData, adjustedItems);
                        event.setIsRedo();
                        return event;
                    });
                }
            };

            var event = new SectionChangedEvent(ChordLeadSheetImpl.this, cliSection, oldData, newData, adjustedItems);
            return new OperationResults(event, edit, null);
        };

        performAPImethodThrowing(operation);         // throws UnsupportedEditException

    }

    @Override
    public List<ChordLeadSheetItem> getItems()
    {
        lock.readLock().lock();
        try
        {
            return List.copyOf(items);
        } finally
        {
            lock.readLock().unlock();
        }
    }

    @Override
    public <T extends ChordLeadSheetItem<?>> T getFirstItemAfter(Position posFrom, boolean inclusiveFrom, Class<T> itemClass, Predicate<T> tester)
    {
        var itemFrom = ChordLeadSheetItem.createItemFrom(posFrom, inclusiveFrom);
        return getFirstItemAfter(itemFrom, itemClass, tester);
    }

    @Override
    public <T extends ChordLeadSheetItem<?>> T getFirstItemAfter(ChordLeadSheetItem<?> cli, Class<T> itemClass, Predicate<T> tester)
    {
        Preconditions.checkNotNull(cli);
        Preconditions.checkNotNull(tester);
        Preconditions.checkNotNull(itemClass);

        T res = null;

        lock.readLock().lock();
        try
        {
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
        } finally
        {
            lock.readLock().unlock();
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
    public <T extends ChordLeadSheetItem<?>> T getLastItemBefore(ChordLeadSheetItem<?> cli, Class<T> itemClass, Predicate<T> tester)
    {
        Preconditions.checkNotNull(cli);
        Preconditions.checkNotNull(tester);
        Preconditions.checkNotNull(itemClass);
        T res = null;

        lock.readLock().lock();
        try
        {
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
        } finally
        {
            lock.readLock().unlock();
        }

        return res;
    }

    @Override
    public <T extends ChordLeadSheetItem<?>> List<T> getItems(Position posFrom, boolean inclusiveFrom, Position posTo, boolean inclusiveTo,
            Class<T> itemClass,
            Predicate<T> tester)
    {
        Preconditions.checkNotNull(posFrom);
        Preconditions.checkNotNull(posTo);
        Preconditions.checkNotNull(tester);
        Preconditions.checkNotNull(itemClass);

        List<T> res;

        lock.readLock().lock();
        try
        {
            var rangeItems = items.subSet(
                    ChordLeadSheetItem.createItemFrom(posFrom, inclusiveFrom),
                    false,
                    ChordLeadSheetItem.createItemTo(posTo, inclusiveTo),
                    false);

            res = rangeItems.stream()
                    .filter(item -> itemClass.isAssignableFrom(item.getClass()))
                    .map(cli -> (T) cli)
                    .filter(tester)
                    .toList();
        } finally
        {
            lock.readLock().unlock();
        }

        return res;
    }

    @Override
    public void addUndoableEditListener(UndoableEditListener l)
    {
        Objects.requireNonNull(l);
        undoListeners.remove(l);
        undoListeners.add(l);
    }

    @Override
    public void removeUndoableEditListener(UndoableEditListener l)
    {
        Objects.requireNonNull(l);
        undoListeners.remove(l);
    }

    @Override
    public void addClsChangeListener(ClsChangeListener l)
    {
        Objects.requireNonNull(l);
        listeners.remove(l);
        listeners.add(l);
    }

    @Override
    public void removeClsChangeListener(ClsChangeListener l)
    {
        listeners.remove(l);
    }

    @Override
    public void addClsChangeSyncListener(ClsChangeListener l)
    {
        Objects.requireNonNull(l);
        syncListeners.remove(l);
        syncListeners.add(l);
    }

    @Override
    public void removeClsChangeSyncListener(ClsChangeListener l)
    {
        syncListeners.remove(l);
    }

    @Override
    public String toString()
    {
        return "ChordLeadSheet section0=" + getSection(0).getData().getName() + " size=" + getSizeInBars();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null || getClass() != obj.getClass())
        {
            return false;
        }

        ChordLeadSheetImpl other = (ChordLeadSheetImpl) obj;

        // Snapshot this
        final int thisSize;
        final List<ChordLeadSheetItem> thisItems;
        lock.readLock().lock();
        try
        {
            thisSize = this.size;
            thisItems = List.copyOf(this.items); // preserves order from TreeSet iteration
        } finally
        {
            lock.readLock().unlock();
        }

        // Snapshot other
        final int otherSize;
        final List<ChordLeadSheetItem> otherItems;
        other.lock.readLock().lock();
        try
        {
            otherSize = other.size;
            otherItems = List.copyOf(other.items);
        } finally
        {
            other.lock.readLock().unlock();
        }

        return thisSize == otherSize && thisItems.equals(otherItems);
    }
    // --------------------------------------------------------------------------------------
    // Private methods
    // --------------------------------------------------------------------------------------

    /**
     * Adjust the position of the specified items to fit newTs.
     * <p>
     *
     * @param oldTs
     * @param newTs
     * @param iitems
     * @return The list of moved items (each item stays within its bar)
     */
    @SuppressWarnings("rawtypes")
    private List<ItemMoved> adjustItemsToTimeSignature(TimeSignature oldTs, TimeSignature newTs, List<? extends ChordLeadSheetItem> iitems)
    {
        Objects.requireNonNull(oldTs);
        Objects.requireNonNull(newTs);
        Objects.requireNonNull(iitems);

        LOGGER.log(Level.FINER, "adjustItemsToTimeSignature() -- oldTs={0} newTs={1} iitems={2}", new Object[]
        {
            oldTs, newTs, iitems
        });

        List<ItemMoved> res = new ArrayList<>();

        if (oldTs.getNbNaturalBeats() == newTs.getNbNaturalBeats())
        {
            return res;
        }

        assert lock.isWriteLockedByCurrentThread();

        for (ChordLeadSheetItem<?> item : iitems)
        {
            Position oldPos = item.getPosition();
            Position newPos = oldPos.getConverted(oldTs, newTs);
            if (!newPos.equals(oldPos))
            {
                // Make sure we don't have a collision
                while (items.contains(item.getCopy(null, newPos)))
                {
                    float newBeat = (float) (Math.floor((newPos.getBeat() - 0.00001f) / 0.25f) * 0.25f);     // nearest 0.25 multiple below
                    if (newBeat < 0)
                    {
                        // It can happen theoretically, but it would mean user fully packed the bar with many identical chords, which makes no sense
                        throw new IllegalStateException("oldTs=" + oldTs + " newTs=" + newTs + " item=" + item + " newBeat=" + newBeat + " iitems=" + items);
                    }
                    newPos.setBeat(newBeat);
                }

                changeItemPositionChecked(item, newPos);
                res.add(new ItemMoved(item, oldPos, newPos));
            }
        }
        return res;
    }

    /**
     * Perform the add while checking internal state consistency, then update item's container.
     *
     * @param item
     */
    private void addItemChecked(ChordLeadSheetItem<?> item)
    {
        assert lock.isWriteLockedByCurrentThread();
        Preconditions.checkArgument(item instanceof WritableItem, "item=%s", item);
        var b = items.add(item);
        assert b : "item=" + item + " items=" + items;
        ((WritableItem) item).setContainer(this);
    }

    /**
     * Perform the remove while checking internal state consistency, then set item's container to null.
     *
     * @param item
     */
    private void removeItemChecked(ChordLeadSheetItem<?> item)
    {
        assert lock.isWriteLockedByCurrentThread();
        Preconditions.checkArgument(item instanceof WritableItem, "item=%s", item);
        var b = items.remove(item);
        assert b : "item=" + item + " items=" + items;
        ((WritableItem) item).setContainer(null);
    }

    private <T> void changeItemDataChecked(ChordLeadSheetItem<T> item, T newData)
    {
        assert lock.isWriteLockedByCurrentThread();
        WritableItem<T> wItem = (WritableItem<T>) item;
        var b = items.remove(wItem);
        assert b : "wItem=" + wItem + " newData=" + newData + " items=" + items;
        wItem.setData(newData);
        b = items.add(wItem);
        assert b : "wItem=" + wItem + " newData=" + newData + " items=" + items;
    }

    private <T> void changeItemPositionChecked(ChordLeadSheetItem<T> item, Position newPos)
    {
        assert lock.isWriteLockedByCurrentThread();
        WritableItem<T> wItem = (WritableItem<T>) item;
        var b = items.remove(wItem);
        assert b : "wItem=" + wItem + " newPos=" + newPos + " items=" + items;
        wItem.setPosition(newPos);
        b = items.add(wItem);
        assert b : "wItem=" + wItem + " newPos=" + newPos + " items=" + items;
    }

    /**
     * Fire a vetoable change event to all synchronized listeners.
     *
     * @param event
     * @throws org.jjazz.chordleadsheet.api.UnsupportedEditException
     */
    private void fireSynchronizedVetoableChangeEvent(ClsVetoableChangeEvent event) throws UnsupportedEditException
    {
        Objects.requireNonNull(event);
        for (var l : syncListeners)
        {
            l.chordLeadSheetChanged(event);         // throws UnsupportedEditException
        }
    }

    /**
     * Fire a non vetoable change event to all (non synchronized) listeners.
     * <p>
     *
     * @param event Can not be a ClsVetoableChangeEvent
     */
    private void fireNonVetoableChangeEvent(ClsChangeEvent event)
    {
        Preconditions.checkArgument(event != null && !(event instanceof ClsVetoableChangeEvent), "event=%s", event);

        for (ClsChangeListener l : listeners)
        {
            try
            {
                l.chordLeadSheetChanged(event);      // throws UnsupportedEditException
            } catch (UnsupportedEditException ex)
            {
                // Should never happen since non vetoable
                Exceptions.printStackTrace(ex);
            }
        }
    }

    /**
     * Fire a non vetoable change event to all synchronized listeners.
     * <p>
     *
     * @param event
     */
    private void fireSynchronizedNonVetoableChangeEvent(ClsChangeEvent event)
    {
        Preconditions.checkArgument(event != null && !(event instanceof ClsVetoableChangeEvent), "event=%s", event);

        for (ClsChangeListener l : syncListeners)
        {
            try
            {
                l.chordLeadSheetChanged(event);      // throws UnsupportedEditException
            } catch (UnsupportedEditException ex)
            {
                // Should never happen since non vetoable
                Exceptions.printStackTrace(ex);
            }
        }
    }

    private void fireUndoableEditHappened(UndoableEdit edit)
    {
        Objects.requireNonNull(edit);
        UndoableEditEvent event = new UndoableEditEvent(this, edit);
        for (UndoableEditListener l : undoListeners)
        {
            l.undoableEditHappened(event);
        }
    }

    /**
     * Safely perform a mutating API method possibly returning a value.
     *
     * @param <R>       The type of the return value
     * @param operation Updates the model and returns the events
     * @return The returnValue from OperationResults. Can be null.
     */
    private <R> R performAPImethod(Supplier<OperationResults> operation)
    {
        OperationResults results = null;

        lock.writeLock().lock();
        try
        {
            results = operation.get();
            assert results != null;

            if (results.undoableEdit() != null)
            {
                fireUndoableEditHappened(results.undoableEdit());
            }
            if (results.clsChangeEvent() != null)
            {
                fireSynchronizedNonVetoableChangeEvent(results.clsChangeEvent());
            }
        } finally
        {
            lock.writeLock().unlock();
        }

        if (results.clsChangeEvent() != null)
        {
            fireNonVetoableChangeEvent(results.clsChangeEvent());
        }

        @SuppressWarnings("unchecked")
        R returnValue = (R) results.returnValue();
        return returnValue;
    }

    /**
     * Safely perform a mutating API method possibly returning a value or throwing an exception
     *
     * @param operation Updates the model and returns the events
     * @return The returnValue from OperationResults. Can be null.
     * @param <R>
     * @param <E>
     * @throws E
     */
    private <R, E extends Exception> R performAPImethodThrowing(ThrowingSupplier<OperationResults, E> operation) throws E
    {
        OperationResults results;

        lock.writeLock().lock();
        try
        {
            results = operation.get();           // may throw E
            assert results != null;

            if (results.undoableEdit() != null)
            {
                fireUndoableEditHappened(results.undoableEdit());
            }
            if (results.clsChangeEvent() != null)
            {
                fireSynchronizedNonVetoableChangeEvent(results.clsChangeEvent());
            }
        } finally
        {
            lock.writeLock().unlock();
        }

        if (results.clsChangeEvent() != null)
        {
            fireNonVetoableChangeEvent(results.clsChangeEvent());
        }

        @SuppressWarnings("unchecked")
        R returnValue = (R) results.returnValue();
        return returnValue;
    }

    /**
     * Safely perform an undo or redo operation for mutating API method.
     *
     * @param operation Updates model and return a ClsChangeEvent
     */
    private void performAPImethodUndoRedo(Supplier<ClsChangeEvent> operation)
    {
        ClsChangeEvent event;
        lock.writeLock().lock();
        try
        {
            event = operation.get();
            assert event != null;
            fireSynchronizedNonVetoableChangeEvent(event);
        } finally
        {
            lock.writeLock().unlock();
        }
        fireNonVetoableChangeEvent(event);
    }

    // ==========================================================================================================================
    // Inner classes
    // ==========================================================================================================================    
    @FunctionalInterface
    private interface ThrowingSupplier<T, E extends Exception>
    {

        T get() throws E;
    }

    /**
     * Helper class to store the events and return value produced by a mutating API method.
     */
    private record OperationResults(ClsChangeEvent clsChangeEvent, UndoableEdit undoableEdit, Object returnValue)
            {

    }

    /**
     * Helper class to store a moved item.
     */
    private record ItemMoved(ChordLeadSheetItem<?> item, Position oldPos, Position newPos)
            {

        ItemMoved
        {
            Objects.requireNonNull(item);
            Objects.requireNonNull(oldPos);
            Objects.requireNonNull(newPos);
            Preconditions.checkArgument(!oldPos.equals(newPos), "this=%s", this);
        }
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
                default ->
                    throw new AssertionError(instanceId.name());
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

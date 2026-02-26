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
import java.beans.PropertyChangeEvent;
import org.jjazz.undomanager.api.SimpleEdit;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
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
import org.jjazz.song.ExecutionManager;
import org.jjazz.song.ThrowingWriteOperation;
import org.jjazz.song.WriteOperation;
import org.jjazz.song.WriteOperationResults;
import org.jjazz.song.api.Song;
import org.jjazz.utilities.api.ThrowingSupplier;
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
    private transient Song song;
    private ExecutionManager executionManager;
    /**
     * The listeners for changes in this LeadSheet.
     */
    protected final transient CopyOnWriteArrayList<ClsChangeListener> listeners;
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

        this.listeners = new CopyOnWriteArrayList<>();
        this.undoListeners = new CopyOnWriteArrayList<>();
        this.items = new TreeSet<>();
        this.size = size;
        this.executionManager = new ExecutionManager();

        items.add(initSection);
    }

    @Override
    public ChordLeadSheet getDeepCopy()
    {

        var res = performReadAPImethod(() -> 
        {
            var initSection = getSection(0);
            assert initSection != null;

            var clsCopy = new ChordLeadSheetImpl(initSection.getData().getName(), initSection.getData().getTimeSignature(), size);

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

            return clsCopy;
        });


        return res;
    }

    public void setExecutionManager(ExecutionManager em)
    {
        Objects.requireNonNull(em);
        executionManager = em;
    }

    public ExecutionManager getExecutionManager()
    {
        return executionManager;
    }

    @Override
    public void setSizeInBars(final int newSize)
    {
        performWriteAPImethod(setSizeInBarsOperation(newSize));
    }


    public WriteOperation setSizeInBarsOperation(final int newSize)
    {
        Preconditions.checkArgument(newSize >= 1 && newSize <= MAX_SIZE, "newSize=%s", newSize);


        WriteOperation operation = () -> 
        {
            LOGGER.log(Level.FINE, "setSizeInBars() -- newSize={0}", newSize);

            final int oldSize = size;
            if (newSize == oldSize)
            {
                return WriteOperationResults.of(null);
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
                    performWriteAPImethod(() -> 
                    {
                        size = oldSize;
                        itemsToRemove.forEach(item -> addItemChecked(item));

                        var event = new SizeChangedEvent(ChordLeadSheetImpl.this, oldSize, newSize, itemsToRemove);
                        event.setIsUndo();
                        return WriteOperationResults.of(event, null);
                    });
                }

                @Override
                public void redoBody()
                {
                    LOGGER.log(Level.FINER, "setSizeInBars.redoBody() newSize={0}", newSize);
                    performWriteAPImethod(() -> 
                    {
                        itemsToRemove.forEach(item -> removeItemChecked(item));
                        size = newSize;

                        var event = new SizeChangedEvent(ChordLeadSheetImpl.this, oldSize, newSize, itemsToRemove);
                        event.setIsRedo();
                        return WriteOperationResults.of(event, null);
                    });
                }
            };

            fireUndoableEdit(edit);

            var event = new SizeChangedEvent(ChordLeadSheetImpl.this, oldSize, newSize, itemsToRemove);
            return WriteOperationResults.of(event, null);
        };

        return operation;
    }

    @Override
    public int getSizeInBars()
    {
        return performReadAPImethod(() -> size);
    }

    @Override
    public boolean addItem(ChordLeadSheetItem<?> item)
    {
        return performWriteAPImethod(addItemOperation(item));
    }

    public WriteOperation<Boolean> addItemOperation(ChordLeadSheetItem<?> item)
    {
        Objects.requireNonNull(item);
        Preconditions.checkArgument(item instanceof WritableItem, "item=%s", item);
        Preconditions.checkArgument(!(item instanceof CLI_Section), "item=%s", item);


        WriteOperation<Boolean> operation = () -> 
        {
            LOGGER.log(Level.FINE, "addItemOperation() -- item={0}", item);


            final ChordLeadSheet oldContainer = item.getContainer();
            final Position oldPos = item.getPosition();
            int barIndex = oldPos.getBar();
            Preconditions.checkArgument(barIndex < getSizeInBars(), "oldPos=%s size=%s", oldPos, size);
            final Position newAdjustedPos = oldPos.getAdjusted(getSection(barIndex).getData().getTimeSignature());


            // Can't add two items at the same location
            if (items.contains(item.getCopy(null, newAdjustedPos)))
            {
                LOGGER.log(Level.FINE, "addItem() item already present. item={0}", item);
                return WriteOperationResults.of(Boolean.FALSE);
            }


            // Update model
            final WritableItem<?> wItem = (WritableItem<?>) item;
            List<PropertyChangeEvent> cliEvents = new ArrayList<>();

            cliEvents.add(wItem.setPosition(newAdjustedPos));
            cliEvents.add(addItemChecked(wItem));


            // Create the undoable event
            UndoableEdit edit = new SimpleEdit("Add " + wItem)
            {
                @Override
                public void undoBody()
                {
                    LOGGER.log(Level.FINER, "addItem.undoBody() item={0}", item);
                    performWriteAPImethod(() -> 
                    {
                        List<PropertyChangeEvent> cliEvents2 = new ArrayList<>();
                        removeItemChecked(wItem);
                        cliEvents2.add(wItem.setPosition(oldPos));
                        cliEvents2.add(wItem.setContainer(oldContainer));

                        var event = new ItemAddedEvent(ChordLeadSheetImpl.this, wItem);
                        event.setIsUndo();
                        event.addItemChanges(cliEvents2);
                        return WriteOperationResults.of(event, Boolean.TRUE);
                    });

                }

                @Override
                public void redoBody()
                {
                    LOGGER.log(Level.FINER, "addItem.redoBody() item={0}", item);
                    performWriteAPImethod(() -> 
                    {
                        List<PropertyChangeEvent> cliEvents2 = new ArrayList<>();
                        cliEvents2.add(wItem.setPosition(newAdjustedPos));
                        cliEvents2.add(addItemChecked(wItem));

                        var event = new ItemAddedEvent(ChordLeadSheetImpl.this, wItem);
                        event.setIsRedo();
                        event.addItemChanges(cliEvents2);
                        return WriteOperationResults.of(event, Boolean.TRUE);
                    });
                }
            };

            fireUndoableEdit(edit);

            var event = new ItemAddedEvent(ChordLeadSheetImpl.this, wItem);
            event.addItemChanges(cliEvents);
            return WriteOperationResults.of(event, Boolean.TRUE);
        };

        return operation;
    }

    @Override
    public void addSection(final CLI_Section cliSection) throws UnsupportedEditException
    {
        performWriteAPImethodThrowing(addSectionOperation(cliSection));
    }

    public ThrowingWriteOperation addSectionOperation(final CLI_Section cliSection) throws UnsupportedEditException
    {
        Objects.requireNonNull(cliSection);
        Preconditions.checkArgument(cliSection instanceof WritableItem, "cliSection=%s", cliSection);


        // Operation
        ThrowingWriteOperation operation = () -> 
        {

            // Verify constraints
            final int bar = cliSection.getPosition().getBar();
            Preconditions.checkArgument(bar < this.size, "cliSection=%s this.size=%s", cliSection, size);

            final CLI_Section curSection = getSection(bar);
            var sameNameSection = getSection(cliSection.getData().getName());
            Preconditions.checkArgument(sameNameSection == null || sameNameSection.getPosition().getBar() == bar,
                    "cliSection=%s sameNameSection=%s", cliSection, sameNameSection);


            LOGGER.log(Level.FINE, "addSectionOperation() -- cliSection={0}", cliSection);


            // Pre-check change
            var preCheckEvent = new SectionAddedEvent(ChordLeadSheetImpl.this,
                    cliSection,
                    curSection.getPosition().getBar() == bar ? curSection : null,
                    Collections.emptyList());
            preCheckChange(preCheckEvent);          // throws UnsupportedEditException


            // Prepare data
            final TimeSignature oldTs = curSection.getData().getTimeSignature();
            final boolean isReplace = curSection.getPosition().getBar() == bar;
            final WritableItem<Section> wSection = (WritableItem<Section>) cliSection;
            final ChordLeadSheet oldContainer = wSection.getContainer();
            List<PropertyChangeEvent> cliEvents = new ArrayList<>();

            // Update model
            if (isReplace)
            {
                cliEvents.add(removeItemChecked(curSection));
            }

            cliEvents.add(addItemChecked(cliSection));


            // Possibly adjust items if time signature change
            final TimeSignature newTs = cliSection.getData().getTimeSignature();
            final List<ItemMoved> adjustments = new ArrayList<>();
            if (newTs.getNbNaturalBeats() < oldTs.getNbNaturalBeats())
            {
                var iitems = getItems(cliSection, ChordLeadSheetItem.class, cli -> !newTs.checkBeat(cli.getPosition().getBeat()));
                adjustments.addAll(adjustItemsToTimeSignature(oldTs, newTs, iitems));
            }
            final List<ChordLeadSheetItem> adjustedItems = adjustments.stream().map(ItemMoved::item).toList();
            adjustments.forEach(im -> cliEvents.add(im.getPositionChangedEvent()));


            // Create events
            UndoableEdit edit = new SimpleEdit("Add Section " + wSection)
            {
                @Override
                public void undoBody()
                {
                    LOGGER.log(Level.FINER, "addSection.undoBody() section={0}", wSection);
                    performWriteAPImethod(() -> 
                    {
                        List<PropertyChangeEvent> cliEvents2 = new ArrayList<>();
                        for (var adjustment : adjustments.reversed())
                        {
                            cliEvents2.add(changeItemPositionChecked(adjustment.item(), adjustment.oldPos()));
                        }

                        cliEvents2.add(removeItemChecked(wSection));
                        cliEvents2.add(wSection.setContainer(oldContainer));

                        if (isReplace)
                        {
                            cliEvents2.add(addItemChecked(curSection));
                        }

                        var event = new SectionAddedEvent(ChordLeadSheetImpl.this, cliSection, isReplace ? curSection : null, adjustedItems);
                        event.setIsUndo();
                        event.addItemChanges(cliEvents2);
                        return WriteOperationResults.of(event, null);
                    });
                }

                @Override
                public void redoBody()
                {
                    LOGGER.log(Level.FINER, "addSection.redoBody() section={0}", wSection);
                    performWriteAPImethod(() -> 
                    {
                        List<PropertyChangeEvent> cliEvents2 = new ArrayList<>();
                        if (isReplace)
                        {
                            cliEvents2.add(removeItemChecked(curSection));
                        }

                        cliEvents2.add(addItemChecked(wSection));

                        for (var adjustment : adjustments)
                        {
                            cliEvents2.add(changeItemPositionChecked(adjustment.item(), adjustment.newPos()));
                        }

                        var event = new SectionAddedEvent(ChordLeadSheetImpl.this, cliSection, isReplace ? curSection : null, adjustedItems);
                        event.setIsRedo();
                        event.addItemChanges(cliEvents2);
                        return WriteOperationResults.of(event, null);
                    });
                }
            };

            fireUndoableEdit(edit);

            var event = new SectionAddedEvent(ChordLeadSheetImpl.this, cliSection, isReplace ? curSection : null, adjustedItems);
            event.addItemChanges(cliEvents);
            return WriteOperationResults.of(event, null);
        };


        return operation;

    }

    @Override
    public void removeSection(final CLI_Section cliSection)
    {
        performWriteAPImethod(removeSectionOperation(cliSection));
    }

    public WriteOperation removeSectionOperation(final CLI_Section cliSection)
    {
        Objects.requireNonNull(cliSection);
        Preconditions.checkArgument(cliSection.getPosition().getBar() != 0, "cliSection=%s", cliSection);


        // Operation
        WriteOperation operation = () -> 
        {
            Preconditions.checkArgument(items.contains(cliSection), "cliSection=%s items=%s", cliSection, items);

            LOGGER.log(Level.FINE, "removeSectionOperation() -- cliSection={0}", cliSection);


            // Context: The section previous to the one being removed will take over the scope
            final int bar = cliSection.getPosition().getBar();
            final CLI_Section prevSection = getSection(bar - 1);
            final TimeSignature newTs = prevSection.getData().getTimeSignature();
            final TimeSignature oldTs = cliSection.getData().getTimeSignature();
            List<PropertyChangeEvent> cliEvents = new ArrayList<>();


            // Update model
            cliEvents.add(removeItemChecked(cliSection));


            // Adjust items position if time signature change
            final List<ItemMoved> adjustments = new ArrayList<>();
            final List<ChordLeadSheetItem> adjustedItems = new ArrayList<>();
            if (newTs.getNbNaturalBeats() < oldTs.getNbNaturalBeats())
            {
                var iitems = getItems(prevSection, ChordLeadSheetItem.class, cli -> !newTs.checkBeat(cli.getPosition().getBeat()));
                adjustments.addAll(adjustItemsToTimeSignature(oldTs, newTs, iitems));
                adjustedItems.addAll(adjustments.stream().map(ItemMoved::item).toList());
            }
            adjustments.forEach(im -> cliEvents.add(im.getPositionChangedEvent()));


            // Create events
            UndoableEdit edit = new SimpleEdit("Remove Section " + cliSection)
            {
                @Override
                public void undoBody()
                {
                    LOGGER.log(Level.FINER, "removeSection.undoBody() section={0}", cliSection);
                    performWriteAPImethod(() -> 
                    {
                        List<PropertyChangeEvent> cliEvents2 = new ArrayList<>();

                        for (var adjustment : adjustments.reversed())
                        {
                            cliEvents2.add(changeItemPositionChecked(adjustment.item(), adjustment.oldPos()));
                        }

                        cliEvents2.add(addItemChecked(cliSection));

                        var event = new SectionRemovedEvent(ChordLeadSheetImpl.this, cliSection, adjustedItems);
                        event.setIsUndo();
                        event.addItemChanges(cliEvents2);
                        return WriteOperationResults.of(event, null);
                    });
                }

                @Override
                public void redoBody()
                {
                    LOGGER.log(Level.FINER, "removeSection.redoBody() section={0}", cliSection);
                    performWriteAPImethod(() -> 
                    {
                        List<PropertyChangeEvent> cliEvents2 = new ArrayList<>();

                        cliEvents2.add(removeItemChecked(cliSection));

                        for (var adjustment : adjustments)
                        {
                            cliEvents2.add(changeItemPositionChecked(adjustment.item(), adjustment.newPos()));
                        }

                        var event = new SectionRemovedEvent(ChordLeadSheetImpl.this, cliSection, adjustedItems);
                        event.addItemChanges(cliEvents2);
                        return WriteOperationResults.of(event, null);
                    });
                }
            };

            fireUndoableEdit(edit);

            var event = new SectionRemovedEvent(ChordLeadSheetImpl.this, cliSection, adjustedItems);
            event.addItemChanges(cliEvents);
            return WriteOperationResults.of(event, null);
        };

        return operation;
    }

    @Override
    public void moveSection(final CLI_Section cliSection, final int newBarIndex)
    {
        performWriteAPImethod(moveSectionOperation(cliSection, newBarIndex));
    }

    public WriteOperation moveSectionOperation(final CLI_Section cliSection, final int newBarIndex)
    {
        Preconditions.checkArgument(cliSection instanceof WritableItem && cliSection.getPosition().getBar() != 0, "cliSection=%s", cliSection);
        Preconditions.checkArgument(newBarIndex > 0, "newBarIndex=%s", newBarIndex);


        WriteOperation operation = () -> 
        {
            // Preconditions and simple cases
            Preconditions.checkArgument(newBarIndex < size, "newBarIndex=%s size=%s", newBarIndex, size);
            Preconditions.checkArgument(items.contains(cliSection), "cliSection=%s items=%s", cliSection, items);

            LOGGER.log(Level.FINE, "moveSection() -- cliSection={0} newBarIndex={1}", new Object[]
            {
                cliSection, newBarIndex
            });

            final int oldBarIndex = cliSection.getPosition().getBar();
            if (newBarIndex == oldBarIndex)
            {
                return WriteOperationResults.of(null);
            }

            CLI_Section newPosOldSection = getSection(newBarIndex);
            var newPosOldTs = newPosOldSection.getData().getTimeSignature();
            if (newPosOldSection.getPosition().getBar() == newBarIndex)
            {
                throw new IllegalArgumentException("There is already a section at destination bar " + newBarIndex);
            }
            List<PropertyChangeEvent> cliEvents = new ArrayList<>();


            // Update model
            cliEvents.add(changeItemPositionChecked(cliSection, new Position(newBarIndex)));


            // Adjust items impacted by possible time signature change
            CLI_Section oldPosNewSection = getSection(oldBarIndex);
            var oldPosNewTs = oldPosNewSection.getData().getTimeSignature();
            var ts = cliSection.getData().getTimeSignature();
            final List<ItemMoved> adjustments = new ArrayList<>();
            if (ts.getNbNaturalBeats() < newPosOldTs.getNbNaturalBeats())
            {
                var iitems = getItems(cliSection, ChordLeadSheetItem.class, cli -> !ts.checkBeat(cli.getPosition().getBeat()));
                adjustments.addAll(adjustItemsToTimeSignature(newPosOldTs, ts, iitems));
            }
            if (oldPosNewTs.getNbNaturalBeats() < ts.getNbNaturalBeats())
            {
                var iitems = getItems(oldPosNewSection, ChordLeadSheetItem.class, cli -> !oldPosNewTs.checkBeat(cli.getPosition().getBeat()));
                adjustments.addAll(adjustItemsToTimeSignature(ts, oldPosNewTs, iitems));
            }
            final List<ChordLeadSheetItem> adjustedItems = adjustments.stream().map(ItemMoved::item).toList();
            adjustments.forEach(im -> cliEvents.add(im.getPositionChangedEvent()));


            // Events
            UndoableEdit edit = new SimpleEdit("Move Section " + cliSection)
            {
                @Override
                public void undoBody()
                {
                    LOGGER.log(Level.FINER, "moveSection.undoBody() section={0}", cliSection);
                    performWriteAPImethod(() -> 
                    {
                        List<PropertyChangeEvent> cliEvents2 = new ArrayList<>();

                        for (var adjustment : adjustments.reversed())
                        {
                            cliEvents2.add(changeItemPositionChecked(adjustment.item(), adjustment.oldPos()));
                        }

                        cliEvents2.add(changeItemPositionChecked(cliSection, new Position(oldBarIndex)));

                        var event = new SectionMovedEvent(ChordLeadSheetImpl.this, cliSection, oldBarIndex, newBarIndex, adjustedItems);
                        event.setIsUndo();
                        event.addItemChanges(cliEvents2);
                        return WriteOperationResults.of(event, null);
                    });
                }

                @Override
                public void redoBody()
                {
                    LOGGER.log(Level.FINER, "moveSection.redoBody() section={0}", cliSection);
                    performWriteAPImethod(() -> 
                    {
                        List<PropertyChangeEvent> cliEvents2 = new ArrayList<>();

                        cliEvents2.add(changeItemPositionChecked(cliSection, new Position(newBarIndex)));

                        for (var adjustment : adjustments)
                        {
                            cliEvents2.add(changeItemPositionChecked(adjustment.item(), adjustment.newPos()));
                        }

                        var event = new SectionMovedEvent(ChordLeadSheetImpl.this, cliSection, oldBarIndex, newBarIndex, adjustedItems);
                        event.setIsRedo();
                        event.addItemChanges(cliEvents2);
                        return WriteOperationResults.of(event, null);
                    });
                }
            };

            fireUndoableEdit(edit);

            var event = new SectionMovedEvent(ChordLeadSheetImpl.this, cliSection, oldBarIndex, newBarIndex, adjustedItems);
            event.addItemChanges(cliEvents);
            return WriteOperationResults.of(event, null);
        };

        return operation;
    }

    @Override
    public boolean removeItem(final ChordLeadSheetItem<?> item)
    {
        return performWriteAPImethod(removeItemOperation(item));
    }

    public WriteOperation<Boolean> removeItemOperation(final ChordLeadSheetItem<?> item)
    {
        Objects.requireNonNull(item);
        Preconditions.checkArgument(!(item instanceof CLI_Section), "item=%s", item);


        WriteOperation<Boolean> operation = () -> 
        {
            LOGGER.log(Level.FINE, "removeItem() -- item={0}", item);

            if (!items.contains(item))
            {
                LOGGER.log(Level.FINE, "addItem() item not present. item={0}", item);
                return WriteOperationResults.of(Boolean.FALSE);
            }


            // Change state
            final PropertyChangeEvent itemEvent = removeItemChecked(item);


            // Create the undoable event
            UndoableEdit edit = new SimpleEdit("Remove " + item)
            {
                @Override
                public void undoBody()
                {
                    LOGGER.log(Level.FINER, "removeItem.undoBody() item={0}", item);
                    performWriteAPImethod(() -> 
                    {
                        PropertyChangeEvent itemEvent2 = addItemChecked(item);

                        var event = new ItemRemovedEvent(ChordLeadSheetImpl.this, item);
                        event.setIsUndo();
                        event.addItemChanges(List.of(itemEvent2));
                        return WriteOperationResults.of(event, Boolean.TRUE);
                    });
                }

                @Override
                public void redoBody()
                {
                    LOGGER.log(Level.FINER, "removeItem.redoBody() item={0}", item);
                    performWriteAPImethod(() -> 
                    {
                        PropertyChangeEvent itemEvent2 = removeItemChecked(item);

                        var event = new ItemRemovedEvent(ChordLeadSheetImpl.this, item);
                        event.setIsRedo();
                        event.addItemChanges(List.of(itemEvent2));
                        return WriteOperationResults.of(event, Boolean.TRUE);

                    });
                }
            };

            fireUndoableEdit(edit);

            var event = new ItemRemovedEvent(ChordLeadSheetImpl.this, item);
            event.addItemChanges(List.of(itemEvent));
            return WriteOperationResults.of(event, Boolean.TRUE);

        };

        return operation;
    }

    @Override
    public boolean moveItem(ChordLeadSheetItem<?> item, Position newPos)
    {
        return performWriteAPImethod(moveItemOperation(item, newPos));
    }

    public WriteOperation<Boolean> moveItemOperation(ChordLeadSheetItem<?> item, Position newPos)
    {
        Objects.requireNonNull(newPos);
        Preconditions.checkArgument(item instanceof WritableItem, "item=%s", item);
        Preconditions.checkArgument(!(item instanceof CLI_Section), "item=%s", item);


        WriteOperation<Boolean> operation = () -> 
        {
            Preconditions.checkArgument(items.contains(item), "item=%s items=%s", item, items);

            LOGGER.log(Level.FINE, "moveItemOperation() -- item={0} newPos={1}", new Object[]
            {
                item, newPos
            });

            final Position oldPos = item.getPosition();
            final Position newAdjustedPos = newPos.getAdjusted(getSection(newPos.getBar()).getData().getTimeSignature());

            if (oldPos.equals(newAdjustedPos) || items.contains(item.getCopy(null, newAdjustedPos)))
            {
                return WriteOperationResults.of(Boolean.FALSE);
            }

            // Change state
            final PropertyChangeEvent itemEvent = changeItemPositionChecked(item, newAdjustedPos);

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
                    performWriteAPImethod(() -> 
                    {
                        PropertyChangeEvent itemEvent2 = changeItemPositionChecked(item, oldPos);

                        var event = new ItemMovedEvent(ChordLeadSheetImpl.this, item, oldPos, newAdjustedPos);
                        event.setIsUndo();
                        event.addItemChanges(List.of(itemEvent2));
                        return WriteOperationResults.of(event, Boolean.TRUE);
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
                    performWriteAPImethod(() -> 
                    {
                        PropertyChangeEvent itemEvent2 = changeItemPositionChecked(item, newAdjustedPos);

                        var event = new ItemMovedEvent(ChordLeadSheetImpl.this, item, oldPos, newAdjustedPos);
                        event.setIsRedo();
                        event.addItemChanges(List.of(itemEvent2));
                        return WriteOperationResults.of(event, Boolean.TRUE);
                    });
                }
            };

            fireUndoableEdit(edit);

            var event = new ItemMovedEvent(ChordLeadSheetImpl.this, item, oldPos, newAdjustedPos);
            event.addItemChanges(List.of(itemEvent));
            return WriteOperationResults.of(event, Boolean.TRUE);
        };

        return operation;
    }

    @Override
    public <T> boolean changeItem(ChordLeadSheetItem<T> item, final T newData)
    {
        return performWriteAPImethod(changeItemOperation(item, newData));
    }

    public <T> WriteOperation<Boolean> changeItemOperation(ChordLeadSheetItem<T> item, final T newData)
    {
        Preconditions.checkArgument(item instanceof WritableItem, "item=%s", item);
        Preconditions.checkArgument(!(item instanceof CLI_Section), "item=%s", item);


        WriteOperation<Boolean> operation = () -> 
        {
            Preconditions.checkArgument(items.contains(item), "item=%s items=%s", item, items);

            LOGGER.log(Level.FINE, "changeItemOperation() -- item={0} newData={1}", new Object[]
            {
                item, newData
            });

            final T oldData = item.getData();
            if (oldData.equals(newData) || items.contains(item.getCopy(newData, null)))
            {
                return WriteOperationResults.of(Boolean.FALSE);
            }

            // Change state
            final PropertyChangeEvent itemEvent = changeItemDataChecked(item, newData);

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
                    performWriteAPImethod(() -> 
                    {
                        final PropertyChangeEvent itemEvent2 = changeItemDataChecked(item, oldData);

                        var event = new ItemChangedEvent(ChordLeadSheetImpl.this, item, oldData, newData);
                        event.setIsUndo();
                        event.addItemChanges(List.of(itemEvent2));
                        return WriteOperationResults.of(event, Boolean.TRUE);
                    });
                }

                @Override
                public void redoBody()
                {
                    LOGGER.log(Level.FINER, "changeItem.redoBody() item={0} oldData={1} newData={2}", new Object[]
                    {
                        item, oldData, newData
                    });
                    performWriteAPImethod(() -> 
                    {
                        final PropertyChangeEvent itemEvent2 = changeItemDataChecked(item, newData);

                        var event = new ItemChangedEvent(ChordLeadSheetImpl.this, item, oldData, newData);
                        event.setIsRedo();
                        event.addItemChanges(List.of(itemEvent2));
                        return WriteOperationResults.of(event, Boolean.TRUE);
                    });
                }
            };

            fireUndoableEdit(edit);

            var event = new ItemChangedEvent(ChordLeadSheetImpl.this, item, oldData, newData);
            event.addItemChanges(List.of(itemEvent));
            return WriteOperationResults.of(event, Boolean.TRUE);
        };

        return operation;
    }

    @Override
    public void insertBars(final int barIndex, final int nbBars)
    {
        performWriteAPImethod(insertBarsOperation(barIndex, nbBars));
    }

    public WriteOperation insertBarsOperation(final int barIndex, final int nbBars)
    {
        Preconditions.checkArgument(barIndex >= 0 && nbBars >= 0, "barIndex=%s nbBars=%s", barIndex, nbBars);


        WriteOperation operation = () -> 
        {
            Preconditions.checkArgument(barIndex <= size, "barIndex=%s size=%s", barIndex, size);

            LOGGER.log(Level.FINE, "insertBarsOperation() -- barIndex={0} nbBars={1}", new Object[]
            {
                barIndex, nbBars
            });

            if (nbBars == 0)
            {
                return WriteOperationResults.of(null);
            }

            final int oldSize = size;
            final int newSize = oldSize + nbBars;
            final List<PropertyChangeEvent> cliEvents = new ArrayList<>();


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
                cliEvents.add(((WritableItem) newSection0).setData(new Section(newName, data0.getTimeSignature())));
            } else
            {
                newSection0 = null;
            }


            // Update model
            size = newSize;

            final List<ChordLeadSheetItem> shiftedItems = getItems(barIndex, Integer.MAX_VALUE, ChordLeadSheetItem.class, cli -> true);
            for (var item : shiftedItems.reversed())
            {
                cliEvents.add(changeItemPositionChecked(item, item.getPosition().getMoved(nbBars, 0)));
            }

            if (newSection0 != null)
            {
                cliEvents.add(addItemChecked(newSection0));
            }


            // Create events
            UndoableEdit edit = new SimpleEdit("Insert Bars " + barIndex + " nb=" + nbBars)
            {
                @Override
                public void undoBody()
                {
                    LOGGER.log(Level.FINER, "insertBars.undoBody() nbBars={0}", nbBars);
                    performWriteAPImethod(() -> 
                    {
                        final List<PropertyChangeEvent> cliEvents2 = new ArrayList<>();

                        if (newSection0 != null)
                        {
                            cliEvents2.add(removeItemChecked(newSection0));
                        }

                        for (var item : shiftedItems)
                        {
                            cliEvents2.add(changeItemPositionChecked(item, item.getPosition().getMoved(-nbBars, 0)));
                        }

                        size = oldSize;

                        var event = new InsertedBarsEvent(ChordLeadSheetImpl.this, barIndex, nbBars, shiftedItems);
                        event.setIsUndo();
                        event.addItemChanges(cliEvents2);
                        return WriteOperationResults.of(event, null);
                    });
                }

                @Override
                public void redoBody()
                {
                    LOGGER.log(Level.FINER, "insertBars.redoBody() nbBars={0}", nbBars);
                    performWriteAPImethod(() -> 
                    {
                        final List<PropertyChangeEvent> cliEvents2 = new ArrayList<>();

                        size = newSize;


                        for (var item : shiftedItems.reversed())
                        {
                            cliEvents2.add(changeItemPositionChecked(item, item.getPosition().getMoved(nbBars, 0)));
                        }

                        if (newSection0 != null)
                        {
                            cliEvents2.add(addItemChecked(newSection0));
                        }

                        var event = new InsertedBarsEvent(ChordLeadSheetImpl.this, barIndex, nbBars, shiftedItems);
                        event.setIsRedo();
                        event.addItemChanges(cliEvents2);
                        return WriteOperationResults.of(event, null);
                    });
                }
            };

            fireUndoableEdit(edit);

            var event = new InsertedBarsEvent(ChordLeadSheetImpl.this, barIndex, nbBars, shiftedItems);
            event.addItemChanges(cliEvents);
            return WriteOperationResults.of(event, null);
        };

        return operation;
    }

    @Override
    public void deleteBars(int barIndexFrom, int barIndexTo)
    {
        performWriteAPImethod(deleteBarsOperation(barIndexFrom, barIndexTo));
    }

    public WriteOperation deleteBarsOperation(int barIndexFrom, int barIndexTo)
    {
        Preconditions.checkArgument(barIndexFrom >= 0 && barIndexTo >= barIndexFrom,
                "barIndexFrom=%s barIndexTo=%s", barIndexFrom, barIndexTo);


        WriteOperation operation = () -> 
        {
            Preconditions.checkArgument(barIndexTo < size && barIndexTo - barIndexFrom + 1 < size, "barIndexFrom=%s barIndexTo=%s size=%s", barIndexFrom,
                    barIndexTo, size);


            LOGGER.log(Level.FINE, "deleteBarsOperation() -- barIndexFrom={0} barIndexTo={1}", new Object[]
            {
                barIndexFrom, barIndexTo
            });


            final int nbBars = barIndexTo - barIndexFrom + 1;
            final int oldSize = size;
            final int newSize = size - nbBars;
            CLI_Section section0 = getSection(0);


            // Determine context *before* mutation at the point just after the cut
            final boolean noBarsAfterCut = barIndexTo + 1 == oldSize;
            final CLI_Section oldSectionAfter = noBarsAfterCut ? null : getSection(barIndexTo + 1);


            // Identify items to remove
            // If we delete from 0 and there is a section immediately after the cut, it replaces the current initSection, then we can remove it
            boolean removeInitSection = barIndexFrom == 0 && oldSectionAfter != null && oldSectionAfter.getPosition().getBar() == barIndexTo + 1;
            final var itemsToRemove = getItems(barIndexFrom, barIndexTo, ChordLeadSheetItem.class, removeInitSection ? cli -> true : cli -> cli != section0);


            // Identify items to shift
            final var itemsToShift = getItems(barIndexTo + 1, Integer.MAX_VALUE, ChordLeadSheetItem.class, cli -> true);      // empty if noBarsAfterCut


            // Find the possible shifted items requiring an additional beat position adjustment because of a time signature change
            List<ChordLeadSheetItem> itemsToAdjust = new ArrayList<>();
            final CLI_Section prevSectionBeforeCut = barIndexFrom == 0 ? null : getSection(barIndexFrom - 1);
            if (prevSectionBeforeCut != null && oldSectionAfter != null && oldSectionAfter.getPosition().getBar() > (barIndexTo + 1)
                    && prevSectionBeforeCut.getData().getTimeSignature().getNbNaturalBeats() < oldSectionAfter.getData().getTimeSignature().getNbNaturalBeats())
            {
                var oldSectionAfterItems = getItems(oldSectionAfter, ChordLeadSheetItem.class);
                itemsToShift.stream()
                        .filter(item -> oldSectionAfterItems.contains(item))
                        .filter(item -> !prevSectionBeforeCut.getData().getTimeSignature().checkBeat(item.getPosition().getBeat()))
                        .forEach(item -> itemsToAdjust.add(item));
            }


            // Update model
            final List<PropertyChangeEvent> cliEvents = new ArrayList<>();
            for (var item : itemsToRemove)
            {
                cliEvents.add(removeItemChecked(item));
            }

            // Shift items, with possible beat adjustment
            for (var item : itemsToShift)
            {
                Position oldPos = item.getPosition();
                Position newPos = oldPos.getMoved(-nbBars, 0);
                if (itemsToAdjust.contains(item))
                {
                    newPos = getAdjustedBeatToTimeSignature(oldSectionAfter.getData().getTimeSignature(),
                            prevSectionBeforeCut.getData().getTimeSignature(),
                            item);
                }
                cliEvents.add(changeItemPositionChecked(item, newPos));
            }

            size = newSize;

            
            // Create events
            UndoableEdit edit = new SimpleEdit("Delete Bars " + barIndexFrom + "-" + barIndexTo)
            {
                @Override
                public void undoBody()
                {
                    LOGGER.log(Level.FINER, "deleteBars.undoBody() nbBars={0}", nbBars);
                    performWriteAPImethod(() -> 
                    {
                        final List<PropertyChangeEvent> cliEvents2 = new ArrayList<>();
                        size = oldSize;

                        for (var item : itemsToShift.reversed())
                        {
                            Position newPos = item.getPosition().getMoved(nbBars, 0);
                            cliEvents2.add(changeItemPositionChecked(item, newPos));
                        }

                        for (var item : itemsToRemove)
                        {
                            cliEvents2.add(addItemChecked(item));
                        }

                        var event = new DeletedBarsEvent(ChordLeadSheetImpl.this, barIndexFrom, barIndexTo, itemsToRemove, itemsToShift);
                        event.setIsUndo();
                        event.addItemChanges(cliEvents2);
                        return WriteOperationResults.of(event, null);
                    });
                }

                @Override
                public void redoBody()
                {
                    LOGGER.log(Level.FINER, "deleteBars.redoBody() nbBars={0}", nbBars);
                    performWriteAPImethod(() -> 
                    {
                        final List<PropertyChangeEvent> cliEvents2 = new ArrayList<>();

                        for (var item : itemsToRemove)
                        {
                            cliEvents2.add(removeItemChecked(item));
                        }

                        for (var item : itemsToShift)
                        {
                            Position newPos = item.getPosition().getMoved(-nbBars, 0);
                            cliEvents2.add(changeItemPositionChecked(item, newPos));
                        }

                        size = newSize;
                        var event = new DeletedBarsEvent(ChordLeadSheetImpl.this, barIndexFrom, barIndexTo, itemsToRemove, itemsToShift);
                        event.setIsRedo();
                        event.addItemChanges(cliEvents2);
                        return WriteOperationResults.of(event, null);

                    });
                }
            };

            fireUndoableEdit(edit);

            var event = new DeletedBarsEvent(ChordLeadSheetImpl.this, barIndexFrom, barIndexTo, itemsToRemove, itemsToShift);
            event.addItemChanges(cliEvents);
            return WriteOperationResults.of(event, null);

        };

        return operation;
    }

    @Override
    public void setSectionName(CLI_Section cliSection, String name)
    {
        performWriteAPImethod(setSectionNameOperation(cliSection, name));
    }

    public WriteOperation setSectionNameOperation(CLI_Section cliSection, String name)
    {
        Preconditions.checkNotNull(cliSection);
        Preconditions.checkNotNull(name);
        Preconditions.checkArgument(cliSection instanceof WritableItem, "cliSection=%s", cliSection);


        WriteOperation operation = () -> 
        {
            Preconditions.checkArgument(items.contains(cliSection), "cliSection=%s items=%s", cliSection, items);
            Preconditions.checkArgument(getSection(name) == null, "cliSection=%s items=%s", cliSection, items);

            LOGGER.log(Level.FINE, "setSectionNameOperation() -- cliSection={0} name={1}", new Object[]
            {
                cliSection, name
            });

            if (cliSection.getData().getName().equals(name))
            {
                return WriteOperationResults.of(null);
            }


            final Section oldData = cliSection.getData();
            final Section newData = new Section(name, oldData.getTimeSignature());


            // Change state
            final PropertyChangeEvent itemEvent = changeItemDataChecked(cliSection, newData);

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
                    performWriteAPImethod(() -> 
                    {
                        PropertyChangeEvent itemEvent2 = changeItemDataChecked(cliSection, oldData);

                        var event = new SectionChangedEvent(ChordLeadSheetImpl.this, cliSection, oldData, newData, Collections.emptyList());
                        event.setIsUndo();
                        event.addItemChanges(List.of(itemEvent2));
                        return WriteOperationResults.of(event, null);
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
                    performWriteAPImethod(() -> 
                    {
                        PropertyChangeEvent itemEvent2 = changeItemDataChecked(cliSection, newData);

                        var event = new SectionChangedEvent(ChordLeadSheetImpl.this, cliSection, oldData, newData, Collections.emptyList());
                        event.setIsRedo();
                        event.addItemChanges(List.of(itemEvent2));
                        return WriteOperationResults.of(event, null);
                    });
                }
            };

            fireUndoableEdit(edit);

            var event = new SectionChangedEvent(ChordLeadSheetImpl.this, cliSection, oldData, newData, Collections.emptyList());
            event.addItemChanges(List.of(itemEvent));
            return WriteOperationResults.of(event, null);
        };

        return operation;
    }

    @Override
    public void setSectionTimeSignature(CLI_Section cliSection, TimeSignature ts) throws UnsupportedEditException
    {
        performWriteAPImethodThrowing(setSectionTimeSignatureOperation(cliSection, ts));
    }

    public ThrowingWriteOperation setSectionTimeSignatureOperation(CLI_Section cliSection, TimeSignature ts) throws UnsupportedEditException
    {
        Preconditions.checkNotNull(ts);
        Preconditions.checkArgument(cliSection instanceof WritableItem, "cliSection=%s", cliSection);


        ThrowingWriteOperation operation = () -> 
        {
            LOGGER.log(Level.FINE, "setSectionTimeSignatureOperation() -- cliSection={0} ts={1}", new Object[]
            {
                cliSection, ts
            });

            final Section oldData = cliSection.getData();
            if (oldData.getTimeSignature().equals(ts))
            {
                return WriteOperationResults.of(null);
            }
            Preconditions.checkArgument(items.contains(cliSection), "cliSection=%s items=%s", cliSection, items);


            final Section newData = new Section(oldData.getName(), ts);


            // Pre-check change
            var preCheckEvent = new SectionChangedEvent(ChordLeadSheetImpl.this, cliSection, oldData, newData, Collections.emptyList());
            preCheckChange(preCheckEvent);          // throws UnsupportedEditException


            // First adjust items if required
            final List<PropertyChangeEvent> cliEvents = new ArrayList<>();
            final List<ItemMoved> adjustments;
            if (ts.getNbNaturalBeats() < oldData.getTimeSignature().getNbNaturalBeats())
            {
                var iitems = getItems(cliSection, ChordLeadSheetItem.class, cli -> !ts.checkBeat(cli.getPosition().getBeat()));
                adjustments = adjustItemsToTimeSignature(oldData.getTimeSignature(), ts, iitems);
                adjustments.forEach(im -> cliEvents.add(im.getPositionChangedEvent()));
            } else
            {
                adjustments = Collections.emptyList();
            }
            final List<ChordLeadSheetItem> adjustedItems = adjustments.stream().map(ItemMoved::item).toList();


            // Update section
            cliEvents.add(changeItemDataChecked(cliSection, newData));


            // Prepare event 
            UndoableEdit edit = new SimpleEdit("Set Section Time Signature " + ts)
            {
                @Override
                public void undoBody()
                {
                    LOGGER.log(Level.FINER, "setSectionTimeSignature.undoBody() cliSection={0}", cliSection);
                    performWriteAPImethod(() -> 
                    {
                        final List<PropertyChangeEvent> cliEvents2 = new ArrayList<>();
                        cliEvents2.add(changeItemDataChecked(cliSection, oldData));

                        for (var adj : adjustments.reversed())
                        {
                            cliEvents2.add(changeItemPositionChecked(adj.item(), adj.oldPos()));
                        }

                        var event = new SectionChangedEvent(ChordLeadSheetImpl.this, cliSection, oldData, newData, adjustedItems);
                        event.setIsUndo();
                        event.addItemChanges(cliEvents2);
                        return WriteOperationResults.of(event, null);
                    });
                }

                @Override
                public void redoBody()
                {
                    LOGGER.log(Level.FINER, "setSectionTimeSignature.redoBody() cliSection={0}", cliSection);
                    performWriteAPImethod(() -> 
                    {
                        final List<PropertyChangeEvent> cliEvents2 = new ArrayList<>();

                        for (var adj : adjustments)
                        {
                            cliEvents2.add(changeItemPositionChecked(adj.item(), adj.newPos()));
                        }

                        cliEvents2.add(changeItemDataChecked(cliSection, newData));

                        var event = new SectionChangedEvent(ChordLeadSheetImpl.this, cliSection, oldData, newData, adjustedItems);
                        event.setIsRedo();
                        event.addItemChanges(cliEvents2);
                        return WriteOperationResults.of(event, null);
                    });
                }
            };

            fireUndoableEdit(edit);

            var event = new SectionChangedEvent(ChordLeadSheetImpl.this, cliSection, oldData, newData, adjustedItems);
            event.addItemChanges(cliEvents);

            return WriteOperationResults.of(event, null);
        };

        return operation;

    }

    @Override
    public List<ChordLeadSheetItem> getItems()
    {
        return performReadAPImethod(() -> List.copyOf(items));
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


        var res = performReadAPImethod(() -> 
        {
            var tailSet = items.tailSet(cli, false);
            for (var item : tailSet)
            {
                if (itemClass.isAssignableFrom(item.getClass()))
                {
                    T itemT = (T) item;
                    if (tester.test(itemT))
                    {
                        return itemT;
                    }
                }
            }
            return null;
        });

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

        var res = performReadAPImethod(() -> 
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
                        return itemT;
                    }
                }
            }
            return null;
        });

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


        var res = performReadAPImethod(() -> 
        {
            var rangeItems = items.subSet(
                    ChordLeadSheetItem.createItemFrom(posFrom, inclusiveFrom),
                    false,
                    ChordLeadSheetItem.createItemTo(posTo, inclusiveTo),
                    false);

            return rangeItems.stream()
                    .filter(item -> itemClass.isAssignableFrom(item.getClass()))
                    .map(cli -> (T) cli)
                    .filter(tester)
                    .toList();
        });

        return res;
    }

    @Override
    public Song getSong()
    {
        return this.song;
    }

    public void setSong(Song sg)
    {
        this.song = sg;
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

    /**
     * Fire change event to all listeners.
     * <p>
     *
     * @param event
     */
    public void fireChangeEvent(ClsChangeEvent event)
    {
        Objects.requireNonNull(event);

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
        record TmpRecord(int size, List<ChordLeadSheetItem> items)
                {

        };

        TmpRecord tmpRecord = performReadAPImethod(() -> 
        {
            return new TmpRecord(this.size, List.copyOf(this.items));
        });
        TmpRecord tmpRecordOther = other.performReadAPImethod(() -> 
        {
            return new TmpRecord(other.size, List.copyOf(other.items));
        });


        return tmpRecord.equals(tmpRecordOther);
    }

    public <R> R performWriteAPImethod(WriteOperation<R> operation)
    {
        R res = executionManager.executeWriteOperation(operation);
        return res;
    }

    public <R> R performWriteAPImethodThrowing(ThrowingWriteOperation<R> operation) throws UnsupportedEditException
    {
        R res = executionManager.executeWriteOperationThrowing(operation);
        return res;
    }

    public <R> R performReadAPImethod(Supplier<R> operation)
    {
        R res = executionManager.executeReadOperation(operation);
        return res;
    }

    public <R, E extends Exception> R performReadAPImethodThrowing(ThrowingSupplier<R, E> operation) throws E
    {
        R res = executionManager.executeReadOperationThrowing(operation);
        return res;
    }

    public void preCheckChange(ClsChangeEvent event) throws UnsupportedEditException
    {
        executionManager.preCheckChange(event);
    }

    // --------------------------------------------------------------------------------------
    // SongModelComponent interface
    // --------------------------------------------------------------------------------------
    public void fireUndoableEdit(UndoableEdit edit)
    {
        Objects.requireNonNull(edit);
        UndoableEditEvent event = new UndoableEditEvent(this, edit);
        for (UndoableEditListener l : undoListeners)
        {
            l.undoableEditHappened(event);
        }
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

        Preconditions.checkState(executionManager.isWriteLockedByCurrentThread(), "write lock required");

        for (ChordLeadSheetItem<?> item : iitems)
        {
            Position oldPos = item.getPosition();
            Position newPos = oldPos.getConverted(oldTs, newTs);
            if (!newPos.equals(oldPos))
            {
                // Make sure we don't have a collision
                newPos = getAdjustedBeatToTimeSignature(oldTs, newTs, item);
                changeItemPositionChecked(item, newPos);
                res.add(new ItemMoved(item, oldPos, newPos));
            }
        }
        return res;
    }

    /**
     * Get the adjusted position of item to newTs, while avoiding collisions with an equal() item.
     *
     * @param oldTs
     * @param newTs
     * @param item
     * @return
     */
    private Position getAdjustedBeatToTimeSignature(TimeSignature oldTs, TimeSignature newTs, ChordLeadSheetItem<?> item)
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
        }
        return newPos;
    }

    /**
     * Perform the add while checking internal state consistency, then update item's container.
     *
     * @param item
     * @return The PropertyChangeEvent for the container change
     */
    private PropertyChangeEvent addItemChecked(ChordLeadSheetItem<?> item)
    {
        Preconditions.checkState(executionManager.isWriteLockedByCurrentThread(), "write lock required");
        Preconditions.checkArgument(item instanceof WritableItem, "item=%s", item);
        var b = items.add(item);
        assert b : "item=" + item + " items=" + items;
        return ((WritableItem) item).setContainer(this);
    }

    /**
     * Perform the remove while checking internal state consistency, then set item's container to null.
     *
     * @param item
     * @return The PropertyChangeEvent for the container change
     */
    private PropertyChangeEvent removeItemChecked(ChordLeadSheetItem<?> item)
    {
        Preconditions.checkState(executionManager.isWriteLockedByCurrentThread(), "write lock required");
        Preconditions.checkArgument(item instanceof WritableItem, "item=%s", item);
        var b = items.remove(item);
        assert b : "item=" + item + " items=" + items;
        return ((WritableItem) item).setContainer(null);
    }

    /**
     *
     * @param <T>
     * @param item
     * @param newData
     * @return The PropertyChangeEvent for the data change
     */
    private <T> PropertyChangeEvent changeItemDataChecked(ChordLeadSheetItem<T> item, T newData)
    {
        Preconditions.checkState(executionManager.isWriteLockedByCurrentThread(), "write lock required");
        WritableItem<T> wItem = (WritableItem<T>) item;
        var b = items.remove(wItem);
        assert b : "wItem=" + wItem + " newData=" + newData + " items=" + items;
        PropertyChangeEvent res = wItem.setData(newData);
        b = items.add(wItem);
        assert b : "wItem=" + wItem + " newData=" + newData + " items=" + items;
        return res;
    }

    /**
     *
     * @param <T>
     * @param item
     * @param newPos
     * @return The PropertyChangeEvent for the position change
     */
    private <T> PropertyChangeEvent changeItemPositionChecked(ChordLeadSheetItem<T> item, Position newPos)
    {
        Preconditions.checkState(executionManager.isWriteLockedByCurrentThread(), "write lock required");
        WritableItem<T> wItem = (WritableItem<T>) item;
        var b = items.remove(wItem);
        assert b : "wItem=" + wItem + " newPos=" + newPos + " items=" + items;
        PropertyChangeEvent res = wItem.setPosition(newPos);
        b = items.add(wItem);
        assert b : "wItem=" + wItem + " newPos=" + newPos + " items=" + items;
        return res;
    }


    // ==========================================================================================================================
    // Inner classes
    // ==========================================================================================================================    
    /**
     * Helper class to store a moved item.
     */
    private record ItemMoved(ChordLeadSheetItem item, Position oldPos, Position newPos)
            {

        ItemMoved
        {
            Objects.requireNonNull(item);
            Objects.requireNonNull(oldPos);
            Objects.requireNonNull(newPos);
            Preconditions.checkArgument(!oldPos.equals(newPos), "this=%s", this);
        }

        public PropertyChangeEvent getPositionChangedEvent()
        {
            return new PropertyChangeEvent(item, ChordLeadSheetItem.PROP_ITEM_POSITION, oldPos, newPos);
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

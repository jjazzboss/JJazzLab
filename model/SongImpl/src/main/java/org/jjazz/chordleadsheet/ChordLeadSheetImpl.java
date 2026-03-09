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
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
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
import org.jjazz.chordleadsheet.item.CLI_LoopRestartBarImpl;
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
    private transient ExecutionManager executionManager;
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

        // Add mandatory items
        ((CLI_SectionImpl) initSection).setContainer(this);
        items.add(initSection);
        var restartBar = new CLI_LoopRestartBarImpl(0);
        restartBar.setContainer(this);
        items.add(restartBar);
    }

    @Override
    public ChordLeadSheet getDeepCopy()
    {
        var res = performReadAPImethod(() -> 
        {
            var initSection = getSection(0);
            assert initSection != null;

            // Create the copy
            var clsCopy = new ChordLeadSheetImpl(initSection.getData().getName(), initSection.getData().getTimeSignature(), size);

            
            // Special handling for the LoopRestartBarItem
            var cliRestartBar = clsCopy.getLoopRestartBarItem();
            clsCopy.moveItem(cliRestartBar, getLoopRestartBarItem().getPosition());


            for (var item : getItems())
            {
                if (item == initSection)
                {
                    clsCopy.getSection(0).getClientProperties().set(initSection.getClientProperties());
                    continue;
                } else if (item instanceof CLI_LoopRestartBarImpl)
                {
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
        WriteOperation operation = () -> 
        {
            return (newSize >= size) ? insertBarsOperation(size, newSize - size).get() : deleteBarsOperation(newSize, size - 1).get();
        };
        performWriteAPImethod(operation);
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
        Preconditions.checkArgument(!(item instanceof CLI_LoopRestartBarImpl), "item=%s", item);


        WriteOperation<Boolean> operation = () -> 
        {
            LOGGER.log(Level.FINE, "addItemOperation() -- item={0}", item);


            final ChordLeadSheet oldContainer = item.getContainer();
            final Position oldPos = item.getPosition();
            int barIndex = oldPos.getBar();
            Preconditions.checkArgument(barIndex < getSizeInBars(), "addItemOperation() oldPos=%s size=%s", oldPos, size);
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
            addItemChecked(wItem);


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
                        wItem.setContainer(oldContainer);
                        cliEvents2.add(wItem.setPosition(oldPos));


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
                        addItemChecked(wItem);

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
            Preconditions.checkArgument(bar < this.size, "addSectionOperation() cliSection=%s this.size=%s", cliSection, size);

            final CLI_Section curSection = getSection(bar);
            var sameNameSection = getSection(cliSection.getData().getName());
            Preconditions.checkArgument(sameNameSection == null || sameNameSection.getPosition().getBar() == bar,
                    "addSectionOperation() cliSection=%s sameNameSection=%s", cliSection, sameNameSection);


            LOGGER.log(Level.FINE, "addSectionOperation() -- cliSection={0}", cliSection);


            // Pre-check change
            var preCheckEvent = new SectionAddedEvent(ChordLeadSheetImpl.this,
                    cliSection,
                    curSection.getPosition().getBar() == bar ? curSection : null);
            preCheckChange(preCheckEvent);          // throws UnsupportedEditException


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


            // Possibly adjust items if time signature change
            List<PropertyChangeEvent> cliEvents = new ArrayList<>();
            final TimeSignature newTs = cliSection.getData().getTimeSignature();
            if (newTs.getNbNaturalBeats() < oldTs.getNbNaturalBeats())
            {
                var iitems = getItems(cliSection, ChordLeadSheetItem.class, cli -> !newTs.checkBeat(cli.getPosition().getBeat()));
                cliEvents.addAll(adjustItemsToTimeSignature(oldTs, newTs, iitems));
            }


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
                        for (var cliEvent : cliEvents.reversed())
                        {
                            cliEvents2.add(changeItemPositionChecked((ChordLeadSheetItem<?>) cliEvent.getSource(), (Position) cliEvent.getOldValue()));
                        }

                        removeItemChecked(wSection);
                        wSection.setContainer(oldContainer);

                        if (isReplace)
                        {
                            addItemChecked(curSection);
                        }

                        var event = new SectionAddedEvent(ChordLeadSheetImpl.this, cliSection, isReplace ? curSection : null);
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
                            removeItemChecked(curSection);
                        }

                        addItemChecked(wSection);


                        for (var cliEvent : cliEvents)
                        {
                            cliEvents2.add(changeItemPositionChecked((ChordLeadSheetItem<?>) cliEvent.getSource(), (Position) cliEvent.getNewValue()));
                        }

                        var event = new SectionAddedEvent(ChordLeadSheetImpl.this, cliSection, isReplace ? curSection : null);
                        event.setIsRedo();
                        event.addItemChanges(cliEvents2);
                        return WriteOperationResults.of(event, null);
                    });
                }
            };

            fireUndoableEdit(edit);

            var event = new SectionAddedEvent(ChordLeadSheetImpl.this, cliSection, isReplace ? curSection : null);
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
            Preconditions.checkArgument(items.contains(cliSection), "removeSectionOperation() cliSection=%s items=%s", cliSection, items);

            LOGGER.log(Level.FINE, "removeSectionOperation() -- cliSection={0}", cliSection);


            // Context: The section previous to the one being removed will take over the scope
            final int bar = cliSection.getPosition().getBar();
            final CLI_Section prevSection = getSection(bar - 1);
            final TimeSignature newTs = prevSection.getData().getTimeSignature();
            final TimeSignature oldTs = cliSection.getData().getTimeSignature();


            // Update model
            removeItemChecked(cliSection);


            // Adjust items position if time signature change
            List<PropertyChangeEvent> cliEvents = new ArrayList<>();
            if (newTs.getNbNaturalBeats() < oldTs.getNbNaturalBeats())
            {
                var iitems = getItems(prevSection, ChordLeadSheetItem.class, cli -> !newTs.checkBeat(cli.getPosition().getBeat()));
                cliEvents.addAll(adjustItemsToTimeSignature(oldTs, newTs, iitems));
            }


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

                        for (var cliEvent : cliEvents.reversed())
                        {
                            cliEvents2.add(changeItemPositionChecked((ChordLeadSheetItem<?>) cliEvent.getSource(), (Position) cliEvent.getOldValue()));
                        }

                        addItemChecked(cliSection);

                        var event = new SectionRemovedEvent(ChordLeadSheetImpl.this, cliSection);
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
                        removeItemChecked(cliSection);

                        List<PropertyChangeEvent> cliEvents2 = new ArrayList<>();
                        for (var cliEvent : cliEvents)
                        {
                            cliEvents2.add(changeItemPositionChecked((ChordLeadSheetItem<?>) cliEvent.getSource(), (Position) cliEvent.getNewValue()));
                        }

                        var event = new SectionRemovedEvent(ChordLeadSheetImpl.this, cliSection);
                        event.addItemChanges(cliEvents2);
                        return WriteOperationResults.of(event, null);
                    });
                }
            };

            fireUndoableEdit(edit);

            var event = new SectionRemovedEvent(ChordLeadSheetImpl.this, cliSection);
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
            Preconditions.checkArgument(newBarIndex < size, "moveSectionOperation() newBarIndex=%s size=%s", newBarIndex, size);
            Preconditions.checkArgument(items.contains(cliSection), "moveSectionOperation() cliSection=%s items=%s", cliSection, items);

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


            // Update model
            List<PropertyChangeEvent> cliEvents = new ArrayList<>();
            cliEvents.add(changeItemPositionChecked(cliSection, new Position(newBarIndex)));


            // Adjust items impacted by possible time signature change
            CLI_Section oldPosNewSection = getSection(oldBarIndex);
            var oldPosNewTs = oldPosNewSection.getData().getTimeSignature();
            var ts = cliSection.getData().getTimeSignature();
            if (ts.getNbNaturalBeats() < newPosOldTs.getNbNaturalBeats())
            {
                var iitems = getItems(cliSection, ChordLeadSheetItem.class, cli -> !ts.checkBeat(cli.getPosition().getBeat()));
                cliEvents.addAll(adjustItemsToTimeSignature(newPosOldTs, ts, iitems));
            }
            if (oldPosNewTs.getNbNaturalBeats() < ts.getNbNaturalBeats())
            {
                var iitems = getItems(oldPosNewSection, ChordLeadSheetItem.class, cli -> !oldPosNewTs.checkBeat(cli.getPosition().getBeat()));
                cliEvents.addAll(adjustItemsToTimeSignature(ts, oldPosNewTs, iitems));
            }


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
                        for (var cliEvent : cliEvents.reversed())
                        {
                            cliEvents2.add(changeItemPositionChecked((ChordLeadSheetItem<?>) cliEvent.getSource(), (Position) cliEvent.getOldValue()));
                        }

                        var event = new SectionMovedEvent(ChordLeadSheetImpl.this, cliSection, oldBarIndex, newBarIndex);
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
                        for (var cliEvent : cliEvents)
                        {
                            cliEvents2.add(changeItemPositionChecked((ChordLeadSheetItem<?>) cliEvent.getSource(), (Position) cliEvent.getNewValue()));
                        }

                        var event = new SectionMovedEvent(ChordLeadSheetImpl.this, cliSection, oldBarIndex, newBarIndex);
                        event.setIsRedo();
                        event.addItemChanges(cliEvents2);
                        return WriteOperationResults.of(event, null);
                    });
                }
            };

            fireUndoableEdit(edit);

            var event = new SectionMovedEvent(ChordLeadSheetImpl.this, cliSection, oldBarIndex, newBarIndex);
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
        Preconditions.checkArgument(!(item instanceof CLI_LoopRestartBarImpl), "item=%s", item);


        WriteOperation<Boolean> operation = () -> 
        {
            LOGGER.log(Level.FINE, "removeItem() -- item={0}", item);

            if (!items.contains(item))
            {
                LOGGER.log(Level.FINE, "addItem() item not present. item={0}", item);
                return WriteOperationResults.of(Boolean.FALSE);
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
                    performWriteAPImethod(() -> 
                    {
                        addItemChecked(item);

                        var event = new ItemRemovedEvent(ChordLeadSheetImpl.this, item);
                        event.setIsUndo();
                        return WriteOperationResults.of(event, Boolean.TRUE);
                    });
                }

                @Override
                public void redoBody()
                {
                    LOGGER.log(Level.FINER, "removeItem.redoBody() item={0}", item);
                    performWriteAPImethod(() -> 
                    {
                        removeItemChecked(item);

                        var event = new ItemRemovedEvent(ChordLeadSheetImpl.this, item);
                        event.setIsRedo();
                        return WriteOperationResults.of(event, Boolean.TRUE);

                    });
                }
            };

            fireUndoableEdit(edit);

            var event = new ItemRemovedEvent(ChordLeadSheetImpl.this, item);
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
            Preconditions.checkArgument(items.contains(item), "moveItemOperation() item=%s items=%s", item, items);

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
            var propEvent = changeItemPositionChecked(item, newAdjustedPos);


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
                        var propEvent2 = changeItemPositionChecked(item, oldPos);

                        var event = new ItemMovedEvent(ChordLeadSheetImpl.this, item, oldPos, newAdjustedPos);
                        event.setIsUndo();
                        event.addItemChanges(List.of(propEvent2));
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
                        var propEvent2 = changeItemPositionChecked(item, newAdjustedPos);

                        var event = new ItemMovedEvent(ChordLeadSheetImpl.this, item, oldPos, newAdjustedPos);
                        event.setIsRedo();
                        event.addItemChanges(List.of(propEvent2));
                        return WriteOperationResults.of(event, Boolean.TRUE);
                    });
                }
            };

            fireUndoableEdit(edit);

            var event = new ItemMovedEvent(ChordLeadSheetImpl.this, item, oldPos, newAdjustedPos);
            event.addItemChanges(List.of(propEvent));
            return WriteOperationResults.of(event, Boolean.TRUE);
        };

        return operation;
    }

    @Override
    public <T> boolean changeItem(ChordLeadSheetItem<T> item, final T newData)
    {
        if (!item.getData().equals(newData))
        {
            return performWriteAPImethod(changeItemOperation(item, newData));
        }
        return false;
    }

    public <T> WriteOperation<Boolean> changeItemOperation(ChordLeadSheetItem<T> item, final T newData)
    {
        Preconditions.checkArgument(item instanceof WritableItem, "item=%s", item);
        Preconditions.checkArgument(!(item instanceof CLI_Section), "item=%s", item);


        WriteOperation<Boolean> operation = () -> 
        {
            Preconditions.checkArgument(items.contains(item), "changeItemOperation() item=%s items=%s", item, items);

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
            var propEvent = changeItemDataChecked(item, newData);


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
                        var propEvent2 = changeItemDataChecked(item, oldData);

                        var event = new ItemChangedEvent(ChordLeadSheetImpl.this, item, oldData, newData);
                        event.setIsUndo();
                        event.addItemChanges(List.of(propEvent2));
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
                        var propEvent2 = changeItemDataChecked(item, newData);

                        var event = new ItemChangedEvent(ChordLeadSheetImpl.this, item, oldData, newData);
                        event.setIsRedo();
                        event.addItemChanges(List.of(propEvent2));
                        return WriteOperationResults.of(event, Boolean.TRUE);
                    });
                }
            };

            fireUndoableEdit(edit);

            var event = new ItemChangedEvent(ChordLeadSheetImpl.this, item, oldData, newData);
            event.addItemChanges(List.of(propEvent));
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
            Preconditions.checkArgument(barIndex <= size, "insertBarsOperation() barIndex=%s size=%s", barIndex, size);
            Preconditions.checkArgument(size + nbBars <= MAX_SIZE, "insertBarsOperation() nbBars=%s size=%s", nbBars, size);

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


            // Init section special case
            final CLI_Section newSection0;
            if (barIndex == 0)
            {
                var section0 = getSection(0);
                var data0 = section0.getData();
                String newName = "_" + data0.getName();
                while (getSection(newName) != null)
                {
                    newName = "_" + newName;
                }
                newSection0 = (CLI_Section) section0.getCopy(new Section(newName, data0.getTimeSignature()), null);
            } else
            {
                newSection0 = null;
            }


            // Update model
            size = newSize;


            // Shift events
            final List<PropertyChangeEvent> cliEvents = new ArrayList<>();
            final List<ChordLeadSheetItem> shiftedItems = getItems(barIndex, Integer.MAX_VALUE, ChordLeadSheetItem.class, cli -> true);
            for (var item : shiftedItems.reversed())
            {
                cliEvents.add(changeItemPositionChecked(item, item.getPosition().getMoved(nbBars, 0)));
            }


            // Add new init section if required
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
                    performWriteAPImethod(() -> 
                    {
                        if (newSection0 != null)
                        {
                            removeItemChecked(newSection0);
                        }

                        final List<PropertyChangeEvent> cliEvents2 = new ArrayList<>();
                        for (var cliEvent : cliEvents.reversed())
                        {
                            cliEvents2.add(changeItemPositionChecked((ChordLeadSheetItem<?>) cliEvent.getSource(), (Position) cliEvent.getOldValue()));
                        }

                        size = oldSize;

                        var event = new InsertedBarsEvent(ChordLeadSheetImpl.this, barIndex, nbBars, newSection0);
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
                        size = newSize;

                        final List<PropertyChangeEvent> cliEvents2 = new ArrayList<>();
                        for (var cliEvent : cliEvents)
                        {
                            cliEvents2.add(changeItemPositionChecked((ChordLeadSheetItem<?>) cliEvent.getSource(), (Position) cliEvent.getNewValue()));
                        }

                        if (newSection0 != null)
                        {
                            addItemChecked(newSection0);
                        }

                        var event = new InsertedBarsEvent(ChordLeadSheetImpl.this, barIndex, nbBars, newSection0);
                        event.setIsRedo();
                        event.addItemChanges(cliEvents2);
                        return WriteOperationResults.of(event, null);
                    });
                }
            };

            fireUndoableEdit(edit);

            var event = new InsertedBarsEvent(ChordLeadSheetImpl.this, barIndex, nbBars, newSection0);
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
            Preconditions.checkArgument(barIndexTo < size && barIndexTo - barIndexFrom + 1 < size, "deleteBarsOperation() barIndexFrom=%s barIndexTo=%s size=%s",
                    barIndexFrom,
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
            if (prevSectionBeforeCut != null
                    && oldSectionAfter != null
                    && oldSectionAfter.getPosition().getBar() <= barIndexTo
                    && prevSectionBeforeCut.getData().getTimeSignature().getNbNaturalBeats() < oldSectionAfter.getData().getTimeSignature().getNbNaturalBeats())
            {
                var oldSectionAfterItems = getItems(oldSectionAfter, ChordLeadSheetItem.class);
                itemsToShift.stream()
                        .filter(item -> oldSectionAfterItems.contains(item))
                        .filter(item -> !prevSectionBeforeCut.getData().getTimeSignature().checkBeat(item.getPosition().getBeat()))
                        .forEach(item -> itemsToAdjust.add(item));
            }


            // Save old position before moving items
            Map<ChordLeadSheetItem, Position> mapSaveItemOldPos = new IdentityHashMap<>();
            itemsToShift.forEach(item -> mapSaveItemOldPos.put(item, item.getPosition()));


            // Update model
            for (var item : itemsToRemove)
            {
                removeItemChecked(item);
            }

            // Shift items of several bars
            for (var item : itemsToShift)
            {
                Position oldPos = item.getPosition();
                Position newPos = oldPos.getMoved(-nbBars, 0);
                changeItemPositionChecked(item, newPos);
            }

            // Possibly adjust some items for time signature change
            // This must be done after the bar shift, so that getAdjustedBeatToTimeSignature() anti-collision feature can work 
            for (var item : itemsToAdjust)
            {
                Position newPos = getAdjustedBeatToTimeSignature(oldSectionAfter.getData().getTimeSignature(),
                        prevSectionBeforeCut.getData().getTimeSignature(),
                        item);
                changeItemPositionChecked(item, newPos);
            }


            // Collect the position change events
            final List<PropertyChangeEvent> cliEvents = new ArrayList<>();
            itemsToShift.forEach(item -> 
            {
                var newPos = item.getPosition();
                var oldPos = mapSaveItemOldPos.get(item);
                cliEvents.add(new PropertyChangeEvent(item, ChordLeadSheetItem.PROP_ITEM_POSITION, oldPos, newPos));
            });


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
                        size = oldSize;

                        List<PropertyChangeEvent> cliEvents2 = new ArrayList<>();
                        for (var cliEvent : cliEvents.reversed())
                        {
                            cliEvents2.add(changeItemPositionChecked((ChordLeadSheetItem<?>) cliEvent.getSource(), (Position) cliEvent.getOldValue()));
                        }

                        for (var item : itemsToRemove)
                        {
                            addItemChecked(item);
                        }

                        var event = new DeletedBarsEvent(ChordLeadSheetImpl.this, barIndexFrom, barIndexTo, itemsToRemove);
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
                        for (var item : itemsToRemove)
                        {
                            removeItemChecked(item);
                        }

                        List<PropertyChangeEvent> cliEvents2 = new ArrayList<>();
                        for (var cliEvent : cliEvents)
                        {
                            cliEvents2.add(changeItemPositionChecked((ChordLeadSheetItem<?>) cliEvent.getSource(), (Position) cliEvent.getNewValue()));
                        }

                        size = newSize;

                        var event = new DeletedBarsEvent(ChordLeadSheetImpl.this, barIndexFrom, barIndexTo, itemsToRemove);
                        event.setIsRedo();
                        event.addItemChanges(cliEvents2);
                        return WriteOperationResults.of(event, null);

                    });
                }
            };

            fireUndoableEdit(edit);

            var event = new DeletedBarsEvent(ChordLeadSheetImpl.this, barIndexFrom, barIndexTo, itemsToRemove);
            event.addItemChanges(cliEvents);
            return WriteOperationResults.of(event, null);

        };

        return operation;
    }

    @Override
    public void setSectionName(CLI_Section cliSection, String name)
    {
        if (!cliSection.getData().getName().equals(name))
        {
            performWriteAPImethod(setSectionNameOperation(cliSection, name));
        }
    }

    public WriteOperation setSectionNameOperation(CLI_Section cliSection, String name)
    {
        Preconditions.checkNotNull(cliSection);
        Preconditions.checkNotNull(name);
        Preconditions.checkArgument(cliSection instanceof WritableItem, "cliSection=%s", cliSection);


        WriteOperation operation = () -> 
        {
            Preconditions.checkArgument(items.contains(cliSection), "setSectionNameOperation() cliSection=%s items=%s", cliSection, items);
            if (cliSection.getData().getName().equals(name))
            {
                return WriteOperationResults.of(null);
            }
            Preconditions.checkArgument(getSection(name) == null, "setSectionNameOperation() cliSection=%s items=%s", cliSection, items);


            LOGGER.log(Level.FINE, "setSectionNameOperation() -- cliSection={0} name={1}", new Object[]
            {
                cliSection, name
            });

            final Section oldData = cliSection.getData();
            final Section newData = new Section(name, oldData.getTimeSignature());


            // Change state
            var propEvent = changeItemDataChecked(cliSection, newData);


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
                        var propEvent2 = changeItemDataChecked(cliSection, oldData);

                        var event = new SectionChangedEvent(ChordLeadSheetImpl.this, cliSection, oldData, newData);
                        event.setIsUndo();
                        event.addItemChanges(List.of(propEvent2));
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
                        var propEvent2 = changeItemDataChecked(cliSection, newData);

                        var event = new SectionChangedEvent(ChordLeadSheetImpl.this, cliSection, oldData, newData);
                        event.setIsRedo();
                        event.addItemChanges(List.of(propEvent2));
                        return WriteOperationResults.of(event, null);
                    });
                }
            };

            fireUndoableEdit(edit);

            var event = new SectionChangedEvent(ChordLeadSheetImpl.this, cliSection, oldData, newData);
            event.addItemChanges(List.of(propEvent));
            return WriteOperationResults.of(event, null);
        };

        return operation;
    }

    @Override
    public void setSectionTimeSignature(CLI_Section cliSection, TimeSignature ts) throws UnsupportedEditException
    {
        if (!cliSection.getData().getTimeSignature().equals(ts))
        {
            performWriteAPImethodThrowing(setSectionTimeSignatureOperation(cliSection, ts));
        }
    }

    public ThrowingWriteOperation setSectionTimeSignatureOperation(CLI_Section cliSection, TimeSignature ts) throws UnsupportedEditException
    {
        Preconditions.checkNotNull(ts);
        Preconditions.checkArgument(cliSection instanceof WritableItem, "cliSection=%s", cliSection);


        ThrowingWriteOperation operation = () -> 
        {
            Preconditions.checkArgument(items.contains(cliSection), "setSectionTimeSignatureOperation() cliSection=%s items=%s", cliSection, items);
            final Section oldData = cliSection.getData();
            if (oldData.getTimeSignature() == ts)
            {
                return WriteOperationResults.of(null);
            }
            final Section newData = new Section(oldData.getName(), ts);

            LOGGER.log(Level.FINE, "setSectionTimeSignatureOperation() -- cliSection={0} ts={1}", new Object[]
            {
                cliSection, ts
            });


            // Pre-check change
            var preCheckEvent = new SectionChangedEvent(ChordLeadSheetImpl.this, cliSection, oldData, newData);
            preCheckChange(preCheckEvent);          // throws UnsupportedEditException


            // First adjust items if required
            final List<PropertyChangeEvent> cliEvents = new ArrayList<>();
            if (ts.getNbNaturalBeats() < oldData.getTimeSignature().getNbNaturalBeats())
            {
                var iitems = getItems(cliSection, ChordLeadSheetItem.class, cli -> !ts.checkBeat(cli.getPosition().getBeat()));
                cliEvents.addAll(adjustItemsToTimeSignature(oldData.getTimeSignature(), ts, iitems));
            }


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

                        for (var cliEvent : filterPropChangeEvents(ChordLeadSheetItem.PROP_ITEM_POSITION, cliEvents.reversed()))
                        {
                            cliEvents2.add(changeItemPositionChecked((ChordLeadSheetItem<?>) cliEvent.getSource(), (Position) cliEvent.getOldValue()));
                        }

                        var event = new SectionChangedEvent(ChordLeadSheetImpl.this, cliSection, oldData, newData);
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
                        for (var cliEvent : filterPropChangeEvents(ChordLeadSheetItem.PROP_ITEM_POSITION, cliEvents))
                        {
                            cliEvents2.add(changeItemPositionChecked((ChordLeadSheetItem<?>) cliEvent.getSource(), (Position) cliEvent.getNewValue()));
                        }

                        cliEvents2.add(changeItemDataChecked(cliSection, newData));

                        var event = new SectionChangedEvent(ChordLeadSheetImpl.this, cliSection, oldData, newData);
                        event.setIsRedo();
                        event.addItemChanges(cliEvents2);
                        return WriteOperationResults.of(event, null);
                    });
                }
            };

            fireUndoableEdit(edit);

            var event = new SectionChangedEvent(ChordLeadSheetImpl.this, cliSection, oldData, newData);
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

    @Override
    public int hashCode()
    {
        int res = performReadAPImethod(() -> 
        {
            int hash = 3;
            hash = 37 * hash + Objects.hashCode(this.items);
            hash = 37 * hash + this.size;
            return hash;
        });
        return res;
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
     * @return PROP_ITEM_POSITION change events
     */
    @SuppressWarnings("rawtypes")
    private List<PropertyChangeEvent> adjustItemsToTimeSignature(TimeSignature oldTs, TimeSignature newTs, List<? extends ChordLeadSheetItem> iitems)
    {
        Objects.requireNonNull(oldTs);
        Objects.requireNonNull(newTs);
        Objects.requireNonNull(iitems);
        Preconditions.checkState(executionManager.isWriteLockedByCurrentThread(), "adjustItemsToTimeSignature() write lock required");


        LOGGER.log(Level.FINER, "adjustItemsToTimeSignature() -- oldTs={0} newTs={1} iitems={2}", new Object[]
        {
            oldTs, newTs, iitems
        });


        if (oldTs.getNbNaturalBeats() == newTs.getNbNaturalBeats())
        {
            return Collections.emptyList();
        }


        List<PropertyChangeEvent> res = new ArrayList<>();
        for (ChordLeadSheetItem<?> item : iitems)
        {
            Position oldPos = item.getPosition();
            Position newPos = oldPos.getConverted(oldTs, newTs);
            if (!newPos.equals(oldPos))
            {
                // Make sure we don't have a collision
                newPos = getAdjustedBeatToTimeSignature(oldTs, newTs, item);
                res.add(changeItemPositionChecked(item, newPos));
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
     */
    private void addItemChecked(ChordLeadSheetItem<?> item)
    {
        Preconditions.checkState(executionManager.isWriteLockedByCurrentThread(), "addItemChecked() write lock required");
        Preconditions.checkArgument(item instanceof WritableItem, "addItemChecked()  item=%s", item);
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
        Preconditions.checkState(executionManager.isWriteLockedByCurrentThread(), "removeItemChecked() write lock required");
        Preconditions.checkArgument(item instanceof WritableItem, "removeItemChecked() item=%s", item);
        var b = items.remove(item);
        assert b : "item=" + item + " items=" + items;
        ((WritableItem) item).setContainer(null);
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
        Preconditions.checkState(executionManager.isWriteLockedByCurrentThread(), "changeItemDataChecked() write lock required");
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
        Preconditions.checkState(executionManager.isWriteLockedByCurrentThread(), "changeItemPositionChecked() write lock required");
        WritableItem<T> wItem = (WritableItem<T>) item;
        var b = items.remove(wItem);
        assert b : "wItem=" + wItem + " newPos=" + newPos + " items=" + items;
        PropertyChangeEvent res = wItem.setPosition(newPos);
        b = items.add(wItem);
        assert b : "wItem=" + wItem + " newPos=" + newPos + " items=" + items;
        return res;
    }


    /**
     * Get the PropertyChangeEvents which match propName.
     *
     * @param propName
     * @param propEvents
     * @return
     */
    private List<PropertyChangeEvent> filterPropChangeEvents(String propName, List<PropertyChangeEvent> propEvents)
    {
        return propEvents.stream()
                .filter(e -> e.getPropertyName().equals(propName))
                .toList();
    }

    // ==========================================================================================================================
    // Inner classes
    // ==========================================================================================================================    
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
     * spVERSION 3 (JJazzLab 5.2) introduces CLI_LoopRestartBar
     */
    private static class SerializationProxy implements Serializable
    {

        private static final long serialVersionUID = 2879716323116L;
        private final int spVERSION = 3;
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
                } else if (item instanceof CLI_LoopRestartBarImpl)
                {
                    var cliLoopRestartBar = cls.getLoopRestartBarItem();
                    cls.moveItem(cliLoopRestartBar, item.getPosition());
                } else
                {
                    cls.addItem(item);
                }
            }
            return cls;
        }
    }
}

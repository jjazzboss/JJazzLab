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
package org.jjazz.songstructure;

import com.google.common.base.Preconditions;
import com.thoughtworks.xstream.XStream;
import java.beans.PropertyChangeEvent;
import org.jjazz.songstructure.api.event.RpValueChangedEvent;
import org.jjazz.songstructure.api.event.SptRhythmChangedEvent;
import org.jjazz.songstructure.api.event.SgsChangeEvent;
import org.jjazz.songstructure.api.event.SptResizedEvent;
import org.jjazz.songstructure.api.event.SptRemovedEvent;
import org.jjazz.songstructure.api.event.SptRenamedEvent;
import org.jjazz.songstructure.api.event.SptAddedEvent;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.UndoableEdit;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.jjazz.undomanager.api.SimpleEdit;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.harmony.api.Position;
import org.jjazz.rhythm.api.AdaptedRhythm;
import org.jjazz.rhythmdatabase.api.RhythmInfo;
import org.jjazz.rhythmdatabase.api.UnavailableRhythmException;
import org.jjazz.song.ExecutionManager;
import org.jjazz.song.ThrowingWriteOperation;
import org.jjazz.song.WriteOperation;
import org.jjazz.song.WriteOperationResults;
import org.jjazz.song.api.Song;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SgsChangeListener;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.event.SptRenamedEvent.Renaming;
import org.jjazz.songstructure.api.event.SptResizedEvent.Resizing;
import org.jjazz.utilities.api.FloatRange;
import org.jjazz.utilities.api.IntRange;
import org.jjazz.utilities.api.SharedExecutorServices;
import org.jjazz.utilities.api.ThrowingSupplier;
import org.jjazz.xstream.spi.XStreamConfigurator;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_SAVE;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_SAVE;
import org.openide.util.Exceptions;
import org.openide.util.lookup.ServiceProvider;


/**
 * SongStructure implementation.
 * <p>
 */
public class SongStructureImpl implements SongStructure, Serializable
{

    /**
     * SongParts ordered by startBarIndex.
     */
    private ArrayList<SongPart> songParts;
    /**
     * Our parent ChordLeadSheet.
     */
    private final ChordLeadSheet parentCls;
    /**
     * Keep the last Rhythm used for each TimeSignature.
     */
    private transient Map<TimeSignature, Rhythm> mapTsLastRhythm;
    private transient Song song;
    /**
     * The listeners for changes.
     */
    private final transient CopyOnWriteArrayList<SgsChangeListener> listeners;
    /**
     * The listeners for undoable edits.
     */
    private final transient CopyOnWriteArrayList<UndoableEditListener> undoListeners;
    private transient ExecutionManager executionManager;
    private static final Logger LOGGER = Logger.getLogger(SongStructureImpl.class.getSimpleName());


    /**
     *
     * @param cls The parent chordleadsheet
     */
    public SongStructureImpl(ChordLeadSheet cls)
    {
        Objects.requireNonNull(cls);
        parentCls = cls;
        songParts = new ArrayList<>();
        mapTsLastRhythm = new HashMap<>();
        listeners = new CopyOnWriteArrayList<>();
        undoListeners = new CopyOnWriteArrayList<>();
        this.executionManager = new ExecutionManager();
    }

    public void setExecutionManager(ExecutionManager em)
    {
        Objects.requireNonNull(em);
        this.executionManager = em;
    }

    public ExecutionManager getExecutionManager()
    {
        return executionManager;
    }

    @Override
    public boolean equals(Object obj)
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
        final SongStructureImpl other = (SongStructureImpl) obj;
        if (parentCls != other.parentCls)       // never change, use identify equals
        {
            return false;
        }
        var sptsSnapshot = performReadAPImethod(() -> new ArrayList<>(this.songParts));
        var otherSptsSnapshot = other.performReadAPImethod(() -> new ArrayList<>(other.songParts));
        return Objects.equals(sptsSnapshot, otherSptsSnapshot);
    }

    @Override
    public int hashCode()
    {
        int res = performReadAPImethod(() -> 
        {
            int hash = 3;
            hash = 89 * hash + Objects.hashCode(this.songParts);
            hash = 89 * hash + Objects.hashCode(this.parentCls);
            return hash;
        });
        return res;
    }


    @Override
    public SongStructureImpl getDeepCopy(ChordLeadSheet newParentCls)
    {
        var newParent = newParentCls == null ? parentCls : newParentCls;
        SongStructureImpl res = new SongStructureImpl(newParent);

        performReadAPImethod(() -> 
        {
            var newSpts = songParts.stream()
                    .map(spt -> 
                    {
                        CLI_Section oldParentSection = spt.getParentSection();
                        CLI_Section newParentSection = newParent.getSection(oldParentSection.getData().getName());                        
                        return spt.getCopy(null, spt.getStartBarIndex(), spt.getNbBars(), newParentSection);
                    })
                    .toList();

            res.songParts.addAll(newSpts);
            res.mapTsLastRhythm = new HashMap<>(mapTsLastRhythm);
            return null;
        });

        return res;
    }


    @Override
    public ChordLeadSheet getParentChordLeadSheet()
    {
        return parentCls;
    }

    /**
     * @return A copy of the list of SongParts ordered according to their getStartBarIndex().
     */
    @Override
    public List<SongPart> getSongParts()
    {
        return performReadAPImethod(() -> new ArrayList<>(songParts));
    }

    @Override
    public int getSizeInBars()
    {
        return performReadAPImethod(() -> songParts.isEmpty() ? 0 : getSptLastBarIndex(songParts.size() - 1) + 1);
    }

    @Override
    public FloatRange toBeatRange(IntRange rg)
    {
        return performReadAPImethod(() -> 
        {
            if (getSizeInBars() == 0)
            {
                return FloatRange.EMPTY_FLOAT_RANGE;
            }

            IntRange songRange = new IntRange(0, getSizeInBars() - 1);
            IntRange rg2 = rg == null ? songRange : rg;
            if (!songRange.contains(rg2))
            {
                return FloatRange.EMPTY_FLOAT_RANGE;
            }

            float startPos = -1;
            float endPos = -1;
            for (SongPart spt : songParts)
            {
                TimeSignature ts = spt.getRhythm().getTimeSignature();
                IntRange ir = rg2.getIntersection(spt.getBarRange());
                if (ir.isEmpty())
                {
                    continue;
                }
                if (startPos == -1)
                {
                    startPos = toPositionInNaturalBeats(ir.from);
                    endPos = startPos + ir.size() * ts.getNbNaturalBeats();
                } else
                {
                    endPos += ir.size() * ts.getNbNaturalBeats();
                }
            }
            return new FloatRange(startPos, endPos);
        });
    }

    @Override
    public SongPart createSongPart(Rhythm r, String name, int startBarIndex, CLI_Section parentSection, boolean reusePrevParamValues)
    {
        Objects.requireNonNull(r);
        Objects.requireNonNull(parentSection);
        Preconditions.checkArgument(r.getTimeSignature() == parentSection.getData().getTimeSignature(), "r=%s parentSection=%s", r, parentSection);
        Preconditions.checkArgument(startBarIndex >= 0, "name=%s startBarIndex=%s", name, startBarIndex);

        SongPartImpl spt;
        int nbBars = parentCls.getBarRange(parentSection).size();
        if (startBarIndex > 0 && reusePrevParamValues)
        {
            // New song part which reuse parameters from previous song part
            SongPart prevSpt = getSongPart(startBarIndex - 1);
            spt = (SongPartImpl) prevSpt.getCopy(r, startBarIndex, nbBars, parentSection);
        } else
        {
            // New song part with default RP values
            spt = new SongPartImpl(this, r, startBarIndex, nbBars, parentSection);
        }

        spt.setName(name == null ? parentSection.getData().getName() : name);

        return spt;
    }


    @Override
    public void addSongParts(List<SongPart> spts) throws UnsupportedEditException
    {
        performWriteAPImethodThrowing(addSongPartsOperation(spts));

        // Make sure all AdaptedRhythms for the song’s rhythms are generated in the database so that the user can access them if needed.
        generateAllAdaptedRhythms();
    }

    public ThrowingWriteOperation addSongPartsOperation(List<SongPart> spts)
    {
        Objects.requireNonNull(spts);
        final var sptsToAdd = new ArrayList<>(spts);

        ThrowingWriteOperation operation = () -> 
        {
            LOGGER.log(Level.FINE, "addSongPartsOperation() -- spts={0}", sptsToAdd);

            if (sptsToAdd.isEmpty())
            {
                return WriteOperationResults.of(null);
            }

            var preCheckEvent = new SptAddedEvent(this, sptsToAdd);
            preCheckChange(preCheckEvent);           // throws UnsupportedEditException  


            // Prepare data
            final ArrayList<SongPart> oldSongParts = new ArrayList<>(songParts);
            final Map<TimeSignature, Rhythm> oldMapTsRhythm = new HashMap<>(mapTsLastRhythm);
            final Map<SongPart, SongStructure> oldContainers = new IdentityHashMap<>();
            sptsToAdd.forEach(spt -> oldContainers.put(spt, spt.getContainer()));
            final Map<SongPart, Integer> mapSptSavedStartBar = new IdentityHashMap<>();
            songParts.forEach(spt -> mapSptSavedStartBar.put(spt, spt.getStartBarIndex()));


            // Update model
            sptsToAdd.forEach(spt -> addSongPartImpl(spt));


            // Prepare events for shifted SongParts
            final List<PropertyChangeEvent> sptEvents = new ArrayList<>();
            final List<PropertyChangeEvent> sptEventsForUndo = new ArrayList<>();
            for (var spt : oldSongParts)
            {
                int newBar = spt.getStartBarIndex();
                int oldBar = mapSptSavedStartBar.get(spt);
                if (newBar == oldBar)
                {
                    continue;
                }
                sptEvents.add(new PropertyChangeEvent(spt, SongPart.PROP_START_BAR_INDEX, oldBar, newBar));
                sptEventsForUndo.add(new PropertyChangeEvent(spt, SongPart.PROP_START_BAR_INDEX, newBar, oldBar));
            }

            final ArrayList<SongPart> newSongParts = new ArrayList<>(songParts);
            final Map<TimeSignature, Rhythm> newMapTsRhythm = new HashMap<>(mapTsLastRhythm);

            UndoableEdit edit = new SimpleEdit("Add SongParts")
            {
                @Override
                public void undoBody()
                {
                    LOGGER.log(Level.FINER, "addSongParts.undoBody() spts={0}", sptsToAdd);
                    performWriteAPImethod(() -> 
                    {
                        songParts = new ArrayList<>(oldSongParts);
                        mapTsLastRhythm = new HashMap<>(oldMapTsRhythm);
                        updateStartBarIndexes();
                        for (SongPart spt : sptsToAdd)
                        {
                            ((SongPartImpl) spt).setContainer(oldContainers.get(spt));
                        }

                        var event = new SptAddedEvent(SongStructureImpl.this, sptsToAdd);
                        event.setIsUndo();
                        event.addSongPartChanges(sptEventsForUndo);
                        return WriteOperationResults.of(event, null);
                    });
                }

                @Override
                public void redoBody()
                {
                    LOGGER.log(Level.FINER, "addSongParts.redoBody() spts={0}", sptsToAdd);
                    performWriteAPImethod(() -> 
                    {
                        songParts = new ArrayList<>(newSongParts);
                        mapTsLastRhythm = new HashMap<>(newMapTsRhythm);
                        updateStartBarIndexes();
                        for (SongPart spt : sptsToAdd)
                        {
                            ((SongPartImpl) spt).setContainer(SongStructureImpl.this);
                        }

                        var event = new SptAddedEvent(SongStructureImpl.this, sptsToAdd);
                        event.setIsRedo();
                        event.addSongPartChanges(sptEvents);
                        return WriteOperationResults.of(event, null);
                    });
                }
            };

            fireUndoableEditHappened(edit);


            var event = new SptAddedEvent(this, sptsToAdd);
            event.addSongPartChanges(sptEvents);


            return WriteOperationResults.of(event, null);
        };

        return operation;
    }

    @Override
    public void removeSongParts(List<SongPart> spts)
    {
        performWriteAPImethod(removeSongPartsOperation(spts));
    }

    public WriteOperation removeSongPartsOperation(List<SongPart> spts)
    {
        Objects.requireNonNull(spts);

        final var sptsToRemove = new ArrayList<>(spts);

        WriteOperation operation = () -> 
        {
            LOGGER.log(Level.FINE, "removeSongPartsOperation() -- spts={0}", sptsToRemove);

            if (sptsToRemove.isEmpty())
            {
                return WriteOperationResults.of(null);
            }

            final ArrayList<SongPart> oldSongParts = new ArrayList<>(songParts);
            final Map<TimeSignature, Rhythm> oldMapTsRhythm = new HashMap<>(mapTsLastRhythm);
            final Map<SongPart, Integer> mapSptSavedStartBar = new IdentityHashMap<>();
            songParts.forEach(spt -> mapSptSavedStartBar.put(spt, spt.getStartBarIndex()));


            // Update model
            sptsToRemove.forEach(spt -> removeSongPartImpl(spt));


            // Prepare events for shifted SongParts
            final List<PropertyChangeEvent> sptEvents = new ArrayList<>();
            final List<PropertyChangeEvent> sptEventsForUndo = new ArrayList<>();
            for (var spt : oldSongParts)
            {
                int newBar = spt.getStartBarIndex();
                int oldBar = mapSptSavedStartBar.get(spt);
                if (newBar == oldBar)
                {
                    continue;
                }
                sptEvents.add(new PropertyChangeEvent(spt, SongPart.PROP_START_BAR_INDEX, oldBar, newBar));
                sptEventsForUndo.add(new PropertyChangeEvent(spt, SongPart.PROP_START_BAR_INDEX, newBar, oldBar));
            }


            // Prepare events
            final ArrayList<SongPart> newSongParts = new ArrayList<>(songParts);
            final Map<TimeSignature, Rhythm> newMapTsRhythm = new HashMap<>(mapTsLastRhythm);

            UndoableEdit edit = new SimpleEdit("Remove SongParts")
            {
                @Override
                public void undoBody()
                {
                    LOGGER.log(Level.FINER, "removeSongParts.undoBody() spts={0}", sptsToRemove);
                    performWriteAPImethod(() -> 
                    {
                        songParts = new ArrayList<>(oldSongParts);
                        mapTsLastRhythm = new HashMap<>(oldMapTsRhythm);
                        for (SongPart spt : sptsToRemove)
                        {
                            ((SongPartImpl) spt).setContainer(SongStructureImpl.this);
                        }
                        updateStartBarIndexes();

                        var event = new SptRemovedEvent(SongStructureImpl.this, sptsToRemove);
                        event.setIsUndo();
                        event.addSongPartChanges(sptEventsForUndo);
                        return WriteOperationResults.of(event, null);
                    });
                }

                @Override
                public void redoBody()
                {
                    LOGGER.log(Level.FINER, "removeSongParts.redoBody() spts={0}", sptsToRemove);
                    performWriteAPImethod(() -> 
                    {
                        songParts = new ArrayList<>(newSongParts);
                        mapTsLastRhythm = new HashMap<>(newMapTsRhythm);
                        updateStartBarIndexes();

                        var event = new SptRemovedEvent(SongStructureImpl.this, sptsToRemove);
                        event.setIsRedo();
                        event.addSongPartChanges(sptEvents);
                        return WriteOperationResults.of(event, null);
                    });
                }
            };

            fireUndoableEditHappened(edit);

            var event = new SptRemovedEvent(this, sptsToRemove);
            event.addSongPartChanges(sptEvents);
            return WriteOperationResults.of(event, null);
        };

        return operation;
    }


    @Override
    public void resizeSongParts(final Map<SongPart, Integer> mapSptNewSize)
    {
        performWriteAPImethod(resizeSongPartsOperation(mapSptNewSize));
    }

    public WriteOperation resizeSongPartsOperation(final Map<SongPart, Integer> mapSptNewSize)
    {
        Objects.requireNonNull(mapSptNewSize);
        final Map<SongPart, Integer> safeMapSptNewSize = new IdentityHashMap<>(mapSptNewSize);  // SongPart instances are mutable

        WriteOperation operation = () -> 
        {
            LOGGER.log(Level.FINE, "resizeSongPartsOperation() -- safeMapSptNewSize={0}", safeMapSptNewSize);

            if (safeMapSptNewSize.isEmpty())
            {
                return WriteOperationResults.of(null);
            }

            final Map<SongPart, Integer> mapSptSavedStartBar = new IdentityHashMap<>();
            songParts.forEach(spt -> mapSptSavedStartBar.put(spt, spt.getStartBarIndex()));


            // Update model
            final Map<SongPart, Resizing> mapSptResizing = new IdentityHashMap<>();
            for (SongPart spt : safeMapSptNewSize.keySet())
            {
                if (!songParts.contains(spt) || !(spt instanceof SongPartImpl))
                {
                    throw new IllegalArgumentException("this=" + this + " spt=" + spt + " safeMapSptNewSize=" + safeMapSptNewSize);
                }
                var resizing = new Resizing(spt.getNbBars(), safeMapSptNewSize.get(spt));
                mapSptResizing.put(spt, resizing);
                ((SongPartImpl) spt).setNbBars(resizing.newSize());
            }
            updateStartBarIndexes();


            // Prepare events for resized/moved song parts
            final List<PropertyChangeEvent> sptEvents = new ArrayList<>();
            final List<PropertyChangeEvent> sptEventsForUndo = new ArrayList<>();
            for (var spt : songParts)
            {
                int newBar = spt.getStartBarIndex();
                int oldBar = mapSptSavedStartBar.get(spt);
                int newSize = spt.getNbBars();
                var resizing = mapSptResizing.get(spt);
                int oldSize = resizing == null ? newSize : resizing.oldSize();
                if (newBar != oldBar)
                {
                    sptEvents.add(new PropertyChangeEvent(spt, SongPart.PROP_START_BAR_INDEX, oldBar, newBar));
                    sptEventsForUndo.add(new PropertyChangeEvent(spt, SongPart.PROP_START_BAR_INDEX, newBar, oldBar));
                }
                if (newSize != oldSize)
                {
                    sptEvents.add(new PropertyChangeEvent(spt, SongPart.PROP_NB_BARS, oldSize, newSize));
                    sptEventsForUndo.add(new PropertyChangeEvent(spt, SongPart.PROP_NB_BARS, newSize, oldSize));
                }
            }


            // Prepare events
            UndoableEdit edit = new SimpleEdit("Resize SongParts")
            {
                @Override
                public void undoBody()
                {
                    LOGGER.log(Level.FINER, "resizeSongParts.undoBody() safeMapSptNewSize={0}", safeMapSptNewSize);
                    performWriteAPImethod(() -> 
                    {
                        for (SongPart spt : mapSptResizing.keySet())
                        {
                            var resizing = mapSptResizing.get(spt);
                            ((SongPartImpl) spt).setNbBars(resizing.oldSize());
                        }
                        updateStartBarIndexes();

                        var event = new SptResizedEvent(SongStructureImpl.this, mapSptResizing);
                        event.setIsUndo();
                        event.addSongPartChanges(sptEventsForUndo);
                        return WriteOperationResults.of(event, null);
                    });
                }

                @Override
                public void redoBody()
                {
                    LOGGER.log(Level.FINER, "resizeSongParts.redoBody() safeMapSptNewSize={0}", safeMapSptNewSize);
                    performWriteAPImethod(() -> 
                    {
                        for (SongPart spt : mapSptResizing.keySet())
                        {
                            var resizing = mapSptResizing.get(spt);
                            ((SongPartImpl) spt).setNbBars(resizing.newSize());
                        }
                        updateStartBarIndexes();

                        var event = new SptResizedEvent(SongStructureImpl.this, mapSptResizing);
                        event.setIsRedo();
                        event.addSongPartChanges(sptEvents);
                        return WriteOperationResults.of(event, null);
                    });
                }
            };

            fireUndoableEditHappened(edit);

            final var event = new SptResizedEvent(this, mapSptResizing);
            event.addSongPartChanges(sptEvents);
            return WriteOperationResults.of(event, null);
        };

        return operation;
    }

    @Override
    public void setSongPartsRhythm(final List<SongPart> spts, final Rhythm newRhythm, final CLI_Section newParentSection) throws UnsupportedEditException
    {
        performWriteAPImethodThrowing(setSongPartsRhythmOperation(spts, newRhythm, newParentSection));  // throws UnsupportedEditException

        // Make sure all AdaptedRhythms for the song’s rhythms are generated in the database so that the user can access them if needed.            
        generateAllAdaptedRhythms();
    }

    public ThrowingWriteOperation setSongPartsRhythmOperation(final List<SongPart> spts, final Rhythm newRhythm, final CLI_Section newParentSection)
    {
        Objects.requireNonNull(spts);
        Preconditions.checkArgument(newParentSection == null
                || newParentSection.getContainer() != null,
                "newParentSection=%s", newParentSection);
        Preconditions.checkArgument(newParentSection == null
                || spts.stream().allMatch(spt -> spt.getNbBars() == newParentSection.getContainer().getBarRange(newParentSection).size()),
                "spts=%s newParentSection=%s", spts, newParentSection);
        Preconditions.checkArgument(
                newParentSection == null
                || newRhythm == null
                || newParentSection.getData().getTimeSignature() == newRhythm.getTimeSignature(),
                "newRhythm=%s newParentSection=%s", newRhythm, newParentSection);


        ThrowingWriteOperation operation = () -> 
        {
            // Preconditions
            Preconditions.checkArgument(songParts.containsAll(spts), "setSongPartsRhythmOperation() spts=%s this=%s", spts, this);

            LOGGER.log(Level.FINE, "setSongPartsRhythmOperation() -- spts=={0} newRhythm={1} newParentSection={2}", new Object[]
            {
                spts, newRhythm, newParentSection
            });

            if (spts.isEmpty() || (newRhythm == null && newParentSection == null))
            {
                return WriteOperationResults.of(null);
            }


            // Save SongParts data
            final Map<SongPart, SptRhythmChangedEvent.OldData> mapSptOldData = new IdentityHashMap<>();
            final Map<SongPart, SptRhythmChangedEvent.OldData> mapSptNewData = new IdentityHashMap<>();
            for (var spt : spts)
            {
                var oldData = new SptRhythmChangedEvent.OldData(spt.getRhythm(), spt.getParentSection());
                mapSptOldData.put(spt, oldData);
                var newData = new SptRhythmChangedEvent.OldData(newRhythm, newParentSection);
                mapSptNewData.put(spt, newData);
            }
            final Map<TimeSignature, Rhythm> oldMapTsRhythm = new HashMap<>(mapTsLastRhythm);


            // Check for possible veto
            var preCheckEvent = new SptRhythmChangedEvent(this, newRhythm, mapSptOldData, spts);
            preCheckChange(preCheckEvent);        // throws UnsupportedEditException


            // Update model
            final List<PropertyChangeEvent> sptEvents = new ArrayList<>();
            for (var spt : spts)
            {
                sptEvents.add(((SongPartImpl) spt).setRhythm(newRhythm, newParentSection));
            }
            if (newRhythm != null)
            {
                mapTsLastRhythm.put(newRhythm.getTimeSignature(), newRhythm);
            }


            // Prepare events
            final Map<TimeSignature, Rhythm> newMapTsRhythm = new HashMap<>(mapTsLastRhythm);

            
            UndoableEdit edit = new SimpleEdit("Set SongParts rhythm")
            {
                @Override
                public void undoBody()
                {
                    LOGGER.log(Level.FINER, "setSongPartsRhythm.undoBody() songParts={0}", songParts);

                    performWriteAPImethod(() -> 
                    {
                        final List<PropertyChangeEvent> sptEvents2 = new ArrayList<>();
                        for (var spt : spts)
                        {
                            var oldData = mapSptOldData.get(spt);
                            sptEvents2.add(((SongPartImpl) spt).setRhythm(oldData.rhythm(), oldData.parentSection()));
                        }
                        mapTsLastRhythm = new HashMap<>(oldMapTsRhythm);

                        var event = new SptRhythmChangedEvent(SongStructureImpl.this, newRhythm, mapSptNewData, spts);
                        event.setIsUndo();
                        event.addSongPartChanges(sptEvents2);
                        return WriteOperationResults.of(event, null);
                    });
                }

                @Override
                public void redoBody()
                {
                    LOGGER.log(Level.FINER, "setSongPartsRhythm.redoBody() songParts={0}", songParts);

                    performWriteAPImethod(() -> 
                    {
                        final List<PropertyChangeEvent> sptEvents2 = new ArrayList<>();
                        for (var spt : spts)
                        {
                            var newData = mapSptNewData.get(spt);
                            sptEvents2.add(((SongPartImpl) spt).setRhythm(newData.rhythm(), newData.parentSection()));
                        }
                        mapTsLastRhythm = new HashMap<>(newMapTsRhythm);

                        var event = new SptRhythmChangedEvent(SongStructureImpl.this, newRhythm, mapSptOldData, spts);
                        event.setIsRedo();
                        event.addSongPartChanges(sptEvents2);
                        return WriteOperationResults.of(event, null);
                    });
                }
            };

            fireUndoableEditHappened(edit);


            var event = new SptRhythmChangedEvent(SongStructureImpl.this, newRhythm, mapSptOldData, spts);
            event.addSongPartChanges(sptEvents);

            return WriteOperationResults.of(event, null);
        };

        return operation;

    }

    @Override
    public void setSongPartsName(List<SongPart> spts, final String name)
    {
        performWriteAPImethod(setSongPartsNameOperation(spts, name));
    }

    public WriteOperation setSongPartsNameOperation(List<SongPart> spts, final String name)
    {
        Objects.requireNonNull(spts);
        Objects.requireNonNull(name);


        WriteOperation operation = () -> 
        {
            LOGGER.log(Level.FINE, "setSongPartsNameOperation() spts={0} name={1}", new Object[]
            {
                spts, name
            });

            if (spts.isEmpty() || spts.stream().allMatch(spt -> spt.getName().equals(name)))
            {
                return WriteOperationResults.of(null);
            }

            // Update model
            final Map<SongPart, Renaming> mapSptRenaming = new IdentityHashMap<>();
            final List<PropertyChangeEvent> sptEvents = new ArrayList<>();
            for (SongPart spt : spts)
            {
                var renaming = new Renaming(spt.getName(), name);
                mapSptRenaming.put(spt, renaming);
                sptEvents.add(((SongPartImpl) spt).setName(name));
            }


            // Prepare events
            UndoableEdit edit = new SimpleEdit("Rename SongParts")
            {
                @Override
                public void undoBody()
                {
                    LOGGER.log(Level.FINER, "setSongPartsName.undoBody() spts={0} name={1}", new Object[]
                    {
                        spts, name
                    });
                    performWriteAPImethod(() -> 
                    {
                        final List<PropertyChangeEvent> sptEvents2 = new ArrayList<>();
                        for (SongPart spt : mapSptRenaming.keySet())
                        {
                            var renaming = mapSptRenaming.get(spt);
                            sptEvents2.add(((SongPartImpl) spt).setName(renaming.oldName()));
                        }

                        var event = new SptRenamedEvent(SongStructureImpl.this, mapSptRenaming);
                        event.setIsUndo();
                        event.addSongPartChanges(sptEvents2);
                        return WriteOperationResults.of(event, null);
                    });
                }

                @Override
                public void redoBody()
                {
                    LOGGER.log(Level.FINER, "setSongPartsName.redoBody() spts={0} name={1}", new Object[]
                    {
                        spts, name
                    });
                    performWriteAPImethod(() -> 
                    {
                        final List<PropertyChangeEvent> sptEvents2 = new ArrayList<>();
                        for (SongPart spt : mapSptRenaming.keySet())
                        {
                            var renaming = mapSptRenaming.get(spt);
                            sptEvents2.add(((SongPartImpl) spt).setName(renaming.newName()));
                        }

                        var event = new SptRenamedEvent(SongStructureImpl.this, mapSptRenaming);
                        event.setIsRedo();
                        event.addSongPartChanges(sptEvents2);
                        return WriteOperationResults.of(event, null);

                    });
                }
            };

            fireUndoableEditHappened(edit);

            var event = new SptRenamedEvent(this, mapSptRenaming);
            event.addSongPartChanges(sptEvents);
            return WriteOperationResults.of(event, null);

        };

        return operation;
    }


    @Override
    public <T> void setRhythmParameterValue(SongPart spt, final RhythmParameter<T> rp, final T newValue)
    {
        performWriteAPImethod(setRhythmParameterValueOperation(spt, rp, newValue));
    }

    public <T> WriteOperation setRhythmParameterValueOperation(SongPart spt, final RhythmParameter<T> rp, final T newValue)
    {
        Objects.requireNonNull(spt);
        Objects.requireNonNull(rp);
        Objects.requireNonNull(newValue);


        WriteOperation operation = () -> 
        {
            Preconditions.checkArgument(songParts.contains(spt), "setRhythmParameterValueOperation() spt=%s this=%s", spt, this);
            Preconditions.checkArgument(spt.getRhythm().getRhythmParameters().contains(rp), "setRhythmParameterValueOperation() rp=%s spt=%s", rp, spt);

            LOGGER.log(Level.FINE, "setRhythmParameterValueOperation() -- spt={0} rp={1} newValue={2}", new Object[]
            {
                spt, rp, newValue
            });


            final T oldValue = spt.getRPValue(rp);
            if (oldValue.equals(newValue))
            {
                return WriteOperationResults.of(null);
            }

            // Update model
            final SongPartImpl wspt = (SongPartImpl) spt;
            final var sptEvent = wspt.setRPValue(rp, newValue);


            // Prepare events
            UndoableEdit edit = new SimpleEdit("set Rhythm Parameter Value")
            {
                @Override
                public void undoBody()
                {
                    LOGGER.log(Level.FINER, "setRhythmParameterValue.undoBody() spt={0} rp={1} newValue={2}", new Object[]
                    {
                        spt, rp, newValue
                    });
                    performWriteAPImethod(() -> 
                    {
                        var sptEvent2 = wspt.setRPValue(rp, oldValue);
                        var event = new RpValueChangedEvent(SongStructureImpl.this, wspt, rp, oldValue, newValue);
                        event.setIsUndo();
                        event.addSongPartChanges(List.of(sptEvent2));
                        return WriteOperationResults.of(event, null);
                    });
                }

                @Override
                public void redoBody()
                {
                    LOGGER.log(Level.FINER, "setRhythmParameterValue.redoBody() spt={0} rp={1} newValue={2}", new Object[]
                    {
                        spt, rp, newValue
                    });
                    performWriteAPImethod(() -> 
                    {
                        var sptEvent2 = wspt.setRPValue(rp, newValue);
                        var event = new RpValueChangedEvent(SongStructureImpl.this, wspt, rp, oldValue, newValue);
                        event.setIsRedo();
                        event.addSongPartChanges(List.of(sptEvent2));
                        return WriteOperationResults.of(event, null);
                    });
                }
            };

            fireUndoableEditHappened(edit);

            var event = new RpValueChangedEvent(SongStructureImpl.this, wspt, rp, oldValue, newValue);
            event.addSongPartChanges(List.of(sptEvent));
            return WriteOperationResults.of(event, null);
        };

        return operation;
    }

    @Override
    public Rhythm getLastUsedRhythm(TimeSignature ts)
    {
        return performReadAPImethod(() -> mapTsLastRhythm.get(ts));
    }


    @Override
    public Rhythm getRecommendedRhythm(TimeSignature ts, int sptBarIndex)
    {
        Objects.requireNonNull(ts);
        Preconditions.checkArgument(sptBarIndex >= 0 && sptBarIndex <= getSizeInBars(), "sptBarIndex=%s this=%s", sptBarIndex, this);


        LOGGER.log(Level.FINE, "getRecommendedRhythm() ts={0} sptBarIndex={1} sizeInBars={2}", new Object[]
        {
            ts, sptBarIndex, getSizeInBars()
        });


        RhythmDatabase rdb = RhythmDatabase.getDefault();


        record TmpRecord(Rhythm rhythm, SongPart songPart)
                {

        }
        TmpRecord tmp = performReadAPImethod(() -> 
        {
            Rhythm tmpRhythm = mapTsLastRhythm.get(ts);
            SongPart tmpSpt = null;
            if (tmpRhythm == null && !songParts.isEmpty())
            {
                tmpSpt = getSongPart(sptBarIndex > 0 ? sptBarIndex - 1 : sptBarIndex);
            }
            return new TmpRecord(tmpRhythm, tmpSpt);
        });

        Rhythm r = tmp.rhythm();
        SongPart refSpt = tmp.songPart();

        if (refSpt != null)
        {
            Rhythm curRhythm = refSpt.getRhythm();
            if (curRhythm instanceof AdaptedRhythm ar)
            {
                curRhythm = ar.getSourceRhythm();
            }
            r = rdb.getAdaptedRhythmInstance(curRhythm, ts);        // may be null
        }

        if (r == null)
        {
            RhythmInfo ri = null;
            try
            {
                ri = rdb.getDefaultRhythm(ts);
                r = rdb.getRhythmInstance(ri);
            } catch (UnavailableRhythmException ex)
            {
                LOGGER.log(Level.WARNING, "getRecommendedRhythm() Can''t get rhythm instance for {0}. Using stub rhythm instead. ex={1}",
                        new Object[]
                        {
                            ri.name(), ex.getMessage()
                        });
                r = rdb.getDefaultStubRhythmInstance(ts);  // non null
            }
        }

        LOGGER.log(Level.FINE, "getRecommendedRhythm() ts={0} sptBarIndex={1} result r={2}", new Object[]
        {
            ts, sptBarIndex, r
        });

        return r;

    }


    @Override
    public SongPart getSongPart(int absoluteBarIndex)
    {
        return performReadAPImethod(() -> 
        {
            int abi = absoluteBarIndex;
            for (SongPart spt : songParts)
            {
                if (spt.getBarRange().contains(abi))
                {
                    return spt;
                }
            }
            return null;
        });
    }

    @Override
    public List<SongPart> getSongParts(Predicate<SongPart> tester)
    {
        return performReadAPImethod(() -> songParts.stream().filter(tester).toList());
    }

    @Override
    public Position toPosition(float posInBeats)
    {
        return performReadAPImethod(() -> 
        {
            for (SongPart spt : songParts)
            {
                FloatRange rg = toBeatRange(spt.getBarRange());
                if (rg.contains(posInBeats, true))
                {
                    TimeSignature ts = spt.getRhythm().getTimeSignature();
                    float beatInSpt = posInBeats - rg.from;
                    int barOffset = (int) Math.floor(beatInSpt / ts.getNbNaturalBeats());
                    int bar = spt.getStartBarIndex() + barOffset;
                    float beatInBar = posInBeats - rg.from - barOffset * ts.getNbNaturalBeats();
                    return new Position(bar, beatInBar);
                }
            }
            return null;
        });
    }

    @Override
    public float toPositionInNaturalBeats(int barIndex)
    {
        return performReadAPImethod(() -> 
        {
            int size = getSizeInBars();
            Preconditions.checkArgument(barIndex >= 0 && barIndex <= size, "toPositionInNaturalBeats() barIndex=%s size=%s", barIndex, size);

            float posInBeats = 0;
            if (barIndex == getSizeInBars())
            {
                for (SongPart spt : songParts)
                {
                    TimeSignature ts = spt.getParentSection().getData().getTimeSignature();
                    posInBeats += spt.getNbBars() * ts.getNbNaturalBeats();
                }
            } else
            {
                SongPart spt = getSongPart(barIndex);
                for (SongPart spti : songParts)
                {
                    TimeSignature ts = spti.getParentSection().getData().getTimeSignature();
                    if (spti == spt)
                    {
                        posInBeats += (barIndex - spt.getStartBarIndex()) * ts.getNbNaturalBeats();
                        break;
                    }
                    posInBeats += spti.getNbBars() * ts.getNbNaturalBeats();
                }
            }
            return posInBeats;
        });
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
    public String toString()
    {
        return "size=" + getSizeInBars() + " spts=" + songParts;
    }


    /**
     * Fire a change event to all listeners.
     * <p>
     *
     * @param event
     */
    public void fireChangeEvent(SgsChangeEvent event)
    {
        Objects.requireNonNull(event);
        for (SgsChangeListener l : listeners)
        {
            try
            {
                l.songStructureChanged(event);
            } catch (UnsupportedEditException ex)
            {
                // Should never happen. If we're here operation's precheck must have a problem.
                Exceptions.printStackTrace(ex);
            }
        }
    }

    @Override
    public void addSgsChangeListener(SgsChangeListener l)
    {
        Objects.requireNonNull(l);
        listeners.remove(l);
        listeners.add(l);
    }

    @Override
    public void removeSgsChangeListener(SgsChangeListener l)
    {
        Objects.requireNonNull(l);
        listeners.remove(l);
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

    public void preCheckChange(SgsChangeEvent event) throws UnsupportedEditException
    {
        executionManager.preCheckChange(event);
    }

    // -------------------------------------------------------------------------------------------
    // Private methods
    // -------------------------------------------------------------------------------------------
    /**
     * Ensure that all required AdaptedRhythms are generated when the song uses multiple time signatures.
     */
    private void generateAllAdaptedRhythms()
    {
        record TmpRecord(Set<TimeSignature> tsSet, List<Rhythm> rhythmList)
                {

        }

        SharedExecutorServices.getExecutor().submit(() -> 
        {
            TmpRecord tmp = performReadAPImethod(() -> 
            {
                var tsSet = getUniqueRhythms(false, true) // Include AdaptedRhythms to get all time signatures
                        .stream()
                        .map(r -> r.getTimeSignature())
                        .collect(Collectors.toSet());
                var rList = getUniqueRhythms(true, false);  // Exclude AdaptedRhythms
                return new TmpRecord(tsSet, rList);
            });
            Set<TimeSignature> timeSignatures = tmp.tsSet();
            List<Rhythm> uniqueRhythms = tmp.rhythmList();


            RhythmDatabase rdb = RhythmDatabase.getDefault();
            for (Rhythm r : uniqueRhythms)         // No adapted rhythms
            {
                for (TimeSignature ts : timeSignatures)
                {
                    if (!ts.equals(r.getTimeSignature()))
                    {
                        // Have the adapted rhythm created and made available in the database
                        if (rdb.getAdaptedRhythmInstance(r, ts) == null)
                        {
                            LOGGER.log(Level.FINE, "generateAllAdaptedRhythms() no {0}-adapted rhythm for r={1}", new Object[]
                            {
                                ts, r
                            });
                        }
                    }
                }
            }
        });
    }


    private void addSongPartImpl(final SongPart spt)
    {
        LOGGER.log(Level.FINE, "addSongPartInternal() -- spt={0}", spt);
        Objects.requireNonNull(spt);
        Preconditions.checkArgument(spt instanceof SongPartImpl, "addSongPartImpl() spt=%s class=%s", spt, spt.getClass());
        Preconditions.checkArgument(!songParts.contains(spt), "addSongPartImpl() spt=%s", spt);
        Preconditions.checkState(executionManager.isWriteLockedByCurrentThread(), "addSongPartImpl() write lock required");

        int barIndex = spt.getStartBarIndex();
        int nbBars = getSizeInBars();

        int index;
        if (barIndex == nbBars)
        {
            index = songParts.size();
        } else if (barIndex >= 0 && barIndex < nbBars)
        {
            SongPart curSpt = getSongPart(barIndex);
            if (barIndex != curSpt.getStartBarIndex())
            {
                throw new IllegalArgumentException("Invalid size for added SongPart spt: this=" + this + " spt=" + spt + " curSpt=" + curSpt);
            }
            index = songParts.indexOf(curSpt);
        } else
        {
            throw new IllegalArgumentException("this=" + this + " spt=" + spt);
        }


        // Update model
        songParts.add(index, spt);
        ((SongPartImpl) spt).setContainer(this);
        updateStartBarIndexes();
        mapTsLastRhythm.put(spt.getRhythm().getTimeSignature(), spt.getRhythm());

    }

    private void removeSongPartImpl(final SongPart spt)
    {
        LOGGER.log(Level.FINE, "removeSongPartImpl() -- spt={0}", spt);
        Objects.requireNonNull(spt);
        Preconditions.checkArgument(spt instanceof SongPartImpl, "removeSongPartImpl() spt=%s class=%s", spt, spt.getClass());
        Preconditions.checkState(executionManager.isWriteLockedByCurrentThread(), "removeSongPartImpl() write lock required");

        if (!songParts.remove(spt))
        {
            throw new IllegalArgumentException("Can not remove absent SongPart: this=" + this + " spt=" + spt + " songParts=" + songParts);
        }
        updateStartBarIndexes();


    }


    private int getSptLastBarIndex(int sptIndex)
    {
        Preconditions.checkElementIndex(sptIndex, songParts.size(), "getSptLastBarIndex()  sptIndex");
        SongPart spt = songParts.get(sptIndex);
        return spt.getStartBarIndex() + spt.getNbBars() - 1;
    }

    /**
     * Check and possibly update each SongPart's startBarIndex.
     * <p>
     * Must be called under this's lock.
     */
    private void updateStartBarIndexes()
    {
        int barIndex = 0;
        for (SongPart spt : songParts)
        {
            if (spt.getStartBarIndex() != barIndex)
            {
                SongPartImpl wspt = (SongPartImpl) spt;
                wspt.setStartBarIndex(barIndex);
            }
            barIndex += spt.getNbBars();
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


    // ==============================================================================================================
    // Inner classes
    // ==============================================================================================================
    /**
     * This enables XStream instance configuration even for private classes or classes from non-public packages of Netbeans modules.
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
                    xstream.alias("SongStructureImpl", SongStructureImpl.class);
                    xstream.alias("SongStructureImplSP", SongStructureImpl.SerializationProxy.class);

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

    // -----------------------------------------------------------------------
    // Serialization
    // -----------------------------------------------------------------------
    private Object writeReplace()
    {
        return new SerializationProxy(this);
    }

    private void readObject(ObjectInputStream stream) throws InvalidObjectException
    {
        throw new InvalidObjectException("Serialization proxy required");
    }

    /**
     * Need to restore each item's container. Allow to be independent of future chordleadsheet internal data structure changes.
     * <p>
     * spVERSION 2 (JJazzLab 4.1.0) introduces several aliases to get rid of hard-coded qualified class names (XStreamConfig class introduction).<br>
     * spVERSION 3 spKeepUpdated is no longer used
     */
    private static class SerializationProxy implements Serializable
    {

        private static final long serialVersionUID = -76876542017265L;
        private final int spVERSION = 3;
        private final ArrayList<SongPart> spSpts;
        private final ChordLeadSheet spParentCls;
        private boolean spKeepUpdated;          // NOT USED FROM spVERSION 3

        private SerializationProxy(SongStructureImpl sgs)
        {
            spParentCls = sgs.getParentChordLeadSheet();
            spSpts = new ArrayList<>();
            spSpts.addAll(sgs.getSongParts());
        }

        private Object readResolve() throws ObjectStreamException
        {
            if (spSpts == null)
            {
                throw new IllegalStateException("spSpts=" + spSpts);
            }

            SongStructureImpl sgs = new SongStructureImpl(spParentCls);
            try
            {
                sgs.addSongParts(spSpts);
            } catch (UnsupportedEditException ex)
            {
                // Translate to an ObjectStreamException
                throw new InvalidObjectException(ex.getMessage());
            }
            return sgs;
        }
    }

}

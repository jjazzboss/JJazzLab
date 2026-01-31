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
import java.beans.PropertyChangeListener;
import org.jjazz.songstructure.api.event.RpValueChangedEvent;
import org.jjazz.songstructure.api.event.SptReplacedEvent;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;
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
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.harmony.api.Position;
import org.jjazz.rhythm.api.AdaptedRhythm;
import org.jjazz.rhythmdatabase.api.RhythmInfo;
import org.jjazz.rhythmdatabase.api.UnavailableRhythmException;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SgsChangeListener;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.event.SgsVetoableChangeEvent;
import org.jjazz.utilities.api.FloatRange;
import org.jjazz.utilities.api.IntRange;
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
 * Implementation is thread-safe and uses the same concurrency design than ChordLeadSheet. Parent ChordLeadSheet's lock is reused (also used by the enclosing
 * Song).
 */
public class SongStructureImpl implements SongStructure, Serializable, PropertyChangeListener
{

    private final transient ReentrantReadWriteLock lock;
    /**
     * Keep SongParts ordered by startBarIndex.
     */
    private ArrayList<SongPart> songParts;
    /**
     * Our parent ChordLeadSheet.
     */
    private ChordLeadSheet parentCls;
    /**
     * Keep the last Rhythm used for each TimeSignature.
     */
    private transient Map<TimeSignature, Rhythm> mapTsLastRhythm;
    /**
     * The listeners for changes.
     */
    private final transient CopyOnWriteArrayList<SgsChangeListener> listeners;
    private final transient CopyOnWriteArrayList<SgsChangeListener> syncListeners;
    /**
     * The listeners for undoable edits.
     */
    private final transient CopyOnWriteArrayList<UndoableEditListener> undoListeners;
    private static final Logger LOGGER = Logger.getLogger(SongStructureImpl.class.getSimpleName());


    public SongStructureImpl()
    {
        this(null);
    }

    /**
     *
     * @param cls The parent chordleadsheet
     */
    public SongStructureImpl(ChordLeadSheet cls)
    {
        parentCls = cls;
        songParts = new ArrayList<>();
        mapTsLastRhythm = new HashMap<>();
        listeners = new CopyOnWriteArrayList<>();
        syncListeners = new CopyOnWriteArrayList<>();
        undoListeners = new CopyOnWriteArrayList<>();
        lock = cls == null ? new ReentrantReadWriteLock() : cls.getLock();  // Song/ChordLeadSheet/SongSTructure all share the same lock
    }


    @Override
    public SongStructureImpl getDeepCopy(ChordLeadSheet parentCls)
    {
        SongStructureImpl res = new SongStructureImpl(parentCls);

        lock.readLock().lock();
        try
        {
            var newSpts = getSongParts().stream()
                    .map(spt -> 
                    {
                        CLI_Section oldParentSection = spt.getParentSection();
                        CLI_Section newParentSection = null;
                        if (oldParentSection != null && parentCls != null)
                        {
                            newParentSection = parentCls.getSection(oldParentSection.getData().getName());
                        }
                        return spt.getCopy(null, spt.getStartBarIndex(), spt.getNbBars(), newParentSection);
                    })
                    .toList();
            res.songParts.addAll(newSpts);

            res.mapTsLastRhythm = new HashMap<>(mapTsLastRhythm);
        } finally
        {
            lock.readLock().unlock();
        }

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
    @SuppressWarnings(
            {
                "unchecked"
            })
    @Override
    public List<SongPart> getSongParts()
    {
        lock.readLock().lock();
        try
        {
            return List.copyOf(songParts);
        } finally
        {
            lock.readLock().unlock();
        }
    }

    @Override
    public int getSizeInBars()
    {
        lock.readLock().lock();
        try
        {
            return songParts.isEmpty() ? 0 : getSptLastBarIndex(songParts.size() - 1) + 1;
        } finally
        {
            lock.readLock().unlock();
        }
    }

    @Override
    public FloatRange toBeatRange(IntRange rg)
    {
        lock.readLock().lock();
        try
        {
            if (getSizeInBars() == 0)
            {
                return FloatRange.EMPTY_FLOAT_RANGE;
            }

            IntRange songRange = new IntRange(0, getSizeInBars() - 1);
            if (rg == null)
            {
                rg = songRange;
            } else if (!songRange.contains(rg))
            {
                return FloatRange.EMPTY_FLOAT_RANGE;
            }

            float startPos = -1;
            float endPos = -1;
            for (SongPart spt : songParts)
            {
                TimeSignature ts = spt.getRhythm().getTimeSignature();
                IntRange ir = rg.getIntersection(spt.getBarRange());
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
        } finally
        {
            lock.readLock().unlock();
        }
    }

    @Override
    public SongPart createSongPart(Rhythm r, String name, int startBarIndex, int nbBars, CLI_Section parentSection, boolean reusePrevParamValues)
    {
        if (r == null || startBarIndex < 0 || nbBars < 0 || name == null)
        {
            throw new IllegalArgumentException("r=" + r + " name=" + name
                    + " startBarIndex=" + startBarIndex + " nbBars=" + nbBars
                    + " parentSection=" + parentSection + " reusePrevParamValues=" + reusePrevParamValues);
        }

        SongPartImpl spt;

        if (startBarIndex > 0 && reusePrevParamValues)
        {
            // New song part which reuse parameters from previous song part
            SongPart prevSpt = getSongPart(startBarIndex - 1);
            spt = (SongPartImpl) prevSpt.getCopy(r, startBarIndex, nbBars, parentSection);

        } else
        {
            // New song part with default RP values
            spt = new SongPartImpl(r, startBarIndex, nbBars, parentSection);
            spt.setContainer(this);

        }

        spt.setName(name);

        return spt;
    }


    @Override
    public void addSongParts(final List<SongPart> spts) throws UnsupportedEditException
    {
        Objects.requireNonNull(spts);

        LOGGER.log(Level.FINE, "addSongParts() -- spts={0}", spts);

        if (spts.isEmpty())
        {
            return;
        }

        ThrowingSupplier<OperationResults, UnsupportedEditException> operation = () -> 
        {
            testChangeEventForVeto(new SptAddedEvent(this, spts));           // Possible exception here        

            final ArrayList<SongPart> oldSpts = new ArrayList<>(songParts);
        final Map<TimeSignature, Rhythm> oldMapTsRhythm = new HashMap<>(mapTsLastRhythm);
            final Map<SongPart, SongStructure> oldContainers = new HashMap<>();
            for (SongPart spt : spts)
            {
                oldContainers.put(spt, spt.getContainer());
            }

            for (SongPart spt : spts)
            {
                addSongPartImpl(spt);
            }

            final ArrayList<SongPart> newSpts = new ArrayList<>(songParts);
            final Map<TimeSignature, Rhythm> newMapTsRhythm = new HashMap<>(mapTsLastRhythm);

            UndoableEdit edit = new SimpleEdit("Add SongParts")
            {
                @Override
                public void undoBody()
                {
                    LOGGER.log(Level.FINER, "addSongParts.undoBody() spts={0}", spts);
                    performAPImethodUndoRedo(() -> 
                    {
                        songParts = new ArrayList<>(oldSpts);
                        mapTsLastRhythm = new HashMap<>(oldMapTsRhythm);
                        updateStartBarIndexes();
                        for (SongPart spt : spts)
                        {
                            ((SongPartImpl) spt).setContainer(oldContainers.get(spt));
                            spt.removePropertyChangeListener(SongStructureImpl.this);
                        }
                        var event = new SptRemovedEvent(SongStructureImpl.this, spts);
                        event.setIsUndo();
                        return event;
                    });
                }

                @Override
                public void redoBody()
                {
                    LOGGER.log(Level.FINER, "addSongParts.redoBody() spts={0}", spts);
                    performAPImethodUndoRedo(() -> 
                    {
                        songParts = new ArrayList<>(newSpts);
                        mapTsLastRhythm = new HashMap<>(newMapTsRhythm);
                        updateStartBarIndexes();
                        for (SongPart spt : spts)
                        {
                            ((SongPartImpl) spt).setContainer(SongStructureImpl.this);
                            spt.addPropertyChangeListener(SongStructureImpl.this);
                        }
                        var event = new SptAddedEvent(SongStructureImpl.this, spts);
                        event.setIsRedo();
                        return event;
                    });
                }
            };

            return new OperationResults(new SptAddedEvent(this, spts), edit, spts.size());
        };

        performAPImethodThrowing(operation);

        // Make sure all AdaptedRhythms for the song rhythms are generated in the database so that user can 
        // access them if he wants too
        generateAllAdaptedRhythms();
    }

    @Override
    public void removeSongParts(List<SongPart> spts)
    {
        Objects.requireNonNull(spts);

        LOGGER.log(Level.FINE, "removeSongParts() -- spts={0}", spts);

        if (spts.isEmpty())
        {
            return;
        }

        Supplier<OperationResults> operation = () -> 
        {
            final ArrayList<SongPart> oldSpts = new ArrayList<>(songParts);
            final Map<TimeSignature, Rhythm> oldMapTsRhythm = new HashMap<>(mapTsLastRhythm);

            removeSongPartsImpl(spts);

            final ArrayList<SongPart> newSpts = new ArrayList<>(songParts);
            final Map<TimeSignature, Rhythm> newMapTsRhythm = new HashMap<>(mapTsLastRhythm);

            UndoableEdit edit = new SimpleEdit("Remove SongParts")
            {
                @Override
                public void undoBody()
                {
                    LOGGER.log(Level.FINER, "removeSongParts.undoBody() spts={0}", spts);
                    performAPImethodUndoRedo(() -> 
                    {
                        songParts = new ArrayList<>(oldSpts);
                        mapTsLastRhythm = new HashMap<>(oldMapTsRhythm);
                        updateStartBarIndexes();
                        spts.forEach(spt -> spt.addPropertyChangeListener(SongStructureImpl.this));
                        var event = new SptAddedEvent(SongStructureImpl.this, spts);
                        event.setIsUndo();
                        return event;
                    });
                }

                @Override
                public void redoBody()
                {
                    LOGGER.log(Level.FINER, "removeSongParts.redoBody() spts={0}", spts);
                    performAPImethodUndoRedo(() -> 
                    {
                        songParts = new ArrayList<>(newSpts);
                        mapTsLastRhythm = new HashMap<>(newMapTsRhythm);
                        updateStartBarIndexes();
                        spts.forEach(spt -> spt.removePropertyChangeListener(SongStructureImpl.this));
                        var event = new SptRemovedEvent(SongStructureImpl.this, spts);
                        event.setIsRedo();
                        return event;
                    });
                }
            };

            return new OperationResults(new SptRemovedEvent(this, spts), edit, spts.size());
        };

        performAPImethod(operation);
    }


    @Override
    public void resizeSongParts(Map<SongPart, Integer> mapSptSize)
    {
        Objects.requireNonNull(mapSptSize);

        LOGGER.log(Level.FINE, "resizeSongParts() -- mapSptSize={0}", mapSptSize);

        if (mapSptSize.isEmpty())
        {
            return;
        }

        Supplier<OperationResults> operation = () -> 
        {
            final Map<SongPart, Integer> saveMap = new HashMap<>(mapSptSize);
            final Map<SongPart, Integer> oldMap = new HashMap<>();

            for (SongPart spt : mapSptSize.keySet())
            {
                if (!songParts.contains(spt))
                {
                    throw new IllegalArgumentException("this=" + this + " spt=" + spt + " mapSptsSize=" + mapSptSize);
                }
                oldMap.put(spt, spt.getNbBars());
                SongPartImpl wspt = (SongPartImpl) spt;
                wspt.setNbBars(mapSptSize.get(spt));
            }
            updateStartBarIndexes();

            UndoableEdit edit = new SimpleEdit("Resize SongParts")
            {
                @Override
                public void undoBody()
                {
                    LOGGER.log(Level.FINER, "resizeSongParts.undoBody() mapSptSize={0}", mapSptSize);
                    performAPImethodUndoRedo(() -> 
                    {
                        for (SongPart spt : oldMap.keySet())
                        {
                            ((SongPartImpl) spt).setNbBars(oldMap.get(spt));
                        }
                        updateStartBarIndexes();
                        var event = new SptResizedEvent(SongStructureImpl.this, saveMap);
                        event.setIsUndo();
                        return event;
                    });
                }

                @Override
                public void redoBody()
                {
                    LOGGER.log(Level.FINER, "resizeSongParts.redoBody() mapSptSize={0}", mapSptSize);
                    performAPImethodUndoRedo(() -> 
                    {
                        for (SongPart spt : saveMap.keySet())
                        {
                            ((SongPartImpl) spt).setNbBars(saveMap.get(spt));
                        }
                        updateStartBarIndexes();
                        var event = new SptResizedEvent(SongStructureImpl.this, oldMap);
                        event.setIsRedo();
                        return event;
                    });
                }
            };

            var event = new SptResizedEvent(this, oldMap);
            return new OperationResults(event, edit, mapSptSize.size());
        };

        performAPImethod(operation);
    }


    /**
     * Test if a change is authorized by listeners.
     *
     * @param event Can not be a SgsVetoableChangeEvent
     * @throws UnsupportedEditException If change is vetoed by a listener
     */
    @Override
    public void testChangeEventForVeto(SgsChangeEvent event) throws UnsupportedEditException
    {
        Objects.requireNonNull(event);
        Preconditions.checkArgument(event.getSource() == this, "event=%s", event);
        SgsVetoableChangeEvent svce;
        if (event instanceof SgsVetoableChangeEvent ve)
        {
            svce = ve;
        } else
        {
            svce = new SgsVetoableChangeEvent(this, event);
        }

        for (SgsChangeListener l : syncListeners)
        {
            l.songStructureChanged(svce);   // Possible exception here
        }
    }

    /**
     * We need a method that works with a list of SongParts because replacing a single spt at a time may cause problems when there is an
     * UnsupportedEditException.
     * <p>
     * Example: We have spt1=rhythm0, spt2=rhythm0, spt3=rhythm1. There are enough Midi channels for both rhythms.<br>
     * We want to change rhythm of both spt1 and spt2. If we do one spt at a time, after the first replacement on spt0 we'll have 3 rhythms and possibly our
     * listeners will trigger an UnsupportedEditException (if not enough Midi channels), though there should be no problem since we want to change both spt1 AND
     * spt2 !
     *
     * @param oldSpts
     * @param newSpts
     * @throws UnsupportedEditException The exception will be thrown before any change is done.
     */
    @Override
    public void replaceSongParts(final List<SongPart> oldSpts, final List<SongPart> newSpts) throws UnsupportedEditException
    {
        Objects.requireNonNull(oldSpts);
        Objects.requireNonNull(newSpts);
        Preconditions.checkArgument(oldSpts.size() == newSpts.size(), "oldSpts=%s, newSpts=%s", oldSpts, newSpts);


        LOGGER.log(Level.FINE, "replaceSongParts() -- oldSpts=={0} newSpts={1}", new Object[]
        {
            oldSpts.toString(), newSpts.toString()
        });

        ThrowingSupplier<OperationResults, UnsupportedEditException> operation = () -> 
        {
            for (int i = 0; i < oldSpts.size(); i++)
            {
                SongPart oldSpt = oldSpts.get(i);
                SongPart newSpt = newSpts.get(i);
                if (!songParts.contains(oldSpt) || ((oldSpt != newSpt) && songParts.contains(newSpt))
                        || oldSpt.getStartBarIndex() != newSpt.getStartBarIndex()
                        || oldSpt.getNbBars() != newSpt.getNbBars())
                {
                    throw new IllegalArgumentException("this=" + this + " oldSpts=" + oldSpts + " newSpts=" + newSpts);
                }
            }
            if (oldSpts.equals(newSpts))
            {
                return new OperationResults(null, null, Boolean.FALSE);
            }

            testChangeEventForVeto(new SptReplacedEvent(this, oldSpts, newSpts));           // Possible exception here        

            final Map<TimeSignature, Rhythm> oldMapTsRhythm = new HashMap<>(mapTsLastRhythm);
            final ArrayList<SongPart> oldSongParts = new ArrayList<>(songParts);
            final ArrayList<SongStructure> newSptsOldContainer = new ArrayList<>();

            for (int i = 0; i < oldSpts.size(); i++)
            {
                SongPart oldSpt = oldSpts.get(i);
                SongPart newSpt = newSpts.get(i);

                newSptsOldContainer.add(newSpt.getContainer());

                int rpIndex = songParts.indexOf(oldSpt);
                songParts.set(rpIndex, newSpt);
                ((SongPartImpl) newSpt).setContainer(this);

                Rhythm r = newSpt.getRhythm();
                TimeSignature ts = r.getTimeSignature();
                mapTsLastRhythm.put(ts, r);

                oldSpt.removePropertyChangeListener(this);
                newSpt.addPropertyChangeListener(this);
            }

            final ArrayList<SongPart> newSongParts = new ArrayList<>(songParts);
            final Map<TimeSignature, Rhythm> newMapTsRhythm = new HashMap<>(mapTsLastRhythm);

            UndoableEdit edit = new SimpleEdit("Replace SongParts")
            {
                @Override
                public void undoBody()
                {
                    LOGGER.log(Level.FINER, "ReplaceSongParts.undoBody() songParts={0}", songParts);

                    performAPImethodUndoRedo(() -> 
                    {
                        songParts = new ArrayList<>(oldSongParts);
                        mapTsLastRhythm = new HashMap<>(oldMapTsRhythm);
                        for (int i = 0; i < newSpts.size(); i++)
                        {
                            var newSpt = newSpts.get(i);
                            var oldSpt = oldSpts.get(i);
                            SongStructure sgs = newSptsOldContainer.get(i);
                            ((SongPartImpl) newSpt).setContainer(sgs);
                            newSpt.removePropertyChangeListener(SongStructureImpl.this);
                            oldSpt.addPropertyChangeListener(SongStructureImpl.this);
                        }
                        var event = new SptReplacedEvent(SongStructureImpl.this, newSpts, oldSpts);
                        event.setIsUndo();
                        return event;
                    });
                }

                @Override
                public void redoBody()
                {
                    LOGGER.log(Level.FINER, "ReplaceSongParts.redoBody() songParts={0}", songParts);

                    performAPImethodUndoRedo(() -> 
                    {
                        songParts = new ArrayList<>(newSongParts);
                        mapTsLastRhythm = new HashMap<>(newMapTsRhythm);
                        for (int i = 0; i < newSpts.size(); i++)
                        {
                            var newSpt = newSpts.get(i);
                            var oldSpt = oldSpts.get(i);
                            ((SongPartImpl) newSpt).setContainer(SongStructureImpl.this);
                            newSpt.addPropertyChangeListener(SongStructureImpl.this);
                            oldSpt.removePropertyChangeListener(SongStructureImpl.this);
                        }
                        var event = new SptReplacedEvent(SongStructureImpl.this, oldSpts, newSpts);
                        event.setIsRedo();
                        return event;
                    });
                }
            };

            return new OperationResults(new SptReplacedEvent(SongStructureImpl.this, oldSpts, newSpts), edit, newSpts.size());
        };

        performAPImethodThrowing(operation);

        generateAllAdaptedRhythms();
    }


    @Override
    public void setSongPartsName(List<SongPart> spts, final String name)
    {
        Objects.requireNonNull(spts);
        Objects.requireNonNull(name);

        LOGGER.log(Level.FINE, "setSongPartsName() spts={0} name={1}", new Object[]
        {
            spts, name
        });

        if (spts.isEmpty() || spts.stream().allMatch(spt -> spt.getName().equals(name)))
        {
            return;
        }

        Supplier<OperationResults> operation = () -> 
        {
            final Map<SongPart, String> saveMap = new HashMap<>();
            for (SongPart spt : spts)
            {
                SongPartImpl wspt = (SongPartImpl) spt;
                saveMap.put(wspt, wspt.getName());
                wspt.setName(name);
            }

            UndoableEdit edit = new SimpleEdit("Rename SongParts")
            {
                @Override
                public void undoBody()
                {
                    LOGGER.log(Level.FINER, "setSongPartsName.undoBody() spts={0} name={1}", new Object[]
                    {
                        spts, name
                    });
                    performAPImethodUndoRedo(() -> 
                    {
                        for (SongPart wspt : saveMap.keySet())
                        {
                            ((SongPartImpl) wspt).setName(saveMap.get(wspt));
                        }
                        var event = new SptRenamedEvent(SongStructureImpl.this, spts);
                        event.setIsUndo();
                        return event;
                    });
                }

                @Override
                public void redoBody()
                {
                    LOGGER.log(Level.FINER, "setSongPartsName.redoBody() spts={0} name={1}", new Object[]
                    {
                        spts, name
                    });
                    performAPImethodUndoRedo(() -> 
                    {
                        for (SongPart wspt : saveMap.keySet())
                        {
                            ((SongPartImpl) wspt).setName(name);
                        }
                        var event = new SptRenamedEvent(SongStructureImpl.this, spts);
                        event.setIsRedo();
                        return event;
                    });
                }
            };

            return new OperationResults(new SptRenamedEvent(this, spts), edit, spts.size());
        };

        performAPImethod(operation);
    }


    @Override
    public <T> void setRhythmParameterValue(SongPart spt, final RhythmParameter<T> rp, final T newValue)
    {
        Objects.requireNonNull(spt);
        Objects.requireNonNull(rp);
        Objects.requireNonNull(newValue);

        LOGGER.log(Level.FINE, "setRhythmParameterValue() -- spt={0} rp={1} newValue={2}", new Object[]
        {
            spt, rp, newValue
        });

        Supplier<OperationResults> operation = () -> 
        {
            if (!songParts.contains(spt) || !spt.getRhythm().getRhythmParameters().contains(rp))
            {
                throw new IllegalArgumentException("this=" + this + " spt=" + spt + " rp=" + rp + " newValue=" + newValue);
            }

            final T oldValue = spt.getRPValue(rp);
            if (oldValue.equals(newValue))
            {
                return new OperationResults(null, null, Boolean.FALSE);
            }

            final SongPartImpl wspt = (SongPartImpl) spt;
            wspt.setRPValue(rp, newValue);

            UndoableEdit edit = new SimpleEdit("set Rhythm Parameter Value")
            {
                @Override
                public void undoBody()
                {
                    LOGGER.log(Level.FINER, "setRhythmParameterValue.undoBody() spt={0} rp={1} newValue={2}", new Object[]
                    {
                        spt, rp, newValue
                    });
                    performAPImethodUndoRedo(() -> 
                    {
                        wspt.setRPValue(rp, oldValue);
                        var event = new RpValueChangedEvent(SongStructureImpl.this, wspt, rp, newValue, oldValue);
                        event.setIsUndo();
                        return event;
                    });
                }

                @Override
                public void redoBody()
                {
                    LOGGER.log(Level.FINER, "setRhythmParameterValue.redoBody() spt={0} rp={1} newValue={2}", new Object[]
                    {
                        spt, rp, newValue
                    });
                    performAPImethodUndoRedo(() -> 
                    {
                        wspt.setRPValue(rp, newValue);
                        var event = new RpValueChangedEvent(SongStructureImpl.this, wspt, rp, oldValue, newValue);
                        event.setIsRedo();
                        return event;
                    });
                }
            };

            return new OperationResults(new RpValueChangedEvent(SongStructureImpl.this, wspt, rp, oldValue, newValue), edit, Boolean.TRUE);
        };

        performAPImethod(operation);
    }


    @Override
    public Rhythm getLastUsedRhythm(TimeSignature ts)
    {
        lock.readLock().lock();
        try
        {
            Rhythm r = mapTsLastRhythm.get(ts);
            LOGGER.log(Level.FINE, "getLastUsedRhythm() ts={0} result r={1}", new Object[]
            {
                ts, r
            });
            return r;
        } finally
        {
            lock.readLock().unlock();
        }
    }


    @Override
    public Rhythm getRecommendedRhythm(TimeSignature ts, int sptBarIndex)
    {
        if (ts == null || sptBarIndex < 0 || sptBarIndex > getSizeInBars())
        {
            throw new IllegalArgumentException("ts=" + ts + " sptBarIndex=" + sptBarIndex + " getSizeInBars()=" + getSizeInBars());
        }

        RhythmDatabase rdb = RhythmDatabase.getDefault();

        LOGGER.log(Level.FINE, "getRecommendedRhythm() ts={0} sptBarIndex={1} sizeInBars={2}", new Object[]
        {
            ts, sptBarIndex, getSizeInBars()
        });

        lock.readLock().lock();
        try
        {
            Rhythm r = mapTsLastRhythm.get(ts);

            if (r == null && !songParts.isEmpty())
            {
                SongPart refSpt = getSongPart(sptBarIndex > 0 ? sptBarIndex - 1 : sptBarIndex);
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
        } finally
        {
            lock.readLock().unlock();
        }
    }


    @Override
    public SongPart getSongPart(int absoluteBarIndex)
    {
        lock.readLock().lock();
        try
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
        } finally
        {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<SongPart> getSongParts(Predicate<SongPart> tester)
    {
        lock.readLock().lock();
        try
        {
            return songParts.stream().filter(tester).toList();
        } finally
        {
            lock.readLock().unlock();
        }
    }

    @Override
    public Position toPosition(float posInBeats)
    {
        if (posInBeats < 0)
        {
            throw new IllegalArgumentException("posInBeats=" + posInBeats);
        }
        lock.readLock().lock();
        try
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
        } finally
        {
            lock.readLock().unlock();
        }
    }

    @Override
    public float toPositionInNaturalBeats(int barIndex)
    {
        lock.readLock().lock();
        try
        {
            int size = getSizeInBars();
            Preconditions.checkArgument(barIndex >= 0 && barIndex < size, "barIndex=%s size=%s", barIndex, size);

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
        } finally
        {
            lock.readLock().unlock();
        }
    }

    @Override
    public Position getSptItemPosition(SongPart spt, ChordLeadSheetItem<?> clsItem)
    {
        if (parentCls == null)
        {
            throw new IllegalStateException("parentCls is null. spt=" + spt + " clsItem=" + clsItem);
        }
        CLI_Section section = spt.getParentSection();
        if (!parentCls.getItems(spt.getParentSection(), clsItem.getClass()).contains(clsItem))
        {
            throw new IllegalArgumentException(
                    "clsItem=" + clsItem + " not found in parent section items. section=" + section + ", spt=" + spt);
        }
        Position pos = clsItem.getPosition();
        int relBar = pos.getBar() - section.getPosition().getBar();
        Position res = new Position(spt.getStartBarIndex() + relBar, pos.getBeat());
        return res;
    }


    @Override
    public String toString()
    {
        return "size=" + getSizeInBars() + " spts=" + songParts;
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

    public void addSgsChangeSyncListener(SgsChangeListener l)
    {
        Objects.requireNonNull(l);
        syncListeners.remove(l);
        syncListeners.add(l);
    }

    public void removeSgsChangeSyncListener(SgsChangeListener l)
    {
        Objects.requireNonNull(l);
        syncListeners.remove(l);
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

    //=============================================================================
    // PropertyChangeListener interface
    //=============================================================================

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getSource() instanceof SongPart spt)
        {
            if (evt.getPropertyName().equals(SongPart.PROP_RP_MUTABLE_VALUE))
            {
                @SuppressWarnings("unchecked")
                RhythmParameter<?> rp = (RhythmParameter<?>) evt.getOldValue();
                Object rpValue = evt.getNewValue();

                Supplier<OperationResults> operation = () -> new OperationResults(
                        new RpValueChangedEvent(SongStructureImpl.this, spt, rp, null, rpValue),
                        null,
                        Boolean.TRUE);

                performAPImethod(operation);
            }
        }
    }

    // -------------------------------------------------------------------------------------------
    // Private methods
    // -------------------------------------------------------------------------------------------

    /**
     * Make sure all possible AdaptedRhythms are generated if this is a multi-time signature song.
     */
    private void generateAllAdaptedRhythms()
    {
        RhythmDatabase rdb = RhythmDatabase.getDefault();
        Set<TimeSignature> timeSignatures = getUniqueRhythms(false, true) // Include AdaptedRhythms to get all time signatures
                .stream()
                .map(r -> r.getTimeSignature())
                .collect(Collectors.toSet());

        for (Rhythm r : getUniqueRhythms(true, false))         // No adapted rhythms
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
    }


    /**
     * Internal implementation, no checks performed.
     *
     * @param spt
     */
    private void addSongPartImpl(final SongPart spt)
    {
        LOGGER.log(Level.FINE, "addSongPartInternal() -- spt={0}", spt);

        Preconditions.checkState(lock.isWriteLockedByCurrentThread(), "write lock required");

        int barIndex = spt.getStartBarIndex();
        if (songParts.contains(spt) || barIndex > getSizeInBars())
        {
            throw new IllegalArgumentException("this=" + this + " spt=" + spt);
        }

        if (barIndex == getSizeInBars())
        {
            songParts.add(spt);
        } else
        {
            SongPart curSpt = getSongPart(barIndex);
            if (barIndex != curSpt.getStartBarIndex())
            {
                throw new IllegalArgumentException("this=" + this + " spt=" + spt + " curSpt=" + curSpt);
            }
            int rpIndex = songParts.indexOf(curSpt);
            songParts.add(rpIndex, spt);
        }

        updateStartBarIndexes();
        ((SongPartImpl) spt).setContainer(this);
        mapTsLastRhythm.put(spt.getRhythm().getTimeSignature(), spt.getRhythm());
        spt.addPropertyChangeListener(this);
    }

    /**
     * Internal implementation, no checks performed.
     *
     * @param spts
     */
    protected void removeSongPartsImpl(List<SongPart> spts)
    {
        Objects.requireNonNull(spts);

        LOGGER.log(Level.FINER, "removeSongPartInternal() -- spts={0}", spts);

        Preconditions.checkState(lock.isWriteLockedByCurrentThread(), "write lock required");

        for (SongPart spt : spts)
        {
            if (!songParts.remove(spt))
            {
                throw new IllegalArgumentException("this=" + this + " spt=" + spt + " songParts=" + songParts);
            }
            spt.removePropertyChangeListener(this);
        }
        updateStartBarIndexes();
    }


    private int getSptLastBarIndex(int sptIndex)
    {
        Preconditions.checkElementIndex(0, songParts.size(), "sptIndex");
        SongPart spt = songParts.get(sptIndex);
        return spt.getStartBarIndex() + spt.getNbBars() - 1;
    }

    /**
     * Check and possibly update each SongPart's startBarIndex. Must be called under this's lock.
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


    /**
     * Fire a non vetoable change event to all (non synchronized) listeners.
     * <p>
     *
     * @param event Can not be a SgsVetoableChangeEvent
     */
    private void fireNonVetoableChangeEvent(SgsChangeEvent event)
    {
        Preconditions.checkArgument(event != null && !(event instanceof SgsVetoableChangeEvent), "event=%s", event);

        for (SgsChangeListener l : listeners)
        {
            try
            {
                l.songStructureChanged(event);
            } catch (UnsupportedEditException ex)
            {
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
    private void fireSynchronizedNonVetoableChangeEvent(SgsChangeEvent event)
    {
        Preconditions.checkArgument(event != null && !(event instanceof SgsVetoableChangeEvent), "event=%s", event);

        for (SgsChangeListener l : syncListeners)
        {
            try
            {
                l.songStructureChanged(event);
            } catch (UnsupportedEditException ex)
            {
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
            if (results.sgsChangeEvent() != null)
            {
                fireSynchronizedNonVetoableChangeEvent(results.sgsChangeEvent());
            }
        } finally
        {
            lock.writeLock().unlock();
        }

        if (results.sgsChangeEvent() != null)
        {
            fireNonVetoableChangeEvent(results.sgsChangeEvent());
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
            results = operation.get();
            assert results != null;

            if (results.undoableEdit() != null)
            {
                fireUndoableEditHappened(results.undoableEdit());
            }
            if (results.sgsChangeEvent() != null)
            {
                fireSynchronizedNonVetoableChangeEvent(results.sgsChangeEvent());
            }
        } finally
        {
            lock.writeLock().unlock();
        }

        if (results.sgsChangeEvent() != null)
        {
            fireNonVetoableChangeEvent(results.sgsChangeEvent());
        }

        @SuppressWarnings("unchecked")
        R returnValue = (R) results.returnValue();
        return returnValue;
    }

    /**
     * Safely perform an undo or redo operation for mutating API method.
     *
     * @param operation Updates model and return a SgsChangeEvent
     */
    private void performAPImethodUndoRedo(Supplier<SgsChangeEvent> operation)
    {
        SgsChangeEvent event;
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

    @FunctionalInterface
    private interface ThrowingSupplier<T, E extends Exception>
    {

        T get() throws E;
    }

    /**
     * Helper class to store the events and return value produced by a mutating API method.
     */
    private record OperationResults(SgsChangeEvent sgsChangeEvent, UndoableEdit undoableEdit, Object returnValue)
            {

    }

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
     * spVERSION 3 spKeepUpdated no used anymore
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

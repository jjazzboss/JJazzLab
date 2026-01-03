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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
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
import org.jjazz.songstructure.api.event.SgsActionEvent;
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

public class SongStructureImpl implements SongStructure, Serializable, PropertyChangeListener
{

    /**
     * Keep SongParts ordered by startBarIndex.
     */
    private ArrayList<SongPart> songParts;
    /**
     * Our parent ChordLeadSheet.
     */
    private ChordLeadSheet parentCls;
    private transient SgsActionEvent activeSgsActionEvent;
    /**
     * Keep the last Rhythm used for each TimeSignature.
     */
    private transient Map<TimeSignature, Rhythm> mapTsLastRhythm;
    /**
     * The listeners for changes.
     */
    private final transient List<SgsChangeListener> listeners;
    /**
     * The listeners for undoable edits.
     */
    private final transient List<UndoableEditListener> undoListeners;
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
        listeners = new ArrayList<>();
        undoListeners = new ArrayList<>();
    }


    @Override
    public synchronized SongStructureImpl getDeepCopy(ChordLeadSheet parentCls)
    {
        SongStructureImpl res = new SongStructureImpl(parentCls);

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
    public synchronized List<SongPart> getSongParts()
    {
        return (List<SongPart>) songParts.clone();
    }

    @Override
    public synchronized int getSizeInBars()
    {
        int res = songParts.isEmpty() ? 0 : getSptLastBarIndex(songParts.size() - 1) + 1;
        return res;
    }

    @Override
    public FloatRange toBeatRange(IntRange rg)
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
        for (SongPart spt : getSongParts())     // Synchronized
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
    public synchronized void addSongParts(final List<SongPart> spts) throws UnsupportedEditException
    {
        Objects.requireNonNull(spts);

        LOGGER.log(Level.FINE, "addSongParts() -- spts={0}", spts);

        if (spts.isEmpty())
        {
            return;
        }

        testChangeEventForVeto(new SptAddedEvent(this, spts));           // Possible exception here        

        var saveSpts = new ArrayList<>(spts);
        fireSgsActionEventStart(SgsActionEvent.API_ID.AddSongParts, saveSpts);

        // Change state
        for (SongPart spt : spts)
        {
            addSongPartImpl(spt);
        }

        fireSgsActionEventComplete(SgsActionEvent.API_ID.AddSongParts);

        // Make sure all AdaptedRhythms for the song rhythms are generated in the database so that user can 
        // access them if he wants too
        generateAllAdaptedRhythms();
    }

    @Override
    public synchronized void removeSongParts(List<SongPart> spts) throws UnsupportedEditException
    {
        Objects.requireNonNull(spts);

        LOGGER.log(Level.FINE, "removeSongParts() -- spts={0}", spts);

        if (spts.isEmpty())
        {
            return;
        }

        testChangeEventForVeto(new SptRemovedEvent(this, spts));           // Possible exception here        

        var saveSpts = new ArrayList<>(spts);
        fireSgsActionEventStart(SgsActionEvent.API_ID.RemoveSongParts, saveSpts);

        // Perform the change
        removeSongPartsImpl(spts);

        fireSgsActionEventComplete(SgsActionEvent.API_ID.RemoveSongParts);
    }


    @Override
    public synchronized void resizeSongParts(Map<SongPart, Integer> mapSptSize)
    {
        Objects.requireNonNull(mapSptSize);

        LOGGER.log(Level.FINE, "resizeSongParts() -- mapSptSize={0}", mapSptSize);

        if (mapSptSize.isEmpty())
        {
            return;
        }

        final Map<SongPart, Integer> saveMap = new HashMap<>(mapSptSize);
        final Map<SongPart, Integer> oldMap = new HashMap<>();

        fireSgsActionEventStart(SgsActionEvent.API_ID.ResizeSongParts, saveMap);

        for (SongPart spt : mapSptSize.keySet())
        {
            if (!songParts.contains(spt))
            {
                throw new IllegalArgumentException("this=" + this + " spt=" + spt + " mapSptsSize=" + mapSptSize);
            }
            // Save the old size before modifying it
            oldMap.put(spt, spt.getNbBars());
            SongPartImpl wspt = (SongPartImpl) spt;
            wspt.setNbBars(mapSptSize.get(spt));
        }
        updateStartBarIndexes();

        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("Resize SongParts")
        {
            @Override
            public void undoBody()
            {
                LOGGER.log(Level.FINER, "resizeSongParts.undoBody() mapSptSize={0}", mapSptSize);
                synchronized (SongStructureImpl.this)
                {
                    for (SongPart spt : oldMap.keySet())
                    {
                        ((SongPartImpl) spt).setNbBars(oldMap.get(spt));
                    }
                    updateStartBarIndexes();
                }
                fireAuthorizedChangeEvent(new SptResizedEvent(SongStructureImpl.this, saveMap));
            }

            @Override
            public void redoBody()
            {
                LOGGER.log(Level.FINER, "resizeSongParts.redoBody() mapSptSize={0}", mapSptSize);
                synchronized (SongStructureImpl.this)
                {
                    for (SongPart spt : saveMap.keySet())
                    {
                        ((SongPartImpl) spt).setNbBars(saveMap.get(spt));
                    }
                    updateStartBarIndexes();
                }
                fireAuthorizedChangeEvent(new SptResizedEvent(SongStructureImpl.this, oldMap));
            }
        };

        var event = new SptResizedEvent(this, oldMap);

        fireUndoableEditHappened(edit);

        // Fire event
        fireAuthorizedChangeEvent(event);

        fireSgsActionEventComplete(SgsActionEvent.API_ID.ResizeSongParts);
    }


    /**
     * Test if a change is authorized by listeners.
     *
     * @param event Can not be a SgsActionEvent
     * @throws UnsupportedEditException If change is vetoed by a listener
     */
    @Override
    public void testChangeEventForVeto(SgsChangeEvent event) throws UnsupportedEditException
    {
        Objects.requireNonNull(event);
        Preconditions.checkArgument(event.getSource() == this && !(event instanceof SgsActionEvent), "event=%s", event);
        SgsVetoableChangeEvent svce;
        if (event instanceof SgsVetoableChangeEvent ve)
        {
            svce = ve;
        } else
        {
            svce = new SgsVetoableChangeEvent(this, event);
        }

        SgsChangeListener[] snapshot;
        synchronized (this)
        {
            snapshot = listeners.toArray(SgsChangeListener[]::new);
        }

        for (SgsChangeListener l : snapshot)
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
    public synchronized void replaceSongParts(final List<SongPart> oldSpts, final List<SongPart> newSpts) throws UnsupportedEditException
    {
        Objects.requireNonNull(oldSpts);
        Objects.requireNonNull(newSpts);
        Preconditions.checkArgument(oldSpts.size() == newSpts.size(), "oldSpts=%s, newSpts=%s", oldSpts, newSpts);


        LOGGER.log(Level.FINE, "replaceSongParts() -- oldSpts=={0} newSpts={1}", new Object[]
        {
            oldSpts.toString(), newSpts.toString()
        });


        // Check arguments consistency
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
            return;
        }

        testChangeEventForVeto(new SptReplacedEvent(this, oldSpts, newSpts));           // Possible exception here        

        var saveNewSpts = new ArrayList<>(newSpts);
        fireSgsActionEventStart(SgsActionEvent.API_ID.ReplaceSongParts, saveNewSpts);


        // Save old state and perform the changes
        final Map<TimeSignature, Rhythm> oldMapTsRhythm = new HashMap<>(mapTsLastRhythm);
        final ArrayList<SongPart> oldSongParts = new ArrayList<>(songParts);
        final ArrayList<SongStructure> newSptsOldContainer = new ArrayList<>();

        for (int i = 0; i < oldSpts.size(); i++)
        {
            SongPart oldSpt = oldSpts.get(i);
            SongPart newSpt = newSpts.get(i);

            // Save previous container
            newSptsOldContainer.add(newSpt.getContainer());

            int rpIndex = songParts.indexOf(oldSpt);
            songParts.set(rpIndex, newSpt);
            ((SongPartImpl) newSpt).setContainer(this);

            // Update mapTsLastRhythm
            Rhythm r = newSpt.getRhythm();
            TimeSignature ts = r.getTimeSignature();
            mapTsLastRhythm.put(ts, r);

            oldSpt.removePropertyChangeListener(this);
            newSpt.addPropertyChangeListener(this);
        }

        final ArrayList<SongPart> newSongParts = new ArrayList<>(songParts);
        final Map<TimeSignature, Rhythm> newMapTsRhythm = new HashMap<>(mapTsLastRhythm);

        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("Replace SongParts")
        {
            @Override
            public void undoBody()
            {
                LOGGER.log(Level.FINER, "ReplaceSongParts.undoBody() songParts={0}", songParts);

                synchronized (SongStructureImpl.this)
                {
                    // Restore the state of the songStructure
                    songParts = new ArrayList<>(oldSongParts);      // Must use a copy to make sure oldSongParts remains unaffected
                    mapTsLastRhythm = new HashMap<>(oldMapTsRhythm);           // Must use a copy to make sure map remains unaffected            
                    // restore the container of the replacing songparts
                    for (int i = 0; i < newSpts.size(); i++)
                    {
                        var newSpt = newSpts.get(i);
                        var oldSpt = oldSpts.get(i);
                        SongStructure sgs = newSptsOldContainer.get(i);
                        ((SongPartImpl) newSpt).setContainer(sgs);
                        newSpt.removePropertyChangeListener(SongStructureImpl.this);
                        oldSpt.addPropertyChangeListener(SongStructureImpl.this);
                    }
                }
                // Don't use vetoablechange  : it already worked, normally there is no reason it would change            
                fireAuthorizedChangeEvent(new SptReplacedEvent(SongStructureImpl.this, newSpts, oldSpts));
            }

            @Override
            public void redoBody()
            {
                LOGGER.log(Level.FINER, "ReplaceSongParts.redoBody() songParts={0}", songParts);

                synchronized (SongStructureImpl.this)
                {
                    // Restore the state of the songStructure
                    songParts = new ArrayList<>(newSongParts);      // Must use a copy to make sure newSongParts remains unaffected
                    mapTsLastRhythm = new HashMap<>(newMapTsRhythm);          // Must use a copy to make sure map remains unaffected                        
                    // Change the container of the replacing songparts
                    for (int i = 0; i < newSpts.size(); i++)
                    {
                        var newSpt = newSpts.get(i);
                        var oldSpt = oldSpts.get(i);
                        ((SongPartImpl) newSpt).setContainer(SongStructureImpl.this);
                        newSpt.addPropertyChangeListener(SongStructureImpl.this);
                        oldSpt.removePropertyChangeListener(SongStructureImpl.this);
                    }
                }
                // Don't use vetoablechange : it already worked, normally there is no reason it would change
                fireAuthorizedChangeEvent(new SptReplacedEvent(SongStructureImpl.this, oldSpts, newSpts));
            }
        };

        // Need to be fired before change event
        fireUndoableEditHappened(edit);

        // Notify listeners        
        var event = new SptReplacedEvent(SongStructureImpl.this, oldSpts, newSpts);
        fireAuthorizedChangeEvent(event);

        fireSgsActionEventComplete(SgsActionEvent.API_ID.ReplaceSongParts);

        // Make sure all AdaptedRhythms for the song rhythms are generated in the database so that user can 
        // access them if he wants too
        generateAllAdaptedRhythms();
    }


    @Override
    public synchronized void setSongPartsName(List<SongPart> spts, final String name)
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

        var saveSpts = new ArrayList<>(spts);
        fireSgsActionEventStart(SgsActionEvent.API_ID.setSongPartsName, saveSpts);

        final Map<SongPart, String> saveMap = new HashMap<>();
        for (SongPart spt : spts)
        {
            SongPartImpl wspt = (SongPartImpl) spt;
            saveMap.put(wspt, wspt.getName());
            wspt.setName(name);
        }

        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("Rename SongParts")
        {
            @Override
            public void undoBody()
            {
                LOGGER.log(Level.FINER, "setSongPartsName.undoBody() spts={0} name={1}", new Object[]
                {
                    spts, name
                });
                synchronized (SongStructureImpl.this)
                {
                    for (SongPart wspt : saveMap.keySet())
                    {
                        ((SongPartImpl) wspt).setName(saveMap.get(wspt));
                    }
                }
                fireAuthorizedChangeEvent(new SptRenamedEvent(SongStructureImpl.this, saveSpts));
            }

            @Override
            public void redoBody()
            {
                LOGGER.log(Level.FINER, "setSongPartsName.redoBody() spts={0} name={1}", new Object[]
                {
                    spts, name
                });
                synchronized (SongStructureImpl.this)
                {
                    for (SongPart wspt : saveMap.keySet())
                    {
                        ((SongPartImpl) wspt).setName(name);
                    }
                }
                fireAuthorizedChangeEvent(new SptRenamedEvent(SongStructureImpl.this, saveSpts));
            }
        };

        fireUndoableEditHappened(edit);

        // Fire event                
        fireAuthorizedChangeEvent(new SptRenamedEvent(this, saveSpts));

        fireSgsActionEventComplete(SgsActionEvent.API_ID.setSongPartsName);
    }


    @Override
    public synchronized <T> void setRhythmParameterValue(SongPart spt, final RhythmParameter<T> rp, final T newValue)
    {
        Objects.requireNonNull(spt);
        Objects.requireNonNull(rp);
        Objects.requireNonNull(newValue);
        if (!songParts.contains(spt) || !spt.getRhythm().getRhythmParameters().contains(rp))
        {
            throw new IllegalArgumentException("this=" + this + " spt=" + spt + " rp=" + rp + " newValue=" + newValue);
        }

        LOGGER.log(Level.FINE, "setRhythmParameterValue() -- spt={0} rp={1} newValue={2}", new Object[]
        {
            spt, rp, newValue
        });

        final T oldValue = spt.getRPValue(rp);
        if (oldValue.equals(newValue))
        {
            return;
        }

        fireSgsActionEventStart(SgsActionEvent.API_ID.SetRhythmParameterValue, rp);

        final SongPartImpl wspt = (SongPartImpl) spt;
        wspt.setRPValue(rp, newValue);

        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("set Rhythm Parameter Value")
        {
            @Override
            public void undoBody()
            {
                LOGGER.log(Level.FINER, "setRhythmParameterValue.undoBody() spt={0} rp={1} newValue={2}", new Object[]
                {
                    spt, rp, newValue
                });
                synchronized (SongStructureImpl.this)
                {
                    wspt.setRPValue(rp, oldValue);
                }
                fireAuthorizedChangeEvent(new RpValueChangedEvent(SongStructureImpl.this, wspt, rp, newValue, oldValue));
            }

            @Override
            public void redoBody()
            {
                LOGGER.log(Level.FINER, "setRhythmParameterValue.redoBody() spt={0} rp={1} newValue={2}", new Object[]
                {
                    spt, rp, newValue
                });
                synchronized (SongStructureImpl.this)
                {
                    wspt.setRPValue(rp, newValue);
                }
                fireAuthorizedChangeEvent(new RpValueChangedEvent(SongStructureImpl.this, wspt, rp, oldValue, newValue));
            }
        };
        fireUndoableEditHappened(edit);

        // Fire event                
        fireAuthorizedChangeEvent(new RpValueChangedEvent(SongStructureImpl.this, wspt, rp, oldValue, newValue));

        fireSgsActionEventComplete(SgsActionEvent.API_ID.SetRhythmParameterValue);
    }


    @Override
    public synchronized Rhythm getLastUsedRhythm(TimeSignature ts)
    {
        Rhythm r = mapTsLastRhythm.get(ts);
        LOGGER.log(Level.FINE, "getLastUsedRhythm() ts={0} result r={1}", new Object[]
        {
            ts, r
        });
        return r;
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

        // Try to use the last used rhythm for this new time signature
        Rhythm r = getLastUsedRhythm(ts);

        // Try to use an AdaptedRhythm from the current rhythm
        if (r == null && !songParts.isEmpty())
        {
            // sptBarIndex is either on an existing rhythm part, or is right after the last song part
            Rhythm curRhythm = getSongPart(sptBarIndex > 0 ? sptBarIndex - 1 : sptBarIndex).getRhythm();
            if (curRhythm instanceof AdaptedRhythm ar)
            {
                curRhythm = ar.getSourceRhythm();
            }
            r = rdb.getAdaptedRhythmInstance(curRhythm, ts);        // may be null
        }

        // Last option
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
    public synchronized SongPart getSongPart(int absoluteBarIndex)
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
    }

    @Override
    public synchronized List<SongPart> getSongParts(Predicate<SongPart> tester)
    {
        return songParts.stream().filter(tester).toList();
    }

    @Override
    public Position toPosition(float posInBeats)
    {
        if (posInBeats < 0)
        {
            throw new IllegalArgumentException("posInBeats=" + posInBeats);
        }
        for (SongPart spt : getSongParts())     // Synchronized
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
    }

    @Override
    public synchronized float toPositionInNaturalBeats(int barIndex)
    {
        int size = getSizeInBars();
        Preconditions.checkArgument(barIndex >= 0 && barIndex < size, "barIndex=%s size=%s", barIndex, size);

        float posInBeats = 0;
        if (barIndex == getSizeInBars())
        {
            // Special case : barIndex is the bar right after the end of the songStructure
            for (SongPart spt : songParts)
            {
                TimeSignature ts = spt.getParentSection().getData().getTimeSignature();
                posInBeats += spt.getNbBars() * ts.getNbNaturalBeats();
            }
        } else
        {
            // Normal case: it's a bar within the songStructure
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
        if (l == null)
        {
            throw new NullPointerException("l=" + l);
        }
        synchronized (this)
        {
            listeners.remove(l);
            listeners.add(l);
        }
    }

    @Override
    public void removeSgsChangeListener(SgsChangeListener l)
    {
        if (l == null)
        {
            throw new NullPointerException("l=" + l);
        }
        synchronized (this)
        {
            listeners.remove(l);
        }
    }

    @Override
    public void addUndoableEditListener(UndoableEditListener l)
    {
        if (l == null)
        {
            throw new NullPointerException("l=" + l);
        }
        synchronized (this)
        {
            undoListeners.remove(l);
            undoListeners.add(l);
        }
    }

    @Override
    public void removeUndoableEditListener(UndoableEditListener l)
    {
        if (l == null)
        {
            throw new NullPointerException("l=" + l);
        }
        synchronized (this)
        {
            undoListeners.remove(l);
        }
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
                // Propagate mutable value change events as SgsActionEvent + RpValueChangedEvent
                @SuppressWarnings("unchecked")
                RhythmParameter<?> rp = (RhythmParameter<?>) evt.getOldValue();
                Object rpValue = evt.getNewValue();

                fireSgsActionEventStart(SgsActionEvent.API_ID.SetRhythmParameterMutableValue, rp);

                fireAuthorizedChangeEvent(new RpValueChangedEvent(SongStructureImpl.this, spt, rp, null, rpValue));

                fireSgsActionEventComplete(SgsActionEvent.API_ID.SetRhythmParameterMutableValue);
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

        // Save the old state
        final ArrayList<SongPart> oldSpts = new ArrayList<>(songParts);
        final Map<TimeSignature, Rhythm> oldMapTsRhythm = new HashMap<>(mapTsLastRhythm);
        final SongStructure oldContainer = spt.getContainer();

        int barIndex = spt.getStartBarIndex();
        if (songParts.contains(spt) || barIndex > getSizeInBars())
        {
            throw new IllegalArgumentException("this=" + this + " spt=" + spt);
        }

        // Update state       
        if (barIndex == getSizeInBars())
        {
            // Append at the end
            songParts.add(spt);
        } else
        {
            // Insert 
            SongPart curSpt = getSongPart(barIndex);
            if (barIndex != curSpt.getStartBarIndex())
            {
                // Caller must have correctly set startBarIndex of the inserted spt
                throw new IllegalArgumentException("this=" + this + " spt=" + spt + " curSpt=" + curSpt);
            }
            int rpIndex = songParts.indexOf(curSpt);
            songParts.add(rpIndex, spt);
        }


        // Restore correct startBarIndexes of following songparts
        updateStartBarIndexes();
        // Set the container
        ((SongPartImpl) spt).setContainer(this);
        // Keep the default rhythm map updated
        mapTsLastRhythm.put(spt.getRhythm().getTimeSignature(), spt.getRhythm());

        // Listen to mutable RP value changes
        spt.addPropertyChangeListener(this);

        // Save the new state
        final ArrayList<SongPart> newSpts = new ArrayList<>(songParts);
        final Map<TimeSignature, Rhythm> newMapTsRhythm = new HashMap<>(mapTsLastRhythm);

        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("Add SongPart spt=" + spt)
        {
            @Override
            public void undoBody()
            {
                LOGGER.log(Level.FINER, "addSongPartInternal.undoBody() spt={0}", spt);
                synchronized (SongStructureImpl.this)
                {
                    ((SongPartImpl) spt).setContainer(oldContainer);
                    songParts = new ArrayList<>(oldSpts);         // Must use a copy to make sure oldSpts remains unaffected
                    mapTsLastRhythm = new HashMap<>(oldMapTsRhythm);           // Must use a copy to make sure map remains unaffected
                    updateStartBarIndexes();
                }

                spt.removePropertyChangeListener(SongStructureImpl.this);

                // Don't use vetoable change  : it already worked, normally there is no reason it would change
                fireAuthorizedChangeEvent(new SptRemovedEvent(SongStructureImpl.this, Arrays.asList(spt)));
            }

            @Override
            public void redoBody()
            {
                LOGGER.log(Level.FINER, "addSongPartInternal.redoBody() spt={0}", spt);

                synchronized (SongStructureImpl.this)
                {
                    ((SongPartImpl) spt).setContainer(SongStructureImpl.this);
                    songParts = new ArrayList<>(newSpts);          // Must use a copy to make sure newSpts remains unaffected
                    mapTsLastRhythm = new HashMap<>(newMapTsRhythm);           // Must use a copy to make sure map remains unaffected
                    updateStartBarIndexes();
                }

                spt.addPropertyChangeListener(SongStructureImpl.this);

                // Don't use vetoable change : it already worked, normally there is no reason it would change
                fireAuthorizedChangeEvent(new SptAddedEvent(SongStructureImpl.this, Arrays.asList(spt)));
            }
        };


        // Need to be before the change event
        fireUndoableEditHappened(edit);

        // Fire change event
        fireAuthorizedChangeEvent(new SptAddedEvent(this, Arrays.asList(spt)));
    }

    /**
     * Internal implementation, no checks performed.
     *
     * @param spts
     */
    protected void removeSongPartsImpl(List<SongPart> spts)
    {
        if (spts == null)
        {
            throw new IllegalArgumentException("this=" + this + " spts=" + spts);
        }

        LOGGER.log(Level.FINER, "removeSongPartInternal() -- spts={0}", spts);

        // Save state
        final ArrayList<SongPart> oldSpts = new ArrayList<>(songParts);

        for (SongPart spt : spts)
        {
            if (!songParts.remove(spt))
            {
                throw new IllegalArgumentException("this=" + this + " spt=" + spt + " songParts=" + songParts);
            }
            spt.removePropertyChangeListener(this);
        }
        updateStartBarIndexes();

        final ArrayList<SongPart> saveSpts = new ArrayList<>(spts);
        final ArrayList<SongPart> newSpts = new ArrayList<>(songParts);


        // Save the new state
        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("Remove SongPart")
        {
            @Override
            public void undoBody()
            {
                LOGGER.log(Level.FINER, "removeSongPartInternal.undoBody() spts={0}", spts);
                synchronized (SongStructureImpl.this)
                {
                    songParts = new ArrayList<>(oldSpts);
                    updateStartBarIndexes();
                    spts.forEach(spt -> spt.addPropertyChangeListener(SongStructureImpl.this));
                }
                fireAuthorizedChangeEvent(new SptAddedEvent(SongStructureImpl.this, saveSpts));
            }

            @Override
            public void redoBody()
            {
                LOGGER.log(Level.FINER, "removeSongPartInternal.redoBody() spts={0}", spts);
                synchronized (SongStructureImpl.this)
                {
                    songParts = new ArrayList<>(newSpts);            // Must use a copy to make sure newSpts remains unaffected
                    updateStartBarIndexes();
                    spts.forEach(spt -> spt.removePropertyChangeListener(SongStructureImpl.this));
                }
                fireAuthorizedChangeEvent(new SptRemovedEvent(SongStructureImpl.this, saveSpts));
            }
        };


        // Before change event
        fireUndoableEditHappened(edit);


        // Fire change event
        var event = new SptRemovedEvent(this, spts);
        fireAuthorizedChangeEvent(event);
    }


    private synchronized int getSptLastBarIndex(int sptIndex)
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
     * Fire an authorized change event to all listeners.
     * <p>
     * If it's not a SgsActionEvent, also adds the event to the active SgsActionEvent.
     *
     * @param event Can not be a SgsVetoableChangeEvent
     */
    private void fireAuthorizedChangeEvent(SgsChangeEvent event)
    {
        Objects.requireNonNull(event);
        Preconditions.checkArgument(!(event instanceof SgsVetoableChangeEvent), "event=%s", event);

        SgsChangeListener[] snapshot;
        synchronized (this)
        {
            if (!(event instanceof SgsActionEvent))
            {
                assert activeSgsActionEvent != null : "event=" + event;
                activeSgsActionEvent.addSubEvent(event);
            }
            snapshot = listeners.toArray(SgsChangeListener[]::new);
        }

        for (SgsChangeListener l : snapshot)
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
     * Set the active SgsActionEvent, unless there is already an active SgsActionEvent, and fire the required events.
     * <p>
     *
     * @param apiId
     * @param data
     */
    private synchronized void fireSgsActionEventStart(SgsActionEvent.API_ID apiId, Object data)
    {
        Objects.requireNonNull(apiId);

        if (activeSgsActionEvent != null)
        {
            assert activeSgsActionEvent.getApiId() != apiId :
                    "apiId=" + apiId + " activeSgsActionEvent=" + activeSgsActionEvent + " data=" + Objects.toString(data, "null");
            return;
        }


        // Create an undoable event for this event which does nothing but refiring the ClsActionEvent
        UndoableEdit edit = new SimpleEdit("SgsActionEventEdit(" + apiId + ")")
        {
            @Override
            public void undoBody()
            {
                synchronized (SongStructureImpl.this)
                {
                    assert activeSgsActionEvent != null;
                    activeSgsActionEvent.complete();
                    fireAuthorizedChangeEvent(activeSgsActionEvent);
                    activeSgsActionEvent = null;
                }
            }

            @Override
            public void redoBody()
            {
                synchronized (SongStructureImpl.this)
                {
                    assert activeSgsActionEvent == null : "activeSgsActionEvent" + activeSgsActionEvent;
                    activeSgsActionEvent = new SgsActionEvent(SongStructureImpl.this, apiId, data);
                    fireAuthorizedChangeEvent(activeSgsActionEvent);
                }
            }
        };
        fireUndoableEditHappened(edit);

        activeSgsActionEvent = new SgsActionEvent(this, apiId, data);
        fireAuthorizedChangeEvent(activeSgsActionEvent);
    }


    /**
     * Complete the active SgsActionEvent, unless the active one is not linked to apiId, and fire the required events.
     *
     * @param apiId
     */
    private synchronized void fireSgsActionEventComplete(SgsActionEvent.API_ID apiId)
    {
        Objects.requireNonNull(apiId);

        assert activeSgsActionEvent != null : "apiId=" + apiId;
        if (activeSgsActionEvent.getApiId() != apiId)
        {
            return;
        }

        var data = activeSgsActionEvent.getData();


        // Create an undoable event for this event which does nothing but refiring the SgsActionEvent
        UndoableEdit edit = new SimpleEdit("SgsActionEventEdit(" + apiId + ")")
        {
            @Override
            public void undoBody()
            {
                synchronized (SongStructureImpl.this)
                {
                    assert activeSgsActionEvent == null : "activeSgsActionEvent=" + activeSgsActionEvent;
                    activeSgsActionEvent = new SgsActionEvent(SongStructureImpl.this, apiId, data);
                    fireAuthorizedChangeEvent(activeSgsActionEvent);
                }
            }

            @Override
            public void redoBody()
            {
                synchronized (SongStructureImpl.this)
                {
                    assert activeSgsActionEvent != null;
                    activeSgsActionEvent.complete();
                    fireAuthorizedChangeEvent(activeSgsActionEvent);
                    activeSgsActionEvent = null;
                }
            }
        };
        fireUndoableEditHappened(edit);

        activeSgsActionEvent.complete();
        fireAuthorizedChangeEvent(activeSgsActionEvent);
        activeSgsActionEvent = null;
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

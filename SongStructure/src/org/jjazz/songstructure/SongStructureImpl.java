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
package org.jjazz.songstructure;

import org.jjazz.songstructure.api.event.RpChangedEvent;
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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.UndoableEdit;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.parameters.RhythmParameter;
import org.jjazz.rhythm.database.api.RhythmDatabase;
import org.jjazz.undomanager.SimpleEdit;
import org.jjazz.util.SmallMap;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SgsChangeListener;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.util.Range;

public class SongStructureImpl implements SongStructure, Serializable
{

    /**
     * Keep SongParts ordered by startBarIndex.
     */
    private ArrayList<SongPart> songParts = new ArrayList<>();
    /**
     * Our parent ChordLeadSheet.
     */
    private ChordLeadSheet parentCls;
    private boolean keepSgsUpdated;
    /**
     * Keep the last Rhythm used for each TimeSignature.
     */
    private transient SmallMap<TimeSignature, Rhythm> mapTsLastRhythm = new SmallMap<>();
    /**
     * The listeners for changes.
     */
    private transient List<SgsChangeListener> listeners = new ArrayList<>();
    /**
     * The listeners for undoable edits.
     */
    private transient List<UndoableEditListener> undoListeners = new ArrayList<>();
    /**
     * Manage updates from parentChordLeadSheet
     */
    private transient SgsUpdater clsListener;
    private static final Logger LOGGER = Logger.getLogger(SongStructureImpl.class.getSimpleName());
    private static int DEBUG_UNDOEDIT_ID = 0;

    public SongStructureImpl()
    {
        // Do nothing
    }

    /**
     *
     * @param cls         The parent chordleadsheet
     * @param keepUpdated If true listen to cls changes to remain uptodate
     */
    public SongStructureImpl(ChordLeadSheet cls, boolean keepUpdated)
    {
        if (cls == null)
        {
            throw new IllegalArgumentException("cls=" + cls + " keepUpdated=" + keepUpdated);
        }
        parentCls = cls;
        keepSgsUpdated = keepUpdated;
        if (parentCls != null && keepSgsUpdated)
        {
            // We want to be updated when parentCls changes
            clsListener = new SgsUpdater(this);
        }
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

        return (List<SongPart>) songParts.clone();
    }

    @Override
    public int getSizeInBars()
    {
        int res = songParts.isEmpty() ? 0 : getSptLastBarIndex(songParts.size() - 1) + 1;
        return res;
    }

    @Override
    public int getSizeInBeats(Range r)
    {
        if (r == null)
        {
            r = new Range(0, getSizeInBars() - 1);
        }
        int size = 0;
        for (SongPart spt : songParts)
        {
            int nbBars = r.getIntersectRange(spt.getRange()).size();
            size += nbBars * spt.getRhythm().getTimeSignature().getNbNaturalBeats();
        }
        return size;
    }

    @Override
    public SongPart createSongPart(Rhythm r, int startBarIndex, int nbBars, CLI_Section parentSection)
    {
        if (r == null || startBarIndex < 0 || nbBars < 0)
        {
            throw new IllegalArgumentException("r=" + r + " startBarIndex=" + startBarIndex + " nbBars=" + nbBars + " parentSection=" + parentSection);
        }
        SongPartImpl spt = new SongPartImpl(r, startBarIndex, nbBars, parentSection);
        spt.setContainer(this);
        return spt;
    }

    @Override
    public void addSongPart(final SongPart spt) throws UnsupportedEditException
    {
        if (spt == null)
        {
            throw new IllegalArgumentException("this=" + this + " spt=" + spt);
        }
        LOGGER.log(Level.FINE, "addSongPart() -- spt={0}", spt);
        // Save the old state
        final ArrayList<SongPart> oldSpts = new ArrayList<>(songParts);
        final SmallMap<TimeSignature, Rhythm> oldMapTsRhythm = mapTsLastRhythm.clone();
        final SongStructure oldContainer = spt.getContainer();
        // Update state
        int barIndex = spt.getStartBarIndex();
        if (songParts.contains(spt) || barIndex > getSizeInBars())
        {
            throw new IllegalArgumentException("this=" + this + " spt=" + spt);
        }
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
        mapTsLastRhythm.putValue(spt.getRhythm().getTimeSignature(), spt.getRhythm());

        // Save the new state
        final ArrayList<SongPart> newSpts = new ArrayList<>(songParts);
        final SmallMap<TimeSignature, Rhythm> newMapTsRhythm = mapTsLastRhythm.clone();

        // Create the undoable event
        UndoableEdit edit;
        edit = new SimpleEdit("Add SongPart id=" + (DEBUG_UNDOEDIT_ID++) + " spt=" + spt)
        {
            @Override
            public void undoBody()
            {
                LOGGER.log(Level.FINE, "edit={0}.undoBody() -- entering songParts={1}", new Object[]
                {
                    this, songParts
                });
                ((SongPartImpl) spt).setContainer(oldContainer);
                songParts = new ArrayList<>(oldSpts);         // Must use a copy to make sure oldSpts remains unaffected
                mapTsLastRhythm = oldMapTsRhythm.clone();           // Must use a copy to make sure map remains unaffected
                updateStartBarIndexes();
                LOGGER.log(Level.FINE, "edit={0}                exiting songParts={1}", new Object[]
                {
                    this, songParts
                });
                // Don't use vetoable change  : it already work, normally there is no reason it would change
                fireChangeEvent(new SptRemovedEvent(SongStructureImpl.this, Arrays.asList(spt)));
            }

            @Override
            public void redoBody()
            {
                LOGGER.log(Level.FINE, "edit={0}.redoBody() -- entering songParts={1}", new Object[]
                {
                    this, songParts
                });
                ((SongPartImpl) spt).setContainer(SongStructureImpl.this);
                songParts = new ArrayList<>(newSpts);          // Must use a copy to make sure newSpts remains unaffected
                mapTsLastRhythm = newMapTsRhythm.clone();            // Must use a copy to make sure map remains unaffected
                updateStartBarIndexes();
                LOGGER.log(Level.FINE, "edit={0}                exiting songParts={1}", new Object[]
                {
                    this, songParts
                });
                // Don't use vetoable change : it already work, normally there is no reason it would change
                fireChangeEvent(new SptAddedEvent(SongStructureImpl.this, Arrays.asList(spt)));
            }
        };
        // Need to be fired BEFORE the vetoable change ! So it can be undoed by the caller who will handle the exception
        fireUndoableEditHappened(edit);

        // Fire event but this might not work, e.g. if a listener has a problem with not enough Midi Channels to 
        // accomodate the new rhythm. Possible Exception here !
        // Important : the undoable
        fireVetoableChangeEvent(new SptAddedEvent(this, Arrays.asList(spt)));
    }

    @Override
    public void removeSongParts(List<SongPart> spts)
    {
        if (spts == null)
        {
            throw new IllegalArgumentException("this=" + this + " spts=" + spts);
        }
        if (spts.isEmpty())
        {
            return;
        }
        // Save state
        final ArrayList<SongPart> oldSpts = new ArrayList<>(songParts);

        // Update state
        for (SongPart spt : spts)
        {
            if (!songParts.remove(spt))
            {
                throw new IllegalArgumentException("this=" + this + " spt=" + spt + " songParts=" + songParts);
            }
        }
        updateStartBarIndexes();

        // Save the new state
        final ArrayList<SongPart> saveSpts = new ArrayList<>(spts);
        final ArrayList<SongPart> newSpts = new ArrayList<>(songParts);

        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("Remove SongPart")
        {
            @Override
            public void undoBody()
            {
                songParts = new ArrayList<>(oldSpts);            // Must use a copy to make sure oldSpts remains unaffected
                updateStartBarIndexes();
                fireChangeEvent(new SptAddedEvent(SongStructureImpl.this, saveSpts));
            }

            @Override
            public void redoBody()
            {
                songParts = new ArrayList<>(newSpts);            // Must use a copy to make sure newSpts remains unaffected
                updateStartBarIndexes();
                fireChangeEvent(new SptRemovedEvent(SongStructureImpl.this, saveSpts));
            }
        };
        fireUndoableEditHappened(edit);

        // Fire event
        fireChangeEvent(new SptRemovedEvent(this, saveSpts));
    }

    @Override
    public void resizeSongParts(SmallMap<SongPart, Integer> mapSptSize)
    {
        if (mapSptSize == null)
        {
            throw new IllegalArgumentException("this=" + this + " mapSptsSize=" + mapSptSize);
        }
        if (mapSptSize.isEmpty())
        {
            return;
        }
        final SmallMap<SongPart, Integer> saveMap = mapSptSize.clone();
        final SmallMap<SongPart, Integer> oldMap = new SmallMap<>();
        for (SongPart spt : mapSptSize.getKeys())
        {
            if (!songParts.contains(spt))
            {
                throw new IllegalArgumentException("this=" + this + " spt=" + spt + " mapSptsSize=" + mapSptSize);
            }
            // Save the old size before modifying it
            oldMap.putValue(spt, spt.getNbBars());
            SongPartImpl wspt = (SongPartImpl) spt;
            wspt.setNbBars(mapSptSize.getValue(spt));
        }

        updateStartBarIndexes();

        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("Resize SongParts")
        {
            @Override
            public void undoBody()
            {
                for (SongPart spt : oldMap.getKeys())
                {
                    ((SongPartImpl) spt).setNbBars(oldMap.getValue(spt));
                }
                updateStartBarIndexes();
                fireChangeEvent(new SptResizedEvent(SongStructureImpl.this, saveMap));
            }

            @Override
            public void redoBody()
            {
                for (SongPart spt : saveMap.getKeys())
                {
                    ((SongPartImpl) spt).setNbBars(saveMap.getValue(spt));
                }
                updateStartBarIndexes();
                fireChangeEvent(new SptResizedEvent(SongStructureImpl.this, oldMap));
            }
        };
        fireUndoableEditHappened(edit);

        // Fire event
        fireChangeEvent(new SptResizedEvent(this, oldMap));
    }

    /**
     * We need a method that works with a list of SongParts because replacing a single spt at a time may cause problems when there
     * is an unsupportedEditException.
     * <p>
     * Example: We have spt1=rhythm0, spt2=rhythm0, spt3=rhythm1. There are enough Midi channels for both rhythms.<br>
     * We want to change rhythm of both spt1 and spt2. If we do one spt at a time, after the first replacement on spt0 we'll have
     * 3 rhythms and possibly our VetoableListeners will trigger an UnsupportedEditException (if not enough Midi channels), though
     * there should be no problem since we want to change both spt1 AND spt2 !
     *
     * @param oldSpts
     * @param newSpts
     * @throws UnsupportedEditException
     */
    @Override
    public void replaceSongParts(final List<SongPart> oldSpts, final List<SongPart> newSpts) throws UnsupportedEditException
    {
        if (oldSpts == null || newSpts == null || oldSpts.size() != newSpts.size())
        {
            throw new IllegalArgumentException("this=" + this + " oldSpts=" + oldSpts + " newSpts=" + newSpts);
        }
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
        LOGGER.log(Level.FINE, "replaceSongParts() -- oldSpts=={0} newSpts={1}", new Object[]
        {
            oldSpts.toString(), newSpts.toString()
        });
        if (oldSpts.equals(newSpts))
        {
            return;
        }

        // Save old state and perform the changes
        final SmallMap<TimeSignature, Rhythm> oldMapTsRhythm = mapTsLastRhythm.clone();
        final ArrayList<SongPart> oldSongParts = new ArrayList<>(songParts);
        final ArrayList<SongStructure> newSptsOldContainer = new ArrayList<>();
        for (int i = 0; i < oldSpts.size(); i++)
        {
            SongPart oldSpt = oldSpts.get(i);
            SongPart newSpt = newSpts.get(i);
            // Save previous container
            newSptsOldContainer.add(newSpt.getContainer());
            // Update songParts and set new container
            int rpIndex = songParts.indexOf(oldSpt);
            songParts.set(rpIndex, newSpt);
            ((SongPartImpl) newSpt).setContainer(this);
            // Update mapTsLastRhythm
            Rhythm r = newSpt.getRhythm();
            TimeSignature ts = r.getTimeSignature();
            mapTsLastRhythm.putValue(ts, r);
        }

        // Save new state
        final ArrayList<SongPart> newSongParts = new ArrayList<>(songParts);
        final SmallMap<TimeSignature, Rhythm> newMapTsRhythm = mapTsLastRhythm.clone();

        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("Replace SongParts id=" + (DEBUG_UNDOEDIT_ID++) + " oldSpts=" + oldSpts + " newSpts=" + newSpts)
        {
            @Override
            public void undoBody()
            {
                LOGGER.log(Level.FINE, "edit={0}.undoBody()  -- entering songParts={1}", new Object[]
                {
                    this.toString(), songParts.toString()
                });
                // Restore the state of the songStructure
                songParts = new ArrayList<>(oldSongParts);      // Must use a copy to make sure oldSongParts remains unaffected
                mapTsLastRhythm = oldMapTsRhythm.clone();           // Must use a copy to make sure map remains unaffected            
                // restore the container of the replacing songparts
                for (int i = 0; i < newSpts.size(); i++)
                {
                    SongPart newSpt = newSpts.get(i);
                    SongStructure sgs = newSptsOldContainer.get(i);
                    ((SongPartImpl) newSpt).setContainer(sgs);
                }
                LOGGER.log(Level.FINE, "edit={0}                                   exiting songParts={1}", new Object[]
                {
                    this, songParts
                });
                // Don't use vetoablechange  : it already worked, normally there is no reason it would change            
                fireChangeEvent(new SptReplacedEvent(SongStructureImpl.this, newSpts, oldSpts));
            }

            @Override
            public void redoBody()
            {
                LOGGER.log(Level.FINE, "edit={0}.redoBody()  -- entering songParts={1}", new Object[]
                {
                    this, songParts
                });
                // Restore the state of the songStructure
                songParts = new ArrayList<>(newSongParts);      // Must use a copy to make sure newSongParts remains unaffected
                mapTsLastRhythm = newMapTsRhythm.clone();           // Must use a copy to make sure map remains unaffected                        
                // Change the container of the replacing songparts
                for (SongPart newSpt : newSpts)
                {
                    ((SongPartImpl) newSpt).setContainer(SongStructureImpl.this);
                }
                LOGGER.log(Level.FINE, "edit={0}                                   exiting songParts={1}", new Object[]
                {
                    this, songParts
                });
                // Don't use vetoablechange : it already worked, normally there is no reason it would change
                fireChangeEvent(new SptReplacedEvent(SongStructureImpl.this, oldSpts, newSpts));
            }
        };
        // Need to be fired BEFORE the vetoable change ! So it can be undoed by the caller who will handle the exception
        fireUndoableEditHappened(edit);

        // Fire event but this might not work, e.g. if a listener has a problem with not enough Midi Channels to 
        // accomodate the new rhythm, possible Exception here !
        fireVetoableChangeEvent(new SptReplacedEvent(SongStructureImpl.this, oldSpts, newSpts));
    }

    @Override
    public void setSongPartsName(List<SongPart> spts, final String name)
    {
        if (spts == null)
        {
            throw new IllegalArgumentException("this=" + this + " spts=" + spts + " name=" + name);
        }
        if (spts.isEmpty() || (spts.size() == 1 && spts.get(0).getName().equals(name)))
        {
            return;
        }
        final SmallMap<SongPart, String> save = new SmallMap<>();
        for (SongPart spt : spts)
        {
            SongPartImpl wspt = (SongPartImpl) spt;
            save.putValue(wspt, wspt.getName());
            wspt.SetName(name);
        }
        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("Rename SongParts")
        {
            @Override
            public void undoBody()
            {
                for (SongPart wspt : save.getKeys())
                {
                    ((SongPartImpl) wspt).SetName(save.getValue(wspt));
                }
                fireChangeEvent(new SptRenamedEvent(SongStructureImpl.this, save.getKeys()));
            }

            @Override
            public void redoBody()
            {
                for (SongPart wspt : save.getKeys())
                {
                    ((SongPartImpl) wspt).SetName(name);
                }
                fireChangeEvent(new SptRenamedEvent(SongStructureImpl.this, save.getKeys()));
            }
        };
        fireUndoableEditHappened(edit);

        // Fire event                
        fireChangeEvent(new SptRenamedEvent(this, save.getKeys()));
    }

    @Override
    public <T> void setRhythmParameterValue(SongPart spt, final RhythmParameter<T> rp, final T newValue)
    {
        if (spt == null || rp == null || newValue == null || !songParts.contains(spt)
                || !spt.getRhythm().getRhythmParameters().contains(rp))
        {
            throw new IllegalArgumentException("this=" + this + " spt=" + spt + " rp=" + rp + " newValue=" + newValue);
        }
        if (spt.getRPValue(rp) == newValue)
        {
            return;
        }
        final T oldValue = spt.getRPValue(rp);
        final SongPartImpl wspt = (SongPartImpl) spt;
        wspt.setRPValue(rp, newValue);

        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("set Rhythm Parameter Value")
        {
            @Override
            public void undoBody()
            {
                wspt.setRPValue(rp, oldValue);
                fireChangeEvent(new RpChangedEvent(SongStructureImpl.this, wspt, rp, newValue, oldValue));
            }

            @Override
            public void redoBody()
            {
                wspt.setRPValue(rp, newValue);
                fireChangeEvent(new RpChangedEvent(SongStructureImpl.this, wspt, rp, oldValue, newValue));
            }
        };
        fireUndoableEditHappened(edit);

        // Fire event                
        fireChangeEvent(new RpChangedEvent(SongStructureImpl.this, wspt, rp, oldValue, newValue));
    }

    /**
     * The default rhythm for this songStructure for the specified time signature.
     * <p>
     * Returns the last rhythm used in this songStructure for this TimeSignature.<br>
     * If time signature ts is used for the first time use in this songStructure, use the RhythmDatabase default rhythm.
     *
     * @param ts
     * @return A non-null Rhythm.
     *
     * @throws IllegalArgumentException If no rhythm could be found for time signature ts.
     */
    @Override
    public Rhythm getDefaultRhythm(TimeSignature ts)
    {
        RhythmDatabase rdb = RhythmDatabase.Utilities.getDefault();
        Rhythm r = mapTsLastRhythm.getValue(ts);
        if (r == null)
        {
            // Nothing in the map, use RhythmDatabase's default
            r = rdb.getDefaultRhythm(ts);
        }
        if (r == null)
        {
            throw new IllegalArgumentException("Can't find a rhythm in the database for ts=" + ts + " rdb=" + rdb);
        }
        return r;
    }

    @Override
    public SongPart getSongPart(int absoluteBarIndex)
    {
        int abi = absoluteBarIndex;
        for (SongPart spt : songParts)
        {
            if (abi >= spt.getStartBarIndex() && abi < (spt.getStartBarIndex() + spt.getNbBars()))
            {
                return spt;
            }
        }
        return null;
    }

    @Override
    public float getPositionInNaturalBeats(int barIndex)
    {
        if (barIndex < 0 || barIndex > getSizeInBars())
        {
            throw new IllegalArgumentException("barIndex=" + barIndex);
        }
        float posInBeats = 0;
        if (barIndex == getSizeInBars())
        {
            // Special case : barIndex is the bar righ after the end of the songStructure
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
    public String toString()
    {
        return "size=" + getSizeInBars() + " spts=" + songParts.toString();
    }

    @Override
    public void addSgsChangeListener(SgsChangeListener l)
    {
        if (l == null)
        {
            throw new NullPointerException("l=" + l);
        }
        listeners.remove(l);
        listeners.add(l);
    }

    @Override
    public void removeSgsChangeListener(SgsChangeListener l)
    {
        if (l == null)
        {
            throw new NullPointerException("l=" + l);
        }
        listeners.remove(l);
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

    // -------------------------------------------------------------------------------------------
    // Private functions
    // -------------------------------------------------------------------------------------------
    private int getSptLastBarIndex(int rpIndex)
    {
        if (rpIndex < 0 || rpIndex >= songParts.size())
        {
            throw new IllegalArgumentException("rpIndex=" + rpIndex);
        }
        SongPart spt = songParts.get(rpIndex);
        return spt.getStartBarIndex() + spt.getNbBars() - 1;
    }

    /**
     * Check and possibly update each SongPart's startBarIndex.
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

    protected void fireVetoableChangeEvent(SgsChangeEvent event) throws UnsupportedEditException
    {
        if (event == null)
        {
            throw new IllegalArgumentException("event=" + event);
        }
        for (SgsChangeListener l : listeners.toArray(new SgsChangeListener[listeners.size()]))
        {
            l.songStructureChanged(event);
        }
    }

    /**
     * Convenience method, identitical to fireVetoableChangeEvent, except that caller considers that an UnsupportedEditException
     * will never be thrown.
     *
     * @param event
     * @throws IllegalStateException If an UnsupportedEditException was catched.
     */
    protected void fireChangeEvent(SgsChangeEvent event)
    {
        if (event == null)
        {
            throw new IllegalArgumentException("event=" + event);
        }
        for (SgsChangeListener l : listeners.toArray(new SgsChangeListener[listeners.size()]))
        {
            try
            {
                l.songStructureChanged(event);
            } catch (UnsupportedEditException ex)
            {
                LOGGER.severe("fireChangeEvent() unexpected UnsupportedEditException ex=" + ex);
                throw new IllegalStateException("Unexpected UnsupportedEditException", ex);
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
        for (UndoableEditListener l : undoListeners.toArray(new UndoableEditListener[undoListeners.size()]))
        {
            l.undoableEditHappened(event);
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
     */
    private static class SerializationProxy implements Serializable
    {

        private static final long serialVersionUID = -76876542017265L;
        private final int spVERSION = 1;
        private final ArrayList<SongPart> spSpts;
        private final ChordLeadSheet spParentCls;
        private final boolean spKeepUpdated;

        private SerializationProxy(SongStructureImpl sgs)
        {
            spParentCls = sgs.getParentChordLeadSheet();
            spSpts = new ArrayList<>();
            spSpts.addAll(sgs.getSongParts());
            spKeepUpdated = sgs.keepSgsUpdated;
        }

        private Object readResolve() throws ObjectStreamException
        {
            if (spSpts == null)
            {
                throw new IllegalStateException("spSpts=" + spSpts);
            }

            SongStructureImpl sgs = new SongStructureImpl(spParentCls, spKeepUpdated);
            for (SongPart spt : spSpts)
            {
                try
                {
                    // This will set again the container of the songparts
                    sgs.addSongPart(spt);
                } catch (UnsupportedEditException ex)
                {
                    // Translate to an ObjectStreamException
                    throw new InvalidObjectException(ex.getLocalizedMessage());
                }
            }
            return sgs;
        }
    }

}

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
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
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
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.rhythm.api.AdaptedRhythm;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SgsChangeListener;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.util.FloatRange;
import org.jjazz.util.IntRange;

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
     * @param cls The parent chordleadsheet
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
    public FloatRange getBeatRange(IntRange rg)
    {
        IntRange songRange = new IntRange(0, getSizeInBars() - 1);
        if (rg == null)
        {
            rg = songRange;
        } else if (!rg.contains(songRange))
        {
            return FloatRange.EMPTY_FLOAT_RANGE;
        }
        float startPos = -1;
        float endPos = -1;
        for (SongPart spt : songParts)
        {
            TimeSignature ts = spt.getRhythm().getTimeSignature();
            IntRange ir = rg.getIntersectRange(spt.getBarRange());
            if (ir.isEmpty())
            {
                continue;
            }
            if (startPos == -1)
            {
                startPos = getPositionInNaturalBeats(ir.from);
                endPos = startPos + ir.size() * ts.getNbNaturalBeats();
            } else
            {
                endPos += ir.size() * ts.getNbNaturalBeats();
            }
        }
        return new FloatRange(startPos, endPos);
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
    public void addSongParts(final List<SongPart> spts) throws UnsupportedEditException
    {
        if (spts == null)
        {
            throw new IllegalArgumentException("spts=" + spts);
        }
        if (spts.isEmpty())
        {
            return;
        }

        // Check that after the operation each AdaptedRhythm must have its source rhythm present
        var finalSpts = new ArrayList<SongPart>(songParts);
        finalSpts.addAll(spts);
        AdaptedRhythm faultyAdRhythm = checkAdaptedRhythmConsistency(finalSpts);
        if (faultyAdRhythm != null)
        {
            var sr = faultyAdRhythm.getSourceRhythm();
            String msg = "Can't add adapted rhythm " + faultyAdRhythm.getName() + ": its source rhythm (" + sr.getName() + ") is not present";
            throw new UnsupportedEditException(msg);
        }


        // Check change is not vetoed by listeners
        var event = new SptAddedEvent(this, spts);
        authorizeChangeEvent(event);            // Possible exception here! 


        // Change state
        for (SongPart spt : spts)
        {
            addSongPartInternal(spt);
        }


        // Make sure all AdaptedRhythms for the song rhythms are generated in the database so that user can 
        // access them if he wants too
        generateAllAdaptedRhythms();

    }

    @Override
    public void removeSongParts(List<SongPart> spts) throws UnsupportedEditException
    {
        if (spts == null)
        {
            throw new IllegalArgumentException("this=" + this + " spts=" + spts);
        }


        if (spts.isEmpty())
        {
            return;
        }


        // Check that after the operation each AdaptedRhythm has its source rhythm present
        var remainingSpts = new ArrayList<SongPart>(songParts);
        remainingSpts.removeAll(spts);
        AdaptedRhythm faultyAdRhythm = checkAdaptedRhythmConsistency(remainingSpts);
        if (faultyAdRhythm != null)
        {
            var sr = faultyAdRhythm.getSourceRhythm();
            String msg = "Can't remove rhythm " + sr + ": it is used by adapted rhythm " + faultyAdRhythm.getName();
            throw new UnsupportedEditException(msg);
        }


        // Check change is not vetoed by listeners 
        var event = new SptRemovedEvent(this, spts);
        authorizeChangeEvent(event);        // Possible exception here!


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
                fireAuthorizedChangeEvent(new SptAddedEvent(SongStructureImpl.this, saveSpts));
            }

            @Override
            public void redoBody()
            {
                songParts = new ArrayList<>(newSpts);            // Must use a copy to make sure newSpts remains unaffected
                updateStartBarIndexes();
                fireAuthorizedChangeEvent(new SptRemovedEvent(SongStructureImpl.this, saveSpts));
            }
        };


        // Before change event
        fireUndoableEditHappened(edit);


        // Fire change event
        fireAuthorizedChangeEvent(event);

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
                fireAuthorizedChangeEvent(new SptResizedEvent(SongStructureImpl.this, saveMap));
            }

            @Override
            public void redoBody()
            {
                for (SongPart spt : saveMap.getKeys())
                {
                    ((SongPartImpl) spt).setNbBars(saveMap.getValue(spt));
                }
                updateStartBarIndexes();
                fireAuthorizedChangeEvent(new SptResizedEvent(SongStructureImpl.this, oldMap));
            }
        };

        var event = new SptResizedEvent(this, oldMap);


        fireUndoableEditHappened(edit);


        // Fire event
        fireAuthorizedChangeEvent(event);
    }

    /**
     * We need a method that works with a list of SongParts because replacing a single spt at a time may cause problems when there
     * is an unsupportedEditException.
     * <p>
     * Example: We have spt1=rhythm0, spt2=rhythm0, spt3=rhythm1. There are enough Midi channels for both rhythms.<br>
     * We want to change rhythm of both spt1 and spt2. If we do one spt at a time, after the first replacement on spt0 we'll have
     * 3 rhythms and possibly our listeners will trigger an UnsupportedEditException (if not enough Midi channels), though there
     * should be no problem since we want to change both spt1 AND spt2 !
     *
     * @param oldSpts
     * @param newSpts
     * @throws UnsupportedEditException The exception will be thrown before any change is done.
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


        // Check that after the operation each AdaptedRhythm must have its source rhythm present
        var remainingSpts = new ArrayList<SongPart>(songParts);
        remainingSpts.removeAll(oldSpts);
        remainingSpts.addAll(newSpts);
        AdaptedRhythm faultyAdRhythm = checkAdaptedRhythmConsistency(remainingSpts);
        if (faultyAdRhythm != null)
        {
            var sr = faultyAdRhythm.getSourceRhythm();
            String msg = "Can't replace song parts: adapted rhythm " + faultyAdRhythm.getName() + " can not be used if its source rhythm (" + sr.getName() + ") is not present";
            throw new UnsupportedEditException(msg);
        }


        // Check change is not vetoed
        var event = new SptReplacedEvent(SongStructureImpl.this, oldSpts, newSpts);
        authorizeChangeEvent(event);            // Possible UnsupportedEditException here


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


        // Save new state for undo/redo
        final ArrayList<SongPart> newSongParts = new ArrayList<>(songParts);
        final SmallMap<TimeSignature, Rhythm> newMapTsRhythm = mapTsLastRhythm.clone();


        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("Replace SongParts")
        {
            @Override
            public void undoBody()
            {
                LOGGER.log(Level.FINE, "ReplaceSongParts.undoBody() -- songParts=" + songParts);
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
                LOGGER.log(Level.FINE, "ReplaceSongParts.undoBody() songParts=" + songParts);
                // Don't use vetoablechange  : it already worked, normally there is no reason it would change            
                fireAuthorizedChangeEvent(new SptReplacedEvent(SongStructureImpl.this, newSpts, oldSpts));
            }

            @Override
            public void redoBody()
            {
                LOGGER.log(Level.FINE, "ReplaceSongParts.redoBody() -- songParts=" + songParts);
                // Restore the state of the songStructure
                songParts = new ArrayList<>(newSongParts);      // Must use a copy to make sure newSongParts remains unaffected
                mapTsLastRhythm = newMapTsRhythm.clone();           // Must use a copy to make sure map remains unaffected                        
                // Change the container of the replacing songparts
                for (SongPart newSpt : newSpts)
                {
                    ((SongPartImpl) newSpt).setContainer(SongStructureImpl.this);
                }
                LOGGER.log(Level.FINE, "ReplaceSongParts.redoBody() songParts=" + songParts);
                // Don't use vetoablechange : it already worked, normally there is no reason it would change
                fireAuthorizedChangeEvent(new SptReplacedEvent(SongStructureImpl.this, oldSpts, newSpts));
            }
        };


        // Need to be fired before change event
        fireUndoableEditHappened(edit);


        // Notify listeners        
        fireAuthorizedChangeEvent(event);


        // Make sure all AdaptedRhythms for the song rhythms are generated in the database so that user can 
        // access them if he wants too
        generateAllAdaptedRhythms();
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
                fireAuthorizedChangeEvent(new SptRenamedEvent(SongStructureImpl.this, save.getKeys()));
            }

            @Override
            public void redoBody()
            {
                for (SongPart wspt : save.getKeys())
                {
                    ((SongPartImpl) wspt).SetName(name);
                }
                fireAuthorizedChangeEvent(new SptRenamedEvent(SongStructureImpl.this, save.getKeys()));
            }
        };

        fireUndoableEditHappened(edit);

        // Fire event                
        fireAuthorizedChangeEvent(new SptRenamedEvent(this, save.getKeys()));
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
                fireAuthorizedChangeEvent(new RpChangedEvent(SongStructureImpl.this, wspt, rp, newValue, oldValue));
            }

            @Override
            public void redoBody()
            {
                wspt.setRPValue(rp, newValue);
                fireAuthorizedChangeEvent(new RpChangedEvent(SongStructureImpl.this, wspt, rp, oldValue, newValue));
            }
        };
        fireUndoableEditHappened(edit);

        // Fire event                
        fireAuthorizedChangeEvent(new RpChangedEvent(SongStructureImpl.this, wspt, rp, oldValue, newValue));
    }


    @Override
    public Rhythm getLastUsedRhythm(TimeSignature ts)
    {
        return mapTsLastRhythm.getValue(ts);
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
    public List<SongPart> getSongParts(Predicate<SongPart> tester)
    {
        return songParts.stream().filter(tester).collect(Collectors.toList());
    }

    @Override
    public Position getPosition(float posInBeats)
    {
        if (posInBeats < 0)
        {
            throw new IllegalArgumentException("posInBeats=" + posInBeats);
        }
        for (SongPart spt : songParts)
        {
            FloatRange rg = getBeatRange(spt.getBarRange());
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
    public float getPositionInNaturalBeats(int barIndex)
    {
        if (barIndex < 0 || barIndex > getSizeInBars())
        {
            throw new IllegalArgumentException("barIndex=" + barIndex);
        }
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
            throw new IllegalArgumentException("clsItem=" + clsItem + " not found in parent section items. section=" + section + ", spt=" + spt);
        }
        Position pos = clsItem.getPosition();
        int relBar = pos.getBar() - section.getPosition().getBar();
        Position res = new Position(spt.getStartBarIndex() + relBar, pos.getBeat());
        return res;
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


    /**
     * Make sure all possible AdaptedRhythms are generated if this is a multi-time signature song.
     */
    public void generateAllAdaptedRhythms()
    {
        RhythmDatabase rdb = RhythmDatabase.getDefault();
        List<Rhythm> rhythms = SongStructure.getUniqueRhythms(this, true);
        Set<TimeSignature> timeSignatures = rhythms
                .stream()
                .map(r -> r.getTimeSignature())
                .collect(Collectors.toSet());

        for (Rhythm r : rhythms)
        {
            for (TimeSignature ts : timeSignatures)
            {
                if (!ts.equals(r.getTimeSignature()))
                {
                    rdb.getAdaptedRhythm(r, ts);    // This will make the rhythm created and available in the database
                }
            }
        }
    }

    // -------------------------------------------------------------------------------------------
    // Private functions
    // -------------------------------------------------------------------------------------------

    /**
     * Internal implementation, no checks performed.
     *
     * @param spt
     * @throws UnsupportedEditException
     */
    private void addSongPartInternal(final SongPart spt) 
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
                fireAuthorizedChangeEvent(new SptRemovedEvent(SongStructureImpl.this, Arrays.asList(spt)));
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
                fireAuthorizedChangeEvent(new SptAddedEvent(SongStructureImpl.this, Arrays.asList(spt)));
            }
        };

        
        // Need to be before the change event
        fireUndoableEditHappened(edit);

        
        // Fire change event
        fireAuthorizedChangeEvent(new SptAddedEvent(this, Arrays.asList(spt)));
    }

    /**
     * Check that each AdaptedRhythm has its source rhythm present in the specified SongParts.
     *
     * @param spts
     * @return Return null if OK, otherwise return the AdaptedRhythm which causes problem.
     */
    private AdaptedRhythm checkAdaptedRhythmConsistency(ArrayList<SongPart> spts)
    {
        var rhythms = spts.stream().map(spt -> spt.getRhythm()).collect(Collectors.toList());
        for (SongPart spt : spts)
        {
            if (spt.getRhythm() instanceof AdaptedRhythm)
            {
                AdaptedRhythm ar = (AdaptedRhythm) spt.getRhythm();
                Rhythm sr = ar.getSourceRhythm();
                if (!rhythms.contains(sr))
                {
                    return ar;
                }
            }
        }
        return null;
    }


    private int getSptLastBarIndex(int sptIndex)
    {
        if (sptIndex < 0 || sptIndex >= songParts.size())
        {
            throw new IllegalArgumentException("rpIndex=" + sptIndex);
        }
        SongPart spt = songParts.get(sptIndex);
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

    /**
     * Make sure change is authorized by all listeners.
     *
     * @param event
     * @throws UnsupportedEditException
     */
    protected void authorizeChangeEvent(SgsChangeEvent event) throws UnsupportedEditException
    {
        if (event == null)
        {
            throw new IllegalArgumentException("event=" + event);
        }
        var ls = listeners.toArray(new SgsChangeListener[0]);
        for (SgsChangeListener l : ls)
        {
            l.authorizeChange(event);   // Possible exception here
        }
    }

    /**
     * Fire an authorized change event to all listeners.
     *
     * @param event
     */
    protected void fireAuthorizedChangeEvent(SgsChangeEvent event)
    {
        if (event == null)
        {
            throw new IllegalArgumentException("event=" + event);
        }
        for (SgsChangeListener l : listeners.toArray(new SgsChangeListener[0]))
        {
            l.songStructureChanged(event);
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
                    sgs.addSongPartInternal(spt);
                } catch (Exception ex)
                {
                    // Translate to an ObjectStreamException
                    throw new InvalidObjectException(ex.getLocalizedMessage());
                }
            }
            return sgs;
        }
    }

}

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
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.rhythm.database.api.RhythmDatabase;
import org.jjazz.undomanager.api.SimpleEdit;
import org.jjazz.util.api.SmallMap;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.rhythm.api.AdaptedRhythm;
import org.jjazz.rhythm.database.api.RhythmInfo;
import org.jjazz.rhythm.database.api.UnavailableRhythmException;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SgsChangeListener;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.event.SgsActionEvent;
import org.jjazz.util.api.FloatRange;
import org.jjazz.util.api.IntRange;
import org.jjazz.util.api.ResUtil;

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
            throw new IllegalArgumentException("cls=" + cls + " keepUpdated=" + keepUpdated);   //NOI18N
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
        } else if (!songRange.contains(rg))
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
    public SongPart createSongPart(Rhythm r, String name, int startBarIndex, int nbBars, CLI_Section parentSection, boolean reusePrevParamValues)
    {
        if (r == null || startBarIndex < 0 || nbBars < 0 || name == null)
        {
            throw new IllegalArgumentException("r=" + r + " name=" + name //NOI18N
                    + " startBarIndex=" + startBarIndex + " nbBars=" + nbBars
                    + " parentSection=" + parentSection + " reusePrevParamValues=" + reusePrevParamValues);
        }

        SongPartImpl spt;

        if (startBarIndex > 0 && reusePrevParamValues)
        {
            // New song part which reuse parameters from previous sont part
            SongPart prevSpt = getSongPart(startBarIndex - 1);
            spt = (SongPartImpl) prevSpt.clone(r, startBarIndex, nbBars, parentSection);

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
    public void authorizeAddSongParts(List<SongPart> spts) throws UnsupportedEditException
    {
        // Check that after the operation each AdaptedRhythm must have its source rhythm present
        var finalSpts = new ArrayList<SongPart>(songParts);
        finalSpts.addAll(spts);
        AdaptedRhythm faultyAdRhythm = checkAdaptedRhythmConsistency(finalSpts);
        if (faultyAdRhythm != null)
        {
            var sr = faultyAdRhythm.getSourceRhythm();
            String msg = ResUtil.getString(getClass(), "ERR_CantAddAdaptedRhythm", faultyAdRhythm.getName(), sr.getName());
            throw new UnsupportedEditException(msg);
        }


        // Check change is not vetoed by listeners
        var event = new SptAddedEvent(this, spts);
        authorizeChangeEvent(event);            // Possible exception here! 
    }

    @Override
    public void addSongParts(final List<SongPart> spts) throws UnsupportedEditException
    {
        addSongParts(spts, true);
    }


    @Override
    public void authorizeRemoveSongParts(List<SongPart> spts) throws UnsupportedEditException
    {
        // Check that after the operation each AdaptedRhythm has its source rhythm present
        var remainingSpts = new ArrayList<SongPart>(songParts);
        remainingSpts.removeAll(spts);
        AdaptedRhythm faultyAdRhythm = checkAdaptedRhythmConsistency(remainingSpts);
        if (faultyAdRhythm != null)
        {
            var sr = faultyAdRhythm.getSourceRhythm();
            String msg = ResUtil.getString(getClass(), "ERR_CantRemoveRhythm", sr, faultyAdRhythm.getName());
            throw new UnsupportedEditException(msg);
        }


        // Check change is not vetoed by listeners 
        var event = new SptRemovedEvent(this, spts);
        authorizeChangeEvent(event);        // Possible exception here!
    }

    @Override
    public void removeSongParts(List<SongPart> spts) throws UnsupportedEditException
    {
        removeSongParts(spts, true);
    }


    @Override
    public void resizeSongParts(SmallMap<SongPart, Integer> mapSptSize)
    {
        resizeSongParts(mapSptSize, true);
    }


    @Override
    public void authorizeReplaceSongParts(List<SongPart> oldSpts, List<SongPart> newSpts) throws UnsupportedEditException
    {
        // Check that after the operation each AdaptedRhythm must have its source rhythm present
        var remainingSpts = new ArrayList<SongPart>(songParts);
        remainingSpts.removeAll(oldSpts);
        remainingSpts.addAll(newSpts);
        AdaptedRhythm faultyAdRhythm = checkAdaptedRhythmConsistency(remainingSpts);
        if (faultyAdRhythm != null)
        {
            var sr = faultyAdRhythm.getSourceRhythm();
            String msg = ResUtil.getString(getClass(), "ERR_CantReplaceSongPart", faultyAdRhythm.getName(), sr.getName());
            throw new UnsupportedEditException(msg);
        }

        // Check that change is not vetoed
        var event = new SptReplacedEvent(SongStructureImpl.this, oldSpts, newSpts);
        authorizeChangeEvent(event);            // Possible UnsupportedEditException here
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
        replaceSongParts(oldSpts, newSpts, true);
    }


    @Override
    public void setSongPartsName(List<SongPart> spts, final String name)
    {
        setSongPartsName(spts, name, true);
    }


    @Override
    public <T> void setRhythmParameterValue(SongPart spt, final RhythmParameter<T> rp, final T newValue)
    {
        setRhythmParameterValue(spt, rp, newValue, true);
    }


    @Override
    public Rhythm getLastUsedRhythm(TimeSignature ts)
    {
        Rhythm r = mapTsLastRhythm.getValue(ts);
        if (r instanceof AdaptedRhythm)
        {
            // Don't return an AdaptedRhythm if its source rhythm is not present
            Rhythm sr = ((AdaptedRhythm) r).getSourceRhythm();
            if (!getUniqueRhythms(true).contains(sr))
            {
                r = null;
            }
        }
        LOGGER.fine("getLastUsedRhythm() ts=" + ts + " result r=" + r);   //NOI18N
        return r;
    }


    @Override
    public Rhythm getRecommendedRhythm(TimeSignature ts, int sptBarIndex)
    {
        if (ts == null || sptBarIndex < 0)
        {
            throw new IllegalArgumentException("ts=" + ts + " sptBarIndex=" + sptBarIndex);   //NOI18N
        }

        RhythmDatabase rdb = RhythmDatabase.getDefault();


        LOGGER.fine("getRecommendedRhythm() ts=" + ts + " sptBarIndex=" + sptBarIndex);   //NOI18N


        // Try to use the last used rhythm for this new time signature
        Rhythm r = getLastUsedRhythm(ts);


        // Try to use an AdaptedRhythm for the previous song part's rhythm
        if (r == null)
        {
            SongPart prevSpt = sptBarIndex == 0 ? null : getSongPart(sptBarIndex - 1);
            if (prevSpt != null)
            {
                Rhythm prevRhythm = prevSpt.getRhythm();
                if (prevRhythm instanceof AdaptedRhythm)
                {
                    prevRhythm = ((AdaptedRhythm) prevRhythm).getSourceRhythm();
                }
                r = rdb.getAdaptedRhythmInstance(prevRhythm, ts);        // may be null
            }
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
                LOGGER.warning("getRecommendedRhythm() Can't get rhythm instance for " + ri.getName() + ". Using stub rhythm instead. ex=" + ex.getMessage());   //NOI18N
                r = rdb.getDefaultStubRhythmInstance(ts);  // non null
            }
        }

        LOGGER.fine("getRecommendedRhythm() ts=" + ts + " sptBarIndex=" + sptBarIndex + " result r=" + r);   //NOI18N
        return r;
    }


    @Override
    public SongPart getSongPart(int absoluteBarIndex)
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
    public List<SongPart> getSongParts(Predicate<SongPart> tester)
    {
        return songParts.stream().filter(tester).collect(Collectors.toList());
    }

    @Override
    public Position getPosition(float posInBeats)
    {
        if (posInBeats < 0)
        {
            throw new IllegalArgumentException("posInBeats=" + posInBeats);   //NOI18N
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
            throw new IllegalArgumentException("barIndex=" + barIndex);   //NOI18N
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
            throw new IllegalStateException("parentCls is null. spt=" + spt + " clsItem=" + clsItem);   //NOI18N
        }
        CLI_Section section = spt.getParentSection();
        if (!parentCls.getItems(spt.getParentSection(), clsItem.getClass()).contains(clsItem))
        {
            throw new IllegalArgumentException("clsItem=" + clsItem + " not found in parent section items. section=" + section + ", spt=" + spt);   //NOI18N
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
            throw new NullPointerException("l=" + l);   //NOI18N
        }
        listeners.remove(l);
        listeners.add(l);
    }

    @Override
    public void removeSgsChangeListener(SgsChangeListener l)
    {
        if (l == null)
        {
            throw new NullPointerException("l=" + l);   //NOI18N
        }
        listeners.remove(l);
    }

    @Override
    public void addUndoableEditListener(UndoableEditListener l)
    {
        if (l == null)
        {
            throw new NullPointerException("l=" + l);   //NOI18N
        }
        undoListeners.remove(l);
        undoListeners.add(l);
    }

    @Override
    public void removeUndoableEditListener(UndoableEditListener l)
    {
        if (l == null)
        {
            throw new NullPointerException("l=" + l);   //NOI18N
        }
        undoListeners.remove(l);
    }


    /**
     * Make sure all possible AdaptedRhythms are generated if this is a multi-time signature song.
     */
    public void generateAllAdaptedRhythms()
    {
        RhythmDatabase rdb = RhythmDatabase.getDefault();
        Set<TimeSignature> timeSignatures = getUniqueRhythms(false) // Include AdaptedRhythms to get all time signatures
                .stream()
                .map(r -> r.getTimeSignature())
                .collect(Collectors.toSet());

        for (Rhythm r : getUniqueRhythms(true))         // No adapted rhythms
        {
            for (TimeSignature ts : timeSignatures)
            {
                if (!ts.equals(r.getTimeSignature()))
                {
                    // Have the adapted rhythm created and made available in the database
                    if (rdb.getAdaptedRhythmInstance(r, ts) == null)
                    {
                        LOGGER.info("generateAllAdaptedRhythms() Can't get a " + ts + "-adapted rhythm for r=" + r);   //NOI18N
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------------------------------------
    // Private functions
    // -------------------------------------------------------------------------------------------
    private void addSongParts(final List<SongPart> spts, boolean enableActionEvent) throws UnsupportedEditException
    {
        if (spts == null)
        {
            throw new IllegalArgumentException("spts=" + spts);   //NOI18N
        }


        LOGGER.fine("addSongParts() -- spts=" + spts);   //NOI18N


        if (spts.isEmpty())
        {
            return;
        }


        // Possible exception here!
        authorizeAddSongParts(spts);

        fireActionEvent(enableActionEvent, "addSongParts", false);

        // Change state
        for (SongPart spt : spts)
        {
            addSongPartImpl(spt);
        }

        fireActionEvent(enableActionEvent, "addSongParts", true);


        // Make sure all AdaptedRhythms for the song rhythms are generated in the database so that user can 
        // access them if he wants too
        generateAllAdaptedRhythms();

    }

    private void removeSongParts(List<SongPart> spts, boolean enableActionEvent) throws UnsupportedEditException
    {
        if (spts == null)
        {
            throw new IllegalArgumentException("this=" + this + " spts=" + spts);   //NOI18N
        }

        LOGGER.fine("removeSongParts() -- spts=" + spts);   //NOI18N


        if (spts.isEmpty())
        {
            return;
        }


        // Possible exception here
        authorizeRemoveSongParts(spts);


        fireActionEvent(enableActionEvent, "removeSongParts", false);


        // Perform the change
        removeSongPartsImpl(spts);

        fireActionEvent(enableActionEvent, "removeSongParts", true);

    }

    private void resizeSongParts(SmallMap<SongPart, Integer> mapSptSize, boolean enableActionEvent)
    {
        if (mapSptSize == null)
        {
            throw new IllegalArgumentException("this=" + this + " mapSptsSize=" + mapSptSize);   //NOI18N
        }

        LOGGER.fine("resizeSongParts() -- mapSptSize=" + mapSptSize);   //NOI18N


        if (mapSptSize.isEmpty())
        {
            return;
        }

        fireActionEvent(enableActionEvent, "resizeSongParts", false);


        final SmallMap<SongPart, Integer> saveMap = mapSptSize.clone();
        final SmallMap<SongPart, Integer> oldMap = new SmallMap<>();
        for (SongPart spt : mapSptSize.getKeys())
        {
            if (!songParts.contains(spt))
            {
                throw new IllegalArgumentException("this=" + this + " spt=" + spt + " mapSptsSize=" + mapSptSize);   //NOI18N
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
                LOGGER.finer("resizeSongParts.undoBody() mapSptSize=" + mapSptSize);   //NOI18N
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
                LOGGER.finer("resizeSongParts.redoBody() mapSptSize=" + mapSptSize);   //NOI18N
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


        fireActionEvent(enableActionEvent, "resizeSongParts", true);
    }

    private void replaceSongParts(final List<SongPart> oldSpts, final List<SongPart> newSpts, boolean enableActionEvent) throws UnsupportedEditException
    {
        if (oldSpts == null || newSpts == null || oldSpts.size() != newSpts.size())
        {
            throw new IllegalArgumentException("this=" + this + " oldSpts=" + oldSpts + " newSpts=" + newSpts);   //NOI18N
        }


        LOGGER.log(Level.FINE, "replaceSongParts() -- oldSpts=={0} newSpts={1}", new Object[]   //NOI18N
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
                throw new IllegalArgumentException("this=" + this + " oldSpts=" + oldSpts + " newSpts=" + newSpts);   //NOI18N
            }
        }
        if (oldSpts.equals(newSpts))
        {
            return;
        }


        // Possible exception here!
        authorizeReplaceSongParts(oldSpts, newSpts);


        fireActionEvent(enableActionEvent, "replaceSongParts", false);


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
                LOGGER.log(Level.FINER, "ReplaceSongParts.undoBody() songParts=" + songParts);   //NOI18N

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
                // Don't use vetoablechange  : it already worked, normally there is no reason it would change            
                fireAuthorizedChangeEvent(new SptReplacedEvent(SongStructureImpl.this, newSpts, oldSpts));
            }

            @Override
            public void redoBody()
            {
                LOGGER.log(Level.FINER, "ReplaceSongParts.redoBody() songParts=" + songParts);   //NOI18N

                // Restore the state of the songStructure
                songParts = new ArrayList<>(newSongParts);      // Must use a copy to make sure newSongParts remains unaffected
                mapTsLastRhythm = newMapTsRhythm.clone();           // Must use a copy to make sure map remains unaffected                        
                // Change the container of the replacing songparts
                for (SongPart newSpt : newSpts)
                {
                    ((SongPartImpl) newSpt).setContainer(SongStructureImpl.this);
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


        fireActionEvent(enableActionEvent, "replaceSongParts", true);


        // Make sure all AdaptedRhythms for the song rhythms are generated in the database so that user can 
        // access them if he wants too
        generateAllAdaptedRhythms();


    }


    private void setSongPartsName(List<SongPart> spts, final String name, boolean enableActionEvent)
    {
        if (spts == null)
        {
            throw new IllegalArgumentException("this=" + this + " spts=" + spts + " name=" + name);   //NOI18N
        }

        LOGGER.fine("setSongPartsName() spts=" + spts + " name=" + name);   //NOI18N

        if (spts.isEmpty() || (spts.size() == 1 && spts.get(0).getName().equals(name)))
        {
            return;
        }

        fireActionEvent(enableActionEvent, "setSongPartsName", false);

        final SmallMap<SongPart, String> save = new SmallMap<>();
        for (SongPart spt : spts)
        {
            SongPartImpl wspt = (SongPartImpl) spt;
            save.putValue(wspt, wspt.getName());
            wspt.setName(name);
        }
        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("Rename SongParts")
        {
            @Override
            public void undoBody()
            {
                LOGGER.finer("setSongPartsName.undoBody() spts=" + spts + " name=" + name);   //NOI18N
                for (SongPart wspt : save.getKeys())
                {
                    ((SongPartImpl) wspt).setName(save.getValue(wspt));
                }
                fireAuthorizedChangeEvent(new SptRenamedEvent(SongStructureImpl.this, save.getKeys()));
            }

            @Override
            public void redoBody()
            {
                LOGGER.finer("setSongPartsName.redoBody() spts=" + spts + " name=" + name);   //NOI18N
                for (SongPart wspt : save.getKeys())
                {
                    ((SongPartImpl) wspt).setName(name);
                }
                fireAuthorizedChangeEvent(new SptRenamedEvent(SongStructureImpl.this, save.getKeys()));
            }
        };

        fireUndoableEditHappened(edit);

        // Fire event                
        fireAuthorizedChangeEvent(new SptRenamedEvent(this, save.getKeys()));


        fireActionEvent(enableActionEvent, "setSongPartsName", true);
    }

    private <T> void setRhythmParameterValue(SongPart spt, final RhythmParameter<T> rp, final T newValue, boolean enableActionEvent)
    {
        if (spt == null || rp == null || newValue == null || !songParts.contains(spt)
                || !spt.getRhythm().getRhythmParameters().contains(rp))
        {
            throw new IllegalArgumentException("this=" + this + " spt=" + spt + " rp=" + rp + " newValue=" + newValue);   //NOI18N
        }

        LOGGER.fine("setRhythmParameterValue() -- spt=" + spt + " rp=" + rp + " newValue=" + newValue);   //NOI18N

        final T oldValue = spt.getRPValue(rp);
        if (oldValue.equals(newValue))
        {
            return;
        }


        fireActionEvent(enableActionEvent, "setRhythmParameterValue", false);


        // Update the value
        final SongPartImpl wspt = (SongPartImpl) spt;
        wspt.setRPValue(rp, newValue);


        // Create the undoable event
        UndoableEdit edit = new SimpleEdit("set Rhythm Parameter Value")
        {
            @Override
            public void undoBody()
            {
                LOGGER.finer("setRhythmParameterValue.undoBody() spt=" + spt + " rp=" + rp + " newValue=" + newValue);   //NOI18N
                wspt.setRPValue(rp, oldValue);
                fireAuthorizedChangeEvent(new RpChangedEvent(SongStructureImpl.this, wspt, rp, newValue, oldValue));
            }

            @Override
            public void redoBody()
            {
                LOGGER.finer("setRhythmParameterValue.redoBody() spt=" + spt + " rp=" + rp + " newValue=" + newValue);   //NOI18N
                wspt.setRPValue(rp, newValue);
                fireAuthorizedChangeEvent(new RpChangedEvent(SongStructureImpl.this, wspt, rp, oldValue, newValue));
            }
        };
        fireUndoableEditHappened(edit);

        // Fire event                
        fireAuthorizedChangeEvent(new RpChangedEvent(SongStructureImpl.this, wspt, rp, oldValue, newValue));


        fireActionEvent(enableActionEvent, "setRhythmParameterValue", true);
    }

    /**
     *
     * @param doFire If false do nothing.
     * @param actionId
     * @param complete
     */
    private void fireActionEvent(boolean doFire, String actionId, boolean complete)
    {
        if (!doFire)
        {
            return;
        }

        // Create an undoable event for this event which does nothing but refiring the SgsActionEvent
        UndoableEdit edit = new SimpleEdit("SgsActionEventEdit(" + actionId + ")")
        {
            @Override
            public void undoBody()
            {
                fireAuthorizedChangeEvent(new SgsActionEvent(SongStructureImpl.this, actionId, !complete, true));
            }

            @Override
            public void redoBody()
            {
                fireAuthorizedChangeEvent(new SgsActionEvent(SongStructureImpl.this, actionId, complete, false));
            }
        };
        fireUndoableEditHappened(edit);


        fireAuthorizedChangeEvent(new SgsActionEvent(this, actionId, complete, false));

    }

    /**
     * Internal implementation, no checks performed.
     *
     * @param spt
     */
    protected void addSongPartImpl(final SongPart spt)
    {
        if (spt == null)
        {
            throw new IllegalArgumentException("this=" + this + " spt=" + spt);   //NOI18N
        }

        LOGGER.log(Level.FINE, "addSongPartInternal() -- spt={0}", spt);   //NOI18N


        // Save the old state
        final ArrayList<SongPart> oldSpts = new ArrayList<>(songParts);
        final SmallMap<TimeSignature, Rhythm> oldMapTsRhythm = mapTsLastRhythm.clone();
        final SongStructure oldContainer = spt.getContainer();


        int barIndex = spt.getStartBarIndex();
        if (songParts.contains(spt) || barIndex > getSizeInBars())
        {
            throw new IllegalArgumentException("this=" + this + " spt=" + spt);   //NOI18N
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
                throw new IllegalArgumentException("this=" + this + " spt=" + spt + " curSpt=" + curSpt);   //NOI18N
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
                LOGGER.finer("addSongPartInternal.undoBody() spt=" + spt);   //NOI18N

                ((SongPartImpl) spt).setContainer(oldContainer);
                songParts = new ArrayList<>(oldSpts);         // Must use a copy to make sure oldSpts remains unaffected
                mapTsLastRhythm = oldMapTsRhythm.clone();           // Must use a copy to make sure map remains unaffected
                updateStartBarIndexes();

                // Don't use vetoable change  : it already worked, normally there is no reason it would change
                fireAuthorizedChangeEvent(new SptRemovedEvent(SongStructureImpl.this, Arrays.asList(spt)));
            }

            @Override
            public void redoBody()
            {
                LOGGER.finer("addSongPartInternal.redoBody() spt=" + spt);   //NOI18N

                ((SongPartImpl) spt).setContainer(SongStructureImpl.this);
                songParts = new ArrayList<>(newSpts);          // Must use a copy to make sure newSpts remains unaffected
                mapTsLastRhythm = newMapTsRhythm.clone();            // Must use a copy to make sure map remains unaffected
                updateStartBarIndexes();

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
            throw new IllegalArgumentException("this=" + this + " spts=" + spts);   //NOI18N
        }

        LOGGER.finer("removeSongPartInternal() -- spts=" + spts);   //NOI18N

        // Save state
        final ArrayList<SongPart> oldSpts = new ArrayList<>(songParts);


        // Update state
        for (SongPart spt : spts)
        {
            if (!songParts.remove(spt))
            {
                throw new IllegalArgumentException("this=" + this + " spt=" + spt + " songParts=" + songParts);   //NOI18N
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
                LOGGER.finer("removeSongPartInternal.undoBody() spts=" + spts);   //NOI18N
                songParts = new ArrayList<>(oldSpts);            // Must use a copy to make sure oldSpts remains unaffected
                updateStartBarIndexes();
                fireAuthorizedChangeEvent(new SptAddedEvent(SongStructureImpl.this, saveSpts));
            }

            @Override
            public void redoBody()
            {
                LOGGER.finer("removeSongPartInternal.redoBody() spts=" + spts);   //NOI18N
                songParts = new ArrayList<>(newSpts);            // Must use a copy to make sure newSpts remains unaffected
                updateStartBarIndexes();
                fireAuthorizedChangeEvent(new SptRemovedEvent(SongStructureImpl.this, saveSpts));
            }
        };


        // Before change event
        fireUndoableEditHappened(edit);


        // Fire change event
        var event = new SptRemovedEvent(this, spts);
        fireAuthorizedChangeEvent(event);

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
            throw new IllegalArgumentException("rpIndex=" + sptIndex);   //NOI18N
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
    private void authorizeChangeEvent(SgsChangeEvent event) throws UnsupportedEditException
    {
        if (event == null)
        {
            throw new IllegalArgumentException("event=" + event);   //NOI18N
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
    private void fireAuthorizedChangeEvent(SgsChangeEvent event)
    {
        if (event == null)
        {
            throw new IllegalArgumentException("event=" + event);   //NOI18N
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
            throw new IllegalArgumentException("edit=" + edit);   //NOI18N
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
                throw new IllegalStateException("spSpts=" + spSpts);   //NOI18N
            }

            SongStructureImpl sgs = new SongStructureImpl(spParentCls, spKeepUpdated);
            for (SongPart spt : spSpts)
            {
                try
                {
                    // This will set again the container of the songparts
                    sgs.addSongPartImpl(spt);
                } catch (Exception ex)
                {
                    // Translate to an ObjectStreamException
                    throw new InvalidObjectException(ex.getMessage());
                }
            }
            return sgs;
        }
    }

}

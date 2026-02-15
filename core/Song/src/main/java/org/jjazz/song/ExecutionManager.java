/*
 * 
 *   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 *   Copyright @2019 Jerome Lelasseux. All rights reserved.
 * 
 *   This file is part of the JJazzLab software.
 *    
 *   JJazzLab is free software: you can redistribute it and/or modify
 *   it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *   as published by the Free Software Foundation, either version 3 of the License, 
 *   or (at your option) any later version.
 * 
 *   JJazzLab is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *  
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *  
 *   Contributor(s): 
 * 
 */
package org.jjazz.song;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import org.jjazz.chordleadsheet.ChordLeadSheetImpl;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.event.ClsChangeEvent;
import org.jjazz.chordleadsheet.api.item.WritableItem;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.song.api.Song;
import org.jjazz.songstructure.SongPartImpl;
import org.jjazz.songstructure.SongStructureImpl;
import org.jjazz.songstructure.api.event.SgsChangeEvent;
import org.openide.util.Exceptions;

/**
 * Execute Song-related API methods with a safe concurrency design while ensuring global Song data consistency across Song model components: ChordLeadSheet,
 * SongStructure, SongParts, ChordLeadSheetItems, MidiMix.
 * <p>
 * The most common concurrency scenario is a background music generation thread which often calls SongContext/Song.getDeepCopy(), while user is possibly making
 * changes to the song via the UI. In the future we could also have the case of a plugin which updates the current song while user is also modifying it via the
 * UI.
 * <p>
 * Global Song data consistency: a ChordLeadSheet change might impact SongStructure (and MidiMix) and vice-versa. We need to make sure that all these changes
 * (managed by the SongInternalUpdater) are executed under the same write lock, so that getDeepCopy() does not capture an inconsistent state.
 * <p>
 * The ExecutionManager also manages change event firing from the Song components. Objective is that event firing takes place outside of any synchronization
 * lock.
 */
public class ExecutionManager
{

    private final ReentrantReadWriteLock lock;
    private final Song song;
    private MidiMix midiMix;
    private final SongInternalUpdater songInternalUpdater;

    public ExecutionManager(Song song)
    {
        Objects.requireNonNull(song);
        this.lock = new ReentrantReadWriteLock();
        this.song = song;
        this.songInternalUpdater = new SongInternalUpdater(song);
    }

    public MidiMix getMidiMix()
    {
        return this.midiMix;
    }

    public Song getSong()
    {
        return song;
    }

    public boolean isWriteLockedByCurrentThread()
    {
        return lock.isWriteLockedByCurrentThread();
    }

    /**
     * Execute a read operation under read lock.
     *
     * @param <T>
     * @param readOperation
     * @return
     */
    public <T> T executeReadOperation(Supplier<T> readOperation)
    {
        lock.readLock().lock();
        try
        {
            return readOperation.get();
        } finally
        {
            lock.readLock().unlock();
        }
    }

    /**
     * Safely perform a mutating operation, possibly returning a value.
     * <p>
     * The operation is responsible to fire its undoable edit if relevant.
     *
     * @param <R>       The type of the return value
     * @param operation
     * @return The returnValue from WriteOperationResults. Can be null.
     */
    public <R> R executeWriteOperation(WriteOperation<R> operation)
    {
        Objects.requireNonNull(operation);

        // Wrap operation as a ThrowingWriteOperation in order to reuse executeWriteOperationThrowing()
        try
        {
            var res = executeWriteOperationThrowing(() -> operation.get());
            return res;
        } catch (UnsupportedEditException ex)
        {
            // Should never happen
            Exceptions.printStackTrace(ex);
            throw new IllegalStateException("executeWriteOperation() ex=" + ex);
        }

    }

    /**
     * Safely perform a mutating operation which can throw an UnsupportedEditException, possibly returning a value.
     * <p>
     * The operation is responsible to fire its undoable edit if relevant.
     *
     * @param <R>               The type of the return value
     * @param throwingOperation
     * @return The returnValue from WriteOperationResults. Can be null.
     * @throws org.jjazz.chordleadsheet.api.UnsupportedEditException
     */
    public <R> R executeWriteOperationThrowing(ThrowingWriteOperation<R> throwingOperation) throws UnsupportedEditException
    {
        Objects.requireNonNull(throwingOperation);

        List<WriteOperationResults> allOperationResults = new ArrayList<>();
        WriteOperationResults<R> operationResults = null;

        lock.writeLock().lock();
        try
        {
            // Execute operation
            operationResults = throwingOperation.get();        // throws UnsupportedEditException
            assert operationResults != null;
            allOperationResults.add(operationResults);


            // Possibly update other Song internal components under write lock
            List<WriteOperation> nextOperations = songInternalUpdater.getNextOperations(operationResults);
            for (var op : nextOperations)
            {
                var results = op.get();         // execute next operation
                assert results != null;
                allOperationResults.add(operationResults);
            }


        } finally
        {
            lock.writeLock().unlock();
        }


        // Fire the change events outside lock
        for (var opResult : allOperationResults)
        {
            if (opResult.clsChangeEvent() instanceof ClsChangeEvent cce)
            {
                // The main change event
                ((ChordLeadSheetImpl) cce.getSource()).fireChangeEvent(cce);

                // Possible associated ChordLeadSheetItem changes
                for (var propEvent : cce.getItemChanges())
                {
                    ((WritableItem) propEvent.getSource()).firePropertyChangeEvent(propEvent);
                }
            }

            if (opResult.sgsChangeEvent() instanceof SgsChangeEvent sce)
            {
                // The main change event
                ((SongStructureImpl) sce.getSource()).fireChangeEvent(sce);

                // Possible associated SongPart changes
                for (var propEvent : sce.getSongPartChanges())
                {
                    ((SongPartImpl) propEvent.getSource()).firePropertyChangeEvent(propEvent);
                }
            }
        }


        // Return the value from the operation
        R returnValue = (R) operationResults.returnValue();     // Can be null
        return returnValue;
    }

    public void preCheckChange(ClsChangeEvent event) throws UnsupportedEditException
    {
        songInternalUpdater.preCheckChange(event);
    }

    public void preCheckChange(SgsChangeEvent event) throws UnsupportedEditException
    {
        songInternalUpdater.preCheckChange(event);
    }


}

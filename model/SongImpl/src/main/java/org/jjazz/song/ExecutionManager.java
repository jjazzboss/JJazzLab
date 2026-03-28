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

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.chordleadsheet.ChordLeadSheetImpl;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.event.ClsChangeEvent;
import org.jjazz.chordleadsheet.api.item.WritableItem;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.MidiMixImpl;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongPropertyChangeEvent;
import org.jjazz.songstructure.SongPartImpl;
import org.jjazz.songstructure.SongStructureImpl;
import org.jjazz.songstructure.api.event.SgsChangeEvent;
import org.jjazz.utilities.api.ThrowingSupplier;
import org.openide.util.Exceptions;

/**
 * Execute Song-related API methods with a safe concurrency design while ensuring global Song data consistency across Song model components: Song,
 * ChordLeadSheet and ChordLeadSheetItems, SongStructure and SongParts, ChordLeadSheetItems, MidiMix.
 * <p>
 * The most common concurrency scenario is a background music generation thread which often calls SongContext/Song.getDeepCopy(), while user is possibly making
 * changes to the song via the UI. In the future we could also have the case of a plugin which updates the current song while user is also modifying it via the
 * UI.
 * <p>
 * Global Song data consistency: a ChordLeadSheet change might impact SongStructure (and MidiMix) and vice-versa. We need to make sure that all these derived
 * changes (managed by the SongInternalUpdater) are executed under the same write lock, so that getDeepCopy() does not capture an inconsistent state.
 * <p>
 * The ExecutionManager fires the change events from the Song components outside of write lock, after the primary and derived changes have been made.
 */
public class ExecutionManager
{

    private final ReentrantReadWriteLock lock;
    private MidiMix midiMix;
    private final SongInternalUpdater songInternalUpdater;
    private static final Logger LOGGER = Logger.getLogger(ExecutionManager.class.getSimpleName());

    public ExecutionManager()
    {
        this.lock = new ReentrantReadWriteLock();
        this.songInternalUpdater = null;
    }

    public ExecutionManager(Song song, boolean disableInternalUpdates)
    {
        this.lock = new ReentrantReadWriteLock();
        this.songInternalUpdater = disableInternalUpdates ? null : new SongInternalUpdater(song);
    }

    public MidiMix getMidiMix()
    {
        return this.midiMix;
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
     * Execute a read operation under read lock, possibly throwing an exception.
     *
     * @param <T>
     * @param <E>
     * @param readOperation
     * @return
     * @throws E
     */
    public <T, E extends Exception> T executeReadOperationThrowing(ThrowingSupplier<T, E> readOperation) throws E
    {
        lock.readLock().lock();
        try
        {
            return readOperation.get();     // throws E
        } finally
        {
            lock.readLock().unlock();
        }
    }

    /**
     * Safely perform a mutating operation, possibly returning a value.
     * <p>
     * The operation and its possible derived operations on Song components are executed under write lock. All change events are fired outside of write lock,
     * after the changes are complete.
     *
     * @param <R>       The type of the return value
     * @param operation The operation to execute. It is responsible to fire its undoable edit if relevant.
     * @return The returnValue from WriteOperationResults. Can be null.
     * @see #executeWriteOperationThrowing(org.jjazz.song.ThrowingWriteOperation)
     */
    public <R> R executeWriteOperation(WriteOperation<R> operation)
    {
        Objects.requireNonNull(operation);
        return executeWriteOperations(List.of(operation));
    }

    /**
     * Same as {@link #executeWriteOperation(org.jjazz.song.WriteOperation)} but for a list of operations.
     *
     * @param <R>        The type of the return value
     * @param operations
     * @return The returnValue from the first operation. Can be null.
     */
    public <R> R executeWriteOperations(List<WriteOperation> operations)
    {
        Objects.requireNonNull(operations);
        Preconditions.checkState(!isWriteLockedByCurrentThread(), "Already under writeLock! lock=" + lock.toString());

        if (operations.isEmpty())
        {
            return null;
        }

        List<WriteOperationResults> allOperationResults = new ArrayList<>();
        WriteOperationResults<R> operationResults = null;

        LOGGER.fine("executeWriteOperations() LOCKING");
        lock.writeLock().lock();
        try
        {
            operationResults = executeOperationChain(operations.getFirst(), allOperationResults);
            for (int i = 1; i < operations.size(); i++)
            {
                executeOperationChain(operations.get(i), allOperationResults);
            }
        } catch (UnsupportedEditException ex)
        {
            // Should never happen with WriteOperations
            Exceptions.printStackTrace(ex);
            throw new IllegalStateException("executeWriteOperation() ex=" + ex);
        } finally
        {
            LOGGER.fine("executeWriteOperations() UNLOCKING");
            lock.writeLock().unlock();
        }


        // Fire the change events outside lock
        fireAllOperationEvents(allOperationResults);


        // Return the value from the initial operation
        R returnValue = (R) operationResults.returnValue();     // Can be null
        return returnValue;
    }

    /**
     * Same as executeWriteOperations() except operation can throw an UnsupportedEditException.
     *
     * @param <R>               The type of the return value
     * @param throwingOperation The operation to execute. It is responsible to fire its undoable edit if relevant.
     * @return The returnValue from WriteOperationResults. Can be null.
     * @throws org.jjazz.chordleadsheet.api.UnsupportedEditException
     * @see #executeWriteOperation(org.jjazz.song.WriteOperation)
     */
    public <R> R executeWriteOperationThrowing(ThrowingWriteOperation<R> throwingOperation) throws UnsupportedEditException
    {
        Objects.requireNonNull(throwingOperation);
        Preconditions.checkState(!isWriteLockedByCurrentThread(), "Already under writeLock! lock=" + lock.toString());

        List<WriteOperationResults> allOperationResults = new ArrayList<>();
        WriteOperationResults<R> operationResults = null;

        LOGGER.fine("executeWriteOperationThrowing() LOCKING");
        lock.writeLock().lock();
        try
        {
            operationResults = executeOperationChain(throwingOperation, allOperationResults);    // throws UnsupportedEditException
        } catch (UnsupportedEditException ex)
        {
            // Catched only for logging purpose
            LOGGER.log(Level.FINE, "executeWriteOperationThrowing() ex={0}", ex);
            throw ex;
        } finally
        {
            LOGGER.fine("executeWriteOperationThrowing() UNLOCKING");
            lock.writeLock().unlock();
        }

        fireAllOperationEvents(allOperationResults);

        // Return the value from the operation
        R returnValue = (R) operationResults.returnValue();     // Can be null
        return returnValue;
    }


    public void preCheckChange(ClsChangeEvent event) throws UnsupportedEditException
    {
        if (songInternalUpdater == null)
        {
            // Internal updates disabled
            return;
        }
        songInternalUpdater.preCheckChange(event);
    }

    public void preCheckChange(SgsChangeEvent event) throws UnsupportedEditException
    {
        if (songInternalUpdater == null)
        {
            // Internal updates disabled
            return;
        }
        songInternalUpdater.preCheckChange(event);
    }

    public void preCheckChange(SongPropertyChangeEvent event) throws UnsupportedEditException
    {
        if (songInternalUpdater == null)
        {
            // Internal updates disabled
            return;
        }
        songInternalUpdater.preCheckChange(event);
    }

    // ===============================================================================================================
    // Private methods
    // ===============================================================================================================    

    /**
     * Execute op then its derived operations recursively.
     * <p>
     * Accumulate the operation results in cumulativeOpResults.
     *
     * @param cumulativeOpResults Cumulative list containing all operationResults
     * @param operation
     * @return The return value from executing operation.
     * @throws org.jjazz.chordleadsheet.api.UnsupportedEditException
     */
    private WriteOperationResults executeOperationChain(Operation operation, List<WriteOperationResults> cumulativeOpResults) throws UnsupportedEditException
    {
        // Execute operation
        WriteOperationResults opResults = execute(operation);        // throws UnsupportedEditException                
        cumulativeOpResults.add(opResults);

        if (songInternalUpdater != null)
        {
            // Execute derived operations recursively
            List<Operation> derivedOperations = songInternalUpdater.getDerivedOperations(opResults);
            for (var derivedOperation : derivedOperations)
            {
                executeOperationChain(derivedOperation, cumulativeOpResults);
            }
        }

        return opResults;
    }

    /**
     * Helper method to run a WriteOperation or a ThrowingWriteOperation.
     *
     * @param <T>
     * @param op
     * @return
     * @throws org.jjazz.chordleadsheet.api.UnsupportedEditException
     */
    private <T> WriteOperationResults<T> execute(Operation<T> op) throws UnsupportedEditException
    {
        Objects.requireNonNull(op);

        WriteOperationResults results = switch (op)
        {
            case WriteOperation wop ->
                ((WriteOperation<T>) wop).get();
            case ThrowingWriteOperation twop ->
                ((ThrowingWriteOperation<T>) twop).get();   // throws UnsupportedEditException
            default -> throw new IllegalArgumentException("op=" + op);
        };
        return results;
    }

    /**
     * Fire all the events from the operation results.
     *
     * @param opResults
     */
    private void fireAllOperationEvents(List<WriteOperationResults> opResults)
    {
        for (var opResult : opResults)
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

            } else if (opResult.sgsChangeEvent() instanceof SgsChangeEvent sce)
            {
                // The main change event
                ((SongStructureImpl) sce.getSource()).fireChangeEvent(sce);

                // Possible associated SongPart changes
                for (var propEvent : sce.getSongPartChanges())
                {
                    ((SongPartImpl) propEvent.getSource()).firePropertyChangeEvent(propEvent);
                }

            } else if (opResult.pChangeEvent() instanceof SongPropertyChangeEvent spce)
            {
                // SongPropertyChangeEvent is used as main event for Song and MidiMix
                switch (spce.getSource())
                {
                    case SongImpl sgImpl ->
                    {
                        sgImpl.firePropertyChangeEvent(spce);

                        // Possible associated changes
                        for (var propEvent : spce.getRelatedPropertyChanges())
                        {
                            sgImpl.firePropertyChangeEvent(propEvent);
                        }
                    }
                    case MidiMixImpl mmImpl ->
                    {
                        mmImpl.firePropertyChangeEvent(spce);

                        // Possible associated changes
                        for (var propEvent : spce.getRelatedPropertyChanges())
                        {
                            mmImpl.firePropertyChangeEvent(propEvent);
                        }
                    }
                    default ->
                    {
                        throw new IllegalArgumentException("spce=" + spce);
                    }
                }

            }
        }
    }
}

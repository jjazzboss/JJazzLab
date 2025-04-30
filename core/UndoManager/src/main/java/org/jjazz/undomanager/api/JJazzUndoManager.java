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
package org.jjazz.undomanager.api;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.ChangeListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;
import org.openide.*;
import org.openide.awt.UndoRedo;
import org.openide.util.ChangeSupport;

/**
 * This is a copy of UndoRedo.Manager with a few convenience methods added to work with CompoundEdits.
 */
public class JJazzUndoManager extends UndoManager implements UndoRedo
{

    /**
     * Listener for JJazzLab user-level undoable edits.
     */
    public interface UserEditListener
    {

        /**
         * Called when endCEdit(source, name) is called, or when the CEdit is undone or redone.
         *
         * @param src
         * @param source   The associated source object. Can be null.
         * @param editName The parameter of endCEdit(). Can't be null.
         */
        void userAction(JJazzUndoManager src, Object source, String editName);
    }

    /**
     * Listeners like Netbeans UndoAction/RedoAction linked to undo/redo buttons
     */
    private final ChangeSupport cs = new ChangeSupport(this);
    private final List<UserEditListener> userEditListeners = new ArrayList<>();
    /**
     * vector of Edits to run
     */
    private final LinkedList<UndoableEditEvent> runus = new LinkedList<>(); // for fix of #8692
    /**
     * Current CEdit.
     */
    private CEdit currentCEdit = null;
    /**
     * True is undo/redo in progress.
     */
    private boolean undoRedoInProgress = false;

    private boolean enabled = true;


    /**
     * for debug purposes.
     */
    private String name;
    private static final Logger LOGGER = Logger.getLogger(JJazzUndoManager.class.getSimpleName());

    public JJazzUndoManager()
    {
        name = "JJazzUndoManager";
    }

    /**
     *
     * @param name Used for debug purpose, returned by toString()
     */
    public JJazzUndoManager(String name)
    {
        if (name == null || name.isBlank())
        {
            throw new IllegalArgumentException("name=" + name);
        }
        this.name = name;
    }

    public boolean isUndoRedoInProgress()
    {
        return undoRedoInProgress;
    }

    public void addUserEditListener(UserEditListener l)
    {
        if (!userEditListeners.contains(l))
        {
            userEditListeners.add(l);
        }
    }

    public void removeUserEditListener(UserEditListener l)
    {
        userEditListeners.remove(l);
    }

    /**
     * Start a JJazzLab high-level compound edit with a null source object.
     *
     * @param editName Name of the edit
     */
    public void startCEdit(String editName)
    {
        startCEdit(null, editName);
    }

    /**
     * Start a JJazzLab high-level compound edit.
     *
     * @param source   The associated source object. Can be null.
     * @param editName Name of the edit. Can't be null.
     */
    public void startCEdit(Object source, String editName)
    {
        if (currentCEdit != null)
        {
            throw new IllegalStateException("currentCEdit=" + currentCEdit + " source=" + source + "  editName=" + editName);
        }
        LOGGER.log(Level.FINE, "startCEdit() source={0} editName={1} edits={2}", new Object[]
        {
            source, editName, edits
        });
        currentCEdit = new CEdit(source, editName);
        addEdit(currentCEdit);
    }

    /**
     * The name of the current compound edit.
     *
     * @return Can be null if not current edit
     * @see #startCEdit(java.lang.Object, java.lang.String)
     */
    public String getCurrentCEditName()
    {
        return currentCEdit == null ? null : currentCEdit.name;
    }

    /**
     *
     * End a JJazzLab high-level compound edit.
     * <p>
     * Notify the UserEditListeners.
     *
     * @return true if the compound edit was non empty.
     */
    public boolean endCEdit(String editName)
    {
        if (currentCEdit == null || !currentCEdit.getPresentationName().equals(editName))
        {
            throw new IllegalStateException("currentCEdit=" + currentCEdit + " editName=" + editName);
        }
        Object source = currentCEdit.getSource();

        LOGGER.log(Level.FINE, "endCEdit() -- source={0} n={1} edits={2} currentCEdit.edits={3}", new Object[]
        {
            source, editName, edits, currentCEdit
        });
        if (LOGGER.isLoggable(Level.FINE))
        {
            LOGGER.log(Level.FINE, "   currentCedit.dumpEdits()={0}", currentCEdit.dumpEdits());
        }

        currentCEdit.end();

        boolean res = true;
        if (currentCEdit.isEmpty())
        {
            // To avoid having undo/redo buttons enabled for nothing
            trimLastEdit();
            res = false;
        }

        // Force notification
        fireChange();

        // Ready for next compoundedit
        currentCEdit = null;

        LOGGER.log(Level.FINE, "endCEdit() POST edits={0}", edits);

        // Notify UserEditListeners
        fireUserEditListeners(source, editName);

        return res;
    }

    /**
     * Overridden for enabled state management.
     */
    @Override
    public synchronized boolean addEdit(UndoableEdit anEdit)
    {
        return enabled ? super.addEdit(anEdit) : false;
    }

    /**
     * Check if this instance is enabled.
     *
     * @return True by default
     * @see #setEnabled(boolean)
     */
    public boolean isEnabled()
    {
        return enabled;
    }

    /**
     * Enable or disable this instance.
     * <p>
     * If disabled no new edit can be added (either via addEdit() or undoableEditHappened()).
     * <p>
     * NOTE: Model must NOT be changed while the associated JJazzUndoManager is disabled.
     *
     * @param enabled
     * @see #isEnabled()
     * @throws IllegalStateException If an undo is in progress
     */
    public void setEnabled(boolean enabled)
    {
        if (isUndoRedoInProgress())
        {
            throw new IllegalStateException("An undo/redo is in progress");
        }
        this.enabled = enabled;
    }

    /**
     * Consume an undoable edit (if the instance is enabled).
     * <p>
     * Delegates to superclass and notifies listeners.
     *
     * @param ue the edit
     */
    @Override
    public void undoableEditHappened(final UndoableEditEvent ue)
    {
        if (!enabled)
        {
            return;
        }

        /*
       * Edits are posted to request processor and the deadlock in #8692 between undoredo and document that fires the
       * undoable edit should be avoided this way.
         */
        synchronized (runus)
        {
            runus.add(ue);
        }

        updateTask();

        if (currentCEdit == null)
        {
            LOGGER.log(Level.FINE, "undoableEditHappened() ue received but currentCEdit=null, ue.source={0} ue.edit={1}", new Object[]
            {
                ue.getSource(),
                ue.getEdit().getPresentationName()
            });
        } else if (LOGGER.isLoggable(Level.FINE))
        {
            LOGGER.log(Level.FINE, "undoableEditHappened() currentCEdit.edits={0}", currentCEdit.dumpEdits());
        }
    }

    /**
     * Discard all the existing edits from the undomanager.
     */
    @Override
    public void discardAllEdits()
    {
        LOGGER.fine("discardAllEdits() -- ");
        synchronized (runus)
        {
            runus.add(null);
        }

        updateTask();
    }

    @Override
    public void undo() throws CannotUndoException
    {
        undoRedoInProgress = true;
        try
        {
            super.undo();
        } catch (CannotUndoException ex)
        {
            LOGGER.severe("undo() ex="+ex.getMessage());
            undoRedoInProgress = false;
            throw ex;
        }
        updateTask();
        undoRedoInProgress = false;
    }

    @Override
    public void redo() throws CannotRedoException
    {
        undoRedoInProgress = true;
        try
        {
            super.redo();
        } catch (CannotUndoException ex)
        {
            LOGGER.severe("redo() ex="+ex.getMessage());
            undoRedoInProgress = false;
            throw ex;
        }
        updateTask();
        undoRedoInProgress = false;
    }

    @Override
    public void undoOrRedo() throws CannotRedoException, CannotUndoException
    {
        undoRedoInProgress = true;
        try
        {
            super.undoOrRedo();
        } catch (CannotUndoException ex)
        {
            LOGGER.severe("undoOrRedo() ex="+ex.getMessage());
            undoRedoInProgress = false;
            throw ex;
        }
        updateTask();
        undoRedoInProgress = false;
    }

    /**
     * Call die on next edit to be redone (if there is one).
     */
    public void killNextEditToBeRedone()
    {
//      UndoableEdit ue = super.editToBeRedone();
//      if (ue != null)
//      {
//         ue.die();
//      }
        LOGGER.fine("killNextEditToBeRedone() --");
        this.trimLastEdit();
        fireChange();
    }

    /**
     * Abort a compound edit started with startCEdit() in the middle: some SimpleEdits have been done but not all of them (endCEdit() was not called).
     * <p>
     * The method will :<br>
     * 0/ Possibly show a dialog to notify user with errMsg.<br>
     * 1/ Call endCEdit() on cEditName to terminate properly the compound edit. <br>
     * Because an exception might have occured some SimpleEdits may be missing in the CEdit (compared to normal).
     * <p>
     * If compound edit is not empty:<br>
     * 2/ Call undo()<br>
     * This will roll back the CEdit eEditName, ie undo each of the collected simple edits before the exception occured.
     * <p>
     * 3/ Remove CEdit cEditName from the undomanager, so that it can't be redone.
     *
     * @param cEditName The aborted edit name used with startCEdit()
     * @param errMsg    Error message to notify user. If null user is not notified.
     */
    public void abortCEdit(String cEditName, String errMsg)
    {
        if (errMsg != null)
        {
            // Notify user
            NotifyDescriptor d = new NotifyDescriptor.Message(errMsg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
        }


        if (endCEdit(cEditName))
        {
            // Only if there is something to undo
            undo();

            // Make redo not possible
            trimLastEdit();
        }

        fireChange();
    }

    private void updateTask()
    {
        for (;;)
        {
            UndoableEditEvent ue;

            synchronized (runus)
            {
                if (runus.isEmpty())
                {
                    break;
                }

                ue = runus.removeFirst();
            }

            if (ue == null)
            {
                super.discardAllEdits();
            } else
            {
                super.undoableEditHappened(ue);
            }
        }
        fireChange();
    }

    /**
     * Notify listeners of a state change.
     */
    public void fireChange()
    {
        cs.fireChange();
    }

    /*
    * Attaches change listener to the this object. The listener is notified everytime the undo/redo ability of this
    * object changes.
     */
    //#32313 - synchronization of this method was removed
    @Override
    public void addChangeListener(ChangeListener l)
    {
        cs.addChangeListener(l);
    }

    /*
    * Removes the listener
     */
    @Override
    public void removeChangeListener(ChangeListener l)
    {
        cs.removeChangeListener(l);
    }

    @Override
    public String getUndoPresentationName()
    {
        return this.canUndo() ? super.getUndoPresentationName() : ""; // NOI18N
    }

    @Override
    public String getRedoPresentationName()
    {
        return this.canRedo() ? super.getRedoPresentationName() : ""; // NOI18N
    }

    // ========================================================================================================
    // Private methods
    // ========================================================================================================

    /**
     * Remove the last edit from the UndoManager.
     * <p>
     * Must be used with care, e.g. only when you know that the last edit is an empty CompoundEdit.
     */
    private void trimLastEdit()
    {
        LOGGER.fine("trimLastEdit() --");
        trimEdits(edits.size() - 1, edits.size() - 1);
    }

    private void fireUserEditListeners(Object source, String actionName)
    {
        userEditListeners.forEach(l -> l.userAction(this, source, actionName));
    }

    @Override
    public String toString()
    {
        return name;
    }

    //========================================================================================
    // Inner classes
    //========================================================================================

    /**
     * A CompoundEdit with convenience operations to work with the JazzUndoManager.
     */
    private class CEdit extends CompoundEdit
    {

        private Object source;
        private String name;

        public CEdit(Object src, String n)
        {
            if (n == null)
            {
                throw new IllegalArgumentException("n=" + n);
            }
            source = src;
            name = n;
        }

        public boolean isEmpty()
        {
            return edits.isEmpty();
        }

        /**
         * The associated source object.
         *
         * @return Can be null.
         */
        public Object getSource()
        {
            return source;
        }

        @Override
        public String getPresentationName()
        {
            return name;
        }

        // @Override
        @Override
        public String getUndoPresentationName()
        {
            // return CTL_Undo() + " " + getPresentationName();
            return getPresentationName();
        }

        // @Override
        @Override
        public String getRedoPresentationName()
        {
            // return CTL_Redo() + " " + getPresentationName();
            return getPresentationName();
        }

        @Override
        public boolean isSignificant()
        {
            return true;
        }

        /**
         * Overridden to notify UserEditListeners.
         *
         * @throws CannotUndoException
         */
        @Override
        public void undo() throws CannotUndoException
        {
            super.undo();
            fireUserEditListeners(source, name);
        }

        /**
         * Overridden to notify UserEditListeners.
         *
         * @throws CannotRedoException
         */
        @Override
        public void redo() throws CannotRedoException
        {
            super.redo();
            fireUserEditListeners(source, name);
        }

        public String dumpEdits()
        {
            return String.valueOf(edits);
        }

        @Override
        public String toString()
        {
            return getPresentationName();
        }
    }
}

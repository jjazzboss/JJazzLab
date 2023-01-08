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
package org.jjazz.pianoroll.api;

import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.swing.event.ChangeListener;
import org.openide.util.ChangeSupport;
import org.openide.util.Lookup;
import org.openide.util.LookupListener;
import org.openide.util.WeakListeners;

/**
 * A helper class to notify registered ChangeListeners when selection changes in a lookup.
 * <p>
 */
public class NotesSelectionListener
{

    private Lookup context;
    private Lookup.Result<NoteView> noteViewLkpResult;
    private LookupListener noteViewLkpListener;
    private NotesSelection selection;
    private static final Map<Lookup, NotesSelectionListener> MAP_INSTANCES = new HashMap<>();
    private final ChangeSupport cs = new ChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(NotesSelectionListener.class.getSimpleName());

    /**
     * Create a listener.
     * <p>
     * If context == Utilities.actionsGlobalContext() listen to the lookup of the active PianoRollEditorTopComponent.
     * <p>
     * Instances are shared for a given context.
     *
     * @param context
     * @return
     */
    static public NotesSelectionListener getInstance(Lookup context)
    {
        Preconditions.checkNotNull(context);
        NotesSelectionListener res;
        synchronized (NotesSelectionListener.class)
        {
            res = MAP_INSTANCES.get(context);
            if (res == null)
            {
                res = new NotesSelectionListener(context);
                MAP_INSTANCES.put(context, res);
            }
        }
        return res;
    }


    public NotesSelectionListener(Lookup context)
    {
        if (context == null)
        {
            throw new IllegalArgumentException("context=" + context);   //NOI18N
        }
        this.context = context;

        // For WeakReferences to work, we need to keep a strong reference on the listeners (see WeakListeners java doc).
        noteViewLkpListener = le -> notePresenceChanged();
        noteViewLkpResult = context.lookupResult(NoteView.class);
        // Need to use WeakListeners so than action can be GC'ed
        // See http://forums.netbeans.org/viewtopic.php?t=35921
        noteViewLkpResult.addLookupListener(WeakListeners.create(LookupListener.class, noteViewLkpListener, noteViewLkpResult));

    }

    /**
     * @return the Lookup context
     */
    public final Lookup getContext()
    {
        return context;
    }

    /**
     * @return The last selection.
     */
    public final NotesSelection getSelection()
    {
        return selection;
    }

    /**
     * Be notified when selection changes in the context.
     *
     * @param l
     */
    public void addListener(ChangeListener l)
    {
        cs.addChangeListener(l);
    }

    public void removeListener(ChangeListener l)
    {
        cs.removeChangeListener(l);
    }


    //----------------------------------------------------------------------------------------
    // Private functions
    //----------------------------------------------------------------------------------------    
    /**
     * Called when NoteEvent instances presence changed in the lookup.
     */
    @SuppressWarnings(
            {
                "rawtypes",
                "unchecked"
            })
    private void notePresenceChanged()
    {
        selection = new NotesSelection(context);
        cs.fireChange();
    }

}

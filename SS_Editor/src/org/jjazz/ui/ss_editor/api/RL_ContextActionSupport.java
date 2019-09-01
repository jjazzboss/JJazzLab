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
package org.jjazz.ui.ss_editor.api;

import java.util.ArrayList;
import java.util.logging.Logger;
import org.jjazz.songstructure.api.SongPartParameter;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;
import org.jjazz.songstructure.api.SongPart;

/**
 * A helper class to write RL_Editor context aware actions.
 * <p>
 * Listen to RhyhmPart or SongPartParameter presence in the lookup. Fire the corresponding events to listeners.
 */
public class RL_ContextActionSupport
{

    static private RL_ContextActionSupport INSTANCE;
    private Lookup context;
    private Lookup.Result<SongPartParameter> sptpLkpResult;
    private LookupListener sptpLkpListener;
    private Lookup.Result<SongPart> sptLkpResult;
    private LookupListener sptLkpListener;
    private RL_SelectionUtilities selection;
    private ArrayList<RL_ContextActionListener> listeners;
    private static final Logger LOGGER = Logger.getLogger(RL_ContextActionSupport.class.getSimpleName());

    /**
     * If context == Utilities.actionsGlobalContext() return a shared instance. Otherwise return a new specific object.
     *
     * @param context
     * @return
     */
    static public RL_ContextActionSupport getInstance(Lookup context)
    {
        RL_ContextActionSupport o;
        synchronized (RL_ContextActionSupport.class)
        {
            if (context == Utilities.actionsGlobalContext())
            {
                if (INSTANCE == null)
                {
                    INSTANCE = new RL_ContextActionSupport(context);
                }
                o = INSTANCE;
            } else
            {
                o = new RL_ContextActionSupport(context);
            }
        }
        return o;
    }

    private RL_ContextActionSupport(Lookup context)
    {
        if (context == null)
        {
            throw new IllegalArgumentException("context=" + context);
        }
        this.context = context;

        listeners = new ArrayList<>();

        // For WeakReferences to work, we need to keep a strong reference on the listeners (see WeakListeners java doc).
        sptLkpListener = new LookupListener()
        {
            @Override
            public void resultChanged(LookupEvent le)
            {
                sptPresenceChanged();
            }
        };
        sptpLkpListener = new LookupListener()
        {
            @Override
            public void resultChanged(LookupEvent le)
            {
                sptpPresenceChanged();
            }
        };

        sptLkpResult = context.lookupResult(SongPart.class);
        // Need to use WeakListeners so than action can be GC'ed
        // See http://forums.netbeans.org/viewtopic.php?t=35921
        sptLkpResult.addLookupListener(WeakListeners.create(LookupListener.class, sptLkpListener, sptLkpResult));

        sptpLkpResult = context.lookupResult(SongPartParameter.class);
        sptpLkpResult.addLookupListener(WeakListeners.create(LookupListener.class, sptpLkpListener, sptpLkpResult));

        // Initialize the selection
        if (context.lookup(SongPart.class) != null)
        {
            sptPresenceChanged();
        } else
        {
            sptpPresenceChanged();
        }
    }

    /**
     * @return the Lookup context
     */
    public final Lookup getContext()
    {
        return context;
    }

    /**
     * @return The latest selection.
     */
    public final RL_SelectionUtilities getSelection()
    {
        return selection;
    }

    public void addListener(RL_ContextActionListener l)
    {
        if (!listeners.contains(l))
        {
            listeners.add(l);
        }
    }

    public void removeListener(RL_ContextActionListener l)
    {
        listeners.remove(l);
    }

    //----------------------------------------------------------------------------------------
    // Private functions
    //----------------------------------------------------------------------------------------    
    /**
     * Called when items presence changed in the lookup. Delegates to selectionChanged(Selection)
     */
    @SuppressWarnings(
            {
                "rawtypes",
                "unchecked"
            })
    private void sptpPresenceChanged()
    {
        selection = new RL_SelectionUtilities(context);
        fireSelectionChanged(selection);
    }

    /**
     * Called when SelectedBar presence changed in the lookup. Delegates to selectionChanged(Selection)
     */
    @SuppressWarnings(
            {
                "rawtypes",
                "unchecked"
            })
    private void sptPresenceChanged()
    {
        selection = new RL_SelectionUtilities(context);
        fireSelectionChanged(selection);
    }

    private void fireSelectionChanged(RL_SelectionUtilities selection)
    {
        for (RL_ContextActionListener l : listeners)
        {
            l.selectionChange(selection);
        }
    }
}

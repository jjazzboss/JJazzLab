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
package org.jjazz.ss_editor.api;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.logging.Logger;
import org.jjazz.songstructure.api.SongPartParameter;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.WeakListeners;
import org.jjazz.songstructure.api.SongPart;

/**
 * A helper class to write SS_Editor context aware actions.
 * <p>
 * Listen to changes in the lookup context:<br>
 * - RhyhmPart or SongPartParameter presence changes are notified to the registered SS_ContextActionListeners.<br>
 * <p>
 * SS_ContextActionSupport instances are cached per lookup context. Only weak listeners are used: declaratively registered actions might be transient actions
 * (e.g. ContextAwareAction instances).
 */
public class SS_ContextActionSupport
{

    private final Lookup context;
    private final Lookup.Result<SongPartParameter> sptpLkpResult;
    private final LookupListener sptpLkpListener;
    private final Lookup.Result<SongPart> sptLkpResult;
    private final LookupListener sptLkpListener;
    private SS_Selection selection;
    private final List<WeakReference<SS_ContextActionListener>> selectionListeners;
    private static WeakHashMap<Lookup, SS_ContextActionSupport> MapContextInstance;
    private static final Logger LOGGER = Logger.getLogger(SS_ContextActionSupport.class.getSimpleName());

    /**
     * Get the instance associated to the specified context.
     *
     * @param context
     * @return
     */
    static public SS_ContextActionSupport getInstance(Lookup context)
    {
        Objects.requireNonNull(context);

        SS_ContextActionSupport res;
        synchronized (SS_ContextActionSupport.class)
        {
            if (MapContextInstance == null)
            {
                MapContextInstance = new WeakHashMap<>();
            }
            res = MapContextInstance.get(context);
            if (res == null)
            {
                res = new SS_ContextActionSupport(context);
                MapContextInstance.put(context, res);
            }
        }
        return res;
    }

    private SS_ContextActionSupport(Lookup context)
    {
        selectionListeners = new ArrayList<>();
        this.context = context;


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
    public final SS_Selection getSelection()
    {
        return selection;
    }

    public void addWeakSelectionListener(SS_ContextActionListener listener)
    {
        if (!getTargetListeners(selectionListeners).contains(listener))
        {
            selectionListeners.add(new WeakReference(listener));
        }
    }

    public void removeWeakListener(SS_ContextActionListener listener)
    {
        selectionListeners.removeIf(wr -> wr.get() == listener);
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
        selection = new SS_Selection(context);
        fireSelectionChanged(SongPartParameter.class, selection);
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
        selection = new SS_Selection(context);
        fireSelectionChanged(SongPart.class, selection);
    }

    /**
     *
     * @param itemsClass The class of the items at the origin of the lookup change. Not used for now.
     * @param selection
     */
    private void fireSelectionChanged(Class itemsClass, SS_Selection selection)
    {
        for (SS_ContextActionListener l : getTargetListeners(selectionListeners))
        {
            l.selectionChange(selection);
        }
    }

    /**
     * Get the listeners not yet GC'ed.
     *
     * @param <T>
     * @param wrs
     * @return
     */
    private <T> List<T> getTargetListeners(List<WeakReference<T>> wrs)
    {
        List<T> res = new ArrayList<>();
        for (var it = wrs.iterator(); it.hasNext();)
        {
            T o = it.next().get();
            if (o == null)
            {
                it.remove();
            } else
            {
                res.add(o);
            }
        }
        return res;
    }
}

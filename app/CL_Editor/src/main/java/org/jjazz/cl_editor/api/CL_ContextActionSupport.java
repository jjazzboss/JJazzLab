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
package org.jjazz.cl_editor.api;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.ClsChangeListener;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.event.ClsChangeEvent;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.WeakListeners;

/**
 * A helper class to write CL_Editor context aware actions.
 * <p>
 * Listen to changes in the lookup context:<br>
 * - ChordLeadSheetItem and SelectedBar presence changes are notified to the registered CL_ContextActionListeners.<br>
 * - If a ChordLeadSheet is present, listen to its changes and forward the ClsChangeEvents to the registered ClsChangeListeners.<br>
 * <p>
 * CL_ContextActionSupport instances are cached per lookup context. Only weak listeners are used: declaratively registered actions might be transient actions
 * (e.g. ContextAwareAction instances).
 */
public class CL_ContextActionSupport implements ClsChangeListener
{

    private final Lookup context;
    private final Lookup.Result<SelectedBar> selectedBarLkpResult;
    private final LookupListener barLkpListener;
    @SuppressWarnings("rawtypes")
    private final Lookup.Result<SelectedCLI> itemLkpResult;
    private final LookupListener itemLkpListener;
    private final Lookup.Result<ChordLeadSheet> clsLkpResult;
    private final LookupListener clsLkpListener;
    private ChordLeadSheet model;
    private CL_Selection selection;
    private final List<WeakReference<CL_ContextActionListener>> selectionListeners;
    private final List<WeakReference<ClsChangeListener>> clsListeners;
    private static WeakHashMap<Lookup, CL_ContextActionSupport> MapContextInstance;
    private static final Logger LOGGER = Logger.getLogger(CL_ContextActionSupport.class.getSimpleName());

    /**
     * Get the instance associated to the specified context.
     *
     * @param context
     * @return
     */
    static public CL_ContextActionSupport getInstance(Lookup context)
    {
        Objects.requireNonNull(context);

        CL_ContextActionSupport res;
        synchronized (CL_ContextActionSupport.class)
        {
            if (MapContextInstance == null)
            {
                MapContextInstance = new WeakHashMap<>();
            }
            res = MapContextInstance.get(context);
            if (res == null)
            {
                res = new CL_ContextActionSupport(context);
                MapContextInstance.put(context, res);
            }
        }
        return res;
    }

    private CL_ContextActionSupport(Lookup context)
    {
        this.context = context;

        selectionListeners = new ArrayList<>();
        clsListeners = new ArrayList<>();

        // For WeakReferences to work, we need to keep a strong reference on the listeners (see WeakListeners java doc).
        barLkpListener = new LookupListener()
        {
            @Override
            public void resultChanged(LookupEvent le)
            {
                lookupContentChanged(SelectedBar.class);
            }
        };
        itemLkpListener = new LookupListener()
        {
            @Override
            public void resultChanged(LookupEvent le)
            {
                lookupContentChanged(SelectedCLI.class);
            }
        };
        clsLkpListener = new LookupListener()
        {
            @Override
            public void resultChanged(LookupEvent le)
            {
                clsPresenceChanged();
            }
        };

        // listen to cls presence to update the model. Need to be the first listener so that model is set first
        // before selection changes.
        clsLkpResult = context.lookupResult(ChordLeadSheet.class);
        clsLkpResult.addLookupListener(WeakListeners.create(LookupListener.class, clsLkpListener, clsLkpResult));
        clsPresenceChanged(); // Need to be called to set up the model if already present in the lookup            

        selectedBarLkpResult = context.lookupResult(SelectedBar.class);
        // Need to use WeakListeners so than action can be GC'ed
        // See http://forums.netbeans.org/viewtopic.php?t=35921
        selectedBarLkpResult.addLookupListener(WeakListeners.create(LookupListener.class, barLkpListener, selectedBarLkpResult));

        itemLkpResult = context.lookupResult(SelectedCLI.class);
        itemLkpResult.addLookupListener(WeakListeners.create(LookupListener.class, itemLkpListener, itemLkpResult));

        // Initialize the selection
        if (context.lookup(SelectedBar.class) != null)
        {
            lookupContentChanged(SelectedBar.class);
        } else
        {
            lookupContentChanged(SelectedCLI.class);
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
    public final CL_Selection getSelection()
    {
        return selection;
    }

    /**
     *
     * @return The ChordLeadSheet currently present in the lookup context. Can be null.
     */
    public final ChordLeadSheet getActiveChordLeadSheet()
    {
        return model;
    }

    /**
     * Add a weak reference to the specified listener.
     * <p>
     * Listener will be notified of selection changes (bars or chord leadsheet items), unless listener is garbage-collected.
     *
     * @param listener
     */
    public void addWeakSelectionListener(CL_ContextActionListener listener)
    {
        if (!getTargetListeners(selectionListeners).contains(listener))
        {
            selectionListeners.add(new WeakReference(listener));
        }
    }

    /**
     * Remove the specified listener.
     * <p>
     * @param listener
     */
    public void removeWeakSelectionListener(CL_ContextActionListener listener)
    {
        selectionListeners.removeIf(wr -> wr.get() == listener);
    }

    /**
     * Add a weak reference to the specified listener.
     * <p>
     * Listener will be notified of ClsChangeEvents from the active ChordLeadSheet, i.e the one present in the lookup context (unless listener is
     * garbage-collected).
     *
     * @param listener
     */
    public void addWeakActiveClsChangeListener(ClsChangeListener listener)
    {
        clsListeners.add(new WeakReference(listener));
    }

    /**
     * Remove the specified listener.
     * <p>
     * @param listener
     */
    public void removeWeakActiveClsChangeListener(ClsChangeListener listener)
    {
        clsListeners.removeIf(wr -> wr.get() == listener);
    }


    // ============================================================================================= 
    // ClsChangeListener implementation
    // =============================================================================================      
    @Override
    public void chordLeadSheetChanged(ClsChangeEvent event) throws UnsupportedEditException
    {
        // Forward the event
        for (ClsChangeListener l : getTargetListeners(clsListeners))
        {
            l.chordLeadSheetChanged(event);
        }
    }

    //----------------------------------------------------------------------------------------
    // Private methods
    //----------------------------------------------------------------------------------------    
    /**
     * Called when items presence changed in the lookup.
     * <p>
     *
     * @param itemsClass The class of the items at the origin of the change.
     */
    private void lookupContentChanged(Class itemsClass)
    {
        selection = new CL_Selection(context);
        LOGGER.log(Level.FINE, "lookupContentChanged() model={0} selection.getSelectedItems()={1}", new Object[]
        {
            model,
            selection.getSelectedItems()
        });
        fireSelectionChanged(itemsClass, selection);
    }

    /**
     * Called when a ChordLeadSheet appeared or disappeared in the lookup.
     */
    private void clsPresenceChanged()
    {
        if (model != null)
        {
            model.removeClsChangeListener(this);
        }
        model = context.lookup(ChordLeadSheet.class);
        if (model != null)
        {
            model.addClsChangeListener(this);
        }
    }

    /**
     *
     * @param itemsClass The class of the items at the origin of the lookup change. Not used for now.
     * @param selection
     */
    private void fireSelectionChanged(Class itemsClass, CL_Selection selection)
    {
        for (CL_ContextActionListener l : getTargetListeners(selectionListeners))
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

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
package org.jjazz.ui.cl_editor.api;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.ClsChangeListener;
import org.jjazz.leadsheet.chordleadsheet.api.event.ClsChangeEvent;
import org.jjazz.leadsheet.chordleadsheet.api.event.SizeChangedEvent;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordLeadSheetItem;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;

/**
 * A helper class to write CL_Editor context aware actions.
 * <p>
 * Listen to ChordLeadSheetItem, SelectedBar, ChordLeadSheet presence in the lookup. Listen to the "present" ChordLeadSheet
 * property changes. Fire the corresponding events to listeners.
 */
public class CL_ContextActionSupport implements ClsChangeListener
{

    static private CL_ContextActionSupport INSTANCE;
    private Lookup context;
    private Lookup.Result<SelectedBar> selectedBarLkpResult;
    private LookupListener barLkpListener;
    @SuppressWarnings("rawtypes")
    private Lookup.Result<ChordLeadSheetItem> itemLkpResult;
    private LookupListener itemLkpListener;
    private Lookup.Result<ChordLeadSheet> clsLkpResult;
    private LookupListener clsLkpListener;
    private ChordLeadSheet model;
    private CL_SelectionUtilities selection;
    private ArrayList<CL_ContextActionListener> listeners;
    private static final Logger LOGGER = Logger.getLogger(CL_ContextActionSupport.class.getSimpleName());

    /**
     * If context == Utilities.actionsGlobalContext() return a shared instance. Otherwise return a new specific object.
     *
     * @param context
     * @return
     */
    static public CL_ContextActionSupport getInstance(Lookup context)
    {
        CL_ContextActionSupport o;
        synchronized (CL_ContextActionSupport.class)
        {
            if (context == Utilities.actionsGlobalContext())
            {
                if (INSTANCE == null)
                {
                    INSTANCE = new CL_ContextActionSupport(context);
                }
                o = INSTANCE;
            } else
            {
                o = new CL_ContextActionSupport(context);
            }
        }
        return o;
    }

    private CL_ContextActionSupport(Lookup context)
    {
        if (context == null)
        {
            throw new IllegalArgumentException("context=" + context);
        }
        this.context = context;

        listeners = new ArrayList<>();

        // For WeakReferences to work, we need to keep a strong reference on the listeners (see WeakListeners java doc).
        barLkpListener = new LookupListener()
        {
            @Override
            public void resultChanged(LookupEvent le)
            {
                barPresenceChanged();
            }
        };
        itemLkpListener = new LookupListener()
        {
            @Override
            public void resultChanged(LookupEvent le)
            {
                itemPresenceChanged();
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

        itemLkpResult = context.lookupResult(ChordLeadSheetItem.class);
        itemLkpResult.addLookupListener(WeakListeners.create(LookupListener.class, itemLkpListener, itemLkpResult));

        // Initialize the selection
        if (context.lookup(SelectedBar.class) != null)
        {
            barPresenceChanged();
        } else
        {
            itemPresenceChanged();
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
    public final CL_SelectionUtilities getSelection()
    {
        return selection;
    }

    public void addListener(CL_ContextActionListener l)
    {
        if (!listeners.contains(l))
        {
            listeners.add(l);
        }
    }

    public void removeListener(CL_ContextActionListener l)
    {
        listeners.remove(l);
    }

    //----------------------------------------------------------------------------------------
    // ClsChangeListener interface
    //----------------------------------------------------------------------------------------      
    @Override
    public void chordLeadSheetChanged(ClsChangeEvent event)
    {
        // Just forward the event
        if (event instanceof SizeChangedEvent)
        {
            SizeChangedEvent e = (SizeChangedEvent) event;
            for (CL_ContextActionListener l : listeners)
            {
                l.sizeChanged(e.getOldSize(), e.getNewSize());
            }
        }
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
    private void itemPresenceChanged()
    {
        selection = new CL_SelectionUtilities(context);
        LOGGER.log(Level.FINE, "itemPresenceChanged() model=" + model + " selection.getSelectedItems()=" + selection.getSelectedItems());
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
    private void barPresenceChanged()
    {
        selection = new CL_SelectionUtilities(context);
        LOGGER.log(Level.FINE, "barPresenceChanged() model=" + model + " selection.getSelectedBars()=" + selection.getSelectedBars());
        fireSelectionChanged(selection);
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

    private void fireSelectionChanged(CL_SelectionUtilities selection)
    {
        for (CL_ContextActionListener l : listeners)
        {
            l.selectionChange(selection);
        }
    }
}

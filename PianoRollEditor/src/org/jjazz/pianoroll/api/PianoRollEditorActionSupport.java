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

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.ChangeListener;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.leadsheet.chordleadsheet.api.event.ClsChangeEvent;
import org.jjazz.leadsheet.chordleadsheet.api.event.SizeChangedEvent;
import org.jjazz.phrase.api.NoteEvent;
import org.openide.util.ChangeSupport;
import org.openide.util.Lookup;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;

/**
 * A helper class to listen to selection changes of the active PianoRollEditorTopComponent.
 * <p>
 */
public class PianoRollEditorActionSupport
{

    static private PianoRollEditorActionSupport INSTANCE;
    private Lookup context;
    private Lookup.Result<NoteEvent> selectedNoteLkpResult;
    private LookupListener noteLkpListener;
    private final ChangeSupport cs = new ChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(PianoRollEditorActionSupport.class.getSimpleName());

    /**
     * If context == Utilities.actionsGlobalContext() return a shared instance, otherwise return a new specific object.
     *
     * @param context
     * @return
     */
    static public PianoRollEditorActionSupport getInstance(Lookup context)
    {
        PianoRollEditorActionSupport o;
        synchronized (PianoRollEditorActionSupport.class)
        {
            if (context == Utilities.actionsGlobalContext())
            {
                if (INSTANCE == null)
                {
                    INSTANCE = new PianoRollEditorActionSupport(context);
                }
                o = INSTANCE;
            } else
            {
                o = new PianoRollEditorActionSupport(context);
            }
        }
        return o;
    }

    public PianoRollEditorActionSupport(Lookup context)
    {
        if (context == null)
        {
            throw new IllegalArgumentException("context=" + context);   //NOI18N
        }
        this.context = context;

        // For WeakReferences to work, we need to keep a strong reference on the listeners (see WeakListeners java doc).
        noteLkpListener = le -> notePresenceChanged();

        selectedNoteLkpResult = context.lookupResult(NoteEvent.class);
        // Need to use WeakListeners so than action can be GC'ed
        // See http://forums.netbeans.org/viewtopic.php?t=35921
        selectedNoteLkpResult.addLookupListener(WeakListeners.create(LookupListener.class, noteLkpListener, selectedNoteLkpResult));
  
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
    public final NotesSelection getSelection()
    {
        return selection;
    }

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
     * Called when SelectedBar presence changed in the lookup.
     */
    @SuppressWarnings(
            {
                "rawtypes",
                "unchecked"
            })
    private void notePresenceChanged()
    {
        cs.fireChange();
    }

}

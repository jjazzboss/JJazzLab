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
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.ChangeListener;
import org.openide.util.ChangeSupport;
import org.openide.util.Lookup;
import org.openide.util.LookupListener;
import org.openide.util.WeakListeners;

/**
 * A helper class to notify registered ChangeListeners when selection changes in the editor's lookup.
 * <p>
 */
public class NotesSelectionListener
{
    private final PianoRollEditor editor;
    private final Lookup context;
    private final Lookup.Result<NoteView> noteViewLkpResult;
    private final LookupListener noteViewLkpListener;
    private NotesSelection selection;
    private List<NoteView> lastSelectedNoteViews;
    private NoteView lastNoteViewAddedToSelection;
    private static final Map<PianoRollEditor, NotesSelectionListener> MAP_INSTANCES = new HashMap<>();
    private final ChangeSupport cs = new ChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(NotesSelectionListener.class.getSimpleName());

    /**
     * Create a note selection listener for the specified editor.
     * <p>
     * Instances are shared per editor.
     *
     * @param editor
     * @return
     */
    static public NotesSelectionListener getInstance(PianoRollEditor editor)
    {
        Preconditions.checkNotNull(editor);
        NotesSelectionListener res;
        
        synchronized (NotesSelectionListener.class)
        {
            res = MAP_INSTANCES.get(editor);
            if (res == null)
            {
                res = new NotesSelectionListener(editor);
                MAP_INSTANCES.put(editor, res);

                // Don't keep a reference if editor is dead
                editor.addPropertyChangeListener(PianoRollEditor.PROP_EDITOR_ALIVE, e -> MAP_INSTANCES.remove(editor));
            }
        }
        return res;
    }


    private NotesSelectionListener(PianoRollEditor editor)
    {
        this.editor = editor;
        this.context = editor.getLookup();

        // For WeakReferences to work, we need to keep a strong reference on the listeners (see WeakListeners java doc).
        noteViewLkpListener = le -> notePresenceChanged();
        noteViewLkpResult = context.lookupResult(NoteView.class);
        // Need to use WeakListeners so than action can be GC'ed
        // See http://forums.netbeans.org/viewtopic.php?t=35921
        noteViewLkpResult.addLookupListener(WeakListeners.create(LookupListener.class, noteViewLkpListener, noteViewLkpResult));

    }

    public PianoRollEditor getEditor()
    {
        return editor;
    }

    /**
     * Get the last selection.
     * <p>
     * Registered ChangeListeners, once notified, can get the new selection via this method.
     *
     * @return The last selection.
     */
    public final NotesSelection getSelection()
    {
        return selection;
    }

    /**
     * Check if the last selection is empty.
     *
     * @return
     */
    public boolean isEmpty()
    {
        return selection == null ? true : selection.isEmpty();
    }

    /**
     * Get the last NoteView added to selection.
     * <p>
     * Registered ChangeListeners, once notified, can get the last new selected NoteView via this method.
     *
     * @return Null if last change was not to add a single NoteView to the selection.
     */
    public final NoteView getLastNoteViewAddedToSelection()
    {
        return lastNoteViewAddedToSelection;
    }

    /**
     * Be notified when selection changes in the editor.
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
     * Called when NoteView instances presence changed in the lookup.
     */
    private void notePresenceChanged()
    {
        // update selection global var
        selection = new NotesSelection(context);


        // update lastNoteViewAddedToSelection global var
        var selectedNvs = selection.getNoteViews();
        lastNoteViewAddedToSelection = null;
        if (lastSelectedNoteViews == null || lastSelectedNoteViews.isEmpty())
        {
            if (selectedNvs.size() == 1)
            {
                lastNoteViewAddedToSelection = selectedNvs.get(0);
            }
        } else
        {
            int size = selectedNvs.size();
            int lastSize = lastSelectedNoteViews.size();
            assert size != lastSize : "selectedNvs=" + selectedNvs + "lastSelectedNoteViews=" + lastSelectedNoteViews;
            if (size == lastSize + 1)
            {
                // Search for the new guy
                lastNoteViewAddedToSelection = selectedNvs.stream()
                        .filter(nv -> !lastSelectedNoteViews.contains(nv))
                        .findAny()
                        .orElse(null);
            }
        }

        LOGGER.log(Level.FINE, "notePresenceChanged() selectedNvs={0}  lastNoteViewAddedToSelection={1}", new Object[]
        {
            selectedNvs, lastNoteViewAddedToSelection
        });

        cs.fireChange();


        lastSelectedNoteViews = selectedNvs;
    }

}

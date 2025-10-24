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
package org.jjazz.chordinspector;

import com.google.common.base.Preconditions;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.harmony.api.Position;
import org.jjazz.cl_editor.api.CL_ContextActionListener;
import org.jjazz.cl_editor.api.CL_ContextActionSupport;
import org.jjazz.cl_editor.api.CL_Selection;
import org.jjazz.musiccontrol.api.PlaybackSettings;
import org.openide.util.Utilities;

/**
 * Listen to the current selected ChordSymbol to update the ChordInspectorPanel.
 */
public class ChordListener implements CL_ContextActionListener, PropertyChangeListener
{

    private final CL_ContextActionSupport cap;
    private CLI_ChordSymbol chordSymbol;
    private int transposition;
    private final ChordInspectorPanel editor;
    private final PlaybackSettings playbackSettings;
    private static final Logger LOGGER = Logger.getLogger(ChordListener.class.getSimpleName());

    public ChordListener(ChordInspectorPanel editor)
    {
        Preconditions.checkNotNull(editor);
        this.editor = editor;

        // Listen to selection changes in the current leadsheet editor
        cap = CL_ContextActionSupport.getInstance(Utilities.actionsGlobalContext());
        cap.addWeakSelectionListener(this);
        selectionChange(cap.getSelection());

        // Listen to display transposition changes
        playbackSettings = PlaybackSettings.getInstance();
        playbackSettings.addPropertyChangeListener(PlaybackSettings.PROP_CHORD_SYMBOLS_DISPLAY_TRANSPOSITION, this);
        setTransposition(playbackSettings.getChordSymbolsDisplayTransposition());
    }

    public void cleanup()
    {
        cap.removeWeakSelectionListener(this);

        playbackSettings.removePropertyChangeListener(PlaybackSettings.PROP_CHORD_SYMBOLS_DISPLAY_TRANSPOSITION, this);
        if (chordSymbol != null)
        {
            chordSymbol.removePropertyChangeListener(this);
        }
    }

    private void setTransposition(int newTransposition)
    {
        transposition = newTransposition;
        if (chordSymbol != null)
        {
            updateEditorModel();
        }
    }

    private void updateEditorModel()
    {
        ExtChordSymbol transposedChord = chordSymbol == null ? null : chordSymbol.getData().getTransposedChordSymbol(transposition, null);
        editor.setModel(transposedChord);
    }

    /**
     * Update editor model only when a new non-null chordsymbol is identified.
     * <p>
     * This lets user activates other TopComponents (including this ChordInspector when switching to a different ChordViewer) while still viewing the
     * last selected chord.
     *
     * @param selection
     */
    @Override
    public final void selectionChange(CL_Selection selection)
    {
        CLI_ChordSymbol newSelectedChordSymbol = null;

        var chordSymbols = selection.getSelectedChordSymbols();
        if (!chordSymbols.isEmpty())
        {
            newSelectedChordSymbol = chordSymbols.get(0);

        } else if (selection.isBarSelectedWithinCls())
        {
            // Find the last chord valid for this bar
            var cls = selection.getChordLeadSheet();
          
            newSelectedChordSymbol = cls.getLastItemBefore(new Position(selection.getMinBarIndex() + 1),    // Might be null if user temporarily removes all chord symbols
                false, CLI_ChordSymbol.class, cli -> true);
        } else
        {
            // Not a valid selection, do nothing
            // Note: an empty selection is received when switching from a CL_Editor TopComponent to a different TopComponent
        }

        LOGGER.log(Level.FINE, "newSelectedChordSymbol={0}", newSelectedChordSymbol);


        // Replace current chord symbol
        if (chordSymbol != null)
        {
            chordSymbol.removePropertyChangeListener(this);
        }

        chordSymbol = newSelectedChordSymbol;

        if (chordSymbol != null)
        {
            chordSymbol.addPropertyChangeListener(this);
            updateEditorModel();
        }
    }

    /**
     * Call from either {@link CLI_ChordSymbol} or the {@link PlaybackSettings}. Objects of this class add themselves as listeners to be notified of
     * changes that require the chord displayed in the inspector to change. At the moment that is the selection in the lead sheet and the
     * transposition option.
     *
     * @param evt
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getSource() == chordSymbol)
        {
            if (ChordLeadSheetItem.PROP_ITEM_DATA.equals(evt.getPropertyName()))
            {
                updateEditorModel();
            }
        } else if (PlaybackSettings.PROP_CHORD_SYMBOLS_DISPLAY_TRANSPOSITION.equals(evt.getPropertyName()))
        {
            setTransposition((int) evt.getNewValue());
        }
    }
}

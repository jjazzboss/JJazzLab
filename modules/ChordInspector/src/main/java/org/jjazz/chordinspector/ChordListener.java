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
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.chordleadsheet.api.item.Position;
import org.jjazz.cl_editor.api.CL_ContextActionListener;
import org.jjazz.cl_editor.api.CL_ContextActionSupport;
import org.jjazz.cl_editor.api.CL_SelectionUtilities;
import org.openide.util.Utilities;

/**
 * Listen to the current selected ChordSymbol to update the ChordInspectorPanel.
 */
public class ChordListener implements CL_ContextActionListener, PropertyChangeListener
{

    private final CL_ContextActionSupport cap;
    private CLI_ChordSymbol chordSymbol;
    private ChordInspectorPanel editor;

    public ChordListener(ChordInspectorPanel editor)
    {
        Preconditions.checkNotNull(editor);
        this.editor = editor;


        // Listen to selection changes in the current leadsheet editor
        cap = CL_ContextActionSupport.getInstance(Utilities.actionsGlobalContext());
        cap.addListener(this);

        selectionChange(cap.getSelection());
    }

    public void cleanup()
    {
        cap.removeListener(this);
        if (chordSymbol != null)
        {
            chordSymbol.removePropertyChangeListener(this);
        }
    }

    // -----------------------------------------------------------------------------
    // CL_ContextActionListener interface
    // -----------------------------------------------------------------------------   
    @Override
    public void selectionChange(CL_SelectionUtilities selection)
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
            newSelectedChordSymbol = cls.getLastItemBefore(new Position(selection.getMinBarIndex() + 1, 0), false, CLI_ChordSymbol.class,
                    cli -> true);
            if (newSelectedChordSymbol == null)
            {
                // Can happen if user temporarily remove all chord symbols!
                return;
            }
        } else
        {
            // Not a valid selection
            // Do nothing
            // Note: an empty selection is received when switching from a CL_Editor TopComponent to a different TopComponent
            return;
        }

        // Replace current chord symbol
        if (chordSymbol != null)
        {
            chordSymbol.removePropertyChangeListener(this);
        }
        chordSymbol = newSelectedChordSymbol;
        if (chordSymbol != null)
        {
            chordSymbol.addPropertyChangeListener(this);
        }

        if (chordSymbol != null)
        {
            editor.setModel(chordSymbol);
        }
    }

    @Override
    public void sizeChanged(int oldSize, int newSize)
    {
        // Nothing
    }

    // =================================================================================
    // PropertyChangeListener implementation
    // =================================================================================
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getSource() == chordSymbol)
        {
            if (ChordLeadSheetItem.PROP_ITEM_DATA.equals(evt.getPropertyName()))
            {
                editor.setModel(chordSymbol);
            }
        }
    }
}

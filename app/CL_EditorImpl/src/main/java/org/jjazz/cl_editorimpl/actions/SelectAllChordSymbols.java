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
package org.jjazz.cl_editorimpl.actions;

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.cl_editor.api.CL_Editor;
import org.jjazz.cl_editor.api.CL_EditorTopComponent;
import org.jjazz.cl_editor.api.CL_Selection;
import org.jjazz.cl_editor.itemrenderer.api.IR_Type;
import org.jjazz.cl_editor.itemrenderer.api.ItemRenderer;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

/**
 * Select all chord symbols
 * <p>
 * @see HalfLeadsheet
 */
@ActionID(category = "JJazz", id = "org.jjazz.cl_editor.actions.selectallchordsymbols")
@ActionRegistration(displayName = "#CTL_SelectAllChordSymbols", lazy = true)
@ActionReferences(
    {
        @ActionReference(path = "Actions/Bar", position = 1310),
        @ActionReference(path = "Actions/ChordSymbol", position = 1310),
        @ActionReference(path = "Menu/Edit", position = 1310),
    })
public class SelectAllChordSymbols implements ActionListener
{

    private final ChordLeadSheet cls;
    private static final Logger LOGGER = Logger.getLogger(SelectAllChordSymbols.class.getSimpleName());

    public SelectAllChordSymbols(ChordLeadSheet context)
    {
        this.cls = context;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        assert cls != null;
        CL_Editor editor = CL_EditorTopComponent.get(cls).getEditor();
        CL_Selection selection = new CL_Selection(editor.getLookup());
        var items = cls.getItems(CLI_ChordSymbol.class);
        if (items.isEmpty())
        {
            return;
        }


        editor.clearSelection();
        editor.selectItems(items, true);

        // Make sure focus ends on a chord symbol
        Component c = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (!(c instanceof ItemRenderer))
        {
            editor.setFocusOnItem(items.get(0), IR_Type.ChordSymbol);
        }

    }
}

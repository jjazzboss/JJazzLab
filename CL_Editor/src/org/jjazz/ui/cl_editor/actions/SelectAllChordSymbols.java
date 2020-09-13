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
package org.jjazz.ui.cl_editor.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import static org.jjazz.ui.cl_editor.actions.Bundle.CTL_SelectAllChordSymbols;
import org.jjazz.ui.cl_editor.api.CL_Editor;
import org.jjazz.ui.cl_editor.api.CL_EditorTopComponent;
import org.jjazz.ui.cl_editor.api.CL_SelectionUtilities;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

/**
 * Select all chord symbols
 * <p>
 * @see HalfLeadsheet
 */
@ActionID(category = "JJazz", id = "org.jjazz.ui.cl_editor.actions.selectallchordsymbols")
@ActionRegistration(displayName = "#CTL_SelectAllChordSymbols", lazy = true)
@ActionReferences(
        {
            @ActionReference(path = "Actions/Bar", position = 1310),
            @ActionReference(path = "Actions/ChordSymbol", position = 1310),
            @ActionReference(path = "Menu/Edit", position = 1310),
        })
@Messages("CTL_SelectAllChordSymbols=Select All Chord Symbols")
public class SelectAllChordSymbols implements ActionListener
{

    private final ChordLeadSheet cls;
    private static final String undoText = CTL_SelectAllChordSymbols();
    private static final Logger LOGGER = Logger.getLogger(SelectAllChordSymbols.class.getSimpleName());

    public SelectAllChordSymbols(ChordLeadSheet context)
    {
        this.cls = context;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        assert cls != null;
        CL_Editor editor = CL_EditorTopComponent.get(cls).getCL_Editor();
        CL_SelectionUtilities selection = new CL_SelectionUtilities(editor.getLookup());
        selection.unselectAll(editor);
        var items = cls.getItems(CLI_ChordSymbol.class);
        editor.selectItems(items, true);
    }
}

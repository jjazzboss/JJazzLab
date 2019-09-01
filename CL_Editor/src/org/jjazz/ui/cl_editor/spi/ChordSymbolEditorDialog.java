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
package org.jjazz.ui.cl_editor.spi;

import javax.swing.JDialog;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.ui.cl_editor.editors.ChordSymbolEditorDialogImpl;
import org.openide.util.Lookup;
import org.openide.windows.WindowManager;

/**
 * An editor dialog for a chord symbol.
 * <p>
 * The Dialog should not directly change the model, it should just return the proposed changes. The calling application will
 * update the model if OK and manage the undo/redo aspects.
 */
public abstract class ChordSymbolEditorDialog extends JDialog
{

    /**
     * Search the global lookup for an instance.
     *
     * @return Can be null if no instance found.
     */
    public static ChordSymbolEditorDialog getDefault()
    {
        ChordSymbolEditorDialog o = Lookup.getDefault().lookup(ChordSymbolEditorDialog.class);
        if (o == null)
        {
            o = ChordSymbolEditorDialogImpl.getInstance();
        }
        return o;
    }

    /**
     * Dialog is automatically owned by WindowManager.getDefault().getMainWindow()
     */
    protected ChordSymbolEditorDialog()
    {
        super(WindowManager.getDefault().getMainWindow());
    }

    /**
     * Prepare the dialog before being used.
     *
     * @param title Dialog title
     * @param item
     * @param key If different from 0 it represents the pressed key which triggered this dialog.
     * @param enableAlternate Enable the edition of the alternate chord symbol
     */
    abstract public void preset(String title, CLI_ChordSymbol item, char key, boolean enableAlternate);

    /**
     * @return True if dialog was exited OK, false if dialog operation was cancelled.
     */
    abstract public boolean exitedOk();

    /**
     * The new ExtChordSymbol data to be applied to the item.
     *
     * @return Non null only if exitedOk() was true.
     */
    abstract public ExtChordSymbol getData();

    /**
     * Cleanup refrerences to preset data and dialog results.
     */
    abstract public void cleanup();
}

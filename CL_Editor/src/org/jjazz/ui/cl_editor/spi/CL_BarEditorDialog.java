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

import java.util.List;
import java.util.Map;
import javax.swing.JDialog;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.leadsheet.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.ui.cl_editor.editors.CL_BarEditorDialogImpl;
import org.openide.util.*;
import org.openide.windows.WindowManager;

/**
 * A JDialog used to edit a ChordLeadSheet bar.
 * <p>
 * The Dialog should not directly change the model, it should just return the proposed changes. The calling application will
 * update the model (if dialog returned OK) and manage the undo/redo aspects.
 * <p>
 */
public abstract class CL_BarEditorDialog extends JDialog
{

    public static CL_BarEditorDialog getDefault()
    {
        CL_BarEditorDialog o = Lookup.getDefault().lookup(CL_BarEditorDialog.class);
        if (o == null)
        {
            o = CL_BarEditorDialogImpl.getInstance();
        }
        return o;
    }

    /**
     * Dialog is automatically owned by WindowManager.getDefault().getMainWindow()
     */
    public CL_BarEditorDialog()
    {
        super(WindowManager.getDefault().getMainWindow());
    }

    /**
     * Preset the dialog before using it.
     *
     * @param preset
     * @param cls ChordLeadSheet
     * @param barIndex
     */
    abstract public void preset(Preset preset, ChordLeadSheet cls, int barIndex);

    /**
     * @return True if dialog was exited OK, false if dialog operation was cancelled.
     */
    abstract public boolean isExitOk();

    /**
     * Return the section which should be inserted, or used to update bar's existing section.
     *
     * @return Can be null if section data (name or timesignature) has not changed.
     */
    abstract public CLI_Section getSection();

    /**
     * The new items that should be inserted in the bar.
     *
     * @return Can be empty if no items inserted.
     */
    abstract public List<ChordLeadSheetItem<?>> getAddedItems();

    /**
     * The items that should be removed in the bar.
     *
     * @return Can be empty if no items to remove.
     */
    abstract public List<ChordLeadSheetItem<?>> getRemovedItems();

    /**
     * The ChordSymbols for which data should be changed.
     *
     * @return Can be empty. Associate the new data for each updated chord symbol.
     */
    abstract public Map<CLI_ChordSymbol, ExtChordSymbol> getUpdatedChordSymbols();

    /**
     * Cleanup references to preset data and dialog results.
     *
     * @see preset()
     */
    abstract public void cleanup();
}

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
package org.jjazz.cl_editor.spi;

import java.util.List;
import java.util.Map;
import javax.swing.JDialog;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.cl_editor.api.DisplayTransposableRenderer;
import org.openide.util.*;
import org.openide.windows.WindowManager;

/**
 * A JDialog used to edit a ChordLeadSheet bar.
 * <p>
 * The Dialog should not directly change the model, it should just return the proposed changes. The calling application will update the model (if dialog
 * returned OK) and manage the undo/redo aspects.
 * <p>
 */
public abstract class CL_BarEditorDialog extends JDialog implements DisplayTransposableRenderer
{

    public static CL_BarEditorDialog getDefault()
    {
        CL_BarEditorDialog o = Lookup.getDefault().lookup(CL_BarEditorDialog.class);
        if (o == null)
        {
            throw new IllegalArgumentException("No CL_BarEditorDialog instance found");
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
     * <p>
     * Method must take into account a possible display transposition if it was previously set.
     *
     * @param preset
     * @param cls      ChordLeadSheet
     * @param barIndex
     * @param swing    If true the bar is in swing mode, eg half-bar position for a 3/4 rhythm is 5/3=1.666...
     * @see #setDisplayTransposition(int)
     */
    abstract public void preset(Preset preset, ChordLeadSheet cls, int barIndex, boolean swing);

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
    abstract public List<ChordLeadSheetItem> getAddedItems();

    /**
     * The items that should be removed in the bar.
     *
     * @return Can be empty if no items to remove.
     */
    abstract public List<ChordLeadSheetItem> getRemovedItems();

    /**
     * The Items for which data should be changed.
     *
     * @return Can be empty. Associate the new data for each changed item.
     */
    abstract public Map<ChordLeadSheetItem, Object> getChangedItems();

    /**
     * Cleanup references to preset data and dialog results.
     * <p>
     */
    abstract public void cleanup();

    /**
     * Must be called before calling preset() to be effective.
     *
     * @param dt
     * @see #preset(org.jjazz.cl_editor.spi.Preset, org.jjazz.chordleadsheet.api.ChordLeadSheet, int, boolean)
     */
    @Override
    abstract public void setDisplayTransposition(int dt);

    @Override
    abstract public int getDisplayTransposition();
}

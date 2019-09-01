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
package org.jjazz.ui.ss_editor.spi;

import javax.swing.JDialog;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.ui.ss_editor.editors.SimpleRhythmSelectionDialog;
import org.openide.util.Lookup;
import org.openide.windows.WindowManager;

/**
 * A dialog to select a rhythm.
 */
abstract public class RhythmSelectionDialog extends JDialog
{

    public static RhythmSelectionDialog getDefault()
    {
        RhythmSelectionDialog result = Lookup.getDefault().lookup(RhythmSelectionDialog.class);
        if (result == null)
        {
            result = new SimpleRhythmSelectionDialog();
        }
        return result;
    }

    /**
     * Dialog is automatically owned by WindowManager.getDefault().getMainWindow()
     */
    protected RhythmSelectionDialog()
    {
        super(WindowManager.getDefault().getMainWindow());
    }

    /**
     * Initialize the dialog for the specified rhythm.
     *
     * @param r
     */
    abstract public void preset(Rhythm r);

    /**
     * @return True if dialog was exited OK, false if dialog operation was cancelled.
     */
    abstract public boolean isExitOk();

    /**
     * Return the selected rhythm.
     *
     * @return Null if no valid rhythm was selected, or user chose Cancel
     */
    abstract public Rhythm getSelectedRhythm();

    /**
     * Set the title of the dialogm eg "Rhythm for bar XX".
     *
     * @param title
     */
    abstract public void setTitleLabel(String title);

    /**
     * Return if the rhythm should be also applied to the next songParts which have the same rhythm than the sptModel.
     *
     * @return
     */
    abstract public boolean isApplyRhythmToNextSongParts();

    /**
     * Return if the rhythm's preferred tempo should be applied to current song
     *
     * @return
     */
    abstract public boolean isUseRhythmTempo();

    /**
     * Cleanup references to preset data and dialog results.
     */
    abstract public void cleanup();
}

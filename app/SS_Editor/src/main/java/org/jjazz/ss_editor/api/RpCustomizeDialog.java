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
package org.jjazz.ss_editor.api;

import java.util.List;
import javax.swing.JDialog;
import org.jjazz.rhythm.api.RhythmParameter;
import org.openide.windows.WindowManager;

/**
 * A dialog to customize a set of RP values.
 */
abstract public class RpCustomizeDialog extends JDialog
{

    /**
     * Dialog is automatically owned by WindowManager.getDefault().getMainWindow()
     */
    protected RpCustomizeDialog()
    {
        super(WindowManager.getDefault().getMainWindow());
    }

    /**
     * Initialize the dialog.
     *
     * @param rp The RhythmParameter for which we want to edit n values.
     * @param n
     */
    abstract public void preset(RhythmParameter<?> rp, int n);

    /**
     * Return a list of RP values as a percentage for each Song Part.
     *
     * @return An empty list if no result available.
     */
    abstract public List<Double> getRpValues();

    /**
     * Cleanup preset data and dialog results.
     */
    abstract public void cleanup();
}

/*
 * 
 *   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 *   Copyright @2019 Jerome Lelasseux. All rights reserved.
 * 
 *   This file is part of the JJazzLab software.
 *    
 *   JJazzLab is free software: you can redistribute it and/or modify
 *   it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *   as published by the Free Software Foundation, either version 3 of the License, 
 *   or (at your option) any later version.
 * 
 *   JJazzLab is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *  
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *  
 *   Contributor(s): 
 * 
 */
package org.jjazz.rhythmselectiondialog.spi;

import org.jjazz.rhythmselectiondialog.api.RhythmSelectionDialog;
import org.openide.util.Lookup;

/**
 * A provider for RhythmSelectionDialogs.
 */
public interface RhythmSelectionDialogProvider
{

    static public RhythmSelectionDialogProvider getDefault()
    {
        var res = Lookup.getDefault().lookup(RhythmSelectionDialogProvider.class);
        if (res == null)
        {
            throw new IllegalArgumentException("No RhythmSelectionDialogProvider instance found in global lookup");
        }
        return res;
    }

    RhythmSelectionDialog getSimpleDialog();

    RhythmSelectionDialog getDialog();
}

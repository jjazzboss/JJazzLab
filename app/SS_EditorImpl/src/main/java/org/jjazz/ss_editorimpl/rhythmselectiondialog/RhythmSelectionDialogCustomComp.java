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
package org.jjazz.ss_editorimpl.rhythmselectiondialog;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import org.jjazz.uiutilities.api.ColumnLeftAlignedLayoutManager;
import org.jjazz.utilities.api.ResUtil;

public class RhythmSelectionDialogCustomComp extends JPanel
{

    private final JCheckBox cb_applyRhythmToNextSpts;
    private final JCheckBox cb_useRhythmTempo;
    private static RhythmSelectionDialogCustomComp INSTANCE;

    private RhythmSelectionDialogCustomComp()
    {
        cb_useRhythmTempo = new JCheckBox();
        cb_useRhythmTempo.setText(ResUtil.getString(RhythmSelectionDialogImpl.class, "RhythmSelectionDialogImpl.cb_useRhythmTempo.text"));
        cb_useRhythmTempo.setToolTipText(ResUtil.getString(RhythmSelectionDialogImpl.class, "RhythmSelectionDialogImpl.cb_useRhythmTempo.toolTipText"));
        cb_useRhythmTempo.setSelected(true);
        cb_applyRhythmToNextSpts = new JCheckBox();
        cb_applyRhythmToNextSpts.setText(ResUtil.getString(RhythmSelectionDialogImpl.class, "RhythmSelectionDialogImpl.cb_applyRhythmToNextSpts.text"));
        cb_applyRhythmToNextSpts.setToolTipText(
            ResUtil.getString(RhythmSelectionDialogImpl.class, "RhythmSelectionDialogImpl.cb_applyRhythmToNextSpts.toolTipText"));
        cb_applyRhythmToNextSpts.setSelected(true);

        setLayout(new ColumnLeftAlignedLayoutManager());
        add(cb_useRhythmTempo);
        add(cb_applyRhythmToNextSpts);
    }

    static public RhythmSelectionDialogCustomComp getInstance()
    {
        if (INSTANCE == null)
        {
            INSTANCE = new RhythmSelectionDialogCustomComp();
        }
        return INSTANCE;
    }

    public boolean isUseRhythmTempo()
    {
        return cb_useRhythmTempo.isSelected();
    }

    public boolean isApplyRhythmToNextSongParts()
    {
        return cb_applyRhythmToNextSpts.isSelected();
    }
}

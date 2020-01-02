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
package org.jjazz.instrumentchooser.api;

import javax.swing.JDialog;
import org.jjazz.instrumentchooser.DrumsInstrumentChooserDialogImpl;
import org.jjazz.midi.DrumKitType;
import org.jjazz.midi.DrumsInstrument;
import org.jjazz.midi.Instrument;
import org.jjazz.util.Filter;
import org.openide.util.Lookup;
import org.openide.windows.WindowManager;
import org.jjazz.midi.DrumKitKeyMap;

/**
 * A dialog to select a drums instrument in the instrument banks available on the system.
 */
public abstract class DrumsInstrumentChooserDialog extends JDialog
{

    public static DrumsInstrumentChooserDialog getDefault()
    {
        DrumsInstrumentChooserDialog result = Lookup.getDefault().lookup(DrumsInstrumentChooserDialog.class);
        if (result == null)
        {
            return DrumsInstrumentChooserDialogImpl.getInstance();
        }
        return result;
    }

    /**
     * Dialog is automatically owned by WindowManager.getDefault().getMainWindow()
     */
    protected DrumsInstrumentChooserDialog()
    {
        super(WindowManager.getDefault().getMainWindow());
    }

    /**
     * Initialize the dialog.
     *
     * @param kitType Expected DrumKitType
     * @param drumMap Expected DrumKitKeyMap
     * @param ins A default selected instrument. If null no selection is done.
     * @param channel Use this Midi channel to send the Midi patch changes. If -1 no midi messages sent.
     * @param title Dialog title.
     * @param filter Filtered instruments must not be shown by the dialog. If null accept all instruments
     */
    public abstract void preset(DrumKitType kitType, DrumKitKeyMap drumMap, DrumsInstrument ins, int channel, String title, Filter<Instrument> filter);

    /**
     * @return The selected instrument, or null if no selection or dialog cancelled.
     */
    public abstract DrumsInstrument getSelectedInstrument();
 
}

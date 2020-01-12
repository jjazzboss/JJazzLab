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
import org.jjazz.instrumentchooser.InstrumentChooserDialogImpl;
import org.jjazz.midi.Instrument;
import org.jjazz.util.Filter;
import org.openide.util.Lookup;
import org.openide.windows.WindowManager;

/**
 * A dialog to select a non-drums instrument in the instrument banks available on the system.
 */
public abstract class InstrumentChooserDialog extends JDialog
{

    public static InstrumentChooserDialog getDefault()
    {
        InstrumentChooserDialog result = Lookup.getDefault().lookup(InstrumentChooserDialog.class);
        if (result == null)
        {
            return InstrumentChooserDialogImpl.getInstance();
        }
        return result;
    }

    /**
     * Dialog is automatically owned by WindowManager.getDefault().getMainWindow()
     */
    protected InstrumentChooserDialog()
    {
        super(WindowManager.getDefault().getMainWindow());
    }

    /**
     * Initialize the dialog.
     *
     * @param ins A default selected instrument. If null no selection is done.
     * @param transpose The transposition in semitons.
     * @param channel Use this Midi channel to send the Midi patch changes. If -1 no midi messages sent.
     * @param title Dialog title.
     * @param filter Filtered instruments must not be shown by the dialog. If null accept all instruments
     */
    public abstract void preset(Instrument ins, int transpose, int channel, String title, Filter<Instrument> filter);

    /**
     * Initialize the dialog.
     *
     * @param ins A default selected instrument. If null no selection is done.
     * @param transpose The transposition in semitons.
     * @param channel Use this Midi channel to send the Midi patch changes. If -1 no midi messages sent.
     * @param title Dialog title.
     * @param filter Filtered instruments must not be shown by the dialog. If null accept all instruments
     */
    // public abstract void preset(Instrument ins, int transpose, int channel, String title, Filter<Instrument> filter);

    
    /**
     * @return The selected instrument, or null if no selection or dialog cancelled.
     */
    public abstract Instrument getSelectedInstrument();

    /**
     * The transposition for the seleced instrument.
     *
     * @return
     */
    public abstract int getTransposition();
}

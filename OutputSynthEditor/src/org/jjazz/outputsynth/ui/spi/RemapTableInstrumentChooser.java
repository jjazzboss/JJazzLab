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
package org.jjazz.outputsynth.ui.spi;

import javax.swing.JDialog;
import org.jjazz.midi.Instrument;
import org.jjazz.outputsynth.OutputSynth;
import org.jjazz.outputsynth.ui.RemapTableInstrumentChooserImpl;
import org.openide.util.Lookup;
import org.openide.windows.WindowManager;

/**
 * A dialog to select an instrument to remap another one.
 */
public abstract class RemapTableInstrumentChooser extends JDialog
{

    public static RemapTableInstrumentChooser getDefault()
    {
        RemapTableInstrumentChooser result = Lookup.getDefault().lookup(RemapTableInstrumentChooser.class);
        if (result == null)
        {
            return RemapTableInstrumentChooserImpl.getInstance();
        }
        return result;
    }

    /**
     * Dialog is automatically owned by WindowManager.getDefault().getMainWindow()
     */
    protected RemapTableInstrumentChooser()
    {
        super(WindowManager.getDefault().getMainWindow());
    }

    /**
     * Initialize the dialog.
     *
     * @param outSynth    The OutputSynth which contains the available instruments to choose from.
     * @param remappedIns The remapped instrument: a GM1Instrument or the special
     */
    public abstract void preset(OutputSynth outSynth, Instrument remappedIns);

    /**
     * @return The selected instrument, or null if no selection or dialog cancelled.
     */
    public abstract Instrument getSelectedInstrument();

    /**
     * Return true if the selected instrument should be also used as the Family default instrument.
     * <p>
     * Not used if the remappedIns passed in preset() was the DRUMS or PERCUSSION special instances.
     *
     * @return
     */
    public abstract boolean useAsFamilyDefault();

}

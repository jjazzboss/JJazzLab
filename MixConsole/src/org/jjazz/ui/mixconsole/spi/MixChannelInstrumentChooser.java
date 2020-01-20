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
package org.jjazz.ui.mixconsole.spi;

import javax.swing.JDialog;
import org.jjazz.midi.Instrument;
import org.jjazz.outputsynth.OutputSynth;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.ui.mixconsole.MixChannelInstrumentChooserImpl;
import org.openide.util.Lookup;
import org.openide.windows.WindowManager;

/**
 * A dialog to select an instrument for a MidiMix channel.
 */
public abstract class MixChannelInstrumentChooser extends JDialog
{

    public static MixChannelInstrumentChooser getDefault()
    {
        MixChannelInstrumentChooser result = Lookup.getDefault().lookup(MixChannelInstrumentChooser.class);
        if (result == null)
        {
            return MixChannelInstrumentChooserImpl.getInstance();
        }
        return result;
    }

    /**
     * Dialog is automatically owned by WindowManager.getDefault().getMainWindow()
     */
    protected MixChannelInstrumentChooser()
    {
        super(WindowManager.getDefault().getMainWindow());
    }

    /**
     * Initialize the dialog.
     *
     * @param outSynth The OutputSynth which contains the available instruments to choose from.
     * @param rv The RhythmVoice for which we're choosing an instrument.
     * @param preselectedIns
     * @param transpose The initial transposition in semi-tons.
     * @param channel The Midi channel for this RhythmVoice.
     */
    public abstract void preset(OutputSynth outSynth, RhythmVoice rv, Instrument preselectedIns, int transpose, int channel);

    /**
     * @return The selected instrument, or null if no selection or dialog cancelled.
     */
    public abstract Instrument getSelectedInstrument();
    
    public abstract int getTransposition();

}

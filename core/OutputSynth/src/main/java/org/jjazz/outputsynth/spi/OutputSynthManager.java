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
package org.jjazz.outputsynth.spi;

import java.beans.PropertyChangeListener;
import org.jjazz.outputsynth.api.OutputSynth;
import org.openide.util.Lookup;

/**
 * A manager for OutputSynth instances.
 */
public interface OutputSynthManager
{

    /**
     * Property change event fired each time a new OutputSynth is associated to the default JJazzLab MidiDevice OUT: oldValue=old OutputSynth, newValue=new
     * OutputSynth.
     * <p>
     * The change event is also fired when default JJazzLab MidiDevice OUT changes.
     */
    String PROP_DEFAULT_OUTPUTSYNTH = "PropDefaultOutputSynth";
    /**
     * Property change event fired each time a new OutputSynth is associated to a MidiDevice OUT: oldValue=Midi device OUT name, newValue=OutputSynth.
     */
    String PROP_MDOUT_OUTPUTSYNTH = "MdOut-OutputSynth";


    public static OutputSynthManager getDefault()
    {
        var res = Lookup.getDefault().lookup(OutputSynthManager.class);
        if (res == null)
        {
            throw new IllegalStateException("Can't find OutputSynthManager instance in the global lookup");
        }
        return res;
    }


    /**
     * Get the current OuputSynth associated to the default JJazzLab Midi Device OUT.
     * <p>
     * If no Midi Device OUT defined, then return a shared instance of a GM Output Synth.
     *
     * @return Can't be null
     * @see JJazzMidiSystem
     */
    OutputSynth getDefaultOutputSynth();


    /**
     * Get the OutputSynth associated to the specified output MidiDevice.
     *
     * @param mdOutName A Midi device OUT name, can't be null or empty
     * @return Can't be null.
     */
    OutputSynth getOutputSynth(String mdOutName);

    /**
     * Associate outSynth to the specified midi OUT device name.
     *
     * @param mdOutName Can't be null
     * @param outSynth  Can't be null
     */
    void setOutputSynth(String mdOutName, OutputSynth outSynth);

    /**
     * Get a new instance of a default OutputSynth which just uses the GMSynth.
     *
     * @return
     */
    OutputSynth getNewGMOuputSynth();

    /**
     * Scan all the OUT MidiDevices and make sure each MidiDevice is associated to an OutputSynth.
     * <p>
     * Should be called if the list of available OUT MidiDevices has changed.
     */
    void refresh();

    void addPropertyChangeListener(PropertyChangeListener l);

    void addPropertyChangeListener(String propName, PropertyChangeListener l);

    void removePropertyChangeListener(PropertyChangeListener l);

    void removePropertyChangeListener(String propName, PropertyChangeListener l);


}

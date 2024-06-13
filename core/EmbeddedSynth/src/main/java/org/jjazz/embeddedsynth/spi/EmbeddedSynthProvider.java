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
package org.jjazz.embeddedsynth.spi;

import java.beans.PropertyChangeListener;
import javax.sound.midi.MidiDevice;
import org.jjazz.embeddedsynth.api.EmbeddedSynth;
import org.jjazz.embeddedsynth.api.EmbeddedSynthException;
import org.openide.util.Lookup;

/**
 * Provide an EmbeddedSynth instance and its related methods.
 */
public interface EmbeddedSynthProvider
{

    public static final String PROP_PROVIDER_ENABLED = "PropProviderEnabled";
    public static final String PROP_EMBEDDED_SYNTH_ACTIVE = "PropEmbeddedSynthActive";

    /**
     * Get the first EmbeddedSynthProvider instance found in the global lookup.
     *
     * @return Can be null
     */
    static EmbeddedSynthProvider getDefaultProvider()
    {
        return Lookup.getDefault().lookup(EmbeddedSynthProvider.class);
    }


    /**
     * Helper method Get the synth from the default EmbeddedSynthProvider instance.
     *
     * @return Can be null
     */
    static EmbeddedSynth getDefaultSynth()
    {
        EmbeddedSynthProvider provider = getDefaultProvider();
        return (provider == null) ? null : provider.getEmbeddedSynth();
    }


    /**
     * Get the unique Id of this instance.
     *
     * @return
     */
    String getId();

    /**
     * Get the EmbeddedSynth instance.
     *
     * @return Might be null if provider is disabled.
     */
    EmbeddedSynth getEmbeddedSynth();

    /**
     * Get the OUT MidiDevice associated to the embedded synth.
     *
     * @return Might be null if provider is disabled.
     */
    MidiDevice getOutMidiDevice();

    /**
     * If b is true do what's necessary so that the EmbeddedSynth becomes the current JJazzLab output synth.
     * <p>
     * If state is changed a PROP_EMBEDDED_SYNTH_ACTIVE change event is fired.
     *
     * @param b
     * @throws org.jjazz.embeddedsynth.api.EmbeddedSynthException
     */
    void setEmbeddedSynthActive(boolean b) throws EmbeddedSynthException;

    /**
     * Check if the EmbeddedSynth is active.
     *
     * @return
     */
    boolean isEmbeddedSynthActive();

    /**
     * Check if this EmbeddedSynthProvider is enabled.
     * <p>
     * By default an EmbeddedSynthProvider is enabled, but it might get itself disabled if it encounters initialization errors, typically when calling
     * setEmbeddedSynthActive(). When it becomes disabled a PROP_PROVIDER_ENABLED change event is fired.
     *
     * @return
     */
    boolean isEnabled();

    void addPropertyChangeListener(PropertyChangeListener l);

    void removePropertyChangeListener(PropertyChangeListener l);

}

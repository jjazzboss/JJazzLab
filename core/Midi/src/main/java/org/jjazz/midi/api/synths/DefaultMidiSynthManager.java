/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 * This file is part of the JJazzLab software.
 *
 * JJazzLab is free software: you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License (LGPLv3) 
 * as published by the Free Software Foundation, either version 3 of the License, 
 * or (at your option) any later version.
 *
 * JJazzLab is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *
 * Contributor(s): 
 *
 */
package org.jjazz.midi.api.synths;

import com.google.common.base.Preconditions;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Logger;
import org.jjazz.midi.api.MidiSynth;
import org.jjazz.midi.spi.MidiSynthManager;


/**
 * A default (and basic) implementation of a MidiSynthManager.
 *
 * Initialized with GM/GM2/XG/GS synths.
 */
public class DefaultMidiSynthManager implements MidiSynthManager
{

    private static DefaultMidiSynthManager INSTANCE;
    protected final List<MidiSynth> midiSynths = new ArrayList<>();
    protected transient final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(DefaultMidiSynthManager.class.getSimpleName());

    static public DefaultMidiSynthManager getInstance()
    {
        if (INSTANCE == null)
        {
            INSTANCE = new DefaultMidiSynthManager();
        }
        return INSTANCE;
    }

    protected DefaultMidiSynthManager()
    {
        midiSynths.add(GMSynth.getInstance());
        midiSynths.add(GM2Synth.getInstance());
        midiSynths.add(XGSynth.getInstance());
        midiSynths.add(GSSynth.getInstance());
    }

    @Override
    public boolean addMidiSynth(MidiSynth midiSynth)
    {
        Preconditions.checkNotNull(midiSynth);

        if (!midiSynths.contains(midiSynth))
        {
            midiSynths.add(midiSynth);
            pcs.firePropertyChange(PROP_MIDISYNTH_LIST, null, midiSynth);
            return true;
        }

        return false;
    }

    @Override
    public boolean removeMidiSynth(MidiSynth midiSynth)
    {
        boolean res = midiSynths.remove(midiSynth);
        if (res)
        {
            pcs.firePropertyChange(PROP_MIDISYNTH_LIST, midiSynth, null);
        }
        return res;
    }

    @Override
    public List<MidiSynth> getMidiSynths()
    {
        return new ArrayList<>(midiSynths);
    }

    @Override
    public List<MidiSynth> getMidiSynths(Predicate<MidiSynth> tester)
    {
        return midiSynths
            .stream()
            .filter(ms -> tester.test(ms))
            .toList();
    }

    @Override
    public MidiSynth getMidiSynth(String name)
    {
        return midiSynths
            .stream()
            .filter(midiSynth -> midiSynth.getName().equals(name))
            .findAny()
            .orElse(null);
    }

    /**
     * Add PropertyChangeListener.
     *
     * @param listener
     */
    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener
    )
    {
        pcs.addPropertyChangeListener(listener);
    }

    /**
     * Remove PropertyChangeListener.
     *
     * @param listener
     */
    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener
    )
    {
        pcs.removePropertyChangeListener(listener);
    }
    // ===============================================================================
    // Private methods
    // ===============================================================================


}

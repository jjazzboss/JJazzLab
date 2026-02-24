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
import static org.jjazz.midi.spi.MidiSynthManager.loadFromResource;
import org.netbeans.api.annotations.common.StaticResource;


/**
 * Default implementation of a MidiSynthManager.
 *
 * Initialized with GM/GM2/XG/GS and JJazzLabSoundFont-based synths.
 */
public class DefaultMidiSynthManager implements MidiSynthManager
{

      // Some builtin MidiSynth names retrieved from a .ins file
    public static String JJAZZLAB_SOUNDFONT_GM2_SYNTH_NAME = "JJazzLab SoundFont (GM2)";
    public static String JJAZZLAB_SOUNDFONT_GS_SYNTH_NAME = "JJazzLab SoundFont (GS)";
    public static String JJAZZLAB_SOUNDFONT_XG_SYNTH_NAME = "JJazzLab SoundFont (XG)";

    @StaticResource(relative = true)
    private final static String JJAZZLAB_SOUNDFONT_GM2_SYNTH_PATH = "resources/JJazzLabSoundFontSynth_GM2.ins";
    @StaticResource(relative = true)
    private final static String JJAZZLAB_SOUNDFONT_GS_SYNTH_PATH = "resources/JJazzLabSoundFontSynth_GS.ins";
    @StaticResource(relative = true)
    private final static String JJAZZLAB_SOUNDFONT_XG_SYNTH_PATH = "resources/JJazzLabSoundFontSynth_XG.ins";

    
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
        midiSynths.add(loadFromResource(getClass(), JJAZZLAB_SOUNDFONT_GS_SYNTH_PATH));
        midiSynths.add(loadFromResource(getClass(), JJAZZLAB_SOUNDFONT_GM2_SYNTH_PATH));
        midiSynths.add(loadFromResource(getClass(), JJAZZLAB_SOUNDFONT_XG_SYNTH_PATH));        
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

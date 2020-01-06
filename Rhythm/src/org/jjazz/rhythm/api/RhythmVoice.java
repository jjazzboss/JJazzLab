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
package org.jjazz.rhythm.api;

import java.util.Objects;
import java.util.logging.Logger;
import org.jjazz.midi.DrumKit;
import org.jjazz.midi.Instrument;
import org.jjazz.midi.InstrumentSettings;
import org.jjazz.midi.MidiConst;
import org.jjazz.midi.StdSynth;

/**
 * Describe a voice for which a Rhythm will generate music.
 * <p>
 * This is an immutable class.
 */
public class RhythmVoice
{
    /**
     * The main types of a rhythm voice.
     */
    public enum Type
    {
        DRUMS, PERCUSSION, BASS, CHORD1, CHORD2, PHRASE1, PHRASE2, PAD, OTHER;

        public boolean isDrums()
        {
            return this.equals(DRUMS) || this.equals(PERCUSSION);
        }
    }

    private final String name;
    protected Instrument instrument;
    protected final InstrumentSettings instrumentSettings;
    private final int preferredChannel;
    private final Rhythm container;
    private final DrumKit drumKit;
    private final Type type;
    private static final Logger LOGGER = Logger.getLogger(RhythmVoice.class.getName());

    /**
     * Create a RhythmVoice for a non-drums voice with a default InstrumentSettings.
     *
     * @param container The Rhythm this RhythmVoice belongs to.
     * @param type DRUMS or PERCUSSION not allowed.
     * @param name
     * @param instrument The instrument to be used by default. Can't be null. Must have getSubstitute() defined.
     * @param preferredChannel Preferred Midi channel
     */
    public RhythmVoice(Rhythm container, Type type, String name, Instrument instrument, int preferredChannel)
    {
        this(container, type, name, instrument, new InstrumentSettings(), preferredChannel);
    }

    /**
     * Create a RhythmVoice for a non-drums voice.
     *
     * @param container The Rhythm this RhythmVoice belongs to.
     * @param type DRUMS or PERCUSSION not allowed.
     * @param name Name of the RhythmVoice
     * @param ins The recommended instrument. Can't be null. Must have getSubstitute() defined.
     * @param is The recommended InstrumentSettings.
     * @param preferredChannel The preferred Midi channel for this voice.
     */
    public RhythmVoice(Rhythm container, Type type, String name, Instrument ins, InstrumentSettings is, int preferredChannel)
    {
        if (container == null || type == null || type.equals(Type.DRUMS) || type.equals(Type.PERCUSSION) || ins == null
                || ins.getSubstitute() == null || name == null || is == null || !MidiConst.checkMidiChannel(preferredChannel))
        {
            throw new IllegalArgumentException(
                    "container=" + container + "type=" + type + " name=" + name + " ins=" + ins.toLongString() + " is=" + is + " preferredChannel=" + preferredChannel);
        }
        this.container = container;
        this.name = name;
        this.instrument = ins;
        this.instrumentSettings = new InstrumentSettings(is);
        this.preferredChannel = preferredChannel;
        this.drumKit = null;
        this.type = type;
    }

    /**
     * Create a drums/percussion RhythmVoice with a default InstrumentSettings.
     *
     * @param drumKit
     * @param container
     * @param type Must be DRUMS or PERCUSSION.
     * @param name
     * @param preferredChannel
     */
    public RhythmVoice(DrumKit drumKit, Rhythm container, Type type, String name, int preferredChannel)
    {
        this(drumKit, container, type, name, new InstrumentSettings(), preferredChannel);
    }

    /**
     * Create a RhythmVoice for Drums/Percussion instruments.
     * <p>
     * The preferred Instrument is set to the VoidInstrument.
     *
     * @param kit
     * @param container The Rhythm this RhythmVoice belongs to.
     * @param type Must be DRUMS or PERCUSSION.
     * @param name Name of the RhythmVoice
     * @param is The recommended InstrumentSettings.
     * @param preferredChannel The preferred Midi channel for this voice.
     */
    public RhythmVoice(DrumKit kit, Rhythm container, Type type, String name, InstrumentSettings is, int preferredChannel)
    {
        if (kit == null || container == null || type == null || (!type.equals(Type.DRUMS) && !type.equals(Type.PERCUSSION))
                || name == null || is == null || !MidiConst.checkMidiChannel(preferredChannel))
        {
            throw new IllegalArgumentException(
                    "kit=" + kit + " container=" + container + " type=" + type + " name=" + name
                    + " is=" + is + " preferredChannel=" + preferredChannel);
        }
        this.container = container;
        this.name = name;
        this.type = type;
        this.instrument = StdSynth.getVoidInstrument();
        this.instrumentSettings = new InstrumentSettings(is);
        this.preferredChannel = preferredChannel;
        this.drumKit = kit;
    }

    /**
     * @return True if this object's type is Drums or Percussion.
     */
    public boolean isDrums()
    {
        return this.type.isDrums();
    }

    /**
     * Get the DrumKit.
     *
     * @return Null if this is not a Drums/Percussion rhythm voice.
     */
    public DrumKit getDrumKit()
    {
        return drumKit;
    }

    /**
     * @return The Rhythm for which this RhythmVoice is set.
     */
    public Rhythm getContainer()
    {
        return container;
    }

    public int getPreferredChannel()
    {
        return preferredChannel;
    }

    /**
     * @return The type of the rhythm voice.
     */
    public Type getType()
    {
        return type;
    }

    /**
     * @return E.g. "Horn riffs", "walking bass"
     */
    public String getName()
    {
        return name;
    }

    /**
     * The preferred Instrument for this voice.
     *
     * @return Can't be null. For Drums/Percussion return the VoidInstrument. Returned instrument has its method getSubstitute()
     * defined.
     */
    public Instrument getPreferredInstrument()
    {
        return this.instrument;
    }

    /**
     * Recommended instrument settings to be used for this voice.
     *
     * @return A copy of this rhythmVoice's InstrumentSettings. Can't be null.
     */
    public InstrumentSettings getPreferredInstrumentSettings()
    {
        return new InstrumentSettings(instrumentSettings);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final RhythmVoice other = (RhythmVoice) obj;
        if (!Objects.equals(this.name, other.name))
        {
            return false;
        }
        if (this.type != other.type)
        {
            return false;
        }
        if (this.preferredChannel != other.preferredChannel)
        {
            return false;
        }
        if (!Objects.equals(this.instrument, other.instrument))
        {
            return false;
        }
        if (!Objects.equals(this.instrumentSettings, other.instrumentSettings))
        {
            return false;
        }
        if (!Objects.equals(this.container, other.container))
        {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 89 * hash + Objects.hashCode(this.type);
        hash = 89 * hash + Objects.hashCode(this.name);
        hash = 89 * hash + Objects.hashCode(this.instrument);
        hash = 89 * hash + Objects.hashCode(this.instrumentSettings);
        hash = 89 * hash + Objects.hashCode(this.container);
        hash = 89 * hash + Objects.hashCode(this.preferredChannel);
        return hash;
    }

    @Override
    public String toString()
    {
        return this.getName() + "[" + getType() + ", pref.ch=" + this.preferredChannel + ", pref.ins=" + instrument
                + ", r=" + container.getUniqueId()
                + ", pref.vol=" + instrumentSettings.getVolume()
                + ", pref.pan=" + instrumentSettings.getPanoramic()
                + ", pref.rev=" + instrumentSettings.getReverb()
                + ", pref.cho=" + instrumentSettings.getChorus()
                + ", pref.velShift=" + instrumentSettings.getVelocityShift()
                + ", pref.tr=" + instrumentSettings.getTransposition()
                + "]";
    }

}

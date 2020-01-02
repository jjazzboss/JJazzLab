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
import org.jjazz.midi.GM1Instrument;
import org.jjazz.midi.Instrument;
import org.jjazz.midi.InstrumentSettings;
import org.jjazz.midi.MidiConst;

/**
 * Describe a voice for which a Rhythm will generate music.
 * <p>
 * This is an immutable class.
 */
public class RhythmVoice
{

    private final RvType type;
    private final String name;
    protected Instrument instrument;
    protected final InstrumentSettings instrumentSettings;
    private final int preferredChannel;
    private final Rhythm container;
    private final DrumKit drumKit;
    private static final Logger LOGGER = Logger.getLogger(RhythmVoice.class.getName());

    /**
     * Create a non-drums/percussion RhythmVoice with a default InstrumentSettings and the default channel associated to specified
     * type.
     *
     * @param container  The Rhythm this RhythmVoice belongs to.
     * @param type       Must NOT be DRUMS or PERCUSSION
     * @param name
     * @param instrument The instrument to be used by default. Can't be null. Must have getSubstitute() defined.
     */
    public RhythmVoice(Rhythm container, RvType type, String name, Instrument instrument)
    {
        this(container, type, name, instrument, new InstrumentSettings(), type.getPreferredChannel());
    }

    /**
     * Create a non-drums/percussion RhythmVoice for instruments which are not Drums/Percussion.
     *
     * @param container        The Rhythm this RhythmVoice belongs to.
     * @param type             Can not be DRUMS or PERCUSSION.
     * @param name             Name of the RhythmVoice
     * @param ins              The recommended instrument. Can't be null. Must have getSubstitute() defined.
     * @param is               The recommended InstrumentSettings.
     * @param preferredChannel The preferred Midi channel for this voice.
     */
    public RhythmVoice(Rhythm container, RvType type, String name, Instrument ins, InstrumentSettings is, int preferredChannel)
    {
        if (container == null || type == null || ins == null || ins.getSubstitute() == null || type.equals(RvType.Drums)
                || type.equals(RvType.Percussion) || name == null || is == null || !MidiConst.checkMidiChannel(preferredChannel))
        {
            throw new IllegalArgumentException(
                    "container=" + container + " type=" + type + " name=" + name + " ins=" + ins.toLongString() + " is=" + is + " preferredChannel=" + preferredChannel);
        }
        this.container = container;
        this.name = name;
        this.type = type;
        this.instrument = ins;
        this.instrumentSettings = new InstrumentSettings(is);
        this.preferredChannel = preferredChannel;
        this.drumKit = null;
    }

    /**
     * Create a drums/percussion RhythmVoice with a default InstrumentSettings and the default channel associated to specified
     * type.
     *
     * @param drumKit
     * @param container
     * @param type      Must be DRUMS or PERCUSSION.
     * @param name
     */
    public RhythmVoice(DrumKit drumKit, Rhythm container, RvType type, String name)
    {
        this(drumKit, container, type, name, new InstrumentSettings(), type.getPreferredChannel());
    }

    /**
     * Create a RhythmVoice for Drums/Percussion instruments.
     *
     * @param kit
     * @param container        The Rhythm this RhythmVoice belongs to.
     * @param type             Must be DRUMS or PERCUSSION : use a RhythmVoiceDrums in this case.
     * @param name             Name of the RhythmVoice
     * @param is               The recommended InstrumentSettings.
     * @param preferredChannel The preferred Midi channel for this voice.
     */
    public RhythmVoice(DrumKit kit, Rhythm container, RvType type, String name, InstrumentSettings is, int preferredChannel)
    {
        if (kit == null || container == null || type == null || !(type.equals(RvType.Drums) || type.equals(RvType.Percussion))
                || name == null || is == null || !MidiConst.checkMidiChannel(preferredChannel))
        {
            throw new IllegalArgumentException(
                    "kit=" + kit + " container=" + container + " type=" + type + " name=" + name
                    + " is=" + is + " preferredChannel=" + preferredChannel);
        }
        this.container = container;
        this.name = name;
        this.type = type;
        this.instrument = null;
        this.instrumentSettings = new InstrumentSettings(is);
        this.preferredChannel = preferredChannel;
        this.drumKit = kit;
    }

    /**
     * @return True if this object's type is Drums or Percussion.
     */
    public boolean isDrums()
    {
        return this.type.equals(RvType.Drums) || this.type.equals(RvType.Percussion);
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
    public RvType getType()
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
     * @return Can be null for drums/percussion. Returned instrument has its method getSubstitute() defined.
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

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
package org.jjazz.rhythm.stubs;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.midi.DrumKit;
import org.jjazz.midi.DrumKit.Type;
import org.jjazz.midi.synths.GM1Bank;
import org.jjazz.midi.synths.StdSynth;
import org.jjazz.midi.keymap.KeyMapGM;
import org.jjazz.midi.synths.Family;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.api.TempoRange;
import org.jjazz.rhythm.parameters.RP_STD_Variation;
import org.jjazz.rhythm.parameters.RhythmParameter;
import org.jjazz.rhythmmusicgeneration.DummyGenerator;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

/**
 * A rhythm stub whatever the time signature.
 * <p>
 */
public class RhythmStub implements Rhythm
{

    protected String uniqueId;
    protected TimeSignature timeSignature;
    protected Lookup lookup;

    /**
     * The default RhythmParameters associated to this rhythm.
     */
    protected ArrayList<RhythmParameter<?>> rhythmParameters = new ArrayList<>();
    /**
     * The supported RhythmVoices.
     */
    protected ArrayList<RhythmVoice> rhythmVoices = new ArrayList<>();
    private static final Logger LOGGER = Logger.getLogger(RhythmStub.class.getSimpleName());

    /**
     * Create a dummy rhythm for specified time signature.
     *
     *
     * @param uniqueId
     * @param ts
     */
    public RhythmStub(String uniqueId, TimeSignature ts)
    {
        if (uniqueId == null || uniqueId.trim().isEmpty() || ts == null)
        {
            throw new IllegalArgumentException("uniqueId=" + uniqueId + " ts=" + ts);
        }

        this.uniqueId = uniqueId;
        this.timeSignature = ts;

        // Our Rhythm Parameters
        rhythmParameters.add(new RP_STD_Variation());

        // Rhythm voices
        GM1Bank gmb = StdSynth.getGM1Bank();
        rhythmVoices.add(new RhythmVoice(new DrumKit(Type.STANDARD, KeyMapGM.getInstance()), this, RhythmVoice.Type.DRUMS, "Drums", 9));
        rhythmVoices.add(new RhythmVoice(this, RhythmVoice.Type.BASS, "Bass", gmb.getDefaultInstrument(Family.Bass), 10));

        // The music generator
        lookup = Lookups.fixed(new DummyGenerator(this));
    }

    @Override
    public boolean equals(Object o)
    {
        boolean res = false;
        if (o instanceof RhythmStub)
        {
            RhythmStub ar = (RhythmStub) o;
            res = ar.uniqueId.equals(uniqueId);
        }
        return res;
    }

    @Override
    public int hashCode()
    {
        int hash = 5;
        hash = 47 * hash + Objects.hashCode(this.uniqueId);
        return hash;
    }

    @Override
    public List<RhythmVoice> getRhythmVoices()
    {
        return new ArrayList<>(rhythmVoices);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<RhythmParameter<?>> getRhythmParameters()
    {
        return new ArrayList<>(rhythmParameters);
    }

    @Override
    public Lookup getLookup()
    {
        return lookup;
    }

    @Override
    public TimeSignature getTimeSignature()
    {
        return timeSignature;
    }

    /**
     * @return true
     */
    @Override
    public boolean loadResources()
    {
        // Do nothing
        return true;
    }

    /**
     *
     * @return true
     */
    @Override
    public boolean isResourcesLoaded()
    {
        return true;
    }

    /**
     * This implementation does nothing.
     */
    @Override
    public void releaseResources()
    {
        // Do nothing
    }

    @Override
    public int compareTo(Rhythm o)
    {
        return getName().compareTo(o.getName());
    }

    @Override
    public File getFile()
    {
        return new File("");
    }

    @Override
    public String getUniqueId()
    {
        return uniqueId;
    }

    @Override
    public String getDescription()
    {
        return "Dummy description";
    }

    @Override
    public Feel getFeel()
    {
        return Feel.BINARY;
    }

    @Override
    public TempoRange getTempoRange()
    {
        return TempoRange.MEDIUM;
    }

    @Override
    public int getPreferredTempo()
    {
        return 120;
    }

    @Override
    public String getName()
    {
        return "DummyName-" + getTimeSignature().toString();
    }

    @Override
    public String getAuthor()
    {
        return "JL";
    }

    @Override
    public String getVersion()
    {
        return "1";
    }

    @Override
    public String[] getTags()
    {
        return new String[]
        {
            "dummy"
        };
    }

    @Override
    public String toString()
    {
        return getName();
    }

}

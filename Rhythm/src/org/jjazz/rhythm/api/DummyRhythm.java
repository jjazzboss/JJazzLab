/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 * This file is part of the JJazzLab-X software.
 *
 * JJazzLab-X is free software: you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License (LGPLv3)
 * as published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * JJazzLab-X is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JJazzLab-X.  If not, see <https://www.gnu.org/licenses/>
 *
 * Contributor(s):
 *
 */
package org.jjazz.rhythm.api;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.midi.synths.Family;
import org.jjazz.midi.synths.GM1Bank;
import org.jjazz.midi.synths.StdSynth;
import org.jjazz.rhythm.api.RhythmVoice.Type;
import org.jjazz.rhythm.parameters.RP_STD_Variation;
import org.jjazz.rhythm.parameters.RhythmParameter;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

/**
 * A dummy rhythm which does nothing.
 */
public class DummyRhythm implements Rhythm
{

    protected String name;
    protected TimeSignature timeSignature;
    protected Lookup lookup;
    protected RhythmFeatures features;

    /**
     * The default RhythmParameters associated to this rhythm.
     */
    protected ArrayList<RhythmParameter<?>> rhythmParameters = new ArrayList<>();
    /**
     * The supported RhythmVoices.
     */
    protected ArrayList<RhythmVoice> rhythmVoices = new ArrayList<>();
    private static final Logger LOGGER = Logger.getLogger(DummyRhythm.class.getSimpleName());

    /**
     * Create a dummy rhythm for specified time signature.
     *
     * @param name
     * @param ts
     */
    public DummyRhythm(String name, TimeSignature ts)
    {
        if (ts == null)
        {
            throw new NullPointerException("ts");
        }

        this.name = name;
        this.timeSignature = ts;
        // Our Rhythm Parameters
        rhythmParameters.add(new RP_STD_Variation());

        // Rhythm voices
        GM1Bank gmb = StdSynth.getInstance().getGM1Bank();
        rhythmVoices.add(new RhythmVoice(this, Type.BASS, "Bass", gmb.getDefaultInstrument(Family.Bass), 11));

        features = new RhythmFeatures();

        // The music generator
        lookup = Lookups.fixed("dummy lookup");
    }

    @Override
    public boolean equals(Object o)
    {
        boolean res = false;
        if (o instanceof DummyRhythm)
        {
            DummyRhythm ar = (DummyRhythm) o;
            res = ar.getUniqueId().equals(getUniqueId());
        }
        return res;
    }

    @Override
    public TimeSignature getTimeSignature()
    {
        return timeSignature;
    }

    @Override
    public int hashCode()
    {
        int hash = 5;
        hash = 47 * hash + Objects.hashCode(this.getUniqueId());
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
    public RhythmFeatures getFeatures()
    {
        return features;
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
        return name + "-ID";
    }

    @Override
    public String getDescription()
    {
        return "Dummy description";
    }

    @Override
    public int getPreferredTempo()
    {
        return 120;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public String getAuthor()
    {
        return "JL";
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

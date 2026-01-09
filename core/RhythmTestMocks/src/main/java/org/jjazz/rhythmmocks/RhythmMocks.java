/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright @2025 Jerome Lelasseux. All rights reserved.
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
package org.jjazz.rhythmmocks;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.midi.api.DrumKit.Type;
import org.jjazz.midi.api.keymap.KeyMapGM;
import org.jjazz.midi.api.synths.GM1Bank;
import org.jjazz.midi.api.synths.GMSynth;
import org.jjazz.midi.api.synths.InstrumentFamily;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmFeatures;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Variation;

/**
 * Test mock for {@link Rhythm} that does not implement MusicGeneratorProvider. This class can be used
 * when testing with rhythms without the need of music generation; e.g. SongStructure.
 */
public class RhythmMocks implements Rhythm
{
    private final String uniqueId;
    private final TimeSignature timeSignature;
    private final RhythmFeatures features;
    /**
     * The default RhythmParameters associated to this rhythm.
     */
    private final ArrayList<RhythmParameter<?>> rhythmParameters = new ArrayList<>();
    /**
     * The supported RhythmVoices.
     */
    private final ArrayList<RhythmVoice> rhythmVoices = new ArrayList<>();

    /**
     * Create a dummy rhythm for specified time signature.
     *
     * @param uniqueId
     * @param ts
     */
    public RhythmMocks(String uniqueId, TimeSignature ts, RhythmFeatures fs)
    {
        if (uniqueId == null || uniqueId.trim().isEmpty() || ts == null|| fs == null)
        {
            throw new IllegalArgumentException("uniqueId=" + uniqueId + " ts=" + ts);
        }

        this.uniqueId = uniqueId;
        this.timeSignature = ts;
        this.features = fs;

        // Rhythm voices
        GM1Bank gmb = GMSynth.getInstance().getGM1Bank();
        rhythmVoices.add(new RhythmVoice(new DrumKit(Type.STANDARD, KeyMapGM.getInstance()), this, RhythmVoice.Type.DRUMS, "Drums",
                GMSynth.getInstance().getVoidInstrument(), 9));
        rhythmVoices.add(new RhythmVoice(this, RhythmVoice.Type.BASS, "Bass", gmb.getDefaultInstrument(InstrumentFamily.Bass), 10));

        // Our Rhythm Parameters
        rhythmParameters.add(new RP_SYS_Variation(true));
    }

    // ==============================================================================================
    // Rhythm interface
    // ==============================================================================================

    @Override
    public boolean equals(Object o)
    {
        boolean res = false;
        if (o instanceof RhythmMocks rs)
        {
            res = rs.uniqueId.equals(uniqueId);
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
    public TimeSignature getTimeSignature()
    {
        return timeSignature;
    }

    @Override
    public void loadResources()
    {
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
    public RhythmFeatures getFeatures()
    {
        return features;
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
    public int getPreferredTempo()
    {
        return 120;
    }

    @Override
    public String getName()
    {
        return "JuanName-" + getTimeSignature().toString();
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

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        // Nothing
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        // Nothing
    }
}

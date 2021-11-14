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

import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.midi.api.DrumKit.Type;
import org.jjazz.midi.api.synths.GM1Bank;
import org.jjazz.midi.api.synths.StdSynth;
import org.jjazz.midi.api.keymap.KeyMapGM;
import org.jjazz.midi.api.synths.Family;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmFeatures;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.api.rhythmparameters.RP_STD_Variation;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.rhythmmusicgeneration.api.DummyGenerator;
import org.jjazz.rhythmmusicgeneration.spi.MusicGenerator;
import org.jjazz.songcontext.api.SongContext;

/**
 * A rhythm stub whatever the time signature.
 * <p>
 */
public class RhythmStub implements Rhythm, MusicGenerator
{

    protected String uniqueId;
    protected TimeSignature timeSignature;
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
            throw new IllegalArgumentException("uniqueId=" + uniqueId + " ts=" + ts);   //NOI18N
        }

        this.uniqueId = uniqueId;
        this.timeSignature = ts;

        // Rhythm voices
        GM1Bank gmb = StdSynth.getInstance().getGM1Bank();
        rhythmVoices.add(new RhythmVoice(new DrumKit(Type.STANDARD, KeyMapGM.getInstance()), this, RhythmVoice.Type.DRUMS, "Drums", StdSynth.getInstance().getVoidInstrument(), 9));
        rhythmVoices.add(new RhythmVoice(this, RhythmVoice.Type.BASS, "Bass", gmb.getDefaultInstrument(Family.Bass), 10));

        // Our Rhythm Parameters
        rhythmParameters.add(new RP_STD_Variation());

    }

    @Override
    public HashMap<RhythmVoice, Phrase> generateMusic(SongContext context) throws MusicGenerationException
    {
        return new DummyGenerator(this).generateMusic(context);
    }

    @Override
    public boolean equals(Object o)
    {
        boolean res = false;
        if (o instanceof RhythmStub)
        {
            RhythmStub rs = (RhythmStub) o;
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
        return new RhythmFeatures();
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
        return "DummyName-" + getTimeSignature().toString();
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

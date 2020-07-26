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
package org.jjazz.rhythm.database;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmFeatures;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.parameters.RhythmParameter;

/**
 * A description of a Rhythm for catalog purpose.
 * <p>
 */
public class RhythmInfo implements Serializable
{

    private String rhythmProviderId;
    private File file;
    private String name;
    private String[] tags;
    private String description;
    private String version;
    private String author;
    private TimeSignature timeSignature;
    private int preferredTempo;
    private RhythmFeatures rhythmFeatures;
    private final List<RhythmVoiceInfo> cacheRvs = new ArrayList<>();
    private final List<RhythmParameterInfo> cacheRps = new ArrayList<>();


    /**
     * Constructs a cache instance from an existing file-based rhythm.
     *
     * @param rhythm
     */
    public RhythmInfo(Rhythm rhythm, String rhythmProviderId)
    {
        if (rhythm == null || rhythmProviderId == null || rhythmProviderId.isBlank())
        {
            throw new IllegalArgumentException("r=" + rhythm + " rhythm.getFile()=" + rhythm.getFile() + " rhythmProviderId=" + rhythmProviderId);
        }
        this.rhythmProviderId = rhythmProviderId;
        this.file = rhythm.getFile();
        this.name = rhythm.getName();
        this.tags = rhythm.getTags();
        this.description = rhythm.getDescription();
        this.version = rhythm.getVersion();
        this.author = rhythm.getAuthor();
        this.preferredTempo = rhythm.getPreferredTempo();
        this.timeSignature = rhythm.getTimeSignature();
        this.rhythmFeatures = rhythm.getFeatures();
        for (RhythmVoice rv : rhythm.getRhythmVoices())
        {
            cacheRvs.add(new RhythmVoiceInfo(rv));
        }
        for (RhythmParameter rp : rhythm.getRhythmParameters())
        {
            cacheRps.add(new RhythmParameterInfo(rp));
        }
    }

    public List<RhythmVoiceInfo> getCacheRhythmVoices()
    {
        return new ArrayList<>(cacheRvs);
    }

    public List<RhythmParameterInfo> getCacheRhythmParameters()
    {
        return new ArrayList<>(cacheRps);
    }


    public RhythmFeatures getFeatures()
    {
        return rhythmFeatures;
    }

    public File getFile()
    {
        return file;
    }

    public String getRhythmProviderId()
    {
        return rhythmProviderId;
    }

    public String getDescription()
    {
        return description;
    }


    public int getPreferredTempo()
    {
        return preferredTempo;
    }

    public TimeSignature getTimeSignature()
    {
        return timeSignature;
    }


    public String getName()
    {
        return name;
    }


    public String getAuthor()
    {
        return author;
    }

    public String getVersion()
    {
        return version;
    }


    public String[] getTags()
    {
        return tags;
    }


    // ===========================================================================================
    // Private methods
    // ===========================================================================================
}

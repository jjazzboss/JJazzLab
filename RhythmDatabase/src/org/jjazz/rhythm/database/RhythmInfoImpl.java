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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.rhythm.api.AdaptedRhythm;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmFeatures;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.database.api.RhythmDatabase;
import org.jjazz.rhythm.database.api.RhythmInfo;
import org.jjazz.rhythm.database.api.RhythmParameterInfo;
import org.jjazz.rhythm.database.api.RhythmVoiceInfo;
import org.jjazz.rhythm.parameters.RhythmParameter;
import org.jjazz.rhythm.spi.RhythmProvider;

public class RhythmInfoImpl implements RhythmInfo
{

    private String rhythmProviderId;
    private String rhythmUniqueId;
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
    private static final Logger LOGGER = Logger.getLogger(RhythmInfoImpl.class.getSimpleName());

    /**
     * Reserved for deserialization only.
     */
    private RhythmInfoImpl()
    {

    }

    /**
     * Constructs a RhythmInfo from an existing rhythm.
     *
     * @param rhythm
     * @param rhythmProvider
     * @throws IllegalArgumentException If rhythm is an AdaptedRhythm
     */
    public RhythmInfoImpl(Rhythm rhythm, RhythmProvider rhythmProvider)
    {
        if (rhythm == null || rhythmProvider == null || rhythm instanceof AdaptedRhythm)
        {
            throw new IllegalArgumentException("rhythm=" + rhythm + " rhythm.getFile()=" + rhythm.getFile() + " rhythmProvider=" + rhythmProvider);
        }
        this.rhythmProviderId = rhythmProvider.getInfo().getUniqueId();
        this.rhythmUniqueId = rhythm.getUniqueId();
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

    /**
     * Check that this RhythmInfo object matches data from specified rhythm.
     * <p>
     * Test only the main fields.
     *
     * @param r
     * @return False if inconsistency detected (see log file for details).
     */
    public boolean checkConsistency(Rhythm r)
    {
        boolean b = true;
        if (!rhythmUniqueId.equals(r.getUniqueId()))
        {
            LOGGER.warning("checkConsistency() r=" + r + ": uniqueId mismatch. rhythmUniqueId=" + rhythmUniqueId + " r.getUniqueId()=" + r.getUniqueId());
            b = false;
        }
        if (!rhythmProviderId.equals(RhythmDatabase.getDefault().getRhythmProvider(r).getInfo().getUniqueId()))
        {
            LOGGER.warning("checkConsistency() r=" + r + ": rhythmProviderId mismatch. rhythmProviderId=" + rhythmProviderId
                    + " rdb.rp.uniqueId=" + RhythmDatabase.getDefault().getRhythmProvider(r).getInfo().getUniqueId());
            b = false;
        }
        if (!name.equals(r.getName()))
        {
            LOGGER.warning("checkConsistency() r=" + r + ": name mismatch. name=" + name + " r.getName()=" + r.getName());
            b = false;
        }
        if (!file.equals(r.getFile()))
        {
            LOGGER.warning("checkConsistency() r=" + r + ": file mismatch. file=" + file.getAbsolutePath() + " r.getFile()=" + r.getFile().getAbsolutePath());
            b = false;
        }
        if (!timeSignature.equals(r.getTimeSignature()))
        {
            LOGGER.warning("checkConsistency() r=" + r + ": timeSignature mismatch. timeSignature=" + timeSignature + " r.getTimeSignature()=" + r.getTimeSignature());
            b = false;
        }

        return b;
    }

    @Override
    public List<RhythmVoiceInfo> getRhythmVoiceInfos()
    {
        return new ArrayList<>(cacheRvs);
    }

    @Override
    public List<RhythmParameterInfo> getRhythmParametersInfos()
    {
        return new ArrayList<>(cacheRps);
    }

    @Override
    public RhythmFeatures getFeatures()
    {
        return rhythmFeatures;
    }

    @Override
    public File getFile()
    {
        return file;
    }

    @Override
    public String getUniqueId()
    {
        return this.rhythmUniqueId;
    }

    @Override
    public String getRhythmProviderId()
    {
        return rhythmProviderId;
    }

    @Override
    public String getDescription()
    {
        return description;
    }

    @Override
    public int getPreferredTempo()
    {
        return preferredTempo;
    }

    @Override
    public TimeSignature getTimeSignature()
    {
        return timeSignature;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public String getAuthor()
    {
        return author;
    }

    @Override
    public String getVersion()
    {
        return version;
    }

    @Override
    public String[] getTags()
    {
        return tags;
    }

    @Override
    public String toString()
    {
        return "Rinfo[" + getName() + "-" + getTimeSignature() + "]";
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 59 * hash + Objects.hashCode(this.rhythmProviderId);
        hash = 59 * hash + Objects.hashCode(this.rhythmUniqueId);
        return hash;
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
        final RhythmInfoImpl other = (RhythmInfoImpl) obj;
        if (!Objects.equals(this.rhythmProviderId, other.rhythmProviderId))
        {
            return false;
        }
        return true;
    }

    // ===========================================================================================
    // Private methods
    // ===========================================================================================
}

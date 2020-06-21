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
import java.util.List;
import java.util.Objects;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.rhythm.api.AdaptedRhythm;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmFeatures;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.parameters.RhythmParameter;
import org.openide.util.Lookup;

/**
 * An adapted rhythm stub whatever the time signature.
 * <p>
 * FOR UNIT TESTS ONLY (when real rhythmdatabase rhythmProviders are not available).
 * <p>
 */
public class AdaptedRhythmStub implements AdaptedRhythm
{

    private RhythmStub sourceRhythm;
    private TimeSignature timeSignature;

    public AdaptedRhythmStub(RhythmStub rs, TimeSignature ts)
    {
        if (rs == null)
        {
            throw new IllegalArgumentException("rs=" + rs);
        }
        sourceRhythm = rs;
        timeSignature = ts;
    }

    @Override
    public boolean equals(Object o)
    {
        boolean res = false;
        if (o instanceof AdaptedRhythmStub)
        {
            AdaptedRhythmStub rs = (AdaptedRhythmStub) o;
            res = rs.getUniqueId().equals(getUniqueId());
        }
        return res;
    }

    @Override
    public int hashCode()
    {
        int hash = 5;
        hash = 47 * hash + Objects.hashCode(getUniqueId());
        return hash;
    }

    @Override
    public List<RhythmVoice> getRhythmVoices()
    {
        return sourceRhythm.getRhythmVoices();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<RhythmParameter<?>> getRhythmParameters()
    {
        return sourceRhythm.getRhythmParameters();
    }

    @Override
    public Lookup getLookup()
    {
        return sourceRhythm.getLookup();
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
        return "StubRhythmProviderID" + RHYTHM_ID_DELIMITER + sourceRhythm.getUniqueId() + RHYTHM_ID_DELIMITER + timeSignature.toString();
    }

    @Override
    public String getDescription()
    {
        return "Adapted Dummy description";
    }

    @Override
    public int getPreferredTempo()
    {
        return 120;
    }

    @Override
    public String getName()
    {
        return "AdaptedDummyName-" + getTimeSignature().toString();
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
    public Rhythm getSourceRhythm()
    {
        return sourceRhythm;
    }


}

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
import java.util.List;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.rhythm.api.AdaptedRhythm;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmFeatures;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.rhythm.api.RhythmVoice;

/**
 * An adapted rhythm for {@link RhythmMocks}.
 */
public class AdaptedRhythmMocks implements AdaptedRhythm
{
    private final RhythmMocks sourceRhythm;
    private final TimeSignature timeSignature;

    public AdaptedRhythmMocks(RhythmMocks r, TimeSignature ts)
    {
        if (r.getTimeSignature().equals(ts))
        {
            throw new IllegalArgumentException("r=" + r + " ts=" + ts);
        }
        sourceRhythm = r;
        timeSignature = ts;
    }

    @Override
    public Rhythm getSourceRhythm()
    {
        return sourceRhythm;
    }

    @Override
    public String getUniqueId()
    {
        return AdaptedRhythm.buildUniqueId(RhythmMocksProviderImpl.ID, sourceRhythm, timeSignature);
    }

    @Override
    public RhythmFeatures getFeatures()
    {
        return sourceRhythm.getFeatures();
    }

    @Override
    public void loadResources() throws MusicGenerationException
    {
        // Nothing
    }

    @Override
    public void releaseResources()
    {
        // Nothing
    }

    @Override
    public boolean isResourcesLoaded()
    {
        return true;
    }

    @Override
    public List<RhythmVoice> getRhythmVoices()
    {
        return sourceRhythm.getRhythmVoices();
    }

    @Override
    public List<RhythmParameter<?>> getRhythmParameters()
    {
        return sourceRhythm.getRhythmParameters();
    }

    @Override
    public File getFile()
    {
        return new File("");
    }

    @Override
    public String getDescription()
    {
        return getName() + " - adapted rhythm";
    }

    @Override
    public int getPreferredTempo()
    {
        return sourceRhythm.getPreferredTempo();
    }

    @Override
    public TimeSignature getTimeSignature()
    {
        return timeSignature;
    }

    @Override
    public String getName()
    {
        return sourceRhythm.getName() + "[" + timeSignature + "]";
    }

    @Override
    public String getAuthor()
    {
        return sourceRhythm.getAuthor();
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

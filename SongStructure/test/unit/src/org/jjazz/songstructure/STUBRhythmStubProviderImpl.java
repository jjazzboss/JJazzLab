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
package org.jjazz.songstructure;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.rhythm.api.AdaptedRhythm;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.spi.RhythmProvider;
import org.jjazz.util.MultipleErrorsReport;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * UNIT TEST STUB.
 */
@ServiceProvider(service = RhythmProvider.class)
public class STUBRhythmStubProviderImpl implements RhythmProvider
{

    private Info info;
    private ArrayList<Rhythm> rhythms = new ArrayList<>();

    public STUBRhythmStubProviderImpl()
    {
        info = new Info("UT-RhythmProviderID", "ut rhythms", "Provides a dummy rhythm for each time signature", "JL", "1.0");
        for (TimeSignature ts : TimeSignature.values())
        {
            rhythms.add(new STUBRhythm("UT-RhythmA-" + ts.toString(), ts));
            rhythms.add(new STUBRhythm("UT-RhythmB-" + ts.toString(), ts));
        }
    }

    @Override
    public void showUserSettingsDialog()
    {
        // Nothing
    }

    /**
     * Return true if RhythmProvider has settings which can be modified by end-user.
     * <p>
     *
     * @return @see showUserSettingsDialog()
     */
    @Override
    public boolean hasUserSettings()
    {
        return false;
    }

    @Override
    public String toString()
    {
        return info.getName();
    }

    @Override
    public Info getInfo()
    {
        return info;
    }

    @Override
    public List<Rhythm> getBuiltinRhythms(MultipleErrorsReport errRpt)
    {
        return new ArrayList<>(rhythms);
    }

    @Override
    public List<Rhythm> getFileRhythms( boolean forceRescan, MultipleErrorsReport errRpt)
    {
        return new ArrayList<>();
    }

    @Override
    public String[] getSupportedFileExtensions()
    {
        return new String[0];
    }

    @Override
    public Rhythm readFast(File f) throws IOException
    {
        throw new IOException("This RhythmProvider (" + getInfo().getName() + ") does not support file reading.");
    }

    @Override
    public AdaptedRhythm getAdaptedRhythm(Rhythm r, TimeSignature ts)
    {
        if (r == null || ts == null || r.getTimeSignature().equals(ts))
        {
            throw new IllegalArgumentException("r=" + r + " ts=" + ts);   //NOI18N
        }
        if (r instanceof STUBRhythm)
        {
            return new STUBAdaptedRhythm((STUBRhythm) r, ts);
        }
        return null;
    }

}

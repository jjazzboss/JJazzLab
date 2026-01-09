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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.rhythm.api.Division;
import org.jjazz.rhythm.api.Genre;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmFeatures;
import org.jjazz.rhythm.api.TempoRange;
import org.jjazz.rhythm.spi.RhythmProvider;
import org.jjazz.rhythm.spi.StubRhythmProvider;
import org.jjazz.utilities.api.MultipleErrorsReport;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

@ServiceProviders(value =
{
    @ServiceProvider(service = StubRhythmProvider.class),
    @ServiceProvider(service = RhythmProvider.class)            // So it's collected by the rhythmdatabase
})
public  class RhythmMocksProviderImpl implements StubRhythmProvider
{
    public static final String ID = "RhythmMocksProviderID";
    private final Info info;
    private final ArrayList<Rhythm> rhythms = new ArrayList<>();

    public RhythmMocksProviderImpl()
    {
        info = new Info(ID, "DUMMY_RHYTHMS", "DUMMY_RHYTHMS_DESC", "JL", "1.0");
        for (Division div : Division.values())
        {
            for (TimeSignature ts : TimeSignature.values())
            {
                rhythms.add(new RhythmMocks("RhythmStubID-" + ts.toString(), ts,
                        new RhythmFeatures(Genre.BALLROOM, div, TempoRange.ALL_TEMPO)));
            }
        }
        System.out.println("=================== RHYTHMS CREATED =============================");
    }

    @Override
    public Rhythm getStubRhythm(TimeSignature ts)
    {
        if (ts == null)
        {
            throw new NullPointerException("ts");
        }
        Rhythm res = null;
        for (Rhythm r : rhythms)
        {
            if (r.getTimeSignature().equals(ts))
            {
                res = r;
                break;
            }
        }
        return res;
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
    public List<Rhythm> getFileRhythms(boolean forceRescan, MultipleErrorsReport errRpt)
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
    public AdaptedRhythmMocks getAdaptedRhythm(Rhythm r, TimeSignature ts)
    {
        var res = (r instanceof RhythmMocks rs) ? new AdaptedRhythmMocks(rs, ts) : null;
        return res;
    }
}

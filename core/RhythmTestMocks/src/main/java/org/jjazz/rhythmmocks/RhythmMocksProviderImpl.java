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
import java.util.Objects;
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


/**
 * Rhythm provider for unit tests.
 * <p>
 * Provides 2 rhythm instances per time signature, each one with different RhythmFeatures.
 * <p>
 * Module has no dependence to RhythmMusicGeneration module to avoid a circular dependency problem during unit tests.
 * <p>
 */
@ServiceProviders(value =
{
    @ServiceProvider(service = StubRhythmProvider.class),
    @ServiceProvider(service = RhythmProvider.class)            // So it's collected by the rhythmdatabase
})
public class RhythmMocksProviderImpl implements StubRhythmProvider
{

    public static final String ID = "RhythmTestMocksProviderID";
    private final Info info;
    private final ArrayList<Rhythm> rhythms = new ArrayList<>();

    public RhythmMocksProviderImpl()
    {
        info = new Info(ID, "RhythmTestMocksProvider", "dummy desc", "JL", "1.0");

        for (TimeSignature ts : TimeSignature.values())
        {
            var r1 = new RhythmMock("BossaMockID-" + ts.toString(), ts, new RhythmFeatures(Genre.BOSSA, Division.BINARY, TempoRange.ALL_TEMPO));
            var r2 = new RhythmMock("JazzMockID-" + ts.toString(), ts, new RhythmFeatures(Genre.JAZZ, Division.EIGHTH_SHUFFLE, TempoRange.MEDIUM));
            rhythms.add(r1);
            rhythms.add(r2);
        }
    }

    @Override
    public Rhythm getStubRhythm(TimeSignature ts)
    {
        Objects.requireNonNull(ts);
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
    public AdaptedRhythmMock getAdaptedRhythm(Rhythm r, TimeSignature ts)
    {
        var res = (r instanceof RhythmMock rs) ? new AdaptedRhythmMock(rs, ts) : null;
        return res;
    }
}

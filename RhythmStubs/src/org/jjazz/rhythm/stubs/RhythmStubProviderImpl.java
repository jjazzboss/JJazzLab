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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.rhythm.api.AdaptedRhythm;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.spi.RhythmProvider;
import org.jjazz.util.MultipleErrorsReport;
import org.jjazz.rhythm.spi.StubRhythmProvider;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

@ServiceProviders(value =
{
    @ServiceProvider(service = StubRhythmProvider.class),
    @ServiceProvider(service = RhythmProvider.class)            // So that it appears in the Rhythm selection dialog box
}
)
public class RhythmStubProviderImpl implements StubRhythmProvider
{

    private Info info;
    private ArrayList<Rhythm> rhythms = new ArrayList<>();

    public RhythmStubProviderImpl()
    {
        info = new Info("StubRhythmProviderID", "Dummy rhythms", "Provides a dummy rhythm for each time signature", "JL", "1.0");
        for (TimeSignature ts : TimeSignature.values())
        {
            rhythms.add(new RhythmStub("RhythmStubID-" + ts.toString(), ts));
        }
    }

    @Override
    public Rhythm getStubRhythm(TimeSignature ts)
    {
        if (ts == null)
        {
            throw new NullPointerException("ts");   //NOI18N
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
    public AdaptedRhythm getAdaptedRhythm(Rhythm r, TimeSignature ts)
    {
        return null;
    }

}

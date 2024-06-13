/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright @2019 Jerome Lelasseux. All rights reserved.
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
package org.jjazz.rhythmdatabaseimpl.api;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.rhythm.api.AdaptedRhythm;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.spi.RhythmProvider;
import org.jjazz.rhythmdatabase.api.RhythmInfo;
import org.jjazz.utilities.api.MultipleErrorsReport;
import org.jjazz.utilities.api.ResUtil;

/**
 * A RhythmProvider instance which only provides the rhythms from the FavoriteRhythms instance.
 * <p>
 */
public class FavoriteRhythmProvider implements RhythmProvider
{

    private static FavoriteRhythmProvider INSTANCE;

    static public FavoriteRhythmProvider getInstance()
    {
        synchronized (FavoriteRhythmProvider.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new FavoriteRhythmProvider();

            }
        }
        return INSTANCE;
    }

    private FavoriteRhythmProvider()
    {

    }

    /**
     * Get the list of favorite rhythms.
     *
     * @return
     */
    public List<RhythmInfo> getBuiltinRhythmInfos()
    {
        return FavoriteRhythms.getInstance().getRhythms();
    }

    // ======================================================================
    // RhythmProvider interface
    // ======================================================================    
    @Override
    public Info getInfo()
    {
        return new RhythmProvider.Info("FavoriteRhythmProviderId", ResUtil.getString(getClass(), "FAVORITE_RHYTHMS"), ResUtil.getString(getClass(), "FAVORITE_RHYTHMS_FROM_ALL_RHYTHM_PROVIDERS"), "JL", "1");
    }

    /**
     * Return an empty list.
     * <p>
     * See getBuiltinRhythmInfos().
     *
     * @return
     */
    @Override
    public List<Rhythm> getBuiltinRhythms(MultipleErrorsReport errRpt)
    {
        return Collections.emptyList();
    }

    /**
     * Return an empty list.
     * <p>
     * See getBuiltinRhythmInfos().
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Rhythm> getFileRhythms(boolean forceRescan, MultipleErrorsReport errRpt)
    {
        return (List<Rhythm>) Collections.EMPTY_LIST;
    }

    @Override
    public void showUserSettingsDialog()
    {
        // Nothing
    }

    @Override
    public boolean hasUserSettings()
    {
        return false;
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

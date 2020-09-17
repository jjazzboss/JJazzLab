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
package org.jjazz.rhythmselectiondialog;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.rhythm.api.AdaptedRhythm;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.database.api.FavoriteRhythms;
import org.jjazz.rhythm.database.api.RhythmInfo;
import org.jjazz.rhythm.spi.RhythmProvider;

/**
 * A "fake" RhythmProvider instance which provides the favorite rhythms from FavoriteRhythms for UI purposes.
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
        return new RhythmProvider.Info("FavoriteRhythmProviderId", "Favorite Rhythms", "Favorite rhythms from all Rhythm Providers", "JL", "1");
    }

    /**
     * Return an empty list.
     * <p>
     * See getBuiltinRhythmInfos().
     *
     * @return
     */
    @Override
    public List<Rhythm> getBuiltinRhythms(RhythmProvider.UserErrorReport errRpt)
    {
        return Collections.emptyList();
    }

    /**
     * Return an empty list.
     * <p>
     * See getBuiltinRhythmInfos().
     *
     * @param prevList
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Rhythm> getFileRhythms(boolean forceRescan, RhythmProvider.UserErrorReport errRpt)
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

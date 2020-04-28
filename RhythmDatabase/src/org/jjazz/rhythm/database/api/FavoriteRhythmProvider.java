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
package org.jjazz.rhythm.database.api;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmFeatures;
import org.jjazz.rhythm.spi.RhythmProvider;

/**
 * A special RhythmProvider instance which provides (as built-in rhythms) the favorite rhythms from FavoriteRhythms.
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

    // ======================================================================
    // RhythmProvider interface
    // ======================================================================    
    @Override
    public Info getInfo()
    {
        return new RhythmProvider.Info("FavoriteRhythmProviderId", "Favorite Rhythms", "Favorite rhythms from all Rhythm Providers", "JL", "1");
    }

    /**
     * Return the rhythms from the FavoriteRhythms object.
     *
     * @return
     */
    @Override
    public List<Rhythm> getBuiltinRhythms()
    {
        return FavoriteRhythms.getInstance().getRhythms();
    }

    /**
     * Return an empty list.
     *
     * @param prevList
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Rhythm> getFileRhythms(List<Rhythm> prevList, boolean forceRescan)
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
    public Rhythm getDefaultRhythm(RhythmFeatures rhythmFeatures, TimeSignature ts)
    {
        return null;
    }

    // ======================================================================
    // Private methods
    // ======================================================================
}

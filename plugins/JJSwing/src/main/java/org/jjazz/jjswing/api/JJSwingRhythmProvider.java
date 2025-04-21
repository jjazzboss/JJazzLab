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
package org.jjazz.jjswing.api;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.rhythm.api.AdaptedRhythm;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.spi.RhythmProvider;
import org.jjazz.utilities.api.MultipleErrorsReport;
import org.openide.util.lookup.ServiceProvider;
import org.jjazz.rhythmdatabase.api.RhythmInfo;
import org.openide.util.Lookup;


/**
 * Provider of the jjSwing rhythms.
 */
@ServiceProvider(service = RhythmProvider.class)
public class JJSwingRhythmProvider implements RhythmProvider
{

    public static final String RP_ID = "jjSwingRhythmProviderID";
    private final Info info;
    private List<Rhythm> rhythms;
    private static final Logger LOGGER = Logger.getLogger(JJSwingRhythmProvider.class.getSimpleName());


    public JJSwingRhythmProvider()
    {
        info = new Info(RP_ID, "jjSwing styles", "jjSwing rhythm provider", "JL", "1");
    }

    static public JJSwingRhythmProvider getInstance()
    {
        return Lookup.getDefault().lookup(JJSwingRhythmProvider.class);
    }

    @Override
    public final String[] getSupportedFileExtensions()
    {
        return new String[0];
    }

    @Override
    public Info getInfo()
    {
        return info;
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
    public List<Rhythm> getBuiltinRhythms(MultipleErrorsReport errRpt)
    {
        if (rhythms == null)
        {
            rhythms = new ArrayList<>();
            try
            {
                var r = new JJSwingRhythm();
                rhythms.add(r);
            } catch (Exception ex)
            {
                LOGGER.log(Level.SEVERE, "JJSwingRhythmProvider() Can not create JJSwingRhythm instance. ex={0}", ex.getMessage());
                errRpt.primaryErrorMessage = "Unexpected error creating the JJSwingRhythm instance: " + ex.getMessage();
            }

        }
        return rhythms;
    }

    @Override
    public List<Rhythm> getFileRhythms(boolean forceRescan, MultipleErrorsReport errRpt)
    {
        return Collections.emptyList();
    }

    @Override
    public Rhythm readFast(File extFile) throws IOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public AdaptedRhythm getAdaptedRhythm(Rhythm r, TimeSignature ts)
    {             
        return null;
    }

    static public boolean isMine(RhythmInfo ri)
    {
        return ri.rhythmProviderId().equals(RP_ID);
    }

    // -------------------------------------------------------------------------------------------------
    // Private methods
    // -------------------------------------------------------------------------------------------------

}

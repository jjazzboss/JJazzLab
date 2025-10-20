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
package org.jjazz.jjswing;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.jjswing.api.JJSwingRhythm;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.rhythm.api.AdaptedRhythm;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.spi.RhythmProvider;
import org.jjazz.utilities.api.MultipleErrorsReport;
import org.openide.util.lookup.ServiceProvider;
import org.jjazz.rhythmdatabase.api.RhythmInfo;
import org.jjazz.utilities.api.Utilities;
import org.netbeans.api.annotations.common.StaticResource;
import org.openide.util.Lookup;
import org.openide.windows.WindowManager;


/**
 * Provider of the jjSwing rhythms.
 */
@ServiceProvider(service = RhythmProvider.class)
public class JJSwingRhythmProvider implements RhythmProvider
{

    public static final String RP_ID = "jjSwingRhythmProviderID";

    @StaticResource(relative = true)
    public static final String DEFAULT_RHYTHM_MIX_RESOURCE_PATH = "jjSwing.mix";
    private final Info info;
    private JJSwingRhythm jjSwingRhythm;
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
        JJSwingRhythmProviderSettingsDialog dlg = new JJSwingRhythmProviderSettingsDialog();
        dlg.setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
        dlg.setVisible(true);
    }

    @Override
    public boolean hasUserSettings()
    {
        return true;
    }

    @Override
    public List<Rhythm> getBuiltinRhythms(MultipleErrorsReport errRpt)
    {
        List<Rhythm> res;
        try
        {
            res = List.of(getJJSwingRhythm());
        } catch (IOException ex)
        {
            rhythmCreationError(ex, errRpt);
            res = Collections.emptyList();
        }
        return res;
    }

    @Override
    public List<Rhythm> getFileRhythms(boolean forceRescan, MultipleErrorsReport errRpt)
    {
        if (forceRescan)
        {
            try
            {
                copyDefaultRhythmMixFile(getJJSwingRhythm());
            } catch (IOException ex)
            {
                rhythmCreationError(ex, errRpt);
            }
        }
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
        // TO BE IMPLEMENTED !
        LOGGER.warning("getAdaptedRhythm() Not yet implemented");
        return null;
    }

    static public boolean isMine(RhythmInfo ri)
    {
        return ri.rhythmProviderId().equals(RP_ID);
    }

    // -------------------------------------------------------------------------------------------------
    // Private methods
    // -------------------------------------------------------------------------------------------------

    private JJSwingRhythm getJJSwingRhythm() throws IOException
    {
        if (jjSwingRhythm == null)
        {
            jjSwingRhythm = new JJSwingRhythm();
        }
        return jjSwingRhythm;
    }

    private void rhythmCreationError(Exception ex, MultipleErrorsReport errRpt)
    {
        LOGGER.log(Level.SEVERE, "rhythmCreationError() Can not create JJSwingRhythm instance. ex={0}", ex.getMessage());
        errRpt.primaryErrorMessage = "Unexpected error creating the JJSwingRhythm instance: " + ex.getMessage();
    }

    private void copyDefaultRhythmMixFile(Rhythm r)
    {
        File mixFile = MidiMix.getRhythmMixFile(r.getName(), r.getFile());
        Utilities.copyResource(getClass(), DEFAULT_RHYTHM_MIX_RESOURCE_PATH, mixFile.toPath());
        LOGGER.log(Level.FINE, "copyDefaultRhythmMixFile() Copied resource " + DEFAULT_RHYTHM_MIX_RESOURCE_PATH + " to {0}", mixFile);
    }

}

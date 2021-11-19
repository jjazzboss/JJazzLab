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
package org.jjazz.uisettings.api;

import com.formdev.flatlaf.FlatDarkLaf;
import java.awt.Color;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.UIManager;
import org.openide.modules.ModuleInstall;
import org.openide.util.NbPreferences;

/**
 * Install the look & feel required by the Theme, see explanations below while @OnStart was not used.
 * <p>
 */
public class LookAndFeelInstaller extends ModuleInstall
{

    private static final Logger LOGGER = Logger.getLogger(LookAndFeelInstaller.class.getSimpleName());

    @Override
    public void validate()
    {

        //
        // IMPORTANT: global lookup ServiceProviders are not yet available at this early stage
        //
        var uis = GeneralUISettings.getInstance();
        GeneralUISettings.LookAndFeelId lafId = uis.getLafIdUponRestart();

        LOGGER.info("validate() Installing Look & Feel: " + lafId.name());   //NOI18N

        switch (lafId)
        {
            case LOOK_AND_FEEL_SYSTEM_DEFAULT:
            // NbPreferences.root().node("laf").remove("laf");
            // break;

            case LOOK_AND_FEEL_FLAT_DARK_LAF:
                // Code from Netbeans mailing list, see PraxisLive application 
                // IMPORTANT NB 12.5: need to add **both** FlatLaf NB modules as dependencies, including the non-API one FlatLaf Look & Feel, otherwise
                // some settings are NOT correctly set (e.g. white background in the Options panel)
                Preferences prefs = NbPreferences.root().node("laf");
                if (prefs.get("laf", "").isBlank())
                {
                    prefs.put("laf", "com.formdev.flatlaf.FlatDarkLaf");
                }
                break;
            default:
                throw new AssertionError(lafId.name());

        }
    }
}

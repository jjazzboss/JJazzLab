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
package org.jjazz.uisettings.api;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import org.openide.modules.ModuleInstall;
import org.openide.util.NbPreferences;

/**
 * Install the look &amp; feel required by the Theme, see explanations below while @OnStart was not used.
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

        var theme = GeneralUISettings.getInstance().getSessionTheme();
        var lafId = theme.getLookAndFeel().getPath();
        LOGGER.log(Level.INFO, "validate() Installing Look & Feel: {0}", lafId);
        Preferences prefs = NbPreferences.root().node("laf");
        prefs.put("laf", lafId);
    }
}

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
package org.jjazz.uisettings;

import java.util.logging.Logger;
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


        var uis = GeneralUISettings.getInstance();
        String tn = uis.getThemeUponRestart();
        Theme theme = uis.getTheme(tn);
        if (theme == null)
        {
            LOGGER.warning("validate() no theme found with name=" + tn + ". Using default theme.");
            theme = uis.getDefaultTheme();
        }

        switch (theme.getLookAndFeel())
        {
            case LOOK_AND_FEEL_DEFAULT:
                NbPreferences.root().node("laf").remove("laf");
                break;
            case LOOK_AND_FEEL_FLAT_DARK_LAF:
                // On Thu, 25 Jun 2020 at 00:40, Laszlo Kishalmi <laszlo.kishalmi@gmail.com> wrote:
                // > NbPreferences.root().node( "laf" ).put( "laf", "com.formdev.flatlaf.FlatDarkLaf" ); 
                // Somewhere really early, probably at an @OnStart marked runnable.
                // This probably needs to be done in ModuleInstall::validate - @OnStart
                // is too late to work consistently, unless behaviour has changed
                // recently.
                // You can see use of validate() in eg.
                // https://github.com/Revivius/nb-darcula/blob/master/src/main/java/com/revivius/nb/darcula/Installer.java#L29
                // and https://github.com/praxis-live/praxis-live/blob/v2.3.3/praxis.live.laf/src/net/neilcsmith/praxis/live/laf/Installer.java#L53
                NbPreferences.root().node("laf").put("laf", "com.formdev.flatlaf.FlatDarkLaf");
                break;
            default:
                throw new AssertionError(theme.getLookAndFeel().name());

        }
    }
}

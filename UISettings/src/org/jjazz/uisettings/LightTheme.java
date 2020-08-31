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

import javax.swing.ImageIcon;
import javax.swing.UIDefaults;
import org.netbeans.api.annotations.common.StaticResource;
import org.openide.util.lookup.ServiceProvider;

/**
 * A JJazzLab theme using light colors.
 */
@ServiceProvider(service = Theme.class)
public class LightTheme implements Theme
{

    public static String NAME = "Light Theme";
    @StaticResource(relative = true)
    private static final String SPEAKER_ICON_DISABLED_PATH = "resources/SpeakerDisabled-20x20.png";

    private UIDefaults uiDefaults;

    private String name;

    public LightTheme()
    {
        this.name = NAME;
        uiDefaults = new UIDefaults();
        UIDefaults.LazyValue value = tbl -> new ImageIcon(getClass().getResource(SPEAKER_ICON_DISABLED_PATH));
        uiDefaults.put("speaker.icon.disabled", value);
    }

    @Override
    public UIDefaults getUIDefaults()
    {
        return uiDefaults;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public GeneralUISettings.LookAndFeelId getLookAndFeel()
    {
        return GeneralUISettings.LookAndFeelId.LOOK_AND_FEEL_SYSTEM_DEFAULT;
    }
}

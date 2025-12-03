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

import javax.swing.UIDefaults;

/**
 * A set of colors/fonts/borders etc and L&amp;F to customize application appearance.
 * <p>
 */
public interface Theme
{
    /**
     * The name of the theme.
     *
     * @return
     */
    String getName();

    /**
     * The UI settings for this theme.
     *
     * @return
     */
    UIDefaults getUIDefaults();

    /**
     * The look &amp; feel required for this theme.
     *
     * @return
     */
    GeneralUISettings.LookAndFeelId getLookAndFeel();
    
    @Override
    String toString();
}

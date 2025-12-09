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
package org.jjazz.base.api.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import org.jjazz.utilities.api.Utilities;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.Exceptions;

@ActionID(category = "Help", id = "org.jjazz.base.actions.Donate")
@ActionRegistration(displayName = "#CTL_Donate", lazy = true)
@ActionReference(path = "Menu/Help", position = 300, separatorBefore = 299)
public final class Donate implements ActionListener
{

    private static final String DONATE_URL = "https://www.jjazzlab.org/en/donate";

    @Override
    public void actionPerformed(ActionEvent e)
    {
        URL url = null;
        try
        {
            var uri = URI.create(DONATE_URL);
            url = uri.toURL();
        } catch (MalformedURLException ex)
        {
            Exceptions.printStackTrace(ex);
        }

        Utilities.systemOpenURLInBrowser(url, false);
    }
}

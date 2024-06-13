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
package org.jjazz.test;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

/**
 * For debug purposes...
 */
//@ActionID(category = "JJazz", id = "org.jjazz.test.printallproperteis")
//@ActionRegistration(displayName = "Print all properties")
//@ActionReferences(
//        {
//           @ActionReference(path = "Menu/Edit", position = 50012),
//        })
public final class PrintAllProperties implements ActionListener
{

    private static final Logger LOGGER = Logger.getLogger(PrintAllProperties.class.getSimpleName());

    @Override
    public void actionPerformed(ActionEvent ae)
    {
        LOGGER.log(Level.INFO, "PrintAllProperties()");   
        Properties p = System.getProperties();
        Enumeration<?> keys = p.keys();
        while (keys.hasMoreElements())
        {
            String key = (String) keys.nextElement();
            String value = (String) p.get(key);
            LOGGER.log(Level.SEVERE, "{0}: {1}", new Object[]{key, value});   
        }
    }
}

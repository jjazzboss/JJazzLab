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
package org.jjazz.test;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

/**
 * For debug purposes...
 */
//@ActionID(category = "JJazz", id = "org.jjazz.test.mytestaction")
//@ActionRegistration(displayName = "MyTestAction")
//@ActionReferences(
//        {
//            @ActionReference(path = "Menu/Edit", position = 870012),
//            @ActionReference(path = "Shortcuts", name = "D-T")      // ctrl T
//        })
public final class MyTestAction implements ActionListener
{

    private static final Logger LOGGER = Logger.getLogger(MyTestAction.class.getSimpleName());

    public MyTestAction()
    {
    }

    @Override
    public void actionPerformed(ActionEvent ae)
    {
        LOGGER.severe("actionPerformed()");
        int a = 10;
        assert a != 10 : " comp=" + comp();

    }

    private int comp()
    {
        LOGGER.severe("comp() called");
        return 2938;
    }
}

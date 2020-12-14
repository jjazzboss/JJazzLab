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
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.database.api.RhythmDatabase;
import org.jjazz.rhythm.database.api.RhythmInfo;
import org.jjazz.rhythm.spi.RhythmProvider;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

/**
 * For debug purposes...
 */
//@ActionID(category = "JJazz", id = "org.jjazz.test.dumpRdbAction")
//@ActionRegistration(displayName = "Dump rhythms DB")
//@ActionReferences(
//        {
//            @ActionReference(path = "Menu/Edit", position = 50100),
//        })
public final class DumpRdbAction implements ActionListener
{

    private static final Logger LOGGER = Logger.getLogger(DumpRdbAction.class.getSimpleName());

    @Override
    public void actionPerformed(ActionEvent ae)
    {
        RhythmDatabase rdb = RhythmDatabase.getDefault();
        LOGGER.severe("Dumping rhythm database ----------");   //NOI18N
        for (RhythmProvider rp : rdb.getRhythmProviders())
        {
            LOGGER.severe("\n===== RhythmProvider = " + rp.getInfo().getName() + '@' + Integer.toHexString(rp.hashCode()));   //NOI18N
            for (RhythmInfo ri : rdb.getRhythms(rp))
            {
                LOGGER.severe("  " + ri);   //NOI18N
            }

        }
    }
}

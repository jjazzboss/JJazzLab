/*
 * 
 *   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 *   Copyright @2019 Jerome Lelasseux. All rights reserved.
 * 
 *   This file is part of the JJazzLab software.
 *    
 *   JJazzLab is free software: you can redistribute it and/or modify
 *   it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *   as published by the Free Software Foundation, either version 3 of the License, 
 *   or (at your option) any later version.
 * 
 *   JJazzLab is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *  
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *  
 *   Contributor(s): 
 * 
 */
package org.jjazz.proswing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.DoubleSummaryStatistics;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.proswing.walkingbass.db.WbpSourceDatabase;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "Edit",
        id = "org.jjazz.proswing.WdbAction"
)
@ActionRegistration(
        displayName = "#CTL_WdbAction"
)
@ActionReference(path = "Menu/Edit", position = 60100)
@Messages("CTL_WdbAction=WdbAction")
public final class WdbAction implements ActionListener
{

    protected static final Logger LOGGER = Logger.getLogger(WdbAction.class.getSimpleName());

    @Override public void actionPerformed(ActionEvent e)
    {
        LOGGER.severe("WdbAction started");
        var db = WbpSourceDatabase.getInstance();

        // Compute stats about offset from beat 0 of the first note of each bar
        DoubleSummaryStatistics stats = new DoubleSummaryStatistics();
        for (var wbpSource : db.getWbpSources(1))
        {
            var sp = wbpSource.getSizedPhrase();
            float shift0 = wbpSource.getFirstNoteBeatShift();
            if (shift0 < 0
                    || (!sp.isEmpty() && (shift0 = sp.first().getPositionInBeats()) < 0.25f))
            {
                stats.accept(shift0);
            }
        }
        LOGGER.log(Level.SEVERE, "WdbAction stats={0}", stats);
    }
}

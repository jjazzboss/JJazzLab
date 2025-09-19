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
package org.jjazz.jjswing.test;

import com.google.common.math.Quantiles;
import com.google.common.math.Stats;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.jjswing.bass.db.WbpSourceDatabase;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "Edit",
        id = "org.jjazz.jjswing.WbpSourceDbStats"
)
@ActionRegistration(
        displayName = "#CTL_WbpSourceDbStats"
)
@ActionReference(path = "Menu/Edit", position = 60100)
@Messages("CTL_WbpSourceDbStats=WbpSourceDatabase stats")
public final class WbpSourceDbStats implements ActionListener
{

    protected static final Logger LOGGER = Logger.getLogger(WbpSourceDbStats.class.getSimpleName());

    @Override public void actionPerformed(ActionEvent e)
    {
        LOGGER.severe("WbpSourceDbStats started");
        var db = WbpSourceDatabase.getInstance();

        // Compute stats about offset from beat 0 of the first note of each bar
        DoubleSummaryStatistics stats = new DoubleSummaryStatistics();

        List<Integer> velocities = new ArrayList<>();
        for (var wbpSource : db.getWbpSources(1))
        {
            var sp = wbpSource.getSizedPhrase();
            float shift0 = wbpSource.getFirstNoteBeatShift();
            if (shift0 < 0
                    || (!sp.isEmpty() && (shift0 = sp.first().getPositionInBeats()) < 0.25f))
            {
                stats.accept(shift0);
            }

            velocities.addAll(sp.stream()
                    .filter(ne -> ne.getDurationInBeats() > 0.6f)
                    .map(ne -> ne.getVelocity()).toList());
        }
        LOGGER.log(Level.SEVERE, "WdbAction stats={0}", stats);
        Stats statsVelocity = Stats.of(velocities);
        
        // Results june 2025: velocity min=35 max=92 mean=62,742 median=62 stdDev=7.63
        LOGGER.log(Level.SEVERE, "WdbAction velocity min={0} max={1} mean={2} median={3} stdDev={4}", new Object[]
        {
            statsVelocity.min(), statsVelocity.max(), statsVelocity.mean(), Quantiles.median().compute(velocities), statsVelocity.populationStandardDeviation()
        });

    }
}

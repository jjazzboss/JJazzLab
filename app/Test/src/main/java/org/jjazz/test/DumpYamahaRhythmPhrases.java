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
package org.jjazz.test;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.rhythm.api.rhythmparameters.RP_STD_Variation;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;
import org.jjazz.yamjjazz.Style;
import org.jjazz.yamjjazz.rhythm.YamJJazzRhythm;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.yamjjazz.StylePart;

//@ActionID(category = "Test", id = "org.jjazz.test.dumpyamaharhythmphrases")
//@ActionRegistration(displayName = "#CTL_DumpYamahaRhythmPhrases", lazy = true)
//@ActionReferences(
//        {
//            @ActionReference(path = "Actions/SongPart", position = 680)      // Add to popupmenu
//        })
//@NbBundle.Messages(
//        {
//            "CTL_DumpYamahaRhythmPhrases=[DEBUG] Dump Yamaha Rhythm Phrases"
//        })

public class DumpYamahaRhythmPhrases implements ActionListener
{

    private SongPart context;
    private static final Logger LOGGER = Logger.getLogger(DumpYamahaRhythmPhrases.class.getSimpleName());

    public DumpYamahaRhythmPhrases(SongPart context)
    {
        this.context = context;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        if (context.getRhythm() instanceof YamJJazzRhythm)
        {
            YamJJazzRhythm yr = (YamJJazzRhythm) context.getRhythm();
            String spStr = context.getRPValue(RP_STD_Variation.getVariationRp(yr));
            StylePart sp = yr.getStylePart(spStr);
            LOGGER.log(Level.INFO, "r={0} sp={1}", new Object[]
            {
                yr, sp
            });
            sp.dump(true, false);
        }
    }
}

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
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Variation;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.yamjjazz.rhythm.api.StylePart;
import org.jjazz.yamjjazz.rhythm.api.YamJJazzRhythm;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

@ActionID(category = "Test", id = "org.jjazz.test.dumpyamaharhythm")
@ActionRegistration(displayName = "#CTL_DumpYamahaRhythm", lazy = true)
@ActionReferences(
        {
            // @ActionReference(path = "Actions/SongPart", position = 682),      // Add to popupmenu
            @ActionReference(path = "Shortcuts", name = "DS-R")           // ctrl-shift R     
        })
@NbBundle.Messages(
        {
           "CTL_DumpYamahaRhythm=[DEBUG] Dump Yamaha Rhythm CASM"
        })
public class DumpYamahaRhythm implements ActionListener
{
   private SongPart context;
   private static final Logger LOGGER = Logger.getLogger(DumpYamahaRhythm.class.getSimpleName());

   public DumpYamahaRhythm(SongPart context)
   {
      this.context = context;
   }

   @Override
   public void actionPerformed(ActionEvent e)
   {
      if (context.getRhythm() instanceof YamJJazzRhythm)
      {
         YamJJazzRhythm yr = (YamJJazzRhythm) context.getRhythm();
         String spStr = context.getRPValue(RP_SYS_Variation.getVariationRp(yr));
         StylePart sp=yr.getStylePart(spStr);
         LOGGER.log(Level.INFO, "r={0} sp={1}", new Object[]{yr, sp});
         sp.dump(false, true);
      } 
   }
}

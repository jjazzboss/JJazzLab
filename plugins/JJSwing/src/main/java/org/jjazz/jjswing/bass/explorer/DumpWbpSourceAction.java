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
package org.jjazz.jjswing.bass.explorer;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import org.jjazz.jjswing.bass.WbpSourceAdaptation;

/**
 *
 * @author Jerome
 */
class DumpWbpSourceAction extends AbstractAction
{

    WbpDatabaseExplorerDialog dialog;
    private static final Logger LOGGER = Logger.getLogger(DumpWbpSourceAction.class.getSimpleName());

    public DumpWbpSourceAction(WbpDatabaseExplorerDialog dlg)
    {
        super("Dump WbpSources");
        this.dialog = dlg;
    }

    @Override
    public void actionPerformed(ActionEvent ae)
    {
        LOGGER.info("\n### DumpWbpSources");

        List<WbpSourceAdaptation> wbpsas = dialog.getSelectedWbpSourceAdaptations();

        for (var wbpsa : wbpsas)
        {
            var ws = wbpsa.getWbpSource();
            LOGGER.log(Level.INFO, "{0}", new Object[]
            {
                ws.toLongString()
            });
        }
    }

}

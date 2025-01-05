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
package org.jjazz.test.walkingbass.explorer;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import org.jjazz.test.walkingbass.WbpSource;
import org.jjazz.test.walkingbass.generator.WbpSourceAdaptation;
import org.jjazz.utilities.api.Utilities;

/**
 *
 * @author Jerome
 */
class PrintPhrasesAction extends AbstractAction
{

    WbpDatabaseExplorerDialog dialog;
    private static final Logger LOGGER = Logger.getLogger(PrintPhrasesAction.class.getSimpleName());

    public PrintPhrasesAction(WbpDatabaseExplorerDialog dlg)
    {
        super("Print phrases");
        this.dialog = dlg;
    }

    @Override
    public void actionPerformed(ActionEvent ae)
    {
        LOGGER.info("\n### Print phrases:");
        List<WbpSourceAdaptation> wbpsas = dialog.getSelectedWbpSourceAdaptations();
        for (var wbpsa : wbpsas)
        {
            var wbps = wbpsa.getWbpSource();
            var sp = wbps.getSizedPhrase();
            LOGGER.info(wbps.getId() + ": " + sp);
            LOGGER.info(Utilities.toMultilineString(sp, "  "));
        }
    }

}

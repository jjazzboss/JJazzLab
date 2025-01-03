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
import javax.swing.AbstractAction;
import org.jjazz.quantizer.api.Quantization;
import java.util.List;
import java.util.logging.Logger;
import org.jjazz.test.walkingbass.WbpSource;
import org.jjazz.test.walkingbass.WbpSources;
import org.jjazz.utilities.api.Utilities;

/**
 *
 * @author Jerome
 */
class FindDuplicatesAction extends AbstractAction
{

    WbpDatabaseExplorerDialog dialog;
    private static final Logger LOGGER = Logger.getLogger(FindDuplicatesAction.class.getSimpleName());

    public FindDuplicatesAction(WbpDatabaseExplorerDialog dlg)
    {
        super("Find duplicate phrases");
        this.dialog = dlg;
    }

    @Override
    public void actionPerformed(ActionEvent ae)
    {
        LOGGER.info("\n### Find duplicates:");
        List<WbpSource> wbpSources = dialog.getSelectedWbpSources();
        List<List<WbpSource>> res = WbpSources.findDuplicates(wbpSources, Quantization.ONE_THIRD_BEAT);
        res.forEach(wbpsList -> LOGGER.info(Utilities.toMultilineString(wbpsList)));
    }

}

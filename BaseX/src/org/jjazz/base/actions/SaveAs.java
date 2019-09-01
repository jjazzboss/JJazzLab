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
package org.jjazz.base.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;
import static org.jjazz.base.actions.Bundle.*;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.awt.StatusDisplayer;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "File", id = "org.jjazz.base.actions.SaveAs"
)
@ActionRegistration(displayName = "#CTL_SaveAs")
@ActionReference(path = "Menu/File", position = 1560)
@Messages(
        {
            "CTL_SaveAs=Save As...",
            "CTL_SavedFile=Saved"
        })
public final class SaveAs implements ActionListener
{

    private final SaveAsCapable context;
    private static final Logger LOGGER = Logger.getLogger(SaveAs.class.getSimpleName());

    public SaveAs(SaveAsCapable context)
    {
        this.context = context;
    }

    @Override
    public void actionPerformed(ActionEvent ev)
    {
        if (context.SaveAs() != 0)
        {
            StatusDisplayer.getDefault().setStatusText(CTL_SavedFile() + " " + context.toString());
        }
    }
}

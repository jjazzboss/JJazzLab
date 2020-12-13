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
import java.util.ResourceBundle;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jjazz.base.actions.Savable;
import org.jjazz.util.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.awt.StatusDisplayer;

@ActionID(category = "File", id = "org.jjazz.base.actions.SaveAll")

// Can't use lazy=true because otherwise then we can't listen to the Savable.ToBeSavedList
// So iconBase in registration is ignored
// @ActionRegistration(displayName = "#CTL_SaveAll", lazy = false, iconBase = "org/jjazz/base/actions/resources/saveAll.gif")
@ActionRegistration(displayName = "blabla", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Menu/File", position = 1540),
            @ActionReference(path = "Toolbars/File", position = 20),
            @ActionReference(path = "Shortcuts", name = "D-S")
        })
public class SaveAll extends AbstractAction implements ChangeListener
{

    private static final Logger LOGGER = Logger.getLogger(SaveAll.class.getSimpleName());
     
    public SaveAll()
    {
        putValue(NAME, ResUtil.getString(getClass(), "CTL_SaveAll"));


        // Need this for auto icon size changing to work... (switch to saveAll24.gif) since can't be done using actionRegistration's iconBase=xx
        putValue("iconBase", "org/jjazz/base/actions/resources/saveAll.gif");

        Savable.ToBeSavedList.addListener(this);
        setEnabled(false);
    }

    @Override
    public void actionPerformed(ActionEvent ev)
    {
        int count = 0;
        String name1 = null, name2 = null;
        for (Savable savable : Savable.ToBeSavedList.getSavables())
        {
            savable.save();
            if (count == 0)
            {
                name1 = savable.toString();
            } else if (count == 1)
            {
                name2 = savable.toString();
            }
            count++;
        }
        if (count == 0)
        {
            return;
        }
        String msg;
        switch (count)
        {
            case 1:
                msg = name1;
                break;
            case 2:
                msg = name1 + ", " + name2;
                break;
            default:
                msg = String.valueOf(count);
                break;
        }
        StatusDisplayer.getDefault().setStatusText(ResUtil.getString(getClass(), "CTL_FilesSaved", msg));
    }

    @Override
    public void stateChanged(ChangeEvent e)
    {
        setEnabled(!Savable.ToBeSavedList.getSavables().isEmpty());
    }
}

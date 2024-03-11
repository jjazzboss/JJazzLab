/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 *  This file is part of the JJazzLab software.
 *   
 *  JJazzLab is free software: you can redistribute it and/or modify
 *  it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *  as published by the Free Software Foundation, either version 3 of the License, 
 *  or (at your option) any later version.
 *
 *  JJazzLab is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 * 
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 * 
 *  Contributor(s): 
 */
package org.jjazz.songeditormanager;

import java.awt.event.ActionEvent;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import org.jjazz.song.api.Song;
import org.jjazz.utilities.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.awt.StatusDisplayer;

@ActionID(
        category = "File", id = "org.jjazz.songeditormanager.SaveAs"
)
@ActionRegistration(displayName = "#CTL_SaveAs")
@ActionReference(path = "Menu/File", position = 1560)
public final class SaveAs extends AbstractAction
{

    private final Song song;
    private static final Logger LOGGER = Logger.getLogger(SaveAs.class.getSimpleName());

    public SaveAs(Song context)
    {
        this.song = context;
//        Icon icon = SystemAction.get(SaveAction.class).getIcon();
//        putValue(SMALL_ICON, icon);

    }

    @Override
    public void actionPerformed(ActionEvent ev)
    {
        int res = SaveUtils.SaveAs(song);
        if (res == 0)
        {
            StatusDisplayer.getDefault().setStatusText(ResUtil.getString(getClass(), "CTL_Saved", song.toString()));
        }
    }
}

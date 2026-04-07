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
package org.jjazz.musiccontrolactions;

import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.jjazz.musiccontrolactions.api.RemoteAction;
import org.jjazz.musiccontrolactions.spi.RemoteActionProvider;
import org.jjazz.utilities.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionRegistration;
import org.openide.util.lookup.ServiceProvider;

/**
 * Try to set variation C on selected song part, or first song part if no selection.
 * <p>
 * This is only used as a remote action, typically in pseudo-arranger mode.
 */
@ActionID(category = "MusicControls", id = "org.jjazz.musiccontrolactions.setvariationd")
@ActionRegistration(displayName = "#CTL_SetVariationD", lazy = false)
public class SetVariationD extends AbstractAction
{

    private static final String UNDO_TEXT = ResUtil.getString(SetVariationD.class, "CTL_SetVariationD");
    private static final Logger LOGGER = Logger.getLogger(SetVariationD.class.getSimpleName());

    public SetVariationD()
    {
        putValue(Action.NAME, UNDO_TEXT);
        putValue(Action.SHORT_DESCRIPTION, ResUtil.getString(getClass(), "DESC_SetVariationD"));
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        SetVariationA.setVariation("Main D");
    }


    // ======================================================================
    // Private methods
    // ======================================================================   
    // ======================================================================
    // Inner classes
    // ======================================================================   
    @ServiceProvider(service = RemoteActionProvider.class)
    public static class SetVariationDRemoteActionProvider implements RemoteActionProvider
    {

        @Override
        public List<RemoteAction> getRemoteActions()
        {
            RemoteAction ra = RemoteAction.loadFromPreference("MusicControls", "org.jjazz.musiccontrolactions.setvariationd");
            if (ra == null)
            {
                ra = new RemoteAction("MusicControls", "org.jjazz.musiccontrolactions.setvariationd");
                ra.setMidiMessages(RemoteAction.noteOnMidiMessages(0, 19));
            }
            ra.setDefaultMidiMessages(RemoteAction.noteOnMidiMessages(0, 19));
            return Arrays.asList(ra);
        }
    }
}

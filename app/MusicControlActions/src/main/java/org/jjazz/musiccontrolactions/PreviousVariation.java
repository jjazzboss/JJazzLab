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
 * Use the previous variation on selected song part, or first song part if no selection.
 * <p>
 * This is only used as a remote action, typically in pseudo-arranger mode.
 */
@ActionID(category = "MusicControls", id = "org.jjazz.musiccontrolactions.previousvariation")
@ActionRegistration(displayName = "#CTL_PreviousVariation", lazy=false)
public class PreviousVariation extends AbstractAction
{
    private static final String UNDO_TEXT = ResUtil.getString(PreviousVariation.class, "CTL_PreviousVariation");
    private static final Logger LOGGER = Logger.getLogger(PreviousVariation.class.getSimpleName());

    public PreviousVariation()
    {
        putValue(Action.NAME, UNDO_TEXT);
        putValue(Action.SHORT_DESCRIPTION, ResUtil.getString(getClass(), "DESC_PreviousVariation"));
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        NextVariation.nextOrPrevousVariation(false);
    }


    // ======================================================================
    // Private methods
    // ======================================================================   
    // ======================================================================
    // Inner classes
    // ======================================================================   
    @ServiceProvider(service = RemoteActionProvider.class)
    public static class PreviousVariationRemoteActionProvider implements RemoteActionProvider
    {

        @Override
        public List<RemoteAction> getRemoteActions()
        {
            RemoteAction ra = RemoteAction.loadFromPreference("MusicControls", "org.jjazz.musiccontrolactions.previousvariation");
            if (ra == null)
            {
                ra = new RemoteAction("MusicControls", "org.jjazz.musiccontrolactions.previousvariation");
                ra.setMidiMessages(RemoteAction.noteOnMidiMessages(0, 21));
            }
            ra.setDefaultMidiMessages(RemoteAction.noteOnMidiMessages(0, 21));
            return Arrays.asList(ra);
        }
    }
}

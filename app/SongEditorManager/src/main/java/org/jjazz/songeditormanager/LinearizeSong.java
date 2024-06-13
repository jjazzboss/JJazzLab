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
import java.awt.event.ActionListener;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import org.jjazz.analytics.api.Analytics;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongUtilities;
import org.jjazz.songeditormanager.spi.SongEditorManager;
import org.jjazz.utilities.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbPreferences;

/**
 * Get a new song with the leadsheet developped according to the song structure so that the leadsheet becomes linear.
 */
@ActionID(category = "JJazz", id = "org.jjazz.cl_editor.actions.linearizesong")
@ActionRegistration(displayName = "#CTL_LinearizeSong", lazy = true)
@ActionReferences(
        {
            @ActionReference(path = "Menu/Edit", position = 2130)
        })
public class LinearizeSong implements ActionListener
{

    private static final String PREF_SHOW_TIP = "ShowTipLinearizeSong";
    private final Song song;
    private static Preferences prefs = NbPreferences.forModule(LinearizeSong.class);
    private static final Logger LOGGER = Logger.getLogger(LinearizeSong.class.getSimpleName());

    public LinearizeSong(Song context)
    {
        this.song = context;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        assert song != null;

        if (prefs.getBoolean(PREF_SHOW_TIP, true))
        {
            String text = ResUtil.getString(getClass(), "LinearizeSongTip");
            prefs.putBoolean(PREF_SHOW_TIP, false);
        }

        Song newSong = SongUtilities.getLinearizedSong(song, true);
        newSong.setName(song.getName() + "-linearized");
        SongEditorManager.getDefault().showSong(newSong, true, false);     // This will post a task on the EDT to display the song        
        Analytics.logEvent("Linearize song");
    }

}

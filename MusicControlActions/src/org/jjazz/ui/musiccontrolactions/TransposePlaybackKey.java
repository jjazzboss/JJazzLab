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
package org.jjazz.ui.musiccontrolactions;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import org.jjazz.analytics.api.Analytics;
import org.jjazz.musiccontrol.api.PlaybackSettings;
import org.jjazz.util.api.ResUtil;
import org.netbeans.api.annotations.common.StaticResource;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.windows.WindowManager;

/**
 * Transpose the leadsheet for playback.
 */
@ActionID(category = "MusicControls", id = "org.jjazz.ui.musiccontrolactions.transposeplaybackkey")
@ActionRegistration(displayName = "#CTL_TransposePlaybackKey", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/ExtendedToolbar", position = 10)    
        })
public class TransposePlaybackKey extends AbstractAction implements PropertyChangeListener
{

    @StaticResource(relative = true)
    private static final String OFF_ICON = "resources/Sax-OFF-24x24.png";
    @StaticResource(relative = true)
    private static final String ON_ICON = "resources/Sax-ON-24x24.png";
    private static final Logger LOGGER = Logger.getLogger(TransposePlaybackKey.class.getSimpleName());

    public TransposePlaybackKey()
    {
        putValue("hideActionText", true);
        updateButtonUI();

        PlaybackSettings.getInstance().addPropertyChangeListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        var ps = PlaybackSettings.getInstance();
        var dlg = TransposePlaybackKeyDialog.getInstance();


        dlg.preset(ps.getPlaybackKeyTransposition());
        dlg.setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
        dlg.setVisible(true);


        if (dlg.isExitOk())
        {
            int old = ps.getPlaybackKeyTransposition();
            ps.setPlaybackKeyTransposition(dlg.getPlaybackKeyTransposition());


            Analytics.setProperties(Analytics.buildMap("Playback Key Transpose", dlg.getPlaybackKeyTransposition()));

        }
    }

    // ======================================================================
    // PropertyChangeListener interface
    // ======================================================================    
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        var mc = PlaybackSettings.getInstance();
        if (evt.getSource() == mc)
        {
            if (evt.getPropertyName().equals(PlaybackSettings.PROP_PLAYBACK_KEY_TRANSPOSITION))
            {
                updateButtonUI();
            }
        }
    }

    // ======================================================================
    // Private methods
    // ======================================================================   
    private void updateButtonUI()
    {
        int t = PlaybackSettings.getInstance().getPlaybackKeyTransposition();


        String iconPath = t == 0 ? OFF_ICON : ON_ICON;
        putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource(iconPath)));


        String s = ResUtil.getString(getClass(), "CTL_CurrentPlaybackTransposition", t);
        putValue(Action.SHORT_DESCRIPTION, s);

    }

}

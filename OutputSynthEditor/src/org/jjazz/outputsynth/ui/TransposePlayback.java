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
package org.jjazz.outputsynth.ui;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import org.jjazz.activesong.ActiveSongManager;
import org.jjazz.musiccontrol.MusicController;
import org.jjazz.song.api.Song;
import org.netbeans.api.annotations.common.StaticResource;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;
import org.openide.windows.WindowManager;

/**
 * Transpose the leadsheet for playback.
 */
@ActionID(category = "MusicControls", id = "org.jjazz.ui.musiccontrolactions.transposeplayback")
@ActionRegistration(displayName = "Transpose playback", lazy = false)
public class TransposePlayback extends AbstractAction implements PropertyChangeListener
{

    @StaticResource(relative = true)
    private static final String OFF_ICON = "resources/Sax-OFF-24x24.png";
    @StaticResource(relative = true)
    private static final String ON_ICON = "resources/Sax-ON-24x24.png";

    private static final Logger LOGGER = Logger.getLogger(TransposePlayback.class.getSimpleName());

    public TransposePlayback()
    {
        putValue("hideActionText", true);
        updateButtonUI();

        MusicController.getInstance().addPropertyChangeListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        var mc = MusicController.getInstance();
        var dlg = TransposePlaybackDialog.getInstance();
        dlg.preset(mc.getPlaybackLeadSheetTransposition());
        dlg.setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
        dlg.setVisible(true);


        if (dlg.isExitOk())
        {
            int old = mc.getPlaybackLeadSheetTransposition();
            mc.setPlaybackLeadSheetTransposition(dlg.getPlaybackTransposition());
            if (old != dlg.getPlaybackTransposition() && mc.getState().equals(MusicController.State.PLAYING))
            {
                String msg = "Change will take place when current song play back is over.";
                NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.INFORMATION_MESSAGE);
                DialogDisplayer.getDefault().notify(d);
            }
        }
    }

    // ======================================================================
    // PropertyChangeListener interface
    // ======================================================================    
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        var mc = MusicController.getInstance();
        if (evt.getSource() == mc)
        {
            if (evt.getPropertyName() == MusicController.PROP_PLAYBACK_TRANSPOSITION)
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
        int t = MusicController.getInstance().getPlaybackLeadSheetTransposition();


        String iconPath = t == 0 ? OFF_ICON : ON_ICON;
        putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource(iconPath)));


        String s = "Current playback transposition: " + (t == 0 ? "0" : "+" + t);
        putValue(Action.SHORT_DESCRIPTION, s);

    }

}

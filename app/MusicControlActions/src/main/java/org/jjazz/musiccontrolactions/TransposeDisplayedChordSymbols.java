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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import org.jjazz.analytics.api.Analytics;
import org.jjazz.musiccontrol.api.PlaybackSettings;
import org.jjazz.utilities.api.ResUtil;
import org.netbeans.api.annotations.common.StaticResource;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.windows.WindowManager;

/**
 * Transpose the leadsheet and all other music shown in the UI.
 */
@ActionID(category = "MusicControls", id = "org.jjazz.musiccontrolactions.transposedisplay")
@ActionRegistration(displayName = "#CTL_TransposeDisplay", lazy = false)
@ActionReferences(
    {
        @ActionReference(path = "Actions/ExtendedToolbar", position = 10)
    })
public class TransposeDisplayedChordSymbols extends AbstractAction implements PropertyChangeListener
{

    @StaticResource(relative = true)
    private static final String OFF_ICON = "resources/Sax-OFF-24x24.png";
    @StaticResource(relative = true)
    private static final String ON_ICON = "resources/Sax-ON-24x24.png";
    private static final Logger LOGGER = Logger.getLogger(TransposeDisplayedChordSymbols.class.getSimpleName());

    public TransposeDisplayedChordSymbols()
    {
        putValue("hideActionText", true);
        updateButtonUI();

        PlaybackSettings.getInstance().addPropertyChangeListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        var ps = PlaybackSettings.getInstance();
        int tOld = ps.getChordSymbolsDisplayTransposition();
        
        
        var dlg = TransposeDisplayedChordSymbolsDialog.getInstance();
        dlg.setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
        dlg.setVisible(true);

        
        int tNew = ps.getChordSymbolsDisplayTransposition();
        if (tNew != 0 && tNew != tOld)
        {
            Analytics.setProperties(Analytics.buildMap("Chord symbol display transposition", tNew));
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
            if (evt.getPropertyName().equals(PlaybackSettings.PROP_CHORD_SYMBOLS_DISPLAY_TRANSPOSITION))
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
        int t = -PlaybackSettings.getInstance().getChordSymbolsDisplayTransposition();

        String iconPath = t == 0 ? OFF_ICON : ON_ICON;
        putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource(iconPath)));

        String s = ResUtil.getString(getClass(), "CTL_CurrentDisplayTransposition", t);
        putValue(Action.SHORT_DESCRIPTION, s);
    }
}

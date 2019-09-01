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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.ImageIcon;
import org.jjazz.musiccontrol.ClickManager;
import org.jjazz.ui.flatcomponents.FlatToggleButton;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.BooleanStateAction;

/**
 * Toggle click precount before playback.
 */
@ActionID(category = "MusicControls", id = "org.jjazz.ui.musiccontrolactions.precount")
@ActionRegistration(displayName = "#CTL_Precount", lazy = false)
@ActionReferences(
        {
            // 
        })
@NbBundle.Messages(
        {
            "CTL_Precount=Precount",
            "CTL_PrecountTooltip=Precount before playback"
        })
public class Precount extends BooleanStateAction implements PropertyChangeListener
{

    private static final Logger LOGGER = Logger.getLogger(Precount.class.getSimpleName());

    public Precount()
    {
        setBooleanState(false);

        putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("/org/jjazz/ui/musiccontrolactions/resources/precount-OFF-24x24.png")));
        putValue(Action.LARGE_ICON_KEY, new ImageIcon(getClass().getResource("/org/jjazz/ui/musiccontrolactions/resources/precount-ON-24x24.png")));
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_PrecountTooltip());
        putValue("hideActionText", true);

        ClickManager cm = ClickManager.getInstance();
        cm.addPropertyChangeListener(this);
        setSelected(cm.isClickPrecount());
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        setSelected(!getBooleanState());
    }

    public void setSelected(boolean b)
    {
        if (b == getBooleanState())
        {
            return;
        }
        ClickManager cm = ClickManager.getInstance();
        cm.setClickPrecount(b);
        setBooleanState(b);  // Notify action listeners
    }

    @Override
    public String getName()
    {
        return Bundle.CTL_Click();
    }

    @Override
    public HelpCtx getHelpCtx()
    {
        return HelpCtx.DEFAULT_HELP;
    }

    @Override
    public Component getToolbarPresenter()
    {
        return new FlatToggleButton(this);
    }

    // ======================================================================
    // PropertyChangeListener interface
    // ======================================================================    
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        ClickManager cm = ClickManager.getInstance();
        if (evt.getSource() == cm)
        {
            if (evt.getPropertyName() == ClickManager.PROP_CLICK_PRECOUNT)
            {
                setBooleanState((boolean) evt.getNewValue());
            }
        }
    }

    // ======================================================================
    // Private methods
    // ======================================================================    
}

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
package org.jjazz.ui.mixconsole.actions;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.ImageIcon;
import org.jjazz.midi.JJazzMidiSystem;
import org.jjazz.ui.flatcomponents.FlatToggleButton;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.BooleanStateAction;

/**
 * Mute all sounds.
 * <p>
 * Rely on the JJazzMidiSystem setMidiOutFilter().
 */
@ActionID(category = "MixConsole", id = "org.jjazz.ui.mixconsole.actions.mastermuteall")
@ActionRegistration(displayName = "#CTL_MasterMuteAll", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/MixConsole/Master", position = 50)
        })
@NbBundle.Messages(
        {
            "CTL_MasterMuteAll=Master Mute",
            "CTL_MasterMuteAllTooltip=Mute all channels"
        })
public class MasterMuteAll extends BooleanStateAction implements PropertyChangeListener
{

    private static final Logger LOGGER = Logger.getLogger(MasterMuteAll.class.getSimpleName());

    public MasterMuteAll()
    {
        setBooleanState(false);

        putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("/org/jjazz/ui/mixconsole/resources/MuteOff_Icon-21x21.png")));
        putValue(Action.LARGE_ICON_KEY, new ImageIcon(getClass().getResource("/org/jjazz/ui/mixconsole/resources/MuteOn_Icon-21x21.png")));
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_MasterMuteAllTooltip());
        putValue("hideActionText", true);

        // Listen to the Midi Out filtering changes
        JJazzMidiSystem.getInstance().addPropertyChangeListener(this);

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
        JJazzMidiSystem jms = JJazzMidiSystem.getInstance();
        jms.setMidiOutFiltering(b);
        setBooleanState(b);
    }

    @Override
    public String getName()
    {
        return Bundle.CTL_MasterMuteAll();
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
        if (evt.getSource() == JJazzMidiSystem.getInstance())
        {
            if (evt.getPropertyName() == JJazzMidiSystem.PROP_MIDI_OUT_FILTERING)
            {
                boolean b = (boolean) evt.getNewValue();
                setSelected(b);
            }
        }
    }

    // ======================================================================
    // Private methods
    // ======================================================================   
}

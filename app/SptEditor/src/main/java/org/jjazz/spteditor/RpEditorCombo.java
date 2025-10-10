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
package org.jjazz.spteditor;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RP_State;
import org.jjazz.spteditor.api.RpEditor;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.spteditor.spi.RpEditorComponent;

/**
 * A RpEditor using a JComboBox for RP_State parameters.
 * <p>
 */
public class RpEditorCombo extends RpEditorComponent<String> implements ActionListener, PropertyChangeListener
{

    private final JComboBox<String> combo_rpValue;

    private static final Logger LOGGER = Logger.getLogger(RpEditorCombo.class.getSimpleName());

    public RpEditorCombo(SongPart spt, RP_State rp)
    {
        super(spt, rp);

        // Prepare our editor component
        combo_rpValue = new JComboBox<>();
        combo_rpValue.addActionListener(this);
        RP_State rpState = (RP_State) rp;
        ComboBoxModel<String> cbModel = new DefaultComboBoxModel<>(rpState.getPossibleValues().toArray(new String[0]));
        combo_rpValue.setModel(cbModel);
        combo_rpValue.setSelectedItem(songPart.getRPValue(rpState));


        // Add it
        setLayout(new BorderLayout());
        add(combo_rpValue);

        
        // Listen to rhythm resource load event, as it might impact our tooltip
        spt.getRhythm().addPropertyChangeListener(this);
    }

    @Override
    public void updateEditorValue(String value)
    {
        if (value != null && !value.equals(getEditorValue()))
        {
            combo_rpValue.removeActionListener(this);
            combo_rpValue.setSelectedItem(value);
            combo_rpValue.addActionListener(this);
        }
    }

    @Override
    public void showMultiValueMode(boolean b)
    {
        RpEditor.showMultiModeUsingFont(b, combo_rpValue);
    }

    @Override
    public String getEditorValue()
    {
        return combo_rpValue.getModel().getElementAt(combo_rpValue.getSelectedIndex());
    }

    @Override
    public void cleanup()
    {
        super.cleanup();
        songPart.getRhythm().removePropertyChangeListener(this);
    }

    // ==============================================================================
    // PropertyChangeListener interface
    // ==============================================================================
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getSource() == songPart.getRhythm())
        {
            if (evt.getPropertyName().equals(Rhythm.PROP_RESOURCES_LOADED))
            {
                updateToolTip();
            }
        }
    }

    // -----------------------------------------------------------------------------
    // ActionListener interface
    // -----------------------------------------------------------------------------
    @Override
    public void actionPerformed(ActionEvent e)
    {
        Object newValue = combo_rpValue.getSelectedItem();
        updateToolTip();
        firePropertyChange(RpEditor.PROP_RP_VALUE, null, newValue);
    }

    // -----------------------------------------------------------------------------
    // Private methods
    // -----------------------------------------------------------------------------
    private void updateToolTip()
    {
        String newValue = combo_rpValue.getModel().getElementAt(combo_rpValue.getSelectedIndex());
        String valueDesc = rp.getValueDescription(newValue);
        combo_rpValue.setToolTipText(valueDesc == null ? newValue : valueDesc);
    }
}

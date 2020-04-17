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
package org.jjazz.ui.spteditor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import org.jjazz.rhythm.parameters.RP_State;
import org.jjazz.rhythm.parameters.RhythmParameter;
import org.jjazz.ui.spteditor.api.RpEditor;
import org.jjazz.songstructure.api.SongPart;

/**
 * A RpEditor using a JComboBox for RP_State parameters.
 * <p>
 */
public class RpEditorCombo extends RpEditor implements ActionListener
{
    
    private final JComboBox combo_rpValue;
    private static final Logger LOGGER = Logger.getLogger(RpEditorCombo.class.getSimpleName());
    
    public RpEditorCombo(SongPart spt, RhythmParameter<?> rp)
    {
        super(spt, rp);
        
        if (!(rp instanceof RP_State))
        {
            throw new IllegalArgumentException("RhythmParameter type not supported for this editor. rp=" + rp);
        }

        // Prepare our editor component
        combo_rpValue = new JComboBox<String>();
        combo_rpValue.addActionListener(this);
        RhythmParameter<?> rpModel = getRpModel();
        SongPart sptModel = getSptModel();        
        ComboBoxModel<String> cbModel = new DefaultComboBoxModel<>(rpModel.getPossibleValues().toArray(new String[0]));
        combo_rpValue.setModel(cbModel);
        combo_rpValue.setSelectedItem(sptModel.getRPValue(rpModel));
        setEditor(combo_rpValue);
    }
    
    @Override
    protected JComponent getEditor()
    {
        return combo_rpValue;
    }

    /**
     * Update the value in the editor.
     *
     * @param value
     * @param firePropChangeEvent If false don't fire a change event.
     */
    @Override
    public void setRpValue(Object value, boolean firePropChangeEvent)
    {
        if (value != null && !value.equals(getRpValue()))
        {
            if (!firePropChangeEvent)
            {
                combo_rpValue.removeActionListener(this);
            }
            combo_rpValue.setSelectedItem(value);
            if (!firePropChangeEvent)
            {
                combo_rpValue.addActionListener(this);
            }
        }
    }
    
    @Override
    protected void showMultiValueMode(boolean b)
    {
        showMultiModeUsingFont(isMultiValueMode(), combo_rpValue);
    }
    
    @Override
    public Object getRpValue()
    {
        return combo_rpValue.getSelectedItem();
    }
    
    @Override
    public void cleanup()
    {
        combo_rpValue.removeActionListener(this);
    }

    // -----------------------------------------------------------------------------
    // ActionListener interface
    // -----------------------------------------------------------------------------
    @Override
    public void actionPerformed(ActionEvent e)
    {
        Object newValue = combo_rpValue.getSelectedItem();
        combo_rpValue.setToolTipText(newValue.toString());
        firePropertyChange(PROP_RPVALUE, null, newValue);
    }
}

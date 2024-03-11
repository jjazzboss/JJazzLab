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
import java.util.logging.Logger;
import javax.swing.JTextField;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jjazz.rhythm.api.RP_Integer;
import org.jjazz.spteditor.api.RpEditor;
import org.jjazz.flatcomponents.api.WheelSpinner;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.spteditor.spi.RpEditorComponent;

/**
 * A RpEditor using a JSpinner for RP_Integer.
 * <p>
 */
public class RpEditorSpinner extends RpEditorComponent<Integer> implements ChangeListener
{

    private final WheelSpinner spinner_rpValue;
    private static final Logger LOGGER = Logger.getLogger(RpEditorSpinner.class.getSimpleName());

    public RpEditorSpinner(SongPart spt, RP_Integer rp)
    {
        super(spt, rp);

        
        spinner_rpValue = new WheelSpinner();
        spinner_rpValue.addChangeListener(this);
        SpinnerModel sm;
        int minValue = rp.getMinValue();
        int maxValue = rp.getMaxValue();
        int step = rp.getStep();
        int value = songPart.getRPValue(rp);
        sm = new MySpinnerNumberModel(value, minValue, maxValue, step);
        spinner_rpValue.setModel(sm);
        spinner_rpValue.getDefaultEditor().getTextField().setHorizontalAlignment(JTextField.TRAILING);                
        spinner_rpValue.setValue(songPart.getRPValue(rp));

        
        
        setLayout(new BorderLayout());
        add(spinner_rpValue);
    }

    @Override
    public void updateEditorValue(Integer value)
    {
        if (value != null && !value.equals(getEditorValue()))
        {
            spinner_rpValue.removeChangeListener(this);
            spinner_rpValue.setValue(value);
            spinner_rpValue.addChangeListener(this);
        }
    }

    @Override
    public void showMultiValueMode(boolean b)
    {
        RpEditor.showMultiModeUsingFont(b, spinner_rpValue.getDefaultEditor().getTextField());
    }

    @Override
    public Integer getEditorValue()
    {
        return (Integer)spinner_rpValue.getValue();
    }

    @Override
    public void cleanup()
    {
        super.cleanup();
        spinner_rpValue.removeChangeListener(this);
    }

    // -----------------------------------------------------------------------------
    // ChangeListener interface
    // -----------------------------------------------------------------------------
    @Override
    public void stateChanged(ChangeEvent e)
    {
        Integer newValue = (Integer) spinner_rpValue.getValue();
        String valueDesc = rp.getValueDescription(newValue);
        spinner_rpValue.setToolTipText(valueDesc == null ? newValue.toString() : valueDesc);
        firePropertyChange(RpEditor.PROP_RP_VALUE, null, newValue);
    }

    // -----------------------------------------------------------------------------
    // Private methods
    // -----------------------------------------------------------------------------    
    /**
     * Our model which enforces valid values.
     * <p>
     * Because SpinnerNumberModel setValue() does not enforce "correct" value.
     */
    private class MySpinnerNumberModel extends SpinnerNumberModel
    {

        public MySpinnerNumberModel(int value, int minValue, int maxValue, int step)
        {
            super(value, minValue, maxValue, step);
        }

        @Override
        public void setValue(Object val)
        {
            if (!(val instanceof Integer) || !rp.isValidValue((Integer) val))
            {
                throw new IllegalArgumentException(); // Will be catched by the JSpinner code to revert to previous value   
            }
            super.setValue(val);
        }
    }
}

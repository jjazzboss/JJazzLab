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

import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.SpinnerListModel;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jjazz.rhythm.parameters.RP_Integer;
import org.jjazz.rhythm.parameters.RP_State;
import org.jjazz.rhythm.parameters.RhythmParameter;
import org.jjazz.ui.spteditor.api.RpEditor;
import org.jjazz.ui.utilities.WheelSpinner;
import org.jjazz.songstructure.api.SongPart;

/**
 * A RpEditor using a JSpinner for RP_Integer and RP_State parameters.
 * <p>
 */
public class RpEditorSpinner extends RpEditor implements ChangeListener
{

    private final WheelSpinner spinner_rpValue;
    private static final Logger LOGGER = Logger.getLogger(RpEditorSpinner.class.getSimpleName());

    public RpEditorSpinner(SongPart spt, RhythmParameter<?> rp)
    {
        super(spt, rp);

        // Prepare our editor component
        spinner_rpValue = new WheelSpinner();
        spinner_rpValue.addChangeListener(this);

        SpinnerModel sm;
        RhythmParameter<?> rpModel = getRpModel();
        SongPart sptModel = getSptModel();
        if (rpModel instanceof RP_Integer)
        {
            RP_Integer rpi = (RP_Integer) rpModel;
            int minValue = rpi.getMinValue();
            int maxValue = rpi.getMaxValue();
            int step = rpi.getStep();
            int value = sptModel.getRPValue(rpi);
            sm = new MySpinnerNumberModel(value, minValue, maxValue, step);
        } else if (rpModel instanceof RP_State)
        {
            sm = new SpinnerListModel(rpModel.getPossibleValues());
        } else
        {
            throw new IllegalArgumentException("RhythmParameter type not supported for this editor. rp=" + rp);   //NOI18N
        }
        spinner_rpValue.setModel(sm);
        spinner_rpValue.setValue(sptModel.getRPValue(rpModel));
        spinner_rpValue.getDefaultEditor().getTextField().setHorizontalAlignment(JTextField.TRAILING);
        // ((DefaultEditor) spinner_rpValue.getEditor()).getTextField().setEditable(false);

        setEditor(spinner_rpValue);
    }

    @Override
    protected JComponent getEditor()
    {
        return spinner_rpValue;
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
                spinner_rpValue.removeChangeListener(this);
            }
            spinner_rpValue.setValue(value);
            if (!firePropChangeEvent)
            {
                spinner_rpValue.addChangeListener(this);
            }
        }
    }

    @Override
    protected void showMultiValueMode(boolean b)
    {
        showMultiModeUsingFont(isMultiValueMode(), spinner_rpValue.getDefaultEditor().getTextField());
    }

    @Override
    public Object getRpValue()
    {
        return spinner_rpValue.getValue();
    }

    @Override
    public void cleanup()
    {
        spinner_rpValue.removeChangeListener(this);
    }

    // -----------------------------------------------------------------------------
    // ChangeListener interface
    // -----------------------------------------------------------------------------
    @Override
    public void stateChanged(ChangeEvent e)
    {
        Object newValue = spinner_rpValue.getValue();
        @SuppressWarnings("rawtypes")
        RhythmParameter rp = getRpModel();      // Needed to get rid of the unbounded wildcard <?>
        @SuppressWarnings("unchecked")         
        String valueDesc = rp.getValueDescription(newValue);
        spinner_rpValue.setToolTipText(valueDesc == null ? newValue.toString() : valueDesc);
        firePropertyChange(PROP_RPVALUE, null, newValue);
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
            if (!(val instanceof Integer) || !((RP_Integer) getRpModel()).isValidValue((Integer) val))
            {
                throw new IllegalArgumentException(); // Will be catched by the JSpinner code to revert to previous value   //NOI18N
            }
            super.setValue(val);
        }
    }
}

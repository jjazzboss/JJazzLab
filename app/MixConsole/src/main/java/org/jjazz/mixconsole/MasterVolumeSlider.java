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
package org.jjazz.mixconsole;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.midi.api.JJazzMidiSystem;
import org.jjazz.flatcomponents.api.FlatIntegerHorizontalSlider;
import org.jjazz.utilities.api.ResUtil;

/**
 * An horizontal slider for master volume.
 * <p>
 * Connects directly to the JJazzMidiSystem model.
 */
public class MasterVolumeSlider extends FlatIntegerHorizontalSlider implements PropertyChangeListener
{

    private static final Logger LOGGER = Logger.getLogger(MasterVolumeSlider.class.getSimpleName());

    public MasterVolumeSlider()
    {
        setMinValue(0);
        setMaxValue(200);
        setValue(100);
        putClientProperty(PROP_HIDE_VALUE, 1);
        putClientProperty(PROP_WH_RATIO, Float.valueOf(6));
        putClientProperty(PROP_NB_GRADUATION_MARKS, 4);
        updateToolTipText();

        // Listen to ourself to get the value changes
        addPropertyChangeListener(this);

        // Listen to system master volume factor changes
        JJazzMidiSystem.getInstance().addPropertyChangeListener(this);
    }

    /**
     * Get the slider value as a volume factor.
     *
     * @return A float value between 0 and 2.
     */
    public float getValueAsVolumeFactor()
    {
        return getValue() / 100f;
    }

    /**
     * Set the slider value as a volume factor.
     *
     * @param f A float value between 0 and 2.
     */
    public void setValueAsVolumeFactor(float f)
    {
        if (f < 0 || f > 2)
        {
            throw new IllegalArgumentException("f=" + f);   
        }
        int v = Math.round(f * 100);
        setValue(v);
    }

    /**
     *
     * @return A user-readable string float value with 1 decimal.
     */
    public String getStringValueAsVolumeFactor()
    {
        String s = String.format("%.1f", getValueAsVolumeFactor());
        return s;
    }

    @Override
    protected void updateToolTipText()
    {
        this.setToolTipText(ResUtil.getString(getClass(), "CTL_MasterToolTip") + " x" + getStringValueAsVolumeFactor());
    }

    // ----------------------------------------------------------------------------
    // PropertyChangeListener interface
    // ----------------------------------------------------------------------------
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        LOGGER.log(Level.FINE, "propertyChange() -- evt={0}", evt);   
        if (evt.getSource() == this)
        {
            if (evt.getPropertyName() == PROP_VALUE)
            {
                JJazzMidiSystem.getInstance().setMasterVolumeFactor(getValueAsVolumeFactor());
            }
        } else if (evt.getSource() == JJazzMidiSystem.getInstance())
        {
            if (evt.getPropertyName() == JJazzMidiSystem.PROP_MASTER_VOL_FACTOR)
            {
                float f = (float) evt.getNewValue();
                setValueAsVolumeFactor(f);
            }
        } else
        {
            throw new IllegalStateException("evt=" + evt);   
        }
    }
}

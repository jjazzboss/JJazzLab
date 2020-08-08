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
package org.jjazz.ui.utilities;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;
import javax.swing.SpinnerListModel;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import org.openide.util.RequestProcessor;
import org.openide.util.Task;

/**
 * A JSpinner with mousewheel support and some convenience functions.
 * <p>
 * Supports only SpinnerListModel and SpinnerNumberModel.
 * <p>
 * This JSpinner consumes all the printable char key events. Use a normal JSpinner if you want the default behavior with key events being
 * processed by JSpinner AND forwarded to component hierarchy via the key binding framework.
 */
public class WheelSpinner extends JSpinner implements MouseWheelListener
{

    private int wheelStep;
    private int ctrlWheelStep;
    private int columns;
    private boolean loopValues;
    private static final Logger LOGGER = Logger.getLogger(WheelSpinner.class.getSimpleName());

    public WheelSpinner()
    {
        addMouseWheelListener(this);        

        // HACK ! 
        // Key presses are not consumed by JSpinner, they are also processed by the keybinding framework
        // Only way is to capture all the keys...
        // see https://docs.oracle.com/javase/tutorial/uiswing/misc/keybinding.html
        for (char c = 32; c <= 126; c++)
        {
            getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(c, 0), "doNothing");
            getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(c, InputEvent.SHIFT_DOWN_MASK), "doNothing");
        }
        getActionMap().put("doNothing", new NoAction());

        setColumns(3);
        setCtrlWheelStep(3);
        setWheelStep(1);
        setLoopValues(true);

    }

    /**
     * @param model Must be an instance of SpinnerListModel or SpinnerNumberModel.
     */
    @Override
    public void setModel(SpinnerModel model)
    {
        if (!(model instanceof SpinnerListModel) && !(model instanceof SpinnerNumberModel))
        {
            throw new IllegalArgumentException("model=" + model);
        }
        super.setModel(model);
    }

    public JSpinner.DefaultEditor getDefaultEditor()
    {
        return ((JSpinner.DefaultEditor) getEditor());
    }

    /**
     * Indicate if spinner loops to min value when max value is reached using the wheel.
     *
     * @return the loopValues
     */
    public boolean isLoopValues()
    {
        return loopValues;
    }

    /**
     * Set whether to loop to min value when max value is reached using the wheel.
     *
     * @param loopValues the loopValues to set
     */
    public void setLoopValues(boolean loopValues)
    {
        this.loopValues = loopValues;
    }

    /**
     * Set the width of the editor so it can display n chars.
     *
     * @param n int
     */
    public void setColumns(int n)
    {
        columns = n < 1 ? 1 : n;
        getDefaultEditor().getTextField().setColumns(columns);
    }

    public int getColumns()
    {
        return columns;
    }

    /**
     * @return the wheelStep
     */
    public int getWheelStep()
    {
        return wheelStep;
    }

    /**
     * @param wheelStep the wheelStep to set
     */
    public void setWheelStep(int wheelStep)
    {
        this.wheelStep = (wheelStep < 1) ? 1 : wheelStep;
    }

    /**
     * Wheel step used when ctrl is pressed.
     *
     * @return the ctrlWheelStep
     */
    public int getCtrlWheelStep()
    {
        return ctrlWheelStep;
    }

    /**
     * Wheel step used when ctrl is pressed.
     *
     * @param ctrlWheelStep the ctrlWheelStep to set
     */
    public void setCtrlWheelStep(int ctrlWheelStep)
    {
        this.ctrlWheelStep = ctrlWheelStep < 1 ? 1 : ctrlWheelStep;
    }

    // -----------------------------------------------------------------------------
    // MouseWheelListener interface
    // -----------------------------------------------------------------------------   
    @Override
    public void mouseWheelMoved(MouseWheelEvent e)
    {
        if (!isEnabled())
        {
            return;
        }
        if (!hasFocus())
        {
            requestFocusInWindow();
        }
        int onMask = InputEvent.CTRL_DOWN_MASK;
        int offMask = 0;
        boolean ctrl = ((e.getModifiersEx() & (onMask | offMask)) == onMask);
        int steps = ctrl ? ctrlWheelStep : wheelStep;
        for (int i = 0; i < steps; i++)
        {
            setValue(e.getWheelRotation() < 0 ? getNext() : getPrevious());
        }
    }

    // -----------------------------------------------------------------------------
    // Private functions
    // -----------------------------------------------------------------------------    
    private Object getNext()
    {
        Object nextValue = getModel().getNextValue();
        if (nextValue == null)
        {
            if (getModel() instanceof SpinnerNumberModel)
            {
                SpinnerNumberModel model = (SpinnerNumberModel) getModel();
                nextValue = loopValues ? model.getMinimum() : model.getMaximum();

            } else
            {
                SpinnerListModel model = (SpinnerListModel) getModel();
                nextValue = loopValues ? model.getList().get(0) : model.getValue();

            }
        }
        return nextValue;
    }

    private Object getPrevious()
    {
        Object previousValue = getModel().getPreviousValue();
        if (previousValue == null)
        {
            if (getModel() instanceof SpinnerNumberModel)
            {
                SpinnerNumberModel model = (SpinnerNumberModel) getModel();
                previousValue = loopValues ? model.getMaximum() : model.getMinimum();

            } else
            {
                SpinnerListModel model = (SpinnerListModel) getModel();
                previousValue = loopValues ? model.getList().get(model.getList().size() - 1) : model.getList().get(0);
            }
        }
        return previousValue;
    }

    private class NoAction extends AbstractAction
    {

        @Override
        public void actionPerformed(ActionEvent e)
        {
            //do nothing
        }
    }
}

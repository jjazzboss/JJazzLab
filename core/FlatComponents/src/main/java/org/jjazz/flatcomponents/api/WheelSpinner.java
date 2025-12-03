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
package org.jjazz.flatcomponents.api;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.text.ParseException;
import java.util.logging.Logger;
import javax.accessibility.AccessibleAction;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpinnerListModel;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.PlainDocument;
import org.openide.util.Exceptions;

/**
 * A JSpinner with mousewheel support, and some convenience methods.
 * <p>
 * Supports only SpinnerListModel and SpinnerNumberModel. Mouse-wheel support is enabled according to GeneralUISettings. If model is a SpinnerNumberModel
 * prevent insertion of anything except digit.
 */
public class WheelSpinner extends JSpinner implements MouseWheelListener
{

    private int wheelStep;
    private int ctrlWheelStep;
    private int columns;
    private boolean loopValues;
    private boolean blockKeyEventForwarding;
    private boolean changeFromManualEdit = true;
    private JButton btnNextArrow;
    private JButton btnPrevArrow;
    private boolean silent;
    private static final Logger LOGGER = Logger.getLogger(WheelSpinner.class.getSimpleName());


    public WheelSpinner()
    {

        // Use mouse wheel only if enabled
        FlatComponentsGlobalSettings.getInstance().installChangeValueWithMouseWheelSupport(this, this);


        setColumns(3);
        setCtrlWheelStep(3);
        setWheelStep(1);
        setLoopValues(true);

        // setBlockKeyEventForwarding(true);

        // Support for ctrl+UP/DOWN arrow (up/down arrow is supported by default by JSpinner if editor is editable)
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.CTRL_DOWN_MASK), "actionIncreaseBig");
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.CTRL_DOWN_MASK), "actionDecreaseBig");
        getActionMap().put("actionIncreaseBig", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                for (int i = 0; i < ctrlWheelStep; i++)
                {
                    setValue(getNext());
                }
            }
        });
        getActionMap().put("actionDecreaseBig", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                for (int i = 0; i < ctrlWheelStep; i++)
                {
                    setValue(getPrevious());
                }
            }
        });


        // !! THIS HACK IS L&F DEPENDENT - Works on Netbeans flatlaf Dark !!
        // We need those buttons for mouseWheelMoved() and isChangeFromManualEdit()
        for (Component c : getComponents())
        {
            if (c instanceof JButton jbtn)
            {
                if ("Spinner.nextButton".equals(jbtn.getName()))        // Might be inexistent for some L&F
                {
                    btnNextArrow = jbtn;
                } else if ("Spinner.previousButton".equals(jbtn.getName()))    // Might be inexistent for some L&F
                {
                    btnPrevArrow = jbtn;
                }
            }
        }
        if (btnNextArrow == null || btnPrevArrow == null)
        {
            LOGGER.warning("WheelSpinner() next/prev buttons not found");
        }
    }

    /**
     * Block/unblock key event forwarding for printable keys.
     * <p>
     * Key presses are not consumed by JSpinner , they are also processed by the keybinding framework. The Only way is to capture all the keys... /* see
     * https://docs.oracle.com/javase/tutorial/uiswing/misc/keybinding.html
     *
     * @param b
     */
    public void setBlockKeyEventForwarding(boolean b)
    {
        blockKeyEventForwarding = b;
        String actionName = b ? "noAction" : "donotexist";
        for (char c = 32; c <= 126; c++)
        {
            getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(c, 0), actionName);
            getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(c, InputEvent.SHIFT_DOWN_MASK), actionName);
        }
        getActionMap().put("noAction", new NoAction());
    }

    /**
     *
     * @return False by default.
     */
    public boolean isBlockKeyEventForwarding()
    {
        return blockKeyEventForwarding;
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

        if (model instanceof SpinnerNumberModel)
        {
            SpinnerNumberModel snm = (SpinnerNumberModel) model;
            JTextField tf = getDefaultEditor().getTextField();

            // Can't juste set DocumentFilter on the Spinner textfield !!!
            // Need to replace the document, and this document needs setDocumentFilter to be overridden
            // See https://stackoverflow.com/questions/9778958/make-jspinner-only-read-numbers-but-also-detect-backspace
            AbstractDocument jsDoc = (AbstractDocument) tf.getDocument();
            if (jsDoc instanceof PlainDocument)
            {
                AbstractDocument doc = new PlainDocument()
                {
                    @Override
                    public void setDocumentFilter(DocumentFilter filter)
                    {
                        if (filter instanceof DigitOnlyFilter)
                        {
                            super.setDocumentFilter(filter);
                        }
                    }
                };
                doc.setDocumentFilter(new DigitOnlyFilter((Integer) snm.getMinimum() < 0));
                tf.setDocument(doc);
                try
                {
                    doc.insertString(0, String.valueOf(model.getValue()), null);
                } catch (BadLocationException ex)
                {
                    // Should never happen
                    Exceptions.printStackTrace(ex);
                }
            }
        }
    }

    public JSpinner.DefaultEditor getDefaultEditor()
    {
        return ((JSpinner.DefaultEditor) getEditor());
    }

    /**
     * Same as setValue(Object) but with possibility of NOT notifying change listeners.
     *
     * @param value
     * @param silent If true don't notify registered ChangeListeners
     */
    public void setValue(Object value, boolean silent)
    {
        this.silent = silent;
        super.setValue(value);
        this.silent = false;
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

    /**
     * Check if the last ChangeEvent resulted from a manual edit (user typed value), or an increment/decrement action (e.g. using the up/down buttons).
     * <p>
     * Note: this may not work on all L&amp;Fs, works at least on Netbeans flatlaf dark.
     *
     * @return
     */
    public boolean isChangeFromManualEdit()
    {
        return changeFromManualEdit;
    }

    /**
     * Overridden only to manage changeFromManualEdit.
     *
     * @throws ParseException
     */
    @Override
    public void commitEdit() throws ParseException
    {
        changeFromManualEdit = false;
        super.commitEdit();
    }

    /**
     * Overridden to manage changeFromManualEdit and setValue(Object,boolean).
     * <p>
     */
    @Override
    protected void fireStateChanged()
    {
        if (!silent)
        {
            super.fireStateChanged();

        } else
        {
            // Notify only the JSpinner internal editor
            ChangeListener[] listeners = getChangeListeners();
            var ce = new ChangeEvent(this);
            for (var cl : listeners)
            {
                if (cl == getEditor())
                {
                    cl.stateChanged(ce);

                }
            }
        }

        changeFromManualEdit = true;
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


        // We need to use the actions so that isChangeFromManualEdit() works when change comes from mouse wheel
        Action action = getActionMap().get(e.getWheelRotation() < 0 ? AccessibleAction.INCREMENT : AccessibleAction.DECREMENT);
        var src = e.getWheelRotation() < 0 ? btnNextArrow : btnPrevArrow;       // btn might be null in some L&F
        if (src != null)
        {
            ActionEvent ae = new ActionEvent(src, ActionEvent.ACTION_FIRST, src.getActionCommand());
            for (int i = 0; i < steps; i++)
            {
                action.actionPerformed(ae);
            }
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

    private class DigitOnlyFilter extends DocumentFilter
    {

        boolean allowNegative;

        DigitOnlyFilter(boolean allowNegative)
        {
            this.allowNegative = allowNegative;
        }

        @Override
        public void insertString(DocumentFilter.FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException
        {
            string = allowNegative ? string.charAt(0) + string.substring(1).replace("-", "") : string.replace("-", "");
            String docText = fb.getDocument().getText(0, fb.getDocument().getLength());
            if (digitOnly(string)
                    && !(offset == 0 && string.startsWith("-") && docText.startsWith("-"))
                    && !(offset > 0 && string.startsWith("-")))
            {
                super.insertString(fb, offset, string, attr);
            }
        }

        @Override
        public void replace(DocumentFilter.FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException
        {
            text = allowNegative ? text.charAt(0) + text.substring(1).replace("-", "") : text.replace("-", "");
            if (digitOnly(text)
                    && !(offset > 0 && text.startsWith("-")))
            {
                super.replace(fb, offset, length, text, attrs);
            }
        }

        private boolean digitOnly(String text)
        {
            if (text.isEmpty())
            {
                return false;
            }
            for (int i = 0; i < text.length(); i++)
            {
                char c = text.charAt(i);
                if (allowNegative && i == 0 && c == '-')
                {
                    continue;
                }
                if (!Character.isDigit(c))
                {
                    return false;
                }
            }
            return true;
        }
    }
}

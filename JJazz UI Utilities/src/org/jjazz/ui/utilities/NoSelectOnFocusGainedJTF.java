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

import java.awt.EventQueue;
import java.awt.KeyboardFocusManager;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.openide.windows.OnShowing;

/**
 * Use the KeyboardFocusManager so that all application JTextFields have their text selected upon gaining focus.
 * <p>
 * Use an instance of this class to avoid this mechanism being applied on a specific JTextField.
 */
@OnShowing
public class NoSelectOnFocusGainedJTF extends JTextField implements Runnable
{

    private static final Logger LOGGER = Logger.getLogger(NoSelectOnFocusGainedJTF.class.getSimpleName());

    /**
     * Will be executed when Netbeans app UI is ready (@onShowing)
     */
    @Override
    public void run()
    {
        assert EventQueue.isDispatchThread();
        LOGGER.fine("ApplicationInit.run() --");
        initJTextFields();
    }

    /**
     * Make all JTextFields selectAll their text when gaining focus.
     * <p>
     * If JTextField is instanceof of NoSelectAllOnFocusGainedJTextField then nothing is done.
     */
    public void initJTextFields()
    {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("permanentFocusOwner", new PropertyChangeListener()
        {
            @Override
            public void propertyChange(final PropertyChangeEvent e)
            {
                if ((e.getOldValue() instanceof JTextField) && !(e.getOldValue() instanceof NoSelectOnFocusGainedJTF))
                {
                    SwingUtilities.invokeLater(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            // Remove selection
                            JTextField oldTextField = (JTextField) e.getOldValue();
                            oldTextField.setSelectionStart(0);
                            oldTextField.setSelectionEnd(0);
                        }
                    });

                }

                if ((e.getNewValue() instanceof JTextField) && !(e.getNewValue() instanceof NoSelectOnFocusGainedJTF))
                {
                    SwingUtilities.invokeLater(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            // Select all
                            JTextField textField = (JTextField) e.getNewValue();
                            textField.selectAll();
                        }
                    });
                }
            }
        });
    }

}

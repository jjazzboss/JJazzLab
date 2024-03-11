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
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import org.jjazz.uiutilities.api.UIUtilities;
import org.jjazz.utilities.api.ResUtil;
import org.netbeans.api.annotations.common.StaticResource;

/**
 * A help icon button that display text as a dialog when clicked.
 */
public class FlatHelpButton extends FlatButton implements PropertyChangeListener
{

    @StaticResource(relative = true)
    private static final String ICON = "resources/HelpIcon16x16.png";
    @StaticResource(relative = true)
    private static final String DISABLED_ICON = "resources/HelpIcon-Disabled-16x16.png";
    private String helpText;
    private static final Logger LOGGER = Logger.getLogger(FlatHelpButton.class.getSimpleName());

    /**
     * Equivalent of FlatButton(true, true, false)
     */
    public FlatHelpButton()
    {
        this("Help text");
    }

    public FlatHelpButton(String helpText)
    {
        super();

        setHelpText(helpText);

        Action a = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                showHelp();
            }
        };
        a.putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource(ICON)));
        a.putValue("JJazzDisabledIcon", new ImageIcon(getClass().getResource(DISABLED_ICON)));
        a.putValue(Action.SHORT_DESCRIPTION, ResUtil.getString(getClass(), "HelpToolTip"));
        a.putValue("hideActionText", true);
        setAction(a);
    }

    /**
     * Overridden to return -1 (no available baseline since icon-only button).
     *
     * @param w
     * @param h
     * @return
     */
    @Override
    public int getBaseline(int w, int h)
    {
        return -1;
    }

    /**
     * Overridden to do nothing.
     *
     * @param text
     */
    @Override
    public void setText(String text)
    {
    }

    /**
     * Used as the help text: can be multiline and use html.
     *
     * @param text
     */
    public final void setHelpText(String text)
    {
        helpText = text;
    }

    public String getHelpText()
    {
        return helpText;
    }

    // ================================================================================
    // Private functions
    // ================================================================================

    private void showHelp()
    {
        LOGGER.fine("showHelp() --");        
        // Don't use WindowManager.getDefault(), because a TopComponent might be floating
        Component parent = getParent();
        while (!(parent instanceof Frame))
        {
            parent = parent.getParent();
        }        
        JDialog dlg = new JDialog((Frame)parent, true);
        dlg.setUndecorated(true);
        JTextPane textPane = new JTextPane();
        textPane.setBackground(textPane.getBackground().darker());
        textPane.setContentType("text/html");
        textPane.setText(helpText);
        textPane.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textPane);
        dlg.add(scrollPane);
        dlg.pack();

        // Mouse listener to close the dialog
        textPane.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent me)
            {
                dlg.setVisible(false);
            }
        }
        );
        UIUtilities.installEnterKeyAction(dlg, null);
        UIUtilities.installEscapeKeyAction(dlg, null);
        UIUtilities.setDialogLocationRelativeTo(dlg, this, 5, 1d, 0.2d);
        dlg.setVisible(true);
        dlg.dispose();
    }
}

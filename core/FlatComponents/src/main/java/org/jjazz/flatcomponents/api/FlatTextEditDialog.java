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

import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import org.openide.windows.WindowManager;

/**
 * A simple modal text edit undecorated dialog.
 * <p>
 * Exit with pressing ENTER or ESC.
 */
public class FlatTextEditDialog extends javax.swing.JDialog
{

    private static FlatTextEditDialog INSTANCE;
    private boolean exitOk;
    private static final Logger LOGGER = Logger.getLogger(FlatTextEditDialog.class.getSimpleName());

    public static FlatTextEditDialog getInstance()
    {
        synchronized (FlatTextEditDialog.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new FlatTextEditDialog(WindowManager.getDefault().getMainWindow(), true);
            }
        }
        return INSTANCE;
    }

    protected FlatTextEditDialog(java.awt.Frame parent, boolean modal)
    {
        super(parent, modal);
        initComponents();
    }

    public void setText(String text)
    {
        textField.setText(text);
        textField.selectAll();
    }

    public String getText()
    {
        return textField.getText();
    }

    /**
     * Set the dialog width so that n char 'm' can be displayed.
     *
     * @param n
     */
    public void setTextNbColumns(int n)
    {
        textField.setColumns(n);
        pack();
    }

    /**
     * Set the text alignment (left, centered, etc.)
     *
     * @param alignment The JTextField constants.
     */
    public void setTextHorizontalAlignment(int alignment)
    {
        textField.setHorizontalAlignment(alignment);
    }

    public int getTextHorizontalAlignment()
    {
        return textField.getHorizontalAlignment();
    }

    public Color getTextBackground()
    {
        return textField.getBackground();
    }

    public void setTextBackground(Color c)
    {
        if (textField != null)
        {
            textField.setBackground(c);
        }
    }

    public void setTextForeground(Color c)
    {
        textField.setForeground(c);
    }

    public Color getTextForeground()
    {
        return textField.getForeground();
    }

    public void setTextFont(Font f)
    {
        textField.setFont(f);
    }

    public Font getTextFont()
    {
        return textField.getFont();
    }

    /**
     * @return True if last exit was by pressing ENTER, false if ESC was pressed.
     */
    public boolean isExitOk()
    {
        return exitOk;
    }

    /**
     * Position this dialog centered relatively to specified component.
     *
     * @param c
     */
    public void setPositionCenter(JComponent c)
    {
        Insets in = c.getInsets();
        Point p = new Point(in.left, in.top);
        SwingUtilities.convertPointToScreen(p, c);
        setLocation(p.x + c.getWidth() / 2 - getWidth() / 2, p.y + c.getHeight() / 2 - getHeight() / 2);
    }

    /**
     * Overridden to add global key bindings
     *
     * @return
     */
    @Override
    protected JRootPane createRootPane()
    {
        JRootPane contentPane = new JRootPane();
        contentPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ENTER"), "actionOk");   
        contentPane.getActionMap().put("actionOk", new AbstractAction("OK")
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                exitOk = true;
                setVisible(false);
            }
        });

        contentPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ESCAPE"), "actionCancel");   
        contentPane.getActionMap().put("actionCancel", new AbstractAction("Cancel")
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                exitOk = false;
                setVisible(false);
            }
        });
        return contentPane;
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of
     * this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        textField = new javax.swing.JTextField();

        setUndecorated(true);

        textField.setText("test"); // NOI18N
        getContentPane().add(textField, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField textField;
    // End of variables declaration//GEN-END:variables

}

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
package org.jjazz.ui.flatcomponents;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
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
        setOpaque(true);
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
    public void setColumns(int n)
    {
        textField.setColumns(n);
        pack();
    }

    @Override
    public void setBackground(Color c)
    {
        if (textField != null)
        {
            textField.setBackground(c);
        }
    }

    /**
     * Set the text alignment (left, centered, etc.)
     *
     * @param alignment The JTextField constants.
     */
    public void setHorizontalAlignment(int alignment)
    {
        textField.setHorizontalAlignment(alignment);
    }

    public int getHorizontalAlignment()
    {
        return textField.getHorizontalAlignment();
    }

    @Override
    public Color getBackground()
    {
        return textField.getBackground();
    }

    @Override
    public void setForeground(Color c)
    {
        if (textField != null)
        {
            textField.setForeground(c);
        }
    }

    @Override
    public Color getForeground()
    {
        return textField.getForeground();
    }

    @Override
    public void setFont(Font f)
    {
        textField.setFont(f);
        pack();
    }

    @Override
    public Font getFont()
    {
        return textField.getFont();
    }

    public void setOpaque(boolean b)
    {
        if (textField != null)
        {
            textField.setOpaque(b);
        }
    }

    @Override
    public boolean isOpaque()
    {
        return (textField != null) ? textField.isOpaque() : super.isOpaque();
    }

    /**
     * For example use BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(1), BorderFactory.createEmptyBorder(0, 3,
     * 0, 0)) to create a line border with a 3 pixels left margin.
     *
     * @param border
     */
    public void setBorder(Border border)
    {
        textField.setBorder(border);
        pack();
    }

    public Border getBorder()
    {
        return textField.getBorder();
    }

    public int getLeftMargin()
    {
        return textField.getBorder().getBorderInsets(textField).left;
    }

    public int getTopMargin()
    {
        return textField.getBorder().getBorderInsets(textField).top;
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

        textField = new MyJTextField();

        setUndecorated(true);

        textField.setText(org.openide.util.NbBundle.getMessage(FlatTextEditDialog.class, "FlatTextEditDialog.textField.text")); // NOI18N
        textField.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(51, 51, 51), 2), javax.swing.BorderFactory.createEmptyBorder(2, 4, 2, 4)));
        getContentPane().add(textField, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField textField;
    // End of variables declaration//GEN-END:variables

    private class MyJTextField extends JTextField
    {

        @Override
        public void setBackground(Color c)
        {
            LOGGER.fine("MyJTextField.setBackground() c=" + c);
            super.setBackground(c);
        }
    }

}

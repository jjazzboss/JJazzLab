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

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Toolkit;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import org.openide.awt.MenuBar;

public class Utilities
{

    private static JFileChooser fileChooser;

    public static JFileChooser getFileChooserInstance()
    {
        synchronized (Utilities.class)
        {
            if (fileChooser == null)
            {
                fileChooser = new JFileChooser();
            }
        }
        return fileChooser;
    }

    /**
     * If already on the EDT, call run.run(), otherwise use SwingUtilities.invokeLater(run).
     *
     * @param run
     */
    public static void invokeLaterIfNeeded(Runnable run)
    {
        if (SwingUtilities.isEventDispatchThread())
        {
            run.run();
        } else
        {
            SwingUtilities.invokeLater(run);
        }
    }

    /**
     * Recursively enable/disable a JComponent and its JComponent children.
     *
     * @param b  boolean
     * @param jc JComponent
     */
    public static void setRecursiveEnabled(boolean b, JComponent jc)
    {
        for (Component c : jc.getComponents())
        {
            if (c instanceof JComponent)
            {
                JComponent jjc = (JComponent) c;
                setRecursiveEnabled(b, jjc);
            }
        }
        jc.setEnabled(b);
    }

    public static Color calculateInverseColor(Color c)
    {
        Color nc;
        // nc = new Color( (c.getRed() < 128) ? 255 : 0, (c.getGreen() < 128) ? 255 : 0, (c.getBlue() < 128) ? 255 : 0);
        float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        float h = hsb[0] * 256;
        float s = hsb[1] * 256;
        float b = hsb[2] * 256;
        nc = (b < 50) ? Color.WHITE : Color.BLACK;
        return nc;
    }

    /**
     * Get a control-key KeyStroke which works on all OSes: Win, Linux AND Mac OSX.
     *
     * @param keyEventCode A KeyEvent constant like KeyEvent.VK_M (for ctrl-M)
     * @return
     */
    public static KeyStroke getGenericControlKeyStroke(int keyEventCode)
    {
        return KeyStroke.getKeyStroke(keyEventCode, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
    }

    /**
     * Get a control-shift key KeyStroke which works on all OSes: Win, Linux AND Mac OSX.
     *
     * @param keyEventCode A KeyEvent constant like KeyEvent.VK_M (for ctrl-shift-M)
     * @return
     */
    public static KeyStroke getGenericAltKeyStroke(int keyEventCode)
    {
        return KeyStroke.getKeyStroke(keyEventCode, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
    }

    public static Color calculateDisabledColor(Color c)
    {
        float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        hsb[2] = Math.min(hsb[2] + 0.4f, 0.8f);
        return Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
    }

    /**
     * Change the font size of a menuBar and its submenus.
     *
     * @param menuBar
     * @param fontSizeOffset eg -2 (smaller) or +1.5 (bigger)
     */
    static public void changeMenuBarFontSize(MenuBar menuBar, float fontSizeOffset)
    {
        if (menuBar == null)
        {
            throw new NullPointerException("menuBar=" + menuBar + " fontSizeOffset=" + fontSizeOffset);
        }
        for (int i = 0; i < menuBar.getMenuCount(); i++)
        {
            changeMenuFontSize(menuBar.getMenu(i), fontSizeOffset);
        }
    }

    /**
     * Change the font size of a menu and of its components (which can be submenus).
     *
     * @param menu
     * @param fontSizeOffset
     */
    static public void changeMenuFontSize(JMenu menu, float fontSizeOffset)
    {
        changeFontSize(menu, fontSizeOffset);
        int nbMenuComponents = menu.getMenuComponentCount();
        for (int j = 0; j < nbMenuComponents; j++)
        {
            Component c = menu.getMenuComponent(j);
            if (c instanceof JMenu)
            {
                changeMenuFontSize((JMenu) c, fontSizeOffset);
            } else
            {
                changeFontSize(c, fontSizeOffset);
            }
        }
    }

    /**
     * Change the font size of a component.
     *
     * @param c
     * @param fontSizeOffset eg -2 (smaller) or +1.5 (bigger)
     */
    static public void changeFontSize(Component c, float fontSizeOffset)
    {
        if (c == null)
        {
            throw new NullPointerException("c=" + c + " fontSizeOffset=" + fontSizeOffset);
        }
        Font f = c.getFont();
        if (f != null)
        {
            float newSize = Math.max(6, f.getSize() + fontSizeOffset);
            Font newFont = f.deriveFont(newSize);
            c.setFont(newFont);
        }
    }
}

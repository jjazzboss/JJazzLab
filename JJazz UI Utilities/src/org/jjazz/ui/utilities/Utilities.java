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
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import org.openide.awt.MenuBar;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.actions.ActionPresenterProvider;
import org.openide.util.actions.Presenter;
import org.openide.windows.WindowManager;

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
     * Create one or more JMenuItems or JSeparators from a Netbeans action.
     * <p>
     * Copied from part of org.openide.util.Utilities.actionsToPopup(). Special handling if action is instance of:<br>
     * - ContextAwareAction<br>
     * - Presenter.Popup<br>
     * <p>
     * If Presenter.Popup is implemented and the JMenuItem returned by getPopupPresenter()... :<br>
     * - has client property DynamicMenuContent.HIDE_WHEN_DISABLED, then no menu item is created if action is disabled.<br>
     * - is instance of DynamicContent, then use the result of item.getMenuPresenters() (JMenuItems, or JSeparators for null
     * values).
     *
     * @param action
     * @param context The context used for the action if it's a ContextAwareAction instance
     * @return A list of JMenuItems or JSeparators. Can be empty.
     */
    public static List<JComponent> actionToMenuItems(Action action, Lookup context)
    {
        if (action == null)
        {
            throw new IllegalArgumentException("action=" + action + " context=" + context);
        }

        // switch to replacement action if there is some
        if (action instanceof ContextAwareAction)
        {
            Action contextAwareAction = ((ContextAwareAction) action).createContextAwareInstance(context);
            if (contextAwareAction == null)
            {
                throw new IllegalArgumentException("ContextAwareAction.createContextAwareInstance(context) returns null.");
            } else
            {
                action = contextAwareAction;
            }
        }

        JMenuItem item;
        if (action instanceof Presenter.Popup)
        {
            item = ((Presenter.Popup) action).getPopupPresenter();
            if (item == null)
            {
                throw new IllegalArgumentException("getPopupPresenter() returning null for action=" + action);
            }
        } else
        {
            // We need to correctly handle mnemonics with '&' etc.
            item = ActionPresenterProvider.getDefault().createPopupPresenter(action);
        }

        var res = new ArrayList<JComponent>();
        for (Component c : ActionPresenterProvider.getDefault().convertComponents(item))
        {
            if (c instanceof JMenuItem || c instanceof JSeparator)
            {
                res.add((JComponent) c);
            }
        }

        return res;

    }

    /**
     * Recursively enable/disable a JComponent and its JComponent children.
     *
     * @param b boolean
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
     * Install a listener to automatically select all text when component gets the focus.
     *
     * @param comp
     */
    public static void installSelectAllWhenFocused(JTextComponent comp)
    {
        comp.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusGained(FocusEvent e)
            {
                comp.selectAll();
            }
        });
    }

    /**
     * Get a control-key KeyStroke which works on all OSes: Win, Linux AND Mac OSX.
     *
     * @param keyEventCode A KeyEvent constant like KeyEvent.VK_M (for ctrl-M)
     * @return
     */
    public static KeyStroke getGenericControlKeyStroke(int keyEventCode)
    {
        return KeyStroke.getKeyStroke(keyEventCode, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    }

    /**
     * Get a control-shift key KeyStroke which works on all OSes: Win, Linux AND Mac OSX.
     *
     * @param keyEventCode A KeyEvent constant like KeyEvent.VK_M (for ctrl-shift-M)
     * @return
     */
    public static KeyStroke getGenericAltKeyStroke(int keyEventCode)
    {
        return KeyStroke.getKeyStroke(keyEventCode, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
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
            if (menuBar.getMenu(i) != null)
            {
                changeMenuFontSize(menuBar.getMenu(i), fontSizeOffset);
            }
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
            } else if (c != null)
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

    /**
     * Show the JFileChooser to select a directory.
     *
     * @param dirPath Initialize chooser with this directory.
     * @param title Title of the dialog.
     * @return The selected dir or null.
     */
    static public File showDirChooser(String dirPath, String title)
    {
        JFileChooser chooser = getFileChooserInstance();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setMultiSelectionEnabled(false);
        chooser.setDialogType(JFileChooser.CUSTOM_DIALOG);
        chooser.setDialogTitle(title);
        File f = new File(dirPath);
        File parent = f.getParentFile();
        if (parent != null)
        {
            chooser.setCurrentDirectory(parent);
        }
        chooser.setSelectedFile(f);
        File newDir = null;
        if (chooser.showDialog(WindowManager.getDefault().getMainWindow(), "Select") == JFileChooser.APPROVE_OPTION)
        {
            newDir = chooser.getSelectedFile();
            if (newDir != null && !newDir.isDirectory())
            {
                newDir = null;
            }
        }
        return newDir;
    }

    // =================================================================================================
    // Static classes
    // =================================================================================================
}

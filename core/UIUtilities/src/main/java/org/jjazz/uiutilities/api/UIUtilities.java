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
package org.jjazz.uiutilities.api;

import com.google.common.base.Preconditions;
import java.awt.AWTEvent;
import java.awt.AWTException;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLayer;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRootPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.LayerUI;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.openide.awt.MenuBar;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.actions.ActionPresenterProvider;
import org.openide.util.actions.Presenter;
import org.openide.windows.WindowManager;

public class UIUtilities
{

    private static JFileChooser fileChooser;
    private static final NoAction NoActionInstance = new NoAction();
    private static final Map<Container, List<JComponent>> enabledContainers = new HashMap<Container, List<JComponent>>();
    private static final Logger LOGGER = Logger.getLogger(UIUtilities.class.getSimpleName());

    public static JFileChooser getFileChooserInstance()
    {
        synchronized (UIUtilities.class)
        {
            if (fileChooser == null)
            {
                fileChooser = new JFileChooser();
            }
        }
        return fileChooser;
    }

    /**
     * Get the current screen bounds where specified component is displayed, excluding possible taskbars.
     * <p>
     * Supposed to handle correctly multiple monitors on various OS.<p>
     * See https://stackoverflow.com/questions/10123735/get-effective-screen-size-from-java/10123912 (answer of Rasmus Faber)
     *
     * @param c
     * @return
     */
    public static Rectangle getEffectiveScreenArea(Component c)
    {
        GraphicsConfiguration gc = c.getGraphicsConfiguration();
        Rectangle bounds = gc.getBounds();
        Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(gc);

        var res = new Rectangle();
        res.x = bounds.x + screenInsets.left;
        res.y = bounds.y + screenInsets.top;
        res.height = bounds.height - screenInsets.top - screenInsets.bottom;
        res.width = bounds.width - screenInsets.left - screenInsets.right;

        return res;
    }


    /**
     * Get the size of a text with the specified font.
     * <p>
     * Use a temporary BufferedImage() to calculate the sizing.
     *
     * @param text
     * @param f
     * @return
     */
    static public Rectangle2D getStringBounds(String text, Font f)
    {
        BufferedImage img = new BufferedImage(3, 3, BufferedImage.TYPE_INT_ARGB);       // Size does not matter
        Graphics2D g2 = img.createGraphics();
        var sm = StringMetrics.create(g2, f);
        var res = sm.getLogicalBoundsNoLeading(text);
        img.flush();
        g2.dispose();
        return res;
    }

    /**
     * Gets the usable rectangle area within the borders (insets) of the JComponent.
     *
     * @param jc
     * @return
     */
    static public Rectangle getUsableArea(JComponent jc)
    {
        Insets in = jc.getInsets();
        int x0 = in.left;
        int w = jc.getWidth() - in.right - in.left;
        int y0 = in.top;
        int h = jc.getHeight() - in.top - in.bottom;
        return new Rectangle(x0, y0, w, h);
    }

    /**
     * Move the mouse pointer to a specific point, handling possible multi-screen setup.
     * <p>
     * From https://stackoverflow.com/questions/2941324/how-do-i-set-the-position-of-the-mouse-in-java
     *
     * @param p
     */
    static public void moveMouse(Point p)
    {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();

        // Search the devices for the one that draws the specified point.
        for (GraphicsDevice device : gs)
        {
            GraphicsConfiguration[] configurations = device.getConfigurations();
            for (GraphicsConfiguration config : configurations)
            {
                Rectangle bounds = config.getBounds();
                if (bounds.contains(p))
                {
                    // Set point to screen coordinates.
                    Point b = bounds.getLocation();
                    Point s = new Point(p.x - b.x, p.y - b.y);
                    try
                    {
                        Robot r = new Robot(device);
                        r.mouseMove(s.x, s.y);
                    } catch (AWTException e)
                    {
                        e.printStackTrace();
                    }

                    return;
                }
            }
        }
        // Couldn't move to the point, it may be off screen.
    }

    /**
     * Positions a dialog at a position relative to an anchor component.
     *
     * @param dialog            the dialog to be positioned.
     * @param anchorComponent   the anchor component
     * @param padding           The maximum space between dialog and anchor component.
     * @param horizontalPercent 0 means left of anchor component, 1 is right, 0.5 is center
     * @param verticalPercent   0 means above of anchor component, 1 is below, 0.5 is center
     */
    public static void setDialogLocationRelativeTo(final Dialog dialog,
            final Component anchorComponent,
            final int padding,
            final double horizontalPercent,
            final double verticalPercent)
    {
        final Dimension dlgSize = dialog.getSize();
        final Dimension anchorSize = anchorComponent.getSize();
        final Point anchorLocation = anchorComponent.getLocationOnScreen();

        final int baseX = anchorLocation.x - padding - dlgSize.width;
        final int baseY = anchorLocation.y - padding - dlgSize.height;
        final int w = dlgSize.width + padding + anchorSize.width + padding;
        final int h = dlgSize.height + padding + anchorSize.height + padding;
        int x = baseX + (int) (horizontalPercent * w);
        int y = baseY + (int) (verticalPercent * h);

        // make sure the dialog fits completely on the screen...
        final Rectangle rScreen = getEffectiveScreenArea(anchorComponent);
        x = Math.min(x, rScreen.width - dlgSize.width);
        x = Math.max(x, 0);
        y = Math.min(y, rScreen.height - dlgSize.height);
        y = Math.max(y, 0);

        dialog.setLocation(x + rScreen.x, y + rScreen.y);
    }

    /**
     * Create an AbstractAction from the specified ActionListener.
     *
     * @param al
     * @return
     */
    static public Action getAction(ActionListener al)
    {
        return new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                al.actionPerformed(e);
            }
        };
    }


    /**
     * Silently update a JComboBox without notifying any ActionListener.
     *
     * @param cmb
     * @param task E.g. addItem(), setSelectedItem(), etc.
     */
    public static void silentlyUpdateComboBox(final JComboBox<?> cmb, final Runnable task)
    {
        final ActionListener[] actionListeners = cmb.getActionListeners();
        Stream.of(actionListeners).forEach(l -> cmb.removeActionListener(l));
        try
        {
            task.run();
        } finally
        {
            Stream.of(actionListeners).forEach(l -> cmb.addActionListener(l));
        }
    }

    /**
     * Convert a JList default list model into a normal list.
     *
     * @param <T>
     * @param listModel
     * @return
     */
    public static <T> List<T> getJListModelAsList(DefaultListModel<T> listModel)
    {
        List<T> pts = new ArrayList<>();
        for (int i = 0; i < listModel.size(); i++)
        {
            pts.add(listModel.get(i));
        }
        return pts;
    }

    /**
     * Install an action on a dialog when the ESCAPE key is pressed.
     *
     * @param dialog
     * @param r      Call r.run() when ESCAPE is pressed. If r is null pressing ESCAPE closes the dialog.
     */
    public static void installEscapeKeyAction(JDialog dialog, Runnable r)
    {
        Action a = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent event)
            {
                if (r == null)
                {
                    dialog.dispatchEvent(new WindowEvent(dialog, WindowEvent.WINDOW_CLOSING));
                } else
                {
                    r.run();
                }
            }
        };

        JRootPane rootPane = dialog.getRootPane();
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "EscapeAction898726");
        rootPane.getActionMap().put("EscapeAction898726", a);

    }

    /**
     * Install an action on a dialog when the ENTER key is pressed.
     *
     * @param dialog
     * @param r      Call r.run() when ENTER is pressed. If r is null pressing ENTER closes the dialog.
     */
    public static void installEnterKeyAction(JDialog dialog, Runnable r)
    {
        Action a = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent event)
            {
                if (r == null)
                {
                    dialog.dispatchEvent(new WindowEvent(dialog, WindowEvent.WINDOW_CLOSING));
                } else
                {
                    r.run();
                }
            }
        };

        JRootPane rootPane = dialog.getRootPane();
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "EnterAction0023");
        rootPane.getActionMap().put("EnterAction0023", a);

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
     * - is instance of DynamicContent, then use the result of item.getMenuPresenters() (JMenuItems, or JSeparators for null values).
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
        if (action instanceof ContextAwareAction cwa)
        {
            Action contextAwareAction = cwa.createContextAwareInstance(context);
            if (contextAwareAction == null)
            {
                throw new IllegalArgumentException("ContextAwareAction.createContextAwareInstance(context) returns null.");
            } else
            {
                action = contextAwareAction;
            }
        }

        JMenuItem item;
        if (action instanceof Presenter.Popup popup)
        {
            item = popup.getPopupPresenter();
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
     * @param b  boolean
     * @param jc JComponent
     */
    public static void setRecursiveEnabled(boolean b, JComponent jc)
    {
        if ((b && jc.isEnabled()) || (!b && !jc.isEnabled()))
        {
            return;
        }

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
     * Creates a JLayer for comp so that enterExitListener is *reliably* called when mouse enters/exit comp.
     *
     * @param comp
     * @param enterExitListener Called whenever mouse enters (bool=true) or exits comp's bounds.
     * @return
     */
    public static JLayer<JComponent> createEnterExitComponentLayer(JComponent comp, Consumer<Boolean> enterExitListener)
    {
        LayerUI<JComponent> layerUI = new LayerUI<JComponent>()
        {
            @Override
            public void paint(Graphics g, JComponent jc)
            {
                // paint the layer as is
                super.paint(g, jc);
            }

            @Override
            public void installUI(JComponent jc)
            {
                super.installUI(jc);
                // enable mouse motion events for the layer's subcomponents
                ((JLayer) jc).setLayerEventMask(AWTEvent.MOUSE_EVENT_MASK);
            }

            @Override
            public void uninstallUI(JComponent jc)
            {
                super.uninstallUI(jc);
                // reset the layer event mask
                ((JLayer) jc).setLayerEventMask(0);
            }


            @Override
            public void eventDispatched(AWTEvent e, JLayer<? extends JComponent> layer)
            {
                if (e instanceof MouseEvent me && (me.getID() == MouseEvent.MOUSE_ENTERED || me.getID() == MouseEvent.MOUSE_EXITED))
                {
                    Point p = SwingUtilities.convertPoint(me.getComponent(), me.getPoint(), layer);
                    boolean inside = layer.contains(p);     // This way it works even if mouse is on another component inside the RpViewer
                    enterExitListener.accept(inside);
                }
            }
        };

        // Create the layer for the component using our custom layerUI
        var layer = new JLayer<JComponent>(comp, layerUI);
        return layer;
    }

    /**
     * Make the specified textComponent capture all ASCII printable key presses.
     * <p>
     * Key presses are used by an editable JTextComponent to display the chars, but it does not consume the key presses. So they are transmitted up the
     * containment hierarchy via the keybinding framework. This means a global Netbeans action might be triggered if user types a global action shortcut (eg
     * SPACE) in the JTextComponent.<p>
     * This method makes textComponent capture all ASCII printable key presses (ASCII char from 32 to 126) to avoid this behaviour.
     * <p>
     *
     * @param textComponent
     */
    public static void installPrintableAsciiKeyTrap(JTextComponent textComponent)
    {
        // HACK ! 
        // Only way to block them is to capture all the printable keys
        // see https://docs.oracle.com/javase/tutorial/uiswing/misc/keybinding.html
        for (char c = 32; c <= 126; c++)
        {
            textComponent.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(c, 0), "doNothing");
            textComponent.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(c,
                    InputEvent.SHIFT_DOWN_MASK), "doNothing");
        }
        textComponent.getActionMap().put("doNothing", NoActionInstance);

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
     * Get a control-sfhit key KeyStroke which works on all OSes: Win, Linux AND Mac OSX.
     *
     * @param keyEventCode A KeyEvent constant like KeyEvent.VK_M (for ctrl-M)
     * @return
     */
    public static KeyStroke getGenericControlShiftKeyStroke(int keyEventCode)
    {
        return KeyStroke.getKeyStroke(keyEventCode, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | InputEvent.SHIFT_DOWN_MASK);
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
     * @param sizeMinMax     If specified limit the size of the returned value, first arg is the min size, second is the max size
     */
    static public void changeMenuFontSize(JMenu menu, float fontSizeOffset, float... sizeMinMax)
    {
        changeFontSize(menu, fontSizeOffset);
        int nbMenuComponents = menu.getMenuComponentCount();
        for (int j = 0; j < nbMenuComponents; j++)
        {
            Component c = menu.getMenuComponent(j);
            if (c instanceof JMenu)
            {
                changeMenuFontSize((JMenu) c, fontSizeOffset, sizeMinMax);
            } else if (c != null)
            {
                changeFontSize(c, fontSizeOffset, sizeMinMax);
            }
        }
    }

    /**
     * Change the font size of a component.
     *
     * @param c
     * @param fontSizeOffset eg -2 (smaller) or +1.5 (bigger)
     * @param sizeMinMax     If specified limit the size of the returned value, first arg is the min size, second is the max size
     */
    static public void changeFontSize(Component c, float fontSizeOffset, float... sizeMinMax)
    {
        Objects.requireNonNull(c);
        Font f = c.getFont();
        if (f != null)
        {
            c.setFont(changeFontSize(f, fontSizeOffset, sizeMinMax));
        }
    }

    /**
     * Get a new font with size modified by adding fontSizeOffset.
     *
     * @param f
     * @param fontSizeOffset
     * @param sizeMinMax     If specified limit the size of the returned value, first arg is the min size, second is the max size
     * @return
     */
    static public Font changeFontSize(Font f, float fontSizeOffset, float... sizeMinMax)
    {
        Objects.requireNonNull(f);
        float min = sizeMinMax.length >= 1 ? sizeMinMax[0] : 4f;
        float max = sizeMinMax.length >= 2 ? sizeMinMax[1] : 100f;
        float newSize = Math.max(min, f.getSize() + fontSizeOffset);
        newSize = Math.min(max, newSize);
        Font res = f.deriveFont(newSize);
        return res;
    }

    /**
     * Get the accepted default Netbeans TopComponent tab actions.
     *
     * @param defaultNbActions The default Netbeans actions returned by TopComponent.getActions() default implementation.
     * @return A non mutable list
     * @see #isNetbeansTopComponentTabActionUsed(javax.swing.Action)
     */
    static public List<Action> getNetbeansTopComponentTabActions(Action[] defaultNbActions)
    {
        Objects.requireNonNull(defaultNbActions);
        var res = Stream.of(defaultNbActions)
                .filter(a -> isNetbeansTopComponentTabActionUsed(a))
                .toList();
        return res;
    }

    /**
     * Check if the specified standard Netbeans action (such as CloseWindowAction) should be left in the tab menu of JJazzLab TopComponents.
     *
     * @param nbAction If null return true
     * @return
     */
    static public boolean isNetbeansTopComponentTabActionUsed(Action nbAction)
    {
        boolean b = true;
        if (nbAction != null)
        {
            var className = nbAction.getClass().getSimpleName();
            var blackList = List.of("CloneDocumentAction", "NewTabGroupAction", "CollapseTabGroupAction", "MoveModeAction", "ResizeModeAction", "MoveWindowAction");
            b = blackList.stream().noneMatch(s -> className.contains(s));
        }
        return b;
    }


    /**
     * Count text lines to line wrap of a JTextArea.
     * <p>
     * Source - https://stackoverflow.com/a/30622483 Posted by sly493, modified by community. License - CC BY-SA 3.0.
     *
     * @param ta
     * @return
     */
    public static int countLines(JTextArea ta)
    {
        final Insets in = ta.getInsets();
        final float formatWidth = ta.getWidth() - in.left - in.right;
        final var text = ta.getText();
        int count = 0;

        for (var line : text.split("\n"))
        {
            if (line.isEmpty())
            {
                count++;
                continue;
            }
            AttributedString attString = new AttributedString(line);
            attString.addAttribute(TextAttribute.FONT, ta.getFont());
            FontRenderContext frc = ta.getFontMetrics(ta.getFont()).getFontRenderContext();
            AttributedCharacterIterator charIt = attString.getIterator();
            LineBreakMeasurer lineMeasurer = new LineBreakMeasurer(charIt, frc);
            lineMeasurer.setPosition(charIt.getBeginIndex());

            int lineCount = 0;
            while (lineMeasurer.getPosition() < charIt.getEndIndex())
            {
                lineMeasurer.nextLayout(formatWidth);
                lineCount++;
            }
            count += lineCount;
        }

        return count;
    }


    /**
     * Show the JFileChooser to select a directory.
     *
     * @param dirPath Initialize chooser with this directory.
     * @param title   Title of the dialog.
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

    /**
     * Draw a string left-aligned/centered/right-aligned on component jc.
     * <p>
     * If component width is too small for text to be centered, text is aligned on the left.<p>
     * If string contains '\n', string will be displayed on several lines.
     *
     * @param g2     Used to draw the string with the default font and color.
     * @param jc
     * @param text
     * @param hAlign 0=left, 1=centered, 2=right
     */
    static public void drawStringAligned(Graphics2D g2, JComponent jc, String text, int hAlign)
    {
        Preconditions.checkArgument(hAlign >= 0 && hAlign <= 2);


        Rectangle r = getUsableArea(jc);
        StringMetrics sm = StringMetrics.create(g2);


        String[] strs = text.split("\\n");
        int nbLines = strs.length;
        if (nbLines == 1)
        {
            // Single line
            var bounds = sm.getLogicalBoundsNoLeading(text);
            float x = switch (hAlign)
            {
                case 0 -> // Left
                    r.x + 1;
                case 1 -> // Centered
                    (float) Math.max(0, r.x + (r.width - bounds.getWidth()) / 2);
                case 2 -> // Right
                    (float) ((r.x + r.width - 2) - bounds.getWidth());
                default ->
                    throw new IllegalArgumentException("alignment=" + hAlign);
            };

            float y = (float) (r.y + (r.height - bounds.getHeight()) / 2 - bounds.getY());  // bounds are in baseline-relative coordinates!
            g2.drawString(text, x, y);
            return;
        } else
        {
            // Multi-line 
            Rectangle2D[] bounds = new Rectangle2D[nbLines];

            // Compute total height
            double h = 0;
            for (int i = 0; i < nbLines; i++)
            {
                bounds[i] = sm.getLogicalBounds(strs[i]);
                h += bounds[i].getHeight();
            }

            float y = (float) (r.y + (r.height - h) / 2);
            for (int i = 0; i < nbLines; i++)
            {
                float x = switch (hAlign)
                {
                    case 0 -> // Left
                        r.x + 1;
                    case 1 -> // Centered
                        (float) Math.max(0, r.x + (r.width - bounds[i].getWidth()) / 2);
                    case 2 -> // Right
                        (float) ((r.x + r.width - 2) - bounds[i].getWidth());
                    default ->
                        throw new IllegalArgumentException("alignment=" + hAlign);
                };
                g2.drawString(strs[i], x, (float) (y - bounds[i].getY()));
                y += bounds[i].getHeight();
            }
        }
    }

    /**
     * Convenience static method to disable container and all its children.
     * <p>
     * The method saves the enabled state of children, in order to reenable them (or not) as required when calling enableContainer().
     *
     * @param container the Container containing Components to be disabled
     * @see #enableContainer(java.awt.Container)
     */
    public static void disableContainer(Container container)
    {
        if (!container.isEnabled())
        {
            return;
        }
        container.setEnabled(false);
        List<JComponent> components = getDescendantsOfType(JComponent.class, container, true);
        List<JComponent> enabledComponents = new ArrayList<>();
        enabledContainers.put(container, enabledComponents);

        for (JComponent component : components)
        {
            if (component.isEnabled())
            {
                enabledComponents.add(component);
                component.setEnabled(false);
            }
        }
    }

    /**
     * Convenience static method to enable container and children components previously disabled by using the disableContainer() method.
     * <p>
     * Only Components disable by the disableContainer() method will be enabled.
     *
     * @param container a Container that has been previously disabled.
     * @see #disableContainer(java.awt.Container)
     */
    public static void enableContainer(Container container)
    {
        if (container.isEnabled())
        {
            return;
        }
        container.setEnabled(true);
        List<JComponent> enabledComponents = enabledContainers.get(container);
        if (enabledComponents != null)
        {
            for (JComponent component : enabledComponents)
            {
                component.setEnabled(true);
            }
            enabledContainers.remove(container);
        }
    }

    /**
     * Convenience method for searching below <code>container</code> in the component hierarchy and return nested components that are instances of class
     * <code>clazz</code> it finds.
     * <p>
     * Returns an empty list if no such components exist in the container.
     * <p>
     * Invoking this method with a class parameter of JComponent.class will return all nested components.
     *
     * @param <T>
     * @param clazz     the class of components whose instances are to be found.
     * @param container the container at which to begin the search
     * @param nested    true to list components nested within another listed component, false otherwise
     * @return the List of components
     */
    public static <T extends JComponent> List<T> getDescendantsOfType(Class<T> clazz, Container container, boolean nested)
    {
        List<T> tList = new ArrayList<>();
        for (Component component : container.getComponents())
        {
            if (clazz.isAssignableFrom(component.getClass()))
            {
                tList.add(clazz.cast(component));
            }
            if (nested || !clazz.isAssignableFrom(component.getClass()))
            {
                tList.addAll(getDescendantsOfType(clazz, (Container) component, nested));
            }
        }
        return tList;
    }

    /**
     * Build a JSlider with an adjusted preferred length.
     *
     * @param orientation The orientation of the JSlider, SwingConstants.VERTICAL or SwingConstants.HORIZONTAL.
     * @param ratio       The ratio to be applied on the preferred length (width or height, depending on orientation).
     * @return
     */
    public static JSlider buildSlider(int orientation, float ratio)
    {
        Preconditions.checkArgument(orientation == SwingConstants.VERTICAL || orientation == SwingConstants.HORIZONTAL);
        Preconditions.checkArgument(ratio > 0);
        JSlider res = new JSlider(orientation)
        {
            @Override
            public Dimension getPreferredSize()
            {
                int w, h;
                var ps = super.getPreferredSize();
                if (getOrientation() == SwingConstants.VERTICAL)
                {
                    w = ps.width;
                    h = (int) (ps.height * ratio);
                } else
                {
                    w = (int) (ps.width * ratio);
                    h = ps.height;
                }
                return new Dimension(w, h);
            }
        };
        return res;
    }

    /**
     * Installs a listener to receive notification when the text of any {@code JTextComponent} is changed.
     * <p>
     * Internally, it installs a {@link DocumentListener} on the text component's {@link Document}, and a {@link PropertyChangeListener} on the text component
     * to detect if the {@code Document} itself is replaced.
     * <p>
     * Usage: addChangeListener(someTextBox, e -> doSomething());
     * <p>
     * From Stackoverflow: https://stackoverflow.com/questions/3953208/value-change-listener-to-jtextfield
     *
     * @param text           any text component, such as a {@link JTextField} or {@link JTextArea}
     * @param changeListener a listener to receieve {@link ChangeEvent}s when the text is changed; the source object for the events will be the text component
     * @throws NullPointerException if either parameter is null
     */
    public static void addChangeListener(JTextComponent text, ChangeListener changeListener)
    {
        Objects.requireNonNull(text);
        Objects.requireNonNull(changeListener);


        DocumentListener dl = new DocumentListener()
        {
            private int lastChange = 0, lastNotifiedChange = 0;

            @Override
            public void insertUpdate(DocumentEvent e)
            {
                changedUpdate(e);
            }

            @Override
            public void removeUpdate(DocumentEvent e)
            {
                changedUpdate(e);
            }

            @Override
            public void changedUpdate(DocumentEvent e)
            {
                lastChange++;
                SwingUtilities.invokeLater(() -> 
                {
                    if (lastNotifiedChange != lastChange)
                    {
                        lastNotifiedChange = lastChange;
                        changeListener.stateChanged(new ChangeEvent(text));
                    }
                });
            }
        };


        text.addPropertyChangeListener("document", (PropertyChangeEvent e) -> 
        {
            Document d1 = (Document) e.getOldValue();
            Document d2 = (Document) e.getNewValue();
            if (d1 != null)
            {
                d1.removeDocumentListener(dl);
            }
            if (d2 != null)
            {
                d2.addDocumentListener(dl);
            }
            dl.changedUpdate(null);
        });


        Document d = text.getDocument();
        if (d != null)
        {
            d.addDocumentListener(dl);

        }
    }


    // =================================================================================================
    // Static classes
    // =================================================================================================
    static private class NoAction extends AbstractAction
    {

        @Override
        public void actionPerformed(ActionEvent e)
        {
            //do nothing
        }
    }
}

package org.jjazz.flatcomponents.api;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;


/**
 * * Copyright (c) 2004 Memorial Sloan-Kettering Cancer Center * * Code written by: Gary Bader * Authors: Gary Bader, Ethan Cerami, Chris
 * Sander * * This library is free software; you can redistribute it and/or modify it * under the terms of the GNU Lesser General Public
 * License as published * by the Free Software Foundation; either version 2.1 of the License, or * any later version. * * This library is
 * distributed in the hope that it will be useful, but * WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF * MERCHANTABILITY OR
 * FITNESS FOR A PARTICULAR PURPOSE. The software and * documentation provided hereunder is on an "as is" basis, and * Memorial
 * Sloan-Kettering Cancer Center * has no obligations to provide maintenance, support, * updates, enhancements or modifications. In no event
 * shall the * Memorial Sloan-Kettering Cancer Center * be liable to any party for direct, indirect, special, * incidental or consequential
 * damages, including lost profits, arising * out of the use of this software and its documentation, even if * Memorial Sloan-Kettering
 * Cancer Center * has been advised of the possibility of such damage. See * the GNU Lesser General Public License for more details. * * You
 * should have received a copy of the GNU Lesser General Public License * along with this library; if not, write to the Free Software
 * Foundation, * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 * <p>
 * User: Vuk Pavlovic Date: Nov 29, 2006 Time: 5:34:46 PM Description: The user-triggered collapsable panel containing the component
 * (trigger) in the titled border
 * <p>
 * Changes: Jerome Lelasseux for JJazzLab
 */
/**
 * The user-triggered collapsable contentPanel containing the component (trigger) in the titled border
 */
public class CollapsiblePanel extends JPanel
{

    /**
     * newValue = isCollapsed
     */
    public static final String PROP_COLLAPSED = "Collapsed";


    CollapsableTitledBorder border; // includes upper left component and line type
    private static final Border COLLAPSED_BORDER_LINE = BorderFactory.createEmptyBorder(2, 2, 2, 2); // no border
    private static final Border EXPANDED_BORDER_LINE = null; // because this is null, default is used, etched lowered border on MAC
    private CollapsableTitledBorder collapsedBorder;
    private CollapsableTitledBorder expandedBorder;

    // Title
    AbstractButton titleComponent; // displayed in the titled border

    // Expand/Collapse button
    final static int COLLAPSED = 0, EXPANDED = 1; // image States
    ImageIcon[] iconArrow = createExpandAndCollapseIcon();
    JButton arrow = createArrowButton();

    // Content Pane
    JPanel contentPanel;

    // Container State
    boolean collapsed = true; // stores curent state of the collapsable contentPanel
    private static final Logger LOGGER = Logger.getLogger(CollapsiblePanel.class.getSimpleName());

    public CollapsiblePanel()
    {
        // this("text", TitledBorder.RIGHT, TitledBorder.DEFAULT_POSITION);
        this("text");
    }

    /**
     * Constructor for an option button controlled collapsable panel.
     * <p>
     * This is useful when a group of options each have unique sub contents. The radio buttons should be created, grouped, and then used to
     * construct their own collapsable panels. This way choosing a different option in the same option group will collapse all unselected
     * options. Expanded panels draw a border around the contents and through the radio button in the fashion of a titled border.
     *
     * @param component Radio button that expands and collapses the contentPanel based on if it is selected or not
     */
    public CollapsiblePanel(JRadioButton component)
    {
        component.addItemListener(new CollapsiblePanel.ExpandAndCollapseAction());
        titleComponent = component;
        collapsed = !component.isSelected();
        commonConstructor(TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION);
    }

    /**
     * Constructor for a label/button controlled collapsable panel.
     * <p>
     * Displays a clickable title that resembles a native titled border except for an arrow on the right side indicating an expandable
     * contentPanel. The actual border only appears when the contentPanel is expanded.
     *
     * @param text Title of the collapsable contentPanel in string format, used to create a button with text and an arrow icon
     */
    public CollapsiblePanel(String text)
    {
        arrow.setText(text);
        titleComponent = arrow;
        commonConstructor(TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION);
    }

    /**
     * Constructor for a label/button controlled collapsable panel.
     *
     * @param text
     * @param titleJustification use TitledBorder constant
     * @param titlePosition      use TitledBorder constant
     */
    public CollapsiblePanel(String text, int titleJustification, int titlePosition)
    {
        arrow.setText(text);
        titleComponent = arrow;
        commonConstructor(titleJustification, titlePosition);
    }

    /**
     * Sets layout, creates the content contentPanel and adds it and the title component to the container, all constructors have this
     * procedure in common.
     *
     * @param titleJustification use TitledBorder constant
     * @param titlePosition      use TitledBorder constant
     */
    private void commonConstructor(int titleJustification, int titlePosition)
    {
        setLayout(new BorderLayout());

        contentPanel = new JPanel();
        contentPanel.setLayout(new BorderLayout());

        add(titleComponent, BorderLayout.CENTER);
        add(contentPanel, BorderLayout.CENTER);

        collapsedBorder = new CollapsableTitledBorder(COLLAPSED_BORDER_LINE, titleComponent, titleJustification, titlePosition);
        expandedBorder = new CollapsableTitledBorder(EXPANDED_BORDER_LINE, titleComponent, titleJustification, titlePosition);
        setCollapsed(collapsed);

        placeTitleComponent();
    }

    /**
     * Sets the bounds of the border title component so that it is properly positioned.
     */
    private void placeTitleComponent()
    {
        Insets insets = this.getInsets();
        Rectangle containerRectangle = this.getBounds();
        Rectangle componentRectangle = border.getComponentRect(containerRectangle, insets);
        titleComponent.setBounds(componentRectangle);
    }

    public void setTitleComponentText(String text)
    {
        if (titleComponent instanceof JButton)
        {
            titleComponent.setText(text + " ");
        }
        placeTitleComponent();
    }

    public String getTitleComponentText()
    {
        return (titleComponent instanceof JButton jb) ? jb.getText() : "";
    }

    /**
     * This class requires that all content be placed within a designated contentPanel, this method returns that contentPanel.
     *
     * @return contentPanel The content contentPanel
     */
    public JPanel getContentPane()
    {
        return contentPanel;
    }

    /**
     * Override make sure preferred width is never smaller than titleComponent preferred width, even when collapsed.
     *
     * @return
     */
    @Override
    public Dimension getPreferredSize()
    {
        Dimension d = super.getPreferredSize();

        int titleWidth = titleComponent.getPreferredSize().width;
        Insets in = border.getBorderInsets(null, new Insets(0, 0, 0, 0));
        titleWidth += in.left + in.right;
        if (d.width < titleWidth)
        {
            d.width = titleWidth;
        }

        return d;
    }

    /**
     * Override make sure minimum width is never smaller than titleComponent preferred size, even when collapsed.
     *
     * @return
     */
    @Override
    public Dimension getMinimumSize()
    {
        Dimension d = super.getMinimumSize();

        int titleWidth = titleComponent.getPreferredSize().width;
        Insets in = border.getBorderInsets(null, new Insets(0, 0, 0, 0));
        titleWidth += in.left + in.right;
        if (d.width < titleWidth)
        {
            d.width = titleWidth;
        }

        return d;
    }

    /**
     * Collapses or expands the panel.
     * <p>
     * This is done by adding or removing the content pane, alternating between a frame and empty border, and changing the title arrow.
     * Also, the current state is stored in the collapsed boolean.
     *
     * @param collapse When set to true, the contentPanel is collapsed, else it is expanded
     */
    public void setCollapsed(boolean collapse)
    {
        if (collapse)
        {
            //collapse the contentPanel, remove content and set border to empty border
            remove(contentPanel);
            arrow.setIcon(iconArrow[COLLAPSED]);
            border = collapsedBorder;
        } else
        {
            //expand the contentPanel, add content and set border to titled border
            add(contentPanel, BorderLayout.NORTH);
            arrow.setIcon(iconArrow[EXPANDED]);
            border = expandedBorder;
        }

        setBorder(border);
        collapsed = collapse;

        revalidate();
        repaint();

        firePropertyChange(PROP_COLLAPSED, !collapsed, collapsed);

    }

    /**
     * Returns the current state of the contentPanel, collapsed (true) or expanded (false).
     *
     * @return collapsed Returns true if the contentPanel is collapsed and false if it is expanded
     */
    public boolean isCollapsed()
    {
        return collapsed;
    }

    /**
     * Returns an ImageIcon array with arrow images used for the different states of the contentPanel.
     *
     * @return iconArrow An ImageIcon array holding the collapse and expanded versions of the right hand side arrow
     */
    private ImageIcon[] createExpandAndCollapseIcon()
    {
        final ImageIcon[] icons = new ImageIcon[2];
        icons[COLLAPSED] = new ImageIcon(getClass().getResource("resources/arrow_collapsed.png"));
        icons[EXPANDED] = new ImageIcon(getClass().getResource("resources/arrow_expanded.png"));
        return icons;
    }

    /**
     * Returns a button with an arrow icon and a collapse/expand action listener.
     *
     * @return button Button which is used in the titled border component
     */
    private JButton createArrowButton()
    {
        JButton button = new JButton("arrow", iconArrow[COLLAPSED]);
        button.setBorder(BorderFactory.createEmptyBorder(0, 1, 5, 1));
        button.setVerticalTextPosition(AbstractButton.CENTER);
        button.setHorizontalTextPosition(AbstractButton.LEFT);
        button.setMargin(new Insets(0, 0, 3, 0));

        //We want to use the same font as those in the titled border font
        Font font = BorderFactory.createTitledBorder("Sample").getTitleFont();
        Color color = BorderFactory.createTitledBorder("Sample").getTitleColor();
        button.setFont(font);
        button.setForeground(color);
        button.setFocusable(false);
        button.setContentAreaFilled(false);

        button.addActionListener(new CollapsiblePanel.ExpandAndCollapseAction());

        return button;
    }

    /**
     * Handles expanding and collapsing of extra content on the user's click of the titledBorder component.
     */
    private class ExpandAndCollapseAction extends AbstractAction implements ActionListener, ItemListener
    {

        @Override
        public void actionPerformed(ActionEvent e)
        {
            setCollapsed(!isCollapsed());
        }

        @Override
        public void itemStateChanged(ItemEvent e)
        {
            setCollapsed(!isCollapsed());
        }
    }

    /**
     * Special titled border that includes a component in the title area
     */
    private class CollapsableTitledBorder extends TitledBorder
    {

        JComponent component;

        public CollapsableTitledBorder(JComponent component)
        {
            this(null, component, LEFT, TOP);
        }

        public CollapsableTitledBorder(Border border)
        {
            this(border, null, LEFT, TOP);
        }

        public CollapsableTitledBorder(Border border, JComponent component)
        {
            this(border, component, LEFT, TOP);
        }

        public CollapsableTitledBorder(Border border, JComponent component, int titleJustification, int titlePosition)
        {
            //TitledBorder needs border, title, justification, position, font, and color
            super(border, null, titleJustification, titlePosition, null, null);
            this.component = component;
            if (border == null)
            {
                this.border = super.getBorder();
            }
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height)
        {
            Rectangle borderR = new Rectangle(x + EDGE_SPACING, y + EDGE_SPACING, width - (EDGE_SPACING * 2),
                    height - (EDGE_SPACING * 2));
            Insets borderInsets;
            if (border != null)
            {
                borderInsets = border.getBorderInsets(c);
            } else
            {
                borderInsets = new Insets(0, 0, 0, 0);
            }

            Rectangle rect = new Rectangle(x, y, width, height);
            Insets insets = getBorderInsets(c);
            Rectangle compR = getComponentRect(rect, insets);
            int diff;
            switch (titlePosition)
            {
                case ABOVE_TOP ->
                {
                    diff = compR.height + TEXT_SPACING;
                    borderR.y += diff;
                    borderR.height -= diff;
                }
                case TOP, DEFAULT_POSITION ->
                {
                    diff = insets.top / 2 - borderInsets.top - EDGE_SPACING;
                    borderR.y += diff;
                    borderR.height -= diff;
                }
                case BELOW_TOP, ABOVE_BOTTOM ->
                {
                }
                case BOTTOM ->
                {
                    diff = insets.bottom / 2 - borderInsets.bottom - EDGE_SPACING;
                    borderR.height -= diff;
                }
                case BELOW_BOTTOM ->
                {
                    diff = compR.height + TEXT_SPACING;
                    borderR.height -= diff;
                }
            }
            border.paintBorder(c, g, borderR.x, borderR.y, borderR.width, borderR.height);
            Color col = g.getColor();
            g.setColor(c.getBackground());
            g.fillRect(compR.x, compR.y, compR.width, compR.height);
            g.setColor(col);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets)
        {
            Insets borderInsets;
            if (border != null)
            {
                borderInsets = border.getBorderInsets(c);
            } else
            {
                borderInsets = new Insets(0, 0, 0, 0);
            }
            insets.top = EDGE_SPACING + TEXT_SPACING + borderInsets.top;
            insets.right = EDGE_SPACING + TEXT_SPACING + borderInsets.right;
            insets.bottom = EDGE_SPACING + TEXT_SPACING + borderInsets.bottom;
            insets.left = EDGE_SPACING + TEXT_SPACING + borderInsets.left;

            if (c == null || component == null)
            {
                return insets;
            }

            int compHeight = component.getPreferredSize().height;

            switch (titlePosition)
            {
                case ABOVE_TOP -> insets.top += compHeight + TEXT_SPACING;
                case TOP, DEFAULT_POSITION -> insets.top += Math.max(compHeight, borderInsets.top) - borderInsets.top;
                case BELOW_TOP -> insets.top += compHeight + TEXT_SPACING;
                case ABOVE_BOTTOM -> insets.bottom += compHeight + TEXT_SPACING;
                case BOTTOM -> insets.bottom += Math.max(compHeight, borderInsets.bottom) - borderInsets.bottom;
                case BELOW_BOTTOM -> insets.bottom += compHeight + TEXT_SPACING;
            }
            return insets;
        }

        public JComponent getTitleComponent()
        {
            return component;
        }

        public void setTitleComponent(JComponent component)
        {
            this.component = component;
        }

        public Rectangle getComponentRect(Rectangle rect, Insets borderInsets)
        {
            Dimension compD = component.getPreferredSize();
            Rectangle compR = new Rectangle(0, 0, compD.width, compD.height);
            switch (titlePosition)
            {
                case ABOVE_TOP -> compR.y = EDGE_SPACING;
                case TOP, DEFAULT_POSITION ->
                {
                    if (titleComponent instanceof JButton)
                    {
                        compR.y = EDGE_SPACING + (borderInsets.top - EDGE_SPACING - TEXT_SPACING - compD.height) / 2;
                    } else if (titleComponent instanceof JRadioButton)
                    {
                        compR.y = (borderInsets.top - EDGE_SPACING - TEXT_SPACING - compD.height) / 2;
                    }
                }
                case BELOW_TOP -> compR.y = borderInsets.top - compD.height - TEXT_SPACING;
                case ABOVE_BOTTOM -> compR.y = rect.height - borderInsets.bottom + TEXT_SPACING;
                case BOTTOM -> compR.y = rect.height - borderInsets.bottom + TEXT_SPACING
                            + (borderInsets.bottom - EDGE_SPACING - TEXT_SPACING - compD.height) / 2;
                case BELOW_BOTTOM -> compR.y = rect.height - compD.height - EDGE_SPACING;
            }
            switch (titleJustification)
            {
                case LEFT, DEFAULT_JUSTIFICATION -> //compR.x = TEXT_INSET_H + borderInsets.left;
                    compR.x = TEXT_INSET_H + borderInsets.left - EDGE_SPACING;
                case RIGHT -> compR.x = rect.width - borderInsets.right - TEXT_INSET_H - compR.width;
                case CENTER -> compR.x = (rect.width - compR.width) / 2;
            }
            return compR;
        }
    }
}

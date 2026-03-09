/*
 * 
 *   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 *   Copyright @2019 Jerome Lelasseux. All rights reserved.
 * 
 *   This file is part of the JJazzLab software.
 *    
 *   JJazzLab is free software: you can redistribute it and/or modify
 *   it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *   as published by the Free Software Foundation, either version 3 of the License, 
 *   or (at your option) any later version.
 * 
 *   JJazzLab is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *  
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *  
 *   Contributor(s): 
 * 
 */
package org.jjazz.uiutilities.api;

import com.google.common.base.Preconditions;
import java.awt.Point;
import java.util.Objects;
import javax.swing.JComponent;
import javax.swing.JToolTip;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.Timer;

/**
 * A service to show a custom tooltip with direct control.
 */
public class ValueToolTip
{

    public static final int DEFAULT_DELAY_MS = 700;
    private static final Timer timer = new Timer(0, e -> hide());
    private static JComponent component;
    private static Popup popup;
    private static String lastText;
    private static final JToolTip toolTip = new JToolTip();
    // private static final Logger LOGGER = Logger.getLogger(ValueToolTip.class.getSimpleName());

    /**
     * Immediatly show a tooltip, and hide it after the default delay.
     * <p>
     * If method is called again before delay is expired and the text has changed, text is updated and delay is restarted.
     *
     * @param comp
     * @param text
     */
    static synchronized public void show(JComponent comp, String text)
    {
        show(comp, text, DEFAULT_DELAY_MS);
    }

    /**
     * Immediatly show a tooltip, and hide it after the specified delay.
     * <p>
     * If method is called again before delay is expired and the text has changed, text is updated and delay is restarted.
     *
     * @param comp
     * @param text
     * @param hideDelay In milliseconds
     */
    static synchronized public void show(JComponent comp, String text, int hideDelay)
    {
        Objects.requireNonNull(comp);
        Objects.requireNonNull(text);
        Preconditions.checkArgument(hideDelay >= 0, "hideDelay=%s", hideDelay);

        if (component != null && comp != component)
        {
            // Special case: cancel current tooltip display
            timer.stop();
            hide();
            component = null;
        }

        if (component == null)
        {
            // Initialize
            component = comp;
            showToolTip(text, component);

            timer.setInitialDelay(hideDelay);
            timer.setRepeats(false);
            timer.start();

        } else if (!Objects.equals(lastText, text))
        {
            // Same component with a different text: update and restart delay
            
            // Make sure tooltip size/location are OK
            if (lastText != null && text.length() != lastText.length())
            {            
                popup.hide();
                showToolTip(text, component);
            }

            toolTip.setTipText(text);
            timer.setInitialDelay(hideDelay);
            timer.restart();
        }

        lastText = text;
    }

    static synchronized public void hide()
    {
        if (popup != null)
        {
            popup.hide();
            popup = null;
            component = null;
        }
    }

    static synchronized public boolean isVisible()
    {
        return popup != null;
    }

    // ==================================================================================================================
    // Private methods
    // ==================================================================================================================    
    static private void showToolTip(String text, JComponent comp)
    {
        toolTip.setTipText(text);
        Point p = computeToolTipPosition(toolTip, comp);
        popup = PopupFactory.getSharedInstance().getPopup(null, toolTip, p.x, p.y);
        popup.show();
    }

    /**
     * Centered above component.
     *
     * @param toolTip
     * @param comp
     * @return
     */
    static private Point computeToolTipPosition(JToolTip toolTip, JComponent comp)
    {
        var ttBounds = toolTip.getBounds();
        var cBounds = comp.getBounds();
        var cPoint = comp.getLocationOnScreen();
        int cCenterX = cPoint.x + cBounds.width / 2;
        int x = cCenterX - ttBounds.width / 2;
        int y = cPoint.y - ttBounds.height - 2;
        return new Point(x, y);
    }
}

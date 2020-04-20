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
package org.jjazz.ui.rpviewer;

import java.awt.Dimension;
import org.jjazz.ui.rpviewer.api.RpViewer;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.function.Function;
import java.util.logging.Logger;
import org.jjazz.rhythm.parameters.RhythmParameter;
import org.jjazz.songstructure.api.SongPart;

/**
 * A simple editor: just display RP value as a string.
 */
public class StringRpViewer extends RpViewer implements PropertyChangeListener
{

    private static final int STRING_HBORDER_PREF_SIZE = 10;
    /**
     * Extra border to not appear vertically centered because of the rp name label managed by superclass
     */
    private static final int TOP_BORDER = 2;
    // UI variables
    private final StringRpViewerSettings settings;
    private final Function<Object, String> formatter;
    private static final Logger LOGGER = Logger.getLogger(StringRpViewer.class.getSimpleName());

    /**
     *
     * @param spt
     * @param rp
     * @param formatter The string formatter of the rp value
     */
    public StringRpViewer(SongPart spt, RhythmParameter<?> rp, Function<Object, String> formatter)
    {
        super(spt, rp);
        if (formatter == null)
        {
            throw new IllegalArgumentException("spt=" + spt + " rp=" + rp + " formatter=" + formatter);
        }
        settings = StringRpViewerSettings.getDefault();
        settings.addPropertyChangeListener(this);
        this.formatter = formatter;
    }

    /**
     * Preferred size depends on displayed string's size and zoomVFactor for height.
     * <p>
     * @return
     */
    @Override
    public Dimension getPreferredSize()
    {
        // Calculate preferred size from string bounds
        Insets ins = this.getInsets();
        String strValue = formatter.apply(getSptModel().getRPValue(getRpModel()));
        FontMetrics fontMetrics = getFontMetrics(settings.getFont());
        int strWidth = fontMetrics.stringWidth(strValue);
        int strHeight = fontMetrics.getMaxAscent() + fontMetrics.getMaxDescent();
        int stringVborderSize = strHeight;

        Dimension prefSize = new Dimension();
        prefSize.width = strWidth + ins.left + ins.right + 2 * STRING_HBORDER_PREF_SIZE;
        prefSize.height = strHeight + TOP_BORDER + ins.top + ins.bottom;
        prefSize.height += Math.round(1.5f * stringVborderSize * getZoomVFactor() / 100f) + 0.75f * stringVborderSize;

        // Make sure size is not smaller than super's preferred size
        Dimension superPrefSize = super.getPreferredSize();
        prefSize.width = Math.max(superPrefSize.width, prefSize.width);
        prefSize.height = Math.max(superPrefSize.height, prefSize.height);

        return prefSize;
    }

    @Override
    public void cleanup()
    {
        super.cleanup();
        settings.removePropertyChangeListener(this);
    }

    @Override
    protected void valueChanged()
    {
        repaint();
    }

    /**
     * Paint the RP value as a string centered.
     *
     * @param g
     */
    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setFont(settings.getFont());
        g2.setColor(settings.getFontColor());

        Insets ins = getInsets();
        String strValue = formatter.apply(getSptModel().getRPValue(getRpModel()));
        FontMetrics fontMetrics = getFontMetrics(settings.getFont());
        int strWidth = fontMetrics.stringWidth(strValue);
        int strHeight = fontMetrics.getMaxAscent() + fontMetrics.getMaxDescent();
        float x = ins.left + (getWidth() - strWidth - ins.left - ins.right) / 2f;
        if (x < ins.left)
        {
            x = ins.left;
        }
        float y = ins.top + TOP_BORDER + (getHeight() - strHeight - ins.top - ins.bottom) / 2f;
        y += fontMetrics.getMaxAscent();
        g2.drawString(strValue, x, y);
    }

    // ---------------------------------------------------------------
    // Implements the PropertyChangeListener interface
    // ---------------------------------------------------------------    
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        super.propertyChange(evt);
        if (evt.getSource() == settings)
        {
            revalidate();
            repaint();
        }
    }

    // ---------------------------------------------------------------
    // Private methods
    // ---------------------------------------------------------------    
}

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

import org.jjazz.ui.rpviewer.api.RpViewer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import org.jjazz.rhythm.parameters.RP_Integer;
import org.jjazz.rhythm.parameters.RhythmParameter;
import static org.jjazz.ui.flatcomponents.FlatIntegerVerticalSlider.PROP_COLOR_BOTTOM;
import static org.jjazz.ui.flatcomponents.FlatIntegerVerticalSlider.PROP_COLOR_TOP;
import org.jjazz.songstructure.api.SongPart;

/**
 * Display the value as a vertical meter with max 10 leds of 3 colors.
 * <p>
 * Sensitive to zoomVFactor.
 */
public class MeterRpViewer extends RpViewer
{

    private final static Color GREEN = Color.GREEN;
    private final static Color YELLOW = Color.ORANGE;
    private final static Color RED = Color.RED;
    private final static Dimension STD_SIZE = new Dimension(60, 40);
    /**
     * Extra height added to the minimum size when vertical zoom is at default value.
     */
    private static final int EXTRA_HEIGHT_ZOOM_50 = 15;

    public MeterRpViewer(SongPart spt, RhythmParameter<?> rp)
    {
        super(spt, rp);
    }

    @Override
    protected void valueChanged()
    {
        repaint();
    }

    /**
     * Preferred size depends on STD_SIZE and getZoomVFactor() for height.
     * <p>
     * If vFactor = 50% = add EXTRA_HEIGHT_ZOOM_50 points to STD_SIZE height<br>
     * If vFactor = 0% = use the STD_SIZE height<br>
     * If vFactor = 100% = add 2*EXTRA_HEIGHT_ZOOM_50 points to STD_SIZE height<br>
     *
     * @return
     */
    @Override
    public Dimension getPreferredSize()
    {
        Dimension d = new Dimension();
        d.width = STD_SIZE.width;
        d.height = STD_SIZE.height + (int) ((getZoomVFactor() * 2f * EXTRA_HEIGHT_ZOOM_50) / 100f);
        return d;
    }

    @SuppressWarnings(
            {
                "unchecked", "rawtypes"
            })
    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        RhythmParameter rp = getRpModel();
        SongPart spt = getSptModel();
        Object value = spt.getRPValue(rp);
        double pValue = rp.calculatePercentage(value);

        final int LED_WIDTH = 28;
        final int LED_HEIGHT = 2;
        final int V_BORDER = 2;
        final int H_BORDER = 2;
        final int V_LED_GAP = 1;
        final Color PROP_COLOR_BOTTOM = new Color(0, 135, 255);
        final Color PROP_COLOR_TOP = new Color(253, 0, 255);

        Insets in = getInsets();
        int h = this.getHeight() - in.bottom - in.top - 2 * V_BORDER;     // available height
        int w = this.getWidth() - in.left - in.right - 2 * H_BORDER;      // available width
        int ledWidth = Math.min(LED_WIDTH, w);
        int ledHeight = LED_HEIGHT;

        int x = in.left + H_BORDER + w / 2 - ledWidth / 2;       // Centered
        int yFirstLed = getHeight() - in.bottom - V_BORDER - 1 - ledHeight;    // y for the first bottom lead
        int maxNbLeds = h / (ledHeight + V_LED_GAP);
        int nbLeds = (int) Math.round(pValue * maxNbLeds);

        GradientPaint gp = new GradientPaint(0, getHeight() - in.bottom - V_BORDER, PROP_COLOR_BOTTOM, 0, in.top + V_BORDER, PROP_COLOR_TOP);

        if (rp instanceof RP_Integer)
        {
            // Special case it's RP_Integer: we have more information to exploit, ie we can handle the case -min/+max case
            RP_Integer rpi = (RP_Integer) rp;
            int intValue = (Integer) value;
            int minValue = rpi.getMinValue();
            int maxValue = rpi.getMaxValue();
            int valueRange = maxValue - minValue + 1;
            if (maxValue > 0 && minValue <= 0)
            {
                // We can draw a "0" line
                double p0Value = rpi.calculatePercentage(0);
                int nbLeds0 = (int) Math.round(p0Value * maxNbLeds);
                int yline0 = yFirstLed + ledHeight - (ledHeight + V_LED_GAP) * nbLeds0;
                int x1line0 = x - 10;
                x1line0 = Math.max(x1line0, in.left + 1);
                int x2line0 = x + ledWidth + 10;
                x2line0 = Math.min(x2line0, getWidth() - in.right - 2);
                g2.setColor(Color.LIGHT_GRAY.darker());
                g2.drawLine(x1line0, yline0, x2line0, yline0);
                g2.setPaint(gp);
                if (intValue > 0)
                {
                    int y = yline0 - ledHeight;
                    float pPositive = intValue / (float) maxValue;
                    int maxNbLedsPositive = Math.round(maxNbLeds * (float) maxValue / valueRange);
                    int nbLedsPositive = Math.round(pPositive * maxNbLedsPositive);
                    for (int i = 1; i <= nbLedsPositive; i++)
                    {
                        g2.fillRect(x, y, ledWidth, ledHeight);
                        y -= (ledHeight + V_LED_GAP);
                    }
                } else
                {
                    int y = yline0 + 1;
                    float pNegative = intValue / (float) minValue;
                    int maxNbLedsNegative = Math.round(maxNbLeds * (float) -minValue / valueRange);
                    int nbLedsNegative = Math.round(pNegative * maxNbLedsNegative);
                    for (int i = 1; i <= nbLedsNegative; i++)
                    {
                        g2.fillRect(x, y, ledWidth, ledHeight);
                        y += (ledHeight + V_LED_GAP);
                    }
                }

            } else
            {
                // Like for any RhythmParameter
                g2.setPaint(gp);
                int y = yFirstLed;
                for (int i = 1; i <= nbLeds; i++)
                {
                    g2.fillRect(x, y, ledWidth, ledHeight);
                    y -= (ledHeight + V_LED_GAP);
                }
            }
        } else
        {
            // Any RhythmParameter
            g2.setPaint(gp);
            int y = yFirstLed;
            for (int i = 1; i <= nbLeds; i++)
            {
                g2.fillRect(x, y, ledWidth, ledHeight);
                y -= (ledHeight + V_LED_GAP);
            }
        }
    }

//    @Override
//    public void paintComponent2(Graphics g)
//    {
//        super.paintComponent(g);
//
//        Graphics2D g2 = (Graphics2D) g;
//        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
//        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//        RhythmParameter rp = getRpModel();
//        SongPart spt = getSptModel();
//        double pValue = rp.calculatePercentage(spt.getRPValue(rp));
//
//        final int V_BORDER = 4;
//        final int H_BORDER = 5;
//        final int V_LED_GAP = 1;
//        Insets in = getInsets();
//        in.bottom += V_BORDER;
//        in.top += V_BORDER;
//        in.left += H_BORDER;
//        in.right += H_BORDER;
//        int h = this.getHeight() - in.bottom - in.top;
//        int w = this.getWidth() - in.left - in.top;
//        double ledH = (h - 9f * V_LED_GAP) / 10f;
//        double ledW = w;
//        Rectangle2D.Double led = new Rectangle2D.Double(in.left, getHeight() - in.bottom - 1, ledW, ledH);
//
//        int count = 1;
//        Color c;
//        while (pValue > 0)
//        {
//            if (count <= 4)
//            {
//                c = GREEN;
//            } else if (count <= 7)
//            {
//                c = YELLOW;
//            } else
//            {
//                c = RED;
//            }
//            g2.setColor(c);
//            g2.fill(led);
//            led.y -= led.height + V_LED_GAP;
//            count++;
//            pValue -= 0.1;
//        }
//    }
    // ---------------------------------------------------------------
    // Private functions
    // ---------------------------------------------------------------    
}

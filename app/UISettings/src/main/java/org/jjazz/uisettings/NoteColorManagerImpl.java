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
package org.jjazz.uisettings;

import com.google.common.base.Preconditions;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import org.jjazz.uisettings.api.NoteColorManager;
import org.jjazz.uiutilities.api.HSLColor;


public class NoteColorManagerImpl implements NoteColorManager
{

    static private NoteColorManagerImpl INSTANCE;
    private Color[] VELOCITY_COLORS;
    private Color[] SELECTED_VELOCITY_COLORS;

    static public NoteColorManagerImpl getInstance()
    {
        synchronized (NoteColorManagerImpl.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new NoteColorManagerImpl();
            }
        }
        return INSTANCE;
    }

    private NoteColorManagerImpl()
    {
    }

    @Override
    public Color getNoteColor(Color refColor, int velocity)
    {
        HSLColor hsl = new HSLColor(refColor);
        float lum = hsl.getLuminance();
        float lumMaxDelta = 30;
        float lumDelta = (velocity - 64) * lumMaxDelta / 64f;
        lum += lumDelta;
        lum = Math.min(100f, lum);
        lum = Math.max(0f, lum);
        Color c = hsl.adjustLuminance(lum);
        return c;
    }

    @Override
    public Color getNoteColor(int velocity)
    {
        Preconditions.checkArgument(velocity >= 0 && velocity <= 127);
        if (VELOCITY_COLORS == null)
        {
            computeVelocityColors();
        }
        return VELOCITY_COLORS[velocity];
    }

    @Override
    public Color getSelectedNoteColor(int velocity)
    {
        Preconditions.checkArgument(velocity >= 0 && velocity <= 127);
        if (SELECTED_VELOCITY_COLORS == null)
        {
            computeVelocityColors();
        }
        return SELECTED_VELOCITY_COLORS[velocity];
    }

    // =============================================================================================================
    // Private methods
    // =============================================================================================================

    /**
     * Pre-calculate all the velocity colors from 0 to 127.
     */
    private void computeVelocityColors()
    {
        BufferedImage img = new BufferedImage(128, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        Point2D start = new Point2D.Float(0, 0);

        // Use 128 instead of 127: 1 pixel more to avoid strange error with LinearGradientPaint with last pixel (x=127) black with the selected color
        Point2D end = new Point2D.Float(128, 0);
        float[] dist =
        {
            0.0f, 0.5f, 1.0f
        };


        Color[] colors =
        {
            new Color(2, 0, 252), new Color(128, 0, 126), new Color(255, 0, 0)
        };
        LinearGradientPaint p = new LinearGradientPaint(start, end, dist, colors);
        g2.setPaint(p);
        g2.fillRect(0, 0, 128, 1);
        VELOCITY_COLORS = new Color[128];
        for (int i = 0; i < 128; i++)
        {
            int cInt = img.getRGB(i, 0);
            VELOCITY_COLORS[i] = new Color(cInt);
        }


        Color sc = new Color(220, 220, 0);  // Yellowish
        Color[] selectedcolors =
        {
            // HSLColor.changeLuminance(sc, -18), HSLColor.changeLuminance(sc, -9), sc
            HSLColor.changeLuminance(sc, -30), HSLColor.changeLuminance(sc, -15), sc
        };
        LinearGradientPaint pSelected = new LinearGradientPaint(start, end, dist, selectedcolors);
        g2.setPaint(pSelected);
        g2.fillRect(0, 0, 128, 1);
        SELECTED_VELOCITY_COLORS = new Color[128];
        for (int i = 0; i < 128; i++)
        {
            int cInt = img.getRGB(i, 0);
            SELECTED_VELOCITY_COLORS[i] = new Color(cInt);
        }


        g2.dispose();

    }


}

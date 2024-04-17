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

import java.awt.Color;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;
import javax.swing.event.SwingPropertyChangeSupport;
import org.openide.util.NbPreferences;
import org.jjazz.uisettings.api.ColorSetManager;
import org.jjazz.upgrade.api.UpgradeManager;
import org.jjazz.upgrade.api.UpgradeTask;
import org.openide.util.lookup.ServiceProvider;

public class ColorSetManagerImpl implements ColorSetManager
{

    // COLORS
    private final Color c1 = new Color(206, 193, 155);
    private final Color c2 = new Color(180, 124, 102);
    private final Color c3 = new Color(204, 149, 104);
    private final Color c4 = new Color(110, 120, 104);
    private final Color c5 = new Color(178, 155, 136);
    private final Color c6 = new Color(151, 202, 195);
    // Obtained from Paletton.com http://paletton.com/#uid=7000u0kbRt14+E48dwffUpTkImm    
//    private final Color c1 = new Color(0x63, 0x76, 0x8F);
//    private final Color c2 = new Color(0xAD, 0x8E, 0x92);
//    private final Color c3 = new Color(0xDB, 0xAF, 0x79);
//    private final Color c4 = new Color(0xDFC889);
//    private final Color c5 = new Color(0xD7896E);
//    private final Color c6 = new Color(0x984B3F);
    // private final Color WHITE = new Color(251, 248, 245);        // "our" white = "old paper"
    // private final Color WHITE = new Color(235, 232, 225);        // "our" white for FlatLAF
    // 
    // Application variables -
    private static final int NB_COLORS = 6;
    private static final String COLOR_PROP_PREFIX = "REFCOLOR-";
    private static ColorSetManagerImpl INSTANCE;
    /**
     * Store the reference colors.
     */
    private static final Preferences prefs = NbPreferences.forModule(ColorSetManagerImpl.class);
    /**
     * Associate an identifier to a color index.
     */
    private final Map<String, Integer> mapIdColor = new HashMap<>();
    /**
     * Current colorIndex
     */
    private int colorIndex;
    /**
     * Listeners for reference colors changes.
     */
    private final SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);

    static public ColorSetManagerImpl getInstance()
    {
        synchronized (ColorSetManagerImpl.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new ColorSetManagerImpl();
            }
        }
        return INSTANCE;
    }

    private ColorSetManagerImpl()
    {
        // Nothing
    }

    @Override
    public Color getColor(String id)
    {
        Integer index = mapIdColor.get(id.toUpperCase());
        if (index == null)
        {
            colorIndex = (colorIndex + 1) % NB_COLORS;
            index = colorIndex;
            mapIdColor.put(id.toUpperCase(), colorIndex);
        }
        return getReferenceColors().get(index);
    }

    @Override
    public void resetColor(String id)
    {
        Integer index = mapIdColor.get(id.toUpperCase());
        if (index != null)
        {
            mapIdColor.remove(id.toUpperCase());
        }
    }

    @Override
    public List<Color> getReferenceColors()
    {
        ArrayList<Color> res = new ArrayList<>(NB_COLORS);
        for (int i = 0; i < NB_COLORS; i++)
        {
            res.add(getReferenceColor(i));
        }
        return res;
    }


    @Override
    public boolean isReferenceColor(Color c)
    {
        return getReferenceColors().contains(c);
    }

    @Override
    public Color getReferenceColor(int index)
    {
        Color c = switch (index)
        {
            case 0 ->
                new Color(prefs.getInt(COLOR_PROP_PREFIX + index, c1.getRGB()));
            case 1 ->
                new Color(prefs.getInt(COLOR_PROP_PREFIX + index, c2.getRGB()));
            case 2 ->
                new Color(prefs.getInt(COLOR_PROP_PREFIX + index, c3.getRGB()));
            case 3 ->
                new Color(prefs.getInt(COLOR_PROP_PREFIX + index, c4.getRGB()));
            case 4 ->
                new Color(prefs.getInt(COLOR_PROP_PREFIX + index, c5.getRGB()));
            case 5 ->
                new Color(prefs.getInt(COLOR_PROP_PREFIX + index, c6.getRGB()));
            default -> throw new IllegalArgumentException("index=" + index + " NB_COLORS=" + NB_COLORS);
        };
        return c;
    }

    @Override
    public void setReferenceColor(int index, Color c)
    {
        if (c == null || index < 0 || index >= NB_COLORS)
        {
            throw new IllegalArgumentException("index=" + index + " c=" + c);
        }
        Color oldColor = getReferenceColor(index);
        prefs.putInt(COLOR_PROP_PREFIX + index, c.getRGB());
        pcs.firePropertyChange(PROP_REF_COLOR_CHANGED, oldColor, c);
    }

    @Override
    public synchronized void addPropertyChangeListener(PropertyChangeListener listener)
    {
        pcs.addPropertyChangeListener(listener);
    }

    @Override
    public synchronized void removePropertyChangeListener(PropertyChangeListener listener)
    {
        pcs.removePropertyChangeListener(listener);
    }


}

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
package org.jjazz.ui.colorsetmanager;

import java.awt.Color;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.util.api.SmallMap;
import org.openide.util.NbPreferences;
import org.jjazz.ui.colorsetmanager.api.ColorSetManager;
import org.jjazz.upgrade.api.UpgradeManager;
import org.jjazz.upgrade.spi.UpgradeTask;
import org.openide.util.lookup.ServiceProvider;

public class ColorSetManagerImpl implements ColorSetManager
{

    // COLORS
    private final Color c1 = new Color(0xCE9668);
    private final Color c2 = new Color(0xCE6868);
    private final Color c3 = new Color(204, 149, 104);
    private final Color c4 = new Color(206, 193, 155);
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
    private SmallMap<String, Integer> mapIdColor = new SmallMap<>();
    /**
     * Current colorIndex
     */
    private int colorIndex;
    /**
     * Listeners for reference colors changes.
     */
    private SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);

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
        Integer index = mapIdColor.getValue(id.toUpperCase());
        if (index == null)
        {
            colorIndex = (colorIndex + 1) % NB_COLORS;
            index = colorIndex;
            mapIdColor.putValue(id.toUpperCase(), colorIndex);
        }
        return getReferenceColors().get(index);
    }

    @Override
    public void resetColor(String id)
    {
        Integer index = mapIdColor.getValue(id.toUpperCase());
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
    public Color getReferenceColor(int index)
    {
        Color c;
        switch (index)
        {
            case 0:
                c = new Color(prefs.getInt(COLOR_PROP_PREFIX + index, c1.getRGB()));
                break;
            case 1:
                c = new Color(prefs.getInt(COLOR_PROP_PREFIX + index, c2.getRGB()));
                break;
            case 2:
                c = new Color(prefs.getInt(COLOR_PROP_PREFIX + index, c3.getRGB()));
                break;
            case 3:
                c = new Color(prefs.getInt(COLOR_PROP_PREFIX + index, c4.getRGB()));
                break;
            case 4:
                c = new Color(prefs.getInt(COLOR_PROP_PREFIX + index, c5.getRGB()));
                break;
            case 5:
                c = new Color(prefs.getInt(COLOR_PROP_PREFIX + index, c6.getRGB()));
                break;
            default:
                throw new IllegalArgumentException("index=" + index + " NB_COLORS=" + NB_COLORS);   //NOI18N
        }
        return c;
    }

    @Override
    public void setReferenceColor(int index, Color c)
    {
        if (c == null || index < 0 || index >= NB_COLORS)
        {
            throw new IllegalArgumentException("index=" + index + " c=" + c);   //NOI18N
        }
        Color oldColor = getReferenceColor(index);
        prefs.putInt(COLOR_PROP_PREFIX + index, c.getRGB());
        pcs.firePropertyChange(PROP_REF_COLORS_CHANGED, oldColor, c);
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

    // =====================================================================================
    // Upgrade Task
    // =====================================================================================
    @ServiceProvider(service = UpgradeTask.class)
    static public class RestoreSettingsTask implements UpgradeTask
    {

        @Override
        public void upgrade(String oldVersion)
        {
            UpgradeManager um = UpgradeManager.getInstance();
            um.duplicateOldPreferences(prefs);
        }

    }

}

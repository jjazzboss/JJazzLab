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
package org.jjazz.test;

/**
 *
 * @author Jerome
 */

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.swing.JComponent;
import org.netbeans.swing.tabcontrol.TabbedContainer;
import org.netbeans.swing.tabcontrol.WinsysInfoForTabbedContainer;
import org.netbeans.swing.tabcontrol.customtabs.Tabbed;
import org.netbeans.swing.tabcontrol.customtabs.TabbedComponentFactory;
import org.netbeans.swing.tabcontrol.customtabs.TabbedType;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

// @ServiceProvider(service = TabbedComponentFactory.class, position = 0)
public class JJazzLabTabbedComponentFactory implements TabbedComponentFactory, MouseMotionListener
{

    private static final Logger LOGGER = Logger.getLogger(JJazzLabTabbedComponentFactory.class.getSimpleName());
    private static TabbedComponentFactory INSTANCE;
    private Map<Component, Tabbed> mapCompTabbed = new HashMap<>();

    @Override
    public Tabbed createTabbedComponent(TabbedType type, WinsysInfoForTabbedContainer info)
    {
        if (INSTANCE == null)
        {
            var factories = Lookup.getDefault().lookupAll(TabbedComponentFactory.class);
            for (var f : factories)
            {
                if (f != this)
                {
                    LOGGER.severe("createTabbedComponent()  using factory=" + INSTANCE);
                    INSTANCE = f;
                }
            }

        }
        Tabbed res = INSTANCE.createTabbedComponent(type, info);
        LOGGER.severe("createTabbedComponent() created type="+type.name()+" res=" + res);
        if (type.toInt() != TabbedContainer.TYPE_EDITOR)
        {
            return res;
        }
        Component c = res.getComponent();        
        mapCompTabbed.put(c, res);
        LOGGER.severe("createTabbedComponent() Associate c=" + c + " res=" + res);

        c.addMouseMotionListener(this);
        return res;
    }

    @Override
    public void mouseDragged(MouseEvent e)
    {
        // Nothing
    }

    @Override
    public void mouseMoved(MouseEvent e)
    {
        var c = (Component) e.getSource();
        LOGGER.severe("mouseMoved() -- c=" + c);
        var tabbed = mapCompTabbed.get(c);
        if (tabbed != null)
        {
            int tabIndex = tabbed.tabForCoordinate(e.getPoint());
            int tabCount = tabbed.getTabCount();
            var tcs = tabbed.getTopComponents();
            var tc = tabbed.getTopComponentAt(tabIndex);
            LOGGER.severe("mouseMoved() tabCount=" + tabCount + " tabIndex=" + tabIndex + " tc=" + tc.getDisplayName());
        }
    }


}

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
package org.jjazz.ui.mixconsole;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.util.logging.Logger;
import org.jjazz.midimix.api.UserRhythmVoice;
import org.jjazz.rhythm.api.RhythmVoice;

/**
 * A LayoutManager to arrange the MixChannelPanels and their UserChannelExtensionPanels in the MixConsole.
 */
public class MixConsoleLayoutManager implements LayoutManager
{

    private static final int H_PADDING = 3;
    private static final int V_PADDING = 2;
    private static final Logger LOGGER = Logger.getLogger(MixConsoleLayoutManager.class.getSimpleName());

    @Override
    public void layoutContainer(Container container)
    {
        Insets in = container.getInsets();
        int xMin = in.left + H_PADDING;
        int yMin = in.top + V_PADDING;
        int x = xMin;
        int y = yMin;


        // Process all MixChannelPanels first
        for (Component c : container.getComponents())       // Components are already ordered by Midi channel
        {
            if (c instanceof MixChannelPanel)
            {
                var mcp = (MixChannelPanel) c;
                Dimension pd = mcp.getPreferredSize();
                mcp.setSize(pd);
                mcp.setLocation(x, y);
                x += pd.width + H_PADDING;
            }
        }


        // Process UserExtensionChannelPanels
        for (Component c : container.getComponents())
        {
            if (c instanceof UserExtensionPanel)
            {
                var ucep = (UserExtensionPanel) c;
                UserRhythmVoice urv = ucep.getUserRhythmVoice();


                // Find the corresponding MixChannelPanel
                MixChannelPanel mcp = getMixChannelPanel(container, urv);
                if (mcp == null)
                {
                    continue;
                }

                // Width must be identical to MixChannelPanel
                Dimension pd = ucep.getPreferredSize();
                ucep.setSize(mcp.getWidth(), pd.height);

                // Location is below MixChannelPanel
                x = mcp.getX();
                y = mcp.getY() + mcp.getHeight() + H_PADDING;
                ucep.setLocation(x, y);
            }
        }

    }

    @Override
    public void addLayoutComponent(String string, Component comp)
    {
        // Nothing
    }

    @Override
    public void removeLayoutComponent(Component comp)
    {
        // Nothing    
    }

    @Override
    public Dimension preferredLayoutSize(Container container)
    {
        Insets in = container.getInsets();
        int xMin = in.left + H_PADDING;
        int yMin = in.top + V_PADDING;
        int w = xMin;
        int h0 = yMin;


        // Process all MixChannelPanels first
        for (Component c : container.getComponents())
        {
            if (c instanceof MixChannelPanel)
            {
                var mcp = (MixChannelPanel) c;
                Dimension pd = mcp.getPreferredSize();
                w += pd.width + H_PADDING;
                h0 = Math.max(h0, yMin + pd.height + V_PADDING);
            }
        }

        w += in.right;
        int h1 = 0;

        // Process UserExtensionChannelPanels
        for (Component c : container.getComponents())
        {
            if (c instanceof UserExtensionPanel)
            {
                var ucep = (UserExtensionPanel) c;
                Dimension pd = ucep.getPreferredSize();
                h1 = Math.max(h1, pd.height + V_PADDING);
            }
        }

        int h = h0 + h1 + in.bottom;

        return new Dimension(w, h);
    }

    @Override
    public Dimension minimumLayoutSize(Container parent)
    {
        return new Dimension(100, 100);
    }


    // ===============================================================================
    // Private methods
    // ===============================================================================
    /**
     *
     * @param container
     * @param rv
     * @return Can be null
     */
    private MixChannelPanel getMixChannelPanel(Container container, RhythmVoice rv)
    {
        MixChannelPanel res = null;
        for (Component c : container.getComponents())
        {
            if (c instanceof MixChannelPanel)
            {
                var mcp = (MixChannelPanel) c;
                MixChannelPanelModelImpl model = (MixChannelPanelModelImpl) mcp.getModel();
                if (model.getRhythmVoice() == rv)
                {
                    res = mcp;
                    break;
                }
            }
        }
        return res;
    }
}

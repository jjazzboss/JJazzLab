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
package org.jjazz.improvisionsupport;

import static com.google.common.base.Preconditions.checkNotNull;
import java.beans.PropertyChangeListener;
import java.util.List;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongFactory;
import org.jjazz.ui.cl_editor.api.CL_Editor;
import org.jjazz.ui.cl_editor.barbox.api.BarBoxConfig;
import org.jjazz.util.api.ResUtil;

/**
 * A class to manage improvisation support for one CL_Editor.
 */
public class ImproSupport
{

    public final static String PROP_MODE = "PropMode";
    public final static String PROP_ENABLED = "PropEnabled";


    public enum Mode
    {
        PLAY_REST_EASY(ResUtil.getString(ImproSupport.class, "PlayRestEasyDisplayName"), ResUtil.getString(ImproSupport.class, "PlayRestHelpText")),
        PLAY_REST_MEDIUM(ResUtil.getString(ImproSupport.class, "PlayRestMediumDisplayName"), ResUtil.getString(ImproSupport.class, "PlayRestHelpText"));

        private final String displayName;
        private final String helpText;

        private Mode(String displayName, String helpText)
        {
            this.displayName = displayName;
            this.helpText = helpText;
        }

        @Override
        public String toString()
        {
            return displayName;
        }

        public String getHelpText()
        {
            return helpText;
        }
    }

    private final CL_Editor clEditor;
    private Mode mode;
    private boolean enabled;


    private final SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);

    public ImproSupport(CL_Editor clEditor)
    {
        checkNotNull(clEditor);
        this.clEditor = clEditor;
        this.mode = Mode.PLAY_REST_EASY;
        this.enabled = false;
    }

    public CL_Editor getCL_Editor()
    {
        return clEditor;
    }

    /**
     * Generate a new improvisation guide for the current mode.
     */
    public void generateGuide()
    {
        if (!enabled)
        {
            return;
        }

        switch (mode)
        {
            case PLAY_REST_EASY:
                generatePlayRest(PlayRestScenario.Level.EASY);
                break;
            case PLAY_REST_MEDIUM:
                generatePlayRest(PlayRestScenario.Level.MEDIUM);
                break;
            default:
                throw new AssertionError(mode.name());

        }

    }

    public Mode getMode()
    {
        return mode;
    }

    public void setMode(Mode mode)
    {
        if (this.mode.equals(mode))
        {
            return;
        }
        var old = this.mode;
        this.mode = mode;

//        switch (mode)
//        {
//            case PLAY_REST_EASY:
//                break;
//            case PLAY_REST_MEDIUM:
//                break;
//            default:
//                throw new AssertionError(mode.name());
//
//        }

        pcs.firePropertyChange(PROP_MODE, old, this.mode);
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        if (this.enabled == enabled)
        {
            return;
        }
        this.enabled = enabled;
        showImproSupportBarRenderer(enabled);
        pcs.firePropertyChange(PROP_ENABLED, !enabled, enabled);
    }

    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }

    //================================================================================================
    // Private methods
    //================================================================================================

    private void generatePlayRest(PlayRestScenario.Level level)
    {
        var scenario = new PlayRestScenario(level, clEditor.getSongModel());
        scenario.generate();
        
        List<BR_ImproSupport> brs = BR_ImproSupport.getBR_ImproSupportInstances(clEditor);
        for (var br : brs)
        {
            br.setScenario(scenario);
        }       
    }

    private void showImproSupportBarRenderer(boolean b)
    {
        for (int i = 0; i < clEditor.getNbBarBoxes(); i++)
        {
            BarBoxConfig bbc = clEditor.getBarBoxConfig(i);
            var activeBrTypes = bbc.getActiveBarRenderers();
            if (!b)
            {
                activeBrTypes.remove(ImproSupportBrProvider.BR_IMPRO_SUPPORT);
            } else
            {
                activeBrTypes.add(ImproSupportBrProvider.BR_IMPRO_SUPPORT);
            }
            bbc = bbc.setActive(activeBrTypes.toArray(new String[0]));
            clEditor.setBarBoxConfig(bbc, i);
        }
    }
}

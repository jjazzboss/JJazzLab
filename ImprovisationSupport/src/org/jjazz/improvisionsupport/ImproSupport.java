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
import java.util.ArrayList;
import java.util.List;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.improvisionsupport.PlayRestScenario.Value;
import org.jjazz.ui.cl_editor.api.CL_Editor;
import org.jjazz.ui.cl_editor.barbox.api.BarBoxConfig;
import org.jjazz.ui.cl_editor.barrenderer.api.BarRendererFactory;
import org.jjazz.util.api.ResUtil;

/**
 * A class to manage improvisation support for one CL_Editor.
 */
public class ImproSupport
{

    public final static String PROP_MODE = "PropMode";
    public final static String PROP_ENABLED = "PropEnabled";
    public final static String PROP_CHORD_POSITIONS_HIDDEN = "PropChordPositionsHidden";


    public enum Mode
    {
        PLAY_REST_EASY(ResUtil.getString(ImproSupport.class, "PlayRestEasyDisplayName"), ResUtil.getString(ImproSupport.class, "PlayRestEasyHelpText")),
        PLAY_REST_MEDIUM(ResUtil.getString(ImproSupport.class, "PlayRestMediumDisplayName"), ResUtil.getString(ImproSupport.class, "PlayRestMediumHelpText"));

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
    private PlayRestScenario scenario;
    private boolean enabled;
    private boolean chordPositionsHidden;


    private final SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);

    public ImproSupport(CL_Editor clEditor)
    {
        checkNotNull(clEditor);
        this.clEditor = clEditor;
        this.mode = Mode.PLAY_REST_EASY;
        this.enabled = false;
        this.chordPositionsHidden = true;
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

    public boolean isChordPositionsHidden()
    {
        return chordPositionsHidden;
    }

    public void setChordPositionsHidden(boolean chordPositionsHidden)
    {
        if (!enabled || this.chordPositionsHidden == chordPositionsHidden)
        {
            return;
        }

        this.chordPositionsHidden = chordPositionsHidden;

        showImproSupportBarRenderer(true, chordPositionsHidden);

        if (scenario != null)
        {
            getBarRenderers().forEach(br -> br.setScenario(scenario));
        }

        pcs.firePropertyChange(PROP_CHORD_POSITIONS_HIDDEN, !chordPositionsHidden, chordPositionsHidden);
    }

    public Mode getMode()
    {
        return mode;
    }

    public void setMode(Mode mode)
    {
        if (!enabled || this.mode.equals(mode))
        {
            return;
        }
        var old = this.mode;
        this.mode = mode;
        generateGuide();
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
        showImproSupportBarRenderer(enabled, chordPositionsHidden);
        if (this.enabled)
        {
            generateGuide();
        }
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

    private List<BR_ImproSupport> getBarRenderers()
    {
        return BR_ImproSupport.getBR_ImproSupportInstances(clEditor);
    }

    private void generatePlayRest(PlayRestScenario.Level level)
    {
        // Generate a scenario until it's different from the previous one (if any)
        List<Value> oldValues = scenario == null ? new ArrayList<>() : scenario.getPlayRestValues();
        List<Value> newValues = null;
        while (newValues == null || oldValues.equals(newValues))
        {
            scenario = new PlayRestScenario(level, clEditor.getSongModel());
            newValues = scenario.generatePlayRestValues();
        }

        getBarRenderers().forEach(br -> br.setScenario(scenario));
    }

    private void showImproSupportBarRenderer(boolean show, boolean hideChordPositions)
    {
        // Update the BarBoxConfig of each barbox
        for (int i = 0; i < clEditor.getNbBarBoxes(); i++)
        {
            BarBoxConfig bbc = clEditor.getBarBoxConfig(i);
            var activeBrTypes = bbc.getActiveBarRenderers();
            if (!show)
            {
                activeBrTypes.remove(ImproSupportBrProvider.BR_IMPRO_SUPPORT);
                if (!activeBrTypes.contains(BarRendererFactory.BR_CHORD_POSITION))
                {
                    activeBrTypes.add(1, BarRendererFactory.BR_CHORD_POSITION);
                }
            } else
            {
                if (!activeBrTypes.contains(ImproSupportBrProvider.BR_IMPRO_SUPPORT))
                {
                    activeBrTypes.add(1, ImproSupportBrProvider.BR_IMPRO_SUPPORT);
                }
                if (hideChordPositions)
                {
                    activeBrTypes.remove(BarRendererFactory.BR_CHORD_POSITION);
                } else if (!activeBrTypes.contains(BarRendererFactory.BR_CHORD_POSITION))
                {
                    activeBrTypes.add(2, BarRendererFactory.BR_CHORD_POSITION);
                }
            }
            bbc = bbc.setActive(activeBrTypes.toArray(new String[0]));
            clEditor.setBarBoxConfig(bbc, i);
        }

        // Udate all BarRenderers
        getBarRenderers().forEach(br -> br.setPlaybackPointEnabled(hideChordPositions));
    }
}

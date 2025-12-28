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
package org.jjazz.improvisionsupport;

import static com.google.common.base.Preconditions.checkNotNull;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.analytics.api.Analytics;
import org.jjazz.improvisionsupport.PlayRestScenario.DenseSparseValue;
import org.jjazz.improvisionsupport.PlayRestScenario.PlayRestValue;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.harmony.api.Position;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.musiccontrol.api.PlaybackListener;
import org.jjazz.song.api.Song;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.cl_editor.api.CL_Editor;
import org.jjazz.cl_editor.barbox.api.BarBoxConfig;
import org.jjazz.cl_editor.spi.BarRendererFactory;
import org.jjazz.musiccontrol.api.playbacksession.PlaybackSession;
import org.jjazz.song.api.SongMetaEvents;
import org.jjazz.utilities.api.ResUtil;

/**
 * A class to manage improvisation support for one CL_Editor.
 */
public class ImproSupport implements PropertyChangeListener
{

    public final static String PROP_MODE = "PropMode";
    public final static String PROP_ENABLED = "PropEnabled";
    public final static String PROP_CHORD_POSITIONS_HIDDEN = "PropChordPositionsHidden";


    private final CL_Editor clEditor;
    private Mode mode;
    private PlayRestScenario scenario;
    private boolean enabled;
    private boolean chordPositionsHidden;
    private final ImproSupportPlaybackListener playbackListener;
    private final SongMetaEvents songMetaEvents;
    private boolean dirty;

    private final SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(ImproSupport.class.getSimpleName());

    public ImproSupport(CL_Editor clEditor)
    {
        checkNotNull(clEditor);
        this.clEditor = clEditor;
        this.mode = Mode.PLAY_REST_EASY;
        this.enabled = false;
        this.dirty = true;
        chordPositionsHidden = true;

        playbackListener = new ImproSupportPlaybackListener();
        songMetaEvents = SongMetaEvents.getInstance(clEditor.getSongModel());

    }

    public CL_Editor getCL_Editor()
    {
        return clEditor;
    }

    public void cleanup()
    {
        stopListeners();
    }

    /**
     * Generate new improvisation guide(s) for the whole song with the current mode.
     */
    public void generate()
    {
        if (!enabled || clEditor.getSongModel().getSongStructure().getSongParts().isEmpty())
        {
            return;
        }
        
        setDirty(false);

        PlayRestScenario prs;
        switch (mode)
        {
            case PLAY_REST_EASY -> prs = generatePlayRestScenario(PlayRestScenario.Level.LEVEL1, scenario, false);
            case PLAY_REST_MEDIUM -> prs = generatePlayRestScenario(PlayRestScenario.Level.LEVEL2, scenario, false);
            case DENSE_SPARSE -> prs = generatePlayRestScenario(PlayRestScenario.Level.LEVEL1, scenario, true);
            default -> throw new AssertionError(mode.name());
        }

        setPlayRestScenario(prs);

        Analytics.incrementProperties("Impro Support Generate", 1);
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
        generate();
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
            startListeners();            
            generate();
        } else
        {
            stopListeners();
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
    // PropertyChangeListener interface
    //================================================================================================

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getSource() == songMetaEvents && evt.getPropertyName().equals(SongMetaEvents.PROP_SONG_STRUCTURE))
        {
            songStructurallyChanged();
        }

    }

    //================================================================================================
    // Private methods
    //================================================================================================
    /**
     * Get all the bar renderers ordered by barIndex.
     *
     * @return
     */
    private List<BR_ImproSupport> getBarRenderers()
    {
        List<BR_ImproSupport> res = BR_ImproSupport.getBR_ImproSupportInstances(clEditor);
        return res;
    }

    /**
     * Create a new scenario guaranteed to be different from oldScenario (for PlayRestValues).
     *
     * @param level
     * @param oldScenario    Can be null
     * @param addDenseSparse
     * @return
     */
    private PlayRestScenario generatePlayRestScenario(PlayRestScenario.Level level, PlayRestScenario oldScenario, boolean addDenseSparse)
    {
        LOGGER.log(Level.FINE, "generatePlayRestScenario() -- level={0}", level);

        PlayRestScenario res = null;

        // Generate a scenario until it's different from the previous one (if any)
        List<PlayRestValue> oldPrValues = oldScenario == null ? new ArrayList<>() : oldScenario.getPlayRestValues();
        List<PlayRestValue> newPrValues = null;
        List<DenseSparseValue> newDsValues = null;
        var song = clEditor.getSongModel();

        while (newPrValues == null || oldPrValues.equals(newPrValues))
        {
            res = new PlayRestScenario(level, song);
            newPrValues = res.generatePlayRestValues();
            if (addDenseSparse)
            {
                newDsValues = res.generateDenseSparseValues(newPrValues);
            }
        }
        assert res != null;
        LOGGER.log(Level.FINE, "generatePlayRestScenario() newPrValues={0} newDsValues={1}", new Object[]
        {
            newPrValues, newDsValues
        });

        return res;
    }

    private void setPlayRestScenario(PlayRestScenario prScenario)
    {
        scenario = prScenario;

        // Update all BarRenderers
        for (var br : getBarRenderers())
        {
            br.setScenario(scenario);
        }

        updateSongBarIndexes();
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
            bbc = bbc.getUpdatedConfig(activeBrTypes.toArray(new String[0]));
            clEditor.setBarBoxConfig(bbc, i);
        }

        if (show)
        {
            getBarRenderers().forEach(br -> br.setPlaybackPointEnabled(hideChordPositions));
            updateSongBarIndexes();
        }
    }

    private synchronized void setDirty(boolean b)
    {
        dirty = b;
    }

    private void startListeners()
    {
        songMetaEvents.addPropertyChangeListener(SongMetaEvents.PROP_SONG_STRUCTURE, this);
        MusicController.getInstance().addPlaybackListener(playbackListener);
        MusicController.getInstance().addPropertyChangeListener(this);
    }

    private void stopListeners()
    {
        songMetaEvents.removePropertyChangeListener(SongMetaEvents.PROP_SONG_STRUCTURE, this);
        MusicController.getInstance().removePlaybackListener(playbackListener);
        MusicController.getInstance().removePropertyChangeListener(this);
    }

    private synchronized boolean isDirty()
    {
        return dirty;
    }

    private void songStructurallyChanged()
    {
        setDirty(true);
        getBarRenderers().forEach(br -> br.setSongBarIndex(-1));
    }

    /**
     * Update the songBarIndexes of all BarRenderers depending on the current playback position.
     * <p>
     * Do nothing if state is dirty.
     */
    private void updateSongBarIndexes()
    {
        if (isDirty() || !enabled)
        {
            return;
        }
        Song song = clEditor.getSongModel();
        var ss = song.getSongStructure();
        var spts = ss.getSongParts();
        int barIndex = MusicController.getInstance().getCurrentBeatPosition().getBar();
        SongPart spt = ss.getSongPart(barIndex);
        var brs = getBarRenderers();
        Set<CLI_Section> doneSections = new HashSet<>();

        while (spt != null)
        {

            var section = spt.getParentSection();
            if (!doneSections.contains(section))
            {
                // Set songBarIndex for each bar of the current section
                int clsBarIndex = spt.getParentSection().getPosition().getBar();
                for (int i = 0; i < spt.getNbBars(); i++)
                {
                    var br = brs.get(clsBarIndex + i);
                    br.setSongBarIndex(spt.getStartBarIndex() + i);
                }

            }
            doneSections.add(section);


            // Next song part, if any
            int sptIndex = spts.indexOf(spt) + 1;
            spt = sptIndex == spts.size() ? null : spts.get(sptIndex);
        }
    }


    // ===================================================================================
    // Inner classes
    // ===================================================================================
    public enum Mode
    {
        PLAY_REST_EASY(ResUtil.getString(ImproSupport.class, "PlayRest1DisplayName"), ResUtil.getString(ImproSupport.class, "PlayRestHelpText")),
        PLAY_REST_MEDIUM(ResUtil.getString(ImproSupport.class, "PlayRest2DisplayName"), ResUtil.getString(ImproSupport.class, "PlayRestHelpText")),
        DENSE_SPARSE(ResUtil.getString(ImproSupport.class, "DenseSparse1DisplayName"), ResUtil.getString(ImproSupport.class, "DenseSparseHelpText"));

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

    private class ImproSupportPlaybackListener implements PlaybackListener
    {

        @Override
        public boolean isAccepted(PlaybackSession session)
        {
            return session.getContext() == PlaybackSession.Context.SONG;
        }

        @Override
        public void enabledChanged(boolean b)
        {
            // Nothing
        }

        @Override
        public void beatChanged(Position oldPos, Position newPos, float newPosInBeats)
        {
            // Nothing
        }

        @Override
        public void chordSymbolChanged(CLI_ChordSymbol chordSymbol)
        {
            // Nothing
        }

        @Override
        public void songPartChanged(SongPart spt)
        {
            LOGGER.log(Level.FINE, "ImproSupportPlaybackListener.songPartChanged() -- spt={0}", spt);
            updateSongBarIndexes();

        }

        @Override
        public void midiActivity(long tick, int channel)
        {
            // Nothing
        }
    }

}

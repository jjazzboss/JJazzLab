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
package org.jjazz.mixconsole;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.BorderFactory;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jjazz.musiccontrol.spi.ActiveSongBackgroundMusicBuilder;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.api.UserRhythmVoice;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.coreuicomponents.api.PhraseBirdsEyeViewComponent;
import org.jjazz.musiccontrol.api.MusicGenerationQueue.Result;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.song.api.Song;
import org.jjazz.uisettings.api.GeneralUISettings;
import org.jjazz.uiutilities.api.CornerLayout;
import org.jjazz.utilities.api.ResUtil;

/**
 * A panel to represent the phrase corresponding to a RhythmVoice.
 * <p>
 * Get the phrase from the ActiveSongMusicBuilder. Use CornerLayout to display buttons in corners.
 */
public class PhraseViewerPanel extends PhraseBirdsEyeViewComponent implements ChangeListener, PropertyChangeListener
{

    protected static final Color BORDER_COLOR = GeneralUISettings.adaptColorToLightThemeIfRequired(new Color(32, 36, 53), 40);
    protected static final int BUTTONS_PADDING = 2;
    private RhythmVoice rhythmVoice;
    private final Song song;
    private final MixChannelPanelController controller;
    private final MidiMix midiMix;


    /**
     * Create the appropriate instance depending on rv type.
     *
     * @param rv
     * @return
     */
    static public PhraseViewerPanel createInstance(Song song, MidiMix midiMix, MixChannelPanelController controller, RhythmVoice rv)
    {
        PhraseViewerPanel res = (rv instanceof UserRhythmVoice) ? new PhraseViewerPanelUser(song, midiMix, controller, rv)
                : new PhraseViewerPanelRhythm(song, midiMix, controller, rv);
        return res;
    }

    protected PhraseViewerPanel(Song song, MidiMix mMix, MixChannelPanelController controller, RhythmVoice rv)
    {
        this.rhythmVoice = rv;
        this.song = song;
        this.midiMix = mMix;
        this.controller = controller;

        midiMix.addPropertyChangeListener(this);

        var asbmb = ActiveSongBackgroundMusicBuilder.getDefault();
        asbmb.addChangeListener(this);


        setPreferredSize(new Dimension(50, 50));        // width will be ignored by MixConsole layout manager        
        setMinimumSize(new Dimension(50, 8));           // width will be ignored by MixConsole layout manager        
        setOpaque(false);
        setShowVelocityMode(2);
        setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        setLayout(new CornerLayout(BUTTONS_PADDING, CornerLayout.NORTH_WEST));
        String text = ResUtil.getString(getClass(), "PhraseViewerTooltip") + "\n" + ResUtil.getString(getClass(), "DragToExportTrack");
        setToolTipText(text);


        // Refresh content if ActiveSongMusicBuilder has already a result for us (useful when user switches between songs)
        var result = asbmb.getLastResult();
        if (asbmb.isLastResultUpToDate() && result.songContext().getSong() == song)
        {
            musicGenerationResultReceived(result);
        }

        // Activate transfer handler if mouse drag initiated
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter()
        {
            @Override
            public void mouseDragged(java.awt.event.MouseEvent evt)
            {
                handleMouseDrag(evt);
            }
        });
    }

    public MixChannelPanelController getController()
    {
        return controller;
    }

    @Override
    public String toString()
    {
        return rhythmVoice.getName();
    }

    public RhythmVoice getRhythmVoice()
    {
        return rhythmVoice;
    }

    public Song getSong()
    {
        return song;
    }

    public MidiMix getMidiMix()
    {
        return midiMix;
    }

    public void cleanup()
    {
        ActiveSongBackgroundMusicBuilder.getDefault().removeChangeListener(this);
        midiMix.removePropertyChangeListener(this);
    }

    //=============================================================================
    // ChangeListener interface
    //=============================================================================
    @Override
    public void stateChanged(ChangeEvent e)
    {
        musicGenerationResultReceived(ActiveSongBackgroundMusicBuilder.getDefault().getLastResult());
    }

    //-----------------------------------------------------------------------
    // Implementation of the PropertiesListener interface
    //-----------------------------------------------------------------------
    @SuppressWarnings(
            {
                "unchecked", "rawtypes"
            })
    @Override
    public void propertyChange(PropertyChangeEvent e)
    {
        if (e.getSource() == midiMix)
        {
            if (e.getPropertyName().equals(MidiMix.PROP_RHYTHM_VOICE))
            {
                if (rhythmVoice == e.getOldValue())
                {
                    rhythmVoice = (RhythmVoice) e.getNewValue();
                }
            } else if (e.getPropertyName().equals(MidiMix.PROP_RHYTHM_VOICE_CHANNEL))
            {
                // Nothing
            }
        }
    }

    // ----------------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------------

    protected void handleMouseDrag(MouseEvent e)
    {
        TransferHandler th = getTransferHandler();  // set in MixConsole
        if (th != null && SwingUtilities.isLeftMouseButton(e))
        {
            th.exportAsDrag(PhraseViewerPanel.this, e, TransferHandler.COPY);
            // Note that from now on our various mouse drag listeners won't be called anymore until DnD export operation is over
        }
    }

    private void musicGenerationResultReceived(Result result)
    {
        if (result == null || result.throwable() != null)
        {
            return;
        }

        Phrase p = result.mapRvPhrases().get(rhythmVoice);
        if (p != null)
        {
            setModel(p, null, song.getSongStructure().toBeatRange(null));
        }
    }


    // ----------------------------------------------------------------------------
    // Inner classes
    // ----------------------------------------------------------------------------
}

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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import org.jjazz.activesong.api.BackgroundSongMusicBuilder;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.phrase.api.ui.PhraseBirdsEyeViewComponent;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythmmusicgeneration.api.MusicGenerationQueue;
import org.jjazz.song.api.Song;

/**
 * A panel to represent the phrase corresponding to a channel.
 * <p>
 * Get the phrase from the BackgroundSongMusicBuilder.
 */
public class PhraseViewerPanel extends PhraseBirdsEyeViewComponent
{

    private static final Color TOP_LINE_COLOR = new Color(39, 61, 69);
    private final RhythmVoice rhythmVoice;
    private final Song song;
    private final int channel;


    public PhraseViewerPanel(Song song, RhythmVoice rv, int channel)
    {
        this.rhythmVoice = rv;
        this.song = song;
        this.channel = channel;


        BackgroundSongMusicBuilder.getInstance().addPropertyChangeListener(this);

        setPreferredSize(new Dimension(50, 50));        // width will be ignored by MixConsole layout manager        
        setMinimumSize(new Dimension(50, 8));           // width will be ignored by MixConsole layout manager        
        setLabel(channel + ": " + rv.getName());
        setOpaque(false);
        setShowVelocityMode(2);
    }


    public void cleanup()
    {
        BackgroundSongMusicBuilder.getInstance().removePropertyChangeListener(this);
    }

    public RhythmVoice getRhythmVoice()
    {
        return rhythmVoice;
    }

    public int getChannel()
    {
        return channel;
    }

    /**
     * Overridden to add a line on the top.
     *
     * @param g
     */
    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        g.setColor(TOP_LINE_COLOR);
        g.drawLine(10, 0, getWidth() - 10, 0);
    }


    // ----------------------------------------------------------------------------
    // PropertyChangeListener interface
    // ----------------------------------------------------------------------------
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        super.propertyChange(evt);
        if (evt.getSource() == BackgroundSongMusicBuilder.getInstance())
        {
            if (evt.getPropertyName().equals(BackgroundSongMusicBuilder.PROP_MUSIC_GENERATION_RESULT))
            {
                musicGenerationResultReceived((MusicGenerationQueue.Result) evt.getNewValue());
            }
        }
    }

    // ----------------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------------

    private void musicGenerationResultReceived(MusicGenerationQueue.Result result)
    {
        if (result.userException() != null)
        {
            return;
        }

        Phrase p = result.mapRvPhrases().get(rhythmVoice);
        if (p != null)
        {
            setModel(p, null, song.getSongStructure().getBeatRange(null));
        }
    }

}

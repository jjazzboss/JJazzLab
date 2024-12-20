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
package org.jjazz.musiccontrolactions.api.ui;

import com.google.common.base.Preconditions;
import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import javax.swing.JLabel;
import org.jjazz.harmony.api.Position;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.song.api.Song;

/**
 * Shows the position in bar/beat.
 */
public class PositionViewer extends JLabel implements PropertyChangeListener
{

    private Position posModel;
    private Song songModel;
    private Color saveBackground;
    private Color playBackground;
    private boolean timeShown;


    private static final Logger LOGGER = Logger.getLogger(PositionViewer.class.getSimpleName());

    public PositionViewer()
    {
        timeShown = true;
        updateEditor(new Position(0));
    }

    public void setModel(Song song, Position pos)
    {
        Preconditions.checkNotNull(pos);
        if (posModel != null)
        {
            posModel.removePropertyChangeListener(this);
        }
        if (songModel != null)
        {
            MusicController.getInstance().removePropertyChangeListener(this);
        }
        playBackground = new Color(51, 204, 255);
        songModel = song;
        posModel = pos;
        posModel.addPropertyChangeListener(this);
        if (songModel != null)
        {
            MusicController.getInstance().addPropertyChangeListener(this);
        }
        updateEditor(posModel);
    }


    public Position getPositionModel()
    {
        return posModel;
    }

    public Song getSongModel()
    {
        return songModel;
    }

    /**
     * @return the playBackground
     */
    public Color getPlayBackground()
    {
        return playBackground;
    }

    /**
     * @param playBackground the playBackground to set
     */
    public void setPlayBackground(Color playBackground)
    {
        this.playBackground = playBackground;
    }

    /**
     * Check if time (minute:seconds) is shown by the viewer.
     *
     * @return true by default
     */
    public boolean isTimeShown()
    {
        return timeShown;
    }

    public void setTimeShown(boolean timeShown)
    {
        this.timeShown = timeShown;
    }

    // ======================================================================
    // PropertyChangeListener interface
    // ======================================================================    
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        MusicController mc = MusicController.getInstance();
        if (evt.getSource() == posModel)
        {
            updateEditor(posModel);

        } else if (evt.getSource() == mc)
        {
            if (evt.getPropertyName().equals(MusicController.PROP_STATE))
            {
                if (mc.isPlaying())
                {
                    saveBackground = getBackground();
                    setBackground(playBackground);
                } else
                {
                    setBackground(saveBackground);
                }
            }
        }
    }

    // ======================================================================
    // Private methods
    // ======================================================================   
    /**
     * Display the specified position.
     *
     * @param pos
     */
    private void updateEditor(Position pos)
    {
        int bar = pos.getBar() + 1;
        int beat = Math.round(pos.getBeat() + 1);
        String posStr = String.format("%1$03d:%2$d", bar, beat);
        String timeStr = "";

        if (timeShown)
        {
            timeStr =  " --m--s";
            if (songModel != null && pos.getBar() < songModel.getSongStructure().getSizeInBars())
            {
                int tempo = songModel.getTempo();
                float posInBeats = songModel.getSongStructure().toPositionInNaturalBeats(pos.getBar()) + pos.getBeat();
                float oneBeatInSec = 60f / tempo;
                float posInSec = posInBeats * oneBeatInSec;
                int min = (int) Math.floor(posInSec / 60);
                int sec = (int) Math.floor(posInSec - min * 60);
                timeStr = String.format(" %1$02dm%2$02ds", min, sec);
            }
        }

        setText(posStr + timeStr);
    }

}

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
package org.jjazz.arranger;

import java.beans.PropertyChangeEvent;
import org.jjazz.activesong.api.ActiveSongManager;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.song.api.Song;

/**
 * Controller for the ArrangerPanel.
 */
public class ArrangerController implements PropertyChangeListener
{

    public void playPause()
    {

    }

    public void nextSongPart()
    {

    }

    public void previousSongPart()
    {

    }
    // =================================================================================
    // PropertyChangeListener implementation
    // =================================================================================

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getSource() == ActiveSongManager.getInstance())
        {
            if (evt.getPropertyName().equals(ActiveSongManager.PROP_ACTIVE_SONG))
            {
                activeSongChanged((Song) evt.getNewValue(), (MidiMix) evt.getOldValue());
            }
        } else if (evt.getSource() == MusicController.getInstance())
        {
            if (evt.getPropertyName().equals(MusicController.PROP_STATE) && notesViewer.getMode().equals(NotesViewer.Mode.ShowBackingTrack))
            {
                MusicController.State state = (MusicController.State) evt.getNewValue();
                switch (state)
                {
                    case DISABLED:  // Fall down
                    case STOPPED:
                        org.jjazz.ui.utilities.api.Utilities.invokeLaterIfNeeded(() -> lbl_chordSymbol.setText(" "));
                        org.jjazz.ui.utilities.api.Utilities.invokeLaterIfNeeded(() -> lbl_scale.setText(" "));
                        notesViewer.releaseAllNotes();
                        break;
                    case PAUSED:
                        // Nothing
                        break;
                    case PLAYING:
                        // Nothing
                        break;
                    default:
                        throw new AssertionError(state.name());
                }
            }
        } else if (evt.getSource() == midiMixPlaybackMode)
        {
            if (MidiMix.PROP_CHANNEL_INSTRUMENT_MIX.equals(evt.getPropertyName()))
            {
                updateComboModel();
            }
        } else if (evt.getSource() == selectedChordSymbol)
        {
            if (Item.PROP_ITEM_DATA.equals(evt.getPropertyName()))
            {
                if (!isUIinPlaybackMode())
                {
                    showChordSymbolNotes(selectedChordSymbol);
                }
            }
        }
    }

}

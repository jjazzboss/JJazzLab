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
package org.jjazz.songeditormanager;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheetFactory;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.rhythm.api.TempoRange;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongManager;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

//@ActionID(category = "File", id = "org.jjazz.ui.actions.RandomSong")
//@ActionRegistration(displayName = "#CTL_RandomSong")
//@ActionReferences(
//        {
//            @ActionReference(path = "Menu/File", position = 20)
//        })
//@Messages("CTL_RandomSong=Random Song")
public final class RandomSong implements ActionListener
{

    private static final Logger LOGGER = Logger.getLogger(RandomSong.class.getSimpleName());

    @Override
    public void actionPerformed(ActionEvent e)
    {
        // Create the song
        SongManager sf = SongManager.getInstance();
        Song song = createRandomSong(sf.getNewSongName(), 16, 120);
        SongEditorManager.getInstance().showSong(song);
    }

    /**
     * Create a random song.
     *
     * @param name The name of the song
     * @param clsSize The number of bars of the song.
     * @param tempo
     * @return
     */
    public Song createRandomSong(String name, int clsSize, int tempo)
    {
        if (name == null || name.isEmpty() || clsSize < 1 || !TempoRange.checkTempo(tempo))
        {
            throw new IllegalArgumentException("name=" + name + " clsSize=" + clsSize + " tempo=" + tempo);
        }
        SongManager sf = SongManager.getInstance();
        ChordLeadSheetFactory clsf = ChordLeadSheetFactory.getDefault();
        Song song = null;
        int robustness = 10;
        while (song == null)
        {
            ChordLeadSheet cls = clsf.createRamdomLeadSheet("A", TimeSignature.FOUR_FOUR, clsSize);
            try
            {
                song = sf.createSong(name, cls);
            } catch (UnsupportedEditException ex)
            {
                robustness--;
                if (robustness == 0)
                {
                    LOGGER.log(Level.WARNING, "Impossible to create random song", ex);
                    song = sf.createEmptySong(name);
                }
            }
        }
        song.setTempo(tempo);
        song.resetNeedSave();
        return song;
    }
}

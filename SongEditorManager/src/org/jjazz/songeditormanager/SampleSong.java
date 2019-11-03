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
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheetFactory;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.rhythm.api.TempoRange;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongFactory;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

//@ActionID(category = "File", id = "org.jjazz.ui.actions.SampleSong")
//@ActionRegistration(displayName = "#CTL_SampleSong")
//@ActionReferences(
//        {
//            @ActionReference(path = "Menu/File", position = 10)
//        })
//@Messages("CTL_SampleSong=Sample Song")
public final class SampleSong implements ActionListener
{

    private static final Logger LOGGER = Logger.getLogger(SampleSong.class.getSimpleName());

    @Override
    public void actionPerformed(ActionEvent e)
    {
        // Create the song
        SongFactory sf = SongFactory.getInstance();
        Song song = createSample12BarsSong(sf.getNewSongName(), 32, 120);
        SongEditorManager.getInstance().showSong(song);
    }

    /**
     * Create a sample song of 12 bars (or more).
     *
     * @param name The name of the song
     * @param clsSize The number of bars of the song, must be &gt;=12.
     * @param tempo
     * @return
     */
    public Song createSample12BarsSong(String name, int clsSize, int tempo)
    {
        if (name == null || name.isEmpty() || clsSize < 12 || !TempoRange.checkTempo(tempo))
        {
            throw new IllegalArgumentException("name=" + name + " clsSize=" + clsSize + " tempo=" + tempo);
        }
        ChordLeadSheetFactory clsf = ChordLeadSheetFactory.getDefault();
        ChordLeadSheet cls = clsf.createSampleLeadSheet12bars("A", clsSize);
        SongFactory sf = SongFactory.getInstance();
        Song song = null;
        try
        {
            song = sf.createSong(name, cls);
        } catch (UnsupportedEditException ex)
        {
            // We should not be here for a simple song !
            LOGGER.log(Level.WARNING, "createSample12BarsSong() Unexpected exception", ex);
            song = sf.createEmptySong(name);
        }
        song.setTempo(tempo);
        song.resetNeedSave();
        return song;
    }
}

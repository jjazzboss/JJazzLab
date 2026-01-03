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
package org.jjazz.test;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MidiUnavailableException;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.spi.MidiMixManager;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.jjazz.rhythmmusicgeneration.api.SongSequenceBuilder;
import org.jjazz.songcontext.api.SongContext;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythmdatabase.api.RhythmInfo;
import org.jjazz.rhythmdatabase.api.UnavailableRhythmException;
import org.jjazz.song.api.Song;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.netbeans.api.progress.BaseProgressUtils;
import org.openide.util.Exceptions;
import org.openide.util.Utilities;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;

/**
 * For debug purposes...
 */
//@ActionID(category = "JJazz", id = "org.jjazz.test.testallrhythmsgeneratesequence")
//@ActionRegistration(displayName = "Test sequence generation all rhythms on current song")
//@ActionReferences(
//        {
//            @ActionReference(path = "Menu/Edit", position = 50200),
//        })
public final class TesAllRhythmsGenerateSequence implements ActionListener
{

    private static final Logger LOGGER = Logger.getLogger(TesAllRhythmsGenerateSequence.class.getSimpleName());

    @Override
    public void actionPerformed(ActionEvent ae)
    {
        LOGGER.info("TestAllRhythmsGenerateSequence() ------------");   
        Song song = Utilities.actionsGlobalContext().lookup(Song.class);
        if (song == null)
        {
            LOGGER.info("No current song");   
            return;
        }

        Runnable r = new Run(song);
        BaseProgressUtils.showProgressDialogAndRun(r, "Executing test music generation all rhythms...");
    }

    private class Run implements Runnable
    {
        Song song;
        MidiMix midiMix;

        Run(Song s)
        {
            song = s;
            try
            {
                midiMix = MidiMixManager.getDefault().findMix(song);      // Can raise MidiUnavailableException         
            } catch (MidiUnavailableException ex)
            {
                Exceptions.printStackTrace(ex);
            }
        }

        @Override
        public void run()
        {
            SongSequenceBuilder seqBuilder = new SongSequenceBuilder(new SongContext(song, midiMix));
            SongStructure sgs = song.getSongStructure();
            JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(sgs);
            um.setLimit(1);      // to not use too much memory with all rhythms instances...
            RhythmDatabase rdb = RhythmDatabase.getDefault();
            TimeSignature ts0 = sgs.getSongPart(0).getRhythm().getTimeSignature();
            for (RhythmInfo ri : rdb.getRhythms(ts0))
            {
                LOGGER.log(Level.SEVERE, "-- ri={0} file={1}", new Object[]   
                {
                    ri.name(), ri.file().getAbsolutePath()
                });
                Rhythm r;
                try
                {
                    r = rdb.getRhythmInstance(ri);
                } catch (UnavailableRhythmException ex)
                {
                    LOGGER.info("Can't get rhythm instance, skipped");   
                    continue;
                }
                var oldSpts = sgs.getSongParts();
                var newSpts = new ArrayList<SongPart>();
                for (SongPart spt : oldSpts)
                {
                    SongPart newSpt = spt.getCopy(r, spt.getStartBarIndex(), spt.getNbBars(), spt.getParentSection());
                    newSpts.add(newSpt);
                }
                try
                {
                    sgs.replaceSongParts(oldSpts, newSpts);

                    // Build the sequence
                    try
                    {
                        seqBuilder.buildAll(false);       // Can raise MusicGenerationException               
                        // LOGGER.info("sequence resolution=" + sequence.getResolution());
                    } catch (MusicGenerationException ex)
                    {
                        Exceptions.printStackTrace(ex);
                    }
                    try
                    {
                        Thread.sleep(20);    // Leave time for the UI to be updated before next model change
                    } catch (InterruptedException ex)
                    {
                        Exceptions.printStackTrace(ex);
                    }
                } catch (UnsupportedEditException ex)
                {
                    LOGGER.log(Level.SEVERE, "Problem changing rhythm. ex={0}", ex.getMessage());   
                    continue;
                }
            }
            um.setLimit(100);
        }
    }

}

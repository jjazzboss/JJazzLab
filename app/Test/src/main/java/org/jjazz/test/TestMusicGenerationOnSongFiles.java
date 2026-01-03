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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MidiUnavailableException;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.filedirectorymanager.api.FileDirectoryManager;
import org.jjazz.jjswing.api.BassStyle;
import org.jjazz.jjswing.api.RP_BassStyle;
import org.jjazz.midimix.spi.MidiMixManager;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.jjazz.rhythmmusicgeneration.api.SongSequenceBuilder;
import org.jjazz.songcontext.api.SongContext;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythmdatabase.api.UnavailableRhythmException;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongCreationException;
import org.netbeans.api.progress.BaseProgressUtils;
import org.openide.util.Exceptions;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.uiutilities.api.UIUtilities;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.windows.WindowManager;

/**
 * Test music generation of a rhythm in batch on all selected song files.
 */
//@ActionID(category = "JJazz", id = "org.jjazz.test.TestMusicGenerationOnSongFiles")
//@ActionRegistration(displayName = "Test music generation on multiple songs")
//@ActionReferences(
//        {
//            @ActionReference(path = "Menu/Edit", position = 50220),
//        })
public final class TestMusicGenerationOnSongFiles implements ActionListener
{

    private static final boolean UPDATE_SONGS = true;
    private static final boolean COPY_SONGS_TO_DEST = false;
    private static final boolean GENERATE_MUSIC = true;
    private static final Logger LOGGER = Logger.getLogger(TestMusicGenerationOnSongFiles.class.getSimpleName());


    @Override
    public void actionPerformed(ActionEvent ae)
    {
        LOGGER.log(Level.INFO, "Starting {0} ===============================", getClass().getSimpleName());


        JFileChooser chooser = UIUtilities.getFileChooserInstance();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("(" + "." + Song.SONG_EXTENSION + ")", Song.SONG_EXTENSION);
        chooser.resetChoosableFileFilters();
        chooser.setFileFilter(filter);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(true);
        chooser.setCurrentDirectory(FileDirectoryManager.getInstance().getLastSongDirectory());
        chooser.setSelectedFile(new File(""));
        chooser.setDialogTitle("Select song files");
        chooser.showOpenDialog(WindowManager.getDefault().getMainWindow());
        chooser.setPreferredSize(null);

        var songFiles = chooser.getSelectedFiles();

        var rdb = RhythmDatabase.getDefault();
        Rhythm r;
        try
        {
            r = rdb.getRhythmInstance("jjSwing-ID");
        } catch (UnavailableRhythmException ex)
        {
            Exceptions.printStackTrace(ex);
            return;
        }

        Runnable task = new RunTask(r, songFiles);
        BaseProgressUtils.showProgressDialogAndRun(task, "Executing test music generation on " + songFiles.length + " song files...");
    }

    private class RunTask implements Runnable
    {

        private final File[] songFiles;
        private final Rhythm rhythm;

        RunTask(Rhythm r, File[] songFiles)
        {
            this.rhythm = r;
            this.songFiles = songFiles;
        }

        @Override
        public void run()
        {
            for (var songFile : songFiles)
            {
                LOGGER.log(Level.INFO, "Processing ===================  {0}", songFile);

                try
                {
                    Song song = Song.loadFromFile(songFile);

                    if (UPDATE_SONGS)
                    {
                        LOGGER.log(Level.INFO, "  Updating song...");
                        if (!updateSongForJJSwing(song))
                        {
                            continue;
                        }
                    }

                    if (COPY_SONGS_TO_DEST)
                    {
                        var destDirFile = songFile.getParentFile().toPath().resolve("dest").toFile();
                        if (!destDirFile.isDirectory())
                        {
                            if (!destDirFile.mkdir())
                            {
                                LOGGER.log(Level.SEVERE, "Can not create {0}", destDirFile.getAbsolutePath());
                                break;
                            } else
                            {
                                LOGGER.log(Level.INFO, "Directory {0} created", destDirFile.getAbsolutePath());
                            }
                        }
                        var newSongFile = new File(destDirFile, songFile.getName());
                        LOGGER.log(Level.SEVERE, "  Saving file to {0}...", newSongFile);
                        song.saveToFile(newSongFile, true);
                    }


                    if (GENERATE_MUSIC)
                    {
                        // Generate music
                        LOGGER.log(Level.INFO, "  Generating music...");
                        var midiMix = MidiMixManager.getDefault().findMix(song);      // Can raise MidiUnavailableException      
                        SongSequenceBuilder seqBuilder = new SongSequenceBuilder(new SongContext(song, midiMix));
                        seqBuilder.buildMapRvPhrase(true);
                    }

                } catch (MidiUnavailableException | IOException | MusicGenerationException | SongCreationException | UnsupportedEditException ex)
                {
                    LOGGER.log(Level.SEVERE, "  !!! EXCEPTION ex={0}", ex.getMessage());
                    // Exceptions.printStackTrace(ex);
                }
            }


            LOGGER.log(Level.INFO, "\n");
            LOGGER.log(Level.INFO, "{0} songs processed", songFiles.length);
        }

        /**
         * Use our rhythm whenever possible
         *
         * @param song
         * @return true if update was successful
         * @throws UnsupportedEditException
         */
        private boolean updateSongForJJSwing(Song song) throws UnsupportedEditException
        {
            SongStructure sgs = song.getSongStructure();


            var oldSpts = sgs.getSongParts().stream()
                    .filter(spt -> spt.getRhythm() != rhythm && spt.getRhythm().getTimeSignature() == rhythm.getTimeSignature())
                    .toList();
            var newSpts = oldSpts.stream()
                    .map(spt -> spt.getCopy(rhythm, spt.getStartBarIndex(), spt.getNbBars(), spt.getParentSection()))
                    .toList();
            sgs.replaceSongParts(oldSpts, newSpts);


            var rhythmSpts = new ArrayList<>(sgs.getSongParts(spt -> spt.getRhythm() == rhythm));
            if (rhythmSpts.isEmpty())
            {
                LOGGER.log(Level.INFO, "   No relevant song parts to use {0}, skipping song", rhythm.getName());
                return false;
            }


            // Update the rhythmSpts with first BassStyle
            var bassStyles = new ArrayList<>(BassStyle.getNonCustomStyles());
            for (var spt : rhythmSpts)
            {
                sgs.setRhythmParameterValue(spt, RP_BassStyle.get(rhythm), RP_BassStyle.toRpValue(bassStyles.get(0)));
            }


            // Create rhythmSpts copies for each other bass style
            int insertBar = rhythmSpts.getLast().getBarRange().to + 1;
            for (var bassStyle : bassStyles.stream().skip(1).toList())
            {
                for (var spt : rhythmSpts.reversed())
                {
                    var newSpt = spt.getCopy(null, insertBar, spt.getNbBars(), spt.getParentSection());
                    sgs.addSongParts(List.of(newSpt));
                    sgs.setRhythmParameterValue(newSpt, RP_BassStyle.get(rhythm), RP_BassStyle.toRpValue(bassStyle));
                }
            }


            return true;
        }

    }
}

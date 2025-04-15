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
import org.jjazz.songstructure.api.SongPart;
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
@ActionID(category = "JJazz", id = "org.jjazz.test.TestMusicGenerationOnSongFiles")
@ActionRegistration(displayName = "Test music generation on multiple songs")
@ActionReferences(
        {
            @ActionReference(path = "Menu/Edit", position = 50220),
        })
public final class TestMusicGenerationOnSongFiles implements ActionListener
{

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

            var rpBassStyle = RP_BassStyle.get(rhythm);

            for (var songFile : songFiles)
            {
                LOGGER.log(Level.INFO, "Processing ===================  {0}", songFile);

                try
                {
                    Song song = Song.loadFromFile(songFile);
                    SongStructure sgs = song.getSongStructure();
                    var midiMix = MidiMixManager.getDefault().findMix(song);      // Can raise MidiUnavailableException      


                    // Use our rhythm whenever possible
                    {
                        var oldSpts = sgs.getSongParts().stream()
                                .filter(spt -> spt.getRhythm() != rhythm && spt.getRhythm().getTimeSignature() == rhythm.getTimeSignature())
                                .toList();
                        var newSpts = oldSpts.stream()
                                .map(spt -> spt.clone(rhythm, spt.getStartBarIndex(), spt.getNbBars(), spt.getParentSection()))
                                .toList();
                        sgs.replaceSongParts(oldSpts, newSpts);
                    }


                    var rhythmSpts = new ArrayList<>(sgs.getSongParts(spt -> spt.getRhythm() == rhythm));
                    if (rhythmSpts.isEmpty())
                    {
                        LOGGER.info("   No 4/4 song parts, skipping song");
                        continue;
                    }


                    // Make sure there is at least 3 song parts with our rhythm
                    if (rhythmSpts.size() < 3)
                    {
                        // Add SongParts
                        int nbMissingSpts = 3 - rhythmSpts.size();
                        var sptLast = rhythmSpts.getLast();
                        var sptLastRange = sptLast.getBarRange();
                        List<SongPart> extraSpts = new ArrayList<>();
                        for (int i = 0; i < nbMissingSpts; i++)
                        {
                            var extraSpt = sptLast.clone(null, sptLastRange.to + 1 + (i * sptLastRange.size()), sptLastRange.size(), sptLast.getParentSection());
                            extraSpts.add(extraSpt);
                        }
                        sgs.addSongParts(extraSpts);
                        rhythmSpts.addAll(extraSpts);
                        LOGGER.info("   Adding " + nbMissingSpts + " song parts");
                    }


                    // Make sure we use all BassStyles
                    for (int i = 0; i < rhythmSpts.size(); i++)
                    {
                        BassStyle style = i % 3 == 0 ? BassStyle.TWO_FEEL : (i % 3 == 1 ? BassStyle.WALKING : BassStyle.WALKING_DOUBLE);
                        var spt = rhythmSpts.get(i);
                        sgs.setRhythmParameterValue(spt, rpBassStyle, RP_BassStyle.toRpValue(style));
                    }


                    // Generate music
                    SongSequenceBuilder seqBuilder = new SongSequenceBuilder(new SongContext(song, midiMix), 0);
                    seqBuilder.buildMapRvPhrase(true);


                } catch (MidiUnavailableException | MusicGenerationException | SongCreationException | UnsupportedEditException ex)
                {
                    LOGGER.log(Level.SEVERE, "  !!! EXCEPTION ex=" + ex.getMessage());
                    // Exceptions.printStackTrace(ex);
                }
            }
        }

    }
}

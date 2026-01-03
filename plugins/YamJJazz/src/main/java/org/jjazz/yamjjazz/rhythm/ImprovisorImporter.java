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
package org.jjazz.yamjjazz.rhythm;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.importers.api.ImprovisorFileReader;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmFeatures;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Fill;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Intensity;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Variation;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Marker;
import org.jjazz.rhythmmusicgeneration.api.RP_SYS_Mute;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.jjazz.rhythmdatabase.api.RhythmInfo;
import org.jjazz.rhythmdatabase.api.UnavailableRhythmException;
import org.jjazz.song.api.Song;
import org.jjazz.song.spi.SongImporter;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.utilities.api.IntRange;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.yamjjazz.rhythm.api.YamJJazzRhythm;
import org.jjazz.yamjjazz.rhythm.api.YamJJazzRhythmProvider;
import org.jjazz.yamjjazz.rhythm.api.YamahaRhythmProvider;
import org.openide.util.Exceptions;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = SongImporter.class)
public class ImprovisorImporter implements SongImporter
{

    /**
     * Map between Improvisor style name (first chars) and a YamJJazz rhythm name
     */
    private HashMap<String, String> map = new HashMap<>();

    private final FileNameExtensionFilter FILTER = new FileNameExtensionFilter(ResUtil.getString(getClass(), "CTL_ImprovisorFiles") + " (.ls)", "ls");
    protected static final Logger LOGGER = Logger.getLogger(ImprovisorImporter.class.getName());


    @Override
    public String getId()
    {
        return ResUtil.getString(getClass(), "CTL_ImprovisorImporter");
    }

    @Override
    public List<FileNameExtensionFilter> getSupportedFileTypes()
    {
        return Arrays.asList(FILTER);
    }

    @Override
    public Song importFromFile(File f) throws IOException
    {
        ImprovisorFileReader reader = new ImprovisorFileReader(f);
        Song song = reader.readSong();
        postProcessSong(song, reader.getStyle());
        return song;
    }

    // =================================================================================================
    // Private methods
    // =================================================================================================
    private void postProcessSong(Song song, String styleText)
    {
        LOGGER.log(Level.INFO, "postProcessSong() -- styleText={0}", styleText);
        SongStructure ss = song.getSongStructure();
        List<SongPart> spts = ss.getSongParts();
        SongPart spt0 = spts.get(0);
        TimeSignature ts = spt0.getRhythm().getTimeSignature();
        int tempo = song.getTempo();


        // Try to guess rhythm based on styleText
        var r = guessRhythm(styleText, ts, tempo);
        if (r == null)
        {
            r = spt0.getRhythm();
        }
        if (!(r instanceof YamJJazzRhythm))
        {
            LOGGER.log(Level.INFO, "postProcessSong() Rhythm not supported for post-processing {0}, Post-processing aborted.", r);
            return;
        }

        // Assign the new rhythm
        SongPart newSpt = spt0.getCopy(r, spt0.getStartBarIndex(), spt0.getNbBars(), spt0.getParentSection());
        try
        {
            ss.replaceSongParts(Arrays.asList(spt0), Arrays.asList(newSpt));
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }

        RP_SYS_Variation rpVariation = RP_SYS_Variation.getVariationRp(r);
        RP_SYS_Intensity rpIntensity = RP_SYS_Intensity.getIntensityRp(r);
        RP_SYS_Fill rpFill = RP_SYS_Fill.getFillRp(r);
        RP_SYS_Mute rpMute = RP_SYS_Mute.getMuteRp(r);
        RP_SYS_Marker rpMarker = RP_SYS_Marker.getMarkerRp(r);

        // Modify SongPart 0
        ss.setRhythmParameterValue(newSpt, rpVariation, "Main A-1");
        ss.setRhythmParameterValue(newSpt, rpFill, "always");
        ss.setRhythmParameterValue(newSpt, rpMarker, "theme");

        if (spts.size() == 1)
        {
            // There is only one SongPart, add more
            // Add SongPart 1
            SongPart spt1 = appendSptCopy(newSpt);
            ss.setRhythmParameterValue(spt1, rpVariation, "Main B-1");
            ss.setRhythmParameterValue(spt1, rpFill, "always");
            ss.setRhythmParameterValue(spt1, rpMarker, "solo");

            // Add SongPart 2
            SongPart spt2 = appendSptCopy(newSpt);
            ss.setRhythmParameterValue(spt2, rpVariation, "Main D-1");
            ss.setRhythmParameterValue(spt2, rpIntensity, Integer.valueOf(3));
            ss.setRhythmParameterValue(spt2, rpFill, "always");
            ss.setRhythmParameterValue(spt2, rpMarker, "solo");

            // Add SongPart 3
            SongPart spt3 = appendSptCopy(newSpt);
            ss.setRhythmParameterValue(spt3, rpVariation, "Main C-1");
            ss.setRhythmParameterValue(spt3, rpFill, "break");
            ss.setRhythmParameterValue(spt3, rpMarker, "theme");
        } else
        {
            // There are several sections in the chord leadsheet, so there are as many SongParts already
            // In this case don't add new Song Parts
            // Just make sure all sections have the new rhythm
            for (int i = 1; i < spts.size(); i++)
            {
                SongPart oldSpt = spts.get(i);
                newSpt = oldSpt.getCopy(r, oldSpt.getStartBarIndex(), oldSpt.getNbBars(), oldSpt.getParentSection());
                try
                {
                    ss.replaceSongParts(Arrays.asList(oldSpt), Arrays.asList(newSpt));
                } catch (UnsupportedEditException ex)
                {
                    Exceptions.printStackTrace(ex);
                }
                if (i % 4 == 0)
                {
                    ss.setRhythmParameterValue(newSpt, rpVariation, "Main A-1");
                } else if (i % 3 == 0)
                {
                    ss.setRhythmParameterValue(newSpt, rpVariation, "Main D-1");
                } else if (i % 2 == 0)
                {
                    ss.setRhythmParameterValue(newSpt, rpVariation, "Main C-1");
                } else
                {
                    ss.setRhythmParameterValue(newSpt, rpVariation, "Main B-1");
                }
                if (i == spts.size() - 1)
                {
                    // Last SongPart
                    ss.setRhythmParameterValue(newSpt, rpFill, "always");
                }
            }
        }
    }

    /**
     * Append at the end of the SongStructure a copy of sptFrom.
     *
     * @param sptFrom
     * @return
     */
    private SongPart appendSptCopy(SongPart sptFrom)
    {
        SongStructure ss = sptFrom.getContainer();
        int startBarIndex = ss.getSizeInBars();
        int nbBars = sptFrom.getNbBars();
        SongPart newSpt = sptFrom.getCopy(null, startBarIndex, nbBars, sptFrom.getParentSection());
        try
        {
            ss.addSongParts(Arrays.asList(newSpt));
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        return newSpt;
    }


    private Rhythm guessRhythm(String text, TimeSignature ts, int tempo)
    {
        var rdb = RhythmDatabase.getDefault();
        var rf = RhythmFeatures.guessFeatures(text, tempo);
        var ri = rdb.findRhythm(rf, rii -> rii.timeSignature() == ts && isYamJJazz(rii));
        if (ri == null)
        {
            ri = rdb.findRhythm(text, rii -> rii.timeSignature() == ts && new IntRange(tempo - 30, tempo + 30).contains(rii.preferredTempo()));
        }
        if (ri == null)
        {
            LOGGER.log(Level.INFO, "guessRhythm() Could not find a JJazzLab rhythm corresponding to {0}, using default rhythm for ts={1}", new Object[]
            {
                text, ts
            });
            ri = rdb.getDefaultRhythm(ts);
        }
        YamJJazzRhythm yjr = null;
        try
        {
            yjr = (YamJJazzRhythm) rdb.getRhythmInstance(ri);
        } catch (UnavailableRhythmException ex)
        {
            LOGGER.log(Level.WARNING, "guessRhythm() Could not get Rhythm instance from RhythmInfo={0}.  ex={1}", new Object[]
            {
                ri,
                ex.getLocalizedMessage()
            });
        }
        return yjr;
    }

    private boolean isYamJJazz(RhythmInfo ri)
    {
        return YamahaRhythmProvider.isMine(ri) || YamJJazzRhythmProvider.isMine(ri);
    }
}

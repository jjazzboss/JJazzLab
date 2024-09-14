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
import org.jjazz.rhythm.api.rhythmparameters.RP_STD_Fill;
import org.jjazz.rhythm.api.rhythmparameters.RP_STD_Intensity;
import org.jjazz.rhythm.api.rhythmparameters.RP_STD_Variation;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Marker;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Mute;
import org.jjazz.song.api.Song;
import org.jjazz.song.spi.SongImporter;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.yamjjazz.rhythm.api.YamJJazzRhythm;
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


        // Find rhythm
        Rhythm r = null;
        r = ImporterRhythmFinder.findRhythm(styleText, song.getTempo(), ts);
        assert r != null;
        if (!(r instanceof YamJJazzRhythm) || r.getRhythmParameters().size() < 5)
        {
            LOGGER.log(Level.INFO, "postProcessSong() Rhythm not supported for post-processing {0}, Post-processing aborted.", r);
        }

        // Assign the new rhythm
        SongPart newSpt = spt0.clone(r, spt0.getStartBarIndex(), spt0.getNbBars(), spt0.getParentSection());
        try
        {
            ss.replaceSongParts(Arrays.asList(spt0), Arrays.asList(newSpt));
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }

        RP_STD_Variation rpVariation = RP_STD_Variation.getVariationRp(r);
        RP_STD_Intensity rpIntensity = RP_STD_Intensity.getIntensityRp(r);
        RP_STD_Fill rpFill = RP_STD_Fill.getFillRp(r);
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
                newSpt = oldSpt.clone(r, oldSpt.getStartBarIndex(), oldSpt.getNbBars(), oldSpt.getParentSection());
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
        SongPart newSpt = sptFrom.clone(null, startBarIndex, nbBars, sptFrom.getParentSection());
        try
        {
            ss.addSongParts(Arrays.asList(newSpt));
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        return newSpt;
    }

}

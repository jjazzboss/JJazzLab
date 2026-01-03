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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.importers.api.MusicXMLFileReader;
import org.jjazz.rhythm.api.AdaptedRhythm;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmFeatures;
import org.jjazz.rhythm.api.TempoRange;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Fill;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Variation;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.jjazz.rhythmdatabase.api.RhythmInfo;
import org.jjazz.rhythmdatabase.api.UnavailableRhythmException;
import org.jjazz.song.api.Song;
import org.jjazz.song.spi.SongImporter;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.utilities.api.Utilities;
import org.jjazz.yamjjazz.rhythm.api.YamJJazzRhythm;
import org.jjazz.yamjjazz.rhythm.api.YamJJazzRhythmProvider;
import org.jjazz.yamjjazz.rhythm.api.YamahaRhythmProvider;
import org.openide.util.Exceptions;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = SongImporter.class)
public class XmlImporter implements SongImporter
{

    private final FileNameExtensionFilter FILTER = new FileNameExtensionFilter(ResUtil.getString(getClass(), "CTL_MXML_Files") + " (.mxl, .xml, .musicxml)",
            "mxl", "xml", "musicxml");
    protected static final Logger LOGGER = Logger.getLogger(XmlImporter.class.getName());


    @Override
    public String getId()
    {
        return ResUtil.getString(getClass(), "CTL_MXML_Importer");
    }

    @Override
    public List<FileNameExtensionFilter> getSupportedFileTypes()
    {
        return Arrays.asList(FILTER);
    }

    @Override
    public Song importFromFile(File f) throws IOException
    {
        if (f == null)
        {
            throw new IllegalArgumentException("f=" + f);   //NOI18N
        }

        // Need to uncompress MXL first
        if (f.getName().toLowerCase().endsWith(".mxl"))
        {
            f = getMxlUncompressedFile(f);
        }

        MusicXMLFileReader reader = new MusicXMLFileReader(f);
        Song song = reader.readSong();
        postProcessSong(song,reader.getMusicalStyle());        // Choose the initial rhythm

        int tempo = reader.getTempo();
        if (tempo < TempoRange.TEMPO_MIN )
        {
            // Use preferred tempo from the initial rhythm
            tempo = song.getSize() > 0 ? song.getSongStructure().getSongPart(0).getRhythm().getPreferredTempo(): TempoRange.TEMPO_STD;
        }
        song.setTempo(tempo);


        return song;
    }

    // =================================================================================================
    // Private methods
    // =================================================================================================
    private File getMxlUncompressedFile(File mxlFile) throws IOException
    {
        File xmlFile = null;

        File destDir = Files.createTempDirectory("mxl-unzip").toFile();
        destDir.deleteOnExit();

        byte[] buffer = new byte[1024];
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(mxlFile)))
        {
            ZipEntry zipEntry = zis.getNextEntry();

            // Unzip only the first .xml file at the root level (ignore META_INF)
            while (zipEntry != null)
            {
                String fileName = zipEntry.getName();
                if (zipEntry.isDirectory()
                        || fileName.contains(File.separator)
                        || !fileName.toLowerCase().endsWith(".xml"))
                {
                    zipEntry = zis.getNextEntry();
                    continue;
                }

                // Got one, change the dest name
                xmlFile = new File(destDir, Utilities.replaceExtension(mxlFile.getName(), ".xml"));
                break;
            }


            if (xmlFile == null)
            {
                throw new IOException("No valid .xml file found in file: " + mxlFile.getAbsolutePath());
            }


            // Extract our file
            try (FileOutputStream fos = new FileOutputStream(xmlFile))
            {
                int len;
                while ((len = zis.read(buffer)) > 0)
                {
                    fos.write(buffer, 0, len);
                }
            }

            zis.closeEntry();
        }

        return xmlFile;
    }

    /**
     * Select the initial rhythm and update a few rhythm parameters.
     * <p>
     * If song containes 2 or more time signatures, use an AdaptedRhythm from the initial rhythm.
     *
     * @param song
     * @param styleText Can be an empty string
     */
    private void postProcessSong(Song song, String styleText)
    {
        Objects.requireNonNull(song);
        Objects.requireNonNull(styleText);
        LOGGER.log(Level.FINE, "postProcessSong() -- styleText={0}", styleText);
        SongStructure sgs = song.getSongStructure();
        List<SongPart> spts = sgs.getSongParts();
        if (spts.isEmpty())
        {
            LOGGER.warning("postProcessSong() no song parts found");
            return;
        }


        // Compute the initial rhythm
        var rdb = RhythmDatabase.getDefault();
        Rhythm rOrig = spts.get(0).getRhythm();            // The rhythm by default when song was first created, might be a stub rhythm (eg if rare timesignature)
        var ts0 = rOrig.getTimeSignature();

        YamJJazzRhythm yjr0 = guessYamJJazzRhythm(styleText, ts0);

        if (yjr0 == null)
        {
            yjr0 = findDefaultYamJJazzRhythm(ts0);
            if (yjr0 == null)
            {
                // eg 6/4 or 7/4 - create an AdaptedRhythm from the nearest 4/4 rhythm (sure to find it)
                var ts = ts0 == TimeSignature.SIX_FOUR ? TimeSignature.THREE_FOUR : TimeSignature.FOUR_FOUR;
                var r44 = guessYamJJazzRhythm(styleText, ts);
                if (r44 == null)
                {
                    r44 = findDefaultYamJJazzRhythm(TimeSignature.FOUR_FOUR);
                }
                if (r44 == null)
                {
                    LOGGER.warning("postProcessSong() Unexpected no 4/4 YamJJazz rhythm available, exiting");
                    return;
                }
                yjr0 = (YamJJazzRhythm) rdb.getAdaptedRhythmInstance(r44, ts0);
            }
        }


        // Update each song part
        for (int i = 0; i < spts.size(); i++)
        {
            // Update rhythm
            SongPart oldSpt = spts.get(i);
            var ts = oldSpt.getParentSection().getData().getTimeSignature();
            var r = yjr0;     // By default
            if (!ts.equals(ts0))
            {
                if (yjr0 instanceof AdaptedRhythm ayjr0)
                {
                    var rSource = ayjr0.getSourceRhythm();
                    r = (YamJJazzRhythm) (rSource.getTimeSignature().equals(ts) ? rSource : RhythmDatabase.getDefault().getAdaptedRhythmInstance(rSource, ts));
                } else
                {
                    r = (YamJJazzRhythm) RhythmDatabase.getDefault().getAdaptedRhythmInstance(yjr0, ts);
                }
            }
            if (r == null)
            {
                LOGGER.log(Level.WARNING, "postProcessSong() No adapted rhythm found from yjr0={0} ts={1}, exit postprocessing", new Object[]
                {
                    yjr0, ts
                });
                return;
            }
            var newSpt = oldSpt.getCopy(r, oldSpt.getStartBarIndex(), oldSpt.getNbBars(), oldSpt.getParentSection());
            try
            {
                sgs.replaceSongParts(List.of(oldSpt), List.of(newSpt));
            } catch (UnsupportedEditException ex)
            {
                Exceptions.printStackTrace(ex);
            }


            // Update rhythm parameters
            RP_SYS_Variation rpVariation = RP_SYS_Variation.getVariationRp(yjr0);
            if (rpVariation != null)
            {
                if (i % 4 == 0)
                {
                    sgs.setRhythmParameterValue(newSpt, rpVariation, "Main A-1");
                } else if (i % 3 == 0)
                {
                    sgs.setRhythmParameterValue(newSpt, rpVariation, "Main D-1");
                } else if (i % 2 == 0)
                {
                    sgs.setRhythmParameterValue(newSpt, rpVariation, "Main C-1");
                } else
                {
                    sgs.setRhythmParameterValue(newSpt, rpVariation, "Main B-1");
                }
            }

            RP_SYS_Fill rpFill = RP_SYS_Fill.getFillRp(yjr0);
            if (rpFill != null && newSpt.getNbBars() > 3)
            {
                sgs.setRhythmParameterValue(newSpt, rpFill, "always");
            }
        }

    }

    private boolean isYamJJazz(RhythmInfo ri)
    {
        return YamahaRhythmProvider.isMine(ri) || YamJJazzRhythmProvider.isMine(ri);
    }


    /**
     *
     * @param text
     * @param ts
     * @return Can be null if no YamJJazz rhythm found
     */
    protected YamJJazzRhythm guessYamJJazzRhythm(String text, TimeSignature ts)
    {
        var rdb = RhythmDatabase.getDefault();
        var rf = RhythmFeatures.guessFeatures(text, -1);
        var ri = rdb.findRhythm(rf, rii -> rii.timeSignature() == ts && isYamJJazz(rii) && !rii.isAdaptedRhythm());
        if (ri == null)
        {
            ri = rdb.findRhythm(text, rii -> rii.timeSignature() == ts && isYamJJazz(rii) && !rii.isAdaptedRhythm());
        }
        if (ri == null)
        {
            return null;
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

    /**
     * @param ts
     * @return Might be null
     */
    private YamJJazzRhythm findDefaultYamJJazzRhythm(TimeSignature ts)
    {
        YamJJazzRhythm res = null;
        var rdb = RhythmDatabase.getDefault();


        var yjri = rdb.getDefaultRhythm(ts);
        if (!isYamJJazz(yjri))
        {
            // No, search for the first one in the database
            yjri = rdb.getRhythms(ts).stream()
                    .filter(rii -> isYamJJazz(rii) && !rii.isAdaptedRhythm())
                    .findAny()
                    .orElse(null);
        }

        if (yjri != null)
        {
            try
            {
                res = (YamJJazzRhythm) rdb.getRhythmInstance(yjri);
            } catch (UnavailableRhythmException ex)
            {
                LOGGER.log(Level.WARNING, "findYamJJazzRhythm() ts={0} ex={1}", new Object[]
                {
                    ts, ex.getMessage()
                });
            }
        }

        return res;
    }


}

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

import org.jjazz.yamjjazz.rhythm.api.YamJJazzRhythm;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.importers.api.MusicXMLFileReader;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.rhythmparameters.RP_STD_Fill;
import org.jjazz.rhythm.api.rhythmparameters.RP_STD_Variation;
import org.jjazz.song.api.Song;
import org.jjazz.song.spi.SongImporter;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.utilities.api.Utilities;
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
        postProcessSong(song, reader.getMusicalStyle());
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

    private void postProcessSong(Song song, String styleText)
    {
        LOGGER.log(Level.FINE, "postProcessSong() -- styleText={0}", styleText);
        SongStructure sgs = song.getSongStructure();
        List<SongPart> spts = sgs.getSongParts();
        SongPart spt0 = spts.get(0);
        TimeSignature ts = spt0.getRhythm().getTimeSignature();


        // Set rhythm
        Rhythm r = null;
        r = ImporterRhythmFinder.findRhythm(styleText, song.getTempo(), ts);
        assert r != null;
        if (!(r instanceof YamJJazzRhythm) || r.getRhythmParameters().size() < 5)
        {
            LOGGER.log(Level.INFO, "postProcessSong() Rhythm not supported for post-processing {0}, Post-processing aborted.", r);
        }


        RP_STD_Variation rpVariation = RP_STD_Variation.getVariationRp(r);
        RP_STD_Fill rpFill = RP_STD_Fill.getFillRp(r);

        for (int i = 0; i < spts.size(); i++)
        {
            // Update rhythm
            SongPart oldSpt = spts.get(i);
            var newSpt = oldSpt.clone(r, oldSpt.getStartBarIndex(), oldSpt.getNbBars(), oldSpt.getParentSection());
            try
            {
                sgs.replaceSongParts(List.of(oldSpt), List.of(newSpt));
            } catch (UnsupportedEditException ex)
            {
                Exceptions.printStackTrace(ex);
            }
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
            if (rpFill != null)
            {
                sgs.setRhythmParameterValue(newSpt, rpFill, "always");
            }
        }

    }


}

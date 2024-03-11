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
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.importers.api.MusicXMLFileReader;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.jjazz.rhythmdatabase.api.RhythmInfo;
import org.jjazz.rhythmdatabase.api.UnavailableRhythmException;
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

    /**
     * Map between XML style name (first chars) and a YamJJazz rhythm name
     */
    private HashMap<String, String> map = new HashMap<>();

    private final FileNameExtensionFilter FILTER = new FileNameExtensionFilter(ResUtil.getString(getClass(), "CTL_MXML_Files") + " (.mxl, .xml)", "mxl", "xml");
    protected static final Logger LOGGER = Logger.getLogger(XmlImporter.class.getName());

    public XmlImporter()
    {
        initMap();
    }

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

        MusicXMLFileReader reader = new MusicXMLFileReader(f, true);
        Song song = reader.readSong();
        // postProcessSong(song, reader.getStyle());
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

    private void postProcessSong(Song song, String style)
    {
        SongStructure ss = song.getSongStructure();
        List<SongPart> spts = ss.getSongParts();
        SongPart spt0 = spts.get(0);
        TimeSignature ts = spt0.getRhythm().getTimeSignature();

        // Check TimeSignature
        switch (ts)
        {
            case THREE_FOUR:
            case FOUR_FOUR:
                break;
            default:
                // Post processing not supported yet
                LOGGER.log(Level.INFO, "postProcessSong() time signature not yet supported: {0}. Post-processing aborted.", ts);
                return;
        }

        // Style
//        YamJJazzRhythm r = null;
//        if (style != null)
//        {
//            r = getRhythmFromStyle(style, song.getTempo(), ts);
//        }
//        if (r == null)
//        {
//            r = getDefaultRhythm(song.getTempo());
//            if (r == null)
//            {
//                LOGGER.warning("postProcessImportedSong() Unexpected null rhythm. style=" + style + ", song.getTempo()=" + song.getTempo() + ". PostProcess stopped.");
//                return;
//            }
//        }
//
//        assert r.getRhythmParameters().size() >= 5 : " r=" + r;
//
//
//        // Assign the new rhythm
//        SongPart newSpt = spt0.clone(r, spt0.getStartBarIndex(), spt0.getNbBars(), spt0.getParentSection());
//        try
//        {
//            ss.replaceSongParts(Arrays.asList(spt0), Arrays.asList(newSpt));
//        } catch (UnsupportedEditException ex)
//        {
//            Exceptions.printStackTrace(ex);
//        }
//
//        RP_STD_Variation rpVariation = (RP_STD_Variation) newSpt.getRhythmInstance().getRhythmParameters().get(0);        // Variation
//        RhythmParameter rpIntensity = newSpt.getRhythmInstance().getRhythmParameters().get(1);        // Intensity
//        RhythmParameter rpFill = newSpt.getRhythmInstance().getRhythmParameters().get(2);        // Fill        
//        RhythmParameter rpMute = newSpt.getRhythmInstance().getRhythmParameters().get(3);        // Mute                
//        RhythmParameter rpMarker = newSpt.getRhythmInstance().getRhythmParameters().get(4);        // marker
//
//        // Modify SongPart 0
//        ss.setRhythmParameterValue(newSpt, rpVariation, "Main A-1");
//        ss.setRhythmParameterValue(newSpt, rpFill, "always");
//        ss.setRhythmParameterValue(newSpt, rpMarker, "theme");
//
//        if (spts.size() == 1)
//        {
//            // There is only one SongPart, add more
//            // Add SongPart 1
//            SongPart spt1 = appendSptCopy(newSpt);
//            ss.setRhythmParameterValue(spt1, rpVariation, "Main B-1");
//            ss.setRhythmParameterValue(spt1, rpFill, "always");
//            ss.setRhythmParameterValue(spt1, rpMarker, "solo");
//
//            // Add SongPart 2
//            SongPart spt2 = appendSptCopy(newSpt);
//            ss.setRhythmParameterValue(spt2, rpVariation, "Main D-1");
//            ss.setRhythmParameterValue(spt2, rpIntensity, Integer.valueOf(3));
//            ss.setRhythmParameterValue(spt2, rpFill, "always");
//            ss.setRhythmParameterValue(spt2, rpMarker, "solo");
//
//            // Add SongPart 3
//            SongPart spt3 = appendSptCopy(newSpt);
//            ss.setRhythmParameterValue(spt3, rpVariation, "Main C-1");
//            ss.setRhythmParameterValue(spt3, rpFill, "break");
//            ss.setRhythmParameterValue(spt3, rpMarker, "theme");
//        } else
//        {
//            // There are several sections in the chord leadsheet, so there are as many SongParts already
//            // In this case don't add new Song Parts
//            // Just make sure all sections have the new rhythm
//            for (int i = 1; i < spts.size(); i++)
//            {
//                SongPart oldSpt = spts.get(i);
//                newSpt = oldSpt.clone(r, oldSpt.getStartBarIndex(), oldSpt.getNbBars(), oldSpt.getParentSection());
//                try
//                {
//                    ss.replaceSongParts(Arrays.asList(oldSpt), Arrays.asList(newSpt));
//                } catch (UnsupportedEditException ex)
//                {
//                    Exceptions.printStackTrace(ex);
//                }
//                if (i % 4 == 0)
//                {
//                    ss.setRhythmParameterValue(newSpt, rpVariation, "Main A-1");
//                } else if (i % 3 == 0)
//                {
//                    ss.setRhythmParameterValue(newSpt, rpVariation, "Main D-1");
//                } else if (i % 2 == 0)
//                {
//                    ss.setRhythmParameterValue(newSpt, rpVariation, "Main C-1");
//                } else
//                {
//                    ss.setRhythmParameterValue(newSpt, rpVariation, "Main B-1");
//                }
//                if (i == spts.size() - 1)
//                {
//                    // Last SongPart
//                    ss.setRhythmParameterValue(newSpt, rpFill, "always");
//                }
//            }
//        }
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

    /**
     * Get a JJazzLab rhythm from a style and tempo.
     *
     * @param style
     * @param tempo
     * @param ts
     * @return Null if no matching found.
     */
    private YamJJazzRhythm getRhythmFromStyle(String style, int tempo, TimeSignature ts)
    {
        String rId = null;
        switch (ts)
        {
            case THREE_FOUR:
                if (tempo > 160)
                {
                    rId = "JazzWaltzFast.S499.sty-ID";
                } else
                {
                    rId = "JazzWaltz4.S001.sty-ID";
                }
                break;
            case FOUR_FOUR:
                // Get the mapped rhythm
                String s = getValueFromMap(style);
                if (s == null)
                {
                    break;
                }
                rId = s + "-ID";

                // Handle special cases
                if (style.toLowerCase().startsWith("ballad"))
                {
                    if (tempo < 90)
                    {
                        rId = "SlowJazz.STY-ID";
                    } else if (tempo < 130)
                    {
                        rId = "LACoolSwing.STY-ID";
                    }
                } else if (style.toLowerCase().startsWith("footprints"))
                {
                    if (tempo < 160)
                    {
                        rId = "SJazzWaltz4.S001.sty-ID";
                    }
                } else if (style.toLowerCase().startsWith("funk"))
                {
                    if (tempo < 120)
                    {
                        rId = "JazzRock_Cz2k.S563.yjz-ID";
                    }
                } else if (style.toLowerCase().startsWith("latin") || style.toLowerCase().startsWith("samba"))
                {
                    if (tempo < 80)
                    {
                        rId = "SlowBossa2.S460.prs-ID";
                    } else if (tempo < 130)
                    {
                        rId = "SambaCity213.s460.yjz-ID";
                    }
                } else if (style.toLowerCase().startsWith("swing"))
                {
                    if (tempo < 90)
                    {
                        rId = "SlowJazz.STY-ID";
                    } else if (tempo < 180)
                    {
                        rId = Math.random() > 0.5d ? "MediumJazz.S499.sty-ID" : "AcousticJazz1.S563.sty-ID";
                    }
                }
                break;
            default:
            // Nothing
        }

        YamJJazzRhythm r = null;
        if (rId != null)
        {
            RhythmDatabase rdb = RhythmDatabase.getDefault();
            try
            {
                r = (YamJJazzRhythm) rdb.getRhythmInstance(rId);
            } catch (UnavailableRhythmException ex)
            {
                LOGGER.log(Level.WARNING, "getRhythmFromStyle() Unexpected null rhythm : rId={0}", rId);
            }
        }

        return r;
    }

    /**
     * The default rhythm to use.
     *
     * @param tempo
     * @return Normally can't be null
     */
    private YamJJazzRhythm getDefaultRhythm(int tempo)
    {
        String rId = (tempo <= 150) ? "PopBossa1.S629.prs-ID" : "FastJazz.S741.sst-ID";
        RhythmDatabase rdb = RhythmDatabase.getDefault();
        YamJJazzRhythm r = null;
        try
        {
            r = (YamJJazzRhythm) rdb.getRhythmInstance(rId);
        } catch (UnavailableRhythmException ex)
        {
            LOGGER.log(Level.WARNING, "getRhythmFromStyle() can''t get RhythmInstance for rId={0}. ex={1}", new Object[]{rId,
                ex.getLocalizedMessage()});
        }
        return r;
    }

    /**
     * Special get: match if the map key matches the first chars of style.
     *
     * @param style
     * @return
     */
    private String getValueFromMap(String style)
    {
        String value = null;
        for (String key : map.keySet())
        {
            if (style.toLowerCase().startsWith(key.toLowerCase()))
            {
                value = map.get(key);
                break;
            }
        }
        return value;
    }

    private void initMap()
    {
        map.put("afri", "FastBossa.S629.prs");
        map.put("bossa", "BossaNova.STY");
        map.put("cha", "OrganChaCha.sty");
        map.put("folk", "CoolPop.sty");
        map.put("song-for-my-father", "FastBossa.S629.prs");
        map.put("mambo", "BigBandMambo.S380.sst");
        map.put("rhumba", "PopRumba.S625.bcs");
        map.put("rock", "CoolPop.sty");
        map.put("rock-heavy-even", "PowerRock.STY");
        map.put("rock-slow", "16beat.S556.yjz");
        map.put("rock-triplet", "LovelyShuffle.S502.prs");
        map.put("salsa", "Salsa 1.S651.STY");
        map.put("blues", "JazzBluesSimple.S740.sty");
        map.put("shuffle", "LovelyShuffle.S502.prs");
        map.put("soul", "Soul.S584.prs");
        map.put("ballad", "MediumJazz.S737.sst");
        map.put("footprints", "JazzWaltzFast.S499.sty");
        map.put("funk", "SoulBeat.STY");
        map.put("latin", "FastBossa.S629.prs");
        map.put("samba", "FastBossa.S629.prs");
        map.put("swing", "FastJazz.S741.sst");
        map.put("waltz", "JazzWaltzFast.S499.sty");

        // Log errors (if list of available styles has changed)
        RhythmDatabase rdb = RhythmDatabase.getDefault();
        for (String key : map.keySet().toArray(new String[0]))
        {
            String id = map.get(key) + "-ID";
            RhythmInfo ri = rdb.getRhythm(id);
            if (ri == null)
            {
                LOGGER.log(Level.WARNING, "initMap() No rhythm found for rhythmId: {0}", id);
                map.remove(key);
            }
        }
    }

}

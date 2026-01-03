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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.importers.api.BiabFileReader;
import org.jjazz.importers.api.BiabStyleFeatures;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmFeatures;
import org.jjazz.rhythm.api.TempoRange;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.jjazz.rhythmdatabase.api.UnavailableRhythmException;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Fill;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Variation;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Marker;
import org.jjazz.song.api.SongCreationException;
import org.jjazz.song.api.Song;
import org.jjazz.song.spi.SongImporter;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.yamjjazz.ui.BiabImportNotificationDialog;
import org.openide.util.Exceptions;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.ServiceProvider;
import org.openide.windows.WindowManager;

@ServiceProvider(service = SongImporter.class)
public class BiabImporter implements SongImporter
{
    
    private static final String PREF_SHOW_NOTIFICATION = "ShowNotification";
    private static final String[] EXTENSIONS = new String[]
    {
        "SG1", "SG2", "SG3", "SG4", "SG5", "SG6", "SG7", "SG8", "SG9", "SGA", "SGB", "SGC", "SGD", "SGE", "SGF", "SGG", "SGH", "SGI", "SGJ", "SGK", "SGL", "SGM",
        "SGN", "SGU",
        "MG1", "MG2", "MG3", "MG4", "MG5", "MG6", "MG7", "MG8", "MG9", "MGA", "MGB", "MGC", "MGD", "MGE", "MGF", "MGG", "MGH", "MGI", "MGJ", "MGK", "MGL", "MGM",
        "MGN", "MGU"
    };
    private static final String[] MAP_VARIATION = new String[]
    {
        "Main A-1", "Main B-1", "Main C-1", "Main D-1"
    };
    /**
     * Map between XML style name (first chars) and a YamJJazz rhythm name
     */
    private final HashMap<String, String> map = new HashMap<>();
    
    private static final Preferences prefs = NbPreferences.forModule(BiabImporter.class);
    private final FileNameExtensionFilter FILTER = new FileNameExtensionFilter(ResUtil.getString(getClass(), "CTL_BIAB_Files") + " (.sg?, .mg?)", EXTENSIONS);
    protected static final Logger LOGGER = Logger.getLogger(BiabImporter.class.getName());
    
    public BiabImporter()
    {
        // initMap();
    }
    
    @Override
    public String getId()
    {
        return ResUtil.getString(getClass(), "CTL_BIAB_Importer");
    }
    
    @Override
    public List<FileNameExtensionFilter> getSupportedFileTypes()
    {
        return Arrays.asList(FILTER);
    }
    
    @Override
    public Song importFromFile(File f) throws IOException, SongCreationException
    {
        if (prefs.getBoolean(PREF_SHOW_NOTIFICATION, true))
        {
            Runnable r = () -> 
            {
                BiabImportNotificationDialog dlg = new BiabImportNotificationDialog(WindowManager.getDefault().getMainWindow(), true);
                dlg.setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
                dlg.setVisible(true);
                if (dlg.isDoNotShowAgain())
                {
                    prefs.putBoolean(PREF_SHOW_NOTIFICATION, false);
                }
            };
            SwingUtilities.invokeLater(r);
        }
        
        BiabFileReader reader = new BiabFileReader(f, null, false);
        Song song = reader.readFile();      // throw exceptions
        postProcess(reader, song);
        return song;
    }

    // =================================================================================================
    // Private methods
    // =================================================================================================
    /**
     * Customize the SongParts.
     *
     * @param reader
     * @param song
     * @throws org.jjazz.song.api.SongCreationException
     */
    private void postProcess(BiabFileReader reader, Song song) throws SongCreationException
    {
        // Find the best possible matching rhythm
        BiabStyleFeatures sf = reader.styleFeatures;
        TempoRange tr = new TempoRange(reader.tempo - 25, reader.tempo + 25, "CustomTempoRange");
        RhythmFeatures rf = new RhythmFeatures(sf.genre, sf.division, tr);
        var ts = reader.timeSignature;
        var r = guessRhythm(rf, ts);
        if (r == null)
        {
            LOGGER.warning("postProcess() aborted");
            return;
        }


        // Replace rhythm on all SongParts
        SongStructure ss = song.getSongStructure();
        var oldSpts = ss.getSongParts();
        var newSpts = new ArrayList<SongPart>();
        for (SongPart spt : ss.getSongParts())
        {
            newSpts.add(spt.getCopy(r, spt.getStartBarIndex(), spt.getNbBars(), spt.getParentSection()));
        }
        try
        {
            ss.replaceSongParts(oldSpts, newSpts);
        } catch (UnsupportedEditException ex)
        {
            // Should never be here, it's not a multi rhythm
            Exceptions.printStackTrace(ex);
        }

        // Find the middle chorus names
        int minMiddleChorusIndex = 2;
        int maxMiddleChorusIndex = 0;
        boolean lastChorusPresent = false;
        for (SongPart spt : ss.getSongParts())
        {
            String s = spt.getName();
            if (s.startsWith("Chorus"))
            {
                maxMiddleChorusIndex = Math.max(Integer.parseInt(s.substring(6)), maxMiddleChorusIndex);
            } else if (s.startsWith("Last"))
            {
                lastChorusPresent = true;
            }
        }
        maxMiddleChorusIndex = lastChorusPresent ? maxMiddleChorusIndex : maxMiddleChorusIndex - 1;

        // Change RPs: variation fill marker
        RP_SYS_Variation rpVariation = RP_SYS_Variation.getVariationRp(r);
        RP_SYS_Fill rpFill = RP_SYS_Fill.getFillRp(r);
        RP_SYS_Marker rpMarker = RP_SYS_Marker.getMarkerRp(r);

        // Change the SongParts
        for (SongPart spt : ss.getSongParts())
        {
            // Bar marker => variation
            Integer barMarker = reader.mapClsBarMarker.get(spt.getParentSection().getPosition().getBar());
            if (barMarker != null && rpVariation != null && rpVariation.getPossibleValues().contains("Main A-1"))
            {
                ss.setRhythmParameterValue(spt, rpVariation, getVariationString(rpVariation, barMarker));
            }

            // Systematic fill
            if (rpFill != null)
            {
                ss.setRhythmParameterValue(spt, rpFill, RP_SYS_Fill.VALUE_ALWAYS);
            }

            // Middle chorus marker
            if (spt.getName().startsWith("Chorus") && rpMarker != null)
            {
                int cIndex = Integer.parseInt(spt.getName().substring(6));
                if (cIndex >= minMiddleChorusIndex && cIndex <= maxMiddleChorusIndex)
                {
                    ss.setRhythmParameterValue(spt, rpMarker, RP_SYS_Marker.SOLO);
                }
            }
        }
    }
    
    private String getVariationString(RP_SYS_Variation rp, int barMarker)
    {
        if (barMarker < 1)
        {
            throw new IllegalArgumentException("barMarker=" + barMarker);   //NOI18N
        }
        barMarker--;
        
        if (barMarker == 0)
        {
            barMarker = (int) Math.round(Math.random());    // 0-1
        } else
        {
            barMarker = (int) Math.round(Math.random()) + 2;    // 2-3
        }
        
        if (barMarker > rp.getPossibleValues().size() - 1)
        {
            barMarker = rp.getPossibleValues().size() - 1;
        }
        return MAP_VARIATION[barMarker];
    }
    
    private Rhythm guessRhythm(RhythmFeatures rf, TimeSignature ts)
    {
        var rdb = RhythmDatabase.getDefault();
        var ri = rdb.findRhythm(rf, rii -> rii.timeSignature() == ts);
        if (ri == null)
        {
            LOGGER.log(Level.INFO, "guessRhythm() Could not find a corresponding JJazzLab rhythm, using default rhythm for ts={0}", ts);
            ri = rdb.getDefaultRhythm(ts);
        }
        Rhythm r = null;
        try
        {
            r = rdb.getRhythmInstance(ri);
        } catch (UnavailableRhythmException ex)
        {
            LOGGER.log(Level.WARNING, "guessRhythm() Could not get Rhythm instance from RhythmInfo={0}.  ex={1}", new Object[]
            {
                ri,
                ex.getLocalizedMessage()
            });
        }
        return r;
    }
}

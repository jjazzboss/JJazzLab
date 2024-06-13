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
package org.jjazz.ss_editorimpl;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.song.api.Song;
import org.jjazz.ss_editor.api.SS_Editor.ViewMode;

/**
 * Helper class to handle SS_Editor settings saved as song client properties.
 */
public class SongSpecificSS_EditorProperties
{

    // Song client properties : do not change the values are they are used in Song serialization
    private static final String PROP_ZOOM_FACTOR_X = "PropSsEditorZoomFactorX";
    private static final String PROP_ZOOM_FACTOR_Y = "PropSsEditorZoomFactorY";
    private static final String PROP_VIEW_MODE = "PropViewMode";
    /**
     * The Song client property basename to store the visible RPs in compact view mode.
     * <p>
     * For example the property name will be "PropCompactViewModeVisibleRps_swingId" for rhythm swing
     */
    private static final String PROP_COMPACT_VIEW_MODE_VISIBLE_RPS = "PropCompactViewModeVisibleRps";

    private final Song song;
    private static final Logger LOGGER = Logger.getLogger(SongSpecificSS_EditorProperties.class.getSimpleName());

    public SongSpecificSS_EditorProperties(Song song)
    {
        this.song = song;
    }


    public ViewMode loadViewMode()
    {
        String s = song.getClientProperties().get(PROP_VIEW_MODE, ViewMode.COMPACT.name());
        ViewMode res = ViewMode.COMPACT;
        try
        {
            res = ViewMode.valueOf(s);
        } catch (IllegalArgumentException e)
        {
            // Ignore
        }
        return res;
    }

    public void storeViewMode(ViewMode mode)
    {
        if (!loadViewMode().equals(mode))
        {
            song.getClientProperties().put(PROP_VIEW_MODE, mode.name());
        }
    }


    /**
     * Get the list of visible RPs in compact view mode for the specified rhythm.
     * <p>
     *
     * @param song
     * @param r
     * @return Empty list if no RPs defined yet.
     */
    public List<RhythmParameter<?>> loadCompactViewModeVisibleRPs(Rhythm r)
    {
        List<RhythmParameter<?>> res = new ArrayList<>();

        String rProp = buildCompactViewRhythmPropertyName(r);
        String s = song.getClientProperties().get(rProp, null);
        if (s != null)
        {
            String strs[] = s.trim().split(",");
            for (String str : strs)
            {
                var rp = getRpFromId(r, str);
                if (rp != null)
                {
                    res.add(rp);
                } else
                {
                    LOGGER.log(Level.FINE,
                            "loadCompactViewModeVisibleRPs() Invalid value={0} for song property {1} in song={2}. Invalid rpId={3}",
                            new Object[]
                            {
                                s,
                                rProp, song.getName(), str
                            });
                }
            }
        }

        return res;
    }

    /**
     * Set the list of visible RPs in compact view mode for the specified rhythm.
     * <p>
     *
     * @param song
     * @param r
     * @param rps
     */
    public void storeCompactViewModeVisibleRPs(Rhythm r, List<RhythmParameter<?>> rps)
    {
        String rProp = buildCompactViewRhythmPropertyName(r);
        StringJoiner joiner = new StringJoiner(",");
        rps.forEach(rp -> joiner.add(rp.getId()));
        song.getClientProperties().put(rProp, joiner.toString());
    }


    /**
     * Save the ZoomFactor X in the song client properties.
     *
     * @param factor 1-100
     */
    public void storeZoomXFactor(int factor)
    {
        song.getClientProperties().put(PROP_ZOOM_FACTOR_X, Integer.toString(factor));
    }

    /**
     * Save the ZoomFactor Y in the song client properties.
     *
     * @param factor [0-100]
     */
    public void storeZoomYFactor(int factor)
    {
        song.getClientProperties().put(PROP_ZOOM_FACTOR_Y, Integer.toString(factor));
    }

    /**
     * Get the ZoomFactor X value from song client properties.
     *
     * @return -1 if client property is not defined or invalid.
     */
    public int loadZoomXFactor()
    {
        return loadZoomFactor(true);
    }

    /**
     * Get the ZoomFactor Y value from song client properties.
     *
     * @return -1 if client property is not defined or invalid.
     */
    public int loadZoomYFactor()
    {
        return loadZoomFactor(false);
    }

    // ==================================================================================================================
    // Private methods
    // ==================================================================================================================    

    private String buildCompactViewRhythmPropertyName(Rhythm r)
    {
        String rProp = PROP_COMPACT_VIEW_MODE_VISIBLE_RPS + "_" + r.getUniqueId();
        return rProp;
    }

    /**
     * Get a RhythmParameter from r which match rpId.
     *
     * @param r
     * @param rpId
     * @return Can be null
     */
    private RhythmParameter<?> getRpFromId(Rhythm r, String rpId)
    {
        return r.getRhythmParameters().stream()
                .filter(rp -> rp.getId().equals(rpId))
                .findAny()
                .orElse(null);
    }

    private int loadZoomFactor(boolean isZoomX)
    {
        String prop = isZoomX ? PROP_ZOOM_FACTOR_X : PROP_ZOOM_FACTOR_Y;
        var strValue = song.getClientProperties().get(prop, null);
        int res = -1;
        if (strValue != null)
        {
            try
            {
                res = Integer.parseInt(strValue);
                if (res < 0 || res > 100)
                {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e)
            {
                LOGGER.log(Level.WARNING, "loadZoomFactor() Invalid zoom factor client property={0}, value={1} in song={2}", new Object[]
                {
                    prop,
                    strValue,
                    song.getName()
                });
                res = -1;
            }
        }
        return res;
    }
}

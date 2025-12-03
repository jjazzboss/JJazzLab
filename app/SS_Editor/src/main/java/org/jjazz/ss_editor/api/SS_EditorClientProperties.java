/*
 * 
 *   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 *   Copyright @2019 Jerome Lelasseux. All rights reserved.
 * 
 *   This file is part of the JJazzLab software.
 *    
 *   JJazzLab is free software: you can redistribute it and/or modify
 *   it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *   as published by the Free Software Foundation, either version 3 of the License, 
 *   or (at your option) any later version.
 * 
 *   JJazzLab is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *  
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *  
 *   Contributor(s): 
 * 
 */
package org.jjazz.ss_editor.api;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.song.api.Song;

/**
 * The Song client properties used by a SS_Editor and its components.
 * <p>
 * It is the responsibility of the object which updates one of these client properties to call {@link org.jjazz.song.api.Song#setSaveNeeded(boolean) } if
 * needed.
 *
 * @see org.jjazz.utilities.api.StringProperties
 */
public class SS_EditorClientProperties
{
    
    public enum ViewMode
    {
        NORMAL, COMPACT;
    }

    //==========================================================================================================================================
    // Note: do not modify the property values below (e.g. "PropSsEditorViewMode"), they are used in Song serialization
    //==========================================================================================================================================

    // All old and new values are encoded as String since Song client properties are StringProperties
    /**
     * Song client (String) property: current view mode.
     * <p>
     * (as String) oldValue=old mode, newValue=new mode.
     */
    static public final String PROP_VIEW_MODE = "PropViewMode";
    /**
     * Song client (String) property: the visible rhythm parameters in compact view mode for a given rhythm.
     * <p>
     * This is the base name used to build the rhythm-specific property name.
     * <p>
     * (as String) oldValue=list of visible RPs, newValue=new list of visible RPs
     *
     * @see #getRhythmIdFromCompactViewRhythmPropertyName(java.lang.String)
     */
    static public final String PROP_COMPACT_VIEW_MODE_VISIBLE_RPS = "PropCompactViewModeVisibleRps";    // do not use '_'
    /**
     * Song client (String) property: zoom X factor
     * <p>
     * (as String) oldValue=old int, newValue=new int
     */
    public static final String PROP_ZOOM_FACTOR_X = "PropSsEditorZoomFactorX";
    /**
     * Song client (String) property: zoom Y factor
     * <p>
     * (as String) oldValue=old int, newValue=new int
     */
    public static final String PROP_ZOOM_FACTOR_Y = "PropSsEditorZoomFactorY";
    private static final Logger LOGGER = Logger.getLogger(SS_EditorClientProperties.class.getSimpleName());

    /**
     *
     * @param song
     * @return COMPACT by default
     * @see #PROP_VIEW_MODE
     */
    static public ViewMode getViewMode(Song song)
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

    /**
     *
     * @param song
     * @param mode
     * @see #PROP_VIEW_MODE
     */
    static public void setViewMode(Song song, ViewMode mode)
    {
        song.getClientProperties().put(PROP_VIEW_MODE, mode != null ? mode.name() : null);
    }


    /**
     * Get the list of visible RPs in compact view mode for the specified rhythm.
     * <p>
     *
     * @param song
     * @param r
     * @return Can be empty
     * @see #PROP_COMPACT_VIEW_MODE_VISIBLE_RPS
     */
    static public List<RhythmParameter<?>> getCompactViewModeVisibleRPs(Song song, Rhythm r)
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
                            "getCompactViewModeVisibleRPs() Invalid value={0} for song property {1} in song={2}. Invalid rpId={3}",
                            new Object[]
                            {
                                s, rProp, song.getName(), str
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
     * @param rps  Can not be null
     * @see #PROP_COMPACT_VIEW_MODE_VISIBLE_RPS
     */
    static public void setCompactViewModeVisibleRPs(Song song, Rhythm r, List<RhythmParameter<?>> rps)
    {
        String rProp = buildCompactViewRhythmPropertyName(r);
        StringJoiner joiner = new StringJoiner(",");
        rps.forEach(rp -> joiner.add(rp.getId()));
        song.getClientProperties().put(rProp, joiner.toString());
    }

    /**
     * Return the rhythmId if propName is a PROP_COMPACT_VIEW_MODE_VISIBLE_RPS-based name.
     *
     * @param propName
     * @return Null if propName is not a PROP_COMPACT_VIEW_MODE_VISIBLE_RPS-based name
     * @see #PROP_COMPACT_VIEW_MODE_VISIBLE_RPS
     */
    static public String getRhythmIdFromCompactViewRhythmPropertyName(String propName)
    {
        String res = null;
        if (propName.startsWith(PROP_COMPACT_VIEW_MODE_VISIBLE_RPS) && propName.length() > PROP_COMPACT_VIEW_MODE_VISIBLE_RPS.length() + 1)
        {
            res = propName.substring(PROP_COMPACT_VIEW_MODE_VISIBLE_RPS.length() + 1);
        }
        return res;
    }

    /**
     *
     * @param song
     * @return 50 by default
     * @see #PROP_ZOOM_FACTOR_X
     */
    public static int getZoomXFactor(Song song)
    {
        return Math.clamp(song.getClientProperties().getInt(PROP_ZOOM_FACTOR_X, 80), 0, 100);
    }

    /**
     *
     * @param song
     * @param factor [0;100]
     * @see #PROP_ZOOM_FACTOR_X
     */
    public static void setZoomXFactor(Song song, int factor)
    {
        song.getClientProperties().putInt(PROP_ZOOM_FACTOR_X, Math.clamp(factor, 0, 100));
    }

    /**
     *
     * @param song
     * @return 50 by default
     * @see #PROP_ZOOM_FACTOR_Y
     */
    public static int getZoomYFactor(Song song)
    {
        return Math.clamp(song.getClientProperties().getInt(PROP_ZOOM_FACTOR_Y, 50), 0, 100);
    }

    /**
     *
     * @param song
     * @param factor [0;100]
     * @see #PROP_ZOOM_FACTOR_Y
     */
    public static void setZoomYFactor(Song song, int factor)
    {
        song.getClientProperties().putInt(PROP_ZOOM_FACTOR_Y, Math.clamp(factor, 0, 100));
    }

    // ==================================================================================================================
    // Private methods
    // ==================================================================================================================    
    /**
     * Build a PROP_COMPACT_VIEW_MODE_VISIBLE_RPS-based property name for the specified rhythm.
     *
     * @param r
     * @return
     */
    static private String buildCompactViewRhythmPropertyName(Rhythm r)
    {
        String rProp = PROP_COMPACT_VIEW_MODE_VISIBLE_RPS + "_" + r.getUniqueId();
        return rProp;
    }

    /**
     * Get a RhythmParameter from r which matches rpId.
     *
     * @param r
     * @param rpId
     * @return Can be null
     */
    static private RhythmParameter<?> getRpFromId(Rhythm r, String rpId)
    {
        return r.getRhythmParameters().stream()
                .filter(rp -> rp.getId().equals(rpId))
                .findAny()
                .orElse(null);
    }
    
    
}

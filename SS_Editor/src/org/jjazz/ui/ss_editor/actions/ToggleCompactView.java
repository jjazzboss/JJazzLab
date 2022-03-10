/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 *  This file is part of the JJazzLabX software.
 *   
 *  JJazzLabX is free software: you can redistribute it and/or modify
 *  it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *  as published by the Free Software Foundation, either version 3 of the License, 
 *  or (at your option) any later version.
 *
 *  JJazzLabX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 *  GNU Lesser General Public License for more details.
 * 
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JJazzLabX.  If not, see <https://www.gnu.org/licenses/>
 * 
 *  Contributor(s): 
 */
package org.jjazz.ui.ss_editor.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import static javax.swing.Action.ACCELERATOR_KEY;
import static javax.swing.Action.NAME;
import static javax.swing.Action.SHORT_DESCRIPTION;
import static javax.swing.Action.SMALL_ICON;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Marker;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_TempoFactor;
import org.jjazz.song.api.Song;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.ui.ss_editor.api.SS_EditorTopComponent;
import org.jjazz.util.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

/**
 * The action to switch between full/compact RP view mode, plus methods to manage the compact view mode using song client
 * properties.
 * <p>
 */
@ActionID(category = "JJazz", id = "org.jjazz.ui.ss_editor.actions.togglecompactview")
@ActionRegistration(displayName = "#CTL_ToggleCompactView", lazy = false)
@ActionReferences(
        {
            // @ActionReference(path = "Actions/SongPart", position = 300)
            @ActionReference(path = "Shortcuts", name = "V")
        })
public class ToggleCompactView extends AbstractAction
{

    /**
     * The Song client property to store the view mode.
     */
    public static final String PROP_COMPACT_VIEW_MODE = "PropCompactViewMode";
    /**
     * The Song client property basename to store the visible RPs in compact view mode.
     * 
     * For example the property name will be "PropCompactViewModeVisibleRps_swingId" for rhythm swing
     */
    public static final String PROP_COMPACT_VIEW_MODE_VISIBLE_RPS = "PropCompactViewModeVisibleRps";

    private static final Logger LOGGER = Logger.getLogger(ToggleCompactView.class.getSimpleName());


    private ToggleCompactView()
    {
        putValue(NAME, ResUtil.getString(getClass(), "CTL_ToggleCompactView"));
        putValue(SHORT_DESCRIPTION, ResUtil.getString(getClass(), "CTL_ToggleCompactView"));        
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("V"));     // Only if action is used to create a menu entry
        putValue(SMALL_ICON, new ImageIcon(getClass().getResource("/org/jjazz/ui/ss_editor/actions/resources/CompactViewMode-OFF.png")));
        putValue(LARGE_ICON_KEY, new ImageIcon(getClass().getResource("/org/jjazz/ui/ss_editor/actions/resources/CompactViewMode-ON.png")));
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        SS_EditorTopComponent ssTc = SS_EditorTopComponent.getVisible();
        if (ssTc == null)
        {
            return;
        }

        // Just switch between compact and full mode
        var song = ssTc.getSS_Editor().getSongModel();
        setSongInCompactViewMode(song, !isSongInCompactViewMode(song));

    }


    // =========================================================
    // Convenience static methods to manage the mode using song client properties
    // =========================================================
    static public boolean isSongInCompactViewMode(Song song)
    {
        return song.getClientProperty(PROP_COMPACT_VIEW_MODE, "true").equals("true");
    }

    static public void setSongInCompactViewMode(Song song, boolean b)
    {
        song.putClientProperty(PROP_COMPACT_VIEW_MODE, b ? "true" : "false");
    }

    /**
     * Get the list of visible RPs in compact view mode for the specified rhythm.
     * <p>
     * Use the song client property PROP_COMPACT_VIEW_MODE_VISIBLE_RPS. If property is not set yet, create a list using only the
     * primary Rhythm Parameters, and possibly Marker and Tempo RPs if actually used in the song, then set the property.
     *
     * @param song
     * @param r
     * @return
     */
    static public List<RhythmParameter<?>> getCompactViewModeVisibleRPs(Song song, Rhythm r)
    {
        List<RhythmParameter<?>> res = new ArrayList<>();

        String rProp = buildCompactViewRhythmPropertyName(r);
        String s = song.getClientProperty(rProp, null);
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
                    LOGGER.fine("getCompactViewModeVisibleRPs() Invalid value=" + s + " for song property " + rProp + " in song=" + song.getName() + ". Invalid rpId=" + str);
                }
            }
        }

        if (res.isEmpty())
        {
            res = getDefaultVisibleRpsInCompactMode(song, r);
            setCompactViewModeVisibleRPs(song, r, res);
        }


        return res;
    }

    /**
     * Set the list of visible RPs in compact view mode for the specified rhythm.
     * <p>
     * Update the song client property PROP_COMPACT_VIEW_MODE_VISIBLE_RPS.
     *
     * @param song
     * @param r
     * @param rps
     */
    static public void setCompactViewModeVisibleRPs(Song song, Rhythm r, List<RhythmParameter<?>> rps)
    {
        String rProp = buildCompactViewRhythmPropertyName(r);
        StringJoiner joiner = new StringJoiner(",");
        rps.forEach(rp -> joiner.add(rp.getId()));
        song.putClientProperty(rProp, joiner.toString());
    }


    // ==============================================================================================
    // Private methods
    // ==============================================================================================
    static private String buildCompactViewRhythmPropertyName(Rhythm r)
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
    static private RhythmParameter<?> getRpFromId(Rhythm r, String rpId)
    {
        return r.getRhythmParameters().stream()
                .filter(rp -> rp.getId().equals(rpId))
                .findAny()
                .orElse(null);
    }


    /**
     * Decide which RhythmParameters are visible by default in compact mode for the specified new rhythm.
     *
     * @param song
     * @param r
     * @return A non-empty list
     */
    static private List<RhythmParameter<?>> getDefaultVisibleRpsInCompactMode(Song song, Rhythm r)
    {

        SongStructure sgs = song.getSongStructure();

        var tmp = new ArrayList<RhythmParameter<?>>();


        // Add only primary RPs
        r.getRhythmParameters().stream()
                .filter(rp -> rp.isPrimary())
                .forEach(tmp::add);


        // Add marker only if supported by the rhythm and actually used by some SongParts 
        RP_SYS_Marker rMarker = RP_SYS_Marker.getMarkerRp(r);
        if (rMarker != null && !tmp.contains(rMarker))
        {
            for (SongPart spt : sgs.getSongParts())
            {
                RP_SYS_Marker rpm = RP_SYS_Marker.getMarkerRp(spt.getRhythm());
                if (rpm != null && !spt.getRPValue(rpm).equals(rpm.getDefaultValue()))
                {
                    tmp.add(rMarker);
                    break;
                }
            }
        }

        // Add tempo only if supported by the rhythm and actually used by some SongParts 
        RP_SYS_TempoFactor rTempo = RP_SYS_TempoFactor.getTempoFactorRp(r);
        if (rTempo != null && !tmp.contains(rTempo))
        {
            for (SongPart spt : sgs.getSongParts())
            {
                RP_SYS_TempoFactor rpm = RP_SYS_TempoFactor.getTempoFactorRp(spt.getRhythm());
                if (rpm != null && !spt.getRPValue(rpm).equals(rpm.getDefaultValue()))
                {
                    tmp.add(rTempo);
                    break;
                }
            }
        }


        // Reorder
        var res = new ArrayList<RhythmParameter<?>>();
        for (var rp : r.getRhythmParameters())
        {
            if (tmp.contains(rp))
            {
                res.add(rp);
            }
        }

        if (res.isEmpty())
        {
            LOGGER.warning("getDefaultVisibleRpsInCompactMode() no default compact-mode visible RPs for r=" + r + ", using 1st RP as default");
            res.add(r.getRhythmParameters().get(0));
        }

        return res;
    }


}

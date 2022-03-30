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
package org.jjazz.ui.ss_editor;

import static com.google.common.base.Preconditions.checkNotNull;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Marker;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_TempoFactor;
import org.jjazz.song.api.Song;
import org.jjazz.songstructure.api.SgsChangeListener;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.event.SgsChangeEvent;
import org.jjazz.songstructure.api.event.SptAddedEvent;
import org.jjazz.songstructure.api.event.SptReplacedEvent;
import org.jjazz.ui.ss_editor.api.SS_Editor;
import org.jjazz.ui.utilities.api.Utilities;

/**
 * Manage the compact/full view modes.
 */
public class CompactViewModeController implements PropertyChangeListener, SgsChangeListener
{

    /**
     * The Song client property to store the view mode.
     */
    public static final String PROP_COMPACT_VIEW_MODE = "PropCompactViewMode";
    /**
     * The Song client property basename to store the visible RPs in compact view mode.
     * <p>
     * For example the property name will be "PropCompactViewModeVisibleRps_swingId" for rhythm swing
     */
    public static final String PROP_COMPACT_VIEW_MODE_VISIBLE_RPS = "PropCompactViewModeVisibleRps";

    private final SS_Editor editor;
    private final Song song;
    private static final Logger LOGGER = Logger.getLogger(CompactViewModeController.class.getSimpleName());

    public CompactViewModeController(SS_Editor editor)
    {
        checkNotNull(editor);
        this.editor = editor;
        this.song = editor.getSongModel();

        // Precalculate default VisibleRP in compact mode values for each rhythm
        // To be done BEFORE listening to song changes, as this will trigger song client property changes and cause Issue #304
        writeVisibleRPsInCompactModeIfRequired(song.getSongStructure().getUniqueRhythms(false, true));

        // Register song to listen for closing and adding new rhythms events
        song.addPropertyChangeListener(this);
        song.getSongStructure().addSgsChangeListener(this);


        updateEditor(editor);

    }
    // ======================================================================
    // PropertyChangeListener interface
    // ======================================================================    

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getSource() == song)
        {
            if (evt.getPropertyName().equals(Song.PROP_CLOSED))
            {
                song.removePropertyChangeListener(this);
                song.getSongStructure().removeSgsChangeListener(this);
            } else if (evt.getPropertyName().equals(CompactViewModeController.PROP_COMPACT_VIEW_MODE))
            {
                // View mode has changed, update the editor
                updateEditor(editor);

            } else if (evt.getPropertyName().startsWith(CompactViewModeController.PROP_COMPACT_VIEW_MODE_VISIBLE_RPS))
            {
                // Compact mode visible RPs have changed, update if required
                if (isSongInCompactViewMode(song))
                {
                    updateEditor(editor);
                }
            }
        }
    }

    //------------------------------------------------------------------------------
    // SgsChangeListener interface
    //------------------------------------------------------------------------------   
    @Override
    public void authorizeChange(SgsChangeEvent e) throws UnsupportedEditException
    {
        // Nothing
    }

    @Override
    public void songStructureChanged(final SgsChangeEvent e)
    {

        // Model changes can be generated outside the EDT
        Runnable run = new Runnable()
        {
            @Override
            public void run()
            {
                LOGGER.log(Level.FINE, "CompactViewModeController.songStructureChanged() -- e={0} spts={1}", new Object[]
                {
                    e, e.getSongParts()
                });   //NOI18N

                // Get the new rhythms
                List<Rhythm> rhythms = new ArrayList<>();

                if (e instanceof SptAddedEvent)
                {
                    rhythms.addAll(e.getSongParts().stream()
                            .map(spt -> spt.getRhythm())
                            .collect(Collectors.toList()));

                } else if (e instanceof SptReplacedEvent)
                {
                    SptReplacedEvent re = (SptReplacedEvent) e;
                    rhythms.addAll(re.getNewSpts().stream()
                            .map(spt -> spt.getRhythm())
                            .collect(Collectors.toList()));
                }

                writeVisibleRPsInCompactModeIfRequired(rhythms);

            }
        };
        Utilities.invokeLaterIfNeeded(run);
    }

    
    /**
     * Get the list of visible RPs in compact view mode for the specified rhythm.
     * <p>
     * Use the song client property PROP_COMPACT_VIEW_MODE_VISIBLE_RPS.
     *
     * @param song
     * @param r
     * @return Empty list if no RPs defined yet.
     */
    static public List<RhythmParameter<?>> readCompactViewModeVisibleRPs(Song song, Rhythm r)
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
    static public void writeCompactViewModeVisibleRPs(Song song, Rhythm r, List<RhythmParameter<?>> rps)
    {
        String rProp = buildCompactViewRhythmPropertyName(r);
        StringJoiner joiner = new StringJoiner(",");
        rps.forEach(rp -> joiner.add(rp.getId()));
        song.putClientProperty(rProp, joiner.toString());
    }


    static public boolean isSongInCompactViewMode(Song song)
    {
        return song.getClientProperty(PROP_COMPACT_VIEW_MODE, "true").equals("true");
    }

    static public void setSongInCompactViewMode(Song song, boolean b)
    {
        if (isSongInCompactViewMode(song) != b)
        {
            song.putClientProperty(PROP_COMPACT_VIEW_MODE, b ? "true" : "false");
        }
    }

    // ==============================================================================================
    // Private methods
    // ==============================================================================================

    /**
     * Process each rhythm and create the default compact view mode visible RPs, if not already done.
     *
     * @param rhythms
     */
    private void writeVisibleRPsInCompactModeIfRequired(List<Rhythm> rhythms)
    {
        for (Rhythm r : rhythms)
        {
            if (readCompactViewModeVisibleRPs(song, r).isEmpty())
            {
                // Rhythm is new, need to set its visible RPs in compact mode
                var rps = getDefaultVisibleRpsInCompactMode(song, r);
                writeCompactViewModeVisibleRPs(song, r, rps);
            }
        }
    }

    /**
     * Get the RhythmParameters visible by default in compact mode for the specified new rhythm.
     * <p>
     * Use the primary Rhythm Parameters, and possibly Marker and Tempo RPs if actually used in the song.
     *
     * @param song
     * @param r
     * @return A non-empty list
     */
    private List<RhythmParameter<?>> getDefaultVisibleRpsInCompactMode(Song song, Rhythm r)
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
     * Update the editor to show/hide RPs depending on the view mode.
     */
    static private void updateEditor(SS_Editor editor)
    {
        boolean b = CompactViewModeController.isSongInCompactViewMode(editor.getSongModel());
        var allRhythms = editor.getSongModel().getSongStructure().getUniqueRhythms(false, true);

        for (var r : allRhythms)
        {
            List<RhythmParameter<?>> rps = b ? readCompactViewModeVisibleRPs(editor.getSongModel(), r) : r.getRhythmParameters();
            if (rps.isEmpty())
            {
                // Might happen e.g. when duplicating a song with multiple time signatures
            }
            editor.setVisibleRps(r, rps);
        }
    }
}

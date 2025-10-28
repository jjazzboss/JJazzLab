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
package org.jjazz.pianoroll.spi;

import java.awt.Color;
import java.awt.Font;
import java.beans.PropertyChangeListener;
import org.openide.util.Lookup;

public interface PianoRollEditorSettings
{

    public static String PROP_BAR_LINE_COLOR = "BarLineColor";
    public static String PROP_BLACK_KEY_LANE_BACKGROUND_COLOR = "BlackKeyLaneBackgroundColor";
    public static String PROP_WHITE_KEY_LANE_BACKGROUND_COLOR = "WhiteKeyLaneBackgroundColor";
    public static String PROP_LOOP_ZONE_WHITE_KEY_LANE_BACKGROUND_COLOR = "LoopZoneWhiteKeyLaneBackgroundColor";
    public static String PROP_LOOP_ZONE_BLACK_KEY_LANE_BACKGROUND_COLOR = "LoopZoneBlackKeyLaneBackgroundColor";
    public static String PROP_RULER_BACKGROUND_COLOR = "RulerBackgroundColor";
    public static String PROP_RULER_TS_LANE_BACKGROUND_COLOR = "RulerTsLaneBackgroundColor";
    public static String PROP_RULER_BAR_TICK_COLOR = "RulerBarTickColor";
    public static String PROP_RULER_BASE_FONT = "RulerBaseFont";
    public static String PROP_NOTE_COLOR = "NoteColor";
    public static String PROP_SELECTED_NOTE_COLOR = "SelectedNoteColor";
    public static String PROP_FOCUSED_NOTE_CONTOUR_COLOR = "FocusedNoteContourColor";
    public static String PROP_NOTE_CONTOUR_COLOR = "NoteContourColor";

    public static PianoRollEditorSettings getDefault()
    {
        PianoRollEditorSettings result = Lookup.getDefault().lookup(PianoRollEditorSettings.class);
        return result;
    }

    Color getBarLineColor();

    void setBarLineColor(Color color);

    Color getBlackKeyLaneBackgroundColor();

    void setBlackKeyLaneBackgroundColor(Color color);

    Color getWhiteKeyLaneBackgroundColor();

    void setWhiteKeyLaneBackgroundColor(Color color);

    Color getLoopZoneWhiteKeyLaneBackgroundColor();

    void setLoopZoneWhiteKeyLaneBackgroundColor(Color color);

    Color getLoopZoneBlackKeyLaneBackgroundColor();

    void setLoopZoneBlackKeyLaneBackgroundColor(Color color);

    Color getRulerBackgroundColor();

    void setRulerBackgroundColor(Color color);

    Color getRulerTsLaneBackgroundColor();

    void setRulerTsLaneBackgroundColor(Color color);

    Color getRulerBarTickColor();

    void setRulerBarTickColor(Color color);

    Font getRulerBaseFont();

    void setRulerBaseFont(Font font);

    Color getNoteColor();

    void setNoteColor(Color color);

    Color getNoteContourColor();

    void setNoteContourColor(Color color);

    Color getFocusedNoteContourColor();

    void setFocusedNoteContourColor(Color color);

    Color getSelectedNoteColor();

    void setSelectedNoteColor(Color color);

    void addPropertyChangeListener(PropertyChangeListener listener);

    void removePropertyChangeListener(PropertyChangeListener listener);
}

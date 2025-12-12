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
package org.jjazz.cl_editor.api;

import java.awt.Color;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.quantizer.api.Quantization;
import org.jjazz.song.api.Song;

/**
 * The Song and ChordLeadSheetItem client properties used by a CL_Editor and its components (e.g. BarRenderers, ItemRenderers, ...).
 * <p>
 * It is the responsibility of the object which updates client property to call {@link org.jjazz.song.api.Song#setSaveNeeded(boolean) } if needed.
 *
 * @see org.jjazz.utilities.api.StringProperties
 */
public class CL_EditorClientProperties
{

    public static final int BAR_ANNOTATION_MAX_NB_LINES = 6;

    //==========================================================================================================================================
    // Note: do not modify the property values below (e.g. "PropBarAnnotationsVisible"), they are used in Song or ChordLeadSheetItem serialization
    //==========================================================================================================================================

    // All old and new values are encoded as String since Song and ChordLeadSheetItem client properties are StringProperties.
    /**
     * Song client (String) property: zoom X factor (impacting nb of columns)
     * <p>
     * (as String) oldValue=old int, newValue=new int
     */
    public static final String PROP_ZOOM_FACTOR_X = "PropClEditorZoomFactorX";
    /**
     * Song client (String) property: zoom Y factor (impacting BarBox height)
     * <p>
     * (as String) oldValue=old int, newValue=new int
     */
    public static final String PROP_ZOOM_FACTOR_Y = "PropClEditorZoomFactorY";
    /**
     * Song client (String) property: bar annotations visibility.
     * <p>
     * (as String) oldValue=old boolean, newValue=new boolean
     */
    public static final String PROP_BAR_ANNOTATION_VISIBLE = "PropBarAnnotationsVisible";
    /**
     * Song client (String) property: nb of lines of bar annotations
     * <p>
     * (as String) oldValue=old int, newValue=new int
     */
    public static final String PROP_BAR_ANNOTATION_NB_LINES = "PropBarAnnotationsNbLines";
    /**
     * CLI_Section client (String) property: user quantization for moving chord symbols.
     * <p>
     * (as String) oldvalue=old quantization value, newValue=new quantization value
     */
    static public final String PROP_SECTION_USER_QUANTIZATION = "PropSectionQuantization";
    /**
     * CLI_Section client (String) property: section color.
     * <p>
     * (as String) oldvalue=old color, newValue=new color.
     */
    static public final String PROP_SECTION_COLOR = "PropSectionColor";
    /**
     * CLI_Section client (String) property: section starts on new line
     * <p>
     * (as String) oldvalue=old boolean, newValue=new boolean.
     */
    static public final String PROP_SECTION_START_ON_NEW_LINE = "PropSectionStartOnNewLine";
    /**
     * CLI_ChordSymbol client (String) property: chord font color.
     * <p>
     * (as String) oldvalue=old color, newValue=new color.
     */
    static public final String PROP_CHORD_USER_FONT_COLOR = "SongPropUserFontColor";

    /**
     *
     * @param song
     * @return false by default
     * @see #PROP_BAR_ANNOTATION_VISIBLE
     */
    public static boolean isBarAnnotationVisible(Song song)
    {
        return song.getClientProperties().getBoolean(PROP_BAR_ANNOTATION_VISIBLE, false);
    }

    /**
     *
     * @param song
     * @param b
     * @see #PROP_BAR_ANNOTATION_VISIBLE
     */
    public static void setBarAnnotationVisible(Song song, boolean b)
    {
        song.getClientProperties().putBoolean(PROP_BAR_ANNOTATION_VISIBLE, b);
    }

    /**
     *
     * @param song
     * @return 1 by default
     * @see #PROP_BAR_ANNOTATION_NB_LINES
     * @see #BAR_ANNOTATION_MAX_NB_LINES
     */
    public static int getBarAnnotationNbLines(Song song)
    {
        return Math.clamp(song.getClientProperties().getInt(PROP_BAR_ANNOTATION_NB_LINES, 1), 1, BAR_ANNOTATION_MAX_NB_LINES);
    }

    /**
     *
     * @param song
     * @param n
     * @see #PROP_BAR_ANNOTATION_NB_LINES
     * @see #BAR_ANNOTATION_MAX_NB_LINES
     */
    public static void setBarAnnotationNbLines(Song song, int n)
    {
        song.getClientProperties().putInt(PROP_BAR_ANNOTATION_NB_LINES, Math.clamp(n, 1, BAR_ANNOTATION_MAX_NB_LINES));
    }

    /**
     *
     * @param song
     * @return 80 by default (corresponds to 4 bars)
     * @see #PROP_ZOOM_FACTOR_X
     */
    public static int getZoomXFactor(Song song)
    {
        return Math.clamp(song.getClientProperties().getInt(PROP_ZOOM_FACTOR_X, 80), 0, 100);
    }

    /**
     * Note: it might be easier to use {@link CL_Editor#setNbColumns(int) }.
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

    /**
     *
     * @param cliSection
     * @param q          Can be null
     * @see #PROP_SECTION_USER_QUANTIZATION
     */
    public static void setSectionUserQuantization(CLI_Section cliSection, Quantization q)
    {
        cliSection.getClientProperties().put(PROP_SECTION_USER_QUANTIZATION, q == null ? null : q.name());
    }

    /**
     *
     * @param cliSection
     * @return Can be null
     * @see #PROP_SECTION_USER_QUANTIZATION
     */
    public static Quantization getSectionUserQuantization(CLI_Section cliSection)
    {
        String qString = cliSection.getClientProperties().get(PROP_SECTION_USER_QUANTIZATION, null);
        return Quantization.isValidStringValue(qString) ? Quantization.valueOf(qString) : null;
    }

    /**
     *
     * @param cliSection
     * @return false by default
     * @see #PROP_SECTION_START_ON_NEW_LINE
     */
    public static boolean isSectionIsOnNewLine(CLI_Section cliSection)
    {
        boolean b = cliSection.getClientProperties().getBoolean(PROP_SECTION_START_ON_NEW_LINE, false);
        return b;
    }

    /**
     *
     * @param cliSection
     * @param b
     * @see #PROP_SECTION_START_ON_NEW_LINE
     */
    public static void setSectionIsOnNewLine(CLI_Section cliSection, boolean b)
    {
        cliSection.getClientProperties().putBoolean(PROP_SECTION_START_ON_NEW_LINE, b ? true : null);
    }

    /**
     *
     * @param cliSection
     * @return null by default
     * @see #PROP_SECTION_COLOR
     */
    public static Color getSectionColor(CLI_Section cliSection)
    {
        Color c = cliSection.getClientProperties().getColor(PROP_SECTION_COLOR, null);
        return c;
    }

    /**
     *
     * @param cliSection
     * @param c          Can be null
     * @see #PROP_SECTION_COLOR
     */
    public static void setSectionColor(CLI_Section cliSection, Color c)
    {
        cliSection.getClientProperties().putColor(PROP_SECTION_COLOR, c);
    }

    /**
     *
     * @param cliCs
     * @return null by default
     * @see #PROP_CHORD_USER_FONT_COLOR
     */
    public static Color getChordSymbolUserColor(CLI_ChordSymbol cliCs)
    {
        Color c = cliCs.getClientProperties().getColor(PROP_CHORD_USER_FONT_COLOR, null);
        return c;
    }

    /**
     *
     * @param cliCs
     * @param c     Can be null
     * @see #PROP_CHORD_USER_FONT_COLOR
     */
    public static void setChordSymbolUserColor(CLI_ChordSymbol cliCs, Color c)
    {
        cliCs.getClientProperties().putColor(PROP_CHORD_USER_FONT_COLOR, c);
    }


}

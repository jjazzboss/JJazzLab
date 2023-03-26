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
package org.jjazz.ui.cl_editor;

import java.awt.Color;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.leadsheet.chordleadsheet.api.Section;
import org.jjazz.quantizer.api.Quantization;
import org.jjazz.song.api.Song;
import org.jjazz.ui.cl_editor.api.CL_Editor;
import org.jjazz.ui.colorsetmanager.api.ColorSetManager;

/**
 * Handles the CL_Editor data saved as song client properties.
 */
public class SongSpecificEditorProperties
{

    private static final String PROP_ZOOM_FACTOR_X = "PropClEditorZoomFactorX";
    private static final String PROP_ZOOM_FACTOR_Y = "PropClEditorZoomFactorY";
    
    private final Song song;
    private static final Logger LOGGER = Logger.getLogger(SongSpecificEditorProperties.class.getSimpleName());

    public SongSpecificEditorProperties(Song song)
    {
        this.song = song;
    }

    /**
     * A section was renamed: update related song properties.
     *
     * @param oldName The old name of the section
     * @param section The renamed section
     */
    public void sectionRenamed(String oldName, Section section)
    {
        var oldDummy = new Section(oldName, TimeSignature.FOUR_FOUR);

        var q = loadSectionQuantization(oldDummy);
        storeSectionQuantization(oldDummy, null);
        storeSectionQuantization(section, q);

        var c = loadSectionColor(oldDummy);
        storeSectionColor(oldDummy, null);
        storeSectionColor(section, c);

        var b = loadSectionIsOnNewLine(oldDummy);
        storeSectionIsOnNewLine(oldDummy, false);
        storeSectionIsOnNewLine(section, b);
    }

    /**
     * A section was removed: update related song properties.
     *
     * @param section
     */
    public void sectionRemoved(Section section)
    {
        storeSectionQuantization(section, null);
        storeSectionColor(section, null);
        storeSectionIsOnNewLine(section, false);
    }

    /**
     * Store the quantization value of section as a client property of the specified song.
     *
     * @param section
     * @param q       Can be null to remove this client property
     */
    public void storeSectionQuantization(Section section, Quantization q)
    {
        song.putClientProperty(CL_Editor.getSectionQuantizationPropertyName(section), q == null ? null : q.name());
    }

    /**
     * Get the quantization value for section from the client property of the specified song.
     *
     * @param section
     * @return Can be null
     */
    public Quantization loadSectionQuantization(Section section)
    {
        String qString = song.getClientProperty(CL_Editor.getSectionQuantizationPropertyName(section), null);
        return Quantization.isValidStringValue(qString) ? Quantization.valueOf(qString) : null;
    }

    /**
     * Check if section is on new line from the song client properties.
     *
     * @param section
     * @return false by default.
     */
    public boolean loadSectionIsOnNewLine(Section section)
    {
        String boolString = song.getClientProperty(CL_Editor.getSectionOnNewLinePropertyName(section), "false");
        boolean b = Boolean.parseBoolean(boolString);
        return b;
    }

    public void storeSectionIsOnNewLine(Section section, boolean b)
    {
        song.putClientProperty(CL_Editor.getSectionOnNewLinePropertyName(section), b ? Boolean.toString(true) : null);
    }

    /**
     * Store a color associated to the specified section.
     *
     * @param section
     * @param c       can be null
     */
    public void storeSectionColor(Section section, Color c)
    {
        song.putClientProperty(CL_Editor.getSectionColorPropertyName(section), c == null ? null : String.valueOf(c.getRGB()));
    }

    /**
     * Get the color associated to the specified section.
     *
     * @param section
     * @return Can be null. If not null it is one of the ColorSetManager reference colors.
     */
    public Color loadSectionColor(Section section)
    {
        String cString = song.getClientProperty(CL_Editor.getSectionColorPropertyName(section), null);
        Color c = null;
        if (cString != null)
        {
            try
            {
                c = new Color(Integer.parseInt(cString));
                if (!ColorSetManager.getDefault().isReferenceColor(c))
                {
                    c = null;
                }
            } catch (NumberFormatException ex)
            {
                // Nothing
            }
        }
        return c;
    }

    /**
     * Save the ZoomFactor X or Y in the song client properties.
     *
     * @param isZoomX If true use PROP_ZOOM_FACTOR_X, otherwise use PROP_ZOOM_FACTOR_Y
     * @param factor
     */
    public void storeZoomFactor(boolean isZoomX, int factor)
    {
        String prop = isZoomX ? PROP_ZOOM_FACTOR_X : PROP_ZOOM_FACTOR_Y;
        song.putClientProperty(prop, Integer.toString(factor));
    }

    /**
     * Get the ZoomFactor X or Y value from song client properties.
     *
     * @param isZoomX If true use PROP_ZOOM_FACTOR_X, otherwise use PROP_ZOOM_FACTOR_Y
     * @return -1 if client property is not defined or invalid.
     */
    public int loadZoomFactor(boolean isZoomX)
    {
        String prop = isZoomX ? PROP_ZOOM_FACTOR_X : PROP_ZOOM_FACTOR_Y;
        var strValue = song.getClientProperty(prop, null);
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

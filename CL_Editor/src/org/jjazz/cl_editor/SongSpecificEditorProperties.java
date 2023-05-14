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
package org.jjazz.cl_editor;

import java.awt.Color;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.quantizer.api.Quantization;
import org.jjazz.song.api.Song;
import org.jjazz.cl_editor.api.CL_Editor;
import org.jjazz.colorsetmanager.api.ColorSetManager;

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
     * Store the quantization value of section as a client property from CLI_Section.
     *
     * @param cliSection
     * @param q          Can be null to remove this client property
     */
    public void storeSectionQuantization(CLI_Section cliSection, Quantization q)
    {
        cliSection.getClientProperties().put(CL_Editor.PROP_SECTION_QUANTIZATION, q == null ? null : q.name());
    }

    /**
     * Get the quantization value for section from CLI_Section's client property.
     *
     * @param cliSection
     * @return Can be null
     */
    public Quantization loadSectionQuantization(CLI_Section cliSection)
    {
        String qString = cliSection.getClientProperties().get(CL_Editor.PROP_SECTION_QUANTIZATION, null);
        return Quantization.isValidStringValue(qString) ? Quantization.valueOf(qString) : null;
    }

    /**
     * Check if section is on new line from the CLI_Section client properties.
     *
     * @param cliSection
     * @return false by default.
     */
    public boolean loadSectionIsOnNewLine(CLI_Section cliSection)
    {
        String boolString = cliSection.getClientProperties().get(CL_Editor.PROP_SECTION_START_ON_NEW_LINE, "false");
        boolean b = Boolean.parseBoolean(boolString);
        return b;
    }

    public void storeSectionIsOnNewLine(CLI_Section cliSection, boolean b)
    {
        cliSection.getClientProperties().put(CL_Editor.PROP_SECTION_START_ON_NEW_LINE, b ? Boolean.toString(true) : null);
    }

    /**
     * Store a color associated to the specified CLI_Section.
     *
     * @param cliSection
     * @param c          can be null
     */
    public void storeSectionColor(CLI_Section cliSection, Color c)
    {
        cliSection.getClientProperties().put(CL_Editor.PROP_SECTION_COLOR, c == null ? null : String.valueOf(c.getRGB()));
    }

    /**
     * Get the color associated to the specified CLI_Section.
     *
     * @param cliSection
     * @return Can be null. If not null it is one of the ColorSetManager reference colors.
     */
    public Color loadSectionColor(CLI_Section cliSection)
    {
        String cString = cliSection.getClientProperties().get(CL_Editor.PROP_SECTION_COLOR, null);
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
        song.getClientProperties().put(prop, Integer.toString(factor));
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

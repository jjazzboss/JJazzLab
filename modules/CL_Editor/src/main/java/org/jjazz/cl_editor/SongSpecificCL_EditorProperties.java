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
 * Helper class to handle editor settings saved as song client properties.
 */
public class SongSpecificCL_EditorProperties
{

    private static final String PROP_ZOOM_FACTOR_X = "PropClEditorZoomFactorX";
    private static final String PROP_ZOOM_FACTOR_Y = "PropClEditorZoomFactorY";

    private final Song song;
    private static final Logger LOGGER = Logger.getLogger(SongSpecificCL_EditorProperties.class.getSimpleName());

    public SongSpecificCL_EditorProperties(Song song)
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

    public boolean loadBarAnnotationVisible()
    {
        return song.getClientProperties().getBoolean(CL_Editor.PROP_BAR_ANNOTATION_VISIBLE, false);
    }

    public void storeBarAnnotationVisible(boolean b)
    {
        song.getClientProperties().putBoolean(CL_Editor.PROP_BAR_ANNOTATION_VISIBLE, b);
    }

    public int loadBarAnnotationNbLines()
    {
        return song.getClientProperties().getInt(CL_Editor.PROP_BAR_ANNOTATION_NB_LINES, 1);
    }

    public void storeBarAnnotationNbLines(int n)
    {
        song.getClientProperties().putInt(CL_Editor.PROP_BAR_ANNOTATION_NB_LINES, n);
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
     * Save the ZoomFactor X in the song client properties.
     *
     * @param factor
     */
    public void storeZoomXFactor(int factor)
    {
        song.getClientProperties().put(PROP_ZOOM_FACTOR_X, Integer.toString(factor));
    }

    /**
     * Get the ZoomFactor X from song client properties.
     *
     * @return -1 if client property is not defined or invalid.
     */
    public int loadZoomXFactor()
    {
        var strValue = song.getClientProperties().get(PROP_ZOOM_FACTOR_X, null);
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
                LOGGER.log(Level.WARNING, "loadZoomXFactor() Invalid zoom factor client property={0}, value={1} in song={2}", new Object[]
                {
                    PROP_ZOOM_FACTOR_X,
                    strValue,
                    song.getName()
                });
                res = -1;
            }
        }
        return res;
    }

    /**
     * Save the ZoomFactor Y in the song client properties.
     *
     * @param factor
     */
    public void storeZoomYFactor(int factor)
    {
        song.getClientProperties().put(PROP_ZOOM_FACTOR_Y, Integer.toString(factor));
    }

    /**
     * Get the ZoomFactor Y from song client properties.
     *
     * @return -1 if client property is not defined or invalid.
     */
    public int loadZoomYFactor()
    {
        var strValue = song.getClientProperties().get(PROP_ZOOM_FACTOR_Y, null);
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
                LOGGER.log(Level.WARNING, "loadZoomYFactor() Invalid zoom factor client property={0}, value={1} in song={2}", new Object[]
                {
                    PROP_ZOOM_FACTOR_Y,
                    strValue,
                    song.getName()
                });
                res = -1;
            }
        }
        return res;
    }
}

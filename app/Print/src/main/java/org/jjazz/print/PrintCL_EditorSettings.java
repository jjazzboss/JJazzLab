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
package org.jjazz.print;

import java.awt.Color;
import java.awt.Font;
import java.beans.PropertyChangeListener;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import org.jjazz.cl_editor.spi.CL_EditorSettings;
import org.jjazz.cl_editor.spi.BarBoxSettings;
import org.jjazz.cl_editor.spi.BarRendererSettings;
import org.jjazz.cl_editor.itemrenderer.api.IR_ChordSymbolSettings;
import org.jjazz.cl_editor.itemrenderer.api.IR_SectionSettings;
import org.jjazz.cl_editor.itemrenderer.api.IR_TimeSignatureSettings;
import org.jjazz.cl_editor.itemrenderer.api.ItemRendererSettings;

/**
 * Special CL_Editor settings only for the print version.
 * <p>
 * Embeds all the sub settings.
 * <p>
 * @todo use ItemRendererSettingsAdapter and IR_ChordSymbolSettingsAdapter (and possibly create Adapters for other settings)
 */
public class PrintCL_EditorSettings implements CL_EditorSettings
{

    private final CL_EditorSettings defaultSettings;
    private final BarBoxSettings bbSettings;
    private final BarRendererSettings brSettings;
    private final ItemRendererSettings irSettings;
    private final IR_ChordSymbolSettings csSettings;

    public PrintCL_EditorSettings(CL_EditorSettings defSettings)
    {
        this.defaultSettings = defSettings;
        bbSettings = new PrintBarBoxSettings(defaultSettings.getBarBoxSettings());
        brSettings = new PrintBarRendererSettings(defaultSettings.getBarBoxSettings().getBarRendererSettings());
        irSettings = new PrintItemRendererSettings(defaultSettings.getBarBoxSettings().getBarRendererSettings().getItemRendererSettings());
        csSettings = new PrintIR_ChordSymbolSettings(
                defaultSettings.getBarBoxSettings().getBarRendererSettings().getItemRendererSettings().getIR_ChordSymbolSettings());
    }

    @Override
    public BarBoxSettings getBarBoxSettings()
    {
        return bbSettings;
    }

    public BarRendererSettings getBarRendererSettings()
    {
        return brSettings;
    }

    public ItemRendererSettings getItemRendererSettings()
    {
        return irSettings;
    }

    @Override
    public Color getBackgroundColor()
    {
        return defaultSettings.getBackgroundColor();
    }

    @Override
    public void setBackgroundColor(Color color)
    {
        // Nothing
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        // Do nothing
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener)
    {
        // Do nothing
    }

    // ====================================================================
    // Private classes
    // ====================================================================
    private class PrintBarBoxSettings implements BarBoxSettings
    {

        private BarBoxSettings defaultSettings;

        private PrintBarBoxSettings(BarBoxSettings defaultSettings)
        {
            this.defaultSettings = defaultSettings;
        }

        /**
         * Our own settings
         *
         * @return
         */
        @Override
        public BarRendererSettings getBarRendererSettings()
        {
            return brSettings;
        }

        @Override
        public void setBorderFont(Font font)
        {
            // Nothing
        }

        @Override
        public Font getBorderFont()
        {
            return defaultSettings.getBorderFont();
        }

        @Override
        public Color getBorderColor()
        {
            return defaultSettings.getBorderColor();
        }

        @Override
        public void setBorderColor(Color color)
        {
            // Nothing
        }

        @Override
        public Color getFocusedBorderColor()
        {
            return defaultSettings.getFocusedBorderColor();
        }

        @Override
        public void setFocusedBorderColor(Color color)
        {
            // Nothing
        }

        @Override
        public TitledBorder getTitledBorder(String str)
        {
            return defaultSettings.getTitledBorder(str);
        }

        @Override
        public TitledBorder getFocusedTitledBorder(String str)
        {
            return defaultSettings.getFocusedTitledBorder(str);
        }

        @Override
        public void setDefaultColor(Color color)
        {
            // Nothing
        }

        @Override
        public Color getDefaultColor()
        {
            // return defaultSettings.getDefaultColor();
            return Color.WHITE;
        }

        @Override
        public void setSelectedColor(Color color)
        {
            // Nothing
        }

        @Override
        public Color getSelectedColor()
        {
            return defaultSettings.getSelectedColor();
        }

        @Override
        public void setPastEndSelectedColor(Color color)
        {
            // Nothing
        }

        @Override
        public Color getPastEndSelectedColor()
        {
            return defaultSettings.getPastEndSelectedColor();
        }

        @Override
        public void setPastEndColor(Color color)
        {
            // Nothing
        }

        @Override
        public Color getPastEndColor()
        {
            return defaultSettings.getPastEndColor();
        }

        @Override
        public void setDisabledColor(Color color)
        {
            // Nothing
        }

        @Override
        public Color getDisabledColor()
        {
            return defaultSettings.getDisabledColor();
        }

        @Override
        public void setDisabledPastEndColor(Color color)
        {
            // Nothing
        }

        @Override
        public Color getDisabledPastEndColor()
        {
            return defaultSettings.getDisabledPastEndColor();
        }

        @Override
        public void setPlaybackColor(Color color)
        {
            // Nothing
        }

        @Override
        public Color getPlaybackColor()
        {
            return defaultSettings.getPlaybackColor();
        }

        @Override
        public void addPropertyChangeListener(PropertyChangeListener listener)
        {
            // Nothing
        }

        @Override
        public void removePropertyChangeListener(PropertyChangeListener listener)
        {
            // Nothing
        }

    }

    private class PrintBarRendererSettings implements BarRendererSettings
    {

        private BarRendererSettings defaultSettings;

        private PrintBarRendererSettings(BarRendererSettings defaultSettings)
        {
            this.defaultSettings = defaultSettings;
        }

        /**
         * Our own settings
         *
         * @return
         */
        @Override
        public ItemRendererSettings getItemRendererSettings()
        {
            return irSettings;
        }

        @Override
        public Border getDefaultBorder()
        {
            return defaultSettings.getDefaultBorder();
        }

        @Override
        public void addPropertyChangeListener(PropertyChangeListener listener)
        {
            // Nothing
        }

        @Override
        public void removePropertyChangeListener(PropertyChangeListener listener)
        {
            // Nothing
        }
    }

    private class PrintItemRendererSettings implements ItemRendererSettings
    {

        private ItemRendererSettings defaultSettings;

        private PrintItemRendererSettings(ItemRendererSettings defaultSettings)
        {
            this.defaultSettings = defaultSettings;
        }

        @Override
        public void addPropertyChangeListener(PropertyChangeListener listener)
        {
            // Nothing
        }

        @Override
        public void removePropertyChangeListener(PropertyChangeListener listener)
        {
            // Nothing
        }

        /**
         * Our own settings
         *
         * @return
         */
        @Override
        public IR_ChordSymbolSettings getIR_ChordSymbolSettings()
        {
            return csSettings;
        }

        @Override
        public IR_SectionSettings getIR_SectionSettings()
        {
            return defaultSettings.getIR_SectionSettings();
        }

        @Override
        public IR_TimeSignatureSettings getIR_TimeSignatureSettings()
        {
            return defaultSettings.getIR_TimeSignatureSettings();
        }

        @Override
        public void setSelectedBackgroundColor(Color color)
        {
            // Nothing
        }

        @Override
        public Color getSelectedBackgroundColor()
        {
            return defaultSettings.getSelectedBackgroundColor();
        }

        @Override
        public Border getFocusedBorder()
        {
            return defaultSettings.getFocusedBorder();
        }

        @Override
        public Border getNonFocusedBorder()
        {
            return defaultSettings.getNonFocusedBorder();
        }

    }

    private class PrintIR_ChordSymbolSettings implements IR_ChordSymbolSettings
    {

        private IR_ChordSymbolSettings defaultSettings;

        public PrintIR_ChordSymbolSettings(IR_ChordSymbolSettings defSettings)
        {
            defaultSettings = defSettings;
        }

        @Override
        public void setFont(Font font)
        {
            // Nothing
        }

        @Override
        public Font getFont()
        {
            return defaultSettings.getFont();
        }

        @Override
        public void setColor(Color color)
        {
            // Nothing
        }

        @Override
        public Color getColor()
        {
            return defaultSettings.getColor();
        }

        @Override
        public void setSubstituteFontColor(Color color)
        {
            // 
        }

        @Override
        public Color getSubstituteFontColor()
        {
            return defaultSettings.getSubstituteFontColor();
        }

        @Override
        public Font getMusicFont()
        {
            return defaultSettings.getMusicFont();
        }

        @Override
        public char getSharpCharInMusicFont()
        {
            return defaultSettings.getSharpCharInMusicFont();
        }

        @Override
        public char getFlatCharInMusicFont()
        {
            return defaultSettings.getFlatCharInMusicFont();
        }

        @Override
        public void addPropertyChangeListener(PropertyChangeListener listener)
        {
            // Nothing
        }

        @Override
        public void removePropertyChangeListener(PropertyChangeListener listener)
        {
            // Nothing
        }


    }
}

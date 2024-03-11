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
package org.jjazz.uiutilities.api;

import java.awt.Color;
import java.awt.Font;
import java.util.List;

/**
 * A special interface for objects managing fonts and colors settings.
 * <p>
 */
public interface FontColorUserSettingsProvider
{

    /**
     * Get all the FCSettings of this provider.
     *
     * @return
     */
    List<FCSetting> getFCSettings();

    /**
     * One Font and Color setting.
     */
    public interface FCSetting
    {

        String getId();

        String getDisplayName();

        Color getColor();

        void setColor(Color c);

        Font getFont();

        void setFont(Font f);
    }

    public class FCSettingAdapter implements FCSetting
    {

        private String id;
        private String displayName;

        /**
         * Create a FCSetting with specified name and id, font and color are null.
         *
         * @param id Id of the setting
         * @param displayName Display displayName of the setting
         */
        public FCSettingAdapter(String id, String displayName)
        {
            if (id == null || id.isEmpty() || displayName == null || displayName.isEmpty())
            {
                throw new IllegalArgumentException("id=" + id + " name=" + displayName);   
            }
            this.id = id;
            this.displayName = displayName;
        }

        @Override
        public String getId()
        {
            return id;
        }

        @Override
        public String getDisplayName()
        {
            return displayName;
        }

        /**
         * Default implementation returns null.
         *
         * @return
         */
        @Override
        public Color getColor()
        {
            return null;
        }

        /**
         * Default implementation does nothing.
         *
         * @param c
         */
        @Override
        public void setColor(Color c)
        {
            // Nothing
        }

        /**
         * Default implementation returns null.
         *
         * @return
         */
        @Override
        public Font getFont()
        {
            return null;
        }

        /**
         * Default implementation does nothing.
         *
         * @param f
         */
        @Override
        public void setFont(Font f)
        {
            // Nothing
        }

        @Override
        public String toString()
        {
            return id + "-" + displayName;
        }
    }

}

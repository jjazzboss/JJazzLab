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
package org.jjazz.phrasetransform.api.ui;

import java.awt.Component;
import java.awt.image.BufferedImage;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import org.jjazz.phrasetransform.api.PhraseTransform;


/**
 * A list renderer for PhraseTransform items.
 */
public class PhraseTransformListCellRenderer extends DefaultListCellRenderer
{

    private final boolean useCategory;

    public PhraseTransformListCellRenderer(boolean useCategory)
    {
        this.useCategory = useCategory;
    }

    @Override
    @SuppressWarnings(value = "rawtypes")
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
    {
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        PhraseTransform pt = (PhraseTransform) value;
        String text = pt.getInfo().getName();
        if (useCategory)
        {
            text = pt.getInfo().getCategory().getDisplayName() + "/" + text;
        }
        label.setText(text);
        label.setToolTipText(pt.getInfo().getDescription());
        Icon icon = pt.getInfo().getIcon();
        if (icon == null)
        {
            // Create a dummy icon for alignment purpose
            BufferedImage bufferedImage = new BufferedImage(PhraseTransform.ICON_SIZE.width, PhraseTransform.ICON_SIZE.height, BufferedImage.TYPE_INT_ARGB);
            icon = new ImageIcon(bufferedImage);
        }
        label.setIcon(icon);
        return label;
    }

}

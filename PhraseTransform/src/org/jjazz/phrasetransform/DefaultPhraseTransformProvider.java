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
package org.jjazz.phrasetransform;

import java.util.ArrayList;
import java.util.List;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.jjazz.phrase.api.CyclicPositions;
import org.openide.util.lookup.ServiceProvider;
import org.jjazz.phrasetransform.spi.PhraseTransformProvider;
import org.jjazz.phrasetransform.api.PhraseTransform;
import org.netbeans.api.annotations.common.StaticResource;

/**
 * Default provider of PhraseTransforms
 */
@ServiceProvider(service = PhraseTransformProvider.class)
public class DefaultPhraseTransformProvider implements PhraseTransformProvider
{

    @StaticResource(relative = true)
    private static final String ACCENT1_ICON_PATH = "resources/Accent8-2-Transformer-48x24.png";
    private static final Icon ACCENT1_ICON = new ImageIcon(OpenHiHatTransform.class.getResource(ACCENT1_ICON_PATH));
    @StaticResource(relative = true)
    private static final String ACCENT2_ICON_PATH = "resources/Accent16-1-Transformer-48x24.png";
    private static final Icon ACCENT2_ICON = new ImageIcon(OpenHiHatTransform.class.getResource(ACCENT2_ICON_PATH));
    @StaticResource(relative = true)
    private static final String ACCENT3_ICON_PATH = "resources/Accent16-2-Transformer-48x24.png";
    private static final Icon ACCENT3_ICON = new ImageIcon(OpenHiHatTransform.class.getResource(ACCENT3_ICON_PATH));
    @StaticResource(relative = true)
    private static final String ACCENT4_ICON_PATH = "resources/Accent16-3-Transformer-48x24.png";
    private static final Icon ACCENT4_ICON = new ImageIcon(OpenHiHatTransform.class.getResource(ACCENT4_ICON_PATH));


    List<PhraseTransform> transforms = new ArrayList<>();

    public DefaultPhraseTransformProvider()
    {
        transforms.add(new OpenHiHatTransform());
        transforms.add(new SwingTransform());
        
        
        transforms.add(new AddAccentTransform("Accent1", ACCENT1_ICON, new CyclicPositions(0.5f, 0f, 1f)));
        transforms.add(new AddAccentTransform("Accent2", ACCENT2_ICON, 0f));
        transforms.add(new AddAccentTransform("Accent3", ACCENT3_ICON, 0.25f));
        transforms.add(new AddAccentTransform("Accent4", ACCENT4_ICON, 0.75f));
    }

    @Override
    public List<PhraseTransform> getTransforms()
    {
        return new ArrayList<>(transforms);
    }

}

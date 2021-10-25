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
import org.jjazz.midi.api.DrumKit;
import org.jjazz.phrase.api.CyclicPositions;
import org.openide.util.lookup.ServiceProvider;
import org.jjazz.phrasetransform.spi.PhraseTransformProvider;
import org.jjazz.phrasetransform.api.PhraseTransform;
import org.jjazz.phrasetransform.api.PhraseTransformCategory;
import org.jjazz.util.api.ResUtil;
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


        transforms.add(getAccent1());
        transforms.add(getAccent2());
        transforms.add(getAccent3());
        transforms.add(getAccent4());
    }

    @Override
    public List<PhraseTransform> getTransforms()
    {
        return new ArrayList<>(transforms);
    }

    // =================================================================================
    // Private methods
    // =================================================================================

    private DrumsAccentsTransform getAccent1()
    {
        PhraseTransform.Info info = new PhraseTransform.Info("Accent1_16Id",
                ResUtil.getString(getClass(), "Accent1_16_name"),
                ResUtil.getString(getClass(), "Accent1_16_desc"),
                PhraseTransformCategory.DRUMS,
                ACCENT1_ICON
        );

        DrumsAccentsTransform t = new DrumsAccentsTransform(info,
                new CyclicPositions(0, 0, 1),
                DrumKit.Subset.HI_HAT, DrumKit.Subset.CYMBAL
        );

        return t;
    }

    private DrumsAccentsTransform getAccent2()
    {
        PhraseTransform.Info info = new PhraseTransform.Info("Accent2_16Id",
                ResUtil.getString(getClass(), "Accent2_16_name"),
                ResUtil.getString(getClass(), "Accent2_16_desc"),
                PhraseTransformCategory.DRUMS,
                ACCENT2_ICON
        );

        DrumsAccentsTransform t = new DrumsAccentsTransform(info,
                new CyclicPositions(0.25f, 0, 1),
                DrumKit.Subset.HI_HAT, DrumKit.Subset.CYMBAL
        );

        return t;
    }

    private DrumsAccentsTransform getAccent3()
    {
        PhraseTransform.Info info = new PhraseTransform.Info("Accent3_16Id",
                ResUtil.getString(getClass(), "Accent3_16_name"),
                ResUtil.getString(getClass(), "Accent3_16_desc"),
                PhraseTransformCategory.DRUMS,
                ACCENT3_ICON
        );

        DrumsAccentsTransform t = new DrumsAccentsTransform(info,
                new CyclicPositions(0.5f, 0, 1),
                DrumKit.Subset.HI_HAT, DrumKit.Subset.CYMBAL
        );

        return t;
    }

    private DrumsAccentsTransform getAccent4()
    {
        PhraseTransform.Info info = new PhraseTransform.Info("Accent4_16Id",
                ResUtil.getString(getClass(), "Accent4_16_name"),
                ResUtil.getString(getClass(), "Accent4_16_desc"),
                PhraseTransformCategory.DRUMS,
                ACCENT4_ICON
        );

        DrumsAccentsTransform t = new DrumsAccentsTransform(info,
                new CyclicPositions(0.75f, 0, 1),
                DrumKit.Subset.HI_HAT, DrumKit.Subset.CYMBAL
        );

        return t;
    }

}

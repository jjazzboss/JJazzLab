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

import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.jjazz.midi.api.Instrument;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.phrasetransform.api.PhraseTransformCategory;
import org.jjazz.phrasetransform.api.PhraseTransform;
import org.netbeans.api.annotations.common.StaticResource;

/**
 *
 * @author Jerome
 */
public class OpenHiHatTransform implements PhraseTransform
{

    @StaticResource(relative = true)
    private static final String ICON_PATH = "resources/OpenHiHatTransformIcon.gif";
    private static final Icon ICON = new ImageIcon(OpenHiHatTransform.class.getResource(ICON_PATH));

    @Override
    public SizedPhrase transform(SizedPhrase inPhrase, Instrument ins)
    {
        SizedPhrase res = new SizedPhrase(inPhrase);

        for (var ne : res)
        {
            var newNe = new NoteEvent(ne, ne.getPitch() + 3);
            res.addOrdered(newNe);
        }

        return res;
    }

    @Override
    public int getFitScore(SizedPhrase inPhrase, Instrument ins)
    {
        return 90;
    }

    @Override
    public String getName()
    {
        return "hi-hat 16th";
    }

    @Override
    public int hashCode()
    {
        return PhraseTransform.hashCode(this);
    }

    @Override
    public boolean equals(Object obj)
    {
        return PhraseTransform.equals(this, obj);
    }

    @Override
    public String getUniqueId()
    {
        return "OpenHiHatTransformID";
    }

    @Override
    public PhraseTransformCategory getCategory()
    {
        return PhraseTransformCategory.PERCUSSION;
    }

    @Override
    public String getDescription()
    {
        return "Make hi-hat 16th notes";
    }

    @Override
    public Icon getIcon()
    {
        return ICON;
    }
}

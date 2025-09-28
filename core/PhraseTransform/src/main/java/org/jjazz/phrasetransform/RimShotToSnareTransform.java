
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
package org.jjazz.phrasetransform;

import java.util.Properties;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.midi.api.DrumKit.KeyMap;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.phrasetransform.api.PhraseTransformCategory;
import org.jjazz.phrasetransform.api.PhraseTransform;
import org.jjazz.phrasetransform.api.PhraseTransforms;
import org.jjazz.phrasetransform.api.PtProperties;
import org.jjazz.songcontext.api.SongPartContext;
import org.jjazz.utilities.api.ResUtil;
import org.netbeans.api.annotations.common.StaticResource;

/**
 *
 * Snare to rimshot.
 */
public class RimShotToSnareTransform implements PhraseTransform
{

    @StaticResource(relative = true)
    private static final String ICON_PATH = "resources/Rimshot2Snare-Transformer-48x24.png";
    private static final Icon ICON = new ImageIcon(RimShotToSnareTransform.class.getResource(ICON_PATH));
    private final Info info;
    private final PtProperties properties;

    public RimShotToSnareTransform()
    {
        info = new Info("Rimshot2SnareId",
                ResUtil.getString(getClass(), "Rimshot2SnareTransformName"),
                ResUtil.getString(getClass(), "Rimshot2SnareTransformDesc"),
                PhraseTransformCategory.DRUMS,
                ICON);

        properties = new PtProperties(new Properties());
    }

    @Override
    public Info getInfo()
    {
        return info;
    }

    @Override
    public SizedPhrase transform(SizedPhrase inPhrase, SongPartContext context)
    {
        SizedPhrase res = new SizedPhrase(inPhrase.getChannel(), inPhrase.getNotesBeatRange(), inPhrase.getTimeSignature(), inPhrase.isDrums());


        KeyMap keyMap = PhraseTransforms.getDrumKit(inPhrase, context).getKeyMap();
        var srcPitches = keyMap.getKeys(DrumKit.Subset.SNARE_RIMSHOT, DrumKit.Subset.SNARE_HANDCLAP);
        if (srcPitches.isEmpty())
        {
            return res;
        }
        var destPitches = keyMap.getKeys(DrumKit.Subset.SNARE_DEFAULT);
        if (destPitches.isEmpty())
        {
            return res;
        }
        int destPitch = destPitches.get(0);
        

        for (var ne : inPhrase)
        {
            int pitch = ne.getPitch();
            if (srcPitches.contains(pitch))
            {
                pitch = destPitch;
            }
            var newNe = ne.setPitch(pitch, true);
            res.add(newNe);
        }

        return res;
    }

    @Override
    public int getFitScore(SizedPhrase inPhrase, SongPartContext context)
    {
        return PhraseTransforms.getRhythmVoice(inPhrase, context).isDrums() ? 100 : 0;
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
    public RimShotToSnareTransform getCopy()
    {
        return this;
    }

    @Override
    public PtProperties getProperties()
    {
        return properties;
    }
}

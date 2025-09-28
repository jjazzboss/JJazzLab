
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
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.phrasetransform.api.PhraseTransformCategory;
import org.jjazz.phrasetransform.api.PhraseTransform;
import org.jjazz.phrasetransform.api.PtProperties;
import org.jjazz.quantizer.api.Quantization;
import org.jjazz.quantizer.api.Quantizer;
import org.jjazz.songcontext.api.SongPartContext;
import org.jjazz.utilities.api.FloatRange;
import org.jjazz.utilities.api.ResUtil;
import org.netbeans.api.annotations.common.StaticResource;

/**
 *
 * @author Jerome
 */
public class SwingTransform implements PhraseTransform
{

    /**
     * A float value between 0 and 1, 0.3 being the standard value for swing.
     */
    public static final String PROP_AMOUNT = "amount";
    /**
     * 0.5f means 8th note swing, 0.25f means 16th note swing.
     */
    public static final String PROP_SWING_UNIT = "swing unit";

    @StaticResource(relative = true)
    private static final String ICON_PATH = "resources/SwingTransformer-48x24.png";
    private static final Icon ICON = new ImageIcon(SwingTransform.class.getResource(ICON_PATH));
    private final Info info;
    private float swingThreshold;

    private PtProperties properties;

    public SwingTransform()
    {
        info = new Info("SwingId",
                "Swing",
                ResUtil.getString(getClass(), "SwingTransformDesc"),
                PhraseTransformCategory.DRUMS,
                ICON);


        Properties defaults = new Properties();
        defaults.setProperty(PROP_AMOUNT, String.valueOf(1 / 3f));
        defaults.setProperty(PROP_SWING_UNIT, String.valueOf(0.5f));
        properties = new PtProperties(defaults);
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


        // Prepare data
        FloatRange fr = inPhrase.getNotesBeatRange();
        float swingUnit = getSwingUnit();
        Quantization q = swingUnit == 0.5f ? Quantization.HALF_BEAT : Quantization.ONE_QUARTER_BEAT;
        float shift = swingUnit * getSwingAmount();


        // Analyze each note
        for (var ne : inPhrase)
        {
            float newDur = ne.getDurationInBeats();
            float newPos = Quantizer.getQuantized(q, ne.getPositionInBeats());
            if (newPos >= fr.to)
            {
                continue;
            }

            boolean makeItSwing = Math.round(newPos / swingUnit) % 2 == 1;


            if (makeItSwing)
            {
                newPos += shift;
                if (!fr.contains(newPos + newDur, false))
                {
                    newDur = fr.to - newPos - 0.1f;
                }
            }
            var newNe = ne.setAll(-1, newDur, -1, newPos, null, true);
            res.add(newNe);
        }

        return res;
    }

    /**
     * 0.5f means 8th note swing, 0.25f means 16th note swing.
     *
     * @return 0.5 or 0.25.
     */
    public float getSwingUnit()
    {
        return properties.getPropertyAsFloat(PROP_SWING_UNIT);
    }

    /**
     * A value between 0f and 1f.
     *
     * @return
     */
    public float getSwingAmount()
    {
        return properties.getPropertyAsFloat(PROP_AMOUNT);
    }


    @Override
    public int getFitScore(SizedPhrase inPhrase, SongPartContext context)
    {
        return 100;
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
    public PtProperties getProperties()
    {
        return properties;
    }

    @Override
    public SwingTransform getCopy()
    {
        SwingTransform res = new SwingTransform();
        res.properties = properties.getCopy();
        return res;
    }

}

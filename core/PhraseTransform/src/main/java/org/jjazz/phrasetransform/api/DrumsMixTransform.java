
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
package org.jjazz.phrasetransform.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.midi.api.DrumKit.Subset;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.songcontext.api.SongPartContext;

/**
 * Change velocity of drums notes.
 * <p>
 */
public class DrumsMixTransform implements PhraseTransform
{

    public static final String PROP_BD_OFFSET = Subset.BASS.toString();
    public static final String PROP_SD_OFFSET = Subset.SNARE.toString();
    public static final String PROP_HH_OFFSET = Subset.HI_HAT.toString();
    public static final String PROP_TOMS_OFFSET = Subset.TOM.toString();
    public static final String PROP_CRASH_OFFSET = Subset.CRASH.toString();
    public static final String PROP_CYMBAL_OFFSET = Subset.CYMBAL.toString();
    public static final String PROP_PERC_OFFSET = Subset.PERCUSSION.toString();

    private final PtProperties properties;
    private final Info info;
    private static final Logger LOGGER = Logger.getLogger(DrumsMixTransform.class.getSimpleName());


    /**
     * Create a DrumsMixTransform.
     * <p>
     * <p>
     */
    public DrumsMixTransform()
    {
        info = new PhraseTransform.Info("DrumsMixId" + PhraseTransform.HIDDEN_ID_TOKEN,
                "Drums Mix",
                "Change the velocity of individual drums sounds",
                PhraseTransformCategory.DRUMS,
                null
        );


        Properties defaults = new Properties();
        defaults.setProperty(PROP_BD_OFFSET, Integer.toString(0));
        defaults.setProperty(PROP_SD_OFFSET, Integer.toString(0));
        defaults.setProperty(PROP_HH_OFFSET, Integer.toString(0));
        defaults.setProperty(PROP_TOMS_OFFSET, Integer.toString(0));
        defaults.setProperty(PROP_CRASH_OFFSET, Integer.toString(0));
        defaults.setProperty(PROP_CYMBAL_OFFSET, Integer.toString(0));
        defaults.setProperty(PROP_PERC_OFFSET, Integer.toString(0));
        properties = new PtProperties(defaults);

    }

    @Override
    public Info getInfo()
    {
        return info;
    }

    public int getBassDrumOffset()
    {
        return properties.getPropertyAsInteger(PROP_BD_OFFSET);
    }

    public void setBassDrumOffset(int offset)
    {
        properties.setProperty(PROP_BD_OFFSET, String.valueOf(offset));
    }

    public int getSnareOffset()
    {
        return properties.getPropertyAsInteger(PROP_SD_OFFSET);
    }

    public void setSnareOffset(int offset)
    {
        properties.setProperty(PROP_SD_OFFSET, String.valueOf(offset));
    }

    public int getHiHatOffset()
    {
        return properties.getPropertyAsInteger(PROP_HH_OFFSET);
    }

    public void setHiHatOffset(int offset)
    {
        properties.setProperty(PROP_HH_OFFSET, String.valueOf(offset));
    }

    public int getTomsOffset()
    {
        return properties.getPropertyAsInteger(PROP_TOMS_OFFSET);
    }

    public void setTomsOffset(int offset)
    {
        properties.setProperty(PROP_TOMS_OFFSET, String.valueOf(offset));
    }

    public int getCrashOffset()
    {
        return properties.getPropertyAsInteger(PROP_CRASH_OFFSET);
    }

    public void setCrashOffset(int offset)
    {
        properties.setProperty(PROP_CRASH_OFFSET, String.valueOf(offset));
    }

    public int getCymbalsOffset()
    {
        return properties.getPropertyAsInteger(PROP_CYMBAL_OFFSET);
    }

    public void setCymbalsOffset(int offset)
    {
        properties.setProperty(PROP_CYMBAL_OFFSET, String.valueOf(offset));
    }

    public int getPercOffset()
    {
        return properties.getPropertyAsInteger(PROP_PERC_OFFSET);
    }

    public void setPercOffset(int offset)
    {
        properties.setProperty(PROP_PERC_OFFSET, String.valueOf(offset));
    }

    @Override
    public SizedPhrase transform(SizedPhrase inPhrase, SongPartContext context)
    {
        SizedPhrase res = new SizedPhrase(inPhrase.getChannel(), inPhrase.getNotesBeatRange(), inPhrase.getTimeSignature(), inPhrase.isDrums());


        DrumKit kit = PhraseTransforms.getDrumKit(inPhrase, context);
        var mapPitchSubset = kit.getSubsetPitches(Subset.BASS, Subset.SNARE, Subset.HI_HAT, Subset.CYMBAL, Subset.CRASH, Subset.TOM, Subset.PERCUSSION);


        for (var ne : inPhrase)
        {
            NoteEvent nne = ne;
            Subset subset = mapPitchSubset.get(ne.getPitch());

            if (subset != null)
            {
                int offset = properties.getPropertyAsInteger(subset.toString());
                if (offset != 0)
                {
                    int velocity = MidiConst.clamp(ne.getVelocity() + offset);
                    nne = ne.setVelocity(velocity, true);
                }
            }

            res.add(nne);           // addOrdered() not needed here
        }


        return res;
    }

    @Override
    public int getFitScore(SizedPhrase inPhrase, SongPartContext context)
    {
        return PhraseTransforms.getRhythmVoice(inPhrase, context).isDrums() ? 100 : 0;
    }

    /**
     * Get a string representing the property values in a user-oriented way.
     *
     * @return Can be an empty string
     */
    public String getPropertiesDisplayString()
    {
        List<String> strs = new ArrayList<>();
        if (getBassDrumOffset() != 0)
        {
            strs.add("BD:" + getPlusMinusString(getBassDrumOffset()));
        }
        if (getSnareOffset() != 0)
        {
            strs.add("SD:" + getPlusMinusString(getSnareOffset()));
        }
        if (getHiHatOffset() != 0)
        {
            strs.add("HH:" + getPlusMinusString(getHiHatOffset()));
        }
        if (getTomsOffset() != 0)
        {
            strs.add("TO:" + getPlusMinusString(getTomsOffset()));
        }
        if (getCymbalsOffset() != 0)
        {
            strs.add("CY:" + getPlusMinusString(getCymbalsOffset()));
        }
        if (getCrashOffset() != 0)
        {
            strs.add("CR:" + getPlusMinusString(getCrashOffset()));
        }
        if (getPercOffset() != 0)
        {
            strs.add("OT:" + getPlusMinusString(getPercOffset()));
        }
        return strs.stream().collect(Collectors.joining(", "));
    }

    @Override
    public PtProperties getProperties()
    {
        return properties;
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
    public DrumsMixTransform getCopy()
    {
        var res = new DrumsMixTransform();
        res.properties.putAll(properties);
        return res;
    }

    @Override
    public String toString()
    {
        return "DrumsMixTransform:" + getPropertiesDisplayString();
    }


    // ==================================================================================================
    // Private methods
    // ==================================================================================================
    private String getPlusMinusString(int v)
    {
        return v > 0 ? "+" + v : String.valueOf(v);
    }
}

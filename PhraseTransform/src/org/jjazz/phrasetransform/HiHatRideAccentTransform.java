
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

import static com.google.common.base.Preconditions.checkArgument;
import java.util.Arrays;
import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.Icon;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.midi.api.DrumKit.KeyMap;
import org.jjazz.midi.api.MidiUtilities;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.phrasetransform.api.PhraseTransformCategory;
import org.jjazz.phrasetransform.api.PhraseTransform;
import org.jjazz.phrasetransform.api.PhraseTransforms;
import org.jjazz.phrasetransform.api.PtProperties;
import org.jjazz.songcontext.api.SongPartContext;
import org.jjazz.util.api.ResUtil;

/**
 * Add accents to a hi-hat or ride phrase.
 * <p>
 * <p>
 */
public class HiHatRideAccentTransform implements PhraseTransform
{
    
    public static final float POS_WINDOW = 0.1f;
    public static final int ACCENT_OFFSET = 50;
    private final PtProperties properties;
    private final float accentsPos[];
    private final int accentOffset;
    private final Icon icon;
    private final String name;
    private static final Logger LOGGER = Logger.getLogger(HiHatRideAccentTransform.class.getSimpleName());

    /**
     * Create the transform.
     * <p>
     * Examples:<br>
     * - accentsPos[0]=0.5 means that an accent should be added on the 2nd eighth note of each beat.<br>
     * - accentsPos[0]=0.5 and accentsPos[0.75] means that 2 accents should be added on the 2nd eighth note and last sixteenth
     * note of each beat.<br>
     *
     * @param accentsPos An array of positions where the accents should be within a single beat [0-0.99]
     * @param accentOffset The velocity offset of the accent.
     * @param icon
     * @IllegalArgumentException If accentsPos contains a value which is not in the accepted range [0;0.99]
     */
    public HiHatRideAccentTransform(String name, Icon icon, int accentOffset, float... accentsPos)
    {
        checkArgument(name != null && !name.isBlank(), "name=%s" + name);

        // Check arguments
        for (float accentPos : accentsPos)
        {
            if (accentPos < 0 || accentPos > 0.99f)
            {
                throw new IllegalArgumentException("accentsPos=" + Arrays.asList(accentsPos) + " accentOffset=" + accentOffset);
            }
        }
        
        this.name = name;
        this.icon = icon;
        properties = new PtProperties(new Properties());
        this.accentsPos = accentsPos;
        this.accentOffset = accentOffset;
    }


    /**
     *
     * @param inPhrase
     * @param context
     * @return
     */
    @Override
    public SizedPhrase transform(SizedPhrase inPhrase, SongPartContext context)
    {
        SizedPhrase res = new SizedPhrase(inPhrase.getChannel(), inPhrase.getBeatRange(), inPhrase.getTimeSignature());
        
        
        KeyMap keyMap = PhraseTransforms.getInstrument(inPhrase, context).getDrumKit().getKeyMap();
        var pitches = keyMap.getKeys(DrumKit.Subset.HI_HAT, DrumKit.Subset.CYMBAL);
        var mapPitchNbNotes = inPhrase.countPitches(ne -> pitches.contains(ne.getPitch()));
        
        
        if (mapPitchNbNotes.isEmpty())
        {
            // No relevant note, nothing to do
            res.add(inPhrase);
            return res;
        }


        // Find the most used pitch
        Integer pitch = Collections.max(mapPitchNbNotes.entrySet(), Map.Entry.comparingByValue()).getKey();


        // Get stats on all notes with the desired pitch
        DoubleSummaryStatistics stats = inPhrase.stream()
                .filter(ne -> ne.getPitch() == pitch)
                .collect(Collectors.summarizingDouble(NoteEvent::getVelocity));
        int velocityThreshold = (int) Math.round(stats.getAverage() + accentOffset);


        // Add the accents when note timing matches and if it's not already accented
        for (var ne : inPhrase)
        {
            NoteEvent nne = ne;
            if (ne.getPitch() == pitch)
            {
                long intPos = (long) ne.getPositionInBeats();        // Take the int part
                for (float accentPos : accentsPos)
                {
                    if (ne.isNear(intPos + accentPos, POS_WINDOW))
                    {
                        if (ne.getVelocity() < velocityThreshold)
                        {
                            int v = MidiUtilities.limit(ne.getVelocity() + accentOffset);
                            nne = new NoteEvent(ne, ne.getPitch(), ne.getDurationInBeats(), v);
                            LOGGER.severe("Adding accent to ne=" + ne + " => nne=" + nne);
                        }
                        break;
                    }
                }
            }
            
            res.add(nne);       // No need to use addOrdered

        }
        
        
        return res;
    }
    
    @Override
    public int getFitScore(SizedPhrase inPhrase, SongPartContext context)
    {
        DrumKit kit = PhraseTransforms.getRhythmVoice(inPhrase, context).getDrumKit();
        int res = 0;
        if (kit != null)
        {
            KeyMap keyMap = PhraseTransforms.getInstrument(inPhrase, context).getDrumKit().getKeyMap();
            var srcPitches = keyMap.getKeys(DrumKit.Subset.HI_HAT_CLOSED);
            var destPitches = keyMap.getKeys(DrumKit.Subset.HI_HAT_OPEN);
            if (!srcPitches.isEmpty() && !destPitches.isEmpty())
            {
                res = 100;
            }
        }
        return res;
    }
    
    
    @Override
    public String getName()
    {
        return name;
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
    public HiHatRideAccentTransform getCopy()
    {
        return this;
    }
    
    @Override
    public String getUniqueId()
    {
        return getName() + "Id";
    }
    
    @Override
    public PhraseTransformCategory getCategory()
    {
        return PhraseTransformCategory.DRUMS;
    }
    
    @Override
    public String getDescription()
    {
        return ResUtil.getString(getClass(), "HiHatRideAccentTransformDesc");
    }
    
    @Override
    public Icon getIcon()
    {
        return icon;
    }
    
    @Override
    public PtProperties getProperties()
    {
        return properties;
    }
}

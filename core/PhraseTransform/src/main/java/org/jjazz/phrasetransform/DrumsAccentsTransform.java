
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

import static com.google.common.base.Preconditions.checkNotNull;
import java.awt.Component;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.phrase.api.CyclicPositions;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.phrasetransform.api.PhraseTransform;
import org.jjazz.phrasetransform.api.PhraseTransforms;
import org.jjazz.phrasetransform.api.PtProperties;
import org.jjazz.songcontext.api.SongPartContext;
import org.jjazz.uiutilities.api.UIUtilities;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.phrasetransform.spi.PtPropertyEditorFactory;

/**
 * Change velocity of specific drums notes which match cyclic positions.
 * <p>
 */
public class DrumsAccentsTransform implements PhraseTransform
{

    public static final String PROP_VELOCITY_OFFSET = "VelocityOffset";
    public static final float POS_WINDOW = 0.1f;
    private final PtProperties properties;
    private final Info info;
    private final DrumKit.Subset[] subsets;
    private final CyclicPositions cyclicPositions;
    private static final Logger LOGGER = Logger.getLogger(DrumsAccentsTransform.class.getSimpleName());


    /**
     * Create a DrumsAccentsTransform.
     * <p>
     * When transformed, notes from subsets which match cyclicPositions will see their velocity changed by
     * getAccentVelocityOffset().
     *
     * @param info
     * @param cyclicPositions
     * @param subsets
     */
    public DrumsAccentsTransform(PhraseTransform.Info info, CyclicPositions cyclicPositions, DrumKit.Subset... subsets)
    {
        checkNotNull(info);
        checkNotNull(cyclicPositions);

        Properties defaults = new Properties();
        defaults.setProperty(PROP_VELOCITY_OFFSET, Integer.toString(30));
        properties = new PtProperties(defaults);

        this.info = info;
        this.subsets = subsets;
        this.cyclicPositions = cyclicPositions;

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
        int accentOffset = getAccentVelocityOffset();


        // Get the pitch list
        DrumKit.KeyMap keyMap = PhraseTransforms.getDrumKit(inPhrase, context).getKeyMap();
        Set<Integer> validPitches = new HashSet<>();
        for (var subset : subsets)
        {
            validPitches.addAll(keyMap.getKeys(subset));
        }


        // Add the accents 
        for (var ne : inPhrase)
        {
            NoteEvent nne = ne;
            int pitch = ne.getPitch();
            float pos = ne.getPositionInBeats();

            if (validPitches.contains(pitch) && cyclicPositions.matches(pos, POS_WINDOW))
            {
                int velocity = MidiConst.clamp(ne.getVelocity() + accentOffset);
                nne = new NoteEvent(pitch, ne.getDurationInBeats(), velocity, pos);
            }

            res.add(nne);       // No need to use addOrdered()

        }


        return res;
    }

    @Override
    public int getFitScore(SizedPhrase inPhrase, SongPartContext context)
    {
        return PhraseTransforms.getRhythmVoice(inPhrase, context).isDrums() ? 100 : 0;
    }

    public int getAccentVelocityOffset()
    {
        return properties.getPropertyAsInteger(PROP_VELOCITY_OFFSET);
    }

    public void setAccentVelocityOffset(int offset)
    {
        properties.setProperty(PROP_VELOCITY_OFFSET, Integer.toString(offset));
    }

    @Override
    public PtProperties getProperties()
    {
        return properties;
    }

    @Override
    public boolean hasUserSettings()
    {
        return true;
    }

    @Override
    public void showUserSettingsDialog(Component anchor)
    {
        String text = ResUtil.getString(getClass(), "AccentStrength");
        var dlg = PtPropertyEditorFactory.getDefault().getSinglePropertyEditor(properties, getInfo().getName(), PROP_VELOCITY_OFFSET, text, -63, 64, true);
        UIUtilities.setDialogLocationRelativeTo(dlg, anchor, 0, 0.5, 0.5);
        dlg.setVisible(true);
        dlg.dispose();
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
    public DrumsAccentsTransform getCopy()
    {
        var res = new DrumsAccentsTransform(info, cyclicPositions, subsets);
        res.properties.putAll(properties);
        return res;
    }


}

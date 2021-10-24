
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
import static com.google.common.base.Preconditions.checkNotNull;
import java.awt.Component;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.JDialog;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.midi.api.DrumKit.KeyMap;
import org.jjazz.midi.api.MidiUtilities;
import org.jjazz.phrase.api.CyclicPositions;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.phrasetransform.api.PhraseTransformCategory;
import org.jjazz.phrasetransform.api.PhraseTransform;
import org.jjazz.phrasetransform.api.PhraseTransforms;
import org.jjazz.phrasetransform.api.PtProperties;
import org.jjazz.songcontext.api.SongPartContext;
import org.jjazz.ui.utilities.api.Utilities;
import org.jjazz.util.api.ResUtil;
import org.openide.windows.WindowManager;

/**
 * Add accents to notes which match specific pitches and cyclic positions.
 * <p>
 */
public class AddAccentTransform implements PhraseTransform
{

    public static final String PROP_STRENGTH = "Accent strength";
    public static final float POS_WINDOW = 0.1f;
    private final PtProperties properties;
    private final List<Integer> pitches;
    private final Info info;
    private static final Logger LOGGER = Logger.getLogger(AddAccentTransform.class.getSimpleName());
    private final CyclicPositions cyclicPositions;

    /**
     * Create the transform.
     * <p>
     * Examples:<br>
     * - accentsPos[0]=0.5 means that an accent should be added on the 2nd eighth note of each beat.<br>
     * - accentsPos[0]=0.5 and accentsPos[2.75] means the accent above plus an accent on the last sixteenth of the 3rd beat.
     *
     * @param accentPositions
     * @param pitches The pitches to be accented.
     * @param icon
     */
    public AddAccentTransform(String name, Icon icon, List<Integer> pitches, CyclicPositions cyclicPositions)
    {
        checkArgument(name != null && !name.isBlank(), "name=%s" + name);
        checkNotNull(pitches);


        info = new Info("AddAccentId",
                ResUtil.getString(getClass(), "AddAccentTransformName"),
                ResUtil.getString(getClass(), "AddAccentTransformDesc"),
                PhraseTransformCategory.DRUMS,
                icon);


        Properties defaults = new Properties();
        defaults.setProperty(PROP_STRENGTH, Integer.toString(30));
        properties = new PtProperties(defaults);


        this.pitches = pitches;
        this.cyclicPositions = cyclicPositions;

    }

    @Override
    public Info getInfo()
    {
        return info;
    }

    /**
     * Change the velocity of notes who match this instance pitches/accentsPos.
     *
     * @param inPhrase
     * @param context
     * @return
     */
    @Override
    public SizedPhrase transform(SizedPhrase inPhrase, SongPartContext context)
    {
        SizedPhrase res = new SizedPhrase(inPhrase.getChannel(), inPhrase.getBeatRange(), inPhrase.getTimeSignature());
        int accentOffset = getAccentVelocityOffset();


        // Add the accents for notes which have the right pitch and timing
        for (var ne : inPhrase)
        {
            NoteEvent nne = ne;
            int pitch = ne.getPitch();
            float pos = ne.getPositionInBeats();

            if (pitches.contains(pitch) && cyclicPositions.matches(pos, POS_WINDOW))
            {
                int velocity = MidiUtilities.limit(ne.getVelocity() + accentOffset);
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
        return properties.getPropertyAsInteger(PROP_STRENGTH);
    }

    public void setAccentVelocityOffset(int offset)
    {
        properties.setProperty(PROP_STRENGTH, Integer.toString(offset));
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
        JDialog dlg = new JDialog(WindowManager.getDefault().getMainWindow(), true);
        // dlg.setUndecorated(true);
        var panel = new HiHatRideAccentSettingsPanel();
        panel.preset(properties);
        dlg.add(panel);
        dlg.pack();
        Utilities.setDialogLocationRelativeTo(dlg, anchor, 10, 0.5, 1);
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
    public AddAccentTransform getCopy()
    {
        var res = new AddAccentTransform(getInfo().getName(), getInfo().getIcon(), pitches, cyclicPositions);
        res.properties.putAll(properties);
        return res;
    }


}

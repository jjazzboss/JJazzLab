
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.phrase.api.Phrases;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.phrasetransform.api.PhraseTransform;
import org.jjazz.phrasetransform.api.PhraseTransforms;
import org.jjazz.phrasetransform.api.PtProperties;
import org.jjazz.songcontext.api.SongPartContext;
import org.jjazz.uiutilities.api.UIUtilities;
import org.jjazz.utilities.api.FloatRange;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.phrasetransform.spi.PtPropertyEditorFactory;

/**
 * Add drums notes from a Midi file to a Drums phrase.
 * <p>
 */
public class AddDrumsMidiPhraseTransform implements PhraseTransform
{

    public static final String PROP_VELOCITY_OFFSET = "VelocityOffset";
    public static final float POS_WINDOW = 0.1f;
    private final PtProperties properties;
    private final Info info;
    private final SizedPhrase addPhrase;
    private final boolean replace;
    private static final Logger LOGGER = Logger.getLogger(AddDrumsMidiPhraseTransform.class.getSimpleName());

    /**
     * Add notes from a Midi file to the phrase.
     *
     * @param info
     * @param addPhrase Phrase beatrange must start at 0.
     * @param replace   If true the added notes will replaceAll all same-pitch notes from the destination phrase. If false notes are just added (but we try to
     *                  avoid having 2 notes too close).
     */
    public AddDrumsMidiPhraseTransform(PhraseTransform.Info info, SizedPhrase addPhrase, boolean replace)
    {
        checkNotNull(info);
        checkNotNull(addPhrase);
        checkArgument(addPhrase.getNotesBeatRange().from == 0, " addPhrase.getBeatRange()=%s", addPhrase.getNotesBeatRange());

        Properties defaults = new Properties();
        defaults.setProperty(PROP_VELOCITY_OFFSET, Integer.toString(0));
        properties = new PtProperties(defaults);

        this.info = info;
        this.addPhrase = addPhrase;
        this.replace = replace;
    }

    @Override
    public Info getInfo()
    {
        return info;
    }


    @Override
    public SizedPhrase transform(SizedPhrase inPhrase, SongPartContext context)
    {
        var res = inPhrase.clone();


        // Prepare the phrase to add so it matches dest phrase size and time signature
        Phrase p = getAdaptedAddPhrase(addPhrase, inPhrase);


        // Update the velocity of the phrase if needed
        p = p.getProcessedPhraseVelocity(v -> v + getVelocityOffset());


        if (replace)
        {
            // Remove all notes from same pitches before adding the new notes            
            var addPhrasePitches = Phrases.getNotesByPitch(addPhrase, ne -> true).keySet();
            res.removeIf(ne -> addPhrasePitches.contains(ne.getPitch()));
            res.add(p);

        } else
        {
            // Add new notes only if there is not already an identical note in the area            
            var br = inPhrase.getNotesBeatRange();

            List<NoteEvent> notesToAdd = new ArrayList<>();

            for (var ne : p)
            {
                FloatRange posRange = new FloatRange(
                        Math.max(br.from, ne.getPositionInBeats() - POS_WINDOW),
                        Math.min(br.to, ne.getPositionInBeats() + POS_WINDOW));
                var posNotes = res.getNotes(ne2 -> ne2.getPitch() == ne.getPitch(), posRange, false);
                if (posNotes.isEmpty())
                {
                    notesToAdd.add(ne);
                }
            }
            notesToAdd.forEach(ne -> res.add(ne));
        }

        return res;
    }

    @Override
    public int getFitScore(SizedPhrase inPhrase, SongPartContext context)
    {
        return PhraseTransforms.getRhythmVoice(inPhrase, context).isDrums() ? 100 : 0;
    }

    public int getVelocityOffset()
    {
        return properties.getPropertyAsInteger(PROP_VELOCITY_OFFSET);
    }

    public void setVelocityOffset(int offset)
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
        String text = ResUtil.getString(getClass(), "VelocityOffset");
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
    public AddDrumsMidiPhraseTransform getCopy()
    {
        var res = new AddDrumsMidiPhraseTransform(info, addPhrase, replace);
        res.properties.putAll(properties);
        return res;
    }


    // =================================================================================================
    // Private methods
    // =================================================================================================
    /**
     * Get an add-phrase adapted to the destination phrase size/time signature.
     * <p>
     * Adapted add-phrase will have the same beat range and time signature than the destination phrase. If destination phrase is larger than the add-phrase,
     * then add-phrase is duplicated as necessary (taking into account possible different time signatures). If destination phrase is shorter, add-phrase is
     * shortened.
     *
     * @param addSp  The original add-phrase. Can be any size and any time signature.
     * @param destSp The destination phrase. Can be any size and any time signature.
     * @return The new add-phrase adapted to the destination phrase, ready to be added.
     */
    private SizedPhrase getAdaptedAddPhrase(SizedPhrase addSp, SizedPhrase destSp)
    {
        SizedPhrase res = new SizedPhrase(destSp.getChannel(), destSp.getNotesBeatRange(), destSp.getTimeSignature(), destSp.isDrums());

        FloatRange addBr = addSp.getNotesBeatRange();
        TimeSignature addTs = addSp.getTimeSignature();
        int addNbBars = (int) Math.floor(addBr.size() / addTs.getNbNaturalBeats());
        FloatRange destBr = destSp.getNotesBeatRange();
        TimeSignature destTs = destSp.getTimeSignature();
        int destNbBars = (int) Math.floor(destBr.size() / destTs.getNbNaturalBeats());


        int nbAddCopies = destNbBars / addNbBars + ((destNbBars % addNbBars > 0) ? 1 : 0);

        int destBarOffset = 0;
        for (int i = 0; i < nbAddCopies; i++)
        {
            for (NoteEvent ne : addSp)
            {
                // Get the bar/beat position within the add-phrase
                int addBar = (int) Math.floor(ne.getPositionInBeats() / addTs.getNbNaturalBeats());
                float addBeat = ne.getPositionInBeats() - addBar * addTs.getNbNaturalBeats();

                // Corresponding bar/beat in the destination phrase
                int destBar = destBarOffset + addBar;
                float destBeat = addBeat;


                if (!destTs.checkBeat(destBeat))
                {
                    // Destination phrase uses a shorter time signature
                    // Do nothing
                } else if (destBar >= destNbBars)
                {
                    // We're past the destination phrase. Can happen only when i==nbAddCopies-1.
                    break;
                } else
                {
                    // Translate to bar/beat position within the destination phrase
                    float destPos = destBr.from + destBar * destTs.getNbNaturalBeats() + destBeat;
                    float destDur = ne.getDurationInBeats();

                    if (!destBr.contains(destPos + destDur, false))
                    {
                        // Make sure note is not too long
                        destDur = destBr.to - destPos;
                    }

                    // Create the note at the right position
                    var nne  = ne.setAll(-1, destDur, -1, destPos, null, true);
                    res.add(nne);
                }
            }

            destBarOffset += addNbBars;
        }


        return res;
    }
}

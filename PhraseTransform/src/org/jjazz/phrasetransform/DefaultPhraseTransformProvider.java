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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.phrase.api.CyclicPositions;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.phrasetransform.api.DrumsMixTransform;
import org.openide.util.lookup.ServiceProvider;
import org.jjazz.phrasetransform.spi.PhraseTransformProvider;
import org.jjazz.phrasetransform.api.PhraseTransform;
import org.jjazz.phrasetransform.api.PhraseTransformCategory;
import org.jjazz.util.api.FloatRange;
import org.jjazz.util.api.ResUtil;
import org.netbeans.api.annotations.common.StaticResource;

/**
 * Default provider of PhraseTransforms
 */
@ServiceProvider(service = PhraseTransformProvider.class)
public class DefaultPhraseTransformProvider implements PhraseTransformProvider
{

    @StaticResource(relative = true)
    private static final String ACCENT1_ICON_PATH = "resources/Accent16-1-Transformer-48x24.png";
    private static final Icon ACCENT1_ICON = new ImageIcon(DefaultPhraseTransformProvider.class.getResource(ACCENT1_ICON_PATH));
    @StaticResource(relative = true)
    private static final String ACCENT2_ICON_PATH = "resources/Accent16-2-Transformer-48x24.png";
    private static final Icon ACCENT2_ICON = new ImageIcon(DefaultPhraseTransformProvider.class.getResource(ACCENT2_ICON_PATH));
    @StaticResource(relative = true)
    private static final String ACCENT3_ICON_PATH = "resources/Accent16-3-Transformer-48x24.png";
    private static final Icon ACCENT3_ICON = new ImageIcon(DefaultPhraseTransformProvider.class.getResource(ACCENT3_ICON_PATH));
    @StaticResource(relative = true)
    private static final String ACCENT4_ICON_PATH = "resources/Accent16-4-Transformer-48x24.png";
    private static final Icon ACCENT4_ICON = new ImageIcon(DefaultPhraseTransformProvider.class.getResource(ACCENT4_ICON_PATH));
    @StaticResource(relative = true)
    private static final String ADD_PERC_ICON_PATH = "resources/AddPerc-Transformer-48x24.png";
    private static final Icon ADD_PERC_ICON = new ImageIcon(DefaultPhraseTransformProvider.class.getResource(ADD_PERC_ICON_PATH));
    @StaticResource(relative = true)
    private static final String ADD_TAMBOURINE24_PATH = "resources/Tambourine2-4.mid";
    @StaticResource(relative = true)
    private static final String ADD_TAMBOURINE_OFFBEAT_PATH = "resources/TambourineOffBeat.mid";
    @StaticResource(relative = true)
    private static final String ADD_TAMBOURINE_EIGHTS_PATH = "resources/TambourineEighths.mid";


    private List<PhraseTransform> transforms = new ArrayList<>();

    private static final Logger LOGGER = Logger.getLogger(DefaultPhraseTransformProvider.class.getSimpleName());

    public DefaultPhraseTransformProvider()
    {
        transforms.add(new OpenHiHatTransform());
        transforms.add(new RideToHiHatTransform());
        transforms.add(new HiHatToRideTransform());
        transforms.add(new SnareToRimShotTransform());
        transforms.add(new RimShotToSnareTransform());

        transforms.add(getAddTambourine24());
        transforms.add(getAddTambourineOffBeat());
        transforms.add(getAddTambourineEights());

        transforms.add(getAccent1());
        transforms.add(getAccent2());
        transforms.add(getAccent3());
        transforms.add(getAccent4());

        transforms.add(new DrumsMixTransform());    // Hidden transform
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


    private AddDrumsMidiPhraseTransform getAddTambourine24()
    {
        PhraseTransform.Info info = new PhraseTransform.Info("AddTambourine24Id",
                ResUtil.getString(getClass(), "AddTambourine24_name"),
                ResUtil.getString(getClass(), "AddTambourine24_desc"),
                PhraseTransformCategory.DRUMS,
                ADD_PERC_ICON
        );

        SizedPhrase sp = loadDrumsSizedPhrase(ADD_TAMBOURINE24_PATH, 1, TimeSignature.FOUR_FOUR);
        AddDrumsMidiPhraseTransform t = new AddDrumsMidiPhraseTransform(info, sp, true);
        return t;
    }

    private AddDrumsMidiPhraseTransform getAddTambourineOffBeat()
    {
        PhraseTransform.Info info = new PhraseTransform.Info("AddTambourineOffBeatId",
                ResUtil.getString(getClass(), "AddTambourineOffBeat_name"),
                ResUtil.getString(getClass(), "AddTambourineOffBeat_desc"),
                PhraseTransformCategory.DRUMS,
                ADD_PERC_ICON
        );

        SizedPhrase sp = loadDrumsSizedPhrase(ADD_TAMBOURINE_OFFBEAT_PATH, 1, TimeSignature.FOUR_FOUR);
        AddDrumsMidiPhraseTransform t = new AddDrumsMidiPhraseTransform(info, sp, true);
        return t;
    }

    private AddDrumsMidiPhraseTransform getAddTambourineEights()
    {
        PhraseTransform.Info info = new PhraseTransform.Info("AddTambourineEightsId",
                ResUtil.getString(getClass(), "AddTambourineEights_name"),
                ResUtil.getString(getClass(), "AddTambourineEights_desc"),
                PhraseTransformCategory.DRUMS,
                ADD_PERC_ICON
        );

        SizedPhrase sp = loadDrumsSizedPhrase(ADD_TAMBOURINE_EIGHTS_PATH, 1, TimeSignature.FOUR_FOUR);

        AddDrumsMidiPhraseTransform t = new AddDrumsMidiPhraseTransform(info, sp, true);
        return t;
    }


    private SizedPhrase loadDrumsSizedPhrase(String midiResourcePath, int nbBars, TimeSignature ts)
    {
        try (InputStream is = getClass().getResourceAsStream(midiResourcePath))
        {
            Phrase p = importDrumsPhrase(midiResourcePath, is);
            if (p.isEmpty())
            {
                throw new IOException("p is empty");
            } else if (p.getBeatRange().to > nbBars * ts.getNbNaturalBeats())
            {
                throw new IOException("Invalid p range: p.getBeatRange().to=" + p.getBeatRange().to + " nbBars=" + nbBars + " ts=" + ts);
            }

            SizedPhrase sp = new SizedPhrase(MidiConst.CHANNEL_DRUMS, new FloatRange(0, nbBars * ts.getNbNaturalBeats()), ts);
            sp.add(p);
            return sp;

        } catch (IOException | InvalidMidiDataException ex)
        {
            throw new IllegalArgumentException("Invalid midiResourcePath=" + midiResourcePath, ex);
        }
    }

    /**
     * Import a Drums phrase (Midi channel 10) from the Midi stream.
     * <p>
     *
     * @param midiFile
     *
     * @return Can be an empty phrase. Phrase with channel 10.
     */
    private Phrase importDrumsPhrase(String midiResourcePath, InputStream midiStream) throws IOException, InvalidMidiDataException
    {
        Phrase res = new Phrase(MidiConst.CHANNEL_DRUMS);

        // Load file into a sequence
        Sequence sequence = MidiSystem.getSequence(midiStream);       // Throws IOException, InvalidMidiDataException
        if (sequence.getDivisionType() != Sequence.PPQ)
        {
            throw new InvalidMidiDataException("Midi stream does not use PPQ division: midiResourcePath=" + midiResourcePath);
        }

        // Get our phrase
        Track[] tracks = sequence.getTracks();
        List<Phrase> phrases = Phrase.getPhrases(sequence.getResolution(), tracks, MidiConst.CHANNEL_DRUMS);
        if (phrases.size() == 1)
        {
            res.add(phrases.get(0));
        }

        return res;
    }

}

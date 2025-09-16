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
import org.jjazz.phrase.api.Phrases;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.phrasetransform.api.DrumsMixTransform;
import org.openide.util.lookup.ServiceProvider;
import org.jjazz.phrasetransform.spi.PhraseTransformProvider;
import org.jjazz.phrasetransform.api.PhraseTransform;
import org.jjazz.phrasetransform.api.PhraseTransformCategory;
import org.jjazz.utilities.api.FloatRange;
import org.jjazz.utilities.api.ResUtil;
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
    private static final String ADD_CABASA_ICON_PATH = "resources/AddCabasa-Transformer-48x24.png";
    private static final Icon ADD_CABASA_ICON = new ImageIcon(DefaultPhraseTransformProvider.class.getResource(ADD_CABASA_ICON_PATH));
    @StaticResource(relative = true)
    private static final String ADD_CONGAS_ICON_PATH = "resources/AddCongas-Transformer-48x24.png";
    private static final Icon ADD_CONGAS_ICON = new ImageIcon(DefaultPhraseTransformProvider.class.getResource(ADD_CONGAS_ICON_PATH));
    @StaticResource(relative = true)
    private static final String ADD_TAMBOURINE_ICON_PATH = "resources/AddTambourine-Transformer-48x24.png";
    private static final Icon ADD_TAMBOURINE_ICON = new ImageIcon(DefaultPhraseTransformProvider.class.getResource(ADD_TAMBOURINE_ICON_PATH));
    @StaticResource(relative = true)
    private static final String ADD_TRIANGLE_ICON_PATH = "resources/AddTriangle-Transformer-48x24.png";
    private static final Icon ADD_TRIANGLE_ICON = new ImageIcon(DefaultPhraseTransformProvider.class.getResource(ADD_TRIANGLE_ICON_PATH));
    @StaticResource(relative = true)
    private static final String ADD_COWBELL_ICON_PATH = "resources/AddTriangle-Transformer-48x24.png";
    private static final Icon ADD_COWBELL_ICON = new ImageIcon(DefaultPhraseTransformProvider.class.getResource(ADD_COWBELL_ICON_PATH));
    @StaticResource(relative = true)
    private static final String ADD_SHAKER_ICON_PATH = "resources/AddMaracas-Transformer-48x24.png";
    private static final Icon ADD_SHAKER_ICON = new ImageIcon(DefaultPhraseTransformProvider.class.getResource(ADD_SHAKER_ICON_PATH));
    @StaticResource(relative = true)
    private static final String ADD_TAMBOURINE24_PATH = "resources/Tambourine2-4.mid";
    @StaticResource(relative = true)
    private static final String ADD_TAMBOURINE_OFFBEAT_PATH = "resources/TambourineOffBeat.mid";
    @StaticResource(relative = true)
    private static final String ADD_TAMBOURINE_EIGHTHS_PATH = "resources/TambourineEighths.mid";
    @StaticResource(relative = true)
    private static final String ADD_TAMBOURINE_SIXTEENTH_PATH = "resources/TambourineSixteenths.mid";
    @StaticResource(relative = true)
    private static final String ADD_CONGAS1_PATH = "resources/Congas1-2bar.mid";
    @StaticResource(relative = true)
    private static final String ADD_CONGAS2_PATH = "resources/Congas2-2bar.mid";
    @StaticResource(relative = true)
    private static final String ADD_CONGAS3_PATH = "resources/Congas3-2bar.mid";
    @StaticResource(relative = true)
    private static final String ADD_CONGAS4_PATH = "resources/Congas4-2bar.mid";
    @StaticResource(relative = true)
    private static final String ADD_SHAKER_SIXTEENTH_PATH = "resources/ShakerSixteenths.mid";
    @StaticResource(relative = true)
    private static final String ADD_TRIANGLE_SIXTEENTH_PATH = "resources/TriangleSixteenths.mid";
    @StaticResource(relative = true)
    private static final String ADD_TRIANGLE_EIGHTH_PATH = "resources/TriangleEighths.mid";
    @StaticResource(relative = true)
    private static final String ADD_COWBELL_BEAT_PATH = "resources/CowBellBeat.mid";
    @StaticResource(relative = true)
    private static final String ADD_CABASAS_EIGHTH_PATH = "resources/CabasaEighths.mid";


    private final List<PhraseTransform> transforms = new ArrayList<>();

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
        transforms.add(getAddTambourineSixteenths());

        transforms.add(getCongas2bar(1, ADD_CONGAS1_PATH));
        transforms.add(getCongas2bar(2, ADD_CONGAS2_PATH));
        transforms.add(getCongas2bar(3, ADD_CONGAS3_PATH));
        transforms.add(getCongas2bar(4, ADD_CONGAS4_PATH));

        transforms.add(getCowBellBeat());
        transforms.add(getCabasaEighths());
        transforms.add(getTriangleEighths());
        transforms.add(getTriangleSixteenths());
        transforms.add(getShakerSixteenths());

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

    private AddDrumsMidiPhraseTransform getCongas2bar(int index, String midiResourcePath)
    {
        String name = "AddCongas" + index;
        String name_name = name + "_name";
        String name_desc = name + "_desc";
        PhraseTransform.Info info = new PhraseTransform.Info(name + "Id",
                ResUtil.getString(getClass(), name_name),
                ResUtil.getString(getClass(), name_desc),
                PhraseTransformCategory.DRUMS,
                ADD_CONGAS_ICON
        );

        SizedPhrase sp = loadDrumsSizedPhrase(midiResourcePath, 2, TimeSignature.FOUR_FOUR);
        AddDrumsMidiPhraseTransform t = new AddDrumsMidiPhraseTransform(info, sp, false);
        return t;
    }

    private AddDrumsMidiPhraseTransform getShakerSixteenths()
    {
        PhraseTransform.Info info = new PhraseTransform.Info("AddShakerSixteenthsId",
                ResUtil.getString(getClass(), "AddShakerSixteenths_name"),
                ResUtil.getString(getClass(), "AddShakerSixteenths_desc"),
                PhraseTransformCategory.DRUMS,
                ADD_SHAKER_ICON
        );

        SizedPhrase sp = loadDrumsSizedPhrase(ADD_SHAKER_SIXTEENTH_PATH, 1, TimeSignature.FOUR_FOUR);
        AddDrumsMidiPhraseTransform t = new AddDrumsMidiPhraseTransform(info, sp, true);
        return t;
    }

    private AddDrumsMidiPhraseTransform getTriangleSixteenths()
    {
        PhraseTransform.Info info = new PhraseTransform.Info("AddTriangleSixteenthsId",
                ResUtil.getString(getClass(), "AddTriangleSixteenths_name"),
                ResUtil.getString(getClass(), "AddTriangleSixteenths_desc"),
                PhraseTransformCategory.DRUMS,
                ADD_TRIANGLE_ICON
        );

        SizedPhrase sp = loadDrumsSizedPhrase(ADD_TRIANGLE_SIXTEENTH_PATH, 1, TimeSignature.FOUR_FOUR);
        AddDrumsMidiPhraseTransform t = new AddDrumsMidiPhraseTransform(info, sp, true);
        return t;
    }

    private AddDrumsMidiPhraseTransform getTriangleEighths()
    {
        PhraseTransform.Info info = new PhraseTransform.Info("AddTriangleEighthsId",
                ResUtil.getString(getClass(), "AddTriangleEighths_name"),
                ResUtil.getString(getClass(), "AddTriangleEighths_desc"),
                PhraseTransformCategory.DRUMS,
                ADD_TRIANGLE_ICON
        );

        SizedPhrase sp = loadDrumsSizedPhrase(ADD_TRIANGLE_EIGHTH_PATH, 1, TimeSignature.FOUR_FOUR);
        AddDrumsMidiPhraseTransform t = new AddDrumsMidiPhraseTransform(info, sp, true);
        return t;
    }

    private AddDrumsMidiPhraseTransform getCabasaEighths()
    {
        PhraseTransform.Info info = new PhraseTransform.Info("AddCabasaEighths",
                ResUtil.getString(getClass(), "AddCabasaEighths_name"),
                ResUtil.getString(getClass(), "AddCabasaEighths_desc"),
                PhraseTransformCategory.DRUMS,
                ADD_CABASA_ICON
        );

        SizedPhrase sp = loadDrumsSizedPhrase(ADD_CABASAS_EIGHTH_PATH, 1, TimeSignature.FOUR_FOUR);
        AddDrumsMidiPhraseTransform t = new AddDrumsMidiPhraseTransform(info, sp, true);
        return t;
    }

    private AddDrumsMidiPhraseTransform getCowBellBeat()
    {
        PhraseTransform.Info info = new PhraseTransform.Info("AddCowbellBeat",
                ResUtil.getString(getClass(), "AddCowbellBeat_name"),
                ResUtil.getString(getClass(), "AddCowbellBeat_desc"),
                PhraseTransformCategory.DRUMS,
                ADD_COWBELL_ICON
        );

        SizedPhrase sp = loadDrumsSizedPhrase(ADD_COWBELL_BEAT_PATH, 1, TimeSignature.FOUR_FOUR);
        AddDrumsMidiPhraseTransform t = new AddDrumsMidiPhraseTransform(info, sp, false);
        return t;
    }


    private AddDrumsMidiPhraseTransform getAddTambourine24()
    {
        PhraseTransform.Info info = new PhraseTransform.Info("AddTambourine24Id",
                ResUtil.getString(getClass(), "AddTambourine24_name"),
                ResUtil.getString(getClass(), "AddTambourine24_desc"),
                PhraseTransformCategory.DRUMS,
                ADD_TAMBOURINE_ICON
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
                ADD_TAMBOURINE_ICON
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
                ADD_TAMBOURINE_ICON
        );

        SizedPhrase sp = loadDrumsSizedPhrase(ADD_TAMBOURINE_EIGHTHS_PATH, 1, TimeSignature.FOUR_FOUR);

        AddDrumsMidiPhraseTransform t = new AddDrumsMidiPhraseTransform(info, sp, true);
        return t;
    }

    private AddDrumsMidiPhraseTransform getAddTambourineSixteenths()
    {
        PhraseTransform.Info info = new PhraseTransform.Info("AddTambourineSixteenthsId",
                ResUtil.getString(getClass(), "AddTambourineSixteenths_name"),
                ResUtil.getString(getClass(), "AddTambourineSixteenths_desc"),
                PhraseTransformCategory.DRUMS,
                ADD_TAMBOURINE_ICON
        );

        SizedPhrase sp = loadDrumsSizedPhrase(ADD_TAMBOURINE_SIXTEENTH_PATH, 1, TimeSignature.FOUR_FOUR);

        AddDrumsMidiPhraseTransform t = new AddDrumsMidiPhraseTransform(info, sp, true);
        return t;
    }


    private SizedPhrase loadDrumsSizedPhrase(String midiResourcePath, int nbBars, TimeSignature ts)
    {
        try (InputStream is = getClass().getResourceAsStream(midiResourcePath))
        {
            Phrase p = importDrumsPhrase(is);
            if (p.isEmpty())
            {
                throw new IOException("p is empty");
            } else if (p.getNotesBeatRange().to > nbBars * ts.getNbNaturalBeats())
            {
                throw new IOException("Invalid p range: p.getBeatRange().to=" + p.getNotesBeatRange().to + " nbBars=" + nbBars + " ts=" + ts);
            }

            SizedPhrase sp = new SizedPhrase(MidiConst.CHANNEL_DRUMS, new FloatRange(0, nbBars * ts.getNbNaturalBeats()), ts, true);
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
     * @param midiStream
     *
     * @return Can be an empty phrase. Phrase with channel 10.
     */
    private Phrase importDrumsPhrase(InputStream midiStream) throws IOException, InvalidMidiDataException
    {
        Phrase res = new Phrase(MidiConst.CHANNEL_DRUMS, true);

        // Load file into a sequence
        Sequence sequence = MidiSystem.getSequence(midiStream);       // Throws IOException, InvalidMidiDataException
        if (sequence.getDivisionType() != Sequence.PPQ)
        {
            throw new InvalidMidiDataException("Midi stream does not use PPQ division");
        }

        // Get our phrase
        Track[] tracks = sequence.getTracks();
        List<Phrase> phrases = Phrases.getPhrases(sequence.getResolution(), tracks, MidiConst.CHANNEL_DRUMS);
        if (phrases.size() == 1)
        {
            res.add(phrases.get(0));
        }

        return res;
    }

}

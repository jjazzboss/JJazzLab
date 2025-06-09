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
package org.jjazz.yjzwizard;

import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;
import org.jjazz.midi.api.Instrument;
import org.jjazz.midi.api.InstrumentMix;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midi.api.MidiUtilities;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.phrase.api.Phrases;
import org.jjazz.phrase.api.SourcePhrase;
import org.jjazz.phrase.api.SourcePhraseSet;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.jjazz.rhythmdatabase.api.RhythmInfo;
import org.jjazz.rhythmdatabase.api.UnavailableRhythmException;
import org.jjazz.yamjjazz.rhythm.api.AccType;
import org.jjazz.yamjjazz.rhythm.api.CtabChannelSettings;
import org.jjazz.yamjjazz.rhythm.api.Style;
import org.jjazz.yamjjazz.rhythm.api.StylePart;
import org.jjazz.yamjjazz.rhythm.api.StylePartType;
import org.jjazz.yamjjazz.rhythm.api.YamJJazzRhythm;

/**
 * Generate the Midi sequence which will be dumped as a .yjz file.
 */
public class YjzFileBuilder
{

    int nbMainA, nbMainB, nbMainC, nbMainD;
    int nbSrcPhrases;
    boolean includeIntroEndings, includeFills;
    RhythmInfo baseRhythmInfo;
    YamJJazzRhythm baseRhythmInstance;
    Style baseStyle;

    private static final Logger LOGGER = Logger.getLogger(YjzFileBuilder.class.getSimpleName());

    public YjzFileBuilder(RhythmInfo baseRhythmInfo, int nbMainA, int nbMainB, int nbMainC, int nbMainD, boolean includeIntroEndings, boolean includeFills, int nbSrcPhrases)
    {
        this.nbMainA = nbMainA;
        this.nbMainB = nbMainB;
        this.nbMainC = nbMainC;
        this.nbMainD = nbMainD;
        this.includeIntroEndings = includeIntroEndings;
        this.includeFills = includeFills;
        this.nbSrcPhrases = nbSrcPhrases;
        this.baseRhythmInfo = baseRhythmInfo;
    }

    public Sequence buildSequence() throws UnavailableRhythmException, MusicGenerationException, InvalidMidiDataException
    {
        assert !baseRhythmInfo.isAdaptedRhythm() : "baseRhythm=" + baseRhythmInfo;   //NOI18N


        // Get the base rhythm & style
        var r = RhythmDatabase.getDefault().getRhythmInstance(baseRhythmInfo);
        assert r instanceof YamJJazzRhythm : "r.getClass()=" + r.getClass();   //NOI18N
        r.loadResources();  // Make source phrases are loaded
        baseRhythmInstance = (YamJJazzRhythm) r;
        assert !baseRhythmInstance.isExtendedRhythm() : "baseRhythm=" + baseRhythmInstance;   //NOI18N
        baseStyle = baseRhythmInstance.getStyle();


        Sequence sequence = new Sequence(Sequence.PPQ, MidiConst.PPQ_RESOLUTION);


        // Add some convenient MetaMessages on track0 to facilitate Midi editing
        createTrack0(sequence);


        // Process each style part
        for (StylePartType spType : baseStyle.getStylePartTypes())  // getStylePartTypes() returns an ordered list
        {
            switch (spType)
            {
                case Intro_A:
                case Intro_B:
                case Intro_C:
                case Intro_D:
                    createTracksForSubVariations(includeIntroEndings ? 1 : 0, sequence, baseStyle.getStylePart(spType), nbSrcPhrases);
                    break;
                case Main_A:
                    createTracksForSubVariations(nbMainA, sequence, baseStyle.getStylePart(spType), nbSrcPhrases);
                    break;
                case Main_B:
                    createTracksForSubVariations(nbMainB, sequence, baseStyle.getStylePart(spType), nbSrcPhrases);
                    break;
                case Main_C:
                    createTracksForSubVariations(nbMainC, sequence, baseStyle.getStylePart(spType), nbSrcPhrases);
                    break;
                case Main_D:
                    createTracksForSubVariations(nbMainD, sequence, baseStyle.getStylePart(spType), nbSrcPhrases);
                    break;
                case Fill_In_AA:
                case Fill_In_BB:
                case Fill_In_CC:
                case Fill_In_DD:
                case Fill_In_BA:
                case Fill_In_AB:
                    createTracksForSubVariations(includeFills ? 1 : 0, sequence, baseStyle.getStylePart(spType), nbSrcPhrases);
                    break;
                case Ending_A:
                case Ending_B:
                case Ending_C:
                case Ending_D:
                    createTracksForSubVariations(includeIntroEndings ? 1 : 0, sequence, baseStyle.getStylePart(spType), nbSrcPhrases);
                    break;
                default:
                    throw new AssertionError(spType.name());

            }
        }

        return sequence;
    }

    // ======================================================================================================
    // Private methods
    // ======================================================================================================    
    /**
     * Useless for the rhythm itself, but convenient for editing in a DAW
     *
     * @param sequence
     */
    private void createTrack0(Sequence sequence)
    {
        Track track0 = sequence.createTrack();
        String trackName = "Track0: meta info";
        MidiEvent me = new MidiEvent(MidiUtilities.getTrackNameMetaMessage(trackName), 0);
        track0.add(me);


        // Time signature MidiEvent 
        me = new MidiEvent(MidiUtilities.getTimeSignatureMessage(baseRhythmInfo.timeSignature()), 0);
        track0.add(me);


        // General MIDI ON
        me = new MidiEvent(MidiUtilities.getGmModeOnSysExMessage(), 0);
        track0.add(me);


        // Add Markers on each source phrase start (using size reference of Main A variation)
        int sizeInBeats = (int) baseStyle.getStylePart(StylePartType.Main_A).getSizeInBeats();
        for (int i = 0; i < nbSrcPhrases + 1; i++)
        {
            long tick = i * MidiConst.PPQ_RESOLUTION * sizeInBeats;
            me = new MidiEvent(MidiUtilities.getMarkerMetaMessage("Main-A P" + (i + 1) + ">"), tick);
            track0.add(me);
        }
    }

    private void createTracksForSubVariations(int nbSubVars, Sequence sequence, StylePart spt, int nbSrcPhrases)
    {
        for (int i = 0; i < nbSubVars; i++)
        {
            String variationName = spt.getType().toString() + "-" + (i + 1);
            createTracksForVariation(sequence, variationName, spt, nbSrcPhrases);
        }
    }

    /**
     * Create the tracks in specified sequence for variation with varName
     *
     * @param sequence
     * @param baseRhythm
     * @param variationName e.g. "Main A-2"
     * @param baseStylePart
     * @param nbSrcPhrases
     */
    private void createTracksForVariation(Sequence sequence, String variationName, StylePart baseStylePart, int nbSrcPhrases)
    {
        LOGGER.log(Level.FINE, "variationName={0} baseStylePart={1} nbSrcPhrases={2}", new Object[]{variationName, baseStylePart,
            nbSrcPhrases});


        // We want the same ordering of tracks in all variations        
        var usedAccTypes = baseStylePart.getAccTypes();
        Collections.sort(usedAccTypes);


        // Add a track for each source phrase of each AccType (e.g. DRUMS, CHORD1, ...)        
        for (AccType at : usedAccTypes)
        {
            for (int srcChannel : baseStylePart.getSourceChannels(at, null, null))
            {
                // Channel settings
                CtabChannelSettings cTab = baseStylePart.getCtabChannelSettings(srcChannel);
                String srcChord = cTab.getSourceChordSymbol().getName();


                // Creat thee empty track
                Track track = sequence.createTrack();


                int sizeInBeats = (int) baseStylePart.getSizeInBeats();


                // Add the appropriate track name
                RhythmVoice rv = baseRhythmInstance.getRhythmVoice(at);
                String trackName = buildTrackName(variationName, at, rv, srcChord, srcChannel, sizeInBeats);
                MidiEvent me = new MidiEvent(MidiUtilities.getTrackNameMetaMessage(trackName), 0);
                track.add(me);


                // Get source phrase
                SourcePhraseSet sps = baseStylePart.getSourcePhraseSet(1, 0);   // There can be only one SourcePhraseSet
                SourcePhrase srcPhrase = sps.getPhrase(srcChannel);
                if (srcPhrase == null)
                {
                    sequence.deleteTrack(track);
                    continue;
                }


                LOGGER.log(Level.FINE, "at={0} srcChannel={1} trackName={2}", new Object[]{at, srcChannel, trackName});


                // Copy the source phrase 1 or more times                                
                Phrase p = new Phrase(srcChannel, at.isDrums());
                p.add(srcPhrase);
                for (int i = 0; i < nbSrcPhrases; i++)
                {
                    Phrases.fillTrack(p, track);
                    p.shiftAllEvents(sizeInBeats, false);
                }


                // Add GM program changes for non drums tracks to facilitate editing
                if (!rv.isDrums())
                {
                    for (MidiMessage mm : rv.getPreferredInstrument().getSubstitute().getMidiMessages(srcChannel))
                    {
                        track.add(new MidiEvent(mm, 0));
                    }
                }


                // Also add vol/pan/effect controller messages
                InstrumentMix insMix = baseStyle.getSInt().get(at);
                for (MidiMessage mm : insMix.getSettings().getAllMidiMessages(srcChannel))
                {
                    track.add(new MidiEvent(mm, 0));
                }


                // Add additional CASM info in a text marker
                String text = "MainNTR=" + cTab.ctb2Main.ntr + ", " + "MutedChords=" + cTab.mutedChords;
                track.add(new MidiEvent(MidiUtilities.getTextMetaMessage(text), 0));

            }
        }
    }

    /**
     *
     * @param variationName E.g "Main A-2"
     * @param rv            Used to propose a understandable id, e.g. "Guitar"
     * @param srcChord
     * @param channel       E.g. 12
     * @param sizeInBeats   E.g 8
     * @return E.g. Main A-2-Guitar[12]-8
     */
    private String buildTrackName(String variationName, AccType at, RhythmVoice rv, String srcChord, int channel, int sizeInBeats)
    {
        String id;
        switch (rv.getType())
        {
            case DRUMS:
                id = "Drums";
                break;
            case PERCUSSION:
                id = "Perc";
                break;
            case BASS:
                id = "Bass";
                break;
            case PAD:
                id = "Pad";
                break;
            default:    // VOICE
                Instrument prefIns = rv.getPreferredInstrument();
                switch (prefIns.getSubstitute().getFamily())
                {
                    case Guitar:
                        id = "Guit";
                        break;
                    case Piano:
                        id = "Piano";
                        break;
                    case Organ:
                        id = "Organ";
                        break;
                    case Synth_Lead:
                        id = "Synth";
                        break;
                    case Bass:
                        id = "Bass";
                        break;
                    case Brass:
                    case Reed:
                        id = "Brass";
                        break;
                    case Strings:
                    case Synth_Pad:
                    case Ensemble:
                        id = "Strgs";
                        break;
                    case Percussive:
                        id = "Vibra";
                        break;
                    default: // Ethnic, Sound_Effects, Synth_Effects, Pipe, Chromatic_Percussion:
                        id = "Other";
                }

                id += "_" + at.toString();
        }

        return variationName + "-[" + id + "," + srcChord + ",ch" + (channel + 1) + "]-" + sizeInBeats;
    }

}

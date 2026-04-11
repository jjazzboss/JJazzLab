/*
 *
 *   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *   Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 *   This file is part of the JJazzLab software.
 *
 *   JJazzLab is free software: you can redistribute it and/or modify
 *   it under the terms of the Lesser GNU General Public License (LGPLv3)
 *   as published by the Free Software Foundation, either version 3 of the License,
 *   or (at your option) any later version.
 *
 *   JJazzLab is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *
 *   Contributor(s):
 *
 */
package org.jjazz.song;

import io.github.secretx33.resourceresolver.PathMatchingResourcePatternResolver;
import io.github.secretx33.resourceresolver.Resource;
import io.github.secretx33.resourceresolver.ResourcePatternResolver;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.ParseException;
import java.util.Locale;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.ChordRenderingInfo;
import org.jjazz.chordleadsheet.api.item.VoidAltExtChordSymbol;
import org.jjazz.jjswing.api.RP_BassStyle;
import org.jjazz.midimix.spi.MidiMixManager;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.rhythmparametersimpl.api.RP_SYS_CustomPhrase;
import org.jjazz.rhythmparametersimpl.api.RP_SYS_CustomPhraseValue;
import org.jjazz.rhythmparametersimpl.api.RP_SYS_DrumsTransform;
import org.jjazz.rhythmparametersimpl.api.RP_SYS_DrumsTransformValue;
import org.jjazz.rhythmparametersimpl.api.RP_SYS_Intensity;
import org.jjazz.rhythmparametersimpl.api.RP_SYS_OverrideTracks;
import org.jjazz.rhythmparametersimpl.api.RP_SYS_OverrideTracksValue;
import org.jjazz.rhythmparametersimpl.api.RP_SYS_Variation;
import org.jjazz.song.spi.SongFactory;
import org.jjazz.utilities.api.Utilities;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

/**
 * Tests that sample .sng files (covering different serialization versions) can be loaded without throwing an exception.
 */
public class LoadSongTest
{

    static Path songDir;

    static
    {
        Utilities.setLoggingFormat(null);
        Locale.setDefault(Locale.ENGLISH);
    }

    @BeforeAll
    public static void setUpClass(TestInfo testInfo) throws Exception
    {
        System.out.println("\n" + testInfo.getDisplayName() + "     ########################\n");

        // Copy resources because we need both .sng and .mix files to be present
        songDir = Paths.get("target/testSongs");
        songDir.toFile().mkdir();

        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:org/jjazz/song/*.sng");
        for (Resource resource : resources)
        {
            Files.copy(resource.getInputStream(), songDir.resolve(resource.getFilename()), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("   copying " + resource.getFilename() + " to " + songDir.toAbsolutePath().toString());
        }
        resources = resolver.getResources("classpath:org/jjazz/song/*.mix");
        for (Resource resource : resources)
        {
            Files.copy(resource.getInputStream(), songDir.resolve(resource.getFilename()), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("   copying " + resource.getFilename() + " to " + songDir.toAbsolutePath().toString());
        }
    }

    @BeforeEach
    public void setUp(TestInfo testInfo) throws UnsupportedEditException, ParseException
    {
        System.out.println(testInfo.getDisplayName() + " ------");

    }


    @Test
    public void testLoadPhaseDance2020() throws Exception
    {
        var file = songDir.resolve("PhaseDance - 2020-spVersion1.sng").toFile();
        var song = SongFactory.getDefault().loadFromFile(file);

        assertNotNull(song);
        assertEquals(270, song.getSize());
        assertEquals("PhaseDance - 2020-spVersion1", song.getName());
        assertEquals(file, song.getFile());


        var cls = song.getChordLeadSheet();
        var chords = cls.getItems(116, 116, CLI_ChordSymbol.class);
        assertEquals(2, chords.size());
        assertEquals("FM7", chords.getLast().getData().getName());
        assertEquals(1, chords.getLast().getPosition().getBeat());
        assertTrue(chords.getLast().getData().getRenderingInfo().hasAllFeatures(ChordRenderingInfo.Feature.HOLD, ChordRenderingInfo.Feature.ACCENT));


        var sgs = song.getSongStructure();
        var spts = sgs.getSongParts();
        var rpVariation = RP_SYS_Variation.getVariationRp(spts.getFirst().getRhythm());
        var rpIntensity = RP_SYS_Intensity.getIntensityRp(spts.getFirst().getRhythm());
        assertEquals(37, spts.size());
        assertEquals("Ending A-1", spts.getLast().getRPValue(rpVariation));
        assertEquals(3, spts.get(35).getRPValue(rpIntensity));
    }

    @Test
    public void testLoadSoul2022() throws Exception
    {
        var file = songDir.resolve("Soul - 2022 - spVersion2.sng").toFile();
        var song = SongFactory.getDefault().loadFromFile(file);

        assertNotNull(song);
        assertEquals(32, song.getSize());
    }

    @Test
    public void testLoadGetLucky2024() throws Exception
    {
        var file = songDir.resolve("GetLuckyTest - 2024 - spVersion4.sng").toFile();
        var song = SongFactory.getDefault().loadFromFile(file);

        assertNotNull(song);
        assertEquals(42, song.getSize());
    }

    @Test
    public void testLoadMaxine2024() throws Exception
    {
        var file = songDir.resolve("Maxine_new - 2024 - spVersion4.sng").toFile();
        var song = SongFactory.getDefault().loadFromFile(file);

        assertNotNull(song);
        assertEquals(54, song.getSize());
        assertEquals(58, song.getTempo());
    }

    @Test
    public void testLoadSpeakNoEvil2026() throws Exception
    {
        var file = songDir.resolve("SpeakNoEvil - 2026.sng").toFile();
        var song = SongFactory.getDefault().loadFromFile(file);

        // Song
        assertNotNull(song);
        assertEquals(138, song.getSize());
        assertEquals(138, song.getTempo());
        var p = song.getUserPhrase("User1");
        assertNotNull(p);
        assertEquals(4, p.size());
        assertEquals(16, p.getLast().getPositionInBeats());
        assertEquals(71, p.getLast().getPitch());
        assertEquals(null, song.getUserPhrase("User2"));


        // ChordLeadSheet
        var cls = song.getChordLeadSheet();
        assertEquals(32, cls.getSizeInBars());

        var chords = cls.getItems(30, 30, CLI_ChordSymbol.class);
        assertEquals(1, chords.size());
        assertEquals("DbM7#11", chords.getFirst().getData().getName());
        assertEquals(0, chords.getFirst().getPosition().getBeat());

        chords = cls.getItems(9, 9, CLI_ChordSymbol.class);
        assertEquals(3, chords.size());
        assertEquals("Em7", chords.getFirst().getData().getName());
        assertEquals(0, chords.getFirst().getPosition().getBeat());
        assertEquals(VoidAltExtChordSymbol.getInstance(), chords.getFirst().getData().getAlternateChordSymbol());
        assertEquals("theme", chords.getFirst().getData().getAlternateFilter().getValues().get(0));


        // SongStructure
        var sgs = song.getSongStructure();
        var spts = sgs.getSongParts();
        assertEquals(13, spts.size());
        var spt0 = spts.get(0);
        var spt1 = spts.get(1);
        var spt2 = spts.get(2);
        var spt3 = spts.get(3);
        var spt4 = spts.get(4);
        var spt12Last = spts.getLast();


        // Rhythm
        var r = spt0.getRhythm();
        assertEquals("jjSwing", r.getName());
        var rvBass = r.getRhythmVoices().stream()
                .filter(rv -> rv.getName().equals("Bass"))
                .findAny()
                .orElseThrow();
        assertNotNull(rvBass);
        var rvPhrase1 = r.getRhythmVoices().stream()
                .filter(rv -> rv.getName().equals("Phrase1"))
                .findAny()
                .orElseThrow();
        assertNotNull(rvPhrase1);


        // Variation        
        var rpVariation = RP_SYS_Variation.getVariationRp(r);
        assertEquals("Ending A-1", spt12Last.getRPValue(rpVariation));
        assertEquals("Main B-1", spt3.getRPValue(rpVariation));

        // Intensity
        var rpIntensity = RP_SYS_Intensity.getIntensityRp(r);
        assertEquals(1, spt3.getRPValue(rpIntensity));

        // BassStyle
        var rpBass = RP_BassStyle.get(r);
        assertEquals("walking", spt1.getRPValue(rpBass));

        // DrumsTransform
        RP_SYS_DrumsTransform rpDrumsTransform = RP_SYS_DrumsTransform.getDrumsTransformRp(r);
        RP_SYS_DrumsTransformValue rpDrumsTransformValue = spt3.getRPValue(rpDrumsTransform);
        assertEquals("CR:-24", rpDrumsTransformValue.toString());
        assertEquals("", spt4.getRPValue(rpDrumsTransform).toString());

        // CustomPhrase
        RP_SYS_CustomPhrase rpCustomPhrase = RP_SYS_CustomPhrase.getCustomPhraseRp(r);
        RP_SYS_CustomPhraseValue rpCustomPhraseValue = spt3.getRPValue(rpCustomPhrase);
        assertEquals(null, rpCustomPhraseValue.getCustomizedPhrase(rvBass));
        p = rpCustomPhraseValue.getCustomizedPhrase(rvPhrase1);
        assertEquals(29, p.size());
        var lastNote = p.getNotes().getLast();
        assertEquals(69, lastNote.getPitch());
        assertEquals(61, lastNote.getVelocity());
        assertEquals(25, (int) lastNote.getPositionInBeats());
        assertEquals(null, spt2.getRPValue(rpCustomPhrase).getCustomizedPhrase(rvPhrase1));


        // Override track
        RP_SYS_OverrideTracks rpOverride = RP_SYS_OverrideTracks.getOverrideTracksRp(r);
        RP_SYS_OverrideTracksValue rpOverrideValue = spt2.getRPValue(rpOverride);
        RP_SYS_OverrideTracksValue.Override override = rpOverrideValue.getOverride(rvBass);
        assertNotNull(override);
        assertEquals("Main A-1", override.variation());
        assertEquals("Chord2", override.rvDest().getName());
        assertEquals("jjSwing", override.rvDest().getContainer().getName());
        assertEquals(null, rpOverrideValue.getOverride(rvPhrase1));
        // There IS actually an override defined in spt1, but to a rhythm NOT present in the test rhythm database, so it should have been discarded upong song loading        
        assertEquals(0, spt1.getRPValue(rpOverride).getAllSourceRhythmVoices().size());


        // MidiMix
        var mm = MidiMixManager.getDefault().findMix(song);
        var rvDrums = r.getRhythmVoices().stream()
                .filter(rv -> rv.isDrums())
                .findFirst()
                .orElseThrow();
        var insMix = mm.getInstrumentMix(rvDrums);
        var settings = insMix.getSettings();
        assertFalse(insMix.isMute());
        assertFalse(insMix.isSolo());
        assertTrue(settings.isChorusEnabled());
        assertEquals(8, settings.getChorus());
        assertEquals(96, settings.getVolume());


        // User rhythm voice
        assertNotNull(mm.getUserRhythmVoice("User1"));
        assertEquals(null, mm.getUserRhythmVoice("User2"));


        var rvPiano = r.getRhythmVoices().stream()
                .filter(rv -> rv.getName().equals("Chord1"))
                .findFirst()
                .orElseThrow();
        insMix = mm.getInstrumentMix(rvPiano);
        settings = insMix.getSettings();
        assertFalse(insMix.isMute());
        assertFalse(insMix.isSolo());
        assertTrue(insMix.isInstrumentEnabled());
        assertFalse(settings.isVolumeEnabled());
        assertEquals(34, settings.getReverb());
        assertEquals(40, settings.getVolume());
        assertEquals(64, settings.getPanoramic());
        assertEquals(1, settings.getVelocityShift());
        assertEquals(-1, settings.getTransposition());


        var rvGuitar = r.getRhythmVoices().stream()
                .filter(rv -> rv.getName().equals("Chord2"))
                .findFirst()
                .orElseThrow();
        insMix = mm.getInstrumentMix(rvGuitar);
        settings = insMix.getSettings();
        assertTrue(insMix.isMute());
        assertFalse(insMix.isSolo());
        assertTrue(settings.isVolumeEnabled());
        assertFalse(settings.isReverbEnabled());
        assertEquals(0, settings.getVolume());
        assertEquals(0, settings.getVolume());

    }
    // =========================================================================================================
    // Helper methods
    // =========================================================================================================


}

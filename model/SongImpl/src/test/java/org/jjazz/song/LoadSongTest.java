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

import java.io.File;
import java.net.URL;
import java.text.ParseException;
import java.util.Locale;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.song.api.Song;
import org.jjazz.song.spi.SongFactory;
import org.jjazz.utilities.api.Utilities;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

/**
 * Tests that sample .sng files (covering different serialization versions) can be loaded without throwing an exception.
 */
public class LoadSongTest
{

    static
    {
        Utilities.setLoggingFormat(null);
        Locale.setDefault(Locale.ENGLISH);
    }

    @BeforeAll
    public static void setUpClass(TestInfo testInfo) throws Exception
    {
        System.out.println("\n" + testInfo.getDisplayName() + "     ########################\n");
    }

    @BeforeEach
    public void setUp(TestInfo testInfo) throws UnsupportedEditException, ParseException
    {
        System.out.println(testInfo.getDisplayName() + " ------");
    }

    @Test
    public void testLoadPhaseDance2020() throws Exception
    {
        loadSng("PhaseDance - 2020-spVersion1.sng");
    }

    @Test
    public void testLoadSoul2022() throws Exception
    {
        loadSng("Soul - 2022 - spVersion2.sng");
    }

    @Test
    public void testLoadGetLucky2024() throws Exception
    {
        loadSng("GetLuckyTest - 2024 - spVersion4.sng");
    }

    @Test
    public void testLoadMaxine2024() throws Exception
    {
        loadSng("Maxine_new - 2024 - spVersion4.sng");
    }

    @Test
    public void testLoadSpeakNoEvil2026() throws Exception
    {
        loadSng("SpeakNoEvil - 2026.sng");
    }

    // =========================================================================================================
    // Helper methods
    // =========================================================================================================

    /**
     * Load a .sng resource by name (relative to this class's package) and assert the result is non-null.
     *
     * @param resourceName file name of the .sng resource
     * @throws java.lang.Exception
     */
    private void loadSng(String resourceName) throws Exception
    {
        URL url = LoadSongTest.class.getResource(resourceName);
        assertNotNull(url, "Resource not found on classpath: " + resourceName);

        var file = new File(url.toURI());
        Song song = SongFactory.getDefault().loadFromFile(file);
        assertNotNull(song, "loadFromFile() returned null for: " + resourceName);
    }

}

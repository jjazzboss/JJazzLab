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

import java.awt.Dialog;
import java.io.File;
import java.net.URL;
import java.util.Locale;
import java.util.logging.Logger;
import org.jjazz.rhythmdatabase.api.DefaultRhythmDatabase;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.jjazz.song.api.Song;
import org.jjazz.song.spi.SongFactory;
import org.jjazz.utilities.api.Utilities;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.lookup.ServiceProvider;

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
    public static void setUpClass()
    {
        // Populate the rhythm database so that XStream converters that reference rhythms can resolve them
        var rdb = (DefaultRhythmDatabase) RhythmDatabase.getDefault();
        rdb.addRhythmsFromRhythmProviders(false, true, false);
        System.out.println(rdb.toStatsString());
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


    /**
     * Basic implementation which just print messages on stdout (instead of getting a dialog).
     */
    @ServiceProvider(service = DialogDisplayer.class)
    static public class TestDialogDisplayer extends DialogDisplayer
    {

        private static final Logger LOGGER = Logger.getLogger(TestDialogDisplayer.class.getSimpleName());

        public TestDialogDisplayer()
        {
            LOGGER.info("Using ToolkitDialogDisplayer()");
        }

        @Override
        public Object notify(NotifyDescriptor nd)
        {
            String msg = nd.getMessage().toString();
            switch (nd.getMessageType())
            {
                case NotifyDescriptor.WARNING_MESSAGE ->
                    LOGGER.warning(msg);
                case NotifyDescriptor.ERROR_MESSAGE ->
                    LOGGER.severe(msg);
                case NotifyDescriptor.QUESTION_MESSAGE ->
                    throw new IllegalArgumentException("nd=" + nd);
                default ->
                    LOGGER.info(msg);
            }

            return NotifyDescriptor.CLOSED_OPTION;
        }

        @Override
        public Dialog createDialog(DialogDescriptor dd)
        {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

    }


}

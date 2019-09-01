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
package org.jjazz.harmony;

import java.util.List;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author lelasseux
 */
public class ModeTest
{

    private static final Logger LOGGER = Logger.getLogger(ModeTest.class.getSimpleName());

    public ModeTest()
    {
    }

    @BeforeClass
    public static void setUpClass()
    {
    }

    @AfterClass
    public static void tearDownClass()
    {
    }

    @Before
    public void setUp()
    {
    }

    @After
    public void tearDown()
    {
    }

    /**
     * Test of relativeMajorKey method, of class Mode.
     */
    @Test
    public void testRelativeMajorKey()
    {
        System.out.println("testRelativeMajorKey =======================================================================================");
        System.out.println(" relativeMajorKey Eb:DORIAN");
//        Mode instance = Mode.DORIAN;
//        instance.setStartNote(new Note(63));  // Eb=63
//        Note result = instance.relativeMajorKey();
//        System.out.println(" instance=" + instance + " result=" + result);
//        assertTrue(result.getRelativePitch() == 1);  // Db
//        System.out.println(" relativeMajorKey G:AEOLIAN");
//        instance = Mode.AEOLIAN;
//        instance.setStartNote(new Note(67));  // 67=G
//        result = instance.relativeMajorKey();
//        System.out.println(" instance=" + instance + " result=" + result);
//        assertTrue(result.getRelativePitch() == 10); // Bb
    }

    /**
     * Test of getNotes method, of class Mode.
     */
    @Test
    public void testGetNotes()
    {
        System.out.println("testGetNotes =======================================================================================");
        System.out.println(" getNotes Eb:DORIAN");
//        Mode instance = Mode.DORIAN;
//        instance.setStartNote(new Note(63));  // Eb=63
//        System.out.println(" instance.getNotes()=" + instance.getNotes());
    }

}

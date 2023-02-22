/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
package org.jjazz.util;

import org.jjazz.util.api.Utilities;
import java.awt.Font;
import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Administrateur
 */
public class UtilitiesTest
{

    public UtilitiesTest()
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
     * Test of getCurrentDir method, of class Utilities.
     */
    @Test
    public void testGetCurrentDir()
    {
        System.out.println("getCurrentDir");
        String result = Utilities.getCurrentDir();
        System.out.println("result=" + result);
        // TODO review the generated test code and remove the default call to fail.
        // fail("The test case is a prototype.");
    }

    /**
     * Test of endsWithIgnoreCase method, of class Utilities.
     */
    public void testEndsWithIgnoreCase()
    {
        System.out.println("endsWithIgnoreCase");
        String str = "";
        String ext = "";
        boolean expResult = false;
        boolean result = Utilities.endsWithIgnoreCase(str, ext);
        assertEquals(expResult, result);   
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getExtension method, of class Utilities.
     */
    public void testGetExtension()
    {
        System.out.println("getExtension");
        String path = "";
        String expResult = "";
        String result = Utilities.getExtension(path);
        assertEquals(expResult, result);   
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of replaceExtension method, of class Utilities.
     */
    public void testReplaceExtension()
    {
        System.out.println("replaceExtension");
        String filename = "";
        String ext = "";
        String expResult = "";
        String result = Utilities.replaceExtension(filename, ext);
        assertEquals(expResult, result);   
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of swapList method, of class Utilities.
     */
    public void testSwapList()
    {
        System.out.println("swapList");
        // Utilities.swapList(null);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of reverseGet method, of class Utilities.
     */
    public void testReverseGet()
    {
        System.out.println("reverseGet");
        Map<?,?> map = null;
        Object v = null;
        Object expResult = null;
        Object result = Utilities.reverseGet(map, v);
        assertEquals(expResult, result);   
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of indexOfInstance method, of class Utilities.
     */
    public void testIndexOfInstance()
    {
        System.out.println("indexOfInstance");
        List<?> list = null;
        Class<?> clazz = null;
        int expResult = 0;
        int result = Utilities.indexOfInstance(list, clazz);
        assertEquals(expResult, result);   
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of expand method, of class Utilities.
     */
    public void testExpand()
    {
        System.out.println("expand");
        String s = "";
        int l = 0;
        String es = "";
        String expResult = "";
        String result = Utilities.expand(s, l, es);
        assertEquals(expResult, result);   
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getObjectRefIndex method, of class Utilities.
     */
    public void testGetObjectRefIndex()
    {
        System.out.println("getObjectRefIndex");
        Object o = null;
        List<? extends Object> array = null;
        int expResult = 0;
        int result = Utilities.getObjectRefIndex(o, array);
        assertEquals(expResult, result);   
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of indexOfStringIgnoreCase method, of class Utilities.
     */
    public void testIndexOfStringIgnoreCase()
    {
        System.out.println("indexOfStringIgnoreCase");
        List<? extends Object> list = null;
        String str = "";
        int expResult = 0;
        int result = Utilities.indexOfStringIgnoreCase(list, str);
        assertEquals(expResult, result);   
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of fontAsString method, of class Utilities.
     */
    public void testFontAsString()
    {
        System.out.println("fontAsString");
        Font font = null;
        String expResult = "";
        String result = Utilities.fontAsString(font);
        assertEquals(expResult, result);   
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getLocation method, of class Utilities.
     */
    @Test
    public void testGetLocation()
    {
        System.out.println("getLocation");
        Class<?> c = Utilities.class;
        URL result = Utilities.getLocation(c);
        System.out.println("result=" + result);
        // TODO review the generated test code and remove the default call to fail.
        // fail("The test case is a prototype.");
    }

    /**
     * Test of urlToFile method, of class Utilities.
     */
    public void testUrlToFile_URL()
    {
        System.out.println("urlToFile");
        URL url = null;
        File expResult = null;
        File result = Utilities.urlToFile(url);
        assertEquals(expResult, result);   
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of urlToFile method, of class Utilities.
     */
    public void testUrlToFile_String()
    {
        System.out.println("urlToFile");
        String url = "";
        File expResult = null;
        File result = Utilities.urlToFile(url);
        assertEquals(expResult, result);   
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

}

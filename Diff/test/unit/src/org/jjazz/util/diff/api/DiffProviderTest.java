/*
 * To change this template, choose Tools | Templates
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
package org.jjazz.util.diff.api;

import java.util.Comparator;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.openide.util.Lookup;

/**
 * !!! Need to set DEBUG = true in DiffProvider.java !!!
 */
public class DiffProviderTest
{

    DiffProvider dp = Lookup.getDefault().lookup(DiffProvider.class);
    String[] a;
    String[] b;

    public DiffProviderTest()
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
     * Test of diff method, of class DiffProvider.
     */
    @Test
    public void test1()
    {
        System.out.println("\nTEST 1");
        a = new String[]
        {
            "A", "B", "C"
        };
        b = new String[]
        {
            "A", "C", "A"
        };
        dp.diff(a, b);
        // assertEquals(expResult, result);
        // fail("The test case is a prototype.");
    }

    @Test
    public void test2()
    {
        System.out.println("\nTEST 2");
        a = new String[]
        {
            "A", "B", "C", "D"
        };
        b = new String[]
        {
            "A", "X", "B", "C"
        };
        dp.diff(a, b);
        // assertEquals(expResult, result);
        // fail("The test case is a prototype.");
    }

}

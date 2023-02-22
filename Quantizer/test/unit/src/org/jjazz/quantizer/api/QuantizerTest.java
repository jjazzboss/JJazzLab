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
package org.jjazz.quantizer.api;

import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import static org.junit.Assert.assertEquals;
import org.junit.*;

public class QuantizerTest
{

    private final Quantizer instance;
    private int maxBarIndex;
    private Position pos;
    private TimeSignature ts;

    public QuantizerTest()
    {
        instance = Quantizer.getInstance();
        instance.setQuantizationValue(Quantization.HALF_BEAT);
        instance.setIterativeQuantizeEnabled(false);
    }

    @BeforeClass
    public static void setUpClass() throws Exception
    {
    }

    @AfterClass
    public static void tearDownClass() throws Exception
    {
    }

    @Before
    public void setUp()
    {
        pos = new Position(0, 0);
        ts = TimeSignature.FOUR_FOUR;
        maxBarIndex = 100;
    }

    @After
    public void tearDown()
    {
    }

    @Test
    public void testQuantizePosition0()
    {
        System.out.println("quantizePosition 0");
        pos.setBeat(0);
        Position expResult = new Position(0, 0);
        Position result = instance.getQuantized(pos, ts, maxBarIndex);
        assertEquals(expResult, result);   
    }

    @Test
    public void testQuantizePosition0bis()
    {
        System.out.println("quantizePosition 0.2");
        pos.setBeat(0.2f);
        Position expResult = new Position(0, 0f);
        Position result = instance.getQuantized(pos, ts, maxBarIndex);
        assertEquals(expResult, result);   
    }

    @Test
    public void testQuantizePosition1()
    {
        System.out.println("quantizePosition 0.25");
        pos.setBeat(0.25f);
        Position expResult = new Position(0, 0.5f);
        Position result = instance.getQuantized(pos, ts, maxBarIndex);
        assertEquals(expResult, result);   
    }

    @Test
    public void testQuantizePosition2()
    {
        System.out.println("quantizePosition 0.4");
        pos.setBeat(0.4f);
        Position expResult = new Position(0, 0.5f);
        Position result = instance.getQuantized(pos, ts, maxBarIndex);
        assertEquals(expResult, result);   
    }

    @Test
    public void testQuantizePosition3()
    {
        System.out.println("quantizePosition 0.5");
        pos.setBeat(0.5f);
        Position expResult = new Position(0, 0.5f);
        Position result = instance.getQuantized(pos, ts, maxBarIndex);
        assertEquals(expResult, result);   
    }

    @Test
    public void testQuantizePosition4()
    {
        System.out.println("quantizePosition 0.7f");
        pos.setBeat(0.7f);
        Position expResult = new Position(0, 0.5f);
        Position result = instance.getQuantized(pos, ts, maxBarIndex);
        assertEquals(expResult, result);   
    }

    @Test
    public void testQuantizePosition5()
    {
        System.out.println("quantizePosition 1f");
        pos.setBeat(1f);
        Position expResult = new Position(0, 1f);
        Position result = instance.getQuantized(pos, ts, maxBarIndex);
        assertEquals(expResult, result);   
    }

    @Test
    public void testQuantizePosition6()
    {
        System.out.println("quantizePosition 1.1f");
        pos.setBeat(1.1f);
        Position expResult = new Position(0, 1f);
        Position result = instance.getQuantized(pos, ts, maxBarIndex);
        assertEquals(expResult, result);   
    }

    @Test
    public void testQuantizePosition7()
    {
        System.out.println("quantizePosition 1.25f");
        pos.setBeat(1.25f);
        Position expResult = new Position(0, 1.5f);
        Position result = instance.getQuantized(pos, ts, maxBarIndex);
        assertEquals(expResult, result);   
    }

    @Test
    public void testQuantizePosition8()
    {
        System.out.println("quantizePosition 1.3f");
        pos.setBeat(1.3f);
        Position expResult = new Position(0, 1.5f);
        Position result = instance.getQuantized(pos, ts, maxBarIndex);
        assertEquals(expResult, result);   
    }

    @Test
    public void testQuantizePosition9()
    {
        System.out.println("quantizePosition 1.5f");
        pos.setBeat(1.5f);
        Position expResult = new Position(0, 1.5f);
        Position result = instance.getQuantized(pos, ts, maxBarIndex);
        assertEquals(expResult, result);   
    }

    @Test
    public void testQuantizePosition10()
    {
        System.out.println("quantizePosition 1.7f");
        pos.setBeat(1.7f);
        Position expResult = new Position(0, 1.5f);
        Position result = instance.getQuantized(pos, ts, maxBarIndex);
        assertEquals(expResult, result);   
    }

    @Test
    public void testQuantizePosition11()
    {
        System.out.println("quantizePosition 1.8f");
        pos.setBeat(1.8f);
        Position expResult = new Position(0, 2f);
        Position result = instance.getQuantized(pos, ts, maxBarIndex);
        assertEquals(expResult, result);   
    }
}

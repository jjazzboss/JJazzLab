/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 * This file is part of the JJazzLab-X software.
 *
 * JJazzLab-X is free software: you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License (LGPLv3) 
 * as published by the Free Software Foundation, either version 3 of the License, 
 * or (at your option) any later version.
 *
 * JJazzLab-X is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JJazzLab-X.  If not, see <https://www.gnu.org/licenses/>
 *
 * Contributor(s): 
 *
 */
package org.jjazz.midiconverters;

import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import org.jjazz.midi.Instrument;
import org.jjazz.midi.InstrumentBank;
import org.jjazz.midi.MidiSynth;
import org.jjazz.midi.synths.GM1Bank;
import org.jjazz.midi.synths.GM2Bank;
import org.jjazz.midi.synths.GSBank;
import org.jjazz.midi.synths.GSSynth;
import org.jjazz.midi.synths.StdSynth;
import org.jjazz.midi.synths.XGBank;
import org.jjazz.midiconverters.api.ConversionTable;
import org.jjazz.midiconverters.spi.InstrumentConverter;
import org.openide.util.lookup.ServiceProvider;

/**
 * Conversion between GM/GS/GM2/XG sounds.
 */
@ServiceProvider(service = InstrumentConverter.class)
public class StdInstrumentConverter implements InstrumentConverter
{

    static private ConversionTable CONVERSION_TABLE_GM2_GS;
    private static final Logger LOGGER = Logger.getLogger(StdInstrumentConverter.class.getSimpleName());

    @Override
    public String getConverterId()
    {
        return "Standard Instrument Converter";
    }

    /**
     * Accept only instruments from/to the standard banks GM/GM2/XG/GS.
     *
     * @return
     */
    @Override
    public Instrument convertInstrument(Instrument srcIns, MidiSynth destSynth, List<InstrumentBank<?>> destBanks)
    {
        if (srcIns == null || destSynth == null)
        {
            throw new IllegalArgumentException("srcIns=" + srcIns + " destSynth=" + destSynth + " destBanks=" + destBanks);
        }
        InstrumentBank<?> srcBank = srcIns.getBank();
        if (srcBank == null || srcBank.getMidiSynth() == null
                || (srcBank.getMidiSynth() != StdSynth.getInstance() && srcBank.getMidiSynth() != GSSynth.getInstance())
                || (destSynth != StdSynth.getInstance() && destSynth != GSSynth.getInstance()))
        {
            return null;
        }
        if (destBanks == null)
        {
            destBanks = destSynth.getBanks();
        }
        if (destBanks.isEmpty())
        {
            return null;
        }
        GM1Bank gm1Bank = StdSynth.getInstance().getGM1Bank();
        GM2Bank gm2Bank = StdSynth.getInstance().getGM2Bank();
        XGBank xgBank = StdSynth.getInstance().getXGBank();
        GSBank gsBank = GSSynth.getInstance().getGSBank();

        Instrument ins = null;
        if (srcBank == gm1Bank)
        {
            // GM => GM2/XG/GS
            ins = srcIns;
        } else if (srcBank == gm2Bank)
        {
            // GM2 => GS/XG/GM 
            if (destBanks.contains(gsBank))
            {
                // GS
                ins = getConvertedInstrument(gm2Bank, gsBank, srcIns);
            } else if (destBanks.contains(xgBank))
            {
                // XG
                ins = getConvertedInstrument(gm2Bank, xgBank, srcIns);
            } else if (destBanks.contains(gm1Bank))
            {
                // GM
                ins = gm1Bank.getInstrument(srcIns.getMidiAddress().getProgramChange());
            }
        } else if (srcBank == gsBank)
        {
            // GS => GM2/XG/GM
            if (destBanks.contains(gsBank))
            {
                // GM2
                ins = getConvertedInstrument(gsBank, gm2Bank, srcIns);
            } else if (destBanks.contains(xgBank))
            {
                // XG
                ins = getConvertedInstrument(gsBank, xgBank, srcIns);
            } else if (destBanks.contains(gm1Bank))
            {
                // GM
                ins = gm1Bank.getInstrument(srcIns.getMidiAddress().getProgramChange());
            }
        }
        return ins;
    }

    // =====================================================================================
    // Private methods
    // =====================================================================================   
    private Instrument getConvertedInstrument(InstrumentBank<?> bankFrom, InstrumentBank<?> bankTo, Instrument srcIns)
    {
        Instrument ins = null;
        HashMap<Integer, Integer> map = getConversionMap(bankFrom, bankTo);
        if (map == null)
        {
            LOGGER.warning("getConvertedInstrument() " + bankFrom.getName() + "=>" + bankTo.getName() + " - no conversion map found.");
            return null;
        }
        int srcIndex = bankFrom.getIndex(srcIns);
        int destIndex = map.get(srcIndex);
        if (destIndex >= 0 && destIndex < bankTo.getSize())
        {
            ins = bankTo.getInstrument(destIndex);
        } else
        {
            LOGGER.warning("getConvertedInstrument() " + bankFrom.getName() + "=>" + bankTo.getName() + " - Invalid destIndex=" + destIndex + ".  srcIns=" + srcIns.toLongString() + " srcIndex=" + srcIndex);
        }
        return ins;
    }

    private HashMap<Integer, Integer> getConversionMap(InstrumentBank<?> bankFrom, InstrumentBank<?> bankTo)
    {
        if ((bankFrom == StdSynth.getInstance().getGM2Bank() && bankTo == GSSynth.getInstance().getGSBank()))
        {
            if (CONVERSION_TABLE_GM2_GS == null)
            {
                CONVERSION_TABLE_GM2_GS = new ConversionTable("GM2_to_GS", getMapGM2_to_GS());
            }
            return CONVERSION_TABLE_GM2_GS.getMapFromTo();
        }
        if ((bankFrom == GSSynth.getInstance().getGSBank() && bankTo == StdSynth.getInstance().getGM2Bank()))
        {
            if (CONVERSION_TABLE_GM2_GS == null)
            {
                CONVERSION_TABLE_GM2_GS = new ConversionTable("GM2_to_GS", getMapGM2_to_GS());
            }
            return CONVERSION_TABLE_GM2_GS.getMapToFrom();
        }
        return null;
    }

    private HashMap<Integer, Integer> getMapGM2_to_GS()
    {
        HashMap<Integer, Integer> map = new HashMap<>((int) (256 / 0.7f));      // Avoid rehash, see HashMap javadoc
        map.put(0, 0);
        map.put(1, 1);
        map.put(2, 2);
        map.put(3, 3);
        map.put(4, 4);
        map.put(5, 5);
        map.put(6, 6);
        map.put(7, 7);
        map.put(8, 8);
        map.put(9, 9);
        map.put(10, 10);
        map.put(11, 11);
        map.put(12, 12);
        map.put(13, 13);
        map.put(14, 14);
        map.put(15, 15);
        map.put(16, 16);
        map.put(17, 16);
        map.put(18, 17);
        map.put(19, 18);
        map.put(20, 19);
        map.put(21, 19);
        map.put(22, 20);
        map.put(23, 20);
        map.put(24, 21);
        map.put(25, 22);
        map.put(26, 23);
        map.put(27, 24);
        map.put(28, 25);
        map.put(29, 26);
        map.put(30, 27);
        map.put(31, 28);
        map.put(32, 29);
        map.put(33, 30);
        map.put(34, 31);
        map.put(35, 32);
        map.put(36, 33);
        map.put(37, 34);
        map.put(38, 35);
        map.put(39, 36);
        map.put(40, 37);
        map.put(41, 38);
        map.put(42, 39);
        map.put(43, 40);
        map.put(44, 41);
        map.put(45, 42);
        map.put(46, 43);
        map.put(47, 44);
        map.put(48, 44);
        map.put(49, 45);
        map.put(50, 46);
        map.put(51, 47);
        map.put(52, 48);
        map.put(53, 49);
        map.put(54, 50);
        map.put(55, 51);
        map.put(56, 52);
        map.put(57, 53);
        map.put(58, 54);
        map.put(59, 55);
        map.put(60, 55);
        map.put(61, 56);
        map.put(62, 57);
        map.put(63, 58);
        map.put(64, 59);
        map.put(65, 59);
        map.put(66, 60);
        map.put(67, 61);
        map.put(68, 62);
        map.put(69, 63);
        map.put(70, 63);
        map.put(71, 63);
        map.put(72, 64);
        map.put(73, 65);
        map.put(74, 65);
        map.put(75, 66);
        map.put(76, 67);
        map.put(77, 68);
        map.put(78, 69);
        map.put(79, 69);
        map.put(80, 70);
        map.put(81, 71);
        map.put(82, 72);
        map.put(83, 73);
        map.put(84, 74);
        map.put(85, 75);
        map.put(86, 76);
        map.put(87, 77);
        map.put(88, 78);
        map.put(89, 78);
        map.put(90, 78);
        map.put(91, 79);
        map.put(92, 79);
        map.put(93, 80);
        map.put(94, 81);
        map.put(95, 82);
        map.put(96, 83);
        map.put(97, 84);
        map.put(98, 85);
        map.put(99, 86);
        map.put(100, 87);
        map.put(101, 87);
        map.put(102, 88);
        map.put(103, 89);
        map.put(104, 90);
        map.put(105, 90);
        map.put(106, 91);
        map.put(107, 92);
        map.put(108, 93);
        map.put(109, 94);
        map.put(110, 95);
        map.put(111, 96);
        map.put(112, 97);
        map.put(113, 97);
        map.put(114, 98);
        map.put(115, 98);
        map.put(116, 99);
        map.put(117, 99);
        map.put(118, 99);
        map.put(119, 99);
        map.put(120, 100);
        map.put(121, 100);
        map.put(122, 101);
        map.put(123, 102);
        map.put(124, 102);
        map.put(125, 103);
        map.put(126, 104);
        map.put(127, 104);
        map.put(128, 105);
        map.put(129, 106);
        map.put(130, 107);
        map.put(131, 108);
        map.put(132, 109);
        map.put(133, 110);
        map.put(134, 111);
        map.put(135, 112);
        map.put(136, 113);
        map.put(137, 114);
        map.put(138, 114);
        map.put(139, 115);
        map.put(140, 116);
        map.put(141, 117);
        map.put(142, 118);
        map.put(143, 119);
        map.put(144, 120);
        map.put(145, 121);
        map.put(146, 122);
        map.put(147, 123);
        map.put(148, 124);
        map.put(149, 125);
        map.put(150, 126);
        map.put(151, 127);
        map.put(152, 128);
        map.put(153, 129);
        map.put(154, 130);
        map.put(155, 131);
        map.put(156, 132);
        map.put(157, 133);
        map.put(158, 134);
        map.put(159, 135);
        map.put(160, 136);
        map.put(161, 136);
        map.put(162, 136);
        map.put(163, 137);
        map.put(164, 138);
        map.put(165, 139);
        map.put(166, 139);
        map.put(167, 140);
        map.put(168, 141);
        map.put(169, 142);
        map.put(170, 142);
        map.put(171, 143);
        map.put(172, 144);
        map.put(173, 144);
        map.put(174, 145);
        map.put(175, 146);
        map.put(176, 146);
        map.put(177, 147);
        map.put(178, 148);
        map.put(179, 149);
        map.put(180, 150);
        map.put(181, 151);
        map.put(182, 152);
        map.put(183, 153);
        map.put(184, 154);
        map.put(185, 155);
        map.put(186, 156);
        map.put(187, 157);
        map.put(188, 158);
        map.put(189, 159);
        map.put(190, 160);
        map.put(191, 161);
        map.put(192, 162);
        map.put(193, 163);
        map.put(194, 164);
        map.put(195, 165);
        map.put(196, 166);
        map.put(197, 167);
        map.put(198, 168);
        map.put(199, 169);
        map.put(200, 170);
        map.put(201, 171);
        map.put(202, 172);
        map.put(203, 173);
        map.put(204, 174);
        map.put(205, 175);
        map.put(206, 176);
        map.put(207, 177);
        map.put(208, 178);
        map.put(209, 179);
        map.put(210, 180);
        map.put(211, 181);
        map.put(212, 182);
        map.put(213, 183);
        map.put(214, 184);
        map.put(215, 185);
        map.put(216, 186);
        map.put(217, 187);
        map.put(218, 188);
        map.put(219, 189);
        map.put(220, 190);
        map.put(221, 191);
        map.put(222, 192);
        map.put(223, 193);
        map.put(224, 194);
        map.put(225, 194);
        map.put(226, 195);
        map.put(227, 196);
        map.put(228, 197);
        map.put(229, 198);
        map.put(230, 199);
        map.put(231, 200);
        map.put(232, 201);
        map.put(233, 202);
        map.put(234, 203);
        map.put(235, 195);
        map.put(236, 204);
        map.put(237, 205);
        map.put(238, 206);
        map.put(239, 207);
        map.put(240, 208);
        map.put(241, 212);
        map.put(242, 209);
        map.put(243, 210);
        map.put(244, 211);
        map.put(245, 212);
        map.put(246, 213);
        map.put(247, 214);
        map.put(248, 215);
        map.put(249, 216);
        map.put(250, 217);
        map.put(251, 209);
        map.put(252, 218);
        map.put(253, 219);
        map.put(254, 220);
        map.put(255, 221);
        return map;
    }
}

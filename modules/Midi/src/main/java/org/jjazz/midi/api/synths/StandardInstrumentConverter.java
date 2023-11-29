/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 * This file is part of the JJazzLab software.
 *
 * JJazzLab is free software: you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License (LGPLv3) 
 * as published by the Free Software Foundation, either version 3 of the License, 
 * or (at your option) any later version.
 *
 * JJazzLab is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *
 * Contributor(s): 
 *
 */
package org.jjazz.midi.api.synths;

import com.google.common.base.Preconditions;
import java.util.List;
import java.util.logging.Logger;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.midi.api.Instrument;
import org.jjazz.midi.api.InstrumentBank;
import org.jjazz.midi.api.MidiSynth;
import org.jjazz.midi.api.keymap.KeyMapGM;
import org.jjazz.midi.api.keymap.KeyMapGSGM2;
import org.jjazz.midi.api.keymap.KeyMapXG_PopLatin;
import org.jjazz.midi.api.keymap.KeyMapXG;


/**
 * Conversion methods between GM/GS/GM2/XG standard instruments.
 */
public class StandardInstrumentConverter
{

    static private ConversionTable CONVERSION_TABLE_GM2_TO_GS;
    static private ConversionTable CONVERSION_TABLE_XG_TO_GM2;
    private static final Logger LOGGER = Logger.getLogger(StandardInstrumentConverter.class.getSimpleName());


    /**
     * Try to find the standard Instrument from destSynth which best matches srcIns.
     * <p>
     *
     * @param srcIns A standard instrument from a GM/GM2/XG/GS-compatible MidiSynth
     * @param destSynth Should be GM/GM2/XG/GS-compatible
     * @return A GM/GM2/XG/GS standard instrument from destSynth. Null if destSynth is not GM/GM2/XG/GS-compatible, or if srcIns
     * is not a standard instrument.
     */
    static public Instrument convertInstrument(Instrument srcIns, MidiSynth destSynth)
    {
        Preconditions.checkNotNull(srcIns);
        Preconditions.checkNotNull(destSynth);


        var srcInsAddr = srcIns.getMidiAddress();
        var srcInsBank = srcIns.getBank();
        var srcInsSynth = srcInsBank != null ? srcInsBank.getMidiSynth() : null;


        if (srcInsSynth == null)
        {
            return null;
        }


        var xgIns = XGSynth.getInstance().getInstrument(srcInsAddr);
        if (srcInsSynth.isXGcompatible() && xgIns != null)
        {
            // ins is a XG bank sound (not necessarily from XGSynth)        
            if (destSynth.isXGcompatible())
            {
                // XG => XG
                return destSynth.getInstrument(srcInsAddr);
            } else if (destSynth.isGM2compatible())
            {
                // Convert XG to GM2
                int xgIndex = XGSynth.getInstance().getXGBank().getIndex(xgIns);
                int index = StandardInstrumentConverter.getXGtoGM2Map().convert(xgIndex);
                return XGSynth.getInstance().getXGBank().getInstrument(index);
            } else if (destSynth.isGScompatible())
            {
                // Convert XG to GS
                int xgIndex = XGSynth.getInstance().getXGBank().getIndex(xgIns);
                int gm2Index = StandardInstrumentConverter.getXGtoGM2Map().convert(xgIndex);     // We don't have a direct GS<>XG map
                int index = StandardInstrumentConverter.getGM2toGSMap().convert(gm2Index);
                return XGSynth.getInstance().getXGBank().getInstrument(index);
            } else if (destSynth.isGMcompatible())
            {
                // Convert XG to GM
                GMSynth.getInstance().getGM1Bank().getInstrument(srcInsAddr.getProgramChange());
            }
        }

        var gm2ins = GM2Synth.getInstance().getInstrument(srcInsAddr);
        if (srcInsSynth.isGM2compatible() && gm2ins != null)
        {
            // ins is a GM2 bank sound (not necessarily from GM2Synth)       
            if (destSynth.isGM2compatible())
            {
                // GM2 to GM2
                return destSynth.getInstrument(srcInsAddr);
            } else if (destSynth.isXGcompatible())
            {
                // Convert GM2 to XG
                int gm2Index = GM2Synth.getInstance().getGM2Bank().getIndex(gm2ins);
                int index = StandardInstrumentConverter.getXGtoGM2Map().reverseConvert(gm2Index);
                return XGSynth.getInstance().getXGBank().getInstrument(index);
            } else if (destSynth.isGScompatible())
            {
                // Convert GM2 to GS
                int gm2Index = GM2Synth.getInstance().getGM2Bank().getIndex(gm2ins);
                int index = StandardInstrumentConverter.getGM2toGSMap().convert(gm2Index);
                return GSSynth.getInstance().getGSBank().getInstrument(index);
            } else if (destSynth.isGMcompatible())
            {
                // Convert GM2 to GM
                GMSynth.getInstance().getGM1Bank().getInstrument(srcInsAddr.getProgramChange());
            }
        }


        var gsIns = GSSynth.getInstance().getInstrument(srcInsAddr);
        if (srcInsSynth.isGScompatible() && gsIns != null)
        {
            // ins is a GS bank sound (not necessarily from GSSynth)       
            if (destSynth.isGScompatible())
            {
                // GS to GS
                return destSynth.getInstrument(srcInsAddr);
            } else if (destSynth.isXGcompatible())
            {
                // Convert GS to XG
                int gsIndex = GSSynth.getInstance().getGSBank().getIndex(gsIns);
                int gm2Index = getGM2toGSMap().reverseConvert(gsIndex);     // We don't have a direct GS<>XG map
                int index = getXGtoGM2Map().reverseConvert(gm2Index);
                return XGSynth.getInstance().getXGBank().getInstrument(index);
            } else if (destSynth.isGScompatible())
            {
                // Convert GS to GM2
                int gsIndex = GSSynth.getInstance().getGSBank().getIndex(gsIns);
                int index = getGM2toGSMap().reverseConvert(gsIndex);
                return GM2Synth.getInstance().getGM2Bank().getInstrument(index);
            } else if (destSynth.isGMcompatible())
            {
                // Convert GS to GM
                GMSynth.getInstance().getGM1Bank().getInstrument(srcInsAddr.getProgramChange());
            }
        }


        if (srcInsSynth.isGM1BankMidiAddress(srcInsAddr) && destSynth.isGMcompatible())        // Note that if this MidiSynth is XG/GM2/GS, it is also GM compatible
        {
            // ins is a GM bank sound (but not necessarily from GMSynth)                   
            return destSynth.getInstrument(destSynth.getGM1BankMidiAddress(srcInsAddr.getProgramChange()));
        }


        return null;

    }


    /**
     * Try to find a GM2/XG/GS/drums percussion instrument from midiSynth which match, as much as possible, srcKit.
     * <p>
     *
     * @param srcKit
     * @param midiSynth Can't be null. Should be GM2/XG/GS compatible.
     * @param tryHarder If initial search did not yield any instrument, try again with a more flexible matching scheme.
     * @return Can be null.
     */
    public static Instrument findDrumsInstrument(DrumKit srcKit, MidiSynth midiSynth, boolean tryHarder)
    {
        if (srcKit == null || midiSynth == null)
        {
            throw new IllegalArgumentException("srcKit=" + srcKit + " midiSynth=" + midiSynth + " tryHarder=" + tryHarder);   
        }

        Instrument res = null;
        DrumKit.KeyMap srcKeyMap = srcKit.getKeyMap();

        DrumKit.KeyMap xgLatinKeyMap = KeyMapXG_PopLatin.getInstance();
        DrumKit.KeyMap xgKeyMap = KeyMapXG.getInstance();
        DrumKit.KeyMap gsgm2KeyMap = KeyMapGSGM2.getInstance();
        DrumKit.KeyMap gmKeyMap = KeyMapGM.getInstance();

        if (!srcKeyMap.isContaining(xgKeyMap)
                && !srcKeyMap.isContaining(xgLatinKeyMap)
                && !srcKeyMap.isContaining(gsgm2KeyMap)
                && !srcKeyMap.isContaining(gmKeyMap))
        {
            // srcIns uses a non standard KeyMap : no possible conversion here
            return null;
        }

        XGBank xgBank = XGSynth.getInstance().getXGBank();
        GM2Bank gm2Bank = GM2Synth.getInstance().getGM2Bank();
        GSBank gsBank = GSSynth.getInstance().getGSBank();
        boolean isDestXG = midiSynth.isXGcompatible();
        boolean isDestGM2 = midiSynth.isGM2compatible();
        boolean isDestGS = midiSynth.isGScompatible();

        if (!isDestXG && !isDestGM2 && !isDestGS)
        {
            // No possible compatibility: there is no drums instrument in GM1
            return null;
        }

        if (isDestXG && (srcKeyMap.isContaining(xgLatinKeyMap) || srcKeyMap.isContaining(xgKeyMap) || srcKeyMap.isContaining(gmKeyMap)))
        {
            res = getDrumsInstrument(xgBank, srcKit, tryHarder);
        } else if (isDestGM2 && (srcKeyMap.isContaining(gsgm2KeyMap) || srcKeyMap.isContaining(gmKeyMap)))
        {
            res = getDrumsInstrument(gm2Bank, srcKit, tryHarder);
        } else if (isDestGS && (srcKeyMap.isContaining(gsgm2KeyMap) || srcKeyMap.isContaining(gmKeyMap)))
        {
            res = getDrumsInstrument(gsBank, srcKit, tryHarder);
        }

        // Get the equivalent instrument in midiSynth
        return res == null ? null : midiSynth.getInstrument(res.getMidiAddress());
    }

    // =====================================================================================
    // Private methods
    // =====================================================================================     

    private static ConversionTable getGM2toGSMap()
    {
        if (CONVERSION_TABLE_GM2_TO_GS == null)
        {
            CONVERSION_TABLE_GM2_TO_GS = new ConversionTable("GM2_to_GS", MAP_GM2_TO_GS);
        }
        return CONVERSION_TABLE_GM2_TO_GS;
    }

    private static ConversionTable getXGtoGM2Map()
    {
        if (CONVERSION_TABLE_XG_TO_GM2 == null)
        {
            CONVERSION_TABLE_XG_TO_GM2 = new XgToGM2ConversionTable();
        }
        return CONVERSION_TABLE_XG_TO_GM2;
    }


    static private Instrument getDrumsInstrument(InstrumentBank<Instrument> bank, DrumKit kit, boolean tryHarder)
    {
        List<Instrument> res = bank.getDrumsInstruments(kit, tryHarder);
        return res.isEmpty() ? null : res.get(0);
    }


    /**
     * Overridden to fix the reverse GM2=>XG drumkit conversion
     * <p>
     */
    static class XgToGM2ConversionTable extends ConversionTable
    {

        /**
         * For GM2=>XG drumkit conversion (GM2index=256-264)
         */
        static private final int[] MAP_GM2_TO_XG_DRUMKIT =
        {
            480, 488, 490, 492, 493, 500, 502, 504, 516
        };

        public XgToGM2ConversionTable()
        {
            super("XG_to_GM2", MAP_XG_TO_GM2);
        }

        /**
         * Overridden to fix GM2=>XG drumkit conversion (GM2index=256-264)
         *
         * @param bankToIndex
         * @return
         */
        @Override
        public int reverseConvert(int bankToIndex)
        {
            int bankFromIndex;
            if (bankToIndex < 256)
            {
                // We can use the normal map
                bankFromIndex = super.reverseConvert(bankToIndex);
            } else
            {
                // The normal map can not be used in reverse mode for drum kits in reverse mode 
                // because a single GM2 drumkit is often used by several very different XG drums kits
                bankFromIndex = MAP_GM2_TO_XG_DRUMKIT[bankToIndex - 256];
            }
            return bankFromIndex;
        }
    }


    /**
     * A bidirectional conversion table between instrument indexes of 2 banks, bankFrom and bankTo.
     */
    static class ConversionTable
    {

        private String id;
        private int[] map;
        private int[] reverseMap;

        /**
         * Create a ConversionTable.
         * <p>
         * bankFrom size must be &gt;= bankTo size.
         *
         * @param id For debugging message, e.g. "GM2=>GS"
         * @param map map[bankFromIndex] = bankToIndex. bankToIndex must not be &gt; (bankFrom.size()-1);
         */
        public ConversionTable(String id, int[] map)
        {
            if (id == null || id.isEmpty() || map == null || map.length == 0)
            {
                throw new IllegalArgumentException("id=" + id + " map=" + map);   
            }
            this.map = map;
        }

        /**
         * Get the index in bankTo corresponding to the specified index in bankFrom.
         * <p>
         * E.g. for a "GM2=>GS" table, get the GS index corresponding to a GM2 index.
         *
         * @param bankFromIndex
         * @return
         */
        public int convert(int bankFromIndex)
        {
            return map[bankFromIndex];
        }

        /**
         * Get the index in bankFrom corresponding to the specified index in bankTo.
         * <p>
         * E.g. for a "GM2=>GS" table, get the GM2 index corresponding to a GS index.
         *
         * @param bankToIndex
         * @return
         */
        public int reverseConvert(int bankToIndex)
        {
            if (reverseMap == null)
            {
                // First use, build the reverse table
                reverseMap = new int[map.length];  // might be too large but not a problem
                int lastBankToIdx = -1;
                for (int bankFromIdx = 0; bankFromIdx < map.length; bankFromIdx++)
                {
                    int bankToIdx = map[bankFromIdx];
                    if (bankToIdx != lastBankToIdx)
                    {
                        // Save only the first bankFromIndex if several ones are mapped to the same bankToIndex
                        reverseMap[bankToIdx] = bankFromIdx;
                    }
                    lastBankToIdx = bankToIdx;
                }
            }
            return reverseMap[bankToIndex];
        }

        @Override
        public String toString()
        {
            return id;
        }
    }


    //===========================================================
    // Conversion tables
    //===========================================================
    static private final int[] MAP_GM2_TO_GS =
    {
        0,
        1,
        2,
        3,
        4,
        5,
        6,
        7,
        8,
        9,
        10,
        11,
        12,
        13,
        14,
        15,
        16,
        16,
        17,
        18,
        19,
        19,
        20,
        20,
        21,
        22,
        23,
        24,
        25,
        26,
        27,
        28,
        29,
        30,
        31,
        32,
        33,
        34,
        35,
        36,
        37,
        38,
        39,
        40,
        41,
        42,
        43,
        44,
        44,
        45,
        46,
        47,
        48,
        49,
        50,
        51,
        52,
        53,
        54,
        55,
        55,
        56,
        57,
        58,
        59,
        59,
        60,
        61,
        62,
        63,
        63,
        63,
        64,
        65,
        65,
        66,
        67,
        68,
        69,
        69,
        70,
        71,
        72,
        73,
        74,
        75,
        76,
        77,
        78,
        78,
        78,
        79,
        79,
        80,
        81,
        82,
        83,
        84,
        85,
        86,
        87,
        87,
        88,
        89,
        90,
        90,
        91,
        92,
        93,
        94,
        95,
        96,
        97,
        97,
        98,
        98,
        99,
        99,
        99,
        99,
        100,
        100,
        101,
        102,
        102,
        103,
        104,
        104,
        105,
        106,
        107,
        108,
        109,
        110,
        111,
        112,
        113,
        114,
        114,
        115,
        116,
        117,
        118,
        119,
        120,
        121,
        122,
        123,
        124,
        125,
        126,
        127,
        128,
        129,
        130,
        131,
        132,
        133,
        134,
        135,
        136,
        136,
        136,
        137,
        138,
        139,
        139,
        140,
        141,
        142,
        142,
        143,
        144,
        144,
        145,
        146,
        146,
        147,
        148,
        149,
        150,
        151,
        152,
        153,
        154,
        155,
        156,
        157,
        158,
        159,
        160,
        161,
        162,
        163,
        164,
        165,
        166,
        167,
        168,
        169,
        170,
        171,
        172,
        173,
        174,
        175,
        176,
        177,
        178,
        179,
        180,
        181,
        182,
        183,
        184,
        185,
        186,
        187,
        188,
        189,
        190,
        191,
        192,
        193,
        194,
        194,
        195,
        196,
        197,
        198,
        199,
        200,
        201,
        202,
        203,
        195,
        204,
        205,
        206,
        207,
        208,
        212,
        209,
        210,
        211,
        212,
        213,
        214,
        215,
        216,
        217,
        209,
        218,
        219,
        220,
        221,
        222, // Standard Kit DRUMS
        223,
        224,
        225,
        226,
        227,
        228,
        229,
        230         // SFX Kit DRUMS
    };

    static private final int[] MAP_XG_TO_GM2 =
    {
        0,
        1,
        2,
        2,
        2,
        3,
        4,
        5,
        6,
        6,
        6,
        6,
        7,
        8,
        9,
        9,
        10,
        11,
        12,
        12,
        12,
        13,
        13,
        14,
        15,
        16,
        17,
        17,
        17,
        17,
        18,
        19,
        20,
        21,
        22,
        22,
        22,
        23,
        23,
        24,
        25,
        26,
        26,
        27,
        28,
        28,
        29,
        30,
        30,
        30,
        30,
        31,
        32,
        33,
        34,
        35,
        35,
        35,
        35,
        36,
        37,
        38,
        38,
        39,
        40,
        40,
        40,
        40,
        40,
        40,
        41,
        41,
        42,
        42,
        42,
        42,
        42,
        43,
        43,
        43,
        43,
        44,
        45,
        46,
        46,
        46,
        46,
        47,
        48,
        49,
        50,
        51,
        51,
        52,
        52,
        53,
        55,
        56,
        56,
        54,
        57,
        57,
        58,
        58,
        60,
        59,
        61,
        62,
        61,
        63,
        64,
        65,
        66,
        67,
        68,
        69,
        70,
        71,
        72,
        73,
        74,
        75,
        76,
        75,
        77,
        77,
        77,
        78,
        78,
        78,
        78,
        79,
        79,
        79,
        80,
        80,
        81,
        81,
        81,
        81,
        81,
        81,
        82,
        82,
        82,
        83,
        83,
        84,
        85,
        85,
        86,
        87,
        88,
        88,
        88,
        91,
        91,
        89,
        89,
        89,
        90,
        90,
        90,
        92,
        92,
        92,
        93,
        94,
        95,
        96,
        97,
        98,
        98,
        98,
        99,
        100,
        101,
        102,
        103,
        103,
        103,
        103,
        103,
        104,
        104,
        104,
        104,
        104,
        106,
        106,
        106,
        106,
        106,
        108,
        107,
        107,
        107,
        107,
        109,
        110,
        111,
        111,
        111,
        111,
        112,
        113,
        114,
        114,
        115,
        116,
        117,
        118,
        120,
        121,
        121,
        121,
        122,
        123,
        125,
        125,
        126,
        128,
        129,
        129,
        129,
        130,
        131,
        132,
        133,
        133,
        133,
        133,
        133,
        134,
        134,
        135,
        135,
        135,
        136,
        136,
        137,
        137,
        138,
        138,
        139,
        140,
        141,
        140,
        141,
        141,
        141,
        141,
        142,
        143,
        144,
        145,
        146,
        147,
        148,
        149,
        150,
        151,
        152,
        153,
        154,
        155,
        156,
        156,
        156,
        156,
        156,
        157,
        157,
        158,
        159,
        160,
        161,
        162,
        162,
        162,
        162,
        162,
        162,
        162,
        162,
        163,
        163,
        164,
        164,
        165,
        165,
        166,
        167,
        167,
        167,
        167,
        168,
        169,
        170,
        170,
        170,
        171,
        171,
        172,
        172,
        172,
        173,
        173,
        173,
        174,
        174,
        174,
        174,
        174,
        175,
        175,
        176,
        176,
        177,
        177,
        177,
        178,
        178,
        178,
        179,
        180,
        180,
        180,
        180,
        180,
        181,
        181,
        181,
        181,
        181,
        182,
        182,
        182,
        183,
        183,
        183,
        183,
        183,
        183,
        183,
        183,
        184,
        184,
        184,
        184,
        184,
        184,
        184,
        184,
        184,
        185,
        185,
        185,
        185,
        185,
        185,
        185,
        185,
        186,
        186,
        186,
        187,
        187,
        187,
        187,
        187,
        187,
        187,
        187,
        187,
        188,
        188,
        190,
        189,
        189,
        189,
        189,
        191,
        191,
        191,
        191,
        192,
        192,
        193,
        193,
        193,
        194,
        194,
        194,
        194,
        194,
        195,
        196,
        197,
        197,
        198,
        199,
        200,
        201,
        201,
        201,
        201,
        202,
        202,
        202,
        202,
        202,
        202,
        202,
        203,
        204,
        202,
        202,
        205,
        206,
        207,
        208,
        209,
        210,
        209,
        210,
        211,
        212,
        213,
        214,
        215,
        218,
        220,
        220,
        220,
        236,
        246,
        252,
        216,
        216,
        217,
        219,
        219,
        222,
        223,
        224,
        225,
        226,
        227,
        228,
        229,
        229,
        229,
        231,
        232,
        233,
        233,
        234,
        235,
        235,
        237,
        238,
        239,
        240,
        241,
        242,
        243,
        244,
        245,
        243,
        244,
        247,
        248,
        249,
        250,
        251,
        253,
        254,
        255,
        255,
        256, // Standard Drums Kit
        256,
        256,
        256,
        256,
        256,
        256,
        256,
        257,
        257,
        258,
        258,
        259,
        260,
        260,
        260,
        260,
        260,
        260,
        260,
        261,
        261,
        262,
        262,
        263,
        259,
        259,
        259,
        259,
        259,
        256, // FROM HERE (XG Index=510, XG Live Drum Kit Series starts), because single GM2 kits are reused by several very different XG drums kits. ReverseMap won't be good.
        256,
        262,
        256,
        256,
        262,
        264,
        264,
        259,
        259,
        259,
        256,
        256,
        256,
        256,
        256,
        256,
        256,
        256
    };

}

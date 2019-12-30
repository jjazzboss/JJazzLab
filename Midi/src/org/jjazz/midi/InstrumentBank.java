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
package org.jjazz.midi;

import java.util.ArrayList;
import java.util.List;

/**
 * A set of Instruments grouped in a bank.
 * <p>
 * It must be guaranteed that Instruments have a unique patch name in the bank.
 *
 * @param <T>
 */
public interface InstrumentBank<T extends Instrument>
{

    public static class Util
    {

        /**
         * Find an instrument in the bank which matches the specified parameters.
         *
         * @param bank
         * @param programChange
         * @param bankMSB
         * @param bankLSB
         * @return Null if not found.
         */
        public Instrument findInstrument(InstrumentBank<? extends Instrument> bank, int programChange, int bankMSB, int bankLSB)
        {
            Instrument res = null;
            for (Instrument ins : bank.getInstruments())
            {
                if (ins.getProgramChange() == programChange && ins.getBankSelectLSB() == bankLSB && ins.getBankSelectMSB() == bankMSB)
                {
                    res = ins;
                    break;
                }
            }
            return res;
        }

        public List<DrumKit.KeyMap> getKeyMaps(InstrumentBank<? extends Instrument> bank)
        {
            ArrayList<DrumKit.KeyMap> res = new ArrayList<>();
            for (Instrument ins : bank.getDrumsInstruments())
            {
                if (ins.getDrumKit() != null)
                {
                    res.add(ins.getDrumKit().getKeyMap());
                }
            }
            return res;
        }

        public List<DrumKit.Type> getTypes(InstrumentBank<? extends Instrument> bank)
        {
            ArrayList<DrumKit.Type> res = new ArrayList<>();
            for (Instrument ins : bank.getDrumsInstruments())
            {
                if (ins.getDrumKit() != null)
                {
                    res.add(ins.getDrumKit().getType());
                }
            }
            return res;
        }
    }

    public enum BankSelectMethod
    {
        MSB_LSB, MSB_ONLY, LSB_ONLY, PC_ONLY
    };

    BankSelectMethod getBankSelectMethod();

    /**
     * The MidiSynth this bank belongs to.
     *
     * @return
     */
    MidiSynth getMidiSynth();

    /**
     * Set the MidiSynth this bank belongs to. Can be called only once.
     *
     * @param synth A non-null value.
     */
    void setMidiSynth(MidiSynth synth);

    /**
     * Find the instruments whose patchName contains specified text (ignoring case).
     *
     * @param text
     * @return
     */
    List<T> findInstruments(String text);

    /**
     * The default BankSelect MSB (Midi control #0).
     * <p>
     * Note that individual instruments belonging to this bank can have a different BankSelect MSB.
     *
     * @return Bank Select Most Significant Byte (MIdi control #0)
     */
    int getDefaultBankSelectMSB();

    /**
     * The default BankSelect LSB (Midi control #32).
     * <p>
     * Note that individual instruments belonging to this bank can have a different BankSelect LSB.
     *
     * @return Bank Select Most Significant Byte (Midi control #32)
     */
    int getDefaultBankSelectLSB();

    /**
     * Get the instrument whose patchName matches (ignoring case) the specified name.
     *
     * @param patchName
     * @return null if not found
     */
    T getInstrument(String patchName);

    /**
     * Get the instrument which is specified index in the bank.
     *
     * @param index
     * @return
     */
    T getInstrument(int index);

    /**
     * Get the instrument which has the specified Program Change.
     *
     * @param progChange Change value [0-127]
     * @return Null if not found.
     */
    T getInstrumentFromPC(int progChange);

    /**
     * Get all the Drums/Percussion instruments.
     *
     * @return All the Instruments which have a DrumKit defined.
     */
    List<T> getDrumsInstruments();

    /**
     * Get all the instruments of the bank.
     *
     * @return
     */
    List<T> getInstruments();

    /**
     * The name of the bank.
     *
     * @return
     */
    String getName();

    /**
     * The next instrument in the database after the specified instrument. Return the 1st element of the database if ins is the
     * last element.
     *
     * @param ins
     * @return
     */
    T getNextInstrument(Instrument ins);

    /**
     * The previous instrument in the database after the specified instrument. Return the 1st element of the database if ins is
     * the last element.
     *
     * @param ins
     * @return
     */
    T getPreviousInstrument(Instrument ins);

    /**
     * The number of instruments in the bank.
     *
     * @return
     */
    int getSize();

}

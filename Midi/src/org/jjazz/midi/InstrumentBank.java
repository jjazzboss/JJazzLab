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
import org.jjazz.midi.MidiAddress.BankSelectMethod;

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
         * Get all the DrumKit.KeyMaps used by this bank.
         *
         * @param bank
         * @return
         */
        public List<DrumKit.KeyMap> getKeyMaps(InstrumentBank<? extends Instrument> bank)
        {
            ArrayList<DrumKit.KeyMap> res = new ArrayList<>();
            for (Instrument ins : bank.getDrumsInstruments())
            {
                if (ins.isDrumKit())
                {
                    res.add(ins.getDrumKit().getKeyMap());
                }
            }
            return res;
        }

        /**
         * Get all the DrumKit.Types used by this bank.
         *
         * @param bank
         * @return
         */

        public List<DrumKit.Type> getTypes(InstrumentBank<? extends Instrument> bank)
        {
            ArrayList<DrumKit.Type> res = new ArrayList<>();
            for (Instrument ins : bank.getDrumsInstruments())
            {
                if (ins.isDrumKit())
                {
                    res.add(ins.getDrumKit().getType());
                }
            }
            return res;
        }
    }

    /**
     * The MidiSynth this bank belongs to.
     *
     * @return
     */
    MidiSynth getMidiSynth();

    /**
     * Set the MidiSynth this bank belongs to.
     * <p>
     * Can be called only once.
     *
     * @param synth A non-null value.
     */
    void setMidiSynth(MidiSynth synth);

    /**
     * The default BankSelect method.
     * <p>
     * Note that individual instruments belonging to this bank can have a different BankSelect method.
     *
     * @return Can't be null.
     */
    BankSelectMethod getDefaultBankSelectMethod();

    /**
     * The default BankSelect MSB (Midi control #0).
     * <p>
     * Note that individual instruments belonging to this bank can have a different BankSelect MSB.
     *
     * @return [0;127] Bank Select Most Significant Byte (MIdi control #0).
     */
    int getDefaultBankSelectMSB();

    /**
     * The default BankSelect LSB (Midi control #32).
     * <p>
     * Note that individual instruments belonging to this bank can have a different BankSelect LSB.
     *
     * @return [0;127] Bank Select Most Significant Byte (Midi control #32)
     */
    int getDefaultBankSelectLSB();

    /**
     * Get all the instruments of the bank.
     *
     * @return
     */
    List<T> getInstruments();

    /**
     * Get the instrument which is specified index in the bank.
     *
     * @param index
     * @return
     */
    T getInstrument(int index);

    /**
     * Get all the Drums/Percussion instruments.
     *
     * @return Returned instruments must have isDrumKit() set to true.
     */
    List<T> getDrumsInstruments();

    /**
     * Get the instrument whose patchName matches (ignoring case) the specified name.
     *
     * @param patchName
     * @return null if not found
     */
    T getInstrument(String patchName);

    /**
     * Get the instrument at the specified MidiAddress.
     *
     * @param address
     * @return null if not found
     */
    T getInstrument(MidiAddress address);

    /**
     * Find the instruments whose patchName contains specified text (ignoring case).
     *
     * @param text
     * @return
     */
    List<T> findInstruments(String text);

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

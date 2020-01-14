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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * A MidiSynth is a collection of InstrumentBanks.
 * <p>
 */
public class MidiSynth
{

    private File file;
    ArrayList<InstrumentBank<?>> banks = new ArrayList<>();
    private String name;
    private String manufacturer;
    private static final Logger LOGGER = Logger.getLogger(MidiSynth.class.getSimpleName());

    public MidiSynth(String name, String manufacturer)
    {
        if (name == null || name.trim().isEmpty() || manufacturer == null)
        {
            throw new IllegalArgumentException("name=" + name + " manufacturer=" + manufacturer);
        }
        this.name = name;
        this.manufacturer = manufacturer;
    }

    /**
     * Add a bank to this MidiSynth.
     * <p>
     * Set the MidiSynth of bank to this object.
     *
     * @param bank
     */
    public void addBank(InstrumentBank<?> bank)
    {
        if (!banks.contains(bank))
        {
            bank.setMidiSynth(this);
            banks.add(bank);
        }
    }

    public List<InstrumentBank<?>> getBanks()
    {
        return new ArrayList<>(banks);
    }

    /**
     * Find the bank whose name matches bankName (ignoring case).
     *
     * @param bankName
     * @return Null if not found
     */
    public InstrumentBank<?> getBank(String bankName)
    {
        for (InstrumentBank<?> bank : banks)
        {
            if (bank.getName().equalsIgnoreCase(bankName))
            {
                return bank;
            }
        }
        return null;
    }

    /**
     * Get all the instruments from this MidiSynth.
     *
     * @return
     */
    public List<Instrument> getInstruments()
    {
        ArrayList<Instrument> res = new ArrayList<>();
        for (InstrumentBank<?> bank : banks)
        {
            res.addAll(bank.getInstruments());
        }
        return res;
    }

    /**
     * Find instruments in this object's banks which match the given string (ignoring case).
     *
     * @param text
     * @return
     */
    public List<Instrument> findInstruments(String text)
    {
        ArrayList<Instrument> res = new ArrayList<>();
        for (InstrumentBank<?> bank : getBanks())
        {
            res.addAll(bank.findInstruments(text));
        }
        return res;
    }

    /**
     * Find an instrument with the specified patchName.
     *
     * @param patchName
     * @return Null if instrument not found in the MidiSynth banks.
     */
    public Instrument getInstrument(String patchName)
    {
        for (InstrumentBank<?> bank : getBanks())
        {
            Instrument ins = bank.getInstrument(patchName);
            if (ins != null)
            {
                return ins;
            }
        }
        return null;
    }

    /**
     * Find an instrument with the specified address.
     *
     * @param address
     * @return Null if instrument not found in the MidiSynth banks.
     */
    public Instrument getInstrument(MidiAddress address)
    {
        for (InstrumentBank<?> bank : getBanks())
        {
            Instrument ins = bank.getInstrument(address);
            if (ins != null)
            {
                return ins;
            }
        }
        return null;
    }

    /**
     * Get the percentage of MidiAddresses from the specified bank which match an instrument in this MidiSynth.
     *
     * @param bank
     * @return A value between 0 and 1.
     */
    public float getMidiAddressMatchingCoverage(InstrumentBank<?> bank)
    {
        if (bank == null)
        {
            throw new NullPointerException("bank");
        }
        float count = 0;
        int nbInstruments = 0;
        for (Instrument ins : bank.getInstruments())
        {
            if (getInstrument(ins.getMidiAddress()) != null)
            {
                count++;
            }
            nbInstruments++;
        }
        return (nbInstruments == 0) ? 0 : count / nbInstruments;
    }
//
//    /**
//     * Get the banks from this MidiSynth which are considered as compatible with the banks of the specified synth's banks.
//     * <p>
//     *
//     * @param synth
//     * @param threshold If the result of getMidiAddressMatchingCoverage() is &gt;= threshold for bank X, then bank X is considered
//     *                  as compatible.
//     * @return A list of banks from this MidiSynth
//     * @see MidiSynth#getMidiAddressMatchingCoverage(InstrumentBank)
//     */
//    public List<InstrumentBank<?>> getCompatibleBanks(MidiSynth synth, float threshold)
//    {
//        if (synth == null || threshold < 0 || threshold > 1)
//        {
//            throw new IllegalArgumentException("synth=" + synth + " threshold=" + threshold);
//        }
//        if (synth == this)
//        {
//            return getBanks();
//        }
//        ArrayList<InstrumentBank<?>> res = new ArrayList<>();
//        for (InstrumentBank<?> bank : getBanks())
//        {
//            if (synth.getMidiAddressMatchingCoverage(bank) >= threshold)
//            {
//                res.add(bank);
//            }
//        }
//        return res;
//    }

    /**
     * The total number of patches in the banks of this synth.
     *
     * @return
     */
    public int getNbPatches()
    {
        int size = 0;
        for (InstrumentBank<?> bank : getBanks())
        {
            size += bank.getSize();
        }
        return size;
    }

    public void setFile(File f)
    {
        file = f;
    }

    public File getFile()
    {
        return file;
    }

    /**
     * @return the name
     */
    public String getName()
    {
        return name;
    }

    /**
     * @return the manufacturer
     */
    public String getManufacturer()
    {
        return manufacturer;
    }

    @Override
    public String toString()
    {
        return getName();
    }

    public void dump()
    {
        LOGGER.severe("DUMP synth: " + this.name + "(" + getNbPatches() + ") ================================================");
        for (InstrumentBank<?> bank : getBanks())
        {
            LOGGER.severe("   Bank=" + bank.getName() + " (" + bank.getSize() + ") ---------");
            for (Instrument ins : bank.getInstruments())
            {
                LOGGER.severe(ins.toLongString() + ", " + ins.getMidiAddress());
            }
        }
    }
}

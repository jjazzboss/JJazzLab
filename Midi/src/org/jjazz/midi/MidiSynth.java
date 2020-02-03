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
import org.openide.util.Lookup;

/**
 * A MidiSynth is a collection of InstrumentBanks.
 * <p>
 */
public class MidiSynth
{

    /**
     * Required by the MidiSynth serialization process : an implementation must be available in the global lookup.
     */
    public interface Finder
    {

        static public class Utilities
        {

            /**
             * Find the first Finder implementation in the global lookup.
             *
             * @return
             */
            static public Finder getDefault()
            {
                Finder finder = Lookup.getDefault().lookup(Finder.class);
                if (finder == null)
                {
                    throw new IllegalStateException("Can't find a MidiSynth.Finder instance in the global lookup");
                }
                return finder;
            }

        }

        /**
         * Search for a MidiSynth instance from the specified name and file name.
         *
         * @param synthName The MidiSynth name containing the bank. Can't be null.
         * @param synthFile The file associated to synthName. Can be null if no file. If synthFile has no parent directory, search the
         *                  default directory for output synth config files.
         * @return Null if no MidiSynth found
         */
        MidiSynth getMidiSynth(String synthName, File synthFile);

    }

    private File file;
    ArrayList<InstrumentBank<?>> banks = new ArrayList<>();
    private String name;
    private String manufacturer;
    private static final Logger LOGGER = Logger.getLogger(MidiSynth.class.getSimpleName());

    /**
     * Create an empty MidiSynth.
     *
     * @param name         If name contains comas (',') they are removed.
     * @param manufacturer
     */
    public MidiSynth(String name, String manufacturer)
    {
        if (name == null || name.trim().isEmpty() || manufacturer == null)
        {
            throw new IllegalArgumentException("name=" + name + " manufacturer=" + manufacturer);
        }
        this.name = name.replaceAll(",", "");
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
     * Get all the Drums/Percussion instruments from this MidiSynth.
     *
     * @return Returned instruments have isDrumKit() set to true.
     */
    public List<Instrument> getDrumsInstruments()
    {
        ArrayList<Instrument> res = new ArrayList<>();
        for (InstrumentBank<?> bank : banks)
        {
            res.addAll(bank.getDrumsInstruments());
        }
        return res;
    }

    /**
     * Get all the non Drums/Percussion instruments from this MidiSynth.
     *
     * @return Returned instruments have isDrumKit() set to false.
     */
    public List<Instrument> getNonDrumsInstruments()
    {
        ArrayList<Instrument> res = new ArrayList<>();
        for (InstrumentBank<?> bank : banks)
        {
            res.addAll(bank.getNonDrumsInstruments());
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

    /**
     * The total number of patches in the banks of this synth.
     *
     * @return
     */
    public int getNbInstruments()
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
        LOGGER.severe("DUMP synth: " + this.name + "(" + getNbInstruments() + ") ================================================");
        for (InstrumentBank<?> bank : getBanks())
        {
            LOGGER.severe("   Bank=" + bank.getName() + " (" + bank.getSize() + ") ---------");
            for (Instrument ins : bank.getInstruments())
            {
                LOGGER.severe(ins.toLongString() + ", " + ins.getMidiAddress());
            }
        }
    }

    /**
     * Save this MidiSynth as a string so that it can be retrieved by loadFromString().
     * <p>
     *
     * @return A string "Name, FilePath". FilePath can be "NOT_SET" if no file associated. If
     * @see loadFromString(String)
     */
    public String saveAsString()
    {
        LOGGER.fine("saveAsString() MidiSynth=" + getName() + ", getFile()=" + getFile());
        String strFile = getFile() == null ? "NOT_SET" : getFile().getAbsolutePath();
        return getName() + "#:#" + strFile;
    }

    /**
     * Get the MidiSynth corresponding to the string produced by saveAsString().
     * <p>
     *
     * @param s
     * @return Null if no MidiSynth could be found corresponding to s.
     * @see saveAsString()
     *
     */
    static public MidiSynth loadFromString(String s)
    {
        if (s == null)
        {
            throw new NullPointerException("s");
        }
        String[] strs = s.split("#:#");
        if (strs.length != 2)
        {
            LOGGER.warning("loadFromString() Invalid string format : " + s);
            return null;
        }
        String synthName = strs[0].trim();
        String filePath = strs[1].trim();
        File f = filePath.equals("NOT_SET") ? null : new File(filePath);
        MidiSynth synth = Finder.Utilities.getDefault().getMidiSynth(synthName, f);
        return synth;
    }

}

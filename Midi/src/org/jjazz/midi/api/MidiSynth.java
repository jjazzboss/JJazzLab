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
package org.jjazz.midi.api;

import com.google.common.base.Preconditions;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.openide.util.Lookup;

/**
 * A MidiSynth is a collection of InstrumentBanks.
 * <p>
 * You can optionnaly specify if this MidiSynth is GM/GM2/XG/GS compatible, and indicate the base MidiAddress of the GM bank.
 */
public class MidiSynth
{

    /**
     * Required by the MidiSynth serialization process : an implementation must be available in the global lookup.
     */
    public interface Finder
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
                throw new IllegalStateException("Can't find a MidiSynth.Finder instance in the global lookup");   //NOI18N
            }
            return finder;
        }


        /**
         * Search for a MidiSynth instance from the specified name and file name.
         *
         * @param synthName The MidiSynth name containing the bank. Can't be null.
         * @param synthFile The file associated to synthName. Can be null if no file. If synthFile has no parent directory, search
         *                  the default directory for output synth config files.
         * @return Null if no MidiSynth found
         */
        MidiSynth getMidiSynth(String synthName, File synthFile);

    }

    private final static MidiAddress DEFAULT_GM_BANK_ADDRESS = new MidiAddress(0, 0, 0, MidiAddress.BankSelectMethod.MSB_LSB);
    private File file;
    ArrayList<InstrumentBank<?>> banks = new ArrayList<>();
    private String name;
    private String manufacturer;
    private boolean isGMcompatible, isGM2compatible, isXGcompatible, isGScompatible;
    private MidiAddress gmBankBaseMidiAddress;
    private static final Logger LOGGER = Logger.getLogger(MidiSynth.class.getSimpleName());

    /**
     * Create an empty MidiSynth.
     * <p>
     * Created MidiSynth is not set to be compatible with GM/GM2/XG/GS standard.
     *
     * @param name         If name contains comas (',') they are removed.
     * @param manufacturer
     */
    public MidiSynth(String name, String manufacturer)
    {
        if (name == null || name.trim().isEmpty() || manufacturer == null)
        {
            throw new IllegalArgumentException("name=" + name + " manufacturer=" + manufacturer);   //NOI18N
        }
        this.name = name.replaceAll(",", "");
        this.manufacturer = manufacturer;
        this.gmBankBaseMidiAddress = DEFAULT_GM_BANK_ADDRESS;
    }

    /**
     * Set the compatibility of this MidiSynth with the Midi standards.
     * <p>
     * No check is performed on the actual Instruments of this MidiSynth to control the validity of this compatibility.
     * <p>
     * The following are automatically enforced: <br>
     * - If a synth is GM2/XG/GS compatible, then it is also GM compatible and GM bank base adress is set to MSB=LSB=0<br>
     * - If a synth is GS compatible, then it can't be GM2 nor XG compatible
     *
     * @param isGMcompatible  If null parameter is ignored.
     * @param isGM2compatible If null parameter is ignored.
     * @param isXGcompatible  If null parameter is ignored.
     * @param isGScompatible  If null parameter is ignored.
     */
    public void setCompatibility(Boolean isGMcompatible, Boolean isGM2compatible, Boolean isXGcompatible, Boolean isGScompatible)
    {
        if (isGMcompatible != null)
        {
            this.isGMcompatible = isGMcompatible;
        }
        if (isGM2compatible != null)
        {
            this.isGM2compatible = isGM2compatible;
        }
        if (isXGcompatible != null)
        {
            this.isXGcompatible = isXGcompatible;
        }
        if (isGScompatible != null)
        {
            this.isGScompatible = isGScompatible;
        }

        // Consistency checks
        if (this.isGScompatible)
        {
            this.isGM2compatible = false;
            this.isXGcompatible = false;
        }
        if (this.isGM2compatible || this.isXGcompatible || this.isGScompatible)
        {
            this.isGMcompatible = true;
            this.gmBankBaseMidiAddress = DEFAULT_GM_BANK_ADDRESS;
        }

    }


    public boolean isGMcompatible()
    {
        return isGMcompatible;
    }

    public boolean isGM2compatible()
    {
        return isGM2compatible;
    }

    public boolean isXGcompatible()
    {
        return isXGcompatible;
    }

    public boolean isGScompatible()
    {
        return isGScompatible;
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
     * Find the instrument with the specified address.
     * <p>
     *
     * @param addr
     * @return Null if instrument not found in the MidiSynth banks.
     */
    public final Instrument getInstrument(MidiAddress addr)
    {
        for (InstrumentBank<?> bank : getBanks())
        {
            Instrument ins = bank.getInstrument(addr);
            if (ins != null)
            {
                return ins;
            }
        }
        return null;
    }

    /**
     * Set the base MidiAddress used to directly access the first instrument (Program Change=0) of the GM bank of this MidiSynth.
     * <p>
     * <p>
     * GM standard does not define a GM bank select messages. The "old" way to access the GM sounds is to first send a Sysex
     * message "Set GM Mode ON" then a Program Change message. But as most synths now have many banks, it's usually possible to
     * directly access the GM sounds using bank select messages. This method lets you specify the GM bank select mechanism used by
     * this synth.
     * <p>
     * Examples:<br>
     * - On most Yamaha synths the GM bank can be directly accessed using LSB=0 and MSB=0.<br>
     * - On Roland JV-1080 the base GM bank address is MSB=81, LSB=3.<br>
     * <p>
     * Note that GM2/XG/GS compatible instruments are also GM-compatible and expect the GM bank to be at MSB=0 LSB=0.
     *
     * @param ma Must have Program Change==0.
     * @see #getGM1BankBaseMidiAddress()
     * @see #setCompatibility(java.lang.Boolean, java.lang.Boolean, java.lang.Boolean, java.lang.Boolean)
     */
    public void setGM1BankBaseMidiAddress(MidiAddress ma)
    {
        Preconditions.checkArgument(ma != null && ma.getProgramChange() == 0);
        gmBankBaseMidiAddress = ma;

        // Check consistency
        if ((isGM2compatible || isXGcompatible || isGScompatible) && (ma.getBankMSB() > 0 || ma.getBankLSB() > 0))
        {
            throw new IllegalStateException("Can't have a GM2/XG/GS compatible MidiSynth with a GM bank base address which is not MSB=LSB=0, this=" + this + " ma=" + ma);
        }
    }

    /**
     * Get the MidiAddress to be used to directly access the first instrument (piano) of the GM bank of this MidiSynth. 
     * <p>
     * IMPORTANT: value is meaningless if this MidiSynth is not GM-compatible.
     * <p>
     * This method is required because synths can have a GM bank anywhere, eg the JV-1080 synth has its GM Bank Midi address at MSB=83, LSB=3.
     
     *
     * @return Can't be null. If not explicitly set, return by default new MidiAddress(0, 0, 0,
     *         MidiAddress.BankSelectMethod.MSB_LSB).
     * @see #setGM1BankBaseMidiAddress(MidiAddress)
     */
    public MidiAddress getGM1BankBaseMidiAddress()
    {
        return gmBankBaseMidiAddress;
    }


    /**
     * Get the MidiAddress of a GM bank instrument.
     *
     * @param programChange Program change of the instrument
     * @return Null if this MidiSynth is not GM-compatible
     */
    public MidiAddress getGM1BankMidiAddress(int programChange)
    {
        return isGMcompatible ? new MidiAddress(programChange, gmBankBaseMidiAddress.getBankMSB(), gmBankBaseMidiAddress.getBankLSB(), gmBankBaseMidiAddress.getBankSelectMethod())
                : null;
    }

    /**
     * Return true if this MidiSynth is GM-compatible and the specified MidiAddress is part of the this synth'GM bank.
     *
     * @param addr
     * @return
     */
    public boolean isGM1BankMidiAddress(MidiAddress addr)
    {
        return isGMcompatible
                && addr.getBankMSB() == gmBankBaseMidiAddress.getBankMSB()
                && addr.getBankLSB() == gmBankBaseMidiAddress.getBankLSB()
                && addr.getBankSelectMethod().equals(gmBankBaseMidiAddress.getBankSelectMethod());
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
            throw new NullPointerException("bank");   //NOI18N
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
        LOGGER.severe("DUMP synth: " + this.name + "(" + getNbInstruments() + ") ================================================");   //NOI18N
        for (InstrumentBank<?> bank : getBanks())
        {
            LOGGER.severe("   Bank=" + bank.getName() + " (" + bank.getSize() + ") ---------");   //NOI18N
            for (Instrument ins : bank.getInstruments())
            {
                LOGGER.severe(ins.toLongString() + ", " + ins.getMidiAddress());   //NOI18N
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
        LOGGER.fine("saveAsString() MidiSynth=" + getName() + ", getFile()=" + getFile());   //NOI18N
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
            throw new NullPointerException("s");   //NOI18N
        }
        String[] strs = s.split("#:#");
        if (strs.length != 2)
        {
            LOGGER.warning("loadFromString() Invalid string format : " + s);   //NOI18N
            return null;
        }
        String synthName = strs[0].trim();
        String filePath = strs[1].trim();
        File f = filePath.equals("NOT_SET") ? null : new File(filePath);
        MidiSynth synth = Finder.getDefault().getMidiSynth(synthName, f);
        return synth;
    }

}

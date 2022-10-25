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
import org.jjazz.midi.api.MidiAddress.BankSelectMethod;
import org.jjazz.midi.api.synths.GM2Synth;
import org.jjazz.midi.api.synths.GMSynth;
import org.jjazz.midi.api.synths.XGSynth;
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
        this.gmBankBaseMidiAddress = new MidiAddress(0, 0, 0, MidiAddress.BankSelectMethod.MSB_LSB);
    }

    /**
     * Set the compatibility of this MidiSynth with the Midi standards.
     * <p>
     * Note that no check is performed on the actual Instruments of this MidiSynth to control the validity of this compatibility.
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
    }

    /**
     * Set the base MidiAddress to be used to directly access the first instrument (Program Change=0) of the GM bank of this
     * MidiSynth.
     * <p>
     * NOTE: return value is meaningful ONLY if this MidiSynth is GM compatible.
     * <p>
     * GM standard does not define a GM bank select messages. The "old" way to access the GM sounds is to first send a Sysex
     * message "Set GM Mode ON" then a Program Change message. But as most synths now have many banks, it's usually possible to
     * directly access the GM sounds using bank select messages. This method lets you specify the GM bank select mechanism used by
     * this synth.
     * <p>
     * Examples:<br>
     * - On most Yamaha synths the GM bank can be directly accessed using LSB=0 and MSB=0.<br>
     * - On Roland JV-1080 the base GM bank address is MSB=81, LSB=3.<br>
     *
     *
     * @param ma Must have Program Change==0.
     * @see #getGM1BankBaseMidiAddress()
     * @see #setCompatibility(java.lang.Boolean, java.lang.Boolean, java.lang.Boolean, java.lang.Boolean)
     */
    public void setGM1BankBaseMidiAddress(MidiAddress ma)
    {
        Preconditions.checkArgument(ma != null && ma.getProgramChange() == 0);
        gmBankBaseMidiAddress = ma;
    }

    /**
     * Get the MidiAddress to be used to directly access the first instrument (piano) of the GM bank of this MidiSynth (so without
     * using SysEx message "set GM Mode ON").
     *
     *
     * @return Can't be null. If not explicitly set, return by default new MidiAddress(0, 0, 0,
     *         MidiAddress.BankSelectMethod.MSB_LSB).
     * @see #setGM1BaseMidiAddress(MidiAddress)
     */
    public MidiAddress getGM1BankBaseMidiAddress()
    {
        return gmBankBaseMidiAddress;
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
    public Instrument getInstrument(MidiAddress addr)
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
     * Find the Instrument from this MidiSynth which best matches the specified instrument.
     * <p>
     * If ins'synth is defined, use its GM/GM2/XG/GS compatibility status + GM Bank base address to find the matching
     * instrument.<p>
     *
     * @param ins
     * @return Null if no match
     */
    public Instrument getMatchingInstrument(Instrument ins)
    {
        var insAddr = ins.getMidiAddress();
        var insBsm = insAddr.getBankSelectMethod();
        var insBank = ins.getBank();
        var insSynth = insBank != null ? insBank.getMidiSynth() : null;


        if (insSynth == null)
        {
            // Can't do much without compatibility information
            var addr = insAddr;


            // We assume that if insAddr is PC_ONLY or MSB_LSB with MSB=LSB=0, then insAddr represents a GM bank instrument address
            if (isGMcompatible
                    && (insBsm.equals(BankSelectMethod.PC_ONLY)
                    || (insBsm.equals(BankSelectMethod.MSB_LSB) && insAddr.getBankMSB() == 0 && insAddr.getBankLSB() == 0)))
            {
                // Translate the MidiAddress to get the instrument on the right bank (which may not be MSB=0 LSB=0, e.g. for the Roland JV-1080)
                var gmBaseAddr = getGM1BankBaseMidiAddress();
                addr = new MidiAddress(insAddr.getProgramChange(), gmBaseAddr.getBankMSB(), gmBaseAddr.getBankLSB(), gmBaseAddr.getBankSelectMethod());
            }

            return getInstrument(addr);
        }

        // insSynth is defined

        var gmIns = GMSynth.getInstance().getMatchingInstrument(ins);
        if (gmIns != null)
        {
            // ins is a GM bank sound (not necessarily from GMSynth)        
            if (!isGMcompatible)
            {
                return null;
            }
            // Use this MidiSynth' GM bank base address
            var addr = new MidiAddress(ins.getMidiAddress().getProgramChange(), gmBankBaseMidiAddress.getBankMSB(), gmBankBaseMidiAddress.getBankLSB(), gmBankBaseMidiAddress.getBankSelectMethod());
            return getInstrument(addr);
        }

        var xgIns = XGSynth.getInstance().getMatchingInstrument(ins);
        if (xgIns != null && isXGcompatible)
        {
            // ins is a XG bank sound (not necessarily from XGSynth)        
            return getInstrument(insAddr);
        }


        var gm2Ins = GM2Synth.getInstance().getMatchingInstrument(ins);
        if (gm2Ins != null && isGM2compatible)
        {
            // ins is a GM2 bank sound (not necessarily from GM2Synth)        
            return getInstrument(insAddr);
        }

        return getInstrument(ins.getMidiAddress());
    }
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

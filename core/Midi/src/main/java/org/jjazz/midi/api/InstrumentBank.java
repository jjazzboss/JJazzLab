/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 *  This file is part of the JJazzLab software.
 *   
 *  JJazzLab is free software: you can redistribute it and/or modify
 *  it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *  as published by the Free Software Foundation, either version 3 of the License, 
 *  or (at your option) any later version.
 *
 *  JJazzLab is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 * 
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 * 
 *  Contributor(s): 
 */
package org.jjazz.midi.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jjazz.midi.api.MidiAddress.BankSelectMethod;
import org.jjazz.midi.api.synths.InstrumentFamily;
import org.jjazz.midi.api.synths.GM1Instrument;

/**
 * A set of Instruments grouped in a bank.
 * <p>
 * It must be guaranteed that Instruments have a unique patch name in the bank.
 *
 * @param <T>
 */
public class InstrumentBank<T extends Instrument>
{

    protected HashMap<MidiAddress, T> mapAddressInstrument = new HashMap<>();
    protected ArrayList<T> instruments = new ArrayList<>();
    protected int defaultLsb, defaultMsb;
    protected BankSelectMethod defaultBsm;
    private MidiSynth synth;
    protected String name;
    private static final Logger LOGGER = Logger.getLogger(InstrumentBank.class.getSimpleName());

    /**
     * Create an empty bank with BankSelectMethod.MSB_LSB.
     *
     * @param name
     * @param msb
     * @param lsb
     */
    public InstrumentBank(String name, int msb, int lsb)
    {
        this(name, msb, lsb, BankSelectMethod.MSB_LSB);
    }

    /**
     * Create an InstrumentBank.
     *
     * @param name
     * @param msb The default Most Significant Byte or "Control 0".
     * @param lsb The default Least Significant Byte or "Control 32"
     * @param m The default bank select method. Can't be null.
     */
    public InstrumentBank(String name, int msb, int lsb, BankSelectMethod m)
    {
        if (name == null || name.trim().isEmpty() || msb < 0 || msb > 127 || lsb < 0 || lsb > 127 || m == null)
        {
            throw new IllegalArgumentException("name=" + name + " msb=" + msb + " lsb=" + lsb + " m=" + m);   
        }
        this.defaultLsb = lsb;
        this.defaultMsb = msb;
        this.name = name;
        defaultBsm = m;
    }

    /**
     * Associate a MidiSynth to this bank.
     * <p>
     * IMPORTANT: this method can be called only once (because a bank can't be assigned to 2 different MidiSynths).
     * <p>
     * It is the responsibility of the specified MidiSynth to add the bank.
     *
     * @param synth A non null value, the MidiSynth this InstrumentBank belongs to
     */
    public void setMidiSynth(MidiSynth synth)
    {
        if (this.synth != null)
        {
            throw new IllegalStateException("synth already set! this.synth=" + this.synth + " synth=" + synth);   
        }
        if (synth == null)
        {
            throw new IllegalArgumentException("synth=" + synth);   
        }
        this.synth = synth;
    }

    /**
     * The MidiSynth this bank belongs to.
     *
     * @return Can be null
     */
    public MidiSynth getMidiSynth()
    {
        return synth;
    }

    /**
     * The default BankSelect method.
     * <p>
     * Note that individual instruments belonging to this bank can have a different BankSelect method.
     *
     * @return Can't be null.
     */
    public BankSelectMethod getDefaultBankSelectMethod()
    {
        return defaultBsm;
    }

    /**
     * The default BankSelect MSB (Midi control #0).
     * <p>
     * Note that individual instruments belonging to this bank can have a different BankSelect MSB.
     *
     * @return [0;127] Bank Select Most Significant Byte (MIdi control #0).
     */
    public int getDefaultBankSelectMSB()
    {
        return defaultMsb;
    }

    /**
     * The default BankSelect LSB (Midi control #32).
     * <p>
     * Note that individual instruments belonging to this bank can have a different BankSelect LSB.
     *
     * @return [0;127] Bank Select Most Significant Byte (Midi control #32)
     */
    public int getDefaultBankSelectLSB()
    {
        return defaultLsb;
    }

    /**
     * Add the instrument to the bank.
     * <p>
     * The method sets the instrument's bank to this bank. It is the responsibility of the caller to check that instrument's
     * patchName is not used twice in the bank.
     *
     * @param instrument
     */
    public void addInstrument(T instrument)
    {
        if (instrument == null)
        {
            throw new IllegalArgumentException("instrument=" + instrument);   
        }
        if (!instruments.contains(instrument))
        {
            instrument.setBank(this);
            instruments.add(instrument);
            Instrument ins = mapAddressInstrument.get(instrument.getMidiAddress());
            if (ins != null)
            {
                throw new IllegalArgumentException("Instrument " + instrument + " conflicts with instrument already in the bank:" + ins);   
            } else
            {
                mapAddressInstrument.put(instrument.getMidiAddress(), instrument);
            }
        }
    }

    /**
     * Remove an Instrument from this bank.
     *
     * @param instrument
     */
    public void removeInstrument(T instrument)
    {
        if (instrument == null)
        {
            throw new IllegalArgumentException("instrument=" + instrument);   
        }
        instruments.remove(instrument);
        mapAddressInstrument.remove(instrument.getMidiAddress());
    }

    /**
     * Get all the DrumKit.KeyMaps used by this bank.
     *
     * @return
     */
    public List<DrumKit.KeyMap> getKeyMaps()
    {
        ArrayList<DrumKit.KeyMap> res = new ArrayList<>();
        for (Instrument ins : getDrumsInstruments())
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
     * @return
     */
    public List<DrumKit.Type> getTypes()
    {
        ArrayList<DrumKit.Type> res = new ArrayList<>();
        for (Instrument ins : getDrumsInstruments())
        {
            if (ins.isDrumKit())
            {
                res.add(ins.getDrumKit().getType());
            }
        }
        return res;
    }

    /**
     * Empty the bank.
     */
    public void clear()
    {
        instruments.clear();
        mapAddressInstrument.clear();
    }

    /**
     * The number of instruments in the bank.
     *
     * @return
     */
    public int getSize()
    {
        return instruments.size();
    }

    /**
     * The name of the bank.
     *
     * @return
     */
    public String getName()
    {
        return name;
    }

    /**
     * The index of the specified instrument.
     *
     * @param ins
     * @return -1 if not found
     */
    public int getIndex(Instrument ins)
    {
        return instruments.indexOf(ins);
    }

    /**
     * Get all the instruments of the bank.
     *
     * @return
     */
    public List<T> getInstruments()
    {
        ArrayList<T> res = new ArrayList<>(instruments);
        return res;
    }

    /**
     * The next instrument in the database after the specified instrument.
     * <p>
     * Return the 1st element of the database if ins is the last element.
     *
     * @param ins
     * @return
     */
    public T getNextInstrument(Instrument ins)
    {
        int index = instruments.indexOf(ins);
        if (index == -1)
        {
            throw new IllegalArgumentException("ins=" + ins);   
        }
        return instruments.get((index == instruments.size() - 1) ? 0 : index + 1);
    }

    /**
     * The previous instrument in the database after the specified instrument.
     * <p>
     * Return the 1st element of the database if ins is the last element.
     *
     * @param ins
     * @return
     */
    public T getPreviousInstrument(Instrument ins)
    {
        int index = instruments.indexOf(ins);
        if (index == -1)
        {
            throw new IllegalArgumentException("ins=" + ins);   
        }
        return instruments.get((index == 0) ? instruments.size() - 1 : index - 1);
    }

    /**
     * Get the instrument whose patchName matches (ignoring case) the specified name.
     *
     * @param patchName
     * @return null if not found
     */
    public T getInstrument(String patchName)
    {
        if (patchName == null)
        {
            throw new IllegalArgumentException("patchName=" + patchName);   
        }
        for (T i : instruments)
        {
            if (i.getPatchName().equalsIgnoreCase(patchName.trim()))
            {
                return i;
            }
        }
        return null;
    }

    /**
     * Get the instrument at the specified MidiAddress.
     *
     * @param address
     * @return null if not found
     */
    public T getInstrument(MidiAddress address)
    {
        return mapAddressInstrument.get(address);
    }

    /**
     * Get the instrument which is specified index in the bank.
     *
     * @param index
     * @return
     */
    public T getInstrument(int index)
    {
        if (index < 0 || index > instruments.size() - 1)
        {
            throw new IllegalArgumentException("index=" + index);   
        }
        return instruments.get(index);
    }


    /**
     * Get the non Drums/Percussion instruments.
     *
     * @return Returned instruments have isDrumKit() set to false.
     */
    public List<T> getNonDrumsInstruments()
    {
        ArrayList<T> res = new ArrayList<>();
        for (T ins : instruments)
        {
            if (!ins.isDrumKit())
            {
                res.add(ins);
            }
        }
        return res;
    }

    /**
     * Get all the Drums/Percussion instruments.
     *
     * @return Returned instruments have isDrumKit() set to true.
     */
    public List<T> getDrumsInstruments()
    {
        ArrayList<T> res = new ArrayList<>();
        for (T ins : instruments)
        {
            if (ins.isDrumKit())
            {
                res.add(ins);
            }
        }
        return res;
    }

    /**
     * Get all the drums/percussion instruments which match the specified DrumKit.
     *
     * @param kit
     * @param tryHarder If true and no instrument matched the specified kit, then try again but with a more flexible matching
     * algorithm. Default implementation starts a second search using kit.Type.STANDARD.
     * @return Can be empty.
     */
    public List<T> getDrumsInstruments(DrumKit kit, boolean tryHarder)
    {
        if (kit == null)
        {
            throw new NullPointerException("kit=" + kit + " tryHarder=" + tryHarder);   
        }
        ArrayList<T> res = new ArrayList<>();
        List<T> drumsInstruments = getDrumsInstruments();
        for (T ins : drumsInstruments)
        {
            if (kit.equals(ins.getDrumKit()))
            {
                res.add(ins);
            }
        }
        if (res.isEmpty() && tryHarder && !kit.getType().equals(DrumKit.Type.STANDARD))
        {
            DrumKit kit2 = new DrumKit(DrumKit.Type.STANDARD, kit.getKeyMap());
            for (T ins : drumsInstruments)
            {
                if (kit2.equals(ins.getDrumKit()))
                {
                    res.add(ins);
                }
            }
        }
        return res;
    }

    /**
     * Get the instruments whose substitute is sub.
     *
     * @param sub Can be null
     * @return
     */
    public List<T> getInstrumentsFromSubstitute(GM1Instrument sub)
    {
        ArrayList<T> res = new ArrayList<>();
        for (T ins : instruments)
        {
            if (ins.getSubstitute() == sub)
            {
                res.add(ins);
            }
        }
        return res;
    }

    /**
     * Get all the instruments which match the specified predicate.
     *
     * @param tester
     * @return
     */
    public List<Instrument> getInstruments(Predicate<Instrument> tester)
    {
        return instruments.stream()
                .filter(i -> tester.test(i))
                .collect(Collectors.toList());
    }

    /**
     * Get the instruments whose substitute's family is f.
     *
     * @param f Can't be null
     * @return
     */
    public List<T> getInstrumentsFromFamily(InstrumentFamily f)
    {
        if (f == null)
        {
            throw new IllegalArgumentException("f=" + f);   
        }
        ArrayList<T> res = new ArrayList<>();
        for (T ins : instruments)
        {
            if (ins.getSubstitute() != null && ins.getSubstitute().getFamily().equals(f))
            {
                res.add(ins);
            }
        }
        return res;
    }

    /**
     * Find the instruments whose patchName contains specified text (ignoring case).
     *
     * @param text
     * @return
     */
    public List<T> findInstruments(String text)
    {
        if (text == null || text.isEmpty())
        {
            throw new IllegalArgumentException("text=" + text);   
        }
        ArrayList<T> res = new ArrayList<>();
        for (T i : instruments)
        {
            if (i.getPatchName().toLowerCase().contains(text.toLowerCase()))
            {
                res.add(i);
            }
        }
        return res;
    }

    @Override
    public String toString()
    {
        return "InstrumentBank=" + getName() + "[" + instruments.size() + "]";
    }
}

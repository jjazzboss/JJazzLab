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
import java.util.logging.Logger;

/**
 * A base class for your InstrumentBanks.
 *
 * @param <T>
 */
public class AbstractInstrumentBank<T extends Instrument> implements InstrumentBank<T>
{

    protected ArrayList<T> instruments;
    protected int lsb, msb;
    private MidiSynth synth;
    protected BankSelectMethod bsm;
    protected String name;
    private static final Logger LOGGER = Logger.getLogger(AbstractInstrumentBank.class.getSimpleName());

    /**
     * Create an empty bank with BankSelectMethod.MSB_LSB.
     *
     * @param name
     */
    public AbstractInstrumentBank(String name, MidiSynth synth, int msb, int lsb)
    {
        this(name, synth, msb, lsb, BankSelectMethod.MSB_LSB);
    }

    /**
     *
     * @param name
     * @param synth The container for this bank. Can be null.
     * @param msb   The default Most Significant Byte or "Control 0"
     * @param lsb   The default Least Significant Byte or "Control 32"
     * @param m
     */
    public AbstractInstrumentBank(String name, MidiSynth synth, int msb, int lsb, BankSelectMethod m)
    {
        if (name == null || name.trim().isEmpty() || msb < 0 || msb > 127 || lsb < 0 || lsb > 127)
        {
            throw new IllegalArgumentException("name=" + name + " synth=" + synth + " msb=" + msb + " lsb=" + lsb);
        }
        instruments = new ArrayList<>();
        this.synth = synth;
        this.lsb = lsb;
        this.msb = msb;
        this.name = name;
        bsm = m;
    }

    /**
     * This function can be called only once.
     * <p>
     * It is the responsibility of the specified MidiSynth to add the bank.
     *
     * @param synth A non null value, the MidiSynth this InstrumentBank belongs to
     */
    @Override
    public void setMidiSynth(MidiSynth synth)
    {
        if (this.synth != null)
        {
            throw new IllegalStateException("synth already set!");
        }
        if (synth == null)
        {
            throw new IllegalArgumentException("synth=" + synth);
        }
        this.synth = synth;
    }

    @Override
    public MidiSynth getMidiSynth()
    {
        return synth;
    }

    /**
     * The BankSelectMethod.
     *
     * @return
     */
    @Override
    public BankSelectMethod getBankSelectMethod()
    {
        return bsm;
    }

    @Override
    public int getDefaultBankSelectMSB()
    {
        return msb;
    }

    @Override
    public int getDefaultBankSelectLSB()
    {
        return lsb;
    }

    /**
     * Add the instrument to the bank.
     * <p>
     * If there is already an Instrument with same patchname, nothing is done. The function sets the instrument's bank to this
     * bank.
     *
     * @param instrument
     */
    protected void addInstrument(T instrument)
    {
        if (instrument == null)
        {
            throw new IllegalArgumentException("instrument=" + instrument);
        }
        if (!instruments.contains(instrument))
        {
            instrument.setBank(this);
            instruments.add(instrument);
        }
    }

    protected void removeInstrument(T instrument)
    {
        if (instrument == null)
        {
            throw new IllegalArgumentException("instrument=" + instrument);
        }
        instruments.remove(instrument);
    }

    /**
     * Empty the bank.
     */
    protected void clear()
    {
        instruments.clear();
    }

    /**
     * The number of instruments in the bank.
     *
     * @return
     */
    @Override
    public int getSize()
    {
        return instruments.size();
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public List<T> getInstruments()
    {
        ArrayList<T> res = new ArrayList<>(instruments);
        return res;
    }

    /**
     * The next instrument in the database after the specified instrument. Return the 1st element of the database if ins is the
     * last element.
     *
     * @param ins
     * @return
     */
    @Override
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
     * The previous instrument in the database after the specified instrument. Return the 1st element of the database if ins is
     * the last element.
     *
     * @param ins
     * @return
     */
    @Override
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
    @Override
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

    @Override
    public T getInstrument(int index)
    {
        if (index < 0 || index > instruments.size() - 1)
        {
            throw new IllegalArgumentException("index=" + index);
        }
        return instruments.get(index);
    }

    @Override
    public T getInstrumentFromPC(int progChange)
    {
        if (progChange < 0 || progChange > 127)
        {
            throw new IllegalArgumentException("progChange=" + progChange);
        }
        for (T i : instruments)
        {
            if (i.getProgramChange() == progChange)
            {
                return i;
            }
        }
        return null;
    }

    @Override
    public List<T> getDrumsInstruments()
    {
        ArrayList<T> res = new ArrayList<>();
        for (T ins : instruments)
        {
            if (ins.getDrumKit() != null)
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
    @Override
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

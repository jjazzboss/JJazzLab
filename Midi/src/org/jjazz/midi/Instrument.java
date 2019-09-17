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

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MidiMessage;
import org.jjazz.midi.InstrumentBank.BankSelectMethod;
import org.openide.util.Lookup;

/**
 * The data used to select via MIDI an instrument on a synthesizer.
 */
public class Instrument implements Serializable
{

    /**
     * Required by the Instrument serialization process : an implementation must be available in the global lookup.
     */
    public interface BankFinder
    {

        static public class Utilities
        {

            /**
             * Find an implementation in the global lookup.
             *
             * @return
             */
            static public BankFinder getDefault()
            {
                BankFinder bf = Lookup.getDefault().lookup(BankFinder.class);
                if (bf == null)
                {
                    throw new IllegalStateException("ibf=" + null);
                }
                return bf;
            }
        }

        /**
         * @param synthName The MidiSynth name containing the bank
         * @param bankName
         * @return Null if no bank found
         */
        InstrumentBank<?> getBank(String synthName, String bankName);

    }

    private InstrumentBank<?> bank;
    private String patchName;
    private int programChange;
    private int bankSelectLSB;
    private int bankSelectMSB;
    private InstrumentBank.BankSelectMethod bankSelectMethod;
    private static final Logger LOGGER = Logger.getLogger(Instrument.class.getSimpleName());

    /**
     * Constructor with bank=null, bankLSB=-1, bankMSB=-1, bankSelectMethod=null
     *
     * @param programChange
     * @param patchName
     */
    public Instrument(int programChange, String patchName)
    {
        this(programChange, patchName, null);
    }

    /**
     * Constructor with bankLSB=-1, bankMSB=-1 and bankSelectMethod=null
     *
     * @param programChange The MIDI Program Change number 0-127
     * @param patchName The patchName of the patch, e.g. "Grand Piano"
     * @param bank The InstrumentBank this instruments belongs to. Can be null.
     */
    public Instrument(int programChange, String patchName, InstrumentBank<?> bank)
    {
        this(programChange, patchName, bank, -1, -1, null);
    }

    /**
     *
     * @param programChange The MIDI Program Change number 0-127
     * @param patchName The patchName of the patch, e.g. "Grand Piano"
     * @param bank The InstrumentBank this instruments belongs to. Can be null.
     * @param bankLSB Can be used to override the bank's bankSelectLSB. Use -1 if not used.
     * @param bankMSB Can be used to override the bank's bankSelectMSB. Use -1 if not used.
     * @param bsm Can be used to override the bank's bankSelectMethod. Use null if not used.
     */
    public Instrument(int programChange, String patchName, InstrumentBank<?> bank, int bankLSB, int bankMSB, BankSelectMethod bsm)
    {
        if (patchName == null || patchName.trim().isEmpty()
                || programChange < 0 || programChange > 127
                || bankLSB < -1 || bankLSB > 127
                || bankMSB < -1 || bankMSB > 127)
        {
            throw new IllegalArgumentException("pc=" + programChange + " name=" + patchName + " bankLSB=" + bankLSB + " bankMSB=" + bankMSB);
        }
        this.patchName = patchName;
        this.programChange = programChange;
        this.bank = bank;
        this.bankSelectMSB = bankMSB;
        this.bankSelectLSB = bankLSB;
        this.bankSelectMethod = bsm;
    }

    /**
     * This function can be called only once. It is the responsibility of the specified bank to add the Instrument.
     *
     * @param bank A non null value, the InstrumentBank this Instrument belongs to, e.g. GM1Bank.
     */
    public void setBank(InstrumentBank<?> bank)
    {
        if (this.bank != null)
        {
            throw new IllegalStateException("bank already set!");
        }
        if (bank == null)
        {
            throw new IllegalArgumentException("bank=" + bank);
        }
        this.bank = bank;
    }

    /**
     * @return Can be null.
     */
    public InstrumentBank<?> getBank()
    {
        return bank;
    }

    public String getPatchName()
    {
        return patchName;
    }

    public int getProgramChange()
    {
        return programChange;
    }

    /**
     * If a specific bankSelectMSB was set for this instrument, return it. Otherwise return getBank().getBankSelectMSB(). If no
     * bank set for this instrument return -1.
     *
     * @return
     */
    public int getBankSelectMSB()
    {
        int r = -1;
        if (bankSelectMSB > 0)
        {
            r = bankSelectMSB;
        } else if (bank != null)
        {
            r = bank.getBankSelectMSB();
        }
        return r;
    }

    /**
     * If a specific bankSelectLSB was set for this instrument, return it. Otherwise return getBank().getBankSelectLSB(). If no
     * bank set for this instrument return -1.
     *
     * @return
     */
    public int getBankSelectLSB()
    {
        int r = -1;
        if (bankSelectLSB > 0)
        {
            r = bankSelectLSB;
        } else if (bank != null)
        {
            r = bank.getBankSelectLSB();
        }
        return r;
    }

    /**
     * Get the Midi messages to be sent to initialize the instrument.
     * <p>
     *
     * @param channel
     * @return
     */
    public MidiMessage[] getMidiMessages(int channel)
    {
        return MidiUtilities.getPatchMessages(channel, this);
    }

    /**
     * If a non-null bankSelectMethod was set for this instrument, return it. Otherwise return getBank().getBankSelectMethod(). If
     * no bank set, return null.
     *
     * @return
     */
    public BankSelectMethod getBankSelectMethod()
    {
        BankSelectMethod r = null;
        if (bankSelectMethod != null)
        {
            r = bankSelectMethod;
        } else if (bank != null)
        {
            r = bank.getBankSelectMethod();
        }
        return r;
    }

    @Override
    public String toString()
    {
        return patchName;
    }

    /**
     * Save this Instrument as a string so that it can be retrieved by loadFromString() if the MidiSynth and the related Bank
     * exists on the system which performs loadFromString().
     *
     * @return A string "MidiSynthName, BankName, PatchName"
     */
    public String saveAsString()
    {
        LOGGER.log(Level.FINE, "saveAsString() this={0} bank={1} midiSynth={2}", new Object[]
        {
            this, getBank().getName(), getBank().getMidiSynth().getName()
        });
        return getBank().getMidiSynth().getName() + ", " + getBank().getName() + ", " + getPatchName();
    }

    /**
     * Get the Instrument corresponding to the string produced by saveAsString().
     * <p>
     * Search the synth and bank from their name, then the Instrument from its patchName (ignoring case).
     *
     * @param s
     * @return Null if no instrument could be found corresponding to s.
     * @see saveAsString()
     *
     */
    static public Instrument loadFromString(String s)
    {
        if (s == null)
        {
            throw new NullPointerException("s");
        }
        String[] strs = s.split(" *, *");
        if (strs.length != 3)
        {
            return null;
        }
        String synthName = strs[0].trim();
        String bankName = strs[1].trim();
        String patchName = strs[2].trim();

        InstrumentBank<?> bank = BankFinder.Utilities.getDefault().getBank(synthName, bankName);
        if (bank == null)
        {
            return null;
        }
        return bank.getInstrument(patchName);
    }

    /* --------------------------------------------------------------------- Serialization
    * --------------------------------------------------------------------- */
    private Object writeReplace()
    {
        return new SerializationProxy(this);
    }

    private void readObject(ObjectInputStream stream)
            throws InvalidObjectException
    {
        throw new InvalidObjectException("Serialization proxy required");
    }

    /**
     * If Instrument's bank is null serialization will fail. Do not dire
     * <p>
     * ctly serialize Instrument instances because we need to reuse instances provided by the local InstrumentBanks.
     */
    private static class SerializationProxy implements Serializable
    {

        private static final long serialVersionUID = 2792087126L;
        private final int spVERSION = 1;
        private String spSaveString;
        private String spPatchname;

        private SerializationProxy(Instrument ins)
        {
            if (ins.getBank() == null)
            {
                throw new IllegalStateException("ins=" + ins);
            }
            spSaveString = ins.saveAsString();
            spPatchname = ins.getPatchName(); // Not really needed, just in case for safety
        }

        private Object readResolve() throws ObjectStreamException
        {
            Instrument ins = Instrument.loadFromString(spSaveString);
            if (ins == null)
            {
                GM1Bank gm1Bank = GM1Bank.getInstance();
                ins = gm1Bank.getSimilarInstrument(spPatchname.trim());
                if (ins == null)
                {
                    ins = gm1Bank.getInstrument(0);
                }
                LOGGER.log(Level.WARNING, "readResolve() Can not retrieve Instrument from string={0}, using instead GM1 Instrument={1}", new Object[]
                {
                    spSaveString, ins.getPatchName()
                });
            }
            return ins;
        }
    }
}

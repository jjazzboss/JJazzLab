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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MidiMessage;
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
    private MidiAddress address;
    private DrumKit drumKit;   // Optional
    private GM1Instrument substitute;   // Optional
    private static final Logger LOGGER = Logger.getLogger(Instrument.class.getSimpleName());

    /**
     * Constructor with bank=null, drumKit=null, and a MidiAddress(pc=programChange, bankLSB=-1, bankMSB=-1,
     * bankSelectMethod=null).
     *
     * @param programChange
     * @param patchName
     */
    public Instrument(int programChange, String patchName)
    {
        this(programChange, patchName, null);
    }

    /**
     * Constructor with bank=null, drumKit=kit, a MidiAddress(pc=programChange, bankLSB=-1, bankMSB=-1, bankSelectMethod=null),
     * and no GM1Instrument substitute.
     *
     * @param programChange
     * @param patchName
     * @param kit Must be null if instrument is not a drums/percussion kit
     */
    public Instrument(int programChange, String patchName, DrumKit kit)
    {
        this(patchName, null, new MidiAddress(programChange, -1, -1, null), kit, null);
    }

    /**
     * Create an instrument.
     * <p>
     * If bank is non-null and ma is not fully defined (see MidiAddress.isFullyDefined()), then a new MidiAddress is created which
     * replaces the undefined values by the bank default values.
     *
     *
     * @param patchName The patchName of the patch, e.g. "Grand Piano"
     * @param bank The InstrumentBank this instruments belongs to. Can be null if undefined.
     * @param ma The MidiAddress of the instrument. Can't be null.
     * @param kit Optional. Must be non-null for drums/percussion instruments.
     * @param substitute Optional. Must be null for drums/percussion instruments.
     */
    public Instrument(String patchName, InstrumentBank<?> bank, MidiAddress ma, DrumKit kit, GM1Instrument substitute)
    {
        if (patchName == null || patchName.trim().isEmpty() || ma == null || (kit != null && substitute != null))
        {
            throw new IllegalArgumentException("patchName=" + patchName + " bank=" + bank + " ma=" + ma + " kit=" + kit + " substitute=" + substitute);
        }
        this.patchName = patchName;
        this.bank = bank;
        this.drumKit = kit;
        this.substitute = substitute;
        address = new MidiAddress(ma.getProgramChange(),
                (bank != null && ma.getBankMSB() == -1) ? this.bank.getDefaultBankSelectMSB() : ma.getBankMSB(),
                (bank != null && ma.getBankLSB() == -1) ? this.bank.getDefaultBankSelectLSB() : ma.getBankLSB(),
                (bank != null && ma.getBankSelectMethod() == null) ? this.bank.getDefaultBankSelectMethod() : ma.getBankSelectMethod()
        );
    }

    /**
     * This function can be called only once.
     * <p>
     * It is the responsibility of the specified bank to add the Instrument.
     * <p>
     * If this object's MidiAddress has undefined bankMSB or bankLSB or bankSelectMethod, then a new MidiAddress is created which
     * replaces the undefined values by the bank default values.
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
        if (!address.isFullyDefined())
        {
            address = new MidiAddress(address.getProgramChange(),
                    (address.getBankMSB() == -1) ? this.bank.getDefaultBankSelectMSB() : address.getBankMSB(),
                    (address.getBankLSB() == -1) ? this.bank.getDefaultBankSelectLSB() : address.getBankLSB(),
                    (address.getBankSelectMethod() == null) ? this.bank.getDefaultBankSelectMethod() : address.getBankSelectMethod()
            );
        }
    }

    /**
     * True is this instrument represents a DrumKit (each note is a different percussion sound).
     *
     * @return
     */
    public boolean isDrumKit()
    {
        return getDrumKit() != null;
    }

    /**
     * An optional DrumKit associated to this instrument.
     *
     * @return Can be null.
     */
    public DrumKit getDrumKit()
    {
        return drumKit;
    }

    /**
     * An optional GM1Instrument that can be used as a GM1 replacement instrument.
     *
     * @return Can be null if not defined, or for drums instruments.
     */
    public GM1Instrument getSubstitute()
    {
        return this.substitute;
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

    /**
     * Get the MidiAddress for this instrument.
     * <p>
     *
     * @return
     */
    public MidiAddress getMidiAddress()
    {
        return address;
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
     *
     * @return Same as getPatchName()
     */
    @Override
    public String toString()
    {
        return patchName;
    }

    /**
     * Longer version.
     *
     * @return "[patchname, bank, substitute, drumkit]" (null values are skipped)
     */
    public String toLongString()
    {
        return "[" + patchName
                + ((bank != null) ? ", Bank=" + bank.getName() : "")
                + ((substitute != null) ? ", Substitute=" + this.substitute.getPatchName() : "")
                + ((this.drumKit != null) ? ", DrumKit=" + this.drumKit : "")
                + "]";
    }

    /**
     * Save this Instrument as a string so that it can be retrieved by loadFromString() if the MidiSynth and the related Bank
     * exists on the system which performs loadFromString().
     * <p>
     * The instrument must have a bank defined.
     *
     * @return A string "MidiSynthName, BankName, PatchName"
     * @throws IllegalStateException If instrument does not have an InstrumentBank defined.
     */
    public String saveAsString()
    {
        LOGGER.log(Level.FINE, "saveAsString() this={0} bank={1} midiSynth={2}", new Object[]
        {
            this, getBank().getName(), getBank().getMidiSynth().getName()
        });
        if (getBank() == null)
        {
            throw new IllegalStateException("Can't use this method if bank is not defined. this=" + this);
        }
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

    // --------------------------------------------------------------------- 
    // Serialization
    // --------------------------------------------------------------------- 
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
     * If Instrument's bank is null serialization will fail.
     * <p>
     * Do not directly serialize Instrument instances because we need to reuse instances provided by the local InstrumentBanks.
     */
    protected static class SerializationProxy implements Serializable
    {

        private static final long serialVersionUID = 2792087126L;
        private final int spVERSION = 1;
        private String spSaveString;
        private String spPatchname;

        protected SerializationProxy(Instrument ins)
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

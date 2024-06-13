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

import com.thoughtworks.xstream.XStream;
import org.jjazz.midi.api.synths.GM1Bank;
import org.jjazz.midi.api.synths.GM1Instrument;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MidiMessage;
import org.jjazz.midi.api.synths.GM2Bank;
import org.jjazz.midi.api.synths.GMSynth;
import org.jjazz.midi.api.synths.GSBank;
import org.jjazz.midi.api.synths.XGBank;
import org.jjazz.xstream.spi.XStreamConfigurator;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_SAVE;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_SAVE;
import org.openide.util.lookup.ServiceProvider;

/**
 * The data used to select via MIDI an instrument on a synthesizer.
 */
public class Instrument implements Serializable
{

    private InstrumentBank<?> bank;
    private String patchName;
    private MidiAddress address;
    private DrumKit drumKit;   // Optional
    private GM1Instrument substitute;   // Optional
    private static final Logger LOGGER = Logger.getLogger(Instrument.class.getSimpleName());

    /**
     * Constructor with bank=null, drumKit=null, and a MidiAddress(pc=programChange, bankLSB=-1, bankMSB=-1, bankSelectMethod=null).
     *
     * @param programChange
     * @param patchName
     */
    public Instrument(int programChange, String patchName)
    {
        this(programChange, patchName, null);
    }

    /**
     * Constructor with bank=null, drumKit=kit, a MidiAddress(pc=programChange, bankLSB=-1, bankMSB=-1, bankSelectMethod=null), and no GM1Instrument substitute.
     *
     * @param programChange
     * @param patchName
     * @param kit           Must be null if instrument is not a drums/percussion kit
     */
    public Instrument(int programChange, String patchName, DrumKit kit)
    {
        this(patchName, null, new MidiAddress(programChange, -1, -1, null), kit, null);
    }

    /**
     * Create an instrument.
     * <p>
     * If bank is non-null and ma is not fully defined (see MidiAddress.isFullyDefined()), then a new MidiAddress is created which replaces the undefined values
     * by the bank default values.
     *
     *
     * @param patchName  The patchName of the patch, e.g. "Grand Piano"
     * @param bank       The InstrumentBank this instruments belongs to. Can be null if undefined.
     * @param ma         The MidiAddress of the instrument. Can't be null.
     * @param kit        Optional. Must be non-null for drums/percussion instruments.
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

    public Instrument getCopy()
    {
        return new Instrument(this.patchName, this.bank, this.address, this.drumKit, this.substitute);
    }

    /**
     * This function can be called only once.
     * <p>
     * It is the responsibility of the specified bank to add the Instrument.
     * <p>
     * If this object's MidiAddress has undefined bankMSB or bankLSB or bankSelectMethod, then a new MidiAddress is created which replaces the undefined values
     * by the bank default values.
     *
     * @param bank A non null value, the InstrumentBank this Instrument belongs to, e.g. GM1Bank.
     */
    public void setBank(InstrumentBank<?> bank)
    {
        if (this.bank != null)
        {
            throw new IllegalStateException(
                    "Instrument=" + this.toLongString() + " - can't set bank to " + bank.getName() + ", bank is already set to " + this.bank.getName());
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
     * Set the optional GM1Instrument that can be used as a GM1 replacement instrument.
     * <p>
     * @param ins Can be null
     */
    public void setSubstitute(GM1Instrument ins)
    {
        this.substitute = ins;
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
     * A user-friendly string describing the instrument with its synth (or bank if its a standard bank).
     * <p>
     * Examples: "GM: Acoustic Piano", "MOXF: JP Strings"
     *
     * @return
     */
    public String getFullName()
    {
        if (bank == null || bank.getMidiSynth() == null)
        {
            return getPatchName();
        } else if (bank instanceof GM1Bank)
        {
            return "GM/" + getPatchName();
        } else if (bank instanceof GM2Bank)
        {
            return "GM2/" + getPatchName();
        } else if (bank instanceof XGBank)
        {
            return "XG/ " + getPatchName();
        } else if (bank instanceof GSBank)
        {
            return "GS/" + getPatchName();
        } else
        {
            return bank.getMidiSynth().getName() + "/" + getPatchName();
        }
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
     * Save this Instrument as a string so that it can be retrieved by loadFromString().
     * <p>
     *
     * @return A string "MidiSynthName#_#BankName#_#PatchName"
     * @throws IllegalStateException If instrument does not have an InstrumentBank and MidiSynth defined.
     * @see loadFromString(String)
     */
    public String saveAsString()
    {
        if (getBank() == null || getBank().getMidiSynth() == null)
        {
            throw new IllegalStateException("getBank()=" + getBank());
        }
        LOGGER.log(Level.FINE, "saveAsString() this={0} bank={1} midiSynth={2}", new Object[]
        {
            this, getBank().getName(), getBank().getMidiSynth().getName()
        });
        return getBank().getMidiSynth().saveAsString() + "#_#" + getBank().getName() + "#_#" + getPatchName();
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
        String[] strs = s.split("#_#");
        if (strs.length != 3)
        {
            strs = s.split(",");        // Kept for backwards compatibility with 1.x 
            if (strs.length != 3)
            {
                LOGGER.log(Level.WARNING, "loadFromString() Invalid string format : {0}", s);
                return null;
            }
        }
        String synthSaveString = strs[0].trim();
        String bankName = strs[1].trim();
        String patchName = strs[2].trim();

        MidiSynth synth = MidiSynth.loadFromString(synthSaveString);
        Instrument ins = null;
        if (synth != null)
        {
            InstrumentBank<?> bank = synth.getBank(bankName);
            if (bank != null)
            {
                ins = bank.getInstrument(patchName);
            }
        }
        return ins;
    }

    /**
     * This enables XStream instance configuration even for private classes or classes from non-public packages of Netbeans modules.
     */
    @ServiceProvider(service = XStreamConfigurator.class)
    public static class XStreamConfig implements XStreamConfigurator
    {

        @Override
        public void configure(XStreamConfigurator.InstanceId instanceId, XStream xstream)
        {
            switch (instanceId)
            {
                case SONG_LOAD, SONG_SAVE ->
                {
                    // Nothing
                }

                case MIDIMIX_LOAD, MIDIMIX_SAVE ->
                {

                    // From 4.1.0 new aliases to get rid of fully qualified class names in .sng files                    
                    xstream.alias("Instrument", Instrument.class);
                    xstream.alias("InstrumentSP", SerializationProxy.class);
                    xstream.useAttributeFor(SerializationProxy.class, "spVERSION");
                    xstream.useAttributeFor(SerializationProxy.class, "spPatchname");
                    xstream.useAttributeFor(SerializationProxy.class, "spSaveString");
                }
                default -> throw new AssertionError(instanceId.name());
            }
        }
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
     * <p>
     * spVERSION 2 introduces new XStream aliases (see XStreamConfig)
     */
    public static class SerializationProxy implements Serializable
    {

        private static final long serialVersionUID = 2792087126L;
        private int spVERSION = 2;          // Do not make final!
        private String spSaveString;
        private String spPatchname;

        public SerializationProxy(Instrument ins)
        {
            if (ins.getBank() == null || ins.getBank().getMidiSynth() == null)
            {
                throw new IllegalStateException("ins=" + ins + " ins.getBank()=" + ins.getBank());
            }
            spSaveString = ins.saveAsString();
            spPatchname = ins.getPatchName();       // Robustness, if spSaveString not usable
        }

        private Object readResolve() throws ObjectStreamException
        {
            Instrument ins = Instrument.loadFromString(spSaveString);
            if (ins == null)
            {
                GM1Bank gm1Bank = GMSynth.getInstance().getGM1Bank();
                ins = gm1Bank.guessInstrument(spPatchname.trim());
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

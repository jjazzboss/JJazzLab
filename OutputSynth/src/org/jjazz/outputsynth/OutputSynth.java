/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 * This file is part of the JJazzLab-X software.
 *
 * JJazzLab-X is free software: you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License (LGPLv3) 
 * as published by the Free Software Foundation, either version 3 of the License, 
 * or (at your option) any later version.
 *
 * JJazzLab-X is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JJazzLab-X.  If not, see <https://www.gnu.org/licenses/>
 *
 * Contributor(s): 
 *
 */
package org.jjazz.outputsynth;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.jjazz.midi.synths.GSSynth;
import org.jjazz.midi.Instrument;
import org.jjazz.midi.InstrumentBank;
import org.jjazz.midi.MidiSynth;
import org.jjazz.midi.synths.StdSynth;
import org.jjazz.midiconverters.api.ConvertersManager;
import org.jjazz.rhythm.api.RhythmVoice;

/**
 * The information about the MidiSynth connected to the Midi output of JJazzLab.
 */
public class OutputSynth implements Serializable
{

    public enum SendModeOnUponStartup
    {
        OFF, GM, GM2, XG, GS
    }
    public static final String PROP_FILE = "file";
    /**
     * Fired when of the sendModeOnUponStartup is changed.
     */
    public static final String PROP_SEND_MSG_UPON_STARTUP = "sendMsgUponStartup";

    /**
     * oldValue=false if removed, true if added. newValue=InstrumentBank<?>
     */
    public static final String PROP_STD_BANK = "PROP_STD_BANK";
    /**
     * oldValue=false if removed, true if added. newValue=MidiSynth
     */
    public static final String PROP_CUSTOM_SYNTH = "PROP_CUSTOM_SYNTH";
    /**
     * oldValue=old UserInstrument, newValue=new UserInstrument
     */
    public static final String PROP_USER_INSTRUMENT = "PROP_USER_INSTRUMENT";

    private final List<InstrumentBank<?>> compatibleStdBanks;
    private final List<MidiSynth> customSynths;
    protected GMRemapTable remapTable;
    private Instrument userInstrument;
    private SendModeOnUponStartup sendModeOnUponStartup;
    private File file;
    private static final Logger LOGGER = Logger.getLogger(OutputSynth.class.getSimpleName());
    private final transient PropertyChangeSupport pcs = new java.beans.PropertyChangeSupport(this);

    /**
     * Construct a default OutputSynth compatible with the GM1 Bank and with no custom MidiSynth.
     */
    public OutputSynth()
    {
        compatibleStdBanks = new ArrayList<>();
        customSynths = new ArrayList<>();
        compatibleStdBanks.add(StdSynth.getGM1Bank());
        remapTable = new GMRemapTable();
        userInstrument = StdSynth.getGM1Bank().getInstrument(0);  // Piano
        sendModeOnUponStartup = SendModeOnUponStartup.OFF;
    }

    /**
     * Construct a new OutputSynth which copies the values from os.
     *
     * @param os
     */
    public OutputSynth(OutputSynth os)
    {
        compatibleStdBanks = new ArrayList<>();
        customSynths = new ArrayList<>();
        compatibleStdBanks.addAll(os.compatibleStdBanks);
        remapTable = new GMRemapTable(os.remapTable);
        customSynths.addAll(customSynths);
        userInstrument = os.userInstrument;
        sendModeOnUponStartup = os.getSendModeOnUponStartup();
    }
    
    public void set(OutputSynth os)
    {
        
    }

    public GMRemapTable getGMRemapTable()
    {
        return remapTable;
    }

    /**
     * Restore the OutputSynth in the initial state, with only a GM1 Bank, no custom synth, and a clear GMRemapTable.
     */
    public void reset()
    {
        for (InstrumentBank<?> bank : getCompatibleStdBanks())
        {
            if (bank != StdSynth.getGM1Bank())
            {
                removeCompatibleStdBank(bank);
            }
        }
        if (getCompatibleStdBanks().isEmpty())
        {
            addCompatibleStdBank(StdSynth.getGM1Bank());
        }
        for (MidiSynth synth : getCustomSynths())
        {
            removeCustomSynth(synth);
        }
        remapTable.clear();
        setUserInstrument(StdSynth.getGM1Bank().getInstrument(0));
        setSendModeOnUponStartup(SendModeOnUponStartup.OFF);
    }

    /**
     * The file associated to this OutputSynth.
     *
     * @return Can be null.
     */
    public File getFile()
    {
        return file;
    }

    /**
     * Set the file associated to this OutputSynth.
     *
     * @param file
     */
    public void setFile(File file)
    {
        File oldFile = this.file;
        this.file = file;
        pcs.firePropertyChange(PROP_FILE, oldFile, file);
    }

    /**
     * Get the list of standard InstrumentBanks (GM/GM2/XG/GS) which are compatible with this OutputSynth.
     *
     * @return Can be an empty list.
     */
    public List<InstrumentBank<?>> getCompatibleStdBanks()
    {
        return new ArrayList<>(compatibleStdBanks);
    }

    /**
     * Add a standard bank compatible with this OutputSynth.
     * <p>
     * NOTE: GS and XG/GM2 are incompatible, one will not be added if the other(s) is present.
     *
     * @param stdBank Must be GM/GM2/XG/GS
     */
    public void addCompatibleStdBank(InstrumentBank<?> stdBank)
    {
        if (stdBank == null || !getStdBanks().contains(stdBank))
        {
            throw new IllegalArgumentException("stdBank=" + stdBank);
        }

        if ((stdBank == StdSynth.getGM2Bank() || stdBank == StdSynth.getXGBank()) && compatibleStdBanks.contains(GSSynth.getGSBank()))
        {
            LOGGER.warning("addCompatibleStdBank() Can't add " + stdBank + " because the GS bank is used");
            return;
        } else if (stdBank == GSSynth.getGSBank() && (compatibleStdBanks.contains(StdSynth.getGM2Bank()) || compatibleStdBanks.contains(StdSynth.getXGBank())))
        {
            LOGGER.warning("addCompatibleStdBank() Can't add " + stdBank + " because the GM2 or XG bank is used");
            return;
        }

        if (!compatibleStdBanks.contains(this))
        {
            compatibleStdBanks.add(stdBank);
            pcs.firePropertyChange(PROP_STD_BANK, true, stdBank);
        }
    }

    /**
     * Remove a standard bank compatible with this OutputSynth.
     *
     * @param stdBank
     */
    public void removeCompatibleStdBank(InstrumentBank<?> stdBank)
    {
        if (stdBank == null)
        {
            throw new IllegalArgumentException("stdBank=" + stdBank);
        }

        if (compatibleStdBanks.remove(stdBank))
        {
            pcs.firePropertyChange(PROP_STD_BANK, false, stdBank);
        }
    }

    /**
     * Get the list of custom MidiSynths which are compatible with this OutputSynth.
     *
     * @return Can be an empty list.
     */
    public List<MidiSynth> getCustomSynths()
    {
        return new ArrayList<>(customSynths);
    }

    /**
     * Add a custom MidiSynth compatible with this OutputSynth.
     * <p>
     *
     * @param synth
     */
    public void addCustomSynth(MidiSynth synth)
    {
        if (synth == null)
        {
            throw new IllegalArgumentException("stdBank=" + synth);
        }

        if (!customSynths.contains(synth))
        {
            customSynths.add(synth);
            for (InstrumentBank<?> bank : getCompatibleStdBanks(synth))
            {
                if (!compatibleStdBanks.contains(bank))
                {
                    addCompatibleStdBank(bank);
                }
            }
            pcs.firePropertyChange(PROP_CUSTOM_SYNTH, true, synth);
        }
    }

    /**
     * Remove a custom MidiSynth compatible with this OutputSynth.
     * <p>
     *
     * @param synth
     */
    public void removeCustomSynth(MidiSynth synth)
    {
        if (synth == null || synth == StdSynth.getInstance())
        {
            throw new IllegalArgumentException("stdBank=" + synth);
        }
        if (customSynths.remove(synth))
        {
            pcs.firePropertyChange(PROP_CUSTOM_SYNTH, false, synth);
        }
    }

    /**
     * Get 'Send XX Mode ON upon startup' feature.
     *
     * @return If null feature is disabled.
     */
    public SendModeOnUponStartup getSendModeOnUponStartup()
    {
        return this.sendModeOnUponStartup;
    }

    /**
     * Enable or disable the 'Send XX Mode ON upon startup' feature.
     *
     * @param mode Can't be null
     */
    public void setSendModeOnUponStartup(SendModeOnUponStartup mode)
    {
        if (mode == null)
        {
            throw new NullPointerException("mode");
        }
        SendModeOnUponStartup old = this.sendModeOnUponStartup;
        this.sendModeOnUponStartup = mode;
        pcs.firePropertyChange(PROP_SEND_MSG_UPON_STARTUP, old, sendModeOnUponStartup);
    }

    /**
     * Get an instrument compatible with this OutputSynth for the specified rhythm voice.
     * <p>
     * Search a matching instrument in the following order :<br>
     * - in the custom synths <br>
     * - in the compatible banks<br>
     * - in the GMRemapTable<br>
     * - Use the substitute or, if , create a custom instrument for the rv.GetPreferredInstrument().
     *
     * @param rv
     * @return Can't be null. It may be the VoidInstrument for drums/percussion.
     */
    public Instrument findInstrument(RhythmVoice rv)
    {
        Instrument rvIns = rv.getPreferredInstrument();
        assert rvIns != null : "rv=" + rv;
        ConvertersManager cm = ConvertersManager.getInstance();
        Instrument ins = null;
        // Try with custom synths first
        for (MidiSynth synth : customSynths)
        {
            ins = cm.convertInstrument(rvIns, synth);
            if (ins != null)
            {
                break;
            }
        }
        if (ins == null)
        {
            // Try with the standard banks
            ins = cm.convertInstrument(rvIns, StdSynth.getInstance());
        }
        if (ins == null)
        {
            // Use the GM1Substitute or its remaps
            switch (rv.getType())
            {
                case DRUMS:
                    ins = remapTable.getInstrument(GMRemapTable.DRUMS_INSTRUMENT);
                    if (ins == null)
                    {
                        ins = StdSynth.getVoidInstrument();
                    }
                    break;
                case PERCUSSION:
                    ins = remapTable.getInstrument(GMRemapTable.PERCUSSION_INSTRUMENT);
                    if (ins == null)
                    {
                        ins = StdSynth.getVoidInstrument();
                    }
                    break;
                default:
                    // Use the default GM1 instrument
                    ins = remapTable.getInstrument(rvIns.getSubstitute());
                    if (ins == null)
                    {
                        // No remap: use the substitute
                        ins = rvIns.getSubstitute();
                    }
                    break;
            }
        }
        assert ins != null : "rv=" + rv;
        return ins;
    }

    /**
     * The instrument for the user channel.
     *
     * @return Can't be null.
     */
    public Instrument getUserInstrument()
    {
        return userInstrument;
    }

    /**
     * Set the instrument for the user channel.
     *
     * @param ins Can't be null
     */
    public void setUserInstrument(Instrument ins)
    {
        if (ins == null)
        {
            throw new IllegalArgumentException("ins=" + ins);
        }
        Instrument oldIns = userInstrument;
        if (oldIns != userInstrument)
        {
            userInstrument = ins;
            pcs.firePropertyChange(PROP_USER_INSTRUMENT, oldIns, ins);
        }
    }

    /**
     * Save this OutputSynth to a file.
     * <p>
     *
     * @param f
     * @throws java.io.IOException
     */
    public void saveToFile(File f) throws IOException
    {
        if (f == null)
        {
            throw new IllegalArgumentException("f=" + f);
        }
        LOGGER.fine("saveToFile() f=" + f.getAbsolutePath());

        setFile(f);

        try (FileOutputStream fos = new FileOutputStream(f))
        {
            XStream xstream = new XStream();
            xstream.alias("OutputSynth", OutputSynth.class);
            xstream.toXML(this, fos);
        } catch (IOException e)
        {
            setFile(null);
            throw new IOException(e);
        } catch (XStreamException e)
        {
            setFile(null);
            LOGGER.warning("saveToFile() exception=" + e.getLocalizedMessage());
            // Translate into an IOException to be handled by the Netbeans framework 
            throw new IOException("XStream XML unmarshalling error", e);
        }
    }
    

    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }

    // ========================================================================================
    // Private methods
    // ========================================================================================    
    /**
     * Check if the specified synth is compatible with standard banks.
     *
     * @param synth
     * @return The list of standard banks compatible with synth.
     */
    private List<InstrumentBank<?>> getCompatibleStdBanks(MidiSynth synth)
    {
        ArrayList<InstrumentBank<?>> res = new ArrayList<>();
        for (InstrumentBank<?> stdBank : getStdBanks())
        {
            float coverage = synth.getMidiAddressMatchingCoverage(stdBank);
            if (coverage > 0.8f)
            {
                res.add(stdBank);
            }
        }
        return res;
    }

    private List<InstrumentBank<?>> getStdBanks()
    {
        ArrayList<InstrumentBank<?>> res = new ArrayList<>();
        res.addAll(StdSynth.getInstance().getBanks());
        res.add(GSSynth.getGSBank());
        return res;
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
     *
     * <p>
     */
    protected static class SerializationProxy implements Serializable
    {

        private static final long serialVersionUID = -29672611210L;
        private final int spVERSION = 1;
        private final List<String> spCompatibleStdBankNames = new ArrayList<>();
        private final List<String> spCustomSynthsStrings = new ArrayList<>();
        private GMRemapTable spRemapTable;
        private Instrument spUserInstrument;
        private SendModeOnUponStartup spSendModeOnUponStartup;

        protected SerializationProxy(OutputSynth outSynth)
        {
            for (InstrumentBank<?> bank : outSynth.getCompatibleStdBanks())
            {
                String str = "GM";
                if (bank == StdSynth.getGM2Bank())
                {
                    str = "GM2";
                } else if (bank == StdSynth.getXGBank())
                {
                    str = "XG";
                } else if (bank == GSSynth.getGSBank())
                {
                    str = "GS";
                }
                spCompatibleStdBankNames.add(str);
            }
            for (MidiSynth synth : outSynth.getCustomSynths())
            {
                spCustomSynthsStrings.add(synth.saveAsString());
            }
            spRemapTable = new GMRemapTable(outSynth.getGMRemapTable());
            spUserInstrument = outSynth.getUserInstrument();
            spSendModeOnUponStartup = outSynth.getSendModeOnUponStartup();
        }

        private Object readResolve() throws ObjectStreamException
        {
            OutputSynth outSynth = new OutputSynth();
            if (!spCompatibleStdBankNames.contains("GM"))
            {
                outSynth.removeCompatibleStdBank(StdSynth.getGM1Bank());
            }
            for (String strBank : spCompatibleStdBankNames)
            {
                switch (strBank)
                {
                    case "GM2":
                        outSynth.addCompatibleStdBank(StdSynth.getGM2Bank());
                        break;
                    case "XG":
                        outSynth.addCompatibleStdBank(StdSynth.getXGBank());
                        break;
                    case "GS":
                        outSynth.addCompatibleStdBank(GSSynth.getGSBank());
                        break;
                    default:
                    // Nothing
                }
            }
            for (String strSynth : spCustomSynthsStrings)
            {
                MidiSynth synth = MidiSynth.loadFromString(strSynth);
                if (synth == null)
                {
                    LOGGER.warning("readResolve() Can't restore MidiSynth from save string: " + strSynth);
                } else
                {
                    outSynth.addCustomSynth(synth);
                }
            }
            outSynth.remapTable = spRemapTable;
            outSynth.setUserInstrument(spUserInstrument);
            outSynth.setSendModeOnUponStartup(spSendModeOnUponStartup);
            return outSynth;
        }
    }

}

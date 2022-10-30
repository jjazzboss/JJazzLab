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
package org.jjazz.outputsynth.api;

import com.google.common.base.Preconditions;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.midi.api.synths.GSSynth;
import org.jjazz.midi.api.Instrument;
import org.jjazz.midi.api.InstrumentBank;
import org.jjazz.midi.api.MidiSynth;
import org.jjazz.midi.api.MidiUtilities;
import org.jjazz.midi.api.synths.GM1Instrument;
import org.jjazz.midi.api.synths.GMSynth;
import org.jjazz.midi.api.synths.StandardInstrumentConverter;
import org.jjazz.midiconverters.api.StdInstrumentConverter;
import org.jjazz.midimix.api.UserRhythmVoice;
import org.jjazz.rhythm.api.RhythmVoice;

/**
 * An OutputSynth associates a MidiSynthList with UserSettings.
 * <p>
 */
public class OutputSynth
{

    private final MidiSynthList midiSynthList;
    private final UserSettings userSettings;

    private static final Logger LOGGER = Logger.getLogger(OutputSynth.class.getSimpleName());


    /**
     * Construct an OutputSynth with the specified midiSynthList and default UserSettings.
     * <p>
     * @param synthList
     */
    public OutputSynth(MidiSynthList synthList)
    {
        Preconditions.checkNotNull(synthList);
        this.midiSynthList = synthList;
        this.userSettings = new UserSettings();
    }

    /**
     * Get the MidiSynthList of this OutputSynth.
     *
     * @return
     */
    public MidiSynthList getMidiSynthList()
    {
        return midiSynthList;
    }

    /**
     * Get the UserSettings of this OutputSynth.
     *
     * @return
     */
    public UserSettings getUserSettings()
    {
        return userSettings;
    }

    /**
     * Find an instrument from this OutputSynth matching, as much as possible, the specified rhythm voice's preferred instrument.
     * <p>
     * <p>
     * Search a matching instrument :<br>
     * - then search for the instrument in the MidiSynths<br>
     * - then search using the GM1 substitute, remap table, and substitute family<p>
     * <p>
     *
     * @param rv Must be a standard RhythmVoice (not a UserRhythmVoice)
     * @return Can't be null. It may be the VoidInstrument for drums/percussion.
     */
    public Instrument findInstrument(RhythmVoice rv)
    {

        if (rv == null || rv instanceof UserRhythmVoice)
        {
            throw new IllegalArgumentException("rv=" + rv);
        }


        // rvIns can be a YamahaRefSynth instrument (with GM1 substitute defined), or  a GM/GM2/XG instrument, or a VoidInstrument
        Instrument rvIns = rv.getPreferredInstrument();
        assert rvIns != null : "rv=" + rv;   //NOI18N
        LOGGER.log(Level.FINE, "findInstrument() -- rv={0}", rv.toString());   //NOI18N


        // Handle special VoidInstrument case
        if (rvIns == GMSynth.getInstance().getVoidInstrument())
        {
            // No conversion possible, use void for drums or the default at instrument
            var ins = rv.isDrums() ? GMSynth.getInstance().getVoidInstrument() : rv.getType().getDefaultInstrument();
            LOGGER.log(Level.FINE, "findInstrument() rv preferred instrument=VoidInstrument, return ins=" + ins);   //NOI18N
            return ins;
        }


        InstrumentBank<?> rvInsBank = rvIns.getBank();
        MidiSynth rvInsSynth = (rvInsBank != null) ? rvInsBank.getMidiSynth() : null;


        // Try to use the StandardInstrumentConverter
        for (MidiSynth synth : midiSynthList)
        {
            var ins = StandardInstrumentConverter.convertInstrument(rvIns, synth);
            if (ins != null)
            {
                LOGGER.log(Level.FINE, "findInstrument()    found by StandardInstrumentConverter, ins={0}", ins.toLongString());   //NOI18N                
                return ins;
            }
        }


        if (!rv.isDrums())
        {
            // Melodic voice
            GM1Instrument gmSubstitute = rvIns.getSubstitute();

            // Try the GM1Instrument remap
            ins = remapTable.getInstrument(gmSubstitute);
            if (ins != null)
            {
                LOGGER.log(Level.FINE, "findInstrument()    using mapped instrument for substitute. ins={0}", ins.toLongString());   //NOI18N
                return ins;
            }

            // Search in the custom synth for instruments whose GMSubstitute match
            assert gmSubstitute != null : "rv=" + rv;   //NOI18N
            for (MidiSynth synth : midiSynthList)
            {
                for (InstrumentBank<? extends Instrument> bank : synth.getBanks())
                {
                    List<? extends Instrument> inss = bank.getInstrumentsFromSubstitute(gmSubstitute);
                    if (!inss.isEmpty())
                    {
                        ins = inss.get(0);
                        LOGGER.log(Level.FINE, "findInstrument()    found in custom synth using substitute. ins={0}", ins.toLongString());   //NOI18N
                        return ins;
                    }
                }
            }

            // Try the family remap
            ins = remapTable.getInstrument(gmSubstitute.getFamily());
            if (ins != null)
            {
                LOGGER.log(Level.FINE, "findInstrument()    using mapped instrument for substitute family. ins={0}", ins.toLongString());   //NOI18N
                return ins;
            }

            // Search in the std banks for instruments whose GMSubstitute match
            assert gmSubstitute != null : "rv=" + rv;   //NOI18N
            for (var bank : compatibleStdBanks)
            {
                List<? extends Instrument> inss = bank.getInstrumentsFromSubstitute(gmSubstitute);
                if (!inss.isEmpty())
                {
                    ins = inss.get(0);
                    LOGGER.log(Level.FINE, "findInstrument()    found in std bank using substitute. ins={0}", ins.toLongString());   //NOI18N
                    return ins;
                }
            }

            // Search in the custom synth for instruments whose GMSubstitute's family match
            for (MidiSynth synth : midiSynthList)
            {
                for (InstrumentBank<? extends Instrument> bank : synth.getBanks())
                {
                    List<? extends Instrument> inss = bank.getInstrumentsFromFamily(gmSubstitute.getFamily());
                    if (!inss.isEmpty())
                    {
                        ins = inss.get(0);
                        LOGGER.log(Level.FINE, "findInstrument()    found in custom synth using substitute's family. ins={0}", ins.toLongString());   //NOI18N
                        return ins;
                    }
                }
            }

            // Search in the compatible banks for instruments whose GMSubstitute's family match
            for (InstrumentBank<? extends Instrument> bank : compatibleStdBanks)
            {
                List<? extends Instrument> inss = bank.getInstrumentsFromFamily(gmSubstitute.getFamily());
                if (!inss.isEmpty())
                {
                    ins = inss.get(0);
                    LOGGER.log(Level.FINE, "findInstrument()    found in std bank using substitute's family. ins={0}", ins.toLongString());   //NOI18N
                    return ins;
                }
            }

            // No possible conversion found
            ins = rvIns.getSubstitute();
            LOGGER.log(Level.FINE, "findInstrument()    no conversion found. Using rv substitute. ins={0}", ins.toLongString());   //NOI18N
            return ins;

        } else
        {
            // Drums voices: use the DrumKit information
            DrumKit kit = rvIns.getDrumKit();
            assert kit != null : "rv=" + rv;   //NOI18N

            // Use the user-defined default instrument for drums/perc if DrumKit matches
            Instrument defaultIns = remapTable.getInstrument(rv.getType().equals(RhythmVoice.Type.DRUMS) ? GMRemapTable.DRUMS_INSTRUMENT : GMRemapTable.PERCUSSION_INSTRUMENT);
            if (defaultIns != null && kit.equals(defaultIns.getDrumKit()))
            {
                ins = defaultIns;
                LOGGER.log(Level.FINE, "findInstrument()    using the remap table (good DrumKit match) ins={0}", ins.toLongString());   //NOI18N
                return defaultIns;
            }

            // Search a matching kit in the custom synths
            for (MidiSynth synth : midiSynthList)
            {
                for (InstrumentBank<? extends Instrument> bank : synth.getBanks())
                {
                    List<? extends Instrument> inss = bank.getDrumsInstrument(kit, false);
                    if (!inss.isEmpty())
                    {
                        ins = inss.get(0);
                        LOGGER.log(Level.FINE, "findInstrument()    found in custom synth using drumkit. ins={0}", ins.toLongString());   //NOI18N
                        return ins;
                    }
                }
            }

            // Search a matching kit in the compatible banks
            ins = StdInstrumentConverter.getInstance().findStandardDrumsInstrument(rvIns.getDrumKit(), compatibleStdBanks, false);
            if (ins != null)
            {
                LOGGER.log(Level.FINE, "findInstrument()    found in std bank using drumkit. ins={0}", ins.toLongString());   //NOI18N
                return ins;
            }

            // Same but TRYHARDER
            for (MidiSynth synth : midiSynthList)
            {
                for (InstrumentBank<? extends Instrument> bank : synth.getBanks())
                {
                    List<? extends Instrument> inss = bank.getDrumsInstrument(kit, true);
                    if (!inss.isEmpty())
                    {
                        ins = inss.get(0);
                        LOGGER.log(Level.FINE, "findInstrument()    found in custom synth using drumkit. ins={0}", ins.toLongString());   //NOI18N
                        return ins;
                    }
                }
            }

            // Same but TRYHARDER
            ins = StdInstrumentConverter.getInstance().findStandardDrumsInstrument(rvIns.getDrumKit(), compatibleStdBanks, true);
            if (ins != null)
            {
                LOGGER.log(Level.FINE, "findInstrument()    found in std bank using drumkit. ins={0}", ins.toLongString());   //NOI18N
                return ins;
            }

            // Use the default Drums if defined
            if (defaultIns != null)
            {
                ins = defaultIns;
                LOGGER.log(Level.FINE, "findInstrument()    using the remap table for drums/perc. ins={0}", ins.toLongString());   //NOI18N
                return ins;
            }

            // NOTHING correct found...
            ins = StdSynth.getInstance().getVoidInstrument();
            LOGGER.log(Level.FINE, "findInstrument()    using VoidInstrument drums/perc. ins={0}", ins.toLongString());   //NOI18N
            return ins;
        }
    }


    /**
     * Save this OutputSynth as a string so that it can be retrieved by loadFromString().
     * <p>
     * If a
     *
     * @return
     * @see loadFromString(String)
     */
    public String saveAsString()
    {
        return midiSynthList.saveAsString() + "##" + userSettings.saveAsString();
    }

    /**
     * Get an OutputSynth corresponding to the string produced by saveAsString().
     * <p>
     *
     * @param s
     * @return
     * @throws java.io.IOException
     * @see saveAsString()
     */
    static public OutputSynth loadFromString(String s) throws IOException
    {
        String[] strs = s.split("##");
        if (strs.length != 2)
        {
            throw new IOException("Invalid string s=" + s);
        }
        String strMsl = strs[0].trim();
        String strUs = strs[1].trim();

        MidiSynthList synthList = MidiSynthList.loadFromString(strMsl);
        OutputSynth res = new OutputSynth(synthList);
        res.userSettings.restoreFromString(strUs);
        return res;
    }


    // ========================================================================================
    // Private methods
    // ========================================================================================    
    // ========================================================================================
    // Private classes
    // ========================================================================================    
    /**
     * The user-dependent settings associated to the OutputSynth: GM remap table, latency, user track default voice, etc.
     */
    public class UserSettings
    {

        public static final String PROP_USERINSTRUMENT = "userInstrument";
        public static final String PROP_AUDIOLATENCY = "AudioLatency";
        public static final String PROP_SENDMODEONUPONSTARTUP = "sendModeOnUponStartup";


        public enum SendModeOnUponStartup
        {
            OFF, GM, GM2, XG, GS;
        }

        private SendModeOnUponStartup sendModeOnUponPlay;
        protected GMRemapTable remapTable;
        private int audioLatency;
        private Instrument userInstrument;

        private transient final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

        /**
         * Create an instance with sendModeOnUponPlay=OFF, audioLatency=50, userInstrument = 1st instrument of the synthList,
         * empty GMremapTable
         */
        public UserSettings()
        {
            this.remapTable = new GMRemapTable(midiSynthList);
            this.sendModeOnUponPlay = SendModeOnUponStartup.OFF;
            this.userInstrument = midiSynthList.getMidiSynths().get(0).getInstruments().get(0);
            this.audioLatency = 50;
        }


        /**
         * Get the value of AudioLatency
         *
         * @return the value of AudioLatency
         */
        public int getAudioLatency()
        {
            return audioLatency;
        }

        /**
         * Set the value of AudioLatency
         *
         * @param audioLatency new value of AudioLatency
         */
        public void setAudioLatency(int audioLatency)
        {
            int oldAudioLatency = this.audioLatency;
            this.audioLatency = audioLatency;
            propertyChangeSupport.firePropertyChange(PROP_AUDIOLATENCY, oldAudioLatency, audioLatency);
        }


        /**
         * Get the value of userInstrument
         *
         * @return Can't be null
         */
        public Instrument getUserInstrument()
        {
            return userInstrument;
        }

        /**
         * Set the value of userInstrument.
         *
         * @param ins Must be an instrument contained in the MidiSynthList.
         */
        public void setUserInstrument(Instrument ins)
        {
            Preconditions.checkNotNull(ins);
            if (!midiSynthList.contains(ins))
            {
                throw new IllegalArgumentException("ins=" + ins.toLongString());   //NOI18N
            }

            Instrument oldUserInstrument = this.userInstrument;
            this.userInstrument = ins;
            propertyChangeSupport.firePropertyChange(PROP_USERINSTRUMENT, oldUserInstrument, ins);

        }

        public GMRemapTable getGMRemapTable()
        {
            return remapTable;
        }


        /**
         * Get the value of sendModeOnUponStartup
         *
         * @return the value of sendModeOnUponStartup
         */
        public SendModeOnUponStartup getSendModeOnUponPlay()
        {
            return sendModeOnUponPlay;
        }

        /**
         * Set the value of sendModeOnUponStartup
         *
         * @param sendModeOnUponPlay new value of sendModeOnUponStartup
         */
        public void setSendModeOnUponPlay(SendModeOnUponStartup sendModeOnUponPlay)
        {
            SendModeOnUponStartup oldSendModeOnUponStartup = this.sendModeOnUponPlay;
            this.sendModeOnUponPlay = sendModeOnUponPlay;
            propertyChangeSupport.firePropertyChange(PROP_SENDMODEONUPONSTARTUP, oldSendModeOnUponStartup, sendModeOnUponPlay);
        }


        /**
         * Send the Sysex messages corresponding to getSendModeOnUponPlay().
         */
        public void sendModeOnUponPlaySysexMessages()
        {
            switch (sendModeOnUponPlay)
            {
                case GM:
                    MidiUtilities.sendSysExMessage(MidiUtilities.getGmModeOnSysExMessage());
                    break;
                case GM2:
                    MidiUtilities.sendSysExMessage(MidiUtilities.getGm2ModeOnSysExMessage());
                    break;
                case XG:
                    MidiUtilities.sendSysExMessage(MidiUtilities.getXgModeOnSysExMessage());
                    break;
                case GS:
                    MidiUtilities.sendSysExMessage(MidiUtilities.getGsModeOnSysExMessage());
                    break;
                case OFF:
                    break;
                default:
                    throw new IllegalStateException("sendModeOnUponPlay=" + sendModeOnUponPlay);   //NOI18N
            }
        }

        /**
         * Add PropertyChangeListener.
         *
         * @param listener
         */
        public void addPropertyChangeListener(PropertyChangeListener listener)
        {
            propertyChangeSupport.addPropertyChangeListener(listener);
        }

        public String saveAsString()
        {
            return audioLatency + ";" + sendModeOnUponPlay + ";" + userInstrument.saveAsString() + ";" + remapTable.saveAsString();
        }

        public void setFromString(String strUs)
        {
            
        }

        /**
         * Remove PropertyChangeListener.
         *
         * @param listener
         */
        public void removePropertyChangeListener(PropertyChangeListener listener)
        {
            propertyChangeSupport.removePropertyChangeListener(listener);
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
     *
     * <p>
     * <p>
     */
    protected static class SerializationProxy implements Serializable
    {

        private static final long serialVersionUID = -29672611210L;
        private final int spVERSION = 1;
        private final List<String> spCustomSynthsStrings = new ArrayList<>();

        protected SerializationProxy(OutputSynth outSynth)
        {
            for (InstrumentBank<?> bank : outSynth.getCompatibleStdBanks())
            {
                String str = "GM";
                if (bank == StdSynth.getInstance().getGM2Bank())
                {
                    str = "GM2";
                } else if (bank == StdSynth.getInstance().getXGBank())
                {
                    str = "XG";
                } else if (bank == GSSynth.getInstance().getGSBank())
                {
                    str = "GS";
                }
                spCompatibleStdBankNames.add(str);
            }
            for (MidiSynth synth : outSynth.getMidiSynths())
            {
                spCustomSynthsStrings.add(synth.saveAsString());
            }
            spRemapTable = new GMRemapTable(outSynth.getGMRemapTable());
            spUserInstrument = outSynth.getUserInstrument();
            spSendModeOnUponPlay = outSynth.getSendModeOnUponPlay();
            spAudioLatency = outSynth.getAudioLatency();
        }

        private Object readResolve() throws ObjectStreamException
        {

            OutputSynth outSynth = new OutputSynth();
            for (String strBank : spCompatibleStdBankNames)
            {
                switch (strBank)
                {
                    case "GM2":
                        outSynth.addCompatibleStdBank(StdSynth.getInstance().getGM2Bank());
                        break;
                    case "XG":
                        outSynth.addCompatibleStdBank(StdSynth.getInstance().getXGBank());
                        break;
                    case "GS":
                        outSynth.addCompatibleStdBank(GSSynth.getInstance().getGSBank());
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
                    LOGGER.warning("readResolve() Can't restore MidiSynth from save string: " + strSynth);   //NOI18N
                } else
                {
                    outSynth.addCustomSynth(synth);
                }
            }
            if (!spCompatibleStdBankNames.contains("GM"))
            {
                boolean b = outSynth.removeCompatibleStdBank(StdSynth.getInstance().getGM1Bank());  // Remove must be done last
                assert b = true : "spCompatibleStdBankNames=" + spCompatibleStdBankNames + " spCustomSynthsStrings=" + spCustomSynthsStrings;   //NOI18N
            }
            outSynth.remapTable = spRemapTable;
            outSynth.remapTable.setContainer(outSynth);
            if (spUserInstrument == null)
            {
                LOGGER.warning("readResolve() spUserInstrument=" + spUserInstrument + ". Using default user instrument");   //NOI18N
            } else
            {
                outSynth.setUserInstrument(spUserInstrument);
            }
            outSynth.setSendModeOnUponPlay(spSendModeOnUponPlay);
            outSynth.setAudioLatency(spAudioLatency);
            return outSynth;
        }
    }

}

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
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.midi.api.synths.GSSynth;
import org.jjazz.midi.api.Instrument;
import org.jjazz.midi.api.InstrumentBank;
import org.jjazz.midi.api.MidiSynth;
import org.jjazz.midi.api.MidiSynth.Finder;
import org.jjazz.midi.api.synths.GM1Instrument;
import org.jjazz.midi.api.synths.GM2Synth;
import org.jjazz.midi.api.synths.GMSynth;
import org.jjazz.midi.api.synths.NotSetBank;
import org.jjazz.midi.api.synths.StdSynth;
import org.jjazz.midi.api.synths.XGSynth;
import org.jjazz.midi.spi.MidiSynthFileReader;
import org.jjazz.midiconverters.api.ConverterManager;
import org.jjazz.midiconverters.api.StdInstrumentConverter;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.api.UserRhythmVoice;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.util.api.Utilities;

/**
 * An OutputSynth is made of one or more MidiSynth instances describing the available instruments.
 * <p>
 */
public class OutputSynth implements Serializable
{

    private final String name;
    private final List<MidiSynth> midiSynths = new ArrayList<>();
    private File file;

    private static final Logger LOGGER = Logger.getLogger(OutputSynth.class.getSimpleName());
    private final transient PropertyChangeSupport pcs = new java.beans.PropertyChangeSupport(this);


    /**
     * Construct an OutputSynth named "GM output synth" with the GMSynth instance.
     */
    public OutputSynth()
    {
        this("GM output synth", Arrays.asList(GMSynth.getInstance()));
    }

    /**
     * Construct an OutputSynth with specified name and MidiSynths.
     * <p>
     * @param name
     * @param synths Can't be empty
     */
    public OutputSynth(String name, List<MidiSynth> synths)
    {
        Preconditions.checkArgument(name != null && !name.isBlank());
        Preconditions.checkArgument(synths != null && !synths.isEmpty());
        this.name = name;
        this.midiSynths.addAll(synths);
    }

    /**
     * Construct an OutputSynth from the (non-empty) MidiSynths found in the specified instrument definition file (.ins).
     * .<p>
     * @param name
     * @param file
     * @throws java.io.IOException
     */
    public OutputSynth(String name, File file) throws IOException
    {
        Preconditions.checkArgument(name != null && !name.isBlank());
        this.name = name;

        var reader = MidiSynthFileReader.getReader(Utilities.getExtension(file.getName()));
        if (reader == null)
        {
            throw new IOException("No MidiSynthFileReader instance found to read file=" + file.getAbsolutePath());
        }
        // Rread the file and add the non-empty synths
        FileInputStream fis = new FileInputStream(file);
        var synths = reader.readSynthsFromStream(fis, file);    // Can raise exception
        synths.stream()
                .filter(s -> s.getNbInstruments() > 0)
                .forEach(s -> midiSynths.add(s));


        this.file = file;
    }

    public String getName()
    {
        return name;
    }

    /**
     * Get the associated MidiSynth definition file (.ins).
     *
     * @return Null if this instance was not created using an instrument definitiion file.
     * @see #OutputSynth(java.lang.String, java.io.File)
     */
    public File getFile()
    {
        return file;
    }


    /**
     * Get the MidiSynths of this OutputSynth.
     *
     * @return Can be an empty list.
     */
    public List<MidiSynth> getMidiSynths()
    {
        return new ArrayList<>(midiSynths);
    }


    /**
     * Get the instruments that should be used in the specified MidiMix to be consistent with this OutputSynth.
     * <p>
     *
     * @param mm
     * @return The channels which need to be fixed and the associated new instrument. HashMap can be empty.
     */
    public HashMap<Integer, Instrument> getNeedFixInstruments(MidiMix mm)
    {
        HashMap<Integer, Instrument> res = new HashMap<>();
        for (int channel : mm.getUsedChannels())
        {
            Instrument ins = mm.getInstrumentMixFromChannel(channel).getInstrument(); // Can be the VoidInstrument
            if (!contains(ins))
            {
                RhythmVoice rv = mm.getRhythmVoice(channel);
                Instrument newIns = findInstrument(rv);     // Can be the VoidInstrument
                if (newIns != ins)
                {
                    res.put(channel, newIns);
                }
            }
        }
        return res;
    }

    /**
     * Find an instrument from this OutputSynth matching, as much as possible, the specified rhythm voice's preferred instrument.
     * <p>
     * <p>
     * Search a matching instrument :<br>
     * - Using custom converters<br>
     * - then search for the instrument in the MidiSynths<br>
     * - then search using the GM1 substitute, remap table, and substitute family<p>
     * <p>
     *
     * @param rv Must be a standard RhythmVoice (not a UserRhythmVoice)
     * @return Can't be null. It may be the VoidInstrument for drums/percussion.
     */
    public Instrument findInstrument(RhythmVoice rv)
    {
        Instrument ins = null;

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
            ins = rv.isDrums() ? GMSynth.getInstance().getVoidInstrument() : rv.getType().getDefaultInstrument();
            LOGGER.log(Level.FINE, "findInstrument() rv preferred instrument=VoidInstrument, return ins=" + ins);   //NOI18N
            return ins;
        }


        InstrumentBank<?> rvInsBank = rvIns.getBank();
        MidiSynth rvInsSynth = (rvInsBank != null) ? rvInsBank.getMidiSynth() : null;

        
        
        if (isStdBank(rvInsBank))
        {
            // ins is GM/GM2/XG    
            if 
        }


        if (isStdBank(rvInsBank))
        {
            ins = StdInstrumentConverter.getInstance().convertInstrument(rvIns, null, compatibleStdBanks);
            if (ins != null)
            {
                LOGGER.log(Level.FINE, "findInstrument()    found in compatibleStdBanks, ins={0}", ins.toLongString());   //NOI18N
                return ins;
            }
        }


        // If 
        // Try first with custom converters for our midiSynths
        ConverterManager cm = ConverterManager.getInstance();
        for (MidiSynth synth : midiSynths)
        {
            ins = cm.convertInstrument(rvIns, synth, null);
            if (ins != null)
            {
                LOGGER.log(Level.FINE, "findInstrument()    Found in custom synth using custom conversion {0}, ins={1}", new Object[]   //NOI18N
                {
                    synth.getName(), ins.toLongString()
                });
                return ins;
            }
        }


        //
        // If we're here we need to do a conversion
        //
        // If rvIns is a standard instrument (but not from compatibleStdBanks), use the special standard converter
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
            for (MidiSynth synth : midiSynths)
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
            for (MidiSynth synth : midiSynths)
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
            for (MidiSynth synth : midiSynths)
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
            for (MidiSynth synth : midiSynths)
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
     * Check whether this output synth contains this instrument.
     *
     * @param ins
     * @return
     */
    public boolean contains(Instrument ins)
    {
        InstrumentBank<?> bank = ins.getBank();
        if (bank != null)
        {
            if (compatibleStdBanks.contains(bank))
            {
                return true;
            }
            MidiSynth synth = bank.getMidiSynth();
            if (synth != null)
            {
                return midiSynths.contains(synth);
            }
        }
        return false;
    }

    /**
     * Return the first drums instrument found.
     *
     * @return Can be the VoidInstrument if no drums instrument found.
     */
    public Instrument getDrumsInstrumentSample()
    {
        Instrument ins = StdSynth.getInstance().getVoidInstrument();
        for (MidiSynth synth : midiSynths)
        {
            List<Instrument> drumsInstruments = synth.getDrumsInstruments();
            if (!drumsInstruments.isEmpty())
            {
                ins = drumsInstruments.get(0);
                break;
            }
        }
        if (ins == StdSynth.getInstance().getVoidInstrument())
        {
            // Try the standard banks
            for (var bank : compatibleStdBanks)
            {
                var drumsInstruments = bank.getDrumsInstruments();
                if (!drumsInstruments.isEmpty())
                {
                    ins = drumsInstruments.get(0);
                    break;
                }
            }
        }
        return ins;
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
            throw new IllegalArgumentException("f=" + f);   //NOI18N
        }
        LOGGER.fine("saveToFile() f=" + f.getAbsolutePath());   //NOI18N

        File prevFile = getFile();
        setFile(f);

        try ( FileOutputStream fos = new FileOutputStream(f))
        {
            XStream xstream = new XStream();
            xstream.alias("OutputSynth", OutputSynth.class);
            Writer w = new BufferedWriter(new OutputStreamWriter(fos, "UTF-8"));        // Needed to support special/accented chars
            xstream.toXML(this, w);
        } catch (IOException e)
        {
            setFile(prevFile);
            throw e;
        } catch (XStreamException e)
        {
            setFile(prevFile);
            LOGGER.warning("saveToFile() exception=" + e.getMessage());   //NOI18N
            // Translate into an IOException to be handled by the Netbeans framework 
            throw new IOException("XStream XML marshalling error", e);
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

    private boolean isStdBank(InstrumentBank<?> bank)
    {
        return bank == GMSynth.getInstance().getGM1Bank() || bank == GM2Synth.getInstance().getGM2Bank() || bank == XGSynth.getInstance().getXGBank() || bank == GSSynth.getInstance().getGSBank();
    }

    private List<InstrumentBank<?>> getStdBanks()
    {
        return Arrays.asList(GMSynth.getInstance().getGM1Bank(), GM2Synth.getInstance().getGM2Bank(), XGSynth.getInstance().getXGBank(), GSSynth.getInstance().getGSBank());
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

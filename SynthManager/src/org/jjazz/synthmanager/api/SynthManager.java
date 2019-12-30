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
package org.jjazz.synthmanager.api;

import org.jjazz.synthmanager.spi.MidiSynthProvider;
import org.jjazz.midi.InstrumentBank;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import org.jjazz.defaultinstruments.JJazzSynth;
import org.jjazz.filedirectorymanager.FileDirectoryManager;
import org.jjazz.midi.GM1Bank;
import org.jjazz.midi.GMSynth;
import org.jjazz.midi.Instrument;
import org.jjazz.midi.MidiSynth;
import org.jjazz.util.Utilities;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.ServiceProvider;

/**
 * A class to maintain the list of usable MidiSynths for the application, which includes the ConnectedSynth, i.e. the synth
 * actually connected to the output of JJazzLab via Midi.
 * <p>
 * By default the usable synths contain the GMSynth and all the builtin synths of the MidiSynthProvider instances found in the
 * global lookup. User can add MidiSynths. The list of user added synths is saved as properties.
 */
public class SynthManager
{

    public static final String MIDISYNTH_FILES_DEST_DIRNAME = "MidiSynthFiles";
    public static final String MIDISYNTH_FILES_RESOURCE_ZIP = "/resources/MidiSynthFiles.zip";

    /**
     * If user synth is added, oldValue=null, newValue=synth, if synth is removed it is the opposite.
     */
    public static final String PROP_USER_SYNTH = "PropUserSynth";
    public static final String PROP_GM1_DELEGATE_BANK = "PropGM1DelegateBank";
    /**
     * Used internally to store property
     */
    private static final String PROP_NB_USER_SYNTHS = "PropNbUserSynths";

    private static SynthManager INSTANCE;
    private ArrayList<MidiSynth> builtinSynths;
    private ArrayList<MidiSynth> userSynths;
    private ConnectedSynth connectedSynth;
    /**
     * Store the Provider for each produced synth.
     */
    private HashMap<MidiSynth, MidiSynthProvider> mapSynthProvider = new HashMap<>();

    /**
     * The Preferences of this object.
     */
    private static Preferences prefs = NbPreferences.forModule(SynthManager.class);
    private final transient PropertyChangeSupport pcs = new java.beans.PropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(SynthManager.class.getSimpleName());

    public static SynthManager getInstance()
    {
        synchronized (SynthManager.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new SynthManager();
            }
        }
        return INSTANCE;
    }

    private SynthManager()
    {
        builtinSynths = new ArrayList<>();
        userSynths = new ArrayList<>();
        builtinSynths.add(GMSynth.getInstance());
        builtinSynths.add(JJazzSynth.getInstance());

        for (MidiSynthProvider p : MidiSynthProvider.Utilities.getProviders())
        {
            for (MidiSynth synth : p.getBuiltinMidiSynths())
            {
                builtinSynths.add(synth);
                mapSynthProvider.put(synth, p);
            }
        }
        restoreUserSynthsFromProperties();
        restoreGM1BankDelegateFromProperties();
    }

    /**
     * This is the synth actually connected to the output of JJazzLab via Midi.
     *
     * @return Can't be null
     */
    public ConnectedSynth getConnectedSynth()
    {
        return connectedSynth;
    }

    /**
     * Set the synth actually connected to the output of JJazzLab via Midi.
     *
     * @param synth Can't be null
     */
    public void setConnectedSynth(ConnectedSynth synth)
    {
        if (synth == null)
        {
            throw new IllegalArgumentException("synth=" + synth);
        }
        this.connectedSynth = synth;
    }

    /**
     * Add a synth.
     *
     * @param provider
     * @param synth
     */
    public void addUserSynth(MidiSynthProvider provider, MidiSynth synth)
    {
        if (!userSynths.contains(synth))
        {
            userSynths.add(synth);
            mapSynthProvider.put(synth, provider);
            updateUserSynthProperties();
            pcs.firePropertyChange(PROP_USER_SYNTH, null, synth);
        }
    }

    public File getMidiSynthFilesDir()
    {
        FileDirectoryManager fdm = FileDirectoryManager.getInstance();
        File rDir = fdm.getAppConfigDirectory(MIDISYNTH_FILES_DEST_DIRNAME);
        assert rDir.isDirectory() : "rDir=" + rDir;
        File[] files = rDir.listFiles();
        if (files.length == 0)
        {
            files = copyBuiltinResourceFiles(rDir);
        }
        return rDir;
    }

    public void removeUserSynth(MidiSynth synth)
    {
        if (userSynths.remove(synth))
        {
            mapSynthProvider.remove(synth);
            updateUserSynthProperties();
            pcs.firePropertyChange(PROP_USER_SYNTH, synth, null);
        }
    }

    /**
     * Get the MidiSynth with specified name (ignore case).
     *
     * @param synthName
     * @return Null if not found in the default or user synths.
     */
    @SuppressWarnings(
            {
                "rawtypes"
            })

    public MidiSynth getSynth(String synthName)
    {
        for (MidiSynth synth : userSynths)
        {
            if (synth.getName().equalsIgnoreCase(synthName))
            {
                return synth;
            }
        }
        for (MidiSynth synth : builtinSynths)
        {
            if (synth.getName().equalsIgnoreCase(synthName))
            {
                return synth;
            }
        }
        return null;
    }

    public List<MidiSynth> getBuiltinSynths()
    {
        return new ArrayList<>(builtinSynths);
    }

    public List<MidiSynth> getUserSynths()
    {
        return new ArrayList<>(userSynths);
    }

    /**
     * Get all the active synths, builtin ones and user added ones.
     *
     * @return
     */
    public List<MidiSynth> getSynths()
    {
        ArrayList<MidiSynth> res = new ArrayList<>(builtinSynths);
        res.addAll(userSynths);
        return res;
    }

    /**
     * Get the MidiSynthProvider for the specified synth
     *
     * @param synth
     * @return Null if synth is not part of this object.
     */
    public MidiSynthProvider getProvider(MidiSynth synth)
    {
        return mapSynthProvider.get(synth);
    }

    /**
     * Set the InstrumentBank delegate of the default JJazz GM1Bank.
     * <p>
     * Practically keep using the GM1Bank but reuse the LSB/MSB/BANK_SELECT_METHOD of the specified bank.
     *
     * @param provider If the bank parameter is null, this parameter is ignored.
     * @param bank     If null no delegate is set. Must not be the JJazz GM1Bank it self.
     */
    public void setGM1DelegateBank(MidiSynthProvider provider, InstrumentBank<?> bank)
    {
        if ((provider == null && bank != null) || bank == GMSynth.getInstance().getGM1Bank())
        {
            throw new IllegalArgumentException("provider=" + provider + " bank=" + bank);
        }
        // Update the GM1Bank parameters
        updateGM1Bank(bank);
        // Update the property
        InstrumentBank<?> old = getGM1DelegateBank();
        String s = (bank == null) ? "NOT_SET" : provider.saveSynthAsString(bank.getMidiSynth()) + "##bankName=" + bank.getName();
        prefs.put(PROP_GM1_DELEGATE_BANK, s);
        pcs.firePropertyChange(PROP_GM1_DELEGATE_BANK, old, bank);
    }

    /**
     * Get the InstrumentBank delegate for the GM1 Bank.
     *
     * @return Can be null if no delegate set.
     */
    public InstrumentBank<?> getGM1DelegateBank()
    {
        String s = prefs.get(PROP_GM1_DELEGATE_BANK, "NOT_SET");
        if ("NOT_SET".equals(s.trim()))
        {
            return null;
        }
        String[] strs = s.split("##bankName=");
        if (strs.length != 2)
        {
            LOGGER.warning("Invalid value for property " + PROP_GM1_DELEGATE_BANK + ": " + s + ". Property will be removed.");
            prefs.remove(PROP_GM1_DELEGATE_BANK);
            return null;
        }
        String synthStringSave = strs[0].trim();
        String bankName = strs[1].trim();
        MidiSynth synth = null;
        // Try all the MidiSynthProviders until one can reuse the property string
        for (MidiSynthProvider p : MidiSynthProvider.Utilities.getProviders())
        {
            synth = p.getSynthFromString(synthStringSave);
            if (synth != null)
            {
                break;
            }
        }
        if (synth == null)
        {
            LOGGER.warning("No MidiSynth could be created from MidiSynthManager property: " + s + ". Property will be removed.");
            prefs.remove(PROP_GM1_DELEGATE_BANK);
            return null;
        }
        InstrumentBank<?> bank = synth.getBank(bankName);
        if (bank == null)
        {
            LOGGER.warning("Bank not found in MidiSynth in MidiSynthManager property: " + s + ". Property will be removed.");
            prefs.remove(PROP_GM1_DELEGATE_BANK);
        }
        return bank;
    }

    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }

    /**
     * Force the standard GM1Bank to use the bank select parameters of the specified bank.
     *
     * @param bank If null, restore the default bank select parameters.
     */
    private void updateGM1Bank(InstrumentBank<?> bank)
    {
        GM1Bank gm1bank = GMSynth.getInstance().getGM1Bank();
        gm1bank.setBankSelectLsb(bank == null ? GM1Bank.DEFAULT_BANK_SELECT_LSB : bank.getDefaultBankSelectLSB());
        gm1bank.setBankSelectMsb(bank == null ? GM1Bank.DEFAULT_BANK_SELECT_MSB : bank.getDefaultBankSelectMSB());
        gm1bank.setBankSelectMethod(bank == null ? GM1Bank.DEFAULT_BANK_SELECT_METHOD : bank.getBankSelectMethod());
    }

    private void restoreGM1BankDelegateFromProperties()
    {
        InstrumentBank<?> bank = getGM1DelegateBank();
        updateGM1Bank(bank);
    }

    private void updateUserSynthProperties()
    {
        // Clean the previous properties (needed when the nb of user synths has decreased)
        int oldNbSynths = prefs.getInt(PROP_NB_USER_SYNTHS, 0);
        for (int i = 0; i < oldNbSynths; i++)
        {
            prefs.remove(PROP_USER_SYNTH + i);
        }
        // Save the new nb of synths
        prefs.putInt(PROP_NB_USER_SYNTHS, userSynths.size());
        // And recreate the needed properties
        int i = 0;
        for (MidiSynth synth : userSynths)
        {
            MidiSynthProvider provider = mapSynthProvider.get(synth);
            prefs.put(PROP_USER_SYNTH + i, provider.saveSynthAsString(synth));
            i++;
        }
    }

    private void restoreUserSynthsFromProperties()
    {
        int nbSynths = prefs.getInt(PROP_NB_USER_SYNTHS, 0);
        List<MidiSynthProvider> providers = MidiSynthProvider.Utilities.getProviders();
        for (int i = 0; i < nbSynths; i++)
        {
            // Loop on each saved synth
            String s = prefs.get(PROP_USER_SYNTH + i, null);
            if (s == null)
            {
                continue;
            }
            MidiSynth synth = null;
            // Try all the MidiSynthProviders until one can reuse the property string
            for (MidiSynthProvider p : providers)
            {
                synth = p.getSynthFromString(s);
                if (synth != null)
                {
                    // Don't use addUserSynth(), otherwise recursive calls !
                    userSynths.add(synth);
                    mapSynthProvider.put(synth, p);
                    break;
                }
            }
            if (synth == null)
            {
                LOGGER.warning("No MidiSynth could be created from MidiSynthManager property: " + s + ". Property will be removed.");
                prefs.remove(PROP_USER_SYNTH + i);
            }
        }
    }

    /**
     * Copy the builtin Midi synth files within the JAR to destPath.
     * <p>
     *
     * @param destPath
     *
     */
    private File[] copyBuiltinResourceFiles(File destDir)
    {
        List<File> res = Utilities.extractZipResource(getClass(), MIDISYNTH_FILES_RESOURCE_ZIP, destDir.toPath());
        if (res.isEmpty())
        {
            LOGGER.log(Level.WARNING, "copyBuiltinResourceFiles() {0}", new Object[]
            {
                "No builtin Midi synth files found"
            });
        }
        return res.toArray(new File[0]);
    }

    // An adapter to use this object to fulfill the instrumentBankFinder contract
    @ServiceProvider(service = Instrument.BankFinder.class)
    static public class BankFinderAdapter implements Instrument.BankFinder
    {

        @Override
        public InstrumentBank<?> getBank(String synthName, String bankName)
        {
            MidiSynth synth = getInstance().getSynth(synthName);
            if (synth == null)
            {
                return null;
            }
            InstrumentBank<?> bank = synth.getBank(bankName);
            return bank;
        }
    }

}

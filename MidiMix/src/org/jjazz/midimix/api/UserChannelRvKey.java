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
package org.jjazz.midimix.api;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.List;
import java.util.prefs.Preferences;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.midi.api.synths.StdSynth;
import org.jjazz.midi.api.InstrumentSettings;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midi.api.synths.Family;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmFeatures;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.upgrade.api.UpgradeManager;
import org.jjazz.upgrade.spi.UpgradeTask;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.ServiceProvider;

/**
 * A special RhythmVoice instance used by MidiMix as the RhythmVoice key for the special User channel.
 */
public class UserChannelRvKey extends RhythmVoice
{

    private static final String PREF_USER_CHANNEL = "PrefUserChannel";
    private static UserChannelRvKey INSTANCE;
    private static Preferences prefs = NbPreferences.forModule(UserChannelRvKey.class);

    static public UserChannelRvKey getInstance()
    {
        synchronized (UserChannelRvKey.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new UserChannelRvKey();
            }
        }
        return INSTANCE;
    }

    private UserChannelRvKey()
    {
        super(new RhythmNotUsed(), Type.CHORD1, "User", StdSynth.getInstance().getGM1Bank().getDefaultInstrument(Family.Piano), new InstrumentSettings(), 0);
    }

    /**
     * The default Midi channel to be used when User channel is enabled.
     * <p>
     * 0 by default.
     *
     * @return
     */
    public int getPreferredUserChannel()
    {
        return prefs.getInt(PREF_USER_CHANNEL, 0);
    }

    /**
     * Set the preferred Midi channel for the user channel.
     * <p>
     */
    public void setPreferredUserChannel(int c)
    {
        if (!MidiConst.checkMidiChannel(c))
        {
            throw new IllegalArgumentException("c=" + c);   //NOI18N
        }
        prefs.putInt(PREF_USER_CHANNEL, c);
    }

    // =====================================================================================
    // Upgrade Task
    // =====================================================================================
    @ServiceProvider(service = UpgradeTask.class)
    static public class RestoreSettingsTask implements UpgradeTask
    {

        @Override
        public void upgrade(String oldVersion)
        {
            UpgradeManager um = UpgradeManager.getInstance();
            um.duplicateOldPreferences(prefs);
        }

    }

    static private class RhythmNotUsed implements Rhythm
    {

        @Override
        public RhythmFeatures getFeatures()
        {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void loadResources() throws MusicGenerationException
        {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void releaseResources()
        {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean isResourcesLoaded()
        {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public List<RhythmVoice> getRhythmVoices()
        {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public List<RhythmParameter<?>> getRhythmParameters()
        {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public File getFile()
        {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public String getUniqueId()
        {
            return "RhythmNotUsedId";
        }

        @Override
        public String getDescription()
        {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public int getPreferredTempo()
        {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public TimeSignature getTimeSignature()
        {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public String getName()
        {
            return "RhythmNotUsed";
        }

        @Override
        public String getAuthor()
        {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void addPropertyChangeListener(PropertyChangeListener l)
        {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void removePropertyChangeListener(PropertyChangeListener l)
        {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

    }

}

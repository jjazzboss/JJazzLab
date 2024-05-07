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
package org.jjazz.midimix.api;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.List;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.midi.api.synths.InstrumentFamily;
import org.jjazz.midi.api.synths.GMSynth;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmFeatures;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.rhythm.api.RhythmVoice;

/**
 * A special RhythmVoice subtype for user phrases.
 */
public class UserRhythmVoice extends RhythmVoice
{

    public static final int DEFAULT_USER_PHRASE_CHANNEL = 0;
    public static final Rhythm CONTAINER = new UserDummyRhythm();


    /**
     * Create a melodic UserRhythmVoice.
     *
     * @param name Can't be null
     */
    public UserRhythmVoice(String name)
    {
        super(CONTAINER, Type.PHRASE1, name, GMSynth.getInstance().getGM1Bank().getDefaultInstrument(InstrumentFamily.Piano), DEFAULT_USER_PHRASE_CHANNEL);
    }

    /**
     * Create a drums UserRhythmVoice.
     *
     * @param name    Can't be null
     * @param drumKit Can't be null
     */
    public UserRhythmVoice(String name, DrumKit drumKit)
    {
        super(drumKit, CONTAINER, Type.DRUMS, name, GMSynth.getInstance().getVoidInstrument(), DEFAULT_USER_PHRASE_CHANNEL);
    }

    @Override
    public String toString()
    {
        return "RvUser[" + getName() + "]";
    }

    static private class UserDummyRhythm implements Rhythm
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
            return "UserRhythmVoiceRhythmId";
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
            return "UserPhrase";
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

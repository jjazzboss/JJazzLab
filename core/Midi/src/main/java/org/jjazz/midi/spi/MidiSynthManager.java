/*
 * 
 *   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 *   Copyright @2019 Jerome Lelasseux. All rights reserved.
 * 
 *   This file is part of the JJazzLab software.
 *    
 *   JJazzLab is free software: you can redistribute it and/or modify
 *   it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *   as published by the Free Software Foundation, either version 3 of the License, 
 *   or (at your option) any later version.
 * 
 *   JJazzLab is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *  
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *  
 *   Contributor(s): 
 * 
 */
package org.jjazz.midi.spi;

import com.google.common.base.Preconditions;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.midi.api.MidiSynth;
import org.jjazz.midi.api.synths.DefaultMidiSynthManager;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

/**
 * Manage MidiSynth instances.
 */
public interface MidiSynthManager
{

    /**
     * Property change event fired when a MidiSynth is added or removed.
     * <p>
     * If added: oldValue=null, newValue=added MidiSynth<br>
     * If removed: oldValue=removed MidiSynth, newValue=null<br>
     */
    public static String PROP_MIDISYNTH_LIST = "PropSynthList";

    /**
     * Get the default implementation in the global lookup, or if not found return the DefaultMidiSynthManager instance.
     *
     * @return
     */
    static public MidiSynthManager getDefault()
    {
        var res = Lookup.getDefault().lookup(MidiSynthManager.class);
        if (res == null)
        {
            res = DefaultMidiSynthManager.getInstance();
        }
        return res;
    }

    /**
     * Search a MidiSynth with the specified name.
     *
     * @param name
     * @return Can be null.
     */
    MidiSynth getMidiSynth(String name);

    /**
     * The list of MidiSynths.
     * <p>
     *
     * @return Can be empty.
     */
    List<MidiSynth> getMidiSynths();

    /**
     * The list of MidiSynths which match the specified criteria.
     * <p>
     *
     * @param tester
     * @return An unmodifiable list, which can be empty.
     */
    List<MidiSynth> getMidiSynths(Predicate<MidiSynth> tester);

    /**
     * Add a MidiSynth.
     * <p>
     *
     * @param midiSynth
     * @return True if midiSynth was successfully added, false if midiSynth was already referenced by the MidiSynthManager.
     */
    boolean addMidiSynth(MidiSynth midiSynth);

    /**
     * Remove the specified MidiSynth.
     * <p>
     * @param midiSynth
     * @return
     */
    boolean removeMidiSynth(MidiSynth midiSynth);




    /**
     * Add PropertyChangeListener.
     *
     * @param listener
     */
    void addPropertyChangeListener(PropertyChangeListener listener);

    /**
     * Remove PropertyChangeListener.
     *
     * @param listener
     */
    void removePropertyChangeListener(PropertyChangeListener listener);


    /**
     * Read one MidiSynth from an JJazzLab internal .ins resource file.
     *
     * @param clazz
     * @param insResourcePath Resource path relative to clazz. Must contain only 1 MidiSynth
     * @return Can't be null
     * @throws IllegalStateException If resource could not be read
     */
    public static MidiSynth loadFromResource(Class clazz, String insResourcePath)
    {
        MidiSynth res;
        InputStream is = clazz.getResourceAsStream(insResourcePath);
        assert is != null : "insResourcePath=" + insResourcePath;
        MidiSynthFileReader r = MidiSynthFileReader.getReader("ins");
        assert r != null;
        try
        {
            List<MidiSynth> synths = r.readSynthsFromStream(is, null);
            assert synths.size() == 1;
            res = synths.get(0);
        }
        catch (IOException ex)
        {
            throw new IllegalStateException("Unexpected error", ex);
        }
        return res;
    }

    // ===============================================================================
    // Inner classes
    // ===============================================================================

    @ServiceProvider(service = MidiSynth.Finder.class)
    static public class SynthFinder implements MidiSynth.Finder
    {

        private static final Logger LOGGER = Logger.getLogger(SynthFinder.class.getSimpleName());

        /**
         * Search the MidiSynthManager instance.
         *
         * @param synthName
         * @param synthFile
         * @return
         */
        @Override
        public MidiSynth getMidiSynth(String synthName, File synthFile)
        {
            Preconditions.checkNotNull(synthName);

            var msm = MidiSynthManager.getDefault();
            MidiSynth res = msm.getMidiSynth(synthName);

            if (res == null && synthFile != null)
            {
                try
                {
                    // Not created yet, load it and add it to the database
                    res = MidiSynth.loadFromFile(synthFile);    // throws IOException
                    msm.addMidiSynth(res);
                }
                catch (IOException ex)
                {
                    LOGGER.log(Level.WARNING, "getMidiSynth() can''t load MidiSynth ex={0}", ex.getMessage());
                }
            }

            return res;
        }
    }

}

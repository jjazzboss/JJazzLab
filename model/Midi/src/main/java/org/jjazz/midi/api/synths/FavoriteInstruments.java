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
package org.jjazz.midi.api.synths;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.midi.api.Instrument;
import org.jjazz.midi.api.MidiSynth;
import org.openide.util.NbPreferences;

/**
 * Store favorite instruments as preferences.
 */
public class FavoriteInstruments
{

    private static FavoriteInstruments INSTANCE;
    /**
     * oldValue=instrument if removed, newValue=instrument if added.
     */
    public static String PROP_FAVORITE_INSTRUMENT = "PropFavoriteInstrument";
    /**
     * Used internally to store the nb of favorites
     */
    private static final String PROP_NB_FAVORITE_INSTRUMENTS = "PropNbFavoriteInstruments";
    private ArrayList<Instrument> instruments = new ArrayList<>();
    /**
     * The Preferences of this object.
     */
    private static Preferences prefs = NbPreferences.forModule(FavoriteInstruments.class);
    private SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(FavoriteInstruments.class.getSimpleName());

    public static FavoriteInstruments getInstance()
    {
        synchronized (FavoriteInstruments.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new FavoriteInstruments();
            }
        }
        return INSTANCE;
    }

    private FavoriteInstruments()
    {
        restoreFavoriteInstrumentsFromProperties();
    }

    /**
     * Add a favorite instrument.
     *
     * @param ins
     * @return True if added successfully (ins was not already present).
     */
    public boolean addInstrument(Instrument ins)
    {
        if (ins == null)
        {
            throw new NullPointerException("ins=" + ins);   
        }
        if (!instruments.contains(ins))
        {
            instruments.add(ins);
            updateProperties();
            pcs.firePropertyChange(PROP_FAVORITE_INSTRUMENT, null, ins);
            return true;
        }
        return false;
    }

    /**
     * Remove a favorite instrument.
     *
     * @param ins
     * @return True if removed successfully (ins was present).
     */
    public boolean removeInstrument(Instrument ins)
    {
        if (ins == null)
        {
            throw new NullPointerException("ins");   
        }
        if (instruments.remove(ins))
        {
            updateProperties();
            pcs.firePropertyChange(PROP_FAVORITE_INSTRUMENT, ins, null);
            return true;
        }
        return false;
    }

    public boolean contains(Instrument ins)
    {
        return instruments.contains(ins);
    }

    /**
     * All the favorite instruments.
     *
     * @return
     */
    public List<Instrument> getInstruments()
    {
        return new ArrayList<>(instruments);
    }

    /**
     * The favorite instruments for the specified MidiSynth.
     *
     * @param synth
     * @return
     */
    public List<Instrument> getInstruments(MidiSynth synth)
    {
        if (synth == null)
        {
            throw new NullPointerException("synth");   
        }
        ArrayList<Instrument> res = new ArrayList<>();
        for (Instrument ins : instruments)
        {
            if (ins.getBank().getMidiSynth() == synth)
            {
                res.add(ins);
            }
        }
        return res;
    }

    public void addPropertyListener(PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }

    // ==============================================================
    // Private methods
    // ==============================================================    
    private void updateProperties()
    {
        // Clean the previous properties (needed when the nb of instruments has decreased)
        int oldNbSynths = prefs.getInt(PROP_NB_FAVORITE_INSTRUMENTS, 0);
        for (int i = 0; i < oldNbSynths; i++)
        {
            prefs.remove(PROP_FAVORITE_INSTRUMENT + i);
        }
        // Save the new nb of instruments
        prefs.putInt(PROP_NB_FAVORITE_INSTRUMENTS, instruments.size());
        // And recreate the needed properties
        int i = 0;
        for (Instrument ins : instruments)
        {
            prefs.put(PROP_FAVORITE_INSTRUMENT + i, ins.saveAsString());
            i++;
        }
    }

    private void restoreFavoriteInstrumentsFromProperties()
    {
        int nbSynths = prefs.getInt(PROP_NB_FAVORITE_INSTRUMENTS, 0);
        for (int i = 0; i < nbSynths; i++)
        {
            // Loop on each saved synth
            String s = prefs.get(PROP_FAVORITE_INSTRUMENT + i, null);
            if (s == null)
            {
                continue;
            }
            Instrument ins = Instrument.loadFromString(s);
            if (ins != null)
            {
                instruments.add(ins);
            } else
            {
                LOGGER.log(Level.WARNING, "No instrument could be created from FavoriteInstruments property: {0}. Property will be removed.", s);   
                prefs.remove(PROP_FAVORITE_INSTRUMENT + i);
            }
        }
    }


    // =====================================================================================
    // Upgrade Task: provided by JJazzMidiSystem
    // =====================================================================================
  


}

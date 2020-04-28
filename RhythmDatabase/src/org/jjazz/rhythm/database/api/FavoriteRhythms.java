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
package org.jjazz.rhythm.database.api;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.spi.RhythmProvider;
import org.openide.util.NbPreferences;

/**
 * Store the favorite rhythms as preferences.
 */
public class FavoriteRhythms
{

    private static FavoriteRhythms INSTANCE;
    /**
     * oldValue=rhythm if removed, newValue=rhythm if added.
     */
    public static String PROP_FAVORITE_RHYTHM = "PropFavoriteRhythm";
    /**
     * Used internally to store the nb of favorites
     */
    private static final String PROP_NB_FAVORITE_RHYTHMS = "PropNbFavoriteRhythms";
    private ArrayList<Rhythm> rhythms = new ArrayList<>();
    private static Preferences prefs = NbPreferences.forModule(FavoriteRhythms.class);
    private SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(FavoriteRhythms.class.getSimpleName());

    public static FavoriteRhythms getInstance()
    {
        synchronized (FavoriteRhythms.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new FavoriteRhythms();
            }
        }
        return INSTANCE;
    }

    private FavoriteRhythms()
    {
        restoreFavoriteRhythmsFromProperties();
    }

    /**
     * Add a favorite rhythm.
     *
     * @param r
     * @return True if added successfully (r was not already present).
     */
    public boolean addRhythm(Rhythm r)
    {
        if (r == null)
        {
            throw new NullPointerException("r=" + r);
        }
        if (!rhythms.contains(r))
        {
            rhythms.add(r);
            updateProperties();
            pcs.firePropertyChange(PROP_FAVORITE_RHYTHM, null, r);
            return true;
        }
        return false;
    }

    /**
     * Remove a favorite rhythm.
     *
     * @param r
     * @return True if removed successfully (r was present).
     */
    public boolean removeRhythm(Rhythm r)
    {
        if (r == null)
        {
            throw new NullPointerException("r");
        }
        if (rhythms.remove(r))
        {
            updateProperties();
            pcs.firePropertyChange(PROP_FAVORITE_RHYTHM, r, null);
            return true;
        }
        return false;
    }

    public boolean contains(Rhythm r)
    {
        return rhythms.contains(r);
    }

    /**
     * All the favorite rhythms.
     *
     * @return
     */
    public List<Rhythm> getRhythms()
    {
        return new ArrayList<>(rhythms);
    }

    /**
     * The favorite rhythms for the specified RhythmProvider.
     *
     * @param rp
     * @return
     */
    public List<Rhythm> getRhythms(RhythmProvider rp)
    {
        if (rp == null)
        {
            throw new NullPointerException("rp");
        }
        RhythmDatabase rdb = RhythmDatabase.getDefault();
        ArrayList<Rhythm> res = new ArrayList<>();
        for (Rhythm r : rhythms)
        {
            if (rdb.getRhythmProvider(r) == rp)
            {
                res.add(r);
            }
        }
        return res;
    }

    /**
     * The favorite rhythms for the specified TimeSignature.
     *
     * @param ts
     * @return
     */
    public List<Rhythm> getRhythms(TimeSignature ts)
    {
        if (ts == null)
        {
            throw new NullPointerException("ts");
        }
        ArrayList<Rhythm> res = new ArrayList<>();
        for (Rhythm r : rhythms)
        {
            if (r.getTimeSignature().equals(ts))
            {
                res.add(r);
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
        // Clean the previous properties (needed when the nb of rhythms has decreased)
        int oldNbRhythms = prefs.getInt(PROP_NB_FAVORITE_RHYTHMS, 0);
        for (int i = 0; i < oldNbRhythms; i++)
        {
            prefs.remove(PROP_FAVORITE_RHYTHM + i);
        }
        // Save the new nb of rhythms
        prefs.putInt(PROP_NB_FAVORITE_RHYTHMS, rhythms.size());
        // And recreate the needed properties
        int i = 0;
        for (Rhythm r : rhythms)
        {
            prefs.put(PROP_FAVORITE_RHYTHM + i, r.getUniqueId());
            i++;
        }
    }

    private void restoreFavoriteRhythmsFromProperties()
    {
        int nbRhythms = prefs.getInt(PROP_NB_FAVORITE_RHYTHMS, 0);
        for (int i = 0; i < nbRhythms; i++)
        {
            // Loop on each saved rhythm
            String rId = prefs.get(PROP_FAVORITE_RHYTHM + i, null);
            if (rId == null)
            {
                continue;
            }
            RhythmDatabase rdb = RhythmDatabase.getDefault();
            Rhythm r = rdb.getRhythm(rId);
            if (r != null)
            {
                rhythms.add(r);
            } else
            {
                LOGGER.warning("Could not restore favorite rhythm using saved property rhythmId=" + rId);
                prefs.remove(PROP_FAVORITE_RHYTHM + i);
            }
        }
    }

}

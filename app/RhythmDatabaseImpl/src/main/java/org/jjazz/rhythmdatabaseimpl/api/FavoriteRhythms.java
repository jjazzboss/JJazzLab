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
package org.jjazz.rhythmdatabaseimpl.api;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.rhythm.spi.RhythmProvider;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.jjazz.rhythmdatabase.api.RhythmInfo;
import org.jjazz.upgrade.api.UpgradeManager;
import org.jjazz.upgrade.api.UpgradeTask;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.ServiceProvider;

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
    private static final String PREF_NB_FAVORITE_RHYTHMS = "PropNbFavoriteRhythms";
    private final ArrayList<RhythmInfo> rhythms = new ArrayList<>();
    private static final Preferences prefs = NbPreferences.forModule(FavoriteRhythms.class);
    private final SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);
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
     * @param ri
     * @return True if added successfully (r was not already present).
     */
    public boolean addRhythm(RhythmInfo ri)
    {
        if (ri == null)
        {
            throw new NullPointerException("r=" + ri);
        }
        if (!rhythms.contains(ri))
        {
            rhythms.add(ri);
            updateProperties();
            pcs.firePropertyChange(PROP_FAVORITE_RHYTHM, null, ri);
            return true;
        }
        return false;
    }

    /**
     * Remove a favorite rhythm.
     *
     * @param ri
     * @return True if removed successfully (r was present).
     */
    public boolean removeRhythm(RhythmInfo ri)
    {
        if (ri == null)
        {
            throw new NullPointerException("ri");
        }
        if (rhythms.remove(ri))
        {
            updateProperties();
            pcs.firePropertyChange(PROP_FAVORITE_RHYTHM, ri, null);
            return true;
        }
        return false;
    }

    public boolean contains(RhythmInfo ri)
    {
        return rhythms.contains(ri);
    }

    /**
     * All the favorite rhythms.
     *
     * @return
     */
    public List<RhythmInfo> getRhythms()
    {
        return new ArrayList<>(rhythms);
    }

    /**
     * The favorite rhythms for the specified RhythmProvider.
     *
     * @param rp
     * @return
     */
    public List<RhythmInfo> getRhythms(RhythmProvider rp)
    {
        if (rp == null)
        {
            throw new NullPointerException("rp");
        }
        RhythmDatabase rdb = RhythmDatabase.getDefault();
        ArrayList<RhythmInfo> res = new ArrayList<>();
        for (RhythmInfo ri : rhythms)
        {
            if (rdb.getRhythmProvider(ri) == rp)
            {
                res.add(ri);
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
    public List<RhythmInfo> getRhythms(TimeSignature ts)
    {
        if (ts == null)
        {
            throw new NullPointerException("ts");
        }
        ArrayList<RhythmInfo> res = new ArrayList<>();
        for (RhythmInfo ri : rhythms)
        {
            if (ri.timeSignature().equals(ts))
            {
                res.add(ri);
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
        int oldNbRhythms = prefs.getInt(PREF_NB_FAVORITE_RHYTHMS, 0);
        for (int i = 0; i < oldNbRhythms; i++)
        {
            prefs.remove(PROP_FAVORITE_RHYTHM + i);
        }
        // Save the new nb of rhythms
        prefs.putInt(PREF_NB_FAVORITE_RHYTHMS, rhythms.size());
        // And recreate the needed properties
        int i = 0;
        for (RhythmInfo ri : rhythms)
        {
            prefs.put(PROP_FAVORITE_RHYTHM + i, ri.rhythmUniqueId());
            i++;
        }
    }

    private void restoreFavoriteRhythmsFromProperties()
    {
        int nbRhythms = prefs.getInt(PREF_NB_FAVORITE_RHYTHMS, 0);
        for (int i = 0; i < nbRhythms; i++)
        {
            // Loop on each saved rhythm
            String rId = prefs.get(PROP_FAVORITE_RHYTHM + i, null);
            if (rId == null)
            {
                continue;
            }
            RhythmDatabase rdb = RhythmDatabase.getDefault();
            RhythmInfo ri = rdb.getRhythm(rId);
            if (ri != null)
            {
                rhythms.add(ri);
            } else
            {
                LOGGER.log(Level.WARNING, "Could not restore favorite rhythm using saved property rhythmId={0}", rId);
                prefs.remove(PROP_FAVORITE_RHYTHM + i);
            }
        }
    }


}

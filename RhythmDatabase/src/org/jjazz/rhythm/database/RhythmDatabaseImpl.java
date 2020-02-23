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
package org.jjazz.rhythm.database;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import org.jjazz.rhythm.database.api.RhythmDatabase;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jjazz.filedirectorymanager.FileDirectoryManager;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.spi.RhythmProvider;
import org.jjazz.rhythm.api.TempoRange;
import static org.jjazz.rhythm.database.Bundle.CTL_Scanning;
import org.jjazz.rhythm.spi.StubRhythmProvider;
import org.openide.util.Lookup;
import org.netbeans.api.progress.BaseProgressUtils;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;

/**
 * Retrieve available rhythms by polling all RhythmProviders at runtime.
 * <p>
 * Default rhythms are stored as Preferences.
 */
@NbBundle.Messages(
        {
            "CTL_Scanning=Scanning available rhythms..."
        })
public class RhythmDatabaseImpl implements RhythmDatabase, PropertyChangeListener
{

    private static final String PREF_DEFAULT_RHYTHM = "DefaultRhythm";
    private static RhythmDatabaseImpl INSTANCE;
    /**
     * Primary data structure
     */
    private final HashMap<RhythmProvider, List<Rhythm>> mapRpRhythms = new HashMap<>();
    /**
     * Mirror map to get Rhythms sorted per TimeSignature. First rhythm is the default for each TimeSignature.
     */
    private final HashMap<TimeSignature, ArrayList<Rhythm>> mapTsRhythms = new HashMap<>();

    /**
     * Used to store the default rhythms
     */
    private static Preferences prefs = NbPreferences.forModule(RhythmDatabaseImpl.class);

    private final ArrayList<ChangeListener> listeners = new ArrayList<>();
    private static final Logger LOGGER = Logger.getLogger(RhythmDatabaseImpl.class.getSimpleName());

    static public RhythmDatabaseImpl getInstance()
    {
        synchronized (RhythmDatabaseImpl.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new RhythmDatabaseImpl();
                INSTANCE.refresh(true);
            }
        }
        return INSTANCE;
    }

    /**
     * Do nothing, for serialization.
     */
    private RhythmDatabaseImpl()
    {
        FileDirectoryManager.getInstance().addPropertyChangeListener(this);
    }

    @Override
    public void refresh(final boolean forceRescan)
    {
        LOGGER.log(Level.FINE, "refresh() forceRescan={0}", forceRescan);

        // Save data for comparison
        final HashMap<RhythmProvider, List<Rhythm>> saveMap = cloneDataMap();

        Runnable r = new Runnable()
        {
            @Override
            public void run()
            {
                performRefresh(forceRescan);
                if (!saveMap.equals(mapRpRhythms))
                {
                    fireChanged(new ChangeEvent(this));
                }
            }
        };
        BaseProgressUtils.showProgressDialogAndRun(r, CTL_Scanning());
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Rhythm> getRhythms(TimeSignature ts, List<Rhythm> optRhythms)
    {
        if (ts == null)
        {
            throw new NullPointerException("ts=" + ts);
        }
        ArrayList<Rhythm> result = new ArrayList<>();
        List<Rhythm> rList = (optRhythms != null) ? optRhythms : mapTsRhythms.get(ts);
        if (rList != null)
        {
            for (Rhythm r : rList)
            {
                if (r.getTimeSignature().equals(ts))
                {
                    result.add(r);
                }
            }
        }
        return result;
    }

    /**
     * For now just find another rhythm which share the same time signature and if possible same name (ignoring case).
     *
     * @param rhythm
     * @return
     */
    @Override
    public Rhythm getSimilarRhythm(final Rhythm rhythm)
    {
        List<Rhythm> rhythms = mapTsRhythms.get(rhythm.getTimeSignature());
        if (rhythms == null)
        {
            return null;
        }
        List<Rhythm> rhythms1 = new ArrayList<>(rhythms);

        // Remove rhythm corresponding to r
        for (Rhythm r : rhythms1.toArray(new Rhythm[0]))
        {
            if (r.getUniqueId().equals(rhythm.getUniqueId()))
            {
                rhythms1.remove(r);
                break;
            }
        }

        if (rhythms1.isEmpty())
        {
            return null;
        }

        // Search same name
        for (Rhythm r : rhythms1)
        {
            if (r.getName().equalsIgnoreCase(rhythm.getName()))
            {
                return r;
            }
        }

        // stop searching...
        return rhythms1.get(0);
    }

    @Override
    public List<Rhythm> getRhythms(List<String> tags, List<Rhythm> optRhythms)
    {
        if (tags == null)
        {
            throw new NullPointerException("tags=" + tags);
        }
        ArrayList<Rhythm> rhythms = new ArrayList<>();
        List<Rhythm> searchedRhythms = (optRhythms == null) ? getRhythms() : optRhythms;
        for (Rhythm ri : searchedRhythms)
        {
            if (org.jjazz.util.Utilities.indexOfStringIgnoreCase(tags, ri.getName()) != -1)
            {
                rhythms.add(ri);
                continue;
            }
            for (String rTag : ri.getTags())
            {
                if (org.jjazz.util.Utilities.indexOfStringIgnoreCase(tags, rTag) != -1)
                {
                    rhythms.add(ri);
                    break;
                }
            }
        }
        return rhythms;
    }

    @Override
    public List<Rhythm> getRhythms(int tempo, List<Rhythm> optRhythms)
    {
        if (!TempoRange.checkTempo(tempo))
        {
            throw new IllegalArgumentException("tempo=" + tempo);
        }
        ArrayList<Rhythm> rhythms = new ArrayList<>();
        List<Rhythm> searchedRhythms = (optRhythms == null) ? getRhythms() : optRhythms;
        for (Rhythm ri : searchedRhythms)
        {
            if (ri.getTempoRange().contains(tempo))
            {
                rhythms.add(ri);
            }
        }
        return rhythms;
    }

    @Override
    public Rhythm getRhythm(String uniqueSerialId)
    {
        for (Rhythm ri : getRhythms())
        {
            if (ri.getUniqueId().equals(uniqueSerialId))
            {
                return ri;
            }
        }
        return null;
    }

    @Override
    public List<Rhythm> getRhythms()
    {
        ArrayList<Rhythm> rhythms = new ArrayList<>();
        for (ArrayList<Rhythm> vr : mapTsRhythms.values())
        {
            rhythms.addAll(vr);
        }
        return rhythms;
    }

    @Override
    public List<Rhythm> getRhythms(RhythmProvider rp)
    {
        if (rp == null)
        {
            throw new IllegalArgumentException("rp=" + rp);
        }
        List<Rhythm> rpRhythms = mapRpRhythms.get(rp);
        if (rpRhythms == null)
        {
            throw new IllegalArgumentException("RhythmProvider not found rp=" + rp.getInfo().getName() + '@' + Integer.toHexString(rp.hashCode()));
        }
        List<Rhythm> rhythms = new ArrayList<>(rpRhythms);
        return rhythms;
    }

    @Override
    public Rhythm getNextRhythm(Rhythm r)
    {
        if (r == null)
        {
            throw new IllegalArgumentException("r=" + r);
        }
        List<Rhythm> rhythms = getRhythms(r.getTimeSignature(), null);
        int index = rhythms.indexOf(r);
        if (index == -1)
        {
            return r;
        }
        return (index == rhythms.size() - 1) ? rhythms.get(0) : rhythms.get(index + 1);
    }

    @Override
    public Rhythm getPreviousRhythm(Rhythm r)
    {
        if (r == null)
        {
            throw new IllegalArgumentException("r=" + r);
        }
        List<Rhythm> rhythms = getRhythms(r.getTimeSignature(), null);
        int index = rhythms.indexOf(r);
        if (index == -1)
        {
            return r;
        }
        return (index == 0) ? rhythms.get(rhythms.size() - 1) : rhythms.get(index - 1);
    }

    @Override
    public Rhythm getDefaultRhythm(TimeSignature ts)
    {
        // Try to restore the Rhythm from the preferences
        String prefName = getPrefString(ts);
        String rIdDefault = "";
        List<Rhythm> tsRhythms = mapTsRhythms.get(ts);
        if (tsRhythms != null && !tsRhythms.isEmpty())
        {
            rIdDefault = tsRhythms.get(0).getUniqueId();         // Used if no preference found
        }
        // Retrieve the Rhythm id
        String rId = prefs.get(prefName, rIdDefault);
        Rhythm r = getRhythm(rId);
        if (r == null)
        {
            // Saved rhythm is no longer in the database
            r = getRhythm(rIdDefault);
            if (r == null)
            {
                // Use the StubRhythmProvider
                StubRhythmProvider srp = Lookup.getDefault().lookup(StubRhythmProvider.class);
                if (srp == null)
                {
                    throw new IllegalStateException("Can't find a StubRhythmProvider instance ! srp=null");
                }
                r = srp.getStubRhythm(ts);
            }
        }
        return r;
    }

    /**
     * Set the default rhythm for this TimeSignature.
     *
     * @param ts TimeSignature
     * @exception IllegalArgumentException If ri is not part of this database
     */
    @Override
    public void setDefaultRhythm(TimeSignature ts, Rhythm ri)
    {
        if (ts == null)
        {
            throw new NullPointerException("ts=" + ts);
        }
        ArrayList<Rhythm> rhythms = mapTsRhythms.get(ts);
        if (rhythms == null || (rhythms.indexOf(ri)) == -1)
        {
            throw new IllegalArgumentException("Rhythm ri unknown. ts=" + ts + " ri=" + ri);
        }
        // Store the uniqueId of the Rhythm as a preference
        String prefName = getPrefString(ts);
        prefs.put(prefName, ri.getUniqueId());
    }

    /**
     * @return The list of TimeSignature for which we have at least 1 Rhythm in the database
     */
    @Override
    public List<TimeSignature> getTimeSignatures()
    {
        return new ArrayList<>(mapTsRhythms.keySet());
    }

    @Override
    public RhythmProvider getRhythmProvider(Rhythm r)
    {
        RhythmProvider resRp = null;
        for (RhythmProvider rp : mapRpRhythms.keySet())
        {
            if (mapRpRhythms.get(rp).contains(r))
            {
                resRp = rp;
                break;
            }
        }
        return resRp;
    }

    @Override
    public List<RhythmProvider> getRhythmProviders()
    {

        List<RhythmProvider> res = new ArrayList<>(mapRpRhythms.keySet());
        res.sort(new Comparator<RhythmProvider>()
        {
            @Override
            public int compare(RhythmProvider t, RhythmProvider t1)
            {
                return t.getInfo().getName().compareTo(t1.getInfo().getName());
            }
        });
        return res;
    }

    @Override
    public int addRhythms(List<RpRhythmPair> pairs)
    {
        if (pairs == null)
        {
            throw new NullPointerException("pairs");
        }
        int n = 0;
        for (RpRhythmPair p : pairs)
        {
            if (addRhythm(p.rp, p.r))
            {
                n++;
            }
        }
        if (n > 0)
        {
            fireChanged(new ChangeEvent(this));
        }
        return n;
    }

    @Override
    public String toString()
    {
        return "RhythmDB=" + getRhythms();
    }

    @Override
    public int size()
    {
        int size = 0;
        for (RhythmProvider rp : mapRpRhythms.keySet())
        {
            List<Rhythm> rhythms = mapRpRhythms.get(rp);
            size += rhythms.size();
        }
        return size;
    }

    @Override
    public void addChangeListener(ChangeListener l)
    {
        listeners.add(l);
    }

    @Override
    public void removeChangeListener(ChangeListener l)
    {
        listeners.remove(l);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        LOGGER.fine("propertyChange() evt=" + evt);
        if (evt.getSource() == FileDirectoryManager.getInstance())
        {
            if (evt.getPropertyName().equals(FileDirectoryManager.PROP_RHYTHM_USER_DIRECTORY))
            {
                // Directory has changed, force a rescan for all RhythmProviders
                refresh(true);
            }
        }
    }

    // ---------------------------------------------------------------------
    // Private 
    // --------------------------------------------------------------------- 
    private void performRefresh(boolean forceRescan)
    {
        LOGGER.fine("performRefresh() forceRescan=" + forceRescan);

        // Get all the available RhythmProviders 
        Collection<? extends RhythmProvider> rps = Lookup.getDefault().lookupAll(RhythmProvider.class);
        if (rps.isEmpty())
        {
            LOGGER.warning("refresh() - no RhythmProvider found, database will be empty");
        }

        for (RhythmProvider rp : rps)
        {
            if (!mapRpRhythms.containsKey(rp))
            {
                // All RhythmProviders must have a rhythm list, even empty
                mapRpRhythms.put(rp, new ArrayList<Rhythm>());
            }

            // First get builtin rhythms         
            for (Rhythm r : rp.getBuiltinRhythms())
            {
                addRhythm(rp, r);
            }

            // Add file rhythms
            List<Rhythm> rhythmsNotBuiltin = rp.getFileRhythms(forceRescan ? null : mapRpRhythms.get(rp));
            for (Rhythm r : rhythmsNotBuiltin)
            {
                addRhythm(rp, r);
            }
        }
    }

    private String getPrefString(TimeSignature ts)
    {
        return PREF_DEFAULT_RHYTHM + "__" + ts.name();
    }

    protected void clear()
    {
        this.mapRpRhythms.clear();
        this.mapTsRhythms.clear();
    }

    /**
     * Add to the database one Rhythm from RhythmProvider rp.
     * <p>
     * Do nothing if rhythm already exists in the database for this rp.
     *
     * @param rp
     * @param r
     * @return True if rhythm was added.
     */
    private boolean addRhythm(RhythmProvider rp, Rhythm r)
    {
        List<Rhythm> rhythms = mapRpRhythms.get(rp);
        assert rhythms != null : "rp=" + rp + " r=" + r;
        boolean b = false;
        if (!rhythms.contains(r))
        {
            rhythms.add(r);
            TimeSignature ts = r.getTimeSignature();
            ArrayList<Rhythm> ts_rInfos = mapTsRhythms.get(ts);
            if (ts_rInfos == null)
            {
                ts_rInfos = new ArrayList<>();
                mapTsRhythms.put(ts, ts_rInfos);
            }
            ts_rInfos.add(r);
            b = true;
        }
        return b;
    }

    private void fireChanged(ChangeEvent e)
    {
        LOGGER.fine("fireChanged()");
        for (ChangeListener l : listeners)
        {
            l.stateChanged(e);
        }
    }

    private void dump()
    {
        LOGGER.info("RhythmDatabaseImpl dump ----- ");
        for (Rhythm r : this.getRhythms())
        {
            LOGGER.severe("  " + r.toString());
        }
    }

    /**
     * Make a deepcopy of our database.
     *
     * @return
     */
    private HashMap<RhythmProvider, List<Rhythm>> cloneDataMap()
    {
        HashMap<RhythmProvider, List<Rhythm>> res = new HashMap<>();
        for (RhythmProvider rp : mapRpRhythms.keySet())
        {
            List<Rhythm> rhythms = new ArrayList<>(mapRpRhythms.get(rp));
            res.put(rp, rhythms);
        }
        return res;
    }

}

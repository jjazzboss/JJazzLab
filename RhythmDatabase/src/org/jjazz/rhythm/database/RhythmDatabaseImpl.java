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
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jjazz.filedirectorymanager.FileDirectoryManager;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.rhythm.api.AdaptedRhythm;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.spi.RhythmProvider;
import org.jjazz.rhythm.database.api.RhythmDatabase;
import org.jjazz.rhythm.database.api.RhythmInfo;
import org.jjazz.rhythm.database.api.UnavailableRhythmException;
import org.jjazz.upgrade.UpgradeManager;
import org.jjazz.upgrade.spi.UpgradeTask;
import org.openide.util.Lookup;
import org.netbeans.api.progress.BaseProgressUtils;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.ServiceProvider;

/**
 * RhythmDatabase implementation.
 * <p>
 * Upon clean/fresh start:<br>
 * - retrieve all available builtin & file-based rhythm instances by polling RhythmProviders (this can be long if many rhythm files need to
 * be scanned).<br>
 * - create RhythmInfo instances from the Rhythm instances and save the file-based RhythmInfos to a cache file.<p>
 * Then upon normal start:<br>
 * - retrieve all available builtin rhythm instances by polling RhythmProviders, create the corresponding RhythmInfos.<br>
 * - load additional file-based RhythmInfos from the cache file<br>
 * - create Rhythm instances only when required.<p>
 * <p>
 * Default rhythms are stored as Preferences.
 */
public class RhythmDatabaseImpl implements RhythmDatabase, PropertyChangeListener
{

    private static final String PREF_DEFAULT_RHYTHM = "DefaultRhythm";
    private static final String PREF_NEED_RESCAN = "FreshScan";

    private static RhythmDatabaseImpl INSTANCE;

    /**
     * Main data structure
     */
    private final HashMap<RhythmProvider, List<RhythmInfo>> mapRpRhythms = new HashMap<>();
    /**
     * Save the created Rhythm instances.
     */
    private final HashMap<RhythmInfo, Rhythm> mapInfoInstance = new HashMap<>();
    /**
     * Keep the AdaptedRhythms instances created on-demand.
     * <p>
     * Map key=originalRhythmId-TimeSignature
     */
    private final HashMap<String, AdaptedRhythm> mapAdaptedRhythms = new HashMap<>();
    /**
     * Used to store the default rhythms
     */
    private static Preferences prefs = NbPreferences.forModule(RhythmDatabaseImpl.class);
    private final RhythmDbCacheFile cacheFile;

    private final ArrayList<ChangeListener> listeners = new ArrayList<>();
    private static final Logger LOGGER = Logger.getLogger(RhythmDatabaseImpl.class.getSimpleName());

    static public RhythmDatabaseImpl getInstance()
    {
        synchronized (RhythmDatabaseImpl.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new RhythmDatabaseImpl();
            }
        }
        return INSTANCE;
    }

    private RhythmDatabaseImpl()
    {
        FileDirectoryManager.getInstance().addPropertyChangeListener(this);


        // Our cache
        cacheFile = new RhythmDbCacheFile();


        // Perform a scan or use the cache file
        if (prefs.getBoolean(PREF_NEED_RESCAN, true))
        {            
            fullScan();

        } else
        {

            // Get all builtin Rhythm+RhythmInfo instances
            performRefresh(true, false);


            try
            {
                // Add RhythmInfo instances from cache
                cacheFile.loadCacheFile(mapRpRhythms);
            } catch (IOException ex)
            {
                LOGGER.severe("RhythmDatabaseImpl() Can't load cache file=" + cacheFile.getFile().getAbsolutePath() + ". ex=" + ex.getLocalizedMessage());
                String msg = "Error loading rhythm database cache file " + cacheFile.getFile().getAbsolutePath() + "\n\n"
                        + "Need to relaunch full scan of the rhythm files, this may take some time...";
                NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(d);
                
                // Rescan
                fullScan();
            }
        }
    }

    /**
     * Note: rhythms are not removed!
     *
     * @param forceRescan
     */
    @Override
    public void refresh(final boolean forceRescan)
    {
        LOGGER.log(Level.FINE, "refresh() forceRescan={0}", forceRescan);

        // Save data for comparison
        final HashMap<RhythmProvider, List<RhythmInfo>> saveMap = cloneDataMap();

        Runnable r = new Runnable()
        {
            @Override
            public void run()
            {
                performRefresh(false, forceRescan);
                if (!saveMap.equals(mapRpRhythms))
                {
                    fireChanged(new ChangeEvent(this));
                }
            }
        };
        String msg = "Scanning rhythm files in " + FileDirectoryManager.getInstance().getUserRhythmDirectory().getAbsolutePath();
        BaseProgressUtils.showProgressDialogAndRun(r, msg);
    }

    @Override
    public Rhythm getRhythmInstance(RhythmInfo ri) throws UnavailableRhythmException
    {
        if (ri == null)
        {
            throw new IllegalArgumentException("ri=" + ri);
        }


        Rhythm r = mapInfoInstance.get(ri);
        if (r != null)
        {
            return r;
        }

        // Builtin rhythms should not be there, they must have a RhythmInfo defined
        assert r.getFile() != null : "r=" + r;


        // Get the instance from provider
        RhythmProvider rp = getRhythmProvider(ri);
        if (rp == null)
        {
            throw new UnavailableRhythmException("No Rhythm Provider found for rhythm" + ri);
        }
        try
        {
            r = rp.readFast(ri.getFile());
        } catch (IOException ex)
        {
            throw new UnavailableRhythmException(ex.getLocalizedMessage());
        }

        if (!ri.checkConsistency(r))
        {
            throw new UnavailableRhythmException("Inconsistency detected for rhythm " + ri + ". Consider refreshing the rhythm database.");
        }

        // Save the instance
        mapInfoInstance.put(ri, r);

        return r;
    }

    @Override
    public RhythmInfo getRhythm(String rhythmId)
    {
        for (RhythmProvider rp : mapRpRhythms.keySet())
        {
            for (RhythmInfo ri : mapRpRhythms.get(rp))
            {
                if (ri.getUniqueId().equals(rhythmId))
                {
                    return ri;
                }
            }
        }
        return null;
    }

    @Override
    public RhythmProvider getRhythmProvider(RhythmInfo ri)
    {
        if (ri == null)
        {
            throw new IllegalArgumentException("ri=" + ri);
        }
        for (RhythmProvider rp : mapRpRhythms.keySet())
        {
            for (RhythmInfo rInfo : mapRpRhythms.get(rp))
            {
                if (rInfo.equals(ri))
                {
                    return rp;
                }
            }
        }
        return null;
    }

    @Override
    public List<RhythmInfo> getRhythms(Predicate<RhythmInfo> tester)
    {
        if (tester == null)
        {
            throw new NullPointerException("tester");
        }
        List<RhythmInfo> res = new ArrayList<>();
        for (RhythmProvider rp : mapRpRhythms.keySet())
        {
            for (RhythmInfo ri : mapRpRhythms.get(rp))
            {
                if (tester.test(ri))
                {
                    res.add(ri);
                }
            }
        }
        return res;
    }

    @Override
    public List<RhythmInfo> getRhythms(TimeSignature ts)
    {
        if (ts == null)
        {
            throw new NullPointerException("ts=" + ts);
        }
        return getRhythms(ri -> ri.getTimeSignature().equals(ts));
    }

    @Override
    public RhythmInfo getSimilarRhythm(final RhythmInfo ri)
    {
        int max = -1;
        RhythmInfo res = null;
        for (RhythmInfo rii : getRhythms())
        {
            if (!rii.getTimeSignature().equals(ri.getTimeSignature()) || rii == ri)
            {
                continue;
            }
            int score = ri.getFeatures().getMatchingScore(rii.getFeatures());
            if (score > max)
            {
                max = score;
                res = rii;
            }
        }

        return res;
    }

    @Override
    public Rhythm getRhythmInstance(String rId) throws UnavailableRhythmException
    {
        Rhythm r = null;
        if (rId.contains(AdaptedRhythm.RHYTHM_ID_DELIMITER))
        {
            String[] strs = rId.split(AdaptedRhythm.RHYTHM_ID_DELIMITER);
            if (strs.length == 3)
            {
                String rpId = strs[0];
                String rIdOriginal = strs[1];
                TimeSignature newTs;
                try
                {
                    newTs = TimeSignature.parse(strs[2]);
                } catch (ParseException ex)
                {
                    LOGGER.warning("getRhythm() Invalid time signature in AdaptedRhythm rId=" + rId);
                    return null;
                }
                RhythmProvider rp = getRhythmProviders().stream().filter(rhp -> rhp.getInfo().getUniqueId().equals(rpId)).findAny().orElse(null);
                if (rp == null)
                {
                    LOGGER.warning("getRhythm() Unknown rhythm provider id in AdaptedRhythm rId=" + rId);
                    return null;
                }
                Rhythm rOriginal = getRhythmInstance(rIdOriginal);
                if (rOriginal == null)
                {
                    LOGGER.warning("getRhythm() Unknown rhythmId in AdaptedRhythm rId=" + rId);
                    return null;
                }
                r = getAdaptedRhythmInstance(rOriginal, newTs);
            }
        } else
        {
            RhythmInfo ri = getRhythm(rId);
            if (ri != null)
            {
                r = getRhythmInstance(ri);
            }
        }
        return r;
    }

    @Override
    public List<RhythmInfo> getRhythms()
    {
        return getRhythms(r -> true);
    }

    @Override
    public List<RhythmInfo> getRhythms(RhythmProvider rp)
    {
        if (rp == null)
        {
            throw new IllegalArgumentException("rp=" + rp);
        }
        List<RhythmInfo> rpRhythms = mapRpRhythms.get(rp);
        if (rpRhythms == null)
        {
            throw new IllegalArgumentException("RhythmProvider not found rp=" + rp.getInfo().getName() + '@' + Integer.toHexString(rp.hashCode()));
        }
        List<RhythmInfo> rhythms = new ArrayList<>(rpRhythms);
        return rhythms;
    }

    @Override
    public Rhythm getDefaultRhythm(TimeSignature ts)
    {
        Rhythm r = null;

        // Try to restore the Rhythm from the preferences
        String prefName = getPrefString(ts);
        String rId = prefs.get(prefName, null);
        if (rId != null)
        {
            try
            {
                r = getRhythmInstance(rId);
            } catch (UnavailableRhythmException ex)
            {
                // Do nothing
            }
        }

        // If problem occured take first rhythm available
        if (r == null)
        {
            List<RhythmInfo> rhythms = getRhythms(ts);
            assert rhythms.size() > 0 : " mapRpRhythms=" + this.mapRpRhythms;
            RhythmInfo ri = rhythms.get(0);
            try
            {
                r = getRhythmInstance(ri);
            } catch (UnavailableRhythmException ex)
            {
                throw new IllegalStateException("Can't access default rhythm " + ri);
            }
        }

        return r;
    }

    @Override
    public void setDefaultRhythm(TimeSignature ts, Rhythm r)
    {
        if (ts == null || r == null)
        {
            throw new NullPointerException("ts=" + ts + " r=" + null);
        }

        if (getRhythm(r.getUniqueId()) == null)
        {
            throw new IllegalArgumentException("Rhythm r not in this database. ts=" + ts + " r=" + r);
        }

        // Store the uniqueId of the Rhythm as a preference
        prefs.put(getPrefString(ts), r.getUniqueId());
    }

    @Override
    public List<TimeSignature> getTimeSignatures()
    {
        List<TimeSignature> res = new ArrayList<>();
        for (RhythmProvider rp : mapRpRhythms.keySet())
        {
            for (RhythmInfo ri : mapRpRhythms.get(rp))
            {
                TimeSignature ts = ri.getTimeSignature();
                if (!res.contains(ts))
                {
                    res.add(ts);
                }
            }
        }
        return res;
    }

    @Override
    public AdaptedRhythm getAdaptedRhythmInstance(Rhythm r, TimeSignature ts)
    {
        if (r == null || ts == null || r.getTimeSignature().equals(ts))
        {
            throw new IllegalArgumentException("r=" + r + " ts=" + ts);
        }

        AdaptedRhythm ar = mapAdaptedRhythms.get(getAdaptedRhythmKey(r, ts));
        if (ar == null)
        {
            for (RhythmProvider rp : getRhythmProviders())
            {
                ar = rp.getAdaptedRhythm(r, ts);
                if (ar != null)
                {
                    addRhythm(rp, ar);
                    mapAdaptedRhythms.put(getAdaptedRhythmKey(r, ts), ar);
                    break;
                }
            }
        }
        return ar;
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
        res.sort((rp1, rp2) -> rp1.getInfo().getName().compareTo(rp2.getInfo().getName()));
        return res;
    }

    @Override
    public int addExtraRhythms(List<RpRhythmPair> pairs)
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
            List<RhythmInfo> rhythms = mapRpRhythms.get(rp);
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
    private void performRefresh(boolean builtinOnly, boolean forceFileRescan)
    {
        LOGGER.fine("performRefresh() builtinOnly=" + builtinOnly + " forceFileRescan=" + forceFileRescan);

        // Get all the available RhythmProviders 
        Collection<? extends RhythmProvider> rps = Lookup.getDefault().lookupAll(RhythmProvider.class);
        if (rps.isEmpty())
        {
            LOGGER.warning("refresh() - no RhythmProvider found, database will be empty");
        }

        for (RhythmProvider rp : rps)
        {
            // First get builtin rhythms         
            for (Rhythm r : rp.getBuiltinRhythms())
            {
                addRhythm(rp, r);
            }

            if (builtinOnly)
            {
                continue;
            }

            // Add file rhythms
            List<Rhythm> rhythmsNotBuiltin = rp.getFileRhythms(forceFileRescan);
            for (Rhythm r : rhythmsNotBuiltin)
            {
                addRhythm(rp, r);
            }

        }
    }

    private void fullScan()
    {
        // Get all rhythm instances from RhythmProviders
        refresh(true);
        assert !mapRpRhythms.isEmpty();


        // Save file-based RhythmInfos
        try
        {
            cacheFile.saveCacheFile(getRhythms(r -> r.getFile() != null));  // Possible exception here
            prefs.putBoolean(PREF_NEED_RESCAN, false);
        } catch (IOException ex)
        {
            LOGGER.severe("firstScan() Can't save cache file=" + cacheFile.getFile().getAbsolutePath() + ". ex=" + ex.getLocalizedMessage());
            prefs.putBoolean(PREF_NEED_RESCAN, true);
        }
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
        // Build the RhythmInfo object
        RhythmInfo ri = new RhythmInfoImpl(r, rp);


        // Update state
        List<RhythmInfo> rhythms = mapRpRhythms.get(rp);
        if (rhythms == null)
        {
            rhythms = new ArrayList<>();
            mapRpRhythms.put(rp, rhythms);
        }
        if (!rhythms.contains(ri))
        {
            rhythms.add(ri);
            return true;
        } else
        {
            return false;
        }
    }

    private String getPrefString(TimeSignature ts)
    {
        return PREF_DEFAULT_RHYTHM + "__" + ts.name();
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
        for (RhythmInfo ri : this.getRhythms())
        {
            LOGGER.severe("  " + ri.toString());
        }
    }

    /**
     * Make a deepcopy of our database.
     *
     * @return
     */
    private HashMap<RhythmProvider, List<RhythmInfo>> cloneDataMap()
    {
        HashMap<RhythmProvider, List<RhythmInfo>> res = new HashMap<>();
        for (RhythmProvider rp : mapRpRhythms.keySet())
        {
            List<RhythmInfo> rhythms = new ArrayList<>(mapRpRhythms.get(rp));
            res.put(rp, rhythms);
        }
        return res;
    }

    private String getAdaptedRhythmKey(Rhythm r, TimeSignature ts)
    {
        return r.getUniqueId() + "-" + ts.name();
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

}

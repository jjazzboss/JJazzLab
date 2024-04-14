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
package org.jjazz.rhythmdatabaseimpl;

import org.jjazz.utilities.api.MultipleErrorsReportDialog;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jjazz.filedirectorymanager.api.FileDirectoryManager;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.rhythm.api.AdaptedRhythm;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.spi.RhythmProvider;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.jjazz.rhythmdatabase.api.RhythmInfo;
import org.jjazz.rhythmdatabase.api.UnavailableRhythmException;
import org.jjazz.utilities.api.MultipleErrorsReport;
import org.jjazz.rhythm.spi.StubRhythmProvider;
import org.jjazz.upgrade.api.UpgradeManager;
import org.jjazz.upgrade.api.UpgradeTask;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.utilities.api.Utilities;
import org.netbeans.api.annotations.common.StaticResource;
import org.netbeans.api.progress.ProgressHandle;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Exceptions;
import org.openide.util.NbPreferences;
import org.openide.util.RequestProcessor;
import org.openide.util.lookup.ServiceProvider;
import org.openide.windows.WindowManager;
import org.jjazz.startup.spi.OnShowingTask;

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
 * @todo Make this database extends DefaultRhythmDatabase ? 
 */
public class RhythmDatabaseImpl implements RhythmDatabase, PropertyChangeListener
{

    private static final String PREF_DEFAULT_RHYTHM = "DefaultRhythm";
    private static final String PREF_NEED_RESCAN = "NeedRescan";

    /**
     * Main data structure
     */
    private final Map<RhythmProvider, List<RhythmInfo>> mapRpRhythms = new HashMap<>();
    /**
     * Save the created Rhythm instances.
     */
    private final Map<RhythmInfo, Rhythm> mapInfoInstance = new HashMap<>();
    /**
     * Keep the AdaptedRhythms instances created on-demand.
     * <p>
     * Map key=originalRhythmId-TimeSignature
     */
    private final Map<String, AdaptedRhythm> mapAdaptedRhythms = new HashMap<>();
    /**
     * The initialization task.
     */
    private volatile RequestProcessor.Task initTask;
    /**
     * Used to store the default rhythms
     */
    private static final Preferences prefs = NbPreferences.forModule(RhythmDatabaseImpl.class);
    private final ArrayList<ChangeListener> listeners = new ArrayList<>();
    private static final Logger LOGGER = Logger.getLogger(RhythmDatabaseImpl.class.getSimpleName());


    /**
     * Create the database and start a background initialization task to populate it.
     *
     * @param builtinRhythmsOnly If true, populate the database only with builtin rhythms (excluding file-based rhythms).
     * @see #isInitializationTaskComplete()
     */
    protected RhythmDatabaseImpl(boolean builtinRhythmsOnly)
    {

        if (!builtinRhythmsOnly)
        {
            LOGGER.fine("RhythmDatabaseImpl() --");
            FileDirectoryManager.getInstance().addPropertyChangeListener(this);

            // Prepare the ProgressHandle
            boolean needRescan = prefs.getBoolean(PREF_NEED_RESCAN, true);
            String dir = FileDirectoryManager.getInstance().getUserRhythmDirectory().getAbsolutePath();
            String msg1 = ResUtil.getString(getClass(), "CTL_ScanningAllRhythmsInDir", dir);
            String msg2 = ResUtil.getString(getClass(), "CTL_ReadingRhythmDbCacheFile");
            String msg = needRescan ? msg1 : msg2;
            ProgressHandle ph = ProgressHandle.createHandle(msg);
            ph.start();

            // Start the task
            Runnable run = () -> initDatabase(ph, needRescan);
            initTask = RequestProcessor.getDefault().post(run);
        }
        else
        {
            // Only builtin rhythms, no task to update
            addNewRhythmsFromRhythmProviders(false, true, false);
        }
    }


    @Override
    public void forceRescan(final boolean immediate)
    {
        LOGGER.log(Level.INFO, "forceRescan() -- immediate={0}", immediate);
        if (immediate)
        {
            LOGGER.warning("forceRescan() immediate=true not supported. Using immediate=false instead.");
        }
        prefs.putBoolean(PREF_NEED_RESCAN, true);
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
        assert !ri.file().getName().equals("") : "ri=" + ri + " ri.file()=" + ri.file().getName();

        // Get the instance from provider
        RhythmProvider rp = getRhythmProvider(ri);
        if (rp == null)
        {
            throw new UnavailableRhythmException("No Rhythm Provider found for rhythm" + ri);
        }
        try
        {
            r = rp.readFast(ri.file());
        }
        catch (IOException ex)
        {
            throw new UnavailableRhythmException(ex.getLocalizedMessage());
        }

        if (!ri.checkConsistency(rp, r))
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
                if (ri.rhythmUniqueId().equals(rhythmId))
                {
                    return ri;
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
    public AdaptedRhythm getAdaptedRhythmInstance(Rhythm r, TimeSignature ts)
    {
        if (r == null || ts == null || r.getTimeSignature().equals(ts))
        {
            throw new IllegalArgumentException("r=" + r + " ts=" + ts);
        }

        String adaptedRhythmKey = getAdaptedRhythmKey(r.getUniqueId(), ts);

        AdaptedRhythm ar = mapAdaptedRhythms.get(adaptedRhythmKey);
        if (ar == null)
        {
            for (RhythmProvider rp : getRhythmProviders())
            {
                ar = rp.getAdaptedRhythm(r, ts);
                if (ar != null)
                {
                    addRhythm(rp, ar);
                    mapAdaptedRhythms.put(adaptedRhythmKey, ar);
                    break;
                }
            }
        }
        return ar;
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
    public RhythmInfo getDefaultRhythm(TimeSignature ts)
    {
        RhythmInfo res = null;

        // Try to restore the Rhythm from the preferences
        String prefName = getPrefString(ts);
        String rId = prefs.get(prefName, null);
        if (rId != null)
        {
            res = getRhythm(rId);
        }
        if (res != null)
        {
            return res;
        }

        // No default rhythm defined : pick a rhythm from the database (AdaptedRhythms excluded)
        List<RhythmInfo> rhythms = getRhythms(ts)
            .stream()
            .filter(ri -> !ri.isAdaptedRhythm())
            .toList();

        assert rhythms.size() > 0 : " mapRpRhythms=" + this.mapRpRhythms;

        // Take first rhythm which does not come from a StubRhythmProvider
        for (RhythmInfo ri : rhythms)
        {
            if (!(getRhythmProvider(ri) instanceof StubRhythmProvider))
            {
                return ri;
            }
        }

        // Take first rhythm
        return rhythms.get(0);

    }

    @Override
    public void setDefaultRhythm(TimeSignature ts, RhythmInfo ri)
    {
        if (ts == null || ri == null || ri.isAdaptedRhythm())
        {
            throw new IllegalArgumentException("ts=" + ts + " ri=" + null);
        }

        if (getRhythm(ri.rhythmUniqueId()) == null)
        {
            throw new IllegalArgumentException("Rhythm ri not in this database. ts=" + ts + " r=" + ri);
        }

        // Store the uniqueId of the Rhythm as a preference
        prefs.put(getPrefString(ts), ri.rhythmUniqueId());
    }


    @Override
    public List<TimeSignature> getTimeSignatures()
    {
        List<TimeSignature> res = new ArrayList<>();
        for (RhythmProvider rp : mapRpRhythms.keySet())
        {
            for (RhythmInfo ri : mapRpRhythms.get(rp))
            {
                TimeSignature ts = ri.timeSignature();
                if (!res.contains(ts))
                {
                    res.add(ts);
                }
            }
        }
        return res;
    }


    @Override
    public RhythmProvider getRhythmProvider(Rhythm r)
    {
        RhythmProvider resRp = null;
        RhythmInfo ri = getRhythm(r.getUniqueId());
        for (RhythmProvider rp : mapRpRhythms.keySet())
        {
            if (mapRpRhythms.get(rp).contains(ri))
            {
                resRp = rp;
                break;
            }
        }
        return resRp;
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
    public int addExtraRhythms(List<RpRhythmPair> pairs)
    {
        if (pairs == null)
        {
            throw new NullPointerException("pairs");
        }
        int n = 0;
        for (RpRhythmPair p : pairs)
        {
            if (addRhythm(p.rp(), p.r()))
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
        LOGGER.log(Level.FINE, "propertyChange() evt={0}", evt);
        if (evt.getSource() == FileDirectoryManager.getInstance())
        {
            if (evt.getPropertyName().equals(FileDirectoryManager.PROP_RHYTHM_USER_DIRECTORY))
            {
                // Directory has changed, plan a rescan
                forceRescan(false);
            }
        }
    }

    // ---------------------------------------------------------------------
    // Private 
    // --------------------------------------------------------------------- 
    /**
     * Initialization operations.
     *
     * @param ph Used to monitor progress (already started)
     * @param needRescan
     */
    private void initDatabase(ProgressHandle ph, boolean needRescan)
    {
        File rDir = FileDirectoryManager.getInstance().getUserRhythmDirectory();
        boolean cacheFilePresent = RhythmDbCache.getFile().isFile();
        LOGGER.log(Level.INFO, "initDatabase() needRescan={0} cacheFilePresent={1}", new Object[]
        {
            needRescan, cacheFilePresent
        });

        String msg1 = ResUtil.getString(getClass(), "CTL_ScanningAllRhythmsInDir", rDir.getAbsolutePath());
        String msg2 = ResUtil.getString(getClass(), "CTL_SavingRhythmDbCacheFile");

        // Perform a scan or use the cache file
        if (needRescan || !cacheFilePresent)
        {
            // Full scan

            // Get all rhythm instances from RhythmProviders
            ph.progress(msg1);
            addNewRhythmsFromRhythmProviders(false, false, true);

            // Build and save cache file
            ph.progress(msg2);
            writeCache();

        }
        else
        {
            // Reuse cache file to avoid a full scan

            // Scan only for builtin Rhythms
            ph.progress(ResUtil.getString(getClass(), "CTL_ScanningAllBuiltinRhythms"));
            addNewRhythmsFromRhythmProviders(false, true, false);

            // Read cache
            try
            {
                ph.progress(ResUtil.getString(getClass(), "CTL_ReadingRhythmDbCacheFile"));
                readCache();
            }
            catch (IOException ex)
            {
                // Notify
                LOGGER.log(Level.WARNING, "RhythmDatabaseImpl() Can''t load cache file. IOException ex={0}", ex.getMessage());
                String msg = ResUtil.getString(getClass(), "ERR_LoadingCacheFile", RhythmDbCache.getFile().getAbsolutePath());
                NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(d);

                // And start full scan!
                // Get all rhythm instances from RhythmProviders, don't need built-in since we already have them
                ph.progress(msg1);
                addNewRhythmsFromRhythmProviders(true, false, true);

                // Rescan
                ph.progress(msg2);
                writeCache();
            }
            catch (ClassNotFoundException ex)
            {
                Exceptions.printStackTrace(ex);
            }
        }

        ph.finish();

        logStats();
    }

    /**
     *
     * @param excludeBuiltinRhythms
     * @param excludeFileRhythms
     * @param forceFileRescan
     * @return The number of new rhythms added
     */
    private int addNewRhythmsFromRhythmProviders(boolean excludeBuiltinRhythms, boolean excludeFileRhythms, boolean forceFileRescan)
    {

        // Get all the available RhythmProviders 
        var rps = getRhythmProviders();
        if (rps.isEmpty())
        {
            LOGGER.warning("addNewRhythmsFromRhythmProviders() - no RhythmProvider found, database might be empty");
        }

        int n = 0;
        for (final RhythmProvider rp : rps)
        {

            // First get builtin rhythms         
            final MultipleErrorsReport builtinErrRpt = new MultipleErrorsReport();
            if (!excludeBuiltinRhythms)
            {
                for (Rhythm r : rp.getBuiltinRhythms(builtinErrRpt))
                {
                    if (addRhythm(rp, r))
                    {
                        n++;
                    }
                }
            }


            // Notify user of possible errors
            if (builtinErrRpt.primaryErrorMessage != null)
            {
                SwingUtilities.invokeLater(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        new MultipleErrorsReportDialog(WindowManager.getDefault().getMainWindow(), ResUtil.getString(getClass(), "CTL_BuiltinRhythmErrors"),
                            builtinErrRpt).setVisible(true);
                    }
                });
            }


            // Add file rhythms
            final MultipleErrorsReport fileErrRpt = new MultipleErrorsReport();
            if (!excludeFileRhythms)
            {
                List<Rhythm> rhythmsNotBuiltin = rp.getFileRhythms(forceFileRescan, fileErrRpt);
                for (Rhythm r : rhythmsNotBuiltin)
                {
                    if (addRhythm(rp, r))
                    {
                        n++;
                    }
                }
            }


            // Notify user of possible errors            
            if (fileErrRpt.primaryErrorMessage != null)
            {
                SwingUtilities.invokeLater(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        new MultipleErrorsReportDialog(WindowManager.getDefault().getMainWindow(), ResUtil.getString(getClass(), "CTL_FileBasedRhythmErrors"),
                            fileErrRpt).setVisible(true);
                    }
                });
            }

        }

        LOGGER.log(Level.INFO, "addNewRhythmsFromRhythmProviders() excludeBuiltinRhythms={0} excludeFileRhythms={1} forceFileRescan={2}. Added {3} rhythms",
            new Object[]
            {
                excludeBuiltinRhythms,
                excludeFileRhythms, forceFileRescan, n
            });

        return n;
    }

    /**
     * Build the cache and write it to file.
     */
    private void writeCache()
    {
        // Buid the cache
        RhythmDbCache cacheFile = new RhythmDbCache(mapRpRhythms);

        // cacheFile.dump();
        // Save to file
        Runnable run = () ->
        {
            File f = RhythmDbCache.getFile();
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f)))
            {
                oos.writeObject(cacheFile);
                prefs.putBoolean(PREF_NEED_RESCAN, false);
                LOGGER.log(Level.INFO, "writeCache.run() cache file created, size={0}", cacheFile.getSize());
            }
            catch (IOException ex)
            {
                LOGGER.log(Level.SEVERE, "writeCache.run() Can''t save cache file={0}. ex={1}", new Object[]
                {
                    RhythmDbCache.getFile().getAbsolutePath(),
                    ex.getMessage()
                });
                prefs.putBoolean(PREF_NEED_RESCAN, true);
            }
        };

        // Can be safely done in another thread
        new Thread(run).start();

    }

    /**
     * Read the cache file and update the database accordingly.
     *
     * @throws java.io.IOException
     * @throws java.lang.ClassNotFoundException
     */
    private void readCache() throws IOException, ClassNotFoundException
    {
        // Read the file
        File f = RhythmDbCache.getFile();
        RhythmDbCache cache;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f)))
        {
            cache = (RhythmDbCache) ois.readObject();
        }

        assert cache != null;

        // Process it
        var cacheData = cache.getData();
        var rps = getRhythmProviders();

        for (String rpId : cacheData.keySet())
        {
            List<RhythmInfo> rhythms = cacheData.get(rpId);

            // Get the RhythmProvider
            RhythmProvider rp = rps.stream()
                .filter(rpi -> rpi.getInfo().getUniqueId().equals(rpId))
                .findAny()
                .orElse(null);
            if (rp == null)
            {
                LOGGER.log(Level.WARNING, "readCache.run() No RhythmProvider found for rpId={0}. Ignoring {1} rhythms.", new Object[]
                {
                    rpId,
                    rhythms.size()
                });
                continue;
            }

            // Update state
            var rpRhythms = mapRpRhythms.get(rp);
            if (rpRhythms == null)
            {
                rpRhythms = new ArrayList<>();
                mapRpRhythms.put(rp, rpRhythms);
            }
            rpRhythms.addAll(rhythms);
        }

        // cache.dump();
        LOGGER.log(Level.INFO, "readCache() Successfully read rhythm list from cache, size={0}", cache.getSize());

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
        RhythmInfo ri = new RhythmInfo(r, rp);

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
            mapInfoInstance.put(ri, r);
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * Get the database initialization task.
     *
     * @return
     */
    protected RequestProcessor.Task getInitializationTask()
    {
        return initTask;
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
        LOGGER.info("RhythmDatabaseImpl dump ----- RhythmInfo instances");
        for (RhythmInfo ri : this.getRhythms())
        {
            LOGGER.log(Level.INFO, "  {0}", ri.toString());
        }
        LOGGER.info("RhythmDatabaseImpl dump ----- Rhythm instances");
        for (RhythmInfo ri : this.mapInfoInstance.keySet())
        {
            LOGGER.log(Level.INFO, "  {0} -> RhythmInstance.isResourcesLoaded()={1}", new Object[]
            {
                ri.toString(),
                mapInfoInstance.get(ri).isResourcesLoaded()
            });
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

    private String getAdaptedRhythmKey(String rId, TimeSignature ts)
    {
        return rId + "-" + ts.name();

    }

    private void logStats()
    {
        LOGGER.log(Level.INFO, "logStats() Rythm Database stats - total={0}", size());
        for (RhythmProvider rp : mapRpRhythms.keySet())
        {
            var rhythms = mapRpRhythms.get(rp);
            long nbBuiltins = rhythms.stream()
                .filter(ri -> ri.file().getName().equals(""))
                .count();
            long nbFiles = rhythms.size() - nbBuiltins;
            String firstRhythm = rhythms.isEmpty() ? "" : "first=" + rhythms.get(0).toString() + "...";

            LOGGER.log(Level.INFO, "  > {0}: total={1} builtin={2} file={3} {4}", new Object[]
            {
                rp.getInfo().getName(), rhythms.size(),
                nbBuiltins, nbFiles, firstRhythm
            });
        }
    }

    // =====================================================================================
    // Upgrade Task
    // =====================================================================================
    @ServiceProvider(service = UpgradeTask.class)
    public static class RdbUpgradeTask implements UpgradeTask
    {

        @Override
        public void upgrade(String oldVersion)
        {
            UpgradeManager um = UpgradeManager.getInstance();


            if (oldVersion != null)
            {
                if (oldVersion.compareTo("4") < 0)
                {
                    // package codebase has changed from JJazzLab 3 to JJazzLab 4: org/jjazz/rhythm/database => org/jjazz/rhythmdatabase
                    um.duplicateOldPreferences(prefs, "org/jjazz/rhythm/database.properties");
                }
                else if (oldVersion.compareTo("4.0.3") < 0)
                {
                    // package codebase has changed from JJazzLab 4.0.3 : org/jjazz/rhythmdatabase => org/jjazz/rhythmdatabaseimpl
                    um.duplicateOldPreferences(prefs, "org/jjazz/rhythmdatabase.properties");

                }
            }
            else
            {
                um.duplicateOldPreferences(prefs);
            }
            // Make sure rhythm database is rebuilt when upgrading
            prefs.remove(PREF_NEED_RESCAN);

        }
    }

    // =====================================================================================
    // Startup Tasks
    // =====================================================================================
    @ServiceProvider(service = OnShowingTask.class)
    public static class CopyDefaultRhythmFilesTask implements OnShowingTask
    {

        public static final int PRIORITY = 500;
        @StaticResource(relative = true)
        public static final String ZIP_RESOURCE_PATH = "resources/Rhythms.zip";
        public static final String DIR_NAME = "Rhythms";

        @Override
        public void run()
        {
            if (UpgradeManager.getInstance().isFreshStart())
            {
                initializeUserRhythmDir();
            }
        }

        @Override
        public int getPriority()
        {
            return PRIORITY;
        }

        @Override
        public String getName()
        {
            return "Copy default rhythm files";
        }

        private void initializeUserRhythmDir()
        {
            // Create the dir if it does not exists, and set it as the default user rhythm directory
            var fdm = FileDirectoryManager.getInstance();
            File dir = new File(fdm.getJJazzLabUserDirectory(), DIR_NAME);
            if (!dir.isDirectory() && !dir.mkdir())
            {
                LOGGER.log(Level.WARNING, "CopyDefaultRhythmFilesTask.initializeUserRhythmDir() Could not create directory {0}.", dir.getAbsolutePath());
            }
            else
            {
                // Copy files 
                copyFilesOrNot(dir);
                fdm.setUserRhythmDirectory(dir);
            }
        }

        /**
         * If dir is not empty ask user confirmation to replace files.
         *
         * @param dir Must exist.
         */
        private void copyFilesOrNot(File dir)
        {
            boolean isEmpty;
            try
            {
                isEmpty = Utilities.isEmpty(dir.toPath());
            }
            catch (IOException ex)
            {
                LOGGER.log(Level.WARNING, "CopyDefaultRhythmFilesTask.copyFilesOrNot() Can''t check if dir. is empty. ex={0}", ex.getMessage());
                return;
            }
            if (!isEmpty)
            {
                String msg = ResUtil.getString(getClass(), "CTL_CopyDefaultRhythmConfirmOverwrite", dir.getAbsolutePath());
                String[] options = new String[]
                {
                    "OK", ResUtil.getString(getClass(), "CTL_Skip")
                };
                NotifyDescriptor d = new NotifyDescriptor(msg, ResUtil.getString(getClass(), "CTL_FirstTimeInit"), 0, NotifyDescriptor.QUESTION_MESSAGE,
                    options,
                    "OK");
                Object result = DialogDisplayer.getDefault().notify(d);

                if (!result.equals("OK"))
                {
                    return;
                }
            }

            // Copy the default rhythms
            List<File> res = Utilities.extractZipResource(getClass(), ZIP_RESOURCE_PATH, dir.toPath(), true);
            LOGGER.log(Level.INFO, "CopyDefaultRhythmFilesTask.copyFilesOrNot() Copied {0} rhythm files to {1}", new Object[]
            {
                res.size(),
                dir.getAbsolutePath()
            });

        }

    }

}

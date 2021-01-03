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

import org.jjazz.util.MultipleErrorsReportDialog;
import org.jjazz.ui.utilities.PleaseWaitDialog;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import javax.swing.SwingUtilities;
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
import org.jjazz.util.MultipleErrorsReport;
import org.jjazz.rhythm.spi.StubRhythmProvider;
import org.jjazz.startup.spi.StartupTask;
import org.jjazz.upgrade.UpgradeManager;
import org.jjazz.upgrade.spi.UpgradeTask;
import org.jjazz.util.ResUtil;
import org.jjazz.util.Utilities;
import org.netbeans.api.annotations.common.StaticResource;
import org.openide.util.Lookup;
import org.netbeans.api.progress.ProgressHandle;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.NbPreferences;
import org.openide.util.RequestProcessor;
import org.openide.util.lookup.ServiceProvider;
import org.openide.windows.WindowManager;

/**
 * RhythmDatabase implementation.
 * <p>
 * Upon clean/fresh start:<br>
 * - retrieve all available builtin & file-based rhythm instances by polling
 * RhythmProviders (this can be long if many rhythm files need to be
 * scanned).<br>
 * - create RhythmInfo instances from the Rhythm instances and save the
 * file-based RhythmInfos to a cache file.<p>
 * Then upon normal start:<br>
 * - retrieve all available builtin rhythm instances by polling RhythmProviders,
 * create the corresponding RhythmInfos.<br>
 * - load additional file-based RhythmInfos from the cache file<br>
 * - create Rhythm instances only when required.<p>
 * <p>
 * Default rhythms are stored as Preferences.
 */
public class RhythmDatabaseImpl implements RhythmDatabase, PropertyChangeListener
{

    private static final String PREF_DEFAULT_RHYTHM = "DefaultRhythm";
    private static final String PREF_NEED_RESCAN = "NeedRescan";
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
     * The initialization task.
     */
    protected volatile RequestProcessor.Task initTask;
    /**
     * Used to store the default rhythms
     */
    private static Preferences prefs = NbPreferences.forModule(RhythmDatabaseImpl.class);

    private final ArrayList<ChangeListener> listeners = new ArrayList<>();
    private static final Logger LOGGER = Logger.getLogger(RhythmDatabaseImpl.class.getSimpleName());

    /**
     * If database is not ready yet (scanning rhythm files) then the call blocks
     * and shows a dialog to inform user we're waiting.
     *
     * @return
     */
    static public RhythmDatabaseImpl getInstance()
    {
        if (INSTANCE == null || INSTANCE.initTask == null)
        {
            // getInstance() calls should happen after initialization
            throw new IllegalStateException("INSTANCE=" + INSTANCE + " initTask=" + INSTANCE.initTask);   //NOI18N
        }

        // Init is done
        if (INSTANCE.initTask.isFinished())
        {
            return INSTANCE;
        }

        // Show a dialog while waiting for end of the init task
        PleaseWaitDialog dlg = new PleaseWaitDialog(WindowManager.getDefault().getMainWindow());

        // Add listener before showing modal dialog. If initTask is finished now directly call the listener
        INSTANCE.initTask.addTaskListener(task ->
        {
            // LOGGER.severe("getInstance().lambda.taskFinished() --");
            dlg.setVisible(false);
            dlg.dispose();
        });

        // Show dialog if really not finished (may happen just before this source code line)
        if (!INSTANCE.initTask.isFinished())
        {
            dlg.setVisible(true);
        }
        return INSTANCE;
    }

    /**
     * Create the database and start a background scanning task.
     */
    private RhythmDatabaseImpl()
    {
        // LOGGER.severe("RhythmDatabaseImpl() --");
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

    @Override
    public void forceRescan(final boolean immediate)
    {
        LOGGER.log(Level.INFO, "forceRescan() -- immediate={0}", immediate);   //NOI18N
        if (immediate)
        {
            LOGGER.warning("forceRescan() immediate=true not supported. Using immediate=false instead.");   //NOI18N
        }
        prefs.putBoolean(PREF_NEED_RESCAN, true);
    }

    @Override
    public Rhythm getRhythmInstance(RhythmInfo ri) throws UnavailableRhythmException
    {
        if (ri == null)
        {
            throw new IllegalArgumentException("ri=" + ri);   //NOI18N
        }

        Rhythm r = mapInfoInstance.get(ri);
        if (r != null)
        {
            return r;
        }

        // Builtin rhythms should not be there, they must have a RhythmInfo defined
        assert !ri.getFile().getName().equals("") : "ri=" + ri + " ri.getFile()=" + ri.getFile().getName();   //NOI18N

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
                if (ri.getUniqueId().equals(rhythmId))
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
            throw new NullPointerException("tester");   //NOI18N
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
            throw new NullPointerException("ts=" + ts);   //NOI18N
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
                TimeSignature newTs = null;

                // Parse time signature and try to get a cached instance of the AdaptedRhythm
                try
                {
                    newTs = TimeSignature.parse(strs[2]);   // Possible ParseException
                    r = mapAdaptedRhythms.get(getAdaptedRhythmKey(rIdOriginal, newTs));       // Can be null if first time request                   
                } catch (ParseException ex)
                {
                    LOGGER.warning("getRhythmInstance() Invalid time signature in AdaptedRhythm rId=" + rId);   //NOI18N
                }


                // Create the AdaptedRhythm if possible
                if (r == null && newTs != null)
                {
                    Rhythm rOriginal = getRhythmInstance(rIdOriginal);      // Possible exception here                    
                    RhythmProvider rp = getRhythmProviders().stream().filter(rhp -> rhp.getInfo().getUniqueId().equals(rpId)).findAny().orElse(null);
                    if (rp == null)
                    {
                        LOGGER.warning("getRhythmInstance() Unknown rhythm provider id in AdaptedRhythm rId=" + rId);   //NOI18N
                    } else
                    {
                        r = rp.getAdaptedRhythm(rOriginal, newTs);        // Can be null!
                    }
                }
            }
        } else
        {
            RhythmInfo ri = getRhythm(rId);     // Can be null
            if (ri != null)
            {
                r = getRhythmInstance(ri);      // Possible UnavailableRhythmException here
            }
        }


        if (r == null)
        {
            throw new UnavailableRhythmException("No rhythm found for id=" + rId);
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
            throw new IllegalArgumentException("rp=" + rp);   //NOI18N
        }
        List<RhythmInfo> rpRhythms = mapRpRhythms.get(rp);
        if (rpRhythms == null)
        {
            throw new IllegalArgumentException("RhythmProvider not found rp=" + rp.getInfo().getName() + '@' + Integer.toHexString(rp.hashCode()));   //NOI18N
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
                .collect(Collectors.toList());

        assert rhythms.size() > 0 : " mapRpRhythms=" + this.mapRpRhythms;   //NOI18N

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
            throw new IllegalArgumentException("ts=" + ts + " ri=" + null);   //NOI18N
        }

        if (getRhythm(ri.getUniqueId()) == null)
        {
            throw new IllegalArgumentException("Rhythm ri not in this database. ts=" + ts + " r=" + ri);   //NOI18N
        }

        // Store the uniqueId of the Rhythm as a preference
        prefs.put(getPrefString(ts), ri.getUniqueId());
    }

    @Override
    public Rhythm getDefaultStubRhythmInstance(TimeSignature ts)
    {
        var srp = StubRhythmProvider.getDefault();
        return srp.getStubRhythm(ts);
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
            throw new IllegalArgumentException("r=" + r + " ts=" + ts);   //NOI18N
        }

        AdaptedRhythm ar = mapAdaptedRhythms.get(getAdaptedRhythmKey(r.getUniqueId(), ts));
        if (ar == null)
        {
            for (RhythmProvider rp : getRhythmProviders())
            {
                ar = rp.getAdaptedRhythm(r, ts);
                if (ar != null)
                {
                    addRhythm(rp, ar);
                    mapAdaptedRhythms.put(getAdaptedRhythmKey(r.getUniqueId(), ts), ar);
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
            throw new IllegalArgumentException("ri=" + ri);   //NOI18N
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
    public RhythmProvider getRhythmProvider(String rpId)
    {
        return getRhythmProviders()
                .stream()
                .filter(rp -> rp.getInfo().getUniqueId().equals(rpId))
                .findAny()
                .orElse(null);
    }

    @Override
    public List<RhythmProvider> getRhythmProviders()
    {
        List<RhythmProvider> res = new ArrayList<>(Lookup.getDefault().lookupAll(RhythmProvider.class));
        res.sort((rp1, rp2) -> rp1.getInfo().getName().compareTo(rp2.getInfo().getName()));
        return res;
    }

    @Override
    public int addExtraRhythms(List<RpRhythmPair> pairs)
    {
        if (pairs == null)
        {
            throw new NullPointerException("pairs");   //NOI18N
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
        LOGGER.fine("propertyChange() evt=" + evt);   //NOI18N
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
     * @return True if ok
     */
    private void initDatabase(ProgressHandle ph, boolean needRescan)
    {
        File rDir = FileDirectoryManager.getInstance().getUserRhythmDirectory();
        boolean cacheFilePresent = RhythmDbCache.getFile().isFile();
        LOGGER.info("initDatabase() needRescan=" + needRescan + " cacheFilePresent=" + cacheFilePresent);   //NOI18N

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

        } else
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

            } catch (IOException ex)
            {
                // Notify
                LOGGER.warning("RhythmDatabaseImpl() Can't load cache file. ex=" + ex.getLocalizedMessage());   //NOI18N
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
            LOGGER.warning("addNewRhythmsFromRhythmProviders() - no RhythmProvider found, database might be empty");   //NOI18N
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
                        new MultipleErrorsReportDialog(WindowManager.getDefault().getMainWindow(), ResUtil.getString(getClass(), "CTL_BuiltinRhythmErrors"), builtinErrRpt).setVisible(true);
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
                        new MultipleErrorsReportDialog(WindowManager.getDefault().getMainWindow(), ResUtil.getString(getClass(), "CTL_FileBasedRhythmErrors"), fileErrRpt).setVisible(true);
                    }
                });
            }

        }

        LOGGER.info("addNewRhythmsFromRhythmProviders() excludeBuiltinRhythms=" + excludeBuiltinRhythms //NOI18N
                + " excludeFileRhythms=" + excludeFileRhythms + " forceFileRescan=" + forceFileRescan + ". Added " + n + " rhythms");

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
                LOGGER.info("writeCache.run() cache file created, size=" + cacheFile.getSize());   //NOI18N
            } catch (IOException ex)
            {
                LOGGER.severe("writeCache.run() Can't save cache file=" + RhythmDbCache.getFile().getAbsolutePath() + ". ex=" + ex.getLocalizedMessage());   //NOI18N
                prefs.putBoolean(PREF_NEED_RESCAN, true);
            }
        };

        // Can be safely done in another thread
        new Thread(run).start();

    }

    /**
     * Read the cache file and update the database accordingly.
     *
     * @param cache
     * @return The number of added RhythmInfo instances
     */
    private void readCache() throws IOException
    {
        // Read the file
        File f = RhythmDbCache.getFile();
        RhythmDbCache cache = null;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f)))
        {
            cache = (RhythmDbCache) ois.readObject();

        } catch (ClassNotFoundException ex)
        {
            throw new IOException(ex);
        }

        assert cache != null;   //NOI18N

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
                LOGGER.warning("readCache.run() No RhythmProvider found for rpId=" + rpId + ". Ignoring " + rhythms.size() + " rhythms.");   //NOI18N
                continue;
            }

            // Update state
            var rpRhythms = mapRpRhythms.get(rp);
            if (rpRhythms == null)
            {
                rpRhythms = new ArrayList<RhythmInfo>();
                mapRpRhythms.put(rp, rpRhythms);
            }
            rpRhythms.addAll(rhythms);
        }

        // cache.dump();
        LOGGER.info("readCache() Successfully read rhythm list from cache, size=" + cache.getSize());   //NOI18N

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
            mapInfoInstance.put(ri, r);
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
        LOGGER.fine("fireChanged()");   //NOI18N
        for (ChangeListener l : listeners)
        {
            l.stateChanged(e);
        }
    }

    private void dump()
    {
        LOGGER.info("RhythmDatabaseImpl dump ----- RhythmInfo instances");   //NOI18N
        for (RhythmInfo ri : this.getRhythms())
        {
            LOGGER.info("  " + ri.toString());   //NOI18N
        }
        LOGGER.info("RhythmDatabaseImpl dump ----- Rhythm instances");   //NOI18N
        for (RhythmInfo ri : this.mapInfoInstance.keySet())
        {
            LOGGER.info("  " + ri.toString() + " -> RhythmInstance.isResourcesLoaded()=" + mapInfoInstance.get(ri).isResourcesLoaded());   //NOI18N
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
        LOGGER.info("logStats() Rythm Database stats - total=" + size());   //NOI18N
        for (RhythmProvider rp : mapRpRhythms.keySet())
        {
            var rhythms = mapRpRhythms.get(rp);
            long nbBuiltins = rhythms.stream()
                    .filter(ri -> ri.getFile().getName().equals(""))
                    .count();
            long nbFiles = rhythms.size() - nbBuiltins;
            String firstRhythm = rhythms.isEmpty() ? "" : "first=" + rhythms.get(0).toString() + "...";

            LOGGER.info("  > " + rp.getInfo().getName() + ": total=" + rhythms.size() + " builtin=" + nbBuiltins + " file=" + nbFiles + " " + firstRhythm);   //NOI18N
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
            // Copy preferences
            UpgradeManager um = UpgradeManager.getInstance();
            um.duplicateOldPreferences(prefs);

        }
    }

    // =====================================================================================
    // Startup Tasks
    // =====================================================================================
    @ServiceProvider(service = StartupTask.class)
    public static class CopyDefaultRhythmFilesTask implements StartupTask
    {

        public static final int PRIORITY = 500;
        @StaticResource(relative = true)
        public static final String ZIP_RESOURCE_PATH = "resources/Rhythms.zip";
        public static final String DIR_NAME = "Rhythms";

        @Override
        public boolean run()
        {
            if (!UpgradeManager.getInstance().isFreshStart())
            {
                return false;
            } else
            {
                initializeUserRhythmDir();
                return true;
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
                LOGGER.warning("CopyDefaultRhythmFilesTask.initializeUserRhythmDir() Could not create directory " + dir.getAbsolutePath() + ".");   //NOI18N
            } else
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
            } catch (IOException ex)
            {
                LOGGER.warning("CopyDefaultRhythmFilesTask.copyFilesOrNot() Can't check if dir. is empty. ex=" + ex.getLocalizedMessage());   //NOI18N
                return;
            }
            if (!isEmpty)
            {
                String msg = ResUtil.getString(getClass(), "CTL_CopyDefaultRhythmConfirmOverwrite", dir.getAbsolutePath());
                String[] options = new String[]
                {
                    "OK", ResUtil.getString(getClass(), "CTL_Skip")
                };
                NotifyDescriptor d = new NotifyDescriptor(msg, ResUtil.getString(getClass(), "CTL_FirstTimeInit"), 0, NotifyDescriptor.QUESTION_MESSAGE, options, "OK");
                Object result = DialogDisplayer.getDefault().notify(d);

                if (!result.equals("OK"))
                {
                    return;
                }
            }

            // Copy the default rhythms
            List<File> res = Utilities.extractZipResource(getClass(), ZIP_RESOURCE_PATH, dir.toPath(), true);
            LOGGER.info("CopyDefaultRhythmFilesTask.copyFilesOrNot() Copied " + res.size() + " rhythm files to " + dir.getAbsolutePath());   //NOI18N

        }

    }

    /**
     * Create the database instance once the CopyDefaultRhythmFilesTask is
     * complete.
     */
    @ServiceProvider(service = StartupTask.class)
    public static class CreateDatabaseTask implements StartupTask
    {

        public static final int PRIORITY = CopyDefaultRhythmFilesTask.PRIORITY + 1;

        @Override
        public boolean run()
        {
            INSTANCE = new RhythmDatabaseImpl();
            return true;
        }

        @Override
        public int getPriority()
        {
            return PRIORITY;
        }

        @Override
        public String getName()
        {
            return "Create Rhythm database";
        }

    }
}

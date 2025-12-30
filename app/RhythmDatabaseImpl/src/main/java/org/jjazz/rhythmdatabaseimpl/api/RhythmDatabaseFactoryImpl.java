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
package org.jjazz.rhythmdatabaseimpl.api;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.SwingUtilities;
import org.jjazz.coreuicomponents.api.MultipleErrorsReportDialog;
import org.jjazz.rhythm.spi.RhythmDirsLocator;
import org.jjazz.rhythmdatabase.api.DefaultRhythmDatabase;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.jjazz.rhythmdatabase.spi.RhythmDatabaseFactory;
import org.jjazz.rhythmdatabaseimpl.RhythmDbCache;
import org.jjazz.uiutilities.api.PleaseWaitDialog;
import org.jjazz.upgrade.api.UpgradeManager;
import org.jjazz.utilities.api.CheckedRunnable;
import org.jjazz.utilities.api.ResUtil;
import org.openide.util.lookup.ServiceProvider;
import org.jjazz.utilities.api.MultipleErrorsReport;
import org.jjazz.utilities.api.Utilities;
import org.netbeans.api.annotations.common.StaticResource;
import org.netbeans.api.progress.ProgressHandle;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbPreferences;

/**
 * Create and initialize the RhythmDatabase instance via a cache file.
 * <p>
 * Upon clean/fresh start:<br>
 * - copy default rhythm files<br>
 * - retrieve all available builtin &amp; file-based rhythm instances by polling RhythmProviders (this can be long if many rhythm files need to be scanned).<br>
 * - update the database<br>
 * - save the file-based RhythmInfos to a cache file.<p>
 * Then upon normal start:<br>
 * - retrieve all available builtin rhythm instances by polling RhythmProviders, create the corresponding RhythmInfos.<br>
 * - load additional file-based RhythmInfos from the cache file<br>
 * - create Rhythm instances only when required.<p>
 */
@ServiceProvider(service = RhythmDatabaseFactory.class)
public class RhythmDatabaseFactoryImpl implements RhythmDatabaseFactory, PropertyChangeListener
{

    @StaticResource(relative = true)
    public static final String ZIP_RESOURCE_PATH = "resources/Rhythms.zip";

    public static final String PREF_NEED_RESCAN = "NeedRescan";
    private static RhythmDatabaseFactoryImpl INSTANCE;
    private final DefaultRhythmDatabase dbInstance;
    private Future<?> initFuture;
    /**
     * Stores PREF_NEED_RESCAN
     */
    private static final Preferences prefs = NbPreferences.forModule(RhythmDatabaseFactoryImpl.class);
    private static final Logger LOGGER = Logger.getLogger(RhythmDatabaseFactoryImpl.class.getSimpleName());

    static public RhythmDatabaseFactoryImpl getInstance()
    {
        synchronized (RhythmDatabaseFactoryImpl.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = Lookup.getDefault().lookup(RhythmDatabaseFactoryImpl.class);
                assert INSTANCE != null;
            }
        }
        return INSTANCE;
    }

    /**
     * Do not use, use getInstance() instead.
     * <p>
     * This was made public because of ServiceProvider.
     */
    public RhythmDatabaseFactoryImpl()
    {
        if (INSTANCE != null)
        {
            throw new IllegalStateException("INSTANCE is not null");
        }

        dbInstance = DefaultRhythmDatabase.getInstance(prefs);

        // Be notified of user rhythm directory location changes
        RhythmDirsLocator rdl = RhythmDirsLocator.getDefault();
        rdl.addPropertyChangeListener(this);

        INSTANCE = this;
    }

    @Override
    public Future<?> initialize()
    {
        if (initFuture == null)
        {

            if (UpgradeManager.getInstance().isFreshStart())
            {
                // This could be done in doInitialization(), but doInitialization() is run in a separate thread, and because copyFilesOrNot might show user 
                // a confirmation dialog, we want to hold the task so that the next OnShowingTask is started *after* this user confirmation.
                LOGGER.info("initialize() Copying default rhythm files");
                copyDefaultRhythmFilesOrNot(RhythmDirsLocator.getDefault().getUserRhythmsDirectory());
            }


            ExecutorService executor = Executors.newSingleThreadExecutor();
            initFuture = executor.submit(new CheckedRunnable(() -> doInitialization()));
        }
        return initFuture;
    }

    @Override
    public boolean isInitialized()
    {
        return initFuture != null && initFuture.isDone();
    }


    @Override
    public RhythmDatabase get()
    {
        boolean b = isInitialized();
        LOGGER.log(Level.FINE, "get() -- isInitialized()={0}", b);

        if (!b)
        {
            if (initFuture == null)
            {
                initialize();
            }

            // Show a "please wait" dialog until initialization's complete
            String msg = ResUtil.getString(RhythmDatabaseFactoryImpl.class, "CTL_PleaseWait");
            PleaseWaitDialog.show(msg, initFuture);
        }
        return dbInstance;
    }

    /**
     * Request or cancel a full rescan upon next startup.
     *
     * @param b
     */
    public void markForStartupRescan(boolean b)
    {
        LOGGER.log(Level.INFO, "markForStartupRescan() b={0}", b);
        prefs.putBoolean(PREF_NEED_RESCAN, b);
    }

    /**
     * Check if a full rescan s planned for next startup.
     *
     * @return
     */
    public boolean isMarkedForStartupRescan()
    {
        return prefs.getBoolean(PREF_NEED_RESCAN, true);
    }
    // ---------------------------------------------------------------------
    // PropertyChangeListener interface 
    // --------------------------------------------------------------------- 

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        LOGGER.log(Level.FINE, "propertyChange() evt={0}", evt);
        if (evt.getSource() == RhythmDirsLocator.getDefault())
        {
            if (evt.getPropertyName().equals(RhythmDirsLocator.PROP_RHYTHM_USER_DIRECTORY))
            {
                // Directory has changed, plan a rescan
                markForStartupRescan(true);
            }
        }
    }


    // ================================================================================================
    // Private methods
    // ================================================================================================
    /**
     * Read the cache or ask RhythmProviders to rescan all the rhythm files.
     */
    private void doInitialization()
    {
        boolean isFreshStart = UpgradeManager.getInstance().isFreshStart();
        boolean markedForRescan = isMarkedForStartupRescan();
        boolean cacheFilePresent = RhythmDbCache.getDefaultFile().isFile();
        LOGGER.log(Level.INFO, "doInitialization() isFreshStart={0} markedForRescan={1} cacheFilePresent={2}", new Object[]
        {
            isFreshStart, markedForRescan, cacheFilePresent
        });


        String msgScanAll = ResUtil.getString(getClass(), "CTL_ScanningAllRhythmsInDir",
                RhythmDirsLocator.getDefault().getUserRhythmsDirectory().getAbsolutePath());


        if (isFreshStart || markedForRescan || !cacheFilePresent)
        {
            // FULL SCAN

            // Prepare the ProgressHandle                    
            ProgressHandle ph = ProgressHandle.createHandle(msgScanAll);
            ph.start();


            // Poll RhythmProviders
            MultipleErrorsReport errReport = dbInstance.addRhythmsFromRhythmProviders(false, false, true);


            // Save cache file
            writeCacheInSeparateThread();


            ph.finish();


            // Notify user of possible errors
            if (errReport.primaryErrorMessage != null)
            {
                SwingUtilities.invokeLater(()
                        -> new MultipleErrorsReportDialog(ResUtil.getString(getClass(), "CTL_FileBasedRhythmErrors"), errReport).setVisible(true)
                );
            }

        } else
        {
            // REUSE CACHE FILE


            // Prepare the ProgressHandle                    
            String msg = ResUtil.getString(getClass(), "CTL_ScanningAllBuiltinRhythms");
            ProgressHandle ph = ProgressHandle.createHandle(msg);
            ph.start();


            // Scan only for builtin Rhythms
            MultipleErrorsReport errReport = dbInstance.addRhythmsFromRhythmProviders(false, true, false);


            // Notify errors
            if (errReport.primaryErrorMessage != null)
            {
                SwingUtilities.invokeLater(()
                        -> new MultipleErrorsReportDialog(ResUtil.getString(getClass(), "CTL_BuiltinRhythmErrors"), errReport).setVisible(true)
                );
            }


            // Read cache
            msg = ResUtil.getString(getClass(), "CTL_ReadingRhythmDbCacheFile");
            ph.setDisplayName(msg);

            try
            {
                int added = RhythmDbCache.loadFromFile(RhythmDbCache.getDefaultFile(), dbInstance);
                LOGGER.log(Level.INFO, "doInitialization() Successfully added {0} RhythmInfos from the cache to the database", added);

            } catch (IOException ex)
            {
                LOGGER.log(Level.SEVERE, "doInitialization() Can''t load cache file, starting full scan... IOException ex={0}", ex.getMessage());

                // Start a full scan (except for built-in rhythms since we already have them)
                ph.progress(msgScanAll);
                dbInstance.addRhythmsFromRhythmProviders(true, false, true);     // Ignore errors
                writeCacheInSeparateThread();

            } catch (ClassNotFoundException ex)
            {
                Exceptions.printStackTrace(ex);
            }

            ph.finish();

        }

        LOGGER.info(dbInstance.toStatsString());

    }


    private void writeCacheInSeparateThread()
    {
        RhythmDbCache cache = new RhythmDbCache(dbInstance);
        File file = RhythmDbCache.getDefaultFile();

        Runnable task = () -> 
        {
            try
            {
                cache.saveToFile(file);
                markForStartupRescan(false);
                LOGGER.log(Level.INFO, "writeCacheInSeparateThread() cache file created, size={0}", cache.getSize());
            } catch (IOException ex)
            {
                LOGGER.log(Level.SEVERE, "writeCacheInSeparateThread() Can''t save cache file={0}. ex={1}", new Object[]
                {
                    file.getAbsolutePath(),
                    ex.getMessage()
                });

                markForStartupRescan(true);
            }
        };

        new Thread(task).start();

    }


    /**
     * If dir is not empty ask user for confirmation to replace files.
     *
     * @param dir Must exist.
     */
    private void copyDefaultRhythmFilesOrNot(File dir)
    {
        boolean isEmpty;
        try
        {
            isEmpty = Utilities.isEmpty(dir.toPath());
        } catch (IOException ex)
        {
            LOGGER.log(Level.WARNING, "copyDefaultRhythmFilesOrNot() Can''t check if dir. is empty. ex={0}", ex.getMessage());
            return;
        }
        if (!isEmpty)
        {
            String msg = ResUtil.getString(getClass(), "CTL_CopyDefaultRhythmConfirmOverwrite", dir.getAbsolutePath());
            String[] options = new String[]
            {
                "OK", ResUtil.getString(getClass(), "SKIP")
            };
            NotifyDescriptor d = new NotifyDescriptor(msg, ResUtil.getString(getClass(), "CTL_FirstTimeInit"), 0, NotifyDescriptor.QUESTION_MESSAGE, options,
                    "OK");
            Object result = DialogDisplayer.getDefault().notify(d);
            if (!result.equals("OK"))
            {
                return;
            }
        }
        // Copy the default rhythms
        List<File> res = Utilities.extractZipResource(getClass(), ZIP_RESOURCE_PATH, dir.toPath(), true);
        LOGGER.log(Level.INFO, "copyDefaultRhythmFilesOrNot() Copied {0} rhythm files to {1}",
                new Object[]
                {
                    res.size(), dir.getAbsolutePath()
                });
    }

}

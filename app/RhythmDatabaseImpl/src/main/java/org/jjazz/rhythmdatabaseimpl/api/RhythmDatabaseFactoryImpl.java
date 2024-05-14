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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.SwingUtilities;
import org.jjazz.coreuicomponents.api.MultipleErrorsReportDialog;
import org.jjazz.filedirectorymanager.api.FileDirectoryManager;
import org.jjazz.rhythmdatabase.api.DefaultRhythmDatabase;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.jjazz.rhythmdatabase.spi.RhythmDatabaseFactory;
import org.jjazz.rhythmdatabaseimpl.RhythmDbCache;
import org.jjazz.uiutilities.api.PleaseWaitDialog;
import org.jjazz.utilities.api.ResUtil;
import org.openide.util.lookup.ServiceProvider;
import org.jjazz.utilities.api.MultipleErrorsReport;
import org.netbeans.api.progress.ProgressHandle;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbPreferences;
import org.openide.util.RequestProcessor;

/**
 * Create and initialize the RhythmDatabase instance via a cache file.
 * <p>
 * Upon clean/fresh start:<br>
 * - retrieve all available builtin & file-based rhythm instances by polling RhythmProviders (this can be long if many rhythm files need to be scanned).<br>
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

    public static final String PREF_NEED_RESCAN = "NeedRescan";
    private static RhythmDatabaseFactoryImpl INSTANCE;
    private final DefaultRhythmDatabase dbInstance;
    private Future<?> initFuture;
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

        dbInstance = DefaultRhythmDatabase.getInstance();

        // Be notified of user rhythm directory location changes
        FileDirectoryManager.getInstance().addPropertyChangeListener(this);

        INSTANCE = this;
    }

    @Override
    public Future<?> initialize()
    {
        if (initFuture == null)
        {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            initFuture = executor.submit(() -> doInitialization());
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
        if (!isInitialized())
        {
            // Show a "please wait" dialog until initialization's complete
            String msg = ResUtil.getString(RhythmDatabaseFactoryImpl.class, "CTL_PleaseWait");
            PleaseWaitDialog.show(msg, initFuture);
            LOGGER.severe("=====================================\n=================================get() CHECK BUG POSSIBLY HERE CALLED with initFuture==null");
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
        if (evt.getSource() == FileDirectoryManager.getInstance())
        {
            if (evt.getPropertyName().equals(FileDirectoryManager.PROP_RHYTHM_USER_DIRECTORY))
            {
                // Directory has changed, plan a rescan
                markForStartupRescan(true);
            }
        }
    }


    // ================================================================================================
    // Private methods
    // ================================================================================================
    private void doInitialization()
    {
        boolean markedForRescan = isMarkedForStartupRescan();
        boolean cacheFilePresent = RhythmDbCache.getDefaultFile().isFile();
        LOGGER.log(Level.INFO, "doInitialization() markedForRescan={0} cacheFilePresent={1}", new Object[]
        {
            markedForRescan, cacheFilePresent
        });


        String msgScanAll = ResUtil.getString(getClass(), "CTL_ScanningAllRhythmsInDir",
                FileDirectoryManager.getInstance().getUserRhythmsDirectory().getAbsolutePath());


        if (markedForRescan || !cacheFilePresent)
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
            ph.progress(msg);
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


}

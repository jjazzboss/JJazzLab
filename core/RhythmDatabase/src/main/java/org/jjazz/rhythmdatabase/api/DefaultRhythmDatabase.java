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
package org.jjazz.rhythmdatabase.api;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import org.jjazz.rhythmdatabase.spi.RhythmDatabaseFactory;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.rhythm.api.AdaptedRhythm;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.spi.RhythmProvider;
import org.jjazz.rhythm.spi.StubRhythmProvider;
import org.jjazz.utilities.api.MultipleErrorsReport;
import org.openide.util.NbPreferences;

/**
 * RhythmDatabase default implementation.
 * <p>
 * Default rhythms are stored as Preferences.
 */
public class DefaultRhythmDatabase implements RhythmDatabase
{
    
    private static DefaultRhythmDatabase INSTANCE;
    private static DefaultFactory INSTANCE_FACTORY;
    private static final String PREF_DEFAULT_RHYTHM = "DefaultRhythm";

    /**
     * Main data structure
     */
    private final ListMultimap<RhythmProvider, RhythmInfo> mmapRpRinfos;
    /**
     * Save the created Rhythm instances.
     */
    private final Map<RhythmInfo, Rhythm> mapRinfoInstance;
    /**
     * Keep the AdaptedRhythms instances created on-demand.
     * <p>
     * Map key=originalRhythmId-TimeSignature
     */
    private final Map<String, AdaptedRhythm> mapAdaptedRhythms;
    /**
     * Stores the default rhythms per time signature
     */
    private final Preferences prefs;
    private final ArrayList<ChangeListener> listeners = new ArrayList<>();
    private static final Logger LOGGER = Logger.getLogger(DefaultRhythmDatabase.class.getSimpleName());

    /**
     *
     * @param prefs The preferences to store the default rhythms (per time signature)
     * @return
     */
    public static DefaultRhythmDatabase getInstance(Preferences prefs)
    {
        synchronized (DefaultRhythmDatabase.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new DefaultRhythmDatabase(prefs);
            }
        }
        return INSTANCE;
    }

    /**
     * Get a factory which just provides the DefaultRhythmDatabase instance.
     *
     * @return
     */
    public static RhythmDatabaseFactory getFactoryInstance()
    {
        synchronized (DefaultFactory.class)
        {
            if (INSTANCE_FACTORY == null)
            {
                INSTANCE_FACTORY = new DefaultFactory();
            }
        }
        return INSTANCE_FACTORY;
    }
    
    private DefaultRhythmDatabase(Preferences prefs)
    {
        Objects.requireNonNull(prefs);
        this.prefs = prefs;
        
        this.mapAdaptedRhythms = new HashMap<>();
        this.mapRinfoInstance = new HashMap<>();
        this.mmapRpRinfos = MultimapBuilder.hashKeys().arrayListValues().build();
    }
    
    @Override
    public Rhythm getRhythmInstance(RhythmInfo ri) throws UnavailableRhythmException
    {
        Objects.requireNonNull(ri);
        
        Rhythm r = mapRinfoInstance.get(ri);
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
        } catch (IOException ex)
        {
            throw new UnavailableRhythmException(ex.getLocalizedMessage());
        }
        
        if (!ri.checkConsistency(rp, r))
        {
            throw new UnavailableRhythmException("Inconsistency detected for rhythm " + ri + ". Consider refreshing the rhythm database.");
        }

        // Save the instance
        mapRinfoInstance.put(ri, r);
        
        return r;
    }
    
    @Override
    public RhythmInfo getRhythm(String rhythmId)
    {
        for (RhythmProvider rp : mmapRpRinfos.keySet())
        {
            for (RhythmInfo ri : mmapRpRinfos.get(rp))
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
        for (RhythmProvider rp : mmapRpRinfos.keySet())
        {
            for (RhythmInfo ri : mmapRpRinfos.get(rp))
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
    public List<RhythmProvider> getRhythmProviders()
    {
        var res = new ArrayList<>(mmapRpRinfos.keySet());
        return res;
    }
    
    @Override
    public Rhythm getRhythmInstance(String rId) throws UnavailableRhythmException
    {
        Rhythm r = null;
        
        if (rId.contains(AdaptedRhythm.RHYTHM_ID_DELIMITER))
        {
            // It's an adapted rhythm
            String[] strs = rId.split(AdaptedRhythm.RHYTHM_ID_DELIMITER);
            if (strs.length == 3)
            {
                String rpId = strs[0];
                String rIdOriginal = strs[1];
                TimeSignature newTs = null;
                
                try
                {
                    newTs = TimeSignature.parse(strs[2]);   // Possible ParseException
                } catch (ParseException ex)
                {
                    LOGGER.log(Level.WARNING, "getRhythmInstance() Invalid time signature in AdaptedRhythm rId={0}", rId);
                    throw new UnavailableRhythmException("Invalid time signature in adapted rhythm id=" + rId);
                }
                
                Rhythm rOriginal = getRhythmInstance(rIdOriginal);      // Possible UnavailableRhythmException exception here                   
                r = getAdaptedRhythmInstance(rOriginal, newTs);         // Can be null

            }
        } else
        {
            // This a standard rhythm
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
                    addRhythmInstance(rp, ar);
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
        Objects.requireNonNull(rp);
        var res = Collections.unmodifiableList(mmapRpRinfos.get(rp));
        return res;
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
        
        assert rhythms.size() > 0 : " mapRpRhythms=" + this.mmapRpRinfos;

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
    public RhythmProvider getRhythmProvider(Rhythm r)
    {
        RhythmProvider resRp = null;
        RhythmInfo ri = getRhythm(r.getUniqueId());
        for (RhythmProvider rp : mmapRpRinfos.keySet())
        {
            if (mmapRpRinfos.get(rp).contains(ri))
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
        for (RhythmProvider rp : mmapRpRinfos.keySet())
        {
            for (RhythmInfo rInfo : mmapRpRinfos.get(rp))
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
    public boolean addRhythm(RhythmProvider rp, RhythmInfo ri)
    {
        boolean b = false;
        
        List<RhythmInfo> rhythmInfos = mmapRpRinfos.get(rp);        
        if (!rhythmInfos.contains(ri))
        {
            rhythmInfos.add(ri);
            fireChanged();
            b = true;
        } else
        {
            String filePath = ri.file() == null ? "null" : ri.file().getAbsolutePath();
            LOGGER.log(Level.WARNING, "addRhythm() Duplicate RhythmInfo ignored {0} (file={1})", new Object[]
            {
                ri, filePath
            });
        }
        
        return b;
    }
    
    @Override
    public boolean addRhythmInstance(RhythmProvider rp, Rhythm r)
    {
        RhythmInfo ri = new RhythmInfo(r, rp);
        boolean added = addRhythm(rp, ri);
        if (added)
        {
            mapRinfoInstance.put(ri, r);
        }
        return added;
    }
    
    @Override
    public String toString()
    {
        return "RhythmDB=" + getRhythms();
    }
    
    @Override
    public int size()
    {
        return mmapRpRinfos.size();
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

    /**
     * Poll RhythmProvider instances from the global lookup to add rhythms.
     *
     * @param excludeBuiltinRhythms
     * @param excludeFileRhythms
     * @param forceFileRhythmsRescan Unused when excludedFileRhythms is true
     * @return
     * @see RhythmProvider
     */
    public MultipleErrorsReport addRhythmsFromRhythmProviders(boolean excludeBuiltinRhythms, boolean excludeFileRhythms, boolean forceFileRhythmsRescan)
    {
        
        var rps = RhythmProvider.getRhythmProviders();
        final MultipleErrorsReport errReport = new MultipleErrorsReport();
        
        int n = 0; 
        for (final RhythmProvider rp : rps)
        {
            LOGGER.log(Level.INFO, "addRhythmsFromRhythmProviders() rp={0}", rp.getInfo().getName());
            
            // First get builtin rhythms         
            if (!excludeBuiltinRhythms)
            {
                for (Rhythm r : rp.getBuiltinRhythms(errReport))
                {
                    if (addRhythmInstance(rp, r))
                    {
                        n++;
                    }
                }
            }


            // Add file-based rhythms
            if (!excludeFileRhythms)
            {
                List<Rhythm> rhythmsNotBuiltin = rp.getFileRhythms(forceFileRhythmsRescan, errReport);
                for (Rhythm r : rhythmsNotBuiltin)
                {
                    if (addRhythmInstance(rp, r))
                    {
                        n++;
                    }
                }
            }
        }
        
        Logger.getLogger(RhythmDatabase.class.getSimpleName()).log(Level.FINE,
            "addRhythmsFromRhythmProviders() excludeBuiltinRhythms={0} excludeFileRhythms={1} forceFileRhythmsRescan={2}:  added {3} rhythms",
            new Object[]
            {
                excludeBuiltinRhythms,
                excludeFileRhythms, forceFileRhythmsRescan, n
            });
        
        
        return errReport;
    }

    // ---------------------------------------------------------------------
    // Private 
    // --------------------------------------------------------------------- 
    private String getPrefString(TimeSignature ts)
    {
        return PREF_DEFAULT_RHYTHM + "__" + ts.name();
    }
    
    private void fireChanged()
    {
        LOGGER.fine("fireChanged()");
        var e = new ChangeEvent(this);
        for (ChangeListener l : listeners)
        {
            l.stateChanged(e);
        }
    }
    
    private String getAdaptedRhythmKey(String rId, TimeSignature ts)
    {
        return rId + "-" + ts.name();
        
    }

    // ================================================================================================
    // Inner classes
    // ================================================================================================  
    private static class DefaultFactory implements RhythmDatabaseFactory
    {
        
        @Override
        public Future<?> initialize()
        {
            return new FutureTask(() ->            
            {
            }, null);
        }
        
        @Override
        public boolean isInitialized()
        {
            return true;
        }
        
        @Override
        public RhythmDatabase get()
        {
            return getInstance(NbPreferences.forModule(DefaultRhythmDatabase.class));
        }
        
    }
    
}

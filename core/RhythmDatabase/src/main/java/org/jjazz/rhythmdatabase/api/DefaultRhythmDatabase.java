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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * A default (and basic) implementation which is used by RhythmDatabaseFactory if no other RyhthmDatabase instance is found in global lookup.
 *
 * The database is initialized with the rhythms from the available RhythmProvider instances. If no rhythm instances found, add StubRhythms
 * from the StubRhythmProvider instance.
 */
public class DefaultRhythmDatabase implements RhythmDatabase
{

    private static final String PREF_DEFAULT_RHYTHM = "DefaultRhythm";
    /**
     * Main data structure
     */
    private final Map<RhythmProvider, List<RhythmInfo>> mapRpRhythms = new HashMap<>();
    /**
     * Save the created Rhythm instances.
     */
    private final Map<RhythmInfo, Rhythm> mapInfoInstance = new HashMap<>();
    private final ArrayList<ChangeListener> listeners = new ArrayList<>();
    /**
     * Used to store the default rhythms
     */
    private static final Preferences prefs = NbPreferences.forModule(DefaultRhythmDatabase.class);
    private static final Logger LOGGER = Logger.getLogger(DefaultRhythmDatabase.class.getSimpleName());


    public DefaultRhythmDatabase()
    {
        int added = addNewRhythmsFromRhythmProviders(false, false, false);

        if (added == 0)
        {
            LOGGER.info("DefaultRhythmDatabase() empty database, adding RhythmStub instances.");
            var srp = StubRhythmProvider.getDefault();
            for (var ts : TimeSignature.values())
            {
                addRhythm(srp, srp.getStubRhythm(ts));
                added++;
            }
        }
        LOGGER.log(Level.INFO, "DefaultRhythmDatabase() initialized with {0} rhythms", added);
    }


    @Override
    public void forceRescan(final boolean immediate)
    {
        LOGGER.log(Level.INFO, "forceRescan() ignored");
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
        return null;
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

        assert !rhythms.isEmpty() : " mapRpRhythms=" + this.mapRpRhythms;

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

    public void dump()
    {
        LOGGER.info("DefaultRhythmDatabase dump ----- RhythmInfo instances");
        for (RhythmInfo ri : this.getRhythms())
        {
            LOGGER.log(Level.INFO, "  {0}", ri.toString());
        }
        LOGGER.info("DefaultRhythmDatabase dump ----- Rhythm instances");
        for (RhythmInfo ri : this.mapInfoInstance.keySet())
        {
            LOGGER.log(Level.INFO, "  {0} -> RhythmInstance.isResourcesLoaded()={1}", new Object[]
            {
                ri.toString(),
                mapInfoInstance.get(ri).isResourcesLoaded()
            });
        }

    }

    // ---------------------------------------------------------------------
    // Private 
    // --------------------------------------------------------------------- 
    /**
     * Add rhythms from the available RhythmProvider instances.
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
            return 0;
        }

        int n = 0;
        for (final RhythmProvider rp : rps)
        {

            // First get builtin rhythms         
            MultipleErrorsReport errReport = new MultipleErrorsReport();
            if (!excludeBuiltinRhythms)
            {
                for (Rhythm r : rp.getBuiltinRhythms(errReport))
                {
                    if (addRhythm(rp, r))
                    {
                        n++;
                    }
                }
            }

            // Notify user of possible errors
            if (errReport.primaryErrorMessage != null)
            {
                LOGGER.log(Level.WARNING, "addNewRhythmsFromRhythmProviders() {0}", errReport.primaryErrorMessage);
            }


            // Add file rhythms
            errReport = new MultipleErrorsReport();
            if (!excludeFileRhythms)
            {
                List<Rhythm> rhythmsNotBuiltin = rp.getFileRhythms(forceFileRescan, errReport);
                for (Rhythm r : rhythmsNotBuiltin)
                {
                    if (addRhythm(rp, r))
                    {
                        n++;
                    }
                }
            }

            if (errReport.primaryErrorMessage != null)
            {
                LOGGER.log(Level.WARNING, "addNewRhythmsFromRhythmProviders() {0}", errReport.primaryErrorMessage);
            }

        }

        return n;
    }

    private void fireChanged(ChangeEvent e)
    {
        LOGGER.fine("fireChanged()");
        for (ChangeListener l : listeners)
        {
            l.stateChanged(e);
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


    private String getPrefString(TimeSignature ts)
    {
        return PREF_DEFAULT_RHYTHM + "__" + ts.name();
    }


}

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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.ChangeListener;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.rhythm.api.AdaptedRhythm;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.spi.RhythmProvider;
import org.jjazz.rhythm.spi.StubRhythmProvider;
import org.jjazz.rhythmdatabase.spi.RhythmDatabaseFactory;
import org.openide.util.Lookup;

/**
 * A RhythmDatabase is a collection of rhythms.
 * <p>
 * RhythmInfo instances are used to describe the available rhythms. They can be serialized by a Rhythmdatabase implementation in a cache
 * file in order to avoid requesting all the Rhythm instances upon each startup -this is very time consuming when hundreds of rhythm files
 * are used. Use getRhythmInstance(RhythmInfo) to get the Rhythm instance from a RhythmInfo instance.
 */
public interface RhythmDatabase
{

    /**
     * Associate a Rhythm instance to a RhythmProvider.
     */
    record RpRhythmPair(RhythmProvider rp, Rhythm r)
        {

        public RpRhythmPair 
        {
            Objects.requireNonNull(r);
            Objects.requireNonNull(rp);
        }
    }
    static final Logger LOGGER = Logger.getLogger(RhythmDatabase.class.getSimpleName());

    /**
     * Helper method which delegates to RhythmDatabaseFactory.getDefault().get().
     *
     * @return
     */
    public static RhythmDatabase getDefault()
    {
        return RhythmDatabaseFactory.getDefault().get();
    }

    /**
     * Get a rhythm instance from its id.
     * <p>
     * If rhythmId contains 2 instances of the AdaptedRhythm.RHYTHM_ID_DELIMITER, then this id represents an AdaptedRhythm which is created
     * on demand, see AdaptedRhythm.getUniqueId(). The rhythm provider, the original rhythm and the time signature are obtained from
     * rhythmId, and the returned rhythm instance is obtained by calling RhythmProvider.getAdaptedRhythmInstance(Rhythm, TimeSignature).
     * Rhythm instances are cached.
     *
     * @param rId A unique id
     * @return The rhythm whose uniqueSerialId matches the specified id
     * @throws org.jjazz.rhythmdatabase.api.UnavailableRhythmException
     */
    default Rhythm getRhythmInstance(String rId) throws UnavailableRhythmException
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
                }
                catch (ParseException ex)
                {
                    LOGGER.log(Level.WARNING, "getRhythmInstance() Invalid time signature in AdaptedRhythm rId={0}", rId);
                    throw new UnavailableRhythmException("Invalid time signature in adapted rhythm id=" + rId);
                }

                Rhythm rOriginal = getRhythmInstance(rIdOriginal);      // Possible UnavailableRhythmException exception here                   
                r = getAdaptedRhythmInstance(rOriginal, newTs);         // Can be null

            }
        }
        else
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

    /**
     * Get a rhythm instance from a RhythmInfo.
     * <p>
     * This might result in a lengthy operation (e.g. because of file reading). Rhythm instances are cached.
     *
     * @param rhythmInfo
     * @return
     * @throws org.jjazz.rhythmdatabase.api.UnavailableRhythmException
     */
    Rhythm getRhythmInstance(RhythmInfo rhythmInfo) throws UnavailableRhythmException;

    /**
     * Get the RhythmInfo instance from the specified rhythm unique id.
     * <p>
     * RhyhmtInfo instances are cached.
     *
     * @param rhythmId
     * @return Can be null if not found.
     */
    RhythmInfo getRhythm(String rhythmId);

    /**
     * Try to provide the rhythm instance which is an adapted version of r for a different time signature.
     * <p>
     * If the adapted rhythm could be obtained it is added in the database. AdaptedRhythm instances are cached.
     *
     * @param r
     * @param ts
     * @return Can be null if no adapted rhythm is available.
     * @throws IllegalArgumentException If ts is the time signature of r, or if r is not a rhythm of this database.
     */
    AdaptedRhythm getAdaptedRhythmInstance(Rhythm r, TimeSignature ts);

    /**
     * Get the rhythms which are tested OK.
     *
     * @param tester
     * @return
     */
    List<RhythmInfo> getRhythms(Predicate<RhythmInfo> tester);

    /**
     * Get the rhythms which match the specified time signature.
     *
     * @param ts TimeSignature
     * @return All rhythms corresponding to TimeSignature ts.
     */
    default List<RhythmInfo> getRhythms(TimeSignature ts)
    {
        Objects.requireNonNull(ts);
        return getRhythms(ri -> ri.timeSignature().equals(ts));
    }

    /**
     * The rhythms associated to the specified RhythmProvider
     *
     * @param rp
     * @return
     * @exception IllegalArgumentException If rp is not a RhythmProvider available.
     */
    List<RhythmInfo> getRhythms(RhythmProvider rp);

    /**
     * Try to find a rhythm in the database which is "similar" to the specified rhythm info.
     * <p>
     * "Similar" means at least share the same time signature. The implementation could for example use RhythmFeatures.getMatchingScore() to
     * help identify the most similar rhythm.
     *
     * @param ri
     * @return A "similar" rhythm which at least share the same timesignature. Null if nothing relevant found.
     */
    default RhythmInfo getSimilarRhythm(RhythmInfo ri)
    {
        int max = -1;
        RhythmInfo res = null;
        for (RhythmInfo rii : getRhythms())
        {
            if (!rii.timeSignature().equals(ri.timeSignature()) || rii == ri)
            {
                continue;
            }
            int score = ri.rhythmFeatures().getMatchingScore(rii.rhythmFeatures());
            if (score > max)
            {
                max = score;
                res = rii;
            }
        }

        return res;
    }

    /**
     * @return All rhythms stored in the database.
     */
    default List<RhythmInfo> getRhythms()
    {
        return getRhythms(r -> true);
    }

    /**
     * @param rhythm
     * @return The RhythmProvider of the specified rhythm. Null if not found.
     */
    RhythmProvider getRhythmProvider(Rhythm rhythm);

    /**
     * Get the RhythmProvider for specified RhythmInfo.
     *
     * @param ri
     * @return The RhythmProvider of the specified RhythmInfo. Can be null.
     */
    RhythmProvider getRhythmProvider(RhythmInfo ri);

    /**
     * Get the RhythmProviderId which matchs the specified unique id.
     *
     * @param rpId
     * @return
     */
    default RhythmProvider getRhythmProvider(String rpId)
    {
        return getRhythmProviders()
            .stream()
            .filter(rp -> rp.getInfo().getUniqueId().equals(rpId))
            .findAny()
            .orElse(null);
    }

    /**
     * The RhythmProviders instances currently available, sorted by name.
     *
     * @return
     */
    default List<RhythmProvider> getRhythmProviders()
    {
        List<RhythmProvider> res = new ArrayList<>(Lookup.getDefault().lookupAll(RhythmProvider.class));
        res.sort((rp1, rp2) -> rp1.getInfo().getName().compareTo(rp2.getInfo().getName()));
        return res;
    }

    /**
     * @return The list of TimeSignature for which we have at least 1 rhythm in the database
     */
    List<TimeSignature> getTimeSignatures();

    /**
     * Get the default Rhythm for TimeSignature ts.
     *
     * @param ts TimeSignature
     * @return Can not be null, but there is no guarantee that getRhythmInstance() on the returned value will work (e.g. if this RhythmInfo
     * depends on a file which is no more available).
     */
    RhythmInfo getDefaultRhythm(TimeSignature ts);


    /**
     * Get the default stub rhythm for the specified TimeSignature.
     *
     * @param ts
     * @return Can't be null.
     */
    default Rhythm getDefaultStubRhythmInstance(TimeSignature ts)
    {
        var srp = StubRhythmProvider.getDefault();
        return srp.getStubRhythm(ts);
    }


    /**
     * Set the default rhythm for this TimeSignature.
     *
     * @param ts TimeSignature
     * @param ri
     * @exception IllegalArgumentException If rhythm is not part of this database or if ri is an AdaptedRhythm
     */
    void setDefaultRhythm(TimeSignature ts, RhythmInfo ri);

    /**
     * @return The number of rhythms in the database.
     */
    int size();

    /**
     * Force a rescan of all the RhythmProviders available in the lookup to add rhythms in the database.
     * <p>
     * Rescan is programmed to be performed at next application startup. It might be done immediatly if the immediate parameter is true and
     * if the implementation supports it.
     * <p>
     * Note: once added in the database, a RhythmProvider and its Rhythms can't be removed (until program restarts).<br>
     * Fire a change event if database has changed after the forceRescanUponStartup.
     *
     * @param immediate If true try to rescan immediatly (without waiting for a restart).
     */
    void forceRescan(boolean immediate);

    /**
     * Add extra rhythms to the database.
     * <p>
     * Add new rhythms to a populated database. Fire a change event after rhythms have been added.
     *
     * @param pairs
     * @return The nb of rhythms actually added.
     */
    int addExtraRhythms(List<RpRhythmPair> pairs);

    /**
     * Listeners are notified when the rhythm database has changed.
     * <p>
     * Note that listeners might be notified out of the Event Dispatch Thread.
     *
     * @param l
     */
    void addChangeListener(ChangeListener l);

    void removeChangeListener(ChangeListener l);


}

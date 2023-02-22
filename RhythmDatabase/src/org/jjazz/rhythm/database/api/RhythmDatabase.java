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
package org.jjazz.rhythm.database.api;

import java.util.List;
import java.util.function.Predicate;
import javax.swing.event.ChangeListener;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.rhythm.api.AdaptedRhythm;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.spi.RhythmProvider;
import org.openide.util.Lookup;
import org.jjazz.rhythm.database.RhythmDatabaseImpl;

/**
 * The RhythmDatabase is the central place to get information about installed rhythms and get Rhythm instances.
 * <p>
 * Implementation should populate the database upon creation.
 * <p>
 * RhythmInfo instances are used to describe the available rhythms. They can be serialized by a Rhythmdatabase implementation in a
 * cache file to avoid requesting all the Rhythm instances upon each startup -this is very time consuming when hundreds of rhythm
 * files are used. Use getRhythmInstance(RhythmInfo) to get the Rhythm instance.
 */
public interface RhythmDatabase
{

    class RpRhythmPair
    {

        public RhythmProvider rp;
        public Rhythm r;

        public RpRhythmPair(RhythmProvider rp, Rhythm r)
        {
            if (rp == null || r == null)
            {
                throw new IllegalArgumentException("rp=" + rp + " r=" + r);   
            }
            this.rp = rp;
            this.r = r;
        }
    }

    /**
     * Use the first implementation present in the global lookup.
     * <p>
     * If nothing found, use the default one.
     *
     * @return
     */
    public static RhythmDatabase getDefault()
    {
        RhythmDatabase result = Lookup.getDefault().lookup(RhythmDatabase.class);
        if (result == null)
        {
            return RhythmDatabaseImpl.getInstance();
        }
        return result;
    }

    /**
     * Get a special RhythmDatabase instance for unit tests.
     *
     * @return
     */
    public static RhythmDatabase getUnitTestDefault()
    {
        return RhythmDatabaseImpl.getUnitTestInstance();
    }

    /**
     * Get a rhythm instance from its id.
     * <p>
     * If rhythmId contains 2 instances of the AdaptedRhythm.RHYTHM_ID_DELIMITER, then this id represents an AdaptedRhythm which
     * is created on demand, see AdaptedRhythm.getUniqueId(). The rhythm provider, the original rhythm and the time signature are
     * obtained from rhythmId, and the returned rhythm instance is obtained by calling
     * RhythmProvider.getAdaptedRhythmInstance(Rhythm, TimeSignature). Rhythm instances are cached.
     *
     * @param rhythmId A unique id
     * @return The rhythm whose uniqueSerialId matches the specified id
     * @throws org.jjazz.rhythm.database.api.UnavailableRhythmException
     */
    Rhythm getRhythmInstance(String rhythmId) throws UnavailableRhythmException;

    /**
     * Get a rhythm instance from a RhythmInfo.
     * <p>
     * This might result in a lengthy operation (e.g. because of file reading). Rhythm instances are cached.
     *
     * @param rhythmInfo
     * @return
     * @throws org.jjazz.rhythm.database.api.UnavailableRhythmException
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
    List<RhythmInfo> getRhythms(TimeSignature ts);

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
     * "Similar" means at least share the same time signature. The implementation could for example use
     * RhythmFeatures.getMatchingScore() to help identify the most similar rhythm.
     *
     * @param ri
     * @return A "similar" rhythm which at least share the same timesignature. Null if nothing relevant found.
     */
    RhythmInfo getSimilarRhythm(RhythmInfo ri);

    /**
     * @return All rhythms stored in the database.
     */
    List<RhythmInfo> getRhythms();

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
    RhythmProvider getRhythmProvider(String rpId);

    /**
     * The RhythmProviders instances currently available, sorted by name.
     *
     * @return
     */
    List<RhythmProvider> getRhythmProviders();

    /**
     * @return The list of TimeSignature for which we have at least 1 rhythm in the database
     */
    List<TimeSignature> getTimeSignatures();

    /**
     * Get the default Rhythm for TimeSignature ts.
     *
     * @param ts TimeSignature
     * @return Can not be null, but there is no guarantee that getRhythmInstance() on the returned value will work (e.g. if this
     * RhythmInfo depends on a file which is no more available).
     */
    RhythmInfo getDefaultRhythm(TimeSignature ts);

    /**
     * Get the default stub rhythm for the specified TimeSignature.
     *
     * @param ts
     * @return Can't be null.
     */
    Rhythm getDefaultStubRhythmInstance(TimeSignature ts);

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
     * Rescan is programmed to be performed at next application startup. It might be done immediatly if the immediate parameter is
     * true and if the implementation supports it.
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

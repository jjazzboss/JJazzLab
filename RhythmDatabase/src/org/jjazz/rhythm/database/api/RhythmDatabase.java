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
import org.jjazz.harmony.TimeSignature;
import org.jjazz.rhythm.api.AdaptedRhythm;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.spi.RhythmProvider;
import org.openide.util.Lookup;
import org.jjazz.rhythm.database.RhythmDatabaseImpl;

/**
 * Operations to get information and retrieve installed rhythms.
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
     * Listeners are notified when the rhythm database has changed.
     * <p>
     * Note that listeners might be notified out of the Event Dispatch Thread.
     *
     * @param l
     */
    void addChangeListener(ChangeListener l);

    void removeChangeListener(ChangeListener l);

    /**
     * Get the default Rhythm for TimeSignature ts.
     *
     * @param ts TimeSignature
     * @return Can not be null: the database should provide at least a stub rhythm.
     */
    Rhythm getDefaultRhythm(TimeSignature ts);

    /**
     * Set the default rhythm for this TimeSignature.
     *
     * @param ts TimeSignature
     * @param rhythm
     * @exception IllegalArgumentException If rhythm is not part of this database.
     */
    void setDefaultRhythm(TimeSignature ts, Rhythm rhythm);


    /**
     * Try to provide a new rhythm which is an adapted version of r for a different time signature.
     * <p>
     * If the adapted rhythm could be obtained it is added in the database.
     *
     * @param r
     * @param ts
     * @return Can be null if no adapted rhythm is available.
     * @throws IllegalArgumentException If ts is the time signature of r, or if r is not a rhythm of this database.
     */
    AdaptedRhythm getAdaptedRhythm(Rhythm r, TimeSignature ts);

    /**
     * Get the rhythms which are tested OK.
     *
     * @param tester
     * @return
     */
    List<Rhythm> getRhythms(Predicate<Rhythm> tester);

    /**
     * Get the rhythms which match the specified time signature.
     *
     * @param ts TimeSignature
     * @return All rhythms corresponding to TimeSignature ts.
     */
    List<Rhythm> getRhythms(TimeSignature ts);

    /**
     * The rhythms associated to the specified RhythmProvider
     *
     * @param rp
     * @return
     * @exception IllegalArgumentException If rp is not a RhythmProvider available.
     */
    List<Rhythm> getRhythms(RhythmProvider rp);

    /**
     * Get a rhythm from its id.
     * <p>
     * If rhythmId contains 2 instances of the AdaptedRhythm.RHYTHM_ID_DELIMITER, then this id represents an AdaptedRhythm which
     * is created on demand, see AdaptedRhythm.getUniqueId(). The rhythm provider, the original rhythm and the time signature are
     * obtained from rhythmId, and the returned rhythm instance is obtained by calling RhythmProvider.getAdaptedRhythm(Rhythm,
     * TimeSignature).
     *
     * @param rhythmId A unique id
     * @return The rhythm whose uniqueSerialId matches the specified id. Null if not found.
     */
    Rhythm getRhythm(String rhythmId);

    /**
     * Try to find a rhythm in the database which is "similar" to the specified rhythm info.
     * <p>
     * "similar" means at least share the same time signature. Then algorithm can use other Info fields (temporange, tags, ...) to
     * calculate how "similar" we are.
     *
     * @param rhythm
     * @return A "similar" rhythm which at least share the same timesignature. Null if nothing relevant found.
     */
    Rhythm getSimilarRhythm(Rhythm rhythm);

    /**
     * @return All rhythms stored in the database.
     */
    List<Rhythm> getRhythms();

    /**
     * @param rhythm
     * @return The RhythmProvider of the specified rhythm. Null if not found.
     */
    RhythmProvider getRhythmProvider(Rhythm rhythm);

    /**
     * The RhythmProviders instances available, sorted by name.
     *
     * @return
     */
    List<RhythmProvider> getRhythmProviders();

    /**
     * @return The list of TimeSignature for which we have at least 1 rhythm in the database
     */
    List<TimeSignature> getTimeSignatures();


    /**
     * @return The number of rhythms in the database.
     */
    int size();

    /**
     * Scan all the RhythmProviders available in the lookup to add rhythms in the database.
     * <p>
     * Note: once added in the database, a RhythmProvider and its Rhythms can't be removed (until program restarts).<br>
     * Fire a change event if database has changed after the refresh.
     *
     * @param forceRescan If true force a complete rescan for each RhythmProvider. If false RhythmProviders are provided with the
     * previous list so they can only update possible added rhythms.
     */
    void refresh(boolean forceRescan);

    /**
     * Add some rhythms to the database (if not already present).
     * <p>
     * Fire a change event after rhythms have been added.
     *
     * @param pairs
     * @return The nb of rhythms actually added.
     */
    int addRhythms(List<RpRhythmPair> pairs);

}

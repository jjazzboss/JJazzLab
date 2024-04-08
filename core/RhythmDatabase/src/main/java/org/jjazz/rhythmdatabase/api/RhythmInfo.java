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

import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.midi.api.synths.GM1Instrument;
import org.jjazz.rhythm.api.AdaptedRhythm;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmFeatures;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.spi.RhythmProvider;

/**
 * A rhythm descriptor for catalog purposes.
 */
public record RhythmInfo(String rhythmProviderId,
        String rhythmUniqueId,
        boolean isAdaptedRhythm,
        File file,
        String name,
        String[] tags,
        String description,
        String version,
        String author,
        TimeSignature timeSignature,
        int preferredTempo,
        RhythmFeatures rhythmFeatures,
        List<RvInfo> rvInfos,
        List<RpInfo> rpInfos) implements Serializable
        {


    /**
     * A RhythmVoice descriptor.
     *
     * @param gmSubstitute Can be null
     * @param drumKit      Can be null
     */
    public record RvInfo(String name, GM1Instrument gmSubstitute, int preferredChannel, DrumKit drumKit, RhythmVoice.Type type) implements Serializable
            {

        public RvInfo(RhythmVoice rv)
        {
            this(rv.getName(), rv.getPreferredInstrument().getSubstitute(), rv.getPreferredChannel(), rv.getDrumKit(), rv.getType());
        }
    }

    /**
     * A RhyhtmParameter descriptor.
     */
    public record RpInfo(String displayName, String description, String className) implements Serializable
            {

        public RpInfo(RhythmParameter<?> rp)
        {
            this(rp.getDisplayName(), rp.getDescription(), rp.getClass().getName());
        }
    }

    private static final Logger LOGGER = Logger.getLogger(RhythmInfo.class.getSimpleName());

    public RhythmInfo(Rhythm r, RhythmProvider rp)
    {
        this(rp.getInfo().getUniqueId(),
                r.getUniqueId(),
                r instanceof AdaptedRhythm,
                r.getFile(),
                r.getName(),
                r.getTags(),
                r.getDescription(),
                r.getVersion(),
                r.getAuthor(),
                r.getTimeSignature(),
                r.getPreferredTempo(),
                r.getFeatures(),
                r.getRhythmVoices().stream()
                        .map(rv -> new RvInfo(rv))
                        .toList(),
                r.getRhythmParameters().stream()
                        .map(rv -> new RpInfo(rv))
                        .toList()
        );
    }

    /**
     * Check that this RhythmInfo object matches data from specified rhythm.
     * <p>
     * Test only the main fields.
     *
     * @param r
     * @return False if inconsistency detected (see log file for details).
     */
    public boolean checkConsistency(RhythmProvider rp, Rhythm r)
    {
        boolean b = true;
        if (!rhythmUniqueId.equals(r.getUniqueId()))
        {
            LOGGER.log(Level.WARNING, "checkConsistency() r={0}: uniqueId mismatch. rhythmUniqueId={1} r.getUniqueId()={2}", new Object[]
            {
                r,
                rhythmUniqueId, r.getUniqueId()
            });
            b = false;
        }
        if (!rhythmProviderId.equals(rp.getInfo().getUniqueId()))
        {
            LOGGER.log(Level.WARNING, "checkConsistency() r={0}: rhythmProviderId mismatch. rhythmProviderId={1} rdb.rp.uniqueId={2}", new Object[]
            {
                r,
                rhythmProviderId, RhythmDatabase.getDefault().getRhythmProvider(r).getInfo().getUniqueId()
            });
            b = false;
        }
        if (!name.equals(r.getName()))
        {
            LOGGER.log(Level.WARNING, "checkConsistency() r={0}: name mismatch. name={1} r.getName()={2}", new Object[]
            {
                r, name, r.getName()
            });
            b = false;
        }
        if (!file.equals(r.getFile()))
        {
            LOGGER.log(Level.WARNING, "checkConsistency() r={0}: file mismatch. file={1} r.getFile()={2}", new Object[]
            {
                r,
                file.getAbsolutePath(), r.getFile().getAbsolutePath()
            });
            b = false;
        }
        if (!timeSignature.equals(r.getTimeSignature()))
        {
            LOGGER.log(Level.WARNING, "checkConsistency() r={0}: timeSignature mismatch. timeSignature={1} r.getTimeSignature()={2}", new Object[]
            {
                r,
                timeSignature, r.getTimeSignature()
            });
            b = false;
        }

        return b;
    }

    @Override
    public String toString()
    {
        return "Rinfo[" + name() + "]";
    }
}

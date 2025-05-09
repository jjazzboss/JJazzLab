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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
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
 * A descriptor for a rhythm.
 * <p>
 * Note: tried to make this class a record, but then a strange bug occured when deserializing the object (I guess linked to the List&lt;&gt; parameters),
 * finally back to a standard class.
 */
public class RhythmInfo implements Serializable
{

    private static final long serialVersionUID = 87291200331L;

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


    private String rhythmProviderId;
    private String rhythmUniqueId;
    private File file;
    private String name;
    private String[] tags;
    private String description;
    private String version;
    private String author;
    private TimeSignature timeSignature;
    private int preferredTempo;
    private RhythmFeatures rhythmFeatures;
    private boolean isAdaptedRhythm;
    private final List<RvInfo> cacheRvs = new ArrayList<>();
    private final List<RpInfo> cacheRps = new ArrayList<>();
    private static final Logger LOGGER = Logger.getLogger(RhythmInfo.class.getSimpleName());

    /**
     * Constructs a RhythmInfo from an existing rhythm.
     *
     * @param rhythm
     * @param rhythmProvider
     */
    public RhythmInfo(Rhythm rhythm, RhythmProvider rhythmProvider)
    {
        Objects.requireNonNull(rhythm);
        Objects.requireNonNull(rhythmProvider);

        this.rhythmProviderId = rhythmProvider.getInfo().getUniqueId();
        this.rhythmUniqueId = rhythm.getUniqueId();
        this.isAdaptedRhythm = rhythm instanceof AdaptedRhythm;
        this.file = rhythm.getFile();
        this.name = rhythm.getName();
        this.tags = rhythm.getTags();
        this.description = rhythm.getDescription();
        this.version = rhythm.getVersion();
        this.author = rhythm.getAuthor();
        this.preferredTempo = rhythm.getPreferredTempo();
        this.timeSignature = rhythm.getTimeSignature();
        this.rhythmFeatures = rhythm.getFeatures();
        for (RhythmVoice rv : rhythm.getRhythmVoices())
        {
            cacheRvs.add(new RvInfo(rv));
        }
        for (RhythmParameter<?> rp : rhythm.getRhythmParameters())
        {
            cacheRps.add(new RpInfo(rp));
        }
    }

    /**
     * Check that this RhythmInfo object matches data from specified rhythm.
     * <p>
     * Test only the main fields.
     *
     * @param rp
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

    public List<RvInfo> rvInfos()
    {
        return new ArrayList<>(cacheRvs);
    }

    public List<RpInfo> rpInfos()
    {
        return new ArrayList<>(cacheRps);
    }

    public RhythmFeatures rhythmFeatures()
    {
        return rhythmFeatures;
    }

    public File file()
    {
        return file;
    }

    public String rhythmUniqueId()
    {
        return this.rhythmUniqueId;
    }

    public String rhythmProviderId()
    {
        return rhythmProviderId;
    }

    public String description()
    {
        return description;
    }

    public int preferredTempo()
    {
        return preferredTempo;
    }

    public TimeSignature timeSignature()
    {
        return timeSignature;
    }

    public String name()
    {
        return name;
    }

    public String author()
    {
        return author;
    }

    public String version()
    {
        return version;
    }

    public String[] tags()
    {
        return tags;
    }


    public boolean isAdaptedRhythm()
    {
        return isAdaptedRhythm;
    }

    @Override
    public String toString()
    {
        return "Rinfo[" + name() + "-" + timeSignature() + "]";
    }


    /**
     * Does not rely on file.
     *
     * @return
     */
    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 83 * hash + Objects.hashCode(this.rhythmProviderId);
        hash = 83 * hash + Objects.hashCode(this.rhythmUniqueId);
        // hash = 83 * hash + Objects.hashCode(this.file);               // Fix issue #548
        hash = 83 * hash + Objects.hashCode(this.name);
        hash = 83 * hash + Arrays.deepHashCode(this.tags);
        hash = 83 * hash + Objects.hashCode(this.description);
        hash = 83 * hash + Objects.hashCode(this.version);
        hash = 83 * hash + Objects.hashCode(this.author);
        hash = 83 * hash + Objects.hashCode(this.timeSignature);
        hash = 83 * hash + this.preferredTempo;
        hash = 83 * hash + Objects.hashCode(this.rhythmFeatures);
        hash = 83 * hash + (this.isAdaptedRhythm ? 1 : 0);
        hash = 83 * hash + Objects.hashCode(this.cacheRvs);
        hash = 83 * hash + Objects.hashCode(this.cacheRps);
        return hash;
    }

    /**
     * Does not rely on file.
     *
     * @return
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final RhythmInfo other = (RhythmInfo) obj;
        if (this.preferredTempo != other.preferredTempo)
        {
            return false;
        }
        if (this.isAdaptedRhythm != other.isAdaptedRhythm)
        {
            return false;
        }
        if (!Objects.equals(this.rhythmProviderId, other.rhythmProviderId))
        {
            return false;
        }
        if (!Objects.equals(this.rhythmUniqueId, other.rhythmUniqueId))
        {
            return false;
        }
        if (!Objects.equals(this.name, other.name))
        {
            return false;
        }
        if (!Objects.equals(this.description, other.description))
        {
            return false;
        }
        if (!Objects.equals(this.version, other.version))
        {
            return false;
        }
        if (!Objects.equals(this.author, other.author))
        {
            return false;
        }
//        if (!Objects.equals(this.file, other.file))           // Fix issue #548
//        {
//            return false;
//        }
        if (!Arrays.deepEquals(this.tags, other.tags))
        {
            return false;
        }
        if (this.timeSignature != other.timeSignature)
        {
            return false;
        }
        if (!Objects.equals(this.rhythmFeatures, other.rhythmFeatures))
        {
            return false;
        }
        if (!Objects.equals(this.cacheRvs, other.cacheRvs))
        {
            return false;
        }
        if (!Objects.equals(this.cacheRps, other.cacheRps))
        {
            return false;
        }
        return true;
    }

    // ===========================================================================================
    // Private methods
    // ===========================================================================================

}

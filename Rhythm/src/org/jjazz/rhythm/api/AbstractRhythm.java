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
package org.jjazz.rhythm.api;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import org.jjazz.harmony.TimeSignature;
import org.jjazz.rhythm.parameters.RhythmParameter;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

/**
 * A convenience class to create rhythm.
 * <p>
 * <p>
 */
public class AbstractRhythm implements Rhythm
{

    protected String uniqueId;
    protected String name;
    protected String description;
    protected String author;
    protected String version;
    protected int preferredTempo;
    protected TimeSignature timeSignature;
    protected TempoRange tempoRange;
    protected Feel feel;
    protected String[] tags;
    protected File file;
    protected Lookup lookup;
    /**
     * The default RhythmParameters associated to this rhythm.
     */
    protected ArrayList<RhythmParameter<?>> rhythmParameters = new ArrayList<>();
    /**
     * The supported RhythmVoices.
     */
    protected ArrayList<RhythmVoice> rhythmVoices = new ArrayList<>();
    private static final Logger LOGGER = Logger.getLogger(AbstractRhythm.class.getSimpleName());

    public AbstractRhythm(String uniqueId, String name, String description, String author, String version, Feel feel, TimeSignature ts, int preferredTempo, TempoRange tempoRange, File f, String... tags)
    {
        if (uniqueId == null || uniqueId.trim().isEmpty() || name == null || feel == null || name.trim().isEmpty() || preferredTempo < 1
                || ts == null || tempoRange == null || description == null || author == null || version == null)
        {
            throw new IllegalArgumentException("name=" + name + ", description=" + description + ", feel=" + feel + ", timeSignature="
                    + timeSignature + ", tempoRange=" + tempoRange + ", author=" + author + ", version=" + version + " file=" + file);
        }
        this.uniqueId = uniqueId;
        this.name = name.trim();
        this.description = description;
        this.author = author;
        this.version = version;
        this.feel = feel;
        this.timeSignature = ts;
        this.preferredTempo = preferredTempo;
        this.tempoRange = new TempoRange(tempoRange);
        this.tags = tags != null ? tags : new String[0];
        this.file = f;
        if (file == null)
        {
            // Force an empty path
            file = new File("");
        }
        lookup = Lookups.fixed("dummy");
    }

    /**
     * Create a dummy rhythm in 4/4, binary, 120.
     *
     * @param uniqueId
     * @param name
     */
    public AbstractRhythm(String uniqueId, String name)
    {
        this(uniqueId, name, "desc", "auth", "ver", Feel.BINARY, TimeSignature.FOUR_FOUR, 120, TempoRange.ALL_TEMPO, null, "dummy");
    }

    @Override
    public boolean equals(Object o)
    {
        boolean res = false;
        if (o instanceof AbstractRhythm)
        {
            AbstractRhythm ar = (AbstractRhythm) o;
            res = ar.uniqueId.equals(uniqueId);
        }
        return res;
    }

    @Override
    public int hashCode()
    {
        int hash = 5;
        hash = 47 * hash + Objects.hashCode(this.uniqueId);
        return hash;
    }

    @Override
    public List<RhythmVoice> getRhythmVoices()
    {
        return new ArrayList<>(rhythmVoices);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<RhythmParameter<?>> getRhythmParameters()
    {
        return new ArrayList<>(rhythmParameters);
    }

    /**
     * Should be overridden !
     * <p>
     * This implementation return a useless lookup.
     *
     * @return
     */
    @Override
    public Lookup getLookup()
    {
        return lookup;
    }

    @Override
    public TimeSignature getTimeSignature()
    {
        return timeSignature;
    }

    /**
     * Should be overridden !
     * <p>
     * This implementation does nothing and return true
     */
    @Override
    public boolean loadResources()
    {
        // Do nothing
        return true;
    }

    /**
     * Default implementation returns true.
     *
     * @return
     */
    @Override
    public boolean isResourcesLoaded()
    {
        return true;
    }

    /**
     * Shold be overridden !
     * <p>
     * This implementation does nothing.
     */
    @Override
    public void releaseResources()
    {
        // Do nothing
    }

    @Override
    public int compareTo(Rhythm o)
    {
        return name.compareTo(o.getName());
    }

    @Override
    public File getFile()
    {
        return file;
    }

    @Override
    public String getUniqueId()
    {
        return uniqueId;
    }

    @Override
    public String getDescription()
    {
        return description;
    }

    @Override
    public Feel getFeel()
    {
        return feel;
    }

    @Override
    public TempoRange getTempoRange()
    {
        return tempoRange;
    }

    @Override
    public int getPreferredTempo()
    {
        return preferredTempo;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public String getAuthor()
    {
        return author;
    }

    @Override
    public String getVersion()
    {
        return version;
    }

    @Override
    public String[] getTags()
    {
        return tags;
    }

    @Override
    public String toString()
    {
        return getName();
    }

}

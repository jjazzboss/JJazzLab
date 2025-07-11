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
package org.jjazz.rhythmcombinator.api;

import com.google.common.base.Preconditions;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmFeatures;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythmmusicgeneration.spi.MusicGenerator;
import org.jjazz.songcontext.api.SongContext;

/**
 * A rhythm which is an assembly of tracks from several rhythms.
 */
public class CompositeRhythm implements Rhythm, MusicGenerator
{

    /**
     * Property change event is fired when a RhythmVoice mapping is changed.
     * <p>
     * old=rvSrc, new=rvDest
     */
    private static final String PROP_RHYTHM_VOICE_MAPPING = "PropRvSrcDestMapping";
    private final Rhythm baseRhythm;
    private final Map<RhythmVoice, RhythmVoice> mapSrcDestRhythmVoices;
    private final transient PropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(CompositeRhythm.class.getSimpleName());

    public CompositeRhythm(Rhythm baseRhythm)
    {
        Objects.requireNonNull(baseRhythm);
        this.mapSrcDestRhythmVoices = new HashMap<>();
        this.baseRhythm = baseRhythm;
    }

    public Rhythm getBaseRhythm()
    {
        return baseRhythm;
    }

    /**
     * Map a source RhythmVoice to a destination RhythmVoice from another rhythm.
     * <p>
     * Fire a PROP_RHYTHM_VOICE_MAPPING change event.
     *
     * @param rvSrc
     * @param rvDest If null mapping is removed
     */
    public void setDestRhythmVoice(RhythmVoice rvSrc, RhythmVoice rvDest)
    {
        Objects.requireNonNull(rvSrc);
        Preconditions.checkArgument(baseRhythm.getRhythmVoices().contains(rvSrc), "rvSrc=%s", rvSrc);
        Preconditions.checkArgument(rvDest == null || (rvDest.getContainer() != null && rvDest.getContainer() != baseRhythm), "rvDest=%s", rvDest);
        if (rvDest != null)
        {
            mapSrcDestRhythmVoices.put(rvSrc, rvDest);
        } else
        {
            mapSrcDestRhythmVoices.remove(rvSrc);
        }
        pcs.firePropertyChange(PROP_RHYTHM_VOICE_MAPPING, rvSrc, rvDest);
    }

    /**
     * Get the mapped RhythmVoice of a source RhythmVoice.
     *
     * @param rvSrc
     * @return Can be null
     */
    public RhythmVoice getDestRhythmVoice(RhythmVoice rvSrc)
    {
        Objects.requireNonNull(rvSrc);
        Preconditions.checkArgument(baseRhythm.getRhythmVoices().contains(rvSrc), "rvSrc=%s", rvSrc);
        var res = mapSrcDestRhythmVoices.get(rvSrc);
        return res;
    }

    /**
     * Get all the source RhythmVoices mapped to a destination RhythmVoice from another rhythm.
     *
     * @return
     */
    public Set<RhythmVoice> getMappedSrcRhythmVoices()
    {
        return Collections.unmodifiableSet(mapSrcDestRhythmVoices.keySet());
    }

    // ===============================================================================================================
    // MusicGenerator interface
    // ===============================================================================================================   
    @Override
    public Map<RhythmVoice, Phrase> generateMusic(SongContext context, RhythmVoice... rhythmVoices) throws MusicGenerationException
    {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    // ===============================================================================================================
    // Rhythm interface
    // ===============================================================================================================    
    @Override
    public RhythmFeatures getFeatures()
    {
        return baseRhythm.getFeatures();
    }

    @Override
    public void loadResources() throws MusicGenerationException
    {
        LOGGER.severe("loadResources() DEBUG TODO load other rhythms ressources too");
        baseRhythm.loadResources();
        pcs.firePropertyChange(PROP_RESOURCES_LOADED, false, true);
    }

    @Override
    public void releaseResources()
    {
        LOGGER.severe("releaseResources() DEBUG TODO release other rhythms ressources too");
        baseRhythm.releaseResources();
        pcs.firePropertyChange(PROP_RESOURCES_LOADED, true, false);
    }

    @Override
    public boolean isResourcesLoaded()
    {
        return baseRhythm.isResourcesLoaded();
    }

    @Override
    public List<RhythmVoice> getRhythmVoices()
    {
        return baseRhythm.getRhythmVoices();
    }

    @Override
    public List<RhythmParameter<?>> getRhythmParameters()
    {
        return baseRhythm.getRhythmParameters();
    }

    @Override
    public File getFile()
    {
        return new File("");
    }

    @Override
    public String getUniqueId()
    {
        return baseRhythm.getUniqueId() + "-composite";
    }

    @Override
    public String getDescription()
    {
        return "Composite rhythm based on " + baseRhythm.getDescription();
    }

    @Override
    public int getPreferredTempo()
    {
        return baseRhythm.getPreferredTempo();
    }

    @Override
    public TimeSignature getTimeSignature()
    {
        return baseRhythm.getTimeSignature();
    }

    @Override
    public String getName()
    {
        return baseRhythm.getName() + "-composite";
    }

    @Override
    public String getAuthor()
    {
        return baseRhythm.getAuthor() + "-composite";
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        baseRhythm.addPropertyChangeListener(l);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        baseRhythm.removePropertyChangeListener(l);
    }


}

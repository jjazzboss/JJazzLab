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
package org.jjazz.yamjjazz.rhythm.api;

import org.jjazz.rhythmmusicgeneration.api.CompositeMusicGenerator;
import com.google.common.base.Preconditions;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.phrasetransform.api.rps.RP_SYS_DrumsTransform;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmFeatures;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_CustomPhrase;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Fill;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Intensity;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Marker;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_TempoFactor;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Variation;
import org.jjazz.rhythmmusicgeneration.api.RP_SYS_Mute;
import org.jjazz.rhythmmusicgeneration.spi.MusicGenerator;

/**
 * A YamJJazzRhythm whose tracks can be replaced by tracks from other rhythms (or possibly from base rhythm).
 *
 * @see CompositeMusicGenerator
 */
public class CompositeRhythm implements YamJJazzRhythm
{

    /**
     * Property change event is fired when a RhythmVoice mapping is changed.
     * <p>
     * old=rvSrc, new=rvDest
     */
    private static final String PROP_RHYTHM_VOICE_MAPPING = "PropRvSrcDestMapping";
    private final YamJJazzRhythm baseRhythm;
    private final Map<RhythmVoice, RhythmVoice> mapSrcDestRhythmVoices;
    private MusicGenerator musicGenerator;
    private final List<RhythmParameter<?>> rhythmParameters;
    private final List<RhythmVoice> rhythmVoices;
    private final transient PropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(CompositeRhythm.class.getSimpleName());

    public CompositeRhythm(YamJJazzRhythm baseRhythm)
    {
        Objects.requireNonNull(baseRhythm);

        this.baseRhythm = baseRhythm;
        this.rhythmVoices = buildRhythmVoices(baseRhythm);
        this.rhythmParameters = buildRhythmParameters(baseRhythm);


        this.mapSrcDestRhythmVoices = new HashMap<>();
        CompositeMusicGenerator.RvToDelegateUnitMapper rvMapper = (baseRv, spt) -> 
        {
            var rvTarget = mapSrcDestRhythmVoices.get(baseRv);
            MusicGenerator mg;
            if (rvTarget == null)
            {
                rvTarget = baseRv;
                mg = this.baseRhythm.getMusicGenerator();
            } else
            {
                mg = ((YamJJazzRhythm) rvTarget.getContainer()).getMusicGenerator();
            }
            return new CompositeMusicGenerator.DelegateUnit(spt, baseRv, mg, rvTarget, null, null);
        };
        musicGenerator = new CompositeMusicGenerator(this, rvMapper);
    }

    /**
     * The underlying base rhythm of this CompositeRhythm.
     *
     * @return
     */
    public YamJJazzRhythm getBaseRhythm()
    {
        return baseRhythm;
    }

    /**
     * Map a RhythmVoice from this rhythm to another RhythmVoice (from this rhythm or another).
     * <p>
     * Fire a PROP_RHYTHM_VOICE_MAPPING change event.
     *
     * @param rvSrc  From this CompositeRhythm
     * @param rvDest If null mapping is removed. Container must be a YamJJazzRhythm.
     */
    public void setDestRhythmVoice(RhythmVoice rvSrc, RhythmVoice rvDest)
    {
        Objects.requireNonNull(rvSrc);
        Preconditions.checkArgument(rhythmVoices.contains(rvSrc), "rvSrc=%s", rvSrc);
        Preconditions.checkArgument(rvDest == null || rvDest.getContainer() instanceof YamJJazzRhythm, "rvDest=%s", rvDest);
        if (rvDest != null && rvDest != rvSrc)
        {
            mapSrcDestRhythmVoices.put(rvSrc, rvDest);
        } else
        {
            mapSrcDestRhythmVoices.remove(rvSrc);
        }
        pcs.firePropertyChange(PROP_RHYTHM_VOICE_MAPPING, rvSrc, rvDest);
    }

    /**
     * Get the destination RhythmVoice of rvSrc.
     *
     * @param rvSrc From this CompositeRhythm
     * @return Null if not mapped
     */
    public RhythmVoice getDestRhythmVoice(RhythmVoice rvSrc)
    {
        Objects.requireNonNull(rvSrc);
        Preconditions.checkArgument(rhythmVoices.contains(rvSrc), "rvSrc=%s", rvSrc);
        var res = mapSrcDestRhythmVoices.get(rvSrc);
        return res;
    }

    /**
     * Get all the mapped source RhythmVoices.
     *
     * @return
     */
    public Set<RhythmVoice> getMappedSrcRhythmVoices()
    {
        return Collections.unmodifiableSet(mapSrcDestRhythmVoices.keySet());
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
        baseRhythm.loadResources();

        // Load resources from other rhythm instances
        for (var r : getOtherRhythms())
        {
            r.loadResources();
        }

        pcs.firePropertyChange(PROP_RESOURCES_LOADED, false, true);
    }

    @Override
    public void releaseResources()
    {
        baseRhythm.releaseResources();

        // Release resources from other rhythm instances
        for (var r : getOtherRhythms())
        {
            r.releaseResources();
        }

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
        return rhythmVoices;
    }

    @Override
    public List<RhythmParameter<?>> getRhythmParameters()
    {
        return rhythmParameters;
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
        pcs.addPropertyChangeListener(l);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }

    // =============================================================================================
    // YamJJazzRhythm interface
    // =============================================================================================

    @Override
    public AccType getAccType(String rpMuteValue)
    {
        return baseRhythm.getAccType(rpMuteValue);
    }

    @Override
    public int getComplexityLevel(String rpValue)
    {
        return baseRhythm.getComplexityLevel(rpValue);
    }

    @Override
    public Style getStyle()
    {
        return baseRhythm.getStyle();
    }

    @Override
    public StylePart getStylePart(String rpValue)
    {
        return baseRhythm.getStylePart(rpValue);
    }

    @Override
    public boolean isExtendedRhythm()
    {
        return baseRhythm.isExtendedRhythm();
    }

    @Override
    public MusicGenerator getMusicGenerator()
    {
        return musicGenerator;
    }

    @Override
    public void setMusicGenerator(MusicGenerator mg)
    {
        Objects.requireNonNull(mg);
        musicGenerator = mg;
    }

    // =============================================================================================
    // Private methods
    // =============================================================================================
    /**
     * Get the other YamJJazzRhythm instances used by this composite rhythm (besides the base rhythm).
     *
     * @return
     */
    private Set<YamJJazzRhythm> getOtherRhythms()
    {
        Set<YamJJazzRhythm> res = new HashSet<>();
        for (var rv : mapSrcDestRhythmVoices.values())
        {
            YamJJazzRhythm yjr = (YamJJazzRhythm) rv.getContainer();
            if (yjr != baseRhythm)
            {
                res.add(yjr);
            }
        }
        return res;
    }

    /**
     * Build the RhythmVoices for this Rhythm.
     *
     * @param r The base rhythm
     * @return
     */
    private List<RhythmVoice> buildRhythmVoices(Rhythm r)
    {
        Objects.requireNonNull(r);

        LOGGER.log(Level.FINER, "buildRhythmVoices() --  this={0}", this);
        List<RhythmVoice> rvs = r.getRhythmVoices().stream()
                .map(rv -> rv.getCopy(r))
                .toList();      // unmodifiable
        return rvs;
    }


    /**
     * Build the RhythmParameters for this rhythm.
     * <p>
     *
     * @param r The base rhythm
     * @return
     */
    private List<RhythmParameter<?>> buildRhythmParameters(Rhythm r)
    {
        Objects.requireNonNull(r);

        List<RhythmParameter<?>> rps = new ArrayList<>();

        // Reuse variation values from the base rhythm
        var baseRpVariation = RP_SYS_Variation.getVariationRp(r);
        RP_SYS_Variation rpVariation = new RP_SYS_Variation(true, baseRpVariation.getDefaultValue(), baseRpVariation.getPossibleValues().toArray(new String[0]));


        RP_SYS_Intensity rpIntensity = new RP_SYS_Intensity(false);
        RP_SYS_Fill rpFill = new RP_SYS_Fill(true);
        RP_SYS_Mute rpMute = RP_SYS_Mute.createMuteRp(this, false);
        RP_SYS_CustomPhrase rpCustomPhrase = new RP_SYS_CustomPhrase(this, false);


        var rvDrums = rhythmVoices.stream()
                .filter(rv -> rv.getType() == RhythmVoice.Type.DRUMS)
                .findAny()
                .orElseThrow();
        RP_SYS_DrumsTransform rpDrumsTransform = new RP_SYS_DrumsTransform(rvDrums, false);


        rps.add(rpVariation);
        rps.add(rpIntensity);
        rps.add(rpFill);
        rps.add(rpMute);
        rps.add(RP_SYS_Marker.getInstance());
        rps.add(RP_SYS_TempoFactor.getInstance());
        rps.add(rpDrumsTransform);
        rps.add(rpCustomPhrase);


        return Collections.unmodifiableList(rps);
    }


}

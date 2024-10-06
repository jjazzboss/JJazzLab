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
package org.jjazz.test.rhythm;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.sound.midi.MidiUnavailableException;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.midimix.api.DefaultMidiMixManager;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmFeatures;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythmmusicgeneration.spi.MusicGenerator;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongFactory;
import org.jjazz.songcontext.api.SongContext;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.utilities.api.Utilities;
import org.openide.util.Exceptions;

/**
 * A composite Rhythm which combines 2 Rhythms into one.
 */
public class CompositeRhythm implements Rhythm, MusicGenerator
{

    private final TimeSignature timeSignature;
    private final String name;
    private final List<RhythmParameter<?>> rhythmParameters;    // unmodifiable
    private final List<RhythmVoice> rhythmVoices;        // unmodifiable
    private final BiMap<RhythmVoice, RhythmVoice> mapSrcRvRv1;
    private final BiMap<RhythmVoice, RhythmVoice> mapSrcRvRv2;
    private final Rhythm rhythm1, rhythm2;
    private static final Logger LOGGER = Logger.getLogger(CompositeRhythm.class.getSimpleName());

    /**
     * Build the composite Rhythm.
     * <p>
     * Both rhythms must have the same time signature. Total number of voices must be &lt;= 16.
     *
     * @param name
     * @param r1
     * @param rvs1 The voices to be used from r1
     * @param r2
     * @param rvs2 The voices to be used from r2
     */
    public CompositeRhythm(String name, Rhythm r1, List<RhythmVoice> rvs1, Rhythm r2, List<RhythmVoice> rvs2)
    {
        Objects.requireNonNull(r1);
        Objects.requireNonNull(r2);
        Objects.requireNonNull(rvs1);
        Objects.requireNonNull(rvs2);
        Preconditions.checkArgument(name != null && !name.isBlank());
        Preconditions.checkArgument(r1 instanceof MusicGenerator);
        Preconditions.checkArgument(r2 instanceof MusicGenerator);
        Preconditions.checkArgument(r1.getTimeSignature() == r2.getTimeSignature());
        Preconditions.checkArgument(rvs1.size() + rvs2.size() <= 16);

        this.name = name;
        timeSignature = r1.getTimeSignature();
        rhythm1 = r1;
        rhythm2 = r2;


        // Rhythm voices   
        List<RhythmVoice> rvs = new ArrayList<>();
        mapSrcRvRv1 = HashBiMap.create();
        for (var srcRv1 : rvs1)
        {
            var newRv = copyFromSrcRhythmVoice(srcRv1);
            mapSrcRvRv1.put(srcRv1, newRv);
            rvs.add(newRv);
        }
        mapSrcRvRv2 = HashBiMap.create();
        for (var srcRv2 : rvs2)
        {
            var newRv = copyFromSrcRhythmVoice(srcRv2);
            mapSrcRvRv2.put(srcRv2, newRv);
            rvs.add(newRv);
        }
        rhythmVoices = Collections.unmodifiableList(rvs);
        
        LOGGER.log(Level.FINE, "CompositeRhythm() -- rhythm1={0} rhythm2={1} rhythmVoices={2}", new Object[]
        {
            rhythm1, rhythm2, rhythmVoices
        });


        // Rhythm Parameters
        rhythmParameters = (List<RhythmParameter<?>>) findCommonRPs(r1, r2).stream()
                .map(rp -> rp.getCopy(this))
                .toList();

    }


    @Override
    public Map<RhythmVoice, Phrase> generateMusic(SongContext context, RhythmVoice... rvsArray) throws MusicGenerationException
    {
        var rvsList = List.of(rvsArray);
        if (rvsList.isEmpty())
        {
            rvsList = rhythmVoices;
        } else if (!rvsList.stream().anyMatch(rv -> !rhythmVoices.contains(rv)))
        {
            throw new IllegalArgumentException("A specified RhytmVoice is not from this rhythm. rvsArray=" + rvsList);
        }

        Map<RhythmVoice, Phrase> res = new HashMap<>();

        for (Rhythm srcRhythm : List.of(rhythm1, rhythm2))
        {
            LOGGER.log(Level.FINE, "generateMusic() processing source rhythm {0}...", srcRhythm.getName());


            // Transpose to source context
            var srcContext = transposeContextToSrcRhythm(context, srcRhythm);       // throws MusicGenerationException


            // Transpose to source RhythmVoices
            var biMap = rhythm1 == srcRhythm ? mapSrcRvRv1 : mapSrcRvRv2;
            var srcRvs = rvsList.stream()
                    .map(rv -> biMap.inverse().get(rv))
                    .filter(rv -> rv != null)
                    .toList();
            if (srcRvs.isEmpty())
            {
                // rvsArray contained RhythmVoices only for one of the source rhythm
                continue;
            }


            // Generate the music in source context
            var srcRes = ((MusicGenerator) srcRhythm).generateMusic(srcContext, srcRvs.toArray(RhythmVoice[]::new));


            // Transpose back to original RhythmVoices
            for (var srcRv : srcRes.keySet())
            {
                Phrase p = srcRes.get(srcRv);
                var rv = biMap.get(srcRv);
                if (rv != null)
                {
                    res.put(rv, p);
                } else
                {
                    // generateMusic() above generated more tracks than necessary ?
                    LOGGER.log(Level.WARNING, "generateMusic() Unexpected rv=null for srcRv={0}", srcRv);
                }
            }
        }
        return res;
    }

    @Override
    public List<RhythmVoice> getRhythmVoices()
    {
        return rhythmVoices;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<RhythmParameter<?>> getRhythmParameters()
    {
        return rhythmParameters;
    }

    @Override
    public TimeSignature getTimeSignature()
    {
        return timeSignature;
    }

    @Override
    public void loadResources() throws MusicGenerationException
    {
        rhythm1.loadResources();
        rhythm2.loadResources();
    }

    @Override
    public boolean isResourcesLoaded()
    {
        return rhythm1.isResourcesLoaded() && rhythm2.isResourcesLoaded();
    }

    /**
     * This implementation does nothing.
     */
    @Override
    public void releaseResources()
    {
        rhythm1.releaseResources();
        rhythm2.releaseResources();
    }

    @Override
    public int compareTo(Rhythm o
    )
    {
        return getName().compareTo(o.getName());
    }

    @Override
    public File getFile()
    {
        return new File("");
    }

    @Override
    public RhythmFeatures getFeatures()
    {
        return rhythm1.getFeatures();
    }

    @Override
    public String getUniqueId()
    {
        return name + "-ID";
    }

    @Override
    public String getDescription()
    {
        return "CompositeRhythmTest description";
    }

    @Override
    public int getPreferredTempo()
    {
        return rhythm1.getPreferredTempo();
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public String getAuthor()
    {
        return "JL";
    }

    @Override
    public String[] getTags()
    {
        return new String[]
        {
            "composite", rhythm1.getName(), rhythm2.getName()
        };
    }

    @Override
    public String toString()
    {
        return getName() + "[" + rhythm1.getName() + "," + rhythm2.getName() + "]";
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l
    )
    {
        // Nothing
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l
    )
    {
        // Nothing
    }

    // =================================================================================================================
    // Private methods
    // =================================================================================================================

    /**
     * Compute a new context adapted to srcRhythm.
     *
     * @param context
     * @param srcRhythm The rhythm to be used instead of this.
     * @return
     * @throws org.jjazz.rhythm.api.MusicGenerationException Can happen if no more Midi channels available because of a srcRhythm which uses more voices than
     *                                                       this rhythm.
     */
    private SongContext transposeContextToSrcRhythm(SongContext context, Rhythm srcRhythm) throws MusicGenerationException
    {
        Song srcSong = SongFactory.getInstance().getCopy(context.getSong(), false);
        SongStructure srcSgs = srcSong.getSongStructure();


        // Replace this rhythm by srcRhythm
        var oldSpts = srcSgs.getSongParts(spt -> spt.getRhythm() == this);
        var newSpts = oldSpts.stream()
                .map(spt -> spt.clone(srcRhythm, spt.getStartBarIndex(), spt.getNbBars(), spt.getParentSection()))
                .toList();
        try
        {
            srcSgs.replaceSongParts(oldSpts, newSpts);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);  // should never happen since no MidiMix associated yet
        }


        // Create the associated MidiMix
        MidiMix srcMidiMix;
        try
        {
            srcMidiMix = DefaultMidiMixManager.getInstance().createMix(srcSong);
        } catch (MidiUnavailableException ex)
        {
            throw new MusicGenerationException("Could not use CompositeRhythm " + toString() + " : " + ex.getMessage());
        }


        var res = new SongContext(srcSong, srcMidiMix, context.getBarRange());
        return res;
    }

    private RhythmVoice copyFromSrcRhythmVoice(RhythmVoice srcRv)
    {
        RhythmVoice res;
        if (srcRv.isDrums())
        {
            res = new RhythmVoice(srcRv.getDrumKit(),
                    this,
                    srcRv.getType(),
                    srcRv.getName(),
                    srcRv.getPreferredInstrument(),
                    srcRv.getPreferredInstrumentSettings(),
                    srcRv.getPreferredChannel());
        } else
        {
            res = new RhythmVoice(this,
                    srcRv.getType(),
                    srcRv.getName(),
                    srcRv.getPreferredInstrument(),
                    srcRv.getPreferredInstrumentSettings(),
                    srcRv.getPreferredChannel());
        }
        return res;
    }

    private List<RhythmParameter<?>> findCommonRPs(Rhythm r1, Rhythm r2)
    {
        List<RhythmParameter<?>> rps = new ArrayList<>();
        var r2Rps = r2.getRhythmParameters();
        for (var rp : r1.getRhythmParameters())
        {
            for (var rp2 : r2Rps)
            {
                if (rp.isCompatibleWith(rp2))
                {
                    rps.add(rp);
                    break;
                }
            }
        }
        return rps;
    }

}

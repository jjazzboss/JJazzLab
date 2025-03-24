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
package org.jjazz.proswing;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.phrasetransform.api.rps.RP_SYS_DrumsTransform;
import org.jjazz.proswing.walkingbass.WbpSourceDatabase;
import org.jjazz.proswing.walkingbass.WalkingBassMusicGenerator;
import org.jjazz.rhythm.api.Division;
import org.jjazz.rhythm.api.Genre;
import org.jjazz.rhythm.api.MusicGenerationException;
import static org.jjazz.rhythm.api.Rhythm.PROP_RESOURCES_LOADED;
import org.jjazz.rhythm.api.RhythmFeatures;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.api.TempoRange;
import org.jjazz.rhythmmusicgeneration.spi.MusicGenerator;
import org.jjazz.songcontext.api.SongContext;
import org.jjazz.utilities.api.Utilities;
import org.jjazz.yamjjazz.rhythm.api.AccType;
import org.jjazz.yamjjazz.rhythm.api.Style;
import org.jjazz.yamjjazz.rhythm.api.StylePart;
import org.jjazz.yamjjazz.rhythm.api.YamJJazzRhythm;
import org.jjazz.yamjjazz.rhythm.api.YamJJazzRhythmProvider;
import org.netbeans.api.annotations.common.StaticResource;
import org.jjazz.proswing.api.YjzCompositeRhythm;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_CustomPhrase;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Fill;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Intensity;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Marker;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_TempoFactor;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Variation;
import org.jjazz.rhythmmusicgeneration.api.CompositeMusicGenerator;
import org.jjazz.rhythmmusicgeneration.api.RP_SYS_Mute;
import org.jjazz.yamjjazz.rhythm.api.YamJJazzRhythmGenerator;

/**
 * Use a YamJJazzRhythm for all the tracks except bass.
 */
public class ProSwingRhythmImpl implements YjzCompositeRhythm
{

    @StaticResource(relative = true)
    private final String YJZ_RESOURCE = "psBase.yjz";
    @StaticResource(relative = true)
    private final String STY_RESOURCE = "psBase.sst";

    private final String uniqueId;
    private final String name;
    private final String author;
    private final String description;
    private final String version;
    private final int preferredTempo;
    private final TimeSignature timeSignature;
    private final RhythmFeatures features;
    private final String[] tags;
    private YamJJazzRhythm baseRhythm;
    private MusicGenerator musicGenerator;
    private final Multimap<MusicGenerator, RhythmVoice> mmapGenRvs;
    private final List<RhythmParameter<?>> rhythmParameters;
    private final List<RhythmVoice> rhythmVoices;
    private final transient SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(ProSwingRhythmImpl.class.getSimpleName());


    public ProSwingRhythmImpl() throws IOException
    {
        name = "ProSwing 4/4";
        uniqueId = name + "-ID";
        author = "-";
        version = "1";
        baseRhythm = null;
        timeSignature = TimeSignature.FOUR_FOUR;
        description = "JJazzLab customizable Swing style";

        preferredTempo = 120;
        features = new RhythmFeatures(Genre.JAZZ, Division.EIGHTH_SHUFFLE, TempoRange.ALL_TEMPO);

        tags = List.of("premium", "swing", "jazz").toArray(new String[0]);


        // Initialize the base rhythm
        var yjrProvider = YamJJazzRhythmProvider.getInstance();
        var yjzFile = extractFiles();       // throw IOException
        baseRhythm = (YamJJazzRhythm) yjrProvider.readFast(yjzFile);    // throw IOException


        rhythmVoices = buildRhythmVoices(baseRhythm);
        rhythmParameters = buildRhythmParameters(baseRhythm);


        // Initialize our CompositeMusicGenerator
        var baseGenerator = new YamJJazzRhythmGenerator(this);
        var bassGenerator = new WalkingBassMusicGenerator(this);
        mmapGenRvs = MultimapBuilder.hashKeys().arrayListValues().build();
        for (var rv : getRhythmVoices())
        {
            var gen = rv.getType() == RhythmVoice.Type.BASS ? bassGenerator : baseGenerator;
            mmapGenRvs.put(gen, rv);
        }
        musicGenerator = new CompositeMusicGenerator(this, mmapGenRvs);

    }

    @Override
    public File getFile()
    {
        return new File("");
    }

    @Override
    public Map<RhythmVoice, Phrase> generateMusic(SongContext context, RhythmVoice... rvs) throws MusicGenerationException
    {
        var res = musicGenerator.generateMusic(context, rvs);
        return res;
    }


    @Override
    public String toString()
    {
        return getName();
    }
    // ================================================================================================
    // YamJJazzRhythm implementation
    // ================================================================================================

    @Override
    public AccType getAccType(String rpMuteValue)
    {
        return baseRhythm.getAccType(rpMuteValue);
    }

    @Override
    public int getComplexityLevel(String rpVariationValue)
    {
        return baseRhythm.getComplexityLevel(rpVariationValue);
    }

    @Override
    public RhythmVoice getRhythmVoice(AccType at)
    {
        var baseRv = baseRhythm.getRhythmVoice(at);
        RhythmVoice res = rhythmVoices.stream()
                .filter(rv -> rv.getName().equals(baseRv.getName()))
                .findAny()
                .orElseThrow();
        return res;
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
    // ================================================================================================
    // ProSwingRhythm implementation
    // ================================================================================================

    @Override
    public Multimap<MusicGenerator, RhythmVoice> getGenerators()
    {
        return mmapGenRvs;
    }

    @Override
    public YamJJazzRhythm getBaseRhythm()
    {
        return baseRhythm;
    }

    // ================================================================================================
    // Rhythm implementation
    // ================================================================================================
    @Override
    public boolean isResourcesLoaded()
    {
        return baseRhythm != null && baseRhythm.isResourcesLoaded();
    }

    @Override
    public void loadResources() throws MusicGenerationException
    {
        if (isResourcesLoaded())
        {
            return;
        }

        // Initialize the base rhythm
        baseRhythm.loadResources();  // throws MusicGenerationException

        // Initialize the WalkingBass database
        var wbpsDB = WbpSourceDatabase.getInstance();
        LOGGER.log(Level.INFO, "loadResources() wbpSourceDB size={0}", wbpsDB.getNbWbpSources(-1));
        LOGGER.severe("loadResources() debug updating SYSTEM_PROP_NOTEEVENT_TOSTRING_FORMAT");
        System.setProperty(NoteEvent.SYSTEM_PROP_NOTEEVENT_TOSTRING_FORMAT, "%1$s");
        wbpsDB.checkConsistency(BassStyle.TWO_FEEL);
        wbpsDB.checkConsistency(BassStyle.WALKING);
        
        
        pcs.firePropertyChange(PROP_RESOURCES_LOADED, false, true);
    }

    @Override
    public void releaseResources()
    {
        LOGGER.log(Level.FINE, "releaseResources() this={0}", getName());
        if (!isResourcesLoaded())
        {
            LOGGER.fine("releaseResources() Do nothing : resources are already released");
            return;
        }

        baseRhythm.releaseResources();

        pcs.firePropertyChange(PROP_RESOURCES_LOADED, true, false);
    }


    @Override
    public final List<RhythmVoice> getRhythmVoices()
    {
        return rhythmVoices;
    }

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
    public RhythmFeatures getFeatures()
    {
        return features;
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

    // ================================================================================================
    // Private methods
    // ================================================================================================

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
        List<RhythmVoice> rvs = new ArrayList<>();

        // Copy all base rhythm RhythmVoices
        for (var rvBase : r.getRhythmVoices())
        {
            RhythmVoice rv;
            if (rvBase.isDrums())
            {
                rv = new RhythmVoice(rvBase.getDrumKit(), this,
                        rvBase.getType(),
                        rvBase.getName(),
                        rvBase.getPreferredInstrument(),
                        rvBase.getPreferredInstrumentSettings(),
                        rvBase.getPreferredChannel());
            } else
            {
                rv = new RhythmVoice(this,
                        rvBase.getType(),
                        rvBase.getName(),
                        rvBase.getPreferredInstrument(),
                        rvBase.getPreferredInstrumentSettings(),
                        rvBase.getPreferredChannel());
            }
            rvs.add(rv);
        }

        return Collections.unmodifiableList(rvs);
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
        rps.add(new RP_BassStyle(true));
        rps.add(rpIntensity);
        rps.add(rpFill);
        rps.add(rpMute);
        rps.add(RP_SYS_Marker.getInstance());
        rps.add(RP_SYS_TempoFactor.getInstance());
        rps.add(rpDrumsTransform);
        rps.add(rpCustomPhrase);


        return Collections.unmodifiableList(rps);
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

    // ======================================================================================================
    // private methods
    // ======================================================================================================

    /**
     * Get the RhythmVoice from baseRhythm which matches name.
     *
     * @param name
     * @return
     */
    private RhythmVoice getBaseRhythmVoice(String name)
    {
        RhythmVoice res = baseRhythm.getRhythmVoices().stream()
                .filter(rv -> rv.getName().equals(name))
                .findAny()
                .orElseThrow();
        return res;
    }

    /**
     * Extract the 2 resource files in the temporary directory.
     *
     * @return The YJZ file
     * @throws java.io.IOException
     */
    private File extractFiles() throws IOException
    {
        Path yjzPath = Files.createTempFile("psBase", ".yjz");
        File yjzFile = yjzPath.toFile();
        yjzFile.deleteOnExit();

        Path styPath = Utilities.replaceExtension(yjzFile, ".sst").toPath();
        styPath.toFile().deleteOnExit();

        Utilities.copyResource(getClass(), STY_RESOURCE, styPath);
        Utilities.copyResource(getClass(), YJZ_RESOURCE, yjzPath);

        return yjzPath.toFile();
    }


}

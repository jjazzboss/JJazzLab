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
package org.jjazz.jjswing.api;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.jjswing.drums.DrumsGenerator;
import org.jjazz.jjswing.drums.db.DpSourceDatabase;
import org.jjazz.phrasetransform.api.rps.RP_SYS_DrumsTransform;
import org.jjazz.jjswing.bass.db.WbpSourceDatabase;
import org.jjazz.jjswing.bass.BassGenerator;
import org.jjazz.jjswing.bass.BassGeneratorSettings;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.rhythm.api.Division;
import org.jjazz.rhythm.api.Genre;
import org.jjazz.rhythm.api.MusicGenerationException;
import static org.jjazz.rhythm.api.Rhythm.PROP_RESOURCES_LOADED;
import org.jjazz.rhythm.api.RhythmFeatures;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.api.TempoRange;
import org.jjazz.rhythmmusicgeneration.spi.MusicGenerator;
import org.jjazz.utilities.api.Utilities;
import org.jjazz.yamjjazz.rhythm.api.AccType;
import org.jjazz.yamjjazz.rhythm.api.Style;
import org.jjazz.yamjjazz.rhythm.api.StylePart;
import org.jjazz.yamjjazz.rhythm.api.YamJJazzRhythm;
import org.jjazz.yamjjazz.rhythm.api.YamJJazzRhythmProvider;
import org.netbeans.api.annotations.common.StaticResource;
import org.jjazz.rhythm.api.Rhythm;
import static org.jjazz.rhythm.api.RhythmVoice.Type.BASS;
import static org.jjazz.rhythm.api.RhythmVoice.Type.DRUMS;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_CustomPhrase;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Fill;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Intensity;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Marker;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_TempoFactor;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Variation;
import org.jjazz.rhythmmusicgeneration.api.CompositeMusicGenerator;
import org.jjazz.rhythmmusicgeneration.api.CompositeMusicGenerator.DelegateUnit;
import org.jjazz.rhythmmusicgeneration.api.RP_SYS_Mute;
import org.jjazz.yamjjazz.rhythm.api.YamJJazzRhythmGenerator;
import org.jjazz.rhythmmusicgeneration.api.RP_SYS_OverrideTracks;
import org.jjazz.rhythmmusicgeneration.api.CompositeMusicGenerator.RvToDelegateUnitMapper;

/**
 * An advanced swing rhythm which uses a specific generator for the bass (walking, etc.).
 */
public class JJSwingRhythm implements YamJJazzRhythm
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
    private final List<RhythmParameter<?>> rhythmParameters;
    private final List<RhythmVoice> rhythmVoices;
    private final transient PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(JJSwingRhythm.class.getSimpleName());


    public JJSwingRhythm() throws IOException
    {
        name = "jjSwing";
        uniqueId = name + "-ID";
        author = "-";
        version = "1";
        baseRhythm = null;
        timeSignature = TimeSignature.FOUR_FOUR;
        description = "JJazzLab premium Swing style";

        preferredTempo = 120;
        features = new RhythmFeatures(Genre.JAZZ, Division.EIGHTH_SHUFFLE, TempoRange.ALL_TEMPO);
        tags = List.of("swing", "jazz", "ballad", "bld").toArray(new String[0]);


        // Initialize the base rhythm
        var yjrProvider = YamJJazzRhythmProvider.getInstance();
        var yjzFile = extractFiles();       // throw IOException
        baseRhythm = (YamJJazzRhythm) yjrProvider.readFast(yjzFile);    // throw IOException
        rhythmVoices = buildRhythmVoices(baseRhythm);
        rhythmParameters = buildRhythmParameters(baseRhythm);

        // Need task to avoid endless loop with the RhythmDatabase constructor
        SwingUtilities.invokeLater(() -> musicGenerator = new CompositeMusicGenerator(this, buildRvMapper()));
    }

    @Override
    public File getFile()
    {
        return new File("");
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


        // We must also process other rhythm instances used by our CompositeMusicGenerator
        for (var r : getOtherUsedRhythms())
        {
            r.loadResources();      // throws MusicGenerationException
        }

        // Initialize the WalkingBass database
        var wbpsDB = WbpSourceDatabase.getInstance();
        LOGGER.log(Level.INFO, "loadResources() WbpSourceDatabase checkConsistency() skipped");
        // LOGGER.severe("loadResources() debug updating SYSTEM_PROP_NOTEEVENT_TOSTRING_FORMAT");
        // System.setProperty(NoteEvent.SYSTEM_PROP_NOTEEVENT_TOSTRING_FORMAT, "%1$s");
        // wbpsDB.checkConsistency(BassStyle.TWO_FEEL);
        // wbpsDB.checkConsistency(BassStyle.WALKING);


        // Initialize the Drums phrase database
        var dpsDB = DpSourceDatabase.getInstance(TimeSignature.FOUR_FOUR);
        LOGGER.log(Level.INFO, "loadResources() DpSourceDatabase checkConsistency() skipped");
        // dpsDB.checkConsistency();


        // LOGGER.log(Level.WARNING, "loadResources() DEBUG forcing randomization OFF");
        // BassGeneratorSettings.getInstance().setWbpsaStoreRandomized(false);

        LOGGER.log(Level.INFO, "loadResources() isWbpsaStoreRandomized={0}", BassGeneratorSettings.getInstance().isWbpsaStoreRandomized());

                
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


        // We must also process other rhythm instances used by our CompositeMusicGenerator
        getOtherUsedRhythms().forEach(r -> r.releaseResources());


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
        List<RhythmVoice> rvs = r.getRhythmVoices().stream()
                .map(rv -> rv.getCopy(this))
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

        String[] possibleValues = new String[]
        {
            "Main A-1", "Main A-2", "Main B-1", "Main B-2", "Main C-1", "Main C-2", "Main D-1", "Main D-2", "Main E-1", "Main E-2",
            "Fill In AA-1", "Fill In BB-1", "Fill In CC-1", "Fill In DD-1", "Fill In BA-1",
            "Intro A-1", "Ending A-1"
        };
        RP_SYS_Variation rpVariation = new RP_SYS_Variation(true, "Main A-1", possibleValues);


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
        rps.add(new RP_DrumsStyle(true));
        rps.add(rpIntensity);
        rps.add(rpFill);
        rps.add(rpMute);
        rps.add(RP_SYS_Marker.getInstance());
        rps.add(RP_SYS_TempoFactor.getInstance());
        rps.add(rpDrumsTransform);
        rps.add(rpCustomPhrase);
        rps.add(new RP_SYS_OverrideTracks(this, false));


        return Collections.unmodifiableList(rps);
    }

    /**
     * The mapper which redirects each base RhythmVoice to a MusicGenerator, a destination RhythmVoice, and possibly a variation and a post-processor.
     *
     * @return
     */
    private RvToDelegateUnitMapper buildRvMapper()
    {
        var yjGenerator = new YamJJazzRhythmGenerator(this);
        var bassGenerator = new BassGenerator(this);
        var drumsGenerator = new DrumsGenerator(this);

        RvToDelegateUnitMapper res = (baseRv, spt) -> 
        {
            Objects.requireNonNull(baseRv);
            Objects.requireNonNull(spt);
            assert rhythmVoices.contains(baseRv) : "baseRv=" + baseRv;

            var rpVariationValue = spt.getRPValue(RP_SYS_Variation.getVariationRp(this));

            // Default values
            MusicGenerator destMg;
            RhythmVoice destRv = baseRv;
            String destRpVariationValue = rpVariationValue;
            Consumer<Phrase> postProcessor = null;


            switch (baseRv.getType())
            {
                case DRUMS, PERCUSSION ->
                {
                    destMg = drumsGenerator;
                }
                case BASS ->
                {
                    destMg = bassGenerator;
                }
                default ->
                {
                    destMg = yjGenerator;
                    if (rpVariationValue.toLowerCase().startsWith("main e"))
                    {
                        destRpVariationValue = "Main C-1";
                    }
                }
            }

            return new DelegateUnit(spt, baseRv, destMg, destRv, destRpVariationValue, postProcessor);
        };

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

    private List<Rhythm> getOtherUsedRhythms()
    {
        return Collections.emptyList();
    }

}

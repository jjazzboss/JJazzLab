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
package org.jjazz.yamjjazz.rhythm;

import org.jjazz.yamjjazz.rhythm.api.YamJJazzRhythmGenerator;
import com.google.common.collect.Lists;
import org.jjazz.yamjjazz.rhythm.api.YamJJazzRhythm;
import org.jjazz.yamjjazz.rhythm.api.YamJJazzDefaultRhythms;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.InvalidMidiDataException;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.midi.api.Instrument;
import org.jjazz.midi.api.InstrumentMix;
import org.jjazz.midi.api.InstrumentSettings;
import org.jjazz.midi.api.synths.GMSynth;
import org.jjazz.phrasetransform.api.rps.RP_SYS_DrumsTransform;
import org.jjazz.rhythm.api.Genre;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.api.RhythmFeatures;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.api.TempoRange;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Fill;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Intensity;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Variation;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_CustomPhrase;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Marker;
import org.jjazz.rhythmmusicgeneration.api.RP_SYS_OverrideTracks;
import org.jjazz.rhythmmusicgeneration.api.RP_SYS_Mute;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_TempoFactor;
import org.jjazz.rhythmmusicgeneration.spi.MusicGenerator;
import org.jjazz.yamjjazz.rhythm.api.AccType;
import org.jjazz.yamjjazz.FormatNotSupportedException;
import org.jjazz.yamjjazz.MPL_ExtensionFile;
import org.jjazz.yamjjazz.rhythm.api.Style;
import org.jjazz.yamjjazz.rhythm.api.StylePart;
import org.jjazz.yamjjazz.rhythm.api.StylePartType;

/**
 * A rhythm based on a Yamaha style file which can optionally be extended using a special Midi extension file.
 * <p>
 * - There can be an unlimited number of StyleParts<br>
 * - Each StylePart has different source phrases per complexity level, and for one complexity level we may have several source phrase variations.<p>
 * Rhythm Parameters are:<br>
 * - RP_Variation (one value for each StylePart/Complexity pair)<br>
 * - RP_Intensity<br>
 * - RP_Fill : use the source phrases from StyleParts Fill_In_**<br>
 * - RP_Mute<br>
 * - RP_SYS_DrumsMix<br>
 *
 * @see MPL_ExtensionFile
 */
public class YamJJazzRhythmImpl implements YamJJazzRhythm
{

    private String uniqueId;
    private String name;
    private String author;
    private String description;
    private String version;
    private int preferredTempo;
    private TimeSignature timeSignature;
    private RhythmFeatures features;
    private String[] tags;
    private File stdFile;
    private File extFile;
    private MusicGenerator generator;
    /**
     * The default RhythmParameters associated to this rhythm.
     */
    private List<RhythmParameter<?>> rhythmParameters;
    /**
     * The supported RhythmVoices.
     */
    private List<RhythmVoice> rhythmVoices;
    private Style style;
    private boolean isExtendedRhythm;
    private final transient PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(YamJJazzRhythm.class.getSimpleName());

    /**
     * Create a YamJJazzRhythm from a standard Yamaha style file.
     * <p>
     *
     * @param stdFile A standard Yamaha stdFile.
     * @throws javax.sound.midi.InvalidMidiDataException
     * @throws java.io.IOException
     * @throws org.jjazz.yamjjazz.FormatNotSupportedException
     */
    public YamJJazzRhythmImpl(File stdFile) throws InvalidMidiDataException, IOException, FormatNotSupportedException
    {
        if (stdFile == null)
        {
            throw new NullPointerException("stdFile");   //NOI18N
        }
        this.stdFile = stdFile;
        uniqueId = stdFile.getName() + "-ID";
        name = stdFile.getName();
        author = "-";
        version = "1";

        style = new Style();
        style.readNonMusicData(stdFile);        // Throw exceptions. Also compute style.division
        timeSignature = style.timeSignature;
        description = "Standard Yamaha style (" + style.sffType.toString() + ")";

        preferredTempo = TempoRange.checkTempo(style.tempo) ? style.tempo : TempoRange.TEMPO_STD;
        var tempoRange = computeTempoRange(preferredTempo);

        // If it's a default rhythm, we can retrieve its genre
        var yjdr = YamJJazzDefaultRhythms.getInstance();
        Genre genre = yjdr.getGenre(name);
        if (genre == null)
        {
            // Not a default name, 
            // Don't try Genre.guess(name) because if it matches (eg name contains "pop"), it might be used instead of the YamJJazzDefaultRhythms
            genre = Genre.UNKNOWN;
        }
        features = new RhythmFeatures(genre, style.division, tempoRange);

        var tmpTags = Lists.newArrayList("yamaha");      // First item must be different from the extended style. See 
        tmpTags.addAll(yjdr.getDefaultTags(name));      // Optional additional tags if it's a default rhythm
        tags = tmpTags.toArray(String[]::new);

        // Generate the main data
        buildRhythmVoices();
        buildRhythmParameters();

        isExtendedRhythm = false;
    }

    /**
     * Create a YamJJazzRhythm from a standard Yamaha style file AND an extension file.
     * <p>
     *
     * @param stdFile A standard Yamaha stdFile.
     * @param extFile A standard Yamaha stdFile.
     * @throws javax.sound.midi.InvalidMidiDataException
     * @throws java.io.IOException
     * @throws org.jjazz.yamjjazz.FormatNotSupportedException
     */
    public YamJJazzRhythmImpl(File stdFile, File extFile) throws InvalidMidiDataException, IOException, FormatNotSupportedException
    {
        if (stdFile == null || extFile == null)
        {
            throw new NullPointerException("stdFile=" + stdFile + " extFile=" + extFile);   //NOI18N
        }
        this.stdFile = stdFile;
        this.extFile = extFile;
        uniqueId = extFile.getName() + "-ID";
        name = extFile.getName();
        description = "YamJJazz extended style";
        author = "-";
        version = "1.0";

        style = new Style();
        style.readNonMusicData(extFile, stdFile);        // Throw exceptions. Also compute style.division


        timeSignature = style.timeSignature;
        preferredTempo = TempoRange.checkTempo(style.tempo) ? style.tempo : TempoRange.TEMPO_STD;
        var tempoRange = computeTempoRange(preferredTempo);


        // If it's a default rhythm, we can retrieve its genre
        var yjdr = YamJJazzDefaultRhythms.getInstance();
        Genre genre = yjdr.getGenre(name);
        if (genre == null)
        {
            // Not a default name, 
            // Don't try Genre.guess(name) because if it matches (eg name contains "pop"), it might be used instead of the YamJJazzDefaultRhythms
            genre = Genre.UNKNOWN;
        }
        features = new RhythmFeatures(genre, style.division, tempoRange);


        var tmpTags = Lists.newArrayList("yamaha extended");           // First item must be different from the standard style. 
        tmpTags.addAll(yjdr.getDefaultTags(name));      // Optional additional tags if it's a default rhythm
        tags = tmpTags.toArray(String[]::new);


        buildRhythmVoices();             // Throw exceptions
        buildRhythmParameters();        // Throw exceptions

        isExtendedRhythm = true;
    }


    @Override
    public MusicGenerator getMusicGenerator()
    {
        if (generator == null)
        {
            generator = new YamJJazzRhythmGenerator(this);
        }
        return generator;
    }

    @Override
    public void setMusicGenerator(MusicGenerator mg)
    {
        Objects.requireNonNull(mg);
        generator = mg;
    }


    /**
     * True if it's an extended rhythm, false if it's a standard Yamaha rhythm.
     *
     * @return
     */
    @Override
    public boolean isExtendedRhythm()
    {
        return isExtendedRhythm;
    }

    /**
     * The base style stdFile (a standard Yamaha style stdFile) for this rhythm.
     * <p>
     * getFile() will return the style extension extFile.
     *
     * @return
     * @see #getFile()
     */
    public File getBaseStyleFile()
    {
        return this.stdFile;
    }

    /**
     * Analyze a RhythmParameter String value produced by buildRhythmParameters() to retrieve the corresponding StylePart.
     *
     * @param rpValue eg "Main A-2"
     * @return Main_A or null if no match
     */
    @Override
    public StylePart getStylePart(String rpValue)
    {
        if (rpValue == null)
        {
            throw new IllegalArgumentException("rpValue=" + rpValue);   //NOI18N
        }
        StylePart res = null;
        String strs[] = rpValue.trim().split("-");
        if (strs.length == 2)
        {
            StylePartType type = StylePartType.getType(strs[0].trim());
            if (type != null)
            {
                res = style.getStylePart(type);
            }
        }
        return res;
    }

    /**
     * Analyze the RP_Variation value produced by buildRhythmParameters() to retrieve the complexity level.
     *
     * @param rpValue eg "Main A-2"
     * @return eg 2, or -1 if no match.
     */
    @Override
    public int getComplexityLevel(String rpValue)
    {
        if (rpValue == null)
        {
            throw new IllegalArgumentException("rpValue");   //NOI18N
        }
        int res = -1;
        String strs[] = rpValue.trim().split("-");
        if (strs.length == 2)
        {
            try
            {
                res = Integer.parseInt(strs[1]);
            } catch (NumberFormatException e)
            {
                // Nothing  
            }
        }
        return res;
    }

    @Override
    public Style getStyle()
    {
        return style;
    }


    /**
     * Get the AccType corresponding to a RP_SYS_Mute value.
     *
     * @param rpMuteValue Can't be null
     * @return Can be null if rpMuteValue is an empty string.
     */
    @Override
    public AccType getAccType(String rpMuteValue)
    {
        if (rpMuteValue == null)
        {
            throw new NullPointerException("rpMuteValue");   //NOI18N
        }
        AccType at = null;
        if (!rpMuteValue.isEmpty())
        {
            at = AccType.valueOf(rpMuteValue);
        }
        return at;
    }

    @Override
    public String toString()
    {
        return getName();
    }

    // ================================================================================================
    // Rhythm implementation
    // ================================================================================================
    @Override
    public boolean isResourcesLoaded()
    {
        StylePart spMainA = style.getStylePart(StylePartType.Main_A);          // Main_A must be always there
        return spMainA == null ? false : spMainA.isMusicLoaded();
    }

    /**
     * If StyleParts Phrases are not loaded, load them.
     * <p>
     */
    @Override
    public void loadResources() throws MusicGenerationException
    {
        if (isResourcesLoaded())
        {
            return;
        }
        try
        {
            if (isExtendedRhythm)
            {
                style.readMusicData(extFile, stdFile);
            } else
            {
                style.readMusicData(stdFile);
            }
        } catch (FormatNotSupportedException | IOException | InvalidMidiDataException ex)
        {
            LOGGER.log(Level.SEVERE, "{0} - loadResources() problem reading file: {1}", new Object[]
            {
                getName(), ex.getLocalizedMessage()
            });
            throw new MusicGenerationException(ex.getLocalizedMessage());
        }

        pcs.firePropertyChange(PROP_RESOURCES_LOADED, false, true);
    }

    /**
     * If StyleParts Phrases are loaded, unload them.
     */
    @Override
    public void releaseResources()
    {
        LOGGER.log(Level.FINE, "releaseResources() this={0}", getName());
        if (!isResourcesLoaded())
        {
            LOGGER.fine("releaseResources() Do nothing : resources are already released");
            return;
        }
        for (StylePartType type : style.getStylePartTypes())
        {
            StylePart sp = style.getStylePart(type);
            sp.clearMusicData(-1);
        }
        pcs.firePropertyChange(PROP_RESOURCES_LOADED, true, false);
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
    public boolean equals(Object o)
    {
        boolean res = false;
        if (o instanceof YamJJazzRhythm)
        {
            YamJJazzRhythm r = (YamJJazzRhythm) o;
            res = r.getUniqueId().equals(getUniqueId());
        }
        return res;
    }

    @Override
    public int hashCode()
    {
        int hash = 5;
        hash = 47 * hash + Objects.hashCode(getUniqueId());
        return hash;
    }

    @Override
    public TimeSignature getTimeSignature()
    {
        return timeSignature;
    }

    /**
     * Return extFile if isExtendedRhythm()==true, otherwise return baseFile
     *
     * @return
     */
    @Override
    public File getFile()
    {
        return isExtendedRhythm() ? extFile : stdFile;
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
     * There is one rhythmVoice per AccPart.
     *
     * @throws org.jjazz.yamjjazz.FormatNotSupportedException
     */
    private void buildRhythmVoices() throws FormatNotSupportedException
    {
        LOGGER.log(Level.FINER, "buildRhythmVoices() --  this={0}", this);
        rhythmVoices = new ArrayList<>();

        for (AccType at : style.getAllAccTypes())
        {
            InstrumentMix insMix = style.getSInt().get(at);
            if (insMix == null)
            {
                LOGGER.log(Level.WARNING,
                        "{0} - buildRhythmVoices() InstrumentMix missing for rhyrhm voice {1}. Using a void InstrumentMix instead.",
                        new Object[]
                        {
                            getName(),
                            at
                        });
                Instrument ins = GMSynth.getInstance().getVoidInstrument();
                insMix = new InstrumentMix(ins, new InstrumentSettings());
            }
            RhythmVoice rv = buildRhythmVoice(at, insMix);
            rhythmVoices.add(rv);
            LOGGER.log(Level.FINER, "    adding rv={0}", rv);
        }

        if (rhythmVoices.isEmpty())
        {
            String msg = getName() + " - invalid style : no rhythm voice found.";
            LOGGER.log(Level.SEVERE, "buildRhythmVoices() {0}", msg);
            throw new FormatNotSupportedException(msg);
        }

        rhythmVoices = Collections.unmodifiableList(rhythmVoices);
    }

    /**
     * Build a RhythmVoice for a style part of specified type.
     * <p>
     * RhythmVoice name is set to at name (RHYTHM, CHORD1, etc.).
     *
     * @param at
     * @param insMix
     * @return
     * @see MPL_SInt.onInstrumentParsed()
     */
    private RhythmVoice buildRhythmVoice(AccType at, InstrumentMix insMix)
    {
        Instrument ins = insMix.getInstrument();

        RhythmVoice.Type type = at.getRvType();

        RhythmVoice rv;
        if (!at.isDrums())
        {
            // No drums
            assert !ins.isDrumKit() :
                    "this=" + this + " ins=" + ins.getFullName() + " at=" + at + " file=" + this.getFile().getAbsolutePath();   //NOI18N
            rv = new RhythmVoice(this, type, at.toString(), ins, insMix.getSettings(), at.getChannel());

        } else
        {
            // Drums
            DrumKit kit = (ins == GMSynth.getInstance().getVoidInstrument()) ? new DrumKit() : ins.getDrumKit();
            assert kit != null : "this=" + this + " ins=" + ins.toLongString() + " at=" + at + " file=" + this.getFile().getAbsolutePath();   //NOI18N
            rv = new RhythmVoice(kit, this, type, at.toString(), ins, insMix.getSettings(), at.getChannel());
        }

        return rv;
    }

    /**
     * Build the RhythmParameters for this rhythm.
     * <p>
     * The RP_Variation values are built from the "defined" StyleParts and complexity levels. We consider a StylePart/Complexity "defined" when there is at
     * least one SourcePhraseSet defined, which can be empty (ie because music data are not yet loaded).<br>
     * Examples: Main A-1, Main A-2, Main B-3, etc.
     * <p>
     * @throws org.jjazz.yamjjazz.FormatNotSupportedException If style does not contain a Main_A style part.
     */
    private void buildRhythmParameters() throws FormatNotSupportedException
    {
        // Need Main A at least
        StylePart spMainA = style.getStylePart(StylePartType.Main_A);
        if (spMainA == null)
        {
            LOGGER.log(Level.SEVERE, "{0} - buildRhythmParameters() invalid style : Main_A style part missing", getName());
            throw new FormatNotSupportedException(getName() + " - invalid style : Main_A style part missing");
        }

        rhythmParameters = new ArrayList<>();


        // Compute the Variation RhythmParameter possible values    
        ArrayList<String> strs = new ArrayList<>();
        String defaultStr = null;
        for (StylePartType type : style.getStylePartTypes())
        {
            StylePart stylePart = style.getStylePart(type);
            int minCplxLevel = 1000000;
            for (Integer complexity : stylePart.getComplexityLevels())
            {
                minCplxLevel = Math.min(complexity, minCplxLevel);
                String str = stylePart.getType().toString() + "-" + complexity;
                strs.add(str);
            }
            if (type.equals(StylePartType.Main_A))
            {
                defaultStr = stylePart.getType().toString() + "-" + minCplxLevel;
            }
        }
        // Robustness: it can happen there is no Main A in a (corrupted probably) style stdFile
        if (defaultStr == null)
        {
            defaultStr = strs.get(0);
        }

        // Special rp variation based on the variation contained in the style
        RP_SYS_Variation rpVariation = new RP_SYS_Variation(true, defaultStr, strs.toArray(String[]::new))
        {
            /**
             * Return the the variation length in bars.
             *
             * @param value
             * @return
             */
            @Override
            public String getValueDescription(String value)
            {
                String res = null;
                StylePart sp = getStylePart(value);
                if (sp != null && isResourcesLoaded())
                {
                    int nbBars = (int) (sp.getSizeInBeats() / getTimeSignature().getNbNaturalBeats());
                    res = "length=" + nbBars + " bars";
                }
                return res;
            }
        };


        // Intensity : use default
        RP_SYS_Intensity rpIntensity = new RP_SYS_Intensity(true);
        // Fill : use default
        RP_SYS_Fill rpFill = new RP_SYS_Fill(true);
        // Mute : use available tracks      
        RP_SYS_Mute rpMute = RP_SYS_Mute.createMuteRp(this, false);
        RP_SYS_CustomPhrase rpCustomPhrase = new RP_SYS_CustomPhrase(this, false);
        RP_SYS_OverrideTracks rpSubstituteTracks = new RP_SYS_OverrideTracks(this, false);


        rhythmParameters.add(rpVariation);
        rhythmParameters.add(rpIntensity);
        rhythmParameters.add(rpFill);
        rhythmParameters.add(rpMute);
        rhythmParameters.add(RP_SYS_Marker.getInstance());
        rhythmParameters.add(RP_SYS_TempoFactor.getInstance());


        // Add the RP_SYS_DrumsTransform parameter if there is a drums track
        RhythmVoice rvDrums = getRhythmVoice(AccType.RHYTHM);
        if (rvDrums != null)
        {
            RP_SYS_DrumsTransform rpDrumsTransform = new RP_SYS_DrumsTransform(rvDrums, false);
            rhythmParameters.add(rpDrumsTransform);
        }

        rhythmParameters.add(rpCustomPhrase);
        rhythmParameters.add(rpSubstituteTracks);

        // Make it unmodifiable
        rhythmParameters = Collections.unmodifiableList(rhythmParameters);
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

    private TempoRange computeTempoRange(int t)
    {
        var tr = new TempoRange(Math.max(TempoRange.TEMPO_MIN, t - 25), Math.min(TempoRange.TEMPO_MAX, t + 25), "Custom");
        return tr;
    }

}

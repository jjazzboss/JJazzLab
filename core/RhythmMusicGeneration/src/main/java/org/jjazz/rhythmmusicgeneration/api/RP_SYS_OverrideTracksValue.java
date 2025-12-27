package org.jjazz.rhythmmusicgeneration.api;

import com.google.common.base.Preconditions;
import static com.google.common.base.Preconditions.checkNotNull;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Variation;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.jjazz.rhythmdatabase.api.UnavailableRhythmException;
import org.jjazz.rhythmmusicgeneration.spi.ConfigurableMusicGeneratorProvider;
import org.jjazz.rhythmmusicgeneration.spi.MusicGeneratorProvider;

/**
 * Store which source RhythmVoice is overridden by which [RhythmVoice-rhythm variation] pair.
 * <p>
 * This is an immutable value.
 */
public class RP_SYS_OverrideTracksValue
{

    /**
     * @param rvDest    Can not be null. Rhythm container must implement MusicGeneratorProvider
     * @param variation rvDest's rhythm variation. If null, the music generator should try to reuse the same variation than the source RhythmVoice.
     */
    public record Override(RhythmVoice rvDest, String variation)
            {

        public Override
        {
            Objects.requireNonNull(rvDest);
            Preconditions.checkArgument(rvDest.getContainer() instanceof MusicGeneratorProvider, "rvDest=%s", rvDest);
            if (variation != null)
            {
                var rpVariation = RP_SYS_Variation.getVariationRp(rvDest.getContainer());
                if (rpVariation == null || !rpVariation.getPossibleValues().contains(variation))
                {
                    throw new IllegalArgumentException("rv=" + rvDest + " variation=" + variation);
                }
            }
        }

        /**
         * Return a new Override with rvDest set to newRvDest.
         *
         * @param newRvDest
         * @return
         */
        public Override set(RhythmVoice newRvDest)
        {
            return new Override(newRvDest, variation());
        }

        /**
         * Return a new Override with variation set to newVariation.
         *
         * @param newVariation
         * @return
         */
        public Override set(String newVariation)
        {
            return new Override(rvDest(), newVariation);
        }
    }
    private final Map<RhythmVoice, Override> mapRvOverride;
    private final Rhythm baseRhythm;
    private static final Logger LOGGER = Logger.getLogger(RP_SYS_OverrideTracksValue.class.getSimpleName());


    /**
     * Create a value with no override.
     *
     * @param baseRhythm Must implement the ConfigurableMusicGeneratorProvider interface
     */
    public RP_SYS_OverrideTracksValue(Rhythm baseRhythm)
    {
        this(baseRhythm, new HashMap<>());
    }

    /**
     * Create the value for baseRhythm with the specified mappings.
     *
     * @param baseRhythm Must implement the ConfigurableMusicGeneratorProvider interface
     * @param mappings   All RhythmVoice keys must belong to baseRhythm. Override can not be null.
     */
    public RP_SYS_OverrideTracksValue(Rhythm baseRhythm, Map<RhythmVoice, Override> mappings)
    {
        checkNotNull(baseRhythm);
        checkNotNull(mappings);
        Preconditions.checkArgument(baseRhythm instanceof ConfigurableMusicGeneratorProvider, "baseRhythm=%s", baseRhythm);

        this.baseRhythm = baseRhythm;
        this.mapRvOverride = new HashMap<>(mappings);
        var rvs = baseRhythm.getRhythmVoices();

        if (mappings.keySet().stream()
                .anyMatch(rv -> !rvs.contains(rv) || mappings.get(rv) == null))
        {
            throw new IllegalArgumentException("Invalid mappings. baseRhythm=" + baseRhythm + " mappings=" + mappings);
        }
    }

    /**
     * Return a new RP_SYS_OverrideTracksValue cloned from this instance but with the rvSrc-Override mapping changed.
     *
     * @param rvSrc    Must belong to the baseRhythm
     * @param override Can be null to remove the mapping for rvSrc
     * @return
     */
    public RP_SYS_OverrideTracksValue set(RhythmVoice rvSrc, Override override)
    {
        var newMap = new HashMap<>(mapRvOverride);
        if (override == null)
        {
            newMap.remove(rvSrc);
        } else
        {
            newMap.put(rvSrc, override);
        }
        return new RP_SYS_OverrideTracksValue(baseRhythm, newMap);        // will do the sanity checks
    }


    public Rhythm getBaseRhythm()
    {
        return baseRhythm;
    }

    /**
     * Get all the destination rhythms used in the mappings.
     *
     * @return
     */
    public Set<Rhythm> getAllDestinationRhythms()
    {
        Set<Rhythm> res = new HashSet<>();
        mapRvOverride.values().forEach(override -> res.add(override.rvDest().getContainer()));
        return res;
    }

    /**
     * If true no RhythmVoice mapping is set.
     *
     * @return
     */
    public boolean isEmpty()
    {
        return mapRvOverride.isEmpty();
    }

    public RP_SYS_OverrideTracksValue clone()
    {
        return new RP_SYS_OverrideTracksValue(baseRhythm, mapRvOverride);
    }

    /**
     * Get all the source RhythmVoices mapped to other RhythmVoices.
     *
     * @return
     */
    public Set<RhythmVoice> getAllSourceRhythmVoices()
    {
        return Collections.unmodifiableSet(mapRvOverride.keySet());
    }

    /**
     * The destination RhythmVoice and variation for rvSrc.
     *
     * @param rvSrc
     * @return Can be null if not mapped
     */
    public Override getOverride(RhythmVoice rvSrc)
    {
        Preconditions.checkArgument(baseRhythm.getRhythmVoices().contains(rvSrc), "rv=%s baseRhythm=%s", rvSrc, baseRhythm);
        return mapRvOverride.get(rvSrc);
    }

    public String toDescriptionString()
    {
        var joiner = new StringJoiner(", ");
        for (var rvSrc : getAllSourceRhythmVoices())
        {
            joiner.add(toString(rvSrc, getOverride(rvSrc)));
        }
        return joiner.toString();
    }

    static public String toString(RhythmVoice rvSrc, Override override)
    {
        String variation = override.variation() == null ? "" : "[" + override.variation() + "]";
        String res = rvSrc.getName() + " > " + override.rvDest().getContainer().getName() + variation + "/" + override.rvDest().getName();
        return res;
    }

    /**
     * Save the specified object state as a string.
     * <p>
     * "Phrase1>>jjSwing-ID>>Bass>>Main B-1 &amp;&amp; Chord1>>popRock-ID>>Chord1" means :<br>
     * - base rhythm voice Phrase1 is mapped to jjSwing/Bass rhythm voice with variation Main B-1<br>
     * - base rhythm voice Chord1 is mapped to popRock/Chord1 rhythm voice with no dest. variation specified.<br>
     *
     * @param v
     * @return
     * @see #loadFromString(org.jjazz.rhythm.api.Rhythm, java.lang.String)
     */
    static public String saveAsString(RP_SYS_OverrideTracksValue v)
    {
        StringJoiner joiner = new StringJoiner(" && ");
        for (RhythmVoice rvSrc : v.getAllSourceRhythmVoices())
        {
            var o = v.getOverride(rvSrc);
            String sVariation = o.variation() == null ? "" : ">>" + o.variation();
            String s = rvSrc.getName() + ">>" + o.rvDest().getContainer().getUniqueId() + ">>" + o.rvDest().getName() + sVariation;
            joiner.add(s);
        }
        return joiner.toString();
    }


    /**
     * Create an object from a string.
     *
     * @param baseRhythm
     * @param s
     * @return Can not be null. If an error occured, returns the default value
     * @see #saveAsString(org.jjazz.rhythmmusicgeneration.api.RP_SYS_OverrideTracksValue)
     */
    static public RP_SYS_OverrideTracksValue loadFromString(Rhythm baseRhythm, String s)
    {
        checkNotNull(baseRhythm);
        checkNotNull(s);

        Map<RhythmVoice, Override> map = new HashMap<>();

        if (s.isBlank())
        {
            // Nothing
        } else if (!s.contains(">>") && s.contains(">"))
        {
            // Try the old method
            map = loadFromStringBefore5_0_2(baseRhythm, s);
        } else if (s.contains(">>"))
        {
            String strs[] = s.split("\\s*&&\\s*");
            for (String str : strs)
            {
                String subStrs[] = str.split("\\s*>>\\s*");
                if (subStrs.length < 3 || subStrs.length > 4)
                {
                    LOGGER.log(Level.WARNING, "loadFromString() Ignoring invalid string: {0}  baseRhythm={1}", new Object[]
                    {
                        str,
                        baseRhythm.getName()
                    });
                    continue;
                }

                // src RhythmVoice
                var strRvSrc = subStrs[0];
                RhythmVoice rvSrc = baseRhythm.getRhythmVoices().stream()
                        .filter(rvi -> rvi.getName().equals(strRvSrc))
                        .findAny()
                        .orElse(null);
                if (rvSrc == null)
                {
                    LOGGER.log(Level.WARNING, "loadFromString() Ignoring invalid rhythm voice name: {0}  baseRhythm={1}", new Object[]
                    {
                        strRvSrc,
                        baseRhythm.getName()
                    });
                    continue;
                }


                // dest Rhythm
                String strDestRhythmId = subStrs[1];
                Rhythm destRhythm;
                try
                {
                    destRhythm = RhythmDatabase.getDefault().getRhythmInstance(strDestRhythmId);
                } catch (UnavailableRhythmException ex)
                {
                    LOGGER.log(Level.WARNING, "loadFromString() Ignoring unknown rhythm on this system. strDestRhythmId={0}", strDestRhythmId);
                    continue;
                }


                // dest RhythmVoice
                var strRvDest = subStrs[2];
                RhythmVoice rvDest = destRhythm.getRhythmVoices().stream()
                        .filter(rvi -> rvi.getName().equals(strRvDest))
                        .findAny()
                        .orElse(null);
                if (rvDest == null)
                {
                    LOGGER.log(Level.WARNING, "loadFromString() Ignoring invalid destination rhythm voice name: {0}   destRhythm={1}", new Object[]
                    {
                        strRvDest,
                        destRhythm.getName()
                    });
                    continue;
                }


                // dest variation
                String destVariation = null;    // by default
                if (subStrs.length == 4)
                {
                    destVariation = subStrs[3].trim();
                }


                // Create the mapping
                try
                {
                    assert rvDest != null;
                    var override = new Override(rvDest, destVariation);     // throws IllegalArgumentException
                    map.put(rvSrc, override);
                } catch (IllegalArgumentException ex)
                {
                    LOGGER.log(Level.WARNING, "loadFromString() Ignoring invalid variation {0} for destRhythm={1}", new Object[]
                    {
                        destVariation,
                        destRhythm.getName()
                    });
                }
            }

        } else
        {

            LOGGER.log(Level.WARNING, "loadFromString() Ignoring invalid string: {0}  baseRhythm={1}", new Object[]
            {
                s,
                baseRhythm.getName()
            });
        }

        RP_SYS_OverrideTracksValue res = new RP_SYS_OverrideTracksValue(baseRhythm, map);

        return res;
    }


    @java.lang.Override
    public String toString()
    {
        return toDescriptionString();
    }

    // ===================================================================================
    // Private methods
    // ===================================================================================    

    /**
     * The method used in JJazzLab *before* version 5.0.2 (retrofitted to use Override objects).
     *
     * @param baseRhythm
     * @param s
     * @return Can not be null
     */
    static private Map<RhythmVoice, Override> loadFromStringBefore5_0_2(Rhythm baseRhythm, String s)
    {
        checkNotNull(baseRhythm);
        checkNotNull(s);

        Map<RhythmVoice, Override> map = new HashMap<>();

        String strs[] = s.split("&");
        for (String str : strs)
        {
            String subStrs[] = str.split(">");
            if (subStrs.length == 3)
            {
                var strRvSrc = subStrs[0];
                RhythmVoice rvSrc = baseRhythm.getRhythmVoices().stream()
                        .filter(rvi -> rvi.getName().equals(strRvSrc))
                        .findAny()
                        .orElse(null);
                if (rvSrc == null)
                {
                    LOGGER.log(Level.WARNING, "loadFromStringBefore5_0_2() Ignoring invalid rhythm voice name {0} for base rhythm {1}", new Object[]
                    {
                        strRvSrc,
                        baseRhythm.getName()
                    });
                    continue;
                }

                String strDestRhythmId = subStrs[1];
                Rhythm destRhythm;
                try
                {
                    destRhythm = RhythmDatabase.getDefault().getRhythmInstance(strDestRhythmId);
                } catch (UnavailableRhythmException ex)
                {
                    LOGGER.log(Level.WARNING, "loadFromStringBefore5_0_2() Ignoring unknown rhythm on this system. rhythmId={0}", strDestRhythmId);
                    continue;
                }

                var strRvDest = subStrs[2];
                RhythmVoice rvDest = destRhythm.getRhythmVoices().stream()
                        .filter(rvi -> rvi.getName().equals(strRvDest))
                        .findAny()
                        .orElse(null);
                if (rvDest == null)
                {
                    LOGGER.log(Level.WARNING, "loadFromStringBefore5_0_2() Ignoring invalid destination rhythm voice name {0} for rhythm {1}", new Object[]
                    {
                        strRvDest,
                        destRhythm.getName()
                    });
                    continue;
                }

                map.put(rvSrc, new Override(rvDest, null));

            }
        }

        return map;
    }

}

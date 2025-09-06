package org.jjazz.rhythmmusicgeneration.api;

import com.google.common.base.Preconditions;
import static com.google.common.base.Preconditions.checkNotNull;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.phrasetransform.api.PhraseTransformChain;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_CustomPhraseValue;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.jjazz.rhythmdatabase.api.UnavailableRhythmException;
import org.jjazz.rhythmmusicgeneration.spi.ConfigurableMusicGeneratorProvider;
import org.jjazz.rhythmmusicgeneration.spi.MusicGeneratorProvider;
import org.jjazz.utilities.api.ResUtil;
import org.openide.util.Exceptions;

/**
 * Store which source RhythmVoices are mapped to which destination RhythmVoices.
 * <p>
 * This is an immutable value.
 */
public class RP_SYS_SubstituteTracksValue
{

    private final Map<RhythmVoice, RhythmVoice> mapSrcDestRhythmVoice;
    private final Rhythm baseRhythm;
    private static final Logger LOGGER = Logger.getLogger(RP_SYS_SubstituteTracksValue.class.getSimpleName());


    /**
     *
     * @param baseRhythm Must implement the ConfigurableMusicGeneratorProvider interface
     */
    public RP_SYS_SubstituteTracksValue(Rhythm baseRhythm)
    {
        this(baseRhythm, new HashMap<>());
    }

    /**
     * Create the value for baseRhythm with the specified src-dest RhythmVoice mappings.
     *
     * @param baseRhythm            Must implement the ConfigurableMusicGeneratorProvider interface
     * @param mapSrcDestRhythmVoice All keys must belong to baseRhythm, and values must belong to a rhythm which implements the MusicGeneratorProvider interface
     */
    public RP_SYS_SubstituteTracksValue(Rhythm baseRhythm, Map<RhythmVoice, RhythmVoice> mapSrcDestRhythmVoice)
    {
        checkNotNull(baseRhythm);
        checkNotNull(mapSrcDestRhythmVoice);
        Preconditions.checkArgument(baseRhythm instanceof ConfigurableMusicGeneratorProvider, "baseRhythm=%s", baseRhythm);

        this.baseRhythm = baseRhythm;
        this.mapSrcDestRhythmVoice = new HashMap<>(mapSrcDestRhythmVoice);
        var rvs = baseRhythm.getRhythmVoices();
        if (!mapSrcDestRhythmVoice.keySet().stream().allMatch(rv -> rvs.contains(rv))
                || !this.mapSrcDestRhythmVoice.values().stream().allMatch(rv -> rv.getContainer() instanceof MusicGeneratorProvider))
        {
            throw new IllegalArgumentException("rhythm=" + baseRhythm + " mapSrcDestRhythmVoice=" + mapSrcDestRhythmVoice);
        }
    }

    /**
     * Return a new RP_SYS_SubstituteTracksValue cloned from this instance but with the rvSrc-rvDest mapping changed.
     *
     * @param rvSrc  Must belong to the baseRhythm
     * @param rvDest Can be null to remove the mapping for rvSrc. Container must be a MusicGeneratorProvider.
     * @return
     */
    public RP_SYS_SubstituteTracksValue set(RhythmVoice rvSrc, RhythmVoice rvDest)
    {
        var newMap = new HashMap<>(mapSrcDestRhythmVoice);
        if (rvDest == null)
        {
            newMap.remove(rvSrc);
        } else
        {
            newMap.put(rvSrc, rvDest);
        }
        return new RP_SYS_SubstituteTracksValue(baseRhythm, newMap);        // will do the sanity checks
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
    public Set<Rhythm> getDestinationRhythms()
    {
        Set<Rhythm> res = new HashSet<>();
        mapSrcDestRhythmVoice.values().forEach(rv -> res.add(rv.getContainer()));
        return res;
    }

    /**
     * If true no RhythmVoice mapping is set.
     *
     * @return
     */
    public boolean isEmpty()
    {
        return mapSrcDestRhythmVoice.isEmpty();
    }

    @Override
    public RP_SYS_SubstituteTracksValue clone()
    {
        return new RP_SYS_SubstituteTracksValue(baseRhythm, mapSrcDestRhythmVoice);
    }

    /**
     * Get all the source RhythmVoices mapped to other RhythmVoices.
     *
     * @return
     */
    public Set<RhythmVoice> getSourceRhythmVoices()
    {
        return Collections.unmodifiableSet(mapSrcDestRhythmVoice.keySet());
    }


    /**
     * The destination RhythmVoice for rvSrc.
     *
     * @param rvSrc
     * @return Can be null if not mapped
     */
    public RhythmVoice getDestRhythmVoice(RhythmVoice rvSrc)
    {
        Preconditions.checkArgument(baseRhythm.getRhythmVoices().contains(rvSrc), "rv=%s baseRhythm=%s", rvSrc, baseRhythm);
        return mapSrcDestRhythmVoice.get(rvSrc);
    }

    public String toDescriptionString()
    {
        String res = "";
        var rvSrcs = mapSrcDestRhythmVoice.keySet();
        int size = rvSrcs.size();
        if (size > 1)
        {
            res = ResUtil.getString(getClass(), "NbSubstituteTracks", size);
        } else if (size == 1)
        {
            var rvSrc = rvSrcs.iterator().next();
            var rvDest = mapSrcDestRhythmVoice.get(rvSrc);
            res = rvSrc.getName() + " > " + rvDest.getContainer().getName();
        }
        return res;
    }

    /**
     * Save the specified object state as a string.
     * <p>
     * "Phrase1>jjSwing-ID>Bass&Chord1>popRock-ID>Chord1" means base rhythm voice Phrase1 has destination rhythmVoice=jjSwing/Bass, etc.
     *
     * @param v
     * @return
     * @see #loadFromString(org.jjazz.rhythm.api.Rhythm, java.lang.String)
     */
    static public String saveAsString(RP_SYS_SubstituteTracksValue v)
    {
        StringJoiner joiner = new StringJoiner("&");
        for (RhythmVoice rvSrc : v.getSourceRhythmVoices())
        {
            var rvDest = v.getDestRhythmVoice(rvSrc);
            String s = rvSrc.getName() + ">" + rvDest.getContainer().getUniqueId() + ">" + rvDest.getName();
            joiner.add(s);
        }
        return joiner.toString();
    }


    /**
     * Create an object from a string.
     *
     * @param baseRhythm
     * @param s
     * @return Can be null
     * @see #saveAsString(org.jjazz.rhythmmusicgeneration.api.RP_SYS_SubstituteTracksValue)
     */
    static public RP_SYS_SubstituteTracksValue loadFromString(Rhythm baseRhythm, String s)
    {
        checkNotNull(baseRhythm);
        checkNotNull(s);

        Map<RhythmVoice, RhythmVoice> mapSrcDestRhythmVoice = new HashMap<>();

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
                    LOGGER.log(Level.WARNING, "loadFromString() Ignoring invalid rhythm voice name {0} for base rhythm {1}", new Object[]
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
                    LOGGER.log(Level.WARNING, "loadFromString() Ignoring unknown rhythm on this system. rhythmId={0}", strDestRhythmId);
                    continue;
                }

                var strRvDest = subStrs[2];
                RhythmVoice rvDest = destRhythm.getRhythmVoices().stream()
                        .filter(rvi -> rvi.getName().equals(strRvDest))
                        .findAny()
                        .orElse(null);
                if (rvDest == null)
                {
                    LOGGER.log(Level.WARNING, "loadFromString() Ignoring invalid destination rhythm voice name {0} for rhythm {1}", new Object[]
                    {
                        strRvDest,
                        destRhythm.getName()                        
                    });
                    continue;
                }

                mapSrcDestRhythmVoice.put(rvSrc, rvDest);

            }
        }

        RP_SYS_SubstituteTracksValue res = new RP_SYS_SubstituteTracksValue(baseRhythm, mapSrcDestRhythmVoice);

        return res;
    }


    @Override
    public String toString()
    {
        return toDescriptionString();
    }

    // ===================================================================================
    // Private methods
    // ===================================================================================    

}

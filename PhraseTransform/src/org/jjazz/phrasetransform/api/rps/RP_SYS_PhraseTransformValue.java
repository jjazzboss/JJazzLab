package org.jjazz.phrasetransform.api.rps;

import static com.google.common.base.Preconditions.checkNotNull;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.logging.Logger;
import org.jjazz.phrasetransform.api.PhraseTransformChain;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;

/**
 * A RhythmParamter to transform YamJJazzRhythm source phrases.
 */
public class RP_SYS_PhraseTransformValue
{

    private final Rhythm rhythm;
    private final Map<RhythmVoice, PhraseTransformChain> mapRvChain = new HashMap<>();

    private static final Logger LOGGER = Logger.getLogger(RP_SYS_PhraseTransformValue.class.getSimpleName());

    public RP_SYS_PhraseTransformValue(Rhythm r)
    {
        checkNotNull(r);
        rhythm = r;
    }

    public RP_SYS_PhraseTransformValue(RP_SYS_PhraseTransformValue v)
    {
        rhythm = v.getRhythm();
        mapRvChain.putAll(v.mapRvChain);
    }

    public Rhythm getRhythm()
    {
        return rhythm;
    }

    /**
     * Set a PhraseTransformChain for the specified RhythmVoice.
     *
     * @param rv
     * @param chain If null or if chain is empty, no chain is associated to rv.
     */
    public void setTransformChain(RhythmVoice rv, PhraseTransformChain chain)
    {
        checkNotNull(rv);
        if (chain == null || chain.isEmpty())
        {
            mapRvChain.remove(rv);
        } else
        {
            mapRvChain.put(rv, new PhraseTransformChain(chain));
        }
    }

    /**
     * Get a copy of the chain associated to rv.
     *
     * @param rv
     * @return Can be null if no chain associated.
     */
    public PhraseTransformChain getTransformChain(RhythmVoice rv)
    {
        var chain = mapRvChain.get(rv);
        return chain == null ? null : new PhraseTransformChain(chain);
    }

    /**
     * Get the RhythmVoices for which a chain is defined.
     *
     * @return
     */
    public Set<RhythmVoice> getChainRhythmVoices()
    {
        return mapRvChain.keySet();
    }

    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 73 * hash + Objects.hashCode(this.mapRvChain);
        return hash;
    }

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
        final RP_SYS_PhraseTransformValue other = (RP_SYS_PhraseTransformValue) obj;
        if (!Objects.equals(this.mapRvChain, other.mapRvChain))
        {
            return false;
        }
        return true;
    }


    public String toDescriptionString()
    {
        return mapRvChain.keySet().toString();
    }

    /**
     * Save the specified object state as a string.
     * <p>
     * Example: "rv1%[PTuniqueId1#prop1=value1,prop2=value2|PTuniqueId2#|PTuniqueId3#prop1=value1]%%%rv2%[PTuniqueId2#]"
     *
     * @param v
     * @return
     * @see loadFromString()
     */
    static public String saveAsString(RP_SYS_PhraseTransformValue v)
    {
        StringJoiner joiner = new StringJoiner("%%%");
        for (RhythmVoice rv : v.mapRvChain.keySet())
        {
            String s = rv.getName() + "%" + PhraseTransformChain.saveAsString(v.mapRvChain.get(rv));
            joiner.add(s);
        }
        return joiner.toString();
    }

    /**
     * Create an object from a string.
     *
     * @param s Example "rv1%[PTuniqueId1#prop1=value1,prop2=value2|PTuniqueId2#|PTuniqueId3#prop1=value1]%%%rv2%[PTuniqueId2#]"
     * @return Can be null
     * @see saveAsString()
     */
    static public RP_SYS_PhraseTransformValue loadFromString(Rhythm r, String s)
    {
        checkNotNull(s);
        RP_SYS_PhraseTransformValue res = new RP_SYS_PhraseTransformValue(r);


        String[] strs = s.split("%%%");


        for (String str : strs)
        {

            String[] subStrs = str.split("%");
            if (subStrs.length != 2)
            {
                res = null;
                break;
            }


            RhythmVoice rv = r.getRhythmVoices().stream().filter(rvi -> rvi.getName().equals(subStrs[0])).findAny().orElse(null);
            if (rv == null)
            {
                res = null;
                break;
            }


            PhraseTransformChain chain;
            try
            {
                chain = PhraseTransformChain.loadFromString(subStrs[1]);
            } catch (ParseException ex)
            {
                res = null;
                break;
            }

            res.mapRvChain.put(rv, chain);

        }
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

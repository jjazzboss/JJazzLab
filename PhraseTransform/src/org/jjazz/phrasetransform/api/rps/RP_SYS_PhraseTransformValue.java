package org.jjazz.phrasetransform.api.rps;

import static com.google.common.base.Preconditions.checkNotNull;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.logging.Logger;
import org.jjazz.phrasetransform.api.PhraseTransformChain;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;

/**
 * A RhythmParameter to transform one or more source phrases.
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

    /**
     * Create a deep copy of the specified value.
     *
     * @param value
     */
    public RP_SYS_PhraseTransformValue(RP_SYS_PhraseTransformValue value)
    {
        rhythm = value.getRhythm();

        for (var rv : value.mapRvChain.keySet())
        {
            var chain = value.mapRvChain.get(rv);
            assert chain != null;
            mapRvChain.put(rv, chain.deepClone());
        }
    }

    public Rhythm getRhythm()
    {
        return rhythm;
    }

    /**
     * Return a copy of this object with the specified chain associated to rv.
     *
     * @param rv
     * @param chain If null or if chain is empty, no chain is associated to rv.
     * @return
     */
    public RP_SYS_PhraseTransformValue getUpdatedTransformChain(RhythmVoice rv, PhraseTransformChain chain)
    {
        checkNotNull(rv);
        var res = new RP_SYS_PhraseTransformValue(this);
        if (chain == null || chain.isEmpty())
        {
            res.mapRvChain.remove(rv);
        } else
        {
            res.mapRvChain.put(rv, new PhraseTransformChain(chain));
        }
        return res;
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
        StringJoiner joiner1 = new StringJoiner(", ");
        List<RhythmVoice> sortedRvs = new ArrayList<>(mapRvChain.keySet());
        sortedRvs.sort(Comparator.comparingInt(RhythmVoice::getPreferredChannel));

        for (var rv : sortedRvs)
        {
            StringJoiner joiner2 = new StringJoiner(">", "[", "]");
            String name = rv.getName();
            var chain = mapRvChain.get(rv);
            chain.forEach(pt -> joiner2.add(pt.getName()));
            joiner1.add(name + "=" + joiner2.toString());
        }

        return joiner1.toString();
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
     * @param r
     * @param s Example "rv1%[PTuniqueId1#prop1=value1,prop2=value2|PTuniqueId2#|PTuniqueId3#prop1=value1]%%%rv2%[PTuniqueId2#]"
     * @return Can be null
     * @see saveAsString()
     */
    static public RP_SYS_PhraseTransformValue loadFromString(Rhythm r, String s)
    {
        checkNotNull(s);
        RP_SYS_PhraseTransformValue res = new RP_SYS_PhraseTransformValue(r);
        if (s.isBlank())
        {
            return res;
        }

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

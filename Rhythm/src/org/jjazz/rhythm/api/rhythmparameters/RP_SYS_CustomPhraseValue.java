package org.jjazz.rhythm.api.rhythmparameters;

import com.google.common.base.Joiner;
import static com.google.common.base.Preconditions.checkNotNull;
import java.text.ParseException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.logging.Logger;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;

/**
 * A RhythmParameter to replace one or more RhythmVoice phrases by custom phrases.
 */
public class RP_SYS_CustomPhraseValue
{

    private Rhythm rhythm;
    private Map<RhythmVoice, SizedPhrase> mapRvSizedPhrase = new HashMap<>();
    private static final Logger LOGGER = Logger.getLogger(RP_SYS_CustomPhraseValue.class.getSimpleName());


    public RP_SYS_CustomPhraseValue(Rhythm r)
    {
        checkNotNull(r);
        rhythm = r;
    }

    /**
     * Create a deep copy of the specified value.
     *
     * @param value
     */
    public RP_SYS_CustomPhraseValue(RP_SYS_CustomPhraseValue value)
    {
        this(value.getRhythm());
        for (var rv : value.getCustomizedRhythmVoices())
        {
            SizedPhrase sp = (SizedPhrase) value.getCustomizedPhrase(rv).clone();
            mapRvSizedPhrase.put(rv, sp);
        }
    }


    /**
     * Create a shallow copy of this RP value with a phrase added for the specified RhythmVoice.
     * <p>
     * If a phrase was already associated for rv, it is replaced.
     * <p>
     * NOTE: Phrases are not cloned.
     *
     * @param rv It must be a RhythmVoice from our rhythm.
     * @param sp
     * @return
     */
    public RP_SYS_CustomPhraseValue getCopyPlus(RhythmVoice rv, SizedPhrase sp)
    {
        checkNotNull(sp);
        if (!rhythm.getRhythmVoices().contains(rv))
        {
            throw new IllegalArgumentException("rhythm=" + rhythm + " rv=" + rv + " sp=" + sp);
        }
        RP_SYS_CustomPhraseValue res = new RP_SYS_CustomPhraseValue(rhythm);
        res.mapRvSizedPhrase = (Map<RhythmVoice, SizedPhrase>) ((HashMap) mapRvSizedPhrase).clone();
        res.mapRvSizedPhrase.put(rv, sp);
        return res;

    }

    /**
     * Create a shallow copy of this RP value with a phrase removed for the specified RhythmVoice.
     * <p>
     * NOTE: Phrases are not cloned.
     *
     * @param minusRv
     * @return
     */
    public RP_SYS_CustomPhraseValue getCopyMinus(RhythmVoice minusRv)
    {
        RP_SYS_CustomPhraseValue res = new RP_SYS_CustomPhraseValue(rhythm);
//        res.mapRvPhrase = (Map<RhythmVoice, SptPhrase>) ((HashMap) mapRvPhrase).clone();
        res.mapRvSizedPhrase = (Map<RhythmVoice, SizedPhrase>) ((HashMap) mapRvSizedPhrase).clone();
        res.mapRvSizedPhrase.remove(minusRv);
        return res;
    }

    /**
     * Get the Rhythm which uses this RhythmParameter instance.
     *
     * @return
     */
    public Rhythm getRhythm()
    {
        return rhythm;
    }

    /**
     * Get a copy of the custom phrase for the specified RhythmVoice.
     *
     * @param rv
     * @return Null if no customized phrase for rv
     */
    public SizedPhrase getCustomizedPhrase(RhythmVoice rv)
    {
        return mapRvSizedPhrase.get(rv).clone();
    }

    /**
     * Get the RhythmVoices for which there is a custom phrase.
     *
     * @return Empty if no custom phrase.
     */
    public Set<RhythmVoice> getCustomizedRhythmVoices()
    {
        return new HashSet<>(mapRvSizedPhrase.keySet());
    }

    /**
     * The list of RhythmVoice names sorted by preferred channel.
     *
     * @return
     */
    public String toDescriptionString()
    {
        List<String> strs = mapRvSizedPhrase.keySet().stream()
                .sorted(Comparator.comparingInt(RhythmVoice::getPreferredChannel))
                .map(rv -> rv.getName())
                .toList();
        return Joiner.on(", ").join(strs);
    }

    /**
     * Save the specified object state as a string.
     * <p>
     * Example "Bass%[SizedPhraseString]&Piano%[SizedPhraseString]" means 2 RhythmVoices/Phrases. "" means no custom phrase.
     *
     * @param v
     * @return
     * @see loadFromString()
     */
    static public String saveAsString(RP_SYS_CustomPhraseValue v)
    {
        StringJoiner joiner = new StringJoiner("&");
        for (RhythmVoice rv : v.getCustomizedRhythmVoices())
        {
            SizedPhrase sp = v.getCustomizedPhrase(rv);
            String s = rv.getName() + "%" + SizedPhrase.saveAsString(sp);
            joiner.add(s);
        }
        return joiner.toString();
    }

    /**
     * Create an object from a string.
     *
     * @param r
     * @param s
     * @return
     * @throws ParseException If s is invalid
     * @see saveAsString(RP_SYS_CustomPhraseValue)
     */
    static public RP_SYS_CustomPhraseValue loadFromString(Rhythm r, String s) throws ParseException
    {
        checkNotNull(s);
        checkNotNull(r);
        RP_SYS_CustomPhraseValue res = new RP_SYS_CustomPhraseValue(r);
        if (s.isBlank())
        {
            return res;
        }

        String strs[] = s.split("&");
        for (String str : strs)
        {
            String subStrs[] = str.split("%");
            if (subStrs.length == 2)
            {
                try
                {
                    RhythmVoice rv = r.getRhythmVoices().stream().filter(rvi -> rvi.getName().equals(subStrs[0])).findAny().orElse(null);
                    if (rv == null)
                    {
                        res = null;
                        break;
                    }

                    SizedPhrase sp = SizedPhrase.loadAsString(subStrs[1]);
                    res.mapRvSizedPhrase.put(rv, sp);

                } catch (IllegalArgumentException ex)
                {
                    res = null;
                    break;
                }
            } else
            {
                res = null;
                break;
            }
        }

        if (res == null)
        {
            throw new IllegalArgumentException("loadAsString() Invalid RP_SYS_CustomPhraseValue string s=" + s);
        }

        return res;
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
        final RP_SYS_CustomPhraseValue other = (RP_SYS_CustomPhraseValue) obj;
        if (!Objects.equals(this.mapRvSizedPhrase, other.mapRvSizedPhrase))
        {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 11 * hash + Objects.hashCode(this.mapRvSizedPhrase);
        return hash;
    }


    @Override
    public String toString()
    {
        return toDescriptionString();
    }

    // ===================================================================================
    // Private methods
    // ===================================================================================   
    // ===================================================================================
    // Inner classes
    // ===================================================================================   


}

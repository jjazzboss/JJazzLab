package org.jjazz.rhythm.api.rhythmparameters;

import static com.google.common.base.Preconditions.checkNotNull;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;

/**
 * A RhythmParameter to replace one or more RhythmVoice phrases by custom phrases.
 */
public class RP_SYS_CustomPhraseValue
{

    private Rhythm rhythm;
    private Map<RhythmVoice, Phrase> mapRvPhrase = new HashMap<>();
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
        for (var rv : value.getRhythmVoices())
        {
            Phrase p = value.getPhrase(rv).clone();
            mapRvPhrase.put(rv, p);
        }
    }


    /**
     * Create a shallow copy of this RP value with a phrase added for the specified RhythmVoice.
     * <p>
     * If a phrase was already associated for rv, it is replaced.
     * <p>
     * NOTE: Phrases are not deeply cloned.
     *
     * @param rv It must be a RhythmVoice from our rhythm.
     * @param p
     * @return
     */
    public RP_SYS_CustomPhraseValue getCopyPlus(RhythmVoice rv, Phrase p)
    {
        checkNotNull(p);
        if (!rhythm.getRhythmVoices().contains(rv))
        {
            throw new IllegalArgumentException("rhythm=" + rhythm + " rv=" + rv + " p=" + p);
        }
        RP_SYS_CustomPhraseValue res = new RP_SYS_CustomPhraseValue(rhythm);
        res.mapRvPhrase = (Map<RhythmVoice, Phrase>) ((HashMap) mapRvPhrase).clone();
        res.mapRvPhrase.put(rv, p);
        return res;

    }

    /**
     * Create a shallow copy of this RP value with a phrase removed for the specified RhythmVoice.
     * <p>
     * NOTE: Phrases are not deeply cloned.
     *
     * @param minusRv
     * @return
     */
    public RP_SYS_CustomPhraseValue getCopyMinus(RhythmVoice minusRv)
    {
        RP_SYS_CustomPhraseValue res = new RP_SYS_CustomPhraseValue(rhythm);
        res.mapRvPhrase = (Map<RhythmVoice, Phrase>) ((HashMap) mapRvPhrase).clone();
        res.mapRvPhrase.remove(minusRv);
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
     * Get the custom phrase for the specified RhythmVoice.
     *
     * @param rv
     * @return Can be null
     */
    public Phrase getPhrase(RhythmVoice rv)
    {
        return mapRvPhrase.get(rv);
    }

    /**
     * Get the RhythmVoices for which there is a custom phrase.
     *
     * @return Empty if no custom phrase.
     */
    public Set<RhythmVoice> getRhythmVoices()
    {
        return mapRvPhrase.keySet();
    }

    public String toDescriptionString()
    {
        return mapRvPhrase.keySet().toString();
    }

    /**
     * Save the specified object state as a string.
     * <p>
     * Example "Bass%[PhraseString]&Piano%[PhraseString]" means 2 RhythmVoices/Phrases. "" means no custom phrase.
     *
     * @param v
     * @return
     * @see loadFromString()
     */
    static public String saveAsString(RP_SYS_CustomPhraseValue v)
    {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (RhythmVoice rv : v.getRhythmVoices())
        {
            Phrase p = v.getPhrase(rv);
            sb.append(rv.getName())
                    .append("%")
                    .append(Phrase.saveAsString(p));
            if (first)
            {
                sb.append("&");
            }
        }
        return sb.toString();
    }

    /**
     * Create an object from a string.
     *
     * @param r
     * @param s
     * @return
     * @throws IllegalArgumentException If s is invalid
     * @see saveAsString(RP_SYS_CustomPhraseValue)
     */
    static public RP_SYS_CustomPhraseValue loadFromString(Rhythm r, String s) throws IllegalArgumentException
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
            String strs2[] = str.split("%");
            if (strs2.length == 2)
            {
                try
                {
                    RhythmVoice rv = r.getRhythmVoices().stream().filter(rvi -> rvi.getName().equals(strs2[0])).findAny().orElse(null);
                    if (rv == null)
                    {
                        res = null;
                        break;
                    }

                    Phrase p = Phrase.loadAsString(strs2[1]);
                    res.mapRvPhrase.put(rv, p);

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
        if (!Objects.equals(this.mapRvPhrase, other.mapRvPhrase))
        {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 11 * hash + Objects.hashCode(this.mapRvPhrase);
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
}

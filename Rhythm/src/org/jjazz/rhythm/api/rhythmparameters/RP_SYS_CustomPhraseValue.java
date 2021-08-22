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


    /**
     * No custom phrase.
     */
    public RP_SYS_CustomPhraseValue(Rhythm r)
    {
        checkNotNull(r);
        rhythm = r;
    }


    /**
     * Create a copy of this RP value with a phrase added for the specified RhythmVoice.
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
     * Create a copy of this RP value with a phrase removed for the specified RhythmVoice.
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

    public Rhythm getRhythm()
    {
        return rhythm;
    }

    public Phrase getPhrase(RhythmVoice rv)
    {
        return mapRvPhrase.get(rv);
    }

    public Set<RhythmVoice> getRhythmVoices()
    {
        return mapRvPhrase.keySet();
    }

    public String toDescriptionString()
    {
        StringBuilder sb = new StringBuilder();


        return sb.toString();
    }

    /**
     * Save the specified object state as a string.
     *
     * @param v
     * @return
     * @see loadFromString()
     */
    static public String saveAsString(RP_SYS_CustomPhraseValue v)
    {
        return 
    }

    /**
     * Create an object from a string.
     *
     * @param s
     * @return
     * @see saveAsString()
     */
    static public RP_SYS_CustomPhraseValue loadFromString(String s)
    {
        String[] strs = s.split(",");
        RP_SYS_CustomPhraseValue res = new RP_SYS_CustomPhraseValue();
        if (strs.length == 7)
        {
            try
            {

            } catch (NumberFormatException ex)
            {
                LOGGER.severe("loadFromString() ex=" + ex.getMessage());
            }
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

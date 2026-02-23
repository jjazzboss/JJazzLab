package org.jjazz.rhythmparameters.api;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
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
import org.jjazz.phrase.api.Phrase;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;

/**
 * The value of RP_SYS_CustomPhrase.
 * <p>
 * All custom phrases start at beat 0.
 */
public class RP_SYS_CustomPhraseValue
{

    private Rhythm rhythm;
    private final Map<RhythmVoice, Phrase> mapRvPhrase;
    private static final Logger LOGGER = Logger.getLogger(RP_SYS_CustomPhraseValue.class.getSimpleName());


    /**
     * Create an empty value.
     *
     * @param r
     */
    public RP_SYS_CustomPhraseValue(Rhythm r)
    {
        this(r, new HashMap<>());
    }

    /**
     * Create a value with custom phrases.
     *
     * @param r
     * @param mapRvPhrase The custom phrases. Can be empty. The instance will keep deep copies of the phrases.
     */
    public RP_SYS_CustomPhraseValue(Rhythm r, Map<RhythmVoice, Phrase> mapRvPhrase)
    {
        rhythm = r;
        this.mapRvPhrase = new HashMap<>();
        for (var rv : mapRvPhrase.keySet())
        {
            Phrase p = mapRvPhrase.get(rv);
            if (p == null)
            {
                throw new IllegalArgumentException("mapRvPhrase=" + mapRvPhrase);
            }
            this.mapRvPhrase.put(rv, p.clone());
        }
    }

    /**
     * Create a deep copy.
     *
     * @param value
     */
    public RP_SYS_CustomPhraseValue(RP_SYS_CustomPhraseValue value)
    {
        rhythm = value.rhythm;
        mapRvPhrase = new HashMap<>();
        
        for (var rv : value.getCustomizedRhythmVoices())
        {
            var p = value.getCustomizedPhrase(rv);
            mapRvPhrase.put(rv, p);
        }
    }


    /**
     * Get a new instance with the specified customized phrase set.
     *
     * @param rv Must belong to the rhythm
     * @param p  Phrase must be beat-0 based.
     * @return
     */
    public RP_SYS_CustomPhraseValue setCustomizedPhrase(RhythmVoice rv, Phrase p)
    {
        Preconditions.checkArgument(rhythm.getRhythmVoices().contains(rv), "rhythm=%s rv=%s", rhythm, rv);
        Preconditions.checkNotNull(p);

        var newMapRvPhrase = new HashMap<>(mapRvPhrase);
        newMapRvPhrase.put(rv, p);
        return new RP_SYS_CustomPhraseValue(rhythm, newMapRvPhrase);
    }

    /**
     * Get a new instance with a customized phrase removed.
     *
     * @param rv The RhythmVoice to be removed
     * @return
     */
    public RP_SYS_CustomPhraseValue removeCustomizedPhrase(RhythmVoice rv)
    {
        Objects.requireNonNull(rv);

        var newMapRvPhrase = new HashMap<>(mapRvPhrase);
        newMapRvPhrase.remove(rv);
        return new RP_SYS_CustomPhraseValue(rhythm, newMapRvPhrase);
    }

    public RP_SYS_CustomPhrase getRhythmParameter()
    {
        var rp = RP_SYS_CustomPhrase.getCustomPhraseRp(rhythm);
        assert rp != null : "rhythm=" + rhythm;
        return rp;
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
     * Get a deep copy of the custom phrase for the specified RhythmVoice.
     *
     * @param rv
     * @return Null if no customized phrase for rv. Phrase starts at beat 0.
     */
    public Phrase getCustomizedPhrase(RhythmVoice rv)
    {
        Objects.requireNonNull(rv);
        Phrase p = mapRvPhrase.get(rv);
        return p == null ? null : p.clone();
    }

    /**
     * Get the RhythmVoices for which there is a custom phrase.
     *
     * @return Empty set if no custom phrase.
     */
    public Set<RhythmVoice> getCustomizedRhythmVoices()
    {
        return new HashSet<>(mapRvPhrase.keySet());
    }

    /**
     * The list of RhythmVoice names sorted by preferred channel.
     *
     * @return
     */
    public String toDescriptionString()
    {
        List<String> strs = mapRvPhrase.keySet().stream()
                .sorted(Comparator.comparingInt(RhythmVoice::getPreferredChannel))
                .map(rv -> rv.getName())
                .toList();
        return Joiner.on(", ").join(strs);
    }

    /**
     * Save the specified object state as a string.
     * <p>
     * Example "Bass%[PhraseString]&amp;Piano%[PhraseString]" means 2 RhythmVoices/Phrases. "" means no custom phrase.
     *
     * @param v
     * @return
     * @see #loadFromString(org.jjazz.rhythm.api.Rhythm, java.lang.String)
     */
    static public String saveAsString(RP_SYS_CustomPhraseValue v)
    {
        StringJoiner joiner = new StringJoiner("&");
        for (RhythmVoice rv : v.getCustomizedRhythmVoices())
        {
            Phrase p = v.getCustomizedPhrase(rv);
            String s = rv.getName() + "%" + Phrase.saveAsString(p);
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
     * @see #saveAsString(RP_SYS_CustomPhraseValue)
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

                    Phrase p;
                    String pStr = subStrs[1];

                    // Backward compatibility HACK: up to JJazzLab 3.2.1, we used SizedPhrase instead of Phrase
                    if (isSizedPhraseSaveString(pStr))
                    {
                        SizedPhrase sp = SizedPhrase.loadAsString(pStr);
                        p = new Phrase(sp.getChannel());
                        p.add(sp);
                    } else
                    {
                        p = Phrase.loadAsString(pStr);
                    }

                    res.setCustomizedPhrase(rv, p);

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
    /**
     * Check is saveString is for a SizedPhrase or a simple phrase.
     *
     * @param saveString
     * @return
     */
    private static boolean isSizedPhraseSaveString(String saveString)
    {
        // PHRASE: "[8|NoteEventStr0|NoteEventStr1]" 
        // SIZED_PHRASE: "[8|12.0|16.0|4/4|NoteEventStr0|NoteEventStr1]"
        String[] strs = saveString.split("\\|");
        assert strs.length > 1 : "saveString=" + saveString;
        return strs.length >= 4 && strs[3].contains("/");   // Check that 4th cell is a time signature
    }

    // ===================================================================================
    // Inner classes
    // ===================================================================================   

}

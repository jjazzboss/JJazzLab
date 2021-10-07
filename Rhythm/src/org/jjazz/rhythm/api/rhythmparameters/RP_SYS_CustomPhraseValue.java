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
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import org.openide.util.Exceptions;

/**
 * A RhythmParameter to replace one or more RhythmVoice phrases by custom phrases.
 */
public class RP_SYS_CustomPhraseValue
{

    private Rhythm rhythm;
    private Map<RhythmVoice, SptPhrase> mapRvPhrase = new HashMap<>();
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
            SptPhrase p = (SptPhrase) value.getCustomizedPhrase(rv).clone();
            mapRvPhrase.put(rv, p);
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
    public RP_SYS_CustomPhraseValue getCopyPlus(RhythmVoice rv, SptPhrase sp)
    {
        checkNotNull(sp);
        if (!rhythm.getRhythmVoices().contains(rv))
        {
            throw new IllegalArgumentException("rhythm=" + rhythm + " rv=" + rv + " sp=" + sp);
        }
        RP_SYS_CustomPhraseValue res = new RP_SYS_CustomPhraseValue(rhythm);
        res.mapRvPhrase = (Map<RhythmVoice, SptPhrase>) ((HashMap) mapRvPhrase).clone();
        res.mapRvPhrase.put(rv, sp);
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
        res.mapRvPhrase = (Map<RhythmVoice, SptPhrase>) ((HashMap) mapRvPhrase).clone();
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
     * Get a copy of the custom phrase for the specified RhythmVoice.
     *
     * @param rv
     * @return Null if no customized phrase for rv
     */
    public SptPhrase getCustomizedPhrase(RhythmVoice rv)
    {
        return mapRvPhrase.get(rv).clone();
    }

    /**
     * Get the RhythmVoices for which there is a custom phrase.
     *
     * @return Empty if no custom phrase.
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
                .collect(Collectors.toList());
        return Joiner.on(",").join(strs);
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
        for (RhythmVoice rv : v.getCustomizedRhythmVoices())
        {
            Phrase p = v.getCustomizedPhrase(rv);
            sb.append(rv.getName())
                    .append("%")
                    .append(Phrase.saveAsString(p));   PROBLEM ! Use SptPhrase ??
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

                    SptPhrase p = SptPhrase.loadAsString(strs2[1]);
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
    // public  classes
    // ===================================================================================  
    /**
     * A phrase with 2 additional fields.
     */
    static public class SptPhrase extends Phrase
    {

        private final float sizeInBeats;
        private final TimeSignature timeSignature;

        public SptPhrase(int channel, float nbBeats, TimeSignature ts)
        {
            super(channel);
            sizeInBeats = nbBeats;
            timeSignature = ts;
        }

        public SptPhrase(Phrase p, float nbBeats, TimeSignature ts)
        {
            super(p.getChannel());
            add(p);
            sizeInBeats = nbBeats;
            timeSignature = ts;
        }


        @Override
        public SptPhrase clone()
        {
            Phrase p = super.clone();
            return new SptPhrase(p, getSizeInBeats(), getTimeSignature());
        }

        /**
         * @return the sizeInBeats
         */
        public float getSizeInBeats()
        {
            return sizeInBeats;
        }

        /**
         * @return the timeSignature
         */
        public TimeSignature getTimeSignature()
        {
            return timeSignature;
        }


        /**
         * Save the specified SptPhrase as a string.
         * <p>
         * Example "[8|12|4/4|NoteEventStr0|NoteEventStr1]" means a Phrase for channel 8, size=12 natural beats, in 4/4, with 2
         * NoteEvents.
         *
         * @param sp
         * @return
         * @see loadAsString(String)
         */
        static public String saveAsString(SptPhrase sp)
        {
            StringBuilder sb = new StringBuilder();
            String delimiter = "|";
            sb.append("[");
            sb.append(sp.getChannel()).append(delimiter);
            sb.append(sp.getSizeInBeats()).append(delimiter);
            sb.append(sp.getTimeSignature()).append(delimiter);
            boolean first = true;
            for (NoteEvent ne : sp)
            {
                if (first)
                {
                    first = false;
                } else
                {
                    sb.append(delimiter);
                }
                sb.append(NoteEvent.saveAsString(ne));
            }
            sb.append("]");
            return sb.toString();
        }

        /**
         * Create a SptPhrase from the specified string.
         * <p>
         *
         * @param s
         * @return
         * @throws IllegalArgumentException If s is not a valid string.
         * @see saveAsString(SptPhrase)
         */
        static public SptPhrase loadAsString(String s) throws IllegalArgumentException
        {
            SptPhrase sp = null;
            if (s.length() >= 10 && s.charAt(0) == '[' && s.charAt(s.length() - 1) == ']')    // minimum string is e.g. [2|1|4/4|]
            {
                String[] strs = s.substring(1, s.length() - 1).split("\\|");
                if (strs.length >= 3)
                {
                    try
                    {
                        int channel = Integer.parseInt(strs[0]);
                        float sizeInBeats = Float.parseFloat(strs[1]);
                        TimeSignature ts = TimeSignature.parse(strs[2]);
                        sp = new SptPhrase(channel, sizeInBeats, ts);
                        for (int i = 3; i < strs.length; i++)
                        {
                            NoteEvent ne = NoteEvent.loadAsString(strs[i]);
                            sp.addOrdered(ne);
                        }
                    } catch (IllegalArgumentException | ParseException ex)
                    {
                        // Nothing
                        LOGGER.warning("SptPhrase.loadAsString() Invalid string s=" + s + ", ex=" + ex.getMessage());
                    }
                }
            }

            if (sp == null)
            {
                throw new IllegalArgumentException("SptPhrase.loadAsString() Invalid SptPhrase string s=" + s);
            }
            return sp;
        }

    }
    // ===================================================================================
    // Private methods
    // ===================================================================================   
    // ===================================================================================
    // Private classes
    // ===================================================================================   


}

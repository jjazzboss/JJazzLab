package org.jjazz.phrasetransform.api.rps;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.phrasetransform.api.DrumsMixTransform;
import org.jjazz.phrasetransform.api.PhraseTransformChain;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.api.RhythmVoice.Type;

/**
 * A RhythmParameter value to transform a drums phrase.
 * <p>
 * Always contains a DrumsMixTransform() at last position in the transform chain.
 */
public class RP_SYS_DrumsTransformValue
{

    private final RhythmVoice rhythmVoice;
    private PhraseTransformChain transformChain;

    private static final Logger LOGGER = Logger.getLogger(RP_SYS_DrumsTransformValue.class.getSimpleName());

    public RP_SYS_DrumsTransformValue(RhythmVoice rv)
    {
        checkNotNull(rv);
        checkArgument(rv.getContainer() != null
                && rv.getType().equals(Type.DRUMS),
                "rv=%s", rv);
        rhythmVoice = rv;
        transformChain = new PhraseTransformChain(Arrays.asList(new DrumsMixTransform()));
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 53 * hash + Objects.hashCode(this.rhythmVoice);
        hash = 53 * hash + Objects.hashCode(this.transformChain);
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
        final RP_SYS_DrumsTransformValue other = (RP_SYS_DrumsTransformValue) obj;
        if (!Objects.equals(this.rhythmVoice, other.rhythmVoice))
        {
            return false;
        }
        return Objects.equals(this.transformChain, other.transformChain);
    }

    /**
     * Create a deep copy of the specified value.
     *
     * @param value
     */
    public RP_SYS_DrumsTransformValue(RP_SYS_DrumsTransformValue value)
    {
        this(value.rhythmVoice);
        transformChain = value.transformChain.deepClone();
    }

    public Rhythm getRhythm()
    {
        return rhythmVoice.getContainer();
    }

    public RhythmVoice getRhythmVoice()
    {
        return rhythmVoice;
    }

    /**
     * Return a copy of this object but with the specified chain.
     *
     * @param chain If null, the returned copy will use the default value. If not null, the last PhraseTransform must be a
     *              DrumsMixTransform instance.
     * @return
     */
    public RP_SYS_DrumsTransformValue getCopy(PhraseTransformChain chain)
    {
        checkArgument(chain == null || (!chain.isEmpty() && chain.get(chain.size() - 1) instanceof DrumsMixTransform), "chain=%s", chain);
        var res = new RP_SYS_DrumsTransformValue(rhythmVoice);
        res.transformChain = chain == null ? new PhraseTransformChain(Arrays.asList(new DrumsMixTransform())) : chain.deepClone();
        return res;
    }

    /**
     * Get a copy of the chain associated to rv.
     *
     * @param excludeDrumsMixTransform If true do not include in the return value the DrumsMixTransform at last position
     * @return Can't be null
     */
    public PhraseTransformChain getTransformChain(boolean excludeDrumsMixTransform)
    {
        var res = transformChain.deepClone();
        if (excludeDrumsMixTransform && !res.isEmpty())
        {
            res.remove(res.size() - 1);
        }
        return res;
    }

    public DrumsMixTransform getDrumsMixTransform()
    {
        var pt = transformChain.get(transformChain.size() - 1);
        if (!(pt instanceof DrumsMixTransform))
        {
            throw new IllegalStateException("Missing DrumsMixTransform at last position. transformChain=" + transformChain);
        }
        return (DrumsMixTransform) pt;
    }


    public String toDescriptionString()
    {
        StringJoiner joiner = new StringJoiner(">");
        joiner.add(getDrumsMixTransform().getPropertiesDisplayString());
        getTransformChain(true).forEach(pt -> joiner.add(pt.getInfo().getName()));
        return joiner.toString();
    }

    /**
     * Save the specified object state as a string.
     * <p>
     *
     * @param v
     * @return
     *
     *  @see #loadFromString(org.jjazz.rhythm.api.RhythmVoice, java.lang.String) 
     * @see PhraseTransformChain#saveAsString(org.jjazz.phrasetransform.api.PhraseTransformChain)
     */
    static public String saveAsString(RP_SYS_DrumsTransformValue v)
    {
        return PhraseTransformChain.saveAsString(v.transformChain);
    }

    /**
     * Create an object from a string.
     *
     * @param rv Must have a container defined and be of RhythmVoice.Type.DRUMS
     * @param s  Example "[uniqueId1#prop1=value1,prop2=value2|uniqueId2#|uniqueId3#prop1=value1]"
     * @return Can be null
     * @see #saveAsString(org.jjazz.phrasetransform.api.rps.RP_SYS_DrumsTransformValue)
     * @see PhraseTransformChain#loadFromString(java.lang.String)
     */
    static public RP_SYS_DrumsTransformValue loadFromString(RhythmVoice rv, String s)
    {
        checkNotNull(s);
        checkArgument(rv.getContainer() != null
                && rv.getType().equals(Type.DRUMS),
                "rv=%s", rv);

        RP_SYS_DrumsTransformValue res = new RP_SYS_DrumsTransformValue(rv);
        if (s.isBlank())
        {
            return res;
        }


        try
        {
            res.transformChain = PhraseTransformChain.loadFromString(s);            // Throws ParseException


            // Check consistency of loaded data => fix regression Issue #309 introduced in 3.2 by changing the position of the DrumsMixInstance (first then last)            
            var lastPt = !res.transformChain.isEmpty() ? res.transformChain.get(res.transformChain.size() - 1) : null;
            if (!(lastPt instanceof DrumsMixTransform))
            {
                // If DrumsMix is at first position then fix it
                var firstPt = !res.transformChain.isEmpty() ? res.transformChain.get(0) : null;
                if (firstPt instanceof DrumsMixTransform)
                {
                    res.transformChain.remove(0);
                    res.transformChain.add(firstPt);
                } else
                {
                    throw new ParseException("loadFromString() Missing first or final DrumsMixTransform, can't build RP_SYS_DrumsTransformValue instance for rv=" + rv.getName() + " from s=" + s, 0);
                }
            }
        } catch (ParseException ex)
        {
            LOGGER.log(Level.WARNING, "loadFromString() {0}", ex.getMessage());
            res = null;
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

package org.jjazz.phrasetransform.api.rps;

import com.google.common.base.Preconditions;
import static com.google.common.base.Preconditions.checkNotNull;
import java.util.logging.Logger;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_CustomPhraseValue;
import org.jjazz.util.api.ResUtil;

/**
 * A RhythmParameter to transform generated phrases.
 * <p>
 */
public class RP_SYS_PhraseTransform implements RhythmParameter<RP_SYS_PhraseTransformValue>
{

    private final RP_SYS_PhraseTransformValue DEFAULT_VALUE;
    private final Rhythm rhythm;
    private static final Logger LOGGER = Logger.getLogger(RP_SYS_PhraseTransform.class.getSimpleName());

    public RP_SYS_PhraseTransform(Rhythm r)
    {
        checkNotNull(r);
        rhythm = r;
        DEFAULT_VALUE = new RP_SYS_PhraseTransformValue(rhythm);
    }

    public Rhythm getRhythm()
    {
        return rhythm;
    }

    @Override
    public String getId()
    {
        return "RP_SYS_PhraseTransformId";
    }

    @Override
    public String getDisplayName()
    {
        return ResUtil.getString(getClass(), "RP_SYS_PhraseTransformName");
    }

    @Override
    public String getDescription()
    {
        return ResUtil.getString(getClass(), "RP_SYS_PhraseTransformDesc");
    }

    @Override
    public String getValueDescription(RP_SYS_PhraseTransformValue value)
    {
        return null;
    }

    @Override
    public RP_SYS_PhraseTransformValue getDefaultValue()
    {
        return DEFAULT_VALUE;
    }

    @Override
    public String saveAsString(RP_SYS_PhraseTransformValue v)
    {
        return RP_SYS_PhraseTransformValue.saveAsString(v);
    }

    @Override
    public RP_SYS_PhraseTransformValue loadFromString(String s)
    {
        return RP_SYS_PhraseTransformValue.loadFromString(rhythm, s);
    }

    @Override
    public boolean isValidValue(RP_SYS_PhraseTransformValue value)
    {
        return value instanceof RP_SYS_PhraseTransformValue;
    }

    @Override
    public RP_SYS_PhraseTransformValue cloneValue(RP_SYS_PhraseTransformValue value)
    {
        return new RP_SYS_PhraseTransformValue(value);
    }

    @Override
    public String toString()
    {
        return "RP_SYS_PhraseTransform[rhythm=" + rhythm + "]";
    }

    @Override
    public boolean isCompatibleWith(RhythmParameter<?> rp)
    {
        return rp instanceof RP_SYS_PhraseTransform && rp.getId().equals(getId());
    }

    /**
     * Reuse each value TransformPhrase for which there is a matching RhythmVoice (same type).
     *
     * @param <T>
     * @param rp
     * @param value
     * @return
     */
    @Override
    public <T> RP_SYS_PhraseTransformValue convertValue(RhythmParameter<T> rp, T value)
    {
        Preconditions.checkArgument(isCompatibleWith(rp), "rp=%s is not compatible with this=%s", rp, this);
        Preconditions.checkNotNull(value);

        RP_SYS_PhraseTransformValue res = getDefaultValue();

        RP_SYS_PhraseTransformValue rpValue = (RP_SYS_PhraseTransformValue) value;
        var rvs = getRhythm().getRhythmVoices();


        for (RhythmVoice rpRv : rpValue.getChainRhythmVoices())
        {
            RhythmVoice rv = rvs.stream()
                    .filter(rvi -> rvi.getType().equals(rpRv.getType()))
                    .findAny().orElse(null);
            if (rv != null)
            {
                var chain = rpValue.getTransformChain(rpRv);
                res = res.getUpdatedTransformChain(rv, chain);
                rvs.remove(rv);     // Do not reuse this phrase
            }
        }

        return res;
    }

    @Override
    public String getDisplayValue(RP_SYS_PhraseTransformValue value)
    {
        return value.toString();
    }

    /**
     * Find the first RP_SYS_PhraseTransform instance in the rhythm parameters of r.
     *
     * @param r
     * @return Can be null if not found
     */
    static public RP_SYS_PhraseTransform getPhraseTransformRp(Rhythm r)
    {
        checkNotNull(r);
        return (RP_SYS_PhraseTransform) r.getRhythmParameters()
                .stream()
                .filter(rp -> (rp instanceof RP_SYS_PhraseTransform))
                .findAny()
                .orElse(null);
    }


}

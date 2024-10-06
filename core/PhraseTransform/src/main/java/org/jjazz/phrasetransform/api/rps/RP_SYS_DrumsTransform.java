package org.jjazz.phrasetransform.api.rps;

import com.google.common.base.Preconditions;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import java.util.logging.Logger;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.api.RhythmVoice.Type;
import org.jjazz.utilities.api.ResUtil;

/**
 * A RhythmParameter to transform a drums phrase.
 * <p>
 */
public class RP_SYS_DrumsTransform implements RhythmParameter<RP_SYS_DrumsTransformValue>
{

    private final RP_SYS_DrumsTransformValue DEFAULT_VALUE;
    private final RhythmVoice rhythmVoice;
    private final boolean primary;
    private static final Logger LOGGER = Logger.getLogger(RP_SYS_DrumsTransform.class.getSimpleName());

    /**
     *
     * @param rv      Must have a Rhythm container defined and type==RhythmVoice.Type.DRUMS
     * @param primary
     */
    public RP_SYS_DrumsTransform(RhythmVoice rv, boolean primary)
    {
        checkNotNull(rv);
        checkArgument(rv.getContainer() != null
                && rv.getType() == Type.DRUMS,
                "rv=%s", rv);

        rhythmVoice = rv;
        this.primary = primary;
        DEFAULT_VALUE = new RP_SYS_DrumsTransformValue(rhythmVoice);
    }

    @Override
    public RP_SYS_DrumsTransform getCopy(Rhythm r)
    {
        var rv = r.getRhythmVoices().stream()
                .filter(rvi -> rvi.getType() == Type.DRUMS)
                .findAny()
                .orElseThrow();
        return new RP_SYS_DrumsTransform(rv, primary);
    }

    public Rhythm getRhythm()
    {
        return rhythmVoice.getContainer();
    }

    @Override
    public boolean isPrimary()
    {
        return primary;
    }

    public RhythmVoice getRhythmVoice()
    {
        return rhythmVoice;
    }

    @Override
    public String getId()
    {
        return "RP_SYS_DrumsTransformId";
    }

    @Override
    public String getDisplayName()
    {
        return ResUtil.getString(getClass(), "RP_SYS_DrumsTransformName");
    }

    @Override
    public String getDescription()
    {
        return ResUtil.getString(getClass(), "RP_SYS_DrumsTransformDesc");
    }

    @Override
    public String getValueDescription(RP_SYS_DrumsTransformValue value)
    {
        return null;
    }

    @Override
    public RP_SYS_DrumsTransformValue getDefaultValue()
    {
        return new RP_SYS_DrumsTransformValue(DEFAULT_VALUE);
    }

    @Override
    public String saveAsString(RP_SYS_DrumsTransformValue v)
    {
        return RP_SYS_DrumsTransformValue.saveAsString(v);
    }

    @Override
    public RP_SYS_DrumsTransformValue loadFromString(String s)
    {
        return RP_SYS_DrumsTransformValue.loadFromString(rhythmVoice, s);
    }

    @Override
    public boolean isValidValue(RP_SYS_DrumsTransformValue value)
    {
        return value instanceof RP_SYS_DrumsTransformValue;
    }

    @Override
    public RP_SYS_DrumsTransformValue cloneValue(RP_SYS_DrumsTransformValue value)
    {
        return new RP_SYS_DrumsTransformValue(value);
    }

    @Override
    public String toString()
    {
        return "RP_SYS_DrumsTransformValue[rhythmVoice=" + rhythmVoice + "]";
    }

    @Override
    public boolean isCompatibleWith(RhythmParameter<?> rp)
    {
        return rp instanceof RP_SYS_DrumsTransform && rp.getId().equals(getId());
    }


    @Override
    public <T> RP_SYS_DrumsTransformValue convertValue(RhythmParameter<T> rp, T rpValue)
    {
        Preconditions.checkArgument(isCompatibleWith(rp), "rp=%s is not compatible with this=%s", rp, this);
        Preconditions.checkNotNull(rpValue);

        RP_SYS_DrumsTransformValue rpDrumsValue = (RP_SYS_DrumsTransformValue) rpValue;
        RP_SYS_DrumsTransformValue res = getDefaultValue().getCopy(rpDrumsValue.getTransformChain(false));

        return res;
    }

    @Override
    public String getDisplayValue(RP_SYS_DrumsTransformValue value)
    {
        return value.toString();
    }

    /**
     * Find the first RP_SYS_DrumsTransform instance in the rhythm parameters of r.
     *
     * @param r
     * @return Can be null if not found
     */
    static public RP_SYS_DrumsTransform getDrumsTransformRp(Rhythm r)
    {
        checkNotNull(r);
        return (RP_SYS_DrumsTransform) r.getRhythmParameters()
                .stream()
                .filter(rp -> (rp instanceof RP_SYS_DrumsTransform))
                .findAny()
                .orElse(null);
    }


}

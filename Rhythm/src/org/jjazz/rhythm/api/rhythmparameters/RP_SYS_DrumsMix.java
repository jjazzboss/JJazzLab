package org.jjazz.rhythm.api.rhythmparameters;

import com.google.common.base.Preconditions;
import java.util.logging.Logger;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.util.api.ResUtil;

/**
 * A RhythmParameter to increase/decrease individual pitches in a drums track (snare, bass drum, toms, etc.).
 * <p>
 */
public class RP_SYS_DrumsMix implements RhythmParameter<RP_SYS_DrumsMixValue>
{

    private static final RP_SYS_DrumsMixValue DEFAULT_VALUE = new RP_SYS_DrumsMixValue();
    private RhythmVoice rhythmVoice;
    private static final Logger LOGGER = Logger.getLogger(RP_SYS_DrumsMix.class.getSimpleName());

    public RP_SYS_DrumsMix(RhythmVoice rv)
    {
        if (rv == null)
        {
            throw new NullPointerException("rv");
        }
        rhythmVoice = rv;
    }

    public RhythmVoice getRhythmVoice()
    {
        return rhythmVoice;
    }

    @Override
    public String getId()
    {
        return "RP_SYS_DrumsMixId";
    }

    @Override
    public String getDisplayName()
    {
        return ResUtil.getString(getClass(), "RP_SYS_DrumsMixName");
    }

    @Override
    public String getDescription()
    {
        return ResUtil.getString(getClass(), "RP_SYS_DrumsMixDesc");
    }

    @Override
    public String getValueDescription(RP_SYS_DrumsMixValue value)
    {
        return null;
    }

    @Override
    public RP_SYS_DrumsMixValue getDefaultValue()
    {
        return DEFAULT_VALUE;
    }

    @Override
    public String saveAsString(RP_SYS_DrumsMixValue v)
    {
        return RP_SYS_DrumsMixValue.saveAsString(v);
    }

    @Override
    public RP_SYS_DrumsMixValue loadFromString(String s)
    {
        return RP_SYS_DrumsMixValue.loadFromString(s);
    }

    @Override
    public boolean isValidValue(RP_SYS_DrumsMixValue value)
    {
        return value instanceof RP_SYS_DrumsMixValue;
    }

    @Override
    public RP_SYS_DrumsMixValue cloneValue(RP_SYS_DrumsMixValue value)
    {
        return new RP_SYS_DrumsMixValue(value);
    }

    @Override
    public String toString()
    {
        return "RP_SYS_DrumsMix[rhythmVoice=" + rhythmVoice + "]";
    }

    @Override
    public boolean isCompatibleWith(RhythmParameter<?> rp)
    {
        return rp instanceof RP_SYS_DrumsMix && rp.getId().equals(getId());
    }

    @Override
    public <T> RP_SYS_DrumsMixValue convertValue(RhythmParameter<T> rp, T value)
    {
        Preconditions.checkArgument(isCompatibleWith(rp), "rp=%s is not compatible with this=%s", rp, this);
        Preconditions.checkNotNull(value);
        return (RP_SYS_DrumsMixValue) value;
    }

    @Override
    public String getDisplayValue(RP_SYS_DrumsMixValue value)
    {
        return value.toString();
    }

    /**
     * Find the first RP_SYS_DrumsMix instance in the rhythm parameters of r.
     *
     * @param rhythm
     * @return Can be null if not found
     */
    static public RP_SYS_DrumsMix getDrumsMixRp(Rhythm rhythm)
    {
        if (rhythm == null)
        {
            throw new NullPointerException("r");   //NOI18N
        }
        return (RP_SYS_DrumsMix) rhythm.getRhythmParameters()
                .stream()
                .filter(rp -> (rp instanceof RP_SYS_DrumsMix))
                .findAny()
                .orElse(null);
    }



}

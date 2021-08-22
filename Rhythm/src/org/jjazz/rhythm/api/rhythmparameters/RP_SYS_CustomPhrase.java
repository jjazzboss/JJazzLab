package org.jjazz.rhythm.api.rhythmparameters;

import java.util.logging.Logger;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.util.api.ResUtil;

/**
 * A RhythmParameter to
 * <p>
 */
public class RP_SYS_CustomPhrase implements RhythmParameter<RP_SYS_CustomPhraseValue>
{

    private static final RP_SYS_CustomPhraseValue DEFAULT_VALUE = new RP_SYS_CustomPhraseValue();
    private RhythmVoice rhythmVoice;
    private static final Logger LOGGER = Logger.getLogger(RP_SYS_CustomPhrase.class.getSimpleName());


    @Override
    public String getId()
    {
        return "RP_SYS_CustomPhraseId";
    }

    @Override
    public String getDisplayName()
    {
        return ResUtil.getString(getClass(), "RP_SYS_CustomPhrase");
    }

    @Override
    public String getDescription()
    {
        return ResUtil.getString(getClass(), "RP_SYS_CustomPhraseDesc");
    }

    @Override
    public String getValueDescription(RP_SYS_CustomPhraseValue value)
    {
        return null;
    }

    @Override
    public RP_SYS_CustomPhraseValue getDefaultValue()
    {
        return DEFAULT_VALUE;
    }

    @Override
    public String valueToString(RP_SYS_CustomPhraseValue v)
    {
        return RP_SYS_CustomPhraseValue.saveAsString(v);
    }

    @Override
    public RP_SYS_CustomPhraseValue stringToValue(String s)
    {
        return RP_SYS_CustomPhraseValue.loadFromString(s);
    }

    @Override
    public boolean isValidValue(RP_SYS_CustomPhraseValue value)
    {
        return value instanceof RP_SYS_CustomPhraseValue;
    }

    @Override
    public RP_SYS_CustomPhraseValue cloneValue(RP_SYS_CustomPhraseValue value)
    {
        return new RP_SYS_CustomPhraseValue(value);
    }

    @Override
    public String toString()
    {
        return value.toDescriptionString();
    }

    /**
     * Find the first RP_SYS_CustomPhrase instance in the rhythm parameters of r.
     *
     * @param rhythm
     * @return Can be null if not found
     */
    static public RP_SYS_CustomPhrase getCustomPhraseRp(Rhythm rhythm)
    {
        if (rhythm == null)
        {
            throw new NullPointerException("r");   //NOI18N
        }
        return (RP_SYS_CustomPhrase) rhythm.getRhythmParameters()
                .stream()
                .filter(rp -> (rp instanceof RP_SYS_CustomPhrase))
                .findAny()
                .orElse(null);
    }

}

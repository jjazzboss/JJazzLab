package org.jjazz.rhythm.api.rhythmparameters;

import com.google.common.base.Preconditions;
import static com.google.common.base.Preconditions.checkNotNull;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.logging.Logger;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.utilities.api.ResUtil;

/**
 * A RhythmParameter to replace one or more generated Phrases by custom Phrases.
 * <p>
 */
public class RP_SYS_CustomPhrase implements RhythmParameter<RP_SYS_CustomPhraseValue>
{

    private final Rhythm rhythm;
    private final boolean primary;
    private static final Logger LOGGER = Logger.getLogger(RP_SYS_CustomPhrase.class.getSimpleName());

    public RP_SYS_CustomPhrase(Rhythm r, boolean primary)
    {
        checkNotNull(r);
        rhythm = r;
        this.primary = primary;
    }

    @Override
    public RP_SYS_CustomPhrase getCopy(Rhythm r)
    {
        return new RP_SYS_CustomPhrase(r, primary);
    }

    @Override
    public boolean isPrimary()
    {
        return primary;
    }

    /**
     * The rhythm associated to this RhythmParameter.
     *
     * @return
     */
    public Rhythm getRhythm()
    {
        return rhythm;
    }

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
        return new RP_SYS_CustomPhraseValue(rhythm);
    }

    @Override
    public String saveAsString(RP_SYS_CustomPhraseValue v)
    {
        return RP_SYS_CustomPhraseValue.saveAsString(v);
    }

    @Override
    public RP_SYS_CustomPhraseValue loadFromString(String s)
    {
        try
        {
            return RP_SYS_CustomPhraseValue.loadFromString(rhythm, s);
        } catch (ParseException ex)
        {
            return null;
        }
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

    /**
     * Compatible with another RP_SYS_CustomPhrase for the same rhythm time signature.
     *
     * @param rp
     * @return
     */
    @Override
    public boolean isCompatibleWith(RhythmParameter<?> rp)
    {
        if (!(rp instanceof RP_SYS_CustomPhrase) || !rp.getId().equals(getId()))
        {
            return false;
        }
        RP_SYS_CustomPhrase rpCustom = (RP_SYS_CustomPhrase) rp;
        return getRhythm().getTimeSignature().equals(rpCustom.getRhythm().getTimeSignature());
    }

    /**
     * Reuse a custom phrase when there is a matching RhythmVoice (same type).
     *
     * @param <T>
     * @param rp
     * @param value
     * @return
     */
    @Override
    public <T> RP_SYS_CustomPhraseValue convertValue(RhythmParameter<T> rp, T value)
    {
        Preconditions.checkArgument(isCompatibleWith(rp), "rp=%s is not compatible with this=%s", rp, this);
        Preconditions.checkNotNull(value);


        RP_SYS_CustomPhraseValue res = getDefaultValue();


        RP_SYS_CustomPhraseValue rpValue = (RP_SYS_CustomPhraseValue) value;
        var rvs = new ArrayList<>(getRhythm().getRhythmVoices());


        for (RhythmVoice rpRv : rpValue.getCustomizedRhythmVoices())
        {
            RhythmVoice rv = rvs.stream()
                    .filter(rvi -> rvi.getType().equals(rpRv.getType()))
                    .findAny().orElse(null);
            if (rv != null)
            {
                res.setCustomizedPhrase(rv, rpValue.getCustomizedPhrase(rpRv));
                rvs.remove(rv);     // Do not reuse this phrase
            }
        }

        return res;
    }

    @Override
    public String getDisplayValue(RP_SYS_CustomPhraseValue value)
    {
        return value.toString();
    }

    @Override
    public String toString()
    {
        return "RP_SYS_CustomPhrase[rhythm=" + rhythm + "]";
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
            throw new NullPointerException("r");
        }
        return (RP_SYS_CustomPhrase) rhythm.getRhythmParameters()
                .stream()
                .filter(rp -> (rp instanceof RP_SYS_CustomPhrase))
                .findAny()
                .orElse(null);
    }

}

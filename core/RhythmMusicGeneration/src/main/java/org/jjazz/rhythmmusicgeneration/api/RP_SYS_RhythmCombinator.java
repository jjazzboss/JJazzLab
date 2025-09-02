package org.jjazz.rhythmmusicgeneration.api;

import com.google.common.base.Preconditions;
import static com.google.common.base.Preconditions.checkNotNull;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.logging.Logger;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.rhythmmusicgeneration.spi.ConfigurableMusicGeneratorProvider;

/**
 * A RhythmParameter to map some tracks to other tracks.
 * <p>
 */
public class RP_SYS_RhythmCombinator implements RhythmParameter<RP_SYS_RhythmCombinatorValue>
{

    private final RP_SYS_RhythmCombinatorValue DEFAULT_VALUE;
    private final Rhythm baseRhythm;
    private final boolean primary;
    private static final Logger LOGGER = Logger.getLogger(RP_SYS_RhythmCombinator.class.getSimpleName());

    /**
     *
     * @param baseRhythm Must implement the ConfigurableMusicGeneratorProvider interface.
     * @param primary
     */
    public RP_SYS_RhythmCombinator(Rhythm baseRhythm, boolean primary)
    {
        Objects.requireNonNull(baseRhythm);
        Preconditions.checkArgument(baseRhythm instanceof ConfigurableMusicGeneratorProvider, "baseRhythm=%s", baseRhythm);
        this.primary = primary;
        this.baseRhythm = baseRhythm;
        DEFAULT_VALUE = new RP_SYS_RhythmCombinatorValue(baseRhythm);
    }

    /**
     *
     * @param newBaseRhythm Must implement the ConfigurableMusicGeneratorProvider interface.
     * @return
     */
    @Override
    public RP_SYS_RhythmCombinator getCopy(Rhythm newBaseRhythm)
    {
        var res = new RP_SYS_RhythmCombinator(newBaseRhythm, primary);
        return res;
    }

    public ConfigurableMusicGeneratorProvider getConfigurableMusicGeneratorProvider()
    {
        return (ConfigurableMusicGeneratorProvider) baseRhythm;
    }

    public Rhythm getBaseRhythm()
    {
        return baseRhythm;
    }

    @Override
    public boolean isPrimary()
    {
        return primary;
    }

    @Override
    public String getId()
    {
        return "RP_SYS_RhythmCombinator";
    }

    @Override
    public String getDisplayName()
    {
        return "rhythm combinator";
    }

    @Override
    public String getDescription()
    {
        return "Combine tracks from different rhythms";
    }

    /**
     * Generally used as tooltip by the framework.
     *
     * @param value
     * @return
     */
    @Override
    public String getValueDescription(RP_SYS_RhythmCombinatorValue value)
    {
        var joiner = new StringJoiner(", ");
        for (var rv : value.getMappedRhythmVoices())
        {
            var rvDest = value.getDestRhythmVoice(rv);
            joiner.add(rv.getName() + " > " + rvDest.getContainer().getName() + "/" + rv.getName());
        }
        return joiner.toString();
    }

    @Override
    public RP_SYS_RhythmCombinatorValue getDefaultValue()
    {
        return DEFAULT_VALUE;
    }

    @Override
    public String saveAsString(RP_SYS_RhythmCombinatorValue v)
    {
        return RP_SYS_RhythmCombinatorValue.saveAsString(v);
    }

    @Override
    public RP_SYS_RhythmCombinatorValue loadFromString(String s)
    {
        return RP_SYS_RhythmCombinatorValue.loadFromString(s);
    }

    @Override
    public boolean isValidValue(RP_SYS_RhythmCombinatorValue value)
    {
        return value != null;
    }

    @Override
    public String toString()
    {
        return "RP_SYS_RhythmCombinator(" + baseRhythm.getName() + ")";
    }

    @Override
    public boolean isCompatibleWith(RhythmParameter<?> rp)
    {
        return rp instanceof RP_SYS_RhythmCombinator && rp.getId().equals(getId());
    }


    @Override
    public <T> RP_SYS_RhythmCombinatorValue convertValue(RhythmParameter<T> rp, T rpValue)
    {
        Preconditions.checkArgument(isCompatibleWith(rp), "rp=%s is not compatible with this=%s", rp, this);
        Preconditions.checkNotNull(rpValue);

        RP_SYS_RhythmCombinator rpRc = (RP_SYS_RhythmCombinator) rp;
        if (rpRc.getBaseRhythm() == baseRhythm)
        {
            return (RP_SYS_RhythmCombinatorValue) rpValue;
        } else
        {
            return new RP_SYS_RhythmCombinatorValue(getBaseRhythm());
        }
    }

    @Override
    public String getDisplayValue(RP_SYS_RhythmCombinatorValue value)
    {
        return value.toString();
    }

    /**
     * Find the first RP_SYS_RhythmCombinator instance in the baseRhythm parameters of r.
     *
     * @param r
     * @return Can be null if not found
     */
    static public RP_SYS_RhythmCombinator getRhythmCombinatorRp(Rhythm r)
    {
        checkNotNull(r);
        return (RP_SYS_RhythmCombinator) r.getRhythmParameters()
                .stream()
                .filter(rp -> (rp instanceof RP_SYS_RhythmCombinator))
                .findAny()
                .orElse(null);
    }


}

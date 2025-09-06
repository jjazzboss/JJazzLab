package org.jjazz.rhythmmusicgeneration.api;

import com.google.common.base.Preconditions;
import static com.google.common.base.Preconditions.checkNotNull;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.logging.Logger;
import org.jjazz.rhythm.api.AdaptedRhythm;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.rhythmmusicgeneration.spi.ConfigurableMusicGeneratorProvider;
import org.jjazz.utilities.api.ResUtil;

/**
 * A RhythmParameter to map substitute baseRhythm tracks by tracks from other rhythms.
 * <p>
 */
public class RP_SYS_SubstituteTracks implements RhythmParameter<RP_SYS_SubstituteTracksValue>
{
    public static String ID = "RP_SYS_SubstituteTracksID";
    private final RP_SYS_SubstituteTracksValue DEFAULT_VALUE;
    private final Rhythm baseRhythm;
    private final boolean primary;
    private static final Logger LOGGER = Logger.getLogger(RP_SYS_SubstituteTracks.class.getSimpleName());

    /**
     *
     * @param baseRhythm Must implement the ConfigurableMusicGeneratorProvider interface.
     * @param primary
     */
    public RP_SYS_SubstituteTracks(Rhythm baseRhythm, boolean primary)
    {
        Objects.requireNonNull(baseRhythm);
        Preconditions.checkArgument(baseRhythm instanceof ConfigurableMusicGeneratorProvider, "baseRhythm=%s", baseRhythm);
        this.primary = primary;
        this.baseRhythm = baseRhythm;
        DEFAULT_VALUE = new RP_SYS_SubstituteTracksValue(baseRhythm);
    }

    /**
     *
     * @param newBaseRhythm Must implement the ConfigurableMusicGeneratorProvider interface.
     * @return
     */
    @Override
    public RP_SYS_SubstituteTracks getCopy(Rhythm newBaseRhythm)
    {
        var res = baseRhythm == newBaseRhythm ? this : new RP_SYS_SubstituteTracks(newBaseRhythm, primary);
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
        return ID;
    }

    @Override
    public String getDisplayName()
    {
        return ResUtil.getString(getClass(), "RpSysSubstituteTracksName");
    }

    @Override
    public String getDescription()
    {
        return ResUtil.getString(getClass(), "RpSysSubstituteTracksDesc");
    }

    /**
     * Generally used as tooltip by the framework.
     *
     * @param value
     * @return
     */
    @Override
    public String getValueDescription(RP_SYS_SubstituteTracksValue value)
    {
        var joiner = new StringJoiner(", ");
        for (var rv : value.getSourceRhythmVoices())
        {
            var rvDest = value.getDestRhythmVoice(rv);
            joiner.add(rv.getName() + " > " + rvDest.getContainer().getName() + "/" + rv.getName());
        }
        return joiner.toString();
    }

    @Override
    public RP_SYS_SubstituteTracksValue getDefaultValue()
    {
        return DEFAULT_VALUE;
    }

    @Override
    public String saveAsString(RP_SYS_SubstituteTracksValue v)
    {
        return RP_SYS_SubstituteTracksValue.saveAsString(v);
    }

    @Override
    public RP_SYS_SubstituteTracksValue loadFromString(String s)
    {
        return RP_SYS_SubstituteTracksValue.loadFromString(baseRhythm, s);
    }

    @Override
    public boolean isValidValue(RP_SYS_SubstituteTracksValue value)
    {
        return value != null;
    }

    @Override
    public String toString()
    {
        return "RP_SYS_SubstituteTracks(" + baseRhythm.getName() + ")";
    }

    @Override
    public boolean isCompatibleWith(RhythmParameter<?> rp)
    {
        return rp instanceof RP_SYS_SubstituteTracks;
    }


    @Override
    public <T> RP_SYS_SubstituteTracksValue convertValue(RhythmParameter<T> rp, T rpValue)
    {
        Preconditions.checkArgument(isCompatibleWith(rp), "rp=%s is not compatible with this=%s", rp, this);
        Preconditions.checkNotNull(rpValue);

        RP_SYS_SubstituteTracks rpSt = (RP_SYS_SubstituteTracks) rp;
        var rpStRhythm = rpSt.getBaseRhythm();
        if (rpStRhythm == baseRhythm)
        {
            return (RP_SYS_SubstituteTracksValue) rpValue;
        } else
        {
            return new RP_SYS_SubstituteTracksValue(baseRhythm);
        }
    }

    @Override
    public String getDisplayValue(RP_SYS_SubstituteTracksValue value)
    {
        return value.toString();
    }

    /**
     * Find the first RP_SYS_SubstituteTracks instance in the baseRhythm parameters of r.
     *
     * @param r
     * @return Can be null if not found
     */
    static public RP_SYS_SubstituteTracks getSubstituteTracksRp(Rhythm r)
    {
        checkNotNull(r);
        return (RP_SYS_SubstituteTracks) r.getRhythmParameters()
                .stream()
                .filter(rp -> (rp instanceof RP_SYS_SubstituteTracks))
                .findAny()
                .orElse(null);
    }


}

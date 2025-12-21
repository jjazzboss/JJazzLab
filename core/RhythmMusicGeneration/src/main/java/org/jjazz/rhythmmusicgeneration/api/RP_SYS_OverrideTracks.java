package org.jjazz.rhythmmusicgeneration.api;

import com.google.common.base.Preconditions;
import static com.google.common.base.Preconditions.checkNotNull;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.logging.Logger;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.rhythmmusicgeneration.spi.ConfigurableMusicGeneratorProvider;
import org.jjazz.utilities.api.ResUtil;

/**
 * A RhythmParameter to override baseRhythm tracks by tracks from other rhythms.
 * <p>
 */
public class RP_SYS_OverrideTracks implements RhythmParameter<RP_SYS_OverrideTracksValue>
{

    public static String ID = "RP_SYS_OverrideTracksID";
    private final RP_SYS_OverrideTracksValue DEFAULT_VALUE;
    private final Rhythm baseRhythm;
    private final boolean primary;
    private static final Logger LOGGER = Logger.getLogger(RP_SYS_OverrideTracks.class.getSimpleName());

    /**
     *
     * @param baseRhythm Must implement the ConfigurableMusicGeneratorProvider interface.
     * @param primary
     */
    public RP_SYS_OverrideTracks(Rhythm baseRhythm, boolean primary)
    {
        Objects.requireNonNull(baseRhythm);
        Preconditions.checkArgument(baseRhythm instanceof ConfigurableMusicGeneratorProvider, "baseRhythm=%s", baseRhythm);
        this.primary = primary;
        this.baseRhythm = baseRhythm;
        DEFAULT_VALUE = new RP_SYS_OverrideTracksValue(baseRhythm);
    }

    /**
     *
     * @param newBaseRhythm Must implement the ConfigurableMusicGeneratorProvider interface.
     * @return
     */
    @Override
    public RP_SYS_OverrideTracks getCopy(Rhythm newBaseRhythm)
    {
        var res = baseRhythm == newBaseRhythm ? this : new RP_SYS_OverrideTracks(newBaseRhythm, primary);
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
        return ResUtil.getString(getClass(), "RpSysOverrideTracksName");
    }

    @Override
    public String getDescription()
    {
        return ResUtil.getString(getClass(), "RpSysOverrideTracksDesc");
    }


    @Override
    public String getValueDescription(RP_SYS_OverrideTracksValue value)
    {
        return value.toDescriptionString();
    }

    @Override
    public RP_SYS_OverrideTracksValue getDefaultValue()
    {
        return DEFAULT_VALUE;
    }

    @Override
    public String saveAsString(RP_SYS_OverrideTracksValue v)
    {
        return RP_SYS_OverrideTracksValue.saveAsString(v);
    }

    @Override
    public RP_SYS_OverrideTracksValue loadFromString(String s)
    {
        return RP_SYS_OverrideTracksValue.loadFromString(baseRhythm, s);
    }

    @Override
    public boolean isValidValue(RP_SYS_OverrideTracksValue value)
    {
        return value != null;
    }

    @Override
    public String toString()
    {
        return "RP_SYS_OverrideTracks(" + baseRhythm.getName() + ")";
    }

    @Override
    public boolean isCompatibleWith(RhythmParameter<?> rp)
    {
        return rp instanceof RP_SYS_OverrideTracks;
    }


    @Override
    public <T> RP_SYS_OverrideTracksValue convertValue(RhythmParameter<T> rp, T rpValue)
    {
        Preconditions.checkArgument(isCompatibleWith(rp), "rp=%s is not compatible with this=%s", rp, this);
        Preconditions.checkNotNull(rpValue);

        RP_SYS_OverrideTracks rpSt = (RP_SYS_OverrideTracks) rp;
        var rpStRhythm = rpSt.getBaseRhythm();
        if (rpStRhythm == baseRhythm)
        {
            return (RP_SYS_OverrideTracksValue) rpValue;
        } else
        {
            return new RP_SYS_OverrideTracksValue(baseRhythm);
        }
    }

    @Override
    public String getDisplayValue(RP_SYS_OverrideTracksValue value)
    {
        String res = value.toDescriptionString();
        var rvSources = value.getAllSourceRhythmVoices();
        int size = rvSources.size();
        if (size > 1)
        {
            res = ResUtil.getString(getClass(), "NbOverrideTracks", size);
        }
        return res;
    }

    /**
     * Find the first RP_SYS_OverrideTracks instance in the baseRhythm parameters of r.
     *
     * @param r
     * @return Can be null if not found
     */
    static public RP_SYS_OverrideTracks getOverrideTracksRp(Rhythm r)
    {
        checkNotNull(r);
        return (RP_SYS_OverrideTracks) r.getRhythmParameters()
                .stream()
                .filter(rp -> (rp instanceof RP_SYS_OverrideTracks))
                .findAny()
                .orElse(null);
    }


}

package org.jjazz.test.walkingbass.generator;

import com.google.common.base.Preconditions;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import org.openide.util.NbPreferences;

/**
 * Generator settings.
 */
public class WalkingBassGeneratorSettings
{

    private static final String PREF_ONE_OUT_OF_X = "PrefOneOutOfX";
    private static final String PREF_SINGLE_WBP_SOURCE_MAX_SONG_COVERAGE = "PrefSingleSourceMaxSongCoverage";
    private static WalkingBassGeneratorSettings INSTANCE;
    private final Preferences prefs = NbPreferences.forModule(WalkingBassGeneratorSettings.class);
    ;
    private static final Logger LOGGER = Logger.getLogger(WalkingBassGeneratorSettings.class.getSimpleName());

    public static WalkingBassGeneratorSettings getInstance()
    {
        synchronized (WalkingBassGeneratorSettings.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new WalkingBassGeneratorSettings();
            }
        }
        return INSTANCE;
    }

    private WalkingBassGeneratorSettings()
    {
    }

    /**
     * When it's possible use a given WbpSource one out of X times.
     * <p>
     */
    public int getOneOutofX()
    {
        return prefs.getInt(PREF_ONE_OUT_OF_X, 4);
    }

    /**
     * When it's possible use a given WbpSource one out of X times.
     *
     * @param f A value &gt; 0
     */
    public void setOneOutofX(int x)
    {
        Preconditions.checkArgument(x > 0, "x=%s", x);
        prefs.putInt(PREF_ONE_OUT_OF_X, x);
    }


    /**
     * How much of a song can be covered by a single WbpSource.
     */
    public float getSingleWbpSourceMaxSongCoveragePercentage()
    {
        return prefs.getFloat(PREF_SINGLE_WBP_SOURCE_MAX_SONG_COVERAGE, 0.2f);
    }

    /**
     * How much of a song can be covered by a single WbpSource.
     *
     * @param f A value between 0 and 1.
     */
    public void setSingleWbpSourceMaxSongCoveragePercentage(float f)
    {
        Preconditions.checkArgument(f >= 0 && f <= 1, "f=%s", f);
        prefs.putFloat(PREF_SINGLE_WBP_SOURCE_MAX_SONG_COVERAGE, f);
    }

}

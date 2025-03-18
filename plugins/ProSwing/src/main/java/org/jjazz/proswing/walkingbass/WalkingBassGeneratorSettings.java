package org.jjazz.proswing.walkingbass;

import com.google.common.base.Preconditions;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import org.openide.util.NbPreferences;

/**
 * Generator settings.
 */
public class WalkingBassGeneratorSettings
{

    private static final String PREF_WBPSA_STORE_WIDTH = "PrefWbpsaStoreWidth";
    private static final String PREF_WBPSA_STORE_RANDOMIZED_SCORE_WINDOW = "PrefRandomizedScoreWindow";
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
     * Maximum number of WbpSourceAdaptations kept for each bar in WbpsaStore.
     * <p>
     * @return
     */
    public int getWbpsaStoreWidth()
    {
        return prefs.getInt(PREF_WBPSA_STORE_WIDTH, 8);
    }

    /**
     * Maximum number of WbpSourceAdaptations kept for each bar in WbpsaStore.
     *
     * @param size
     */
    public void setWbpsaStoreWidth(int size)
    {
        Preconditions.checkArgument(size > 0, "size=%s", size);
        prefs.putInt(PREF_WBPSA_STORE_WIDTH, size);
    }

    /**
     * WbpsaStore applies some randomization in the order of WbpSourceAdaptations whose compatibility overall scores are similar, i.e.within scoreWindow.
     *
     * @return
     */
    public float getWbpsaStoreRandomizedScoreWindow()
    {
        return prefs.getFloat(PREF_WBPSA_STORE_RANDOMIZED_SCORE_WINDOW, 0f);
    }

    /**
     * WbpsaStore applies some randomization in the order of WbpSourceAdaptations whose compatibility overall scores are similar, i.e. within scoreWindow.
     *
     * @param scoreWindow Use 0 to disable randomization.
     */
    public void setWbpsaStoreRandomizedScoreWindow(float scoreWindow)
    {
        Preconditions.checkArgument(scoreWindow >= 0, "scoreWindow=%s", scoreWindow);
        prefs.putFloat(PREF_WBPSA_STORE_RANDOMIZED_SCORE_WINDOW, scoreWindow);
    }


}

package org.jjazz.jjswing.walkingbass;

import com.google.common.base.Preconditions;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import org.openide.util.NbPreferences;

/**
 * Generator settings.
 */
public class JJSwingBassMusicGeneratorSettings
{

    private static final String PREF_WBPSA_STORE_WIDTH = "PrefWbpsaStoreWidth";
    private static final String PREF_WBPSA_STORE_RANDOMIZED_SCORE_WINDOW = "PrefRandomizedScoreWindow";
    private static final String PREF_ACCEPT_NON_CHORD_BASS_START_NOTE = "PrefAcceptNonChordBassStartNote";
    private static final String PREF_NOTE_TIMING_BIAS_FACTOR = "PrefNoteTimingBias";
    private static JJSwingBassMusicGeneratorSettings INSTANCE;
    private final Preferences prefs = NbPreferences.forModule(JJSwingBassMusicGeneratorSettings.class);
    ;
    private static final Logger LOGGER = Logger.getLogger(JJSwingBassMusicGeneratorSettings.class.getSimpleName());

    public static JJSwingBassMusicGeneratorSettings getInstance()
    {
        synchronized (JJSwingBassMusicGeneratorSettings.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new JJSwingBassMusicGeneratorSettings();
            }
        }
        return INSTANCE;
    }

    private JJSwingBassMusicGeneratorSettings()
    {
    }

    public boolean isAcceptNonChordBassStartNote()
    {
        return prefs.getBoolean(PREF_ACCEPT_NON_CHORD_BASS_START_NOTE, true);
    }

    /**
     * Do we accept that a start bass note for a chord symbol is not the root note.
     *
     * @param b
     */
    public void setAcceptNonRootStartNote(boolean b)
    {
        prefs.putBoolean(PREF_ACCEPT_NON_CHORD_BASS_START_NOTE, b);
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
        return prefs.getFloat(PREF_WBPSA_STORE_RANDOMIZED_SCORE_WINDOW, 0f);        // Default is 5f. 0 means no randomization.
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

    /**
     * Bass note timing bias factor.
     *
     * @return [-1;1]
     */
    public float getTempoNotePositionBiasFactor()
    {
        return prefs.getFloat(PREF_NOTE_TIMING_BIAS_FACTOR, 0f);
    }

    /**
     * Bass note timing bias factor.
     *
     * @param bias [-1;1]
     */
    public void setTempoNotePositionBias(float bias)
    {
        Preconditions.checkArgument(bias >= -1 && bias <= 1, "bias=%s", bias);
        prefs.putFloat(PREF_NOTE_TIMING_BIAS_FACTOR, bias);
    }


}

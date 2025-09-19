package org.jjazz.jjswing.bass;

import com.google.common.base.Preconditions;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import org.openide.util.NbPreferences;

/**
 * JJSwing settings.
 */
public class BassGeneratorSettings
{
    private static final String PREF_WBPSA_STORE_RANDOMIZED = "PrefRandomizedWbpsaStore";
    private static final String PREF_ACCEPT_NON_CHORD_BASS_START_NOTE = "PrefAcceptNonChordBassStartNote";
    private static final String PREF_NOTE_TIMING_BIAS_FACTOR = "PrefNoteTimingBias";
    private static BassGeneratorSettings INSTANCE;
    private final Preferences prefs = NbPreferences.forModule(BassGeneratorSettings.class);
    ;
    private static final Logger LOGGER = Logger.getLogger(BassGeneratorSettings.class.getSimpleName());

    public static BassGeneratorSettings getInstance()
    {
        synchronized (BassGeneratorSettings.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new BassGeneratorSettings();
            }
        }
        return INSTANCE;
    }

    private BassGeneratorSettings()
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
     * Check if WbpsaStore applies some partial randomization in the order of WbpSourceAdaptations.
     *
     * @return
     */
    public boolean isWbpsaStoreRandomized()
    {
        return prefs.getBoolean(PREF_WBPSA_STORE_RANDOMIZED, true);        
    }

    /**
     * Set if WbpsaStore applies some partial randomization in the order of WbpSourceAdaptations.
     *
     * @param b 
     */
    public void setWbpsaStoreRandomized(boolean b)
    {
        prefs.putBoolean(PREF_WBPSA_STORE_RANDOMIZED, b);
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

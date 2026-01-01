package org.jjazz.jjswing.bass;

import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.event.SwingPropertyChangeSupport;
import org.openide.util.NbPreferences;

/**
 * JJSwing settings.
 */
public class BassGeneratorSettings
{

    private static final String PREF_WBPSA_STORE_RANDOMIZED = "PrefRandomizedWbpsaStore";
    private static final String PREF_ACCEPT_NON_CHORD_BASS_START_NOTE = "PrefAcceptNonChordBassStartNote";
    private static final String PREF_SWING_PROFILE_INTENSITY = "PrefSwingProfileIntensity";
    private static BassGeneratorSettings INSTANCE;
    private final Preferences prefs = NbPreferences.forModule(BassGeneratorSettings.class);
    private final SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);

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
        var old = isAcceptNonChordBassStartNote();
        prefs.putBoolean(PREF_ACCEPT_NON_CHORD_BASS_START_NOTE, b);
        pcs.firePropertyChange(PREF_ACCEPT_NON_CHORD_BASS_START_NOTE, old, b);
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
        var old = isWbpsaStoreRandomized();
        prefs.putBoolean(PREF_WBPSA_STORE_RANDOMIZED, b);
        pcs.firePropertyChange(PREF_WBPSA_STORE_RANDOMIZED, old, b);
    }

    /**
     * Intensity of swing tempo-based adjutments.
     *
     * @return 0 means no adjustment, 1 full adjustment.
     * @see org.jjazz.jjswing.tempoadapter.SwingProfile
     */
    public float getSwingProfileIntensity()
    {
        return prefs.getFloat(PREF_SWING_PROFILE_INTENSITY, 1f);
    }

    public void setSwingProfileIntensity(float intensity)
    {
        float old = getSwingProfileIntensity();
        prefs.putFloat(PREF_SWING_PROFILE_INTENSITY, intensity);
        pcs.firePropertyChange(PREF_SWING_PROFILE_INTENSITY, old, intensity);
    }

    public synchronized void addPropertyChangeListener(PropertyChangeListener listener)
    {
        pcs.addPropertyChangeListener(listener);
    }

    public synchronized void removePropertyChangeListener(PropertyChangeListener listener)
    {
        pcs.removePropertyChangeListener(listener);
    }

}

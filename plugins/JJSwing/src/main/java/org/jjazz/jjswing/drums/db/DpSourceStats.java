package org.jjazz.jjswing.drums.db;

/**
 * Statistics of a DrumsPhraseSource.
 * <p>
 */
public record DpSourceStats()
        {

// ===============================================================================================
// Private methods
// ===============================================================================================
    public static DpSourceStats of(DpSource dps)
    {
        return new DpSourceStats();
    }
}

package org.jjazz.base.support;

import java.text.ParseException;
import java.time.Instant;
import org.openide.util.Lookup;



public interface DonManager
{
    /**
     * Get an implementation available in the global lookup.
     * 
     * @return Might be null
     */
    public static DonManager getDefault()
    {
        return Lookup.getDefault().lookup(DonManager.class);
    }

    /**
     * Return creation date in nb of days since 2020 of the last registered authorization code (might be expired).
     *
     * @return -1 if no valid code.
     */
    int getRegisteredCodeDateAsNbDays2020();

    /**
     * Return expiration date of last registered code (might be expired).
     *
     * @return Null if no valid current donation code.
     */
    Instant getRegisteredCodeExpirationDate();

    /**
     * The expiration date of the last registered code, if available.
     *
     * @return The number of days from the start of 2020. Or -1 if no expiration date available.
     */
    int getRegisteredCodeExpirationDateAsNbDays2020();

    /**
     *
     * @return A string like "4 Jul 2023". Null if no expiration date stored.
     */
    String getRegisteredCodeExpirationDateAsString();

    /**
     * Check if there is a registered authorization code and it's still valid (not expired).
     *
     * @return
     */
    boolean hasValidRegisteredCode();

    /**
     * Register an authorization code.
     * <p>
     * Check if parameters are valid and register the code if OK.
     *
     * @param code
     * @param email
     * @throws java.text.ParseException If code and/or email are not valid.
     */
    void registerCode(String code, String email) throws ParseException;
    
}

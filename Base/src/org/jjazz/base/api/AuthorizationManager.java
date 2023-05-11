/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 *  This file is part of the JJazzLabX software.
 *   
 *  JJazzLabX is free software: you can redistribute it and/or modify
 *  it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *  as published by the Free Software Foundation, either version 3 of the License, 
 *  or (at your option) any later version.
 *
 *  JJazzLabX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 *  GNU Lesser General Public License for more details.
 * 
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JJazzLabX.  If not, see <https://www.gnu.org/licenses/>
 * 
 *  Contributor(s): 
 */
package org.jjazz.base.api;

import java.text.ParseException;
import java.time.Instant;
import org.openide.util.Lookup;


/**
 * Manage an authorization code.
 */
public interface AuthorizationManager
{
    /**
     * Get an implementation available in the global lookup.
     * 
     * @return Might be null
     */
    public static AuthorizationManager getDefault()
    {
        return Lookup.getDefault().lookup(AuthorizationManager.class);
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

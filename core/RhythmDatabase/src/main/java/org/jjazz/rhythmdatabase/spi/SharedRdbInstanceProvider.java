/*
 * 
 *   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 *   Copyright @2019 Jerome Lelasseux. All rights reserved.
 * 
 *   This file is part of the JJazzLab software.
 *    
 *   JJazzLab is free software: you can redistribute it and/or modify
 *   it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *   as published by the Free Software Foundation, either version 3 of the License, 
 *   or (at your option) any later version.
 * 
 *   JJazzLab is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *  
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *  
 *   Contributor(s): 
 * 
 */
package org.jjazz.rhythmdatabase.spi;

import java.util.concurrent.Future;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.openide.util.Lookup;

/**
 * Provide a shared RhythmDatabase instance.
 */
public interface SharedRdbInstanceProvider
{

    /**
     * Get the default implementation available in the global lookup.
     *
     * @return Can be null
     */
    public static SharedRdbInstanceProvider getDefault()
    {
        return Lookup.getDefault().lookup(SharedRdbInstanceProvider.class);
    }

    /**
     * Initialize the RhythmDatabase instance.
     * <p>
     * As the initialization can take some time (e.g. reading files), the implementation must launch a task in a different thread and return the corresponding
     * Future so that caller can monitor completion.
     *
     * @return
     */
    Future<?> initialize();

    /**
     * Check if the RhythmDatabase instance is initialized.
     *
     * @return
     */
    boolean isInitialized();

    /**
     * Get the initialized shared instance.
     * <p>
     * If initialization is not done, call initialize(). If initialization is not complete, wait for its completion.
     *
     * @return cannot be null
     */
    RhythmDatabase get();


    /**
     * Request or cancel a full refresh of the shared database upon next startup.
     *
     * @param b Request if true, cancel if false
     */
    public void markForStartupRefresh(boolean b);

    /**
     * Check if a full refresh is planned for next startup.
     *
     * @return
     */
    public boolean isMarkedForStartupRefresh();


}

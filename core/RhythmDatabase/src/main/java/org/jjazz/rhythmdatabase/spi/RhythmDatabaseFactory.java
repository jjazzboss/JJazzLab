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
import org.jjazz.rhythmdatabase.api.DefaultRhythmDatabase;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.openide.util.Lookup;

/**
 * A factory for a RhythmDatabase instance.
 */
public interface RhythmDatabaseFactory
{

    /**
     * Get the default implementation available in the global lookup or, if nothing found, return a factory which just provides the DefaultRhythmDatabase
     * instance.
     *
     * @return
     */
    public static RhythmDatabaseFactory getDefault()
    {
        RhythmDatabaseFactory res = Lookup.getDefault().lookup(RhythmDatabaseFactory.class);
        if (res == null)
        {
            res = DefaultRhythmDatabase.getFactoryInstance();
        }
        return res;
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
     * Get the initialized instance.
     * <p>
     * If initialization is not done, call initialize(). If initialization is not complete, wait for its completion.
     *
     * @return
     */
    RhythmDatabase get();

}

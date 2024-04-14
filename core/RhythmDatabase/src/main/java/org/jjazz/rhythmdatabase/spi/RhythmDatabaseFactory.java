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

import org.jjazz.rhythmdatabase.api.DefaultRhythmDatabase;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.openide.util.Lookup;

/**
 * A service to access a RhythmDatabase instance.
 */
public interface RhythmDatabaseFactory
{

    static class DefaultFactory implements RhythmDatabaseFactory
    {

        static private DefaultFactory INSTANCE;
        private final RhythmDatabase dbInstance;

        static DefaultFactory getInstance()
        {
            if (INSTANCE == null)
            {
                INSTANCE = new DefaultFactory();
            }
            return INSTANCE;
        }

        private DefaultFactory()
        {
            dbInstance = new DefaultRhythmDatabase();
        }

        @Override
        public RhythmDatabase get()
        {
            return dbInstance;
        }
    }

    /**
     * Return the first implementation found in the global lookup, or the DefaultFactory.
     *
     * @return
     */
    public static RhythmDatabaseFactory getDefault()
    {
        RhythmDatabaseFactory result = Lookup.getDefault().lookup(RhythmDatabaseFactory.class);
        if (result == null)
        {
            result = DefaultFactory.getInstance();
        }
        return result;
    }


    /**
     * A factory to return a DefaultRhythmDatabase instance.
     *
     * @return
     */
    RhythmDatabase get();


}

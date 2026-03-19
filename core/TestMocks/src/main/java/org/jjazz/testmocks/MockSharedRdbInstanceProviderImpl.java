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
package org.jjazz.testmocks;

import java.util.concurrent.Future;
import java.util.logging.Logger;
import org.jjazz.rhythmdatabase.api.DefaultRhythmDatabaseImpl;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.jjazz.rhythmdatabase.spi.SharedRdbInstanceProvider;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.NbPreferences;

/**
 * An implementation for unit tests.
 * <p>
 * Provide a RhythmDatabase which only contains RhythmMocks.
 */
@ServiceProvider(service = SharedRdbInstanceProvider.class, position = -100000)
public class MockSharedRdbInstanceProviderImpl implements SharedRdbInstanceProvider
{

    private DefaultRhythmDatabaseImpl rdb = null;
    private static final Logger LOGGER = Logger.getLogger(MockSharedRdbInstanceProviderImpl.class.getSimpleName());

    public MockSharedRdbInstanceProviderImpl()
    {
        LOGGER.info("MockSharedRdbInstanceProviderImpl registered");
    }

    @Override
    public Future<?> initialize()
    {
        LOGGER.info("initialize() --");
        rdb = new DefaultRhythmDatabaseImpl(NbPreferences.forModule(getClass()));
        rdb.addRhythmsFromRhythmProviders(false, true, false, RhythmMocksProviderImpl.ID);
        System.out.println(rdb.toStatsString());
        assert !rdb.getRhythms().isEmpty();
        return null;
    }

    @Override
    public boolean isInitialized()
    {
        return rdb != null;
    }

    @Override
    public RhythmDatabase get()
    {
        if (!isInitialized())
        {
            initialize();
        }
        return rdb;
    }

    @Override
    public void markForStartupRefresh(boolean b)
    {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public boolean isMarkedForStartupRefresh()
    {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

}

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
package org.jjazz.utilities.api;

/**
 * A wrapper for a runnable which makes unexpected runtime problems visible.
 * <p>
 * By default a Runnable passed to an ExecutorService will stop with no info if an unexpected Exception or Error occurs. Use this wrapper to
 * make sure that such Exception or Error is logged with a stack trace.
 */
public class CheckedRunnable implements Runnable
{

    private final Runnable r;

    public CheckedRunnable(Runnable r)
    {
        this.r = r;
    }

    @Override
    public void run()
    {
        try
        {
            r.run();
        } catch (Throwable ex)
        {
            ex.printStackTrace();
            throw ex;
        }
    }
}

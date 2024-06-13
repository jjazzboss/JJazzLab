/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 *  This file is part of the JJazzLab software.
 *   
 *  JJazzLab is free software: you can redistribute it and/or modify
 *  it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *  as published by the Free Software Foundation, either version 3 of the License, 
 *  or (at your option) any later version.
 *
 *  JJazzLab is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 * 
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 * 
 *  Contributor(s): 
 */
package org.jjazz.rhythm.spi;

import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.rhythm.api.Rhythm;
import org.openide.util.Lookup;

/**
 * The service provider in charge of providing the rhythm stubs when no valid rhythm is available for a given time signature.
 */
public interface StubRhythmProvider extends RhythmProvider
{

    static public StubRhythmProvider getDefault()
    {
        StubRhythmProvider result = Lookup.getDefault().lookup(StubRhythmProvider.class);
        if (result == null)
        {
            throw new IllegalStateException("No StubRhythmProvider implementation found");  
        }
        return result;
    }


    /**
     * Get the stub rhythm for the specified time signature.
     *
     * @param ts
     * @return Can't be null.
     */
    Rhythm getStubRhythm(TimeSignature ts);
}

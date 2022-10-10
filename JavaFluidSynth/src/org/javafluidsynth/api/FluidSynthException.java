/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright @2022 Jerome Lelasseux. All rights reserved.
 *
 *  You can redistribute it and/or modify
 *  it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *  as published by the Free Software Foundation, either version 3 of the License, 
 *  or (at your option) any later version.
 *
 *  Software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 *  GNU Lesser General Public License for more details.
 * 
 *  Contributor(s): 
 */
package org.javafluidsynth.api;

/**
 * FluidSynth exception wrapper.
 */
public class FluidSynthException extends Exception
{

    public FluidSynthException(String message)
    {
        super(message);
    }

    public FluidSynthException(String message, Throwable cause)
    {
        super(message, cause);
    }


}

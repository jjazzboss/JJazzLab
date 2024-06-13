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
package org.jjazz.embeddedsynth.api;

import java.io.File;

public interface Mp3Encoder
{

    /**
     * Encode an audio file to a mp3 file.
     *
     * @param audioFile If audio file format is not supported an exception is
     * thrown. Must support at least .wav file.
     * @param mp3File
     * @param lowQuality If true encode with low-quality settings.
     * @param useVariableEncoding If false use fix-rate encoding
     * @throws EmbeddedSynthException
     */
    void encode(File audioFile, File mp3File, boolean lowQuality, boolean useVariableEncoding) throws EmbeddedSynthException;
}

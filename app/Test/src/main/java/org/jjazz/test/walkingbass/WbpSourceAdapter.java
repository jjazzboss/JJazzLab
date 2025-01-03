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
package org.jjazz.test.walkingbass;

import org.jjazz.harmony.api.Note;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.test.walkingbass.generator.WbpSourceAdaptation;

/**
 * Get the resulting phrase from a WbpSourceAdaptation.
 */
public interface WbpSourceAdapter
{

    /**
     * Get the resulting phrase from wbpsa.
     *
     * @param wbpsa       The source phrase to adapt
     * @param offsetStart If false returned phrase starts at bar 0, otherwise at bar scs.getBarRange().from
     * @return
     */
    Phrase getPhrase(WbpSourceAdaptation wbpsa, boolean offsetStart);

    /**
     * Get the resulting target note from wbpsa.
     *
     * @param wbpsa
     * @return
     */
    Note getTargetNote(WbpSourceAdaptation wbpsa);
}

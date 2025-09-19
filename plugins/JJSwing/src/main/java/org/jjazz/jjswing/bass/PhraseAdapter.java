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
package org.jjazz.jjswing.bass;

import org.jjazz.phrase.api.Phrase;

/**
 * Adapt a WbpSource phrase from a WbpSourceAdaptation into the resulting phrase.
 */
public interface PhraseAdapter
{

    /**
     * Get the resulting phrase from wbpsa.
     * <p>
     * NOTE: first note position might be slightly before wbpsa start bar if WbpSource.getFirstNoteBeatShift() is &lt; 0.
     *
     * @param wbpsa The source phrase context
     * @return
     */
    Phrase getPhrase(WbpSourceAdaptation wbpsa);

    /**
     * Get the resulting target note from wbpsa.
     *
     * @param wbpsa
     * @return -1 if WbpSource has no target note 
     */
    int getTargetPitch(WbpSourceAdaptation wbpsa);
}

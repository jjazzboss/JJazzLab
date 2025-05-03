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
package org.jjazz.rhythm.api;

import javax.swing.event.ChangeListener;

/**
 * A tagging interface for RhythmParameter value classes which are mutable.
 * <p>
 * Instance must notify listeners when its state has changed.
 * <p>
 * Immutable value classes should be preferred, but it might not always be possible like for RP_SYS_CustomPhraseValue, which contains editable Phrases.
 */
public interface MutableRpValue
{
    /**
     * Be notified of RP value changes.
     * <p>
     * Note that listeners won't be notified if a new RhythmParameter value instance is replaced by another one. Use SongStructure RpChangedEvent to get all
     * types of RP value changes.
     *
     * @param listener
     */
    void addChangeListener(ChangeListener listener);

    void removeChangeListener(ChangeListener listener);
}

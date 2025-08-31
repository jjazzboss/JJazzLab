package org.jjazz.ss_editor.rpviewer.api;

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
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.songstructure.api.SongPart;

/**
 * The edit operations which can be triggered directly by a RpViewer.
 */
public interface RpViewerController
{

    /**
     * User wants to use the custom edit dialog of the specified RhythmParameter.
     *
     * @param spt
     * @param rp 
     */
    void rhythmParameterEditWithCustomDialog(SongPart spt, RhythmParameter<?> rp);

    /**
     * User has directly edited a RhythmParameter value, probably from a RpViewerEditableRenderer.
     *
     * @param <E>
     * @param spt
     * @param rp
     * @param rpValue The new value to set
     */
    <E> void rhythmParameterEdit(SongPart spt, RhythmParameter<E> rp, E rpValue);
}

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
package org.jjazz.ss_editor.sptviewer.api;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ss_editor.rpviewer.api.RpViewerController;

/**
 * Listener of SptViewer events.
 */
public interface SptViewerMouseListener extends RpViewerController
{

    /**
     *
     * @param e
     * @param spt
     * @param multiSelect True if user clicked on the multiSelect bar.
     */
    void songPartClicked(MouseEvent e, SongPart spt, boolean multiSelect);

    void songPartDragged(MouseEvent e, SongPart spt);

    void songPartReleased(MouseEvent e, SongPart spt);

    void rhythmParameterClicked(MouseEvent e, SongPart spt, RhythmParameter<?> rp);

    void rhythmParameterDragged(MouseEvent e, SongPart spt, RhythmParameter<?> rp);

    void rhythmParameterReleased(MouseEvent e, SongPart spt, RhythmParameter<?> rp);

    void rhythmParameterWheelMoved(MouseWheelEvent e, SongPart spt, RhythmParameter<?> rp);

    /**
     * User wants to edit the specified songpart's rhythm.
     *
     * @param spt
     */
    void editSongPartRhythm(SongPart spt);

    /**
     * User wants to edit the specified songpart's name.
     *
     * @param spt
     */
    void editSongPartName(SongPart spt);
}

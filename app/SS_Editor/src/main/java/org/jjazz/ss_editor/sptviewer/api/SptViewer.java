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

import java.awt.Rectangle;
import org.jjazz.ss_editor.sptviewer.spi.SptViewerSettings;
import java.util.List;
import javax.swing.JPanel;
import org.jjazz.harmony.api.Position;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ss_editor.rpviewer.spi.DefaultRpViewerRendererFactory;

/**
 * A base class for SongPart viewers.
 * <p>
 * Must keep itself updated by listening to model changes. User actions are sent to the controller.
 */
public abstract class SptViewer extends JPanel
{

    public abstract SongPart getModel();

    public abstract SptViewerSettings getSettings();

    public abstract DefaultRpViewerRendererFactory getDefaultRpRendererFactory();

    public abstract void setController(SptViewerMouseListener controller);

    public abstract void setSelected(boolean b);

    public abstract void setSelected(RhythmParameter<?> rp, boolean b);

    /**
     * Horizontal zoom factor.
     *
     * @param factor 0=min zoom (bird's view), 100=max zoom
     */
    public abstract void setZoomHFactor(int factor);

    public abstract int getZoomHFactor();

    /**
     * Vertical zoom factor.
     *
     * @param factor 0=min zoom (bird's view), 100=max zoom
     */
    public abstract void setZoomVFactor(int factor);

    public abstract int getZoomVFactor();

    public abstract void setFocusOnRpViewer(RhythmParameter<?> rp);

    public abstract void setVisibleRps(List<RhythmParameter<?>> rps);
    
    public abstract Rectangle getRpViewerRectangle(RhythmParameter<?> rp);

    /**
     * True by default.
     *
     * @param b
     */
    public abstract void setRhythmVisible(boolean b);

    /**
     *
     * @param b True if this SptViewer is this Spt is part of a multi-selection
     * @param first True if this SptViewer is the first Spt of a multi-selection
     */
    public abstract void setMultiSelectMode(boolean b, boolean first);

    public abstract void setNameVisible(boolean b);

    public abstract void setTimeSignatureVisible(boolean b);

    public abstract void cleanup();

    /**
     * Show a playback point in the editor at specified position.
     *
     * @param show Show/hide the playback point.
     * @param pos The position within the SongStructure model. Not used if b==false.
     */
    public abstract void showPlaybackPoint(boolean show, Position pos);
}

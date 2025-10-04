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
package org.jjazz.chordinspector.spi;

import javax.swing.Icon;
import javax.swing.JComponent;
import org.jjazz.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.song.api.Song;

/**
 * A service provider to represent a chord symbol.
 * <p>
 */
public interface ChordViewer
{
    /**
     * Get the visual component which represents the chord symbol.
     *
     * @return
     */
    JComponent getComponent();

    /**
     * A one-line description of this viewer.
     * <p>
     * Used by UI to describe this viewer, e.g. as a tooltip.
     *
     * @return
     */
    String getDescription();

    /**
     * A 16x16 icon representing this viewer.
     * <p>
     * Used by the framework to build a UI which lets the user select the active NotesViewer.
     *
     * @return
     */
    Icon getIcon();

    /**
     * Provide optional context info to the viewer.
     *
     * @param song    Can be null.
     * @param midiMix Can be null.
     * @param rv      Can be null.
     */
    void setContext(Song song, MidiMix midiMix, RhythmVoice rv);


    /**
     * Represent the specified chord symbol.
     *
     * @param ecs
     */
    void setModel(ExtChordSymbol ecs);
    
    ExtChordSymbol getModel();

    void setEnabled(boolean b);

    boolean isEnabled();

    void cleanup();
}

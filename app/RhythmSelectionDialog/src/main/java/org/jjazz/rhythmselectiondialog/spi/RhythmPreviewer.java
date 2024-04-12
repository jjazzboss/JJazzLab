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
package org.jjazz.rhythmselectiondialog.spi;

import java.awt.event.ActionListener;
import java.util.Map;
import javax.sound.midi.MidiUnavailableException;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.song.api.Song;
import org.jjazz.songstructure.api.SongPart;
import org.openide.util.Lookup;


/**
 * A service to "preview" (prehear actually) a rhythm.
 */
public interface RhythmPreviewer
{

    /**
     * Get the first implementation available in the global lookup.
     *
     * @return Can be null.
     */
    static public RhythmPreviewer getDefault()
    {
        var res = Lookup.getDefault().lookup(RhythmPreviewer.class);
        return res;
    }

    /**
     * Set the context for which the object will preview rhythms.
     *
     * @param sg  The song for which we preview rhythm
     * @param spt The spt for which rhythm is changed
     * @throws MidiUnavailableException
     */
    void setContext(Song sg, SongPart spt) throws MidiUnavailableException;

    /**
     * Should be called when provider will no longer be used.
     * <p>
     * Enable the provider to release resources or restore settings if needed.
     */
    void cleanup();

    /**
     * Hear a "preview" of the specified rhythm.
     * <p>
     * If a preview is already being played on a different rhythm, stop it and start a new one. The context must have been set previously.
     *
     * @param r
     * @param rpValues          The rhythm RhythmParameter values. Can't be null. For non defined values the previewer should use the default RhythmParameter
     *                          values in this case.
     * @param useRhythmTempo    If true use r preferred tempo, otherwise use default tempo.
     * @param loop              If true the rhythm preview loops until stop() is called.
     * @param endActionListener Called when preview is complete (if loop disabled) or stopped. Called on the EDT. Can be null if not used.
     * @throws org.jjazz.rhythm.api.MusicGenerationException If a problem occured. endActionListener is not called in this case.
     */
    void previewRhythm(Rhythm r, Map<RhythmParameter<?>, Object> rpValues, boolean useRhythmTempo, boolean loop, ActionListener endActionListener) throws MusicGenerationException;

    /**
     * The rhythm currently being previewed.
     *
     * @return Null if no preview being currently played.
     */
    Rhythm getPreviewedRhythm();

    /**
     * Stop the current preview.
     * <p>
     * Do nothing if isPreviewRunning() returns false. If endActionListener is specified in previewRhythm(), it is called.
     */
    void stop();
}

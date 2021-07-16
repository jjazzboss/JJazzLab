/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 *  This file is part of the JJazzLabX software.
 *   
 *  JJazzLabX is free software: you can redistribute it and/or modify
 *  it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *  as published by the Free Software Foundation, either version 3 of the License, 
 *  or (at your option) any later version.
 *
 *  JJazzLabX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 * 
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JJazzLabX.  If not, see <https://www.gnu.org/licenses/>
 * 
 *  Contributor(s): 
 */
package org.jjazz.ui.ss_editor.spi;

import java.awt.event.ActionListener;
import java.util.Map;
import javax.swing.JDialog;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.rhythm.database.api.RhythmInfo;
import org.jjazz.ui.ss_editor.editors.SimpleRhythmSelectionDialog;
import org.openide.util.Lookup;
import org.openide.windows.WindowManager;

/**
 * A dialog to select a rhythm for a SongStructure.
 */
abstract public class RhythmSelectionDialog extends JDialog
{

    /**
     * The object responsible to preview a rhythm.
     */
    public interface RhythmPreviewProvider
    {

        /**
         * Should be called when provider will no longer be used.
         * <p>
         * Enable the provider to release resources or restore settings if needed.
         */
        void cleanup();

        /**
         * Hear a "preview" of the specified rhythm.
         * <p>
         * If a preview is already being played on a different rhythm, stop it and start a new one.
         *
         * @param r
         * @param rpValues The rhythm RhythmParameter values. Can't be null, when non defined values the previewer should use the
         * default RhythmParameter values in this case.
         * @param useRhythmTempo If true use r preferred tempo, otherwise use default tempo.
         * @param loop If true the rhythm preview loops until stop() is called.
         * @param endActionListener Called when preview is complete (if loop disabled) or stopped. Called on the EDT. Can be null
         * if not used.
         * @throws org.jjazz.rhythm.api.MusicGenerationException If a problem occured. endActionListener is not called in this
         * case.
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

    public static RhythmSelectionDialog getDefault()
    {
        RhythmSelectionDialog result = Lookup.getDefault().lookup(RhythmSelectionDialog.class);
        if (result == null)
        {
            result = new SimpleRhythmSelectionDialog();
        }
        return result;
    }

    /**
     * Dialog is automatically owned by WindowManager.getDefault().getMainWindow()
     */
    protected RhythmSelectionDialog()
    {
        super(WindowManager.getDefault().getMainWindow());
    }

    /**
     * Initialize the dialog for the specified song rhythm.
     *
     * @param ri
     * @param rpp If null then the rhythm preview feature is disabled. If not null caller is responsible to call rpp.cleanup()
     * when rpp is not used anymore.
     */
    abstract public void preset(RhythmInfo ri, RhythmPreviewProvider rpp);

    /**
     * @return True if dialog was exited OK, false if dialog operation was cancelled.
     */
    abstract public boolean isExitOk();

    /**
     * Return the selected rhythm.
     *
     * @return Null if no valid rhythm was selected, or user chose Cancel
     */
    abstract public RhythmInfo getSelectedRhythm();

    /**
     * Set the title of the dialogm eg "Rhythm for bar XX".
     *
     * @param title
     */
    abstract public void setTitleLabel(String title);

    /**
     * Return if the rhythm should be also applied to the next songParts which have the same rhythm than the sptModel.
     *
     * @return
     */
    abstract public boolean isApplyRhythmToNextSongParts();

    /**
     * Return if the rhythm's preferred tempo should be applied to current song
     *
     * @return
     */
    abstract public boolean isUseRhythmTempo();

    /**
     * Cleanup references to preset data and dialog results.
     */
    abstract public void cleanup();
}

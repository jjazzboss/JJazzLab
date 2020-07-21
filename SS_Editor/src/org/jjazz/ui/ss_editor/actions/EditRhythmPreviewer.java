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
package org.jjazz.ui.ss_editor.actions;

import java.awt.event.ActionListener;
import java.beans.PropertyVetoException;
import java.util.Arrays;
import java.util.logging.Logger;
import javax.sound.midi.MidiUnavailableException;
import org.jjazz.activesong.ActiveSongManager;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.midimix.MidiMix;
import org.jjazz.midimix.MidiMixManager;
import org.jjazz.musiccontrol.ClickManager;
import org.jjazz.musiccontrol.MusicController;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythmmusicgeneration.MusicGenerationContext;
import org.jjazz.rhythmmusicgeneration.spi.MusicGenerator;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongFactory;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.ui.ss_editor.spi.RhythmSelectionDialog;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Exceptions;

/**
 * A RhythmPreviewProvider instance which plays a song part.
 */
public class EditRhythmPreviewer implements RhythmSelectionDialog.RhythmPreviewProvider
{

    private MusicGenerationContext context;
    private MusicGenerationContext saveContext;
    private MusicGenerator.PostProcessor[] savePostProcessors;
    private boolean isPreviewRunning;
    private int saveLoopCount;
    private boolean savePrecountEnabled;
    private boolean save;
    private Song previewSong;
    private Song song;
    private static final Logger LOGGER = Logger.getLogger(EditRhythmPreviewer.class.getSimpleName());

    /**
     *
     * @param sg The song for which we preview rhythm
     * @param spt The spt for which rhythm is changed
     * @throws MidiUnavailableException
     */
    public EditRhythmPreviewer(Song sg, SongPart spt) throws MidiUnavailableException
    {
        if (sg == null || spt == null)
        {
            throw new IllegalArgumentException("sg=" + sg.getName() + " spt=" + spt);
        }
        song = sg;

        // Save playback status
        var mc = MusicController.getInstance();
        saveContext = mc.getContext();
        savePostProcessors = mc.getPostProcessors();
        saveLoopCount = mc.getLoopCount();
        savePrecountEnabled = ClickManager.getInstance().isClickPrecountEnabled();

        // Construct our context with a partial copy of the song
        previewSong = buildPreviewSong(song, spt);
        MidiMix mm = MidiMixManager.getInstance().findMix(previewSong);        // Possible exception here
        context = new MusicGenerationContext(previewSong, mm);

    }

    @Override
    public void cleanup()
    {
        var mc = MusicController.getInstance();
        mc.setLoopCount(saveLoopCount);
        try
        {
            if (savePostProcessors != null)
            {
                mc.setContext(saveContext, savePostProcessors);
            } else
            {
                mc.setContext(saveContext);
            }
        } catch (MusicGenerationException ex)
        {
            // Should never happen, we did not change anything
            Exceptions.printStackTrace(ex);
        }
        ClickManager.getInstance().setClickPrecountEnabled(savePrecountEnabled);
        previewSong.close(false);
    }


    @Override
    public boolean previewRhythm(Rhythm r, ActionListener endListener)
    {
        // Song must be active
        ActiveSongManager asm = ActiveSongManager.getInstance();
        if (asm.getActiveSong() != song)
        {
            String msg = "Can't preview rhythm if song is not active";
            NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return false;
        }


        // Stop any previous playing
        var mc = MusicController.getInstance();
        mc.stop();
        mc.setLoopCount(0);
        ClickManager.getInstance().setClickPrecountEnabled(false);
        isPreviewRunning = false;


        // Update song 
        previewSong.setTempo(r.getPreferredTempo());
        SongStructure ss = previewSong.getSongStructure();
        SongPart spt = ss.getSongPart(0);
        SongPart newSpt = spt.clone(r, spt.getStartBarIndex(), spt.getNbBars(), spt.getParentSection());
        try
        {
            ss.replaceSongParts(Arrays.asList(spt), Arrays.asList(newSpt));
        } catch (UnsupportedEditException ex)
        {
            // Should never happen since there is only 1 song part!
            LOGGER.warning("previewRhythm() Unexpected exception r=" + r.getName() + ". ex=" + ex.getLocalizedMessage());
            return false;
        }


        // Configure and play
        try
        {
            mc.setContext(context);
            mc.play(context.getBarRange().from);        // Possible exception here
            isPreviewRunning = true;
        } catch (MusicGenerationException | PropertyVetoException ex)
        {
            if (ex.getMessage() != null)
            {
                NotifyDescriptor d = new NotifyDescriptor.Message(ex.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(d);
            }
        }

        return isPreviewRunning;
    }

    @Override
    public boolean isPreviewRunning()
    {
        return isPreviewRunning;
    }

    @Override
    public void cancel()
    {
        var mc = MusicController.getInstance();
        mc.stop();
        isPreviewRunning = false;
    }

    private Song buildPreviewSong(Song song, SongPart spt0)
    {
        // Get a copy
        Song newSong = SongFactory.getInstance().getCopy(song);
        SongStructure ss = newSong.getSongStructure();

        // Remove all SongParts except spt0
        try
        {
            ss.removeSongParts(ss.getSongParts(spt -> spt.getStartBarIndex() != spt0.getStartBarIndex()));
        } catch (UnsupportedEditException ex)
        {
            // Should never happen
            Exceptions.printStackTrace(ex);
        }
        return newSong;
    }

}

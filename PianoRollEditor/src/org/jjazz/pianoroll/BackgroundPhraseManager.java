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
package org.jjazz.pianoroll;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import javax.sound.midi.MidiUnavailableException;
import javax.swing.SwingUtilities;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.api.MidiMixManager;
import org.jjazz.musiccontrol.api.SongMusicGenerationListener;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.phrase.api.PhraseSamples;
import org.jjazz.pianoroll.api.PianoRollEditor;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythmmusicgeneration.api.MusicGenerationQueue;
import org.jjazz.song.api.Song;
import org.jjazz.songcontext.api.SongContext;
import org.openide.util.Exceptions;

/**
 * Manage the generation of background phrases and coordination with UI elements.
 */
public class BackgroundPhraseManager implements PropertyChangeListener
{

    private MusicGenerationQueue.Result lastResult;
    private MusicGenerationQueue musicGenerationQueue;
    private SongMusicGenerationListener songMusicGenerationListener;
    private MidiMix midiMix;
    private final PianoRollEditor editor;
    private final BackgroundPhrasesPanel backgroundPhrasesPanel;
    private static final Logger LOGGER = Logger.getLogger(BackgroundPhraseManager.class.getSimpleName());

    public BackgroundPhraseManager(PianoRollEditor editor, BackgroundPhrasesPanel bgPhrasesPanel)
    {
        this.editor = editor;
        this.backgroundPhrasesPanel = bgPhrasesPanel;
        midiMix = null;
        try
        {
            midiMix = MidiMixManager.getInstance().findMix(editor.getSong());
        } catch (MidiUnavailableException ex)
        {
            // Should never happen
            Exceptions.printStackTrace(ex);
        }
        midiMix.addPropertyChangeListener(this);
        songMidiMixChanged();


        // Listen to user selection changes
        backgroundPhrasesPanel.addPropertyChangeListener(BackgroundPhrasesPanel.PROP_VISIBLE_TRACK_NAMES, this);


        musicGenerationQueue = new MusicGenerationQueue(300, 600);
        musicGenerationQueue.addChangeListener(e -> musicGenerationResultReceived(musicGenerationQueue.getLastResult()));
    }

    public void cleanup()
    {
        midiMix.removePropertyChangeListener(this);
        backgroundPhrasesPanel.removePropertyChangeListener(this);
        musicGenerationQueue.stop();
        if (songMusicGenerationListener != null)
        {
            songMusicGenerationListener.cleanup();
        }
    }

    //=============================================================================
    // Private methods
    //=============================================================================

    /**
     * Called when user changes the selected background phrases.
     *
     * @param selectedNames
     */
    private void selectionChanged(List<String> selectedNames)
    {
        if (selectedNames.isEmpty())
        {
            musicGenerationQueue.stop();
            editor.setBackgroundPhases(null);
            if (songMusicGenerationListener != null)
            {
                songMusicGenerationListener.cleanup();
            }
            return;
        }


        if (!musicGenerationQueue.isRunning())
        {
            // Start music generation thread
            musicGenerationQueue.start();


            // Regenerate music each time song context is musically updated -except for our own phrase change events
            songMusicGenerationListener = new SongMusicGenerationListener(editor.getSong(), midiMix);
            songMusicGenerationListener.setBlackList(Set.of(Song.PROP_VETOABLE_USER_PHRASE_CONTENT));
            songMusicGenerationListener.addChangeListener(
                    e -> musicGenerationQueue.add(new SongContext(editor.getSong(), midiMix)));
        }


        // Check if we already got all the phrases
        Map<Integer, Phrase> map = null;
        if (lastResult != null && lastResult.mapRvPhrases() != null)
        {
            map = new HashMap<>();
            for (var n : selectedNames)
            {
                int channel = getChannelFromPhraseName(n);
                RhythmVoice rv = midiMix.getRhythmVoice(channel);
                assert rv != null : " channel=" + channel + " midiMix=" + midiMix;
                Phrase p = lastResult.mapRvPhrases().get(rv);
                if (p == null)
                {
                    map = null;
                    break;
                }
                map.put(channel, p);
            }
        }


        if (map != null)
        {
            // We already got all the phrases
            editor.setBackgroundPhases(map);

        } else
        {
            // Need to generate music
            SongContext sgContext = new SongContext(editor.getSong(), midiMix, editor.getBarRange());
            musicGenerationQueue.add(sgContext);
        }

    }

    /**
     * A music generation task is complete, update visible background phrases.
     *
     * @param result
     */
    private void musicGenerationResultReceived(MusicGenerationQueue.Result result)
    {
        LOGGER.fine("musicGenerationResultReceived() -- ");
        lastResult = result;

        if (lastResult.userException() != null)
        {
            // Problem occured, ignore
            LOGGER.info("musicGenerationResultReceived() User error music generation exception ex="
                    + lastResult.userException().getMessage());
            return;
        }

        // Go back to the EDT
        SwingUtilities.invokeLater(() -> 
        {
            var visibleBackgroundTrackNames = backgroundPhrasesPanel.getSelectedTracks();
            if (visibleBackgroundTrackNames.isEmpty())
            {
                return;
            }


            // Update visible background phrases
            Map<Integer, Phrase> res = new HashMap<>();
            for (var name : visibleBackgroundTrackNames)
            {
                int channel = getChannelFromPhraseName(name);
                var rv = midiMix.getRhythmVoice(channel);
                var p = lastResult.mapRvPhrases().get(rv);
                assert p != null : "rv=" + rv + " lastResult.mapRvPhrases()=" + lastResult.mapRvPhrases();
                res.put(channel, p);
            }

            editor.setBackgroundPhases(res);
        });
    }

    /**
     * Song Midi mix has changed, update the available background phrase names.
     */
    private void songMidiMixChanged()
    {
        List<String> names = new ArrayList<>();
        for (int ch : midiMix.getUsedChannels())
        {
            if (ch == editor.getChannel())
            {
                continue;
            }
            String name = buildPhraseName(ch);
            names.add(name);
        }
        backgroundPhrasesPanel.setTracks(names);
    }

    private String buildPhraseName(int channel)
    {
        String rvName = midiMix.getRhythmVoice(channel).getName();
        String inst = midiMix.getInstrumentMixFromChannel(channel).getInstrument().getPatchName();
        String name = String.format("%d: %s - %s", channel + 1, rvName, inst);
        return name;
    }

    private int getChannelFromPhraseName(String name)
    {
        int index = name.indexOf(":");
        assert index != -1 : "name=" + name;
        return Integer.parseInt(name.substring(0, index)) - 1;
    }


    private Phrase getBackgroundPhrase(int channel)
    {
        return PhraseSamples.getRandomPhrase(channel, 7, 16);
    }

    //=============================================================================
    // PropertyChangeListener interface
    //=============================================================================
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getSource() == midiMix)
        {
            if (evt.getPropertyName().equals(MidiMix.PROP_CHANNEL_INSTRUMENT_MIX))
            {
                songMidiMixChanged();
            } else if (evt.getPropertyName().equals(MidiMix.PROP_RHYTHM_VOICE))
            {
                songMidiMixChanged();
            }
        } else if (evt.getSource() == backgroundPhrasesPanel)
        {
            if (evt.getPropertyName().equals(BackgroundPhrasesPanel.PROP_VISIBLE_TRACK_NAMES))
            {
                selectionChanged((List<String>) evt.getNewValue());
            }
        }
    }


}

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
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import org.jjazz.leadsheet.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.leadsheet.chordleadsheet.api.ClsChangeListener;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.leadsheet.chordleadsheet.api.event.ClsActionEvent;
import org.jjazz.leadsheet.chordleadsheet.api.event.ClsChangeEvent;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.musiccontrol.api.PlaybackSettings;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.pianoroll.api.PianoRollEditor;
import org.jjazz.pianoroll.api.PianoRollEditorTopComponent;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_CustomPhrase;
import org.jjazz.rhythmmusicgeneration.api.MusicGenerationQueue;
import org.jjazz.song.api.Song;
import org.jjazz.songcontext.api.SongContext;
import org.jjazz.songstructure.api.SgsChangeListener;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.event.RpValueChangedEvent;
import org.jjazz.songstructure.api.event.SgsActionEvent;
import org.jjazz.songstructure.api.event.SgsChangeEvent;
import org.openide.util.ChangeSupport;

/**
 * Manage the generation of background phrases and coordination with UI elements.
 */
public class BackgroundPhraseManager implements PropertyChangeListener
{

    private MusicGenerationQueue.Result lastResult;
    private MusicGenerationQueue musicGenerationQueue;
    private NeedMusicGenerationListener needMusicGenerationListener;
    private final PianoRollEditorTopComponent topComponent;
    private final PianoRollEditor editor;
    private RP_SYS_CustomPhrase rpCustomPhrase;
    private final BackgroundPhrasesPanel backgroundPhrasesPanel;
    private static final Logger LOGGER = Logger.getLogger(BackgroundPhraseManager.class.getSimpleName());

    public BackgroundPhraseManager(PianoRollEditorTopComponent preTc, BackgroundPhrasesPanel bgPhrasesPanel)
    {
        this.topComponent = preTc;
        this.editor = preTc.getEditor();
        this.backgroundPhrasesPanel = bgPhrasesPanel;


        this.topComponent.getMidiMix().addPropertyChangeListener(this);
        updateTrackNames();


        // Listen to user selection changes
        backgroundPhrasesPanel.addPropertyChangeListener(BackgroundPhrasesPanel.PROP_SELECTED_TRACK_NAMES, this);


        musicGenerationQueue = new MusicGenerationQueue(300, 600);
        musicGenerationQueue.addChangeListener(e -> musicGenerationResultReceived(musicGenerationQueue.getLastResult()));
    }

    /**
     * Refresh the track names in the backgroundPhrasesPanel.
     * <p>
     */
    public void updateTrackNames()
    {
        List<String> names = new ArrayList<>();
        for (int ch : topComponent.getMidiMix().getUsedChannels())
        {
            if (ch == editor.getChannel())
            {
                continue;
            }
            String name = buildPhraseName(ch);
            names.add(name);
        }
        lastResult = null;    // Important to make sure we regenerate music when editor's model changes eg from a user track edit to a RP_SYS_CustomPhrase edit
        backgroundPhrasesPanel.setTracks(names);        // This will clear selection
    }

    public RP_SYS_CustomPhrase getRpCustomPhrase()
    {
        return rpCustomPhrase;
    }

    public void setRpCustomPhrase(RP_SYS_CustomPhrase rpCustomPhrase)
    {
        this.rpCustomPhrase = rpCustomPhrase;
    }

    public void cleanup()
    {
        topComponent.getMidiMix().removePropertyChangeListener(this);
        backgroundPhrasesPanel.removePropertyChangeListener(this);
        musicGenerationQueue.stop();
        if (needMusicGenerationListener != null)
        {
            needMusicGenerationListener.cleanup();
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
            if (needMusicGenerationListener != null)
            {
                needMusicGenerationListener.cleanup();
            }
            return;
        }


        if (!musicGenerationQueue.isRunning())
        {
            // Start music generation thread
            musicGenerationQueue.start();


            // Regenerate music each time song context is musically updated -except for our own phrase change events
            needMusicGenerationListener = new NeedMusicGenerationListener(editor.getSong(), topComponent.getMidiMix());
            needMusicGenerationListener.addChangeListener(e -> 
            {
                LOGGER.fine("selectionChanged() requesting music generation...");
                musicGenerationQueue.add(new SongContext(editor.getSong(), topComponent.getMidiMix()));
            });
        }


        // Check if we already got all the phrases
        Map<Integer, Phrase> map = null;
        if (lastResult != null && lastResult.mapRvPhrases() != null)
        {
            map = new HashMap<>();
            for (var n : selectedNames)
            {
                int channel = getChannelFromPhraseName(n);
                RhythmVoice rv = topComponent.getMidiMix().getRhythmVoice(channel);
                assert rv != null : " channel=" + channel + " midiMix=" + topComponent.getMidiMix();
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
            SongContext sgContext = new SongContext(topComponent.getSong(), topComponent.getMidiMix(), editor.getPhraseBarRange());
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
                var rv = topComponent.getMidiMix().getRhythmVoice(channel);
                var p = lastResult.mapRvPhrases().get(rv);
                assert p != null : "rv=" + rv + " lastResult.mapRvPhrases()=" + lastResult.mapRvPhrases();
                res.put(channel, p);
            }

            editor.setBackgroundPhases(res);
        });
    }


    private String buildPhraseName(int channel)
    {
        String rvName = topComponent.getMidiMix().getRhythmVoice(channel).getName();
        String inst = topComponent.getMidiMix().getInstrumentMix(channel).getInstrument().getPatchName();
        String name = String.format("%d: %s - %s", channel + 1, rvName, inst);
        return name;
    }

    private int getChannelFromPhraseName(String name)
    {
        int index = name.indexOf(":");
        assert index != -1 : "name=" + name;
        return Integer.parseInt(name.substring(0, index)) - 1;
    }


    //=============================================================================
    // PropertyChangeListener interface
    //=============================================================================
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getSource() == topComponent.getMidiMix())
        {
            switch (evt.getPropertyName())
            {
                case MidiMix.PROP_CHANNEL_INSTRUMENT_MIX, MidiMix.PROP_RHYTHM_VOICE, MidiMix.PROP_RHYTHM_VOICE_CHANNEL ->
                {
                    updateTrackNames();
                }
                default ->
                {
                }
            }
        } else if (evt.getSource() == backgroundPhrasesPanel)
        {
            if (evt.getPropertyName().equals(BackgroundPhrasesPanel.PROP_SELECTED_TRACK_NAMES))
            {
                selectionChanged((List<String>) evt.getNewValue());
            }
        }
    }


    /**
     * A listener which fires when we need a new music generation.
     * <p>
     */
    private class NeedMusicGenerationListener implements PropertyChangeListener, VetoableChangeListener, SgsChangeListener, ClsChangeListener
    {

        private final ChangeSupport cs = new ChangeSupport(this);
        private final Song song;
        private final SongStructure songStructure;
        private final ChordLeadSheet chordLeadSheet;
        private final MidiMix midiMix;

        public NeedMusicGenerationListener(Song song, MidiMix midiMix)
        {
            this.song = song;
            this.songStructure = song.getSongStructure();
            this.chordLeadSheet = song.getChordLeadSheet();
            this.midiMix = midiMix;

            this.song.addVetoableChangeListener(this);
            this.song.getSongStructure().addSgsChangeListener(this);
            this.song.getChordLeadSheet().addClsChangeListener(this);
            this.midiMix.addPropertyChangeListener(this);
            PlaybackSettings.getInstance().addPropertyChangeListener(this);
        }

        public void cleanup()
        {
            this.song.removeVetoableChangeListener(this);
            this.songStructure.removeSgsChangeListener(this);
            this.chordLeadSheet.removeClsChangeListener(this);
            this.midiMix.removePropertyChangeListener(this);
            PlaybackSettings.getInstance().removePropertyChangeListener(this);
        }

        /**
         * Add a listener to be notified when a music generation impacting change has occured.
         *
         * @param listener
         */
        public void addChangeListener(ChangeListener listener)
        {
            cs.addChangeListener(listener);
        }

        public void removeChangeListener(ChangeListener listener)
        {
            cs.removeChangeListener(listener);
        }

        // ========================================================================================================
        // PropertyChangeListener
        // ========================================================================================================   
        @Override
        public void propertyChange(PropertyChangeEvent evt)
        {
            boolean fire = false;

            if (evt.getSource() == song)
            {
                switch (evt.getPropertyName())
                {
                    case Song.PROP_VETOABLE_USER_PHRASE_CONTENT ->
                    {
                        // Ignore user phrase changes which come from our PianoRollEditor
                        String name = (String) evt.getNewValue();
                        RhythmVoice rv = midiMix.getUserRhythmVoice(name);
                        assert rv != null : "name=" + name + " midiMix=" + midiMix;
                        RhythmVoice rvEditor = midiMix.getRhythmVoice(editor.getChannel());
                        fire = rv != rvEditor;
                    }
                    case Song.PROP_VETOABLE_USER_PHRASE ->
                    {
                        // A user phrase was added or removed
                        fire = true;
                    }
                    default ->
                    {

                    }
                }
            } else if (evt.getSource() == midiMix)
            {
                switch (evt.getPropertyName())
                {
                    case MidiMix.PROP_CHANNEL_DRUMS_REROUTED, MidiMix.PROP_CHANNEL_INSTRUMENT_MIX, MidiMix.PROP_DRUMS_INSTRUMENT_KEYMAP, MidiMix.PROP_INSTRUMENT_TRANSPOSITION, MidiMix.PROP_INSTRUMENT_VELOCITY_SHIFT ->
                    {
                        fire = true;
                    }
                    case MidiMix.PROP_RHYTHM_VOICE, MidiMix.PROP_RHYTHM_VOICE_CHANNEL ->
                    {
                        // If this happens, the PianoRollEditor's model must have been reset by caller
                    }
                    default ->
                    {

                    }
                }
            } else if (evt.getSource() == PlaybackSettings.getInstance())
            {
                switch (evt.getPropertyName())
                {
                    case PlaybackSettings.PROP_CLICK_PITCH_HIGH, PlaybackSettings.PROP_CLICK_PITCH_LOW, PlaybackSettings.PROP_CLICK_VELOCITY_HIGH, PlaybackSettings.PROP_CLICK_VELOCITY_LOW, PlaybackSettings.PROP_PLAYBACK_KEY_TRANSPOSITION ->
                    {
                        fire = true;
                    }
                    default ->
                    {
                    }
                }
            }

            if (fire)
            {
                cs.fireChange();
            }
        }
        // ========================================================================================================
        // VetoableChangeListener
        // ========================================================================================================   

        @Override
        public void vetoableChange(PropertyChangeEvent evt) throws PropertyVetoException
        {
            propertyChange(evt);
        }

        // ========================================================================================================
        // SgsChangeListener
        // ========================================================================================================        

        @Override
        public void authorizeChange(SgsChangeEvent e) throws UnsupportedEditException
        {
            // Nothing
        }

        @Override
        public void songStructureChanged(SgsChangeEvent e)
        {
            boolean fire = false;

            if (e instanceof RpValueChangedEvent ce)
            {
                // Ignore changes coming from the edited RP_SYS_CustomPhrase (even if it could be, theorically, coming from another RhythmVoice)
                fire = ce.getRhythmParameter() != rpCustomPhrase;

            } else if (e instanceof SgsActionEvent ae && ae.isActionComplete() && !ae.getActionId().startsWith("setRhythmParameterValue"))
            {
                fire = true;
            }

            if (fire)
            {
                cs.fireChange();
            }
        }

        // ========================================================================================================
        // ClsChangeListener
        // ========================================================================================================        

        @Override
        public void authorizeChange(ClsChangeEvent e) throws UnsupportedEditException
        {
            // Nothing
        }

        @Override
        public void chordLeadSheetChanged(ClsChangeEvent e)
        {
            if (e instanceof ClsActionEvent ae && ae.isActionComplete())
            {
                cs.fireChange();
            }
        }

    }


}

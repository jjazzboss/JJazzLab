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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.jjazz.activesong.api.BackgroundSongMusicBuilder;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.pianoroll.api.PianoRollEditor;
import org.jjazz.pianoroll.api.PianoRollEditorTopComponent;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_CustomPhrase;
import org.jjazz.rhythmmusicgeneration.api.MusicGenerationQueue;

/**
 * Manage the update of background phrases and coordination with UI elements.
 */
public class BackgroundPhraseManager implements PropertyChangeListener
{

    private MusicGenerationQueue.Result lastResult;
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

        // Update track names when midi mix changes
        this.topComponent.getMidiMix().addPropertyChangeListener(this);
        updateTrackNames();


        // Get notified of new song phrases
        var bsmb = BackgroundSongMusicBuilder.getInstance();
        bsmb.addPropertyChangeListener(this);
        lastResult = bsmb.getLastResult();          // Might be null, but important to get some data if PianoRollEditor is created after bsmb produced a result

        // Listen to user selection changes
        backgroundPhrasesPanel.addPropertyChangeListener(BackgroundPhrasesPanel.PROP_SELECTED_TRACK_NAMES, this);


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
        BackgroundSongMusicBuilder.getInstance().removePropertyChangeListener(this);

    }

    //=============================================================================
    // Private methods
    //=============================================================================

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
            LOGGER.log(Level.FINE, "musicGenerationResultReceived() User error music generation exception ex={0}",
                    lastResult.userException().getMessage());
            lastResult = null;
            return;
        }

        updateBackgroundPhrases();

    }

    private void updateBackgroundPhrases()
    {
        SwingUtilities.invokeLater(() -> 
        {
            var selectedPhraseNames = backgroundPhrasesPanel.getSelectedTracks();
            if (selectedPhraseNames.isEmpty() || lastResult == null)
            {
                editor.setBackgroundPhases(null);
                return;
            }


            // Update selected background phrases
            Map<Integer, Phrase> mapChannelPhrase = new HashMap<>();
            for (var name : selectedPhraseNames)
            {
                int channel = getChannelFromPhraseName(name);
                var rv = topComponent.getMidiMix().getRhythmVoice(channel);
                var p = lastResult.mapRvPhrases().get(rv);
                assert p != null : "rv=" + rv + " lastResult.mapRvPhrases()=" + lastResult.mapRvPhrases();
                mapChannelPhrase.put(channel, p);
            }

            editor.setBackgroundPhases(mapChannelPhrase);
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
                updateBackgroundPhrases();
            }
        } else if (evt.getSource() == BackgroundSongMusicBuilder.getInstance())
        {
            if (evt.getPropertyName().equals(BackgroundSongMusicBuilder.PROP_MUSIC_GENERATION_RESULT))
            {
                musicGenerationResultReceived((MusicGenerationQueue.Result) evt.getNewValue());
            }
        }
    }

}

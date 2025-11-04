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
package org.jjazz.pianoroll.api;

import com.google.common.base.Preconditions;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.SwingPropertyChangeSupport;
import org.jjazz.midi.api.InstrumentMix;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.musiccontrol.spi.ActiveSongBackgroundMusicBuilder;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.musiccontrol.api.MusicGenerationQueue.Result;

/**
 * The ghost phrases (one per channel, excepted for the edited channel) and their visible state.
 */
public class GhostPhrasesModel implements PropertyChangeListener, ChangeListener
{

    /**
     * A phrase was added or removed or renamed or channel changed in the midimix.
     * <p>
     * The visible channels might also have been changed as a result.
     */
    public static final String PROP_PHRASE_LIST = "PropPhraseList";
    /**
     * One or more phrases content were musically changed for the visible phrases.
     */
    public static final String PROP_VISIBLE_PHRASE_CONTENT = "PropVisiblePhraseContent";
    /**
     * A phrase was made visible or hidden.
     * <p>
     * OldValue=old visible channels set, newValue=new visible channels set.
     */
    public static final String PROP_VISIBLE_PHRASE_SELECTION = "PropVisiblePhraseSelection";

    private Result lastResult;
    private final MidiMix midiMix;
    private volatile Map<Integer, Phrase> mapChannelPhrase = new HashMap<>();
    private volatile Set<Integer> visibleChannels = new HashSet<>();
    private int editedChannel;
    private final SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(GhostPhrasesModel.class.getSimpleName());


    /**
     * Create the model.
     * <p>
     * By default no ghost phrase is visible.
     *
     * @param midiMix
     * @param editedChannel The channel currently edited, which does not require a GhostPhrase.
     */
    public GhostPhrasesModel(MidiMix midiMix, int editedChannel)
    {
        Objects.requireNonNull(midiMix);

        setEditedChannel(editedChannel);
        this.midiMix = midiMix;
        this.lastResult = null;


        midiMix.addPropertyChangeListener(this);


        // Get notified of when new song phrases are generated
        var asmb = ActiveSongBackgroundMusicBuilder.getDefault();
        asmb.addChangeListener(this);


        if (asmb.isLastResultUpToDate())
        {
            // Important to get some data if PianoRollEditor is created after bsmb produced a result
            musicGenerationResultReceived(asmb.getLastResult());
        }

    }

    public int getEditedChannel()
    {
        return editedChannel;
    }

    /**
     * Set the channel currently edited, which does not require a GhostPhrase.
     *
     * @param editedChannel
     */
    public void setEditedChannel(int editedChannel)
    {
        Preconditions.checkArgument(MidiConst.checkMidiChannel(editedChannel), "editedChannel=%s", editedChannel);
        this.editedChannel = editedChannel;
        visibleChannels.remove(editedChannel);
        pcs.firePropertyChange(PROP_PHRASE_LIST, false, true);
    }

    public MidiMix getMidiMix()
    {
        return midiMix;
    }

    /**
     * Set which ghost phrases are visible.
     *
     * @param newVisibleChannels Can be null or empty to not show any ghost phrase.
     */
    public void setVisibleChannels(Collection<Integer> newVisibleChannels)
    {
        var old = visibleChannels;
        visibleChannels = newVisibleChannels == null ? new HashSet<>() : new HashSet<>(newVisibleChannels);
        visibleChannels.remove(editedChannel);
        pcs.firePropertyChange(PROP_VISIBLE_PHRASE_SELECTION, old, visibleChannels);
    }

    /**
     * Get all the channels, visible or not, except the edited channel.
     *
     * @return
     */
    public List<Integer> getAllChannels()
    {
        return midiMix.getUsedChannels().stream()
                .filter(c -> c != editedChannel)
                .toList();
    }

    /**
     * Get the channels for which ghost phrases are visible.
     *
     * @return
     */
    public Set<Integer> getVisibleChannels()
    {
        return visibleChannels;
    }


    /**
     * Get the visible ghost phrases.
     *
     * @return
     */
    public Map<Integer, Phrase> getVisibleGhostPhrases()
    {
        Map<Integer, Phrase> res = new HashMap<>();
        for (var channel : visibleChannels)
        {
            res.put(channel, mapChannelPhrase.get(channel));
        }
        return res;
    }

    public void cleanup()
    {
        midiMix.removePropertyChangeListener(this);
        ActiveSongBackgroundMusicBuilder.getDefault().removeChangeListener(this);

    }

    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }

    //=============================================================================
    // ChangeListener interface
    //=============================================================================
    @Override
    public void stateChanged(ChangeEvent evt)
    {
        musicGenerationResultReceived(ActiveSongBackgroundMusicBuilder.getDefault().getLastResult());
    }

    //=============================================================================
    // PropertyChangeListener interface
    //=============================================================================
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getSource() == midiMix)
        {
            switch (evt.getPropertyName())
            {
                case MidiMix.PROP_CHANNEL_INSTRUMENT_MIX ->
                {
                    // Added, replaced or removed
                    int channel = (int) evt.getNewValue();
                    var insMix = midiMix.getInstrumentMix(channel);
                    var oldInsMix = (InstrumentMix) evt.getOldValue();
                    if (insMix == null && oldInsMix != null)
                    {
                        // Removed mix
                        visibleChannels.remove(channel);
                    }
                    pcs.firePropertyChange(PROP_PHRASE_LIST, false, true);


                    if (insMix != null && oldInsMix != null && visibleChannels.contains(channel))
                    {
                        // The InstrumentMix of a visible ghostPhrase was replaced, this might impact the ghost phrase
                        pcs.firePropertyChange(PROP_VISIBLE_PHRASE_CONTENT, false, true);
                    }
                }
                case MidiMix.PROP_RHYTHM_VOICE -> pcs.firePropertyChange(PROP_PHRASE_LIST, false, true);

                case MidiMix.PROP_RHYTHM_VOICE_CHANNEL ->
                {
                    int oldChannel = (int) evt.getOldValue();
                    int newChannel = (int) evt.getNewValue();
                    if (visibleChannels.contains(oldChannel))
                    {
                        visibleChannels.remove(oldChannel);
                        visibleChannels.add(newChannel);
                    }
                    pcs.firePropertyChange(PROP_PHRASE_LIST, false, true);
                }

                default ->
                {
                }
            }
        }
    }

    //=============================================================================
    // Private methods
    //=============================================================================

    /**
     * A music generation task is complete, phrases may have changed.
     *
     * @param result
     */
    private void musicGenerationResultReceived(Result result)
    {
        Preconditions.checkNotNull(result);
        LOGGER.fine("musicGenerationResultReceived() -- ");
        lastResult = result;

        if (lastResult.throwable() != null)
        {
            // Problem occured, ignore
            LOGGER.log(Level.FINE, "musicGenerationResultReceived() Error music generation error ex={0}",
                    lastResult.throwable().getMessage());
            lastResult = null;
            return;
        }

        // Only update the phrases content
        Map<Integer, Phrase> newMap = new HashMap<>();
        for (int channel : midiMix.getUsedChannels())
        {
            var rv = midiMix.getRhythmVoice(channel);
            Phrase p = lastResult == null ? new Phrase(channel) : lastResult.mapRvPhrases().get(rv);
            if (p != null)      // Might happen if lastResult does not contain a new user track just added ?
            {
                newMap.put(channel, p);
            }
        }
        mapChannelPhrase = newMap;

        pcs.firePropertyChange(PROP_VISIBLE_PHRASE_CONTENT, false, true);

    }


}

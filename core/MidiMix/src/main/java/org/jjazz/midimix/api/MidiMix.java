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
package org.jjazz.midimix.api;

import com.google.common.base.Preconditions;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MidiUnavailableException;
import javax.swing.event.SwingPropertyChangeSupport;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.UndoableEdit;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.midi.api.Instrument;
import org.jjazz.midi.api.synths.GM1Instrument;
import org.jjazz.midi.api.InstrumentMix;
import org.jjazz.midi.api.InstrumentSettings;
import org.jjazz.midi.api.JJazzMidiSystem;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midi.api.synths.InstrumentFamily;
import org.jjazz.midi.api.synths.GMSynth;
import org.jjazz.midimix.spi.MidiMixManager;
import org.jjazz.midimix.spi.RhythmVoiceInstrumentProvider;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.rhythm.api.AdaptedRhythm;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.api.RhythmVoiceDelegate;
import org.jjazz.rhythm.spi.RhythmDirsLocator;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.jjazz.rhythmdatabase.api.UnavailableRhythmException;
import org.jjazz.songstructure.api.event.SgsChangeEvent;
import org.jjazz.songstructure.api.event.SptAddedEvent;
import org.jjazz.songstructure.api.event.SptRemovedEvent;
import org.jjazz.songstructure.api.event.SptReplacedEvent;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongCreationException;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SgsChangeListener;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.songstructure.api.event.SgsVetoableChangeEvent;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.undomanager.api.SimpleEdit;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.utilities.api.Utilities;
import org.jjazz.xstream.api.XStreamInstancesManager;
import org.jjazz.xstream.spi.XStreamConfigurator;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.MIDIMIX_SAVE;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_LOAD;
import static org.jjazz.xstream.spi.XStreamConfigurator.InstanceId.SONG_SAVE;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.lookup.ServiceProvider;

/**
 * A set of up to 16 InstrumentMixes, 1 per Midi channel with 1 RhythmVoice associated.
 * <p>
 * The object manages the solo functionality between the InstrumentMixes.<p>
 * A Song can be associated to the MidiMix so that InstrumentMixes are kept up to date with song's songStructure and user phrase changes.<p>
 * If MidiMix is modified the corresponding property change event is fired (e.g. PROP_INSTRUMENT_MUTE) then the PROP_MODIFIED_OR_SAVED_OR_RESET change event is
 * also fired.
 * <p>
 */

public class MidiMix implements SgsChangeListener, PropertyChangeListener, VetoableChangeListener, Serializable
{

    public static final String MIX_FILE_EXTENSION = "mix";
    /**
     * Added, replaced or removed InstrumentMix.
     * <p>
     * OldValue=the old InstrumentMix (null if new InstrumentMix added), newValue=channel
     */
    public static final String PROP_CHANNEL_INSTRUMENT_MIX = "ChannelInstrumentMix";
    /**
     * oldValue=channel, newValue=true/false.
     */
    public static final String PROP_CHANNEL_DRUMS_REROUTED = "ChannelDrumsRerouted";
    /**
     * oldValue=InstumentMix, newValue=mute boolean state.
     */
    public static final String PROP_INSTRUMENT_MUTE = "InstrumentMute";
    /**
     * A drums instrument has changed with different keymap.
     * <p>
     * oldValue=channel, newValue=old keymap (may be null)
     */
    public static final String PROP_DRUMS_INSTRUMENT_KEYMAP = "DrumsInstrumentKeyMap";
    /**
     * oldValue=InstumentMix, newValue=transposition value.
     */
    public static final String PROP_INSTRUMENT_TRANSPOSITION = "InstrumentTransposition";
    /**
     * oldValue=InstumentMix, newValue=velocity shift value.
     */
    public static final String PROP_INSTRUMENT_VELOCITY_SHIFT = "InstrumentVelocityShift";
    /**
     * A RhythmVoice has replaced another one, e.g. this is used to change the name of a user track.
     * <p>
     * oldValue=old RhythmVoice, newValue=new RhythmVoice
     */
    public static final String PROP_RHYTHM_VOICE = "RhythmVoice";
    /**
     * The channel of a RhythmVoice has been changed by user.
     * <p>
     * oldValue=old channel, newValue=new channel
     */
    public static final String PROP_RHYTHM_VOICE_CHANNEL = "RhythmVoiceChannel";
    /**
     * This property changes when the MidiMix is modified (false-&gt;true) or saved (true-&gt;false).
     */
    public static final String PROP_MODIFIED_OR_SAVED = "PROP_MIDIMIX_MODIFIED_OR_SAVED";
    /**
     * Fired each time a MidiMix parameter which impacts music generation is modified, like instrument transposition.
     * <p>
     * OldValue=the property name that triggers the musical change, newValue=optional associated data.<p>
     * Use PROP_MODIFIED_OR_SAVED to get notified of any MidiMix change, including non-musical ones like track mute change, etc.
     */
    public static final String PROP_MUSIC_GENERATION = "MidiMixMusicContent";
    public static final int NB_AVAILABLE_CHANNELS = MidiConst.CHANNEL_MAX - MidiConst.CHANNEL_MIN + 1;

    /**
     * Store the instrumentMixes, one per Midi Channel.
     */
    private final InstrumentMix[] instrumentMixes = new InstrumentMix[MidiConst.CHANNEL_MIN + NB_AVAILABLE_CHANNELS];
    /**
     * Store the RhythmVoices associated to an instrumentMix, one per channel.
     */
    private final RhythmVoice[] rhythmVoices = new RhythmVoice[MidiConst.CHANNEL_MIN + NB_AVAILABLE_CHANNELS];
    /**
     * The InstrumentMixes with Solo ON
     */
    private final transient HashSet<InstrumentMix> soloedInsMixes = new HashSet<>();
    /**
     * The channels which should be rerouted to the GM DRUMS channel, and the related saved config.
     */
    private final transient HashMap<Integer, InstrumentMix> drumsReroutedChannels = new HashMap<>();
    /**
     * Saved Mute configuration on first soloed channel
     */
    private final transient boolean[] saveMuteConfiguration = new boolean[MidiConst.CHANNEL_MIN + NB_AVAILABLE_CHANNELS];
    private final transient List<UndoableEditListener> undoListeners = new ArrayList<>();
    /**
     * The file where MidiMix was saved.
     */
    private transient File file;
    private transient Song song;
    private transient boolean needSave = false;
    private final SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(MidiMix.class.getSimpleName());

    /**
     * Create an empty MidiMix.
     */
    public MidiMix()
    {

    }

    /**
     * create an empty MidiMix and associate it to specified song.
     *
     * @param s
     */
    public MidiMix(Song s)
    {
        setSong(s);
    }


    /**
     * Get a deep copy of this MidiMix.
     * <p>
     * Mutable internal objects are deeply copied, e.g. InstrumentMixes.<br>
     * Not copied: undoableListeners, isSaveNeeded.
     *
     * @return
     */
    public synchronized MidiMix getDeepCopy()
    {
        MidiMix mm = new MidiMix();
        mm.song = song;
        mm.file = file;


        System.arraycopy(rhythmVoices, 0, mm.rhythmVoices, 0, rhythmVoices.length);
        System.arraycopy(saveMuteConfiguration, 0, mm.saveMuteConfiguration, 0, saveMuteConfiguration.length);

        for (int i = 0; i < instrumentMixes.length; i++)
        {
            var insMix = instrumentMixes[i];
            mm.instrumentMixes[i] = insMix == null ? null : new InstrumentMix(insMix);
            if (soloedInsMixes.contains(insMix))
            {
                mm.soloedInsMixes.add(mm.instrumentMixes[i]);
            }
        }

        for (int channel : drumsReroutedChannels.keySet())
        {
            assert mm.instrumentMixes[channel] != null : "this=" + this;
            mm.drumsReroutedChannels.put(channel, mm.instrumentMixes[channel]);
        }

        return mm;
    }

    /**
     * Associate a song to this MidiMix : listen to song changes to keep this MidiMix consistent.
     * <p>
     * Listen to rhythms and user phrases changes.
     *
     * @param sg Can be null.
     * @throws IllegalArgumentException If checkConsistency(sg, false) fails.
     */
    public final void setSong(Song sg)
    {
        if (song == sg)
        {
            return;
        }
        if (song != null)
        {
            song.getSongStructure().removeSgsChangeListener(this);
            song.removeVetoableChangeListener(this);  // User phrase events
        }

        this.song = sg;


        if (song != null)
        {
            SongStructure sgs = song.getSongStructure();

            try
            {
                checkConsistency(song, false);
            } catch (SongCreationException ex)
            {
                throw new IllegalArgumentException(ex);
            }

            // Register for changes
            song.addVetoableChangeListener(this);
            sgs.addSgsChangeListener(this);
        }
    }

    /**
     * Check if this MidiMix is consistent with the specified song.
     * <p>
     * Check that all RhythmVoices of this MidiMix belong to song rhythms. Check user tracks consistency between midiMix and song.
     *
     * @param sg
     * @param fullCheck If true also check that all song RhythmVoices are used in this MidiMix.
     * @throws org.jjazz.song.api.SongCreationException If an inconsistency is detected
     *
     */
    public void checkConsistency(Song sg, boolean fullCheck) throws SongCreationException
    {
        List<RhythmVoice> sgRvs = sg.getSongStructure().getUniqueRhythmVoices(true, false);

        for (Integer channel : getUsedChannels())
        {
            RhythmVoice rv = getRhythmVoice(channel);
            if (rv instanceof UserRhythmVoice)
            {
                // Check that we have the corresponding phrase in the song
                Phrase p = sg.getUserPhrase(rv.getName());
                if (p == null)
                {
                    // No, try to fix it
                    p = new Phrase(getChannel(rv));
                    try
                    {
                        LOGGER.log(Level.WARNING, "checkConsistency() missing user phrase for UserRhythmVoice {0} in song {1}. Fixed.",
                                new Object[]
                                {
                                    rv.getName(),
                                    sg.getName()
                                });
                        sg.setUserPhrase(rv.getName(), p);
                    } catch (PropertyVetoException ex)
                    {
                        throw new SongCreationException(ex.getMessage());
                    }
                }
            } else if (!sgRvs.contains(rv))
            {
                throw new SongCreationException("channel=" + channel + " rv=" + rv + " sgRvs=" + sgRvs);
            }
        }


        if (fullCheck)
        {
            for (RhythmVoice rv : sgRvs)
            {
                if (getChannel(rv) == -1)
                {
                    throw new SongCreationException("song rv=" + rv + " not found in MidiMix " + toString());
                }
            }

            var rvs = getRhythmVoices();
            for (String userPhraseName : sg.getUserPhraseNames())
            {
                if (!rvs.stream().anyMatch(rv -> rv.getName().equals(userPhraseName)))
                {
                    throw new SongCreationException("missing RhythmVoice for song user phrase " + userPhraseName);
                }
            }
        }


    }

    public Song getSong()
    {
        return song;
    }


    /**
     * Return the list user phrase Midi channels.
     *
     * @return
     */
    public synchronized List<Integer> getUserChannels()
    {
        List<Integer> res = new ArrayList<>();
        for (int i = MidiConst.CHANNEL_MIN; i <= MidiConst.CHANNEL_MAX; i++)
        {
            if (rhythmVoices[i] instanceof UserRhythmVoice)
            {
                res.add(i);
            }
        }
        return res;
    }

    /**
     * Return the subset of RhythmVoices which are UserRhythmVoices.
     *
     * @return
     */
    public synchronized List<UserRhythmVoice> getUserRhythmVoices()
    {
        List<UserRhythmVoice> res = new ArrayList<>();
        for (int i = MidiConst.CHANNEL_MIN; i <= MidiConst.CHANNEL_MAX; i++)
        {
            if (rhythmVoices[i] instanceof UserRhythmVoice urv)
            {
                res.add(urv);
            }
        }
        return res;
    }

    /**
     * Get the user phrase RhythmVoice key for the specified name.
     *
     * @param name
     * @return Null if not found
     */
    public UserRhythmVoice getUserRhythmVoice(String name)
    {
        return getUserRhythmVoices().stream()
                .filter(urv -> urv.getName().equals(name))
                .findAny()
                .orElse(null);
    }

    /**
     * Assign an InstrumentMix to a midi channel and to a key.
     * <p>
     * Replace any existing InstrumentMix associated to the midi channel. The solo and "drums rerouted channel" status are reset to off for the channel. <br>
     * Fire a PROP_CHANNEL_INSTRUMENT_MIX change event for this channel, and one UndoableEvent.
     *
     * @param channel A valid midi channel number.
     * @param rvKey   Can be null if insMix is also null. If a song is set, must be consistent with its rhythms and user phrases. Can't be a
     *                RhythmVoiceDelegate.
     * @param insMix  Can be null if rvKey is also null.
     * @throws IllegalArgumentException if insMix is already part of this MidiMix for a different channel, or if rvKey is a RhythmVoiceDelegate.
     */
    public void setInstrumentMix(int channel, RhythmVoice rvKey, InstrumentMix insMix)
    {
        if (!MidiConst.checkMidiChannel(channel) || (rvKey instanceof RhythmVoiceDelegate) || (rvKey == null && insMix != null) || (rvKey != null && insMix == null))
        {
            throw new IllegalArgumentException("channel=" + channel + " rvKey=" + rvKey + " insMix=" + insMix);
        }

        LOGGER.log(Level.FINE, "setInstrumentMix() channel={0} rvKey={1} insMix={2}", new Object[]
        {
            channel, rvKey, insMix
        });

        if (rvKey != null && song != null)
        {
            // Check that rvKey belongs to song
            if (!(rvKey instanceof UserRhythmVoice) && !song.getSongStructure().getUniqueRhythmVoices(true, false).contains(rvKey))
            {
                throw new IllegalArgumentException(
                        "channel=" + channel + " rvKey=" + rvKey + " insMix=" + insMix + ". rvKey does not belong to any of the song's rhythms.");
            }
            if ((rvKey instanceof UserRhythmVoice) && !song.getUserPhraseNames().contains(rvKey.getName()))
            {
                throw new IllegalArgumentException("channel=" + channel + " rvKey=" + rvKey
                        + " insMix=" + insMix + " rvKey.getName()=" + rvKey.getName()
                        + " song=" + song.getName() + ". Song does not have a user phrase with the specified name");  // NOI18N
            }
        }


        if (insMix != null)
        {
            // Check the InstrumentMix is not already used for a different channel
            int ch = getInstrumentMixesPerChannel().indexOf(insMix);
            if (ch != -1 && ch != channel)
            {
                throw new IllegalArgumentException(
                        "channel=" + channel + " rvKey=" + rvKey + " im=" + insMix + ". im is already present in MidiMix at channel " + ch);
            }
        }

        changeInstrumentMix(channel, insMix, rvKey);
    }

    /**
     * Replace an existing RhythmVoice by a new one.
     * <p>
     * Fire a PROP_RHYTHM_VOICE and an undoable event.
     *
     * @param oldRv Must be a RhythmVoice used by this MidiMix.
     * @param newRv Must not be already used by this MidiMix.
     */
    public void replaceRhythmVoice(RhythmVoice oldRv, RhythmVoice newRv)
    {
        int channel = getChannel(oldRv);
        Preconditions.checkArgument(channel != -1, "oldRv=%s", oldRv);
        Preconditions.checkArgument(getChannel(newRv) == -1, "newRv=%s", newRv);


        // Change state
        synchronized (this)
        {
            rhythmVoices[channel] = newRv;
        }


        // Prepare the undoable edit
        UndoableEdit edit = new SimpleEdit("Replace RhythmVoice")
        {
            @Override
            public void undoBody()
            {
                synchronized (MidiMix.this)
                {
                    rhythmVoices[channel] = oldRv;
                }

                LOGGER.log(Level.FINER, "replaceRhythmVoice().undoBody oldRv={0} newRv={1}", new Object[]
                {
                    oldRv, newRv
                });

                pcs.firePropertyChange(PROP_RHYTHM_VOICE, newRv, oldRv);
                fireIsMusicGenerationModified(PROP_RHYTHM_VOICE, newRv);
                fireIsModified();
            }

            @Override
            public void redoBody()
            {
                synchronized (MidiMix.this)
                {
                    rhythmVoices[channel] = newRv;
                }

                LOGGER.log(Level.FINER, "replaceRhythmVoice().redoBody oldRv={0} newRv={1}", new Object[]
                {
                    oldRv, newRv
                });

                pcs.firePropertyChange(PROP_RHYTHM_VOICE, oldRv, newRv);
                fireIsMusicGenerationModified(PROP_RHYTHM_VOICE, newRv);
                fireIsModified();
            }
        };
        fireUndoableEditHappened(edit);

        pcs.firePropertyChange(PROP_RHYTHM_VOICE, oldRv, newRv);
        fireIsMusicGenerationModified(PROP_RHYTHM_VOICE, newRv);
        fireIsModified();
    }

    /**
     * Change the channel of a RhythmVoice.
     * <p>
     * Fire a PROP_RHYTHM_VOICE_CHANNEL and an undoable event.
     *
     * @param rv         Must be a RhythmVoice used by this MidiMix.
     * @param newChannel Must be a free channel
     */
    public void setRhythmVoiceChannel(RhythmVoice rv, int newChannel)
    {
        int oldChannel = getChannel(rv);
        Preconditions.checkArgument(oldChannel != -1, "rv=%s", rv);
        Preconditions.checkArgument(getRhythmVoice(newChannel) == null, "newChannel=%s",
                newChannel + " getRhythmVoice(newChannel)=" + getRhythmVoice(newChannel));


        // Change state
        swapChannels(oldChannel, newChannel);


        // Prepare the undoable edit
        UndoableEdit edit = new SimpleEdit("Set RhythmVoice channel")
        {
            @Override
            public void undoBody()
            {
                swapChannels(newChannel, oldChannel);

                LOGGER.log(Level.FINER, "setRhythmVoiceChannel().undoBody oldChannel={0} newChannel={1}", new Object[]
                {
                    oldChannel, newChannel
                });

                pcs.firePropertyChange(PROP_RHYTHM_VOICE_CHANNEL, newChannel, oldChannel);
                fireIsMusicGenerationModified(PROP_RHYTHM_VOICE_CHANNEL, rv);
                fireIsModified();
            }

            @Override
            public void redoBody()
            {
                swapChannels(oldChannel, newChannel);

                LOGGER.log(Level.FINER, "setRhythmVoiceChannel().undoBody oldChannel={0} newChannel={1}", new Object[]
                {
                    oldChannel, newChannel
                });

                pcs.firePropertyChange(PROP_RHYTHM_VOICE_CHANNEL, oldChannel, newChannel);
                fireIsMusicGenerationModified(PROP_RHYTHM_VOICE_CHANNEL, rv);
                fireIsModified();
            }
        };
        fireUndoableEditHappened(edit);


        pcs.firePropertyChange(PROP_RHYTHM_VOICE_CHANNEL, oldChannel, newChannel);
        fireIsMusicGenerationModified(PROP_RHYTHM_VOICE_CHANNEL, rv);
        fireIsModified();
    }


    /**
     * Get the instrumet mix for the specified channel.
     *
     * @param channel A valid midi channel number
     * @return The InstrumentMix assigned to the specified Midi channel, or null if no InstrumentMix for this channel.
     */
    public synchronized InstrumentMix getInstrumentMix(int channel)
    {
        if (!MidiConst.checkMidiChannel(channel))
        {
            throw new IllegalArgumentException("channel=" + channel);
        }
        return instrumentMixes[channel];
    }

    /**
     * Get the instrumet mix for the specified RhythmVoice.
     *
     * @param rv If it's a RhythmVoiceDelegate, return the channel associated to its source RhythmVoice.
     * @return The InstrumentMix associated to rv. Null if no InstrumentMix found.
     */
    public synchronized InstrumentMix getInstrumentMix(RhythmVoice rv)
    {
        if (rv == null)
        {
            return null;
        }
        if (rv instanceof RhythmVoiceDelegate rvd)
        {
            rv = rvd.getSource();
        }
        int index = Arrays.asList(rhythmVoices).indexOf(rv);
        return index == -1 ? null : instrumentMixes[index];
    }


    /**
     * Find the channel corresponding to the specified InstrumentMix.
     *
     * @param im
     * @return -1 if InstrumentMix not found.
     */
    public synchronized int getChannel(InstrumentMix im)
    {
        return Arrays.asList(instrumentMixes).indexOf(im);
    }

    /**
     *
     * @param im
     * @return null if InstrumentMix not found.
     */
    public synchronized RhythmVoice geRhythmVoice(InstrumentMix im)
    {
        int index = getChannel(im);
        return index == -1 ? null : rhythmVoices[index];
    }

    /**
     * Get the RhythmVoice for the specified Midi channel.
     * <p>
     *
     * @param channel
     * @return The RhythmVoice key corresponding to specified channel. Can be null.
     */
    public synchronized RhythmVoice getRhythmVoice(int channel)
    {
        if (!MidiConst.checkMidiChannel(channel))
        {
            throw new IllegalArgumentException("channel=" + channel);
        }
        return rhythmVoices[channel];
    }

    /**
     * Get the Midi channel associated to the specified RhythmVoice.
     *
     * @param rvKey If it's a RhythmVoiceDelegate, return the channel associated to its source RhythmVoice.
     * @return -1 if key not found.
     */
    public synchronized int getChannel(RhythmVoice rvKey)
    {
        if (rvKey == null)
        {
            throw new IllegalArgumentException("key=" + rvKey);
        }
        if (rvKey instanceof RhythmVoiceDelegate rvd)
        {
            rvKey = rvd.getSource();
        }
        return Arrays.asList(rhythmVoices).indexOf(rvKey);
    }

    /**
     * Get the list of used channels in this MidiMix.
     *
     * @return The list of Midi channel numbers for which a non-null InstrumentMix is assigned.
     */
    public synchronized List<Integer> getUsedChannels()
    {
        ArrayList<Integer> channels = new ArrayList<>();
        for (int i = MidiConst.CHANNEL_MIN; i <= MidiConst.CHANNEL_MAX; i++)
        {
            if (instrumentMixes[i] != null)
            {
                channels.add(i);
            }
        }
        return channels;
    }

    /**
     * Get the list of used channels for specified rhythm in this MidiMix.
     *
     * @param r If null return all used channels. If r is an AdaptedRhythm, returns the channels from it source rhythm.
     * @return The list of Midi channel numbers for rhythm r and for which a non-null InstrumentMix is assigned.
     */
    public synchronized List<Integer> getUsedChannels(Rhythm r)
    {
        if (r == null)
        {
            return getUsedChannels();
        }
        r = getSourceRhythm(r);
        ArrayList<Integer> channels = new ArrayList<>();
        for (int i = MidiConst.CHANNEL_MIN; i <= MidiConst.CHANNEL_MAX; i++)
        {
            if (instrumentMixes[i] != null && rhythmVoices[i].getContainer() == r)
            {
                channels.add(i);
            }
        }
        return channels;
    }

    /**
     * @return The list of Midi channel numbers for which no InstrumentMix is assigned.
     */
    public synchronized List<Integer> getUnusedChannels()
    {
        ArrayList<Integer> channels = new ArrayList<>();
        for (int i = MidiConst.CHANNEL_MIN; i <= MidiConst.CHANNEL_MAX; i++)
        {
            if (instrumentMixes[i] == null)
            {
                channels.add(i);
            }
        }
        return channels;
    }

    /**
     * Get all the RhythmVoices corresponding to the non-null InstrumentMixes.
     * <p>
     * Returned list includes UserRhythmVoice instances as well. The list does not contain RhythmVoiceDelegate instances.
     *
     * @return
     */
    public synchronized List<RhythmVoice> getRhythmVoices()
    {
        ArrayList<RhythmVoice> res = new ArrayList<>();
        for (RhythmVoice rvKey : rhythmVoices)
        {
            if (rvKey != null)
            {
                res.add(rvKey);
            }
        }
        return res;
    }

    /**
     * The channels which should be rerouted to the GM Drums channel.
     *
     * @return Note that returned list is not ordered. Can be empty.
     */
    public synchronized List<Integer> getDrumsReroutedChannels()
    {
        return new ArrayList<>(drumsReroutedChannels.keySet());
    }

    /**
     * Enable or disable the rerouting of specified channel to GM Drums channel.
     * <p>
     * If enabled, the related InstrumentMix/Settings will be disabled, and vice versa.
     *
     * @param b
     * @param channel
     */
    public void setDrumsReroutedChannel(boolean b, int channel)
    {
        LOGGER.log(Level.FINE, "setDrumsReroutedChannel() -- b={0} channel={1}", new Object[]
        {
            b, channel
        });
        if (instrumentMixes[channel] == null)
        {
            throw new IllegalArgumentException("b=" + b + " channel=" + channel + " instrumentMixes=" + getInstrumentMixesPerChannel());
        }


        if (b == drumsReroutedChannels.keySet().contains(channel) || channel == MidiConst.CHANNEL_DRUMS)
        {
            return;
        }


        InstrumentMix insMix = instrumentMixes[channel];
        synchronized (this)
        {
            if (b)
            {
                // Save state
                InstrumentMix saveMixData = new InstrumentMix(insMix);
                drumsReroutedChannels.put(channel, saveMixData);


                // Disable all parameters since it's rerouted
                insMix.setInstrumentEnabled(false);
                insMix.getSettings().setChorusEnabled(false);
                insMix.getSettings().setReverbEnabled(false);
                insMix.getSettings().setPanoramicEnabled(false);
                insMix.getSettings().setVolumeEnabled(false);
            } else
            {
                InstrumentMix saveMixData = drumsReroutedChannels.get(channel);
                assert saveMixData != null : "b=" + b + " channel=" + channel + " this=" + this;
                drumsReroutedChannels.remove(channel);


                // Restore parameters enabled state
                insMix.setInstrumentEnabled(saveMixData.isInstrumentEnabled());
                insMix.getSettings().setChorusEnabled(saveMixData.getSettings().isChorusEnabled());
                insMix.getSettings().setReverbEnabled(saveMixData.getSettings().isReverbEnabled());
                insMix.getSettings().setPanoramicEnabled(saveMixData.getSettings().isPanoramicEnabled());
                insMix.getSettings().setVolumeEnabled(saveMixData.getSettings().isVolumeEnabled());
            }
        }

        pcs.firePropertyChange(PROP_CHANNEL_DRUMS_REROUTED, channel, b);
        fireIsMusicGenerationModified(PROP_CHANNEL_DRUMS_REROUTED, channel);
        fireIsModified();
    }

    /**
     * Get the channels which normally need drums rerouting.
     * <p>
     * A channel needs rerouting if all the following conditions are met:<br>
     * 0/ InsMix at MidiConst.CHANNEL_DRUMS has its instrument Midi message enabled <br>
     * 1/ channel != MidiConst.CHANNEL_DRUMS <br>
     * 2/ rv.isDrums() == true and rerouting is not already enabled <br>
     * 3/ instrument (or new instrument if one is provided in the mapChannelNewIns parameter) is the VoidInstrument<br>
     *
     * @param mapChannelNewIns Optional channel instruments to be used for the exercise. Ignored if null. See OutputSynth.getNeedFixInstruments().
     * @return Can be empty
     */
    public List<Integer> getChannelsNeedingDrumsRerouting(HashMap<Integer, Instrument> mapChannelNewIns)
    {
        List<Integer> res = new ArrayList<>();


        var channelDrumsInsMix = getInstrumentMix(MidiConst.CHANNEL_DRUMS);
        if (channelDrumsInsMix == null || !channelDrumsInsMix.isInstrumentEnabled())
        {
            return res;
        }


        for (RhythmVoice rv : getRhythmVoices())
        {
            int channel = getChannel(rv);
            InstrumentMix insMix = getInstrumentMix(rv);
            Instrument newIns = mapChannelNewIns == null ? null : mapChannelNewIns.get(channel);
            Instrument ins = (newIns != null) ? newIns : insMix.getInstrument();
            LOGGER.log(Level.FINE, "getChannelsNeedingDrumsRerouting() rv={0} channel={1} ins={2}", new Object[]
            {
                rv, channel, ins
            });


            if (channel != MidiConst.CHANNEL_DRUMS
                    && rv.isDrums()
                    && !getDrumsReroutedChannels().contains(channel)
                    && ins == GMSynth.getInstance().getVoidInstrument())
            {
                res.add(channel);
            }

        }
        LOGGER.log(Level.FINE, "getChannelsNeedingDrumsRerouting() res={0}", res);
        return res;
    }

    /**
     * Return a free channel to be used in this MidiMix.
     * <p>
     * Try to keep channels in one section above the drums channel reserved to Drums. If not enough channels extend to channel below the drums channel.
     *
     * @param findDrumsChannel If true try to use CHANNEL_DRUMS if it is available.
     * @return -1 if no channel found
     */
    public int findFreeChannel(boolean findDrumsChannel)
    {
        List<Integer> usedChannels = getUsedChannels();
        if (findDrumsChannel && !usedChannels.contains(MidiConst.CHANNEL_DRUMS))
        {
            return MidiConst.CHANNEL_DRUMS;
        }

        // First search channels above Drums channel
        for (int channel = MidiConst.CHANNEL_DRUMS + 1; channel <= MidiConst.CHANNEL_MAX; channel++)
        {
            if (!usedChannels.contains(channel))
            {
                return channel;
            }
        }
        for (int channel = MidiConst.CHANNEL_DRUMS - 1; channel >= MidiConst.CHANNEL_MIN; channel--)
        {
            if (!usedChannels.contains(channel))
            {
                return channel;
            }
        }
        return -1;
    }

    /**
     * Add RhythmVoices (of Rhythm instances only, UserRhythmVoices are skipped) and InstrumentMixes copies from mm into this MidiMix.
     * <p>
     * Copies have solo/drumsRerouting set to OFF. Method uses findFreeChannel() to allocate the new channels of mm if they are not free in this MidiMix.
     * <p>
     * The operation will fire UndoableEvent edits.
     *
     * @param fromMm
     * @param r      If non null, copy fromMm instrumentMixes only if they belong to rhythm r (if r is an AdaptedRhythm, use its source rhythm).
     * @throws MidiUnavailableException If not enough channels available to accommodate mm instruments.
     */
    public final void addInstrumentMixes(MidiMix fromMm, Rhythm r) throws MidiUnavailableException
    {
        LOGGER.log(Level.FINE, "copyInstrumentMixes() -- rvKeys={0} fromMm.rvKeys={1}", new Object[]
        {
            getRhythmVoices(), fromMm.getRhythmVoices()
        });


        List<Integer> fromUsedChannels = (r == null) ? fromMm.getUsedChannels() : fromMm.getUsedChannels(r);
        if (getUnusedChannels().size() < fromUsedChannels.size())
        {
            throw new MidiUnavailableException(ResUtil.getString(getClass(), "ERR_NotEnoughChannels"));
        }


        for (Integer fromChannel : fromUsedChannels)
        {
            RhythmVoice fromRvKey = fromMm.getRhythmVoice(fromChannel);
            if (!(fromRvKey instanceof UserRhythmVoice))
            {
                int newChannel = getUsedChannels().contains(fromChannel) ? findFreeChannel(fromRvKey.isDrums()) : fromChannel;
                assert newChannel != -1 : " getUsedChannels()=" + getUsedChannels();
                InstrumentMix fromInsMix = fromMm.getInstrumentMix(fromChannel);
                setInstrumentMix(newChannel, fromRvKey, new InstrumentMix(fromInsMix));
            }
        }


        LOGGER.log(Level.FINE, "addInstrumentMixes()     exit : rvKeys={0}", getRhythmVoices());
    }

    /**
     * The file where this object is stored.
     *
     * @return Null if not set.
     */
    public File getFile()
    {
        return file;
    }

    public void setFile(File f)
    {
        file = f;
    }

    /**
     * Import InstrumentMixes from mm into this object.
     * <p>
     * Import is first done on matching RhythmVoices from the same rhythm. Then import only when RvTypes match. For UserRhythmVoices import is done only if name
     * matches.<br>
     * Create new copy instances of Instruments Mixes with solo OFF.
     * <p>
     * The operation will fire UndoableEvent(s).
     *
     * @param mm
     */
    public void importInstrumentMixes(MidiMix mm)
    {
        if (mm == null)
        {
            throw new NullPointerException("mm");
        }
        if (mm.rhythmVoices.length != rhythmVoices.length)
        {
            throw new IllegalStateException("mm.rvKeys.length=" + mm.rhythmVoices.length + " rvKeys.length=" + rhythmVoices.length);
        }


        // Find the matching voices except the user phrase channels
        HashMap<RhythmVoice, RhythmVoice> mapMatchingVoices = new HashMap<>();
        List<RhythmVoice> rvs = getRhythmVoices();
        List<RhythmVoice> mmRvs = mm.getRhythmVoices();
        for (RhythmVoice mmRv : mmRvs)
        {
            boolean matched = false;

            // Try first on matching RhythmVoices from same rhythm
            for (RhythmVoice rv : rvs.toArray(RhythmVoice[]::new))
            {
                if (!(rv instanceof UserRhythmVoice) && mmRv == rv)
                {
                    mapMatchingVoices.put(mmRv, rv);
                    rvs.remove(rv);
                    matched = true;
                    break;
                }
            }
            if (!matched)
            {
                // If no match, try any channel with the same RvType
                for (RhythmVoice rv : rvs.toArray(RhythmVoice[]::new))
                {
                    if (!(rv instanceof UserRhythmVoice) && !(mmRv instanceof UserRhythmVoice) && mmRv.getType().equals(rv.getType()))
                    {
                        mapMatchingVoices.put(mmRv, rv);
                        rvs.remove(rv);
                        matched = true;
                        break;
                    }
                }
            }
        }


        // Copy the InstrumentMixes
        for (RhythmVoice mmRv : mapMatchingVoices.keySet())
        {
            InstrumentMix mmInsMix = mm.getInstrumentMix(mmRv);
            RhythmVoice rv = mapMatchingVoices.get(mmRv);
            int channel = getChannel(rv);
            setInstrumentMix(channel, rv, new InstrumentMix(mmInsMix));
        }


        // For user phrase channels, import instrument mixes only when name match
        for (var mmUserRv : mm.getUserRhythmVoices())
        {
            var urv = getUserRhythmVoice(mmUserRv.getName());
            if (urv != null)
            {
                InstrumentMix mmInsMix = mm.getInstrumentMix(mmUserRv);
                changeInstrumentMix(getChannel(urv), mmInsMix, urv);
            }
        }
    }

    /**
     * @return True if MidiMix has some unsaved changes.
     */
    public boolean needSave()
    {
        return needSave;
    }

    /**
     * Same as saveToFile() but notify user if problems.
     *
     * @param f
     * @param isCopy
     * @return False if a problem occured
     */
    public boolean saveToFileNotify(File f, boolean isCopy)
    {
        if (f == null)
        {
            throw new IllegalArgumentException("f=" + f);
        }

        boolean b = true;
        if (f.exists() && !f.canWrite())
        {
            String msg = ResUtil.getString(getClass(), "ERR_CantOverwrite", f.getAbsolutePath());
            LOGGER.log(Level.WARNING, "saveToFileNotify() {0}", msg);
            NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.WARNING_MESSAGE);
            DialogDisplayer.getDefault().notify(nd);
            b = false;
        }
        if (b)
        {
            try
            {
                saveToFile(f, isCopy);
            } catch (IOException ex)
            {
                String msg = ResUtil.getString(getClass(), "ERR_ProblemSavingMixFile", f.getAbsolutePath()) + " : " + ex.getLocalizedMessage();
                if (ex.getCause() != null)
                {
                    msg += "\n" + ex.getCause().getLocalizedMessage();
                }
                LOGGER.log(Level.WARNING, "saveToFileNotify() {0}", msg);
                NotifyDescriptor nd = new NotifyDescriptor.Message(msg, NotifyDescriptor.WARNING_MESSAGE);
                DialogDisplayer.getDefault().notify(nd);
                b = false;
            }
        }
        return b;
    }

    /**
     * Save this MidiMix to a file.
     * <p>
     * This will fire a PROP_MODIFIED_OR_SAVED change event (true=&gt;false).
     *
     * @param f
     * @param isCopy Indicate that we save a copy, ie perform the file save but nothing else (eg no PROP_MODIFIED_OR_SAVED state change)
     * @throws java.io.IOException
     */
    public void saveToFile(File f, boolean isCopy) throws IOException
    {
        if (f == null)
        {
            throw new IllegalArgumentException("f=" + f + " isCopy=" + isCopy);
        }
        LOGGER.log(Level.FINE, "saveToFile() f={0} isCopy={1}", new Object[]
        {
            f.getAbsolutePath(), isCopy
        });

        if (!isCopy)
        {
            file = f;
        }

        try (FileOutputStream fos = new FileOutputStream(f))
        {
            XStream xstream = XStreamInstancesManager.getInstance().getSaveMidiMixInstance();
            Writer w = new BufferedWriter(new OutputStreamWriter(fos, "UTF-8"));        // Needed to support special/accented chars
            xstream.toXML(this, w);
            if (!isCopy)
            {
                pcs.firePropertyChange(PROP_MODIFIED_OR_SAVED, true, false);
            }
        } catch (IOException e)
        {
            if (!isCopy)
            {
                file = null;
            }
            throw new IOException(e);
        } catch (XStreamException e)
        {
            if (!isCopy)
            {
                file = null;
            }
            LOGGER.log(Level.WARNING, "saveToFile() exception={0}", e.getMessage());
            // Translate into an IOException to be handled by the Netbeans framework 
            throw new IOException("XStream XML unmarshalling error", e);
        }
    }

    /**
     * All InstrumentMixes ordered by channel.
     *
     * @return A 16 items list, one instrumentMix per channel (some items can be null)
     */
    public synchronized List<InstrumentMix> getInstrumentMixesPerChannel()
    {
        return Arrays.asList(instrumentMixes);
    }

    /**
     * The non-null instrument mixes ordered by channel.
     *
     * @return Can be an empty list.
     */
    public synchronized List<InstrumentMix> getInstrumentMixes()
    {
        ArrayList<InstrumentMix> insMixes = new ArrayList<>();
        for (InstrumentMix im : instrumentMixes)
        {
            if (im != null)
            {
                insMixes.add(im);
            }
        }
        return insMixes;
    }


    /**
     * Add a user phrase channel for the specified phrase name.
     *
     * @param userPhraseName
     * @throws MidiUnavailableException
     */
    public void addUserChannel(String userPhraseName) throws MidiUnavailableException
    {
        int channel = getUsedChannels().contains(UserRhythmVoice.DEFAULT_USER_PHRASE_CHANNEL) ? findFreeChannel(false)
                : UserRhythmVoice.DEFAULT_USER_PHRASE_CHANNEL;
        if (channel == -1)
        {
            String msg = ResUtil.getString(getClass(), "ERR_NotEnoughChannels");
            throw new MidiUnavailableException(msg);
        }

        Phrase p = song.getUserPhrase(userPhraseName);
        assert p != null : "userPhraseName=" + userPhraseName;


        // Update the MidiMix
        RhythmVoiceInstrumentProvider insProvider = RhythmVoiceInstrumentProvider.getProvider();
        UserRhythmVoice urv;
        Instrument ins;

        if (!p.isDrums())
        {
            // Directly use a RhythmVoiceInstrumentProvider to get the melodic instrument
            urv = new UserRhythmVoice(userPhraseName);
            ins = insProvider.findInstrument(urv);
        } else
        {
            // Try to reuse the same drums instrument than in the current song
            var rvDrums = getRhythmVoice(MidiConst.CHANNEL_DRUMS);
            if (rvDrums == null)
            {
                // Unusual, but there might be another drums channel
                rvDrums = getRhythmVoices().stream()
                        .filter(rv -> rv.isDrums())
                        .findAny()
                        .orElse(null);
            }
            if (rvDrums != null)
            {
                ins = getInstrumentMix(rvDrums).getInstrument();
                DrumKit kit = ins.getDrumKit();     // Might be null if ins is the VoidInstrument from the GM bank
                urv = new UserRhythmVoice(userPhraseName, kit != null ? kit : new DrumKit());
            } else
            {
                urv = new UserRhythmVoice(userPhraseName, new DrumKit());
                ins = insProvider.findInstrument(urv);
            }
        }

        var insMix = new InstrumentMix(ins, new InstrumentSettings());
        setInstrumentMix(channel, urv, insMix);

        if (p.isDrums() && ins == GMSynth.getInstance().getVoidInstrument() && channel != MidiConst.CHANNEL_DRUMS)
        {
            // Special case, better to activate drums rerouting
            setDrumsReroutedChannel(true, channel);
        }
    }


    /**
     * Send the midi messages to initialize all the instrument mixes.
     * <p>
     * Midi messages are sent to the default JJazzLab Midi OUT device.
     */
    public void sendAllMidiMixMessages()
    {
        LOGGER.fine("sendAllMidiMixMessages()");
        for (Integer channel : getUsedChannels())
        {
            InstrumentMix insMix = getInstrumentMix(channel);
            JJazzMidiSystem jms = JJazzMidiSystem.getInstance();
            jms.sendMidiMessagesOnJJazzMidiOut(insMix.getAllMidiMessages(channel));
        }
    }

    /**
     * Send the midi messages to set the volume of all instrument mixes.
     * <p>
     * Midi messages are sent to the default JJazzLab Midi OUT device.
     */
    public void sendAllMidiVolumeMessages()
    {
        LOGGER.fine("sendAllMidiVolumeMessages()");
        for (Integer channel : getUsedChannels())
        {
            InstrumentMix insMix = getInstrumentMix(channel);
            InstrumentSettings insSet = insMix.getSettings();
            JJazzMidiSystem.getInstance().sendMidiMessagesOnJJazzMidiOut(insSet.getVolumeMidiMessages(channel));
        }
    }

    public void addUndoableEditListener(UndoableEditListener l)
    {
        if (l == null)
        {
            throw new NullPointerException("l=" + l);
        }
        undoListeners.remove(l);
        undoListeners.add(l);
    }

    public void removeUndoableEditListener(UndoableEditListener l)
    {
        if (l == null)
        {
            throw new NullPointerException("l=" + l);
        }
        undoListeners.remove(l);
    }

    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(propertyName, l);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(propertyName, l);
    }

    @Override
    public String toString()
    {
        return "MidiMix[song=" + song + ", channels=" + Arrays.toString(getUsedChannels().toArray(new Integer[0])) + "]";
    }

    public String toDumpString()
    {
        StringBuilder sb = new StringBuilder();
        var reroutedChannels = getDrumsReroutedChannels();
        sb.append(toString()).append(":\n");
        for (int i = 0; i < 15; i++)
        {
            InstrumentMix insMix = instrumentMixes[i];
            if (insMix != null)
            {
                sb.append(" ").append(i).append(": ").append(insMix);
                if (reroutedChannels.contains(i))
                {
                    sb.append(" REROUTED");
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * Get the song mix File object for a specified song file.
     * <p>
     * SongMix file will be located in the same directory than songFile.
     *
     * @param songFile
     * @return Return a new file identical to songFile except the extension. If songFile is null returns null.
     */
    static public File getSongMixFile(File songFile)
    {
        if (songFile == null)
        {
            return null;
        }
        var res = Utilities.replaceExtension(songFile, MIX_FILE_EXTENSION);
        return res;
    }

    /**
     * Get the expected location of the rhythm mix file.
     * <p>
     * If rhythmFile is defined then .mix file is located in the same directory. Otherwise .mix file is located in the default rhythms directory<br>
     *
     * @param rhythmName
     * @param rhythmFile Can not be null but can be the empty path ("")
     * @return
     * @see Rhythm#getFile()
     * @see RhythmDirsLocator#getDefaultRhythmsDirectory()
     */
    static public File getRhythmMixFile(String rhythmName, File rhythmFile)
    {
        Preconditions.checkNotNull(rhythmName);
        Preconditions.checkNotNull(rhythmFile);
        String mixFileName;
        File dir = rhythmFile.getParentFile();
        if (dir != null)
        {
            // File-based rhythm
            mixFileName = Utilities.replaceExtension(rhythmFile.getName(), MIX_FILE_EXTENSION);
        } else
        {
            dir = RhythmDirsLocator.getDefault().getDefaultRhythmsDirectory();
            var rhythmNameNoSpace = rhythmName.replace(" ", "");
            mixFileName = rhythmNameNoSpace + "." + MIX_FILE_EXTENSION;
        }
        File f = new File(dir, mixFileName);
        return f;
    }

    /**
     *
     * @param f
     * @return Null if MidiMix could not be created for some reason.
     * @throws java.io.IOException If problem occured while reading file
     */
    public static MidiMix loadFromFile(File f) throws IOException
    {
        if (f == null)
        {
            throw new IllegalArgumentException("f=" + f);
        }
        MidiMix mm = null;

        try (var fis = new FileInputStream(f))
        {
            XStream xstream = XStreamInstancesManager.getInstance().getLoadMidiMixInstance();
            Reader r = new BufferedReader(new InputStreamReader(fis, "UTF-8"));        // Needed to support special/accented chars
            mm = (MidiMix) xstream.fromXML(r);
            mm.setFile(f);
        } catch (XStreamException e)
        {
            LOGGER.log(Level.WARNING, "loadFromFile() XStreamException e={0}", e.getMessage());   // Important in order to get the details of the XStream error   
            throw new IOException("XStream loading error", e);         // Translate into an IOException to be handled by the Netbeans framework 
        }
        return mm;
    }

    //-----------------------------------------------------------------------
    // Implementation of the SgsChangeListener interface
    //-----------------------------------------------------------------------

    @Override
    public void songStructureChanged(SgsChangeEvent e) throws UnsupportedEditException
    {
        LOGGER.log(Level.FINE, "songStructureChanged() -- e={0}", e);


        JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(song);
        if (um != null && um.isUndoRedoInProgress())
        {
            // IMPORTANT : MidiMix generates his own undoableEdits
            // so we must not listen to SongStructure changes if undo/redo in progress, otherwise 
            // the "undo/redo" restore operations will be performed twice !
            return;
        }

        if (e instanceof SgsVetoableChangeEvent svce)
        {
            testChangeEventForVeto(svce.getChangeEvent());
            return;
        }


        // Used to check if rhythm is really gone in case of multi-rhythm songs
        List<Rhythm> songRhythms = song.getSongStructure().getUniqueRhythms(true, false);
        List<Rhythm> mixRhythms = getUniqueRhythms();

        if (e instanceof SptAddedEvent sae)
        {
            for (SongPart spt : sae.getSongParts())
            {

                Rhythm r = getSourceRhythm(spt.getRhythm());
                if (!mixRhythms.contains(r))
                {
                    try
                    {
                        // It's a new rhythm in the MidiMix
                        addRhythm(r);
                    } catch (MidiUnavailableException ex)
                    {
                        // Should not be here since we made a test just above to avoid this
                        throw new IllegalStateException(
                                "Unexpected MidiUnavailableException ex=" + ex.getMessage() + " this=" + this + " r=" + r);
                    }
                    mixRhythms.add(r);
                }
            }

        } else if (e instanceof SptRemovedEvent)
        {
            SptRemovedEvent e2 = (SptRemovedEvent) e;
            for (SongPart spt : e2.getSongParts())
            {
                Rhythm r = getSourceRhythm(spt.getRhythm());
                if (!songRhythms.contains(r) && mixRhythms.contains(r))
                {
                    // There is no more such rhythm in the song, we can remove it from the midimix
                    removeRhythm(r);
                    mixRhythms.remove(r);
                }
            }
        } else if (e instanceof SptReplacedEvent)
        {

            SptReplacedEvent e2 = (SptReplacedEvent) e;
            List<SongPart> oldSpts = e2.getSongParts();
            List<SongPart> newSpts = e2.getNewSpts();

            // Important : remove rhythm parts before adding (otherwise we could have a "not enough midi channels
            // available" in the loop).
            oldSpts.stream()
                    .map(spt -> getSourceRhythm(spt.getRhythm()))
                    .filter(r -> !songRhythms.contains(r))
                    .forEach(r -> 
                    {
                        // Rhythm is no more present in the song, remove it also from the MidiMix
                        removeRhythm(r);
                        mixRhythms.remove(r);
                    });

            // Add the new rhythms
            newSpts.stream()
                    .map(spt -> getSourceRhythm(spt.getRhythm()))
                    .filter(r -> !mixRhythms.contains(r))
                    .forEach(r -> 
                    {
                        // New song rhythm is not yet in the midimix, add it
                        try
                        {
                            addRhythm(r);
                        } catch (MidiUnavailableException ex)
                        {
                            // Should not be here since we made a test earlier to avoid this
                            throw new IllegalStateException(
                                    "Unexpected MidiUnavailableException ex=" + ex.getMessage() + " this=" + this + " r=" + r);
                        }
                        mixRhythms.add(r);
                    });
        }
    }

    //-----------------------------------------------------------------------
    // Implementation of the VetoableChangeListener interface
    //-----------------------------------------------------------------------
    @Override
    public void vetoableChange(PropertyChangeEvent e) throws PropertyVetoException
    {

        JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(song);
        if (um != null && um.isUndoRedoInProgress())
        {
            // IMPORTANT : Song generates its own undoableEdits,
            // so we must not listen to song changes if undo/redo in progress, otherwise 
            // the "undo/redo" restore operations will be performed twice !
            LOGGER.log(Level.FINE, "vetoableChange() undo is in progress, exiting");
            return;
        }


        if (e.getSource() == song)
        {
            switch (e.getPropertyName())
            {
                case Song.PROP_VETOABLE_USER_PHRASE ->
                {
                    if (e.getNewValue() instanceof String name)
                    {
                        // New user phrase added
                        try
                        {
                            addUserChannel(name);
                        } catch (MidiUnavailableException ex)
                        {
                            throw new PropertyVetoException(ex.getMessage(), e);
                        }

                    } else
                    {
                        // User phrase was removed
                        var name = (String) e.getOldValue();
                        removeUserChannel(name);
                    }
                }
                case Song.PROP_VETOABLE_USER_PHRASE_CONTENT ->
                {
                    // User phrase was updated, nothing to do at the MidiMix level
                }
                case Song.PROP_VETOABLE_PHRASE_NAME ->
                {
                    // User phrase was renamed, replace the UserRhythmVoice
                    String oldName = (String) e.getOldValue();
                    String newName = (String) e.getNewValue();
                    UserRhythmVoice oldUrv = getUserRhythmVoice(oldName);
                    assert oldUrv != null : "oldName=" + oldName;
                    var kit = oldUrv.getDrumKit();
                    UserRhythmVoice newUrv = kit != null ? new UserRhythmVoice(newName, oldUrv.getDrumKit())
                            : new UserRhythmVoice(newName);
                    replaceRhythmVoice(oldUrv, newUrv);

                }
                default ->
                {
                    // Nothing
                }
            }
        }
    }

    //-----------------------------------------------------------------------
    // Implementation of the PropertiesListener interface
    //-----------------------------------------------------------------------
    @SuppressWarnings(
            {
                "unchecked", "rawtypes"
            })
    @Override
    public void propertyChange(PropertyChangeEvent e)
    {
        LOGGER.log(Level.FINE, "propertyChange() e={0}", e);


        if (e.getSource() instanceof InstrumentMix insMix)
        {
            int channel = getChannel(insMix);

            switch (e.getPropertyName())
            {
                case InstrumentMix.PROP_SOLO ->
                {
                    boolean b = (boolean) e.getNewValue();
                    LOGGER.log(Level.FINE, "propertyChange() channel={0} solo={1}", new Object[]
                    {
                        channel, b
                    });

                    if (b)
                    {
                        // Solo switched to ON
                        if (soloedInsMixes.isEmpty())
                        {
                            // Fist solo !
                            soloedInsMixes.add(insMix);
                            // Save config
                            saveMuteConfig();
                            // Switch other channels to ON (ezxcept soloed one)
                            for (InstrumentMix im : instrumentMixes)
                            {
                                if (im != null && im != insMix)
                                {
                                    im.setMute(true);
                                }
                            }
                        } else
                        {
                            // It's another solo
                            soloedInsMixes.add(insMix);
                        }
                    } else // Solo switched to OFF
                    {
                        soloedInsMixes.remove(insMix);
                        if (soloedInsMixes.isEmpty())
                        {
                            // This was the last SOLO OFF, need to restore Mute config
                            restoreMuteConfig();
                        } else
                        {
                            // There are still other Solo ON channels, put it in mute again
                            insMix.setMute(true);
                        }
                    }
                }

                case InstrumentMix.PROP_MUTE ->
                {
                    boolean b = (boolean) e.getNewValue();
                    // If in solo mode, pressing unmute of a muted channel turns it in solo mode
                    if (b == false && !soloedInsMixes.isEmpty())
                    {
                        insMix.setSolo(true);
                    }
                    // Forward the MUTE change event
                    pcs.firePropertyChange(MidiMix.PROP_INSTRUMENT_MUTE, insMix, b);
                }

                case InstrumentMix.PROP_INSTRUMENT ->
                {
                    // If drums instrument change with different KeyMap
                    Instrument oldIns = (Instrument) e.getOldValue();
                    Instrument newIns = (Instrument) e.getNewValue();
                    RhythmVoice rv = getRhythmVoice(channel);
                    if (rv.isDrums())
                    {
                        DrumKit oldKit = oldIns.getDrumKit();
                        DrumKit newKit = newIns.getDrumKit();
                        if ((oldKit != null && newKit != null && oldKit.getKeyMap() != newKit.getKeyMap())
                                || (oldKit == null && newKit != null)
                                || (oldKit != null && newKit == null))
                        {
                            pcs.firePropertyChange(MidiMix.PROP_DRUMS_INSTRUMENT_KEYMAP, channel, oldKit != null
                                    ? oldKit.getKeyMap()
                                    : null);
                            fireIsMusicGenerationModified(MidiMix.PROP_DRUMS_INSTRUMENT_KEYMAP, null);
                        }
                    }
                }
                default ->
                {
                }
            }
            fireIsModified();

        } else if (e.getSource() instanceof InstrumentSettings)
        {
            // Forward some change events
            InstrumentSettings insSet = (InstrumentSettings) e.getSource();
            InstrumentMix insMix = insSet.getContainer();
            if (e.getPropertyName().equals(InstrumentSettings.PROPERTY_TRANSPOSITION))
            {
                int value = (Integer) e.getNewValue();
                pcs.firePropertyChange(MidiMix.PROP_INSTRUMENT_TRANSPOSITION, insMix, value);
                fireIsMusicGenerationModified(MidiMix.PROP_INSTRUMENT_TRANSPOSITION, insMix);
            } else if (e.getPropertyName().equals(InstrumentSettings.PROPERTY_VELOCITY_SHIFT))
            {
                int value = (Integer) e.getNewValue();
                pcs.firePropertyChange(MidiMix.PROP_INSTRUMENT_VELOCITY_SHIFT, insMix, value);
                fireIsMusicGenerationModified(MidiMix.PROP_INSTRUMENT_VELOCITY_SHIFT, insMix);
            }
            fireIsModified();
        }
    }
    //-----------------------------------------------------------------------
    // Private methods
    //-----------------------------------------------------------------------

    /**
     * Perform the InstrumentMix change operation.
     * <p>
     * There is no validity check done on parameters, this must be done by the caller method.
     * <p>
     * This method fires an UndoableEvent(s).
     *
     * @param channel
     * @param insMix
     * @param rvKey
     */
    private void changeInstrumentMix(final int channel, final InstrumentMix insMix, final RhythmVoice rvKey)
    {
        LOGGER.log(Level.FINER, "changeInstrumentMix() -- channel={0} rvKey={1} insMix={2}", new Object[]
        {
            channel, rvKey, insMix
        });

        final InstrumentMix oldInsMix = instrumentMixes[channel];
        final RhythmVoice oldRvKey = rhythmVoices[channel];
        if (oldInsMix == null && insMix == null)
        {
            return;
        } else if (oldInsMix != null && oldInsMix.equals(insMix))
        {
            return;
        } else if (oldInsMix != null)
        {
            oldInsMix.setSolo(false);  // So it does not mess if we were in solo mode
            oldInsMix.removePropertyChangeListener(this);
            oldInsMix.getSettings().removePropertyChangeListener(this);
            setDrumsReroutedChannel(false, channel);
        }
        if (insMix != null)
        {
            // insMix.setMute(false);       // Don't change
            insMix.setSolo(false);
            insMix.addPropertyChangeListener(this);
            insMix.getSettings().addPropertyChangeListener(this);
        }

        // Update our internal state
        synchronized (this)
        {
            instrumentMixes[channel] = insMix;
            rhythmVoices[channel] = rvKey;
        }

        // Prepare the undoable edit
        UndoableEdit edit = new SimpleEdit("Change instrumemt mix")
        {
            @Override
            public void undoBody()
            {
                synchronized (MidiMix.this)
                {
                    instrumentMixes[channel] = oldInsMix;
                    rhythmVoices[channel] = oldRvKey;
                }
                if (insMix != null)
                {
                    insMix.removePropertyChangeListener(MidiMix.this);
                    insMix.getSettings().removePropertyChangeListener(MidiMix.this);
                }
                if (oldInsMix != null)
                {
                    oldInsMix.addPropertyChangeListener(MidiMix.this);
                    oldInsMix.getSettings().addPropertyChangeListener(MidiMix.this);
                }
                if (oldInsMix != null)
                {
                    // Safety call because drumsrerouted state is not saved in the undo command 
                    // Not doing this caused some problems in some cases with undo/redo when changing rhythm in a multi-ryhtm song
                    setDrumsReroutedChannel(false, channel);
                }

                LOGGER.log(Level.FINER, "changeInstrumentMix().undoBody() oldInsMix={0} insMix={1}", new Object[]
                {
                    oldInsMix, insMix
                });

                pcs.firePropertyChange(PROP_CHANNEL_INSTRUMENT_MIX, insMix, channel);
                fireIsModified();
                fireIsMusicGenerationModified(PROP_CHANNEL_INSTRUMENT_MIX, channel);
            }

            @Override
            public void redoBody()
            {
                synchronized (MidiMix.this)
                {
                    instrumentMixes[channel] = insMix;
                    rhythmVoices[channel] = rvKey;
                }
                if (oldInsMix != null)
                {
                    oldInsMix.removePropertyChangeListener(MidiMix.this);
                    oldInsMix.getSettings().removePropertyChangeListener(MidiMix.this);
                }
                if (insMix != null)
                {
                    insMix.addPropertyChangeListener(MidiMix.this);
                    insMix.getSettings().addPropertyChangeListener(MidiMix.this);
                }
                if (insMix != null)
                {
                    // Safety call because drumsrerouted state is not saved in the undo command 
                    // Not doing this caused some problems in some cases with undo/redo when changing rhythm in a multi-ryhtm song
                    setDrumsReroutedChannel(false, channel);
                }

                LOGGER.log(Level.FINER, "changeInstrumentMix().redoBody() oldInsMix={0} insMix={1}", new Object[]
                {
                    oldInsMix, insMix
                });

                pcs.firePropertyChange(PROP_CHANNEL_INSTRUMENT_MIX, oldInsMix, channel);
                fireIsModified();
                fireIsMusicGenerationModified(PROP_CHANNEL_INSTRUMENT_MIX, oldRvKey);
            }
        };
        fireUndoableEditHappened(edit);

        pcs.firePropertyChange(PROP_CHANNEL_INSTRUMENT_MIX, oldInsMix, channel);
        fireIsModified();
        fireIsMusicGenerationModified(PROP_CHANNEL_INSTRUMENT_MIX, oldRvKey);

    }

    private void fireIsModified()
    {
        needSave = true;
        pcs.firePropertyChange(PROP_MODIFIED_OR_SAVED, false, true);
    }

    private void fireIsMusicGenerationModified(String id, Object data)
    {
        pcs.firePropertyChange(PROP_MUSIC_GENERATION, id, data);
    }

    private void saveMuteConfig()
    {
        int i = 0;
        for (InstrumentMix im : instrumentMixes)
        {
            saveMuteConfiguration[i++] = (im == null) ? false : im.isMute();
        }
    }

    private void restoreMuteConfig()
    {
        int i = 0;
        for (InstrumentMix im : instrumentMixes)
        {
            if (im != null)
            {
                im.setMute(saveMuteConfiguration[i]);
            }
            i++;
        }
    }

    /**
     * Add a rhythm to this MidiMix.
     * <p>
     * Manage the case where r is not the unique rhythm of the MidiMix: need to maintain instruments consistency to avoid poor-sounding rhythms transitions.
     *
     * @param r
     * @throws MidiUnavailableException
     */
    private void addRhythm(Rhythm r) throws MidiUnavailableException
    {
        LOGGER.log(Level.FINE, "addRhythm() -- r={0} current rvKeys={1}", new Object[]
        {
            r.getName(), Arrays.asList(rhythmVoices).toString()
        });

        assert !(r instanceof AdaptedRhythm) : "r=" + r;

        MidiMix mm = MidiMixManager.getDefault().findMix(r);
        if (!getUniqueRhythms().isEmpty())
        {
            // Adapt mm to sound like the InstrumentMixes of r0           
            Rhythm r0 = getUniqueRhythms().get(0);
            adaptInstrumentMixes(mm, r0);
        }
        addInstrumentMixes(mm, r);
    }

    /**
     * Adapt the InstrumentMixes of mm to "sound" like the InstrumentMixes of r0 in this MidiMix.
     *
     * @param mm
     * @param r0
     */
    private void adaptInstrumentMixes(MidiMix mm, Rhythm r0)
    {
        LOGGER.log(Level.FINE, "adaptInstrumentMixes() mm={0} r0={1}", new Object[]
        {
            mm, r0
        });
        HashMap<String, InstrumentMix> mapKeyMix = new HashMap<>();
        HashMap<InstrumentFamily, InstrumentMix> mapFamilyMix = new HashMap<>();
        InstrumentMix r0InsMixDrums = null;
        InstrumentMix r0InsMixPerc = null;
        // First try to match InstrumentMixes using a "key" = "3 first char of Rv.getName() + GM1 family"
        for (int channel : getUsedChannels(r0))
        {
            // Build the keys from r0
            RhythmVoice rv = rhythmVoices[channel];
            InstrumentMix insMix = instrumentMixes[channel];
            if (rv.isDrums())
            {
                // Special case, use the 2 special variables for Drums or Percussion
                // Use the saved InstrumentMix if channel is drums rerouted
                InstrumentMix insMixDrums = getDrumsReroutedChannels().contains(channel) ? drumsReroutedChannels.get(channel)
                        : insMix;
                if (rv.getType().equals(RhythmVoice.Type.DRUMS))
                {
                    r0InsMixDrums = insMixDrums;
                } else
                {
                    r0InsMixPerc = insMixDrums;
                }
                continue;
            }
            GM1Instrument insGM1 = insMix.getInstrument().getSubstitute();  // Might be null            
            InstrumentFamily family = insGM1 != null ? insGM1.getFamily() : null;
            String mapKey = Utilities.truncate(rv.getName().toLowerCase(), 3) + "-" + ((family != null) ? family.name() : "");
            if (mapKeyMix.get(mapKey) == null)
            {
                mapKeyMix.put(mapKey, insMix);  // If several instruments have the same Type, save only the first one
            }
            if (family != null && mapFamilyMix.get(family) == null)
            {
                mapFamilyMix.put(family, insMix);       // If several instruments have the same family, save only the first one
            }
        }

        // Try to convert using the keys
        HashSet<Integer> doneChannels = new HashSet<>();
        for (int mmChannel : mm.getUsedChannels())
        {
            RhythmVoice mmRv = mm.rhythmVoices[mmChannel];
            InstrumentMix mmInsMix = mm.instrumentMixes[mmChannel];
            InstrumentMix insMix;

            switch (mmRv.getType())
            {
                case DRUMS ->
                    insMix = r0InsMixDrums;
                case PERCUSSION ->
                    insMix = r0InsMixPerc;
                default ->
                {
                    GM1Instrument mmInsGM1 = mmInsMix.getInstrument().getSubstitute();  // Can be null            
                    InstrumentFamily mmFamily = mmInsGM1 != null ? mmInsGM1.getFamily() : null;
                    String mapKey = Utilities.truncate(mmRv.getName().toLowerCase(), 3) + "-" + ((mmFamily != null)
                            ? mmFamily.name() : "");
                    insMix = mapKeyMix.get(mapKey);
                }

            }

            if (insMix != null)
            {
                // Copy InstrumentMix data
                mmInsMix.setInstrument(insMix.getInstrument());
                mmInsMix.getSettings().set(insMix.getSettings());
                doneChannels.add(mmChannel);
                LOGGER.log(Level.FINER, "adaptInstrumentMixes() set (1) channel {0} instrument setting to : {1}", new Object[]
                {
                    mmChannel,
                    insMix.getSettings()
                });
            }

        }

        // Try to convert also the other channels by matching only the instrument family
        for (int mmChannel : mm.getUsedChannels())
        {
            if (doneChannels.contains(mmChannel))
            {
                continue;
            }
            InstrumentMix mmInsMix = mm.instrumentMixes[mmChannel];
            GM1Instrument mmInsGM1 = mmInsMix.getInstrument().getSubstitute();  // Can be null          
            if (mmInsGM1 == null || mmInsGM1 == GMSynth.getInstance().getVoidInstrument())
            {
                continue;
            }
            InstrumentFamily mmFamily = mmInsGM1.getFamily();
            InstrumentMix insMix = mapFamilyMix.get(mmFamily);
            if (insMix != null)
            {
                // Copy InstrumentMix data
                mmInsMix.setInstrument(insMix.getInstrument());
                mmInsMix.getSettings().set(insMix.getSettings());
                LOGGER.log(Level.FINER, "adaptInstrumentMixes() set (2) channel {0} instrument setting to : {1}", new Object[]
                {
                    mmChannel,
                    insMix.getSettings()
                });
            }
        }
    }


    /**
     * Check that we don't exceed number of available Midi channels.
     *
     * @param e
     * @throws UnsupportedEditException
     */
    private void testChangeEventForVeto(SgsChangeEvent e) throws UnsupportedEditException
    {
        LOGGER.log(Level.FINE, "testChangeEventForVeto() -- e={0}", e);

        // Build the list of SongPart after the change
        List<SongPart> spts = null;
        if (e instanceof SptAddedEvent sae)
        {
            spts = song.getSongStructure().getSongParts();
            spts.addAll(sae.getSongParts());

        } else if (e instanceof SptReplacedEvent sre)
        {
            List<SongPart> oldSpts = sre.getSongParts();
            List<SongPart> newSpts = sre.getNewSpts();

            spts = song.getSongStructure().getSongParts();
            spts.removeAll(oldSpts);
            spts.addAll(newSpts);
        }

        if (spts == null)
        {
            // Nb of rhythmVoices can't have increased
            return;
        }

        // Number of RhythmVoices has possibly changed
        HashSet<Rhythm> rhythms = new HashSet<>();
        int nbVoices = getUserChannels().size();      // Initialize with user rhythm voices
        for (SongPart spt : spts)
        {
            Rhythm r = getSourceRhythm(spt.getRhythm());
            if (!rhythms.contains(r))
            {
                nbVoices += r.getRhythmVoices().size();
                rhythms.add(r);
            }
        }

        if (nbVoices > NB_AVAILABLE_CHANNELS)
        {
            throw new UnsupportedEditException(ResUtil.getString(getClass(), "ERR_NotEnoughChannels"));
        }

    }

    /**
     * Remove a rhythm.
     * <p>
     *
     * @param r
     */
    private void removeRhythm(Rhythm r)
    {
        LOGGER.log(Level.FINE, "removeRhythm() -- r={0} rhythmVoices={1}", new Object[]
        {
            r, Arrays.asList(rhythmVoices)
        });

        assert !(r instanceof AdaptedRhythm) : "r=" + r;

        for (RhythmVoice rvKey : rhythmVoices)
        {
            if (rvKey != null && rvKey.getContainer() == r)
            {
                int channel = getChannel(rvKey);
                setInstrumentMix(channel, null, null);
            }
        }
    }


    /**
     * Remove the user phrase channel with specific name.
     *
     * @param name
     * @return The channel which was removed, otherwise -1 if no channel was removed.
     */
    private int removeUserChannel(String name)
    {
        for (int channel : getUserChannels())
        {
            RhythmVoice rv = rhythmVoices[channel];
            if (rv.getName().equals(name))
            {
                changeInstrumentMix(channel, null, null);
                return channel;
            }
        }

        return -1;
    }

    /**
     * Change the internal state so that oldChannel becomes newChannel.
     *
     * @param oldChannel
     * @param newChannel
     */
    private synchronized void swapChannels(int oldChannel, int newChannel)
    {
        rhythmVoices[newChannel] = rhythmVoices[oldChannel];
        rhythmVoices[oldChannel] = null;
        var insMix = instrumentMixes[oldChannel];
        instrumentMixes[newChannel] = insMix;
        instrumentMixes[oldChannel] = null;
        var reroutingSaveInsMix = drumsReroutedChannels.get(oldChannel);
        if (reroutingSaveInsMix != null)
        {
            drumsReroutedChannels.remove(oldChannel);
            drumsReroutedChannels.put(newChannel, reroutingSaveInsMix);
        }
        saveMuteConfiguration[newChannel] = saveMuteConfiguration[oldChannel];
        saveMuteConfiguration[oldChannel] = false;
    }

    /**
     * The unique rhythm list used in this MidiMix.
     * <p>
     *
     * @return
     */
    private List<Rhythm> getUniqueRhythms()
    {
        ArrayList<Rhythm> result = new ArrayList<>();
        for (RhythmVoice rv : rhythmVoices)
        {
            if (rv != null && !(rv instanceof UserRhythmVoice) && !result.contains(rv.getContainer()))
            {
                result.add(rv.getContainer());
            }
        }
        return result;
    }

    private void fireUndoableEditHappened(UndoableEdit edit)
    {
        if (edit == null)
        {
            throw new IllegalArgumentException("edit=" + edit);
        }
        UndoableEditEvent event = new UndoableEditEvent(this, edit);
        for (UndoableEditListener l : undoListeners.toArray(new UndoableEditListener[undoListeners.size()]))
        {
            l.undoableEditHappened(event);
        }
    }

    /**
     * Return the source rhythm if r is an AdaptedRhythm, otherwise return r.
     *
     * @param r
     * @return A Rhythm instance which is not an AdaptedRhythm
     */
    private Rhythm getSourceRhythm(Rhythm r)
    {
        if (r instanceof AdaptedRhythm)
        {
            return ((AdaptedRhythm) r).getSourceRhythm();
        }
        return r;
    }


    /**
     * This enables XStream instance configuration even for private classes or classes from non-public packages of Netbeans modules.
     */
    @ServiceProvider(service = XStreamConfigurator.class)
    public static class XStreamConfig implements XStreamConfigurator
    {

        @Override
        public void configure(XStreamConfigurator.InstanceId instanceId, XStream xstream)
        {
            switch (instanceId)
            {
                case SONG_LOAD, SONG_SAVE ->
                {
                    // Nothing
                }
                case MIDIMIX_LOAD, MIDIMIX_SAVE ->
                {
                    if (instanceId.equals(MIDIMIX_LOAD))
                    {
                        // From 3.0 all public packages were renamed with api or spi somewhere in the path
                        // Need package aliasing to be able to load old sng/mix files            
                        xstream.aliasPackage("org.jjazz.midimix.api", "org.jjazz.midimix.api");     // Make sure new package name is not replaced by next alias
                        xstream.aliasPackage("org.jjazz.midimix", "org.jjazz.midimix.api");
                    }

                    // From 4.1.0 new aliases to get rid of fully qualified class names in .sng files
                    xstream.alias("MidiMix", MidiMix.class);
                    xstream.alias("MidiMixSP", MidiMix.SerializationProxy.class);
                    xstream.alias("RvStorage", MidiMix.SerializationProxy.RvStorage.class);

                    // From 4.1.3 XStream with Java23 can not read anymore the 2 private fields from the RvStorage class. 
                    // Making RvStorage class static + its 2 fields public solves the issue for the future, but this does not let us read old .mix files
                    // which contain the "outer-class reference=..." tag. So we just ignore this element.
                    xstream.ignoreUnknownElements("outer-class");

                }
                default ->
                    throw new AssertionError(instanceId.name());
            }
        }
    }


    // ---------------------------------------------------------------------
    // Serialization
    // --------------------------------------------------------------------- 
    private Object writeReplace()
    {
        return new SerializationProxy(this);
    }

    private void readObject(ObjectInputStream stream)
            throws InvalidObjectException
    {
        throw new InvalidObjectException("Serialization proxy required");

    }


    /**
     * A RhythmVoice depends on system dependent rhythm, therefore it must be stored in a special way: just save rhythm serial id + RhythmVoice name, and it
     * will be reconstructed at deserialization.
     * <p>
     * MidiMix is saved with Drums rerouting disabled and all solo status OFF, but all Mute status are saved.
     * <p>
     * spVERSION 2 changes saved fields see below<br>
     * spVERSION 3 (JJazzLab 4.1.0) introduces aliases to get rid of hard-coded qualified class names (XStreamConfig class introduction) <br>
     * spVERSION 4 (JJazzLab 4.1.3 with Java23) makes RvStorage class static + its 2 fields public + xStream.ignoreElement("outer-cast").<br>
     * spVERSION 5 (JJazzLab 5.0.2) add SP_RHYTHM_USER_CHANNEL_RHYTHM_ID to differentiate the melodic/drums type of a user voice.<br>
     */
    private static class SerializationProxy implements Serializable
    {

        private static final long serialVersionUID = -344448971122L;
        private static final String SP_MELODIC_USER_CHANNEL_RHYTHM_ID = "SpUserChannelRhythmID";
        private static final String SP_DRUMS_USER_CHANNEL_RHYTHM_ID = "SpDrumsUserChannelRhythmID";     // Since spVERSION 5
        private int spVERSION = 5;      // Do not make final!
        private InstrumentMix[] spInsMixes;
        private RvStorage[] spKeys;
        // spDelegates introduced with JJazzLab 2.1 => not used anymore with spVERSION=2        
        private List<RvStorage> spDelegates;   // Not used anymore, but keep it for backward compatibility

        private SerializationProxy(MidiMix mm)
        {
            // Make a copy because we want to disable drums rerouting in the saved instance
            MidiMix mmCopy = new MidiMix();         // Drums rerouting disabled by default
            for (Integer channel : mm.getUsedChannels())
            {
                RhythmVoice rv = mm.getRhythmVoice(channel);
                InstrumentMix insMix = mm.getInstrumentMix(channel);
                mmCopy.setInstrumentMix(channel, rv, new InstrumentMix(insMix));
            }


            // If mm had some rerouted channels, apply the saved settings in the copy
            for (Integer channel : mm.getDrumsReroutedChannels())
            {
                InstrumentMix saveInsMix = mm.drumsReroutedChannels.get(channel);
                mmCopy.setInstrumentMix(channel, mm.getRhythmVoice(channel), saveInsMix);           // This also sets solo/mute off
            }


            // Save the RhythmVoice keys and the InstrumentMixes
            spInsMixes = mmCopy.instrumentMixes;
            spKeys = new RvStorage[mmCopy.rhythmVoices.length];
            for (int i = 0; i < mmCopy.rhythmVoices.length; i++)
            {
                RhythmVoice rv = mmCopy.rhythmVoices[i];
                if (rv != null)
                {
                    // Restore the mute status so it can be saved
                    InstrumentMix originalInsMix = mm.getInstrumentMix(i);
                    if (!(rv instanceof UserRhythmVoice))
                    {
                        spInsMixes[i].setMute(originalInsMix.isMute());
                    }

                    // Store the RhythmVoice using a RvStorage object
                    spKeys[i] = new RvStorage(rv);
                }
            }
        }

        private Object readResolve() throws ObjectStreamException
        {
            assert spKeys.length == this.spInsMixes.length :
                    "spKeys=" + Arrays.asList(spKeys) + " spInsMixes=" + Arrays.asList(spInsMixes);
            MidiMix mm = new MidiMix();
            StringBuilder msg = new StringBuilder();


            for (int channel = 0; channel < spKeys.length; channel++)
            {
                RvStorage rvs = spKeys[channel];
                InstrumentMix insMix = spInsMixes[channel];


                if (insMix == null && rvs == null)
                {
                    // Normal case: no instrument
                    continue;
                }


                if (insMix == null || rvs == null)
                {
                    msg.append("Mix file error, unexpected null value for channel ").append(channel)
                            .append(":  rvs=").append(rvs).append(" insMix=").append(insMix);
                    throw new XStreamException(msg.toString());
                }


                // Retrieve the RhythmVoice
                RhythmVoice rv = rvs.rebuildRhythmVoice(insMix.getInstrument());
                if (rv == null)
                {
                    msg.append("Mix file error, can't rebuild RhythmVoice for channel=").append(channel)
                            .append(", rhythmId=").append(rvs.rhythmId).append(" and RhythmVoiceName=").append(rvs.rvName);
                    throw new XStreamException(msg.toString());
                }

                // Need a copy of insMix because setInstrumentMix() will make modifications on the object (e.g. setMute(false))
                // and we can not modify spInsMixes during the deserialization process.
                InstrumentMix insMixNew = new InstrumentMix(insMix);


                // Make sure we don't have a melodic instrument on a rhythm channel (can happen if we could not retrieve the MidiSynth for example)
                Instrument ins = insMixNew.getInstrument();
                if (rv.isDrums() && ins != GMSynth.getInstance().getVoidInstrument() && !ins.isDrumKit())
                {
                    insMixNew.setInstrument(GMSynth.getInstance().getVoidInstrument());
                }

                // Update the created MidiMix with the deserialized data
                mm.setInstrumentMix(channel, rv, insMixNew);
            }

            // spDelegates are no longer used from spVersion=2, IGNORED

            return mm;
        }

        /**
         * Stores the 2 strings used to identifiy a RhythmVoice.
         */
        static private class RvStorage
        {

            public String rhythmId;         // Had to switch to public for XStream with Java23
            public String rvName;           // Had to switch to public for XStream with Java23


            public RvStorage(RhythmVoice rv)
            {
                if (rv instanceof UserRhythmVoice)
                {
                    rhythmId = rv.isDrums() ? SP_DRUMS_USER_CHANNEL_RHYTHM_ID : SP_MELODIC_USER_CHANNEL_RHYTHM_ID;
                } else
                {
                    rhythmId = rv.getContainer().getUniqueId();
                }
                this.rvName = rv.getName();
            }

            /**
             * Rebuild a RhythmVoice or a UserRhythmVoice
             *
             * @param ins
             * @return Can be null
             */
            public RhythmVoice rebuildRhythmVoice(Instrument ins)
            {
                Objects.requireNonNull(ins);
                RhythmVoice rv = null;

                if (rhythmId.equals(SP_MELODIC_USER_CHANNEL_RHYTHM_ID))
                {
                    rv = new UserRhythmVoice(rvName);
                } else if (rhythmId.equals(SP_DRUMS_USER_CHANNEL_RHYTHM_ID))
                {
                    DrumKit kit = ins.getDrumKit();     // Might be null if ins is the VoidInstrument from the GM bank
                    rv = new UserRhythmVoice(rvName, kit == null ? new DrumKit() : kit);
                } else
                {
                    // Normal RhythmVoice
                    RhythmDatabase rdb = RhythmDatabase.getDefault();
                    Rhythm r;
                    try
                    {
                        r = rdb.getRhythmInstance(rhythmId);    // Possible exception here
                        rv = r.getRhythmVoices().stream().filter(rhv -> rhv.getName().equals(rvName)).findAny().orElse(null);
                    } catch (UnavailableRhythmException ex)
                    {
                        // Nothing
                    }
                }
                return rv;
            }
        }


    }

}

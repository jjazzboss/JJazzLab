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
package org.jjazz.midimix.api;

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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MidiUnavailableException;
import javax.swing.event.SwingPropertyChangeSupport;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.UndoableEdit;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.midi.api.Instrument;
import org.jjazz.midi.api.synths.GM1Instrument;
import org.jjazz.midi.api.InstrumentMix;
import org.jjazz.midi.api.InstrumentSettings;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midi.api.synths.Family;
import org.jjazz.midi.api.synths.StdSynth;
import org.jjazz.midimix.spi.RhythmVoiceInstrumentProvider;
import org.jjazz.rhythm.api.AdaptedRhythm;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.api.RhythmVoiceDelegate;
import org.jjazz.rhythm.database.api.RhythmDatabase;
import org.jjazz.rhythm.database.api.UnavailableRhythmException;
import org.jjazz.songstructure.api.event.SgsChangeEvent;
import org.jjazz.songstructure.api.event.SptAddedEvent;
import org.jjazz.songstructure.api.event.SptRemovedEvent;
import org.jjazz.songstructure.api.event.SptReplacedEvent;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongCreationException;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SgsChangeListener;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.undomanager.api.SimpleEdit;
import org.jjazz.util.api.ResUtil;
import org.jjazz.util.api.Utilities;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;

/**
 * A set of up to 16 InstrumentMixes, 1 per Midi channel with 1 RhythmVoice associated.
 * <p>
 * The object manages the solo functionality between the InstrumentMixes.<p>
 * A Song can be associated to the MidiMix so that InstrumentMixes are kept up to date with song's songStructure changes.<p>
 If MidiMix is modified the corresponding property change event is fired (e.g. PROP_INSTRUMENT_MUTE) then the
 PROP_MODIFIED_OR_SAVED_OR_RESET change event is also fired.
 <p>
 */

public class MidiMix implements SgsChangeListener, PropertyChangeListener, VetoableChangeListener, Serializable
{

    public static final String MIX_FILE_EXTENSION = "mix";
    /**
     * New or removed InstrumentMix.
     * <p>
     * OldValue=the old InstrumentMix, newValue=channel
     */
    public static final String PROP_CHANNEL_INSTRUMENT_MIX = "ChannelInstrumentMix";   //NOI18N 
    /**
     * oldValue=channel, newValue=true/false.
     */
    public static final String PROP_CHANNEL_DRUMS_REROUTED = "ChannelDrumsRerouted";   //NOI18N 
    /**
     * oldValue=InstumentMix, newValue=mute boolean state.
     */
    public static final String PROP_INSTRUMENT_MUTE = "InstrumentMute";   //NOI18N 
    /**
     * A drums instrument has changed with different keymap.
     * <p>
     * oldValue=channel, newValue=old keymap (may be null)
     */
    public static final String PROP_DRUMS_INSTRUMENT_KEYMAP = "DrumsInstrumentKeyMap";   //NOI18N 
    /**
     * oldValue=InstumentMix, newValue=transposition value.
     */
    public static final String PROP_INSTRUMENT_TRANSPOSITION = "InstrumentTransposition";   //NOI18N 
    /**
     * oldValue=InstumentMix, newValue=velocity shift value.
     */
    public static final String PROP_INSTRUMENT_VELOCITY_SHIFT = "InstrumentVelocityShift";   //NOI18N 
    /**
     * This property changes when the MidiMix is modified (false-&gt;true) or saved (true-&gt;false).
     */
    public static final String PROP_MODIFIED_OR_SAVED = "PROP_MODIFIED_OR_SAVED";   //NOI18N 
    public static final int NB_AVAILABLE_CHANNELS = MidiConst.CHANNEL_MAX - MidiConst.CHANNEL_MIN + 1;

    /**
     * Store the instrumentMixes, one per Midi Channel.
     */
    private final InstrumentMix[] instrumentMixes = new InstrumentMix[MidiConst.CHANNEL_MIN + NB_AVAILABLE_CHANNELS];
    /**
     * Store the RhythmVoices associated to an instrumentMix, one per channel.
     */
    private final RhythmVoice[] rvKeys = new RhythmVoice[MidiConst.CHANNEL_MIN + NB_AVAILABLE_CHANNELS];
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
            song.removeVetoableChangeListener(this);
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
     * Check that all RhythmVoices of this MidiMix belong to song rhythms.
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
            RhythmVoice rvKey = getRhythmVoice(channel);
            if (!(rvKey instanceof UserRhythmVoice) && !sgRvs.contains(rvKey))
            {
                throw new SongCreationException("channel=" + channel + " rvKey=" + rvKey + " sgRvs=" + sgRvs);   //NOI18N
            }
        }


        if (fullCheck)
        {
            for (RhythmVoice rvKey : sgRvs)
            {
                if (getChannel(rvKey) == -1)
                {
                    throw new SongCreationException("song rvKey=" + rvKey + " not found in MidiMix " + toString());   //NOI18N
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
    public List<Integer> getUserChannels()
    {
        List<Integer> res = new ArrayList<>();
        for (int i = MidiConst.CHANNEL_MIN; i <= MidiConst.CHANNEL_MAX; i++)
        {
            if (rvKeys[i] instanceof UserRhythmVoice)
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
    public List<UserRhythmVoice> getUserRhythmVoices()
    {
        List<UserRhythmVoice> res = new ArrayList<>();
        for (int i = MidiConst.CHANNEL_MIN; i <= MidiConst.CHANNEL_MAX; i++)
        {
            if (rvKeys[i] instanceof UserRhythmVoice)
            {
                res.add((UserRhythmVoice) rvKeys[i]);
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
     * Replace any existing InstrumentMix associated to the midi channel. The solo and "drums rerouted channel" status are reset
     * to off for the channel. <br>
     * Fire a PROP_CHANNEL_INSTRUMENT_MIX change event for this channel, and one UndoableEvent.
     *
     * @param channel A valid midi channel number.
     * @param rvKey   Can be null if insMix is also null. Is a song is set, must be consistent with its rhythms and user phrases.
     *                Can't be a RhythmVoiceDelegate.
     * @param insMix  Can be null if rvKey is also null.
     * @throws IllegalArgumentException if insMix is already part of this MidiMix for a different channel, or if rvKey is a
     *                                  RhythmVoiceDelegate.
     */
    public void setInstrumentMix(int channel, RhythmVoice rvKey, InstrumentMix insMix)
    {
        if (!MidiConst.checkMidiChannel(channel) || (rvKey instanceof RhythmVoiceDelegate) || (rvKey == null && insMix != null) || (rvKey != null && insMix == null))
        {
            throw new IllegalArgumentException("channel=" + channel + " rvKey=" + rvKey + " insMix=" + insMix);   //NOI18N
        }

        LOGGER.log(Level.FINE, "setInstrumentMix() channel={0} rvKey={1} insMix={2}", new Object[]
        {
            channel, rvKey, insMix
        });   //NOI18N

        if (rvKey != null && song != null)
        {
            // Check that rvKey belongs to song
            if (!(rvKey instanceof UserRhythmVoice) && !song.getSongStructure().getUniqueRhythmVoices(true, false).contains(rvKey))
            {
                throw new IllegalArgumentException("channel=" + channel + " rvKey=" + rvKey + " insMix=" + insMix + ". rvKey does not belong to any of the song's rhythms.");   //NOI18N
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
                throw new IllegalArgumentException("channel=" + channel + " rvKey=" + rvKey + " im=" + insMix + ". im is already present in MidiMix at channel " + ch);   //NOI18N
            }
        }

        changeInstrumentMix(channel, insMix, rvKey);
    }

    /**
     * Get the instrumet mix for the specified channel.
     *
     * @param channel A valid midi channel number
     * @return The InstrumentMix assigned to the specified Midi channel, or null if no InstrumentMix for this channel.
     */
    public InstrumentMix getInstrumentMixFromChannel(int channel)
    {
        if (!MidiConst.checkMidiChannel(channel))
        {
            throw new IllegalArgumentException("channel=" + channel);   //NOI18N
        }
        return instrumentMixes[channel];
    }

    /**
     * Get the instrumet mix for the specified RhythmVoice.
     *
     * @param rvKey If it's a RhythmVoiceDelegate, return the channel associated to its source RhythmVoice.
     * @return The InstrumentMix associated to rvKey. Null if no InstrumentMix found.
     */
    public InstrumentMix getInstrumentMixFromKey(RhythmVoice rvKey)
    {
        if (rvKey == null)
        {
            return null;
        }
        if (rvKey instanceof RhythmVoiceDelegate)
        {
            rvKey = ((RhythmVoiceDelegate) rvKey).getSource();
        }
        int index = Arrays.asList(rvKeys).indexOf(rvKey);
        return index == -1 ? null : instrumentMixes[index];
    }


    /**
     * Find the channel corresponding to the specified InstrumentMix.
     *
     * @param im
     * @return -1 if InstrumentMix not found.
     */
    public int getChannel(InstrumentMix im)
    {
        return Arrays.asList(instrumentMixes).indexOf(im);
    }

    /**
     *
     * @param im
     * @return null if InstrumentMix not found.
     */
    public RhythmVoice geRhythmVoice(InstrumentMix im)
    {
        int index = getChannel(im);
        return index == -1 ? null : rvKeys[index];
    }

    /**
     * Get the RhythmVoice for the specified Midi channel.
     * <p>
     *
     * @param channel
     * @return The RhythmVoice key corresponding to specified channel. Can be null.
     */
    public RhythmVoice getRhythmVoice(int channel)
    {
        if (!MidiConst.checkMidiChannel(channel))
        {
            throw new IllegalArgumentException("channel=" + channel);   //NOI18N
        }
        return rvKeys[channel];
    }

    /**
     * Get the Midi channel associated to the specified RhythmVoice.
     *
     * @param rvKey If it's a RhythmVoiceDelegate, return the channel associated to its source RhythmVoice.
     * @return -1 if key not found.
     */
    public int getChannel(RhythmVoice rvKey)
    {
        if (rvKey == null)
        {
            throw new IllegalArgumentException("key=" + rvKey);   //NOI18N
        }
        if (rvKey instanceof RhythmVoiceDelegate)
        {
            rvKey = ((RhythmVoiceDelegate) rvKey).getSource();
        }
        return Arrays.asList(rvKeys).indexOf(rvKey);
    }

    /**
     * Get the list of used channels in this MidiMix.
     *
     * @return The list of Midi channel numbers for which a non-null InstrumentMix is assigned.
     */
    public List<Integer> getUsedChannels()
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
    public List<Integer> getUsedChannels(Rhythm r)
    {
        if (r == null)
        {
            return getUsedChannels();
        }
        r = getSourceRhythm(r);
        ArrayList<Integer> channels = new ArrayList<>();
        for (int i = MidiConst.CHANNEL_MIN; i <= MidiConst.CHANNEL_MAX; i++)
        {
            if (instrumentMixes[i] != null && rvKeys[i].getContainer() == r)
            {
                channels.add(i);
            }
        }
        return channels;
    }

    /**
     * @return The list of Midi channel numbers for which no InstrumentMix is assigned.
     */
    public List<Integer> getUnusedChannels()
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
    public List<RhythmVoice> getRhythmVoices()
    {
        ArrayList<RhythmVoice> res = new ArrayList<>();
        for (RhythmVoice rvKey : rvKeys)
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
    public List<Integer> getDrumsReroutedChannels()
    {
        return new ArrayList<>(drumsReroutedChannels.keySet());
    }

    /**
     * Enable or disable the rerouting of specified channel to the GM Drums channel.
     * <p>
     * If enabled, the related InstrumentMix/Settings will be disabled, and vice versa.
     *
     * @param b
     * @param channel
     */
    public void setDrumsReroutedChannel(boolean b, int channel)
    {
        LOGGER.fine("setDrumsReroutedChannel() -- b=" + b + " channel=" + channel);   //NOI18N
        if (instrumentMixes[channel] == null)
        {
            throw new IllegalArgumentException("b=" + b + " channel=" + channel + " instrumentMixes=" + getInstrumentMixesPerChannel());   //NOI18N
        }
        if (b == drumsReroutedChannels.keySet().contains(channel) || channel == MidiConst.CHANNEL_DRUMS)
        {
            return;
        }
        InstrumentMix insMix = instrumentMixes[channel];
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
            assert saveMixData != null : "b=" + b + " channel=" + channel + " this=" + this;   //NOI18N
            drumsReroutedChannels.remove(channel);
            // Restore parameters enabled state
            insMix.setInstrumentEnabled(saveMixData.isInstrumentEnabled());
            insMix.getSettings().setChorusEnabled(saveMixData.getSettings().isChorusEnabled());
            insMix.getSettings().setReverbEnabled(saveMixData.getSettings().isReverbEnabled());
            insMix.getSettings().setPanoramicEnabled(saveMixData.getSettings().isPanoramicEnabled());
            insMix.getSettings().setVolumeEnabled(saveMixData.getSettings().isVolumeEnabled());
        }
        pcs.firePropertyChange(PROP_CHANNEL_DRUMS_REROUTED, channel, b);
        fireIsModified();
    }

    /**
     * Get the channels which normally need drums rerouting.
     * <p>
     * A channel needs rerouting if all the following conditions are met:<br>
     * 1/ channel != MidiConst.CHANNEL_DRUMS <br>
     * 2/ rv.isDrums() == true and rerouting is not already enabled <br>
     * 3/ instrument (or new instrument if one is provided in the mapChannelNewIns parameter) is the VoidInstrument<br>
     *
     * @param mapChannelNewIns Optional new instruments to use for some channels. Ignored if null. See
     *                         OutputSynth.getNeedFixInstruments().
     * @return Can be empty
     */
    public List<Integer> getChannelsNeedingDrumsRerouting(HashMap<Integer, Instrument> mapChannelNewIns)
    {
        List<Integer> res = new ArrayList<>();
        for (RhythmVoice rv : getRhythmVoices())
        {
            int channel = getChannel(rv);
            InstrumentMix insMix = getInstrumentMixFromKey(rv);
            Instrument newIns = mapChannelNewIns == null ? null : mapChannelNewIns.get(channel);
            Instrument ins = (newIns != null) ? newIns : insMix.getInstrument();
            LOGGER.fine("getChannelsNeedingDrumsRerouting() rv=" + rv + " channel=" + channel + " ins=" + ins);   //NOI18N


            if (channel != MidiConst.CHANNEL_DRUMS
                    && rv.isDrums()
                    && !getDrumsReroutedChannels().contains(channel)
                    && ins == StdSynth.getInstance().getVoidInstrument())
            {
                res.add(channel);
            }

        }
        LOGGER.fine("getChannelsNeedingDrumsRerouting() res=" + res);   //NOI18N
        return res;
    }

    /**
     * Return a free channel to be used in this MidiMix.
     * <p>
     * Try to keep channels in one section above the drums channel reserved to Drums. If not enough channels extend to channel
     * below the drums channel.
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
     * Add RhythmVoices (of Rhythm instances only, UserRhythmVoices are skipped) and InstrumentMixes copies from mm into this
     * MidiMix.
     * <p>
     * Copies have solo/drumsRerouting set to OFF. Method uses findFreeChannel() to allocate the new channels of mm if they are
     * not free in this MidiMix.
     * <p>
     * The operation will fire UndoableEvent edits.
     *
     * @param fromMm
     * @param r      If non null, copy fromMm instrumentMixes only if they belong to rhythm r (if r is an AdaptedRhythm, use its
     *               source rhythm).
     * @throws MidiUnavailableException If not enough channels available to accommodate mm instruments.
     */
    public final void addInstrumentMixes(MidiMix fromMm, Rhythm r) throws MidiUnavailableException
    {
        LOGGER.log(Level.FINE, "copyInstrumentMixes() -- rvKeys={0} fromMm.rvKeys={1}", new Object[]   //NOI18N
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
                assert newChannel != -1 : " getUsedChannels()=" + getUsedChannels();   //NOI18N
                InstrumentMix fromInsMix = fromMm.getInstrumentMixFromChannel(fromChannel);
                setInstrumentMix(newChannel, fromRvKey, new InstrumentMix(fromInsMix));
            }
        }


        LOGGER.log(Level.FINE, "addInstrumentMixes()     exit : rvKeys={0}", getRhythmVoices());   //NOI18N
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
     * Import is first done on matching RhythmVoices from the same rhythm. Then import only when RvTypes match. For
     * UserRhythmVoices import is done only if name matches.<br>
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
            throw new NullPointerException("mm");   //NOI18N
        }
        if (mm.rvKeys.length != rvKeys.length)
        {
            throw new IllegalStateException("mm.rvKeys.length=" + mm.rvKeys.length + " rvKeys.length=" + rvKeys.length);   //NOI18N
        }


        // Find the matching voices except the user phrase channels
        HashMap<RhythmVoice, RhythmVoice> mapMatchingVoices = new HashMap<>();
        List<RhythmVoice> rvs = getRhythmVoices();
        List<RhythmVoice> mmRvs = mm.getRhythmVoices();
        for (RhythmVoice mmRv : mmRvs)
        {
            boolean matched = false;

            // Try first on matching RhythmVoices from same rhythm
            for (RhythmVoice rv : rvs.toArray(new RhythmVoice[0]))
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
                for (RhythmVoice rv : rvs.toArray(new RhythmVoice[0]))
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
            InstrumentMix mmInsMix = mm.getInstrumentMixFromKey(mmRv);
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
                InstrumentMix mmInsMix = mm.getInstrumentMixFromKey(mmUserRv);
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
            throw new IllegalArgumentException("f=" + f);   //NOI18N
        }

        boolean b = true;
        if (f.exists() && !f.canWrite())
        {
            String msg = ResUtil.getString(getClass(), "ERR_CantOverwrite", f.getAbsolutePath());
            LOGGER.log(Level.WARNING, "saveToFileNotify() " + msg);   //NOI18N
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
                LOGGER.warning("saveToFileNotify() " + msg);   //NOI18N
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
 This will fire a PROP_MODIFIED_OR_SAVED_OR_RESET change event (true=&gt;false).
     *
     * @param f
     * @param isCopy Indicate that we save a copy, ie perform the file save but nothing else (eg no PROP_MODIFIED_OR_SAVED_OR_RESET state
               change)
     * @throws java.io.IOException
     */
    public void saveToFile(File f, boolean isCopy) throws IOException
    {
        if (f == null)
        {
            throw new IllegalArgumentException("f=" + f + " isCopy=" + isCopy);   //NOI18N
        }
        LOGGER.fine("saveToFile() f=" + f.getAbsolutePath() + " isCopy=" + isCopy);   //NOI18N

        if (!isCopy)
        {
            file = f;
        }

        try ( FileOutputStream fos = new FileOutputStream(f))
        {
            XStream xstream = new XStream();
            xstream.alias("MidiMix", MidiMix.class);
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
            LOGGER.warning("saveToFile() exception=" + e.getMessage());   //NOI18N
            // Translate into an IOException to be handled by the Netbeans framework 
            throw new IOException("XStream XML unmarshalling error", e);
        }
    }

    /**
     * All InstrumentMixes ordered by channel.
     *
     * @return A 16 items list, one instrumentMix per channel (some items can be null)
     */
    public List<InstrumentMix> getInstrumentMixesPerChannel()
    {
        return Arrays.asList(instrumentMixes);
    }

    /**
     * The non-null instrument mixes ordered by channel.
     *
     * @return Can be an empty list.
     */
    public List<InstrumentMix> getInstrumentMixes()
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

    public void addUndoableEditListener(UndoableEditListener l)
    {
        if (l == null)
        {
            throw new NullPointerException("l=" + l);   //NOI18N
        }
        undoListeners.remove(l);
        undoListeners.add(l);
    }

    public void removeUndoableEditListener(UndoableEditListener l)
    {
        if (l == null)
        {
            throw new NullPointerException("l=" + l);   //NOI18N
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
     *
     * @param f
     * @return Null if MidiMix could not be created for some reason.
     * @throws java.io.IOException If problem occured while reading file
     */
    public static MidiMix loadFromFile(File f) throws IOException
    {
        if (f == null)
        {
            throw new IllegalArgumentException("f=" + f);   //NOI18N
        }
        MidiMix mm = null;

        try ( var fis = new FileInputStream(f))
        {
            XStream xstream = Utilities.getSecuredXStreamInstance();
            // From 3.0 all public packages are renamed with api or spi somewhere in the path
            // Need package aliasing required to be able to load old sng/mix files            
            xstream.aliasPackage("org.jjazz.harmony.api", "org.jjazz.harmony.api");     // Make sure new package name is not replaced by next alias
            xstream.aliasPackage("org.jjazz.harmony", "org.jjazz.harmony.api");
            xstream.aliasPackage("org.jjazz.midi.api", "org.jjazz.midi.api");           // Make sure new package name is not replaced by next alias
            xstream.aliasPackage("org.jjazz.midi", "org.jjazz.midi.api");
            xstream.aliasPackage("org.jjazz.midimix.api", "org.jjazz.midimix.api");     // Make sure new package name is not replaced by next alias
            xstream.aliasPackage("org.jjazz.midimix", "org.jjazz.midimix.api");
            Reader r = new BufferedReader(new InputStreamReader(fis, "UTF-8"));        // Needed to support special/accented chars
            mm = (MidiMix) xstream.fromXML(r);
            mm.setFile(f);
        } catch (XStreamException e)
        {
            LOGGER.warning("loadFromFile() XStreamException e=" + e);   // Important in order to get the details of the XStream error   //NOI18N
            throw new IOException("XStream loading error", e);         // Translate into an IOException to be handled by the Netbeans framework 
        }
        return mm;
    }

    //-----------------------------------------------------------------------
    // Implementation of the SgsChangeListener interface
    //-----------------------------------------------------------------------
    @Override
    public void authorizeChange(SgsChangeEvent e) throws UnsupportedEditException
    {

        LOGGER.fine("authorizeChange() -- e=" + e);   //NOI18N

        // Build the list of SongPart after the change
        List<SongPart> spts = null;
        if (e instanceof SptAddedEvent)
        {
            spts = song.getSongStructure().getSongParts();
            spts.addAll(((SptAddedEvent) e).getSongParts());

        } else if (e instanceof SptReplacedEvent)
        {
            SptReplacedEvent e2 = (SptReplacedEvent) e;
            List<SongPart> oldSpts = e2.getSongParts();
            List<SongPart> newSpts = e2.getNewSpts();

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

    @Override
    public void songStructureChanged(SgsChangeEvent e)
    {
        LOGGER.fine("songStructureChanged() -- e=" + e);   //NOI18N


        JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(song);
        if (um != null && um.isUndoRedoInProgress())
        {
            // IMPORTANT : MidiMix generates his own undoableEdits
            // so we must not listen to SongStructure changes if undo/redo in progress, otherwise 
            // the "undo/redo" restore operations will be performed twice !
            return;
        }


        // Used to check if rhythm is really gone in case of multi-rhythm songs
        List<Rhythm> songRhythms = song.getSongStructure().getUniqueRhythms(true, false);
        List<Rhythm> mixRhythms = getUniqueRhythms();

        if (e instanceof SptAddedEvent)
        {

            SptAddedEvent e2 = (SptAddedEvent) e;
            for (SongPart spt : e2.getSongParts())
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
                        throw new IllegalStateException("Unexpected MidiUnavailableException ex=" + ex.getMessage() + " this=" + this + " r=" + r);   //NOI18N
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
                            throw new IllegalStateException("Unexpected MidiUnavailableException ex=" + ex.getMessage() + " this=" + this + " r=" + r);   //NOI18N
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
            // IMPORTANT : Song generates his own undoableEdits,
            // so we must not listen to song changes if undo/redo in progress, otherwise 
            // the "undo/redo" restore operations will be performed twice !
            LOGGER.log(Level.FINE, "vetoableChange() undo is in progress, exiting");   //NOI18N
            return;
        }


        if (e.getSource() == song)
        {
            if (e.getPropertyName().equals(Song.PROP_VETOABLE_USER_PHRASE))
            {
                String name = (String) e.getNewValue();
                if (name != null)
                {
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
                    name = (String) e.getOldValue();
                    removeUserChannel(name);

                }
            } else if (e.getPropertyName().equals(Song.PROP_VETOABLE_USER_PHRASE_CONTENT))
            {
                // User phrase was updated
                // Nothing to do
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
        LOGGER.fine("propertyChange() e=" + e);   //NOI18N
        if (e.getSource() instanceof InstrumentMix)
        {
            InstrumentMix insMix = (InstrumentMix) e.getSource();
            int channel = getChannel(insMix);
            if (e.getPropertyName().equals(InstrumentMix.PROP_SOLO))
            {
                boolean b = (boolean) e.getNewValue();
                LOGGER.fine("propertyChange() channel=" + channel + " solo=" + b);   //NOI18N
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
            } else if (e.getPropertyName().equals(InstrumentMix.PROP_MUTE))
            {
                boolean b = (boolean) e.getNewValue();
                // If in solo mode, pressing unmute of a muted channel turns it in solo mode
                if (b == false && !soloedInsMixes.isEmpty())
                {
                    insMix.setSolo(true);
                }
                // Forward the MUTE change event
                pcs.firePropertyChange(MidiMix.PROP_INSTRUMENT_MUTE, insMix, b);
            } else if (e.getPropertyName().equals(InstrumentMix.PROP_INSTRUMENT))
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
                        pcs.firePropertyChange(MidiMix.PROP_DRUMS_INSTRUMENT_KEYMAP, channel, oldKit != null ? oldKit.getKeyMap() : null);
                    }
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
            } else if (e.getPropertyName().equals(InstrumentSettings.PROPERTY_VELOCITY_SHIFT))
            {
                int value = (Integer) e.getNewValue();
                pcs.firePropertyChange(MidiMix.PROP_INSTRUMENT_VELOCITY_SHIFT, insMix, value);
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
        });   //NOI18N

        final InstrumentMix oldInsMix = instrumentMixes[channel];
        final RhythmVoice oldRvKey = rvKeys[channel];
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
        instrumentMixes[channel] = insMix;
        rvKeys[channel] = rvKey;

        // Prepare the undoable edit
        UndoableEdit edit = new SimpleEdit("Change instrumemt mix")
        {
            @Override
            public void undoBody()
            {
                instrumentMixes[channel] = oldInsMix;
                rvKeys[channel] = oldRvKey;
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
                });   //NOI18N

                pcs.firePropertyChange(PROP_CHANNEL_INSTRUMENT_MIX, insMix, channel);
                fireIsModified();
            }

            @Override
            public void redoBody()
            {
                instrumentMixes[channel] = insMix;
                rvKeys[channel] = rvKey;
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
                });   //NOI18N

                pcs.firePropertyChange(PROP_CHANNEL_INSTRUMENT_MIX, oldInsMix, channel);
                fireIsModified();
            }
        };
        fireUndoableEditHappened(edit);

        pcs.firePropertyChange(PROP_CHANNEL_INSTRUMENT_MIX, oldInsMix, channel);
        fireIsModified();

    }

    private void fireIsModified()
    {
        needSave = true;
        pcs.firePropertyChange(PROP_MODIFIED_OR_SAVED, false, true);
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
     * Manage the case where r is not the unique rhythm of the MidiMix: need to maintain instruments consistency to avoid
     * poor-sounding rhythms transitions.
     *
     * @param r
     * @throws MidiUnavailableException
     */
    private void addRhythm(Rhythm r) throws MidiUnavailableException
    {
        LOGGER.log(Level.FINE, "addRhythm() -- r={0} current rvKeys={1}", new Object[]   //NOI18N
        {
            r.getName(), Arrays.asList(rvKeys).toString()
        });

        assert !(r instanceof AdaptedRhythm) : "r=" + r;

        MidiMix mm = MidiMixManager.getInstance().findMix(r);
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
        LOGGER.fine("adaptInstrumentMixes() mm=" + mm + " r0=" + r0);   //NOI18N
        HashMap<String, InstrumentMix> mapKeyMix = new HashMap<>();
        HashMap<Family, InstrumentMix> mapFamilyMix = new HashMap<>();
        InstrumentMix r0InsMixDrums = null;
        InstrumentMix r0InsMixPerc = null;
        // First try to match InstrumentMixes using a "key" = "3 first char of Rv.getName() + GM1 family"
        for (int channel : getUsedChannels(r0))
        {
            // Build the keys from r0
            RhythmVoice rv = rvKeys[channel];
            InstrumentMix insMix = instrumentMixes[channel];
            if (rv.isDrums())
            {
                // Special case, use the 2 special variables for Drums or Percussion
                // Use the saved InstrumentMix if channel is drums rerouted
                InstrumentMix insMixDrums = getDrumsReroutedChannels().contains(channel) ? drumsReroutedChannels.get(channel) : insMix;
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
            Family family = insGM1 != null ? insGM1.getFamily() : null;
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
            RhythmVoice mmRv = mm.rvKeys[mmChannel];
            InstrumentMix mmInsMix = mm.instrumentMixes[mmChannel];
            InstrumentMix insMix;
            
            switch (mmRv.getType())
            {
                case DRUMS:
                    insMix = r0InsMixDrums;
                    break;
                case PERCUSSION:
                    insMix = r0InsMixPerc;
                    break;
                default:
                    GM1Instrument mmInsGM1 = mmInsMix.getInstrument().getSubstitute();  // Can be null            
                    Family mmFamily = mmInsGM1 != null ? mmInsGM1.getFamily() : null;
                    String mapKey = Utilities.truncate(mmRv.getName().toLowerCase(), 3) + "-" + ((mmFamily != null) ? mmFamily.name() : "");
                    insMix = mapKeyMix.get(mapKey);
                    break;
            }
            
            if (insMix != null)
            {
                // Copy InstrumentMix data
                mmInsMix.setInstrument(insMix.getInstrument());
                mmInsMix.getSettings().set(insMix.getSettings());
                doneChannels.add(mmChannel);
                LOGGER.finer("adaptInstrumentMixes() set (1) channel " + mmChannel + " instrument setting to : " + insMix.getSettings());   //NOI18N
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
            if (mmInsGM1 == null || mmInsGM1 == StdSynth.getInstance().getVoidInstrument())
            {
                continue;
            }
            Family mmFamily = mmInsGM1.getFamily();
            InstrumentMix insMix = mapFamilyMix.get(mmFamily);
            if (insMix != null)
            {
                // Copy InstrumentMix data
                mmInsMix.setInstrument(insMix.getInstrument());
                mmInsMix.getSettings().set(insMix.getSettings());
                LOGGER.finer("adaptInstrumentMixes() set (2) channel " + mmChannel + " instrument setting to : " + insMix.getSettings());   //NOI18N
            }
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
        LOGGER.log(Level.FINE, "removeRhythm() -- r={0} rvKeys={1}", new Object[]   //NOI18N
        {
            r, Arrays.asList(rvKeys)
        });

        assert !(r instanceof AdaptedRhythm) : "r=" + r;

        for (RhythmVoice rvKey : rvKeys)
        {
            if (rvKey != null && rvKey.getContainer() == r)
            {
                int channel = getChannel(rvKey);
                setInstrumentMix(channel, null, null);
            }
        }
    }

    /**
     * Add a user phrase channel for the specified name.
     *
     * @param userPhraseName
     * @throws MidiUnavailableException
     */
    protected void addUserChannel(String userPhraseName) throws MidiUnavailableException
    {
        int channel = getUsedChannels().contains(UserRhythmVoice.DEFAULT_USER_PHRASE_CHANNEL) ? findFreeChannel(false) : UserRhythmVoice.DEFAULT_USER_PHRASE_CHANNEL;
        if (channel == -1)
        {
            String msg = ResUtil.getString(getClass(), "ERR_NotEnoughChannels");
            throw new MidiUnavailableException(msg);

        }

        // Use a RhythmVoiceInstrumentProvider to get the instrument
        var urv = new UserRhythmVoice(userPhraseName);
        RhythmVoiceInstrumentProvider p = RhythmVoiceInstrumentProvider.Util.getProvider();
        Instrument ins = p.findInstrument(urv);
        var insMix = new InstrumentMix(ins, new InstrumentSettings());
        setInstrumentMix(channel, urv, insMix);
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
            RhythmVoice rv = rvKeys[channel];
            if (rv.getName().equals(name))
            {
                changeInstrumentMix(channel, null, null);
                return channel;
            }
        }

        return -1;
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
        for (RhythmVoice rv : rvKeys)
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
            throw new IllegalArgumentException("edit=" + edit);   //NOI18N
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
     * RhythmVoices depend on a system dependent rhythm, therefore it must be stored in a special way: just save rhythm serial id
     * + RhythmVoice name, and it will be reconstructed at deserialization.
     * <p>
     * MidiMix is saved with Drums rerouting disabled and all solo status OFF, but all Mute status are saved.
     */
    private static class SerializationProxy implements Serializable
    {

        private static final long serialVersionUID = -344448971122L;
        private static final String SP_USER_CHANNEL_RHYTHM_ID = "SpUserChannelRhythmID";
        private final int spVERSION = 2;
        private final InstrumentMix[] spInsMixes;
        private final RvStorage[] spKeys;
        // spDelegates introduced with JJazzLab 2.1 => not used anymore with spVERSION=2        
        private List<RvStorage> spDelegates;   // Not used anymore, but keep it for backward compatibility

        private SerializationProxy(MidiMix mm)
        {
            // Make a copy because we want to disable drums rerouting in the saved instance
            MidiMix mmCopy = new MidiMix();         // Drums rerouting disabled by default
            for (Integer channel : mm.getUsedChannels())
            {
                RhythmVoice rv = mm.getRhythmVoice(channel);
                InstrumentMix insMix = mm.getInstrumentMixFromChannel(channel);
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
            spKeys = new RvStorage[mmCopy.rvKeys.length];
            for (int i = 0; i < mmCopy.rvKeys.length; i++)
            {
                RhythmVoice rv = mmCopy.rvKeys[i];
                if (rv != null)
                {
                    // Restore the mute status so it can be saved
                    InstrumentMix originalInsMix = mm.getInstrumentMixFromChannel(i);
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
            assert spKeys.length == this.spInsMixes.length : "spKeys=" + Arrays.asList(spKeys) + " spInsMixes=" + Arrays.asList(spInsMixes);   //NOI18N
            MidiMix mm = new MidiMix();
            StringBuilder msg = new StringBuilder();


            for (int channel = 0; channel < spKeys.length; channel++)
            {
                RvStorage rvs = spKeys[channel];
                RhythmVoice rv = null;
                InstrumentMix insMix = spInsMixes[channel];


                if (insMix == null && rvs == null)
                {
                    // Normal case: no instrument
                    continue;
                }


                if (insMix == null || rvs == null)
                {
                    msg.append("Mix file error, unexpected null value for channel " + channel + ":  rvs=" + rvs + " insMix=" + insMix);
                    throw new XStreamException(msg.toString());
                }


                // Retrieve the RhythmVoice
                rv = rvs.rebuildRhythmVoice();
                if (rv == null)
                {
                    msg.append("Mix file error, can't rebuild RhythmVoice for channel=" + channel + ", rhythmId=" + rvs.rhythmId + " and RhythmVoiceName=" + rvs.rvName);
                    throw new XStreamException(msg.toString());
                }


                // Update the created MidiMix with the deserialized data
                // Need a copy of insMix because setInstrumentMix() will make modifications on the object (e.g. setMute(false))
                // and we can not modify spInsMixes during the deserialization process.
                InstrumentMix insMixNew = new InstrumentMix(insMix);
                mm.setInstrumentMix(channel, rv, insMixNew);
            }

            // spDelegates are no longer used from spVersion=2, IGNORED

            return mm;
        }

        private class RvStorage
        {

            private String rhythmId;
            private String rvName;

            public RvStorage(RhythmVoice rv)
            {
                if (rv instanceof UserRhythmVoice)
                {
                    rhythmId = SP_USER_CHANNEL_RHYTHM_ID;
                } else
                {
                    rhythmId = rv.getContainer().getUniqueId();
                }
                this.rvName = rv.getName();
            }

            /**
             * Rebuild a RhythmVoice or a UserRhythmVoice
             *
             * @return Can be null
             */
            public RhythmVoice rebuildRhythmVoice()
            {
                RhythmVoice rv = null;

                if (rhythmId.equals(SP_USER_CHANNEL_RHYTHM_ID))
                {
                    rv = new UserRhythmVoice(rvName);
                } else
                {
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

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
package org.jjazz.midimix.api;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import javax.sound.midi.MidiUnavailableException;
import javax.swing.event.UndoableEditListener;
import org.jjazz.midi.api.Instrument;
import org.jjazz.midi.api.InstrumentMix;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongCreationException;

/**
 * A set of up to 16 InstrumentMixes, 1 per Midi channel with 1 RhythmVoice associated.
 * <p>
 * The object manages the solo functionality between the InstrumentMixes.<p>
 * If MidiMix is modified the corresponding property change event is fired (e.g. PROP_INSTRUMENT_MUTE) then the PROP_MODIFIED_OR_SAVED_OR_RESET change event is
 * also fired.
 * <p>
 */
public interface MidiMixInt
{

    /**
     * oldValue=channel, newValue=true/false.
     */
    String PROP_CHANNEL_DRUMS_REROUTED = "ChannelDrumsRerouted";
    /**
     * Added, replaced or removed InstrumentMix.
     * <p>
     * OldValue=the old InstrumentMix (null if new InstrumentMix added), newValue=channel
     */
    String PROP_CHANNEL_INSTRUMENT_MIX = "ChannelInstrumentMix";
    /**
     * A drums instrument has changed with different keymap.
     * <p>
     * oldValue=channel, newValue=old keymap (may be null)
     */
    String PROP_DRUMS_INSTRUMENT_KEYMAP = "DrumsInstrumentKeyMap";
    /**
     * oldValue=InstumentMix, newValue=mute boolean state.
     */
    String PROP_INSTRUMENT_MUTE = "InstrumentMute";
    /**
     * oldValue=InstumentMix, newValue=transposition value.
     */
    String PROP_INSTRUMENT_TRANSPOSITION = "InstrumentTransposition";
    /**
     * oldValue=InstumentMix, newValue=velocity shift value.
     */
    String PROP_INSTRUMENT_VELOCITY_SHIFT = "InstrumentVelocityShift";
    /**
     * This property changes when the MidiMix is modified (false-&gt;true) or saved (true-&gt;false).
     */
    String PROP_MODIFIED_OR_SAVED = "PROP_MIDIMIX_MODIFIED_OR_SAVED";
    /**
     * Fired each time a MidiMix parameter which impacts music generation is modified, like instrument transposition.
     * <p>
     * OldValue=the property name that triggers the musical change, newValue=optional associated data.<p>
     * Use PROP_MODIFIED_OR_SAVED to get notified of any MidiMix change, including non-musical ones like track mute change, etc.
     */
    String PROP_MUSIC_GENERATION = "MidiMixMusicContent";
    /**
     * A RhythmVoice has replaced another one, e.g. this is used to change the name of a user track.
     * <p>
     * oldValue=old RhythmVoice, newValue=new RhythmVoice
     */
    String PROP_RHYTHM_VOICE = "RhythmVoice";
    /**
     * The channel of a RhythmVoice has been changed by user.
     * <p>
     * oldValue=old channel, newValue=new channel
     */
    String PROP_RHYTHM_VOICE_CHANNEL = "RhythmVoiceChannel";

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
    void addInstrumentMixes(MidiMix fromMm, Rhythm r) throws MidiUnavailableException;

    void addPropertyChangeListener(PropertyChangeListener l);

    void addPropertyChangeListener(String propertyName, PropertyChangeListener l);

    void addUndoableEditListener(UndoableEditListener l);

    /**
     * Add a user phrase channel for the specified phrase name.
     *
     * @param userPhraseName
     * @throws MidiUnavailableException
     */
    void addUserChannel(String userPhraseName) throws MidiUnavailableException;

    /**
     * Check if this MidiMix is consistent with the specified song.
     * <p>
     * Check that all RhythmVoices of this MidiMix belong to song rhythms. Check user tracks consistency between midiMix and song.
     *
     * @param sg
     * @param fullCheck If true also check that all song RhythmVoices are used in this MidiMix.
     * @throws org.jjazz.song.api.SongCreationException If an inconsistency is detected
     */
    void checkConsistency(Song sg, boolean fullCheck) throws SongCreationException;

    /**
     * Return a free channel to be used in this MidiMix.
     * <p>
     * Try to keep channels in one section above the drums channel reserved to Drums. If not enough channels extend to channel below the drums channel.
     *
     * @param findDrumsChannel If true try to use CHANNEL_DRUMS if it is available.
     * @return -1 if no channel found
     */
    int findFreeChannel(boolean findDrumsChannel);

    /**
     * Get the RhythmVoice corresponding to im.
     *
     * @param im
     * @return null if InstrumentMix not found.
     */
    RhythmVoice geRhythmVoice(InstrumentMix im);

    /**
     * Find the channel corresponding to the specified InstrumentMix.
     *
     * @param im
     * @return -1 if InstrumentMix not found.
     */
    int getChannel(InstrumentMix im);

    /**
     * Get the Midi channel associated to the specified RhythmVoice.
     *
     * @param rvKey If it's a RhythmVoiceDelegate, return the channel associated to its source RhythmVoice.
     * @return -1 if key not found.
     */
    int getChannel(RhythmVoice rvKey);

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
    List<Integer> getChannelsNeedingDrumsRerouting(HashMap<Integer, Instrument> mapChannelNewIns);

    /**
     * Get a deep copy of this MidiMix.
     * <p>
     * Mutable internal objects are deeply copied, e.g. InstrumentMixes.<br>
     * Not copied: undoableListeners, isSaveNeeded.
     *
     * @return
     */
    MidiMix getDeepCopy();

    /**
     * The channels which should be rerouted to the GM Drums channel.
     *
     * @return Note that returned list is not ordered. Can be empty.
     */
    List<Integer> getDrumsReroutedChannels();

    /**
     * The file where this object is stored.
     *
     * @return Null if not set.
     */
    File getFile();

    /**
     * Get the instrumet mix for the specified channel.
     *
     * @param channel A valid midi channel number
     * @return The InstrumentMix assigned to the specified Midi channel, or null if no InstrumentMix for this channel.
     */
    InstrumentMix getInstrumentMix(int channel);

    /**
     * Get the instrumet mix for the specified RhythmVoice.
     *
     * @param rv If it's a RhythmVoiceDelegate, return the channel associated to its source RhythmVoice.
     * @return The InstrumentMix associated to rv. Null if no InstrumentMix found.
     */
    InstrumentMix getInstrumentMix(RhythmVoice rv);

    /**
     * The non-null instrument mixes ordered by channel.
     *
     * @return Can be an empty list.
     */
    List<InstrumentMix> getInstrumentMixes();

    /**
     * All InstrumentMixes ordered by channel.
     *
     * @return A 16 items list, one instrumentMix per channel (some items can be null)
     */
    List<InstrumentMix> getInstrumentMixesPerChannel();

    /**
     * Get the RhythmVoice for the specified Midi channel.
     * <p>
     *
     * @param channel
     * @return The RhythmVoice key corresponding to specified channel. Can be null.
     */
    RhythmVoice getRhythmVoice(int channel);

    /**
     * Get all the RhythmVoices corresponding to the non-null InstrumentMixes.
     * <p>
     * Returned list includes UserRhythmVoice instances as well. The list does not contain RhythmVoiceDelegate instances.
     *
     * @return
     */
    List<RhythmVoice> getRhythmVoices();

    Song getSong();

    /**
     * The unique rhythm set used in this MidiMix.
     * <p>
     *
     * @return
     */
    Set<Rhythm> getUniqueRhythms();

    /**
     * @return The list of Midi channel numbers for which no InstrumentMix is assigned.
     */
    List<Integer> getUnusedChannels();

    /**
     * Get the list of used channels in this MidiMix.
     *
     * @return The list of Midi channel numbers for which a non-null InstrumentMix is assigned.
     */
    List<Integer> getUsedChannels();

    /**
     * Get the list of used channels for specified rhythm in this MidiMix.
     *
     * @param r If null return all used channels. If r is an AdaptedRhythm, returns the channels from it source rhythm.
     * @return The list of Midi channel numbers for rhythm r and for which a non-null InstrumentMix is assigned.
     */
    List<Integer> getUsedChannels(Rhythm r);

    /**
     * Get the Midi channels used by user tracks.
     *
     * @return
     */
    List<Integer> getUserChannels();

    /**
     * Get the user track RhythmVoice key for the specified name.
     *
     * @param name
     * @return Null if not found
     */
    UserRhythmVoice getUserRhythmVoice(String name);

    /**
     * Return the subset of RhythmVoices which are UserRhythmVoices.
     *
     * @return
     */
    List<UserRhythmVoice> getUserRhythmVoices();

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
    void importInstrumentMixes(MidiMix mm);

    /**
     * @return True if MidiMix has some unsaved changes.
     */
    boolean needSave();

    void removePropertyChangeListener(PropertyChangeListener l);

    void removePropertyChangeListener(String propertyName, PropertyChangeListener l);

    void removeUndoableEditListener(UndoableEditListener l);

    /**
     * Replace an existing RhythmVoice by a new one.
     * <p>
     * Fire a PROP_RHYTHM_VOICE and an undoable event.
     *
     * @param oldRv Must be a RhythmVoice used by this MidiMix.
     * @param newRv Must not be already used by this MidiMix.
     */
    void replaceRhythmVoice(RhythmVoice oldRv, RhythmVoice newRv);

    /**
     * Save this MidiMix to a file.
     * <p>
     * This will fire a PROP_MODIFIED_OR_SAVED change event (true=&gt;false).
     *
     * @param f
     * @param isCopy Indicate that we save a copy, ie perform the file save but nothing else (eg no PROP_MODIFIED_OR_SAVED state change)
     * @throws java.io.IOException
     */
    void saveToFile(File f, boolean isCopy) throws IOException;

    /**
     * Same as saveToFile() but notify user if problems.
     *
     * @param f
     * @param isCopy
     * @return False if a problem occured
     */
    boolean saveToFileNotify(File f, boolean isCopy);

    /**
     * Send the midi messages to initialize all the instrument mixes.
     * <p>
     * Midi messages are sent to the default JJazzLab Midi OUT device.
     */
    void sendAllMidiMixMessages();

    /**
     * Send the midi messages to set the volume of all instrument mixes.
     * <p>
     * Midi messages are sent to the default JJazzLab Midi OUT device.
     */
    void sendAllMidiVolumeMessages();

    /**
     * Enable or disable the rerouting of specified channel to GM Drums channel.
     * <p>
     * If enabled, the related InstrumentMix/Settings will be disabled, and vice versa.
     *
     * @param b
     * @param channel
     */
    void setDrumsReroutedChannel(boolean b, int channel);

    void setFile(File f);

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
    void setInstrumentMix(int channel, RhythmVoice rvKey, InstrumentMix insMix);

    /**
     * Change the channel of a RhythmVoice.
     * <p>
     * Fire a PROP_RHYTHM_VOICE_CHANNEL and an undoable event.
     *
     * @param rv         Must be a RhythmVoice used by this MidiMix.
     * @param newChannel Must be a free channel
     */
    void setRhythmVoiceChannel(RhythmVoice rv, int newChannel);

    /**
     * Associate a song to this MidiMix : listen to song changes to keep this MidiMix consistent.
     * <p>
     * Listen to rhythms and user phrases changes.
     *
     * @param sg Can be null.
     * @throws IllegalArgumentException If checkConsistency(sg, false) fails.
     */
    void setSong(Song sg);

}
